package com.music.vivi.desktop.data.backup.module

import com.music.vivi.desktop.data.backup.model.BackupSection
import com.music.vivi.desktop.data.backup.model.BackupSongMetadata
import com.music.vivi.desktop.data.database.FavoritesDao
import com.music.vivi.desktop.data.store.LocalLibraryStore
import com.music.vivi.desktop.data.store.StoredFavorite
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.longOrNull

@Serializable
data class FavoriteBackupEntry(
    val songId: String,
    val dateAdded: Long = System.currentTimeMillis(),
    val song: BackupSongMetadata? = null
)

class FavoritesModuleHandler(
    private val favoritesDao: FavoritesDao,
    private val store: LocalLibraryStore,
    private val songResolver: BackupSongResolver,
    private val json: Json = Json { ignoreUnknownKeys = true; encodeDefaults = true }
) : BackupModuleHandler {

    override val section: BackupSection = BackupSection.FAVORITES

    override suspend fun export(): String = withContext(Dispatchers.IO) {
        val entries = store.load().favorites
            .sortedByDescending { it.dateAdded }
            .map { it.songId to it.dateAdded }

        val metadataMap = songResolver.metadataFor(entries.map { it.first })
        val backupEntries = entries.map { (songId, dateAdded) ->
            FavoriteBackupEntry(
                songId = songId,
                dateAdded = dateAdded,
                song = metadataMap[songId]
            )
        }

        json.encodeToString(ListSerializer(FavoriteBackupEntry.serializer()), backupEntries)
    }

    override suspend fun countEntries(): Int = withContext(Dispatchers.IO) {
        store.load().favorites.size
    }

    override suspend fun snapshot(): String = withContext(Dispatchers.IO) {
        val entries = store.load().favorites.map { FavoriteBackupEntry(it.songId, it.dateAdded) }
        json.encodeToString(ListSerializer(FavoriteBackupEntry.serializer()), entries)
    }

    override suspend fun restore(payload: String) = withContext(Dispatchers.IO) {
        val entries = runCatching {
            json.decodeFromString(ListSerializer(FavoriteBackupEntry.serializer()), payload)
        }.getOrElse {
            parseLegacyFavorites(payload)
        }
        val library = songResolver.library()

        val resolvedEntries = entries.mapNotNull { entry ->
            val resolvedId = library.resolve(entry.songId, entry.song) ?: entry.songId
            resolvedId to entry.dateAdded
        }.distinctBy { it.first }

        replaceFavorites(resolvedEntries)
        Unit
    }

    override suspend fun rollback(snapshot: String) = withContext(Dispatchers.IO) {
        val entries = json.decodeFromString(ListSerializer(FavoriteBackupEntry.serializer()), snapshot)
        replaceFavorites(entries.map { it.songId to it.dateAdded }.distinctBy { it.first })
        Unit
    }

    private suspend fun replaceFavorites(entries: List<Pair<String, Long>>) {
        val liked = entries.map { it.first }.toSet()
        store.update("favorites", "songs") { state ->
            state.copy(
                favorites = entries.map { StoredFavorite(songId = it.first, dateAdded = it.second) },
                songs = state.songs.map { song ->
                    val isLiked = song.id in liked
                    if (song.isFavorite == isLiked) song else song.copy(isFavorite = isLiked)
                }
            )
        }
        Unit
    }

    private fun parseLegacyFavorites(payload: String): List<FavoriteBackupEntry> {
        val array = runCatching { json.parseToJsonElement(payload) }.getOrNull() as? JsonArray ?: return emptyList()
        return array.mapNotNull { element ->
            val obj = element as? JsonObject ?: return@mapNotNull null
            val songId = (obj["songId"] as? JsonPrimitive)?.content
                ?: (obj["song_id"] as? JsonPrimitive)?.content
                ?: return@mapNotNull null
            val dateAdded = (obj["dateAdded"] as? JsonPrimitive)?.longOrNull
                ?: (obj["addedAt"] as? JsonPrimitive)?.longOrNull
                ?: System.currentTimeMillis()
            FavoriteBackupEntry(songId = songId, dateAdded = dateAdded)
        }
    }
}
