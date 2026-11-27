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
import androidx.compose.ui.test.runComposeUiTest
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import com.music.vivi.desktop.presentation.screens.LibrarySubDetail
import com.music.vivi.desktop.presentation.screens.SettingsScreen
import com.music.vivi.desktop.presentation.screens.settings.Legacy155HomeEntries
import com.music.vivi.desktop.presentation.screens.settings.LegacyKind
import com.music.vivi.desktop.presentation.screens.settings.isLegacy155
import com.music.vivi.desktop.presentation.screens.settings.legacyPageFor
import com.music.vivi.desktop.presentation.screens.settings.SettingsSection
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
