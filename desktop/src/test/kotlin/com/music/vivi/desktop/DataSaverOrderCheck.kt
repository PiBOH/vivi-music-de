@file:OptIn(ExperimentalTestApi::class)

package com.music.vivi.desktop

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.ImageComposeScene
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.state.ToggleableState
import androidx.compose.ui.test.ComposeUiTest
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.SemanticsNodeInteraction
import androidx.compose.ui.test.hasClickAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.isToggleable
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.runComposeUiTest
import androidx.compose.ui.unit.dp
import org.jetbrains.skia.EncodedImageFormat
import java.awt.image.BufferedImage
import kotlin.math.abs

/**
 * Headless check of the Data Saver screen against the preview, run with
 *
 *     ./gradlew :desktop:dataSaverOrderCheck
 *
 * The preview's `dataSaver` list (`js/screens.js`) is the title, one card of the
 * one switch with the sentence that says what it turns off, and then the app's
 * own info card - the caption in the accent and one glyph with one line per
 * item - 24 dp under the switch's card. The five things the switch turns off are
 * information, not five more switches, so they are rows of that card and not a
 * list of bullets under a loose label.
 *
 * The check composes the real screen with each state of the switch and reads:
 * the sequence of texts, the heading 16 dp under the back row, the switch card
 * 16 dp under it, the info card 24 dp under the switch card with its five lines
 * 14 dp apart, both states of the switch, and the press that reaches the screen's
 * own preference instead of announcing itself.
 */
object DataSaverOrderCheck {

    private const val WIDTH = 640

    private const val HEIGHT = 900

    /** A rectangle read off the test's semantics tree, in root px. */
    private data class Rect(val left: Float, val top: Float, val right: Float, val bottom: Float)

    /** A run of card fill down the measured column, in pixels from the top. */
    private data class Band(val top: Int, val bottom: Int) {
        val height: Int get() = bottom - top + 1
    }

    private fun string(key: String) = Localization.get("en", key)

    /** The five things the switch turns off, in the page's own order. */
    private val items = listOf(
        "data_saver_player_canvas",
        "data_saver_artist_video",
        "data_saver_artist_bg_video",
        "data_saver_album_canvas",
        "data_saver_high_quality_images",
    )

    /** Every text the screen should show, in the order it should show them. */
    private val expected = listOf(
        "\u2039 Back",
        string("data_saver"),
        string("data_saver"),
        string("data_saver_desc"),
        string("data_saver_turns_off_header"),
    ) + items.map { string(it) }

    @JvmStatic
    fun main(args: Array<String>) {
        val failures = mutableListOf<String>()

        var read: List<String> = emptyList()
        var back: Rect? = null
        var title: Rect? = null
        var header: Rect? = null
        var lastItem: Rect? = null
        var lineOffsets: List<Int> = emptyList()
        var state: ToggleableState? = null

        runComposeUiTest {
            setContent { content(dataSaver = false) }
            waitForIdle()

            read = texts()
            back = onNodeWithText("\u2039 Back").rectOrNull()
            title = firstWithText(string("data_saver")).rectOrNull()
            header = onNodeWithText(string("data_saver_turns_off_header")).rectOrNull()
            lastItem = onNodeWithText(string(items.last())).rectOrNull()
            lineOffsets = items.map { key -> onNodeWithText(string(key)).rectOrNull()?.top?.toInt() ?: -1 }
            state = onNode(isToggleable()).fetchSemanticsNode().config[SemanticsProperties.ToggleableState]
        }

        // ------------------------------------------------------ the sequence
        if (read == expected) {
            println("order: ${read.size} texts, the preview's own sequence")
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

        // ------------------------------------------------------ the two blocks
        // Two runs of fill: the switch's card and the info card under it, which is
        // a surface of the page's own low container rather than a settings card, so
        // it reads as one block and not as rows joined by the 3 dp seam.
        val blocks = bands(settledFrame(dataSaver = false))
        println("cards: ${blocks.size} band(s) ${blocks.map { "${it.top}..${it.bottom}" }}")
        if (blocks.size != 2) {
            failures += "the page is the switch's card and the info card (2 blocks), read ${blocks.size}"
        } else {
            val between = blocks[1].top - blocks[0].bottom - 1
            println("cards: the switch card ends ${blocks[0].bottom}, the info card starts ${blocks[1].top} (gap $between)")
            if (between < 21 || between > 26) {
                failures += "the info card must stand the page's 24 dp under the switch's card, read $between"
            }
        }
        if (t != null && blocks.isNotEmpty()) {
            val underTitle = blocks[0].top - t.bottom
            println("heading to card: title ends ${t.bottom.toInt()}, card starts ${blocks[0].top} (gap ${underTitle.toInt()})")
            if (abs(underTitle - 16f) > 3f) {
                failures += "the switch's card must stand 16 dp under the heading, read ${underTitle.toInt()}"
            }
        }

        // ------------------------------------------------------ the info card
        val h = header
        if (h != null && blocks.size == 2) {
            // The caption sits the card's own 16 dp in from its top edge, and the
            // card's 24 dp margin is the gap already measured between the two runs.
            val inTop = h.top - blocks[1].top
            println("info card: caption top ${h.top.toInt()} inside the card's ${blocks[1].top} ($inTop)")
            if (inTop < 13f || inTop > 19f) {
                failures += "the caption must stand the card's 16 dp inside it, read ${inTop.toInt()}"
            }
            val li = lastItem
            if (li != null) {
                val bottom = blocks[1].bottom - li.bottom
                println("info card: last line bottom ${li.bottom.toInt()} inside the card's ${blocks[1].bottom} ($bottom)")
                if (bottom < 11f || bottom > 20f) {
                    failures += "the last line must stand the card's 16 dp inside it, read ${bottom.toInt()}"
                }
            }
        }
        // The five lines on the page's 14 dp rhythm: the distance from one line to
        // the next is a line tall plus 14 dp, and it is the same for all four steps.
        val steps = lineOffsets
        println("info rows: ${steps.size} line(s), steps $steps")
        if (steps.size != items.size) {
            failures += "the info card must draw ${items.size} lines, read ${steps.size}"
        } else {
            val gaps = (1 until steps.size).map { steps[it] - steps[it - 1] }
            gaps.forEach { gap ->
                if (gap < 30 || gap > 40) {
                    failures += "the info card's lines must stand 14 dp apart, read a step of $gap"
                }
            }
            if (gaps.max() - gaps.min() > 2) {
                failures += "the five lines must sit on one rhythm, read $gaps"
            }
        }

        // ------------------------------------------------------ the two states
        if (state != ToggleableState.Off) {
            failures += "with the preference off the switch must read off, read $state"
        }
        var onState: ToggleableState? = null
        runComposeUiTest {
            setContent { content(dataSaver = true) }
            waitForIdle()
            onState = onNode(isToggleable()).fetchSemanticsNode().config[SemanticsProperties.ToggleableState]
        }
        println("states: off reads $state, on reads $onState")
        if (onState != ToggleableState.On) {
            failures += "with the preference on the switch must read on, read $onState"
        }

        // ------------------------------------------------------ the entrance
        val arrival = bands(firstFrame())
        println("entrance: ${arrival.size} band(s) on the arrival frame against ${blocks.size} once settled")
        if (blocks.size == 2 && arrival.size != 1) {
            failures += "the switch's card fades up as the screen arrives while the info card is painted at once: " +
                "the arrival frame must show the info card alone, read ${arrival.size} band(s)"
        }

        // ------------------------------------------------------ the press acts
        var asked: Boolean? = null
        runComposeUiTest {
            setContent {
                var on by remember { mutableStateOf(false) }
                statefulContent(
                    dataSaver = on,
                    onToggle = {
                        asked = it
                        on = it
                    },
                )
            }
            waitForIdle()
            // The row itself, not only the switch's own knob: the page lets the
            // whole row turn the setting.
            onNode(hasClickAction() and hasText(string("data_saver"))).performClick()
            waitForIdle()
            val after = onNode(isToggleable()).fetchSemanticsNode().config[SemanticsProperties.ToggleableState]
            println("press: asked for $asked, the switch reads ${after == ToggleableState.On}")
            if (asked != true) {
                failures += "a press on the row must ask for the other state, read $asked"
            }
            if (after != ToggleableState.On) {
                failures += "the switch must follow the preference the press set, read $after"
            }
        }

        if (failures.isEmpty()) {
            println()
            println(
                "OK: Data Saver is the preview's - the heading 16 dp under the back row and the switch's card " +
                    "16 dp under it, the sentence in the row it belongs to, the app's own info card 24 dp below " +
                    "with its caption and five lines on the 14 dp rhythm, both states of the switch read from the " +
                    "preference, and a press on the row that really changes it",
            )
            return
        }
        println()
        println("FAILED:")
        failures.forEach { println("  - $it") }
        throw IllegalStateException("data saver order check failed (${failures.size} case(s))")
    }

    /** The first node carrying a text, in document order. */
    private fun ComposeUiTest.firstWithText(text: String): SemanticsNodeInteraction =
        onAllNodes(hasText(text))[0]

    /** Every text of the composed screen, in document order. */
    private fun ComposeUiTest.texts(): List<String> {
        val read = mutableListOf<String>()
        fun walk(node: androidx.compose.ui.semantics.SemanticsNode) {
            val config = node.config
            if (config.contains(SemanticsProperties.Text)) {
                config[SemanticsProperties.Text].forEach { read += it.text }
            }
            node.children.forEach { walk(it) }
        }
        walk(onRoot().fetchSemanticsNode())
        return read
    }

    /**
     * The same screen for the off-screen scene and for the press's own arm.
     *
     * [arriving] is what the app's own screen host provides on a navigation: the
     * switch's card only fades up when one has just put its screen on show, which
     * is the preview's `screen--enter`.
     */
    @androidx.compose.runtime.Composable
    private fun content(arriving: Boolean = false, dataSaver: Boolean = false) {
        AppTheme(mode = ThemeMode.LIGHT, accent = Color(0xFFED5564), language = "en") {
            Box(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.surface)) {
                Box(Modifier.size(WIDTH.dp, HEIGHT.dp)) {
                    CompositionLocalProvider(LocalScreenArrival provides arriving) {
                        SettingsDataSaverScreen(
                            language = "en",
                            onBack = {},
                            dataSaver = dataSaver,
                            onToggleDataSaver = {},
                        )
                    }
                }
            }
        }
    }

    /** The same screen over a preference the caller holds, for the press's arm. */
    @androidx.compose.runtime.Composable
    private fun statefulContent(dataSaver: Boolean, onToggle: (Boolean) -> Unit) {
        AppTheme(mode = ThemeMode.LIGHT, accent = Color(0xFFED5564), language = "en") {
            Box(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.surface)) {
                Box(Modifier.size(WIDTH.dp, HEIGHT.dp)) {
                    SettingsDataSaverScreen(
                        language = "en",
                        onBack = {},
                        dataSaver = dataSaver,
                        onToggleDataSaver = onToggle,
                    )
                }
            }
        }
    }

    @OptIn(ExperimentalComposeUiApi::class)
    private fun scene(arriving: Boolean = false, dataSaver: Boolean = false): ImageComposeScene =
        ImageComposeScene(WIDTH, HEIGHT) { content(arriving = arriving, dataSaver = dataSaver) }

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
    private fun settledFrame(dataSaver: Boolean = false): BufferedImage {
        val scene = scene(arriving = true, dataSaver = dataSaver)
        try {
            scene.render(1_000_000_000L)
            scene.render(1_100_000_000L)
            return png(scene.render(1_600_000_000L))
        } finally {
            scene.close()
        }
    }

    /**
     * The runs of fill down the column: the switch's card and the info card.
     *
     * The switch's card is one row, so it is one run; the info card is a surface of
     * its own, a tone away from the page, so it is the second run - its lines are
     * not seams but rows inside that surface, and they are read from their texts.
     */
    private fun bands(image: BufferedImage): List<Band> {
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
