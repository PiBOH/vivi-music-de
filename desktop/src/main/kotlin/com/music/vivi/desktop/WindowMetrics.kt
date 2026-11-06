package com.music.vivi.desktop

import androidx.compose.runtime.compositionLocalOf

/**
 * The window's width in layout dp *before* the small-window density step-down
 * (see [effectiveDensityScale]).
 *
 * It exists because the player's narrow layout is about the window the person
 * resized, not about the dp space left after the density shrank: at a 700 px
 * window the density drops to 0.75 and the composition reports 933 dp, so a
 * breakpoint compared with `maxWidth` never fires and the player stays in its
 * two-column form in a window that is plainly narrow.
 *
 * Null outside a window (the off-screen render harness), where the caller's own
 * `maxWidth` is the truth and nothing is scaled.
 */
val LocalWindowWidthDp = compositionLocalOf<Float?> { null }
