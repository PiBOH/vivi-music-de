package com.music.vivi.desktop

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import java.io.File
import java.util.concurrent.atomic.AtomicLong
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.concurrent.locks.ReentrantLock
import kotlin.concurrent.withLock

/**
 * Structured, timestamped log of what VIVI Music DE actually did: playback
 * commands, navigation, settings changes, login/sync events, errors and other
 * user actions. Two consumers:
 *
 *  1. **Live viewer** (Developer options → the "Live monitor" card, opened in
 *     a dedicated window): the last [MAX_LINES] lines are exposed as a
 *     [StateFlow] and rendered as they arrive.
 *  2. **On-disk session logs** (`~/.vivimusic/logs/<yyyyMMdd-HHmmss>/`): every
 *     launch creates a timestamped session folder holding one file per
 *     category (`playback.log`, `queue.log`, `lyrics.log`, `nav.log`,
 *     `actions.log`, …) plus nothing else — the old single flat
 *     `~/.vivimusic/actions.log` is migrated into the newest session folder on
 *     startup. [LogExporter] packages the whole tree for support requests.
 *
 * Lines are intentionally kept technical (English) — they are diagnostic
 * data, not UI, so they never go through localization.
 */
object AppLog {
    private const val MAX_LINES = 4000
    private const val MAX_FILE_BYTES = 2L * 1024 * 1024 // 2 MB cap before trimming

    /**
     * How long a run's logs are kept. A session folder is the trace of one
     * launch, which is useful while a problem is being chased and clutter
     * afterwards, and the app is launched for years: without this the logs tree
     * grows without a bound (the folder count here was already climbing by the
     * hour during normal use). Seven days keeps a full week of history, which
     * covers "it broke a few days ago" support requests.
     */
    private const val LOG_MAX_AGE_MS = 7L * 24L * 60L * 60L * 1000L

    /** Every category that must always have a file in the session folder, even when empty. */
    private val KNOWN_CATEGORIES = listOf(
        "actions", "browse", "cache", "gc", "lyrics", "nav", "playback", "playlists", "queue",
        "settings", "spotify", "sync", "volume", "window"
    )

    private val vivimusicDir: File
        get() = File(System.getProperty("user.home"), ".vivimusic")

    /** `~/.vivimusic/logs`, the parent of every session folder. */
    private val logsRoot: File = File(vivimusicDir, "logs")

    /** Session folder created at startup: `~/.vivimusic/logs/<yyyyMMdd-HHmmss>/`. */
    private val sessionDir: File = run {
        logsRoot.mkdirs()
        val stamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss"))
        File(logsRoot, stamp).apply { mkdirs() }
    }

    /** One file per category inside the session folder. */
    private fun categoryFile(category: String): File {
        val safe = category.replace(Regex("[^A-Za-z0-9._-]"), "_").ifBlank { "misc" }
        return File(sessionDir, "$safe.log")
    }

    private val lock = ReentrantLock()
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    /**
     * One log line plus a monotonic [seq] id.
     *
     * The id exists for the live viewer: a log line is NOT unique (two
     * identical lines land in the same millisecond all the time — a retried
     * resolve logs the same text twice), and using the text itself as the
     * `LazyColumn` key crashed the whole app with `Key "…" was already used`
     * while the live log window was open. Lines are written from several
     * threads, so the id is taken from an atomic counter.
     */
    data class Entry(val seq: Long, val text: String)

    private val nextSeq = AtomicLong(0L)
    private val _entries = MutableStateFlow<List<Entry>>(emptyList())
    /** Last [MAX_LINES] log lines (oldest first), each with a unique id. */
    val entries: StateFlow<List<Entry>> = _entries.asStateFlow()

    private val stamp: String
        get() = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.SSS"))

    init {
        vivimusicDir.mkdirs()
        pruneOldLogs()
        // Migrate the legacy flat actions.log into this session's folder so
        // the old diagnostics are not lost (and the root stays clean).
        val legacy = File(vivimusicDir, "actions.log")
        if (legacy.exists()) {
            runCatching {
                legacy.copyTo(File(sessionDir, "actions.log"), overwrite = true)
                legacy.delete()
            }
        }
        // Guarantee that every known category has a file in this session from
        // the start — even when nothing was logged to it yet — so the support
        // zip is always complete and an empty file is still exported.
        runCatching {
            for (cat in KNOWN_CATEGORIES) {
                val f = categoryFile(cat)
                if (!f.exists()) {
                    f.createNewFile()
                }
            }
        }
    }

    /**
     * Deletes everything older than [LOG_MAX_AGE_MS]: the session folders of
     * past runs, and any loose `.log` file in `~/.vivimusic/` (the legacy flat
     * `actions.log`, `login-debug.log`). The crash dump is a `.log` file in the
     * same place and goes too, and that is intentional: the dump of a crash is
     * also written inside the session folder it happened in
     * (`crash_<session>.log`), so the newest days of crash evidence are still
     * there while nothing accumulates forever.
     *
     * The session being written right now is never touched, whatever its
     * timestamp says: deleting a folder that a live download or a log line is
     * appending to would lose exactly the run the user is in.
     *
     * Called once, at startup: "automatically" for a long-running app means on
     * every launch, which is also when the cost is invisible (nothing else is
     * running yet).
     */
    fun pruneOldLogs(now: Long = System.currentTimeMillis()) {
        runCatching {
            val sessionFolders = logsRoot.listFiles()?.filter { it.isDirectory }.orEmpty()
            val looseFiles = vivimusicDir.listFiles()?.filter { it.isFile }.orEmpty()
            staleLogs(now, sessionFolders, looseFiles, sessionDir.name).forEach { target ->
                if (target.isDirectory) target.listFiles()?.forEach { it.delete() }
                target.delete()
            }
        }
    }

    /**
     * Which of the candidates [pruneOldLogs] deletes: the session folders of old
     * runs (never the one being written) and the loose files that aged out.
     *
     * The `.log` test is part of the rule, not of the caller: `~/.vivimusic`
     * also holds `settings.json`, `playlists.json`, `history.json`, and the
     * prune must never be one edit away from deleting the user's library. Pure
     * and separate from the file system, so the rule can be checked on its own
     * (see `:desktop:localDataMaintenanceCheck`) instead of by watching a real
     * logs directory.
     */
    internal fun staleLogs(
        now: Long,
        sessionFolders: List<File>,
        looseFiles: List<File>,
        currentSessionName: String,
    ): List<File> {
        val cutoff = now - LOG_MAX_AGE_MS
        return sessionFolders.filter { it.isDirectory && it.name != currentSessionName && it.lastModified() < cutoff } +
            looseFiles.filter {
                it.isFile && it.extension.equals("log", ignoreCase = true) && it.lastModified() < cutoff
            }
    }

    /** Appends a diagnostic line with a category tag, e.g. `log("playback", "seek to 42s")`. */
    fun log(category: String, message: String) {
        val line = "[$stamp] [$category] $message"
        val entry = Entry(nextSeq.incrementAndGet(), line)
        _entries.update { (it + entry).takeLast(MAX_LINES) }
        scope.launch {
            lock.withLock {
                runCatching {
                    val f = categoryFile(category)
                    f.parentFile?.mkdirs()
                    f.appendText(line + "\n", Charsets.UTF_8)
                    // Trim the file when it grows past the cap (keep the tail).
                    if (f.length() > MAX_FILE_BYTES) {
                        val tail = f.readText(Charsets.UTF_8).takeLast((MAX_FILE_BYTES / 2).toInt())
                        f.writeText(tail, Charsets.UTF_8)
                    }
                }
            }
        }
    }

    /**
     * Records a user click with a short human-readable target, e.g.
     * `click("Home shuffle")`. Goes to the session's `actions.log`, which is
     * the click trail used to reproduce a crash from the logs (issue #55).
     */
    fun click(target: String) {
        log("actions", "click $target")
    }

    /** Clears the in-memory buffer and the current session's on-disk logs. */
    fun clear() {
        _entries.value = emptyList()
        scope.launch {
            lock.withLock {
                runCatching {
                    sessionDir.listFiles { f -> f.isFile && f.extension.equals("log", ignoreCase = true) }
                        ?.forEach { it.delete() }
                }
            }
        }
    }
}
