package com.music.vivi.desktop.presentation.screens.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.music.vivi.desktop.data.model.StatsTimeRange
import com.music.vivi.desktop.data.store.LocalLibraryStore
import com.music.vivi.desktop.data.youtube.BrowserCookies
import com.music.vivi.desktop.presentation.components.StatsOverviewCard
import com.music.vivi.desktop.presentation.viewmodel.PlayerViewModel
import com.music.vivi.desktop.utils.DesktopPathProvider
import com.music.vivi.desktop.utils.DirectoryPicker
import com.music.vivi.desktop.data.backup.FullBackup
import com.music.vivi.desktop.generated.resources.Res
import com.music.vivi.desktop.generated.resources.ic_outlined_file_download
import com.music.vivi.desktop.generated.resources.ic_outlined_file_upload
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.jetbrains.compose.resources.painterResource
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.roundToInt

/**
 * The operations the imported 1.55 pages perform.
 *
 * Production passes [DefaultLegacy155Actions], which delegates to the same
 * holders, repositories and backup implementation the 1.56 pages use. The
 * screen takes the interface so a widget test can record the action a row
 * triggers without opening a file picker or a Spotify session.
 */
/** Pages whose body is the real 1.56-backed content instead of the generic catalog. */
val Legacy155RealPages = setOf(
    SettingsSection.V155_ACCOUNT,
    SettingsSection.V155_BACKUP,
    SettingsSection.V155_STORAGE,
    SettingsSection.V155_WRAPPED,
    SettingsSection.V155_SPOTIFY,
)

/** Options the real content above already owns, so the catalog does not draw them twice. */
val Legacy155RealPageKeys: Map<SettingsSection, Set<String>> = mapOf(
    SettingsSection.V155_BACKUP to setOf("autoBackupEnabled", "autoBackupWeekly", "backupBeforeUpdate", "lastAutoBackupAt", "backupFile"),
    SettingsSection.V155_ACCOUNT to setOf("accountName", "accountEmail"),
    SettingsSection.V155_STORAGE to setOf("audioCacheLimitMb", "audioCacheEnabled", "musicDirectories", "librarySize"),
    SettingsSection.V155_WRAPPED to setOf("wrappedTracks", "wrappedMinutes", "wrappedTopArtist"),
    SettingsSection.V155_SPOTIFY to setOf("spotifyAccount", "spotifyImportState"),
)

interface Legacy155Actions {
    fun signInYouTube()
    fun signOutYouTube()
    fun refreshYouTube()
    fun detectProfiles()
    fun useProfile(profile: BrowserCookies.Profile)
    fun switchAccount(id: String)
    fun removeAccount(id: String)
    fun exportFullBackup()
    fun restoreFullBackup()
    fun clearAudioCache()
    fun clearLyricsCache()
    fun deleteInstallers()
    fun connectSpotify(spDc: String, spKey: String)
    fun disconnectSpotify()
    fun refreshSpotify()
    fun importSpotify(selected: List<String>)
}

/** Delegates every action to the implementation the 1.56 surface already uses. */
class DefaultLegacy155Actions(
    private val viewModel: PlayerViewModel,
    private val scope: CoroutineScope,
    private val store: LocalLibraryStore? = null,
    private val storeFile: File = DesktopPathProvider.syncStateFile,
    private val exportPicker: ((String, (File) -> Unit) -> Unit)? = null,
    private val restorePicker: (((File) -> Unit) -> Unit)? = null,
) : Legacy155Actions {

    private fun pickExport(name: String, onPicked: (File) -> Unit) {
        if (exportPicker != null) exportPicker.invoke(name, onPicked)
        else DirectoryPicker.saveVivideBackupFile(defaultFileName = name, onSelected = onPicked)
    }

    private fun pickRestore(onPicked: (File) -> Unit) {
        if (restorePicker != null) restorePicker.invoke(onPicked)
        else DirectoryPicker.chooseVivideBackupFile(onSelected = onPicked)
    }

    override fun signInYouTube() = viewModel.youtubeAccountStateHolder.signInWithWebView()

    override fun signOutYouTube() = viewModel.youtubeAccountStateHolder.signOut()

    override fun refreshYouTube() = viewModel.youtubeAccountStateHolder.refresh()

    override fun detectProfiles() = viewModel.youtubeAccountStateHolder.detectProfiles()

    override fun useProfile(profile: BrowserCookies.Profile) =
        viewModel.youtubeAccountStateHolder.signIn(profile)

    override fun switchAccount(id: String) = viewModel.youtubeAccountStateHolder.switchTo(id)

    override fun removeAccount(id: String) = viewModel.youtubeAccountStateHolder.removeAccount(id)

    override fun exportFullBackup() {
        pickExport(
            "AxolotlMusic_${
                SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
            }.${FullBackup.EXTENSION}"
        ) { target ->
            scope.launch {
                withContext(Dispatchers.IO) {
                    FullBackup.export(target, storeFile)
                }
            }
        }
    }

    override fun restoreFullBackup() {
        pickRestore { file ->
            scope.launch {
                withContext(Dispatchers.IO) {
                    FullBackup.restore(file, storeFile)
                }.onSuccess {
                    store?.reload()
                    store?.notifyAllTables()
                }
            }
        }
    }

    override fun clearAudioCache() = viewModel.clearAudioCache()

    override fun clearLyricsCache() = viewModel.resetAllLyrics()

    override fun deleteInstallers() {
        viewModel.updateStateHolder?.deleteDownloadedInstallers()
    }

    override fun connectSpotify(spDc: String, spKey: String) {
        // The Spotify manager is added in the same change as the screen that
        // uses it; keeping the call here means the screen and the 1.55 action
        // cannot drift apart.
        Spotify155.connect(spDc, spKey)
    }

    override fun disconnectSpotify() = Spotify155.disconnect()

    override fun refreshSpotify() = Spotify155.refresh()

    override fun importSpotify(selected: List<String>) = Spotify155.import(selected)
}

/**
 * The 1.55 account page, rendered with the same [YouTubeAccountCard] the 1.56
 * Accounts page uses. Every button is the holder's own callable, so signing in,
 * out, switching and refreshing are the production operations, not a copy.
 */
@Composable
fun Legacy155AccountPage(
    viewModel: PlayerViewModel,
    actions: Legacy155Actions,
    modifier: Modifier = Modifier,
) {
    val state by viewModel.youtubeAccountStateHolder.state.collectAsState()
    Legacy155AccountPage(state = state, actions = actions, modifier = modifier)
}

/** The same card with an explicit state, so every control can be exercised in tests. */
@Composable
fun Legacy155AccountPage(
    state: com.music.vivi.desktop.presentation.viewmodel.YouTubeAccountUiState,
    actions: Legacy155Actions,
    modifier: Modifier = Modifier,
) {
    Column(modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        YouTubeAccountCard(
            state = state,
            onFindBrowsers = actions::detectProfiles,
            onUseProfile = actions::useProfile,
            onDisconnect = actions::signOutYouTube,
            onWebSignIn = actions::signInYouTube,
            onSwitchAccount = actions::switchAccount,
            onRemoveAccount = actions::removeAccount,
            onRefreshSession = actions::refreshYouTube,
        )
    }
}

/** Backup and restore, both through [FullBackup] and the same file pickers. */
@Composable
fun Legacy155BackupPage(
    viewModel: PlayerViewModel,
    actions: Legacy155Actions,
    modifier: Modifier = Modifier,
) {
    val settings by viewModel.userSettings.collectAsState()
    val dateFormat = remember { SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault()) }
    val newest = remember(settings.lastAutoBackupAt) {
        File(DesktopPathProvider.syncStateFile.absoluteFile.parentFile, "backups")
            .listFiles()
            ?.filter { it.isFile && it.name.endsWith(".${FullBackup.EXTENSION}") }
            ?.maxByOrNull { it.lastModified() }
    }
    Column(modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        SettingsSubsection(title = "FULL DEVICE BACKUP") {
            Surface(shape = MaterialTheme.shapes.large, color = MaterialTheme.colorScheme.surfaceContainer, modifier = Modifier.fillMaxWidth()) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("Library and sync data (.${FullBackup.EXTENSION})", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                    Text(
                        "One file with playlists, favorites, history, lyrics and the sync identity. Account credentials are never included.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        Button(onClick = actions::exportFullBackup, modifier = Modifier.testTag("legacy-backup-export")) {
                            Icon(painterResource(Res.drawable.ic_outlined_file_download), null, Modifier.size(18.dp))
                            Spacer(Modifier.size(8.dp))
                            Text("Export .${FullBackup.EXTENSION}")
                        }
                        TextButton(onClick = actions::restoreFullBackup, modifier = Modifier.testTag("legacy-backup-restore")) {
                            Icon(painterResource(Res.drawable.ic_outlined_file_upload), null, Modifier.size(18.dp))
                            Spacer(Modifier.size(8.dp))
                            Text("Restore")
                        }
                    }
                    Text(
                        if (settings.lastAutoBackupAt > 0L) {
                            "Last automatic backup: ${dateFormat.format(Date(settings.lastAutoBackupAt))}"
                        } else {
                            "No automatic backup has been written yet."
                        },
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Text(
                        newest?.let { "Newest backup: ${it.name}" } ?: "No backup file in the backups folder.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
        SettingsSubsection(title = "AUTOMATIC BACKUP") {
            Surface(shape = MaterialTheme.shapes.large, color = MaterialTheme.colorScheme.surfaceContainer, modifier = Modifier.fillMaxWidth()) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f)) {
                            Text("Weekly backup", style = MaterialTheme.typography.titleMedium)
                            Text("A full backup is written while the app is open and a week has passed.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        Switch(checked = settings.autoBackupEnabled, onCheckedChange = viewModel.themeStateHolder::setAutoBackupEnabled, modifier = Modifier.testTag("legacy-auto-backup"))
                    }
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f)) {
                            Text("Before updating", style = MaterialTheme.typography.bodyLarge)
                            Text("Keep a restore point before a new version is downloaded.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        Switch(checked = settings.backupBeforeUpdate, onCheckedChange = viewModel.themeStateHolder::setBackupBeforeUpdate, modifier = Modifier.testTag("legacy-backup-before-update"))
                    }
                }
            }
        }
    }
}

/** The three caches 1.55 cleared, each through the implementation that owns it. */
@Composable
fun Legacy155StoragePage(
    viewModel: PlayerViewModel,
    actions: Legacy155Actions,
    modifier: Modifier = Modifier,
) {
    val enabled by viewModel.audioCacheEnabled.collectAsState()
    val limit by viewModel.audioCacheLimitMb.collectAsState()
    val used by viewModel.audioCacheSizeBytes.collectAsState()
    val songs by viewModel.songs.collectAsState()
    val albums by viewModel.albums.collectAsState()
    val playlists by viewModel.playlists.collectAsState()
    val folders by viewModel.musicDirectories.collectAsState()
    var draft by remember(limit) { mutableStateOf(limit.toFloat()) }
    Column(modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        SettingsSubsection(title = "CACHES") {
            Surface(shape = MaterialTheme.shapes.large, color = MaterialTheme.colorScheme.surfaceContainer, modifier = Modifier.fillMaxWidth()) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f)) {
                            Text("Audio cache", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                            Text("Keep played catalogue tracks on disk.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        Switch(checked = enabled, onCheckedChange = viewModel::setAudioCacheEnabled, modifier = Modifier.testTag("legacy-audio-cache-enabled"))
                    }
                    Text("$used bytes used of $limit MB", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Slider(
                        value = draft,
                        onValueChange = { draft = it },
                        onValueChangeFinished = {
                            viewModel.setAudioCacheLimitMb(draft.roundToInt())
                            viewModel.refreshAudioCacheSize()
                        },
                        valueRange = 128f..10240f,
                        steps = 19,
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Button(onClick = { actions.clearAudioCache(); viewModel.refreshAudioCacheSize() }, modifier = Modifier.testTag("legacy-clear-audio")) { Text("Clear audio cache") }
                        TextButton(onClick = actions::clearLyricsCache, modifier = Modifier.testTag("legacy-clear-lyrics")) { Text("Clear lyrics cache") }
                        TextButton(onClick = actions::deleteInstallers, modifier = Modifier.testTag("legacy-delete-installers")) { Text("Delete installers") }
                    }
                }
            }
        }
        SettingsSubsection(title = "LIBRARY") {
            Surface(shape = MaterialTheme.shapes.large, color = MaterialTheme.colorScheme.surfaceContainer, modifier = Modifier.fillMaxWidth()) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text("${songs.size} songs · ${albums.size} albums · ${playlists.size} playlists", style = MaterialTheme.typography.bodyLarge)
                    Text("${folders.size} music folder(s)", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }
    }
}

/** The real listening summary, not fixed counters. */
@Composable
fun Legacy155WrappedPage(
    viewModel: PlayerViewModel,
    modifier: Modifier = Modifier,
) {
    val summary by viewModel.statsStateHolder.currentSummary.collectAsState()
    val scope = rememberCoroutineScope()
    LaunchedEffect(viewModel) { viewModel.statsStateHolder.refreshStats() }
    Column(modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            StatsTimeRange.entries.forEach { candidate ->
                TextButton(
                    onClick = { viewModel.statsStateHolder.selectRange(candidate) },
                    modifier = Modifier.testTag("legacy-wrapped-range-${candidate.name}"),
                ) {
                    Text(candidate.name.lowercase().replaceFirstChar { it.uppercase() })
                }
            }
        }
        StatsOverviewCard(
            summary = summary,
            onClick = { scope.launch { viewModel.statsStateHolder.refreshStats() } },
            modifier = Modifier.testTag("legacy-wrapped-card"),
        )
        if (summary == null) {
            Text("No listening activity in this range yet.", color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

/** Spotify connect and import, calling the desktop Spotify manager. */
@Composable
fun Legacy155SpotifyPage(
    actions: Legacy155Actions,
    modifier: Modifier = Modifier,
) {
    val state by Spotify155.state.collectAsState()
    Legacy155SpotifyPage(state = state, actions = actions, modifier = modifier)
}

/** The same Spotify controls with an explicit state, so each one can be tested. */
@Composable
fun Legacy155SpotifyPage(
    state: Spotify155.State,
    actions: Legacy155Actions,
    modifier: Modifier = Modifier,
) {
    var spDc by remember { mutableStateOf("") }
    var spKey by remember { mutableStateOf("") }
    var selected by remember { mutableStateOf(emptySet<String>()) }
    Column(modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        SettingsSubsection(title = "SPOTIFY ACCOUNT") {
            if (!state.connected) {
                OutlinedTextField(spDc, { spDc = it }, label = { Text("sp_dc") }, singleLine = true, modifier = Modifier.fillMaxWidth().testTag("legacy-spotify-spdc"))
                OutlinedTextField(spKey, { spKey = it }, label = { Text("sp_key") }, singleLine = true, modifier = Modifier.fillMaxWidth().testTag("legacy-spotify-spkey"))
                Button(
                    onClick = { actions.connectSpotify(spDc, spKey) },
                    enabled = spDc.isNotBlank(),
                    modifier = Modifier.testTag("legacy-spotify-connect"),
                ) { Text("Connect Spotify") }
            } else {
                Text("Connected as ${state.accountName}", style = MaterialTheme.typography.titleMedium)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    TextButton(onClick = actions::refreshSpotify) { Text("Refresh") }
                    TextButton(onClick = { actions.disconnectSpotify(); selected = emptySet() }) { Text("Disconnect") }
                    TextButton(onClick = { selected = (state.playlists.map { it.id } + Spotify155.LIKED_SOURCE).toSet() }) { Text("Select all") }
                    Button(onClick = { actions.importSpotify(selected.toList()) }, modifier = Modifier.testTag("legacy-spotify-import")) { Text("Import") }
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Checkbox(
                        checked = Spotify155.LIKED_SOURCE in selected,
                        onCheckedChange = { checked ->
                            selected = if (checked) selected + Spotify155.LIKED_SOURCE else selected - Spotify155.LIKED_SOURCE
                        },
                    )
                    Text("Liked Songs (${state.likedSongsCount})", style = MaterialTheme.typography.bodyLarge)
                }
                state.playlists.forEach { playlist ->
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Checkbox(
                            checked = playlist.id in selected,
                            onCheckedChange = { checked ->
                                selected = if (checked) selected + playlist.id else selected - playlist.id
                            },
                        )
                        Text(playlist.name, style = MaterialTheme.typography.bodyMedium)
                    }
                }
                if (state.loading) Text("Working...", style = MaterialTheme.typography.bodySmall)
                state.error?.let { Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall) }
                if (state.imported > 0) Text("${state.imported} tracks imported", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.primary)
            }
        }
    }
}
