package com.music.vivi.desktop.data.listentogether

import com.music.vivi.desktop.data.model.Song
import com.music.vivi.desktop.data.player.PlaybackAudioCache
import com.music.vivi.desktop.data.player.QueueManager
import com.music.vivi.desktop.data.youtube.YouTubeMusicRepository
import com.music.vivi.desktop.domain.player.AudioEngine
import com.music.vivi.desktop.domain.repository.SettingsRepository
import com.music.vivi.desktop.presentation.viewmodel.PlaybackStateHolder
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlin.math.abs

/**
 * Bridges the Listen Together client with the desktop player.
 *
 * As HOST it watches the local transport (track, play/pause, seeks, queue) and
 * broadcasts it. As GUEST it applies the host's actions and locks the local
 * transport, so the room stays the single source of truth. Only catalogue
 * tracks travel: a local file is private to the machine that owns it and could
 * not be played by anyone else.
 */
class ListenTogetherManager(
    private val playback: PlaybackStateHolder,
    private val queueManager: QueueManager,
    private val audioEngine: AudioEngine,
    private val settingsRepository: SettingsRepository,
    private val coroutineScope: CoroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
) {

    companion object {
        const val DEFAULT_SERVER_URL = "wss://devilmi-vivi-music-listen-together.hf.space"
        private const val HOST_TICK_MS = 700L
        private const val POSITION_DRIFT_MS = 2500L
    }

    val client = ListenTogetherClient {
        settingsRepository.settingsFlow.value.listenTogetherServerUrl
            .ifBlank { DEFAULT_SERVER_URL }
    }

    val connectionState = client.connectionState
    val roomState = client.roomState
    val role = client.role
    val userId = client.userId
    val pendingJoinRequests = client.pendingJoinRequests
    val busy = client.busy
    val latencyMs = client.latencyMs
    val serverOnline = client.serverOnline
    val events = client.events

    private val _lastError = MutableStateFlow<String?>(null)
    val lastError: StateFlow<String?> = _lastError.asStateFlow()

    private var hostJob: Job? = null
    private var eventJob: Job? = null
    private var approvalJob: Job? = null
    private var roleJob: Job? = null

    init {
        initialize()
    }

    fun initialize() {
        // Connecting is deferred to startParty/joinParty: an app that never opens
        // Listen Together must not hold a WebSocket to the relay.
        roleJob?.cancel()
        roleJob = coroutineScope.launch {
            client.role.collect { newRole ->
                val locked = newRole == LtRoomRole.GUEST
                playback.userControlLocked = locked
            }
        }

        eventJob?.cancel()
        eventJob = coroutineScope.launch {
            client.events.collect { event ->
                when (event) {
                    is LtEvent.RoomCreated -> {
                        _lastError.value = null
                        startHostObservation()
                    }

                    is LtEvent.JoinApproved -> {
                        _lastError.value = null
                        applyRoomState(event.state)
                    }

                    is LtEvent.JoinRejected -> {
                        _lastError.value = event.reason
                    }

                    is LtEvent.PlaybackSync -> handlePlaybackAction(event.action)

                    is LtEvent.SyncStateReceived -> applySyncState(event.state)

                    is LtEvent.Reconnected -> {
                        if (event.isHost) {
                            startHostObservation()
                            broadcastCurrentTrack()
                        } else {
                            applyRoomState(event.state)
                            if (settingsRepository.settingsFlow.value.listenTogetherSmartResync) {
                                delay(700)
                                if (client.isInRoom && !client.isHost) client.requestSync()
                            }
                        }
                    }

                    is LtEvent.Kicked -> {
                        _lastError.value = "Removed from the party: ${event.reason}"
                        stopHostObservation()
                    }

                    is LtEvent.Error -> {
                        _lastError.value = event.message
                    }

                    else -> Unit
                }
            }
        }

        approvalJob?.cancel()
        approvalJob = coroutineScope.launch {
            client.pendingJoinRequests.collect { requests ->
                if (!client.isHost || requests.isEmpty()) return@collect
                val settings = settingsRepository.settingsFlow.value
                val cap = settings.listenTogetherPartySize.coerceIn(2, 10)
                val currentUsers = client.roomState.value?.users?.count { it.isConnected } ?: 0
                for (request in requests) {
                    if (currentUsers < cap) {
                        client.approveJoin(request.userId)
                    } else {
                        client.rejectJoin(request.userId, "Room is full")
                    }
                }
            }
        }
    }

    fun startParty(nickname: String, partySize: Int) {
        _lastError.value = null
        coroutineScope.launch {
            settingsRepository.setListenTogetherUsername(nickname)
            settingsRepository.setListenTogetherPartySize(partySize)
        }
        client.connect()
        client.createRoom(nickname)
    }

    fun joinParty(code: String, nickname: String) {
        _lastError.value = null
        coroutineScope.launch { settingsRepository.setListenTogetherUsername(nickname) }
        client.connect()
        client.joinRoom(code, nickname)
    }

    fun leaveParty() {
        stopHostObservation()
        playback.userControlLocked = false
        client.leaveRoom()
    }

    fun setPartySize(size: Int) {
        coroutineScope.launch { settingsRepository.setListenTogetherPartySize(size) }
    }

    fun testServer(address: String) {
        coroutineScope.launch {
            settingsRepository.setListenTogetherServerUrl(address)
            client.testConnection()
        }
    }

    fun useBuiltInServer() {
        coroutineScope.launch {
            settingsRepository.setListenTogetherServerUrl(DEFAULT_SERVER_URL)
            client.testConnection()
        }
    }

    fun clearError() {
        _lastError.value = null
    }

    fun kick(userId: String) = client.kickUser(userId)
    fun makeHost(userId: String) = client.transferHost(userId)

    private fun startHostObservation() {
        if (hostJob?.isActive == true) return
        hostJob = coroutineScope.launch {
            var lastTrackId: String? = null
            var lastPlaying: Boolean? = null
            var lastQueueIds: List<String>? = null
            var lastPosition = 0L
            var lastPositionAt = 0L
            while (isActive) {
                if (client.isHost && client.isInRoom) {
                    val current = playback.currentSong.value
                    val info = current?.toLtTrackInfo()
                    val playing = playback.playerInfo.value.isPlaying
                    if (info == null) {
                        lastTrackId = null
                        lastPlaying = null
                    } else if (info.id != lastTrackId) {
                        lastTrackId = info.id
                        lastPlaying = playing
                        lastPosition = playback.currentPositionMs.value
                        lastPositionAt = System.currentTimeMillis()
                        client.sendPlaybackAction(
                            action = LtPlaybackActions.CHANGE_TRACK,
                            trackId = info.id,
                            trackInfo = info,
                            queue = queueManager.queue.value.mapNotNull { it.toLtTrackInfo() },
                            position = lastPosition
                        )
                        if (playing) {
                            client.sendPlaybackAction(
                                action = LtPlaybackActions.PLAY,
                                position = lastPosition
                            )
                        }
                    } else {
                        val now = System.currentTimeMillis()
                        val position = playback.currentPositionMs.value
                        if (lastPlaying != null && playing != lastPlaying) {
                            client.sendPlaybackAction(
                                action = if (playing) LtPlaybackActions.PLAY else LtPlaybackActions.PAUSE,
                                position = position
                            )
                        }
                        lastPlaying = playing
                        if (playing && lastPositionAt > 0L) {
                            val elapsed = now - lastPositionAt
                            val moved = position - lastPosition
                            if (abs(moved - elapsed) > POSITION_DRIFT_MS) {
                                client.sendPlaybackAction(
                                    action = LtPlaybackActions.SEEK,
                                    position = position
                                )
                            }
                        }
                        lastPosition = position
                        lastPositionAt = now
                    }

                    val queueIds = queueManager.queue.value.map { it.id }
                    if (lastQueueIds != null && queueIds != lastQueueIds) {
                        client.sendPlaybackAction(
                            action = LtPlaybackActions.SYNC_QUEUE,
                            queue = queueManager.queue.value.mapNotNull { it.toLtTrackInfo() }
                        )
                    }
                    lastQueueIds = queueIds
                }
                delay(HOST_TICK_MS)
            }
        }
    }

    private fun stopHostObservation() {
        hostJob?.cancel()
        hostJob = null
    }

    private fun broadcastCurrentTrack() {
        val current = playback.currentSong.value ?: return
        val info = current.toLtTrackInfo() ?: return
        client.sendPlaybackAction(
            action = LtPlaybackActions.CHANGE_TRACK,
            trackId = info.id,
            trackInfo = info,
            queue = queueManager.queue.value.mapNotNull { it.toLtTrackInfo() },
            position = playback.currentPositionMs.value
        )
        if (playback.playerInfo.value.isPlaying) {
            client.sendPlaybackAction(
                action = LtPlaybackActions.PLAY,
                position = playback.currentPositionMs.value
            )
        }
    }

    private fun handlePlaybackAction(action: LtPlaybackAction) {
        if (client.isHost && action.action !in setOf(
                LtPlaybackActions.QUEUE_ADD,
                LtPlaybackActions.QUEUE_REMOVE,
                LtPlaybackActions.QUEUE_CLEAR,
                LtPlaybackActions.SYNC_QUEUE
            )
        ) {
            return
        }
        when (action.action) {
            LtPlaybackActions.PLAY -> {
                val target = action.position ?: 0L
                playback.asRemote { playback.seekTo(target) }
                if (!playback.playerInfo.value.isPlaying) {
                    playback.asRemote { playback.togglePlayPause() }
                }
            }

            LtPlaybackActions.PAUSE -> {
                action.position?.let { playback.asRemote { playback.seekTo(it) } }
                if (playback.playerInfo.value.isPlaying) {
                    playback.asRemote { playback.togglePlayPause() }
                }
            }

            LtPlaybackActions.SEEK -> {
                action.position?.let { playback.asRemote { playback.seekTo(it) } }
            }

            LtPlaybackActions.SKIP_NEXT -> playback.asRemote { playback.playNext() }

            LtPlaybackActions.SKIP_PREV -> playback.asRemote { playback.playPrevious() }

            LtPlaybackActions.CHANGE_TRACK -> {
                val info = action.trackInfo ?: return
                val songs = action.queue.orEmpty().mapNotNull { it.toSong() }
                    .ifEmpty { listOfNotNull(info.toSong()) }
                if (songs.isEmpty()) return
                val index = songs.indexOfFirst { it.id == info.toSong()?.id }.coerceAtLeast(0)
                playback.asRemote { playback.playSongs(songs, index) }
                val position = action.position ?: 0L
                coroutineScope.launch {
                    delay(650)
                    if (position > 0L) playback.asRemote { playback.seekTo(position) }
                }
            }

            LtPlaybackActions.SYNC_QUEUE -> {
                val songs = action.queue.orEmpty().mapNotNull { it.toSong() }
                if (songs.isNotEmpty()) {
                    val currentId = playback.currentSong.value?.id
                    val index = songs.indexOfFirst { it.id == currentId }.coerceAtLeast(0)
                    queueManager.setQueue(songs, index)
                }
            }

            LtPlaybackActions.QUEUE_ADD -> {
                val songs = action.queue.orEmpty().mapNotNull { it.toSong() }
                if (songs.isNotEmpty()) queueManager.addToQueue(songs)
            }

            LtPlaybackActions.QUEUE_CLEAR -> queueManager.clearQueue()

            LtPlaybackActions.SET_VOLUME -> {
                if (settingsRepository.settingsFlow.value.listenTogetherSyncVolume) {
                    action.volume?.let { audioEngine.setVolume(it.coerceIn(0f, 1f)) }
                }
            }
        }
    }

    private fun applyRoomState(state: LtRoomState) {
        val queue = state.queue.mapNotNull { it.toSong() }
        val track = state.currentTrack
        val song = track?.toSong()
        val playList = queue.ifEmpty { listOfNotNull(song) }
        if (playList.isNotEmpty()) {
            val currentIndex = playList.indexOfFirst { it.id == song?.id }.coerceAtLeast(0)
            queueManager.setQueue(playList, currentIndex)
        }
        if (song != null) {
            val index = playList.indexOfFirst { it.id == song.id }.coerceAtLeast(0)
            playback.asRemote { playback.playSongs(playList, index) }
            val position = state.position
            val shouldPlay = state.isPlaying
            coroutineScope.launch {
                delay(650)
                if (position > 0L) playback.asRemote { playback.seekTo(position) }
                if (!shouldPlay && playback.playerInfo.value.isPlaying) {
                    playback.asRemote { playback.togglePlayPause() }
                }
            }
        }
        if (state.volume > 0f && settingsRepository.settingsFlow.value.listenTogetherSyncVolume) {
            audioEngine.setVolume(state.volume.coerceIn(0f, 1f))
        }
    }

    private fun applySyncState(state: LtSyncState) {
        val track = state.currentTrack ?: return
        val song = track.toSong() ?: return
        val songs = state.queue?.mapNotNull { it.toSong() }.orEmpty().ifEmpty { listOf(song) }
        val index = songs.indexOfFirst { it.id == song.id }.coerceAtLeast(0)
        queueManager.setQueue(songs, index)
        playback.asRemote { playback.playSongs(songs, index) }
        coroutineScope.launch {
            delay(650)
            if (state.position > 0L) playback.asRemote { playback.seekTo(state.position) }
            if (!state.isPlaying && playback.playerInfo.value.isPlaying) {
                playback.asRemote { playback.togglePlayPause() }
            }
        }
    }

    private fun LtTrackInfo.toSong(): Song? {
        val videoId = id.removePrefix("yt:").takeIf { it.isNotBlank() } ?: return null
        if (videoId.contains('/') || videoId.contains('\\')) return null
        return Song(
            id = if (id.startsWith("yt:")) id else "yt:$videoId",
            title = title,
            artist = artist,
            album = album.orEmpty(),
            path = YouTubeMusicRepository.VIDEO_SCHEME + videoId,
            contentUriString = YouTubeMusicRepository.VIDEO_SCHEME + videoId,
            albumArtUriString = thumbnail,
            duration = duration
        )
    }

    private fun Song.toLtTrackInfo(): LtTrackInfo? {
        val videoId = PlaybackAudioCache.videoIdOf(this) ?: return null
        return LtTrackInfo(
            id = videoId,
            title = title,
            artist = displayArtist,
            album = album.ifBlank { null },
            duration = duration,
            thumbnail = albumArtUriString
        )
    }
}
