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
 * Headless check of the Account screen against the preview, run with
 *
 *     ./gradlew :desktop:accountOrderCheck
 *
 * The preview's `account` list (`js/screens.js`) is the title, then - signed in -
 * the account's own card with *Log out* in its trailing slot, and one card of
 * three rows: the auto-sync switch, the run it triggers and the upload that
 * closes the same job. Signed out it is the sign-in the app already draws (the
 * hero, the numbered connect card and the cookie card), which `loginRenderCheck`
 * measures; this check reads the signed-in state, where the page changed.
 *
 * Two harnesses, for the two kinds of fact:
 *
 *  * **the ui-test one** composes the real `SettingsAccountScreen` and reads the
 *    semantics tree: the heading, the account's name and its one-word state, the
 *    action beside it, and then the three rows of the sync card in the preview's
 *    order - with nothing between them, so a row that moved out of the card (as
 *    the upload row used to) fails here;
 *  * **the pixel one** renders the same screen into an off-screen
 *    `ImageComposeScene` and reads the column of card fill: one band for the
 *    account row, three for the sync card's rows, so two cards and not four, with
 *    the 3 dp seam inside the card and the 16 dp the page puts between two cards.
 *    The arrival frame is read too: a group fades up there, so it paints nothing.
 */
object AccountOrderCheck {

    private const val WIDTH = 640

    private const val HEIGHT = 1200

    /** A rectangle read off the test's semantics tree, in root px. */
    private data class Rect(val left: Float, val top: Float, val right: Float, val bottom: Float)

    /** A run of card fill down the measured column, in pixels from the top. */
    private data class Band(val top: Int, val bottom: Int) {
        val height: Int get() = bottom - top + 1
    }

    /** The three labels the upload row can carry, and the two descriptions. */
    private val uploadLabels = listOf(
        "playlists_upload",
        "playlists_upload_and_sync",
        "playlists_sync_ytm",
    ).map { Localization.get("en", it) }

    private val uploadDescriptions = listOf("playlists_upload_desc", "playlists_upload_none")
        .map { Localization.get("en", it) }

    /**
     * The fixed head of the page, in the preview's order.
     *
     * The account's name is the store's own value: the row shows who is signed
     * in, which is the one text here that is data rather than a label.
     */
    private fun head(): List<String> = listOf(
        "\u2039 Back",
        Localization.get("en", "account"),
        DesktopSettings.load().accountName.ifBlank { "YouTube" },
        Localization.get("en", "account_signed_in"),
        Localization.get("en", "logout"),
        Localization.get("en", "yt_sync"),
        Localization.get("en", "ytm_sync"),
        Localization.get("en", "sync_now"),
    )

    @JvmStatic
    fun main(args: Array<String>) {
        val failures = mutableListOf<String>()

        var read: List<String> = emptyList()
        var back: Rect? = null
        var title: Rect? = null

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
            title = onNodeWithText(Localization.get("en", "account")).rectOrNull()
        }

        // ------------------------------------------------------ the sequence
        val head = head()
        if (read.size != head.size + 2) {
            failures += "the screen must read its ${head.size} fixed texts and the upload row's two, " +
                "read ${read.size}: $read"
        }
        if (read.take(head.size) != head) {
            failures += "the page's first texts are not the preview's: read ${read.take(head.size)}"
        }
        val uploadLabel = read.getOrNull(head.size)
        val uploadDescription = read.getOrNull(head.size + 1)
        if (uploadLabel != null && uploadLabel !in uploadLabels) {
            failures += "the sync card's third row must be the upload row, read '$uploadLabel'"
        }
        if (uploadDescription != null && uploadDescription !in uploadDescriptions) {
            failures += "the upload row's description is not one of the action's two, read '$uploadDescription'"
        }
        if (failures.isEmpty()) {
            println("order: ${read.size} texts, the preview's own sequence")
            println("rows: account (${read[2]}), ${read[5]}, ${read[7]}, ${uploadLabel}")
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
        val settled = settledFrame()
        val bands = cards(settled)
        println(
            "cards: ${bands.size} band(s) ${bands.map { "${it.top}..${it.bottom}" }}",
        )
        // The account row, then the three rows of the sync card: the preview has
        // two cards here, and the app drew four (the switch and the run in one,
        // the upload in another, the way out in a third).
        if (bands.size != 4) {
            failures += "the page must be the account row and the three rows of the sync card (4), " +
                "read ${bands.size}"
        } else {
            val betweenCards = bands[1].top - bands[0].bottom - 1
            println("cards: the account row ends ${bands[0].bottom}, the sync card starts ${bands[1].top} (gap $betweenCards)")
            if (abs(betweenCards - 16) > 2) {
                failures += "two cards must stand 16 dp apart, read $betweenCards"
            }
            val seams = listOf(
                bands[2].top - bands[1].bottom - 1,
                bands[3].top - bands[2].bottom - 1,
            )
            println("seams: $seams")
            seams.forEach { seam ->
                if (abs(seam - 3) > 1) {
                    failures += "the sync card's rows must be joined by the 3 dp seam, read $seam"
                }
            }
            if (bands.drop(1).any { it.height < 40 }) {
                failures += "the sync card's three rows must each be a row tall, read ${bands.drop(1).map { it.height }}"
            }
        }

        // ------------------------------------------------------ the entrance
        val arrival = cards(firstFrame())
        println("entrance: ${arrival.size} band(s) on the arrival frame against ${bands.size} once settled")
        if (bands.size != 4) {
            // Nothing to read: the shape failure above already says why.
        } else if (arrival.isNotEmpty()) {
            failures += "a group must fade up as the screen arrives: the arrival frame already paints " +
                "${arrival.size} card(s)"
        }

        if (failures.isEmpty()) {
            println()
            println(
                "OK: Account is the preview's - the heading 16 dp under the back row, the account's " +
                    "name with its one-word state and Log out beside it on one card, then the card of " +
                    "the switch, the run and the upload, two cards 16 dp apart with the three rows on " +
                    "their 3 dp seams, and the groups fading up on arrival",
            )
            return
        }
        println()
        println("FAILED:")
        failures.forEach { println("  - $it") }
        throw IllegalStateException("account order check failed (${failures.size} case(s))")
    }

    /**
     * The same screen for the off-screen scene, which has no test harness.
     *
     * [arriving] is what the app's own screen host provides on a navigation: a
     * group only fades up when one has just put its screen on show, which is the
     * preview's `screen--enter`.
     */
    @androidx.compose.runtime.Composable
    private fun content(arriving: Boolean = false) {
        AppTheme(mode = ThemeMode.LIGHT, accent = Color(0xFFED5564), language = "en") {
            Box(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.surface)) {
                Box(Modifier.size(WIDTH.dp, HEIGHT.dp)) {
                    CompositionLocalProvider(LocalScreenArrival provides arriving) {
                        SettingsAccountScreen(
                            language = "en",
                            onBack = {},
                            isLoggedIn = true,
                            onOpenLogin = {},
                            onLogout = {},
                            onLoggedIn = {},
                        )
                    }
                }
            }
        }
    }

    @OptIn(ExperimentalComposeUiApi::class)
    private fun scene(arriving: Boolean = false): ImageComposeScene =
        ImageComposeScene(WIDTH, HEIGHT) { content(arriving) }

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
    private fun settledFrame(): BufferedImage {
        val scene = scene(arriving = true)
        try {
            scene.render(1_000_000_000L)
            scene.render(1_100_000_000L)
            return png(scene.render(1_600_000_000L))
        } finally {
            scene.close()
        }
    }

    /**
     * The runs of card fill down the column, the title's own glyphs dropped.
     *
     * A card is not one run: its rows are separated by the 3 dp seam the page
     * shows through, so a card of three rows reads as three bands - which is the
     * point, because the seams are then measurable.
     */
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
