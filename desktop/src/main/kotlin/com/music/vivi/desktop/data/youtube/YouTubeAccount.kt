package com.music.vivi.desktop.data.youtube

import com.music.innertube.YouTube
import com.music.innertube.models.AccountInfo
import com.music.vivi.desktop.utils.DesktopPathProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

/**
 * The YouTube Music session the app acts as.
 *
 * `innertube` signs a request with the cookie it was given: it sends the jar as
 * `Cookie` and a `SAPISIDHASH` authorization, so a session is exactly one string
 * and no OAuth dance. The session survives a restart in `yt-cookie.txt`, next to
 * the guest identity in [GuestSession].
 *
 * Signing in only changes who the catalogue answers as: the search, the stream
 * resolution and the lyrics chain keep working the same way, and signing out
 * returns to the guest identity, which still plays.
 */
object YouTubeAccount {

    private const val STORE_NAME = "yt-cookie.txt"

    private val store: File by lazy { File(DesktopPathProvider.configDir, STORE_NAME) }

    @Volatile
    var cookie: String? = null
        private set

    val isSignedIn: Boolean get() = cookie != null

    /** Restores the session a previous run saved. Safe to call more than once. */
    fun restore() {
        if (cookie != null) return
        val saved = runCatching { store.takeIf { it.isFile }?.readText()?.trim() }.getOrNull()
        if (saved.isNullOrBlank()) return
        cookie = saved
        YouTube.cookie = saved
        YtLog.log("account", "restored the saved session")
    }

    /** Acts as the account the cookie belongs to, and remembers it for the next start. */
    fun adopt(value: String) {
        val trimmed = value.trim()
        if (trimmed.isBlank()) return
        cookie = trimmed
        YouTube.cookie = trimmed
        runCatching { store.writeText(trimmed) }
        YtLog.log("account", "signed in")
    }

    /** Back to the guest identity. */
    fun signOut() {
        cookie = null
        YouTube.cookie = null
        runCatching { store.delete() }
        YtLog.log("account", "signed out")
    }

    /** Who the session belongs to, as YouTube Music reports it. */
    suspend fun info(): Result<AccountInfo> = withContext(Dispatchers.IO) {
        if (!isSignedIn) {
            Result.failure(IllegalStateException("Not signed in"))
        } else {
            YouTube.accountInfo()
        }
    }
}
