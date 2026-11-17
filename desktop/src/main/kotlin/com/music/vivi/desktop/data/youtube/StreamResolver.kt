package com.music.vivi.desktop.data.youtube

import com.music.innertube.NewPipeExtractor
import com.music.innertube.YouTube
import com.music.innertube.YouTubeExtractor
import com.music.innertube.models.YouTubeClient
import com.music.innertube.models.response.PlayerResponse
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import java.util.concurrent.ConcurrentHashMap

/**
 * Resolves a playable googlevideo URL for a YouTube video id.
 *
 * The URL is handed straight to libmpv, so unlike the Java-decoder build this one
 * keeps the Opus/WebM candidates too: mpv decodes them natively. AAC (itag 140/141)
 * still comes first because it starts fastest and is the format the client chain
 * returns most reliably.
 *
 * Two sources are tried in order, and both are needed:
 *  1. **NewPipe** - decrypts the signature cipher and returns ready URLs.
 *  2. **The PoToken-free client chain** - reached when NewPipe is bot-blocked. A
 *     web client (`useWebPoTokens`) signs a URL that googlevideo only serves with a
 *     PoToken, and the JVM has no WebView to mint one, so those clients are
 *     filtered out: their URL is not "less reliable", it is guaranteed 403.
 */
object StreamResolver {

    data class ResolvedStream(val url: String, val userAgent: String, val durationMs: Long? = null)

    /** Preferred itags, best first: AAC-LC 256k, AAC-LC 128k, then Opus (mpv handles all three). */
    private val PREFERRED_ITAGS = listOf(141, 140, 251, 250)

    private val CLIENT_CHAIN: List<YouTubeClient> = listOf(
        YouTubeClient.VISIONOS,
        YouTubeClient.ANDROID_VR_1_65_10,
        YouTubeClient.IPADOS,
        YouTubeClient.IOS,
        YouTubeClient.ANDROID_VR_1_43_32,
        YouTubeClient.ANDROID_CREATOR,
        YouTubeClient.MWEB,
        YouTubeClient.TVHTML5_SIMPLY_EMBEDDED_PLAYER,
    ).filterNot { it.useWebPoTokens }

    private class Cached(val streams: List<ResolvedStream>, val expiresAt: Long)

    private val cache = ConcurrentHashMap<String, Cached>()
    private const val CACHE_MAX_ENTRIES = 32
    private const val CACHE_TTL_MS = 10 * 60_000L

    /** Bumped on invalidation so a slow resolution cannot store a stale URL afterwards. */
    private val generations = ConcurrentHashMap<String, Long>()

    private fun generation(videoId: String): Long = generations[videoId] ?: 0L

    /** Drops a cached resolution (a googlevideo URL is single-use and expires). */
    fun invalidate(videoId: String) {
        cache.remove(videoId)
        generations[videoId] = generation(videoId) + 1
    }

    /**
     * Returns an ordered list of candidate stream URLs. The player tries them in
     * order, so a bot-blocked URL from one source never prevents playback when
     * another source works.
     */
    suspend fun resolve(videoId: String): List<ResolvedStream> {
        val now = System.currentTimeMillis()
        cache[videoId]?.let { hit ->
            if (hit.expiresAt > now && hit.streams.isNotEmpty()) return hit.streams
            cache.remove(videoId)
        }

        GuestSession.ensure()
        var resolution = resolveOnce(videoId)
        // Bot detection: no candidate at all usually means the guest identity was
        // flagged, so rotate it and try once more.
        if (resolution.streams.isEmpty()) {
            GuestSession.rotate()
            resolution = resolveOnce(videoId)
        }
        var attempts = 0
        while (resolution.streams.isEmpty() && attempts < 2) {
            delay(750L)
            resolution = resolveOnce(videoId)
            attempts++
        }

        if (resolution.streams.isNotEmpty() && generation(videoId) == resolution.generation) {
            cache[videoId] = Cached(resolution.streams, System.currentTimeMillis() + CACHE_TTL_MS)
            while (cache.size > CACHE_MAX_ENTRIES) {
                val oldest = cache.entries.minByOrNull { it.value.expiresAt }?.key ?: break
                cache.remove(oldest)
            }
        }
        return resolution.streams
    }

    private data class Resolution(val streams: List<ResolvedStream>, val generation: Long)

    private suspend fun resolveOnce(videoId: String): Resolution {
        val newPipeStreams = withContext(Dispatchers.IO) {
            runCatching { YouTube.getNewPipeStreamUrls(videoId) }.getOrDefault(emptyList())
        }
        // The NewPipe list is the decrypted one, so an audio-only format from it is
        // the best answer there is. A muxed one (itag 18 and friends) carries the
        // video track too and a third of the bitrate, so it is kept only as a
        // fallback and the client chain is asked for an audio-only URL first.
        val newPipeAudio = PREFERRED_ITAGS.firstNotNullOfOrNull { tag ->
            newPipeStreams.firstOrNull { it.first == tag }?.second
        }
        if (!newPipeAudio.isNullOrBlank()) {
            YtLog.log("resolve", "$videoId via NewPipe")
            return Resolution(
                listOf(ResolvedStream(newPipeAudio, YouTubeClient.USER_AGENT_WEB)),
                generation(videoId)
            )
        }
        val newPipeFallback = newPipeStreams.firstOrNull()?.second?.takeIf { it.isNotBlank() }

        val collected = mutableListOf<ResolvedStream>()
        var signatureTimestamp: Int? = null
        var signatureFetched = false

        for (ytClient in CLIENT_CHAIN) {
            if (ytClient.useSignatureTimestamp && !signatureFetched) {
                signatureTimestamp = withContext(Dispatchers.IO) {
                    runCatching { NewPipeExtractor.getSignatureTimestamp(videoId).getOrNull() }.getOrNull()
                }
                signatureFetched = true
            }
            val ts = if (ytClient.useSignatureTimestamp) signatureTimestamp else null

            val response = YouTube.player(videoId, null, ytClient, ts, null).getOrNull() ?: continue
            if (response.playabilityStatus.status != "OK") {
                YtLog.log(
                    "resolve",
                    "$videoId client ${ytClient.clientName}: ${response.playabilityStatus.status}" +
                        response.playabilityStatus.reason?.let { " ($it)" }.orEmpty()
                )
                continue
            }

            val url = resolveFromResponse(response, ytClient) ?: continue
            val durationMs = response.videoDetails?.lengthSeconds?.toDoubleOrNull()?.times(1000)?.toLong()
            if (collected.any { it.url == url }) continue
            collected += ResolvedStream(url, ytClient.userAgent, durationMs)
            YtLog.log("resolve", "$videoId client ${ytClient.clientName}: ok")
            // Two independent candidates are enough for the fall-through.
            if (collected.size >= 2) break
        }

        if (newPipeFallback != null && collected.none { it.url == newPipeFallback }) {
            if (collected.isEmpty()) YtLog.log("resolve", "$videoId via NewPipe (muxed fallback)")
            collected += ResolvedStream(newPipeFallback, YouTubeClient.USER_AGENT_WEB)
        }

        return Resolution(collected, generation(videoId))
    }

    /** Picks the best audio format from a successful player response and resolves its URL. */
    private fun resolveFromResponse(response: PlayerResponse, ytClient: YouTubeClient): String? {
        val adaptive = response.streamingData?.adaptiveFormats ?: return null
        val audio = adaptive.filter { it.isAudio && it.isOriginal }
        val format = PREFERRED_ITAGS.firstNotNullOfOrNull { tag -> audio.firstOrNull { it.itag == tag } }
            ?: audio.firstOrNull()
            ?: return null

        val raw = when {
            !format.url.isNullOrEmpty() -> format.url
            else -> format.signatureCipher ?: format.cipher
        } ?: return null

        val url = if (raw.startsWith("http")) raw else YouTubeExtractor.decryptUrl(raw)
        if (url.isNullOrBlank()) return null

        // The n-parameter transform belongs to the web player: applying it to an
        // Android/iOS/VisionOS URL corrupts their `n` param and googlevideo answers 403.
        return if (ytClient.useWebPoTokens) {
            runCatching { YouTubeExtractor.deobfuscateUrlNParam(url) }.getOrDefault(url)
        } else {
            url
        }
    }
}
