package com.music.vivi.desktop

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import com.music.vivi.desktop.player.PlayerController
import com.music.vivi.desktop.player.PlayerState
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString

/**
 * The queue the app remembers between runs.
 *
 * With [persistentQueue] on, the queue is written to the settings file on every
 * change and restored on startup, paused and not auto-played.
 */
@Composable
internal fun PersistentQueueEffect(
    player: PlayerController,
    playerState: PlayerState,
    persistentQueue: Boolean,
) {
    // Persistent queue: restore the saved queue on startup (paused, not auto-played).
    LaunchedEffect(Unit) {
        val s = DesktopSettings.load()
        if (s.persistentQueue && s.queueJson.isNotBlank()) {
            runCatching { queueJson.decodeFromString<List<NowPlaying>>(s.queueJson) }
                .getOrNull()
                ?.let { player.restoreQueue(it, s.queueIndex) }
        }
    }

    // Persistent queue: save the queue whenever it changes.
    LaunchedEffect(playerState.queue, playerState.index, persistentQueue) {
        if (persistentQueue && playerState.queue.isNotEmpty()) {
            DesktopSettings.update {
                it.copy(queueJson = queueJson.encodeToString(playerState.queue), queueIndex = playerState.index)
            }
        }
    }
}

private val queueJson = sharedJson
