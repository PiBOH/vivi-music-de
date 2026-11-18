package com.music.vivi.desktop.data.store

import com.music.vivi.desktop.data.model.ArtistRef
import com.music.vivi.desktop.data.model.PlaybackEvent
import com.music.vivi.desktop.data.model.Playlist
import com.music.vivi.desktop.data.model.Song
import com.music.vivi.desktop.data.network.listenbrainz.model.ListenBrainzPendingListen
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.builtins.serializer
import kotlinx.serialization.json.Json
import java.io.File

/**
 * Reads the library out of the file the previous build wrote (`vivimusicde.db`).
 *
 * No engine is linked: [EmbeddedTableReader] parses the file itself. Only the
 * tables this app owns are read, and the file is never modified or deleted: it
 * stays on disk as the user's own backup. A file that cannot be read yields
 * `null`, and the import is attempted again on the next start while the JSON
 * store is still empty.
 */
internal object LegacyLibraryImport {

    private val json = Json { ignoreUnknownKeys = true }
    private val stringListSerializer = ListSerializer(String.serializer())
    private val artistRefListSerializer = ListSerializer(ArtistRef.serializer())

    fun read(file: File): LocalLibraryState? {
        if (!file.isFile || file.length() < 512L) return null
        return try {
            val tables = EmbeddedTableReader.read(file).associateBy { it.name }
            map(tables)
        } catch (error: Exception) {
            System.err.println("[store] the old library at ${file.absolutePath} could not be read: $error")
            null
        }
    }

    private fun map(tables: Map<String, EmbeddedTable>): LocalLibraryState {
        val artistsById = LinkedHashMap<Long, StoredArtist>()
        tables["artists"]?.rows?.forEach { row ->
            val table = tables.getValue("artists")
            val id = table.long(row, "id") ?: return@forEach
            artistsById[id] = StoredArtist(
                id = id,
                name = table.text(row, "name").orEmpty(),
                imageUrl = table.text(row, "image_url"),
                customImageUri = table.text(row, "custom_image_uri")
            )
        }

        val refsBySong = LinkedHashMap<String, MutableList<ArtistRef>>()
        tables["song_artists"]?.let { table ->
            table.rows.forEach { row ->
                val songId = table.text(row, "song_id") ?: return@forEach
                val artistId = table.long(row, "artist_id") ?: return@forEach
                refsBySong.getOrPut(songId) { mutableListOf() }.add(
                    ArtistRef(
                        id = artistId,
                        name = artistsById[artistId]?.name.orEmpty(),
                        isPrimary = table.long(row, "is_primary") == 1L
                    )
                )
            }
        }

        val songs = tables["songs"]?.let { table ->
            table.rows.mapNotNull { row -> song(table, row, refsBySong) }
        }.orEmpty()

        val favorites = tables["favorites"]?.let { table ->
            table.rows.mapNotNull { row ->
                val songId = table.text(row, "song_id") ?: return@mapNotNull null
                StoredFavorite(songId = songId, dateAdded = table.long(row, "date_added") ?: 0L)
            }.toMutableList()
        } ?: mutableListOf()
        val favoriteIds = favorites.mapTo(mutableSetOf()) { it.songId }
        for (song in songs) {
            if (song.isFavorite && favoriteIds.add(song.id)) {
                favorites.add(StoredFavorite(songId = song.id, dateAdded = song.dateAdded))
            }
        }

        val playlists = tables["playlists"]?.let { table ->
            table.rows.mapNotNull { row -> playlist(table, row) }
        }.orEmpty()

        val lyrics = tables["lyrics_cache"]?.let { table ->
            table.rows.mapNotNull { row ->
                val songId = table.text(row, "song_id") ?: return@mapNotNull null
                StoredLyrics(
                    songId = songId,
                    plainJson = table.text(row, "plain_lyrics"),
                    syncedJson = table.text(row, "synced_lyrics_json"),
                    isFromRemote = table.long(row, "is_from_remote") == 1L,
                    lastUpdated = table.long(row, "last_updated") ?: 0L
                )
            }
        }.orEmpty()

        val events = tables["playback_events"]?.let { table ->
            table.rows.mapNotNull { row ->
                PlaybackEvent(
                    id = table.long(row, "id") ?: 0L,
                    songId = table.text(row, "song_id") ?: return@mapNotNull null,
                    timestamp = table.long(row, "timestamp") ?: 0L,
                    durationMs = table.long(row, "duration_ms") ?: 0L
                )
            }
        }.orEmpty()

        val listens = tables["listenbrainz_pending_listens"]?.let { table ->
            table.rows.mapNotNull { row ->
                ListenBrainzPendingListen(
                    id = table.long(row, "id") ?: 0L,
                    listenedAtMs = table.long(row, "listened_at_ms") ?: 0L,
                    trackName = table.text(row, "track_name").orEmpty(),
                    artistName = table.text(row, "artist_name").orEmpty(),
                    releaseName = table.text(row, "release_name"),
                    durationMs = table.long(row, "duration_ms"),
                    recordingMbid = table.text(row, "recording_mbid"),
                    source = table.text(row, "source") ?: "LOCAL",
                    createdAtMs = table.long(row, "created_at_ms") ?: 0L
                )
            }
        }.orEmpty()

        for (ref in refsBySong.values.flatten()) {
            if (ref.name.isBlank()) continue
            artistsById.getOrPut(ref.id) { StoredArtist(id = ref.id, name = ref.name) }
        }
        for (song in songs) {
            for (ref in song.resolvedArtists) {
                if (ref.name.isBlank()) continue
                artistsById.getOrPut(ref.id) { StoredArtist(id = ref.id, name = ref.name) }
            }
        }

        return LocalLibraryState(
            songs = songs,
            favorites = favorites,
            playlists = playlists,
            lyrics = lyrics,
            playbackEvents = events.sortedBy { it.timestamp },
            pendingListens = listens.sortedBy { it.createdAtMs },
            artists = artistsById.values.sortedBy { it.name.lowercase() },
            nextEventId = (events.maxOfOrNull { it.id } ?: 0L) + 1L,
            nextListenId = (listens.maxOfOrNull { it.id } ?: 0L) + 1L
        )
    }

    private fun song(
        table: EmbeddedTable,
        row: List<Any?>,
        refsBySong: Map<String, List<ArtistRef>>
    ): Song? {
        val id = table.text(row, "id") ?: return null
        val declared = table.text(row, "artists_json")?.let { raw ->
            runCatching { json.decodeFromString(artistRefListSerializer, raw) }.getOrNull()
        }.orEmpty()
        val artists = when {
            declared.isNotEmpty() -> declared
            refsBySong[id].orEmpty().isNotEmpty() -> refsBySong.getValue(id)
            else -> emptyList()
        }
        return Song(
            id = id,
            title = table.text(row, "title").orEmpty(),
            artist = table.text(row, "artist").orEmpty(),
            artistId = table.long(row, "artist_id") ?: 0L,
            artists = artists,
            album = table.text(row, "album").orEmpty(),
            albumId = table.long(row, "album_id") ?: 0L,
            albumArtist = table.text(row, "album_artist"),
            path = table.text(row, "path").orEmpty(),
            contentUriString = table.text(row, "content_uri_string").orEmpty(),
            albumArtUriString = table.text(row, "album_art_uri_string"),
            duration = table.long(row, "duration") ?: 0L,
            genre = table.text(row, "genre"),
            lyrics = table.text(row, "lyrics"),
            isFavorite = table.long(row, "is_favorite") == 1L,
            trackNumber = (table.long(row, "track_number") ?: 0L).toInt(),
            discNumber = table.long(row, "disc_number")?.toInt(),
            year = (table.long(row, "year") ?: 0L).toInt(),
            dateAdded = table.long(row, "date_added") ?: 0L,
            dateModified = table.long(row, "date_modified") ?: 0L,
            mimeType = table.text(row, "mime_type"),
            bitrate = table.long(row, "bitrate")?.toInt(),
            sampleRate = table.long(row, "sample_rate")?.toInt(),
            navidromeId = table.text(row, "navidrome_id"),
            jellyfinId = table.text(row, "jellyfin_id"),
            playCount = (table.long(row, "play_count") ?: 0L).toInt(),
            lastPlayedTimestamp = table.long(row, "last_played_timestamp") ?: 0L
        )
    }

    private fun playlist(table: EmbeddedTable, row: List<Any?>): Playlist? {
        val id = table.text(row, "id") ?: return null
        val songIds = table.text(row, "song_ids_json")?.let { raw ->
            runCatching { json.decodeFromString(stringListSerializer, raw) }.getOrNull()
        }.orEmpty()
        return Playlist(
            id = id,
            name = table.text(row, "name").orEmpty(),
            songIds = songIds,
            createdAt = table.long(row, "created_at") ?: 0L,
            lastModified = table.long(row, "last_modified") ?: 0L,
            isQueueGenerated = table.long(row, "is_queue_generated") == 1L,
            coverImageUri = table.text(row, "cover_image_uri"),
            coverColorArgb = table.long(row, "cover_color_argb")?.toInt(),
            coverIconName = table.text(row, "cover_icon_name"),
            coverShapeType = table.text(row, "cover_shape_type"),
            coverShapeDetail1 = table.float(row, "cover_shape_detail1"),
            coverShapeDetail2 = table.float(row, "cover_shape_detail2"),
            coverShapeDetail3 = table.float(row, "cover_shape_detail3"),
            coverShapeDetail4 = table.float(row, "cover_shape_detail4"),
            source = table.text(row, "source") ?: "LOCAL"
        )
    }

    private fun EmbeddedTable.float(row: List<Any?>, column: String): Float? {
        val index = columns.indexOfFirst { it.equals(column, ignoreCase = true) }
        if (index < 0) return null
        return when (val value = row.getOrNull(index)) {
            is Double -> value.toFloat()
            is Long -> value.toFloat()
            else -> null
        }
    }
}
