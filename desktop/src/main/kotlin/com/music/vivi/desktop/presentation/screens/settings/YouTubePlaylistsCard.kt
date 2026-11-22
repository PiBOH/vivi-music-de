package com.music.vivi.desktop.presentation.screens.settings

import androidx.compose.foundation.VerticalScrollbar
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.defaultScrollbarStyle
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollbarAdapter
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.music.vivi.desktop.data.youtube.AccountPlaylistSnapshot
import com.music.vivi.desktop.data.youtube.RemotePlaylist
import com.music.vivi.desktop.data.youtube.YouTubeMusicRepository
import com.music.vivi.desktop.data.youtube.YouTubePlaylistsRepository
import com.music.vivi.desktop.generated.resources.Res
import com.music.vivi.desktop.generated.resources.ic_automirrored_rounded_playlist_add
import com.music.vivi.desktop.generated.resources.ic_rounded_add
import com.music.vivi.desktop.generated.resources.ic_rounded_close
import com.music.vivi.desktop.generated.resources.ic_rounded_drag_indicator
import com.music.vivi.desktop.generated.resources.ic_rounded_refresh
import com.music.vivi.desktop.presentation.components.subcomps.AppTooltip
import com.music.vivi.desktop.presentation.components.TrackDialogInput
import com.music.vivi.desktop.presentation.viewmodel.PlayerViewModel
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.painterResource
import org.koin.compose.koinInject
import sh.calvin.reorderable.ReorderableItem
import sh.calvin.reorderable.rememberReorderableLazyListState

private sealed interface PlaylistUndo {
    data class Renamed(val playlistId: String, val previousTitle: String) : PlaylistUndo
    data class Deleted(val snapshots: List<AccountPlaylistSnapshot>) : PlaylistUndo
}

/**
 * The account's own YouTube Music playlists.
 *
 * The card is here because this is the Accounts section: the playlists belong
 * to the signed-in account, not to the local library. Adding the playing track
 * needs a YouTube track, so the action is disabled while a local file plays.
 *
 * Selection handles several playlists at once; Delete snapshots each playlist
 * before removing it so Undo can recreate it, and Rename keeps the old name for
 * its own Undo. YouTube exposes no order for the library list, so drag and drop
 * saves the card's order in the local store.
 */
@Composable
fun YouTubePlaylistsCard(
    repository: YouTubePlaylistsRepository = koinInject(),
    musicRepository: YouTubeMusicRepository = koinInject(),
    playerViewModel: PlayerViewModel = koinInject()
) {
    val colors = MaterialTheme.colorScheme
    val scope = rememberCoroutineScope()
    val currentSong by playerViewModel.currentSong.collectAsState()

    var playlists by remember { mutableStateOf<List<RemotePlaylist>>(emptyList()) }
    var loading by remember { mutableStateOf(true) }
    var error by remember { mutableStateOf<String?>(null) }
    var message by remember { mutableStateOf<String?>(null) }
    var refreshToken by remember { mutableIntStateOf(0) }
    var showCreate by remember { mutableStateOf(false) }
    var renaming by remember { mutableStateOf<RemotePlaylist?>(null) }
    var deleting by remember { mutableStateOf<List<RemotePlaylist>>(emptyList()) }
    var busyAction by remember { mutableStateOf(false) }
    var selectionMode by remember { mutableStateOf(false) }
    var selectedIds by remember { mutableStateOf<Set<String>>(emptySet()) }
    var undo by remember { mutableStateOf<PlaylistUndo?>(null) }
    var undoMessage by remember { mutableStateOf("") }

    val videoId = remember(currentSong) { currentSong?.let(musicRepository::videoIdOf) }
    val listState = rememberLazyListState()
    val reorderableState = rememberReorderableLazyListState(
        lazyListState = listState,
        onMove = { from, to ->
            playlists = playlists.toMutableList().apply {
                add(to.index, removeAt(from.index))
            }
        }
    )

    LaunchedEffect(refreshToken) {
        loading = true
        error = null
        repository.load()
            .onSuccess { loaded ->
                playlists = loaded
                val ids = loaded.mapTo(HashSet()) { it.id }
                selectedIds = selectedIds.filterTo(HashSet()) { it in ids }
                if (selectionMode && selectedIds.isEmpty()) selectionMode = false
            }
            .onFailure { error = it.message ?: "The account playlists could not be read." }
        loading = false
    }

    LaunchedEffect(reorderableState.isAnyItemDragging) {
        if (!reorderableState.isAnyItemDragging && playlists.isNotEmpty()) {
            repository.saveOrder(playlists.map { it.id })
        }
    }

    val selectedPlaylists = playlists.filter { it.id in selectedIds }
    val selectedEditable = selectedPlaylists.count { it.isEditable }

    fun toggleSelection(playlist: RemotePlaylist) {
        if (!playlist.isEditable) return
        selectedIds = if (playlist.id in selectedIds) {
            selectedIds - playlist.id
        } else {
            selectedIds + playlist.id
        }
    }

    fun exitSelection() {
        selectionMode = false
        selectedIds = emptySet()
    }

    fun addCurrentTo(targets: List<RemotePlaylist>, singleLabel: String? = null) {
        val video = videoId ?: return
        val trackTitle = currentSong?.title.orEmpty()
        val editable = targets.filter { it.isEditable }
        if (editable.isEmpty()) {
            message = "Select at least one editable playlist."
            return
        }
        scope.launch {
            busyAction = true
            undo = null
            var added = 0
            var failed = 0
            for (playlist in editable) {
                if (repository.addSong(playlist.id, video).isSuccess) added++ else failed++
            }
            message = when {
                singleLabel != null && failed == 0 -> "Added \"$trackTitle\" to $singleLabel."
                failed == 0 -> "Added the track to $added ${if (added == 1) "playlist" else "playlists"}."
                added == 0 -> "The track could not be added."
                else -> "Added to $added ${if (added == 1) "playlist" else "playlists"}, $failed failed."
            }
            busyAction = false
            if (singleLabel == null) exitSelection()
            refreshToken++
        }
    }

    fun deletePlaylists(targets: List<RemotePlaylist>) {
        deleting = emptyList()
        if (targets.isEmpty()) return
        scope.launch {
            busyAction = true
            undo = null
            undoMessage = ""
            val snapshots = mutableListOf<AccountPlaylistSnapshot>()
            var failed = 0
            var skipped = 0
            for (target in targets) {
                val snapshot = repository.snapshot(target.id).getOrNull()
                if (snapshot == null) {
                    skipped++
                    continue
                }
                if (repository.delete(target.id).isSuccess) {
                    snapshots += snapshot
                } else {
                    failed++
                }
            }
            message = buildString {
                append("Deleted ${snapshots.size} ${if (snapshots.size == 1) "playlist" else "playlists"}.")
                if (failed > 0) append(" $failed could not be deleted.")
                if (skipped > 0) append(" $skipped skipped because their tracks could not be read.")
            }
            if (snapshots.isNotEmpty()) {
                undo = PlaylistUndo.Deleted(snapshots)
                undoMessage = "Deleted ${snapshots.size} ${if (snapshots.size == 1) "playlist" else "playlists"}."
            }
            busyAction = false
            exitSelection()
            refreshToken++
        }
    }

    fun renamePlaylist(playlist: RemotePlaylist, name: String) {
        renaming = null
        if (name == playlist.title) {
            message = "The name is unchanged."
            return
        }
        scope.launch {
            busyAction = true
            undo = null
            val outcome = repository.rename(playlist.id, name)
            message = outcome.fold(
                onSuccess = { "Renamed in the account." },
                onFailure = { it.message ?: "The playlist could not be renamed." }
            )
            if (outcome.isSuccess) {
                undo = PlaylistUndo.Renamed(playlist.id, playlist.title)
                undoMessage = "Renamed \"${playlist.title}\" to \"$name\"."
            }
            busyAction = false
            refreshToken++
        }
    }

    fun performUndo(change: PlaylistUndo) {
        scope.launch {
            busyAction = true
            when (change) {
                is PlaylistUndo.Renamed -> {
                    val outcome = repository.rename(change.playlistId, change.previousTitle)
                    message = outcome.fold(
                        onSuccess = { "The old name is back." },
                        onFailure = { it.message ?: "The name could not be restored." }
                    )
                }

                is PlaylistUndo.Deleted -> {
                    var restored = 0
                    var failed = 0
                    var tracksRestored = 0
                    for (snapshot in change.snapshots) {
                        val outcome = repository.restore(snapshot)
                        if (outcome.isSuccess) {
                            restored++
                            tracksRestored += outcome.getOrThrow().restoredTracks
                        } else {
                            failed++
                        }
                    }
                    message = when {
                        failed == 0 -> "Restored $restored ${if (restored == 1) "playlist" else "playlists"} " +
                            "with $tracksRestored ${if (tracksRestored == 1) "track" else "tracks"}."
                        restored == 0 -> "The playlists could not be restored."
                        else -> "Restored $restored, $failed could not be restored."
                    }
                }
            }
            undo = null
            undoMessage = ""
            busyAction = false
            refreshToken++
        }
    }

    SettingsSubsection(title = "YOUTUBE MUSIC PLAYLISTS") {
        Surface(
            shape = RoundedCornerShape(20.dp),
            color = colors.surfaceContainer,
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Playlists in your account",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = colors.onSurface
                        )
                        Text(
                            text = if (videoId != null) {
                                "Adding \"${currentSong?.title}\" is possible while it plays."
                            } else {
                                "Play a YouTube Music track to add it to one of these playlists."
                            },
                            style = MaterialTheme.typography.bodySmall,
                            color = colors.onSurfaceVariant
                        )
                    }
                    if (playlists.any { it.isEditable } && !selectionMode) {
                        AppTooltip("Select playlists") {
                            TextButton(
                                enabled = !loading && !busyAction,
                                onClick = { selectionMode = true }
                            ) { Text("Select") }
                        }
                    }
                    AppTooltip("Refresh account playlists") {
                        TextButton(onClick = { refreshToken++ }, enabled = !loading && !busyAction) {
                            Icon(
                                painter = painterResource(Res.drawable.ic_rounded_refresh),
                                contentDescription = null,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                }

                if (selectionMode) {
                    Surface(
                        shape = RoundedCornerShape(16.dp),
                        color = colors.surfaceContainerHigh,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier.padding(12.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "${selectedIds.size} selected",
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.SemiBold,
                                    color = colors.onSurface
                                )
                                Row {
                                    TextButton(
                                        enabled = !busyAction,
                                        onClick = {
                                            selectedIds = playlists
                                                .filter { it.isEditable }
                                                .mapTo(HashSet()) { it.id }
                                        }
                                    ) { Text("Select all") }
                                    TextButton(
                                        enabled = !busyAction,
                                        onClick = { exitSelection() }
                                    ) { Text("Done") }
                                }
                            }
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Button(
                                    enabled = videoId != null && selectedEditable > 0 && !busyAction,
                                    onClick = { addCurrentTo(selectedPlaylists) }
                                ) {
                                    Icon(
                                        painter = painterResource(Res.drawable.ic_automirrored_rounded_playlist_add),
                                        contentDescription = null,
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Spacer(Modifier.width(8.dp))
                                    Text("Add current track")
                                }
                                TextButton(
                                    enabled = selectedEditable > 0 && !busyAction,
                                    colors = ButtonDefaults.textButtonColors(contentColor = colors.error),
                                    onClick = {
                                        deleting = selectedPlaylists.filter { it.isEditable }
                                    }
                                ) { Text("Delete") }
                            }
                            Text(
                                text = "Adding uses the playing YouTube track. Read-only playlists " +
                                    "cannot be selected.",
                                style = MaterialTheme.typography.labelSmall,
                                color = colors.onSurfaceVariant
                            )
                        }
                    }
                }

                when {
                    loading -> Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
                        Text("Loading the account playlists...", style = MaterialTheme.typography.bodySmall)
                    }

                    error != null -> Text(
                        text = error ?: "",
                        style = MaterialTheme.typography.bodySmall,
                        color = colors.error
                    )

                    playlists.isEmpty() -> Text(
                        text = "No editable playlists were found in this account.",
                        style = MaterialTheme.typography.bodySmall,
                        color = colors.onSurfaceVariant
                    )

                    else -> Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(max = 330.dp)
                    ) {
                        LazyColumn(
                            state = listState,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(end = 12.dp),
                            contentPadding = PaddingValues(vertical = 4.dp),
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            items(
                                items = playlists,
                                key = { it.id },
                                contentType = { "account_playlist" }
                            ) { playlist ->
                                ReorderableItem(
                                    state = reorderableState,
                                    key = playlist.id
                                ) { isDragging ->
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clip(RoundedCornerShape(12.dp))
                                            .background(
                                                if (isDragging) colors.surfaceContainerHighest else Color.Transparent
                                            )
                                            .then(
                                                if (selectionMode && playlist.isEditable) {
                                                    Modifier.clickable { toggleSelection(playlist) }
                                                } else {
                                                    Modifier
                                                }
                                            )
                                            .padding(vertical = 2.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        if (selectionMode) {
                                            Checkbox(
                                                checked = playlist.id in selectedIds,
                                                onCheckedChange = if (playlist.isEditable) {
                                                    { toggleSelection(playlist) }
                                                } else {
                                                    null
                                                },
                                                enabled = playlist.isEditable
                                            )
                                        } else {
                                            AppTooltip("Drag to reorder") {
                                                Box(
                                                    modifier = Modifier
                                                        .size(30.dp)
                                                        .draggableHandle(),
                                                    contentAlignment = Alignment.Center
                                                ) {
                                                    Icon(
                                                        painter = painterResource(Res.drawable.ic_rounded_drag_indicator),
                                                        contentDescription = "Reorder playlist",
                                                        tint = colors.onSurfaceVariant.copy(alpha = 0.55f),
                                                        modifier = Modifier.size(19.dp)
                                                    )
                                                }
                                            }
                                        }
                                        Icon(
                                            painter = painterResource(Res.drawable.ic_automirrored_rounded_playlist_add),
                                            contentDescription = null,
                                            tint = colors.secondary,
                                            modifier = Modifier.size(20.dp)
                                        )
                                        Column(modifier = Modifier.weight(1f)) {
                                            AppTooltip(playlist.title) {
                                                Text(
                                                    text = playlist.title,
                                                    style = MaterialTheme.typography.bodyMedium,
                                                    color = colors.onSurface,
                                                    maxLines = 1,
                                                    overflow = TextOverflow.Ellipsis
                                                )
                                            }
                                            val detail = listOfNotNull(
                                                playlist.songCountText,
                                                if (playlist.isEditable) null else "Read-only"
                                            ).joinToString(" · ")
                                            if (detail.isNotEmpty()) {
                                                Text(
                                                    text = detail,
                                                    style = MaterialTheme.typography.labelSmall,
                                                    color = colors.onSurfaceVariant
                                                )
                                            }
                                        }
                                        if (!selectionMode && playlist.isEditable) {
                                            AppTooltip(
                                                if (videoId == null) {
                                                    "Play a YouTube Music track to add it"
                                                } else {
                                                    "Add \"${currentSong?.title.orEmpty()}\" to ${playlist.title}"
                                                }
                                            ) {
                                                TextButton(
                                                    enabled = videoId != null && !busyAction,
                                                    onClick = { addCurrentTo(listOf(playlist), playlist.title) }
                                                ) { Text("Add") }
                                            }
                                            TextButton(
                                                enabled = !busyAction,
                                                onClick = { renaming = playlist }
                                            ) { Text("Rename") }
                                            TextButton(
                                                enabled = !busyAction,
                                                colors = ButtonDefaults.textButtonColors(contentColor = colors.error),
                                                onClick = { deleting = listOf(playlist) }
                                            ) { Text("Delete") }
                                        }
                                    }
                                }
                            }
                        }

                        VerticalScrollbar(
                            adapter = rememberScrollbarAdapter(listState),
                            modifier = Modifier
                                .align(Alignment.CenterEnd)
                                .fillMaxHeight()
                                .padding(vertical = 4.dp),
                            style = defaultScrollbarStyle().copy(
                                unhoverColor = colors.onSurfaceVariant.copy(alpha = 0.35f),
                                hoverColor = colors.primary.copy(alpha = 0.85f)
                            )
                        )
                    }
                }

                if (playlists.isNotEmpty() && !loading) {
                    Text(
                        text = "Drag the handle to set the order on this device. YouTube Music " +
                            "has no account-level playlist order.",
                        style = MaterialTheme.typography.labelSmall,
                        color = colors.onSurfaceVariant
                    )
                }

                undo?.let { change ->
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = colors.secondaryContainer,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(start = 12.dp, end = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = undoMessage,
                                style = MaterialTheme.typography.bodySmall,
                                color = colors.onSecondaryContainer,
                                modifier = Modifier.weight(1f)
                            )
                            TextButton(
                                enabled = !busyAction,
                                onClick = { performUndo(change) }
                            ) { Text("Undo") }
                            AppTooltip("Dismiss undo") {
                                IconButton(
                                    enabled = !busyAction,
                                    onClick = {
                                        undo = null
                                        undoMessage = ""
                                    },
                                    modifier = Modifier.size(32.dp)
                                ) {
                                    Icon(
                                        painter = painterResource(Res.drawable.ic_rounded_close),
                                        contentDescription = "Dismiss undo",
                                        tint = colors.onSecondaryContainer.copy(alpha = 0.75f),
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                            }
                        }
                    }
                }

                message?.let {
                    Text(
                        text = it,
                        style = MaterialTheme.typography.bodySmall,
                        color = colors.primary
                    )
                }

                Row {
                    Button(
                        enabled = !busyAction,
                        onClick = { showCreate = true }
                    ) {
                        Icon(
                            painter = painterResource(Res.drawable.ic_rounded_add),
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(Modifier.width(8.dp))
                        Text("New account playlist")
                    }
                }
            }
        }
    }

    if (showCreate) {
        PlaylistNameDialog(
            title = "New account playlist",
            initialName = "",
            confirmText = "Create",
            onDismiss = { showCreate = false },
            onConfirm = { name ->
                showCreate = false
                scope.launch {
                    busyAction = true
                    undo = null
                    val outcome = repository.create(name)
                    message = outcome.fold(
                        onSuccess = { "Created in the account." },
                        onFailure = { it.message ?: "The playlist could not be created." }
                    )
                    busyAction = false
                    refreshToken++
                }
            }
        )
    }

    renaming?.let { playlist ->
        PlaylistNameDialog(
            title = "Rename playlist",
            initialName = playlist.title,
            confirmText = "Save",
            onDismiss = { renaming = null },
            onConfirm = { name -> renamePlaylist(playlist, name) }
        )
    }

    if (deleting.isNotEmpty()) {
        AlertDialog(
            onDismissRequest = { deleting = emptyList() },
            title = {
                Text(
                    if (deleting.size == 1) {
                        "Delete account playlist?"
                    } else {
                        "Delete ${deleting.size} account playlists?"
                    }
                )
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    deleting.take(10).forEach { playlist ->
                        Text(
                            text = "\u2022 ${playlist.title}",
                            style = MaterialTheme.typography.bodyMedium,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                    if (deleting.size > 10) {
                        Text(
                            text = "... and ${deleting.size - 10} more",
                            style = MaterialTheme.typography.bodySmall,
                            color = colors.onSurfaceVariant
                        )
                    }
                    Text(
                        text = "They are removed from YouTube Music. Undo recreates each one " +
                            "with its name and tracks; the cover, description and privacy are not restored.",
                        style = MaterialTheme.typography.bodySmall,
                        color = colors.onSurfaceVariant
                    )
                }
            },
            confirmButton = {
                TextButton(
                    colors = ButtonDefaults.textButtonColors(contentColor = colors.error),
                    onClick = { deletePlaylists(deleting) }
                ) {
                    Text(if (deleting.size == 1) "Delete" else "Delete ${deleting.size}")
                }
            },
            dismissButton = {
                TextButton(onClick = { deleting = emptyList() }) { Text("Cancel") }
            }
        )
    }
}

@Composable
private fun PlaylistNameDialog(
    title: String,
    initialName: String,
    confirmText: String,
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit
) {
    TrackDialogInput()
    var name by remember { mutableStateOf(initialName) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                label = { Text("Playlist name") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
        },
        confirmButton = {
            Button(
                enabled = name.isNotBlank(),
                onClick = { onConfirm(name.trim()) }
            ) { Text(confirmText) }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}
