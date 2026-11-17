package com.music.vivi.desktop.presentation.viewmodel

import com.music.vivi.desktop.data.network.lyrics.LyricsSearchResultItem

sealed interface LyricsSearchUiState {
    data object Idle : LyricsSearchUiState
    data object Loading : LyricsSearchUiState
    data class PickResult(val query: String, val results: List<LyricsSearchResultItem>) : LyricsSearchUiState
    data class NotFound(val query: String) : LyricsSearchUiState
    data class Error(val message: String) : LyricsSearchUiState
}
