package com.music.vivi.desktop

/**
 * The UI-density presets, shared by the Settings picker and by the automatic
 * step-down that a small window triggers.
 *
 * Descending order matters: "the next smallest one" is simply the next entry, so
 * both the picker list and the automatic reduction read the same source.
 * 1f = 100%.
 *
 * The reduction itself only starts below [FULL_LAYOUT_MIN_WIDTH_DP], so a
 * window at 800x600 still shows the whole layout at the user's own scale.
 */
val DENSITY_PRESETS: List<Float> = listOf(
    2.0f, 1.8f, 1.5f, 1.4f, 1.3f, 1.25f, 1.2f, 1.1f,
    1f, 0.85f, 0.75f, 0.65f, 0.55f,
)

/**
 * The width the interface is designed for. At and above it NOTHING is taken
 * away: the sidebar is expanded, the right panel is visible and the density is
 * exactly the user's own choice.
 *
 * 800 is 800x600 at 100% display scaling — the smallest desktop the app is
 * expected to run on — and it is deliberately the point where the layout starts
 * giving things up, so the classic "everything on screen" layout survives on a
 * 800x600 window (it used to collapse the sidebar at 980 and hide the right
 * panel at 1150, i.e. both were already gone on that display).
 */
const val FULL_LAYOUT_MIN_WIDTH_DP = 800f

/**
 * Below this window width (raw layout dp) the sidebar collapses to its icon
 * rail. It is the LAST thing to go: the density steps down and the right panel
 * hides first, so the sidebar is only traded away when the window is genuinely
 * too narrow for it.
 */
const val SIDEBAR_AUTO_COLLAPSE_DP = 700f

/** Below this window width (raw layout dp) the right Now Playing panel is hidden. */
const val RIGHT_PANEL_AUTO_HIDE_DP = FULL_LAYOUT_MIN_WIDTH_DP

/**
 * How many presets to step down for a window this wide. A window at or above
 * [FULL_LAYOUT_MIN_WIDTH_DP] changes nothing; each band below it takes one more
 * step, so shrinking the window shrinks the interface instead of clipping it.
 */
private fun densityStepsDown(windowWidthDp: Float): Int = when {
    windowWidthDp >= FULL_LAYOUT_MIN_WIDTH_DP -> 0
    windowWidthDp >= 750f -> 1
    windowWidthDp >= 700f -> 2
    windowWidthDp >= 650f -> 3
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
