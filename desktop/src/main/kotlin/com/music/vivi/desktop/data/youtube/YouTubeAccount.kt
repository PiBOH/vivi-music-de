package com.music.vivi.desktop.data.youtube

import com.music.innertube.YouTube
import com.music.innertube.models.AccountInfo
import com.music.vivi.desktop.utils.DesktopPathProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

/**
 * The YouTube Music sessions the app can act as.
 *
 * `innertube` signs a request with the cookie it was given: it sends the jar as
 * `Cookie` and a `SAPISIDHASH` authorization, so a session is exactly one string
 * and no OAuth dance. The active session survives a restart in `yt-cookie.txt`,
 * next to the guest identity in [GuestSession]; every saved identity, including
 * the active one, lives in the sealed list `yt-accounts.json` that
 * [YouTubeAccountBook] reads and writes. Both are sealed by [SessionVault]
 * (DPAPI on Windows, the system keystore elsewhere): a cookie is a bearer
 * credential and is never written in plaintext.
 *
 * Switching is a hot swap: [switchTo] replaces the cookie `innertube` signs with
 * and persists the active files, so the next search, stream resolution and
 * account request use the new identity without restarting the app. Signing out
 * removes the active identity and falls back to the most recently used one;
 * when none is left the app acts as the guest, which still plays.
 */
object YouTubeAccount {

    private const val STORE_NAME = "yt-cookie.txt"
    private const val CONTEXT_NAME = "yt-context.txt"
    private const val ACCOUNTS_NAME = "yt-accounts.json"

    private val store: File by lazy { File(DesktopPathProvider.configDir, STORE_NAME) }
    private val contextStore: File by lazy { File(DesktopPathProvider.configDir, CONTEXT_NAME) }
    private val book: YouTubeAccountBook by lazy {
        YouTubeAccountBook(File(DesktopPathProvider.configDir, ACCOUNTS_NAME))
    }

    /** One identity for the UI, without the cookie. */
    data class Summary(
        val id: String,
        val name: String,
        val email: String?,
        val handle: String?,
        val thumbnailUrl: String?,
        val isActive: Boolean
    )

    @Volatile
    var cookie: String? = null
        private set

    val isSignedIn: Boolean get() = cookie != null

    /** Restores the session a previous run saved. Safe to call more than once. */
    fun restore() {
        if (cookie != null) return
        book.load()
        val saved = SessionVault.read(store)?.trim()
        if (!saved.isNullOrBlank()) {
            // A plaintext yt-context.txt from an older build is re-sealed by the read.
            val context = SessionVault.read(contextStore)?.lines().orEmpty()
            val dataSyncId = context.getOrNull(0)?.trim()?.ifBlank { null }
            val visitorData = context.getOrNull(1)?.trim()?.ifBlank { null }
            // An installation from before the account list has a session but no
            // entry: the session becomes the first account, with its name filled
            // in by the first accountInfo answer.
            book.upsert(saved, dataSyncId, visitorData)
            activate(saved, dataSyncId, visitorData)
            YtLog.log("account", "restored the saved session")
            return
        }
        // The active file can be gone while the sealed list still holds the
        // session (a crash between the two writes): the list is the fallback.
        val fromList = book.record(book.activeId())
        if (fromList != null) {
            activate(fromList.cookie, fromList.dataSyncId, fromList.visitorData)
            writeActiveFiles(fromList.cookie, fromList.dataSyncId, fromList.visitorData)
            YtLog.log("account", "restored the active session from the account list")
        }
    }

    /**
     * Acts as the account the cookie belongs to, and remembers it for the next
     * start. The WebView also hands over the page's DATASYNC_ID and visitor id,
     * which put innertube requests in the account's context. Returns the entry id.
     */
    fun adopt(value: String, dataSyncId: String? = null, visitorData: String? = null): String {
        val trimmed = value.trim()
        if (trimmed.isBlank()) return ""
        val id = book.upsert(trimmed, dataSyncId, visitorData)
        activate(trimmed, dataSyncId, visitorData)
        val persisted = writeActiveFiles(trimmed, dataSyncId, visitorData)
        if (persisted) {
            YtLog.log("account", "signed in; the session is sealed with ${SessionVault.protector}")
        } else {
            YtLog.log(
                "account",
                "signed in; no system keystore is available, so the session stays in memory for this run"
            )
        }
        return id
    }

    /** Makes a saved identity the one the app acts as, without a restart. */
    fun switchTo(id: String): Boolean {
        book.load()
        val record = book.switchTo(id) ?: return false
        activate(record.cookie, record.dataSyncId, record.visitorData)
        writeActiveFiles(record.cookie, record.dataSyncId, record.visitorData)
        YtLog.log("account", "switched to another saved account")
        return true
    }

    /** Fills in who the active identity is, once YouTube Music has answered. */
    fun updateIdentity(id: String, info: AccountInfo): String? {
        book.load()
        return book.updateIdentity(id, info)
    }

    /**
     * Removes [id]. When it was the active account the most recently used one
     * takes over; when none is left the app returns to the guest identity.
     */
    fun remove(id: String): Boolean {
        book.load()
        val wasActive = book.activeId() == id
        val next = book.remove(id)
        if (!wasActive) {
            YtLog.log("account", "removed a saved account")
            return true
        }
        if (next != null) {
            activate(next.cookie, next.dataSyncId, next.visitorData)
            writeActiveFiles(next.cookie, next.dataSyncId, next.visitorData)
            YtLog.log("account", "removed the active account; switched to another saved one")
        } else {
            clearActive()
            SessionVault.delete(store, contextStore)
            SessionVault.forget()
            YtLog.log("account", "signed out; no account remains, the app acts as the guest")
        }
        return true
    }

    /** Back to the guest identity, or to the next saved account when there is one. */
    fun signOut(): Boolean {
        val active = activeAccountId()
        if (active == null) {
            clearActive()
            SessionVault.delete(store, contextStore)
            SessionVault.forget()
            YtLog.log("account", "signed out; the app acts as the guest")
            return true
        }
        return remove(active)
    }

    fun summaries(): List<Summary> {
        book.load()
        val active = book.activeId()
        return book.accounts().map { record ->
            Summary(
                id = record.id,
                name = record.name,
                email = record.email,
                handle = record.handle,
                thumbnailUrl = record.thumbnailUrl,
                isActive = record.id == active
            )
        }
    }

    fun activeSummary(): Summary? = summaries().firstOrNull { it.isActive }

    fun activeAccountId(): String? {
        book.load()
        return book.activeId()
    }

    /** Who the active session belongs to, as YouTube Music reports it. */
    suspend fun info(): Result<AccountInfo> = withContext(Dispatchers.IO) {
        if (!isSignedIn) {
            Result.failure(IllegalStateException("Not signed in"))
        } else {
            YouTube.accountInfo()
        }
    }

    private fun activate(cookie: String, dataSyncId: String?, visitorData: String?) {
        this.cookie = cookie
        YouTube.cookie = cookie
        YouTube.dataSyncId = dataSyncId?.trim()?.ifBlank { null }
        YouTube.visitorData = visitorData?.trim()?.ifBlank { null }
        YouTube.useLoginForBrowse = true
    }

    private fun clearActive() {
        cookie = null
        YouTube.cookie = null
        YouTube.dataSyncId = null
        YouTube.visitorData = null
        YouTube.useLoginForBrowse = false
    }

    private fun writeActiveFiles(cookie: String, dataSyncId: String?, visitorData: String?): Boolean {
        val persisted = SessionVault.write(store, cookie)
        val lines = listOf(dataSyncId.orEmpty(), visitorData.orEmpty())
        if (lines.all { it.isBlank() }) {
            SessionVault.delete(contextStore)
        } else {
            SessionVault.write(contextStore, lines.joinToString("\n"))
        }
        return persisted
    }
}
