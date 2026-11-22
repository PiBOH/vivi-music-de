package com.music.vivi.desktop.presentation.screens.settings

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.music.vivi.desktop.data.library.BrokenArtworkIssue
import com.music.vivi.desktop.data.library.LibraryHealthChecker
import com.music.vivi.desktop.data.library.LibraryHealthGroup
import com.music.vivi.desktop.data.library.LibraryHealthReport
import com.music.vivi.desktop.data.library.LibraryHealthUndo
import com.music.vivi.desktop.data.library.MissingFileIssue
import com.music.vivi.desktop.data.library.OrphanPlaylistIssue
import com.music.vivi.desktop.generated.resources.Res
import com.music.vivi.desktop.generated.resources.ic_outlined_clear_all
import com.music.vivi.desktop.generated.resources.ic_outlined_content_copy
import com.music.vivi.desktop.generated.resources.ic_outlined_error_outline
import com.music.vivi.desktop.generated.resources.ic_outlined_file_open
import com.music.vivi.desktop.generated.resources.ic_outlined_palette
import com.music.vivi.desktop.generated.resources.ic_rounded_check_circle
import com.music.vivi.desktop.generated.resources.ic_rounded_chevron_right
import com.music.vivi.desktop.presentation.components.DismissUndoBar
import com.music.vivi.desktop.presentation.viewmodel.PlayerViewModel
import com.music.vivi.desktop.utils.DirectoryPicker
import com.music.vivi.desktop.utils.DuplicateFinder
import java.io.File
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.jetbrains.compose.resources.DrawableResource
import org.jetbrains.compose.resources.painterResource
import org.koin.compose.koinInject

private sealed interface RepairPreview {
    data class Relink(val issue: MissingFileIssue, val file: File) : RepairPreview
    data class RemoveOrphans(val entries: List<OrphanPlaylistIssue>) : RepairPreview
}

/**
 * The group list and the repairs behind it.
 *
 * Every action here is explicit: a group opens its own list, [RepairPreview]
 * shows exactly what will change, the change happens only after confirmation,
 * and the bar at the bottom restores the previous state. Nothing is deleted
 * automatically, and no repair removes a song file.
 */
@Composable
fun LibraryHealthDetailScreen(
    viewModel: PlayerViewModel,
    onBack: () -> Unit,
    onOpenDuplicates: () -> Unit,
    initialGroup: LibraryHealthGroup? = null,
    checker: LibraryHealthChecker = koinInject(),
    modifier: Modifier = Modifier
) {
    val colors = MaterialTheme.colorScheme
    val scope = rememberCoroutineScope()
    var report by remember { mutableStateOf<LibraryHealthReport?>(null) }
    var busy by remember { mutableStateOf(false) }
    var scanRequest by remember { mutableIntStateOf(0) }
    var selectedGroup by remember { mutableStateOf(initialGroup) }
    var orphanSelection by remember { mutableStateOf<Set<String>>(emptySet()) }
    var pendingPreview by remember { mutableStateOf<RepairPreview?>(null) }
    var undo by remember { mutableStateOf<LibraryHealthUndo?>(null) }
    var undoMessage by remember { mutableStateOf("") }

    LaunchedEffect(scanRequest) {
        busy = true
        val scanned = withContext(Dispatchers.IO) { checker.scan() }
        report = scanned
        orphanSelection = scanned.orphanPlaylistEntries.mapTo(HashSet()) { it.playlistId }
        busy = false
    }

    fun confirmPreview(preview: RepairPreview) {
        pendingPreview = null
        scope.launch {
            busy = true
            val result = withContext(Dispatchers.IO) {
                when (preview) {
                    is RepairPreview.Relink -> checker.relinkSong(preview.issue.songId, preview.file)
                    is RepairPreview.RemoveOrphans -> checker.removeOrphanEntries(preview.entries)
                }
            }
            if (result.changed > 0) {
                undo = result.undo
                undoMessage = when (preview) {
                    is RepairPreview.Relink -> "Relinked 1 song"
                    is RepairPreview.RemoveOrphans ->
                        "Removed ${result.changed} orphan ${if (result.changed == 1) "entry" else "entries"}"
                }
                scanRequest++
            } else {
                busy = false
            }
        }
    }

    fun locate(issue: MissingFileIssue) {
        DirectoryPicker.chooseAudioFile(title = "Locate ${issue.label}") { file ->
            if (file.isFile) {
                pendingPreview = RepairPreview.Relink(issue, file)
            }
        }
    }

    val undoAction: () -> Unit = remember {
        {
            val change = undo
            if (change != null) {
                scope.launch {
                    withContext(Dispatchers.IO) { checker.undo(change) }
                    undo = null
                    scanRequest++
                }
            }
        }
    }

    BoxWithConstraints(modifier = modifier.fillMaxSize()) {
        val horizontalPadding = if (maxWidth < 540.dp) 16.dp else 28.dp
        val current = report

        Column(modifier = Modifier.fillMaxSize()) {
            SettingsHeader(
                title = "Library Health",
                subtitle = healthSubtitle(current),
                horizontalPadding = horizontalPadding
            )

            Box(modifier = Modifier.fillMaxWidth().weight(1f)) {
                if (current == null) {
                    Column(
                        modifier = Modifier.fillMaxSize(),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        CircularProgressIndicator(modifier = Modifier.size(32.dp))
                        Spacer(Modifier.height(12.dp))
                        Text(
                            text = "Checking the library...",
                            style = MaterialTheme.typography.bodyMedium,
                            color = colors.onSurfaceVariant
                        )
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(
                            start = horizontalPadding,
                            end = horizontalPadding,
                            top = 8.dp,
                            bottom = 92.dp
                        ),
                        verticalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        item {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Button(
                                    enabled = !busy,
                                    onClick = { scanRequest++ }
                                ) {
                                    Icon(
                                        painter = painterResource(Res.drawable.ic_rounded_check_circle),
                                        contentDescription = null,
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Spacer(Modifier.width(8.dp))
                                    Text(if (busy) "Checking..." else "Check again")
                                }
                                Text(
                                    text = "Nothing happens without confirmation",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = colors.onSurfaceVariant
                                )
                            }
                        }

                        if (current.isClean) {
                            item { CleanHealthState(songs = current.songs, playlists = current.playlists) }
                        } else if (selectedGroup == null) {
                            item {
                                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                    Text(
                                        text = "SELECT A GROUP",
                                        style = MaterialTheme.typography.labelMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = colors.primary,
                                        modifier = Modifier.padding(start = 4.dp)
                                    )
                                    HealthGroupCard(
                                        title = "Missing song files",
                                        count = current.missingFiles.size,
                                        description = "Songs whose file is no longer at the stored path.",
                                        icon = Res.drawable.ic_outlined_file_open,
                                        onClick = { selectedGroup = LibraryHealthGroup.MISSING_FILES }
                                    )
                                    HealthGroupCard(
                                        title = "Broken covers",
                                        count = current.brokenArtwork.size,
                                        description = "Cover paths that point at a picture that is gone.",
                                        icon = Res.drawable.ic_outlined_palette,
                                        onClick = { selectedGroup = LibraryHealthGroup.BROKEN_ARTWORK }
                                    )
                                    HealthGroupCard(
                                        title = "Playlist entries without a song",
                                        count = current.orphanEntryCount,
                                        description = "References to songs that are no longer in the library.",
                                        icon = Res.drawable.ic_outlined_clear_all,
                                        onClick = { selectedGroup = LibraryHealthGroup.ORPHAN_ENTRIES }
                                    )
                                    HealthGroupCard(
                                        title = "Duplicate groups",
                                        count = current.duplicateGroups.size,
                                        description = "Copies that share a title, artist and duration.",
                                        icon = Res.drawable.ic_outlined_content_copy,
                                        onClick = { selectedGroup = LibraryHealthGroup.DUPLICATES }
                                    )
                                }
                            }
                        } else {
                            item {
                                TextButton(onClick = { selectedGroup = null }) {
                                    Text("All groups")
                                }
                            }
                            item {
                                when (selectedGroup) {
                                    LibraryHealthGroup.MISSING_FILES -> MissingFilesSection(
                                        issues = current.missingFiles,
                                        enabled = !busy,
                                        onLocate = ::locate
                                    )
                                    LibraryHealthGroup.BROKEN_ARTWORK -> BrokenArtworkSection(
                                        issues = current.brokenArtwork
                                    )
                                    LibraryHealthGroup.ORPHAN_ENTRIES -> OrphanEntriesSection(
                                        entries = current.orphanPlaylistEntries,
                                        selected = orphanSelection,
                                        enabled = !busy,
                                        onToggle = { playlistId ->
                                            orphanSelection = if (playlistId in orphanSelection) {
                                                orphanSelection - playlistId
                                            } else {
                                                orphanSelection + playlistId
                                            }
                                        },
                                        onPreview = { selected ->
                                            pendingPreview = RepairPreview.RemoveOrphans(selected)
                                        }
                                    )
                                    LibraryHealthGroup.DUPLICATES -> DuplicatesSection(
                                        groups = current.duplicateGroups,
                                        onOpenDuplicates = onOpenDuplicates
                                    )
                                    else -> Unit
                                }
                            }
                        }
                    }
                }

                val activeUndo = undo
                if (activeUndo != null) {
                    DismissUndoBar(
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .padding(horizontal = 24.dp, vertical = 20.dp)
                            .fillMaxWidth(0.72f)
                            .height(52.dp),
                        message = undoMessage,
                        onUndo = undoAction,
                        onClose = { undo = null }
                    )
                }
            }
        }
    }

    pendingPreview?.let { preview ->
        RepairPreviewDialog(
            preview = preview,
            onDismiss = { pendingPreview = null },
            onConfirm = { confirmPreview(preview) }
        )
    }
}

private fun healthSubtitle(report: LibraryHealthReport?): String = when {
    report == null -> "Scanning the library..."
    report.isClean -> "${report.songs} songs and ${report.playlists} playlists are consistent with the disk."
    else -> "${report.issueCount} issue${if (report.issueCount == 1) "" else "s"} found. " +
        "Nothing is deleted automatically."
}

@Composable
private fun HealthGroupCard(
    title: String,
    count: Int,
    description: String,
    icon: DrawableResource,
    onClick: () -> Unit
) {
    val colors = MaterialTheme.colorScheme

    Surface(
        shape = RoundedCornerShape(18.dp),
        color = colors.surfaceContainer,
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .clickable(onClick = onClick)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = colors.primaryContainer,
                modifier = Modifier.size(40.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        painter = painterResource(icon),
                        contentDescription = null,
                        tint = colors.onPrimaryContainer,
                        modifier = Modifier.size(21.dp)
                    )
                }
            }
            Spacer(Modifier.width(12.dp))
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                Text(
                    text = "$title ($count)",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = colors.onSurface
                )
                Text(
                    text = description,
                    style = MaterialTheme.typography.bodySmall,
                    color = colors.onSurfaceVariant,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }
            Spacer(Modifier.width(8.dp))
            Icon(
                painter = painterResource(Res.drawable.ic_rounded_chevron_right),
                contentDescription = "Open $title",
                tint = colors.onSurfaceVariant.copy(alpha = 0.7f),
                modifier = Modifier.size(22.dp)
            )
        }
    }
}

@Composable
private fun MissingFilesSection(
    issues: List<MissingFileIssue>,
    enabled: Boolean,
    onLocate: (MissingFileIssue) -> Unit
) {
    val colors = MaterialTheme.colorScheme

    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        SectionIntro(
            text = "Choose the file that replaced each missing one. The song keeps its id, likes " +
                "and playlist entries; the old path and the files on disk are left alone."
        )
        if (issues.isEmpty()) {
            SectionEmpty("No missing files in this group.")
            return@Column
        }
        issues.forEach { issue ->
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = colors.surfaceContainer,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = issue.label,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold,
                        color = colors.onSurface
                    )
                    Text(
                        text = issue.path,
                        style = MaterialTheme.typography.bodySmall,
                        color = colors.onSurfaceVariant,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                    OutlinedButton(
                        enabled = enabled,
                        onClick = { onLocate(issue) }
                    ) {
                        Icon(
                            painter = painterResource(Res.drawable.ic_outlined_file_open),
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(Modifier.width(8.dp))
                        Text("Locate file")
                    }
                }
            }
        }
    }
}

@Composable
private fun BrokenArtworkSection(issues: List<BrokenArtworkIssue>) {
    val colors = MaterialTheme.colorScheme

    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        SectionIntro(
            text = "The cover path points at a picture that is no longer on disk. A library " +
                "re-scan or a new cover on the track replaces it; this page does not change covers."
        )
        if (issues.isEmpty()) {
            SectionEmpty("No broken covers in this group.")
            return@Column
        }
        issues.forEach { issue ->
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = colors.surfaceContainer,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        painter = painterResource(Res.drawable.ic_outlined_palette),
                        contentDescription = null,
                        tint = colors.onSurfaceVariant,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(Modifier.width(12.dp))
                    Column(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(2.dp)
                    ) {
                        Text(
                            text = issue.label,
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.SemiBold,
                            color = colors.onSurface
                        )
                        Text(
                            text = issue.artworkUri,
                            style = MaterialTheme.typography.bodySmall,
                            color = colors.onSurfaceVariant,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun OrphanEntriesSection(
    entries: List<OrphanPlaylistIssue>,
    selected: Set<String>,
    enabled: Boolean,
    onToggle: (String) -> Unit,
    onPreview: (List<OrphanPlaylistIssue>) -> Unit
) {
    val colors = MaterialTheme.colorScheme
    val selectedEntries = entries.filter { it.playlistId in selected }
    val selectedCount = selectedEntries.sumOf { it.missingSongIds.size }

    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        SectionIntro(
            text = "Only the playlist reference is removed. Song files, likes, lyrics and " +
                "playback history are not touched, and a reference that became valid again is kept."
        )
        if (entries.isEmpty()) {
            SectionEmpty("No orphan playlist entries in this group.")
            return@Column
        }
        entries.forEach { entry ->
            val checked = entry.playlistId in selected
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = colors.surfaceContainer,
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .clickable { onToggle(entry.playlistId) }
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Checkbox(
                        checked = checked,
                        onCheckedChange = { onToggle(entry.playlistId) }
                    )
                    Spacer(Modifier.width(4.dp))
                    Column(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(2.dp)
                    ) {
                        Text(
                            text = entry.playlistName,
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.SemiBold,
                            color = colors.onSurface
                        )
                        Text(
                            text = "${entry.missingSongIds.size} missing " +
                                (if (entry.missingSongIds.size == 1) "entry" else "entries"),
                            style = MaterialTheme.typography.bodySmall,
                            color = colors.onSurfaceVariant
                        )
                    }
                }
            }
        }
        Button(
            enabled = enabled && selectedCount > 0,
            onClick = { onPreview(selectedEntries) }
        ) {
            Icon(
                painter = painterResource(Res.drawable.ic_outlined_clear_all),
                contentDescription = null,
                modifier = Modifier.size(18.dp)
            )
            Spacer(Modifier.width(8.dp))
            Text(
                if (selectedCount == 0) {
                    "Select a playlist to preview"
                } else {
                    "Preview removal ($selectedCount)"
                }
            )
        }
    }
}

@Composable
private fun DuplicatesSection(
    groups: List<DuplicateFinder.DuplicateGroup>,
    onOpenDuplicates: () -> Unit
) {
    val colors = MaterialTheme.colorScheme

    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        SectionIntro(
            text = "Opening duplicates leaves every choice to you: the Duplicate Songs screen " +
                "lists each copy and removes one only after you ask. Nothing is deleted here."
        )
        if (groups.isEmpty()) {
            SectionEmpty("No duplicate groups in this group.")
            return@Column
        }
        groups.forEach { group ->
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = colors.surfaceContainer,
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .clickable(onClick = onOpenDuplicates)
            ) {
                Row(
                    modifier = Modifier.padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(2.dp)
                    ) {
                        Text(
                            text = group.title,
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.SemiBold,
                            color = colors.onSurface,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = "${group.artist} \u2022 ${group.songs.size} copies",
                            style = MaterialTheme.typography.bodySmall,
                            color = colors.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                    Icon(
                        painter = painterResource(Res.drawable.ic_rounded_chevron_right),
                        contentDescription = "Open duplicate songs",
                        tint = colors.onSurfaceVariant.copy(alpha = 0.7f),
                        modifier = Modifier.size(22.dp)
                    )
                }
            }
        }
        Button(onClick = onOpenDuplicates) {
            Icon(
                painter = painterResource(Res.drawable.ic_outlined_content_copy),
                contentDescription = null,
                modifier = Modifier.size(18.dp)
            )
            Spacer(Modifier.width(8.dp))
            Text("Open Duplicate Songs")
        }
    }
}

@Composable
private fun CleanHealthState(songs: Int, playlists: Int) {
    val colors = MaterialTheme.colorScheme

    Column(
        modifier = Modifier.fillMaxWidth().padding(top = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Surface(
            shape = RoundedCornerShape(24.dp),
            color = colors.secondaryContainer,
            modifier = Modifier.size(64.dp)
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    painter = painterResource(Res.drawable.ic_rounded_check_circle),
                    contentDescription = null,
                    tint = colors.onSecondaryContainer,
                    modifier = Modifier.size(36.dp)
                )
            }
        }
        Text(
            text = "No issues found",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold,
            color = colors.onSurface
        )
        Text(
            text = "$songs songs and $playlists playlists point at files that exist.",
            style = MaterialTheme.typography.bodyMedium,
            color = colors.onSurfaceVariant
        )
    }
}

@Composable
private fun SectionIntro(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant
    )
}

@Composable
private fun SectionEmpty(text: String) {
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surfaceContainer,
        modifier = Modifier.fillMaxWidth()
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(16.dp)
        )
    }
}

@Composable
private fun RepairPreviewDialog(
    preview: RepairPreview,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit
) {
    val colors = MaterialTheme.colorScheme

    when (preview) {
        is RepairPreview.Relink -> AlertDialog(
            onDismissRequest = onDismiss,
            title = { Text("Relink this song?") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = preview.issue.label,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold
                    )
                    PreviewPath("Old path", preview.issue.path)
                    PreviewPath("New file", preview.file.absolutePath)
                    Text(
                        text = "The song keeps its id, likes and playlist entries. No file is " +
                            "copied, moved or deleted.",
                        style = MaterialTheme.typography.bodySmall,
                        color = colors.onSurfaceVariant
                    )
                }
            },
            confirmButton = {
                Button(onClick = onConfirm) { Text("Relink") }
            },
            dismissButton = {
                TextButton(onClick = onDismiss) { Text("Cancel") }
            }
        )

        is RepairPreview.RemoveOrphans -> {
            val count = preview.entries.sumOf { it.missingSongIds.size }
            AlertDialog(
                onDismissRequest = onDismiss,
                title = { Text("Remove orphan playlist entries?") },
                text = {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        preview.entries.forEach { entry ->
                            Text(
                                text = "${entry.playlistName}: ${entry.missingSongIds.size} " +
                                    (if (entry.missingSongIds.size == 1) "entry" else "entries"),
                                style = MaterialTheme.typography.bodyMedium
                            )
                        }
                        Text(
                            text = "$count ${if (count == 1) "entry" else "entries"} will be removed " +
                                "from ${preview.entries.size} " +
                                (if (preview.entries.size == 1) "playlist." else "playlists."),
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.SemiBold
                        )
                        Text(
                            text = "Song files, likes, lyrics and playback history are not " +
                                "touched. The bar after the change restores the entries.",
                            style = MaterialTheme.typography.bodySmall,
                            color = colors.onSurfaceVariant
                        )
                    }
                },
                confirmButton = {
                    Button(
                        onClick = onConfirm,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = colors.error,
                            contentColor = colors.onError
                        )
                    ) {
                        Text("Remove entries")
                    }
                },
                dismissButton = {
                    TextButton(onClick = onDismiss) { Text("Cancel") }
                }
            )
        }
    }
}

@Composable
private fun PreviewPath(label: String, path: String) {
    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.primary
        )
        Text(
            text = path,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 3,
            overflow = TextOverflow.Ellipsis
        )
    }
}
