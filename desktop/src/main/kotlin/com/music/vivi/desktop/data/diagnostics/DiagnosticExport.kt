package com.music.vivi.desktop.data.diagnostics

import com.music.vivi.desktop.BuildConfig
import com.music.vivi.desktop.data.youtube.SecretRedactor
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonArray

/**
 * One file a support report can carry.
 *
 * The package holds what a diagnosis needs (version, system, settings, log
 * files, store size and counts, backup file names) and nothing that can sign
 * anybody in: every text goes through [SecretRedactor], the settings file loses
 * its token and password fields, and the library itself is never copied.
 */
object DiagnosticExport {

    private val json = Json { ignoreUnknownKeys = true; prettyPrint = true }

    fun export(target: File, settingsFile: File, storeFile: File): Result<File> = runCatching {
        val directory = target.absoluteFile.parentFile ?: error("The package has no parent directory.")
        directory.mkdirs()
        val temporary = File(directory, "${target.name}.tmp")
        try {
            ZipOutputStream(temporary.outputStream().buffered()).use { zip ->
                zip.text("system.txt", systemReport())
                zip.text("settings.redacted.json", redactedSettings(settingsFile))
                zip.text("store-summary.txt", storeSummary(storeFile))
                zip.text("backups.txt", backupList(storeFile))
                zip.text("README.txt", readme())
                logFiles(listOfNotNull(settingsFile.parentFile, storeFile.parentFile)).forEach { log ->
                    zip.text("logs/${log.name}", redactFile(log))
                }
            }
            if (target.exists()) target.delete()
            if (!temporary.renameTo(target)) {
                temporary.copyTo(target, overwrite = true)
                temporary.delete()
            }
            target
        } finally {
            if (temporary.exists()) runCatching { temporary.delete() }
        }
    }

    private fun systemReport(): String = buildString {
        appendLine("Axolotl Music ${BuildConfig.APP_VERSION_NAME}")
        appendLine("Application id: ${BuildConfig.APPLICATION_ID}")
        appendLine("OS: ${System.getProperty("os.name")} ${System.getProperty("os.version")} (${System.getProperty("os.arch")})")
        appendLine("Java: ${System.getProperty("java.version")} (${System.getProperty("java.vendor")})")
        appendLine("Locale: ${Locale.getDefault()}")
        appendLine("Timezone: ${java.util.TimeZone.getDefault().id}")
        appendLine("Memory: ${Runtime.getRuntime().maxMemory() / (1024 * 1024)} MB maximum")
        appendLine("Generated: ${SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US).format(Date())}")
    }

    /** Parses the settings file and drops every field that can hold a credential. */
    internal fun redactedSettings(settingsFile: File): String {
        if (!settingsFile.isFile) return "The settings file does not exist at ${settingsFile.absolutePath}."
        val text = runCatching { settingsFile.readText() }.getOrElse { return "The settings file cannot be read: ${it.message}" }
        val element = runCatching { json.parseToJsonElement(text) }.getOrNull()
            ?: return SecretRedactor.redact(text)
        fun scrub(value: kotlinx.serialization.json.JsonElement, key: String = ""): kotlinx.serialization.json.JsonElement =
            when (value) {
                is kotlinx.serialization.json.JsonObject -> kotlinx.serialization.json.JsonObject(
                    value.entries.associate { (name, child) ->
                        name to if (isSecret(name)) kotlinx.serialization.json.JsonPrimitive("<redacted>") else scrub(child, name)
                    }
                )
                is kotlinx.serialization.json.JsonArray -> kotlinx.serialization.json.JsonArray(value.map { scrub(it, key) })
                else -> value
            }
        return json.encodeToString(kotlinx.serialization.json.JsonElement.serializer(), scrub(element))
    }

    private fun isSecret(key: String): Boolean =
        listOf("token", "password", "cookie", "secret", "session", "accessToken", "userHash").any {
            key.contains(it, ignoreCase = true)
        }

    private fun storeSummary(storeFile: File): String {
        if (!storeFile.isFile) return "The store does not exist at ${storeFile.absolutePath}."
        val size = storeFile.length()
        val root = runCatching { json.parseToJsonElement(storeFile.readText()).jsonObject }.getOrNull()
        val library = root?.get("localLibrary") as? kotlinx.serialization.json.JsonObject
        fun count(name: String): Int = (library?.get(name) as? kotlinx.serialization.json.JsonArray)?.size ?: 0
        return buildString {
            appendLine("Store: ${storeFile.absolutePath}")
            appendLine("Bytes: $size")
            appendLine("Modified: ${SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US).format(Date(storeFile.lastModified()))}")
            appendLine("Songs: ${count("songs")}")
            appendLine("Playlists: ${count("playlists")}")
            appendLine("Favorites: ${count("favorites")}")
            appendLine("Playback events: ${count("playbackEvents")}")
            appendLine("Root keys preserved: ${root?.keys?.sorted()?.joinToString(", ").orEmpty()}")
        }
    }

    private fun backupList(storeFile: File): String {
        val directory = File(storeFile.absoluteFile.parentFile, "backups")
        val files = directory.listFiles()?.filter { it.isFile }?.sortedByDescending { it.lastModified() }.orEmpty()
        if (files.isEmpty()) return "No backup files in ${directory.absolutePath}."
        return files.joinToString("\n") { "${it.name} (${it.length()} bytes, ${Date(it.lastModified())})" }
    }

    private fun logFiles(directories: List<File>): List<File> {
        val found = LinkedHashMap<String, File>()
        directories.forEach { directory ->
            listOf(directory, File(directory, "logs")).forEach { candidate ->
                candidate.listFiles { file -> file.isFile && file.name.endsWith(".log", ignoreCase = true) }
                    ?.forEach { file -> found.putIfAbsent(file.name, file) }
            }
        }
        return found.values.sortedByDescending { it.lastModified() }.take(6)
    }

    private fun redactFile(file: File): String = runCatching {
        file.readLines().takeLast(4000).joinToString("\n") { SecretRedactor.redact(it) }
    }.getOrElse { "The log cannot be read: ${it.message}" }

    private fun readme(): String = """
        Axolotl Music diagnostic package.

        Included: app and system version, settings with credentials removed, the
        size and counts of device-sync.json, the names of the backup files, and
        the last lines of the log files with cookie values masked.

        Not included: the library itself, playlists, lyrics, account cookies or
        any token. Review the files before sharing them.
    """.trimIndent()

    private fun ZipOutputStream.text(name: String, body: String) {
        putNextEntry(ZipEntry(name))
        write(body.toByteArray(Charsets.UTF_8))
        closeEntry()
    }
}
