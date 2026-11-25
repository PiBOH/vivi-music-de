package com.music.vivi.desktop.data.youtube

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlinx.coroutines.runBlocking

class YouTubePlaylistCopyTest {

    @Test
    fun anEmptyTargetAccountGetsACopyWithItsTracks() = runBlocking {
        val created = mutableListOf<String>()
        val added = mutableListOf<Pair<String, String>>()

        val outcome = copyPlaylistSnapshots(
            playlists = listOf(AccountPlaylistSnapshot("Road Trip", listOf("v1", "v2"))),
            existingTitles = emptyList(),
            create = { title ->
                created += title
                Result.success("new-${created.size}")
            },
            add = { playlistId, videoId ->
                added += playlistId to videoId
                Result.success(Unit)
            }
        ).getOrThrow()

        assertEquals(1, outcome.created)
        assertEquals(2, outcome.restoredTracks)
        assertEquals(0, outcome.failedTracks)
        assertEquals(listOf("Road Trip"), created)
        assertEquals(listOf("new-1" to "v1", "new-1" to "v2"), added)
    }

    @Test
    fun anExistingPlaylistWithTheSameTracksGetsANewCopy() = runBlocking {
        val created = mutableListOf<String>()
        val added = mutableListOf<Pair<String, String>>()

        val outcome = copyPlaylistSnapshots(
            playlists = listOf(AccountPlaylistSnapshot("Road Trip", listOf("v1", "v2"))),
            existingTitles = listOf("Road Trip"),
            create = { title ->
                created += title
                Result.success("new")
            },
            add = { playlistId, videoId ->
                added += playlistId to videoId
                Result.success(Unit)
            }
        ).getOrThrow()

        assertEquals(listOf("Road Trip (imported)"), created)
        assertEquals(listOf("new" to "v1", "new" to "v2"), added)
        assertEquals(1, outcome.created)
        assertEquals(2, outcome.restoredTracks)
        assertEquals(0, outcome.failedTracks)
    }

    @Test
    fun anEmptySourcePlaylistStillCreatesAnEmptyCopy() = runBlocking {
        val created = mutableListOf<String>()
        var addCalls = 0

        val outcome = copyPlaylistSnapshots(
            playlists = listOf(AccountPlaylistSnapshot("Empty", emptyList())),
            existingTitles = emptyList(),
            create = { title ->
                created += title
                Result.success("new")
            },
            add = { _, _ ->
                addCalls++
                Result.success(Unit)
            }
        ).getOrThrow()

        assertEquals(listOf("Empty"), created)
        assertEquals(0, addCalls)
        assertEquals(1, outcome.created)
        assertEquals(0, outcome.restoredTracks)
        assertEquals(0, outcome.failedTracks)
    }

    @Test
    fun aRefusedTrackIsCountedWithoutStoppingTheCopy() = runBlocking {
        val added = mutableListOf<String>()

        val outcome = copyPlaylistSnapshots(
            playlists = listOf(AccountPlaylistSnapshot("Mix", listOf("refused", "accepted"))),
            existingTitles = emptyList(),
            create = { Result.success("new") },
            add = { _, videoId ->
                added += videoId
                if (videoId == "refused") {
                    Result.failure(IllegalStateException("refused"))
                } else {
                    Result.success(Unit)
                }
            }
        ).getOrThrow()

        assertEquals(listOf("refused", "accepted"), added)
        assertEquals(1, outcome.created)
        assertEquals(1, outcome.restoredTracks)
        assertEquals(1, outcome.failedTracks)
    }

    @Test
    fun anEmptyPackIsRefused() = runBlocking {
        val result = copyPlaylistSnapshots(
            playlists = emptyList(),
            existingTitles = emptyList(),
            create = { Result.success("unused") },
            add = { _, _ -> Result.success(Unit) }
        )

        assertTrue(result.isFailure)
    }
}
