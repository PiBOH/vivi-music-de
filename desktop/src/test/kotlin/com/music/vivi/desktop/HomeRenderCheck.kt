@file:OptIn(ExperimentalComposeUiApi::class)

package com.music.vivi.desktop

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.ImageComposeScene
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.PointerEventType
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
 * It writes `home-1364.png`, `home-1024.png` and `home-1364-hover.png` under
 * `desktop/.ignore/render-home/` and reads them back. The live Home needs the
 * network, so the screen itself cannot be a render case; [HomeShelfStack] takes
 * the page, and this check hands it a fixed one with the same album cards the
 * service returns.
 *
 * What is asserted is the shelf geometry the reference fixed: the 24 dp gutter,
 * the 150 dp card, the 14 dp gap, the last card cut by the window edge instead
 * of fitting, and the 12 dp corner on the artwork, which is the shape the
 * reference draws and the one departure from the cookie map. A shelf that
 * silently went back to the cookie, to a hero card or to full-width rows would
 * still compile and still draw something, so the numbers are read off the
 * painted pixels instead of trusted.
 *
 * The arrows are part of it. They are drawn only while the pointer is on the
 * row, because a permanent circle sitting on the card the edge cuts in half
 * reads as a defect over the cover; this check moves a real pointer into the row
 * of a scene and proves the circles appear, which a screenshot of an unfocused
 * window cannot do.
 */
object HomeRenderCheck {

    private val outDir = File(".ignore/render-home")

    /** Home's shelf headings and the card rows the geometry is read from. */
    private fun page(): HomePage = HomePage(
        chips = null,
        sections = listOf(
            section("Album per te", 12),
            section("Voglia di ballare", 12),
            section("Cover e remix", 12),
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

    private fun scene(width: Int, height: Int): ImageComposeScene =
        ImageComposeScene(width, height) {
            AppTheme(mode = ThemeMode.DARK, accent = Color(0xFFED5564), language = "en") {
                home()()
            }
        }

    private fun settle(scene: ImageComposeScene): ByteArray {
        repeat(4) {
            scene.render()
            Thread.sleep(100)
        }
        return scene.render().encodeToData(EncodedImageFormat.PNG, 100)!!.bytes
    }

    private fun render(width: Int, height: Int, name: String): BufferedImage {
        val scene = scene(width, height)
        val png = try {
            settle(scene)
        } finally {
            scene.close()
        }
        outDir.mkdirs()
        File(outDir, name).writeBytes(png)
        return ImageIO.read(png.inputStream())
    }

    /**
     * The same shelf at rest and with the pointer inside the first row: the
     * arrows must exist in the second frame and not in the first.
     */
    private fun hoverFrame(): BufferedImage {
        val width = 1364
        val height = 905
        val scene = scene(width, height)
        try {
            val rest = settle(scene)
            scene.sendPointerEvent(PointerEventType.Move, Offset(400f, 160f))
            val hovered = settle(scene)
            outDir.mkdirs()
            File(outDir, "home-1364-hover.png").writeBytes(hovered)
            val a = ImageIO.read(rest.inputStream())
            val b = ImageIO.read(hovered.inputStream())
            var differing = 0
            for (y in 0 until height) {
                for (x in 0 until width) {
                    if (a.getRGB(x, y) != b.getRGB(x, y)) differing++
                }
            }
            println("hover frame: $differing px differ from the resting one")
            check(differing > 300) {
                "the row's arrows did not appear while the pointer was on the row ($differing px changed)"
            }
            return b
        } finally {
            scene.close()
        }
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
            val painted = band.count { y -> diff(img.getRGB(x, y), bg) > 14 }
            val isCard = painted >= band.count() * 0.6
            if (isCard && start < 0) start = x
            if (!isCard && start >= 0) {
                if (x - start >= 40) out += start until x
                start = -1
            }
        }
        if (start >= 0) out += start until img.width
        return out
    }

    private fun check(condition: Boolean, message: String) {
        if (!condition) throw IllegalStateException(message)
    }

    @JvmStatic
    fun main(args: Array<String>) {
        val wide = render(1364, 905, "home-1364.png")
        val narrow = render(1024, 700, "home-1024.png")
        hoverFrame()

        for ((name, img) in listOf("home-1364" to wide, "home-1024" to narrow)) {
            val bg = img.getRGB(0, img.height - 1)
            val bands = coverBands(img, bg)
            check(bands.size >= 3) {
                "$name: expected three card rows, painted ${bands.size} cover band(s)"
            }
            val row = bands[0]
            val rowCards = cards(img, row, bg)
            check(rowCards.size >= 4) { "$name: first shelf painted ${rowCards.size} card(s)" }
            val left = rowCards[0].first
            val width = rowCards[0].count()
            val gap = if (rowCards.size > 1) rowCards[1].first - rowCards[0].last - 1 else -1
            val lastCut = rowCards.last().last >= img.width - 1
            println(
                "%-12s row %d..%d, first card at %d, %d px wide, gap %d, %d cards, last cut: %s".format(
                    name, row.first, row.last, left, width, gap, rowCards.size, lastCut,
                ),
            )
            check(kotlin.math.abs(left - 24) <= 1) {
                "$name: the shelf gutter is $left px, expected the 24 dp 24"
            }
            check(kotlin.math.abs(width - 150) <= 1) {
                "$name: a card is $width px wide, expected the 150 dp 150"
            }
            check(kotlin.math.abs(gap - 14) <= 1) {
                "$name: the card gap is $gap px, expected the 14 dp 14"
            }
            check(lastCut) {
                "$name: the last card fits inside the window; the reference cuts it at the edge"
            }
            // The artwork is a rounded square, not a cookie: its top-left corner
            // is page, its left edge 12 px lower is cover.
            check(diff(img.getRGB(left + 1, row.first + 1), bg) <= 14) {
                "$name: the card's corner is painted, so the 12 dp corner is gone"
            }
            check(diff(img.getRGB(left + 1, row.first + 12), bg) > 14) {
                "$name: the card's left edge is empty 12 px down, so it is not the reference's square"
            }
        }
        println("homeRenderCheck: OK")
    }
}
