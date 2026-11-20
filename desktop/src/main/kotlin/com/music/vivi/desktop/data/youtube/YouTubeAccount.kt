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
 * the guest identity in [GuestSession], and both files are sealed by
 * [SessionVault] (DPAPI on Windows, the system keystore elsewhere): the cookie
 * is a bearer credential and is never written in plaintext.
 *
 * Signing in only changes who the catalogue answers as: the search, the stream
 * resolution and the lyrics chain keep working the same way, and signing out
 * returns to the guest identity, which still plays.
 */
object YouTubeAccount {

    private const val STORE_NAME = "yt-cookie.txt"
    private const val CONTEXT_NAME = "yt-context.txt"

    private val store: File by lazy { File(DesktopPathProvider.configDir, STORE_NAME) }
    private val contextStore: File by lazy { File(DesktopPathProvider.configDir, CONTEXT_NAME) }

    @Volatile
    var cookie: String? = null
        private set

    val isSignedIn: Boolean get() = cookie != null

    /** Restores the session a previous run saved. Safe to call more than once. */
    fun restore() {
        if (cookie != null) return
        val saved = SessionVault.read(store)?.trim()
        if (saved.isNullOrBlank()) return
        cookie = saved
        YouTube.cookie = saved
        // A plaintext yt-context.txt from an older build is re-sealed by the read.
        val context = SessionVault.read(contextStore)?.lines().orEmpty()
        YouTube.dataSyncId = context.getOrNull(0)?.trim()?.ifBlank { null }
        YouTube.visitorData = context.getOrNull(1)?.trim()?.ifBlank { null }
        YouTube.useLoginForBrowse = true
        YtLog.log("account", "restored the saved session")
    }

    /**
     * Acts as the account the cookie belongs to, and remembers it for the next
     * start. The WebView also hands over the page's DATASYNC_ID and visitor id,
     * which put innertube requests in the account's context.
     */
    fun adopt(value: String, dataSyncId: String? = null, visitorData: String? = null) {
        val trimmed = value.trim()
        if (trimmed.isBlank()) return
        cookie = trimmed
        YouTube.cookie = trimmed
        YouTube.dataSyncId = dataSyncId?.trim()?.ifBlank { null }
        visitorData?.trim()?.takeIf { it.isNotBlank() }?.let { YouTube.visitorData = it }
        YouTube.useLoginForBrowse = true
        persistContext()
        val persisted = SessionVault.write(store, trimmed)
        if (persisted) {
            YtLog.log("account", "signed in; the session is sealed with ${SessionVault.protector}")
        } else {
            YtLog.log(
                "account",
                "signed in; no system keystore is available, so the session stays in memory for this run"
            )
        }
    }

    /** Back to the guest identity. */
    fun signOut() {
        cookie = null
        YouTube.cookie = null
        YouTube.dataSyncId = null
        YouTube.visitorData = null
        YouTube.useLoginForBrowse = false
        // Both files, their temporary copy and the keystore key: nothing is left
        // that could reopen the session.
        SessionVault.delete(store, contextStore)
        SessionVault.forget()
        YtLog.log("account", "signed out; the sealed session and its keystore key are gone")
    }

    private fun persistContext() {
        val lines = listOf(YouTube.dataSyncId.orEmpty(), YouTube.visitorData.orEmpty())
        if (lines.all { it.isBlank() }) {
            SessionVault.delete(contextStore)
        } else {
            SessionVault.write(contextStore, lines.joinToString("\n"))
        }
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
