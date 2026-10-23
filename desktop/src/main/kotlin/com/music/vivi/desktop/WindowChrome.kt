package com.music.vivi.desktop

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.window.WindowDraggableArea
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.QueueMusic
import androidx.compose.material.icons.automirrored.filled.Subject
import androidx.compose.material.icons.automirrored.outlined.QueueMusic
import androidx.compose.material.icons.filled.Album
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Group
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Leaderboard
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PlayCircle
import androidx.compose.material.icons.filled.PlaylistPlay
import androidx.compose.material.icons.filled.QueueMusic
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.SpeakerGroup
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Group
import androidx.compose.material.icons.outlined.History
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Leaderboard
import androidx.compose.material.icons.outlined.MusicNote
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.WindowScope
import com.music.innertube.YouTube
import com.music.vivi.desktop.player.AudioOutput

/**
 * The window's own chrome: the header that hosts the window controls, the drag
 * surface behind the interface, and the overlay that keeps minimize, maximize
 * and close reachable on the screens the header does not cover.
 */
@Composable
fun WindowScope.WindowDragSurface(nativeTitleBar: Boolean) {
    if (!nativeTitleBar) {
        WindowDraggableArea(Modifier.fillMaxSize()) {}
    }
}

@Composable
fun WindowControlsOverlay(
    nativeTitleBar: Boolean,
    spotifyLayout: Boolean,
    onPlayerScreen: Boolean,
    language: String,
    isMaximized: Boolean,
    onMinimize: () -> Unit,
    onMaximize: () -> Unit,
    onClose: () -> Unit,
) {
    if (!nativeTitleBar && (!spotifyLayout || onPlayerScreen)) {
        Box(Modifier.fillMaxSize()) {
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = MaterialTheme.colorScheme.surfaceContainer.copy(alpha = 0.85f),
                modifier = Modifier.align(Alignment.TopEnd).padding(top = 6.dp, end = 6.dp),
            ) {
                WindowControls(
                    language = language,
                    isMaximized = isMaximized,
                    onMinimize = onMinimize,
                    onMaximize = onMaximize,
                    onClose = onClose,
                )
            }
        }
    }
}

@Composable
fun WindowScope.SpotifyTopHeader(
    language: String,
    canGoBack: Boolean,
    onBack: () -> Unit,
    onOpenSearch: () -> Unit,
    onOpenHome: () -> Unit,
    onOpenHistory: () -> Unit = {},
    onOpenStats: () -> Unit = {},
    onOpenListenTogether: () -> Unit,
    onOpenSettings: () -> Unit,
    onOpenLyrics: () -> Unit = {},
    onOpenQueue: () -> Unit = {},
    onMinimize: () -> Unit,
    onMaximize: () -> Unit,
    onClose: () -> Unit,
    isMaximized: Boolean = false,
    showWindowControls: Boolean = true,
    searchQuery: String = "",
    onSearchQueryChange: (String) -> Unit = {},
    onSearchSubmit: (String) -> Unit = {},
    selectedFilter: YouTube.SearchFilter? = null,
    onFilterSelect: (YouTube.SearchFilter?) -> Unit = {},
) {
    // Real output-device picker (the button used to be a no-op placeholder).
    var showOutputPicker by remember { mutableStateOf(false) }
    WindowDraggableArea {
        Row(
            Modifier
                .fillMaxWidth()
                .height(48.dp)
                .background(MaterialTheme.colorScheme.surfaceContainer)
                .padding(start = 12.dp, end = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                // No sidebar toggle here any more: the rail is opened and
                // collapsed by clicking the "VIVI Music" title (and, collapsed,
                // the logo) at its top.
                Tooltip(Localization.get(language, "tooltip_back")) {
                    IconButton(
                        onClick = onBack,
                        enabled = canGoBack,
                        modifier = Modifier.size(32.dp),
                    ) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = Localization.get(language, "back"),
                            tint = if (canGoBack) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f),
                            modifier = Modifier.size(16.dp),
                        )
                    }
                }
            }

            Row(
                modifier = Modifier.weight(1f, fill = false).padding(horizontal = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Tooltip(Localization.get(language, "tooltip_home")) {
                    IconButton(
                        onClick = onOpenHome,
                        modifier = Modifier.size(36.dp),
                    ) {
                        Icon(
                            Icons.Filled.Home,
                            contentDescription = Localization.get(language, "home"),
                            tint = MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.size(20.dp),
                        )
                    }
                }

                Surface(
                    onClick = onOpenSearch,
                    shape = RoundedCornerShape(50),
                    color = MaterialTheme.colorScheme.surfaceContainerHigh,
                    modifier = Modifier.widthIn(min = 140.dp, max = 420.dp).weight(1f, fill = false).height(42.dp),
                ) {
                    Row(
                        Modifier.padding(start = 14.dp, end = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Icon(
                            Icons.Filled.Search,
                            contentDescription = Localization.get(language, "search"),
                            tint = if (searchQuery.isNotEmpty()) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                            modifier = Modifier.size(20.dp),
                        )
                        Spacer(Modifier.width(10.dp))
                        Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.CenterStart) {
                            if (searchQuery.isEmpty()) {
                                val hintText = Localization.get(language, "search_hint").let {
                                    if (it.isBlank() || it == "search_hint") "What do you want to play?" else it
                                }
                                Text(
                                    text = hintText,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                )
                            }
                            BasicTextField(
                                value = searchQuery,
                                onValueChange = { newQuery ->
                                    onSearchQueryChange(newQuery)
                                    onOpenSearch()
                                },
                                modifier = Modifier.fillMaxWidth().onFocusChanged { focusState ->
                                    if (focusState.isFocused) {
                                        onOpenSearch()
                                    }
                                },
                                singleLine = true,
                                textStyle = MaterialTheme.typography.bodyMedium.copy(color = MaterialTheme.colorScheme.onSurface),
                                cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
                                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                                keyboardActions = KeyboardActions(onSearch = {
                                    if (searchQuery.trim().isNotEmpty()) {
                                        onSearchSubmit(searchQuery.trim())
                                    }
                                }),
                            )
                        }
                        if (searchQuery.isNotEmpty()) {
                            Tooltip(Localization.get(language, "tooltip_clear")) {
                                IconButton(
                                    onClick = { onSearchQueryChange("") },
                                    modifier = Modifier.size(24.dp),
                                ) {
                                    Icon(
                                        Icons.Filled.Close,
                                        contentDescription = "Clear",
                                        tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                                        modifier = Modifier.size(16.dp),
                                    )
                                }
                            }
                            Spacer(Modifier.width(4.dp))
                        }

                        // Divider between input and filter dropdown
                        Box(
                            Modifier
                                .width(1.dp)
                                .height(18.dp)
                                .background(MaterialTheme.colorScheme.onSurface.copy(alpha = 0.2f))
                        )
                        Spacer(Modifier.width(4.dp))

                        // Right-aligned filter dropdown button
                        Box {
                            var filterMenuExpanded by remember { mutableStateOf(false) }
                            val currentFilterLabel = when (selectedFilter) {
                                YouTube.SearchFilter.FILTER_SONG -> Localization.get(language, "filter_songs")
                                YouTube.SearchFilter.FILTER_VIDEO -> Localization.get(language, "filter_videos")
                                YouTube.SearchFilter.FILTER_ALBUM -> Localization.get(language, "filter_albums")
                                YouTube.SearchFilter.FILTER_ARTIST -> Localization.get(language, "filter_artists")
                                YouTube.SearchFilter.FILTER_FEATURED_PLAYLIST -> Localization.get(language, "filter_playlists")
                                else -> Localization.get(language, "filter_all")
                            }

                            Row(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(12.dp))
                                    .clickable { filterMenuExpanded = true }
                                    .padding(horizontal = 8.dp, vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Text(
                                    text = currentFilterLabel,
                                    style = MaterialTheme.typography.bodySmall.copy(
                                        fontWeight = if (selectedFilter != null) FontWeight.Bold else FontWeight.Medium
                                    ),
                                    color = if (selectedFilter != null) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.85f),
                                )
                                Spacer(Modifier.width(2.dp))
                                Icon(
                                    Icons.Filled.ArrowDropDown,
                                    contentDescription = "Filter",
                                    tint = if (selectedFilter != null) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                                    modifier = Modifier.size(16.dp),
                                )
                            }

                            DropdownMenu(
                                expanded = filterMenuExpanded,
                                onDismissRequest = { filterMenuExpanded = false },
                                modifier = Modifier
                                    .width(180.dp)
                                    .clip(RoundedCornerShape(18.dp))
                                    .background(MaterialTheme.colorScheme.surfaceContainerHigh)
                                    .border(1.dp, MaterialTheme.colorScheme.onSurface.copy(alpha = 0.12f), RoundedCornerShape(18.dp))
                                    .padding(vertical = 4.dp),
                            ) {
                                data class FilterOption(
                                    val filter: YouTube.SearchFilter?,
                                    val label: String,
                                    val icon: ImageVector
                                )
                                val filters = listOf(
                                    FilterOption(null, Localization.get(language, "filter_all"), Icons.Filled.Tune),
                                    FilterOption(YouTube.SearchFilter.FILTER_SONG, Localization.get(language, "filter_songs"), Icons.Filled.MusicNote),
                                    FilterOption(YouTube.SearchFilter.FILTER_VIDEO, Localization.get(language, "filter_videos"), Icons.Filled.PlayCircle),
                                    FilterOption(YouTube.SearchFilter.FILTER_ALBUM, Localization.get(language, "filter_albums"), Icons.Filled.Album),
                                    FilterOption(YouTube.SearchFilter.FILTER_ARTIST, Localization.get(language, "filter_artists"), Icons.Filled.Person),
                                    FilterOption(YouTube.SearchFilter.FILTER_FEATURED_PLAYLIST, Localization.get(language, "filter_playlists"), Icons.Filled.PlaylistPlay),
                                )
                                filters.forEach { opt ->
                                    val isSelected = selectedFilter == opt.filter
                                    DropdownMenuItem(
                                        text = {
                                            Row(
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.spacedBy(10.dp),
                                                modifier = Modifier.fillMaxWidth(),
                                            ) {
                                                Icon(
                                                    opt.icon,
                                                    contentDescription = null,
                                                    tint = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                                                    modifier = Modifier.size(18.dp),
                                                )
                                                Text(
                                                    text = opt.label,
                                                    color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
                                                    style = MaterialTheme.typography.bodyMedium.copy(
                                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                                    ),
                                                    modifier = Modifier.weight(1f),
                                                )
                                                if (isSelected) {
                                                    Icon(
                                                        Icons.Filled.Check,
                                                        contentDescription = "Selected",
                                                        tint = MaterialTheme.colorScheme.primary,
                                                        modifier = Modifier.size(16.dp),
                                                    )
                                                }
                                            }
                                        },
                                        onClick = {
                                            onFilterSelect(opt.filter)
                                            filterMenuExpanded = false
                                            onOpenSearch()
                                        },
                                        modifier = Modifier
                                            .padding(horizontal = 6.dp, vertical = 2.dp)
                                            .clip(RoundedCornerShape(12.dp))
                                            .background(
                                                if (isSelected) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.35f)
                                                else Color.Transparent
                                            ),
                                    )
                                }
                            }
                        }
                    }
                }
            }

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(2.dp),
            ) {
                Tooltip(Localization.get(language, "tooltip_output_device")) {
                    IconButton(
                        onClick = { showOutputPicker = true },
                        modifier = Modifier.size(32.dp),
                    ) {
                        Icon(
                            Icons.Filled.SpeakerGroup,
                            contentDescription = Localization.get(language, "output_device"),
                            tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.85f),
                            modifier = Modifier.size(16.dp),
                        )
                    }
                }
                if (showOutputPicker) {
                    OutputDeviceDialog(
                        language = language,
                        onDismiss = { showOutputPicker = false },
                        onSelect = { name ->
                            AudioOutput.apply(name)
                            AppLog.log(
                                "settings",
                                "audio output device: " + name.ifBlank { "system default" },
                            )
                            showOutputPicker = false
                        },
                    )
                }
                Tooltip(Localization.get(language, "tooltip_lyrics")) {
                    IconButton(
                        onClick = onOpenLyrics,
                        modifier = Modifier.size(32.dp),
                    ) {
                        Icon(
                            Icons.AutoMirrored.Filled.Subject,
                            contentDescription = Localization.get(language, "lyrics"),
                            tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.85f),
                            modifier = Modifier.size(16.dp),
                        )
                    }
                }
                Tooltip(Localization.get(language, "tooltip_queue")) {
                    IconButton(
                        onClick = onOpenQueue,
                        modifier = Modifier.size(32.dp),
                    ) {
                        Icon(
                            Icons.AutoMirrored.Filled.QueueMusic,
                            contentDescription = Localization.get(language, "queue"),
                            tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.85f),
                            modifier = Modifier.size(16.dp),
                        )
                    }
                }
                Tooltip(Localization.get(language, "tooltip_history")) {
                    IconButton(
                        onClick = onOpenHistory,
                        modifier = Modifier.size(32.dp),
                    ) {
                        Icon(
                            Icons.Filled.History,
                            contentDescription = Localization.get(language, "history"),
                            tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.85f),
                            modifier = Modifier.size(16.dp),
                        )
                    }
                }
                Tooltip(Localization.get(language, "tooltip_wrapped")) {
                    IconButton(
                        onClick = onOpenStats,
                        modifier = Modifier.size(32.dp),
                    ) {
                        Icon(
                            Icons.Filled.Leaderboard,
                            contentDescription = Localization.get(language, "wrapped_title"),
                            tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.85f),
                            modifier = Modifier.size(16.dp),
                        )
                    }
                }
                Tooltip(Localization.get(language, "tooltip_listen_together")) {
                    IconButton(
                        onClick = onOpenListenTogether,
                        modifier = Modifier.size(32.dp),
                    ) {
                        Icon(
                            Icons.Filled.Group,
                            contentDescription = Localization.get(language, "listen_together_title"),
                            tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.85f),
                            modifier = Modifier.size(16.dp),
                        )
                    }
                }
                Tooltip(Localization.get(language, "tooltip_settings")) {
                    IconButton(
                        onClick = onOpenSettings,
                        modifier = Modifier.size(32.dp),
                    ) {
                        Icon(
                            Icons.Filled.Settings,
                            contentDescription = Localization.get(language, "settings"),
                            tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.85f),
                            modifier = Modifier.size(16.dp),
                        )
                    }
                }
                Spacer(Modifier.width(4.dp))
                if (showWindowControls) {
                    WindowControls(
                        language = language,
                        isMaximized = isMaximized,
                        onMinimize = onMinimize,
                        onMaximize = onMaximize,
                        onClose = onClose,
                    )
                }
            }
        }
    }
}
