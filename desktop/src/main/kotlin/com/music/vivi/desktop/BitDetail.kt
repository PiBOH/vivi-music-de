package com.music.vivi.desktop

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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Download
import androidx.compose.material.icons.rounded.MoreHoriz
import androidx.compose.material.icons.rounded.QueueMusic
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.PlaylistAdd
import androidx.compose.material.icons.rounded.Share
import androidx.compose.material.icons.rounded.Shuffle
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import com.music.innertube.YouTube
import com.music.innertube.models.AlbumItem
import com.music.innertube.models.ArtistItem
import com.music.innertube.models.PlaylistItem
import com.music.innertube.models.SongItem
import com.music.innertube.models.YTItem
import com.music.innertube.pages.AlbumPage
import com.music.innertube.pages.ArtistPage
import com.music.innertube.pages.PlaylistPage
import com.music.vivi.desktop.player.PlayerController

/** A round or pill action button, the shape every Bit page header uses. */
@Composable
private fun BcActionButton(
    label: String,
    filled: Boolean,
    onClick: () -> Unit,
    icon: androidx.compose.ui.graphics.vector.ImageVector? = null,
    round: Boolean = false,
) {
    val shape = if (round) CircleShape else RoundedCornerShape(50)
    Row(
        Modifier
            .height(44.dp)
            .clip(shape)
            .background(if (filled) BC.White else Color.White.copy(alpha = 0.10f))
            .clickable(onClick = onClick)
            .padding(horizontal = if (round) 12.dp else 18.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (icon != null) {
            Icon(
                icon,
                label,
                tint = if (filled) Color.Black else BC.White,
                modifier = Modifier.size(if (round) 24.dp else 19.dp),
            )
        }
        if (!round) {
            Spacer(Modifier.width(6.dp))
            Text(
                label,
                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold),
                color = if (filled) Color.Black else BC.White,
            )
        }
    }
}

@Composable
private fun BcSongHeaderActions(
    songs: List<SongItem>,
    onPlayAll: (List<SongItem>, Int) -> Unit,
    onPlayNextAll: (List<SongItem>) -> Unit = {},
    onQueueAll: (List<SongItem>) -> Unit = {},
    onSaveAll: (List<SongItem>) -> Unit = {},
    shareLink: String? = null,
) {
    // The two round buttons on the right used to be decoration: "+" and "..."
    // had an empty onClick, so an album or a playlist had no menu at all. They
    // now carry the reference's collection menu, in the same order as the
    // per-song one.
    var menu by remember { mutableStateOf(false) }
    Row(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.CenterVertically) {
        BcActionButton("", filled = false, onClick = { onPlayAll(songs.shuffled(), 0) }, icon = Icons.Rounded.Shuffle, round = true)
        BcActionButton("Play", filled = true, onClick = { onPlayAll(songs, 0) }, icon = Icons.Rounded.PlayArrow)
        BcActionButton("", filled = false, onClick = { onQueueAll(songs) }, icon = Icons.Rounded.Add, round = true)
        Box {
            BcActionButton("", filled = false, onClick = { menu = true }, icon = Icons.Rounded.MoreHoriz, round = true)
            DropdownMenu(
                expanded = menu,
                onDismissRequest = { menu = false },
                modifier = Modifier.background(BC.Chrome),
            ) {
                DropdownMenuItem(
                    text = { Text("Play next", color = BC.White) },
                    leadingIcon = { Icon(Icons.Rounded.Add, null, tint = BC.Secondary, modifier = Modifier.size(18.dp)) },
                    onClick = { menu = false; onPlayNextAll(songs) },
                )
                DropdownMenuItem(
                    text = { Text("Add to queue", color = BC.White) },
                    leadingIcon = { Icon(Icons.Rounded.QueueMusic, null, tint = BC.Secondary, modifier = Modifier.size(18.dp)) },
                    onClick = { menu = false; onQueueAll(songs) },
                )
                DropdownMenuItem(
                    text = { Text("Save to library", color = BC.White) },
                    leadingIcon = { Icon(Icons.Rounded.PlaylistAdd, null, tint = BC.Secondary, modifier = Modifier.size(18.dp)) },
                    onClick = { menu = false; onSaveAll(songs) },
                )
                DropdownMenuItem(
                    text = { Text("Download", color = BC.White) },
                    leadingIcon = { Icon(Icons.Rounded.Download, null, tint = BC.Secondary, modifier = Modifier.size(18.dp)) },
                    onClick = { menu = false; BitDownloads.songs(songs) },
                )
                if (shareLink != null) {
                    DropdownMenuItem(
                        text = { Text("Share", color = BC.White) },
                        leadingIcon = { Icon(Icons.Rounded.Share, null, tint = BC.Secondary, modifier = Modifier.size(18.dp)) },
                        onClick = {
                            menu = false
                            copyToClipboard(shareLink)
                            DesktopSnackbar.show("Link copied")
                        },
                    )
                }
            }
        }
    }
}

// ---------------------------------------------------------------------------
// Album
// ---------------------------------------------------------------------------

@Composable
internal fun BcAlbumPage(
    browseId: String,
    language: String,
    onOpenArtist: (String) -> Unit,
    onPlaySong: (SongItem) -> Unit,
    onPlayAll: (List<SongItem>, Int) -> Unit,
    onPlayNext: (SongItem) -> Unit,
    onAddToQueue: (SongItem) -> Unit,
    onAddToPlaylist: (SongItem) -> Unit,
    onPlayNextAll: (List<SongItem>) -> Unit = {},
    onQueueAll: (List<SongItem>) -> Unit = {},
    onSaveAll: (List<SongItem>) -> Unit = {},
) {
    var page by remember { mutableStateOf<AlbumPage?>(null) }
    var error by remember { mutableStateOf<String?>(null) }
    LaunchedEffect(browseId) {
        YouTube.album(browseId).fold(
            onSuccess = { page = it },
            onFailure = { error = it.message },
        )
    }
    when {
        error != null -> BcError(error)
        page == null -> BcLoading()
        else -> {
            val album = page!!.album
            val songs = page!!.songs
            // The header's artist run carries no browse id on most albums, so
            // the link resolves the id from the album's own song credits, which
            // do. Without it the name is drawn red and does nothing, which is
            // not a link.
            val headerArtist = album.artists?.firstOrNull()
            val headerArtistId = headerArtist?.id
                ?: headerArtist?.let { named ->
                    songs.firstNotNullOfOrNull { song ->
                        song.artists.firstOrNull { it.name.equals(named.name, ignoreCase = true) }?.id
                    }
                }
            LazyColumn(
                Modifier.fillMaxSize().background(BC.Page),
                contentPadding = PaddingValues(start = 16.dp, end = 18.dp, top = 22.dp, bottom = 40.dp),
            ) {
                item(key = "album-header") {
                    Row(Modifier.fillMaxWidth().padding(bottom = 30.dp), verticalAlignment = Alignment.Bottom) {
                        BcArtwork(album.thumbnail, Modifier.size(232.dp), RoundedCornerShape(6.dp))
                        Spacer(Modifier.width(28.dp))
                        Column(Modifier.weight(1f)) {
                            Text(
                                "ALBUM",
                                style = MaterialTheme.typography.labelMedium.copy(letterSpacing = 1.3.sp),
                                color = BC.Secondary,
                            )
                            Spacer(Modifier.height(8.dp))
                            Text(
                                album.title,
                                style = MaterialTheme.typography.displayLarge,
                                color = BC.White,
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis,
                            )
                            headerArtist?.let { artist ->
                                Spacer(Modifier.height(6.dp))
                                Text(
                                    artist.name,
                                    style = MaterialTheme.typography.titleMedium,
                                    color = BC.Red,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                    modifier = Modifier.clickable(enabled = headerArtistId != null) {
                                        headerArtistId?.let(onOpenArtist)
                                    },
                                )
                            }
                            Spacer(Modifier.height(6.dp))
                            val meta = buildList {
                                album.year?.let { add(it.toString()) }
                                if (songs.isNotEmpty()) add("${songs.size} songs")
                                val total = songs.sumOf { it.duration ?: 0 }
                                bcDuration(total).takeIf { it.isNotBlank() }?.let(::add)
                            }.joinToString(" - ")
                            if (meta.isNotBlank()) BcSecondary(meta)
                            Spacer(Modifier.height(20.dp))
                            BcSongHeaderActions(
                                songs = songs,
                                onPlayAll = onPlayAll,
                                onPlayNextAll = onPlayNextAll,
                                onQueueAll = onQueueAll,
                                onSaveAll = onSaveAll,
                            )
                        }
                    }
                }
                item(key = "album-divider") {
                    Box(Modifier.fillMaxWidth().height(1.dp).background(BC.Divider))
                }
                itemsIndexed(songs, key = { _, song -> song.id }) { index, song ->
                    BcSongRow(
                        song = song,
                        number = index + 1,
                        isCurrent = false,
                        onClick = { onPlaySong(song) },
                        onPlayNext = { onPlayNext(song) },
                        onAddToQueue = { onAddToQueue(song) },
                        onAddToPlaylist = { onAddToPlaylist(song) },
                    )
                }
                if (songs.isEmpty()) {
                    item {
                        Text(
                            "No tracks",
                            style = MaterialTheme.typography.bodyMedium,
                            color = BC.Secondary,
                            modifier = Modifier.padding(vertical = 30.dp),
                        )
                    }
                }
            }
        }
    }
}

// ---------------------------------------------------------------------------
// Playlist
// ---------------------------------------------------------------------------

@Composable
internal fun BcPlaylistPage(
    playlistId: String,
    language: String,
    onOpenArtist: (String) -> Unit,
    onPlaySong: (SongItem) -> Unit,
    onPlayAll: (List<SongItem>, Int) -> Unit,
    onPlayNext: (SongItem) -> Unit,
    onAddToQueue: (SongItem) -> Unit,
    onAddToPlaylist: (SongItem) -> Unit,
    onPlayNextAll: (List<SongItem>) -> Unit = {},
    onQueueAll: (List<SongItem>) -> Unit = {},
    onSaveAll: (List<SongItem>) -> Unit = {},
    shareLink: String? = null,
) {
    var page by remember { mutableStateOf<PlaylistPage?>(null) }
    var error by remember { mutableStateOf<String?>(null) }
    LaunchedEffect(playlistId) {
        YouTube.playlist(playlistId).fold(
            onSuccess = { page = it },
            onFailure = { error = it.message },
        )
    }
    when {
        error != null -> BcError(error)
        page == null -> BcLoading()
        else -> {
            val playlist = page!!.playlist
            val songs = page!!.songs
            LazyColumn(
                Modifier.fillMaxSize().background(BC.Page),
                contentPadding = PaddingValues(start = 16.dp, end = 18.dp, top = 22.dp, bottom = 40.dp),
            ) {
                item(key = "playlist-header") {
                    Row(Modifier.fillMaxWidth().padding(bottom = 30.dp), verticalAlignment = Alignment.Bottom) {
                        BcArtwork(playlist.thumbnail, Modifier.size(232.dp), RoundedCornerShape(6.dp))
                        Spacer(Modifier.width(28.dp))
                        Column(Modifier.weight(1f)) {
                            Text(
                                "PLAYLIST",
                                style = MaterialTheme.typography.labelMedium.copy(letterSpacing = 1.3.sp),
                                color = BC.Secondary,
                            )
                            Spacer(Modifier.height(8.dp))
                            Text(
                                playlist.title,
                                style = MaterialTheme.typography.displayLarge,
                                color = BC.White,
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis,
                            )
                            playlist.author?.let { author ->
                                Spacer(Modifier.height(6.dp))
                                Text(
                                    author.name,
                                    style = MaterialTheme.typography.titleMedium,
                                    color = BC.Red,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                    modifier = Modifier.clickable(enabled = author.id != null) {
                                        author.id?.let(onOpenArtist)
                                    },
                                )
                            }
                            Spacer(Modifier.height(6.dp))
                            val meta = buildList {
                                playlist.songCountText?.let(::add)
                                val total = songs.sumOf { it.duration ?: 0 }
                                bcDuration(total).takeIf { it.isNotBlank() }?.let(::add)
                            }.joinToString(" - ")
                            if (meta.isNotBlank()) BcSecondary(meta)
                            Spacer(Modifier.height(20.dp))
                            BcSongHeaderActions(
                                songs = songs,
                                onPlayAll = onPlayAll,
                                onPlayNextAll = onPlayNextAll,
                                onQueueAll = onQueueAll,
                                onSaveAll = onSaveAll,
                                shareLink = shareLink ?: "https://music.youtube.com/playlist?list=$playlistId",
                            )
                        }
                    }
                }
                item(key = "playlist-divider") {
                    Box(Modifier.fillMaxWidth().height(1.dp).background(BC.Divider))
                }
                itemsIndexed(songs, key = { index, song -> "$index:${song.id}" }) { index, song ->
                    BcSongRow(
                        song = song,
                        number = index + 1,
                        isCurrent = false,
                        onClick = { onPlaySong(song) },
                        onPlayNext = { onPlayNext(song) },
                        onAddToQueue = { onAddToQueue(song) },
                        onAddToPlaylist = { onAddToPlaylist(song) },
                    )
                }
            }
        }
    }
}

// ---------------------------------------------------------------------------
// Artist
// ---------------------------------------------------------------------------

@Composable
internal fun BcArtistPage(
    browseId: String,
    language: String,
    onOpenAlbum: (String) -> Unit,
    onOpenPlaylist: (String) -> Unit,
    onPlaySong: (SongItem) -> Unit,
    onPlayAll: (List<SongItem>, Int) -> Unit,
    onPlayNext: (SongItem) -> Unit,
    onAddToQueue: (SongItem) -> Unit,
    onAddToPlaylist: (SongItem) -> Unit,
) {
    var page by remember { mutableStateOf<ArtistPage?>(null) }
    var error by remember { mutableStateOf<String?>(null) }
    LaunchedEffect(browseId) {
        YouTube.artist(browseId).fold(
            onSuccess = { page = it },
            onFailure = { error = it.message },
        )
    }
    when {
        error != null -> BcError(error)
        page == null -> BcLoading()
        else -> {
            val artist = page!!.artist
            val sections = page!!.sections
            LazyColumn(Modifier.fillMaxSize().background(BC.Page)) {
                item(key = "artist-hero") {
                    Box(Modifier.fillMaxWidth().height(360.dp)) {
                        if (artist.thumbnail != null) {
                            AsyncImage(
                                model = artist.thumbnail,
                                contentDescription = null,
                                contentScale = ContentScale.Crop,
                                modifier = Modifier.fillMaxSize(),
                            )
                        }
                        Box(
                            Modifier.fillMaxSize().background(
                                Brush.verticalGradient(
                                    listOf(Color.Black.copy(alpha = 0.15f), BC.Page.copy(alpha = 0.55f), BC.Page),
                                ),
                            ),
                        )
                        Column(
                            Modifier.align(Alignment.BottomStart).padding(start = 24.dp, end = 24.dp, bottom = 22.dp),
                        ) {
                            Text(
                                artist.title,
                                style = MaterialTheme.typography.displayLarge,
                                color = BC.White,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                            val sub = page!!.monthlyListenerCount ?: page!!.subscriberCountText
                            if (!sub.isNullOrBlank()) {
                                Spacer(Modifier.height(4.dp))
                                BcSecondary(sub)
                            }
                        }
                    }
                }
                item(key = "artist-actions") {
                    val songs = sections.flatMap { it.items }.filterIsInstance<SongItem>()
                    Row(
                        Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 16.dp),
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                    ) {
                        BcSongHeaderActions(songs, onPlayAll)
                    }
                }
                sections.forEachIndexed { index, section ->
                    item(key = "artist-section-$index") {
                        Column(Modifier.padding(horizontal = 20.dp, vertical = 10.dp)) {
                            Text(
                                section.title,
                                style = MaterialTheme.typography.headlineMedium,
                                color = BC.White,
                            )
                            Spacer(Modifier.height(10.dp))
                            if (section.items.all { it is SongItem }) {
                                section.items.filterIsInstance<SongItem>().take(6).forEach { song ->
                                    BcSongRow(
                                        song = song,
                                        isCurrent = false,
                                        onClick = { onPlaySong(song) },
                                        onPlayNext = { onPlayNext(song) },
                                        onAddToQueue = { onAddToQueue(song) },
                                        onAddToPlaylist = { onAddToPlaylist(song) },
                                    )
                                }
                            } else {
                                LazyRow(horizontalArrangement = Arrangement.spacedBy(18.dp)) {
                                    items(section.items, key = { it.id }) { item ->
                                        Column(
                                            Modifier.width(152.dp).clickable {
                                                when (item) {
                                                    is AlbumItem -> onOpenAlbum(item.browseId)
                                                    is PlaylistItem -> onOpenPlaylist(item.id)
                                                    is SongItem -> onPlaySong(item)
                                                    is ArtistItem -> Unit
                                                }
                                            },
                                        ) {
                                            BcArtwork(item.thumbnail, Modifier.size(152.dp), RoundedCornerShape(6.dp))
                                            Spacer(Modifier.height(7.dp))
                                            Text(
                                                item.title,
                                                style = MaterialTheme.typography.bodyMedium,
                                                color = BC.White,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis,
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
                item(key = "artist-bottom") { Spacer(Modifier.height(40.dp)) }
            }
        }
    }
}

// ---------------------------------------------------------------------------
// Local playlist
// ---------------------------------------------------------------------------

@Composable
internal fun BcLocalPlaylistPage(
    playlistId: String,
    language: String,
    player: PlayerController,
) {
    val all by PlaylistStore.all.collectAsState()
    val playlist = remember(all, playlistId) { all.firstOrNull { it.id == playlistId && !it.deleted } }
    if (playlist == null) {
        BcError("Playlist not found")
        return
    }
    val songs = playlist.songs
    LazyColumn(
        Modifier.fillMaxSize().background(BC.Page),
        contentPadding = PaddingValues(start = 16.dp, end = 18.dp, top = 22.dp, bottom = 40.dp),
    ) {
        item(key = "local-header") {
            Row(Modifier.fillMaxWidth().padding(bottom = 30.dp), verticalAlignment = Alignment.Bottom) {
                BcArtwork(songs.firstOrNull()?.thumbnail, Modifier.size(232.dp), RoundedCornerShape(6.dp))
                Spacer(Modifier.width(28.dp))
                Column(Modifier.weight(1f)) {
                    Text(
                        "PLAYLIST",
                        style = MaterialTheme.typography.labelMedium.copy(letterSpacing = 1.3.sp),
                        color = BC.Secondary,
                    )
                    Spacer(Modifier.height(8.dp))
                    Text(
                        playlist.name.ifBlank { "Playlist" },
                        style = MaterialTheme.typography.displayLarge,
                        color = BC.White,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Spacer(Modifier.height(6.dp))
                    BcSecondary("${songs.size} songs")
                    Spacer(Modifier.height(20.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        BcActionButton(
                            "Play",
                            filled = true,
                            onClick = {
                                player.playAll(
                                    songs.map {
                                        NowPlaying(
                                            videoId = it.id,
                                            title = it.title,
                                            artist = it.artist,
                                            thumbnail = it.thumbnail,
                                        )
                                    },
                                    0,
                                )
                            },
                            icon = Icons.Rounded.PlayArrow,
                        )
                        BcActionButton("", filled = false, onClick = { PlaylistStore.delete(playlist.id) }, icon = Icons.Rounded.MoreHoriz, round = true)
                    }
                }
            }
        }
        item(key = "local-divider") { Box(Modifier.fillMaxWidth().height(1.dp).background(BC.Divider)) }
        itemsIndexed(songs, key = { index, song -> "$index:${song.id}" }) { index, song ->
            BcHover(
                onClick = {
                    player.playAll(
                        songs.map {
                            NowPlaying(
                                videoId = it.id,
                                title = it.title,
                                artist = it.artist,
                                thumbnail = it.thumbnail,
                            )
                        },
                        index,
                    )
                },
                modifier = Modifier.fillMaxWidth().height(56.dp),
                shape = RoundedCornerShape(6.dp),
                hoverFill = Color.White.copy(alpha = 0.06f),
            ) {
                Row(Modifier.fillMaxSize().padding(horizontal = 6.dp), verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        "${index + 1}",
                        style = MaterialTheme.typography.bodySmall,
                        color = BC.Secondary,
                        modifier = Modifier.width(28.dp),
                    )
                    BcArtwork(song.thumbnail, Modifier.size(40.dp), RoundedCornerShape(3.dp))
                    Spacer(Modifier.width(12.dp))
                    Column(Modifier.weight(1f)) {
                        Text(song.title, style = MaterialTheme.typography.bodyLarge, color = BC.White, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        Text(song.artist, style = MaterialTheme.typography.bodySmall, color = BC.Secondary, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    }
                }
            }
        }
        if (songs.isEmpty()) {
            item {
                Text(
                    "This playlist is empty",
                    style = MaterialTheme.typography.bodyMedium,
                    color = BC.Secondary,
                    modifier = Modifier.padding(vertical = 30.dp),
                )
            }
        }
    }
}

// ---------------------------------------------------------------------------
// The song row and its context menu
// ---------------------------------------------------------------------------

@Composable
internal fun BcSongRow(
    song: SongItem,
    isCurrent: Boolean,
    onClick: () -> Unit,
    onAddToQueue: () -> Unit,
    onAddToPlaylist: () -> Unit,
    number: Int? = null,
    onPlayNext: (() -> Unit)? = null,
) {
    var menu by remember { mutableStateOf(false) }
    BcHover(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth().height(54.dp),
        shape = RoundedCornerShape(6.dp),
        hoverFill = Color.White.copy(alpha = 0.06f),
    ) {
        Row(Modifier.fillMaxSize().padding(horizontal = 6.dp), verticalAlignment = Alignment.CenterVertically) {
            if (number != null) {
                Text(
                    "$number",
                    style = MaterialTheme.typography.bodySmall,
                    color = if (isCurrent) BC.Red else BC.Secondary,
                    modifier = Modifier.width(30.dp),
                )
            } else {
                BcArtwork(song.thumbnail, Modifier.size(40.dp), RoundedCornerShape(3.dp))
                Spacer(Modifier.width(12.dp))
            }
            Column(Modifier.weight(1f)) {
                Text(
                    song.title,
                    style = MaterialTheme.typography.bodyLarge,
                    color = if (isCurrent) BC.Red else BC.White,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    song.artists.joinToString(", ") { it.name },
                    style = MaterialTheme.typography.bodySmall,
                    color = BC.Secondary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            Text(
                bcDuration(song.duration),
                style = MaterialTheme.typography.bodySmall,
                color = BC.Secondary,
                modifier = Modifier.padding(horizontal = 10.dp),
            )
            Box {
                BcIconButton(onClick = { menu = true }, size = 30) {
                    Icon(Icons.Rounded.MoreHoriz, "More", tint = BC.Secondary, modifier = Modifier.size(19.dp))
                }
                BcSongMenu(
                    expanded = menu,
                    onDismiss = { menu = false },
                    onPlay = { menu = false; onClick() },
                    onPlayNext = onPlayNext?.let { next -> { menu = false; next() } },
                    onAddToQueue = { menu = false; onAddToQueue() },
                    onAddToPlaylist = { menu = false; onAddToPlaylist() },
                    onShare = {
                        menu = false
                        copyToClipboard(song.shareLink)
                        DesktopSnackbar.show("Link copied")
                    },
                    onDownload = { BitDownloads.song(song) },
                )
            }
        }
    }
}

@Composable
internal fun BcSongMenu(
    expanded: Boolean,
    onDismiss: () -> Unit,
    onPlay: () -> Unit,
    onPlayNext: (() -> Unit)?,
    onAddToQueue: () -> Unit,
    onAddToPlaylist: () -> Unit,
    onShare: () -> Unit,
    onDownload: () -> Unit,
) {
    DropdownMenu(
        expanded = expanded,
        onDismissRequest = onDismiss,
        modifier = Modifier.background(BC.Chrome),
    ) {
        DropdownMenuItem(
            text = { Text("Play", color = BC.White) },
            leadingIcon = { Icon(Icons.Rounded.PlayArrow, null, tint = BC.Secondary, modifier = Modifier.size(18.dp)) },
            onClick = onPlay,
        )
        if (onPlayNext != null) {
            DropdownMenuItem(
                text = { Text("Play next", color = BC.White) },
                leadingIcon = { Icon(Icons.Rounded.Add, null, tint = BC.Secondary, modifier = Modifier.size(18.dp)) },
                onClick = onPlayNext,
            )
        }
        DropdownMenuItem(
            text = { Text("Add to queue", color = BC.White) },
            leadingIcon = { Icon(Icons.Rounded.Add, null, tint = BC.Secondary, modifier = Modifier.size(18.dp)) },
            onClick = onAddToQueue,
        )
        DropdownMenuItem(
            text = { Text("Add to playlist", color = BC.White) },
            leadingIcon = { Icon(Icons.Rounded.PlaylistAdd, null, tint = BC.Secondary, modifier = Modifier.size(18.dp)) },
            onClick = onAddToPlaylist,
        )
        DropdownMenuItem(
            text = { Text("Download", color = BC.White) },
            leadingIcon = { Icon(Icons.Rounded.Download, null, tint = BC.Secondary, modifier = Modifier.size(18.dp)) },
            onClick = { onDismiss(); onDownload() },
        )
        DropdownMenuItem(
            text = { Text("Share", color = BC.White) },
            leadingIcon = { Icon(Icons.Rounded.Share, null, tint = BC.Secondary, modifier = Modifier.size(18.dp)) },
            onClick = onShare,
        )
    }
}
