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
 * Headless check of the Appearance screen against the preview, run with
 *
 *     ./gradlew :desktop:appearanceOrderCheck
 *
 * The preview's `appearance` list (`js/screens.js`) is two cards under one title:
 * six doors - theme, the player and mini player's design, the canvas, the font,
 * the display density and the intro - then three rows that act here instead of
 * elsewhere: the animations door, and the two switches (the native title bar,
 * with its restart and its hint, and the right Now Playing panel).
 *
 * Two harnesses, for the two kinds of fact:
 *
 *  * **the ui-test one** composes the real `SettingsAppearanceScreen` and reads
 *    the semantics tree, so the page's own text is the input: every text of the
 *    screen, in document order, must be the preview's own sequence - which is
 *    also how the six doors are proven to carry no sentence (the preview draws
 *    none) and the intro row its one;
 *  * **the pixel one** renders the same screen into an off-screen
 *    `ImageComposeScene` and reads the column of card fill: six bands for the
 *    first card and three for the second, so two cards and not nine, joined by
 *    the 3 dp seam inside each and standing the page's 16 dp apart. The arrival
 *    frame is read too: a group fades up there, so it paints nothing.
 */
object AppearanceOrderCheck {

    private const val WIDTH = 640

    private const val HEIGHT = 1200

    /** A rectangle read off the test's semantics tree, in root px. */
    private data class Rect(val left: Float, val top: Float, val right: Float, val bottom: Float)

    /** A run of card fill down the measured column, in pixels from the top. */
    private data class Band(val top: Int, val bottom: Int) {
        val height: Int get() = bottom - top + 1
    }

    /**
     * Every text the screen should show, in the order it should show them.
     *
     * The preview's two cards in its own order, read from the app's own strings
     * rather than retyped, so a wording change reaches this check instead of
     * leaving it comparing the page against a stale copy of itself.
     */
    private val expected = listOf(
        "\u2039 Back",
        "appearance",
        "theme_colors",
        "player_and_mini_player_design",
        // The preview's own title for the canvas screen, which is shorter than
        // the `vivimusic_canvas` the hub used to print.
        "vivimusic_canvas",
        "app_font",
        "density_and_grid",
        "intro",
        "show_intro_on_startup",
        "animations",
        "animations_hub_desc",
        "native_title_bar",
        "native_title_bar_desc",
        "native_title_bar_desc_hint",
        "right_panel",
        "right_panel_desc",
    ).map { if (it == "\u2039 Back") it else Localization.get("en", it) }

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
            fun walk(node: androidx.compose.ui.semantics.SemanticsNode) {
                val config = node.config
                if (config.contains(androidx.compose.ui.semantics.SemanticsProperties.Text)) {
                    config[androidx.compose.ui.semantics.SemanticsProperties.Text]
                        .forEach { texts += it.text }
                }
                node.children.forEach { walk(it) }
            }
            walk(onRoot().fetchSemanticsNode())
            read = texts

            back = onNodeWithText("\u2039 Back").rectOrNull()
            title = onNodeWithText(Localization.get("en", "appearance")).rectOrNull()
        }

        // ------------------------------------------------------ the sequence
        if (read == expected) {
            println("order: ${read.size} texts, the preview's own sequence")
            println("cards: the six doors, then the animations row and the two switches")
        } else {
            failures += "the screen's texts are not the preview's: read $read"
        }

        // ------------------------------------------------------ the heading
        val t = title
        val b = back
        if (t != null && b != null) {
            val heading = t.top - b.bottom
            println("heading: back ends ${b.bottom.toInt()}, title starts ${t.top.toInt()} (gap ${heading.toInt()})")
            if (abs(heading - 16f) > 2f) {
                failures += "the heading must stand 16 dp under the back row, read a gap of ${heading.toInt()}"
            }
        } else {
            failures += "the heading could not be measured"
        }

        // ------------------------------------------------------ the cards
        val bands = cards(settledFrame())
        println("cards: ${bands.size} band(s) ${bands.map { "${it.top}..${it.bottom}" }}")
        // Six doors in the first card, three rows in the second: two cards, not
        // nine rows standing loose, and the bands are what tells the difference.
        if (bands.size != 9) {
            failures += "the page must be the title and two cards of six and three rows (9 bands), " +
                "read ${bands.size}"
        } else {
            val between = bands[6].top - bands[5].bottom - 1
            println("cards: the first card ends ${bands[5].bottom}, the second starts ${bands[6].top} (gap $between)")
            if (abs(between - 16) > 2) {
                failures += "two cards must stand 16 dp apart, read $between"
            }
            // The seam inside each card, read where the preview draws it: 3 dp
            // between two rows, and the page's own 16 dp between the two cards.
            val seams = (1..5).map { bands[it].top - bands[it - 1].bottom - 1 } +
                (7..8).map { bands[it].top - bands[it - 1].bottom - 1 }
            println("seams: $seams")
            seams.forEach { seam ->
                if (abs(seam - 3) > 1) {
                    failures += "a card's rows must be joined by the 3 dp seam, read $seam"
                }
            }
            val short = bands.filter { it.height < 40 }
            if (short.isNotEmpty()) {
                failures += "every row must be a row tall, read ${short.map { it.height }}"
            }
        }
        if (t != null && bands.isNotEmpty()) {
            val underTitle = bands[0].top - t.bottom
            println("heading to card: title ends ${t.bottom.toInt()}, card starts ${bands[0].top} (gap ${underTitle.toInt()})")
            if (abs(underTitle - 16f) > 3f) {
                failures += "the first card must stand 16 dp under the heading, read ${underTitle.toInt()}"
            }
        }

        // ------------------------------------------------------ the entrance
        val arrival = cards(firstFrame())
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
                "OK: Appearance is the preview's - the heading 16 dp under the back row and the first " +
                    "card 16 dp under it, the six doors with no sentences, then the animations row with " +
                    "its own and the two switches with theirs, two cards 16 dp apart on their 3 dp seams, " +
                    "and both groups fading up on arrival",
            )
            return
        }
        println()
        println("FAILED:")
        failures.forEach { println("  - $it") }
        throw IllegalStateException("appearance order check failed (${failures.size} case(s))")
    }

    /**
     * The same screen for the off-screen scene, which has no test harness.
     *
     * [arriving] is what the app's own screen host provides on a navigation: a
     * group only fades up when one has just put its screen on show, which is the
     * preview's `screen--enter`.
     */
    @androidx.compose.runtime.Composable
    private fun content(arriving: Boolean = false) {
        AppTheme(mode = ThemeMode.LIGHT, accent = Color(0xFFED5564), language = "en") {
            Box(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.surface)) {
                Box(Modifier.size(WIDTH.dp, HEIGHT.dp)) {
                    CompositionLocalProvider(LocalScreenArrival provides arriving) {
                        SettingsAppearanceScreen(
                            language = "en",
                            onBack = {},
                            // The switches' own two values: off and on, so the rows
                            // are drawn as the preview draws them and not folded.
                            nativeTitleBar = false,
                            showRightSidebar = true,
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
    private fun firstFrame(): BufferedImage {
        val scene = scene(arriving = true)
        try {
            return png(scene.render(1_000_000_000L))
        } finally {
            scene.close()
        }
    }

    /** The same screen once an entrance has had its time. */
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

    /**
     * The runs of card fill down the column.
     *
     * A card is not one run: its rows are separated by the 3 dp seam the page
     * shows through, so a card of six rows reads as six bands - which is the
     * point, because the seams are then measurable.
     */
    private fun cards(image: BufferedImage): List<Band> {
        val page = image.getRGB(2, 2)
        val runs = mutableListOf<Band>()
        var start = -1
        for (y in 0 until image.height) {
            val filled = !near(image.getRGB(40, y), page)
            if (filled && start < 0) start = y
            if (!filled && start >= 0) {
                if (y - start >= 20) runs += Band(start, y - 1)
                start = -1
            }
        }
        if (start >= 0 && image.height - start >= 20) runs += Band(start, image.height - 1)
        return runs
    }

    private fun near(a: Int, b: Int): Boolean {
        val dr = abs(((a shr 16) and 255) - ((b shr 16) and 255))
        val dg = abs(((a shr 8) and 255) - ((b shr 8) and 255))
        val db = abs((a and 255) - (b and 255))
        return dr + dg + db <= 6
    }

    private fun png(image: org.jetbrains.skia.Image): BufferedImage =
        image.encodeToData(EncodedImageFormat.PNG)?.bytes
            ?.let { javax.imageio.ImageIO.read(it.inputStream()) }
            ?: error("the scene produced no image")

    private fun SemanticsNodeInteraction.rectOrNull(): Rect? {
        val node = try {
            fetchSemanticsNode()
        } catch (missing: AssertionError) {
            return null
        }
        val r = node.boundsInRoot
        return Rect(r.left, r.top, r.right, r.bottom)
    }
}
