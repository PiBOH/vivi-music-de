package com.music.vivi.desktop.utils

import java.io.File

object DesktopPathProvider {

    private val osName = System.getProperty("os.name").lowercase()
    private val userHome = System.getProperty("user.home") ?: "."

    val configDir: File by lazy {
        val dir = when {
            osName.contains("win") -> {
                val appData = System.getenv("APPDATA") ?: "$userHome/AppData/Roaming"
                File(appData, "VIVI Music DE")
            }
            osName.contains("mac") -> {
                File(userHome, "Library/Application Support/VIVI Music DE")
            }
            else -> {
                val xdgConfig = System.getenv("XDG_CONFIG_HOME") ?: "$userHome/.config"
                File(xdgConfig, "vivimusicde")
            }
        }
        if (!dir.exists()) {
            dir.mkdirs()
        }
        dir
    }

    val cacheDir: File by lazy {
        val dir = when {
            osName.contains("win") -> {
                val localAppData = System.getenv("LOCALAPPDATA") ?: "$userHome/AppData/Local"
                File(localAppData, "VIVI Music DE/Cache")
            }
            osName.contains("mac") -> {
                File(userHome, "Library/Caches/VIVI Music DE")
            }
            else -> {
                val xdgCache = System.getenv("XDG_CACHE_HOME") ?: "$userHome/.cache"
                File(xdgCache, "vivimusicde")
            }
        }
        if (!dir.exists()) {
            dir.mkdirs()
        }
        dir
    }

    val artworkCacheDir: File by lazy {
        val dir = File(cacheDir, "artworks")
        if (!dir.exists()) {
            dir.mkdirs()
        }
        dir
    }

    val artistImagesDir: File by lazy {
        val dir = File(configDir, "artist_images")
        if (!dir.exists()) {
            dir.mkdirs()
        }
        dir
    }

    val playlistCoversDir: File by lazy {
        val dir = File(configDir, "playlist_covers")
        if (!dir.exists()) {
            dir.mkdirs()
        }
        dir
    }

    val databaseFile: File by lazy {
        File(configDir, "vivimusicde.db")
    }

    /**
     * The library store: `~/.vivimusic/device-sync.json`, the file 1.55.0 used.
     * It is not under [configDir] because that version wrote it in the user's
     * home, and the whole point of the store is to read the file that is there.
     */
    val syncStateFile: File by lazy {
        File(userHome, ".vivimusic/device-sync.json").apply {
            parentFile?.mkdirs()
        }
    }

    val settingsFile: File by lazy {
        File(configDir, "settings.json")
    }

    val defaultMusicDirectories: List<String> by lazy {
        val defaultMusic = File(userHome, "Music")
        if (defaultMusic.exists() && defaultMusic.isDirectory) {
            listOf(defaultMusic.absolutePath)
        } else {
            listOf(userHome)
        }
    }
}
