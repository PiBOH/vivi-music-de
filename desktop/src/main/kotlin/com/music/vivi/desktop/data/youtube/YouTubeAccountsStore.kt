package com.music.vivi.desktop.data.youtube

import com.music.innertube.models.AccountInfo
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import java.io.File
import java.security.MessageDigest

/** One saved YouTube Music identity. The cookie never leaves this record. */
@Serializable
internal data class YouTubeAccountRecord(
    val id: String,
    val name: String = DEFAULT_NAME,
    val email: String? = null,
    val handle: String? = null,
    val thumbnailUrl: String? = null,
    val cookie: String,
    val dataSyncId: String? = null,
    val visitorData: String? = null,
    val addedAt: Long = System.currentTimeMillis(),
    val lastUsedAt: Long = System.currentTimeMillis()
) {
    companion object {
        const val DEFAULT_NAME = "YouTube Music account"
    }
}

@Serializable
internal data class YouTubeAccountsFile(
    val version: Int = 1,
    val activeId: String? = null,
    val accounts: List<YouTubeAccountRecord> = emptyList()
)

/**
 * The account list, sealed by [SessionVault] in `yt-accounts.json`.
 *
 * It holds every identity the app can act as, plus which one is active. The
 * active session is still mirrored into `yt-cookie.txt`/`yt-context.txt` so the
 * startup path and the 1.55.0 files stay the ones a session is read from; this
 * file is the list, the identity and the switch.
 *
 * The class is pure file logic with no Android or Compose dependency, which is
 * what lets `--youtube-accounts-check` exercise it in a temporary directory
 * instead of the account this machine is signed in with.
 */
internal class YouTubeAccountBook(private val file: File) {

    private val json = Json {
        ignoreUnknownKeys = true
        encodeDefaults = true
    }

    private var state = YouTubeAccountsFile()
    private var loaded = false

    @Synchronized
    fun load(): List<YouTubeAccountRecord> {
        if (loaded) return state.accounts
        loaded = true
        val text = SessionVault.read(file) ?: return emptyList()
        state = runCatching { json.decodeFromString(YouTubeAccountsFile.serializer(), text) }
            .getOrElse { YouTubeAccountsFile() }
            .let { file ->
                val accounts = file.accounts.filter { it.id.isNotBlank() && it.cookie.isNotBlank() }
                file.copy(
                    accounts = accounts,
                    activeId = file.activeId?.takeIf { id -> accounts.any { it.id == id } }
                )
            }
        return state.accounts
    }

    @Synchronized
    fun accounts(): List<YouTubeAccountRecord> = state.accounts

    @Synchronized
    fun activeId(): String? = state.activeId

    @Synchronized
    fun record(id: String?): YouTubeAccountRecord? =
        id?.let { wanted -> state.accounts.firstOrNull { it.id == wanted } }

    @Synchronized
    fun persist(): Boolean {
        state = state.copy(activeId = state.activeId?.takeIf { id -> state.accounts.any { it.id == id } })
        if (state.accounts.isEmpty()) {
            SessionVault.delete(file)
            return true
        }
        return SessionVault.write(file, json.encodeToString(YouTubeAccountsFile.serializer(), state))
    }

    /**
     * Stores [cookie] as the active account.
     *
     * A session is matched by its cookie hash, and once the identity is known by
     * its email too: re-adding the same account after the cookie rotated updates
     * the entry instead of growing a second one with the same name.
     */
    @Synchronized
    fun upsert(
        cookie: String,
        dataSyncId: String?,
        visitorData: String?,
        name: String? = null,
        email: String? = null,
        handle: String? = null,
        thumbnailUrl: String? = null
    ): String {
        val id = accountId(cookie)
        val mail = email?.takeIf { it.isNotBlank() }
        val existing = state.accounts.firstOrNull { it.id == id }
            ?: mail?.let { wanted -> state.accounts.firstOrNull { it.email?.equals(wanted, true) == true } }
        val now = System.currentTimeMillis()
        val record = if (existing != null) {
            existing.copy(
                name = name?.takeIf { it.isNotBlank() } ?: existing.name,
                email = mail ?: existing.email,
                handle = handle?.takeIf { it.isNotBlank() } ?: existing.handle,
                thumbnailUrl = thumbnailUrl?.takeIf { it.isNotBlank() } ?: existing.thumbnailUrl,
                cookie = cookie,
                dataSyncId = dataSyncId?.takeIf { it.isNotBlank() } ?: existing.dataSyncId,
                visitorData = visitorData?.takeIf { it.isNotBlank() } ?: existing.visitorData,
                lastUsedAt = now
            )
        } else {
            YouTubeAccountRecord(
                id = id,
                name = name?.takeIf { it.isNotBlank() } ?: YouTubeAccountRecord.DEFAULT_NAME,
                email = mail,
                handle = handle?.takeIf { it.isNotBlank() },
                thumbnailUrl = thumbnailUrl?.takeIf { it.isNotBlank() },
                cookie = cookie,
                dataSyncId = dataSyncId?.takeIf { it.isNotBlank() },
                visitorData = visitorData?.takeIf { it.isNotBlank() },
                addedAt = now,
                lastUsedAt = now
            )
        }
        val others = state.accounts.filterNot { it.id == record.id }
            .filterNot { other -> mail != null && other.email?.equals(mail, true) == true }
        state = state.copy(accounts = others + record, activeId = record.id)
        persist()
        return record.id
    }

    /** Makes [id] the active account; returns its record, or null when it is gone. */
    @Synchronized
    fun switchTo(id: String): YouTubeAccountRecord? {
        val record = record(id) ?: return null
        val now = System.currentTimeMillis()
        state = state.copy(
            activeId = id,
            accounts = state.accounts.map { if (it.id == id) it.copy(lastUsedAt = now) else it }
        )
        persist()
        return record.copy(lastUsedAt = now)
    }

    /** Fills in who [id] is once YouTube Music answers; merges a same-email duplicate. */
    @Synchronized
    fun updateIdentity(id: String, info: AccountInfo): String? {
        val current = state.accounts.firstOrNull { it.id == id } ?: return null
        val mail = info.email?.takeIf { it.isNotBlank() }
        val now = System.currentTimeMillis()
        val updated = current.copy(
            name = info.name.takeIf { it.isNotBlank() } ?: current.name,
            email = mail ?: current.email,
            handle = info.channelHandle?.takeIf { it.isNotBlank() } ?: current.handle,
            thumbnailUrl = info.thumbnailUrl?.takeIf { it.isNotBlank() } ?: current.thumbnailUrl,
            lastUsedAt = now
        )
        val others = state.accounts.filterNot { it.id == id }
            .filterNot { other -> mail != null && other.email?.equals(mail, true) == true }
        state = state.copy(accounts = others + updated)
        persist()
        return id
    }

    /**
     * Removes [id]; returns the account that should be active afterwards, or
     * null when the list is empty. A removed active account falls back to the
     * most recently used one.
     */
    @Synchronized
    fun remove(id: String): YouTubeAccountRecord? {
        val wasActive = state.activeId == id
        val remaining = state.accounts.filterNot { it.id == id }
        val next = when {
            !wasActive -> record(state.activeId)
            else -> remaining.maxByOrNull { it.lastUsedAt }
        }
        state = state.copy(accounts = remaining, activeId = next?.id)
        persist()
        return next
    }

    /** The id is derived from the cookie, so the same session keeps its name. */
    private fun accountId(cookie: String): String {
        val digest = MessageDigest.getInstance("SHA-256").digest(cookie.toByteArray(Charsets.UTF_8))
        val hex = digest.joinToString("") { "%02x".format(it) }
        return "yt-${hex.take(20)}"
    }
}
