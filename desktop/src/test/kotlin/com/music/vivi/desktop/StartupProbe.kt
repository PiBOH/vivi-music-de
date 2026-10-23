package com.music.vivi.desktop

import java.io.File

/**
 * Launches the real application against a throwaway data directory and stops it
 * after a fixed number of seconds, so one cold start can be timed without
 * touching the user's own `~/.vivimusic`.
 *
 * Arguments: the home directory to use, then the seconds to run. The app's own
 * `startup:` lines land in `<home>/.vivimusic/logs/<session>/app.log`, which is
 * what this probe exists to read. A manual tool, not part of `check`.
 */
fun main(args: Array<String>) {
    val home = File(args.getOrElse(0) { ".ignore/startup-home" }).absoluteFile
    home.mkdirs()
    System.setProperty("user.home", home.path)
    val seconds = args.getOrElse(1) { "20" }.toLongOrNull() ?: 20L
    Thread {
        Thread.sleep(seconds * 1000)
        System.err.println("startup probe: stopping after ${seconds}s")
        Runtime.getRuntime().halt(0)
    }.apply {
        isDaemon = true
        name = "startup-probe-stop"
    }.start()
    // The app's own entry point, by its file facade: Kotlin resolves top-level
    // functions by name, and this file has a `main` of its own.
    val appMain = Class.forName("com.music.vivi.desktop.MainWindowKt").getMethod("main", Array<String>::class.java)
    appMain.invoke(null, emptyArray<String>())
}
