package com.music.vivi.desktop

/**
 * The UI-density presets, shared by the Settings picker and by the automatic
 * step-down that a small window triggers.
 *
 * Descending order matters: "the next smallest one" is simply the next entry, so
 * both the picker list and the automatic reduction read the same source.
 * 1f = 100%.
 */
val DENSITY_PRESETS: List<Float> = listOf(
    2.0f, 1.8f, 1.5f, 1.4f, 1.3f, 1.25f, 1.2f, 1.1f,
    1f, 0.85f, 0.75f, 0.65f, 0.55f,
)

/** Below this window width (raw layout dp) the sidebar collapses to its icon rail. */
const val SIDEBAR_AUTO_COLLAPSE_DP = 980f

/** Below this window width (raw layout dp) the right Now Playing panel is hidden. */
const val RIGHT_PANEL_AUTO_HIDE_DP = 1150f

/**
 * How many presets to step down for a window this wide. A comfortable window
 * changes nothing; each band below it takes one more step, so shrinking the
 * window shrinks the interface instead of clipping it.
 */
private fun densityStepsDown(windowWidthDp: Float): Int = when {
    windowWidthDp >= 1200f -> 0
    windowWidthDp >= 1050f -> 1
    windowWidthDp >= 920f -> 2
    windowWidthDp >= 800f -> 3
    else -> 4
}

/**
 * The density scale actually used for layout: the user's choice, stepped down
 * (never up) while the window is too small for it.
 *
 * [windowWidthDp] is the window width before any scaling is applied, which is
 * what "the window became very small" means to the person resizing it. The
 * stored setting is never written: growing the window restores the user's own
 * choice exactly.
 */
fun effectiveDensityScale(userScale: Float, windowWidthDp: Float): Float {
    val steps = densityStepsDown(windowWidthDp)
    if (steps == 0) return userScale
    val index = DENSITY_PRESETS.indexOfFirst { it <= userScale + 1e-4f }
        .let { if (it < 0) DENSITY_PRESETS.lastIndex else it }
    return DENSITY_PRESETS[(index + steps).coerceAtMost(DENSITY_PRESETS.lastIndex)]
}
