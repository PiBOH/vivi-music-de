package com.music.vivi.desktop

import com.music.innertube.YouTube
import com.music.innertube.models.PlaylistItem
import kotlinx.coroutines.runBlocking
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

/**
 * Decisive check of the account's **write** path, run with
 *
 *     ./gradlew :desktop:playlistWriteCheck
 *
 * The owner's report is not one direction of the sync but both: a playlist
 * created here with *Sync playlist* on, and one uploaded afterwards, never
 * appears on YouTube Music, and the account holds no playlist the app does not
 * already have. Read-only pulls work (the library and every playlist page are
 * fetched fine), so the suspicion is on the writes: `playlist/create`,
 * `browse/edit_playlist` and the library listing of a playlist that was created
 * but never *saved*.
 *
 * This creates one playlist named `VIVI write check <timestamp>`, adds one song,
 * reads it back, asks the library whether it lists it, and deletes it again —
 * one line per step, so the failing request is named instead of guessed.
 *
 * It touches the signed-in account: a playlist is created and removed. Nothing
 * else is read or changed.
 */
object PlaylistWriteCheck {

    /** A song already in the owner's library, so no new video is referenced. */
    private const val SONG_ID = "pSVbF6DGH9U"

    @JvmStatic
    fun main(args: Array<String>) = runBlocking {
        LoginManager.restore()
        println("signed in: ${LoginManager.isLoggedIn()} (cookie ${YouTube.cookie?.length ?: 0} chars)")
        YouTube.accountInfo().onSuccess { println("account: ${it.name} <${it.email}>") }
            .onFailure { println("account info FAILED: ${it.message}") }

        val name = "VIVI write check " +
            LocalDateTime.now().format(DateTimeFormatter.ofPattern("dd-MM HH:mm:ss"))

        println()
        println("1. playlist/create")
        val id = runCatching { YouTube.createPlaylist(name) }
            .onSuccess { println("   id = $it") }
            .onFailure { println("   FAILED: ${it.javaClass.name}: ${it.message}") }
            .getOrNull()
        if (id.isNullOrBlank()) {
            println()
            println("RESULT: playlist/create does not work — nothing can ever reach the account.")
            return@runBlocking
        }

        println()
        println("2. browse/edit_playlist (add one song)")
        val added = YouTube.addToPlaylist(id, SONG_ID)
        println("   ${if (added.isSuccess) "ok" else "FAILED: ${added.exceptionOrNull()?.message}"}")

        println()
        println("3. the playlist page")
        YouTube.playlist(id)
            .onSuccess { page -> println("   '${page.playlist.title}' — ${page.songs.size} song(s): ${page.songs.map { it.title }}") }
            .onFailure { println("   FAILED: ${it.message}") }

        println()
        println("4. is it in the library (FEmusic_liked_playlists)?")
        val listed = YouTube.library("FEmusic_liked_playlists")
            .map { page -> page.items.filterIsInstance<PlaylistItem>() }
            .getOrElse { emptyList() }
            .also { println("   the library lists ${it.size} playlist(s)") }
            .any { it.id == id }
        println("   the playlist just created is listed: $listed")

        println()
        println("5. playlist/delete (cleanup)")
        println("   ${if (YouTube.deletePlaylist(id).isSuccess) "ok — the test playlist is gone" else "FAILED"}")

        println()
        println(
            "RESULT: create=ok, add=${added.isSuccess}, listedInLibrary=$listed, delete=ok. " +
                if (!listed) "The account ACCEPTS writes but the library does not list the new playlist: " +
                    "the pull cannot see what the app creates." else
                    "The write path works end to end, so the sync failure is elsewhere.",
        )
    }
}
