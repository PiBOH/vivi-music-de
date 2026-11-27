package com.music.vivi.desktop.data.youtube

import com.music.innertube.YouTube
import com.music.innertube.models.PlaylistItem
import com.music.vivi.desktop.data.store.LocalLibraryState
import com.music.vivi.desktop.data.store.LocalLibraryStore
import com.music.vivi.desktop.data.store.StoredPlaylistSnapshot
import com.music.vivi.desktop.data.store.StoredPlaylistUndo
import java.util.concurrent.ConcurrentHashMap
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

data class RemotePlaylist(
    val id: String,
    val title: String,
    val songCountText: String?,
    val thumbnail: String?,
    val isEditable: Boolean
)

/** Everything needed to put an account playlist back, in its order. */
@Serializable
data class AccountPlaylistSnapshot(
    val title: String,
    val videoIds: List<String> = emptyList()
)

data class RestoreOutcome(
    val playlistId: String,
    val restoredTracks: Int,
    val failedTracks: Int
)

/** One row of a playlist page, with the token a removal needs. */
data class PlaylistTrack(
    val videoId: String,
    val setVideoId: String?
)

/** What happened when the playing track was offered to one playlist. */
enum class AddOutcome {
    ADDED,
    ALREADY_PRESENT,
    FAILED
}

/** One file that can be moved to another account: names and tracks, nothing else. */
@Serializable
data class AccountPlaylistPack(
    val format: String = "vivi-account-playlists",
    val version: Int = 1,
    val exportedAt: Long = 0L,
    val source: String? = null,
    val playlists: List<AccountPlaylistSnapshot> = emptyList()
) {
    companion object {
        const val FORMAT = "vivi-account-playlists"
        const val VERSION = 1
    }
}

data class CopyOutcome(
    val created: Int,
    val restoredTracks: Int,
    val failedTracks: Int
)

/** One account-playlist problem the health check reports. */
enum class AccountPlaylistIssueKind {
    READ_ONLY,
    DUPLICATE_TITLE,
    EMPTY,
    DUPLICATE_TRACKS,
    UNREADABLE
}

data class AccountPlaylistIssue(
    val playlistId: String,
    val title: String,
    val kind: AccountPlaylistIssueKind,
    val detail: String
)

data class AccountPlaylistHealth(
    val playlistCount: Int,
    val editableCount: Int,
    val readOnlyCount: Int,
    val trackCount: Int,
    val issues: List<AccountPlaylistIssue>
) {
    val isClean: Boolean
        get() = issues.isEmpty()
}

/** Writes and reads the JSON file used to move playlists between accounts. */
object AccountPlaylistPacks {

    private val json = Json {
        prettyPrint = true
        ignoreUnknownKeys = true
        encodeDefaults = true
    }

    fun encode(pack: AccountPlaylistPack): String =
        json.encodeToString(AccountPlaylistPack.serializer(), pack)

    fun decode(text: String): Result<AccountPlaylistPack> = runCatching {
        val pack = json.decodeFromString(AccountPlaylistPack.serializer(), text)
        require(pack.format == AccountPlaylistPack.FORMAT) {
            "This is not an Axolotl Music account playlist file."
        }
        require(pack.version <= AccountPlaylistPack.VERSION) {
            "This file was written by a newer Axolotl Music."
        }
        pack
    }
}

/** The key the order and the undo of a signed-out session are stored under. */
internal const val GUEST_PLAYLIST_ACCOUNT = "guest"

internal fun playlistAccountKey(accountId: String?): String =
    accountId?.takeIf { it.isNotBlank() } ?: GUEST_PLAYLIST_ACCOUNT

/**
 * The order one account's card draws.
 *
 * The map is asked first. A file written before the orders were per account
 * only has the old single list, so that list is used as the first account's
 * order until the map is written; once any account saves an order the old list
 * is no longer applied to a different account.
 */
internal fun storedPlaylistOrder(state: LocalLibraryState, accountId: String?): List<String> {
    val key = playlistAccountKey(accountId)
    state.youTubePlaylistOrders[key]?.let { return it }
    return if (state.youTubePlaylistOrders.isEmpty()) state.youTubePlaylistOrder else emptyList()
}

/** The state with one account's order replaced; the old single list is left readable. */
internal fun withPlaylistOrder(
    state: LocalLibraryState,
    accountId: String?,
    playlistIds: List<String>
): LocalLibraryState {
    val key = playlistAccountKey(accountId)
    if (state.youTubePlaylistOrders[key] == playlistIds) return state
    return state.copy(youTubePlaylistOrders = state.youTubePlaylistOrders + (key to playlistIds))
}

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

/** A title the target account does not already use, so an import never merges by accident. */
internal fun uniquePlaylistTitle(title: String, existing: Collection<String>): String {
    val base = title.trim().ifBlank { "Imported playlist" }
    val taken = existing.mapTo(HashSet()) { it.trim().lowercase() }
    if (base.lowercase() !in taken) return base
    var index = 1
    while (true) {
        val candidate = if (index == 1) "$base (imported)" else "$base (imported $index)"
        if (candidate.lowercase() !in taken) return candidate
        index++
    }
}

/**
 * The problems in the account's own playlists, from the list and the tracks
 * that were read for it. A playlist whose page failed is [unreadable] and is
 * not called empty, because the page did not prove it.
 */
internal fun accountPlaylistHealth(
    playlists: List<RemotePlaylist>,
    tracksByPlaylist: Map<String, List<PlaylistTrack>>,
    unreadable: Set<String> = emptySet()
): AccountPlaylistHealth {
    val issues = mutableListOf<AccountPlaylistIssue>()
    val seenTitles = HashMap<String, Int>()

    for (playlist in playlists) {
        if (!playlist.isEditable) {
            issues += AccountPlaylistIssue(
                playlistId = playlist.id,
                title = playlist.title,
                kind = AccountPlaylistIssueKind.READ_ONLY,
                detail = "Read-only: it cannot be renamed or deleted here."
            )
        }

        val key = playlist.title.trim().lowercase()
        if (key.isNotBlank()) {
            val seen = seenTitles[key] ?: 0
            if (seen > 0) {
                issues += AccountPlaylistIssue(
                    playlistId = playlist.id,
                    title = playlist.title,
                    kind = AccountPlaylistIssueKind.DUPLICATE_TITLE,
                    detail = "Another playlist has this name."
                )
            }
            seenTitles[key] = seen + 1
        }

        if (playlist.id in unreadable) {
            issues += AccountPlaylistIssue(
                playlistId = playlist.id,
                title = playlist.title,
                kind = AccountPlaylistIssueKind.UNREADABLE,
                detail = "Its tracks could not be read; try the check again."
            )
            continue
        }

        val tracks = tracksByPlaylist[playlist.id] ?: continue
        if (tracks.isEmpty()) {
            issues += AccountPlaylistIssue(
                playlistId = playlist.id,
                title = playlist.title,
                kind = AccountPlaylistIssueKind.EMPTY,
                detail = "It has no tracks."
            )
        }
        val repeated = tracks.groupingBy { it.videoId }.eachCount().values.count { it > 1 }
        if (repeated > 0) {
            issues += AccountPlaylistIssue(
                playlistId = playlist.id,
                title = playlist.title,
                kind = AccountPlaylistIssueKind.DUPLICATE_TRACKS,
                detail = "$repeated ${if (repeated == 1) "track is" else "tracks are"} in it more than once."
            )
        }
    }

    return AccountPlaylistHealth(
        playlistCount = playlists.size,
        editableCount = playlists.count { it.isEditable },
        readOnlyCount = playlists.count { !it.isEditable },
        trackCount = tracksByPlaylist.values.sumOf { it.size },
        issues = issues
    )
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
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO,
    /** The account whose order, undo and health are read; a seam for tests. */
    private val accountId: () -> String? = { YouTubeAccount.activeAccountId() }
) {

    private val editability = ConcurrentHashMap<String, Boolean>()

    suspend fun load(): Result<List<RemotePlaylist>> = withContext(ioDispatcher) {
        runCatching {
            val items = readLibraryItems()
            val gate = Semaphore(4)
            val resolved = coroutineScope {
                items.map { playlist ->
                    async {
                        playlist.copy(isEditable = resolveEditable(playlist, gate))
                    }
                }.awaitAll()
            }
            applyStoredPlaylistOrder(
                resolved,
                store?.let { storedPlaylistOrder(it.load(), accountId()) }.orEmpty()
            )
        }
    }

    private suspend fun readLibraryItems(): List<RemotePlaylist> {
        val page = YouTube.library("FEmusic_liked_playlists").getOrThrow()
        return page.items
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

    /**
     * Adds the track only when the playlist does not already hold it, so the
     * card can say "already there" instead of adding a second copy and calling
     * it a success.
     */
    suspend fun addSongIfAbsent(playlistId: String, videoId: String): AddOutcome =
        withContext(ioDispatcher) {
            addSongIfAbsentWith(
                playlistId = playlistId,
                videoId = videoId,
                readTracks = { readTracks(it) },
                add = { id, video -> YouTube.addToPlaylistUnit(id, video) }
            )
        }

    /** Reads every page of a playlist, keeping the token a removal needs. */
    suspend fun tracks(playlistId: String): Result<List<PlaylistTrack>> =
        withContext(ioDispatcher) { readTracks(playlistId) }

    private suspend fun readTracks(playlistId: String): Result<List<PlaylistTrack>> = runCatching {
        val first = YouTube.playlist(playlistId).getOrThrow()
        val tracks = first.songs.mapTo(mutableListOf()) {
            PlaylistTrack(videoId = it.id, setVideoId = it.setVideoId)
        }
        var continuation = first.songsContinuation
        while (continuation != null) {
            val next = YouTube.playlistContinuation(continuation).getOrThrow()
            tracks += next.songs.map { PlaylistTrack(videoId = it.id, setVideoId = it.setVideoId) }
            continuation = next.continuation
        }
        tracks
    }

    /** Removes one track from a playlist by resolving its `setVideoId` first. */
    suspend fun removeSong(playlistId: String, videoId: String): Result<Unit> =
        withContext(ioDispatcher) {
            removeSongWith(
                playlistId = playlistId,
                videoId = videoId,
                readTracks = { readTracks(it) },
                remove = { id, video, setVideoId ->
                    YouTube.removeFromPlaylistUnit(id, video, setVideoId)
                }
            )
        }

    /** Saves one account's order, so a drag survives leaving the screen. */
    suspend fun saveOrder(playlistIds: List<String>) = withContext(ioDispatcher) {
        val library = store ?: return@withContext
        val account = accountId()
        library.update("youtube_playlists") { state ->
            withPlaylistOrder(state, account, playlistIds)
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
            val videoIds = readTracksFromFirstPage(first).toMutableList()
            var continuation = first.songsContinuation
            while (continuation != null) {
                val next = YouTube.playlistContinuation(continuation).getOrThrow()
                videoIds += next.songs.map { it.id }
                continuation = next.continuation
            }
            AccountPlaylistSnapshot(title = first.playlist.title, videoIds = videoIds)
        }
    }

    private fun readTracksFromFirstPage(page: com.music.innertube.pages.PlaylistPage): List<String> =
        page.songs.map { it.id }

    /**
     * Recreates a deleted playlist with its name and its tracks in order. The
     * cover and the description are not part of the snapshot, so they are not
     * restored; a track that YouTube refuses is counted, not fatal.
     */
    suspend fun restore(snapshot: AccountPlaylistSnapshot): Result<RestoreOutcome> =
        withContext(ioDispatcher) { createWithTracks(snapshot) }

    private suspend fun createWithTracks(snapshot: AccountPlaylistSnapshot): Result<RestoreOutcome> = runCatching {
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

    /** The Undo bar for this account, when one was left by an earlier session. */
    suspend fun pendingUndo(): StoredPlaylistUndo? = withContext(ioDispatcher) {
        val stored = store?.load()?.youTubePlaylistUndo ?: return@withContext null
        if (stored.accountId == playlistAccountKey(accountId())) stored else null
    }

    /** Writes the Undo bar for this account, scoped to it and stamped once. */
    suspend fun rememberUndo(undo: StoredPlaylistUndo) = withContext(ioDispatcher) {
        val library = store ?: return@withContext
        val scoped = undo.copy(
            accountId = playlistAccountKey(accountId()),
            createdAt = undo.createdAt.takeIf { it > 0L } ?: System.currentTimeMillis()
        )
        library.update("youtube_playlists") { state ->
            if (state.youTubePlaylistUndo == scoped) state else state.copy(youTubePlaylistUndo = scoped)
        }
    }

    suspend fun clearUndo() = withContext(ioDispatcher) {
        val library = store ?: return@withContext
        library.update("youtube_playlists") { state ->
            if (state.youTubePlaylistUndo == null) state else state.copy(youTubePlaylistUndo = null)
        }
    }

    /** Writes the selected playlists, names and tracks only, to one portable file. */
    suspend fun exportPack(playlistIds: List<String>): Result<AccountPlaylistPack> =
        withContext(ioDispatcher) {
            runCatching {
                val snapshots = playlistIds.distinct().map { id -> snapshot(id).getOrThrow() }
                AccountPlaylistPack(
                    exportedAt = System.currentTimeMillis(),
                    source = YouTubeAccount.activeSummary()?.name,
                    playlists = snapshots
                )
            }
        }

    /**
     * Copies every playlist in [pack] into the signed-in account. A name the
     * account already uses gets a suffix, and nothing is merged into an
     * existing playlist.
     */
    suspend fun copyPack(pack: AccountPlaylistPack): Result<CopyOutcome> = withContext(ioDispatcher) {
        runCatching {
            copyPlaylistSnapshots(
                playlists = pack.playlists,
                existingTitles = readLibraryItems().map { it.title },
                create = { title ->
                    runCatching {
                        YouTube.createPlaylist(title).removePrefix("VL").also { editability[it] = true }
                    }
                },
                add = { id, video -> YouTube.addToPlaylistUnit(id, video) }
            ).getOrThrow()
        }
    }

    /**
     * Reads every playlist and checks names, editability, empty lists and
     * repeated tracks. It is an explicit action: nothing here changes YouTube.
     */
    suspend fun checkHealth(): Result<AccountPlaylistHealth> = withContext(ioDispatcher) {
        runCatching {
            val playlists = load().getOrThrow()
            val gate = Semaphore(4)
            val reads = coroutineScope {
                playlists.map { playlist ->
                    async {
                        playlist.id to gate.withPermit { readTracks(playlist.id) }
                    }
                }.awaitAll()
            }
            val tracksByPlaylist = HashMap<String, List<PlaylistTrack>>()
            val unreadable = HashSet<String>()
            for ((id, result) in reads) {
                result.onSuccess { tracksByPlaylist[id] = it }
                    .onFailure { unreadable += id }
            }
            accountPlaylistHealth(playlists, tracksByPlaylist, unreadable)
        }
    }

}

/**
 * Adds one track only when it is absent. The read and the write are callbacks
 * so the decision can be tested without a YouTube session.
 */
internal suspend fun addSongIfAbsentWith(
    playlistId: String,
    videoId: String,
    readTracks: suspend (String) -> Result<List<PlaylistTrack>>,
    add: suspend (String, String) -> Result<Unit>
): AddOutcome {
    if (videoId.isBlank()) return AddOutcome.FAILED
    val tracks = readTracks(playlistId).getOrElse { return AddOutcome.FAILED }
    if (tracks.any { it.videoId == videoId }) return AddOutcome.ALREADY_PRESENT
    return if (add(playlistId, videoId).isSuccess) AddOutcome.ADDED else AddOutcome.FAILED
}

/** What one bulk Add did; only [addedIds] may be removed by its Undo. */
internal data class BulkAddResult(
    val addedIds: List<String>,
    val already: Int,
    val failed: Int
) {
    val added: Int get() = addedIds.size
}

/** Offers one track to several playlists, keeping the three outcomes separate. */
internal suspend fun bulkAddSong(
    playlistIds: List<String>,
    videoId: String,
    add: suspend (String, String) -> AddOutcome
): BulkAddResult {
    val addedIds = mutableListOf<String>()
    var already = 0
    var failed = 0
    for (id in playlistIds) {
        when (add(id, videoId)) {
            AddOutcome.ADDED -> addedIds += id
            AddOutcome.ALREADY_PRESENT -> already++
            AddOutcome.FAILED -> failed++
        }
    }
    return BulkAddResult(addedIds, already, failed)
}

/** Removes one track from several playlists and returns the ids that failed. */
internal suspend fun removeSongFromPlaylists(
    playlistIds: List<String>,
    videoId: String,
    remove: suspend (String, String) -> Result<Unit>
): List<String> = playlistIds.filter { id -> remove(id, videoId).isFailure }

/**
 * Removes one track, using the last non-blank `setVideoId` the playlist holds
 * for it. The read and the write are callbacks for the same reason as above.
 */
internal suspend fun removeSongWith(
    playlistId: String,
    videoId: String,
    readTracks: suspend (String) -> Result<List<PlaylistTrack>>,
    remove: suspend (String, String, String) -> Result<Unit>
): Result<Unit> = runCatching {
    val tracks = readTracks(playlistId).getOrThrow()
    val target = tracks.lastOrNull {
        it.videoId == videoId && !it.setVideoId.isNullOrBlank()
    } ?: error("The track is not in that playlist, so it cannot be removed.")
    remove(playlistId, videoId, target.setVideoId!!).getOrThrow()
}

/** The title and tracks one source playlist becomes, before anything is written. */
internal data class PlaylistCopyPlan(
    val title: String,
    val videoIds: List<String>
)

/** A suffix is added when the target already uses the title, never a merge. */
internal fun planPlaylistCopies(
    playlists: List<AccountPlaylistSnapshot>,
    existingTitles: Collection<String>
): List<PlaylistCopyPlan> {
    val titles = existingTitles.toMutableList()
    return playlists.map { snapshot ->
        val title = uniquePlaylistTitle(snapshot.title, titles)
        titles += title
        PlaylistCopyPlan(title = title, videoIds = snapshot.videoIds)
    }
}

/**
 * Creates one new playlist per source snapshot and adds its tracks in order.
 * A refused track is counted; it does not stop the rest of the copy.
 */
internal suspend fun copyPlaylistSnapshots(
    playlists: List<AccountPlaylistSnapshot>,
    existingTitles: Collection<String>,
    create: suspend (String) -> Result<String>,
    add: suspend (String, String) -> Result<Unit>
): Result<CopyOutcome> = runCatching {
    require(playlists.isNotEmpty()) { "The file has no playlists." }
    var created = 0
    var restoredTracks = 0
    var failedTracks = 0
    for (plan in planPlaylistCopies(playlists, existingTitles)) {
        val playlistId = create(plan.title).getOrThrow()
        created++
        for (videoId in plan.videoIds) {
            if (add(playlistId, videoId).isSuccess) restoredTracks++ else failedTracks++
        }
    }
    CopyOutcome(created, restoredTracks, failedTracks)
}

/** The shape the Undo bar persists for one snapshot. */
internal fun AccountPlaylistSnapshot.toStoredSnapshot(): StoredPlaylistSnapshot =
    StoredPlaylistSnapshot(title = title, videoIds = videoIds)
