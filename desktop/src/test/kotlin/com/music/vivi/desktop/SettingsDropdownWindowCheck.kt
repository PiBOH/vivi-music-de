@file:OptIn(ExperimentalTestApi::class)

package com.music.vivi.desktop

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.HighQuality
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.ModelTraining
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.SmartToy
import androidx.compose.material.icons.filled.Translate
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.SemanticsNode
import androidx.compose.ui.test.DesktopComposeUiTest
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.hasClickAction
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.runDesktopComposeUiTest
import androidx.compose.ui.unit.dp
import kotlin.math.abs

/**
 * Window-sized check of where every settings dropdown opens, run with
 *
 *     ./gradlew :desktop:settingsDropdownWindowCheck
 *
 * `:desktop:dropdownPositionCheck` measures one row shape. This one composes
 * **every** settings dropdown the DE draws - all twenty-eight, each in the shape
 * its own screen gives it - on Compose's desktop test harness at the size of a
 * real window (900x1000 px), presses the row and reads the menu back from the
 * composition.
 *
 * What it asserts, per case:
 *
 *  * the row opens a menu at all, with an entry per option;
 *  * the menu opens **under the row that was pressed** - its first entry a menu
 *    margin under the row's bottom edge, never at the window corner or halfway
 *    down the page - and is **right-aligned to the value** that was pressed
 *    (the row is the width of the page, so a menu hung off its left edge opened
 *    at the window's margin, nowhere near the value);
 *  * the menu is the row's width of menu (**at least 200 dp wide**), so a menu
 *    collapsed into a stripe fails;
 *  * near the floor of the window the same menu opens **above** the row;
 *  * pressing an entry **applies the choice**: the row then shows the pressed
 *    entry and the menu is gone.
 *
 * The rows themselves are the product's: `M3SettingsDropdownItem` and
 * `M3SettingsDropdownCard` (a loose row and a card) and `M3SettingsItem` with a
 * `M3Dropdown`, which is how the Player & mini-player design screen carries its
 * five choices in one card. (The two menus outside that row - the AI screen's
 * trailing choice and Device sync's connection method - are not rows of a
 * settings card and are not cases.)
 *
 * Earlier this check pressed the row with the system pointer in a `JFrame` and
 * read the menu out of a screen capture. It measured the machine instead of the
 * menu: on a renderer that falls back from DirectX (`Failed to create DirectX12
 * device`) the same row opened its menu on one press and not on the next, while
 * the row's hover state showed the pointer had arrived; the window also left
 * bands of the page unpainted (pure black), and the capture of a window with
 * rounded corners pulled in the desktop along its bottom edge. Reading the
 * composition instead is exact - the menu's real bounds, its real entries, the
 * real response to pressing one - and the popup that is measured is the same
 * popup the screens draw.
 *
 * What this cannot see is that the popup is *painted* on a physical screen; that
 * part of the old check was its own, and it is the part the renderer above
 * defeated. It is listed as open in `TODO.md`.
 */
object SettingsDropdownWindowCheck {

    private const val WIDTH = 900
    private const val HEIGHT = 1000

    /** A menu margin of 8 dp plus its own 4 dp top padding. */
    private const val MENU_GAP_MAX = 24f

    /**
     * How far the menu's right edge may sit from the row's own right edge: the
     * row keeps 17 dp of trailing padding after the value, so the menu ends
     * there, and a shadow can pull the read edge a little further.
     */
    private const val MENU_RIGHT_SLACK = 44f

    private const val MENU_MIN_WIDTH = 200f

    private enum class Kind { ITEM, CARD, INLINE }

    /**
     * One dropdown as its screen draws it. [title] is the row's label and
     * [value] the choice it shows; neither is asserted, they are what makes the
     * case readable. [options] is the real list where it is public
     * (`LanguageCodeToName`, `CountryCodeToName`), and otherwise its length is
     * what the screen offers, which is what decides how tall the menu is.
     */
    private data class Case(
        val name: String,
        val kind: Kind,
        val title: String,
        val value: String,
        val options: List<String>,
        val icon: ImageVector? = Icons.Filled.Tune,
        /** Drawn at the bottom of the window, where a menu has no room below it. */
        val nearFloor: Boolean = false,
    )

    private val cases: List<Case> = listOf(
        // Appearance -> Transitions (M3SettingsDropdownItem, a loose row).
        Case("transitions: transition style", Kind.ITEM, "Transition style", "Fade", listOf("Fade", "Slide"), Icons.Filled.AutoAwesome),
        Case("transitions: animation speed", Kind.ITEM, "Animation speed", "Normal", listOf("Slow", "Normal", "Fast"), Icons.Filled.Settings),
        // Appearance -> Canvas.
        Case("canvas: source", Kind.ITEM, "Canvas source", "Auto", listOf("Auto", "YouTube Music", "Local file"), Icons.Filled.AutoAwesome),
        // Appearance -> Display density and grid (two loose rows).
        Case("density: scale", Kind.ITEM, "Display density", "100%", listOf("70%", "80%", "90%", "100%", "110%", "120%"), Icons.Filled.Settings),
        Case("density: grid item size", Kind.ITEM, "Grid item size", "Medium", listOf("Small", "Medium", "Large", "Extra large"), Icons.Filled.Settings),
        // Appearance -> Intro (two loose rows).
        Case("intro: style", Kind.ITEM, "Intro style", "Logo and tagline", listOf("Logo", "Logo and name", "Logo and tagline"), Icons.Filled.AutoAwesome),
        Case("intro: background", Kind.ITEM, "Intro background", "Gradient", listOf("Gradient", "Glow", "Dark"), Icons.Filled.Palette),
        // Content (two dropdown cards, one of them 48 languages long).
        Case("content: language", Kind.CARD, "Content language", "System default", LanguageCodeToName.values.toList(), Icons.Filled.Language),
        Case("content: country", Kind.CARD, "Content country", "System default", CountryCodeToName.values.toList(), Icons.Filled.Language),
        // Lyrics (two loose rows).
        Case("lyrics: text position", Kind.ITEM, "Lyrics text position", "Center", lyricsLabels(), Icons.Filled.Tune),
        Case("lyrics: animation style", Kind.ITEM, "Lyrics animation style", "Slide", listOf("Slide", "Fade", "Scale", "None"), Icons.Filled.AutoAwesome),
        // Updates (two loose rows).
        Case("updates: interval", Kind.ITEM, "Update check interval", "Daily", listOf("Every hour", "Every 6 hours", "Every 12 hours", "Daily", "Weekly"), Icons.Filled.Settings),
        Case("updates: source", Kind.ITEM, "Update source", "GitHub", listOf("GitHub", "Official repository"), Icons.Filled.Settings),
        // Notifications (two loose rows).
        Case("notifications: mode", Kind.ITEM, "Notification mode", "Main window", listOf("Main window", "Native"), Icons.Filled.Settings),
        Case("notifications: duration", Kind.ITEM, "Notification duration", "5s", listOf("3s", "5s", "8s", "10s"), Icons.Filled.Settings),
        // Player & mini-player design (five choices, all rows of cards).
        Case("player design: design", Kind.INLINE, "Design", "Classic", listOf("Classic", "Modern", "Compact", "Vinyl"), Icons.Filled.Tune),
        Case("player design: background", Kind.INLINE, "Background", "Gradient", listOf("Gradient", "Solid", "None"), Icons.Filled.Palette),
        Case("player design: mini player design", Kind.INLINE, "Mini player design", "Default", listOf("Default", "Compact"), Icons.Filled.Tune),
        Case("player design: mini player background", Kind.INLINE, "Mini player background", "Default", listOf("Default", "Gradient", "Solid"), Icons.Filled.Palette),
        Case("player design: slider style", Kind.INLINE, "Slider style", "Slim", listOf("Slim", "Thick"), Icons.Filled.Tune),
        // Player & audio -> audio quality, the stream cache and the history
        // duration: rows of the player card.
        Case("player & audio: audio quality", Kind.INLINE, "Audio quality", "Auto", listOf("Auto", "High", "Low"), Icons.Filled.HighQuality),
        Case("player & audio: stream cache", Kind.INLINE, "Stream cache (minutes)", "10 min", listOf("5 min", "10 min", "30 min", "60 min", "Forever"), Icons.Filled.Bolt),
        Case("player & audio: history duration", Kind.INLINE, "History duration", "30 s", listOf("10 s", "30 s", "60 s", "5 min", "Forever"), Icons.Filled.History),
        // AI Lyrics Translation: the provider, the model, DeepL's formality and
        // the translation mode, all rows of the screen's cards.
        Case("ai: provider", Kind.INLINE, "Provider", "OpenRouter", listOf("OpenRouter", "OpenAI", "Perplexity", "Claude", "Gemini", "XAi", "Mistral", "DeepL", "Custom"), Icons.Filled.SmartToy),
        Case("ai: model", Kind.INLINE, "Model", "gemini-2.5-flash-lite", listOf("gemini-2.5-flash-lite", "gemini-2.5-flash", "gemini-2.5-pro", "Custom model"), Icons.Filled.ModelTraining),
        Case("ai: formality", Kind.INLINE, "Formality", "Default", listOf("Default", "More Formal", "Less Formal"), Icons.Filled.Tune),
        Case("ai: translation mode", Kind.INLINE, "Translation Mode", "Translation", listOf("Translation", "Transcription"), Icons.Filled.Translate),
        // The same row at the floor of the window: it has to open upwards.
        Case("flip: at the floor", Kind.ITEM, "Transition style", "Fade", listOf("Fade", "Slide"), Icons.Filled.AutoAwesome, nearFloor = true),
    )

    /**
     * The choice the harness's row shows: `null` until an entry is pressed, so a
     * case starts on the value its own screen shows and the row, after the press,
     * is the row saying what was chosen.
     */
    private val chosen = mutableStateOf<String?>(null)

    /** The row every case renders, at the top of the page or at its floor. */
    @Composable
    private fun page(case: Case) {
        // One instance for the whole run: `M3Dropdown` is a data class, and the
        // row's `remember(dropdown)` keeps the menu open only while the choice it
        // carries compares equal - a lambda built at each composition would make
        // every recomposition a different choice and close the menu as it opened.
        val onSelect = remember { { key: String -> chosen.value = key } }
        AppTheme(mode = ThemeMode.LIGHT, accent = Color(0xFFED5564), language = "en") {
            Box(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.surface)) {
                Column(Modifier.fillMaxHeight()) {
                    if (case.nearFloor) {
                        Spacer(Modifier.weight(1f))
                        // The row keeps 10 dp of page under it at the floor.
                        Spacer(Modifier.height(10.dp))
                    }
                    Box(Modifier.fillMaxWidth().padding(start = 20.dp, end = 20.dp)) {
                        DropdownRow(case, chosen.value ?: case.value, onSelect)
                    }
                }
            }
        }
    }

    /** The row in the shape its own screen draws it. */
    @Composable
    private fun DropdownRow(case: Case, value: String, onSelect: (String) -> Unit) {
        val options = case.options.map { it to it }
        when (case.kind) {
            Kind.ITEM -> Box(Modifier.fillMaxWidth().padding(top = 40.dp)) {
                M3SettingsDropdownItem(
                    icon = case.icon,
                    title = case.title,
                    value = value,
                    options = options,
                    onSelect = onSelect,
                )
            }
            Kind.CARD -> Box(Modifier.fillMaxWidth().padding(top = 40.dp)) {
                M3SettingsDropdownCard(
                    icon = case.icon,
                    title = case.title,
                    value = value,
                    options = options,
                    onSelect = onSelect,
                )
            }
            Kind.INLINE -> Box(Modifier.fillMaxWidth().padding(top = 40.dp)) {
                M3SettingsGroup(
                    items = listOf(
                        M3SettingsItem(
                            icon = case.icon,
                            title = { Text(case.title) },
                            dropdown = M3Dropdown(value = value, options = options, onSelect = onSelect),
                        ),
                    ),
                )
            }
        }
    }

    /** A rectangle read off a semantics node, in root px. */
    private data class Rect(val left: Float, val top: Float, val right: Float, val bottom: Float) {
        val width get() = right - left
    }

    @JvmStatic
    fun main(args: Array<String>) {
        // The desktop harness builds a real Skia surface: with no display it
        // verifies nothing, and says so instead of claiming a pass.
        if (java.awt.GraphicsEnvironment.isHeadless()) {
            println("NOT RUN: no display in this environment, so nothing was measured")
            return
        }
        // An argument narrows the run to the cases whose name contains it, for
        // chasing one row without measuring the other twenty-seven.
        val only = args.firstOrNull()
        val selected = if (only == null) cases else cases.filter { it.name.contains(only, ignoreCase = true) }
        if (selected.isEmpty()) error("no case matches '$only'")
        val failures = mutableListOf<String>()
        val active = mutableStateOf(selected.first())

        runDesktopComposeUiTest(WIDTH, HEIGHT) {
            setContent { page(active.value) }
            waitForIdle()
            println("window: ${WIDTH}x$HEIGHT, ${selected.size} row(s)")


            selected.forEachIndexed { index, case ->
                active.value = case
                chosen.value = null
                waitForIdle()
                val row = rowOf(case)
                if (row == null) {
                    failures += "${case.name}: the row is not in the window"
                    return@forEachIndexed
                }
                val rowRect = row.rect()

                // The press. A row that does not open a menu fails here.
                onNodeWithText(case.title).performClick()
                waitForIdle()
                val menu = menuOf(case, rowRect)
                if (menu == null) {
                    failures += "${case.name}: pressing the row opened no menu " +
                        if (case.nearFloor) "above it" else "under it"
                    return@forEachIndexed
                }

                val gap = with(density) { (menu.first.top - rowRect.bottom).toDp().value }
                val rightGap = with(density) { (rowRect.right - menu.first.right).toDp().value }
                val width = with(density) { menu.first.width.toDp().value }
                println(
                    "${case.name}\n" +
                        "    row ${rowRect.left.enDesc()}..${rowRect.right.enDesc()} bottom ${rowRect.bottom.enDesc()}, " +
                        "menu ${menu.first.left.enDesc()}..${menu.first.right.enDesc()} " +
                        "${menu.first.top.enDesc()}..${menu.first.bottom.enDesc()} (${menu.second.size} entries), " +
                        "gap ${"%.1f".format(gap)} dp, right ${"%.1f".format(rightGap)} dp, " +
                        "width ${"%.1f".format(width)} dp",
                )

                if (case.nearFloor) {
                    val belowRow = with(density) { (menu.first.bottom - rowRect.top).toDp().value }
                    if (belowRow > 4f) {
                        failures += "${case.name}: with no room below, the menu must open above the row " +
                            "(the row starts at ${rowRect.top.enDesc()}, the menu ends ${menu.first.bottom.enDesc()})"
                    }
                } else {
                    if (gap < 0f) failures += "${case.name}: the menu starts ${-gap} dp above the row's bottom"
                    if (gap > MENU_GAP_MAX) {
                        failures += "${case.name}: the menu is not attached to the row, its first entry is " +
                            "${"%.1f".format(gap)} dp below it"
                    }
                    if (abs(rightGap) > MENU_RIGHT_SLACK) {
                        failures += "${case.name}: the menu must be right-aligned to the row's value " +
                            "(the row ends at ${rowRect.right.enDesc()}), read ${menu.first.right.enDesc()}"
                    }
                    if (width < MENU_MIN_WIDTH) {
                        failures += "${case.name}: the menu must be at least $MENU_MIN_WIDTH dp wide, " +
                            "read ${"%.1f".format(width)} dp"
                    }
                }

                // The entry press: the entry picked is not the one the row already
                // shows, so a value that follows can only come from that press. A
                // list longer than the window (the 48 languages, the countries)
                // draws only the entries that fit, so the pick is made from those.
                val current = chosen.value ?: case.value
                val pick = menu.second.firstOrNull { it.first != current }
                if (pick == null) {
                    failures += "${case.name}: the menu offers no entry other than '$current'"
                    return@forEachIndexed
                }
                pick.second.performClick()
                waitForIdle()
                if (chosen.value != pick.first) {
                    failures += "${case.name}: pressing the entry '${pick.first}' applied " +
                        "'${chosen.value}' instead"
                }
                if (menuOf(case, rowRect) != null) {
                    failures += "${case.name}: pressing an entry of the menu left it open"
                }
                val shows = rowOf(case)?.rect()?.let { row ->
                    nodesWithText(pick.first).any { node -> row.contains(node.boundsInRoot) }
                }
                if (shows != true) {
                    failures += "${case.name}: after pressing the entry '${pick.first}' the row " +
                        "does not show it"
                }
            }
        }

        println()
        if (failures.isEmpty()) {
            println(
                "OK: ${selected.count { !it.nearFloor }} settings dropdown(s) open their menu under the row " +
                    "that was pressed, right-aligned to its value and at least $MENU_MIN_WIDTH dp wide, " +
                    "${selected.count { it.nearFloor }} at the floor of a window opens above it, and pressing an " +
                    "entry applies that entry to the row",
            )
            return
        }
        println("FAILED:")
        failures.forEach { println("  - $it") }
        throw IllegalStateException("settings dropdown window check failed (${failures.size} case(s))")
    }

    // ------------------------------------------------------------------ window

    /** The node of the row the case draws: the clickable that holds its label. */
    private fun DesktopComposeUiTest.rowOf(case: Case): SemanticsNode? {
        val labels = onAllNodesWithText(case.title).fetchSemanticsNodes()
        if (labels.isEmpty()) return null
        val label = labels.first().boundsInRoot
        val clickables = onAllNodes(hasClickAction()).fetchSemanticsNodes()
        return clickables.firstOrNull { node ->
            val b = node.boundsInRoot
            label.left >= b.left - 1f && label.right <= b.right + 1f &&
                label.top >= b.top - 1f && label.bottom <= b.bottom + 1f
        }
    }

    /**
     * The menu of [case] on screen: the box around it, and its entries.
     *
     * The entries are the option labels drawn outside the row - the row itself
     * shows one of them as its value, and that one is not the menu. A menu taller
     * than the window draws only the entries that fit, so the list is what is on
     * screen, not the whole list the row offers.
     */
    private fun DesktopComposeUiTest.menuOf(
        case: Case,
        row: Rect,
    ): Pair<Rect, List<Pair<String, androidx.compose.ui.test.SemanticsNodeInteraction>>>? {
        val entries = case.options.mapNotNull { label ->
            onFirstOutside(label, row, case.nearFloor)?.let { label to it }
        }
        if (entries.isEmpty()) return null
        val rects = entries.map { it.second.fetchSemanticsNode().boundsInRoot }
        val box = Rect(
            rects.minOf { it.left },
            rects.minOf { it.top },
            rects.maxOf { it.right },
            rects.maxOf { it.bottom },
        )
        return box to entries
    }

    private fun DesktopComposeUiTest.nodesWithText(text: String): List<SemanticsNode> =
        onAllNodesWithText(text).fetchSemanticsNodes()

    /** The first node carrying [text] that is on the menu's side of [row]. */
    private fun DesktopComposeUiTest.onFirstOutside(
        text: String,
        row: Rect,
        above: Boolean,
    ): androidx.compose.ui.test.SemanticsNodeInteraction? {
        val copies = onAllNodesWithText(text)
        val index = copies.fetchSemanticsNodes().indexOfFirst { node ->
            val b = node.boundsInRoot
            if (above) b.bottom <= row.top + 1f else b.top >= row.bottom - 1f
        }
        return if (index < 0) null else copies[index]
    }

    private fun SemanticsNode.rect(): Rect {
        val b = boundsInRoot
        return Rect(b.left, b.top, b.right, b.bottom)
    }

    private fun Rect.contains(other: androidx.compose.ui.geometry.Rect): Boolean =
        other.left >= left - 1f && other.right <= right + 1f && other.top >= top - 1f && other.bottom <= bottom + 1f

    private fun Float.enDesc(): String = toInt().toString()

    /** The labels the lyrics position row offers, from the app's own enum. */
    private fun lyricsLabels(): List<String> = LyricsPosition.entries.map { it.name }
}
