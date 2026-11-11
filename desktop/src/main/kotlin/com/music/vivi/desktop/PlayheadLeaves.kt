package com.music.vivi.desktop

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import kotlin.math.abs
import kotlin.math.roundToInt

/**
 * The seek row of the Expressive player.
 *
 * The playhead is read here, in the row that draws it, so the screen above does
 * not recompose at the audio report rate. The drag is held locally because a
 * seek on a stream is a round trip.
 */
@Composable
internal fun M3ESeekBar(
    positionMs: () -> Long,
    durationMs: Long,
    sliderStyle: ViviSliderStyle,
    onSeek: (Long) -> Unit,
    modifier: Modifier = Modifier,
) {
    val live = positionMs()
    val probe = LocalRecompositionProbe.current
    if (probe != null) SideEffect { probe("m3e-seek") }
    var isSeeking by remember { mutableStateOf(false) }
    var seekValue by remember { mutableStateOf(0f) }
    LaunchedEffect(live, isSeeking) {
        if (isSeeking && abs(live - seekValue) < 1500f) isSeeking = false
    }
    LaunchedEffect(isSeeking) {
        if (isSeeking) {
            kotlinx.coroutines.delay(4000)
            isSeeking = false
        }
    }
    val sliderMax = durationMs.coerceAtLeast(1L)
    val unknownDuration = durationMs <= 0L
    val displayPosition = when {
        isSeeking -> seekValue
        unknownDuration -> (playbackPendingSeekFraction() ?: 0f) * sliderMax
        else -> live.toFloat().coerceIn(0f, sliderMax.toFloat())
    }
    Column(modifier) {
        ViviSlider(
            value = displayPosition.coerceIn(0f, sliderMax.toFloat()),
            onValueChange = {
                seekValue = it.coerceIn(0f, sliderMax.toFloat())
                isSeeking = true
            },
            onValueChangeFinished = {
                if (durationMs > 0) onSeek(seekValue.toLong())
                else onSeek((seekValue * 1000).toLong())
            },
            enabled = !ListenTogetherGate.locked.value,
            valueRange = 0f..sliderMax.toFloat(),
            style = sliderStyle,
            bufferedFraction = playbackBufferedFraction(),
            modifier = Modifier.fillMaxWidth(),
        )
        Row(Modifier.fillMaxWidth()) {
            Text(
                if (unknownDuration) "${(displayPosition * 100).roundToInt()}%"
                else formatTime(displayPosition.toLong()),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(Modifier.weight(1f))
            if (!unknownDuration) {
                val left = durationMs - displayPosition.toLong()
                Text(
                    if (left > 0) "-" + formatTime(left) else formatTime(durationMs),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

/** The seek slider and its clock, the only playhead reader in this screen. */
@Composable
internal fun PlayerSeekControls(
    positionMs: () -> Long,
    durationMs: Long,
    sliderStyle: ViviSliderStyle,
    onSeek: (Long) -> Unit,
) {
    val live = positionMs()
    var isSeeking by remember { mutableStateOf(false) }
    var seekValue by remember { mutableStateOf(0f) }
    LaunchedEffect(live, isSeeking) {
        if (isSeeking && abs(live - seekValue) < 1500f) isSeeking = false
    }
    LaunchedEffect(isSeeking) {
        if (isSeeking) {
            kotlinx.coroutines.delay(4000)
            isSeeking = false
        }
    }
    val sliderMax = durationMs.coerceAtLeast(1L)
    val unknownDuration = durationMs <= 0L
    val displayPosition = when {
        isSeeking -> seekValue
        unknownDuration -> (playbackPendingSeekFraction() ?: 0f) * sliderMax
        else -> live.toFloat().coerceIn(0f, sliderMax.toFloat())
    }
    ViviSlider(
        value = displayPosition.coerceIn(0f, sliderMax.toFloat()),
        onValueChange = {
            seekValue = it.coerceIn(0f, sliderMax.toFloat())
            isSeeking = true
        },
        onValueChangeFinished = {
            if (durationMs > 0) onSeek(seekValue.toLong())
            else onSeek((seekValue * 1000).toLong())
        },
        enabled = !ListenTogetherGate.locked.value,
        valueRange = 0f..sliderMax.toFloat(),
        style = sliderStyle,
        bufferedFraction = playbackBufferedFraction(),
        modifier = Modifier.fillMaxWidth(),
    )
    Row(Modifier.fillMaxWidth()) {
        Text(
            if (unknownDuration) "${(displayPosition * 100).roundToInt()}%"
            else formatTime(displayPosition.toLong()),
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(Modifier.weight(1f))
        if (!unknownDuration) {
            Text(
                formatTime(durationMs),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

/** The classic mini player's seek row, the bar's only playhead reader. */
@Composable
internal fun MiniSeekBar(
    videoId: String,
    positionMs: () -> Long,
    durationMs: Long,
    sliderStyle: ViviSliderStyle,
    mutedColor: Color,
    onSeek: (Long) -> Unit,
) {
    val live = positionMs()
    var isSeeking by remember(videoId) { mutableStateOf(false) }
    var seekValue by remember(videoId) { mutableStateOf(0f) }
    val sliderMax = durationMs.coerceAtLeast(1L)
    val unknownDuration = durationMs <= 0L
    val displayPosition = when {
        isSeeking -> seekValue
        unknownDuration -> (playbackPendingSeekFraction() ?: 0f) * sliderMax
        else -> live.toFloat().coerceIn(0f, sliderMax.toFloat())
    }
    Row(
        Modifier.fillMaxWidth(0.9f),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            if (unknownDuration) "${(displayPosition * 100).roundToInt()}%"
            else formatTime(displayPosition.toLong()),
            style = MaterialTheme.typography.labelSmall,
            color = mutedColor,
        )
        Spacer(Modifier.width(8.dp))
        ViviSlider(
            value = displayPosition.coerceIn(0f, sliderMax.toFloat()),
            onValueChange = {
                seekValue = it.coerceIn(0f, sliderMax.toFloat())
                isSeeking = true
            },
            onValueChangeFinished = {
                if (durationMs > 0) onSeek(seekValue.toLong())
                else onSeek((seekValue * 1000).toLong())
                isSeeking = false
            },
            enabled = !ListenTogetherGate.locked.value,
            valueRange = 0f..sliderMax.toFloat(),
            style = sliderStyle,
            bufferedFraction = playbackBufferedFraction(),
            modifier = Modifier.weight(1f),
        )
        Spacer(Modifier.width(8.dp))
        if (!unknownDuration) {
            Text(
                formatTime(durationMs),
                style = MaterialTheme.typography.labelSmall,
                color = mutedColor,
            )
        }
    }
}

/** The elapsed and remaining clocks, the Apple player's only clock readers. */
@Composable
internal fun AppleTimeRow(positionMs: () -> Long, durationMs: Long) {
    val live = positionMs()
    Row(Modifier.fillMaxWidth()) {
        Text(
            formatTime(live.coerceAtLeast(0)),
            style = MaterialTheme.typography.labelSmall,
            color = PaletteInk.copy(alpha = 0.6f),
        )
        Spacer(Modifier.weight(1f))
        if (durationMs > 0) {
            Text(
                "-" + formatTime((durationMs - live).coerceAtLeast(0)),
                style = MaterialTheme.typography.labelSmall,
                color = PaletteInk.copy(alpha = 0.6f),
            )
        }
    }
}
