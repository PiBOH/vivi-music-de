package com.music.vivi.desktop.data.database

import com.music.vivi.desktop.data.model.Song
import com.music.vivi.desktop.data.store.LocalLibraryState
import com.music.vivi.desktop.data.store.LocalLibraryStore
import java.io.File
import java.nio.file.Files
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlinx.coroutines.runBlocking

class MusicDaoTest {

    private val directory: File = Files.createTempDirectory("vivi-dao-test").toFile()
    private val store = LocalLibraryStore(
        File(directory, "device-sync.json"),
        File(directory, "vivimusicde.db")
    )
    private val dao = MusicDao(store)

    private val scannedRoot = File(directory, "scanned").apply { mkdirs() }
    private val failedRoot = File(directory, "failed").apply { mkdirs() }
    private val presentFile = File(scannedRoot, "present.mp3").apply { writeText("audio") }
    private val missingFile = File(scannedRoot, "missing.mp3")
    private val failedFile = File(failedRoot, "unread.mp3")

    @AfterTest
    fun tearDown() {
        directory.deleteRecursively()
    }

    private fun song(id: String, file: File) = Song(
        id = id,
        title = id,
        artist = "Artist",
        path = file.absolutePath,
        contentUriString = file.toURI().toString()
    )

    private fun seed() {
        store.save(
            LocalLibraryState(
                songs = listOf(
                    song("present", presentFile),
                    song("missing", missingFile),
                    song("failed-root", failedFile)
                )
            )
        )
    }

    @Test
    fun pruningOnlyTouchesTheRootsInTheScanScope() = runBlocking {
        seed()

        dao.deleteSongsNotInPaths(
            existingPaths = setOf(presentFile.absolutePath),
            scannedDirectories = listOf(scannedRoot.absolutePath)
        )

        assertEquals(
            listOf("present", "failed-root"),
            store.load().songs.map { it.id }
        )
    }

    @Test
    fun noScannedRootLeavesEverySong() = runBlocking {
        seed()

        dao.deleteSongsNotInPaths(
            existingPaths = emptySet(),
            scannedDirectories = emptyList()
        )

        assertEquals(
            listOf("present", "missing", "failed-root"),
            store.load().songs.map { it.id }
        )
    }
}
