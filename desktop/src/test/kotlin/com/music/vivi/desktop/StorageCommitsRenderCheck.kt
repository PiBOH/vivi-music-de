@file:OptIn(ExperimentalComposeUiApi::class)

package com.music.vivi.desktop

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.SdStorage
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.ImageComposeScene
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import org.jetbrains.skia.EncodedImageFormat
import java.awt.image.BufferedImage
import java.io.File

/**
 * Headless check of the heads the Storage and Commits screens now carry, run with
 *
 *     ./gradlew :desktop:storageCommitsRenderCheck
 *
 * Two things a compiler cannot see: that the hero is one card built from the
 * theme's own colours, and that the Storage bar is drawn as the proportion of the
 * total each part takes. Both are rendered into an off-screen
 * `ImageComposeScene` and read back at density 1, so a dp is a pixel.
 *
 *  * **Storage's head**: a 56 dp `secondaryContainer` tile, and a 12 dp bar under
 *    it whose three pills hold the same colour and the same width ratio as the
 *    weights passed in (`6 : 3 : 1`).
 *  * **Commits' head**: the same tile, three chips (one tonal, on
 *    `secondaryContainer`; two on `surfaceContainerHigh`), and under it two day
 *    headers - each a hairline in `outlineVariant` - over three rows.
 *
 * The theme colours are read from swatches drawn in the corners of the frame, the
 * way `SliderRenderCheck` does, so the assertions are against what the theme
 * actually resolved rather than a hard-coded palette.
 *
 * The frames are written under `.ignore/hero/` so they can be looked at.
 */
object StorageCommitsRenderCheck {

    private val outDir = File(".ignore/hero")

    private const val WIDTH = 720
    private const val HEIGHT = 720

    @JvmStatic
    fun main(args: Array<String>) {
        outDir.mkdirs()
        val failures = mutableListOf<String>()

        val storage = render {
            M3ScreenHero(
                icon = Icons.Filled.SdStorage,
                title = "Storage",
                description = "Everything VIVI keeps on this machine.",
                trailing = {
                    Column(horizontalAlignment = Alignment.End) {
                        Text("562 MB", style = MaterialTheme.typography.headlineMedium)
                        Text("caches and installers", style = MaterialTheme.typography.labelLarge)
                    }
                },
                content = {
                    val parts = listOf(
                        M3BarPart("Audio cache", MaterialTheme.colorScheme.primary, 6f, "312 MB"),
                        M3BarPart("Lyrics cache", MaterialTheme.colorScheme.tertiary, 3f, "4.2 MB"),
                        M3BarPart("AI lyrics cache", MaterialTheme.colorScheme.secondary, 1f, "1.1 MB"),
                    )
                    M3SegmentedBar(parts)
                    Spacer(Modifier.height(12.dp))
                    M3SegmentedLegend(parts)
                },
                modifier = Modifier.width(640.dp),
            )
        }
        File(outDir, "storage-hero.png").writeBytes(storage)

        val hero = frame(storage)
        println(
            "storage: tile=${hero.tile.describe()} bar=${hero.bars.joinToString { it.describe() }}" +
                " legend=${hero.legend.joinToString { it.describe() }}",
        )
        if (hero.tile.width !in 54..58 || hero.tile.height !in 54..58) {
            failures += "the head should open with a 56 dp tile, read ${hero.tile.describe()}"
        }
        if (hero.tile.color != Swatch.SECONDARY_CONTAINER) {
            failures += "the tile should be secondaryContainer, read ${hero.tile.color}"
        }
        if (hero.bars.size != 3) {
            failures += "the bar should hold one pill per part (3), read ${hero.bars.size}"
        }
        if (hero.legend.size != 3) {
            failures += "the legend should name every part with a dot (3), read ${hero.legend.size}"
        }
        // Two per line: three parts sit on two rows, in the bar's own order.
        if (hero.legend.map { it.y }.distinct().size != 2) {
            failures += "the legend should wrap two per line, read ${hero.legend.map { it.y }} for its dots"
        }
        if (hero.legend.map { it.color } != hero.bars.map { it.color }) {
            failures += "the legend's dots should be the bar's colours, read ${hero.legend.map { it.color }}"
        }
        hero.bars.forEachIndexed { index, bar ->
            if (bar.height !in 11..13) failures += "pill $index should be 12 dp tall, read ${bar.height}"
        }
        // 6 : 3 : 1 - the widths are the weights, so the pixels are too (a pill
        // loses only the rounding of its own ends to the clip).
        val widths = hero.bars.map { it.width.toFloat() }
        if (widths.size == 3 && widths[1] > 0f && widths[2] > 0f) {
            val six = widths[0] / widths[2]
            val three = widths[1] / widths[2]
            if (kotlin.math.abs(six - 6f) > 0.9f) {
                failures += "the first pill should be 6x the last (6 : 3 : 1), read ${"%.2f".format(six)}x"
            }
            if (kotlin.math.abs(three - 3f) > 0.5f) {
                failures += "the second pill should be 3x the last (6 : 3 : 1), read ${"%.2f".format(three)}x"
            }
        }

        val commits = render {
            CommitsContent(
                language = "en",
                commits = listOf(
                    CommitData("a1", "The settings bars are one control", "PiBOH", null, "Oct 22, 2026", "", "18:04"),
                    CommitData("b2", "Storage and Commits gain a head", "PiBOH", null, "Oct 22, 2026", "", "17:12"),
                    CommitData("c3", "The equalizer folds its editor", "PiBOH", null, "Oct 21, 2026", "", "09:31"),
                ),
            )
        }
        File(outDir, "commits.png").writeBytes(commits)
        val list = frame(commits)
        println("commits: tile=${list.tile.describe()} chips=${list.chips.map { it.describe() }} rules=${list.rules} rows=${list.rows}")
        if (list.tile.width !in 54..58) {
            failures += "the Commits head should open with the same 56 dp tile, read ${list.tile.describe()}"
        }
        if (list.chips.count { it.color == Swatch.SECONDARY_CONTAINER } != 1) {
            failures += "one chip should be tonal (secondaryContainer), read ${list.chips.map { it.color }}"
        }
        if (list.chips.count { it.color == Swatch.SURFACE_CONTAINER_HIGH } != 2) {
            failures += "the other two chips should sit on surfaceContainerHigh, read ${list.chips.map { it.color }}"
        }
        if (list.rules != 2) {
            failures += "three commits over two days should draw two day rules, read ${list.rules}"
        }
        if (list.rows != 3) {
            failures += "three commits should draw three rows, read ${list.rows}"
        }

        if (failures.isEmpty()) {
            println()
            println(
                "OK: both heads are one card with the 56 dp tile; Storage's bar is the proportion of its parts " +
                    "(6 : 3 : 1) with a legend dot per part; Commits carries three chips over two day rules and three rows",
            )
            return
        }
        println()
        println("FAILED:")
        failures.forEach { println("  - $it") }
        throw IllegalStateException("storage/commits render check failed (${failures.size} case(s))")
    }

    /** The theme colours the measurements are matched against. */
    private enum class Swatch {
        SECONDARY_CONTAINER,
        SURFACE_CONTAINER_HIGH,
        OUTLINE_VARIANT,
        TERTIARY_CONTAINER,
        PRIMARY,
        SECONDARY,
        TERTIARY,
    }

    private data class Blob(val color: Swatch, val x: Int, val y: Int, val width: Int, val height: Int) {
        fun describe(): String = "$color ${width}x$height at $x,$y"
    }

    private data class Frame(
        val image: BufferedImage,
        val tile: Blob,
        val bars: List<Blob>,
        val chips: List<Blob>,
        val rules: Int,
        val rows: Int,
        val legend: List<Blob>,
    )

    /**
     * One frame with the components under test near the top, and one 6 dp swatch
     * per measured colour along the bottom edge (10 dp apart, so a blob of each is
     * never joined to its neighbour).
     */
    private fun render(content: @Composable () -> Unit): ByteArray {
        val scene = ImageComposeScene(WIDTH, HEIGHT) {
            AppTheme(mode = ThemeMode.LIGHT, accent = Color(0xFFED5564), language = "en") {
                Box(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.surface)) {
                    Column(Modifier.padding(start = 20.dp, top = 20.dp)) { content() }
                    Swatch.entries.forEach { swatch ->
                        val color = when (swatch) {
                            Swatch.SECONDARY_CONTAINER -> MaterialTheme.colorScheme.secondaryContainer
                            Swatch.SURFACE_CONTAINER_HIGH -> MaterialTheme.colorScheme.surfaceContainerHigh
                            Swatch.OUTLINE_VARIANT -> MaterialTheme.colorScheme.outlineVariant
                            Swatch.TERTIARY_CONTAINER -> MaterialTheme.colorScheme.tertiaryContainer
                            Swatch.PRIMARY -> MaterialTheme.colorScheme.primary
                            Swatch.SECONDARY -> MaterialTheme.colorScheme.secondary
                            Swatch.TERTIARY -> MaterialTheme.colorScheme.tertiary
                        }
                        Box(
                            Modifier
                                .align(Alignment.BottomStart)
                                .padding(start = (swatch.ordinal * 10).dp)
                                .size(6.dp)
                                .background(color),
                        )
                    }
                }
            }
        }
        try {
            scene.render(1_000_000_000L)
            return png(scene.render(1_500_000_000L))
        } finally {
            scene.close()
        }
    }

    /** Reads the frame: the tile, the bar's pills, the chips, the rules and the rows. */
    private fun frame(png: ByteArray): Frame {
        val image = javax.imageio.ImageIO.read(png.inputStream())
        val colours = Swatch.entries.associateWith { image.getRGB(2 + it.ordinal * 10, image.height - 3) }
        fun kind(argb: Int): Swatch? = colours.entries.firstOrNull { near(argb, it.value, 6) }?.key

        val blobs = blobRows(image, colours, ::kind)
        // The tile is the first secondaryContainer blob at least 40 px tall.
        val tile = blobs.firstOrNull { it.color == Swatch.SECONDARY_CONTAINER && it.height in 40..70 && it.width in 40..70 }
            ?: Blob(Swatch.SECONDARY_CONTAINER, 0, 0, 0, 0)
        // The bar's pills: the primary, secondary and tertiary blobs of the strip's
        // own height (`M3SegmentedBar` draws 12 dp), found in order of x. A glyph
        // cannot be one blob 40 px wide, so the width floor keeps text out.
        val bars = blobs.filter {
            it.height in 11..13 && it.width >= 40 &&
                (it.color == Swatch.PRIMARY || it.color == Swatch.SECONDARY || it.color == Swatch.TERTIARY)
        }.sortedBy { it.x }
        // The chips: wide, short blobs of the two chip fills, under the tile row.
        val chips = blobs.filter {
            it.height in 22..44 && (it.color == Swatch.SECONDARY_CONTAINER || it.color == Swatch.SURFACE_CONTAINER_HIGH)
        }
        // A day rule is a row of `outlineVariant` running 200 px or more: counted
        // by the row it sits on, not by its blobs, because a 1 px line can break
        // into two runs where the layout splits its width.
        val ruleRows = (0 until image.height).filter { y -> longestRun(image, y, colours.getValue(Swatch.OUTLINE_VARIANT)) >= 200 }
        val rules = ruleRows.zipWithNext().count { (a, b) -> b != a + 1 } + if (ruleRows.isEmpty()) 0 else 1
        // A row is one 36 dp avatar circle in tertiaryContainer (the sample holds
        // no avatar URL, so the fallback circle is what is drawn).
        val rows = blobs.count { it.color == Swatch.TERTIARY_CONTAINER && it.width in 32..40 && it.height in 32..40 }
        // The legend's own dots: one 8 dp disc per part, in that part's colour.
        val legend = blobs.filter {
            it.width in 5..9 && it.height in 5..9 &&
                (it.color == Swatch.PRIMARY || it.color == Swatch.SECONDARY || it.color == Swatch.TERTIARY)
        }.sortedWith(compareBy({ it.y }, { it.x }))
        return Frame(image, tile, bars, chips, rules, rows, legend)
    }

    /** The longest run of pixels on row [y] that matches [argb]. */
    private fun longestRun(image: BufferedImage, y: Int, argb: Int): Int {
        var best = 0
        var run = 0
        for (x in 0 until image.width) {
            if (near(image.getRGB(x, y), argb, 16)) {
                run++
                if (run > best) best = run
            } else {
                run = 0
            }
        }
        return best
    }

    /**
     * Every blob of a measured colour.
     *
     * One pass marks the pixels of each [colours] entry, a second collects the
     * 4-connected groups, and a group's box is its blob. Only colours passed in
     * are considered, so the text and the icons never become blobs.
     */
    private fun blobRows(
        image: BufferedImage,
        colours: Map<Swatch, Int>,
        kind: (Int) -> Swatch?,
    ): List<Blob> {
        val width = image.width
        // The bottom band holds the colour references themselves, not the screen.
        val height = image.height - 20
        val marks = arrayOfNulls<Swatch>(width * height)
        for (y in 0 until height) {
            for (x in 0 until width) {
                marks[y * width + x] = kind(image.getRGB(x, y))
            }
        }
        val seen = BooleanArray(width * height)
        val out = mutableListOf<Blob>()
        val stack = ArrayDeque<Int>()
        for (start in marks.indices) {
            val swatch = marks[start] ?: continue
            if (seen[start]) continue
            var minX = width
            var maxX = -1
            var minY = height
            var maxY = -1
            var count = 0
            stack.addLast(start)
            seen[start] = true
            while (stack.isNotEmpty()) {
                val index = stack.removeLast()
                val x = index % width
                val y = index / width
                count++
                if (x < minX) minX = x
                if (x > maxX) maxX = x
                if (y < minY) minY = y
                if (y > maxY) maxY = y
                for (neighbour in intArrayOf(index - 1, index + 1, index - width, index + width)) {
                    if (neighbour !in marks.indices || seen[neighbour]) continue
                    if (marks[neighbour] != swatch) continue
                    seen[neighbour] = true
                    stack.addLast(neighbour)
                }
            }
            // The page's own colour is everywhere; only solid-looking blobs count,
            // and 25 px is an 8 dp dot after its antialiased rim.
            if (count < 25) continue
            out += Blob(swatch, minX, minY, maxX - minX + 1, maxY - minY + 1)
        }
        return out
    }

    private fun png(image: org.jetbrains.skia.Image): ByteArray =
        image.encodeToData(EncodedImageFormat.PNG)?.bytes ?: error("the scene produced no image")

    private fun near(a: Int, b: Int, threshold: Int): Boolean {
        val dr = kotlin.math.abs(((a shr 16) and 255) - ((b shr 16) and 255))
        val dg = kotlin.math.abs(((a shr 8) and 255) - ((b shr 8) and 255))
        val db = kotlin.math.abs((a and 255) - (b and 255))
        return dr + dg + db <= threshold
    }
}
