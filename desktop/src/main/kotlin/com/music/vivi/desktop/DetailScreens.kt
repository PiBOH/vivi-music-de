package com.music.vivi.desktop

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
import androidx.compose.foundation.lazy.LazyRow
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
import androidx.compose.material3.OutlinedButton
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
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
fun AlbumScreen(
    browseId: String,
    language: String,
    onBack: () -> Unit,
    onOpenArtist: (String) -> Unit,
    onPlaySong: (SongItem) -> Unit,
    onAddToQueue: (SongItem) -> Unit,
    onAddToPlaylist: (SongItem) -> Unit,
    onPlayAll: (List<SongItem>) -> Unit,
    onShuffleAll: (List<SongItem>) -> Unit,
    /** Header menu: queue the whole album right after the current track. */
    onPlayNext: (List<SongItem>) -> Unit = {},
    /** Header menu: append the whole album to the queue. */
    onAddAllToQueue: (List<SongItem>) -> Unit = {},
) {
    var page by remember { mutableStateOf<AlbumPage?>(null) }
    var error by remember { mutableStateOf<String?>(null) }
    // Bumped by the header menu's "Refresh": re-runs the load below.
    var refreshKey by remember { mutableStateOf(0) }

    LaunchedEffect(browseId, refreshKey) {
        YouTube.album(browseId).fold(
            onSuccess = { page = it.filteredContent() },
            onFailure = { error = it.message },
        )
    }

    Column(Modifier.fillMaxSize().padding(16.dp)) {
        BackButton(language, onBack)
        when {
            error != null -> ErrorBox(language, error)
            page == null -> LoadingBox(language)
            else -> {
                val album = page!!.album
                val albumSongs = page!!.songs
                val scope = rememberCoroutineScope()
                val albumLiked = DetailActions.isCollectionLiked(album.playlistId)
                GradientHeader(
                    title = album.title,
                    subtitle = album.artists?.joinToString(", ") { it.name }.orEmpty().ifBlank { null },
                    meta = album.year?.toString(),
                    thumbnailUrl = album.thumbnail,
                    language = language,
                    menuEntries = listOfNotNull(
                        DetailMenuEntry(Localization.get(language, "play_all"), Icons.Filled.PlayArrow) {
                            onPlayAll(albumSongs)
                        },
                        DetailMenuEntry(Localization.get(language, "shuffle_all"), Icons.Filled.Shuffle) {
                            onShuffleAll(albumSongs)
                        },
                        DetailMenuEntry(Localization.get(language, "play_next"), Icons.AutoMirrored.Filled.PlaylistPlay) {
                            onPlayNext(albumSongs)
                        },
                        DetailMenuEntry(Localization.get(language, "add_to_queue"), Icons.AutoMirrored.Filled.QueueMusic) {
                            onAddAllToQueue(albumSongs)
                        },
                        DetailMenuEntry(Localization.get(language, "add_to_playlist"), Icons.AutoMirrored.Filled.PlaylistAdd) {
                            albumSongs.firstOrNull()?.let(onAddToPlaylist)
                        },
                        album.artists?.firstOrNull { it.id != null }?.let { artist ->
                            DetailMenuEntry(Localization.get(language, "view_artist"), Icons.Filled.Person) {
                                artist.id?.let(onOpenArtist)
                            }
                        },
                        DetailMenuEntry(
                            Localization.get(language, if (albumLiked) "unlike" else "like"),
                            if (albumLiked) Icons.Filled.Favorite else Icons.Filled.FavoriteBorder,
                        ) {
                            val next = DetailActions.toggleCollectionLiked(album.playlistId)
                            scope.launch { YouTube.likePlaylist(album.playlistId, next) }
                        },
                        DetailMenuEntry(Localization.get(language, "share"), Icons.Filled.Share) {
                            copyToClipboard(album.shareLink)
                            DesktopSnackbar.show(Localization.get(language, "copied_to_clipboard"))
                        },
                        DetailMenuEntry(Localization.get(language, "refresh"), Icons.Filled.Refresh) {
                            refreshKey++
                        },
                    ),
                )
                Spacer(Modifier.height(16.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(Localization.get(language, "songs"), style = MaterialTheme.typography.titleLarge)
                    Spacer(Modifier.weight(1f))
                    Button(onClick = { onPlayAll(page!!.songs) }) { Text(Localization.get(language, "play_all")) }
                    Spacer(Modifier.width(8.dp))
                    OutlinedButton(onClick = { onShuffleAll(page!!.songs) }) {
                        Icon(Icons.Filled.Shuffle, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(8.dp))
                        Text(Localization.get(language, "shuffle_all"))
                    }
                }
                LazyColumn(Modifier.weight(1f).fillMaxWidth()) {
                    items(page!!.songs, key = { it.id }) { song ->
                        SongRow(song, language, { onPlaySong(song) }, onAddToQueue = { onAddToQueue(song) }, onAddToPlaylist = { onAddToPlaylist(song) })
                    }
                }
            }
        }
    }
}

@Composable
fun ArtistScreen(
    browseId: String,
    language: String,
    onBack: () -> Unit,
    onOpenAlbum: (String) -> Unit,
    onOpenArtist: (String) -> Unit,
    onOpenPlaylist: (String) -> Unit,
    onPlaySong: (SongItem) -> Unit,
    onAddToQueue: (SongItem) -> Unit,
    onAddToPlaylist: (SongItem) -> Unit,
    /** Header menu: play / shuffle the artist's songs. */
    onPlayAll: (List<SongItem>) -> Unit = {},
    onShuffleAll: (List<SongItem>) -> Unit = {},
    /** Header menu: queue the artist's songs after the current track. */
    onPlayNext: (List<SongItem>) -> Unit = {},
    /** Header menu: append the artist's songs to the queue. */
    onAddAllToQueue: (List<SongItem>) -> Unit = {},
) {
    var page by remember { mutableStateOf<ArtistPage?>(null) }
    var error by remember { mutableStateOf<String?>(null) }
    var tab by remember { mutableStateOf(0) } // 0 = Songs, 1 = Albums, 2 = Items
    // Bumped by the header menu's "Refresh": re-runs the load below.
    var refreshKey by remember { mutableStateOf(0) }
    // Content screen options: both rows are mobile ones (the artist page shows
    // the description and the subscriber count unless they are switched off).
    val showDescription = remember(settingsFileRevision()) { DesktopSettings.load().showArtistDescription }
    val showSubscribers = remember(settingsFileRevision()) { DesktopSettings.load().showArtistSubscriberCount }
    var itemsPage by remember { mutableStateOf<ArtistItemsPage?>(null) }
    var itemsEndpoint by remember { mutableStateOf<BrowseEndpoint?>(null) }

    LaunchedEffect(browseId, refreshKey) {
        YouTube.artist(browseId).fold(
            onSuccess = { p ->
                page = p.filteredContent()
                // Prefer the first section with a "See all" endpoint for the Items tab.
                itemsEndpoint = p.sections.firstNotNullOfOrNull { it.moreEndpoint }
            },
            onFailure = { error = it.message },
        )
    }

    LaunchedEffect(tab, itemsEndpoint) {
        if (tab == 2 && itemsEndpoint != null) {
            YouTube.artistItems(itemsEndpoint!!).fold(
                onSuccess = { itemsPage = it.filteredContent() },
                onFailure = { itemsPage = null },
            )
        }
    }

    Column(Modifier.fillMaxSize().padding(16.dp)) {
        BackButton(language, onBack)
        when {
            error != null -> ErrorBox(language, error)
            page == null -> LoadingBox(language)
            else -> {
                val artist = page!!.artist
                val artistSongs = page!!.sections
                    .flatMap { it.items.filterIsInstance<SongItem>() }
                    .distinctBy { it.id }
                val scope = rememberCoroutineScope()
                val subscribed = DetailActions.isArtistSubscribed(artist.id)
                GradientHeader(
                    title = artist.title,
                    subtitle = if (showSubscribers) page!!.subscriberCountText else null,
                    thumbnailUrl = artist.thumbnail,
                    language = language,
                    menuEntries = listOfNotNull(
                        if (artistSongs.isNotEmpty()) {
                            DetailMenuEntry(Localization.get(language, "play"), Icons.Filled.PlayArrow) {
                                onPlayAll(artistSongs)
                            }
                        } else null,
                        if (artistSongs.isNotEmpty()) {
                            DetailMenuEntry(Localization.get(language, "shuffle"), Icons.Filled.Shuffle) {
                                onShuffleAll(artistSongs)
                            }
                        } else null,
                        if (artistSongs.isNotEmpty()) {
                            DetailMenuEntry(Localization.get(language, "play_next"), Icons.AutoMirrored.Filled.PlaylistPlay) {
                                onPlayNext(artistSongs)
                            }
                        } else null,
                        if (artistSongs.isNotEmpty()) {
                            DetailMenuEntry(Localization.get(language, "add_to_queue"), Icons.AutoMirrored.Filled.QueueMusic) {
                                onAddAllToQueue(artistSongs)
                            }
                        } else null,
                        if (artistSongs.isNotEmpty()) {
                            DetailMenuEntry(Localization.get(language, "add_to_playlist"), Icons.AutoMirrored.Filled.PlaylistAdd) {
                                artistSongs.firstOrNull()?.let(onAddToPlaylist)
                            }
                        } else null,
                        DetailMenuEntry(
                            Localization.get(language, if (subscribed) "subscribed" else "subscribe"),
                            if (subscribed) Icons.Filled.Favorite else Icons.Filled.FavoriteBorder,
                        ) {
                            val next = DetailActions.toggleArtistSubscribed(artist.id)
                            scope.launch { YouTube.subscribeChannel(artist.id, next) }
                        },
                        DetailMenuEntry(Localization.get(language, "share"), Icons.Filled.Share) {
                            copyToClipboard(artist.shareLink)
                            DesktopSnackbar.show(Localization.get(language, "copied_to_clipboard"))
                        },
                        DetailMenuEntry(Localization.get(language, "refresh"), Icons.Filled.Refresh) {
                            refreshKey++
                        },
                    ),
                )
                if (showDescription) {
                    page!!.description?.takeIf { it.isNotBlank() }?.let { description ->
                        Text(
                            text = description,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 4,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                        )
                    }
                }
                Spacer(Modifier.height(12.dp))

                val tabs = listOf(Localization.get(language, "songs"), Localization.get(language, "albums"), Localization.get(language, "items"))
                TabRow(selectedTabIndex = tab) {
                    tabs.forEachIndexed { i, title ->
                        Tab(selected = tab == i, onClick = { tab = i }, text = { Text(title) })
                    }
                }
                Spacer(Modifier.height(8.dp))

                if (tab == 2) {
                    val items = itemsPage?.items.orEmpty()
                    if (itemsEndpoint == null) {
                        // This artist page has no "see all" section at all: it is
                        // not loading, there is nothing to load.
                        EmptyBox(language)
                    } else if (items.isEmpty()) {
                        LoadingBox(language)
                    } else if (items.all { it is SongItem }) {
                        LazyColumn(Modifier.weight(1f).fillMaxWidth()) {
                            items(items.filterIsInstance<SongItem>(), key = { it.id }) { song ->
                                SongRow(song, language, { onPlaySong(song) }, onAddToQueue = { onAddToQueue(song) }, onAddToPlaylist = { onAddToPlaylist(song) })
                            }
                        }
                    } else {
                        LazyColumn(Modifier.weight(1f).fillMaxWidth()) {
                            items(items, key = { it.id }) { item ->
                                YtItemCard(item = item, width = null, onClick = { onItemClick(item, onOpenAlbum, onOpenArtist, onOpenPlaylist, onPlaySong) })
                            }
                        }
                    }
                } else if (page!!.sections.none { section ->
                        when (tab) {
                            0 -> section.items.any { it is SongItem }
                            else -> section.items.isNotEmpty()
                        }
                    }
                ) {
                    // The artist loaded, but this tab has nothing to show (a
                    // page that came back in a shape the parser does not know
                    // has no sections at all): say so instead of drawing a
                    // blank area that looks like a broken screen.
                    EmptyBox(language)
                } else {
                    LazyColumn(Modifier.weight(1f).fillMaxWidth()) {
                        page!!.sections.forEach { section ->
                            val songs = section.items.filterIsInstance<SongItem>()
                            val albums = section.items.filterIsInstance<AlbumItem>()
                            val others = section.items.filterNot { it is SongItem || it is AlbumItem }
                            val visible = when (tab) {
                                0 -> songs
                                1 -> albums
                                else -> emptyList()
                            }
                            val mixed = tab == 1 && (albums.isNotEmpty() || others.isNotEmpty())

                            if (visible.isNotEmpty()) {
                                item(key = "header-${tab}-${section.title}") {
                                    Text(section.title, style = MaterialTheme.typography.titleLarge, modifier = Modifier.padding(top = 16.dp, bottom = 8.dp))
                                }
                                if (tab == 0) {
                                    items(visible.filterIsInstance<SongItem>().distinctBy { it.id }, key = { "song-${it.id}" }) { song ->
                                        SongRow(song, language, { onPlaySong(song) }, onAddToQueue = { onAddToQueue(song) }, onAddToPlaylist = { onAddToPlaylist(song) })
                                    }
                                } else {
                                    item(key = "grid-${tab}-${section.title}") {
                                        LazyRow(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                                            items((albums + others).distinctBy { it.id }, key = { it.id }) { item ->
                                                YtItemCard(item = item, onClick = { onItemClick(item, onOpenAlbum, onOpenArtist, onOpenPlaylist, onPlaySong) })
                                            }
                                        }
                                    }
                                }
                            } else if (mixed && albums.isNotEmpty()) {
                                item(key = "header-${tab}-${section.title}") {
                                    Text(section.title, style = MaterialTheme.typography.titleLarge, modifier = Modifier.padding(top = 16.dp, bottom = 8.dp))
                                }
                                item(key = "grid-${tab}-${section.title}") {
                                    LazyRow(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                                        items((albums + others).distinctBy { it.id }, key = { it.id }) { item ->
                                            YtItemCard(item = item, onClick = { onItemClick(item, onOpenAlbum, onOpenArtist, onOpenPlaylist, onPlaySong) })
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

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
        BackButton(language, onBack)
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
                        DetailMenuEntry(Localization.get(language, "play_all"), Icons.Filled.PlayArrow) {
                            onPlayAll(playlistSongs)
                        },
                        DetailMenuEntry(Localization.get(language, "shuffle_all"), Icons.Filled.Shuffle) {
                            onShuffleAll(playlistSongs)
                        },
                        DetailMenuEntry(Localization.get(language, "play_next"), Icons.AutoMirrored.Filled.PlaylistPlay) {
                            onPlayNext(playlistSongs)
                        },
                        DetailMenuEntry(Localization.get(language, "add_to_queue"), Icons.AutoMirrored.Filled.QueueMusic) {
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
                )
                Spacer(Modifier.height(16.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Button(onClick = { onPlayAll(page!!.songs) }) { Text(Localization.get(language, "play_all")) }
                    Spacer(Modifier.width(8.dp))
                    OutlinedButton(onClick = { onShuffleAll(page!!.songs) }) {
                        Icon(Icons.Filled.Shuffle, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(8.dp))
                        Text(Localization.get(language, "shuffle_all"))
                    }
                }
                Spacer(Modifier.height(8.dp))
                LazyColumn(Modifier.weight(1f).fillMaxWidth()) {
                    items(page!!.songs, key = { it.id }) { song ->
                        SongRow(song, language, { onPlaySong(song) }, onAddToQueue = { onAddToQueue(song) }, onAddToPlaylist = { onAddToPlaylist(song) })
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

    Column(Modifier.fillMaxSize().padding(16.dp)) {
        BackButton(language, onBack)
        Text(Localization.get(language, "history"), style = MaterialTheme.typography.headlineMedium)
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
                items(localTracks.take(50), key = { "local-${it.videoId}" }) { entry ->
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
                        showDuration = false,
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
                    items(section.songs, key = { "song-${it.id}" }) { song ->
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
            style = MaterialTheme.typography.titleLarge,
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
