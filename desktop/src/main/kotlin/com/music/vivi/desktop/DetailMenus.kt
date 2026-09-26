package com.music.vivi.desktop

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp

/**
 * One entry of a detail screen's context menu (Album / Artist / Playlist).
 * The label is always a localized string: the menus never print a raw key.
 */
class DetailMenuEntry(
    val label: String,
    val icon: ImageVector,
    val onClick: () -> Unit,
)

/**
 * The "⋮" context menu the Album, Artist and Playlist screens share.
 *
 * The song menu (like, library, add to playlist, share) already existed on the
 * rows; these screens had no menu of their own, so the whole-collection
 * actions — play next, add all to the queue, go to the artist, share, refresh —
 * were only reachable by playing a song or opening a row menu. The entries are
 * built by each screen, so the menu only lists what that screen can actually do.
 */
@Composable
fun DetailMenu(
    language: String,
    entries: List<DetailMenuEntry>,
    tint: Color = Color.Unspecified,
) {
    if (entries.isEmpty()) return
    var expanded by remember { mutableStateOf(false) }
    Box {
        Tooltip(Localization.get(language, "tooltip_more")) {
            IconButton(onClick = { expanded = true }) {
                Icon(
                    Icons.Filled.MoreVert,
                    contentDescription = Localization.get(language, "more"),
                    tint = tint,
                )
            }
        }
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            entries.forEach { entry ->
                DropdownMenuItem(
                    text = { Text(entry.label) },
                    leadingIcon = { Icon(entry.icon, contentDescription = null) },
                    onClick = {
                        expanded = false
                        entry.onClick()
                    },
                )
            }
        }
    }
}

/**
 * The gradient header shared by the Album, Artist and Playlist screens.
 *
 * Those screens drew a plain row — a thumbnail, a title and a subtitle on the
 * window background — while the mobile app puts the artwork behind the header
 * as a gradient that fades into the page. The artwork is decoded and blurred by
 * [CachedBlurBackdrop] (the same helper the rest of the app uses, so the cost is
 * paid once per URL and cached), then a vertical scrim turns it into a gradient
 * that ends exactly on the page background, so the header has no hard edge and
 * the content below starts on the normal surface.
 *
 * The text is white on purpose: the scrim is dark at the top, so the title stays
 * readable whatever the artwork's own colours are, in light and dark mode alike.
 */
@Composable
fun GradientHeader(
    title: String,
    thumbnailUrl: String?,
    language: String,
    menuEntries: List<DetailMenuEntry>,
    subtitle: String? = null,
    meta: String? = null,
    modifier: Modifier = Modifier,
) {
    val fade = MaterialTheme.colorScheme.background
    Box(modifier.fillMaxWidth()) {
        CachedBlurBackdrop(
            artworkUrl = thumbnailUrl,
            blurRadiusPx = 120f,
            scrimColor = Color.Transparent,
            modifier = Modifier.matchParentSize(),
        ) {}
        // The scrim: dark enough at the top for white text, fading to the page
        // background at the bottom so the header blends into the list under it.
        Box(
            Modifier
                .matchParentSize()
                .background(
                    Brush.verticalGradient(
                        0f to Color.Black.copy(alpha = 0.45f),
                        0.7f to Color.Black.copy(alpha = 0.25f),
                        1f to fade,
                    )
                )
        )
        Row(
            Modifier.fillMaxWidth().padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Thumbnail(thumbnailUrl, Modifier.size(128.dp), sizePx = 256)
            Spacer(Modifier.width(16.dp))
            Column(Modifier.weight(1f)) {
                Text(
                    title,
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
                subtitle?.takeIf { it.isNotBlank() }?.let {
                    Text(
                        it,
                        style = MaterialTheme.typography.bodyLarge,
                        color = Color.White.copy(alpha = 0.82f),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
                meta?.takeIf { it.isNotBlank() }?.let {
                    Text(
                        it,
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color.White.copy(alpha = 0.72f),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
            DetailMenu(language, menuEntries, tint = Color.White)
        }
    }
}

/**
 * Session state of the detail-screen menus: whether an album or a playlist was
 * liked, and whether an artist was subscribed, **in this run**.
 *
 * The desktop has no local database like the mobile app (where the like lives on
 * the album row), so the menus keep the optimistic state here and push the real
 * change to YouTube Music through `YouTube.likePlaylist` / `subscribeChannel`.
 * The label flips immediately (that is what the user just asked for); it resets
 * on restart, exactly like the song library flag in [SongActions].
 */
object DetailActions {
    private val likedCollections = mutableStateMapOf<String, Boolean>()
    private val subscribedArtists = mutableStateMapOf<String, Boolean>()

    fun isCollectionLiked(id: String): Boolean = likedCollections[id] == true

    /** Flips and returns the new liked state of an album/playlist id. */
    fun toggleCollectionLiked(id: String): Boolean =
        (!isCollectionLiked(id)).also { likedCollections[id] = it }

    fun isArtistSubscribed(id: String): Boolean = subscribedArtists[id] == true

    /** Flips and returns the new subscribed state of an artist id. */
    fun toggleArtistSubscribed(id: String): Boolean =
        (!isArtistSubscribed(id)).also { subscribedArtists[id] = it }
}
