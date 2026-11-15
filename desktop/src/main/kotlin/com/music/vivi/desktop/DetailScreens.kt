package com.music.vivi.desktop

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.PlaylistAdd
import androidx.compose.material.icons.automirrored.filled.PlaylistPlay
import androidx.compose.material.icons.automirrored.filled.QueueMusic
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Shuffle
import androidx.compose.material3.Button
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.music.innertube.YouTube
import com.music.innertube.models.AlbumItem
import com.music.innertube.models.Artist
import com.music.innertube.models.BrowseEndpoint
import com.music.innertube.models.SongItem
import com.music.innertube.pages.AlbumPage
import com.music.innertube.pages.ArtistItemsPage
import com.music.innertube.pages.ArtistPage
import com.music.innertube.pages.HistoryPage
import com.music.innertube.pages.PlaylistPage
import kotlinx.coroutines.launch

@Composable
fun PlaylistScreen(
    playlistId: String,
    language: String,
    onBack: () -> Unit,
    onOpenArtist: (String) -> Unit,
    onPlaySong: (SongItem) -> Unit,
    onAddToQueue: (SongItem) -> Unit,
    onAddToPlaylist: (SongItem) -> Unit,
    onPlayAll: (List<SongItem>) -> Unit,
    onShuffleAll: (List<SongItem>) -> Unit,
    /** Header menu: queue the whole playlist right after the current track. */
    onPlayNext: (List<SongItem>) -> Unit = {},
    /** Header menu: append the whole playlist to the queue. */
    onAddAllToQueue: (List<SongItem>) -> Unit = {},
) {
    var page by remember { mutableStateOf<PlaylistPage?>(null) }
    var error by remember { mutableStateOf<String?>(null) }
    // Bumped by the header menu's "Refresh": re-runs the load below.
    var refreshKey by remember { mutableStateOf(0) }

    LaunchedEffect(playlistId, refreshKey) {
        YouTube.playlist(playlistId).fold(
            onSuccess = { page = it.filteredContent() },
            onFailure = { error = it.message },
        )
    }

    Column(Modifier.fillMaxSize().padding(16.dp)) {
        when {
            error != null -> ErrorBox(language, error)
            page == null -> LoadingBox(language)
            else -> {
                val playlist = page!!.playlist
                val playlistSongs = page!!.songs
                val scope = rememberCoroutineScope()
                val playlistLiked = DetailActions.isCollectionLiked(playlist.id)
                GradientHeader(
                    title = playlist.title,
                    subtitle = playlist.author?.name,
                    meta = playlist.songCountText,
                    thumbnailUrl = playlist.thumbnail,
                    language = language,
                    menuEntries = listOfNotNull(
                        DetailMenuEntry(Localization.get(language, "play_all"), Icons.Filled.PlayArrow, enabled = !transportLocked()) {
                            onPlayAll(playlistSongs)
                        },
                        DetailMenuEntry(Localization.get(language, "shuffle_all"), Icons.Filled.Shuffle, enabled = !transportLocked()) {
                            onShuffleAll(playlistSongs)
                        },
                        DetailMenuEntry(Localization.get(language, "play_next"), Icons.AutoMirrored.Filled.PlaylistPlay, enabled = !transportLocked()) {
                            onPlayNext(playlistSongs)
                        },
                        DetailMenuEntry(Localization.get(language, "add_to_queue"), Icons.AutoMirrored.Filled.QueueMusic, enabled = !transportLocked()) {
                            onAddAllToQueue(playlistSongs)
                        },
                        DetailMenuEntry(Localization.get(language, "add_to_playlist"), Icons.AutoMirrored.Filled.PlaylistAdd) {
                            playlistSongs.firstOrNull()?.let(onAddToPlaylist)
                        },
                        DetailMenuEntry(
                            Localization.get(language, if (playlistLiked) "unlike" else "like"),
                            if (playlistLiked) Icons.Filled.Favorite else Icons.Filled.FavoriteBorder,
                        ) {
                            val next = DetailActions.toggleCollectionLiked(playlist.id)
                            scope.launch { YouTube.likePlaylist(playlist.id, next) }
                        },
                        DetailMenuEntry(Localization.get(language, "share"), Icons.Filled.Share) {
                            copyToClipboard(playlist.shareLink)
                            DesktopSnackbar.show(Localization.get(language, "copied_to_clipboard"))
                        },
                        DetailMenuEntry(Localization.get(language, "refresh"), Icons.Filled.Refresh) {
                            refreshKey++
                        },
                    ),
                    actions = {
                        if (playlistSongs.isNotEmpty()) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                            ) {
                                Button(onClick = { onPlayAll(playlistSongs) }) { Text(Localization.get(language, "play_all")) }
                                TextButton(onClick = { onShuffleAll(playlistSongs) }) {
                                    Icon(Icons.Filled.Shuffle, contentDescription = null, modifier = Modifier.size(18.dp))
                                    Spacer(Modifier.width(8.dp))
                                    Text(Localization.get(language, "shuffle_all"))
                                }
                            }
                        }
                    },
                )
                Spacer(Modifier.height(16.dp))
                // An account playlist can be genuinely empty (an auto playlist
                // like "Episodes for Later" before anything is saved to it): the
                // header then says so in the API's own words and the screen used
                // to draw nothing under two buttons that could do nothing. Say it
                // once and keep the buttons out of the way (same wording the
                // local playlist screen uses).
                if (playlistSongs.isEmpty()) {
                    Text(
                        Localization.get(language, "empty_playlist"),
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(top = 8.dp),
                    )
                } else {
                    LazyColumn(Modifier.weight(1f).fillMaxWidth()) {
                        items(page!!.songs, key = { it.id }, contentType = { "song" }) { song ->
                            SongRow(song, language, { onPlaySong(song) }, onAddToQueue = { onAddToQueue(song) }, onAddToPlaylist = { onAddToPlaylist(song) })
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun HistoryScreen(
    language: String,
    onBack: () -> Unit,
    onPlaySong: (SongItem) -> Unit,
    onAddToQueue: (SongItem) -> Unit,
    onAddToPlaylist: (SongItem) -> Unit,
    /** Re-runs one of the remembered searches. */
    onSearch: (String) -> Unit = {},
) {
    var page by remember { mutableStateOf<HistoryPage?>(null) }
    var error by remember { mutableStateOf<String?>(null) }
    // Local history: what was played and searched on this machine, across
    // restarts. The server list below only covers the signed-in account, which
    // is why an unsigned or offline user saw an empty screen forever.
    val localTracks by HistoryStore.tracks.collectAsState()
    val localSearches by HistoryStore.searches.collectAsState()

    LaunchedEffect(Unit) {
        YouTube.musicHistory().fold(
            onSuccess = { page = it },
            onFailure = { error = it.message },
        )
    }

    // This screen and the notifications list are two different screens with two
    // different empty states (they shared one key until 1.53.13, which is how
    // opening the history could look like opening the notifications). What the
    // history is actually made of is recorded, so a support zip can tell "the
    // history was empty" apart from "the history was not read".
    LaunchedEffect(localTracks.size, localSearches.size, page, error) {
        AppLog.log(
            "history",
            "listening history: ${localTracks.size} track(s), ${localSearches.size} search(es); " +
                "account sections=${page?.sections?.size ?: -1}" +
                (error?.let { " — $it" } ?: ""),
        )
    }

    Column(Modifier.fillMaxSize().background(BitColors.page()).padding(start = 16.dp, end = 18.dp, top = 18.dp)) {
        Text(
            Localization.get(language, "history"),
            style = MaterialTheme.typography.headlineMedium.copy(fontSize = 30.sp, fontWeight = FontWeight.ExtraBold),
            color = if (isAppInDarkTheme()) Color.White else MaterialTheme.colorScheme.onBackground,
        )
        LazyColumn(Modifier.weight(1f).fillMaxWidth()) {
            if (localSearches.isNotEmpty()) {
                item(key = "header-local-searches") {
                    HistorySectionHeader(
                        title = Localization.get(language, "search_history"),
                        clearLabel = Localization.get(language, "clear_search_history"),
                        onClear = { HistoryStore.clearSearches() },
                    )
                }
                item(key = "local-searches") {
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        localSearches.take(20).forEach { entry ->
                            FilterChip(
                                selected = false,
                                onClick = { onSearch(entry.term) },
                                label = { Text(entry.term) },
                            )
                        }
                    }
                }
            }

            if (localTracks.isNotEmpty()) {
                item(key = "header-local-tracks") {
                    HistorySectionHeader(
                        title = Localization.get(language, "recently_played"),
                        clearLabel = Localization.get(language, "clear_history"),
                        onClear = { HistoryStore.clearTracks() },
                    )
                }
                items(localTracks.take(50), key = { "local-${it.videoId}" }, contentType = { "song" }) { entry ->
                    val song = entry.toSongItem()
                    SongRow(
                        song,
                        language,
                        { onPlaySong(song) },
                        onAddToQueue = { onAddToQueue(song) },
                        onAddToPlaylist = { onAddToPlaylist(song) },
                        // No length on a history row: the local rows know it and
                        // the account's rows do not, so drawing it showed a
                        // duration on some rows and nothing on the others.
                        showDuration = true,
                    )
                }
            }

            val err = error
            val loaded = page
            when {
                err != null -> item(key = "server-error") { ErrorBox(language, err) }
                loaded == null -> item(key = "server-loading") { LoadingBox(language) }
                loaded.sections.orEmpty().isEmpty() -> if (localTracks.isEmpty()) {
                    item(key = "server-empty") {
                        Text(
                            Localization.get(language, "history_empty"),
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(top = 16.dp),
                        )
                    }
                }
                else -> loaded.sections.orEmpty().forEach { section ->
                    item(key = "header-${section.title}") {
                        Text(
                            section.title,
                            style = MaterialTheme.typography.titleLarge,
                            modifier = Modifier.padding(top = 12.dp, bottom = 8.dp),
                        )
                    }
                    items(section.songs, key = { "song-${it.id}" }, contentType = { "song" }) { song ->
                        SongRow(
                            song,
                            language,
                            { onPlaySong(song) },
                            onAddToQueue = { onAddToQueue(song) },
                            onAddToPlaylist = { onAddToPlaylist(song) },
                            showDuration = false,
                        )
                    }
                }
            }
        }
    }
}

/** Section title of the local history lists, with its "clear" action. */
@Composable
private fun HistorySectionHeader(title: String, clearLabel: String, onClear: () -> Unit) {
    Row(
        Modifier.fillMaxWidth().padding(top = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            title,
            style = MaterialTheme.typography.titleLarge.copy(fontSize = 22.sp, fontWeight = FontWeight.SemiBold),
            color = if (isAppInDarkTheme()) Color.White else MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.weight(1f),
        )
        TextButton(onClick = onClear) { Text(clearLabel) }
    }
}

/**
 * Rebuilds a playable [SongItem] from a stored history row. The album id is
 * not kept (only its name), so the entry plays as a standalone song: that is
 * the same thing the row would do when tapped from the server history.
 */
private fun HistoryEntry.toSongItem(): SongItem = SongItem(
    id = videoId,
    title = title,
    artists = artist.split(",").map { it.trim() }.filter { it.isNotEmpty() }.map { Artist(name = it, id = null) },
    duration = if (durationMs > 0) (durationMs / 1000).toInt() else null,
    thumbnail = thumbnail?.takeIf { it.isNotBlank() } ?: "https://i.ytimg.com/vi/$videoId/hqdefault.jpg",
)
