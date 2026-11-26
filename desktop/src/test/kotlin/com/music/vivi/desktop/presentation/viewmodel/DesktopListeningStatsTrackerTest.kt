package com.music.vivi.desktop.presentation.viewmodel

import com.music.vivi.desktop.data.database.FavoritesDao
import com.music.vivi.desktop.data.database.LocalPlaylistDao
import com.music.vivi.desktop.data.database.MusicDao
import com.music.vivi.desktop.data.model.Song
import com.music.vivi.desktop.data.repository.MusicRepositoryImpl
import com.music.vivi.desktop.data.store.LocalLibraryStore
import java.io.File
import java.nio.file.Files
import java.util.concurrent.atomic.AtomicLong
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking

/**
 * The history screen reads the same store the playback tracker writes. These
 * tests drive the tracker with a fake clock, not the DAO directly, so a play is
 * recorded by the real playback path: start a song, listen past the threshold,
 * stop the transport.
 */
class DesktopListeningStatsTrackerTest {

    private val directory: File = Files.createTempDirectory("vivi-history-test").toFile()
    private val store = LocalLibraryStore(
        File(directory, "device-sync.json"),
        File(directory, "vivimusicde.db")
    )
    private val repository = MusicRepositoryImpl(
        store = store,
        musicDao = MusicDao(store),
        favoritesDao = FavoritesDao(store),
        playlistDao = LocalPlaylistDao(store)
    )
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    private val song = Song(
        id = "local:history-check",
        title = "History Check",
        artist = "Tester",
        album = "Check Album",
        path = File(directory, "history-check.mp3").absolutePath,
        duration = 180_000L
    )

    @AfterTest
    fun tearDown() {
        directory.deleteRecursively()
    }

    private suspend fun awaitHistory(songId: String): Boolean {
        repeat(40) {
            val recent = repository.getRecentlyPlayedSongsFlow(10).first()
            if (recent.any { it.id == songId && it.lastPlayedTimestamp > 0L }) return true
            delay(50)
        }
        return false
    }

    @Test
    fun pausingAfterTheThresholdRecordsThePlayWithoutChangingSong() = runBlocking {
        val clock = AtomicLong(1_000L)
        val tracker = DesktopListeningStatsTracker(
            musicRepository = repository,
            coroutineScope = scope,
            timeProvider = { clock.get() }
        )

        tracker.onSongChanged(song, isPlaying = false)
        tracker.onPlaybackStateChanged(true)
        clock.addAndGet(6_000L)
        tracker.onPlaybackStateChanged(false)

        // No finalizeCurrentSession, no next song: the pause itself must count as
        // a played track, which is the case that left the history empty.
        assertTrue(awaitHistory(song.id), "history was not written when the user paused after 6 s")
        val recent = repository.getRecentlyPlayedSongsFlow(10).first()
        assertEquals(1, recent.first { it.id == song.id }.playCount)
    }

    @Test
    fun aShortPauseBelowTheThresholdIsNotRecorded() = runBlocking {
        val clock = AtomicLong(1_000L)
        val tracker = DesktopListeningStatsTracker(
            musicRepository = repository,
            coroutineScope = scope,
            timeProvider = { clock.get() }
        )

        tracker.onSongChanged(song, isPlaying = false)
        tracker.onPlaybackStateChanged(true)
        clock.addAndGet(2_000L)
        tracker.onPlaybackStateChanged(false)
        tracker.finalizeCurrentSession(forceSynchronous = true)

        val recent = repository.getRecentlyPlayedSongsFlow(10).first()
        assertTrue(recent.none { it.id == song.id }, "a 2 s play must not enter the history")
    }

    @Test
    fun pausingAndResumingRecordsThePlayOnlyOnce() = runBlocking {
        val clock = AtomicLong(1_000L)
        val tracker = DesktopListeningStatsTracker(
            musicRepository = repository,
            coroutineScope = scope,
            timeProvider = { clock.get() }
        )

        tracker.onSongChanged(song, isPlaying = false)
        tracker.onPlaybackStateChanged(true)
        clock.addAndGet(6_000L)
        tracker.onPlaybackStateChanged(false)
        assertTrue(awaitHistory(song.id))

        // Resume the same session: finalizing it later must not add a second
        // history entry for the same play.
        tracker.onPlaybackStateChanged(true)
        clock.addAndGet(6_000L)
        tracker.finalizeCurrentSession(forceSynchronous = true)

        val events = repository.getPlaybackEvents(10).filter { it.songId == song.id }
        assertEquals(1, events.size, "pause + resume must not duplicate the history entry")
    }
}
