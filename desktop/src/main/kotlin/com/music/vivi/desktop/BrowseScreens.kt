package com.music.vivi.desktop

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Shuffle
import androidx.compose.material3.Icon
import androidx.compose.ui.Alignment
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items as gridItems
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.clickable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.ui.graphics.Color
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.snap
import androidx.compose.animation.core.tween
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.music.innertube.YouTube
import com.music.innertube.models.Artist
import com.music.innertube.models.SongItem
import com.music.innertube.models.WatchEndpoint
import com.music.innertube.models.YTItem
import com.music.innertube.pages.BrowseResult
import com.music.innertube.pages.HomePage
import com.music.innertube.pages.MoodAndGenres
import com.music.innertube.pages.SearchResult
import kotlinx.coroutines.delay
import com.music.innertube.pages.SearchSummary
import com.music.innertube.pages.SearchSummaryPage

import androidx.compose.foundation.shape.CircleShape
import androidx.compose.ui.graphics.Brush
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material3.IconButtonDefaults
import java.time.LocalTime

data class CuratedMix(
    val title: String,
    val subtitle: String,
    val gradientColors: List<Color>,
)

@Composable
fun HomeScreen(
    language: String,
    onOpenAlbum: (String) -> Unit,
    onOpenArtist: (String) -> Unit,
    onOpenPlaylist: (String) -> Unit,
    onPlaySong: (SongItem) -> Unit,
    onAddToPlaylist: (SongItem) -> Unit,
    onPlayAll: (List<SongItem>) -> Unit,
    onOpenBrowse: (String, String?) -> Unit,
    userName: String = "Guest",
    useLastListen: Boolean = false,
    onUseLastListenChange: (Boolean) -> Unit = {},
    randomizeOrder: Boolean = false,
    onRandomizeOrderChange: (Boolean) -> Unit = {},
    wrappedStats: WrappedStats = WrappedStats(),
    showWrapped: Boolean = false,
    /** Recently started tracks (newest first) used as recommendation seeds. */
    recentSeedTracks: List<NowPlaying> = emptyList(),
) {
    var home by remember { mutableStateOf<HomePage?>(null) }
    var moodAndGenres by remember { mutableStateOf<List<MoodAndGenres>?>(null) }
    var error by remember { mutableStateOf<String?>(null) }
    var selectedChip by remember { mutableStateOf<HomePage.Chip?>(null) }
    // Retry support: bump [loadRequest] to refetch. Also retries once when the
    // response is an empty shell (YouTube sometimes returns the shell before
    // streaming the real carousels), so the Home can't be left permanently empty.
    var loadRequest by remember { mutableStateOf(0) }
    var autoRetried by remember { mutableStateOf(false) }

    // Recommended songs, ported from the mobile app's HomeViewModel:
    // for each recent seed track ask `YouTube.next(videoId)` for its related
    // endpoint, then `YouTube.related(endpoint)` and surface the songs.
    var recommendations by remember { mutableStateOf<List<SongItem>?>(null) }
    // Seeds come from the most recent in-session tracks. On a fresh profile
    // there is no history yet, so the first songs that came back in the Home
    // feed are used instead — the "Recommended" row must never silently
    // disappear just because nothing was played in this session.
    LaunchedEffect(
        recentSeedTracks.map { it.videoId }.take(3).joinToString(","),
        home,
    ) {
        var seeds = recentSeedTracks.take(3).map { it.videoId }
        if (seeds.isEmpty() && home != null) {
            val homeSongs: List<SongItem> =
                home!!.sections.flatMap { it.items }.filterIsInstance<SongItem>()
            seeds = homeSongs.map { it.id }.distinct().take(3)
        }
        if (seeds.isEmpty()) {
            recommendations = null
            return@LaunchedEffect
        }
        val collected = mutableListOf<SongItem>()
        for (videoId in seeds) {
            val endpoint = YouTube.next(WatchEndpoint(videoId = videoId))
                .getOrNull()?.relatedEndpoint ?: continue
            val page = YouTube.related(endpoint).getOrNull() ?: continue
            collected += page.songs.filter { it.id != videoId }
            if (collected.size >= 12) break
        }
        recommendations = collected.distinctBy { it.id }.take(12).ifEmpty { null }
    }

    LaunchedEffect(loadRequest, selectedChip) {
        val params = selectedChip?.endpoint?.params
        error = null
        home = null
        YouTube.home(params = params).fold(
            onSuccess = { home = it.filteredContent(); error = null },
            onFailure = { error = it.message },
        )
    }

    // One automatic retry when the response is empty (rare YouTube layout
    // changes return an empty shell for the first request).
    LaunchedEffect(home) {
        if (home != null && home!!.sections.isEmpty() && !autoRetried) {
            autoRetried = true
            delay(1200)
            loadRequest++
        }
    }

    LaunchedEffect(Unit) {
        YouTube.moodAndGenres().fold(
            onSuccess = { moodAndGenres = it },
            onFailure = { /* mood & genres is optional */ },
        )
    }

    val greetingText = remember(language) {
        val hour = LocalTime.now().hour
        when {
            hour < 12 -> Localization.get(language, "home_greeting_morning")
            hour < 17 -> Localization.get(language, "home_greeting_afternoon")
            else -> Localization.get(language, "home_greeting_evening")
        }
    }

    // Expressive Spotify/Apple-style mix cards, but derived from the theme's
    // accent (hue rotations of it) so they stay inside the Material palette.
    val accent = MaterialTheme.colorScheme.primary
    val curatedMixes = remember(accent) {
        listOf(
            CuratedMix("Get Up! Mix", "Upbeat Essentials", listOf(accent, rotateHue(accent, 40f))),
            CuratedMix("Chill Mix", "Ambient & Relaxing", listOf(rotateHue(accent, 160f), rotateHue(accent, 200f))),
            CuratedMix("New Music Mix", "Fresh Releases", listOf(rotateHue(accent, 280f), rotateHue(accent, 320f))),
            CuratedMix("Heavy Rotation Mix", "Most Played", listOf(rotateHue(accent, 60f), rotateHue(accent, 100f))),
            CuratedMix("Focus Mix", "Deep Concentration", listOf(rotateHue(accent, 220f), rotateHue(accent, 260f))),
        )
    }

    when {
        error != null && home == null -> ErrorBox(language, error)
        home == null -> LoadingBox(language)
        // Empty response (even after the automatic retry): show a clear state
        // with a manual Retry instead of a silently blank Home.
        home!!.sections.isEmpty() -> Column(
            Modifier.fillMaxSize().padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            Text(
                Localization.get(language, "home_empty"),
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(Modifier.height(16.dp))
            Button(onClick = {
                AppLog.click("Home retry")
                autoRetried = true
                loadRequest++
            }) {
                Text(Localization.get(language, "retry"))
            }
        }
        else -> LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.surfaceContainer),
            contentPadding = PaddingValues(top = 16.dp, bottom = 32.dp),
            verticalArrangement = Arrangement.spacedBy(26.dp),
        ) {
            // BitChord's Home is a stack of shelves, not a dashboard: Recents
            // first, then the service's own shelves. The first shelf is the
            // hero row (one large card and the next one peeking), every other
            // shelf is the 150 dp card row, and each heading is the shelf's
            // own title. No greeting, chips or mixes sit above them.
            val page = home ?: return@LazyColumn

            val recents = recentSeedTracks.take(8)
            if (recents.isNotEmpty()) {
                item(key = "recents_header") { ShelfHeading("Recents") }
                item(key = "recents_row") {
                    HorizontalCarousel(
                        language = language,
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        contentPadding = PaddingValues(horizontal = 24.dp),
                    ) {
                        items(recents, key = { "recent-${it.videoId}" }) { track ->
                            Box(Modifier.width(340.dp)) {
                                SongRow(
                                    song = SongItem(
                                        id = track.videoId,
                                        title = track.title,
                                        artists = listOf(Artist(name = track.artist, id = null)),
                                        thumbnail = track.thumbnail.orEmpty(),
                                    ),
                                    language = language,
                                    onClick = {
                                        AppLog.click("Home recent '${track.title}'")
                                        onPlaySong(
                                            SongItem(
                                                id = track.videoId,
                                                title = track.title,
                                                artists = listOf(Artist(name = track.artist, id = null)),
                                                thumbnail = track.thumbnail.orEmpty(),
                                            )
                                        )
                                    },
                                    onAddToPlaylist = {
                                        onAddToPlaylist(
                                            SongItem(
                                                id = track.videoId,
                                                title = track.title,
                                                artists = listOf(Artist(name = track.artist, id = null)),
                                                thumbnail = track.thumbnail.orEmpty(),
                                            )
                                        )
                                    },
                                )
                            }
                        }
                    }
                }
            }

            page.sections.filter { it.items.isNotEmpty() }.forEachIndexed { index, section ->
                val hero = index == 0
                item(key = "shelf-header-$index-${section.title}") {
                    ShelfHeading(section.title)
                }
                item(key = "shelf-$index-${section.title}") {
                    val songs = section.items.filterIsInstance<SongItem>()
                    if (songs.size == section.items.size) {
                        HorizontalCarousel(
                            language = language,
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                            contentPadding = PaddingValues(horizontal = 24.dp),
                        ) {
                            val sectionSongs = songs.distinctBy { it.id }
                            items(sectionSongs, key = { it.id }) { song ->
                                Box(Modifier.width(if (hero) 400.dp else 340.dp)) {
                                    SongRow(
                                        song = song,
                                        language = language,
                                        // Tapping a song plays the whole section
                                        // from that track, as the mobile app does.
                                        onClick = {
                                            val start = sectionSongs.indexOfFirst { it.id == song.id }.coerceAtLeast(0)
                                            val rotated = sectionSongs.slice(start until sectionSongs.size) +
                                                sectionSongs.slice(0 until start)
                                            onPlayAll(rotated)
                                        },
                                        onAddToPlaylist = { onAddToPlaylist(song) },
                                    )
                                }
                            }
                        }
                    } else {
                        HorizontalCarousel(
                            language = language,
                            horizontalArrangement = Arrangement.spacedBy(14.dp),
                            contentPadding = PaddingValues(horizontal = 24.dp),
                        ) {
                            items(section.items, key = { it.id }) { item ->
                                YtItemCard(
                                    item = item,
                                    width = if (hero) 320.dp else 150.dp,
                                    onClick = {
                                        onItemClick(item, onOpenAlbum, onOpenArtist, onOpenPlaylist, onPlaySong)
                                    },
                                )
                            }
                        }
                    }
                }
            }

            item(key = "bottom_spacer") {
                Box(Modifier.fillMaxWidth().padding(bottom = 24.dp))
            }
        }
    }
}

/** A shelf's heading, at BitChord's `headlineMedium` weight and gutter. */
@Composable
private fun ShelfHeading(title: String) {
    Text(
        text = title,
        style = MaterialTheme.typography.headlineMedium,
        color = MaterialTheme.colorScheme.onBackground,
        maxLines = 2,
        overflow = TextOverflow.Ellipsis,
        modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp),
    )
}

/**
 * "VIVI Wrapped" card showing the current session's listening stats.
 *
 * [modifier] carries the card's own inset, which is Home's list: the settings
 * screen draws the same card flush with its other blocks and passes its own.
 * [showTitle] drops the card's heading for a caller that already has that
 * heading above it (the settings screen's title is the same sentence).
 */
@Composable
fun WrappedCard(
    wrappedStats: WrappedStats,
    language: String,
    modifier: Modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
    showTitle: Boolean = true,
) {
    val minutes = wrappedStats.playedMs / 60_000
    val timeText = if (minutes >= 60) {
        "${minutes / 60}h ${minutes % 60}m"
    } else {
        "${minutes}m"
    }
    val accent = MaterialTheme.colorScheme.primary
    Card(
        shape = RoundedCornerShape(topStart = ShapeRadii.extraLarge, topEnd = ShapeRadii.small, bottomEnd = ShapeRadii.extraLarge, bottomStart = ShapeRadii.small),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
        modifier = modifier,
    ) {
        Column(Modifier.padding(16.dp)) {
            if (showTitle) {
                Text(
                    Localization.get(language, "wrapped_title"),
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                )
                Spacer(Modifier.height(8.dp))
            }
            Row(Modifier.fillMaxWidth()) {
                WrappedStat(
                    value = wrappedStats.trackStarts.toString(),
                    label = Localization.get(language, "wrapped_tracks"),
                    accent = accent,
                    modifier = Modifier.weight(1f),
                )
                WrappedStat(
                    value = timeText,
                    label = Localization.get(language, "wrapped_listening_time"),
                    accent = accent,
                    modifier = Modifier.weight(1f),
                )
                WrappedStat(
                    value = wrappedStats.topSongCount.toString(),
                    label = wrappedStats.topSongTitle?.take(18) ?: Localization.get(language, "wrapped_top_song"),
                    accent = accent,
                    modifier = Modifier.weight(1f),
                )
            }
        }
    }
}

@Composable
private fun WrappedStat(value: String, label: String, accent: Color, modifier: Modifier = Modifier) {
    Column(modifier, horizontalAlignment = Alignment.CenterHorizontally) {
        Text(value, style = MaterialTheme.typography.headlineSmall, color = accent)
        Tooltip(label) {
            Text(
                label,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onPrimaryContainer,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

/** Generic browse screen (mood/genre pages, "More" endpoints, etc.). */
@Composable
fun BrowseScreen(
    browseId: String,
    params: String?,
    language: String,
    gridItemSize: Int,
    onBack: () -> Unit,
    onOpenAlbum: (String) -> Unit,
    onOpenArtist: (String) -> Unit,
    onOpenPlaylist: (String) -> Unit,
    onPlaySong: (SongItem) -> Unit,
) {
    var result by remember { mutableStateOf<BrowseResult?>(null) }
    var error by remember { mutableStateOf<String?>(null) }
    // Manual retry: a page that loads with nothing in it (or a transient
    // failure) used to leave a blank screen with no way to reload it.
    var reloadKey by remember { mutableStateOf(0) }

    LaunchedEffect(browseId, params, reloadKey) {
        // A library page ("FEmusic_library_*") has a different shape from a
        // browse page: YouTube.browse only maps the two-row cards of a browse
        // page, while a library page carries the grid / musicShelf renderers
        // that YouTube.library reads. The Artists screen — and the "See all"
        // link that opens it — asked for the library page and got an empty
        // result, so it is routed through the same loader the Library screen
        // uses ([loadLibraryPage], which also logs the page shape and falls
        // back to the artists of the account's songs).
        val fetched = if (browseId.startsWith("FEmusic_library")) {
            loadLibraryPage(browseId).map { page ->
                BrowseResult(
                    title = null,
                    items = listOf(BrowseResult.Item(title = null, items = page.items)),
                )
            }
        } else {
            YouTube.browse(browseId, params).map { it.filteredContent() }
        }
        fetched.fold(
            onSuccess = { page ->
                result = page
                error = null
                // A browse that succeeds with nothing to show is exactly what
                // "the screen is empty" looks like in a support zip: record
                // what came back, so the next report says whether the page was
                // empty or the parser dropped it.
                runCatching {
                    AppLog.log(
                        "browse",
                        "browse ok $browseId → ${page.items.size} section(s), " +
                            "${page.items.sumOf { it.items.size }} item(s)" +
                            if (page.items.all { it.items.isEmpty() }) " (empty page)" else "",
                    )
                }
            },
            onFailure = { t ->
                val raw = t.message.orEmpty()
                // The status is what the error code means: a 401 really is a
                // missing/expired session (E1031), while a 400 INVALID_ARGUMENT
                // is the server rejecting the request itself (E1032, e.g. a
                // browse page opened without the params it needs). Labelling
                // every failure E1031 sent users to re-sign-in for a request
                // problem — the log line below used to hardcode that code.
                val code = when {
                    raw.contains("401") && raw.contains("UNAUTHENTICATED", ignoreCase = true) -> "E1031"
                    raw.contains("400") && raw.contains("INVALID_ARGUMENT", ignoreCase = true) -> "E1032"
                    else -> null
                }
                // Visible to the user (covers the unreadable 401/400 JSON).
                error = if (code != null) "$code $raw" else raw.ifBlank { t.javaClass.simpleName }
                // Always land in the session logs — the support zip was missing
                // this failure completely, leaving nothing to debug.
                runCatching {
                    AppLog.log(
                        "browse",
                        "browse failed ${code ?: "-"} — browseId=$browseId params=${params?.take(64)} loggedIn=${LoginManager.isLoggedIn()} cookie=${YouTube.cookie?.length ?: 0}b visitorData=${!YouTube.visitorData.isNullOrBlank()} dataSyncId=${!YouTube.dataSyncId.isNullOrBlank()} — $raw — ${t.javaClass.name}"
                    )
                }
            },
        )
    }

    Column(Modifier.fillMaxSize().padding(16.dp)) {
        when {
            error != null -> ErrorBox(language, error)
            result == null -> LoadingBox(language)
            // A page that loaded but carries nothing gets a message and a
            // retry instead of a blank screen (the Artists page did exactly
            // that: the request succeeded, the account had no saved artists,
            // and the screen showed nothing at all).
            result!!.items.all { it.items.isEmpty() } -> EmptyBox(language) { reloadKey++ }
            else -> LazyVerticalGrid(
                columns = GridCells.Adaptive(gridItemSize.dp),
                contentPadding = PaddingValues(16.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                result!!.items.forEachIndexed { secIdx, section ->
                    if (!section.title.isNullOrBlank()) {
                        item(key = "header-$secIdx-${section.title}", span = { GridItemSpan(maxLineSpan) }) {
                            SectionHeader(title = section.title!!, language = language)
                        }
                    }
                    section.items.forEachIndexed { itemIdx, ytItem ->
                        item(key = "browse-$secIdx-$itemIdx-${ytItem.id}") {
                            YtItemCard(
                                item = ytItem,
                                width = null,
                                onClick = { onItemClick(ytItem, onOpenAlbum, onOpenArtist, onOpenPlaylist, onPlaySong) },
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun SearchScreen(
    language: String,
    gridItemSize: Int,
    onOpenAlbum: (String) -> Unit,
    onOpenArtist: (String) -> Unit,
    onOpenPlaylist: (String) -> Unit,
    onPlaySong: (SongItem) -> Unit,
    onAddToQueue: (SongItem) -> Unit,
    onAddToPlaylist: (SongItem) -> Unit,
    searchHistory: List<String> = emptyList(),
    onRecordSearch: (String) -> Unit = {},
    onClearSearchHistory: () -> Unit = {},
    externalQuery: String? = null,
    onQueryChange: ((String) -> Unit)? = null,
    showTextField: Boolean = false,
    externalFilter: YouTube.SearchFilter? = null,
    onFilterChange: ((YouTube.SearchFilter?) -> Unit)? = null,
    showFiltersInBody: Boolean = false,
) {
    var internalQuery by remember { mutableStateOf("") }
    val query = externalQuery ?: internalQuery
    val updateQuery: (String) -> Unit = { newQ ->
        onQueryChange?.invoke(newQ)
        if (externalQuery == null) {
            internalQuery = newQ
        }
    }

    var page by remember { mutableStateOf<SearchSummaryPage?>(null) }
    var filterItems by remember { mutableStateOf<List<YTItem>?>(null) }
    var internalFilter by remember { mutableStateOf<YouTube.SearchFilter?>(null) }
    val selectedFilter = externalFilter ?: internalFilter
    val setSelectedFilter: (YouTube.SearchFilter?) -> Unit = { f ->
        onFilterChange?.invoke(f)
        if (externalFilter == null) {
            internalFilter = f
        }
    }

    var suggestions by remember { mutableStateOf<List<String>>(emptyList()) }
    var focused by remember { mutableStateOf(false) }
    var loading by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }

    // Results: summary when "All", filtered results when a chip is selected.
    LaunchedEffect(query, selectedFilter) {
        val q = query.trim()
        if (q.isEmpty()) {
            page = null
            filterItems = null
            error = null
            loading = false
            return@LaunchedEffect
        }
        loading = true
        val filter = selectedFilter
        if (filter == null) {
            YouTube.searchSummary(q).fold(
                onSuccess = { page = it.filteredContent(); filterItems = null; error = null },
                onFailure = { error = it.message },
            )
        } else {
            YouTube.search(q, filter).fold(
                onSuccess = { page = null; filterItems = it.items.filteredContent().distinctBy { item -> item.id }; error = null },
                onFailure = { error = it.message },
            )
        }
        loading = false
    }

    // Live suggestions while typing (hidden once results are shown).
    LaunchedEffect(query, focused) {
        val q = query.trim()
        if (!focused || q.isEmpty() || page != null || filterItems != null) {
            suggestions = emptyList()
            return@LaunchedEffect
        }
        suggestions = YouTube.searchSuggestions(q).getOrNull()?.queries.orEmpty().take(6)
    }

    Column(Modifier.fillMaxSize().padding(16.dp)) {
        if (showTextField) {
            OutlinedTextField(
                value = query,
                onValueChange = updateQuery,
                modifier = Modifier.fillMaxWidth().onFocusChanged { focused = it.isFocused },
                singleLine = true,
                placeholder = { Text(Localization.get(language, "search_placeholder")) },
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                keyboardActions = KeyboardActions(onSearch = {
                    val q = query.trim()
                    if (q.isNotEmpty()) onRecordSearch(q)
                }),
            )
        }

        // Filter chips (All / Songs / Videos / Albums / Artists / Playlists).
        if (showFiltersInBody) {
            val filters = listOf(
                null to Localization.get(language, "filter_all"),
                YouTube.SearchFilter.FILTER_SONG to Localization.get(language, "filter_songs"),
                YouTube.SearchFilter.FILTER_VIDEO to Localization.get(language, "filter_videos"),
                YouTube.SearchFilter.FILTER_ALBUM to Localization.get(language, "filter_albums"),
                YouTube.SearchFilter.FILTER_ARTIST to Localization.get(language, "filter_artists"),
                YouTube.SearchFilter.FILTER_FEATURED_PLAYLIST to Localization.get(language, "filter_playlists"),
            )
            HorizontalCarousel(
                language = language,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                contentPadding = PaddingValues(vertical = 8.dp),
            ) {
                items(filters.size) { i ->
                    val (filter, label) = filters[i]
                    val selected = selectedFilter == filter
                    FilterChip(
                        selected = selected,
                        onClick = { setSelectedFilter(if (selected) null else filter) },
                        label = { Text(label) },
                    )
                }
            }
        }

        // Recent searches (saved locally, shown when the query is empty).
        if (query.isBlank() && searchHistory.isNotEmpty()) {
            Row(
                Modifier.fillMaxWidth().padding(top = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    Localization.get(language, "search_history"),
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(Modifier.weight(1f))
                TextButton(onClick = onClearSearchHistory) {
                    Text(Localization.get(language, "clear_search_history"))
                }
            }
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                searchHistory.take(12).forEach { term ->
                    FilterChip(
                        selected = false,
                        onClick = { updateQuery(term); onRecordSearch(term) },
                        label = { Text(term) },
                    )
                }
            }
        }

        // Suggestion list.
        if (focused && query.isNotBlank() && suggestions.isNotEmpty() && page == null && filterItems == null) {
            Surface(
                tonalElevation = 3.dp,
                shadowElevation = 8.dp,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Column {
                    suggestions.forEach { s ->
                        Text(
                            s,
                            style = MaterialTheme.typography.bodyLarge,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { updateQuery(s); focused = false; onRecordSearch(s) }
                                .padding(horizontal = 16.dp, vertical = 12.dp),
                        )
                    }
                }
            }
        }

        val result = page
        when {
            error != null -> ErrorBox(language, error)
            query.isBlank() -> Text(
                Localization.get(language, "search"),
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 16.dp),
            )
            loading && result == null && filterItems == null -> LoadingBox(language)
            result != null -> LazyColumn(Modifier.fillMaxSize().padding(top = 8.dp)) {
                result.summaries.forEach { summary ->
                    item(key = "header-${summary.title}") {
                        Text(
                            summary.title,
                            style = MaterialTheme.typography.titleLarge,
                            modifier = Modifier.padding(top = 16.dp, bottom = 8.dp),
                        )
                    }
                    item(key = "body-${summary.title}") {
                        SummaryBody(
                            summary,
                            language,
                            onOpenAlbum,
                            onOpenArtist,
                            onOpenPlaylist,
                            onPlaySong,
                            onAddToQueue,
                            onAddToPlaylist,
                            // Acting on a result is what proves the search was
                            // meant: only pressing Enter used to save it, and
                            // results appear while typing, so nothing was ever
                            // remembered ("history does not remember what I
                            // search").
                            onActed = { query.trim().takeIf { it.isNotEmpty() }?.let(onRecordSearch) },
                        )
                    }
                }
                if (result.summaries.isEmpty()) {
                    item {
                        Text(
                            Localization.get(language, "no_results_found"),
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(top = 16.dp),
                        )
                    }
                }
            }
            filterItems != null -> {
                val results = filterItems!!
                val record = { query.trim().takeIf { it.isNotEmpty() }?.let(onRecordSearch) }
                if (results.all { it is SongItem }) {
                    LazyColumn(Modifier.fillMaxSize().padding(top = 8.dp)) {
                        items(results.filterIsInstance<SongItem>(), key = { it.id }) { song ->
                            SongRow(
                                song,
                                language,
                                { record(); onPlaySong(song) },
                                onAddToQueue = { record(); onAddToQueue(song) },
                                onAddToPlaylist = { record(); onAddToPlaylist(song) },
                            )
                        }
                    }
                } else {
                    LazyVerticalGrid(
                        columns = GridCells.Adaptive(gridItemSize.dp),
                        modifier = Modifier.fillMaxSize().padding(top = 8.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        gridItems(results, key = { it.id }) { item ->
                            YtItemCard(
                                item = item,
                                width = null,
                                onClick = { record(); onItemClick(item, onOpenAlbum, onOpenArtist, onOpenPlaylist, onPlaySong) },
                            )
                        }
                    }
                }
                if (results.isEmpty()) {
                    Text(
                        Localization.get(language, "no_results_found"),
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(top = 16.dp),
                    )
                }
            }
        }
    }
}

@Composable
private fun SummaryBody(
    summary: SearchSummary,
    language: String,
    onOpenAlbum: (String) -> Unit,
    onOpenArtist: (String) -> Unit,
    onOpenPlaylist: (String) -> Unit,
    onPlaySong: (SongItem) -> Unit,
    onAddToQueue: (SongItem) -> Unit,
    onAddToPlaylist: (SongItem) -> Unit,
    /** Called before acting on a result: acting on a search is what saves it. */
    onActed: () -> Unit = {},
) {
    val songs = summary.items.filterIsInstance<SongItem>()
    val others = summary.items.filterNot { it is SongItem }

    Column {
        songs.forEach { song ->
            SongRow(
                song,
                language,
                { onActed(); onPlaySong(song) },
                onAddToQueue = { onActed(); onAddToQueue(song) },
                onAddToPlaylist = { onActed(); onAddToPlaylist(song) },
            )
        }
        if (others.isNotEmpty()) {
            HorizontalCarousel(language = language, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                items(others, key = { it.id }) { item ->
                    YtItemCard(
                        item = item,
                        onClick = { onActed(); onItemClick(item, onOpenAlbum, onOpenArtist, onOpenPlaylist, onPlaySong) },
                    )
                }
            }
        }
    }
}
