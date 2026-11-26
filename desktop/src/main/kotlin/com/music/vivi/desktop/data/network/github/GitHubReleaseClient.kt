package com.music.vivi.desktop.data.network.github

import com.music.vivi.desktop.BuildConfig
import com.music.vivi.desktop.domain.model.AppReleaseInfo
import com.music.vivi.desktop.domain.model.ReleaseAsset
import java.io.File
import java.security.MessageDigest
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import okhttp3.OkHttpClient
import okhttp3.Request

@Serializable
internal data class GitHubAssetDto(
    val name: String = "",
    @SerialName("browser_download_url") val downloadUrl: String = "",
    val size: Long = 0L,
    /** GitHub publishes "sha256:<hex>" for assets uploaded with a digest. */
    val digest: String? = null
)

@Serializable
internal data class GitHubReleaseDto(
    @SerialName("tag_name") val tagName: String = "",
    val name: String? = null,
    @SerialName("html_url") val htmlUrl: String = "",
    val body: String? = null,
    @SerialName("published_at") val publishedAt: String? = null,
    val prerelease: Boolean = false,
    val draft: Boolean = false,
    val assets: List<GitHubAssetDto> = emptyList()
)

class GitHubReleaseClient(
    private val okHttpClient: OkHttpClient,
    private val json: Json,
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO,
    private val owner: String = "PiBOH",
    // The repository was renamed from vivi-music-de to axolotl-music; GitHub
    // redirects the old API path, but ask for the current name directly.
    private val repo: String = "axolotl-music"
) {
    /**
     * Reads the newest published release, prereleases included.
     *
     * `/releases/latest` only returns a release GitHub marks as "Latest", and
     * every desktop release is published as a prerelease (the beta channel),
     * so that endpoint answers 404 for this repository. `/releases` lists all
     * of them; the newest non-draft release with assets is the update.
     */
    suspend fun fetchLatestRelease(): AppReleaseInfo? = withContext(ioDispatcher) {
        val url = "https://api.github.com/repos/$owner/$repo/releases?per_page=20"
        val request = Request.Builder()
            .url(url)
            .header("Accept", "application/vnd.github.v3+json")
            .header("User-Agent", "${BuildConfig.APP_NAME}/${BuildConfig.APP_VERSION_NAME}")
            .build()

        runCatching {
            okHttpClient.newCall(request).execute().use { response ->
                if (!response.isSuccessful) {
                    return@withContext null
                }
                val bodyString = response.body?.string() ?: return@withContext null
                val releases = json.decodeFromString<List<GitHubReleaseDto>>(bodyString)
                releases
                    .asSequence()
                    .filter { release -> !release.draft && release.assets.any { it.downloadUrl.isNotBlank() } }
                    .sortedByDescending { release -> release.publishedAt.orEmpty() }
                    .firstOrNull()
                    ?.toAppReleaseInfo()
            }
        }.getOrNull()
    }

    private fun GitHubReleaseDto.toAppReleaseInfo(): AppReleaseInfo {
        val cleanReleaseNotes = body
            ?.substringBefore("---")
            ?.replace(Regex("""^#{1,6}\s*""", RegexOption.MULTILINE), "")
            ?.trim()
            .orEmpty()

        return AppReleaseInfo(
            versionTag = tagName,
            versionName = name ?: tagName,
            releaseNotes = cleanReleaseNotes,
            htmlUrl = htmlUrl,
            publishedAt = publishedAt,
            isPrerelease = prerelease,
            assets = assets.mapNotNull { asset ->
                if (asset.downloadUrl.isBlank()) return@mapNotNull null
                ReleaseAsset(
                    name = asset.name,
                    downloadUrl = asset.downloadUrl,
                    size = asset.size,
                    sha256 = asset.digest
                        ?.removePrefix("sha256:")
                        ?.lowercase()
                        ?.takeIf { it.length == 64 }
                )
            }
        )
    }

    /** Streams an asset to [destination], reporting the bytes as they arrive. */
    suspend fun download(
        asset: ReleaseAsset,
        destination: File,
        onProgress: (received: Long, total: Long) -> Unit
    ): Result<File> = withContext(ioDispatcher) {
        runCatching {
            val directory = destination.absoluteFile.parentFile ?: error("The update has no parent directory.")
            directory.mkdirs()
            val temporary = File(directory, "${destination.name}.part")
            temporary.delete()
            val request = Request.Builder()
                .url(asset.downloadUrl)
                .header("User-Agent", "${BuildConfig.APP_NAME}/${BuildConfig.APP_VERSION_NAME}")
                .build()
            try {
                okHttpClient.newCall(request).execute().use { response ->
                    if (!response.isSuccessful) error("GitHub answered HTTP ${response.code} for ${asset.name}.")
                    val body = response.body ?: error("The update has no body.")
                    val total = if (asset.size > 0L) asset.size else body.contentLength()
                    body.byteStream().use { input ->
                        temporary.outputStream().buffered().use { output ->
                            val buffer = ByteArray(64 * 1024)
                            var received = 0L
                            while (true) {
                                val read = input.read(buffer)
                                if (read < 0) break
                                output.write(buffer, 0, read)
                                received += read
                                onProgress(received, total)
                            }
                        }
                    }
                }
                if (destination.exists()) destination.delete()
                if (!temporary.renameTo(destination)) {
                    temporary.copyTo(destination, overwrite = true)
                    temporary.delete()
                }
                destination
            } finally {
                if (temporary.exists()) runCatching { temporary.delete() }
            }
        }
    }

    /** Reads a small text asset, such as the `.sha256` published beside an installer. */
    suspend fun fetchText(url: String): Result<String> = withContext(ioDispatcher) {
        runCatching {
            val request = Request.Builder()
                .url(url)
                .header("User-Agent", "${BuildConfig.APP_NAME}/${BuildConfig.APP_VERSION_NAME}")
                .build()
            okHttpClient.newCall(request).execute().use { response ->
                if (!response.isSuccessful) error("HTTP ${response.code}")
                response.body?.string().orEmpty()
            }
        }
    }

    fun sha256(file: File): String {
        val digest = MessageDigest.getInstance("SHA-256")
        file.inputStream().buffered().use { input ->
            val buffer = ByteArray(64 * 1024)
            while (true) {
                val read = input.read(buffer)
                if (read < 0) break
                digest.update(buffer, 0, read)
            }
        }
        return digest.digest().joinToString("") { "%02x".format(it) }
    }
}
