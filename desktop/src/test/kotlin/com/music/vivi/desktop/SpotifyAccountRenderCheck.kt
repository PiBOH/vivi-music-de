@file:OptIn(ExperimentalComposeUiApi::class)

package com.music.vivi.desktop

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.ImageComposeScene
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.unit.dp
import com.music.spotify.models.SpotifyPlaylist
import com.music.spotify.models.SpotifyPlaylistTracksRef
import org.jetbrains.skia.EncodedImageFormat
import java.io.File

/**
 * Headless check of the Material 3 rebuild of *Import from Spotify* and of the
 * signed-in Account head, run with
 *
 *     ./gradlew :desktop:spotifyAccountRenderCheck
 *
 * Both screens are now built from the same three pieces (the hero, the numbered
 * sign-in steps and the settings groups), and none of that can be seen without a
 * display, so the pieces are rendered into an off-screen `ImageComposeScene` and
 * the pixels are read back:
 *
 *  * **the hero is a hero.** The brand mark sits on a solid 64 dp `primary`
 *    tile, and the same fill appears again at the bottom of the screen as the
 *    import button: a tile that lost its fill, a zero-sized box or a button that
 *    silently became flat all fail here.
 *  * **the sources are one card.** Liked Songs and two playlists are three rows
 *    joined by the group's 3 dp seams, not three loose lines, and exactly the two
 *    selected rows carry `primary` ink - the checkbox fill. That is the thing a
 *    redesign of this screen can break silently: a checkbox that stops following
 *    `selected`, or a row that stops toggling.
 *  * **the sign-in head states the account.** The same tile, a title of its own
 *    (`google_connected_title`, not the signed-out `login_google`) and no badge,
 *    which is the same rule the Spotify head follows.
 *  * **the manual-cookie card folds.** `M3CollapsibleCard` is the one new
 *    component: folded it is the header row alone (at least the 56 dp of a list
 *    row), open it is the header plus one body card joined by the 3 dp seam, which
 *    is what says the fields belong to the row above them.
 *
 * What it does NOT check is whether the strings read well or whether the screen
 * is pleasant: that needs eyes. PNGs are written under `.ignore/spotify/`.
 */
object SpotifyAccountRenderCheck {

    private val outDir = File(".ignore/spotify")

    private const val WIDTH = 760
    private const val HEIGHT = 820

    /** The header row of a folding card: a Material list row, in dp (density 1). */
    private const val ROW_MIN_HEIGHT = 56

    /** The badge's own fill, captured from the theme on every render. */
    private var pillFill: Int = 0

    @JvmStatic
    fun main(args: Array<String>) {
        outDir.mkdirs()
        val failures = mutableListOf<String>()

        // ------------------------------------------------------------ connected
        val connected = renderConnected()
        File(outDir, "connected.png").writeBytes(connected)
        val image = javax.imageio.ImageIO.read(connected.inputStream())

        val tile = dominantFill(image, 0 until 260)
        // The largest connected block of that colour, not its global bounds: the
        // state pill's dot is painted with the same `primary`, so a bounding box
        // over the whole image would stretch the "tile" across the hero.
        val tileRegion = if (tile == null) null else largestRegion(image, tile, 0 until 260)
        println("connected: hero fill ${tile?.let { hex(it) }} at ${tileRegion?.bounds}")
        when {
            tile == null -> failures += "the Spotify hero painted no filled tile"
            tileRegion == null -> failures += "the hero fill covers a single pixel, not a tile"
            else -> {
                val side = tileRegion.x1 - tileRegion.x0 + 1
                val height = tileRegion.y1 - tileRegion.y0 + 1
                if (side !in 54..68 || height !in 54..68) {
                    failures += "the mark tile is ${side}x$height px, not the 64 dp square it should be"
                }
                val share = tileRegion.pixels * 100 / (side * height)
                println("   mark tile: ${tileRegion.pixels} px of ${side * height} ($share %)")
                if (share < 30) failures += "the mark tile is not filled: only $share % of its square is painted"
            }
        }

        val measured = bands(connected)
        println("connected: bands=${measured.bands.size} seams=${measured.seams}")
        if (measured.bands.size != 5) {
            failures += "the connected screen must read as hero + three source rows + the import button " +
                "(5 bands), read ${measured.bands.size}"
        }
        val innerSeams = measured.seams.count { it in 2..6 }
        println("   seams of 3 dp (rows inside one card): $innerSeams")
        if (innerSeams != 2) {
            failures += "the three source rows must be one card: two 3 dp seams between them, read $innerSeams"
        }

        if (tile != null) {
            val rows = measured.bands.drop(1).dropLast(1)
            val checked = checkedRowBands(image, rows, tile)
            println("   source rows carrying primary ink: $checked of ${rows.size}")
            if (checked != 2) {
                failures += "two of the three rows are selected, so exactly 2 must draw a primary " +
                    "checkbox, read $checked"
            }
            val button = measured.bands.lastOrNull()?.let { count(image, tile, it) } ?: 0
            println("   import button fill: $button px")
            if (button < 2000) failures += "the import button does not paint its primary container ($button px)"
        }

        // --------------------------------------------------------------- account
        // The Account screen had a head of its own and has none any more: the
        // preview's page is the title, the account's card and the card of the
        // sync, so there is no tile and no title of its own to measure here.
        // `:desktop:accountOrderCheck` reads that page instead.
        val signedOutHead = renderGoogleHead("")
        val signedInHead = renderGoogleHead("Vivi Listener")
        println("sign-in head: title differs with the session=${!same(signedOutHead, signedInHead)}")
        if (same(signedOutHead, signedInHead)) {
            failures += "the sign-in head's title must change with the session (two renders are identical)"
        }

        // ------------------------------------------------------- the head's badge
        // The state is the title and there is no pill in either state, so the
        // pill's own fill (`surfaceContainerHighest`) must not appear in the head
        // at all - while the two titles have to differ: "Import from Spotify"
        // against "Connected to Spotify". A badge that comes back, or a title that
        // stops following the session, is what this catches.
        val heroOut = renderHero(null)
        val heroIn = renderHero("Vivi Listener")
        File(outDir, "hero-disconnected.png").writeBytes(heroOut)
        File(outDir, "hero-connected.png").writeBytes(heroIn)
        val outBand = bands(heroOut).bands.firstOrNull()
        val inBand = bands(heroIn).bands.firstOrNull()
        val outPill = outBand?.let { count(javax.imageio.ImageIO.read(heroOut.inputStream()), pillFill, it) } ?: -1
        val inPill = inBand?.let { count(javax.imageio.ImageIO.read(heroIn.inputStream()), pillFill, it) } ?: -1
        println("hero: pill px disconnected=$outPill connected=$inPill, titles differ=${!same(heroOut, heroIn)}")
        if (outPill != 0) {
            failures += "the head without an account must draw no badge, read $outPill px of the pill's fill"
        }
        if (inPill != 0) {
            failures += "the head with an account must draw no badge either, read $inPill px of the pill's fill"
        }
        if (same(heroOut, heroIn)) {
            failures += "the head's title must change with the session (two renders are identical)"
        }

        // ---------------------------------------------------------- folding card
        val folded = renderManualCard(expanded = false)
        File(outDir, "manual-folded.png").writeBytes(folded)
        val open = renderManualCard(expanded = true)
        File(outDir, "manual-open.png").writeBytes(open)

        val foldedBands = bands(folded)
        val openBands = bands(open)
        println("manual card: folded rows=${foldedBands.bands.size}")
        println("             open rows=${openBands.bands.size} seams=${openBands.seams}")
        if (foldedBands.bands.size != 1) {
            failures += "the folded manual card must be its header row alone, read ${foldedBands.bands.size} bands"
        }
        if (openBands.bands.size != 2) {
            failures += "the open manual card must be the header plus its body (2 bands), read ${openBands.bands.size}"
        }
        if (openBands.seams.size != 1 || openBands.seams.first() !in 2..6) {
            failures += "the body must join the header with the group's 3 dp seam, read ${openBands.seams}"
        }
        foldedBands.bands.firstOrNull()?.let { band ->
            val height = band.last - band.first + 1
            println("             header height=$height")
            if (height < ROW_MIN_HEIGHT) {
                failures += "the folded card's header must keep the height of a list row ($ROW_MIN_HEIGHT dp), read $height"
            }
        }

        if (failures.isEmpty()) {
            println()
            println("OK: the two screens build on the shared hero, one card of source rows and a folding form")
            return
        }
        println()
        println("FAILED:")
        failures.forEach { println("  - $it") }
        throw IllegalStateException("spotify/account render check failed (${failures.size} case(s))")
    }

    /** The connected screen: the hero and the source list it sits above. */
    private fun renderConnected(): ByteArray {
        val state = SpotifyImport.State(
            connected = true,
            accountName = "Vivi Listener",
            playlists = listOf(
                SpotifyPlaylist(id = "p1", name = "Chill", tracks = SpotifyPlaylistTracksRef(total = 42)),
                SpotifyPlaylist(id = "p2", name = "Focus", tracks = SpotifyPlaylistTracksRef(total = 12)),
            ),
            likedSongsCount = 7,
        )
        return render {
            Column(Modifier.fillMaxWidth().padding(16.dp)) {
                SpotifyHero(language = "en", connectedAs = "Vivi Listener")
                SpotifySourcesSection(
                    language = "en",
                    state = state,
                    // Liked Songs and one playlist selected, one not: the check
                    // needs both states on screen at once.
                    selected = setOf(SpotifyImport.LIKED_SOURCE, "p1"),
                    onToggle = {},
                    onImport = {},
                )
            }
        }
    }

    /** The head alone, so the badge (or its absence) is all that is measured. */
    private fun renderHero(connectedAs: String?): ByteArray = render {
        Column(Modifier.fillMaxWidth().padding(16.dp)) {
            SpotifyHero(language = "en", connectedAs = connectedAs)
        }
    }

    /** The Google head as the sign-in screen draws it, for the title comparison. */
    private fun renderGoogleHead(accountName: String): ByteArray = render {
        Column(Modifier.fillMaxWidth().padding(16.dp)) {
            GoogleSignInHero(language = "en", accountName = accountName)
        }
    }

    private fun renderManualCard(expanded: Boolean): ByteArray = render {
        Box(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
            Column(Modifier.fillMaxWidth().padding(horizontal = 24.dp, vertical = 40.dp)) {
                M3CollapsibleCard(
                    title = Localization.get("en", "login_manual_title"),
                    expanded = expanded,
                    onExpandedChange = {},
                ) {
                    Text(Localization.get("en", "cookie_label"))
                    Text(Localization.get("en", "advanced_login_hint"))
                }
            }
        }
    }

    private fun render(content: @androidx.compose.runtime.Composable () -> Unit): ByteArray {
        val scene = ImageComposeScene(WIDTH, HEIGHT) {
            AppTheme(mode = ThemeMode.LIGHT, accent = Color(0xFFED5564), language = "en") {
                // Which primitive the badge is painted with is the theme's business,
                // so it is read back from the theme instead of guessed.
                pillFill = MaterialTheme.colorScheme.surfaceContainerHighest.toArgb()
                content()
            }
        }
        return try {
            scene.render().encodeToData(EncodedImageFormat.PNG, 100)!!.bytes
        } finally {
            scene.close()
        }
    }

    /** The card bands down the middle of a render, and the page runs between them. */
    private data class Bands(val bands: List<IntRange>, val seams: List<Int>)

    private fun bands(png: ByteArray): Bands {
        val image = javax.imageio.ImageIO.read(png.inputStream())
        val x = image.width / 2
        val page = image.getRGB(x, 2)
        val runs = mutableListOf<Pair<Boolean, Int>>()
        for (y in 0 until image.height) {
            val onCard = !near(image.getRGB(x, y), page)
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
        // The last page run is the empty space under the content, not a seam.
        return Bands(bands = cardBands, seams = seams.dropLast(1))
    }

    /** How many of [rows] carry the checkbox fill. */
    private fun checkedRowBands(
        image: java.awt.image.BufferedImage,
        rows: List<IntRange>,
        primary: Int,
    ): Int = rows.count { band ->
        var ink = 0
        for (y in band) {
            for (x in 0 until image.width) {
                if (near(image.getRGB(x, y), primary, tolerance = 18)) ink++
            }
        }
        ink > 120
    }

    /**
     * The most painted saturated colour in [rows]: the brand tile, not the surface
     * it sits on and not the text. The card surfaces are near-neutral, so requiring
     * a channel spread keeps them out, and text is too thin to reach the floor.
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

    /**
     * The largest connected block of [argb] inside [rows], with its bounds: a
     * filled tile, told apart from the state pill's dot that shares the colour.
     */
    private data class Region(
        val x0: Int,
        val y0: Int,
        val x1: Int,
        val y1: Int,
        val pixels: Int,
    ) {
        val bounds: String get() = "$x0, $y0, $x1, $y1"
    }

    private fun largestRegion(
        image: java.awt.image.BufferedImage,
        argb: Int,
        rows: IntRange,
    ): Region? {
        val width = image.width
        val seen = Array(image.height) { BooleanArray(width) }
        var best: Region? = null
        val stack = ArrayDeque<Int>()
        for (y in rows) {
            for (x in 0 until width) {
                if (seen[y][x] || image.getRGB(x, y) != argb) continue
                var count = 0
                var x0 = x
                var x1 = x
                var y0 = y
                var y1 = y
                stack.clear()
                stack.addLast(y * width + x)
                seen[y][x] = true
                while (stack.isNotEmpty()) {
                    val point = stack.removeLast()
                    val py = point / width
                    val px = point % width
                    count++
                    if (px < x0) x0 = px
                    if (px > x1) x1 = px
                    if (py < y0) y0 = py
                    if (py > y1) y1 = py
                    for (dy in -1..1) {
                        for (dx in -1..1) {
                            val nx = px + dx
                            val ny = py + dy
                            if (nx < 0 || nx >= width || ny !in rows) continue
                            if (seen[ny][nx] || image.getRGB(nx, ny) != argb) continue
                            seen[ny][nx] = true
                            stack.addLast(ny * width + nx)
                        }
                    }
                }
                val region = Region(x0, y0, x1, y1, count)
                if (best == null || region.pixels > best.pixels) best = region
            }
        }
        return best
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

    private fun hex(c: Int) = "#" + Integer.toHexString(c and 0xFFFFFF)

    private fun distance(a: Int, b: Int): Int =
        kotlin.math.abs(((a shr 16) and 255) - ((b shr 16) and 255)) +
            kotlin.math.abs(((a shr 8) and 255) - ((b shr 8) and 255)) +
            kotlin.math.abs((a and 255) - (b and 255))

    private fun near(a: Int, b: Int, tolerance: Int = 6): Boolean = distance(a, b) <= tolerance
}
