package com.music.vivi.desktop.data.library

import com.music.vivi.desktop.data.model.Playlist
import com.music.vivi.desktop.data.model.Song
import com.music.vivi.desktop.data.store.LocalLibraryState
import com.music.vivi.desktop.data.store.LocalLibraryStore
import java.io.File
import java.nio.file.Files
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class LibraryHealthCheckerTest {

    private val directory: File = Files.createTempDirectory("vivi-health-test").toFile()
    private val storeFile = File(directory, "device-sync.json")
    private val legacy = File(directory, "vivimusicde.db")
    private val store = LocalLibraryStore(storeFile, legacy)
    private val checker = LibraryHealthChecker(store)

    private val presentFile = File(directory, "present.mp3").apply { writeText("audio") }
    private val missingFile = File(directory, "missing.mp3")
    private val secondMissingFile = File(directory, "missing-other.mp3")
    private val missingArt = File(directory, "missing-cover.png").toURI().toString()

    private val first = song(
        id = "first",
        title = "Get Lucky",
        path = missingFile.absolutePath,
        artwork = missingArt
    )
    private val second = song(
        id = "second",
        title = "Get Lucky",
        path = secondMissingFile.absolutePath,
        artwork = null
    )
    private val valid = song(
        id = "valid",
        title = "Instant Crush",
        path = presentFile.absolutePath,
        artwork = null
    )

    @AfterTest
    fun tearDown() {
        directory.deleteRecursively()
    }

    private fun song(id: String, title: String, path: String, artwork: String?) = Song(
        id = id,
        title = title,
        artist = "Daft Punk",
        album = "Random Access Memories",
        path = path,
        contentUriString = File(path).toURI().toString(),
        albumArtUriString = artwork,
        duration = 369_000L,
        mimeType = "audio/mpeg"
    )

    private fun seed(): LocalLibraryState {
        val state = LocalLibraryState(
            songs = listOf(first, second, valid),
            playlists = listOf(
                Playlist(
                    id = "trip",
                    name = "Road Trip",
                    songIds = listOf("first", "valid", "ghost-1", "ghost-2")
                ),
                Playlist(id = "clean", name = "Clean", songIds = listOf("valid"))
            )
        )
        store.save(state)
        return state
    }

    @Test
    fun scanReportsAllFourGroupsWithoutWriting() {
        val seeded = seed()
        val before = storeFile.readText()

        val report = checker.scan()

        assertEquals(3, report.songs)
        assertEquals(2, report.playlists)
        assertEquals(2, report.missingFiles.size)
        assertEquals(1, report.brokenArtwork.size)
        assertEquals(1, report.orphanPlaylistEntries.size)
        assertEquals(2, report.orphanEntryCount)
        assertEquals(1, report.duplicateGroups.size)
        assertEquals(2, report.duplicateGroups.single().songs.size)
        assertEquals(6, report.issueCount)
        assertEquals(before, storeFile.readText())
        assertEquals(seeded, store.load())
    }

    @Test
    fun orphanRemovalOnlyDropsMissingReferencesAndUndoRestoresTheOrder() {
        val seeded = seed()
        val report = checker.scan()

        val result = checker.removeOrphanEntries(report.orphanPlaylistEntries)

        assertEquals(2, result.changed)
        val afterRemoval = store.load()
        assertEquals(
            listOf("first", "valid"),
            afterRemoval.playlists.first { it.id == "trip" }.songIds
        )
        assertEquals(seeded.songs, afterRemoval.songs)
        assertEquals(
            listOf("valid"),
            afterRemoval.playlists.first { it.id == "clean" }.songIds
        )

        val undo = assertNotNull(result.undo)
        checker.undo(undo)

        assertEquals(
            listOf("first", "valid", "ghost-1", "ghost-2"),
            store.load().playlists.first { it.id == "trip" }.songIds
        )
    }

    @Test
    fun orphanRemovalKeepsAReferenceThatBecameValid() {
        seed()

        val result = checker.removeOrphanEntries(
            listOf(
                OrphanPlaylistIssue(
                    playlistId = "trip",
                    playlistName = "Road Trip",
                    missingSongIds = listOf("first", "valid", "ghost-1")
                )
            )
        )

        assertEquals(1, result.changed)
        // The valid ids and the orphan that was not selected keep their places;
        // only the requested ghost-1 reference is gone.
        assertEquals(
            listOf("first", "valid", "ghost-2"),
            store.load().playlists.first { it.id == "trip" }.songIds
        )
    }

    @Test
    fun relinkUpdatesOnlyTheChosenSongAndUndoRestoresTheOldPath() {
        seed()
        val replacement = File(directory, "replacement.mp3").apply { writeText("audio") }

        val result = checker.relinkSong("first", replacement)

        assertEquals(1, result.changed)
        val relinked = store.load().songs.first { it.id == "first" }
        assertEquals(replacement.absolutePath, relinked.path)
        assertEquals(replacement.toURI().toString(), relinked.contentUriString)
        assertEquals(
            presentFile.absolutePath,
            store.load().songs.first { it.id == "valid" }.path
        )
        assertEquals(
            secondMissingFile.absolutePath,
            store.load().songs.first { it.id == "second" }.path
        )

        checker.undo(assertNotNull(result.undo))

        val restored = store.load().songs.first { it.id == "first" }
        assertEquals(missingFile.absolutePath, restored.path)
        assertEquals(File(missingFile.absolutePath).toURI().toString(), restored.contentUriString)
    }

    @Test
    fun relinkRefusesAFileThatDoesNotExist() {
        val seeded = seed()

        val result = checker.relinkSong("first", File(directory, "not-there.mp3"))

        assertEquals(0, result.changed)
        assertEquals(seeded, store.load())
        assertTrue(result.undo == null)
    }
}
