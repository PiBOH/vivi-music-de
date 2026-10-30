@file:OptIn(ExperimentalTestApi::class)

package com.music.vivi.desktop

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.ImageComposeScene
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.state.ToggleableState
import androidx.compose.ui.test.ComposeUiTest
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.SemanticsNodeInteraction
import androidx.compose.ui.test.hasClickAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.isToggleable
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.runComposeUiTest
import androidx.compose.ui.unit.dp
import org.jetbrains.skia.EncodedImageFormat
import java.awt.image.BufferedImage
import java.io.File
import kotlin.math.abs

/**
 * Headless check of the Backup and restore screen against the preview, run with
 *
 *     ./gradlew :desktop:backupOrderCheck
 *
 * The preview's `backup` list (`js/screens.js`) is the heading; the two actions,
 * each in a block of its own at its label's width (the page keeps two rows there
 * because the app draws a sentence under each); the automatic-backup card with
 * its header inside the card and the two rows that fold with the master switch;
 * and then the page's label with the stored list under it, 12 dp below.
 *
 * The check composes the real screen body - `SettingsSubScreen` with
 * [BackupSection], the composable [SettingsBackupScreen] draws - with two stored
 * backups and with none, and reads: the sequence of texts, the heading 16 dp
 * under the back row, the first action 16 dp under the heading, the cards and the
 * 3 dp seams between their rows, the label 12 dp over the stored card, both
 * states of the master switch with its two folded rows, the arrival frame, and
 * the presses that reach the screen's own callbacks instead of announcing
 * themselves.
 */
object BackupOrderCheck {

    private const val WIDTH = 640

    private const val HEIGHT = 1400

    /** A rectangle read off the test's semantics tree, in root px. */
    private data class Rect(val left: Float, val top: Float, val right: Float, val bottom: Float)

    /** A run of card fill down the measured column, in pixels from the top. */
    private data class Band(val top: Int, val bottom: Int) {
        val height: Int get() = bottom - top + 1
    }

    private fun string(key: String) = Localization.get("en", key)

    /** Two stored backups, as the app's own list would carry them. */
    private val stored = listOf(
        StoredBackup(
            File("auto_backup_weekly_20261014_193712.vivide.backup"),
            "2026-10-14 19:37",
            "backup_type_weekly",
            4_300_000L,
        ),
        StoredBackup(
            File("auto_backup_before_update_20261007_080000.vivide.backup"),
            "2026-10-07 08:00",
            "backup_type_before_update",
            4_000_000L,
        ),
    )

    /** The line under a stored row: the type the page's own second line names, and the size. */
    private fun detail(backup: StoredBackup) =
        string(backup.typeKey) + " \u00b7 " + formatBytes(backup.bytes)

    /**
     * The page's own texts, in its own order.
     *
     * `backup` is the title, Backup, Restore, the card header, the master row and
     * its sentence, Weekly backup, Before each update, the label, and then each
     * stored row with its own line and its Restore. The app's two sentences under
     * the buttons and its Delete beside each row are not on the page, and the
     * before-update row is the app's existing translated key.
     */
    private val previewOrder = listOf(
        "\u2039 Back",
        string("backup_restore"),
        string("action_backup"),
        string("action_restore"),
        string("auto_backup"),
        string("auto_backup"),
        string("automatic_backup_desc"),
        string("weekly_backup"),
        "Before each update",
        string("stored_backups"),
    ) + stored.flatMap { listOf(it.date, detail(it), string("action_restore")) }

    @JvmStatic
    fun main(args: Array<String>) {
        val failures = mutableListOf<String>()

        var read: List<String> = emptyList()
        var back: Rect? = null
        var title: Rect? = null
        var backupButton: Rect? = null
        var label: Rect? = null

        runComposeUiTest {
            setContent { screen(auto = true) }
            waitForIdle()

            read = texts()
            back = onNodeWithText("\u2039 Back").rectOrNull()
            title = firstWithText(string("backup_restore")).rectOrNull()
            backupButton = onAllNodes(hasText(string("action_backup")) and hasClickAction())[0].rectOrNull()
            label = onNodeWithText(string("stored_backups")).rectOrNull()
        }

        // ------------------------------------------------------ the sequence
        // The two notes under the buttons and the two Delete buttons are the
        // app's own, so they come out of the comparison; the before-update row is
        // the app's key, which has said "Backup before update" since before the
        // page.
        val mapped = read
            .filterNot { it == string("backup_desc") || it == string("restore_desc") || it == string("delete") }
            .map { if (it == string("backup_before_update")) "Before each update" else it }
        if (mapped == previewOrder) {
            println("order: ${read.size} texts, the preview's ${previewOrder.size} in its own order")
        } else {
            failures += "the screen's texts are not the preview's: read $mapped"
        }

        // ------------------------------------------------------ the heading
        val t = title
        val b = back
        if (t != null && b != null) {
            val heading = t.top - b.bottom
            println("heading: back ends ${b.bottom.toInt()}, title starts ${t.top.toInt()} (gap ${heading.toInt()})")
            if (abs(heading - 16f) > 2f) {
                failures += "the heading must stand 16 dp under the back row, read a gap of ${heading.toInt()}"
            }
        } else {
            failures += "the heading could not be measured"
        }

        // ------------------------------------------------------ the cards
        // Two cards each row drawn as its own surface: the automatic backup's
        // three rows and the stored list's two. The column is read clear of the
        // buttons and of the page's own padding, so only the cards' fill counts.
        val blocks = bands(settledFrame(auto = true))
        println("cards: ${blocks.size} band(s) ${blocks.map { "${it.top}..${it.bottom}" }}")
        if (blocks.size != 5) {
            failures += "the page is three switch rows and two stored rows (5 rows), read ${blocks.size}"
        }
        val first = backupButton
        if (t != null && first != null) {
            val under = first.top - t.bottom
            println(
                "actions: title ends ${t.bottom.toInt()}, Backup starts ${first.top.toInt()} " +
                    "(gap ${under.toInt()}, the button's 4 dp touch target included)",
            )
            // The page's 16 dp, read from the button's semantics bounds, which are
            // 4 dp wider than its fill on every side (`minimumInteractiveComponentSize`).
            if (under < 15f || under > 21f) {
                failures += "the first action must stand the page's 16 dp under the heading, read ${under.toInt()}"
            }
            // The action sits at its label's width, not across the pane: a filled
            // button the width of the screen reads as a banner (the page's own note).
            val wide = first.right - first.left
            println("actions: Backup is ${wide.toInt()} px wide of the ${WIDTH - 28} dp pane")
            if (wide > 200f) {
                failures += "an action must sit at its label's width, read ${wide.toInt()} px of the pane"
            }
        }
        if (blocks.size == 5) {
            val seams = listOf(
                blocks[1].top - blocks[0].bottom - 1,
                blocks[2].top - blocks[1].bottom - 1,
                blocks[4].top - blocks[3].bottom - 1,
            )
            println("seams: ${seams.map { "$it dp" }} inside the two cards")
            if (seams.any { it < 2 || it > 4 }) {
                failures += "the rows of a card must stand 3 dp apart, read $seams"
            }
            val between = blocks[3].top - blocks[2].bottom - 1
            println("cards: the automatic card ends ${blocks[2].bottom}, the stored card starts ${blocks[3].top} (label between, $between)")
            if (between < 40) {
                failures += "the label must stand between the two cards, read the cards $between apart"
            }
            val l = label
            if (l != null) {
                val gap = blocks[3].top - l.bottom
                val over = l.top - blocks[2].bottom - 1
                println(
                    "label: the card ends ${blocks[2].bottom}, the label ${l.top.toInt()}..${l.bottom.toInt()} " +
                        "($over over it), the stored card starts ${blocks[3].top} (gap ${gap.toInt()})",
                )
                // The page's 12 dp between the label and the card, over the label's
                // own 6 dp of bottom padding: a text's bounds are its line, the
                // padding around it is the space the two gaps are measured through.
                if (abs(gap - 18f) > 3f) {
                    failures += "the stored card must stand the page's 12 dp under its label, read ${gap.toInt()}"
                }
            } else {
                failures += "the label could not be measured"
            }
        }

        // ------------------------------------------------------ the two states
        // The master switch is the card's first row; the two under it belong to
        // it, so they fold away with it - the page's `visibleWhen`.
        var state: ToggleableState? = null
        var onRows: Int = 0
        runComposeUiTest {
            setContent { screen(auto = true) }
            waitForIdle()
            state = onAllNodes(isToggleable())[0].fetchSemanticsNode().config[SemanticsProperties.ToggleableState]
            onRows = onAllNodes(isToggleable()).fetchSemanticsNodes().size
        }
        var offState: ToggleableState? = null
        var offRows: Int = 0
        runComposeUiTest {
            setContent { screen(auto = false) }
            waitForIdle()
            offState = onAllNodes(isToggleable())[0].fetchSemanticsNode().config[SemanticsProperties.ToggleableState]
            offRows = onAllNodes(isToggleable()).fetchSemanticsNodes().size
        }
        val offBands = bands(settledFrame(auto = false))
        println("states: off reads $offState with $offRows switch(es) and ${offBands.size} row(s), on reads $state with $onRows")
        if (state != ToggleableState.On) {
            failures += "with the preference on the master switch must read on, read $state"
        }
        if (offState != ToggleableState.Off) {
            failures += "with the preference off the master switch must read off, read $offState"
        }
        if (onRows != 3 || offRows != 1) {
            failures += "the card is one switch with its two dependants (3 on, 1 off), read $onRows on and $offRows off"
        }
        if (offBands.size != 3) {
            failures += "with the two rows folded away the page is one switch row and two stored rows (3), read ${offBands.size}"
        }

        // ------------------------------------------------------ the empty truth
        val emptyBands = bands(emptyFrame())
        val emptyText = emptyTexts()
        println("empty: ${emptyBands.size} band(s) and the list's own sentence ${emptyText == string("backups_empty")}")
        if (emptyText != string("backups_empty")) {
            failures += "an empty list must say so instead of drawing rows, read $emptyText"
        }
        if (emptyBands.size != 3) {
            failures += "an empty list is the switch card's three rows and no card under it (3), read ${emptyBands.size}"
        }

        // ------------------------------------------------------ the entrance
        val arrival = bands(firstFrame())
        println("entrance: ${arrival.size} band(s) on the arrival frame against ${blocks.size} once settled")
        if (arrival.size >= blocks.size) {
            failures += "both cards fade up as the screen arrives: the arrival frame must not show all of them, read ${arrival.size}"
        }

        // ------------------------------------------------------ the presses act
        var asked: Boolean? = null
        var backedUp = 0
        var pickedFile = 0
        var restored: StoredBackup? = null
        var deleted: StoredBackup? = null
        runComposeUiTest {
            setContent {
                screen(
                    auto = false,
                    onToggle = { asked = it },
                    onBackup = { backedUp++ },
                    onRestoreFile = { pickedFile++ },
                    onRestoreStored = { restored = it },
                    onDeleteStored = { deleted = it },
                )
            }
            waitForIdle()
            // The page's whole row turns its switch, not only the knob.
            onAllNodes(hasText(string("auto_backup")) and hasClickAction())[0].performClick()
            // The two actions: the top button and the file picker it opens.
            onAllNodes(hasText(string("action_backup")) and hasClickAction())[0].performClick()
            // The rows' Restore, in the stored list's trailing slot: the first one is
            // the top button's own, the next two are the rows'.
            val restores = onAllNodes(hasText(string("action_restore")) and hasClickAction())
            restores[0].performClick()
            restores[1].performClick()
            onAllNodes(hasText(string("delete")) and hasClickAction())[0].performClick()
            waitForIdle()
            println(
                "press: the switch asked for $asked, Backup ran $backedUp time(s), " +
                    "Restore ran $pickedFile time(s), the row's Restore asked for ${restored?.file?.name}, " +
                    "Delete asked for ${deleted?.file?.name}",
            )
        }
        if (asked != true) {
            failures += "a press on the master row must ask for the other state, read $asked"
        }
        if (backedUp != 1) {
            failures += "the Backup button must run the backup once, read $backedUp"
        }
        if (pickedFile != 1) {
            failures += "the Restore button must open the picker once, read $pickedFile"
        }
        if (restored != stored[0]) {
            failures += "the row's Restore must ask for that row's file, read ${restored?.file?.name}"
        }
        if (deleted != stored[0]) {
            failures += "the row's Delete must ask for that row's file, read ${deleted?.file?.name}"
        }

        // ------------------------------------------------------ the real screen
        // The section is the body; this is the screen itself, with its own state
        // (the settings file and the automatic backups on this machine), so the
        // wrapper is read too and not only the composable the check drives.
        var realTexts: List<String> = emptyList()
        runComposeUiTest {
            setContent {
                AppTheme(mode = ThemeMode.LIGHT, accent = Color(0xFFED5564), language = "en") {
                    Box(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.surface)) {
                        Box(Modifier.size(WIDTH.dp, HEIGHT.dp)) {
                            SettingsBackupScreen(language = "en", onBack = {})
                        }
                    }
                }
            }
            waitForIdle()
            realTexts = texts()
        }
        val realHead = realTexts.take(2)
        println(
            "real: ${realTexts.size} text(s) from SettingsBackupScreen, head $realHead, " +
                "stored list says its own truth ${realTexts.lastOrNull() == string("backups_empty") || realTexts.size > 20}",
        )
        if (realHead != listOf("\u2039 Back", string("backup_restore"))) {
            failures += "the app's own screen must open on the back row and its title, read $realHead"
        }
        if (realTexts.size < 12) {
            failures += "the app's own screen must draw the page's blocks, read ${realTexts.size} text(s)"
        }

        if (failures.isEmpty()) {
            println()
            println(
                "OK: Backup and restore is the preview's - the heading 16 dp under the back row, the two actions " +
                    "each in its own block at its label's width with its sentence, the automatic-backup card with " +
                    "its header inside it and its two rows folding with the master switch, the page's label 12 dp " +
                    "over the stored list, rows 3 dp apart, an empty list that says so, both states read from the " +
                    "preference, and every button reaching the screen's own action",
            )
            return
        }
        println()
        println("FAILED:")
        failures.forEach { println("  - $it") }
        throw IllegalStateException("backup order check failed (${failures.size} case(s))")
    }

    /** The first node carrying a text, in document order. */
    private fun ComposeUiTest.firstWithText(text: String): SemanticsNodeInteraction =
        onAllNodesWithText(text)[0]

    /** Every text of the composed screen, in document order. */
    private fun ComposeUiTest.texts(): List<String> {
        val read = mutableListOf<String>()
        fun walk(node: androidx.compose.ui.semantics.SemanticsNode) {
            val config = node.config
            if (config.contains(SemanticsProperties.Text)) {
                config[SemanticsProperties.Text].forEach { read += it.text }
            }
            node.children.forEach { walk(it) }
        }
        walk(onRoot().fetchSemanticsNode())
        return read
    }

    /** The sentence an empty list shows, read from the composed screen. */
    private fun emptyTexts(): String? {
        var found: String? = null
        runComposeUiTest {
            setContent { screen(auto = true, backups = emptyList()) }
            waitForIdle()
            found = texts().lastOrNull()
        }
        return found
    }

    /**
     * The screen body the app's own screen draws: the back row through
     * [SettingsSubScreen], then [BackupSection] over the state the caller holds.
     *
     * [arriving] is what the screen host provides on a navigation: the cards fade
     * up when one has just put its screen on show, which is the page's
     * `screen--enter`.
     */
    @Composable
    private fun screen(
        arriving: Boolean = false,
        auto: Boolean,
        backups: List<StoredBackup> = stored,
        onToggle: (Boolean) -> Unit = {},
        onBackup: () -> Unit = {},
        onRestoreFile: () -> Unit = {},
        onRestoreStored: (StoredBackup) -> Unit = {},
        onDeleteStored: (StoredBackup) -> Unit = {},
    ) {
        AppTheme(mode = ThemeMode.LIGHT, accent = Color(0xFFED5564), language = "en") {
            Box(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.surface)) {
                Box(Modifier.size(WIDTH.dp, HEIGHT.dp)) {
                    CompositionLocalProvider(LocalScreenArrival provides arriving) {
                        SettingsSubScreen(language = "en", onBack = {}) {
                            BackupSection(
                                language = "en",
                                backups = backups,
                                autoBackupEnabled = auto,
                                autoBackupWeekly = true,
                                autoBackupBeforeUpdate = true,
                                onToggleAutoBackup = onToggle,
                                onToggleWeekly = {},
                                onToggleBeforeUpdate = {},
                                onBackup = onBackup,
                                onRestoreFile = onRestoreFile,
                                onRestoreStored = onRestoreStored,
                                onDeleteStored = onDeleteStored,
                            )
                        }
                    }
                }
            }
        }
    }

    @OptIn(ExperimentalComposeUiApi::class)
    private fun scene(
        arriving: Boolean = false,
        auto: Boolean,
        backups: List<StoredBackup> = stored,
    ): ImageComposeScene = ImageComposeScene(WIDTH, HEIGHT) {
        screen(arriving = arriving, auto = auto, backups = backups)
    }

    /** The frame the screen arrives on. */
    private fun firstFrame(): BufferedImage {
        val scene = scene(arriving = true, auto = true)
        try {
            return png(scene.render(1_000_000_000L))
        } finally {
            scene.close()
        }
    }

    /** The same screen once an entrance has had its time. */
    private fun settledFrame(auto: Boolean): BufferedImage {
        val scene = scene(arriving = true, auto = auto)
        try {
            scene.render(1_000_000_000L)
            scene.render(1_100_000_000L)
            return png(scene.render(1_600_000_000L))
        } finally {
            scene.close()
        }
    }

    /** The same screen with no stored backup at all. */
    private fun emptyFrame(): BufferedImage {
        val scene = scene(arriving = false, auto = true, backups = emptyList())
        try {
            scene.render(1_000_000_000L)
            return png(scene.render(1_600_000_000L))
        } finally {
            scene.close()
        }
    }

    /**
     * The runs of card fill down a column clear of the buttons, the icons and the
     * padding: each row of a card is a surface of its own, so a row is one run and
     * the 3 dp seam between two of them is the gap that parts them.
     */
    private fun bands(image: BufferedImage): List<Band> {
        val page = image.getRGB(2, 2)
        val column = image.width - 30
        val runs = mutableListOf<Band>()
        var start = -1
        for (y in 0 until image.height) {
            val filled = !near(image.getRGB(column, y), page)
            if (filled && start < 0) start = y
            if (!filled && start >= 0) {
                if (y - start >= 20) runs += Band(start, y - 1)
                start = -1
            }
        }
        if (start >= 0 && image.height - start >= 20) runs += Band(start, image.height - 1)
        return runs
    }

    private fun near(a: Int, b: Int): Boolean {
        val dr = abs(((a shr 16) and 255) - ((b shr 16) and 255))
        val dg = abs(((a shr 8) and 255) - ((b shr 8) and 255))
        val db = abs((a and 255) - (b and 255))
        return dr + dg + db <= 6
    }

    private fun png(image: org.jetbrains.skia.Image): BufferedImage =
        image.encodeToData(EncodedImageFormat.PNG)?.bytes
            ?.let { javax.imageio.ImageIO.read(it.inputStream()) }
            ?: error("the scene produced no image")

    private fun SemanticsNodeInteraction.rectOrNull(): Rect? {
        val node = try {
            fetchSemanticsNode()
        } catch (missing: AssertionError) {
            return null
        }
        val r = node.boundsInRoot
        return Rect(r.left, r.top, r.right, r.bottom)
    }
}
