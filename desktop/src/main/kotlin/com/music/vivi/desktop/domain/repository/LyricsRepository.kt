package com.music.vivi.desktop.domain.repository

import com.music.vivi.desktop.data.model.Lyrics
import com.music.vivi.desktop.data.network.lyrics.LyricsSearchResultItem

interface LyricsRepository {
    suspend fun getLyrics(
        songId: String,
        songPath: String,
        title: String,
        artist: String,
        durationMs: Long = 0L,
        album: String = ""
    ): Lyrics?
    suspend fun saveLyrics(songId: String, lyrics: Lyrics)
    suspend fun deleteLyrics(songId: String)
    suspend fun resetAllLyrics()
    suspend fun searchOnlineLyrics(query: String): List<LyricsSearchResultItem>
}
