package com.music.vivi.desktop

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha

/**
 * The Listen Together guest's transport lock, as the player surfaces read it.
 *
 * A guest may not drive playback — the host owns the transport — and the lock
 * itself has been enforced in `PlayerController` since 1.54.22, where every one
 * of the blocked commands answers with the room's own sentence (`lt_guest_note`).
 * That answer is right for a keyboard shortcut or a media key, where there is no
 * control to grey out and silence would read as a broken app. It is the wrong
 * shape for a button: a play button that looks enabled and does nothing is what
 * the report described ("the button is there but nothing tells me I can't use
 * it"), and the mobile app disables exactly these controls instead.
 *
 * So the buttons are now drawn disabled — greyed, not clickable, with the
 * sentence as their tooltip — through [transportLocked] and [dimmedWhenLocked],
 * and the keyboard/media-key path keeps the message.
 *
 * The state is [ListenTogetherGate], which the manager already keeps in step with
 * the room (in a room and not its host), so nothing new has to be published.
 */

/** Alpha a transport control falls back to while this user is a locked guest. */
internal const val LOCKED_CONTROL_ALPHA = 0.38f

/**
 * True while this user is a guest in a Listen Together room. Read inside a
 * composable so the control redraws the moment the role changes (entering or
 * leaving a room, or a host transfer).
 */
@Composable
internal fun transportLocked(): Boolean = ListenTogetherGate.locked.value

/**
 * Greys out whatever it is applied to while this user is a locked guest, so it
 * reads as unavailable next to its `enabled = !transportLocked()`.
 */
@Composable
internal fun Modifier.dimmedWhenLocked(): Modifier =
    if (transportLocked()) alpha(LOCKED_CONTROL_ALPHA) else this

/** The tooltip a blocked transport control shows: the room's own sentence. */
internal fun guestLockTooltip(language: String): String =
    Localization.get(language, "lt_guest_note")
