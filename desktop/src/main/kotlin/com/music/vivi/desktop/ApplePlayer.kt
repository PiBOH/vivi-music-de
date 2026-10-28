package com.music.vivi.desktop

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.QueueMusic
import androidx.compose.material.icons.filled.Repeat
import androidx.compose.material.icons.filled.RepeatOne
import androidx.compose.material.icons.filled.Shuffle
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.SkipPrevious
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.music.vivi.desktop.player.LoadPhase
import com.music.vivi.desktop.player.RepeatMode

/**
 * The "Apple Music" player: the web player's full-screen view, on a desktop.
 *
 * The reference is the lyrics screen of `music.apple.com`
 * (<https://help.apple.com>): one blurred, colour-soaked backdrop taken from the
 * artwork, the cover and every control in a single column on the left, and the
 * lyrics filling the right half at a large size with the line being sung picked
 * out while the rest wait dimmed. That is the whole layout: no header, no
 * sidebar, a single close control in the corner, so the two things a listener
 * looks at - the song and what it says - are the two things on screen.
 *
 * What is reused from the app instead of rebuilt: the artwork tier
 * ([PlayerThumbnail]), the seek and volume controls ([ViviSlider]), the blurred
 * backdrop ([PlayerBackground] with the Apple Music style, rasterized once at a
 * fixed size) and the lyrics renderer ([LyricsScreen], with its own animation
 * styles, romanization and translation).
 *
 * The times follow Apple's convention rather than the app's: elapsed on the
 * left, the time still to play on the right as a negative (`-3:27`), which is
 * the reading that tells a listener how long what is left lasts.
 */
@Composable
fun AppleMusicPlayer(
    np: NowPlaying,
    positionMs: Long,
    durationMs: Long,
    isPlaying: Boolean,
    volume: Float,
    isShuffle: Boolean,
    repeatMode: RepeatMode,
    language: String,
    accent: Color,
    rotatingThumbnail: Boolean = false,
    /** The decode/resolve phase, drawn under the controls like the shared panel. */
    loadPhase: LoadPhase = LoadPhase.NONE,
    /** A failure to resolve or decode, shown where the shared panel shows it. */
    errorKey: String? = null,
    errorDetail: String? = null,
    onBack: () -> Unit,
    onTogglePlay: () -> Unit,
    onNext: () -> Unit,
    onPrevious: () -> Unit,
    onSeek: (Long) -> Unit,
    onVolume: (Float) -> Unit,
    onToggleShuffle: () -> Unit,
    onCycleRepeat: () -> Unit,
    onOpenQueue: () -> Unit,
    onAddToPlaylist: (() -> Unit)? = null,
) {
    BoxWithConstraints(Modifier.fillMaxSize()) {
        // The backdrop is the artwork itself, blurred and darkened: it is what
        // gives the screen its colour, and it is why every control below is
        // drawn in ink rather than in a theme role.
        PlayerBackground(
            style = PlayerBackgroundStyle.APPLE_MUSIC,
            bgUrl = np.thumbnail,
            accent = accent,
            modifier = Modifier.fillMaxSize(),
            isPlaying = isPlaying,
        )
        // A scrim of its own, over the backdrop. Every control on this screen is
        // drawn in ink rather than in a theme role, so it cannot rely on the
        // surface under it: the app's apple backdrop follows the theme (faint in
        // light mode) and draws nothing at all for a track with no artwork, and
        // white text on either of those is unreadable. This guarantees the
        // contrast in both themes, with or without a cover.
        Box(
            Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        0f to PaletteBlack.copy(alpha = 0.30f),
                        1f to PaletteBlack.copy(alpha = 0.62f),
                    )
                )
        )

        // Two columns from 720 dp up. Narrower than that the lyrics are not drawn
        // at all rather than squeezed into a strip: at 26 sp a 30 % column of
        // them is three words per line, which is not reading. The control panel
        // keeps the app's own lyrics button, so they stay one click away.
        val twoColumns = maxWidth >= 720.dp
        val cover = if (twoColumns) {
            minOf(maxWidth * 0.34f, maxHeight * 0.5f)
        } else {
            minOf(maxWidth * 0.6f, maxHeight * 0.42f)
        }

        Row(
            Modifier.fillMaxSize().padding(horizontal = 28.dp, vertical = 22.dp),
            // One column on a narrow window, centred: pinned to the left it read
            // as a screen that had failed to fill, with half the window empty.
            horizontalArrangement = if (twoColumns) {
                Arrangement.spacedBy(28.dp)
            } else {
                Arrangement.spacedBy(28.dp, Alignment.CenterHorizontally)
            },
        ) {
            Column(
                Modifier
                    .width(cover)
                    .verticalScroll(rememberScrollState()),
            ) {
                PlayerThumbnail(
                    url = np.thumbnail,
                    size = cover,
                    corner = ShapeRadii.small,
                    rotating = rotatingThumbnail,
                )
                Spacer(Modifier.height(18.dp))
                Text(
                    np.title,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = PaletteInk,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        listOfNotNull(np.artist, np.album).distinct().joinToString(" · "),
                        style = MaterialTheme.typography.bodyMedium,
                        color = PaletteInk.copy(alpha = 0.75f),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f),
                    )
                    Surface(
                        onClick = onOpenQueue,
                        shape = CircleShape,
                        color = PaletteInk.copy(alpha = 0.16f),
                        modifier = Modifier.size(26.dp),
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                Icons.Filled.QueueMusic,
                                contentDescription = Localization.get(language, "queue"),
                                tint = PaletteInk,
                                modifier = Modifier.size(15.dp),
                            )
                        }
                    }
                    if (onAddToPlaylist != null) {
                        Spacer(Modifier.width(8.dp))
                        Surface(
                            onClick = onAddToPlaylist,
                            shape = CircleShape,
                            color = PaletteInk.copy(alpha = 0.16f),
                            modifier = Modifier.size(26.dp),
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    Icons.Filled.MoreVert,
                                    contentDescription = Localization.get(language, "more"),
                                    tint = PaletteInk,
                                    modifier = Modifier.size(15.dp),
                                )
                            }
                        }
                    }
                }

                Spacer(Modifier.height(10.dp))
                AppleProgress(
                    positionMs = positionMs,
                    durationMs = durationMs,
                    onSeek = onSeek,
                )
                Row(Modifier.fillMaxWidth()) {
                    Text(
                        formatTime(positionMs.coerceAtLeast(0)),
                        style = MaterialTheme.typography.labelSmall,
                        color = PaletteInk.copy(alpha = 0.6f),
                    )
                    Spacer(Modifier.weight(1f))
                    if (durationMs > 0) {
                        Text(
                            "-" + formatTime((durationMs - positionMs).coerceAtLeast(0)),
                            style = MaterialTheme.typography.labelSmall,
                            color = PaletteInk.copy(alpha = 0.6f),
                        )
                    }
                }

                Spacer(Modifier.height(8.dp))
                AppleTransport(
                    isPlaying = isPlaying,
                    isShuffle = isShuffle,
                    repeatMode = repeatMode,
                    language = language,
                    onTogglePlay = onTogglePlay,
                    onNext = onNext,
                    onPrevious = onPrevious,
                    onToggleShuffle = onToggleShuffle,
                    onCycleRepeat = onCycleRepeat,
                )

                Spacer(Modifier.height(10.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        Icons.Filled.VolumeUp,
                        contentDescription = Localization.get(language, "volume"),
                        tint = PaletteInk.copy(alpha = 0.7f),
                        modifier = Modifier.size(16.dp),
                    )
                    Spacer(Modifier.width(10.dp))
                    ViviSlider(
                        value = volume,
                        onValueChange = onVolume,
                        style = ViviSliderStyle.SLIM,
                        modifier = Modifier.weight(1f),
                    )
                }
                // Load state and failures. This screen draws none of the shared
                // chrome, so without these a track that never resolves would sit
                // here silently and look like a player that had simply stopped.
                if (loadPhase != LoadPhase.NONE) {
                    Spacer(Modifier.height(12.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(14.dp),
                            strokeWidth = 2.dp,
                            color = PaletteInk.copy(alpha = 0.8f),
                        )
                        Spacer(Modifier.width(8.dp))
                        Text(
                            Localization.get(language, if (loadPhase == LoadPhase.RESOLVING) "resolving" else "downloading"),
                            style = MaterialTheme.typography.bodySmall,
                            color = PaletteInk.copy(alpha = 0.8f),
                        )
                    }
                }
                if (errorKey != null || errorDetail != null) {
                    Spacer(Modifier.height(12.dp))
                    if (errorKey != null) {
                        Text(
                            Localization.get(language, errorKey),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.errorContainer,
                        )
                    }
                    if (errorDetail != null) {
                        // Selectable, like the shared panel: the detail carries the
                        // URL that has to be reported.
                        SelectionContainer {
                            Text(
                                errorDetail,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.85f),
                            )
                        }
                    }
                }
                if (twoColumns) Spacer(Modifier.height(18.dp))
            }

            if (twoColumns) {
                Box(Modifier.weight(1f).fillMaxSize()) {
                    LyricsScreen(
                        nowPlaying = np,
                        positionMs = positionMs,
                        isPlaying = isPlaying,
                        language = language,
                        display = LyricsDisplayOptions(
                            textSizeSp = 26f,
                            lineSpacing = 1.55f,
                            appleMusicBlur = true,
                        ),
                        onSeek = onSeek,
                        onTogglePlay = onTogglePlay,
                        onBack = null,
                        // The header belongs to the lyrics screen: here the
                        // column starts at the top of the window.
                        showHeader = false,
                    )
                }
            }
        }

        // The close control: one, in the corner, like the reference. It is what
        // returns to the app, since this layout hides the header and the sidebar.
        Surface(
            onClick = onBack,
            shape = CircleShape,
            color = PaletteBlack.copy(alpha = 0.28f),
            modifier = Modifier.align(Alignment.TopStart).padding(14.dp).size(34.dp),
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    Icons.Filled.Close,
                    contentDescription = Localization.get(language, "close"),
                    tint = PaletteInk,
                    modifier = Modifier.size(18.dp),
                )
            }
        }
    }
}

/**
 * The seek bar of the Apple player: a thin line, and the drag is the seek.
 *
 * It keeps the one rule the old player's panel had to learn: while the pointer
 * is down the live position is ignored, and the thumb is released a moment after
 * the engine reports the new position (a seek on a stream is a round trip, and
 * dropping straight back to the old position made the seek look like it had not
 * moved at all).
 */
@Composable
private fun AppleProgress(
    positionMs: Long,
    durationMs: Long,
    onSeek: (Long) -> Unit,
) {
    var seeking by remember { mutableStateOf(false) }
    var seekTo by remember { mutableStateOf(0f) }
    val max = durationMs.coerceAtLeast(1L)
    val shown = if (seeking) seekTo else positionMs.toFloat().coerceIn(0f, max.toFloat())
    androidx.compose.runtime.LaunchedEffect(positionMs, seeking) {
        if (seeking && kotlin.math.abs(positionMs - seekTo) < 1500f) seeking = false
    }
    ViviSlider(
        value = shown,
        onValueChange = {
            seekTo = it.coerceIn(0f, max.toFloat())
            seeking = true
        },
        onValueChangeFinished = {
            if (durationMs > 0) onSeek(seekTo.toLong()) else seeking = false
        },
        enabled = durationMs > 0 && !ListenTogetherGate.locked.value,
        valueRange = 0f..max.toFloat(),
        style = ViviSliderStyle.SLIM,
        bufferedFraction = playbackBufferedFraction(),
        modifier = Modifier.fillMaxWidth(),
    )
}

/**
 * The five transport controls, in the reference's order and weights: shuffle,
 * previous, play, next, repeat, with the play button the only filled shape.
 */
@Composable
private fun AppleTransport(
    isPlaying: Boolean,
    isShuffle: Boolean,
    repeatMode: RepeatMode,
    language: String,
    onTogglePlay: () -> Unit,
    onNext: () -> Unit,
    onPrevious: () -> Unit,
    onToggleShuffle: () -> Unit,
    onCycleRepeat: () -> Unit,
) {
    val locked = ListenTogetherGate.locked.value
    Row(
        Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Tooltip(Localization.get(language, "shuffle")) {
            Icon(
                Icons.Filled.Shuffle,
                contentDescription = Localization.get(language, "shuffle"),
                tint = PaletteInk.copy(alpha = if (isShuffle) 1f else 0.65f),
                modifier = Modifier
                    .size(19.dp)
                    .iconHoverLift(enabled = !locked, label = "appleShuffle")
                    .clickable(enabled = !locked) { onToggleShuffle() },
            )
        }
        Tooltip(Localization.get(language, "previous")) {
            Icon(
                Icons.Filled.SkipPrevious,
                contentDescription = Localization.get(language, "previous"),
                tint = PaletteInk.copy(alpha = if (locked) 0.4f else 1f),
                modifier = Modifier
                    .size(30.dp)
                    .iconHoverLift(enabled = !locked, label = "applePrevious")
                    .clickable(enabled = !locked) { onPrevious() },
            )
        }
        Surface(
            onClick = onTogglePlay,
            enabled = !locked,
            shape = CircleShape,
            color = PaletteInk.copy(alpha = if (locked) 0.5f else 1f),
            modifier = Modifier.size(52.dp).pointerTilt(enabled = !locked, label = "applePlayTilt"),
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    if (isPlaying) Icons.Filled.Pause else Icons.Filled.PlayArrow,
                    contentDescription = Localization.get(
                        language,
                        if (isPlaying) "pause" else "play",
                    ),
                    tint = PaletteBlack,
                    modifier = Modifier.size(26.dp),
                )
            }
        }
        Tooltip(Localization.get(language, "next")) {
            Icon(
                Icons.Filled.SkipNext,
                contentDescription = Localization.get(language, "next"),
                tint = PaletteInk.copy(alpha = if (locked) 0.4f else 1f),
                modifier = Modifier
                    .size(30.dp)
                    .iconHoverLift(enabled = !locked, label = "appleNext")
                    .clickable(enabled = !locked) { onNext() },
            )
        }
        Tooltip(Localization.get(language, "repeat")) {
            Icon(
                if (repeatMode == RepeatMode.ONE) Icons.Filled.RepeatOne else Icons.Filled.Repeat,
                contentDescription = Localization.get(language, "repeat"),
                tint = PaletteInk.copy(alpha = if (repeatMode == RepeatMode.OFF) 0.65f else 1f),
                modifier = Modifier
                    .size(19.dp)
                    .iconHoverLift(enabled = !locked, label = "appleRepeat")
                    .clickable(enabled = !locked) { onCycleRepeat() },
            )
        }
    }
}
