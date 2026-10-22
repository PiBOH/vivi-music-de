package com.music.vivi.desktop

import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.PathParser
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * The Google "G", drawn from the project's own vector.
 *
 * `app/src/main/res/drawable/google.xml` is the mark the Android app already
 * ships for this sign-in; the path below is its `pathData`, so the desktop draws
 * the same letterform instead of a generic login glyph. Material Icons has no
 * Google brand icon (`Facebook` is there, `Google` is not), which is why this is
 * a vector of the project's own rather than an `Icons.Filled.*` reference.
 *
 * The path is monochrome: the drawable's single `fillColor` keeps every subpath
 * one colour, so it is built here with a fill the `Icon` tint replaces. One
 * `ImageVector`, built once, for the three sizes the sign-in screen asks for.
 */
private const val GOOGLE_G_PATH =
    "M22.56,12.25c0,-0.78 -0.07,-1.53 -0.2,-2.25H12v4.26h5.92c-0.26,1.37 -1.04,2.53 " +
        "-2.21,3.31v2.77h3.57c2.08,-1.92 3.28,-4.74 3.28,-8.09z M12,23c2.97,0 5.46,-0.98 " +
        "7.28,-2.66l-3.57,-2.77c-0.98,0.66 -2.23,1.06 -3.71,1.06 -2.86,0 -5.29,-1.93 " +
        "-6.16,-4.53H2.18v2.84C3.99,20.53 7.7,23 12,23z M5.84,14.09c-0.22,-0.66 " +
        "-0.35,-1.36 -0.35,-2.09s0.13,-1.43 0.35,-2.09V7.06H2.18C1.43,8.55 1,10.22 1,12" +
        "s0.43,3.45 1.18,4.94l2.85,-2.22 0.81,-0.63z M12,5.38c1.62,0 3.06,0.56 4.21,1.64" +
        "l3.15,-3.15C17.45,2.09 14.97,1 12,1c-4.3,0 -8.01,2.47 -9.82,6.06l3.66,2.84" +
        "c0.87,-2.6 3.3,-4.53 6.16,-4.53z"

/** The mark as an [ImageVector]; the fill is a placeholder the `Icon` tint owns. */
private val googleG: ImageVector by lazy {
    ImageVector.Builder(
        name = "GoogleG",
        defaultWidth = 24.dp,
        defaultHeight = 24.dp,
        viewportWidth = 24f,
        viewportHeight = 24f,
    ).addPath(
        pathData = PathParser().parsePathString(GOOGLE_G_PATH).toNodes(),
        fill = SolidColor(Color.Black),
    ).build()
}

/** The Google mark at [size], tinted by the surrounding content colour. */
@Composable
fun GoogleMark(size: Dp) {
    Icon(imageVector = googleG, contentDescription = null, modifier = Modifier.size(size))
}
