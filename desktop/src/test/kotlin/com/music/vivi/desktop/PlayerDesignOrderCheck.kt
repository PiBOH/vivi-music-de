@file:OptIn(ExperimentalTestApi::class)

package com.music.vivi.desktop

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.ImageComposeScene
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.SemanticsNode
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.SemanticsNodeInteraction
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.runComposeUiTest
import androidx.compose.ui.unit.dp
import org.jetbrains.skia.EncodedImageFormat
import java.awt.image.BufferedImage
import kotlin.math.abs

/**
 * Headless check of the Player & mini-player design screen against the preview,
 * run with
 *
 *     ./gradlew :desktop:playerDesignOrderCheck
 *
 * The preview's `playerDesign` list is one column: the title, the player's own
 * choice in a card of one row, the full player's card (background, the turning
 * artwork), the mini player's card (design, background, its pure black finish,
 * the swipe and the sensitivity that switch turns on), then the slider style in a
 * card of its own.
 *
 * Two harnesses, for the two kinds of fact:
 *
 *  * **the ui-test one** composes the real `SettingsPlayerDesignScreen` and reads
 *    the semantics tree, so the page's own text is the input: every text of the
 *    screen, in document order, must be the preview's own sequence;
 *  * **the pixel one** renders the same screen into an off-screen
 *    `ImageComposeScene` and reads the column of card fill, which is the only way
 *    to say how the rows are divided into cards: four cards of 1 + 2 + 5 + 1 rows,
 *    the page's 16 dp between two cards and 3 dp between the rows of one, and the
 *    groups fading up on arrival (the frame the screen lands on paints no card).
 *
 * `playerDesignRenderCheck` already reads the same division from the screen
 * without the Back row; this check owns the column's order and the heading, and
 * reads the division again here so a screen rebuilt into loose cards fails in one
 * place rather than passing for the wrong reason.
 */
object PlayerDesignOrderCheck {

    private const val WIDTH = 700

    private const val HEIGHT = 1600

    private val accent = Color(0xFFED5564)

    /** A rectangle read off the test's semantics tree, in root px. */
    private data class Rect(val left: Float, val top: Float, val right: Float, val bottom: Float)

    /** A run of card fill down the measured column, in pixels from the top. */
    private data class Band(val top: Int, val bottom: Int) {
        val height: Int get() = bottom - top + 1
    }

    /**
     * Every text the screen should show, in the order it should show them.
     *
     * The preview's column, read from the app's own strings and from the same
     * label builder the row uses, so a wording change reaches this check instead
     * of leaving it comparing the page against a stale copy of itself.
     */
    private val expected: List<String> = buildList {
        add("\u2039 Back")
        add(Localization.get("en", "player_and_mini_player_design"))
        add(Localization.get("en", "player_design"))
        add(playerDesignLabel("en", PlayerDesign.EXPRESSIVE))
        add(Localization.get("en", "player_background"))
        add(Localization.get("en", "player_background_blur"))
        add(Localization.get("en", "rotating_thumbnail"))
        add(Localization.get("en", "mini_player_design"))
        add(Localization.get("en", "mini_player_classic"))
        add(Localization.get("en", "mini_player_background"))
        add(Localization.get("en", "mini_player_bg_follow_theme"))
        add(Localization.get("en", "pure_black_mini"))
        add(Localization.get("en", "enable_swipe_thumbnail"))
        add(Localization.get("en", "enable_swipe_thumbnail_desc"))
        add(Localization.get("en", "swipe_sensitivity"))
        add(Localization.get("en", "swipe_sensitivity_desc"))
        // The swipe's value lives in the row's trailing slot, the page's own
        // `.md-slider__value`, and not in its title.
        add("73%")
        add(Localization.get("en", "slider_style"))
        add(Localization.get("en", "slider_slim"))
    }

    @JvmStatic
    fun main(args: Array<String>) {
        val failures = mutableListOf<String>()

        var read: List<String> = emptyList()
        var back: Rect? = null
        var title: Rect? = null

        runComposeUiTest {
            setContent { content() }
            waitForIdle()

            val texts = mutableListOf<String>()
            fun walk(node: SemanticsNode) {
                val config = node.config
                if (config.contains(SemanticsProperties.Text)) {
                    config[SemanticsProperties.Text].forEach { texts += it.text }
                }
                node.children.forEach { walk(it) }
            }
            walk(onRoot().fetchSemanticsNode())
            read = texts

            back = onNodeWithText("\u2039 Back").firstRectOrNull()
            title = onNodeWithText(Localization.get("en", "player_and_mini_player_design")).firstRectOrNull()
        }

        // ------------------------------------------------------ the sequence
        if (read == expected) {
            println("order: ${read.size} texts, the preview's own column")
        } else {
            failures += "the screen's texts are not the preview's column: read $read"
        }

        // ------------------------------------------------------ the heading
        val b = back
        val t = title
        if (t != null && b != null) {
            val gap = t.top - b.bottom
            println("heading: back ends ${b.bottom.toInt()}, title starts ${t.top.toInt()} (gap ${gap.toInt()})")
            if (abs(gap - 16f) > 2f) {
                failures += "the heading must stand 16 dp under the back row, read ${gap.toInt()}"
            }
        } else {
            failures += "the heading could not be measured"
        }

        // ------------------------------------------------------ the cards
        val bands = cards(settledFrame())
        val spans = bands.joinToString { band -> "${band.top}..${band.bottom}" }
        println("cards: ${bands.size} band(s) $spans")
        // 1 + 2 + 5 + 1 rows: the player's choice alone, the full player's two,
        // the mini player's five (the sensitivity rides the switch it tunes) and
        // the slider style on its own.
        if (bands.size != 9) {
            failures += "the screen must be four cards of 1 + 2 + 5 + 1 rows (9 bands), read ${bands.size}"
        } else {
            val seams = bands.zipWithNext { a, next -> next.top - a.bottom - 1 }
            println("seams: $seams")
            // 3 dp inside a card; 16 dp after the 1st row, after the 3rd and
            // after the 8th, which is where the four cards meet.
            val expectedSeams = listOf(16, 3, 16, 3, 3, 3, 3, 16)
            if (seams != expectedSeams) {
                failures += "the rows must join as $expectedSeams, read $seams"
            }
            bands.forEach { band ->
                if (band.height < 40) {
                    failures += "every row must be a row tall, read ${band.height}"
                }
            }
        }
        if (t != null && bands.isNotEmpty()) {
            val underTitle = bands[0].top - t.bottom
            println("heading to card: title ends ${t.bottom.toInt()}, the first card starts ${bands[0].top} (gap ${underTitle.toInt()})")
            if (abs(underTitle - 16f) > 3f) {
                failures += "the first card must stand 16 dp under the heading, read ${underTitle.toInt()}"
            }
        }

        // ------------------------------------------------------ the entrance
        val arrival = cards(arrivalFrame())
        println("entrance: ${arrival.size} band(s) on the arrival frame against ${bands.size} once settled")
        if (bands.size != 9) {
            // Nothing to read: the shape failure above already says why.
        } else if (arrival.isNotEmpty()) {
            failures += "a group must fade up as the screen arrives: the arrival frame already paints " +
                "${arrival.size} card(s)"
        }

        if (failures.isEmpty()) {
            println()
            println(
                "OK: Player & mini-player design is the preview's column - the heading 16 dp under the back " +
                    "row and the first card 16 dp under it, the player's choice alone, the full player's two " +
                    "rows, the mini player's five (the sensitivity riding its switch, 73% in the row's " +
                    "trailing slot) and the slider style on its own, on the page's 16/3 dp seams, the four " +
                    "groups fading up on arrival",
            )
            return
        }
        println()
        println("FAILED:")
        failures.forEach { println("  - $it") }
        throw IllegalStateException("player design order check failed (${failures.size} case(s))")
    }

    /**
     * The screen for both harnesses.
     *
     * [arriving] is what the app's own screen host provides on a navigation: a
     * group only fades up when one has just put its screen on show, which is the
     * preview's `screen--enter`.
     */
    @androidx.compose.runtime.Composable
    private fun content(arriving: Boolean = false) {
        AppTheme(mode = ThemeMode.LIGHT, accent = accent, language = "en") {
            Box(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.surface)) {
                Box(Modifier.size(WIDTH.dp, HEIGHT.dp)) {
                    CompositionLocalProvider(LocalScreenArrival provides arriving) {
                        SettingsPlayerDesignScreen(
                            language = "en",
                            onBack = {},
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
                            sliderStyle = "slim",
                            onSliderStyleChange = {},
                            // On, so the sensitivity row is drawn: it is the last
                            // row of the mini player's card and the page's own
                            // rule (`if (swipeThumbnail)`).
                            swipeThumbnail = true,
                            onSwipeThumbnailChange = {},
                            swipeSensitivity = 0.73f,
                            onSwipeSensitivityChange = {},
                        )
                    }
                }
            }
        }
    }

    @OptIn(ExperimentalComposeUiApi::class)
    private fun scene(arriving: Boolean = false): ImageComposeScene =
        ImageComposeScene(WIDTH, HEIGHT) { content(arriving) }

    /** The frame the screen arrives on. */
    private fun arrivalFrame(): BufferedImage {
        val scene = scene(arriving = true)
        try {
            return png(scene.render(1_000_000_000L))
        } finally {
            scene.close()
        }
    }

    /** The same screen once the entrance has had its time. */
    private fun settledFrame(): BufferedImage {
        val scene = scene(arriving = true)
        try {
            scene.render(1_000_000_000L)
            scene.render(1_100_000_000L)
            return png(scene.render(1_600_000_000L))
        } finally {
            scene.close()
        }
    }

    private fun png(image: org.jetbrains.skia.Image): BufferedImage =
        image.encodeToData(EncodedImageFormat.PNG)?.bytes
            ?.let { javax.imageio.ImageIO.read(it.inputStream()) }
            ?: error("the scene produced no image")

    /**
     * The runs of card fill down the middle of the frame.
     *
     * A card is not one run: its rows are separated by the 3 dp seam the page
     * shows through, so a card of five rows reads as five bands - which is the
     * point, because the seams are then measurable. The title's own glyphs are a
     * short run of ink, not a row, so any run shorter than 40 px is dropped.
     */
    private fun cards(image: BufferedImage): List<Band> {
        val x = image.width / 2
        val page = image.getRGB(x, 2)
        val runs = mutableListOf<Pair<Boolean, Int>>()
        for (y in 0 until image.height) {
            val onCard = !near(image.getRGB(x, y), page)
            if (runs.isNotEmpty() && runs.last().first == onCard) {
                runs[runs.size - 1] = onCard to runs.last().second + 1
            } else {
                runs += onCard to 1
            }
        }
        val bands = mutableListOf<Band>()
        var offset = 0
        for (run in runs) {
            if (run.first && run.second >= 40) bands += Band(offset, offset + run.second - 1)
            offset += run.second
        }
        return bands
    }

    private fun near(a: Int, b: Int): Boolean {
        val dr = abs(((a shr 16) and 255) - ((b shr 16) and 255))
        val dg = abs(((a shr 8) and 255) - ((b shr 8) and 255))
        val db = abs((a and 255) - (b and 255))
        return dr + dg + db <= 6
    }

    private fun SemanticsNodeInteraction.firstRectOrNull(): Rect? {
        val node = try {
            fetchSemanticsNode()
        } catch (missing: AssertionError) {
            return null
        }
        val r = node.boundsInRoot
        return Rect(r.left, r.top, r.right, r.bottom)
    }
}
