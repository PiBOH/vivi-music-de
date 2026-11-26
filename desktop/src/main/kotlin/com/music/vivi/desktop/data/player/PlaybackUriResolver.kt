package com.music.vivi.desktop.data.player

import com.music.vivi.desktop.data.model.Song
import com.music.vivi.desktop.data.youtube.StreamResolver
import com.music.vivi.desktop.data.youtube.YouTubeMusicRepository
import com.music.vivi.desktop.domain.repository.JellyfinRepository
import com.music.vivi.desktop.domain.repository.NavidromeRepository
import com.music.vivi.desktop.domain.repository.SettingsRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * A media target the engine can open, with the headers the request needs.
 *
 * A googlevideo URL is signed for the client that asked for it, so the URL and
 * its User-Agent are one value here: passing the URL alone to the engine leaves
 * mpv asking with its own User-Agent and the server answering 403.
 */
data class PlaybackTarget(
    val uri: String,
    val httpHeaders: List<String> = emptyList()
)

class PlaybackUriResolver(
    private val navidromeRepository: NavidromeRepository,
    private val jellyfinRepository: JellyfinRepository,
    private val youtubeMusicRepository: YouTubeMusicRepository? = null,
    private val audioCache: PlaybackAudioCache? = null,
    private val settingsRepository: SettingsRepository? = null
) {

    /**
     * Turns a song into a media target the engine can open.
     *
     * A YouTube Music track (`ytmusic://<videoId>`) is resolved to a googlevideo
     * URL here, at play time, because those URLs are single-use and expire within
     * minutes. The network round-trip is why this is a suspending function: the
     * caller resolves off the UI thread.
     */
    suspend fun resolveTarget(song: Song): PlaybackTarget = withContext(Dispatchers.IO) {
        val youtubeId = youtubeMusicRepository?.videoIdOf(song)
            ?: song.path
                .takeIf { it.startsWith(YouTubeMusicRepository.VIDEO_SCHEME) }
                ?.removePrefix(YouTubeMusicRepository.VIDEO_SCHEME)
                ?.takeIf { it.isNotBlank() }
        if (youtubeId != null) {
            val settings = settingsRepository?.settingsFlow?.value
            val cached = audioCache?.cachedFile(song)
            if (settings?.audioCacheEnabled != false && cached != null) {
                return@withContext PlaybackTarget(uri = cached.absolutePath)
            }
            val quality = StreamResolver.AudioQuality.from(settings?.audioQuality)
            val stream = youtubeMusicRepository?.resolveStream(youtubeId, quality)
            if (stream != null) {
                val target = PlaybackTarget(
                    uri = stream.url,
                    httpHeaders = listOf(
                        "User-Agent: ${stream.userAgent}",
                        "Referer: https://www.youtube.com/"
                    )
                )
                if (settings?.audioCacheEnabled == true) {
                    audioCache?.scheduleDownload(
                        song = song,
                        target = target,
                        limitMb = settings.audioCacheLimitMb
                    )
                }
                return@withContext target
            }
            // Unresolved: hand the scheme back so the engine reports one failed
            // track instead of the player silently keeping the previous audio.
            return@withContext PlaybackTarget(song.path)
        }

        val navId = song.navidromeId ?: song.path.takeIf { it.startsWith("navidrome://") }?.removePrefix("navidrome://")
        if (navId != null) {
            val streamUrl = navidromeRepository.getStreamUrl(navId)
            if (streamUrl != null) return@withContext PlaybackTarget(streamUrl)
        }

        val jfId = song.jellyfinId ?: song.path.takeIf { it.startsWith("jellyfin://") }?.removePrefix("jellyfin://")
        if (jfId != null) {
            val streamUrl = jellyfinRepository.getStreamUrl(jfId)
            if (streamUrl != null) return@withContext PlaybackTarget(streamUrl)
        }

        PlaybackTarget(song.path)
    }

    /** The URL alone, for the callers that do not open the engine themselves. */
    suspend fun resolve(song: Song): String = resolveTarget(song).uri
}
