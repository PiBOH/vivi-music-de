package com.music.vivi.devicesync

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Whether the phone is currently paired with the desktop edition.
 *
 * While paired the screen must stay on: the OS sleeping the display suspends
 * the network and tears the sync socket down. `MainActivity` already adds
 * `FLAG_KEEP_SCREEN_ON` on the window, but the player and the lyrics views each
 * own their own `DisposableEffect` that CLEARS the flag as soon as their own
 * condition stops holding (the player collapses, the lyrics close) — which was
 * silently defeating the paired keep-on. This shared flag lets those views know
 * a pairing is active so they never clear it while the desktop needs them
 * connected.
 */
object ScreenAwake {
    private val _paired = MutableStateFlow(false)

    /** True while this device is paired with a desktop (or another device). */
    val paired: StateFlow<Boolean> = _paired.asStateFlow()

    fun setPaired(value: Boolean) {
        _paired.value = value
    }
}
