package com.music.vivi.desktop.presentation.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.music.vivi.desktop.data.model.Song
import com.music.vivi.desktop.generated.resources.Res
import com.music.vivi.desktop.generated.resources.ic_automirrored_rounded_playlist_play
import com.music.vivi.desktop.generated.resources.ic_automirrored_rounded_queue_music
import com.music.vivi.desktop.generated.resources.ic_rounded_play_arrow
import com.music.vivi.desktop.presentation.components.subcomps.AppTooltip
import org.jetbrains.compose.resources.painterResource

/**
 * One YouTube Music search result.
 *
 * The page and the compact block the search screen shows draw the same row, so a
 * track looks the same wherever it is found. The row itself sends no request:
 * the stream is resolved when it is played.
 */
@Composable
fun YouTubeMusicRow(
    song: Song,
    isCurrent: Boolean,
    isPlaying: Boolean,
    onPlay: () -> Unit,
    onPlayNext: () -> Unit,
    onAddToQueue: () -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = MaterialTheme.colorScheme

    Row(
        modifier = modifier
            .fillMaxWidth()
            .background(
                color = if (isCurrent) colors.primaryContainer.copy(alpha = 0.35f) else colors.surface,
                shape = RoundedCornerShape(12.dp)
            )
            .clickable(onClick = onPlay)
            .padding(horizontal = 10.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        AsyncArtworkImage(
            artworkUri = song.albumArtUriString,
            modifier = Modifier.size(48.dp),
            targetSize = ArtworkSize.SMALL,
            shape = MaterialTheme.shapes.medium
        )
        Spacer(Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            AppTooltip(song.title) {
                Text(
                    text = song.title,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = if (isCurrent) FontWeight.SemiBold else FontWeight.Normal,
                    color = if (isCurrent) colors.primary else colors.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            AppTooltip(buildString {
                append(song.artist)
                if (song.album.isNotBlank()) append(" - ").append(song.album)
            }) {
                Text(
                    text = buildString {
                        append(song.artist)
                        if (song.album.isNotBlank()) append(" - ").append(song.album)
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = colors.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
        Text(
            text = formatRowDuration(song.duration),
            style = MaterialTheme.typography.bodySmall,
            color = colors.onSurfaceVariant
        )
        Spacer(Modifier.width(8.dp))
        AppTooltip(text = if (isCurrent && isPlaying) "Playing" else "Play") {
            IconButton(onClick = onPlay) {
                Icon(
                    painter = painterResource(Res.drawable.ic_rounded_play_arrow),
                    contentDescription = "Play",
                    tint = if (isCurrent) colors.primary else colors.onSurfaceVariant
                )
            }
        }
        AppTooltip(text = "Play next") {
            IconButton(onClick = onPlayNext) {
                Icon(
                    painter = painterResource(Res.drawable.ic_automirrored_rounded_playlist_play),
                    contentDescription = "Play next",
                    tint = colors.onSurfaceVariant
                )
            }
        }
        AppTooltip(text = "Add to queue") {
            IconButton(onClick = onAddToQueue) {
                Icon(
                    painter = painterResource(Res.drawable.ic_automirrored_rounded_queue_music),
                    contentDescription = "Add to queue",
                    tint = colors.onSurfaceVariant
                )
            }
        }
    }
}

/** `m:ss`, or a hyphen when the catalogue did not report a duration. */
private fun formatRowDuration(durationMs: Long): String {
    if (durationMs <= 0L) return "-"
    val totalSeconds = durationMs / 1000L
    val minutes = totalSeconds / 60L
    val seconds = totalSeconds % 60L
    return "$minutes:${seconds.toString().padStart(2, '0')}"
}
