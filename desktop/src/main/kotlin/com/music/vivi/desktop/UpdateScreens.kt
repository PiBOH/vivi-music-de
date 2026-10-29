package com.music.vivi.desktop

import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Article
import androidx.compose.material.icons.filled.CloudDownload
import androidx.compose.material.icons.filled.Commit
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.NewReleases
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material.icons.outlined.History
import androidx.compose.material.icons.outlined.NewReleases
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import java.awt.Desktop
import java.io.File
import kotlin.system.exitProcess
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@Composable
fun UpdateSection(
    language: String,
    status: UpdateStatus,
    includePreReleases: Boolean,
    updateIntervalHours: Int,
    updateSource: String,
    onIntervalChange: (Int) -> Unit,
    onTogglePreReleases: (Boolean) -> Unit,
    onUpdateSourceChange: (String) -> Unit,
    onCheckUpdates: () -> Unit,
    onOpenChangelog: () -> Unit,
    onOpenCommits: () -> Unit,
) {
    val scope = rememberCoroutineScope()
    // Shared download state (also used by the notification banner), so the two
    // surfaces stay in sync.
    val progress by UpdateState.progress.collectAsState()
    val downloadedFile by UpdateState.downloadedFile.collectAsState()
    val installerCount by UpdateState.installerCount.collectAsState()
    var openError by remember { mutableStateOf<String?>(null) }

    // The screen is the preview's four blocks: the title, what the app is and the
    // check itself, the interval it waits between checks, and the two
    // destinations the version history is read from. The update's own state
    // (download, progress, installer) stays under them, where it appears only
    // once there is something to report.
    // The title carries no top padding of its own: `SettingsSubScreen` already
    // puts the preview's 16 dp between the Back row and the screen's first block.
    Text(
        Localization.get(language, "updates"),
        style = MaterialTheme.typography.titleLarge,
    )
    Spacer(Modifier.height(16.dp))

    M3SettingsGroup(
        items = listOf(
            M3SettingsItem(
                icon = Icons.Filled.Refresh,
                title = { Text(Localization.get(language, "check_updates")) },
                // The preview puts the version itself under the row, with no label
                // in front of it: the row is about that one version, and
                // "Current version:" repeated it.
                description = { Text(AppInfo.FULL_VERSION) },
                // The preview's trailing slot is the action, not a symbol: a filled
                // button that names what pressing it does. It used to be a refresh
                // icon, which said the row was a switch for something.
                trailing = {
                    Tooltip(Localization.get(language, "check_now")) {
                        Button(
                            onClick = onCheckUpdates,
                            enabled = status != UpdateStatus.Checking,
                        ) {
                            Text(Localization.get(language, "check_now"))
                        }
                    }
                },
                onClick = onCheckUpdates,
            ),
            M3SettingsItem(
                icon = Icons.Filled.NewReleases,
                title = { Text(Localization.get(language, "include_prereleases")) },
                trailing = { Switch(checked = includePreReleases, onCheckedChange = onTogglePreReleases) },
                onClick = { onTogglePreReleases(!includePreReleases) },
            ),
        ),
    )

    Spacer(Modifier.height(16.dp))
    M3SettingsDropdownItem(
        icon = Icons.Filled.Schedule,
        title = Localization.get(language, "update_check_interval"),
        value = intervalLabel(language, updateIntervalHours),
        options = updateCheckIntervalOptions().map { hours -> hours.toString() to intervalLabel(language, hours) },
        onSelect = { picked -> picked.toIntOrNull()?.let(onIntervalChange) },
    )

    Spacer(Modifier.height(16.dp))
    M3SettingsDropdownItem(
        icon = Icons.Filled.CloudDownload,
        title = Localization.get(language, "update_source"),
        value = UpdateSource.repoFor(updateSource),
        options = listOf(UpdateSource.FORK, UpdateSource.ORIGINAL).map { source -> source to UpdateSource.repoFor(source) },
        onSelect = onUpdateSourceChange,
    )

    Spacer(Modifier.height(16.dp))
    M3SettingsGroup(
        items = listOf(
            M3SettingsItem(
                icon = Icons.Filled.Article,
                title = { Text(Localization.get(language, "changelog")) },
                trailing = { SettingsChevron() },
                onClick = onOpenChangelog,
            ),
            M3SettingsItem(
                // The preview's `commit` glyph, where the DE reused the history
                // clock: a commit is a point in the log, not a clock face, and the
                // clock already means the listening history elsewhere.
                icon = Icons.Filled.Commit,
                title = { Text(Localization.get(language, "commits")) },
                trailing = { SettingsChevron() },
                onClick = onOpenCommits,
            ),
        ),
    )

    Spacer(Modifier.height(16.dp))

    when (status) {
        is UpdateStatus.Checking -> Text(Localization.get(language, "checking"), modifier = Modifier.padding(top = 8.dp))
        is UpdateStatus.UpToDate -> Text(Localization.get(language, "up_to_date"), modifier = Modifier.padding(top = 8.dp))
        is UpdateStatus.Available -> {
            Text("${Localization.get(language, "update_available")}: ${status.version}", modifier = Modifier.padding(top = 8.dp))
            val asset = status.asset
            when {
                asset == null -> {
                    Text(
                        Localization.get(language, "no_installer"),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(top = 8.dp),
                    )
                    Button(
                        onClick = {
                            openError = null
                            if (!openUrl(status.url)) openError = Localization.get(language, "open_failed")
                        },
                        modifier = Modifier.padding(top = 4.dp),
                    ) {
                        Text(Localization.get(language, "open_release_page"))
                    }
                }
                downloadedFile != null -> {
                    Text(
                        "${Localization.get(language, "downloaded")}: ${downloadedFile!!.name}",
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.padding(top = 4.dp),
                    )
                    Button(
                        onClick = {
                            openError = null
                            scope.launch {
                                if (!prepareAndOpenInstaller(downloadedFile!!)) {
                                    openError = Localization.get(language, "open_failed")
                                }
                            }
                        },
                        modifier = Modifier.padding(top = 4.dp),
                    ) {
                        Text(Localization.get(language, "open_installer"))
                    }
                }
                progress != null -> {
                    val p = progress!!
                    Text(
                        "${Localization.get(language, "downloading")}: ${p.percent}% · ${formatBytes(p.downloadedBytes)} / ${formatBytes(p.totalBytes)} · ${formatSpeed(p.speedBytesPerSecond)}",
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.padding(top = 8.dp),
                    )
                    LinearProgressIndicator(
                        progress = { p.percent / 100f },
                        modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                    )
                }
                else -> Button(
                    onClick = {
                        // Owned by UpdateState, not by this screen: leaving the
                        // screen no longer cancels the download (#82).
                        UpdateState.startDownload(asset)
                    },
                    modifier = Modifier.padding(top = 4.dp),
                ) {
                    Text("${Localization.get(language, "download")} (${formatBytes(asset.sizeBytes)})")
                }
            }
        }
        is UpdateStatus.Failed -> SelectionContainer {
            Text(
                "${Localization.get(language, "update_failed")}: ${status.message}",
                color = MaterialTheme.colorScheme.error,
                modifier = Modifier.padding(top = 8.dp),
            )
        }
        is UpdateStatus.Idle -> Unit
    }

    // The available release's own notes, rendered as Markdown (the same renderer
    // the About → Changelog screen uses), so the update can be judged before it
    // is downloaded. The section below the update controls is the natural place
    // for it.
    (status as? UpdateStatus.Available)?.body?.takeIf { it.isNotBlank() }?.let { body ->
        Text(
            Localization.get(language, "latest_release"),
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.padding(top = 16.dp),
        )
        MarkdownView(
            markdown = body,
            modifier = Modifier.padding(top = 4.dp),
            onOpenUrl = { openUrl(it) },
        )
    }

    openError?.let {
        SelectionContainer { Text(it, color = MaterialTheme.colorScheme.error, modifier = Modifier.padding(top = 8.dp)) }
    }

    // Downloaded installer management, drawn only while an installer is on disk:
    // it is the state of a download, not a choice, and a page that ends on
    // "Downloaded installers: 0" says nothing the preview's page says.
    if (installerCount > 0) {
        Text(
            "${Localization.get(language, "installers_downloaded")}: $installerCount",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(top = 16.dp),
        )
        Button(
            onClick = { UpdateState.deleteAllInstallers() },
            modifier = Modifier.padding(top = 4.dp),
        ) {
            Text(Localization.get(language, "delete_installers"))
        }
    }
}

/** Available update-check intervals, in hours (0 = manual only). */
private fun updateCheckIntervalOptions(): List<Int> = listOf(0, 6, 12, 24, 72, 168)

/** Localized label for an update-check interval. */
private fun intervalLabel(language: String, hours: Int): String = when (hours) {
    0 -> Localization.get(language, "interval_manual")
    6 -> Localization.get(language, "interval_6h")
    12 -> Localization.get(language, "interval_12h")
    24 -> Localization.get(language, "interval_24h")
    72 -> Localization.get(language, "interval_3d")
    168 -> Localization.get(language, "interval_7d")
    else -> "$hours h"
}

internal fun openFile(file: File): Boolean {
    if (!file.exists()) return false
    if (Desktop.isDesktopSupported() && Desktop.getDesktop().isSupported(Desktop.Action.OPEN)) {
        if (runCatching { Desktop.getDesktop().open(file) }.isSuccess) return true
    }
    return runCatching {
        val path = file.absolutePath
        val cmd = when (Platform.os) {
            DesktopOs.WINDOWS -> listOf("cmd", "/c", "start", "", path)
            DesktopOs.MACOS -> listOf("open", path)
            DesktopOs.LINUX -> listOf("xdg-open", path)
        }
        ProcessBuilder(cmd).start()
        true
    }.getOrDefault(false)
}


/**
 * Runs the optional "backup before update" (if enabled) and then opens the
 * installer, exiting the app on success so the installer can replace the
 * running files. Returns false when the installer could not be opened. Shared
 * by the update notification and the Updates screen so both behave the same.
 */
internal suspend fun prepareAndOpenInstaller(file: File): Boolean {
    val s = DesktopSettings.load()
    if (s.autoBackupEnabled && s.autoBackupBeforeUpdate) {
        withContext(Dispatchers.IO) { BackupManager.autoBackup("before_update") }
    }
    val ok = openFile(file)
    if (ok) exitProcess(0)
    return ok
}
