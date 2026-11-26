package com.music.vivi.desktop.data.repository

import java.io.File

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.jaudiotagger.tag.FieldKey

import com.music.vivi.desktop.data.database.LyricsDao
import com.music.vivi.desktop.data.model.Lyrics
import com.music.vivi.desktop.data.network.lyrics.LrcLibClient
import com.music.vivi.desktop.data.network.lyrics.LyricsProvidersClient
import com.music.vivi.desktop.data.preferences.LyricsSourcePreference
import com.music.vivi.desktop.data.youtube.YouTubeLyrics
import com.music.vivi.desktop.data.youtube.YouTubeMusicRepository
import com.music.vivi.desktop.domain.repository.LyricsRepository
import com.music.vivi.desktop.domain.repository.SettingsRepository
import com.music.vivi.desktop.utils.AudioFileReaderUtils
import com.music.vivi.desktop.utils.LrcParser
import com.music.vivi.desktop.utils.MultiLangRomanizer

class LyricsRepositoryImpl(
    private val lyricsDao: LyricsDao,
    private val lrcLibClient: LrcLibClient = LrcLibClient(),
    private val lyricsProvidersClient: LyricsProvidersClient = LyricsProvidersClient(),
    private val settingsRepository: SettingsRepository? = null,
    private val youtubeLyrics: YouTubeLyrics? = null,
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO
) : LyricsRepository {

    override suspend fun getLyrics(
        songId: String,
        songPath: String,
        title: String,
        artist: String,
        durationMs: Long,
        album: String
    ): Lyrics? = withContext(ioDispatcher) {
        val cached = lyricsDao.getLyrics(songId)
        if (cached != null) {
            val enriched = MultiLangRomanizer.enrichLyrics(cached)
            if (enriched != cached) {
                lyricsDao.insertOrUpdateLyrics(songId, enriched)
            }
            return@withContext enriched
        }

        // A streamed track has no file to read: the lyrics provider answers by video
        // id, which is exact, so it is tried before the title/artist chain below.
        if (songPath.startsWith(YouTubeMusicRepository.VIDEO_SCHEME)) {
            val videoId = songPath.removePrefix(YouTubeMusicRepository.VIDEO_SCHEME)
            val settings = settingsRepository?.getSettings()
            // The mobile order asks the configured providers first (Musixmatch at
            // the top), then falls back to the video-id lookup.
            if (settings?.fetchOnlineLyrics == true) {
                val provided = fetchFromProviders(
                    title = title,
                    artist = artist,
                    durationMs = durationMs,
                    album = album,
                    videoId = videoId,
                    enabled = settings.lyricsProviders
                )
                if (provided != null) {
                    lyricsDao.insertOrUpdateLyrics(songId, provided)
                    return@withContext provided
                }
            }
            val remote = youtubeLyrics?.fetch(videoId, durationMs)?.copy(source = "SimpMusic")
            if (remote != null) {
                lyricsDao.insertOrUpdateLyrics(songId, remote)
                return@withContext remote
            }
            return@withContext null
        }

        val settings = settingsRepository?.getSettings()
        val preference = settings?.let {
            LyricsSourcePreference.fromStorageKey(it.lyricsSourcePreference)
        } ?: LyricsSourcePreference.EMBEDDED_FIRST
        val allowOnline = settings?.fetchOnlineLyrics ?: false
        val file = File(songPath)

        val fetchers: List<suspend () -> Lyrics?> = when (preference) {
            LyricsSourcePreference.EMBEDDED_FIRST -> listOfNotNull(
                { fetchEmbedded(file) },
                if (allowOnline) ({ fetchOnline(title, artist, durationMs, album, settings?.lyricsProviders.orEmpty()) }) else null,
                { fetchLocal(file) }
            )
            LyricsSourcePreference.API_FIRST -> listOfNotNull(
                if (allowOnline) ({ fetchOnline(title, artist, durationMs, album, settings?.lyricsProviders.orEmpty()) }) else null,
                { fetchEmbedded(file) },
                { fetchLocal(file) }
            )
            LyricsSourcePreference.LOCAL_FIRST -> listOfNotNull(
                { fetchLocal(file) },
                { fetchEmbedded(file) },
                if (allowOnline) ({ fetchOnline(title, artist, durationMs, album, settings?.lyricsProviders.orEmpty()) }) else null
            )
        }

        for (fetcher in fetchers) {
            val lyrics = fetcher()
            if (lyrics != null && (!lyrics.synced.isNullOrEmpty() || !lyrics.plain.isNullOrEmpty())) {
                val enriched = MultiLangRomanizer.enrichLyrics(lyrics)
                lyricsDao.insertOrUpdateLyrics(songId, enriched)
                return@withContext enriched
            }
        }

        null
    }

    private fun fetchLocal(file: File): Lyrics? {
        if (!file.exists()) return null
        val lrcCandidate = File(file.parentFile, "${file.nameWithoutExtension}.lrc")
        val ttmlCandidate = File(file.parentFile, "${file.nameWithoutExtension}.ttml")
        val lyricsFile = when {
            lrcCandidate.exists() && lrcCandidate.isFile -> lrcCandidate
            ttmlCandidate.exists() && ttmlCandidate.isFile -> ttmlCandidate
            else -> null
        }
        if (lyricsFile != null) {
            return runCatching {
                val text = lyricsFile.readText(Charsets.UTF_8)
                LrcParser.parse(text).copy(areFromRemote = false, source = "Local file")
            }.getOrNull()
        }
        return null
    }

    private fun fetchEmbedded(file: File): Lyrics? {
        if (!file.exists()) return null
        return runCatching {
            val audioFile = AudioFileReaderUtils.readAudioFile(file)
            val tag = audioFile.tag ?: return@runCatching null

            var firstPlainLyrics: Lyrics? = null
            val candidates = buildList {
                val standardTag = tag.getFirst(FieldKey.LYRICS)
                if (!standardTag.isNullOrBlank()) add(standardTag)
                listOf("LYRICS", "SYNCEDLYRICS", "TTML", "UNSYNCEDLYRICS").forEach { key ->
                    runCatching { tag.getFirst(key) }.getOrNull()?.let {
                        if (it.isNotBlank()) add(it)
                    }
                }
            }.distinct()

            for (field in candidates) {
                val parsed = LrcParser.parse(field).copy(areFromRemote = false, source = "Embedded")
                if (!parsed.synced.isNullOrEmpty()) return@runCatching parsed
                if (firstPlainLyrics == null && !parsed.plain.isNullOrEmpty()) {
                    firstPlainLyrics = parsed
                }
            }
            firstPlainLyrics
        }.getOrNull()
    }

    private suspend fun fetchOnline(
        title: String,
        artist: String,
        durationMs: Long,
        album: String,
        enabledProviders: List<String>
    ): Lyrics? {
        if (title.isBlank()) return null
        val provided = fetchFromProviders(
            title = title,
            artist = artist,
            durationMs = durationMs,
            album = album,
            videoId = null,
            enabled = enabledProviders
        )
        if (provided != null) return provided
        // LrcLib keeps its own title/artist client for the search dialog; ask it
        // only when the provider list itself has nothing to offer.
        if (artist.isBlank()) return null
        val onlineLyrics = lrcLibClient.fetchLyrics(title = title, artist = artist)
        return onlineLyrics?.copy(areFromRemote = true, source = "LrcLib")
    }

    private suspend fun fetchFromProviders(
        title: String,
        artist: String,
        durationMs: Long,
        album: String,
        videoId: String?,
        enabled: List<String>
    ): Lyrics? {
        val result = lyricsProvidersClient.fetch(
            title = title,
            artist = artist,
            durationSeconds = (durationMs / 1000L).toInt().coerceAtLeast(0),
            album = album.takeIf { it.isNotBlank() },
            videoId = videoId,
            enabled = enabled.ifEmpty { LyricsProvidersClient.DEFAULT_ENABLED }
        ) ?: return null
        val parsed = LrcParser.parse(result.text).copy(areFromRemote = true, source = result.source)
        return parsed.takeIf { !it.synced.isNullOrEmpty() || !it.plain.isNullOrEmpty() }
    }

    override suspend fun saveLyrics(songId: String, lyrics: Lyrics) {
        val enriched = MultiLangRomanizer.enrichLyrics(lyrics)
        lyricsDao.insertOrUpdateLyrics(songId, enriched)
    }

    override suspend fun deleteLyrics(songId: String) = withContext(ioDispatcher) {
        lyricsDao.deleteLyrics(songId)
    }

    override suspend fun resetAllLyrics() = withContext(ioDispatcher) {
        lyricsDao.deleteAllLyrics()
    }

    override suspend fun searchOnlineLyrics(query: String): List<com.music.vivi.desktop.data.network.lyrics.LyricsSearchResultItem> = withContext(ioDispatcher) {
        lrcLibClient.searchCandidates(query)
    }
}
