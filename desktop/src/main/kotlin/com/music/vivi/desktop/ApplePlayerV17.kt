package com.music.vivi.desktop

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
import androidx.compose.material.icons.filled.Close
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
        // The cover is asked for at a size a large window can actually show:
        // the stored original when the provider has one (see
        // `adjustedThumbnailUrl`), so the artwork is not a 120 px variant
        // stretched over half the screen.
        val artworkUrl = adjustedThumbnailUrl(
            np.thumbnail,
            requestedPx = 1200,
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
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        Localization.get(language, "now_playing"),
                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
                        color = mutedColor,
                    )
                    np.album?.takeIf { it.isNotBlank() }?.let { album ->
                        Text(
                            " - $album",
                            style = MaterialTheme.typography.labelMedium,
                            color = mutedColor,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f, fill = false),
                        )
                    }
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
                                onAddToPlaylist = {
                                    menuOpen = false
                                    onAddToPlaylist()
                                },
                                onOpenLyrics = {
                                    menuOpen = false
                                    onOpenLyrics()
                                },
                                onOpenQueue = {
                                    menuOpen = false
                                    onOpenQueue()
                                },
                            )
                        }
                    }
                }

                Spacer(Modifier.height(14.dp))

                // BitChord prints the line being sung here, right above the
                // scrubber, and opens the lyrics page from it. The line is read
                // from the same cache and providers the lyrics screen uses, and
                // only while the option is on: with it off the row is the page
                // link it used to be and no fetch is made.
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
                    }
                }

                Spacer(Modifier.height(8.dp))

                ViviSlider(
                    value = positionMs.toFloat().coerceIn(0f, duration.toFloat()),
                    onValueChange = { onSeek(it.toLong()) },
                    valueRange = 0f..duration.toFloat(),
                    style = sliderStyle,
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
                    Tooltip(Localization.get(language, "shuffle")) {
                        M3IconButton(onClick = onToggleShuffle) {
                            Icon(
                                Icons.Filled.Shuffle,
                                contentDescription = Localization.get(language, "shuffle"),
                                tint = if (isShuffle) primaryColor else mutedColor,
                                modifier = Modifier.size(20.dp),
                            )
                        }
                    }
                    Tooltip(Localization.get(language, "previous")) {
                        M3IconButton(onClick = onPrevious) {
                            Icon(
                                Icons.Filled.FastRewind,
                                contentDescription = Localization.get(language, "previous"),
                                tint = contentColor,
                                modifier = Modifier.size(32.dp),
                            )
                        }
                    }
                    Tooltip(Localization.get(language, if (isPlaying) "pause" else "play")) {
                        M3IconButton(onClick = onTogglePlay) {
                            Icon(
                                if (isPlaying) Icons.Filled.Pause else Icons.Filled.PlayArrow,
                                contentDescription = Localization.get(language, if (isPlaying) "pause" else "play"),
                                tint = contentColor,
                                modifier = Modifier.size(46.dp),
                            )
                        }
                    }
                    Tooltip(Localization.get(language, "next")) {
                        M3IconButton(onClick = onNext) {
                            Icon(
                                Icons.Filled.FastForward,
                                contentDescription = Localization.get(language, "next"),
                                tint = contentColor,
                                modifier = Modifier.size(32.dp),
                            )
                        }
                    }
                    Tooltip(Localization.get(language, "repeat")) {
                        M3IconButton(onClick = onCycleRepeat) {
                            Icon(
                                if (repeatMode == RepeatMode.ONE) Icons.Filled.RepeatOne else Icons.Filled.Repeat,
                                contentDescription = Localization.get(language, "repeat"),
                                tint = if (repeatMode == RepeatMode.OFF) mutedColor else primaryColor,
                                modifier = Modifier.size(20.dp),
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
                        modifier = Modifier.size(18.dp),
                    )
                    Spacer(Modifier.width(8.dp))
                    ViviSlider(
                        value = volume.coerceIn(0f, 1f),
                        onValueChange = onVolume,
                        valueRange = 0f..1f,
                        style = sliderStyle,
                        modifier = Modifier.weight(1f),
                    )
                    Spacer(Modifier.width(8.dp))
                    Icon(
                        Icons.Filled.VolumeUp,
                        contentDescription = Localization.get(language, "volume"),
                        tint = mutedColor,
                        modifier = Modifier.size(18.dp),
                    )
                }

                Spacer(Modifier.height(4.dp))

                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    TextButton(onClick = onOpenLyrics) {
                        Icon(Icons.AutoMirrored.Filled.Subject, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(6.dp))
                        Text(Localization.get(language, "lyrics"))
                    }
                    TextButton(onClick = { if (wide) showQueuePanel = true else onOpenQueue() }) {
                        Icon(Icons.AutoMirrored.Filled.QueueMusic, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(6.dp))
                        Text(Localization.get(language, "queue"))
                    }
                    TextButton(onClick = onAddToPlaylist) {
                        Icon(Icons.Filled.PlaylistAdd, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(6.dp))
                        Text(Localization.get(language, "add_to_playlist"))
                    }
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

        val lyricsPanel: @Composable () -> Unit = {
            // The player's own lyrics view: headerless, no back control, and the
            // same loader/cache the lyrics screen uses, so nothing is fetched a
            // second time. It fills the column the controls occupy.
            LyricsScreen(
                nowPlaying = np,
                positionMs = positionMs,
                isPlaying = isPlaying,
                language = language,
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

        if (wide) {
            Row(
                Modifier
                    .fillMaxSize()
                    .padding(horizontal = 40.dp, vertical = 28.dp),
                horizontalArrangement = Arrangement.spacedBy(44.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                artwork()
                // Centred, not left-aligned: on a wide window the column keeps
                // its own 620 dp and the rest of the cell used to stay empty, so
                // the player read as a layout with a hole on the right.
                Box(Modifier.weight(1f), contentAlignment = Alignment.Center) {
                    when {
                        showLyricsPanel -> lyricsPanel()
                        showQueuePanel -> queuePanel()
                        else -> info()
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
                    if (showLyricsPanel) {
                        Box(Modifier.fillMaxWidth().height(320.dp)) { lyricsPanel() }
                    } else {
                        info()
                    }
                }
            }
        }

        // BitChord's bottom row: the lyrics page, the audio output the sound
        // card is writing to, and the queue. It is the row of three controls
        // that sat under the artwork in the reference.
        V17BottomBar(
            language = language,
            isShuffle = isShuffle,
            repeatMode = repeatMode,
            autoPlayNext = autoPlayNext,
            videoId = np.videoId,
            lyricsOpen = showLyricsPanel,
            initialControlsRow = initialControlsRow,
            onToggleLyrics = { showLyricsPanel = !showLyricsPanel },
            onToggleShuffle = onToggleShuffle,
            onCycleRepeat = onCycleRepeat,
            onToggleAutoPlayNext = onToggleAutoPlayNext,
            onOpenLyrics = onOpenLyrics,
            onOpenQueue = {
                when {
                    showQueuePanel -> showQueuePanel = false
                    wide -> showQueuePanel = true
                    else -> onOpenQueue()
                }
            },
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 10.dp),
        )

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
 * BitChord's bottom controls: the lyrics page, the audio output device (the
 * caption under it is the one the sound card is writing to) and the queue.
 * They act on the same controller as every other control of the player.
 */
/**
 * The v17 bottom row, the way the reference draws it.
 *
 * Two states: the page row (Lyrics, Audio Output, Listening Together, Queue) and
 * the playback row (Lyrics, Shuffle, Cycle, Autoplay, Queue), reached from Queue
 * or from Lyrics. Every control draws the state it is in, the Queue control
 * closes its own panel instead of leading to a dead end, and the Lyrics control
 * is the one that opens the large lyrics page in place of the controls and comes
 * back from it.
 *
 * The glyphs are [AppleSymbols]: this is an Apple surface, and the edition keeps
 * Apple's own icon for the action there (see `AGENTS.md`).
 */
@Composable
private fun V17BottomBar(
    language: String,
    isShuffle: Boolean,
    repeatMode: RepeatMode,
    autoPlayNext: Boolean,
    videoId: String?,
    lyricsOpen: Boolean,
    initialControlsRow: Boolean,
    onToggleLyrics: () -> Unit,
    onToggleShuffle: () -> Unit,
    onCycleRepeat: () -> Unit,
    onToggleAutoPlayNext: (Boolean) -> Unit,
    onOpenLyrics: () -> Unit,
    onOpenQueue: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val mutedColor = MaterialTheme.colorScheme.onSurfaceVariant
    val activeColor = MaterialTheme.colorScheme.primary
    val pillColor = MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = 0.72f)
    val clipboard = LocalClipboardManager.current
    var controlsRow by remember(lyricsOpen) { mutableStateOf(initialControlsRow || lyricsOpen) }
    var outputOpen by remember { mutableStateOf(false) }
    var togetherOpen by remember { mutableStateOf(false) }
    var selected by remember { mutableStateOf(AudioOutput.selectedName) }
    val devices = remember { AudioOutput.devices() }
    val selectedLabel = devices.firstOrNull { it.name == selected }?.label
        ?: if (selected.isBlank()) "System default" else selected

    @Composable
    fun pill(icon: androidx.compose.ui.graphics.vector.ImageVector, label: String, tint: Color, onClick: () -> Unit) {
        Tooltip(label) {
            Surface(
                onClick = onClick,
                shape = CircleShape,
                color = pillColor,
                modifier = Modifier.size(40.dp),
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(icon, contentDescription = label, tint = tint, modifier = Modifier.size(21.dp))
                }
            }
        }
    }

    Column(modifier, horizontalAlignment = Alignment.CenterHorizontally) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(18.dp),
        ) {
            if (!controlsRow) {
                pill(
                    AppleSymbols.Lyrics,
                    Localization.get(language, "lyrics"),
                    if (lyricsOpen) activeColor else mutedColor,
                ) {
                    onToggleLyrics()
                    controlsRow = true
                }
                Box {
                    pill(AppleSymbols.AudioOutput, Localization.get(language, "audio_output"), mutedColor) { outputOpen = true }
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
                }
                Box {
                    // No session service exists in the desktop build: the nearest
                    // real action is sharing the track, so the menu copies its
                    // link and says the session itself is missing.
                    pill(AppleSymbols.ListeningTogether, "Listening Together", mutedColor) { togetherOpen = true }
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
                pill(AppleSymbols.Queue, Localization.get(language, "queue"), mutedColor) { controlsRow = true }
            } else {
                pill(
                    AppleSymbols.Lyrics,
                    Localization.get(language, "lyrics"),
                    if (lyricsOpen) activeColor else mutedColor,
                ) {
                    if (lyricsOpen) onToggleLyrics()
                    controlsRow = false
                }
                pill(
                    AppleSymbols.Shuffle,
                    Localization.get(language, "shuffle"),
                    if (isShuffle) activeColor else mutedColor,
                    onToggleShuffle,
                )
                pill(
                    if (repeatMode == RepeatMode.ONE) AppleSymbols.RepeatOne else AppleSymbols.Repeat,
                    Localization.get(language, "repeat"),
                    if (repeatMode == RepeatMode.OFF) mutedColor else activeColor,
                    onCycleRepeat,
                )
                pill(
                    AppleSymbols.Autoplay,
                    Localization.get(language, "tooltip_autoplay"),
                    if (autoPlayNext) activeColor else mutedColor,
                ) { onToggleAutoPlayNext(!autoPlayNext) }
                pill(AppleSymbols.Queue, Localization.get(language, "queue"), mutedColor, onOpenQueue)
            }
        }
        Spacer(Modifier.height(4.dp))
        Text(
            when {
                lyricsOpen -> Localization.get(language, "lyrics")
                controlsRow -> when (repeatMode) {
                    RepeatMode.OFF -> "Repeat off"
                    RepeatMode.ALL -> "Repeat all"
                    RepeatMode.ONE -> "Repeat one"
                }
                else -> selectedLabel
            },
            style = MaterialTheme.typography.labelSmall,
            color = mutedColor,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
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
@Composable
internal fun androidx.compose.foundation.layout.ColumnScope.V17PlayerMenuContent(
    language: String,
    liked: Boolean,
    onLike: () -> Unit,
    onAddToPlaylist: () -> Unit,
    onOpenLyrics: () -> Unit,
    onOpenQueue: () -> Unit,
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
        text = { Text(Localization.get(language, "add_to_playlist")) },
        leadingIcon = {
            Icon(Icons.Filled.PlaylistAdd, contentDescription = null)
        },
        onClick = onAddToPlaylist,
    )
    DropdownMenuItem(
        text = { Text(Localization.get(language, "lyrics")) },
        leadingIcon = {
            Icon(AppleSymbols.Lyrics, contentDescription = null)
        },
        onClick = onOpenLyrics,
    )
    DropdownMenuItem(
        text = { Text(Localization.get(language, "queue")) },
        leadingIcon = {
            Icon(AppleSymbols.Queue, contentDescription = null)
        },
        onClick = onOpenQueue,
    )
}
