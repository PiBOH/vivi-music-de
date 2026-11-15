package com.music.vivi.desktop

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
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
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Album
import androidx.compose.material.icons.rounded.ArrowForwardIos
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Download
import androidx.compose.material.icons.rounded.Folder
import androidx.compose.material.icons.rounded.GraphicEq
import androidx.compose.material.icons.rounded.GridView
import androidx.compose.material.icons.rounded.ViewList
import androidx.compose.material.icons.rounded.History
import androidx.compose.material.icons.rounded.MoreHoriz
import androidx.compose.material.icons.rounded.MusicNote
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material3.CircularProgressIndicator
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
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.music.innertube.YouTube
import com.music.innertube.models.AlbumItem
import com.music.innertube.models.ArtistItem
import com.music.innertube.models.PlaylistItem
import com.music.innertube.models.SongItem
import com.music.innertube.models.YTItem
import com.music.innertube.pages.BrowseResult
import com.music.innertube.pages.ExplorePage
import com.music.innertube.pages.HomePage
import com.music.innertube.pages.MoodAndGenres
import com.music.innertube.pages.SearchResult
import kotlinx.coroutines.async
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

// ---------------------------------------------------------------------------
// Shared page furniture
// ---------------------------------------------------------------------------

@Composable
internal fun BcPage(
    content: androidx.compose.foundation.lazy.LazyListScope.() -> Unit,
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize().background(BC.Page),
        // The bottom inset clears the floating mini player, which sits over the
        // page's last row instead of taking a row of its own.
        contentPadding = PaddingValues(start = 28.dp, end = 28.dp, top = 20.dp, bottom = 104.dp),
        content = content,
    )
}

@Composable
internal fun BcPageTitle(title: String, subtitle: String? = null) {
    Column(Modifier.fillMaxWidth().padding(bottom = 18.dp)) {
        Text(title, style = MaterialTheme.typography.headlineLarge, color = BC.White)
        if (subtitle != null) {
            Spacer(Modifier.height(4.dp))
            BcSecondary(subtitle)
        }
    }
}

@Composable
internal fun BcLoading() {
    // The reference lays grey stand-ins over the same metrics as the real
    // shelves, so nothing moves when the data lands. One shared sweep drives
    // every block: the blocks read it inside their draw pass, so a screenful
    // of placeholders does not recompose on each animation frame.
    val sweep = rememberInfiniteTransition(label = "bit-skeleton").animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1400, easing = LinearEasing),
        ),
        label = "bit-sweep",
    )
    Column(
        Modifier
            .fillMaxSize()
            .background(BC.Page)
            .padding(start = 16.dp, end = 18.dp, top = 22.dp),
    ) {
        BcSkeletonBlock(width = 180.dp, height = 34.dp, sweep = sweep.value)
        repeat(2) {
            Spacer(Modifier.height(26.dp))
            BcSkeletonBlock(width = 220.dp, height = 22.dp, sweep = sweep.value)
            Spacer(Modifier.height(14.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(18.dp)) {
                repeat(5) {
                    Column(Modifier.width(150.dp)) {
                        BcSkeletonBlock(width = 150.dp, height = 150.dp, sweep = sweep.value)
                        Spacer(Modifier.height(9.dp))
                        BcSkeletonBlock(width = 120.dp, height = 13.dp, sweep = sweep.value)
                        Spacer(Modifier.height(7.dp))
                        BcSkeletonBlock(width = 84.dp, height = 11.dp, sweep = sweep.value)
                    }
                }
            }
        }
    }
}

@Composable
private fun BcSkeletonBlock(width: androidx.compose.ui.unit.Dp, height: androidx.compose.ui.unit.Dp, sweep: Float) {
    Box(
        Modifier
            .size(width, height)
            .clip(RoundedCornerShape(6.dp))
            .drawWithCache {
                val band = size.width * 0.5f
                val start = -band + sweep * (size.width + band * 2f)
                val brush = Brush.horizontalGradient(
                    colors = listOf(BC.Card, BC.Hover, BC.Card),
                    startX = start,
                    endX = start + band,
                )
                onDrawBehind { drawRect(brush) }
            },
    )
}

@Composable
internal fun BcError(message: String?) {
    Box(Modifier.fillMaxSize().background(BC.Page).padding(32.dp), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text("Something went wrong", style = MaterialTheme.typography.titleMedium, color = BC.White)
            Spacer(Modifier.height(6.dp))
            Text(message ?: "Try again", style = MaterialTheme.typography.bodyMedium, color = BC.Secondary)
        }
    }
}

// ---------------------------------------------------------------------------
// Home
// ---------------------------------------------------------------------------

@Composable
internal fun BcHomePage(
    language: String,
    onOpenAlbum: (String) -> Unit,
    onOpenArtist: (String) -> Unit,
    onOpenPlaylist: (String) -> Unit,
    onOpenLocalPlaylist: (String) -> Unit,
    onOpenBrowse: (String, String?) -> Unit,
    onPlaySong: (SongItem) -> Unit,
    onPlayAll: (List<SongItem>, Int) -> Unit,
) {
    var home by remember { mutableStateOf<HomePage?>(null) }
    var error by remember { mutableStateOf<String?>(null) }
    var retried by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        YouTube.home().fold(
            onSuccess = { first ->
                val page = first.filteredContent()
                val sections = page.sections.toMutableList()
                val seen = sections.map { it.title }.toMutableSet()
                var token = page.continuation
                var fetched = 0
                home = page
                while (token != null && fetched < 3) {
                    val more = YouTube.home(continuation = token).getOrNull() ?: break
                    fetched++
                    more.filteredContent().sections.forEach { section ->
                        if (seen.add(section.title)) sections += section
                    }
                    token = more.continuation
                    home = page.copy(sections = sections.toList(), continuation = token)
                }
            },
            onFailure = { error = it.message },
        )
    }
    LaunchedEffect(home) {
        val page = home
        if (page != null && page.sections.isEmpty() && !retried) {
            retried = true
            delay(1200)
            home = null
            YouTube.home().onSuccess { home = it.filteredContent() }
        }
    }

    when {
        error != null && home == null -> BcError(error)
        home == null -> BcLoading()
        else -> {
            val sections = home!!.sections.filter { it.items.isNotEmpty() }
            if (sections.isEmpty()) {
                BcPage {
                    item {
                        Text("Nothing to show yet", style = MaterialTheme.typography.titleMedium, color = BC.Secondary)
                    }
                }
            } else {
                BcPage {
                    sections.forEachIndexed { index, section ->
                        item(key = "shelf-$index-${section.title}") {
                            BcShelf(
                                section = section,
                                onOpenAlbum = onOpenAlbum,
                                onOpenArtist = onOpenArtist,
                                onOpenPlaylist = onOpenPlaylist,
                                onOpenSong = onPlaySong,
                                onShowAll = {
                                    section.endpoint?.let { onOpenBrowse(it.browseId, it.params) }
                                },
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun BcShelf(
    section: HomePage.Section,
    onOpenAlbum: (String) -> Unit,
    onOpenArtist: (String) -> Unit,
    onOpenPlaylist: (String) -> Unit,
    onOpenSong: (SongItem) -> Unit,
    onShowAll: () -> Unit,
) {
    Column(Modifier.fillMaxWidth().padding(bottom = 44.dp)) {
        Text(
            section.title,
            style = MaterialTheme.typography.headlineMedium,
            color = BC.White,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.fillMaxWidth(),
        )
        Spacer(Modifier.height(14.dp))
        LazyRow(horizontalArrangement = Arrangement.spacedBy(15.dp)) {
            items(section.items, key = { "${it.id}:${it.title}" }) { item ->
                BcShelfCard(
                    item = item,
                    onClick = {
                        when (item) {
                            is AlbumItem -> onOpenAlbum(item.browseId)
                            is ArtistItem -> onOpenArtist(item.id)
                            is PlaylistItem -> onOpenPlaylist(item.id)
                            is SongItem -> onOpenSong(item)
                        }
                    },
                )
            }
        }
    }
}

@Composable
private fun BcShelfCard(item: YTItem, onClick: () -> Unit) {
    Column(
        Modifier
            .width(150.dp)
            .bcHoverLift(RoundedCornerShape(6.dp))
            .clickable(onClick = onClick),
    ) {
        BcArtwork(item.thumbnail, Modifier.size(150.dp), RoundedCornerShape(6.dp))
        Spacer(Modifier.height(12.dp))
        Text(
            item.title,
            style = MaterialTheme.typography.titleMedium,
            color = BC.White,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
        )
        val subtitle = when (item) {
            is SongItem -> item.artists.joinToString(", ") { it.name }
            is AlbumItem -> item.artists?.joinToString(", ") { it.name } ?: item.year?.toString().orEmpty()
            is PlaylistItem -> item.songCountText ?: item.author?.name.orEmpty()
            is ArtistItem -> item.subtext ?: "Artist"
            else -> ""
        }
        if (subtitle.isNotBlank()) {
            Spacer(Modifier.height(3.dp))
            Text(
                subtitle,
                style = MaterialTheme.typography.bodySmall.copy(fontSize = 13.sp),
                color = BC.Secondary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

// ---------------------------------------------------------------------------
// Explore
// ---------------------------------------------------------------------------

@Composable
internal fun BcExplorePage(
    language: String,
    onOpenAlbum: (String) -> Unit,
    onOpenPlaylist: (String) -> Unit,
    onOpenBrowse: (String, String?) -> Unit,
) {
    var explore by remember { mutableStateOf<ExplorePage?>(null) }
    var error by remember { mutableStateOf<String?>(null) }
    var moods by remember { mutableStateOf<List<MoodAndGenres>>(emptyList()) }

    LaunchedEffect(Unit) {
        val moodsJob = async {
            YouTube.moodAndGenres().getOrNull().orEmpty()
        }
        YouTube.explore().fold(
            onSuccess = {
                explore = it
                if (it.moodAndGenres.isNotEmpty()) moods = listOf(MoodAndGenres("Moods & genres", it.moodAndGenres))
            },
            onFailure = { error = it.message },
        )
        val fromMoods = moodsJob.await()
        if (fromMoods.isNotEmpty()) moods = fromMoods
    }

    // The reference gives every mood tile the artwork of its own browse page
    // instead of a flat colour. Resolved here once per mood, four requests at a
    // time, and kept for the session.
    val moodArt = remember { androidx.compose.runtime.mutableStateMapOf<String, String>() }
    val usedArt = remember { mutableSetOf<String>() }
    LaunchedEffect(moods) {
        val pending = moods.flatMap { it.items }.distinctBy { it.title }
        pending.chunked(4).forEach { batch ->
            kotlinx.coroutines.coroutineScope {
                batch.forEach { mood ->
                    launch {
                        // The mood's own browse page answers with one generic
                        // shelf for every mood on this backend, which is why the
                        // tiles all showed the same cover. The reference shows a
                        // playlist cover per mood; searching the mood name for a
                        // playlist gives the same kind of picture and one that
                        // matches the label.
                        val page = YouTube
                            .search(mood.title, YouTube.SearchFilter.FILTER_FEATURED_PLAYLIST)
                            .getOrNull()
                        val candidates = page?.items.orEmpty()
                            .mapNotNull { item -> item.thumbnail?.takeIf { it.isNotBlank() } }
                        val thumb = candidates.firstOrNull { it !in usedArt } ?: candidates.firstOrNull()
                        if (!thumb.isNullOrBlank()) {
                            usedArt.add(thumb)
                            moodArt[mood.title] = thumb
                        }
                    }
                }
            }
        }
    }

    when {
        error != null && explore == null -> BcError(error)
        explore == null -> BcLoading()
        else -> {
            val releases = explore!!.newReleaseAlbums
            BcPage {
                // The reference's Explore is one grid of mood and genre tiles,
                // four to a row, with the label in the tile. There is no page
                // title and no second shelf, so neither is drawn here.
                val tiles = moods.flatMap { it.items }
                if (tiles.isEmpty()) {
                    itemsIndexed(releases.chunked(4), key = { index, _ -> "explore-$index" }) { _, row ->
                        Row(Modifier.fillMaxWidth().padding(bottom = 12.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            row.forEach { album ->
                                Box(
                                    Modifier
                                        .weight(1f)
                                        .height(150.dp)
                                        .clip(RoundedCornerShape(8.dp))
                                        .clickable { onOpenAlbum(album.browseId) },
                                ) {
                                    BcArtwork(album.thumbnail, Modifier.fillMaxSize(), RoundedCornerShape(8.dp))
                                    Text(
                                        album.title,
                                        style = MaterialTheme.typography.titleMedium,
                                        color = BC.White,
                                        maxLines = 2,
                                        overflow = TextOverflow.Ellipsis,
                                        modifier = Modifier.align(Alignment.TopStart).padding(16.dp),
                                    )
                                }
                            }
                            repeat(4 - row.size) { Spacer(Modifier.weight(1f)) }
                        }
                    }
                } else {
                    itemsIndexed(tiles.chunked(4), key = { index, _ -> "moods-$index" }) { _, row ->
                        Row(Modifier.fillMaxWidth().padding(bottom = 12.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            row.forEach { mood ->
                                val color = Color(0xFF000000 or (mood.stripeColor and 0xFFFFFF))
                                val art = moodArt[mood.title]
                                // Measured on the reference: the tile is 233 x 133 px at
                                // 1220 px wide, 12 px between tiles and between rows. The
                                // stripe colour fills the tile, the label sits on it at the
                                // top left, and the artwork takes the right side with no
                                // dark overlay of any kind.
                                Box(
                                    Modifier
                                        .weight(1f)
                                        .height(133.dp)
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(color)
                                        .clickable { onOpenBrowse(mood.endpoint.browseId, mood.endpoint.params) },
                                ) {
                                    if (art != null) {
                                        BcArtwork(
                                            art,
                                            Modifier
                                                .align(Alignment.CenterEnd)
                                                .fillMaxHeight()
                                                .fillMaxWidth(0.72f),
                                            RoundedCornerShape(8.dp),
                                        )
                                    }
                                    Text(
                                        mood.title,
                                        style = MaterialTheme.typography.titleMedium.copy(fontSize = 16.sp, fontWeight = FontWeight.Bold),
                                        color = BC.White,
                                        maxLines = 2,
                                        overflow = TextOverflow.Ellipsis,
                                        modifier = Modifier.align(Alignment.TopStart).padding(14.dp),
                                    )
                                }
                            }
                            repeat(4 - row.size) { Spacer(Modifier.weight(1f)) }
                        }
                    }
                }
            }
        }
    }
}

// ---------------------------------------------------------------------------
// Library
// ---------------------------------------------------------------------------

@Composable
internal fun BcLibraryPage(
    language: String,
    onOpenAlbum: (String) -> Unit,
    onOpenArtist: (String) -> Unit,
    onOpenPlaylist: (String) -> Unit,
    onOpenLocalPlaylist: (String) -> Unit,
    onOpenDownloads: () -> Unit,
    onOpenLocalMusic: () -> Unit,
    onPlaySong: (SongItem) -> Unit,
) {
    val history by HistoryStore.tracks.collectAsState()
    val stored by PlaylistStore.all.collectAsState()
    val playlists = remember(stored) { stored.filter { !it.deleted } }

    val minutes = remember(history) { (history.sumOf { it.durationMs } / 60_000L).toInt() }
    val topArtist = remember(history) { history.groupingBy { it.artist }.eachCount().maxByOrNull { it.value } }
    val topSong = history.firstOrNull()
    val cover = history.firstOrNull { it.thumbnail != null }?.thumbnail
    val firstPlaylist = playlists.firstOrNull()?.id

    BcPage {
        item(key = "library-stats") {
            LazyRow(
                modifier = Modifier.padding(bottom = 26.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                item(key = "stat-minutes") {
                    BcStatCard(
                        modifier = Modifier.width(300.dp),
                        header = "YOUR LISTENING",
                        value = "$minutes",
                        label = "MINUTES LISTENED",
                        footerLine1 = topArtist?.key ?: "No artist yet",
                        footerLine2 = "${topArtist?.value ?: 0} plays - All time",
                        cover = cover,
                        day = null,
                    )
                }
                item(key = "stat-artist") {
                    BcStatCard(
                        modifier = Modifier.width(300.dp),
                        header = "YOUR LISTENING",
                        value = topArtist?.key ?: "-",
                        label = "TOP ARTIST",
                        footerLine1 = topSong?.artist ?: "",
                        footerLine2 = "${topArtist?.value ?: 0} mins - ${topArtist?.value ?: 0} plays",
                        cover = cover,
                        day = null,
                    )
                }
                item(key = "stat-song") {
                    BcStatCard(
                        modifier = Modifier.width(300.dp),
                        header = "YOUR LISTENING",
                        value = topSong?.title ?: "-",
                        label = "TOP SONG",
                        footerLine1 = topSong?.artist ?: "",
                        footerLine2 = "${topSong?.plays ?: 0} plays",
                        cover = topSong?.thumbnail,
                        day = null,
                    )
                }
                item(key = "stat-album") {
                    BcStatCard(
                        modifier = Modifier.width(300.dp),
                        header = "YOUR LISTENING",
                        value = topSong?.album ?: "-",
                        label = "TOP ALBUM",
                        footerLine1 = topSong?.artist ?: "",
                        footerLine2 = "${topSong?.plays ?: 0} plays",
                        cover = topSong?.thumbnail,
                        day = null,
                    )
                }
            }
        }
        item(key = "library-downloads") {
            BcNavTile(Icons.Rounded.Download, "Downloads", onOpenDownloads)
        }
        item(key = "library-local") {
            BcNavTile(Icons.Rounded.Folder, "Local Music", onOpenLocalMusic)
        }
        item(key = "library-on-device") {
            BcSectionTitle("Playlist", Modifier.padding(top = 26.dp, bottom = 12.dp)) {
                Text(
                    "Show all",
                    style = MaterialTheme.typography.bodySmall.copy(fontSize = 13.sp),
                    color = BC.Secondary,
                    modifier = Modifier
                        .clip(RoundedCornerShape(6.dp))
                        .clickable { firstPlaylist?.let(onOpenLocalPlaylist) }
                        .padding(horizontal = 6.dp, vertical = 4.dp),
                )
            }
        }
        item(key = "library-playlist-row") {
            LazyRow(horizontalArrangement = Arrangement.spacedBy(18.dp), contentPadding = PaddingValues(bottom = 24.dp)) {
                item(key = "new-playlist") {
                    Box(
                        Modifier
                            .size(152.dp)
                            .clip(RoundedCornerShape(6.dp))
                            .background(Color.White.copy(alpha = 0.05f))
                            .clickable {
                                val created = PlaylistStore.create("My Playlist")
                                onOpenLocalPlaylist(created.id)
                            },
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(Icons.Rounded.Add, "New playlist", tint = BC.Secondary, modifier = Modifier.size(34.dp))
                    }
                }
                items(playlists, key = { it.id }) { playlist ->
                    Column(Modifier.width(152.dp).clickable { onOpenLocalPlaylist(playlist.id) }) {
                        BcArtwork(
                            playlist.songs.firstOrNull()?.thumbnail,
                            Modifier.size(152.dp),
                            RoundedCornerShape(6.dp),
                        )
                        Spacer(Modifier.height(7.dp))
                        Text(
                            playlist.name,
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

@Composable
private fun BcStatCard(
    modifier: Modifier,
    header: String,
    value: String,
    label: String,
    footerLine1: String,
    footerLine2: String,
    cover: String?,
    day: String?,
) {
    // Measured on the reference at 1220 x 780: the card is 300 px wide and 189
    // px tall with a 16 px inset, its tint comes from the artwork (the purple
    // card runs from rgb(72, 15, 65) to rgb(45, 12, 42)), the big figure is 28 px
    // and a text value is 21 px.
    val tint = rememberArtworkTint(cover) ?: Color(0xFF3A3A3D)
    val numeric = value.isNotEmpty() && value.all { it.isDigit() || it == '.' || it == ' ' }
    val bottom = tint.copy(red = tint.red * 0.62f, green = tint.green * 0.62f, blue = tint.blue * 0.62f)
    Box(
        modifier
            .height(189.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(Brush.verticalGradient(listOf(tint, bottom)))
            .padding(16.dp),
    ) {
        Column(Modifier.fillMaxSize()) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    header,
                    style = MaterialTheme.typography.labelSmall.copy(letterSpacing = 1.sp),
                    color = BC.White.copy(alpha = 0.9f),
                    modifier = Modifier.weight(1f),
                )
                Icon(Icons.Rounded.GraphicEq, null, tint = BC.White, modifier = Modifier.size(18.dp))
            }
            Spacer(Modifier.height(30.dp))
            Text(
                value,
                style = MaterialTheme.typography.headlineLarge.copy(
                    fontSize = if (numeric) 28.sp else 21.sp,
                    lineHeight = if (numeric) 32.sp else 25.sp,
                ),
                color = BC.White,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
            Spacer(Modifier.height(6.dp))
            Text(label, style = MaterialTheme.typography.labelSmall.copy(letterSpacing = 1.sp), color = BC.White.copy(alpha = 0.7f))
            Spacer(Modifier.weight(1f))
            Row(verticalAlignment = Alignment.Bottom) {
                Column(Modifier.weight(1f)) {
                    Text(
                        footerLine1,
                        style = MaterialTheme.typography.labelSmall,
                        color = BC.White,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Text(
                        footerLine2,
                        style = MaterialTheme.typography.labelSmall,
                        color = BC.White.copy(alpha = 0.6f),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
                if (day != null) {
                    Text(day, style = MaterialTheme.typography.labelSmall, color = BC.White.copy(alpha = 0.7f))
                }
            }
        }
    }
}

@Composable
private fun BcNavTile(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    onClick: () -> Unit,
) {
    Row(
        Modifier
            .fillMaxWidth()
            .height(48.dp)
            .clip(RoundedCornerShape(7.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(icon, label, tint = BC.White, modifier = Modifier.size(20.dp))
        Spacer(Modifier.width(14.dp))
        Text(label, style = MaterialTheme.typography.titleMedium, color = BC.White, modifier = Modifier.weight(1f))
        Icon(Icons.Rounded.ArrowForwardIos, null, tint = BC.Secondary, modifier = Modifier.size(13.dp))
    }
}

// ---------------------------------------------------------------------------
// Search
// ---------------------------------------------------------------------------

@Composable
internal fun BcSearchPage(
    language: String,
    query: String,
    onQueryChange: (String) -> Unit,
    focusTick: Int,
    onOpenAlbum: (String) -> Unit,
    onOpenArtist: (String) -> Unit,
    onOpenPlaylist: (String) -> Unit,
    onPlaySong: (SongItem) -> Unit,
    onAddToQueue: (SongItem) -> Unit,
    onAddToPlaylist: (SongItem) -> Unit,
) {
    var results by remember { mutableStateOf<SearchResult?>(null) }
    var loading by remember { mutableStateOf(false) }
    var filter by remember { mutableStateOf<YouTube.SearchFilter?>(YouTube.SearchFilter.FILTER_SONG) }
    val focus = remember { FocusRequester() }
    var submitted by remember { mutableStateOf("") }
    val recent by BitSearchHistory.recent.collectAsState()

    fun runSearch(term: String, nextFilter: YouTube.SearchFilter?) {
        if (term.isBlank()) {
            results = null
            return
        }
        submitted = term
        loading = true
        results = null
    }

    LaunchedEffect(submitted, filter) {
        val term = submitted
        if (term.isBlank()) return@LaunchedEffect
        loading = true
        YouTube.search(term, filter ?: YouTube.SearchFilter.FILTER_SONG).fold(
            onSuccess = {
                results = it
                loading = false
            },
            onFailure = {
                results = SearchResult(emptyList())
                loading = false
            },
        )
    }
    LaunchedEffect(focusTick) {
        if (focusTick > 0) {
            // A submit from the rail field reaches this page by opening it, so the
            // query is already filled and no search has run yet: run it here, the
            // same way the page's own Enter does.
            runSearch(query, filter)
        }
    }

    // The reference's Search page has no field of its own: the rail field is the
    // one input, and the page starts at "Recent searches".
    Column(Modifier.fillMaxSize().background(BC.Page).padding(start = 28.dp, end = 28.dp, top = 22.dp)) {
        when {
            query.isBlank() -> BcSearchRecent(
                recent = recent,
                onOpen = { entry ->
                    onQueryChange(entry.title)
                    runSearch(entry.title, filter)
                },
                onRemove = { entry -> BitSearchHistory.remove(entry.id) },
                onClear = { BitSearchHistory.clear() },
            )
            loading -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = BC.White, strokeWidth = 2.dp, modifier = Modifier.size(22.dp))
            }
            results != null -> LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(bottom = 104.dp)) {
                // Read through the nullable state, not with `!!`: a search can
                                // clear `results` while this lazy scope is being invalidated,
                                // and the old read crashed with a NullPointerException.
                val items = results?.items.orEmpty()
                if (items.isEmpty()) {
                    item {
                        Box(Modifier.fillMaxWidth().padding(top = 60.dp), contentAlignment = Alignment.Center) {
                            Text("No results", color = BC.Secondary, style = MaterialTheme.typography.titleMedium)
                        }
                    }
                }
                itemsIndexed(items, key = { index, item -> "$index:${item.id}" }) { _, item ->
                    when (item) {
                        is SongItem -> BcSongRow(
                            song = item,
                            isCurrent = false,
                            onClick = {
                                BitSearchHistory.record(item)
                                onPlaySong(item)
                            },
                            onAddToQueue = { onAddToQueue(item) },
                            onAddToPlaylist = { onAddToPlaylist(item) },
                        )
                        else -> BcYTItemRow(
                            item = item,
                            onOpenAlbum = onOpenAlbum,
                            onOpenArtist = onOpenArtist,
                            onOpenPlaylist = onOpenPlaylist,
                        )
                    }
                }
            }
            else -> BcSearchEmpty()
        }
    }
}

@Composable
private fun BcSearchEmpty() {
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Text("Search YouTube Music", color = BC.Secondary, style = MaterialTheme.typography.titleMedium)
    }
}

/**
 * The reference's "Recent searches" page: a 22 px heading with a Clear action on
 * the right, then one row per stored item (40 dp cover, title, artist and
 * duration, and a cross that removes that row).
 */
@Composable
private fun BcSearchRecent(
    recent: List<BitSearchEntry>,
    onOpen: (BitSearchEntry) -> Unit,
    onRemove: (BitSearchEntry) -> Unit,
    onClear: () -> Unit,
) {
    if (recent.isEmpty()) {
        BcSearchEmpty()
        return
    }
    LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(bottom = 104.dp)) {
        item(key = "recent-header") {
            Row(
                Modifier.fillMaxWidth().padding(bottom = 14.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    "Recent searches",
                    style = MaterialTheme.typography.headlineMedium,
                    color = BC.White,
                    modifier = Modifier.weight(1f),
                )
                Text(
                    "Clear",
                    style = MaterialTheme.typography.bodySmall.copy(fontSize = 13.sp),
                    color = BC.Secondary,
                    modifier = Modifier
                        .clip(RoundedCornerShape(6.dp))
                        .clickable(onClick = onClear)
                        .padding(horizontal = 8.dp, vertical = 4.dp),
                )
            }
        }
        items(recent, key = { "recent-${it.id}" }) { entry ->
            Row(
                Modifier.fillMaxWidth().height(56.dp).clickable { onOpen(entry) },
                verticalAlignment = Alignment.CenterVertically,
            ) {
                BcArtwork(entry.thumbnail ?: "", Modifier.size(40.dp), RoundedCornerShape(4.dp))
                Spacer(Modifier.width(12.dp))
                Column(Modifier.weight(1f)) {
                    Text(
                        entry.title,
                        style = MaterialTheme.typography.titleSmall.copy(fontSize = 15.sp),
                        color = BC.White,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    val meta = listOfNotNull(
                        entry.subtitle.takeIf { it.isNotBlank() },
                        bcDurationMs(entry.durationMs).takeIf { it.isNotBlank() },
                    ).joinToString(" - ")
                    if (meta.isNotBlank()) {
                        Text(
                            meta,
                            style = MaterialTheme.typography.bodySmall.copy(fontSize = 13.sp),
                            color = BC.Secondary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                }
                Spacer(Modifier.width(10.dp))
                Box(
                    Modifier.size(24.dp).clip(RoundedCornerShape(50)).clickable { onRemove(entry) },
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(Icons.Rounded.Close, "Remove", tint = BC.Secondary, modifier = Modifier.size(15.dp))
                }
            }
        }
    }
}

@Composable
private fun BcChip(label: String, selected: Boolean, onClick: () -> Unit) {
    Box(
        Modifier
            .clip(RoundedCornerShape(50))
            .background(if (selected) BC.White else Color.White.copy(alpha = 0.08f))
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 6.dp),
    ) {
        Text(
            label,
            style = MaterialTheme.typography.labelMedium,
            color = if (selected) Color.Black else BC.White.copy(alpha = 0.85f),
        )
    }
}

@Composable
internal fun BcYTItemRow(
    item: YTItem,
    onOpenAlbum: (String) -> Unit,
    onOpenArtist: (String) -> Unit,
    onOpenPlaylist: (String) -> Unit,
) {
    val subtitle = when (item) {
        is AlbumItem -> "Album - " + item.artists?.joinToString(", ") { it.name }.orEmpty()
        is ArtistItem -> item.subtext ?: "Artist"
        is PlaylistItem -> "Playlist - " + (item.songCountText ?: item.author?.name.orEmpty())
        is SongItem -> item.artists.joinToString(", ") { it.name }
        else -> ""
    }
    BcHover(
        onClick = {
            when (item) {
                is AlbumItem -> onOpenAlbum(item.browseId)
                is ArtistItem -> onOpenArtist(item.id)
                is PlaylistItem -> onOpenPlaylist(item.id)
                is SongItem -> Unit
                else -> Unit
            }
        },
        modifier = Modifier.fillMaxWidth().height(60.dp),
        shape = RoundedCornerShape(6.dp),
        hoverFill = Color.White.copy(alpha = 0.06f),
    ) {
        Row(Modifier.fillMaxSize().padding(horizontal = 6.dp), verticalAlignment = Alignment.CenterVertically) {
            BcArtwork(
                item.thumbnail,
                Modifier.size(44.dp),
                if (item is ArtistItem) CircleShape else RoundedCornerShape(4.dp),
            )
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(item.title, style = MaterialTheme.typography.bodyLarge, color = BC.White, maxLines = 1, overflow = TextOverflow.Ellipsis)
                if (subtitle.isNotBlank()) {
                    Text(subtitle, style = MaterialTheme.typography.bodySmall, color = BC.Secondary, maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
            }
        }
    }
}

// ---------------------------------------------------------------------------
// History
// ---------------------------------------------------------------------------

@Composable
internal fun BcHistoryPage(
    language: String,
    currentId: String?,
    onPlay: (HistoryEntry) -> Unit,
    onAddToQueue: (HistoryEntry) -> Unit,
) {
    val history by HistoryStore.tracks.collectAsState()
    BcPage {
        if (history.isEmpty()) {
            item {
                Box(Modifier.fillMaxWidth().padding(top = 60.dp), contentAlignment = Alignment.Center) {
                    Text("Nothing played yet", color = BC.Secondary, style = MaterialTheme.typography.titleMedium)
                }
            }
        }
        itemsIndexed(history, key = { _, entry -> entry.videoId }) { _, entry ->
            BcHover(
                onClick = { onPlay(entry) },
                modifier = Modifier.fillMaxWidth().height(56.dp),
                shape = RoundedCornerShape(6.dp),
                hoverFill = Color.White.copy(alpha = 0.06f),
            ) {
                Row(Modifier.fillMaxSize().padding(horizontal = 6.dp), verticalAlignment = Alignment.CenterVertically) {
                    BcArtwork(entry.thumbnail, Modifier.size(40.dp), RoundedCornerShape(3.dp))
                    Spacer(Modifier.width(12.dp))
                    Column(Modifier.weight(1f)) {
                        Text(
                            entry.title,
                            style = MaterialTheme.typography.bodyLarge,
                            color = if (entry.videoId == currentId) BC.Red else BC.White,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                        Text(entry.artist, style = MaterialTheme.typography.bodySmall, color = BC.Secondary, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    }
                    BcIconButton(onClick = { onAddToQueue(entry) }, size = 30) {
                        Icon(Icons.Rounded.Add, "Add to queue", tint = BC.Secondary, modifier = Modifier.size(18.dp))
                    }
                }
            }
        }
    }
}

// ---------------------------------------------------------------------------
// Downloads and local music
// ---------------------------------------------------------------------------

@Composable
internal fun BcDownloadsPage(language: String) {
    BcLocalLibraryPage(
        tabs = listOf("Songs", "Artists", "Albums"),
        emptyIcon = Icons.Rounded.Download,
        emptyTitle = "No downloads yet",
        emptyDescription = "Tracks saved for offline listening appear here.",
    )
}

@Composable
internal fun BcLocalMusicPage(language: String) {
    BcLocalLibraryPage(
        tabs = listOf("Songs", "Artists", "Albums"),
        emptyIcon = Icons.Rounded.Folder,
        emptyTitle = "No local files",
        emptyDescription = "Audio files found on this machine appear here.",
    )
}

/**
 * The reference's local-library frame, shared by Downloads and Local Music: a
 * "Search your music" field with the sort label and the grid/list toggle, then
 * the Songs / Artists / Albums tabs with the 2 dp white underline, a hairline
 * under the row and the content below it. The page draws no title of its own.
 */
@Composable
private fun BcLocalLibraryPage(
    tabs: List<String>,
    emptyIcon: androidx.compose.ui.graphics.vector.ImageVector,
    emptyTitle: String,
    emptyDescription: String,
) {
    var tab by remember { mutableStateOf(0) }
    var query by remember { mutableStateOf("") }
    var sortAsc by remember { mutableStateOf(true) }
    var grid by remember { mutableStateOf(false) }

    Column(Modifier.fillMaxSize().background(BC.Page).padding(start = 28.dp, end = 28.dp, top = 20.dp)) {
        Row(Modifier.fillMaxWidth().height(44.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(
                Modifier
                    .weight(1f)
                    .fillMaxHeight()
                    .clip(RoundedCornerShape(7.dp))
                    .background(BC.Search)
                    .border(1.dp, Color.White.copy(alpha = 0.12f), RoundedCornerShape(7.dp))
                    .padding(horizontal = 12.dp),
                contentAlignment = Alignment.CenterStart,
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Rounded.Search, null, tint = BC.Secondary, modifier = Modifier.size(17.dp))
                    Spacer(Modifier.width(9.dp))
                    BasicTextField(
                        value = query,
                        onValueChange = { query = it },
                        singleLine = true,
                        cursorBrush = SolidColor(BC.White),
                        textStyle = MaterialTheme.typography.bodyMedium.copy(color = BC.White),
                        modifier = Modifier.fillMaxWidth(),
                        decorationBox = { inner ->
                            Box {
                                if (query.isEmpty()) {
                                    Text(
                                        "Search your music",
                                        style = MaterialTheme.typography.bodySmall.copy(fontSize = 13.sp),
                                        color = BC.Secondary,
                                    )
                                }
                                inner()
                            }
                        },
                    )
                }
            }
            Spacer(Modifier.width(16.dp))
            Text(
                if (sortAsc) "A-Z" else "Z-A",
                style = MaterialTheme.typography.bodySmall.copy(fontSize = 13.sp),
                color = BC.Secondary,
                modifier = Modifier
                    .clip(RoundedCornerShape(6.dp))
                    .clickable { sortAsc = !sortAsc }
                    .padding(horizontal = 6.dp, vertical = 4.dp),
            )
            Spacer(Modifier.width(10.dp))
            Box(
                Modifier.size(24.dp).clip(RoundedCornerShape(6.dp)).clickable { grid = !grid },
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    if (grid) Icons.Rounded.ViewList else Icons.Rounded.GridView,
                    "Change view",
                    tint = BC.Secondary,
                    modifier = Modifier.size(18.dp),
                )
            }
        }
        Spacer(Modifier.height(16.dp))
        Row(Modifier.fillMaxWidth()) {
            tabs.forEachIndexed { index, label ->
                Column(
                    Modifier.weight(1f).clickable { tab = index },
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Text(
                        label,
                        style = MaterialTheme.typography.bodyMedium.copy(fontSize = 13.sp),
                        color = if (tab == index) BC.White else BC.Secondary,
                        modifier = Modifier.padding(vertical = 10.dp),
                    )
                    Box(
                        Modifier
                            .fillMaxWidth()
                            .height(2.dp)
                            .background(if (tab == index) BC.White else Color.Transparent),
                    )
                }
            }
        }
        Box(Modifier.fillMaxWidth().height(1.dp).background(BC.Divider))
        Spacer(Modifier.height(8.dp))
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.TopCenter) {
            Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.padding(top = 90.dp)) {
                Icon(emptyIcon, null, tint = BC.Secondary, modifier = Modifier.size(40.dp))
                Spacer(Modifier.height(12.dp))
                Text(emptyTitle, style = MaterialTheme.typography.titleMedium, color = BC.White)
                Spacer(Modifier.height(4.dp))
                Text(
                    emptyDescription,
                    style = MaterialTheme.typography.bodyMedium,
                    color = BC.Secondary,
                )
            }
        }
    }
}

// ---------------------------------------------------------------------------
// Browse (a mood, a genre, or Show all)
// ---------------------------------------------------------------------------

@Composable
internal fun BcBrowsePage(
    browseId: String,
    params: String?,
    language: String,
    onOpenAlbum: (String) -> Unit,
    onOpenArtist: (String) -> Unit,
    onOpenPlaylist: (String) -> Unit,
    onPlaySong: (SongItem) -> Unit,
) {
    var page by remember { mutableStateOf<BrowseResult?>(null) }
    var error by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(browseId, params) {
        YouTube.browse(browseId, params).fold(
            onSuccess = { page = it },
            onFailure = { error = it.message },
        )
    }
    when {
        error != null -> BcError(error)
        page == null -> BcLoading()
        else -> {
            val items = page!!.items.flatMap { it.items }
            BcPage {
                item(key = "browse-title") { BcPageTitle(page!!.title.orEmpty().ifBlank { "Browse" }) }
                itemsIndexed(items, key = { index, item -> "$index:${item.id}" }) { _, item ->
                    if (item is SongItem) {
                        BcSongRow(
                            song = item,
                            isCurrent = false,
                            onClick = { onPlaySong(item) },
                            onAddToQueue = {},
                            onAddToPlaylist = {},
                        )
                    } else {
                        BcYTItemRow(item, onOpenAlbum, onOpenArtist, onOpenPlaylist)
                    }
                }
            }
        }
    }
}

// Search results read through the nullable state; see BcSearchPage.
