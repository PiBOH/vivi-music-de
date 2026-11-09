package com.music.vivi.desktop

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.snap
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.animateScrollBy
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.snapping.rememberSnapFlingBehavior
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.hoverable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsHoveredAsState
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.automirrored.filled.PlaylistAdd
import androidx.compose.material3.Icon
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Button
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlin.math.roundToInt
import kotlin.math.sin
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.toComposeImageBitmap
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import com.music.innertube.models.AlbumItem
import com.music.innertube.models.ArtistItem
import com.music.innertube.models.PlaylistItem
import com.music.innertube.models.SongItem
import com.music.innertube.models.YTItem
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Renders a heavily blurred, opaque backdrop derived from an artwork URL,
 * cached per artwork URL to ensure zero performance hit on recomposition/window resizing.
 */
@Composable
fun CachedBlurBackdrop(
    artworkUrl: String?,
    modifier: Modifier = Modifier,
    blurRadiusPx: Float = 80f,
    scrimColor: Color = PaletteBlack.copy(alpha = 0.55f),
    fallbackColor: Color = MaterialTheme.colorScheme.surfaceContainer,
    content: @Composable BoxScope.() -> Unit
) {
    var cachedBitmap by remember { mutableStateOf<ImageBitmap?>(null) }
    var loadedUrl by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(artworkUrl) {
        if (artworkUrl.isNullOrBlank()) {
            cachedBitmap = null
            loadedUrl = null
            return@LaunchedEffect
        }
        if (loadedUrl == artworkUrl && cachedBitmap != null) return@LaunchedEffect
        withContext(Dispatchers.IO) {
            try {
                val url = java.net.URI(artworkUrl).toURL()
                val bytes = url.readBytes()
                val skiaImage = org.jetbrains.skia.Image.makeFromEncoded(bytes)
                val targetW = (skiaImage.width / 4).coerceAtLeast(64)
                val targetH = (skiaImage.height / 4).coerceAtLeast(64)
                val surface = org.jetbrains.skia.Surface.makeRasterN32Premul(targetW, targetH)
                val canvas = surface.canvas
                val blurFilter = org.jetbrains.skia.ImageFilter.makeBlur(
                    blurRadiusPx / 4f,
                    blurRadiusPx / 4f,
                    org.jetbrains.skia.FilterTileMode.CLAMP
                )
                val paint = org.jetbrains.skia.Paint().apply { imageFilter = blurFilter }
                canvas.drawImageRect(
                    skiaImage,
                    org.jetbrains.skia.Rect.makeWH(skiaImage.width.toFloat(), skiaImage.height.toFloat()),
                    org.jetbrains.skia.Rect.makeWH(targetW.toFloat(), targetH.toFloat()),
                    paint
                )
                val snapshot = surface.makeImageSnapshot()
                val composeBitmap = snapshot.toComposeImageBitmap()
                withContext(Dispatchers.Main) {
                    cachedBitmap = composeBitmap
                    loadedUrl = artworkUrl
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    cachedBitmap = null
                    loadedUrl = artworkUrl
                }
            }
        }
    }

    Box(modifier = modifier) {
        val bitmap = cachedBitmap
        if (bitmap != null) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                drawImage(
                    image = bitmap,
                    dstSize = IntSize(size.width.toInt(), size.height.toInt())
                )
                drawRect(color = scrimColor)
            }
        } else {
            Box(Modifier.fillMaxSize().background(fallbackColor)) {
                Box(Modifier.fillMaxSize().background(scrimColor))
            }
        }
        content()
    }
}

/**
 * Playback context for list rows: which track is currently loaded, whether it
 * is playing, and the live audio level (0..1) for the now-playing indicator.
 * Provided by the app root; rows read it via [LocalPlayback].
 */
data class PlaybackContext(
    val videoId: String? = null,
    val isPlaying: Boolean = false,
    val audioLevel: StateFlow<Float>? = null,
    /** Buffered fraction (0..1) of the current track (1f = fully cached / not
     *  streaming). Powers the YouTube-style secondary segment on seek bars. */
    val bufferedFraction: StateFlow<Float>? = null,
    /** Fraction (0..1) of the current track scrubbed while its duration was
     *  still unknown (loaded from the restored queue but never played); null
     *  when the duration is known or nothing was scrubbed. Keeps the seek bar
     *  thumb at the scrubbed point until playback actually begins. */
    val pendingSeekFraction: StateFlow<Float?>? = null,
)

/** Composition local exposing the current playback to list rows (SongRow etc.). */
val LocalPlayback = compositionLocalOf { PlaybackContext() }

/**
 * Current buffered fraction for the seek bars, collected from [LocalPlayback].
 * Returns 1f when the app root doesn't provide one (no stream loaded or the
 * composable is rendered outside the provider): a full buffer means the
 * secondary "buffered" segment stays hidden.
 */
@Composable
fun playbackBufferedFraction(): Float {
    val flow = LocalPlayback.current.bufferedFraction ?: return 1f
    return flow.collectAsState().value
}

/**
 * Current pending scrub fraction (0..1) for the seek bars, collected from
 * [LocalPlayback]. Returns null when the duration is known (real seek range)
 * or nothing was scrubbed yet.
 */
@Composable
fun playbackPendingSeekFraction(): Float? {
    return LocalPlayback.current.pendingSeekFraction?.collectAsState()?.value
}

// ---------------------------------------------------------------------------
// Artwork quality (port of the mobile `String.resize()`)
// ---------------------------------------------------------------------------

/** Google CDN size parameter, e.g. `=w120-h120` / `=s120`. */
private val googleCdnParam = Regex("=[wshd]\\d+")

/** Video id inside a YouTube thumbnail URL (`/vi/<id>/...` or `/vi_webp/<id>/...`). */
private val ytThumbVideoId = Regex("/vi(?:_webp)?/([^/]+)/")

/**
 * Rewrites a thumbnail URL to the resolution the app should actually load.
 *
 * The desktop used to feed the provider's own URL straight to the image loader,
 * which for YouTube Music is a small `w120-h120` crop: covers looked soft even
 * with *Data saver* OFF, while the mobile app always asks for a high-resolution
 * variant and only caps it when the saver is on. This is the mobile rule:
 *
 * - Google CDN (`googleusercontent.com` / `ggpht.com` / `=wNN`): `wN-hN-p-l90-rj`,
 *   at least 544 px per side, capped at 150 px with the saver on;
 * - YouTube thumbnails (`i.ytimg.com`): `maxresdefault` for big artwork,
 *   `hqdefault`/`mqdefault`/`default` for the smaller tiers — and the two low
 *   tiers when the saver is on;
 * - anything else is returned untouched.
 */
fun adjustedThumbnailUrl(url: String?, requestedPx: Int = 544, dataSaver: Boolean = false): String? {
    if (url.isNullOrBlank()) return url
    val isGoogleCdn = url.contains("googleusercontent.com") ||
        url.contains("ggpht.com") ||
        googleCdnParam.containsMatchIn(url)
    val isYtimg = url.contains("ytimg") || url.contains("youtube.com") || url.contains("/vi/")
    return when {
        isGoogleCdn -> {
            // BitChord's rule, at BitChord's sizes: the size hint the provider
            // put in the URL is rewritten in place at the rung the surface draws
            // at (see `ArtworkPx`), which keeps the crop and the palette around
            // it. The full player is the ladder's 1200, so a cover that fills
            // half the window is the 1200 px render instead of the 120 px
            // variant the provider hands out when nobody asks.
            val px = if (dataSaver) requestedPx.coerceAtMost(150) else requestedPx.coerceAtLeast(ArtworkPx.ROW)
            url.artworkAt(px)
        }

        isYtimg -> {
            val id = ytThumbVideoId.find(url)?.groupValues?.get(1) ?: return url
            if (dataSaver) {
                if (requestedPx >= 800) {
                    "https://i.ytimg.com/vi/$id/mqdefault.jpg"
                } else {
                    "https://i.ytimg.com/vi/$id/default.jpg"
                }
            } else {
                // The tiers are the video's own: 480x360 (`hqdefault`) is what
                // the player used to ask for above 320 px, and cropped to a
                // square it is only 360 px of cover - upscaled into the 400-500
                // px the full player draws it at, which is the soft cover the
                // owner kept reporting with *Data saver* off. Above 200 px the
                // request is the original (`maxresdefault`); a video without one
                // answers 404 and [fallbackThumbnailUrl] takes over.
                when {
                    requestedPx >= 200 -> "https://i.ytimg.com/vi/$id/maxresdefault.jpg"
                    requestedPx >= 100 -> "https://i.ytimg.com/vi/$id/hqdefault.jpg"
                    else -> "https://i.ytimg.com/vi/$id/mqdefault.jpg"
                }
            }
        }

        else -> url
    }
}

/**
 * Drawn width at which a cover is worth the stored original instead of a
 * rendered size: the full player and the Now Playing panel are the two surfaces
 * the owner reported as soft, and both are well above it.
 */
private const val BIG_ART_PX = 260

/**
 * The tier to ask for when the best one the provider has is not there.
 *
 * `maxresdefault` exists for most official videos and not for every upload; a
 * 404 in the image loader leaves the placeholder behind, so the cover falls back
 * to the 480x360 original tier instead of showing nothing. Null means "there is
 * no simpler URL to try" and the retry is skipped.
 */
fun fallbackThumbnailUrl(url: String?): String? {
    if (url.isNullOrBlank()) return null
    if (!url.contains("ytimg")) return null
    val id = ytThumbVideoId.find(url)?.groupValues?.get(1) ?: return null
    val fallback = "https://i.ytimg.com/vi/$id/hqdefault.jpg"
    return fallback.takeIf { it != url }
}

/**
 * Square-ish artwork with a neutral placeholder behind it while loading.
 *
 * [sizePx] is how big the image really is on screen (approximately): it decides
 * which quality tier is requested, so a 48 dp row does not download a 1200 px
 * cover and the full player does not show a blurry 120 px one.
 */
@Composable
fun Thumbnail(
    url: String?,
    modifier: Modifier = Modifier,
    sizePx: Int = 544,
    // Every artwork in the DE is the 4-sided cookie (`CookieShapes`), the same
    // shape the UI preview gives every cover: an artist's, a song's, an album's,
    // a playlist's and a player's big artwork alike. It is the default here so a
    // new call site inherits it instead of starting from a corner, and a caller
    // that needs the morphing one (`rememberArtistCookie`) still passes its own.
    shape: Shape = CookieShapes.fourSided,
) {
    // Data saver is part of the URL (it changes the requested tier), so the
    // resolved URL is remembered against the settings revision too.
    val revision = settingsFileRevision()
    val dataSaver = remember(revision) { DesktopSettings.load().dataSaver }
    // The size the image is ACTUALLY drawn at, measured off the layout: the
    // fixed default above is a guess, and a cover drawn wider than it (a big
    // header, the full player on a large window) came out soft because the
    // request stayed at 544 px. The measured size wins when it is the larger
    // of the two, so a thumbnail can never ask for less than it shows.
    var measuredPx by remember { mutableStateOf(0) }
    // The tier is decided on what is DRAWN, not on the caller's default: a 48 dp
    // row asks for 544 px because the default says so, and it must not be read
    // as "big artwork" and pull a full-resolution original.
    val drawnPx = if (measuredPx > 0) measuredPx else sizePx
    val model = remember(url, drawnPx, dataSaver) { adjustedThumbnailUrl(url, drawnPx, dataSaver) }
    // One step down, once: a video without a `maxresdefault` answers 404, and a
    // soft cover beats the placeholder. The step is remembered against the model
    // so a new track starts from the best tier again.
    var fellBack by remember(model) { mutableStateOf(false) }
    val shown = remember(model, fellBack) {
        if (fellBack) fallbackThumbnailUrl(model) ?: model else model
    }
    Box(
        modifier
            .clip(shape)
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .onSizeChanged { measuredPx = maxOf(it.width, it.height) },
    ) {
        if (!shown.isNullOrBlank()) {
            AsyncImage(
                model = shown,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                onError = { if (!fellBack) fellBack = true },
                modifier = Modifier.matchParentSize(),
            )
        }
    }
}

fun formatDuration(totalSeconds: Int): String {
    val minutes = totalSeconds / 60
    val seconds = totalSeconds % 60
    return "%d:%02d".format(minutes, seconds)
}

fun formatTime(millis: Long): String {
    val totalSeconds = millis / 1000
    val minutes = totalSeconds / 60
    val seconds = totalSeconds % 60
    return "%d:%02d".format(minutes, seconds)
}

private fun subtitleOf(item: YTItem): String = when (item) {
    is SongItem -> item.artists.joinToString(", ") { it.name }
    is AlbumItem -> buildString {
        item.artists?.joinToString(", ") { it.name }?.takeIf { it.isNotBlank() }?.let { append(it) }
        if (item.year != null) {
            if (isNotEmpty()) append(" • ")
            append(item.year)
        }
    }
    is ArtistItem -> "Artist"
    is PlaylistItem -> item.author?.name ?: item.songCountText.orEmpty()
}

/**
 * Compact tile used in the Home/Search/Artist carousels and Browse grid.
 *
 * [titleMaxLines] is the title's clamp: Home's and Explore's shelves keep one
 * line so the cards line up row after row, while a grid of fewer, wider cards
 * keeps two. The full name is on the tooltip either way.
 */
@Composable
fun YtItemCard(
    item: YTItem,
    onClick: () -> Unit,
    width: Dp? = 140.dp,
    titleMaxLines: Int = 2,
    modifier: Modifier = Modifier,
) {
    val root = if (width != null) Modifier.width(width) else Modifier.fillMaxWidth()
    val interaction = remember { MutableInteractionSource() }
    // The card turns with the pointer (the 3D tilt of Orion-Store's `data-tilt`)
    // as well as the cookie rounding its corners: the shape and the card under it
    // answer the same movement.
    Column(root.then(modifier).hoverable(interaction).pointerTilt().clickable(onClick = {
        AppLog.click("card '${item.title}'")
        onClick()
    })) {
        // A card's artwork is a rounded square, which is what the reference
        // draws and what these rows have to match. This is the one deliberate
        // departure from the cookie map in AGENTS.md: the 4-sided cookie stays
        // the shape of a song's row cover ([SongRow]) and of a playlist's 2x2
        // mosaic header, where it marks one object; on a shelf of 150 dp covers
        // its cut corners made every card look like a blob, and the reference
        // shows a plain 12 dp corner there.
        Thumbnail(
            item.thumbnail,
            Modifier.fillMaxWidth().aspectRatio(1f),
            shape = MaterialTheme.shapes.small,
        )
        Tooltip(item.title) {
            Text(
                item.title,
                style = MaterialTheme.typography.bodyMedium,
                maxLines = titleMaxLines,
                overflow = TextOverflow.Ellipsis,
            )
        }
        val subtitle = subtitleOf(item)
        if (subtitle.isNotBlank()) {
            Tooltip(subtitle) {
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

/** One song in a vertical list (album / playlist / search songs). */
@Composable
fun SongRow(
    song: SongItem,
    language: String,
    onClick: () -> Unit,
    onAddToQueue: (() -> Unit)? = null,
    onAddToPlaylist: (() -> Unit)? = null,
    /**
     * Whether to draw the track length on the right.
     *
     * Off in the history, and deliberately at the *row* level rather than by
     * emptying the durations: the history mixes rows built from this machine's
     * own records (which know the length) with rows the account returned (which
     * do not), so hiding the ones it happens to know is what made the same list
     * show a duration on some rows and nothing on others. A history answers
     * "what did I play", not "how long is it".
     */
    showDuration: Boolean = true,
) {
    val playback = LocalPlayback.current
    val isCurrent = song.id == playback.videoId
    val accent = MaterialTheme.colorScheme.primary
    // A song's cover is the 4-sided cookie, and it rounds its corners under the
    // pointer: the row is the hover target, so the shape answers wherever on the
    // row the mouse is rather than only over its 48 dp.
    val interaction = remember { MutableInteractionSource() }
    val hovered by interaction.collectIsHoveredAsState()
    val cover = rememberArtistCookie(hovered, label = "songCookie")
    Row(
        Modifier
            .fillMaxWidth()
            .hoverable(interaction)
            .clickable(onClick = {
                AppLog.click("song '${song.title}'")
                onClick()
            })
            .padding(vertical = 6.dp)
            .let { if (isCurrent) it.background(accent.copy(alpha = 0.07f), MaterialTheme.shapes.extraSmall) else it }
            .padding(horizontal = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        // The now-playing indicator sits ON the artwork, the way the mobile row
        // draws it: beside the cover it read as one more list icon, and on a row
        // that is already tinted it was hard to tell the song was the one
        // playing.
        Box(Modifier.size(48.dp).pointerTilt(label = "songCookieTilt")) {
            Thumbnail(song.thumbnail, Modifier.fillMaxSize(), shape = cover)
            if (isCurrent) {
                NowPlayingBars(
                    audioLevel = playback.audioLevel,
                    isPlaying = playback.isPlaying,
                    color = PaletteInk,
                    modifier = Modifier
                        .align(Alignment.Center)
                        .background(PaletteBlack.copy(alpha = 0.5f), MaterialTheme.shapes.extraSmall)
                        .padding(horizontal = 5.dp, vertical = 4.dp),
                )
            }
        }
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Tooltip(song.title) {
                Text(
                    song.title,
                    style = MaterialTheme.typography.bodyLarge,
                    color = if (isCurrent) accent else Color.Unspecified,
                    fontWeight = if (isCurrent) FontWeight.Bold else FontWeight.Normal,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            Tooltip(song.artists.joinToString(", ") { it.name }) {
                Text(
                    song.artists.joinToString(", ") { it.name },
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
        if (onAddToPlaylist != null) {
            Tooltip(Localization.get(language, "add_to_playlist")) {
                M3IconButton(onClick = onAddToPlaylist) {
                    Icon(
                        Icons.AutoMirrored.Filled.PlaylistAdd,
                        contentDescription = Localization.get(language, "add_to_playlist"),
                        tint = MaterialTheme.colorScheme.primary,
                    )
                }
            }
        }
        SongMenu(
            song = song,
            language = language,
            onAddToPlaylist = onAddToPlaylist,
            onAddToQueue = onAddToQueue?.let { add ->
                {
                    AppLog.click("add to queue '${song.title}'")
                    add()
                }
            },
        )
        if (showDuration) {
            song.duration?.let {
                Text(it.let(::formatDuration), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}

/**
 * Small animated equalizer bars marking the currently playing row. When
 * [audioLevel] is available and the track is playing, the bars move with the
 * real decoded audio; otherwise they fall back to a gentle idle pulse. Only
 * composed for the single current row, so the ~43 Hz level updates stay cheap.
 *
 * Internal rather than private: the queue and history rows of the player draw
 * the same indicator on their own artwork (see `AppleUpNextQueueScreen`).
 */
@Composable
internal fun NowPlayingBars(
    audioLevel: StateFlow<Float>?,
    isPlaying: Boolean,
    color: Color,
    modifier: Modifier = Modifier,
) {
    val level by (audioLevel ?: remember { kotlinx.coroutines.flow.MutableStateFlow(0.5f) }).collectAsState()
    val target = if (isPlaying) level.coerceIn(0.05f, 1f) else 0.05f
    val smooth by animateFloatAsState(
        targetValue = target,
        animationSpec = tween(durationMillis = 140, easing = LinearEasing),
        label = "npBarsLevel",
    )
    // Paused: the bars stop wobbling. The row stays marked (the bars are still
    // drawn at the idle height) but the endless 900 ms loop no longer keeps the
    // whole window redrawing while nothing is playing.
    val phase = if (isPlaying) {
        val transition = rememberInfiniteTransition(label = "npBars")
        val animated by transition.animateFloat(
            initialValue = 0f,
            targetValue = 1f,
            animationSpec = infiniteRepeatable(
                tween(Animations.ms(Motion.EXTRA_LONG3), easing = Motion.linear),
                RepeatMode.Restart,
            ),
            label = "npBarsPhase",
        )
        animated
    } else {
        0f
    }
    Canvas(modifier.size(width = 16.dp, height = 14.dp)) {
        val barWidth = size.width / 5f
        val gap = barWidth * 0.8f
        val maxH = size.height
        // The bars used to be drawn at `level * maxH`: a decoded RMS level of
        // music sits around 0.1-0.3, so on a 14 dp canvas every bar came out
        // between one and two pixels and the floor below clamped all three to
        // the same three pixels for the whole song — a frozen icon, which is
        // exactly the "the dots that follow the music don't move" report.
        // The level now *modulates* a span that is always visible: quiet music
        // moves through ~20-45% of the height, loud music through 45-100%, and
        // the corner case of a silent level still animates instead of freezing.
        val energy = 0.35f + 0.65f * smooth.coerceIn(0f, 1f)
        for (i in 0 until 3) {
            val wobble = 0.5f + 0.5f * sin((phase * 2 * Math.PI + i * 1.4).toFloat())
            val h = (maxH * energy * (0.45f + 0.55f * wobble))
                .coerceAtLeast(maxH * 0.18f)
            drawRoundRect(
                color = color,
                topLeft = Offset(i * (barWidth + gap), (maxH - h) / 2f),
                size = Size(barWidth, h),
                cornerRadius = CornerRadius(barWidth / 2f),
            )
        }
    }
}

@Composable
fun LoadingBox(language: String, hint: String? = null) {
    Box(Modifier.fillMaxSize().padding(16.dp)) {
        Column {
            Text(Localization.get(language, "loading"))
            // Some lists take noticeably longer than others, and a spinner on
            // its own reads as "stuck" after a few seconds: the caller can say
            // how long it may take.
            hint?.let {
                Text(
                    it,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 4.dp),
                )
            }
        }
    }
}

@Composable
fun ErrorBox(language: String, message: String?) {
    Box(Modifier.fillMaxSize().padding(16.dp)) {
        // Selectable so every network/load error can be copied for a bug report.
        SelectionContainer {
            Text("${Localization.get(language, "error")}: $message", color = MaterialTheme.colorScheme.error)
        }
        // And where to read what it means: a VIVI error carries a code (E1033,
        // SPOT-005, 1009), and the code is only useful if the user can look it
        // up. Same pointer the error dialogs carry.
        Text(
            "ERRORS.md · ${Localization.get(language, "website")}: $ERROR_PAGE_URL",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.align(Alignment.BottomStart),
        )
    }
}

/**
 * "Nothing here yet", with an optional retry, for a page that loaded fine but
 * has no items to draw. A page that loads empty used to render an empty grid —
 * indistinguishable from a broken screen.
 */
@Composable
fun EmptyBox(language: String, onRetry: (() -> Unit)? = null) {
    Column(
        modifier = Modifier.fillMaxSize().padding(16.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            Localization.get(language, "library_empty"),
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        if (onRetry != null) {
            Spacer(Modifier.height(12.dp))
            Button(onClick = onRetry) { Text(Localization.get(language, "refresh")) }
        }
    }
}

@Composable
fun BackButton(language: String, onClick: () -> Unit) {
    Text(
        "‹ ${Localization.get(language, "back")}",
        style = MaterialTheme.typography.titleMedium,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier
            .clickable {
                AppLog.click("back")
                onClick()
            }
            .padding(vertical = 8.dp),
    )
}

/**
 * The head the two sign-ins (Google and Spotify) and the signed-in Account screen
 * share: the brand mark on a filled 64 dp tile, what the screen is for, and - when
 * there is something to state - the state as one pill.
 *
 * The three screens are the same decision for the user - which account the app
 * talks to, and whether it has one - so they are one shape instead of three
 * differently built cards. [mark] draws the brand glyph on the tile; it inherits
 * `onPrimary`, so a mark that carries its own shape still reads on the fill.
 *
 * [stateText] is the identity when there is one; a `null` draws no pill at all.
 * A head with no account does not need to say "not connected": the card under it
 * is the one sign-in button and the title already says what the screen is for, so
 * a pill there only repeated the absence a third time.
 */
@Composable
internal fun SignInHero(
    title: String,
    promise: String,
    stateText: String?,
    active: Boolean,
    mark: @Composable () -> Unit,
) {
    Surface(
        modifier = Modifier.fillMaxWidth().padding(top = 16.dp),
        shape = MaterialTheme.shapes.extraLarge,
        color = MaterialTheme.colorScheme.surfaceContainerLow,
    ) {
        Row(
            Modifier.fillMaxWidth().padding(20.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            verticalAlignment = Alignment.Top,
        ) {
            Box(
                modifier = Modifier
                    .size(64.dp)
                    .clip(MaterialTheme.shapes.large)
                    .background(MaterialTheme.colorScheme.primary),
                contentAlignment = Alignment.Center,
            ) {
                CompositionLocalProvider(LocalContentColor provides MaterialTheme.colorScheme.onPrimary) {
                    mark()
                }
            }
            Column(Modifier.weight(1f)) {
                Text(
                    title,
                    style = MaterialTheme.typography.headlineSmall,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                Text(
                    promise,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 4.dp),
                )
                if (stateText != null) HeroStatePill(text = stateText, active = active)
            }
        }
    }
}

/** The state of a [SignInHero], said once: a dot and what it means. */
@Composable
internal fun HeroStatePill(text: String, active: Boolean) {
    Row(
        modifier = Modifier
            .padding(top = 12.dp)
            .clip(CircleShape)
            .background(MaterialTheme.colorScheme.surfaceContainerHighest)
            .padding(horizontal = 12.dp, vertical = 5.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            Modifier
                .size(8.dp)
                .clip(CircleShape)
                .background(
                    if (active) MaterialTheme.colorScheme.primary
                    else MaterialTheme.colorScheme.outline,
                ),
        )
        Text(
            text,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(start = 8.dp),
        )
    }
}

/**
 * The numbered sequence under a sign-in hero (`login_step1`-`3`,
 * `spotify_step1`-`3`): a sign-in is a sequence, and the number is what says so.
 * The two screens print the same three steps, so they share the list instead of
 * drawing it twice with two different paddings.
 */
@Composable
internal fun SignInStepList(language: String, keys: List<String>, firstStepTop: Dp = 16.dp) {
    keys.forEachIndexed { index, key ->
        Row(
            Modifier.fillMaxWidth().padding(top = if (index == 0) firstStepTop else 12.dp),
            verticalAlignment = Alignment.Top,
        ) {
            Box(
                modifier = Modifier
                    .size(26.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primaryContainer),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    "${index + 1}",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                )
            }
            Text(
                Localization.get(language, key),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(start = 10.dp),
            )
        }
    }
}

/** Route a tap on any [YTItem] to the matching screen/action. */
fun onItemClick(
    item: YTItem,
    onOpenAlbum: (String) -> Unit,
    onOpenArtist: (String) -> Unit,
    onOpenPlaylist: (String) -> Unit,
    onPlaySong: (SongItem) -> Unit,
) {
    when (item) {
        is AlbumItem -> onOpenAlbum(item.browseId)
        is ArtistItem -> onOpenArtist(item.id)
        is PlaylistItem -> onOpenPlaylist(item.id)
        is SongItem -> onPlaySong(item)
    }
}

/**
 * Section header in the style of the Android app's `NavigationTitle`: an
 * optional label above a bold, primary-coloured title, with an optional
 * "Play all" button and a chevron when the whole header is clickable.
 */
@Composable
fun SectionHeader(
    title: String,
    language: String,
    modifier: Modifier = Modifier,
    label: String? = null,
    onClick: (() -> Unit)? = null,
    onPlayAll: (() -> Unit)? = null,
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        modifier = modifier
            .fillMaxWidth()
            .clickable(enabled = onClick != null) {
                if (onClick != null) AppLog.click("section '${title.ifBlank { label.orEmpty() }}' see all")
                onClick?.invoke()
            }
            .padding(horizontal = 16.dp, vertical = 12.dp),
    ) {
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.Center) {
            if (!label.isNullOrBlank()) {
                Tooltip(label) {
                    Text(
                        label,
                        style = MaterialTheme.typography.labelLarge,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
            Tooltip(title) {
                Text(
                    title,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }

        if (onPlayAll != null) {
            Button(
                onClick = {
                    AppLog.click("section '${title.ifBlank { label.orEmpty() }}' play all")
                    onPlayAll()
                },
                shape = MaterialTheme.shapes.small,
                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 2.dp),
                modifier = Modifier.height(28.dp),
            ) {
                Text(Localization.get(language, "play_all"), style = MaterialTheme.typography.labelSmall)
            }
        }

        if (onClick != null) {
            Icon(
                Icons.AutoMirrored.Filled.KeyboardArrowRight,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
            )
        }
    }
}

/** A single Mood & genres chip, styled like the Android app's button with M3 Expressive geometry. */
@Composable
fun MoodAndGenresButton(title: String, onClick: () -> Unit, modifier: Modifier = Modifier) {
    Box(
        contentAlignment = Alignment.CenterStart,
        modifier = modifier
            .height(48.dp)
            .clip(RoundedCornerShape(topStart = ShapeRadii.medium, topEnd = ShapeRadii.extraSmall, bottomEnd = ShapeRadii.medium, bottomStart = ShapeRadii.extraSmall))
            .background(MaterialTheme.colorScheme.surfaceContainer)
            .clickable {
                AppLog.click("mood/genre '$title'")
                onClick()
            }
            .padding(horizontal = 14.dp),
    ) {
        Tooltip(title) {
            Text(
                title,
                style = MaterialTheme.typography.labelLarge,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

// ---------------------------------------------------------------------------
// ViviSlider: slim / squiggly / wavy slider styles (ported from mobile)
// ---------------------------------------------------------------------------

/** Slider visual style (matches the mobile slider-style setting). */
enum class ViviSliderStyle(val key: String) {
    SLIM("slim"),
    EXPRESSIVE("expressive"),
    SQUIGGLY("squiggly"),
    WAVY("wavy");

    companion object {
        fun from(key: String?): ViviSliderStyle = entries.firstOrNull { it.key == key } ?: SLIM
    }
}

/**
 * The player's slider: one thin straight track, the buffered segment behind the
 * played part, and no circle at its head unless the caller asks for one.
 *
 * The wavy and squiggly tracks and the thick Expressive capsule were removed on
 * the owner's instruction; [style] stays only so a stored `sliderStyle` key keeps
 * resolving, and it no longer changes the drawing.
 */
@Composable
fun ViviSlider(
    value: Float,
    onValueChange: (Float) -> Unit,
    valueRange: ClosedFloatingPointRange<Float> = 0f..1f,
    onValueChangeFinished: (() -> Unit)? = null,
    /** Kept for the stored key: the wavy and squiggly tracks are gone. */
    style: ViviSliderStyle = ViviSliderStyle.SLIM,
    enabled: Boolean = true,
    /** The circle at the head of the line. BitChord's sliders have none. */
    showThumb: Boolean = true,
    /** Optional buffered fraction (0..1) drawn as a fainter secondary segment
     *  behind the played portion (YouTube-style). Null hides it entirely;
     *  a full buffer (1f) draws it full-width behind the played fill, like a
     *  fully loaded YouTube video. */
    bufferedFraction: Float? = null,
    modifier: Modifier = Modifier,
) {
    val range = valueRange.endInclusive - valueRange.start
    val fraction = if (range == 0f) 0f else ((value - valueRange.start) / range).coerceIn(0f, 1f)
    // The buffer segment stays visible like on YouTube: it grows while the
    // stream downloads and remains full-width once fully loaded/cached (it is
    // only hidden when the buffered fraction is null, i.e. no stream info).
    val buffer = (bufferedFraction ?: 1f).coerceIn(0f, 1f)
    val showBuffer = bufferedFraction != null && buffer > fraction + 0.002f
    val bufferColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.35f)
    val primary = MaterialTheme.colorScheme.primary
    val trackColor = MaterialTheme.colorScheme.surfaceVariant
    val thumbColor = if (enabled) primary else MaterialTheme.colorScheme.outline

    Box(
        modifier
            .height(28.dp)
            .fillMaxWidth()
            // ONE gesture handler for press, drag and tap. Two `pointerInput`
            // blocks (one `detectTapGestures`, one `detectDragGestures`) fight
            // over the same pointer: the tap detector takes the down event, the
            // drag detector needs unconsumed changes to pass touch slop, and the
            // result is a thumb that only ever follows a tap — a drag looked like
            // it moved and then snapped back to the position the player last
            // reported, which is exactly "I cannot move the seek bar".
            .pointerInput(enabled, valueRange) {
                awaitEachGesture {
                    val down = awaitFirstDown(requireUnconsumed = false)
                    if (!enabled) return@awaitEachGesture
                    fun fraction(x: Float) = (x / size.width.toFloat().coerceAtLeast(1f)).coerceIn(0f, 1f)
                    // The press itself already moves the thumb, so the user sees
                    // where the seek is going before releasing (as on YouTube).
                    onValueChange(valueRange.start + fraction(down.position.x) * range)
                    down.consume()
                    while (true) {
                        val event = awaitPointerEvent()
                        val change = event.changes.firstOrNull { it.id == down.id } ?: break
                        if (!change.pressed) break
                        onValueChange(valueRange.start + fraction(change.position.x) * range)
                        change.consume()
                    }
                    onValueChangeFinished?.invoke()
                }
            },
        contentAlignment = Alignment.CenterStart,
    ) {
        val trackHeight = when (style) {
            ViviSliderStyle.SLIM -> 3.dp
            ViviSliderStyle.EXPRESSIVE -> 6.dp
            ViviSliderStyle.SQUIGGLY -> 6.dp
            ViviSliderStyle.WAVY -> 6.dp
        }
        Canvas(Modifier.fillMaxWidth().height(28.dp)) {
            val cy = size.height / 2f
            // One track, BitChord's: a 3 px line for the whole length, the
            // buffered segment behind the played part and the played part on
            // top. The squiggly zig-zag, the wavy sine and the thick Expressive
            // capsule are gone, together with the circle that used to sit at the
            // head of the line.
            drawRoundRect(
                color = trackColor,
                topLeft = Offset(0f, cy - 1.5f),
                size = androidx.compose.ui.geometry.Size(size.width, 3f),
                cornerRadius = CornerRadius(1.5f, 1.5f),
            )
            if (showBuffer) {
                drawRoundRect(
                    color = bufferColor,
                    topLeft = Offset(0f, cy - 1.5f),
                    size = androidx.compose.ui.geometry.Size(size.width * buffer, 3f),
                    cornerRadius = CornerRadius(1.5f, 1.5f),
                )
            }
            drawRoundRect(
                color = primary,
                topLeft = Offset(0f, cy - 1.5f),
                size = androidx.compose.ui.geometry.Size(size.width * fraction, 3f),
                cornerRadius = CornerRadius(1.5f, 1.5f),
            )
            if (showThumb) {
                val thumbRadius = 6f
                drawCircle(
                    color = thumbColor,
                    radius = thumbRadius,
                    center = Offset(size.width * fraction, cy),
                )
                // Subtle ring so the thumb is visible on any background.
                drawCircle(
                    color = PaletteInk.copy(alpha = 0.35f),
                    radius = thumbRadius,
                    center = Offset(size.width * fraction, cy),
                    style = Stroke(width = 1.5f),
                )
            }
        }
    }
}

/**
 * The Material 3 Expressive slider, used by every slider in the app that is not
 * a player's own seek or volume strip (those are [ViviSlider], which carries the
 * buffer segment and the player's four track styles).
 *
 * The geometry is the Expressive one, not `material3.Slider`'s: a 16 dp capsule
 * track (8 dp radius), a handle 4 dp wide and 44 dp tall that widens to 8 dp
 * while it is pressed, and a 6 dp cut either side of the handle where the track
 * stops. The active ink is `primary`, the rest of the track
 * `secondaryContainer`, and the handle draws a 40 dp state layer (8 % hovered,
 * 10 % pressed) so a mouse gets the same acknowledgement a finger does.
 *
 * The gesture is the one [ViviSlider] settled on - a single `pointerInput` for
 * press, drag and release - because a press must already move the value and a
 * separate tap detector steals the down event from the drag. `steps` snaps the
 * reported value to the ticks the caller declared, the way the M3 slider does.
 */
@Composable
fun M3SettingsSlider(
    value: Float,
    onValueChange: (Float) -> Unit,
    valueRange: ClosedFloatingPointRange<Float> = 0f..1f,
    steps: Int = 0,
    enabled: Boolean = true,
    onValueChangeFinished: (() -> Unit)? = null,
    modifier: Modifier = Modifier,
) {
    val range = valueRange.endInclusive - valueRange.start
    val fraction = if (range == 0f) 0f else ((value - valueRange.start) / range).coerceIn(0f, 1f)
    val primary = MaterialTheme.colorScheme.primary
    val inactive = MaterialTheme.colorScheme.secondaryContainer
    val handleColor = if (enabled) primary else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f)

    var pressed by remember { mutableStateOf(false) }
    val interactionSource = remember { MutableInteractionSource() }
    val hovered by interactionSource.collectIsHoveredAsState()
    // The state layer is an effect (it must not overshoot), the handle's own
    // growth is a movement (the spatial spring gives a press its trace of
    // overshoot). Both are off with the master switch, like every transition.
    val stateLayer by animateFloatAsState(
        targetValue = if (pressed) 0.10f else if (hovered && enabled) 0.08f else 0f,
        animationSpec = if (Animations.instant) snap() else Motion.effectsFast<Float>(),
        label = "sliderStateLayer",
    )
    val handleWidth by animateFloatAsState(
        targetValue = if (pressed) 8f else 4f,
        animationSpec = if (Animations.instant) snap() else Motion.spatialFast<Float>(),
        label = "sliderHandleWidth",
    )

    fun snapped(raw: Float): Float {
        if (steps <= 0 || range == 0f) return raw.coerceIn(valueRange.start, valueRange.endInclusive)
        val step = range / (steps + 1)
        val tick = ((raw - valueRange.start) / step).roundToInt() * step + valueRange.start
        return tick.coerceIn(valueRange.start, valueRange.endInclusive)
    }

    Canvas(
        modifier = modifier
            .height(48.dp)
            .fillMaxWidth()
            .hoverable(interactionSource, enabled = enabled)
            .pointerInput(enabled, valueRange, steps) {
                awaitEachGesture {
                    val down = awaitFirstDown(requireUnconsumed = false)
                    if (!enabled) return@awaitEachGesture
                    // The handle's own half-width, so it never pokes out of the ends.
                    val inset = 4.dp.toPx()
                    fun valueAt(x: Float): Float {
                        val usable = (size.width.toFloat() - inset * 2f).coerceAtLeast(1f)
                        val f = ((x - inset) / usable).coerceIn(0f, 1f)
                        return snapped(valueRange.start + f * range)
                    }
                    pressed = true
                    onValueChange(valueAt(down.position.x))
                    down.consume()
                    while (true) {
                        val event = awaitPointerEvent()
                        val change = event.changes.firstOrNull { it.id == down.id } ?: break
                        if (!change.pressed) break
                        onValueChange(valueAt(change.position.x))
                        change.consume()
                    }
                    pressed = false
                    onValueChangeFinished?.invoke()
                }
            },
    ) {
        val trackHeight = 16.dp.toPx()
        val gap = 6.dp.toPx()
        val handleHeight = 44.dp.toPx()
        val handleWidthPx = handleWidth.dp.toPx()
        val inset = 4.dp.toPx()
        val cy = size.height / 2f
        val usable = (size.width - inset * 2f).coerceAtLeast(1f)
        val centerX = inset + usable * fraction
        val radius = CornerRadius(trackHeight / 2f, trackHeight / 2f)

        if (stateLayer > 0f) {
            drawCircle(
                color = primary.copy(alpha = stateLayer),
                radius = 20.dp.toPx(),
                center = Offset(centerX, cy),
            )
        }

        // The track is two capsules with the handle's gap cut out between them,
        // so neither end runs under the handle or past the edge of the strip.
        val activeEnd = centerX - handleWidthPx / 2f - gap
        val inactiveStart = centerX + handleWidthPx / 2f + gap
        if (activeEnd > 0f) {
            drawRoundRect(
                color = primary,
                topLeft = Offset(0f, cy - trackHeight / 2f),
                size = Size(activeEnd, trackHeight),
                cornerRadius = radius,
            )
        }
        if (inactiveStart < size.width) {
            drawRoundRect(
                color = inactive,
                topLeft = Offset(inactiveStart, cy - trackHeight / 2f),
                size = Size(size.width - inactiveStart, trackHeight),
                cornerRadius = radius,
            )
        }

        drawRoundRect(
            color = handleColor,
            topLeft = Offset(centerX - handleWidthPx / 2f, cy - handleHeight / 2f),
            size = Size(handleWidthPx, handleHeight),
            cornerRadius = CornerRadius(handleWidthPx / 2f, handleWidthPx / 2f),
        )
    }
}

/**
 * A horizontal carousel: a [LazyRow] with a scroll arrow at each end.
 *
 * Every Home/Browse row is a `LazyRow`, and on a mouse without a horizontal
 * wheel (or without a trackpad) everything past the right edge is simply
 * unreachable — the row shows its first three or four cards and nothing says
 * there are more, which is what makes "Mood & genres" and the "made for you"
 * playlists look like they only hold the few entries that fit. This puts the
 * Spotify desktop home rows' own affordance over the row: a round arrow at each
 * end, each press moving the row about a page.
 *
 * An arrow that cannot do anything is worse than no arrow, so each one is drawn
 * only while ITS OWN direction has something to scroll to. The row at its start
 * has no left arrow, the row at its end has no right arrow, and a row that
 * already fits has neither: they are absent, not dimmed. Showing a dead, faded
 * arrow on the side the row cannot move towards was the remaining half of the
 * old behaviour (both arrows used to appear as soon as either direction could
 * move).
 *
 * `language` is only there for the two tooltips, so the buttons are named the
 * way every other icon button in the app is (see the tooltip rule in
 * AGENTS.md); the labels come from the existing `previous` / `next` keys.
 */
@Composable
fun HorizontalCarousel(
    language: String,
    modifier: Modifier = Modifier,
    horizontalArrangement: Arrangement.Horizontal = Arrangement.spacedBy(12.dp),
    contentPadding: PaddingValues = PaddingValues(0.dp),
    content: LazyListScope.() -> Unit,
) {
    val state = rememberLazyListState()
    val scope = rememberCoroutineScope()
    // Material 3 Expressive's carousel snapping: the row settles with a card's
    // edge on the gutter instead of stopping half way through one, which is what
    // makes a row of covers read as a carousel and not as a strip cut mid-card.
    // The arrows are unchanged and still move the row by a page.
    val fling = rememberSnapFlingBehavior(lazyListState = state)
    // The arrows belong to the row and exist only while the pointer is on it: a
    // permanent circle sitting on the half-visible card at the edge reads as a
    // defect over a cut cover, which is also what the reference never draws.
    // Hovering the row is what reveals them, and each one is still drawn only
    // while ITS direction has somewhere to scroll.
    val interaction = remember { MutableInteractionSource() }
    val hovered by interaction.collectIsHoveredAsState()
    // The arrows must not eat the row's own clicks: the Box only overlays them,
    // the LazyRow keeps the full width underneath.
    Box(modifier.fillMaxWidth().hoverable(interaction)) {
        LazyRow(
            state = state,
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = horizontalArrangement,
            contentPadding = contentPadding,
            flingBehavior = fling,
            content = content,
        )

        // Both flags come from the row's own layout, so they are recomputed when
        // it scrolls or the window is resized, not on every recomposition.
        val canGoBackward by remember { derivedStateOf { state.canScrollBackward } }
        val canGoForward by remember { derivedStateOf { state.canScrollForward } }
        // The `align` modifier has to sit on a direct child of THIS Box. It
        // used to be handed to `CarouselArrow` and applied to the `Surface`
        // inside `Tooltip`, whose own internal Box consumed it: both arrows
        // then fell back to the Box's default top-start position, and since
        // the forward one is drawn last it covered the backward one. That is
        // why only one arrow ever showed, on the left. Wrapping each arrow in
        // its own aligned Box fixes the placement.
        if (hovered && canGoBackward) {
            Box(Modifier.align(Alignment.CenterStart)) {
                CarouselArrow(
                    language = language,
                    forward = false,
                    onClick = { scope.launch { state.animateScrollBy(-carouselPage(state)) } },
                )
            }
        }
        if (hovered && canGoForward) {
            Box(Modifier.align(Alignment.CenterEnd)) {
                CarouselArrow(
                    language = language,
                    forward = true,
                    onClick = { scope.launch { state.animateScrollBy(carouselPage(state)) } },
                )
            }
        }
    }
}

/**
 * How far one press moves the row: most of a screenful, so a press reveals the
 * cards that were cut off at the edge instead of nudging them by one card.
 * Never so small that a press looks like it did nothing (a narrow row/panel).
 */
private fun carouselPage(state: LazyListState): Float =
    (state.layoutInfo.viewportSize.width * 0.85f).coerceAtLeast(240f)

@Composable
private fun CarouselArrow(
    language: String,
    forward: Boolean,
    onClick: () -> Unit,
) {
    Tooltip(Localization.get(language, if (forward) "next" else "previous")) {
        Surface(
            onClick = onClick,
            shape = CircleShape,
            color = MaterialTheme.colorScheme.surfaceContainerHighest.copy(alpha = 0.92f),
            contentColor = MaterialTheme.colorScheme.onSurface,
            shadowElevation = 3.dp,
            modifier = Modifier.size(34.dp),
        ) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Icon(
                    imageVector = if (forward) Icons.AutoMirrored.Filled.KeyboardArrowRight
                    else Icons.AutoMirrored.Filled.KeyboardArrowLeft,
                    contentDescription = null,
                    modifier = Modifier.size(20.dp),
                )
            }
        }
    }
}

/**
 * Unique `LazyColumn` keys for a list that may hold the same id twice.
 *
 * A queue and a local playlist both accept the same track again - adding it
 * twice is a normal thing to do - and a repeated key is a hard crash in Compose
 * ("Key ... was already used"), not a warning: that is how a track added twice
 * took the whole app down. Each key is the id plus how many times it has already
 * appeared, which is unique AND keeps following the same entry while the list is
 * reordered with a drag (an id used once gets `#0` and never moves).
 */
internal fun List<String>.withOccurrences(): List<String> {
    val seen = HashMap<String, Int>()
    return map { id ->
        val n = seen.getOrDefault(id, 0)
        seen[id] = n + 1
        "$id#$n"
    }
}

// ---------------------------------------------------------------------------
// The head of a settings screen (the Material 3 hero)
// ---------------------------------------------------------------------------

/**
 * A settings screen's head, drawn as one Material 3 card.
 *
 * Several screens answer "what is this, and how much of it is there" with a line
 * of headline text and then the numbers somewhere below - Storage is the one that
 * came first - so the shape lives here instead of being retyped per screen: a
 * 56 dp glyph tile on `secondaryContainer`, the title in `headlineSmall` over a
 * sentence saying what the screen is for, the screen's own figure on the right of
 * that row, and a block under it for what the figure is made of (Storage's
 * breakdown bar, Commits' chips). The card is `surfaceVariant` at 30 %, the fill
 * the equalizer's curve card uses, so the head reads as part of the same family
 * without a second elevation.
 *
 * [trailing] and [content] are optional: a screen with no single figure passes
 * neither and gets the title row alone.
 */
@Composable
fun M3ScreenHero(
    icon: ImageVector,
    title: String,
    description: String?,
    modifier: Modifier = Modifier,
    trailing: (@Composable () -> Unit)? = null,
    content: (@Composable ColumnScope.() -> Unit)? = null,
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.extraLarge,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
    ) {
        Column(Modifier.fillMaxWidth().padding(horizontal = 17.dp, vertical = 16.dp)) {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(56.dp)
                        .clip(MaterialTheme.shapes.large)
                        .background(MaterialTheme.colorScheme.secondaryContainer),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        icon,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSecondaryContainer,
                        modifier = Modifier.size(28.dp),
                    )
                }
                Spacer(Modifier.width(16.dp))
                Column(Modifier.weight(1f)) {
                    Text(title, style = MaterialTheme.typography.headlineSmall)
                    if (description != null) {
                        Text(
                            description,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(top = 2.dp),
                        )
                    }
                }
                if (trailing != null) {
                    Spacer(Modifier.width(16.dp))
                    trailing()
                }
            }
            if (content != null) {
                Spacer(Modifier.height(14.dp))
                content()
            }
        }
    }
}

/**
 * A small informational chip, the shape `AssistChip` has without pretending to be
 * a button: the Commits head says which branch, how many commits and which
 * repository in three of them and none of the three does anything.
 */
@Composable
fun M3InfoChip(text: String, icon: ImageVector? = null, tonal: Boolean = false) {
    val container = if (tonal) MaterialTheme.colorScheme.secondaryContainer
    else MaterialTheme.colorScheme.surfaceContainerHigh
    val ink = if (tonal) MaterialTheme.colorScheme.onSecondaryContainer
    else MaterialTheme.colorScheme.onSurfaceVariant
    Row(
        modifier = Modifier
            .clip(MaterialTheme.shapes.extraSmall)
            .background(container)
            .padding(horizontal = 10.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        if (icon != null) {
            Icon(icon, contentDescription = null, tint = ink, modifier = Modifier.size(16.dp))
        }
        Text(text, style = MaterialTheme.typography.labelLarge, color = ink)
    }
}

/** One part of an [M3SegmentedBar]: what it is called, its colour, its weight and its own figure. */
data class M3BarPart(
    val label: String,
    val color: Color,
    val weight: Float,
    val value: String? = null,
)

/**
 * A bar showing how a total is made of its parts.
 *
 * Each part is a pill sized by its own weight, so a cache that is half the disk
 * is half the strip: the only place on the screen where the four numbers are one
 * figure, which is why the total above it is not enough on its own. A part of
 * zero is not drawn (an absent pill and an empty one look the same, and only one
 * of them can be pressed); the caller passes what it has.
 */
@Composable
fun M3SegmentedBar(parts: List<M3BarPart>, modifier: Modifier = Modifier) {
    val shown = parts.filter { it.weight > 0f }
    Row(
        modifier = modifier.fillMaxWidth().height(12.dp),
        horizontalArrangement = Arrangement.spacedBy(3.dp),
    ) {
        shown.forEach { part ->
            Box(
                Modifier
                    .weight(part.weight.coerceAtLeast(0.001f))
                    .fillMaxSize()
                    .clip(MaterialTheme.shapes.extraSmall)
                    .background(part.color),
            )
        }
    }
}

/**
 * The names under an [M3SegmentedBar]: each part's dot, its label and its figure.
 *
 * A bar says the proportions and nothing else, so without this the four colours
 * are unreadable. Two per line, so the labels stay legible in a narrow window,
 * and the figure repeats the one the row below carries.
 */
@Composable
fun M3SegmentedLegend(parts: List<M3BarPart>, modifier: Modifier = Modifier) {
    Column(modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(6.dp)) {
        parts.chunked(2).forEach { line ->
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                line.forEach { part ->
                    Row(Modifier.weight(1f), verticalAlignment = Alignment.CenterVertically) {
                        Box(Modifier.size(8.dp).clip(CircleShape).background(part.color))
                        Spacer(Modifier.width(6.dp))
                        Tooltip(part.label, modifier = Modifier.weight(1f)) {
                            Text(
                                part.label,
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                        }
                        if (part.value != null) {
                            Text(
                                part.value,
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                }
                // An odd number of parts leaves the last cell empty, so a lone
                // item keeps the width of the pair above it.
                if (line.size == 1) Spacer(Modifier.weight(1f))
            }
        }
    }
}
