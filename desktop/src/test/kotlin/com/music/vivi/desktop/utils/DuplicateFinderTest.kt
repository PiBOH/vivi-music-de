package com.music.vivi.desktop.utils

import com.music.vivi.desktop.data.model.Song
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals
import kotlin.test.assertTrue

class DuplicateFinderTest {

    private fun song(
        id: String,
        title: String = "Get Lucky",
        artist: String = "Daft Punk",
        duration: Long = 369_000L
    ) = Song(
        id = id,
        title = title,
        artist = artist,
        duration = duration,
        path = "/music/$id.mp3"
    )

    @Test
    fun anIgnoredGroupIsNotReported() {
        val first = song("a")
        val second = song("b")
        val unique = song("c", title = "Instant Crush")
        val songs = listOf(first, second, unique)

        val group = DuplicateFinder.findDuplicates(songs).single()
        val ignoredKey = DuplicateFinder.groupKey(group)

        assertEquals(2, group.songs.size)
        assertTrue(
            DuplicateFinder.findDuplicates(
                songs = songs,
                ignoredGroups = setOf(ignoredKey)
            ).isEmpty()
        )
    }

    @Test
    fun theKeyIsStableWhenTheSameSongsComeInAnotherOrder() {
        val first = song("a")
        val second = song("b")

        val forward = DuplicateFinder.findDuplicates(listOf(first, second)).single()
        val backward = DuplicateFinder.findDuplicates(listOf(second, first)).single()

        assertEquals(
            DuplicateFinder.groupKey(forward),
            DuplicateFinder.groupKey(backward)
        )
    }

    @Test
    fun aChangedGroupGetsANewKey() {
        val first = song("a")
        val second = song("b")
        val third = song("c")

        val pair = DuplicateFinder.findDuplicates(listOf(first, second)).single()
        val triple = DuplicateFinder.findDuplicates(listOf(first, second, third)).single()

        assertNotEquals(
            DuplicateFinder.groupKey(pair),
            DuplicateFinder.groupKey(triple)
        )
    }
}
