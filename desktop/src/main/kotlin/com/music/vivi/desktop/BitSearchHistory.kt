package com.music.vivi.desktop

import com.music.innertube.models.SongItem
import java.io.File
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.serialization.Serializable
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.json.Json

/**
 * One row of the Search page's "Recent searches" list.
 *
 * The reference keeps the searched item itself, not the typed words: the row
 * shows the artwork, the title and the artist with the duration, and its cross
 * removes that one row. The same shape is stored here, so the list is real data
 * and not a placeholder drawn to look right.
 */
@Serializable
internal data class BitSearchEntry(
    val id: String,
    val title: String,
    val subtitle: String = "",
    val thumbnail: String? = null,
    val durationMs: Long = 0L,
)

/**
 * The recent-search list, persisted as JSON beside the rest of the app data.
 *
 * BitChord writes its list into its own preferences file under the
 * `search_entities` key, newest first, capped at twenty entries, with the same
 * three operations the page uses: record, remove, clear. This does the same in
 * `~/.vivimusic/bit-search-history.json`, so a restart keeps the list.
 */
internal object BitSearchHistory {
    private const val MAX_ENTRIES = 20
    private val file = File(System.getProperty("user.home"), ".vivimusic/bit-search-history.json")
    private val json = Json { ignoreUnknownKeys = true; prettyPrint = false }
    private val serializer = ListSerializer(BitSearchEntry.serializer())

    private val _recent = MutableStateFlow(load())
    val recent: StateFlow<List<BitSearchEntry>> = _recent

    /** Puts [entry] on top, dropping any earlier record of the same item. */
    fun record(entry: BitSearchEntry) {
        if (entry.id.isBlank()) return
        save((listOf(entry) + _recent.value.filterNot { it.id == entry.id }).take(MAX_ENTRIES))
    }

    /** Records the song a search row opened. */
    fun record(song: SongItem) {
        record(
            BitSearchEntry(
                id = song.id,
                title = song.title,
                subtitle = song.artists.joinToString(", ") { it.name },
                thumbnail = song.thumbnail.ifBlank { null },
                durationMs = (song.duration ?: 0) * 1000L,
            ),
        )
    }

    fun remove(id: String) = save(_recent.value.filterNot { it.id == id })

    fun clear() = save(emptyList())

    private fun save(value: List<BitSearchEntry>) {
        _recent.value = value
        runCatching {
            file.parentFile?.mkdirs()
            file.writeText(json.encodeToString(serializer, value))
        }
    }

    private fun load(): List<BitSearchEntry> = runCatching {
        if (!file.exists()) return emptyList()
        json.decodeFromString(serializer, file.readText())
    }.getOrDefault(emptyList())
}
