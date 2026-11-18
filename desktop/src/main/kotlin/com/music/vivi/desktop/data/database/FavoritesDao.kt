package com.music.vivi.desktop.data.database

import com.music.vivi.desktop.data.store.LocalLibraryStore
import com.music.vivi.desktop.data.store.StoredFavorite
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Liked songs, on the JSON store.
 *
 * A like is an entry in `favorites` (with the moment it was liked) plus the
 * `isFavorite` flag on the song, exactly the two rows the previous build wrote
 * in one transaction.
 */
class FavoritesDao(
    private val store: LocalLibraryStore,
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO
) {

    suspend fun isFavorite(songId: String): Boolean = withContext(ioDispatcher) {
        store.load().favorites.any { it.songId == songId }
    }

    suspend fun getFavoriteSongIds(): List<String> = withContext(ioDispatcher) {
        store.load().favorites
            .sortedByDescending { it.dateAdded }
            .map { it.songId }
    }

    suspend fun addFavorite(songId: String) = withContext(ioDispatcher) {
        val now = System.currentTimeMillis()
        store.update("favorites", "songs") { state ->
            state.copy(
                favorites = state.favorites.filterNot { it.songId == songId } +
                    StoredFavorite(songId = songId, dateAdded = now),
                songs = state.songs.map { song ->
                    if (song.id == songId) song.copy(isFavorite = true) else song
                }
            )
        }
        Unit
    }

    suspend fun removeFavorite(songId: String) = withContext(ioDispatcher) {
        store.update("favorites", "songs") { state ->
            state.copy(
                favorites = state.favorites.filterNot { it.songId == songId },
                songs = state.songs.map { song ->
                    if (song.id == songId) song.copy(isFavorite = false) else song
                }
            )
        }
        Unit
    }

    suspend fun toggleFavorite(songId: String): Boolean = withContext(ioDispatcher) {
        val currentlyFavorite = store.load().favorites.any { it.songId == songId }
        if (currentlyFavorite) removeFavorite(songId) else addFavorite(songId)
        !currentlyFavorite
    }
}
