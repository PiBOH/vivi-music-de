package com.music.vivi.desktop.data.network.github

import com.music.vivi.desktop.domain.model.AppReleaseInfo
import com.music.vivi.desktop.domain.model.ReleaseAsset

/**
 * The release asset the updater downloads on the system it runs on.
 *
 * The live checksum harness calls this same function, so the installer it
 * verifies is the one `UpdateStateHolder.downloadUpdate` would download.
 */
internal object UpdateAssets {

    fun forCurrentPlatform(
        release: AppReleaseInfo,
        osName: String = System.getProperty("os.name").orEmpty()
    ): ReleaseAsset? {
        val os = osName.lowercase()
        val suffix = when {
            os.contains("win") -> "-setup.exe"
            os.contains("mac") -> ".dmg"
            else -> ".AppImage"
        }
        return release.assets.firstOrNull { it.name.endsWith(suffix, ignoreCase = true) }
            ?: release.assets.firstOrNull { it.name.endsWith("-setup.exe", ignoreCase = true) }
    }
}
