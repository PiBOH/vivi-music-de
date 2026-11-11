@file:OptIn(ExperimentalComposeUiApi::class)

package com.music.vivi.desktop

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.ImageComposeScene
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.PointerButtons
import androidx.compose.ui.unit.dp
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.input.pointer.PointerType
import com.music.innertube.models.AlbumItem
import com.music.innertube.models.Artist
import com.music.innertube.models.PlaylistItem
import com.music.innertube.models.SongItem
import com.music.innertube.pages.HomePage
import org.jetbrains.skia.EncodedImageFormat
import java.awt.image.BufferedImage
import java.io.File
import javax.imageio.ImageIO

/**
 * Headless render of Home's shelves, run with
 *
 *     ./gradlew :desktop:homeRenderCheck
 *
 * The live Home needs the network, so [HomeShelfStack] takes the page and this
 * check hands it a fixed one with the same album cards the service returns.
 * What it asserts is read off the painted pixels instead of trusted from the
 * code, because a shelf that silently lost its card size, its 12 dp corner or
 * its arrows would still compile and still draw something:
 *
 *  - Classic Home is BitChord's shelf: every card the same 150 dp rounded
 *    square on a 12 dp corner, 14 dp apart on the page's 24 dp gutter, the
 *    trailing one cut by the window, with no lead card, no arrow and no dot row;
 *  - the m3e Home paints one row for every category of the page (the Library
 *    and BitChord's eight), in feed order, each on the 16 dp inset with its 180
 *    dp card and no dot row, and its row builder returns those same titles;
 *  - Explore's New releases row is the reference video's multi-browse row: a
 *    245 dp leading card, 162 dp cards after it and a trailing card the window
 *    cuts to 0.43-0.46 of a card, which is what the video's own frames measure;
 *  - a scrollable row carries the Expressive indicator under it;
 *  - the hover arrow exists only while the pointer is on the row, at the row's
 *    trailing edge, and pressing it pages the shelf onto a new leading card,
 *    which the indicator's pill says by moving to the next page's dot.
 *
 * Frames land under `desktop/.ignore/render-home/`: `home-1364.png` and
 * `home-1024.png` at rest, `explore-1364.png` for Explore's New releases row
 * (the screen's own row composable), `explore-1364-hover.png` with its arrows
 * and `explore-1364-paged.png` after the press.
 */
object HomeRenderCheck {

    private val outDir = File(".ignore/render-home")

    /** Home's shelf headings and the card rows the geometry is read from. */
    private fun page(): HomePage = HomePage(
        chips = null,
        sections = listOf(
            // Sixteen albums, not seven: a page of six cards has to be able to
            // reach a new leading card, and a row whose content is barely wider
            // than its window cannot scroll one page at all.
            section("Album per te", 16),
            section("Voglia di ballare", 16),
            section("Cover e remix", 16),
        ),
    )

    private fun section(title: String, count: Int): HomePage.Section = HomePage.Section(
        title = title,
        label = null,
        thumbnail = null,
        endpoint = null,
        items = (1..count).map { n ->
            AlbumItem(
                browseId = "MPREb-$n",
                playlistId = "OLAK5uy-$n",
                title = "$title $n",
                artists = listOf(Artist(name = "Artist $n", id = null)),
                year = 2026,
                // No URL: the card's own surface paints the cover slot, which is
                // the rectangle the geometry is measured on and keeps the check
                // offline.
                thumbnail = "",
            )
        },
    )

    /** The shelves, in the theme, filling a frame of the given size. */
    private fun home(): @androidx.compose.runtime.Composable () -> Unit = {
        Box(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.surfaceContainer)) {
            HomeShelfStack(
                home = page(),
                localCollections = emptyList(),
                language = "en",
                onOpenAlbum = {},
                onOpenArtist = {},
                onOpenPlaylist = {},
                onOpenLocalPlaylist = {},
                onPlaySong = {},
            )
        }
    }

    /**
     * Explore's New releases row.
     *
     * The screen loads its albums from the service, which a headless check cannot
     * do, so what is painted here is the screen's own row composable
     * ([NewReleaseShelf]) with the same albums and the same 16 dp page inset.
     * The row's geometry is then read off the pixels exactly as the Home shelves
     * are.
     */
    private fun explore(): @Composable () -> Unit = {
        Box(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.surfaceContainer)) {
            Column(Modifier.padding(16.dp)) {
                Text(
                    Localization.get("en", "new_release_albums"),
                    style = MaterialTheme.typography.titleLarge,
                    modifier = Modifier.padding(bottom = 10.dp),
                )
                NewReleaseShelf(
                    language = "en",
                    albums = page().sections.first().items,
                    onOpenAlbum = {},
                )
            }
        }
    }

    private fun scene(
        width: Int,
        height: Int,
        content: @Composable () -> Unit,
    ): ImageComposeScene =
        ImageComposeScene(width, height) {
            AppTheme(mode = ThemeMode.DARK, accent = Color(0xFFED5564), language = "en") {
                content()
            }
        }

    /**
     * The scene's clock, in ns, one frame at a time.
     *
     * `render()` with no time draws the frame but does not move the clock, and
     * an animation that waits for `withFrameNanos` then never advances: the
     * arrow's `animateScrollToItem` started and never finished. Every frame the
     * check draws goes through here, and the time is shared with [encode] so
     * the clock only ever moves forward.
     */
    private var clockNanos = 0L

    private fun nextFrame(): Long {
        clockNanos += 40_000_000L
        return clockNanos
    }

    /** Renders [frames] frames, which is what lets an animation reach its end. */
    private fun pump(scene: ImageComposeScene, frames: Int = 6) {
        repeat(frames) {
            scene.render(nextFrame())
        }
    }

    private fun encode(scene: ImageComposeScene): ByteArray =
        scene.render(nextFrame()).encodeToData(EncodedImageFormat.PNG, 100)!!.bytes

    private fun render(
        width: Int,
        height: Int,
        name: String,
        content: @Composable () -> Unit = home(),
    ): BufferedImage {
        val scene = scene(width, height, content)
        val png = try {
            pump(scene)
            encode(scene)
        } finally {
            scene.close()
        }
        outDir.mkdirs()
        File(outDir, name).writeBytes(png)
        return ImageIO.read(png.inputStream())
    }

    private fun diff(a: Int, b: Int): Int {
        val ar = (a shr 16) and 0xFF
        val ag = (a shr 8) and 0xFF
        val ab = a and 0xFF
        val br = (b shr 16) and 0xFF
        val bg = (b shr 8) and 0xFF
        val bb = b and 0xFF
        return kotlin.math.abs(ar - br) + kotlin.math.abs(ag - bg) + kotlin.math.abs(ab - bb)
    }

    /** The y ranges of a painted card row: the rows where most of the width is cover. */
    private fun coverBands(img: BufferedImage, bg: Int): List<IntRange> {
        val out = mutableListOf<IntRange>()
        var start = -1
        for (y in 0 until img.height) {
            val painted = (0 until img.width).count { x -> diff(img.getRGB(x, y), bg) > 14 }
            val isCover = painted >= img.width / 3
            if (isCover && start < 0) start = y
            if (!isCover && start >= 0) {
                out += start until y
                start = -1
            }
        }
        if (start >= 0) out += start until img.height
        return out
    }

    /** The x ranges of the cards inside one row. */
    private fun cards(img: BufferedImage, band: IntRange, bg: Int): List<IntRange> {
        val out = mutableListOf<IntRange>()
        var start = -1
        for (x in 0 until img.width) {
            // Any real paint, not most of the band: a card's artwork is a cookie,
            // so its outline tapers to a point near the box's edge and a column
            // there carries only the middle rows.
            val painted = band.count { y -> diff(img.getRGB(x, y), bg) > 14 }
            val isCard = painted >= 2
            if (isCard && start < 0) start = x
            if (!isCard && start >= 0) {
                if (x - start >= 40) out += start until x
                start = -1
            }
        }
        if (start >= 0) out += start until img.width
        return out
    }

    /** One painted card: its bounding box and how much of that box the cover fills. */
    private data class Card(val x0: Int, val x1: Int, val y0: Int, val y1: Int, val painted: Int) {
        val width get() = x1 - x0 + 1
        val height get() = y1 - y0 + 1
        val coverage get() = painted.toDouble() / (width * height)
    }

    private fun card(img: BufferedImage, xs: IntRange, band: IntRange, bg: Int): Card {
        var x0 = Int.MAX_VALUE
        var x1 = -1
        var y0 = Int.MAX_VALUE
        var y1 = -1
        var painted = 0
        val from = (band.first - 24).coerceAtLeast(0)
        // The title starts a few px under the cover: the window stops where the
        // artwork does, or the text would widen the box and lower the coverage.
        val to = (band.last + 4).coerceAtMost(img.height - 1)
        for (y in from..to) {
            for (x in xs) {
                if (diff(img.getRGB(x, y), bg) > 14) {
                    painted++
                    if (x < x0) x0 = x
                    if (x > x1) x1 = x
                    if (y < y0) y0 = y
                    if (y > y1) y1 = y
                }
            }
        }
        return Card(x0, x1, y0, y1, painted)
    }

    /**
     * The indicator's pill: the longest run of painted px centred under a shelf.
     *
     * The dots sit under the card's title and subtitle, 8 dp under the row, and
     * the pill is 18 dp wide where a glyph's stroke is a few px, so a run of at
     * least 14 px within 40 px of the centre is the indicator and not text.
     */
    private fun indicatorPill(img: BufferedImage, band: IntRange, bg: Int): Pair<Int, Int> {
        val center = img.width / 2
        val from = (band.last + 8).coerceAtLeast(0)
        val to = (band.last + 120).coerceAtMost(img.height - 1)
        var longest = 0
        var where = -1
        for (y in from..to) {
            var run = 0
            var start = -1
            for (x in center - 160..center + 160) {
                if (diff(img.getRGB(x, y), bg) > 14) {
                    if (run == 0) start = x
                    run++
                } else {
                    if (run > longest && start >= center - 40 && start + run <= center + 40) {
                        longest = run
                        where = start
                    }
                    run = 0
                }
            }
            if (run > longest && start >= center - 40 && start + run <= center + 40) {
                longest = run
                where = start
            }
        }
        return longest to where
    }

    private fun indicatorRun(img: BufferedImage, band: IntRange, bg: Int): Int =
        indicatorPill(img, band, bg).first

    private fun check(condition: Boolean, message: String) {
        if (!condition) throw IllegalStateException(message)
    }

    /**
     * The multi-browse row, the cookie covers and the indicator, on one frame.
     *
     * [pad] is the row's own leading gutter and [right] its trailing one: Home
     * indents its shelves by 24 dp,
     * Explore's New releases row sits inside the page's 16 dp padding and passes
     * none. [minShelves] is how many cover rows the frame must paint, and
     * [coverFill] the share of a card's box its drawing fills: a cookie about
     * 0.82, a rounded card 0.99.
     */
    private fun shelf(
        name: String,
        img: BufferedImage,
        pad: Int,
        minShelves: Int = 2,
        right: Int = 0,
        coverFill: ClosedFloatingPointRange<Double>? = 0.55..0.90,
        roundedCards: Boolean = false,
        expectIndicator: Boolean = true,
    ) {
        val bg = img.getRGB(0, img.height - 1)
        val bands = coverBands(img, bg)
        check(bands.size >= minShelves) {
            "$name: expected at least $minShelves cover band(s), painted ${bands.size}"
        }
        val band = bands.first()
        val ranges = cards(img, band, bg)
        check(ranges.size >= 3) { "$name: the first shelf painted ${ranges.size} card(s)" }
        val boxes = ranges.map { card(img, it, band, bg) }
        val widths = boxes.map { it.width }
        println("%-12s shelf: %d cards, widths %s".format(name, boxes.size, widths))
        check(widths.max() - widths.min() >= 40) {
            "$name: every card is the same width ($widths), so the shelf lost its multi-browse sizes"
        }
        // The video's own ratios: its lead is 1.49 to 1.53 medium cards wide. A
        // cookie's painted outline is inset a few px at its tapered ends, which
        // lifts the lead's ratio a little, so the band holds both.
        val leadWidth = boxes.first().width
        val mediumWidth = boxes[1].width
        val leadRatio = leadWidth.toDouble() / mediumWidth
        println("%-12s lead/medium: %.2f (%d px / %d px)".format(name, leadRatio, leadWidth, mediumWidth))
        check(leadRatio in 1.40..1.80) {
            "$name: the lead is %.2f medium cards wide; the reference video measures 1.49 to 1.53"
                .format(leadRatio)
        }
        // The painted outline of a cookie is inset a few px at its tapered
        // ends, so a width alone cannot say the lead's own size. The centres can:
        // the first sits at pad + lead/2 and the step to the next is
        // lead/2 + gap + card/2, and a different card size moves both.
        val leadDp = ShelfLeadWidth.value
        val cardDp = ShelfCardWidth.value
        val gapDp = ShelfGap.value
        val center0 = boxes.first().x0 + boxes.first().width / 2
        val center1 = boxes[1].x0 + boxes[1].width / 2
        val expectedCenter = pad + leadDp / 2
        val expectedStep = leadDp / 2 + gapDp + cardDp / 2
        println(
            "%-12s centres: first %d, step %d (lead %.0f, card %.0f, gap %.0f)"
                .format(name, center0, center1 - center0, leadDp, cardDp, gapDp),
        )
        check(kotlin.math.abs(center0 - expectedCenter) <= 3.0) {
            "$name: the leading card's centre is $center0 px, expected $pad + ${leadDp / 2}"
        }
        check(kotlin.math.abs((center1 - center0) - expectedStep) <= 4.0) {
            "$name: the second card's centre is ${center1 - center0} px after the lead, " +
                "expected ${leadDp / 2} + $gapDp + ${cardDp / 2}"
        }
        check(boxes.last().x1 >= img.width - 1 - right) {
            "$name: the trailing card fits inside the window; the shelf must be cut at the edge"
        }
        // The video cuts its trailing card to 0.43-0.46 of a medium card. The
        // strip is what says the row continues, so it is measured, not assumed.
        val stripFraction = boxes.last().width.toDouble() / mediumWidth
        println("%-12s trailing strip: %.2f of a card".format(name, stripFraction))
        check(stripFraction in 0.33..0.60) {
            "$name: the trailing card shows %.2f of a medium card; the reference video shows 0.43 to 0.46"
                .format(stripFraction)
        }
        boxes.forEachIndexed { index, box ->
            println(
                "%-12s card %d: %dx%d at %d, fill %.2f".format(
                    name, index, box.width, box.height, box.x0, box.coverage,
                ),
            )
            // A cookie fills about 0.82 of its painted box, and the range is the
            // shape the row is supposed to draw. An m3e card passes no range:
            // its scrim fades into the page and the card is told apart by its
            // corner below, not by a share of paint.
            if (coverFill != null) {
                check(box.coverage in coverFill) {
                    "$name: card $index fills %.2f of its box, expected %.2f..%.2f"
                        .format(box.coverage, coverFill.start, coverFill.endInclusive)
                }
            }
            if (roundedCards) {
                // The fill alone cannot tell a rounded card from a cookie: the
                // scrim's bottom edge fades into the page. The corner can - a
                // 16 dp radius leaves the box's top-left pixel unpainted, a
                // square card paints it.
                check(diff(img.getRGB(box.x0, box.y0), bg) <= 14) {
                    "$name: card $index paints its top-left corner, so it is square instead of 16 dp rounded"
                }
                // Sampled past the arc, not on it: at 16 px in, the corner's
                // own radius still leaves the sample on the edge.
                check(diff(img.getRGB(box.x0 + 24, box.y0 + 24), bg) > 14) {
                    "$name: card $index is not painted inside its rounded corner"
                }
            }
        }
        val pill = indicatorRun(img, band, bg)
        println("%-12s indicator: %d px pill under the shelf".format(name, pill))
        if (expectIndicator) {
            check(pill >= 14) {
                "$name: no pill under the shelf ($pill px), so the carousel indicator is gone"
            }
        } else {
            check(pill < 14) {
                "$name: a dot row is painted under the shelf ($pill px); the Home draws none"
            }
        }
    }

    /**
     * The whole category set, painted: one cover band per row, the Library and
     * every category of the page, in the page's order.
     *
     * A row the Home dropped leaves the frame one band short, which is the
     * failure this report exists for. Each band is then read on its own: the
     * first card on the 16 dp page inset, the 180 dp card height, and no dot row
     * under it. The rows the builder produces are compared with the page too, so
     * the set is proven in the model and in the pixels instead of only one of
     * the two.
     */
    private fun m3eAllCategories(img: BufferedImage) {
        val expected = listOf(Localization.get("en", "library")) + M3E_CATEGORIES
        val bg = img.getRGB(0, img.height - 1)
        // A row's scrim fades into the page ~28 px before the card's edge, and
        // the rounded bottom edge paints its own 2 px line: both are the same
        // row, so bands closer than 32 px are one band before they are counted.
        val bands = mutableListOf<IntRange>()
        coverBands(img, bg).forEach { band ->
            val last = bands.lastOrNull()
            if (last != null && band.first - last.last <= 32 && band.last - band.first <= 4) {
                bands[bands.size - 1] = last.first..band.last
            } else {
                bands += band
            }
        }
        println("m3e-full    rows: ${bands.size} painted for ${expected.size} categories")
        check(bands.size == expected.size) {
            "m3e-full: painted ${bands.size} row(s) for ${expected.size} categories " +
                "(${expected.joinToString()}), so the Home lost one"
        }
        bands.forEachIndexed { index, band ->
            val ranges = cards(img, band, bg)
            check(ranges.isNotEmpty()) { "m3e-full: row $index painted no card" }
            // The band is the whole 180 dp card here, so it is measured on
            // its own band instead of the 24 px the Classic shelf reserves.
            val box = coverBox(img, ranges.first(), band, bg)
            check(kotlin.math.abs(box.x0 - 16) <= 3) {
                "m3e-full: row $index starts at ${box.x0} px, expected the 16 dp page inset"
            }
            check(box.height in 168..184) {
                "m3e-full: row $index's first card paints ${box.height} px, expected the 180 dp card"
            }
            val pill = indicatorRun(img, band, bg)
            check(pill < 14) {
                "m3e-full: row $index draws a $pill px dot row under the shelf; the Home has none"
            }
        }
        val titles = m3eRows(m3ePage(), emptyList(), m3eHistory(), "en").map { it.title }
        println("m3e-full    rows: ${titles.joinToString()}")
        check(titles == expected) {
            "m3e-full: the Home builds ${titles.joinToString()} instead of ${expected.joinToString()}"
        }
    }

    /**
     * BitChord's shelf, read off the frame: one heading and one row of equal
     * 150 dp rounded-square cards, 14 dp apart, starting on the 24 dp gutter
     * and cut by the window's edge.
     *
     * A row that grew a lead card, an arrow or a dot row is not this shelf: the
     * reference draws every card at one size and nothing under the row. The
     * corner is read on the pixels too, because a square card would paint its
     * top-left pixel and the 12 dp corner must not.
     */
    private fun bitChordShelf(name: String, img: BufferedImage, pad: Int, cardDp: Int, gapDp: Int) {
        val bg = img.getRGB(0, img.height - 1)
        val bands = coverBands(img, bg)
        check(bands.size >= 3) {
            "$name: expected at least 3 cover rows, painted ${bands.size}"
        }
        val band = bands.first()
        val ranges = cards(img, band, bg)
        check(ranges.size >= 4) { "$name: the first shelf painted ${ranges.size} card(s)" }
        val boxes = ranges.map { box -> coverBox(img, box, band, bg) }
        val widths = boxes.map { it.width }
        println("%-12s shelf: %d cards, widths %s".format(name, boxes.size, widths))
        // The last box is the card the window cuts: every card before it is the
        // same square, and the cut one is narrower than a card. BitChord's own
        // row ends the same way.
        val full = boxes.filter { it.width >= cardDp / 2 }
        val fullWidths = full.map { it.width }
        check(full.size >= 4) { "$name: only ${full.size} full card(s) painted" }
        check(fullWidths.max() - fullWidths.min() <= 2) {
            "$name: the cards are not one size ($fullWidths), so the row lost BitChord's equal cards"
        }
        check(kotlin.math.abs(fullWidths.first() - cardDp) <= 2) {
            "$name: the card paints ${fullWidths.first()} px, expected the $cardDp dp square"
        }
        val step = boxes[1].x0 - boxes[0].x0
        println(
            "%-12s step: %d px (card %d + gap %d), first at %d"
                .format(name, step, cardDp, gapDp, boxes.first().x0),
        )
        check(kotlin.math.abs(step - (cardDp + gapDp)) <= 3) {
            "$name: the second card starts $step px after the first, expected ${cardDp + gapDp}"
        }
        check(kotlin.math.abs(boxes.first().x0 - pad) <= 3) {
            "$name: the row starts at ${boxes.first().x0} px, expected the $pad dp gutter"
        }
        check(band.count { y -> diff(img.getRGB(img.width - 2, y), bg) > 14 } > 40) {
            "$name: the row does not reach the window's edge, so its trailing card is not cut"
        }
        boxes.forEachIndexed { index, box ->
            println(
                "%-12s card %d: %dx%d at %d, fill %.2f".format(
                    name, index, box.width, box.height, box.x0, box.coverage,
                ),
            )
            check(box.coverage in 0.93..1.0) {
                "$name: card $index fills %.2f of its box, expected a rounded square's 0.97"
                    .format(box.coverage)
            }
            check(diff(img.getRGB(box.x0, box.y0), bg) <= 14) {
                "$name: card $index paints its top-left corner, so it is square instead of 12 dp rounded"
            }
            // The window cuts the trailing card, so it can be narrower than the
            // arc's own 24 px sample; a full card is asked to paint inside it.
            if (box.width >= 48) {
                check(diff(img.getRGB(box.x0 + 24, box.y0 + 24), bg) > 14) {
                    "$name: card $index is not painted inside its rounded corner"
                }
            }
        }
        val pill = indicatorRun(img, band, bg)
        println("%-12s indicator: %d px pill under the shelf".format(name, pill))
        check(pill < 14) {
            "$name: a dot row is painted under the shelf ($pill px); BitChord's row has none"
        }
    }

    /** One card's box inside the cover band alone, so the heading above cannot widen it. */
    private fun coverBox(img: BufferedImage, xs: IntRange, band: IntRange, bg: Int): Card {
        var x0 = Int.MAX_VALUE
        var x1 = -1
        var y0 = Int.MAX_VALUE
        var y1 = -1
        var painted = 0
        for (y in band) {
            for (x in xs) {
                if (diff(img.getRGB(x, y), bg) > 14) {
                    painted++
                    if (x < x0) x0 = x
                    if (x > x1) x1 = x
                    if (y < y0) y0 = y
                    if (y > y1) y1 = y
                }
            }
        }
        return Card(x0, x1, y0, y1, painted)
    }

    /** The arrows exist on hover only, and the forward one pages the row. */
    private fun paging() {
        val width = 1364
        val height = 900
        val scene = scene(width, height, explore())
        try {
            pump(scene)
            val rest = ImageIO.read(File(outDir, "explore-1364.png").inputStream())
            val bg = rest.getRGB(0, rest.height - 1)
            val band = coverBands(rest, bg).first()
            val rowMiddle = (band.first + band.last) / 2f
            val restPill = indicatorPill(rest, band, bg)

            val resting = encode(scene)
            scene.sendPointerEvent(PointerEventType.Move, Offset(400f, rowMiddle))
            pump(scene, 4)
            val hovered = encode(scene)
            outDir.mkdirs()
            File(outDir, "explore-1364-hover.png").writeBytes(hovered)
            val hoverImage = ImageIO.read(hovered.inputStream())
            var arrowPixels = 0
            for (y in band.first..band.last) {
                for (x in 0 until width) {
                    if (rest.getRGB(x, y) != hoverImage.getRGB(x, y)) arrowPixels++
                }
            }
            // The forward arrow is a circle at the row's trailing edge, so the
            // last 44 px of the row must change when it appears. Where it sits
            // vertically is read off those changed px: the arrow centres on the
            // row including the title under the cover, not on the cover band.
            var edgePixels = 0
            var edgeY = 0L
            for (y in band.first..band.last) {
                for (x in width - 44 until width) {
                    if (rest.getRGB(x, y) != hoverImage.getRGB(x, y)) {
                        edgePixels++
                        edgeY += y
                    }
                }
            }
            val arrowY = if (edgePixels > 0) edgeY.toFloat() / edgePixels else rowMiddle
            println(
                "hover frame: $arrowPixels px differ in the first row, " +
                    "$edgePixels at its trailing edge (arrow at %.0f,%.0f)".format(width - 17f, arrowY),
            )
            check(arrowPixels > 300) {
                "the shelf's arrows did not appear while the pointer was on the row ($arrowPixels px changed)"
            }
            check(edgePixels > 80) {
                "the row's trailing edge drew no arrow while the pointer was on it ($edgePixels px changed)"
            }

            // Hover the arrow before pressing it: the arrows exist only while
            // the pointer is on the row, and a press that carries the pointer to
            // them in the same event lands after they have gone.
            val arrowX = width - 17f
            scene.sendPointerEvent(PointerEventType.Move, Offset(arrowX, arrowY))
            pump(scene, 4)
            // A press and a release dispatched before one frame are one batch to
            // the gesture detector and never become a click.
            scene.sendPointerEvent(
                PointerEventType.Press,
                Offset(arrowX, arrowY),
                type = PointerType.Mouse,
                buttons = PointerButtons(isPrimaryPressed = true),
            )
            scene.render(nextFrame())
            scene.sendPointerEvent(
                PointerEventType.Release,
                Offset(arrowX, arrowY),
                type = PointerType.Mouse,
                buttons = PointerButtons(),
            )
            pump(scene, 90)
            val paged = encode(scene)
            File(outDir, "explore-1364-paged.png").writeBytes(paged)
            val after = ImageIO.read(paged.inputStream())
            var moved = 0
            for (y in 0 until height) {
                for (x in 0 until width) {
                    if (rest.getRGB(x, y) != after.getRGB(x, y)) moved++
                }
            }
            val afterBand = coverBands(after, bg).first()
            val afterPill = indicatorPill(after, afterBand, bg)
            println(
                "paged frame: $moved px differ; pill %d px at %d, was %d px at %d"
                    .format(afterPill.first, afterPill.second, restPill.first, restPill.second),
            )
            // A page here is six cards: the covers are the same placeholder on
            // every card, so the row's geometry cannot move and what changes is
            // the text under each cover. The pill below is the proof of the page.
            check(moved > 1_500) { "the forward arrow did not page the shelf ($moved px changed)" }
            check(restPill.first >= 14 && afterPill.first >= 14) {
                "the indicator lost its pill while paging (rest ${restPill.first} px, after ${afterPill.first} px)"
            }
            check(afterPill.second >= restPill.second + 10) {
                "the forward arrow did not page the row: the indicator's pill sits at ${afterPill.second} px, " +
                    "was ${restPill.second}, so the row is still on its first page"
            }
            val afterCards = cards(after, afterBand, bg).map { card(after, it, afterBand, bg) }
            check(afterCards.isNotEmpty()) { "after one page the shelf painted no card" }
            check(afterCards.first().width >= ShelfLeadWidth.value - 30) {
                "after one page the leading card is ${afterCards.first().width} px wide, " +
                    "not the ${ShelfLeadWidth.value} dp lead"
            }
        } finally {
            scene.close()
        }
    }

    /**
     * BitChord's Home categories as its own screenshots carry them. Their names
     * are the listener's, so the app reads them from the feed instead of
     * hard-coding them; the check paints this set to prove that every section
     * the page holds becomes a row.
     */
    private val M3E_CATEGORIES = listOf(
        "Playlist Pop",
        "Successi pop senza tempo",
        "Nuove scoperte",
        "Vecchi successi",
        "Discoteca",
        "Party del passato",
        "Playlist in primo piano per te",
        "Album per te",
    )

    /** The m3e Home's page: the Library plus every category, each with its cards. */
    private fun m3ePage(): HomePage = HomePage(
        chips = null,
        sections = M3E_CATEGORIES.map { title -> section(title, 8) },
    )

    /** Eight played tracks, so the Library row has to be cut like the others. */
    private fun m3eHistory(): List<HistoryEntry> = (1..8).map { n ->
        HistoryEntry(
            videoId = "history-$n",
            title = "Ascoltato $n",
            artist = "Artist $n",
            thumbnail = "",
            durationMs = 200_000L,
            playedAt = n.toLong(),
            plays = 1,
        )
    }

    /**
     * Home in the m3e style. [home] is null for the loading frame: the three
     * rows then show the shape-morphing indicator instead of their cards.
     */
    private fun m3e(home: HomePage?): @Composable () -> Unit = {
        M3EHomeContent(
            home = home,
            localCollections = emptyList(),
            history = m3eHistory(),
            language = "en",
            onOpenAlbum = {},
            onOpenArtist = {},
            onOpenPlaylist = {},
            onOpenLocalPlaylist = {},
            onPlaySong = {},
        )
    }

    /**
     * The lead card's painted height, read across the card's own width from the
     * top of its cover band: a cover row is painted edge to edge, the title
     * under a Classic card covers only part of the width, and the m3e card's
     * scrim fades but is still painted. That tells the 150 dp Classic card from
     * the 180 dp m3e one without trusting either design's constants.
     */
    private fun leadPaintedHeight(img: BufferedImage): Int {
        val bg = img.getRGB(0, img.height - 1)
        val band = coverBands(img, bg).first()
        val lead = cards(img, band, bg).first()
        val width = lead.last - lead.first + 1
        var bottom = band.first
        for (y in band.first until img.height) {
            // The card's last rows fade to within a point of the page colour,
            // and the empty page is exactly the background: any difference is
            // the card, and the 8 px break below stops at the next shelf.
            val painted = lead.count { x -> img.getRGB(x, y) != bg }
            if (painted >= width * 3 / 4) bottom = y else if (y - bottom > 8) break
        }
        return bottom - band.first + 1
    }

    /**
     * The design switch at the level the window sees it: the same content draws
     * the Classic shelves, then its state flips to m3e and the next frame is the
     * Expressive Home, with no restart. The lead card says which style each frame
     * is (BitChord's 150 dp card paints 150 px, the m3e 180 dp card 180),
     * and the changed pixels say the switch really repainted instead of leaving
     * the old Home on screen.
     */
    private fun styleSwitch() {
        val style = mutableStateOf(DesignStyle.CLASSIC)
        val scene = scene(1364, 1100) {
            if (style.value == DesignStyle.M3E) m3e(m3ePage())() else home()()
        }
        try {
            pump(scene)
            File(outDir, "switch-classic-1364.png").writeBytes(encode(scene))
            val classic = ImageIO.read(File(outDir, "switch-classic-1364.png").inputStream())
            style.value = DesignStyle.M3E
            pump(scene, 4)
            File(outDir, "switch-m3e-1364.png").writeBytes(encode(scene))
            val switched = ImageIO.read(File(outDir, "switch-m3e-1364.png").inputStream())
            var changed = 0
            for (y in 0 until classic.height step 2) {
                for (x in 0 until classic.width step 2) {
                    if (classic.getRGB(x, y) != switched.getRGB(x, y)) changed++
                }
            }
            val classicHeight = leadPaintedHeight(classic)
            val m3eHeight = leadPaintedHeight(switched)
            println("switch: $changed sampled px changed, lead $classicHeight px -> $m3eHeight px")
            check(changed > 5_000) {
                "the design switch changed only $changed sampled px, so the Home did not redraw"
            }
            check(classicHeight in 144..156) {
                "the Classic frame's lead paints $classicHeight px, expected BitChord's 150 dp card"
            }
            check(m3eHeight in 168..184) {
                "the m3e frame's lead paints $m3eHeight px, expected the 180 dp card"
            }
        } finally {
            scene.close()
        }
    }

    /**
     * The move on a real store: a `device-sync.json` from the previous beta
     * carries `classic` and no generation, the first start moves it to m3e once,
     * and a Classic chosen afterwards is read back instead of being moved again.
     * It runs in a throwaway home directory, so the check never touches this
     * machine's options.
     */
    private fun designDefaultsMigrateOnce() {
        val home = File(".ignore/check-home").absoluteFile
        home.deleteRecursively()
        home.mkdirs()
        System.setProperty("user.home", home.path)
        val store = File(home, ".vivimusic/device-sync.json")
        store.parentFile.mkdirs()
        store.writeText("""{"homeDesign":"classic"}""")
        DesktopSettings.applyDesignDefaults()
        val moved = DesktopSettings.load()
        check(moved.homeDesign == DesignStyle.M3E.key) {
            "the previous beta's classic was not moved to m3e (read '${moved.homeDesign}')"
        }
        check(moved.designDefaultsVersion == 2) {
            "the move was not recorded (generation ${moved.designDefaultsVersion})"
        }
        DesktopSettings.update { it.copy(homeDesign = DesignStyle.CLASSIC.key) }
        DesktopSettings.applyDesignDefaults()
        val kept = DesktopSettings.load()
        check(kept.homeDesign == DesignStyle.CLASSIC.key) {
            "a Classic chosen after the move was overwritten (read '${kept.homeDesign}')"
        }
        // The store this machine carried before this build: generation 1 and the
        // previous Home. It moves once, so the rebuilt Home is the one shown.
        DesktopSettings.update {
            it.copy(homeDesign = DesignStyle.CLASSIC.key, designDefaultsVersion = 1)
        }
        DesktopSettings.applyDesignDefaults()
        val fromFirstGeneration = DesktopSettings.load()
        check(fromFirstGeneration.homeDesign == DesignStyle.M3E.key) {
            "a store still on the previous Home was not moved to m3e (read '${fromFirstGeneration.homeDesign}')"
        }
        check(fromFirstGeneration.designDefaultsVersion == 2) {
            "the second move was not recorded (generation ${fromFirstGeneration.designDefaultsVersion})"
        }
        println(
            "design defaults: the previous Home moves to m3e once (generations 1 and 0), a later Classic stays",
        )
    }

    @JvmStatic
    fun main(args: Array<String>) {
        designDefaultsMigrateOnce()
        // The window a new installation opens on: Home is the m3e style unless
        // the store says otherwise (see [designDefaultsMigrateOnce]).
        check(DesktopSyncState().homeDesign == DesignStyle.M3E.key) {
            "the shipped default for Home is '${DesktopSyncState().homeDesign}', expected m3e"
        }
        val wide = render(1364, 1560, "home-1364.png")
        val narrow = render(1024, 1560, "home-1024.png")
        bitChordShelf("home-1364", wide, pad = 24, cardDp = 150, gapDp = 14)
        bitChordShelf("home-1024", narrow, pad = 24, cardDp = 150, gapDp = 14)
        // Explore's New releases row: the screen's own composable, painted inside
        // the page's 16 dp inset so the row sees the 1332 dp viewport it does in
        // the app at 1364.
        val explore = render(1364, 900, "explore-1364.png", explore())
        shelf("explore-1364", explore, pad = 16, minShelves = 1, right = 16)
        // Home in the m3e style: the same multi-browse carousel, on the sketch's
        // 180 dp cards whose titles sit on the artwork.
        val m3eHome = render(1364, 1100, "m3e-home-1364.png", m3e(m3ePage()))
        shelf(
            "m3e-home-1364", m3eHome, pad = 16, minShelves = 3, right = 16,
            coverFill = null, roundedCards = true, expectIndicator = false,
        )
        // The whole category set: nine rows (the Library and BitChord's eight)
        // in one frame, so a row that went missing cannot hide below the fold.
        val m3eFull = render(1364, 2400, "m3e-home-full-1364.png", m3e(m3ePage()))
        m3eAllCategories(m3eFull)
        // The m3e card is a 180 dp rounded rectangle, so the lead's own centre
        // column spans the whole card: a card that went back to 308 would read
        // ~308, a 150 dp one ~150.
        val m3eBg = m3eHome.getRGB(0, m3eHome.height - 1)
        val m3eBand = coverBands(m3eHome, m3eBg).first()
        val m3eLead = cards(m3eHome, m3eBand, m3eBg).first()
        val m3eBox = card(m3eHome, m3eLead, m3eBand, m3eBg)
        val columnX = m3eBox.x0 + m3eBox.width / 2
        val middle = (m3eBand.first + m3eBand.last) / 2
        var top = middle
        while (top > 0 && m3eHome.getRGB(columnX, top - 1) != m3eBg) top--
        var bottom = middle
        while (bottom < m3eHome.height - 1 && m3eHome.getRGB(columnX, bottom + 1) != m3eBg) bottom++
        val paintedHeight = bottom - top + 1
        // The scrim's bottom edge blends into the page, so the painted box reads
        // about 175 of the card's 180 dp; a 308 dp card would read ~303.
        println("m3e-home-1364 lead: $paintedHeight px painted (the sketch's 180 dp card)")
        check(paintedHeight in 168..184) {
            "m3e: the lead card paints $paintedHeight px, expected the sketch's 180 dp card"
        }
        // The loading Home: the page is still in flight, so each row draws its
        // own shape-morphing indicator and the frame is not an empty window.
        val m3eLoading = render(1364, 900, "m3e-home-loading-1364.png", m3e(null))
        val loadingBg = m3eLoading.getRGB(0, m3eLoading.height - 1)
        var painted = 0
        for (y in 0 until m3eLoading.height step 2) {
            for (x in 0 until m3eLoading.width step 2) {
                if (m3eLoading.getRGB(x, y) != loadingBg) painted++
            }
        }
        println("m3e-home-loading-1364: $painted painted px (frames under .ignore/render-home/)")
        check(painted > 200) {
            "m3e: the loading Home painted $painted px, so the row loaders are missing"
        }
        paging()
        styleSwitch()
        println("homeRenderCheck: OK")
    }
}
