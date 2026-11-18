package com.music.vivi.desktop.data.database

import com.music.vivi.desktop.data.model.Playlist
import com.music.vivi.desktop.data.model.SortOption
import com.music.vivi.desktop.data.store.LocalLibraryStore
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Playlists, on the JSON store.
 *
 * A playlist is one value in the store (the ordered song ids and its cover),
 * so an insert replaces the whole entry and a delete removes it, exactly like
 * the upsert and delete statements the previous build ran.
 */
class LocalPlaylistDao(
    private val store: LocalLibraryStore,
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO
) {

    suspend fun getAllPlaylists(sortOption: SortOption = SortOption.PlaylistNameAZ): List<Playlist> =
        withContext(ioDispatcher) {
            val playlists = store.load().playlists
            when (sortOption) {
                SortOption.PlaylistNameZA -> playlists.sortedByDescending { it.name.lowercase() }
                SortOption.PlaylistDateCreated -> playlists.sortedByDescending { it.createdAt }
                SortOption.PlaylistDateCreatedAsc -> playlists.sortedBy { it.createdAt }
                else -> playlists.sortedBy { it.name.lowercase() }
            }
        }

    suspend fun getPlaylistById(id: String): Playlist? = withContext(ioDispatcher) {
        store.load().playlists.firstOrNull { it.id == id }
    }

    suspend fun insertPlaylist(playlist: Playlist) = withContext(ioDispatcher) {
        store.update("playlists") { state ->
            val existing = state.playlists.firstOrNull { it.id == playlist.id }
            val stored = if (existing == null) playlist else playlist.copy(createdAt = existing.createdAt)
            state.copy(playlists = state.playlists.filterNot { it.id == playlist.id } + stored)
        }
    }

    suspend fun deletePlaylist(id: String) = withContext(ioDispatcher) {
        store.update("playlists") { state ->
            state.copy(playlists = state.playlists.filterNot { it.id == id })
        }
    }

    suspend fun clearPlaylistsBySource(source: String) = withContext(ioDispatcher) {
        store.update("playlists") { state ->
            state.copy(playlists = state.playlists.filterNot { it.source == source })
        }
    }
}
