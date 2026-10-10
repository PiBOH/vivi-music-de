@file:OptIn(ExperimentalComposeUiApi::class)

package com.music.vivi.desktop

import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.ImageComposeScene
import androidx.compose.ui.graphics.Color
import org.jetbrains.skia.EncodedImageFormat
import java.io.File

/**
 * Headless check of the custom colour wheel, run with
 *
 *     ./gradlew :desktop:colorWheelRenderCheck
 *
 * The picker is one round wheel on the right of the Theme & Colors screen (hue
 * around the rim, saturation from the centre out) with the brightness strip
 * beside it, in place of the three HSV bars it used to be. This renders the real
 * `ThemeSection` into an off-screen `ImageComposeScene` (no window, no GPU) with
 * the picker open and reads the pixels back, because a wheel is a picture and
 * nothing else can tell a wheel from a rectangle:
 *
 *  * the wheel is there, and it is round: the largest chromatic region must be a
 *    disc of the wheel's own diameter, not a wide bar;
 *  * the hue runs around the rim: the four cardinal samples of that disc must be
 *    red at the right, green-ish at the bottom, cyan at the left and blue at the
 *    top, which is the sweep gradient plus the clockwise angle the pointer uses;
 *  * the saturation runs from the centre: the middle of the disc must be white
 *    (saturation 0) while the rim is fully saturated;
 *  * the marker follows the picked colour: with a red accent it sits at the
 *    right of the disc and with a green one at its lower-left, so the accent the
 *    screen opens on is the point the wheel draws;
 *  * the brightness strip is beside the wheel: full colour at its top, half way
 *    down in the middle, black at the bottom;
 *  * no horizontal HSV bar is left on the screen (a wide, thin chromatic band).
 *
 * PNGs of both renders are written under `.ignore/colorwheel/`.
 */
object ColorWheelRenderCheck {

    private const val WIDTH = 1000
    private const val HEIGHT = 1400

    /** The wheel's diameter in dp, i.e. what it must measure at density 1. */
    private const val WHEEL_DP = 220

    private val outDir = File(".ignore/colorwheel")

    private val red = Color(0xFFFF0000)
    private val green = Color(0xFF00FF00)

    @JvmStatic
    fun main(args: Array<String>) {
        outDir.mkdirs()
        val failures = mutableListOf<String>()

        val greenShot = render(green, "green")
        val redShot = render(red, "red")

        // The wheel is the round chromatic region: the screen also draws the
        // accent preview card and the palette, so the disc is picked as the
        // squarest of the large regions rather than the biggest of them.
        val regions = blobs(greenShot)
        println("chromatic regions (largest first): " + regions.take(5).joinToString { "${it.width}x${it.height}" })
        val wheel = regions
            .filter { it.width >= 150 && it.height >= 150 }
            .minByOrNull { kotlin.math.abs(it.width.toFloat() / it.height.toFloat() - 1f) }
        if (wheel == null) {
            println("FAILED: no large chromatic region was drawn at all")
            println("  the picker did not render")
            throw IllegalStateException("colour wheel render check failed")
        }
        println("wheel blob: ${wheel.width}x${wheel.height}px, ${wheel.size}px of ink, at ${wheel.x1},${wheel.y1}")

        // 1. It is a disc of the wheel's own size, not a bar or a card.
        val sidePx = maxOf(wheel.width, wheel.height)
        if (wheel.width < 190 || wheel.height < 190 || sidePx > WHEEL_DP * 1.3) {
            failures += "the wheel is ${wheel.width}x${wheel.height}px, expected a disc of about ${WHEEL_DP}px"
        }
        if (wheel.width.toFloat() / wheel.height.toFloat() !in 0.88f..1.14f) {
            failures += "the wheel is not round: ${wheel.width}x${wheel.height}px"
        }

        // 2. The hue runs around the rim, clockwise from 3 o'clock.
        val cx = (wheel.x1 + wheel.x2) / 2
        val cy = (wheel.y1 + wheel.y2) / 2
        val radius = sidePx / 2f
        val at = 0.8f * radius
        val right = sample(greenShot, (cx + at).toInt(), cy)
        val bottom = sample(greenShot, cx, (cy + at).toInt())
        val left = sample(greenShot, (cx - at).toInt(), cy)
        val top = sample(greenShot, cx, (cy - at).toInt())
        println("rim: right=${fmt(right)} bottom=${fmt(bottom)} left=${fmt(left)} top=${fmt(top)}")
        if (!(right[0] > 200 && right[1] < 100 && right[2] < 100)) {
            failures += "the right of the rim is ${fmt(right)}, expected red (hue 0 at 3 o'clock)"
        }
        if (!(bottom[1] > 200 && bottom[0] < 200 && bottom[2] < 120)) {
            failures += "the bottom of the rim is ${fmt(bottom)}, expected yellow-green (hue 90 downwards)"
        }
        if (!(left[1] > 200 && left[2] > 200 && left[0] < 120)) {
            failures += "the left of the rim is ${fmt(left)}, expected cyan (hue 180)"
        }
        if (!(top[2] > 200 && top[0] < 200 && top[1] < 120)) {
            failures += "the top of the rim is ${fmt(top)}, expected blue (hue 270)"
        }

        // 3. Saturation comes from the centre: the middle is white, the rim is not.
        val middle = greenShot.rgb(cx, cy)
        println("centre=(${middle[0]},${middle[1]},${middle[2]})")
        val middleSpread = maxOf(middle[0], middle[1], middle[2]) - minOf(middle[0], middle[1], middle[2])
        if (middleSpread > 30 || middle[1] < 240) {
            failures += "the centre of the wheel is (${middle[0]},${middle[1]},${middle[2]}), expected white (saturation 0)"
        }

        // 4. The marker is where the picked colour is: at the right for a red
        //    accent (hue 0) and at the lower-left for a green one (hue 120).
        val greenMarker = markerCentre(greenShot, cx, cy, radius)
        val redMarker = markerCentre(redShot, cx, cy, radius)
        println("marker: green=$greenMarker red=$redMarker")
        if (redMarker == null || greenMarker == null) {
            failures += "no marker was found in one of the two renders"
        } else {
            val redAway = distance(redMarker, cx, cy)
            val redTowardRight = redMarker.first > cx + 0.6f * radius
            if (!redTowardRight || redAway < 0.6f * radius) {
                failures += "with a red accent the marker is at $redMarker, expected the right of the wheel"
            }
            val belowLeft = greenMarker.first < cx - 0.2f * radius &&
                greenMarker.second > cy + 0.4f * radius
            if (!belowLeft) {
                failures += "with a green accent the marker is at $greenMarker, expected the lower-left of the wheel"
            }
            val moved = distance(greenMarker, redMarker.first, redMarker.second)
            if (moved < 30f) {
                failures += "the marker did not move between a red and a green accent (moved ${moved.toInt()}px)"
            }
        }

        // 5. The brightness strip sits beside the wheel: full colour at its top,
        //    half way down in the middle, black at the bottom.
        // Four px below the top, because the thumb sitting at full brightness
        // is drawn as a white border right at the top of the strip.
        val stripX = wheel.x2 + 24
        val stripTop = sample(greenShot, stripX, wheel.y1 + 16)
        val stripMiddle = sample(greenShot, stripX, cy)
        println("strip: top=${fmt(stripTop)} middle=${fmt(stripMiddle)}")
        if (!(stripTop[1] > 200 && stripTop[0] < 100 && stripTop[2] < 100)) {
            failures += "the strip's top is ${fmt(stripTop)}, expected the colour at full brightness"
        }
        if (!(stripMiddle[1] in 90..180 && stripMiddle[0] < 90 && stripMiddle[2] < 90)) {
            failures += "the strip's middle is ${fmt(stripMiddle)}, expected half the brightness"
        }

        // 6. The three HSV bars are gone: a wide, thin band may still exist (the
        //    accent intensity slider is one), but no band may run through the
        //    hues the way the old hue bar did.
        val bands = blobs(greenShot).filter { it.width > 420 && it.height in 8..40 }
        for (band in bands) {
            val y = (band.y1 + band.y2) / 2
            val at = listOf(0.1, 0.5, 0.9).map { f ->
                sample(greenShot, band.x1 + (band.width * f).toInt(), y)
            }
            val change = colorDistance(at[0], at[2])
            if (change > 120) {
                failures += "a wide gradient colour bar is still drawn (${band.width}x${band.height}px at ${band.x1},${band.y1}: ${fmt(at[0])} to ${fmt(at[2])})"
            }
        }

        if (failures.isEmpty()) {
            println()
            println("OK: the wheel is round, the hue runs around the rim, the saturation from the centre, the marker follows the accent and its brightness strip is beside it")
            return
        }
        println()
        println("FAILED:")
        failures.forEach { println("  - $it") }
        throw IllegalStateException("colour wheel render check failed (${failures.size} case(s))")
    }

    private fun render(accent: Color, name: String): Pixels {
        val scene = ImageComposeScene(WIDTH, HEIGHT) {
            AppTheme(mode = ThemeMode.LIGHT, accent = accent) {
                ThemeSection(
                    language = "en",
                    mode = ThemeMode.LIGHT,
                    accent = accent,
                    onModeChange = {},
                    onAccentChange = {},
                    pureBlack = false,
                    onPureBlackChange = {},
                    // The picker opens on an accent it knows, which is the same
                    // condition the palette's own circle opens it through.
                    customAccents = listOf(colorToArgbInt(accent)),
                )
            }
        }
        val png = try {
            scene.render().encodeToData(EncodedImageFormat.PNG, 100)!!.bytes
        } finally {
            scene.close()
        }
        File(outDir, "$name.png").writeBytes(png)
        val image = javax.imageio.ImageIO.read(png.inputStream())
        val argb = IntArray(image.width * image.height)
        image.getRGB(0, 0, image.width, image.height, argb, 0, image.width)
        return Pixels(image.width, image.height, argb)
    }

    private class Pixels(val width: Int, val height: Int, val argb: IntArray) {

        fun chroma(x: Int, y: Int): Int {
            val p = argb[y * width + x]
            val r = (p shr 16) and 255
            val g = (p shr 8) and 255
            val b = p and 255
            return maxOf(r, g, b) - minOf(r, g, b)
        }

        fun rgb(x: Int, y: Int): IntArray {
            val p = argb[y.coerceIn(0, height - 1) * width + x.coerceIn(0, width - 1)]
            return intArrayOf((p shr 16) and 255, (p shr 8) and 255, p and 255)
        }
    }

    private data class Blob(val size: Int, val x1: Int, val y1: Int, val x2: Int, val y2: Int) {
        val width: Int get() = x2 - x1 + 1
        val height: Int get() = y2 - y1 + 1
    }

    /** Distance between a point and (x, y), in pixels. */
    private fun distance(point: Pair<Int, Int>, x: Int, y: Int): Float =
        kotlin.math.hypot((point.first - x).toFloat(), (point.second - y).toFloat())

    private fun sample(p: Pixels, x: Int, y: Int): IntArray = p.rgb(x, y)

    private fun fmt(rgb: IntArray): String = "(${rgb[0]},${rgb[1]},${rgb[2]})"

    /** How far apart two colours are, 0 (identical) to about 441. */
    private fun colorDistance(a: IntArray, b: IntArray): Double {
        val dr = (a[0] - b[0]).toDouble()
        val dg = (a[1] - b[1]).toDouble()
        val db = (a[2] - b[2]).toDouble()
        return kotlin.math.sqrt(dr * dr + dg * dg + db * db)
    }

    /**
     * The centre of the brightest (near-white) blob inside the disc: the marker.
     * The middle of the wheel is white too (saturation 0), so the centre of the
     * disc is skipped and only the outer half of the disc is searched.
     */
    private fun markerCentre(p: Pixels, cx: Int, cy: Int, radius: Float): Pair<Int, Int>? {
        var sumX = 0L
        var sumY = 0L
        var count = 0L
        val inner = radius * 0.35f
        val outer = radius * 0.99f
        for (y in maxOf(0, (cy - radius).toInt())..minOf(p.height - 1, (cy + radius).toInt())) {
            for (x in maxOf(0, (cx - radius).toInt())..minOf(p.width - 1, (cx + radius).toInt())) {
                val dx = x - cx
                val dy = y - cy
                val d2 = dx * dx + dy * dy
                if (d2 < inner * inner || d2 > outer * outer) continue
                val rgb = p.rgb(x, y)
                if (rgb[0] > 245 && rgb[1] > 245 && rgb[2] > 245) {
                    sumX += x
                    sumY += y
                    count++
                }
            }
        }
        if (count == 0L) return null
        return (sumX / count).toInt() to (sumY / count).toInt()
    }

    /** Every chromatic region of the picture, largest first. */
    private fun blobs(p: Pixels): List<Blob> {
        val seen = BooleanArray(p.width * p.height)
        val stack = IntArray(p.width * p.height)
        val found = mutableListOf<Blob>()
        for (start in 0 until p.width * p.height) {
            if (seen[start] || p.chroma(start % p.width, start / p.width) <= 40) continue
            var top = 0
            stack[top++] = start
            seen[start] = true
            var count = 0
            var minX = p.width
            var maxX = -1
            var minY = p.height
            var maxY = -1
            fun push(index: Int) {
                if (seen[index]) return
                if (p.chroma(index % p.width, index / p.width) <= 40) return
                seen[index] = true
                stack[top++] = index
            }
            while (top > 0) {
                val index = stack[--top]
                val x = index % p.width
                val y = index / p.width
                count++
                if (x < minX) minX = x
                if (x > maxX) maxX = x
                if (y < minY) minY = y
                if (y > maxY) maxY = y
                if (x > 0) push(index - 1)
                if (x < p.width - 1) push(index + 1)
                if (y > 0) push(index - p.width)
                if (y < p.height - 1) push(index + p.width)
            }
            found += Blob(count, minX, minY, maxX, maxY)
        }
        return found.sortedByDescending { it.size }
    }
}
