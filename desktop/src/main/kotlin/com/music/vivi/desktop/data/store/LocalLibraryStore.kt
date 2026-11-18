package com.music.vivi.desktop.data.store

import com.music.vivi.desktop.data.model.PlaybackEvent
import com.music.vivi.desktop.data.model.Playlist
import com.music.vivi.desktop.data.model.Song
import com.music.vivi.desktop.data.network.listenbrainz.model.ListenBrainzPendingListen
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.encodeToJsonElement
import kotlinx.serialization.json.jsonObject
import java.io.File

/** A liked song: the id and the moment it was liked (the `favorites` table). */
@Serializable
data class StoredFavorite(
    val songId: String,
    val dateAdded: Long
)

/**
 * A cached lyrics body (the `lyrics_cache` table). The two bodies stay the JSON
 * strings the lyrics reader produced, so a cache hit is decoded exactly like a
 * fresh network answer.
 */
@Serializable
data class StoredLyrics(
    val songId: String,
    val plainJson: String? = null,
    val syncedJson: String? = null,
    val isFromRemote: Boolean = false,
    val lastUpdated: Long = 0L
)

/** An artist row: the name plus the artwork the user pinned to it (the `artists` table). */
@Serializable
data class StoredArtist(
    val id: Long,
    val name: String,
    val imageUrl: String? = null,
    val customImageUri: String? = null
)

/**
 * Everything the desktop library keeps, in one serializable object.
 *
 * The shape follows the 1.55.0 `device-sync.json`: one state object, songs and
 * playlists as full values, the ids of the liked songs as entries, and the
 * append-only histories as lists. It is what [LocalLibraryStore] reads at
 * startup, keeps in memory and writes back on every change.
 */
@Serializable
data class LocalLibraryState(
    val songs: List<Song> = emptyList(),
    val favorites: List<StoredFavorite> = emptyList(),
    val playlists: List<Playlist> = emptyList(),
    val lyrics: List<StoredLyrics> = emptyList(),
    val playbackEvents: List<PlaybackEvent> = emptyList(),
    val pendingListens: List<ListenBrainzPendingListen> = emptyList(),
    val artists: List<StoredArtist> = emptyList(),
    val nextEventId: Long = 1L,
    val nextListenId: Long = 1L
) {
    val isEmpty: Boolean
        get() = songs.isEmpty() && favorites.isEmpty() && playlists.isEmpty() &&
            lyrics.isEmpty() && playbackEvents.isEmpty() && pendingListens.isEmpty()
}

/**
 * The desktop's only local store, on `~/.vivimusic/device-sync.json`.
 *
 * 1.55.0 kept its library, queue, liked songs and settings in that one pretty
 * JSON file: it was read once through a cached snapshot, every change went
 * through a lock so two writers could never clobber each other, and the file
 * was rewritten whole. This store reproduces that model for the library data.
 * The state lives under the `localLibrary` key so the rest of the 1.55.0 file
 * (device identity, pairing, settings written by a previous build) is preserved
 * instead of being overwritten.
 *
 * The previous build kept the same data in `vivimusicde.db`. That file is read
 * once, on the first start with an empty store, through [LegacyLibraryImport];
 * the original stays on disk untouched.
 */
class LocalLibraryStore(
    private val file: File,
    private val legacyDatabaseFile: File
) : AutoCloseable {

    private val json = Json {
        prettyPrint = true
        ignoreUnknownKeys = true
        encodeDefaults = true
        explicitNulls = false
    }

    /** Serializes load/save, like the 1.55.0 `DesktopSettings` lock. */
    private val lock = Any()

    /**
     * In-memory snapshot of the file. The screens ask for the library on every
     * navigation and every scan pass asks for the known paths, so the file is
     * parsed once and served from here until [save] replaces it.
     */
    @Volatile
    private var cached: LocalLibraryState? = null

    private val _tableInvalidationFlow = MutableSharedFlow<String>(extraBufferCapacity = 64)

    /** Table-name invalidations the repository flows watch (`songs`, `favorites`, ...). */
    val tableInvalidationFlow: SharedFlow<String> = _tableInvalidationFlow.asSharedFlow()

    init {
        file.parentFile?.mkdirs()
    }

    fun load(): LocalLibraryState = cached ?: synchronized(lock) {
        cached ?: read().also { cached = it }
    }

    private fun read(): LocalLibraryState {
        val root = readRoot()
        if (root.containsKey("localLibrary")) {
            val stored = root["localLibrary"]?.let { element ->
                runCatching { json.decodeFromJsonElement(LocalLibraryState.serializer(), element) }.getOrNull()
            }
            if (stored != null) return stored
        }

        // First start on this model: the key is not in the file yet, so the old
        // database, when there is one, is the library. It is imported once and
        // written into the file; from then on the key is the library.
        val imported = LegacyLibraryImport.read(legacyDatabaseFile)
        if (imported != null && !imported.isEmpty) {
            write(imported)
            return imported
        }
        return LocalLibraryState()
    }

    fun save(state: LocalLibraryState) {
        synchronized(lock) {
            write(state)
        }
    }

    /**
     * Atomic read-modify-write: applies [transform] to the in-memory state and
     * writes the result under the same lock, then tells the watched flows which
     * tables changed. This is the `DesktopSettings.update` shape of 1.55.0.
     */
    fun update(vararg tables: String, transform: (LocalLibraryState) -> LocalLibraryState) {
        val changed = synchronized(lock) {
            val before = load()
            val after = transform(before)
            if (after !== before) {
                write(after)
            }
            after !== before
        }
        if (changed) notify(*tables)
    }

    fun notify(vararg tables: String) {
        for (table in tables) {
            _tableInvalidationFlow.tryEmit(table)
        }
    }

    private fun write(state: LocalLibraryState) {
        val root = readRoot().toMutableMap()
        root["localLibrary"] = json.encodeToJsonElement(LocalLibraryState.serializer(), state)
        runCatching {
            file.writeText(json.encodeToString(JsonObject.serializer(), JsonObject(root)))
            cached = state
        }.onFailure {
            // A failed write keeps the in-memory state: the session is not lost,
            // and the next successful save writes the whole file again.
            cached = state
        }
    }

    /**
     * The file as it is on disk, empty when it does not exist or cannot be
     * parsed. Every key this store does not own is written back untouched.
     */
    private fun readRoot(): JsonObject = try {
        if (file.exists()) {
            json.parseToJsonElement(file.readText()).jsonObject
        } else {
            JsonObject(emptyMap())
        }
    } catch (_: Exception) {
        JsonObject(emptyMap())
    }

    override fun close() = Unit
}
