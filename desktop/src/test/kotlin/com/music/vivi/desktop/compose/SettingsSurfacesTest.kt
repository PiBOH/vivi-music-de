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
import com.music.vivi.desktop.presentation.screens.LibrarySubDetail
import com.music.vivi.desktop.presentation.screens.SettingsScreen
import com.music.vivi.desktop.presentation.screens.settings.Legacy155HomeEntries
import com.music.vivi.desktop.presentation.screens.settings.SettingsSection
import com.music.vivi.desktop.ui.theme.PixelPlayerTheme
import kotlin.test.Test
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
                onNodeWithText("Removed features").assertExists()

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
                waitUntil(timeoutMillis = 5_000) { !harness.viewModel.isDarkMode.value }

                // The 1.56 surface reads and writes the same value.
                onNodeWithContentDescription("Back").performClick()
                waitForText("PixelPlayer")
                onNodeWithText("PixelPlayer").performClick()
                waitForText("Music Management")
                onNodeWithText("Appearance").performClick()
                waitForText("Dark Theme")
                onNodeWithText("Dark Theme").performClick()
                waitUntil(timeoutMillis = 5_000) { harness.viewModel.isDarkMode.value }

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
                waitUntil(timeoutMillis = 5_000) { !harness.viewModel.isDynamicColorEnabled.value }
                sectionState.value = null
                waitForText("PixelPlayer")
                onNodeWithText("PixelPlayer").performClick()
                waitForText("Music Management")
                onNodeWithText("Appearance").performClick()
                waitForText("Dynamic Color Theme")
                onNodeWithText("Dynamic Color Theme").performClick()
                waitUntil(timeoutMillis = 5_000) { harness.viewModel.isDynamicColorEnabled.value }
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
}
