package com.music.vivi.desktop

import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.QueueMusic
import androidx.compose.material.icons.automirrored.filled.Subject
import androidx.compose.material.icons.automirrored.filled.PlaylistPlay
import androidx.compose.material.icons.filled.Album
import androidx.compose.material.icons.filled.Bedtime
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Radio
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.ThumbDown
import androidx.compose.material.icons.filled.FastForward
import androidx.compose.material.icons.filled.FastRewind
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.Headphones
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.PlaylistAdd
import androidx.compose.material.icons.filled.QueuePlayNext
import androidx.compose.material.icons.filled.Repeat
import androidx.compose.material.icons.filled.RepeatOne
import androidx.compose.material.icons.filled.Shuffle
import androidx.compose.material.icons.filled.VolumeDown
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import com.music.innertube.YouTube
import com.music.innertube.YouTube.SearchFilter
import com.music.innertube.models.AlbumItem
import com.music.innertube.models.ArtistItem
import com.music.lyrics.LyricsParser
import com.music.vivi.desktop.player.AudioOutput
import com.music.vivi.desktop.player.PlaybackTech
import com.music.vivi.desktop.player.RepeatMode
import kotlinx.coroutines.launch
import java.util.Locale

/**
 * "Apple Player v17": the BitChord desktop player, ported to the DE on top of
 * the same PlayerController as every other design.
 *
 * The layout is BitChord's: the artwork is a large square on the left and the
 * whole control set is a column on the right (the context caption, the title
 * with the like and menu circles, the artist, the lyric row, the scrubber with
 * the elapsed and remaining time, the transport, and the volume). The queue
 * replaces that column with BitChord's own queue panel instead of opening a
 * separate screen, and the "Stats for nerds" overlay is the tiny monospace line
 * BitChord prints on the artwork, whose full detail is one hover away.
 *
 * Ported from BitChord's `NowPlayingScreen` / `PlayerControls` (GPLv3).
 */
@Composable
internal fun ApplePlayerV17Screen(
    np: NowPlaying,
    isPlaying: Boolean,
    isLoading: Boolean,
    positionMs: Long,
    durationMs: Long,
    volume: Float,
    isShuffle: Boolean,
    repeatMode: RepeatMode,
    errorKey: String?,
    errorDetail: String?,
    onTogglePlay: () -> Unit,
    onNext: () -> Unit,
    onPrevious: () -> Unit,
    onSeek: (Long) -> Unit,
    onVolume: (Float) -> Unit,
    onToggleShuffle: () -> Unit,
    onCycleRepeat: () -> Unit,
    /** The window-level "Autoplay next track" setting (Settings -> Player). */
    autoPlayNext: Boolean = true,
    onToggleAutoPlayNext: (Boolean) -> Unit = {},
    language: String,
    onOpenLyrics: () -> Unit,
    onOpenQueue: () -> Unit,
    onAddToPlaylist: () -> Unit,
    sliderStyle: ViviSliderStyle,
    onBack: (() -> Unit)?,
    queue: List<NowPlaying> = emptyList(),
    queueIndex: Int = 0,
    onSkipTo: (Int) -> Unit = {},
    onRemoveAt: (Int) -> Unit = {},
    /** The player background the options pick (canvas, gradient, blur, glow…). */
    background: PlayerBackgroundStyle = PlayerBackgroundStyle.APPLE_MUSIC,
    /** Accent wash the background styles are drawn with. */
    accent: Color = MaterialTheme.colorScheme.primary,
    /** Opens the playlist picker for a queue row. */
    onAddToPlaylistSong: ((NowPlaying) -> Unit)? = null,
    /** Replaces the queue, used by the rows' Play next and Add to queue. */
    onReorderQueue: (List<NowPlaying>) -> Unit = {},
    /** Where the queue was started from, the reference's "Playing from". */
    queueSource: String? = null,
    /** The song menu's queue actions (the same calls the rest of the app uses). */
    onPlayNext: (() -> Unit)? = null,
    onAddToQueue: (() -> Unit)? = null,
    onStartRadio: (() -> Unit)? = null,
    /** True when this track already has a complete copy in the audio cache. */
    isDownloadedNow: Boolean = false,
    onDownload: (() -> Unit)? = null,
    onDeleteDownload: (() -> Unit)? = null,
    /** Opens the album / artist page; the id is resolved from the names. */
    onOpenAlbum: ((String) -> Unit)? = null,
    onOpenArtist: ((String) -> Unit)? = null,
    sleepTimerMinutes: Int? = null,
    onSleepTimer: (Int?) -> Unit = {},
    /** Opens on the queue instead of the controls, for the render harness. */
    initialQueueOpen: Boolean = false,
    /** Opens on the lyrics page instead of the controls (render harness). */
    initialLyricsOpen: Boolean = false,
    /** Draws the bar\'s playback row from the first frame (render harness). */
    initialControlsRow: Boolean = false,
    modifier: Modifier = Modifier,
) {
    val showStats = remember(settingsFileRevision()) { DesktopSettings.load().statsForNerds }
    val tech by PlaybackTech.state.collectAsState()
    val scrim = MaterialTheme.colorScheme.scrim
    val contentColor = MaterialTheme.colorScheme.onSurface
    val mutedColor = MaterialTheme.colorScheme.onSurfaceVariant
    val primaryColor = MaterialTheme.colorScheme.primary
    val duration = durationMs.coerceAtLeast(1L)

    var showQueuePanel by remember(np.videoId) { mutableStateOf(initialQueueOpen) }
    // The lyrics page takes the controls' place, the way the reference does it,
    // instead of pushing the separate lyrics screen with the transport still
    // underneath it.
    var showLyricsPanel by remember(np.videoId) { mutableStateOf(initialLyricsOpen) }
    var menuOpen by remember(np.videoId) { mutableStateOf(false) }
    var liked by remember(np.videoId) { mutableStateOf(SongActions.isLiked(np.videoId)) }
    val scope = rememberCoroutineScope()

    BoxWithConstraints(modifier.fillMaxSize()) {
        // Opaque on purpose. This is the one surface drawn over a translucent
        // backdrop, so whatever the window has behind it reached the eye through
        // the blurred cover: at a narrow window a band of white blocks showed
        // across the bottom, behind the volume row and the three buttons. A
        // solid base closes that off whatever draws it; the blurred cover and
        // the scrim are painted over this.
        Box(
            Modifier
                .fillMaxSize()
                .background(scrim),
        )
        // The two-column form is for a wide window, and "wide" is the window's
        // own width: at a narrow one the density step-down makes `maxWidth`
        // report 933 dp in a 700 px window, so a breakpoint on it never fired and
        // the narrow layout could not be reached. Outside a window (the render
        // harness) the composition's own width is used.
        val windowWidthDp = LocalWindowWidthDp.current
        val wide = if (windowWidthDp != null) windowWidthDp >= 820f else maxWidth >= 820.dp
        // Read once here: inside the nested Box of the stacked form the
        // BoxWithConstraints' own height is no longer reachable implicitly.
        val windowMaxHeight = maxHeight
        val artSize = if (wide) {
            minOf(maxWidth * 0.40f, maxHeight - 140.dp).coerceAtLeast(220.dp)
        } else {
            minOf(maxWidth * 0.62f, maxHeight * 0.36f).coerceAtLeast(160.dp)
        }

        // The background style the option picks is this screen's base. It used
        // to be painted only for the other designs, so choosing Canvas, the
        // gradient or the visualizer here did nothing at all.
        PlayerBackground(
            style = background,
            bgUrl = np.thumbnail,
            accent = accent,
            modifier = Modifier.fillMaxSize(),
            isPlaying = isPlaying,
        )
        // Only the two styles that ARE a blurred cover keep BitChord's own blur:
        // drawn over the canvas or the visualizer it would hide them.
        val ownBackdrop = background == PlayerBackgroundStyle.BLUR ||
            background == PlayerBackgroundStyle.APPLE_MUSIC
        if (ownBackdrop && !np.thumbnail.isNullOrBlank()) {
            // The project's own fixed-size blur: the artwork is rasterized at
            // 256 dp once and only scaled afterwards, so a window resize is a
            // transform instead of a re-decode of a full-window bitmap. Painting
            // the raw cover instead let its own text show through the controls.
            BlurredBackdrop(
                url = np.thumbnail,
                blurRadius = 56.dp,
                modifier = Modifier
                    .fillMaxSize()
                    .graphicsLayer { alpha = 0.55f },
                extraScale = 1.2f,
            )
        }
        Box(
            Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        listOf(
                            scrim.copy(alpha = if (ownBackdrop) 0.28f else 0.55f),
                            scrim.copy(alpha = if (ownBackdrop) 0.80f else 0.88f),
                        ),
                    ),
                ),
        )
        // The cover is asked for at the reference's player rung (1200 px, see
        // `ArtworkPx`), the size BitChord loads for the same square, so the
        // artwork is not a 120 px variant stretched over half the screen.
        val artworkUrl = adjustedThumbnailUrl(
            np.thumbnail,
            requestedPx = ArtworkPx.PLAYER,
            dataSaver = DesktopSettings.load().dataSaver,
        )

        val artwork: @Composable () -> Unit = {
            Box(
                Modifier
                    .size(artSize)
                    .clip(MaterialTheme.shapes.extraSmall)
                    .background(MaterialTheme.colorScheme.surfaceContainerHigh)
                    .pointerInput(np.videoId) {
                        var dragged = 0f
                        detectHorizontalDragGestures(
                            onHorizontalDrag = { _, amount -> dragged += amount },
                            onDragEnd = {
                                val threshold = 80.dp.toPx()
                                when {
                                    dragged <= -threshold -> onNext()
                                    dragged >= threshold -> onPrevious()
                                }
                                dragged = 0f
                            },
                            onDragCancel = { dragged = 0f },
                        )
                    }
                    .clickable(onClick = onTogglePlay),
            ) {
                AsyncImage(
                    model = artworkUrl,
                    contentDescription = np.title,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize(),
                )
                if (isLoading) {
                    Box(
                        Modifier
                            .fillMaxSize()
                            .background(scrim.copy(alpha = 0.24f)),
                        contentAlignment = Alignment.Center,
                    ) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(32.dp),
                            strokeWidth = 3.dp,
                            color = MaterialTheme.colorScheme.inverseOnSurface,
                        )
                    }
                }
                if (showStats) {
                    // The alignment has to sit on a DIRECT child of this Box:
                    // passed through `Tooltip` it became parent data for the
                    // tooltip's own layout and the line drifted to the top.
                    Box(
                        Modifier
                            .align(Alignment.BottomStart)
                            .padding(10.dp),
                    ) {
                        StatsLine(tech = tech, language = language)
                    }
                }
            }
        }

        val info: @Composable () -> Unit = {
            // No fixed maximum width: the column fills the cell the Row gives it.
            // A 620 dp cap was wider than that cell on a scaled 1300 px window,
            // so the scrubber, the volume and the three links ran past the right
            // edge of the player.
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.Center,
            ) {
                // The reference's caption: where the queue was started from.
                // The album is the fallback for a queue with no source of its
                // own (a single song from a search), and the artist when the
                // track carries no album at all.
                val playingFrom = queueSource?.takeIf { it.isNotBlank() }
                    ?: np.album?.takeIf { it.isNotBlank() }
                Tooltip(playingFrom ?: Localization.get(language, "now_playing")) {
                    Text(
                        if (playingFrom != null) "${Localization.get(language, "playing_from")} $playingFrom"
                        else Localization.get(language, "now_playing"),
                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
                        color = mutedColor,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }

                Spacer(Modifier.height(6.dp))

                Row(verticalAlignment = Alignment.Top) {
                    Column(Modifier.weight(1f)) {
                        Tooltip(np.title) {
                            Text(
                                np.title,
                                style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold),
                                color = contentColor,
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis,
                            )
                        }
                        Tooltip(np.artist) {
                            Text(
                                np.artist,
                                style = MaterialTheme.typography.titleMedium,
                                color = mutedColor,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                        }
                    }
                    Spacer(Modifier.width(12.dp))
                    Tooltip(Localization.get(language, "tooltip_favorite")) {
                        Surface(
                            onClick = {
                                val next = !liked
                                liked = next
                                SongActions.setLiked(
                                    np.videoId,
                                    next,
                                    title = np.title,
                                    artist = np.artist,
                                    thumbnail = np.thumbnail,
                                )
                                scope.launch { YouTube.likeVideo(np.videoId, next) }
                            },
                            shape = CircleShape,
                            color = if (liked) primaryColor else MaterialTheme.colorScheme.surfaceContainerHigh,
                            modifier = Modifier.size(38.dp),
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    if (liked) Icons.Filled.Favorite else Icons.Filled.FavoriteBorder,
                                    contentDescription = Localization.get(language, "tooltip_favorite"),
                                    tint = if (liked) MaterialTheme.colorScheme.onPrimary else contentColor,
                                    modifier = Modifier.size(18.dp),
                                )
                            }
                        }
                    }
                    Spacer(Modifier.width(8.dp))
                    Box {
                        Tooltip(Localization.get(language, "more")) {
                            Surface(
                                onClick = { menuOpen = true },
                                shape = CircleShape,
                                color = MaterialTheme.colorScheme.surfaceContainerHigh,
                                modifier = Modifier.size(38.dp),
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        AppleSymbols.More,
                                        contentDescription = Localization.get(language, "more"),
                                        tint = contentColor,
                                        modifier = Modifier.size(20.dp),
                                    )
                                }
                            }
                        }
                        DropdownMenu(expanded = menuOpen, onDismissRequest = { menuOpen = false }) {
                            V17PlayerMenuContent(
                                language = language,
                                liked = liked,
                                downloaded = isDownloadedNow,
                                sleepTimerMinutes = sleepTimerMinutes,
                                onLike = {
                                    menuOpen = false
                                    val next = !liked
                                    liked = next
                                    SongActions.setLiked(
                                        np.videoId,
                                        next,
                                        title = np.title,
                                        artist = np.artist,
                                        thumbnail = np.thumbnail,
                                    )
                                    scope.launch { YouTube.likeVideo(np.videoId, next) }
                                },
                                onDislike = {
                                    menuOpen = false
                                    // A dislike is the account's thumb-down and
                                    // clears the heart: YouTube keeps the two
                                    // ratings exclusive.
                                    if (liked) {
                                        liked = false
                                        SongActions.setLiked(
                                            np.videoId,
                                            false,
                                            title = np.title,
                                            artist = np.artist,
                                            thumbnail = np.thumbnail,
                                        )
                                    }
                                    scope.launch { YouTube.dislikeVideo(np.videoId) }
                                },
                                onAddToPlaylist = {
                                    menuOpen = false
                                    onAddToPlaylist()
                                },
                                onDownload = {
                                    menuOpen = false
                                    onDownload?.invoke()
                                },
                                onDeleteDownload = {
                                    menuOpen = false
                                    onDeleteDownload?.invoke()
                                },
                                onStartRadio = {
                                    menuOpen = false
                                    onStartRadio?.invoke()
                                },
                                onPlayNext = {
                                    menuOpen = false
                                    onPlayNext?.invoke()
                                },
                                onAddToQueue = {
                                    menuOpen = false
                                    onAddToQueue?.invoke()
                                },
                                onOpenAlbum = onOpenAlbum?.let { open ->
                                    {
                                        menuOpen = false
                                        // The player carries names, not ids, so
                                        // the album is resolved by its own name
                                        // and the first card the search returns.
                                        scope.launch {
                                            val query = listOfNotNull(np.album, np.artist)
                                                .joinToString(" ").trim()
                                            YouTube.search(query, SearchFilter.FILTER_ALBUM)
                                                .onSuccess { result ->
                                                    result.items.filterIsInstance<AlbumItem>()
                                                        .firstOrNull()
                                                        ?.let { open(it.id) }
                                                }
                                        }
                                    }
                                } ?: {},
                                onOpenArtist = onOpenArtist?.let { open ->
                                    {
                                        menuOpen = false
                                        scope.launch {
                                            YouTube.search(np.artist, SearchFilter.FILTER_ARTIST)
                                                .onSuccess { result ->
                                                    result.items.filterIsInstance<ArtistItem>()
                                                        .firstOrNull()
                                                        ?.let { open(it.id) }
                                                }
                                        }
                                    }
                                } ?: {},
                                onSleepTimer = { minutes ->
                                    menuOpen = false
                                    onSleepTimer(minutes)
                                },
                                onShare = {
                                    menuOpen = false
                                    copyToClipboard("https://music.youtube.com/watch?v=${np.videoId}")
                                },
                            )
                        }
                    }
                }

                Spacer(Modifier.height(14.dp))

                val inlineLyricsOn = remember(settingsFileRevision()) {
                    DesktopSettings.load().inlineLyrics
                }
                if (inlineLyricsOn) {
                    // BitChord prints the line being sung here, right above the
                    // scrubber, and opens the lyrics page from it. The line is read
                    // from the same cache and providers the lyrics screen uses. With
                    // the option off there is no row at all: nothing is fetched and
                    // the space it occupied is gone, so the scrubber sits right
                    // under the artist.
                    val inlineLine = rememberInlineLyricLine(np, positionMs)
                    Tooltip(Localization.get(language, "tooltip_lyrics")) {
                        Row(
                            Modifier
                                .fillMaxWidth()
                                .clip(MaterialTheme.shapes.small)
                                .clickable(onClick = onOpenLyrics)
                                .padding(vertical = 4.dp, horizontal = 2.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Icon(
                                Icons.Filled.MusicNote,
                                contentDescription = null,
                                tint = mutedColor,
                                modifier = Modifier.size(16.dp),
                            )
                            Spacer(Modifier.width(6.dp))
                            Text(
                                inlineLine ?: Localization.get(language, "lyrics"),
                                style = MaterialTheme.typography.bodyMedium.copy(
                                    fontWeight = if (inlineLine != null) FontWeight.SemiBold else FontWeight.Normal,
                                ),
                                color = if (inlineLine != null) contentColor.copy(alpha = 0.9f) else mutedColor,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.weight(1f, fill = false),
                            )
                            Spacer(Modifier.width(8.dp))
                            Icon(
                                Icons.Filled.ChevronRight,
                                contentDescription = null,
                                tint = mutedColor,
                                modifier = Modifier.size(18.dp),
                            )
                        }
                    }
                }

                Spacer(Modifier.height(8.dp))

                ViviSlider(
                    value = positionMs.toFloat().coerceIn(0f, duration.toFloat()),
                    onValueChange = { onSeek(it.toLong()) },
                    valueRange = 0f..duration.toFloat(),
                    style = sliderStyle,
                    // Slim from edge to edge, the way the reference draws it.
                    showThumb = false,
                    modifier = Modifier.fillMaxWidth(),
                )
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text(formatTime(positionMs), style = MaterialTheme.typography.labelSmall, color = mutedColor)
                    Text(
                        "-${formatTime((durationMs - positionMs).coerceAtLeast(0L))}",
                        style = MaterialTheme.typography.labelSmall,
                        color = mutedColor,
                    )
                }

                Spacer(Modifier.height(10.dp))

                Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(26.dp, Alignment.CenterHorizontally),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Tooltip(Localization.get(language, "previous")) {
                        M3IconButton(onClick = onPrevious) {
                            Icon(
                                Icons.Filled.FastRewind,
                                contentDescription = Localization.get(language, "previous"),
                                tint = contentColor,
                                modifier = Modifier.size(34.dp),
                            )
                        }
                    }
                    Tooltip(Localization.get(language, if (isPlaying) "pause" else "play")) {
                        M3IconButton(onClick = onTogglePlay) {
                            Icon(
                                if (isPlaying) Icons.Filled.Pause else Icons.Filled.PlayArrow,
                                contentDescription = Localization.get(language, if (isPlaying) "pause" else "play"),
                                tint = contentColor,
                                modifier = Modifier.size(52.dp),
                            )
                        }
                    }
                    Tooltip(Localization.get(language, "next")) {
                        M3IconButton(onClick = onNext) {
                            Icon(
                                Icons.Filled.FastForward,
                                contentDescription = Localization.get(language, "next"),
                                tint = contentColor,
                                modifier = Modifier.size(34.dp),
                            )
                        }
                    }
                }

                Spacer(Modifier.height(8.dp))

                Row(
                    Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(
                        Icons.Filled.VolumeDown,
                        contentDescription = Localization.get(language, "volume"),
                        tint = mutedColor,
                        modifier = Modifier.size(16.dp),
                    )
                    Spacer(Modifier.width(8.dp))
                    ViviSlider(
                        value = volume.coerceIn(0f, 1f),
                        onValueChange = onVolume,
                        valueRange = 0f..1f,
                        style = sliderStyle,
                        showThumb = false,
                        modifier = Modifier.weight(1f),
                    )
                    Spacer(Modifier.width(8.dp))
                    Icon(
                        Icons.Filled.VolumeUp,
                        contentDescription = Localization.get(language, "volume"),
                        tint = mutedColor,
                        modifier = Modifier.size(22.dp),
                    )
                }

                if (errorKey != null || errorDetail != null) {
                    Spacer(Modifier.height(8.dp))
                    if (errorKey != null) {
                        Text(
                            Localization.get(language, errorKey),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.error,
                        )
                    }
                    if (errorDetail != null) {
                        SelectionContainer {
                            Text(
                                errorDetail,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.error,
                            )
                        }
                    }
                }
            }
        }

        // The lyrics settings the rest of the app runs, read here too: the style
        // (fade, glow, karaoke, Apple and the rest), the glow, the blur, the
        // auto-scroll and the click-to-seek are the user's, so the words in this
        // column animate like the words on the lyrics screen instead of falling
        // back to the defaults. Only the typography is the panel's own, because
        // this is the large Apple surface (the size `ApplePlayer` uses for it).
        val lyricsOptions = remember(settingsFileRevision()) {
            lyricsDisplayOptionsFrom(DesktopSettings.load()).copy(
                textSizeSp = 26f,
                lineSpacing = 1.55f,
            )
        }
        val lyricsPanel: @Composable () -> Unit = {
            // The player's own lyrics view: headerless, no back control, and the
            // same loader/cache the lyrics screen uses, so nothing is fetched a
            // second time. It fills the column the controls occupy.
            LyricsScreen(
                nowPlaying = np,
                positionMs = positionMs,
                isPlaying = isPlaying,
                language = language,
                display = lyricsOptions,
                onSeek = onSeek,
                onTogglePlay = onTogglePlay,
                onBack = null,
                showHeader = false,
            )
        }

        val queuePanel: @Composable () -> Unit = {
            AppleV17QueuePanel(
                queue = queue,
                index = queueIndex,
                language = language,
                onClose = { showQueuePanel = false },
                onSkipTo = onSkipTo,
                onRemoveAt = onRemoveAt,
                onAddToPlaylist = onAddToPlaylistSong,
                onReorderQueue = onReorderQueue,
                modifier = Modifier
                    .widthIn(max = 520.dp)
                    .fillMaxHeight(),
            )
        }

        // The reference's action row, defined once and placed at the bottom of
        // the artwork column by both layouts.
        val bottomRow: @Composable (Modifier) -> Unit = { rowModifier ->
            V17BottomBar(
                language = language,
                isShuffle = isShuffle,
                repeatMode = repeatMode,
                autoPlayNext = autoPlayNext,
                videoId = np.videoId,
                lyricsOpen = showLyricsPanel,
                // Only the inline panel counts: under 820 dp the queue is the queue
                // screen, and the row must not claim a panel that is not there.
                queueOpen = wide && showQueuePanel,
                onToggleLyrics = {
                    showLyricsPanel = !showLyricsPanel
                    // One panel at a time: the column is swapped, not stacked.
                    if (showLyricsPanel) showQueuePanel = false
                },
                onToggleShuffle = onToggleShuffle,
                onCycleRepeat = onCycleRepeat,
                onToggleAutoPlayNext = onToggleAutoPlayNext,
                onOpenQueue = {
                    when {
                        showQueuePanel -> showQueuePanel = false
                        else -> {
                            showLyricsPanel = false
                            if (wide) showQueuePanel = true else onOpenQueue()
                        }
                    }
                },
                modifier = rowModifier,
            )
        }

        if (wide) {
            Row(
                Modifier
                    .fillMaxSize()
                    .padding(horizontal = 40.dp, vertical = 28.dp),
                horizontalArrangement = Arrangement.spacedBy(44.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(
                    Modifier.fillMaxHeight().width(artSize),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Box(Modifier.weight(1f), contentAlignment = Alignment.Center) { artwork() }
                    bottomRow(Modifier.fillMaxWidth())
                }
                // Centred, not left-aligned: on a wide window the column keeps
                // its own 620 dp and the rest of the cell used to stay empty, so
                // the player read as a layout with a hole on the right.
                V17ColumnSwap(
                    target = when {
                        showLyricsPanel -> 1
                        showQueuePanel -> 2
                        else -> 0
                    },
                    modifier = Modifier.weight(1f),
                ) { face ->
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        when (face) {
                            1 -> lyricsPanel()
                            2 -> queuePanel()
                            else -> info()
                        }
                    }
                }
            }
        } else {
            // Stacked: one column, centred when the window is tall enough to hold
            // it and scrollable when it is not. Sitting at the top of a tall
            // window left the whole lower half empty, which read as a layout
            // that had failed to fill the screen.
            Box(
                Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState()),
            ) {
                Column(
                    Modifier
                        .fillMaxWidth()
                        .heightIn(min = windowMaxHeight)
                        .padding(start = 24.dp, end = 24.dp, top = 20.dp, bottom = 88.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center,
                ) {
                    artwork()
                    Spacer(Modifier.height(18.dp))
                    V17ColumnSwap(
                        target = if (showLyricsPanel) 1 else 0,
                        modifier = Modifier.fillMaxWidth(),
                    ) { face ->
                        if (face == 1) {
                            Box(Modifier.fillMaxWidth().height(320.dp)) { lyricsPanel() }
                        } else {
                            info()
                        }
                    }
                    Spacer(Modifier.height(10.dp))
                    bottomRow(Modifier.fillMaxWidth())
                }
            }
        }


        if (onBack != null) {
            Tooltip(Localization.get(language, "back")) {
                Surface(
                    onClick = onBack,
                    shape = CircleShape,
                    color = MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = 0.72f),
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .padding(18.dp)
                        .size(38.dp),
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            Icons.Filled.KeyboardArrowDown,
                            contentDescription = Localization.get(language, "back"),
                            tint = contentColor,
                            modifier = Modifier.size(24.dp),
                        )
                    }
                }
            }
        }
    }
}

/**
 * BitChord's own queue: the track that is playing, then everything that comes
 * next, with the equalizer mark on the current one and the remove control on
 * every entry. It replaces the player's right column instead of pushing a
 * second screen, which is how the reference shows it.
 */
@Composable
private fun AppleV17QueuePanel(
    queue: List<NowPlaying>,
    index: Int,
    language: String,
    onClose: () -> Unit,
    onSkipTo: (Int) -> Unit,
    onRemoveAt: (Int) -> Unit,
    onAddToPlaylist: ((NowPlaying) -> Unit)? = null,
    onReorderQueue: (List<NowPlaying>) -> Unit = {},
    modifier: Modifier = Modifier,
) {
    val contentColor = MaterialTheme.colorScheme.onSurface
    val mutedColor = MaterialTheme.colorScheme.onSurfaceVariant
    val current = queue.getOrNull(index)

    Column(modifier.verticalScroll(rememberScrollState())) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Text(
                Localization.get(language, "queue"),
                style = MaterialTheme.typography.titleLarge,
                color = contentColor,
                modifier = Modifier.weight(1f),
            )
            Tooltip(Localization.get(language, "close")) {
                M3IconButton(onClick = onClose) {
                    Icon(
                        Icons.Filled.Close,
                        contentDescription = Localization.get(language, "close"),
                        tint = mutedColor,
                        modifier = Modifier.size(20.dp),
                    )
                }
            }
        }
        Spacer(Modifier.height(8.dp))
        Text(
            Localization.get(language, "now_playing"),
            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
            color = mutedColor,
        )
        if (current != null) {
            QueueEntry(
                song = current,
                language = language,
                isCurrent = true,
                onOpen = {},
                onRemove = { onRemoveAt(index) },
                onAddToPlaylist = onAddToPlaylist?.let { add -> { add(current) } },
                onPlayNext = null,
                onAddToQueue = { onReorderQueue(queue + current) },
            )
        }
        Spacer(Modifier.height(10.dp))
        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
        Spacer(Modifier.height(10.dp))
        Text(
            Localization.get(language, "up_next"),
            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
            color = mutedColor,
        )
        Spacer(Modifier.height(4.dp))
        val next = queue.drop(index + 1)
        next.forEachIndexed { offset, song ->
            QueueEntry(
                song = song,
                language = language,
                isCurrent = false,
                onOpen = { onSkipTo(index + 1 + offset) },
                onRemove = { onRemoveAt(index + 1 + offset) },
                onAddToPlaylist = onAddToPlaylist?.let { add -> { add(song) } },
                onPlayNext = {
                    val list = queue.toMutableList()
                    if (index + 1 + offset in list.indices) {
                        val moved = list.removeAt(index + 1 + offset)
                        list.add((index + 1).coerceIn(0, list.size), moved)
                        onReorderQueue(list)
                    }
                },
                onAddToQueue = { onReorderQueue(queue + song) },
            )
        }
    }
}

@Composable
private fun QueueEntry(
    song: NowPlaying,
    language: String,
    isCurrent: Boolean,
    onOpen: () -> Unit,
    onRemove: () -> Unit,
    onAddToPlaylist: (() -> Unit)? = null,
    onPlayNext: (() -> Unit)? = null,
    onAddToQueue: (() -> Unit)? = null,
) {
    val contentColor = MaterialTheme.colorScheme.onSurface
    val mutedColor = MaterialTheme.colorScheme.onSurfaceVariant
    var menuOpen by remember { mutableStateOf(false) }
    var liked by remember(song.videoId) { mutableStateOf(SongActions.isLiked(song.videoId)) }
    val scope = rememberCoroutineScope()
    Row(
        Modifier
            .fillMaxWidth()
            .clip(MaterialTheme.shapes.small)
            .clickable(onClick = onOpen)
            .padding(vertical = 6.dp, horizontal = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            Modifier
                .size(38.dp)
                .clip(MaterialTheme.shapes.extraSmall)
                .background(MaterialTheme.colorScheme.surfaceContainerHigh),
        ) {
            AsyncImage(
                model = song.thumbnail,
                contentDescription = song.title,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize(),
            )
        }
        Spacer(Modifier.width(10.dp))
        Column(Modifier.weight(1f)) {
            Tooltip(song.title) {
                Text(
                    song.title,
                    style = MaterialTheme.typography.bodyMedium.copy(
                        fontWeight = if (isCurrent) FontWeight.Bold else FontWeight.Normal,
                    ),
                    color = contentColor,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            Tooltip(song.artist) {
                Text(
                    song.artist,
                    style = MaterialTheme.typography.bodySmall,
                    color = mutedColor,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
        Spacer(Modifier.width(8.dp))
        if (isCurrent) {
            Icon(
                Icons.Filled.GraphicEq,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(18.dp),
            )
            Spacer(Modifier.width(6.dp))
        }
        // BitChord's context menu on a queue row: the actions that make sense
        // on a track that is already in the queue, each one running the same
        // call the rest of the app uses for it.
        // The same context menu the standalone queue shows, on the same rows:
        // every entry runs the same call, so the two surfaces cannot drift.
        Box {
            Tooltip(Localization.get(language, "more")) {
                M3IconButton(onClick = { menuOpen = true }) {
                    Icon(
                        Icons.Filled.MoreVert,
                        contentDescription = Localization.get(language, "more"),
                        tint = mutedColor,
                        modifier = Modifier.size(16.dp),
                    )
                }
            }
            DropdownMenu(expanded = menuOpen, onDismissRequest = { menuOpen = false }) {
                DropdownMenuItem(
                    text = { Text(Localization.get(language, if (liked) "unlike" else "like")) },
                    leadingIcon = {
                        Icon(
                            if (liked) Icons.Filled.Favorite else Icons.Filled.FavoriteBorder,
                            contentDescription = null,
                        )
                    },
                    onClick = {
                        menuOpen = false
                        val next = !liked
                        liked = next
                        SongActions.setLiked(
                            song.videoId,
                            next,
                            title = song.title,
                            artist = song.artist,
                            thumbnail = song.thumbnail,
                        )
                        scope.launch { YouTube.likeVideo(song.videoId, next) }
                    },
                )
                if (onAddToPlaylist != null) {
                    DropdownMenuItem(
                        text = { Text(Localization.get(language, "add_to_playlist")) },
                        leadingIcon = { Icon(Icons.Filled.PlaylistAdd, contentDescription = null) },
                        onClick = {
                            menuOpen = false
                            onAddToPlaylist()
                        },
                    )
                }
                if (onPlayNext != null) {
                    DropdownMenuItem(
                        text = { Text("Play next") },
                        leadingIcon = { Icon(Icons.Filled.QueuePlayNext, contentDescription = null) },
                        onClick = {
                            menuOpen = false
                            onPlayNext()
                        },
                    )
                }
                if (onAddToQueue != null) {
                    DropdownMenuItem(
                        text = { Text("Add to queue") },
                        leadingIcon = {
                            Icon(Icons.AutoMirrored.Filled.QueueMusic, contentDescription = null)
                        },
                        onClick = {
                            menuOpen = false
                            onAddToQueue()
                        },
                    )
                }
                DropdownMenuItem(
                    text = { Text("Remove from queue") },
                    leadingIcon = { Icon(Icons.Filled.Close, contentDescription = null) },
                    onClick = {
                        menuOpen = false
                        onRemove()
                    },
                )
            }
        }
        Tooltip(Localization.get(language, "close")) {
            M3IconButton(onClick = onRemove) {
                Icon(
                    Icons.Filled.Close,
                    contentDescription = Localization.get(language, "close"),
                    tint = mutedColor,
                    modifier = Modifier.size(16.dp),
                )
            }
        }
    }
}

/**
 * The "Stats for nerds" line BitChord prints on the artwork, built from what the
 * player really reported (`PlaybackTech`): nothing is estimated, and a field the
 * stream or the sound card has not reported yet is left out of the line.
 */
@Composable
private fun StatsLine(
    tech: PlaybackTech.Snapshot,
    language: String,
    modifier: Modifier = Modifier,
) {
    val parts = buildList {
        tech.codec?.let { add(it) }
        tech.sampleRateHz?.let { add(String.format(Locale.US, "%.1f kHz", it / 1000f)) }
        tech.bitrateKbps?.let { add("$it kbps") }
        tech.channels?.let { channels ->
            add(
                when (channels) {
                    1 -> "Mono"
                    2 -> "Stereo"
                    else -> "$channels ch"
                },
            )
        }
    }
    val line = if (parts.isEmpty()) "n/a" else parts.joinToString(" - ")
    val detail = buildString {
        appendLine(Localization.get(language, "stats_for_nerds"))
        appendLine("${Localization.get(language, "stats_codec")}: ${tech.codec ?: "n/a"}")
        appendLine("${Localization.get(language, "stats_bitrate")}: ${tech.bitrateKbps?.let { "$it kbps" } ?: "n/a"}")
        append(
            "${Localization.get(language, "stats_output")}: " +
                if (tech.sampleRateHz != null && tech.bitDepth != null && tech.channels != null) {
                    "${tech.sampleRateHz} Hz / ${tech.bitDepth}-bit / ${tech.channels} ch"
                } else {
                    "n/a"
                },
        )
        appendLine()
        appendLine(
            "${Localization.get(language, "stats_device_buffer")}: " +
                (tech.deviceBufferMs?.let { "$it ms" } ?: "n/a"),
        )
        append(
            "${Localization.get(language, "stats_pcm_queue")}: " +
                (tech.pcmQueueMs?.let { "$it ms" } ?: "n/a"),
        )
    }
    Tooltip(detail) {
        Text(
            line,
            style = MaterialTheme.typography.labelSmall.copy(fontFamily = FontFamily.Monospace),
            color = Color.White.copy(alpha = 0.92f),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = modifier
                .clip(RoundedCornerShape(4.dp))
                .background(Color.Black.copy(alpha = 0.42f))
                .padding(horizontal = 6.dp, vertical = 3.dp),
        )
    }
}

/**
 * The line being sung, read from the same lyrics the lyrics page shows.
 *
 * Null when the option is off, while nothing is loaded yet, on an
 * unsynchronised text (there is no "current" line without timestamps) and when
 * the position has not reached the first line: the caller then shows the page
 * link instead of an invented line.
 */
@Composable
private fun rememberInlineLyricLine(np: NowPlaying, positionMs: Long): String? {
    val enabled = remember(settingsFileRevision()) { DesktopSettings.load().inlineLyrics }
    if (!enabled) return null
    var text by remember(np.videoId) {
        mutableStateOf(LyricsCache.get(np.videoId, preferSynced = true))
    }
    LaunchedEffect(np.videoId) {
        if (text != null) return@LaunchedEffect
        // Same provider chain as the lyrics page, and only the cached text is
        // reused: this row must never cost a second lookup of its own.
        DesktopLyrics.fetch(
            videoId = np.videoId,
            title = np.title,
            artist = np.artist,
            durationMs = np.durationMs,
            album = np.album,
            preferSynced = true,
        ).onSuccess {
            text = it
            if (np.durationMs > 0) LyricsCache.put(np.videoId, it, preferSynced = true)
        }
    }
    val lines = remember(text) {
        val raw = text
        if (raw == null || !LyricsParser.hasTimestamps(raw)) {
            emptyList()
        } else {
            LyricsParser.parse(raw)
        }
    }
    return remember(lines, positionMs) {
        if (lines.isEmpty() || positionMs < lines.first().timeMs) {
            null
        } else {
            lines.getOrNull(LyricsParser.currentLineIndex(lines, positionMs))
                ?.text
                ?.takeIf { it.isNotBlank() }
        }
    }
}

/**
 * The v17 action row, the way the reference draws it.
 *
 * One row under the artwork: the lyrics page on the left, the queue on the
 * right, and one capsule between them that carries two sets of controls. With
 * the queue closed it is where the sound goes and who is in the session
 * (Output, Listening Together); with the queue open it is how the queue plays
 * (Shuffle, Repeat, Autoplay). The two glyphs on the ends are lit while the
 * page they open is the one on screen, and the queue glyph closes its own
 * panel instead of leading to a dead end.
 *
 * The glyphs are [BitChordIcons], the reference's own: same paths, same
 * 2.2 px strokes, same round caps. The fills are the reference's too - a 44 dp
 * circle and a capsule as tall as it, at 12 to 20 percent of the content
 * colour, with the icon at full strength when its control is on.
 */

@Composable
private fun V17BottomBar(
    language: String,
    isShuffle: Boolean,
    repeatMode: RepeatMode,
    autoPlayNext: Boolean,
    videoId: String?,
    lyricsOpen: Boolean,
    queueOpen: Boolean,
    onToggleLyrics: () -> Unit,
    onToggleShuffle: () -> Unit,
    onCycleRepeat: () -> Unit,
    onToggleAutoPlayNext: (Boolean) -> Unit,
    onOpenQueue: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val contentColor = MaterialTheme.colorScheme.onSurface
    val mutedColor = MaterialTheme.colorScheme.onSurfaceVariant
    val capsuleColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.12f)
    val highlightColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.14f)
    val glyphHighlightColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.20f)
    val dividerColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.20f)
    val clipboard = LocalClipboardManager.current
    var outputOpen by remember { mutableStateOf(false) }
    var togetherOpen by remember { mutableStateOf(false) }
    var selected by remember { mutableStateOf(AudioOutput.selectedName) }
    val devices = remember { AudioOutput.devices() }

    @Composable
    fun bottomGlyph(
        icon: androidx.compose.ui.graphics.vector.ImageVector,
        label: String,
        highlighted: Boolean,
        onClick: () -> Unit,
    ) {
        Tooltip(label) {
            Box(
                Modifier
                    .size(V17_ACTION_SIZE)
                    .clip(CircleShape)
                    .background(if (highlighted) glyphHighlightColor else Color.Transparent)
                    .clickable(onClick = onClick),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    icon,
                    contentDescription = label,
                    tint = if (highlighted) contentColor else mutedColor,
                    modifier = Modifier.size(26.dp),
                )
            }
        }
    }

    @Composable
    fun pillSegment(
        label: String,
        highlighted: Boolean,
        width: androidx.compose.ui.unit.Dp,
        icon: androidx.compose.ui.graphics.vector.ImageVector? = null,
        iconSize: androidx.compose.ui.unit.Dp = V17_PILL_ICON,
        text: String? = null,
        onClick: () -> Unit,
    ) {
        Tooltip(label) {
            Box(
                Modifier
                    .width(width)
                    .height(V17_ACTION_SIZE)
                    .background(if (highlighted) highlightColor else Color.Transparent)
                    .clickable(onClick = onClick),
                contentAlignment = Alignment.Center,
            ) {
                if (icon != null) {
                    Icon(
                        icon,
                        contentDescription = label,
                        tint = if (highlighted) contentColor else mutedColor,
                        modifier = Modifier.size(iconSize),
                    )
                } else if (text != null) {
                    Text(
                        text,
                        color = if (highlighted) contentColor else mutedColor,
                        style = MaterialTheme.typography.titleMedium,
                    )
                }
            }
        }
    }

    @Composable
    fun pill(content: @Composable androidx.compose.foundation.layout.RowScope.() -> Unit) {
        Row(
            modifier = Modifier
                .height(V17_ACTION_SIZE)
                .clip(CircleShape)
                .background(capsuleColor),
            verticalAlignment = Alignment.CenterVertically,
            content = content,
        )
    }

    @Composable
    fun pillDivider() {
        Box(Modifier.width(1.dp).fillMaxHeight().background(dividerColor))
    }

    Row(
        modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        // The lyrics page, lit while it is the column being shown.
        bottomGlyph(
            BitChordIcons.LyricsQuote,
            Localization.get(language, "lyrics"),
            lyricsOpen,
            onToggleLyrics,
        )
        // One capsule, two sets: where the sound goes and who is in it while the
        // queue is closed, how the queue plays while it is open. The reference
        // swaps the two in place instead of adding a second row.
        Crossfade(targetState = queueOpen, animationSpec = tween(180), label = "v17BottomCapsule") { showQueueModes ->
            if (showQueueModes) {
                pill {
                    pillSegment(
                        Localization.get(language, "shuffle"),
                        isShuffle,
                        V17_PILL_TRIPLE,
                        icon = BitChordIcons.Shuffle,
                        onClick = onToggleShuffle,
                    )
                    pillDivider()
                    pillSegment(
                        when (repeatMode) {
                            RepeatMode.OFF -> "Repeat off"
                            RepeatMode.ALL -> "Repeat all"
                            RepeatMode.ONE -> "Repeat one"
                        },
                        repeatMode != RepeatMode.OFF,
                        V17_PILL_TRIPLE,
                        icon = if (repeatMode == RepeatMode.ONE) null else BitChordIcons.Repeat,
                        text = if (repeatMode == RepeatMode.ONE) "1" else null,
                        onClick = onCycleRepeat,
                    )
                    pillDivider()
                    pillSegment(
                        Localization.get(language, "tooltip_autoplay"),
                        autoPlayNext,
                        V17_PILL_TRIPLE,
                        icon = BitChordIcons.Infinity,
                        onClick = { onToggleAutoPlayNext(!autoPlayNext) },
                    )
                }
            } else {
                Box {
                    pill {
                        pillSegment(
                            Localization.get(language, "audio_output"),
                            selected.isNotBlank(),
                            V17_PILL_SEGMENT,
                            icon = Icons.Filled.Headphones,
                            iconSize = V17_PILL_HEADPHONES,
                            onClick = { outputOpen = true },
                        )
                        pillDivider()
                        pillSegment(
                            "Listening Together",
                            false,
                            V17_PILL_SEGMENT,
                            icon = Icons.Filled.Person,
                            iconSize = V17_PILL_PARTY,
                            onClick = { togetherOpen = true },
                        )
                    }
                    DropdownMenu(expanded = outputOpen, onDismissRequest = { outputOpen = false }) {
                        DropdownMenuItem(
                            text = { Text("System default") },
                            onClick = {
                                AudioOutput.apply("")
                                selected = ""
                                outputOpen = false
                            },
                        )
                        devices.forEach { device ->
                            DropdownMenuItem(
                                text = { Text(device.label) },
                                onClick = {
                                    AudioOutput.apply(device.name)
                                    selected = device.name
                                    outputOpen = false
                                },
                            )
                        }
                    }
                    // No session service exists in the desktop build: the nearest
                    // real action is sharing the track, so the menu copies its
                    // link and says the session itself is missing.
                    DropdownMenu(expanded = togetherOpen, onDismissRequest = { togetherOpen = false }) {
                        DropdownMenuItem(
                            text = { Text("Copy track link") },
                            enabled = !videoId.isNullOrBlank(),
                            onClick = {
                                togetherOpen = false
                                videoId?.let {
                                    clipboard.setText(AnnotatedString("https://music.youtube.com/watch?v=$it"))
                                }
                            },
                        )
                        DropdownMenuItem(
                            text = { Text("Listening Together needs a session") },
                            enabled = false,
                            onClick = {},
                        )
                    }
                }
            }
        }
        // The queue, lit while its panel is up: the same control opens and closes
        // it, the way the reference's does.
        bottomGlyph(
            BitChordIcons.Queue,
            Localization.get(language, "queue"),
            queueOpen,
            onOpenQueue,
        )
    }
}
/**
 * The three-dot menu's entries, as a block that the real menu mounts inside its
 * popup and the render check draws inline.
 *
 * The harness that paints the player off-screen cannot paint a `DropdownMenu`
 * (an `ImageComposeScene` draws no popups), so the entries live here once: the
 * menu and the picture of the menu are then the same code and cannot drift.
 */
/**
 * The player column changing face: the controls, the inline lyrics or the queue.
 *
 * The swap used to be a hard cut, so the large lyrics arrived as a replaced
 * frame instead of as a surface. It is a cross-fade on the project's own motion
 * tokens - one SHORT4 step on the decelerating curve - and it goes instant when
 * the Animations switch is off, like every other transition in the app.
 */
@Composable
private fun V17ColumnSwap(
    target: Int,
    modifier: Modifier = Modifier,
    content: @Composable (Int) -> Unit,
) {
    Crossfade(
        targetState = target,
        animationSpec = tween(
            if (Animations.enabled) Animations.ms(Motion.SHORT4) else 0,
            easing = Motion.standardDecelerate,
        ),
        label = "v17ColumnSwap",
        modifier = modifier,
    ) { face -> content(face) }
}

/**
 * The v17 song menu: the reference's eleven entries, in its own order.
 *
 * Every entry runs the same call the rest of the app uses for that action
 * (like/dislike, the playlist picker, the queue calls, the cache, the sleep
 * timer, the share link), so the player cannot drift from the song menus.
 */
@Composable
internal fun androidx.compose.foundation.layout.ColumnScope.V17PlayerMenuContent(
    language: String,
    liked: Boolean,
    downloaded: Boolean,
    sleepTimerMinutes: Int?,
    onLike: () -> Unit,
    onDislike: () -> Unit,
    onAddToPlaylist: () -> Unit,
    onDownload: () -> Unit,
    onDeleteDownload: () -> Unit,
    onStartRadio: () -> Unit,
    onPlayNext: () -> Unit,
    onAddToQueue: () -> Unit,
    onOpenAlbum: () -> Unit,
    onOpenArtist: () -> Unit,
    onSleepTimer: (Int?) -> Unit,
    onShare: () -> Unit,
) {
    DropdownMenuItem(
        text = { Text(Localization.get(language, if (liked) "unlike" else "like")) },
        leadingIcon = {
            Icon(
                if (liked) Icons.Filled.Favorite else Icons.Filled.FavoriteBorder,
                contentDescription = null,
                tint = if (liked) MaterialTheme.colorScheme.error else Color.Unspecified,
            )
        },
        onClick = onLike,
    )
    DropdownMenuItem(
        text = { Text(Localization.get(language, "dislike")) },
        leadingIcon = {
            Icon(Icons.Filled.ThumbDown, contentDescription = null)
        },
        onClick = onDislike,
    )
    DropdownMenuItem(
        text = { Text(Localization.get(language, "add_to_playlist")) },
        leadingIcon = {
            Icon(Icons.Filled.PlaylistAdd, contentDescription = null)
        },
        onClick = onAddToPlaylist,
    )
    if (downloaded) {
        DropdownMenuItem(
            text = { Text(Localization.get(language, "downloaded")) },
            leadingIcon = {
                Icon(Icons.Filled.CheckCircle, contentDescription = null)
            },
            onClick = onDeleteDownload,
            trailingIcon = {
                Text(
                    Localization.get(language, "delete"),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.error,
                )
            },
        )
    } else {
        DropdownMenuItem(
            text = { Text(Localization.get(language, "download")) },
            leadingIcon = {
                Icon(Icons.Filled.Download, contentDescription = null)
            },
            onClick = onDownload,
        )
    }
    DropdownMenuItem(
        text = { Text(Localization.get(language, "start_radio")) },
        leadingIcon = {
            Icon(Icons.Filled.Radio, contentDescription = null)
        },
        onClick = onStartRadio,
    )
    DropdownMenuItem(
        text = { Text(Localization.get(language, "play_next")) },
        leadingIcon = {
            Icon(Icons.AutoMirrored.Filled.PlaylistPlay, contentDescription = null)
        },
        onClick = onPlayNext,
    )
    DropdownMenuItem(
        text = { Text(Localization.get(language, "add_to_queue")) },
        leadingIcon = {
            Icon(Icons.AutoMirrored.Filled.QueueMusic, contentDescription = null)
        },
        onClick = onAddToQueue,
    )
    DropdownMenuItem(
        text = { Text(Localization.get(language, "album")) },
        leadingIcon = {
            Icon(Icons.Filled.Album, contentDescription = null)
        },
        onClick = onOpenAlbum,
    )
    DropdownMenuItem(
        text = { Text(Localization.get(language, "artist")) },
        leadingIcon = {
            Icon(Icons.Filled.Person, contentDescription = null)
        },
        onClick = onOpenArtist,
    )
    // One entry, cycling: 15, 30, 60 minutes, then off again. The running timer
    // says its own length, so the menu is also where it is read.
    DropdownMenuItem(
        text = {
            val current = sleepTimerMinutes
            Text(
                if (current == null) Localization.get(language, "sleep_timer")
                else "${Localization.get(language, "sleep_timer")} - $current min"
            )
        },
        leadingIcon = {
            Icon(
                Icons.Filled.Bedtime,
                contentDescription = null,
                tint = if (sleepTimerMinutes != null) MaterialTheme.colorScheme.primary else Color.Unspecified,
            )
        },
        onClick = {
            onSleepTimer(
                when (sleepTimerMinutes) {
                    null -> 15
                    15 -> 30
                    30 -> 60
                    else -> null
                }
            )
        },
    )
    DropdownMenuItem(
        text = { Text(Localization.get(language, "share")) },
        leadingIcon = {
            Icon(Icons.Filled.Share, contentDescription = null)
        },
        onClick = onShare,
    )
}

/** The reference's action row: a 44 dp round control and a capsule as tall. */
private val V17_ACTION_SIZE = 44.dp
private val V17_PILL_SEGMENT = 64.dp
private val V17_PILL_TRIPLE = 52.dp
private val V17_PILL_ICON = 24.dp
private val V17_PILL_HEADPHONES = 23.dp
private val V17_PILL_PARTY = 22.dp
