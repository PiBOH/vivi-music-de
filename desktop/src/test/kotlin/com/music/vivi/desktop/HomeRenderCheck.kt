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
 * code, because a shelf that silently went back to equal squares, to a rounded
 * square cover or to a row without arrows would still compile and still draw
 * something:
 *
 *  - every cover is the project's 4-sided cookie: its mask fills about 0.82 of
 *    the card's box, where a rounded square fills 0.99;
 *  - a shelf is the reference video's multi-browse row: a 245 dp leading card,
 *    162 dp cards after it and a trailing card the window cuts to 0.43-0.46 of
 *    a card, which is what the video's own frames measure;
 *  - a scrollable shelf carries the Expressive indicator under it;
 *  - the hover arrow exists only while the pointer is on the row, at the row's
 *    trailing edge, and pressing it pages the shelf onto a new leading card,
 *    which the indicator's pill says by moving to the next page's dot.
 *
 * Frames land under `desktop/.ignore/render-home/`: `home-1364.png` and
 * `home-1024.png` at rest, `home-1364-hover.png` with the arrows,
 * `home-1364-paged.png` after the press and `explore-1364.png` for Explore's
 * New releases row, which is the screen's own row composable.
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
     * none. [minShelves] is how many cover rows the frame must paint.
     */
    private fun shelf(
        name: String,
        img: BufferedImage,
        pad: Int,
        minShelves: Int = 2,
        right: Int = 0,
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
            // A rounded square fills about 0.99 of its box, so the cover stops
            // being a cookie well before this bound; the measured cookie reads
            // 0.82 because the box is the painted outline and its tapered tips
            // fall outside the band the rows are found in.
            check(box.coverage in 0.55..0.90) {
                "$name: card $index fills %.2f of its box; a cookie fills about 0.82 and a rounded square 0.99"
                    .format(box.coverage)
            }
        }
        val pill = indicatorRun(img, band, bg)
        println("%-12s indicator: %d px pill under the shelf".format(name, pill))
        check(pill >= 14) {
            "$name: no pill under the shelf ($pill px), so the carousel indicator is gone"
        }
    }

    /** The arrows exist on hover only, and the forward one pages the row. */
    private fun paging() {
        val width = 1364
        val height = 1560
        val scene = scene(width, height, home())
        try {
            pump(scene)
            val rest = ImageIO.read(File(outDir, "home-1364.png").inputStream())
            val bg = rest.getRGB(0, rest.height - 1)
            val band = coverBands(rest, bg).first()
            val rowMiddle = (band.first + band.last) / 2f
            val restPill = indicatorPill(rest, band, bg)

            val resting = encode(scene)
            scene.sendPointerEvent(PointerEventType.Move, Offset(400f, rowMiddle))
            pump(scene, 4)
            val hovered = encode(scene)
            outDir.mkdirs()
            File(outDir, "home-1364-hover.png").writeBytes(hovered)
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
            File(outDir, "home-1364-paged.png").writeBytes(paged)
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

    @JvmStatic
    fun main(args: Array<String>) {
        val wide = render(1364, 1560, "home-1364.png")
        val narrow = render(1024, 1560, "home-1024.png")
        shelf("home-1364", wide, pad = 24, minShelves = 3)
        shelf("home-1024", narrow, pad = 24, minShelves = 3)
        // Explore's New releases row: the screen's own composable, painted inside
        // the page's 16 dp inset so the row sees the 1332 dp viewport it does in
        // the app at 1364.
        val explore = render(1364, 900, "explore-1364.png", explore())
        shelf("explore-1364", explore, pad = 16, minShelves = 1, right = 16)
        paging()
        println("homeRenderCheck: OK")
    }
}
