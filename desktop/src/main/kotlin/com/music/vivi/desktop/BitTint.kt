package com.music.vivi.desktop

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.Color
import java.awt.image.BufferedImage
import java.net.URI
import javax.imageio.ImageIO
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * The dominant colour of a cover, for the Library's listening cards.
 *
 * The reference tints each card from the artwork it shows: the measured card of
 * the first shelf runs from rgb(72, 15, 65) at the top to rgb(45, 12, 42) at the
 * bottom, a dark version of the cover's own hue. The cover is reduced to a small
 * grid, the cells are averaged and the result is pushed to a usable saturation
 * and brightness, so a grey cover yields a grey card and a red one a red card.
 */
internal object BitTint {
    private val cache = HashMap<String, Color?>()

    fun cached(url: String?): Color? = if (url.isNullOrBlank()) null else cache[url]

    suspend fun of(url: String?): Color? {
        if (url.isNullOrBlank()) return null
        cache[url]?.let { return it }
        if (cache.containsKey(url)) return null
        val tint = withContext(Dispatchers.IO) {
            runCatching {
                ImageIO.read(URI(url).toURL()).let { image ->
                    if (image == null) null else colourOf(image)
                }
            }.getOrNull()
        }
        cache[url] = tint
        return tint
    }

    private fun colourOf(image: BufferedImage): Color {
        val steps = 12
        var r = 0.0
        var g = 0.0
        var b = 0.0
        var weight = 0.0
        for (ix in 0 until steps) {
            for (iy in 0 until steps) {
                val x = (ix + 0.5) * image.width / steps
                val y = (iy + 0.5) * image.height / steps
                val rgb = image.getRGB(x.toInt().coerceIn(0, image.width - 1), y.toInt().coerceIn(0, image.height - 1))
                val cr = (rgb shr 16 and 0xFF) / 255.0
                val cg = (rgb shr 8 and 0xFF) / 255.0
                val cb = (rgb and 0xFF) / 255.0
                // Very dark pixels are mostly shadow and would grey the result out.
                val luma = 0.2126 * cr + 0.7152 * cg + 0.0722 * cb
                val w = 0.15 + luma
                r += cr * w
                g += cg * w
                b += cb * w
                weight += w
            }
        }
        if (weight == 0.0) return Color(0xFF3A3A3D)
        val hsb = java.awt.Color(
            ((r / weight) * 255).toInt().coerceIn(0, 255),
            ((g / weight) * 255).toInt().coerceIn(0, 255),
            ((b / weight) * 255).toInt().coerceIn(0, 255),
        ).let { java.awt.Color.RGBtoHSB(it.red, it.green, it.blue, null) }
        val hue = hsb[0]
        val saturation = hsb[1].coerceAtLeast(0.35f)
        val rgb = java.awt.Color.HSBtoRGB(hue, saturation, 0.28f)
        return Color(0xFF000000.toInt() or (rgb and 0xFFFFFF))
    }
}

/** The card tint for [url], recomputed only when the url changes. */
@Composable
internal fun rememberArtworkTint(url: String?): Color? {
    var tint by remember(url) { mutableStateOf(BitTint.cached(url)) }
    LaunchedEffect(url) {
        if (tint == null) tint = BitTint.of(url)
    }
    return tint
}
