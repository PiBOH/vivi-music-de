package com.music.vivi.desktop.dev

import com.music.innertube.YouTube
import com.music.innertube.models.SongItem
import com.music.vivi.desktop.data.youtube.RemotePlaylist
import com.music.vivi.desktop.data.youtube.YouTubeAccount
import com.music.vivi.desktop.data.youtube.YouTubePlaylistsRepository
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import kotlin.system.exitProcess

/**
 * Exercises the account playlist card against the signed-in account.
 *
 * The card goes through [YouTubePlaylistsRepository], so this check does too:
 * list, create, rename, add the first YouTube Music result and delete. It only
 * ever touches the playlist it created (the name carries a timestamp) and
 * deletes it in `finally`, including after a failure, so a run cannot leave a
 * test playlist in the account.
 *
 * Usage: `VIVIMusic.exe --youtube-playlists-check`. Exit code 0 means every
 * step was observed through a fresh account read.
 */
object YouTubePlaylistsCheck {

    const val FLAG = "--youtube-playlists-check"

    fun handle(args: Array<String>): Boolean {
        if (!args.contains(FLAG)) return false
        exitProcess(runBlocking { run() })
    }

    private suspend fun run(): Int {
        YouTubeAccount.restore()
        if (!YouTubeAccount.isSignedIn) {
            println("[playlists] no YouTube Music session is saved; sign in first")
            println("[playlists] RESULT: FAIL")
            return 1
        }

        val repository = YouTubePlaylistsRepository()
        val stamp = System.currentTimeMillis()
        val name = "VIVI check $stamp"
        val renamed = "$name renamed"
        var createdId: String? = null
        val failures = mutableListOf<String>()

        fun expect(condition: Boolean, message: String) {
            println("[playlists] ${if (condition) "ok" else "FAIL"}: $message")
            if (!condition) failures += message
        }

        suspend fun find(id: String): RemotePlaylist? =
            repository.load().getOrNull()?.firstOrNull { it.id == id }

        suspend fun waitFor(timeoutMs: Long, check: suspend () -> Boolean): Boolean {
            val deadline = System.currentTimeMillis() + timeoutMs
            while (System.currentTimeMillis() < deadline) {
                if (check()) return true
                delay(750)
            }
            return check()
        }

        try {
            println("[playlists] account: ${YouTubeAccount.summaries().firstOrNull { it.isActive }?.name ?: "signed in"}")

            val before = repository.load().getOrThrow()
            println(
                "[playlists] account list: ${before.size} playlists, " +
                    "${before.count { it.isEditable }} editable, " +
                    "${before.count { !it.isEditable }} read only"
            )
            println(
                "[playlists] read only: " +
                    before.filter { !it.isEditable }.joinToString(", ") { it.title }.ifBlank { "none" }
            )
            expect(before.none { it.title == name }, "the temporary name is unused")

            val created = repository.create(name).getOrThrow()
            createdId = created
            println("[playlists] created ${created.take(12)}...")
            expect(
                waitFor(15_000) { find(created)?.title == name },
                "the new playlist appears in the account list"
            )
            expect(
                find(created)?.isEditable == true,
                "the new playlist is editable, so the card would show Add, Rename and Delete"
            )

            expect(repository.rename(created, renamed).isSuccess, "rename succeeds")
            expect(
                waitFor(15_000) { find(created)?.title == renamed },
                "the renamed title is in the account list"
            )

            val song = YouTube.search("Daft Punk Get Lucky", YouTube.SearchFilter.FILTER_SONG)
                .getOrNull()
                ?.items
                ?.filterIsInstance<SongItem>()
                ?.firstOrNull()
            expect(song != null, "a track to add was found")
            if (song != null) {
                println("[playlists] adding \"${song.title}\" (${song.id})")
                expect(repository.addSong(created, song.id).isSuccess, "adding the track succeeds")
                var present = false
                expect(
                    waitFor(15_000) {
                        present = YouTube.playlist(created)
                            .getOrNull()
                            ?.songs
                            ?.any { it.id == song.id } == true
                        present
                    },
                    "the playlist page contains the added track"
                )
            }

            expect(repository.delete(created).isSuccess, "delete succeeds")
            expect(
                waitFor(15_000) { find(created) == null },
                "the playlist is gone from the account list"
            )
            createdId = null
        } catch (t: Throwable) {
            expect(false, "the run threw ${t.javaClass.simpleName}: ${t.message}")
        } finally {
            val leftover = createdId
            if (leftover != null) {
                println("[playlists] cleanup: deleting the temporary playlist")
                val deleted = runCatching { repository.delete(leftover) }.getOrNull()
                if (deleted?.isSuccess != true) {
                    println("[playlists] cleanup: FAILED, the playlist ${leftover.take(12)}... may remain")
                }
            } else {
                // A create that threw before returning an id can still have made
                // the playlist; the timestamped name is the safe way to find it.
                runCatching {
                    repository.load().getOrNull()
                        ?.filter { it.title == name || it.title == renamed }
                        ?.forEach { repository.delete(it.id) }
                }
            }
        }

        val pass = failures.isEmpty()
        println("[playlists] RESULT: ${if (pass) "PASS" else "FAIL"}")
        failures.forEach { println("[playlists] failed: $it") }
        return if (pass) 0 else 1
    }
}
