package com.music.vivi.desktop.presentation.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import org.jetbrains.compose.resources.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.music.vivi.desktop.data.model.Song
import com.music.vivi.desktop.generated.resources.Res
import com.music.vivi.desktop.generated.resources.ic_automirrored_rounded_playlist_play
import com.music.vivi.desktop.generated.resources.ic_automirrored_rounded_queue_music
import com.music.vivi.desktop.generated.resources.ic_rounded_clear
import com.music.vivi.desktop.generated.resources.ic_rounded_play_arrow
import com.music.vivi.desktop.generated.resources.ic_rounded_search
import com.music.vivi.desktop.presentation.components.AsyncArtworkImage
import com.music.vivi.desktop.presentation.components.ArtworkSize
import com.music.vivi.desktop.presentation.components.subcomps.AppTooltip
import com.music.vivi.desktop.presentation.viewmodel.YouTubeMusicStateHolder

/**
 * YouTube Music search page.
 *
 * The catalogue is the `innertube` module; a row plays the whole result page as
 * the queue, so the next/previous controls and shuffle keep working exactly as
 * they do for the local library.
 */
@Composable
fun YouTubeMusicScreen(
    stateHolder: YouTubeMusicStateHolder,
    currentSong: Song?,
    isPlaying: Boolean,
    onPlaySongs: (List<Song>, Int) -> Unit,
    onPlayNext: (Song) -> Unit,
    onAddToQueue: (Song) -> Unit,
    modifier: Modifier = Modifier
) {
    val state by stateHolder.state.collectAsState()
    val colors = MaterialTheme.colorScheme

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 28.dp, vertical = 24.dp)
    ) {
        Text(
            text = "YouTube Music",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
            color = colors.onSurface
        )
        Spacer(Modifier.height(6.dp))
        Text(
            text = "Search the catalogue. Tracks stream through libmpv, with lyrics from the lyrics provider.",
            style = MaterialTheme.typography.bodyMedium,
            color = colors.onSurfaceVariant
        )
        Spacer(Modifier.height(20.dp))

        OutlinedTextField(
            value = state.query,
            onValueChange = stateHolder::onQueryChange,
            modifier = Modifier
                .fillMaxWidth(0.62f)
                .onPreviewKeyEvent { event ->
                    // KeyboardActions(onSearch) is the soft-keyboard IME action and a
                    // desktop never sends it, so Enter is handled here.
                    if (event.type == KeyEventType.KeyDown &&
                        (event.key == Key.Enter || event.key == Key.NumPadEnter)
                    ) {
                        stateHolder.submit()
                        true
                    } else {
                        false
                    }
                },
            placeholder = { Text("Songs, albums, artists") },
            singleLine = true,
            shape = MaterialTheme.shapes.large,
            leadingIcon = {
                Icon(
                    painter = painterResource(Res.drawable.ic_rounded_search),
                    contentDescription = null
                )
            },
            trailingIcon = {
                if (state.query.isNotEmpty()) {
                    AppTooltip(text = "Clear search") {
                        IconButton(onClick = stateHolder::clear) {
                            Icon(
                                painter = painterResource(Res.drawable.ic_rounded_clear),
                                contentDescription = "Clear search"
                            )
                        }
                    }
                }
            }
        )

        Spacer(Modifier.height(8.dp))

        when {
            state.isLoading -> {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
                    Spacer(Modifier.width(10.dp))
                    Text(
                        text = "Searching YouTube Music...",
                        style = MaterialTheme.typography.bodySmall,
                        color = colors.onSurfaceVariant
                    )
                }
            }
            state.error != null -> {
                Text(
                    text = state.error.orEmpty(),
                    style = MaterialTheme.typography.bodySmall,
                    color = colors.error
                )
            }
            state.hasSearched && state.songs.isEmpty() -> {
                Text(
                    text = "Nothing found for \"${state.query}\".",
                    style = MaterialTheme.typography.bodySmall,
                    color = colors.onSurfaceVariant
                )
            }
            else -> Unit
        }

        if (state.songs.isNotEmpty()) {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(top = 12.dp),
                contentPadding = PaddingValues(bottom = 32.dp),
                verticalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                itemsIndexed(
                    items = state.songs,
                    key = { _, song -> song.id },
                    contentType = { _, _ -> "yt-song" }
                ) { index, song ->
                    val isCurrent = currentSong?.id == song.id
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(
                                color = if (isCurrent) colors.primaryContainer.copy(alpha = 0.35f) else colors.surface,
                                shape = RoundedCornerShape(12.dp)
                            )
                            .clickable { onPlaySongs(state.songs, index) }
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
                            Text(
                                text = song.title,
                                style = MaterialTheme.typography.bodyLarge,
                                fontWeight = if (isCurrent) FontWeight.SemiBold else FontWeight.Normal,
                                color = if (isCurrent) colors.primary else colors.onSurface,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
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
                        Text(
                            text = formatDuration(song.duration),
                            style = MaterialTheme.typography.bodySmall,
                            color = colors.onSurfaceVariant
                        )
                        Spacer(Modifier.width(8.dp))
                        AppTooltip(text = if (isCurrent && isPlaying) "Playing" else "Play") {
                            IconButton(onClick = { onPlaySongs(state.songs, index) }) {
                                Icon(
                                    painter = painterResource(Res.drawable.ic_rounded_play_arrow),
                                    contentDescription = "Play",
                                    tint = if (isCurrent) colors.primary else colors.onSurfaceVariant
                                )
                            }
                        }
                        AppTooltip(text = "Play next") {
                            IconButton(onClick = { onPlayNext(song) }) {
                                Icon(
                                    painter = painterResource(Res.drawable.ic_automirrored_rounded_playlist_play),
                                    contentDescription = "Play next",
                                    tint = colors.onSurfaceVariant
                                )
                            }
                        }
                        AppTooltip(text = "Add to queue") {
                            IconButton(onClick = { onAddToQueue(song) }) {
                                Icon(
                                    painter = painterResource(Res.drawable.ic_automirrored_rounded_queue_music),
                                    contentDescription = "Add to queue",
                                    tint = colors.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
            }
        } else if (!state.hasSearched && !state.isLoading) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text(
                    text = "Search a song, an album or an artist to start listening.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = colors.onSurfaceVariant
                )
            }
        }
    }
}

private fun formatDuration(durationMs: Long): String {
    if (durationMs <= 0L) return "-"
    val totalSeconds = durationMs / 1000L
    val minutes = totalSeconds / 60L
    val seconds = totalSeconds % 60L
    return "$minutes:${seconds.toString().padStart(2, '0')}"
}
