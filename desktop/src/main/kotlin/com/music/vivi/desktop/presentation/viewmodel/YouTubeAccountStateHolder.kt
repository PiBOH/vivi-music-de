package com.music.vivi.desktop.presentation.viewmodel

import androidx.compose.runtime.Immutable
import com.music.vivi.desktop.data.youtube.BrowserCookies
import com.music.vivi.desktop.data.youtube.YouTubeAccount
import com.music.vivi.desktop.data.youtube.YouTubeWebLogin
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/** How the active session currently stands. */
enum class YouTubeSessionStatus {
    /** No account is saved: the app acts as the guest. */
    GUEST,

    /** A cookie is active and its identity is being read. */
    CHECKING,

    /** YouTube Music answered with the account, so the cookie still authenticates. */
    ACTIVE,

    /** The cookie no longer authenticates: the account has to be added again. */
    EXPIRED,

    /** The session could not be checked (network, service), the cookie may still be good. */
    ERROR
}

/** What the YouTube Music account card draws. */
@Immutable
data class YouTubeAccountUiState(
    val isSignedIn: Boolean = false,
    val name: String? = null,
    val email: String? = null,
    val handle: String? = null,
    val thumbnailUrl: String? = null,
    val accounts: List<YouTubeAccount.Summary> = emptyList(),
    val activeAccountId: String? = null,
    val status: YouTubeSessionStatus = YouTubeSessionStatus.GUEST,
    val isBusy: Boolean = false,
    val profiles: List<BrowserCookies.Profile> = emptyList(),
    val message: String? = null,
    /** Bumped when the active identity changes, so an open catalogue can re-ask. */
    val sessionRevision: Int = 0
)

/**
 * The account card's state: who the app acts as, every saved identity, how the
 * active session stands, and the browser profiles that could add another.
 *
 * Reading a browser's cookie store, switching the sealed files and asking
 * `innertube` who a session belongs to all do I/O, so they run on
 * [Dispatchers.IO] and never in composition. A switch changes the cookie
 * `innertube` signs with before the state is published, so the next request in
 * the running app already uses the new identity.
 */
class YouTubeAccountStateHolder(
    private val coroutineScope: CoroutineScope
) {

    private val _state = MutableStateFlow(YouTubeAccountUiState(isSignedIn = YouTubeAccount.isSignedIn))
    val state: StateFlow<YouTubeAccountUiState> = _state.asStateFlow()

    init {
        YouTubeAccount.restore()
        refresh()
    }

    /** Re-reads the session and, when signed in, verifies the account it belongs to. */
    fun refresh() {
        YouTubeAccount.restore()
        val summaries = YouTubeAccount.summaries()
        val active = summaries.firstOrNull { it.isActive }
        if (active == null) {
            _state.update {
                it.copy(
                    isSignedIn = false,
                    name = null,
                    email = null,
                    handle = null,
                    thumbnailUrl = null,
                    accounts = summaries,
                    activeAccountId = null,
                    status = YouTubeSessionStatus.GUEST,
                    isBusy = false
                )
            }
            return
        }
        _state.update {
            it.copy(
                isSignedIn = true,
                name = active.name,
                email = active.email,
                handle = active.handle,
                thumbnailUrl = active.thumbnailUrl,
                accounts = summaries,
                activeAccountId = active.id,
                status = YouTubeSessionStatus.CHECKING,
                isBusy = true,
                message = null
            )
        }
        coroutineScope.launch { verify(active.id) }
    }

    /**
     * Acts as another saved identity. The cookie is swapped and persisted before
     * the state is published, so this is the whole change: no restart, and the
     * search is re-run through [YouTubeAccountUiState.sessionRevision].
     */
    fun switchTo(id: String) {
        if (_state.value.isBusy || id == _state.value.activeAccountId) return
        _state.update { it.copy(isBusy = true, status = YouTubeSessionStatus.CHECKING, message = null) }
        coroutineScope.launch {
            val switched = withContext(Dispatchers.IO) { YouTubeAccount.switchTo(id) }
            if (!switched) {
                _state.update {
                    it.copy(
                        isBusy = false,
                        status = if (it.isSignedIn) YouTubeSessionStatus.ACTIVE else YouTubeSessionStatus.GUEST,
                        message = "That account is no longer saved."
                    )
                }
                return@launch
            }
            val active = YouTubeAccount.activeSummary()
            _state.update {
                it.copy(
                    isSignedIn = true,
                    name = active?.name,
                    email = active?.email,
                    handle = active?.handle,
                    thumbnailUrl = active?.thumbnailUrl,
                    accounts = YouTubeAccount.summaries(),
                    activeAccountId = active?.id,
                    status = YouTubeSessionStatus.CHECKING,
                    isBusy = true,
                    message = null,
                    sessionRevision = it.sessionRevision + 1
                )
            }
            verify(id)
        }
    }

    /** Removes one saved identity; removing the active one falls back to another. */
    fun removeAccount(id: String) {
        if (_state.value.isBusy) return
        val wasActive = id == _state.value.activeAccountId
        _state.update { it.copy(isBusy = true, message = null) }
        coroutineScope.launch {
            withContext(Dispatchers.IO) { YouTubeAccount.remove(id) }
            val summaries = YouTubeAccount.summaries()
            val active = summaries.firstOrNull { it.isActive }
            if (active == null) {
                _state.update {
                    it.copy(
                        isSignedIn = false,
                        name = null,
                        email = null,
                        handle = null,
                        thumbnailUrl = null,
                        accounts = summaries,
                        activeAccountId = null,
                        status = YouTubeSessionStatus.GUEST,
                        isBusy = false,
                        message = null,
                        sessionRevision = if (wasActive) it.sessionRevision + 1 else it.sessionRevision
                    )
                }
                return@launch
            }
            _state.update {
                it.copy(
                    isSignedIn = true,
                    name = active.name,
                    email = active.email,
                    handle = active.handle,
                    thumbnailUrl = active.thumbnailUrl,
                    accounts = summaries,
                    activeAccountId = active.id,
                    status = YouTubeSessionStatus.CHECKING,
                    isBusy = true,
                    message = null,
                    sessionRevision = if (wasActive) it.sessionRevision + 1 else it.sessionRevision
                )
            }
            verify(active.id)
        }
    }

    /**
     * Opens the in-app WebView so the user can sign in on Google's page and add
     * the identity. The browser cookie-jar path stays available when Google
     * refuses an embedded browser.
     */
    fun signInWithWebView() {
        if (_state.value.isBusy) return
        _state.update {
            it.copy(isBusy = true, message = "Complete the sign-in in the YouTube Music window.")
        }
        YouTubeWebLogin.open { result ->
            result.onSuccess { session ->
                YouTubeAccount.adopt(session.cookie, session.dataSyncId, session.visitorData)
                _state.update {
                    it.copy(
                        isSignedIn = true,
                        status = YouTubeSessionStatus.CHECKING,
                        profiles = emptyList(),
                        message = null,
                        sessionRevision = it.sessionRevision + 1
                    )
                }
                refresh()
            }.onFailure { error ->
                _state.update {
                    it.copy(
                        isBusy = false,
                        status = if (it.isSignedIn) YouTubeSessionStatus.ACTIVE else YouTubeSessionStatus.GUEST,
                        message = error.message ?: "The sign-in window could not be opened."
                    )
                }
            }
        }
    }

    /** Lists the browsers on this machine, so the card can offer them. */
    fun detectProfiles() {
        if (_state.value.isBusy) return
        _state.update { it.copy(isBusy = true, message = null, profiles = emptyList()) }
        coroutineScope.launch {
            val profiles = withContext(Dispatchers.IO) { BrowserCookies.profiles() }
            _state.update {
                it.copy(
                    isBusy = false,
                    profiles = profiles,
                    message = if (profiles.isEmpty()) "No browser with a YouTube cookie store was found" else null
                )
            }
        }
    }

    /** Adds [profile]'s session as an account, or reports why it cannot be used. */
    fun signIn(profile: BrowserCookies.Profile) {
        if (_state.value.isBusy) return
        _state.update { it.copy(isBusy = true, message = null) }
        coroutineScope.launch {
            val result = withContext(Dispatchers.IO) { BrowserCookies.read(profile) }
            when (result) {
                is BrowserCookies.Result.Session -> {
                    YouTubeAccount.adopt(result.cookie)
                    _state.update {
                        it.copy(
                            isSignedIn = true,
                            status = YouTubeSessionStatus.CHECKING,
                            profiles = emptyList(),
                            message = null,
                            sessionRevision = it.sessionRevision + 1
                        )
                    }
                    refresh()
                }
                BrowserCookies.Result.SignedOut -> _state.update {
                    it.copy(isBusy = false, message = "${profile.label} is not signed in to YouTube")
                }
                is BrowserCookies.Result.Unavailable -> _state.update {
                    it.copy(isBusy = false, message = "${profile.label}: ${result.reason}")
                }
            }
        }
    }

    /** Signing out removes the active identity and falls back to another saved one. */
    fun signOut() {
        val active = _state.value.activeAccountId ?: YouTubeAccount.activeAccountId() ?: return
        removeAccount(active)
    }

    private suspend fun verify(id: String) {
        val info = YouTubeAccount.info()
        // A switch can land while the request is in flight: its answer belongs to
        // the session that was active when it was sent, so it is dropped.
        if (YouTubeAccount.activeAccountId() != id) return
        info.fold(
            onSuccess = { account ->
                YouTubeAccount.updateIdentity(id, account)
                val active = YouTubeAccount.activeSummary()
                _state.update {
                    it.copy(
                        isSignedIn = true,
                        name = account.name,
                        email = account.email,
                        handle = account.channelHandle,
                        thumbnailUrl = account.thumbnailUrl,
                        accounts = YouTubeAccount.summaries(),
                        activeAccountId = active?.id,
                        status = YouTubeSessionStatus.ACTIVE,
                        isBusy = false,
                        message = null
                    )
                }
            },
            onFailure = { error ->
                _state.update {
                    it.copy(
                        isBusy = false,
                        status = if (isAuthFailure(error)) {
                            YouTubeSessionStatus.EXPIRED
                        } else {
                            YouTubeSessionStatus.ERROR
                        },
                        message = when {
                            isAuthFailure(error) ->
                                "YouTube Music no longer accepts this session. Add the account again."
                            else -> error.message ?: "The account did not answer."
                        }
                    )
                }
            }
        )
    }

    private fun isAuthFailure(error: Throwable): Boolean {
        val text = error.message.orEmpty().lowercase()
        return "not signed in" in text ||
            "401" in text ||
            "credential" in text ||
            "authenticate" in text ||
            "unauthorized" in text
    }
}
