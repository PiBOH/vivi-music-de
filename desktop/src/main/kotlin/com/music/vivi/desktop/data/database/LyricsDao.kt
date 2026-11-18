package com.music.vivi.desktop.data.database

import com.music.vivi.desktop.data.model.Lyrics
import com.music.vivi.desktop.data.model.SyncedLine
import com.music.vivi.desktop.data.store.LocalLibraryStore
import com.music.vivi.desktop.data.store.StoredLyrics
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.json.Json

private val json = Json { ignoreUnknownKeys = true }
private val syncedLineListSerializer = ListSerializer(SyncedLine.serializer())

/**
 * The lyrics cache, on the JSON store.
 *
 * The bodies are kept as the JSON text the reader produced (a plain-line array
 * and a synced-line array), which is what the previous `lyrics_cache` table
 * stored in its two text columns.
 */
class LyricsDao(
    private val store: LocalLibraryStore,
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO
) {

    suspend fun getLyrics(songId: String): Lyrics? = withContext(ioDispatcher) {
        val entry = store.load().lyrics.firstOrNull { it.songId == songId } ?: return@withContext null
        val plain = entry.plainJson?.takeIf { it.isNotBlank() }?.let { raw ->
            runCatching { json.decodeFromString<List<String>>(raw) }.getOrElse {
                raw.split("\n").filter { line -> line.isNotBlank() }
            }
        }
        val synced = entry.syncedJson?.takeIf { it.isNotBlank() }?.let { raw ->
            runCatching { json.decodeFromString(syncedLineListSerializer, raw) }.getOrNull()
        }
        Lyrics(plain = plain, synced = synced, areFromRemote = entry.isFromRemote)
    }

    suspend fun insertOrUpdateLyrics(songId: String, lyrics: Lyrics) = withContext(ioDispatcher) {
        val entry = StoredLyrics(
            songId = songId,
            plainJson = lyrics.plain?.let { json.encodeToString(it) },
            syncedJson = lyrics.synced?.let { json.encodeToString(syncedLineListSerializer, it) },
            isFromRemote = lyrics.areFromRemote,
            lastUpdated = System.currentTimeMillis()
        )
        store.update("lyrics_cache") { state ->
            state.copy(lyrics = state.lyrics.filterNot { it.songId == songId } + entry)
        }
    }

    suspend fun deleteLyrics(songId: String) = withContext(ioDispatcher) {
        store.update("lyrics_cache") { state ->
            state.copy(lyrics = state.lyrics.filterNot { it.songId == songId })
        }
    }

    suspend fun deleteAllLyrics() = withContext(ioDispatcher) {
        store.update("lyrics_cache") { state ->
            state.copy(lyrics = emptyList())
        }
    }
}
