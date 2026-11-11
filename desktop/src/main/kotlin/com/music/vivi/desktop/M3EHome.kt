package com.music.vivi.desktop

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.music.innertube.models.AlbumItem
import com.music.innertube.models.Artist
import com.music.innertube.models.ArtistItem
import com.music.innertube.models.PlaylistItem
import com.music.innertube.models.SongItem
import com.music.innertube.models.YTItem
import com.music.innertube.pages.HomePage
import com.music.vivi.sync.SyncedPlaylist
import kotlin.math.cos
import kotlin.math.sin

/**
 * The m3e Home card height: the reference sketch's 180 dp shelf card, with the
 * title drawn on the cover's bottom edge instead of under the card.
 */
internal val M3ECardHeight = 180.dp

/**
 * Home in the Material 3 Expressive design language.
 *
 * The sketch is a stack of rows, each one a 28 sp heading over a sideways
 * carousel of 180 dp cards whose titles sit on the cover, spread over the screen
 * height with equal gaps while there are three rows to spread, and 36 dp apart
 * once the feed's whole category set makes the page scroll. Its first row is the machine's own **Library** (this
 * installation's playlists, then the local listening history once they run
 * out); after it comes **every category the account's Home returns**, with the
 * feed's own title - `Playlist Pop`, `Successi pop senza tempo`, `Nuove
 * scoperte`, `Vecchi successi`, `Discoteca`, `Party del passato`, `Playlist in
 * primo piano per te`, `Album per te` and so on. Those names are the feed's for
 * this listener, so the rows are the feed's sections rather than a fixed set:
 * a different account gets its own categories without a code change.
 *
 * A row with nothing to show is not drawn, and a row that is still loading draws
 * the shape-morphing indicator in its own 180 dp band, so each section says
 * "loading" by itself. When there are only the three reference rows the body
 * spreads them over the viewport; once the feed brings more categories the rows
 * sit closer and the page scrolls, because four or more spread rows would leave
 * more air than content.
 *
 * The cards are plain Material 3 cards: the 16 dp corner of the shape scale, a
 * scrim and the title on the cover.
 */
@Composable
internal fun M3EHomeContent(
    home: HomePage?,
    localCollections: List<SyncedPlaylist>,
    history: List<HistoryEntry>,
    language: String,
    onOpenAlbum: (String) -> Unit,
    onOpenArtist: (String) -> Unit,
    onOpenPlaylist: (String) -> Unit,
    onOpenLocalPlaylist: (String) -> Unit,
    onPlaySong: (SongItem) -> Unit,
) {
    val rows = remember(home, localCollections, history, language) {
        m3eRows(home, localCollections, history, language)
    }
    val click = remember(onOpenAlbum, onOpenArtist, onOpenPlaylist, onOpenLocalPlaylist, onPlaySong) {
        M3EClick(onOpenAlbum, onOpenArtist, onOpenPlaylist, onOpenLocalPlaylist, onPlaySong)
    }
    BoxWithConstraints(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.surface)) {
        // The sketch spreads the parts over the screen height: three rows get the
        // viewport as their minimum height and equal gaps, a page full of the
        // feed's categories keeps a steady 28 dp between rows and scrolls.
        val viewport = maxHeight
        val scroll = rememberScrollState()
        val spread = rows.size <= 3
        Column(Modifier.fillMaxSize().verticalScroll(scroll)) {
            Column(
                Modifier
                    .fillMaxWidth()
                    .heightIn(min = viewport)
                    .padding(horizontal = 16.dp, vertical = 24.dp),
                verticalArrangement = if (spread) Arrangement.SpaceEvenly else Arrangement.spacedBy(36.dp),
            ) {
                rows.forEach { row -> M3ERow(row, language, click) }
            }
        }
    }
}

/** One row: its heading, then the carousel or its loader. */
internal data class M3ERow(
    /** The heading as it is drawn: the feed's own category name, or a key. */
    val title: String,
    val loading: Boolean,
    val items: List<M3EHomeItem>,
)

/**
 * What one m3e card holds: this machine's playlist, a history track, or any item
 * the feed returned. The cover, the card and the title are one drawing; only
 * what they hold changes.
 */
internal sealed interface M3EHomeItem {
    val id: String

    data class Local(val playlist: SyncedPlaylist) : M3EHomeItem {
        override val id: String get() = "local-" + playlist.id
    }

    data class Track(val entry: HistoryEntry) : M3EHomeItem {
        override val id: String get() = "track-" + entry.videoId
    }

    data class Yt(val item: YTItem) : M3EHomeItem {
        override val id: String get() = "yt-" + item.id
    }
}

/** The callbacks one m3e card may need, kept together so the card takes one. */
private class M3EClick(
    val onOpenAlbum: (String) -> Unit,
    val onOpenArtist: (String) -> Unit,
    val onOpenPlaylist: (String) -> Unit,
    val onOpenLocalPlaylist: (String) -> Unit,
    val onPlaySong: (SongItem) -> Unit,
)

/**
 * The rows of the m3e Home.
 *
 * The Library row is the machine's own; after it, every section of the page is
 * one row, titled by the feed. The feed's sections arrive in the order the
 * account is meant to read them, so they are kept as they come and only an
 * empty one is dropped. While the page is in flight the three reference rows
 * are drawn with their loaders, so the first paint already has the layout.
 */
internal fun m3eRows(
    home: HomePage?,
    localCollections: List<SyncedPlaylist>,
    history: List<HistoryEntry>,
    language: String,
): List<M3ERow> {
    val library = localCollections.map { M3EHomeItem.Local(it) } +
        history.take(12).map { M3EHomeItem.Track(it) }
    val libraryRow = M3ERow(
        title = Localization.get(language, "library"),
        loading = library.isEmpty() && home == null,
        items = library,
    )
    if (home == null) {
        return listOf(
            libraryRow,
            M3ERow(Localization.get(language, "quick_picks"), true, emptyList()),
            M3ERow(Localization.get(language, "community"), true, emptyList()),
        )
    }
    val categories = home.sections.distinctBy { it.title }.mapNotNull { section ->
        val items = section.items.distinctBy { it.id }.map { M3EHomeItem.Yt(it) }
        if (items.isEmpty()) {
            null
        } else {
            M3ERow(
                title = section.title.ifBlank { Localization.get(language, "community") },
                loading = false,
                items = items,
            )
        }
    }
    // A library with nothing in it is not a section either.
    return (if (library.isEmpty()) emptyList() else listOf(libraryRow)) + categories
}

@Composable
private fun M3ERow(row: M3ERow, language: String, click: M3EClick) {
    // A loaded row with nothing in it is not a section: an empty carousel under
    // a heading is a hole, and while the page is in flight the loader is the row.
    if (row.items.isEmpty() && !row.loading) return
    Column(Modifier.fillMaxWidth()) {
        Text(
            row.title,
            style = MaterialTheme.typography.headlineMedium,
            modifier = Modifier.padding(bottom = 10.dp),
        )
        if (row.items.isEmpty()) {
            Box(
                Modifier.fillMaxWidth().height(M3ECardHeight),
                contentAlignment = Alignment.CenterStart,
            ) {
                M3ELoadingIndicator()
            }
        } else {
            ExpressiveShelf(
                items = row.items,
                itemKey = { it.id },
                contentPadding = PaddingValues(horizontal = 0.dp),
                language = language,
                showIndicator = false,
            ) { item, width ->
                M3EHomeCard(item, width, language, click)
            }
        }
    }
}

/**
 * One m3e card: the cover on the shape scale's 16 dp corner, a scrim and the
 * title on the bottom edge, as the sketch draws it. The card is a plain Material
 * 3 container, so the scrim and the title follow the same rounded corner as the
 * cover under them.
 */
@Composable
private fun M3EHomeCard(item: M3EHomeItem, width: Dp, language: String, click: M3EClick) {
    val cardShape = MaterialTheme.shapes.medium
    val title = when (item) {
        is M3EHomeItem.Local -> item.playlist.name.ifBlank { Localization.get(language, "playlist") }
        is M3EHomeItem.Track -> item.entry.title
        is M3EHomeItem.Yt -> item.item.title
    }
    val subtitle = when (item) {
        is M3EHomeItem.Local -> {
            val count = item.playlist.songs.size
            "$count " + Localization.get(language, "songs").lowercase()
        }
        is M3EHomeItem.Track -> item.entry.artist
        is M3EHomeItem.Yt -> when (val yt = item.item) {
            is AlbumItem -> yt.artists?.joinToString(", ") { it.name }.orEmpty()
            is SongItem -> yt.artists.joinToString(", ") { it.name }
            is PlaylistItem -> yt.author?.name ?: yt.songCountText.orEmpty()
            is ArtistItem -> ""
            else -> ""
        }
    }
    Column(
        Modifier
            .width(width)
            .pointerTilt()
            .clickable {
                when (item) {
                    is M3EHomeItem.Local -> {
                        AppLog.click("m3e playlist '${item.playlist.name}'")
                        click.onOpenLocalPlaylist(item.playlist.id)
                    }
                    is M3EHomeItem.Track -> {
                        AppLog.click("m3e track '${item.entry.title}'")
                        click.onPlaySong(item.entry.asSongItem())
                    }
                    is M3EHomeItem.Yt -> onItemClick(
                        item.item,
                        click.onOpenAlbum,
                        click.onOpenArtist,
                        click.onOpenPlaylist,
                        click.onPlaySong,
                    )
                }
            },
    ) {
        Box(Modifier.fillMaxWidth().height(M3ECardHeight).clip(cardShape)) {
            when (item) {
                is M3EHomeItem.Local -> PlaylistMosaic(item.playlist, Modifier.fillMaxSize(), cardShape)
                // The box above already wears the card's corner: the cover is
                // clipped to the card, not to a shape of its own.
                is M3EHomeItem.Track -> Thumbnail(item.entry.thumbnail, Modifier.fillMaxSize(), shape = RectangleShape)
                is M3EHomeItem.Yt -> Thumbnail(item.item.thumbnail, Modifier.fillMaxSize(), shape = RectangleShape)
            }
            Box(
                Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .height(84.dp)
                    .background(
                        Brush.verticalGradient(
                            listOf(Color.Transparent, PaletteBlack.copy(alpha = 0.78f)),
                        ),
                    ),
            )
            Column(
                Modifier
                    .align(Alignment.BottomStart)
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 12.dp),
            ) {
                Tooltip(title) {
                    Text(
                        title,
                        style = MaterialTheme.typography.titleMedium,
                        color = PaletteInk,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
                if (subtitle.isNotBlank()) {
                    Tooltip(subtitle) {
                        Text(
                            subtitle,
                            style = MaterialTheme.typography.bodySmall,
                            color = PaletteInk.copy(alpha = 0.82f),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                }
            }
        }
    }
}

/**
 * The Material 3 Expressive shape-morphing loader: a 12-lobed polygon that turns
 * while its lobes grow out into a circle, inside the contained variant's
 * `secondaryContainer` disc.
 *
 * The shapes and the durations are the project's own: one turn is
 * `Motion.EXTRA_LONG4`, so a change to the motion table reaches the loader too.
 */
@Composable
fun M3ELoadingIndicator(
    modifier: Modifier = Modifier,
    size: Dp = 36.dp,
) {
    val ink = MaterialTheme.colorScheme.primary
    val container = MaterialTheme.colorScheme.secondaryContainer
    val transition = rememberInfiniteTransition(label = "m3eLoading")
    val turn by transition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(Motion.EXTRA_LONG4, easing = LinearEasing),
            repeatMode = RepeatMode.Restart,
        ),
        label = "m3eLoadingTurn",
    )
    val morph by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(Motion.EXTRA_LONG4, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "m3eLoadingMorph",
    )
    Box(
        modifier.size(size).clip(CircleShape).background(container),
        contentAlignment = Alignment.Center,
    ) {
        Canvas(Modifier.size(size * 0.62f)) {
            val radius = this.size.minDimension / 2f
            val lobes = 12
            val inner = radius * (0.62f + 0.38f * morph)
            val path = Path()
            repeat(lobes * 2) { index ->
                val angle = (Math.PI * index / lobes).toFloat() - (Math.PI / 2.0).toFloat()
                val r = if (index % 2 == 0) radius else inner
                val x = center.x + cos(angle) * r
                val y = center.y + sin(angle) * r
                if (index == 0) path.moveTo(x, y) else path.lineTo(x, y)
            }
            path.close()
            rotate(turn, pivot = center) {
                drawPath(path, color = ink)
            }
        }
    }
}

/** A history row as the player's own model, so tapping it plays the track. */
private fun HistoryEntry.asSongItem(): SongItem = SongItem(
    id = videoId,
    title = title,
    artists = listOf(Artist(name = artist.ifBlank { "Unknown" }, id = null)),
    duration = (durationMs / 1000L).toInt().takeIf { it > 0 },
    thumbnail = thumbnail.orEmpty(),
)
