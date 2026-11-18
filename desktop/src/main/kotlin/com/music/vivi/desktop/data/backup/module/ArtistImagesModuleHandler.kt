package com.music.vivi.desktop.data.backup.module

import com.music.vivi.desktop.data.backup.model.BackupSection
import com.music.vivi.desktop.data.store.LocalLibraryStore
import com.music.vivi.desktop.data.store.StoredArtist
import com.music.vivi.desktop.utils.DesktopPathProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.json.Json
import java.io.File
import java.util.Base64

@Serializable
data class ArtistImageBackupEntry(
    val artistName: String,
    val imageUrl: String = "",
    val customImageBase64: String? = null
)

class ArtistImagesModuleHandler(
    private val store: LocalLibraryStore,
    private val json: Json = Json { ignoreUnknownKeys = true; encodeDefaults = true }
) : BackupModuleHandler {

    override val section: BackupSection = BackupSection.ARTIST_IMAGES

    override suspend fun export(): String = withContext(Dispatchers.IO) {
        val entries = store.load().artists.mapNotNull { artist ->
            val imageUrl = artist.imageUrl.orEmpty()
            val customUri = artist.customImageUri
            val base64 = customUri?.let { readFileAsBase64(it) }
            if (imageUrl.isBlank() && base64 == null) {
                null
            } else {
                ArtistImageBackupEntry(
                    artistName = artist.name,
                    imageUrl = imageUrl,
                    customImageBase64 = base64
                )
            }
        }
        json.encodeToString(ListSerializer(ArtistImageBackupEntry.serializer()), entries)
    }

    override suspend fun countEntries(): Int = withContext(Dispatchers.IO) {
        store.load().artists.count { artist ->
            !artist.imageUrl.isNullOrBlank() || !artist.customImageUri.isNullOrBlank()
        }
    }

    override suspend fun snapshot(): String = export()

    override suspend fun restore(payload: String) = withContext(Dispatchers.IO) {
        val entries = json.decodeFromString(ListSerializer(ArtistImageBackupEntry.serializer()), payload)
        val updates = mutableMapOf<Long, Pair<String, String?>>()
        val state = store.load()

        for (entry in entries) {
            val artist = state.artists.firstOrNull { it.name.equals(entry.artistName, ignoreCase = true) }
                ?: continue

            var customPath: String? = null
            if (!entry.customImageBase64.isNullOrBlank()) {
                try {
                    val file = File(DesktopPathProvider.artistImagesDir, "artist_art_${artist.id}.jpg")
                    file.writeBytes(Base64.getDecoder().decode(entry.customImageBase64))
                    customPath = file.absolutePath
                } catch (_: Exception) {}
            }
            updates[artist.id] = entry.imageUrl to customPath
        }

        if (updates.isEmpty()) return@withContext
        store.update("artists") { current ->
            current.copy(
                artists = current.artists.map { artist ->
                    val update = updates[artist.id] ?: return@map artist
                    StoredArtist(
                        id = artist.id,
                        name = artist.name,
                        imageUrl = update.first,
                        customImageUri = update.second
                    )
                }
            )
        }
    }

    override suspend fun rollback(snapshot: String) = withContext(Dispatchers.IO) {
        restore(snapshot)
    }

    private fun readFileAsBase64(path: String): String? {
        return try {
            val file = File(path)
            if (!file.exists() || file.length() == 0L) null
            else Base64.getEncoder().encodeToString(file.readBytes())
        } catch (_: Exception) {
            null
        }
    }
}
