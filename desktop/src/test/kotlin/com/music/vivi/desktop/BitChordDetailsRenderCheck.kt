@file:OptIn(ExperimentalComposeUiApi::class)

package com.music.vivi.desktop

import androidx.compose.runtime.Composable
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.ImageComposeScene
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import com.music.innertube.models.Album
import com.music.innertube.models.AlbumItem
import com.music.innertube.models.Artist
import com.music.innertube.models.ArtistItem
import com.music.innertube.models.SongItem
import com.music.innertube.models.WatchEndpoint
import com.music.innertube.pages.ArtistSection
import org.jetbrains.skia.EncodedImageFormat
import java.awt.image.BufferedImage
import java.io.File
import javax.imageio.ImageIO

/**
 * Headless render of the two BitChord detail surfaces, run with
 *
 *     ./gradlew :desktop:detailSurfacesRenderCheck
 *
 * The live screens fetch, so the check hands [BitChordAlbumContent] and
 * [BitChordArtistContent] the same fixtures the service returns and reads the
 * pixels: the #202020 page, the 240 dp album sleeve, a bright title block, the
 * artist's 340 dp banner and the top-song table. A surface that compiled but
 * painted an empty page fails here.
 *
 * Frames land under `.ignore/render-details/`.
 */
object BitChordDetailsRenderCheck {

    private val outDir = File(".ignore/render-details")
    private const val WIDTH = 1364
    private const val HEIGHT = 900

    @JvmStatic
    fun main(args: Array<String>) {
        outDir.mkdirs()
        val cover = ensureCover()
        val songs = fixtureSongs(cover)
        val album = AlbumItem(
            browseId = "MPREb_bitChord_album",
            playlistId = "OLAK5uy_bitChord_album",
            title = "The Long Way Home",
            artists = listOf(Artist("Aurora Lane", "UC_bitChord_artist")),
            year = 2024,
            thumbnail = cover,
        )
        val artist = ArtistItem(
            id = "UC_bitChord_artist",
            title = "Aurora Lane",
            thumbnail = cover,
            shuffleEndpoint = WatchEndpoint(),
            radioEndpoint = WatchEndpoint(),
        )
        val sections = listOf(
            ArtistSection("Albums", listOf(album), null),
            ArtistSection("Singles", songs.take(4), null),
        )
        val queue = songs.take(5).mapIndexed { index, song ->
            NowPlaying(
                videoId = song.id,
                title = song.title,
                artist = song.artists.joinToString(", ") { it.name },
                thumbnail = song.thumbnail,
                durationMs = (song.duration ?: 0) * 1000L,
            )
        }

        val albumFrame = render("album-1364.png") {
            BitChordAlbumContent(
                album = album,
                songs = songs,
                language = "en",
                onOpenArtist = {},
                onPlaySong = {},
                onAddToQueue = {},
                onAddToPlaylist = {},
                onPlayAll = {},
                onShuffleAll = {},
                onDownloadAll = {},
                onShare = {},
                onRefresh = {},
            )
        }
        val artistFrame = render("artist-1364.png") {
            BitChordArtistContent(
                artist = artist,
                sections = sections,
                description = "Aurora Lane writes slow, wide songs about the road between two homes.",
                subscriberCountText = "1.2M subscribers",
                monthlyListenerCount = "840K monthly listeners",
                language = "en",
                onOpenAlbum = {},
                onOpenArtist = {},
                onOpenPlaylist = {},
                onPlaySong = {},
                onAddToQueue = {},
                onAddToPlaylist = {},
                onPlayAll = {},
                onShuffleAll = {},
            )
        }

        val queueFrame = render("queue-1364.png") {
            BitChordQueueScreen(
                queue = queue,
                index = 0,
                language = "en",
                onBack = {},
                onSkipTo = {},
                onRemoveAt = {},
                onClear = {},
                onReorder = {},
                onAddToPlaylist = {},
                onPlayNext = {},
                onAddToQueue = {},
            )
        }

        checkPage("album", albumFrame)
        checkPage("artist", artistFrame)
        checkPage("queue", queueFrame)

        // The album sleeve is the first painted block on the page.
        val page = BitChordColors.Page
        check(albumFrame.getRGB(100, 100) != page.toArgb()) {
            "album: the 240 dp sleeve is not painted at the page's top-left"
        }
        val titleInk = ink(albumFrame, 300, 1100, 30, 180, page)
        println("album title ink: $titleInk")
        check(titleInk > 400) { "album: the title block paints only $titleInk px" }
        val albumBands = textBands(albumFrame, 300, 1240, 320, 880, page)
        println("album row bands: $albumBands")
        check(albumBands >= 5) { "album: only $albumBands track row(s) painted" }

        // The artist banner is the coloured block across the top, and its name
        // sits at the foot of it in white.
        check(artistFrame.getRGB(600, 20) != page.toArgb()) {
            "artist: the banner is not painted"
        }
        val nameInk = ink(artistFrame, 24, 1000, 250, 335, page)
        println("artist name ink: $nameInk")
        check(nameInk > 300) { "artist: the name paints only $nameInk px" }
        val songBands = textBands(artistFrame, 80, 300, 380, 880, page)
        println("artist content bands: $songBands")
        check(songBands >= 4) { "artist: only $songBands content band(s) painted" }

        val queueTitle = ink(queueFrame, 24, 400, 40, 110, page)
        val queueBands = textBands(queueFrame, 80, 1200, 180, 880, page)
        println("queue title ink: $queueTitle, row bands: $queueBands")
        check(queueTitle > 300) { "queue: the title paints only $queueTitle px" }
        check(queueBands >= 5) { "queue: only $queueBands row band(s) painted" }

        println("OK: album, artist and queue surfaces painted on the BitChord page")
    }

    private fun checkPage(name: String, image: BufferedImage) {
        val page = BitChordColors.Page.toArgb()
        val corner = image.getRGB(image.width - 2, image.height - 2)
        check(diff(corner, page) <= 6) {
            "$name: the page corner is ${Integer.toHexString(corner)}, expected #202020"
        }
    }

    private fun scene(content: @Composable () -> Unit): ImageComposeScene =
        ImageComposeScene(WIDTH, HEIGHT) {
            AppTheme(mode = ThemeMode.DARK, accent = Color(0xFFED5564), language = "en") {
                content()
            }
        }

    private fun render(name: String, content: @Composable () -> Unit): BufferedImage {
        val scene = scene(content)
        val png = try {
            var clock = 0L
            repeat(6) {
                clock += 40_000_000L
                scene.render(clock)
                Thread.sleep(24)
            }
            scene.render(clock + 40_000_000L).encodeToData(EncodedImageFormat.PNG, 100)!!.bytes
        } finally {
            scene.close()
        }
        File(outDir, name).writeBytes(png)
        return ImageIO.read(png.inputStream())
    }

    private fun ink(image: BufferedImage, x0: Int, x1: Int, y0: Int, y1: Int, page: Color): Int {
        val bg = page.toArgb()
        var count = 0
        for (y in y0 until y1) {
            for (x in x0 until x1) {
                if (diff(image.getRGB(x, y), bg) > 24) count++
            }
        }
        return count
    }

    /** Rows of text: consecutive y runs with at least 12 painted pixels. */
    private fun textBands(image: BufferedImage, x0: Int, x1: Int, y0: Int, y1: Int, page: Color): Int {
        val bg = page.toArgb()
        var bands = 0
        var inside = false
        for (y in y0 until y1) {
            var row = 0
            for (x in x0 until x1) {
                if (diff(image.getRGB(x, y), bg) > 24) row++
            }
            if (row >= 12 && !inside) {
                bands++
                inside = true
            } else if (row < 12) {
                inside = false
            }
        }
        return bands
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

    private fun fixtureSongs(cover: String): List<SongItem> = (1..10).map { index ->
        SongItem(
            id = "video-$index",
            title = listOf(
                "The Long Way Home",
                "Paper Lanterns",
                "Northbound",
                "Half a Mile",
                "Sunday Static",
                "Blue Hour",
                "Every Open Door",
                "The Quiet Part",
                "Slow Tide",
                "Last Light",
            )[(index - 1) % 10],
            artists = listOf(Artist("Aurora Lane", "UC_bitChord_artist")),
            album = Album("The Long Way Home", "MPREb_bitChord_album"),
            duration = 180 + index * 11,
            thumbnail = cover,
        )
    }

    private fun ensureCover(): String {
        val file = File(outDir, "cover.png")
        if (file.isFile) return file.toURI().toString()
        val image = BufferedImage(600, 600, BufferedImage.TYPE_INT_RGB)
        val g = image.createGraphics()
        try {
            g.paint = java.awt.GradientPaint(
                0f, 0f, java.awt.Color(0x33507F),
                600f, 600f, java.awt.Color(0x142033),
            )
            g.fillRect(0, 0, 600, 600)
            g.color = java.awt.Color(0xF3E3C3)
            g.font = java.awt.Font("SansSerif", java.awt.Font.BOLD, 220)
            g.drawString("AL", 150, 360)
        } finally {
            g.dispose()
        }
        ImageIO.write(image, "png", file)
        return file.toURI().toString()
    }
}
