package com.music.vivi.desktop.data.store

import java.io.File
import java.io.IOException
import java.nio.file.Files
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class LocalLibraryStoreTest {

    private val directory: File = Files.createTempDirectory("vivi-store-test").toFile()
    private val file = File(directory, "device-sync.json")
    private val legacy = File(directory, "vivimusicde.db")
    private val originalWriter = StoreFileWriter.write

    @AfterTest
    fun tearDown() {
        StoreFileWriter.write = originalWriter
        directory.deleteRecursively()
    }

    private fun store() = LocalLibraryStore(file, legacy)

    private val first = LocalLibraryState(
        favorites = listOf(StoredFavorite("song-a", 1L)),
        playlists = listOf(com.music.vivi.desktop.data.model.Playlist(id = "p", name = "First"))
    )

    private val second = first.copy(
        favorites = first.favorites + StoredFavorite("song-b", 2L)
    )

    @Test
    fun interruptedWriteKeepsThePreviousLibrary() {
        store().save(first)

        // Half of the new JSON reaches the temporary file, then the "machine" dies.
        StoreFileWriter.write = { target, bytes ->
            target.outputStream().use { output -> output.write(bytes, 0, bytes.size / 2) }
            throw IOException("simulated interruption")
        }
        store().save(second)

        StoreFileWriter.write = originalWriter

        val onDisk = store().load()
        assertEquals(first, onDisk)
        assertFalse(file.readText().contains("song-b"))
        assertFalse(File(directory, "device-sync.json.tmp").exists())
    }

    @Test
    fun partialTemporaryFileIsNeverRead() {
        store().save(first)
        File(directory, "device-sync.json.tmp").writeText("{\"localLibrary\": {\"songs\":")

        assertEquals(first, store().load())
        assertTrue(file.readText().contains("song-a"))
    }

    @Test
    fun unreadableFileIsQuarantinedAndTheRotatedBackupIsRestored() {
        store().save(first)
        store().save(second)
        assertTrue(File(directory, "device-sync.json.bak").isFile)

        file.writeText("{ this is not json")

        assertEquals(first, store().load())
        assertTrue(
            directory.listFiles().orEmpty().any { it.name.startsWith("device-sync.json.corrupt-") }
        )
    }

    @Test
    fun reimportWithoutLegacyLibraryFailsWithoutTouchingTheStore() {
        store().save(first)

        val result = store().reimportFromLegacy()

        assertTrue(result.isFailure)
        assertEquals(first, store().load())
    }
}
