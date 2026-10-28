@file:OptIn(ExperimentalComposeUiApi::class)

package com.music.vivi.desktop

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.ImageComposeScene
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import org.jetbrains.skia.EncodedImageFormat
import java.awt.image.BufferedImage
import java.io.File

/**
 * Headless check of the Material 3 Expressive shapes, run with
 *
 *     ./gradlew :desktop:cookieShapesCheck
 *
 * `CookieShapes` are two rounded polygons and a clover, drawn from arithmetic:
 * nothing stops them compiling while drawing a diamond, or nothing at all, and
 * the four screens that use them (`SongRow`, `YtItemCard` for an artist, the
 * Contributors list, `LtAvatar`) would only show it to a person looking at the
 * window. Each shape is therefore painted here into an off-screen
 * `ImageComposeScene` and probed at the points that tell the three apart:
 *
 *  * the centre is painted and the four corners are not, for all three (a
 *    clover or a cookie leaves the box's corners empty, a plain square does not);
 *  * the 4-sided cookie has a *nearly flat top*: its upper edge is a long line
 *    from one corner to the next, so the middle of the top is painted just under
 *    it and bare above it, which a diamond (the same square turned 45 degrees)
 *    would fail;
 *  * the 9-sided cookie has an *apex*: the middle of its top is painted close to
 *    the edge, where the 4-sided one is bare;
 *  * the clover has a lobe at the middle of each side, so all four midpoints are
 *    painted and the diagonals between them are not.
 *
 * PNGs of each render are written under `.ignore/cookies/`.
 */
object CookieShapesCheck {

    private const val SIZE = 240

    private val outDir = File(".ignore/cookies")

    @JvmStatic
    fun main(args: Array<String>) {
        outDir.mkdirs()
        val failures = mutableListOf<String>()

        val cookie4 = render("cookie4", CookieShapes.fourSided)
        val cookie9 = render("cookie9", CookieShapes.nineSided)
        val clover = render("clover", CookieShapes.clover)

        for ((name, image) in listOf("cookie4" to cookie4, "cookie9" to cookie9, "clover" to clover)) {
            if (!image.filled(0.5, 0.5)) failures += "$name: the middle of the shape is not painted"
            for ((x, y) in CORNERS) {
                if (image.filled(x, y)) failures += "$name: the box corner at ($x, $y) is painted, so the shape fills its box"
            }
        }

        // The 4-sided cookie: a long top edge just under the top of the box.
        if (!cookie4.filled(0.5, 0.12)) failures += "cookie4: the middle of its top edge is not painted (no flat top)"
        if (cookie4.filled(0.5, 0.01)) failures += "cookie4: it is painted at the very top of the box, which a tilted square is not"

        // The 9-sided cookie: an apex at the middle of the top.
        if (!cookie9.filled(0.5, 0.06)) failures += "cookie9: no apex at the middle of its top"
        if (cookie9.filled(0.02, 0.02)) failures += "cookie9: it is painted in the box's corner"

        // The clover: a lobe on every side, nothing between them.
        for ((x, y) in LOBES) {
            if (!clover.filled(x, y)) failures += "clover: the lobe at ($x, $y) is not painted"
        }
        for ((x, y) in DIAGONALS) {
            if (clover.filled(x, y)) failures += "clover: the gap at ($x, $y) between two lobes is painted"
        }

        // The three are three shapes, not one drawn three times.
        if (differing(cookie4, cookie9) <= 0) failures += "the 4-sided and the 9-sided cookie draw the same pixels"
        if (differing(cookie9, clover) <= 0) failures += "the 9-sided cookie and the clover draw the same pixels"

        println("colours: cookie4=${painted(cookie4)}, cookie9=${painted(cookie9)}, clover=${painted(clover)}")

        if (failures.isEmpty()) {
            println()
            println("OK: a 4-sided cookie with a flat top, a 9-sided cookie with an apex and a clover with four lobes")
            return
        }
        println()
        println("FAILED:")
        failures.forEach { println("  - $it") }
        throw IllegalStateException("cookie shapes check failed (${failures.size} case(s))")
    }

    private val CORNERS = listOf(0.03 to 0.03, 0.97 to 0.03, 0.03 to 0.97, 0.97 to 0.97)
    private val LOBES = listOf(0.5 to 0.1, 0.9 to 0.5, 0.5 to 0.9, 0.1 to 0.5)
    private val DIAGONALS = listOf(0.1 to 0.1, 0.9 to 0.1, 0.1 to 0.9, 0.9 to 0.9)

    /** One shape painted on the page's own colour, so "painted" is "not the page". */
    private fun render(name: String, shape: Shape): BufferedImage {
        val scene = ImageComposeScene(SIZE, SIZE) {
            AppTheme(mode = ThemeMode.LIGHT, accent = Color(0xFFED5564)) {
                Box(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.surface)) {
                    Box(
                        Modifier
                            .fillMaxSize()
                            .clip(shape)
                            .background(MaterialTheme.colorScheme.primary),
                    )
                }
            }
        }
        val png = try {
            scene.render().encodeToData(EncodedImageFormat.PNG, 100)!!.bytes
        } finally {
            scene.close()
        }
        File(outDir, "$name.png").writeBytes(png)
        return javax.imageio.ImageIO.read(png.inputStream())
    }

    /** Is the point at ([x], [y]) of the box painted, or is it still the page? */
    private fun BufferedImage.filled(x: Double, y: Double): Boolean {
        val page = getRGB(0, height - 1)
        val px = (x * (width - 1)).toInt()
        val py = (y * (height - 1)).toInt()
        val pixel = getRGB(px, py)
        val dr = kotlin.math.abs(((pixel shr 16) and 255) - ((page shr 16) and 255))
        val dg = kotlin.math.abs(((pixel shr 8) and 255) - ((page shr 8) and 255))
        val db = kotlin.math.abs((pixel and 255) - (page and 255))
        return dr + dg + db > 12
    }

    private fun painted(image: BufferedImage): Int {
        var count = 0
        for (y in 0 until image.height) {
            for (x in 0 until image.width) {
                if (image.filled(x.toDouble() / (image.width - 1), y.toDouble() / (image.height - 1))) count++
            }
        }
        return count
    }

    private fun differing(a: BufferedImage, b: BufferedImage): Int {
        var count = 0
        for (y in 0 until a.height) {
            for (x in 0 until a.width) {
                if (a.getRGB(x, y) != b.getRGB(x, y)) count++
            }
        }
        return count
    }
}
