package com.music.vivi.desktop

import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
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
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.PlaylistPlay
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Download
import androidx.compose.material.icons.rounded.Headphones
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material.icons.rounded.LibraryMusic
import androidx.compose.material.icons.rounded.MoreHoriz
import androidx.compose.material.icons.rounded.MusicNote
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.PlaylistAdd
import androidx.compose.material.icons.rounded.QueueMusic
import androidx.compose.material.icons.rounded.RemoveCircleOutline
import androidx.compose.material.icons.rounded.Repeat
import androidx.compose.material.icons.rounded.RepeatOne
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material.icons.rounded.SkipNext
import androidx.compose.material.icons.rounded.SkipPrevious
import androidx.compose.material.icons.rounded.VolumeUp
import androidx.compose.material3.CircularProgressIndicator
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
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.isAltPressed
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.music.innertube.models.AlbumItem
import com.music.innertube.models.ArtistItem
import com.music.innertube.models.PlaylistItem
import com.music.innertube.models.SongItem
import com.music.innertube.models.YTItem
import com.music.vivi.desktop.player.LoadPhase
import com.music.vivi.desktop.player.PlayerController
import com.music.vivi.desktop.player.PlayerState
import com.music.vivi.desktop.player.RepeatMode
import com.music.vivi.sync.SyncedPlaylist

/** The destinations of the Bit rail. */
internal sealed interface BcRoute {
    data object Home : BcRoute
    data object Explore : BcRoute
    data object Library : BcRoute
    data object Search : BcRoute
    data object History : BcRoute
    data object Downloads : BcRoute
    data object LocalMusic : BcRoute
    data object Settings : BcRoute
    data class Album(val browseId: String) : BcRoute
    data class Playlist(val playlistId: String) : BcRoute
    data class Artist(val browseId: String) : BcRoute
    data class Browse(val browseId: String, val params: String?) : BcRoute
    data class LocalPlaylist(val playlistId: String) : BcRoute
}

private enum class BcPanel { LYRICS, QUEUE }

private fun BcRoute.tab(): BcRoute = when (this) {
    BcRoute.Home, BcRoute.Explore, BcRoute.Library, BcRoute.Search,
    BcRoute.History, BcRoute.Downloads, BcRoute.LocalMusic, BcRoute.Settings,
    -> this
    else -> BcRoute.Home
}

/**
 * The cloned Bit window: a full-height 220 dp rail, a 64 dp transport bar
 * over the content column, and pages that scroll under both. Nothing here is
 * material: the three near-black steps, the hairlines and the 8 px grid are the
 * reference's, and the only accent is the Apple red the reference keeps for a
 * single badge.
 */
@Composable
internal fun BcRoot(
    player: PlayerController,
    language: String,
    initialSection: String?,
    isMaximized: Boolean,
    nativeTitleBar: Boolean,
    onMinimize: () -> Unit,
    onMaximize: () -> Unit,
    onClose: () -> Unit,
    dragArea: @Composable (Modifier, @Composable () -> Unit) -> Unit,
) {
    val state by player.uiState.collectAsState()
    // The Bit menus' Download entry needs the controller; attach it once here
    // instead of threading a lambda through every page.
    LaunchedEffect(player) { BitDownloads.attach(player) }
    val positionProvider: () -> Long = remember(player) { { player.positionMs.value } }

    var backStack by remember {
        mutableStateOf(
            listOf(
                when (initialSection) {
                    "explore" -> BcRoute.Explore
                    "library" -> BcRoute.Library
                    "search" -> BcRoute.Search
                    "history" -> BcRoute.History
                    "downloads" -> BcRoute.Downloads
                    "local" -> BcRoute.LocalMusic
                    "settings" -> BcRoute.Settings
                    else -> BcRoute.Home
                },
            ),
        )
    }
    val route = backStack.last()
    fun open(next: BcRoute) { backStack = backStack + next }
    fun goBack() { if (backStack.size > 1) backStack = backStack.dropLast(1) }

    var query by remember { mutableStateOf("") }
    var panel by remember { mutableStateOf<BcPanel?>(null) }
    var expanded by remember { mutableStateOf(false) }
    // Probe only, and never while the full player is open: the player's own
    // playhead already asks for the frame, and two awaiters would count two
    // frames per refresh.
    if (BitFramePacing.enabled && !expanded) {
        LaunchedEffect(Unit) {
            while (true) {
                androidx.compose.runtime.withFrameNanos { BitFramePacing.tick(it) }
            }
        }
    }
    var searchFocusTick by remember { mutableStateOf(0) }

    val localPlaylists by PlaylistStore.all.collectAsState()
    val activePlaylists = remember(localPlaylists) { localPlaylists.filter { !it.deleted } }

    val openAlbum: (String) -> Unit = { open(BcRoute.Album(it)) }
    val openArtist: (String) -> Unit = { open(BcRoute.Artist(it)) }
    val openPlaylist: (String) -> Unit = { open(BcRoute.Playlist(it)) }
    val openBrowse: (String, String?) -> Unit = { id, params -> open(BcRoute.Browse(id, params)) }
    val playSong: (SongItem) -> Unit = { player.play(it.toNowPlaying()) }
    val playAll: (List<SongItem>, Int) -> Unit = { songs, index ->
        player.playAll(songs.map { it.toNowPlaying() }, index.coerceAtLeast(0))
    }
    val addToQueue: (SongItem) -> Unit = { player.addToQueue(it.toNowPlaying()) }
    val playNext: (SongItem) -> Unit = { player.insertNext(it.toNowPlaying()) }
    val addToPlaylist: (SongItem) -> Unit = { song ->
        val target = PlaylistStore.active.firstOrNull()
        if (target != null) {
            PlaylistStore.addSongs(
                target.id,
                listOf(
                    com.music.vivi.sync.SyncedSong(
                        id = song.id,
                        title = song.title,
                        artist = song.artists.joinToString(", ") { it.name },
                        thumbnail = song.thumbnail,
                    ),
                ),
            )
            DesktopSnackbar.show("${song.title} -> ${target.name}")
        } else {
            val created = PlaylistStore.create("My Playlist")
            PlaylistStore.addSongs(
                created.id,
                listOf(
                    com.music.vivi.sync.SyncedSong(
                        id = song.id,
                        title = song.title,
                        artist = song.artists.joinToString(", ") { it.name },
                        thumbnail = song.thumbnail,
                    ),
                ),
            )
            DesktopSnackbar.show("${song.title} -> ${created.name}")
        }
    }

    val isMac = System.getProperty("os.name", "").lowercase().contains("mac")
    // The reference's key map, unchanged: the three media keys, Alt+Left for
    // back, Escape for one layer at a time (the player first, then a side
    // panel, then the page). Handled on preview so a focused text field does
    // not swallow them.
    val keys = Modifier.onPreviewKeyEvent { event ->
        if (event.type != KeyEventType.KeyUp) return@onPreviewKeyEvent false
        when (event.key) {
            Key.MediaPlayPause -> {
                player.toggle()
                true
            }
            Key.MediaNext -> {
                player.next()
                true
            }
            Key.MediaPrevious -> {
                player.previous()
                true
            }
            Key.DirectionLeft -> if (event.isAltPressed) {
                goBack()
                true
            } else {
                false
            }
            Key.Escape -> when {
                expanded -> {
                    expanded = false
                    true
                }
                panel != null -> {
                    panel = null
                    true
                }
                backStack.size > 1 -> {
                    goBack()
                    true
                }
                else -> false
            }
            else -> false
        }
    }
    BoxWithConstraints(
        Modifier
            .fillMaxSize()
            .background(BC.Page)
            .then(keys)
            // Passive pacing: the probe reports the frames Compose really
            // paints, it never asks for one.
            .drawWithContent {
                if (BitFramePacing.enabled) {
                    val started = System.nanoTime()
                    drawContent()
                    BitFramePacing.frame(System.nanoTime() - started)
                } else {
                    drawContent()
                }
            },
    ) {
        val compact = maxWidth < 980.dp
        Row(Modifier.fillMaxSize()) {
            if (!compact) {
            BcSidebar(
                route = route,
                query = query,
                onQueryChange = { query = it },
                onSubmit = {
                    if (query.isNotBlank()) {
                        open(BcRoute.Search)
                        searchFocusTick++
                    }
                },
                playlists = activePlaylists,
                openedPlaylistId = (route as? BcRoute.LocalPlaylist)?.playlistId,
                onOpen = { open(it.tab()) },
                onOpenPlaylist = { open(BcRoute.LocalPlaylist(it)) },
                isMaximized = isMaximized,
                onMinimize = onMinimize,
                onMaximize = onMaximize,
                onClose = onClose,
                dragArea = dragArea,
                drawWindowControls = isMac && !nativeTitleBar,
            )
            }
            Column(Modifier.weight(1f).fillMaxHeight()) {
                BcTopBar(
                    compact = compact,
                    player = player,
                    state = state,
                    language = language,
                    positionProvider = positionProvider,
                    canGoBack = backStack.size > 1,
                    onBack = ::goBack,
                    onOpenNowPlaying = { expanded = true },
                    onToggleLyrics = { panel = if (panel == BcPanel.LYRICS) null else BcPanel.LYRICS },
                    onToggleQueue = { panel = if (panel == BcPanel.QUEUE) null else BcPanel.QUEUE },
                    activePanel = panel,
                    dragArea = dragArea,
                    isMaximized = isMaximized,
                    drawWindowControls = !isMac && !nativeTitleBar,
                    onMinimize = onMinimize,
                    onMaximize = onMaximize,
                    onClose = onClose,
                )
                Row(Modifier.fillMaxWidth().weight(1f)) {
                    Box(Modifier.weight(1f).fillMaxHeight()) {
                        // The reference fades one surface into the next instead of
                        // swapping it in place.
                        Crossfade(
                            targetState = route,
                            animationSpec = tween(durationMillis = 180),
                            label = "bit-page",
                        ) { visibleRoute ->
                            when (visibleRoute) {
                            BcRoute.Home -> BcHomePage(
                                language = language,
                                onOpenAlbum = openAlbum,
                                onOpenArtist = openArtist,
                                onOpenPlaylist = openPlaylist,
                                onOpenLocalPlaylist = { open(BcRoute.LocalPlaylist(it)) },
                                onOpenBrowse = openBrowse,
                                onPlaySong = playSong,
                                onPlayAll = playAll,
                            )
                            BcRoute.Explore -> BcExplorePage(
                                language = language,
                                onOpenAlbum = openAlbum,
                                onOpenPlaylist = openPlaylist,
                                onOpenBrowse = openBrowse,
                            )
                            BcRoute.Library -> BcLibraryPage(
                                language = language,
                                onOpenAlbum = openAlbum,
                                onOpenArtist = openArtist,
                                onOpenPlaylist = openPlaylist,
                                onOpenLocalPlaylist = { open(BcRoute.LocalPlaylist(it)) },
                                onOpenDownloads = { open(BcRoute.Downloads) },
                                onOpenLocalMusic = { open(BcRoute.LocalMusic) },
                                onPlaySong = playSong,
                            )
                            BcRoute.Search -> BcSearchPage(
                                language = language,
                                query = query,
                                onQueryChange = { query = it },
                                focusTick = searchFocusTick,
                                onOpenAlbum = openAlbum,
                                onOpenArtist = openArtist,
                                onOpenPlaylist = openPlaylist,
                                onPlaySong = playSong,
                                onAddToQueue = addToQueue,
                                onAddToPlaylist = addToPlaylist,
                            )
                            BcRoute.History -> BcHistoryPage(
                                language = language,
                                currentId = state.current?.videoId,
                                onPlay = { history ->
                                    player.play(
                                        NowPlaying(
                                            videoId = history.videoId,
                                            title = history.title,
                                            artist = history.artist,
                                            thumbnail = history.thumbnail,
                                            durationMs = history.durationMs,
                                            album = history.album,
                                        ),
                                    )
                                },
                                onAddToQueue = { history ->
                                    player.addToQueue(
                                        NowPlaying(
                                            videoId = history.videoId,
                                            title = history.title,
                                            artist = history.artist,
                                            thumbnail = history.thumbnail,
                                            durationMs = history.durationMs,
                                            album = history.album,
                                        ),
                                    )
                                },
                            )
                            BcRoute.Downloads -> BcDownloadsPage(language = language)
                            BcRoute.LocalMusic -> BcLocalMusicPage(language = language)
                            BcRoute.Settings -> BcSettingsPage(language = language)
                            is BcRoute.Album -> BcAlbumPage(
                                browseId = visibleRoute.browseId,
                                language = language,
                                onOpenArtist = openArtist,
                                onPlaySong = playSong,
                                onPlayAll = playAll,
                                onPlayNext = playNext,
                                onAddToQueue = addToQueue,
                                onAddToPlaylist = addToPlaylist,
                                onPlayNextAll = { list -> list.asReversed().forEach { player.insertNext(it.toNowPlaying()) } },
                                onQueueAll = { list -> list.forEach { player.addToQueue(it.toNowPlaying()) } },
                                onSaveAll = { list -> list.forEach(addToPlaylist) },
                            )
                            is BcRoute.Playlist -> BcPlaylistPage(
                                playlistId = visibleRoute.playlistId,
                                language = language,
                                onOpenArtist = openArtist,
                                onPlaySong = playSong,
                                onPlayAll = playAll,
                                onPlayNext = playNext,
                                onAddToQueue = addToQueue,
                                onAddToPlaylist = addToPlaylist,
                                onPlayNextAll = { list -> list.asReversed().forEach { player.insertNext(it.toNowPlaying()) } },
                                onQueueAll = { list -> list.forEach { player.addToQueue(it.toNowPlaying()) } },
                                onSaveAll = { list -> list.forEach(addToPlaylist) },
                            )
                            is BcRoute.Artist -> BcArtistPage(
                                browseId = visibleRoute.browseId,
                                language = language,
                                onOpenAlbum = openAlbum,
                                onOpenPlaylist = openPlaylist,
                                onPlaySong = playSong,
                                onPlayAll = playAll,
                                onPlayNext = playNext,
                                onAddToQueue = addToQueue,
                                onAddToPlaylist = addToPlaylist,
                            )
                            is BcRoute.Browse -> BcBrowsePage(
                                browseId = visibleRoute.browseId,
                                params = visibleRoute.params,
                                language = language,
                                onOpenAlbum = openAlbum,
                                onOpenArtist = openArtist,
                                onOpenPlaylist = openPlaylist,
                                onPlaySong = playSong,
                            )
                            is BcRoute.LocalPlaylist -> BcLocalPlaylistPage(
                                playlistId = visibleRoute.playlistId,
                                language = language,
                                player = player,
                            )
                            }
                        }
                    }
                    panel?.let { active ->
                        BcSidePanel(
                            panel = active,
                            state = state,
                            positionProvider = positionProvider,
                            language = language,
                            onClose = { panel = null },
                            onSkipTo = player::skipTo,
                            onRemoveAt = player::removeAt,
                            onClear = player::clearQueue,
                            onSeek = player::seekTo,
                            onTogglePlay = player::toggle,
                        )
                    }
                }
                if (compact) {
                    BcBottomMini(
                        player = player,
                        state = state,
                        language = language,
                        positionProvider = positionProvider,
                        onOpen = { expanded = true },
                    )
                }
            }
        }
    }

    if (expanded) {
        BcNowPlayingOverlay(
            player = player,
            state = state,
            language = language,
            positionProvider = positionProvider,
            onClose = { expanded = false },
        )
    }
}

@Composable
private fun BcSidebar(
    route: BcRoute,
    query: String,
    onQueryChange: (String) -> Unit,
    onSubmit: () -> Unit,
    playlists: List<SyncedPlaylist>,
    openedPlaylistId: String?,
    onOpen: (BcRoute) -> Unit,
    onOpenPlaylist: (String) -> Unit,
    isMaximized: Boolean,
    onMinimize: () -> Unit,
    onMaximize: () -> Unit,
    onClose: () -> Unit,
    dragArea: @Composable (Modifier, @Composable () -> Unit) -> Unit,
    drawWindowControls: Boolean,
) {
    val focus = remember { FocusRequester() }
    Box(Modifier.width(208.dp).fillMaxHeight().background(BC.Chrome)) {
        Column(Modifier.fillMaxSize()) {
            dragArea(Modifier.fillMaxWidth().height(44.dp)) {
                Row(
                    Modifier.fillMaxSize().padding(start = 16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    if (drawWindowControls) BcWindowDots(isMaximized, onMinimize, onMaximize, onClose)
                }
            }
            Column(Modifier.fillMaxSize().padding(start = 12.dp, end = 12.dp, bottom = 16.dp)) {
                BcSidebarSearch(
                    value = query,
                    onValueChange = onQueryChange,
                    onSubmit = onSubmit,
                    focusRequester = focus,
                )
                Spacer(Modifier.height(18.dp))
                BcNavRow(Icons.Rounded.Home, "Home", route is BcRoute.Home) { onOpen(BcRoute.Home) }
                BcNavRow(Icons.Rounded.Search, "Explore", route is BcRoute.Explore) { onOpen(BcRoute.Explore) }
                BcNavRow(Icons.Rounded.LibraryMusic, "Library", route is BcRoute.Library) { onOpen(BcRoute.Library) }
                BcNavRow(Icons.Rounded.Search, "Search", route is BcRoute.Search) { onOpen(BcRoute.Search) }
                Spacer(Modifier.height(6.dp))
                BcDivider(Modifier.fillMaxWidth())
                Spacer(Modifier.height(12.dp))
                BcNavRow(Icons.Rounded.MusicNote, "History", route is BcRoute.History) { onOpen(BcRoute.History) }
                BcNavRow(Icons.Rounded.Download, "Downloads", route is BcRoute.Downloads) { onOpen(BcRoute.Downloads) }
                BcNavRow(Icons.Rounded.LibraryMusic, "Local Music", route is BcRoute.LocalMusic) { onOpen(BcRoute.LocalMusic) }
                Spacer(Modifier.height(6.dp))
                BcDivider(Modifier.fillMaxWidth())
                Spacer(Modifier.height(8.dp))
                LazyColumn(Modifier.weight(1f).fillMaxWidth(), contentPadding = PaddingValues(bottom = 8.dp)) {
                    itemsIndexed(playlists, key = { _, p -> p.id }) { _, playlist ->
                        BcNavRow(
                            Icons.AutoMirrored.Rounded.PlaylistPlay,
                            playlist.name.ifBlank { "Playlist" },
                            openedPlaylistId == playlist.id,
                        ) { onOpenPlaylist(playlist.id) }
                    }
                    if (playlists.isEmpty()) {
                        item {
                            Text(
                                "Your playlists will appear here",
                                style = MaterialTheme.typography.bodySmall,
                                color = BC.Secondary.copy(alpha = 0.68f),
                                modifier = Modifier.padding(horizontal = 9.dp, vertical = 8.dp),
                            )
                        }
                    }
                }
                BcDivider(Modifier.fillMaxWidth())
                Spacer(Modifier.height(8.dp))
                BcNavRow(Icons.Rounded.Settings, "Settings", route is BcRoute.Settings) { onOpen(BcRoute.Settings) }
            }
        }
        Box(Modifier.align(Alignment.CenterEnd).width(1.dp).fillMaxHeight().background(BC.Divider))
    }
}

@Composable
private fun BcNavRow(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
) {
    BcHover(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth().height(32.dp),
        shape = RoundedCornerShape(7.dp),
        hoverFill = Color.White.copy(alpha = 0.07f),
    ) {
        Row(
            Modifier
                .fillMaxSize()
                .background(if (selected) BC.RailSelected else Color.Transparent, RoundedCornerShape(7.dp))
                .padding(horizontal = 9.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(icon, label, tint = if (selected) BC.White else BC.Secondary, modifier = Modifier.size(17.dp))
            Spacer(Modifier.width(10.dp))
            Text(
                label,
                color = if (selected) BC.White else BC.Secondary,
                style = MaterialTheme.typography.bodyMedium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

@Composable
private fun BcSidebarSearch(
    value: String,
    onValueChange: (String) -> Unit,
    onSubmit: () -> Unit,
    focusRequester: FocusRequester,
) {
    val shape = RoundedCornerShape(7.dp)
    Box(
        Modifier
            .fillMaxWidth()
            .height(36.dp)
            .clip(shape)
            .background(BC.Search)
            .clickable { focusRequester.requestFocus() }
            .padding(horizontal = 9.dp),
        contentAlignment = Alignment.CenterStart,
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Rounded.Search, null, tint = BC.Secondary, modifier = Modifier.size(17.dp))
            Spacer(Modifier.width(8.dp))
            BasicTextField(
                value = value,
                onValueChange = onValueChange,
                singleLine = true,
                cursorBrush = SolidColor(BC.White),
                textStyle = MaterialTheme.typography.bodyMedium.copy(color = BC.White),
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                keyboardActions = KeyboardActions(onSearch = { onSubmit() }),
                modifier = Modifier
                    .fillMaxWidth()
                    .focusRequester(focusRequester)
                    .bcSubmitOnEnter(onSubmit),
                decorationBox = { inner ->
                    Box {
                        if (value.isEmpty()) {
                            Text("Search", style = MaterialTheme.typography.bodyMedium, color = BC.Secondary)
                        }
                        inner()
                    }
                },
            )
        }
    }
}

@Composable
private fun BcTopBar(
    compact: Boolean,
    player: PlayerController,
    state: PlayerState,
    language: String,
    positionProvider: () -> Long,
    canGoBack: Boolean,
    onBack: () -> Unit,
    onOpenNowPlaying: () -> Unit,
    onToggleLyrics: () -> Unit,
    onToggleQueue: () -> Unit,
    activePanel: BcPanel?,
    dragArea: @Composable (Modifier, @Composable () -> Unit) -> Unit,
    isMaximized: Boolean,
    drawWindowControls: Boolean,
    onMinimize: () -> Unit,
    onMaximize: () -> Unit,
    onClose: () -> Unit,
) {
    // The narrow layout folds the rail away and moves the transport to the
    // bottom bar, so the top strip keeps only the drag area (and the window
    // dots on a custom frame). Two rows of the same controls was the defect.
    if (compact) {
        Box(Modifier.fillMaxWidth().height(36.dp).background(BC.Chrome)) {
            dragArea(Modifier.fillMaxSize()) {
                Row(
                    Modifier.fillMaxSize().padding(horizontal = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    if (drawWindowControls) BcWindowDots(isMaximized, onMinimize, onMaximize, onClose)
                }
            }
        }
        return
    }
    Box(Modifier.fillMaxWidth().height(64.dp).background(BC.Chrome)) {
        dragArea(Modifier.fillMaxSize()) {
            Row(Modifier.fillMaxSize().padding(horizontal = 12.dp), verticalAlignment = Alignment.CenterVertically) {
                BcLogo()
                Spacer(Modifier.width(12.dp))
                BcIconButton(onClick = onBack, enabled = canGoBack, size = 30) {
                    Icon(
                        Icons.Rounded.ArrowBack,
                        "Back",
                        tint = if (canGoBack) BC.White else BC.Secondary.copy(alpha = 0.35f),
                        modifier = Modifier.size(19.dp),
                    )
                }
                Spacer(Modifier.width(4.dp))
                BcIconButton(onClick = player::toggleShuffle, size = 30) {
                    Icon(
                        BitIcons.Shuffle,
                        "Shuffle",
                        tint = if (state.isShuffle) BC.White else BC.Secondary,
                        modifier = Modifier.size(17.dp),
                    )
                }
                BcIconButton(onClick = player::previous, size = 30) {
                    Icon(Icons.Rounded.SkipPrevious, "Previous", tint = BC.White, modifier = Modifier.size(21.dp))
                }
                BcIconButton(onClick = player::toggle, size = 30) {
                    if (state.loadPhase != LoadPhase.NONE) {
                        CircularProgressIndicator(Modifier.size(16.dp), color = BC.White, strokeWidth = 2.dp)
                    } else {
                        Icon(
                            if (state.isPlaying) Icons.Rounded.Pause else Icons.Rounded.PlayArrow,
                            if (state.isPlaying) "Pause" else "Play",
                            tint = BC.White,
                            modifier = Modifier.size(23.dp),
                        )
                    }
                }
                BcIconButton(onClick = player::next, size = 30) {
                    Icon(Icons.Rounded.SkipNext, "Next", tint = BC.White, modifier = Modifier.size(21.dp))
                }
                BcIconButton(onClick = player::cycleRepeatMode, size = 30) {
                    Icon(
                        if (state.repeatMode == RepeatMode.ONE) Icons.Rounded.RepeatOne else Icons.Rounded.Repeat,
                        "Repeat",
                        tint = if (state.repeatMode != RepeatMode.OFF) BC.White else BC.Secondary,
                        modifier = Modifier.size(17.dp),
                    )
                }
                Spacer(Modifier.width(8.dp))
                Box(Modifier.weight(1f).padding(horizontal = 10.dp), contentAlignment = Alignment.Center) {
                    BcNowPill(
                        state = state,
                        positionProvider = positionProvider,
                        onOpen = onOpenNowPlaying,
                    )
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Rounded.VolumeUp, "Volume", tint = BC.Secondary, modifier = Modifier.size(17.dp))
                    Spacer(Modifier.width(6.dp))
                    BcThinSlider(
                        value = state.volume,
                        onValueChange = player::setVolume,
                        modifier = Modifier.width(72.dp),
                    )
                    Spacer(Modifier.width(4.dp))
                    BcIconButton(onClick = {}, size = 30) {
                        Icon(Icons.Rounded.Headphones, "Audio output", tint = BC.Secondary, modifier = Modifier.size(18.dp))
                    }
                    BcIconButton(onClick = onToggleLyrics, size = 30) {
                        Icon(
                            BitIcons.LyricsQuote,
                            "Lyrics",
                            tint = if (activePanel == BcPanel.LYRICS) BC.White else BC.Secondary,
                            modifier = Modifier.size(18.dp),
                        )
                    }
                    BcIconButton(onClick = onToggleQueue, size = 30) {
                        Icon(
                            Icons.Rounded.QueueMusic,
                            "Queue",
                            tint = if (activePanel == BcPanel.QUEUE) BC.White else BC.Secondary,
                            modifier = Modifier.size(20.dp),
                        )
                    }
                    BcIconButton(onClick = {}, size = 30) {
                        Icon(Icons.Rounded.MoreHoriz, "More", tint = BC.Secondary, modifier = Modifier.size(20.dp))
                    }
                    Spacer(Modifier.width(4.dp))
                    BcAvatar("V", 22)
                    if (drawWindowControls) {
                        Spacer(Modifier.width(10.dp))
                        BcWindowDots(isMaximized, onMinimize, onMaximize, onClose)
                        Spacer(Modifier.width(4.dp))
                    }
                }
            }
        }
        BcDivider(Modifier.align(Alignment.BottomCenter).fillMaxWidth())
    }
}

@Composable
private fun BcLogo() {
    val bitmap = remember { BrandLogo.composeBitmap(64) }
    if (bitmap != null) {
        androidx.compose.foundation.Image(
            bitmap = bitmap,
            contentDescription = "VIVI Music DE",
            modifier = Modifier.size(width = 26.dp, height = 24.dp),
        )
    } else {
        Text("V", style = MaterialTheme.typography.titleLarge, color = BC.White)
    }
}

@Composable
private fun BcNowPill(
    state: PlayerState,
    positionProvider: () -> Long,
    onOpen: () -> Unit,
) {
    val track = state.current
    val shape = RoundedCornerShape(4.dp)
    val style = androidx.compose.ui.graphics.Color(0xFF323236)
    BcHover(
        onClick = onOpen,
        modifier = Modifier.widthIn(min = 300.dp, max = 520.dp).fillMaxWidth().height(46.dp),
        shape = shape,
        hoverFill = Color.Transparent,
    ) {
        Box(
            Modifier
                .fillMaxSize()
                .clip(shape)
                .background(style)
                .then(
                    if (track != null) Modifier else Modifier,
                ),
        ) {
            if (track == null) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("Not playing", style = MaterialTheme.typography.labelSmall, color = BC.Secondary)
                }
            } else {
                Row(Modifier.fillMaxSize(), verticalAlignment = Alignment.CenterVertically) {
                    BcArtwork(track.thumbnail, Modifier.size(44.dp), RoundedCornerShape(3.dp))
                    Column(
                        Modifier.weight(1f).padding(start = 12.dp, end = 46.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                    ) {
                        Text(
                            track.title,
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.SemiBold,
                            color = BC.White,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                        Text(
                            track.artist,
                            style = MaterialTheme.typography.labelSmall,
                            color = BC.Secondary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                }
                Box(
                    Modifier
                        .align(Alignment.BottomStart)
                        .padding(start = 44.dp)
                        .fillMaxWidth()
                        .height(2.dp)
                        .drawBehind {
                            val duration = state.durationMs.takeIf { it > 0 } ?: return@drawBehind
                            val fraction = (positionProvider().toFloat() / duration).coerceIn(0f, 1f)
                            drawRect(
                                color = Color.White.copy(alpha = 0.55f),
                                size = androidx.compose.ui.geometry.Size(size.width * fraction, size.height),
                            )
                        },
                )
            }
        }
    }
}

@Composable
private fun BcBottomMini(
    player: PlayerController,
    state: PlayerState,
    language: String,
    positionProvider: () -> Long,
    onOpen: () -> Unit,
) {
    val track = state.current
    if (track == null) {
        Column(Modifier.fillMaxWidth().background(BC.Chrome)) {
            BcDivider(Modifier.fillMaxWidth())
            Box(
                Modifier.fillMaxWidth().height(58.dp).padding(horizontal = 12.dp),
                contentAlignment = Alignment.CenterStart,
            ) {
                Text("Not playing", style = MaterialTheme.typography.bodyMedium, color = BC.Secondary)
            }
        }
        return
    }
    // Bit's player bar, the reference's own mini player: it sits on the window's
    // bottom edge and carries the whole transport, the artwork, the title, the
    // volume, the output picker, the lyrics and the queue.
    AppleMusicV17MiniPlayer(
        nowPlaying = track,
        isPlaying = state.isPlaying,
        isLoading = state.isLoading,
        swipeThumbnail = false,
        swipeSensitivity = 1f,
        onTogglePlay = player::toggle,
        onNext = player::next,
        onPrevious = player::previous,
        onOpenPlayer = onOpen,
        language = language,
        volume = state.volume,
        isShuffle = state.isShuffle,
        repeatMode = state.repeatMode,
        positionMs = positionProvider,
        durationMs = state.durationMs,
        onVolume = player::setVolume,
        onToggleShuffle = player::toggleShuffle,
        onCycleRepeat = player::cycleRepeatMode,
        onOpenLyrics = onOpen,
        onOpenQueue = onOpen,
        modifier = Modifier.fillMaxWidth(),
    )
}

@Composable
private fun BcSidePanel(
    panel: BcPanel,
    state: PlayerState,
    positionProvider: () -> Long,
    language: String,
    onClose: () -> Unit,
    onSkipTo: (Int) -> Unit,
    onRemoveAt: (Int) -> Unit,
    onClear: () -> Unit,
    onSeek: (Long) -> Unit,
    onTogglePlay: () -> Unit,
) {
    Column(
        Modifier
            .width(360.dp)
            .fillMaxHeight()
            .background(BC.Chrome)
            .drawBehind { drawRect(BC.Divider, size = androidx.compose.ui.geometry.Size(1f, size.height)) },
    ) {
        Row(Modifier.fillMaxWidth().height(46.dp).padding(start = 16.dp, end = 8.dp), verticalAlignment = Alignment.CenterVertically) {
            Text(
                if (panel == BcPanel.LYRICS) "Lyrics" else "Queue",
                style = MaterialTheme.typography.titleMedium,
                color = BC.White,
            )
            Spacer(Modifier.weight(1f))
            if (panel == BcPanel.QUEUE && state.queue.isNotEmpty()) {
                Text(
                    "Clear",
                    style = MaterialTheme.typography.labelMedium,
                    color = BC.Secondary,
                    modifier = Modifier.clip(RoundedCornerShape(6.dp)).clickable(onClick = onClear).padding(horizontal = 8.dp, vertical = 4.dp),
                )
            }
            BcIconButton(onClick = onClose, size = 28) {
                Icon(Icons.Rounded.Close, "Close", tint = BC.Secondary, modifier = Modifier.size(16.dp))
            }
        }
        BcDivider(Modifier.fillMaxWidth())
        if (panel == BcPanel.LYRICS) {
            Box(Modifier.fillMaxSize()) {
                LyricsScreen(
                    nowPlaying = state.current,
                    positionMs = positionProvider,
                    isPlaying = state.isPlaying,
                    language = language,
                    synced = true,
                    onSeek = onSeek,
                    onTogglePlay = onTogglePlay,
                    onBack = null,
                    showHeader = false,
                )
            }
        } else {
            LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(vertical = 8.dp)) {
                itemsIndexed(state.queue, key = { index, item -> "$index:${item.videoId}" }) { index, item ->
                    val current = index == state.index
                    BcHover(
                        onClick = { onSkipTo(index) },
                        modifier = Modifier.fillMaxWidth().height(52.dp),
                        shape = RoundedCornerShape(0.dp),
                        hoverFill = Color.White.copy(alpha = 0.06f),
                    ) {
                        Row(
                            Modifier.fillMaxSize().padding(horizontal = 12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            BcArtwork(item.thumbnail, Modifier.size(38.dp), RoundedCornerShape(3.dp))
                            Spacer(Modifier.width(10.dp))
                            Column(Modifier.weight(1f)) {
                                Text(
                                    item.title,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = if (current) BC.White else BC.White.copy(alpha = 0.9f),
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                )
                                Text(
                                    item.artist,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = BC.Secondary,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                )
                            }
                            BcIconButton(onClick = { onRemoveAt(index) }, size = 26) {
                                Icon(
                                    Icons.Rounded.RemoveCircleOutline,
                                    "Remove",
                                    tint = BC.Secondary,
                                    modifier = Modifier.size(16.dp),
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun BcNowPlayingOverlay(
    player: PlayerController,
    state: PlayerState,
    language: String,
    positionProvider: () -> Long,
    onClose: () -> Unit,
) {
    val track = state.current ?: return
    // The reference's player, not a redraw of it: this is the same Apple Player
    // v17 surface the DE already ships (large artwork, the control column, the
    // inline lyric, the scrubber, the transport and the bottom bar), so the
    // window matches the reference screen for screen.
    ApplePlayerV17Screen(
        np = track,
        isPlaying = state.isPlaying,
        isLoading = state.isLoading,
        positionMs = positionProvider,
        durationMs = state.durationMs,
        volume = state.volume,
        isShuffle = state.isShuffle,
        repeatMode = state.repeatMode,
        errorKey = state.errorKey,
        errorDetail = state.errorDetail,
        onTogglePlay = player::toggle,
        onNext = player::next,
        onPrevious = player::previous,
        onSeek = player::seekTo,
        onVolume = player::setVolume,
        onToggleShuffle = player::toggleShuffle,
        onCycleRepeat = player::cycleRepeatMode,
        language = language,
        onOpenLyrics = {},
        onOpenQueue = {},
        onAddToPlaylist = {},
        sliderStyle = ViviSliderStyle.SLIM,
        // A painted gradient, not the 56 dp live blur: the blur costs about 16
        // fps on this class of machine (44 against 60), and the Bit surface is
        // held to the 60 fps floor. The gradient keeps the artwork's tint.
        background = PlayerBackgroundStyle.GRADIENT,
        onBack = onClose,
        queue = state.queue,
        queueIndex = state.index,
        onSkipTo = player::skipTo,
        onRemoveAt = player::removeAt,
        queueSource = state.sourceLabel,
    )
}

@Composable
private fun BcPlaybackLine(
    state: PlayerState,
    positionProvider: () -> Long,
    onSeek: (Long) -> Unit,
) {
    val position by rememberDrawnPosition(positionProvider)
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text(bcDurationMs(position), style = MaterialTheme.typography.labelSmall, color = BC.Secondary)
        Spacer(Modifier.width(10.dp))
        Box(Modifier.weight(1f)) {
            androidx.compose.material3.Slider(
                value = if (state.durationMs > 0) (position.toFloat() / state.durationMs).coerceIn(0f, 1f) else 0f,
                onValueChange = { onSeek((it * state.durationMs).toLong()) },
                colors = androidx.compose.material3.SliderDefaults.colors(
                    thumbColor = BC.White,
                    activeTrackColor = BC.White,
                    inactiveTrackColor = Color.White.copy(alpha = 0.22f),
                ),
            )
        }
        Spacer(Modifier.width(10.dp))
        Text(bcDurationMs(state.durationMs), style = MaterialTheme.typography.labelSmall, color = BC.Secondary)
    }
}

/** Reads the playhead at frame rate without asking the parent to recompose. */
@Composable
private fun rememberDrawnPosition(provider: () -> Long): androidx.compose.runtime.State<Long> {
    val value = remember { androidx.compose.runtime.mutableStateOf(provider()) }
    LaunchedEffect(Unit) {
        while (true) {
            value.value = provider()
            kotlinx.coroutines.delay(250L)
        }
    }
    return value
}
