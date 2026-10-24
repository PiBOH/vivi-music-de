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
 *  * animations ON, closing: the box is drawn part-way at least twice and is gone
 *    at the end;
 *  * animations ON, opening: the box is drawn part-way at least twice and stands
 *    at its full height at the end;
 *  * animations OFF: every frame is either the full box or nothing at all, in both
 *    directions, at the same times.
 *
 * Both directions are measured because the fold is one tween run forwards or
 * backwards, and only one side of it being read would leave the other free to
 * move in a way nobody looked at: a tween that opens instantly and closes over
 * 200 ms reads the same as a working one when only the closing samples exist.
 *
 * The frames are written under `.ignore/foldanim/` so the runs can be looked
 * at. What is measured is the movement in each direction; the easing curve is
 * not.
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

        val closingOn = measure(enabled = true, startVisible = true, name = "on-close")
        val openingOn = measure(enabled = true, startVisible = false, name = "on-open")
        val closingOff = measure(enabled = false, startVisible = true, name = "off-close")
        val openingOff = measure(enabled = false, startVisible = false, name = "off-open")

        println("animations on:  closing=$closingOn")
        println("animations on:  opening=$openingOn")
        println("animations off: closing=$closingOff")
        println("animations off: opening=$openingOff")

        // With the switch on, the box has to be SEEN between its two states in each
        // direction: a run that is neither the full box nor nothing proves the tween
        // is drawn, and the end of the run says it arrived where it was going.
        val partialCloseOn = closingOn.count { it in 1 until BOX_DP }
        if (partialCloseOn < 2) {
            failures += "closing with animations on: drawn part-way at least twice, read $partialCloseOn frame(s)"
        }
        if (closingOn.last() != 0) {
            failures += "closing with animations on: gone at the end, read ${closingOn.last()} dp"
        }
        val partialOpenOn = openingOn.count { it in 1 until BOX_DP }
        if (partialOpenOn < 2) {
            failures += "opening with animations on: drawn part-way at least twice, read $partialOpenOn frame(s)"
        }
        if (openingOn.last() != BOX_DP) {
            failures += "opening with animations on: at $BOX_DP dp at the end, read ${openingOn.last()} dp"
        }

        // With it off, every sample is one of the two states and never anything in
        // between, in both directions: the block is dropped or shown in the frame
        // the switch flips.
        val partialCloseOff = closingOff.count { it in 1 until BOX_DP }
        if (partialCloseOff != 0) {
            failures += "closing with animations off: never drawn part-way, read $closingOff"
        }
        if (closingOff.last() != 0) {
            failures += "closing with animations off: gone at once, read ${closingOff.last()} dp"
        }
        val partialOpenOff = openingOff.count { it in 1 until BOX_DP }
        if (partialOpenOff != 0) {
            failures += "opening with animations off: never drawn part-way, read $openingOff"
        }
        if (openingOff.first() != BOX_DP) {
            failures += "opening with animations off: there in the frame the switch flips, read ${openingOff.first()} dp"
        }

        if (failures.isEmpty()) {
            println()
            println(
                "OK: the fold is drawn part-way $partialCloseOn time(s) closing and $partialOpenOn opening " +
                    "with animations on ($closingOn / $openingOn) and never with them off " +
                    "($closingOff / $openingOff), where it is dropped and shown in the frame the switch flips",
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
     * flipped, starting from [startVisible] so the same scene measures the fold
     * closing and the fold opening. `Animations.enabled` is set before the scene
     * exists, so the first composition already reads the state being measured.
     */
    private fun measure(enabled: Boolean, startVisible: Boolean, name: String): List<Int> {
        Animations.speed = "normal"
        Animations.enabled = enabled
        val visible = mutableStateOf(startVisible)
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
            // One frame in the starting state, then the flip: the samples below
            // are times after it.
            scene.render(0L)
            visible.value = !startVisible
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
