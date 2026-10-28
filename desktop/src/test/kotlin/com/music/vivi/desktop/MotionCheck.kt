@file:OptIn(ExperimentalComposeUiApi::class)

package com.music.vivi.desktop

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.ImageComposeScene
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import org.jetbrains.skia.EncodedImageFormat
import java.awt.image.BufferedImage
import java.io.File
import kotlin.math.abs

/**
 * Headless check of the Material 3 motion tokens, run with
 *
 *     ./gradlew :desktop:motionCheck
 *
 * Every transition in the DE is built out of [Motion], and nothing about a
 * duration or a curve is visible in a compile: a table that drifted from the
 * published tokens, or a curve replaced by the one beside it, would keep working
 * and simply move differently. Two things are therefore measured here:
 *
 *  * the table itself - the sixteen duration steps are the published numbers, the
 *    seven easing tokens are the published cubic-beziers (sampled and compared
 *    against an independent evaluation of those control points, which is what
 *    tells `emphasized` and `emphasized-decelerate` apart), and the
 *    Animation-speed setting still scales a duration (fast 0.6x, slow 1.6x);
 *  * the hover movement - [Modifier.hoverGrow] is painted at rest and at its
 *    hovered end, and the hovered render must cover strictly more pixels, which
 *    is what proves the shape grows rather than merely being drawn twice.
 *
 * PNGs of the two renders are written under `.ignore/motion/`.
 */
object MotionCheck {

    private const val SIZE = 160

    private val outDir = File(".ignore/motion")

    /** The published table, written out once so a change to it has to be meant. */
    private val DURATIONS = listOf(
        Triple("short1", Motion.SHORT1, 50),
        Triple("short2", Motion.SHORT2, 100),
        Triple("short3", Motion.SHORT3, 150),
        Triple("short4", Motion.SHORT4, 200),
        Triple("medium1", Motion.MEDIUM1, 250),
        Triple("medium2", Motion.MEDIUM2, 300),
        Triple("medium3", Motion.MEDIUM3, 350),
        Triple("medium4", Motion.MEDIUM4, 400),
        Triple("long1", Motion.LONG1, 450),
        Triple("long2", Motion.LONG2, 500),
        Triple("long3", Motion.LONG3, 550),
        Triple("long4", Motion.LONG4, 600),
        Triple("extra-long1", Motion.EXTRA_LONG1, 700),
        Triple("extra-long2", Motion.EXTRA_LONG2, 800),
        Triple("extra-long3", Motion.EXTRA_LONG3, 900),
        Triple("extra-long4", Motion.EXTRA_LONG4, 1000),
    )

    /** The easing tokens as published: (x1, y1, x2, y2). */
    private val CURVES = listOf(
        "emphasized" to (Motion.emphasized to listOf(0.2f, 0f, 0f, 1f)),
        "emphasized-decelerate" to (Motion.emphasizedDecelerate to listOf(0.05f, 0.7f, 0.1f, 1f)),
        "emphasized-accelerate" to (Motion.emphasizedAccelerate to listOf(0.3f, 0f, 0.8f, 0.15f)),
        "standard" to (Motion.standard to listOf(0.2f, 0f, 0f, 1f)),
        "standard-decelerate" to (Motion.standardDecelerate to listOf(0f, 0f, 0f, 1f)),
        "standard-accelerate" to (Motion.standardAccelerate to listOf(0.3f, 0f, 1f, 1f)),
        "linear" to (Motion.linear to listOf(0f, 0f, 1f, 1f)),
    )

    @JvmStatic
    fun main(args: Array<String>) {
        outDir.mkdirs()
        val failures = mutableListOf<String>()

        // 1. The duration table, and that it is in the published order.
        var previous = 0
        for ((name, value, published) in DURATIONS) {
            if (value != published) failures += "duration $name is ${value}ms, the token is ${published}ms"
            if (value <= previous) failures += "duration $name ($value) is not longer than the one before ($previous)"
            previous = value
        }
        println("durations: ${DURATIONS.size} steps, ${DURATIONS.first().first}=${DURATIONS.first().second}ms .. ${DURATIONS.last().first}=${DURATIONS.last().second}ms")

        // 2. The easing tokens, sampled against the published control points.
        //    Comparing the sampled curve is what distinguishes two tokens that
        //    both pass through (0,0) and (1,1): `emphasized` and
        //    `emphasized-decelerate` differ in the middle, not at the ends.
        for ((name, pair) in CURVES) {
            val (easing, points) = pair
            for (t in listOf(0.15f, 0.35f, 0.5f, 0.75f, 0.9f)) {
                val got = easing.transform(t)
                val want = bezier(points[0], points[1], points[2], points[3], t)
                if (abs(got - want) > 0.01f) {
                    failures += "easing $name at t=$t is ${round(got)}, the published curve gives ${round(want)}"
                }
            }
        }
        println("curves: ${CURVES.size} tokens, ${CURVES.first().first} .. ${CURVES.last().first}")

        // 3. The Animation-speed setting still reaches a duration.
        val wasSpeed = Animations.speed
        val wasEnabled = Animations.enabled
        try {
            Animations.enabled = true
            Animations.speed = "normal"
            val normal = Animations.ms(Motion.SHORT4)
            Animations.speed = "fast"
            val fast = Animations.ms(Motion.SHORT4)
            Animations.speed = "slow"
            val slow = Animations.ms(Motion.SHORT4)
            println("speed: short4 -> fast ${fast}ms, normal ${normal}ms, slow ${slow}ms")
            if (normal != Motion.SHORT4) failures += "the normal speed changed the token: ${normal}ms instead of ${Motion.SHORT4}ms"
            if (!(fast < normal && normal < slow)) failures += "the speed setting does not order fast < normal < slow ($fast / $normal / $slow)"
            Animations.enabled = false
            if (!Animations.instant) failures += "the master switch off does not report instant transitions"
            if (Animations.rowMs() != 0) failures += "with the master switch off a row still folds over ${Animations.rowMs()}ms"
        } finally {
            Animations.speed = wasSpeed
            Animations.enabled = wasEnabled
        }

        // 4. The hover movement, painted: at rest and at the end of the grow.
        val rest = render("cookie-rest", 0f)
        val hovered = render("cookie-hovered", 1f)
        val atRest = painted(rest)
        val atHover = painted(hovered)
        println("hover: rest=$atRest px, hovered=$atHover px")
        if (atRest <= 0) {
            failures += "the cookie painted nothing at rest, so the grow is untested"
        } else if (atHover <= atRest) {
            failures += "the hovered cookie covers $atHover px, not more than the $atRest px at rest"
        }

        if (failures.isEmpty()) {
            println()
            println("OK: the M3 duration and easing tokens, the speed setting and the hover grow")
            return
        }
        println()
        println("FAILED:")
        failures.forEach { println("  - $it") }
        throw IllegalStateException("motion check failed (${failures.size} case(s))")
    }

    /** A cookie at [progress] of its hover movement, drawn in the window it fits. */
    private fun render(name: String, progress: Float): BufferedImage {
        val scene = ImageComposeScene(SIZE, SIZE) {
            AppTheme(mode = ThemeMode.LIGHT, accent = Color(0xFFED5564)) {
                Box(Modifier.size(SIZE.dp).background(MaterialTheme.colorScheme.surface)) {
                    Box(
                        Modifier
                            .size(80.dp)
                            .hoverGrow(progress, grow = 0.08f)
                            .clip(CookieShapes.nineSided)
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

    private fun painted(image: BufferedImage): Int {
        val page = image.getRGB(0, 0)
        var count = 0
        for (y in 0 until image.height) {
            for (x in 0 until image.width) {
                if (image.getRGB(x, y) != page) count++
            }
        }
        return count
    }

    /**
     * The y a cubic-bezier timing function returns for [t], from its control
     * points: the curve is solved for x first (the standard bisection every
     * implementation of these tokens uses) and then evaluated at that parameter.
     */
    private fun bezier(x1: Float, y1: Float, x2: Float, y2: Float, t: Float): Float {
        fun sample(p1: Float, p2: Float, u: Float): Float {
            val v = 1f - u
            return 3f * v * v * u * p1 + 3f * v * u * u * p2 + u * u * u
        }
        var low = 0f
        var high = 1f
        var u = t
        repeat(40) {
            u = (low + high) / 2f
            if (sample(x1, x2, u) < t) low = u else high = u
        }
        return sample(y1, y2, u)
    }

    private fun round(value: Float): String = "%.3f".format(value)
}
