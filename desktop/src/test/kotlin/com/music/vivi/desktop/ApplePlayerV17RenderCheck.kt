@file:OptIn(ExperimentalComposeUiApi::class)

package com.music.vivi.desktop

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.ImageComposeScene
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Surface
import androidx.compose.ui.Alignment
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.layout.widthIn
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.unit.dp
import com.music.innertube.YouTube
import com.music.innertube.models.BrowseEndpoint
import com.music.innertube.pages.MoodAndGenres
import com.music.vivi.desktop.player.PlaybackTech
import com.music.vivi.desktop.player.RepeatMode
import org.jetbrains.skia.EncodedImageFormat
import java.awt.image.BufferedImage
import java.io.File

/**
 * Headless render of "Apple Player v17", run with
 *
 *     ./gradlew :desktop:applePlayerV17RenderCheck
 *
 * It writes `wide.png`, `wide-controls.png`, `wide-lyrics.png`, `wide-menu.png`,
 * `wide-queue.png`, `wide-stats.png`, `medium.png`, `narrow.png`,
 * `narrow-queue.png`, `minimum.png`, `minimum-queue.png`, `mini-wide.png` and
 * `mini-compact.png` under `desktop/.ignore/render-v17/`.
 *
 * The player is a two-column layout over an artwork, and both halves have to be
 * seen to know they were drawn: the artwork and its statistics line on the left,
 * the caption, title, artist, lyric row, scrubber, transport and volume on the
 * right, and the queue taking that right column when it is open. Below 820 dp
 * the columns stack, which is the case a wide-only render would never show.
 *
 * No display is attached, so every size is painted into an off-screen
 * `ImageComposeScene`, written under `.ignore/render-v17/` and read back:
 *
 *  * wide (1280x800), medium (1024x680), narrow (700x800) and the minimum
 *    (860x560) all paint a darkened screen with ink on both the artwork and the
 *    controls;
 *  * the wide render with the queue open paints the queue in the right column
 *    instead of the controls;
 *  * the bar's playback row (Shuffle, Cycle, Autoplay, Queue) and the large
 *    lyrics page that replaces the controls are painted and differ from the
 *    plain frame;
 *  * the "Stats for nerds" render differs from the same frame without it, which
 *    is what says the overlay is really on the artwork;
 *  * the mini player paints its transport at 1100 px and at 640 px, where it
 *    drops the shuffle, repeat and volume;
 *  * a press on the mini player's title reaches the bar and opens the player,
 *    which is what the hover tooltip wrapping each text used to swallow.
 */
object ApplePlayerV17RenderCheck {

    private val outDir = File(".ignore/render-v17")

    /** The artwork figures are read as numbers, so they keep the decimal point. */
    private val EN = java.util.Locale.ROOT

    /**
     * Where the fixture's cover comes from: a real album sleeve at the 1200 px
     * the provider serves for it (the record the fixture's own track belongs
     * to). It is downloaded once and kept beside the renders so the check stays
     * offline: what it proves is the pixels the app decodes and the size it
     * paints them at, not a network fetch.
     */
    private val ARTWORK_SOURCE =
        "https://is1-ssl.mzstatic.com/image/thumb/Music115/v4/c2/a2/38/" +
            "c2a2384d-8c55-8180-53ed-88b152f82db4/743217932756.jpg/1200x1200bb.jpg"

    /**
     * The cover itself, as cover material and nothing else: a player screenshot
     * here would paint a thumbnail of the interface into the artwork slot and
     * the backdrop of every frame instead of a sleeve.
     */
    private val artworkFile: File by lazy {
        File(".ignore/render-v17/cover-hires.jpg").also { file ->
            if (!file.isFile) downloadArtwork(file)
            check(file.isFile) {
                "artwork fixture missing: ${file.absolutePath}; the fixture must be a real cover " +
                    "at its native size ($ARTWORK_SOURCE), never a screenshot of the app"
            }
        }
    }

    /** The pixels a card's sleeve is drawn across: the card is 210 dp wide. */
    private const val CARD_DRAW_PX = 210

    private val http: java.net.http.HttpClient by lazy {
        java.net.http.HttpClient.newBuilder()
            .followRedirects(java.net.http.HttpClient.Redirect.NORMAL)
            .connectTimeout(java.time.Duration.ofSeconds(20))
            .build()
    }

    /**
     * The fixture the check needs, fetched from the provider when it is not
     * already beside the renders: a clean machine runs the check without a
     * hand-placed file. The bytes are the sleeve at the size the provider serves
     * for it and are written untouched, so the report still measures the
     * decoded size of the real source and not a resize this task did.
     */
    private fun downloadArtwork(target: File) {
        target.parentFile?.mkdirs()
        println("artwork fixture: missing, downloading $ARTWORK_SOURCE")
        val request = java.net.http.HttpRequest.newBuilder(java.net.URI.create(ARTWORK_SOURCE))
            .timeout(java.time.Duration.ofSeconds(40))
            .header("User-Agent", "vivi-music-desktop-render-check")
            .GET()
            .build()
        val response = http.send(request, java.net.http.HttpResponse.BodyHandlers.ofByteArray())
        check(response.statusCode() == 200) {
            "the artwork download answered HTTP ${response.statusCode()} for $ARTWORK_SOURCE"
        }
        val body = response.body()
        check(body.size > 20_000) { "the artwork download returned ${body.size} bytes, which is no sleeve" }
        target.writeBytes(body)
        val decoded = org.jetbrains.skia.Image.makeFromEncoded(body)
        check(decoded.width >= 1024) {
            "the downloaded sleeve is only ${decoded.width} px wide, below the 1024 a cover has to reach"
        }
        println("artwork fixture: downloaded %d bytes, %dx%d".format(body.size, decoded.width, decoded.height))
    }

    /** The artwork behind a URL, decoded, or null when it cannot be read. */
    private fun decodeUrl(url: String): org.jetbrains.skia.Image? = try {
        val request = java.net.http.HttpRequest.newBuilder(java.net.URI.create(url))
            .timeout(java.time.Duration.ofSeconds(30))
            .header("User-Agent", "vivi-music-desktop-render-check")
            .GET()
            .build()
        val response = http.send(request, java.net.http.HttpResponse.BodyHandlers.ofByteArray())
        if (response.statusCode() != 200) null else org.jetbrains.skia.Image.makeFromEncoded(response.body())
    } catch (e: Exception) {
        println("  artwork unreadable: $url (${e.message})")
        null
    }

    private val artwork: String by lazy {
        val file = artworkFile
        "file:///" + file.absolutePath.replace('\\', '/').replace(" ", "%20")
    }

    /** The 1220x780 player reference: the frame the renders are read against, never artwork. */
    private val referenceFrame: File? by lazy {
        listOf(
            File("../.ignore/BitChord/vivimusic vs bitchord/apple player v17 come dovrebbe essere fatto 1.png"),
            File(".ignore/BitChord/vivimusic vs bitchord/apple player v17 come dovrebbe essere fatto 1.png"),
        ).firstOrNull { it.isFile }
    }

    /** Timed lines around the frame's 1:21, so one of them is the current one. */
    private val LYRICS = """
        [00:00.00] Around the world, around the world
        [00:12.00] Around the world, around the world
        [00:24.00] Around the world, around the world
        [00:36.00] Around the world, around the world
        [00:48.00] Around the world, around the world
        [01:00.00] Around the world, around the world
        [01:12.00] Around the world, around the world
        [01:20.00] La la la la la la la la la la
        [01:26.00] La la la la la la la la la la
        [01:32.00] La la la la la la la la la la
        [01:38.00] La la la la la la la la la la
        [01:44.00] Around the world, around the world
    """.trimIndent()

    private fun track(index: Int = 0) = NowPlaying(
        videoId = "v17Render$index",
        title = if (index == 0) "Around the World (La La La La La)" else "Track $index of the queue",
        artist = if (index == 0) "A Touch of Class" else "Artist $index",
        album = "Planet Pop",
        durationMs = 234_000,
        thumbnail = artwork,
    )

    private fun queue() = List(6) { track(it) }

    @JvmStatic
    fun main(args: Array<String>) {
        outDir.mkdirs()
        // A throwaway home: turning "Stats for nerds" on has to go through the
        // real settings file, and it must not touch the user's own one.
        val tempHome = File(outDir, "home")
        // A fresh home on every run: the previous run left `statsForNerds` on,
        // which drew the overlay over every frame of this one.
        tempHome.deleteRecursively()
        tempHome.mkdirs()
        System.setProperty("user.home", tempHome.absolutePath)

        val rows = queue()
        // The lyrics page is drawn from the app's own cache and parser, so the
        // frame shows verses instead of a spinner: the lines are seeded through
        // `LyricsCache.put`, the same entry point the providers write to.
        LyricsCache.put(
            rows[0].videoId,
            LYRICS,
            preferSynced = true,
        )

        val failures = mutableListOf<String>()

        artworkReport(rows, failures)

        val wide = render(1280, 800, "wide.png", rows)
        val wideQueue = render(1280, 800, "wide-queue.png", rows, queueOpen = true)
        val medium = render(1024, 680, "medium.png", rows)
        val narrow = render(700, 800, "narrow.png", rows)
        val narrowQueue = render(700, 800, "narrow-queue.png", rows, queueOpen = true)
        val minimum = render(860, 560, "minimum.png", rows)
        val minimumQueue = render(860, 560, "minimum-queue.png", rows, queueOpen = true)

        // The two surfaces this check exists for now: the bar's playback row
        // (Shuffle, Cycle, Autoplay, Queue) and the large lyrics page taking the
        // controls' place. Both are drawn from a state the screen takes as a
        // parameter, so the frames are the real composables and not a mock-up.
        val wideMenu = renderMenu(rows)
        val wideControls = render(1280, 800, "wide-controls.png", rows, controlsRow = true)
        val wideLyrics = render(1280, 800, "wide-lyrics.png", rows, lyricsOpen = true)

        PlaybackTech.stream("https://example.invalid/stream?itag=140&mime=audio%2Fmp4")
        PlaybackTech.output(48000, 16, 2, 1000, 2000)
        DesktopSettings.update { it.copy(statsForNerds = true) }
        val wideStats = render(1280, 800, "wide-stats.png", rows)
        val statsInk = differingPixels(wide, wideStats)

        val miniWide = mini(1100, 96, "mini-wide.png", rows[0])
        val miniCompact = mini(640, 96, "mini-compact.png", rows[0])

        data class Case(val name: String, val image: BufferedImage, val artInk: Int, val controlsInk: Int)
        val cases = listOf(
            Case("1280x800", wide, ink(wide, 0.06, 0.42, 0.10, 0.90), ink(wide, 0.48, 0.97, 0.10, 0.90)),
            Case("1280x800 queue", wideQueue, ink(wideQueue, 0.06, 0.42, 0.10, 0.90), ink(wideQueue, 0.48, 0.97, 0.10, 0.90)),
            Case("1024x680", medium, ink(medium, 0.06, 0.42, 0.10, 0.90), ink(medium, 0.48, 0.97, 0.10, 0.90)),
            Case("700x800", narrow, ink(narrow, 0.10, 0.90, 0.04, 0.45), ink(narrow, 0.10, 0.90, 0.50, 0.99)),
            Case("860x560", minimum, ink(minimum, 0.10, 0.90, 0.04, 0.45), ink(minimum, 0.10, 0.90, 0.50, 0.99)),
            Case("860x560 queue", minimumQueue, ink(minimumQueue, 0.06, 0.42, 0.10, 0.90), ink(minimumQueue, 0.48, 0.97, 0.10, 0.90)),
        )

        cases.forEach { case ->
            val brightness = meanLuminance(case.image)
            val miniInk = ink(case.image, 0.0, 1.0, 0.0, 1.0)
            println("%-16s brightness %.2f, artwork ink %d, controls ink %d".format(case.name, brightness, case.artInk, case.controlsInk))
            if (brightness > 0.62) failures += "${case.name}: the screen is not dark (%.2f), the backdrop and its scrim are missing".format(brightness)
            if (case.artInk < 500) failures += "${case.name}: the artwork drew almost no ink (${case.artInk} px)"
            if (case.controlsInk < 200) failures += "${case.name}: the control column drew almost no ink (${case.controlsInk} px), so the title, the scrubber or the transport are missing"
            if (miniInk < 1000) failures += "${case.name}: the frame is almost empty (${miniInk} ink px in total)"
        }

        val narrowQueueDiff = differingPixels(narrow, narrowQueue)
        println("queue under 820 dp: %d pixels differ from the plain player".format(narrowQueueDiff))
        if (narrowQueueDiff != 0) {
            failures += "under 820 dp the queue replaced the column instead of leaving it to the queue screen ($narrowQueueDiff px)"
        }

        val menuDiff = differingPixels(wide, wideMenu)
        println("three-dot menu: %d pixels differ from the frame without it".format(menuDiff))
        if (menuDiff < 1500) {
            failures += "the open menu drew almost nothing over the player ($menuDiff px)"
        }

        val controlsDiff = differingPixels(wide, wideControls)
        val lyricsDiff = differingPixels(wide, wideLyrics)
        println("playback row: %d pixels differ from the page row".format(controlsDiff))
        println("inline lyrics: %d pixels differ from the controls".format(lyricsDiff))
        if (controlsDiff < 200) {
            failures += "the playback row drew almost nothing over the page row ($controlsDiff px)"
        }
        if (lyricsDiff < 2000) {
            failures += "the lyrics page drew almost nothing over the controls ($lyricsDiff px)"
        }

        println("stats overlay: %d pixels differ from the frame without it".format(statsInk))
        if (statsInk < 100) {
            failures += "the Stats for nerds overlay changed almost nothing ($statsInk px): the line is not drawn on the artwork"
        }

        val titlePress = miniPressOpensPlayer(rows[0])
        println("mini player press: the title opened the player %d time(s)".format(titlePress))
        if (titlePress < 1) {
            failures += "pressing the mini player title did not open the player ($titlePress call(s))"
        }

        listOf("mini player 1100" to miniWide, "mini player 640" to miniCompact).forEach { (name, image) ->
            val miniInk = ink(image, 0.0, 1.0, 0.0, 1.0)
            println("%-16s ink %d".format(name, miniInk))
            if (miniInk < 400) failures += "$name drew almost no ink ($miniInk px)"
        }

        if (failures.isEmpty()) {
            println()
            println("OK: the artwork, the controls, the queue, the stats line and both mini player widths are painted")
            return
        }
        println()
        println("FAILED:")
        failures.forEach { println("  - $it") }
        throw IllegalStateException("Apple Player v17 render check failed (${failures.size} case(s))")
    }

    private fun playerComposable(
        rows: List<NowPlaying>,
        queueOpen: Boolean,
        lyricsOpen: Boolean = false,
        controlsRow: Boolean = false,
    ): @Composable () -> Unit = {
        ApplePlayerV17Screen(
            np = rows[0],
            isPlaying = true,
            isLoading = false,
            positionMs = 81_000,
            durationMs = 234_000,
            volume = 0.72f,
            isShuffle = true,
            repeatMode = RepeatMode.ALL,
            errorKey = null,
            errorDetail = null,
            onTogglePlay = {},
            onNext = {},
            onPrevious = {},
            onSeek = {},
            onVolume = {},
            onToggleShuffle = {},
            onCycleRepeat = {},
            language = "en",
            onOpenLyrics = {},
            onOpenQueue = {},
            onAddToPlaylist = {},
            sliderStyle = ViviSliderStyle.SLIM,
            onBack = {},
            queue = rows,
            queueIndex = 0,
            initialQueueOpen = queueOpen,
            initialLyricsOpen = lyricsOpen,
            initialControlsRow = controlsRow,
        )
    }

    /**
     * The player with the three-dot menu open. Its entries come from
     * `V17PlayerMenuContent`, the block the real menu mounts inside its popup, so
     * the picture cannot drift from the menu itself.
     */

    /**
     * What the artwork frames actually load: the source the card or the player
     * asks the loader for, the pixels it decodes to, and the pixels it is drawn
     * at. The rule is the mobile one - at least twice the drawn size, or the
     * largest variant the provider has.
     */
    private fun artworkReport(rows: List<NowPlaying>, failures: MutableList<String>) {
        println("artwork source:  %s".format(ARTWORK_SOURCE))
        println("artwork fixture: %s".format(artworkFile.absolutePath))
        artworkIsACover(failures)

        // What the app actually asks the provider for: a mood page hands out a
        // 120 px crop, and both the card and the player rewrite it upwards, so
        // the loader never receives the small variant.
        val smallMood = "https://lh3.googleusercontent.com/moodArt=w120-h120-l90-rj"
        println("mood source -> card request:  %s".format(adjustedThumbnailUrl(smallMood, 640)))
        println("mood source -> player request: %s".format(adjustedThumbnailUrl(smallMood, 1200)))
        val ytSmall = "https://i.ytimg.com/vi/dQw4w9WgXcQ/hqdefault.jpg"
        println("ytimg source -> player request: %s".format(adjustedThumbnailUrl(ytSmall, 1200)))

        val decoded = org.jetbrains.skia.Image.makeFromEncoded(artworkFile.readBytes())
        // The narrow frame (700x800) draws the cover at 0.36 of the height, 288
        // px: it is the tightest artwork slot the player has, so it is the one a
        // source has to beat by 2x to be sharp everywhere. The wide frame draws
        // it at 512 px and is reported too, for the record.
        val playerDraw = 288
        val wideDraw = 512
        val playerRatio = decoded.width.toFloat() / playerDraw
        println(
            "player artwork: %s -> %dx%d decoded, drawn at %d px (%.2fx)".format(
                EN,
                ARTWORK_SOURCE, decoded.width, decoded.height, playerDraw, playerRatio,
            ),
        )
        println(
            "player artwork at the wide frame: %dx%d decoded, drawn at %d px (%.2fx)%s".format(
                EN,
                decoded.width,
                decoded.height,
                wideDraw,
                decoded.width.toFloat() / wideDraw,
                if (decoded.width.toFloat() / wideDraw < 2f) " max available" else "",
            ),
        )
        if (playerRatio < 2f) {
            println("player artwork: max available (%dx%d) at the narrow frame".format(decoded.width, decoded.height))
        }

        // An Explore card: 210x118 dp, so ~315 px of artwork across at 1.5x.
        val item = MoodAndGenres.Item(
            title = "Chill",
            stripeColor = 0xFF334455,
            endpoint = BrowseEndpoint(browseId = "FEmusic_moods", params = null),
        )
        MoodArtworkCache.seed(item, artwork)
        val cardUrl = adjustedThumbnailUrl(
            MoodArtworkCache.cached(item),
            requestedPx = 640,
            dataSaver = false,
        )
        val cardDecoded = org.jetbrains.skia.Image.makeFromEncoded(artworkFile.readBytes())
        // 210 dp of card at density 1, which is what the harness scene draws at.
        val cardDraw = 210
        val cardRatio = cardDecoded.width.toFloat() / cardDraw
        println(
            "explore card artwork: %s -> %dx%d decoded, drawn at %d px (%.2fx)".format(
                EN,
                ARTWORK_SOURCE, cardDecoded.width, cardDecoded.height, cardDraw, cardRatio,
            ),
        )
        println("explore card request: %s".format(cardUrl))
        if (cardRatio < 2f) {
            println("explore card artwork: max available (%dx%d)".format(cardDecoded.width, cardDecoded.height))
        }

        // And the card really paints it: one frame of the tile itself.
        val scene = ImageComposeScene(320, 200) {
            AppTheme(mode = ThemeMode.DARK, accent = Color(0xFFED5564), language = "en") {
                Box(Modifier.fillMaxSize()) {
                    MoodTile(item = item, ink = Color.White, onClick = {})
                }
            }
        }
        val inkPx = try {
            repeat(4) {
                scene.render()
                Thread.sleep(120)
            }
            val png = scene.render().encodeToData(EncodedImageFormat.PNG, 100)!!.bytes
            javax.imageio.ImageIO.read(png.inputStream())
        } finally {
            scene.close()
        }.let { ink(it, 0.0, 1.0, 0.0, 1.0) }
        println("explore card frame: %d ink px".format(inkPx))
        if (inkPx < 1000) {
            failures += "the Explore card painted almost nothing ($inkPx px), so its artwork is missing"
        }

        exploreCardsReport(failures)
    }

    /**
     * Every mood and genre card of the Explore grid, measured one by one.
     *
     * The card asks the service's own page for the picture it carries and then
     * rewrites it to the size it draws, so the figures are the ones the app
     * really loads: the request URL, the pixels it decodes to, the 210 px the
     * card is across, and their ratio. A page that hands out nothing is named as
     * such; a card whose sleeve is smaller than the 2x rule is reported as
     * "max available" with its cap, because there is nothing larger to ask for.
     */
    private fun exploreCardsReport(failures: MutableList<String>) {
        val sections = kotlinx.coroutines.runBlocking { YouTube.moodAndGenres() }.getOrElse { error ->
            failures += "the Explore card report could not read the moods and genres: ${error.message}"
            return
        }
        val items = sections.flatMap { it.items }
        println(
            "explore cards: %d mood(s) and genre(s) in %d section(s)".format(items.size, sections.size),
        )
        val pool = java.util.concurrent.Executors.newFixedThreadPool(8)
        data class Measured(val title: String, val request: String?, val decoded: org.jetbrains.skia.Image?)
        val measured = try {
            items.map { item ->
                pool.submit(
                    java.util.concurrent.Callable {
                        val source = kotlinx.coroutines.runBlocking {
                            YouTube.moodArtwork(item.endpoint.browseId, item.endpoint.params)
                        }.getOrNull()
                        if (source == null) {
                            return@Callable Measured(item.title, null, null)
                        }
                        MoodArtworkCache.seed(item, source)
                        val request = adjustedThumbnailUrl(source, requestedPx = 640, dataSaver = false)
                        Measured(item.title, request, request?.let { decodeUrl(it) })
                    },
                )
            }.map { it.get() }
        } finally {
            pool.shutdown()
        }

        var sharp = 0
        var capped = 0
        var empty = 0
        measured.forEach { card ->
            when {
                card.request == null -> {
                    empty++
                    println("explore card %-24s no artwork on its page".format(card.title))
                }
                card.decoded == null ->
                    failures += "explore card ${card.title}: the artwork did not decode (${card.request})"
                else -> {
                    val ratio = card.decoded.width.toFloat() / CARD_DRAW_PX
                    val tag = if (ratio < 2f) {
                        capped++
                        " max available"
                    } else {
                        sharp++
                        ""
                    }
                    println(
                        "explore card %-24s %s -> %dx%d decoded, drawn at %d px (%.2fx)%s".format(
                            EN,
                            card.title,
                            card.request,
                            card.decoded.width,
                            card.decoded.height,
                            CARD_DRAW_PX,
                            ratio,
                            tag,
                        ),
                    )
                }
            }
        }
        println(
            "explore cards: $sharp at 2x or better, $capped at the provider's cap, $empty without artwork",
        )
    }

    /**
     * A frame is not artwork. The fixture is rejected when it is the player
     * reference (byte for byte, or at the reference's own 1220x780), when it is
     * not square like a sleeve, or when it is smaller than the 1024 px a cover
     * has to reach for the slots to be sharp: every artwork line below reads it,
     * so a wrong fixture would make the whole report meaningless.
     */
    private fun artworkIsACover(failures: MutableList<String>) {
        val bytes = artworkFile.readBytes()
        val decoded = org.jetbrains.skia.Image.makeFromEncoded(bytes)
        val reference = referenceFrame
        val sameAsReference = reference != null &&
            reference.length() == bytes.size.toLong() &&
            reference.readBytes().contentEquals(bytes)
        val referenceSized = decoded.width == 1220 && decoded.height == 780
        if (sameAsReference || referenceSized) {
            failures += "the artwork fixture is the player reference (${decoded.width}x${decoded.height}), " +
                "not a cover: every artwork slot would show a screenshot of the interface"
        }
        val aspect = decoded.width.toFloat() / decoded.height
        if (aspect < 0.95f || aspect > 1.05f) {
            failures += "the artwork fixture is not square (${decoded.width}x${decoded.height}), so it is not a cover"
        }
        if (decoded.width < 1024) {
            failures += "the artwork fixture is only ${decoded.width} px wide: a cover at its native size has to reach 1024"
        }
        println(
            "artwork fixture check: %dx%d, square %s, source of reference %s".format(
                EN,
                decoded.width,
                decoded.height,
                if (aspect in 0.95f..1.05f) "yes" else "no",
                if (sameAsReference) "yes" else "no",
            ),
        )
    }

    private fun renderMenu(rows: List<NowPlaying>): BufferedImage {
        val scene = ImageComposeScene(1280, 800) {
            AppTheme(mode = ThemeMode.DARK, accent = Color(0xFFED5564), language = "en") {
                Box(Modifier.fillMaxSize()) {
                    playerComposable(rows, queueOpen = false)()
                    Surface(
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .padding(top = 196.dp, end = 30.dp)
                            .widthIn(max = 260.dp),
                        shape = MaterialTheme.shapes.extraSmall,
                        color = MaterialTheme.colorScheme.surfaceContainer,
                        tonalElevation = 3.dp,
                        shadowElevation = 6.dp,
                    ) {
                        Column(Modifier.padding(vertical = 4.dp)) {
                            V17PlayerMenuContent(
                                language = "en",
                                liked = true,
                                downloaded = false,
                                sleepTimerMinutes = null,
                                onLike = {},
                                onDislike = {},
                                onAddToPlaylist = {},
                                onDownload = {},
                                onDeleteDownload = {},
                                onStartRadio = {},
                                onPlayNext = {},
                                onAddToQueue = {},
                                onOpenAlbum = {},
                                onOpenArtist = {},
                                onSleepTimer = {},
                                onShare = {},
                            )
                        }
                    }
                }
            }
        }
        val png = try {
            repeat(5) {
                scene.render()
                Thread.sleep(120)
            }
            scene.render().encodeToData(EncodedImageFormat.PNG, 100)!!.bytes
        } finally {
            scene.close()
        }
        File(outDir, "wide-menu.png").writeBytes(png)
        return javax.imageio.ImageIO.read(png.inputStream())
    }

    private fun render(
        width: Int,
        height: Int,
        name: String,
        rows: List<NowPlaying>,
        queueOpen: Boolean = false,
        lyricsOpen: Boolean = false,
        controlsRow: Boolean = false,
    ): BufferedImage {
        val scene = ImageComposeScene(width, height) {
            AppTheme(mode = ThemeMode.DARK, accent = Color(0xFFED5564), language = "en") {
                Box(Modifier.fillMaxSize()) {
                    playerComposable(rows, queueOpen, lyricsOpen, controlsRow)()
                }
            }
        }
        val png = try {
            repeat(5) {
                scene.render()
                Thread.sleep(120)
            }
            scene.render().encodeToData(EncodedImageFormat.PNG, 100)!!.bytes
        } finally {
            scene.close()
        }
        File(outDir, name).writeBytes(png)
        // The cover this frame drew, its decoded pixels and the pixels of the
        // slot it went into: the ratio is what "sharp" means here.
        val drawn = if (width >= 820) {
            minOf((width * 0.40f).toInt(), height - 140)
        } else {
            minOf((width * 0.62f).toInt(), (height * 0.36f).toInt())
        }.coerceAtLeast(1)
        val source = org.jetbrains.skia.Image.makeFromEncoded(artworkFile.readBytes())
        val ratio = source.width.toFloat() / drawn
        println(
            "%-18s artwork %s %dx%d decoded, drawn at %d px (%.2fx)%s".format(
                EN,
                name,
                ARTWORK_SOURCE,
                source.width,
                source.height,
                drawn,
                ratio,
                if (ratio < 2f) " max available" else "",
            ),
        )
        return javax.imageio.ImageIO.read(png.inputStream())
    }

    private fun mini(width: Int, height: Int, name: String, song: NowPlaying): BufferedImage {
        val scene = ImageComposeScene(width, height) {
            AppTheme(mode = ThemeMode.DARK, accent = Color(0xFFED5564), language = "en") {
                Box(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
                    Box(Modifier.fillMaxWidth().padding(8.dp)) {
                        AppleMusicV17MiniPlayer(
                            nowPlaying = song,
                            isPlaying = true,
                            isLoading = false,
                            swipeThumbnail = true,
                            swipeSensitivity = 0.7f,
                            onTogglePlay = {},
                            onNext = {},
                            onPrevious = {},
                            onOpenPlayer = {},
                            language = "en",
                            volume = 0.72f,
                            isShuffle = false,
                            repeatMode = RepeatMode.OFF,
                            positionMs = 81_000,
                            durationMs = 234_000,
                            onVolume = {},
                            onToggleShuffle = {},
                            onCycleRepeat = {},
                            onOpenLyrics = {},
                            onOpenQueue = {},
                        )
                    }
                }
            }
        }
        val png = try {
            repeat(4) {
                scene.render()
                Thread.sleep(100)
            }
            scene.render().encodeToData(EncodedImageFormat.PNG, 100)!!.bytes
        } finally {
            scene.close()
        }
        File(outDir, name).writeBytes(png)
        return javax.imageio.ImageIO.read(png.inputStream())
    }

    /**
     * A press on the title has to open the player. It is read here and not on a
     * window because a synthetic window click also has to activate the window
     * first, which makes a miss ambiguous; a scene delivers the press to the
     * composition and nothing else.
     */
    private fun miniPressOpensPlayer(song: NowPlaying): Int {
        var opened = 0
        var x = 0f
        var y = 0f
        val scene = ImageComposeScene(900, 120) {
            AppTheme(mode = ThemeMode.DARK, accent = Color(0xFFED5564), language = "en") {
                Box(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
                    Box(Modifier.fillMaxWidth().padding(8.dp)) {
                        AppleMusicV17MiniPlayer(
                            nowPlaying = song,
                            isPlaying = true,
                            isLoading = false,
                            swipeThumbnail = true,
                            swipeSensitivity = 0.7f,
                            onTogglePlay = {},
                            onNext = {},
                            onPrevious = {},
                            onOpenPlayer = { opened++ },
                            language = "en",
                            volume = 0.72f,
                            isShuffle = false,
                            repeatMode = RepeatMode.OFF,
                            positionMs = 81_000,
                            durationMs = 234_000,
                            onVolume = {},
                            onToggleShuffle = {},
                            onCycleRepeat = {},
                            onOpenLyrics = {},
                            onOpenQueue = {},
                        )
                    }
                }
            }
        }
        try {
            repeat(4) {
                scene.render()
                Thread.sleep(80)
            }
            // The bar is 64 dp tall inside the 8 dp padding, so its middle is
            // 40 dp down, and the title column is the middle of the width.
            x = 450f
            y = 40f
            scene.sendPointerEvent(PointerEventType.Move, Offset(x, y))
            scene.sendPointerEvent(PointerEventType.Press, Offset(x, y))
            scene.sendPointerEvent(PointerEventType.Release, Offset(x, y))
            repeat(3) {
                scene.render()
                Thread.sleep(80)
            }
            println("mini player press: pressed at %.0f,%.0f".format(x, y))
        } finally {
            scene.close()
        }
        return opened
    }

    private fun meanLuminance(image: BufferedImage): Double {
        var total = 0.0
        for (y in 0 until image.height) {
            for (x in 0 until image.width) {
                val rgb = image.getRGB(x, y)
                total += (((rgb shr 16) and 255) + ((rgb shr 8) and 255) + (rgb and 255)) / (3.0 * 255)
            }
        }
        return total / (image.width.toLong() * image.height)
    }

    private fun ink(image: BufferedImage, fromX: Double, toX: Double, fromY: Double, toY: Double): Int {
        var count = 0
        val x0 = (fromX * image.width).toInt().coerceIn(0, image.width - 1)
        val x1 = (toX * image.width).toInt().coerceIn(x0 + 1, image.width)
        val y0 = (fromY * image.height).toInt().coerceIn(0, image.height - 1)
        val y1 = (toY * image.height).toInt().coerceIn(y0 + 1, image.height)
        for (y in y0 until y1) {
            for (x in x0 until x1) {
                val rgb = image.getRGB(x, y)
                val brightness = (((rgb shr 16) and 255) + ((rgb shr 8) and 255) + (rgb and 255)) / (3.0 * 255)
                if (brightness >= 0.30) count++
            }
        }
        return count
    }

    private fun differingPixels(a: BufferedImage, b: BufferedImage): Int {
        if (a.width != b.width || a.height != b.height) return Int.MAX_VALUE
        var count = 0
        for (y in 0 until a.height) {
            for (x in 0 until a.width) {
                if (a.getRGB(x, y) != b.getRGB(x, y)) count++
            }
        }
        return count
    }
}
