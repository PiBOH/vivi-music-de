package com.music.vivi.desktop

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.automirrored.filled.QueueMusic
import androidx.compose.material.icons.filled.PlaylistAdd
import androidx.compose.material.icons.filled.QueuePlayNext
import androidx.compose.material.icons.filled.Radio
import androidx.compose.material3.Button
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
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import com.music.innertube.YouTube
import com.music.innertube.models.WatchEndpoint
import com.music.innertube.pages.MoodAndGenres
import com.music.innertube.models.SongItem
import kotlinx.coroutines.launch
import sh.calvin.reorderable.ReorderableItem
import sh.calvin.reorderable.rememberReorderableLazyListState

/**
 * "Queue" in BitChord's own shape: the track that is playing, then the tracks
 * that come next, each row carrying the drag handle, the context menu and the
 * remove control of the reference. The list is the player's real queue: a drag
 * commits through [onReorder], the menu runs the same calls the rest of the app
 * uses, and nothing here is decorative.
 *
 * Ported from BitChord's queue panel (GPLv3).
 */
@Composable
fun BitChordQueueScreen(
    queue: List<NowPlaying>,
    index: Int,
    language: String,
    onBack: () -> Unit,
    onSkipTo: (Int) -> Unit,
    onRemoveAt: (Int) -> Unit,
    onClear: () -> Unit,
    onReorder: (List<NowPlaying>) -> Unit,
    onAddToPlaylist: (NowPlaying) -> Unit,
    onPlayNext: (Int) -> Unit,
    onAddToQueue: (NowPlaying) -> Unit,
    onPlayRelated: (SongItem) -> Unit = {},
) {
    val contentColor = MaterialTheme.colorScheme.onSurface
    val mutedColor = MaterialTheme.colorScheme.onSurfaceVariant
    val current = queue.getOrNull(index)
    val upcoming = queue.drop(index + 1)
    // Discovery: the Related tab of the track that is playing, which is the
    // chain the Home "Recommended" row uses (`YouTube.next(videoId)` for the
    // related endpoint, then `YouTube.related(endpoint)` for the songs). It is
    // a real browse of the service, not a list built here.
    var discoverySeed by remember { mutableStateOf<NowPlaying?>(null) }
    var discovery by remember { mutableStateOf<List<SongItem>?>(null) }
    var discoveryError by remember { mutableStateOf<String?>(null) }
    LaunchedEffect(discoverySeed) {
        val seed = discoverySeed ?: return@LaunchedEffect
        discovery = null
        discoveryError = null
        // Both halves of the recipe the player itself uses: the Related tab of
        // the seed, then the automix/"up next" queue for the same video, which
        // is served for the tracks that have no Related tab at all.
        val next = YouTube.next(WatchEndpoint(videoId = seed.videoId)).getOrNull()
        if (next == null) {
            discoveryError = "the service returned no page for this track"
            return@LaunchedEffect
        }
        val relatedSongs = next.relatedEndpoint
            ?.let { endpoint -> YouTube.related(endpoint).getOrNull()?.songs }
            .orEmpty()
        val songs = (relatedSongs + next.items)
            .filter { it.id != seed.videoId }
            .distinctBy { it.id }
        if (songs.isEmpty()) {
            discoveryError = "the service returned no related songs for this track"
        } else {
            discovery = songs
        }
    }

    val listState = rememberLazyListState()
    val local = remember { mutableStateListOf<NowPlaying>() }
    var dragged by remember { mutableStateOf(false) }
    val reorderState = rememberReorderableLazyListState(listState) { from, to ->
        local.add(to.index, local.removeAt(from.index))
        dragged = true
    }
    LaunchedEffect(upcoming) {
        if (!reorderState.isAnyItemDragging) {
            local.clear()
            local.addAll(upcoming)
        }
    }
    LaunchedEffect(reorderState.isAnyItemDragging) {
        if (!reorderState.isAnyItemDragging && dragged) {
            val head = queue.take(index + 1)
            onReorder(head + local.toList())
            dragged = false
        }
    }

    // Computed in the composable body: `remember` is not available inside the
    // LazyColumn scope, and a queue may hold the same track twice, so the keys
    // come from `withOccurrences` rather than from the video id alone.
    val localSnapshot = local.toList()
    val keys = remember(localSnapshot) { localSnapshot.map { it.videoId }.withOccurrences() }

    // Its own sub-page: opened from the discovery row, closed with Back, with the
    // songs of the related page as rows that play on click.
    if (discoverySeed != null) {
        Column(Modifier.fillMaxSize().padding(16.dp)) {
            BackButton(language) { discoverySeed = null }
            Text(
                "Discover",
                style = MaterialTheme.typography.headlineMedium,
                color = contentColor,
            )
            Text(
                discoverySeed?.title.orEmpty(),
                style = MaterialTheme.typography.bodyMedium,
                color = mutedColor,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.padding(top = 2.dp, bottom = 10.dp),
            )
            when {
                discoveryError != null -> ErrorBox(language, discoveryError)
                discovery == null -> LoadingBox(language)
                discovery!!.isEmpty() -> Text(
                    Localization.get(language, "charts_empty"),
                    style = MaterialTheme.typography.bodyMedium,
                    color = mutedColor,
                )
                else -> LazyColumn(Modifier.fillMaxSize()) {
                    items(discovery!!, key = { it.id }) { song ->
                        SongRow(
                            song,
                            language,
                            { onPlayRelated(song) },
                            onAddToQueue = {},
                            onAddToPlaylist = {},
                        )
                    }
                }
            }
        }
        return
    }

    Column(Modifier.fillMaxSize().padding(16.dp)) {
        BackButton(language, onBack)
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Text(
                Localization.get(language, "queue"),
                style = MaterialTheme.typography.headlineMedium,
                color = contentColor,
                modifier = Modifier.weight(1f),
            )
            if (queue.isNotEmpty()) {
                Button(onClick = onClear, enabled = !transportLocked()) {
                    Text(Localization.get(language, "clear_queue"))
                }
            }
        }

        if (queue.isEmpty()) {
            Text(
                Localization.get(language, "queue_empty"),
                style = MaterialTheme.typography.bodyLarge,
                color = mutedColor,
                modifier = Modifier.padding(top = 16.dp),
            )
        } else {
            Text(
                Localization.get(language, "drag_to_reorder"),
                style = MaterialTheme.typography.bodySmall,
                color = mutedColor,
                modifier = Modifier.padding(top = 4.dp),
            )
            LazyColumn(state = listState, modifier = Modifier.fillMaxSize().padding(top = 12.dp)) {
                if (current != null) {
                    item(key = "section-now") {
                        SectionLabel(Localization.get(language, "now_playing"))
                    }
                    item(key = "now") {
                        BitChordQueueRow(
                            song = current,
                            language = language,
                            isCurrent = true,
                            onOpen = {},
                            onRemove = { onRemoveAt(index) },
                            onAddToPlaylist = { onAddToPlaylist(current) },
                            onPlayNext = { onPlayNext(index) },
                            onAddToQueue = { onAddToQueue(current) },
                            dragHandle = null,
                        )
                    }
                    item(key = "divider") {
                        HorizontalDivider(
                            color = MaterialTheme.colorScheme.outlineVariant,
                            modifier = Modifier.padding(vertical = 10.dp),
                        )
                    }
                }
                item(key = "section-next") {
                    SectionLabel(
                        Localization.get(language, "up_next") + ": " + upcoming.size,
                    )
                }
                if (current != null) {
                    item(key = "discovery") {
                        HorizontalDivider(
                            color = MaterialTheme.colorScheme.outlineVariant,
                            modifier = Modifier.padding(vertical = 10.dp),
                        )
                        Surface(
                            onClick = { discoverySeed = current },
                            shape = MaterialTheme.shapes.medium,
                            color = MaterialTheme.colorScheme.surfaceContainerHigh,
                            modifier = Modifier.fillMaxWidth(),
                        ) {
                            Row(
                                Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 12.dp),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Icon(
                                    Icons.Filled.Radio,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(20.dp),
                                )
                                Spacer(Modifier.width(10.dp))
                                Column(Modifier.weight(1f)) {
                                    Text(
                                        "Discover",
                                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold),
                                        color = contentColor,
                                    )
                                    Text(
                                        "Related to ${current.title}",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = mutedColor,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                    )
                                }
                                Text(
                                    "\u203A",
                                    style = MaterialTheme.typography.titleLarge,
                                    color = mutedColor,
                                )
                            }
                        }
                    }
                }
                itemsIndexed(local, key = { i, song -> song.videoId + "#" + i }) { i, song ->
                    ReorderableItem(state = reorderState, key = song.videoId) {
                        BitChordQueueRow(
                            song = song,
                            language = language,
                            isCurrent = false,
                            onOpen = { onSkipTo(index + 1 + i) },
                            onRemove = { onRemoveAt(index + 1 + i) },
                            onAddToPlaylist = { onAddToPlaylist(song) },
                            onPlayNext = { onPlayNext(index + 1 + i) },
                            onAddToQueue = { onAddToQueue(song) },
                            dragHandle = Modifier.draggableHandle(),
                        )
                    }
                }
                item(key = "tail") { Spacer(Modifier.height(16.dp)) }
            }
        }
    }
}

@Composable
private fun SectionLabel(text: String) {
    Text(
        text,
        style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.SemiBold),
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.padding(vertical = 4.dp),
    )
}

/**
 * One row of the queue, in the reference's shape: drag handle, cover, title and
 * artist, the reference's menu, and the remove control. Every entry of the menu
 * is wired to a real call; nothing is listed that the app cannot do.
 */
@Composable
private fun BitChordQueueRow(
    song: NowPlaying,
    language: String,
    isCurrent: Boolean,
    onOpen: () -> Unit,
    onRemove: () -> Unit,
    onAddToPlaylist: () -> Unit,
    onPlayNext: () -> Unit,
    onAddToQueue: () -> Unit,
    dragHandle: Modifier?,
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
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (dragHandle != null) {
            Text(
                "\u283F",
                style = MaterialTheme.typography.titleMedium,
                color = mutedColor,
                modifier = dragHandle.padding(horizontal = 8.dp),
            )
        } else {
            Spacer(Modifier.width(34.dp))
        }
        Box(
            Modifier
                .size(44.dp)
                .clip(MaterialTheme.shapes.extraSmall),
        ) {
            AsyncImage(
                model = adjustedThumbnailUrl(song.thumbnail, 200, DesktopSettings.load().dataSaver),
                contentDescription = song.title,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize(),
            )
            if (isCurrent) {
                Box(
                    Modifier
                        .align(Alignment.Center)
                        .clip(MaterialTheme.shapes.extraSmall)
                        .padding(2.dp),
                ) {
                    Icon(
                        Icons.Filled.GraphicEq,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onPrimary,
                        modifier = Modifier.size(18.dp),
                    )
                }
            }
        }
        Spacer(Modifier.width(12.dp))
        Column(
            Modifier
                .weight(1f)
                .padding(vertical = 4.dp),
        ) {
            Tooltip(song.title) {
                Text(
                    song.title,
                    style = MaterialTheme.typography.bodyLarge.copy(
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
        Box {
            Tooltip(Localization.get(language, "more")) {
                M3IconButton(onClick = { menuOpen = true }) {
                    Icon(
                        Icons.Filled.MoreVert,
                        contentDescription = Localization.get(language, "more"),
                        tint = mutedColor,
                        modifier = Modifier.size(18.dp),
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
                DropdownMenuItem(
                    text = { Text(Localization.get(language, "add_to_playlist")) },
                    leadingIcon = { Icon(Icons.Filled.PlaylistAdd, contentDescription = null) },
                    onClick = {
                        menuOpen = false
                        onAddToPlaylist()
                    },
                )
                DropdownMenuItem(
                    text = { Text("Play next") },
                    leadingIcon = { Icon(Icons.Filled.QueuePlayNext, contentDescription = null) },
                    onClick = {
                        menuOpen = false
                        onPlayNext()
                    },
                )
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
        if (isCurrent) {
            Icon(
                Icons.Filled.GraphicEq,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(start = 8.dp).size(18.dp),
            )
        }
        Tooltip(Localization.get(language, "close")) {
            M3IconButton(onClick = onRemove) {
                Icon(
                    Icons.Filled.Close,
                    contentDescription = Localization.get(language, "close"),
                    tint = mutedColor,
                    modifier = Modifier.size(18.dp),
                )
            }
        }
    }
}

/**
 * "Radio" in BitChord's shape: the stations as a grid of large cards, each one a
 * real browse page of its own (the mood/genre page YouTube serves), so the card
 * is a door and not a picture. The old charts list is gone from this entry.
 *
 * Ported from BitChord's radio/explore cards (GPLv3).
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun BitChordRadioScreen(
    language: String,
    onOpenBrowse: (String, String?) -> Unit,
) {
    var sections by remember { mutableStateOf<List<MoodAndGenres>?>(null) }
    var error by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(Unit) {
        YouTube.moodAndGenres().fold(
            onSuccess = { sections = it },
            onFailure = { error = it.message },
        )
    }

    // Every card is a filled tone of the theme, like the reference: the four
    // containers cycle with the item so no card is the plain surface colour.
    val cardColors = listOf(
        MaterialTheme.colorScheme.primaryContainer,
        MaterialTheme.colorScheme.secondaryContainer,
        MaterialTheme.colorScheme.tertiaryContainer,
        MaterialTheme.colorScheme.errorContainer,
    )
    val cardInk = listOf(
        MaterialTheme.colorScheme.onPrimaryContainer,
        MaterialTheme.colorScheme.onSecondaryContainer,
        MaterialTheme.colorScheme.onTertiaryContainer,
        MaterialTheme.colorScheme.onErrorContainer,
    )

    Column(Modifier.fillMaxSize().padding(16.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                Icons.Filled.Radio,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(26.dp),
            )
            Spacer(Modifier.width(10.dp))
            Text(
                "Radio",
                style = MaterialTheme.typography.headlineMedium,
                modifier = Modifier.weight(1f),
            )
        }
        Spacer(Modifier.height(12.dp))
        when {
            error != null -> ErrorBox(language, error)
            sections == null -> LoadingBox(language)
            else -> LazyColumn(Modifier.fillMaxSize()) {
                sections!!.forEachIndexed { sectionIndex, section ->
                    item(key = "header-${section.title}") {
                        Text(
                            section.title,
                            style = MaterialTheme.typography.titleLarge,
                            modifier = Modifier.padding(top = if (sectionIndex == 0) 0.dp else 20.dp, bottom = 10.dp),
                        )
                    }
                    item(key = "grid-${section.title}") {
                        // A FlowRow, not a LazyVerticalGrid: a lazily scrollable
                        // grid measured inside the LazyColumn has no bounded
                        // height and throws, and the screen stayed on the frame
                        // before it.
                        val distinct = section.items.distinctBy { it.endpoint.browseId + it.title }
                        FlowRow(
                            Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp),
                        ) {
                            distinct.forEach { item ->
                                val tone = (item.title.length + sectionIndex) % cardColors.size
                                Surface(
                                    onClick = { onOpenBrowse(item.endpoint.browseId, item.endpoint.params) },
                                    shape = MaterialTheme.shapes.medium,
                                    color = cardColors[tone],
                                    modifier = Modifier
                                        .width(220.dp)
                                        .height(120.dp),
                                ) {
                                    Column(
                                        Modifier.fillMaxSize().padding(14.dp),
                                        verticalArrangement = Arrangement.SpaceBetween,
                                    ) {
                                        Text(
                                            item.title,
                                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                            color = cardInk[tone],
                                            maxLines = 2,
                                            overflow = TextOverflow.Ellipsis,
                                        )
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Icon(
                                                Icons.Filled.PlayArrow,
                                                contentDescription = null,
                                                tint = cardInk[tone],
                                                modifier = Modifier.size(18.dp),
                                            )
                                            Spacer(Modifier.width(6.dp))
                                            Text(
                                                "Station",
                                                style = MaterialTheme.typography.labelMedium,
                                                color = cardInk[tone],
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
                item(key = "tail") { Spacer(Modifier.height(16.dp)) }
            }
        }
    }
}
