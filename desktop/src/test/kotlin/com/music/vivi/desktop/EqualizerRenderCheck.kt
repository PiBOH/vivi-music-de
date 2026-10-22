@file:OptIn(ExperimentalComposeUiApi::class)

package com.music.vivi.desktop

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Alignment
import androidx.compose.ui.ImageComposeScene
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.pointer.PointerButtons
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.input.pointer.PointerType
import androidx.compose.ui.unit.dp
import org.jetbrains.skia.EncodedImageFormat
import java.io.File

/**
 * Headless check of the rebuilt Equalizer screen, run with
 *
 *     ./gradlew :desktop:equalizerRenderCheck
 *
 * The screen is the phone's own list (no equalization plus every AutoEQ profile
 * imported here) and, for the active profile, the shape of it: a response curve,
 * the preamp and one row per band with the Q factors folded under a header. None
 * of that can be seen without a display, so the screen is rendered into an
 * off-screen `ImageComposeScene` in both states and the pixels are read back:
 *
 *  * **no profile: the list and nothing else.** Four card bands, in ONE card (no
 *    8 dp gap between them), and no `primary` ink anywhere on the page - there is
 *    no curve and no band row while the id is empty, which is what
 *    `activeProfileId.isEmpty()` means in the app.
 *  * **a profile: the curve, the preamp and ten bands.** Exactly one wide band of
 *    `primary` ink in the top part of the page (the response line and its dots,
 *    which the band rows cannot produce: their ink is the slider's, and the
 *    swatch corner is outside the measured region) and four cards more than the
 *    empty state, of which the band card alone holds ten rows.
 *  * **the editor folds.** Choosing a profile grows the curve, the preamp and the
 *    ten bands out from under the list, and dropping it folds them back: the
 *    page's bottom edge is read frame by frame, so a jump fails the check.
 *  * **the curve is a control.** A drag on a point edits that band, and the band
 *    is named over it while the button is down; a plain click on a point sets that
 *    band to the height clicked and names it for a second, while a click away from
 *    every point changes nothing.
 *
 * The first render is the negative control: it also proves the curve is the
 * profile's, not a painted decoration that would appear for "no equalization".
 *
 * PNGs are written under `.ignore/equalizer/` so both states can be looked at.
 */
object EqualizerRenderCheck {

    private val outDir = File(".ignore/equalizer")

    /** Wide enough that no band title wraps, tall enough for all four cards. */
    private const val WIDTH = 900
    private const val HEIGHT = 2600

    /** The app's own bands, as an imported AutoEQ profile carries them. */
    private val BANDS = listOf(
        31.0 to 4.0, 62.0 to 3.0, 125.0 to 1.0, 250.0 to -1.0, 500.0 to 0.0,
        1000.0 to 0.0, 2000.0 to 2.0, 4000.0 to 3.0, 8000.0 to 1.0, 16000.0 to -2.0,
    )

    private val PROFILE = SavedEQProfile(
        id = "example",
        name = "Example profile",
        deviceModel = "Example in-ear",
        bands = BANDS.map { (hz, gain) -> ParametricEQBand(frequency = hz, gain = gain) },
    )

    @JvmStatic
    fun main(args: Array<String>) {
        outDir.mkdirs()
        val failures = mutableListOf<String>()

        val empty = render(activeId = "")
        File(outDir, "equalizer-none.png").writeBytes(empty)
        val emptyBands = bands(empty)
        val emptyInk = primaryInk(empty, from = 0, to = HEIGHT)
        println("no profile: cards=${emptyBands.cards} rows=${emptyBands.rows} gaps=${emptyBands.gaps} primary ink=$emptyInk")

        val active = render(activeId = PROFILE.id)
        File(outDir, "equalizer-active.png").writeBytes(active)
        val activeBands = bands(active)
        // The curve is the top card, under the four list rows: the ink is read
        // there, where no slider lives.
        val curveInk = primaryInk(active, from = 0, to = HEIGHT / 2)
        println("profile: cards=${activeBands.cards} rows=${activeBands.rows} gaps=${activeBands.gaps} curve ink=$curveInk")

        if (emptyBands.cards != 1) {
            failures += "with no profile the list should be ONE card, read ${emptyBands.cards}"
        }
        if (emptyBands.rows != 4) {
            failures += "with no profile the list should hold 4 rows (no equalization, the profile, import, add example), read ${emptyBands.rows}"
        }
        // The only `primary` a screen of rows paints is the tint of the 22 dp icon
        // tiles, a few hundred pixels; a drawn curve is tens of thousands.
        if (emptyInk > 600) {
            failures += "with no profile there should be no curve on the page: the primary ink reads $emptyInk px, which is more than the icon tiles paint"
        }

        if (activeBands.cards != 4) {
            failures += "with a profile the screen should hold 4 cards (the list, the curve, the preamp, the bands), read ${activeBands.cards}: ${activeBands.gaps}"
        }
        if (activeBands.rows != 16) {
            failures += "with a profile the screen should hold 16 rows (4 list, the curve, the preamp, 10 bands), read ${activeBands.rows}"
        }
        if (curveInk < 20_000) {
            failures += "with a profile the response curve should be drawn in primary ink, read only $curveInk px in the upper half"
        }
        // The band card is the run of ten rows joined by 3 dp seams that follows
        // the third card gap (list, curve, preamp): nine seams, ten rows.
        val gapList = activeBands.gaps
        val thirdGap = gapList.indices.filter { gapList[it] >= 6 }.getOrNull(2)
        val bandSeams = if (thirdGap == null) 0 else gapList.drop(thirdGap + 1).takeWhile { it < 6 }.size
        if (bandSeams != 9) {
            failures += "the band card should hold ten rows joined by nine 3 dp seams, read $bandSeams seam(s) in ${gapList}"
        }

        // Choosing a profile folds the editor open instead of dropping four cards
        // under the list in one frame, and *No equalization* folds it shut.
        val fold = foldCase()
        println("fold: open=${fold.opening} close=${fold.closing}")
        val closed = fold.opening.first()
        val open = fold.opening.last()
        if (open <= closed) {
            failures += "choosing a profile should push the page down by the editor, read $closed -> $open"
        }
        if (fold.opening.any { it > open }) {
            failures += "the fold should not overshoot the editor's own height, read ${fold.opening}"
        }
        // A jump in one frame is not a fold: at least two frames have to sit
        // strictly between the closed page and the open one.
        val opening = fold.opening.count { it > closed && it < open }
        if (Animations.enabled && opening < 2) {
            failures += "the editor should fold open over several frames, read $opening frame(s) between $closed and $open: ${fold.opening}"
        }
        // And back: dropping the profile collapses the same block, not just
        // removes it.
        val closing = fold.closing.count { it > closed && it < open }
        if (Animations.enabled && closing < 2) {
            failures += "the editor should fold shut over several frames, read $closing frame(s) between $open and $closed: ${fold.closing}"
        }
        if (fold.closing.last() != closed) {
            failures += "the closed editor should leave the page where the list ends ($closed), read ${fold.closing.last()}"
        }

        // The curve is a control, not a picture: a press on it takes the band
        // under the pointer and a vertical drag sets that band's gain, the same
        // edit its own slider makes below.
        val dragged = dragCase()
        File(outDir, "equalizer-dragged.png").writeBytes(dragged.png)
        File(outDir, "equalizer-dragged-readout.png").writeBytes(dragged.during)
        println("drag: x=${dragged.x} y=${dragged.fromY}->${dragged.toY} gains=${dragged.calls.map { call -> call[3].gain }}")
        if (dragged.calls.isEmpty()) {
            failures += "dragging the curve reported no band change at all: the press at (${dragged.x}, ${dragged.fromY}) reached nothing"
        } else {
            val changes = dragged.last.orEmpty().mapIndexedNotNull { index, band ->
                if (index < BANDS.size && band.gain != BANDS[index].second) index else null
            }
            if (changes != listOf(3)) {
                failures += "a drag on the point of the fourth band should move that band alone, moved $changes of ${dragged.last?.map { it.gain }}"
            } else if (dragged.calls.size < 2) {
                failures += "one drag should report both of its moves, reported ${dragged.calls.size}"
            } else {
                // The second half of the gesture is 20 px further up, and 20 px
                // is 20 / (60 * 0.92) * 12 dB: the check reads the difference
                // between the two reports, so it measures the drag's own map
                // instead of trusting where the chart was thought to be.
                val step = dragged.last!![3].gain - dragged.calls.first()[3].gain
                if (step < 3.5 || step > 5.0) {
                    failures += "a second 20 px up should add about 4.35 dB, read $step"
                }
                // The value lands on the sliders' half-dB stops, not on a float.
                if (dragged.last!![3].gain * 2 != (dragged.last!![3].gain * 2).toInt().toDouble()) {
                    failures += "a dragged gain should land on a half-dB stop, read ${dragged.last!![3].gain}"
                }
            }
        }

        // The band under the pointer is named while it is dragged: the readout
        // carries its frequency and the gain being set, on the sliders' stops.
        listOf(
            ParametricEQBand(frequency = 250.0, gain = 5.5) to "250 Hz   +5.5 dB",
            ParametricEQBand(frequency = 16000.0, gain = -2.0) to "16000 Hz   -2.0 dB",
            ParametricEQBand(frequency = 62.0, gain = 0.0) to "62 Hz   0.0 dB",
        ).forEach { (band, expected) ->
            val read = eqBandReadout(band)
            if (read != expected) {
                failures += "the readout of a ${band.frequency} Hz band at ${band.gain} dB should read \"$expected\", read \"$read\""
            }
        }
        // And it is drawn: the frame with the button still down paints a pill
        // above the dragged point that the frame after the release does not.
        val readoutInk = frameDiff(dragged.during, dragged.png)
        println("readout: painted=${readoutInk.count} px, top=${readoutInk.top} (point at ${dragged.toY - 20})")
        if (readoutInk.count < 700) {
            failures += "the dragged band's readout should be painted while the drag is in flight and gone after the release, read only ${readoutInk.count} differing px"
        } else if (readoutInk.top > dragged.toY - 20) {
            failures += "the readout should sit above the dragged point (${dragged.toY - 20}), read its top at ${readoutInk.top}"
        }

        // A plain click on a point is the second way to set that band: the height
        // clicked becomes its gain, on the point itself only, and the pill names it
        // for a second afterwards.
        val tapped = tapCase()
        File(outDir, "equalizer-clicked.png").writeBytes(tapped.png)
        File(outDir, "equalizer-clicked-settled.png").writeBytes(tapped.settled)
        val gained = tapped.calls.drop(tapped.farCalls)
        println("click: x=${tapped.x} y=${tapped.y} far=${tapped.farCalls} reported=${gained.map { it[3].gain }}")
        val clickPill = frameDiff(tapped.png, tapped.settled)
        println("click readout: painted=${clickPill.count} px")
        if (tapped.farCalls != 0) {
            failures += "a click 60 px above the fourth band's point is outside the 24 dp it reaches for, but it reported ${tapped.farCalls} change(s)"
        }
        if (gained.size != 1) {
            failures += "a click on the fourth band's point should report one change, reported ${gained.size}"
        } else {
            val after = gained.first()
            val moved = after.mapIndexedNotNull { index, band ->
                if (index < BANDS.size && band.gain != BANDS[index].second) index else null
            }
            if (moved != listOf(3)) {
                failures += "a click on the fourth band's point should move that band alone, moved $moved"
            }
            if (after[3].gain != 0.0) {
                failures += "a click on the chart's 0 dB line should set that band to 0 dB, read ${after[3].gain}"
            }
        }
        if (clickPill.count < 700) {
            failures += "a click on a point should name the band for a second, read only ${clickPill.count} differing px between the frame after the click and the one a second later"
        }

        if (failures.isEmpty()) {
            println()
            println("OK: the equalizer list is one card, the curve is drawn only for an active profile, its ten band rows are there (no Q card), the editor folds open and shut over its frames, a drag on the curve edits the band under it and names it over its point, and a click on a point sets that band to the height clicked and names it for a second")
            return
        }
        println()
        println("FAILED:")
        failures.forEach { println("  - $it") }
        throw IllegalStateException("equalizer render check failed (${failures.size} case(s))")
    }

    /**
     * The editor's fold, frame by frame.
     *
     * The chosen id is the screen's own parameter, so the case flips it in the
     * middle of the render, exactly as a click on the profile row does. The page's
     * bottom edge (the last row of content in the middle column) is the thing that
     * travels: one jump means the cards were dropped in, a travel means the block
     * folded.
     */
    private data class Fold(val opening: List<Int>, val closing: List<Int>)

    private fun foldCase(): Fold {
        val chosen = mutableStateOf("")
        val scene = ImageComposeScene(WIDTH, HEIGHT) {
            AppTheme(mode = ThemeMode.LIGHT, accent = Color(0xFFED5564), language = "en") {
                Box(Modifier.fillMaxSize()) {
                    SettingsEqualizerScreen(
                        language = "en",
                        onBack = {},
                        profiles = listOf(PROFILE),
                        activeProfileId = chosen.value,
                        onSelectProfile = {},
                        onImportProfile = { _, _ -> },
                        onDeleteProfile = {},
                        onAddExampleProfile = {},
                        onUpdateProfile = { _, _, _ -> },
                    )
                }
            }
        }
        try {
            val heights = mutableListOf<Int>()
            var time = 0L
            heights += bottomOf(scene.render(time))
            chosen.value = PROFILE.id
            repeat(8) {
                time += 25_000_000L
                // One frame to apply the new state and start the transition, the
                // same instant read again: a `render` at the start time returns the
                // state before the transition's first frame has been drawn.
                scene.render(time)
                heights += bottomOf(scene.render(time))
            }
            time += 2_000_000_000L
            heights += bottomOf(scene.render(time))
            val opening = heights.toList()
            // Dropping the profile: the same block, folding shut.
            val closing = mutableListOf<Int>()
            chosen.value = ""
            repeat(8) {
                time += 25_000_000L
                scene.render(time)
                closing += bottomOf(scene.render(time))
            }
            time += 2_000_000_000L
            closing += bottomOf(scene.render(time))
            return Fold(opening, closing)
        } finally {
            scene.close()
        }
    }

    /** The page's bottom edge in the middle column of [image]. */
    private fun bottomOf(image: org.jetbrains.skia.Image): Int {
        val png = image.encodeToData(EncodedImageFormat.PNG)?.bytes ?: error("the scene produced no image")
        val decoded = javax.imageio.ImageIO.read(png.inputStream())
        val x = decoded.width / 2
        val page = decoded.getRGB(2, 2)
        for (y in decoded.height - 1 downTo 0) {
            if (!near(decoded.getRGB(x, y), page, 6)) return y
        }
        return 0
    }

    /** What a drag on the curve reported, and the frames it left behind. */
    private data class Drag(
        val during: ByteArray,
        val png: ByteArray,
        val calls: List<List<ParametricEQBand>>,
        val last: List<ParametricEQBand>?,
        val x: Int,
        val fromY: Int,
        val toY: Int,
    )

    /**
     * Presses the curve on the point of the fourth band and drags it 40 px up.
     *
     * The card is rendered on its own, in a corner, so its geometry is the one
     * the composable gives it instead of a guess about the screen around it. The
     * chart's bottom is the last row its fill covers across the card's inner
     * width (the 0.16 `primary` over the card's own surface); the canvas content
     * is 120 dp tall with 0 dB at its middle, the same map the drawing uses.
     */
    private fun dragCase(): Drag {
        val calls = mutableListOf<List<ParametricEQBand>>()
        val bands = mutableStateOf(BANDS.map { (hz, gain) -> ParametricEQBand(frequency = hz, gain = gain) })
        val scene = ImageComposeScene(WIDTH, HEIGHT) {
            AppTheme(mode = ThemeMode.LIGHT, accent = Color(0xFFED5564), language = "en") {
                Box(Modifier.fillMaxSize()) {
                    EqualizerResponseCard(
                        language = "en",
                        profile = PROFILE,
                        bands = bands.value,
                        modifier = Modifier.fillMaxWidth().padding(start = 24.dp, top = 24.dp, end = 24.dp),
                        // The card is the owner of its own draft in the screen, so
                        // here the check is: the value it hands up is stored and
                        // fed back, which is what makes the point follow the drag.
                        onBandGainChange = { index, gain ->
                            bands.value = bands.value.toMutableList().also {
                                it[index] = it[index].copy(gain = gain)
                            }
                            calls += bands.value
                        },
                    )
                }
            }
        }
        try {
            scene.render()
            val frame = scene.render(2_000_000_000L).encodeToData(EncodedImageFormat.PNG)!!.bytes
            val fill = chartFill(javax.imageio.ImageIO.read(frame.inputStream()))
            val fromY = fill.bottom - 59
            val toY = fromY - 40
            val x = fill.left + ((fill.right - fill.left) * 0.3f).toInt()
            // A mouse drag: the press, the move with the button still down, the
            // release. `detectDragGestures` waits for the slop, so the move is
            // 40 px, far past it.
            scene.sendPointerEvent(
                PointerEventType.Press,
                Offset(x.toFloat(), fromY.toFloat()),
                type = PointerType.Mouse,
                buttons = PointerButtons(isPrimaryPressed = true),
            )
            scene.render()
            scene.sendPointerEvent(
                PointerEventType.Move,
                Offset(x.toFloat(), toY.toFloat()),
                type = PointerType.Mouse,
                buttons = PointerButtons(isPrimaryPressed = true),
            )
            scene.render()
            scene.sendPointerEvent(
                PointerEventType.Move,
                Offset(x.toFloat(), (toY - 20).toFloat()),
                type = PointerType.Mouse,
                buttons = PointerButtons(isPrimaryPressed = true),
            )
            scene.render()
            // The button is still down here: the gains have settled (the same
            // 2 s the other frames read at, so the curve is where the drag put
            // it) and the readout of the dragged band is on the chart.
            val during = scene.render(2_000_000_000L)
                .encodeToData(EncodedImageFormat.PNG)?.bytes ?: error("the scene produced no image")
            scene.sendPointerEvent(
                PointerEventType.Release,
                Offset(x.toFloat(), (toY - 20).toFloat()),
                type = PointerType.Mouse,
                buttons = PointerButtons(),
            )
            val image2 = scene.render(4_000_000_000L)
            val png = image2.encodeToData(EncodedImageFormat.PNG)?.bytes ?: error("the scene produced no image")
            return Drag(during, png, calls, calls.lastOrNull(), x, fromY, toY)
        } finally {
            scene.close()
        }
    }

    /** Where the card's chart sits in a frame of the card rendered on its own. */
    private data class ChartFill(val left: Int, val right: Int, val bottom: Int)

    /**
     * Reads the chart's left, right and bottom edge out of a frame.
     *
     * The fill is `primary` at 0.16 over the card's own surface, so a row of it is
     * pinker than the page: the card, the page and the text are all neutral to the
     * same degree, which is what makes the difference from the page's own tint the
     * signal. The bottom is the last row the fill covers across the card's inner
     * width; the canvas content is 120 dp tall with 0 dB at its middle, the same
     * map the drawing uses.
     */
    private fun chartFill(image: java.awt.image.BufferedImage): ChartFill {
        val innerLeft = 24 + 17
        val innerRight = WIDTH - 24 - 17
        val cardTop = 24
        val samples = (innerRight - innerLeft) / 4
        val page = image.getRGB(2, HEIGHT - 2)
        val pageTint = ((page shr 16) and 255) - ((page shr 8) and 255)
        var bottom = -1
        for (y in image.height - 1 downTo cardTop) {
            var pink = 0
            for (x in innerLeft until innerRight step 4) {
                val rgb = image.getRGB(x, y)
                if (((rgb shr 16) and 255) - ((rgb shr 8) and 255) - pageTint >= 4) pink++
            }
            if (pink >= samples * 0.9) {
                bottom = y
                break
            }
        }
        check(bottom > 0) { "the frame has no filled response curve on the card" }
        return ChartFill(innerLeft, innerRight, bottom)
    }

    /** What a plain click on the curve reported, and the frames it left behind. */
    private data class Tap(
        val png: ByteArray,
        val settled: ByteArray,
        val calls: List<List<ParametricEQBand>>,
        val farCalls: Int,
        val x: Int,
        val y: Int,
    )

    /**
     * Clicks the curve twice: once far from every point, once on the fourth
     * band's own dot.
     *
     * `detectTapGestures` and `detectDragGestures` are two pointer inputs on the
     * same canvas, so the check is also that a click reaches the one that is about
     * to be tested: a press and a release with no movement in between must report
     * the gain of the height clicked, for the band under the pointer. A click 60 px
     * away from the point is outside the 24 dp the dot reaches for and must report
     * nothing, and the readout pill must be painted for the second after it.
     */
    private fun tapCase(): Tap {
        val calls = mutableListOf<List<ParametricEQBand>>()
        val bands = mutableStateOf(BANDS.map { (hz, gain) -> ParametricEQBand(frequency = hz, gain = gain) })
        val scene = ImageComposeScene(WIDTH, HEIGHT) {
            AppTheme(mode = ThemeMode.LIGHT, accent = Color(0xFFED5564), language = "en") {
                Box(Modifier.fillMaxSize()) {
                    EqualizerResponseCard(
                        language = "en",
                        profile = PROFILE,
                        bands = bands.value,
                        modifier = Modifier.fillMaxWidth().padding(start = 24.dp, top = 24.dp, end = 24.dp),
                        onBandGainChange = { index, gain ->
                            bands.value = bands.value.toMutableList().also {
                                it[index] = it[index].copy(gain = gain)
                            }
                            calls += bands.value
                        },
                    )
                }
            }
        }
        // A press and a release with a frame between them: the gesture block awaits
        // the press first, and with both events dispatched before any frame it
        // would see them as one batch.
        fun click(x: Int, y: Int) {
            scene.sendPointerEvent(
                PointerEventType.Press,
                Offset(x.toFloat(), y.toFloat()),
                type = PointerType.Mouse,
                buttons = PointerButtons(isPrimaryPressed = true),
            )
            scene.render()
            scene.sendPointerEvent(
                PointerEventType.Release,
                Offset(x.toFloat(), y.toFloat()),
                type = PointerType.Mouse,
                buttons = PointerButtons(),
            )
            scene.render()
        }
        try {
            scene.render()
            val frame = scene.render(2_000_000_000L).encodeToData(EncodedImageFormat.PNG)!!.bytes
            val fill = chartFill(javax.imageio.ImageIO.read(frame.inputStream()))
            // On the fourth band's own column: a click is hit-tested against its
            // point, not against the nearest column the way a drag is.
            val x = fill.left + ((fill.right - fill.left) * (3f / (BANDS.size - 1))).toInt()
            // The fill and the card's own surface are both slightly tinted, so the
            // row the helper finds is the card's bottom edge: the canvas ends 14 px
            // above it (the card's vertical padding). The canvas is 120 dp tall with
            // 0 dB at its middle, so its centre line is 60 px above its bottom. The
            // fourth band sits at -1 dB, 4.6 px above that line on the same column: a
            // click there is inside the 24 dp the dot reaches for, and asks for a gain
            // it does not already have.
            val canvasBottom = fill.bottom - 14
            val y = canvasBottom - 60
            // 60 px further up is well outside the 24 dp a click may land from a dot.
            click(x, y - 60)
            scene.render(2_300_000_000L)
            val farCalls = calls.size
            click(x, y)
            // 300 ms after the click: the dot has settled and the readout is still
            // up (its second has not passed).
            val png = scene.render(2_600_000_000L)
                .encodeToData(EncodedImageFormat.PNG)?.bytes ?: error("the scene produced no image")
            // The pill goes down by a second: a late frame must not draw it. The
            // wait is real time as well, because a LaunchedEffect may sit on either
            // clock depending on the scene's dispatcher.
            Thread.sleep(1_200)
            val settled = scene.render(4_000_000_000L)
                .encodeToData(EncodedImageFormat.PNG)?.bytes ?: error("the scene produced no image")
            return Tap(png, settled, calls, farCalls, x, y)
        } finally {
            scene.close()
        }
    }

    private fun render(activeId: String): ByteArray {
        val scene = ImageComposeScene(WIDTH, HEIGHT) {
            AppTheme(mode = ThemeMode.LIGHT, accent = Color(0xFFED5564), language = "en") {
                Box(Modifier.fillMaxSize()) {
                    SettingsEqualizerScreen(
                        language = "en",
                        onBack = {},
                        profiles = listOf(PROFILE),
                        activeProfileId = activeId,
                        onSelectProfile = {},
                        onImportProfile = { _, _ -> },
                        onDeleteProfile = {},
                        onAddExampleProfile = {},
                        onUpdateProfile = { _, _, _ -> },
                    )
                    // The theme's own `primary`, in a corner the measured region
                    // never covers: the ink count is compared against this.
                    Box(
                        Modifier
                            .align(Alignment.BottomStart)
                            .size(6.dp)
                            .background(MaterialTheme.colorScheme.primary),
                    )
                }
            }
        }
        try {
            // Two frames: the first lays the screen out, the second paints what
            // the animated curve settled on (the band heights are
            // `animateFloatAsState`, so frame one is still at 0 dB).
            scene.render()
            val image = scene.render(2_000_000_000L)
            return image.encodeToData(EncodedImageFormat.PNG)?.bytes
                ?: error("the scene produced no image")
        } finally {
            scene.close()
        }
    }

    /** What the middle column of the render says about the cards. */
    private data class Bands(val cards: Int, val rows: Int, val gaps: List<Int>)

    private fun bands(png: ByteArray): Bands {
        val cleaned = cleanedRuns(png)
        val bands = cleaned.filter { it.first }
        val gaps = cleaned.dropWhile { !it.first }.filter { !it.first }.map { it.second }
        // The last page run is the space under the last card, not a gap.
        val between = gaps.dropLast(1)
        val cards = between.count { it >= 6 } + 1
        return Bands(cards = cards, rows = bands.size, gaps = between)
    }

    /** The middle column as runs of "card" and "page", with text dropped. */
    private fun cleanedRuns(png: ByteArray): List<Pair<Boolean, Int>> {
        val image = javax.imageio.ImageIO.read(png.inputStream())
        val x = image.width / 2
        val page = image.getRGB(x, 2)
        val runs = mutableListOf<Pair<Boolean, Int>>()
        for (y in 0 until image.height) {
            val onCard = !near(image.getRGB(x, y), page, 6)
            if (runs.isNotEmpty() && runs.last().first == onCard) {
                runs[runs.size - 1] = onCard to runs.last().second + 1
            } else {
                runs += onCard to 1
            }
        }
        // A title is a few pixels of glyphs, a row of a card is at least 48 dp.
        val cleaned = mutableListOf<Pair<Boolean, Int>>()
        for (run in runs) {
            if (run.first && run.second < 40) continue
            if (cleaned.isNotEmpty() && cleaned.last().first == run.first) {
                cleaned[cleaned.size - 1] = run.first to cleaned.last().second + run.second
            } else {
                cleaned += run
            }
        }
        return cleaned
    }

    /**
     * Pixels in [from]..[to] whose colour is the theme's `primary`.
     *
     * The theme's own `primary` is read from a swatch painted in the corner of the
     * scene, the way the other checks do it, so the count does not depend on which
     * accent the render was given.
     */
    private fun primaryInk(png: ByteArray, from: Int, to: Int): Int {
        val image = javax.imageio.ImageIO.read(png.inputStream())
        val primary = image.getRGB(2, image.height - 3)
        var count = 0
        for (y in from until minOf(to, image.height)) {
            for (x in 0 until image.width) {
                if (near(image.getRGB(x, y), primary, 24)) count++
            }
        }
        return count
    }

    /** Pixels that differ between two frames of the same size, and the first row one appears on. */
    private data class Diff(val count: Int, val top: Int)

    private fun frameDiff(during: ByteArray, after: ByteArray): Diff {
        val a = javax.imageio.ImageIO.read(during.inputStream())
        val b = javax.imageio.ImageIO.read(after.inputStream())
        var count = 0
        var top = Int.MAX_VALUE
        for (y in 0 until minOf(a.height, b.height)) {
            for (x in 0 until minOf(a.width, b.width)) {
                if (!near(a.getRGB(x, y), b.getRGB(x, y), 12)) {
                    count++
                    if (y < top) top = y
                }
            }
        }
        return Diff(count, if (top == Int.MAX_VALUE) -1 else top)
    }

    private fun near(a: Int, b: Int, threshold: Int = 6): Boolean {
        val dr = kotlin.math.abs(((a shr 16) and 255) - ((b shr 16) and 255))
        val dg = kotlin.math.abs(((a shr 8) and 255) - ((b shr 8) and 255))
        val db = kotlin.math.abs((a and 255) - (b and 255))
        return dr + dg + db <= threshold
    }
}
