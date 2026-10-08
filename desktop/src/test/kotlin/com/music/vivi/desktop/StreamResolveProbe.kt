package com.music.vivi.desktop

import com.music.innertube.YouTube
import com.music.innertube.YouTubeExtractor
import com.music.innertube.models.YouTubeClient
import com.music.innertube.models.YouTubeLocale
import com.music.vivi.desktop.player.StreamResolver
import kotlinx.coroutines.runBlocking
import okhttp3.OkHttpClient
import okhttp3.Request
import java.util.concurrent.TimeUnit

/**
 * Measures WHY a track stops with "giving up after 3 attempts: HTTP 403
 * downloading audio", run with
 *
 *     ./gradlew :desktop:streamResolveProbe [--args="videoId videoId"]
 *
 * The playback log shows the shape of the failure but not its cause: it says
 * "stream ready ... (network)" and then fails on the URL, and every retry
 * re-resolves through the same client chain, so it fails the same way three
 * times. What was missing is WHICH client's URL was used and whether the other
 * candidates would have played.
 *
 * Phase 1 runs the real resolver for a set of video ids (the ones from a failing
 * log by default) and, for every candidate URL it returns, prints the client the
 * URL came from (`c=` in the URL) and the HTTP status of a real ranged GET of the
 * first 256 KB sent with that candidate's own User-Agent, which is what the
 * downloader does. A 2xx is playable; 403 is the reported failure.
 *
 * Phase 2 signs the same track with `WEB_CREATOR` and fetches it the same way, as
 * the control: that client is PoToken-only, and on a desktop the URL it signs is
 * expected to answer 403 no matter what. That is what made the old chain hand the
 * player a dead URL instead of falling through to the guest-identity retry.
 */
object StreamResolveProbe {

    /** The ids that failed in `~/.vivimusic/logs/20261007-205734/playback.log`,
     *  plus one track that played fine in the same session, as a control. */
    private val defaultIds = listOf(
        "GVwZmVfShsM" to "Vamos a la Playa",
        "cbeZLwOO6ag" to "Ban this guy",
        "b2VwSe6iIpQ" to "Banana Joe (feat. Bud Spencer)",
        "x8-Wpxu7hag" to "MIGUEL Phonk",
        "5NV6Rdv1a3I" to "Get Lucky (played fine, cached)",
    )

    private val http = OkHttpClient.Builder()
        .connectTimeout(20, TimeUnit.SECONDS)
        .readTimeout(20, TimeUnit.SECONDS)
        .callTimeout(30, TimeUnit.SECONDS)
        .build()

    @JvmStatic
    fun main(args: Array<String>) {
        val ids = if (args.isNotEmpty()) args.map { it to it } else defaultIds

        YouTube.locale = YouTubeLocale(gl = "IT", hl = "it")
        runBlocking { GuestSession.ensure() }
        println("guest visitorData: ${if (YouTube.visitorData.isNullOrBlank()) "MISSING" else "present"}")
        println()

        var chainPlayable = false
        var chain403 = false
        var noCandidate = false
        for ((id, title) in ids) {
            println("== $title [$id]")
            val streams = runBlocking {
                runCatching { StreamResolver.resolveAacStream(id) }
                    .onFailure { println("   resolve threw: ${it.javaClass.simpleName}: ${it.message}") }
                    .getOrDefault(emptyList())
            }
            if (streams.isEmpty()) {
                println("   NO CANDIDATE URL")
                noCandidate = true
                continue
            }
            for ((index, stream) in streams.withIndex()) {
                val status = probe(stream.url, stream.userAgent)
                if (status in 200..299) chainPlayable = true
                if (status == 403) chain403 = true
                println(
                    "   #${index + 1} client=${clientOf(stream.url)} http=$status " +
                        "ua=${stream.userAgent.take(55)} dur=${stream.durationMs ?: "-"}"
                )
            }
            println()
        }

        // ---- Phase 2: the control, a PoToken-only web client ----
        val (controlId, controlTitle) = ids.first()
        println("== control: WEB_CREATOR signs the same track ($controlTitle)")
        val web = runBlocking { webClientCandidate(controlId) }
        if (web == null) {
            println("   WEB_CREATOR returned no playable format (playability refused)")
        } else {
            val status = probe(web.first, web.second)
            println("   client=WEB_CREATOR http=$status ua=${web.second.take(55)}")
            if (status == 403) {
                println("   the web URL is refused, which is what a chain ending in a web client plays")
            }
        }
        println()

        println(
            when {
                chainPlayable ->
                    "VERDICT: the PoToken-free chain returns playable candidates (2xx), so this failure is the " +
                        "chain being refused for every free identity at that moment, and the right answer is the " +
                        "guest-identity retry, never a web URL"
                noCandidate && !chain403 ->
                    "VERDICT: the chain returned no candidate at all, which is the guest-rotation path"
                else ->
                    "VERDICT: the chain returned candidates but none was playable, see the statuses above"
            }
        )
    }

    /**
     * A AAC URL signed by the PoToken-only web client, extracted and n-param
     * deobfuscated exactly like `StreamResolver` does it for web clients, so the
     * control measures the real thing and not a strawman.
     */
    private fun webClientCandidate(id: String): Pair<String, String>? = runBlocking {
        val response = YouTube.player(id, null, YouTubeClient.WEB_CREATOR, null, null).getOrNull()
            ?: return@runBlocking null
        if (response.playabilityStatus.status != "OK") return@runBlocking null
        val format = response.streamingData?.adaptiveFormats
            ?.firstOrNull { it.isAudio && it.mimeType.contains("mp4a.40.2") }
            ?: return@runBlocking null
        val raw = format.url ?: format.signatureCipher ?: format.cipher ?: return@runBlocking null
        val url = if (raw.startsWith("http")) raw else YouTubeExtractor.decryptUrl(raw)
        if (url.isNullOrBlank()) return@runBlocking null
        val deobfuscated = runCatching { YouTubeExtractor.deobfuscateUrlNParam(url) }.getOrDefault(url)
        deobfuscated to YouTubeClient.WEB_CREATOR.userAgent
    }

    /** A ranged GET of the first 256 KB, exactly what the downloader does first. */
    private fun probe(url: String, userAgent: String): Int = runCatching {
        val request = Request.Builder()
            .url(url)
            .header("User-Agent", userAgent)
            .header("Accept", "*/*")
            .header("Accept-Encoding", "identity")
            .header("Range", "bytes=0-262143")
            .build()
        http.newCall(request).execute().use { response ->
            // Read a little of the body so a stalled/blocked response is real.
            response.body?.byteStream()?.read(ByteArray(16 * 1024))
            response.code
        }
    }.getOrElse { -1 }

    /** The `c=` origin of a googlevideo URL, which names the client that signed it. */
    private fun clientOf(url: String): String = Regex("[?&]c=([A-Z_]+)").find(url)?.groupValues?.get(1) ?: "?"
}
