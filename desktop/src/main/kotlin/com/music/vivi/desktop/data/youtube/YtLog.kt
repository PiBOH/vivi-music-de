package com.music.vivi.desktop.data.youtube

import com.music.vivi.desktop.utils.DesktopPathProvider
import java.io.File
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

/**
 * Diagnostic log of the YouTube Music backend (stream resolution and search).
 *
 * Written to `youtube.log` in the app's config directory instead of the standard
 * stream: a resolution failure is the one thing that has to be readable after the
 * fact ("which client answered what"), and the console of a packaged build is not.
 * Every line goes through [SecretRedactor] before it reaches the file, so a
 * diagnostic that quotes a header cannot persist a cookie value.
 */
object YtLog {

    private const val MAX_BYTES = 1_000_000L

    private val file: File by lazy { File(DesktopPathProvider.configDir, "youtube.log") }

    private val stamp: DateTimeFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.SSS")

    fun log(category: String, message: String) {
        runCatching {
            val safe = SecretRedactor.redact(message)
            if (file.length() > MAX_BYTES) file.writeText("")
            file.appendText("[${LocalDateTime.now().format(stamp)}] [$category] $safe\n", Charsets.UTF_8)
        }
    }
}
