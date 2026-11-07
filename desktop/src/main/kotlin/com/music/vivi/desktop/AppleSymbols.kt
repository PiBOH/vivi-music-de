package com.music.vivi.desktop

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.addPathNodes
import androidx.compose.ui.unit.dp

/**
 * The Apple shapes the v17 player draws its controls with.
 *
 * The edition follows the reference on this surface (see the Apple icons rule
 * in `AGENTS.md`): a control of the Apple player keeps the glyph the Apple
 * player uses for it, so these are line drawings in the SF Symbols manner
 * rather than Material's filled glyphs. Every one is a 24 dp box, stroke only,
 * so a `tint` reaches it like any other icon.
 */
object AppleSymbols {

    /** SF `text.bubble`: the lyrics page. */
    val Lyrics: ImageVector by lazy {
        line(
            "AppleLyrics",
            "M4 7A2.6 2.6 0 0 1 6.6 4.4h10.8A2.6 2.6 0 0 1 20 7v6.4a2.6 2.6 0 0 1-2.6 2.6H10.4L6 19.6v-3.6H6.6A2.6 2.6 0 0 1 4 13.4z " +
                "M8 8.6h8 M8 12h5",
        )
    }

    /** SF `airplayvideo`: the audio output. */
    val AudioOutput: ImageVector by lazy {
        line(
            "AppleAudioOutput",
            "M5.6 4.8h12.8A1.6 1.6 0 0 1 20 6.4v8A1.6 1.6 0 0 1 18.4 16h-2.1 " +
                "M5.6 4.8A1.6 1.6 0 0 0 4 6.4v8A1.6 1.6 0 0 0 5.6 16h2.1 M9.2 19.4 12 15.6l2.8 3.8z",
        )
    }

    /** SF `person.2`: listening together. */
    val ListeningTogether: ImageVector by lazy {
        line(
            "AppleListeningTogether",
            "M9.2 11.1a2.7 2.7 0 1 0 0-5.4 2.7 2.7 0 0 0 0 5.4z " +
                "M4.2 19.4c0-2.7 2.2-4.6 5-4.6s5 1.9 5 4.6 " +
                "M15.6 11.4a2.4 2.4 0 1 0 0-4.8 M16.8 19.4c0-2 .4-3.4 1.4-4.2",
        )
    }

    /** SF `shuffle`: the queue order. */
    val Shuffle: ImageVector by lazy {
        line(
            "AppleShuffle",
            "M4 7.6h2.4c1 0 1.9.4 2.6 1.1l6 6.2c.7.7 1.6 1.1 2.6 1.1H20 " +
                "M4 16.4h2.4c1 0 1.9-.4 2.6-1.1l1.4-1.5 " +
                "M14.4 9.2 16 7.5c.7-.7 1.6-1.1 2.6-1.1H20 " +
                "M17.4 4.8 20.2 6.8l-2.8 2 " +
                "M17.4 13.6l2.8 2-2.8 2",
        )
    }

    /** SF `repeat`: cycling the queue. */
    val Repeat: ImageVector by lazy {
        line(
            "AppleRepeat",
            "M7.2 6.2h7.2A4.6 4.6 0 0 1 19 10.8v.6 " +
                "M16.8 17.8H9.6A4.6 4.6 0 0 1 5 13.2v-.6 " +
                "M15.8 3.4l3.2 2.6-3.2 2.6 " +
                "M8.2 15.4 5 17.8l3.2 2.6",
        )
    }

    /** SF `repeat.1`: repeating the one track. */
    val RepeatOne: ImageVector by lazy {
        line(
            "AppleRepeatOne",
            "M7.2 6.2h7.2A4.6 4.6 0 0 1 19 10.8v.6 " +
                "M16.8 17.8H9.6A4.6 4.6 0 0 1 5 13.2v-.6 " +
                "M15.8 3.4l3.2 2.6-3.2 2.6 " +
                "M8.2 15.4 5 17.8l3.2 2.6 " +
                "M11.6 10.4l1.4-.9v5",
        )
    }

    /** SF `infinity`: autoplay. */
    val Autoplay: ImageVector by lazy {
        line(
            "AppleAutoplay",
            "M12.2 12c-1.3-2.1-2.2-2.9-3.8-2.9a2.9 2.9 0 1 0 0 5.8c1.6 0 2.5-.8 3.8-2.9z " +
                "M12.2 12c1.3-2.1 2.2-2.9 3.8-2.9a2.9 2.9 0 1 1 0 5.8c-1.6 0-2.5-.8-3.8-2.9z",
        )
    }

    /** SF `list.bullet`: the queue. */
    val Queue: ImageVector by lazy {
        line(
            "AppleQueue",
            "M4.4 7h9.2 M4.4 12h9.2 M4.4 17h5.6 " +
                "M15.6 13.2l4.2 3-4.2 3z",
        )
    }

    /** SF `ellipsis`: the player's menu. */
    val More: ImageVector by lazy {
        ImageVector.Builder(
            name = "AppleMore",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f,
        ).apply {
            addPath(
                pathData = addPathNodes(
                    "M6 12a1.6 1.6 0 1 0 0-3.2 1.6 1.6 0 0 0 0 3.2z " +
                        "M12 12a1.6 1.6 0 1 0 0-3.2 1.6 1.6 0 0 0 0 3.2z " +
                        "M18 12a1.6 1.6 0 1 0 0-3.2 1.6 1.6 0 0 0 0 3.2z",
                ),
                fill = SolidColor(Color.Black),
            )
        }.build()
    }

    private fun line(name: String, path: String): ImageVector = ImageVector.Builder(
        name = name,
        defaultWidth = 24.dp,
        defaultHeight = 24.dp,
        viewportWidth = 24f,
        viewportHeight = 24f,
    ).apply {
        addPath(
            pathData = addPathNodes(path),
            stroke = SolidColor(Color.Black),
            strokeLineWidth = 1.7f,
            strokeLineCap = StrokeCap.Round,
            strokeLineJoin = StrokeJoin.Round,
            fill = null,
        )
    }.build()
}
