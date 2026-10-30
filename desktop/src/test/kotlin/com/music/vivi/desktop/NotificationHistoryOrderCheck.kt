@file:OptIn(ExperimentalTestApi::class)

package com.music.vivi.desktop

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.ImageComposeScene
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.test.ComposeUiTest
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.SemanticsNodeInteraction
import androidx.compose.ui.test.hasClickAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.runComposeUiTest
import androidx.compose.ui.unit.dp
import org.jetbrains.skia.EncodedImageFormat
import java.awt.image.BufferedImage
import kotlin.math.abs

/**
 * Headless check of the Notification history screen against the preview, run
 *
 *     ./gradlew :desktop:notificationHistoryOrderCheck
 *
 * The preview's `notificationsHistory` list (`js/screens.js`) is the title and one
 * card of rows, each carrying its own mark, its title and a second line, and
 * nothing else: no sentence under the heading and no action of its own. The app
 * has two more facts the page's two demo rows cannot hold - the message the
 * notification carried and the mode that showed it - so its row is the page's row
 * with the message where the page puts its second line and the time and the mode
 * in the trailing column, and it carries the one action the screen can take.
 *
 * The check composes the real `NotificationHistorySection` with a list of its own,
 * so neither state depends on the machine's own history:
 *
 *  * with entries: the sequence of texts, the heading 16 dp under the back row,
 *    the card of rows on the 3 dp seams, the cleared action 16 dp under it, the
 *    entrance, and a press on that action that really empties the list;
 *  * empty: the page's title and the app's own sentence, no card and no action -
 *    an action over an empty list cannot do what it says.
 */
object NotificationHistoryOrderCheck {

    private const val WIDTH = 640

    private const val HEIGHT = 900

    /** A rectangle read off the test's semantics tree, in root px. */
    private data class Rect(val left: Float, val top: Float, val right: Float, val bottom: Float)

    /** A run of card fill down the measured column, in pixels from the top. */
    private data class Band(val top: Int, val bottom: Int) {
        val height: Int get() = bottom - top + 1
    }

    private fun string(key: String) = Localization.get("en", key)

    /** Two notifications of the two modes, so a row of each is read. */
    private val records = listOf(
        NotificationRecord(1_700_000_000_000L, "Update available", "Version 1.55.0 is ready to install.", "in_app"),
        NotificationRecord(1_699_900_000_000L, "Device sync", "The phone is paired.", "native"),
    )

    /** Every text the list should show, in the order it should show them. */
    private val expected = listOf(
        "\u2039 Back",
        string("notification_history"),
        records[0].title,
        records[0].message,
        formatNotificationTime(records[0].timestamp),
        string("notification_main_window"),
        records[1].title,
        records[1].message,
        formatNotificationTime(records[1].timestamp),
        string("notification_native"),
        string("clear_history"),
    )

    @JvmStatic
    fun main(args: Array<String>) {
        val failures = mutableListOf<String>()

        var read: List<String> = emptyList()
        var back: Rect? = null
        var title: Rect? = null
        var action: Rect? = null

        runComposeUiTest {
            setContent { content() }
            waitForIdle()

            read = texts()
            back = onNodeWithText("\u2039 Back").rectOrNull()
            title = onNodeWithText(string("notification_history")).rectOrNull()
            action = onNode(hasClickAction() and hasText(string("clear_history"))).rectOrNull()
        }

        // ------------------------------------------------------ the sequence
        if (read == expected) {
            println("order: ${read.size} texts, the preview's own sequence")
        } else {
            failures += "the screen's texts are not the preview's: read $read"
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

        // ------------------------------------------------------ the card
        val bands = cards(settledFrame())
        println("cards: ${bands.size} band(s) ${bands.map { "${it.top}..${it.bottom}" }}")
        if (bands.size != records.size) {
            failures += "the page must be one card of ${records.size} rows, read ${bands.size} band(s)"
        } else {
            val seams = (1 until bands.size).map { bands[it].top - bands[it - 1].bottom - 1 }
            println("seams: $seams")
            seams.forEach { seam ->
                if (abs(seam - 3) > 1) {
                    failures += "the card's rows must be joined by the 3 dp seam, read $seam"
                }
            }
        }
        if (t != null && bands.isNotEmpty()) {
            val underTitle = bands[0].top - t.bottom
            println("heading to card: title ends ${t.bottom.toInt()}, card starts ${bands[0].top} (gap ${underTitle.toInt()})")
            if (abs(underTitle - 16f) > 3f) {
                failures += "the card must stand 16 dp under the heading, read ${underTitle.toInt()}"
            }
        }

        // ------------------------------------------------------ the action
        val a = action
        if (a != null && bands.isNotEmpty()) {
            val under = a.top - bands.last().bottom - 1
            println("action: top ${a.top.toInt()} against the card's ${bands.last().bottom} (gap $under)")
            // The spacer is the page's 16 dp; the button carries the interactive
            // padding around its own box, so the painted gap reads a few px larger.
            if (under < 14 || under > 24) {
                failures += "the action must stand 16 dp under the card, read $under"
            }
        } else {
            failures += "the clear action is missing from a list with entries"
        }

        // ------------------------------------------------------ the entrance
        val arrival = cards(firstFrame())
        println("entrance: ${arrival.size} band(s) on the arrival frame against ${bands.size} once settled")
        if (bands.size == records.size && arrival.isNotEmpty()) {
            failures += "the card must fade up as the screen arrives: the arrival frame already paints " +
                "${arrival.size} band(s)"
        }

        // ------------------------------------------------------ the empty state
        var emptyTexts: List<String> = emptyList()
        var emptyBands = 0
        runComposeUiTest {
            setContent { content(history = emptyList()) }
            waitForIdle()
            emptyTexts = texts()
        }
        emptyBands = cards(settledFrame(history = emptyList())).size
        val emptyExpected = listOf("\u2039 Back", string("notification_history"), string("notification_history_empty"))
        println("empty: ${emptyTexts.size} texts, $emptyBands band(s), ${emptyTexts.getOrNull(2)}")
        if (emptyTexts != emptyExpected) {
            failures += "an empty list must say the truth and nothing else, read $emptyTexts"
        }
        if (emptyBands != 0) {
            failures += "an empty list must not draw a card, read $emptyBands band(s)"
        }
        runComposeUiTest {
            setContent { content(history = emptyList()) }
            waitForIdle()
            val actions = onAllNodesWithText(string("clear_history")).fetchSemanticsNodes()
            if (actions.isNotEmpty()) {
                failures += "an empty list must not offer an action that cannot do anything"
            }
        }

        // ------------------------------------------------------ the action acts
        var pressed = false
        runComposeUiTest {
            setContent {
                var history by remember { mutableStateOf(records) }
                statefulContent(
                    history = history,
                    onClear = {
                        pressed = true
                        history = emptyList()
                    },
                )
            }
            waitForIdle()
            onNode(hasClickAction() and hasText(string("clear_history"))).performClick()
            waitForIdle()
            val after = texts()
            println("clear: pressed $pressed, the list after is ${after.size} text(s)")
            if (!pressed) {
                failures += "the clear action did not reach the screen's own clear"
            }
            if (after != emptyExpected) {
                failures += "a cleared list must show the empty sentence, read $after"
            }
        }

        if (failures.isEmpty()) {
            println()
            println(
                "OK: Notification history is the preview's - the heading 16 dp under the back row and the card " +
                    "16 dp under it, one card of rows on the 3 dp seam each carrying its message and its own " +
                    "time and mode, the action 16 dp under the card only while there is something to clear, " +
                    "and a list with nothing in it saying so instead of drawing rows",
            )
            return
        }
        println()
        println("FAILED:")
        failures.forEach { println("  - $it") }
        throw IllegalStateException("notification history order check failed (${failures.size} case(s))")
    }

    /** Every text of the composed screen, in document order. */
    private fun ComposeUiTest.texts(): List<String> {
        val read = mutableListOf<String>()
        fun walk(node: androidx.compose.ui.semantics.SemanticsNode) {
            val config = node.config
            if (config.contains(androidx.compose.ui.semantics.SemanticsProperties.Text)) {
                config[androidx.compose.ui.semantics.SemanticsProperties.Text].forEach { read += it.text }
            }
            node.children.forEach { walk(it) }
        }
        walk(onRoot().fetchSemanticsNode())
        return read
    }

    /**
     * The screen's own body for the off-screen scene and for the state arms.
     *
     * [arriving] is what the app's own screen host provides on a navigation: a
     * card only fades up when one has just put its screen on show, which is the
     * preview's `screen--enter`.
     */
    @androidx.compose.runtime.Composable
    private fun content(arriving: Boolean = false, history: List<NotificationRecord> = records) {
        AppTheme(mode = ThemeMode.LIGHT, accent = Color(0xFFED5564), language = "en") {
            Box(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.surface)) {
                Box(Modifier.size(WIDTH.dp, HEIGHT.dp)) {
                    CompositionLocalProvider(LocalScreenArrival provides arriving) {
                        SettingsSubScreen(language = "en", onBack = {}) {
                            NotificationHistorySection(language = "en", history = history, onClear = {})
                        }
                    }
                }
            }
        }
    }

    /** The same body over a list the caller holds, for the press's own arm. */
    @androidx.compose.runtime.Composable
    private fun statefulContent(history: List<NotificationRecord>, onClear: () -> Unit) {
        AppTheme(mode = ThemeMode.LIGHT, accent = Color(0xFFED5564), language = "en") {
            Box(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.surface)) {
                Box(Modifier.size(WIDTH.dp, HEIGHT.dp)) {
                    SettingsSubScreen(language = "en", onBack = {}) {
                        NotificationHistorySection(language = "en", history = history, onClear = onClear)
                    }
                }
            }
        }
    }

    @OptIn(ExperimentalComposeUiApi::class)
    private fun scene(arriving: Boolean = false, history: List<NotificationRecord> = records): ImageComposeScene =
        ImageComposeScene(WIDTH, HEIGHT) { content(arriving = arriving, history = history) }

    /** The frame the screen arrives on. */
    private fun firstFrame(): BufferedImage {
        val scene = scene(arriving = true)
        try {
            return png(scene.render(1_000_000_000L))
        } finally {
            scene.close()
        }
    }

    /** The same screen once an entrance has had its time. */
    private fun settledFrame(history: List<NotificationRecord> = records): BufferedImage {
        val scene = scene(arriving = true, history = history)
        try {
            scene.render(1_000_000_000L)
            scene.render(1_100_000_000L)
            return png(scene.render(1_600_000_000L))
        } finally {
            scene.close()
        }
    }

    /** The runs of card fill down the column, one per row of the card. */
    private fun cards(image: BufferedImage): List<Band> {
        val page = image.getRGB(2, 2)
        val runs = mutableListOf<Band>()
        var start = -1
        for (y in 0 until image.height) {
            val filled = !near(image.getRGB(40, y), page)
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
