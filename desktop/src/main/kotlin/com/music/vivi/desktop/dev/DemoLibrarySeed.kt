package com.music.vivi.desktop.dev

import com.music.vivi.desktop.data.model.ArtistRef
import com.music.vivi.desktop.data.model.Playlist
import com.music.vivi.desktop.data.model.Song
import com.music.vivi.desktop.data.store.LocalLibraryState
import com.music.vivi.desktop.data.store.LocalLibraryStore
import com.music.vivi.desktop.data.store.StoredFavorite
import com.music.vivi.desktop.utils.DesktopPathProvider
import java.io.File
import kotlin.system.exitProcess

/**
 * Fills the JSON library with the small Album/Playlist fixture the visual checks
 * use. The fixture is data, not a database: it goes through the same
 * [LocalLibraryStore.save] every user change uses, so the screens read exactly
 * what a real library would give them.
 *
 * Usage: `VIVI_SYNC_STATE_FILE=<fixture>` with
 * `VIVIMusic.exe --seed-demo-library=<directory>`, where the directory holds
 * `covers/random-access-memories.jpg`, `covers/after-hours.jpg` and
 * `covers/late-night-drive.jpg`. The flag refuses to run without
 * `VIVI_SYNC_STATE_FILE`, so an accidental call cannot replace the default
 * store.
 */
object DemoLibrarySeed {

    private const val FLAG_PREFIX = "--seed-demo-library="

    fun handle(args: Array<String>): Boolean {
        val directory = args.firstOrNull { it.startsWith(FLAG_PREFIX) }
            ?.substringAfter(FLAG_PREFIX)
            ?.takeIf { it.isNotBlank() }
            ?: return false
        exitProcess(seed(File(directory)))
    }

    private fun seed(fixtureRoot: File): Int {
        if (System.getenv("VIVI_SYNC_STATE_FILE").isNullOrBlank()) {
            return fail("set VIVI_SYNC_STATE_FILE to the fixture store")
        }
        val root = fixtureRoot.absoluteFile
        val covers = File(root, "covers")
        val randomAccess = cover(covers, "random-access-memories.jpg")
            ?: return fail("random-access-memories.jpg is missing from ${covers.absolutePath}")
        val afterHours = cover(covers, "after-hours.jpg")
            ?: return fail("after-hours.jpg is missing from ${covers.absolutePath}")
        val playlistCover = cover(covers, "late-night-drive.jpg")
            ?: return fail("late-night-drive.jpg is missing from ${covers.absolutePath}")

        val daftPunk = ArtistRef(id = 101L, name = "Daft Punk", isPrimary = true)
        val weeknd = ArtistRef(id = 102L, name = "The Weeknd", isPrimary = true)
        val miles = ArtistRef(id = 103L, name = "Miles Davis", isPrimary = true)

        val songs = listOf(
            song(
                id = "demo-get-lucky", title = "Get Lucky", artist = "Daft Punk", artistRef = daftPunk,
                album = "Random Access Memories", albumId = 101L, track = 1, art = randomAccess
            ),
            song(
                id = "demo-instant-crush", title = "Instant Crush", artist = "Daft Punk", artistRef = daftPunk,
                album = "Random Access Memories", albumId = 101L, track = 2, art = randomAccess
            ),
            song(
                id = "demo-blinding-lights", title = "Blinding Lights", artist = "The Weeknd", artistRef = weeknd,
                album = "After Hours", albumId = 102L, track = 1, art = afterHours
            ),
            song(
                id = "demo-save-your-tears", title = "Save Your Tears", artist = "The Weeknd", artistRef = weeknd,
                album = "After Hours", albumId = 102L, track = 2, art = afterHours
            ),
            // No artwork on purpose: this album exercises AlbumGridItem's placeholder.
            song(
                id = "demo-so-what", title = "So What", artist = "Miles Davis", artistRef = miles,
                album = "Kind of Blue", albumId = 103L, track = 1, art = null, year = 1959
            ),
            song(
                id = "demo-freddie", title = "Freddie Freeloader", artist = "Miles Davis", artistRef = miles,
                album = "Kind of Blue", albumId = 103L, track = 2, art = null, year = 1959
            )
        )

        val now = System.currentTimeMillis()
        val playlists = listOf(
            Playlist(
                id = "demo-late-night",
                name = "Late Night Drive",
                songIds = listOf("demo-get-lucky", "demo-instant-crush"),
                createdAt = now,
                lastModified = now,
                coverImageUri = playlistCover
            ),
            Playlist(
                id = "demo-collage",
                name = "Collage Mix",
                songIds = listOf("demo-blinding-lights", "demo-save-your-tears", "demo-so-what"),
                createdAt = now,
                lastModified = now
            ),
            Playlist(
                id = "demo-empty",
                name = "Empty Queue",
                songIds = emptyList(),
                createdAt = now,
                lastModified = now
            )
        )

        val storeFile = DesktopPathProvider.syncStateFile
        val store = LocalLibraryStore(storeFile, DesktopPathProvider.databaseFile)
        store.save(
            LocalLibraryState(
                songs = songs,
                favorites = listOf(StoredFavorite(songId = "demo-get-lucky", dateAdded = now)),
                playlists = playlists,
                nextEventId = 1L,
                nextListenId = 1L
            )
        )

        val written = store.load()
        println("[demo] file: ${storeFile.absolutePath}")
        println("[demo] songs=${written.songs.size} albums=${written.songs.map { it.album }.distinct().size} playlists=${written.playlists.size}")
        println("[demo] cover album: ${written.songs.first { it.album == "Random Access Memories" }.albumArtUriString}")
        println("[demo] empty album art: ${written.songs.first { it.album == "Kind of Blue" }.albumArtUriString}")
        println("[demo] RESULT: ${if (written.songs.size == 6 && written.playlists.size == 3) "PASS" else "FAIL"}")
        return if (written.songs.size == 6 && written.playlists.size == 3) 0 else 1
    }

    private fun song(
        id: String,
        title: String,
        artist: String,
        artistRef: ArtistRef,
        album: String,
        albumId: Long,
        track: Int,
        art: String?,
        year: Int = 2013
    ) = Song(
        id = id,
        title = title,
        artist = artist,
        artistId = artistRef.id,
        artists = listOf(artistRef),
        album = album,
        albumId = albumId,
        albumArtist = artist,
        path = "demo-library/$album/${track.toString().padStart(2, '0')} - $title.wav",
        albumArtUriString = art,
        duration = 180_000L,
        year = year,
        dateAdded = 1_700_000_000_000L + track,
        mimeType = "audio/wav"
    )

    private fun cover(directory: File, name: String): String? =
        File(directory, name).takeIf { it.isFile }?.absolutePath

    private fun fail(message: String): Int {
        println("[demo] $message")
        println("[demo] RESULT: FAIL")
        return 1
    }
}
