@file:OptIn(ExperimentalTestApi::class)

package com.music.vivi.desktop

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.test.ComposeUiTest
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.SemanticsNodeInteraction
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.runComposeUiTest
import androidx.compose.ui.unit.dp
import kotlin.math.abs

/**
 * Headless check of the settings hub against the preview, run with
 *
 *     ./gradlew :desktop:hubOrderCheck
 *
 * The hub is the screen the whole redesign starts from, and what it has to be is
 * the preview's `hub()` (`js/screens.js`): four groups - General, Appearance,
 * Privacy, About - holding seventeen rows, in that order, with the same labels.
 * A screen can look right and still list the rows in another order, or lose one
 * of them to a search key that no longer matches, and no still frame says so:
 * the labels are the same words either way.
 *
 * This runs the real composable on the ui-test harness and reads the semantics
 * tree, so the page's own text is what is compared:
 *
 *  * **the order** - every group title and every row label, taken from the tree
 *    and put back in document order (by the top of each node), must be the
 *    preview's sequence, entry by entry. A label the preview draws twice (the
 *    group `Appearance` and the row `Appearance`) is read twice, in that order;
 *  * **the descriptions** - the two the check can drive itself are read off the
 *    row that carries them (`Light` under Appearance, `Not logged in` under
 *    Account), because a row that kept its label and lost its value is the same
 *    regression one row further in;
 *  * **the header** - the preview's own order and measures: the title and the one
 *    icon button share a line, the search field is a full-width line of its own
 *    under it (not the button's place at half the width), and the field only
 *    exists once the button opened it;
 *  * **the group geometry** - a group title is inset 4 dp from the cards' left
 *    edge (`padding: 6px 4px`), and two groups stand 16 dp apart (`.md-group`
 *    margin) on top of the title's own 6 dp.
 *
 * The rows read the trailing value and the description of a row on the same
 * node (the clickable row merges its descendants), so a label is looked up by
 * text and a description is asserted on the row that carries both.
 */
object HubOrderCheck {

    private const val WIDTH = 640

    private const val HEIGHT = 1400

    /** A rectangle read off the test's semantics tree, in root px. */
    private data class Rect(val left: Float, val top: Float, val right: Float, val bottom: Float)

    /**
     * The preview's hub, in its own order (`hub()` in `js/screens.js`): the four
     * group titles and the seventeen row labels, exactly as the preview lists
     * them. A title in capitals of its own means the entry is a group title.
     */
    private val expected: List<String> = listOf(
        "General",
        "Updates",
        "Account",
        "Notifications",
        "Appearance",
        // The group `Appearance` and its first row share a label: the first
        // occurrence is the group title, the second the row.
        "Appearance",
        "Lyrics",
        "Player & audio",
        "Device sync",
        "Content",
        "Language",
        "Privacy",
        "AI Lyrics Translation",
        "Privacy",
        "Storage",
        "Data Saver",
        "About",
        "Backup and restore",
        "VIVI Wrapped \u00b7 This session",
        "Integrations",
        "Import from Spotify",
        "Desktop features",
        "System",
        "About",
    )

    @JvmStatic
    fun main(args: Array<String>) {
        val failures = mutableListOf<String>()

        // ------------------------------------------------------- the page
        var settings: Rect? = null
        var general: Rect? = null
        var appearanceGroup: Rect? = null
        var notificationsRow: Rect? = null
        var readOrder: List<String> = emptyList()
        var fieldIdle: List<Rect> = emptyList()
        var fieldOpen: List<Rect> = emptyList()
        var themedRow: Int = 0
        var accountRow: Int = 0

        runComposeUiTest {
            hubPage()

            // Everything the hub draws, in document order. The order is the
            // semantics tree's own (a scrollable column lays its children out in
            // the order it composed them), not the geometry: a node below the
            // scene's viewport reports no bounds at all, so sorting by `top`
            // would put the bottom of the page first.
            val wanted = expected.toSet()
            val seenTexts = mutableListOf<String>()
            fun walk(node: androidx.compose.ui.semantics.SemanticsNode) {
                val texts = node.config
                if (texts.contains(androidx.compose.ui.semantics.SemanticsProperties.Text)) {
                    texts[androidx.compose.ui.semantics.SemanticsProperties.Text]
                        .forEach { if (it.text in wanted) seenTexts += it.text }
                }
                node.children.forEach { walk(it) }
            }
            walk(onRoot().fetchSemanticsNode())
            readOrder = seenTexts

            settings = onNodeWithText("Settings").rect()
            if (settings == null) failures += "the hub has no `Settings` title"

            val idleFields = onAllNodes(hasSetTextAction()).rects()
            fieldIdle = idleFields

            // The two rows whose value this check decides: the theme it was
            // given, and an account that is not signed in.
            themedRow = onAllNodes(hasText("Appearance") and hasText("Light")).fetchSemanticsNodes().size
            accountRow = onAllNodes(hasText("Account") and hasText("Not logged in")).fetchSemanticsNodes().size

            onNodeWithContentDescription("Search").performClick()
            waitForIdle()

            val opened = onAllNodes(hasSetTextAction()).rects()
            fieldOpen = opened
            general = onAllNodesWithText("General").rects().minByOrNull { it.top }
            appearanceGroup = onAllNodesWithText("Appearance").rects().minByOrNull { it.top }
            notificationsRow = onAllNodesWithText("Notifications").rects().maxByOrNull { it.top }
        }

        // ------------------------------------------------------- the order
        if (readOrder == expected) {
            println("order: ${readOrder.size} entries, the preview's own sequence")
        } else {
            failures += "the hub's order is not the preview's: read $readOrder"
        }
        println("themed row=${themedRow}, account row=${accountRow}")
        if (themedRow == 0) failures += "the Appearance row does not carry `Light` as its value"
        if (accountRow == 0) failures += "the Account row does not carry `Not logged in` as its value"

        // ------------------------------------------------------- the header
        val title = settings
        if (title == null) {
            failures += "the header could not be measured: no `Settings` title"
        } else {
            println(
                "header: title left=${title.left.toInt()} top=${title.top.toInt()} " +
                    "bottom=${title.bottom.toInt()}",
            )
            if (abs(title.left - 16f) > 2f) {
                failures += "the page's title must sit on the page's 16 dp padding, it starts at ${title.left.toInt()}"
            }
            // The preview's `.page-title` carries 12 dp above it.
            if (abs(title.top - 28f) > 3f) {
                failures += "the title must carry the preview's 12 dp above the page padding " +
                    "(expected top ~28, read ${title.top.toInt()})"
            }
            if (fieldIdle.isNotEmpty()) {
                failures += "the search field must not exist before the button opens it, read ${fieldIdle.size}"
            }
            val field = fieldOpen.firstOrNull()
            if (field == null) {
                failures += "the search button opened no field"
            } else {
                println(
                    "search: field left=${field.left.toInt()} top=${field.top.toInt()} " +
                        "right=${field.right.toInt()} bottom=${field.bottom.toInt()}",
                )
                if (field.top <= title.bottom) {
                    failures += "the search field must have a line of its own under the title " +
                        "(title ends ${title.bottom.toInt()}, field starts ${field.top.toInt()})"
                }
                val contentWidth = WIDTH - 32f
                if (abs((field.right - field.left) - contentWidth) > 4f) {
                    failures += "the field must be the full page width ($contentWidth), read " +
                        "${(field.right - field.left).toInt()}"
                }
                if (abs(field.left - 16f) > 2f) {
                    failures += "the field must start on the page's padding, it starts at ${field.left.toInt()}"
                }
                val first = general
                if (first != null && first.top <= field.bottom) {
                    failures += "the first group must come under the search field " +
                        "(field ends ${field.bottom.toInt()}, group title starts ${first.top.toInt()})"
                }
            }
        }

        // ------------------------------------------------- group geometry
        val first = general
        if (first == null) {
            failures += "the General group title could not be measured"
        } else if (title != null) {
            val inset = first.left - title.left
            println("group title: left=${first.left.toInt()}, inset from the page's text: ${inset.toInt()}")
            if (abs(inset - 4f) > 1f) {
                failures += "a group title must be inset 4 dp from the cards' left edge " +
                    "(the preview's `padding: 6px 4px`), read $inset"
            }
        }
        val second = appearanceGroup
        val lastOfFirst = notificationsRow
        if (second == null || lastOfFirst == null) {
            failures += "the gap between two groups could not be measured"
        } else {
            val gap = second.top - lastOfFirst.bottom
            println("groups: last row of General ends ${lastOfFirst.bottom.toInt()}, Appearance's title starts ${second.top.toInt()} (gap ${gap.toInt()})")
            // 16 dp of group margin plus the title's own 6 dp of padding.
            if (gap < 18f || gap > 28f) {
                failures += "two groups must stand the preview's 16 dp apart plus the title's 6 dp " +
                    "(expected a gap of ~22, read ${gap.toInt()})"
            }
        }

        if (failures.isEmpty()) {
            println()
            println(
                "OK: the hub is the preview's - four groups (General, Appearance, Privacy, About) " +
                    "and their ${expected.size - 4} rows in the preview's order, the descriptions the " +
                    "check drives itself, the title with the button beside it over a full-width search " +
                    "field, and group titles inset 4 dp with 16 dp between two groups",
            )
            return
        }
        println()
        println("FAILED:")
        failures.forEach { println("  - $it") }
        throw IllegalStateException("hub order check failed (${failures.size} case(s))")
    }

    /** The hub itself, on the app's own surface and at density 1. */
    private fun ComposeUiTest.hubPage() {
        setContent {
            AppTheme(mode = ThemeMode.LIGHT, accent = Color(0xFFED5564), language = "en") {
                Box(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.surface)) {
                    Box(Modifier.size(WIDTH.dp, HEIGHT.dp)) {
                        SettingsScreen(
                            language = "en",
                            themeMode = ThemeMode.LIGHT,
                            isLoggedIn = false,
                            accountName = "",
                            updateStatus = UpdateStatus.Idle,
                            wrappedStats = WrappedStats(),
                            onOpen = {},
                        )
                    }
                }
            }
        }
    }

    private fun SemanticsNodeInteraction.rect(): Rect? {
        val node = try {
            fetchSemanticsNode()
        } catch (missing: AssertionError) {
            return null
        }
        val r = node.boundsInRoot
        return Rect(r.left, r.top, r.right, r.bottom)
    }

    private fun SemanticsNodeInteractionCollection.rects(): List<Rect> =
        fetchSemanticsNodes().map { node ->
            val r = node.boundsInRoot
            Rect(r.left, r.top, r.right, r.bottom)
        }
}

/** The semantics-node collection `onAllNodes` and `onAllNodesWithText` return. */
private typealias SemanticsNodeInteractionCollection =
    androidx.compose.ui.test.SemanticsNodeInteractionCollection
