package com.music.vivi.desktop.dev

import com.music.vivi.desktop.BuildConfig
import com.music.vivi.desktop.data.model.SortOption
import com.music.vivi.desktop.data.store.LocalLibraryStore
import com.music.vivi.desktop.di.appModules
import com.music.vivi.desktop.domain.repository.MusicRepository
import com.music.vivi.desktop.presentation.viewmodel.PlayerViewModel
import com.music.vivi.desktop.utils.DesktopPathProvider
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeoutOrNull
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.booleanOrNull
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import org.koin.core.context.GlobalContext
import org.koin.core.context.startKoin
import kotlin.system.exitProcess

/**
 * Proves that a user action really lands in the JSON store.
 *
 * The action is not simulated at the file level: this starts the window's own
 * Koin graph, takes the library from the repository the screens bind, and calls
 * `PlayerViewModel.toggleFavorite`, which is the exact call the heart in the
 * player and the row menus make. The value is then read back **from the file**
 * (not from the in-memory cache) and printed before and after.
 *
 * Usage:
 * - `VIVIMusic.exe --verify-store-write` toggles the first liked song and exits
 *   non-zero when the file does not change within ten seconds.
 * - `VIVIMusic.exe --store-state` only prints what the file holds, so a second
 *   process can prove the value survived the restart.
 */
object StoreWriteCheck {

    const val WRITE_FLAG = "--verify-store-write"
    const val STATE_FLAG = "--store-state"

    fun handle(args: Array<String>): Boolean {
        val write = args.contains(WRITE_FLAG)
        if (!write && !args.contains(STATE_FLAG)) return false
        exitProcess(runBlocking { run(write) })
    }

    private suspend fun run(write: Boolean): Int {
        println("[store] app: ${BuildConfig.APP_NAME} ${BuildConfig.APP_VERSION_NAME}")
        println("[store] file: ${DesktopPathProvider.syncStateFile.absolutePath}")
        val koin = GlobalContext.getOrNull()?.get() ?: startKoin { modules(appModules) }.koin
        koin.get<LocalLibraryStore>()
        val viewModel = koin.get<PlayerViewModel>()
        val songs = koin.get<MusicRepository>().getAllSongs(SortOption.SongTitleAZ)
        val target = songs.firstOrNull { it.isFavorite } ?: songs.firstOrNull()
        if (target == null) {
            println("[store] the library has no songs")
            println("[store] RESULT: FAIL")
            return 1
        }

        val before = readFile()
        println("[store] song: ${target.title} (${target.id.take(8)})")
        println("[store] before (file): favorite=${before.isFavorite(target.id)} favorites=${before.favorites}")

        if (!write) {
            println("[store] RESULT: PASS (read only)")
            return 0
        }

        println("[store] action: PlayerViewModel.toggleFavorite(\"${target.title}\")")
        viewModel.toggleFavorite(target)

        val after = withTimeoutOrNull(10_000) {
            var current = readFile()
            while (current.favorites == before.favorites) {
                delay(100)
                current = readFile()
            }
            current
        }
        if (after == null) {
            println("[store] the file did not change within ten seconds")
            println("[store] RESULT: FAIL")
            return 1
        }

        println("[store] after (file): favorite=${after.isFavorite(target.id)} favorites=${after.favorites}")
        val changed = after.isFavorite(target.id) != before.isFavorite(target.id)
        println("[store] RESULT: ${if (changed) "PASS" else "FAIL"}")
        return if (changed) 0 else 1
    }

    /** The library part of the JSON file as it is on disk right now. */
    private fun readFile(): FileState {
        val text = runCatching { DesktopPathProvider.syncStateFile.readText() }.getOrNull()
            ?: return FileState(0, emptyMap())
        val root = runCatching { Json.parseToJsonElement(text).jsonObject }.getOrNull()
            ?: return FileState(0, emptyMap())
        val library = root["localLibrary"]?.jsonObject ?: return FileState(0, emptyMap())
        val favorites = library["favorites"]?.jsonArray?.size ?: 0
        val flags = library["songs"]?.jsonArray?.mapNotNull { element ->
            val song = runCatching { element.jsonObject }.getOrNull() ?: return@mapNotNull null
            val id = song["id"]?.jsonPrimitive?.content ?: return@mapNotNull null
            id to (song["isFavorite"]?.jsonPrimitive?.booleanOrNull ?: false)
        }?.toMap().orEmpty()
        return FileState(favorites = favorites, flags = flags)
    }

    private data class FileState(
        val favorites: Int,
        val flags: Map<String, Boolean>
    ) {
        fun isFavorite(songId: String): Boolean = flags[songId] ?: false
    }
}
