package com.music.vivi.desktop.presentation.screens.settings

import com.music.innertube.YouTube
import com.music.innertube.models.SongItem
import com.music.spotify.Spotify
import com.music.spotify.SpotifyAuth
import com.music.spotify.SpotifyMapper
import com.music.vivi.desktop.data.youtube.YouTubePlaylistsRepository
import com.music.vivi.desktop.utils.DesktopPathProvider
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
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
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.io.File

/**
 * The 1.55 Spotify import, using the same [SpotifyAuth]/[Spotify] client the
 * 1.55 screen used and the account-playlist repository the 1.56 app already
 * owns as its destination. Every matched track is searched with
 * [SpotifyMapper.matchScore], exactly as before.
 */
object Spotify155 {
    private val json = Json { ignoreUnknownKeys = true; prettyPrint = true }
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val file = File(DesktopPathProvider.syncStateFile.parentFile, "spotify.json")

    @Serializable
    private data class Session(
        val spDc: String = "",
        val spKey: String = "",
        val accessToken: String? = null,
        val expiresAt: Long = 0L,
        val accountName: String? = null,
    )

    data class Playlist(val id: String, val name: String)

    data class State(
        val connected: Boolean = false,
        val accountName: String = "",
        val playlists: List<Playlist> = emptyList(),
        val likedSongsCount: Int = 0,
        val loading: Boolean = false,
        val error: String? = null,
        val imported: Int = 0,
    )

    private val _state = MutableStateFlow(State())
    val state: StateFlow<State> = _state.asStateFlow()

    private var session: Session? = null
    private var job: Job? = null
    private var refreshJob: Job? = null

    /** The source id the screen draws for the account's liked songs. */
    const val LIKED_SOURCE: String = "liked"

    fun restore() {
        val saved = runCatching {
            if (file.exists()) json.decodeFromString<Session>(file.readText()) else null
        }.getOrNull() ?: return
        session = saved
        if (saved.accessToken != null && saved.expiresAt > System.currentTimeMillis() + 60_000L) {
            Spotify.accessToken = saved.accessToken
            _state.update { it.copy(connected = true, accountName = saved.accountName.orEmpty()) }
            refresh()
        } else {
            connect(saved.spDc, saved.spKey)
        }
    }

    fun connect(spDc: String, spKey: String) {
        if (spDc.isBlank()) return
        job?.cancel()
        job = scope.launch {
            _state.update { it.copy(loading = true, error = null) }
            runCatching {
                val token = SpotifyAuth.fetchAccessToken(spDc.trim(), spKey.trim()).getOrThrow()
                Spotify.accessToken = token.accessToken
                val me = Spotify.me().getOrThrow()
                val saved = Session(
                    spDc = spDc.trim(),
                    spKey = spKey.trim(),
                    accessToken = token.accessToken,
                    expiresAt = token.accessTokenExpirationTimestampMs,
                    accountName = me.displayName,
                )
                session = saved
                file.parentFile?.mkdirs()
                file.writeText(json.encodeToString(saved))
                _state.update {
                    it.copy(connected = true, accountName = me.displayName.orEmpty(), loading = false)
                }
                refresh()
            }.onFailure { error ->
                _state.update { it.copy(loading = false, error = error.message ?: "Spotify sign-in failed.") }
            }
        }
    }

    fun refresh() {
        if (session == null) return
        refreshJob?.cancel()
        refreshJob = scope.launch {
            _state.update { it.copy(loading = true, error = null) }
            runCatching {
                val me = withTokenRetry { Spotify.me().getOrThrow() }
                val liked = withTokenRetry { Spotify.likedSongs(limit = 1, offset = 0).getOrThrow() }
                val playlists = mutableListOf<Playlist>()
                var offset = 0
                while (true) {
                    val page = withTokenRetry { Spotify.myPlaylists(50, offset).getOrThrow() }
                    if (page.items.isEmpty()) break
                    playlists += page.items.map { Playlist(it.id, it.name) }
                    offset += 50
                    if (offset >= page.total) break
                }
                _state.update {
                    it.copy(
                        connected = true,
                        accountName = me.displayName.orEmpty(),
                        playlists = playlists,
                        likedSongsCount = liked.total,
                        loading = false,
                    )
                }
            }.onFailure { error ->
                _state.update { it.copy(loading = false, error = error.message) }
            }
        }
    }

    fun import(selected: List<String>) {
        if (session == null || selected.isEmpty()) return
        job?.cancel()
        job = scope.launch {
            _state.update { it.copy(loading = true, error = null, imported = 0) }
            runCatching {
                val repo = YouTubePlaylistsRepository()
                var written = 0
                selected.forEach { id ->
                    val isLiked = id == LIKED_SOURCE
                    val source = _state.value.playlists.firstOrNull { it.id == id }
                    if (!isLiked && source == null) return@forEach
                    val tracks = pagedTracks(100) { offset, limit ->
                        if (isLiked) {
                            withTokenRetry { Spotify.likedSongs(limit, offset).getOrThrow() }
                                .let { page -> page.items.map { it.track } to page.total }
                        } else {
                            withTokenRetry { Spotify.playlistTracks(id, limit, offset).getOrThrow() }
                                .let { page -> page.items.mapNotNull { it.track } to page.total }
                        }
                    }
                    val matched = matchTracks(tracks)
                    if (matched.isNotEmpty()) {
                        val name = if (isLiked) "Liked Songs (Spotify)" else "${source!!.name} (Spotify)"
                        val existing = repo.load().getOrNull()
                            ?.firstOrNull { it.title.equals(name, ignoreCase = true) }
                        val target = existing?.id ?: repo.create(name).getOrThrow()
                        matched.forEach { repo.addSongIfAbsent(target, it) }
                        written += matched.size
                    }
                }
                _state.update { it.copy(loading = false, imported = written) }
            }.onFailure { error ->
                _state.update { it.copy(loading = false, error = error.message ?: "The import failed.") }
            }
        }
    }

    fun disconnect() {
        job?.cancel()
        job = null
        refreshJob?.cancel()
        refreshJob = null
        session = null
        Spotify.accessToken = null
        runCatching { file.delete() }
        _state.value = State()
    }

    fun dismissError() = _state.update { it.copy(error = null) }

    private suspend fun <T> withTokenRetry(block: suspend () -> T): T = runCatching { block() }.getOrElse { error ->
        if ((error as? Spotify.SpotifyException)?.statusCode != 401) throw error
        val saved = session ?: throw error
        val token = SpotifyAuth.fetchAccessToken(saved.spDc, saved.spKey).getOrThrow()
        Spotify.accessToken = token.accessToken
        block()
    }

    private suspend fun pagedTracks(
        limit: Int,
        page: suspend (offset: Int, limit: Int) -> Pair<List<com.music.spotify.models.SpotifyTrack>, Int>,
    ): List<com.music.spotify.models.SpotifyTrack> {
        val out = mutableListOf<com.music.spotify.models.SpotifyTrack>()
        var offset = 0
        while (true) {
            val (items, total) = page(offset, limit)
            if (items.isEmpty()) break
            out += items
            offset += limit
            if (offset >= total) break
        }
        return out
    }

    private suspend fun matchTracks(tracks: List<com.music.spotify.models.SpotifyTrack>): List<String> = coroutineScope {
        val gate = Semaphore(4)
        tracks.map { track ->
            async {
                gate.withPermit {
                    runCatching {
                        val artist = track.artists.joinToString(" ") { it.name }
                        val query = if (artist.isBlank()) track.name else "$artist ${track.name}"
                        YouTube.search(query, YouTube.SearchFilter.FILTER_SONG)
                            .getOrNull()
                            ?.items
                            ?.filterIsInstance<SongItem>()
                            ?.distinctBy { it.id }
                            .orEmpty()
                            .maxByOrNull { candidate ->
                                SpotifyMapper.matchScore(
                                    spotifyTitle = track.name,
                                    spotifyArtist = artist,
                                    spotifyDurationMs = track.durationMs,
                                    candidateTitle = candidate.title,
                                    candidateArtist = candidate.artists.joinToString(" ") { it.name },
                                    candidateDurationSec = candidate.duration,
                                )
                            }
                            ?.id
                    }.getOrNull()
                }
            }
        }.awaitAll().filterNotNull()
    }
}
