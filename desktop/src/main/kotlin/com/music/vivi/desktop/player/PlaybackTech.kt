package com.music.vivi.desktop.player

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import java.net.URI
import java.net.URLDecoder
import java.nio.charset.StandardCharsets

/**
 * The real technical facts behind the current playback, for the "Stats for
 * nerds" overlay. The stream fields come from the resolved googlevideo URL
 * (its `itag` and `mime`), the output fields from the AudioFormat the player
 * actually opened, and the buffer from the line the sound card granted.
 *
 * Nothing here is estimated: a field that has not been reported yet is null
 * and the overlay prints "n/a".
 */
object PlaybackTech {

    data class Snapshot(
        val codec: String? = null,
        val bitrateKbps: Int? = null,
        val sampleRateHz: Int? = null,
        val bitDepth: Int? = null,
        val channels: Int? = null,
        val deviceBufferMs: Int? = null,
        val pcmQueueMs: Int? = null,
    )

    private val _state = MutableStateFlow(Snapshot())
    val state: StateFlow<Snapshot> = _state.asStateFlow()

    /** Reads the chosen AAC itag from the URL the resolver returned. */
    fun stream(url: String?) {
        if (url.isNullOrBlank()) return
        val query = runCatching { URI(url).rawQuery }.getOrNull() ?: return
        val params = query.split('&').mapNotNull { part ->
            val i = part.indexOf('=')
            if (i <= 0) {
                null
            } else {
                part.substring(0, i) to URLDecoder.decode(part.substring(i + 1), StandardCharsets.UTF_8)
            }
        }.toMap()
        val itag = params["itag"]?.toIntOrNull()
        val mime = params["mime"]
        val codec = when (itag) {
            140, 141 -> "AAC-LC (mp4a.40.2)"
            139 -> "HE-AAC (mp4a.40.5)"
            251 -> "Opus"
            else -> mime ?: "unknown"
        }
        val bitrate = when (itag) {
            140 -> 128
            141 -> 256
            139 -> 48
            251 -> 160
            else -> null
        }
        _state.update { it.copy(codec = codec, bitrateKbps = bitrate) }
    }

    /** Reports the format and device ring the decoder actually opened. */
    fun output(
        sampleRateHz: Int,
        bitDepth: Int,
        channels: Int,
        deviceBufferMs: Int,
        pcmQueueMs: Int,
    ) {
        _state.update {
            it.copy(
                sampleRateHz = sampleRateHz,
                bitDepth = bitDepth,
                channels = channels,
                deviceBufferMs = deviceBufferMs,
                pcmQueueMs = pcmQueueMs,
            )
        }
    }

    fun reset() {
        _state.value = Snapshot()
    }
}
