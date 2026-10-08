@file:OptIn(ExperimentalComposeUiApi::class)

package com.music.vivi.desktop

import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.ImageComposeScene
import androidx.compose.ui.graphics.Color
import org.jetbrains.skia.EncodedImageFormat
import java.io.File

/**
 * Headless smoke check of the first-run flow, run with
 *
 *     ./gradlew :desktop:firstRunRenderCheck
 *
 * `FirstRunFlow` is the screen a brand-new install shows before anything else,
 * and it is the one screen that can only be seen on a fresh install, so a
 * mistake in it is a mistake most users meet before they meet the app. It is
 * rendered here into an off-screen `ImageComposeScene` (no window, no display)
 * at the smallest window the app allows and at a large one, and each render is
 * checked for the only two things this can check without eyes:
 *
 *  * it composes at all (a crash here is a crash on first launch), and
 *  * it paints ink: a screen that draws nothing, or that is laid out outside the
 *    window, is not a working first-run screen. The language picker list is
 *    height-limited for the same reason, so the 600x480 case is the interesting
 *    one.
 *
 * What it does NOT check is whether the screen looks right, and it does not walk
 * the three steps: stepping it would need a click at a position that moves with
 * the layout. The static half of that (every string the screen asks for exists
 * in every language) is covered by `scripts/audit_desktop_localization.py`.
 *
 * PNGs of each render are written under `.ignore/firstrun/`.
 */
object FirstRunRenderCheck {

    /** The app's own window minimum, and two sizes up from it. */
    private val sizes = listOf(
        "small" to (600 to 480),
        "normal" to (1024 to 768),
        "large" to (1600 to 900),
    )

    private val outDir = File(".ignore/firstrun")

    @JvmStatic
    fun main(args: Array<String>) {
        outDir.mkdirs()
        val failures = mutableListOf<String>()

        for ((name, size) in sizes) {
            val (width, height) = size
            val png = render(width, height)
            File(outDir, "$name.png").writeBytes(png)
            val ink = ink(png)
            println("== $name  ${width}x$height  ink=$ink")
            if (ink <= 0) failures += "$name ($width x $height): the first-run screen painted nothing"
        }

        if (failures.isEmpty()) {
            println()
            println("OK: the first-run flow composes and paints at every window size")
            return
        }
        println()
        println("FAILED:")
        failures.forEach { println("  - $it") }
        throw IllegalStateException("first-run render check failed (${failures.size} case(s))")
    }

    private fun render(width: Int, height: Int): ByteArray {
        val scene = ImageComposeScene(width, height) {
            AppTheme(mode = ThemeMode.LIGHT, accent = Color(0xFFED5564)) {
                FirstRunFlow(
                    language = "en",
                    onLanguageSelected = {},
                    onFinish = {},
                )
            }
        }
        return try {
            scene.render().encodeToData(EncodedImageFormat.PNG, 100)!!.bytes
        } finally {
            scene.close()
        }
    }

    private fun ink(png: ByteArray): Int {
        val image = javax.imageio.ImageIO.read(png.inputStream())
        var ink = 0
        for (y in 0 until image.height) {
            for (x in 0 until image.width) {
                val rgb = image.getRGB(x, y)
                val r = (rgb shr 16) and 255
                val g = (rgb shr 8) and 255
                val b = rgb and 255
                if (r > 245 && g > 245 && b > 245) continue
                ink++
            }
        }
        return ink
    }
}
