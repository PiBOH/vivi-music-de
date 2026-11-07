package com.music.vivi.desktop

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.addPathNodes
import androidx.compose.ui.unit.dp

/**
 * BitChord's own glyph family, for the controls the v17 player shares with it.
 *
 * The row under the artwork is the reference's, so its glyphs are the
 * reference's too: thick strokes (2.2 px) with round caps and joins, drawn as
 * strokes so a `tint` reaches them like any Material icon. The paths are the
 * ones `BitChordIcons` builds with the vector DSL in the reference source, kept
 * to the same coordinates and the same weights.
 */
object BitChordIcons {

    private const val STROKE = 2.2f

    /** Beamed list of three lines with a dot in front of each: the queue. */
    val Queue: ImageVector by lazy {
        glyph(
            name = "BitChordQueue",
            strokePath = "M8 6H21 M8 12H21 M8 18H21",
            strokeWidth = 2f,
            fillPath = "M4.35 6a1.25 1.25 0 1 1-2.5 0 1.25 1.25 0 1 1 2.5 0z " +
                "M4.35 12a1.25 1.25 0 1 1-2.5 0 1.25 1.25 0 1 1 2.5 0z " +
                "M4.35 18a1.25 1.25 0 1 1-2.5 0 1.25 1.25 0 1 1 2.5 0z",
        )
    }

    /** Quotation bubble with two quote marks: the lyrics page. */
    val LyricsQuote: ImageVector by lazy {
        glyph(
            name = "BitChordLyricsQuote",
            strokePath = "M6 3.5H18Q21 3.5 21 6.5V15Q21 18 18 18H10L6 21V18Q3 18 3 15V6.5Q3 3.5 6 3.5Z",
            strokeWidth = 2f,
            fillPath = "M7.6 8H10.6V11.1Q10.6 13.2 8.1 13.7V12.3Q9.3 12 9.3 11H7.6Z " +
                "M12.8 8H15.8V11.1Q15.8 13.2 13.3 13.7V12.3Q14.5 12 14.5 11H12.8Z",
        )
    }

    /** Two crossing strands, each with its own arrow head. */
    val Shuffle: ImageVector by lazy {
        glyph(
            name = "BitChordShuffle",
            strokePath = "M3.4 7.4H7L16.6 16.6H20.6 " +
                "M18.1 14.1L20.6 16.6L18.1 19.1 " +
                "M3.4 16.6H7L9.8 13.9 " +
                "M13.9 10.1L16.6 7.4H20.6 " +
                "M18.1 4.9L20.6 7.4L18.1 9.9",
            strokeWidth = STROKE,
        )
    }

    /** Two straight runs joined by semicircles, arrow heads flat at the ends. */
    val Repeat: ImageVector by lazy {
        glyph(
            name = "BitChordRepeat",
            strokePath = "M8.6 7.6H15.4a4.4 4.4 0 0 1 0 8.8H8.6a4.4 4.4 0 0 1 0-8.8Z " +
                "M13.5 5.7L15.4 7.6L13.5 9.5 " +
                "M10.5 14.5L8.6 16.4L10.5 18.3",
            strokeWidth = STROKE,
        )
    }

    /** The lemniscate of autoplay. */
    val Infinity: ImageVector by lazy {
        glyph(
            name = "BitChordInfinity",
            strokePath = "M12 12C10.1 9.1 8.7 8 7.1 8a4 4 0 0 0 0 8C8.7 16 10.1 14.9 12 12 " +
                "C13.9 9.1 15.3 8 16.9 8a4 4 0 0 1 0 8C15.3 16 13.9 14.9 12 12Z",
            strokeWidth = STROKE,
        )
    }

    private fun glyph(
        name: String,
        strokePath: String,
        strokeWidth: Float,
        fillPath: String? = null,
    ): ImageVector = ImageVector.Builder(
        name = name,
        defaultWidth = 24.dp,
        defaultHeight = 24.dp,
        viewportWidth = 24f,
        viewportHeight = 24f,
    ).apply {
        addPath(
            pathData = addPathNodes(strokePath),
            stroke = SolidColor(Color.Black),
            strokeLineWidth = strokeWidth,
            strokeLineCap = StrokeCap.Round,
            strokeLineJoin = StrokeJoin.Round,
            fill = null,
        )
        if (fillPath != null) {
            addPath(
                pathData = addPathNodes(fillPath),
                fill = SolidColor(Color.Black),
                stroke = null,
            )
        }
    }.build()
}
