@file:OptIn(ExperimentalComposeUiApi::class)

package com.music.vivi.desktop

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.ImageComposeScene
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * Headless check of how Compose Desktop routes a pointer press when a
 * full-window surface with its own pointer input sits BEHIND the interface.
 *
 *     ./gradlew :desktop:framelessHitTestCheck
 *
 * Why this exists. The window is drawn without an OS title bar, so VIVI has to
 * provide the drag surface itself. `WindowDraggableArea` starts moving the
 * window on the POINTER DOWN and then follows every mouse move until release:
 * it never waits for a drag gesture and it never consumes the event (both
 * verified in the shipped bytecode). So wherever that surface is hit, a click
 * also begins a window move, and any jitter while pressing a button nudges the
 * window. The drag surface must therefore be hit ONLY where the interface has
 * nothing interactive: it is placed behind everything as the first child of the
 * root box.
 *
 * That placement is only safe if Compose stops hit testing at the frontmost
 * node, i.e. a button drawn in front receives the press and the surface behind
 * it does not. This check renders both cases off screen and counts the presses
 * each layer received. It is the difference between "the window can be dragged
 * from its empty top edge" and "every button click drags the window".
 *
 * Case 1 (press on a button, surface behind): the button must fire and the
 * surface must NOT see the press. Hit testing stops at the frontmost layer, so
 * a button protects the area it covers.
 * Case 2 (press on empty space, surface behind): the surface must see it, which
 * is what makes the empty parts of the window draggable.
 * Case 3 (press inside a strip that is drawn IN FRONT): the strip takes it and
 * the surface no longer does. Same rule as case 1 from the other side, and the
 * reason the surface is placed behind: in front it would swallow the presses of
 * whatever it covers.
 */
object FramelessHitTestCheck {

    private class Counts {
        var surfacePresses = 0
        var surfaceDrags = 0
        var clicks = 0
    }

    @JvmStatic
    fun main(args: Array<String>) {
        val failures = mutableListOf<String>()

        val onButton = run(fullWindow = true, pressAt = 40f to 40f, buttonSize = 80.dp)
        println("== press on a button drawn in front of the drag surface")
        println("   button clicks=${onButton.clicks}  surface presses=${onButton.surfacePresses}")
        if (onButton.clicks != 1) failures += "a press on the button did not click it"
        if (onButton.surfacePresses != 0) {
            failures += "the surface behind received the press too (${onButton.surfacePresses}), " +
                "so every button click would start a window move"
        }

        val onEmpty = run(buttonSize = 0.dp, pressAt = 120f to 120f)
        println("== press on empty space with the full-window surface behind")
        println("   surface presses=${onEmpty.surfacePresses}")
        if (onEmpty.surfacePresses != 1) failures += "the full-window surface did not receive the press"

        val band = run(band = true, buttonSize = 0.dp, pressAt = 120f to 14f)
        println("== press inside a 28 dp band drawn in front of the full-window surface")
        println("   surface presses=${band.surfacePresses}")
        if (band.surfacePresses != 0) {
            failures += "a layer in front did not shield the surface behind it (${band.surfacePresses})"
        }

        if (failures.isEmpty()) {
            println()
            println("OK: a drag surface behind the interface is hit only where nothing interactive is in front")
            return
        }
        println()
        println("FAILED:")
        failures.forEach { println("  - $it") }
        throw IllegalStateException("frameless hit test failed (${failures.size} case(s))")
    }

    /**
     * Renders the same layer stack the app uses: the drag surface first (so it
     * is behind), then the interface on top.
     *
     * @param fullWindow the surface covers the whole window.
     * @param band adds a 28 dp strip drawn in front, as a height-limited
     *   alternative would.
     */
    private fun run(
        fullWindow: Boolean = true,
        band: Boolean = false,
        buttonSize: Dp,
        pressAt: Pair<Float, Float>,
    ): Counts {
        val counts = Counts()
        ImageComposeScene(width = 240, height = 240) {
            Box(Modifier.fillMaxSize()) {
                if (fullWindow) {
                    Box(
                        Modifier
                            .fillMaxSize()
                            .pointerInput(Unit) {
                                awaitPointerEventScope { awaitPointerEvents { counts.surfacePresses++ } }
                            }
                    )
                }
                if (band) {
                    // An unrelated pointer-input layer in front of the surface,
                    // deliberately not counting anything: the question is only
                    // whether the surface BEHIND it still sees the press.
                    Box(
                        Modifier
                            .fillMaxWidth()
                            .height(28.dp)
                            .background(Color(0x22000000))
                            .pointerInput(Unit) { awaitPointerEventScope { awaitPointerEvents {} } },
                    )
                }
                if (buttonSize > 0.dp) {
                    Box(
                        Modifier
                            .align(Alignment.TopStart)
                            .size(buttonSize)
                            .background(Color(0x33666666))
                            .clickable { counts.clicks++ },
                    ) {
                        Text("button")
                    }
                }
            }
        }.use { scene ->
            val (x, y) = pressAt
            scene.sendPointerEvent(PointerEventType.Press, Offset(x, y))
            scene.sendPointerEvent(PointerEventType.Release, Offset(x, y))
        }
        return counts
    }

    /**
     * Counts one press and one drag on the surface, the two things
     * `WindowDraggableArea` reacts to (it starts moving on the press).
     */
    private suspend fun androidx.compose.ui.input.pointer.AwaitPointerEventScope.awaitPointerEvents(
        onPress: () -> Unit,
    ) {
        while (true) {
            val event = awaitPointerEvent()
            if (event.type == PointerEventType.Press) onPress()
        }
    }

    private inline fun <T> ImageComposeScene.use(block: (ImageComposeScene) -> T): T =
        try {
            block(this)
        } finally {
            close()
        }
}
