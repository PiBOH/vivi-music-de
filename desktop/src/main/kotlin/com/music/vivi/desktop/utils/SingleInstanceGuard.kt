package com.music.vivi.desktop.utils

import java.io.File
import java.io.RandomAccessFile
import java.nio.channels.FileLock

/**
 * One running window per user data directory.
 *
 * Two copies of the app would both hold [LocalLibraryStore] in memory and both
 * rewrite `device-sync.json`, so the last save silently drops the other window's
 * changes. The lock file lives next to the store and is held for the life of the
 * process; the operating system releases it when the process dies, so a crash
 * never leaves a stale lock behind.
 */
object SingleInstanceGuard {

    private var file: RandomAccessFile? = null
    private var lock: FileLock? = null

    /** True when this process owns the store, false when another copy already does. */
    fun acquire(): Boolean {
        if (lock?.isValid == true) return true
        val directory = DesktopPathProvider.syncStateFile.absoluteFile.parentFile ?: return true
        directory.mkdirs()
        val lockFile = File(directory, ".vivimusic.lock")
        return runCatching {
            val opened = RandomAccessFile(lockFile, "rw")
            val acquired = opened.channel.tryLock()
            if (acquired == null) {
                opened.close()
                false
            } else {
                file = opened
                lock = acquired
                true
            }
        }.getOrElse {
            // A filesystem without file locking must not stop the app from opening.
            true
        }
    }
}
