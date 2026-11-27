package com.music.vivi.desktop.data.youtube

import com.sun.jna.Platform
import java.io.File
import java.nio.file.AtomicMoveNotSupportedException
import java.nio.file.Files
import java.nio.file.StandardCopyOption
import java.nio.file.attribute.PosixFileAttributeView
import java.nio.file.attribute.PosixFilePermission
import java.security.SecureRandom
import java.util.Base64
import java.util.concurrent.TimeUnit
import javax.crypto.Cipher
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.SecretKeySpec

/**
 * Seals the YouTube Music session on disk.
 *
 * A cookie jar is a bearer credential: anyone who can read `yt-cookie.txt` can
 * act as the account. The file is therefore never written in plaintext.
 *
 * - **Windows**: the payload is wrapped by DPAPI (`CryptProtectData`) with an
 *   application entropy, so only this Windows user on this machine can unwrap
 *   it, and the OS masters the key.
 * - **macOS**: a random AES-256 key lives in the login keychain (`security`),
 *   and the file is AES-GCM under it.
 * - **Linux**: the same AES-256 key lives in the Secret Service
 *   (`secret-tool`), the system keystore on a desktop session.
 *
 * When no keystore can be reached the session is not persisted at all: [write]
 * removes the file rather than leaving a readable cookie on disk. A plaintext
 * file left by an earlier build is re-sealed by [read] before it is returned.
 * The container is `VIVICOK1` + a protector byte + the sealed payload, written
 * through a temporary file and an atomic rename. On POSIX the file is 0600.
 */
internal object SessionVault {

    private val MAGIC = "VIVICOK1".toByteArray(Charsets.US_ASCII)
    private const val PROTECTOR_DPAPI = 1
    private const val PROTECTOR_AES = 2
    private const val IV_BYTES = 12
    private const val TAG_BITS = 128
    private const val KEY_BYTES = 32

    private const val KEYCHAIN_SERVICE = "com.music.vivi.desktop.session"
    private const val KEYCHAIN_ACCOUNT = "yt-session-key"
    private const val SECRET_TOOL_APPLICATION = "vivi-music-de"
    private const val SECRET_TOOL_PURPOSE = "yt-session-key"

    private val entropy = "VIVI Music DE/session/v1".toByteArray(Charsets.UTF_8)
    private val random = SecureRandom()

    @Volatile
    private var cachedKey: ByteArray? = null

    /** The protector this host uses, for a log line that carries no secret. */
    val protector: String
        get() = when {
            Platform.isWindows() -> "DPAPI"
            Platform.isMac() -> "the macOS keychain"
            Platform.isLinux() -> "the Secret Service"
            else -> "none"
        }

    /** Reads a sealed session, re-sealing a plaintext one written by an older build. */
    fun read(file: File): String? {
        if (!file.isFile) return null
        val bytes = runCatching { file.readBytes() }.getOrNull() ?: return null
        if (bytes.isEmpty()) return null

        val sealed = bytes.size >= MAGIC.size && MAGIC.indices.all { bytes[it] == MAGIC[it] }
        if (!sealed) {
            val legacy = runCatching { String(bytes, Charsets.UTF_8).trim() }.getOrNull()
            if (legacy.isNullOrBlank()) {
                delete(file)
                return null
            }
            // The plaintext file is gone before this call returns: either it was
            // sealed in place, or [write] removed it because this machine has no
            // keystore and the session will only live in memory for this run.
            write(file, legacy)
            return legacy
        }

        val body = bytes.copyOfRange(MAGIC.size, bytes.size)
        if (body.size < 2) return null
        return when (body[0].toInt()) {
            PROTECTOR_DPAPI -> runCatching {
                com.sun.jna.platform.win32.Crypt32Util
                    .cryptUnprotectData(
                        body.copyOfRange(1, body.size),
                        entropy,
                        com.sun.jna.platform.win32.WinCrypt.CRYPTPROTECT_UI_FORBIDDEN,
                        null
                    )
                    .toString(Charsets.UTF_8)
            }.getOrNull()

            PROTECTOR_AES -> unsealAes(body.copyOfRange(1, body.size))
            else -> null
        }
    }

    /**
     * Seals [text] into [file]. Returns false when this host has no keystore;
     * in that case the file is removed, so a caller can keep the value in
     * memory without ever leaving it unprotected on disk.
     */
    fun write(file: File, text: String): Boolean {
        val sealed = seal(text) ?: run {
            delete(file)
            return false
        }
        val parent = file.absoluteFile.parentFile ?: return false
        parent.mkdirs()
        val temporary = File(parent, "${file.name}.tmp")
        return try {
            Files.write(temporary.toPath(), sealed)
            restrict(temporary)
            moveIntoPlace(temporary, file)
            true
        } catch (_: Throwable) {
            temporary.delete()
            false
        }
    }

    /** Removes [files] and the temporary copy each write uses. */
    fun delete(vararg files: File) {
        files.forEach { file ->
            runCatching { file.delete() }
            runCatching { File(file.absoluteFile.parentFile, "${file.name}.tmp").delete() }
        }
    }

    /** Drops the host keystore entry, so a deleted session cannot be reopened. */
    fun forget() {
        cachedKey = null
        when {
            Platform.isMac() -> runCommand(
                listOf("security", "delete-generic-password", "-s", KEYCHAIN_SERVICE, "-a", KEYCHAIN_ACCOUNT)
            )
            Platform.isLinux() -> runCommand(
                listOf(
                    "secret-tool", "clear",
                    "application", SECRET_TOOL_APPLICATION,
                    "purpose", SECRET_TOOL_PURPOSE
                )
            )
        }
    }

    private fun seal(text: String): ByteArray? {
        val plain = text.toByteArray(Charsets.UTF_8)
        if (Platform.isWindows()) {
            val sealed = runCatching {
                com.sun.jna.platform.win32.Crypt32Util.cryptProtectData(
                    plain,
                    entropy,
                    com.sun.jna.platform.win32.WinCrypt.CRYPTPROTECT_UI_FORBIDDEN,
                    "Axolotl Music session",
                    null
                )
            }.getOrNull() ?: return null
            return MAGIC + byteArrayOf(PROTECTOR_DPAPI.toByte()) + sealed
        }
        val key = keystoreKey() ?: return null
        val iv = ByteArray(IV_BYTES).also(random::nextBytes)
        val body = runCatching {
            Cipher.getInstance("AES/GCM/NoPadding").run {
                init(Cipher.ENCRYPT_MODE, SecretKeySpec(key, "AES"), GCMParameterSpec(TAG_BITS, iv))
                doFinal(plain)
            }
        }.getOrNull() ?: return null
        return MAGIC + byteArrayOf(PROTECTOR_AES.toByte()) + iv + body
    }

    private fun unsealAes(body: ByteArray): String? {
        if (body.size <= IV_BYTES) return null
        val key = keystoreKey() ?: return null
        val iv = body.copyOfRange(0, IV_BYTES)
        val cipherText = body.copyOfRange(IV_BYTES, body.size)
        return runCatching {
            Cipher.getInstance("AES/GCM/NoPadding").run {
                init(Cipher.DECRYPT_MODE, SecretKeySpec(key, "AES"), GCMParameterSpec(TAG_BITS, iv))
                String(doFinal(cipherText), Charsets.UTF_8)
            }
        }.getOrNull()
    }

    /** The host key, created on first use and kept in the OS keystore. */
    private fun keystoreKey(): ByteArray? {
        cachedKey?.let { return it }
        val encoded = readKeystoreSecret() ?: Base64.getEncoder().withoutPadding()
            .encodeToString(ByteArray(KEY_BYTES).also(random::nextBytes))
            .also { generated -> if (!writeKeystoreSecret(generated)) return null }
        val key = runCatching { Base64.getDecoder().decode(encoded) }.getOrNull() ?: return null
        if (key.size != KEY_BYTES) return null
        cachedKey = key
        return key
    }

    private fun readKeystoreSecret(): String? = when {
        Platform.isMac() -> runCommand(
            listOf(
                "security", "find-generic-password",
                "-s", KEYCHAIN_SERVICE, "-a", KEYCHAIN_ACCOUNT, "-w"
            )
        ).takeIf { it.ok }?.output?.trim()?.ifBlank { null }

        Platform.isLinux() -> runCommand(
            listOf(
                "secret-tool", "lookup",
                "application", SECRET_TOOL_APPLICATION,
                "purpose", SECRET_TOOL_PURPOSE
            )
        ).takeIf { it.ok }?.output?.trim()?.ifBlank { null }

        else -> null
    }

    private fun writeKeystoreSecret(value: String): Boolean = when {
        Platform.isMac() -> runCommand(
            listOf(
                "security", "add-generic-password", "-U",
                "-s", KEYCHAIN_SERVICE, "-a", KEYCHAIN_ACCOUNT, "-w", value
            )
        ).ok

        Platform.isLinux() -> runCommand(
            listOf(
                "secret-tool", "store", "--label=Axolotl Music YouTube session key",
                "application", SECRET_TOOL_APPLICATION,
                "purpose", SECRET_TOOL_PURPOSE
            ),
            stdin = value
        ).ok

        else -> false
    }

    private data class CommandResult(val ok: Boolean, val output: String)

    private fun runCommand(command: List<String>, stdin: String? = null): CommandResult = runCatching {
        val process = ProcessBuilder(command).redirectErrorStream(true).start()
        process.outputStream.use { input ->
            if (stdin != null) input.write(stdin.toByteArray(Charsets.UTF_8))
        }
        val output = process.inputStream.bufferedReader().use { it.readText() }
        if (!process.waitFor(4, TimeUnit.SECONDS)) {
            process.destroyForcibly()
            CommandResult(false, output)
        } else {
            CommandResult(process.exitValue() == 0, output)
        }
    }.getOrDefault(CommandResult(false, ""))

    private fun restrict(file: File) {
        runCatching {
            val view = Files.getFileAttributeView(file.toPath(), PosixFileAttributeView::class.java)
                ?: return
            view.setPermissions(setOf(PosixFilePermission.OWNER_READ, PosixFilePermission.OWNER_WRITE))
        }
    }

    private fun moveIntoPlace(source: File, target: File) {
        try {
            Files.move(
                source.toPath(),
                target.toPath(),
                StandardCopyOption.ATOMIC_MOVE,
                StandardCopyOption.REPLACE_EXISTING
            )
        } catch (_: AtomicMoveNotSupportedException) {
            Files.move(source.toPath(), target.toPath(), StandardCopyOption.REPLACE_EXISTING)
        }
    }
}
