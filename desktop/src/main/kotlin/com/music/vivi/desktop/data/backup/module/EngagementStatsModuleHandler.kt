package com.music.vivi.desktop.data.backup.module

import com.music.vivi.desktop.data.backup.model.BackupSection
import com.music.vivi.desktop.data.backup.model.BackupSongMetadata
import com.music.vivi.desktop.data.store.LocalLibraryStore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.intOrNull
import kotlinx.serialization.json.longOrNull

@Serializable
data class EngagementStatBackupEntry(
    val songId: String,
    val playCount: Int,
    val lastPlayedTimestamp: Long,
    val song: BackupSongMetadata? = null
)

class EngagementStatsModuleHandler(
    private val store: LocalLibraryStore,
    private val songResolver: BackupSongResolver,
    private val json: Json = Json { ignoreUnknownKeys = true; encodeDefaults = true }
) : BackupModuleHandler {

    override val section: BackupSection = BackupSection.ENGAGEMENT_STATS

    override suspend fun export(): String = withContext(Dispatchers.IO) {
        val list = store.load().songs
            .filter { it.playCount > 0 }
            .map { song ->
                EngagementStatBackupEntry(
                    songId = song.id,
                    playCount = song.playCount,
                    lastPlayedTimestamp = song.lastPlayedTimestamp
                )
            }

        val metadataMap = songResolver.metadataFor(list.map { it.songId })
        val entries = list.map { it.copy(song = metadataMap[it.songId]) }
        json.encodeToString(ListSerializer(EngagementStatBackupEntry.serializer()), entries)
    }

    override suspend fun countEntries(): Int = withContext(Dispatchers.IO) {
        store.load().songs.count { it.playCount > 0 }
    }

    override suspend fun snapshot(): String = export()

    override suspend fun restore(payload: String) = withContext(Dispatchers.IO) {
        val trimmed = payload.trim()
        val entries = runCatching {
            json.decodeFromString(ListSerializer(EngagementStatBackupEntry.serializer()), trimmed)
        }.getOrElse {
            parseLegacyEngagement(trimmed)
        }

        val library = songResolver.library()
        val resolved = entries.mapNotNull { entry ->
            val resolvedId = library.resolve(entry.songId, entry.song) ?: entry.songId
            entry.copy(songId = resolvedId)
        }

        applyStats(resolved)
    }

    override suspend fun rollback(snapshot: String) = withContext(Dispatchers.IO) {
        val entries = json.decodeFromString(ListSerializer(EngagementStatBackupEntry.serializer()), snapshot)
        applyStats(entries)
    }

    private suspend fun applyStats(entries: List<EngagementStatBackupEntry>) {
        val byId = entries.associateBy { it.songId }
        store.update("songs") { state ->
            state.copy(
                songs = state.songs.map { song ->
                    val stat = byId[song.id] ?: return@map song
                    song.copy(
                        playCount = stat.playCount,
                        lastPlayedTimestamp = stat.lastPlayedTimestamp
                    )
                }
            )
        }
        Unit
    }

    private fun parseLegacyEngagement(payload: String): List<EngagementStatBackupEntry> {
        val array = runCatching { json.parseToJsonElement(payload) }.getOrNull() as? JsonArray ?: return emptyList()
        return array.mapNotNull { element ->
            val obj = element as? JsonObject ?: return@mapNotNull null
            val songId = (obj["songId"] as? JsonPrimitive)?.content
                ?: (obj["song_id"] as? JsonPrimitive)?.content
                ?: return@mapNotNull null
            val playCount = (obj["playCount"] as? JsonPrimitive)?.intOrNull
                ?: (obj["play_count"] as? JsonPrimitive)?.intOrNull ?: 1
            val lastPlayed = (obj["lastPlayedTimestamp"] as? JsonPrimitive)?.longOrNull
                ?: (obj["last_played_timestamp"] as? JsonPrimitive)?.longOrNull
                ?: System.currentTimeMillis()
            EngagementStatBackupEntry(songId = songId, playCount = playCount, lastPlayedTimestamp = lastPlayed)
        }
    }
}
