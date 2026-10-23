@file:OptIn(ExperimentalComposeUiApi::class)

package com.music.vivi.desktop

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.awt.ComposePanel
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import java.awt.BorderLayout
import java.awt.GraphicsEnvironment
import java.awt.Rectangle
import java.awt.Robot
import java.awt.event.InputEvent
import java.awt.image.BufferedImage
import javax.swing.JFrame
import javax.swing.SwingUtilities
import kotlin.math.abs

/**
 * Real-window check of where every settings dropdown opens, run with
 *
 *     ./gradlew :desktop:settingsDropdownWindowCheck
 *
 * `:desktop:dropdownPositionCheck` measures the same behaviour on the ui-test
 * harness, whose scene is off-screen. This one opens an actual OS window - a
 * `ComposePanel` in a `JFrame`, on the machine's own display - presses the row
 * with the real pointer (`java.awt.Robot`), captures the screen and reads the
 * menu back from the pixels, so what is verified is what a user sees.
 *
 * Every settings dropdown drawn as a row is a case: one per configuration, in
 * the shape its screen gives it - `M3SettingsDropdownItem` and
 * `M3SettingsDropdownCard` (a loose row and a card), and `M3SettingsItem` with a
 * `M3Dropdown`, which is how the Player & mini-player design screen carries its
 * five choices in one card. (The two menus outside that row - the AI screen's
 * trailing choice and Device sync's connection method - are not rows of a
 * settings card and are not cases.) A case renders that row on its own, presses the
 * control at the row's right end (the value and its chevron), and asserts:
 *
 *  * the menu opens **under the row that was pressed**, its top 0..24 px below
 *    the row's bottom edge and its left edge on the row's;
 *  * the menu is the row's own width of menu (at least 200 px wide), so a menu
 *    collapsed into a stripe fails;
 *  * near the floor of the window the same menu opens **above** the row.
 *
 * The row's bottom edge is marked by the harness with a 2 px magenta rule drawn
 * under it (`drawWithContent`, no layout change), because that is the one
 * measurement the pixels cannot otherwise name. The menu is then the region
 * that changed below that rule between the frame before the press and the frame
 * after it.
 *
 * A check that needs a display prints `NOT RUN` and returns without claiming
 * anything: it has not seen a window, so it has verified nothing.
 */
object SettingsDropdownWindowCheck {

    private const val WIDTH = 900
    private const val HEIGHT = 1000

    /** A menu margin of 8 dp plus its own 4 dp top padding. */
    private const val MENU_GAP_MAX = 24

    /** How far the menu's shadow may pull its read left edge from the row's. */
    private const val MENU_LEFT_SLACK = 20

    private const val MENU_MIN_WIDTH = 200

    private enum class Kind { ITEM, CARD, INLINE }

    /**
     * One dropdown as its screen draws it. [title] is the row's label and
     * [value] the choice it shows; neither is measured, they are what makes the
     * case readable in the output and in the window. [options] is the real list
     * where it is public (`LanguageCodeToName`, `CountryCodeToName`), and
     * otherwise its length is what the screen offers, which is what decides how
     * tall the menu is.
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
        // Player & audio -> audio quality (a loose row).
        Case("player & audio: audio quality", Kind.ITEM, "Audio quality", "Auto", listOf("Auto", "High", "Low"), Icons.Filled.GraphicEq),
        // The same row at the floor of the window: it has to open upwards.
        Case("flip: at the floor", Kind.ITEM, "Transition style", "Fade", listOf("Fade", "Slide"), Icons.Filled.AutoAwesome, nearFloor = true),
    )

    /** The row every case renders, with a magenta rule under its bottom edge. */
    @Composable
    private fun page(case: Case) {
        AppTheme(mode = ThemeMode.LIGHT, accent = Color(0xFFED5564), language = "en") {
            // 1 dp = 1 px, so the pixels the window is captured in and the
            // layout of the row are the same numbers whatever the display's
            // scaling is.
            CompositionLocalProvider(LocalDensity provides Density(1f, 1f)) {
                Box(Modifier.fillMaxWidth().fillMaxHeight().background(MaterialTheme.colorScheme.surface)) {
                    Column(Modifier.fillMaxHeight()) {
                        if (case.nearFloor) {
                            Spacer(Modifier.weight(1f))
                            // The rule has to be on screen: the page keeps its
                            // last 10 px below the row.
                            Spacer(Modifier.height(10.dp))
                        }
                        Box(
                            Modifier
                                .fillMaxWidth()
                                .padding(start = 20.dp, end = 20.dp)
                                .drawWithContent {
                                    drawContent()
                                    drawRect(
                                        color = Color(0xFFFF00FF),
                                        topLeft = Offset(0f, size.height - 2f),
                                        size = Size(size.width, 2f),
                                    )
                                },
                        ) {
                            DropdownRow(case)
                        }
                    }
                }
            }
        }
    }

    /** The row in the shape its own screen draws it. */
    @Composable
    private fun DropdownRow(case: Case) {
        val options = case.options.map { it to it }
        when (case.kind) {
            Kind.ITEM -> Box(Modifier.fillMaxWidth().padding(top = 40.dp)) {
                M3SettingsDropdownItem(
                    icon = case.icon,
                    title = case.title,
                    value = case.value,
                    options = options,
                    onSelect = {},
                )
            }
            Kind.CARD -> Box(Modifier.fillMaxWidth().padding(top = 40.dp)) {
                M3SettingsDropdownCard(
                    icon = case.icon,
                    title = case.title,
                    value = case.value,
                    options = options,
                    onSelect = {},
                )
            }
            Kind.INLINE -> Box(Modifier.fillMaxWidth().padding(top = 40.dp)) {
                M3SettingsGroup(
                    items = listOf(
                        M3SettingsItem(
                            icon = case.icon,
                            title = { Text(case.title) },
                            dropdown = M3Dropdown(value = case.value, options = options, onSelect = {}),
                        ),
                    ),
                )
            }
        }
    }

    @JvmStatic
    fun main(args: Array<String>) {
        if (GraphicsEnvironment.isHeadless()) {
            println("NOT RUN: no display in this environment, so no window was opened and nothing was verified")
            return
        }
        // An argument narrows the run to the cases whose name contains it, for
        // chasing one row without re-measuring the other twenty.
        val only = args.firstOrNull()
        val selected = if (only == null) cases else cases.filter { it.name.contains(only, ignoreCase = true) }
        if (selected.isEmpty()) error("no case matches '$only'")
        val failures = mutableListOf<String>()
        val active = mutableStateOf(selected.first())

        // A `ComposePanel` may only be built on the AWT event dispatch thread.
        var panelHolder: ComposePanel? = null
        var windowHolder: JFrame? = null
        SwingUtilities.invokeAndWait {
            val composePanel = ComposePanel()
            composePanel.setContent { page(active.value) }
            val composeWindow = JFrame("VIVI Music - settings dropdown check")
            composeWindow.contentPane.layout = BorderLayout()
            composeWindow.contentPane.add(composePanel, BorderLayout.CENTER)
            composeWindow.defaultCloseOperation = JFrame.DISPOSE_ON_CLOSE
            composeWindow.setSize(WIDTH, HEIGHT)
            // Centred on the display the machine boots on (the laptop panel), never
            // at a fixed offset: `setLocation(120, 20)` is a point in the whole
            // virtual desktop, so on a desk with a second monitor to the left of
            // the laptop it opened this window on that monitor instead. The
            // centring is clamped to that display's own corner, because the window
            // is 1000 px tall and a laptop panel is often shorter: without the
            // clamp its title bar landed above the screen (measured `at 241,-85`
            // here), and a check whose window is half off screen measures nothing.
            val primary = GraphicsEnvironment.getLocalGraphicsEnvironment()
                .defaultScreenDevice.defaultConfiguration.bounds
            composeWindow.setLocation(
                (primary.x + (primary.width - WIDTH) / 2).coerceAtLeast(primary.x),
                (primary.y + (primary.height - HEIGHT) / 2).coerceAtLeast(primary.y),
            )
            // Nothing may cover the window: the frame is read from the screen.
            composeWindow.isAlwaysOnTop = true
            composeWindow.isVisible = true
            composeWindow.toFront()
            composeWindow.requestFocus()
            panelHolder = composePanel
            windowHolder = composeWindow
        }
        val panel = panelHolder ?: error("the window was not created")
        val window = windowHolder ?: error("the window was not created")
        Thread.sleep(1500)

        val robot = Robot()
        val pointer = java.awt.MouseInfo.getPointerInfo().location
        try {
            val origin = panel.locationOnScreen
            val scale = screenScale(robot, panel)
            println("window: panel ${panel.width}x${panel.height} at ${origin.x},${origin.y}, screen scale ${"%.2f".format(scale)}")
            println()
            // The first press on a window that was just opened activates it
            // instead of reaching the page, so the first case's row is pressed
            // once and thrown away before anything is measured.
            active.value = selected.first()
            settled(robot, panel)
            press(robot, panel, origin, scale, pointer)
            dismiss(robot, panel, origin, scale, pointer)

            selected.forEach { case ->
                active.value = case
                // The page is read only once two frames in a row are the same,
                // so a capture never holds the previous case, or a frame of the
                // composition still moving.
                var before = settled(robot, panel)
                val marker = marker(before)
                if (marker == null) {
                    failures += "${case.name}: the row's bottom edge is not on screen"
                    return@forEach
                }
                val clickX = origin.x + ((marker.left + marker.right) / 2 + (marker.right - marker.left) / 4) / scale
                val clickY = origin.y + (marker.y - 14) / scale
                // The window has to own the pointer for the menu to stay open,
                // and a menu that is still closing can swallow a press: two
                // presses at most, the second one after the page is still.
                var menu: Region? = null
                var after: BufferedImage = before
                for (attempt in 0..1) {
                    focus(window, panel)
                    robot.mouseMove(clickX.toInt(), clickY.toInt())
                    robot.mousePress(InputEvent.BUTTON1_DOWN_MASK)
                    robot.mouseRelease(InputEvent.BUTTON1_DOWN_MASK)
                    // The pointer itself is drawn in a screen capture, so it is
                    // parked outside the window before the frame is read.
                    robot.mouseMove(pointer.x, pointer.y)
                    after = settled(robot, panel)
                    // A menu that fits is read under the row; one that has no
                    // room below is read above it, where it has to be.
                    menu = if (case.nearFloor) {
                        changedBetween(before, after, 0, marker.y - 3)
                    } else {
                        changedBetween(before, after, marker.y + 3, after.height)
                    }
                    if (menu != null) break
                    if (attempt == 0) {
                        dismiss(robot, panel, origin, scale, pointer)
                        before = settled(robot, panel)
                    }
                }
                val opened = menu
                if (opened == null) {
                    val anywhere = changedBetween(before, after, 0, after.height)
                    save(before, "${case.name}-before")
                    save(after, "${case.name}-after")
                    failures += "${case.name}: pressing the row opened no menu " +
                        (if (case.nearFloor) "above it" else "under it") +
                        " (press at ${clickX.toInt()},${clickY.toInt()}, marker ${marker.left}..${marker.right} bottom ${marker.y}; " +
                        (anywhere?.let { "the frame changed at ${it.left}..${it.right} ${it.top}..${it.bottom})" } ?: "the frame did not change at all)")
                    dismiss(robot, panel, origin, scale, pointer)
                    return@forEach
                }
                val gap = opened.top - marker.y
                println(
                    "${case.name}\n" +
                        "    row ${marker.left}..${marker.right} bottom ${marker.y}, " +
                        "menu ${opened.left}..${opened.right} ${opened.top}..${opened.bottom}, gap $gap",
                )
                if (case.nearFloor) {
                    if (opened.bottom > marker.y - 4) {
                        failures += "${case.name}: with no room below, the menu must open above the row " +
                            "(row bottom ${marker.y}, menu ends ${opened.bottom})"
                    }
                } else {
                    if (gap < 0) failures += "${case.name}: the menu starts ${-gap} px above the row's bottom"
                    if (gap > MENU_GAP_MAX) {
                        failures += "${case.name}: the menu is not attached to the row, its first pixel is $gap px below it"
                    }
                    if (abs(opened.left - marker.left) > MENU_LEFT_SLACK) {
                        failures += "${case.name}: the menu must start at the row's left edge (${marker.left}), read ${opened.left}"
                    }
                    if (opened.right - opened.left < MENU_MIN_WIDTH) {
                        failures += "${case.name}: the menu must be at least $MENU_MIN_WIDTH px wide, read ${opened.right - opened.left}"
                    }
                }
                dismiss(robot, panel, origin, scale, pointer)
            }
        } finally {
            robot.mouseMove(pointer.x, pointer.y)
            SwingUtilities.invokeAndWait { window.dispose() }
        }

        println()
        if (failures.isEmpty()) {
            println(
                "OK: ${selected.count { !it.nearFloor }} settings dropdown(s) open their menu under the row " +
                    "that was pressed, on the row's left edge, ${selected.count { it.nearFloor }} at the floor " +
                    "of a real window opens above it",
            )
            return
        }
        println("FAILED:")
        failures.forEach { println("  - $it") }
        throw IllegalStateException("settings dropdown window check failed (${failures.size} case(s))")
    }

    // ------------------------------------------------------------------ window

    /** Device pixels per AWT unit, read off the panel's own capture. */
    private fun screenScale(robot: Robot, panel: ComposePanel): Double {
        val shot = capture(robot, panel)
        return shot.width.toDouble() / panel.width
    }

    private fun capture(robot: Robot, panel: ComposePanel): BufferedImage {
        val origin = panel.locationOnScreen
        return robot.createScreenCapture(Rectangle(origin.x, origin.y, panel.width, panel.height))
    }

    /** Gives the window the pointer back: a menu that loses it does not stay. */
    private fun focus(window: JFrame, panel: ComposePanel) {
        SwingUtilities.invokeAndWait {
            window.toFront()
            window.requestFocus()
            panel.requestFocusInWindow()
        }
        Thread.sleep(200)
    }

    /** Presses the row of the case on screen, for the warm-up. */
    private fun press(robot: Robot, panel: ComposePanel, origin: java.awt.Point, scale: Double, pointer: java.awt.Point) {
        val frame = settled(robot, panel)
        val marker = marker(frame) ?: return
        val clickX = origin.x + ((marker.left + marker.right) / 2 + (marker.right - marker.left) / 4) / scale
        val clickY = origin.y + (marker.y - 14) / scale
        robot.mouseMove(clickX.toInt(), clickY.toInt())
        robot.mousePress(InputEvent.BUTTON1_DOWN_MASK)
        robot.mouseRelease(InputEvent.BUTTON1_DOWN_MASK)
        robot.mouseMove(pointer.x, pointer.y)
        Thread.sleep(400)
    }

    /**
     * The frame the page has come to rest on: two captures in a row that are
     * the same, 150 ms apart, so a composition, the menu's fade or a ripple is
     * never read halfway through.
     */
    private fun settled(robot: Robot, panel: ComposePanel): BufferedImage {
        var previous = capture(robot, panel)
        repeat(20) {
            Thread.sleep(150)
            val next = capture(robot, panel)
            if (!anyDiff(previous, next)) return next
            previous = next
        }
        return previous
    }

    /** Whether two frames differ anywhere, sampled so a frame check is cheap. */
    private fun anyDiff(a: BufferedImage, b: BufferedImage): Boolean {
        for (y in 0 until minOf(a.height, b.height) step 2) {
            for (x in 0 until minOf(a.width, b.width) step 2) {
                if (differs(a.getRGB(x, y), b.getRGB(x, y))) return true
            }
        }
        return false
    }

    /**
     * Closes an open menu by pressing empty page under the row, so the window
     * keeps the focus a menu needs: an Escape key leaves the popup owner in a
     * state where the next menu does not open at all.
     */
    private fun dismiss(robot: Robot, panel: ComposePanel, origin: java.awt.Point, scale: Double, pointer: java.awt.Point) {
        robot.mouseMove((origin.x + 40 * scale).toInt(), (origin.y + panel.height - 30).toInt())
        robot.mousePress(InputEvent.BUTTON1_DOWN_MASK)
        robot.mouseRelease(InputEvent.BUTTON1_DOWN_MASK)
        robot.mouseMove(pointer.x, pointer.y)
        // The menu is a popup of its own while it fades out, and a press under
        // it is swallowed: the next case waits for the page to be still.
        Thread.sleep(700)
        settled(robot, panel)
    }

    // ------------------------------------------------------------------ pixels

    private data class Marker(val left: Int, val right: Int, val y: Int)
    private data class Region(val left: Int, val right: Int, val top: Int, val bottom: Int)

    /** The magenta rule under the row, read from its middle column. */
    private fun marker(image: BufferedImage): Marker? {
        for (y in 0 until image.height) {
            if (!magenta(image.getRGB(image.width / 2, y))) continue
            var left = image.width / 2
            while (left > 0 && magenta(image.getRGB(left - 1, y))) left--
            var right = image.width / 2
            while (right < image.width - 1 && magenta(image.getRGB(right + 1, y))) right++
            // Keep the lowest of the two rule rows, so the row's bottom is the
            // edge the menu has to clear.
            var bottom = y
            while (bottom + 1 < image.height && magenta(image.getRGB(image.width / 2, bottom + 1))) bottom++
            return Marker(left, right, bottom)
        }
        return null
    }

    private fun magenta(rgb: Int): Boolean {
        val r = (rgb shr 16) and 255
        val g = (rgb shr 8) and 255
        val b = rgb and 255
        return r > 200 && g < 90 && b > 200
    }

    /** The bounding box of what changed between [fromY] and [toY] in two frames. */
    private fun changedBetween(before: BufferedImage, after: BufferedImage, fromY: Int, toY: Int): Region? {
        var left = Int.MAX_VALUE
        var right = -1
        var top = Int.MAX_VALUE
        var bottom = -1
        val yStart = fromY.coerceAtLeast(0)
        val yEnd = toY.coerceAtMost(minOf(before.height, after.height))
        for (y in yStart until yEnd) {
            for (x in 0 until minOf(before.width, after.width)) {
                if (!differs(before.getRGB(x, y), after.getRGB(x, y))) continue
                if (x < left) left = x
                if (x > right) right = x
                if (y < top) top = y
                if (y > bottom) bottom = y
            }
        }
        return if (right < 0) null else Region(left, right, top, bottom)
    }

    private fun differs(a: Int, b: Int): Boolean {
        val dr = abs(((a shr 16) and 255) - ((b shr 16) and 255))
        val dg = abs(((a shr 8) and 255) - ((b shr 8) and 255))
        val db = abs((a and 255) - (b and 255))
        return dr + dg + db > 24
    }

    /** The labels the lyrics position row offers, from the app's own enum. */
    private fun lyricsLabels(): List<String> = LyricsPosition.entries.map { it.name }

    /** Writes a frame under `.ignore/windowcheck/` for a case that failed. */
    private fun save(image: BufferedImage, name: String) {
        val dir = java.io.File(".ignore/windowcheck").apply { mkdirs() }
        val file = java.io.File(dir, name.replace(Regex("[^A-Za-z0-9]+"), "_") + ".png")
        javax.imageio.ImageIO.write(image, "png", file)
        println("    frame: ${file.path}")
    }
}
