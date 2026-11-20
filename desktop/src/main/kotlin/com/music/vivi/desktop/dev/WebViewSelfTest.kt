package com.music.vivi.desktop.dev

import javafx.application.Platform as FxPlatform
import javafx.concurrent.Worker
import javafx.scene.Scene
import javafx.scene.web.WebView
import javafx.stage.Stage
import java.awt.image.BufferedImage
import java.io.File
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicReference
import javax.imageio.ImageIO
import kotlin.system.exitProcess

/**
 * `--webview-self-test`: the packaged WebView check a release runs.
 *
 * The in-app sign-in window (`--youtube-webview-check`) waits for a person to
 * close it, so it cannot be part of an unattended release check. This harness
 * opens the same engine on the same Google page, waits for the load to finish,
 * reads the document back and closes itself: it exits 0 only when the page
 * really rendered text, so a packaged runtime that lost `java.net.http` (the
 * historical `NoClassDefFoundError`) or JavaFX's own natives fails here instead
 * of on a user's machine.
 *
 * Options: `--webview-timeout=<seconds>` (default 120, clamped to 15..600) and
 * `--webview-snapshot=<file.png>` for the frame the window painted.
 */
object WebViewSelfTest {

    /** The page the 1.55.0 sign-in window opens, kept identical on purpose. */
    private const val LOGIN_URL =
        "https://accounts.google.com/ServiceLogin?continue=https%3A%2F%2Fmusic.youtube.com%2F"

    private data class PageInfo(val readyState: String, val title: String, val chars: Int)

    fun handle(args: Array<String>) {
        val flag = args.firstOrNull {
            it == "--webview-self-test" || it.startsWith("--webview-self-test=")
        } ?: return

        val timeoutSeconds = args.firstOrNull { it.startsWith("--webview-timeout=") }
            ?.substringAfter('=')
            ?.toLongOrNull()
            ?.coerceIn(15L, 600L)
            ?: 120L
        val snapshotPath = args.firstOrNull { it.startsWith("--webview-snapshot=") }
            ?.substringAfter('=')
            ?.takeIf { it.isNotBlank() }

        println("WEBVIEW: self test, timeout ${timeoutSeconds}s, snapshot=${snapshotPath ?: "none"}")
        println("WEBVIEW: java=${System.getProperty("java.version")} home=${System.getProperty("user.home")}")
        val ok = try {
            run(timeoutSeconds, snapshotPath?.let(::File))
        } catch (t: Throwable) {
            println("WEBVIEW: error ${t::class.java.name}: ${t.message}")
            false
        }
        println(if (ok) "RESULT: PASS" else "RESULT: FAIL")
        System.out.flush()
        exitProcess(if (ok) 0 else 1)
    }

    private fun run(timeoutSeconds: Long, snapshot: File?): Boolean {
        // The sign-in window forces the software renderer for the same reason:
        // a JavaFX WebView can paint a blank surface next to Compose/Skia.
        if (System.getProperty("prism.order") == null) System.setProperty("prism.order", "sw")

        val webViewJar = runCatching {
            WebView::class.java.protectionDomain?.codeSource?.location?.toURI()?.let { File(it).path }
        }.getOrNull()
        println("WEBVIEW: engine=${webViewJar ?: "unknown"}")

        startToolkit()

        val closed = CountDownLatch(1)
        var loaded = false
        var reason = "no page state"
        val stageRef = AtomicReference<Stage?>(null)

        FxPlatform.runLater {
            try {
                val browser = WebView().apply {
                    prefWidth = 1000.0
                    prefHeight = 640.0
                    minWidth = 400.0
                    minHeight = 300.0
                    engine.userAgent = userAgent()
                    engine.load(LOGIN_URL)
                }
                val window = Stage()
                window.title = "VIVI Music DE - WebView self test"
                window.scene = Scene(browser, 1000.0, 720.0)
                window.setOnHidden { closed.countDown() }
                window.show()
                stageRef.set(window)

                browser.engine.loadWorker.stateProperty().addListener { _, _, state ->
                    when (state) {
                        Worker.State.SUCCEEDED -> {
                            val page = readPage(browser)
                            println(
                                "WEBVIEW: state=SUCCEEDED location=${browser.engine.location} " +
                                    "title=${page.title} readyState=${page.readyState} chars=${page.chars}"
                            )
                            if (page.chars > 0) {
                                loaded = true
                            } else {
                                reason = "the page loaded but painted no text"
                            }
                            snapshot?.let { target ->
                                runCatching { writePng(browser, target) }
                                    .onSuccess { println("WEBVIEW: snapshot=$target") }
                                    .onFailure { println("WEBVIEW: snapshot failed: ${it.message}") }
                            }
                            window.close()
                        }

                        Worker.State.FAILED, Worker.State.CANCELLED -> {
                            val failure = browser.engine.loadWorker.exception
                            println(
                                "WEBVIEW: state=$state " +
                                    "${failure?.let { it::class.java.name + ": " + it.message }.orEmpty()}"
                            )
                            reason = "the page did not load ($state)"
                            window.close()
                        }

                        else -> Unit
                    }
                }
            } catch (t: Throwable) {
                println("WEBVIEW: window failed: ${t::class.java.name}: ${t.message}")
                reason = "the window could not be created"
                closed.countDown()
            }
        }

        val finished = closed.await(timeoutSeconds, TimeUnit.SECONDS)
        if (!finished) {
            println("WEBVIEW: timeout after ${timeoutSeconds}s")
            reason = "timeout"
            runCatching { FxPlatform.runLater { stageRef.get()?.close() } }
            closed.await(5, TimeUnit.SECONDS)
        }
        if (!loaded) println("WEBVIEW: $reason")
        return loaded
    }

    private fun startToolkit() {
        val ready = CountDownLatch(1)
        val failure = arrayOfNulls<Throwable>(1)
        Thread {
            try {
                FxPlatform.startup {
                    FxPlatform.setImplicitExit(false)
                    ready.countDown()
                }
            } catch (e: IllegalStateException) {
                ready.countDown()
            } catch (t: Throwable) {
                failure[0] = t
                ready.countDown()
            }
        }.apply { name = "vivimusic-webview-selftest"; isDaemon = true }.start()
        if (!ready.await(20, TimeUnit.SECONDS)) {
            throw IllegalStateException("JavaFX toolkit startup timed out")
        }
        failure[0]?.let { throw it }
    }

    private fun readPage(browser: WebView): PageInfo {
        fun js(script: String): String =
            runCatching { browser.engine.executeScript(script) as? String }
                .getOrNull()
                .orEmpty()
        val readyState = js("document.readyState")
        val title = js("document.title")
        val chars = runCatching {
            (browser.engine.executeScript(
                "(function(){try{return (document.body ? document.body.innerText.length : 0)}catch(e){return 0}})()"
            ) as? Number)?.toInt() ?: 0
        }.getOrDefault(0)
        return PageInfo(readyState, title, chars)
    }

    /** The same desktop Chrome identity the sign-in window announces. */
    private fun userAgent(): String = when {
        System.getProperty("os.name").orEmpty().startsWith("Mac", ignoreCase = true) ->
            "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/537.36 Chrome/131 Safari/537.36"
        System.getProperty("os.name").orEmpty().startsWith("Linux", ignoreCase = true) ->
            "Mozilla/5.0 (X11; Linux x86_64) AppleWebKit/537.36 Chrome/131 Safari/537.36"
        else ->
            "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 Chrome/131 Safari/537.36"
    }

    /** Writes the painted frame without `javafx.swing`, which is not packaged. */
    private fun writePng(browser: WebView, target: File) {
        val image = browser.snapshot(null, null)
        val width = image.width.toInt()
        val height = image.height.toInt()
        if (width <= 0 || height <= 0) return
        val reader = image.pixelReader ?: return
        val buffered = BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB)
        for (y in 0 until height) {
            for (x in 0 until width) {
                buffered.setRGB(x, y, reader.getArgb(x, y))
            }
        }
        target.parentFile?.mkdirs()
        ImageIO.write(buffered, "png", target)
    }
}
