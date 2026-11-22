package com.music.vivi.desktop.presentation.screens.settings

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
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
import com.music.vivi.desktop.data.library.LibraryHealthChecker
import com.music.vivi.desktop.data.library.LibraryHealthGroup
import com.music.vivi.desktop.data.library.LibraryHealthReport
import com.music.vivi.desktop.generated.resources.Res
import com.music.vivi.desktop.generated.resources.ic_rounded_check_circle
import com.music.vivi.desktop.generated.resources.ic_rounded_chevron_right
import com.music.vivi.desktop.generated.resources.ic_outlined_error_outline
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.jetbrains.compose.resources.painterResource
import org.koin.compose.koinInject

@Composable
fun LibraryHealthCard(
    checker: LibraryHealthChecker = koinInject(),
    onOpenDetail: (LibraryHealthGroup) -> Unit = {}
) {
    val colors = MaterialTheme.colorScheme
    val scope = rememberCoroutineScope()
    var busy by remember { mutableStateOf(false) }
    var report by remember { mutableStateOf<LibraryHealthReport?>(null) }

    SettingsSubsection(title = "LIBRARY HEALTH") {
        Surface(
            shape = RoundedCornerShape(20.dp),
            color = colors.surfaceContainer,
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text(
                    text = "Check what the library points at",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = colors.onSurface
                )
                Text(
                    text = "Missing song files, covers that no longer exist, playlist entries whose " +
                        "song is gone and duplicate tracks. The check only reads, nothing is deleted " +
                        "and every repair asks for confirmation first.",
                    style = MaterialTheme.typography.bodySmall,
                    color = colors.onSurfaceVariant
                )
                Row {
                    Button(
                        enabled = !busy,
                        onClick = {
                            busy = true
                            scope.launch {
                                report = withContext(Dispatchers.IO) { checker.scan() }
                                busy = false
                            }
                        }
                    ) {
                        Icon(
                            painter = painterResource(Res.drawable.ic_rounded_check_circle),
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(Modifier.width(8.dp))
                        Text(if (busy) "Checking..." else "Check library")
                    }
                }

                report?.let { result ->
                    HorizontalDivider(color = colors.outlineVariant.copy(alpha = 0.4f))
                    HealthSummary(result, onOpenDetail)
                }
            }
        }
    }
}

@Composable
private fun HealthSummary(
    report: LibraryHealthReport,
    onOpenDetail: (LibraryHealthGroup) -> Unit
) {
    val colors = MaterialTheme.colorScheme
    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        Icon(
            painter = painterResource(
                if (report.isClean) Res.drawable.ic_rounded_check_circle else Res.drawable.ic_outlined_error_outline
            ),
            contentDescription = null,
            tint = if (report.isClean) colors.primary else colors.error,
            modifier = Modifier.size(20.dp)
        )
        Text(
            text = if (report.isClean) {
                "${report.songs} songs and ${report.playlists} playlists are consistent with the disk."
            } else {
                "${report.issueCount} issue${if (report.issueCount == 1) "" else "s"} found."
            },
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.SemiBold,
            color = colors.onSurface
        )
    }
    if (!report.isClean) {
        Text(
            text = "Select a group to review it and run a repair.",
            style = MaterialTheme.typography.bodySmall,
            color = colors.onSurfaceVariant
        )
        val missingPreviews = report.missingFiles.take(3).map { it.label }
        val artworkPreviews = report.brokenArtwork.take(3).map { it.label }
        val orphanPreviews = report.orphanPlaylistEntries.take(3).map {
            "${it.playlistName} (${it.missingSongIds.size})"
        }
        val duplicatePreviews = report.duplicateGroups.take(3).map {
            "${it.title} - ${it.artist} (${it.songs.size} copies)"
        }

        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            HealthGroupRow(
                count = report.missingFiles.size,
                headline = "Missing song files: ${report.missingFiles.size}",
                previews = missingPreviews,
                hiddenText = healthMoreText(report.missingFiles.size, missingPreviews.size),
                group = LibraryHealthGroup.MISSING_FILES,
                onOpen = onOpenDetail
            )
            HealthGroupRow(
                count = report.brokenArtwork.size,
                headline = "Broken covers: ${report.brokenArtwork.size}",
                previews = artworkPreviews,
                hiddenText = healthMoreText(report.brokenArtwork.size, artworkPreviews.size),
                group = LibraryHealthGroup.BROKEN_ARTWORK,
                onOpen = onOpenDetail
            )
            HealthGroupRow(
                count = report.orphanEntryCount,
                headline = orphanEntriesHeadline(report),
                previews = orphanPreviews,
                hiddenText = healthMoreText(
                    total = report.orphanPlaylistEntries.size,
                    shown = orphanPreviews.size,
                    singular = "playlist",
                    plural = "playlists"
                ),
                group = LibraryHealthGroup.ORPHAN_ENTRIES,
                onOpen = onOpenDetail
            )
            HealthGroupRow(
                count = report.duplicateGroups.size,
                headline = "Duplicate groups: ${report.duplicateGroups.size}",
                previews = duplicatePreviews,
                hiddenText = healthMoreText(report.duplicateGroups.size, duplicatePreviews.size),
                group = LibraryHealthGroup.DUPLICATES,
                onOpen = onOpenDetail
            )
        }
    }
}

/**
 * The line that closes a preview list. [total] and [shown] count the same kind
 * of item; [singular] and [plural] name it when the row previews playlists
 * rather than the entries themselves.
 */
internal fun healthMoreText(
    total: Int,
    shown: Int,
    singular: String = "",
    plural: String = ""
): String? {
    val remaining = (total - shown).coerceAtLeast(0)
    if (remaining == 0) return null
    val unit = when {
        singular.isBlank() -> ""
        remaining == 1 -> " $singular"
        else -> " ${plural.ifBlank { singular }}"
    }
    return "... and $remaining more$unit"
}

internal fun orphanEntriesHeadline(report: LibraryHealthReport): String {
    val playlists = report.orphanPlaylistEntries.size
    return "Playlist entries without a song: ${report.orphanEntryCount} in $playlists " +
        (if (playlists == 1) "playlist" else "playlists")
}

@Composable
private fun HealthGroupRow(
    count: Int,
    headline: String,
    previews: List<String>,
    hiddenText: String?,
    group: LibraryHealthGroup,
    onOpen: (LibraryHealthGroup) -> Unit
) {
    if (count == 0) return
    val colors = MaterialTheme.colorScheme

    Surface(
        shape = RoundedCornerShape(14.dp),
        color = colors.surfaceContainerHigh,
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .clickable { onOpen(group) }
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                Text(
                    text = headline,
                    style = MaterialTheme.typography.bodySmall,
                    fontWeight = FontWeight.SemiBold,
                    color = colors.onSurface
                )
                previews.forEach { item ->
                    Text(
                        text = "\u2022 $item",
                        style = MaterialTheme.typography.bodySmall,
                        color = colors.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                hiddenText?.let { more ->
                    Text(
                        text = more,
                        style = MaterialTheme.typography.bodySmall,
                        color = colors.onSurfaceVariant
                    )
                }
            }
            Icon(
                painter = painterResource(Res.drawable.ic_rounded_chevron_right),
                contentDescription = "Open $headline",
                tint = colors.onSurfaceVariant.copy(alpha = 0.7f),
                modifier = Modifier.size(20.dp)
            )
        }
    }
}
