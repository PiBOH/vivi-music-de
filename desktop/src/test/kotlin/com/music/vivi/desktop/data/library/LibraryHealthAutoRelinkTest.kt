package com.music.vivi.desktop.data.library

import com.music.vivi.desktop.data.model.Song
import com.music.vivi.desktop.data.store.LocalLibraryState
import com.music.vivi.desktop.data.store.LocalLibraryStore
import java.io.File
import java.nio.file.Files
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class LibraryHealthAutoRelinkTest {

    private val directory: File = Files.createTempDirectory("vivi-auto-relink").toFile()
    private val musicDirectory = File(directory, "music").apply { mkdirs() }
    private val store = LocalLibraryStore(
        File(directory, "device-sync.json"),
        File(directory, "vivimusicde.db")
    )
    private val checker = LibraryHealthChecker(store)

    @AfterTest
    fun tearDown() {
        directory.deleteRecursively()
    }

    private fun song(id: String, title: String, artist: String, path: File) = Song(
        id = id,
        title = title,
        artist = artist,
        album = "Album",
        path = path.absolutePath,
        contentUriString = path.toURI().toString(),
        duration = 180_000L,
        mimeType = "audio/mpeg"
    )

    @Test
    fun aUniqueFileNameIsRelinkedAndUndoRestoresTheOldPath() {
        val oldFile = File(directory, "old/Artist - Title.mp3")
        val replacement = File(musicDirectory, "Artist - Title.mp3").apply { writeText("audio") }
        val stored = song("s1", "Title", "Artist", oldFile)
        store.save(LocalLibraryState(songs = listOf(stored)))

        val candidates = checker.findRelinkCandidates(listOf(musicDirectory.absolutePath))

        assertEquals(1, candidates.size)
        assertEquals(replacement.absolutePath, candidates.single().newFile.absolutePath)

        val result = checker.relinkSongs(candidates)

        assertEquals(1, result.changed)
        assertEquals(replacement.absolutePath, store.load().songs.single().path)
        assertEquals(replacement.toURI().toString(), store.load().songs.single().contentUriString)

        checker.undo(assertNotNull(result.undo))

        assertEquals(oldFile.absolutePath, store.load().songs.single().path)
        assertEquals(oldFile.toURI().toString(), store.load().songs.single().contentUriString)
    }

    @Test
    fun aDifferentNameIsMatchedByTitleAndArtist() {
        val oldFile = File(directory, "old/title.mp3")
        File(musicDirectory, "Artist - Title (Official Audio).mp3").apply { writeText("audio") }
        store.save(LocalLibraryState(songs = listOf(song("s1", "Title", "Artist", oldFile))))

        val candidates = checker.findRelinkCandidates(listOf(musicDirectory.absolutePath))

        assertEquals(1, candidates.size)
        assertEquals(
            "Artist - Title (Official Audio).mp3",
            candidates.single().newFile.name
        )
    }

    @Test
    fun aFileAnotherSongAlreadyUsesIsNotTaken() {
        val used = File(musicDirectory, "Artist - Title.mp3").apply { writeText("audio") }
        store.save(
            LocalLibraryState(
                songs = listOf(
                    song("valid", "Other", "Artist", used),
                    song("missing", "Title", "Artist", File(directory, "old/Artist - Title.mp3"))
                )
            )
        )

        assertTrue(checker.findRelinkCandidates(listOf(musicDirectory.absolutePath)).isEmpty())
    }

    @Test
    fun noCandidateIsOfferedWhenNothingMatches() {
        File(musicDirectory, "Unrelated.mp3").writeText("audio")
        store.save(
            LocalLibraryState(
                songs = listOf(
                    song("s1", "Title", "Artist", File(directory, "old/Title.mp3"))
                )
            )
        )

        assertTrue(checker.findRelinkCandidates(listOf(musicDirectory.absolutePath)).isEmpty())

        val result = checker.relinkSongs(emptyList())
        assertEquals(0, result.changed)
        assertNull(result.undo)
    }

    @Test
    fun twoFilesWithTheSameNameStayManual() {
        File(musicDirectory, "sub-a").mkdirs()
        File(musicDirectory, "sub-b").mkdirs()
        File(musicDirectory, "sub-a/Song.mp3").writeText("a")
        File(musicDirectory, "sub-b/Song.mp3").writeText("b")
        store.save(
            LocalLibraryState(
                songs = listOf(song("s1", "Song", "Artist", File(directory, "old/Song.mp3")))
            )
        )

        assertTrue(checker.findRelinkCandidates(listOf(musicDirectory.absolutePath)).isEmpty())
    }
}
