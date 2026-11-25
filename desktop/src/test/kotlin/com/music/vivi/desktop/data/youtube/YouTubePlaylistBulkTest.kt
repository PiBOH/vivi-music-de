package com.music.vivi.desktop.data.youtube

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlinx.coroutines.runBlocking

class YouTubePlaylistBulkTest {

    @Test
    fun anAlreadyPresentTrackIsNotAddedAgain() = runBlocking {
        var addCalls = 0

        val outcome = addSongIfAbsentWith(
            playlistId = "playlist",
            videoId = "video",
            readTracks = {
                Result.success(listOf(PlaylistTrack(videoId = "video", setVideoId = "set-1")))
            },
            add = { _, _ ->
                addCalls++
                Result.success(Unit)
            }
        )

        assertEquals(AddOutcome.ALREADY_PRESENT, outcome)
        assertEquals(0, addCalls)
    }

    @Test
    fun anEmptyPlaylistReceivesTheTrack() = runBlocking {
        var addedTo: Pair<String, String>? = null

        val outcome = addSongIfAbsentWith(
            playlistId = "empty",
            videoId = "video",
            readTracks = { Result.success(emptyList()) },
            add = { playlistId, videoId ->
                addedTo = playlistId to videoId
                Result.success(Unit)
            }
        )

        assertEquals(AddOutcome.ADDED, outcome)
        assertEquals("empty" to "video", addedTo)
    }

    @Test
    fun bulkAddOnlyRecordsThePlaylistsThatReceivedTheTrack() = runBlocking {
        val outcomes = mapOf(
            "empty" to AddOutcome.ADDED,
            "already" to AddOutcome.ALREADY_PRESENT,
            "broken" to AddOutcome.FAILED
        )

        val result = bulkAddSong(
            playlistIds = outcomes.keys.toList(),
            videoId = "video",
            add = { playlistId, _ -> outcomes.getValue(playlistId) }
        )

        assertEquals(listOf("empty"), result.addedIds)
        assertEquals(1, result.added)
        assertEquals(1, result.already)
        assertEquals(1, result.failed)

        val undoRemovals = mutableListOf<String>()
        val failed = removeSongFromPlaylists(result.addedIds, "video") { playlistId, _ ->
            undoRemovals += playlistId
            Result.success(Unit)
        }

        assertEquals(listOf("empty"), undoRemovals)
        assertTrue(failed.isEmpty())
    }

    @Test
    fun undoSendsOnlyTheFailedPlaylistsBackToTheToken() = runBlocking {
        val failed = removeSongFromPlaylists(
            playlistIds = listOf("first", "offline", "third"),
            videoId = "video"
        ) { playlistId, _ ->
            if (playlistId == "offline") {
                Result.failure(IllegalStateException("network"))
            } else {
                Result.success(Unit)
            }
        }

        assertEquals(listOf("offline"), failed)
    }

    @Test
    fun undoUsesTheLastRemovalTokenForADuplicateTrack() = runBlocking {
        var removedToken: String? = null

        val result = removeSongWith(
            playlistId = "playlist",
            videoId = "video",
            readTracks = {
                Result.success(
                    listOf(
                        PlaylistTrack("video", "old"),
                        PlaylistTrack("video", "new"),
                        PlaylistTrack("video", null)
                    )
                )
            },
            remove = { _, _, setVideoId ->
                removedToken = setVideoId
                Result.success(Unit)
            }
        )

        assertTrue(result.isSuccess)
        assertEquals("new", removedToken)
    }
}
