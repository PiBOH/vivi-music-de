package com.music.vivi.desktop.presentation

/**
 * The two windows the desktop shell can draw, for one state.
 *
 * The main window owns the shell and is hidden (not closed) in mini player
 * mode; the small window is created from the moment the mode is on and is
 * visible while the app is not closing. `Main.kt` reads this instead of
 * spelling the three conditions out, so the swap can be exercised by a test
 * without a real OS window.
 */
data class MiniPlayerWindows(
    val mainVisible: Boolean,
    val miniCreated: Boolean,
    val miniVisible: Boolean
)

fun miniPlayerWindows(
    isWindowVisible: Boolean,
    isMiniPlayerMode: Boolean,
    isExiting: Boolean
): MiniPlayerWindows {
    val miniCreated = isMiniPlayerMode && !isExiting
    return MiniPlayerWindows(
        mainVisible = isWindowVisible && !isMiniPlayerMode,
        miniCreated = miniCreated,
        miniVisible = isWindowVisible && miniCreated
    )
}
