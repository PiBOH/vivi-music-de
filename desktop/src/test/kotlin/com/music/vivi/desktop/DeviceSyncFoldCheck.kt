@file:OptIn(ExperimentalComposeUiApi::class)

package com.music.vivi.desktop

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Speaker
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.ImageComposeScene
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import org.jetbrains.skia.EncodedImageFormat
import java.io.File

/**
 * Headless check of the folding group Device sync puts its two volume channels in,
 * run with
 *
 *     ./gradlew :desktop:deviceSyncFoldCheck
 *
 * The group is `M3SettingsCollapsibleGroup` (`M3Settings.kt`): a header row that is
 * the card's first row, and a body that folds away. Whether the body is really
 * gone, whether the two switches are in the rows it draws, and whether the chevron
 * says which state it is in cannot be seen without a display, so the group is
 * rendered into an off-screen `ImageComposeScene` in both states and the pixels
 * are read back:
 *
 *  * **folded: one row, open: three.** The middle column of each render is walked
 *    and split into runs of card and of page; a row is a card band, and the 3 dp
 *    the rows of one card keep between them is a page run of 3 px. So the folded
 *    group has to read one band, the open one three bands in ONE card (no gap of
 *    a card's own 8 dp between them).
 *  * **the header is a row of the card, not a title above it.** It keeps its
 *    height when the body rolls up, and it is at least as tall as the 56 dp a
 *    Material list row is (the preview's `.md-row` min-height) instead of hugging
 *    its label, so the folded group looks like a single row of the same family as
 *    the ones the other groups are made of.
 *  * **the two switches are in the body, and only there.** Both are drawn checked,
 *    so their track is `primary`; a small swatch of
 *    `MaterialTheme.colorScheme.primary` is painted in the corner of the scene to
 *    read that colour back. The folded render must hold no primary pixel below
 *    the header, the open one must hold primary ink in exactly two bands below it
 *    - the two rows.
 *  * **the chevron turns.** `ExpandMore` is a downward chevron: its ink is widest
 *    at its top (the two arm tips) and narrowest at its bottom (the vertex), so
 *    the horizontal spread of the ink's first rows against its last rows says
 *    which way it points. It must read wide-then-narrow folded and narrow-then-
 *    wide open, which is the half turn; and its darkest pixel must be nearer
 *    `primary` than `onSurfaceVariant`, the tint the preview gives it.
 *
 * PNGs are written under `.ignore/devicesync/` so the two states can be looked at.
 */
object DeviceSyncFoldCheck {

    private val outDir = File(".ignore/devicesync")

    /** The settings column's own width, and room for the folded and the open card. */
    private const val WIDTH = 720
    private const val HEIGHT = 420

    /** Where the group starts, below the corner swatches. */
    private const val TOP_PADDING = 40

    /** A Material list row: the preview's `.md-row` min-height, in dp (density 1). */
    private const val ROW_MIN_HEIGHT = 56

    @JvmStatic
    fun main(args: Array<String>) {
        outDir.mkdirs()
        val failures = mutableListOf<String>()

        val folded = render(expanded = false)
        File(outDir, "folded.png").writeBytes(folded)
        val open = render(expanded = true)
        File(outDir, "open.png").writeBytes(open)

        val foldedBands = bands(folded)
        val openBands = bands(open)
        println("folded: rows=${foldedBands.bands.size} seams=${foldedBands.seams}")
        println("open:   rows=${openBands.bands.size} seams=${openBands.seams}")

        if (foldedBands.bands.size != 1) {
            failures += "the folded group must be one row (its header), read ${foldedBands.bands.size}"
        }
        if (openBands.bands.size != 3) {
            failures += "the open group must be three rows (the header and the two switches), read ${openBands.bands.size}"
        }
        if (openBands.seams.size != 2 || openBands.seams.any { it !in 2..6 }) {
            failures += "the open group must be one card: two 3 dp seams between its rows, read ${openBands.seams}"
        }

        val header = foldedBands.bands.firstOrNull()
        if (header != null) {
            val height = header.last - header.first + 1
            println("header height: folded=$height")
            if (height < ROW_MIN_HEIGHT) {
                failures += "the header is a list row and must be at least $ROW_MIN_HEIGHT dp tall, read $height"
            }
            val openHeader = openBands.bands.firstOrNull()
            if (openHeader != null && kotlin.math.abs((openHeader.last - openHeader.first + 1) - height) > 2) {
                failures += "the header changed height when the group folded " +
                    "(${openHeader.last - openHeader.first + 1} vs $height px): rolling the body up must not resize its own row"
            }
        }

        val foldedSwitches = switchBands(folded, foldedBands)
        val openSwitches = switchBands(open, openBands)
        println("switch rows carrying primary ink: folded=$foldedSwitches open=$openSwitches")
        if (foldedSwitches != 0) {
            failures += "the folded group drew $foldedSwitches switch(es): the body is still on screen"
        }
        if (openSwitches != 2) {
            failures += "the open group must draw primary switch ink in exactly 2 rows, read $openSwitches"
        }

        val foldedChevron = chevron(folded, foldedBands)
        val openChevron = chevron(open, openBands)
        println("chevron: folded=$foldedChevron")
        println("         open=$openChevron")
        if (foldedChevron == null || openChevron == null) {
            failures += "the header's chevron painted no ink in one of the two states"
        } else {
            if (foldedChevron.topSpread <= foldedChevron.bottomSpread) {
                failures += "the folded chevron must point down (wide at the top, a vertex at the bottom), " +
                    "read ${foldedChevron.topSpread}/${foldedChevron.bottomSpread} px of ink"
            }
            if (openChevron.topSpread >= openChevron.bottomSpread) {
                failures += "the open chevron must point up, half a turn from the folded one, " +
                    "read ${openChevron.topSpread}/${openChevron.bottomSpread} px of ink"
            }
            if (!openChevron.tintedPrimary) {
                failures += "the chevron is not tinted with primary: its darkest pixel is ${openChevron.darkest}" +
                    " (primary ${openChevron.primary}, on-surface-variant ${openChevron.surfaceVariant})"
            }
        }

        if (failures.isEmpty()) {
            println()
            println("OK: the group folds to its header row, the two switches are in the body, and the chevron turns")
            return
        }
        println()
        println("FAILED:")
        failures.forEach { println("  - $it") }
        throw IllegalStateException("device sync fold check failed (${failures.size} case(s))")
    }

    /** The group exactly as Device sync draws it: the app's own two volume items. */
    private fun render(expanded: Boolean): ByteArray {
        val scene = ImageComposeScene(WIDTH, HEIGHT) {
            AppTheme(mode = ThemeMode.LIGHT, accent = Color(0xFFED5564), language = "en") {
                Box(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
                    Column(Modifier.padding(horizontal = 24.dp, vertical = TOP_PADDING.dp)) {
                        M3SettingsCollapsibleGroup(
                            title = Localization.get("en", "advanced"),
                            expanded = expanded,
                            onExpandedChange = {},
                            items = listOf(
                                M3SettingsItem(
                                    icon = Icons.Filled.VolumeUp,
                                    title = { Text(Localization.get("en", "sync_vivi_volume")) },
                                    trailing = { Switch(checked = true, onCheckedChange = {}) },
                                ),
                                M3SettingsItem(
                                    icon = Icons.Filled.Speaker,
                                    title = { Text(Localization.get("en", "sync_os_volume")) },
                                    trailing = { Switch(checked = true, onCheckedChange = {}) },
                                ),
                            ),
                        )
                    }
                    // The colours the checks compare against, read back from the
                    // theme instead of hardcoded: the theme is built from the accent
                    // and a literal would break on any restyle. `primary` at the top
                    // left (the switches' track and the chevron's tint),
                    // `onSurfaceVariant` at the bottom left (the tint a chevron is
                    // NOT allowed to have).
                    Box(
                        Modifier
                            .align(Alignment.TopStart)
                            .size(2.dp)
                            .background(MaterialTheme.colorScheme.primary),
                    )
                    Box(
                        Modifier
                            .align(Alignment.BottomStart)
                            .size(2.dp)
                            .background(MaterialTheme.colorScheme.onSurfaceVariant),
                    )
                }
            }
        }
        return try {
            scene.render().encodeToData(EncodedImageFormat.PNG, 100)!!.bytes
        } finally {
            scene.close()
        }
    }

    /** The card bands down the middle of a render, and the page runs between them. */
    private data class Bands(val bands: List<IntRange>, val seams: List<Int>)

    private fun bands(png: ByteArray): Bands {
        val image = javax.imageio.ImageIO.read(png.inputStream())
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
        val bands = mutableListOf<IntRange>()
        val seams = mutableListOf<Int>()
        var y = 0
        for ((onCard, length) in runs) {
            if (onCard) bands += y until (y + length) else if (bands.isNotEmpty()) seams += length
            y += length
        }
        // The last page run is the empty space under the card, not a seam.
        return Bands(bands = bands, seams = seams.dropLast(1))
    }

    /** How many of the bands below the header carry `primary` ink. */
    private fun switchBands(png: ByteArray, measured: Bands): Int {
        val image = javax.imageio.ImageIO.read(png.inputStream())
        val primary = image.getRGB(0, 0)
        return measured.bands.drop(1).count { band ->
            var ink = 0
            for (y in band) {
                for (x in 0 until image.width) {
                    if (near(image.getRGB(x, y), primary, tolerance = 24)) ink++
                }
            }
            ink > 100
        }
    }

    /**
     * The header's trailing chevron: the ink in the right end of the header row, its
     * top and bottom spread and its darkest colour.
     */
    private data class Chevron(
        val topSpread: Int,
        val bottomSpread: Int,
        val ink: Int,
        val tintedPrimary: Boolean,
        val darkest: String,
        val primary: String,
        val surfaceVariant: String,
    )

    private fun chevron(png: ByteArray, measured: Bands): Chevron? {
        val image = javax.imageio.ImageIO.read(png.inputStream())
        val header = measured.bands.firstOrNull() ?: return null
        val middle = (header.first + header.last) / 2
        val primary = image.getRGB(0, 0)
        val surfaceVariant = image.getRGB(0, image.height - 1)
        // The row's own surface, read where the title is not drawn, and the page
        // beside the card: the card's rounded corners make the far right columns of
        // the header's edge rows page-coloured, so the box stays off those rows.
        val card = image.getRGB(image.width / 2, middle)
        val page = image.getRGB(image.width - 2, middle)
        var cardRight = image.width - 1
        while (cardRight > 0 && near(image.getRGB(cardRight, middle), page, 12)) cardRight--
        val ink = mutableListOf<Pair<Int, Int>>()
        var darkest = card
        var darkestDistance = Int.MAX_VALUE
        for (y in (middle - 11)..(middle + 11)) {
            for (x in (cardRight - 60)..(cardRight - 2)) {
                val c = image.getRGB(x, y)
                if (near(c, card, 120)) continue
                ink += x to y
                val d = distance(c, primary)
                if (d < darkestDistance) {
                    darkestDistance = d
                    darkest = c
                }
            }
        }
        if (ink.size < 12) return null
        val ys = ink.map { it.second }.sorted()
        // The first and last two rows that carry ink, not just one: a single row of
        // an antialiased 45 degree stroke is a couple of faint pixels.
        val topRows = ys.take(2).toSet()
        val bottomRows = ys.takeLast(2).toSet()
        fun spread(rows: Set<Int>): Int {
            val xs = ink.filter { it.second in rows }.map { it.first }
            return xs.max() - xs.min()
        }
        return Chevron(
            topSpread = spread(topRows),
            bottomSpread = spread(bottomRows),
            ink = ink.size,
            tintedPrimary = darkestDistance < distance(darkest, surfaceVariant),
            darkest = hex(darkest),
            primary = hex(primary),
            surfaceVariant = hex(surfaceVariant),
        )
    }

    private fun hex(c: Int) = "#" + Integer.toHexString(c and 0xFFFFFF)

    private fun distance(a: Int, b: Int): Int =
        kotlin.math.abs(((a shr 16) and 255) - ((b shr 16) and 255)) +
            kotlin.math.abs(((a shr 8) and 255) - ((b shr 8) and 255)) +
            kotlin.math.abs((a and 255) - (b and 255))

    private fun near(a: Int, b: Int, tolerance: Int = 6): Boolean = distance(a, b) <= tolerance
}
