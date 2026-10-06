package com.music.vivi.desktop

import com.music.spotify.SpotifyAuth
import javafx.application.Platform as FxPlatform
import javafx.concurrent.Worker
import javafx.geometry.Insets
import javafx.geometry.Pos
import javafx.scene.Scene
import javafx.scene.control.Hyperlink
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
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
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
    internal val userAgent: String = when (Platform.os) {
        DesktopOs.WINDOWS -> "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/131.0.0.0 Safari/537.36"
        DesktopOs.MACOS -> "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/131.0.0.0 Safari/537.36"
        DesktopOs.LINUX -> "Mozilla/5.0 (X11; Linux x86_64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/131.0.0.0 Safari/537.36"
    }

    /**
     * The `navigator.userAgentData.platform` a real desktop Chrome would report.
     */
    private val platformName: String = when (Platform.os) {
        DesktopOs.WINDOWS -> "Windows"
        DesktopOs.MACOS -> "macOS"
        DesktopOs.LINUX -> "Linux"
    }

    /**
     * Best-effort JavaScript shim applied to every page the window loads, on top
     * of the real Chrome user agent above.
     *
     * Google answers any embedded browser it recognises with
     * `disallowed_useragent` ("This browser or app may not be secure"). The UA
     * is already a desktop Chrome string; this adds the JavaScript surface a
     * desktop Chrome exposes and an embedded engine does not (no
     * `navigator.webdriver`, a `window.chrome` object, `navigator.userAgentData`,
     * plugins, mime types and languages). It is a mitigation, not a guarantee:
     * Google can still identify the engine, and when it does the window keeps
     * its honest `spotify_google_blocked` message and the Retry link.
     */
    private val googleShimScript: String = """
        (function () {
          try {
            var def = function (obj, key, value) {
              try { Object.defineProperty(obj, key, { configurable: true, get: function () { return value; } }); }
              catch (e) { try { obj[key] = value; } catch (e2) {} }
            };
            var UA_BRANDS = [
              { brand: 'Chromium', version: '131' },
              { brand: 'Google Chrome', version: '131' },
              { brand: 'Not?A_Brand', version: '24' }
            ];
            def(navigator, 'webdriver', false);
            // Automation/embedded tells a real desktop Chrome does not have.
            def(navigator, 'vendor', 'Google Inc.');
            def(navigator, 'platform', '$platformName');
            def(navigator, 'hardwareConcurrency', 8);
            def(navigator, 'deviceMemory', 8);
            def(navigator, 'maxTouchPoints', 0);
            def(navigator, 'pdfViewerEnabled', true);
            def(navigator, 'doNotTrack', 'unspecified');
            var plugins = [
              { name: 'PDF Viewer', filename: 'internal-pdf-viewer', description: 'Portable Document Format' },
              { name: 'Chrome PDF Viewer', filename: 'internal-pdf-viewer', description: 'Portable Document Format' },
              { name: 'Chromium PDF Viewer', filename: 'internal-pdf-viewer', description: 'Portable Document Format' }
            ];
            def(navigator, 'plugins', plugins);
            def(navigator, 'mimeTypes', [{ type: 'application/pdf' }, { type: 'text/pdf' }]);
            def(navigator, 'languages', ['en-US', 'en']);
            def(navigator, 'userAgentData', {
              brands: UA_BRANDS,
              mobile: false,
              platform: '$platformName',
              getHighEntropyValues: function () {
                return Promise.resolve({
                  architecture: 'x86', bitness: '64', brands: UA_BRANDS, mobile: false,
                  model: '', platform: '$platformName', platformVersion: '15.0.0',
                  uaFullVersion: '131.0.0.0'
                });
              }
            });
            if (!window.chrome) { window.chrome = {}; }
            if (!window.chrome.runtime) { window.chrome.runtime = {}; }
            if (!window.chrome.app) { window.chrome.app = { isInstalled: false, InstallState: {}, RunningState: {} }; }
            def(window.chrome, 'csi', function () { return { startE: Date.now(), onloadT: Date.now(), pageT: 1, tran: 15 }; });
            def(window.chrome, 'loadTimes', function () {
              return { requestTime: Date.now() / 1000, startLoadTime: Date.now() / 1000, commitLoadTime: Date.now() / 1000, finishDocumentLoadTime: Date.now() / 1000, finishLoadTime: Date.now() / 1000, firstPaintTime: Date.now() / 1000, navigationType: 'Other' };
            });
            if (navigator.permissions && navigator.permissions.query) {
              var realQuery = navigator.permissions.query.bind(navigator.permissions);
              navigator.permissions.query = function (params) {
                if (params && params.name === 'notifications') {
                  return Promise.resolve({ state: 'default', onchange: null });
                }
                return realQuery(params);
              };
            }
          } catch (e) {}
        })();
    """.trimIndent()

    @Volatile private var windowOpen = false
    @Volatile private var unavailable = false
    @Volatile private var delivered = false

    /**
     * False once neither window can be created: the screen then offers the paste
     * fallback. JCEF is the preferred window, so this is only true when it and
     * the JavaFX fallback have both failed ([SpotifyLoginJcef]).
     */
    val isUnavailable: Boolean get() = unavailable && SpotifyLoginJcef.isUnavailable

    fun isWindowOpen(): Boolean = windowOpen || SpotifyLoginJcef.isWindowOpen()

    /**
     * Opens the sign-in window and calls [onCaptured] once — with the cookies the
     * page signed in with, or with null if the user closed the window without
     * signing in. Returns false when no window could be created at all.
     */
    fun open(language: String, onCaptured: (Capture?) -> Unit): Boolean {
        // JCEF first: a real Chromium is far likelier to pass Google's
        // embedded-browser check than the JavaFX WebEngine, which Google refuses
        // outright (issue #97). The JavaFX window below stays as the fallback
        // for a machine where CEF cannot start. See [SpotifyLoginJcef].
        if (SpotifyLoginJcef.open(language, onCaptured)) return true
        if (unavailable || windowOpen) return !unavailable
        return try {
            if (CookieHandler.getDefault() !is CookieManager) {
                CookieHandler.setDefault(CookieManager())
            }
            dropStaleSpotifyCookies()
            windowOpen = true
            delivered = false
            // This window never prepares anything (no download, no engine to
            // unpack): the bar the import screen draws under the button belongs
            // to [SpotifyLoginJcef]'s first run only.
            SpotifyLoginProgress.publish(null)
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
        // The window is done with, whatever it is done for: a capture, a close
        // without one, or a failure. Clearing `windowOpen` here (and not only in
        // the close request) is what lets the button open a second window in the
        // same session — the app closes the window itself once it has the
        // cookies, and a window closed that way used to leave the flag set for
        // the rest of the session, so only the very first attempt ever showed a
        // window.
        windowOpen = false
        runCatching { callback(capture) }
    }

    private fun createWindow(language: String, callback: (Capture?) -> Unit) {
        try {
            val stage = Stage()
            // The header is dark: the status text is light and selectable, so
            // it is readable on it and can be copied (see [selectableText]).
            val status = selectableText(
                Localization.get(language, "login_waiting"),
                Color.web("#e6e1e5"),
                background = Color.web("#1f1f2e"),
            )
            val spinner = ProgressIndicator().apply {
                prefWidth = 18.0
                prefHeight = 18.0
            }
            // Shown only once Google has refused to serve its sign-in page in
            // this window: that page cannot be used, so the way out is the
            // Spotify form the window opened on.
            val retry = Hyperlink(Localization.get(language, "retry")).apply {
                isVisible = false
                isManaged = false
                // The header is dark already; the default hyperlink blue is hard
                // to read on it.
                textFill = Color.web("#d0bcff")
            }
            // The way in for an account created with Google, which has no
            // password to type in the Spotify form: the browser the user
            // already has is a supported sign-in client, so the Google button
            // works there, and the cookie that comes out of it is the one this
            // window's screen asks to paste. Shown only once Google has refused
            // the page here (see the location listener below).
            val browserLink = Hyperlink(Localization.get(language, "spotify_open_browser")).apply {
                isVisible = false
                isManaged = false
                textFill = Color.web("#d0bcff")
            }
            val header = HBox(10.0, spinner, status, retry, browserLink).apply {
                padding = Insets(10.0, 14.0, 10.0, 14.0)
                alignment = Pos.CENTER_LEFT
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
            val savedWindow = DesktopSettings.load()
            val sceneWidth = savedWindow.spotifyLoginWindowWidth.takeIf { it >= 480 }?.toDouble() ?: 980.0
            val sceneHeight = savedWindow.spotifyLoginWindowHeight.takeIf { it >= 400 }?.toDouble() ?: 760.0
            stage.scene = Scene(root, sceneWidth, sceneHeight)
            stage.setOnCloseRequest {
                // Closed without finishing: hand back whatever is there (null
                // when nothing is), so the screen can drop its spinner.
                saveWindowSize(stage)
                windowOpen = false
                deliver(capture(), callback)
            }
            stage.show()

            // A Spotify profile created with Google is the hard case: Google
            // answers an embedded browser it recognises with
            // `disallowed_useragent` ("This browser or app may not be secure")
            // instead of its sign-in page. The window mitigates that (the real
            // Chrome UA plus the JavaScript shim applied on every load, see
            // [googleShimScript]), but it is not a guarantee: when Google still
            // refuses, the window says what happened and hands the Spotify form
            // back, which is the one way in that Google has no say in.
            browser.engine.locationProperty().addListener { _, _, location ->
                if (!isGoogleSignInHost(location)) return@addListener
                AppLog.log("spotify", "Google refused its sign-in page inside the window ($location)")
                FxPlatform.runLater {
                    spinner.isVisible = false
                    status.text = Localization.get(language, "spotify_google_blocked")
                    retry.isVisible = true
                    retry.isManaged = true
                    browserLink.isVisible = true
                    browserLink.isManaged = true
                }
            }
            browserLink.setOnAction { event ->
                event.consume()
                AppLog.click("Spotify sign-in: opened Spotify in the browser after Google blocked the window")
                openUrl(SpotifyAuth.LOGIN_URL)
                // What to do once the browser is open: the same instruction the
                // manual cookie field carries, so the window and the screen say
                // the same thing.
                status.text = Localization.get(language, "spotify_cookie_hint")
            }
            retry.setOnAction { event ->
                event.consume()
                AppLog.click("Spotify sign-in: back to Spotify's own form after Google blocked the window")
                retry.isVisible = false
                retry.isManaged = false
                spinner.isVisible = true
                status.text = Localization.get(language, "login_waiting")
                browser.engine.load(SpotifyAuth.LOGIN_URL)
            }

            // Kick the WebView so it paints its first frame. In a process where
            // Compose/AWT already owns the display, the WebView can stay blank
            // (known JavaFX painting bug) until it is nudged: force a re-layout
            // once the page starts loading, and again on load success.
            browser.engine.loadWorker.stateProperty().addListener { _, _, newState ->
                val loaded = newState == Worker.State.SUCCEEDED
                val running = newState == Worker.State.RUNNING
                val scheduled = newState == Worker.State.SCHEDULED
                if (loaded || running || scheduled) {
                    FxPlatform.runLater {
                        browser.resize(browser.width + 1.0, browser.height)
                        browser.resize(browser.width - 1.0, browser.height)
                        browser.requestLayout()
                        // Re-apply the anti-WebView shim on every state, not only
                        // once the page finished: the sign-in flow navigates
                        // between documents and Google's own detection runs from
                        // the first script on the page, so the shim has to be in
                        // place before that script, not after it.
                        runCatching { browser.engine.executeScript(googleShimScript) }
                    }
                }
            }

            // The window is its own deadline: it stays open until the user
            // finishes (or Google blocks the sign-in and the user retries in
            // this same window), and closing it ends the poll through
            // `windowOpen`.
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
                        FxPlatform.runLater {
                            saveWindowSize(stage)
                            stage.close()
                        }
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
     * Remembers the window's size, mirroring the JCEF window, so a sign-in
     * through this one restores the size the user left it at too.
     */
    private fun saveWindowSize(stage: Stage) {
        if (stage.width < 480.0 || stage.height < 400.0) return
        DesktopSettings.update {
            it.copy(
                spotifyLoginWindowWidth = stage.width.toInt(),
                spotifyLoginWindowHeight = stage.height.toInt(),
            )
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

    /**
     * True for a Google sign-in URL.
     *
     * Google's `disallowed_useragent` answer is served from a Google host
     * (`accounts.google.com`), and that page is the one the user can never get
     * past — so seeing it is the signal that this window's sign-in is over.
     */
    private fun isGoogleSignInHost(location: String?): Boolean {
        if (location.isNullOrBlank()) return false
        val host = runCatching { URI(location).host }.getOrNull()?.lowercase() ?: return false
        return host == "google.com" || host.endsWith(".google.com")
    }

    private fun HttpCookie.isSpotifyCookie(): Boolean =
        domain?.removePrefix(".")?.lowercase()?.endsWith("spotify.com") == true
}

/**
 * What the Spotify sign-in window is doing while it gets ready, published for the
 * main window to draw.
 *
 * The percentage used to live in the sign-in window's own header, where it sat
 * next to the message and pushed the two ways out around the row. The screen the
 * user is actually looking at is the one they pressed **Sign in to Spotify** on,
 * so the bar belongs under that button (`SettingsSpotifyImportScreen`), where it
 * also reads as "the button is doing something".
 *
 * - `null`: nothing is being prepared, and the bar is hidden.
 * - a negative value: work with no percentage to show (CEF's page load, which it
 *   reports no progress for).
 * - `0f..100f`: the real preparation percentage.
 */
internal object SpotifyLoginProgress {
    private val _value = MutableStateFlow<Float?>(null)
    val value: StateFlow<Float?> = _value.asStateFlow()

    fun publish(percent: Float?) {
        _value.value = percent
    }
}
