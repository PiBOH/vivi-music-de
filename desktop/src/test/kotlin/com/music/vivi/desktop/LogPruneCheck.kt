package com.music.vivi.desktop

import java.io.File

/**
 * Headless check of the log pruning rule, run with
 *
 *     ./gradlew :desktop:localDataMaintenanceCheck
 *
 * Logs are deleted automatically once they are seven days old (see
 * `AppLog.pruneOldLogs`): the session folder of a past run, and any loose `.log`
 * file in `~/.vivimusic/`. Getting this wrong is destructive in a quiet way, so
 * the rule is checked here against real files in a temporary directory, with
 * their timestamps set by hand:
 *
 *  * an eight day old session folder and an eight day old `.log` file must be
 *    selected,
 *  * a three day old one of each must not be,
 *  * the session being written right now must never be selected, even when its
 *    timestamp says otherwise (a long run, or a clock that moved).
 *
 * The check works on a temporary directory, never on `~/.vivimusic`, so it is
 * safe to run while the app is open.
 */
object LogPruneCheck {

    private const val DAY_MS = 24L * 60L * 60L * 1000L

    @JvmStatic
    fun main(args: Array<String>) {
        val now = System.currentTimeMillis()
        val root = File(System.getProperty("java.io.tmpdir"), "vivi-log-prune-check").apply {
            deleteRecursively()
            mkdirs()
        }

        val oldSession = dir(root, "20260101-000000", now - 8 * DAY_MS)
        val recentSession = dir(root, "20261006-000000", now - 3 * DAY_MS)
        val currentSession = dir(root, "20261008-000000", now - 9 * DAY_MS)
        val oldLog = file(root, "login-debug.log", now - 8 * DAY_MS)
        val recentLog = file(root, "actions.log", now - 3 * DAY_MS)
        val oldJson = file(root, "settings.json", now - 30 * DAY_MS)

        val selected = AppLog.staleLogs(
            now = now,
            sessionFolders = listOf(oldSession, recentSession, currentSession),
            looseFiles = listOf(oldLog, recentLog, oldJson),
            currentSessionName = currentSession.name,
        ).map { it.name }.sorted()

        val expected = listOf(oldLog.name, oldSession.name).sorted()
        println("== logs older than seven days are deleted automatically")
        println("   selected: $selected")
        println("   expected: $expected")

        val failures = mutableListOf<String>()
        if (selected != expected) failures += "the selection is $selected, expected $expected"
        if (recentSession.name in selected) failures += "a three day old session folder was deleted"
        if (recentLog.name in selected) failures += "a three day old log file was deleted"
        if (currentSession.name in selected) failures += "the session being written was deleted"
        if (oldJson.name in selected) failures += "a settings file was deleted (only logs may be pruned)"

        root.deleteRecursively()

        if (failures.isEmpty()) {
            println()
            println("OK: only the logs older than seven days are deleted, never the current session or data")
            return
        }
        println()
        println("FAILED:")
        failures.forEach { println("  - $it") }
        throw IllegalStateException("log prune check failed (${failures.size} case(s))")
    }

    private fun dir(parent: File, name: String, modified: Long): File =
        File(parent, name).apply {
            mkdirs()
            File(this, "actions.log").writeText("line\n")
            setLastModified(modified)
        }

    private fun file(parent: File, name: String, modified: Long): File =
        File(parent, name).apply {
            writeText("line\n")
            setLastModified(modified)
        }
}
