package com.music.vivi.desktop.data.network.lyrics

import com.music.kugou.KuGou
import com.music.lrclib.LrcLib
import com.music.musixmatch.Musixmatch
import com.music.paxsenix.Paxsenix
import com.music.simpmusic.SimpMusicLyrics
import com.music.unison.Unison
import com.music.vivi.betterlyrics.BetterLyrics
import com.music.vivi.desktop.BuildConfig
import com.music.youlyplus.YouLyPlus
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull

/**
 * The online lyrics sources, in the priority order the mobile app uses.
 *
 * Every provider answers with a raw LRC body: the caller parses it with the
 * shared [com.music.vivi.desktop.utils.LrcParser] and caches the result. A
 * provider that fails or times out is skipped, so one dead service never
 * leaves a song without lyrics while another source is reachable.
 */
class LyricsProvidersClient(
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO
) {

    data class ProviderResult(val source: String, val text: String)

    companion object {
        /** The mobile registry's default order, without the providers this module does not ship. */
        val DEFAULT_ORDER = listOf(
            "Musixmatch",
            "YouLyPlus",
            "Paxsenix",
            "BetterLyrics",
            "LrcLib",
            "Kugou",
            "Unison",
            "SimpMusic"
        )

        val DEFAULT_ENABLED: List<String> = DEFAULT_ORDER

        fun displayName(key: String): String = when (key) {
            "BetterLyrics" -> "Better Lyrics"
            "Kugou" -> "KuGou"
            "SimpMusic" -> "SimpMusic"
            else -> key
        }
    }

    private val paxsenixReady: Boolean by lazy {
        runCatching { Paxsenix.init(BuildConfig.APP_VERSION_NAME) }.isSuccess
    }

    /**
     * Fetches the first usable body from the enabled providers.
     *
     * [durationSeconds] is what the providers compare against their catalogue
     * entries; [videoId] is only used by SimpMusic, which addresses a track by
     * its YouTube id instead of title and artist.
     */
    suspend fun fetch(
        title: String,
        artist: String,
        durationSeconds: Int,
        album: String? = null,
        videoId: String? = null,
        enabled: List<String> = DEFAULT_ENABLED
    ): ProviderResult? = withContext(ioDispatcher) {
        if (title.isBlank()) return@withContext null
        Musixmatch.logger = { message -> println("[lyrics] Musixmatch: $message") }
        val enabledSet = enabled.toSet()
        val order = DEFAULT_ORDER.filter { it in enabledSet }
        println("[lyrics] chain for \"$title\" / \"$artist\": ${order.joinToString()}")
        for (provider in order) {
            val attempt = runCatching {
                withTimeoutOrNull(15_000L) {
                    when (provider) {
                        "Musixmatch" -> Musixmatch.getLyrics(title, artist, durationSeconds, album).getOrThrow()
                        "YouLyPlus" -> YouLyPlus.getLyrics(title, artist, durationSeconds, album, videoId).getOrThrow()
                        "Paxsenix" -> {
                            if (!paxsenixReady) error("Paxsenix is not initialized")
                            Paxsenix.getLyrics(title, artist, durationSeconds, album).getOrThrow()
                        }
                        "BetterLyrics" -> BetterLyrics.getLyrics(title, artist, durationSeconds, album).getOrThrow()
                        "LrcLib" -> LrcLib.getLyrics(title, artist, durationSeconds, album).getOrThrow()
                        "Kugou" -> KuGou.getLyrics(title, artist, durationSeconds, album).getOrThrow()
                        "Unison" -> Unison.getLyrics(title, artist, durationSeconds, album, videoId).getOrThrow()
                        "SimpMusic" -> videoId
                            ?.takeIf { it.isNotBlank() }
                            ?.let { SimpMusicLyrics.getLyrics(it, durationSeconds).getOrThrow() }
                            ?: error("SimpMusic needs a video id")
                        else -> error("Unknown provider")
                    }
                }
            }
            attempt.exceptionOrNull()?.let { error ->
                println("[lyrics] $provider failed: ${error::class.simpleName}: ${error.message}")
            }
            val body = attempt.getOrNull()
            if (body.isNullOrBlank()) {
                println("[lyrics] $provider: no lyrics${if (attempt.exceptionOrNull() == null) " (timed out or empty body)" else ""}")
                continue
            }
            println("[lyrics] $provider answered ${body.length} chars")
            return@withContext ProviderResult(provider, body)
        }
        println("[lyrics] no provider answered")
        null
    }
}
