package com.music.vivi.desktop.data.library

import com.music.vivi.desktop.data.model.Song
import com.music.vivi.desktop.data.store.LocalLibraryStore
import com.music.vivi.desktop.utils.DuplicateFinder
import java.io.File
import java.net.URI
import java.nio.file.Files
import java.text.Normalizer

/** The four groups the health card and its detail screen list. */
enum class LibraryHealthGroup {
    MISSING_FILES,
    BROKEN_ARTWORK,
    ORPHAN_ENTRIES,
    DUPLICATES
}

data class MissingFileIssue(
    val songId: String,
    val label: String,
    val path: String
)

/** A missing song and the existing file that can take its place. */
data class RelinkCandidate(
    val issue: MissingFileIssue,
    val newFile: File
)

data class BrokenArtworkIssue(
    val songId: String,
    val label: String,
    val artworkUri: String
)

data class OrphanPlaylistIssue(
    val playlistId: String,
    val playlistName: String,
    val missingSongIds: List<String>
)

data class LibraryHealthReport(
    val songs: Int,
    val playlists: Int,
    val missingFiles: List<MissingFileIssue>,
    val brokenArtwork: List<BrokenArtworkIssue>,
    val orphanPlaylistEntries: List<OrphanPlaylistIssue>,
    val duplicateGroups: List<DuplicateFinder.DuplicateGroup>
) {
    val orphanEntryCount: Int
        get() = orphanPlaylistEntries.sumOf { it.missingSongIds.size }

    val issueCount: Int
        get() = missingFiles.size + brokenArtwork.size + orphanEntryCount + duplicateGroups.size

    val isClean: Boolean
        get() = issueCount == 0
}

/** The playlist ids a repair removed, kept so the bar's Undo can put them back. */
data class PlaylistSnapshot(
    val playlistId: String,
    val previousSongIds: List<String>
)

/** What a repair changed, in a form [LibraryHealthChecker.undo] can reverse. */
sealed interface LibraryHealthUndo {
    data class Playlists(val playlists: List<PlaylistSnapshot>) : LibraryHealthUndo

    data class SongPath(
        val songId: String,
        val previousPath: String,
        val previousContentUri: String
    ) : LibraryHealthUndo

    /** Several relinks made by the automatic action, reversed in one step. */
    data class SongPaths(val songs: List<SongPath>) : LibraryHealthUndo
}

data class HealthRepairResult(
    val changed: Int,
    val undo: LibraryHealthUndo?
)

/**
 * Checks what the library points at, not what it contains.
 *
 * A local song whose file was moved, a cover that points at a deleted picture,
 * a playlist entry whose song is gone and a group of duplicates are the four
 * ways the JSON store drifts from the disk. [scan] only reads. The two repairs
 * below are explicit calls made after the user confirms a preview, they never
 * delete a file or a song, and each returns the token [undo] restores.
 */
class LibraryHealthChecker(
    private val store: LocalLibraryStore
) {

    fun scan(): LibraryHealthReport {
        val state = store.load()
        val ids = state.songs.mapTo(HashSet()) { it.id }

        val missing = state.songs
            .filter { it.isLocalFile() }
            .filter { !File(it.path).isFile }
            .map { MissingFileIssue(songId = it.id, label = it.displayLabel(), path = it.path) }

        val brokenArtwork = state.songs.mapNotNull { song ->
            val art = song.albumArtUriString ?: return@mapNotNull null
            if (!art.startsWith("file:", ignoreCase = true)) return@mapNotNull null
            val path = art.removePrefix("file://").removePrefix("file:")
            val exists = runCatching { File(URI(art)).isFile }.getOrDefault(false) ||
                File(path).isFile
            if (exists) null else BrokenArtworkIssue(song.id, song.displayLabel(), art)
        }.distinctBy { it.label }

        val orphans = state.playlists.mapNotNull { playlist ->
            val missingIds = playlist.songIds.filter { it !in ids }
            if (missingIds.isEmpty()) {
                null
            } else {
                OrphanPlaylistIssue(
                    playlistId = playlist.id,
                    playlistName = playlist.name,
                    missingSongIds = missingIds
                )
            }
        }

        return LibraryHealthReport(
            songs = state.songs.size,
            playlists = state.playlists.size,
            missingFiles = missing,
            brokenArtwork = brokenArtwork,
            orphanPlaylistEntries = orphans,
            duplicateGroups = DuplicateFinder.findDuplicates(
                songs = state.songs,
                ignoredGroups = state.ignoredDuplicateGroups.toSet()
            )
        )
    }

    /**
     * Removes only the playlist references in [entries] that still point at an
     * absent song. A reference that became valid after the scan is kept, so the
     * preview cannot remove a song that was reconnected in the meantime.
     */
    fun removeOrphanEntries(entries: Collection<OrphanPlaylistIssue>): HealthRepairResult {
        if (entries.isEmpty()) return HealthRepairResult(0, null)
        val requested = entries.associate { it.playlistId to it.missingSongIds.toSet() }
        val snapshots = mutableListOf<PlaylistSnapshot>()
        var removed = 0

        store.update("playlists") { state ->
            val knownIds = state.songs.mapTo(HashSet()) { it.id }
            var changed = false
            val playlists = state.playlists.map { playlist ->
                val candidates = requested[playlist.id] ?: return@map playlist
                val drop = candidates.filter { it !in knownIds }.toSet()
                if (drop.isEmpty()) return@map playlist
                val kept = playlist.songIds.filterNot { it in drop }
                if (kept.size == playlist.songIds.size) return@map playlist
                snapshots += PlaylistSnapshot(playlist.id, playlist.songIds)
                removed += playlist.songIds.size - kept.size
                changed = true
                playlist.copy(
                    songIds = kept,
                    lastModified = System.currentTimeMillis()
                )
            }
            if (changed) state.copy(playlists = playlists) else state
        }

        if (removed == 0) return HealthRepairResult(0, null)
        return HealthRepairResult(removed, LibraryHealthUndo.Playlists(snapshots))
    }

    /**
     * Finds one existing file for each missing song, inside the configured
     * music folders. A file that another song already points at is never
     * taken, and a song is only offered when exactly one file matches its old
     * name or its title and artist; everything else stays a manual choice.
     */
    fun findRelinkCandidates(
        musicDirectories: Collection<String>,
        excludedDirectories: Collection<String> = emptyList()
    ): List<RelinkCandidate> {
        val state = store.load()
        val missing = state.songs
            .filter { it.isLocalFile() && !File(it.path).isFile }
            .sortedBy { it.displayLabel() }
        if (missing.isEmpty()) return emptyList()

        val usedPaths = state.songs.mapNotNull { song ->
            File(song.path).takeIf { it.isFile }?.absolutePath
        }.toHashSet()
        val files = collectAudioFiles(musicDirectories, excludedDirectories)
        val byName = files.groupBy { it.name.lowercase() }
        val byNormalizedName = files.groupBy { normalizeForRelink(it.nameWithoutExtension) }
        val taken = HashSet<String>()
        val candidates = mutableListOf<RelinkCandidate>()

        for (song in missing) {
            val oldName = File(song.path).name.lowercase()
            var matches = byName[oldName].orEmpty()
                .filter { it.absolutePath !in usedPaths && it.absolutePath !in taken }
            if (matches.size != 1) {
                val title = normalizeForRelink(song.title)
                val artist = normalizeForRelink(song.displayArtist)
                matches = if (title.isBlank()) {
                    emptyList()
                } else {
                    byNormalizedName.entries
                        .filter { (normalized, _) ->
                            normalized.contains(title) &&
                                (artist.isBlank() || normalized.contains(artist))
                        }
                        .flatMap { it.value }
                        .filter { it.absolutePath !in usedPaths && it.absolutePath !in taken }
                        .distinctBy { it.absolutePath }
                }
            }
            if (matches.size != 1) continue
            val match = matches.single()
            taken += match.absolutePath
            candidates += RelinkCandidate(
                issue = MissingFileIssue(
                    songId = song.id,
                    label = song.displayLabel(),
                    path = song.path
                ),
                newFile = match
            )
        }
        return candidates
    }

    /**
     * Applies the candidates in one write. A song whose old path exists again,
     * or whose file disappeared after the preview, is skipped.
     */
    fun relinkSongs(candidates: Collection<RelinkCandidate>): HealthRepairResult {
        if (candidates.isEmpty()) return HealthRepairResult(0, null)
        val songsById = store.load().songs.associateBy { it.id }
        data class Planned(
            val songId: String,
            val newFile: File,
            val undo: LibraryHealthUndo.SongPath
        )

        val planned = candidates.distinctBy { it.issue.songId }.mapNotNull { candidate ->
            val song = songsById[candidate.issue.songId] ?: return@mapNotNull null
            if (!candidate.newFile.isFile) return@mapNotNull null
            if (File(song.path).isFile) return@mapNotNull null
            Planned(
                songId = song.id,
                newFile = candidate.newFile,
                undo = LibraryHealthUndo.SongPath(
                    songId = song.id,
                    previousPath = song.path,
                    previousContentUri = song.contentUriString
                )
            )
        }
        if (planned.isEmpty()) return HealthRepairResult(0, null)
        val byId = planned.associateBy { it.songId }
        store.update("songs") { state ->
            state.copy(
                songs = state.songs.map { song ->
                    val plan = byId[song.id] ?: return@map song
                    song.copy(
                        path = plan.newFile.absolutePath,
                        contentUriString = plan.newFile.toURI().toString()
                    )
                }
            )
        }
        return HealthRepairResult(
            changed = planned.size,
            undo = LibraryHealthUndo.SongPaths(planned.map { it.undo })
        )
    }

    /** Points [songId] at [newFile] without copying, moving or deleting anything. */
    fun relinkSong(songId: String, newFile: File): HealthRepairResult {
        if (!newFile.isFile) return HealthRepairResult(0, null)
        val existing = store.load().songs.firstOrNull { it.id == songId }
            ?: return HealthRepairResult(0, null)
        if (existing.path == newFile.absolutePath) return HealthRepairResult(0, null)

        val undo = LibraryHealthUndo.SongPath(
            songId = songId,
            previousPath = existing.path,
            previousContentUri = existing.contentUriString
        )
        store.update("songs") { state ->
            state.copy(
                songs = state.songs.map { song ->
                    if (song.id == songId) {
                        song.copy(
                            path = newFile.absolutePath,
                            contentUriString = newFile.toURI().toString()
                        )
                    } else {
                        song
                    }
                }
            )
        }
        return HealthRepairResult(1, undo)
    }

    /** Reverses one repair, restoring exactly the references or path it replaced. */
    fun undo(change: LibraryHealthUndo) {
        when (change) {
            is LibraryHealthUndo.Playlists -> {
                if (change.playlists.isEmpty()) return
                val snapshots = change.playlists.associateBy { it.playlistId }
                store.update("playlists") { state ->
                    state.copy(
                        playlists = state.playlists.map { playlist ->
                            val snapshot = snapshots[playlist.id] ?: return@map playlist
                            playlist.copy(
                                songIds = snapshot.previousSongIds,
                                lastModified = System.currentTimeMillis()
                            )
                        }
                    )
                }
            }

            is LibraryHealthUndo.SongPath -> store.update("songs") { state ->
                state.copy(
                    songs = state.songs.map { song ->
                        if (song.id == change.songId) {
                            song.copy(
                                path = change.previousPath,
                                contentUriString = change.previousContentUri
                            )
                        } else {
                            song
                        }
                    }
                )
            }

            is LibraryHealthUndo.SongPaths -> {
                if (change.songs.isEmpty()) return
                val byId = change.songs.associateBy { it.songId }
                store.update("songs") { state ->
                    state.copy(
                        songs = state.songs.map { song ->
                            val snapshot = byId[song.id] ?: return@map song
                            song.copy(
                                path = snapshot.previousPath,
                                contentUriString = snapshot.previousContentUri
                            )
                        }
                    )
                }
            }
        }
    }

    private fun collectAudioFiles(
        musicDirectories: Collection<String>,
        excludedDirectories: Collection<String>
    ): List<File> {
        val extensions = setOf("mp3", "flac", "ogg", "opus", "m4a", "wav", "aac", "wma")
        val excludedRoots = excludedDirectories.map {
            File(it).absolutePath.trimEnd(File.separatorChar)
        }
        val files = mutableListOf<File>()
        for (directory in musicDirectories.distinct()) {
            val root = File(directory)
            if (!root.isDirectory) continue
            runCatching {
                Files.walk(root.toPath()).use { stream ->
                    stream.filter { Files.isRegularFile(it) }
                        .map { it.toFile() }
                        .filter { it.extension.lowercase() in extensions }
                        .filter { file ->
                            excludedRoots.none { excluded ->
                                file.absolutePath == excluded ||
                                    file.absolutePath.startsWith("$excluded${File.separator}")
                            }
                        }
                        .forEach { files += it }
                }
            }
        }
        return files.sortedBy { it.absolutePath }
    }

    private fun normalizeForRelink(text: String): String =
        Normalizer.normalize(text.lowercase(), Normalizer.Form.NFD)
            .replace(Regex("\\p{M}+"), "")
            .replace(Regex("[^a-z0-9]+"), " ")
            .trim()

    private fun Song.isLocalFile(): Boolean =
        navidromeId == null && jellyfinId == null &&
            path.isNotBlank() && !path.startsWith("ytmusic://") &&
            !path.startsWith("http://") && !path.startsWith("https://")

    private fun Song.displayLabel(): String = "$title - $displayArtist"
}
