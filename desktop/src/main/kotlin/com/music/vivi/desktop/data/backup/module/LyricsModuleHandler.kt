package com.music.vivi.desktop.data.backup.module

import com.music.vivi.desktop.data.backup.model.BackupSection
import com.music.vivi.desktop.data.backup.model.BackupSongMetadata
import com.music.vivi.desktop.data.store.LocalLibraryStore
import com.music.vivi.desktop.data.store.StoredLyrics
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.booleanOrNull

@Serializable
data class LyricsBackupEntry(
    val songId: String,
    val plainLyrics: String? = null,
    val syncedLyricsJson: String? = null,
    val isFromRemote: Boolean = false,
    val lastUpdated: Long = System.currentTimeMillis(),
    val song: BackupSongMetadata? = null
)

class LyricsModuleHandler(
    private val store: LocalLibraryStore,
    private val songResolver: BackupSongResolver,
    private val json: Json = Json { ignoreUnknownKeys = true; encodeDefaults = true }
) : BackupModuleHandler {

    override val section: BackupSection = BackupSection.LYRICS

    override suspend fun export(): String = withContext(Dispatchers.IO) {
        val list = store.load().lyrics.map { entry ->
            LyricsBackupEntry(
                songId = entry.songId,
                plainLyrics = entry.plainJson,
                syncedLyricsJson = entry.syncedJson,
                isFromRemote = entry.isFromRemote,
                lastUpdated = entry.lastUpdated
            )
        }

        val metadataMap = songResolver.metadataFor(list.map { it.songId })
        val entriesWithMeta = list.map { it.copy(song = metadataMap[it.songId]) }
        json.encodeToString(ListSerializer(LyricsBackupEntry.serializer()), entriesWithMeta)
    }

    override suspend fun countEntries(): Int = withContext(Dispatchers.IO) {
        store.load().lyrics.size
    }

    override suspend fun snapshot(): String = withContext(Dispatchers.IO) {
        val list = store.load().lyrics.map { entry ->
            LyricsBackupEntry(
                songId = entry.songId,
                plainLyrics = entry.plainJson,
                syncedLyricsJson = entry.syncedJson,
                isFromRemote = entry.isFromRemote,
                lastUpdated = entry.lastUpdated
            )
        }
        json.encodeToString(ListSerializer(LyricsBackupEntry.serializer()), list)
    }

    override suspend fun restore(payload: String) = withContext(Dispatchers.IO) {
        val entries = runCatching {
            json.decodeFromString(ListSerializer(LyricsBackupEntry.serializer()), payload)
        }.getOrElse {
            parseLegacyLyrics(payload)
        }
        val library = songResolver.library()

        val resolved = entries.mapNotNull { entry ->
            val resolvedId = library.resolve(entry.songId, entry.song) ?: entry.songId
            entry.copy(songId = resolvedId)
        }.distinctBy { it.songId }

        replaceLyrics(resolved)
    }

    override suspend fun rollback(snapshot: String) = withContext(Dispatchers.IO) {
        val entries = json.decodeFromString(ListSerializer(LyricsBackupEntry.serializer()), snapshot)
        replaceLyrics(entries)
    }

    private suspend fun replaceLyrics(entries: List<LyricsBackupEntry>) {
        store.update("lyrics_cache") { state ->
            state.copy(
                lyrics = entries.map { item ->
                    StoredLyrics(
                        songId = item.songId,
                        plainJson = item.plainLyrics,
                        syncedJson = item.syncedLyricsJson,
                        isFromRemote = item.isFromRemote,
                        lastUpdated = item.lastUpdated
                    )
                }
            )
        }
        Unit
    }

    private fun parseLegacyLyrics(payload: String): List<LyricsBackupEntry> {
        val array = runCatching { json.parseToJsonElement(payload) }.getOrNull() as? JsonArray ?: return emptyList()
        return array.mapNotNull { element ->
            val obj = element as? JsonObject ?: return@mapNotNull null
            val songId = (obj["songId"] as? JsonPrimitive)?.content
                ?: (obj["song_id"] as? JsonPrimitive)?.content
                ?: return@mapNotNull null
            val content = (obj["content"] as? JsonPrimitive)?.content
            val isSynced = (obj["isSynced"] as? JsonPrimitive)?.booleanOrNull ?: false
            val plainLyrics = (obj["plainLyrics"] as? JsonPrimitive)?.content
                ?: if (!isSynced) content else null
            val syncedLyricsJson = (obj["syncedLyricsJson"] as? JsonPrimitive)?.content
                ?: if (isSynced) content else null
            val isFromRemote = (obj["isFromRemote"] as? JsonPrimitive)?.booleanOrNull ?: false
            LyricsBackupEntry(
                songId = songId,
                plainLyrics = plainLyrics,
                syncedLyricsJson = syncedLyricsJson,
                isFromRemote = isFromRemote
            )
        }
    }
}
