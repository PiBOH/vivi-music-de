package com.music.vivi.desktop.compose

import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.runComposeUiTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

import com.music.vivi.desktop.presentation.miniPlayerWindows
import com.music.vivi.desktop.presentation.screens.MainAppScaffold
import com.music.vivi.desktop.ui.theme.PixelPlayerTheme

/**
 * The mini player swap, exercised through the widgets instead of read off
 * `Main.kt`: the capsule opens the full player, the full player's header button
 * is pressed in a headless Compose scene, and the state it writes is the one
 * the two windows read. No pointer is moved on the machine, no window opens.
 */
@OptIn(ExperimentalTestApi::class)
class MiniPlayerModeTest {

    @Test
    fun miniPlayerButtonInFullPlayerTurnsMiniPlayerModeOn() {
        val harness = DesktopUiHarness()
        try {
            val song = DesktopUiHarness.song(
                id = "mini-mode-song",
                title = "Mini Mode Song",
                lastPlayedTimestamp = System.currentTimeMillis(),
                path = "C:/fixture/mini-mode.mp3"
            )
            harness.viewModel.playSongs(listOf(song), 0)
            val deadline = System.currentTimeMillis() + 5_000
            while (harness.viewModel.currentSong.value?.id != song.id && System.currentTimeMillis() < deadline) {
                Thread.sleep(20)
            }
            assertEquals(
                song.id,
                harness.viewModel.currentSong.value?.id,
                "the mini bar is only drawn once a song is loaded"
            )
            assertFalse(
                harness.viewModel.isMiniPlayerMode.value,
                "mini player mode must start off"
            )

            runComposeUiTest {
                setContent {
                    PixelPlayerTheme {
                        MainAppScaffold(viewModel = harness.viewModel)
                    }
                }

                // The capsule's own expand path, the one a user presses.
                onNodeWithTag("mini-bar-expand").performClick()
                waitUntil(timeoutMillis = 10_000) {
                    onAllNodesWithTag("full-player-mini-player").fetchSemanticsNodes().isNotEmpty()
                }
                assertFalse(
                    harness.viewModel.isMiniPlayerMode.value,
                    "opening the full player must not enter mini player mode by itself"
                )

                onNodeWithTag("full-player-mini-player").performClick()
                waitUntil(timeoutMillis = 5_000) { harness.viewModel.isMiniPlayerMode.value }
                assertTrue(
                    harness.viewModel.isMiniPlayerMode.value,
                    "the header button must reach PlayerViewModel.setMiniPlayerMode(true)"
                )
            }

            val windows = miniPlayerWindows(
                isWindowVisible = true,
                isMiniPlayerMode = harness.viewModel.isMiniPlayerMode.value,
                isExiting = false
            )
            assertFalse(windows.mainVisible, "the shell must hide in mini player mode")
            assertTrue(windows.miniCreated, "the mini window must be created in mini player mode")
            assertTrue(windows.miniVisible, "the mini window must be visible in mini player mode")
            println(
                "[UI] Full player Mini Player mode=${harness.viewModel.isMiniPlayerMode.value} " +
                    "mainVisible=${windows.mainVisible} miniCreated=${windows.miniCreated} " +
                    "miniVisible=${windows.miniVisible}"
            )
        } finally {
            harness.close()
        }
    }

    @Test
    fun miniPlayerWindowsHideTheShellAndShowTheSmallOne() {
        val mini = miniPlayerWindows(isWindowVisible = true, isMiniPlayerMode = true, isExiting = false)
        assertFalse(mini.mainVisible)
        assertTrue(mini.miniCreated)
        assertTrue(mini.miniVisible)
        println(
            "[UI] Mini mode mainVisible=${mini.mainVisible} " +
                "miniCreated=${mini.miniCreated} miniVisible=${mini.miniVisible}"
        )

        val full = miniPlayerWindows(isWindowVisible = true, isMiniPlayerMode = false, isExiting = false)
        assertTrue(full.mainVisible)
        assertFalse(full.miniCreated)
        assertFalse(full.miniVisible)
        println(
            "[UI] Full mode mainVisible=${full.mainVisible} " +
                "miniCreated=${full.miniCreated} miniVisible=${full.miniVisible}"
        )

        val exiting = miniPlayerWindows(isWindowVisible = true, isMiniPlayerMode = true, isExiting = true)
        assertFalse(exiting.miniCreated, "a closing app must not create the mini window")
        assertFalse(exiting.miniVisible)
        println("[UI] Exiting miniCreated=${exiting.miniCreated} miniVisible=${exiting.miniVisible}")
    }
}
