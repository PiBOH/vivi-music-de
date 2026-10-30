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
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onFirst
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.runComposeUiTest
import androidx.compose.ui.unit.dp
import org.jetbrains.skia.EncodedImageFormat
import java.awt.image.BufferedImage
import kotlin.math.abs

/**
 * Headless check of the Animations screen against the preview, run with
 *
 *     ./gradlew :desktop:animationsOrderCheck
 *
 * The preview's `animations` list (`js/screens.js`) is the title, one card of two
 * switches - the master one and the screen transitions it governs, each with its
 * own sentence - then the transition style, which belongs to both switches, and
 * the speed, which belongs to the master alone. Both choices are cards of their
 * own that fold away with the switches above them.
 *
 * Two harnesses, for the two kinds of fact:
 *
 *  * **the ui-test one** composes the real `SettingsTransitionsScreen` and reads
 *    the semantics tree, so the page's own text is the input: every text of the
 *    screen, in document order, must be the preview's own sequence - which is
 *    also how the transitions row is proven to carry the page's sentence and not
 *    only its name;
 *  * **the pixel one** renders the same screen into an off-screen
 *    `ImageComposeScene` and reads the column of card fill: one band per row, so
 *    the card of two switches reads as two bands and each choice as one, joined
 *    by the 3 dp seam inside the card and standing the page's 16 dp apart. The
 *    arrival frame is read too: every card fades up there, so it paints nothing.
 *
 * The two folds are read as their own arms: with the master switch off the page
 * is the one switch, and with the transitions off it is the card and the speed.
 */
object AnimationsOrderCheck {

    private const val WIDTH = 640

    private const val HEIGHT = 900

    /** A rectangle read off the test's semantics tree, in root px. */
    private data class Rect(val left: Float, val top: Float, val right: Float, val bottom: Float)

    /** A run of card fill down the measured column, in pixels from the top. */
    private data class Band(val top: Int, val bottom: Int) {
        val height: Int get() = bottom - top + 1
    }

    /** The page's sentence under the transitions switch, from the app's strings. */
    private fun string(key: String) = Localization.get("en", key)

    /**
     * Every text the screen should show, in the order it should show them, read
     * from the app's own strings rather than retyped.
     *
     * The name of the master switch is the page's title again, which is the
     * page's own doing: two rows of text that say the same word.
     */
    private val expected = listOf(
        "\u2039 Back",
        string("animations"),
        string("animations"),
        string("animations_desc"),
        string("screen_transitions"),
        string("screen_transitions_desc"),
        string("transition_style"),
        string("transition_fade"),
        string("animation_speed"),
        string("animation_speed_normal"),
    )

    @JvmStatic
    fun main(args: Array<String>) {
        val failures = mutableListOf<String>()

        val wasEnabled = Animations.enabled
        val wasSpeed = Animations.speed
        try {
            Animations.speed = "normal"
            Animations.enabled = true

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
                // The title and the master switch share the word, and the title is
                // the first of the two in the page's own order.
                title = onAllNodesWithText(string("animations")).onFirst().rectOrNull()
            }

            // -------------------------------------------------- the sequence
            if (read == expected) {
                println("order: ${read.size} texts, the preview's own sequence")
            } else {
                failures += "the screen's texts are not the preview's: read $read"
            }

            // -------------------------------------------------- the heading
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

            // -------------------------------------------------- the cards
            val bands = cards(settledFrame())
            println("cards: ${bands.size} band(s) ${bands.map { "${it.top}..${it.bottom}" }}")
            // One band per row: two switches in the card, then the style, then the
            // speed. Four bands is the two cards of one row and the card of two.
            if (bands.size != 4) {
                failures += "the page must be the card of two switches and the two choices (4 bands), " +
                    "read ${bands.size}"
            } else {
                val seams = (1..3).map { bands[it].top - bands[it - 1].bottom - 1 }
                println("seams: $seams")
                if (abs(seams[0] - 3) > 1) {
                    failures += "the two switches must be joined by the 3 dp seam, read ${seams[0]}"
                }
                seams.drop(1).forEach { gap ->
                    if (abs(gap - 16) > 2) {
                        failures += "two cards must stand 16 dp apart, read $gap"
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

            // -------------------------------------------------- the entrance
            val arrival = cards(firstFrame())
            println("entrance: ${arrival.size} band(s) on the arrival frame against ${bands.size} once settled")
            if (bands.size == 4 && arrival.isNotEmpty()) {
                failures += "every card fades up as the screen arrives: the arrival frame already paints " +
                    "${arrival.size} band(s)"
            }

            // -------------------------------------------------- the two folds
            val transitionsOff = textsFor(screenTransition = "off", animationsEnabled = true)
            val transitionsOffBands = cards(settledFrame(screenTransition = "off"))
            println("transitions off: ${transitionsOff.size} texts, ${transitionsOffBands.size} band(s)")
            if (transitionsOff.size != 8 || transitionsOffBands.size != 3) {
                failures += "with the transitions off the page must be the card and the speed only, " +
                    "read ${transitionsOff.size} text(s) and ${transitionsOffBands.size} band(s)"
            }

            val animationsOff = textsFor(screenTransition = "fade", animationsEnabled = false)
            Animations.enabled = false
            val offBands = cards(settledFrame(screenTransition = "fade", animationsEnabled = false))
            Animations.enabled = true
            println("animations off: ${animationsOff.size} texts, ${offBands.size} band(s)")
            if (animationsOff.size != 4 || offBands.size != 1) {
                failures += "with the master switch off the page must be the one switch, " +
                    "read ${animationsOff.size} text(s) and ${offBands.size} band(s)"
            }

            if (failures.isEmpty()) {
                println()
                println(
                    "OK: Animations is the preview's - the heading 16 dp under the back row and the card 16 dp " +
                        "under it, the master switch and the transitions it governs in one card of two rows on " +
                        "the 3 dp seam with their own sentences, the style and the speed as cards 16 dp apart " +
                        "that fold with the switches above them (the style with both, the speed with the " +
                        "master), and every card fading up on arrival",
                )
                return
            }
            println()
            println("FAILED:")
            failures.forEach { println("  - $it") }
            throw IllegalStateException("animations order check failed (${failures.size} case(s))")
        } finally {
            Animations.enabled = wasEnabled
            Animations.speed = wasSpeed
        }
    }

    /** Every text the screen shows for one state of the two switches. */
    private fun textsFor(screenTransition: String, animationsEnabled: Boolean): List<String> {
        Animations.enabled = animationsEnabled
        var texts: List<String> = emptyList()
        runComposeUiTest {
            setContent { content(screenTransition = screenTransition, animationsEnabled = animationsEnabled) }
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
        Animations.enabled = true
        return texts
    }

    /**
     * The same screen for the off-screen scene and for the folds' own arms.
     *
     * [arriving] is what the app's own screen host provides on a navigation: a
     * card only fades up when one has just put its screen on show, which is the
     * preview's `screen--enter`.
     */
    @androidx.compose.runtime.Composable
    private fun content(
        arriving: Boolean = false,
        screenTransition: String = "fade",
        animationsEnabled: Boolean = true,
    ) {
        AppTheme(mode = ThemeMode.LIGHT, accent = Color(0xFFED5564), language = "en") {
            Box(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.surface)) {
                Box(Modifier.size(WIDTH.dp, HEIGHT.dp)) {
                    CompositionLocalProvider(LocalScreenArrival provides arriving) {
                        SettingsTransitionsScreen(
                            language = "en",
                            onBack = {},
                            screenTransition = screenTransition,
                            onScreenTransitionChange = {},
                            animationsEnabled = animationsEnabled,
                            onAnimationsEnabledChange = {},
                            animationSpeed = "normal",
                            onAnimationSpeedChange = {},
                        )
                    }
                }
            }
        }
    }

    @OptIn(ExperimentalComposeUiApi::class)
    private fun scene(
        arriving: Boolean = false,
        screenTransition: String = "fade",
        animationsEnabled: Boolean = true,
    ): ImageComposeScene =
        ImageComposeScene(WIDTH, HEIGHT) {
            content(arriving = arriving, screenTransition = screenTransition, animationsEnabled = animationsEnabled)
        }

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
    private fun settledFrame(screenTransition: String = "fade", animationsEnabled: Boolean = true): BufferedImage {
        val scene = scene(arriving = true, screenTransition = screenTransition, animationsEnabled = animationsEnabled)
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
     * shows through, so a card of two switches reads as two bands - which is the
     * point, because the seam is then measurable.
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
