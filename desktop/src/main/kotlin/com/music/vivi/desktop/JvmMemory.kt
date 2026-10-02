package com.music.vivi.desktop

import java.io.File
import kotlin.math.abs
import kotlin.system.exitProcess

/**
 * The JVM heap the app asks for at start, editable from Developer options.
 *
 * A running JVM cannot change its `-Xmx`, so the chosen value is stored in
 * settings.json ([DesktopSettings.jvmHeapMb]) and applied on the next start:
 * [relaunchIfNeeded] runs at the very top of `main`, and when the stored value
 * differs from the heap the process actually got, it starts a fresh process
 * with the matching `-Xmx` and exits. The relaunched process finds the value it
 * already runs with, so it never relaunches again (and an environment guard
 * makes a loop impossible even if the JVM rounds the request).
 */
object JvmMemory {

    /** Set on the relaunched process so it can never relaunch again itself. */
    private const val ENV_GUARD = "VIVI_JVM_RELAUNCH"

    /** The launcher's own default (`-Xmx2g` in build.gradle.kts). */
    const val DEFAULT_MB = 2048

    /** Bounds offered by the Developer options screen. */
    const val MIN_MB = 512
    const val MAX_MB = 32768

    /** Preset sizes (MB) shown in the picker, in the order they are offered. */
    val PRESETS_MB: List<Int> = listOf(1024, 2048, 3072, 4096, 6144, 8192, 12288, 16384)

    /** The heap the running JVM actually got, in MB (JVM granularity, approximate). */
    val currentMaxMb: Int
        get() = (Runtime.getRuntime().maxMemory() / (1024L * 1024L)).toInt()

    /**
     * Relaunches the app with the stored `-Xmx` when it differs from the current
     * one. Does nothing when no value is stored ([DesktopSettings.jvmHeapMb] is
     * 0), on the relaunched process, or when no java launcher can be found.
     */
    fun relaunchIfNeeded() {
        if (System.getenv(ENV_GUARD) == "1") return
        val stored = runCatching { DesktopSettings.load().jvmHeapMb }.getOrDefault(0)
        if (stored <= 0) return
        val target = stored.coerceIn(MIN_MB, MAX_MB)
        // `maxMemory()` is not exact (it rounds to the heap the JVM negotiated),
        // so anything within 128 MB of the target is considered already applied.
        if (abs(target - currentMaxMb) <= 128) return

        val javaBin = File(
            System.getProperty("java.home"),
            "bin/" + if (Platform.os == DesktopOs.WINDOWS) "javaw.exe" else "java",
        )
        if (!javaBin.isFile) return
        val classpath = System.getProperty("java.class.path").orEmpty()
        if (classpath.isBlank()) return

        val command = listOf(
            javaBin.absolutePath,
            "-Xmx${target}m",
            "-XX:+UseG1GC",
            "-XX:MaxGCPauseMillis=20",
            "-XX:NewSize=128m",
            "-XX:MaxNewSize=384m",
            "-XX:MaxMetaspaceSize=256m",
            "-XX:+ExplicitGCInvokesConcurrent",
            "-cp", classpath,
            "com.music.vivi.desktop.MainKt",
        )
        runCatching {
            ProcessBuilder(command)
                .apply { environment()[ENV_GUARD] = "1" }
                .start()
        }.onSuccess {
            AppLog.log("app", "relaunching with -Xmx${target}m (was ${currentMaxMb}m)")
            exitProcess(0)
        }
    }

    /** Human label for a heap size in MB (\"2 GB\", \"1536 MB\"). */
    fun label(mb: Int): String =
        if (mb > 0 && mb % 1024 == 0) "${mb / 1024} GB" else "$mb MB"
}
