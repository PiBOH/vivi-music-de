package com.music.vivi.desktop.data.youtube

import com.music.vivi.desktop.data.store.LocalLibraryStore
import java.io.File
import java.nio.file.Files
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlinx.coroutines.runBlocking

class YouTubePlaylistOrderTest {

    private val directory: File = Files.createTempDirectory("vivi-playlist-order-test").toFile()

    @AfterTest
    fun tearDown() {
        directory.deleteRecursively()
    }

    private fun playlist(id: String) = RemotePlaylist(
        id = id,
        title = "Playlist $id",
        songCountText = null,
        thumbnail = null,
        isEditable = true
    )

    @Test
    fun savedOrderComesFirstAndNewPlaylistsKeepTheirLoadedOrder() {
        val loaded = listOf(playlist("a"), playlist("b"), playlist("c"), playlist("d"))

        val ordered = applyStoredPlaylistOrder(loaded, listOf("c", "a"))

        assertEquals(listOf("c", "a", "b", "d"), ordered.map { it.id })
    }

    @Test
    fun idsThatAreNoLongerInTheAccountAreIgnored() {
        val loaded = listOf(playlist("a"), playlist("b"), playlist("c"))

        val ordered = applyStoredPlaylistOrder(loaded, listOf("gone", "b"))

        assertEquals(listOf("b", "a", "c"), ordered.map { it.id })
    }

    @Test
    fun anEmptySavedOrderLeavesTheLoadedOrderAlone() {
        val loaded = listOf(playlist("a"), playlist("b"), playlist("c"))

        assertEquals(loaded, applyStoredPlaylistOrder(loaded, emptyList()))
    }

    @Test
    fun theCardOrderIsWrittenToTheStore() = runBlocking {
        val store = LocalLibraryStore(
            File(directory, "device-sync.json"),
            File(directory, "vivimusicde.db")
        )
        val repository = YouTubePlaylistsRepository(store = store)

        repository.saveOrder(listOf("b", "a"))

        assertEquals(listOf("b", "a"), store.load().youTubePlaylistOrder)
    }
}
