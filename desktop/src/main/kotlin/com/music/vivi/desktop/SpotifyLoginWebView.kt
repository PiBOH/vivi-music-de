package com.music.vivi.desktop

import com.music.spotify.SpotifyAuth
import javafx.application.Platform as FxPlatform
import javafx.concurrent.Worker
import javafx.geometry.Insets
import javafx.scene.Scene
import javafx.scene.control.Label
import javafx.scene.control.ProgressIndicator
import javafx.scene.layout.Background
import javafx.scene.layout.BackgroundFill
import javafx.scene.layout.CornerRadii
import javafx.scene.layout.HBox
import javafx.scene.layout.Priority
import javafx.scene.layout.VBox
import javafx.scene.paint.Color
import javafx.scene.web.WebView
import javafx.stage.Stage
import java.net.CookieHandler
import java.net.CookieManager
import java.net.HttpCookie
import java.net.URI

/**
 * Embedded Spotify sign-in window — the desktop port of the phone's
 * `SpotifyLoginSheet`.
 *
 * Spotify's web player authenticates with the `sp_dc` cookie, and the only
 * supported way to get one is to sign in on Spotify's own page: there is no
 * OAuth flow behind the import. The phone therefore opens
 * `SpotifyAuth.LOGIN_URL` in a `WebView`, lets the user type their credentials
 * there, and reads `sp_dc` / `sp_key` off that WebView's cookie store as soon as
 * the page it lands on has them. This is the same window, in JavaFX, and it
 * replaces the "paste the cookie you found in your browser's developer tools"
 * field the desktop shipped first — a step that asked the user to do what this
 * window does for them.
 *
 * The cookie store is JavaFX's ([CookieHandler]): WebView hands its cookies to
 * the `java.net.CookieManager` installed as the default handler, which is what
 * [LoginWebView] already relies on for the YouTube session. Capture asks that
 * handler the same question the phone asks `CookieManager.getCookie(
 * "https://open.spotify.com")`: *which cookies would you send to the web
 * player?* — a raw dump of the store would also hand over cookies scoped to
 * other hosts, and `sp_dc` for the wrong host is not a session.
 *
 * If the window cannot be created at all (no JavaFX, no display) the caller is
 * told so and the screen offers the manual paste it used to be the only way —
 * [isUnavailable] is that answer.
 */
internal object SpotifyLoginWebView {

    /** What the window hands back: the cookies the web player authenticates with. */
    data class Capture(val spDc: String, val spKey: String)

    /** The origin the capture is scoped to, exactly as the phone scopes it. */
    private const val SPOTIFY_ORIGIN = "https://open.spotify.com"

    /**
     * The web player is served differently per platform and Spotify refuses
     * unknown clients; the window therefore announces a real Chrome, per OS,
     * the way the app's own Spotify requests already do
     * (`SpotifyAuth.USER_AGENT`). The phone pins an *Android* Chrome string for
     * the same reason: the WebView's default UA is not a browser Spotify
     * supports, while a desktopping one is.
     */
    private val userAgent: String = when (Platform.os) {
        DesktopOs.WINDOWS -> "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/131.0.0.0 Safari/537.36"
        DesktopOs.MACOS -> "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/131.0.0.0 Safari/537.36"
        DesktopOs.LINUX -> "Mozilla/5.0 (X11; Linux x86_64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/131.0.0.0 Safari/537.36"
    }

    @Volatile private var windowOpen = false
    @Volatile private var unavailable = false
    @Volatile private var delivered = false

    /** False once a window could not be created: the screen then offers the paste fallback. */
    val isUnavailable: Boolean get() = unavailable

    fun isWindowOpen(): Boolean = windowOpen

    /**
     * Opens the sign-in window and calls [onCaptured] once — with the cookies the
     * page signed in with, or with null if the user closed the window without
     * signing in. Returns false when no window could be created at all.
     */
    fun open(language: String, onCaptured: (Capture?) -> Unit): Boolean {
        if (unavailable || windowOpen) return !unavailable
        return try {
            if (CookieHandler.getDefault() !is CookieManager) {
                CookieHandler.setDefault(CookieManager())
            }
            dropStaleSpotifyCookies()
            windowOpen = true
            delivered = false
            JavaFxToolkit.ensureStarted()
            FxPlatform.runLater { createWindow(language, onCaptured) }
            true
        } catch (t: Throwable) {
            windowOpen = false
            unavailable = true
            AppLog.log("spotify", "the sign-in window is unavailable: $t")
            deliver(null, onCaptured)
            false
        }
    }

    /**
     * Signs the previous account out before the new one signs in.
     *
     * The phone calls `CookieManager.removeAllCookies` before loading the page,
     * and this is that step scoped to Spotify: the YouTube window shares the same
     * cookie store, so clearing it wholesale would throw away a session that has
     * nothing to do with this one — and a leftover `sp_dc` would be captured
     * before the user ever typed anything.
     */
    private fun dropStaleSpotifyCookies() {
        val store = (CookieHandler.getDefault() as? CookieManager)?.cookieStore ?: return
        val stale = store.cookies.filter { it.isSpotifyCookie() }
        val removed = stale.count { cookie ->
            runCatching { store.remove(URI(SPOTIFY_ORIGIN), cookie) }.getOrDefault(false)
        }
        if (removed > 0) {
            AppLog.log("spotify", "cleared $removed stale Spotify cookie(s) before sign-in")
        }
    }

    private fun deliver(capture: Capture?, callback: (Capture?) -> Unit) {
        if (delivered) return
        delivered = true
        runCatching { callback(capture) }
    }

    private fun createWindow(language: String, callback: (Capture?) -> Unit) {
        try {
            val stage = Stage()
            val status = Label(Localization.get(language, "login_waiting"))
            val spinner = ProgressIndicator().apply {
                prefWidth = 18.0
                prefHeight = 18.0
            }
            val header = HBox(10.0, spinner, status).apply {
                padding = Insets(10.0, 14.0, 10.0, 14.0)
                background = Background(BackgroundFill(Color.web("#1f1f2e"), CornerRadii.EMPTY, Insets.EMPTY))
            }
            val browser = WebView().apply {
                prefWidth = 980.0
                prefHeight = 700.0
                minWidth = 420.0
                minHeight = 320.0
                engine.userAgent = userAgent
                engine.load(SpotifyAuth.LOGIN_URL)
            }
            val root = VBox(header, browser).apply {
                VBox.setVgrow(browser, Priority.ALWAYS)
            }
            stage.title = "VIVI Music DE — ${Localization.get(language, "spotify_open_login")}"
            stage.scene = Scene(root, 980.0, 760.0)
            stage.setOnCloseRequest {
                // Closed without finishing: hand back whatever is there (null
                // when nothing is), so the screen can drop its spinner.
                windowOpen = false
                deliver(capture(), callback)
            }
            stage.show()

            // Kick the WebView so it paints its first frame. In a process where
            // Compose/AWT already owns the display, the WebView can stay blank
            // (known JavaFX painting bug) until it is nudged: force a re-layout
            // once the page starts loading, and again on load success.
            browser.engine.loadWorker.stateProperty().addListener { _, _, newState ->
                val loaded = newState == Worker.State.SUCCEEDED
                val running = newState == Worker.State.RUNNING
                if (loaded || running) {
                    FxPlatform.runLater {
                        browser.resize(browser.width + 1.0, browser.height)
                        browser.resize(browser.width - 1.0, browser.height)
                        browser.requestLayout()
                    }
                }
            }

            // The window is its own deadline: it stays open until the user
            // finishes, and closing it ends the poll through `windowOpen`.
            Thread {
                while (windowOpen && !delivered) {
                    val captured = capture()
                    if (captured != null) {
                        FxPlatform.runLater {
                            spinner.isVisible = false
                            status.text = Localization.get(language, "login_saving")
                        }
                        AppLog.log(
                            "spotify",
                            "sign-in captured sp_dc (sp_key ${if (captured.spKey.isBlank()) "absent" else "present"})",
                        )
                        deliver(captured, callback)
                        FxPlatform.runLater { stage.close() }
                        break
                    }
                    Thread.sleep(1000)
                }
            }.apply { name = "vivimusic-spotify-cookie-poll"; isDaemon = true }.start()
        } catch (t: Throwable) {
            windowOpen = false
            unavailable = true
            AppLog.log("spotify", "the sign-in window could not be created: $t")
            deliver(null, callback)
        }
    }

    /**
     * The `sp_dc` / `sp_key` pair the web player would be sent right now, or null
     * while there is no session yet.
     *
     * The scoped lookup is the primary source (it applies the domain, path and
     * secure rules exactly as the browser does); the store scan behind it is the
     * backstop for a cookie the handler does not return for that origin, which is
     * the same shape of fallback the YouTube capture carries.
     */
    private fun capture(): Capture? {
        val manager = CookieHandler.getDefault() as? CookieManager ?: return null
        val header = runCatching {
            manager.get(URI("$SPOTIFY_ORIGIN/"), emptyMap<String, List<String>>())["Cookie"]
                ?.firstOrNull()
        }.getOrNull().orEmpty()
        val scoped = header.split(';')
            .mapNotNull { part ->
                val equals = part.indexOf('=')
                if (equals <= 0) null
                else part.substring(0, equals).trim().lowercase() to part.substring(equals + 1).trim()
            }
            .toMap()
        val store = manager.cookieStore.cookies
            .filter { it.isSpotifyCookie() }
            .associateBy { it.name.lowercase() }

        val spDc = scoped["sp_dc"]?.takeIf { it.isNotBlank() }
            ?: store["sp_dc"]?.value?.takeIf { it.isNotBlank() }
            ?: return null
        val spKey = scoped["sp_key"]?.takeIf { it.isNotBlank() }
            ?: store["sp_key"]?.value.orEmpty()
        return Capture(spDc, spKey)
    }

    private fun HttpCookie.isSpotifyCookie(): Boolean =
        domain?.removePrefix(".")?.lowercase()?.endsWith("spotify.com") == true
}
