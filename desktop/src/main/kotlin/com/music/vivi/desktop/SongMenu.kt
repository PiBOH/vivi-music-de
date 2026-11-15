package com.music.vivi.desktop

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.PlaylistAdd
import androidx.compose.material.icons.automirrored.filled.QueueMusic
import androidx.compose.material.icons.filled.Album
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.LibraryAdd
import androidx.compose.material.icons.filled.LibraryAddCheck
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.automirrored.filled.PlaylistPlay
import androidx.compose.material.icons.filled.Radio
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.ThumbDown
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.staticCompositionLocalOf
import com.music.innertube.YouTube
import com.music.innertube.models.SongItem
import com.music.vivi.sync.SyncedSong
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.awt.Toolkit
import java.awt.datatransfer.StringSelection

/** Global non-invasive toast/snackbar for one-shot feedback (copy, …). */
object DesktopSnackbar {
    private val _events = MutableSharedFlow<String>(extraBufferCapacity = 16)
    val events = _events.asSharedFlow()
    fun show(message: String) { _events.tryEmit(message) }
}

/**
 * In-memory like / library state for the current session. The desktop has no
 * Room database like the Android app, so the toggled state lives here for the
 * session; the authoritative state stays on the YouTube account.
 *
 * The liked part is also this desktop's side of the **device-sync liked-song
 * ledger**. Each id maps to a [SyncedSong] entry — a like carries the song's
 * metadata (so the phone can store a song it has never seen), an unlike a
 * `deleted` tombstone — and `updatedAt` is the last-write-wins key against the
 * paired device's own entry for the same id. Entries learned from the peer are
 * held here too (that is what keeps the hearts right for the songs the phone
 * liked), but they are **not** sent back: the phone already has them.
 */
object SongActions {
    private val liked = mutableStateMapOf<String, SyncedSong>()
    private val inLibrary = mutableStateMapOf<String, Boolean>()

    /**
     * Incremented on every *local* like/unlike, so the sync layer knows when to
     * push the ledger. An entry learned from the peer does not bump it: a remote
     * change must never be sent back (the echo would ping-pong).
     */
    private val _likedVersion = MutableStateFlow(0)
    val likedVersion: StateFlow<Int> = _likedVersion.asStateFlow()

    /**
     * Fired when a song is liked ("Auto download on like" hooks here):
     * receives the song id and title. Registered once by the main window.
     * Only a like the **user** makes fires it, never one learned from the peer.
     */
    @Volatile
    var onSongLiked: ((id: String, title: String) -> Unit)? = null

    fun isLiked(id: String): Boolean = liked[id]?.let { !it.deleted } ?: false

    fun isInLibrary(song: SongItem): Boolean =
        inLibrary[song.id] ?: (song.libraryRemoveToken != null)

    /**
     * Applies a like/unlike the **user** made here: stamps the ledger entry with
     * the current time and marks it for the next library push.
     */
    fun setLiked(
        id: String,
        value: Boolean,
        title: String? = null,
        artist: String? = null,
        thumbnail: String? = null,
    ) {
        val previous = liked[id]
        liked[id] = SyncedSong(
            id = id,
            title = title ?: previous?.title.orEmpty(),
            artist = artist ?: previous?.artist.orEmpty(),
            thumbnail = thumbnail ?: previous?.thumbnail,
            updatedAt = System.currentTimeMillis(),
            deleted = !value,
        )
        _likedVersion.value += 1
        // "Songs" in the Library screen is the liked list: it has to reload, or
        // the heart toggles and the list behind it does not move.
        LibraryRefresh.bump()
        if (value && title != null) {
            onSongLiked?.invoke(id, title)
        }
    }

    fun setInLibrary(id: String, value: Boolean) { inLibrary[id] = value }

    /** The songs liked right now (the flat id list of the wire protocol). */
    fun likedIds(): List<String> = liked.values.filterNot { it.deleted }.map { it.id }

    /**
     * The ledger as this desktop sends it: the entries **this** session edited
     * (a like with metadata, an unlike as a tombstone) plus the tombstones of
     * the ones it was told about and must not forget. Entries simply learned
     * from the peer are left out — the phone already has them, and re-sending a
     * whole liked library on every push (a snapshot goes out on every queue
     * change) is bandwidth nothing needs.
     */
    fun toSyncedLiked(now: Long = System.currentTimeMillis()): List<SyncedSong> =
        liked.values.filter { it.updatedAt > 0L && !(it.deleted && now - it.updatedAt > TOMBSTONE_TTL_MS) }

    /**
     * Merges the peer's liked entries into the ledger, last-write-wins per id.
     *
     * A remote entry only wins on a **strictly newer** edit time, so an echo of
     * our own like (same timestamp) cannot flip it back, and an unlike is never
     * invented: an entry with no time at all (`updatedAt == 0`, an older peer, or
     * the flat [ids] list) can only *add* a like, and only when this desktop has
     * no entry for that song yet.
     *
     * Nothing here is marked as a local edit and [onSongLiked] is not fired: the
     * peer's like is not this desktop's like ("Auto download on like" must not
     * start downloading the phone's likes), and a remote change must not travel
     * back.
     */
    fun applyRemoteLiked(entries: List<SyncedSong>, ids: List<String> = emptyList()) {
        val pending = LinkedHashMap<String, SyncedSong>()
        for (r in entries) {
            if (r.id.isBlank()) continue
            val local = liked[r.id]
            val wins = when {
                local == null -> !r.deleted && r.updatedAt >= 0L
                r.updatedAt > 0L -> r.updatedAt > local.updatedAt
                else -> false
            }
            if (wins) pending[r.id] = r
        }
        for (id in ids) {
            if (id.isBlank() || liked.containsKey(id) || pending.containsKey(id)) continue
            pending[id] = SyncedSong(id = id, deleted = false)
        }
        if (pending.isEmpty()) return
        pending.forEach { (id, r) ->
            liked[id] = SyncedSong(
                id = id,
                title = r.title.ifBlank { liked[id]?.title.orEmpty() },
                artist = r.artist.ifBlank { liked[id]?.artist.orEmpty() },
                thumbnail = r.thumbnail ?: liked[id]?.thumbnail,
                updatedAt = r.updatedAt,
                deleted = r.deleted,
            )
        }
        // A like/unlike made on the paired phone lands in the same list.
        LibraryRefresh.bump()
    }

    /**
     * How long a tombstone is kept. It is what lets the peer apply an unlike made
     * while it was offline; a whole quarter is far more than a pairing needs, and
     * it keeps the ledger from growing forever.
     */
    private const val TOMBSTONE_TTL_MS = 90L * 24 * 60 * 60 * 1000
}

/**
 * Puts [text] on the system clipboard, retrying briefly.
 *
 * A single attempt is not enough on Windows: the clipboard is a shared, locked
 * resource, and `setContents` throws `IllegalStateException: cannot open system
 * clipboard` whenever another process happens to hold it. The old silent
 * `runCatching` swallowed that and the caller cheerfully said "copied", which is
 * the "I press copy and nothing lands on the clipboard" report. Returns whether
 * the text really got there.
 */
fun copyToClipboard(text: String): Boolean {
    repeat(5) { attempt ->
        val ok = runCatching {
            Toolkit.getDefaultToolkit().systemClipboard.setContents(StringSelection(text), null)
            true
        }.getOrDefault(false)
        if (ok) return true
        if (attempt < 4) runCatching { Thread.sleep(60L) }
    }
    return false
}

/**
 * The row actions every screen shares. The menu used to reach the player only
 * through the per-row callbacks, so most screens offered a shorter menu than
 * Bit's. The app provides these once at the root, and any row on any
 * screen gets the same actions the player itself has.
 */
data class SongMenuActions(
    val playNext: (SongItem) -> Unit = {},
    val addToQueue: (SongItem) -> Unit = {},
    val startRadio: (SongItem) -> Unit = {},
    val download: (SongItem) -> Unit = {},
    val deleteDownload: (SongItem) -> Unit = {},
    val isDownloaded: (SongItem) -> Boolean = { false },
    val openAlbum: (String) -> Unit = {},
    val openArtist: (String) -> Unit = {},
    val setSleepTimer: (Int?) -> Unit = {},
)

val LocalSongMenuActions = staticCompositionLocalOf { SongMenuActions() }

/** The player's own track type, built from a search or library row. */
internal fun SongItem.toNowPlaying(): NowPlaying = NowPlaying(
    videoId = id,
    title = title,
    artist = artists.joinToString(", ") { it.name },
    thumbnail = thumbnail,
    durationMs = (duration ?: 0) * 1000L,
    album = album?.name,
)

/** "⋮" context menu for a song: like, library, add-to-playlist, queue and share. */
@Composable
fun SongMenu(
    song: SongItem,
    language: String,
    onAddToPlaylist: (() -> Unit)?,
    /** Adds the song to the play queue. It used to be a bare "＋" glyph at the
     *  end of the row: no icon, no tooltip, no label, so it read as a mystery
     *  button that did nothing (the row never showed whether the song landed in
     *  the queue). The action lives here now, where it is named. */
    onAddToQueue: (() -> Unit)? = null,
    /** Queue-specific "play next" (the queue reorders itself instead of adding
     *  a second copy); other surfaces leave it null and use the root action. */
    onPlayNext: (() -> Unit)? = null,
) {
    var expanded by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    val liked = SongActions.isLiked(song.id)
    val inLibrary = SongActions.isInLibrary(song)
    val actions = LocalSongMenuActions.current
    val downloaded = actions.isDownloaded(song)
    var disliked by remember(song.id) { mutableStateOf(false) }
    var sleepDialog by remember { mutableStateOf(false) }

    Box {
        Tooltip(Localization.get(language, "tooltip_more")) {
            M3IconButton(onClick = { expanded = true }) {
                Icon(
                    Icons.Filled.MoreVert,
                    contentDescription = Localization.get(language, "more"),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            DropdownMenuItem(
                text = { Text(Localization.get(language, if (liked) "unlike" else "like")) },
                leadingIcon = {
                    Icon(
                        if (liked) Icons.Filled.Favorite else Icons.Filled.FavoriteBorder,
                        contentDescription = null,
                        tint = if (liked) MaterialTheme.colorScheme.error else LocalContentColor.current,
                    )
                },
                onClick = {
                    // A rating row stays open: Bit lets a listener change
                    // their mind without reopening the menu.
                    val next = !liked
                    // The metadata travels with the like: it is what lets the
                    // paired phone store a song it has never seen (see
                    // SongActions / LibrarySnapshot.likedSongs).
                    SongActions.setLiked(
                        song.id,
                        next,
                        title = song.title,
                        artist = song.artists.joinToString(", ") { it.name },
                        thumbnail = song.thumbnail,
                    )
                    scope.launch { YouTube.likeVideo(song.id, next) }
                },
            )
            DropdownMenuItem(
                text = { Text(Localization.get(language, if (disliked) "undo_dislike" else "dislike")) },
                leadingIcon = { Icon(Icons.Filled.ThumbDown, contentDescription = null) },
                onClick = {
                    disliked = !disliked
                    if (disliked) scope.launch { YouTube.dislikeVideo(song.id) }
                },
            )
            HorizontalDivider()
            DropdownMenuItem(
                text = { Text(Localization.get(language, if (inLibrary) "remove_from_library" else "add_to_library")) },
                leadingIcon = {
                    Icon(
                        if (inLibrary) Icons.Filled.LibraryAddCheck else Icons.Filled.LibraryAdd,
                        contentDescription = null,
                    )
                },
                onClick = {
                    expanded = false
                    val next = !inLibrary
                    SongActions.setInLibrary(song.id, next)
                    scope.launch { YouTube.toggleSongLibrary(song.id, addToLibrary = next) }
                },
            )
            if (onAddToPlaylist != null) {
                DropdownMenuItem(
                    text = { Text(Localization.get(language, "add_to_playlist")) },
                    leadingIcon = { Icon(Icons.AutoMirrored.Filled.PlaylistAdd, contentDescription = null) },
                    onClick = {
                        expanded = false
                        onAddToPlaylist()
                    },
                )
            }
            DropdownMenuItem(
                text = { Text(Localization.get(language, if (downloaded) "remove_download" else "download")) },
                leadingIcon = {
                    Icon(if (downloaded) Icons.Filled.Delete else Icons.Filled.Download, contentDescription = null)
                },
                onClick = {
                    expanded = false
                    if (downloaded) actions.deleteDownload(song) else actions.download(song)
                },
            )
            // Listen Together: the queue belongs to the host, so a guest's
            // row is greyed instead of looking available (PlayerController
            // refuses the call either way).
            DropdownMenuItem(
                text = { Text(Localization.get(language, "add_to_queue")) },
                leadingIcon = {
                    Icon(Icons.AutoMirrored.Filled.QueueMusic, contentDescription = null)
                },
                enabled = !transportLocked(),
                onClick = {
                    expanded = false
                    onAddToQueue?.invoke() ?: actions.addToQueue(song)
                },
            )
            DropdownMenuItem(
                text = { Text(Localization.get(language, "play_next")) },
                leadingIcon = { Icon(Icons.AutoMirrored.Filled.PlaylistPlay, contentDescription = null) },
                enabled = !transportLocked(),
                onClick = {
                    expanded = false
                    onPlayNext?.invoke() ?: actions.playNext(song)
                },
            )
            DropdownMenuItem(
                text = { Text(Localization.get(language, "start_radio")) },
                leadingIcon = { Icon(Icons.Filled.Radio, contentDescription = null) },
                onClick = {
                    expanded = false
                    actions.startRadio(song)
                },
            )
            // "Suggest to the host": offered only while this user is a GUEST in
            // a Listen Together room. The host owns the queue, so the action has
            // no meaning for them, and outside a room it has nowhere to go,
            // exactly the condition the mobile song menu uses.
            val inRoom by ListenTogetherGate.inRoom
            val isHostRole by ListenTogetherGate.isHost
            if (inRoom && !isHostRole) {
                DropdownMenuItem(
                    text = { Text(Localization.get(language, "lt_suggest")) },
                    leadingIcon = {
                        Icon(Icons.AutoMirrored.Filled.QueueMusic, contentDescription = null)
                    },
                    onClick = {
                        expanded = false
                        ListenTogetherBridge.manager?.suggestTrack(
                            LtTrackInfo(
                                id = song.id,
                                title = song.title,
                                artist = song.artists.joinToString(", ") { it.name },
                                album = song.album?.name,
                                duration = (song.duration ?: 0) * 1000L,
                                thumbnail = song.thumbnail,
                            ),
                        )
                        DesktopSnackbar.show(Localization.get(language, "lt_suggest"))
                    },
                )
            }
            if (song.album?.id != null) {
                DropdownMenuItem(
                    text = { Text(Localization.get(language, "open_album")) },
                    leadingIcon = { Icon(Icons.Filled.Album, contentDescription = null) },
                    onClick = {
                        expanded = false
                        song.album?.id?.let(actions.openAlbum)
                    },
                )
            }
            song.artists.firstOrNull()?.id?.let { artistId ->
                DropdownMenuItem(
                    text = { Text(Localization.get(language, "open_artist")) },
                    leadingIcon = { Icon(Icons.Filled.Person, contentDescription = null) },
                    onClick = {
                        expanded = false
                        actions.openArtist(artistId)
                    },
                )
            }
            DropdownMenuItem(
                text = { Text(Localization.get(language, "sleep_timer")) },
                leadingIcon = { Icon(Icons.Filled.Timer, contentDescription = null) },
                onClick = {
                    expanded = false
                    sleepDialog = true
                },
            )
            HorizontalDivider()
            DropdownMenuItem(
                text = { Text(Localization.get(language, "share")) },
                leadingIcon = { Icon(Icons.Filled.Share, contentDescription = null) },
                onClick = {
                    expanded = false
                    copyToClipboard(song.shareLink)
                    DesktopSnackbar.show(Localization.get(language, "copied_to_clipboard"))
                },
            )
        }
    }

    if (sleepDialog) {
        AlertDialog(
            onDismissRequest = { sleepDialog = false },
            title = { Text(Localization.get(language, "sleep_timer")) },
            text = {
                Column {
                    listOf(5, 10, 15, 30, 45, 60).forEach { minutes ->
                        TextButton(
                            onClick = {
                                actions.setSleepTimer(minutes)
                                sleepDialog = false
                            },
                        ) { Text(Localization.get(language, "${minutes}_minutes")) }
                    }
                    TextButton(
                        onClick = {
                            actions.setSleepTimer(null)
                            sleepDialog = false
                        },
                    ) { Text(Localization.get(language, "turn_off")) }
                }
            },
            confirmButton = {},
            dismissButton = {
                TextButton(onClick = { sleepDialog = false }) {
                    Text(Localization.get(language, "cancel"))
                }
            },
        )
    }
}
