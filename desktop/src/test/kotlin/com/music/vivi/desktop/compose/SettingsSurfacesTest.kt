package com.music.vivi.desktop.compose

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.runComposeUiTest
import com.music.vivi.desktop.presentation.screens.LibrarySubDetail
import com.music.vivi.desktop.presentation.screens.SettingsScreen
import com.music.vivi.desktop.presentation.screens.settings.SettingsSection
import com.music.vivi.desktop.ui.theme.PixelPlayerTheme
import kotlin.test.Test
import kotlin.test.assertFalse

/**
 * The two settings surfaces the owner asked for: the imported 1.55 home is the
 * root, PixelPlayer keeps the entire 1.56 surface behind its own entry, and a
 * setting changed on one surface is the same UserSettings value on the other.
 */
@OptIn(ExperimentalTestApi::class)
class SettingsSurfacesTest {

    @Test
    fun pixelPlayerSubmenuKeepsThe155HomeAndSharesItsSettings() {
        val harness = DesktopUiHarness()
        try {
            runComposeUiTest {
                var section by mutableStateOf<SettingsSection?>(null)

                setContent {
                    PixelPlayerTheme {
                        SettingsScreen(
                            viewModel = harness.viewModel,
                            isDarkTheme = false,
                            isDynamicColor = false,
                            onToggleDarkTheme = {},
                            onToggleDynamicColor = {},
                            onRescanLibrary = {},
                            activeSection = section,
                            activeSubDetail = LibrarySubDetail.NONE,
                            onNavigate = { next, _ -> section = next },
                            onBack = { section = null },
                        )
                    }
                }

                // The root is the 1.55 home, and PixelPlayer is its own entry.
                onNodeWithText("PixelPlayer").assertExists()
                onNodeWithText("Intro").assertExists()

                onNodeWithText("PixelPlayer").performClick()
                waitUntil(timeoutMillis = 5_000) {
                    onAllNodesWithText("Music Management").fetchSemanticsNodes().isNotEmpty()
                }

                onNodeWithContentDescription("Back").performClick()
                waitUntil(timeoutMillis = 5_000) {
                    onAllNodesWithText("PixelPlayer").fetchSemanticsNodes().isNotEmpty()
                }

                onNodeWithText("Intro").performScrollTo().performClick()
                waitUntil(timeoutMillis = 5_000) {
                    onAllNodesWithText("Show intro on startup").fetchSemanticsNodes().isNotEmpty()
                }

                // The switch writes the shared UserSettings, the same key the
                // PixelPlayer surface reads.
                onNodeWithTag("intro-show-switch").performClick()
                waitUntil(timeoutMillis = 5_000) { !harness.viewModel.showIntroSplash.value }
                assertFalse(
                    harness.viewModel.showIntroSplash.value,
                    "the Intro switch must reach the shared settings store"
                )
                println("[UI] Settings surfaces: PixelPlayer hub reached, intro splash disabled")
            }
        } finally {
            harness.close()
        }
    }
}
