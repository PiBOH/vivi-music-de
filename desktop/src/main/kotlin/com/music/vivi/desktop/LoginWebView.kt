package com.music.vivi.desktop

import javafx.application.Platform as FxPlatform
import javafx.scene.Scene
import javafx.scene.control.ProgressIndicator
import javafx.scene.layout.Background
import javafx.scene.layout.BackgroundFill
import javafx.scene.layout.CornerRadii
import javafx.scene.layout.HBox
import javafx.scene.layout.Priority
import javafx.scene.layout.VBox
import javafx.scene.web.WebView
import javafx.geometry.Insets
import javafx.scene.paint.Color
import javafx.stage.Stage

import java.awt.Desktop
import java.io.File
import java.net.CookieHandler
import java.net.CookieManager
import java.net.URI
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit

/**
 * Embedded YouTube sign-in window using JavaFX directly.
 *
 * JavaFX is initialized with `Platform.startup` exactly once. This is important
 * in a Compose Desktop process: `Application.launch` is single-use and can race
 * with the already-running AWT/Compose event loop, causing the WebView startup
 * failure to be reported repeatedly. No JFXPanel/Swing interop is used.
 */
object LoginWebView {
    @Volatile private var windowOpen = false
    @Volatile private var unavailable = false
    @Volatile private var delivered = false

    /**
     * Every request waiting for this window's answer.
     *
     * The window used to hold ONE callback — the one of the request that opened
     * it — and a second request while it was open was answered with `return true`
     * and no callback at all. The screen sets "waiting for the window" before it
     * calls in and only clears it from the callback, so that press left the
     * button disabled for the rest of the session: exactly the report "I can only
     * open the Google sign-in window once". A request that arrives while a window
     * is already up now JOINS it and gets the same answer.
     */
    private val waiting = java.util.Collections.synchronizedList(mutableListOf<(Capture?) -> Unit>())

    /** Requests made this session, so the log says which attempt did what. */
    private val requests = java.util.concurrent.atomic.AtomicInteger(0)

    /** When the window on screen was created, for the staleness test below. */
    @Volatile private var openedAtMs = 0L

    /**
     * How long a window may stay open without answering before the next request
     * ignores it and opens a fresh one. The capture itself gives up at 120 s and
     * hands over what it has, so a window still silent after this much time is
     * stuck (a refused Stage, a toolkit that dropped it) and must not be able to
     * block the rest of the session.
     */
    private const val WINDOW_STALE_MS = 150_000L

    private val debugLog = File(System.getProperty("user.home"), ".vivimusic/login-debug.log")

    /**
     * How the missing `LOGIN_INFO` is chased: the www.youtube.com visit is made
     * up to [LOGIN_INFO_VISITS] times, and after each one the cookie store is
     * polled for [LOGIN_INFO_POLL_MS] instead of being read once. Both numbers
     * are bounded on purpose: the whole capture runs under a 120 s deadline, and
     * a sign-in the user is watching cannot hang on a cookie that may never
     * come.
     */
    private const val LOGIN_INFO_VISITS = 2
    private const val LOGIN_INFO_POLL_MS = 15_000L

    private fun logDebug(msg: String) {
        runCatching {
            debugLog.parentFile?.mkdirs()
            debugLog.appendText("[${java.time.LocalDateTime.now()}] $msg\n")
        }
    }

    private data class SessionCapture(
        val header: String?,
        val names: List<String>,
        val missing: List<String>,
        val hasSession: Boolean,
        val hasFullSession: Boolean,
    )

    /** What the window hands back after a sign-in attempt. */
    data class Capture(
        val cookie: String?,
        val dataSyncId: String?,
        val visitorData: String?,
    )

    fun isWindowOpen(): Boolean = windowOpen

    /** True once a sign-in window could not be created at all on this machine. */
    val isUnavailable: Boolean get() = unavailable

    /**
     * Starts JavaFX once, then creates the embedded login Stage.
     *
     * [resetFirst] drops every YouTube/Google cookie the previous attempt left in
     * the shared cookie store **before** the window loads (see [clearSession]).
     * The login screen sets it on an attempt that follows a failure: a stale
     * half-session in that store is what makes the capture come back without
     * `LOGIN_INFO` (E1033 in ERRORS.md), and signing in from a clean store is
     * exactly the "clear the cache and it works" the reports describe.
     */
    fun openEmbedded(
        language: String,
        resetFirst: Boolean = false,
        onCaptured: (Capture?) -> Unit,
    ): Boolean {
        // `windowOpen` alone blocks a second window: a sign-in that is already
        // on screen must not be opened twice. `unavailable` deliberately does
        // NOT, it is retried: it is set by a failure of THIS attempt (JavaFX
        // missing, the toolkit refusing a second stage), and a machine where one
        // attempt failed can still succeed on the next — treating the first
        // failure as permanent is what left "the sign-in window only opens
        // once" (the button then fell through to the manual paste forever).
        val attempt = requests.incrementAndGet()
        val ageMs = if (windowOpen) System.currentTimeMillis() - openedAtMs else 0L
        val stale = windowOpen && ageMs > WINDOW_STALE_MS
        if (windowOpen && !stale) {
            // Already on screen: this request rides along and is answered with the
            // same capture, instead of being dropped (see [waiting]).
            logDebug("window request #$attempt: joining the window opened ${ageMs / 1000}s ago")
            waiting.add(onCaptured)
            return true
        }
        if (stale) {
            logDebug(
                "window request #$attempt: the window from ${ageMs / 1000}s ago never answered, " +
                    "treating it as stuck and opening a new one"
            )
            windowOpen = false
            delivered = false
        }
        return try {
            if (CookieHandler.getDefault() !is CookieManager) {
                CookieHandler.setDefault(CookieManager())
            }
            if (resetFirst) clearSession()
            windowOpen = true
            openedAtMs = System.currentTimeMillis()
            delivered = false
            unavailable = false
            waiting.add(onCaptured)
            logDebug("window request #$attempt: opening")
            ensureFxStarted()
            FxPlatform.runLater { createWindow(language) }
            true
        } catch (t: Throwable) {
            windowOpen = false
            unavailable = true
            logDebug("window request #$attempt failed: $t")
            deliver(null, null, null)
            false
        }
    }

    /** Opens the direct Google sign-in page in the system browser as fallback. */
    fun openBrowser(): Boolean = runCatching {
        if (!Desktop.isDesktopSupported()) return@runCatching false
        Desktop.getDesktop().browse(URI("https://accounts.google.com/ServiceLogin?continue=https%3A%2F%2Fmusic.youtube.com%2F"))
        true
    }.getOrDefault(false)

    /**
     * The toolkit is shared with the Spotify sign-in window: see [JavaFxToolkit]
     * for why one process has exactly one place that starts it.
     */
    private fun ensureFxStarted() = JavaFxToolkit.ensureStarted()

    /**
     * Removes every YouTube/Google cookie from the shared store, so the next
     * sign-in starts from nothing instead of from a half-finished session.
     *
     * This is the programmatic form of the workaround every E1033 report ends
     * with ("I cleared the cache and it works"): the store is a JVM-wide
     * [CookieManager], so a session that was captured *before* a real sign-in
     * (or one whose `LOGIN_INFO` never arrived) sits there and is re-read by the
     * next attempt. Only the two sign-in domains are touched, so nothing else in
     * the process (the Spotify window keeps its own store) is affected.
     *
     * @return how many cookies were dropped.
     */
    fun clearSession(): Int {
        val manager = CookieHandler.getDefault() as? CookieManager ?: return 0
        val store = manager.cookieStore
        val doomed = runCatching {
            store.cookies.filter { cookie ->
                val domain = cookie.domain.removePrefix(".")
                domain.endsWith("youtube.com") || domain.endsWith("google.com")
            }
        }.getOrDefault(emptyList())
        var removed = 0
        doomed.forEach { cookie ->
            // The store removes by the cookie itself (the URI only has to be a
            // valid one for the two sign-in hosts); the scheme is safe to assume
            // because every session cookie here is secure.
            val uri = runCatching {
                URI("https://" + cookie.domain.removePrefix(".") + cookie.path.ifBlank { "/" })
            }.getOrNull() ?: return@forEach
            if (runCatching { store.remove(uri, cookie) }.getOrDefault(false)) removed++
        }
        logDebug("fresh sign-in: dropped $removed of ${doomed.size} YouTube/Google cookie(s)")
        return removed
    }

    /**
     * Hands the answer to EVERY request waiting for this window and closes the
     * window's life: `windowOpen` goes back to false so the next press opens a
     * new one, and the waiting list is emptied so no caller stays stuck (which is
     * what made the button work only once). The app closes this window itself
     * once it has the session, and a window closed that way never reaches its own
     * close request, so this is the only place that can do it. See the same shape
     * in [SpotifyLoginWebView].
     */
    private fun deliver(cookie: String?, dataSyncId: String?, visitorData: String?) {
        if (delivered) return
        delivered = true
        windowOpen = false
        val capture = Capture(cookie, dataSyncId, visitorData)
        val pending = synchronized(waiting) {
            val copy = waiting.toList()
            waiting.clear()
            copy
        }
        logDebug("delivering to ${pending.size} waiting request(s)")
        pending.forEach { callback -> runCatching { callback(capture) } }
    }

    private fun createWindow(language: String) {
        try {
            val stage = Stage()
            // The header is dark: its text is set light, and a plain label could
            // not be selected (see [selectableText]).
            val status = selectableText(
                Localization.get(language, "login_waiting"),
                Color.web("#e6e1e5"),
                background = Color.web("#1f1f2e"),
            )
            val spinner = ProgressIndicator().apply {
                prefWidth = 18.0
                prefHeight = 18.0
            }
            val header = HBox(10.0, spinner, status).apply {
                padding = Insets(10.0, 14.0, 10.0, 14.0)
                background = Background(BackgroundFill(Color.web("#1f1f2e"), CornerRadii.EMPTY, Insets.EMPTY))
            }
            // The numbered steps sit on a light bar: dark text on light, and
            // selectable so they can be copied. They were three full-width rows
            // at 13px, which pushed the page itself down by a fifth of the
            // window; they are the same three sentences in ONE compact line now,
            // at a size that reads as a hint rather than as a heading (the user
            // reads them once and then wants the page).
            val steps = selectableText(
                listOf("login_step1", "login_step2", "login_step3")
                    .mapIndexed { index, key -> "${index + 1}. ${Localization.get(language, key)}" }
                    .joinToString("   ·   "),
                Color.web("#1c1b1f"),
                11.5,
                Color.web("#f3eef9"),
            ).apply {
                padding = Insets(4.0, 14.0, 4.0, 14.0)
            }
            val browser = WebView().apply {
                prefWidth = 1000.0
                prefHeight = 640.0
                minWidth = 400.0
                minHeight = 300.0
                engine.userAgent = when (Platform.os) {
                    DesktopOs.WINDOWS -> "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 Chrome/131 Safari/537.36"
                    DesktopOs.MACOS -> "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/537.36 Chrome/131 Safari/537.36"
                    DesktopOs.LINUX -> "Mozilla/5.0 (X11; Linux x86_64) AppleWebKit/537.36 Chrome/131 Safari/537.36"
                }
                engine.load("https://accounts.google.com/ServiceLogin?continue=https%3A%2F%2Fmusic.youtube.com%2F")
            }
            val root = VBox(header, steps, browser).apply {
                VBox.setVgrow(browser, Priority.ALWAYS)
            }
            stage.title = "VIVI Music DE — ${Localization.get(language, "login")}"
            stage.scene = Scene(root, 1000.0, 720.0)
            stage.setOnCloseRequest {
                windowOpen = false
                deliver(capture().header, null, null)
            }
            stage.show()

            // Kick the WebView so it paints its first frame. In a process where
            // Compose/AWT already owns the display, the WebView can stay blank
            // (known JavaFX painting bug) until it is nudged: force a re-layout
            // once the page starts loading, and again on load success.
            browser.engine.loadWorker.stateProperty().addListener { _, _, newState ->
                val loaded = newState == javafx.concurrent.Worker.State.SUCCEEDED
                val running = newState == javafx.concurrent.Worker.State.RUNNING
                if (loaded || running) {
                    FxPlatform.runLater {
                        browser.resize(browser.width + 1.0, browser.height)
                        browser.resize(browser.width - 1.0, browser.height)
                        browser.requestLayout()
                        println("[login-webview] state=$newState size=${browser.width}x${browser.height} title=${browser.engine.title}")
                    }
                }
            }

            Thread {
                val deadline = System.currentTimeMillis() + 120_000
                // The www.youtube.com pass runs once: it is what issues
                // LOGIN_INFO, and without it the hand-over cannot complete.
                var loginInfoPass = false
                while (windowOpen && !delivered) {
                    val cap = capture()
                    if (cap.hasFullSession) {
                        FxPlatform.runLater {
                            spinner.isVisible = false
                            status.text = Localization.get(language, "login_saving")
                        }
                        // Reload music.youtube.com WITH the session cookie so
                        // every youtube.com session cookie is set, then settle
                        // and re-capture the full header before closing. The ids
                        // come from the page itself (ytcfg), right there.
                        FxPlatform.runLater { browser.engine.load("https://music.youtube.com/") }
                        Thread.sleep(4500)
                        val ids = extractPageIds(browser)
                        val finalCap = capture()
                        logDebug(
                            "delivering full session: ${finalCap.names.size} cookies, missing=" +
                                "${finalCap.missing}, dataSyncId=${if (ids.first != null) "ok" else "MISSING"}, " +
                                "visitorData=${if (ids.second != null) "ok" else "MISSING"}"
                        )
                        deliver(finalCap.header ?: cap.header, ids.first, ids.second)
                        FxPlatform.runLater { stage.close() }
                        break
                    }
                    // Signed in, but the critical set is incomplete — which, in
                    // every report so far, means exactly one cookie: LOGIN_INFO.
                    // YouTube issues it for its OWN domain, and the window was
                    // opened on accounts.google.com with music.youtube.com as
                    // `continue`, so the store can end up with the whole Google
                    // session and no LOGIN_INFO. Validating with such a header
                    // answers 401 (E1003/E1031) and looks like a dead session,
                    // while the very same cookies pasted by hand include
                    // LOGIN_INFO and work. Visit www.youtube.com once, let the
                    // cookie store catch up, then go back to the music property
                    // for the ids.
                    if (cap.hasSession && "LOGIN_INFO" in cap.missing && !loginInfoPass) {
                        loginInfoPass = true
                        logDebug("LOGIN_INFO missing — visiting www.youtube.com to complete the session")
                        // Poll for the cookie instead of reading the store once
                        // after a fixed wait. Whether the visit lands on the
                        // signed-in youtube.com property is not a fixed amount of
                        // time, and the user's own log holds both sides of that
                        // coin, seven seconds after the visit either way:
                        // 2026-10-08T16:52 caught LOGIN_INFO on the first visit
                        // and delivered a completed session, while
                        // 2026-10-08T16:45 came back with 35 cookies and no
                        // LOGIN_INFO, handed the partial session over and answered
                        // 401 (`E1033`). Polling costs nothing when the cookie
                        // appears at once and waits for the rest when it does not,
                        // and the second visit covers the case where the first one
                        // simply did not land.
                        var after: SessionCapture? = null
                        var completed = false
                        for (pass in 1..LOGIN_INFO_VISITS) {
                            FxPlatform.runLater {
                                if (pass == 1) browser.engine.load("https://www.youtube.com/")
                                else browser.engine.reload()
                            }
                            val pollUntil = System.currentTimeMillis() + LOGIN_INFO_POLL_MS
                            while (System.currentTimeMillis() < pollUntil && windowOpen && !delivered) {
                                Thread.sleep(1_000)
                                val now = capture()
                                after = now
                                if ("LOGIN_INFO" !in now.missing) {
                                    completed = true
                                    break
                                }
                            }
                            val seen = after
                            logDebug(
                                "after www.youtube.com (visit $pass/$LOGIN_INFO_VISITS): " +
                                    "${seen?.names?.size ?: 0} cookies, missing=${seen?.missing}"
                            )
                            if (completed) break
                        }
                        val seen = after
                        if (completed && seen != null) {
                            FxPlatform.runLater { browser.engine.load("https://music.youtube.com/") }
                            Thread.sleep(4_000)
                            val ids = extractPageIds(browser)
                            val finalCap = capture()
                            logDebug(
                                "delivering completed session: ${finalCap.names.size} cookies, " +
                                    "dataSyncId=${if (ids.first != null) "ok" else "MISSING"}, " +
                                    "visitorData=${if (ids.second != null) "ok" else "MISSING"}"
                            )
                            deliver(finalCap.header ?: seen.header, ids.first, ids.second)
                            FxPlatform.runLater { stage.close() }
                            break
                        }
                        logDebug(
                            "LOGIN_INFO still absent after $LOGIN_INFO_VISITS www.youtube.com visit(s) — " +
                                "handing over what we have"
                        )
                    }
                    // Fallback: a session existed but the critical set never
                    // completed (either the pass above ran, or the 60 s mark was
                    // reached). Hand over what we have — the header stays in the
                    // manual field for a one-click retry — and name the missing
                    // cookies in the log.
                    if (cap.hasSession && (loginInfoPass || System.currentTimeMillis() > deadline - 60_000)) {
                        logDebug("delivering PARTIAL session, missing critical: ${cap.missing}")
                        FxPlatform.runLater {
                            spinner.isVisible = false
                            status.text = Localization.get(language, "login_saving")
                        }
                        val ids = extractPageIds(browser)
                        deliver(cap.header, ids.first, ids.second)
                        FxPlatform.runLater { stage.close() }
                        break
                    }
                    if (System.currentTimeMillis() > deadline) {
                        // The window is taken down and the app is told, exactly
                        // like a user closing it: a window that stays open while
                        // the capture thread has given up cannot sign anyone in, 
                        // and leaving `windowOpen` set would make the button
                        // inert ("it only opens once").
                        logDebug("capture timeout — no session cookies; closing the window")
                        FxPlatform.runLater { stage.close() }
                        deliver(null, null, null)
                        break
                    }
                    Thread.sleep(1000)
                }
            }.apply { name = "vivimusic-login-cookie-poll"; isDaemon = true }.start()
        } catch (t: Throwable) {
            windowOpen = false
            unavailable = true
            deliver(null, null, null)
        }
    }

    /**
     * Reads `DATASYNC_ID` and `VISITOR_DATA` straight from the loaded page's
     * ytcfg while the WebView is sitting on music.youtube.com with the session
     * — the same values that appear in the page source. These two are
     * mandatory (without them the account validation answers as guest), so
     * this is the authoritative source; the shell-fetch fallback in
     * [LoginManager] only runs when this capture comes back empty.
     */
    private fun extractPageIds(browser: WebView): Pair<String?, String?> {
        val result = arrayOfNulls<String>(2)
        val latch = CountDownLatch(1)
        FxPlatform.runLater {
            try {
                val getter = "(function(k){ try { if (window.ytcfg && window.ytcfg.get) { return window.ytcfg.get(k) || ''; } if (window.ytcfg && window.ytcfg.data_) { return window.ytcfg.data_[k] || ''; } return ''; } catch (e) { return ''; } })"
                result[0] = sanitizeDataSyncId(
                    (browser.engine.executeScript("$getter('DATASYNC_ID')") as? String)
                )
                result[1] = (browser.engine.executeScript("$getter('VISITOR_DATA')") as? String)
                    ?.takeIf { it.isNotBlank() }
                if (result[0] == null || result[1] == null) {
                    logDebug("ytcfg ids incomplete: dataSyncId=${result[0] != null}, visitorData=${result[1] != null}")
                }
            } catch (t: Throwable) {
                logDebug("ytcfg extraction failed: $t")
            }
            latch.countDown()
        }
        runCatching { latch.await(5, TimeUnit.SECONDS) }
        return result[0] to result[1]
    }

    /**
     * Reads the cookies for youtube/google domains. The authoritative header
     * is the one the cookie handler itself would send to music.youtube.com —
     * the same domain/path/secure matching a browser applies when it builds
     * the Cookie header the manual method pastes (which is why manual paste
     * kept working while the WebView capture failed). SAPISID alone
     * authenticates nothing: the innertube account_menu validation answers
     * as guest (NPE) when the critical HttpOnly session cookies are missing.
     */
    private fun capture(): SessionCapture {
        val store = (CookieHandler.getDefault() as? CookieManager)?.cookieStore
        val cookies = store?.cookies.orEmpty().filter { c ->
            val domain = c.domain.removePrefix(".")
            domain.endsWith("youtube.com") || domain.endsWith("google.com")
        }
        // A cookie name can exist on several domains (e.g. SAPISID on .google.com
        // and .youtube.com). Keep the most specific domain per name, preferring
        // the youtube.com variant on ties: the domains have equal length, and
        // only the .youtube.com session authenticates the music.youtube.com API.
        val byName = cookies
            .groupBy { it.name }
            .mapValues { (_, list) ->
                list.maxByOrNull { (if ("youtube" in it.domain) 1 else 0) * 1000 + it.domain.length }
            }
            .mapNotNull { (_, c) -> c }
            .associateBy { it.name }
        // Primary header: ask the cookie handler which cookies it would send
        // to the API host. This applies the same domain/path/secure rules a
        // browser does, so the result matches the manually pasted header
        // exactly; a plain store dump mixes in cookies scoped to other
        // Google properties.
        val scopedHeader = runCatching {
            CookieHandler.getDefault()
                .get(URI("https://music.youtube.com/"), emptyMap<String, List<String>>())["Cookie"]
                ?.firstOrNull()
        }.getOrNull()?.takeIf { it.isNotBlank() }
        val scopedNames = scopedHeader.orEmpty().split(";")
            .mapNotNull { part -> part.substringBefore('=').trim().takeIf { n -> n.isNotEmpty() } }
            .toSet()
        val names = (scopedNames + cookies.map { it.name }).distinct().sorted()
        val critical = listOf("SID", "HSID", "SSID", "APISID", "__Secure-3PSID", "LOGIN_INFO")
        val missing = critical.filter { it !in byName && it !in scopedNames }
        val authNames = listOf("SAPISID", "__Secure-1PAPISID", "__Secure-3PAPISID")
        val hasSession = authNames.any { it in scopedNames } || authNames.any { byName.containsKey(it) }
        // A complete session has the SAPISID pair AND a session id cookie:
        // the legacy SID or either Secure PSID variant. Modern Google logins
        // often never issue the legacy SID — requiring it blocked the
        // full-session hand-over and made the WebView fail while the manual
        // paste of the very same session succeeded.
        // A complete session is a session cookie, an id cookie AND the whole
        // critical set: LOGIN_INFO belongs to it (YouTube issues it for its own
        // domain) and a header without it is answered with 401 — delivering such
        // a session is what made the embedded sign-in look broken while the
        // manual paste of the same cookies worked.
        val sessionIds = listOf("SID", "__Secure-1PSID", "__Secure-3PSID")
        val hasFullSession = hasSession &&
            (sessionIds.any { it in scopedNames } || sessionIds.any { byName.containsKey(it) }) &&
            missing.isEmpty()
        if (hasSession) {
            logDebug("captured ${cookies.size} cookies: $names | missing critical: $missing | scoped=${scopedNames.size}")
        }
        // Backfill: EVERY cookie of the store the scoped lookup did not return,
        // the auth-critical ones first, without duplicating a name.
        //
        // This used to be a ten-name whitelist, and the user's own
        // `~/.vivimusic/login-debug.log` is the measurement that says why that is
        // not enough: the scoped lookup answered `scoped=1` (one single cookie)
        // while the store held 28 cookies for the two sign-in domains. The header
        // that reached the API was therefore that one cookie plus whichever of
        // the ten the whitelist happened to name, and every other session cookie
        // was dropped in silence - including `LOGIN_INFO`, whose absence is
        // exactly what answers 401 ("the captured session has no LOGIN_INFO
        // cookie", E1033) while the same cookies pasted by hand work. The whole
        // set is therefore sent, and the couple of cookies scoped to a sibling
        // Google host travel with it on purpose: an unexpected cookie is ignored
        // by the API, whereas a dropped session cookie costs the login.
        val criticalFirst = listOf(
            "SAPISID", "__Secure-1PAPISID", "__Secure-3PAPISID", "APISID",
            "SID", "__Secure-1PSID", "__Secure-3PSID", "HSID", "SSID", "LOGIN_INFO",
        )
        val backfill = byName.values
            .filter { it.name !in scopedNames }
            .sortedWith(
                compareBy(
                    { cookie ->
                        val rank = criticalFirst.indexOf(cookie.name)
                        if (rank == -1) criticalFirst.size else rank
                    },
                    { it.name },
                ),
            )
            .joinToString("; ") { "${it.name}=${it.value}" }
        val header = when {
            !hasSession -> null
            scopedHeader == null -> backfill
            backfill.isEmpty() -> scopedHeader
            else -> "$scopedHeader; $backfill"
        }
        if (header != null) {
            val sent = header.split(";").count { it.isNotBlank() }
            logDebug("session header: $sent cookie(s) sent ($scopedNames.size via the scoped lookup)")
        }
        return SessionCapture(
            header = header,
            names = names,
            missing = missing,
            hasSession = hasSession,
            hasFullSession = hasFullSession,
        )
    }
}
