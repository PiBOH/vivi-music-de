package com.music.vivi.desktop

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Shuffle
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import com.music.innertube.YouTube
import com.music.innertube.models.AlbumItem
import com.music.innertube.models.ArtistItem
import com.music.innertube.models.PlaylistItem
import com.music.innertube.models.SongItem
import com.music.innertube.models.YTItem
import com.music.innertube.pages.SearchSummary
import com.music.innertube.pages.SearchSummaryPage
import com.music.vivi.sync.SyncedPlaylist
import com.music.vivi.sync.SyncedSong
import kotlinx.coroutines.launch

/**
 * BitChord's playlist screen, on this machine's own playlists.
 *
 * The header is the reference's: the 2x2 mosaic built from the first four
 * covers the playlist actually holds, the small PLAYLIST label, the name, the
 * real track count, and the row of round controls (shuffle, the filled play,
 * and the menu). The table under it is the real order stored by
 * [PlaylistStore]: a click plays the track, the heart is the same like the rest
 * of the app writes, and the menu renames or deletes the playlist for real.
 *
 * Two entries of the reference's menu are not here: there is no download store
 * on the desktop ("Download all" would do nothing) and a local playlist has no
 * share link to copy.
 */
@Composable
fun BitChordLocalPlaylistScreen(
    playlistId: String,
    language: String,
    onBack: () -> Unit,
    onPlay: (SyncedSong) -> Unit,
    onPlayAll: (List<SyncedSong>) -> Unit,
    onAddToQueue: (SyncedSong) -> Unit = {},
) {
    val playlists by PlaylistStore.all.collectAsState()
    val playlist = playlists.firstOrNull { it.id == playlistId && !it.deleted }
    var menuOpen by remember { mutableStateOf(false) }
    var renaming by remember { mutableStateOf(false) }
    var nameDraft by remember { mutableStateOf("") }
    var confirmDelete by remember { mutableStateOf(false) }

    Column(Modifier.fillMaxSize().padding(16.dp)) {
        BackButton(language, onBack)
        if (playlist == null) {
            Text(
                Localization.get(language, "playlist_not_found"),
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 16.dp),
            )
            return@Column
        }

        Row(Modifier.fillMaxWidth().padding(top = 8.dp)) {
            PlaylistMosaic(playlist)
            Spacer(Modifier.width(24.dp))
            Column(Modifier.weight(1f)) {
                Text(
                    Localization.get(language, "playlist").uppercase(),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Text(
                    playlist.name.ifBlank { Localization.get(language, "playlist") },
                    style = MaterialTheme.typography.displaySmall.copy(fontWeight = FontWeight.Bold),
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.padding(top = 4.dp),
                )
                Text(
                    "${playlist.songs.size} " + Localization.get(language, "songs").lowercase(),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 6.dp),
                )
                Row(
                    Modifier.padding(top = 16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    FilledIconButton(
                        onClick = { onPlayAll(playlist.songs.shuffled()) },
                        enabled = playlist.songs.isNotEmpty(),
                        shape = CircleShape,
                        colors = IconButtonDefaults.filledIconButtonColors(
                            containerColor = MaterialTheme.colorScheme.surfaceContainerHighest,
                            contentColor = MaterialTheme.colorScheme.onSurface,
                        ),
                    ) { Icon(Icons.Filled.Shuffle, contentDescription = Localization.get(language, "shuffle_all")) }
                    FilledIconButton(
                        onClick = { onPlayAll(playlist.songs) },
                        enabled = playlist.songs.isNotEmpty(),
                        shape = CircleShape,
                        modifier = Modifier.size(56.dp),
                    ) { Icon(Icons.Filled.PlayArrow, contentDescription = Localization.get(language, "play_all"), modifier = Modifier.size(28.dp)) }
                    Box {
                        FilledIconButton(
                            onClick = { menuOpen = true },
                            shape = CircleShape,
                            colors = IconButtonDefaults.filledIconButtonColors(
                                containerColor = MaterialTheme.colorScheme.surfaceContainerHighest,
                                contentColor = MaterialTheme.colorScheme.onSurface,
                            ),
                        ) { Icon(Icons.Filled.MoreVert, contentDescription = Localization.get(language, "more")) }
                        DropdownMenu(expanded = menuOpen, onDismissRequest = { menuOpen = false }) {
                            DropdownMenuItem(
                                text = { Text(Localization.get(language, "rename")) },
                                leadingIcon = { Icon(Icons.Filled.Edit, contentDescription = null) },
                                onClick = {
                                    menuOpen = false
                                    nameDraft = playlist.name
                                    renaming = true
                                },
                            )
                            DropdownMenuItem(
                                text = { Text(Localization.get(language, "delete")) },
                                leadingIcon = { Icon(Icons.Filled.Delete, contentDescription = null) },
                                onClick = {
                                    menuOpen = false
                                    confirmDelete = true
                                },
                            )
                        }
                    }
                }
            }
        }

        Spacer(Modifier.height(18.dp))
        if (playlist.songs.isEmpty()) {
            Text(
                Localization.get(language, "empty_playlist"),
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        } else {
            LazyColumn(Modifier.weight(1f).fillMaxWidth()) {
                itemsIndexed(playlist.songs, key = { i, s -> s.id + "#" + i }) { index, song ->
                    PlaylistTrackRow(
                        position = index + 1,
                        song = song,
                        language = language,
                        onPlay = { onPlay(song) },
                        onAddToQueue = { onAddToQueue(song) },
                    )
                }
            }
        }
    }

    if (renaming) {
        AlertDialog(
            onDismissRequest = { renaming = false },
            title = { Text(Localization.get(language, "rename")) },
            text = {
                OutlinedTextField(
                    value = nameDraft,
                    onValueChange = { nameDraft = it },
                    singleLine = true,
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        PlaylistStore.rename(playlistId, nameDraft.trim())
                        renaming = false
                    },
                ) { Text(Localization.get(language, "save")) }
            },
            dismissButton = {
                TextButton(onClick = { renaming = false }) { Text(Localization.get(language, "cancel")) }
            },
        )
    }
    if (confirmDelete) {
        AlertDialog(
            onDismissRequest = { confirmDelete = false },
            title = { Text(Localization.get(language, "delete_playlist")) },
            text = { Text(Localization.get(language, "delete_playlist_confirm")) },
            confirmButton = {
                TextButton(
                    onClick = {
                        PlaylistStore.delete(playlistId)
                        confirmDelete = false
                        onBack()
                    },
                ) { Text(Localization.get(language, "delete")) }
            },
            dismissButton = {
                TextButton(onClick = { confirmDelete = false }) { Text(Localization.get(language, "cancel")) }
            },
        )
    }
}

/** The 2x2 cover mosaic of the reference, from the playlist's own songs. */
@Composable
private fun PlaylistMosaic(playlist: SyncedPlaylist) {
    val covers = remember(playlist.songs) {
        playlist.songs.mapNotNull { it.thumbnail?.takeIf { url -> url.isNotBlank() } }.distinct().take(4)
    }
    val tile = MaterialTheme.colorScheme.surfaceContainerHigh
    Column(Modifier.size(196.dp).clip(MaterialTheme.shapes.large)) {
        Row(Modifier.weight(1f).fillMaxWidth()) {
            MosaicCell(covers.getOrNull(0), tile)
            MosaicCell(covers.getOrNull(1), tile)
        }
        Row(Modifier.weight(1f).fillMaxWidth()) {
            MosaicCell(covers.getOrNull(2), tile)
            MosaicCell(covers.getOrNull(3), tile)
        }
    }
}

@Composable
private fun androidx.compose.foundation.layout.RowScope.MosaicCell(url: String?, fallback: androidx.compose.ui.graphics.Color) {
    Box(Modifier.weight(1f).fillMaxSize().padding(1.dp).background(fallback)) {
        if (url != null) {
            AsyncImage(model = url, contentDescription = null, modifier = Modifier.fillMaxSize())
        }
    }
}

/** One row of the reference's table: number, cover, title, artist, like, add. */
@Composable
private fun PlaylistTrackRow(
    position: Int,
    song: SyncedSong,
    language: String,
    onPlay: () -> Unit,
    onAddToQueue: () -> Unit,
) {
    var liked by remember(song.id) { mutableStateOf(SongActions.isLiked(song.id)) }
    val scope = rememberCoroutineScope()
    Row(
        Modifier.fillMaxWidth().clickable { onPlay() }.padding(horizontal = 6.dp, vertical = 7.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            position.toString(),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.width(28.dp),
        )
        AsyncImage(
            model = song.thumbnail,
            contentDescription = null,
            modifier = Modifier.size(40.dp).clip(MaterialTheme.shapes.small),
        )
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(song.title, style = MaterialTheme.typography.bodyLarge, maxLines = 1, overflow = TextOverflow.Ellipsis)
            if (song.artist.isNotBlank()) {
                Text(
                    song.artist,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
        M3IconButton(
            onClick = {
                val next = !liked
                liked = next
                SongActions.setLiked(
                    song.id,
                    next,
                    title = song.title,
                    artist = song.artist,
                    thumbnail = song.thumbnail,
                )
                scope.launch { YouTube.likeVideo(song.id, next) }
            },
        ) {
            Icon(
                if (liked) Icons.Filled.Favorite else Icons.Filled.FavoriteBorder,
                contentDescription = Localization.get(language, if (liked) "unlike" else "like"),
                tint = if (liked) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(18.dp),
            )
        }
        M3IconButton(onClick = onAddToQueue) {
            Icon(
                Icons.Filled.Add,
                contentDescription = Localization.get(language, "add_to_queue"),
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(18.dp),
            )
        }
    }
}

/**
 * BitChord's search: one large field, the service's own suggestions under it
 * while typing, then a flat list of results with the cover, the title and the
 * line that says what the row is (Album - artist - year, Artist - subscribers,
 * Playlist - author - count, or the song's artists).
 *
 * Everything comes from the endpoints the screen already used
 * ([YouTube.searchSummary], [YouTube.search] for a filter and
 * [YouTube.searchSuggestions]); nothing is fabricated for an empty query, which
 * keeps the recent searches and the plain prompt instead.
 */
@Composable
fun BitChordSearchScreen(
    language: String,
    onOpenAlbum: (String) -> Unit,
    onOpenArtist: (String) -> Unit,
    onOpenPlaylist: (String) -> Unit,
    onPlaySong: (SongItem) -> Unit,
    searchHistory: List<String> = emptyList(),
    onRecordSearch: (String) -> Unit = {},
    onClearSearchHistory: () -> Unit = {},
    externalQuery: String? = null,
    onQueryChange: ((String) -> Unit)? = null,
    externalFilter: YouTube.SearchFilter? = null,
    onFilterChange: ((YouTube.SearchFilter?) -> Unit)? = null,
) {
    var internalQuery by remember { mutableStateOf("") }
    val query = externalQuery ?: internalQuery
    val updateQuery: (String) -> Unit = { newQ ->
        onQueryChange?.invoke(newQ)
        if (externalQuery == null) internalQuery = newQ
    }
    var page by remember { mutableStateOf<SearchSummaryPage?>(null) }
    var filterItems by remember { mutableStateOf<List<YTItem>?>(null) }
    var internalFilter by remember { mutableStateOf<YouTube.SearchFilter?>(null) }
    val selectedFilter = externalFilter ?: internalFilter
    val setSelectedFilter: (YouTube.SearchFilter?) -> Unit = { f ->
        onFilterChange?.invoke(f)
        if (externalFilter == null) internalFilter = f
    }
    var suggestions by remember { mutableStateOf<List<String>>(emptyList()) }
    var focused by remember { mutableStateOf(false) }
    var loading by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    val clipboard = LocalClipboardManager.current

    LaunchedEffect(query, selectedFilter) {
        val q = query.trim()
        if (q.isEmpty()) {
            page = null
            filterItems = null
            error = null
            loading = false
            return@LaunchedEffect
        }
        loading = true
        val filter = selectedFilter
        if (filter == null) {
            YouTube.searchSummary(q).fold(
                onSuccess = { page = it; filterItems = null; error = null },
                onFailure = { error = it.message },
            )
        } else {
            YouTube.search(q, filter).fold(
                onSuccess = { page = null; filterItems = it.items.distinctBy { i -> i.id }; error = null },
                onFailure = { error = it.message },
            )
        }
        loading = false
    }

    LaunchedEffect(query, focused) {
        val q = query.trim()
        if (!focused || q.isEmpty() || page != null || filterItems != null) {
            suggestions = emptyList()
            return@LaunchedEffect
        }
        suggestions = YouTube.searchSuggestions(q).getOrNull()?.queries.orEmpty().take(6)
    }

    Column(Modifier.fillMaxSize().padding(16.dp)) {
        OutlinedTextField(
            value = query,
            onValueChange = updateQuery,
            modifier = Modifier.fillMaxWidth().onFocusChanged { focused = it.isFocused },
            singleLine = true,
            shape = MaterialTheme.shapes.extraLarge,
            leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null) },
            trailingIcon = {
                if (query.isNotBlank()) {
                    M3IconButton(onClick = { updateQuery("") }) {
                        Icon(Icons.Filled.Close, contentDescription = Localization.get(language, "clear"))
                    }
                }
            },
            placeholder = { Text(Localization.get(language, "search_placeholder")) },
        )

        val filters = listOf(
            null to Localization.get(language, "filter_all"),
            YouTube.SearchFilter.FILTER_SONG to Localization.get(language, "filter_songs"),
            YouTube.SearchFilter.FILTER_ALBUM to Localization.get(language, "filter_albums"),
            YouTube.SearchFilter.FILTER_ARTIST to Localization.get(language, "filter_artists"),
            YouTube.SearchFilter.FILTER_FEATURED_PLAYLIST to Localization.get(language, "filter_playlists"),
        )
        Row(
            Modifier.fillMaxWidth().padding(top = 10.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            filters.forEach { (filter, label) ->
                FilterChip(
                    selected = selectedFilter == filter,
                    onClick = { setSelectedFilter(if (selectedFilter == filter) null else filter) },
                    label = { Text(label) },
                )
            }
        }

        // The service's own suggestions, while the field has the focus and no
        // results are on screen yet.
        if (focused && suggestions.isNotEmpty() && page == null && filterItems == null) {
            Column(Modifier.fillMaxWidth().padding(top = 8.dp)) {
                suggestions.forEach { s ->
                    Row(
                        Modifier.fillMaxWidth()
                            .clickable {
                                updateQuery(s)
                                focused = false
                                onRecordSearch(s)
                            }
                            .padding(horizontal = 8.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Icon(
                            Icons.Filled.Search,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(18.dp),
                        )
                        Spacer(Modifier.width(14.dp))
                        Text(s, style = MaterialTheme.typography.bodyLarge)
                    }
                }
            }
        }

        when {
            error != null -> ErrorBox(language, error)
            loading && page == null && filterItems == null -> LoadingBox(language)
            page == null && filterItems == null -> {
                if (searchHistory.isNotEmpty()) {
                    Row(
                        Modifier.fillMaxWidth().padding(top = 16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            Localization.get(language, "search_history"),
                            style = MaterialTheme.typography.titleSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        Spacer(Modifier.weight(1f))
                        TextButton(onClick = onClearSearchHistory) {
                            Text(Localization.get(language, "clear_search_history"))
                        }
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        searchHistory.take(8).forEach { term ->
                            FilterChip(
                                selected = false,
                                onClick = { updateQuery(term); onRecordSearch(term) },
                                label = { Text(term) },
                            )
                        }
                    }
                }
            }
            filterItems != null -> LazyColumn(Modifier.fillMaxSize().padding(top = 8.dp)) {
                items(filterItems.orEmpty(), key = { it.id }) { item ->
                    SearchResultRow(item, language) {
                        openSearchResult(item, onOpenAlbum, onOpenArtist, onOpenPlaylist, onPlaySong)
                    }
                }
            }
            else -> {
                val rows = page!!.summaries.flatMap { summary: SearchSummary -> summary.items }
                    .distinctBy { it.id }
                if (rows.isEmpty()) {
                    Text(
                        Localization.get(language, "search_no_results"),
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(top = 20.dp),
                    )
                } else {
                    LazyColumn(Modifier.fillMaxSize().padding(top = 8.dp)) {
                        items(rows, key = { it.id }) { item ->
                            SearchResultRow(item, language) {
                                openSearchResult(item, onOpenAlbum, onOpenArtist, onOpenPlaylist, onPlaySong)
                            }
                        }
                    }
                }
            }
        }
    }
}

private fun openSearchResult(
    item: YTItem,
    onOpenAlbum: (String) -> Unit,
    onOpenArtist: (String) -> Unit,
    onOpenPlaylist: (String) -> Unit,
    onPlaySong: (SongItem) -> Unit,
) {
    when (item) {
        is SongItem -> onPlaySong(item)
        is AlbumItem -> onOpenAlbum(item.browseId)
        is ArtistItem -> onOpenArtist(item.id)
        is PlaylistItem -> onOpenPlaylist(item.id)
        else -> Unit
    }
}

/** The reference's result row: cover, title, and the line that says what it is. */
@Composable
private fun SearchResultRow(
    item: YTItem,
    language: String,
    onClick: () -> Unit,
) {
    val subtitle = when (item) {
        is SongItem -> item.artists.joinToString(", ") { it.name }
        is AlbumItem -> listOfNotNull(
            Localization.get(language, "album"),
            item.artists?.joinToString(", ") { it.name }?.takeIf { it.isNotBlank() },
            item.year?.toString(),
        ).joinToString(" - ")
        is ArtistItem -> listOfNotNull(
            Localization.get(language, "artist"),
            item.subtext?.takeIf { it.isNotBlank() },
        ).joinToString(" - ")
        is PlaylistItem -> listOfNotNull(
            Localization.get(language, "playlist"),
            item.author?.name?.takeIf { it.isNotBlank() },
            item.songCountText?.takeIf { it.isNotBlank() },
        ).joinToString(" - ")
        else -> ""
    }
    Row(
        Modifier.fillMaxWidth().clickable { onClick() }.padding(horizontal = 6.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        AsyncImage(
            model = item.thumbnail,
            contentDescription = null,
            modifier = Modifier.size(46.dp).clip(MaterialTheme.shapes.small),
        )
        Spacer(Modifier.width(14.dp))
        Column(Modifier.weight(1f)) {
            Text(
                item.title,
                style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.SemiBold),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            if (subtitle.isNotBlank()) {
                Text(
                    subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
    }
}
