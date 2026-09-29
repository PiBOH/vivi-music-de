package com.music.vivi.desktop

import com.music.innertube.YouTube
import com.music.innertube.models.SongItem
import com.music.spotify.Spotify
import com.music.spotify.SpotifyAuth
import com.music.spotify.SpotifyMapper
import com.music.spotify.models.SpotifyPlaylist
import com.music.spotify.models.SpotifyTrack
import com.music.vivi.sync.SyncedSong
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit
import kotlinx.serialization.Serializable
import java.io.File
import java.util.concurrent.atomic.AtomicInteger

/**
 * Spotify import — the desktop port of the mobile app's `SpotifyImportViewModel`.
 *
 * Three things happen here, in the same order the phone does them:
 *
 *  1. **Connect.** Spotify's web player authenticates with the `sp_dc` cookie;
 *     [SpotifyAuth] turns it into a short-lived access token (with a TOTP the
 *     token endpoint requires) and [Spotify] then talks GraphQL with it. The
 *     cookie is what [loginWithWindow] reads off the embedded sign-in window
 *     (the manual paste is only the fallback for a machine where that window
 *     cannot open), the token is derived and cached until it expires:
 *     everything is stored locally, in `~/.vivimusic/spotify.json`:
 *     nothing about this feature leaves the machine except the requests to
 *     Spotify and to YouTube Music.
 *  2. **List.** The account's playlists (paged) and the Liked Songs count, so
 *     the user can pick what to bring over.
 *  3. **Import.** Each Spotify track is searched on YouTube Music
 *     ([YouTube.search] with the song filter) and the best candidate by
 *     [SpotifyMapper.matchScore] wins; the matches become a local playlist in
 *     [PlaylistStore], under a **stable id** (`SPOT…`) so importing the same
 *     Spotify playlist twice *updates* it here instead of creating a second
 *     copy.
 *
 * An import only ever writes on this machine. It deliberately does NOT go
 * through [PlaylistSync]: the account action is the explicit, confirmed one in
 * the Account screen, and creating a playlist on the user's YouTube Music
 * account is exactly the side effect an import must not have.
 */
object SpotifyImport {
    private val json = sharedJsonPretty
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    private val file = File(System.getProperty("user.home"), ".vivimusic/spotify.json").apply {
        parentFile?.mkdirs()
    }

    /** The local session: the captured cookies plus the token derived from them. */
    @Serializable
    private data class Session(
        val spDc: String = "",
        val spKey: String = "",
        val accessToken: String? = null,
        val expiresAt: Long = 0L,
        val accountName: String? = null,
        val accountAvatarUrl: String? = null,
    )

    data class State(
        val connected: Boolean = false,
        val accountName: String = "",
        val accountAvatarUrl: String? = null,
        val playlists: List<SpotifyPlaylist> = emptyList(),
        val likedSongsCount: Int = 0,
        val loading: Boolean = false,
        val error: String? = null,
    )

    /** Everything the progress row needs, and nothing else. */
    data class Progress(
        val playlistName: String,
        val matched: Int,
        val total: Int,
        /**
         * True once the source is written. The row then shows the outcome until
         * the user dismisses it, instead of vanishing with the work.
         */
        val finished: Boolean = false,
    )

    private val _state = MutableStateFlow(State())
    val state: StateFlow<State> = _state.asStateFlow()

    private val _progress = MutableStateFlow<Progress?>(null)
    val progress: StateFlow<Progress?> = _progress.asStateFlow()

    @Volatile
    private var session: Session? = null

    @Volatile
    private var job: kotlinx.coroutines.Job? = null

    /** The source id the UI uses for the Liked Songs row. */
    const val LIKED_SOURCE: String = "liked"

    /**
     * The page the sign-in window opens, and the page the manual fallback links
     * to when no window can be created.
     */
    fun loginUrl(): String = SpotifyAuth.LOGIN_URL

    /**
     * Opens the embedded Spotify sign-in window (the desktop port of the phone's
     * `SpotifyLoginSheet`) and connects with the cookies it captures.
     *
     * Returns false when the window could not be created at all — the screen
     * then falls back to the manual `sp_dc` paste.
     */
    fun loginWithWindow(language: String): Boolean {
        val opened = SpotifyLoginWebView.open(language) { captured ->
            if (captured == null || captured.spDc.isBlank()) {
                // The window was closed without signing in: keep the session the
                // account already had (there is nothing new to adopt) and stop
                // the spinner.
                _state.update { it.copy(loading = false) }
            } else {
                connect(captured.spDc, captured.spKey)
            }
        }
        if (opened) _state.update { it.copy(loading = true, error = null) }
        return opened
    }

    /**
     * The stable local playlist id a Spotify source imports into: the account's
     * own id behind a `SPOT` prefix, so re-importing rewrites that playlist
     * rather than adding another, and so it can never collide with the app's
     * `LP…` rows or with an account mirror (`yt-…`).
     */
    private fun localId(spotifyId: String): String = "SPOT$spotifyId"

    private fun load(): Session? = runCatching {
        if (!file.exists()) null
        else json.decodeFromString<Session>(file.readText())
    }.getOrNull()

    private fun save(s: Session) {
        session = s
        runCatching { file.writeText(json.encodeToString(Session.serializer(), s)) }
    }

    // -----------------------------------------------------------------------
    // Connect
    // -----------------------------------------------------------------------

    /**
     * Called when the settings screen opens. A session that is still valid is
     * adopted as it is (no request); an expired one is refreshed from the stored
     * cookie, and only a *failed* refresh disconnects — a network blip must not
     * throw the session away.
     */
    fun restore() {
        val s = load() ?: run {
            _state.update { it.copy(connected = false, loading = false) }
            return
        }
        session = s
        if (s.accessToken != null && s.expiresAt > System.currentTimeMillis() + 60_000L) {
            Spotify.accessToken = s.accessToken
            _state.update {
                it.copy(
                    connected = true,
                    accountName = s.accountName.orEmpty(),
                    accountAvatarUrl = s.accountAvatarUrl,
                )
            }
            refresh()
            return
        }
        scope.launch {
            runCatching { connectInternal(s.spDc, s.spKey) }
                .onSuccess { refresh() }
                .onFailure { error ->
                    AppLog.log("spotify", "the stored session expired and could not be refreshed: $error")
                    disconnect()
                    _state.update {
                        it.copy(connected = false, loading = false, error = error.message)
                    }
                }
        }
    }

    fun connect(spDc: String, spKey: String) {
        if (spDc.isBlank()) return
        scope.launch {
            _state.update { it.copy(loading = true, error = null) }
            runCatching { connectInternal(spDc.trim(), spKey.trim()) }
                .onSuccess { refresh() }
                .onFailure { error ->
                    AppLog.log("spotify", "connect failed: $error")
                    _state.update { it.copy(loading = false, error = error.message) }
                }
        }
    }

    /** Turns the cookies into a token, resolves the account and persists both. */
    private suspend fun connectInternal(spDc: String, spKey: String) {
        val token = SpotifyAuth.fetchAccessToken(spDc, spKey).getOrThrow()
        Spotify.accessToken = token.accessToken
        val profile = Spotify.me().getOrNull()
        save(
            Session(
                spDc = spDc,
                spKey = spKey,
                accessToken = token.accessToken,
                expiresAt = token.accessTokenExpirationTimestampMs,
                accountName = profile?.displayName,
                accountAvatarUrl = profile?.images?.firstOrNull()?.url,
            ),
        )
        _state.update {
            it.copy(
                connected = true,
                accountName = profile?.displayName.orEmpty(),
                accountAvatarUrl = profile?.images?.firstOrNull()?.url,
                loading = false,
                error = null,
            )
        }
        AppLog.log("spotify", "connected as '${profile?.displayName.orEmpty()}'")
    }

    fun disconnect() {
        job?.cancel()
        job = null
        _progress.update { null }
        runCatching { file.delete() }
        session = null
        Spotify.accessToken = null
        _state.value = State()
        AppLog.log("spotify", "disconnected — the stored session was removed")
    }

    /**
     * Runs [block], and on a 401 refreshes the token from the stored cookie once
     * and runs it again. Without this every request after the first hour failed
     * with "Token expired" instead of just renewing.
     */
    private suspend fun <T> withTokenRetry(block: suspend () -> T): T =
        runCatching { block() }.getOrElse { error ->
            if ((error as? Spotify.SpotifyException)?.statusCode != 401) throw error
            val s = session ?: throw error
            connectInternal(s.spDc, s.spKey)
            block()
        }

    // -----------------------------------------------------------------------
    // List
    // -----------------------------------------------------------------------

    /**
     * Loads the account: its playlists (every page) and how many Liked Songs
     * there are.
     */
    fun refresh() {
        scope.launch {
            _state.update { it.copy(loading = true, error = null) }
            try {
                val me = withTokenRetry { Spotify.me().getOrThrow() }
                val liked = withTokenRetry { Spotify.likedSongs(limit = 1, offset = 0).getOrThrow() }

                val playlists = mutableListOf<SpotifyPlaylist>()
                var offset = 0
                val limit = 50
                while (true) {
                    val page = withTokenRetry {
                        Spotify.myPlaylists(limit = limit, offset = offset).getOrThrow()
                    }
                    if (page.items.isEmpty()) break
                    playlists.addAll(page.items)
                    offset += limit
                    if (offset >= page.total) break
                }

                _state.update {
                    it.copy(
                        connected = true,
                        accountName = me.displayName.orEmpty(),
                        accountAvatarUrl = me.images.firstOrNull()?.url,
                        playlists = playlists,
                        likedSongsCount = liked.total,
                        loading = false,
                    )
                }
                AppLog.log(
                    "spotify",
                    "library loaded: ${playlists.size} playlist(s), ${liked.total} liked song(s)",
                )
            } catch (e: Exception) {
                AppLog.log("spotify", "library load failed: $e")
                _state.update { it.copy(loading = false, error = e.message) }
            }
        }
    }

    // -----------------------------------------------------------------------
    // Import
    // -----------------------------------------------------------------------

    /**
     * Imports the selected sources ([LIKED_SOURCE] and/or Spotify playlist ids),
     * reporting progress through [progress].
     *
     * Matching runs a few tracks at a time — the phone uses four — because the
     * search is what takes the time here and a big playlist must not hammer
     * YouTube Music's endpoint.
     */
    fun import(selected: List<String>) {
        job?.cancel()
        job = scope.launch {
            _progress.update { null }
            try {
                var processed = 0
                for (source in selected) {
                    val data = sourceData(source) ?: continue
                    processed++
                    _progress.update { Progress(data.name, 0, data.tracks.size) }
                    val matched = if (data.tracks.isEmpty()) emptyList() else matchTracks(data)
                    PlaylistStore.upsert(
                        id = localId(data.spotifyId),
                        name = data.name,
                        songs = matched,
                    )
                    AppLog.log(
                        "spotify",
                        "'${data.name}': ${matched.size} of ${data.tracks.size} track(s) " +
                            "matched and written to the local playlist",
                    )
                    _progress.update {
                        Progress(data.name, matched.size, data.tracks.size, finished = true)
                    }
                }
                AppLog.log("spotify", "import finished: $processed source(s) processed")
            } catch (e: Exception) {
                if (e !is kotlinx.coroutines.CancellationException) {
                    AppLog.log("spotify", "import failed: $e")
                    _state.update { it.copy(error = e.message) }
                }
            } finally {
                if (_progress.value?.finished != true) _progress.update { null }
            }
        }
    }

    fun cancelImport() {
        job?.cancel()
        job = null
        _progress.update { null }
    }

    fun dismissProgress() {
        if (_progress.value?.finished == true) _progress.update { null }
    }

    fun dismissError() {
        _state.update { it.copy(error = null) }
    }

    private data class Source(
        val spotifyId: String,
        val name: String,
        val tracks: List<SpotifyTrack>,
    )

    private suspend fun sourceData(source: String): Source? {
        if (source == LIKED_SOURCE) {
            return Source(
                spotifyId = "LIKEDSONGS",
                name = "Liked Songs",
                tracks = pagedTracks(limit = 50) { offset, limit ->
                    withTokenRetry { Spotify.likedSongs(limit = limit, offset = offset).getOrThrow() }
                        .let { it.items.map { saved -> saved.track } to it.total }
                },
            )
        }
        val playlist = _state.value.playlists.firstOrNull { it.id == source } ?: return null
        return Source(
            spotifyId = playlist.id,
            name = playlist.name,
            tracks = pagedTracks(limit = 100) { offset, limit ->
                withTokenRetry {
                    Spotify.playlistTracks(playlist.id, limit = limit, offset = offset).getOrThrow()
                }.let { page -> page.items.mapNotNull { it.track } to page.total }
            },
        )
    }

    /**
     * Walks a paged Spotify source to its end.
     *
     * The reported total decides when to stop, and an empty page is the
     * backstop: the API's `total` and what it actually returns are not always in
     * step, and a loop that trusted the total alone could spin forever on a
     * source it cannot read.
     */
    private suspend fun pagedTracks(
        limit: Int,
        page: suspend (offset: Int, limit: Int) -> Pair<List<SpotifyTrack>, Int>,
    ): List<SpotifyTrack> {
        val out = mutableListOf<SpotifyTrack>()
        var offset = 0
        while (true) {
            val (items, total) = page(offset, limit)
            if (items.isEmpty()) break
            out.addAll(items)
            offset += limit
            if (offset >= total) break
        }
        return out
    }

    /**
     * Searches YouTube Music for every Spotify track — bounded parallelism — and
     * keeps each best match. A track nothing matched is skipped; the progress
     * counters still account for it, so the result says how many were lost.
     */
    private suspend fun matchTracks(data: Source): List<SyncedSong> = coroutineScope {
        val semaphore = Semaphore(4)
        val done = AtomicInteger(0)
        val total = data.tracks.size
        data.tracks.map { track ->
            async {
                val result = semaphore.withPermit { searchBestMatch(track) }
                _progress.update { Progress(data.name, done.incrementAndGet(), total) }
                result
            }
        }.awaitAll().filterNotNull()
    }

    private suspend fun searchBestMatch(track: SpotifyTrack): SyncedSong? = runCatching {
        val artist = track.artists.joinToString(" ") { it.name }
        val query = if (artist.isBlank()) track.name else "$artist ${track.name}"
        val candidates = YouTube.search(query, YouTube.SearchFilter.FILTER_SONG)
            .getOrNull()
            ?.items
            ?.filterIsInstance<SongItem>()
            ?.distinctBy { it.id }
            .orEmpty()
        val best = candidates.maxByOrNull { candidate ->
            SpotifyMapper.matchScore(
                spotifyTitle = track.name,
                spotifyArtist = artist,
                spotifyDurationMs = track.durationMs,
                candidateTitle = candidate.title,
                candidateArtist = candidate.artists.joinToString(" ") { it.name },
                candidateDurationSec = candidate.duration,
            )
        } ?: return null
        SyncedSong(
            id = best.id,
            title = best.title,
            artist = best.artists.joinToString(", ") { it.name },
            thumbnail = best.thumbnail,
        )
    }.getOrNull()
}
