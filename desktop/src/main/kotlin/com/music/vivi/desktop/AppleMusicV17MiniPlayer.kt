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
import androidx.compose.material.icons.filled.Repeat
import androidx.compose.material.icons.filled.RepeatOne
import androidx.compose.material.icons.filled.Shuffle
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
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
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
    positionMs: () -> Long = { 0L },
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
                // One dp between 34 dp slots: BitChord's 32 px pitch at its
                // 59 px bar is 35 dp at this 64 dp one.
                horizontalArrangement = Arrangement.spacedBy(1.dp),
            ) {
                if (!compact) {
                    MiniTransportButton(Localization.get(language, "shuffle"), onToggleShuffle) {
                        Icon(
                            BitChordIcons.Shuffle,
                            contentDescription = null,
                            tint = if (isShuffle) primaryColor else mutedColor,
                            modifier = Modifier.size(18.dp),
                        )
                    }
                }
                MiniTransportButton(Localization.get(language, "previous"), onPrevious) {
                    V17Glyph(V17TransportGlyph.PREVIOUS, V17MiniSkipGlyph, contentColor)
                }
                MiniTransportButton(
                    Localization.get(language, if (isPlaying) "pause" else "play"),
                    onTogglePlay,
                ) {
                    if (isLoading) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(18.dp),
                            strokeWidth = 2.dp,
                            color = contentColor,
                        )
                    } else {
                        V17Glyph(
                            if (isPlaying) V17TransportGlyph.PAUSE else V17TransportGlyph.PLAY,
                            V17MiniPlayGlyph,
                            contentColor,
                        )
                    }
                }
                MiniTransportButton(Localization.get(language, "next"), onNext) {
                    V17Glyph(V17TransportGlyph.NEXT, V17MiniSkipGlyph, contentColor)
                }
                if (!compact) {
                    MiniTransportButton(Localization.get(language, "repeat"), onCycleRepeat) {
                        Icon(
                            BitChordIcons.Repeat,
                            contentDescription = null,
                            tint = if (repeatMode == RepeatMode.OFF) mutedColor else primaryColor,
                            modifier = Modifier.size(18.dp),
                        )
                    }
                }

                // BitChord leaves a wider breath between the last control and
                // the cover than between the controls themselves.
                Spacer(Modifier.width(22.dp))

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
                        // Apple Music v17's mini bar has a bare line for the
                        // volume, with no knob at its head.
                        showThumb = false,
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
                    (positionMs().toFloat() / durationMs.toFloat()).coerceIn(0f, 1f)
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

/**
 * One control of the bar's transport, on BitChord's own pitch.
 *
 * The reference lays shuffle, previous, play/pause, next and repeat out on a
 * 32 px step at its 59 px bar, which is 35 dp at this 64 dp one. An
 * `IconButton` reserves 48 dp, so the five controls of the bar were spreading
 * 1.5 times wider than the reference; this 34 dp slot with 1 dp between the
 * slots is that pitch. The tooltip stays the button's name, and the semantics
 * keep it announced as one.
 */
@Composable
private fun MiniTransportButton(
    label: String,
    onClick: () -> Unit,
    content: @Composable () -> Unit,
) {
    Tooltip(label) {
        Box(
            Modifier
                .size(V17MiniButton)
                .clickable(role = Role.Button, onClick = onClick)
                .semantics { contentDescription = label },
            contentAlignment = Alignment.Center,
        ) {
            content()
        }
    }
}

/** The bar's control slot: a 35 dp pitch with the 1 dp the row adds. */
private val V17MiniButton = 34.dp

/** The bar's skip glyph: the reference's 15 x 9 px arrow at a 64 dp bar. */
private val V17MiniSkipGlyph = 20.dp

/** The bar's play/pause glyph, at the ratio the player's row uses. */
private val V17MiniPlayGlyph = 28.dp
