package com.music.vivi.desktop

import javafx.application.Platform as FxPlatform
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit

/**
 * The JavaFX toolkit, started exactly once for every embedded browser window.
 *
 * JavaFX is initialized with `Platform.startup`, which is single-use per
 * process. This is important in a Compose Desktop process: `Application.launch`
 * is single-use and can race with the already-running AWT/Compose event loop,
 * causing the WebView startup failure to be reported repeatedly. No
 * JFXPanel/Swing interop is used.
 *
 * The windows themselves ([LoginWebView] for YouTube, [SpotifyLoginWebView] for
 * Spotify) share this state instead of each keeping their own flag: two
 * components deciding independently that the toolkit is not up yet would both
 * try to start it, and the second `startup` call is the race the comment above
 * is about.
 */
internal object JavaFxToolkit {
    @Volatile private var started = false
    private val startupLock = Any()

    /** True once `Platform.startup` has returned in this process. */
    val isStarted: Boolean get() = started

    /** Starts the toolkit, or returns at once if it is already up. */
    fun ensureStarted() {
        if (started) return
        synchronized(startupLock) {
            if (started) return
            // The packaged app runs a Compose/Skia window on the same display;
            // on machines with weak or conflicting GPU drivers the JavaFX WebView
            // then stays blank white even though the page loaded (paint never
            // happens). The WebView is the only JavaFX surface we have, so force
            // the software renderer: slower but guaranteed to paint.
            runCatching {
                if (System.getProperty("prism.order") == null) {
                    System.setProperty("prism.order", "sw")
                }
                if (System.getProperty("prism.dirtyopts") == null) {
                    System.setProperty("prism.dirtyopts", "false")
                }
            }
            val failure = arrayOfNulls<Throwable>(1)
            val ready = CountDownLatch(1)
            Thread {
                try {
                    FxPlatform.startup { ready.countDown() }
                } catch (t: IllegalStateException) {
                    // Toolkit was started by another component between the
                    // check and startup; it is safe to use runLater now.
                    ready.countDown()
                } catch (t: Throwable) {
                    failure[0] = t
                    ready.countDown()
                }
            }.apply { name = "vivimusic-javafx-startup"; isDaemon = true }.start()
            if (!ready.await(15, TimeUnit.SECONDS)) {
                throw IllegalStateException("JavaFX toolkit startup timed out")
            }
            failure[0]?.let { throw it }
            started = true
        }
    }
}
