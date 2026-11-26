package com.music.vivi.desktop.compose

import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performScrollToNode
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.test.runComposeUiTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

import com.music.vivi.desktop.data.listentogether.LtConnectionState
import com.music.vivi.desktop.data.model.StatsTimeRange
import com.music.vivi.desktop.data.listentogether.LtRoomRole
import com.music.vivi.desktop.data.listentogether.LtRoomState
import com.music.vivi.desktop.data.listentogether.LtUserInfo
import com.music.vivi.desktop.presentation.screens.ListenTogetherScreen
import com.music.vivi.desktop.presentation.screens.RecentlyPlayedScreen
import com.music.vivi.desktop.ui.theme.PixelPlayerTheme

@OptIn(ExperimentalTestApi::class)
class DesktopWidgetPathsTest {

    @Test
    fun historyRangePlayAndShuffleReachTheQueue() {
        val harness = DesktopUiHarness()
        try {
            val songs = DesktopUiHarness.historySongs()
            runComposeUiTest {
                setContent {
                    PixelPlayerTheme {
                        RecentlyPlayedScreen(
                            recentlyPlayedSongs = songs,
                            currentSong = null,
                            isPlaying = false,
                            viewModel = harness.viewModel
                        )
                    }
                }

                val now = System.currentTimeMillis()
                val expectedCounts = StatsTimeRange.entries.associateWith { range ->
                    val (start, end) = range.resolveBounds(now)
                    songs.count { song ->
                        val timestamp = song.lastPlayedTimestamp
                        timestamp > 0 && timestamp <= end && (start == null || timestamp >= start)
                    }
                }
                println("[UI] History expected counts=$expectedCounts")
                StatsTimeRange.entries.forEach { range ->
                    val tag = "history-range-${range.name.lowercase()}"
                    val expected = expectedCounts.getValue(range)
                    onNodeWithTag(tag).performScrollTo().performClick()
                    waitForIdle()
                    onNodeWithTag(tag).assertIsSelected()
                    onNodeWithText("$expected tracks played").assertExists()
                    println("[UI] History range=${range.name} count=$expected")
                }

                onNodeWithTag("history-play-latest").performClick()
                waitUntil(timeoutMillis = 5_000) { harness.queueManager.queue.value.size == 24 }
                assertEquals("today-newest", harness.queueManager.currentSong.value?.id)
                println("[UI] Play Latest currentSong=${harness.queueManager.currentSong.value?.id} queue=24")

                harness.viewModel.stopPlayback()
                assertEquals(0, harness.queueManager.queue.value.size)

                val playCallsBeforeShuffle = harness.engine.playCalls
                onNodeWithTag("history-shuffle").performClick()
                waitUntil(timeoutMillis = 5_000) { harness.queueManager.queue.value.size == 24 }
                waitUntil(timeoutMillis = 5_000) { harness.engine.playCalls > playCallsBeforeShuffle }

                val shuffledIds = harness.queueManager.queue.value.map { it.id }.toSet()
                assertEquals(songs.map { it.id }.toSet(), shuffledIds)
                assertNotNull(harness.queueManager.currentSong.value)
                println(
                    "[UI] Shuffle queue=24 enginePlayCalls=${harness.engine.playCalls} " +
                        "currentSong=${harness.queueManager.currentSong.value?.id}"
                )
            }
        } finally {
            harness.close()
        }
    }

    @Test
    fun historyListScrollsToTheOldestTodaySong() {
        val harness = DesktopUiHarness()
        try {
            val songs = DesktopUiHarness.historySongs()
            runComposeUiTest {
                setContent {
                    PixelPlayerTheme {
                        RecentlyPlayedScreen(
                            recentlyPlayedSongs = songs,
                            currentSong = null,
                            isPlaying = false,
                            viewModel = harness.viewModel
                        )
                    }
                }

                onNodeWithTag("history-range-day").performScrollTo().performClick()
                waitForIdle()
                onNodeWithText("Scroll Target").assertDoesNotExist()

                onNodeWithTag("history-list").performScrollToNode(hasText("Scroll Target"))
                onNodeWithText("Scroll Target").assertExists()
                println("[UI] History scroll reached=Scroll Target")
            }
        } finally {
            harness.close()
        }
    }

    @Test
    fun listenTogetherJoinCodeReachesTheManagerAndLeaveEndsTheRoom() {
        val harness = DesktopUiHarness()
        try {
            runComposeUiTest {
                setContent {
                    PixelPlayerTheme {
                        ListenTogetherScreen(
                            viewModel = harness.viewModel,
                            manager = harness.listenTogetherManager
                        )
                    }
                }

                onNodeWithText("Join a party").assertExists()
                onNodeWithTag("lt-join-code").performTextInput("ABC123")
                onNodeWithText("Join party").performClick()

                // The click is the widget path: the manager records the code and starts a connection.
                waitUntil(timeoutMillis = 5_000) {
                    harness.listenTogetherManager.client.lastJoinCodeForTest == "ABC123"
                }
                assertEquals("ABC123", harness.listenTogetherManager.client.lastJoinCodeForTest)
                waitUntil(timeoutMillis = 5_000) {
                    harness.listenTogetherManager.connectionState.value != LtConnectionState.DISCONNECTED ||
                        harness.listenTogetherManager.busy.value ||
                        harness.listenTogetherManager.lastError.value != null
                }
                assertTrue(
                    harness.listenTogetherManager.connectionState.value != LtConnectionState.DISCONNECTED ||
                        harness.listenTogetherManager.busy.value ||
                        harness.listenTogetherManager.lastError.value != null
                )
                println(
                    "[UI] Join party code=${harness.listenTogetherManager.client.lastJoinCodeForTest} " +
                        "connection=${harness.listenTogetherManager.connectionState.value} " +
                        "busy=${harness.listenTogetherManager.busy.value} " +
                        "error=${harness.listenTogetherManager.lastError.value != null}"
                )

                // No relay is available in a unit test: settle the failed attempt, then
                // install the room state the screen shows after an accepted join.
                harness.listenTogetherManager.client.disconnect()
                harness.listenTogetherManager.client.installRoomStateForTest(
                    state = LtRoomState(
                        roomCode = "5BE0D4",
                        hostId = "host",
                        users = listOf(
                            LtUserInfo("me", "Tester"),
                            LtUserInfo("host", "Host", isHost = true)
                        )
                    ),
                    role = LtRoomRole.GUEST,
                    connection = LtConnectionState.CONNECTED
                )
                waitUntil(timeoutMillis = 5_000) {
                    onAllNodesWithText("Connected").fetchSemanticsNodes().isNotEmpty()
                }
                onNodeWithText("Leave party").assertExists()
                assertEquals(
                    "5BE0D4",
                    harness.listenTogetherManager.roomState.value?.roomCode
                )
                println(
                    "[UI] Room after join code=5BE0D4 connection=${harness.listenTogetherManager.connectionState.value} " +
                        "room=${harness.listenTogetherManager.roomState.value?.roomCode}"
                )

                onNodeWithText("Leave party").performScrollTo().performClick()
                waitUntil(timeoutMillis = 5_000) { harness.listenTogetherManager.roomState.value == null }
                assertEquals(null, harness.listenTogetherManager.roomState.value)
                assertEquals(LtRoomRole.NONE, harness.listenTogetherManager.role.value)
                waitUntil(timeoutMillis = 5_000) {
                    onAllNodesWithText("Join a party").fetchSemanticsNodes().isNotEmpty()
                }
                onNodeWithText("Connected").assertDoesNotExist()
                println(
                    "[UI] Leave room=${harness.listenTogetherManager.roomState.value} " +
                        "role=${harness.listenTogetherManager.role.value} lobby=Join a party"
                )
            }
        } finally {
            harness.close()
        }
    }
}
