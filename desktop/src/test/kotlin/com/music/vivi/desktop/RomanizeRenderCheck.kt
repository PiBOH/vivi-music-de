@file:OptIn(ExperimentalComposeUiApi::class)

package com.music.vivi.desktop

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.ImageComposeScene
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import com.music.lyrics.LyricsRomanizer
import org.jetbrains.skia.EncodedImageFormat
import java.io.File

/**
 * Headless check of the Romanization screen, run with
 *
 *     ./gradlew :desktop:romanizeRenderCheck
 *
 * Romanization used to be a card in the middle of *Lyrics*: the master switch,
 * the twelve script switches and the "as main" choice. It is a screen of its own
 * now (`SettingsRomanizeScreen`), reached from the Lyrics row, and none of that
 * can be seen without a display. The screen is rendered off screen in both states
 * and the middle column of the pixels is read back:
 *
 *  * **off: one row.** The master switch alone: the twelve scripts and the
 *    "as main" row are `visible = romanize != null`, so with the switch off they
 *    are not drawn at all - one card, one row, no leftover gap where they were.
 *  * **on: the master and the twelve, then the choice.** Two cards: thirteen rows
 *    in the first (master + 12 scripts, joined by 3 dp seams) and one row in the
 *    second, which is the switch the lyrics read when they decide whether the
 *    romanized text is the main line.
 *
 * The first render doubles as the negative control: a screen that drew the
 * scripts regardless of the switch would read thirteen rows with it off.
 *
 * PNGs are written under `.ignore/romanize/`.
 */
object RomanizeRenderCheck {

    private val outDir = File(".ignore/romanize")

    private const val WIDTH = 900
    private const val HEIGHT = 2400

    @JvmStatic
    fun main(args: Array<String>) {
        outDir.mkdirs()
        val failures = mutableListOf<String>()

        val off = render(romanize = null)
        File(outDir, "romanize-off.png").writeBytes(off)
        val offBands = bands(off)
        println("off: cards=${offBands.cards} rows=${offBands.rows} gaps=${offBands.gaps}")

        val on = render(romanize = LyricsRomanizer.Options())
        File(outDir, "romanize-on.png").writeBytes(on)
        val onBands = bands(on)
        println("on: cards=${onBands.cards} rows=${onBands.rows} gaps=${onBands.gaps}")

        if (offBands.cards != 1 || offBands.rows != 1) {
            failures += "with the switch off the screen should be ONE card of ONE row, read cards=${offBands.cards} rows=${offBands.rows}"
        }
        if (onBands.rows != 14 || onBands.cards != 2) {
            failures += "with the switch on it should be 14 rows in TWO cards (master + 12 scripts, then the as-main row), read cards=${onBands.cards} rows=${onBands.rows}"
        }
        // The scripts and the as-main row are rows OF the card, so the seams
        // inside it are 3 dp and only the gap between the two cards is wider.
        if (onBands.gaps.none { it >= 6 }) {
            failures += "the as-main row should sit in a card of its own, gaps=${onBands.gaps}"
        }

        if (failures.isEmpty()) {
            println("OK: off is one row, on is the master, the twelve scripts and the as-main choice in two cards")
            return
        }
        println()
        println("FAILED:")
        failures.forEach { println("  - $it") }
        throw IllegalStateException("romanize render check failed (${failures.size} case(s))")
    }

    private fun render(romanize: LyricsRomanizer.Options?): ByteArray {
        val options = LyricsDisplayOptions(romanize = romanize, romanizeAsMain = false)
        val scene = ImageComposeScene(WIDTH, HEIGHT) {
            AppTheme(mode = ThemeMode.LIGHT, accent = Color(0xFFED5564), language = "en") {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.TopCenter) {
                    SettingsRomanizeScreen(language = "en", onBack = {}, options = options)
                }
            }
        }
        try {
            // Two frames, like the other checks: the fold's rows measure on the
            // first and paint on the second.
            scene.render()
            val image = scene.render(2_000_000_000L)
            return image.encodeToData(EncodedImageFormat.PNG)?.bytes
                ?: error("the scene produced no image")
        } finally {
            scene.close()
        }
    }

    /** What the middle column of the render says about the cards. */
    private data class Bands(val cards: Int, val rows: Int, val gaps: List<Int>)

    private fun bands(png: ByteArray): Bands {
        val cleaned = cleanedRuns(png)
        val bands = cleaned.filter { it.first }
        val gaps = cleaned.dropWhile { !it.first }.filter { !it.first }.map { it.second }
        val between = gaps.dropLast(1)
        val cards = between.count { it >= 6 } + 1
        return Bands(cards = cards, rows = bands.size, gaps = between)
    }

    /** The middle column as runs of "card" and "page", with text dropped. */
    private fun cleanedRuns(png: ByteArray): List<Pair<Boolean, Int>> {
        val image = javax.imageio.ImageIO.read(png.inputStream())
        val x = image.width / 2
        val page = image.getRGB(x, 2)
        val runs = mutableListOf<Pair<Boolean, Int>>()
        for (y in 0 until image.height) {
            val onCard = !near(image.getRGB(x, y), page, 6)
            if (runs.isNotEmpty() && runs.last().first == onCard) {
                runs[runs.size - 1] = onCard to runs.last().second + 1
            } else {
                runs += onCard to 1
            }
        }
        // A title is a few pixels of glyphs, a row of a card is at least 48 dp.
        val cleaned = mutableListOf<Pair<Boolean, Int>>()
        for (run in runs) {
            if (run.first && run.second < 40) continue
            if (cleaned.isNotEmpty() && cleaned.last().first == run.first) {
                cleaned[cleaned.size - 1] = run.first to cleaned.last().second + run.second
            } else {
                cleaned += run
            }
        }
        return cleaned
    }

    private fun near(a: Int, b: Int, threshold: Int = 6): Boolean {
        val dr = kotlin.math.abs(((a shr 16) and 255) - ((b shr 16) and 255))
        val dg = kotlin.math.abs(((a shr 8) and 255) - ((b shr 8) and 255))
        val db = kotlin.math.abs((a and 255) - (b and 255))
        return dr + dg + db <= threshold
    }
}
