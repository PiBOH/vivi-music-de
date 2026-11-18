package com.music.vivi.desktop.data.database

import java.io.File
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import com.music.vivi.desktop.data.model.Album
import com.music.vivi.desktop.data.model.Artist
import com.music.vivi.desktop.data.model.Genre
import com.music.vivi.desktop.data.model.PlaybackEvent
import com.music.vivi.desktop.data.model.SearchFilterType
import com.music.vivi.desktop.data.model.SearchResultItem
import com.music.vivi.desktop.data.model.Song
import com.music.vivi.desktop.data.model.SortOption
import com.music.vivi.desktop.data.store.LocalLibraryStore
import com.music.vivi.desktop.data.store.StoredArtist

/**
 * The library queries, on the JSON store.
 *
 * Every method keeps the signature the desktop already calls, so the repository
 * and the screens are unchanged. The set operations below are the SQL the
 * previous build ran: the same filters, the same groupings, the same ordering
 * keys (`NOCASE` is a lowercase comparison) and the same limits.
 */
class MusicDao(
    private val store: LocalLibraryStore,
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO
) {

    suspend fun getAllSongs(
        sortOption: SortOption = SortOption.SongTitleAZ,
        minDurationMs: Long = 0L
    ): List<Song> = withContext(ioDispatcher) {
        store.load().songs
            .filter { minDurationMs <= 0L || it.duration >= minDurationMs }
            .sortedWith(songComparator(sortOption))
    }

    suspend fun hasSongs(): Boolean = withContext(ioDispatcher) {
        store.load().songs.isNotEmpty()
    }

    suspend fun getSongById(id: String): Song? = withContext(ioDispatcher) {
        store.load().songs.firstOrNull { it.id == id }
    }

    suspend fun getSongByPath(path: String): Song? = withContext(ioDispatcher) {
        store.load().songs.firstOrNull { it.path == path }
    }

    suspend fun getSongsByIds(ids: List<String>): List<Song> = withContext(ioDispatcher) {
        if (ids.isEmpty()) return@withContext emptyList()
        val byId = store.load().songs.associateBy { it.id }
        ids.distinct().mapNotNull { byId[it] }
    }

    suspend fun upsertSongs(songs: List<Song>) = withContext(ioDispatcher) {
        if (songs.isEmpty()) return@withContext
        store.update("songs", "artists") { state ->
            val byPath = LinkedHashMap<String, Song>()
            for (song in state.songs) byPath[song.path] = song
            for (incoming in songs) {
                // The previous schema keyed a song by its path: a re-scan of the
                // same file keeps the row it already had (the id, the likes and
                // the play counters) and refreshes the metadata.
                val existing = byPath[incoming.path]
                val merged = when {
                    existing != null -> incoming.copy(
                        id = existing.id,
                        isFavorite = existing.isFavorite,
                        playCount = existing.playCount,
                        lastPlayedTimestamp = existing.lastPlayedTimestamp,
                        dateAdded = existing.dateAdded
                    )
                    else -> {
                        // A new path carrying a known id replaces that row rather
                        // than duplicating an id the rest of the store treats as
                        // unique.
                        byPath.entries.removeAll { it.value.id == incoming.id }
                        incoming
                    }
                }
                byPath[incoming.path] = merged
            }
            val updatedSongs = byPath.values.toList()

            val artists = LinkedHashMap<Long, StoredArtist>()
            for (artist in state.artists) artists[artist.id] = artist
            val referenced = HashSet<Long>()
            for (song in updatedSongs) {
                for (ref in song.resolvedArtists) {
                    if (ref.name.isBlank()) continue
                    referenced.add(ref.id)
                    val known = artists[ref.id]
                    artists[ref.id] = if (known == null || known.name.isBlank()) {
                        StoredArtist(id = ref.id, name = ref.name, imageUrl = known?.imageUrl, customImageUri = known?.customImageUri)
                    } else {
                        known
                    }
                }
            }
            state.copy(
                songs = updatedSongs,
                artists = artists.values.filter { it.id in referenced }.sortedBy { it.name.lowercase() }
            )
        }
    }

    suspend fun updateSongPath(id: String, newPath: String, newContentUri: String) = withContext(ioDispatcher) {
        store.update("songs") { state ->
            state.copy(
                songs = state.songs.map { song ->
                    if (song.id == id) {
                        song.copy(path = newPath, contentUriString = newContentUri)
                    } else {
                        song
                    }
                }
            )
        }
    }

    suspend fun deleteSongsNotInPaths(
        existingPaths: Set<String>,
        scannedDirectories: List<String>
    ) = withContext(ioDispatcher) {
        if (scannedDirectories.isEmpty()) return@withContext
        val prefixes = scannedDirectories.map { directory ->
            val absolute = File(directory).absolutePath
            if (absolute.endsWith(File.separator)) absolute else absolute + File.separator
        }
        val doomed = store.load().songs.filter { song ->
            song.navidromeId == null && song.jellyfinId == null &&
                prefixes.any { song.path.startsWith(it) } &&
                song.path !in existingPaths
        }
        if (doomed.isEmpty()) return@withContext
        removeSongs(doomed)
    }

    suspend fun deleteSongsByPaths(paths: Collection<String>) = withContext(ioDispatcher) {
        if (paths.isEmpty()) return@withContext
        val wanted = paths.toSet()
        val doomed = store.load().songs.filter {
            it.path in wanted && it.navidromeId == null && it.jellyfinId == null
        }
        if (doomed.isEmpty()) return@withContext
        removeSongs(doomed)
    }

    suspend fun deleteSongsInDirectory(directoryPath: String) = withContext(ioDispatcher) {
        val absolute = File(directoryPath).absolutePath
        val prefix = if (absolute.endsWith(File.separator)) absolute else absolute + File.separator
        val doomed = store.load().songs.filter {
            it.path.startsWith(prefix) && it.navidromeId == null && it.jellyfinId == null
        }
        if (doomed.isEmpty()) return@withContext
        removeSongs(doomed)
    }

    suspend fun clearNavidromeSongs() = withContext(ioDispatcher) {
        val doomed = store.load().songs.filter { it.navidromeId != null }
        if (doomed.isEmpty()) return@withContext
        removeSongs(doomed)
    }

    suspend fun clearJellyfinSongs() = withContext(ioDispatcher) {
        val doomed = store.load().songs.filter { it.jellyfinId != null }
        if (doomed.isEmpty()) return@withContext
        removeSongs(doomed)
    }

    suspend fun deleteSong(song: Song) = withContext(ioDispatcher) {
        runCatching {
            val file = File(song.path)
            if (file.exists()) file.delete()
        }
        removeSongs(listOf(song))
    }

    suspend fun getAllAlbums(
        sortOption: SortOption = SortOption.AlbumTitleAZ,
        minTracks: Int = 1,
        minDurationMs: Long = 0L
    ): List<Album> = withContext(ioDispatcher) {
        val songs = store.load().songs.filter {
            it.album.isNotBlank() && (minDurationMs <= 0L || it.duration >= minDurationMs)
        }
        albumsOf(songs)
            .filter { it.songCount >= minTracks }
            .sortedWith(albumComparator(sortOption))
    }

    suspend fun getAlbumById(albumId: Long, minDurationMs: Long = 0L): Album? = withContext(ioDispatcher) {
        val songs = store.load().songs.filter {
            it.albumId == albumId && (minDurationMs <= 0L || it.duration >= minDurationMs)
        }
        albumsOf(songs).firstOrNull()
    }

    suspend fun getSongsByAlbum(albumId: Long, minDurationMs: Long = 0L): List<Song> = withContext(ioDispatcher) {
        store.load().songs
            .filter { it.albumId == albumId && (minDurationMs <= 0L || it.duration >= minDurationMs) }
            .sortedWith(
                compareBy<Song> { it.discNumber ?: -1 }
                    .thenBy { it.trackNumber }
                    .thenBy { it.title.lowercase() }
            )
    }

    suspend fun getAllArtists(
        sortOption: SortOption = SortOption.ArtistNameAZ,
        minDurationMs: Long = 0L
    ): List<Artist> = withContext(ioDispatcher) {
        val state = store.load()
        val songs = state.songs.filter { minDurationMs <= 0L || it.duration >= minDurationMs }
        val images = state.artists.associateBy { it.id }
        val counts = LinkedHashMap<Long, Int>()
        val names = LinkedHashMap<Long, String>()
        for (song in songs) {
            for (ref in song.resolvedArtists) {
                if (ref.name.isBlank()) continue
                counts[ref.id] = (counts[ref.id] ?: 0) + 1
                if (names[ref.id].isNullOrBlank()) names[ref.id] = ref.name
            }
        }
        val artists = counts.map { (id, count) ->
            val stored = images[id]
            Artist(
                id = id,
                name = stored?.name?.takeIf { it.isNotBlank() } ?: names[id].orEmpty(),
                songCount = count,
                imageUrl = stored?.imageUrl,
                customImageUri = stored?.customImageUri
            )
        }
        artists.sortedWith(artistComparator(sortOption))
    }

    suspend fun getArtistById(artistId: Long, minDurationMs: Long = 0L): Artist? = withContext(ioDispatcher) {
        val state = store.load()
        val songs = state.songs.filter { song ->
            (minDurationMs <= 0L || song.duration >= minDurationMs) &&
                (song.artistId == artistId || song.resolvedArtists.any { it.id == artistId })
        }
        if (songs.isEmpty()) return@withContext null
        val stored = state.artists.firstOrNull { it.id == artistId }
        Artist(
            id = artistId,
            name = stored?.name?.takeIf { it.isNotBlank() } ?: songs.first().artist,
            songCount = songs.size,
            imageUrl = stored?.imageUrl,
            customImageUri = stored?.customImageUri
        )
    }

    suspend fun updateArtistCustomImage(artistId: Long, customImageUri: String?) = withContext(ioDispatcher) {
        store.update("artists") { state ->
            val known = state.artists.firstOrNull { it.id == artistId }
            val name = known?.name?.takeIf { it.isNotBlank() }
                ?: state.songs.firstOrNull { it.artistId == artistId }?.artist
                ?: return@update state
            val updated = StoredArtist(
                id = artistId,
                name = name,
                imageUrl = known?.imageUrl,
                customImageUri = customImageUri
            )
            state.copy(
                artists = (state.artists.filterNot { it.id == artistId } + updated)
                    .sortedBy { it.name.lowercase() }
            )
        }
    }

    suspend fun getSongsByArtist(artistId: Long, minDurationMs: Long = 0L): List<Song> = withContext(ioDispatcher) {
        store.load().songs
            .filter {
                (minDurationMs <= 0L || it.duration >= minDurationMs) &&
                    it.resolvedArtists.any { ref -> ref.id == artistId }
            }
            .sortedWith(
                compareByDescending<Song> { it.year }
                    .thenBy { it.album.lowercase() }
                    .thenBy { it.trackNumber }
            )
    }

    suspend fun getAllGenres(): List<Genre> = withContext(ioDispatcher) {
        store.load().songs
            .mapNotNull { it.genre }
            .filter { it.isNotBlank() }
            .distinct()
            .sortedBy { it.lowercase() }
            .map { Genre(id = it, name = it) }
    }

    suspend fun getSongsByGenre(genreName: String): List<Song> = withContext(ioDispatcher) {
        store.load().songs
            .filter { it.genre == genreName }
            .sortedBy { it.title.lowercase() }
    }

    suspend fun recordSongPlayed(songId: String, durationMs: Long = 0L) = withContext(ioDispatcher) {
        val now = System.currentTimeMillis()
        store.update("recently_played", "playback_events", "songs") { state ->
            state.copy(
                songs = state.songs.map { song ->
                    if (song.id == songId) {
                        song.copy(playCount = song.playCount + 1, lastPlayedTimestamp = now)
                    } else {
                        song
                    }
                },
                playbackEvents = state.playbackEvents + PlaybackEvent(
                    id = state.nextEventId,
                    songId = songId,
                    timestamp = now,
                    durationMs = durationMs
                ),
                nextEventId = state.nextEventId + 1L
            )
        }
        Unit
    }

    suspend fun getPlaybackEvents(limit: Int = 500): List<PlaybackEvent> = withContext(ioDispatcher) {
        store.load().playbackEvents
            .sortedByDescending { it.timestamp }
            .take(limit)
    }

    suspend fun getPlaybackEventsBetween(start: Long, end: Long): List<PlaybackEvent> = withContext(ioDispatcher) {
        store.load().playbackEvents
            .filter { it.timestamp in start..end }
            .sortedByDescending { it.timestamp }
    }

    suspend fun getRecentlyPlayedSongs(limit: Int = 500): List<Song> = withContext(ioDispatcher) {
        store.load().songs
            .filter { it.lastPlayedTimestamp > 0L }
            .sortedByDescending { it.lastPlayedTimestamp }
            .take(limit)
    }

    suspend fun search(
        query: String,
        filter: SearchFilterType = SearchFilterType.ALL,
        minDurationMs: Long = 0L,
        minTracks: Int = 1
    ): List<SearchResultItem> = withContext(ioDispatcher) {
        if (query.isBlank()) return@withContext emptyList()
        val needle = query.lowercase()
        val state = store.load()
        val results = mutableListOf<SearchResultItem>()

        if (filter == SearchFilterType.ALL || filter == SearchFilterType.SONGS) {
            val matches = state.songs.asSequence()
                .filter { minDurationMs <= 0L || it.duration >= minDurationMs }
                .filter {
                    it.title.lowercase().contains(needle) ||
                        it.artist.lowercase().contains(needle) ||
                        it.album.lowercase().contains(needle)
                }
                .take(50)
                .map { SearchResultItem.SongItem(it) }
                .toList()
            results.addAll(matches)
        }

        if (filter == SearchFilterType.ALL || filter == SearchFilterType.ALBUMS) {
            val matchingSongs = state.songs.filter { it.album.lowercase().contains(needle) }
            results.addAll(
                albumsOf(matchingSongs)
                    .filter { it.songCount >= minTracks }
                    .take(20)
                    .map { SearchResultItem.AlbumItem(it) }
            )
        }

        if (filter == SearchFilterType.ALL || filter == SearchFilterType.ARTISTS) {
            val images = state.artists.associateBy { it.id }
            val counts = LinkedHashMap<Long, Int>()
            val names = LinkedHashMap<Long, String>()
            for (song in state.songs) {
                for (ref in song.resolvedArtists) {
                    if (!ref.name.lowercase().contains(needle)) continue
                    counts[ref.id] = (counts[ref.id] ?: 0) + 1
                    if (names[ref.id].isNullOrBlank()) names[ref.id] = ref.name
                }
            }
            results.addAll(
                counts.entries.take(20).map { (id, count) ->
                    SearchResultItem.ArtistItem(
                        Artist(
                            id = id,
                            name = images[id]?.name?.takeIf { it.isNotBlank() } ?: names[id].orEmpty(),
                            songCount = count,
                            imageUrl = images[id]?.imageUrl,
                            customImageUri = images[id]?.customImageUri
                        )
                    )
                }
            )
        }
        results
    }

    suspend fun clearLibrary() = withContext(ioDispatcher) {
        store.update("songs") { state ->
            state.copy(songs = emptyList(), favorites = emptyList(), artists = emptyList())
        }
    }

    /**
     * Removes [doomed] songs and everything that pointed at them, the way the
     * previous schema did: their like and lyrics rows went with the song, the
     * playback events cascaded, and a playlist kept its order minus the ids.
     */
    private suspend fun removeSongs(doomed: List<Song>) {
        val ids = doomed.map { it.id }.toSet()
        val paths = doomed.map { it.path }.toSet()
        store.update("songs", "artists", "favorites", "playlists", "playback_events") { state ->
            val songs = state.songs.filterNot { it.path in paths }
            val referenced = songs.flatMap { song -> song.resolvedArtists.map { it.id } }.toSet()
            state.copy(
                songs = songs,
                favorites = state.favorites.filterNot { it.songId in ids },
                lyrics = state.lyrics.filterNot { it.songId in ids },
                playbackEvents = state.playbackEvents.filterNot { it.songId in ids },
                artists = state.artists.filter { it.id in referenced },
                playlists = state.playlists.map { playlist ->
                    if (playlist.songIds.none { it in ids }) {
                        playlist
                    } else {
                        playlist.copy(songIds = playlist.songIds.filterNot { it in ids })
                    }
                }
            )
        }
    }

    private fun albumsOf(songs: List<Song>): List<Album> {
        val groups = LinkedHashMap<Pair<String, String>, MutableList<Song>>()
        for (song in songs) {
            if (song.album.isBlank()) continue
            groups.getOrPut(song.album to song.artist) { mutableListOf() }.add(song)
        }
        return groups.map { (key, group) ->
            Album(
                id = group.maxOfOrNull { it.albumId } ?: 0L,
                title = key.first,
                artist = key.second,
                albumArtist = group.firstNotNullOfOrNull { it.albumArtist },
                year = group.maxOfOrNull { it.year } ?: 0,
                dateAdded = group.maxOfOrNull { it.dateAdded } ?: 0L,
                albumArtUriString = group.mapNotNull { it.albumArtUriString }.filter { it.isNotBlank() }.maxOrNull(),
                songCount = group.size
            )
        }
    }

    private fun songComparator(option: SortOption): Comparator<Song> = when (option) {
        SortOption.SongTitleZA -> compareBy<Song> { it.title.lowercase() }.reversed()
        SortOption.SongArtist -> compareBy<Song> { it.artist.lowercase() }
            .thenBy { it.album.lowercase() }
            .thenBy { it.trackNumber }
        SortOption.SongArtistDesc -> compareByDescending<Song> { it.artist.lowercase() }
            .thenBy { it.album.lowercase() }
            .thenBy { it.trackNumber }
        SortOption.SongAlbum -> compareBy<Song> { it.album.lowercase() }.thenBy { it.trackNumber }
        SortOption.SongAlbumDesc -> compareByDescending<Song> { it.album.lowercase() }.thenBy { it.trackNumber }
        SortOption.SongDateAdded -> compareByDescending { it.dateAdded }
        SortOption.SongDateAddedAsc -> compareBy { it.dateAdded }
        SortOption.SongDuration -> compareByDescending { it.duration }
        SortOption.SongDurationAsc -> compareBy { it.duration }
        else -> compareBy { it.title.lowercase() }
    }

    private fun albumComparator(option: SortOption): Comparator<Album> = when (option) {
        SortOption.AlbumTitleZA -> compareBy<Album> { it.title.lowercase() }.reversed()
        SortOption.AlbumArtist -> compareBy<Album> { it.artist.lowercase() }.thenBy { it.title.lowercase() }
        SortOption.AlbumArtistDesc -> compareByDescending<Album> { it.artist.lowercase() }
            .thenBy { it.title.lowercase() }
        SortOption.AlbumReleaseYear -> compareByDescending { it.year }
        SortOption.AlbumReleaseYearAsc -> compareBy { it.year }
        SortOption.AlbumDateAdded -> compareByDescending { it.dateAdded }
        SortOption.AlbumSizeAsc -> compareBy { it.songCount }
        SortOption.AlbumSizeDesc -> compareByDescending { it.songCount }
        else -> compareBy { it.title.lowercase() }
    }

    private fun artistComparator(option: SortOption): Comparator<Artist> = when (option) {
        SortOption.ArtistNameZA -> compareBy<Artist> { it.name.lowercase() }.reversed()
        SortOption.ArtistNumSongsDesc -> compareByDescending<Artist> { it.songCount }.thenBy { it.name.lowercase() }
        SortOption.ArtistNumSongsAsc -> compareBy<Artist> { it.songCount }.thenBy { it.name.lowercase() }
        else -> compareBy { it.name.lowercase() }
    }
}
