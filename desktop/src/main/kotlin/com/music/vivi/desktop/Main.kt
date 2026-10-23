package com.music.vivi.desktop

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.LocalContextMenuRepresentation
import androidx.compose.foundation.LocalIndication
import androidx.compose.foundation.background
import androidx.compose.foundation.Image
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsHoveredAsState
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.foundation.verticalScroll
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Radio
import androidx.compose.material.icons.outlined.Radio
import androidx.compose.material.icons.filled.Explore
import androidx.compose.material.icons.outlined.Explore
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.rotate
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.filled.PlaylistPlay
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.automirrored.filled.QueueMusic
import androidx.compose.material.icons.automirrored.outlined.QueueMusic
import androidx.compose.material.icons.automirrored.filled.Subject
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.LibraryMusic
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Repeat
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.SpeakerGroup
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.NewReleases
import androidx.compose.material.icons.outlined.NewReleases
import androidx.compose.material.icons.filled.Leaderboard
import androidx.compose.material.icons.outlined.Leaderboard
import androidx.compose.material.icons.filled.Group
import androidx.compose.material.icons.outlined.Group
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.outlined.MusicNote
import androidx.compose.material.icons.filled.Lyrics
import androidx.compose.material.icons.filled.Person
import androidx.compose.foundation.window.WindowDraggableArea
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Album
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.PlayCircle
import androidx.compose.material.icons.filled.QueueMusic
import androidx.compose.foundation.border
import androidx.compose.material.icons.outlined.History
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.LibraryMusic
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.platform.LocalWindowInfo
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.withFrameNanos
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.FilterQuality
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Density
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEvent
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.isAltPressed
import androidx.compose.ui.input.key.isCtrlPressed
import androidx.compose.ui.input.key.isShiftPressed
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.WindowScope
import com.music.lastfm.LastFM
import com.music.innertube.YouTube
import kotlin.math.abs
import com.music.innertube.models.SongItem
import com.music.vivi.desktop.player.AudioOutput
import com.music.vivi.desktop.player.PlayerController
import com.music.vivi.desktop.player.RepeatMode
import com.music.vivi.desktop.player.StreamResolver
import com.music.vivi.sync.LibrarySnapshot
import com.music.vivi.sync.PlaybackSnapshot
import com.music.vivi.sync.SyncServer
import com.music.vivi.sync.SyncedSong
import com.music.vivi.sync.TrackRef
import java.awt.Desktop
import java.net.URI
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString


/** Maps a toast/command-line section id to the screen it should open. */
private fun screenForSection(section: String?): Screen? = when (section) {
    "updates" -> Screen.SettingsUpdates
    "developer" -> Screen.SettingsDeveloper
    "devices" -> Screen.SettingsDevices
    else -> null
}

@Composable
fun WindowScope.App(
    language: String,
    onLanguageChange: (String) -> Unit,
    font: AppFont,
    onFontChange: (AppFont) -> Unit,
    customFontPath: String = "",
    onImportFont: () -> Unit = {},
    themeMode: ThemeMode,
    accent: Color,
    onThemeModeChange: (ThemeMode) -> Unit,
    onAccentChange: (Color) -> Unit,
    accentIntensity: Float = 1f,
    onAccentIntensityChange: (Float) -> Unit = {},
    onAccentIntensityChangeFinished: () -> Unit = {},
    customAccents: List<Int> = emptyList(),
    onAddCustomAccent: (Int) -> Unit = {},
    onRemoveCustomAccent: (Int) -> Unit = {},
    pureBlack: Boolean,
    onPureBlackChange: (Boolean) -> Unit,
    spotifyLayout: Boolean = false,
    initialSection: String? = null,
    isFullscreen: Boolean = false,
    isMaximized: Boolean = false,
    nativeTitleBar: Boolean = false,
    onNativeTitleBarChange: (Boolean) -> Unit = {},
    onRestart: () -> Unit = {},
    onToggleFullscreen: () -> Unit = {},
    bringToFront: () -> Unit = {},
    onMinimize: () -> Unit = {},
    onMaximize: () -> Unit = {},
    onClose: () -> Unit = {},
) {
    // Startup timing: the first frame the window actually puts on screen,
    // measured from the JVM's own start, so "it takes N seconds to open" is a
    // number in `app.log` instead of an impression. Gated like the stages in
    // main(): nothing is written unless DetailedStartupLogs is on.
    LaunchedEffect(Unit) {
        withFrameNanos { }
        if (DesktopSettings.load().detailedStartupLogs) {
            val since = ProcessHandle.current().info().startInstant().orElse(null)
            val elapsed = if (since == null) -1L else java.time.Duration.between(since, java.time.Instant.now()).toMillis()
            AppLog.log("app", "startup: first frame at +${elapsed}ms after the JVM started")
        }
    }

    var backStack by remember { mutableStateOf(listOf<Screen>(Screen.Home)) }
    val player = remember { PlayerController() }
    val playerState by player.state.collectAsState()
    // Recently started tracks, used as seeds for the Home "Recommended" section.
    val recentSeedTracks by player.recentTracks.collectAsState()
    val nowPlaying = playerState.current
    val isPlaying = playerState.isPlaying
    // NOTE: the live audio level is deliberately NOT collected here. Reading it
    // at the app root recomposed this whole composable (and every screen below
    // it) on every audio tick (~14/s while a track plays), which is exactly the
    // steady UI load that competes with the audio writer on slower machines.
    // It is passed down as a flow instead and collected only inside the
    // visualizer background that displays it.

    // Restore the active EQ profile (Settings → Player & audio → Equalizer) on
    // startup so the saved equalization applies from the first track.
    LaunchedEffect(Unit) {
        val s = DesktopSettings.load()
        if (s.activeEqProfileId.isNotEmpty()) {
            player.setEqualizer(s.eqProfiles.find { it.id == s.activeEqProfileId })
        }
    }

    // Cider-style desktop features state (floating widget, media keys, tray menu).
    var showWidget by remember(settingsFileRevision()) { mutableStateOf(DesktopSettings.load().showNowPlayingWidget) }
    var mediaKeysEnabled by remember(settingsFileRevision()) { mutableStateOf(DesktopSettings.load().mediaKeysEnabled) }
    var trayMenuEnabled by remember(settingsFileRevision()) { mutableStateOf(DesktopSettings.load().trayMenuEnabled) }

    // Cider-style desktop integrations: global media keys (Windows hook),
    // tray right-click menu and tray tooltip with the current track.
    val isMac = System.getProperty("os.name", "").lowercase().contains("mac")
    // macOS no longer uses JNativeHook at all: the MediaPlayer session (system
    // "Now Playing" tile + media keys) is an OS-level integration that needs NO
    // Accessibility permission (issue #63). The session is therefore always
    // registered — the switch below only enables/disables its remote commands.
    LaunchedEffect(mediaKeysEnabled) {
        if (isMac) {
            MacMediaSession.start(
                appName = "VIVI Music",
                onPlayPause = { player.toggle() },
                onNext = { player.next() },
                onPrevious = { player.previous() },
                onSeek = { ms -> player.seekTo(ms) },
            )
            MacMediaSession.setCommandsEnabled(mediaKeysEnabled)
        } else if (mediaKeysEnabled) {
            MediaKeys.start(
                onPlayPause = { player.toggle() },
                onNext = { player.next() },
                onPrevious = { player.previous() },
            )
        } else {
            MediaKeys.stop()
        }
    }

    // macOS only: the native window chrome (title bar, native menus and
    // dialogs) follows the app's own Light/Dark mode instead of the OS one,
    // which used to leave a white title bar over a dark VIVI (issue #62).
    val appDark = when (themeMode) {
        ThemeMode.SYSTEM -> isSystemInDarkTheme()
        ThemeMode.LIGHT -> false
        ThemeMode.DARK -> true
    }
    LaunchedEffect(appDark) {
        if (isMac) MacMediaSession.setWindowAppearance(appDark)
    }

    // macOS only: keep the system "Now Playing" tile in sync with the current
    // track. Gated by the same "Media keys" toggle as the session above (so
    // disabling it clears the tile; re-enabling re-pushes the current state).
    // Runs once per track and pushes a position update every 500 ms while
    // playing. Artwork is downloaded in the background by MacMediaSession
    // itself (and decoded once per path on the native side).
    if (isMac && mediaKeysEnabled) {
        LaunchedEffect(nowPlaying?.videoId, isPlaying) {
            val np = nowPlaying
            if (np == null) {
                // No track: only the tile is cleared — the session stays
                // registered, so the NEXT track brings it back on its own
                // (tearing it down here left "Now Playing" dead until the
                // media-keys switch was toggled, issue #63).
                MacMediaSession.clearNowPlaying()
                return@LaunchedEffect
            }
            while (true) {
                // Read the controller directly, not the composition value:
                // the tile must carry the position and the playing flag as they
                // are NOW, and `player.state` is the authoritative flow behind
                // `playerState` (the effect is keyed on the track only, so it
                // keeps looping while the track plays).
                val live = player.state.value
                MacMediaSession.setNowPlaying(
                    title = np.title,
                    artist = np.artist,
                    durationMs = live.durationMs,
                    positionMs = live.positionMs,
                    playing = live.isPlaying,
                    artworkUrl = np.thumbnail?.takeIf {
                        it.startsWith("http://") || it.startsWith("https://")
                    },
                )
                // A track that was restored (persistent queue) and never played
                // this run: claiming the tile the way a real play-and-stop does
                // is what makes it appear on a freshly launched process (issue
                // #63). Called AFTER the push above so the claim has the track's
                // metadata; it is a no-op once it has run.
                if (!live.isPlaying) MacMediaSession.claimRestoredTrack()
                // While PAUSED the claim must stay alive too: with a track
                // restored from the persistent queue nothing has been played
                // since launch, and if the app stops being the system's "Now
                // Playing" owner the first media-key press can't reach it, so
                // the paused track could only be started by hand (issue #63).
                // Refreshing the claim is cheap now that the native side caches
                // the decoded artwork.
                delay(if (live.isPlaying) 500L else 2_000L)
            }
        }
    }
    // The tray menu is a transport control like any other, so it follows the same
    // gate: as a Listen Together guest its three rows are greyed and inert, and
    // the menu is rebuilt the moment the role changes (the effect keys on the
    // lock, which is snapshot state).
    val trayTransportLocked by ListenTogetherGate.locked
    LaunchedEffect(language, trayMenuEnabled, trayTransportLocked) {
        if (trayMenuEnabled) {
            NativeNotifier.configureTray(
                NativeNotifier.TrayActions(
                    onPlayPause = { player.toggle() },
                    onNext = { player.next() },
                    onPrevious = { player.previous() },
                    onOpen = { bringToFront() },
                    onQuit = onClose,
                    labelPlayPause = Localization.get(language, if (isPlaying) "pause" else "play"),
                    labelNext = Localization.get(language, "next"),
                    labelPrevious = Localization.get(language, "previous"),
                    labelOpen = Localization.get(language, "open_vivi"),
                    labelQuit = Localization.get(language, "quit"),
                    transportEnabled = !trayTransportLocked,
                )
            )
        } else {
            NativeNotifier.clearTrayMenu()
        }
    }
    LaunchedEffect(nowPlaying?.videoId) {
        NativeNotifier.setTrayTooltip(
            if (nowPlaying != null) "VIVI Music — ${nowPlaying.title} — ${nowPlaying.artist}" else "VIVI Music"
        )
    }

    // Cider-style floating "Now Playing" widget (always-on-top, draggable).
    if (showWidget) {
        NowPlayingWidgetWindow(
            player = player,
            language = language,
            themeMode = themeMode,
            accent = accent,
            pureBlack = pureBlack,
            font = font,
            onClose = {
                showWidget = false
                DesktopSettings.update { it.copy(showNowPlayingWidget = false) }
            },
        )
    }

    var autoPlayNext by remember(settingsFileRevision()) { mutableStateOf(DesktopSettings.load().autoPlayNext) }
    player.autoPlayNext = autoPlayNext
    var autoLoadMore by remember(settingsFileRevision()) { mutableStateOf(DesktopSettings.load().autoLoadMore) }
    player.autoLoadMore = autoLoadMore
    var preventDuplicateTracksInQueue by remember(settingsFileRevision()) { mutableStateOf(DesktopSettings.load().preventDuplicateTracksInQueue) }
    player.preventDuplicateTracksInQueue = preventDuplicateTracksInQueue
    var autoSkipNextOnError by remember(settingsFileRevision()) { mutableStateOf(DesktopSettings.load().autoSkipNextOnError) }
    player.autoSkipNextOnError = autoSkipNextOnError
    var pauseWhenMediaMuted by remember(settingsFileRevision()) { mutableStateOf(DesktopSettings.load().pauseWhenMediaMuted) }
    var keepScreenOnWhenPlayerExpanded by remember(settingsFileRevision()) { mutableStateOf(DesktopSettings.load().keepScreenOnWhenPlayerExpanded) }

    var persistentShuffle by remember(settingsFileRevision()) { mutableStateOf(DesktopSettings.load().persistentShuffle) }
    player.persistentShuffleAcrossQueues = persistentShuffle
    var progressiveSeek by remember(settingsFileRevision()) { mutableStateOf(DesktopSettings.load().progressiveSeek) }
    var historyDurationSeconds by remember(settingsFileRevision()) { mutableStateOf(DesktopSettings.load().historyDurationSeconds) }
    var autoDownloadOnLike by remember(settingsFileRevision()) { mutableStateOf(DesktopSettings.load().autoDownloadOnLike) }
    var skipSilence by remember(settingsFileRevision()) { mutableStateOf(DesktopSettings.load().skipSilence) }
    var skipSilenceInstant by remember(settingsFileRevision()) { mutableStateOf(DesktopSettings.load().skipSilenceInstant) }
    var crossfade by remember(settingsFileRevision()) { mutableStateOf(DesktopSettings.load().crossfade) }
    var crossfadeDurationSeconds by remember(settingsFileRevision()) { mutableStateOf(DesktopSettings.load().crossfadeDurationSeconds) }
    var disableCrossfadeGapless by remember(settingsFileRevision()) { mutableStateOf(DesktopSettings.load().disableCrossfadeGapless) }

    // "Auto download on like": cache a song into the audio cache the moment it
    // is liked (the setting is read live when the like happens).
    LaunchedEffect(player) {
        SongActions.onSongLiked = { id, _ ->
            if (DesktopSettings.load().autoDownloadOnLike) player.downloadToCache(id)
        }
    }

    // Bootup speed: parse the settings file once for the whole settings block
    // instead of one disk read + JSON parse per option.
    val startupSettings = remember(settingsFileRevision()) { DesktopSettings.load() }
    var densityScale by remember(settingsFileRevision()) { mutableStateOf(startupSettings.densityScale) }
    var gridItemSize by remember(settingsFileRevision()) { mutableStateOf(startupSettings.gridItemSize) }
    var screenTransition by remember(settingsFileRevision()) { mutableStateOf(startupSettings.screenTransition) }
    var animationsEnabled by remember(settingsFileRevision()) { mutableStateOf(startupSettings.animationsEnabled) }
    var animationSpeed by remember(settingsFileRevision()) { mutableStateOf(startupSettings.animationSpeed) }
    var sliderStyle by remember(settingsFileRevision()) { mutableStateOf(startupSettings.sliderStyle) }
    var playerDesign by remember(settingsFileRevision()) { mutableStateOf(PlayerDesign.from(startupSettings.playerDesign)) }
    var playerBackground by remember(settingsFileRevision()) { mutableStateOf(PlayerBackgroundStyle.from(startupSettings.playerBackground)) }
    var rotatingThumbnail by remember(settingsFileRevision()) { mutableStateOf(startupSettings.rotatingThumbnail) }
    var miniPlayerDesign by remember(settingsFileRevision()) { mutableStateOf(MiniPlayerDesign.from(DesktopSettings.load().miniPlayerDesign)) }
    var miniPlayerBackgroundStyle by remember(settingsFileRevision()) { mutableStateOf(MiniPlayerBackgroundStyle.from(DesktopSettings.load().miniPlayerBackgroundStyle)) }
    var pureBlackMiniPlayer by remember(settingsFileRevision()) { mutableStateOf(DesktopSettings.load().pureBlackMiniPlayer) }
    var showRightSidebar by remember(settingsFileRevision()) { mutableStateOf(DesktopSettings.load().showRightSidebar) }
    var homeUseLastListen by remember(settingsFileRevision()) { mutableStateOf(DesktopSettings.load().homeUseLastListen) }
    var randomizeHomeOrder by remember(settingsFileRevision()) { mutableStateOf(DesktopSettings.load().randomizeHomeOrder) }
    var showWrappedOnHome by remember(settingsFileRevision()) { mutableStateOf(DesktopSettings.load().showWrappedOnHome) }
    var showIntroSplash by remember(settingsFileRevision()) { mutableStateOf(DesktopSettings.load().showIntroSplash) }
    var introStyle by remember(settingsFileRevision()) { mutableStateOf(DesktopSettings.load().introStyle) }
    var introBackground by remember(settingsFileRevision()) { mutableStateOf(DesktopSettings.load().introBackground) }
    var hideExplicit by remember(settingsFileRevision()) { mutableStateOf(DesktopSettings.load().hideExplicit) }
    var hideVideoSongs by remember(settingsFileRevision()) { mutableStateOf(DesktopSettings.load().hideVideoSongs) }
    var hideYoutubeShorts by remember(settingsFileRevision()) { mutableStateOf(DesktopSettings.load().hideYoutubeShorts) }
    var showArtistDescription by remember(settingsFileRevision()) { mutableStateOf(DesktopSettings.load().showArtistDescription) }
    var showArtistSubscriberCount by remember(settingsFileRevision()) { mutableStateOf(DesktopSettings.load().showArtistSubscriberCount) }
    var lyricsProviders by remember(settingsFileRevision()) { mutableStateOf(DesktopSettings.load().lyricsProviderPriority) }
    var swipeThumbnail by remember(settingsFileRevision()) { mutableStateOf(DesktopSettings.load().swipeThumbnail) }
    var swipeSensitivity by remember(settingsFileRevision()) { mutableStateOf(DesktopSettings.load().swipeSensitivity) }
    // The tab finish of the expressive player is not a flag of its own any more:
    // the two expressive finishes are two entries of the player picker, so the
    // chosen [PlayerDesign] IS the answer. It is still mirrored into the older
    // boolean on write, so a settings file stays readable by an older build.
    val expressiveTabTranslucent = playerDesign == PlayerDesign.EXPRESSIVE_TRANSLUCENT
    var lyricsThumbnailPlayPause by remember(settingsFileRevision()) { mutableStateOf(DesktopSettings.load().lyricsThumbnailPlayPause) }
    var pauseSearchHistory by remember(settingsFileRevision()) { mutableStateOf(DesktopSettings.load().pauseSearchHistory) }
    var syncPlaylistsWithYoutube by remember(settingsFileRevision()) { mutableStateOf(DesktopSettings.load().syncPlaylistsWithYoutube) }
    var pauseListenHistory by remember(settingsFileRevision()) { mutableStateOf(DesktopSettings.load().pauseListenHistory) }
    var searchHistory by remember(settingsFileRevision()) { mutableStateOf(DesktopSettings.load().searchHistory) }
    val recordSearch: (String) -> Unit = { term ->
        if (!pauseSearchHistory && term.isNotBlank()) {
            val updated = (listOf(term.trim()) + searchHistory.filter { !it.equals(term.trim(), ignoreCase = true) }).take(12)
            searchHistory = updated
            DesktopSettings.update { it.copy(searchHistory = updated) }
            // The same term goes to the persistent history, so the History
            // screen can offer every search of the past sessions, not just the
            // twelve the sidebar remembers.
            HistoryStore.recordSearch(term)
        }
    }
    // The player owns the local playback history: keep its "pause listen" flag
    // in sync with the setting (read once here, then on every change below).
    remember(pauseListenHistory) { player.listenHistoryPaused = pauseListenHistory }

    // Session listening stats for the Home "VIVI Wrapped" card (session-only).
    var sessionTrackStarts by remember { mutableStateOf(0) }
    var sessionPlayedMs by remember { mutableStateOf(0L) }
    var sessionTopSong by remember { mutableStateOf<Pair<String, String>?>(null) } // videoId to title
    var sessionTopCount by remember { mutableStateOf(0) }
    var lastSessionSongId by remember { mutableStateOf<String?>(null) }
    var lastSessionPosition by remember { mutableStateOf(0L) }
    // Keyed on the TRACK, never on the position (issue #3): keying this effect
    // on `playerState.positionMs` cancelled and restarted the coroutine on
    // ~every position tick (the writer reports ~8/s, the player 20+/s). The
    // position is now read from the controller inside the loop instead.
    LaunchedEffect(nowPlaying?.videoId, isPlaying) {
        val id = nowPlaying?.videoId ?: return@LaunchedEffect
        if (id != lastSessionSongId) {
            // New track started in this session: count it and track the top one.
            lastSessionSongId = id
            if (sessionTopSong?.first == id) {
                sessionTopCount++
            } else {
                sessionTopSong = id to (nowPlaying?.title ?: "")
                sessionTopCount = 1
            }
            sessionTrackStarts++
            lastSessionPosition = player.state.value.positionMs
        }
        while (true) {
            delay(1_000L)
            val pos = player.state.value.positionMs
            val delta = pos - lastSessionPosition
            // A jump bigger than 10 s is a seek (or a track change), not
            // listening time: the same guard the previous version used.
            if (delta in 1..10_000) sessionPlayedMs += delta
            lastSessionPosition = pos
        }
    }
    var canvasEnabled by remember(settingsFileRevision()) { mutableStateOf(DesktopSettings.load().canvasEnabled) }
    var canvasSource by remember(settingsFileRevision()) { mutableStateOf(CanvasSource.from(DesktopSettings.load().canvasSource)) }

    // Guest sessions need a visitorData (like the Android app) or YouTube flags
    // the requests as bots and 403s audio playback.
    LaunchedEffect(Unit) { GuestSession.ensure() }

    // A muted Windows master volume ignores every volume write, so a paired
    // mobile device could never control it; unmute it (and drop it to 0%) once
    // at startup. Non-Windows and non-muted states are left untouched.
    LaunchedEffect(Unit) { SystemVolume.unmuteIfMuted() }

    // Scheduled automatic backups (weekly): check on startup, then hourly.
    LaunchedEffect(Unit) {
        while (true) {
            runCatching { BackupManager.maybeRunScheduled() }
            delay(3600_000L)
        }
    }

    var isLoggedIn by remember { mutableStateOf(LoginManager.isLoggedIn()) }
    var accountName by remember(settingsFileRevision()) { mutableStateOf(DesktopSettings.load().accountName) }
    var accountChannelHandle by remember(settingsFileRevision()) { mutableStateOf(DesktopSettings.load().accountChannelHandle) }

    LaunchedEffect(isLoggedIn) {
        if (isLoggedIn) {
            YouTube.accountInfo().onSuccess { account ->
                accountName = account.name
                accountChannelHandle = account.channelHandle.orEmpty()
                DesktopSettings.update {
                    it.copy(
                        accountName = account.name,
                        accountEmail = account.email.orEmpty(),
                        accountChannelHandle = account.channelHandle.orEmpty(),
                    )
                }
            }
        }
    }

    // Mobile "Auto sync with account": on sign-in (and whenever the option is
    // switched back on) the account's playlists are mirrored into the local
    // store, so they can be played, queued and edited like a local playlist
    // instead of only being listed by the sidebar.
    LaunchedEffect(isLoggedIn, syncPlaylistsWithYoutube) {
        if (isLoggedIn && syncPlaylistsWithYoutube) PlaylistSync.sync("startup")
    }

    // The account is not only read at startup. A song added on YouTube Music
    // from the phone cannot reach this device while the app stays open, because
    // the only runs of the pull were the one at startup and the one behind the
    // switch - which is the report "I added songs from mobile to a YouTube
    // Music playlist and it still does not update on the DE", made minutes
    // after the startup run had already logged its own result. A timer re-reads
    // the account every few minutes, and [PlaylistSync.refreshLinked] is what
    // turns a song added there into a song here (it also moves the playlist's
    // `updatedAt`, so the same change travels on to the paired phone). A run
    // that finds nothing writes nothing, which is what keeps the last-write-wins
    // key of an untouched playlist intact for the device sync.
    LaunchedEffect(isLoggedIn, syncPlaylistsWithYoutube) {
        if (!isLoggedIn || !syncPlaylistsWithYoutube) return@LaunchedEffect
        val intervalMs = 5L * 60L * 1000L
        while (true) {
            delay(intervalMs)
            PlaylistSync.sync("periodic")
        }
    }

    val displayUserName = if (isLoggedIn && accountName.isNotBlank()) accountName else "Guest"
    val displayUserHandle = if (isLoggedIn && accountName.isNotBlank()) {
        accountChannelHandle.ifBlank { "@${accountName.lowercase().replace(" ", "")}" }
    } else {
        "Not signed in"
    }

    var sidebarCollapsed by remember(settingsFileRevision()) { mutableStateOf(DesktopSettings.load().sidebarCollapsed) }
    var contentLanguage by remember(settingsFileRevision()) { mutableStateOf(DesktopSettings.load().contentLanguage) }
    var contentCountry by remember(settingsFileRevision()) { mutableStateOf(DesktopSettings.load().contentCountry) }
    var syncedLyrics by remember(settingsFileRevision()) { mutableStateOf(DesktopSettings.load().syncedLyrics) }
    var audioQuality by remember(settingsFileRevision()) { mutableStateOf(DesktopSettings.load().audioQuality) }
    var rememberShuffleRepeat by remember(settingsFileRevision()) { mutableStateOf(DesktopSettings.load().rememberShuffleRepeat) }
    var persistentQueue by remember(settingsFileRevision()) { mutableStateOf(DesktopSettings.load().persistentQueue) }
    var syncViviVolume by remember(settingsFileRevision()) { mutableStateOf(DesktopSettings.load().syncViviVolume) }
    var syncNativeVolume by remember(settingsFileRevision()) { mutableStateOf(DesktopSettings.load().syncNativeVolume) }
    var lyricsTextSize by remember(settingsFileRevision()) { mutableStateOf(DesktopSettings.load().lyricsTextSize) }
    var lyricsLineSpacing by remember(settingsFileRevision()) { mutableStateOf(DesktopSettings.load().lyricsLineSpacing) }
    // Animation style + display options (mobile lyrics port). Kept as one object
    // so the renderer, the settings screen and the file always agree.
    var lyricsDisplay by remember(settingsFileRevision()) { mutableStateOf(lyricsDisplayOptionsFrom(DesktopSettings.load())) }
    var translateLyrics by remember(settingsFileRevision()) { mutableStateOf(DesktopSettings.load().translateLyrics) }
    // The lyrics' own face: on, the app font the font screen chose; off, the
    // renderer's own default. See `FontSection` and `LyricsDisplayOptions.fontFamily`.
    var lyricsUseAppFont by remember(settingsFileRevision()) { mutableStateOf(DesktopSettings.load().lyricsUseAppFont) }
    // The family the lyrics draw with, resolved from the same choice the theme
    // uses. Null means "the renderer's own default", which is what the switch off.
    val lyricsFontFamily = if (lyricsUseAppFont) AppFonts.familyFor(font, customFontPath) else null
    // AI translation needs a key, and the row that offers it says so: DeepL has
    // its own key, every other provider the AI one.
    val lyricsTranslationAvailable = remember(settingsFileRevision()) {
        val s = DesktopSettings.load()
        if (s.aiProvider == "DeepL") s.deeplApiKey.isNotBlank() else s.aiApiKey.isNotBlank()
    }
    var streamCacheMinutes by remember(settingsFileRevision()) { mutableStateOf(DesktopSettings.load().streamCacheMinutes) }
    var discordRpcEnabled by remember(settingsFileRevision()) { mutableStateOf(DesktopSettings.load().discordRpcEnabled) }
    var discordClientId by remember(settingsFileRevision()) { mutableStateOf(DesktopSettings.load().discordClientId) }
    var lastfmEnabled by remember(settingsFileRevision()) { mutableStateOf(DesktopSettings.load().lastfmEnabled) }
    var lastfmSession by remember(settingsFileRevision()) { mutableStateOf(DesktopSettings.load().lastfmSession) }
    var lastfmNowPlaying by remember(settingsFileRevision()) { mutableStateOf(DesktopSettings.load().lastfmNowPlaying) }

    // Integrations: initialize Last.fm with env-provided credentials (like the
    // mobile BuildConfig) and mirror the Discord toggles to the RPC client.
    LaunchedEffect(Unit) {
        val key = System.getenv("LASTFM_API_KEY").orEmpty()
        val secret = System.getenv("LASTFM_SECRET").orEmpty()
        if (key.isNotEmpty() && secret.isNotEmpty()) LastFM.initialize(key, secret)
        DiscordRPC.clientId = DiscordRPC.clientId.ifBlank { discordClientId }
        DiscordRPC.enabled = discordRpcEnabled
    }
    LaunchedEffect(discordRpcEnabled, discordClientId) {
        DiscordRPC.clientId = discordClientId
        DiscordRPC.enabled = discordRpcEnabled
    }

    // Discord presence + Last.fm now-playing / scrobble feed.
    var lastfmReported by remember { mutableStateOf<String?>(null) }
    var lastfmScrobbled by remember { mutableStateOf<String?>(null) }
    // Keyed on the track/options, not on the position: with `positionMs` in the
    // keys this effect restarted ~20 times a second and pushed a Discord
    // presence update on every one of them (issue #3). The scrobble check is
    // polled once a second from the controller instead.
    LaunchedEffect(nowPlaying?.videoId, isPlaying, discordRpcEnabled, lastfmEnabled, lastfmSession) {
        val np = nowPlaying
        if (np == null) {
            DiscordRPC.updateActivity(null, null, null, null)
            return@LaunchedEffect
        }
        if (discordRpcEnabled && discordClientId.isNotBlank() && isPlaying) {
            DiscordRPC.updateActivity(details = np.title, state = np.artist, largeImage = "vivimusic")
        }
        if (!(lastfmEnabled && lastfmSession.isNotBlank() && LastFM.isInitialized())) return@LaunchedEffect
        LastFM.sessionKey = lastfmSession
        if (isPlaying && lastfmReported != np.videoId) {
            lastfmReported = np.videoId
            if (lastfmNowPlaying) {
                runCatching { LastFM.updateNowPlaying(artist = np.artist, track = np.title, duration = (np.durationMs / 1000L).toInt().coerceAtLeast(0)) }
            }
        }
        // Scrobble near the end of the track (>30 s long, >=15 s before it
        // finishes), checked once a second until it fires.
        val dur = np.durationMs
        if (dur <= 30_000) return@LaunchedEffect
        while (lastfmScrobbled != np.videoId) {
            delay(1_000L)
            val pos = player.state.value.positionMs
            if (pos > 0 && pos >= dur - 15_000) {
                lastfmScrobbled = np.videoId
                runCatching { LastFM.scrobble(artist = np.artist, track = np.title, timestamp = System.currentTimeMillis() / 1000, duration = (dur / 1000L).toInt()) }
            }
        }
    }

    PersistentQueueEffect(player = player, playerState = playerState, persistentQueue = persistentQueue)

    val current = backStack.last()

    // "Keep screen on when player is expanded": hold a keep-awake request
    // while the full player screen is open (desktop adaptation of the mobile
    // window flag). Independent of the pairing keep-awake, which uses its own
    // request name.
    LaunchedEffect(keepScreenOnWhenPlayerExpanded, current) {
        KeepAwake.request("expanded-player", keepScreenOnWhenPlayerExpanded && current is Screen.Player)
    }

    // Undo/redo stacks for keyboard navigation history (Ctrl+Z / Ctrl+Y).
    var undoStack by remember { mutableStateOf(listOf<Screen>()) }
    var redoStack by remember { mutableStateOf(listOf<Screen>()) }
    fun screenLabel(s: Screen): String = s::class.simpleName ?: s.toString()

    val navigate: (Screen) -> Unit = navigate@{ target ->
        // Never push a duplicate of the screen already on top (double-clicks
        // or a stale click handler must not create [X, X] entries).
        if (backStack.last() == target) return@navigate
        AppLog.click("open ${screenLabel(target)}")
        AppLog.log("nav", "open ${screenLabel(target)}")
        redoStack = emptyList()
        undoStack = undoStack + backStack.last()
        backStack = backStack + target
    }
    val openRoot: (Screen) -> Unit = { screen ->
        // Keep Home at the base so "back" from a root (e.g. Settings) returns
        // to Home instead of getting stuck with nothing to pop.
        AppLog.click("root ${screenLabel(screen)}")
        AppLog.log("nav", "root → ${screenLabel(screen)}")
        backStack = if (screen == Screen.Home) listOf(Screen.Home) else listOf(Screen.Home, screen)
    }
    val goBack: () -> Unit = {
        if (backStack.size > 1) {
            AppLog.click("back ← ${screenLabel(backStack.last())}")
            AppLog.log("nav", "back ← ${screenLabel(backStack.last())}")
            undoStack = undoStack + backStack.last()
            backStack = backStack.dropLast(1)
        }
    }
    val undo: () -> Unit = {
        if (undoStack.isNotEmpty()) {
            AppLog.log("nav", "undo to ${screenLabel(undoStack.last())}")
            redoStack = redoStack + backStack.last()
            backStack = backStack + undoStack.last()
            undoStack = undoStack.dropLast(1)
        }
    }
    val redo: () -> Unit = {
        if (redoStack.isNotEmpty()) {
            AppLog.log("nav", "redo to ${screenLabel(redoStack.last())}")
            undoStack = undoStack + backStack.last()
            backStack = backStack + redoStack.last()
            redoStack = redoStack.dropLast(1)
        }
    }

    // Mouse thumb / "special" buttons: X1 (button 4) goes back, X2 (button 5)
    // goes forward, like every browser and media app. Always active — there is
    // deliberately no setting for it. A global AWT listener is used because
    // Compose's Skia canvas consumes the mouse event before it ever reaches a
    // listener attached to the window itself.
    val mouseBackRef = rememberUpdatedState(goBack)
    val mouseForwardRef = rememberUpdatedState(redo)
    val mainWindowComponent = window
    DisposableEffect(mainWindowComponent) {
        val listener = java.awt.event.AWTEventListener { e ->
            if (e !is java.awt.event.MouseEvent) return@AWTEventListener
            if (e.id != java.awt.event.MouseEvent.MOUSE_PRESSED) return@AWTEventListener
            if (e.button != 4 && e.button != 5) return@AWTEventListener
            // Only react to clicks inside the main window (the floating widget
            // and dialogs keep their own handling).
            val inMainWindow = generateSequence<java.awt.Component>(e.component as? java.awt.Component) { it.parent }
                .any { it === mainWindowComponent }
            if (!inMainWindow) return@AWTEventListener
            when (e.button) {
                4 -> {
                    e.consume()
                    mouseBackRef.value()
                }
                5 -> {
                    e.consume()
                    mouseForwardRef.value()
                }
            }
        }
        java.awt.Toolkit.getDefaultToolkit()
            .addAWTEventListener(listener, java.awt.AWTEvent.MOUSE_EVENT_MASK)
        onDispose {
            java.awt.Toolkit.getDefaultToolkit().removeAWTEventListener(listener)
        }
    }

    // Global keyboard navigation: Backspace/Alt+Left goes back, Ctrl+Z/Ctrl+Y
    // undo/redo the navigation history.
    val onGlobalKey: (KeyEvent) -> Boolean = { event ->
        when {
            event.type != KeyEventType.KeyDown -> false
            event.key == Key.F11 -> {
                onToggleFullscreen()
                true
            }
            event.key == Key.Backspace || event.key == Key.Escape || (event.isAltPressed && event.key == Key.DirectionLeft) -> {
                goBack(); true
            }
            event.isCtrlPressed && event.key == Key.Z -> { undo(); true }
            event.isCtrlPressed && event.key == Key.Y -> { redo(); true }
            // Listen Together chat: the same toggle from anywhere in the app, so
            // the conversation does not need the room screen to be open (the
            // window outlives it on purpose). Ctrl+Shift+C, which nothing else
            // here claims: Ctrl+C is plain copy and Ctrl+Shift+C is not used by
            // the app's own shortcut set.
            event.isCtrlPressed && event.isShiftPressed && event.key == Key.C -> {
                ListenTogetherChatWindow.toggle()
                true
            }
            else -> false
        }
    }

    // Toast / command-line "open section" requests: navigate and bring the
    // window to the foreground (handled once at startup, then polled for the
    // lifetime of the app so a toast click can reach a running instance).
    LaunchedEffect(Unit) {
        var pending = initialSection
        while (true) {
            AppCommand.poll()?.let { pending = it }
            if (pending != null) {
                screenForSection(pending)?.let { openRoot(it) }
                bringToFront()
                pending = null
            }
            delay(500)
        }
    }
    fun songToNowPlaying(song: SongItem): NowPlaying = NowPlaying(
        videoId = song.id,
        title = song.title,
        artist = song.artists.joinToString(", ") { it.name },
        thumbnail = song.thumbnail,
        durationMs = (song.duration ?: 0) * 1000L,
        // Album name (when known): feeds the "disable crossfade for gapless
        // albums" rule on the player.
        album = song.album?.name,
    )

    val playSong: (SongItem) -> Unit = { song -> player.play(songToNowPlaying(song)) }
    val addToQueue: (SongItem) -> Unit = { song -> player.addToQueue(songToNowPlaying(song)) }
    var recognitionHistory by remember(settingsFileRevision()) { mutableStateOf(DesktopSettings.load().recognitionHistory) }
    var addToPlaylistSong by remember { mutableStateOf<SyncedSong?>(null) }
    val addToPlaylist: (SongItem) -> Unit = { song -> addToPlaylistSong = song.toSyncedSong() }
    // Same, but for the Player / Queue (which carry NowPlaying, not SongItem).
    val addNowPlayingToPlaylist: (NowPlaying) -> Unit = { np ->
        addToPlaylistSong = SyncedSong(id = np.videoId, title = np.title, artist = np.artist, thumbnail = np.thumbnail)
    }
    val playAll: (List<SongItem>) -> Unit = { songs -> player.playAll(songs.map(::songToNowPlaying)) }
    val shuffleAll: (List<SongItem>) -> Unit = { songs ->
        if (!playerState.isShuffle) player.toggleShuffle()
        player.playAll(songs.shuffled().map(::songToNowPlaying))
    }
    // The Album / Artist / Playlist header menus queue the whole collection:
    // `insertNext` puts one track right after the current position, so the list
    // is walked backwards to keep the album's own order. With nothing playing
    // there is no "next" to insert after, so the collection just starts.
    val playNextAll: (List<SongItem>) -> Unit = { songs ->
        when {
            songs.isEmpty() -> Unit
            playerState.current == null -> player.playAll(songs.map(::songToNowPlaying))
            else -> songs.asReversed().forEach { player.insertNext(songToNowPlaying(it)) }
        }
    }
    val addAllToQueue: (List<SongItem>) -> Unit = { songs ->
        player.addAllToQueue(songs.map(::songToNowPlaying))
    }

    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }
    LaunchedEffect(Unit) {
        DesktopSnackbar.events.collectLatest { msg ->
            snackbarHostState.currentSnackbarData?.dismiss()
            snackbarHostState.showSnackbar(msg)
        }
    }
    // Generic in-app notification banner (title + message) for notifications
    // dispatched in "main window" mode.
    var appNotification by remember { mutableStateOf<DesktopNotifier.Notice?>(null) }
    LaunchedEffect(Unit) {
        DesktopNotifier.events.collect { appNotification = it }
    }
    // Auto-dismiss the generic in-app notification after the configured time.
    LaunchedEffect(appNotification) {
        val notice = appNotification ?: return@LaunchedEffect
        val seconds = DesktopSettings.load().inAppNotificationDurationSeconds
        if (seconds > 0) {
            delay(seconds * 1000L)
            appNotification = null
        }
    }
    var headerSearchQuery by remember { mutableStateOf("") }
    var headerSearchFilter by remember { mutableStateOf<YouTube.SearchFilter?>(null) }
    var includePreReleases by remember {
        mutableStateOf(
            DesktopSettings.load().includePreReleases || AppInfo.CHANNEL.lowercase() != "stable"
        )
    }
    var updateIntervalHours by remember(settingsFileRevision()) { mutableStateOf(DesktopSettings.load().updateCheckIntervalHours) }
    var updateSource by remember(settingsFileRevision()) { mutableStateOf(DesktopSettings.load().updateSource) }
    var notificationMode by remember(settingsFileRevision()) { mutableStateOf(DesktopSettings.load().notificationMode) }
    var notificationDurationSeconds by remember(settingsFileRevision()) { mutableStateOf(DesktopSettings.load().inAppNotificationDurationSeconds) }
    var saveNotificationHistory by remember(settingsFileRevision()) { mutableStateOf(DesktopSettings.load().saveNotificationHistory) }
    var updateStatus by remember { mutableStateOf<UpdateStatus>(UpdateStatus.Idle) }
    // Keep the shared download state (used by both the notification and the
    // Updates screen) in sync with the latest update status.
    LaunchedEffect(updateStatus) { UpdateState.syncWithStatus(updateStatus) }
    val devEnabled by DeveloperOptions.enabled.collectAsState()
    val devMode by DeveloperOptions.mode.collectAsState()
    val overlayMovable by DeveloperOptions.overlayMovable.collectAsState()

    // One-off hint when the developer options get unlocked. Respects the
    // notification mode (in-app banner vs native system notification).
    var showDevNotification by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        DeveloperOptions.unlockedEvent.collect {
            val title = Localization.get(language, "dev_unlocked_title")
            val desc = Localization.get(language, "dev_unlocked_desc")
            if (DesktopSettings.load().notificationMode == "native") {
                DesktopNotifier.notify(title, desc, "developer")
            } else {
                NotificationHistory.record(title, desc, "in_app")
                showDevNotification = true
            }
        }
    }
    // Auto-dismiss the dev-unlocked hint after the configured time.
    LaunchedEffect(showDevNotification) {
        if (showDevNotification) {
            val seconds = DesktopSettings.load().inAppNotificationDurationSeconds
            if (seconds > 0) {
                delay(seconds * 1000L)
                showDevNotification = false
            }
        }
    }

    fun runUpdateCheck() {
        updateStatus = UpdateStatus.Checking
        scope.launch {
            val result = withContext(Dispatchers.IO) { UpdateChecker.check(includePreReleases) }
            updateStatus = result
        }
    }

    // Automatic update check on startup.
    LaunchedEffect(Unit) { runUpdateCheck() }

    // Periodic update check, at the user-selected interval (0 = manual only).
    LaunchedEffect(updateIntervalHours) {
        if (updateIntervalHours <= 0) return@LaunchedEffect
        while (true) {
            delay(updateIntervalHours * 3_600_000L)
            runUpdateCheck()
        }
    }

    // Update notification, shown once per new version. Where it appears
    // depends on the user's notification mode (in-app vs native system).
    var showUpdateNotification by remember { mutableStateOf(false) }
    var updateNotifiedVersion by remember { mutableStateOf<String?>(null) }
    LaunchedEffect(updateStatus) {
        val available = updateStatus as? UpdateStatus.Available
        if (available != null && available.version != updateNotifiedVersion) {
            updateNotifiedVersion = available.version
            val mode = if (DesktopSettings.load().notificationMode == "native") "native" else "in_app"
            val title = Localization.get(language, "update_available")
            val message = "${Localization.get(language, "current_version")}: ${AppInfo.FULL_VERSION}\n${available.version}"
            NotificationHistory.record(title, message, mode)
            if (mode == "native") {
                NativeNotifier.notify(title, message, "updates")
            } else {
                showUpdateNotification = true
            }
        }
    }
    // Auto-dismiss the update banner after the configured time, but never while
    // it is showing download progress. The download itself keeps running in the
    // shared UpdateState (also visible in Settings → Updates).
    val updateDownloading by UpdateState.progress.collectAsState()
    val updateBusy = updateDownloading != null
    LaunchedEffect(showUpdateNotification, updateBusy) {
        if (showUpdateNotification && !updateBusy) {
            val seconds = DesktopSettings.load().inAppNotificationDurationSeconds
            if (seconds > 0) {
                delay(seconds * 1000L)
                // A download may have started while we waited; don't dismiss then.
                if (UpdateState.progress.value == null) {
                    showUpdateNotification = false
                }
            }
        }
    }

    // ---- Device sync (Android <-> desktop) ----
    val syncManager = remember { DesktopSyncManager() }

    // ---- Listen Together (shared rooms over the relay) ----
    val listenTogetherManager = remember { ListenTogetherManager(player) }
    LaunchedEffect(listenTogetherManager) {
        listenTogetherManager.initialize()
        // Published so a song's "⋮" menu (far from the room screen) can offer
        // the guest-only "Suggest to the host" action.
        ListenTogetherBridge.manager = listenTogetherManager
    }

    // Echo guards: when we apply a remote volume, we must not push the
    // resulting local change straight back to the peer.
    val systemVolumeGuard = remember { VolumeGuard() }
    val volumeGuard = remember { VolumeGuard() }

    // Local change wins over the peer: a drag on the VIVI volume slider
    // stamps this timestamp, and incoming remote volumes are ignored for a
    // short window so a stale/echoed value can't snap the slider back.
    var localVolumeChangedAt by remember { mutableStateOf(0L) }
    val setLocalVolume: (Float) -> Unit = { v ->
        localVolumeChangedAt = System.currentTimeMillis()
        player.setVolume(v)
    }

    // Notify when a phone pairs or un-pairs (respects the notification mode).
    var wasPaired by remember { mutableStateOf(false) }
    LaunchedEffect(syncManager) {
        syncManager.paired.collect { paired ->
            // Keep the display/system awake while paired so the OS sleeping
            // the screen can't tear down the sync socket and unpair the two
            // devices.
            KeepAwake.request("paired", paired)
            if (paired != wasPaired) {
                wasPaired = paired
                if (paired) {
                    val name = syncManager.peerDeviceName.value.ifBlank { null }
                    DesktopNotifier.notify(
                        Localization.get(language, "device_paired_title"),
                        name ?: Localization.get(language, "device_paired_desc"),
                        "devices",
                    )
                } else {
                    DesktopNotifier.notify(
                        Localization.get(language, "device_unpaired_title"),
                        Localization.get(language, "device_unpaired_desc"),
                        "devices",
                    )
                }
            }
        }
    }

    // Look-ahead prefetch: cache the tracks around the current one in the
    // background so they start instantly when skipped to. Runs regardless of
    // play/pause, so pausing still fills the cache. Order: the current track
    // first (a restored queue has nothing loaded, so it must be ready the
    // moment the user presses play), then the 3 next + 3 previous tracks,
    // then the rest of the queue, one download at a time, so with "cache
    // forever" the whole queue ends up on disk.
    LaunchedEffect(player) {
        var prefetchJob: kotlinx.coroutines.Job? = null
        player.state
            .map { it.queue to it.index }
            .distinctUntilChanged()
            .collect { (queue, index) ->
                if (queue.isEmpty() || index !in queue.indices) return@collect

                val upcoming = queue.drop(index + 1)
                val nearestFirst = (upcoming.take(3) +
                    queue.take(index).takeLast(3).asReversed())
                    .distinctBy { it.videoId }
                val restOfQueue = upcoming.drop(3)
                // The current track goes FIRST: at startup (queue restored
                // from the persistent queue) it is not playing yet, so by the
                // time the user presses play it is already on disk and starts
                // instantly instead of resolving + downloading.
                val currentTrack = queue.getOrNull(index)

                // Lyrics for the upcoming tracks used to be pre-fetched here
                // with a fake duration (-1). Without a real duration the
                // community server can return the WRONG recording of a title
                // and, because the result was cached forever, the mistake
                // stuck. Lyrics are fetched on demand (with the real duration,
                // through [DesktopLyrics]) when each song actually plays, so
                // this poisoning pre-fetch is intentionally gone.

                // A queue/index change restarts the pass on the new window
                // (an old pass must not keep downloading stale tracks).
                prefetchJob?.cancel()
                prefetchJob = launch(Dispatchers.IO) {
                    // Seconds of the playing track's own headroom below which the
                    // cache pass yields (see the wait inside the loop).
                    val prefetchMinCushionSeconds = 20.0
                    val order = (listOfNotNull(currentTrack) + nearestFirst + restOfQueue).distinctBy { it.videoId }
                    for (track in order) {
                        if (player.isCached(track.videoId)) continue
                        // The cache pass must never take bandwidth away from the
                        // track the user is actually listening to (issue #3):
                        // while that track's own cushion is thin the pass waits
                        // and resumes as soon as the stream is comfortably
                        // ahead. Bounded, so a failed download cannot park it
                        // forever.
                        val cushionDeadline = System.currentTimeMillis() + 60_000L
                        while (player.playbackCushionSeconds() < prefetchMinCushionSeconds &&
                            System.currentTimeMillis() < cushionDeadline
                        ) {
                            delay(500)
                        }
                        val streams = StreamResolver.resolveAacStream(
                            track.videoId,
                            StreamResolver.AudioQuality.from(DesktopSettings.load().audioQuality),
                        )
                        if (streams.isEmpty()) continue
                        player.prefetch(streams, track.videoId)
                        // Wait for the `.part` to be promoted to the final
                        // cache name before moving on (best-effort: failures
                        // time out and are skipped).
                        val deadline = System.currentTimeMillis() + 120_000L
                        while (!player.isCached(track.videoId) && System.currentTimeMillis() < deadline) {
                            delay(750)
                        }
                    }
                }
            }
    }

    // Push the local playback state to the peer when the track, play/pause
    // state, or queue changes (position is sent as a best-effort snapshot).
    LaunchedEffect(syncManager) {
        player.state
            .map { s ->
                PlaybackSyncKey(
                    trackId = s.current?.videoId,
                    isPlaying = s.isPlaying,
                    isResolving = s.isResolving,
                    index = s.index,
                    queue = s.queue.map { it.videoId },
                    repeatMode = s.repeatMode.name,
                    isShuffle = s.isShuffle,
                )
            }
            .distinctUntilChanged()
            .collect { player.toPlaybackSnapshot()?.let { syncManager.updatePlayback(it) } }
    }

    // Push the in-app (VIVI) player volume to the peer. Polled (not event-
    // driven) so it also syncs when nothing is playing, and echo-guarded so a
    // locally-applied remote value isn't bounced straight back.
    LaunchedEffect(syncManager) {
        while (true) {
            // Unpaired: nothing to push. Idling here keeps the loop from
            // building snapshots (and poking the sync socket) several times a
            // second for the whole session, on the same machine that is playing
            // audio.
            if (!syncManager.paired.value) {
                delay(2_000L)
                continue
            }
            if (DesktopSettings.load().syncViviVolume) {
                val v = player.state.value.volume
                val isEcho = System.currentTimeMillis() < volumeGuard.echoUntil &&
                    abs(v - volumeGuard.echoValue) < 0.01f
                val changed = volumeGuard.lastPushed == null ||
                    abs(v - volumeGuard.lastPushed!!) > 0.001f
                if (!isEcho && changed) {
                    val s = player.state.value
                    val snapshot = player.toPlaybackSnapshot() ?: PlaybackSnapshot(volume = s.volume)
                    // Only mark as pushed when it was actually sent, so a dropped
                    // push (echo-suppression window) is retried on the next tick.
                    if (syncManager.updatePlayback(snapshot)) {
                        volumeGuard.lastPushed = v
                    }
                }
            }
            delay(500L)
        }
    }

    // Push immediately on user seeks so the peer follows to the same position.
    // Marked as a user seek so the receiver applies it exactly (both directions).
    LaunchedEffect(syncManager) {
        player.seekEvents.collect {
            player.toPlaybackSnapshot()?.copy(userSeek = true)?.let { syncManager.updatePlayback(it) }
        }
    }

    // Periodic re-sync: while playing, re-push the position every few seconds so
    // the peer auto-corrects drift (buffering / clock skew) instead of waiting
    // for the next discrete seek/play/track event.
    LaunchedEffect(syncManager) {
        var lastPushedPositionMs = -1L
        while (true) {
            delay(SyncServer.RESYNC_TICK_MS)
            if (!syncManager.paired.value) continue
            val s = player.state.value
            // Only push when the position actually advanced: a stalled/frozen
            // player must not repeatedly drag the peer back to the same point.
            if (s.isPlaying && s.positionMs != lastPushedPositionMs) {
                lastPushedPositionMs = s.positionMs
                player.toPlaybackSnapshot()?.let { syncManager.updatePlayback(it) }
            }
        }
    }

    // Poll the OS system volume and push changes to the peer (so changing the
    // Windows/Linux/mac volume controls the phone's system volume, and vice
    // versa). Echo-suppressed so a locally-applied remote value isn't bounced.
    // The native OS volume is its own channel, gated by "Sync volume": with it
    // off the two machines keep their own master volume (turning "Sync VIVI
    // volume" off only gates the in-app slider and never stopped this one).
    LaunchedEffect(syncManager, syncNativeVolume) {
        while (true) {
            // Unpaired: there is no peer to mirror the OS volume with, so skip
            // the native read (a COM round-trip on Windows) altogether instead
            // of doing it twice a second for nothing.
            if (!syncManager.paired.value || !syncNativeVolume) {
                delay(2_000L)
                continue
            }
            val sv = SystemVolume.get()
            if (sv != null) {
                val isEcho = System.currentTimeMillis() < systemVolumeGuard.echoUntil &&
                    abs(sv - systemVolumeGuard.echoValue) < 0.02f
                val changed = systemVolumeGuard.lastPushed == null ||
                    abs(sv - systemVolumeGuard.lastPushed!!) > 0.01f
                if (!isEcho && changed) {
                    val s = player.state.value
                    val snapshot = player.toPlaybackSnapshot() ?: PlaybackSnapshot(volume = s.volume)
                    if (syncManager.updatePlayback(snapshot.copy(systemVolume = sv))) {
                        systemVolumeGuard.lastPushed = sv
                    }
                }
            }
            delay(800L)
        }
    }

    // "Pause music when media is muted": when the OS output volume is muted
    // (or at zero) while VIVI is playing, pause; resume when it comes back.
    // Only reacts to state transitions (like the mobile volume listener), so a
    // manual play while muted is not fought.
    LaunchedEffect(Unit) {
        var lastMuted: Boolean? = null
        var pausedForMute = false
        while (true) {
            val enabled = DesktopSettings.load().pauseWhenMediaMuted
            if (enabled) {
                val muted = SystemVolume.isMuted() == true
                val volume = SystemVolume.get()
                val nowMuted = muted || (volume != null && volume <= 0.001f)
                if (nowMuted != lastMuted) {
                    lastMuted = nowMuted
                    if (nowMuted) {
                        if (player.state.value.isPlaying) {
                            pausedForMute = true
                            AppLog.log("playback", "OS output muted — pausing (pause when media is muted)")
                            player.toggle()
                        }
                    } else if (pausedForMute && !player.state.value.isPlaying) {
                        pausedForMute = false
                        AppLog.log("playback", "OS output unmuted — resuming")
                        player.toggle()
                    }
                }
            } else {
                lastMuted = null
                pausedForMute = false
            }
            delay(800L)
        }
    }

    // Latest peer playback snapshot. While WE are still resolving our stream,
    // incoming seek/play-pause is held off (our own resolution decides when
    // audio starts), so a command sent during that window is re-applied as soon
    // as our audio actually starts flowing instead of being dropped until the
    // next 5s periodic re-sync (which was especially noticeable on a slow
    // phone-hotspot connection where resolution takes a while).
    // Latest peer playback snapshot, stamped with the local receipt time so
    // the re-apply-after-resolving effect can tell whether it arrived while
    // we were buffering (and must be honored) or before we started (and is
    // stale — a pre-play snapshot must not pause a track the user just
    // started).
    val latestRemotePlayback =
        remember { java.util.concurrent.atomic.AtomicReference<Pair<Long, PlaybackSnapshot>?>(null) }

    // Apply incoming playback snapshots from the peer.
    LaunchedEffect(syncManager) {
        // The peer's last unambiguous play/pause intent: the `isPlaying` of the
        // most recent snapshot taken while the peer was NOT rebuffering. While
        // the peer is resolving, its `isPlaying` reads false for a moment even
        // though the user never pressed pause (see the track-change branch
        // below, which is the one that has to tell the two apart).
        var peerPlayIntent = false
        syncManager.incomingPlayback.collect { pb ->
            latestRemotePlayback.set(System.currentTimeMillis() to pb)
            // Remember the peer's clear intent for the next track change: a
            // snapshot taken while the peer is rebuffering says `isPlaying =
            // false` without the user having pressed pause, so it must not
            // overwrite the last real intent.
            if (!pb.isResolving) peerPlayIntent = pb.isPlaying
            // App (player) volume sync: mirror the peer's in-app volume slider.
            // A very recent local drag wins: the peer's value may be an echo of
            // our own push or a stale pre-drag snapshot, and re-applying it
            // snaps the slider back the moment the user lets go.
            if (DesktopSettings.load().syncViviVolume) {
                pb.volume?.let { v ->
                    if (System.currentTimeMillis() - localVolumeChangedAt > 2_000L) {
                        volumeGuard.echoUntil = System.currentTimeMillis() + 1500L
                        volumeGuard.echoValue = v
                        volumeGuard.lastPushed = v
                        if (abs(v - player.state.value.volume) > 0.001f) player.setVolume(v)
                    }
                }
            }
            // Native OS system volume sync: mirror the peer's system volume.
            // Its own channel, gated by "Sync volume": with that off neither
            // side's master volume follows the other (turning "Sync VIVI
            // volume" off only gates the in-app slider and never stopped this
            // one). Read from the settings file, not from a captured state:
            // this collector lives for the whole session.
            if (DesktopSettings.load().syncNativeVolume) {
                pb.systemVolume?.let { v ->
                    systemVolumeGuard.echoUntil = System.currentTimeMillis() + 1500L
                    systemVolumeGuard.echoValue = v
                    systemVolumeGuard.lastPushed = v
                    SystemVolume.set(v)
                }
            }
            // Repeat mode + shuffle sync (independent of the queue/position).
            pb.repeatMode?.let { mode ->
                runCatching { RepeatMode.valueOf(mode) }.getOrNull()?.let { player.setRepeatMode(it) }
            }
            pb.isShuffle?.let { player.setShuffle(it) }
            val currentId = player.state.value.current?.videoId
            val sameTrack = currentId != null && pb.trackId != null && pb.trackId == currentId
            // One line that says everything a sync report needs: what the peer
            // sent (track/queue/index/playing/resolving/pos/seek) AND what this
            // device held at that instant. Without the local half a dropped
            // command could not be told from an applied one (a bare 'seek=true' in
            // the recv line never says whether we already sat at that position, or were
            // still resolving our own stream and therefore deferred it).
            val localState = player.state.value
            AppLog.log(
                "sync",
                "recv: track='${pb.trackTitle ?: pb.trackId}' queue=${pb.queue.size} index=${pb.queueIndex} " +
                    "playing=${pb.isPlaying} resolving=${pb.isResolving} pos=${pb.positionMs} " +
                    "seek=${pb.userSeek} queueAt=${pb.queueUpdatedAt} " +
                    "localQueueAt=${syncManager.queueUpdatedAt()} " +
                    "localPos=${localState.positionMs} localPlaying=${localState.isPlaying} " +
                    "localResolving=${localState.isResolving} " +
                    (if (sameTrack) "[same track]" else "[track change] [local='${player.state.value.current?.title ?: "-"}']"),
            )
            if (currentId != null && pb.trackId != null && pb.trackId == currentId) {
                // The queue travels with the current track too. Both devices
                // sitting on the same song used to keep their OWN queues (the
                // desktop's often a single track), so of a whole synced queue
                // only the song that happened to match ever lined up. Adopt the
                // peer's queue in place — the track does not restart, only the
                // list around it is replaced.
                val localIds = player.state.value.queue.map { it.videoId }
                val remoteIds = pb.queue.map { it.id }
                if (remoteIds.isNotEmpty() && remoteIds != localIds) {
                    val newerQueue = pb.queueUpdatedAt <= 0L ||
                        pb.queueUpdatedAt >= syncManager.queueUpdatedAt()
                    if (newerQueue) {
                        val tracks = pb.queue.map { ref ->
                            NowPlaying(
                                videoId = ref.id,
                                title = ref.title,
                                artist = ref.artist.orEmpty(),
                                thumbnail = ref.thumbnail,
                                durationMs = ref.durationMs,
                            )
                        }
                        AppLog.log(
                            "sync",
                            "queue adopted from the peer: ${localIds.size} → ${tracks.size} track(s) " +
                                "(current stays '${pb.trackTitle ?: currentId}')",
                        )
                        // Recorded BEFORE the player state changes: the queue we
                        // are about to adopt is the peer's, so the desktop's own
                        // state handler must not read the change as a *local*
                        // edit and stamp a fresher last-write-wins time — that
                        // stamp then vetoed the peer's next skip (the "next does
                        // not sync, previous does" report: it is the very next
                        // queue change that gets rejected). The mobile notes the
                        // applied snapshot first for the same reason.
                        syncManager.noteQueueApplied(pb)
                        player.replaceQueuePreservingCurrent(
                            tracks,
                            player.state.value.positionMs,
                            player.state.value.isPlaying,
                        )
                    }
                }
                // Same track: lightweight seek (instant + precise), no restart.
                // While WE are still resolving this track our own resolution
                // decides when playback starts: ignore the peer's play/pause
                // echoes (the peer pauses only because we told it to hold) so we
                // don't pause ourselves out of the startup. Drift is corrected
                // by the re-apply effect below once we actually start playing.
                if (!player.state.value.isResolving) {
                    val target = syncManager.effectivePosition(pb)
                    // Over a slow phone-hotspot link a peer's periodic tick can
                    // arrive seconds late: a stale "paused" snapshot (older than
                    // a full sync tick) must not pause a track the user just
                    // started locally. The next fresh tick corrects anyway.
                    val staleAgeMs = syncManager.snapshotAgeMs(pb)
                    val stalePaused = !pb.isPlaying &&
                        !pb.userSeek &&
                        staleAgeMs != null && staleAgeMs > 4_000L
                    // Grace window after a local play/navigation command: the
                    // peer keeps echoing its pre-action "paused" state until it
                    // processes our play, and if that echo lands just after we
                    // finished resolving it pauses the track the user just
                    // started ("must press play twice when paired"). Within the
                    // window, a fresh peer "paused" snapshot is ignored.
                    val localPlayAgo = System.currentTimeMillis() - player.lastLocalPlayIntentAt
                    val gracePaused = !pb.isPlaying &&
                        !pb.userSeek &&
                        localPlayAgo >= 0L && localPlayAgo < 3_000L
                    when {
                        // A user seek is checked FIRST, before the resolving
                        // hold: a phone user seek always arrives that way — the
                        // seek makes the phone rebuffer, so its very snapshot
                        // carries `resolving=true`. As the second branch it fell
                        // into the hold and was dropped, and the follow-up tick
                        // (seek=false) is applied by the receiver as a
                        // *forward-only* catch-up, so a backward seek had no way
                        // back ("forward transmits, backward does not").
                        pb.userSeek -> {
                            AppLog.log(
                                "sync",
                                "applying peer USER SEEK: -> ${target}ms (was ${player.state.value.positionMs}ms), " +
                                    "playing=${pb.isPlaying} peerResolving=${pb.isResolving} (exact, both directions)",
                            )
                            player.seekRemote(target, pb.isPlaying, toleranceMs = 0L)
                        }
                        pb.isResolving -> {
                            // Peer is mid-song buffering (position frozen): keep
                            // playing and skip the seek instead of pausing, so a
                            // brief rebuffer on the phone doesn't stop the desktop.
                            AppLog.log("sync", "held: peer is resolving (position frozen), keeping local playback")
                        }
                        stalePaused -> {
                            // Ignored: stale peer "paused" snapshot (see above).
                            AppLog.log("sync", "ignored a stale peer 'paused' snapshot (age ${staleAgeMs}ms)")
                        }
                        gracePaused -> {
                            // Ignored: peer pre-action "paused" echo (see above).
                            // The next fresh tick applies the peer's real state.
                            AppLog.log("sync", "ignored a peer 'paused' echo inside the local play grace window (${localPlayAgo}ms)")
                        }
                        else -> {
                            // Periodic drift tick: forward-only catch-up.
                            if (pb.isPlaying && target - player.state.value.positionMs > SyncServer.RESYNC_TOLERANCE_MS) {
                                AppLog.log("sync", "drift catch-up: ${player.state.value.positionMs}ms -> ${target}ms")
                            }
                            player.seekRemoteCatchUp(target, pb.isPlaying, SyncServer.RESYNC_TOLERANCE_MS)
                        }
                    }
                } else {
                    // We are still resolving our OWN stream, so the peer's
                    // play/pause and position are deferred (our own resolution
                    // decides when audio starts). Say so explicitly: without
                    // this line the snapshot is logged in and then nothing
                    // happens, which reads as "the command vanished". The
                    // re-apply effect below picks it up the moment the stream is
                    // ready.
                    AppLog.log(
                        "sync",
                        "deferred (our own stream is resolving): peer playing=${pb.isPlaying} " +
                            "seek=${pb.userSeek} peerResolving=${pb.isResolving} -> re-applied when ready",
                    )
                }
            } else {
                // Last-write-wins for the queue: only replace the local queue if
                // the remote edit is newer (or unknown, from an older peer).
                // Volume/position sync above still runs regardless.
                val newerQueue = pb.queueUpdatedAt <= 0L || pb.queueUpdatedAt >= syncManager.queueUpdatedAt()
                val localPlayAgo = System.currentTimeMillis() - player.lastLocalPlayIntentAt
                val inGraceWindow = localPlayAgo >= 0L && localPlayAgo < 3_000L
                if (newerQueue && !(inGraceWindow && !pb.isPlaying && !pb.isResolving)) {
                    val tracks = pb.queue.map { ref ->
                        NowPlaying(videoId = ref.id, title = ref.title, artist = ref.artist.orEmpty(), thumbnail = ref.thumbnail, durationMs = ref.durationMs)
                    }
                    if (tracks.isNotEmpty()) {
                        // A skipped track on the phone arrives as
                        // `playing=false resolving=true`: the skip itself makes
                        // the new stream rebuffer, and the phone's `isPlaying`
                        // (playWhenReady && playbackState != ended) is briefly
                        // false while the media item is swapped in. Reading that
                        // as a *pause* left the desktop silent on the new track
                        // until play was pressed ("the song changes on the
                        // desktop but I have to press play"): every later
                        // `playing=true` tick landed while the desktop was still
                        // resolving its own new stream, so it was deferred, and
                        // the deferral re-applies a *position*, not an intent.
                        // The phone's skip path (seekToNext / seekToPrevious)
                        // always forces playWhenReady = true, so a track change
                        // that arrives mid-rebuffer inherits the peer's last
                        // clear intent. A change from a peer that was really
                        // paused (first pair, queue edit while paused) still
                        // stays paused.
                        val peerPlaying = pb.isPlaying || (pb.isResolving && peerPlayIntent)
                        AppLog.log(
                            "sync",
                            "track change applied from the peer: '${pb.trackTitle ?: pb.trackId}' " +
                                "(index ${pb.queueIndex.coerceAtLeast(0)} of ${tracks.size}), " +
                                "playing=${pb.isPlaying} resolving=${pb.isResolving} " +
                                "startPlaying=$peerPlaying at ${syncManager.effectivePosition(pb)}ms",
                        )
                        // Same ordering rule as above: the applied snapshot is
                        // recorded first, so the queue change that follows is not
                        // mistaken for a local edit of ours.
                        syncManager.noteQueueApplied(pb)
                        player.applyRemotePlayback(tracks, pb.queueIndex, syncManager.effectivePosition(pb), peerPlaying, pb.isResolving)
                    }
                }
            }
        }
    }

    // Re-apply the peer's latest snapshot the moment our stream finishes
    // resolving: a seek/play-pause received while we were still buffering was
    // held off above, so without this it would be lost until the next periodic
    // re-sync (slow over a phone hotspot). Only same-track corrections are
    // re-applied; a changed queue is handled by the collector above.
    LaunchedEffect(syncManager) {
        var wasResolving = player.state.value.isResolving
        var resolvingSince = if (wasResolving) System.currentTimeMillis() else 0L
        player.state.map { it.isResolving }.distinctUntilChanged().collect { resolving ->
            if (resolving) {
                resolvingSince = System.currentTimeMillis()
            } else if (wasResolving) {
                // Only honor a snapshot that arrived WHILE we were buffering: a
                // pre-play snapshot (received before we started resolving) is
                // stale and must not pause a track the user just started — that
                // left the button stuck and forced a second play press.
                val (receivedAt, pb) = latestRemotePlayback.get() ?: (0L to null)
                val currentId = player.state.value.current?.videoId
                // A user seek is a discrete command, not drift: it is honoured
                // even when it arrived just BEFORE we started resolving (the
                // `receivedAt >= resolvingSince` guard exists to drop a stale
                // pre-play *drift* tick, which a seek never is).
                if (pb != null && !pb.isResolving &&
                    currentId != null && pb.trackId == currentId &&
                    (receivedAt >= resolvingSince || pb.userSeek)
                ) {
                    val target = syncManager.effectivePosition(pb)
                    AppLog.log(
                        "sync",
                        "stream ready — re-applying the snapshot received while buffering: " +
                            "-> ${target}ms playing=${pb.isPlaying} seek=${pb.userSeek}",
                    )
                    if (pb.userSeek) {
                        player.seekRemote(target, pb.isPlaying, toleranceMs = 0L)
                    } else {
                        player.seekRemoteCatchUp(target, pb.isPlaying, SyncServer.RESYNC_TOLERANCE_MS)
                    }
                }
            }
            wasResolving = resolving
        }
    }

    // Apply incoming settings snapshots from the peer.
    LaunchedEffect(syncManager) {
        syncManager.incomingSettings.collect { settings ->
            settings["darkMode"]?.let { mode ->
                onThemeModeChange(
                    when (mode) {
                        "ON" -> ThemeMode.DARK
                        "OFF" -> ThemeMode.LIGHT
                        else -> ThemeMode.SYSTEM
                    }
                )
            }
            settings["appLanguage"]?.let { lang ->
                val normalized = Languages.fromMobileCode(lang)
                if (lang != "SYSTEM_DEFAULT" && Languages.all.any { it.code == normalized }) {
                    // Bidirectional language sync, pair-hijack-proof: the peer's
                    // snapshot only carries a language that *its user* changed,
                    // identified by (deviceId, per-device sequence). Stale or
                    // echoed values (our own deviceId, an older sequence, or a
                    // legacy peer that sends no sequence at all) never override
                    // the desktop's own choice — that is what kept forcing
                    // English back on every fresh pair.
                    val peerId = settings["languageDeviceId"].orEmpty()
                    val peerSeq = settings["languageSeq"]?.toLongOrNull() ?: 0L
                    if (peerId.isNotBlank() && peerSeq > 0L) {
                        val s = DesktopSettings.load()
                        val staleEcho = peerId == s.deviceId ||
                            (peerId == s.languagePeerId && peerSeq <= s.languagePeerSeq)
                        if (!staleEcho) {
                            // Remember which peer change we accepted (so a repeat
                            // push of the same change is ignored), then adopt the
                            // language through the normal change path.
                            DesktopSettings.update {
                                it.copy(languagePeerId = peerId, languagePeerSeq = peerSeq)
                            }
                            onLanguageChange(normalized)
                        }
                    }
                }
            }
            settings["selectedThemeColor"]?.toIntOrNull()?.let { argb ->
                onAccentChange(argbIntToColor(argb))
            }
            settings["syncViviVolume"]?.toBooleanStrictOrNull()?.let { v ->
                syncViviVolume = v
                DesktopSettings.update { it.copy(syncViviVolume = v) }
            }
            // "Sync OS volume" is deliberately NOT read from the peer: it is a
            // per-device choice about THIS machine's own master volume, and
            // syncing it made the switch flip itself back on — the peer keeps
            // pushing its stored value (default on) while our own push is still
            // in flight, so the local toggle lost the race and re-enabled
            // itself. Each side decides for itself and the send/receive gates
            // below honour it, which is what stops the volume crossing.
        }
    }

    // Push the local settings when they change (also once on startup).
    LaunchedEffect(syncManager, language, themeMode, accent, syncViviVolume) {
        syncManager.updateSettings(desktopSettingsMap(language, themeMode, accent, syncViviVolume))
    }

    // Library sync: push the local playlists and the liked songs whenever either
    // changes, and apply the peer's (last-write-wins per playlist, and per liked
    // song id through `updatedAt`).
    //
    // `SongActions.likedVersion` only moves when the USER likes or unlikes
    // something here, so a like learned from the phone is never sent back.
    LaunchedEffect(syncManager) {
        combine(PlaylistStore.all, SongActions.likedVersion) { playlists, _ -> playlists }.collect {
            syncManager.updateLibrary(
                LibrarySnapshot(
                    // The flat list is what an older peer reads; the entries carry
                    // the edit times (and the metadata of a like made here) that
                    // make an unlike or a re-like land correctly (issue #96).
                    songIds = SongActions.likedIds(),
                    likedSongs = SongActions.toSyncedLiked(),
                    playlists = PlaylistStore.toSynced(),
                )
            )
        }
    }
    LaunchedEffect(syncManager) {
        // The last library the peer sent us survives a restart (the sync manager
        // keeps it in the settings), so the hearts are already right before the
        // first snapshot of this session arrives.
        DesktopSettings.load().library?.let { last ->
            SongActions.applyRemoteLiked(last.likedSongs, last.songIds)
        }
        syncManager.incomingLibrary.collect { lib ->
            lib ?: return@collect
            PlaylistStore.applyRemote(lib.playlists)
            SongActions.applyRemoteLiked(lib.likedSongs, lib.songIds)
        }
    }

    // UI density scale: multiply the density so every dp-based measurement
    // zooms (200% down to 55%), matching the Android density setting.
    // CALIBRATION: the desktop layout is drawn generously, so the "100%"
    // preset would look oversized next to the phone; scale 100% down to the
    // size the mobile UI has at 75% (0.75x), keeping the relative presets.
    val baseDensity = LocalDensity.current
    // The window's width before any density scaling: what the user sees when
    // they say "the window got very small". The density steps down (and the two
    // panels compress) from it, and the stored setting is never rewritten, so a
    // resized-back window restores the user's own choice exactly.
    // `LocalWindowInfo.containerSize` is the live content size in raw pixels and
    // recomposes on every resize, so the layout follows the dragged edge.
    val windowWidthPx = LocalWindowInfo.current.containerSize.width
    val unscaledWindowWidthDp = if (windowWidthPx > 0) windowWidthPx / baseDensity.density else 1280f
    val effectiveDensityScale = effectiveDensityScale(densityScale, unscaledWindowWidthDp)
    val narrowSidebar = unscaledWindowWidthDp < SIDEBAR_AUTO_COLLAPSE_DP
    val narrowRightPanel = unscaledWindowWidthDp < RIGHT_PANEL_AUTO_HIDE_DP
    CompositionLocalProvider(
        LocalDensity provides Density(baseDensity.density * effectiveDensityScale * DENSITY_CALIBRATION, baseDensity.fontScale),
        // The right-click menu is Compose Desktop's own flat popup; the Material 3
        // representation replaces it everywhere so every context menu (text
        // selection and ContextMenuArea alike) matches the app.
        LocalContextMenuRepresentation provides Material3ContextMenuRepresentation,
    ) {
    CompositionLocalProvider(
        LocalPlayback provides PlaybackContext(
            videoId = nowPlaying?.videoId,
            isPlaying = isPlaying,
            audioLevel = player.audioLevel,
            bufferedFraction = player.bufferedFraction,
            pendingSeekFraction = player.pendingSeekFraction,
        )
    ) {
    WindowDragSurface(nativeTitleBar = nativeTitleBar)
    Row(
        Modifier
            .fillMaxSize()
            // Spotify mode: the flat background (#121212) sits behind the
            // `surfaceContainer` panels (#181818) so the 3-panel shell gets
            // depth; the M3 path keeps the tonal background as before.
            .background(MaterialTheme.colorScheme.background)
            .onKeyEvent(onGlobalKey)
    ) {
        if (spotifyLayout) {
            AnimatedVisibility(
                // Collapsed state compresses the sidebar to a compact icon rail
                // (72dp) instead of hiding it; it is fully hidden only on the
                // full-screen player.
                visible = current != Screen.Player,
                enter = Animations.panelEnter(),
                exit = Animations.panelExit(),
            ) {
                Surface(
                    modifier = Modifier.fillMaxHeight().padding(start = 6.dp, top = 6.dp, end = 4.dp, bottom = 6.dp),
                    // Spotify mode: the black sidebar floats directly on the
                    // flat background (no surface frame around it).
                    color = if (spotifyLayout) Color.Transparent else MaterialTheme.colorScheme.surfaceContainer,
                    shape = RoundedCornerShape(12.dp),
                ) {
                    Sidebar(
                        hideHistory = pauseListenHistory,
                        language = language,
                        current = current,
                        collapsed = sidebarCollapsed || narrowSidebar,
                        showTitleHeader = false,
                        userName = displayUserName,
                        userHandle = displayUserHandle,
                        isLoggedIn = isLoggedIn,
                        spotify = spotifyLayout,
                        onToggleCollapsed = {
                            sidebarCollapsed = !sidebarCollapsed
                            DesktopSettings.update { it.copy(sidebarCollapsed = sidebarCollapsed) }
                        },
                        onSelect = openRoot,
                    )
                }
            }
        } else {
            AnimatedVisibility(
                visible = current != Screen.Player,
                enter = Animations.panelEnter(),
                exit = Animations.panelExit(),
            ) {
                Sidebar(
                    hideHistory = pauseListenHistory,
                    language = language,
                    current = current,
                    collapsed = sidebarCollapsed || narrowSidebar,
                    userName = displayUserName,
                    userHandle = displayUserHandle,
                    isLoggedIn = isLoggedIn,
                    spotify = spotifyLayout,
                    onToggleCollapsed = {
                        sidebarCollapsed = !sidebarCollapsed
                        DesktopSettings.update { it.copy(sidebarCollapsed = sidebarCollapsed) }
                    },
                    onSelect = openRoot,
                )
            }
        }
        Column(
            Modifier
                .weight(1f)
                .fillMaxHeight()
        ) {
            if (spotifyLayout && current != Screen.Player) {
                SpotifyTopHeader(
                    language = language,
                    canGoBack = backStack.size > 1,
                    onBack = goBack,
                    onOpenSearch = { navigate(Screen.Search) },
                    onOpenHome = { openRoot(Screen.Home) },
                    onOpenHistory = { navigate(Screen.History) },
                    onOpenStats = { navigate(Screen.SettingsWrapped) },
                    onOpenListenTogether = { openRoot(Screen.ListenTogether) },
                    onOpenSettings = { openRoot(Screen.Settings) },
                    onOpenLyrics = { navigate(Screen.Lyrics) },
                    onOpenQueue = { openRoot(Screen.Queue) },
                    onMinimize = onMinimize,
                    onMaximize = onMaximize,
                    onClose = onClose,
                    isMaximized = isMaximized,
                    // With the native title bar the OS provides the window
                    // controls, so VIVI's bar hides its own buttons.
                    showWindowControls = !nativeTitleBar,
                    searchQuery = headerSearchQuery,
                    onSearchQueryChange = { newQ -> headerSearchQuery = newQ },
                    onSearchSubmit = { q -> recordSearch(q) },
                    selectedFilter = headerSearchFilter,
                    onFilterSelect = { f -> headerSearchFilter = f },
                )
            }
            Row(Modifier.weight(1f).fillMaxWidth().padding(if (spotifyLayout) androidx.compose.foundation.layout.PaddingValues(horizontal = 2.dp) else androidx.compose.foundation.layout.PaddingValues(0.dp))) {
            Surface(
                modifier = Modifier.weight(1f).fillMaxHeight().padding(if (spotifyLayout) androidx.compose.foundation.layout.PaddingValues(start = 2.dp, top = 0.dp, end = 2.dp, bottom = 6.dp) else androidx.compose.foundation.layout.PaddingValues(0.dp)),
                color = if (spotifyLayout) MaterialTheme.colorScheme.surfaceContainer else Color.Transparent,
                shape = if (spotifyLayout) RoundedCornerShape(12.dp) else androidx.compose.ui.graphics.RectangleShape,
            ) {
                    Box(Modifier.fillMaxSize()) {
                        AnimatedContent(
                            targetState = current,
                            transitionSpec = {
                                if (!animationsEnabled) {
                                    fadeIn(animationSpec = tween(0)) togetherWith fadeOut(animationSpec = tween(0))
                                } else if (targetState == Screen.Player || initialState == Screen.Player) {
                                    // Opening and closing the full player reads as
                                    // the bar expanding upwards (the bottom bar's
                                    // own exit goes the other way), instead of
                                    // being one more horizontal screen slide.
                                    Animations.playerEnter() togetherWith Animations.playerExit()
                                } else {
                                    when (screenTransition) {
                                        "slide" -> (slideInHorizontally(animationSpec = tween(Animations.ms(220))) { it / 4 } + fadeIn(animationSpec = tween(Animations.ms(220)))) togetherWith
                                            (slideOutHorizontally(animationSpec = tween(Animations.ms(220))) { -it / 4 } + fadeOut(animationSpec = tween(Animations.ms(220))))
                                        "off" -> fadeIn(animationSpec = tween(0)) togetherWith fadeOut(animationSpec = tween(0))
                                        else -> fadeIn(animationSpec = tween(Animations.ms(180))) togetherWith fadeOut(animationSpec = tween(Animations.ms(180)))
                                    }
                                }
                            },
                            label = "screenTransition",
                        ) { screen ->
                when (screen) {
                    is Screen.Home -> HomeScreen(
                        language = language,
                        onOpenAlbum = { navigate(Screen.Album(it)) },
                        onOpenArtist = { navigate(Screen.Artist(it)) },
                        onOpenPlaylist = { navigate(Screen.Playlist(it)) },
                        onPlaySong = playSong,
                        onAddToPlaylist = addToPlaylist,
                        onPlayAll = playAll,
                        onOpenBrowse = { browseId, params -> navigate(Screen.Browse(browseId, params)) },
                        userName = displayUserName,
                        useLastListen = homeUseLastListen,
                        onUseLastListenChange = { v ->
                            homeUseLastListen = v
                            DesktopSettings.update { it.copy(homeUseLastListen = v) }
                        },
                        randomizeOrder = randomizeHomeOrder,
                        onRandomizeOrderChange = { v ->
                            randomizeHomeOrder = v
                            DesktopSettings.update { it.copy(randomizeHomeOrder = v) }
                        },
                        wrappedStats = WrappedStats(
                            trackStarts = sessionTrackStarts,
                            playedMs = sessionPlayedMs,
                            topSongTitle = sessionTopSong?.second,
                            topSongCount = sessionTopCount,
                        ),
                        showWrapped = showWrappedOnHome,
                        recentSeedTracks = recentSeedTracks,
                    )
                    is Screen.Search -> SearchScreen(
                        language = language,
                        gridItemSize = gridItemSize,
                        onOpenAlbum = { navigate(Screen.Album(it)) },
                        onOpenArtist = { navigate(Screen.Artist(it)) },
                        onOpenPlaylist = { navigate(Screen.Playlist(it)) },
                        onPlaySong = playSong,
                        onAddToQueue = addToQueue,
                        onAddToPlaylist = addToPlaylist,
                        searchHistory = if (pauseSearchHistory) emptyList() else searchHistory,
                        onRecordSearch = recordSearch,
                        onClearSearchHistory = {
                            searchHistory = emptyList()
                            DesktopSettings.update { it.copy(searchHistory = emptyList()) }
                        },
                        externalQuery = headerSearchQuery,
                        onQueryChange = { newQ -> headerSearchQuery = newQ },
                        showTextField = !spotifyLayout,
                        externalFilter = headerSearchFilter,
                        onFilterChange = { f -> headerSearchFilter = f },
                        showFiltersInBody = !spotifyLayout,
                    )
                    is Screen.Library -> LibraryScreen(
                        language = language,
                        isLoggedIn = isLoggedIn,
                        gridItemSize = gridItemSize,
                        onLoggedIn = {
                            isLoggedIn = true
                            accountName = DesktopSettings.load().accountName
                        },
                        onOpenAlbum = { navigate(Screen.Album(it)) },
                        onOpenArtist = { navigate(Screen.Artist(it)) },
                        onOpenPlaylist = { navigate(Screen.Playlist(it)) },
                        onPlaySong = playSong,
                        onAddToQueue = addToQueue,
                        onAddToPlaylist = addToPlaylist,
                        onShuffleAll = shuffleAll,
                    )
                    is Screen.History -> HistoryScreen(
                        language = language,
                        onBack = goBack,
                        onPlaySong = playSong,
                        onAddToQueue = addToQueue,
                        onAddToPlaylist = addToPlaylist,
                        // Tapping a remembered search reopens it in the Search
                        // screen, which is what makes the local history useful.
                        onSearch = { term ->
                            headerSearchQuery = term
                            navigate(Screen.Search)
                        },
                    )
                    is Screen.NewReleases -> NewReleasesScreen(
                        language = language,
                        gridItemSize = gridItemSize,
                        onBack = goBack,
                        onOpenAlbum = { navigate(Screen.Album(it)) },
                    )
                    is Screen.Charts -> ChartsScreen(
                        language = language,
                        onBack = goBack,
                        onOpenAlbum = { navigate(Screen.Album(it)) },
                        onOpenArtist = { navigate(Screen.Artist(it)) },
                        onOpenPlaylist = { navigate(Screen.Playlist(it)) },
                        onPlaySong = playSong,
                        onAddToQueue = addToQueue,
                        onAddToPlaylist = addToPlaylist,
                    )
                    is Screen.MoodGenres -> MoodGenresScreen(
                        language = language,
                        onBack = goBack,
                        onOpenBrowse = { browseId, params -> navigate(Screen.Browse(browseId, params)) },
                    )
                    is Screen.SongRecognition -> SongRecognitionScreen(
                        language = language,
                        onBack = goBack,
                        history = recognitionHistory,
                        onHistoryChange = { h ->
                            recognitionHistory = h
                            DesktopSettings.update { it.copy(recognitionHistory = h) }
                        },
                    )
                    is Screen.Settings -> SettingsScreen(
                        language = language,
                        themeMode = themeMode,
                        isLoggedIn = isLoggedIn,
                        accountName = accountName,
                        updateStatus = updateStatus,
                        wrappedStats = WrappedStats(
                            trackStarts = sessionTrackStarts,
                            playedMs = sessionPlayedMs,
                            topSongTitle = sessionTopSong?.second,
                            topSongCount = sessionTopCount,
                        ),
                        onOpen = navigate,
                    )
                    is Screen.SettingsLanguage -> SettingsLanguageScreen(
                        language = language,
                        onBack = goBack,
                        onLanguageChange = onLanguageChange,
                    )
                    is Screen.SettingsAppearance -> SettingsAppearanceScreen(
                        language = language,
                        onBack = goBack,
                        onOpenTheme = { navigate(Screen.SettingsTheme) },
                        onOpenFont = { navigate(Screen.SettingsFont) },
                        onOpenCanvas = { navigate(Screen.SettingsCanvas) },
                        onOpenDensity = { navigate(Screen.SettingsDensity) },
                        onOpenTransitions = { navigate(Screen.SettingsTransitions) },
                        onOpenPlayerDesign = { navigate(Screen.SettingsPlayerDesign) },
                        onOpenIntro = { navigate(Screen.SettingsIntro) },
                        nativeTitleBar = nativeTitleBar,
                        onNativeTitleBarChange = onNativeTitleBarChange,
                        showRightSidebar = showRightSidebar,
                        onShowRightSidebarChange = { v ->
                            showRightSidebar = v
                            DesktopSettings.update { it.copy(showRightSidebar = v) }
                        },
                        onRestart = onRestart,
                    )
                    is Screen.SettingsFont -> SettingsFontScreen(
                        language = language,
                        onBack = goBack,
                        selectedFont = font,
                        onFontChange = onFontChange,
                        customFontPath = customFontPath,
                        onImportFont = onImportFont,
                        useFontInLyrics = lyricsUseAppFont,
                        onUseFontInLyricsChange = { on ->
                            lyricsUseAppFont = on
                            DesktopSettings.update { it.copy(lyricsUseAppFont = on) }
                        },
                    )
                    is Screen.SettingsCanvas -> SettingsCanvasScreen(
                        language = language,
                        onBack = goBack,
                        canvasEnabled = canvasEnabled,
                        onCanvasEnabledChange = { enabled ->
                            canvasEnabled = enabled
                            DesktopSettings.update { it.copy(canvasEnabled = enabled) }
                        },
                        canvasSource = canvasSource,
                        onCanvasSourceChange = { s ->
                            canvasSource = s
                            DesktopSettings.update { it.copy(canvasSource = s.key) }
                        },
                    )
                    is Screen.SettingsDensity -> SettingsDensityScreen(
                        language = language,
                        onBack = goBack,
                        densityScale = densityScale,
                        onDensityScaleChange = { s ->
                            densityScale = s
                            DesktopSettings.update { it.copy(densityScale = s) }
                        },
                        gridItemSize = gridItemSize,
                        onGridItemSizeChange = { g ->
                            gridItemSize = g
                            DesktopSettings.update { it.copy(gridItemSize = g) }
                        },
                    )
                    is Screen.SettingsTransitions -> SettingsTransitionsScreen(
                        language = language,
                        onBack = goBack,
                        screenTransition = screenTransition,
                        onScreenTransitionChange = { t ->
                            screenTransition = t
                            DesktopSettings.update { it.copy(screenTransition = t) }
                        },
                        animationsEnabled = animationsEnabled,
                        onAnimationsEnabledChange = { v ->
                            animationsEnabled = v
                            // The transition helpers read this directly, so the
                            // change is visible on the next frame instead of
                            // waiting for the settings file reload.
                            Animations.enabled = v
                            DesktopSettings.update { it.copy(animationsEnabled = v) }
                        },
                        animationSpeed = animationSpeed,
                        onAnimationSpeedChange = { v ->
                            animationSpeed = v
                            Animations.speed = v
                            DesktopSettings.update { it.copy(animationSpeed = v) }
                        },
                    )
                    is Screen.SettingsPlayerDesign -> SettingsPlayerDesignScreen(
                        language = language,
                        onBack = goBack,
                        design = playerDesign,
                        onDesignChange = { d ->
                            playerDesign = d
                            // The legacy boolean is written in step so an older
                            // build reading the same file still sees the finish
                            // it knew as a switch (see DesktopSettings.migrate).
                            DesktopSettings.update {
                                it.copy(
                                    playerDesign = d.key,
                                    expressiveTabTranslucent = d == PlayerDesign.EXPRESSIVE_TRANSLUCENT,
                                )
                            }
                        },
                        background = playerBackground,
                        onBackgroundChange = { b ->
                            playerBackground = b
                            DesktopSettings.update { it.copy(playerBackground = b.key) }
                        },
                        rotatingThumbnail = rotatingThumbnail,
                        onRotatingThumbnailChange = { r ->
                            rotatingThumbnail = r
                            DesktopSettings.update { it.copy(rotatingThumbnail = r) }
                        },
                        miniPlayerDesign = miniPlayerDesign,
                        onMiniPlayerDesignChange = { d ->
                            miniPlayerDesign = d
                            DesktopSettings.update { it.copy(miniPlayerDesign = d.key) }
                        },
                        miniPlayerBackgroundStyle = miniPlayerBackgroundStyle,
                        onMiniPlayerBackgroundStyleChange = { b ->
                            miniPlayerBackgroundStyle = b
                            DesktopSettings.update { it.copy(miniPlayerBackgroundStyle = b.key) }
                        },
                        pureBlackMiniPlayer = pureBlackMiniPlayer,
                        onPureBlackMiniPlayerChange = { p ->
                            pureBlackMiniPlayer = p
                            DesktopSettings.update { it.copy(pureBlackMiniPlayer = p) }
                        },
                        sliderStyle = sliderStyle,
                        onSliderStyleChange = { s ->
                            sliderStyle = s
                            DesktopSettings.update { it.copy(sliderStyle = s) }
                        },
                        swipeThumbnail = swipeThumbnail,
                        onSwipeThumbnailChange = { s ->
                            swipeThumbnail = s
                            DesktopSettings.update { it.copy(swipeThumbnail = s) }
                        },
                        swipeSensitivity = swipeSensitivity,
                        onSwipeSensitivityChange = { s ->
                            swipeSensitivity = s
                            DesktopSettings.update { it.copy(swipeSensitivity = s) }
                        },
                    )
                    is Screen.SettingsTheme -> SettingsThemeScreen(
                        language = language,
                        onBack = goBack,
                        themeMode = themeMode,
                        accent = accent,
                        onThemeModeChange = onThemeModeChange,
                        onAccentChange = onAccentChange,
                        accentIntensity = accentIntensity,
                        onAccentIntensityChange = onAccentIntensityChange,
                        onAccentIntensityChangeFinished = onAccentIntensityChangeFinished,
                        pureBlack = pureBlack,
                        onPureBlackChange = onPureBlackChange,
                        customAccents = customAccents,
                        // Forward the stateful callbacks from the window level
                        // (they update the live list AND persist it); duplicating
                        // them here with DesktopSettings.update only persisted
                        // the change, so new swatches appeared only after
                        // leaving and re-entering the screen.
                        onAddCustomAccent = onAddCustomAccent,
                        onRemoveCustomAccent = onRemoveCustomAccent,
                    )
                    is Screen.SettingsPlayer -> SettingsPlayerScreen(
                        language = language,
                        onBack = goBack,
                        autoPlayNext = autoPlayNext,
                        onToggleAutoPlayNext = { checked ->
                            autoPlayNext = checked
                            DesktopSettings.update { it.copy(autoPlayNext = checked) }
                        },
                        autoLoadMore = autoLoadMore,
                        onToggleAutoLoadMore = { checked ->
                            autoLoadMore = checked
                            player.autoLoadMore = checked
                            DesktopSettings.update { it.copy(autoLoadMore = checked) }
                        },
                        preventDuplicateTracksInQueue = preventDuplicateTracksInQueue,
                        onTogglePreventDuplicateTracksInQueue = { checked ->
                            preventDuplicateTracksInQueue = checked
                            player.preventDuplicateTracksInQueue = checked
                            DesktopSettings.update { it.copy(preventDuplicateTracksInQueue = checked) }
                        },
                        autoSkipNextOnError = autoSkipNextOnError,
                        onToggleAutoSkipNextOnError = { checked ->
                            autoSkipNextOnError = checked
                            player.autoSkipNextOnError = checked
                            DesktopSettings.update { it.copy(autoSkipNextOnError = checked) }
                        },
                        pauseWhenMediaMuted = pauseWhenMediaMuted,
                        onTogglePauseWhenMediaMuted = { checked ->
                            pauseWhenMediaMuted = checked
                            DesktopSettings.update { it.copy(pauseWhenMediaMuted = checked) }
                        },
                        keepScreenOnWhenPlayerExpanded = keepScreenOnWhenPlayerExpanded,
                        onToggleKeepScreenOnWhenPlayerExpanded = { checked ->
                            keepScreenOnWhenPlayerExpanded = checked
                            DesktopSettings.update { it.copy(keepScreenOnWhenPlayerExpanded = checked) }
                        },
                        persistentShuffle = persistentShuffle,
                        onTogglePersistentShuffle = { checked ->
                            persistentShuffle = checked
                            player.persistentShuffleAcrossQueues = checked
                            DesktopSettings.update { it.copy(persistentShuffle = checked) }
                        },
                        progressiveSeek = progressiveSeek,
                        onToggleProgressiveSeek = { checked ->
                            progressiveSeek = checked
                            DesktopSettings.update { it.copy(progressiveSeek = checked) }
                        },
                        autoDownloadOnLike = autoDownloadOnLike,
                        onToggleAutoDownloadOnLike = { checked ->
                            autoDownloadOnLike = checked
                            DesktopSettings.update { it.copy(autoDownloadOnLike = checked) }
                        },
                        historyDurationSeconds = historyDurationSeconds,
                        onHistoryDurationSecondsChange = { v ->
                            historyDurationSeconds = v
                            DesktopSettings.update { it.copy(historyDurationSeconds = v) }
                        },
                        skipSilence = skipSilence,
                        onToggleSkipSilence = { checked ->
                            skipSilence = checked
                            // The "Instantly skip silence" option is a derivative of
                            // the master toggle: turning the master off disables it too.
                            if (!checked) {
                                skipSilenceInstant = false
                                DesktopSettings.update { it.copy(skipSilence = false, skipSilenceInstant = false) }
                            } else {
                                DesktopSettings.update { it.copy(skipSilence = true) }
                            }
                        },
                        skipSilenceInstant = skipSilenceInstant,
                        onToggleSkipSilenceInstant = { checked ->
                            skipSilenceInstant = checked
                            DesktopSettings.update { it.copy(skipSilenceInstant = checked) }
                        },
                        crossfade = crossfade,
                        onToggleCrossfade = { checked ->
                            crossfade = checked
                            DesktopSettings.update { it.copy(crossfade = checked) }
                        },
                        crossfadeDurationSeconds = crossfadeDurationSeconds,
                        onCrossfadeDurationSecondsChange = { v ->
                            crossfadeDurationSeconds = v
                            DesktopSettings.update { it.copy(crossfadeDurationSeconds = v) }
                        },
                        disableCrossfadeGapless = disableCrossfadeGapless,
                        onToggleDisableCrossfadeGapless = { checked ->
                            disableCrossfadeGapless = checked
                            DesktopSettings.update { it.copy(disableCrossfadeGapless = checked) }
                        },
                        audioQuality = audioQuality,
                        onAudioQualityChange = { q ->
                            audioQuality = q
                            DesktopSettings.update { it.copy(audioQuality = q) }
                        },
                        rememberShuffleRepeat = rememberShuffleRepeat,
                        onToggleRememberShuffleRepeat = { checked ->
                            rememberShuffleRepeat = checked
                            DesktopSettings.update { it.copy(rememberShuffleRepeat = checked) }
                        },
                        persistentQueue = persistentQueue,
                        onTogglePersistentQueue = { checked ->
                            persistentQueue = checked
                            DesktopSettings.update { it.copy(persistentQueue = checked) }
                        },
                        onOpenEqualizer = { navigate(Screen.SettingsEqualizer) },
                        streamCacheMinutes = streamCacheMinutes,
                        onStreamCacheMinutesChange = { m ->
                            streamCacheMinutes = m
                            DesktopSettings.update { it.copy(streamCacheMinutes = m) }
                        },
                    )
                    is Screen.SettingsEqualizer -> {
                        val s = DesktopSettings.load()
                        SettingsEqualizerScreen(
                            language = language,
                            onBack = goBack,
                            profiles = s.eqProfiles,
                            activeProfileId = s.activeEqProfileId,
                            onSelectProfile = { id ->
                                DesktopSettings.update { it.copy(activeEqProfileId = id ?: "") }
                                player.setEqualizer(s.eqProfiles.find { p -> p.id == id })
                            },
                            onImportProfile = { name, eq ->
                                val profile = SavedEQProfile(
                                    id = "custom_${System.currentTimeMillis()}",
                                    name = name,
                                    deviceModel = name,
                                    bands = eq.bands,
                                    preamp = eq.preamp,
                                    isCustom = true,
                                    addedTimestamp = System.currentTimeMillis(),
                                )
                                DesktopSettings.update { state ->
                                    val profiles = state.eqProfiles + profile
                                    // Auto-activate the freshly imported profile, like the mobile screen.
                                    state.copy(eqProfiles = profiles, activeEqProfileId = profile.id)
                                }
                                player.setEqualizer(profile)
                            },
                            onDeleteProfile = { id ->
                                DesktopSettings.update { state ->
                                    val profiles = state.eqProfiles.filterNot { it.id == id }
                                    val active = if (state.activeEqProfileId == id) "" else state.activeEqProfileId
                                    state.copy(eqProfiles = profiles, activeEqProfileId = active)
                                }
                                if (DesktopSettings.load().activeEqProfileId.isEmpty()) player.setEqualizer(null)
                            },
                            onAddExampleProfile = {
                                val profile = exampleEQProfile()
                                DesktopSettings.update { state ->
                                    val existing = state.eqProfiles.any { it.id == profile.id }
                                    val profiles = if (existing) state.eqProfiles else state.eqProfiles + profile
                                    state.copy(eqProfiles = profiles, activeEqProfileId = profile.id)
                                }
                                player.setEqualizer(profile)
                            },
                            onUpdateProfile = { id, bands, preamp ->
                                DesktopSettings.update { state ->
                                    val profiles = state.eqProfiles.map {
                                        if (it.id == id) it.copy(bands = bands, preamp = preamp) else it
                                    }
                                    state.copy(eqProfiles = profiles)
                                }
                                player.setEqualizer(DesktopSettings.load().eqProfiles.find { it.id == id })
                            },
                        )
                    }
                    is Screen.SettingsDataSaver -> {
                        // Local observable state: DesktopSettings.load() is a plain
                        // file read, so without this the Switch would only reflect
                        // the change after leaving and re-entering the screen.
                        var dataSaver by remember(settingsFileRevision()) { mutableStateOf(DesktopSettings.load().dataSaver) }
                        SettingsDataSaverScreen(
                            language = language,
                            onBack = goBack,
                            dataSaver = dataSaver,
                            onToggleDataSaver = { enabled ->
                                dataSaver = enabled
                                DesktopSettings.update { state ->
                                    if (enabled) {
                                        state.copy(
                                            dataSaver = true,
                                            dataSaverBackupCanvas = state.canvasEnabled,
                                            dataSaverBackupRotating = state.rotatingThumbnail,
                                            canvasEnabled = false,
                                            rotatingThumbnail = false,
                                        )
                                    } else {
                                        state.copy(
                                            dataSaver = false,
                                            canvasEnabled = state.dataSaverBackupCanvas,
                                            rotatingThumbnail = state.dataSaverBackupRotating,
                                        )
                                    }
                                }
                            },
                        )
                    }
                    is Screen.SettingsAi -> {
                        val s = DesktopSettings.load()
                        SettingsAiScreen(
                            language = language,
                            onBack = goBack,
                            aiProvider = s.aiProvider,
                            aiApiKey = s.aiApiKey,
                            aiBaseUrl = s.aiBaseUrl,
                            aiModel = s.aiModel,
                            translateLanguage = s.translateLanguage,
                            translateMode = s.translateMode,
                            deeplApiKey = s.deeplApiKey,
                            deeplFormality = s.deeplFormality,
                            onAiProviderChange = { v -> DesktopSettings.update { it.copy(aiProvider = v) } },
                            onAiApiKeyChange = { v -> DesktopSettings.update { it.copy(aiApiKey = v) } },
                            onAiBaseUrlChange = { v -> DesktopSettings.update { it.copy(aiBaseUrl = v) } },
                            onAiModelChange = { v -> DesktopSettings.update { it.copy(aiModel = v) } },
                            onTranslateLanguageChange = { v -> DesktopSettings.update { it.copy(translateLanguage = v) } },
                            onTranslateModeChange = { v -> DesktopSettings.update { it.copy(translateMode = v) } },
                            onDeeplApiKeyChange = { v -> DesktopSettings.update { it.copy(deeplApiKey = v) } },
                            onDeeplFormalityChange = { v -> DesktopSettings.update { it.copy(deeplFormality = v) } },
                        )
                    }
                    is Screen.SettingsAccount -> SettingsAccountScreen(
                        language = language,
                        onBack = goBack,
                        isLoggedIn = isLoggedIn,
                        onOpenLogin = { navigate(Screen.Login) },
                        onLogout = {
                            LoginManager.logout()
                            isLoggedIn = false
                            accountName = ""
                            accountChannelHandle = ""
                        },
                        onLoggedIn = {
                            isLoggedIn = true
                            accountName = DesktopSettings.load().accountName
                        },
                    )
                    is Screen.SettingsDevices -> SettingsDevicesScreen(
                        language = language,
                        onBack = goBack,
                        syncManager = syncManager,
                        syncViviVolume = syncViviVolume,
                        onToggleSyncViviVolume = { checked ->
                            syncViviVolume = checked
                            DesktopSettings.update { it.copy(syncViviVolume = checked) }
                        },
                        syncNativeVolume = syncNativeVolume,
                        onToggleSyncNativeVolume = { checked ->
                            syncNativeVolume = checked
                            DesktopSettings.update { it.copy(syncNativeVolume = checked) }
                        },
                    )
                    is Screen.SettingsDesktop -> SettingsDesktopScreen(
                        language = language,
                        onBack = goBack,
                        showWidget = showWidget,
                        onShowWidgetChange = { v ->
                            showWidget = v
                            DesktopSettings.update { it.copy(showNowPlayingWidget = v) }
                        },
                        mediaKeysEnabled = mediaKeysEnabled,
                        onMediaKeysChange = { v ->
                            mediaKeysEnabled = v
                            DesktopSettings.update { it.copy(mediaKeysEnabled = v) }
                        },
                        trayMenuEnabled = trayMenuEnabled,
                        onTrayMenuChange = { v ->
                            trayMenuEnabled = v
                            DesktopSettings.update { it.copy(trayMenuEnabled = v) }
                        },
                    )
                    is Screen.SettingsIntegrations -> SettingsIntegrationsScreen(
                        language = language,
                        onBack = goBack,
                        discordEnabled = discordRpcEnabled,
                        onDiscordEnabledChange = { v ->
                            discordRpcEnabled = v
                            DesktopSettings.update { it.copy(discordRpcEnabled = v) }
                        },
                        discordClientId = discordClientId,
                        onDiscordClientIdChange = { v ->
                            discordClientId = v
                            DesktopSettings.update { it.copy(discordClientId = v) }
                        },
                        lastfmEnabled = lastfmEnabled,
                        onLastfmEnabledChange = { v ->
                            lastfmEnabled = v
                            DesktopSettings.update { it.copy(lastfmEnabled = v) }
                        },
                        lastfmSession = lastfmSession,
                        onLastfmSessionChange = { v ->
                            lastfmSession = v
                            DesktopSettings.update { it.copy(lastfmSession = v) }
                        },
                        lastfmNowPlaying = lastfmNowPlaying,
                        onLastfmNowPlayingChange = { v ->
                            lastfmNowPlaying = v
                            DesktopSettings.update { it.copy(lastfmNowPlaying = v) }
                        },
                    )
                    is Screen.SettingsSpotifyImport -> SettingsSpotifyImportScreen(
                        language = language,
                        onBack = goBack,
                    )
                    is Screen.SettingsWrapped -> SettingsWrappedScreen(
                        language = language,
                        onBack = goBack,
                        wrappedStats = WrappedStats(
                            trackStarts = sessionTrackStarts,
                            playedMs = sessionPlayedMs,
                            topSongTitle = sessionTopSong?.second,
                            topSongCount = sessionTopCount,
                        ),
                        showWrappedOnHome = showWrappedOnHome,
                        onShowWrappedOnHomeChange = { v ->
                            showWrappedOnHome = v
                            DesktopSettings.update { it.copy(showWrappedOnHome = v) }
                        },
                    )
                    is Screen.SettingsPrivacy -> SettingsPrivacyScreen(
                        language = language,
                        onBack = goBack,
                        pauseListenHistory = pauseListenHistory,
                        onPauseListenHistoryChange = { v ->
                            pauseListenHistory = v
                            DesktopSettings.update { it.copy(pauseListenHistory = v) }
                        },
                        pauseSearchHistory = pauseSearchHistory,
                        onPauseSearchHistoryChange = { v ->
                            pauseSearchHistory = v
                            DesktopSettings.update { it.copy(pauseSearchHistory = v) }
                        },
                        onClearSearchHistory = {
                            searchHistory = emptyList()
                            DesktopSettings.update { it.copy(searchHistory = emptyList()) }
                        },
                    )
                    is Screen.SettingsContent -> SettingsContentScreen(
                        language = language,
                        onBack = goBack,
                        contentLanguage = contentLanguage,
                        contentCountry = contentCountry,
                        onContentLanguageChange = { code ->
                            contentLanguage = code
                            DesktopSettings.update { it.copy(contentLanguage = code) }
                            YouTube.locale = resolveYouTubeLocale(contentLanguage, contentCountry, language)
                        },
                        onContentCountryChange = { code ->
                            contentCountry = code
                            DesktopSettings.update { it.copy(contentCountry = code) }
                            YouTube.locale = resolveYouTubeLocale(contentLanguage, contentCountry, language)
                        },
                        hideExplicit = hideExplicit,
                        hideVideoSongs = hideVideoSongs,
                        hideYoutubeShorts = hideYoutubeShorts,
                        onHideExplicitChange = { on ->
                            hideExplicit = on
                            DesktopSettings.update { it.copy(hideExplicit = on) }
                            ContentFilters.load()
                        },
                        onHideVideoSongsChange = { on ->
                            hideVideoSongs = on
                            DesktopSettings.update { it.copy(hideVideoSongs = on) }
                            ContentFilters.load()
                        },
                        onHideYoutubeShortsChange = { on ->
                            hideYoutubeShorts = on
                            DesktopSettings.update { it.copy(hideYoutubeShorts = on) }
                            ContentFilters.load()
                        },
                        showArtistDescription = showArtistDescription,
                        showArtistSubscriberCount = showArtistSubscriberCount,
                        onShowArtistDescriptionChange = { on ->
                            showArtistDescription = on
                            DesktopSettings.update { it.copy(showArtistDescription = on) }
                        },
                        onShowArtistSubscriberCountChange = { on ->
                            showArtistSubscriberCount = on
                            DesktopSettings.update { it.copy(showArtistSubscriberCount = on) }
                        },
                        lyricsProviders = lyricsProviders,
                        onLyricsProvidersChange = { order ->
                            lyricsProviders = order
                            DesktopSettings.update { it.copy(lyricsProviderPriority = order) }
                        },
                    )
                    is Screen.SettingsLyrics -> SettingsLyricsScreen(
                        language = language,
                        onBack = goBack,
                        syncedLyrics = syncedLyrics,
                        onToggleSyncedLyrics = { checked ->
                            syncedLyrics = checked
                            DesktopSettings.update { it.copy(syncedLyrics = checked) }
                        },
                        lyricsTextSize = lyricsTextSize,
                        onLyricsTextSizeChange = { size ->
                            lyricsTextSize = size
                            // The renderer draws from `lyricsDisplay`, not from
                            // this separate state: updating only the latter made
                            // the size slider move a number with no effect at
                            // all until another option was touched.
                            lyricsDisplay = lyricsDisplay.copy(textSizeSp = size)
                            DesktopSettings.update { it.copy(lyricsTextSize = size) }
                        },
                        lyricsLineSpacing = lyricsLineSpacing,
                        onLyricsLineSpacingChange = { ls ->
                            lyricsLineSpacing = ls
                            lyricsDisplay = lyricsDisplay.copy(lineSpacing = ls)
                            DesktopSettings.update { it.copy(lyricsLineSpacing = ls) }
                        },
                        options = lyricsDisplay,
                        onOptionsChange = { opts ->
                            lyricsDisplay = opts
                            DesktopSettings.update { it.copy(lyricsTextSize = opts.textSizeSp, lyricsLineSpacing = opts.lineSpacing).withLyricsDisplayOptions(opts) }
                        },
                        translateLyrics = translateLyrics,
                        onToggleTranslateLyrics = { on ->
                            translateLyrics = on
                            DesktopSettings.update { it.copy(translateLyrics = on) }
                        },
                        lyricsThumbnailPlayPause = lyricsThumbnailPlayPause,
                        onToggleLyricsThumbnailPlayPause = { on ->
                            lyricsThumbnailPlayPause = on
                            DesktopSettings.update { it.copy(lyricsThumbnailPlayPause = on) }
                        },
                        onOpenRomanize = { navigate(Screen.SettingsRomanize) },
                        translationAvailable = lyricsTranslationAvailable,
                    )
                    is Screen.SettingsRomanize -> SettingsRomanizeScreen(
                        language = language,
                        onBack = goBack,
                        options = lyricsDisplay,
                        onOptionsChange = { opts ->
                            lyricsDisplay = opts
                            DesktopSettings.update {
                                it.copy(
                                    lyricsTextSize = opts.textSizeSp,
                                    lyricsLineSpacing = opts.lineSpacing,
                                ).withLyricsDisplayOptions(opts)
                            }
                        },
                    )
                    is Screen.SettingsStorage -> SettingsStorageScreen(
                        language = language,
                        onBack = goBack,
                    )
                    is Screen.SettingsUpdates -> SettingsUpdatesScreen(
                        language = language,
                        onBack = goBack,
                        updateStatus = updateStatus,
                        includePreReleases = includePreReleases,
                        updateIntervalHours = updateIntervalHours,
                        updateSource = updateSource,
                        onIntervalChange = { hours ->
                            updateIntervalHours = hours
                            DesktopSettings.update { it.copy(updateCheckIntervalHours = hours) }
                        },
                        onTogglePreReleases = { checked ->
                            includePreReleases = checked
                            DesktopSettings.update { it.copy(includePreReleases = checked) }
                            runUpdateCheck()
                        },
                        onUpdateSourceChange = { source ->
                            updateSource = source
                            DesktopSettings.update { it.copy(updateSource = source) }
                            runUpdateCheck()
                        },
                        onCheckUpdates = { runUpdateCheck() },
                        onOpenChangelog = { navigate(Screen.Changelog) },
                        onOpenCommits = { navigate(Screen.SettingsCommits) },
                    )
                    is Screen.SettingsCommits -> CommitsScreen(
                        language = language,
                        onBack = goBack,
                    )
                    is Screen.SettingsAbout -> SettingsAboutScreen(
                        language = language,
                        onBack = goBack,
                        onOpenContributors = { navigate(Screen.SettingsContributors) },
                        onOpenLicense = { navigate(Screen.SettingsLicense) },
                    )
                    is Screen.SettingsContributors -> SettingsContributorsScreen(
                        language = language,
                        onBack = goBack,
                    )
                    is Screen.SettingsLicense -> SettingsLicenseScreen(
                        language = language,
                        onBack = goBack,
                    )
                    is Screen.SettingsPerformance -> SettingsPerformanceScreen(
                        language = language,
                        onBack = goBack,
                        syncManager = syncManager,
                    )
                    is Screen.SettingsDeveloper -> SettingsDeveloperOptionsScreen(
                        language = language,
                        onBack = goBack,
                    )
                    is Screen.SettingsSystem -> SettingsSystemScreen(
                        language = language,
                        onBack = goBack,
                        showIntroSplash = showIntroSplash,
                        onOpenPerformance = { navigate(Screen.SettingsPerformance) },
                        onOpenDeveloperOptions = { navigate(Screen.SettingsDeveloper) },
                        onOpenIntro = { navigate(Screen.SettingsIntro) },
                    )
                    is Screen.SettingsIntro -> SettingsIntroScreen(
                        language = language,
                        onBack = goBack,
                        showIntroSplash = showIntroSplash,
                        onShowIntroSplashChange = { v ->
                            showIntroSplash = v
                            DesktopSettings.update { it.copy(showIntroSplash = v) }
                        },
                        introStyle = introStyle,
                        onIntroStyleChange = { v ->
                            introStyle = v
                            DesktopSettings.update { it.copy(introStyle = v) }
                        },
                        introBackground = introBackground,
                        onIntroBackgroundChange = { v ->
                            introBackground = v
                            DesktopSettings.update { it.copy(introBackground = v) }
                        },
                    )
                    is Screen.SettingsBackup -> SettingsBackupScreen(
                        language = language,
                        onBack = goBack,
                    )
                    is Screen.SettingsNotifications -> SettingsNotificationsScreen(
                        language = language,
                        onBack = goBack,
                        notificationMode = notificationMode,
                        onNotificationModeChange = { mode ->
                            notificationMode = mode
                            DesktopSettings.update { it.copy(notificationMode = mode) }
                        },
                        notificationDurationSeconds = notificationDurationSeconds,
                        onNotificationDurationChange = { secs ->
                            notificationDurationSeconds = secs
                            DesktopSettings.update { it.copy(inAppNotificationDurationSeconds = secs) }
                        },
                        saveHistory = saveNotificationHistory,
                        onSaveHistoryChange = { save ->
                            saveNotificationHistory = save
                            DesktopSettings.update { it.copy(saveNotificationHistory = save) }
                        },
                        onOpenHistory = { navigate(Screen.SettingsNotificationsHistory) },
                        onTestNotification = {
                            DesktopNotifier.notify(
                                "VIVI Music DE",
                                Localization.get(language, "test_notification"),
                                null,
                            )
                        },
                        onRequestNotificationPermission = {
                            // macOS grants the notification permission once, on
                            // the first request; a notification right after is
                            // both the proof it went through and what makes the
                            // OS show the prompt on a build that never sent one.
                            MacMediaSession.requestNotificationPermissionOnce()
                            DesktopNotifier.notify(
                                "VIVI Music DE",
                                Localization.get(language, "notifications"),
                                null,
                            )
                        },
                    )
                    is Screen.SettingsNotificationsHistory -> NotificationHistoryScreen(
                        language = language,
                        onBack = goBack,
                    )
                    is Screen.Album -> AlbumScreen(
                        browseId = screen.browseId,
                        language = language,
                        onBack = goBack,
                        onOpenArtist = { navigate(Screen.Artist(it)) },
                        onPlaySong = playSong,
                        onAddToQueue = addToQueue,
                        onAddToPlaylist = addToPlaylist,
                        onPlayAll = playAll,
                        onShuffleAll = shuffleAll,
                        onPlayNext = playNextAll,
                        onAddAllToQueue = addAllToQueue,
                    )
                    is Screen.Artist -> ArtistScreen(
                        browseId = screen.browseId,
                        language = language,
                        onBack = goBack,
                        onOpenAlbum = { navigate(Screen.Album(it)) },
                        onOpenArtist = { navigate(Screen.Artist(it)) },
                        onOpenPlaylist = { navigate(Screen.Playlist(it)) },
                        onPlaySong = playSong,
                        onAddToQueue = addToQueue,
                        onAddToPlaylist = addToPlaylist,
                        onPlayAll = playAll,
                        onShuffleAll = shuffleAll,
                        onPlayNext = playNextAll,
                        onAddAllToQueue = addAllToQueue,
                    )
                    is Screen.Playlist -> PlaylistScreen(
                        playlistId = screen.playlistId,
                        language = language,
                        onBack = goBack,
                        onOpenArtist = { navigate(Screen.Artist(it)) },
                        onPlaySong = playSong,
                        onAddToQueue = addToQueue,
                        onAddToPlaylist = addToPlaylist,
                        onPlayAll = playAll,
                        onShuffleAll = shuffleAll,
                        onPlayNext = playNextAll,
                        onAddAllToQueue = addAllToQueue,
                    )
                    is Screen.LocalPlaylists -> LocalPlaylistsScreen(
                        language = language,
                        onBack = goBack,
                        onOpenPlaylist = { navigate(Screen.LocalPlaylist(it)) },
                    )
                    is Screen.LocalPlaylist -> LocalPlaylistScreen(
                        playlistId = screen.playlistId,
                        language = language,
                        onBack = goBack,
                        onPlay = { s -> player.play(NowPlaying(videoId = s.id, title = s.title, artist = s.artist, thumbnail = s.thumbnail)) },
                        onPlayAll = { songs -> player.playAll(songs.map { NowPlaying(videoId = it.id, title = it.title, artist = it.artist, thumbnail = it.thumbnail) }) },
                    )
                    is Screen.Browse -> BrowseScreen(
                        browseId = screen.browseId,
                        params = screen.params,
                        language = language,
                        gridItemSize = gridItemSize,
                        onBack = goBack,
                        onOpenAlbum = { navigate(Screen.Album(it)) },
                        onOpenArtist = { navigate(Screen.Artist(it)) },
                        onOpenPlaylist = { navigate(Screen.Playlist(it)) },
                        onPlaySong = playSong,
                    )
                    is Screen.Player -> PlayerScreen(
                        queue = playerState.queue,
                        index = playerState.index,
                        isPlaying = isPlaying,
                        positionMs = playerState.positionMs,
                        durationMs = playerState.durationMs,
                        volume = playerState.volume,
                        isShuffle = playerState.isShuffle,
                        repeatMode = playerState.repeatMode,
                        errorKey = playerState.errorKey,
                        errorDetail = playerState.errorDetail,
                        loadPhase = playerState.loadPhase,
                        onTogglePlay = { player.toggle() },
                        onNext = { player.next() },
                        onPrevious = { player.previous() },
                        onSeek = { player.seekTo(it) },
                        onVolume = setLocalVolume,
                        onToggleShuffle = { player.toggleShuffle() },
                        onCycleRepeat = { player.cycleRepeatMode() },
                        language = language,
                        onOpenLyrics = { navigate(Screen.Lyrics) },
                        onOpenLyricsFocus = { navigate(Screen.LyricsFocus) },
                        onOpenLyricsSettings = { navigate(Screen.SettingsLyrics) },
                        onOpenQueue = { navigate(Screen.Queue) },
                        onAddToPlaylist = addNowPlayingToPlaylist,
                        onSkipTo = { player.skipTo(it) },
                        onRemoveAt = { player.removeAt(it) },
                        onClearQueue = { player.clearQueue() },
                        onReorderQueue = { player.reorder(it) },
                        sliderStyle = ViviSliderStyle.from(sliderStyle),
                        design = playerDesign,
                        background = playerBackground,
                        expressiveTabTranslucent = expressiveTabTranslucent,
                        rotatingThumbnail = rotatingThumbnail,
                        accent = accent,
                        audioLevel = player.audioLevel,
                        onBack = goBack,
                        progressiveSeek = progressiveSeek,
                        // The expressive player's queue panel offers the autoplay
                        // toggle; it has to be the same setting the player obeys
                        // when a track ends, not a flag of its own.
                        autoPlayNext = autoPlayNext,
                        onToggleAutoPlayNext = { checked ->
                            autoPlayNext = checked
                            player.autoPlayNext = checked
                            DesktopSettings.update { it.copy(autoPlayNext = checked) }
                        },
                    )
                    is Screen.LyricsFocus -> LyricsFocusScreen(
                        nowPlaying = nowPlaying,
                        positionMs = playerState.positionMs,
                        isPlaying = isPlaying,
                        language = language,
                        synced = syncedLyrics,
                        textSizeSp = lyricsTextSize,
                        lineSpacing = lyricsLineSpacing,
                        display = lyricsDisplay.copy(fontFamily = lyricsFontFamily),
                        translate = lyricsTranslationConfig(DesktopSettings.load(), translateLyrics),
                        onSeek = { ms -> player.seekTo(ms) },
                        onTogglePlay = { player.toggle() },
                        onNext = { player.next() },
                        onPrevious = { player.previous() },
                        onBack = goBack,
                    )
                    is Screen.Lyrics -> LyricsScreen(
                        nowPlaying = nowPlaying,
                        positionMs = playerState.positionMs,
                        isPlaying = isPlaying,
                        language = language,
                        synced = syncedLyrics,
                        textSizeSp = lyricsTextSize,
                        lineSpacing = lyricsLineSpacing,
                        display = lyricsDisplay.copy(fontFamily = lyricsFontFamily),
                        translate = lyricsTranslationConfig(DesktopSettings.load(), translateLyrics),
                        onSeek = { ms -> player.seekTo(ms) },
                        onTogglePlay = { player.toggle() },
                        onBack = goBack,
                    )
                    is Screen.Queue -> QueueScreen(
                        queue = playerState.queue,
                        index = playerState.index,
                        language = language,
                        onBack = goBack,
                        onSkipTo = { player.skipTo(it) },
                        onRemoveAt = { player.removeAt(it) },
                        onClear = { player.clearQueue() },
                        onReorder = { player.reorder(it) },
                        onAddToPlaylist = addNowPlayingToPlaylist,
                    )
                    is Screen.Changelog -> ChangelogScreen(
                        language = language,
                        onBack = goBack,
                    )
                    is Screen.ListenTogether -> ListenTogetherScreen(
                        language = language,
                        onBack = goBack,
                        manager = listenTogetherManager,
                    )
                    is Screen.ArtistItems -> BrowseScreen(
                        browseId = screen.browseId,
                        params = screen.params,
                        language = language,
                        gridItemSize = gridItemSize,
                        onBack = goBack,
                        onOpenAlbum = { navigate(Screen.Album(it)) },
                        onOpenArtist = { navigate(Screen.Artist(it)) },
                        onOpenPlaylist = { navigate(Screen.Playlist(it)) },
                        onPlaySong = playSong,
                    )
                    is Screen.AutoPlaylist -> AutoPlaylistScreen(
                        browseId = screen.browseId,
                        title = screen.title,
                        language = language,
                        gridItemSize = gridItemSize,
                        onBack = goBack,
                        onOpenAlbum = { navigate(Screen.Album(it)) },
                        onOpenArtist = { navigate(Screen.Artist(it)) },
                        onOpenPlaylist = { navigate(Screen.Playlist(it)) },
                        onPlaySong = playSong,
                        onAddToQueue = addToQueue,
                        onAddToPlaylist = addToPlaylist,
                    )
                    is Screen.Login -> LoginScreen(
                        language = language,
                        onBack = goBack,
                        onLoggedIn = {
                            isLoggedIn = true
                            accountName = DesktopSettings.load().accountName
                        },
                    )
                }
                } // AnimatedContent
                if (showUpdateNotification && updateStatus is UpdateStatus.Available) {
                    UpdateNotification(
                        status = updateStatus as UpdateStatus.Available,
                        language = language,
                        onDismiss = { showUpdateNotification = false },
                        onDone = { showUpdateNotification = false },
                    )
                }
                if (showDevNotification) {
                    DevUnlockedNotification(
                        language = language,
                        onOpen = {
                            showDevNotification = false
                            navigate(Screen.SettingsDeveloper)
                        },
                        onDismiss = { showDevNotification = false },
                    )
                }
                appNotification?.let { notice ->
                    InAppNotification(
                        title = notice.title,
                        message = notice.message,
                        language = language,
                        onDismiss = { appNotification = null },
                    )
                }
                if (devEnabled && devMode == DevToolsMode.OVERLAY) {
                    DevToolsOverlay(
                        syncManager = syncManager,
                        language = language,
                        movable = overlayMovable,
                    )
                }
                addToPlaylistSong?.let { song ->
                    AddToPlaylistDialog(
                        language = language,
                        song = song,
                        onDismiss = { addToPlaylistSong = null },
                    )
                }
                SnackbarHost(
                    hostState = snackbarHostState,
                    modifier = Modifier.align(Alignment.BottomCenter),
                )
            }
        }
        // Hide the right Now Playing panel while the full player screen is
        // open (it would duplicate what the player already shows); it comes
        // back automatically when leaving the player.
        // The right Now Playing panel used to appear and disappear with no
        // transition at all: it is a panel like the sidebar, so it takes the
        // same expand/shrink (which the master switch can turn off).
        AnimatedVisibility(
            // A narrow window gives the panel away to the content instead of
            // squeezing it: it comes back as soon as the window has room.
            visible = showRightSidebar && spotifyLayout && current != Screen.Player && !narrowRightPanel,
            enter = Animations.panelEnter(),
            exit = Animations.panelExit(),
        ) {
            SpotifyRightNowPlayingPanel(
                nowPlaying = nowPlaying,
                isPlaying = isPlaying,
                positionMs = playerState.positionMs,
                language = language,
                onClose = {
                    showRightSidebar = false
                    DesktopSettings.update { it.copy(showRightSidebar = false) }
                },
                onOpenLyrics = { navigate(Screen.Lyrics) },
                onAddToPlaylist = addNowPlayingToPlaylist,
                modifier = Modifier.padding(start = 4.dp, bottom = 6.dp),
            )
        }
    }
    NowPlayingBar(
        spotifyLayout = spotifyLayout,
        current = current,
        nowPlaying = nowPlaying,
        isPlaying = isPlaying,
        playerState = playerState,
        player = player,
        setLocalVolume = setLocalVolume,
        navigate = navigate,
        goBack = goBack,
        showRightSidebar = showRightSidebar,
        onToggleRightSidebar = {
            showRightSidebar = !showRightSidebar
            DesktopSettings.update { it.copy(showRightSidebar = showRightSidebar) }
        },
        isFullscreen = isFullscreen,
        onToggleFullscreen = onToggleFullscreen,
        language = language,
        miniPlayerDesign = miniPlayerDesign,
        miniPlayerBackgroundStyle = miniPlayerBackgroundStyle,
        pureBlackMiniPlayer = pureBlackMiniPlayer,
        swipeThumbnail = swipeThumbnail,
        swipeSensitivity = swipeSensitivity,
        sliderStyle = sliderStyle,
    )
}
}
    WindowControlsOverlay(
        nativeTitleBar = nativeTitleBar,
        spotifyLayout = spotifyLayout,
        onPlayerScreen = current == Screen.Player,
        language = language,
        isMaximized = isMaximized,
        onMinimize = onMinimize,
        onMaximize = onMaximize,
        onClose = onClose,
    )
    }
    } // density scale CompositionLocalProvider

    // Developer tools in a dedicated window (closing it falls back to overlay).
    if (devEnabled && devMode == DevToolsMode.WINDOW) {
        Window(
            onCloseRequest = { DeveloperOptions.setMode(DevToolsMode.OVERLAY) },
            title = "VIVI Music DE — Developer tools",
        ) {
            AppTheme(
                mode = themeMode,
                accent = accent,
                pureBlack = pureBlack,
                font = font,
                spotify = spotifyLayout,
                accentIntensity = accentIntensity,
                customFontPath = customFontPath,
            ) {
                SelectionContainer {
                    DevToolsPanel(syncManager = syncManager, language = language)
                }
            }
        }
    }

    // Live activity log in a dedicated window (Settings → System →
    // "Open live log", always available). Shows playback/actions/errors in
    // real time.
    if (DeveloperOptions.logWindowVisible.collectAsState().value) {
        Window(
            onCloseRequest = { DeveloperOptions.setLogWindowVisible(false) },
            title = "VIVI Music DE — Live log",
        ) {
            AppTheme(
                mode = themeMode,
                accent = accent,
                pureBlack = pureBlack,
                font = font,
                spotify = spotifyLayout,
                accentIntensity = accentIntensity,
                customFontPath = customFontPath,
            ) {
                LiveLogWindowContent(
                    language = language,
                    onClose = { DeveloperOptions.setLogWindowVisible(false) },
                )
            }
        }
    }

    // Listen Together chat in a dedicated window. A conversation deserves its
    // own window the same way the live log does, and keeping it out of the room
    // screen is what lets the user browse the library while the room keeps
    // playing. Emitted here, at the top of the composition, so it survives
    // leaving the room screen; the manager keeps feeding it either way.
    if (ListenTogetherChatWindow.visible.value) {
        Window(
            onCloseRequest = { ListenTogetherChatWindow.close() },
            title = "VIVI Music DE - Listen Together Chat",
        ) {
            AppTheme(
                mode = themeMode,
                accent = accent,
                pureBlack = pureBlack,
                font = font,
                spotify = spotifyLayout,
                accentIntensity = accentIntensity,
                customFontPath = customFontPath,
            ) {
                ListenTogetherChatWindowContent(
                    language = language,
                    manager = listenTogetherManager,
                    onClose = { ListenTogetherChatWindow.close() },
                )
            }
        }
    }
}

private data class SidebarEntry(
    val screen: Screen,
    val key: String,
    val icon: ImageVector,
    val selectedIcon: ImageVector,
)

/**
 * M3 Expressive Cider-Inspired Navigation Rail (Sidebar).
 * Solid surfaceContainer background, spring collapsible sections, active indicator pills,
 * and pinned bottom user account capsule.
 */
@Composable
fun Sidebar(
    language: String,
    current: Screen,
    collapsed: Boolean,
    hideHistory: Boolean = false,
    showTitleHeader: Boolean = true,
    userName: String = "Guest",
    userHandle: String = "Not signed in",
    isLoggedIn: Boolean = false,
    spotify: Boolean = false,
    onToggleCollapsed: () -> Unit,
    onSelect: (Screen) -> Unit,
) {
    // Spotify style: pure-black sidebar (dark) / white (light) with a grey
    // selected pill; the classic layout keeps the accent-filled selection.
    val selectedBg = if (spotify) MaterialTheme.colorScheme.surfaceContainerHighest
    else MaterialTheme.colorScheme.primaryContainer
    val selectedFg = if (spotify) MaterialTheme.colorScheme.onSurface
    else MaterialTheme.colorScheme.onPrimaryContainer
    val mainRadius = if (spotify) 8.dp else 14.dp
    val subRadius = if (spotify) 8.dp else 12.dp
    var libraryExpanded by remember { mutableStateOf(true) }
    var playlistsExpanded by remember { mutableStateOf(true) }
    var showCreateDialog by remember { mutableStateOf(false) }

    val localPlaylists by PlaylistStore.all.collectAsState()
    val activeLocalPlaylists = remember(localPlaylists) {
        localPlaylists.filter { !it.deleted }.sortedByDescending { it.updatedAt }
    }

    var onlinePlaylists by remember { mutableStateOf<List<com.music.innertube.models.PlaylistItem>>(emptyList()) }
    val scope = rememberCoroutineScope()

    LaunchedEffect(isLoggedIn) {
        if (isLoggedIn) {
            com.music.innertube.YouTube.library("FEmusic_liked_playlists").fold(
                onSuccess = { page ->
                    onlinePlaylists = page.items.filterIsInstance<com.music.innertube.models.PlaylistItem>()
                },
                onFailure = {
                    onlinePlaylists = emptyList()
                }
            )
        } else {
            onlinePlaylists = emptyList()
        }
    }

    // The group chevrons follow the same switch as the sections they open (they
    // used a fixed spring, so the arrow kept moving with animations off).
    val libraryChevronRotation by animateFloatAsState(
        targetValue = if (libraryExpanded) 90f else 0f,
        animationSpec = tween(Animations.ms(200), easing = FastOutSlowInEasing),
        label = "libraryChevronRotation",
    )
    val playlistsChevronRotation by animateFloatAsState(
        targetValue = if (playlistsExpanded) 90f else 0f,
        animationSpec = tween(Animations.ms(200), easing = FastOutSlowInEasing),
        label = "playlistsChevronRotation",
    )

    val mainEntries = listOf(
        SidebarEntry(Screen.Home, "home", Icons.Outlined.Home, Icons.Filled.Home),
        SidebarEntry(Screen.NewReleases, "new", Icons.Outlined.Explore, Icons.Filled.Explore),
        SidebarEntry(Screen.Charts, "radio", Icons.Outlined.Radio, Icons.Filled.Radio),
    )

    val librarySubEntries = listOf(
        SidebarEntry(Screen.History, "history", Icons.Outlined.History, Icons.Filled.History),
        SidebarEntry(Screen.Library, "songs", Icons.Outlined.MusicNote, Icons.Filled.MusicNote),
        SidebarEntry(Screen.LocalPlaylists, "albums", Icons.Outlined.LibraryMusic, Icons.Filled.LibraryMusic),
        SidebarEntry(
            Screen.ArtistItems("FEmusic_library_corpus_artists", null),
            "artists",
            Icons.Outlined.Group,
            Icons.Filled.Group,
        ),
    )

    // The rail collapse is a panel resize: it follows "Animation speed" and
    // becomes instant when the master "Animations" switch is off.
    val width by animateDpAsState(
        targetValue = if (collapsed) 72.dp else 230.dp,
        animationSpec = tween(Animations.sizeMs(), easing = FastOutSlowInEasing),
        label = "sidebarWidth",
    )

    Surface(
        color = if (spotify) {
            if (isAppInDarkTheme()) Color(0xFF000000) else MaterialTheme.colorScheme.surface
        } else {
            MaterialTheme.colorScheme.surfaceContainer
        }
    ) {
        Column(
            Modifier
                .width(width)
                .fillMaxHeight()
                .padding(horizontal = if (collapsed) 8.dp else 12.dp, vertical = 12.dp),
        ) {
            // The brand IS the rail's control: clicking "VIVI Music" (or the
            // logo alone, when the rail is collapsed to the icon strip) opens and
            // closes it. The dedicated buttons that did the same job are gone
            // (the Menu button here, the MenuOpen one next to the title, and the
            // one that sat in the top bar next to Back), and the label no longer
            // collapses the group of entries under it: that group is always open.
            Tooltip(Localization.get(language, if (collapsed) "tooltip_expand_sidebar" else "tooltip_collapse_sidebar")) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null,
                        ) { onToggleCollapsed() }
                        .padding(vertical = 6.dp, horizontal = if (collapsed) 0.dp else 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = if (collapsed) Arrangement.Center else Arrangement.Start,
                ) {
                    // The mark is 26-30 dp here: it is resampled to that size (x
                    // the density in use) instead of being left to Compose to
                    // shrink from 1024 px in one bilinear step, which is what
                    // made it look muddy.
                    val brandDensity = LocalDensity.current.density
                    val brandSize = if (collapsed) 30 else 26
                    val brandLogo = remember(brandDensity, brandSize) { loadLogo(brandSize, brandDensity) }
                    if (brandLogo != null) {
                        Image(
                            bitmap = brandLogo,
                            contentDescription = "VIVI Music",
                            filterQuality = FilterQuality.High,
                            modifier = Modifier
                                .size(brandSize.dp)
                                .clip(RoundedCornerShape(8.dp)),
                        )
                    }
                    if (!collapsed) {
                        Spacer(Modifier.width(10.dp))
                        Text(
                            text = "VIVI Music",
                            style = if (showTitleHeader) {
                                MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold)
                            } else {
                                MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold)
                            },
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                }
            }
            Spacer(Modifier.height(6.dp))

            // Scrollable Content Region
            val scrollState = rememberScrollState()
            Column(
                Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .verticalScroll(scrollState),
            ) {
                // The home / new releases / radio entries are always visible: the
                // "VIVI Music" label above them used to collapse this group and is
                // now the rail's own open/close control.
                Column {
                    mainEntries.forEach { entry ->
                        val selected = current == entry.screen
                        val interaction = remember(entry.screen) { MutableInteractionSource() }
                        val hovered by interaction.collectIsHoveredAsState()
                        Row(
                            Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(mainRadius))
                                .background(
                                    when {
                                        selected -> selectedBg
                                        spotify && hovered -> MaterialTheme.colorScheme.surfaceContainerHigh
                                        else -> Color.Transparent
                                    }
                                )
                                .clickable(
                                    interactionSource = interaction,
                                    indication = if (spotify) null else LocalIndication.current,
                                ) { onSelect(entry.screen) }
                                .padding(horizontal = if (collapsed) 0.dp else 14.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = if (collapsed) Arrangement.Center else Arrangement.Start,
                        ) {
                            Icon(
                                if (selected) entry.selectedIcon else entry.icon,
                                contentDescription = Localization.get(language, entry.key),
                                tint = if (selected) selectedFg
                                else MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                            if (!collapsed) {
                                Spacer(Modifier.width(14.dp))
                                Text(
                                    Localization.get(language, entry.key),
                                    style = MaterialTheme.typography.titleMedium.copy(
                                        fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium
                                    ),
                                    color = if (selected) selectedFg
                                    else MaterialTheme.colorScheme.onSurface,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                )
                            }
                        }
                        Spacer(Modifier.height(2.dp))
                    }
                }

                Spacer(Modifier.height(12.dp))

                // Library Section Header (Collapsible)
                if (!collapsed) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { libraryExpanded = !libraryExpanded }
                            .padding(horizontal = 8.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                    ) {
                        Text(
                            text = Localization.get(language, "library"),
                            style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                            contentDescription = "Expand Library",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier
                                .size(18.dp)
                                .rotate(libraryChevronRotation),
                        )
                    }
                }

                AnimatedVisibility(
                    visible = libraryExpanded || collapsed,
                    enter = Animations.sectionEnter(),
                    exit = Animations.sectionExit(),
                ) {
                    // One Column: AnimatedVisibility lays multiple children out ON
                    // TOP of each other, so the group has to be a single child.
                    Column {
                    librarySubEntries.forEach { entry ->
                        val selected = current == entry.screen
                        val interaction = remember(entry.screen) { MutableInteractionSource() }
                        val hovered by interaction.collectIsHoveredAsState()
                        Row(
                            Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(subRadius))
                                .background(
                                    when {
                                        selected -> selectedBg
                                        spotify && hovered -> MaterialTheme.colorScheme.surfaceContainerHigh
                                        else -> Color.Transparent
                                    }
                                )
                                .clickable(
                                    interactionSource = interaction,
                                    indication = if (spotify) null else LocalIndication.current,
                                ) { onSelect(entry.screen) }
                                .padding(horizontal = if (collapsed) 0.dp else 14.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = if (collapsed) Arrangement.Center else Arrangement.Start,
                        ) {
                            Icon(
                                if (selected) entry.selectedIcon else entry.icon,
                                contentDescription = Localization.get(language, entry.key),
                                tint = if (selected) selectedFg
                                else MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(20.dp),
                            )
                            if (!collapsed) {
                                Spacer(Modifier.width(12.dp))
                                Text(
                                    Localization.get(language, entry.key),
                                    style = MaterialTheme.typography.bodyMedium.copy(
                                        fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal
                                    ),
                                    color = if (selected) selectedFg
                                    else MaterialTheme.colorScheme.onSurface,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                )
                            }
                        }
                        Spacer(Modifier.height(2.dp))
                    }
                    }
                }

                Spacer(Modifier.height(12.dp))

                // Playlists Section Header: clicking the label opens the full
                // playlist list screen; only the chevron arrow expands/collapses
                // the inline playlist list in the sidebar.
                if (!collapsed) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onSelect(Screen.LocalPlaylists) }
                            .padding(horizontal = 8.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                    ) {
                        Text(
                            text = Localization.get(language, "playlists"),
                            style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        // Larger invisible touch target so the arrow is easy to
                        // hit; the nested clickable consumes the tap, so it never
                        // triggers the row's navigation.
                        Box(
                            modifier = Modifier
                                .size(28.dp)
                                .clip(CircleShape)
                                .clickable { playlistsExpanded = !playlistsExpanded },
                            contentAlignment = Alignment.Center,
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                                contentDescription = "Expand Playlists",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier
                                    .size(18.dp)
                                    .rotate(playlistsChevronRotation),
                            )
                        }
                    }
                }

                AnimatedVisibility(
                    visible = playlistsExpanded || collapsed,
                    enter = Animations.sectionEnter(),
                    exit = Animations.sectionExit(),
                ) {
                    // One Column: AnimatedVisibility lays multiple children out ON
                    // TOP of each other, so the group has to be a single child.
                    Column {
                    // Create New Playlist
                    Row(
                        Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .clickable { showCreateDialog = true }
                            .padding(horizontal = if (collapsed) 0.dp else 14.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = if (collapsed) Arrangement.Center else Arrangement.Start,
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Add,
                            contentDescription = "Create New",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(20.dp),
                        )
                        if (!collapsed) {
                            Spacer(Modifier.width(12.dp))
                            Text(
                                Localization.get(language, "new_playlist").let { if (it == "new_playlist" || it.isBlank()) "Create New..." else it },
                                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                                color = MaterialTheme.colorScheme.primary,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                        }
                    }

                    // Active Local Playlists
                    activeLocalPlaylists.forEach { p ->
                        val selected = current == Screen.LocalPlaylist(p.id)
                        val interaction = remember(p.id) { MutableInteractionSource() }
                        val hovered by interaction.collectIsHoveredAsState()
                        Row(
                            Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(subRadius))
                                .background(
                                    when {
                                        selected -> selectedBg
                                        spotify && hovered -> MaterialTheme.colorScheme.surfaceContainerHigh
                                        else -> Color.Transparent
                                    }
                                )
                                .clickable(
                                    interactionSource = interaction,
                                    indication = if (spotify) null else LocalIndication.current,
                                ) { onSelect(Screen.LocalPlaylist(p.id)) }
                                .padding(horizontal = if (collapsed) 0.dp else 14.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = if (collapsed) Arrangement.Center else Arrangement.Start,
                        ) {
                            Icon(
                                imageVector = Icons.Filled.PlaylistPlay,
                                contentDescription = p.name,
                                tint = if (selected) selectedFg
                                else if (spotify) MaterialTheme.colorScheme.onSurfaceVariant
                                else MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(20.dp),
                            )
                            if (!collapsed) {
                                Spacer(Modifier.width(12.dp))
                                Text(
                                    text = p.name,
                                    style = MaterialTheme.typography.bodyMedium.copy(
                                        fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal
                                    ),
                                    color = if (selected) selectedFg else MaterialTheme.colorScheme.onSurface,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                )
                            }
                        }
                        Spacer(Modifier.height(2.dp))
                    }

                    // Logged-in YouTube Playlists
                    if (isLoggedIn) {
                        // The account's playlists that are already a local
                        // playlist — mirrored into the local list above
                        // (`yt-<id>`), or created here and pushed to the account
                        // (`remoteId`) — are not listed again here: doing so
                        // listed every synced playlist twice, which is exactly
                        // what the sync is there to remove.
                        // The account's playlists that already have a local entry
                        // are not listed here. Two things are compared, because
                        // one is not enough:
                        //
                        //  * the account ids of **every** local playlist —
                        //    including the copies a merge retired (a tombstone
                        //    still names the account playlist it was merged into,
                        //    and that playlist is of course still on YouTube
                        //    Music). Looking at the active ones only is why the
                        //    duplicates kept coming back *here* after they were
                        //    gone everywhere else: the retired copy was listed
                        //    again from the online list.
                        //  * the names of the active local playlists, for the
                        //    copies the account holds under a *different* id —
                        //    the pair of playlists a two-device sync used to
                        //    create (one per device, same name, different id) is
                        //    exactly that, and only one of them is the local one.
                        val localAccountIds = remember(localPlaylists) {
                            localPlaylists
                                .flatMap { p ->
                                    listOfNotNull(PlaylistSync.run { p.accountPlaylistId() }, p.id)
                                }
                                .toSet()
                        }
                        val localNames = remember(activeLocalPlaylists) {
                            activeLocalPlaylists.map { it.name.trim().lowercase() }.toSet()
                        }
                        val online = onlinePlaylists
                            .distinctBy { it.id }
                            .filterNot { it.id in localAccountIds || it.title.trim().lowercase() in localNames }
                        if (onlinePlaylists.isNotEmpty() && online.size != onlinePlaylists.size) {
                            AppLog.log(
                                "playlists",
                                "sidebar: ${onlinePlaylists.size} account playlist(s) listed, " +
                                    "${onlinePlaylists.size - online.size} already in the local library",
                            )
                        }
                        online.forEach { op ->
                            val selected = current == Screen.Playlist(op.id)
                            val interaction = remember(op.id) { MutableInteractionSource() }
                            val hovered by interaction.collectIsHoveredAsState()
                            Row(
                                Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(subRadius))
                                    .background(
                                        when {
                                            selected -> selectedBg
                                            spotify && hovered -> MaterialTheme.colorScheme.surfaceContainerHigh
                                            else -> Color.Transparent
                                        }
                                    )
                                    .clickable(
                                        interactionSource = interaction,
                                        indication = if (spotify) null else LocalIndication.current,
                                    ) { onSelect(Screen.Playlist(op.id)) }
                                    .padding(horizontal = if (collapsed) 0.dp else 14.dp, vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = if (collapsed) Arrangement.Center else Arrangement.Start,
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.MusicNote,
                                    contentDescription = op.title,
                                    tint = if (selected) selectedFg
                                    else if (spotify) MaterialTheme.colorScheme.onSurfaceVariant
                                    else MaterialTheme.colorScheme.secondary,
                                    modifier = Modifier.size(20.dp),
                                )
                                if (!collapsed) {
                                    Spacer(Modifier.width(12.dp))
                                    Text(
                                        text = op.title,
                                        style = MaterialTheme.typography.bodyMedium.copy(
                                            fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal
                                        ),
                                        color = if (selected) selectedFg else MaterialTheme.colorScheme.onSurface,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                    )
                                }
                            }
                            Spacer(Modifier.height(2.dp))
                        }
                    }

                    // Fallback Favourite Songs entry if no playlists exist yet
                    if (activeLocalPlaylists.isEmpty() && onlinePlaylists.isEmpty()) {
                        val favSelected = current == Screen.LocalPlaylists
                        val interaction = remember { MutableInteractionSource() }
                        val hovered by interaction.collectIsHoveredAsState()
                        Row(
                            Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(subRadius))
                                .background(
                                    when {
                                        favSelected -> selectedBg
                                        spotify && hovered -> MaterialTheme.colorScheme.surfaceContainerHigh
                                        else -> Color.Transparent
                                    }
                                )
                                .clickable(
                                    interactionSource = interaction,
                                    indication = if (spotify) null else LocalIndication.current,
                                ) { onSelect(Screen.LocalPlaylists) }
                                .padding(horizontal = if (collapsed) 0.dp else 14.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = if (collapsed) Arrangement.Center else Arrangement.Start,
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Favorite,
                                contentDescription = "Favourite Songs",
                                tint = if (favSelected) selectedFg
                                else if (spotify) MaterialTheme.colorScheme.onSurfaceVariant
                                else MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(20.dp),
                            )
                            if (!collapsed) {
                                Spacer(Modifier.width(12.dp))
                                Text(
                                    "Favourite Songs",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = if (favSelected) selectedFg else MaterialTheme.colorScheme.onSurface,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                )
                            }
                        }
                    }
                    }
                }
            }

            if (showCreateDialog) {
                PlaylistNameDialog(
                    language = language,
                    initialName = "",
                    confirmLabel = Localization.get(language, "create").let { if (it == "create" || it.isBlank()) "Create" else it },
                    onConfirm = { name ->
                        val newPlaylist = PlaylistStore.create(name)
                        showCreateDialog = false
                        onSelect(Screen.LocalPlaylist(newPlaylist.id))
                    },
                    onDismiss = { showCreateDialog = false },
                    allowSyncing = isLoggedIn,
                    // Created on the account by [PlaylistSync] now, which also
                    // records the account's id on the local playlist: the old
                    // inline call created a copy over there and then lost it,
                    // leaving a stray playlist on the account and an unlinked one
                    // here.
                    onConfirmSynced = { name ->
                        val newPlaylist = PlaylistStore.create(name)
                        PlaylistSync.pushPlaylist(newPlaylist.id)
                        showCreateDialog = false
                        onSelect(Screen.LocalPlaylist(newPlaylist.id))
                    },
                )
            }

            Spacer(Modifier.height(8.dp))

            // User Account Row (Pinned Bottom Active-Pill). It opens the
            // Account screen directly: the row IS the account (name, avatar),
            // so landing on the General list made the user click twice for the
            // thing they had just clicked. The gear in the rail is the way to
            // the General list.
            val settingsSelected = current == Screen.Settings || current == Screen.SettingsAccount
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(14.dp))
                    .clickable { onSelect(Screen.SettingsAccount) },
                color = if (settingsSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.primaryContainer,
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(if (settingsSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.primary),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(
                            text = userName.take(1).uppercase(),
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                            color = if (settingsSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onPrimary,
                        )
                    }

                    if (!collapsed) {
                        Spacer(Modifier.width(10.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = userName,
                                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                                color = if (settingsSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onPrimaryContainer,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                            Text(
                                text = userHandle,
                                style = MaterialTheme.typography.labelSmall,
                                color = if (settingsSelected) MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.8f) else MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.75f),
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



@Composable
fun MiniPlayer(
    language: String,
    nowPlaying: NowPlaying?,
    isPlaying: Boolean,
    isLoading: Boolean,
    positionMs: Long,
    durationMs: Long,
    onTogglePlay: () -> Unit,
    onNext: () -> Unit,
    onOpen: () -> Unit,
    onOpenQueue: () -> Unit,
    style: MiniPlayerStyle = MiniPlayerStyle.STANDARD,
) {
    val np = nowPlaying ?: return
    val pureBlack = style == MiniPlayerStyle.PURE_BLACK

    // Swipe-to-expand: dragging the mini player up reveals the full player.
    var dragOffset by remember { mutableStateOf(0f) }
    val openThreshold = 90.dp
    val density = LocalDensity.current
    val thresholdPx = with(density) { openThreshold.toPx() }
    Box(
        Modifier
            .fillMaxWidth()
            .graphicsLayer { translationY = -dragOffset.coerceAtLeast(0f) }
            .pointerInput(Unit) {
                detectVerticalDragGestures(
                    onVerticalDrag = { _, dragAmount ->
                        dragOffset = (dragOffset + dragAmount).coerceAtLeast(0f)
                    },
                    onDragEnd = {
                        if (dragOffset > thresholdPx) onOpen()
                        dragOffset = 0f
                    },
                    onDragCancel = { dragOffset = 0f },
                )
            },
    ) {
        Surface(
            color = when {
                pureBlack -> Color.Black
                style == MiniPlayerStyle.OUTLINE -> MaterialTheme.colorScheme.surface
                else -> MaterialTheme.colorScheme.surfaceContainerHigh
            },
            tonalElevation = if (style == MiniPlayerStyle.OUTLINE) 0.dp else 4.dp,
            shadowElevation = 4.dp,
            border = if (style == MiniPlayerStyle.OUTLINE) {
                BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
            } else null,
            shape = when (style) {
                MiniPlayerStyle.APPLE -> RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp)
                MiniPlayerStyle.OUTLINE -> RoundedCornerShape(14.dp)
                else -> RoundedCornerShape(0.dp)
            },
        ) {
            Column {
                // Drag handle (hint that the bar can be swiped up to expand).
                Box(
                    Modifier.fillMaxWidth().padding(top = 4.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Box(
                        Modifier
                            .width(36.dp)
                            .height(3.dp)
                            .clip(RoundedCornerShape(2.dp))
                            .background(if (pureBlack) Color.White.copy(alpha = 0.4f) else MaterialTheme.colorScheme.outlineVariant),
                    )
                }
                if (durationMs > 0) {
                    LinearProgressIndicator(
                        progress = { (positionMs.toFloat() / durationMs.toFloat()).coerceIn(0f, 1f) },
                        modifier = Modifier.fillMaxWidth().height(if (style == MiniPlayerStyle.APPLE) 3.dp else 2.dp),
                        color = if (pureBlack) Color.White else MaterialTheme.colorScheme.primary,
                    )
                }
                Row(
                    Modifier
                        .fillMaxWidth()
                        .clickable(onClick = onOpen)
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    if (style == MiniPlayerStyle.APPLE) {
                        Box(Modifier.clip(RoundedCornerShape(10.dp))) {
                            Thumbnail(np.thumbnail, Modifier.size(40.dp))
                        }
                    } else {
                        Thumbnail(np.thumbnail, Modifier.size(44.dp))
                    }
                    Spacer(Modifier.width(12.dp))
                    Column(Modifier.weight(1f)) {
                        Text(
                            np.title,
                            style = MaterialTheme.typography.bodyLarge,
                            color = if (pureBlack) Color.White else MaterialTheme.colorScheme.onSurface,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                        Text(
                            np.artist,
                            style = MaterialTheme.typography.bodySmall,
                            color = if (pureBlack) Color.White.copy(alpha = 0.7f) else MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                    // Listen Together guest: the transport in this bar is the
                    // host's. The lock rides with the play button the user is
                    // about to press, and (1.54.24) the controls themselves are
                    // drawn grey and inert: a play button that looks usable and
                    // does nothing was the report. The keyboard and media keys
                    // still answer with the reason, through PlayerController.
                    val transportOff = transportLocked()
                    if (transportOff) {
                        Tooltip(Localization.get(language, "lt_guest_note")) {
                            Icon(
                                Icons.Filled.Lock,
                                contentDescription = Localization.get(language, "lt_guest_note"),
                                tint = if (pureBlack) Color.White.copy(alpha = 0.7f) else MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(16.dp),
                            )
                        }
                        Spacer(Modifier.width(6.dp))
                    }
                    Tooltip(if (transportOff) guestLockTooltip(language) else Localization.get(language, if (isPlaying) "pause" else "play")) {
                        IconButton(onClick = onTogglePlay, enabled = !transportOff) {
                            if (isLoading) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(18.dp),
                                    strokeWidth = 2.dp,
                                    color = if (pureBlack) Color.White else MaterialTheme.colorScheme.primary,
                                )
                            } else {
                                Icon(
                                    if (isPlaying) Icons.Filled.Pause else Icons.Filled.PlayArrow,
                                    contentDescription = Localization.get(language, if (isPlaying) "pause" else "play"),
                                    tint = if (pureBlack) Color.White else LocalContentColor.current,
                                    modifier = Modifier
                                        .size(22.dp)
                                        .alpha(if (transportOff) LOCKED_CONTROL_ALPHA else 1f),
                                )
                            }
                        }
                    }
                    Tooltip(if (transportOff) guestLockTooltip(language) else Localization.get(language, "tooltip_next")) {
                        IconButton(onClick = onNext, enabled = !transportOff) {
                            Icon(
                                Icons.Filled.SkipNext,
                                contentDescription = "Next",
                                tint = if (pureBlack) Color.White else LocalContentColor.current,
                                modifier = Modifier
                                    .size(22.dp)
                                    .alpha(if (transportOff) LOCKED_CONTROL_ALPHA else 1f),
                            )
                        }
                    }
                    Tooltip(Localization.get(language, "tooltip_queue")) {
                        IconButton(onClick = onOpenQueue) {
                            Icon(
                                Icons.AutoMirrored.Filled.QueueMusic,
                                contentDescription = "Queue",
                                tint = if (pureBlack) Color.White else LocalContentColor.current,
                                modifier = Modifier.size(22.dp),
                            )
                        }
                    }
                }
            }
        }
    }
}


/**
 * Generic non-invasive in-app notification banner (title + message + dismiss),
 * used for notifications dispatched in "main window" mode.
 */
@Composable
fun BoxScope.InAppNotification(
    title: String,
    message: String,
    language: String,
    onDismiss: () -> Unit,
) {
    Surface(
        tonalElevation = 6.dp,
        shadowElevation = 8.dp,
        shape = RoundedCornerShape(12.dp),
        modifier = Modifier.align(Alignment.TopEnd).padding(16.dp),
    ) {
        Row(
            Modifier.padding(start = 16.dp, top = 12.dp, bottom = 12.dp, end = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(Modifier.widthIn(max = 300.dp)) {
                Text(
                    title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                )
                Text(
                    message,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            TextButton(onClick = onDismiss, modifier = Modifier.padding(start = 12.dp)) {
                Text(Localization.get(language, "dismiss"))
            }
        }
    }
}

/**
 * One-off banner shown right after the developer options are unlocked,
 * pointing the user to the settings screen where they can configure them.
 */
@Composable
fun BoxScope.DevUnlockedNotification(
    language: String,
    onOpen: () -> Unit,
    onDismiss: () -> Unit,
) {
    Surface(
        tonalElevation = 6.dp,
        shadowElevation = 8.dp,
        shape = RoundedCornerShape(12.dp),
        modifier = Modifier.align(Alignment.TopEnd).padding(16.dp),
    ) {
        Row(
            Modifier.padding(start = 16.dp, top = 12.dp, bottom = 12.dp, end = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(Modifier.widthIn(max = 300.dp)) {
                Text(
                    Localization.get(language, "dev_unlocked_title"),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                )
                Text(
                    Localization.get(language, "dev_unlocked_desc"),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Button(onClick = onOpen, modifier = Modifier.padding(start = 12.dp)) {
                Text(Localization.get(language, "dev_unlocked_open"))
            }
            TextButton(onClick = onDismiss) {
                Text(Localization.get(language, "dismiss"))
            }
        }
    }
}

/**
 * Non-invasive banner shown when a newer desktop release is available, with
 * "Install now" (download + launch the installer) and a dismiss button.
 */
@Composable
fun BoxScope.UpdateNotification(
    status: UpdateStatus.Available,
    language: String,
    onDismiss: () -> Unit,
    onDone: () -> Unit,
) {
    val scope = rememberCoroutineScope()
    val progress by UpdateState.progress.collectAsState()
    val downloadedFile by UpdateState.downloadedFile.collectAsState()
    // Shared with the Updates screen: if the installer for this version is
    // already downloaded (from anywhere), offer to open it, not re-download.
    val existingInstaller = downloadedFile

    Surface(
        tonalElevation = 6.dp,
        shadowElevation = 8.dp,
        shape = RoundedCornerShape(12.dp),
        modifier = Modifier
            .align(Alignment.TopEnd)
            .padding(16.dp),
    ) {
        Row(
            Modifier.padding(start = 16.dp, top = 12.dp, bottom = 12.dp, end = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(Modifier.widthIn(max = 300.dp)) {
                Text(
                    Localization.get(language, "update_available"),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                )
                Text(
                    status.version,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                progress?.let { p ->
                    LinearProgressIndicator(
                        progress = { p.percent / 100f },
                        modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                    )
                }
            }
            Button(
                onClick = {
                    if (progress != null) return@Button
                    existingInstaller?.let { file ->
                        scope.launch {
                            prepareAndOpenInstaller(file)
                            onDone()
                        }
                        return@Button
                    }
                    val asset = status.asset
                    if (asset == null) {
                        openUrl(status.url)
                        onDone()
                    } else {
                        scope.launch {
                            // Started on UpdateState's own scope: the transfer is
                            // not tied to this banner (or to the Updates screen),
                            // so a screen change can no longer cut it in half.
                            UpdateState.startDownload(asset).join()
                            UpdateState.downloadedFile.value?.let { prepareAndOpenInstaller(it) }
                            onDone()
                        }
                    }
                },
                enabled = progress == null,
                modifier = Modifier.padding(start = 12.dp),
            ) {
                Text(
                    Localization.get(
                        language,
                        when {
                            existingInstaller != null -> "open_installer"
                            progress != null -> "downloading"
                            else -> "install_now"
                        },
                    )
                )
            }
            Tooltip(Localization.get(language, "dismiss")) {
                IconButton(onClick = onDismiss) {
                    Icon(Icons.Filled.Close, contentDescription = Localization.get(language, "dismiss"))
                }
            }
        }
    }
}

internal fun openUrl(url: String): Boolean {
    if (Desktop.isDesktopSupported() && Desktop.getDesktop().isSupported(Desktop.Action.BROWSE)) {
        if (runCatching { Desktop.getDesktop().browse(URI(url)) }.isSuccess) return true
    }
    return runCatching {
        val cmd = when (Platform.os) {
            DesktopOs.WINDOWS -> listOf("cmd", "/c", "start", "", url)
            DesktopOs.MACOS -> listOf("open", url)
            DesktopOs.LINUX -> listOf("xdg-open", url)
        }
        ProcessBuilder(cmd).start()
        true
    }.getOrDefault(false)
}

/**
 * Density calibration factor: the desktop layout is drawn generously, so
 * the "100%" preset would look oversized next to the phone. Scaling 100%
 * down to 0.75x makes it match the size the mobile UI has at 75%, while
 * keeping the relative presets (200% still zooms in, 55% zooms out).
 */
private const val DENSITY_CALIBRATION = 0.75f

/** JSON codec for persisting the queue between sessions. */

/** JSON codec for the bundled `contributorsde.json` (ignores `_guide` etc.). */
private val contributorsJson = sharedJson

/** Key used to detect discrete playback changes worth syncing (no per-frame pushes). */
private data class PlaybackSyncKey(
    val trackId: String?,
    val isPlaying: Boolean,
    val isResolving: Boolean,
    val index: Int,
    val queue: List<String>,
    val repeatMode: String,
    val isShuffle: Boolean,
)

/** Mutable echo-suppression state for a volume sync loop. */
private class VolumeGuard {
    var echoUntil = 0L
    var echoValue = -1f
    var lastPushed: Float? = null
}

private fun SongItem.toSyncedSong() = SyncedSong(
    id = id,
    title = title,
    artist = artists.joinToString(", ") { it.name },
    thumbnail = thumbnail,
)

/** Builds a [PlaybackSnapshot] from the current player state (null if nothing plays). */
private fun PlayerController.toPlaybackSnapshot(): PlaybackSnapshot? {
    val s = state.value
    val current = s.current ?: return null
    return PlaybackSnapshot(
        trackId = current.videoId,
        trackTitle = current.title,
        positionMs = s.positionMs,
        isResolving = s.isResolving,
        isPlaying = s.isPlaying,
        volume = if (DesktopSettings.load().syncViviVolume) s.volume else null,
        // Same toggle as the in-app channel: the OS volume must not leave this
        // device when volume sync is disabled.
        systemVolume = if (DesktopSettings.load().syncNativeVolume) SystemVolume.get() else null,
        repeatMode = s.repeatMode.name,
        isShuffle = s.isShuffle,
        queue = s.queue.map { np ->
            // The length travels with the queue: the peer's seek bar needs a real
            // range, and a queue that arrives here without durations leaves it
            // with a 0:00 bar (the same bug this side just fixed) — so the
            // desktop must not be the one sending the empty lengths.
            TrackRef(
                id = np.videoId,
                title = np.title,
                artist = np.artist,
                thumbnail = np.thumbnail,
                durationMs = np.durationMs,
            )
        },
        queueIndex = s.index,
    )
}

/** Maps the desktop theme/language/accent onto the Android shared-preference keys. */
private fun desktopSettingsMap(
    language: String,
    themeMode: ThemeMode,
    accent: Color,
    syncViviVolume: Boolean,
): Map<String, String> {
    val s = DesktopSettings.load()
    return mapOf(
    "appLanguage" to Languages.toMobileCode(language).ifBlank { "SYSTEM_DEFAULT" },
    // Source markers for bidirectional language sync: (deviceId, seq) where
    // seq grows on every manual language change on THIS device. The peer only
    // applies the language when the markers prove it is a newer manual change
    // (never an echo or a stale stored value pushed at pair time).
    "languageDeviceId" to s.deviceId,
    "languageSeq" to s.languageSeq.toString(),
    "darkMode" to when (themeMode) {
        ThemeMode.SYSTEM -> "AUTO"
        ThemeMode.LIGHT -> "OFF"
        ThemeMode.DARK -> "ON"
    },
    "selectedThemeColor" to colorToArgbInt(accent).toString(),
    "pureBlack" to "false",
    "dynamicTheme" to "false",
    "syncViviVolume" to syncViviVolume.toString(),
    )
}
