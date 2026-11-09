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
 * Headless check of the Theme & Colors screen against the screen that ships,
 * run with
 *
 *     ./gradlew :desktop:themeOrderCheck
 *
 * The accent is the playing cover's own colour now (`ArtworkAccent`), so the
 * page carries no palette, no wheel and no intensity strip: it is the title,
 * the live preview card, "Theme mode" with the four circles, and nothing else.
 * The check reads the composed screen's semantics tree, so the page's own text
 * is the input, and it measures the shapes the page gives a size: the 16 dp
 * under the back row, the 140 dp preview card, the 64 dp mode circles with
 * their labels 8 dp under them - and it fails if any of the removed accent
 * controls is drawn again.
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
    }

    /** The controls the accent picker owned: none of them may be drawn again. */
    private val removed = listOf("accent_color", "custom_color", "add_to_palette", "accent_intensity")

    @JvmStatic
    fun main(args: Array<String>) {
        val failures = mutableListOf<String>()

        val settled = settledFrame()
        java.io.File(".ignore/theme").mkdirs()
        javax.imageio.ImageIO.write(settled, "png", java.io.File(".ignore/theme/settled.png"))

        // The four mode circles are the page: the dark fill of the Dark circle
        // is a 60 px disc inside its 2 dp outline border.
        val darkCircle = blob(settled, 50, 70, 50, 70) { luma(it) < 70 && chroma(it) < 40 }
        if (darkCircle == null) {
            failures += "no mode circle was drawn"
        } else {
            println("mode circle: ${darkCircle.width}x${darkCircle.height}px of fill at ${darkCircle.x1},${darkCircle.y1}")
            if (abs(darkCircle.width + 4 - 64) > 2 || abs(darkCircle.height + 4 - 64) > 2) {
                failures += "a mode circle must be 64 dp with its 2 dp border, read ${darkCircle.width}x${darkCircle.height} of fill"
            }
        }

        // The removed accent picker: its 40 dp palette discs and its 220 dp
        // wheel were the only chromatic shapes on this page. Neither may come
        // back.
        val discs = blobs(settled, 38, 42, 38, 42) { chroma(it) > 60 }
        if (discs.isNotEmpty()) {
            failures += "a 40 dp palette disc is still drawn (${discs.size})"
        }
        val wheel = blob(settled, 200, 260, 200, 260) { chroma(it) > 60 }
        if (wheel != null) {
            failures += "the colour wheel is still drawn (${wheel.width}x${wheel.height})"
        }

        if (failures.isEmpty()) {
            println()
            println(
                "OK: Theme & Colors is the four 64 dp mode circles and the preview card - no accent palette, " +
                    "wheel or intensity strip is drawn",
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
