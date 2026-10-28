@file:OptIn(ExperimentalComposeUiApi::class)

package com.music.vivi.desktop

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.ImageComposeScene
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import org.jetbrains.skia.EncodedImageFormat
import java.io.File

/**
 * Headless check of the player design screen, run with
 *
 *     ./gradlew :desktop:playerDesignRenderCheck
 *
 * The screen was a loose stack of ten single-row cards, so every choice looked
 * unrelated to the next one; it is now four blocks - the full player (its design,
 * then the background and the rotating artwork), the mini player (design,
 * background, pure black and the swipe), and the slider style on its own.
 *
 * A claim about how a screen is divided into cards can only be checked against
 * pixels: no display is attached here, so `PlayerDesignScreen` is rendered into
 * an off-screen `ImageComposeScene` and the column through the middle of it is
 * read back. Each card paints a band, the page shows between bands, and the two
 * kinds of gap have different sizes - 3 dp between the rows of one card, 8 dp
 * between two cards - so counting bands and classifying the gaps says whether
 * the rows really share a card or each one still has its own:
 *
 *  * four cards, holding 1 + 2 + 4 + 1 rows (the sensitivity slider is off, so
 *    its derived row is not drawn);
 *  * with the swipe on, the sensitivity is the mini player card's fifth row and
 *    not a card of its own: 9 rows and the seam sequence
 *    `[16, 3, 16, 3, 3, 3, 3, 16]`, which is what says which card took the row;
 *  * the chips draw the value they are given: two renders that differ only in
 *    `sliderStyle` must not produce the same image.
 *
 * PNGs are written under `.ignore/playerdesign/` so the layout can be looked at.
 */
object PlayerDesignRenderCheck {

    private val outDir = File(".ignore/playerdesign")

    /** Wide enough that no title wraps, tall enough for the four cards. */
    private const val WIDTH = 900
    private const val HEIGHT = 1200

    @JvmStatic
    fun main(args: Array<String>) {
        outDir.mkdirs()
        val failures = mutableListOf<String>()

        val slim = render(sliderStyle = "slim")
        File(outDir, "player-design.png").writeBytes(slim)

        val bands = bands(slim)
        println("cards=${bands.cards} rows=${bands.rows} gaps=${bands.gaps}")
        if (bands.cards != 4) {
            failures += "expected 4 cards (player, player background + artwork, mini player, slider style), read ${bands.cards}"
        }
        if (bands.rows != 8) {
            failures += "expected 8 rows in those cards (1 + 2 + 4 + 1), read ${bands.rows}"
        }

        // The swipe on: the sensitivity is a row of the mini player's card now,
        // directly under the switch it tunes (it used to be a card of its own
        // with the Slider floating under the group). One more row, same four
        // cards - if it went back to a card of its own this reads five.
        val swiped = render(sliderStyle = "slim", swipe = true)
        File(outDir, "player-design-swipe.png").writeBytes(swiped)
        val swipedBands = bands(swiped)
        println("swipe on: cards=${swipedBands.cards} rows=${swipedBands.rows} gaps=${swipedBands.gaps}")
        if (swipedBands.rows != 9) {
            failures += "with the swipe on the screen should hold 9 rows (1 + 2 + 5 + 1), read ${swipedBands.rows}"
        }
        // The file's own seam sequence is what says WHICH card the extra row
        // joined: 16 dp between cards (the settings cards' `--md-sys-spacing-4`)
        // and 3 dp between the rows of one card, so the third card holds five
        // rows. A sensitivity appended to the last card instead reads
        // `[16, 3, 16, 3, 3, 3, 16, 3]` and a card of its own puts a 16 among them.
        val expectedGaps = listOf(16, 3, 16, 3, 3, 3, 3, 16)
        if (swipedBands.gaps != expectedGaps) {
            failures += "with the swipe on the seams should read $expectedGaps (5 rows in the mini player card), read ${swipedBands.gaps}"
        }
        val wavy = render(sliderStyle = "wavy")
        File(outDir, "player-design-wavy.png").writeBytes(wavy)
        val differing = differingPixels(slim, wavy)
        println("pixels that follow sliderStyle=$differing")
        if (differing <= 0) {
            failures += "changing sliderStyle changed nothing on screen: the chip is not drawing its value"
        }

        if (failures.isEmpty()) {
            println()
            println("OK: four cards, 8 rows, the swipe row joining the mini player card, and the chips drawing the value they are given")
            return
        }
        println()
        println("FAILED:")
        failures.forEach { println("  - $it") }
        throw IllegalStateException("player design render check failed (${failures.size} case(s))")
    }

    private fun render(sliderStyle: String, swipe: Boolean = false): ByteArray {
        val scene = ImageComposeScene(WIDTH, HEIGHT) {
            AppTheme(mode = ThemeMode.LIGHT, accent = Color(0xFFED5564), language = "en") {
                Column(
                    Modifier
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState())
                        .padding(horizontal = 24.dp),
                ) {
                    PlayerDesignScreen(
                        language = "en",
                        design = PlayerDesign.EXPRESSIVE,
                        onDesignChange = {},
                        background = PlayerBackgroundStyle.BLUR,
                        onBackgroundChange = {},
                        rotatingThumbnail = true,
                        onRotatingThumbnailChange = {},
                        miniPlayerDesign = MiniPlayerDesign.CLASSIC,
                        onMiniPlayerDesignChange = {},
                        miniPlayerBackgroundStyle = MiniPlayerBackgroundStyle.FOLLOW_THEME,
                        onMiniPlayerBackgroundStyleChange = {},
                        pureBlackMiniPlayer = false,
                        onPureBlackMiniPlayerChange = {},
                        sliderStyle = sliderStyle,
                        onSliderStyleChange = {},
                        // Off in the base render, so the sensitivity row is not
                        // drawn and the four cards hold exactly the rows the
                        // check counts; the second render turns it on to prove
                        // that row joins the mini player's card.
                        swipeThumbnail = swipe,
                        onSwipeThumbnailChange = {},
                        swipeSensitivity = 0.73f,
                        onSwipeSensitivityChange = {},
                    )
                }
            }
        }
        return try {
            scene.render().encodeToData(EncodedImageFormat.PNG, 100)!!.bytes
        } finally {
            scene.close()
        }
    }

    /** What the middle column of the render says about the cards. */
    private data class Bands(val cards: Int, val rows: Int, val gaps: List<Int>)

    private fun bands(png: ByteArray): Bands {
        val image = javax.imageio.ImageIO.read(png.inputStream())
        val x = image.width / 2
        val page = image.getRGB(x, 2)
        // Walk the column and turn it into runs of "card" and "page".
        val runs = mutableListOf<Pair<Boolean, Int>>()
        for (y in 0 until image.height) {
            val onCard = !near(image.getRGB(x, y), page)
            if (runs.isNotEmpty() && runs.last().first == onCard) {
                runs[runs.size - 1] = onCard to runs.last().second + 1
            } else {
                runs += onCard to 1
            }
        }
        // The screen title is a few pixels of glyphs above the first card, not a
        // card: a row of a card is at least ~48 dp tall, so any short run of ink
        // is text and is dropped (merging the page runs it leaves behind).
        val cleaned = mutableListOf<Pair<Boolean, Int>>()
        for (run in runs) {
            if (run.first && run.second < 40) continue
            if (cleaned.isNotEmpty() && cleaned.last().first == run.first) {
                cleaned[cleaned.size - 1] = run.first to cleaned.last().second + run.second
            } else {
                cleaned += run
            }
        }
        // A band is a card; the page runs between bands are the gaps.
        val bands = cleaned.filter { it.first }
        val gaps = cleaned.dropWhile { !it.first }
            .filter { !it.first }
            .map { it.second }
        // The last page run is the empty space under the last card, not a gap.
        val between = gaps.dropLast(1)
        // 3 dp seams sit between the rows of one card, 8 dp between two cards.
        val cards = between.count { it >= 6 } + 1
        return Bands(cards = cards, rows = bands.size, gaps = between)
    }

    private fun near(a: Int, b: Int): Boolean {
        val dr = kotlin.math.abs(((a shr 16) and 255) - ((b shr 16) and 255))
        val dg = kotlin.math.abs(((a shr 8) and 255) - ((b shr 8) and 255))
        val db = kotlin.math.abs((a and 255) - (b and 255))
        return dr + dg + db <= 6
    }

    private fun differingPixels(a: ByteArray, b: ByteArray): Int {
        val ia = javax.imageio.ImageIO.read(a.inputStream())
        val ib = javax.imageio.ImageIO.read(b.inputStream())
        if (ia.width != ib.width || ia.height != ib.height) return -1
        var differing = 0
        for (y in 0 until ia.height) {
            for (x in 0 until ia.width) {
                if (ia.getRGB(x, y) != ib.getRGB(x, y)) differing++
            }
        }
        return differing
    }
}
