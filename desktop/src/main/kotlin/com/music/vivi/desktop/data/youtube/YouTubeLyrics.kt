package com.music.vivi.desktop.data.youtube

import com.music.simpmusic.SimpMusicLyrics
import com.music.vivi.desktop.data.model.Lyrics
import com.music.vivi.desktop.utils.LrcParser
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlin.math.abs

/**
 * Lyrics for a YouTube Music track, taken from the `lyricsProvider` module.
 *
 * SimpMusic answers by video id, which is exact, so it is tried before the
 * title/artist chain the local library uses (LrcLib). `richSyncLyrics` is skipped
 * on purpose: it is a word-timing JSON, not LRC, and the app's parser reads LRC.
 */
class YouTubeLyrics(
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO
) {

    suspend fun fetch(videoId: String, durationMs: Long): Lyrics? = withContext(ioDispatcher) {
        val tracks = runCatching { SimpMusicLyrics.getLyricsByVideoId(videoId) }
            .onFailure { YtLog.log("lyrics", "$videoId failed: ${it.message}") }
            .getOrNull()
            .orEmpty()
        if (tracks.isEmpty()) return@withContext null

        val ordered = if (durationMs > 0) {
            tracks.sortedBy { abs(((it.duration ?: 0) * 1000L) - durationMs) }
        } else {
            tracks
        }

        for (track in ordered) {
            val syncedRaw = track.syncedLyrics?.takeIf { it.isNotBlank() }
            if (syncedRaw != null) {
                val parsed = runCatching { LrcParser.parse(syncedRaw) }.getOrNull()
                if (parsed != null && (!parsed.synced.isNullOrEmpty() || !parsed.plain.isNullOrEmpty())) {
                    return@withContext parsed.copy(areFromRemote = true)
                }
            }
            val plainRaw = track.plainLyrics?.takeIf { it.isNotBlank() }
            if (plainRaw != null) {
                return@withContext Lyrics(
                    plain = plainRaw.lines().map { it.trim() },
                    areFromRemote = true
                )
            }
        }
        null
    }
}
