package com.music.vivi.desktop.data.database

import com.music.vivi.desktop.data.network.listenbrainz.model.ListenBrainzPendingListen
import com.music.vivi.desktop.data.store.LocalLibraryStore
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.withContext

/**
 * The ListenBrainz queue, on the JSON store.
 *
 * A queued listen is an entry with its own id, kept in the order it was
 * recorded; the ids come from the store's counter so a listen keeps the same
 * identity across a restart, like the auto-increment key did.
 */
class ListenBrainzDao(
    private val store: LocalLibraryStore,
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO
) {

    suspend fun insert(listen: ListenBrainzPendingListen): Boolean = withContext(ioDispatcher) {
        store.update("listenbrainz_pending_listens") { state ->
            state.copy(
                pendingListens = state.pendingListens + listen.copy(id = state.nextListenId),
                nextListenId = state.nextListenId + 1L
            )
        }
        true
    }

    suspend fun getPendingListens(limit: Int = 100): List<ListenBrainzPendingListen> = withContext(ioDispatcher) {
        store.load().pendingListens
            .sortedBy { it.createdAtMs }
            .take(limit)
    }

    suspend fun deleteListens(ids: List<Long>): Int = withContext(ioDispatcher) {
        if (ids.isEmpty()) return@withContext 0
        val wanted = ids.toSet()
        var removed = 0
        store.update("listenbrainz_pending_listens") { state ->
            val kept = state.pendingListens.filterNot { it.id in wanted }
            removed = state.pendingListens.size - kept.size
            state.copy(pendingListens = kept)
        }
        removed
    }

    suspend fun count(): Int = withContext(ioDispatcher) {
        store.load().pendingListens.size
    }

    fun countFlow(): Flow<Int> {
        return store.tableInvalidationFlow
            .filter { it == "listenbrainz_pending_listens" }
            .onStart { emit("listenbrainz_pending_listens") }
            .map { count() }
            .distinctUntilChanged()
            .flowOn(ioDispatcher)
    }

    suspend fun trimQueue(maxSize: Int = 3000): Int = withContext(ioDispatcher) {
        val total = store.load().pendingListens.size
        if (total <= maxSize) return@withContext 0
        val toRemove = total - maxSize
        var removed = 0
        store.update("listenbrainz_pending_listens") { state ->
            val ordered = state.pendingListens.sortedBy { it.createdAtMs }
            val kept = ordered.drop(toRemove)
            removed = ordered.size - kept.size
            state.copy(pendingListens = kept)
        }
        removed
    }
}
