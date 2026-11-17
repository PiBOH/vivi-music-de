package com.music.vivi.desktop.data.youtube

import com.music.innertube.YouTube
import com.music.vivi.desktop.utils.DesktopPathProvider
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.io.File

/**
 * Keeps a guest (logged-out) YouTube identity.
 *
 * The desktop cannot mint PoTokens (that needs a WebView), so it keeps a fresh
 * `visitorData` instead: without the `X-Goog-Visitor-Id` header YouTube flags the
 * requests as bots and googlevideo answers 403 on the audio stream.
 */
object GuestSession {

    private val mutex = Mutex()
    private val store: File by lazy { File(DesktopPathProvider.configDir, "yt-visitor.txt") }

    @Volatile private var lastRefreshAttemptAt = 0L
    @Volatile private var lastRefreshSucceeded = false

    /** A failed fetch is not retried on every play: that would add a round-trip per track. */
    private const val REFRESH_COOLDOWN_MS = 30_000L

    suspend fun ensure() {
        mutex.withLock {
            if (!YouTube.visitorData.isNullOrBlank()) return@withLock
            val saved = runCatching { store.takeIf { it.isFile }?.readText()?.trim() }.getOrNull()
            if (!saved.isNullOrBlank()) {
                YouTube.visitorData = saved
                return@withLock
            }
            refresh()
        }
    }

    suspend fun rotate() {
        mutex.withLock { refresh() }
    }

    private suspend fun refresh() {
        val now = System.currentTimeMillis()
        if (now - lastRefreshAttemptAt < REFRESH_COOLDOWN_MS && !lastRefreshSucceeded) return
        lastRefreshAttemptAt = now
        YouTube.visitorData = null
        val result = YouTube.visitorData()
        lastRefreshSucceeded = result.isSuccess
        result.onSuccess { value ->
            YouTube.visitorData = value
            runCatching { store.writeText(value) }
        }
    }
}
