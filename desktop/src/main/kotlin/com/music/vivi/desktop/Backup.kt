package com.music.vivi.desktop

import com.music.vivi.sync.SyncedPlaylist
import kotlinx.serialization.builtins.ListSerializer
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream
import java.util.zip.ZipOutputStream

/**
 * Comprehensive backup/restore for the desktop edition.
 *
 * A backup is a ZIP archive with two entries:
 *   - `settings.json`  -> the full [DesktopSyncState] (settings, library,
 *     account/login, dev options, update + notification prefs, …).
 *   - `playlists.json` -> the local playlists ([SyncedPlaylist] list).
 *
 * This mirrors the mobile app's approach (which zips its datastore + Room DB).
 * Backups use the `.vivide.backup` extension. Old single-JSON backups produced
 * by the previous `exportSettings` (`.backup`) are still recognized on import.
 *
 * Import is deliberately forgiving about the *container* and strict about the
 * *content*: the entries are matched by their base name (so an archive that was
 * unpacked and zipped again under a folder still restores), a byte-order mark is
 * stripped (an editor-written `settings.json` has one, and a decoder refuses the
 * whole file because of it) and an entry under an unexpected name still counts if
 * it decodes as the settings object. What it will not do is call a *half* restore
 * a success: if the settings entry is present but unreadable, that is a failure
 * with a reason, not a green light on a playlists-only import.
 *
 * Every failure carries the sentence the UI shows and the detail the log keeps —
 * the report that led here was one user sentence ("I tried 3 times to restore it
 * from my backup file but DE is not accepting it and saying failed to restore")
 * against a bare `false` that could equally have meant "no file chosen".
 */
object BackupManager {
    private const val SETTINGS_ENTRY = "settings.json"
    private const val PLAYLISTS_ENTRY = "playlists.json"

    /** What the phone's backup looks like: a different format this app cannot read. */
    private val MOBILE_ENTRY_SUFFIXES = listOf(".preferences_pb", ".db", ".sqlite", ".sqlite3")

    private val json = sharedJsonPretty

    /**
     * The variant the restore *reads* with. It is more forgiving than the file
     * stores' own JSON in one specific way that matters here: a value this build
     * does not know — an enum entry added by a newer version, which is exactly
     * what a backup made before a downgrade contains — falls back to the
     * property's default instead of failing the whole document. Losing one
     * option to a default is a restore; losing the file is not.
     */
    private val restoreJson = kotlinx.serialization.json.Json {
        ignoreUnknownKeys = true
        coerceInputValues = true
        encodeDefaults = true
        prettyPrint = true
    }

    /**
     * What a restore did. [messageKey] is a localization key the screen shows,
     * [detail] is the sentence `AppLog` keeps for whoever has to diagnose it.
     */
    data class RestoreResult(val ok: Boolean, val messageKey: String, val detail: String)

    /** Thrown inside [import], carrying the key + detail the caller reports. */
    private class RestoreFailure(val messageKey: String, override val message: String) :
        IllegalStateException(message)

    /** Auto backups live under `~/.vivimusic/backups/`. */
    private val autoDir: File = File(System.getProperty("user.home"), ".vivimusic/backups").apply { mkdirs() }

    private val timestamp: String
        get() = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"))

    /** Default filename for a manual backup, including date + timestamp. */
    fun defaultBackupFileName(): String = "vivimusic-de_$timestamp.vivide.backup"

    // ------------------------------------------------------------------
    // Manual export / import
    // ------------------------------------------------------------------

    /** Writes a full backup (settings + playlists) to [file]. */
    fun export(file: File): Boolean = runCatching {
        ZipOutputStream(FileOutputStream(file).buffered()).use { zip ->
            zip.putNextEntry(ZipEntry(SETTINGS_ENTRY))
            zip.write(json.encodeToString(DesktopSyncState.serializer(), DesktopSettings.load()).toByteArray(Charsets.UTF_8))
            zip.closeEntry()

            zip.putNextEntry(ZipEntry(PLAYLISTS_ENTRY))
            val playlists = json.encodeToString(ListSerializer(SyncedPlaylist.serializer()), PlaylistStore.toSynced())
            zip.write(playlists.toByteArray(Charsets.UTF_8))
            zip.closeEntry()
        }
        true
    }.getOrDefault(false)

    /**
     * Restores a backup from [file]. Accepts the new ZIP format or a legacy
     * bare-JSON settings file. The current device id and first-launch date are
     * preserved, and any stale pairing is dropped.
     */
    fun import(file: File): RestoreResult {
        if (!file.isFile) {
            return RestoreResult(false, "restore_failed", "no such file: ${file.path}")
        }
        val archive = runCatching { isZip(file) }.getOrElse {
            return RestoreResult(false, "restore_failed", "${file.name}: cannot be read (${it.message})")
        }
        return runCatching {
            if (archive) importZip(file) else importLegacy(file)
            AppLog.log("backup", "restore: ${file.name} applied")
            RestoreResult(true, "restore_success_title", "restored ${file.name}")
        }.getOrElse { e ->
            val key = (e as? RestoreFailure)?.messageKey ?: "restore_failed"
            val detail = "${file.name}: ${e.message}"
            AppLog.log("backup", "restore FAILED: $detail")
            RestoreResult(false, key, detail)
        }
    }

    private fun isZip(file: File): Boolean = runCatching {
        FileInputStream(file).use { input ->
            val header = ByteArray(2)
            input.read(header) == 2 && header[0] == 'P'.code.toByte() && header[1] == 'K'.code.toByte()
        }
    }.getOrDefault(false)

    private fun importZip(file: File) {
        // The entries are read into memory first (a settings entry is ~90 KB, the
        // playlists ~200 KB): the decode attempts below are independent of the
        // stream, and an entry is retried under its own name even when the
        // archive names it differently.
        val entries = mutableListOf<Pair<String, ByteArray>>()
        ZipInputStream(FileInputStream(file).buffered()).use { zip ->
            var entry = zip.nextEntry
            while (entry != null) {
                entries += entry.name to zip.readBytes()
                entry = zip.nextEntry
            }
        }
        if (entries.isEmpty()) {
            throw RestoreFailure("restore_failed_corrupt", "the archive has no entries")
        }

        fun baseName(name: String) = name.substringAfterLast('/').substringAfterLast('\\').lowercase()

        var settingsText: String? = null
        var settingsFailure: String? = null
        var playlistsText: String? = null
        entries.firstOrNull { baseName(it.first) == SETTINGS_ENTRY }?.let { (name, bytes) ->
            // Each entry is decoded on its own: an unreadable *playlists* file
            // must not cost the user their settings.
            settingsText = decodeText(bytes)
            if (settingsText == null) settingsFailure = "$name is not valid UTF-8"
        }
        entries.firstOrNull { baseName(it.first) == PLAYLISTS_ENTRY }?.let { (_, bytes) ->
            playlistsText = decodeText(bytes)
        }

        // The name is a convention, not a guarantee: an archive repacked by
        // another tool may hold the settings under any .json name.
        if (settingsText == null) {
            entries.firstOrNull { baseName(it.first).endsWith(".json") && baseName(it.first) != PLAYLISTS_ENTRY }
                ?.let { (_, bytes) -> settingsText = decodeText(bytes) }
        }

        val importedSettings = settingsText?.let { text ->
            runCatching { restoreJson.decodeFromString(DesktopSyncState.serializer(), text) }
                .onFailure { settingsFailure = "settings entry unreadable: ${it.message}" }
                .getOrNull()
        }
        if (importedSettings == null) {
            val names = entries.joinToString(", ") { it.first }
            val phone = entries.any { e -> MOBILE_ENTRY_SUFFIXES.any { s -> baseName(e.first).endsWith(s) } }
            val reason = when {
                phone ->
                    "this is the Android app's backup (${entries.first().first}), which this edition cannot read"
                settingsFailure != null -> settingsFailure!!
                else -> "no settings entry found; the archive holds: $names"
            }
            throw RestoreFailure("restore_failed_corrupt", reason)
        }

        val importedPlaylists = playlistsText?.let { text ->
            runCatching { restoreJson.decodeFromString(ListSerializer(SyncedPlaylist.serializer()), text) }
                .onFailure { AppLog.log("backup", "restore: playlists entry unreadable (${it.message})") }
                .getOrNull()
        }

        applyImportedSettings(importedSettings)

        // Playlists come from their own entry; when a backup predates that
        // entry (or it was unreadable) fall back to the copy embedded in the
        // settings' library snapshot, so a restore never silently leaves the
        // playlists behind.
        val playlists = importedPlaylists ?: importedSettings.library?.playlists
        if (playlists != null) {
            PlaylistStore.replaceAll(playlists)
            AppLog.log("backup", "restore: ${playlists.size} playlist(s) restored")
        } else {
            AppLog.log("backup", "restore: no playlists found in this backup")
        }
    }

    private fun importLegacy(file: File) {
        val text = decodeText(file.readBytes())
            ?: throw RestoreFailure("restore_failed_corrupt", "the file is not valid UTF-8")
        val imported = runCatching { restoreJson.decodeFromString(DesktopSyncState.serializer(), text) }
            .getOrElse {
                throw RestoreFailure(
                    "restore_failed_corrupt",
                    "not a settings backup: ${it.message}",
                )
            }
        applyImportedSettings(imported)
    }

    /**
     * Decodes bytes as UTF-8 text, dropping the byte-order mark first. A file
     * saved by an editor (or re-encoded by a tool) starts with one, and every
     * decoder then refuses the whole document — the restore said "failed" for a
     * file whose JSON was in fact perfect.
     */
    private fun decodeText(bytes: ByteArray): String? {
        val text = runCatching { bytes.decodeToString() }.getOrNull() ?: return null
        return text.removePrefix("\uFEFF")
    }

    /** Preserves this machine's identity + first-launch date, drops pairing. */
    private fun applyImportedSettings(imported: DesktopSyncState) {
        DesktopSettings.update { current ->
            imported.copy(
                deviceId = current.deviceId,
                firstLaunchDate = current.firstLaunchDate,
                pairId = "",
            )
        }
    }

    // ------------------------------------------------------------------
    // Automatic backups
    // ------------------------------------------------------------------

    /** Runs an automatic backup of [type] (`weekly` / `before_update` / …). */
    fun autoBackup(type: String): File? = runCatching {
        val file = File(autoDir, "auto_backup_${type}_$timestamp.vivide.backup")
        if (export(file)) {
            cleanup(type)
            file
        } else {
            null
        }
    }.getOrNull()

    /** Lists automatic backups, newest first. */
    fun listAuto(): List<File> = autoDir.listFiles { f ->
        f.isFile && f.name.startsWith("auto_backup_") && f.name.endsWith(".vivide.backup")
    }?.sortedByDescending { it.lastModified() } ?: emptyList()

    fun deleteAuto(file: File): Boolean = runCatching { file.delete() }.getOrDefault(false)

    /**
     * Runs the scheduled automatic backup if the weekly toggle is on and the
     * most recent weekly backup is older than 7 days. Called at startup and on
     * a periodic tick so backups happen even without a reboot.
     */
    fun maybeRunScheduled() {
        val s = DesktopSettings.load()
        if (!s.autoBackupEnabled || !s.autoBackupWeekly) return
        val last = listAuto()
            .filter { it.name.contains("_weekly_") }
            .maxOfOrNull { it.lastModified() }
        val now = System.currentTimeMillis()
        if (last == null || now - last >= 7L * 24 * 3600 * 1000) {
            autoBackup("weekly")
        }
    }

    /** Keeps the 5 newest backups of [type]; older ones are deleted. */
    private fun cleanup(type: String) {
        val backups = listAuto().filter { it.name.startsWith("auto_backup_${type}_") }
        if (backups.size > 5) {
            backups.drop(5).forEach { runCatching { it.delete() } }
        }
    }
}
