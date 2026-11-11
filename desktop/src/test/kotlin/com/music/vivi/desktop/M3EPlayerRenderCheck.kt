@file:OptIn(ExperimentalComposeUiApi::class)

package com.music.vivi.desktop

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.ImageComposeScene
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import com.music.vivi.desktop.player.LoadPhase
import com.music.vivi.desktop.player.RepeatMode
import org.jetbrains.skia.EncodedImageFormat
import java.awt.image.BufferedImage
import java.io.File

/**
 * Headless render check of the Expressive (M3E) player after the playhead
 * refactor.
 *
 *     ./gradlew :desktop:m3ePlayerRenderCheck
 *
 * The screen hands the playhead to a seek leaf. This check confirms the screen
 * still paints: the left artwork and control column and the right queue and
 * lyrics panel carry ink, and the seek row carries its slider. The frame is
 * written under `.ignore/m3e-player/`.
 */
object M3EPlayerRenderCheck {

    private const val WIDTH = 1280
    private const val HEIGHT = 800
    private val outDir = File(".ignore/m3e-player")

    @JvmStatic
    fun main(args: Array<String>) {
        outDir.mkdirs()
        val track = NowPlaying(
            videoId = "m3eRenderCheck",
            title = "Expressive player",
            artist = "VIVI Music DE",
            album = "Performance",
            durationMs = 213_000,
            thumbnail = null,
        )
        var playhead by mutableStateOf(81_000L)
        val scene = ImageComposeScene(WIDTH, HEIGHT) {
            AppTheme(mode = ThemeMode.DARK, accent = Color(0xFFED5564), language = "en") {
                CompositionLocalProvider(LocalRecompositionProbe provides null) {
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
        val png = try {
            repeat(4) {
                scene.render()
                Thread.sleep(80)
            }
            scene.render().encodeToData(EncodedImageFormat.PNG, 100)!!.bytes
        } finally {
            scene.close()
        }
        File(outDir, "m3e-player.png").writeBytes(png)
        val image = javax.imageio.ImageIO.read(png.inputStream())

        val leftInk = inkPixels(image, 0.03, 0.48, 0.05, 0.95)
        val rightInk = inkPixels(image, 0.52, 0.98, 0.05, 0.95)
        val seekInk = inkPixels(image, 0.08, 0.45, 0.62, 0.78)
        println("m3ePlayerRenderCheck: left ink=$leftInk right ink=$rightInk seek ink=$seekInk")

        val failures = mutableListOf<String>()
        if (leftInk < 200) failures += "the Expressive left column drew almost no ink ($leftInk px)"
        if (rightInk < 200) failures += "the queue panel drew almost no ink ($rightInk px)"
        if (seekInk < 20) failures += "the seek row drew no slider ink ($seekInk px)"
        if (failures.isNotEmpty()) {
            failures.forEach { println("  - $it") }
            error("M3E player render check failed (${failures.size} case(s))")
        }
        println("OK: the M3E player paints its column, its panel and the seek row")
    }

    private fun inkPixels(image: BufferedImage, x0: Double, x1: Double, y0: Double, y1: Double): Int {
        var ink = 0
        for (y in (image.height * y0).toInt() until (image.height * y1).toInt()) {
            for (x in (image.width * x0).toInt() until (image.width * x1).toInt()) {
                val rgb = image.getRGB(x, y)
                val r = (rgb shr 16) and 255
                val g = (rgb shr 8) and 255
                val b = rgb and 255
                if (r + g + b > 330) ink++
            }
        }
        return ink
    }
}
