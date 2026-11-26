package com.music.vivi.desktop.data.player

import com.music.vivi.desktop.data.model.Song
import com.music.vivi.desktop.data.youtube.YouTubeMusicRepository
import com.music.vivi.desktop.utils.DesktopPathProvider
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File
import java.util.concurrent.ConcurrentHashMap

/**
 * The audio cache: a catalogue track that has been played once is kept on disk
 * and the next play opens the local file instead of resolving and streaming it
 * again.
 *
 * Only whole files are visible to the player: a download lands in a `.part`
 * file and is renamed when it is complete, so an interrupted download can never
 * become a truncated track. The cache is bounded by the limit in Settings and
 * the oldest entries are removed first.
 */
class PlaybackAudioCache(
    private val okHttpClient: OkHttpClient,
    private val coroutineScope: CoroutineScope,
    private val directory: File = File(DesktopPathProvider.cacheDir, "audio")
) {

    private val downloads = ConcurrentHashMap<String, Job>()

    /** Why the last background download stopped, for the Settings card and the checks. */
    @Volatile
    var lastError: String? = null
        private set

    fun isDownloading(song: Song): Boolean =
        videoIdOf(song)?.let { downloads[it]?.isActive } == true

    /** The cached file for [song], or null when it is not catalogue audio or not cached yet. */
    fun cachedFile(song: Song): File? {
        val id = videoIdOf(song) ?: return null
        val file = File(directory, "$id.audio")
        return file.takeIf { it.isFile && it.length() > 0L }
    }

    /**
     * Downloads [song] in the background for the next play. A download that is
     * already running is not started twice, and a track that is already cached
     * is skipped.
     */
    fun scheduleDownload(song: Song, target: PlaybackTarget, limitMb: Int) {
        val id = videoIdOf(song) ?: return
        if (cachedFile(song) != null) return
        if (downloads[id]?.isActive == true) return
        downloads[id] = coroutineScope.launch(Dispatchers.IO) {
            try {
                download(id, target, limitMb)
            } catch (t: Throwable) {
                lastError = "${t::class.simpleName}: ${t.message}"
            } finally {
                downloads.remove(id)
            }
        }
    }

    private fun download(videoId: String, target: PlaybackTarget, limitMb: Int) {
        directory.mkdirs()
        val part = File(directory, "$videoId.part")
        val destination = File(directory, "$videoId.audio")
        val request = Request.Builder().url(target.uri).apply {
            // googlevideo throttles an open-ended download; the range form is what
            // the player asks for and what it serves at full speed.
            header("Range", "bytes=0-")
            for (header in target.httpHeaders) {
                val separator = header.indexOf(':')
                if (separator > 0) {
                    header(header.substring(0, separator).trim(), header.substring(separator + 1).trim())
                }
            }
        }.build()

        val response = okHttpClient.newCall(request).execute()
        response.use { answer ->
            if (!answer.isSuccessful) {
                lastError = "HTTP ${answer.code}"
                return
            }
            val body = answer.body ?: run {
                lastError = "empty body"
                return
            }
            part.outputStream().use { output -> body.byteStream().copyTo(output) }
        }
        if (part.length() <= 0L) {
            lastError = "empty file"
            part.delete()
            return
        }
        if (!part.renameTo(destination)) {
            lastError = "rename failed"
            part.delete()
            return
        }
        lastError = null
        prune(limitMb)
    }

    /** Total size of the cache, for the Settings card. */
    suspend fun sizeBytes(): Long = withContext(Dispatchers.IO) {
        directory.listFiles()?.sumOf { it.length() } ?: 0L
    }

    suspend fun clear() = withContext(Dispatchers.IO) {
        directory.listFiles()?.forEach { it.delete() }
        Unit
    }

    /** Removes the oldest entries until the cache fits [limitMb]. */
    private fun prune(limitMb: Int) {
        val limitBytes = limitMb.coerceAtLeast(128).toLong() * 1024L * 1024L
        val files = directory.listFiles()?.sortedBy { it.lastModified() } ?: return
        var total = files.sumOf { it.length() }
        for (file in files) {
            if (total <= limitBytes) break
            val length = file.length()
            if (file.delete()) total -= length
        }
    }

    companion object {
        fun videoIdOf(song: Song): String? =
            song.path.removePrefix(YouTubeMusicRepository.VIDEO_SCHEME)
                .takeIf {
                    song.path.startsWith(YouTubeMusicRepository.VIDEO_SCHEME) &&
                        it.isNotBlank() && !it.contains('/') && !it.contains('\\')
                }
    }
}
