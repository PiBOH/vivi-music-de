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
import androidx.compose.ui.test.hasClickAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.runComposeUiTest
import androidx.compose.ui.unit.dp
import org.jetbrains.skia.EncodedImageFormat
import java.awt.image.BufferedImage
import kotlin.math.abs

/**
 * Headless check of the Intro screen against the preview, run with
 *
 *     ./gradlew :desktop:introOrderCheck
 *
 * The preview's `intro` list (`js/screens.js`) is the title, the card of the one
 * switch, the two choices as cards of their own and the action that plays the
 * splash. The screen draws the same, and this check reads it off two harnesses:
 *
 *  * **the ui-test one** composes the real `SettingsIntroScreen` and reads the
 *    semantics tree, so the page's own text is the input: every text of the
 *    screen, in document order, must be the preview's own sequence - which is
 *    also how the switch is proven to carry no sentence (the page draws none)
 *    and each choice to print the value in use. The action is measured from its
 *    clickable node, because the page draws it at its label's width and its own
 *    label cuts any band read down a single pixel column.
 *  * **the pixel one** renders the same screen into an off-screen
 *    `ImageComposeScene` and reads the column of card fill: the switch card and
 *    the two choice cards, the page's 16 dp apart. The arrival frame is probed
 *    at the middle of each block: the three cards fade up there, the action is
 *    not a group and is painted at once.
 */
object IntroOrderCheck {

    private const val WIDTH = 640

    private const val HEIGHT = 900

    /** A rectangle read off the test's semantics tree, in root px. */
    private data class Rect(val left: Float, val top: Float, val right: Float, val bottom: Float)

    /** A run of card fill down the measured column, in pixels from the top. */
    private data class Band(val top: Int, val bottom: Int) {
        val height: Int get() = bottom - top + 1
    }

    /**
     * Every text the screen should show, in the order it should show them, read
     * from the app's own strings rather than retyped.
     */
    private val expected = listOf(
        "\u2039 Back",
        Localization.get("en", "intro"),
        Localization.get("en", "show_intro_on_startup"),
        Localization.get("en", "intro_style"),
        Localization.get("en", "intro_style_logo_name"),
        Localization.get("en", "intro_background"),
        Localization.get("en", "intro_background_gradient"),
        Localization.get("en", "preview_intro"),
    )

    @JvmStatic
    fun main(args: Array<String>) {
        val failures = mutableListOf<String>()

        var read: List<String> = emptyList()
        var back: Rect? = null
        var title: Rect? = null
        var action: Rect? = null

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
            title = onNodeWithText(Localization.get("en", "intro")).rectOrNull()
            // The action's box is the node that carries the click, not the label
            // inside it: a filled button is wider than its own text.
            action = onNode(
                hasClickAction() and hasText(Localization.get("en", "preview_intro")),
            ).rectOrNull()
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

        // ------------------------------------------------------ the action
        val a = action
        if (a != null) {
            val width = a.right - a.left
            println("action: width ${width.toInt()}, left ${a.left.toInt()}, right ${a.right.toInt()}")
            // The page's filled button is its label's width (`Preview intro`), not the
            // pane's: a button across the screen reads as a banner.
            if (width > WIDTH * 0.45f) {
                failures += "the action must be its label's width, not the pane's, read ${width.toInt()}"
            }
        } else {
            failures += "the action could not be measured"
        }

        // ------------------------------------------------------ the blocks
        val frame = settledFrame()
        val bands = cards(frame)
        println("cards: ${bands.size} band(s) ${bands.map { "${it.top}..${it.bottom}" }}")
        if (bands.size != 3) {
            failures += "the page must be the switch card and the two choice cards (3 bands), " +
                "read ${bands.size}"
        } else {
            val gaps = (1..2).map { bands[it].top - bands[it - 1].bottom - 1 }
            println("gaps: $gaps")
            gaps.forEach { gap ->
                if (abs(gap - 16) > 2) {
                    failures += "two cards must stand 16 dp apart, read $gap"
                }
            }
            if (a != null) {
                val under = a.top - bands[2].bottom - 1
                println("action: top ${a.top.toInt()} against the last card's ${bands[2].bottom} (gap $under)")
                // The page leaves 20 dp above its action (`.md-intro-preview`); the
                // app's spacer is the cards' own 16 dp and the interactive padding
                // a filled button carries above its surface adds the remaining 3, so
                // the painted gap is the page's 20.
                if (under < 18f || under > 22f) {
                    failures += "the action must stand the page's 20 dp under the last card, read $under"
                }
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
        val arrival = firstFrame()
        val page = frame.getRGB(2, 2)
        val painted = bands.map { !near(arrival.getRGB(40, (it.top + it.bottom) / 2), page) }
        val actionPainted = a != null &&
            !near(arrival.getRGB(40, (a.top.toInt() + a.bottom.toInt()) / 2), page)
        println("entrance: cards painted on arrival $painted, action painted $actionPainted against " +
            "${bands.size} band(s) once settled")
        if (bands.size == 3) {
            if (painted.any { it }) {
                failures += "the three cards must fade up as the screen arrives: the arrival frame " +
                    "already paints $painted"
            }
            if (!actionPainted) {
                failures += "the action is not a group and must be painted on the arrival frame"
            }
        }

        // --------------------------------------------- the rows read the state
        val logoOnly = textsFor(style = "logo", background = "dark")
        val tagline = textsFor(style = "logo_tagline", background = "glow")
        println(
            "state: '${logoOnly.getOrNull(4)}' / '${logoOnly.getOrNull(6)}', " +
                "then '${tagline.getOrNull(4)}' / '${tagline.getOrNull(6)}'",
        )
        if (logoOnly.getOrNull(4) != Localization.get("en", "intro_style_logo") ||
            logoOnly.getOrNull(6) != Localization.get("en", "intro_background_dark")
        ) {
            failures += "the choices must print the value in use, read $logoOnly"
        }
        if (tagline.getOrNull(4) != Localization.get("en", "intro_style_logo_tagline") ||
            tagline.getOrNull(6) != Localization.get("en", "intro_background_glow")
        ) {
            failures += "the choices must follow the store, read $tagline"
        }

        if (failures.isEmpty()) {
            println()
            println(
                "OK: Intro is the preview's - the heading 16 dp under the back row, the card of one switch " +
                    "with no sentence, the two choices as cards 16 dp apart printing the value in use, the " +
                    "action at its label's width a page margin under them, and the three cards fading up " +
                    "on arrival while the action is painted at once",
            )
            return
        }
        println()
        println("FAILED:")
        failures.forEach { println("  - $it") }
        throw IllegalStateException("intro order check failed (${failures.size} case(s))")
    }

    /** Every text the screen shows for one pair of choices. */
    private fun textsFor(style: String, background: String): List<String> {
        var texts: List<String> = emptyList()
        runComposeUiTest {
            setContent { content(style = style, background = background) }
            waitForIdle()
            val read = mutableListOf<String>()
            fun walk(node: androidx.compose.ui.semantics.SemanticsNode) {
                val config = node.config
                if (config.contains(androidx.compose.ui.semantics.SemanticsProperties.Text)) {
                    config[androidx.compose.ui.semantics.SemanticsProperties.Text]
                        .forEach { read += it.text }
                }
                node.children.forEach { walk(it) }
            }
            walk(onRoot().fetchSemanticsNode())
            texts = read
        }
        return texts
    }

    /**
     * The same screen for the off-screen scene and for the state arms.
     *
     * [arriving] is what the app's own screen host provides on a navigation: a
     * group only fades up when one has just put its screen on show, which is the
     * preview's `screen--enter`.
     */
    @androidx.compose.runtime.Composable
    private fun content(
        arriving: Boolean = false,
        style: String = "logo_name",
        background: String = "gradient",
    ) {
        AppTheme(mode = ThemeMode.LIGHT, accent = Color(0xFFED5564), language = "en") {
            Box(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.surface)) {
                Box(Modifier.size(WIDTH.dp, HEIGHT.dp)) {
                    CompositionLocalProvider(LocalScreenArrival provides arriving) {
                        SettingsIntroScreen(
                            language = "en",
                            onBack = {},
                            showIntroSplash = true,
                            onShowIntroSplashChange = {},
                            introStyle = style,
                            onIntroStyleChange = {},
                            introBackground = background,
                            onIntroBackgroundChange = {},
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

    /** The runs of card fill down the measured column, in pixels from the top. */
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
