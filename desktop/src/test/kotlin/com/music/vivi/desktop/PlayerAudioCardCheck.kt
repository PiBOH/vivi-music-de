@file:OptIn(ExperimentalComposeUiApi::class)

package com.music.vivi.desktop

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.FlashOn
import androidx.compose.material.icons.filled.MusicOff
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.ImageComposeScene
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import org.jetbrains.skia.EncodedImageFormat
import java.io.File

/**
 * Headless check of the Player & audio screen's cards and of the fold a row that
 * belongs to a switch makes, run with
 *
 *     ./gradlew :desktop:playerAudioCardCheck
 *
 * Two settings on that screen are a threshold of a switch above them: the
 * crossfade duration (only meaningful with crossfade on) and the stream cache's
 * number. Both used to be a card of their own with a loose `Slider` floating under
 * the group, so the number a switch governs read as a setting of its own. They are
 * rows of the switch's card now - the duration carries its slider in the row, the
 * cache is one row with its slider under the description.
 *
 * Whether a row joined the right card can only be read from pixels. The section is
 * rendered into an off-screen `ImageComposeScene` and the column through the
 * middle of it is walked, where a card paints a band and the page shows between
 * bands. The two kinds of gap differ - 3 dp between the rows of one card, 8 dp
 * between two cards - so the bands say how many rows a card holds:
 *
 *  * crossfade off against on: the gapless exception and the duration are rows of
 *    the crossfade card, so the same cards and two more bands;
 *  * skip silence off against on: one more band in the same cards;
 *  * both on: three more bands, still the same cards.
 *
 * This section's groups are drawn flush (nothing spaces two of its cards apart), so
 * a row that went back to a card of its own would touch the card above it and merge
 * into ONE band: it is the band count that says the new rows are rows, and the 3 dp
 * seam count that says they are seams of a card.
 *
 * The second half is the motion. A row that belongs to a switch folds away instead
 * of being dropped: the same card is rendered at three times - fully on, 40 ms into
 * the fold and after it - and the row must still be there at the second frame,
 * shorter than a full row, and gone at the third. A row that merely disappeared
 * reads one band at 40 ms already, which is what the check refuses. The way back is
 * measured the same way, and the mid-fold frame is written to `.ignore/`.
 *
 * PNGs are written under `.ignore/playeraudio/` so the layout and the mid-fold
 * frame can be looked at.
 */
object PlayerAudioCardCheck {

    private val outDir = File(".ignore/playeraudio")

    /** Wide enough that no title wraps, tall enough for the whole section. */
    private const val WIDTH = 900
    private const val HEIGHT = 3000

    /** Tall enough for the two rows of the folding card under the page padding. */
    private const val FOLD_HEIGHT = 520

    /** A moment into the fold, and well past the end of it. */
    private const val MID_MS = 40L
    private const val DONE_MS = 500L

    @JvmStatic
    fun main(args: Array<String>) {
        outDir.mkdirs()
        val failures = mutableListOf<String>()

        val off = render(crossfade = false, skipSilence = false)
        File(outDir, "player-audio-off.png").writeBytes(off)
        val offBands = bands(off)
        println("crossfade off: cards=${offBands.cards} rows=${offBands.rows} gaps=${offBands.gaps}")

        val crossfaded = render(crossfade = true, skipSilence = false)
        File(outDir, "player-audio-crossfade.png").writeBytes(crossfaded)
        val crossfadeBands = bands(crossfaded)
        println("crossfade on: cards=${crossfadeBands.cards} rows=${crossfadeBands.rows} gaps=${crossfadeBands.gaps}")

        val silence = render(crossfade = false, skipSilence = true)
        File(outDir, "player-audio-skip-silence.png").writeBytes(silence)
        val silenceBands = bands(silence)
        println("skip silence on: cards=${silenceBands.cards} rows=${silenceBands.rows} gaps=${silenceBands.gaps}")

        val both = render(crossfade = true, skipSilence = true)
        File(outDir, "player-audio-both.png").writeBytes(both)
        val bothBands = bands(both)
        println("both on: cards=${bothBands.cards} rows=${bothBands.rows} gaps=${bothBands.gaps}")

        // The crossfade rows: the gapless exception, and the duration with its own
        // slider inside the row. Two rows, no new card.
        if (crossfadeBands.cards != offBands.cards) {
            failures += "turning crossfade on changed the card count from ${offBands.cards} to ${crossfadeBands.cards}: the gapless row and the duration must be rows of the crossfade card, not cards of their own"
        }
        if (crossfadeBands.rows != offBands.rows + 2) {
            failures += "with crossfade on the screen should hold ${offBands.rows + 2} rows (the gapless exception and the duration added), read ${crossfadeBands.rows}"
        }
        // Two 3 dp seams more, and no big gap added: the rows went into a card
        // rather than beside it.
        if (crossfadeBands.gaps.count { it == 3 } != offBands.gaps.count { it == 3 } + 2) {
            failures += "with crossfade on the 3 dp seams should grow by two, read ${crossfadeBands.gaps}"
        }
        if (crossfadeBands.gaps.count { it >= 6 } != offBands.gaps.count { it >= 6 }) {
            failures += "with crossfade on a new card appeared among the seams: ${crossfadeBands.gaps}"
        }

        if (silenceBands.cards != offBands.cards) {
            failures += "turning skip silence on changed the card count from ${offBands.cards} to ${silenceBands.cards}"
        }
        if (silenceBands.rows != offBands.rows + 1) {
            failures += "with skip silence on the screen should hold ${offBands.rows + 1} rows (\"Instantly skip silence\" added), read ${silenceBands.rows}"
        }

        if (bothBands.cards != offBands.cards) {
            failures += "with both switches on the card count changed to ${bothBands.cards}"
        }
        if (bothBands.rows != offBands.rows + 3) {
            failures += "with both switches on the screen should hold ${offBands.rows + 3} rows, read ${bothBands.rows}"
        }

        // The fold: the card walked frame by frame through the collapse and back
        // in. A row that merely disappears is gone at the first sampled frame; a
        // folded one is drawn part-way down, then gone, then part-way back.
        val fold = foldFrames()
        File(outDir, "fold-mid.png").writeBytes(fold.midFrame)
        println("fold: on=${fold.on} in=${fold.folding} out=${fold.opening}")
        val full = fold.on.getOrElse(1) { 0 }
        if (fold.on.size != 2) {
            failures += "with the sub-option on the card should hold 2 rows, read ${fold.on}"
        }
        if (fold.folding.none { it.size == 2 && it[1] < full }) {
            failures += "the collapse was never drawn shorter than a full row: ${fold.folding}, a full row is $full px"
        }
        if (fold.folding.last().size != 1) {
            failures += "once the fold is over the card should hold 1 row, read ${fold.folding.last()}"
        }
        if (fold.opening.none { it.size == 2 && it[1] < full }) {
            failures += "the unfold was never drawn shorter than a full row: ${fold.opening}, a full row is $full px"
        }
        if (fold.opening.last().size != 2) {
            failures += "once the unfold is over the card should hold 2 rows, read ${fold.opening.last()}"
        }
        // The card's bottom end moves with the fold. The row above starts at a 6 dp
        // corner (inset about 30 px) and ends at 24 dp (about 48), and the frames in
        // between are what says it travelled instead of jumping.
        val insets = fold.corners.filter { it >= 0 }
        println("bottom corner inset over the fold: ${insets.distinct()}")
        if (insets.map { it }.distinct().size < 3) {
            failures += "the card's bottom corner snapped instead of moving: the fold reads ${insets.distinct()} px of inset"
        }
        if (insets.isNotEmpty() && insets.last() !in 42..54) {
            failures += "after the fold the row above should own the 24 dp bottom (an inset of about 48 px), read ${insets.last()}"
        }

        if (failures.isEmpty()) {
            println()
            println("OK: the duration and the stream cache are rows of their own cards, the skip-silence row stays in its switch's card, and it folds in and out")
            return
        }
        println()
        println("FAILED:")
        failures.forEach { println("  - $it") }
        throw IllegalStateException("player audio card check failed (${failures.size} case(s))")
    }

    private fun render(crossfade: Boolean, skipSilence: Boolean): ByteArray {
        val scene = ImageComposeScene(WIDTH, HEIGHT) {
            AppTheme(mode = ThemeMode.LIGHT, accent = Color(0xFFED5564), language = "en") {
                Column(
                    Modifier
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState())
                        .padding(horizontal = 24.dp),
                ) {
                    PlayerSection(
                        language = "en",
                        autoPlayNext = true,
                        onToggleAutoPlayNext = {},
                        autoLoadMore = false,
                        onToggleAutoLoadMore = {},
                        preventDuplicateTracksInQueue = true,
                        onTogglePreventDuplicateTracksInQueue = {},
                        autoSkipNextOnError = true,
                        onToggleAutoSkipNextOnError = {},
                        pauseWhenMediaMuted = false,
                        onTogglePauseWhenMediaMuted = {},
                        keepScreenOnWhenPlayerExpanded = false,
                        onToggleKeepScreenOnWhenPlayerExpanded = {},
                        persistentShuffle = false,
                        onTogglePersistentShuffle = {},
                        progressiveSeek = true,
                        onToggleProgressiveSeek = {},
                        autoDownloadOnLike = false,
                        onToggleAutoDownloadOnLike = {},
                        historyDurationSeconds = 30,
                        onHistoryDurationSecondsChange = {},
                        skipSilence = skipSilence,
                        onToggleSkipSilence = {},
                        skipSilenceInstant = false,
                        onToggleSkipSilenceInstant = {},
                        crossfade = crossfade,
                        onToggleCrossfade = {},
                        crossfadeDurationSeconds = 6,
                        onCrossfadeDurationSecondsChange = {},
                        disableCrossfadeGapless = false,
                        onToggleDisableCrossfadeGapless = {},
                        audioQuality = "high",
                        onAudioQualityChange = {},
                        rememberShuffleRepeat = false,
                        onToggleRememberShuffleRepeat = {},
                        persistentQueue = true,
                        onTogglePersistentQueue = {},
                        onOpenEqualizer = {},
                        streamCacheMinutes = 10,
                        onStreamCacheMinutesChange = {},
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
     * The card walked frame by frame: on, then the collapse sampled every few
     * milliseconds, then the row switched back on and the unfold sampled the same
     * way. Every sampled frame is kept, so the assertions can ask whether the row
     * was ever DRAWN part-way instead of taking one sample's word for it.
     */
    private class FoldFrames(
        val on: List<Int>,
        val folding: List<List<Int>>,
        val opening: List<List<Int>>,
        /** The first row's bottom-left corner, in px of inset, one per fold sample. */
        val corners: List<Int>,
        val midFrame: ByteArray,
    )

    /** The times the fold is sampled at, in ms after the switch was flipped. */
    private val FOLD_SAMPLES_MS = listOf(5L, 10L, 15L, 20L, 25L, 30L, 35L, 40L, 50L, 60L, 80L, 100L, 150L, 250L, 400L, 600L)

    private fun foldFrames(): FoldFrames {
        // The master switch is read from the settings file: the check sets it, so
        // what it measures is the motion and not the machine it runs on.
        Animations.enabled = true
        Animations.speed = "normal"
        val on = mutableStateOf(true)
        val scene = ImageComposeScene(WIDTH, FOLD_HEIGHT) {
            AppTheme(mode = ThemeMode.LIGHT, accent = Color(0xFFED5564), language = "en") {
                Column(Modifier.fillMaxSize().padding(24.dp)) {
                    M3SettingsGroup(
                        items = listOf(
                            M3SettingsItem(
                                icon = Icons.Filled.MusicOff,
                                title = { Text(Localization.get("en", "skip_silence")) },
                                description = { Text(Localization.get("en", "skip_silence_desc")) },
                                trailing = { Switch(checked = on.value, onCheckedChange = { on.value = it }) },
                                onClick = { on.value = !on.value },
                            ),
                            M3SettingsItem(
                                icon = Icons.Filled.FlashOn,
                                title = { Text(Localization.get("en", "skip_silence_instant")) },
                                description = { Text(Localization.get("en", "skip_silence_instant_desc")) },
                                visible = on.value,
                                trailing = { Switch(checked = false, onCheckedChange = {}) },
                            ),
                        ),
                    )
                }
            }
        }
        var midFrame = ByteArray(0)
        val corners = mutableListOf<Int>()
        fun shot(nanos: Long): List<Int> {
            val bytes = scene.render(nanos).encodeToData(EncodedImageFormat.PNG, 100)!!.bytes
            corners += bandInsets(bytes).firstOrNull() ?: -1
            return bandHeights(bytes).also { heights ->
                // The frame that shows the row part-way down, kept for the report.
                if (heights.size == 2 && (midFrame.isEmpty() || heights[1] < 40)) midFrame = bytes
            }
        }
        try {
            val start = shot(0L)
            on.value = false
            shot(0L)
            corners.clear()
            val folding = FOLD_SAMPLES_MS.map { shot(it * 1_000_000L) }
            val foldCorners = corners.toList()
            val flip = FOLD_SAMPLES_MS.last() + 20L
            on.value = true
            shot(flip * 1_000_000L)
            val opening = FOLD_SAMPLES_MS.map { shot((flip + it) * 1_000_000L) }
            return FoldFrames(
                on = start,
                folding = folding,
                opening = opening,
                corners = foldCorners,
                midFrame = midFrame,
            )
        } finally {
            scene.close()
        }
    }

    /** What the middle column of the render says about the cards. */
    private data class Bands(val cards: Int, val rows: Int, val gaps: List<Int>)

    private fun bands(png: ByteArray): Bands {
        val cleaned = cleanedRuns(png)
        // A band is a card; the page runs between bands are the gaps.
        val bands = cleaned.filter { it.first }
        val gaps = cleaned.dropWhile { !it.first }
            .filter { !it.first }
            .map { it.second }
        // The last page run is the empty space under the last card, not a gap.
        val between = gaps.dropLast(1)
        // 3 dp seams sit between the rows of one card, 8 dp between two cards.
        val cards = between.count { it >= 6 } + 1
        return Bands(cards = cards, rows = bands.size, gaps = between)
    }

    /**
     * The left inset of the ink on the LAST pixel row of each band, top to bottom.
     *
     * The card fills the padded column, so a card whose bottom is 24 dp round has its
     * last row starting 24 px in from the card's own edge (and the scene pads 24, so
     * the inset reads about 48); a 6 dp row reads about 30. That is how the fold's
     * other half is measured: the card's bottom end has to MOVE between the two,
     * not appear at its destination in one frame.
     */
    private fun bandInsets(png: ByteArray): List<Int> {
        val image = javax.imageio.ImageIO.read(png.inputStream())
        val x = image.width / 2
        val page = image.getRGB(x, 2)
        val bands = mutableListOf<Pair<Int, Int>>() // start row to end row
        var start = -1
        for (y in 0 until image.height) {
            val ink = !near(image.getRGB(x, y), page, 1)
            if (ink && start < 0) start = y
            if (!ink && start >= 0) {
                if (y - start >= 3) bands += start to y
                start = -1
            }
        }
        return bands.map { (_, end) ->
            val row = end - 1
            var first = -1
            for (px in 0 until image.width) {
                if (!near(image.getRGB(px, row), page, 1)) {
                    first = px
                    break
                }
            }
            first
        }
    }

    /**
     * The height of every band in the middle column, top to bottom.
     *
     * The tolerance is 1 here, not the 6 the card count uses: a row that is fading
     * out is still drawn long after its own colour has drifted to within a few
     * units of the page, and the fold's middle frames are exactly what this reads.
     */
    private fun bandHeights(png: ByteArray): List<Int> =
        cleanedRuns(png, threshold = 1, minInk = 3).filter { it.first }.map { it.second }

    /** The middle column as runs of "card" and "page", with text dropped. */
    private fun cleanedRuns(png: ByteArray, threshold: Int = 6, minInk: Int = 40): List<Pair<Boolean, Int>> {
        val image = javax.imageio.ImageIO.read(png.inputStream())
        val x = image.width / 2
        val page = image.getRGB(x, 2)
        // Walk the column and turn it into runs of "card" and "page".
        val runs = mutableListOf<Pair<Boolean, Int>>()
        for (y in 0 until image.height) {
            val onCard = !near(image.getRGB(x, y), page, threshold)
            if (runs.isNotEmpty() && runs.last().first == onCard) {
                runs[runs.size - 1] = onCard to runs.last().second + 1
            } else {
                runs += onCard to 1
            }
        }
        // The screen title is a few pixels of glyphs above the first card, not a
        // card: a row of a card is at least ~48 dp tall, so any short run of ink
        // is text and is dropped (merging the page runs it leaves behind). The fold
        // measurement drops the threshold to a few pixels instead, because a row
        // part-way through its collapse IS a short band.
        val cleaned = mutableListOf<Pair<Boolean, Int>>()
        for (run in runs) {
            if (run.first && run.second < minInk) continue
            if (cleaned.isNotEmpty() && cleaned.last().first == run.first) {
                cleaned[cleaned.size - 1] = run.first to cleaned.last().second + run.second
            } else {
                cleaned += run
            }
        }
        return cleaned
    }

    private fun near(a: Int, b: Int, threshold: Int = 6): Boolean {
        val dr = kotlin.math.abs(((a shr 16) and 255) - ((b shr 16) and 255))
        val dg = kotlin.math.abs(((a shr 8) and 255) - ((b shr 8) and 255))
        val db = kotlin.math.abs((a and 255) - (b and 255))
        return dr + dg + db <= threshold
    }
}
