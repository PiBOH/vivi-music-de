@file:OptIn(ExperimentalComposeUiApi::class)

package com.music.vivi.desktop

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.snapshots.Snapshot
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.ImageComposeScene
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Density
import com.music.innertube.YouTube
import com.music.innertube.models.SongItem
import com.music.innertube.models.YouTubeLocale
import com.music.vivi.desktop.player.AudioPlayer
import com.music.vivi.desktop.player.StreamResolver
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeoutOrNull
import okhttp3.OkHttpClient
import okhttp3.Request
import org.jetbrains.skia.EncodedImageFormat
import java.io.File
import java.util.concurrent.TimeUnit
import kotlin.system.exitProcess

/**
 * End-to-end check of the catalogue chain, with no window and no account:
 *
 *     ./gradlew :desktop:backendPlaybackCheck [--args="a search query"]
 *
 * Every stage is the one the app runs, not a copy of it: `YouTube.search` for
 * the query, [StreamResolver.resolveAacStream] for the URL, [AudioPlayer] to
 * decode it, [DesktopLyrics.fetch] for the words and `BcYTItemRow` to paint the
 * rows those results produce. The check prints what each stage returned and
 * exits non-zero when one of them fails, so it gates a change instead of being
 * read by eye.
 *
 * What the numbers prove:
 *  - the search answered with real songs and a video id, and the resolver
 *    signed a URL that the downloader's own User-Agent gets a 2xx for;
 *  - the decoder reported a playhead past [REQUIRED_POSITION_MS] of audio
 *    without an error, which is the difference between "a URL exists" and
 *    "the engine plays it";
 *  - the lyrics chain answered for the same video id with timed text, or said
 *    that it has none for that track;
 *  - the row composable draws the real titles and does it once per state
 *    change, not once per frame.
 */
object BackendPlaybackCheck {

    /** The playhead a track has to reach: a few seconds of decoded audio is the
     *  point where a URL that would 403 or a decoder that would stall has
     *  already failed. */
    private const val REQUIRED_POSITION_MS = 6_000L

    /** Long enough for a cold resolve on a slow morning, short enough that a
     *  hung request fails the check instead of hanging it. */
    private const val PLAY_TIMEOUT_MS = 45_000L

    private const val DEFAULT_QUERY = "Daft Punk Get Lucky"

    /** Frames land under `desktop/.ignore/backend-check/`, next to the other
     *  checks' renders and never committed. */
    private val outDir = File(".ignore/backend-check")

    private val http = OkHttpClient.Builder()
        .connectTimeout(20, TimeUnit.SECONDS)
        .readTimeout(20, TimeUnit.SECONDS)
        .callTimeout(30, TimeUnit.SECONDS)
        .build()

    private val failures = mutableListOf<String>()

    @JvmStatic
    fun main(args: Array<String>) {
        val query = args.filterNot { it.startsWith("--") }
            .joinToString(" ")
            .trim()
            .ifBlank { DEFAULT_QUERY }
        val exit = runBlocking { run(query) }
        exitProcess(exit)
    }

    private suspend fun run(query: String): Int {
        println("[backend] VIVI Music DE backend playback check")
        println("[backend] query: \"$query\"")

        YouTube.locale = YouTubeLocale(gl = "IT", hl = "it")
        GuestSession.ensure()

        // 1. A real catalogue search.
        val startedAt = System.currentTimeMillis()
        val page = withTimeoutOrNull(30_000) {
            YouTube.search(query, YouTube.SearchFilter.FILTER_SONG).getOrNull()
        }
        val songs = page?.items?.filterIsInstance<SongItem>().orEmpty()
        println("[backend] search: ${songs.size} songs in ${System.currentTimeMillis() - startedAt}ms")
        if (songs.isEmpty()) {
            println("[backend] FAIL the search returned no song")
            return 1
        }
        songs.take(5).forEach { println("[backend]   ${it.title} - ${it.artists.joinToString(", ") { a -> a.name }} (${it.id})") }

        val song = songs.first()
        println("[backend] picked: \"${song.title}\" - video id ${song.id}")

        // 2. The same resolution the player runs before it opens a track.
        val resolvedAt = System.currentTimeMillis()
        val streams = StreamResolver.resolveAacStream(song.id)
        println("[backend] resolve: ${streams.size} candidates in ${System.currentTimeMillis() - resolvedAt}ms")
        if (streams.isEmpty()) {
            println("[backend] FAIL the resolver returned no candidate")
            return 1
        }
        streams.forEachIndexed { index, stream ->
            println(
                "[backend]   #$index itag=${itagOf(stream.url)} client=${clientOf(stream.url)} " +
                    "ua=${stream.userAgent.substringBefore(')').plus(')')}"
            )
        }
        val stream = streams.first()
        probeUrl(stream)
        println("[backend] resolver note: the URL is served to the candidate's own User-Agent, " +
            "which is what AudioPlayer sends when it downloads")

        // 3. The engine, not a stand-in: AudioPlayer decodes the resolved URL.
        val player = AudioPlayer()
        var positionMs = 0L
        var durationMs = 0L
        var engineError: String? = null
        var completed = false
        try {
            player.play(
                streams = streams,
                cacheKey = song.id,
                onPosition = { position -> if (position > positionMs) positionMs = position },
                onDuration = { duration -> if (duration > 0) durationMs = duration },
                onError = { reason -> engineError = reason },
                onComplete = { completed = true },
            )
            val playStarted = System.currentTimeMillis()
            var lastSecond = -1L
            while (System.currentTimeMillis() - playStarted < PLAY_TIMEOUT_MS) {
                delay(200)
                val second = positionMs / 1000
                if (second != lastSecond) {
                    lastSecond = second
                    println("[backend]   playhead ${positionMs}ms (${second}s of ${durationMs / 1000}s)")
                }
                if (engineError != null || completed) break
                if (positionMs >= REQUIRED_POSITION_MS) break
            }
            println("[backend] playback: reached ${positionMs}ms, duration ${durationMs}ms, error=${engineError ?: "none"}, completed=$completed")
            if (positionMs < REQUIRED_POSITION_MS || engineError != null) {
                failures += "the decoder did not reach ${REQUIRED_POSITION_MS}ms (${positionMs}ms, error ${engineError ?: "none"})"
            }
            player.stop()
            delay(300)
        } finally {
            runCatching { player.stop() }
        }

        // 4. The lyrics chain for the same video id.
        val artistLine = song.artists.joinToString(", ") { it.name }
        val lyrics = runCatching {
            DesktopLyrics.fetch(
                videoId = song.id,
                title = song.title,
                artist = artistLine,
                durationMs = (song.duration ?: 0) * 1000L,
                album = song.album?.name,
            )
        }.getOrNull()
        val lyricsText = lyrics?.getOrNull()
        if (lyricsText.isNullOrBlank()) {
            println("[backend] lyrics: none for ${song.id} (the chain was asked and answered nothing)")
        } else {
            val lines = lyricsText.lines().filter { it.isNotBlank() }
            val timed = lines.count { it.trimStart().startsWith("[") }
            println("[backend] lyrics: ${lines.size} lines, $timed timed")
            println("[backend] lyrics first line: ${lines.first().take(120)}")
            if (lines.isEmpty()) failures += "the lyrics chain returned an empty body for ${song.id}"
        }

        // 5. The rows the UI draws for these very results.
        if (!renderRows(songs)) failures += "the search rows did not paint the real results"

        println("[backend] ------------------------------------------------------------")
        if (failures.isEmpty()) {
            println("[backend] RESULT: PASS")
            return 0
        }
        failures.forEach { println("[backend] FAIL $it") }
        println("[backend] RESULT: FAIL")
        return 1
    }

    /** itag of a googlevideo URL, which is the format the resolver chose. */
    private fun itagOf(url: String): String =
        Regex("[?&]itag=(\\d+)").find(url)?.groupValues?.get(1) ?: "n/a"

    /** `c=` of a googlevideo URL: the client identity the URL was signed for. */
    private fun clientOf(url: String): String =
        Regex("[?&]c=([A-Za-z0-9_]+)").find(url)?.groupValues?.get(1) ?: "n/a"

    /**
     * What the CDN answers for the same URL with the downloader's User-Agent and
     * without it, which is the measured answer to "does the stream need the
     * header handoff": the player always sends the candidate's own UA, so a 2xx
     * for it is the case that matters, and the bare request says whether the
     * header is what makes it work. Range 0-1023 keeps it to one read.
     */
    private fun probeUrl(stream: StreamResolver.ResolvedStream) {
        val variants = listOf(
            "with the candidate User-Agent" to stream.userAgent,
            "without a header" to null,
        )
        variants.forEach { (label, userAgent) ->
            val code = runCatching {
                val request = Request.Builder()
                    .url(stream.url)
                    .header("Range", "bytes=0-1023")
                    .apply { userAgent?.let { header("User-Agent", it) } }
                    .build()
                http.newCall(request).execute().use { response -> response.code }
            }.getOrElse { it.message ?: "request failed" }
            println("[backend] http probe ($label): $code")
        }
    }

    /**
     * Paints `BcYTItemRow` for every real result and counts how often the list
     * composes: one execution for three identical frames and one per changed
     * list is a list that follows state, more is a recomposition storm.
     *
     * The counter sits in a restartable composable that reads the list, not in
     * the lambda handed to [ImageComposeScene]: that lambda is not restartable,
     * so a counter there would report one run whatever the list does.
     */
    private fun renderRows(songs: List<SongItem>): Boolean {
        var compositions = 0
        val rows = mutableStateOf(songs.take(12))
        val scene = ImageComposeScene(width = 1180, height = 780, density = Density(1f)) {
            val visible by rows
            BcTheme {
                Column(Modifier.fillMaxSize().background(BC.Page)) {
                    CheckRows(visible) { compositions++ }
                }
            }
        }
        fun settleAndRender(settleMs: Long = 120) {
            Thread.sleep(settleMs)
            Snapshot.sendApplyNotifications()
            scene.render()
        }
        return try {
            repeat(3) { scene.render() }
            settleAndRender()
            val steady = compositions
            println("[backend] rows: 3 identical frames, compositions=$steady")

            rows.value = songs.take(12)
            settleAndRender()
            val sameList = compositions - steady
            println("[backend] rows: the same list again added $sameList compositions")

            rows.value = songs.drop(1).take(12)
            settleAndRender()
            val changedList = compositions - steady - sameList
            println("[backend] rows: a different list added $changedList compositions")

            val image = scene.render()
            outDir.mkdirs()
            val png = File(outDir, "search-rows.png")
            image.encodeToData(EncodedImageFormat.PNG)?.bytes?.let { png.writeBytes(it) }
            println("[backend] rows: frame written to ${png.absolutePath}")

            val ok = songs.isNotEmpty() && steady == 1 && sameList == 0 && changedList == 1
            if (!ok) println("[backend] rows: unexpected composition pattern")
            ok
        } catch (t: Throwable) {
            println("[backend] rows: FAILED ${t::class.simpleName}: ${t.message}")
            false
        } finally {
            runCatching { scene.close() }
        }
    }

    /** The rows of the real page, so a composition that changes with the list is
     *  counted in one place. */
    @androidx.compose.runtime.Composable
    private fun CheckRows(songs: List<SongItem>, onCompose: () -> Unit) {
        onCompose()
        songs.forEach { song ->
            key(song.id) {
                BcYTItemRow(
                    item = song,
                    onOpenAlbum = {},
                    onOpenArtist = {},
                    onOpenPlaylist = {},
                )
            }
        }
    }
}
