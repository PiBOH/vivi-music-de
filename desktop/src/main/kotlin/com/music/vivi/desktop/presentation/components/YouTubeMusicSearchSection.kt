package com.music.vivi.desktop.presentation.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.music.vivi.desktop.data.model.Song
import com.music.vivi.desktop.presentation.viewmodel.YouTubeMusicStateHolder

/**
 * The YouTube Music half of the search screen.
 *
 * The window's search field is the only query there is: this block shows what
 * the catalogue answered for it, next to the local results, and "See all" opens
 * the full page with the same query still in force. It draws nothing while the
 * search field is empty, so a normal library page is unchanged.
 */
@Composable
fun YouTubeMusicSearchSection(
    stateHolder: YouTubeMusicStateHolder,
    currentSong: Song?,
    isPlaying: Boolean,
    onPlaySongs: (List<Song>, Int) -> Unit,
    onPlayNext: (Song) -> Unit,
    onAddToQueue: (Song) -> Unit,
    onSeeAll: () -> Unit,
    modifier: Modifier = Modifier
) {
    val state by stateHolder.state.collectAsState()
    if (state.query.isBlank()) return

    val colors = MaterialTheme.colorScheme
    val preview = state.songs.take(PREVIEW_ROWS)

    Surface(
        shape = RoundedCornerShape(20.dp),
        color = colors.surfaceContainer,
        modifier = modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "YouTube Music",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = colors.onSurface
                )
                if (state.songs.isNotEmpty()) {
                    Spacer(Modifier.width(10.dp))
                    Text(
                        text = "${state.songs.size} results",
                        style = MaterialTheme.typography.bodySmall,
                        color = colors.onSurfaceVariant
                    )
                }
                Spacer(Modifier.weight(1f))
                TextButton(onClick = onSeeAll) {
                    Text("See all")
                }
            }

            when {
                state.isLoading -> Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp)
                ) {
                    CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
                    Spacer(Modifier.width(10.dp))
                    Text(
                        text = "Searching YouTube Music...",
                        style = MaterialTheme.typography.bodySmall,
                        color = colors.onSurfaceVariant
                    )
                }
                state.error != null -> Text(
                    text = state.error.orEmpty(),
                    style = MaterialTheme.typography.bodySmall,
                    color = colors.error,
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp)
                )
                state.hasSearched && state.songs.isEmpty() -> Text(
                    text = "Nothing on YouTube Music for \"${state.query}\"",
                    style = MaterialTheme.typography.bodySmall,
                    color = colors.onSurfaceVariant,
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp)
                )
                else -> Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    preview.forEachIndexed { index, song ->
                        YouTubeMusicRow(
                            song = song,
                            isCurrent = currentSong?.id == song.id,
                            isPlaying = isPlaying,
                            onPlay = { onPlaySongs(state.songs, index) },
                            onPlayNext = { onPlayNext(song) },
                            onAddToQueue = { onAddToQueue(song) }
                        )
                    }
                }
            }
        }
    }
}

private const val PREVIEW_ROWS = 4
