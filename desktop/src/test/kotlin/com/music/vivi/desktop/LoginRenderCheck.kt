@file:OptIn(ExperimentalComposeUiApi::class)

package com.music.vivi.desktop

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.ImageComposeScene
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.unit.dp
import org.jetbrains.skia.EncodedImageFormat
import java.io.File

/**
 * Headless check of the Google sign-in screen, run with
 *
 *     ./gradlew :desktop:loginRenderCheck
 *
 * The screen is now a hero (the Google mark on a filled tile, what the screen is
 * for, the state as one pill) plus one card that numbers the three steps it
 * already printed and carries the single button. Both pieces are rendered into
 * an off-screen `ImageComposeScene` (no window, no GPU) and the pixels are
 * measured, because "it compiles" says nothing about a layout:
 *
 *  * the mark tile is a solid filled square at the top left of the hero - the
 *    thing that makes the screen look like a product rather than a form, and
 *    what a missing fill or a zero-sized box would take away;
 *  * the same fill appears again lower down, which is the button: the tile and
 *    the action share `primary`, so a card whose button silently lost its
 *    container fails here;
 *  * the pill tells the truth: the "logged in as" state must change the pixels.
 *  * **the switch-over is one movement of two halves.** Opening the manual-cookie
 *    card takes the window button away (upwards, with its row) while the form
 *    unfolds downwards, and both have to be moving in the same frame, so an
 *    intermediate frame is rendered on the way and must show the card already
 *    shorter than folded, the button's fill already fading, and the form's body
 *    already open. A sequence where one waits for the other leaves a frame with
 *    only one of the three, which is what this checks. The hand-over runs for
 *    both cards - Google's and Spotify's - because they are separate composables
 *    that have to behave the same way.
 *
 * A session that already exists is not this content any more: `LoginContent`
 * hands over to `AccountSection` (name, playlist sync, log out), so there is no
 * second "signed in" render here.
 *
 * What it does NOT check is whether the strings read well or whether the card
 * is pleasant: that needs eyes. Every string it asks for is covered by
 * `scripts/audit_desktop_localization.py`.
 *
 * PNGs are written under `.ignore/login/`.
 */
object LoginRenderCheck {

    private val outDir = File(".ignore/login")

    /** The badge's own fill, captured from the theme on every render. */
    private var pillFill: Int = 0

    /** Wide enough for the two-column hero, tall enough for the whole card. */
    private const val WIDTH = 900
    private const val HEIGHT = 1100

    @JvmStatic
    fun main(args: Array<String>) {
        outDir.mkdirs()
        val failures = mutableListOf<String>()

        val signedOut = render(accountName = "")
        File(outDir, "signed-out.png").writeBytes(signedOut)
        val named = render(accountName = "Vivi Listener")
        File(outDir, "named.png").writeBytes(named)
        val again = render(accountName = "")

        val image = javax.imageio.ImageIO.read(signedOut.inputStream())
        val ink = ink(image)
        println("== signed out  ${WIDTH}x$HEIGHT  ink=$ink")
        if (ink <= 0) failures += "the sign-in screen painted nothing"

        // The mark tile: 64 dp at a 20 dp inset in the hero, filled with
        // `primary`. The fill is read back structurally - the colour that covers
        // the most pixels inside the hero band - so the check never needs to know
        // which primitive the accent resolves to, and a sample landing on the
        // glyph inside the tile cannot be mistaken for the fill.
        val tile = dominantFill(image, 0 until 200)
        val box = if (tile == null) null else bounds(image, tile, 0 until 200)
        println("   hero fill ${tile?.let { hex(it) }} at ${box?.joinToString()}")
        when {
            tile == null -> failures += "the hero has no filled tile at all"
            box == null -> failures += "the hero fill covers a single pixel, not a tile"
            else -> {
                val (x0, y0, x1, y1) = box
                val side = x1 - x0 + 1
                val height = y1 - y0 + 1
                if (side !in 60..68 || height !in 60..68) {
                    failures += "the mark tile is ${side}x$height px, not the 64 dp square it should be"
                }
                // The Google mark is drawn on top of it, so the fill covers the
                // square minus the glyph: a lot less than the whole area, and
                // much more than nothing.
                val filled = count(image, tile, 0 until 200)
                val share = filled * 100 / (side * height)
                println("   mark tile: $filled px of ${side * height} ($share %)")
                if (share < 25) failures += "the mark tile is not filled: only $share % of its square is painted"
            }
        }

        // The same fill, below the hero: the button of the card.
        val belowHero = if (tile == null) 0 else count(image, tile, 200 until image.height)
        println("   same fill below the hero (button): $belowHero px")
        if (belowHero < 2000) {
            failures += "the sign-in button does not paint its primary container ($belowHero px)"
        }

        println("== named      ink=${ink(javax.imageio.ImageIO.read(named.inputStream()))}, differs=${!same(signedOut, named)}")
        if (same(signedOut, named)) {
            failures += "the state pill does not change with the account (two renders are identical)"
        }
        // Same input, same pixels: a render that drifts makes every other
        // comparison here meaningless.
        if (!same(signedOut, again)) {
            failures += "the same input rendered two different images"
        }

        // The pill is the identity, not the absence: with no session the head must
        // carry no badge at all, and with one it must carry the account. The pill's
        // fill is read off the theme, so the check never guesses which primitive it
        // is; Import from Spotify follows the same rule (see the Spotify check).
        val outPill = pillInHero(signedOut)
        val namedPill = pillInHero(named)
        println("== badge     signed out=$outPill px  named=$namedPill px")
        if (outPill != 0) {
            failures += "the head without a session must draw no badge, read $outPill px of the pill's fill"
        }
        if (namedPill <= 0) {
            failures += "the head with a session must show who it belongs to, read no pill ($namedPill px)"
        }

        // ------------------------------------------------------- sign-in hand-overs
        // Three frames each: folded, 50 ms into the hand-over and settled. The
        // middle one is the whole point - it is where a hand-over that waits for
        // the other half shows itself.
        handOver("Google", failures) { manualOpen ->
            GoogleSignInCard(
                language = "en",
                waiting = false,
                saving = false,
                manualOpen = manualOpen,
                onStart = {},
            )
        }
        handOver("Spotify", failures) { manualOpen ->
            SpotifyConnectCard(
                language = "en",
                loading = false,
                manualOpen = manualOpen,
                onStart = {},
            )
        }

        if (failures.isEmpty()) {
            println()
            println("OK: the sign-in screen composes, its mark tile is filled, its button shares that fill")
            println("    and both hand-overs to the manual card are one movement")
            return
        }
        println()
        println("FAILED:")
        failures.forEach { println("  - $it") }
        throw IllegalStateException("login render check failed (${failures.size} case(s))")
    }

    private class HandOver(val folded: ByteArray, val mid: ByteArray, val settled: ByteArray)

    /**
     * Renders the card [card] above the manual-cookie card in one scene, three
     * frames of the same hand-over, and checks them.
     *
     * Folded the page is the sign-in card plus the manual header; settled it is
     * the card, the header and the form body, and the card has lost exactly the
     * height of its button row (no empty gap where the button used to be). The
     * middle frame is where the two halves have to be seen moving together: the
     * form already open and not yet all the way, the card already shrinking and
     * not yet done, the button's fill already on its way out.
     */
    private fun handOver(
        label: String,
        failures: MutableList<String>,
        card: @Composable (Boolean) -> Unit,
    ) {
        val handOver = handOverFrames(card)
        File(outDir, "hand-over-${label.lowercase()}-folded.png").writeBytes(handOver.folded)
        File(outDir, "hand-over-${label.lowercase()}-mid.png").writeBytes(handOver.mid)
        File(outDir, "hand-over-${label.lowercase()}-settled.png").writeBytes(handOver.settled)
        val foldedBands = bands(handOver.folded)
        val midBands = bands(handOver.mid)
        val settledBands = bands(handOver.settled)
        println("== $label hand-over  folded=${foldedBands.bands.size} rows seams=${foldedBands.seams} ${ranges(foldedBands)}")
        println("                      mid=${midBands.bands.size} rows seams=${midBands.seams} ${ranges(midBands)}")
        println("                  settled=${settledBands.bands.size} rows seams=${settledBands.seams} ${ranges(settledBands)}")
        if (foldedBands.bands.size != 2) {
            failures += "$label: folded the page is the card plus the manual header (2 bands), " +
                "read ${foldedBands.bands.size}"
        }
        if (settledBands.bands.size != 3) {
            failures += "$label: settled the page is the card, the manual header and its body (3 bands), " +
                "read ${settledBands.bands.size}"
        }
        if (midBands.bands.size != 3) {
            failures += "$label: in the middle of the hand-over the form body must already be open " +
                "(3 bands), read ${midBands.bands.size}"
        }
        val foldedCard = foldedBands.bands.firstOrNull()?.let { it.last - it.first + 1 }
        val midCard = midBands.bands.firstOrNull()?.let { it.last - it.first + 1 }
        val settledCard = settledBands.bands.firstOrNull()?.let { it.last - it.first + 1 }
        println("                      card height folded=$foldedCard mid=$midCard settled=$settledCard")
        if (foldedCard != null && settledCard != null) {
            val lost = foldedCard - settledCard
            if (lost !in 45..95) {
                failures += "$label: open, the card must lose the height of its button row (45..95 px), lost $lost"
            }
            if (midCard != null && midCard >= foldedCard) {
                failures += "$label: in the middle of the hand-over the card has not started to shrink yet " +
                    "($midCard of $foldedCard)"
            }
            if (midCard != null && midCard <= settledCard && settledCard < foldedCard) {
                failures += "$label: in the middle of the hand-over the card has already finished shrinking " +
                    "($midCard): the two halves are not one movement"
            }
        }
        // The form, in the same frame: open, but not yet all the way, so the frame
        // really is between the two ends of the movement.
        val midBody = midBands.bands.lastOrNull()?.let { it.last - it.first + 1 }
        val settledBody = settledBands.bands.lastOrNull()?.let { it.last - it.first + 1 }
        println("                      form body mid=$midBody settled=$settledBody")
        if (midBody != null && settledBody != null && midBody >= settledBody) {
            failures += "$label: in the middle of the hand-over the form is already fully open " +
                "($midBody of $settledBody): it opened before the button left"
        }
        // And the button itself: its `primary` fill is smaller in the mid frame
        // than when folded (it is on its way out) and gone once settled. The fill
        // is read off the folded card once, so every frame is counted in the same
        // colour and the mark tile's `secondaryContainer` cannot stand in for it.
        val fill = buttonFill(handOver.folded, foldedBands)
        val foldedFill = fillInCard(handOver.folded, foldedBands, fill)
        val midFill = fillInCard(handOver.mid, midBands, fill)
        val settledFill = fillInCard(handOver.settled, settledBands, fill)
        println("                      window button fill folded=$foldedFill mid=$midFill settled=$settledFill")
        if (foldedFill < 2000) {
            failures += "$label: the card's window button does not paint its fill when folded ($foldedFill px)"
        }
        if (midFill >= foldedFill) {
            failures += "$label: in the middle of the hand-over the window button is still fully painted " +
                "($midFill of $foldedFill px): it is not leaving while the form opens"
        }
        if (settledFill > 400) {
            failures += "$label: open, the card still paints a window button ($settledFill px)"
        }
    }

    /**
     * The hand-over, frame by frame: folded, 100 ms in, settled.
     *
     * The card and the form read the same mutable state, as they do on the real
     * screen, and the scene's own clock is stepped so the animations are driven
     * exactly as a display would drive them. The step has to be generous: a
     * transition starts on the frame *after* the one its state change landed on,
     * so the 16 ms frame is where the state is applied and the movement begins on
     * the one after it.
     */
    private fun handOverFrames(card: @Composable (Boolean) -> Unit): HandOver {
        val probe = Probe()
        val scene = ImageComposeScene(WIDTH, HEIGHT) {
            AppTheme(mode = ThemeMode.LIGHT, accent = Color(0xFFED5564), language = "en") {
                Box(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
                    Column(Modifier.fillMaxWidth().padding(16.dp)) {
                        card(probe.manualOpen)
                        Spacer(Modifier.height(12.dp))
                        M3CollapsibleCard(
                            title = Localization.get("en", "login_manual_title"),
                            expanded = probe.manualOpen,
                            onExpandedChange = { probe.manualOpen = it },
                        ) {
                            Text(Localization.get("en", "cookie_label"))
                            Text(Localization.get("en", "advanced_login_hint"))
                        }
                    }
                }
            }
        }
        val start = 1_000_000_000L
        return try {
            fun shot(nanos: Long) = scene.render(nanos).encodeToData(EncodedImageFormat.PNG, 100)!!.bytes
            val folded = shot(start)
            probe.manualOpen = true
            // A transition needs a frame to be created and one more to take its
            // start time, so 16 and 66 ms are still the folded image and the
            // movement is under way at 116; 166 ms is 50 ms into it (the easing
            // has already covered most of the distance) and 266 ms is the end.
            shot(start + 16_000_000L)
            shot(start + 66_000_000L)
            shot(start + 116_000_000L)
            val mid = shot(start + 166_000_000L)
            val settled = shot(start + 700_000_000L)
            HandOver(folded = folded, mid = mid, settled = settled)
        } finally {
            scene.close()
        }
    }

    /** The mutable state the card and the form share, written between frames. */
    private class Probe {
        var manualOpen by mutableStateOf(false)
    }

    /** How many pixels of the state pill the head of [png] paints. */
    private fun pillInHero(png: ByteArray): Int {
        val band = bands(png).bands.firstOrNull() ?: return -1
        return count(javax.imageio.ImageIO.read(png.inputStream()), pillFill, band)
    }

    private fun render(accountName: String): ByteArray {
        val scene = ImageComposeScene(WIDTH, HEIGHT) {
            AppTheme(mode = ThemeMode.LIGHT, accent = Color(0xFFED5564)) {
                // Which primitive the badge is painted with is the theme's business.
                pillFill = MaterialTheme.colorScheme.surfaceContainerHighest.toArgb()
                Column(
                    Modifier
                        .fillMaxWidth()
                        .verticalScroll(rememberScrollState())
                        .padding(16.dp),
                ) {
                    GoogleSignInHero(language = "en", accountName = accountName)
                    GoogleSignInCard(
                        language = "en",
                        waiting = false,
                        saving = false,
                        manualOpen = false,
                        onStart = {},
                    )
                }
            }
        }
        return try {
            scene.render().encodeToData(EncodedImageFormat.PNG, 100)!!.bytes
        } finally {
            scene.close()
        }
    }

    /**
     * The card bands of a render, and the page runs between them.
     *
     * A row is "on a card" when most of its pixels are not the page colour, not
     * when one sample is: a single column crosses the button's label, and a white
     * glyph in the middle of the fill would cut the card in two.
     */
    private data class Bands(val bands: List<IntRange>, val seams: List<Int>)

    private fun bands(png: ByteArray): Bands {
        val image = javax.imageio.ImageIO.read(png.inputStream())
        val page = image.getRGB(image.width / 2, 2)
        val from = 20
        val to = image.width - 20
        val runs = mutableListOf<Pair<Boolean, Int>>()
        for (y in 0 until image.height) {
            var painted = 0
            for (x in from until to) {
                if (!near(image.getRGB(x, y), page)) painted++
            }
            val onCard = painted * 2 > (to - from)
            if (runs.isNotEmpty() && runs.last().first == onCard) {
                runs[runs.size - 1] = onCard to runs.last().second + 1
            } else {
                runs += onCard to 1
            }
        }
        val cardBands = mutableListOf<IntRange>()
        val seams = mutableListOf<Int>()
        var y = 0
        for ((onCard, length) in runs) {
            if (onCard) cardBands += y until (y + length) else if (cardBands.isNotEmpty()) seams += length
            y += length
        }
        return Bands(bands = cardBands, seams = seams.dropLast(1))
    }

    /**
     * The colour the window button is filled with, read off the folded card (its
     * first band): the most painted saturated colour there, so the form's own
     * button further down cannot be mistaken for it.
     */
    private fun buttonFill(png: ByteArray, bands: Bands): Int? {
        val image = javax.imageio.ImageIO.read(png.inputStream())
        val band = bands.bands.firstOrNull() ?: return null
        return dominantFill(image, band.first until band.last + 1)
    }

    /** How many pixels of [fill] [png] paints inside the card's first band. */
    private fun fillInCard(png: ByteArray, bands: Bands, fill: Int?): Int {
        val band = bands.bands.firstOrNull() ?: return 0
        if (fill == null) return 0
        return count(javax.imageio.ImageIO.read(png.inputStream()), fill, band.first until band.last + 1)
    }

    private fun ranges(bands: Bands): String =
        bands.bands.joinToString(", ", "[", "]") { "${it.first}..${it.last} (${it.last - it.first + 1})" }

    private fun near(a: Int, b: Int, tolerance: Int = 2): Boolean {
        val da = Math.abs(((a shr 16) and 255) - ((b shr 16) and 255))
        val db = Math.abs(((a shr 8) and 255) - ((b shr 8) and 255))
        val dc = Math.abs((a and 255) - (b and 255))
        return da <= tolerance && db <= tolerance && dc <= tolerance
    }

    private fun ink(image: java.awt.image.BufferedImage): Int {
        var ink = 0
        for (y in 0 until image.height) {
            for (x in 0 until image.width) {
                val rgb = image.getRGB(x, y)
                val r = (rgb shr 16) and 255
                val g = (rgb shr 8) and 255
                val b = rgb and 255
                if (r > 245 && g > 245 && b > 245) continue
                ink++
            }
        }
        return ink
    }

    /**
     * The most painted *saturated* colour in [rows]: the tile, not the surface it
     * sits on and not the text. The hero surface and the page are near-neutral,
     * so requiring a channel spread keeps them out, and the text is too thin to
     * reach the pixel floor.
     */
    private fun dominantFill(
        image: java.awt.image.BufferedImage,
        rows: IntRange,
    ): Int? {
        val counts = HashMap<Int, Int>()
        for (y in rows) {
            for (x in 0 until image.width) {
                val rgb = image.getRGB(x, y) or (0xFF shl 24)
                counts[rgb] = (counts[rgb] ?: 0) + 1
            }
        }
        return counts.entries
            .filter { it.value >= 200 }
            .filter { entry ->
                val r = (entry.key shr 16) and 255
                val g = (entry.key shr 8) and 255
                val b = entry.key and 255
                maxOf(r, g, b) - minOf(r, g, b) >= 40
            }
            .maxByOrNull { it.value }
            ?.key
    }

    /** Inclusive pixel bounds of [argb] within [rows]. */
    private fun bounds(
        image: java.awt.image.BufferedImage,
        argb: Int,
        rows: IntRange,
    ): IntArray? {
        var x0 = Int.MAX_VALUE
        var y0 = Int.MAX_VALUE
        var x1 = -1
        var y1 = -1
        for (y in rows) {
            for (x in 0 until image.width) {
                if (image.getRGB(x, y) != argb) continue
                if (x < x0) x0 = x
                if (x > x1) x1 = x
                if (y < y0) y0 = y
                if (y > y1) y1 = y
            }
        }
        return if (x1 < 0) null else intArrayOf(x0, y0, x1, y1)
    }

    private fun count(image: java.awt.image.BufferedImage, argb: Int, rows: IntRange): Int {
        var n = 0
        for (y in rows) {
            for (x in 0 until image.width) {
                if (image.getRGB(x, y) == argb) n++
            }
        }
        return n
    }

    private fun same(a: ByteArray, b: ByteArray): Boolean {
        val left = javax.imageio.ImageIO.read(a.inputStream())
        val right = javax.imageio.ImageIO.read(b.inputStream())
        if (left.width != right.width || left.height != right.height) return false
        for (y in 0 until left.height) {
            for (x in 0 until left.width) {
                if (left.getRGB(x, y) != right.getRGB(x, y)) return false
            }
        }
        return true
    }

    private fun hex(argb: Int): String = "#%08X".format(argb)
}
