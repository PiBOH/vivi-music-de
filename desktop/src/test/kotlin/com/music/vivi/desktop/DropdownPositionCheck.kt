@file:OptIn(ExperimentalTestApi::class)

package com.music.vivi.desktop

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.test.ComposeUiTest
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.runComposeUiTest
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.Dp
import kotlin.math.abs

/**
 * Headless check of where a settings dropdown opens, run with
 *
 *     ./gradlew :desktop:dropdownPositionCheck
 *
 * Every choice in Settings goes through `M3SettingsItem.dropdown`, whose menu is
 * a Material 3 `DropdownMenu` drawn from inside the row's own `Box`. That is
 * meant to anchor the menu to the row, and until this check it was never
 * measured: an `ImageComposeScene` paints no popups, so a still frame of a
 * settings screen cannot show the menu at all.
 *
 * This runs the real thing. `runComposeUiTest` composes a scene in which the
 * popup exists and its semantics can be read, so a click on the row opens the
 * real menu and the menu's own option rows give its bounds next to the row's:
 *
 *  * with room under the row the menu opens **below** it, its first option
 *    starting a menu margin under the row (not at the window corner, not halfway
 *    down the page) and its left edge on the row's left edge;
 *  * with the row near the floor of the scene the same menu opens **above** it,
 *    because a menu that does not fit below is flipped instead of being drawn off
 *    the window.
 *
 * Both are asserted, so a menu anchored to the wrong parent, or one that ignored
 * the space left below, fails the run.
 */
object DropdownPositionCheck {

    /** A rectangle read off the test's semantics tree, in root px. */
    private data class Rect(val left: Float, val top: Float, val right: Float, val bottom: Float)

    private const val WIDTH = 640
    private const val HEIGHT = 900

    /** The menu's own gap from its anchor: 8 dp of margin and 4 dp of padding. */
    private const val MENU_GAP_MAX = 24.0

    private const val OPTION_HEIGHT_MAX = 80.0

    private val options = listOf("one" to "One", "two" to "Two", "three" to "Three")

    @JvmStatic
    fun main(args: Array<String>) {
        val failures = mutableListOf<String>()

        // ---------------------------------------------------- opens under the row
        var row: Rect? = null
        var firstOption: Rect? = null
        var menuLeft: Float? = null
        runComposeUiTest {
            setContent { page(top = 40.dp, title = "Dropdown") }
            onNodeWithText("Dropdown").assertExists()
            row = onNodeWithText("Dropdown").bounds()
            onNodeWithText("Dropdown").performClick()
            waitForIdle()
            // `One` is also the row's own trailing value, so the menu's copy is
            // the one that is not the row: the first option below the row.
            val copies = onAllNodesWithText("One").bounds()
            val opened = copies.filter { it.top > (row?.bottom ?: 0f) }.minByOrNull { it.top }
            if (opened == null) {
                failures += "the menu did not open under the row (no option below ${row?.bottom})"
            } else {
                firstOption = opened
                menuLeft = listOfNotNull(opened.left).first()
            }
        }
        val rowRect = row ?: error("the row did not compose")
        val optionRect = firstOption
        if (optionRect != null) {
            println(
                "under: row=${rowRect.top.toInt()}..${rowRect.bottom.toInt()} left=${rowRect.left.toInt()}, " +
                    "first option=${optionRect.top.toInt()}..${optionRect.bottom.toInt()} left=${optionRect.left.toInt()}",
            )
            val gap = optionRect.top - rowRect.bottom
            if (gap < 0) failures += "the menu must open under the row, it starts ${-gap} px above its bottom"
            if (gap > MENU_GAP_MAX) {
                failures += "the menu must be attached to the row, its first option is ${gap.toInt()} px below it"
            }
            if (abs(optionRect.left - rowRect.left) > 20) {
                failures += "the menu must start at the row's left edge (${rowRect.left.toInt()}), read ${optionRect.left.toInt()}"
            }
            if (optionRect.right < rowRect.left + 120) {
                failures += "the menu must be drawn under the row's own width, read ${optionRect.right.toInt()}"
            }
        }

        // ------------------------------------------- the menu follows its own row
        // A menu that sat at a fixed place would still pass the case above; this
        // one moves the row and reads the move. The menu must move by exactly the
        // row's own shift and keep the same gap and the same left edge.
        var movedRow: Rect? = null
        var movedOption: Rect? = null
        runComposeUiTest {
            setContent { page(top = 300.dp, title = "Moved") }
            onNodeWithText("Moved").assertExists()
            movedRow = onNodeWithText("Moved").bounds()
            onNodeWithText("Moved").performClick()
            waitForIdle()
            val rowBottom = movedRow?.bottom ?: 0f
            movedOption = onAllNodesWithText("One").bounds()
                .filter { it.top > rowBottom }
                .minByOrNull { it.top }
        }
        val movedRowRect = movedRow ?: error("the moved row did not compose")
        val movedOptionRect = movedOption
        if (optionRect == null || movedOptionRect == null) {
            failures += "the moved row's menu did not open"
        } else {
            println(
                "moved: row=${movedRowRect.top.toInt()}..${movedRowRect.bottom.toInt()}, " +
                    "first option=${movedOptionRect.top.toInt()}..${movedOptionRect.bottom.toInt()} left=${movedOptionRect.left.toInt()}",
            )
            val rowShift = movedRowRect.top - rowRect.top
            val menuShift = movedOptionRect.top - optionRect.top
            if (abs(rowShift - menuShift) > 2) {
                failures += "the menu must follow its row: the row moved ${rowShift.toInt()} px and " +
                    "the menu ${menuShift.toInt()} px"
            }
            if (abs((movedOptionRect.top - movedRowRect.bottom) - (optionRect.top - rowRect.bottom)) > 2) {
                failures += "the menu's gap from the row must not change when the row moves"
            }
        }

        // ------------------------------------------------ flips above near a floor
        var lowRow: Rect? = null
        var lastOption: Rect? = null
        runComposeUiTest {
            // The row is the last thing in the page: three options cannot fit
            // under it inside the scene.
            setContent { page(top = (HEIGHT - 200).dp, title = "Bottom") }
            onNodeWithText("Bottom").assertExists()
            lowRow = onNodeWithText("Bottom").bounds()
            onNodeWithText("Bottom").performClick()
            waitForIdle()
            val nodes = onAllNodesWithText("Three").bounds()
            lastOption = nodes.maxByOrNull { it.top }
        }
        val lowRowRect = lowRow ?: error("the low row did not compose")
        val lastOptionRect = lastOption
        if (lastOptionRect == null) {
            failures += "the menu near the floor did not compose an option at all"
        } else {
            println(
                "flip: row=${lowRowRect.top.toInt()}..${lowRowRect.bottom.toInt()}, " +
                    "last option=${lastOptionRect.top.toInt()}..${lastOptionRect.bottom.toInt()}",
            )
            if (lastOptionRect.bottom > lowRowRect.top + 4) {
                failures += "with no room below, the menu must open above the row " +
                    "(row starts ${lowRowRect.top.toInt()}, last option ends ${lastOptionRect.bottom.toInt()})"
            }
        }

        if (failures.isEmpty()) {
            println()
            println(
                "OK: the menu opens under the row that owns it (first option ${optionRect?.top?.toInt()} " +
                    "against the row's bottom ${rowRect.bottom.toInt()}, left ${optionRect?.left?.toInt()} " +
                    "against ${rowRect.left.toInt()}) and above it when it does not fit below " +
                    "(last option ends ${lastOptionRect?.bottom?.toInt()}, row starts ${lowRowRect.top.toInt()})",
            )
            return
        }
        println()
        println("FAILED:")
        failures.forEach { println("  - $it") }
        throw IllegalStateException("dropdown position check failed (${failures.size} case(s))")
    }

    /** The page every case renders: one dropdown row at [top]. */
    @androidx.compose.runtime.Composable
    private fun page(top: Dp, title: String) {
        AppTheme(mode = ThemeMode.LIGHT, accent = Color(0xFFED5564), language = "en") {
            Box(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.surface)) {
                Column(Modifier.padding(start = 20.dp, top = top).width(600.dp)) {
                    M3SettingsGroup(
                        items = listOf(
                            M3SettingsItem(
                                icon = null,
                                title = { Text(title) },
                                dropdown = M3Dropdown(
                                    value = "One",
                                    options = options,
                                    onSelect = {},
                                ),
                            ),
                        ),
                    )
                }
            }
        }
    }

    /** The bounds of the one node carrying [text], in root px. */
    private fun androidx.compose.ui.test.SemanticsNodeInteraction.bounds(): Rect {
        val r = fetchSemanticsNode().boundsInRoot
        return Rect(r.left, r.top, r.right, r.bottom)
    }

    private fun androidx.compose.ui.test.SemanticsNodeInteractionCollection.bounds(): List<Rect> =
        fetchSemanticsNodes().map { node ->
            val r = node.boundsInRoot
            Rect(r.left, r.top, r.right, r.bottom)
        }
}
