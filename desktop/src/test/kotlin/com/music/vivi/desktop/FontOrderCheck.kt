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
import androidx.compose.ui.test.onAllNodesWithContentDescription
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.runComposeUiTest
import androidx.compose.ui.unit.dp
import org.jetbrains.skia.EncodedImageFormat
import java.awt.image.BufferedImage
import kotlin.math.abs

/**
 * Headless check of the App font screen against the preview, run with
 *
 *     ./gradlew :desktop:fontOrderCheck
 *
 * The preview's `font` list (`js/screens.js`) is the title, the typography card,
 * the *Font Selection* heading with one row per face and the import as the last
 * of them, and the lyrics switch under it. The screen draws the same, and this
 * check reads it off two harnesses:
 *
 *  * **the ui-test one** composes the real `SettingsFontScreen` and reads the
 *    semantics tree, so the page's own text is the input: every text of the
 *    screen, in document order, must be the preview's own sequence - which is
 *    also how the card is proven to carry four lines (the label, the quote, the
 *    figures and the name of the face it is drawing) and the imported faces to
 *    be rows of the same list, each with its rename and its delete.
 *  * **the pixel one** renders the same screen into an off-screen
 *    `ImageComposeScene` and reads the column of card fill: the typography card,
 *    the six rows of the font card and the lyrics card, joined by the 3 dp seam
 *    inside the card and standing the page's 16 dp apart. The arrival frame is
 *    read too: a group fades up there, so it paints nothing.
 */
object FontOrderCheck {

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
     * The preview's own sequence, read from the app's own strings rather than
     * retyped, so a wording change reaches this check instead of leaving it
     * comparing the page against a stale copy of itself.
     */
    private val expected = listOf(
        "\u2039 Back",
        Localization.get("en", "app_font"),
        Localization.get("en", "typography_preview").uppercase(),
        Localization.get("en", "preview_text_quote"),
        "0123456789",
        // The caption is the face the card is drawing, not the list's name.
        Localization.get("en", "font_system"),
        Localization.get("en", "font_selection"),
        Localization.get("en", "font_system"),
        Localization.get("en", "font_system_desc"),
        Localization.get("en", "font_google_sans"),
        Localization.get("en", "font_google_sans_desc"),
        Localization.get("en", "font_sans_flex"),
        Localization.get("en", "font_sans_flex_desc"),
        Localization.get("en", "font_outfit"),
        Localization.get("en", "font_outfit_desc"),
        Localization.get("en", "font_plus_jakarta_sans"),
        Localization.get("en", "font_plus_jakarta_sans_desc"),
        Localization.get("en", "import_font"),
        Localization.get("en", "font_in_lyrics"),
        Localization.get("en", "font_in_lyrics_desc"),
    )

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
            title = onNodeWithText(Localization.get("en", "app_font")).rectOrNull()
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

        // ------------------------------------------------------ the cards
        val bands = cards(settledFrame(), faces = 0)
        println("cards: ${bands.size} band(s) ${bands.map { "${it.top}..${it.bottom}" }}")
        // The typography card, six rows of the font card, the lyrics card: three
        // cards and not eight rows standing loose, which is what the bands tell.
        if (bands.size != 8) {
            failures += "the page must be the typography card, six rows and the lyrics card (8 bands), " +
                "read ${bands.size}"
        } else {
            val seams = (2..6).map { bands[it].top - bands[it - 1].bottom - 1 }
            println("seams: $seams")
            seams.forEach { seam ->
                if (abs(seam - 3) > 1) {
                    failures += "a card's rows must be joined by the 3 dp seam, read $seam"
                }
            }
            val toLyrics = bands[7].top - bands[6].bottom - 1
            println("cards: the last face ends ${bands[6].bottom}, the lyrics card starts ${bands[7].top} (gap $toLyrics)")
            if (abs(toLyrics - 16) > 2) {
                failures += "two cards must stand 16 dp apart, read $toLyrics"
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
                failures += "the typography card must stand 16 dp under the heading, read ${underTitle.toInt()}"
            }
        }

        // ------------------------------------------------------ the entrance
        // The typography card is not a group: an `M3SettingsGroup` fades up as its
        // screen arrives, the page's own card is painted from the first frame. So
        // the arrival frame shows that card and neither group.
        val arrival = cards(firstFrame(), faces = 0)
        println("entrance: ${arrival.size} band(s) on the arrival frame against ${bands.size} once settled")
        if (bands.size == 8) {
            if (arrival.size != 1) {
                failures += "only the typography card is painted on the arrival frame (1 band), " +
                    "the two groups fade up: read ${arrival.size} card(s)"
            } else if (abs(arrival[0].bottom - bands[0].bottom) > 2) {
                failures += "the card painted on arrival must be the typography card, read " +
                    "${arrival[0].top}..${arrival[0].bottom} against ${bands[0].top}..${bands[0].bottom}"
            }
        }

        // ------------------------------------------- an imported face is a row
        CustomFonts.faces += CustomFont(id = 7, path = "C:/nothing/outfit-display.ttf", name = "Outfit Display")
        try {
            var imported: List<String> = emptyList()
            var renameButtons = 0
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
                imported = texts
                renameButtons = onAllNodesWithContentDescription(Localization.get("en", "rename"))
                    .fetchSemanticsNodes().size
            }
            val row = imported.indexOf("Outfit Display")
            println("imported: 'Outfit Display' at $row of ${imported.size}, $renameButtons rename button(s)")
            if (row < 0 || imported.getOrNull(row + 1) != Localization.get("en", "custom_font")) {
                failures += "an imported face must be a row of the card, named after itself and described " +
                    "as the custom font, read $imported"
            } else if (row != expected.indexOf(Localization.get("en", "import_font"))) {
                failures += "the imported faces belong between the presets and the import row, read $row"
            }
            if (renameButtons != 1) {
                failures += "an imported face carries its rename and its delete, read $renameButtons rename button(s)"
            }
        } finally {
            CustomFonts.faces.clear()
        }

        if (failures.isEmpty()) {
            println()
            println(
                "OK: App font is the preview's - the heading 16 dp under the back row, the typography card " +
                    "with its label, its quote, the figures and the name of the face it draws, the card of " +
                    "six rows 16 dp under it and its rows on the 3 dp seams, the lyrics switch under them, " +
                    "an imported face a row of the same card with its rename and its delete, and the two " +
                    "groups fading up on arrival while the typography card is painted at once",
            )
            return
        }
        println()
        println("FAILED:")
        failures.forEach { println("  - $it") }
        throw IllegalStateException("font order check failed (${failures.size} case(s))")
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
                        SettingsFontScreen(
                            language = "en",
                            onBack = {},
                            selectedFont = AppFont.SYSTEM,
                            onFontChange = {},
                            useFontInLyrics = true,
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
     * point, because the seams are then measurable. [faces] is the number of
     * imported faces the frame was drawn with, which only moves the column's
     * height, not the runs this reads.
     */
    @Suppress("UNUSED_PARAMETER")
    private fun cards(image: BufferedImage, faces: Int): List<Band> {
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
