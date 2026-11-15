package com.music.vivi.desktop

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.hoverable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsHoveredAsState
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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Album
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.MoreHoriz
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Shuffle
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.music.innertube.YouTube
import com.music.innertube.models.AlbumItem
import com.music.innertube.models.ArtistItem
import com.music.innertube.models.SongItem
import com.music.innertube.models.YTItem
import com.music.innertube.pages.AlbumPage
import com.music.innertube.pages.ArtistPage
import kotlinx.coroutines.launch

/**
 * Bit's album page. The reference draws the release as one wide block:
 * the 286 dp sleeve, the small ALBUM label, the title, the artist in the accent
 * colour, the metadata line and the round shuffle / play / search / overflow
 * controls. The table under it drops the sleeve, because every row is on that
 * album, and the album column, because it would only repeat the page title.
 *
 * The loading and error states are the app's own boxes, on the same page
 * colour, so the screen never flashes a different background while it waits.
 */
@Composable
fun BitAlbumScreen(
    browseId: String,
    language: String,
    onOpenArtist: (String) -> Unit,
    onPlaySong: (SongItem) -> Unit,
    onAddToQueue: (SongItem) -> Unit,
    onAddToPlaylist: (SongItem) -> Unit,
    onPlayAll: (List<SongItem>) -> Unit,
    onShuffleAll: (List<SongItem>) -> Unit,
) {
    var page by remember { mutableStateOf<AlbumPage?>(null) }
    var error by remember { mutableStateOf<String?>(null) }
    var refreshKey by remember { mutableStateOf(0) }
    val actions = LocalSongMenuActions.current

    LaunchedEffect(browseId, refreshKey) {
        YouTube.album(browseId).fold(
            onSuccess = { page = it.filteredContent() },
            onFailure = { error = it.message },
        )
    }

    Column(Modifier.fillMaxSize().background(BitColors.page())) {
        when {
            error != null -> ErrorBox(language, error)
            page == null -> LoadingBox(language)
            else -> BitAlbumContent(
                album = page!!.album,
                songs = page!!.songs,
                language = language,
                onOpenArtist = onOpenArtist,
                onPlaySong = onPlaySong,
                onAddToQueue = onAddToQueue,
                onAddToPlaylist = onAddToPlaylist,
                onPlayAll = onPlayAll,
                onShuffleAll = onShuffleAll,
                onDownloadAll = { songs -> songs.forEach(actions.download) },
                onShare = {
                    copyToClipboard(page!!.album.shareLink)
                    DesktopSnackbar.show(Localization.get(language, "copied_to_clipboard"))
                },
                onRefresh = { refreshKey++ },
            )
        }
    }
}

@Composable
internal fun BitAlbumContent(
    album: AlbumItem,
    songs: List<SongItem>,
    language: String,
    onOpenArtist: (String) -> Unit = {},
    onPlaySong: (SongItem) -> Unit = {},
    onAddToQueue: (SongItem) -> Unit = {},
    onAddToPlaylist: (SongItem) -> Unit = {},
    onPlayAll: (List<SongItem>) -> Unit = {},
    onShuffleAll: (List<SongItem>) -> Unit = {},
    onDownloadAll: (List<SongItem>) -> Unit = {},
    onShare: () -> Unit = {},
    onRefresh: () -> Unit = {},
) {
    var menuOpen by remember { mutableStateOf(false) }
    val totalSeconds = remember(songs) { songs.sumOf { it.duration ?: 0 } }
    val metadata = buildList {
        add("ALBUM")
        album.year?.let { add(it.toString()) }
        if (songs.isNotEmpty()) add("${songs.size} songs")
        totalDurationLabel(totalSeconds)?.let(::add)
    }.joinToString(" • ").uppercase()

    LazyColumn(
        modifier = Modifier.fillMaxSize().background(BitColors.page()),
        contentPadding = PaddingValues(start = 24.dp, end = 24.dp, top = 24.dp, bottom = 32.dp),
        verticalArrangement = Arrangement.spacedBy(0.dp),
    ) {
        item(key = "album-header") {
            Row(
                Modifier.fillMaxWidth().padding(bottom = 36.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Thumbnail(
                    album.thumbnail,
                    Modifier.size(286.dp),
                    shape = RoundedCornerShape(8.dp),
                )
                Spacer(Modifier.width(30.dp))
                Column(Modifier.weight(1f)) {
                    Text(
                        "ALBUM",
                        color = BitColors.secondary(),
                        style = MaterialTheme.typography.labelMedium.copy(
                            fontWeight = FontWeight.SemiBold,
                            letterSpacing = 1.2.sp,
                        ),
                    )
                    Spacer(Modifier.height(8.dp))
                    Text(
                        album.title,
                        style = MaterialTheme.typography.displaySmall.copy(fontWeight = FontWeight.Bold),
                        color = if (isAppInDarkTheme()) Color.White else MaterialTheme.colorScheme.onBackground,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                    )
                    album.artists?.firstOrNull()?.let { artist ->
                        Spacer(Modifier.height(6.dp))
                        Text(
                            artist.name,
                            color = MaterialTheme.colorScheme.primary,
                            style = MaterialTheme.typography.titleMedium,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.clickable(enabled = artist.id != null) {
                                artist.id?.let(onOpenArtist)
                            },
                        )
                    }
                    if (metadata.isNotBlank()) {
                        Spacer(Modifier.height(8.dp))
                        Text(
                            metadata,
                            color = BitColors.secondary(),
                            style = MaterialTheme.typography.bodySmall,
                        )
                    }
                    Spacer(Modifier.height(22.dp))
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        BitCircleAction(Icons.Filled.Shuffle, Localization.get(language, "shuffle"), enabled = songs.isNotEmpty()) {
                            onShuffleAll(songs)
                        }
                        BitPlayPill(Localization.get(language, "play"), enabled = songs.isNotEmpty()) {
                            onPlayAll(songs)
                        }
                        Box {
                            BitCircleAction(Icons.Filled.MoreHoriz, Localization.get(language, "more")) {
                                menuOpen = true
                            }
                            DropdownMenu(expanded = menuOpen, onDismissRequest = { menuOpen = false }) {
                                DropdownMenuItem(
                                    text = { Text(Localization.get(language, "download_all")) },
                                    leadingIcon = { Icon(Icons.Filled.Download, contentDescription = null) },
                                    onClick = {
                                        menuOpen = false
                                        onDownloadAll(songs)
                                    },
                                )
                                DropdownMenuItem(
                                    text = { Text(Localization.get(language, "share")) },
                                    leadingIcon = { Icon(Icons.Filled.Share, contentDescription = null) },
                                    onClick = {
                                        menuOpen = false
                                        onShare()
                                    },
                                )
                                HorizontalDivider()
                                DropdownMenuItem(
                                    text = { Text(Localization.get(language, "refresh")) },
                                    leadingIcon = { Icon(Icons.Filled.Refresh, contentDescription = null) },
                                    onClick = {
                                        menuOpen = false
                                        onRefresh()
                                    },
                                )
                            }
                        }
                    }
                }
            }
        }
        if (songs.isEmpty()) {
            item(key = "album-empty") {
                Text(
                    Localization.get(language, "empty_playlist"),
                    style = MaterialTheme.typography.bodyLarge,
                    color = BitColors.secondary(),
                    modifier = Modifier.padding(top = 8.dp),
                )
            }
        } else {
            itemsIndexed(songs, key = { index, song -> "${song.id}-$index" }, contentType = { _, _ -> "albumSong" }) { index, song ->
                BitDetailSongRow(
                    number = index + 1,
                    song = song,
                    language = language,
                    showCover = false,
                    showArtist = false,
                    showAlbum = false,
                    striped = index % 2 == 1,
                    onPlay = { onPlaySong(song) },
                    onAddToQueue = { onAddToQueue(song) },
                    onAddToPlaylist = { onAddToPlaylist(song) },
                )
            }
        }
    }
}

/**
 * Bit's artist page: the 340 dp photograph with the name across its foot,
 * the listener chips, subscribe / play / shuffle, the about block, the top
 * songs in two columns of four and then the release shelves. There are no tabs:
 * the phone's artist page draws its shelves one after another and so does this.
 */
@Composable
fun BitArtistScreen(
    browseId: String,
    language: String,
    onOpenAlbum: (String) -> Unit,
    onOpenArtist: (String) -> Unit,
    onOpenPlaylist: (String) -> Unit,
    onPlaySong: (SongItem) -> Unit,
    onAddToQueue: (SongItem) -> Unit,
    onAddToPlaylist: (SongItem) -> Unit,
    onPlayAll: (List<SongItem>) -> Unit,
    onShuffleAll: (List<SongItem>) -> Unit,
) {
    var page by remember { mutableStateOf<ArtistPage?>(null) }
    var error by remember { mutableStateOf<String?>(null) }
    var refreshKey by remember { mutableStateOf(0) }

    LaunchedEffect(browseId, refreshKey) {
        YouTube.artist(browseId).fold(
            onSuccess = { page = it.filteredContent() },
            onFailure = { error = it.message },
        )
    }

    Column(Modifier.fillMaxSize().background(BitColors.page())) {
        when {
            error != null -> ErrorBox(language, error)
            page == null -> LoadingBox(language)
            else -> BitArtistContent(
                artist = page!!.artist,
                sections = page!!.sections,
                description = page!!.description,
                subscriberCountText = page!!.subscriberCountText,
                monthlyListenerCount = page!!.monthlyListenerCount,
                language = language,
                onOpenAlbum = onOpenAlbum,
                onOpenArtist = onOpenArtist,
                onOpenPlaylist = onOpenPlaylist,
                onPlaySong = onPlaySong,
                onAddToQueue = onAddToQueue,
                onAddToPlaylist = onAddToPlaylist,
                onPlayAll = onPlayAll,
                onShuffleAll = onShuffleAll,
            )
        }
    }
}

@Composable
internal fun BitArtistContent(
    artist: ArtistItem,
    sections: List<com.music.innertube.pages.ArtistSection>,
    description: String?,
    subscriberCountText: String?,
    monthlyListenerCount: String?,
    language: String,
    onOpenAlbum: (String) -> Unit = {},
    onOpenArtist: (String) -> Unit = {},
    onOpenPlaylist: (String) -> Unit = {},
    onPlaySong: (SongItem) -> Unit = {},
    onAddToQueue: (SongItem) -> Unit = {},
    onAddToPlaylist: (SongItem) -> Unit = {},
    onPlayAll: (List<SongItem>) -> Unit = {},
    onShuffleAll: (List<SongItem>) -> Unit = {},
) {
    val topSongs = remember(sections) {
        sections.flatMap { section -> section.items.filterIsInstance<SongItem>() }
            .distinctBy { it.id }
            .take(20)
    }
    val subscribed = DetailActions.isArtistSubscribed(artist.id)
    val scope = rememberCoroutineScope()

    LazyColumn(
        modifier = Modifier.fillMaxSize().background(BitColors.page()),
        contentPadding = PaddingValues(bottom = 32.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        item(key = "artist-banner") {
            BitArtistBanner(artist.thumbnail, artist.title)
        }
        if (subscriberCountText != null || monthlyListenerCount != null) {
            item(key = "artist-stats") {
                Row(
                    Modifier.fillMaxWidth().padding(horizontal = 24.dp, vertical = 12.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    subscriberCountText?.let {
                        BitStatChip(Icons.Filled.Person, it.substringBefore(' ') + " subscribers")
                    }
                    monthlyListenerCount?.let {
                        BitStatChip(Icons.Filled.GraphicEq, it.substringBefore(' ') + " monthly listeners")
                    }
                }
            }
        }
        item(key = "artist-actions") {
            Row(
                Modifier.fillMaxWidth().padding(horizontal = 24.dp, vertical = 6.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                BitCircleAction(
                    if (subscribed) Icons.Filled.Favorite else Icons.Filled.FavoriteBorder,
                    Localization.get(language, if (subscribed) "subscribed" else "subscribe"),
                ) {
                    val next = DetailActions.toggleArtistSubscribed(artist.id)
                    scope.launch { YouTube.subscribeChannel(artist.id, next) }
                }
                BitPlayPill(Localization.get(language, "play"), enabled = topSongs.isNotEmpty()) {
                    onPlayAll(topSongs)
                }
                BitCircleAction(Icons.Filled.Shuffle, Localization.get(language, "shuffle"), enabled = topSongs.isNotEmpty()) {
                    onShuffleAll(topSongs)
                }
            }
        }
        description?.takeIf { it.isNotBlank() }?.let { blurb ->
            item(key = "artist-about") {
                Column(Modifier.padding(top = 16.dp)) {
                    BitSectionTitle(Localization.get(language, "about_artist"))
                    Spacer(Modifier.height(8.dp))
                    Text(
                        blurb,
                        color = BitColors.secondary(),
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.padding(horizontal = 24.dp),
                    )
                }
            }
        }
        if (topSongs.isNotEmpty()) {
            item(key = "artist-top-songs") {
                Column(Modifier.padding(top = 20.dp, bottom = 6.dp)) {
                    BitSectionTitle(Localization.get(language, "top_songs"))
                    Spacer(Modifier.height(8.dp))
                    LazyRow(
                        contentPadding = PaddingValues(horizontal = 24.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        items(topSongs.chunked(4), key = { column -> column.first().id + "-" + column.size }) { column ->
                            Column(Modifier.width(400.dp)) {
                                column.forEach { song ->
                                    BitArtistTrack(
                                        song = song,
                                        language = language,
                                        onPlay = { onPlaySong(song) },
                                        onAddToQueue = { onAddToQueue(song) },
                                        onAddToPlaylist = { onAddToPlaylist(song) },
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
        sections.forEach { section ->
            item(key = "artist-shelf-${section.title}") {
                Column(Modifier.padding(top = 18.dp)) {
                    BitSectionTitle(section.title)
                    Spacer(Modifier.height(8.dp))
                    LazyRow(
                        contentPadding = PaddingValues(horizontal = 24.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        items(section.items, key = { it.id }, contentType = { "artistCard" }) { item ->
                            YtItemCard(
                                item = item,
                                width = 160.dp,
                                onClick = {
                                    onItemClick(item, onOpenAlbum, onOpenArtist, onOpenPlaylist, onPlaySong)
                                },
                            )
                        }
                    }
                }
            }
        }
        if (topSongs.isEmpty() && sections.isEmpty()) {
            item(key = "artist-empty") {
                Text(
                    Localization.get(language, "empty_playlist"),
                    color = BitColors.secondary(),
                    style = MaterialTheme.typography.bodyLarge,
                    modifier = Modifier.padding(24.dp),
                )
            }
        }
    }
}

@Composable
private fun BitArtistBanner(url: String?, name: String) {
    Box(Modifier.fillMaxWidth().height(340.dp)) {
        Thumbnail(url, Modifier.fillMaxSize(), shape = RectangleShape)
        Box(
            Modifier.fillMaxSize().background(
                Brush.verticalGradient(
                    0f to Color.Black.copy(alpha = 0.20f),
                    0.55f to Color.Transparent,
                    1f to Color.Black.copy(alpha = 0.78f),
                ),
            ),
        )
        Text(
            name,
            style = MaterialTheme.typography.displaySmall.copy(fontWeight = FontWeight.Bold),
            color = Color.White,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.align(Alignment.BottomStart).padding(start = 24.dp, end = 24.dp, bottom = 18.dp),
        )
    }
}

@Composable
private fun BitStatChip(icon: androidx.compose.ui.graphics.vector.ImageVector, text: String) {
    Row(
        Modifier.clip(CircleShape).background(BitColors.chrome()).padding(horizontal = 12.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(icon, contentDescription = null, tint = BitColors.secondary(), modifier = Modifier.size(15.dp))
        Spacer(Modifier.width(7.dp))
        Text(text, style = MaterialTheme.typography.bodySmall, color = if (isAppInDarkTheme()) Color.White else MaterialTheme.colorScheme.onSurface)
    }
}

@Composable
private fun BitSectionTitle(text: String) {
    Text(
        text,
        style = MaterialTheme.typography.titleLarge.copy(fontSize = 22.sp, fontWeight = FontWeight.SemiBold),
        color = if (isAppInDarkTheme()) Color.White else MaterialTheme.colorScheme.onBackground,
        modifier = Modifier.padding(horizontal = 24.dp),
    )
}

@Composable
private fun BitPlayPill(label: String, enabled: Boolean, onClick: () -> Unit) {
    Surface(
        shape = CircleShape,
        color = if (enabled) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceContainerHighest,
        contentColor = if (enabled) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.height(40.dp).clip(CircleShape).clickable(enabled = enabled, onClick = onClick),
    ) {
        Row(
            Modifier.padding(horizontal = 18.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(7.dp),
        ) {
            Icon(Icons.Filled.PlayArrow, contentDescription = null, modifier = Modifier.size(18.dp))
            Text(label, style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.SemiBold)
        }
    }
}

@Composable
private fun BitCircleAction(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    enabled: Boolean = true,
    onClick: () -> Unit,
) {
    Tooltip(label) {
        Surface(
            shape = CircleShape,
            color = if (isAppInDarkTheme()) BitColors.chrome() else MaterialTheme.colorScheme.surfaceContainerHigh,
            contentColor = if (enabled) {
                if (isAppInDarkTheme()) Color.White else MaterialTheme.colorScheme.onSurface
            } else {
                MaterialTheme.colorScheme.onSurfaceVariant
            },
            modifier = Modifier.size(40.dp).clip(CircleShape).clickable(enabled = enabled, onClick = onClick),
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(icon, contentDescription = label, modifier = Modifier.size(19.dp))
            }
        }
    }
}

@Composable
private fun BitArtistTrack(
    song: SongItem,
    language: String,
    onPlay: () -> Unit,
    onAddToQueue: () -> Unit,
    onAddToPlaylist: () -> Unit,
) {
    Row(
        Modifier.fillMaxWidth().height(56.dp).clip(RoundedCornerShape(7.dp)).clickable(onClick = onPlay).padding(horizontal = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Thumbnail(song.thumbnail, Modifier.size(48.dp), shape = RoundedCornerShape(7.dp))
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(
                song.title,
                style = MaterialTheme.typography.titleMedium,
                color = if (isAppInDarkTheme()) Color.White else MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                song.artists.joinToString(", ") { it.name },
                style = MaterialTheme.typography.bodyMedium,
                color = BitColors.secondary(),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
        SongMenu(
            song = song,
            language = language,
            onAddToPlaylist = onAddToPlaylist,
            onAddToQueue = onAddToQueue,
        )
    }
}

/**
 * One table row: place, optional sleeve, title, artist, optional album, length
 * and the row's own like / download / overflow. Every album row has the same
 * fixed action column, so the duration lines up down the page.
 */
@Composable
private fun BitDetailSongRow(
    number: Int,
    song: SongItem,
    language: String,
    showCover: Boolean,
    showArtist: Boolean,
    showAlbum: Boolean,
    striped: Boolean,
    onPlay: () -> Unit,
    onAddToQueue: () -> Unit,
    onAddToPlaylist: () -> Unit,
) {
    val interaction = remember { MutableInteractionSource() }
    val hovered by interaction.collectIsHoveredAsState()
    val liked = SongActions.isLiked(song.id)
    val actions = LocalSongMenuActions.current
    val downloaded = actions.isDownloaded(song)
    val scope = rememberCoroutineScope()

    Row(
        Modifier
            .fillMaxWidth()
            .height(56.dp)
            .clip(RoundedCornerShape(6.dp))
            .background(
                when {
                    hovered -> BitColors.Hover
                    striped -> Color.White.copy(alpha = 0.04f)
                    else -> Color.Transparent
                },
            )
            .hoverable(interaction)
            .clickable(onClick = onPlay)
            .padding(horizontal = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            number.toString(),
            Modifier.width(28.dp),
            color = BitColors.secondary(),
            textAlign = TextAlign.Center,
            style = MaterialTheme.typography.labelSmall,
        )
        Spacer(Modifier.width(12.dp))
        if (showCover) {
            Thumbnail(song.thumbnail, Modifier.size(36.dp).clip(MaterialTheme.shapes.extraSmall))
            Spacer(Modifier.width(10.dp))
        }
        Text(
            song.title,
            if (showArtist) Modifier.weight(0.42f) else Modifier.weight(1f),
            color = if (isAppInDarkTheme()) Color.White.copy(alpha = 0.88f) else MaterialTheme.colorScheme.onSurface,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            fontWeight = FontWeight.Medium,
        )
        if (showArtist) {
            Text(
                song.artists.joinToString(", ") { it.name },
                Modifier.weight(0.20f).padding(horizontal = 8.dp),
                color = BitColors.secondary(),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
        if (showAlbum) {
            Text(
                song.album?.name ?: "-",
                Modifier.weight(0.25f).padding(horizontal = 8.dp),
                color = BitColors.secondary(),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
        Text(
            song.duration?.let { formatTime(it * 1000L) } ?: "-",
            Modifier.width(58.dp),
            color = BitColors.secondary(),
            textAlign = TextAlign.End,
            style = MaterialTheme.typography.bodySmall,
        )
        Row(
            Modifier.width(102.dp),
            horizontalArrangement = Arrangement.End,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            M3IconButton(onClick = {
                val next = !liked
                SongActions.setLiked(
                    song.id,
                    next,
                    title = song.title,
                    artist = song.artists.joinToString(", ") { it.name },
                    thumbnail = song.thumbnail,
                )
                scope.launch { YouTube.likeVideo(song.id, next) }
            }, modifier = Modifier.size(32.dp)) {
                Icon(
                    if (liked) Icons.Filled.Favorite else Icons.Filled.FavoriteBorder,
                    contentDescription = Localization.get(language, if (liked) "unlike" else "like"),
                    tint = if (liked) MaterialTheme.colorScheme.primary else BitColors.secondary(),
                    modifier = Modifier.size(17.dp),
                )
            }
            M3IconButton(onClick = {
                if (downloaded) actions.deleteDownload(song) else actions.download(song)
            }, modifier = Modifier.size(32.dp)) {
                Icon(
                    if (downloaded) Icons.Filled.Delete else Icons.Filled.Download,
                    contentDescription = Localization.get(language, if (downloaded) "remove_download" else "download"),
                    tint = BitColors.secondary(),
                    modifier = Modifier.size(17.dp),
                )
            }
            SongMenu(
                song = song,
                language = language,
                onAddToPlaylist = onAddToPlaylist,
                onAddToQueue = onAddToQueue,
            )
        }
    }
    HorizontalDivider(color = BitColors.Divider)
}

private fun totalDurationLabel(totalSeconds: Int): String? {
    if (totalSeconds <= 0) return null
    val hours = totalSeconds / 3600
    val minutes = (totalSeconds % 3600) / 60
    if (minutes == 0 && hours == 0) return null
    return if (hours > 0) "$hours hr $minutes min" else "$minutes min"
}
