@file:OptIn(ExperimentalComposeUiApi::class)

package com.music.vivi.desktop

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.ImageComposeScene
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import com.music.vivi.desktop.player.LoadPhase
import com.music.vivi.desktop.player.RepeatMode

/**
 * Headless recomposition count for the playhead refactor.
 *
 *     ./gradlew :desktop:playheadRecompositionCheck
 *
 * The real `PlayerScreen` is composed at 1280x800 with the Expressive design,
 * and the playhead is advanced 20 times. A test-only CompositionLocal counts
 * recompositions in the screen, in the Expressive container and in the seek
 * leaf. The run also measures the old shape: a control composable that takes
 * `position: Long` at its container and draws it, advanced the same way.
 *
 * The check reports counts, not FPS. It fails when the screen or the container
 * recomposes more than twice for 20 ticks, or when the leaf stops following it.
 */
object PlayheadRecompositionCheck {

    private const val WIDTH = 1280
    private const val HEIGHT = 800
    private const val TICKS = 20
    private const val STEP_MS = 500L

    private val counts = linkedMapOf<String, Int>()

    @JvmStatic
    fun main(args: Array<String>) {
        val track = NowPlaying(
            videoId = "recompositionCheck",
            title = "Playhead isolation",
            artist = "VIVI Music DE",
            album = "Performance",
            durationMs = 213_000,
            thumbnail = null,
        )

        var playhead by mutableStateOf(0L)
        val scene = ImageComposeScene(WIDTH, HEIGHT) {
            AppTheme(mode = ThemeMode.DARK, accent = Color(0xFFED5564), language = "en") {
                CompositionLocalProvider(
                    LocalRecompositionProbe provides { name ->
                        counts[name] = (counts[name] ?: 0) + 1
                    },
                ) {
                    Box(Modifier.fillMaxSize()) {
                        PlayerScreen(
                            queue = listOf(track),
                            index = 0,
                            isPlaying = true,
                            positionMs = { playhead },
                            durationMs = 213_000,
                            volume = 0.7f,
                            isShuffle = false,
                            repeatMode = RepeatMode.OFF,
                            errorKey = null,
                            errorDetail = null,
                            loadPhase = LoadPhase.NONE,
                            onTogglePlay = {},
                            onNext = {},
                            onPrevious = {},
                            onSeek = {},
                            onVolume = {},
                            onToggleShuffle = {},
                            onCycleRepeat = {},
                            language = "en",
                            onOpenLyrics = {},
                            onOpenQueue = {},
                            onAddToPlaylist = {},
                            design = PlayerDesign.EXPRESSIVE,
                            background = PlayerBackgroundStyle.BLUR,
                            sliderStyle = ViviSliderStyle.SLIM,
                        )
                    }
                }
            }
        }

        try {
            repeat(4) {
                scene.render()
                Thread.sleep(80)
            }
            counts.clear()
            repeat(TICKS) {
                playhead += STEP_MS
                scene.render()
            }
        } finally {
            scene.close()
        }

        val after = counts.toMap()

        // The control takes the position as a Long, the shape the containers used
        // before.
        var legacyPosition by mutableStateOf(0L)
        counts.clear()
        val legacyScene = ImageComposeScene(320, 120) {
            AppTheme(mode = ThemeMode.DARK, accent = Color(0xFFED5564), language = "en") {
                CompositionLocalProvider(
                    LocalRecompositionProbe provides { name ->
                        counts[name] = (counts[name] ?: 0) + 1
                    },
                ) {
                    LegacyPrimitivePlayhead(legacyPosition)
                }
            }
        }
        try {
            repeat(2) { legacyScene.render() }
            counts.clear()
            repeat(TICKS) {
                legacyPosition += STEP_MS
                legacyScene.render()
            }
        } finally {
            legacyScene.close()
        }
        val before = counts.toMap()

        println("playheadRecompositionCheck: $TICKS ticks of ${STEP_MS}ms")
        println("before (Long at the container): container=${before["legacy-container"] ?: 0} clock=${before["legacy-clock"] ?: 0}")
        println("after  (provider at the leaf): screen=${after["player-screen"] ?: 0} container=${after["m3e-content"] ?: 0} seek=${after["m3e-seek"] ?: 0}")

        val failures = mutableListOf<String>()
        // The first ticks can merge into the initial frame, so the control may
        // stop short of 20. It must still follow the playhead at tick rate while
        // the isolated screen stays at zero.
        if ((before["legacy-container"] ?: 0) < 10) {
            failures += "the control container did not follow the playhead, so the measurement is not comparable"
        }
        val screen = after["player-screen"] ?: 0
        val container = after["m3e-content"] ?: 0
        val seek = after["m3e-seek"] ?: 0
        if (screen > 2) failures += "the player screen recomposed $screen times for $TICKS playhead ticks"
        if (container > 2) failures += "the Expressive container recomposed $container times for $TICKS playhead ticks"
        if (seek < TICKS) failures += "the seek leaf only recomposed $seek times for $TICKS playhead ticks"

        if (failures.isNotEmpty()) {
            failures.forEach { println("  - $it") }
            error("playhead recomposition check failed (${failures.size} case(s))")
        }
        println("OK: screen and container constant, the leaf follows every tick")
    }

    @Composable
    private fun LegacyPrimitivePlayhead(position: Long) {
        val probe = LocalRecompositionProbe.current
        if (probe != null) SideEffect { probe("legacy-container") }
        Text(formatTime(position))
        if (probe != null) SideEffect { probe("legacy-clock") }
    }
}
