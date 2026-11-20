package com.music.vivi.desktop.data.youtube

import javafx.application.Platform
import javafx.concurrent.Worker
import javafx.geometry.Insets
import javafx.scene.Scene
import javafx.scene.control.Button
import javafx.scene.control.Label
import javafx.scene.layout.BorderPane
import javafx.scene.layout.HBox
import javafx.scene.layout.Priority
import javafx.scene.web.WebView
import javafx.stage.Stage
import java.net.CookieHandler
import java.net.CookieManager
import java.net.CookiePolicy
import java.util.concurrent.CountDownLatch
import java.util.concurrent.atomic.AtomicBoolean

/**
 * The in-app YouTube Music sign-in window.
 *
 * `innertube` signs a request with a cookie jar, so the whole flow is: open
 * music.youtube.com in a WebView, let the user sign in on Google's own page,
 * then read the youtube.com cookies and hand them to [YouTubeAccount]. The
 * cookie jar is in memory only and the window is closed as soon as the session
 * is adopted, so no browser profile is modified.
 *
 * Google sometimes refuses an embedded browser. The card keeps the browser
 * cookie-jar path as the fallback, and the window says so instead of pretending
 * the sign-in failed for another reason.
 */
object YouTubeWebLogin {

    private const val START_URL = "https://music.youtube.com/"
    private const val CHROME_USER_AGENT =
        "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 " +
            "(KHTML, like Gecko) Chrome/126.0.0.0 Safari/537.36"

    private val toolkitStarted = AtomicBoolean(false)
    private val cookies = CookieManager(null, CookiePolicy.ACCEPT_ALL)

    @Volatile
    private var window: Stage? = null

    /** Opens the window; [onResult] runs on the JavaFX thread once the user adopts a session. */
    fun open(onResult: (Result<String>) -> Unit) {
        val failure = ensureToolkit()
        if (failure != null) {
            onResult(Result.failure(failure))
            return
        }
        Platform.runLater {
            try {
                show(onResult = onResult, closed = null)
            } catch (error: Throwable) {
                onResult(Result.failure(error))
            }
        }
    }

    /**
     * Opens the window without capturing anything and returns a latch that counts
     * down when it closes. Used by `--youtube-webview-check` to prove the Google
     * page loads in this build.
     */
    fun openForCheck(): CountDownLatch {
        val closed = CountDownLatch(1)
        val failure = ensureToolkit()
        if (failure != null) {
            System.err.println("[weblogin] $failure")
            closed.countDown()
            return closed
        }
        Platform.runLater {
            try {
                show(
                    onResult = { result ->
                        System.out.println(
                            "[weblogin] " + (result.exceptionOrNull()?.message ?: "session available")
                        )
                    },
                    closed = closed
                )
            } catch (error: Throwable) {
                System.err.println("[weblogin] ${error.message}")
                closed.countDown()
            }
        }
        return closed
    }

    private fun ensureToolkit(): Throwable? {
        if (!toolkitStarted.compareAndSet(false, true)) return null
        return try {
            Platform.startup {
                Platform.setImplicitExit(false)
            }
            null
        } catch (_: IllegalStateException) {
            // The toolkit was already initialized (another JavaFX window or a test).
            null
        } catch (error: Throwable) {
            error
        }
    }

    private fun show(onResult: (Result<String>) -> Unit, closed: CountDownLatch?) {
        if (CookieHandler.getDefault() !is CookieManager) {
            CookieHandler.setDefault(cookies)
        }

        val engineView = WebView()
        val engine = engineView.engine
        engine.userAgent = CHROME_USER_AGENT

        val status = Label("Sign in on the page below, then choose \"Use this session\".")
        status.isWrapText = true
        HBox.setHgrow(status, Priority.ALWAYS)

        val adopt = Button("Use this session")
        adopt.isDefaultButton = true
        val close = Button("Close")

        val bar = HBox(12.0, status, adopt, close)
        bar.padding = Insets(10.0, 12.0, 10.0, 12.0)
        bar.style = "-fx-alignment: center-left;"

        val root = BorderPane(engineView)
        root.top = bar

        val stage = Stage()
        stage.title = "VIVI Music DE - YouTube Music sign-in"
        stage.scene = Scene(root, 1120.0, 820.0)
        stage.setOnHidden {
            if (window === stage) window = null
            closed?.countDown()
        }

        fun readSession() {
            val now = System.currentTimeMillis()
            val jar = cookies.cookieStore.cookies
                .filter { cookie ->
                    val domain = cookie.domain?.lowercase().orEmpty()
                    domain.contains("youtube.com") && !cookie.hasExpired() && cookie.value.isNotBlank()
                }
                .joinToString("; ") { "${it.name}=${it.value}" }
            if (!BrowserCookies.hasSigningSecret(jar)) {
                status.text =
                    "No YouTube session yet. Sign in above, then choose \"Use this session\" again. " +
                        "If Google refuses this window, close it and use \"Find a signed-in browser\"."
                return
            }
            onResult(Result.success(jar))
            stage.close()
        }

        adopt.setOnAction { readSession() }
        close.setOnAction { stage.close() }
        engine.loadWorker.stateProperty().addListener { _, _, state ->
            if (state == Worker.State.SUCCEEDED) {
                System.out.println("[weblogin] loaded ${engine.location}")
            } else if (state == Worker.State.FAILED) {
                status.text = "The page could not be loaded: ${engine.loadWorker.exception?.message.orEmpty()}"
            }
        }
        engine.load(START_URL)

        window = stage
        stage.show()
        stage.toFront()
    }
}
