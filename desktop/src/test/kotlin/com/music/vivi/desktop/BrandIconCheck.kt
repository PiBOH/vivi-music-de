@file:OptIn(ExperimentalComposeUiApi::class)

package com.music.vivi.desktop

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
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
 * Headless check of the two brand marks the Integrations rows draw, run with
 *
 *     ./gradlew :desktop:brandIconCheck
 *
 * `BrandIcons.Discord` is the official Simple Icons path, and the path alone
 * says nothing about how Compose fills it: the Discord logo's two eyes are
 * separate subpaths INSIDE the head, so with the non-zero rule they are painted
 * in and the mark reads as a solid blob - which is exactly what was reported
 * ("il logo di Discord continua a non essere corretto"). The mark is cut with
 * the even-odd rule instead, and this check paints both marks at 200 px on white
 * and reads the eye centres back:
 *
 *  * the centre of each eye is background, because it is a hole;
 *  * the head around them carries ink, so the hole is not just an empty icon;
 *  * Last.fm's mark keeps its ink, so the rule did not eat the other one.
 *
 * The PNG is written to `desktop/build/reports/brand-icons.png` so the two marks
 * can be looked at.
 */
object BrandIconCheck {

    private const val SIZE = 200
    private const val CANVAS = 440

    private val outFile = File("build/reports/brand-icons.png")

    @JvmStatic
    fun main(args: Array<String>) {
        outFile.parentFile.mkdirs()
        val image = render()
        val failures = mutableListOf<String>()

        // The eye centres in the 24x24 viewBox, scaled to the drawn size.
        val leftEye = SIZE * 8.02f / 24f
        val rightEye = SIZE * 15.99f / 24f
        val eyeY = SIZE * 12.9f / 24f
        // The glyphs are laid out side by side, each in its own 220 px cell.
        val discordLeft = 10f
        val eyeLeft = isInk(image, discordLeft + leftEye, eyeY)
        val eyeRight = isInk(image, discordLeft + rightEye, eyeY)
        // The head around the eyes: just above the eyes is the top of the face.
        val head = isInk(image, discordLeft + SIZE * 0.5f, SIZE * 0.30f)
        // Last.fm's own mark, in the second cell.
        val lastFmInk = inkPixels(image, 230, 430, 10, 210)

        if (eyeLeft) failures += "the left eye of the Discord mark is filled in (non-zero rule?)"
        if (eyeRight) failures += "the right eye of the Discord mark is filled in (non-zero rule?)"
        if (!head) failures += "the Discord mark draws no head at all"
        if (lastFmInk < 1_000) failures += "the Last.fm mark draws almost nothing ($lastFmInk px)"

        println(
            "Discord: eyes ${if (eyeLeft) "filled" else "holes"}, head ${if (head) "inked" else "empty"}; " +
                "Last.fm ink $lastFmInk px",
        )
        if (failures.isEmpty()) {
            println("OK: the Discord eyes are holes and both marks draw")
        } else {
            failures.forEach { println("FAIL: $it") }
            kotlin.system.exitProcess(1)
        }
    }

    private fun render(): BufferedImage {
        val scene = ImageComposeScene(CANVAS, SIZE + 20) {
            Box(Modifier.fillMaxSize().background(Color.White), contentAlignment = Alignment.Center) {
                Row {
                    BrandCell(BrandIcons.Discord)
                    BrandCell(BrandIcons.LastFm)
                }
            }
        }
        val bytes = try {
            scene.render().encodeToData(EncodedImageFormat.PNG, 100)!!.bytes
        } finally {
            scene.close()
        }
        outFile.writeBytes(bytes)
        return javax.imageio.ImageIO.read(bytes.inputStream())
    }

    @Composable
    private fun BrandCell(mark: androidx.compose.ui.graphics.vector.ImageVector) {
        Box(Modifier.size((SIZE + 20).dp), contentAlignment = Alignment.Center) {
            Icon(
                imageVector = mark,
                contentDescription = null,
                tint = Color.Black,
                modifier = Modifier.size(SIZE.dp),
            )
        }
    }

    /** True when the pixel is darker than the white page it is drawn on. */
    private fun isInk(image: BufferedImage, x: Float, y: Float): Boolean {
        val rgb = image.getRGB(x.toInt().coerceIn(0, image.width - 1), y.toInt().coerceIn(0, image.height - 1))
        val luma = (((rgb shr 16) and 255) + ((rgb shr 8) and 255) + (rgb and 255)) / 3
        return luma < 200
    }

    private fun inkPixels(image: BufferedImage, x0: Int, x1: Int, y0: Int, y1: Int): Int {
        var count = 0
        for (y in y0 until y1) {
            for (x in x0 until x1) {
                if (isInk(image, x.toFloat(), y.toFloat())) count++
            }
        }
        return count
    }
}
