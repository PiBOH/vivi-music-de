package com.music.vivi.desktop

import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.graphics.Color
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.net.HttpURLConnection
import java.net.URL
import java.util.concurrent.ConcurrentHashMap
import javax.imageio.ImageIO

/**
 * The accent the app's own colours come from: the dominant colour of the
 * artwork of the track that is playing.
 *
 * The theme used to be built from an accent the user picked on the Theme &
 * Colors screen. The accent is no longer a choice: every surface follows the
 * cover on screen, so a red sleeve gives a red interface and the next track
 * re-colours it with the same tonal engine `AppTheme` already ran (TonalSpot,
 * SPEC 2025), nothing else about the scheme changes.
 *
 * The colour is read once per artwork URL and kept in memory: the player asks
 * on every track, and a decoded 64 px thumbnail is cheap but not free. A cover
 * that cannot be read leaves the last colour in place instead of flashing the
 * fallback, and the fallback is the app's own coral so a machine that has never
 * played anything still has a scheme.
 */
object ArtworkAccent {

    /** The coral the desktop shipped before the accent became the cover's own. */
    val fallback: Color = Color(0xFFED5564)

    /** The colour in use; null until a cover has been read. */
    val current = mutableStateOf<Color?>(null)

    private val cache = ConcurrentHashMap<String, Color>()

    /** Reads the cover's accent, or clears it when nothing is playing. */
    suspend fun update(url: String?) {
        val key = url?.takeIf { it.isNotBlank() } ?: run {
            current.value = null
            return
        }
        cache[key]?.let {
            current.value = it
            return
        }
        val color = withContext(Dispatchers.IO) { runCatching { dominantColor(key) }.getOrNull() } ?: return
        cache[key] = color
        current.value = color
    }

    /**
     * The most present saturated colour of the image, weighted by how vivid
     * each pixel is so a large area of near-grey fabric does not beat a small
     * bright logo. Pixels that cannot carry an interface colour (near black,
     * near white, grey) are ignored; a cover made only of those falls back to
     * its own mean, lightened enough to seed a scheme.
     */
    private fun dominantColor(url: String): Color {
        val connection = (URL(url).openConnection() as HttpURLConnection).apply {
            connectTimeout = 4_000
            readTimeout = 4_000
            instanceFollowRedirects = true
            setRequestProperty("User-Agent", "VIVI Music DE")
        }
        try {
            val source = connection.inputStream.use { ImageIO.read(it) } ?: error("the cover is not an image")
            val width = 64
            val height = (source.height.toFloat() * width / source.width).toInt().coerceAtLeast(1)
            val scaled = java.awt.image.BufferedImage(width, height, java.awt.image.BufferedImage.TYPE_INT_RGB)
            scaled.createGraphics().apply {
                drawImage(source.getScaledInstance(width, height, java.awt.Image.SCALE_SMOOTH), 0, 0, null)
                dispose()
            }
            val buckets = Array(24) { DoubleArray(4) } // weight, r, g, b
            var meanR = 0.0
            var meanG = 0.0
            var meanB = 0.0
            var meanCount = 0
            for (y in 0 until height) {
                for (x in 0 until width) {
                    val rgb = scaled.getRGB(x, y)
                    val r = (rgb shr 16) and 0xFF
                    val g = (rgb shr 8) and 0xFF
                    val b = rgb and 0xFF
                    val max = maxOf(r, g, b)
                    val min = minOf(r, g, b)
                    if (max < 48 || max - min < 20) continue
                    val weight = (max - min).toDouble() * max / 255.0
                    val hue = hueDegrees(r, g, b)
                    val bucket = buckets[((hue / 15f).toInt()) % 24]
                    bucket[0] += weight
                    bucket[1] += r * weight
                    bucket[2] += g * weight
                    bucket[3] += b * weight
                    meanR += r
                    meanG += g
                    meanB += b
                    meanCount++
                }
            }
            val best = buckets.maxByOrNull { it[0] }
            val color = if (best != null && best[0] > 0.0) {
                Color(
                    (best[1] / best[0] / 255.0).toFloat(),
                    (best[2] / best[0] / 255.0).toFloat(),
                    (best[3] / best[0] / 255.0).toFloat(),
                )
            } else if (meanCount > 0) {
                Color(
                    (meanR / meanCount / 255.0).toFloat(),
                    (meanG / meanCount / 255.0).toFloat(),
                    (meanB / meanCount / 255.0).toFloat(),
                )
            } else {
                fallback
            }
            return readable(color)
        } finally {
            connection.disconnect()
        }
    }

    /** Raises a colour that is too dark or too pale to seed a readable scheme. */
    private fun readable(color: Color): Color {
        val max = maxOf(color.red, color.green, color.blue)
        val min = minOf(color.red, color.green, color.blue)
        return when {
            max < 0.28f -> color.copy(
                red = (color.red + 0.18f).coerceAtMost(1f),
                green = (color.green + 0.18f).coerceAtMost(1f),
                blue = (color.blue + 0.18f).coerceAtMost(1f),
            )
            min > 0.92f -> color.copy(red = color.red * 0.82f, green = color.green * 0.82f, blue = color.blue * 0.82f)
            else -> color
        }
    }

    private fun hueDegrees(r: Int, g: Int, b: Int): Float {
        val rf = r / 255f
        val gf = g / 255f
        val bf = b / 255f
        val max = maxOf(rf, gf, bf)
        val min = minOf(rf, gf, bf)
        val delta = max - min
        if (delta == 0f) return 0f
        val hue = when (max) {
            rf -> 60f * (((gf - bf) / delta) % 6f)
            gf -> 60f * (((bf - rf) / delta) + 2f)
            else -> 60f * (((rf - gf) / delta) + 4f)
        }
        return (hue + 360f) % 360f
    }
}
