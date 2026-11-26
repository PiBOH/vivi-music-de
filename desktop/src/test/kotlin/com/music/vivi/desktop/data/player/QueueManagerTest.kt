package com.music.vivi.desktop.data.player

import com.music.vivi.desktop.data.model.Song
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class QueueManagerTest {

    private fun song(id: String) = Song(
        id = id,
        title = id,
        artist = "Tester",
        path = "/tmp/$id.mp3",
        duration = 60_000L
    )

    private val a = song("a")
    private val b = song("b")
    private val c = song("c")
    private val d = song("d")

    @Test
    fun addPlayNextMoveAndRemoveKeepTheCurrentTrackConsistent() {
        val queue = QueueManager()

        queue.setQueue(listOf(a, b), startIndex = 0)
        assertEquals("a", queue.currentSong.value?.id)

        queue.addToQueue(listOf(d))
        assertEquals(listOf("a", "b", "d"), queue.queue.value.map { it.id })

        queue.playNextInQueue(c)
        assertEquals(listOf("a", "c", "b", "d"), queue.queue.value.map { it.id })

        // The current song does not move when an item before it is reordered.
        queue.moveQueueItem(0, 2)
        assertEquals(listOf("c", "b", "a", "d"), queue.queue.value.map { it.id })
        assertEquals(2, queue.currentIndex.value)
        assertEquals("a", queue.currentSong.value?.id)

        // Removing the current track selects the item that took its place.
        queue.removeFromQueue(2)
        assertEquals(listOf("c", "b", "d"), queue.queue.value.map { it.id })
        assertEquals(2, queue.currentIndex.value)
        assertEquals("d", queue.currentSong.value?.id)
    }

    @Test
    fun removingTheOnlyTrackClearsTheQueue() {
        val queue = QueueManager()
        queue.setQueue(listOf(a))

        assertNull(queue.removeFromQueue(0))
        assertEquals(emptyList(), queue.queue.value)
        assertEquals(-1, queue.currentIndex.value)
        assertNull(queue.currentSong.value)
    }
}
