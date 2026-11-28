package com.music.vivi.desktop.presentation.screens.settings

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import com.music.vivi.desktop.data.model.Song
import com.music.vivi.desktop.domain.player.PlaybackState
import com.music.vivi.desktop.presentation.viewmodel.PlayerViewModel
import com.music.vivi.sync.PlaybackSnapshot
import com.music.vivi.sync.TrackRef
import kotlin.math.abs
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

/**
 * The desktop half of the 1.55 device-sync bridge. It lives beside the shell
 * (not inside the settings screen), so a paired phone keeps syncing while the
 * user is anywhere in the window.
 *
 * The wire payload is the 1.55 [PlaybackSnapshot]; only the mapping to the
 * current desktop player is new.
 */
@Composable
fun DeviceSyncRuntime(viewModel: PlayerViewModel) {
    val manager = viewModel.desktopSyncManager ?: return

    LaunchedEffect(manager) {
        launch {
            while (isActive) {
                val song = viewModel.currentSong.value
                if (song != null) {
                    manager.updatePlayback(viewModel.toSyncSnapshot(song))
                }
                delay(700L)
            }
        }
        launch {
            manager.incomingPlayback.collect { snapshot ->
                applyRemotePlayback(viewModel, manager, snapshot)
            }
        }
    }
}

private fun PlayerViewModel.toSyncSnapshot(song: Song): PlaybackSnapshot {
    val info = playerInfo.value
    val queue = queue.value
    val repeat = when (info.repeatMode) {
        1 -> "ALL"
        2 -> "ONE"
        else -> "OFF"
    }
    return PlaybackSnapshot(
        trackId = song.id,
        trackTitle = song.title,
        positionMs = currentPositionMs.value.coerceAtLeast(0L),
        userSeek = false,
        isResolving = playbackState.value == PlaybackState.BUFFERING,
        isPlaying = playbackState.value == PlaybackState.PLAYING,
        volume = volume.value,
        repeatMode = repeat,
        isShuffle = info.isShuffleEnabled,
        queue = queue.map { it.toTrackRef() },
        queueIndex = currentQueueIndex.value,
    )
}

private fun Song.toTrackRef(): TrackRef = TrackRef(
    id = id,
    title = title,
    artist = displayArtist,
    album = album,
    thumbnail = albumArtUriString,
    durationMs = duration,
)

private fun TrackRef.toSong(): Song = Song(
    id = id,
    title = title.ifBlank { "Unknown title" },
    artist = artist.orEmpty(),
    album = album.orEmpty(),
    duration = durationMs,
    albumArtUriString = thumbnail,
)

private suspend fun applyRemotePlayback(
    viewModel: PlayerViewModel,
    manager: com.music.vivi.desktop.data.sync.DesktopSyncManager,
    snapshot: PlaybackSnapshot,
) {
    val trackId = snapshot.trackId ?: return
    val localQueue = viewModel.songs.value
    val remoteQueue = snapshot.queue.map { ref -> localQueue.firstOrNull { it.id == ref.id } ?: ref.toSong() }
    val current = viewModel.currentSong.value
    val sameQueue = viewModel.queue.value.map { it.id } == remoteQueue.map { it.id }

    if (remoteQueue.isNotEmpty() && (!sameQueue || current?.id != trackId)) {
        val index = snapshot.queueIndex.coerceIn(0, remoteQueue.lastIndex)
        viewModel.playSongs(remoteQueue, index)
        manager.noteQueueApplied(snapshot)
        // playSongs resolves the stream asynchronously; give it a beat before
        // seeking so the position is not overwritten by the new track's start.
        delay(600L)
    }

    val target = manager.effectivePosition(snapshot)
    val localPosition = viewModel.currentPositionMs.value
    if (snapshot.userSeek || abs(localPosition - target) > 1_200L) {
        viewModel.seekTo(target)
    }

    val localPlaying = viewModel.playbackState.value == PlaybackState.PLAYING
    val buffering = viewModel.playbackState.value == PlaybackState.BUFFERING
    if (!buffering && snapshot.isPlaying != localPlaying) {
        viewModel.togglePlayPause()
    }

    if (viewModel.deviceSyncViviVolume.value) {
        snapshot.volume?.let { viewModel.setVolume(it.coerceIn(0f, 1f)) }
    }
}
