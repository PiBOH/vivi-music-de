package com.music.vivi.desktop

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.io.File

/**
 * Single source of truth for the update download state, shared by the update
 * notification banner and the Settings → Updates screen. Both surfaces read and
 * write the same state, so downloading/opening an installer from one is
 * immediately reflected in the other (and vice versa).
 */
object UpdateState {
    /**
     * App-level scope for the update download. The screen that starts it uses
     * `rememberCoroutineScope`, which is cancelled the moment the user leaves
     * the screen — so the transfer was cut in half exactly as #82 describes
     * (leave the menu mid-download, come back, and it claims to be finished).
     * Owned here it survives leaving the screen, and both surfaces keep showing
     * its progress because this object is the shared state.
     */
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    private var downloadJob: Job? = null

    /** Starts [download] on [scope], at most once; a re-entrant call joins it. */
    fun startDownload(asset: UpdateAsset): Job {
        downloadJob?.takeIf { it.isActive }?.let { return it }
        val job = scope.launch { download(asset) }
        downloadJob = job
        return job
    }
    private val _progress = MutableStateFlow<DownloadProgress?>(null)
    val progress: StateFlow<DownloadProgress?> = _progress.asStateFlow()

    private val _downloadedFile = MutableStateFlow<File?>(null)
    val downloadedFile: StateFlow<File?> = _downloadedFile.asStateFlow()

    private val _installerCount = MutableStateFlow(countInstallers())
    val installerCount: StateFlow<Int> = _installerCount.asStateFlow()

    /**
     * How many installers count: only the ones for the version that is already
     * installed and running (see [UpdateDownloader.installedInstallers]). The
     * installer for an update the user has downloaded but not run yet is not
     * one of them: nothing has been installed, so nothing is counted. Once that
     * installer is run, the app starts up as the new version and the file
     * counts from then on.
     */
    private fun countInstallers(): Int =
        UpdateDownloader.installedInstallers(AppInfo.FULL_VERSION).size

    /**
     * Re-derives the on-disk installer for the currently available version (or
     * clears it) and refreshes the installer count. Call whenever the update
     * status changes so a stale installer for an older version is never offered.
     */
    fun syncWithStatus(status: UpdateStatus?) {
        val asset = (status as? UpdateStatus.Available)?.asset
        // The asset's size is what makes this answer trustworthy: an interrupted
        // download never reaches the final name anyway, and a file that is the
        // right name but the wrong length is refused here as well (#82).
        _downloadedFile.value = asset?.let {
            UpdateDownloader.downloadedInstaller(it.fileName, it.sizeBytes)
        }
        _installerCount.value = countInstallers()
    }

    /** Downloads [asset], reporting progress through [progress]; null on error. */
    suspend fun download(asset: UpdateAsset): File? {
        _progress.value = DownloadProgress(0, asset.sizeBytes, 0)
        return try {
            val file = UpdateDownloader.download(
                asset.downloadUrl,
                asset.fileName,
                asset.sizeBytes,
            ) { p -> _progress.value = p }
            _downloadedFile.value = file
            // Deliberately NOT counted yet: this is the installer for an update
            // that has been downloaded, not installed. `syncWithStatus` and the
            // next launch recompute the count from what is on disk.
            _installerCount.value = countInstallers()
            AppLog.log(
                "cache",
                "update installer ready: ${file.name} (${file.length()} bytes)",
            )
            file
        } catch (e: Exception) {
            // The reason belongs in the exported log: a truncated transfer is
            // reported here instead of surfacing later as a broken installer.
            AppLog.log("cache", "update download failed: ${e.message}")
            null
        } finally {
            _progress.value = null
        }
    }

    /**
     * Deletes the counted installers (the ones of the installed version). The
     * installer of a pending update is left alone: it is what the user is about
     * to run, and deleting it would break the update button that sits right
     * next to this one.
     */
    fun deleteAllInstallers() {
        UpdateDownloader.deleteFiles(UpdateDownloader.installedInstallers(AppInfo.FULL_VERSION))
        _downloadedFile.value = null
        _installerCount.value = countInstallers()
    }
}
