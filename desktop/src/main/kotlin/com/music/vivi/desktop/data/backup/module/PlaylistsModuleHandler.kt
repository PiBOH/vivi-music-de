package com.music.vivi.desktop.data.backup.module

import com.music.vivi.desktop.data.backup.model.BackupSection
import com.music.vivi.desktop.data.backup.model.BackupSongMetadata
import com.music.vivi.desktop.data.database.LocalPlaylistDao
import com.music.vivi.desktop.data.model.Playlist
import com.music.vivi.desktop.data.model.SortOption
import com.music.vivi.desktop.data.store.LocalLibraryStore
import com.music.vivi.desktop.utils.DesktopPathProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.booleanOrNull
import kotlinx.serialization.json.longOrNull
import java.io.File
import java.util.Base64

@Serializable
data class PlaylistsBackupPayload(
    val playlists: List<PlaylistBackupEntry> = emptyList(),
    val songMetadata: Map<String, BackupSongMetadata>? = null,
    val coverImages: Map<String, String>? = null
)

@Serializable
data class PlaylistBackupEntry(
    val id: String,
    val name: String,
    val songIds: List<String> = emptyList(),
    val createdAt: Long = System.currentTimeMillis(),
    val lastModified: Long = System.currentTimeMillis(),
    val isQueueGenerated: Boolean = false,
    val coverImageUri: String? = null,
    val coverColorArgb: Int? = null,
    val coverIconName: String? = null,
    val coverShapeType: String? = null,
    val coverShapeDetail1: Float? = null,
    val coverShapeDetail2: Float? = null,
    val coverShapeDetail3: Float? = null,
    val coverShapeDetail4: Float? = null,
    val source: String = "LOCAL"
)

class PlaylistsModuleHandler(
    private val localPlaylistDao: LocalPlaylistDao,
    private val store: LocalLibraryStore,
    private val songResolver: BackupSongResolver,
    private val json: Json = Json { ignoreUnknownKeys = true; encodeDefaults = true }
) : BackupModuleHandler {

    override val section: BackupSection = BackupSection.PLAYLISTS

    override suspend fun export(): String = withContext(Dispatchers.IO) {
        val playlists = localPlaylistDao.getAllPlaylists(SortOption.PlaylistNameAZ)
            .filter { it.source == "LOCAL" }

        val allSongIds = playlists.flatMap { it.songIds }.distinct()
        val metadata = songResolver.metadataFor(allSongIds)

        val coverImages = mutableMapOf<String, String>()
        for (pl in playlists) {
            val uri = pl.coverImageUri ?: continue
            val base64 = readFileAsBase64(uri)
            if (base64 != null) {
                coverImages[pl.id] = base64
            }
        }

        val payload = PlaylistsBackupPayload(
            playlists = playlists.map { it.toBackupEntry() },
            songMetadata = metadata.ifEmpty { null },
            coverImages = coverImages.ifEmpty { null }
        )
        json.encodeToString(PlaylistsBackupPayload.serializer(), payload)
    }

    override suspend fun countEntries(): Int = withContext(Dispatchers.IO) {
        localPlaylistDao.getAllPlaylists().count { it.source == "LOCAL" }
    }

    override suspend fun snapshot(): String = withContext(Dispatchers.IO) {
        val entries = localPlaylistDao.getAllPlaylists(SortOption.PlaylistNameAZ)
            .map { it.toBackupEntry() }
        json.encodeToString(ListSerializer(PlaylistBackupEntry.serializer()), entries)
    }

    override suspend fun restore(payload: String) = withContext(Dispatchers.IO) {
        val trimmed = payload.trim()
        val (backupPlaylists, songMetadata, coverImages) = if (trimmed.startsWith("[")) {
            val list = runCatching {
                json.decodeFromString(ListSerializer(PlaylistBackupEntry.serializer()), trimmed)
            }.getOrElse {
                parseLegacyPlaylists(trimmed)
            }
            Triple(list, null, null)
        } else {
            val parsed = json.decodeFromString(PlaylistsBackupPayload.serializer(), trimmed)
            Triple(parsed.playlists, parsed.songMetadata, parsed.coverImages)
        }

        val library = songResolver.library()
        val resolvedPlaylists = backupPlaylists.map { pl ->
            val resolvedSongIds = pl.songIds.mapNotNull { origId ->
                val meta = songMetadata?.get(origId)
                library.resolve(origId, meta) ?: origId.takeIf { library.resolve(it, null) != null }
            }

            var restoredCoverUri = pl.coverImageUri
            if (coverImages != null && coverImages.containsKey(pl.id)) {
                val base64 = coverImages[pl.id]
                if (!base64.isNullOrBlank()) {
                    val file = File(DesktopPathProvider.playlistCoversDir, "playlist_cover_${pl.id}.jpg")
                    try {
                        file.writeBytes(Base64.getDecoder().decode(base64))
                        restoredCoverUri = file.absolutePath
                    } catch (_: Exception) {}
                }
            }

            pl.copy(songIds = resolvedSongIds, coverImageUri = restoredCoverUri)
        }

        insertPlaylistsTransaction(resolvedPlaylists)
    }

    override suspend fun rollback(snapshot: String) = withContext(Dispatchers.IO) {
        val list = json.decodeFromString(ListSerializer(PlaylistBackupEntry.serializer()), snapshot)
        insertPlaylistsTransaction(list)
    }

    private suspend fun insertPlaylistsTransaction(playlists: List<PlaylistBackupEntry>) {
        val others = localPlaylistDao.getAllPlaylists().filter { it.source != "LOCAL" }
        val restored = playlists.map { it.toPlaylist() }
        store.update("playlists") { state ->
            state.copy(playlists = others + restored)
        }
        Unit
    }

    private fun Playlist.toBackupEntry(): PlaylistBackupEntry = PlaylistBackupEntry(
        id = id,
        name = name,
        songIds = songIds,
        createdAt = createdAt,
        lastModified = lastModified,
        isQueueGenerated = isQueueGenerated,
        coverImageUri = coverImageUri,
        coverColorArgb = coverColorArgb,
        coverIconName = coverIconName,
        coverShapeType = coverShapeType,
        coverShapeDetail1 = coverShapeDetail1,
        coverShapeDetail2 = coverShapeDetail2,
        coverShapeDetail3 = coverShapeDetail3,
        coverShapeDetail4 = coverShapeDetail4,
        source = source
    )

    private fun PlaylistBackupEntry.toPlaylist(): Playlist = Playlist(
        id = id,
        name = name,
        songIds = songIds,
        createdAt = createdAt,
        lastModified = lastModified,
        isQueueGenerated = isQueueGenerated,
        coverImageUri = coverImageUri,
        coverColorArgb = coverColorArgb,
        coverIconName = coverIconName,
        coverShapeType = coverShapeType,
        coverShapeDetail1 = coverShapeDetail1,
        coverShapeDetail2 = coverShapeDetail2,
        coverShapeDetail3 = coverShapeDetail3,
        coverShapeDetail4 = coverShapeDetail4,
        source = source
    )

    private fun readFileAsBase64(path: String): String? {
        return try {
            val file = File(path)
            if (!file.exists() || file.length() == 0L) null
            else Base64.getEncoder().encodeToString(file.readBytes())
        } catch (_: Exception) {
            null
        }
    }

    private fun parseLegacyPlaylists(jsonStr: String): List<PlaylistBackupEntry> {
        val element = runCatching { json.parseToJsonElement(jsonStr) }.getOrNull() as? JsonArray ?: return emptyList()
        return element.mapNotNull { item ->
            val obj = item as? JsonObject ?: return@mapNotNull null
            val id = (obj["id"] as? JsonPrimitive)?.content ?: return@mapNotNull null
            val name = (obj["name"] as? JsonPrimitive)?.content ?: "Playlist"
            val songIdsArray = obj["songIds"] as? JsonArray ?: obj["song_ids"] as? JsonArray
            val songIds = songIdsArray?.mapNotNull { (it as? JsonPrimitive)?.content } ?: emptyList()
            PlaylistBackupEntry(
                id = id,
                name = name,
                songIds = songIds,
                createdAt = (obj["createdAt"] as? JsonPrimitive)?.longOrNull ?: System.currentTimeMillis(),
                lastModified = (obj["lastModified"] as? JsonPrimitive)?.longOrNull ?: System.currentTimeMillis(),
                isQueueGenerated = (obj["isQueueGenerated"] as? JsonPrimitive)?.booleanOrNull ?: false
            )
        }
    }
}
