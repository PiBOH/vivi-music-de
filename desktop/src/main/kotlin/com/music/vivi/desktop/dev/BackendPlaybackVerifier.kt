package com.music.vivi.desktop.dev

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.snapshots.Snapshot
import androidx.compose.ui.ImageComposeScene
import androidx.compose.ui.unit.Density
import com.music.vivi.desktop.data.model.Song
import com.music.vivi.desktop.data.player.mpv.MpvAudioEngine
import com.music.vivi.desktop.data.youtube.YouTubeLyrics
import com.music.vivi.desktop.data.youtube.YouTubeMusicRepository
import com.music.vivi.desktop.domain.player.PlaybackState
import com.music.vivi.desktop.presentation.screens.YouTubeMusicScreen
import com.music.vivi.desktop.presentation.viewmodel.YouTubeMusicStateHolder
import com.music.vivi.desktop.presentation.viewmodel.YouTubeMusicUiState
import com.music.vivi.desktop.ui.theme.PixelPlayerTheme
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.cancel
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import java.io.File
import java.net.HttpURLConnection
import java.net.URL
import kotlin.system.exitProcess

/**
 * End-to-end proof of the backend chain, with no window and no login.
 *
 * It runs the same pieces the app runs: [YouTubeMusicRepository] searches through
 * the `innertube` module, [com.music.vivi.desktop.data.youtube.StreamResolver]
 * signs a googlevideo URL, and [MpvAudioEngine] - the engine the player screen
 * drives - decodes it. Then [YouTubeLyrics] fetches words for the same video id
 * and the cloned page is composed with the real result.
 *
 * Usage: `./gradlew :run --args="--verify-backend-playback"`. The exit code is 0
 * only when every stage passed, so a CI job can gate on it.
 */
object BackendPlaybackVerifier {

    const val FLAG = "--verify-backend-playback"
    private const val DEFAULT_QUERY = "Daft Punk Get Lucky"
    private const val REQUIRED_POSITION_MS = 6_000L

    /** Runs the verification when the flag is present; true means the CLI was handled. */
    fun handle(args: Array<String>): Boolean {
        if (!args.contains(FLAG)) return false
        val query = queryOf(args)
        val exit = runBlocking { run(query) }
        exitProcess(exit)
    }

    /**
     * `--args="--verify-backend-playback --query=Dua Lipa Levitating"` reaches the
     * JVM as three arguments, so the words after the flag are joined back together.
     */
    private fun queryOf(args: Array<String>): String {
        val index = args.indexOfFirst { it.startsWith("--query=") }
        if (index < 0) return DEFAULT_QUERY
        val tail = args.drop(index)
        val words = listOf(tail.first().substringAfter('=')) +
            tail.drop(1).takeWhile { !it.startsWith("--") }
        return words.joinToString(" ").trim().ifBlank { DEFAULT_QUERY }
    }

    private suspend fun run(query: String): Int {
        println("[verify] VIVI Music DE backend playback verification")
        println("[verify] query: \"$query\"")
        val failures = mutableListOf<String>()
        val repository = YouTubeMusicRepository()
        val lyrics = YouTubeLyrics()

        // 1. A real innertube search.
        val searchStarted = System.currentTimeMillis()
        val results = repository.search(query).getOrElse { error ->
            println("[verify] FAIL search: ${error.message}")
            return 1
        }
        val searchMs = System.currentTimeMillis() - searchStarted
        println("[verify] search returned ${results.size} songs in ${searchMs}ms")
        if (results.isEmpty()) {
            println("[verify] FAIL search returned nothing")
            return 1
        }
        results.take(5).forEach { println("[verify]   ${it.title} - ${it.artist} (id ${it.id})") }

        val song: Song = results.first()
        val videoId = song.path.removePrefix(YouTubeMusicRepository.VIDEO_SCHEME)
        println("[verify] picked: \"${song.title}\" by ${song.artist}, video id $videoId")

        // 2. Resolve the stream the same way the UI does, at play time.
        val resolvedAt = System.currentTimeMillis()
        val stream = repository.resolveStream(videoId)
        if (stream == null) {
            println("[verify] FAIL StreamResolver returned no candidate for $videoId")
            return 1
        }
        println("[verify] resolved in ${System.currentTimeMillis() - resolvedAt}ms")
        println("[verify] url host: ${URL(stream.url).host}${URL(stream.url).path}")
        println("[verify] itag: ${Regex("[?&]itag=(\\d+)").find(stream.url)?.groupValues?.get(1) ?: "n/a"}")
        println("[verify] user agent: ${stream.userAgent}")

        val headers = listOf(
            "User-Agent: ${stream.userAgent}",
            "Referer: https://www.youtube.com/"
        )
        probeHttp(stream.url, headers)

        // 3. The engine, twice: without the headers and with them.
        val engine = MpvAudioEngine()
        var completed = false
        try {
            val errorJob = CoroutineScope(SupervisorJob() + Dispatchers.Default).launch {
                engine.errorEvents.collect { println("[verify] engine error event: $it") }
            }

            val bare = playAndMeasure(engine, stream.url, emptyList(), "without headers", 10_000L)
            println("[verify] stage A (no headers): position ${bare.positionMs}ms, state ${bare.state}")

            val withHeaders = playAndMeasure(engine, stream.url, headers, "with headers", 25_000L)
            println("[verify] stage B (innertube headers): position ${withHeaders.positionMs}ms, state ${withHeaders.state}")

            if (withHeaders.positionMs < REQUIRED_POSITION_MS || withHeaders.state == PlaybackState.ERROR) {
                failures += "engine did not reach ${REQUIRED_POSITION_MS}ms of audio (reached ${withHeaders.positionMs}ms, state ${withHeaders.state})"
            }

            engine.stop()
            delay(300)
            println("[verify] stopped: state ${engine.playbackState.value}, position ${engine.currentPositionMs.value}ms")
            errorJob.cancel()
            completed = withHeaders.positionMs >= REQUIRED_POSITION_MS
        } finally {
            engine.release()
        }

        // 4. Lyrics for the same video id, through the lyricsProvider module.
        val lyricsResult = runCatching { lyrics.fetch(videoId, song.duration) }.getOrNull()
        if (lyricsResult == null) {
            println("[verify] lyrics: not found for $videoId (the provider was called and returned nothing)")
        } else {
            val syncedLines = lyricsResult.synced.orEmpty()
            val plainLines = lyricsResult.plain.orEmpty()
            val synced = syncedLines.size
            val plain = plainLines.size
            println("[verify] lyrics: ${if (synced > 0) "$synced synced lines" else "$plain plain lines"} (remote=${lyricsResult.areFromRemote})")
            val firstLine = syncedLines.firstOrNull()?.line
                ?: plainLines.firstOrNull().orEmpty()
            println("[verify] lyrics first line: ${firstLine.take(120).ifBlank { "<empty>" }}")
            if (synced == 0 && plain == 0) failures += "lyrics provider returned an empty body for $videoId"
        }

        // 5. The network and the JSON parsing must not run on the UI dispatcher.
        if (!verifyOffMainThread(repository, query)) {
            failures += "the catalogue search blocked the UI dispatcher"
        }

        // 6. The cloned page, composed with the real result.
        val rendered = verifyUiRender(repository, query)
        if (!rendered) failures += "the YouTube Music screen did not render the real result"

        println("[verify] ----------------------------------------------------------")
        if (failures.isEmpty() && completed) {
            println("[verify] RESULT: PASS")
            return 0
        }
        failures.forEach { println("[verify] FAIL $it") }
        println("[verify] RESULT: FAIL")
        return 1
    }

    /**
     * Proves the search does not run on the UI dispatcher: a probe queued on Main
     * keeps its 50 ms delay while the search takes seconds, which cannot happen if
     * the request held the thread.
     */
    private suspend fun verifyOffMainThread(repository: YouTubeMusicRepository, query: String): Boolean {
        var searchMs = 0L
        var probeMs = 0L
        var songs = 0
        withContext(Dispatchers.Main) {
            val searchDone = CompletableDeferred<Long>()
            val started = System.currentTimeMillis()
            launch {
                songs = repository.search(query).getOrDefault(emptyList()).size
                searchDone.complete(System.currentTimeMillis())
            }
            val probeStarted = System.currentTimeMillis()
            delay(50)
            probeMs = System.currentTimeMillis() - probeStarted
            searchMs = searchDone.await() - started
        }
        val offMain = probeMs < 500 && searchMs > probeMs
        println("[verify] main dispatcher probe: ${probeMs}ms while the search took ${searchMs}ms (${songs} songs) -> ${if (offMain) "off main thread" else "SEARCH BLOCKED MAIN"}")
        return offMain
    }

    /**
     * The page plus a counter in the same restartable scope that reads its state.
     *
     * The lambda handed to [ImageComposeScene] is not a restartable composable, so a
     * counter there reports one run no matter what the page does. A function is, and
     * it is what a real screen looks like to the recomposer.
     */
    @Composable
    private fun VerifiedPage(holder: YouTubeMusicStateHolder, state: YouTubeMusicUiState, onCompose: () -> Unit) {
        onCompose()
        PixelPlayerTheme(darkTheme = true) {
            YouTubeMusicScreen(
                stateHolder = holder,
                currentSong = state.songs.firstOrNull(),
                isPlaying = false,
                onPlaySongs = { _, _ -> },
                onPlayNext = {},
                onAddToQueue = {}
            )
        }
    }

    private data class PlaybackMeasurement(val positionMs: Long, val state: PlaybackState)

    /** Plays [url] and follows the playhead until it passes [targetMs], errors or times out. */
    private suspend fun playAndMeasure(
        engine: MpvAudioEngine,
        url: String,
        headers: List<String>,
        label: String,
        timeoutMs: Long
    ): PlaybackMeasurement {
        println("[verify] play $label: headers=${headers.size}")
        engine.play(url, headers)
        val started = System.currentTimeMillis()
        var maxPosition = 0L
        var lastState = PlaybackState.IDLE
        var lastReportedSecond = -1L
        val deadline = started + timeoutMs
        while (System.currentTimeMillis() < deadline) {
            delay(200)
            val position = engine.currentPositionMs.value
            if (position > maxPosition) maxPosition = position
            val state = engine.playbackState.value
            if (state != lastState) {
                println("[verify]   state ${lastState} -> $state at ${position}ms")
                lastState = state
            }
            val second = position / 1000
            if (second != lastReportedSecond) {
                lastReportedSecond = second
                println("[verify]   playhead ${position}ms (${second}s)")
            }
            if (state == PlaybackState.ERROR || state == PlaybackState.ENDED) break
            if (maxPosition >= REQUIRED_POSITION_MS) break
        }
        if (maxPosition < REQUIRED_POSITION_MS) {
            println("[verify]   gave up after ${System.currentTimeMillis() - started}ms")
        }
        return PlaybackMeasurement(maxPosition, engine.playbackState.value)
    }

    /**
     * Shows what the server answers for the same URL with the different header
     * sets, so the "does innertube need the handoff" question has a number and not
     * an opinion. Range 0-1023 keeps it to one read.
     */
    private fun probeHttp(url: String, headers: List<String>) {
        val variants = listOf(
            "no headers" to emptyList(),
            "innertube headers" to headers
        )
        variants.forEach { (label, set) ->
            val code = runCatching {
                val connection = URL(url).openConnection() as HttpURLConnection
                connection.connectTimeout = 15_000
                connectionReadTimeoutOrDefault(connection)
                set.forEach { header ->
                    val name = header.substringBefore(':').trim()
                    val value = header.substringAfter(':').trim()
                    connection.setRequestProperty(name, value)
                }
                connection.setRequestProperty("Range", "bytes=0-1023")
                try {
                    connection.responseCode
                } finally {
                    connection.disconnect()
                }
            }.getOrElse { it.message ?: "request failed" }
            println("[verify] http probe ($label): $code")
        }
    }

    private fun connectionReadTimeoutOrDefault(connection: HttpURLConnection) {
        connection.readTimeout = 20_000
    }

    /**
     * Renders the real screen with the real result and counts how many times its
     * content lambda runs: one execution for three identical frames is a page that
     * reflects state changes only, more is a recomposition storm.
     */
    private fun verifyUiRender(repository: YouTubeMusicRepository, query: String): Boolean {
        val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
        val holder = YouTubeMusicStateHolder(repository, scope)
        holder.onQueryChange(query)
        holder.submit()
        val loaded = runBlocking {
            withTimeoutOrNull(45_000) {
                while (holder.state.value.isLoading) delay(100)
                true
            } ?: false
        }
        val state = holder.state.value
        println("[verify] ui state: songs=${state.songs.size} loading=${state.isLoading} error=${state.error ?: "none"}")
        if (!loaded) {
            println("[verify] ui: the search never finished")
            return false
        }
        if (state.error != null) {
            println("[verify] ui: the page would show the error state (${state.error})")
            return false
        }

        var bodyRuns = 0
        // The page is measured through a snapshot state that mirrors the holder, fed
        // by a collector this harness owns: the scene's own effect scope does not
        // deliver flow emissions when the frame clock is driven by hand, and a
        // counter that can never move is not a measurement.
        val liveMirror = androidx.compose.runtime.mutableStateOf(holder.state.value)
        val mirrorScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
        mirrorScope.launch {
            holder.state.collect { liveMirror.value = it }
        }
        val scene = ImageComposeScene(
            width = 1280,
            height = 900,
            density = Density(1f),
            coroutineContext = Dispatchers.Main
        ) {
            VerifiedPage(holder, liveMirror.value) { bodyRuns++ }
        }

        // A frame in a hand-driven scene has no clock to publish snapshot writes, so
        // the apply notifications are sent the way a real frame would.
        fun settleAndRender(settleMs: Long = 150) {
            Thread.sleep(settleMs)
            Snapshot.sendApplyNotifications()
            scene.render()
            Snapshot.sendApplyNotifications()
            scene.render()
        }

        try {
            repeat(3) { scene.render() }
            settleAndRender()
            val afterIdentical = bodyRuns
            println("[verify] ui render: 3 identical frames, page executions=$afterIdentical")

            val sameValue = holder.state.value.query
            holder.onQueryChange(sameValue)
            settleAndRender()
            val afterSameValue = bodyRuns - afterIdentical
            println("[verify] ui render: same-value update added $afterSameValue executions")

            holder.onQueryChange("$sameValue ")
            settleAndRender()
            val afterChange = bodyRuns - afterIdentical - afterSameValue
            println("[verify] ui render: real query change added $afterChange executions")
            println("[verify] ui mirror query: \"${liveMirror.value.query}\" (holder: \"${holder.state.value.query}\")")

            val image = scene.render()
            val png = runCatching { image.encodeToData()?.bytes }.getOrNull()
            val out = File("build/verify-youtube-screen.png")
            if (png != null) {
                out.parentFile?.mkdirs()
                runCatching { out.writeBytes(png) }
                    .onSuccess { println("[verify] ui screenshot: ${out.absolutePath}") }
            }
            val steady = afterIdentical == 1 && afterSameValue == 0 && afterChange == 1
            println("[verify] ui render: ${if (steady) "one composition per state change, none per frame" else "unexpected recomposition pattern"}")
            return state.songs.isNotEmpty() && steady
        } catch (t: Throwable) {
            println("[verify] ui render FAILED: ${t::class.simpleName}: ${t.message}")
            return false
        } finally {
            runCatching { scene.close() }
            mirrorScope.cancel()
            scope.coroutineContext[kotlinx.coroutines.Job]?.cancel()
        }
    }

}
