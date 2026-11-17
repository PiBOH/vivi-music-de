package com.music.vivi.desktop.data.youtube

import com.music.innertube.YouTube
import com.music.innertube.models.SongItem
import com.music.vivi.desktop.data.model.ArtistRef
import com.music.vivi.desktop.data.model.Song
import com.music.vivi.desktop.data.model.generateArtistId
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * The YouTube Music catalogue, as the app's own [Song] model.
 *
 * A YouTube track is addressed with `ytmusic://<videoId>` in `path`: the playback
 * resolver turns that into a googlevideo URL at play time, because those URLs are
 * single-use and expire. Everything else (title, artist, cover, duration) is
 * carried on the [Song] itself, so the clone's rows, queue and player need no
 * special case.
 */
class YouTubeMusicRepository(
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO
) {

    /** Search the catalogue. Throws nothing: a failure is reported as an empty list plus a reason. */
    suspend fun search(query: String): Result<List<Song>> = withContext(ioDispatcher) {
        val trimmed = query.trim()
        if (trimmed.isEmpty()) return@withContext Result.success(emptyList())
        GuestSession.ensure()
        YouTube.search(trimmed, YouTube.SearchFilter.FILTER_SONG)
            .map { page ->
                page.items
                    .filterIsInstance<SongItem>()
                    .map(::toSong)
                    .distinctBy { it.id }
            }
            .onFailure { YtLog.log("search", "\"$trimmed\" failed: ${it.message}") }
    }

    /** Resolves the playable URL for a track, or null when every source failed. */
    suspend fun resolveStreamUrl(videoId: String): String? = resolveStream(videoId)?.url

    /**
     * Resolves the stream together with the client that signed it.
     *
     * The User-Agent travels with the URL because googlevideo expects the one of
     * the client that requested it, so the engine needs the pair, not the URL alone.
     */
    suspend fun resolveStream(videoId: String): StreamResolver.ResolvedStream? {
        val streams = StreamResolver.resolve(videoId)
        return streams.firstOrNull()
    }

    /** Drops a cached resolution so the next play fetches a fresh URL. */
    fun invalidate(videoId: String) = StreamResolver.invalidate(videoId)

    fun videoIdOf(song: Song): String? =
        song.path.removePrefix(VIDEO_SCHEME).takeIf { song.path.startsWith(VIDEO_SCHEME) && it.isNotBlank() }

    private fun toSong(item: SongItem): Song {
        val artists = item.artists.map { artist ->
            ArtistRef(
                id = generateArtistId(artist.name),
                name = artist.name,
                isPrimary = artist === item.artists.first()
            )
        }
        return Song(
            id = "yt:${item.id}",
            title = item.title,
            artist = item.artists.joinToString(", ") { it.name },
            artistId = artists.firstOrNull()?.id ?: 0L,
            artists = artists,
            album = item.album?.name.orEmpty(),
            albumId = item.album?.id?.hashCode()?.toLong() ?: 0L,
            path = "$VIDEO_SCHEME${item.id}",
            contentUriString = "$VIDEO_SCHEME${item.id}",
            albumArtUriString = item.thumbnail,
            duration = (item.duration ?: 0).toLong() * 1000L,
            dateAdded = System.currentTimeMillis()
        )
    }

    companion object {
        const val VIDEO_SCHEME = "ytmusic://"
    }
}
