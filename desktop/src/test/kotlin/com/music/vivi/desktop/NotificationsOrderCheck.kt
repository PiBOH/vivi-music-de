@file:OptIn(ExperimentalTestApi::class)

package com.music.vivi.desktop

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.ImageComposeScene
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.test.ComposeUiTest
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.SemanticsNodeInteraction
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.runComposeUiTest
import androidx.compose.ui.unit.dp
import org.jetbrains.skia.EncodedImageFormat
import java.awt.image.BufferedImage
import kotlin.math.abs

/**
 * Headless check of the Notifications screen against the preview, run with
 *
 *     ./gradlew :desktop:notificationsOrderCheck
 *
 * The preview's `notifications` list (`js/screens.js`) is five blocks: the title,
 * the mode choice, the sentence that says what the choice means, one card of
 * three rows and the action that proves notifications arrive. Nothing here has a
 * new order to invent, which is exactly why it is read instead of looked at: a
 * value written twice, a row moved above its card or a button the width of the
 * page all look plausible in a still frame.
 *
 * Two harnesses, for the two kinds of fact:
 *
 *  * **the ui-test one** composes the real `SettingsNotificationsScreen` and reads
 *    the semantics tree, so the page's own text is the input: every text of the
 *    screen, in document order, must be the preview's own sequence (`expected`
 *    below, with the app's one extra row named in it); the heading must stand
 *    16 dp under the back row (`.md-section-title` after `.back-row`'s 8 dp), the
 *    sentence 8 dp under the mode card and 4 dp in from its edge (`.md-note`), the
 *    remaining cards 16 dp apart (`.md-group`), the action 16 dp under the last
 *    card and at its label's width (`.md-button-row`, and `.md-button` which is
 *    never as wide as its row);
 *  * **the pixel one** renders the same screen into an off-screen
 *    `ImageComposeScene` twice - the frame the screen arrives on and the one it
 *    settles on - because an entrance is invisible to a semantics tree: with
 *    Animations on the card is not painted in the first frame (the group fades up
 *    8 dp from nothing, the preview's `.screen--enter .md-group`) and with the
 *    switch off the two frames are the same.
 */
object NotificationsOrderCheck {

    private const val WIDTH = 640

    private const val HEIGHT = 1200

    /** A rectangle read off the test's semantics tree, in root px. */
    private data class Rect(val left: Float, val top: Float, val right: Float, val bottom: Float)

    /**
     * Every text the screen should show, in the order it should show them.
     *
     * The preview's five blocks in its own order, with the app's own duration row
     * - and its sentence - between the mode's sentence and the card: the preview
     * has no duration (it is a desktop option), and naming it here keeps the
     * sequence complete rather than filtered, so a second extra row fails.
     */
    private val expected = listOf(
        "\u2039 Back",
        "Notifications",
        "Notification mode",
        "Main window",
        "Choose how app notifications (updates, device sync, and more) are shown.",
        "In-app notification duration",
        "How long an in-app notification stays on screen before disappearing.",
        "5s",
        "Allow notifications",
        "Save notification history",
        "Notification history",
        "Send test notification",
    )

    @JvmStatic
    fun main(args: Array<String>) {
        val failures = mutableListOf<String>()

        var read: List<String> = emptyList()
        var back: Rect? = null
        var title: Rect? = null
        var mode: Rect? = null
        var note: Rect? = null
        var duration: Rect? = null
        var allow: Rect? = null
        var history: Rect? = null
        var button: Rect? = null

        runComposeUiTest {
            screen()
            waitForIdle()

            val texts = mutableListOf<String>()
            fun walk(node: androidx.compose.ui.semantics.SemanticsNode) {
                val config = node.config
                if (config.contains(androidx.compose.ui.semantics.SemanticsProperties.Text)) {
                    config[androidx.compose.ui.semantics.SemanticsProperties.Text]
                        .forEach { texts += it.text }
                }
                node.children.forEach { walk(it) }
            }
            walk(onRoot().fetchSemanticsNode())
            read = texts

            back = onNodeWithText("\u2039 Back").rectOrNull()
            title = onNodeWithText("Notifications").rectOrNull()
            mode = onNodeWithText("Notification mode").rectOrNull()
            note = onNodeWithText(expected[4]).rectOrNull()
            duration = onNodeWithText("In-app notification duration").rectOrNull()
            allow = onNodeWithText("Allow notifications").rectOrNull()
            history = onNodeWithText("Notification history").rectOrNull()
            button = onNodeWithText("Send test notification").rectOrNull()
        }

        // ------------------------------------------------------ the sequence
        if (read == expected) {
            println("blocks: ${read.size} texts, the preview's own sequence (the duration row named in it)")
        } else {
            failures += "the screen's texts are not the preview's: read $read"
        }

        // ------------------------------------------------------ the measures
        // One copy per read: a local the closure above assigns is not smart-cast.
        val t = title
        val b = back
        val m = mode
        val n = note
        val d = duration
        val a = allow
        val h = history
        val act = button

        fun gap(name: String, from: Rect?, to: Rect?, expected: Float, note: String = "") {
            if (from == null || to == null) {
                failures += "$name could not be measured"
                return
            }
            val read = to.top - from.bottom
            println("$name: ends ${from.bottom.toInt()}, next starts ${to.top.toInt()} (gap ${read.toInt()})")
            if (abs(read - expected) > 2f) {
                failures += "$name must stand ${expected.toInt()} dp where the preview puts it, read ${read.toInt()}$note"
            }
        }

        if (t != null && b != null) {
            val heading = t.top - b.bottom
            println("heading: back ends ${b.bottom.toInt()}, title starts ${t.top.toInt()} (gap ${heading.toInt()})")
            if (abs(heading - 16f) > 2f) {
                failures += "the heading must stand 16 dp under the back row, read a gap of ${heading.toInt()}"
            }
        } else {
            failures += "the heading could not be measured"
        }
        if (t != null && n != null) {
            val inset = n.left - t.left
            println("note: left=${n.left.toInt()} against the page's text at ${t.left.toInt()} (inset ${inset.toInt()})")
            if (abs(inset - 4f) > 1f) {
                failures += "the sentence must be inset 4 dp from the page's text, read $inset"
            }
        }
        gap("note", m, n, 8f, " (the mode card to its own sentence)")
        gap("duration", n, d, 16f, " (the app's own row, a card like the others)")
        gap("card", d, a, 16f)
        // The M3 `Button` carries 4 dp of minimum touch target above its 40 dp box,
        // which the preview's plain button does not; the card's edge to the box is
        // 20 px and the box is what is painted.
        gap("action", h, act, 20f)

        if (act == null) {
            failures += "the action could not be measured"
        } else {
            val width = act.right - act.left
            println("action: width ${width.toInt()}, starting at ${act.left.toInt()}")
            if (width > 400f) {
                failures += "the action must be as wide as its label, not as wide as the page: read ${width.toInt()}"
            }
            if (t != null && abs(act.left - t.left) > 1f) {
                failures += "the action must start on the page's own text edge (${t.left.toInt()}), read ${act.left.toInt()}"
            }
        }

        // ------------------------------------------------------ the entrance
        val onFirst = firstFrameInk()
        val onSettled = settledFrameInk()
        println("entrance on: card ink ${onFirst.first} px on the arrival frame, ${onSettled.first} px once settled (top ${onSettled.second})")
        if (onSettled.first < 500) {
            failures += "the card was not found in the settled frame (ink ${onSettled.first}), so the entrance cannot be read"
        } else if (onFirst.first > onSettled.first / 5) {
            failures += "a group must fade up from nothing as the screen arrives: the arrival frame already paints " +
                "${onFirst.first} of the ${onSettled.first} px"
        }

        Animations.enabled = false
        try {
            val offFirst = firstFrameInk()
            val offSettled = settledFrameInk()
            println("entrance off: card ink ${offFirst.first} px on the arrival frame, ${offSettled.first} px once settled")
            if (offSettled.first > 500 && abs(offFirst.first - offSettled.first) > offSettled.first / 20) {
                failures += "with Animations off the card must be painted on the arrival frame too, read " +
                    "${offFirst.first} against ${offSettled.first} px"
            }
        } finally {
            Animations.enabled = true
        }

        if (failures.isEmpty()) {
            println()
            println(
                "OK: Notifications is the preview's - the heading, the mode, its sentence, the app's own " +
                    "duration row, the card of three rows and the action, in that order, the value written " +
                    "once per row, the heading 16 dp under the back row, the sentence 8 dp under the mode " +
                    "card and 4 dp in, the cards 16 dp apart, the action at its label's width, and the " +
                    "groups fading up 8 dp only with Animations on",
            )
            return
        }
        println()
        println("FAILED:")
        failures.forEach { println("  - $it") }
        throw IllegalStateException("notifications order check failed (${failures.size} case(s))")
    }

    /** The screen itself, on the app's own surface and at density 1. */
    private fun ComposeUiTest.screen() {
        setContent { content() }
    }

    /**
     * The same screen for the off-screen scene, which has no test harness.
     *
     * [arriving] is what the app's own screen host provides on a navigation: a
     * group only fades up when one has just put its screen on show, which is the
     * preview's `screen--enter`. The settled frames of the check leave it off and
     * the entrance arm turns it on.
     */
    @androidx.compose.runtime.Composable
    private fun content(arriving: Boolean = false) {
        AppTheme(mode = ThemeMode.LIGHT, accent = Color(0xFFED5564), language = "en") {
            Box(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.surface)) {
                Box(Modifier.size(WIDTH.dp, HEIGHT.dp)) {
                    androidx.compose.runtime.CompositionLocalProvider(
                        LocalScreenArrival provides arriving,
                    ) {
                        SettingsNotificationsScreen(
                            language = "en",
                            onBack = {},
                            notificationMode = "in_app",
                            onNotificationModeChange = {},
                            notificationDurationSeconds = 5,
                            onNotificationDurationChange = {},
                            saveHistory = false,
                            onSaveHistoryChange = {},
                            onOpenHistory = {},
                            onTestNotification = {},
                        )
                    }
                }
            }
        }
    }

    /**
     * The frame the screen arrives on, and the ink the card paints in it.
     *
     * A scene's clock starts at the first `render`, so one render is the arrival
     * frame; the animation is a group's own `graphicsLayer`, and a card at
     * `alpha = 0` leaves the page's surface in that strip. The second value is the
     * top of the card in the same frame, so the two arms measure the same rows.
     */
    private fun firstFrameInk(): Pair<Int, Int> {
        val scene = scene(arriving = true)
        try {
            val arrival = png(scene.render(1_000_000_000L))
            // The arrival frame is the one the animation starts on; the frame after
            // it is the first the entrance has advanced from, and the last one is
            // past the medium1 token. Three frames, not two, because a scene's
            // first render is also the composition that starts the effect.
            scene.render(1_100_000_000L)
            val settled = png(scene.render(1_600_000_000L))
            val card = groupCard(settled) ?: return 0 to -1
            return ink(arrival, card.first, card.second) to card.first
        } finally {
            scene.close()
        }
    }

    /** The same strip once the entrance has had its time. */
    private fun settledFrameInk(): Pair<Int, Int> {
        val scene = scene(arriving = true)
        try {
            scene.render(1_000_000_000L)
            scene.render(1_100_000_000L)
            val settled = png(scene.render(1_600_000_000L))
            val card = groupCard(settled) ?: return 0 to -1
            return ink(settled, card.first, card.second) to card.first
        } finally {
            scene.close()
        }
    }

    @OptIn(ExperimentalComposeUiApi::class)
    private fun scene(arriving: Boolean = false): ImageComposeScene =
        ImageComposeScene(WIDTH, HEIGHT) { content(arriving) }

    /**
     * The tallest block of card fill in the frame, as its top and height.
     *
     * The screen is three cards in a column - the mode row, the duration row and
     * the group of three rows - and only the group fades up. A card is not one
     * run of fill: its rows are separated by the 3 dp seam the page shows
     * through, so the runs are merged back with a seam's width of slack, and the
     * group is the longest of the three.
     */
    @OptIn(ExperimentalComposeUiApi::class)
    private fun groupCard(png: ByteArray): Pair<Int, Int>? {
        val image: BufferedImage = javax.imageio.ImageIO.read(png.inputStream())
        val page = image.getRGB(2, 2)
        val runs = mutableListOf<IntArray>()
        var start = -1
        for (y in 0 until image.height) {
            val filled = !near(image.getRGB(40, y), page)
            if (filled && start < 0) start = y
            if (!filled && start >= 0) {
                if (y - start >= 6) runs += intArrayOf(start, y - 1)
                start = -1
            }
        }
        if (start >= 0) runs += intArrayOf(start, image.height - 1)
        // Merge what a seam split: 3 dp of page between two rows of one card.
        val merged = mutableListOf<IntArray>()
        runs.forEach { run ->
            val last = merged.lastOrNull()
            if (last != null && run[0] - last[1] <= 4) {
                last[1] = run[1]
            } else {
                merged += intArrayOf(run[0], run[1])
            }
        }
        val group = merged.maxByOrNull { it[1] - it[0] } ?: return null
        return group[0] to (group[1] - group[0] + 1)
    }

    /** Non-page pixels in the first 40 rows of the card that starts at [top]. */
    private fun ink(png: ByteArray, top: Int, height: Int): Int {
        val image: BufferedImage = javax.imageio.ImageIO.read(png.inputStream())
        val page = image.getRGB(2, 2)
        var count = 0
        for (y in top until minOf(top + 40, top + height, image.height)) {
            for (x in 20 until WIDTH - 20) {
                if (!near(image.getRGB(x, y), page)) count++
            }
        }
        return count
    }

    private fun near(a: Int, b: Int): Boolean {
        val dr = abs(((a shr 16) and 255) - ((b shr 16) and 255))
        val dg = abs(((a shr 8) and 255) - ((b shr 8) and 255))
        val db = abs((a and 255) - (b and 255))
        return dr + dg + db <= 6
    }

    private fun png(image: org.jetbrains.skia.Image): ByteArray =
        image.encodeToData(EncodedImageFormat.PNG)?.bytes ?: error("the scene produced no image")

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
