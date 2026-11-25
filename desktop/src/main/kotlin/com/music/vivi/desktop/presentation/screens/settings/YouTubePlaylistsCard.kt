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
import com.music.vivi.desktop.data.store.StoredPlaylistUndo
import com.music.vivi.desktop.data.youtube.AccountPlaylistHealth
import com.music.vivi.desktop.data.youtube.AccountPlaylistPack
import com.music.vivi.desktop.data.youtube.AccountPlaylistPacks
import com.music.vivi.desktop.data.youtube.AccountPlaylistSnapshot
import com.music.vivi.desktop.data.youtube.RemotePlaylist
import com.music.vivi.desktop.data.youtube.bulkAddSong
import com.music.vivi.desktop.data.youtube.removeSongFromPlaylists
import com.music.vivi.desktop.data.youtube.YouTubeAccount
import com.music.vivi.desktop.data.youtube.YouTubeMusicRepository
import com.music.vivi.desktop.data.youtube.YouTubePlaylistsRepository
import com.music.vivi.desktop.data.youtube.toStoredSnapshot
import com.music.vivi.desktop.generated.resources.Res
import com.music.vivi.desktop.generated.resources.ic_automirrored_rounded_playlist_add
import com.music.vivi.desktop.generated.resources.ic_rounded_add
import com.music.vivi.desktop.generated.resources.ic_rounded_close
import com.music.vivi.desktop.generated.resources.ic_rounded_drag_indicator
import com.music.vivi.desktop.generated.resources.ic_rounded_refresh
import com.music.vivi.desktop.presentation.components.subcomps.AppTooltip
import com.music.vivi.desktop.presentation.components.TrackDialogInput
import com.music.vivi.desktop.presentation.viewmodel.PlayerViewModel
import com.music.vivi.desktop.utils.DirectoryPicker
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.jetbrains.compose.resources.painterResource
import org.koin.compose.koinInject
import sh.calvin.reorderable.ReorderableItem
import sh.calvin.reorderable.rememberReorderableLazyListState

private sealed interface PlaylistUndo {
    data class Renamed(val playlistId: String, val previousTitle: String) : PlaylistUndo
    data class Deleted(val snapshots: List<AccountPlaylistSnapshot>) : PlaylistUndo
    data class Added(
        val playlistIds: List<String>,
        val videoId: String,
        val trackTitle: String
    ) : PlaylistUndo
}

private fun PlaylistUndo.toStored(message: String): StoredPlaylistUndo = when (this) {
    is PlaylistUndo.Renamed -> StoredPlaylistUndo(
        kind = StoredPlaylistUndo.RENAMED,
        playlistId = playlistId,
        previousTitle = previousTitle,
        message = message
    )

    is PlaylistUndo.Deleted -> StoredPlaylistUndo(
        kind = StoredPlaylistUndo.DELETED,
        snapshots = snapshots.map { it.toStoredSnapshot() },
        message = message
    )

    is PlaylistUndo.Added -> StoredPlaylistUndo(
        kind = StoredPlaylistUndo.ADDED,
        addedPlaylistIds = playlistIds,
        videoId = videoId,
        trackTitle = trackTitle,
        message = message
    )
}

private fun StoredPlaylistUndo.toCardUndo(): PlaylistUndo? = when (kind) {
    StoredPlaylistUndo.RENAMED -> {
        val id = playlistId
        val previous = previousTitle
        if (id.isNullOrBlank() || previous == null) null else PlaylistUndo.Renamed(id, previous)
    }

    StoredPlaylistUndo.DELETED -> {
        val snapshots = snapshots.map { AccountPlaylistSnapshot(it.title, it.videoIds) }
        if (snapshots.isEmpty()) null else PlaylistUndo.Deleted(snapshots)
    }

    StoredPlaylistUndo.ADDED -> {
        if (addedPlaylistIds.isEmpty() || videoId.isNullOrBlank()) {
            null
        } else {
            PlaylistUndo.Added(addedPlaylistIds, videoId, trackTitle.orEmpty())
        }
    }

    else -> null
}

private fun playlistCount(count: Int): String =
    "$count ${if (count == 1) "playlist" else "playlists"}"

private fun trackCount(count: Int): String =
    "$count ${if (count == 1) "track" else "tracks"}"

/**
 * The account's own YouTube Music playlists.
 *
 * The card is here because this is the Accounts section: the playlists belong
 * to the signed-in account, not to the local library. Adding the playing track
 * needs a YouTube track, so the action is disabled while a local file plays.
 *
 * Selection handles several playlists at once; Delete snapshots each playlist
 * before removing it so Undo can recreate it, and Rename keeps the old name for
 * its own Undo. The Undo token is written to the store, so it survives leaving
 * the screen or restarting, and a restore that failed halfway keeps only the
 * playlists that still need a retry. YouTube exposes no order for the library
 * list, so drag and drop saves the card's order for the active account.
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
    var health by remember { mutableStateOf<AccountPlaylistHealth?>(null) }
    var healthBusy by remember { mutableStateOf(false) }
    var importPack by remember { mutableStateOf<AccountPlaylistPack?>(null) }
    var importName by remember { mutableStateOf("") }

    val videoId = remember(currentSong) { currentSong?.let(musicRepository::videoIdOf) }
    // Switching account must reload the list and the Undo bar of the new account.
    val activeAccountId = YouTubeAccount.activeAccountId()
    val listState = rememberLazyListState()
    val reorderableState = rememberReorderableLazyListState(
        lazyListState = listState,
        onMove = { from, to ->
            playlists = playlists.toMutableList().apply {
                add(to.index, removeAt(from.index))
            }
        }
    )

    LaunchedEffect(activeAccountId) {
        undo = null
        undoMessage = ""
        repository.pendingUndo()?.let { stored ->
            stored.toCardUndo()?.let { change ->
                undo = change
                undoMessage = stored.message
            }
        }
    }

    LaunchedEffect(activeAccountId, refreshToken) {
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

    suspend fun rememberUndo(change: PlaylistUndo, text: String) {
        undo = change
        undoMessage = text
        repository.rememberUndo(change.toStored(text))
    }

    suspend fun forgetUndo() {
        undo = null
        undoMessage = ""
        repository.clearUndo()
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
            forgetUndo()
            val result = bulkAddSong(
                playlistIds = editable.map { it.id },
                videoId = video
            ) { id, videoId -> repository.addSongIfAbsent(id, videoId) }
            val added = result.added
            val already = result.already
            val failed = result.failed
            val addedIds = result.addedIds
            message = when {
                singleLabel != null && added == 1 ->
                    "Added \"$trackTitle\" to $singleLabel."
                singleLabel != null && already == 1 ->
                    "\"$trackTitle\" is already in $singleLabel."
                singleLabel != null ->
                    "The track could not be added to $singleLabel."
                added == 0 && already > 0 && failed == 0 ->
                    "The track is already in ${playlistCount(already)}."
                added == 0 && already > 0 ->
                    "Already in ${playlistCount(already)}, $failed failed."
                added == 0 ->
                    "The track could not be added."
                failed == 0 && already == 0 ->
                    "Added the track to ${playlistCount(added)}."
                failed == 0 ->
                    "Added to ${playlistCount(added)}, already in ${playlistCount(already)}."
                already == 0 ->
                    "Added to ${playlistCount(added)}, $failed failed."
                else ->
                    "Added to ${playlistCount(added)}, already in ${playlistCount(already)}, $failed failed."
            }
            if (addedIds.isNotEmpty()) {
                rememberUndo(
                    PlaylistUndo.Added(addedIds, video, trackTitle),
                    "Added \"$trackTitle\" to ${playlistCount(addedIds.size)}."
                )
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
            forgetUndo()
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
                append("Deleted ${playlistCount(snapshots.size)}.")
                if (failed > 0) append(" $failed could not be deleted.")
                if (skipped > 0) append(" $skipped skipped because their tracks could not be read.")
            }
            if (snapshots.isNotEmpty()) {
                rememberUndo(
                    PlaylistUndo.Deleted(snapshots),
                    "Deleted ${playlistCount(snapshots.size)}."
                )
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
            forgetUndo()
            val outcome = repository.rename(playlist.id, name)
            message = outcome.fold(
                onSuccess = { "Renamed in the account." },
                onFailure = { it.message ?: "The playlist could not be renamed." }
            )
            if (outcome.isSuccess) {
                rememberUndo(
                    PlaylistUndo.Renamed(playlist.id, playlist.title),
                    "Renamed \"${playlist.title}\" to \"$name\"."
                )
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
                    if (outcome.isSuccess) {
                        message = "The old name is back."
                        forgetUndo()
                    } else {
                        // The persisted token stays as it is, so Undo can be pressed again.
                        message = outcome.exceptionOrNull()?.message
                            ?: "The name could not be restored."
                    }
                }

                is PlaylistUndo.Deleted -> {
                    var restored = 0
                    var tracksRestored = 0
                    val failedSnapshots = mutableListOf<AccountPlaylistSnapshot>()
                    for (snapshot in change.snapshots) {
                        val outcome = repository.restore(snapshot)
                        if (outcome.isSuccess) {
                            restored++
                            tracksRestored += outcome.getOrThrow().restoredTracks
                        } else {
                            failedSnapshots += snapshot
                        }
                    }
                    when {
                        failedSnapshots.isEmpty() -> {
                            message = "Restored ${playlistCount(restored)} " +
                                "with ${trackCount(tracksRestored)}."
                            forgetUndo()
                        }

                        restored == 0 -> {
                            message = "The playlists could not be restored."
                        }

                        else -> {
                            message = "Restored $restored, ${failedSnapshots.size} still to retry."
                            val retry = PlaylistUndo.Deleted(failedSnapshots)
                            rememberUndo(
                                retry,
                                "Deleted ${playlistCount(failedSnapshots.size)}, still to restore."
                            )
                        }
                    }
                }

                is PlaylistUndo.Added -> {
                    val failedIds = removeSongFromPlaylists(
                        playlistIds = change.playlistIds,
                        videoId = change.videoId
                    ) { id, videoId -> repository.removeSong(id, videoId) }
                    when {
                        failedIds.isEmpty() -> {
                            message = "Removed \"${change.trackTitle}\" from " +
                                "${playlistCount(change.playlistIds.size)}."
                            forgetUndo()
                        }

                        failedIds.size == change.playlistIds.size -> {
                            message = "The track could not be removed from any playlist."
                        }

                        else -> {
                            val removed = change.playlistIds.size - failedIds.size
                            message = "Removed from ${playlistCount(removed)}, " +
                                "${failedIds.size} still to retry."
                            rememberUndo(
                                PlaylistUndo.Added(failedIds, change.videoId, change.trackTitle),
                                "Added \"${change.trackTitle}\" to " +
                                    "${playlistCount(failedIds.size)}, still to undo."
                            )
                        }
                    }
                }
            }
            busyAction = false
            refreshToken++
        }
    }

    fun checkHealth() {
        scope.launch {
            healthBusy = true
            health = null
            repository.checkHealth()
                .onSuccess { health = it }
                .onFailure { message = it.message ?: "The account playlists could not be checked." }
            healthBusy = false
        }
    }

    fun exportSelection(targets: List<RemotePlaylist>) {
        val editable = targets.filter { it.isEditable }
        if (editable.isEmpty()) {
            message = "Select at least one editable playlist."
            return
        }
        DirectoryPicker.savePlaylistPackFile { file ->
            scope.launch {
                busyAction = true
                repository.exportPack(editable.map { it.id })
                    .onSuccess { pack ->
                        runCatching {
                            withContext(Dispatchers.IO) {
                                file.writeText(AccountPlaylistPacks.encode(pack), Charsets.UTF_8)
                            }
                        }.onSuccess {
                            message = "Exported ${playlistCount(pack.playlists.size)} " +
                                "with ${trackCount(pack.playlists.sumOf { it.videoIds.size })} to ${file.name}."
                        }.onFailure {
                            message = "The file could not be written: ${it.message}"
                        }
                    }
                    .onFailure { message = it.message ?: "The playlists could not be read." }
                busyAction = false
            }
        }
    }

    fun chooseImport() {
        DirectoryPicker.choosePlaylistPackFile { file ->
            scope.launch {
                busyAction = true
                val parsed = withContext(Dispatchers.IO) {
                    runCatching {
                        AccountPlaylistPacks.decode(file.readText(Charsets.UTF_8)).getOrThrow()
                    }
                }
                parsed.onSuccess {
                    importPack = it
                    importName = file.name
                }.onFailure {
                    message = it.message ?: "The file could not be read."
                }
                busyAction = false
            }
        }
    }

    fun copyImported() {
        val pack = importPack ?: return
        importPack = null
        scope.launch {
            busyAction = true
            val outcome = repository.copyPack(pack)
            message = outcome.fold(
                onSuccess = { copied ->
                    buildString {
                        append("Copied ${playlistCount(copied.created)} ")
                        append("with ${trackCount(copied.restoredTracks)}.")
                        if (copied.failedTracks > 0) {
                            append(" ${copied.failedTracks} tracks were refused.")
                        }
                    }
                },
                onFailure = { it.message ?: "The playlists could not be copied." }
            )
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
                    AppTooltip("Check names, empty lists and repeated tracks") {
                        TextButton(
                            enabled = !loading && !busyAction && !healthBusy,
                            onClick = { checkHealth() }
                        ) { Text(if (healthBusy) "Checking..." else "Health") }
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
                                    onClick = { exportSelection(selectedPlaylists) }
                                ) { Text("Export") }
                                TextButton(
                                    enabled = selectedEditable > 0 && !busyAction,
                                    colors = ButtonDefaults.textButtonColors(contentColor = colors.error),
                                    onClick = {
                                        deleting = selectedPlaylists.filter { it.isEditable }
                                    }
                                ) { Text("Delete") }
                            }
                            Text(
                                text = "Adding uses the playing YouTube track; a track already in a " +
                                    "playlist is reported, not added twice. Read-only playlists cannot " +
                                    "be selected.",
                                style = MaterialTheme.typography.labelSmall,
                                color = colors.onSurfaceVariant
                            )
                        }
                    }
                }

                health?.let { report ->
                    AccountPlaylistHealthPanel(
                        report = report,
                        onDismiss = { health = null }
                    )
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
                        text = "Drag the handle to set the order for this account on this device. " +
                            "YouTube Music has no account-level playlist order.",
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
                                        scope.launch { forgetUndo() }
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

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
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
                    TextButton(
                        enabled = !busyAction,
                        onClick = { chooseImport() }
                    ) { Text("Copy from file") }
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
                    forgetUndo()
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
                            "with its name and tracks; the cover, description and privacy are not " +
                            "restored. The Undo stays available after a restart.",
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

    importPack?.let { pack ->
        AlertDialog(
            onDismissRequest = { importPack = null },
            title = { Text("Copy account playlists?") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(
                        text = "From $importName",
                        style = MaterialTheme.typography.bodySmall,
                        color = colors.onSurfaceVariant
                    )
                    Text(
                        text = "${playlistCount(pack.playlists.size)} with " +
                            "${trackCount(pack.playlists.sumOf { it.videoIds.size })}.",
                        style = MaterialTheme.typography.bodyMedium
                    )
                    pack.playlists.take(8).forEach { playlist ->
                        Text(
                            text = "\u2022 ${playlist.title} (${playlist.videoIds.size} tracks)",
                            style = MaterialTheme.typography.bodySmall,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                    if (pack.playlists.size > 8) {
                        Text(
                            text = "... and ${pack.playlists.size - 8} more",
                            style = MaterialTheme.typography.bodySmall,
                            color = colors.onSurfaceVariant
                        )
                    }
                    Text(
                        text = "They are created in the signed-in account. A name already in " +
                            "the account gets an \"(imported)\" suffix, and nothing is merged " +
                            "into an existing playlist.",
                        style = MaterialTheme.typography.bodySmall,
                        color = colors.onSurfaceVariant
                    )
                }
            },
            confirmButton = {
                TextButton(onClick = { copyImported() }) { Text("Copy") }
            },
            dismissButton = {
                TextButton(onClick = { importPack = null }) { Text("Cancel") }
            }
        )
    }
}

@Composable
private fun AccountPlaylistHealthPanel(
    report: AccountPlaylistHealth,
    onDismiss: () -> Unit
) {
    val colors = MaterialTheme.colorScheme
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = colors.surfaceContainerHigh,
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text(
                text = "Account playlist health",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
                color = colors.onSurface
            )
            Text(
                text = "${playlistCount(report.playlistCount)} · " +
                    "${report.editableCount} editable · ${report.readOnlyCount} read-only · " +
                    "${trackCount(report.trackCount)} read",
                style = MaterialTheme.typography.bodySmall,
                color = colors.onSurfaceVariant
            )
            if (report.isClean) {
                Text(
                    text = "No issue was found in the account playlists.",
                    style = MaterialTheme.typography.bodySmall,
                    color = colors.primary
                )
            } else {
                report.issues.take(6).forEach { issue ->
                    Text(
                        text = "\u2022 ${issue.title}: ${issue.detail}",
                        style = MaterialTheme.typography.bodySmall,
                        color = colors.onSurface,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                if (report.issues.size > 6) {
                    Text(
                        text = "... and ${report.issues.size - 6} more",
                        style = MaterialTheme.typography.bodySmall,
                        color = colors.onSurfaceVariant
                    )
                }
            }
            TextButton(onClick = onDismiss) { Text("Dismiss") }
        }
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
