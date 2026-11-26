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
import java.io.FileOutputStream
import java.nio.file.AtomicMoveNotSupportedException
import java.nio.file.Files
import java.nio.file.StandardCopyOption

/** Where the library came from, shown in Settings next to the reimport action. */
@Serializable
data class LibraryImportRecord(
    val sourcePath: String,
    val importedAt: Long,
    val songs: Int,
    val favorites: Int,
    val playlists: Int,
    val lyrics: Int,
    val playbackEvents: Int
)

/**
 * The byte writer [LocalLibraryStore] uses for its temporary file. It is a seam so a
 * test can stop a write halfway and check that the library on disk is still the
 * previous one.
 */
internal object StoreFileWriter {
    @Volatile
    var write: (File, ByteArray) -> Unit = { target, bytes ->
        FileOutputStream(target).use { output ->
            output.write(bytes)
            output.fd.sync()
        }
    }
}

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
    val source: String? = null,
    val lastUpdated: Long = 0L
)

/** One playlist in a persisted Undo token: the name and the tracks in order. */
@Serializable
data class StoredPlaylistSnapshot(
    val title: String,
    val videoIds: List<String> = emptyList()
)

/**
 * The last account-playlist change Undo can still reverse.
 *
 * It is written beside the library so the bar survives closing Settings or
 * restarting the app, and so a restore that failed halfway can be retried with
 * the playlists that are left instead of losing the whole token.
 */
@Serializable
data class StoredPlaylistUndo(
    /** One of [RENAMED], [DELETED] or [ADDED]. */
    val kind: String,
    /** The account the change belongs to, so another account never sees this bar. */
    val accountId: String? = null,
    val playlistId: String? = null,
    val previousTitle: String? = null,
    val addedPlaylistIds: List<String> = emptyList(),
    val videoId: String? = null,
    val trackTitle: String? = null,
    val snapshots: List<StoredPlaylistSnapshot> = emptyList(),
    val message: String = "",
    val createdAt: Long = 0L
) {
    companion object {
        const val RENAMED = "renamed"
        const val DELETED = "deleted"
        const val ADDED = "added"
    }
}

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
    /**
     * Catalogue tracks that were played but are not part of the local library.
     * The history screen reads them next to the library songs, so a YouTube
     * Music track keeps its title, artist and cover after the play. Only the
     * most recent [PLAYED_SONGS_LIMIT] are kept.
     */
    val playedSongs: List<Song> = emptyList(),
    /** The queue as it was when the app last closed, so a restart resumes it. */
    val playbackQueue: List<Song> = emptyList(),
    val playbackQueueIndex: Int = -1,
    val pendingListens: List<ListenBrainzPendingListen> = emptyList(),
    val artists: List<StoredArtist> = emptyList(),
    /**
     * The order the Accounts card drew before orders were kept per account.
     * It is still read as the first account's order so a file written by an
     * older build keeps its order; once [youTubePlaylistOrders] has an entry
     * for that account, the map wins.
     */
    val youTubePlaylistOrder: List<String> = emptyList(),
    /**
     * The order each signed-in YouTube Music account's card draws, keyed by the
     * account id from `yt-accounts.json`. YouTube Music exposes no account-level
     * order, so the order is local to the device and to that account.
     */
    val youTubePlaylistOrders: Map<String, List<String>> = emptyMap(),
    /** The last account-playlist change Undo can reverse; survives a restart. */
    val youTubePlaylistUndo: StoredPlaylistUndo? = null,
    /**
     * Duplicate groups the owner chose to hide from the health scan. A key is
     * the group's song ids, sorted and joined with `|`, so a group whose files
     * change is reported again instead of staying hidden.
     */
    val ignoredDuplicateGroups: List<String> = emptyList(),
    val nextEventId: Long = 1L,
    val nextListenId: Long = 1L,
    /** The last track that was playing, so the next start can resume it paused. */
    val resumeSong: Song? = null,
    val resumePositionMs: Long = 0L,
    val resumeUpdatedAt: Long = 0L
) {
    val isEmpty: Boolean
        get() = songs.isEmpty() && favorites.isEmpty() && playlists.isEmpty() &&
            lyrics.isEmpty() && playbackEvents.isEmpty() && playedSongs.isEmpty() &&
            pendingListens.isEmpty() && ignoredDuplicateGroups.isEmpty()

    companion object {
        const val PLAYED_SONGS_LIMIT = 500
    }
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
        val element = root["localLibrary"]
        if (element != null) {
            val stored = runCatching {
                json.decodeFromJsonElement(LocalLibraryState.serializer(), element)
            }.getOrNull()
            if (stored != null) return stored

            // The file parsed as JSON but the library shape is not one this build
            // knows. Keep the file as evidence instead of writing a new library over
            // it, then try the rotated backup before anything else.
            log("the localLibrary key has an unknown shape; keeping the file as evidence")
            quarantine("schema")
            backupRoot()?.get("localLibrary")?.let { recovered ->
                runCatching {
                    json.decodeFromJsonElement(LocalLibraryState.serializer(), recovered)
                }.getOrNull()?.let { return it }
            }
        }

        // First start on this model: the key is not in the file yet, so the old
        // database, when there is one, is the library. It is imported once and
        // written into the file; from then on the key is the library.
        val imported = LegacyLibraryImport.read(legacyDatabaseFile)
        if (imported != null && !imported.isEmpty) {
            val record = recordFor(imported)
            write(imported, record)
            return imported
        }
        return LocalLibraryState()
    }

    /** The import that produced the current library, when there was one. */
    fun importRecord(): LibraryImportRecord? = runCatching {
        readRoot()["libraryImport"]?.let { element ->
            json.decodeFromJsonElement(LibraryImportRecord.serializer(), element)
        }
    }.getOrNull()

    /**
     * Replaces the current library with the one in the old database, as the first
     * start did, after the atomic write has rotated the current file into `.bak`.
     * The old database is only read: it is never modified.
     */
    fun reimportFromLegacy(): Result<LibraryImportRecord> {
        val imported = LegacyLibraryImport.read(legacyDatabaseFile)
            ?: return Result.failure(
                IllegalStateException(
                    if (legacyDatabaseFile.isFile) {
                        "The old library at ${legacyDatabaseFile.absolutePath} could not be read."
                    } else {
                        "No old library was found at ${legacyDatabaseFile.absolutePath}."
                    }
                )
            )
        if (imported.isEmpty) {
            return Result.failure(IllegalStateException("The old library is empty."))
        }
        val record = recordFor(imported)
        synchronized(lock) {
            write(imported, record)
            // write() keeps the previous snapshot when the disk write fails.
            if (cached !== imported) {
                return Result.failure(IllegalStateException("The library could not be written to ${file.absolutePath}."))
            }
        }
        notifyAllTables()
        return Result.success(record)
    }

    /** Drops the in-memory snapshot so the next read comes from disk (full restore). */
    fun reload() {
        synchronized(lock) { cached = null }
    }

    fun notifyAllTables() {
        notify("songs", "favorites", "playlists", "lyrics", "playback_events", "artists", "listenbrainz_pending")
    }

    private fun recordFor(state: LocalLibraryState) = LibraryImportRecord(
        sourcePath = legacyDatabaseFile.absolutePath,
        importedAt = System.currentTimeMillis(),
        songs = state.songs.size,
        favorites = state.favorites.size,
        playlists = state.playlists.size,
        lyrics = state.lyrics.size,
        playbackEvents = state.playbackEvents.size
    )

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

    private fun write(state: LocalLibraryState, record: LibraryImportRecord? = null) {
        val root = readRoot().toMutableMap()
        root["localLibrary"] = json.encodeToJsonElement(LocalLibraryState.serializer(), state)
        if (record != null) {
            root["libraryImport"] = json.encodeToJsonElement(LibraryImportRecord.serializer(), record)
        }
        val text = json.encodeToString(JsonObject.serializer(), JsonObject(root))
        try {
            writeAtomically(text)
            cached = state
        } catch (error: Exception) {
            // The file on disk is still the previous one: keep that snapshot so the
            // session does not advertise changes that were never persisted.
            log("could not write ${file.absolutePath}: $error")
        }
    }

    /**
     * Writes next to the target, syncs the bytes, rotates the previous file into
     * `.bak`, then renames the temporary file over the target. A crash at any point
     * leaves either the old file or the new one, never a half-written library.
     */
    private fun writeAtomically(text: String) {
        val directory = file.absoluteFile.parentFile ?: throw IllegalStateException("no parent directory")
        directory.mkdirs()
        val temporary = File(directory, "${file.name}.tmp")
        try {
            StoreFileWriter.write(temporary, text.toByteArray(Charsets.UTF_8))
            if (file.isFile && file.length() > 0L) {
                runCatching {
                    Files.copy(file.toPath(), backupFile.toPath(), StandardCopyOption.REPLACE_EXISTING)
                }.onFailure { log("could not rotate ${backupFile.absolutePath}: $it") }
            }
            try {
                Files.move(
                    temporary.toPath(),
                    file.toPath(),
                    StandardCopyOption.ATOMIC_MOVE,
                    StandardCopyOption.REPLACE_EXISTING
                )
            } catch (_: AtomicMoveNotSupportedException) {
                Files.move(temporary.toPath(), file.toPath(), StandardCopyOption.REPLACE_EXISTING)
            }
        } finally {
            if (temporary.exists()) runCatching { temporary.delete() }
        }
    }

    private val backupFile: File get() = File(file.absoluteFile.parentFile, "${file.name}.bak")

    /**
     * The file as it is on disk. A file that cannot be parsed is moved aside, so the
     * next start does not keep reading it, and the rotated backup is tried in its
     * place. Every key this store does not own is written back untouched.
     */
    private fun readRoot(): JsonObject {
        if (!file.exists()) return JsonObject(emptyMap())
        return try {
            json.parseToJsonElement(file.readText()).jsonObject
        } catch (error: Exception) {
            log("${file.absolutePath} cannot be parsed: $error")
            quarantine("unreadable")
            backupRoot() ?: JsonObject(emptyMap())
        }
    }

    private fun backupRoot(): JsonObject? = runCatching {
        if (backupFile.isFile) {
            json.parseToJsonElement(backupFile.readText()).jsonObject
        } else {
            null
        }
    }.onFailure { log("the rotated backup ${backupFile.absolutePath} cannot be parsed: $it") }
        .getOrNull()

    /** Moves the unreadable file to `device-sync.json.corrupt-<millis>`; never deletes it. */
    private fun quarantine(reason: String) {
        val target = File(
            file.absoluteFile.parentFile,
            "${file.name}.corrupt-${System.currentTimeMillis()}"
        )
        val moved = runCatching {
            Files.move(file.toPath(), target.toPath(), StandardCopyOption.REPLACE_EXISTING)
            true
        }.getOrDefault(false)
        if (moved) {
            log("moved the $reason file to ${target.absolutePath}")
        } else {
            log("the $reason file could not be moved aside; it stays at ${file.absolutePath}")
        }
    }

    private fun log(message: String) {
        System.err.println("[store] $message")
    }

    override fun close() = Unit
}
