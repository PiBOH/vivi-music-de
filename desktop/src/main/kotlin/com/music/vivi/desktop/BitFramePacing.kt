package com.music.vivi.desktop

import java.util.Locale

/**
 * Real frame pacing, measured on the frames the window actually paints.
 *
 * The previous probe drove the frame clock itself (`withFrameNanos` in a loop),
 * so it measured its own request rate and reported numbers that had nothing to
 * do with the player (29 to 98 fps on unchanged screens). This one is passive:
 * the root draw pass reports a timestamp only when Compose really draws, so a
 * static screen reports no frames and an animating one reports its own cadence.
 *
 * Enabled with `-Dvivi.frameProbe=<file>` (through `JAVA_TOOL_OPTIONS` for the
 * packaged launcher). Every 60 painted frames it rewrites the file with the
 * average over the last ten seconds, the p95 inter-frame gap, the p95 draw
 * cost, the dropped-frame count and the worst gap. Nothing is drawn on screen.
 */
internal object BitFramePacing {
    private const val WINDOW = 1200
    private const val BUDGET_NS = 16_666_667L
    private const val DROP_NS = 25_000_000L

    private val path: String? = System.getProperty("vivi.frameProbe")
    val enabled: Boolean get() = path != null

    private val gaps = LongArray(WINDOW)
    private val draws = LongArray(WINDOW)
    private var count = 0
    private var head = 0
    private var lastFrame = 0L
    private var framesSinceWrite = 0
    private var wroteFirst = false

    /**
     * One timestamp from the animation's own frame clock (`withFrameNanos`).
     * The animation asks for the frame; the probe only records when it lands.
     */
    fun tick(frameTimeNanos: Long) {
        val file = path ?: return
        if (!wroteFirst) {
            wroteFirst = true
            runCatching { java.io.File(file).writeText("first_frame") }
        }
        draws[head] = 0L
        if (lastFrame != 0L) {
            gaps[head] = saneGap(frameTimeNanos - lastFrame)
        } else {
            gaps[head] = 0L
        }
        lastFrame = frameTimeNanos
        head = (head + 1) % WINDOW
        if (count < WINDOW) count++
        framesSinceWrite++
        if (framesSinceWrite >= 120 && count >= 600) {
            framesSinceWrite = 0
            write(file)
        }
    }

    /** Called once per painted frame; [drawNanos] is how long the frame took. */
    fun frame(drawNanos: Long) {
        val file = path ?: return
        if (!wroteFirst) {
            wroteFirst = true
            runCatching { java.io.File(file).writeText("first_frame") }
        }
        val now = System.nanoTime()
        draws[head] = drawNanos
        if (lastFrame != 0L) {
            gaps[head] = saneGap(now - lastFrame)
        } else {
            gaps[head] = 0L
        }
        lastFrame = now
        head = (head + 1) % WINDOW
        if (count < WINDOW) count++
        framesSinceWrite++
        if (framesSinceWrite >= 120 && count >= 600) {
            framesSinceWrite = 0
            write(file)
        }
    }

    fun reset() {
        count = 0
        head = 0
        lastFrame = 0L
        framesSinceWrite = 0
    }

    /**
     * A frame clock that restarts (window recreated, app suspended) reports a
     * timestamp far from the previous one; counting it would poison the ten
     * second window with a gap of hours.
     */
    private fun saneGap(gap: Long): Long = if (gap in 0..2_000_000_000L) gap else 0L

    private fun write(file: String) {
        val n = count
        if (n < 2) return
        val frameGaps = LongArray(n)
        val frameDraws = LongArray(n)
        var gapSum = 0L
        var drawSum = 0L
        var dropped = 0
        var maxGap = 0L
        var i = 0
        while (i < n) {
            val g = gaps[i]
            val d = draws[i]
            frameGaps[i] = g
            frameDraws[i] = d
            gapSum += g
            drawSum += d
            if (g > DROP_NS) dropped++
            if (g > maxGap) maxGap = g
            i++
        }
        frameGaps.sort()
        frameDraws.sort()
        val span = gapSum / 1_000_000_000.0
        val avgFps = if (gapSum > 0) (n - 1) * 1_000_000_000.0 / gapSum else 0.0
        val p95Gap = frameGaps[(n * 95 / 100).coerceAtMost(n - 1)]
        val p95Draw = frameDraws[(n * 95 / 100).coerceAtMost(n - 1)]
        val line = String.format(
            Locale.US,
            "frames=%d span_s=%.1f avg_fps=%.1f p95_gap_ms=%.1f p95_draw_ms=%.1f dropped=%d max_gap_ms=%.1f budget_ms=16.7",
            n,
            span,
            avgFps,
            p95Gap / 1_000_000.0,
            p95Draw / 1_000_000.0,
            dropped,
            maxGap / 1_000_000.0,
        )
        runCatching { java.io.File(file).writeText(line) }
    }
}
