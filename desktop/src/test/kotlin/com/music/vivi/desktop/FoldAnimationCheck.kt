package com.music.vivi.desktop

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.ImageComposeScene
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import org.jetbrains.skia.EncodedImageFormat
import java.io.File
import javax.imageio.ImageIO

/**
 * Headless check of one rule: a settings block that belongs to a switch folds
 * open and shut only while the Animations switch is on, run with
 *
 *     ./gradlew :desktop:foldAnimationCheck
 *
 * `M3SettingsFold` is the block the performance reader's panels, the developer
 * options' group and every row that belongs to a switch are made of, and its
 * movement is the app's one 200 ms tween. With the master switch off the block
 * must be there in the frame it is switched on and gone in the frame it is
 * switched off: a music player that has been asked not to animate must not grow a
 * box over 200 ms either.
 *
 * The fold is rendered into an off-screen `ImageComposeScene` on its own, over a
 * coloured box the exact height of a card, and the column through the middle is
 * read at a fixed set of times after the switch is flipped. A painted box is a run
 * of pixels that is not the page, so the run's height is the block's height:
 *
 *  * animations ON: the box is drawn part-way at least twice on the way in and on
 *    the way out, which is the tween being drawn and not a jump;
 *  * animations OFF: every frame is either the full box or nothing at all, at the
 *    same times.
 *
 * The frames are written under `.ignore/foldanim/` so the two runs can be looked
 * at. What is measured is the movement; the easing curve and the switch's own
 * round trip are not.
 */
object FoldAnimationCheck {

    private const val WIDTH = 480
    private const val HEIGHT = 420
    private const val BOX_DP = 200

    /** Times after the switch was flipped, in ms. */
    private val SAMPLES_MS = listOf(5L, 20L, 40L, 60L, 80L, 100L, 140L, 200L, 320L)

    private val outDir = File(".ignore/foldanim").also { it.mkdirs() }

    @OptIn(ExperimentalComposeUiApi::class)
    @JvmStatic
    fun main(args: Array<String>) {
        val failures = mutableListOf<String>()

        val animated = measure(enabled = true, name = "on")
        val instant = measure(enabled = false, name = "off")

        println("animations on:  closing=$animated")
        println("animations off: closing=$instant")

        // With the switch on, the box has to be SEEN between its two states: a run
        // that is neither the full box nor nothing proves the tween is drawn.
        val partialOn = animated.count { it in 1 until BOX_DP }
        if (partialOn < 2) {
            failures += "with animations on the fold should be drawn part-way at least twice, read $partialOn frame(s)"
        }
        if (animated.last() != 0) {
            failures += "with animations on the fold should be gone at the end, read ${animated.last()} dp"
        }

        // With it off, every sample is one of the two states and never anything in
        // between: the block is dropped in the frame the switch flips.
        val partialOff = instant.count { it in 1 until BOX_DP }
        if (partialOff != 0) {
            failures += "with animations off the fold must never be drawn part-way, read $instant"
        }
        if (instant.last() != 0) {
            failures += "with animations off the fold should be gone at once, read ${instant.last()} dp"
        }

        if (failures.isEmpty()) {
            println()
            println(
                "OK: the fold is drawn part-way $partialOn time(s) with animations on ($animated) " +
                    "and never with them off ($instant), where it is dropped in the frame the switch flips",
            )
        } else {
            println()
            println("FAILED:")
            failures.forEach { println("  - $it") }
            throw IllegalStateException("fold animation check failed (${failures.size} case(s))")
        }
    }

    /**
     * The height of the box in dp, sampled at [SAMPLES_MS] after the switch was
     * flipped off. `Animations.enabled` is set before the scene exists, so the
     * first composition already reads the state being measured.
     */
    private fun measure(enabled: Boolean, name: String): List<Int> {
        Animations.speed = "normal"
        Animations.enabled = enabled
        val visible = mutableStateOf(true)
        val scene = ImageComposeScene(WIDTH, HEIGHT) {
            AppTheme(mode = ThemeMode.LIGHT, accent = Color(0xFFED5564), language = "en") {
                Column(Modifier.fillMaxWidth().padding(24.dp)) {
                    M3SettingsFold(visible = visible.value) {
                        Box(
                            Modifier
                                .fillMaxWidth()
                                .height(BOX_DP.dp)
                                .background(Color(0xFF000000)),
                        )
                    }
                }
            }
        }
        try {
            // One frame with the block open, then the flip: the samples below are
            // times after it.
            scene.render(0L)
            visible.value = false
            val heights = SAMPLES_MS.map { ms ->
                val bytes = scene.render(ms * 1_000_000L).encodeToData(EncodedImageFormat.PNG, 100)!!.bytes
                File(outDir, "$name-$ms.png").writeBytes(bytes)
                paintedHeight(bytes)
            }
            return heights
        } finally {
            scene.close()
        }
    }

    /** How tall the painted box is, in dp (1 dp = 1 px on this scene's density). */
    private fun paintedHeight(png: ByteArray): Int {
        val image = ImageIO.read(png.inputStream())
        val page = image.getRGB(2, 2)
        var first = -1
        var last = -1
        for (y in 0 until image.height) {
            val pixel = image.getRGB(image.width / 2, y)
            if (pixel != page && !near(pixel, page)) {
                if (first < 0) first = y
                last = y
            }
        }
        return if (first < 0) 0 else last - first + 1
    }

    private fun near(a: Int, b: Int, tolerance: Int = 6): Boolean =
        kotlin.math.abs(((a shr 16) and 0xFF) - ((b shr 16) and 0xFF)) <= tolerance &&
            kotlin.math.abs(((a shr 8) and 0xFF) - ((b shr 8) and 0xFF)) <= tolerance &&
            kotlin.math.abs((a and 0xFF) - (b and 0xFF)) <= tolerance
}
