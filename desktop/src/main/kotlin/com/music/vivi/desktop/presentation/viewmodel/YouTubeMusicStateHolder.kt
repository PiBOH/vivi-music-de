package com.music.vivi.desktop.presentation.viewmodel

import androidx.compose.runtime.Immutable
import com.music.vivi.desktop.data.model.Song
import com.music.vivi.desktop.data.youtube.YouTubeMusicRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * State of the YouTube Music page.
 *
 * [Immutable] because the whole object is replaced on every change: Compose then
 * compares one reference instead of walking the song list, and the result rows
 * skip recomposition unless the page really changed.
 */
@Immutable
data class YouTubeMusicUiState(
    val query: String = "",
    val songs: List<Song> = emptyList(),
    val isLoading: Boolean = false,
    val hasSearched: Boolean = false,
    val error: String? = null
)

/**
 * Drives the YouTube Music page: it owns the query and the search job, and it is
 * the only place that talks to [YouTubeMusicRepository] from the UI.
 */
class YouTubeMusicStateHolder(
    private val repository: YouTubeMusicRepository,
    private val coroutineScope: CoroutineScope
) {

    private val _state = MutableStateFlow(YouTubeMusicUiState())
    val state: StateFlow<YouTubeMusicUiState> = _state.asStateFlow()

    private var searchJob: Job? = null

    fun onQueryChange(query: String) {
        _state.update { it.copy(query = query) }
    }

    /** Runs the search for the current query. A new submit cancels the one in flight. */
    fun submit() {
        val query = _state.value.query.trim()
        if (query.isEmpty()) {
            clear()
            return
        }
        searchJob?.cancel()
        _state.update { it.copy(isLoading = true, error = null, hasSearched = true) }
        searchJob = coroutineScope.launch {
            repository.search(query)
                .onSuccess { songs ->
                    _state.update { it.copy(songs = songs, isLoading = false, error = null) }
                }
                .onFailure { error ->
                    _state.update {
                        it.copy(
                            songs = emptyList(),
                            isLoading = false,
                            error = error.message ?: "YouTube Music did not answer"
                        )
                    }
                }
        }
    }

    fun clear() {
        searchJob?.cancel()
        _state.value = YouTubeMusicUiState()
    }
}
