package com.music.vivi.desktop.utils

import com.music.vivi.desktop.data.model.Song

/**
 * Pure duplicate-track detection utility.
 *
 * Groups tracks whose normalized title and artist match AND whose durations
 * fall within [DEFAULT_DURATION_TOLERANCE_MS] of each other. Groups the owner
 * ignored are left out, so the health scan and the Duplicates screen agree.
 */
object DuplicateFinder {

    const val DEFAULT_DURATION_TOLERANCE_MS = 3_000L

    data class DuplicateGroup(
        val title: String,
        val artist: String,
        val songs: List<Song>
    )

    private val whitespace = Regex("""\s+""")

    private fun normalize(value: String): String =
        value.trim().lowercase().replace(whitespace, " ")

    fun findDuplicates(
        songs: List<Song>,
        durationToleranceMs: Long = DEFAULT_DURATION_TOLERANCE_MS,
        ignoredGroups: Set<String> = emptySet()
    ): List<DuplicateGroup> {
        val result = mutableListOf<DuplicateGroup>()

        songs
            .groupBy { normalize(it.title) to normalize(it.artist) }
            .forEach { (_, sameNamed) ->
                if (sameNamed.size < 2) return@forEach

                val sorted = sameNamed.sortedBy { it.duration }
                var cluster = mutableListOf(sorted.first())
                for (i in 1 until sorted.size) {
                    val song = sorted[i]
                    if (song.duration - cluster.first().duration <= durationToleranceMs) {
                        cluster.add(song)
                    } else {
                        result.addClusterIfDuplicate(cluster)
                        cluster = mutableListOf(song)
                    }
                }
                result.addClusterIfDuplicate(cluster)
            }

        return result
            .filterNot { groupKey(it) in ignoredGroups }
            .sortedWith(
                compareByDescending<DuplicateGroup> { it.songs.size }
                    .thenBy { it.title.lowercase() }
            )
    }

    /**
     * The stable identity of one reported group: its song ids, sorted. Two
     * groups made of the same files share a key; adding or removing a file
     * changes it, so an ignored group returns when its files change.
     */
    fun groupKey(group: DuplicateGroup): String =
        group.songs.map { it.id }.sorted().joinToString("|")

    private fun MutableList<DuplicateGroup>.addClusterIfDuplicate(cluster: List<Song>) {
        if (cluster.size > 1) {
            val head = cluster.first()
            add(DuplicateGroup(title = head.title, artist = head.artist, songs = cluster.toList()))
        }
    }
}
