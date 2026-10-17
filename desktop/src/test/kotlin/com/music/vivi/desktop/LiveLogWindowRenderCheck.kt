@file:OptIn(ExperimentalComposeUiApi::class)

package com.music.vivi.desktop

import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.ImageComposeScene
import androidx.compose.ui.graphics.Color
import org.jetbrains.skia.EncodedImageFormat
import java.io.File

/**
 * Headless check of the live log window, run with
 *
 *     ./gradlew :desktop:liveLogRenderCheck
 *
 * The window's own `Window` needs a display, so the content is rendered into an
 * off-screen `ImageComposeScene`. What this proves, in the order a support
 * report needs it:
 *
 *  * a log with lines draws at all (the window used to be a bare list);
 *  * the sidebar scrollbar is a real slider: for a longer log its thumb is
 *    strictly SHORTER, which is the visible difference between a scrollbar and
 *    a painted strip — a full-height bar would measure the same either way.
 *
 * It does not check the two jump buttons or the follow state: both are
 * interaction (a click, a wheel scroll), and this scene has no input. They are
 * covered by the compiler and by using the window; what can be lost silently is
 * the bar, which is why the bar is the part measured here.
 * PNGs land under `.ignore/livelog/`.
 */
object LiveLogWindowRenderCheck {

    private val outDir = File(".ignore/livelog")
    private const val WIDTH = 900
    private const val HEIGHT = 620

    /**
     * The scrollbar's own columns: the 10 px between the 12 dp window padding
     * and the right edge. Kept off the header's icon buttons (which sit a few
     * pixels further left) so their ink cannot be measured as a thumb.
     */
    private const val BAR_START_INSET = 22
    private const val BAR_END_INSET = 12

    @JvmStatic
    fun main(args: Array<String>) {
        outDir.mkdirs()
        val failures = mutableListOf<String>()

        val shortInk = ink(render("short", lines = 120))
        println("== 120 lines   ink=$shortInk")
        if (shortInk <= 0) failures += "a log with lines painted nothing"

        val longInk = ink(render("long", lines = 4000))
        println("== 4000 lines  ink=$longInk")
        if (longInk <= 0) failures += "a long log painted nothing"

        val shortThumb = barHeight("short")
        val longThumb = barHeight("long")
        println("== scrollbar thumb: 120 lines=${shortThumb}px, 4000 lines=${longThumb}px")
        if (shortThumb <= 0 || longThumb <= 0) {
            failures += "the sidebar scrollbar drew no thumb (${shortThumb}px / ${longThumb}px)"
        } else if (longThumb >= shortThumb) {
            failures += "the scrollbar thumb did not shrink with a longer log " +
                "(${longThumb}px for 4000 lines is not less than ${shortThumb}px for 120)"
        }

        if (failures.isEmpty()) {
            println()
            println("OK: the live log draws and its sidebar scrollbar scales with the length of the log")
            return
        }
        println()
        println("FAILED:")
        failures.forEach { println("  - $it") }
        throw IllegalStateException("live log render check failed (${failures.size} case(s))")
    }

    /** Fills the live buffer with [lines] lines and renders the window. */
    private fun render(name: String, lines: Int): ByteArray {
        AppLog.clear()
        repeat(lines) { i ->
            AppLog.log("playback", "line $i — audio writer pass #$i: 250ms total, 250ms inside the device write")
        }
        val scene = ImageComposeScene(WIDTH, HEIGHT) {
            AppTheme(mode = ThemeMode.LIGHT, accent = Color(0xFFED5564)) {
                LiveLogWindowContent(language = "en", onClose = {})
            }
        }
        return try {
            // Two frames: the first one lays the list out, and the scrollbar's
            // thumb is measured from that layout — a single frame would capture
            // an empty thumb and report "no scrollbar" for a bar that is there.
            scene.render()
            val png = scene.render().encodeToData(EncodedImageFormat.PNG, 100)!!.bytes
            File(outDir, "$name.png").writeBytes(png)
            png
        } finally {
            scene.close()
        }
    }

    private fun ink(png: ByteArray): Int {
        val image = javax.imageio.ImageIO.read(png.inputStream())
        val bg = image.getRGB(0, 0)
        var ink = 0
        for (y in 0 until image.height) {
            for (x in 0 until image.width) {
                if (differs(image.getRGB(x, y), bg)) ink++
            }
        }
        return ink
    }

    /** True when a pixel is not the window's own background (the top-left one). */
    private fun differs(rgb: Int, bg: Int): Boolean {
        for (shift in intArrayOf(16, 8, 0)) {
            val a = (rgb shr shift) and 255
            val b = (bg shr shift) and 255
            if (kotlin.math.abs(a - b) > 6) return true
        }
        return false
    }

    /**
     * Height of the drawn scrollbar thumb, in pixels: the longest run of rows in
     * the bar's own columns that is not background. A log shorter than the
     * viewport (or a bar that was never drawn) reads as 0; the longest run is
     * taken because the header above it also paints a few rows in that area.
     */
    private fun barHeight(name: String): Int {
        val image = javax.imageio.ImageIO.read(File(outDir, "$name.png"))
        val bg = image.getRGB(0, 0)
        val from = image.width - BAR_START_INSET
        val to = image.width - BAR_END_INSET
        var best = 0
        var run = 0
        for (y in 0 until image.height) {
            var rowInk = 0
            for (x in from until to) {
                if (differs(image.getRGB(x, y), bg)) rowInk++
            }
            if (rowInk > 0) {
                run++
                if (run > best) best = run
            } else {
                run = 0
            }
        }
        return best
    }
}
