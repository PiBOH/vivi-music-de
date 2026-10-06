package com.music.vivi.desktop

import java.io.File
import kotlin.math.abs
import kotlin.system.exitProcess

/**
 * The JVM heap the app asks for at start, editable from Developer options.
 *
 * A running JVM cannot change its `-Xmx`, so the chosen value is stored in
 * settings.json ([DesktopSettings.jvmHeapMb]) and applied by starting the app
 * again with the matching heap and exiting. That happens once at the very top of
 * `main` ([relaunchIfNeeded]) or on request from the screen ([restartNow]).
 *
 * **How the new heap is asked for.** The installed app is a jpackage image: it
 * is started by its own launcher, and the runtime it ships has no `java`
 * launcher of its own to re-run (`runtime/bin` on the installed 1.54.17 image
 * carries `java.dll` and no `java.exe`), so the launcher is the only way in. That
 * launcher takes `-Xmx2g` from the `.cfg` beside it, which on Windows sits under
 * `Program Files` and cannot be rewritten. What it does honour is the
 * `_JAVA_OPTIONS` environment variable, and it is applied **after** the `.cfg`,
 * so it replaces that `-Xmx`. Measured on the installed image before this was
 * written: the launcher started with `_JAVA_OPTIONS=-Xmx3072m` came up with a
 * maximum heap of 3221225472 bytes instead of the image's 2 GB. Nothing outside
 * the process being started is affected — the variable is set on that one child.
 *
 * Because the launcher is what applies it, a launch from the Start Menu (where
 * no variable is set) comes up on the image's 2 GB and is corrected by
 * [relaunchIfNeeded] a moment later; that is also what makes a size chosen while
 * the app was closed take effect. Every decision is written to `app.log`
 * (`jvm heap: …`) so a heap that does not take effect is readable instead of a
 * silent no-op, which is what this was until 1.54.18.
 */
object JvmMemory {

    /** Set on the restarted process so it can never restart itself again. */
    private const val ENV_GUARD = "VIVI_JVM_RELAUNCH"

    /** The variable the jpackage launcher honours after its own `.cfg`. */
    private const val JAVA_OPTIONS_ENV = "_JAVA_OPTIONS"

    /** The app's entry point, used by the dev-run fallback below. */
    private const val MAIN_CLASS = "com.music.vivi.desktop.MainKt"

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
     * Applies the stored heap at start: when it differs from the heap this
     * process got, the app is started again on the stored size and this process
     * exits.
     */
    fun relaunchIfNeeded() {
        if (System.getenv(ENV_GUARD) == "1") {
            AppLog.log("app", "jvm heap: this process was started by a restart, keeping its ${currentMaxMb} MB")
            return
        }
        val stored = runCatching { DesktopSettings.load().jvmHeapMb }.getOrDefault(0)
        if (stored <= 0) {
            AppLog.log("app", "jvm heap: no size chosen, using the default (${currentMaxMb} MB)")
            return
        }
        val target = stored.coerceIn(MIN_MB, MAX_MB)
        // `maxMemory()` is not exact (it rounds to the heap the JVM negotiated),
        // so anything within 128 MB of the target is considered already applied.
        if (abs(target - currentMaxMb) <= 128) {
            AppLog.log("app", "jvm heap: the stored ${target} MB is already in use (${currentMaxMb} MB)")
            return
        }
        restart(target, "the stored size is not the running one")
    }

    /**
     * Starts the app again on [targetMb] because the user asked for it. Runs on
     * its own thread: it waits a moment to see whether the new process survives,
     * and the screen must not freeze for that.
     */
    fun restartNow(targetMb: Int) {
        val target = targetMb.coerceIn(MIN_MB, MAX_MB)
        Thread({ restart(target, "chosen in Developer options") }, "vivimusic-jvm-restart")
            .apply { isDaemon = true }
            .start()
    }

    /**
     * Starts the app again on [targetMb] and exits, or leaves this process
     * running and says why in `app.log` when that cannot be done.
     */
    private fun restart(targetMb: Int, reason: String): Boolean {
        val launcher = installedLauncher()
        val command = when {
            launcher != null -> listOf(launcher.absolutePath)
            else -> devRunCommand(targetMb)
        }
        if (command == null) {
            AppLog.log("app", "jvm heap: cannot restart for ${targetMb} MB ($reason), staying on ${currentMaxMb} MB")
            return false
        }
        // The restarted app must be able to take the single-instance lock, or it
        // is turned away as a duplicate of the process it is replacing. (At
        // startup nothing has been acquired yet and this is a no-op.)
        SingleInstance.release()
        val child = runCatching {
            ProcessBuilder(command).apply {
                environment()[ENV_GUARD] = "1"
                environment()[JAVA_OPTIONS_ENV] = "-Xmx${targetMb}m"
            }.start()
        }.onFailure {
            AppLog.log("app", "jvm heap: the restart for ${targetMb} MB could not be started: $it")
        }.getOrNull() ?: return false

        // Wait for the new process to get past its own startup. A restart that
        // dies immediately used to take this process down with it, leaving the
        // user with no app and no explanation of why.
        runCatching { Thread.sleep(2500) }
        if (!child.isAlive) {
            val code = runCatching { child.exitValue() }.getOrDefault(-1)
            AppLog.log("app", "jvm heap: the restarted app exited at once (code $code), staying on ${currentMaxMb} MB")
            return false
        }
        AppLog.log("app", "jvm heap: restarting with -Xmx${targetMb}m (was ${currentMaxMb} MB, $reason)")
        exitProcess(0)
    }

    /**
     * The installed app's own launcher, or null when this is a dev run.
     *
     * `jpackage.app-path` is what the launcher sets for the app it started. When
     * it is missing (a launcher that does not set it, or a run from an IDE) the
     * exe next to the bundled runtime is used: the app image puts the launcher
     * beside `runtime/`, named after its own root folder, which is what tells it
     * apart from `unins000.exe` sitting in the same place.
     */
    private fun installedLauncher(): File? {
        val appPath = System.getProperty("jpackage.app-path").orEmpty()
        if (appPath.isNotBlank()) File(appPath).takeIf { it.isFile }?.let { return it }
        val root = File(System.getProperty("java.home")).parentFile ?: return null
        return root.listFiles()
            ?.firstOrNull {
                it.isFile &&
                    it.extension.equals("exe", ignoreCase = true) &&
                    it.nameWithoutExtension.equals(root.name, ignoreCase = true)
            }
    }

    /**
     * The fallback for a run with no jpackage launcher (an IDE or `gradlew run`):
     * a fresh JVM on the same classpath. It cannot work for the installed app —
     * that image ships no `java` launcher — which is why [installedLauncher] is
     * tried first.
     */
    private fun devRunCommand(targetMb: Int): List<String>? {
        val javaBin = File(
            System.getProperty("java.home"),
            "bin/" + if (Platform.os == DesktopOs.WINDOWS) "javaw.exe" else "java",
        )
        if (!javaBin.isFile) {
            AppLog.log("app", "jvm heap: no java launcher at ${javaBin.absolutePath} and no app launcher to re-run")
            return null
        }
        val classpath = System.getProperty("java.class.path").orEmpty()
        if (classpath.isBlank()) {
            AppLog.log("app", "jvm heap: java.class.path is empty, the app cannot be started again")
            return null
        }
        return listOf(javaBin.absolutePath, "-Xmx${targetMb}m", "-cp", classpath, MAIN_CLASS)
    }

    /** Human label for a heap size in MB ("2 GB", "1536 MB"). */
    fun label(mb: Int): String =
        if (mb > 0 && mb % 1024 == 0) "${mb / 1024} GB" else "$mb MB"
}
