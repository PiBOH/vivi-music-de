package com.music.vivi.desktop

import androidx.compose.runtime.staticCompositionLocalOf

/**
 * Test-only recomposition sink.
 *
 * The app never provides it, so the probes cost one null read and no work.
 * `:desktop:playheadRecompositionCheck` provides a counter around the real
 * player screen: the screen and the Expressive container stay constant while
 * the seek leaf follows every playhead tick.
 */
internal val LocalRecompositionProbe = staticCompositionLocalOf<((String) -> Unit)?> { null }
