package com.music.vivi.desktop.presentation.screens.settings

import com.music.vivi.desktop.data.library.LibraryHealthReport
import com.music.vivi.desktop.data.library.OrphanPlaylistIssue
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class LibraryHealthCardSummaryTest {

    private fun report(vararg playlists: Pair<String, Int>): LibraryHealthReport =
        LibraryHealthReport(
            songs = 20,
            playlists = playlists.size,
            missingFiles = emptyList(),
            brokenArtwork = emptyList(),
            orphanPlaylistEntries = playlists.mapIndexed { index, (name, entries) ->
                OrphanPlaylistIssue(
                    playlistId = "playlist-$index",
                    playlistName = name,
                    missingSongIds = List(entries) { "missing-$index-$it" }
                )
            },
            duplicateGroups = emptyList()
        )

    @Test
    fun moreThanThreePlaylistsCountsTheHiddenPlaylists() {
        val report = report(
            "Road Trip" to 3,
            "Favorites" to 2,
            "Workout" to 1,
            "Evening" to 2,
            "Archive" to 2
        )
        val previews = report.orphanPlaylistEntries.take(3)
            .map { "${it.playlistName} (${it.missingSongIds.size})" }

        assertEquals(
            "Playlist entries without a song: 10 in 5 playlists",
            orphanEntriesHeadline(report)
        )
        assertEquals(listOf("Road Trip (3)", "Favorites (2)", "Workout (1)"), previews)
        assertEquals(
            "... and 2 more playlists",
            healthMoreText(
                total = report.orphanPlaylistEntries.size,
                shown = previews.size,
                singular = "playlist",
                plural = "playlists"
            )
        )
    }

    @Test
    fun oneHiddenPlaylistUsesTheSingular() {
        val report = report(
            "A" to 1,
            "B" to 1,
            "C" to 1,
            "D" to 1
        )

        assertEquals(
            "Playlist entries without a song: 4 in 4 playlists",
            orphanEntriesHeadline(report)
        )
        assertEquals(
            "... and 1 more playlist",
            healthMoreText(4, 3, "playlist", "playlists")
        )
    }

    @Test
    fun threePlaylistsHaveNoHiddenLine() {
        val report = report(
            "A" to 1,
            "B" to 1,
            "C" to 1
        )
        val previews = report.orphanPlaylistEntries.take(3)

        assertEquals(
            "Playlist entries without a song: 3 in 3 playlists",
            orphanEntriesHeadline(report)
        )
        assertNull(
            healthMoreText(
                total = report.orphanPlaylistEntries.size,
                shown = previews.size,
                singular = "playlist",
                plural = "playlists"
            )
        )
    }

    @Test
    fun noOrphansHaveNoHiddenLine() {
        val report = report()

        assertEquals(
            "Playlist entries without a song: 0 in 0 playlists",
            orphanEntriesHeadline(report)
        )
        assertNull(healthMoreText(0, 0, "playlist", "playlists"))
    }

    @Test
    fun otherGroupsStillCountTheirOwnItems() {
        assertEquals("... and 7 more", healthMoreText(10, 3))
        assertNull(healthMoreText(3, 3))
    }
}
