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
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.runComposeUiTest
import androidx.compose.ui.unit.dp
import org.jetbrains.skia.EncodedImageFormat
import java.awt.image.BufferedImage
import kotlin.math.abs

/**
 * Headless check of the Canvas screen against the preview, run with
 *
 *     ./gradlew :desktop:canvasOrderCheck
 *
 * The preview's `canvas` list is a title, one card of one row (the Canvas's own
 * switch) and, while that switch is on, the source in a card of its own: the page
 * hides the choice when the Canvas is off, because a source for something that
 * never draws is a control with nothing behind it.
 *
 * Two harnesses, for the two kinds of fact:
 *
 *  * **the ui-test one** composes the real `SettingsCanvasScreen` and reads the
 *    semantics tree, on and off, so the page's own text is the input: every text
 *    of the screen, in document order, must be the preview's own sequence, and
 *    the source's row must be gone when the Canvas is;
 *  * **the pixel one** renders the same screen into an off-screen
 *    `ImageComposeScene` and reads the column of card fill: two cards of one row
 *    each, the page's 16 dp between them, one card while the Canvas is off, and
 *    the groups fading up on arrival (the frame the screen lands on paints
 *    nothing).
 */
object CanvasOrderCheck {

    private const val WIDTH = 700

    private const val HEIGHT = 900

    private val accent = Color(0xFFED5564)

    /** A rectangle read off the test's semantics tree, in root px. */
    private data class Rect(val left: Float, val top: Float, val right: Float, val bottom: Float)

    /** A run of card fill down the measured column, in pixels from the top. */
    private data class Band(val top: Int, val bottom: Int) {
        val height: Int get() = bottom - top + 1
    }

    /**
     * Every text the screen should show with the Canvas on, in order.
     *
     * The preview's column, read from the app's own strings rather than retyped:
     * the page's heading and its row are both the Canvas's own name, and the
     * source's value is the label of the stored source.
     */
    private val expectedOn: List<String> = listOf(
        "\u2039 Back",
        Localization.get("en", "vivimusic_canvas"),
        Localization.get("en", "vivimusic_canvas"),
        Localization.get("en", "canvas_source"),
        Localization.get("en", "canvas_source_auto"),
    )

    /** With the Canvas off the source is not on the screen at all. */
    private val expectedOff: List<String> = listOf(
        "\u2039 Back",
        Localization.get("en", "vivimusic_canvas"),
        Localization.get("en", "vivimusic_canvas"),
    )

    @JvmStatic
    fun main(args: Array<String>) {
        val failures = mutableListOf<String>()

        var readOn: List<String> = emptyList()
        var readOff: List<String> = emptyList()
        var back: Rect? = null
        var title: Rect? = null

        runComposeUiTest {
            setContent { content(canvasEnabled = true) }
            waitForIdle()

            fun texts(): List<String> {
                val out = mutableListOf<String>()
                fun walk(node: SemanticsNode) {
                    val config = node.config
                    if (config.contains(SemanticsProperties.Text)) {
                        config[SemanticsProperties.Text].forEach { out += it.text }
                    }
                    node.children.forEach { walk(it) }
                }
                walk(onRoot().fetchSemanticsNode())
                return out
            }

            readOn = texts()
            back = onNodeWithText("\u2039 Back").firstRectOrNull()
            // The heading and the switch's row carry the same word, so the first
            // of the two is the heading: the page's own `{type:'title'}`.
            title = onAllNodesWithText(Localization.get("en", "vivimusic_canvas"))[0].firstRectOrNull()
        }

        runComposeUiTest {
            setContent { content(canvasEnabled = false) }
            waitForIdle()

            val out = mutableListOf<String>()
            fun walk(node: SemanticsNode) {
                val config = node.config
                if (config.contains(SemanticsProperties.Text)) {
                    config[SemanticsProperties.Text].forEach { out += it.text }
                }
                node.children.forEach { walk(it) }
            }
            walk(onRoot().fetchSemanticsNode())
            readOff = out
        }

        // ------------------------------------------------------ the sequence
        if (readOn == expectedOn) {
            println("order, Canvas on: ${readOn.size} texts, the preview's own column")
        } else {
            failures += "with the Canvas on the screen's texts are not the preview's: read $readOn"
        }
        if (readOff == expectedOff) {
            println("order, Canvas off: ${readOff.size} texts, the source's row gone with the switch")
        } else {
            failures += "with the Canvas off the screen should keep its switch alone: read $readOff"
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
        val bands = cards(settledFrame(canvasEnabled = true))
        val spans = bands.joinToString { band -> "${band.top}..${band.bottom}" }
        println("cards, Canvas on: ${bands.size} band(s) $spans")
        if (bands.size != 2) {
            failures += "the Canvas on must be two cards of one row (2 bands), read ${bands.size}"
        } else {
            val seam = bands[1].top - bands[0].bottom - 1
            println("cards: the first ends ${bands[0].bottom}, the second starts ${bands[1].top} (gap $seam)")
            if (abs(seam - 16) > 1) {
                failures += "two cards must stand 16 dp apart, read $seam"
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

        val offBands = cards(settledFrame(canvasEnabled = false))
        println("cards, Canvas off: ${offBands.size} band(s)")
        if (offBands.size != 1) {
            failures += "the Canvas off must be its switch alone (1 band), read ${offBands.size}"
        }

        // ------------------------------------------------------ the entrance
        val arrival = cards(arrivalFrame())
        println("entrance: ${arrival.size} band(s) on the arrival frame against ${bands.size} once settled")
        if (bands.size != 2) {
            // Nothing to read: the shape failure above already says why.
        } else if (arrival.isNotEmpty()) {
            failures += "a group must fade up as the screen arrives: the arrival frame already paints " +
                "${arrival.size} card(s)"
        }

        if (failures.isEmpty()) {
            println()
            println(
                "OK: Canvas is the preview's - the heading 16 dp under the back row and the switch's card " +
                    "16 dp under it, the source in a card of its own while the Canvas is on and gone while " +
                    "it is off, and both groups fading up on arrival",
            )
            return
        }
        println()
        println("FAILED:")
        failures.forEach { println("  - $it") }
        throw IllegalStateException("canvas order check failed (${failures.size} case(s))")
    }

    /**
     * The screen for both harnesses.
     *
     * [arriving] is what the app's own screen host provides on a navigation: a
     * group only fades up when one has just put its screen on show, which is the
     * preview's `screen--enter`.
     */
    @androidx.compose.runtime.Composable
    private fun content(canvasEnabled: Boolean, arriving: Boolean = false) {
        AppTheme(mode = ThemeMode.LIGHT, accent = accent, language = "en") {
            Box(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.surface)) {
                Box(Modifier.size(WIDTH.dp, HEIGHT.dp)) {
                    CompositionLocalProvider(LocalScreenArrival provides arriving) {
                        SettingsCanvasScreen(
                            language = "en",
                            onBack = {},
                            canvasEnabled = canvasEnabled,
                            onCanvasEnabledChange = {},
                            canvasSource = CanvasSource.AUTO,
                            onCanvasSourceChange = {},
                        )
                    }
                }
            }
        }
    }

    @OptIn(ExperimentalComposeUiApi::class)
    private fun scene(canvasEnabled: Boolean, arriving: Boolean = false): ImageComposeScene =
        ImageComposeScene(WIDTH, HEIGHT) { content(canvasEnabled = canvasEnabled, arriving = arriving) }

    /** The frame the screen arrives on, with the Canvas on. */
    private fun arrivalFrame(): BufferedImage {
        val scene = scene(canvasEnabled = true, arriving = true)
        try {
            return png(scene.render(1_000_000_000L))
        } finally {
            scene.close()
        }
    }

    /** The same screen once the entrance has had its time. */
    private fun settledFrame(canvasEnabled: Boolean): BufferedImage {
        val scene = scene(canvasEnabled = canvasEnabled, arriving = true)
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
     * shows through, so a card of two rows reads as two bands. The title's own
     * glyphs are a short run of ink, not a row, so any run shorter than 40 px is
     * dropped.
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
