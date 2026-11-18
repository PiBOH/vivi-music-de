package com.music.vivi.desktop.presentation.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.music.vivi.desktop.data.model.Song
import com.music.vivi.desktop.presentation.components.YouTubeMusicRow
import com.music.vivi.desktop.presentation.viewmodel.YouTubeMusicStateHolder

/**
 * YouTube Music search page.
 *
 * The query is the window's own: typing in the top bar searches the catalogue
 * and this page shows the full answer for the same text, so the search screen
 * and this page are one search with two layouts rather than two searches. The
 * catalogue is the `innertube` module; a row plays the whole result page as the
 * queue, so next/previous and shuffle keep working as they do for the library.
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
            text = "Type in the search field above and the catalogue answers here. " +
                "Tracks stream through libmpv, with lyrics from the lyrics provider.",
            style = MaterialTheme.typography.bodyMedium,
            color = colors.onSurfaceVariant
        )
        Spacer(Modifier.height(20.dp))

        when {
            state.query.isBlank() -> Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "Search a song, an album or an artist to start listening.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = colors.onSurfaceVariant,
                    textAlign = TextAlign.Center
                )
            }
            state.isLoading -> Row(verticalAlignment = Alignment.CenterVertically) {
                CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
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
                color = colors.error
            )
            state.hasSearched && state.songs.isEmpty() -> Text(
                text = "Nothing found for \"${state.query}\".",
                style = MaterialTheme.typography.bodySmall,
                color = colors.onSurfaceVariant
            )
            else -> LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(bottom = 32.dp),
                verticalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                itemsIndexed(
                    items = state.songs,
                    key = { _, song -> song.id },
                    contentType = { _, _ -> "yt-song" }
                ) { index, song ->
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
