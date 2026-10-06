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
import java.awt.BasicStroke
import java.awt.BorderLayout
import java.awt.Color
import java.awt.Dimension
import java.awt.FlowLayout
import java.awt.Font
import java.awt.Graphics
import java.awt.Graphics2D
import java.awt.RenderingHints
import java.io.File
import java.net.URI
import java.util.Collections
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import javax.swing.BorderFactory
import javax.swing.JButton
import javax.swing.JComponent
import javax.swing.JFrame
import javax.swing.JLabel
import javax.swing.JPanel
import javax.swing.SwingUtilities
import javax.swing.Timer
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
 * ask. CEF delivers a visit asynchronously, so each one is awaited before the
 * captured pair is read. **The visit runs on this window's own poll thread, never
 * on the AWT event thread:** CEF dispatches the visitor callbacks on that same
 * thread, so submitting the visit from it would deadlock the visit against
 * itself (measured, 1.54.18 — see [collectCookies]).
 *
 * The window is deliberately plain: one row with a spinner and the status, then
 * the page. The preparation percentage is not here — it belongs to the screen
 * the user pressed **Sign in to Spotify** on, so it is published through
 * [SpotifyLoginProgress] and drawn under that button.
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

    /**
     * The cookie count the diagnostic line was last written for. The poll runs
     * once a second, so logging every visit would flood `spotify.log`; the count
     * changing is what says the store moved.
     */
    @Volatile private var loggedCookieCount = -1

    /** True once JCEF could not be started: the caller then uses the JavaFX window. */
    val isUnavailable: Boolean get() = unavailable

    fun isWindowOpen(): Boolean = windowOpen

    /**
     * Opens the JCEF sign-in window. Returns false, synchronously, when JCEF is
     * already known not to start, so [SpotifyLoginWebView] can fall back to its
     * JavaFX window.
     *
     * The window appears at once (with its status line) and the engine is started
     * behind it, because the first run downloads and unpacks about 100 MB. That
     * wait reports its percentage to the screen the button lives on (see
     * [SpotifyLoginProgress]).
     */
    fun open(language: String, onCaptured: (SpotifyLoginWebView.Capture?) -> Unit): Boolean {
        if (unavailable) return false
        if (windowOpen) return true
        windowOpen = true
        delivered = false
        loggedCookieCount = -1
        SpotifyLoginProgress.publish(0f)
        SwingUtilities.invokeLater { showWindow(language, onCaptured) }
        return true
    }

    private fun showWindow(language: String, callback: (SpotifyLoginWebView.Capture?) -> Unit) {
        try {
            val saved = DesktopSettings.load()
            val width = saved.spotifyLoginWindowWidth.takeIf { it >= 480 } ?: DEFAULT_WIDTH
            val height = saved.spotifyLoginWindowHeight.takeIf { it >= 400 } ?: DEFAULT_HEIGHT

            // One row: a spinner, the status, and — only once Google has refused
            // its sign-in page here — the two ways out. The long blocked sentence
            // is clipped to the row and carried in full in the tooltip; the row
            // itself never grows, so neither button can be pushed off it.
            val spinner = Spinner()
            val status = JLabel(Localization.get(language, "login_waiting")).apply {
                foreground = Color(0xE6, 0xE1, 0xE5)
                font = font.deriveFont(Font.PLAIN, 13f)
            }
            fun setStatus(text: String) {
                status.text = text
                status.toolTipText = text
            }
            // Flat and borderless, in the accent the JavaFX window's hyperlinks
            // use: the YouTube sign-in window is the look this one is asked to
            // match, and a stock Swing button next to it reads as a different
            // program. Both stay hidden until Google refuses its page.
            fun headerLink(text: String) = JButton(text).apply {
                isVisible = false
                isFocusable = false
                isBorderPainted = false
                isContentAreaFilled = false
                isOpaque = false
                foreground = Color(0xD0, 0xBC, 0xFF)
                cursor = java.awt.Cursor.getPredefinedCursor(java.awt.Cursor.HAND_CURSOR)
                font = font.deriveFont(Font.PLAIN, 13f)
                border = BorderFactory.createEmptyBorder(2, 6, 2, 6)
            }
            val retry = headerLink(Localization.get(language, "retry"))
            val browserLink = headerLink(Localization.get(language, "spotify_open_browser"))
            val header = JPanel(FlowLayout(FlowLayout.LEFT, 10, 8)).apply {
                background = Color(0x1F, 0x1F, 0x2E)
                border = BorderFactory.createEmptyBorder(2, 8, 2, 8)
                add(spinner)
                add(status)
                add(retry)
                add(browserLink)
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
                    spinner.stop()
                    rememberBounds(frame)
                    windowOpen = false
                    SpotifyLoginProgress.publish(null)
                    deliver(capture(), callback)
                    frame.isVisible = false
                    frame.dispose()
                }
            })
            frame.isVisible = true

            // Start CEF behind the window, reporting the real download/extract
            // percentage jcefmaven gives us to the screen behind it.
            Thread({
                try {
                    val app = cefApp ?: buildApp { _, value ->
                        // A negative value is jcefmaven's "no estimation": keep the
                        // bar working and show no number, rather than a stuck 0 %.
                        SpotifyLoginProgress.publish(if (value < 0f) -1f else value)
                    }.also { cefApp = it }
                    SwingUtilities.invokeLater {
                        attachBrowser(app, frame, center, spinner, status, retry, browserLink, language, callback)
                    }
                } catch (t: Throwable) {
                    windowOpen = false
                    unavailable = true
                    SpotifyLoginProgress.publish(null)
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
            SpotifyLoginProgress.publish(null)
            AppLog.log("spotify", "the JCEF sign-in window could not be created: $t")
            SpotifyLoginWebView.open(language, callback)
        }
    }

    private fun attachBrowser(
        app: CefApp,
        frame: JFrame,
        center: JPanel,
        spinner: Spinner,
        status: JLabel,
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

            client.addLoadHandler(object : CefLoadHandlerAdapter() {
                override fun onLoadingStateChange(b: CefBrowser?, isLoading: Boolean, canGoBack: Boolean, canGoForward: Boolean) {
                    if (!isLoading) {
                        // The page is up: the preparation is over, whether it came
                        // from the download or from the navigation itself.
                        spinner.stop()
                        SpotifyLoginProgress.publish(null)
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
                    // Only the main frame. Spotify's sign-in page embeds Google's
                    // own script, which loads an `accounts.google.com` iframe, and
                    // without this guard every one of those iframes said the
                    // sign-in had been blocked while the page was the Spotify form
                    // the user was happily typing into.
                    if (f?.isMain != true) return
                    if (!isGoogleSignInHost(url)) return
                    AppLog.log("spotify", "Google refused its sign-in page inside the JCEF window ($url)")
                    SwingUtilities.invokeLater {
                        spinner.stop()
                        status.text = Localization.get(language, "spotify_google_blocked")
                        status.toolTipText = status.text
                        retry.isVisible = true
                        browserLink.isVisible = true
                        frame.revalidate()
                    }
                }
            })

            browserLink.addActionListener {
                AppLog.click("Spotify sign-in (JCEF): opened Spotify in the browser after Google blocked the window")
                openUrl(SpotifyAuth.LOGIN_URL)
                status.text = Localization.get(language, "spotify_cookie_hint")
                status.toolTipText = status.text
            }
            retry.addActionListener {
                AppLog.click("Spotify sign-in (JCEF): back to Spotify's own form after Google blocked the window")
                retry.isVisible = false
                browserLink.isVisible = false
                spinner.start()
                status.text = Localization.get(language, "login_waiting")
                status.toolTipText = status.text
                frame.revalidate()
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
                            spinner.stop()
                            SpotifyLoginProgress.publish(null)
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
            SpotifyLoginProgress.publish(null)
            AppLog.log("spotify", "the JCEF sign-in window could not be created: $t")
            // Take the empty frame down before the JavaFX fallback opens, so a
            // failure here does not leave two sign-in windows on screen.
            SwingUtilities.invokeLater {
                spinner.stop()
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
        SpotifyLoginProgress.publish(null)
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
        if (scoped.size != loggedCookieCount) {
            loggedCookieCount = scoped.size
            AppLog.log(
                "spotify",
                "JCEF cookie visit returned ${scoped.size} cookie(s): " +
                    scoped.take(40).joinToString(", ") { it.name ?: "?" },
            )
        }
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

    /**
     * A blocking, bounded visit: null [url] means the whole store.
     *
     * **The visitor must return `true`.** In JCEF the return value is *keep
     * visiting*: returning `false` stops the walk after the first cookie, which is
     * what made this window capture nothing at all — the store's first cookie is
     * almost never `sp_dc`, so the visit came back with a single unrelated cookie
     * and the capture answered null forever, while the same account's cookies
     * pasted by hand worked. Measured on JCEF 152.0.6: with `false` the walk
     * delivers 1 cookie of 5, with `true` it delivers all 5, and it deletes
     * nothing.
     *
     * It is called from a background thread on purpose. CEF dispatches the visitor
     * callbacks on the AWT event thread, so a visit submitted from that thread
     * waits for callbacks that can only run once it returns: measured, zero
     * callbacks in three seconds. From any other thread the same visit delivers
     * everything in milliseconds.
     */
    private fun collectCookies(url: String?, includeHttpOnly: Boolean): List<CefCookie> {
        val manager = runCatching { CefCookieManager.getGlobalManager() }.getOrNull() ?: return emptyList()
        val out: MutableList<CefCookie> = Collections.synchronizedList(ArrayList())
        val done = CountDownLatch(1)
        val visitor = CefCookieVisitor { cookie, count, total, _ ->
            out.add(cookie)
            // `count` is documented as the number visited so far, but guard both
            // the 1-based and the 0-based reading so the wait never relies on it.
            if (total <= 0 || count >= total || count + 1 >= total) done.countDown()
            true // keep visiting; `false` would end the walk after this cookie
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

    /**
     * The window's "something is happening" mark: a small arc that turns while
     * the status line says what is being waited for. Swing has no spinner, and an
     * indeterminate `JProgressBar` is a bar, not a mark; this is a dozen lines and
     * it stops with the window.
     */
    private class Spinner : JComponent() {
        private var angle = 0
        private val timer = Timer(80) {
            angle = (angle + 30) % 360
            repaint()
        }

        init {
            preferredSize = Dimension(16, 16)
            isOpaque = false
            timer.start()
        }

        fun stop() = timer.stop()

        fun start() {
            if (!timer.isRunning) timer.start()
            isVisible = true
        }

        override fun paintComponent(g: Graphics) {
            val g2 = g.create() as Graphics2D
            try {
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON)
                g2.color = Color(0xD0, 0xBC, 0xFF)
                g2.stroke = BasicStroke(2f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND)
                g2.drawArc(2, 2, width - 4, height - 4, angle, 270)
            } finally {
                g2.dispose()
            }
        }
    }
}
