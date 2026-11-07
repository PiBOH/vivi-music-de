package com.music.vivi.desktop

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.QueueMusic
import androidx.compose.material.icons.automirrored.filled.Subject
import androidx.compose.material.icons.filled.Headphones
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Repeat
import androidx.compose.material.icons.filled.RepeatOne
import androidx.compose.material.icons.filled.Shuffle
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.SkipPrevious
import androidx.compose.material.icons.filled.VolumeDown
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.music.vivi.desktop.player.AudioOutput
import com.music.vivi.desktop.player.RepeatMode

/**
 * "Apple Music v17": BitChord's player bar, kept at the bottom of the DE window
 * as a mini player.
 *
 * It is the whole transport of that bar (shuffle, previous, play/pause, next,
 * repeat), the artwork with BitChord's swipe, the title and the artist, the
 * volume, the output-device picker, the lyrics and the queue, over the same
 * PlayerController as every other mini player. The bar sits at the bottom of the
 * window and the artwork still changes track when dragged sideways.
 *
 * Ported from BitChord's `PlayerBar` (GPLv3).
 */
@Composable
internal fun AppleMusicV17MiniPlayer(
    nowPlaying: NowPlaying,
    isPlaying: Boolean,
    isLoading: Boolean,
    swipeThumbnail: Boolean,
    swipeSensitivity: Float,
    onTogglePlay: () -> Unit,
    onNext: () -> Unit,
    onPrevious: () -> Unit,
    onOpenPlayer: () -> Unit,
    language: String,
    volume: Float = 0f,
    isShuffle: Boolean = false,
    repeatMode: RepeatMode = RepeatMode.OFF,
    positionMs: Long = 0L,
    durationMs: Long = 0L,
    onVolume: (Float) -> Unit = {},
    onToggleShuffle: () -> Unit = {},
    onCycleRepeat: () -> Unit = {},
    onOpenLyrics: () -> Unit = {},
    onOpenQueue: () -> Unit = {},
    modifier: Modifier = Modifier,
) {
    val contentColor = MaterialTheme.colorScheme.onSurface
    val mutedColor = MaterialTheme.colorScheme.onSurfaceVariant
    val primaryColor = MaterialTheme.colorScheme.primary
    var showOutputPicker by remember { mutableStateOf(false) }

    BoxWithConstraints(modifier.fillMaxWidth()) {
        // Compact is a narrow window, measured before the density step-down:
        // otherwise a 700 px window reports 933 dp and the bar keeps the whole
        // transport for a slot that is half its size. No window (harness) uses
        // the composition's own width.
        val windowWidthDp = LocalWindowWidthDp.current
        val compact = if (windowWidthDp != null) windowWidthDp < 720f else maxWidth < 720.dp
        // No width cap: the bar is the window's own bottom edge, and the 1200 dp
        // maximum left the parent's colour uncovered on every wider window,
        // which is the empty strip that showed to the right of the last control.
        Column(
            Modifier
                .fillMaxWidth()
                .background(MaterialTheme.colorScheme.surfaceContainerLow),
        ) {
            Row(
                Modifier
                    .fillMaxWidth()
                    .height(64.dp)
                    .padding(horizontal = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(2.dp),
            ) {
                if (!compact) {
                    Tooltip(Localization.get(language, "shuffle")) {
                        M3IconButton(onClick = onToggleShuffle) {
                            Icon(
                                BitChordIcons.Shuffle,
                                contentDescription = Localization.get(language, "shuffle"),
                                tint = if (isShuffle) primaryColor else mutedColor,
                                modifier = Modifier.size(18.dp),
                            )
                        }
                    }
                }
                Tooltip(Localization.get(language, "previous")) {
                    M3IconButton(onClick = onPrevious) {
                        Icon(
                            Icons.Filled.SkipPrevious,
                            contentDescription = Localization.get(language, "previous"),
                            tint = contentColor,
                            modifier = Modifier.size(22.dp),
                        )
                    }
                }
                Tooltip(Localization.get(language, if (isPlaying) "pause" else "play")) {
                    M3IconButton(onClick = onTogglePlay) {
                        if (isLoading) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(18.dp),
                                strokeWidth = 2.dp,
                                color = contentColor,
                            )
                        } else {
                            Icon(
                                if (isPlaying) Icons.Filled.Pause else Icons.Filled.PlayArrow,
                                contentDescription = Localization.get(language, if (isPlaying) "pause" else "play"),
                                tint = contentColor,
                                modifier = Modifier.size(26.dp),
                            )
                        }
                    }
                }
                Tooltip(Localization.get(language, "next")) {
                    M3IconButton(onClick = onNext) {
                        Icon(
                            Icons.Filled.SkipNext,
                            contentDescription = Localization.get(language, "next"),
                            tint = contentColor,
                            modifier = Modifier.size(22.dp),
                        )
                    }
                }
                if (!compact) {
                    Tooltip(Localization.get(language, "repeat")) {
                        M3IconButton(onClick = onCycleRepeat) {
                            Icon(
                                BitChordIcons.Repeat,
                                contentDescription = Localization.get(language, "repeat"),
                                tint = if (repeatMode == RepeatMode.OFF) mutedColor else primaryColor,
                                modifier = Modifier.size(18.dp),
                            )
                        }
                    }
                }

                Spacer(Modifier.width(8.dp))

                Box(
                    Modifier
                        .size(40.dp)
                        .clip(MaterialTheme.shapes.extraSmall)
                        .swipeToChangeTrack(
                            enabled = swipeThumbnail,
                            sensitivity = swipeSensitivity,
                            key = nowPlaying.videoId,
                            onNext = onNext,
                            onPrevious = onPrevious,
                        )
                        .clickable(onClick = onOpenPlayer),
                ) {
                    PlayerThumbnail(nowPlaying.thumbnail, 40.dp, false)
                }

                Spacer(Modifier.width(10.dp))

                Column(
                    Modifier
                        .weight(1f)
                        // Pressing the title or the artist opens the full player.
                        // The press is read on the initial pass: the hover tooltip
                        // that wraps each text handled the pointer first, so the
                        // bar's clickable never saw the tap and the press selected
                        // nothing instead of opening the player.
                        .pointerInput(nowPlaying.videoId) {
                            awaitPointerEventScope {
                                while (true) {
                                    val event = awaitPointerEvent(PointerEventPass.Initial)
                                    if (event.type == PointerEventType.Press) onOpenPlayer()
                                }
                            }
                        },
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Tooltip(nowPlaying.title) {
                        Text(
                            nowPlaying.title,
                            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                            color = contentColor,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                    Tooltip(nowPlaying.artist) {
                        Text(
                            nowPlaying.artist,
                            style = MaterialTheme.typography.bodySmall,
                            color = mutedColor,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                }

                Spacer(Modifier.width(10.dp))

                if (!compact) {
                    Icon(
                        Icons.Filled.VolumeDown,
                        contentDescription = Localization.get(language, "volume"),
                        tint = mutedColor,
                        modifier = Modifier.size(16.dp),
                    )
                    ViviSlider(
                        value = volume.coerceIn(0f, 1f),
                        onValueChange = onVolume,
                        valueRange = 0f..1f,
                        style = ViviSliderStyle.SLIM,
                        modifier = Modifier.width(104.dp),
                    )
                }

                Tooltip(Localization.get(language, "tooltip_output_device")) {
                    M3IconButton(onClick = { showOutputPicker = true }) {
                        Icon(
                            Icons.Filled.Headphones,
                            contentDescription = Localization.get(language, "tooltip_output_device"),
                            tint = mutedColor,
                            modifier = Modifier.size(19.dp),
                        )
                    }
                }
                Tooltip(Localization.get(language, "tooltip_lyrics")) {
                    M3IconButton(onClick = onOpenLyrics) {
                        Icon(
                            BitChordIcons.LyricsQuote,
                            contentDescription = Localization.get(language, "tooltip_lyrics"),
                            tint = mutedColor,
                            modifier = Modifier.size(19.dp),
                        )
                    }
                }
                Tooltip(Localization.get(language, "tooltip_queue")) {
                    M3IconButton(onClick = onOpenQueue) {
                        Icon(
                            BitChordIcons.Queue,
                            contentDescription = Localization.get(language, "tooltip_queue"),
                            tint = mutedColor,
                            modifier = Modifier.size(19.dp),
                        )
                    }
                }
            }

            // BitChord's bar carries the position as a hairline at its own edge.
            Box(
                Modifier
                    .fillMaxWidth()
                    .height(2.dp)
                    .background(MaterialTheme.colorScheme.surfaceVariant),
            ) {
                val fraction = if (durationMs > 0L) {
                    (positionMs.toFloat() / durationMs.toFloat()).coerceIn(0f, 1f)
                } else {
                    0f
                }
                if (fraction > 0f) {
                    Box(
                        Modifier
                            .fillMaxWidth(fraction)
                            .height(2.dp)
                            .background(primaryColor),
                    )
                }
            }
        }
    }

    if (showOutputPicker) {
        OutputDeviceDialog(
            language = language,
            onDismiss = { showOutputPicker = false },
            onSelect = { name ->
                AudioOutput.apply(name)
                showOutputPicker = false
            },
        )
    }
}
