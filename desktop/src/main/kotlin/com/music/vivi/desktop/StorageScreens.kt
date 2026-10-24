package com.music.vivi.desktop

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.SdStorage
import androidx.compose.material.icons.filled.Lyrics
import androidx.compose.material.icons.filled.Storage
import androidx.compose.material.icons.filled.Translate
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.setValue
import androidx.compose.runtime.getValue
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.key.key
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import java.io.File
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

private fun dirSize(dir: File): Long =
    dir.walkBottomUp().filter { it.isFile }.sumOf { it.length() }

/**
 * Storage: how much disk the app's own files take, and the buttons that free it.
 *
 * Ported from the mobile Storage screen, which is a set of cards (a group title
 * over a card of rows) rather than the two loose lines and a button that stood
 * here. The port keeps the structure and drops the mobile's sliders: those set
 * an image-cache and a song-cache ceiling that only exist on Android
 * (`MaxImageCacheSizeKey` / `MaxSongCacheSizeKey` and the two Coil / player
 * caches), so showing them on the desktop would be a control wired to nothing.
 * What the desktop really keeps on disk is what is listed here, one card per
 * kind: the audio the extractor cached, the lyrics fetched for it, the AI's
 * translations of those lyrics, and the downloaded installers. All four can be
 * removed from this screen, and removing one never touches the others.
 */
@Composable
fun StorageSection(language: String) {
    val scope = rememberCoroutineScope()
    // Three caches, three rows, three buttons (see the note above the section).
    // The audio one is the extractor's directory MINUS the two lyrics folders:
    // they live inside it on disk, so counting the whole directory would report
    // the same bytes three times and "clear audio" would take the lyrics away
    // with it.
    val cacheRoot = remember { File(System.getProperty("user.home"), ".vivimusic/cache") }
    val lyricsDir = remember { File(cacheRoot, "lyrics") }
    val aiLyricsDir = remember { LyricsTranslator.cacheDir }
    var audioSize by remember { mutableStateOf<Long?>(null) }
    var lyricsSize by remember { mutableStateOf<Long?>(null) }
    var aiLyricsSize by remember { mutableStateOf<Long?>(null) }
    var installerSize by remember { mutableStateOf<Long?>(null) }
    var audioCleared by remember { mutableStateOf(false) }
    var lyricsCleared by remember { mutableStateOf(false) }
    var aiLyricsCleared by remember { mutableStateOf(false) }
    var installersDeleted by remember { mutableStateOf(false) }

    /** The extractor cache without the two lyrics folders inside it. */
    fun audioBytes(): Long = dirSize(cacheRoot) - dirSize(lyricsDir) - dirSize(aiLyricsDir)

    /** Deletes the audio cache and leaves both lyrics caches alone. */
    fun clearAudio() {
        cacheRoot.listFiles()?.forEach { child ->
            if (child != lyricsDir && child != aiLyricsDir) child.deleteRecursively()
        }
        LyricsCache.forget()
    }

    // Every size is measured off the main thread (a cache holds thousands of
    // files) and re-measured after every clear, so the numbers follow the
    // buttons instead of going stale.
    fun refresh() {
        scope.launch {
            audioSize = withContext(Dispatchers.IO) { audioBytes() }
            lyricsSize = withContext(Dispatchers.IO) { dirSize(lyricsDir) }
            aiLyricsSize = withContext(Dispatchers.IO) { dirSize(aiLyricsDir) }
            // Only the installers the update counter counts, so what this row
            // shows is exactly what its button deletes: the installer of a
            // downloaded, not yet installed update is not included and is not
            // removed (see UpdateState.deleteAllInstallers).
            installerSize = withContext(Dispatchers.IO) {
                UpdateDownloader.installedInstallers(AppInfo.FULL_VERSION).sumOf { it.length() }
            }
        }
    }

    LaunchedEffect(Unit) { refresh() }

    // The head of the screen is one card: what this is, the total it comes to and
    // the four parts it is made of as one bar. The numbers below are the same
    // four, row by row, with the action that empties each; the bar is the only
    // place where they read as a proportion of the whole.
    //
    // The four parts in the preview's own order and roles, so the same cache keeps
    // the same colour in the bar, in the legend and on its row. The sizes are
    // null while they are being measured, which is why the total only appears
    // once all four are known.
    val parts = listOf(
        M3BarPart(
            Localization.get(language, "cache_audio"),
            MaterialTheme.colorScheme.primary,
            (audioSize ?: 0L).toFloat(),
            audioSize?.let { formatBytes(it) },
        ),
        M3BarPart(
            Localization.get(language, "cache_lyrics"),
            MaterialTheme.colorScheme.tertiary,
            (lyricsSize ?: 0L).toFloat(),
            lyricsSize?.let { formatBytes(it) },
        ),
        M3BarPart(
            Localization.get(language, "cache_ai_lyrics"),
            MaterialTheme.colorScheme.secondary,
            (aiLyricsSize ?: 0L).toFloat(),
            aiLyricsSize?.let { formatBytes(it) },
        ),
        M3BarPart(
            Localization.get(language, "installers_downloaded"),
            MaterialTheme.colorScheme.outline,
            (installerSize ?: 0L).toFloat(),
            installerSize?.let { formatBytes(it) },
        ),
    )
    val known = listOf(audioSize, lyricsSize, aiLyricsSize, installerSize)
    val totalBytes = if (known.all { it != null }) known.sumOf { it ?: 0L } else null
    M3ScreenHero(
        icon = Icons.Filled.SdStorage,
        title = Localization.get(language, "storage"),
        description = Localization.get(language, "storage_desc"),
        trailing = {
            Column(horizontalAlignment = Alignment.End) {
                Text(
                    totalBytes?.let { formatBytes(it) } ?: "...",
                    style = MaterialTheme.typography.headlineMedium,
                )
                Text(
                    Localization.get(language, "storage_total"),
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        },
        content = {
            M3SegmentedBar(parts)
            // The bar says the proportions; the legend says which colour is which
            // and repeats the figure the row below carries.
            Spacer(Modifier.height(12.dp))
            M3SegmentedLegend(parts)
        },
    )

    Spacer(Modifier.height(16.dp))

    // One row per kind, as the preview draws it: the number and the action that
    // empties it belong to the same row - they were two rows per kind, a size
    // above a "Clear cache" that named the cache nowhere.
    M3SettingsGroup(
        title = Localization.get(language, "cache_size"),
        items = listOf(
            storageRow(
                language = language,
                icon = Icons.Filled.GraphicEq,
                titleKey = "cache_audio",
                descriptionKey = "privacy_desc",
                size = audioSize,
                actionKey = "clear_cache",
                onAction = {
                    scope.launch {
                        withContext(Dispatchers.IO) { clearAudio() }
                        audioCleared = true
                        audioSize = withContext(Dispatchers.IO) { audioBytes() }
                    }
                },
            ),
            storageRow(
                language = language,
                icon = Icons.Filled.Lyrics,
                titleKey = "cache_lyrics",
                descriptionKey = "lyrics",
                size = lyricsSize,
                actionKey = "clear_cache",
                onAction = {
                    scope.launch {
                        withContext(Dispatchers.IO) { LyricsCache.clearDisk() }
                        lyricsCleared = true
                        lyricsSize = withContext(Dispatchers.IO) { dirSize(lyricsDir) }
                    }
                },
            ),
            storageRow(
                language = language,
                icon = Icons.Filled.Translate,
                titleKey = "cache_ai_lyrics",
                descriptionKey = "ai_lyrics_translation",
                size = aiLyricsSize,
                actionKey = "clear_cache",
                onAction = {
                    scope.launch {
                        withContext(Dispatchers.IO) { LyricsTranslator.clearDiskCache() }
                        aiLyricsCleared = true
                        aiLyricsSize = withContext(Dispatchers.IO) { dirSize(aiLyricsDir) }
                    }
                },
            ),
        ),
    )

    Spacer(Modifier.height(16.dp))

    M3SettingsGroup(
        title = Localization.get(language, "installers_downloaded"),
        items = listOf(
            storageRow(
                language = language,
                icon = Icons.Filled.Download,
                titleKey = "installers_downloaded",
                size = installerSize,
                actionKey = "delete_installers",
                onAction = {
                    UpdateState.deleteAllInstallers()
                    installersDeleted = true
                    refresh()
                },
            ),
        ),
    )

    // The confirmations live below the cards rather than in a dialog: on the
    // mobile screen these actions ask first (they are irreversible), and the
    // desktop does the same by showing what happened where the user is looking.
    if (audioCleared) {
        Text(
            Localization.get(language, "cache_cleared") + " — " + Localization.get(language, "cache_audio"),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.padding(top = 12.dp, start = 6.dp),
        )
    }
    if (lyricsCleared) {
        Text(
            Localization.get(language, "cache_cleared") + " — " + Localization.get(language, "cache_lyrics"),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.padding(top = 12.dp, start = 6.dp),
        )
    }
    if (aiLyricsCleared) {
        Text(
            Localization.get(language, "cache_cleared") + " — " + Localization.get(language, "cache_ai_lyrics"),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.padding(top = 12.dp, start = 6.dp),
        )
    }
    if (installersDeleted) {
        Text(
            Localization.get(language, "installers_deleted"),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.padding(top = 12.dp, start = 6.dp),
        )
    }
}

/**
 * One kind of file the app keeps: its glyph, what it is, its size and the action
 * that empties it, all on one row.
 *
 * The size used to be a row of its own with the button under it, so a screen of
 * four kinds read as eight rows and the button named its cache nowhere. The
 * number and the action are the same subject, so they share the row: the figure
 * in the trailing slot, the action beside it as a tonal button. [size] is null
 * while it is being measured, and the row says so instead of showing a 0 B; an
 * emptied kind keeps its button, disabled, as the preview draws it.
 */
@Composable
private fun storageRow(
    language: String,
    icon: ImageVector,
    titleKey: String,
    size: Long?,
    actionKey: String,
    onAction: () -> Unit,
    descriptionKey: String? = null,
): M3SettingsItem = M3SettingsItem(
    icon = icon,
    title = { Text(Localization.get(language, titleKey)) },
    description = descriptionKey?.let { key ->
        @Composable { Text(Localization.get(language, key)) }
    },
    trailing = {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(
                size?.let { formatBytes(it) } ?: "...",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            FilledTonalButton(onClick = onAction, enabled = (size ?: 0L) > 0L) {
                Text(Localization.get(language, actionKey), style = MaterialTheme.typography.labelLarge)
            }
        }
    },
)
