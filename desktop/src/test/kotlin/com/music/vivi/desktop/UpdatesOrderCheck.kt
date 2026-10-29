@file:OptIn(ExperimentalTestApi::class)

package com.music.vivi.desktop

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.CompositionLocalProvider
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
 * Headless check of the Updates screen against the preview, run with
 *
 *     ./gradlew :desktop:updatesOrderCheck
 *
 * The preview's `updates` list (`js/screens.js`) is four blocks: the title, one
 * card of two rows (the check with the version under it and its Check now
 * button, the pre-release switch), the interval choice, and the card of the two
 * destinations the version history is read from. Nothing here has an order to
 * invent, which is why it is read instead of looked at: a version that repeats
 * itself under the title, an icon where the action belongs, or a card that
 * carries one 12 dp gap among 16 dp ones all look plausible in a still frame.
 *
 * Two harnesses, for the two kinds of fact:
 *
 *  * **the ui-test one** composes the real `SettingsUpdatesScreen` and reads the
 *    semantics tree, so the page's own text is the input: every text of the
 *    screen, in document order, must be the preview's own sequence (`expected`
 *    below, with the app's one extra row named in it); the heading must stand
 *    16 dp under the back row (`.md-section-title` after `.back-row`'s 8 dp),
 *    the four blocks 16 dp apart (`.md-group`'s top margin), the two rows of a
 *    card joined by the 3 dp seam, and the action must be a button at its
 *    label's width inside the row's trailing slot (`.md-row__trailing`, capped at
 *    60 % of the row) instead of a control the width of the page;
 *  * **the pixel one** renders the same screen into an off-screen
 *    `ImageComposeScene` twice - the frame the screen arrives on and the one it
 *    settles on - because an entrance is invisible to a semantics tree: with
 *    Animations on the card is not painted in the first frame (the group fades up
 *    8 dp from nothing, the preview's `.screen--enter .md-group`) and with the
 *    switch off the two frames are the same.
 */
object UpdatesOrderCheck {

    private const val WIDTH = 640

    private const val HEIGHT = 1200

    /** A rectangle read off the test's semantics tree, in root px. */
    private data class Rect(val left: Float, val top: Float, val right: Float, val bottom: Float)

    /** What the interval row shows for the stored 24 hours. */
    private val intervalLabel = Localization.get("en", "interval_24h")

    /** What the source row shows for the default source. */
    private val sourceLabel = UpdateSource.repoFor(UpdateSource.FORK)

    /**
     * Every text the screen should show, in the order it should show them.
     *
     * The preview's four blocks in its own order, with the app's own source row
     * named in it: the preview has no choice of repository (it is a real
     * desktop option), and naming it here keeps the sequence complete rather
     * than filtered, so a second extra row fails instead of hiding.
     */
    private val expected = listOf(
        "\u2039 Back",
        "Updates",
        "Check for updates",
        // The row's description is the version and nothing else: the preview
        // prints `FULL_VERSION` under the title.
        AppInfo.FULL_VERSION,
        "Check now",
        "Include pre-releases",
        "Check interval",
        intervalLabel,
        "Update source",
        sourceLabel,
        "Changelog",
        "Commits",
    )

    @JvmStatic
    fun main(args: Array<String>) {
        val failures = mutableListOf<String>()

        var read: List<String> = emptyList()
        var back: Rect? = null
        var title: Rect? = null
        var check: Rect? = null
        var prerelease: Rect? = null
        var interval: Rect? = null
        var source: Rect? = null
        var changelog: Rect? = null
        var commits: Rect? = null
        var action: Rect? = null

        runComposeUiTest {
            setContent { content() }
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
            title = onNodeWithText("Updates").rectOrNull()
            check = onNodeWithText("Check for updates").rectOrNull()
            prerelease = onNodeWithText("Include pre-releases").rectOrNull()
            interval = onNodeWithText("Check interval").rectOrNull()
            source = onNodeWithText("Update source").rectOrNull()
            changelog = onNodeWithText("Changelog").rectOrNull()
            commits = onNodeWithText("Commits").rectOrNull()
            action = onNodeWithText("Check now").rectOrNull()
        }

        // ------------------------------------------------------ the sequence
        if (read == expected) {
            println("blocks: ${read.size} texts, the preview's own sequence (the source row named in it)")
        } else {
            failures += "the screen's texts are not the preview's: read $read"
        }

        // ------------------------------------------------------ the measures
        // One copy per read: a local the closure above assigns is not smart-cast.
        val t = title
        val b = back
        val c = check
        val p = prerelease
        val i = interval
        val s = source
        val g = changelog
        val cm = commits
        val act = action

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
        // The title's own box ends after its text: the preview's 16 dp sits
        // between it and the first card.
        gap("first card", t, c, 16f)
        // Two rows of one card are 3 dp apart - the seam the page shows through -
        // and two cards 16 dp.
        gap("pre-release", c, p, 3f, " (one card: the 3 dp seam)")
        gap("interval", p, i, 16f, " (a card of its own)")
        gap("source", i, s, 16f, " (the app's own row, a card like the others)")
        gap("changelog", s, g, 16f, " (the card of the two destinations)")
        gap("commits", g, cm, 3f, " (one card: the 3 dp seam)")

        if (act == null) {
            failures += "the action could not be measured"
        } else if (c == null) {
            failures += "the row the action sits in could not be measured"
        } else {
            val width = act.right - act.left
            println("action: width ${width.toInt()}, right edge ${act.right.toInt()} against the row's ${c.right.toInt()}")
            if (width > 260f) {
                failures += "the action must be as wide as its label, not as wide as the page: read ${width.toInt()}"
            }
            // The row pads 17 dp inside the card, and the button ends there.
            val inset = c.right - act.right
            if (abs(inset - 17f) > 3f) {
                failures += "the action must end on the row's trailing edge (17 dp inside the card), read an inset of ${inset.toInt()}"
            }
            // It is the row's trailing slot: to the right of the title, not under it.
            if (act.left <= c.left + 120f) {
                failures += "the action must sit in the row's trailing slot, not at its start: read ${act.left.toInt()}"
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
                "OK: Updates is the preview's - the heading, the check row with the version under it and " +
                    "Check now beside it, the pre-release switch, the interval, the app's own source row and " +
                    "the Changelog and Commits card, in that order, the blocks 16 dp apart with each card's " +
                    "rows on its 3 dp seam, the action at its label's width on the row's trailing edge, and " +
                    "the groups fading up 8 dp only with Animations on",
            )
            return
        }
        println()
        println("FAILED:")
        failures.forEach { println("  - $it") }
        throw IllegalStateException("updates order check failed (${failures.size} case(s))")
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
                    CompositionLocalProvider(LocalScreenArrival provides arriving) {
                        SettingsUpdatesScreen(
                            language = "en",
                            onBack = {},
                            updateStatus = UpdateStatus.Idle,
                            includePreReleases = false,
                            updateIntervalHours = 24,
                            updateSource = UpdateSource.FORK,
                            onIntervalChange = {},
                            onTogglePreReleases = {},
                            onUpdateSourceChange = {},
                            onCheckUpdates = {},
                            onOpenChangelog = {},
                            onOpenCommits = {},
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
     * The screen is the check card, two choices of one row each and the card of
     * the two destinations, and only the groups fade up: a choice is drawn by
     * `M3SettingsDropdownItem`, which is not a group. The tallest run of fill is
     * therefore a group, and a card is not one run - its rows are separated by the
     * 3 dp seam the page shows through - so the runs are merged back with a
     * seam's width of slack.
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
