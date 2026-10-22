@file:OptIn(ExperimentalComposeUiApi::class)

package com.music.vivi.desktop

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.ImageComposeScene
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.PointerButtons
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.input.pointer.PointerType
import androidx.compose.ui.unit.dp
import org.jetbrains.skia.EncodedImageFormat
import java.awt.image.BufferedImage
import java.io.File

/**
 * Headless check of the Material 3 Expressive slider the settings screens use,
 * run with
 *
 *     ./gradlew :desktop:sliderRenderCheck
 *
 * The shape of a slider is a pixel-level fact that neither the compiler nor a
 * row count can see, so the composable is rendered into an off-screen
 * `ImageComposeScene` and read back. Density is 1, so a dp is a pixel:
 *
 *  * **at rest**: a row through the track centre is, left to right, `primary`
 *    up to the cut, 6 px of page, the 4 px handle, 6 px of page again, then
 *    `secondaryContainer` to the end. The handle sits where the value says.
 *  * **pressed**: the handle widens to 8 px and a state layer tints the ring
 *    around it, which at rest is plain page. Releasing puts both back.
 *  * **`steps`**: the values reported while dragging land on the ticks the
 *    caller declared (`steps = 3` over 0..1 means 0, .25, .5, .75, 1) and a
 *    continuous slider reports the position itself.
 *
 * The frames are written under `.ignore/slider/` so they can be looked at.
 */
object SliderRenderCheck {

    private val outDir = File(".ignore/slider")

    private const val WIDTH = 480
    private const val HEIGHT = 260

    /** Where the slider's 400 dp strip starts, and where its centre line is. */
    private const val LEFT = 40
    private const val CENTRE = 72

    @JvmStatic
    fun main(args: Array<String>) {
        outDir.mkdirs()
        val failures = mutableListOf<String>()

        val rest = restCase()
        File(outDir, "slider-rest.png").writeBytes(rest.png)
        File(outDir, "slider-pressed.png").writeBytes(rest.pressed)
        File(outDir, "slider-released.png").writeBytes(rest.released)
        println("at rest: ${rest.rest.row.describe()}")
        println("pressed: ${rest.during.row.describe()} (handle ${rest.during.handle} px, layer tinted ${rest.during.tinted} px)")
        println("released: ${rest.after.row.describe()} (handle ${rest.after.handle} px)")
        println("value: ${rest.reported}")

        if (rest.rest.row.significant() != listOf("P", "N", "P", "N", "I")) {
            failures += "a slider at rest should read primary, cut, handle, cut, inactive track, read ${rest.rest.row.significant()}: ${rest.rest.row.describe()}"
        }
        val handle = rest.rest.row.run("P", index = 1)
        if (handle == null || handle.length !in 3..6) {
            failures += "the handle at rest should be 4 px wide, read ${handle?.length ?: 0}: ${rest.rest.row.describe()}"
        }
        val leftCut = rest.rest.row.run("N", index = 0)
        val rightCut = rest.rest.row.run("N", index = 1)
        if (leftCut == null || leftCut.length !in 5..8) {
            failures += "the track should be cut 6 px before the handle, read ${leftCut?.length ?: 0} px"
        }
        if (rightCut == null || rightCut.length !in 5..8) {
            failures += "the track should be cut 6 px after the handle, read ${rightCut?.length ?: 0} px"
        }
        // 0.25 of a 400 dp strip, inset by the handle's own half-width: the
        // handle centre is at 4 + 0.25 * 392 = 102 px.
        if (handle != null && kotlin.math.abs(handle.start + handle.length / 2 - (LEFT + 102)) > 2) {
            failures += "at 0.25 the handle should sit at x=${LEFT + 102}, read its centre at ${handle.start + handle.length / 2}"
        }
        val active = rest.rest.row.run("P", index = 0)
        if (active == null || active.length < 80) {
            failures += "at 0.25 the primary part of the track should run up to the cut, read ${active?.length ?: 0} px"
        }
        if (rest.reported.firstOrNull()?.let { kotlin.math.abs(it - 0.755f) > 0.02f } != false) {
            failures += "pressing 300 px along the strip should put the value there (0.755), read ${rest.reported}"
        }

        if (rest.during.handle !in 6..10) {
            failures += "the handle should widen to 8 px while pressed, read ${rest.during.handle} px: ${rest.during.row.describe()}"
        }
        if (!rest.during.stateLayer) {
            failures += "while pressed the handle should draw its 40 dp state layer, and the ring around it is still plain page (${rest.during.tinted} px tinted)"
        }
        if (rest.after.handle !in 3..6) {
            failures += "after the release the handle should be 4 px again, read ${rest.after.handle} px: ${rest.after.row.describe()}"
        }

        val stepped = stepCase()
        println("steps: ${stepped.joinToString()}")
        if (stepped.any { it !in listOf(0f, 0.25f, 0.5f, 0.75f, 1f) }) {
            failures += "a slider with 3 steps should only report 0, .25, .5, .75 or 1, read $stepped"
        }
        if (stepped.firstOrNull { it >= 0.5f } != 0.5f) {
            failures += "a press at 60 % of a stepped slider should report 0.5, read $stepped"
        }

        if (failures.isEmpty()) {
            println()
            println("OK: the settings slider is a 16 dp capsule track cut 6 px either side of a 4 px handle that widens to 8 px with a state layer while pressed, and its values land on the declared ticks")
            return
        }
        println()
        println("FAILED:")
        failures.forEach { println("  - $it") }
        throw IllegalStateException("slider render check failed (${failures.size} case(s))")
    }

    /** One run of a single colour along the track's centre row. */
    private data class Run(val start: Int, val length: Int)

    /** The track's centre row, split into runs of `primary`, page and inactive. */
    private data class Row(val runs: List<Run>, val kinds: List<String>) {
        fun run(kind: String, index: Int): Run? = runs.filterIndexed { i, _ -> kinds[i] == kind }.getOrNull(index)
        fun describe(): String = kinds.mapIndexed { i, k -> "$k${runs[i].length}" }.joinToString(" ")

        /**
         * The runs that are the slider's shape, without the antialiased edges.
         *
         * A capsule's rounded ends feather into the page over a pixel or two, so
         * every edge arrives as an `X` run of one or two columns: they are the
         * drawing's own smoothing, not a part of the shape, and a row that read
         * `P92 X1 N6 P4 N6 X1 I288 X1` is the `P N P N I` a slider should read.
         * A wider `X` is the state layer over the page around the handle, which
         * is measured by [tinted] instead and never belongs to this list.
         */
        fun significant(): List<String> =
            kinds.indices.filter { kinds[it] != "X" || runs[it].length > 2 }.map { kinds[it] }
    }

    /**
     * One rendered frame, measured.
     *
     * `handle` is the run of `primary` the handle paints on the centre row and
     * `tinted` is the number of pixels of the ring around the handle that are no
     * longer the page colour. Both are read without the row's run splitting: the
     * state layer covers the cut either side of the handle, so the row merges
     * into one long `primary` run while the button is down.
     */
    private data class Frame(
        val row: Row,
        val handle: Int,
        val tinted: Int,
        val stateLayer: Boolean,
    )

    private data class Rest(
        val png: ByteArray,
        val pressed: ByteArray,
        val released: ByteArray,
        val rest: Frame,
        val during: Frame,
        val after: Frame,
        val reported: List<Float>,
    )

    /**
     * At rest, pressed 300 px along the strip, then released.
     *
     * Every frame is a strictly later timestamp than the one before: a render at
     * a time the scene has already passed stands still, and the 120 ms tweens
     * that widen the handle and fade the layer in would never finish. A pointer
     * event travels a pass further still - the frame that dispatches it, the one
     * that recomposes on the state it wrote, and the one where its tween starts -
     * so each state is settled frame by frame ([settle]) before it is read.
     */
    private fun restCase(): Rest {
        val reported = mutableListOf<Float>()
        val value = mutableStateOf(0.25f)
        val scene = scene {
            M3SettingsSlider(
                value = value.value,
                onValueChange = {
                    value.value = it
                    reported += it
                },
                modifier = Modifier.width(400.dp),
            )
        }
        try {
            scene.render(1_000_000_000L)
            val png = png(scene.render(1_500_000_000L))
            val rest = frame(png)

            scene.sendPointerEvent(
                PointerEventType.Press,
                Offset(LEFT + 300f, CENTRE.toFloat()),
                type = PointerType.Mouse,
                buttons = PointerButtons(isPrimaryPressed = true),
            )
            scene.render(2_000_000_000L)
            val pressed = png(scene.render(settle(scene, 2_100_000_000L)))
            val during = frame(pressed)

            scene.sendPointerEvent(
                PointerEventType.Release,
                Offset(LEFT + 300f, CENTRE.toFloat()),
                type = PointerType.Mouse,
                buttons = PointerButtons(),
            )
            scene.render(3_000_000_000L)
            val released = png(scene.render(settle(scene, 3_100_000_000L)))
            val after = frame(released)
            return Rest(png, pressed, released, rest, during, after, reported)
        } finally {
            scene.close()
        }
    }

    /** The values a stepped slider reports when pressed at three positions. */
    private fun stepCase(): List<Float> {
        val reported = mutableListOf<Float>()
        val value = mutableStateOf(0f)
        val scene = scene {
            M3SettingsSlider(
                value = value.value,
                onValueChange = {
                    value.value = it
                    // The down event of every press reports once; the value is
                    // fed back so the next press starts from the stored tick.
                    if (reported.lastOrNull() != it) reported += it
                },
                valueRange = 0f..1f,
                steps = 3,
                modifier = Modifier.width(400.dp),
            )
        }
        try {
            scene.render()
            scene.render(2_000_000_000L)
            // 60 %, 90 % and 2 % along the strip: the ticks nearest to them are
            // 0.5, 1 and 0.
            for (fraction in listOf(0.60f, 0.90f, 0.02f)) {
                val x = LEFT + 4 + 392 * fraction
                scene.sendPointerEvent(
                    PointerEventType.Press,
                    Offset(x, CENTRE.toFloat()),
                    type = PointerType.Mouse,
                    buttons = PointerButtons(isPrimaryPressed = true),
                )
                scene.render()
                scene.sendPointerEvent(
                    PointerEventType.Release,
                    Offset(x, CENTRE.toFloat()),
                    type = PointerType.Mouse,
                    buttons = PointerButtons(),
                )
                scene.render()
            }
            return reported.toList()
        } finally {
            scene.close()
        }
    }

    /** A full-size scene with the slider under test near the top. */
    private fun scene(content: @androidx.compose.runtime.Composable () -> Unit): ImageComposeScene =
        ImageComposeScene(WIDTH, HEIGHT) {
            AppTheme(mode = ThemeMode.LIGHT, accent = Color(0xFFED5564), language = "en") {
                Box(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.surface)) {
                    Column(Modifier.padding(start = LEFT.dp, top = 24.dp).width(400.dp)) {
                        Box(Modifier.height(24.dp))
                        content()
                    }
                    // The theme's own colours in the corners the measured region
                    // never covers, so the runs are read against what the theme
                    // actually resolved instead of a hard-coded palette.
                    Box(
                        Modifier
                            .align(Alignment.BottomStart)
                            .size(6.dp)
                            .background(MaterialTheme.colorScheme.primary),
                    )
                    Box(
                        Modifier
                            .align(Alignment.BottomStart)
                            .padding(start = 10.dp)
                            .size(6.dp)
                            .background(MaterialTheme.colorScheme.secondaryContainer),
                    )
                }
            }
        }

    /**
     * Reads one frame.
     *
     * The row is found rather than assumed: the one with the most `primary`
     * pixels is inside the 16 dp track, and every row of the track reads the
     * same. `primary` is the swatch in the bottom-left corner, the inactive
     * track the one 10 dp to its right, the page the empty corner.
     */
    private fun frame(png: ByteArray): Frame {
        val image = javax.imageio.ImageIO.read(png.inputStream())
        val primary = image.getRGB(2, image.height - 3)
        val inactive = image.getRGB(12, image.height - 3)
        val page = image.getRGB(image.width - 2, 2)
        var y = -1
        var best = -1
        for (candidate in 0 until image.height - 20) {
            var count = 0
            for (x in LEFT until LEFT + 400) {
                if (near(image.getRGB(x, candidate), primary, 24)) count++
            }
            if (count > best) {
                best = count
                y = candidate
            }
        }
        check(y >= 0 && best > 0) { "the frame has no track in it" }

        val runs = mutableListOf<Run>()
        val kinds = mutableListOf<String>()
        var kind = ""
        var start = LEFT
        for (x in LEFT until LEFT + 400) {
            val ref = image.getRGB(x, y)
            // `X` is a pixel that is none of the three: the state layer over the
            // page around the handle, or an antialiased edge.
            val k = when {
                near(ref, primary, 24) -> "P"
                near(ref, inactive, 24) -> "I"
                near(ref, page, 8) -> "N"
                else -> "X"
            }
            if (k != kind || runs.isEmpty()) {
                if (runs.isNotEmpty()) runs[runs.size - 1] = Run(start, x - start)
                runs += Run(x, 0)
                kinds += k
                kind = k
                start = x
            }
        }
        runs[runs.size - 1] = Run(start, LEFT + 400 - start)
        // The runs against the strip's ends are the page, not part of the slider.
        val first = kinds.indexOfFirst { it != "N" }
        val last = kinds.indexOfLast { it != "N" }
        check(first >= 0) { "the frame has no slider in it" }

        // The handle is the primary run around the pointer, walked out from the
        // centre so the state layer's `X` runs either side cannot join it.
        val centre = handleCentre(image, y, primary) ?: -1
        val handle = if (centre < 0) 0 else runLength(image, y, centre, primary)
        val tint = tinted(image, page, primary)
        return Frame(
            row = Row(runs.subList(first, last + 1), kinds.subList(first, last + 1)),
            handle = handle,
            tinted = tint,
            stateLayer = tint >= 40,
        )
    }

    /** The middle of the handle: the last `primary` column of the track's row. */
    private fun handleCentre(image: BufferedImage, y: Int, primary: Int): Int? {
        for (x in LEFT + 399 downTo LEFT) {
            if (near(image.getRGB(x, y), primary, 24)) {
                // The rightmost primary column on the row is the handle's right
                // edge, the inactive track being a different colour.
                return x - 1
            }
        }
        return null
    }

    /** How wide the run of [primary] around [x] is. */
    private fun runLength(image: BufferedImage, y: Int, x: Int, primary: Int): Int {
        var left = x
        var right = x
        while (left > 0 && near(image.getRGB(left - 1, y), primary, 24)) left--
        while (right < image.width - 1 && near(image.getRGB(right + 1, y), primary, 24)) right++
        return right - left + 1
    }

    /**
     * Pixels of the handle's 40 dp state layer that are no longer the page.
     *
     * The strip's centre line is at y = 72, so the layer runs 52..92; the rows
     * read are 12 px either side of it, clear of the 16 dp track (8 px each way)
     * and, at 5..16 px from the handle, clear of the handle itself. At rest they
     * are plain page, so only the layer can tint them.
     */
    private fun tinted(image: BufferedImage, page: Int, primary: Int): Int {
        var count = 0
        val centre = handleCentre(image, CENTRE, primary) ?: return 0
        for (y in listOf(CENTRE - 12, CENTRE + 12)) {
            for (dx in 5..16) {
                for (x in listOf(centre - dx, centre + dx)) {
                    if (x in 0 until image.width && !near(image.getRGB(x, y), page, 3)) count++
                }
            }
        }
        return count
    }

    /**
     * Renders frames 150 ms apart until the 120 ms tweens of a state change have
     * finished, and answers the timestamp the next frame should be read at.
     *
     * The scene applies a state written by a pointer handler some passes later
     * than the event, and an animation takes its start time from the first frame
     * after that: one render per millisecond of tween is the only way to be sure
     * the figure read is the one the drawing settled on.
     */
    private fun settle(scene: ImageComposeScene, start: Long): Long {
        var time = start
        repeat(4) {
            scene.render(time)
            time += 150_000_000L
        }
        return time
    }

    private fun png(image: org.jetbrains.skia.Image): ByteArray =
        image.encodeToData(EncodedImageFormat.PNG)?.bytes ?: error("the scene produced no image")

    private fun near(a: Int, b: Int, threshold: Int): Boolean {
        val dr = kotlin.math.abs(((a shr 16) and 255) - ((b shr 16) and 255))
        val dg = kotlin.math.abs(((a shr 8) and 255) - ((b shr 8) and 255))
        val db = kotlin.math.abs((a and 255) - (b and 255))
        return dr + dg + db <= threshold
    }
}
