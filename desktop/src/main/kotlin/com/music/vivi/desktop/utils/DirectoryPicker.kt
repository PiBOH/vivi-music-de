package com.music.vivi.desktop.utils

import io.github.vinceglb.filekit.FileKit
import io.github.vinceglb.filekit.PlatformFile
import io.github.vinceglb.filekit.dialogs.FileKitDialogSettings
import io.github.vinceglb.filekit.dialogs.FileKitType
import io.github.vinceglb.filekit.dialogs.openDirectoryPicker
import io.github.vinceglb.filekit.dialogs.openFilePicker
import io.github.vinceglb.filekit.dialogs.openFileSaver
import java.io.File
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import com.music.vivi.desktop.data.backup.BackupManager
import com.music.vivi.desktop.data.backup.FullBackup

object DirectoryPicker {
    private val scope = CoroutineScope(Dispatchers.Main + SupervisorJob())

    fun chooseDirectory(
        initialDir: File? = null,
        title: String = "Select Music Directory",
        onSelected: (File) -> Unit
    ) {
        scope.launch {
            val directory = FileKit.openDirectoryPicker(
                directory = initialDir?.let { PlatformFile(it) },
                dialogSettings = FileKitDialogSettings(title = title)
            )
            if (directory != null) {
                onSelected(directory.file)
            }
        }
    }

    fun chooseLrcFile(
        title: String = "Select Lyric File (.lrc, .ttml)",
        onSelected: (File) -> Unit
    ) {
        scope.launch {
            val file = FileKit.openFilePicker(
                type = FileKitType.File(listOf("lrc", "ttml")),
                dialogSettings = FileKitDialogSettings(title = title)
            )
            if (file != null) {
                onSelected(file.file)
            }
        }
    }

    fun saveLrcFile(
        defaultFileName: String = "lyrics.lrc",
        initialDir: File? = null,
        title: String = "Save .lrc Lyric File",
        onSelected: (File) -> Unit
    ) {
        scope.launch {
            val baseName = if (defaultFileName.endsWith(".lrc", ignoreCase = true)) {
                defaultFileName.substringBeforeLast(".lrc")
            } else {
                defaultFileName
            }

            val file = FileKit.openFileSaver(
                suggestedName = baseName,
                defaultExtension = "lrc",
                directory = initialDir?.let { PlatformFile(it) },
                dialogSettings = FileKitDialogSettings(title = title)
            )
            if (file != null) {
                val finalFile = if (file.file.name.endsWith(".lrc", ignoreCase = true)) {
                    file.file
                } else {
                    File(file.file.parentFile, "${file.file.name}.lrc")
                }
                onSelected(finalFile)
            }
        }
    }

    /** The audio extensions the scanner recognizes, for relinking a missing song. */
    fun chooseAudioFile(
        title: String = "Select Audio File",
        onSelected: (File) -> Unit
    ) {
        scope.launch {
            val file = FileKit.openFilePicker(
                type = FileKitType.File(
                    listOf(
                        "mp3", "flac", "ogg", "opus", "m4a", "wav", "aac", "wma",
                        "alac", "aiff", "aif", "ape", "wv", "tta", "dsf", "dff"
                    )
                ),
                dialogSettings = FileKitDialogSettings(title = title)
            )
            if (file != null) {
                onSelected(file.file)
            }
        }
    }

    /** The portable JSON file the Accounts card uses to move playlists between accounts. */
    fun choosePlaylistPackFile(
        title: String = "Select an Axolotl Music account playlist file",
        onSelected: (File) -> Unit
    ) {
        scope.launch {
            FileKit.openFilePicker(
                type = FileKitType.File(listOf("json")),
                dialogSettings = FileKitDialogSettings(title = title)
            )?.let { onSelected(it.file) }
        }
    }

    fun savePlaylistPackFile(
        defaultFileName: String = "axolotl-music-account-playlists.json",
        initialDir: File? = null,
        title: String = "Export account playlists",
        onSelected: (File) -> Unit
    ) {
        scope.launch {
            val file = FileKit.openFileSaver(
                suggestedName = defaultFileName.removeSuffix(".json"),
                defaultExtension = "json",
                directory = initialDir?.let { PlatformFile(it) },
                dialogSettings = FileKitDialogSettings(title = title)
            ) ?: return@launch
            val finalFile = if (file.file.name.endsWith(".json", ignoreCase = true)) {
                file.file
            } else {
                File(file.file.parentFile, "${file.file.name}.json")
            }
            onSelected(finalFile)
        }
    }

    fun chooseImageFile(
        title: String = "Select Album Artwork",
        onSelected: (File) -> Unit
    ) {
        scope.launch {
            val file = FileKit.openFilePicker(
                type = FileKitType.Image,
                dialogSettings = FileKitDialogSettings(title = title)
            )
            if (file != null) {
                onSelected(file.file)
            }
        }
    }

    /** .vivide.sections and .pxpl stay readable so a section backup made before the rename imports. */
    fun chooseBackupFile(
        title: String = "Select a .${BackupManager.EXTENSION} backup file",
        onSelected: (File) -> Unit
    ) = pickBackupFile(title, listOf(BackupManager.EXTENSION, "vivide.sections", "pxpl"), onSelected)

    fun saveBackupFile(
        defaultFileName: String = "AxolotlMusic_Sections.${BackupManager.EXTENSION}",
        initialDir: File? = null,
        title: String = "Export Section Backup",
        onSelected: (File) -> Unit
    ) = saveBackupFileAs(
        defaultFileName = defaultFileName,
        extension = BackupManager.EXTENSION,
        legacySuffixes = listOf(".vivide.sections", ".pxpl"),
        initialDir = initialDir,
        title = title,
        onSelected = onSelected
    )

    /** A diagnostic package is a plain zip: no legacy suffix to strip. */
    fun saveDiagnosticsFile(
        defaultFileName: String = "AxolotlMusic_Diagnostics.zip",
        initialDir: File? = null,
        title: String = "Export diagnostic package",
        onSelected: (File) -> Unit
    ) = saveBackupFileAs(
        defaultFileName = defaultFileName,
        extension = "zip",
        legacySuffixes = emptyList(),
        initialDir = initialDir,
        title = title,
        onSelected = onSelected
    )

    /** The old .vivide.bkp and .vivide.backup names are still accepted; only the extension moved. */
    fun chooseVivideBackupFile(
        title: String = "Select a .${FullBackup.EXTENSION} file",
        onSelected: (File) -> Unit
    ) = pickBackupFile(title, listOf(FullBackup.EXTENSION, "vivide.bkp", "vivide.backup"), onSelected)

    fun saveVivideBackupFile(
        defaultFileName: String = "AxolotlMusic.${FullBackup.EXTENSION}",
        initialDir: File? = null,
        title: String = "Export full device backup",
        onSelected: (File) -> Unit
    ) = saveBackupFileAs(
        defaultFileName = defaultFileName,
        extension = FullBackup.EXTENSION,
        legacySuffixes = listOf(".vivide.bkp", ".vivide.backup"),
        initialDir = initialDir,
        title = title,
        onSelected = onSelected
    )

    private fun pickBackupFile(
        title: String,
        extensions: List<String>,
        onSelected: (File) -> Unit
    ) {
        scope.launch {
            FileKit.openFilePicker(
                type = FileKitType.File(extensions),
                dialogSettings = FileKitDialogSettings(title = title)
            )?.let { onSelected(it.file) }
        }
    }

    private fun saveBackupFileAs(
        defaultFileName: String,
        extension: String,
        legacySuffixes: List<String>,
        initialDir: File?,
        title: String,
        onSelected: (File) -> Unit
    ) {
        scope.launch {
            val targetSuffix = ".$extension"
            val file = FileKit.openFileSaver(
                suggestedName = defaultFileName.removeSuffix(targetSuffix),
                defaultExtension = extension,
                directory = initialDir?.let { PlatformFile(it) },
                dialogSettings = FileKitDialogSettings(title = title)
            ) ?: return@launch
            val name = file.file.name
            val finalFile = if (name.endsWith(targetSuffix, ignoreCase = true)) {
                file.file
            } else {
                val stem = legacySuffixes.fold(name) { acc, suffix -> acc.removeSuffix(suffix) }
                File(file.file.parentFile, "$stem$targetSuffix")
            }
            onSelected(finalFile)
        }
    }

    fun chooseM3uFile(
        title: String = "Select .m3u Playlist File",
        onSelected: (File) -> Unit
    ) {
        scope.launch {
            val file = FileKit.openFilePicker(
                type = FileKitType.File(listOf("m3u", "m3u8")),
                dialogSettings = FileKitDialogSettings(title = title)
            )
            if (file != null) {
                onSelected(file.file)
            }
        }
    }

    fun saveM3uFile(
        defaultFileName: String = "playlist.m3u",
        initialDir: File? = null,
        title: String = "Export M3U Playlist",
        onSelected: (File) -> Unit
    ) {
        scope.launch {
            val rawBaseName = defaultFileName.removeSuffix(".m3u").removeSuffix(".m3u8")
            val sanitizedBaseName = rawBaseName.replace(Regex("[\\\\/:*?\"<>|]"), "_").ifBlank { "playlist" }
            val file = FileKit.openFileSaver(
                suggestedName = sanitizedBaseName,
                defaultExtension = "m3u",
                directory = initialDir?.let { PlatformFile(it) },
                dialogSettings = FileKitDialogSettings(title = title)
            )
            if (file != null) {
                val finalFile = if (file.file.name.endsWith(".m3u", ignoreCase = true) || file.file.name.endsWith(".m3u8", ignoreCase = true)) {
                    file.file
                } else {
                    val parent = file.file.parentFile
                    if (parent != null) File(parent, "${file.file.name}.m3u") else File("${file.file.name}.m3u")
                }
                onSelected(finalFile)
            }
        }
    }
}

