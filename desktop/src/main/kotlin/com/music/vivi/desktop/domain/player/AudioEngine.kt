package com.music.vivi.desktop.domain.player

import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow

enum class PlaybackState {
    IDLE,
    BUFFERING,
    READY,
    PLAYING,
    PAUSED,
    ENDED,
    ERROR
}

interface AudioEngine {
    val playbackState: StateFlow<PlaybackState>
    val currentPositionMs: StateFlow<Long>
    val durationMs: StateFlow<Long>
    val volume: StateFlow<Float> // 0.0f to 1.0f
    val isMuted: StateFlow<Boolean>
    val playbackSpeed: StateFlow<Float> get() = kotlinx.coroutines.flow.MutableStateFlow(1.0f)
    val isCrossfading: StateFlow<Boolean> get() = kotlinx.coroutines.flow.MutableStateFlow(false)
    val errorEvents: SharedFlow<String>
    val completionEvents: SharedFlow<Unit>
    val currentTrackPath: String? get() = null

    /**
     * Opens a media target.
     *
     * [httpHeaders] carries the request headers a remote stream needs: an
     * innertube stream is served by googlevideo, which wants the User-Agent of
     * the client that resolved it and a YouTube Referer. Local files pass none.
     */
    fun play(mediaPathOrUri: String, httpHeaders: List<String> = emptyList())
    fun play()
    fun pause()
    fun stop()
    fun seekTo(positionMs: Long)
    fun setVolume(volume: Float)
    fun setMute(muted: Boolean)
    fun setPlaybackSpeed(speed: Float) {}
    fun setReplayGain(enabled: Boolean, mode: String = "track")
    fun setEqualizer(enabled: Boolean, bands: List<Float>, preamp: Float = 0f) {}
    val audioDeviceFallbackEvents: SharedFlow<String> get() = MutableSharedFlow()
    val audioDeviceListChangedEvents: SharedFlow<Unit> get() = MutableSharedFlow()
    fun getAudioDevices(): List<AudioDevice> = emptyList()
    fun setAudioDevice(deviceName: String) {}
    fun getCurrentAudioDevice(): String = "auto"
    fun crossfadeTo(
        mediaPathOrUri: String,
        durationMs: Long,
        targetSpeed: Float? = null,
        httpHeaders: List<String> = emptyList()
    ) {
        play(mediaPathOrUri, httpHeaders)
    }
    fun setAudioFadeOnPlayPause(enabled: Boolean) {}
    fun isClosed(): Boolean = false
    fun release()
}
