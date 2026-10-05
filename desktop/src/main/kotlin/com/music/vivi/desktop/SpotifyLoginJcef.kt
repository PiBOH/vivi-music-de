package com.music.vivi.desktop

import com.music.spotify.SpotifyAuth
import me.friwi.jcefmaven.CefAppBuilder
import me.friwi.jcefmaven.MavenCefAppHandlerAdapter
import org.cef.CefApp
import org.cef.CefApp.CefAppState
import org.cef.CefClient
import org.cef.browser.CefBrowser
import org.cef.browser.CefFrame
import org.cef.callback.CefCookieVisitor
import org.cef.handler.CefDisplayHandlerAdapter
import org.cef.network.CefCookieManager
import java.awt.BorderLayout
import java.awt.Color
import java.awt.Dimension
import java.awt.FlowLayout
import java.awt.Font
import java.io.File
import java.net.URI
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicReference
import javax.swing.BorderFactory
import javax.swing.JButton
import javax.swing.JFrame
import javax.swing.JLabel
import javax.swing.JPanel
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
 * pretending to be one. **Constraint, unchanged and unverified here:** Google
 * can still decide the engine is embedded and refuse, and no test has proven
 * otherwise in this environment; if it does, the window keeps the honest
 * `spotify_google_blocked` message and the **Open it in your browser** link the
 * JavaFX window already offers, and the screen behind it still carries the
 * `sp_dc` / `sp_key` paste. The JavaFX window stays as the fallback for a machine
 * where JCEF cannot start (no natives, no network to fetch them).
 *
 * The cookies are read from CEF's own store ([CefCookieManager]) — a different
 * store from the JavaFX window's `java.net.CookieManager` — scoped to
 * `https://open.spotify.com`, the same question the phone and the JavaFX window
 * ask.
 */
internal object SpotifyLoginJcef {

    /** The origin the capture is scoped to, exactly as the other window scopes it. */
    private const val SPOTIFY_ORIGIN = "https://open.spotify.com"

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
     * JavaFX window; returns true once the (background) start has been kicked off.
     */
    fun open(language: String, onCaptured: (SpotifyLoginWebView.Capture?) -> Unit): Boolean {
        if (unavailable) return false
        if (windowOpen) return true
        windowOpen = true
        delivered = false
        AppLog.log("spotify", "opening the JCEF sign-in window (CEF natives under $installDir)")
        Thread({
            try {
                val app = cefApp ?: buildApp().also { cefApp = it }
                SwingUtilities.invokeLater { createWindow(app, language, onCaptured) }
            } catch (t: Throwable) {
                windowOpen = false
                unavailable = true
                AppLog.log("spotify", "JCEF could not start, falling back to the JavaFX window: $t")
                deliver(null, onCaptured)
            }
        }, "vivimusic-spotify-jcef-init").apply { isDaemon = true }.start()
        return true
    }

    /**
     * Builds (once) the global [CefApp]. The first call downloads and unpacks the
     * native CEF bundle under [installDir], so it is deliberately off the UI
     * thread. Windowed rendering (not off-screen) is what a Swing-embedded
     * browser needs.
     */
    private fun buildApp(): CefApp {
        val builder = CefAppBuilder()
        builder.setInstallDir(installDir)
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

    private fun createWindow(app: CefApp, language: String, callback: (SpotifyLoginWebView.Capture?) -> Unit) {
        try {
            val client: CefClient = app.createClient()

            val status = JLabel(Localization.get(language, "login_waiting")).apply {
                foreground = Color(0xE6, 0xE1, 0xE5)
                font = font.deriveFont(Font.PLAIN, 13f)
            }
            val browserLink = JButton(Localization.get(language, "spotify_open_browser")).apply {
                isVisible = false
                isFocusable = false
            }
            val retry = JButton(Localization.get(language, "retry")).apply {
                isVisible = false
                isFocusable = false
            }
            val header = JPanel(FlowLayout(FlowLayout.LEFT, 10, 8)).apply {
                background = Color(0x1F, 0x1F, 0x2E)
                border = BorderFactory.createEmptyBorder(4, 6, 4, 6)
                add(status)
                add(retry)
                add(browserLink)
            }

            val browser: CefBrowser = client.createBrowser(SpotifyAuth.LOGIN_URL, false, false)
            val browserUi = browser.uiComponent.apply {
                minimumSize = Dimension(420, 320)
                preferredSize = Dimension(980, 700)
            }

            val frame = JFrame("VIVI Music DE — ${Localization.get(language, "spotify_open_login")}").apply {
                defaultCloseOperation = WindowConstants.DO_NOTHING_ON_CLOSE
                layout = BorderLayout()
                contentPane.add(header, BorderLayout.NORTH)
                contentPane.add(browserUi, BorderLayout.CENTER)
                setSize(1000, 800)
                setLocationRelativeTo(null)
            }

            // A Spotify profile created with Google is the hard case: Google
            // answers an embedded browser it recognises with
            // `disallowed_useragent`. CEF is the Chromium engine, so this is far
            // less likely than it was in the JavaFX window, but it is not
            // guaranteed: when it still happens, the window says what happened
            // and offers the browser link, which is the one way in Google has no
            // say in.
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

            frame.addWindowListener(object : java.awt.event.WindowAdapter() {
                override fun windowClosing(e: java.awt.event.WindowEvent) {
                    // Closed without finishing: hand back whatever is there (null
                    // when nothing is), so the screen can drop its spinner.
                    windowOpen = false
                    deliver(capture(), callback)
                    frame.isVisible = false
                    frame.dispose()
                }
            })

            frame.isVisible = true
            browser.setFocus(true)

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
            deliver(null, callback)
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
     * `visitUrlCookies` delivers the store asynchronously on the CEF UI thread
     * and returns before the visitor runs, so reading the captured values right
     * after the call would almost always see nothing. The visitor counts the
     * visit down instead and this waits for it (bounded, so a store with no
     * cookies cannot hang the poll).
     */
    private fun capture(): SpotifyLoginWebView.Capture? {
        val manager = runCatching { CefCookieManager.getGlobalManager() }.getOrNull() ?: return null
        val spDc = AtomicReference<String?>()
        val spKey = AtomicReference("")
        val done = CountDownLatch(1)
        val visited = runCatching {
            manager.visitUrlCookies(
                "$SPOTIFY_ORIGIN/",
                true,
                CefCookieVisitor { cookie, count, total, _ ->
                    val domain = cookie.domain?.removePrefix(".")?.lowercase()
                    if (domain == "spotify.com" || domain?.endsWith(".spotify.com") == true) {
                        when (cookie.name?.lowercase()) {
                            "sp_dc" -> if (!cookie.value.isNullOrBlank()) spDc.set(cookie.value)
                            "sp_key" -> spKey.set(cookie.value.orEmpty())
                        }
                    }
                    if (total <= 0 || count >= total) done.countDown()
                    false // keep visiting; returning true would delete the cookie
                },
            )
        }.getOrDefault(false)
        if (!visited) return null
        runCatching { done.await(500, TimeUnit.MILLISECONDS) }
        val dc = spDc.get()?.takeIf { it.isNotBlank() } ?: return null
        return SpotifyLoginWebView.Capture(dc, spKey.get())
    }

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
