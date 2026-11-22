package com.music.vivi.desktop.data.youtube

import com.music.innertube.YouTube
import com.music.innertube.models.PlaylistItem
import com.music.vivi.desktop.data.store.LocalLibraryStore
import java.util.concurrent.ConcurrentHashMap
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit
import kotlinx.coroutines.withContext

data class RemotePlaylist(
    val id: String,
    val title: String,
    val songCountText: String?,
    val thumbnail: String?,
    val isEditable: Boolean
)

/** Everything needed to put a deleted account playlist back, in its order. */
data class AccountPlaylistSnapshot(
    val title: String,
    val videoIds: List<String>
)

data class RestoreOutcome(
    val playlistId: String,
    val restoredTracks: Int,
    val failedTracks: Int
)

/**
 * The order the card draws the account playlists in.
 *
 * YouTube Music does not expose an order for the library list, so this order
 * is local to the device: ids in [storedOrder] come first in that order, and an
 * id the account gained since the order was saved keeps its loaded position
 * after them.
 */
internal fun applyStoredPlaylistOrder(
    playlists: List<RemotePlaylist>,
    storedOrder: List<String>
): List<RemotePlaylist> {
    if (storedOrder.isEmpty()) return playlists
    val rank = HashMap<String, Int>(storedOrder.size)
    storedOrder.forEachIndexed { index, id -> rank.putIfAbsent(id, index) }
    return playlists.sortedBy { rank[it.id] ?: Int.MAX_VALUE }
}

/**
 * The playlists that live in the signed-in YouTube Music account.
 *
 * Reading goes through the same `FEmusic_liked_playlists` library page the
 * phone uses; writing uses the edit endpoints the innertube module already
 * exposes. Only the extension-free id travels between the screen and YouTube,
 * because every edit endpoint expects it without the `VL` prefix.
 *
 * The library row's menu is not a reliable editability signal: for a playlist
 * this account owns it carries bookmark, share and keep but no edit action, so
 * `PlaylistItem.isEditable` is false even on a playlist that can be renamed and
 * deleted. The playlist page does carry the answer
 * (`musicEditablePlaylistDetailHeaderRenderer`), so [load] reads it there once
 * per id, four at a time, and caches it for the session.
 */
class YouTubePlaylistsRepository(
    private val store: LocalLibraryStore? = null,
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO
) {

    private val editability = ConcurrentHashMap<String, Boolean>()

    suspend fun load(): Result<List<RemotePlaylist>> = withContext(ioDispatcher) {
        runCatching {
            val page = YouTube.library("FEmusic_liked_playlists").getOrThrow()
            val items = page.items
                .filterIsInstance<PlaylistItem>()
                .map { item ->
                    RemotePlaylist(
                        id = item.id.removePrefix("VL"),
                        title = item.title,
                        songCountText = item.songCountText,
                        thumbnail = item.thumbnail,
                        isEditable = item.isEditable
                    )
                }
                .filter { it.id.isNotBlank() }
                .distinctBy { it.id }

            val gate = Semaphore(4)
            val resolved = coroutineScope {
                items.map { playlist ->
                    async {
                        playlist.copy(isEditable = resolveEditable(playlist, gate))
                    }
                }.awaitAll()
            }
            applyStoredPlaylistOrder(resolved, store?.load()?.youTubePlaylistOrder.orEmpty())
        }
    }

    private suspend fun resolveEditable(
        playlist: RemotePlaylist,
        gate: Semaphore
    ): Boolean {
        if (playlist.isEditable) return true
        editability[playlist.id]?.let { return it }
        return gate.withPermit {
            editability[playlist.id] ?: run {
                // A failed page must not be cached as "read only": the next
                // refresh should be able to ask again.
                val page = YouTube.playlist(playlist.id).getOrNull()
                    ?: return@withPermit false
                val editable = page.playlist.isEditable
                editability[playlist.id] = editable
                editable
            }
        }
    }

    suspend fun create(name: String): Result<String> = withContext(ioDispatcher) {
        val title = name.trim()
        if (title.isBlank()) return@withContext Result.failure(IllegalArgumentException("The name is empty."))
        runCatching { YouTube.createPlaylist(title).removePrefix("VL") }
    }

    suspend fun rename(playlistId: String, name: String): Result<Unit> = withContext(ioDispatcher) {
        val title = name.trim()
        if (title.isBlank()) return@withContext Result.failure(IllegalArgumentException("The name is empty."))
        YouTube.renamePlaylistUnit(playlistId, title)
    }

    suspend fun delete(playlistId: String): Result<Unit> = withContext(ioDispatcher) {
        YouTube.deletePlaylistUnit(playlistId)
            .onSuccess { editability.remove(playlistId) }
    }

    suspend fun addSong(playlistId: String, videoId: String): Result<Unit> = withContext(ioDispatcher) {
        if (videoId.isBlank()) return@withContext Result.failure(IllegalArgumentException("No track is playing."))
        YouTube.addToPlaylistUnit(playlistId, videoId)
    }

    /** Saves the order the card draws, so a drag survives leaving the screen. */
    suspend fun saveOrder(playlistIds: List<String>) = withContext(ioDispatcher) {
        val library = store ?: return@withContext
        library.update("youtube_playlists") { state ->
            if (state.youTubePlaylistOrder == playlistIds) {
                state
            } else {
                state.copy(youTubePlaylistOrder = playlistIds)
            }
        }
    }

    /**
     * Reads the whole playlist, not just its first page, before it is deleted.
     * The snapshot is the only way [restore] can put the tracks back: YouTube
     * has no undelete call.
     */
    suspend fun snapshot(playlistId: String): Result<AccountPlaylistSnapshot> = withContext(ioDispatcher) {
        runCatching {
            val first = YouTube.playlist(playlistId).getOrThrow()
            val videoIds = first.songs.mapTo(mutableListOf()) { it.id }
            var continuation = first.songsContinuation
            while (continuation != null) {
                val next = YouTube.playlistContinuation(continuation).getOrThrow()
                videoIds += next.songs.map { it.id }
                continuation = next.continuation
            }
            AccountPlaylistSnapshot(title = first.playlist.title, videoIds = videoIds)
        }
    }

    /**
     * Recreates a deleted playlist with its name and its tracks in order. The
     * cover and the description are not part of the snapshot, so they are not
     * restored; a track that YouTube refuses is counted, not fatal.
     */
    suspend fun restore(snapshot: AccountPlaylistSnapshot): Result<RestoreOutcome> =
        withContext(ioDispatcher) {
            runCatching {
                val playlistId = YouTube.createPlaylist(snapshot.title).removePrefix("VL")
                var restored = 0
                var failed = 0
                for (videoId in snapshot.videoIds) {
                    if (YouTube.addToPlaylistUnit(playlistId, videoId).isSuccess) {
                        restored++
                    } else {
                        failed++
                    }
                }
                editability[playlistId] = true
                RestoreOutcome(playlistId, restored, failed)
            }
        }
}
