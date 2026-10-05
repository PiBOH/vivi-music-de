package com.music.vivi.desktop

import com.music.spotify.SpotifyAuth
import me.friwi.jcefmaven.CefAppBuilder
import me.friwi.jcefmaven.EnumProgress
import me.friwi.jcefmaven.MavenCefAppHandlerAdapter
import org.cef.CefApp
import org.cef.CefApp.CefAppState
import org.cef.CefClient
import org.cef.browser.CefBrowser
import org.cef.browser.CefFrame
import org.cef.callback.CefCookieVisitor
import org.cef.handler.CefDisplayHandlerAdapter
import org.cef.handler.CefLoadHandlerAdapter
import org.cef.network.CefCookie
import org.cef.network.CefCookieManager
import java.awt.BorderLayout
import java.awt.Color
import java.awt.Dimension
import java.awt.FlowLayout
import java.awt.Font
import java.io.File
import java.net.URI
import java.util.Collections
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import javax.swing.BorderFactory
import javax.swing.JButton
import javax.swing.JFrame
import javax.swing.JPanel
import javax.swing.JProgressBar
import javax.swing.JTextArea
import javax.swing.SwingUtilities
import javax.swing.WindowConstants

/**
 * Embedded Spotify sign-in window, backed by Chromium Embedded Framework (JCEF)
 * instead of the JavaFX `WebEngine` [SpotifyLoginWebView] used since 1.54.5.
 *
 * Why a real Chromium: Google refuses its own sign-in page inside a browser
 * engine it recognises as embedded (`disallowed_useragent`, issue #97), and the
 * JavaFX `WebEngine` has no supported way out of that. CEF is the Chromium
 * engine itself, so the window is a real Chrome to the page rather than a shim
 * pretending to be one. **Constraint, unchanged:** Google can still refuse, and
 * when it does the window keeps the honest message and the **Open it in your
 * browser** link, with the `sp_dc` / `sp_key` paste on the screen behind it. The
 * JavaFX window stays as the fallback for a machine where CEF cannot start.
 *
 * The cookies are read from CEF's own store ([CefCookieManager]) — a different
 * store from the JavaFX window's `java.net.CookieManager` — scoped to
 * `https://open.spotify.com`, the same question the phone and the JavaFX window
 * ask. CEF delivers a visit asynchronously on its own UI thread, so each visit
 * is awaited before the captured pair is read.
 */
internal object SpotifyLoginJcef {

    /** The origin the capture is scoped to, exactly as the other window scopes it. */
    private const val SPOTIFY_ORIGIN = "https://open.spotify.com"

    /** Default window size when nothing has been remembered yet. */
    private const val DEFAULT_WIDTH = 1000
    private const val DEFAULT_HEIGHT = 800

    /** Where jcefmaven unpacks the CEF natives (downloaded on first use). */
    private val installDir: File = File(System.getProperty("user.home"), ".vivimusic/cef")

    @Volatile private var windowOpen = false
    @Volatile private var unavailable = false
    @Volatile private var delivered = false
    @Volatile private var cefApp: CefApp? = null

    /** True once JCEF could not be started: the caller then uses the JavaFX window. */
    val isUnavailable: Boolean get() = unavailable

    fun isWindowOpen(): Boolean = windowOpen

    /**
     * Opens the JCEF sign-in window. Returns false, synchronously, when JCEF is
     * already known not to start, so [SpotifyLoginWebView] can fall back to its
     * JavaFX window.
     *
     * The window appears at once (with the CEF preparation progress) and the
     * engine is started behind it, because the first run downloads and unpacks
     * about 100 MB and a silent wait looked like a hang.
     */
    fun open(language: String, onCaptured: (SpotifyLoginWebView.Capture?) -> Unit): Boolean {
        if (unavailable) return false
        if (windowOpen) return true
        windowOpen = true
        delivered = false
        SwingUtilities.invokeLater { showWindow(language, onCaptured) }
        return true
    }

    private fun showWindow(language: String, callback: (SpotifyLoginWebView.Capture?) -> Unit) {
        try {
            val saved = DesktopSettings.load()
            val width = saved.spotifyLoginWindowWidth.takeIf { it >= 480 } ?: DEFAULT_WIDTH
            val height = saved.spotifyLoginWindowHeight.takeIf { it >= 400 } ?: DEFAULT_HEIGHT

            // A two-line, wrapping status: the "Google blocked" sentence is long,
            // and a single-line label used to push the two buttons off the panel
            // (they came out clipped and unusable).
            val status = JTextArea(Localization.get(language, "login_waiting")).apply {
                isEditable = false
                isOpaque = false
                lineWrap = true
                wrapStyleWord = true
                rows = 2
                columns = 40
                foreground = Color(0xE6, 0xE1, 0xE5)
                font = font.deriveFont(Font.PLAIN, 13f)
                border = BorderFactory.createEmptyBorder()
            }
            val progress = JProgressBar(0, 100).apply {
                isStringPainted = true
                isVisible = true
                value = 0
                foreground = Color(0xD0, 0xBC, 0xFF)
                background = Color(0x33, 0x33, 0x44)
                preferredSize = Dimension(240, 16)
            }
            val retry = JButton(Localization.get(language, "retry")).apply {
                isVisible = false
                isFocusable = false
            }
            val browserLink = JButton(Localization.get(language, "spotify_open_browser")).apply {
                isVisible = false
                isFocusable = false
            }
            // The buttons sit on their own row under the status, so a long status
            // can never clip them.
            val buttonRow = JPanel(FlowLayout(FlowLayout.LEFT, 8, 0)).apply {
                isOpaque = false
                add(retry)
                add(browserLink)
            }
            val header = JPanel(BorderLayout(8, 6)).apply {
                background = Color(0x1F, 0x1F, 0x2E)
                border = BorderFactory.createEmptyBorder(10, 14, 10, 14)
                add(status, BorderLayout.CENTER)
                add(buttonRow, BorderLayout.SOUTH)
                add(progress, BorderLayout.EAST)
            }
            val center = JPanel(BorderLayout()).apply { background = Color(0x1F, 0x1F, 0x2E) }

            val frame = JFrame("VIVI Music DE — ${Localization.get(language, "spotify_open_login")}").apply {
                defaultCloseOperation = WindowConstants.DO_NOTHING_ON_CLOSE
                layout = BorderLayout()
                contentPane.add(header, BorderLayout.NORTH)
                contentPane.add(center, BorderLayout.CENTER)
                setSize(width, height)
                if (saved.spotifyLoginWindowX >= 0 && saved.spotifyLoginWindowY >= 0) {
                    setLocation(saved.spotifyLoginWindowX, saved.spotifyLoginWindowY)
                } else {
                    setLocationRelativeTo(null)
                }
            }
            frame.addWindowListener(object : java.awt.event.WindowAdapter() {
                override fun windowClosing(e: java.awt.event.WindowEvent) {
                    // Closed without finishing: hand back whatever is there (a
                    // session captured just before the close included), so the
                    // screen can use it and drop its spinner.
                    rememberBounds(frame)
                    windowOpen = false
                    deliver(capture(), callback)
                    frame.isVisible = false
                    frame.dispose()
                }
            })
            frame.isVisible = true

            // Start CEF behind the window, driving the progress bar with the real
            // download/extract percentage jcefmaven reports.
            Thread({
                try {
                    val app = cefApp ?: buildApp { phase, value ->
                        SwingUtilities.invokeLater {
                            if (value < 0f) {
                                progress.isIndeterminate = true
                            } else {
                                progress.isIndeterminate = false
                                progress.value = value.toInt().coerceIn(0, 100)
                            }
                            progress.toolTipText = phase.name.lowercase()
                        }
                    }.also { cefApp = it }
                    SwingUtilities.invokeLater {
                        attachBrowser(app, frame, center, progress, status, retry, browserLink, language, callback)
                    }
                } catch (t: Throwable) {
                    windowOpen = false
                    unavailable = true
                    AppLog.log("spotify", "JCEF could not start, falling back to the JavaFX window: $t")
                    SwingUtilities.invokeLater { frame.isVisible = false; frame.dispose() }
                    // Fall back in the same attempt, not on the user's next press:
                    // with JCEF now unavailable, this runs the JavaFX window.
                    SpotifyLoginWebView.open(language, callback)
                }
            }, "vivimusic-spotify-jcef-init").apply { isDaemon = true }.start()
        } catch (t: Throwable) {
            windowOpen = false
            unavailable = true
            AppLog.log("spotify", "the JCEF sign-in window could not be created: $t")
            SpotifyLoginWebView.open(language, callback)
        }
    }

    private fun attachBrowser(
        app: CefApp,
        frame: JFrame,
        center: JPanel,
        progress: JProgressBar,
        status: JTextArea,
        retry: JButton,
        browserLink: JButton,
        language: String,
        callback: (SpotifyLoginWebView.Capture?) -> Unit,
    ) {
        try {
            val client: CefClient = app.createClient()
            val browser: CefBrowser = client.createBrowser(SpotifyAuth.LOGIN_URL, false, false)
            val browserUi = browser.uiComponent.apply {
                minimumSize = Dimension(420, 320)
            }
            center.removeAll()
            center.add(browserUi, BorderLayout.CENTER)
            center.revalidate()
            center.repaint()
            browser.setFocus(true)

            // A running page load shows an indeterminate bar; the real percentage
            // belongs to the CEF preparation above, not to a page CEF does not
            // report progress for.
            client.addLoadHandler(object : CefLoadHandlerAdapter() {
                override fun onLoadingStateChange(b: CefBrowser?, isLoading: Boolean, canGoBack: Boolean, canGoForward: Boolean) {
                    SwingUtilities.invokeLater {
                        progress.isVisible = isLoading
                        progress.isIndeterminate = isLoading
                        if (!isLoading) progress.value = 100
                    }
                    if (!isLoading) {
                        // One diagnostic line per finished load, so a window that
                        // does not capture can be told from one that never loaded.
                        // No cookie visit here: this runs on CEF's own UI thread,
                        // which a blocking visit would stall.
                        AppLog.log("spotify", "JCEF page loaded (${b?.url})")
                    }
                }
            })

            // Google's block page is served from a Google host: seeing it is the
            // signal that this window's sign-in is over, and the browser link is
            // the way in Google has no say in.
            client.addDisplayHandler(object : CefDisplayHandlerAdapter() {
                override fun onAddressChange(b: CefBrowser?, f: CefFrame?, url: String?) {
                    if (!isGoogleSignInHost(url)) return
                    AppLog.log("spotify", "Google refused its sign-in page inside the JCEF window ($url)")
                    SwingUtilities.invokeLater {
                        status.text = Localization.get(language, "spotify_google_blocked")
                        retry.isVisible = true
                        browserLink.isVisible = true
                    }
                }
            })

            browserLink.addActionListener {
                AppLog.click("Spotify sign-in (JCEF): opened Spotify in the browser after Google blocked the window")
                openUrl(SpotifyAuth.LOGIN_URL)
                status.text = Localization.get(language, "spotify_cookie_hint")
            }
            retry.addActionListener {
                AppLog.click("Spotify sign-in (JCEF): back to Spotify's own form after Google blocked the window")
                retry.isVisible = false
                browserLink.isVisible = false
                status.text = Localization.get(language, "login_waiting")
                browser.loadURL(SpotifyAuth.LOGIN_URL)
            }

            // The window is its own deadline: closing it ends the poll through
            // `windowOpen`.
            Thread({
                while (windowOpen && !delivered) {
                    val captured = capture()
                    if (captured != null) {
                        SwingUtilities.invokeLater { status.text = Localization.get(language, "login_saving") }
                        AppLog.log(
                            "spotify",
                            "JCEF sign-in captured sp_dc (sp_key ${if (captured.spKey.isBlank()) "absent" else "present"})",
                        )
                        deliver(captured, callback)
                        SwingUtilities.invokeLater {
                            rememberBounds(frame)
                            frame.isVisible = false
                            frame.dispose()
                        }
                        break
                    }
                    Thread.sleep(1000)
                }
            }, "vivimusic-spotify-jcef-cookie-poll").apply { isDaemon = true }.start()
        } catch (t: Throwable) {
            windowOpen = false
            unavailable = true
            AppLog.log("spotify", "the JCEF sign-in window could not be created: $t")
            // Take the empty frame down before the JavaFX fallback opens, so a
            // failure here does not leave two sign-in windows on screen.
            SwingUtilities.invokeLater {
                frame.isVisible = false
                frame.dispose()
            }
            SpotifyLoginWebView.open(language, callback)
        }
    }

    /**
     * Builds (once) the global [CefApp]. The first call downloads and unpacks the
     * native CEF bundle under [installDir], so it is deliberately off the UI
     * thread and reports its progress. Windowed rendering (not off-screen) is
     * what a Swing-embedded browser needs.
     */
    private fun buildApp(onProgress: (EnumProgress, Float) -> Unit): CefApp {
        val builder = CefAppBuilder()
        builder.setInstallDir(installDir)
        builder.setProgressHandler { progress, value -> onProgress(progress, value) }
        builder.getCefSettings().windowless_rendering_enabled = false
        builder.getCefSettings().cache_path = File(installDir, "cache").absolutePath
        builder.getCefSettings().user_agent = SpotifyLoginWebView.userAgent
        // builder.setAppHandler, never CefApp.addAppHandler: the latter breaks on
        // macOS. Mirrors the sample app.
        builder.setAppHandler(object : MavenCefAppHandlerAdapter() {
            override fun stateHasChanged(state: CefAppState) {
                if (state == CefAppState.TERMINATED) cefApp = null
            }
        })
        return builder.build()
    }

    /**
     * Remembers the size/position the user left the window at, so the next
     * opening restores it. Written on close (by hand or by the automatic close
     * on capture) rather than on every resize event, which would write the
     * settings file in a storm; an iconified or not-showing window is skipped
     * because its bounds would be the platform's nonsense values.
     */
    private fun rememberBounds(frame: JFrame) {
        if (!frame.isShowing || frame.width < 200 || frame.height < 200) return
        DesktopSettings.update {
            it.copy(
                spotifyLoginWindowWidth = frame.width,
                spotifyLoginWindowHeight = frame.height,
                spotifyLoginWindowX = frame.x,
                spotifyLoginWindowY = frame.y,
            )
        }
    }

    private fun deliver(capture: SpotifyLoginWebView.Capture?, callback: (SpotifyLoginWebView.Capture?) -> Unit) {
        if (delivered) return
        delivered = true
        // The window is done with, whatever it is done for. Clearing `windowOpen`
        // here is what lets the button open a second window in the same session.
        windowOpen = false
        runCatching { callback(capture) }
    }

    /**
     * The `sp_dc` / `sp_key` pair CEF would send to the web player right now, or
     * null while there is no session yet.
     *
     * The scoped visit is the primary source (it applies the domain, path and
     * secure rules exactly as the browser does); a full-store visit filtered to
     * `spotify.com` is the backstop for a cookie the scoped visit does not
     * return. Both are awaited, because CEF runs the visitor on its own UI
     * thread after the call returns.
     */
    private fun capture(): SpotifyLoginWebView.Capture? {
        val scoped = collectCookies("$SPOTIFY_ORIGIN/", true)
        findCookie(scoped, "sp_dc")?.let { dc ->
            return SpotifyLoginWebView.Capture(dc, findCookie(scoped, "sp_key").orEmpty())
        }
        val all = collectCookies(null, true).filter { cookie ->
            val domain = cookie.domain?.removePrefix(".")?.lowercase()
            domain == "spotify.com" || domain?.endsWith(".spotify.com") == true
        }
        val dc = findCookie(all, "sp_dc") ?: return null
        return SpotifyLoginWebView.Capture(dc, findCookie(all, "sp_key").orEmpty())
    }

    /** A blocking, bounded visit: null [url] means the whole store. */
    private fun collectCookies(url: String?, includeHttpOnly: Boolean): List<CefCookie> {
        val manager = runCatching { CefCookieManager.getGlobalManager() }.getOrNull() ?: return emptyList()
        val out: MutableList<CefCookie> = Collections.synchronizedList(ArrayList())
        val done = CountDownLatch(1)
        val visitor = CefCookieVisitor { cookie, count, total, _ ->
            out.add(cookie)
            // `count` is documented as the number visited so far, but guard both
            // the 1-based and the 0-based reading so the wait never relies on it.
            if (total <= 0 || count >= total || count + 1 >= total) done.countDown()
            false // keep visiting; returning true would delete the cookie
        }
        val submitted = runCatching {
            if (url == null) manager.visitAllCookies(visitor) else manager.visitUrlCookies(url, includeHttpOnly, visitor)
        }.getOrDefault(false)
        if (!submitted) return emptyList()
        runCatching { done.await(800, TimeUnit.MILLISECONDS) }
        return out.toList()
    }

    private fun findCookie(cookies: List<CefCookie>, name: String): String? =
        cookies.firstOrNull { it.name.equals(name, ignoreCase = true) && !it.value.isNullOrBlank() }?.value

    /**
     * True for a Google sign-in URL. Google's `disallowed_useragent` answer is
     * served from a Google host (`accounts.google.com`), so seeing it is the
     * signal that this window's sign-in is over.
     */
    private fun isGoogleSignInHost(location: String?): Boolean {
        if (location.isNullOrBlank()) return false
        val host = runCatching { URI(location).host }.getOrNull()?.lowercase() ?: return false
        return host == "google.com" || host.endsWith(".google.com")
    }
}
