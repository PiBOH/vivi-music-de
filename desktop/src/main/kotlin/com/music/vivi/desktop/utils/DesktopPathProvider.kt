package com.music.vivi.desktop.utils

import java.io.File

/**
 * Every file the desktop edition owns lives under `~/.axolotlmusic`: the
 * settings mirror, the library/device store, caches, artist images, playlist
 * covers and the single-instance lock.
 *
 * The 1.55/1.56 beta line wrote them under `.vivimusic` (and, on Windows and
 * macOS, under a "VIVI Music DE" application-data folder). [migrateLegacyData]
 * copies those trees into the new directory once, without overwriting a file
 * the new installation already has and without deleting the old one.
 */
object DesktopPathProvider {

    private val osName = System.getProperty("os.name").lowercase()
    private val userHome = System.getProperty("user.home") ?: "."

    val configDir: File by lazy {
        val override = System.getenv("VIVI_CONFIG_DIR")?.takeIf { it.isNotBlank() }
        val dir = override?.let(::File) ?: File(userHome, ".axolotlmusic")
        if (!dir.exists()) dir.mkdirs()
        migrateLegacyData(dir)
        dir
    }

    val cacheDir: File by lazy {
        val override = System.getenv("VIVI_CACHE_DIR")?.takeIf { it.isNotBlank() }
        if (override != null) {
            val dir = File(override)
            if (!dir.exists()) dir.mkdirs()
            return@lazy dir
        }
        val dir = File(configDir, "cache")
        if (!dir.exists()) dir.mkdirs()
        migrateLegacyCache(dir)
        dir
    }

    val artworkCacheDir: File by lazy {
        val dir = File(cacheDir, "artworks")
        if (!dir.exists()) dir.mkdirs()
        dir
    }

    val artistImagesDir: File by lazy {
        val dir = File(configDir, "artist_images")
        if (!dir.exists()) dir.mkdirs()
        dir
    }

    val playlistCoversDir: File by lazy {
        val dir = File(configDir, "playlist_covers")
        if (!dir.exists()) dir.mkdirs()
        dir
    }

    val databaseFile: File by lazy {
        // Kept under its historical name so a migrated 1.55 installation is
        // still found by LegacyLibraryImport; the new home is configDir.
        File(configDir, "vivimusicde.db")
    }

    /**
     * The library store: `~/.axolotlmusic/device-sync.json`, the 1.55.0 JSON
     * shape (the file the store has always read), now in the Axolotl folder.
     */
    val syncStateFile: File by lazy {
        val override = System.getenv("VIVI_SYNC_STATE_FILE")?.takeIf { it.isNotBlank() }
        override?.let(::File) ?: File(configDir, "device-sync.json").apply { parentFile?.mkdirs() }
    }

    val settingsFile: File by lazy {
        val override = System.getenv("VIVI_SETTINGS_FILE")?.takeIf { it.isNotBlank() }
        override?.let(::File) ?: File(configDir, "settings.json")
    }

    val loginDebugLog: File by lazy {
        File(configDir, "login-debug.log")
    }

    val defaultMusicDirectories: List<String> by lazy {
        val defaultMusic = File(userHome, "Music")
        if (defaultMusic.exists() && defaultMusic.isDirectory) {
            listOf(defaultMusic.absolutePath)
        } else {
            listOf(userHome)
        }
    }

    // ---------------------------------------------------------------------
    // Migration from the former .vivimusic locations
    // ---------------------------------------------------------------------

    private fun legacyConfigDirs(): List<File> = buildList {
        // The 1.56 options win over the older home file, so copy the OS config
        // tree first and let `.vivimusic` fill only the missing keys (the
        // library, pairing and device identity live there).
        when {
            osName.contains("win") -> {
                val appData = System.getenv("APPDATA") ?: "$userHome/AppData/Roaming"
                add(File(appData, "VIVI Music DE"))
            }
            osName.contains("mac") -> add(File(userHome, "Library/Application Support/VIVI Music DE"))
            else -> {
                val xdgConfig = System.getenv("XDG_CONFIG_HOME") ?: "$userHome/.config"
                add(File(xdgConfig, "vivimusicde"))
            }
        }
        add(File(userHome, ".vivimusic"))
    }.distinctBy { it.absolutePath }.filter { it.isDirectory }

    private fun legacyCacheDirs(): List<File> = buildList {
        when {
            osName.contains("win") -> {
                val localAppData = System.getenv("LOCALAPPDATA") ?: "$userHome/AppData/Local"
                add(File(localAppData, "VIVI Music DE/Cache"))
            }
            osName.contains("mac") -> add(File(userHome, "Library/Caches/VIVI Music DE"))
            else -> {
                val xdgCache = System.getenv("XDG_CACHE_HOME") ?: "$userHome/.cache"
                add(File(xdgCache, "vivimusicde"))
            }
        }
    }.distinctBy { it.absolutePath }.filter { it.isDirectory }

    private fun migrateLegacyData(target: File) {
        val marker = File(target, ".migrated-from-vivimusic")
        if (marker.isFile) return
        legacyConfigDirs().forEach { legacy -> copyTree(legacy, target) }
        if (!marker.exists()) {
            runCatching { marker.writeText("1", Charsets.UTF_8) }
        }
    }

    private fun migrateLegacyCache(target: File) {
        val marker = File(target, ".migrated-from-vivimusic")
        if (marker.isFile) return
        legacyCacheDirs().forEach { legacy -> copyTree(legacy, target) }
        if (!marker.exists()) {
            runCatching { marker.writeText("1", Charsets.UTF_8) }
        }
    }

    /**
     * Copies every file of [source] that does not already exist under [target].
     * The legacy folder is left untouched, so a downgrade still finds its data.
     */
    private fun copyTree(source: File, target: File) {
        val sourceRoot = runCatching { source.canonicalFile }.getOrDefault(source)
        sourceRoot.walkTopDown().forEach { entry ->
            val relative = entry.relativeTo(sourceRoot).path
            if (relative.isEmpty()) return@forEach
            val destination = File(target, relative)
            if (entry.isDirectory) {
                destination.mkdirs()
            } else if (entry.isFile && !destination.exists()) {
                destination.parentFile?.mkdirs()
                runCatching { entry.copyTo(destination, overwrite = false) }
            }
        }
    }
}
