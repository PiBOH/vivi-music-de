package com.music.vivi.desktop.data.backup.module

import com.music.vivi.desktop.data.backup.model.BackupSection
import com.music.vivi.desktop.data.backup.model.BackupSongMetadata
import com.music.vivi.desktop.data.model.PlaybackEvent
import com.music.vivi.desktop.data.store.LocalLibraryStore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.longOrNull

@Serializable
data class PlaybackEventBackupEntry(
    val songId: String,
    val timestamp: Long,
    val durationMs: Long,
    val song: BackupSongMetadata? = null
)

@Serializable
data class SongEngagementBackupEntry(
    val songId: String,
    val playCount: Int,
    val lastPlayedTimestamp: Long,
    val song: BackupSongMetadata? = null
)

@Serializable
data class PlaybackHistoryBackupPayload(
    val events: List<PlaybackEventBackupEntry> = emptyList(),
    val engagementStats: List<SongEngagementBackupEntry> = emptyList()
)

class PlaybackHistoryModuleHandler(
    private val store: LocalLibraryStore,
    private val songResolver: BackupSongResolver,
    private val json: Json = Json { ignoreUnknownKeys = true; encodeDefaults = true }
) : BackupModuleHandler {

    override val section: BackupSection = BackupSection.PLAYBACK_HISTORY

    override suspend fun export(): String = withContext(Dispatchers.IO) {
        val state = store.load()
        val events = state.playbackEvents
            .sortedByDescending { it.timestamp }
            .take(10000)
            .map { PlaybackEventBackupEntry(songId = it.songId, timestamp = it.timestamp, durationMs = it.durationMs) }
        val engagementStats = state.songs
            .filter { it.playCount > 0 }
            .map {
                SongEngagementBackupEntry(
                    songId = it.id,
                    playCount = it.playCount,
                    lastPlayedTimestamp = it.lastPlayedTimestamp
                )
            }

        val metadataMap = songResolver.metadataFor(
            (events.map { it.songId } + engagementStats.map { it.songId }).toSet()
        )
        val payload = PlaybackHistoryBackupPayload(
            events = events.map { it.copy(song = metadataMap[it.songId]) },
            engagementStats = engagementStats.map { it.copy(song = metadataMap[it.songId]) }
        )
        json.encodeToString(PlaybackHistoryBackupPayload.serializer(), payload)
    }

    override suspend fun countEntries(): Int = withContext(Dispatchers.IO) {
        store.load().playbackEvents.size
    }

    override suspend fun snapshot(): String = withContext(Dispatchers.IO) {
        val state = store.load()
        val payload = PlaybackHistoryBackupPayload(
            events = state.playbackEvents.map {
                PlaybackEventBackupEntry(songId = it.songId, timestamp = it.timestamp, durationMs = it.durationMs)
            },
            engagementStats = state.songs
                .filter { it.playCount > 0 }
                .map {
                    SongEngagementBackupEntry(
                        songId = it.id,
                        playCount = it.playCount,
                        lastPlayedTimestamp = it.lastPlayedTimestamp
                    )
                }
        )
        json.encodeToString(PlaybackHistoryBackupPayload.serializer(), payload)
    }

    override suspend fun restore(payload: String) = withContext(Dispatchers.IO) {
        val trimmed = payload.trim()
        val parsed = if (trimmed.startsWith("[")) {
            val list = runCatching {
                json.decodeFromString(ListSerializer(PlaybackEventBackupEntry.serializer()), trimmed)
            }.getOrElse {
                parseLegacyPlaybackEvents(trimmed)
            }
            PlaybackHistoryBackupPayload(events = list)
        } else {
            json.decodeFromString(PlaybackHistoryBackupPayload.serializer(), trimmed)
        }

        val library = songResolver.library()
        val resolvedEvents = parsed.events.mapNotNull { event ->
            val resolvedId = library.resolve(event.songId, event.song) ?: event.songId
            event.copy(songId = resolvedId)
        }

        val resolvedStats = parsed.engagementStats.mapNotNull { stat ->
            val resolvedId = library.resolve(stat.songId, stat.song) ?: stat.songId
            stat.copy(songId = resolvedId)
        }

        replaceHistory(resolvedEvents, resolvedStats)
    }

    override suspend fun rollback(snapshot: String) = withContext(Dispatchers.IO) {
        val parsed = json.decodeFromString(PlaybackHistoryBackupPayload.serializer(), snapshot)
        replaceHistory(parsed.events, parsed.engagementStats)
    }

    private suspend fun replaceHistory(
        events: List<PlaybackEventBackupEntry>,
        stats: List<SongEngagementBackupEntry>
    ) {
        val statsById = stats.associateBy { it.songId }
        store.update("playback_events", "songs") { state ->
            var nextId = 1L
            val storedEvents = events.map { event ->
                PlaybackEvent(
                    id = nextId++,
                    songId = event.songId,
                    timestamp = event.timestamp,
                    durationMs = event.durationMs
                )
            }
            state.copy(
                playbackEvents = storedEvents,
                nextEventId = nextId,
                songs = state.songs.map { song ->
                    val stat = statsById[song.id] ?: return@map song
                    song.copy(
                        playCount = stat.playCount,
                        lastPlayedTimestamp = stat.lastPlayedTimestamp
                    )
                }
            )
        }
        Unit
    }

    private fun parseLegacyPlaybackEvents(payload: String): List<PlaybackEventBackupEntry> {
        val array = runCatching { json.parseToJsonElement(payload) }.getOrNull() as? JsonArray ?: return emptyList()
        return array.mapNotNull { element ->
            val obj = element as? JsonObject ?: return@mapNotNull null
            val songId = (obj["songId"] as? JsonPrimitive)?.content
                ?: (obj["song_id"] as? JsonPrimitive)?.content
                ?: return@mapNotNull null
            val timestamp = (obj["timestamp"] as? JsonPrimitive)?.longOrNull ?: 0L
            val durationMs = (obj["durationMs"] as? JsonPrimitive)?.longOrNull ?: 0L
            PlaybackEventBackupEntry(songId = songId, timestamp = timestamp, durationMs = durationMs)
        }
    }
}
