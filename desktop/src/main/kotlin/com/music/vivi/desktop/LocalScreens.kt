package com.music.vivi.desktop

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.music.innertube.models.Artist
import com.music.innertube.models.SongItem
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.awt.Desktop
import java.io.File
import java.util.Locale

private val AUDIO_EXTENSIONS = setOf("mp3", "m4a", "aac", "flac", "wav", "ogg", "opus", "wma")

private data class LocalAudioFile(
    val file: File,
    val title: String,
    val artist: String,
)

/**
 * Completed audio cache files, keyed by their video id. A file whose track is
 * not in the local history still appears; it keeps the id as its name instead
 * of pretending to know a title.
 */
@Composable
fun DownloadsScreen(
    language: String,
    initialFiles: List<File>,
    onPlaySong: (SongItem) -> Unit,
    onDelete: (String) -> Unit,
    onOpenFolder: () -> Unit,
    onClearAll: () -> Unit,
) {
    val history by HistoryStore.tracks.collectAsState()
    val byId = remember(history) { history.associateBy { it.videoId } }
    var files by remember { mutableStateOf(initialFiles) }
    LaunchedEffect(initialFiles) { files = initialFiles }

    Column(Modifier.fillMaxSize().background(BitColors.page()).padding(start = 16.dp, end = 18.dp, top = 18.dp)) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Text(
                Localization.get(language, "downloads"),
                style = MaterialTheme.typography.headlineMedium.copy(fontSize = 30.sp, fontWeight = FontWeight.ExtraBold),
                color = if (isAppInDarkTheme()) Color.White else MaterialTheme.colorScheme.onBackground,
                modifier = Modifier.weight(1f),
            )
            if (files.isNotEmpty()) {
                Text(
                    Localization.get(language, "clear_all"),
                    style = MaterialTheme.typography.labelLarge,
                    color = BitColors.secondary(),
                    modifier = Modifier
                        .clip(RoundedCornerShape(7.dp))
                        .clickable {
                            files = emptyList()
                            onClearAll()
                        }
                        .padding(horizontal = 10.dp, vertical = 7.dp),
                )
                Spacer(Modifier.width(4.dp))
            }
            Icon(
                Icons.Filled.Folder,
                contentDescription = Localization.get(language, "open_folder"),
                tint = BitColors.secondary(),
                modifier = Modifier
                    .size(30.dp)
                    .clip(RoundedCornerShape(7.dp))
                    .clickable { onOpenFolder() }
                    .padding(6.dp),
            )
        }
        Text(
            if (files.isEmpty()) Localization.get(language, "downloads_empty") else "${files.size} ${Localization.get(language, "tracks")}",
            style = MaterialTheme.typography.bodyMedium,
            color = BitColors.secondary(),
            modifier = Modifier.padding(top = 6.dp, bottom = 14.dp),
        )

        if (files.isEmpty()) {
            EmptyLocalState(
                icon = Icons.Filled.Download,
                text = Localization.get(language, "downloads_empty"),
            )
            return@Column
        }

        LazyColumn(Modifier.weight(1f).fillMaxWidth()) {
            items(files, key = { it.absolutePath }, contentType = { "download" }) { file ->
                val id = file.nameWithoutExtension
                val entry = byId[id]
                val song = entry?.asSong()
                DownloadRow(
                    language = language,
                    title = entry?.title ?: id,
                    subtitle = entry?.artist ?: Localization.get(language, "cached_audio"),
                    thumbnail = entry?.thumbnail,
                    size = file.length(),
                    canPlay = song != null,
                    onPlay = { song?.let(onPlaySong) },
                    onDelete = {
                        files = files.filterNot { it.absolutePath == file.absolutePath }
                        onDelete(id)
                    },
                )
            }
        }
    }
}

@Composable
private fun DownloadRow(
    language: String,
    title: String,
    subtitle: String,
    thumbnail: String?,
    size: Long,
    canPlay: Boolean,
    onPlay: () -> Unit,
    onDelete: () -> Unit,
) {
    Row(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(7.dp))
            .clickable(enabled = canPlay, onClick = onPlay)
            .padding(vertical = 7.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Thumbnail(thumbnail, Modifier.size(48.dp).clip(RoundedCornerShape(6.dp)))
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(
                title,
                style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.SemiBold),
                color = if (isAppInDarkTheme()) Color.White else MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                "$subtitle - ${formatLocalBytes(size)}",
                style = MaterialTheme.typography.bodySmall,
                color = BitColors.secondary(),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
        Tooltip(Localization.get(language, "delete")) {
            M3IconButton(onClick = onDelete) {
                Icon(Icons.Filled.Delete, contentDescription = Localization.get(language, "delete"), modifier = Modifier.size(18.dp))
            }
        }
    }
    HorizontalDivider(color = BitColors.Divider)
}

/**
 * Local audio found under the user's Music and Downloads folders. This screen
 * reads the filesystem only; it does not scan the network and it does not
 * invent tracks. Playback opens the file in the system's default audio player,
 * because VIVI's engine is a YouTube/AAC pipeline, not a general local decoder.
 */
@Composable
fun LocalMusicScreen(language: String) {
    var files by remember { mutableStateOf<List<LocalAudioFile>>(emptyList()) }
    var scanning by remember { mutableStateOf(true) }
    var scanKey by remember { mutableStateOf(0) }
    var error by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(scanKey) {
        scanning = true
        error = null
        files = withContext(Dispatchers.IO) { scanLocalAudio() }
        scanning = false
    }

    Column(Modifier.fillMaxSize().background(BitColors.page()).padding(start = 16.dp, end = 18.dp, top = 18.dp)) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(
                    Localization.get(language, "local_music"),
                    style = MaterialTheme.typography.headlineMedium.copy(fontSize = 30.sp, fontWeight = FontWeight.ExtraBold),
                    color = if (isAppInDarkTheme()) Color.White else MaterialTheme.colorScheme.onBackground,
                )
                Text(
                    Localization.get(language, "local_music_subtitle"),
                    style = MaterialTheme.typography.bodyMedium,
                    color = BitColors.secondary(),
                    modifier = Modifier.padding(top = 4.dp),
                )
            }
            Tooltip(Localization.get(language, "rescan")) {
                M3IconButton(onClick = { scanKey++ }) {
                    Icon(
                        Icons.Filled.Refresh,
                        contentDescription = Localization.get(language, "rescan"),
                        tint = BitColors.secondary(),
                        modifier = Modifier.size(20.dp),
                    )
                }
            }
        }

        Row(
            Modifier.fillMaxWidth().padding(top = 14.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            FolderChip(File(System.getProperty("user.home"), "Music"))
            FolderChip(File(System.getProperty("user.home"), "Downloads"))
        }

        Text(
            when {
                scanning -> Localization.get(language, "scanning")
                error != null -> error.orEmpty()
                files.isEmpty() -> Localization.get(language, "local_music_empty")
                else -> "${files.size} ${Localization.get(language, "tracks")}"
            },
            style = MaterialTheme.typography.bodyMedium,
            color = BitColors.secondary(),
            modifier = Modifier.padding(top = 14.dp, bottom = 8.dp),
        )

        if (files.isEmpty() && !scanning) {
            EmptyLocalState(icon = Icons.Filled.MusicNote, text = Localization.get(language, "local_music_empty"))
            return@Column
        }

        LazyColumn(Modifier.weight(1f).fillMaxWidth()) {
            items(files, key = { it.file.absolutePath }, contentType = { "local" }) { local ->
                Row(
                    Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(7.dp))
                        .clickable { openFile(local.file) { error = it } }
                        .padding(vertical = 7.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Box(
                        Modifier.size(44.dp).clip(RoundedCornerShape(6.dp)).background(BitColors.Hover),
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(Icons.Filled.MusicNote, contentDescription = null, tint = BitColors.secondary(), modifier = Modifier.size(20.dp))
                    }
                    Spacer(Modifier.width(12.dp))
                    Column(Modifier.weight(1f)) {
                        Text(
                            local.title,
                            style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.SemiBold),
                            color = if (isAppInDarkTheme()) Color.White else MaterialTheme.colorScheme.onSurface,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                        Text(
                            "${local.artist} - ${formatLocalBytes(local.file.length())}",
                            style = MaterialTheme.typography.bodySmall,
                            color = BitColors.secondary(),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                    Tooltip(Localization.get(language, "open")) {
                        M3IconButton(onClick = { openFile(local.file) { error = it } }) {
                            Icon(Icons.Filled.PlayArrow, contentDescription = Localization.get(language, "open"), modifier = Modifier.size(20.dp))
                        }
                    }
                }
                HorizontalDivider(color = BitColors.Divider)
            }
        }
    }
}

@Composable
private fun FolderChip(folder: File) {
    Surface(
        color = BitColors.Search,
        shape = RoundedCornerShape(7.dp),
        modifier = Modifier.clip(RoundedCornerShape(7.dp)).clickable { openFolder(folder) },
    ) {
        Row(Modifier.padding(horizontal = 10.dp, vertical = 7.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Filled.Folder, contentDescription = null, tint = BitColors.secondary(), modifier = Modifier.size(14.dp))
            Spacer(Modifier.width(6.dp))
            Text(folder.name, style = MaterialTheme.typography.labelMedium, color = BitColors.secondary())
        }
    }
}

@Composable
private fun EmptyLocalState(icon: androidx.compose.ui.graphics.vector.ImageVector, text: String) {
    Column(
        Modifier.fillMaxWidth().padding(top = 48.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Icon(icon, contentDescription = null, tint = BitColors.Secondary, modifier = Modifier.size(38.dp))
        Spacer(Modifier.height(10.dp))
        Text(text, style = MaterialTheme.typography.bodyMedium, color = BitColors.secondary())
    }
}

private fun scanLocalAudio(): List<LocalAudioFile> {
    val home = System.getProperty("user.home") ?: return emptyList()
    val roots = listOf(File(home, "Music"), File(home, "Downloads"))
    val found = ArrayList<LocalAudioFile>()
    roots.forEach { root ->
        if (!root.isDirectory) return@forEach
        runCatching {
            root.walkTopDown()
                .onEnter { it.name != ".git" && it.name != "node_modules" }
                .filter { it.isFile && it.extension.lowercase(Locale.ROOT) in AUDIO_EXTENSIONS }
                .take(2_000)
                .forEach { file ->
                    val base = file.nameWithoutExtension.trim()
                    val split = base.split(" - ", limit = 2)
                    val artist = if (split.size == 2) split[0].trim() else file.parentFile?.name.orEmpty().ifBlank { "Local" }
                    val title = if (split.size == 2) split[1].trim() else base
                    found += LocalAudioFile(file, title.ifBlank { base }, artist)
                }
        }
    }
    return found.distinctBy { it.file.absolutePath }.sortedBy { it.title.lowercase(Locale.ROOT) }
}

private fun openFolder(folder: File) {
    runCatching { Desktop.getDesktop().open(folder.takeIf { it.isDirectory } ?: folder.parentFile) }
}

private fun openFile(file: File, onError: (String) -> Unit) {
    runCatching { Desktop.getDesktop().open(file) }
        .onFailure { onError(it.message ?: "Cannot open ${file.name}") }
}

private fun formatLocalBytes(bytes: Long): String = when {
    bytes >= 1_048_576 -> String.format(Locale.ROOT, "%.1f MB", bytes / 1_048_576.0)
    bytes >= 1_024 -> "${bytes / 1_024} KB"
    else -> "$bytes B"
}

private fun HistoryEntry.asSong(): SongItem = SongItem(
    id = videoId,
    title = title,
    artists = artist.split(",").map { it.trim() }.filter { it.isNotEmpty() }.map { Artist(name = it, id = null) },
    duration = if (durationMs > 0) (durationMs / 1000).toInt() else null,
    thumbnail = thumbnail?.takeIf { it.isNotBlank() } ?: "https://i.ytimg.com/vi/$videoId/hqdefault.jpg",
)
