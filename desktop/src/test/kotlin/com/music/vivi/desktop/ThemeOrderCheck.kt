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
 * Headless check of the Theme & Colors screen against the preview, run with
 *
 *     ./gradlew :desktop:themeOrderCheck
 *
 * The preview's `theme` list (`js/screens.js`) is one column: the title, the
 * live preview card, "Theme mode" with the four circles, "Accent color" with the
 * palette, then the colour picker - a group whose head is "Custom color" and the
 * close button, whose body is the wheel with the brightness strip beside it, and
 * whose foot is the hex field with *Add to palette* under it.
 *
 * Two harnesses, for the two kinds of fact:
 *
 *  * **the ui-test one** composes the real `SettingsThemeScreen` and reads the
 *    semantics tree, so the page's own text is the input: every text of the
 *    screen, in document order, must be the preview's own sequence;
 *  * **the pixel one** renders the same screen into an off-screen
 *    `ImageComposeScene` and measures the shapes the page's CSS gives a size: the
 *    64 dp mode circles, the 40 dp palette discs on the page's auto-fill pitch,
 *    the 220 dp wheel with its 20 dp strip at a 32 dp gap, and 12/16/8 dp of
 *    spacing between the blocks the page puts them between. The arrival frame is
 *    read too: the picker is a `md-group` on the page, so it fades up.
 *
 * The two rows the page does not draw - the intensity strip beside the wheel and
 * the caption under it - are kept on the DE and are named here as DE-only, so a
 * screen that lost them would fail rather than pass quietly.
 */
object ThemeOrderCheck {

    private const val WIDTH = 700

    private const val HEIGHT = 1500

    /** The accent the screen opens on: the page's own default red. */
    private val accent = Color(0xFFED5564)

    /** A rectangle read off the test's semantics tree, in root px. */
    private data class Rect(val left: Float, val top: Float, val right: Float, val bottom: Float) {
        val width: Float get() = right - left
        val height: Float get() = bottom - top
    }

    /**
     * Every text the screen should show, in the order it should show them.
     *
     * The preview's own column, read from the app's own strings rather than
     * retyped, so a wording change reaches this check instead of leaving it
     * comparing the page against a stale copy of itself. The intensity caption
     * and its percentage are the two DE-only texts and are marked as such.
     */
    private val expected: List<String> = buildList {
        add("\u2039 Back")
        add(Localization.get("en", "theme_colors"))
        add(Localization.get("en", "theme_mode"))
        add(Localization.get("en", "theme_system"))
        add(Localization.get("en", "theme_light"))
        add(Localization.get("en", "theme_dark"))
        add(Localization.get("en", "pure_black"))
        add(Localization.get("en", "accent_color"))
        // The picker's head, then the strip that belongs to the DE beside the
        // wheel (the page draws only the wheel and the brightness strip), then
        // the hex field and the button under the wheel.
        add(Localization.get("en", "custom_color"))
        add(Localization.get("en", "accent_intensity"))
        add("100%")
        // The hex field's value is an `EditableText`, not a `Text`, so it is not
        // in this column; the button under it is.
        add(Localization.get("en", "add_to_palette"))
    }

    @JvmStatic
    fun main(args: Array<String>) {
        val failures = mutableListOf<String>()

        var read: List<String> = emptyList()
        val boxes = mutableMapOf<String, Rect>()

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

            listOf(
                "\u2039 Back",
                Localization.get("en", "theme_colors"),
                Localization.get("en", "theme_mode"),
                Localization.get("en", "accent_color"),
                Localization.get("en", "theme_system"),
                Localization.get("en", "theme_light"),
                Localization.get("en", "theme_dark"),
                Localization.get("en", "pure_black"),
                Localization.get("en", "custom_color"),
                Localization.get("en", "add_to_palette"),
            ).forEach { label ->
                onNodeWithText(label).firstRectOrNull()?.let { boxes[label] = it }
            }
        }

        // ------------------------------------------------------ the sequence
        if (read == expected) {
            println("order: ${read.size} texts, the preview's own column")
        } else {
            failures += "the screen's texts are not the preview's column: read $read"
        }

        // ------------------------------------------------------ the heading
        val back = boxes["\u2039 Back"]
        val title = boxes[Localization.get("en", "theme_colors")]
        val modeLabel = boxes[Localization.get("en", "theme_mode")]
        val accentLabel = boxes[Localization.get("en", "accent_color")]
        val darkLabel = boxes[Localization.get("en", "theme_dark")]
        val headTitle = boxes[Localization.get("en", "custom_color")]
        val addButton = boxes[Localization.get("en", "add_to_palette")]
        val modeLabels = listOf("theme_system", "theme_light", "theme_dark", "pure_black")
            .mapNotNull { boxes[Localization.get("en", it)] }

        if (back != null && title != null) {
            val gap = title.top - back.bottom
            println("heading: back ends ${back.bottom.toInt()}, title starts ${title.top.toInt()} (gap ${gap.toInt()})")
            if (abs(gap - 16f) > 2f) {
                failures += "the heading must stand 16 dp under the back row, read ${gap.toInt()}"
            }
        } else {
            failures += "the heading could not be measured"
        }

        // ------------------------------------------------------ the preview card
        // The card carries no text, and its fill is the page's own surface, so
        // the height it must be (140 dp, `.md-preview`) is read from the spacing
        // it leaves: 16 dp under the heading, then 16 dp to the mode label.
        if (title != null && modeLabel != null) {
            val box = modeLabel.top - title.bottom - 32f
            println("preview card: ${box.toInt()} dp between the heading and the mode label, so the card is ${box.toInt()} tall")
            if (abs(box - 140f) > 2f) {
                failures += "the preview card must be 140 dp tall, read ${box.toInt()}"
            }
        } else {
            failures += "the preview card could not be measured"
        }

        // ------------------------------------------------------ the mode circles
        val settled = settledFrame()
        val arrival = arrivalFrame()
        java.io.File(".ignore/theme").mkdirs()
        javax.imageio.ImageIO.write(settled, "png", java.io.File(".ignore/theme/settled.png"))
        // The dark fill of the Dark circle: the page's 64 dp disc less its own
        // 2 dp `outline` border, which is a light grey and so not matched here.
        val darkCircle = blob(settled, 50, 70, 50, 70) { luma(it) < 70 && chroma(it) < 40 }
        if (darkCircle == null) {
            failures += "no mode circle was drawn"
        } else {
            println("mode circle: ${darkCircle.width}x${darkCircle.height}px of fill at ${darkCircle.x1},${darkCircle.y1}")
            if (abs(darkCircle.width + 4 - 64) > 2 || abs(darkCircle.height + 4 - 64) > 2) {
                failures += "a mode circle must be 64 dp with its 2 dp border, read ${darkCircle.width}x${darkCircle.height} of fill"
            }
            if (darkLabel != null) {
                val gap = darkLabel.top - darkCircle.y2 - 1
                println("mode label: circle ends ${darkCircle.y2}, label starts ${darkLabel.top.toInt()} (gap ${gap.toInt()})")
                if (abs(gap - 8f) > 2f) {
                    failures += "a mode label must stand 8 dp under its circle, read ${gap.toInt()}"
                }
            }
        }

        // ------------------------------------------------------ the palette
        if (accentLabel != null && modeLabels.isNotEmpty()) {
            val underModes = accentLabel.top - modeLabels.maxOf { it.bottom }
            println("accent heading: the circles end at ${modeLabels.maxOf { it.bottom }.toInt()}, \"Accent color\" starts ${accentLabel.top.toInt()} (gap ${underModes.toInt()})")
            if (abs(underModes - 16f) > 3f) {
                failures += "\"Accent color\" must stand 16 dp under the mode circles, read ${underModes.toInt()}"
            }
        }
        // The palette's discs: 40 dp, chromatic, and the selected one is split by
        // its white check, so the count is a count of discs and not of blobs.
        // The discs are the palette's own 40 dp circles; the hex chip under the
        // wheel is one of them, so only those above the wheel are the palette.
        val discs = blobs(settled, 38, 42, 38, 42) { chroma(it) > 60 }
            .filter { it.y1 < 600 }
            .sortedWith(compareBy({ it.y1 }, { it.x1 }))
        if (discs.isEmpty()) {
            failures += "no palette swatch was drawn"
        } else {
            val firstRowX = discs.filter { it.y1 - discs.first().y1 <= 3 }.map { it.x1 }.sorted()
            println("palette: ${discs.size} disc(s) drawn, first row at x=$firstRowX")
            // The page's grid is `repeat(auto-fill, minmax(44px, 1fr))` with a
            // 12 dp gap: as many 44 dp columns as fit the content, each stretched
            // to share it, so the pitch between two discs is that cell plus the
            // gap - not the fixed 40 + 12 a plain row would pack.
            val content = WIDTH - 2 * 14
            val columns = ((content + 12) / (44 + 12)).coerceAtLeast(1)
            val pagePitch = (content - 12 * (columns - 1)).toFloat() / columns + 12f
            val pitch = if (firstRowX.size >= 2) (firstRowX[1] - firstRowX[0]).toFloat() else 0f
            println("palette: ${columns} columns fit $content dp, the page's pitch ${pagePitch.toInt()}px against the drawn ${pitch.toInt()}px")
            if (abs(pitch - pagePitch) > 2f) {
                failures += "the palette must use the page's own auto-fill pitch (${pagePitch.toInt()}px), read ${pitch.toInt()}px"
            }
            if (abs(firstRowX.first() - 14) > 2) {
                failures += "the palette must start at the page's own ${14} dp padding, read ${firstRowX.first()}"
            }
            if (accentLabel != null) {
                val gap = discs.first().y1 - accentLabel.bottom
                println("palette: \"Accent color\" ends ${accentLabel.bottom.toInt()}, the discs start ${discs.first().y1} (gap ${gap.toInt()})")
                if (abs(gap - 12f) > 3f) {
                    failures += "the palette must stand 12 dp under \"Accent color\", read ${gap.toInt()}"
                }
            }
        }

        // ------------------------------------------------------ the picker
        val wheel = blob(settled, 200, 260, 200, 260) { chroma(it) > 60 }
        if (wheel == null) {
            failures += "no colour wheel was drawn"
        } else {
            println("wheel: ${wheel.width}x${wheel.height}px at ${wheel.x1},${wheel.y1}")
            if (abs(wheel.width - 220) > 2 || abs(wheel.height - 220) > 2) {
                failures += "the wheel must be 220 dp, read ${wheel.width}x${wheel.height}"
            }
            // The picker's block: 16 dp under the palette (`.md-picker`), then
            // its head row of 32 dp (the page's close button) and the 12 dp under
            // it. The head's title text is centred in that row, so its own bottom
            // is not the row's and the gap is read off the palette instead.
            val paletteBottom = if (discs.isEmpty()) null else discs.maxOf { it.y1 } + 40
            if (paletteBottom != null) {
                val gap = wheel.y1 - paletteBottom
                println("wheel: the palette ends $paletteBottom, the wheel starts ${wheel.y1} (gap $gap = 16 + the 32 dp head + 12)")
                if (abs(gap - 60) > 4) {
                    failures += "the wheel must stand 16 + 32 + 12 dp under the palette, read $gap"
                }
            }
            // The strips beside the wheel, read as the runs of non-background on
            // the wheel's own middle row: they are vertical bars, so that row
            // crosses them at their full height and the disc's own white centre
            // is left of it.
            val beside = runsAt(settled, (wheel.y1 + wheel.y2) / 2, wheel.x2 + 1)
            println("beside the wheel: ${beside.size} bar(s) ${beside.map { "${it.first}..${it.last}" }}")
            val brightness = beside.getOrNull(0)
            val intensity = beside.getOrNull(1)
            if (brightness == null) {
                failures += "no brightness strip was drawn beside the wheel"
            } else {
                val width = brightness.last - brightness.first + 1
                val gap = brightness.first - wheel.x2 - 1
                println("strip: ${width}px wide at ${brightness.first} (gap $gap from the wheel)")
                if (abs(width - 20) > 1) {
                    failures += "the brightness strip must be 20 dp wide, read $width"
                }
                if (abs(gap - 32) > 2) {
                    failures += "the strip must stand 32 dp right of the wheel, read $gap"
                }
                // Level with the wheel: its own bottom row is still a bar.
                val atBottom = runsAt(settled, wheel.y2 - 10, wheel.x2 + 1)
                if (atBottom.isEmpty()) {
                    failures += "the strip must be as tall as the wheel: nothing is drawn beside it at its bottom"
                }
            }
            // The DE keeps the accent intensity beside the wheel; the page draws
            // only the wheel and the brightness strip, so this is the DE's own bar
            // and it is checked where it stands (centred in its own caption's
            // column, which is why its gap carries that column's half width).
            if (intensity == null) {
                failures += "the accent intensity strip is missing from beside the wheel"
            } else {
                val width = intensity.last - intensity.first + 1
                println("intensity: ${width}px wide at ${intensity.first}")
                if (abs(width - 20) > 1) {
                    failures += "the intensity strip must be 20 dp wide, read $width"
                }
            }
        }

        // ------------------------------------------------------ the entrance
        val wheelFirst = blob(arrival, 200, 260, 200, 260) { chroma(it) > 60 }
        println("entrance: the wheel is ${if (wheelFirst == null) "not painted" else "already painted"} on the arrival frame")
        if (wheelFirst != null) {
            failures += "the picker must fade up as the screen arrives (it is a group on the page): " +
                "the arrival frame already paints the wheel"
        }

        if (failures.isEmpty()) {
            println()
            println(
                "OK: Theme & Colors is the preview's column - the heading 16 dp under the back row, the " +
                    "140 dp preview card, the four 64 dp mode circles with their labels 8 dp under them, " +
                    "\"Accent color\" 16 dp below, the palette on the page's own auto-fill pitch 12 dp under " +
                    "it, then the picker - 220 dp wheel with its 20 dp strip level and 32 dp away, the hex " +
                    "field under it - fading up on arrival",
            )
            return
        }
        println()
        println("FAILED:")
        failures.forEach { println("  - $it") }
        throw IllegalStateException("theme order check failed (${failures.size} case(s))")
    }

    /**
     * The screen for the off-screen scene, which has no test harness.
     *
     * [arriving] is what the app's own screen host provides on a navigation: the
     * picker is a group, and a group only fades up when one has just put its
     * screen on show, which is the preview's `screen--enter`.
     */
    @androidx.compose.runtime.Composable
    private fun content(arriving: Boolean = false) {
        AppTheme(mode = ThemeMode.LIGHT, accent = accent, language = "en") {
            Box(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.surface)) {
                Box(Modifier.size(WIDTH.dp, HEIGHT.dp)) {
                    CompositionLocalProvider(LocalScreenArrival provides arriving) {
                        SettingsThemeScreen(
                            language = "en",
                            onBack = {},
                            themeMode = ThemeMode.LIGHT,
                            accent = accent,
                            onThemeModeChange = {},
                            onAccentChange = {},
                            pureBlack = false,
                            onPureBlackChange = {},
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

    private data class Blob(val size: Int, val x1: Int, val y1: Int, val x2: Int, val y2: Int) {
        val width: Int get() = x2 - x1 + 1
        val height: Int get() = y2 - y1 + 1
    }

    /**
     * The runs of non-background pixels along the row [y], from [from] onwards.
     * The background is the page's own surface, read at the frame's top-left
     * corner; a run shorter than 4 px is a border and is dropped.
     */
    private fun runsAt(image: BufferedImage, y: Int, from: Int): List<IntRange> {
        val page = image.getRGB(2, 2)
        val runs = mutableListOf<IntRange>()
        var start = -1
        for (x in from until image.width) {
            val filled = image.getRGB(x, y) != page
            if (filled && start < 0) start = x
            if (!filled && start >= 0) {
                if (x - start >= 4) runs += start..(x - 1)
                start = -1
            }
        }
        if (start >= 0 && image.width - start >= 4) runs += start..(image.width - 1)
        return runs
    }

    private fun chroma(p: Int): Int {
        val r = (p shr 16) and 255
        val g = (p shr 8) and 255
        val b = p and 255
        return maxOf(r, g, b) - minOf(r, g, b)
    }

    private fun luma(p: Int): Int {
        val r = (p shr 16) and 255
        val g = (p shr 8) and 255
        val b = p and 255
        return (r * 299 + g * 587 + b * 114) / 1000
    }

    /** The largest region of matching pixels inside the given box. */
    private fun blob(
        image: BufferedImage,
        minWidth: Int,
        maxWidth: Int,
        minHeight: Int,
        maxHeight: Int,
        match: (Int) -> Boolean,
    ): Blob? = blobs(image, minWidth, maxWidth, minHeight, maxHeight, match).maxByOrNull { it.size }

    /** Every region of matching pixels, as its bounding box. */
    private fun blobs(
        image: BufferedImage,
        minWidth: Int,
        maxWidth: Int,
        minHeight: Int,
        maxHeight: Int,
        match: (Int) -> Boolean,
    ): List<Blob> {
        val width = image.width
        val height = image.height
        val seen = BooleanArray(width * height)
        val stack = IntArray(width * height)
        val found = mutableListOf<Blob>()
        val argb = IntArray(width * height)
        image.getRGB(0, 0, width, height, argb, 0, width)
        for (start in 0 until width * height) {
            if (seen[start] || !match(argb[start])) continue
            var top = 0
            stack[top++] = start
            seen[start] = true
            var count = 0
            var minX = width
            var maxX = -1
            var minY = height
            var maxY = -1
            while (top > 0) {
                val index = stack[--top]
                val x = index % width
                val y = index / width
                count++
                if (x < minX) minX = x
                if (x > maxX) maxX = x
                if (y < minY) minY = y
                if (y > maxY) maxY = y
                fun push(next: Int) {
                    if (seen[next]) return
                    if (!match(argb[next])) return
                    seen[next] = true
                    stack[top++] = next
                }
                if (x > 0) push(index - 1)
                if (x < width - 1) push(index + 1)
                if (y > 0) push(index - width)
                if (y < height - 1) push(index + width)
            }
            val w = maxX - minX + 1
            val h = maxY - minY + 1
            if (w in minWidth..maxWidth && h in minHeight..maxHeight && count >= (w * h) / 6) {
                found += Blob(count, minX, minY, maxX, maxY)
            }
        }
        return found
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
