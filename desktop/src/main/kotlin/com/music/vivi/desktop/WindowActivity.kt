package com.music.vivi.desktop

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue

/**
 * Whether VIVI's window is visible and focused.
 *
 * The player's endless backgrounds keep the window redrawing at the display's
 * rate. Pausing them when the track is paused is not enough: a minimized or
 * unfocused window draws no one, so its loops are pure battery and GPU load.
 * [MainWindow] updates this from the AWT focus and state listeners, and the
 * animated surfaces read it as snapshot state, so the loops stop and restart
 * with the window instead of running behind it.
 */
internal object WindowActivity {
    var active by mutableStateOf(true)
}
