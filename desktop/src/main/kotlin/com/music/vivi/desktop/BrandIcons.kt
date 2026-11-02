package com.music.vivi.desktop

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathFillType
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.PathParser
import androidx.compose.ui.unit.dp

/**
 * The two brand marks the Integrations screen draws.
 *
 * The preview's `discord` and `lastfm` glyphs (`js/screens.js`, `integrations`)
 * are the brands' own marks, and the Material set has nothing that means either
 * one, so the row carries the mark itself instead of a glyph that only looks
 * close. They are built the way the app's other marks are ([GoogleMark], the
 * GitHub mark in `AboutScreens`, the Spotify mark): the official path, filled
 * with a placeholder colour that the `Icon`'s tint replaces, so they sit in the
 * row's tile exactly like a Material icon.
 *
 * Mark: Simple Icons (`discord.svg`, `lastdotfm.svg`), viewBox 0 0 24 24.
 *
 * The Discord path is not the file's text. Its arcs pack the two flags into the
 * numbers around them (`0 00-4.885`), and a parser that reads `00` as one number
 * then draws a broken face - which is what shipped twice and was reported as
 * "il logo di Discord continua a non essere corretto". The path below is the
 * same mark rewritten by `scripts` arithmetic: every command absolute, every
 * number and every flag separated by a space, so there is nothing left to read
 * two ways.
 */
object BrandIcons {

    /** Discord's mark, the face of Rich Presence. */
    val Discord: ImageVector by lazy { mark("Discord", DISCORD_PATH, evenOdd = true) }

    /** Last.fm's mark, the scrobble's own. */
    val LastFm: ImageVector by lazy { mark("Last.fm", LASTFM_PATH) }

    private fun mark(name: String, path: String, evenOdd: Boolean = false): ImageVector = ImageVector.Builder(
        name = name,
        defaultWidth = 24.dp,
        defaultHeight = 24.dp,
        viewportWidth = 24f,
        viewportHeight = 24f,
    ).addPath(
        pathData = PathParser().parsePathString(path).toNodes(),
        // Discord's two eyes are holes in the head: they are separate subpaths
        // inside it, so even-odd cuts them out whatever their winding.
        pathFillType = if (evenOdd) PathFillType.EvenOdd else PathFillType.NonZero,
        fill = SolidColor(Color.Black),
    ).build()

    /** Discord, absolute and space-separated (see the object's own note). */
    private const val DISCORD_PATH =
        "M 20.317 4.3698 A 19.7913 19.7913 0 0 0 15.4319 2.8546 A 0.0741 0.0741 0 0 0 15.3534 2.8917 C " +
            "15.1424 3.267 14.9087 3.7565 14.7451 4.1412 C 12.9004 3.865 11.0651 3.865 9.2583 4.1412 C 9.0947 " +
            "3.7479 8.8525 3.267 8.6406 2.8917 A 0.077 0.077 0 0 0 8.5621 2.8547 A 19.7363 19.7363 0 0 0 " +
            "3.6769 4.3697 A 0.0699 0.0699 0 0 0 3.6448 4.3974 C 0.5334 9.0458 -0.319 13.5799 0.0992 18.0578 " +
            "A 0.0824 0.0824 0 0 0 0.1304 18.1139 C 2.1832 19.6215 4.1717 20.5367 6.1233 21.1433 A 0.0777 " +
            "0.0777 0 0 0 6.2075 21.1157 C 6.6691 20.4853 7.0806 19.8205 7.4335 19.1215 A 0.076 0.076 0 0 0 " +
            "7.3919 19.0158 C 6.7391 18.7682 6.1176 18.4663 5.5197 18.1235 A 0.077 0.077 0 0 1 5.5121 17.9958 " +
            "C 5.6379 17.9015 5.7638 17.8035 5.8839 17.7044 A 0.0743 0.0743 0 0 1 5.9615 17.6939 C 9.8893 " +
            "19.4872 14.1415 19.4872 18.0229 17.6939 A 0.0739 0.0739 0 0 1 18.1014 17.7034 C 18.2216 17.8024 " +
            "18.3474 17.9015 18.4742 17.9958 A 0.077 0.077 0 0 1 18.4676 18.1234 A 12.2986 12.2986 0 0 1 " +
            "16.5946 19.0148 A 0.0766 0.0766 0 0 0 16.5539 19.1215 C 16.9143 19.8195 17.3258 20.4843 17.7789 " +
            "21.1147 A 0.076 0.076 0 0 0 17.8631 21.1433 C 19.8241 20.5366 21.8126 19.6214 23.8654 18.1139 A " +
            "0.077 0.077 0 0 0 23.8967 18.0587 C 24.3971 12.8817 23.0585 8.3848 20.3482 4.3983 A 0.061 0.061 " +
            "0 0 0 20.317 4.3697 Z M 8.02 15.3312 C 6.8375 15.3312 5.8631 14.2455 5.8631 12.9122 C 5.8631 " +
            "11.579 6.8186 10.4933 8.0201 10.4933 C 9.2309 10.4933 10.1958 11.5885 10.1769 12.9123 C 10.1769 " +
            "14.2455 9.2214 15.3312 8.02 15.3312 Z M 15.9948 15.3312 C 14.8123 15.3312 13.8379 14.2455 " +
            "13.8379 12.9122 C 13.8379 11.579 14.7933 10.4933 15.9948 10.4933 C 17.2056 10.4933 18.1705 " +
            "11.5885 18.1516 12.9123 C 18.1516 14.2455 17.2056 15.3312 15.9948 15.3312 Z"

    private const val LASTFM_PATH =
        "M10.584 17.21l-.88-2.392s-1.43 1.594-3.573 1.594c-1.897 0-3.244-1.649-3.244-4.288 0-3.382 " +
            "1.704-4.591 3.381-4.591 2.42 0 3.189 1.567 3.849 3.574l.88 2.749c.88 2.666 2.529 4.81 7.285 " +
            "4.81 3.409 0 5.718-1.044 5.718-3.793 0-2.227-1.265-3.381-3.63-3.931l-1.758-.385c-1.21-.275" +
            "-1.567-.77-1.567-1.595 0-.934.742-1.484 1.952-1.484 1.32 0 2.034.495 2.144 1.677l2.749-.33c" +
            "-.22-2.474-1.924-3.492-4.729-3.492-2.474 0-4.893.935-4.893 3.932 0 1.87.907 3.051 3.189 " +
            "3.601l1.87.44c1.402.33 1.869.907 1.869 1.704 0 1.017-.99 1.43-2.86 1.43-2.776 0-3.93-1.457" +
            "-4.59-3.464l-.907-2.75c-1.155-3.573-2.997-4.893-6.653-4.893C2.144 5.333 0 7.89 0 12.233c0 " +
            "4.18 2.144 6.434 5.993 6.434 3.106 0 4.591-1.457 4.591-1.457z"
}
