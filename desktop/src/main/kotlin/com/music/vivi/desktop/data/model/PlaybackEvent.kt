package com.music.vivi.desktop.data.model

import androidx.compose.runtime.Immutable
import kotlinx.serialization.Serializable

@Serializable
@Immutable
data class PlaybackEvent(
    val id: Long = 0L,
    val songId: String,
    val timestamp: Long,
    val durationMs: Long = 0L
)
