package com.music.vivi.desktop

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
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
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items as gridItems
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Explore
import androidx.compose.material.icons.filled.Shuffle
import androidx.compose.material3.Button
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.foundation.background
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import coil3.compose.AsyncImage
import androidx.compose.ui.unit.dp
import com.music.innertube.YouTube
import com.music.innertube.models.Artist
import com.music.innertube.models.SongItem
import com.music.innertube.models.YTItem
import com.music.innertube.pages.LibraryPage
import com.music.innertube.pages.MoodAndGenres

/**
 * BitChord's Explore, on the sidebar entry that used to be "New".
 *
 * Nothing here is drawn for the look of it: the moods and genres are the ones
 * the service returns for `FEmusic_moods_and_genres` and each card is the real
 * browse page behind it, while the New releases row is `newReleaseAlbums()` and
 * each album opens its own page.
 *
 * Every mood tile carries a real picture, like the reference does. The moods
 * payload itself has no image (only a title, a stripe colour and the endpoint),
 * so the tile asks its own browse page for the first artwork it contains and
 * caches the result per endpoint; the stripe colour is the card background and
 * stays visible only while the picture is being resolved or when that page has
 * no artwork at all. The fetch happens per visible tile, not for the whole grid
 * at once.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun BitChordExploreScreen(
    language: String,
    onOpenBrowse: (String, String?) -> Unit,
    onOpenAlbum: (String) -> Unit,
) {
    var sections by remember { mutableStateOf<List<MoodAndGenres>?>(null) }
    var albums by remember { mutableStateOf<List<YTItem>?>(null) }
    var error by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(Unit) {
        YouTube.moodAndGenres().fold(
            onSuccess = { sections = it },
            onFailure = { error = it.message },
        )
        albums = YouTube.newReleaseAlbums().getOrNull()?.distinctBy { it.id }?.take(12)
    }

    val inks = listOf(
        MaterialTheme.colorScheme.onPrimaryContainer,
        MaterialTheme.colorScheme.onSecondaryContainer,
        MaterialTheme.colorScheme.onTertiaryContainer,
        MaterialTheme.colorScheme.onErrorContainer,
    )

    Column(Modifier.fillMaxSize().padding(16.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                Icons.Filled.Explore,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(26.dp),
            )
            Spacer(Modifier.width(10.dp))
            Text("Explore", style = MaterialTheme.typography.headlineMedium)
        }
        when {
            error != null -> ErrorBox(language, error)
            sections == null -> LoadingBox(language)
            else -> LazyColumn(Modifier.fillMaxSize().padding(top = 12.dp)) {
                if (!albums.isNullOrEmpty()) {
                    item(key = "new_releases_header") {
                        Text(
                            Localization.get(language, "new_release_albums"),
                            style = MaterialTheme.typography.titleLarge,
                            modifier = Modifier.padding(bottom = 10.dp),
                        )
                    }
                    item(key = "new_releases_grid") {
                        FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp),
                        ) {
                            albums.orEmpty().forEach { album ->
                                YtItemCard(
                                    item = album,
                                    width = 168.dp,
                                    onClick = { onOpenAlbum(album.id) },
                                )
                            }
                        }
                    }
                }
                sections.orEmpty().forEachIndexed { sectionIndex, section ->
                    item(key = "header-" + section.title) {
                        Text(
                            section.title,
                            style = MaterialTheme.typography.titleLarge,
                            modifier = Modifier.padding(
                                top = if (sectionIndex == 0 && albums.isNullOrEmpty()) 0.dp else 22.dp,
                                bottom = 10.dp,
                            ),
                        )
                    }
                    item(key = "grid-" + section.title) {
                        FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp),
                        ) {
                            section.items.forEachIndexed { index, item ->
                                MoodTile(
                                    item = item,
                                    ink = inks[index % inks.size],
                                    onClick = { onOpenBrowse(item.endpoint.browseId, item.endpoint.params) },
                                )
                            }
                        }
                    }
                }
                item(key = "explore_tail") { Spacer(Modifier.height(16.dp)) }
            }
        }
    }
}

/**
 * The artwork cache behind the mood tiles.
 *
 * One entry per endpoint, filled the first time a tile is composed and kept for
 * the session, so scrolling the grid back and forth does not ask the network
 * again. Only a resolved URL is stored: a tile whose page has no artwork keeps
 * its stripe colour and is not retried on every recomposition.
 */
private object MoodArtworkCache {
    private val resolved = androidx.compose.runtime.mutableStateMapOf<String, String>()
    private val inFlight = mutableSetOf<String>()

    private fun key(item: MoodAndGenres.Item): String =
        item.endpoint.browseId + "|" + (item.endpoint.params ?: "")

    fun cached(item: MoodAndGenres.Item): String? = resolved[key(item)]

    suspend fun resolve(item: MoodAndGenres.Item): String? {
        val k = key(item)
        resolved[k]?.let { return it }
        if (!inFlight.add(k)) return null
        return try {
            val url = YouTube.moodArtwork(item.endpoint.browseId, item.endpoint.params).getOrNull()
            if (url != null) resolved[k] = url
            url
        } finally {
            inFlight.remove(k)
        }
    }
}

/** One mood/genre card: the resolved artwork, a scrim that keeps the name readable, and the name. */
@Composable
private fun MoodTile(
    item: MoodAndGenres.Item,
    ink: Color,
    onClick: () -> Unit,
) {
    val stripe = Color(item.stripeColor)
    var artwork by remember(item) { mutableStateOf(MoodArtworkCache.cached(item)) }
    LaunchedEffect(item) {
        if (artwork == null) artwork = MoodArtworkCache.resolve(item)
    }
    val picture = artwork
    Surface(
        onClick = onClick,
        shape = MaterialTheme.shapes.large,
        color = stripe,
        modifier = Modifier.width(210.dp).height(118.dp),
    ) {
        Box(Modifier.fillMaxSize()) {
            if (!picture.isNullOrBlank()) {
                AsyncImage(
                    model = picture,
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.matchParentSize(),
                )
                Box(
                    Modifier.matchParentSize().background(
                        Brush.verticalGradient(
                            0f to Color.Transparent,
                            0.45f to Color.Black.copy(alpha = 0.12f),
                            1f to Color.Black.copy(alpha = 0.74f),
                        ),
                    ),
                )
            }
            Text(
                item.title,
                style = MaterialTheme.typography.titleMedium.copy(
                    fontWeight = FontWeight.SemiBold,
                ),
                color = if (picture.isNullOrBlank()) ink else Color.White,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.align(Alignment.BottomStart).padding(14.dp),
            )
            Text(
                "\u203A",
                style = MaterialTheme.typography.titleLarge,
                color = if (picture.isNullOrBlank()) ink else Color.White,
                modifier = Modifier.align(Alignment.BottomEnd).padding(end = 14.dp, bottom = 12.dp),
            )
        }
    }
}

/**
 * BitChord's Library: the listening cards, the playlists that live on this
 * machine, and then the account's own liked songs / albums / artists /
 * playlists, with the tabs the app already had.
 *
 * The cards read the local history (`~/.vivimusic/history.json`), so they say
 * what this machine actually played: minutes, top artist, top song and top
 * album, each with its number of plays. With no history yet the row is not
 * drawn at all, instead of showing zeros.
 *
 * BitChord's two rows above its "on device" section (Downloads and Local Music)
 * are not here because the desktop has neither feature: there is no download
 * store and no local-files scan to open, and a row that opens an empty list
 * would be worse than no row. The playlist tiles and the "+" are real
 * ([PlaylistStore]).
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun BitChordLibraryScreen(
    language: String,
    isLoggedIn: Boolean,
    gridItemSize: Int,
    onLoggedIn: () -> Unit,
    onOpenAlbum: (String) -> Unit,
    onOpenArtist: (String) -> Unit,
    onOpenPlaylist: (String) -> Unit,
    onOpenLocalPlaylist: (String) -> Unit,
    onPlaySong: (SongItem) -> Unit,
    onAddToQueue: (SongItem) -> Unit,
    onAddToPlaylist: (SongItem) -> Unit,
    onShuffleAll: (List<SongItem>) -> Unit,
) {
    val history by HistoryStore.tracks.collectAsState()
    val localPlaylists by PlaylistStore.all.collectAsState()
    val playlists = remember(localPlaylists) {
        localPlaylists.filter { !it.deleted }.sortedByDescending { it.updatedAt }
    }

    val minutesListened = remember(history) { history.sumOf { it.durationMs * it.plays } / 60_000 }
    val topArtist = remember(history) {
        history.groupBy { it.artist }.maxByOrNull { group -> group.value.sumOf { it.plays } }
    }
    val topSong = remember(history) { history.maxByOrNull { it.plays } }
    val topAlbum = remember(history) {
        history.filter { !it.album.isNullOrBlank() }
            .groupBy { it.album }
            .maxByOrNull { group -> group.value.sumOf { it.plays } }
    }

    val tabs = listOf("songs", "albums", "artists", "playlists")
    var selectedTab by remember { mutableStateOf(0) }
    var page by remember { mutableStateOf<LibraryPage?>(null) }
    var loading by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    var sortAsc by remember { mutableStateOf(true) }
    var sortByArtist by remember { mutableStateOf(false) }
    var reloadKey by remember { mutableStateOf(0) }
    val libraryRevision by LibraryRefresh.revision.collectAsState()
    var loadedTab by remember { mutableStateOf(-1) }

    val browseIds = listOf(
        "FEmusic_liked_videos",
        "FEmusic_liked_albums",
        ARTISTS_BROWSE_ID,
        "FEmusic_liked_playlists",
    )

    LaunchedEffect(isLoggedIn, selectedTab, reloadKey, libraryRevision) {
        if (!isLoggedIn) return@LaunchedEffect
        if (loadedTab != selectedTab) {
            page = null
            loading = true
        }
        error = null
        loadLibraryPage(browseIds[selectedTab]).fold(
            onSuccess = { page = it; loadedTab = selectedTab; loading = false },
            onFailure = { error = it.message; loading = false },
        )
    }

    Column(Modifier.fillMaxSize().padding(16.dp)) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Text(
                Localization.get(language, "library"),
                style = MaterialTheme.typography.headlineMedium,
                modifier = Modifier.weight(1f),
            )
            if (isLoggedIn && selectedTab == 0 && page?.items?.isNotEmpty() == true) {
                Button(onClick = { onShuffleAll(page!!.items.filterIsInstance<SongItem>()) }) {
                    Icon(Icons.Filled.Shuffle, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(8.dp))
                    Text(Localization.get(language, "shuffle_all"))
                }
            }
        }

        // The four listening cards. Real numbers from the local history, and
        // nothing drawn at all while there is no history to count.
        if (history.isNotEmpty()) {
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.padding(top = 14.dp),
            ) {
                LibraryStatCard(
                    headline = if (minutesListened > 0) minutesListened.toString() else "<1",
                    label = "MINUTES LISTENED",
                    footer = "${history.sumOf { it.plays }} plays - All time",
                )
                LibraryStatCard(
                    headline = topArtist?.key ?: "-",
                    label = "TOP ARTIST",
                    footer = topArtist?.let { g ->
                        "${g.value.sumOf { it.plays }} plays - ${g.value.size} song(s)"
                    } ?: "",
                )
                LibraryStatCard(
                    headline = topSong?.title ?: "-",
                    label = "TOP SONG",
                    footer = topSong?.let { "${it.artist} - ${it.plays} plays" } ?: "",
                )
                if (topAlbum != null) {
                    LibraryStatCard(
                        headline = topAlbum.key.orEmpty(),
                        label = "TOP ALBUM",
                        footer = "${topAlbum.value.sumOf { it.plays }} plays",
                    )
                }
            }
        }

        // The playlists that live on this machine, with the create tile.
        if (playlists.isNotEmpty()) {
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.padding(top = 18.dp),
            ) {
                Surface(
                    onClick = {
                        val created = PlaylistStore.create("Playlist ${localPlaylists.size + 1}")
                        onOpenLocalPlaylist(created.id)
                    },
                    shape = MaterialTheme.shapes.large,
                    color = MaterialTheme.colorScheme.surfaceContainerHigh,
                    modifier = Modifier.width(150.dp).height(150.dp),
                ) {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Icon(
                            Icons.Filled.Add,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(34.dp),
                        )
                    }
                }
                playlists.take(6).forEach { playlist ->
                    Surface(
                        onClick = { onOpenLocalPlaylist(playlist.id) },
                        shape = MaterialTheme.shapes.large,
                        color = MaterialTheme.colorScheme.secondaryContainer,
                        modifier = Modifier.width(150.dp).height(150.dp),
                    ) {
                        Column(Modifier.fillMaxSize().padding(12.dp)) {
                            Text(
                                playlist.name.ifBlank { "Playlist" },
                                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold),
                                color = MaterialTheme.colorScheme.onSecondaryContainer,
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.weight(1f),
                            )
                            Text(
                                "${playlist.songs.size} tracks",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSecondaryContainer,
                            )
                        }
                    }
                }
            }
        }

        // Real playback without an account: these rows are this machine's own
        // history, so the Library can start a track even when signed out.
        if (history.isNotEmpty()) {
            Text(
                "Recently played",
                style = MaterialTheme.typography.titleLarge,
                modifier = Modifier.padding(top = 20.dp, bottom = 6.dp),
            )
            LazyColumn(Modifier.fillMaxWidth().height(290.dp)) {
                items(history.take(5), key = { it.videoId }) { entry ->
                    val song = entry.toLibrarySong()
                    SongRow(song, language, { onPlaySong(song) })
                }
            }
        }

        if (!isLoggedIn) {
            Text(
                Localization.get(language, "library_login_prompt"),
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 20.dp),
            )
            LoginContent(language = language, onLoggedIn = onLoggedIn)
            return@Column
        }

        Row(
            Modifier.fillMaxWidth().padding(top = 18.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            tabs.forEachIndexed { i, key ->
                FilterChip(
                    selected = i == selectedTab,
                    onClick = { selectedTab = i },
                    label = { Text(Localization.get(language, key)) },
                )
                if (i != tabs.lastIndex) Spacer(Modifier.width(8.dp))
            }
        }

        if (page?.items?.isNotEmpty() == true) {
            Row(
                Modifier.fillMaxWidth().padding(top = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                FilterChip(
                    selected = sortAsc,
                    onClick = { sortAsc = true },
                    label = { Text(Localization.get(language, "sort_az")) },
                )
                FilterChip(
                    selected = !sortAsc,
                    onClick = { sortAsc = false },
                    label = { Text(Localization.get(language, "sort_za")) },
                )
                if (selectedTab == 0) {
                    FilterChip(
                        selected = sortByArtist,
                        onClick = { sortByArtist = !sortByArtist },
                        label = { Text(Localization.get(language, "sort_artist")) },
                    )
                }
            }
        }

        when {
            error != null -> ErrorBox(language, error)
            loading || page == null -> LoadingBox(
                language,
                hint = if (selectedTab == 2) Localization.get(language, "artists_loading_hint") else null,
            )
            else -> {
                val rawItems = page!!.items
                val items = run {
                    val base = if (selectedTab == 0) {
                        val songs = rawItems.filterIsInstance<SongItem>()
                        if (sortByArtist) {
                            songs.sortedWith(
                                compareBy(
                                    { it.artists.firstOrNull()?.name.orEmpty().lowercase() },
                                    { it.title.lowercase() },
                                ),
                            )
                        } else {
                            songs.sortedBy { it.title.lowercase() }
                        }
                    } else {
                        rawItems.sortedBy { it.title.lowercase() }
                    }
                    if (sortAsc) base else base.reversed()
                }
                if (items.isEmpty()) {
                    Column(Modifier.padding(top = 16.dp)) {
                        Text(
                            Localization.get(language, "library_empty"),
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        Spacer(Modifier.size(12.dp))
                        Button(onClick = { reloadKey++ }) {
                            Text(Localization.get(language, "refresh"))
                        }
                    }
                } else if (selectedTab == 0) {
                    LazyColumn(Modifier.fillMaxSize().padding(top = 8.dp)) {
                        items(items.filterIsInstance<SongItem>(), key = { it.id }) { song ->
                            SongRow(
                                song,
                                language,
                                { onPlaySong(song) },
                                onAddToQueue = { onAddToQueue(song) },
                                onAddToPlaylist = { onAddToPlaylist(song) },
                            )
                        }
                    }
                } else {
                    LazyVerticalGrid(
                        columns = GridCells.Adaptive(gridItemSize.dp),
                        modifier = Modifier.fillMaxSize().padding(top = 8.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        gridItems(items, key = { it.id }) { item ->
                            Box(Modifier.fillMaxWidth()) {
                                YtItemCard(
                                    item = item,
                                    width = null,
                                    onClick = {
                                        onItemClick(item, onOpenAlbum, onOpenArtist, onOpenPlaylist, onPlaySong)
                                    },
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

/** One of the four listening cards on the Library: a value, its label, a footer. */
@Composable
private fun LibraryStatCard(
    headline: String,
    label: String,
    footer: String,
) {
    Surface(
        shape = MaterialTheme.shapes.large,
        color = MaterialTheme.colorScheme.surfaceContainerHigh,
        modifier = Modifier.width(232.dp).height(150.dp),
    ) {
        Column(Modifier.fillMaxSize().padding(14.dp)) {
            Text(
                label,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Spacer(Modifier.height(12.dp))
            Text(
                headline,
                style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold),
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
            Spacer(Modifier.weight(1f))
            Text(
                footer,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

/** A stored history row as a playable song (the shape the History screen uses). */
private fun HistoryEntry.toLibrarySong(): SongItem = SongItem(
    id = videoId,
    title = title,
    artists = artist.split(",").map { it.trim() }.filter { it.isNotEmpty() }.map { Artist(name = it, id = null) },
    duration = if (durationMs > 0) (durationMs / 1000).toInt() else null,
    thumbnail = thumbnail?.takeIf { it.isNotBlank() } ?: "https://i.ytimg.com/vi/$videoId/hqdefault.jpg",
)
