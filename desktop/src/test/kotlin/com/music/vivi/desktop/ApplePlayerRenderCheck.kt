@file:OptIn(ExperimentalComposeUiApi::class)

package com.music.vivi.desktop

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.ImageComposeScene
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import org.jetbrains.skia.EncodedImageFormat
import java.awt.image.BufferedImage
import java.io.File

/**
 * Headless check of the Apple player, run with
 *
 *     ./gradlew :desktop:applePlayerRenderCheck
 *
 * `AppleMusicPlayer` is four things at once - a backdrop taken from the cover, an
 * ink-only column of controls on the left, the lyrics on the right, and one close
 * control in the corner - and every one of them can be wrong while the module
 * still compiles: a screen that is the plain theme surface, white text on that
 * surface (which is what a track with no artwork produced before the scrim), a
 * lyrics column that never drew, or lyrics squeezed into a narrow window where
 * they cannot be read. No display is attached here, so the screen is painted into
 * an off-screen `ImageComposeScene` and the pixels are read back:
 *
 *  * the screen is dark - the cover is blurred and the app's own scrim sits over
 *    it, so the mean brightness is well under half the range. A render of the
 *    bare surface fails this, which is the point: this screen draws white ink;
 *  * a track WITH artwork paints its blurred backdrop over the whole window: all
 *    four corners carry the artwork's colour. Before this case existed the
 *    fixture had no cover on purpose, so the backdrop was never drawn by this
 *    check while the real screen painted one small square of it in the middle of
 *    the window;
 *  * the left column carries ink (the title, the artist, the two times);
 *  * the right half carries the lyrics, which are put in `LyricsCache` first so
 *    the check does not depend on a provider answering;
 *  * at 640 dp the lyrics are gone - the documented 720 dp threshold - and the
 *    screen is still dark and still has its column.
 *
 * PNGs are written under `.ignore/appleplayer/` so the layout can be looked at.
 */
object ApplePlayerRenderCheck {

    private const val WIDE = 1280
    private const val NARROW = 640
    private const val HEIGHT = 800
    private const val VIDEO_ID = "applePlayerRenderCheck"

    private val outDir = File(".ignore/appleplayer")

    /** Timed lines, so the sung one can be told from the ones waiting. */
    private val LYRICS = """
        [00:00.00] I keep the light on in the hall
        [00:04.00] waiting for a sound
        [00:08.00] the record turns without a word
        [00:12.00] and the room spins around
        [00:16.00] hold on, hold on
        [00:20.00] the night is long enough
        [00:24.00] hold on, hold on
        [00:28.00] we are not in a rush
        [00:32.00] every window, every street
        [00:36.00] carries what we said
        [00:40.00] and the morning comes in slow
        [00:44.00] with the song still in my head
    """.trimIndent()

    private fun track(thumbnail: String? = null) = NowPlaying(
        videoId = VIDEO_ID,
        title = "Murder Song (5, 4, 3, 2, 1)",
        artist = "AURORA",
        album = "All My Demons",
        durationMs = 213_000,
        // No artwork on purpose: that is the case the scrim exists for, and the
        // one a cover-less track really hits.
        thumbnail = thumbnail,
    )

    @JvmStatic
    fun main(args: Array<String>) {
        outDir.mkdirs()
        LyricsCache.put(VIDEO_ID, LYRICS, preferSynced = true)
        val failures = mutableListOf<String>()

        val wide = render(WIDE, "apple-player.png", track = track())
        val wideDark = meanLuminance(wide)
        val columnInk = inkPixels(wide, 0.02, 0.36, 0.05, 0.95)
        val lyricsInk = inkPixels(wide, 0.55, 0.98, 0.05, 0.95)
        println("1280x800: mean brightness %.2f, %d ink px in the column, %d in the lyrics".format(wideDark, columnInk, lyricsInk))

        if (wideDark > 0.55) {
            failures += "the screen is not dark (mean brightness %.2f): the backdrop and its scrim are missing, and the white ink would sit on the theme surface".format(wideDark)
        }
        if (columnInk < 200) {
            failures += "the left column drew almost no ink ($columnInk px): the title, the artist and the times are missing"
        }
        if (lyricsInk < 2000) {
            failures += "the lyrics half drew almost no ink ($lyricsInk px): the column is empty"
        }

        // A track with a cover: the blurred backdrop has to reach the four
        // corners of the window. It used to be drawn into a 256 dp layer that
        // clipped the scale meant to fill the screen, so the only painted part
        // was one small square in the middle - black everywhere else, which is
        // what a listener sees as "the background is not the one I set".
        val artwork = artworkFile()
        val covered = render(WIDE, "apple-player-artwork.png", track = track(artwork))
        val corners = listOf(
            "top-left" to cornerGreen(covered, 0, 0),
            "top-right" to cornerGreen(covered, 1, 0),
            "bottom-left" to cornerGreen(covered, 0, 1),
            "bottom-right" to cornerGreen(covered, 1, 1),
        )
        println("1280x800 with artwork: " + corners.joinToString { "%s %d".format(it.first, it.second) })
        // The scrim is darkest at the bottom (0.62 of black over the blur), so a
        // corner there is dim on purpose: what is asserted is that it is still
        // the artwork's colour rather than the surface's grey (0).
        corners.filter { it.second < 6 }.forEach { (where, green) ->
            failures += "the blurred backdrop does not reach the $where corner of the window (green - red = $green): the artwork is painted in one small square instead of behind the screen"
        }

        val narrow = render(NARROW, "apple-player-narrow.png", track = track())
        val narrowDark = meanLuminance(narrow)
        // Right of the centred column (which ends at 78 % of the window), so this
        // box can only hold lyrics.
        val narrowLyrics = inkPixels(narrow, 0.82, 0.99, 0.05, 0.95)
        println("640x800: mean brightness %.2f, %d ink px right of the column".format(narrowDark, narrowLyrics))
        if (narrowDark > 0.55) {
            failures += "the narrow window is not dark (mean brightness %.2f)".format(narrowDark)
        }
        if (narrowLyrics > 60) {
            failures += "the narrow window still draws the lyrics ($narrowLyrics px): under 720 dp they do not fit"
        }

        if (failures.isEmpty()) {
            println()
            println("OK: a darkened screen, an ink column on the left, the lyrics on the right, and none of them under 720 dp")
            return
        }
        println()
        println("FAILED:")
        failures.forEach { println("  - $it") }
        throw IllegalStateException("apple player render check failed (${failures.size} case(s))")
    }

    /**
     * A solid green square to play the part of the artwork: no asset has to live
     * in the repository for this, and one flat colour is what makes "did the
     * backdrop reach the corners" a question with an answer.
     */
    private fun artworkFile(): String {
        val file = File(outDir, "artwork.png")
        val image = BufferedImage(512, 512, BufferedImage.TYPE_INT_RGB)
        val green = java.awt.Color(0x2E, 0xC8, 0x4B)
        for (y in 0 until image.height) for (x in 0 until image.width) image.setRGB(x, y, green.rgb)
        javax.imageio.ImageIO.write(image, "png", file)
        // Three slashes on purpose: `File.toURI()` yields `file:/C:/...`, which
        // the image loader does not resolve on Windows, and a cover that silently
        // failed to load would make this case pass for the wrong reason.
        return "file:///" + file.absolutePath.replace('\\', '/')
    }

    private fun render(width: Int, name: String, track: NowPlaying): BufferedImage {
        val scene = ImageComposeScene(width, HEIGHT) {
            AppTheme(mode = ThemeMode.DARK, accent = Color(0xFFED5564), language = "en") {
                // The composition the app draws: the screen's own backdrop (the
                // style the user picked) and the Apple player over it. The player
                // stopped painting a second one of its own in 1.56.8.
                Box(Modifier.fillMaxSize()) {
                    PlayerBackground(
                        style = PlayerBackgroundStyle.APPLE_MUSIC,
                        bgUrl = track.thumbnail,
                        accent = Color(0xFFED5564),
                        modifier = Modifier.fillMaxSize(),
                        isPlaying = true,
                    )
                    AppleMusicPlayer(
                        np = track,
                        positionMs = 70_000,
                        durationMs = 213_000,
                        isPlaying = true,
                        volume = 0.7f,
                        isShuffle = false,
                        repeatMode = com.music.vivi.desktop.player.RepeatMode.OFF,
                        language = "en",
                        onBack = {},
                        onTogglePlay = {},
                        onNext = {},
                        onPrevious = {},
                        onSeek = {},
                        onVolume = {},
                        onToggleShuffle = {},
                        onCycleRepeat = {},
                        onOpenQueue = {},
                    )
                }
            }
        }
        // More than twice now that a cover has to be decoded and blurred before
        // the frame is worth reading: the lyrics arrive in a `LaunchedEffect` and
        // the image loader answers on its own threads.
        val png = try {
            repeat(4) {
                scene.render()
                Thread.sleep(120)
            }
            scene.render().encodeToData(EncodedImageFormat.PNG, 100)!!.bytes
        } finally {
            scene.close()
        }
        File(outDir, name).writeBytes(png)
        return javax.imageio.ImageIO.read(png.inputStream())
    }

    /**
     * Mean `green - red` over a 40 px box in one corner of the render, 0..255.
     * The backdrop is a green artwork blurred and then darkened by the screen's
     * scrim, so it stays clearly greener than red wherever it is painted; the
     * theme surface and a missing backdrop leave the box grey (or black) and read
     * zero here.
     */
    private fun cornerGreen(image: BufferedImage, right: Int, bottom: Int): Int {
        val box = 40
        val x0 = if (right == 0) 0 else image.width - box
        val y0 = if (bottom == 0) 0 else image.height - box
        var green = 0.0
        var red = 0.0
        var count = 0
        for (y in y0 until (y0 + box)) {
            for (x in x0 until (x0 + box)) {
                val rgb = image.getRGB(x, y)
                red += (rgb shr 16) and 255
                green += (rgb shr 8) and 255
                count++
            }
        }
        return ((green - red) / count).toInt()
    }

    /** Mean brightness of the render, 0..1. */
    private fun meanLuminance(image: BufferedImage): Double {
        var total = 0.0
        for (y in 0 until image.height) {
            for (x in 0 until image.width) {
                val rgb = image.getRGB(x, y)
                total += (((rgb shr 16) and 255) + ((rgb shr 8) and 255) + (rgb and 255)) / (3.0 * 255)
            }
        }
        return total / (image.width.toLong() * image.height)
    }

    /**
     * Pixels of ink in a box: anything clearly brighter than the backdrop. The
     * threshold is low on purpose - the lines away from the sung one are drawn at
     * 40 % and blurred, and near-white would count only the active line.
     */
    private fun inkPixels(
        image: BufferedImage,
        fromX: Double,
        toX: Double,
        fromY: Double,
        toY: Double,
    ): Int {
        var count = 0
        val x0 = (fromX * image.width).toInt()
        val x1 = (toX * image.width).toInt().coerceAtMost(image.width)
        val y0 = (fromY * image.height).toInt()
        val y1 = (toY * image.height).toInt().coerceAtMost(image.height)
        for (y in y0 until y1) {
            for (x in x0 until x1) {
                val rgb = image.getRGB(x, y)
                val r = (rgb shr 16) and 255
                val g = (rgb shr 8) and 255
                val b = rgb and 255
                if ((r + g + b) / (3.0 * 255) >= 0.30) count++
            }
        }
        return count
    }
}
