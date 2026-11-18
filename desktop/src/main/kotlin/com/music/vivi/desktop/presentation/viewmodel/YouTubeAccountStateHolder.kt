package com.music.vivi.desktop.presentation.viewmodel

import com.music.vivi.desktop.data.youtube.BrowserCookies
import com.music.vivi.desktop.data.youtube.YouTubeAccount
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/** What the YMM account card draws. */
data class YouTubeAccountUiState(
    val isSignedIn: Boolean = false,
    val name: String? = null,
    val email: String? = null,
    val handle: String? = null,
    val isBusy: Boolean = false,
    val profiles: List<BrowserCookies.Profile> = emptyList(),
    val message: String? = null
)

/**
 * The account card's state: who the app acts as, and the browser profiles that
 * could sign it in.
 *
 * Reading a browser's cookie store and asking `innertube` who the session
 * belongs to both do I/O, so they run on [Dispatchers.IO] and never in
 * composition.
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

    /** Re-reads the session and, when signed in, the account it belongs to. */
    fun refresh() {
        YouTubeAccount.restore()
        if (!YouTubeAccount.isSignedIn) {
            _state.update { it.copy(isSignedIn = false, name = null, email = null, handle = null) }
            return
        }
        _state.update { it.copy(isSignedIn = true, isBusy = true, message = null) }
        coroutineScope.launch {
            val info = YouTubeAccount.info()
            _state.update {
                info.fold(
                    onSuccess = { account ->
                        it.copy(
                            isBusy = false,
                            name = account.name,
                            email = account.email,
                            handle = account.channelHandle
                        )
                    },
                    onFailure = { error ->
                        it.copy(isBusy = false, message = error.message ?: "The account did not answer")
                    }
                )
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

    /** Adopts [profile]'s session, or reports why it cannot be used. */
    fun signIn(profile: BrowserCookies.Profile) {
        if (_state.value.isBusy) return
        _state.update { it.copy(isBusy = true, message = null) }
        coroutineScope.launch {
            val result = withContext(Dispatchers.IO) { BrowserCookies.read(profile) }
            when (result) {
                is BrowserCookies.Result.Session -> {
                    YouTubeAccount.adopt(result.cookie)
                    _state.update {
                        it.copy(isSignedIn = true, isBusy = false, profiles = emptyList(), message = null)
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

    /** Back to the guest identity. */
    fun signOut() {
        YouTubeAccount.signOut()
        _state.value = YouTubeAccountUiState()
    }
}
