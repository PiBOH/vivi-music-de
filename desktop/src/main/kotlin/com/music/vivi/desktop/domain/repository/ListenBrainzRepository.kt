package com.music.vivi.desktop.domain.repository

import com.music.vivi.desktop.data.network.listenbrainz.model.ListenBrainzAccountState
import com.music.vivi.desktop.data.network.listenbrainz.model.ListenBrainzPendingListen
import kotlinx.coroutines.flow.StateFlow

interface ListenBrainzRepository {
    val accountState: StateFlow<ListenBrainzAccountState>

    fun hasToken(): Boolean
    suspend fun connect(token: String, customServerUrl: String? = null): Result<String>
    suspend fun disconnect()
    suspend fun submitPlayingNow(
        trackName: String,
        artistName: String,
        releaseName: String?,
        durationMs: Long?,
        recordingMbid: String?
    ): Result<Unit>
    suspend fun enqueueListen(listen: ListenBrainzPendingListen): Boolean
    fun launchFlush()
    suspend fun flushQueue(): Result<Int>
}
