package com.music.vivi.desktop.compose

import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertIsOff
import androidx.compose.ui.test.assertIsOn
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.test.runComposeUiTest
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import com.music.vivi.desktop.presentation.screens.LibrarySubDetail
import com.music.vivi.desktop.presentation.screens.SettingsScreen
import com.music.vivi.desktop.presentation.screens.settings.Legacy155Actions
import com.music.vivi.desktop.presentation.screens.settings.Legacy155HomeEntries
import com.music.vivi.desktop.presentation.screens.settings.LegacyKind
import com.music.vivi.desktop.presentation.screens.settings.isLegacy155
import com.music.vivi.desktop.presentation.screens.settings.legacyPageFor
import com.music.vivi.desktop.presentation.screens.settings.SettingsSection
import com.music.vivi.desktop.data.youtube.BrowserCookies
import com.music.vivi.desktop.data.youtube.YouTubeAccount
import com.music.vivi.desktop.presentation.screens.settings.Legacy155AccountPage
import com.music.vivi.desktop.presentation.screens.settings.Legacy155SpotifyPage
import com.music.vivi.desktop.presentation.screens.settings.Spotify155
import com.music.vivi.desktop.presentation.viewmodel.YouTubeAccountUiState
import com.music.vivi.desktop.presentation.viewmodel.YouTubeSessionStatus
import com.music.vivi.desktop.data.model.StatsTimeRange
import com.music.vivi.desktop.data.backup.FullBackup
import com.music.vivi.desktop.presentation.screens.settings.DefaultLegacy155Actions
import com.music.vivi.desktop.ui.theme.PixelPlayerTheme
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * The 1.55 home is the root, every one of its rows opens its own 1.55 page, and
 * the fields it shares with PixelPlayer are the same UserSettings values.
 */
@OptIn(ExperimentalTestApi::class)
class SettingsSurfacesTest {

    private fun androidx.compose.ui.test.ComposeUiTest.openSettings(
        harness: DesktopUiHarness,
        sectionState: MutableState<SettingsSection?>,
        actions: Legacy155Actions? = null,
    ) {
        setContent {
            PixelPlayerTheme {
                SettingsScreen(
                    viewModel = harness.viewModel,
                    isDarkTheme = false,
                    isDynamicColor = false,
                    onToggleDarkTheme = { harness.viewModel.toggleDarkMode() },
                    onToggleDynamicColor = { harness.viewModel.toggleDynamicColor() },
                    onRescanLibrary = {},
                    activeSection = sectionState.value,
                    activeSubDetail = LibrarySubDetail.NONE,
                    onNavigate = { next, _ -> sectionState.value = next },
                    onBack = { sectionState.value = null },
                    legacyActions = actions,
                )
            }
        }
    }

    private fun androidx.compose.ui.test.ComposeUiTest.waitForText(text: String) {
        waitUntil(timeoutMillis = 5_000) {
            onAllNodesWithText(text).fetchSemanticsNodes().isNotEmpty()
        }
    }

    @Test
    fun everyImportedHomeEntryOpensItsOwnPageAndReturns() {
        val harness = DesktopUiHarness()
        try {
            runComposeUiTest {
                val sectionState = mutableStateOf<SettingsSection?>(null)
                openSettings(harness, sectionState)

                onNodeWithText("PixelPlayer").assertExists()

                Legacy155HomeEntries.forEach { entry ->
                    onNodeWithText(entry.title).performScrollTo().performClick()
                    onNodeWithTag("legacy155-page").assertExists()
                    waitForText(entry.title)
                    onNodeWithContentDescription("Back").performClick()
                    waitForText("PixelPlayer")
                }

                // The 1.56 hub is reachable only through PixelPlayer, and its
                // own back action returns to the 1.55 home.
                onNodeWithText("PixelPlayer").performClick()
                waitForText("Music Management")
                SettingsSection.entries
                    .filterNot { it.isLegacy155 || it == SettingsSection.PIXELPLAYER }
                    .forEach { section -> onNodeWithText(section.title).assertExists() }
                onNodeWithContentDescription("Back").performClick()
                waitForText("PixelPlayer")

                println("[UI] Settings surfaces: ${Legacy155HomeEntries.size} 1.55 entries open their own page")
            }
        } finally {
            harness.close()
        }
    }

    @Test
    fun sharedOptionsChangeBothSurfaces() {
        val harness = DesktopUiHarness()
        try {
            runComposeUiTest {
                val sectionState = mutableStateOf<SettingsSection?>(null)
                openSettings(harness, sectionState)

                // Appearance -> Theme on the 1.55 surface.
                onNodeWithText("Appearance").performScrollTo().performClick()
                waitForText("Theme")
                onNodeWithText("Theme").performClick()
                waitForText("Dark theme")
                onNodeWithText("Dark theme").performClick()
                waitUntil(timeoutMillis = 5_000) {
                    !harness.viewModel.isDarkMode.value &&
                        !harness.viewModel.userSettings.value.isDarkMode
                }

                // The 1.56 surface reads and writes the same value.
                onNodeWithContentDescription("Back").performClick()
                waitForText("PixelPlayer")
                onNodeWithText("PixelPlayer").performClick()
                waitForText("Music Management")
                onNodeWithText("Appearance").performClick()
                waitForText("Dark Theme")
                onNodeWithText("Dark Theme").performClick()
                waitUntil(timeoutMillis = 5_000) {
                    harness.viewModel.isDarkMode.value &&
                        harness.viewModel.userSettings.value.isDarkMode
                }

                // Back on the 1.55 page, the switch is already on.
                sectionState.value = null
                waitForText("PixelPlayer")
                onNodeWithText("Appearance").performScrollTo().performClick()
                waitForText("Theme")
                onNodeWithText("Theme").performClick()
                waitForText("Dark theme")
                onNodeWithTag("legacy-darkMode").assertIsOn()
                onNodeWithContentDescription("Back").performClick()
                waitForText("PixelPlayer")

                // Dynamic color, the second shared option: 1.55 off, 1.56 on.
                onNodeWithText("Appearance").performScrollTo().performClick()
                waitForText("Theme")
                onNodeWithText("Theme").performClick()
                waitForText("Dynamic color")
                onNodeWithText("Dynamic color").performClick()
                waitUntil(timeoutMillis = 5_000) {
                    !harness.viewModel.isDynamicColorEnabled.value &&
                        !harness.viewModel.userSettings.value.isDynamicColorEnabled
                }
                sectionState.value = null
                waitForText("PixelPlayer")
                onNodeWithText("PixelPlayer").performClick()
                waitForText("Music Management")
                onNodeWithText("Appearance").performClick()
                waitForText("Dynamic Color Theme")
                onNodeWithText("Dynamic Color Theme").performClick()
                waitUntil(timeoutMillis = 5_000) {
                    harness.viewModel.isDynamicColorEnabled.value &&
                        harness.viewModel.userSettings.value.isDynamicColorEnabled
                }
                sectionState.value = null
                waitForText("PixelPlayer")
                onNodeWithText("Appearance").performScrollTo().performClick()
                waitForText("Theme")
                onNodeWithText("Theme").performClick()
                waitForText("Dynamic color")
                onNodeWithTag("legacy-dynamicColor").assertIsOn()

                assertTrue(harness.viewModel.isDarkMode.value)
                assertTrue(harness.viewModel.isDynamicColorEnabled.value)
                println("[UI] Settings surfaces: dark theme and dynamic color stay in sync in both directions")
            }
        } finally {
            harness.close()
        }
    }
    @Test
    fun every155PageHasControlsAndTheOriginalDropdownSets() {
        val pages = SettingsSection.entries.filter { it.isLegacy155 }
        assertEquals(29, pages.size, "the full 1.55 tree is present")
        pages.forEach { section ->
            val spec = legacyPageFor(section)
            assertTrue(spec.sections.isNotEmpty(), "${section.name} has no section")
            spec.sections.forEach { group ->
                assertTrue(group.options.isNotEmpty(), "${section.name}/${group.title} has no option")
                group.options.filter { it.kind == LegacyKind.DROPDOWN }.forEach { option ->
                    assertTrue(option.choices.size >= 2, "${section.name}/${option.key} has a one-entry dropdown")
                    assertEquals(option.choices.size, option.choices.map { it.first }.toSet().size, "${section.name}/${option.key} has duplicate values")
                }
            }
            // About is read-only information; every other page must write a real
            // setting, so no page can quietly become a placeholder.
            if (section != SettingsSection.V155_ABOUT) {
                assertTrue(
                    spec.sections.flatMap { it.options }.any { it.kind != LegacyKind.TEXT },
                    "${section.name} has no switch, dropdown, slider or action",
                )
            }
        }

        fun choices(section: SettingsSection, key: String): Set<String> =
            legacyPageFor(section).sections.flatMap { it.options }
                .first { it.key == key }.choices.map { it.first }.toSet()

        assertEquals(setOf("AUTO", "APPLE_MUSIC", "VIVIMUSIC", "TIDAL"), choices(SettingsSection.V155_CANVAS, "canvasSource"))
        assertEquals(setOf("system", "google_sans", "sans_flex", "outfit", "plus_jakarta_sans", "custom"), choices(SettingsSection.V155_FONT, "selectedFont"))
        assertEquals(setOf("classic", "new", "expressive", "expressive_translucent"), choices(SettingsSection.V155_PLAYER_DESIGN, "playerDesign"))
        assertEquals(setOf("canvas", "gradient", "blur", "glow", "apple_music", "live_mesh", "visualizer"), choices(SettingsSection.V155_PLAYER_DESIGN, "playerBackground"))
        assertEquals(setOf("classic", "new", "apple"), choices(SettingsSection.V155_PLAYER_DESIGN, "miniPlayerDesign"))
        assertEquals(setOf("follow_theme", "gradient", "blur", "glow_motion", "live_mesh"), choices(SettingsSection.V155_PLAYER_DESIGN, "miniPlayerBackgroundStyle"))
        assertEquals(setOf("slim", "squiggly", "wavy"), choices(SettingsSection.V155_PLAYER_DESIGN, "sliderStyle"))
        assertEquals(setOf("off", "fade", "slide"), choices(SettingsSection.V155_TRANSITIONS, "screenTransition"))
        assertEquals(setOf("NONE", "FADE", "GLOW", "SLIDE", "KARAOKE", "APPLE", "APPLE_V2", "VIVIMUSIC_1", "LYRICS_V2", "METRO_LYRICS", "ALPHA"), choices(SettingsSection.V155_LYRICS, "lyricsAnimationStyle"))
        assertEquals(setOf("LEFT", "CENTER", "RIGHT"), choices(SettingsSection.V155_LYRICS, "lyricsTextPosition"))
        assertEquals(setOf("OpenRouter", "OpenAI", "Perplexity", "Claude", "Gemini", "XAi", "Mistral", "DeepL", "Custom"), choices(SettingsSection.V155_AI, "aiProvider"))
        assertEquals(setOf("Literal", "Transcribed"), choices(SettingsSection.V155_AI, "translateMode"))
        assertEquals(setOf("0", "6", "12", "24", "72", "168"), choices(SettingsSection.V155_UPDATES, "updateCheckIntervalHours"))
        assertEquals(setOf("fork", "original"), choices(SettingsSection.V155_UPDATES, "updateSource"))
        assertEquals(setOf("3", "5", "10", "15", "30"), choices(SettingsSection.V155_NOTIFICATIONS, "inAppNotificationDurationSeconds"))
        assertEquals(13, choices(SettingsSection.V155_DENSITY, "densityScale").size)
        assertEquals(4, choices(SettingsSection.V155_DENSITY, "gridItemSize").size)

        // The language and country lists are the 1.55 maps, not a shortlist.
        assertEquals(48, choices(SettingsSection.V155_LANGUAGE, "language").size)
        assertEquals(78, choices(SettingsSection.V155_CONTENT, "contentLanguage").size, "77 languages plus system default")
        assertEquals(110, choices(SettingsSection.V155_CONTENT, "contentCountry").size, "109 countries plus system default")
        assertEquals(77, choices(SettingsSection.V155_AI, "translateLanguage").size, "the AI target language uses the full 1.55 language list")
    }

    private class RecordingActions : Legacy155Actions {
        val calls = mutableListOf<String>()
        var lastSpotify: Pair<String, String>? = null
        var imported: List<String>? = null

        override fun signInYouTube() { calls += "signInYouTube" }
        override fun signOutYouTube() { calls += "signOutYouTube" }
        override fun refreshYouTube() { calls += "refreshYouTube" }
        override fun detectProfiles() { calls += "detectProfiles" }
        override fun useProfile(profile: BrowserCookies.Profile) { calls += "useProfile" }
        override fun switchAccount(id: String) { calls += "switchAccount" }
        override fun removeAccount(id: String) { calls += "removeAccount" }
        override fun exportFullBackup() { calls += "exportFullBackup" }
        override fun restoreFullBackup() { calls += "restoreFullBackup" }
        override fun clearAudioCache() { calls += "clearAudioCache" }
        override fun clearLyricsCache() { calls += "clearLyricsCache" }
        override fun deleteInstallers() { calls += "deleteInstallers" }
        override fun connectSpotify(spDc: String, spKey: String) {
            calls += "connectSpotify"
            lastSpotify = spDc to spKey
        }
        override fun disconnectSpotify() { calls += "disconnectSpotify" }
        override fun refreshSpotify() { calls += "refreshSpotify" }
        override fun importSpotify(selected: List<String>) {
            calls += "importSpotify"
            imported = selected
        }
    }

    @Test
    fun importedPagesInvokeTheRealActionsInsteadOfOnlyShowingText() {
        val harness = DesktopUiHarness()
        try {
            runBlocking {
                harness.musicDao.recordSongPlayed(
                    DesktopUiHarness.song(
                        id = "wrapped-real",
                        title = "Wrapped Real Track",
                        lastPlayedTimestamp = System.currentTimeMillis(),
                        path = "C:/fixture/wrapped-real.mp3"
                    ),
                    durationMs = 120_000L
                )
                harness.statsStateHolder.refreshStats()
            }
            runComposeUiTest {
                val sectionState = mutableStateOf<SettingsSection?>(SettingsSection.V155_ACCOUNT)
                val actions = RecordingActions()
                openSettings(harness, sectionState, actions)

                waitForText("YouTube Music")
                onNodeWithText("Sign in").performScrollTo().performClick()
                onNodeWithText("Find a signed-in browser").performScrollTo().performClick()
                assertTrue(actions.calls.contains("signInYouTube"))
                assertTrue(actions.calls.contains("detectProfiles"))

                sectionState.value = SettingsSection.V155_BACKUP
                waitForText("FULL DEVICE BACKUP")
                onNodeWithTag("legacy-backup-export").performScrollTo().performClick()
                onNodeWithTag("legacy-backup-restore").performScrollTo().performClick()
                assertTrue(actions.calls.containsAll(listOf("exportFullBackup", "restoreFullBackup")))

                sectionState.value = SettingsSection.V155_STORAGE
                waitForText("CACHES")
                onNodeWithTag("legacy-clear-audio").performScrollTo().performClick()
                onNodeWithTag("legacy-clear-lyrics").performScrollTo().performClick()
                onNodeWithTag("legacy-delete-installers").performScrollTo().performClick()
                assertTrue(actions.calls.containsAll(listOf("clearAudioCache", "clearLyricsCache", "deleteInstallers")))

                sectionState.value = SettingsSection.V155_SPOTIFY
                waitForText("SPOTIFY ACCOUNT")
                onNodeWithTag("legacy-spotify-spdc").performScrollTo().performTextInput("cookie")
                onNodeWithTag("legacy-spotify-connect").performScrollTo().performClick()
                assertEquals("connectSpotify", actions.calls.last())
                assertEquals("cookie" to "", actions.lastSpotify)

                sectionState.value = SettingsSection.V155_WRAPPED
                onNodeWithTag("legacy-wrapped-card").assertExists()
                waitForText("Total Plays")
                onNodeWithText("Wrapped Real Track").assertExists()
                onNodeWithText("No listening activity in this range yet.").assertDoesNotExist()
                onNodeWithTag("legacy-wrapped-range-MONTH").performScrollTo().performClick()
                waitUntil(timeoutMillis = 5_000) {
                    harness.viewModel.selectedStatsRange.value == StatsTimeRange.MONTH
                }

                println("[UI] Settings surfaces: ${actions.calls} real actions invoked")
            }
        } finally {
            harness.close()
        }
    }

    @Test
    fun accountPageRoutesSignInRefreshSwitchRemoveAndSignOut() {
        val actions = RecordingActions()
        val state = YouTubeAccountUiState(
            isSignedIn = true,
            status = YouTubeSessionStatus.EXPIRED,
            activeAccountId = "active",
            accounts = listOf(
                YouTubeAccount.Summary("active", "Active account", "active@example.com", null, null, true),
                YouTubeAccount.Summary("other", "Other account", "other@example.com", null, null, false),
            ),
            profiles = listOf(
                BrowserCookies.Profile(
                    browser = "Firefox",
                    profile = "default",
                    family = BrowserCookies.Family.FIREFOX,
                    database = java.nio.file.Path.of("C:/fixture/cookies.sqlite"),
                )
            ),
        )
        runComposeUiTest {
            setContent {
                PixelPlayerTheme {
                    Legacy155AccountPage(state = state, actions = actions)
                }
            }
            onNodeWithText("Sign out").performClick()
            onNodeWithText("Check the session again").performClick()
            onNodeWithText("Switch").performClick()
            onNodeWithContentDescription("Remove account").performClick()
            onNodeWithText("Add account").performClick()
            onNodeWithText("Find a signed-in browser").performClick()
            onNodeWithText("Firefox · default").performClick()

            assertTrue(actions.calls.contains("signOutYouTube"))
            assertTrue(actions.calls.contains("refreshYouTube"))
            assertTrue(actions.calls.contains("switchAccount"))
            assertTrue(actions.calls.contains("removeAccount"))
            assertTrue(actions.calls.contains("signInYouTube"))
            assertTrue(actions.calls.contains("detectProfiles"))
            assertTrue(actions.calls.contains("useProfile"))
        }
    }

    @Test
    fun spotifyPageRoutesConnectRefreshImportAndDisconnect() {
        val actions = RecordingActions()
        val state = Spotify155.State(
            connected = true,
            accountName = "Spotify test",
            playlists = listOf(Spotify155.Playlist("playlist-1", "Imported mix")),
            likedSongsCount = 2,
        )
        runComposeUiTest {
            setContent {
                PixelPlayerTheme {
                    Legacy155SpotifyPage(state = state, actions = actions)
                }
            }
            onNodeWithText("Refresh").performClick()
            onNodeWithText("Select all").performClick()
            onNodeWithTag("legacy-spotify-import").performClick()
            onNodeWithText("Disconnect").performClick()

            assertTrue(actions.calls.contains("refreshSpotify"))
            assertTrue(actions.calls.contains("importSpotify"))
            assertTrue(actions.calls.contains("disconnectSpotify"))
            assertTrue(actions.imported.orEmpty().isNotEmpty())
            assertTrue(actions.imported.orEmpty().contains(Spotify155.LIKED_SOURCE))
            assertTrue(actions.imported.orEmpty().contains("playlist-1"))
        }
    }

    @Test
    fun storageAndBackupControlsWriteTheRealManagers() {
        val harness = DesktopUiHarness()
        val cacheFile = java.io.File(harness.tempDir, "audio-cache/preload.audio").apply {
            parentFile?.mkdirs()
            writeText("cached audio")
        }
        try {
            runComposeUiTest {
                val sectionState = mutableStateOf<SettingsSection?>(SettingsSection.V155_STORAGE)
                openSettings(harness, sectionState)

                waitForText("CACHES")
                onNodeWithTag("legacy-audio-cache-enabled").performClick()
                waitUntil(timeoutMillis = 5_000) {
                    !harness.viewModel.audioCacheEnabled.value &&
                        !harness.viewModel.userSettings.value.audioCacheEnabled
                }
                onNodeWithTag("legacy-clear-audio").performScrollTo().performClick()
                waitUntil(timeoutMillis = 5_000) { !cacheFile.exists() }

                sectionState.value = SettingsSection.V155_BACKUP
                waitForText("AUTOMATIC BACKUP")
                onNodeWithTag("legacy-auto-backup").performClick()
                waitUntil(timeoutMillis = 5_000) {
                    !harness.viewModel.userSettings.value.autoBackupEnabled
                }
                onNodeWithTag("legacy-backup-before-update").performClick()
                waitUntil(timeoutMillis = 5_000) {
                    !harness.viewModel.userSettings.value.backupBeforeUpdate
                }
            }
        } finally {
            harness.close()
        }
    }

    @Test
    fun backupActionExportsAndRestoresTheRealFullBackup() {
        val harness = DesktopUiHarness()
        try {
            val storeFile = java.io.File(harness.tempDir, "device-sync.json")
            val target = java.io.File(harness.tempDir, "roundtrip.${FullBackup.EXTENSION}")
            val actions = DefaultLegacy155Actions(
                viewModel = harness.viewModel,
                scope = harness.scope,
                store = harness.store,
                storeFile = storeFile,
                exportPicker = { _, onPicked -> onPicked(target) },
                restorePicker = { onPicked -> onPicked(target) },
            )
            runBlocking {
                harness.store.update("settings-test") { state ->
                    state.copy(
                        songs = state.songs + DesktopUiHarness.song(
                            id = "backup-real",
                            title = "Backup Real Track",
                            lastPlayedTimestamp = System.currentTimeMillis(),
                            path = "C:/fixture/backup-real.mp3"
                        )
                    )
                }
                val before = harness.store.load().songs.size
                actions.exportFullBackup()
                withTimeout(10_000) {
                    while (!target.isFile) kotlinx.coroutines.delay(20)
                }
                assertTrue(target.isFile)

                harness.store.update("settings-test") { state ->
                    state.copy(songs = emptyList())
                }
                assertEquals(0, harness.store.load().songs.size)

                actions.restoreFullBackup()
                withTimeout(10_000) {
                    while (harness.store.load().songs.size != before) {
                        kotlinx.coroutines.delay(20)
                    }
                }
                assertEquals(before, harness.store.load().songs.size)
            }
        } finally {
            harness.close()
        }
    }

    @Test
    fun legacyOnlyOptionsArePersistedInTheSharedSettingsStore() {
        val harness = DesktopUiHarness()
        try {
            runBlocking {
                harness.viewModel.setLegacySetting("canvasSource", "TIDAL")
                withTimeout(5_000) {
                    harness.viewModel.userSettings.first { it.legacySettings["canvasSource"] == "TIDAL" }
                }
                assertEquals("TIDAL", harness.settingsRepository.getSettings().legacySettings["canvasSource"])
            }
        } finally {
            harness.close()
        }
    }

}
