package com.music.vivi.desktop.data.backup

import com.music.vivi.desktop.BuildConfig
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import java.io.File
import java.nio.file.AtomicMoveNotSupportedException
import java.nio.file.Files
import java.nio.file.StandardCopyOption
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream
import java.util.zip.ZipOutputStream

/**
 * A full-device backup: the whole `device-sync.json`, which is both the local
 * library and the data the sync layer of 1.55.0 keeps in that file. Unlike the
 * section backup, this is a byte snapshot, so a restore brings back playlists,
 * favorites, history and the sync identity together.
 *
 * Account cookies are not in that file (they live in `yt-cookie.txt`, sealed by
 * SessionVault) and are deliberately never included. The archive is a zip with a
 * manifest, so the `.vivide.lbkp` name is a convention rather than a new
 * container format; a `.vivide.bkp` or `.vivide.backup` written by an earlier
 * build restores unchanged, because only the entries are read, never the file
 * name. Section backups carry their own `.vivide.sbkp` extension, so a picker
 * or an uninstaller can tell the two kinds apart without opening them.
 */
object FullBackup {

    const val EXTENSION = "vivide.lbkp"
    private const val MANIFEST_ENTRY = "manifest.json"
    private const val STORE_ENTRY = "device-sync.json"
    private const val MAX_ENTRY_BYTES = 64L * 1024 * 1024

    @Serializable
    data class Manifest(
        val app: String,
        val version: String,
        val createdAt: Long,
        val storeFile: String
    )

    private val json = Json {
        prettyPrint = true
        ignoreUnknownKeys = true
        encodeDefaults = true
    }

    fun export(target: File, storeFile: File): Result<Manifest> = runCatching {
        require(storeFile.isFile) { "There is no library to back up at ${storeFile.absolutePath}." }
        val manifest = Manifest(
            app = BuildConfig.APP_NAME,
            version = BuildConfig.APP_VERSION_NAME,
            createdAt = System.currentTimeMillis(),
            storeFile = storeFile.name
        )
        val directory = target.absoluteFile.parentFile ?: error("The backup has no parent directory.")
        directory.mkdirs()
        val temporary = File(directory, "${target.name}.tmp")
        try {
            ZipOutputStream(temporary.outputStream().buffered()).use { zip ->
                zip.putNextEntry(ZipEntry(MANIFEST_ENTRY))
                zip.write(json.encodeToString(manifest).toByteArray(Charsets.UTF_8))
                zip.closeEntry()
                zip.putNextEntry(ZipEntry(STORE_ENTRY))
                storeFile.inputStream().buffered().use { input -> input.copyTo(zip) }
                zip.closeEntry()
            }
            moveIntoPlace(temporary, target)
        } finally {
            if (temporary.exists()) runCatching { temporary.delete() }
        }
        manifest
    }

    fun inspect(file: File): Result<Manifest> = runCatching {
        readArchive(file).first
    }

    /**
     * Replaces the whole store after copying the current file to
     * `device-sync.json.pre-restore-<millis>`. The caller reloads the store and
     * notifies the DAOs.
     */
    fun restore(file: File, storeFile: File): Result<Manifest> = runCatching {
        val (manifest, bytes) = readArchive(file)
        // Refuse a file that is not this app's store before touching anything.
        json.parseToJsonElement(String(bytes, Charsets.UTF_8)).jsonObject
            .let { require(it.containsKey("localLibrary")) { "The backup has no library." } }

        val directory = storeFile.absoluteFile.parentFile ?: error("The store has no parent directory.")
        directory.mkdirs()
        if (storeFile.isFile) {
            Files.copy(
                storeFile.toPath(),
                File(directory, "${storeFile.name}.pre-restore-${System.currentTimeMillis()}").toPath(),
                StandardCopyOption.REPLACE_EXISTING
            )
        }
        val temporary = File(directory, "${storeFile.name}.restore.tmp")
        try {
            Files.write(temporary.toPath(), bytes)
            moveIntoPlace(temporary, storeFile)
        } finally {
            if (temporary.exists()) runCatching { temporary.delete() }
        }
        manifest
    }

    private fun readArchive(file: File): Pair<Manifest, ByteArray> {
        require(file.isFile) { "The backup file does not exist." }
        var manifest: Manifest? = null
        var store: ByteArray? = null
        ZipInputStream(file.inputStream().buffered()).use { zip ->
            while (true) {
                val entry = zip.nextEntry ?: break
                when (entry.name) {
                    MANIFEST_ENTRY -> manifest = json.decodeFromString(
                        Manifest.serializer(),
                        zip.readBounded(MAX_ENTRY_BYTES).toString(Charsets.UTF_8)
                    )
                    STORE_ENTRY -> store = zip.readBounded(MAX_ENTRY_BYTES)
                }
                zip.closeEntry()
            }
        }
        val found = manifest ?: error("The backup has no manifest.")
        require(found.app == BuildConfig.APP_NAME) { "The backup belongs to ${found.app}." }
        val bytes = store ?: error("The backup has no library.")
        return found to bytes
    }

    private fun ZipInputStream.readBounded(limit: Long): ByteArray {
        val output = java.io.ByteArrayOutputStream()
        val buffer = ByteArray(64 * 1024)
        var total = 0L
        while (true) {
            val read = read(buffer)
            if (read < 0) break
            total += read
            require(total <= limit) { "The backup contains an entry that is too large." }
            output.write(buffer, 0, read)
        }
        return output.toByteArray()
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
