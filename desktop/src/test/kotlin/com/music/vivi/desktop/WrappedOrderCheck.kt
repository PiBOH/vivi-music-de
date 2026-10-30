@file:OptIn(ExperimentalTestApi::class)

package com.music.vivi.desktop

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
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
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.runComposeUiTest
import androidx.compose.ui.unit.dp
import org.jetbrains.skia.EncodedImageFormat
import java.awt.image.BufferedImage
import kotlin.math.abs

/**
 * Headless check of the VIVI Wrapped screen against the preview, run with
 *
 *     ./gradlew :desktop:wrappedOrderCheck
 *
 * The preview's `wrapped` list (`js/screens.js`) is the heading and then one card
 * of one row - the Home glyph, *Show on Home*, its sentence and the switch - 16 dp
 * under it. The page draws nothing else: the numbers of the session are the Home
 * card's subject, so the app's own block sits under the page's card.
 *
 * The check composes the real screen (`SettingsSubScreen` with [WrappedSection],
 * which is what [SettingsWrappedScreen] draws) in both states of the session - one
 * with a track start and a minute behind it, one with nothing - and reads: the
 * sequence of texts, the heading 16 dp under the back row, the card 16 dp under
 * it, the card's one row, both states of the switch, the press that reaches the
 * screen's own preference, the session card's place under the page's card, the
 * empty state that draws no zeroes, and the arrival frame.
 */
object WrappedOrderCheck {

    private const val WIDTH = 640

    private const val HEIGHT = 900

    /** A rectangle read off the test's semantics tree, in root px. */
    private data class Rect(val left: Float, val top: Float, val right: Float, val bottom: Float)

    /** A run of card fill down the measured column, in pixels from the top. */
    private data class Band(val top: Int, val bottom: Int) {
        val height: Int get() = bottom - top + 1
    }

    private fun string(key: String) = Localization.get("en", key)

    /** A session with something in it: 7 starts, 12 minutes, one song 3 times. */
    private val session = WrappedStats(
        trackStarts = 7,
        playedMs = 12 * 60_000L,
        topSongTitle = "Nightcall",
        topSongCount = 3,
    )

    /** The page's own texts, in its own order. */
    private val previewOrder = listOf(
        "\u2039 Back",
        string("wrapped_title"),
        string("wrapped_show_on_home"),
        string("wrapped_show_on_home_desc"),
    )

    /** The three figures of the app's own block, in the card's own order. */
    private val sessionTexts = listOf(
        "7",
        string("wrapped_tracks"),
        "12m",
        string("wrapped_listening_time"),
        "3",
        "Nightcall",
    )

    @JvmStatic
    fun main(args: Array<String>) {
        val failures = mutableListOf<String>()

        var read: List<String> = emptyList()
        var back: Rect? = null
        var title: Rect? = null

        runComposeUiTest {
            setContent { screen(stats = WrappedStats()) }
            waitForIdle()

            read = texts()
            back = onNodeWithText("\u2039 Back").rectOrNull()
            title = onNodeWithText(string("wrapped_title")).rectOrNull()
        }

        // ------------------------------------------------------ the sequence
        // An empty session is the preview's page and nothing else: no zeroes, no
        // card to read them from.
        if (read == previewOrder) {
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

        // ------------------------------------------------------ the page's card
        val blocks = bands(settledFrame(stats = WrappedStats()))
        println("cards: ${blocks.size} band(s) ${blocks.map { "${it.top}..${it.bottom}" }}, one row each")
        if (blocks.size != 1) {
            failures += "the page is one card with the empty session (1 block), read ${blocks.size}"
        }
        if (t != null && blocks.isNotEmpty()) {
            val under = blocks[0].top - t.bottom
            println("heading to card: title ends ${t.bottom.toInt()}, card starts ${blocks[0].top} (gap ${under.toInt()})")
            if (abs(under - 16f) > 3f) {
                failures += "the card must stand the page's 16 dp under the heading, read ${under.toInt()}"
            }
        }

        // ------------------------------------------------------ the session block
        // With a session behind it the app draws its own card under the page's,
        // on the page's own 16 dp, and the switch card is still the only row.
        var withSession: List<String> = emptyList()
        runComposeUiTest {
            setContent { screen(stats = session) }
            waitForIdle()
            withSession = texts()
        }
        val statsBands = bands(settledFrame(stats = session))
        println("cards: ${statsBands.size} band(s) ${statsBands.map { "${it.top}..${it.bottom}" }} with a session")
        if (statsBands.size != 2) {
            failures += "with a session the page's card and the app's own make 2 blocks, read ${statsBands.size}"
        } else {
            val between = statsBands[1].top - statsBands[0].bottom - 1
            println("cards: the switch card ends ${statsBands[0].bottom}, the session card starts ${statsBands[1].top} (gap $between)")
            if (abs(between - 16f) > 3f) {
                failures += "the session card must stand the page's 16 dp under the switch's, read $between"
            }
        }
        // The page's own texts first (the row's sentence is the last of them), then
        // the app's card under it.
        val expectedWithSession = previewOrder + sessionTexts
        if (withSession == expectedWithSession) {
            println("stats: ${withSession.size} texts with a session behind it, the three figures in the card's own order")
        } else {
            failures += "the session's figures are not the card's own: read $withSession"
        }

        // ------------------------------------------------------ the two states
        var offState: ToggleableState? = null
        var toggles = 0
        runComposeUiTest {
            setContent { screen(stats = WrappedStats(), showOnHome = false) }
            waitForIdle()
            offState = onNode(isToggleable()).fetchSemanticsNode().config[SemanticsProperties.ToggleableState]
            toggles = onAllNodesWithText(string("wrapped_show_on_home")).fetchSemanticsNodes().size
        }
        var onState: ToggleableState? = null
        runComposeUiTest {
            setContent { screen(stats = WrappedStats(), showOnHome = true) }
            waitForIdle()
            onState = onNode(isToggleable()).fetchSemanticsNode().config[SemanticsProperties.ToggleableState]
        }
        println("states: off reads $offState, on reads $onState, the row carries the name $toggles time(s)")
        if (offState != ToggleableState.Off) {
            failures += "with the preference off the switch must read off, read $offState"
        }
        if (onState != ToggleableState.On) {
            failures += "with the preference on the switch must read on, read $onState"
        }
        if (toggles != 1) {
            failures += "the page's one row is the switch: its name must be drawn once, read $toggles"
        }

        // ------------------------------------------------------ the entrance
        val arrival = bands(firstFrame())
        println("entrance: ${arrival.size} band(s) on the arrival frame against ${blocks.size} once settled")
        if (blocks.size == 1 && arrival.size != 0) {
            failures += "the card fades up as the screen arrives: the arrival frame must not show it, read ${arrival.size}"
        }

        // ------------------------------------------------------ the press acts
        var asked: Boolean? = null
        runComposeUiTest {
            setContent {
                var on by remember { mutableStateOf(false) }
                screen(
                    stats = WrappedStats(),
                    showOnHome = on,
                    onToggle = {
                        asked = it
                        on = it
                    },
                )
            }
            waitForIdle()
            // The whole row turns the switch, not only its knob.
            onNode(hasClickAction() and hasText(string("wrapped_show_on_home"))).performClick()
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

        // ------------------------------------------------------ the real screen
        var realTexts: List<String> = emptyList()
        runComposeUiTest {
            setContent {
                AppTheme(mode = ThemeMode.LIGHT, accent = Color(0xFFED5564), language = "en") {
                    Box(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.surface)) {
                        Box(Modifier.size(WIDTH.dp, HEIGHT.dp)) {
                            SettingsWrappedScreen(language = "en", onBack = {}, wrappedStats = WrappedStats())
                        }
                    }
                }
            }
            waitForIdle()
            realTexts = texts()
        }
        println("real: ${realTexts.size} text(s) from SettingsWrappedScreen, head ${realTexts.take(2)}")
        if (realTexts != previewOrder) {
            failures += "the app's own screen must be the page, read $realTexts"
        }

        if (failures.isEmpty()) {
            println()
            println(
                "OK: VIVI Wrapped is the preview's - the heading 16 dp under the back row, the one card of the one " +
                    "switch 16 dp under it with its glyph, its name and its sentence, the switch reading the " +
                    "preference in both states with a press on the row that really changes it, the session's card " +
                    "16 dp under the page's one while there is something to report and nothing at all when the " +
                    "session is empty, and the page fading up on arrival",
            )
            return
        }
        println()
        println("FAILED:")
        failures.forEach { println("  - $it") }
        throw IllegalStateException("wrapped order check failed (${failures.size} case(s))")
    }

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
     * The screen body the app's own screen draws: the back row through
     * [SettingsSubScreen], then [WrappedSection] over the state the caller holds.
     *
     * [arriving] is what the screen host provides on a navigation: the card fades
     * up when one has just put its screen on show, which is the page's
     * `screen--enter`.
     */
    @Composable
    private fun screen(
        arriving: Boolean = false,
        stats: WrappedStats,
        showOnHome: Boolean = false,
        onToggle: (Boolean) -> Unit = {},
    ) {
        AppTheme(mode = ThemeMode.LIGHT, accent = Color(0xFFED5564), language = "en") {
            Box(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.surface)) {
                Box(Modifier.size(WIDTH.dp, HEIGHT.dp)) {
                    CompositionLocalProvider(LocalScreenArrival provides arriving) {
                        SettingsSubScreen(language = "en", onBack = {}) {
                            WrappedSection(
                                language = "en",
                                wrappedStats = stats,
                                showWrappedOnHome = showOnHome,
                                onShowWrappedOnHomeChange = onToggle,
                            )
                        }
                    }
                }
            }
        }
    }

    @OptIn(ExperimentalComposeUiApi::class)
    private fun scene(arriving: Boolean = false, stats: WrappedStats): ImageComposeScene =
        ImageComposeScene(WIDTH, HEIGHT) { screen(arriving = arriving, stats = stats) }

    /** The frame the screen arrives on. */
    private fun firstFrame(): BufferedImage {
        val scene = scene(arriving = true, stats = WrappedStats())
        try {
            return png(scene.render(1_000_000_000L))
        } finally {
            scene.close()
        }
    }

    /** The same screen once an entrance has had its time. */
    private fun settledFrame(stats: WrappedStats): BufferedImage {
        val scene = scene(arriving = true, stats = stats)
        try {
            scene.render(1_000_000_000L)
            scene.render(1_100_000_000L)
            return png(scene.render(1_600_000_000L))
        } finally {
            scene.close()
        }
    }

    /**
     * The runs of card fill down a column clear of the glyphs and the page's
     * padding: the switch's card is one row, so one run, and the session's card is
     * the other.
     */
    private fun bands(image: BufferedImage): List<Band> {
        val page = image.getRGB(2, 2)
        val column = image.width - 30
        val runs = mutableListOf<Band>()
        var start = -1
        for (y in 0 until image.height) {
            val filled = !near(image.getRGB(column, y), page)
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
