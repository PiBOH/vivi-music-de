package com.music.vivi.desktop.presentation.viewmodel

import java.awt.Desktop
import java.io.File
import java.util.concurrent.atomic.AtomicBoolean

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

import com.music.vivi.desktop.BuildConfig
import com.music.vivi.desktop.data.backup.AutoBackupScheduler
import com.music.vivi.desktop.data.network.github.GitHubReleaseClient
import com.music.vivi.desktop.data.network.github.ReleaseChecksums
import com.music.vivi.desktop.data.network.github.UpdateAssets
import com.music.vivi.desktop.domain.model.AppReleaseInfo
import com.music.vivi.desktop.domain.model.SemanticVersion
import com.music.vivi.desktop.domain.model.UpdateCheckResult
import com.music.vivi.desktop.domain.model.UpdateDownloadState
import com.music.vivi.desktop.domain.repository.SettingsRepository
import com.music.vivi.desktop.utils.BrowserUtils
import com.music.vivi.desktop.utils.DesktopPathProvider

class UpdateStateHolder(
    private val releaseClient: GitHubReleaseClient,
    private val settingsRepository: SettingsRepository,
    private val coroutineScope: CoroutineScope,
    private val autoBackupScheduler: AutoBackupScheduler? = null
) {
    private val startupCheckTriggered = AtomicBoolean(false)

    private val _checkResult = MutableStateFlow<UpdateCheckResult>(UpdateCheckResult.Idle)
    val checkResult: StateFlow<UpdateCheckResult> = _checkResult.asStateFlow()

    private val _showUpdateDialog = MutableStateFlow(false)
    val showUpdateDialog: StateFlow<Boolean> = _showUpdateDialog.asStateFlow()

    private val _availableRelease = MutableStateFlow<AppReleaseInfo?>(null)
    val availableRelease: StateFlow<AppReleaseInfo?> = _availableRelease.asStateFlow()

    private val _downloadState = MutableStateFlow<UpdateDownloadState>(UpdateDownloadState.Idle)
    val downloadState: StateFlow<UpdateDownloadState> = _downloadState.asStateFlow()

    val checkUpdatesOnStartup: StateFlow<Boolean> = settingsRepository.settingsFlow
        .map { it.checkUpdatesOnStartup }
        .stateIn(coroutineScope, SharingStarted.Eagerly, true)

    fun runStartupCheckIfNeeded() {
        if (!startupCheckTriggered.compareAndSet(false, true)) {
            return
        }

        coroutineScope.launch {
            val settings = settingsRepository.getSettings()
            if (!settings.checkUpdatesOnStartup) {
                return@launch
            }

            performCheck(isManual = false)
        }
    }

    fun checkForUpdates() {
        coroutineScope.launch {
            performCheck(isManual = true)
        }
    }

    private suspend fun performCheck(isManual: Boolean) {
        if (isManual) {
            _checkResult.value = UpdateCheckResult.Checking
        }

        val release = releaseClient.fetchLatestRelease()
        val currentVersion = BuildConfig.APP_VERSION_NAME

        if (release == null) {
            if (isManual) {
                _checkResult.value = UpdateCheckResult.Error("Could not reach GitHub Releases.")
            }
            return
        }

        val isNewer = SemanticVersion.isNewer(
            remoteVersion = release.versionTag,
            currentVersion = currentVersion
        )

        if (isNewer) {
            _availableRelease.value = release
            _checkResult.value = UpdateCheckResult.UpdateAvailable(
                release = release,
                currentVersion = currentVersion
            )

            val settings = settingsRepository.getSettings()
            val alreadyDismissed = settings.lastDismissedUpdateVersion == release.versionTag

            if (isManual || !alreadyDismissed) {
                _showUpdateDialog.value = true
            }
        } else {
            _checkResult.value = UpdateCheckResult.UpToDate(currentVersion)
        }
    }

    fun dismissUpdateDialog(skipThisVersion: Boolean = false) {
        _showUpdateDialog.value = false
        if (skipThisVersion) {
            val release = _availableRelease.value ?: return
            coroutineScope.launch {
                settingsRepository.setLastDismissedUpdateVersion(release.versionTag)
            }
        }
    }

    fun openReleasePage(url: String) {
        BrowserUtils.openBrowser(url)
    }

    /**
     * Downloads this system's installer and refuses it when the published
     * SHA-256 does not match. A release without a digest is not downloaded at
     * all: an unverifiable setup is not an update path.
     */
    fun downloadUpdate() {
        if (_downloadState.value is UpdateDownloadState.Downloading) return
        val release = _availableRelease.value ?: return
        val asset = UpdateAssets.forCurrentPlatform(release)
        if (asset == null) {
            _downloadState.value = UpdateDownloadState.Failed(
                "The ${release.versionName} release has no installer for this system."
            )
            return
        }
        coroutineScope.launch {
            val expected = ReleaseChecksums.resolve(release, asset, releaseClient)?.sha256
            if (expected == null) {
                _downloadState.value = UpdateDownloadState.Failed(
                    "The release does not publish a SHA-256 for ${asset.name}, so the download was not started."
                )
                return@launch
            }
            _downloadState.value = UpdateDownloadState.Downloading(0L, asset.size)
            val settings = settingsRepository.getSettings()
            if (settings.backupBeforeUpdate) {
                autoBackupScheduler?.backupNow()
            }
            val target = File(DesktopPathProvider.cacheDir, "updates/${asset.name}")
            val outcome = releaseClient.download(asset, target) { received, total ->
                _downloadState.value = UpdateDownloadState.Downloading(received, total)
            }
            outcome.onSuccess { file ->
                val actual = runCatching { releaseClient.sha256(file) }.getOrNull()
                if (actual == null) {
                    file.delete()
                    _downloadState.value = UpdateDownloadState.Failed("The downloaded file could not be hashed.")
                } else if (!ReleaseChecksums.matches(expected, actual)) {
                    file.delete()
                    _downloadState.value = UpdateDownloadState.Failed(
                        "The downloaded file does not match the published SHA-256. It was deleted."
                    )
                } else {
                    _downloadState.value = UpdateDownloadState.Ready(file, actual)
                }
            }.onFailure { error ->
                _downloadState.value = UpdateDownloadState.Failed(error.message ?: "The download failed.")
            }
        }
    }

    /** Opens the verified installer with the system's default handler. */
    fun installUpdate() {
        val ready = _downloadState.value as? UpdateDownloadState.Ready ?: return
        runCatching {
            if (!Desktop.isDesktopSupported()) error("This system cannot open the installer.")
            Desktop.getDesktop().open(ready.file)
        }.onFailure { error ->
            _downloadState.value = UpdateDownloadState.Failed(error.message ?: "The installer could not be opened.")
        }
    }

    fun resetDownloadState() {
        _downloadState.value = UpdateDownloadState.Idle
    }

    /** Deletes the installers [downloadUpdate] kept under the cache directory. */
    fun deleteDownloadedInstallers() {
        runCatching {
            File(DesktopPathProvider.cacheDir, "updates").listFiles()?.forEach { file ->
                if (file.isFile) file.delete()
            }
        }
    }

    fun setCheckUpdatesOnStartup(enabled: Boolean) {
        coroutineScope.launch {
            settingsRepository.setCheckUpdatesOnStartup(enabled)
        }
    }
}
