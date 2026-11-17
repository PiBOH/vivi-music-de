package com.music.vivi.desktop.dev

import androidx.compose.runtime.Composable
import androidx.compose.runtime.snapshots.Snapshot
import androidx.compose.ui.ImageComposeScene
import androidx.compose.ui.unit.Density
import com.music.vivi.desktop.data.model.Lyrics
import com.music.vivi.desktop.data.player.mpv.MpvAudioEngine
import com.music.vivi.desktop.di.appModules
import com.music.vivi.desktop.domain.player.AudioEngine
import com.music.vivi.desktop.domain.player.PlaybackState
import com.music.vivi.desktop.presentation.navigation.DesktopNavTarget
import com.music.vivi.desktop.presentation.navigation.DesktopNavigator
import com.music.vivi.desktop.presentation.screens.ExpandedPlayerScreen
import com.music.vivi.desktop.presentation.screens.ExpandedTab
import com.music.vivi.desktop.presentation.screens.MainAppScaffold
import com.music.vivi.desktop.presentation.viewmodel.PlayerViewModel
import com.music.vivi.desktop.ui.theme.PixelPlayerTheme
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeoutOrNull
import org.koin.core.context.GlobalContext
import org.koin.core.context.startKoin
import java.io.File
import kotlin.system.exitProcess

/**
 * The app its own way: no hand-built repository chain.
 *
 * This check starts the same Koin graph the window starts, asks the search holder
 * the search screen binds to, calls the very lambda the screen passes as
 * `onPlaySongs`, and reads the player state and the lyrics the screens read. If a
 * view model were still a stub, or the screen still built its own fake data, this
 * would fail: every value printed here comes from the objects the window composes.
 *
 * Usage: `VIVIMusic.exe --verify-app-flow [--query=...]`. Exit code 0 means the
 * whole flow passed.
 */
object RealAppFlowCheck {

    const val FLAG = "--verify-app-flow"
    private const val DEFAULT_QUERY = "Daft Punk Get Lucky"
    private const val REQUIRED_POSITION_MS = 5_000L

    fun handle(args: Array<String>): Boolean {
        if (!args.contains(FLAG)) return false
        val exit = runBlocking { run(queryOf(args)) }
        exitProcess(exit)
    }

    private fun queryOf(args: Array<String>): String {
        val index = args.indexOfFirst { it.startsWith("--query=") }
        if (index < 0) return DEFAULT_QUERY
        val tail = args.drop(index)
        val words = listOf(tail.first().substringAfter('=')) +
            tail.drop(1).takeWhile { !it.startsWith("--") }
        return words.joinToString(" ").trim().ifBlank { DEFAULT_QUERY }
    }

    private suspend fun run(query: String): Int {
        println("[app] VIVI Music DE real app flow")
        val failures = mutableListOf<String>()

        // The window's own graph, exactly as Main.kt builds it.
        val koin = GlobalContext.getOrNull()?.get() ?: startKoin { modules(appModules) }.koin
        val viewModel = koin.get<PlayerViewModel>()
        val engine = koin.get<AudioEngine>()
        val mpv = engine as? MpvAudioEngine
        println("[app] graph: ${viewModel::class.simpleName}, engine ${engine::class.simpleName}")

        // 1. The search field of the cloned screen. `YouTubeMusicScreen` binds
        // `stateHolder.onQueryChange` to the field and `submit()` to Enter, so this is
        // the same two calls a typist makes.
        val search = viewModel.youtubeMusicStateHolder
        println("[app] query: \"$query\"")
        val searchStarted = System.currentTimeMillis()
        search.onQueryChange(query)
        search.submit()
        val songs = withTimeoutOrNull(60_000) {
            while (search.state.value.isLoading || search.state.value.songs.isEmpty()) {
                if (search.state.value.error != null) break
                delay(120)
            }
            search.state.value.songs
        }.orEmpty()
        val state = search.state.value
        val searchMs = System.currentTimeMillis() - searchStarted
        println(
            "[app] search state: ${songs.size} songs in ${searchMs}ms, " +
                "loading=${state.isLoading}, error=${state.error ?: "none"}"
        )
        if (songs.isEmpty()) {
            println("[app] FAIL the screen's search state never received results")
            return 1
        }
        val picked = songs.first()
        println("[app] first result in the screen state: \"${picked.title}\" by ${picked.displayArtist}")

        // 2. The selection, through the callback the screen passes as `onPlaySongs`.
        viewModel.playSongs(songs, 0)
        println("[app] playSongs(songs, 0) called on ${viewModel::class.simpleName}")

        val playing = withTimeoutOrNull(45_000) {
            while (true) {
                val position = engine.currentPositionMs.value
                if (engine.playbackState.value == PlaybackState.PLAYING && position >= REQUIRED_POSITION_MS) {
                    return@withTimeoutOrNull true
                }
                if (engine.playbackState.value == PlaybackState.ERROR) return@withTimeoutOrNull false
                delay(250)
            }
            @Suppress("UNREACHABLE_CODE")
            false
        } ?: false
        val snapshot = mpv?.runtimeAudioSnapshot()
        val position = engine.currentPositionMs.value
        println(
            "[app] player state: ${engine.playbackState.value} at ${position}ms, " +
                "song “${viewModel.currentSong.value?.title ?: "none"}”, " +
                "audio out ${snapshot?.output.orEmpty().ifEmpty { "n/a" }}, " +
                "codec ${snapshot?.codec.orEmpty().ifEmpty { "n/a" }}, " +
                "${snapshot?.sampleRate.orEmpty().ifEmpty { "?" }} Hz, " +
                "${snapshot?.channels.orEmpty().ifEmpty { "?" }} channels"
        )
        if (!playing) failures += "the real player did not reach $REQUIRED_POSITION_MS ms of playback"
        if (snapshot == null || snapshot.output.isEmpty()) failures += "mpv reported no audio output"

        // 3. The lyrics the player state carries, and the line the pane highlights.
        val lyrics = withTimeoutOrNull(30_000) {
            while (true) {
                val current = viewModel.playerInfo.value.lyrics
                if (current != null && !current.synced.isNullOrEmpty()) return@withTimeoutOrNull current
                if (!viewModel.playerInfo.value.isLoadingLyrics && current != null) return@withTimeoutOrNull current
                delay(200)
            }
            @Suppress("UNREACHABLE_CODE")
            null
        }
        if (lyrics == null || lyrics.synced.isNullOrEmpty()) {
            failures += "the player state never carried synced lyrics"
            println("[app] lyrics: none in the player state")
        } else {
            val synced = lyrics.synced!!
            println(
                "[app] lyrics in the player state: ${synced.size} synced lines, " +
                    "first at ${synced.first().time}ms, last at ${synced.last().time}ms"
            )
            // An intro has no line yet, and a pane that highlights nothing there is correct,
            // so the reading starts once the playhead is inside the lyrics.
            val entered = withTimeoutOrNull(60_000) {
                while (engine.playbackState.value == PlaybackState.PLAYING) {
                    if (synced.any { it.time <= engine.currentPositionMs.value }) break
                    delay(250)
                }
                true
            } ?: false
            val firstPosition = engine.currentPositionMs.value
            val firstLine = activeLine(synced, firstPosition)
            // A sung line lasts as long as the singer takes over it, so the reading follows
            // the playhead until the highlighted line is a different one.
            var secondPosition = firstPosition
            var secondLine = firstLine
            withTimeoutOrNull(45_000) {
                while (engine.playbackState.value == PlaybackState.PLAYING) {
                    secondPosition = engine.currentPositionMs.value
                    secondLine = activeLine(synced, secondPosition)
                    if (secondLine != firstLine) break
                    delay(250)
                }
            }
            println(
                "[app] active line: at ${firstPosition}ms \"${firstLine.take(70)}\", " +
                    "at ${secondPosition}ms \"${secondLine.take(70)}\""
            )
            if (!entered || firstLine.isBlank()) failures += "no lyric line matched the playhead"
            if (firstLine == secondLine && secondPosition > firstPosition) {
                failures += "the active line did not follow the playhead"
            }
            if (secondPosition <= firstPosition) failures += "the playhead did not advance between the two readings"
        }

        // 4. The screens themselves, composed from the state the player holds now.
        renderScreens(viewModel)

        println("[app] ---------------------------------------------------------")
        if (failures.isEmpty()) {
            println("[app] RESULT: PASS")
            engine.stop()
            delay(300)
            return 0
        }
        failures.forEach { println("[app] FAIL $it") }
        println("[app] RESULT: FAIL")
        engine.stop()
        delay(300)
        return 1
    }

    private fun activeLine(synced: List<com.music.vivi.desktop.data.model.SyncedLine>, positionMs: Long): String =
        synced.lastOrNull { it.time <= positionMs }?.line.orEmpty()

    /**
     * The window's content, composed with the running view model: the search page shows
     * what the search state holds now, and the expanded player shows the lyrics the
     * player state carries. Both are the app's own screens, not a harness panel.
     */
    private fun renderScreens(viewModel: PlayerViewModel) {
        render("app-search-screen.png") {
            PixelPlayerTheme(darkTheme = true) {
                MainAppScaffold(
                    viewModel = viewModel,
                    navigator = DesktopNavigator(DesktopNavTarget.YouTubeMusic),
                    isQueueSidePanelOpen = true
                )
            }
        }
        render("app-lyrics-screen.png") {
            PixelPlayerTheme(darkTheme = true) {
                ExpandedPlayerScreen(
                    viewModel = viewModel,
                    initialTab = ExpandedTab.LYRICS,
                    onClose = {}
                )
            }
        }
    }

    private fun render(name: String, width: Int = 1440, height: Int = 900, content: @Composable () -> Unit) {
        val scene = ImageComposeScene(
            width = width,
            height = height,
            density = Density(1f),
            coroutineContext = Dispatchers.Main
        ) { content() }
        try {
            // Effects in the scene (the page's network state, the artwork colour) need a
            // few frames and a snapshot apply notification before they settle.
            repeat(3) { scene.render() }
            Thread.sleep(500)
            Snapshot.sendApplyNotifications()
            scene.render()
            Thread.sleep(700)
            Snapshot.sendApplyNotifications()
            val bytes = scene.render().encodeToData()?.bytes ?: return
            val out = File("build/$name")
            out.parentFile?.mkdirs()
            out.writeBytes(bytes)
            println("[app] screenshot: ${out.absolutePath} (${bytes.size} bytes)")
        } catch (t: Throwable) {
            println("[app] screenshot $name FAILED: ${t::class.simpleName}: ${t.message}")
        } finally {
            scene.close()
        }
    }
}
