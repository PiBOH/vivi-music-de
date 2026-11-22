package com.music.vivi.desktop.data.backup

import com.music.vivi.desktop.domain.repository.SettingsRepository
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * Writes a full `.vivide.lbkp` without being asked.
 *
 * The rule is one week between automatic copies, checked at startup and every
 * six hours while the window stays open. A copy is also written before an
 * update is downloaded when the user keeps that option on. Old automatic copies
 * are pruned to five; manual exports are never touched.
 */
class AutoBackupScheduler(
    private val storeFile: File,
    private val backupDirectory: File,
    private val settingsRepository: SettingsRepository,
    private val coroutineScope: CoroutineScope
) {

    @Volatile
    private var started = false

    fun start() {
        if (started) return
        started = true
        coroutineScope.launch {
            runIfDue()
            while (isActive) {
                delay(CHECK_INTERVAL_MS)
                runIfDue()
            }
        }
    }

    suspend fun runIfDue(now: Long = System.currentTimeMillis()) {
        val settings = settingsRepository.getSettings()
        if (!settings.autoBackupEnabled) return
        if (now - settings.lastAutoBackupAt < WEEK_MS) return
        backupNow(now)
    }

    suspend fun backupNow(now: Long = System.currentTimeMillis()): Result<File> =
        withContext(Dispatchers.IO) {
            if (!storeFile.isFile) {
                return@withContext Result.failure(
                    IllegalStateException("There is no library at ${storeFile.absolutePath} yet.")
                )
            }
            backupDirectory.mkdirs()
            val stamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date(now))
            val target = File(backupDirectory, "auto_backup_$stamp.${FullBackup.EXTENSION}")
            FullBackup.export(target, storeFile)
                .map {
                    prune()
                    target
                }
                .onSuccess {
                    settingsRepository.setLastAutoBackupAt(now)
                }
        }

    /** Keeps the five newest automatic copies; a manual backup is not in this set. */
    private fun prune(keep: Int = 5) {
        backupDirectory
            .listFiles { candidate ->
                candidate.isFile &&
                    candidate.name.startsWith("auto_backup_") &&
                    candidate.name.endsWith(".${FullBackup.EXTENSION}")
            }
            ?.sortedByDescending { it.name }
            ?.drop(keep)
            ?.forEach { runCatching { it.delete() } }
    }

    companion object {
        private const val WEEK_MS = 7L * 24L * 60L * 60L * 1000L
        private const val CHECK_INTERVAL_MS = 6L * 60L * 60L * 1000L
    }
}
