package com.music.vivi.desktop

import java.io.File
import java.io.FileOutputStream
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

/**
 * End-to-end check of backup restore, run with
 *
 *     ./gradlew :desktop:backupRestoreCheck [--args="<file> <file> …"]
 *
 * The report that started this was one sentence — "I tried 3 times to restore it
 * from my backup file but DE is not accepting it and saying failed to restore" —
 * and the code answered it with a bare `false`: one boolean for "the file was
 * not chosen", "the archive had no readable entry", "the JSON did not decode"
 * and "writing the settings back failed". This check drives the real
 * [BackupManager.import] over the files a user actually has, and prints what each
 * one does, so a failure has a name instead of a sentence.
 *
 * It runs against a temporary `user.home`, so the machine's own settings and
 * playlists are never touched, and it checks the paths a real restore takes:
 *
 *  * a backup this build just wrote, restored into a changed app;
 *  * the legacy single-JSON file (and one with a byte-order mark, which the
 *    editor-written file has and `decodeFromString` refuses);
 *  * an archive whose entries sit under a folder (`vivimusic/settings.json`),
 *    which is what a re-zipped or tool-made archive looks like;
 *  * an archive with a broken settings entry but a readable playlists one, which
 *    must not be reported as a success;
 *  * the phone's own backup archive, which is a different format entirely and
 *    must be refused by name rather than half-read;
 *  * any file passed on the command line — point it at a real backup on this
 *    machine, in `~/.vivimusic/backups`, to check a file the app itself wrote.
 *
 * The failures are checked as [BackupManager.RestoreResult]s: the point of the
 * fix is not only that a good file restores, but that a bad one says which kind
 * of bad it is.
 */
object BackupRestoreCheck {

    private val work = File(".ignore/backup-restore")
    private val failures = mutableListOf<String>()

    @JvmStatic
    fun main(args: Array<String>) {
        // Before anything reads it: the stores resolve user.home lazily, so every
        // path below lands in the sandbox and the real ~/.vivimusic is untouched.
        val home = File(work, "home").apply { deleteRecursively(); mkdirs() }
        System.setProperty("user.home", home.absolutePath)
        work.mkdirs()

        val marker = "pekinese-" + System.nanoTime()
        DesktopSettings.update { it.copy(listenTogetherUsername = marker) }

        // 1. A backup this build just wrote, restored after the setting changed.
        val fresh = File(work, "fresh.vivide.backup")
        check("export a backup") { BackupManager.export(fresh) }
        DesktopSettings.update { it.copy(listenTogetherUsername = "") }
        val restored = check("restore the backup this build wrote") { BackupManager.import(fresh).ok }
        if (restored && DesktopSettings.load().listenTogetherUsername != marker) {
            failures += "the restore reported success but the settings did not come back"
        }

        // 2. The legacy format: the whole settings object as one bare JSON file.
        val legacy = File(work, "legacy.backup")
        legacy.writeText(sharedJsonPretty.encodeToString(DesktopSyncState.serializer(), DesktopSettings.load()))
        DesktopSettings.update { it.copy(listenTogetherUsername = "") }
        val legacyOk = check("restore a legacy single-JSON file") { BackupManager.import(legacy).ok }
        if (legacyOk && DesktopSettings.load().listenTogetherUsername != marker) {
            failures += "the legacy restore reported success but the settings did not come back"
        }

        // 3. The same thing with a byte-order mark, as an editor would save it.
        val bom = File(work, "legacy-bom.backup")
        bom.writeBytes(
            byteArrayOf(0xEF.toByte(), 0xBB.toByte(), 0xBF.toByte()) +
                sharedJsonPretty.encodeToString(DesktopSyncState.serializer(), DesktopSettings.load()).toByteArray(Charsets.UTF_8),
        )
        DesktopSettings.update { it.copy(listenTogetherUsername = "") }
        val bomOk = check("restore a legacy file with a byte-order mark") { BackupManager.import(bom).ok }
        if (bomOk && DesktopSettings.load().listenTogetherUsername != marker) {
            failures += "the byte-order-mark restore reported success but the settings did not come back"
        }

        // 4. An archive with the entries under a folder.
        val nested = File(work, "nested.vivide.backup")
        zip(nested) { entry ->
            when (entry) {
                "settings.json" -> "vivimusic/settings.json"
                else -> "vivimusic/playlists.json"
            }
        }
        DesktopSettings.update { it.copy(listenTogetherUsername = "") }
        val nestedOk = check("restore an archive whose entries sit in a folder") { BackupManager.import(nested).ok }
        if (nestedOk && DesktopSettings.load().listenTogetherUsername != marker) {
            failures += "the folder-prefixed restore reported success but the settings did not come back"
        }

        // 5. A broken settings entry must be a failure, not a silent half-restore.
        val broken = File(work, "broken-settings.vivide.backup")
        zip(broken, corruptSettings = true) { it }
        val brokenResult = BackupManager.import(broken)
        println("== refuse an archive whose settings entry is corrupt\n   -> ${brokenResult.ok} (${brokenResult.messageKey})")
        if (brokenResult.ok) failures += "a corrupt settings entry was reported as a successful restore"
        if (brokenResult.messageKey != "restore_failed_corrupt") {
            failures += "a corrupt settings entry answered with '${brokenResult.messageKey}', not the corrupt-backup message"
        }

        // 5b. The phone's archive: a real file a user can easily hand to this
        // screen, and one this app can never read. It must be refused by name.
        val phone = File(work, "phone.vivide.backup")
        ZipOutputStream(FileOutputStream(phone).buffered()).use { zip ->
            zip.putNextEntry(ZipEntry("settings.preferences_pb"))
            zip.write(ByteArray(64))
            zip.closeEntry()
            zip.putNextEntry(ZipEntry("vivi.db"))
            zip.write(ByteArray(64))
            zip.closeEntry()
        }
        val phoneResult = BackupManager.import(phone)
        println("== refuse the Android app's archive\n   -> ${phoneResult.ok} (${phoneResult.messageKey})")
        println("   ${phoneResult.detail}")
        if (phoneResult.ok) failures += "the Android app's backup was reported as restored"
        if (!phoneResult.detail.contains("Android")) {
            failures += "the refusal does not name the Android format, so the log cannot tell the user what they picked"
        }

        // 6. Whatever the caller pointed at — a real backup, if there is one.
        args.forEach { path ->
            val file = File(path)
            println()
            println("== $path")
            if (!file.isFile) {
                println("   (no such file)")
                return@forEach
            }
            val result = runCatching { BackupManager.import(file) }
                .onFailure { println("   threw ${it::class.simpleName}: ${it.message}") }
                .getOrNull()
            println("   import -> ${result?.ok ?: false} (${result?.messageKey ?: "threw"})")
            result?.detail?.let { println("   $it") }
            val ok = result?.ok ?: false
            val after = DesktopSettings.load()
            println("   settings read back: username=${after.listenTogetherUsername.ifBlank { "(empty)" }}")
            if (!ok) failures += "$path: the real backup was not restored"
        }

        println()
        if (failures.isEmpty()) {
            println("OK: every backup shape restores, and a corrupt one is refused")
            return
        }
        println("FAILED:")
        failures.forEach { println("  - $it") }
        throw IllegalStateException("backup restore check failed (${failures.size} case(s))")
    }

    /** Runs [block], printing the verdict, and answers whether it said yes. */
    private fun check(what: String, block: () -> Boolean): Boolean {
        val ok = runCatching(block)
            .onFailure { println("== $what\n   threw ${it::class.simpleName}: ${it.message}") }
            .getOrDefault(false)
        println("== $what\n   -> $ok")
        if (!ok) failures += what
        return ok
    }

    /**
     * Writes a two-entry archive: [name] maps each entry's own name, and
     * [corruptSettings] replaces the settings payload with something that cannot
     * be read, so the playlists entry is the only readable one.
     */
    private fun zip(target: File, corruptSettings: Boolean = false, name: (String) -> String) {
        ZipOutputStream(FileOutputStream(target).buffered()).use { zip ->
            listOf("settings.json", "playlists.json").forEach { entry ->
                zip.putNextEntry(ZipEntry(name(entry)))
                val text = when {
                    entry == "settings.json" && corruptSettings -> "{ this is not the settings object"
                    entry == "settings.json" -> sharedJsonPretty.encodeToString(DesktopSyncState.serializer(), DesktopSettings.load())
                    else -> sharedJsonPretty.encodeToString(
                        kotlinx.serialization.builtins.ListSerializer(com.music.vivi.sync.SyncedPlaylist.serializer()),
                        PlaylistStore.toSynced(),
                    )
                }
                zip.write(text.toByteArray(Charsets.UTF_8))
                zip.closeEntry()
            }
        }
    }
}
