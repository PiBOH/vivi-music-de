package com.music.vivi.desktop.data.network.github

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import com.music.vivi.desktop.domain.model.AppReleaseInfo
import com.music.vivi.desktop.domain.model.ReleaseAsset

class UpdateAssetsTest {

    private fun asset(name: String) = ReleaseAsset(name = name, downloadUrl = "https://example.invalid/$name")

    private fun release(vararg names: String) = AppReleaseInfo(
        versionTag = "test",
        versionName = "test",
        releaseNotes = "",
        htmlUrl = "",
        assets = names.map(::asset)
    )

    @Test
    fun picksTheWindowsSetupOnWindows() {
        val release = release(
            "app.AppImage",
            "app-arm64.dmg",
            "app-setup.exe",
            "app.msi"
        )
        assertEquals("app-setup.exe", UpdateAssets.forCurrentPlatform(release, "Windows 11")?.name)
    }

    @Test
    fun picksTheDmgOnMac() {
        val release = release("app-setup.exe", "app-arm64.dmg", "app.AppImage")
        assertEquals("app-arm64.dmg", UpdateAssets.forCurrentPlatform(release, "Mac OS X")?.name)
    }

    @Test
    fun picksTheAppImageElsewhere() {
        val release = release("app-setup.exe", "app-x64.dmg", "app.AppImage")
        assertEquals("app.AppImage", UpdateAssets.forCurrentPlatform(release, "Linux")?.name)
    }

    @Test
    fun fallsBackToTheSetupWhenThePlatformAssetIsMissing() {
        val release = release("app.msi", "app-setup.exe")
        assertEquals("app-setup.exe", UpdateAssets.forCurrentPlatform(release, "Linux")?.name)
        assertNull(UpdateAssets.forCurrentPlatform(release("app.msi"), "Linux"))
    }
}
