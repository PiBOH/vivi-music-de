package com.music.vivi.desktop.dev

import com.music.vivi.desktop.data.backup.FullBackup
import com.music.vivi.desktop.data.youtube.SecretRedactor
import com.music.vivi.desktop.data.youtube.SessionVault
import com.music.vivi.desktop.utils.DesktopPathProvider
import com.sun.jna.Platform
import java.io.File
import java.nio.file.Files
import java.nio.file.attribute.PosixFilePermission
import java.util.zip.ZipInputStream
import kotlin.system.exitProcess

/**
 * Proves the session-at-rest guarantees with real files, without touching the
 * session this machine is signed in with: everything happens in a temporary
 * directory.
 *
 * - a session written by [SessionVault] is sealed (the container carries the
 *   `VIVICOK1` header and the file contains neither the cookie names nor the
 *   value) and reads back exactly;
 * - a plaintext file from an older build is read once and re-sealed in place;
 * - the cleanup sign-out uses removes the sealed files and their temporary copy;
 * - a full backup holds only the manifest and the library, never the session;
 * - [SecretRedactor] masks cookie headers but leaves a diagnostic that only
 *   names cookies readable;
 * - the files this machine already has are scanned for cookie assignments, so
 *   the claim is about the logs on disk and not only about the code.
 */
object SessionVaultCheck {

    const val FLAG = "--session-vault-check"

    /** Seals the session files this installation already has, in place. */
    const val SEAL_FLAG = "--seal-session"

    private const val SECRET_VALUE = "AQBcE9f0veryLongSecretValueThatMustNotBeWritten0123456789"
    private const val HSID_VALUE = "HSIDVALUE0123456789"
    private const val SAPISID_VALUE = "SAPISIDVALUE987654321"
    private const val SESSION = "SID=$SECRET_VALUE; HSID=$HSID_VALUE; SAPISID=$SAPISID_VALUE"

    fun handle(args: Array<String>): Boolean {
        if (args.contains(SEAL_FLAG)) exitProcess(sealExisting())
        if (!args.contains(FLAG)) return false
        exitProcess(run())
    }

    /**
     * Migrates the real `yt-cookie.txt` and `yt-context.txt` of this installation:
     * the same [SessionVault.read] the app runs at startup, so a machine that was
     * signed in before this build stops having a plaintext cookie immediately.
     * Only sizes and container states are printed; never a value.
     */
    private fun sealExisting(): Int {
        val files = listOf(
            File(DesktopPathProvider.configDir, "yt-cookie.txt"),
            File(DesktopPathProvider.configDir, "yt-context.txt"),
        )
        var failures = 0
        files.forEach { file ->
            if (!file.isFile) {
                println("[session] ${file.name}: absent")
                return@forEach
            }
            val before = runCatching { file.readBytes() }.getOrElse { ByteArray(0) }
            val wasSealed = isSealed(before)
            val readable = SessionVault.read(file) != null
            val after = runCatching { file.readBytes() }.getOrElse { ByteArray(0) }
            val sealedNow = isSealed(after)
            val state = when {
                wasSealed -> "already sealed"
                sealedNow -> "sealed"
                after.isEmpty() -> "removed (no keystore)"
                else -> "still plaintext"
            }
            println(
                "[session] ${file.name}: $state, readable=$readable, " +
                    "${before.size} -> ${after.size} bytes"
            )
            if (!readable || !sealedNow) failures++
        }
        val pass = failures == 0
        println("[session] RESULT: ${if (pass) "PASS" else "FAIL"}")
        return if (pass) 0 else 1
    }

    private fun isSealed(bytes: ByteArray): Boolean =
        bytes.size > 8 && String(bytes, 0, 8, Charsets.US_ASCII) == "VIVICOK1"

    private fun run(): Int {
        val failures = mutableListOf<String>()
        val directory = Files.createTempDirectory("vivi-session-check").toFile()

        fun expect(condition: Boolean, message: String) {
            println("[session] ${if (condition) "ok" else "FAIL"}: $message")
            if (!condition) failures += message
        }

        try {
            println("[session] protector: ${SessionVault.protector}")

            // 1. Sealed round trip.
            val store = File(directory, "yt-cookie.txt")
            expect(SessionVault.write(store, SESSION), "the session is sealed with ${SessionVault.protector}")
            val bytes = store.readBytes()
            expect(
                bytes.size > 8 && String(bytes, 0, 8, Charsets.US_ASCII) == "VIVICOK1",
                "the file carries the VIVICOK1 container header"
            )
            val raw = String(bytes, Charsets.ISO_8859_1)
            expect(
                !raw.contains(SECRET_VALUE) && !raw.contains("SID=") && !raw.contains("HSID="),
                "neither the cookie value nor a cookie name is written in the file"
            )
            expect(SessionVault.read(store) == SESSION, "the sealed session reads back exactly")

            // 2. POSIX mode, when the host has one.
            val mode = runCatching { Files.getPosixFilePermissions(store.toPath()) }.getOrNull()
            if (Platform.isWindows() || mode == null) {
                println("[session] file mode: n/a (${SessionVault.protector} protects the content)")
            } else {
                expect(
                    mode == setOf(PosixFilePermission.OWNER_READ, PosixFilePermission.OWNER_WRITE),
                    "the session file is owner read/write only (${mode})"
                )
            }

            // 3. A plaintext file from an older build is migrated on the first read.
            val legacy = File(directory, "legacy-cookie.txt")
            legacy.writeText("SID=$SECRET_VALUE; SAPISID=$SAPISID_VALUE")
            expect(
                SessionVault.read(legacy) == "SID=$SECRET_VALUE; SAPISID=$SAPISID_VALUE",
                "a plaintext session from an older build is still read"
            )
            val migrated = legacy.readBytes()
            expect(
                migrated.size > 8 && String(migrated, 0, 8, Charsets.US_ASCII) == "VIVICOK1",
                "the plaintext file is re-sealed in place on that read"
            )
            expect(
                !String(migrated, Charsets.ISO_8859_1).contains(SECRET_VALUE),
                "the plaintext value is gone from the migrated file"
            )

            // 4. The cleanup sign-out runs.
            val context = File(directory, "yt-context.txt")
            SessionVault.write(context, "DATASYNC_ID\nVISITOR_DATA")
            File(directory, "yt-cookie.txt.tmp").writeText("leftover")
            SessionVault.delete(store, context, legacy)
            expect(
                !store.exists() && !context.exists() && !legacy.exists() &&
                    !File(directory, "yt-cookie.txt.tmp").exists(),
                "sign-out cleanup removes the sealed files and their temporary copy"
            )
            expect(SessionVault.read(store) == null, "a deleted session reads as signed out")

            // 5. Backup holds the library and nothing of the session.
            val library = File(directory, "device-sync.json").apply {
                writeText("""{"localLibrary":{"songs":[],"playlists":[]}}""")
            }
            val backup = File(directory, "check.vivide.lbkp")
            val manifest = FullBackup.export(backup, library).getOrThrow()
            val entries = readZipEntries(backup)
            expect(
                entries.keys == setOf("manifest.json", "device-sync.json"),
                "the backup holds only the manifest and the library (${entries.keys})"
            )
            val dump = entries.entries.joinToString("\n") { "${it.key}\n${it.value}" }
            expect(
                !dump.contains("yt-cookie") && !dump.contains("yt-context") &&
                    !dump.contains(SECRET_VALUE) && !dump.contains("SID="),
                "the backup contains no session file and no cookie value"
            )
            expect(
                FullBackup.inspect(backup).getOrThrow() == manifest,
                "the backup manifest reads back"
            )

            // 6. Redaction masks a header but leaves diagnostics readable.
            val header = "Cookie: SID=$SECRET_VALUE; HSID=$HSID_VALUE; Authorization: SAPISIDHASH 1712345678_0123456789abcdef"
            val redacted = SecretRedactor.redact(header)
            expect(
                !redacted.contains(SECRET_VALUE) && !redacted.contains(HSID_VALUE) &&
                    !redacted.contains("1712345678_0123456789abcdef"),
                "a quoted cookie header is masked: $redacted"
            )
            val assignment = "SAPISID=$SAPISID_VALUE"
            expect(
                !SecretRedactor.redact(assignment).contains(SAPISID_VALUE),
                "a bare NAME=value assignment is masked"
            )
            val benign = "captured 12 cookies: [SID, HSID, SAPISID] | missing critical: [] | scoped=12"
            expect(
                SecretRedactor.redact(benign) == benign,
                "a diagnostic that only names cookies is left readable"
            )

            // 7. Scan the logs this machine already has.
            val logs = listOf(
                File(DesktopPathProvider.configDir, "youtube.log"),
                com.music.vivi.desktop.utils.DesktopPathProvider.loginDebugLog,
            ).filter { it.isFile }
            if (logs.isEmpty()) {
                println("[session] log audit: no log file exists yet")
            }
            val assignmentPattern = Regex(
                "(?i)\\b(SID|HSID|SSID|APISID|SAPISID|LOGIN_INFO|__Secure-[13]P(?:SID|APISID)(?:TS|CC)?|SIDCC|LSID)\\s*="
            )
            logs.forEach { log ->
                val hits = runCatching {
                    log.useLines { lines -> lines.count { assignmentPattern.containsMatchIn(it) } }
                }.getOrDefault(-1)
                println("[session] log audit: ${log.absolutePath} -> $hits cookie assignment line(s)")
                expect(hits == 0, "${log.name} holds no cookie value")
            }

            val pass = failures.isEmpty()
            println("[session] RESULT: ${if (pass) "PASS" else "FAIL"}")
            return if (pass) 0 else 1
        } catch (t: Throwable) {
            println("[session] FAIL: ${t.javaClass.simpleName} - ${t.message}")
            println("[session] RESULT: FAIL")
            return 1
        } finally {
            directory.deleteRecursively()
        }
    }

    private fun readZipEntries(file: File): Map<String, String> {
        val entries = linkedMapOf<String, String>()
        ZipInputStream(file.inputStream().buffered()).use { zip ->
            while (true) {
                val entry = zip.nextEntry ?: break
                entries[entry.name] = zip.readBytes().toString(Charsets.UTF_8)
                zip.closeEntry()
            }
        }
        return entries
    }
}
