@file:OptIn(ExperimentalTestApi::class)

package com.music.vivi.desktop

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
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
import androidx.compose.ui.test.hasSetTextAction
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
import javax.imageio.ImageIO
import kotlin.math.abs

/**
 * Headless check of the Integrations screen against the preview, run with
 *
 *     ./gradlew :desktop:integrationsOrderCheck
 *
 * The preview's `integrations` list (`js/screens.js`) is the heading and one card
 * of four rows, with no group title: the Discord mark and *Discord Rich Presence*
 * with its switch, *Discord application ID* with the field inside its own row, the
 * Last.fm mark and *Last.fm* with its switch, and *Send Now Playing* with its
 * switch. Rows 2 and 4 belong to the switch above them and fold away with it. The
 * page draws no sentence under the names; the app keeps the three it already had
 * there, by the owner's decision.
 *
 * The check composes the real screen in both states - both switches off, both on
 * with a field filled in - and reads: the sequence of texts, the heading 16 dp
 * under the back row, the cards, the 3 dp seams between the page's rows, the 16
 * dp between the page's card and the app's session card, the state of every
 * switch, the fields' place inside their rows (inline, at most 320 dp, to the
 * right of the title), the presses that change the screen, and the arrival
 * frame. It also writes the settled screen to `build/reports/integrations-screen.png`
 * so the page and the app can be looked at side by side.
 */
object IntegrationsOrderCheck {

    private const val WIDTH = 640

    private const val HEIGHT = 900

    /** A rectangle read off the test's semantics tree, in root px. */
    private data class Rect(val left: Float, val top: Float, val right: Float, val bottom: Float) {
        val width: Float get() = right - left
        val centreY: Float get() = (top + bottom) / 2f
    }

    /** A run of card fill down the measured column, in pixels from the top. */
    private data class Band(val top: Int, val bottom: Int)

    private fun string(key: String) = Localization.get("en", key)

    /** The page's own texts: the two rows left with both switches off. */
    private val foldedOrder = listOf(
        "\u2039 Back",
        string("integrations"),
        string("discord_presence"),
        string("discord_presence_desc"),
        string("lastfm"),
        string("lastfm_enable_desc"),
    )

    /**
     * The page's four rows with both switches on, then the app's own session row
     * under them: the key the page draws no field for.
     */
    private val openOrder = listOf(
        "\u2039 Back",
        string("integrations"),
        string("discord_presence"),
        string("discord_presence_desc"),
        string("discord_client_id"),
        string("lastfm"),
        string("lastfm_enable_desc"),
        string("lastfm_now_playing"),
        string("lastfm_now_playing_desc"),
        string("lastfm_session"),
        string("lastfm_session_hint"),
    )

    @JvmStatic
    fun main(args: Array<String>) {
        val failures = mutableListOf<String>()

        // ------------------------------------------------------ both switches off
        var folded: List<String> = emptyList()
        var back: Rect? = null
        var title: Rect? = null
        var offSwitches: List<ToggleableState?> = emptyList()
        var fieldsWhenFolded = 0

        runComposeUiTest {
            setContent { screen(discord = false, lastfm = false) }
            waitForIdle()
            folded = texts()
            back = onNodeWithText("\u2039 Back").rectOrNull()
            title = onNodeWithText(string("integrations")).rectOrNull()
            offSwitches = onAllToggleables()
            fieldsWhenFolded = onAllFields().size
        }

        if (fieldsWhenFolded != 0) {
            failures += "both switches off draw no field at all, read $fieldsWhenFolded"
        }

        if (folded == foldedOrder) {
            println("order: ${folded.size} texts, the page's two rows with their sentences and both switches off")
        } else {
            failures += "the folded screen's texts are not the preview's: read $folded"
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

        // ------------------------------------------------------ both switches on
        var open: List<String> = emptyList()
        var onSwitches: List<ToggleableState?> = emptyList()
        var fields: List<Rect> = emptyList()
        var idTitle: Rect? = null
        var sessionTitle: Rect? = null

        runComposeUiTest {
            setContent {
                screen(
                    discord = true,
                    lastfm = true,
                    nowPlaying = true,
                    id = "123456789012345678",
                    session = "b25l",
                )
            }
            waitForIdle()
            open = texts()
            onSwitches = onAllToggleables()
            fields = onAllFields()
            idTitle = onNodeWithText(string("discord_client_id")).rectOrNull()
            sessionTitle = onNodeWithText(string("lastfm_session")).rectOrNull()
        }

        if (open == openOrder) {
            println("order: ${open.size} texts, the page's four rows with their sentences then the app's session row")
        } else {
            failures += "the open screen's texts are not the page's then the app's: read $open"
        }

        // ------------------------------------------------------ the switches
        println("states: folded reads $offSwitches, open reads $onSwitches")
        if (offSwitches.isNotEmpty() && offSwitches.any { it != ToggleableState.Off }) {
            failures += "both switches are off in the folded state, read $offSwitches"
        }
        if (onSwitches.size != 3 || onSwitches.any { it != ToggleableState.On }) {
            failures += "the open screen draws three switches, all on, read $onSwitches"
        }

        // ------------------------------------------------------ the fields
        println("fields: ${fields.size} field(s) at ${fields.map { "x ${it.left.toInt()}..${it.right.toInt()} (${it.width.toInt()} wide)" }}")
        if (fields.size != 2) {
            failures += "the open screen draws two fields (the app ID and the session key), read ${fields.size}"
        }
        fields.forEach { field ->
            if (field.width > 320f + 1f) {
                failures += "a field is wider than the page's 320 dp cap, read ${field.width.toInt()}"
            }
        }
        val id = idTitle
        if (id != null && fields.isNotEmpty()) {
            val inline = abs(fields[0].centreY - id.centreY) <= 24f
            println("field: the app ID's field sits at y ${fields[0].centreY.toInt()}, its title at ${id.centreY.toInt()}, inline $inline")
            if (!inline) {
                failures += "the app ID's field must sit in its row, beside the title, not under the card"
            }
            // The page pushes the field to the row's right edge (margin-left:
            // auto): the row's right edge is the window less the page's 14 dp and
            // the row's 17 dp.
            val rowRight = WIDTH - 14 - 17f
            println("field: the app ID's field ends at ${fields[0].right.toInt()}, the row's right edge is ${rowRight.toInt()}")
            if (abs(fields[0].right - rowRight) > 2f) {
                failures += "the app ID's field must end at the row's right edge (${rowRight.toInt()}), read ${fields[0].right.toInt()}"
            }
        } else {
            failures += "the app ID's field could not be measured against its title"
        }

        // ------------------------------------------------------ the cards
        val foldedCards = cards(bands(settledFrame(discord = false, lastfm = false)))
        println("cards: ${foldedCards.size} band(s) ${foldedCards.map { "${it.first().top}..${it.last().bottom}" }} with both switches off")
        if (foldedCards.size != 1) {
            failures += "both switches off leave the page's one card, read ${foldedCards.size}"
        } else {
            val seams = seamsOf(foldedCards[0])
            println("seams: $seams in the page's card")
            if (seams.any { abs(it - 3) > 1 }) {
                failures += "the rows of the page's card sit on the 3 dp seam, read $seams"
            }
        }

        val openCards = cards(bands(settledFrame(discord = true, lastfm = true)))
        println("cards: ${openCards.size} band(s) ${openCards.map { "${it.first().top}..${it.last().bottom}" }} with both switches on")
        if (openCards.size != 2) {
            failures += "with both switches on the page's card and the session's card make 2 blocks, read ${openCards.size}"
        } else {
            val seams = seamsOf(openCards[0])
            println("seams: $seams in the page's card (four rows)")
            if (seams.size != 3 || seams.any { abs(it - 3) > 1 }) {
                failures += "the page's four rows sit on three 3 dp seams, read $seams"
            }
            val between = openCards[1].first().top - openCards[0].last().bottom - 1
            println("cards: the page's card ends ${openCards[0].last().bottom}, the session card starts ${openCards[1].first().top} (gap $between, 16 plus the rounded corner)")
            if (abs(between - 16f) > 4f) {
                failures += "the session card must stand the page's 16 dp under the page's card, read $between"
            }
        }

        // ------------------------------------------------------ the entrance
        val arrival = bands(firstFrame()).size
        println("entrance: $arrival band(s) on the arrival frame against ${bands(settledFrame(discord = false, lastfm = false)).size} once settled")
        if (arrival != 0) {
            failures += "the card fades up as the screen arrives: the arrival frame must not show it, read $arrival"
        }

        // ------------------------------------------------------ the presses act
        var askedDiscord: Boolean? = null
        var askedLastfm: Boolean? = null
        var fieldAfterDiscord = 0
        var fieldAfterLastfm = 0
        runComposeUiTest {
            setContent {
                var discord by remember { mutableStateOf(false) }
                var lastfm by remember { mutableStateOf(false) }
                screen(
                    discord = discord,
                    lastfm = lastfm,
                    onDiscord = { askedDiscord = it; discord = it },
                    onLastfm = { askedLastfm = it; lastfm = it },
                )
            }
            waitForIdle()
            // The whole row turns its switch, not only the knob.
            onNode(hasClickAction() and hasText(string("discord_presence"))).performClick()
            waitForIdle()
            fieldAfterDiscord = onAllFields().size
            onNode(hasClickAction() and hasText(string("lastfm"))).performClick()
            waitForIdle()
            fieldAfterLastfm = onAllFields().size
        }
        println("press: Discord asked for $askedDiscord and left $fieldAfterDiscord field(s), Last.fm asked for $askedLastfm and left $fieldAfterLastfm")
        if (askedDiscord != true) {
            failures += "a press on the Discord row must ask for Rich Presence on, read $askedDiscord"
        }
        if (fieldAfterDiscord != 1) {
            failures += "turning Rich Presence on must reveal its application ID row (1 field), read $fieldAfterDiscord"
        }
        if (askedLastfm != true) {
            failures += "a press on the Last.fm row must ask for scrobbling on, read $askedLastfm"
        }
        if (fieldAfterLastfm != 2) {
            failures += "turning scrobbling on must reveal the session key row (2 fields), read $fieldAfterLastfm"
        }

        // ------------------------------------------------------ the real screen
        var realTexts: List<String> = emptyList()
        runComposeUiTest {
            setContent {
                AppTheme(mode = ThemeMode.LIGHT, accent = Color(0xFFED5564), language = "en") {
                    Box(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.surface)) {
                        Box(Modifier.size(WIDTH.dp, HEIGHT.dp)) {
                            SettingsIntegrationsScreen(
                                language = "en",
                                onBack = {},
                                discordEnabled = true,
                                onDiscordEnabledChange = {},
                                discordClientId = "123456789012345678",
                                onDiscordClientIdChange = {},
                                lastfmEnabled = true,
                                onLastfmEnabledChange = {},
                                lastfmSession = "b25l",
                                onLastfmSessionChange = {},
                                lastfmNowPlaying = true,
                                onLastfmNowPlayingChange = {},
                            )
                        }
                    }
                }
            }
            waitForIdle()
            realTexts = texts()
        }
        println("real: ${realTexts.size} text(s) from SettingsIntegrationsScreen, head ${realTexts.take(3)}")
        if (realTexts != openOrder) {
            failures += "the app's own screen must be the page then its session row, read $realTexts"
        }

        // ------------------------------------------------------ the render
        val render = File("build/reports/integrations-screen.png")
        render.parentFile.mkdirs()
        ImageIO.write(settledFrame(discord = true, lastfm = true), "png", render)
        println("render: ${render.absolutePath}")

        if (failures.isEmpty()) {
            println()
            println(
                "OK: Integrations is the preview's - the heading 16 dp under the back row, one card of four rows " +
                    "on their 3 dp seams with the Discord mark, the application ID field inside its own row, the " +
                    "Last.fm mark, and Send Now Playing folding with Last.fm, both switches reading their " +
                    "preference in both states with a press on the row that really changes the screen, the " +
                    "session key in a card of the app's own 16 dp under the page's, and the page fading up on arrival",
            )
            return
        }
        println()
        println("FAILED:")
        failures.forEach { println("  - $it") }
        throw IllegalStateException("integrations order check failed (${failures.size} case(s))")
    }

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

    private fun ComposeUiTest.onAllToggleables(): List<ToggleableState?> =
        onAllNodes(isToggleable()).fetchSemanticsNodes().map { node ->
            if (node.config.contains(SemanticsProperties.ToggleableState)) {
                node.config[SemanticsProperties.ToggleableState]
            } else {
                null
            }
        }

    private fun ComposeUiTest.onAllFields(): List<Rect> =
        onAllNodes(hasSetTextAction()).fetchSemanticsNodes().map { node ->
            val r = node.boundsInRoot
            Rect(r.left, r.top, r.right, r.bottom)
        }

    /**
     * The screen body the app's own screen draws, with the options the caller
     * holds: the real [SettingsIntegrationsScreen] is composed as well, further
     * down, so this is only the way the check can drive the states.
     */
    @Composable
    private fun screen(
        arriving: Boolean = false,
        discord: Boolean,
        lastfm: Boolean,
        id: String = "",
        session: String = "",
        nowPlaying: Boolean = false,
        onDiscord: (Boolean) -> Unit = {},
        onLastfm: (Boolean) -> Unit = {},
    ) {
        AppTheme(mode = ThemeMode.LIGHT, accent = Color(0xFFED5564), language = "en") {
            Box(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.surface)) {
                Box(Modifier.size(WIDTH.dp, HEIGHT.dp)) {
                    CompositionLocalProvider(LocalScreenArrival provides arriving) {
                        SettingsIntegrationsScreen(
                            language = "en",
                            onBack = {},
                            discordEnabled = discord,
                            onDiscordEnabledChange = onDiscord,
                            discordClientId = id,
                            onDiscordClientIdChange = {},
                            lastfmEnabled = lastfm,
                            onLastfmEnabledChange = onLastfm,
                            lastfmSession = session,
                            onLastfmSessionChange = {},
                            lastfmNowPlaying = nowPlaying,
                            onLastfmNowPlayingChange = {},
                        )
                    }
                }
            }
        }
    }

    @OptIn(ExperimentalComposeUiApi::class)
    private fun scene(discord: Boolean, lastfm: Boolean, arriving: Boolean = false): ImageComposeScene =
        ImageComposeScene(WIDTH, HEIGHT) {
            screen(arriving = arriving, discord = discord, lastfm = lastfm)
        }

    /** The frame the screen arrives on. */
    private fun firstFrame(): BufferedImage {
        val scene = scene(discord = false, lastfm = false, arriving = true)
        try {
            return png(scene.render(1_000_000_000L))
        } finally {
            scene.close()
        }
    }

    /** The same screen once an entrance has had its time. */
    private fun settledFrame(discord: Boolean, lastfm: Boolean): BufferedImage {
        val scene = scene(discord = discord, lastfm = lastfm, arriving = true)
        try {
            scene.render(1_000_000_000L)
            scene.render(1_100_000_000L)
            return png(scene.render(1_600_000_000L))
        } finally {
            scene.close()
        }
    }

    /**
     * The runs of card fill down a column clear of the glyphs and the page's
     * padding: a row is one run, the 3 dp seam between two rows is the gap
     * between their runs.
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

    /** The runs that belong to one card: a seam smaller than a gap between cards. */
    private fun cards(runs: List<Band>): List<List<Band>> {
        val out = mutableListOf<List<Band>>()
        var current = mutableListOf<Band>()
        runs.forEach { run ->
            val previous = current.lastOrNull()
            if (previous != null && run.top - previous.bottom - 1 > 8) {
                out += current
                current = mutableListOf()
            }
            current += run
        }
        if (current.isNotEmpty()) out += current
        return out
    }

    /** The gaps between the rows of one card, in pixels. */
    private fun seamsOf(card: List<Band>): List<Int> =
        card.zipWithNext { a, b -> b.top - a.bottom - 1 }

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
