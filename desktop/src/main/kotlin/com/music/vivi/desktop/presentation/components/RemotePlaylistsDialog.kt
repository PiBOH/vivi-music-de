package com.music.vivi.desktop.presentation.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.music.vivi.desktop.data.youtube.RemotePlaylist
import com.music.vivi.desktop.data.youtube.YouTubePlaylistsRepository
import kotlinx.coroutines.launch
import org.koin.compose.koinInject

/**
 * Picks one of the account's playlists and adds every YouTube track in the
 * selection to it. A local file has no video id, so the caller only opens this
 * dialog when there is at least one YouTube track.
 */
@Composable
fun RemotePlaylistsDialog(
    videoIds: List<String>,
    onDismiss: () -> Unit,
    repository: YouTubePlaylistsRepository = koinInject()
) {
    TrackDialogInput()
    val colors = MaterialTheme.colorScheme
    val scope = rememberCoroutineScope()
    var playlists by remember { mutableStateOf<List<RemotePlaylist>>(emptyList()) }
    var loading by remember { mutableStateOf(true) }
    var error by remember { mutableStateOf<String?>(null) }
    var message by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(Unit) {
        repository.load()
            .onSuccess { playlists = it }
            .onFailure { error = it.message ?: "The account playlists could not be read." }
        loading = false
    }

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(24.dp),
            color = colors.surfaceContainerHigh,
            tonalElevation = 6.dp,
            modifier = Modifier.width(440.dp).heightIn(max = 520.dp)
        ) {
            Column(modifier = Modifier.fillMaxSize().padding(20.dp)) {
                Text(
                    text = "Add to YouTube Music",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = colors.onSurface
                )
                Text(
                    text = if (videoIds.size == 1) "One track" else "${videoIds.size} tracks",
                    style = MaterialTheme.typography.bodySmall,
                    color = colors.onSurfaceVariant
                )

                Spacer(Modifier.height(12.dp))

                when {
                    loading -> Box(
                        modifier = Modifier.weight(1f).fillMaxWidth(),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator()
                    }

                    error != null -> Text(
                        text = error ?: "",
                        style = MaterialTheme.typography.bodyMedium,
                        color = colors.error
                    )

                    playlists.isEmpty() -> Text(
                        text = "No editable playlist was found in this account.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = colors.onSurfaceVariant
                    )

                    else -> LazyColumn(
                        modifier = Modifier.weight(1f).fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        items(items = playlists, key = { it.id }) { playlist ->
                            Surface(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(12.dp))
                                    .clickable(enabled = playlist.isEditable) {
                                        scope.launch {
                                            message = "Adding..."
                                            var added = 0
                                            var failure: String? = null
                                            videoIds.forEach { videoId ->
                                                repository.addSong(playlist.id, videoId).fold(
                                                    onSuccess = { added++ },
                                                    onFailure = { failure = it.message }
                                                )
                                            }
                                            message = if (failure != null && added == 0) {
                                                failure
                                            } else {
                                                "Added $added track${if (added == 1) "" else "s"} to ${playlist.title}."
                                            }
                                        }
                                    },
                                shape = RoundedCornerShape(12.dp),
                                color = colors.surfaceContainerLow
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth().padding(12.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = playlist.title,
                                            style = MaterialTheme.typography.titleSmall,
                                            color = colors.onSurface,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                        playlist.songCountText?.let {
                                            Text(
                                                text = it,
                                                style = MaterialTheme.typography.bodySmall,
                                                color = colors.onSurfaceVariant
                                            )
                                        }
                                    }
                                    if (!playlist.isEditable) {
                                        Text(
                                            text = "Read only",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = colors.onSurfaceVariant
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                message?.let {
                    Spacer(Modifier.height(8.dp))
                    Text(
                        text = it,
                        style = MaterialTheme.typography.bodySmall,
                        color = colors.primary
                    )
                }

                Spacer(Modifier.height(8.dp))
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                    TextButton(onClick = onDismiss) { Text("Close") }
                }
            }
        }
    }
}
