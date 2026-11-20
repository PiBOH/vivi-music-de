package com.music.vivi.desktop.data.youtube

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
import java.io.File
import java.net.CookieHandler
import java.net.CookieManager
import java.net.CookiePolicy
import java.net.URI
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit

/**
 * The YouTube Music sign-in window and cookie capture used by VIVI Music DE
 * 1.55.0.
 *
 * The flow is deliberately the one that worked there: open Google's own sign-in
 * page, let JavaFX keep the cookies in the shared [CookieManager], and poll the
 * jar until the session is complete. A full header needs more than SAPISID:
 * YouTube issues `LOGIN_INFO` on its own domain, so the window visits
 * www.youtube.com when that cookie is missing and only then hands the header to
 * [YouTubeAccount].
 *
 * JavaFX is started once through [ensureFxStarted]. The old window also forced
 * the software renderer because a JavaFX WebView can stay blank when Compose
 * already owns the display; that workaround is kept here.
 */
object YouTubeWebLogin {

    /** Google's sign-in page, exactly as the working 1.55.0 window opened it. */
    private const val LOGIN_URL =
        "https://accounts.google.com/ServiceLogin?continue=https%3A%2F%2Fmusic.youtube.com%2F"

    private const val WINDOW_STALE_MS = 150_000L
    private const val LOGIN_INFO_VISITS = 2
    private const val LOGIN_INFO_POLL_MS = 15_000L

    private val debugLog = File(System.getProperty("user.home"), ".vivimusic/login-debug.log")

    @Volatile
    private var windowOpen = false

    @Volatile
    private var unavailable = false

    @Volatile
    private var delivered = false

    @Volatile
    private var openedAtMs = 0L

    @Volatile
    private var window: Stage? = null

    private val waiting = java.util.Collections.synchronizedList(mutableListOf<(Result<Session>) -> Unit>())

    private val requests = java.util.concurrent.atomic.AtomicInteger(0)

    private val toolkitStarted = java.util.concurrent.atomic.AtomicBoolean(false)

    /** The session the window hands back. */
    data class Session(
        val cookie: String,
        val dataSyncId: String?,
        val visitorData: String?,
    )

    private data class SessionCapture(
        val header: String?,
        val names: List<String>,
        val missing: List<String>,
        val hasSession: Boolean,
        val hasFullSession: Boolean,
    )

    fun isWindowOpen(): Boolean = windowOpen

    /** True once a sign-in window could not be created at all on this machine. */
    val isUnavailable: Boolean get() = unavailable

    /**
     * Opens the window; [onResult] runs when the window closes or the session is
     * complete. A request made while the window is already open joins it and
     * receives the same answer instead of being dropped.
     */
    fun open(onResult: (Result<Session>) -> Unit) {
        val ageMs = if (windowOpen) System.currentTimeMillis() - openedAtMs else 0L
        val stale = windowOpen && ageMs > WINDOW_STALE_MS
        if (windowOpen && !stale) {
            logDebug("window request: joining the window opened ${ageMs / 1000}s ago")
            waiting.add(onResult)
            return
        }
        if (stale) {
            logDebug(
                "window request: the window from ${ageMs / 1000}s ago never answered, " +
                    "treating it as stuck and opening a new one"
            )
            windowOpen = false
            delivered = false
        }

        val attempt = requests.incrementAndGet()
        try {
            ensureCookieHandler()
            // The first attempt starts from an empty in-memory jar. A retry after
            // a failed or partial sign-in starts clean, the same reset the 1.55.0
            // screen applied on a second attempt.
            if (attempt > 1) clearSession()
            windowOpen = true
            openedAtMs = System.currentTimeMillis()
            delivered = false
            unavailable = false
            waiting.add(onResult)
            logDebug("window request #$attempt: opening")
            ensureFxStarted()
            FxPlatform.runLater { createWindow(closed = null, captureOnClose = true) }
        } catch (t: Throwable) {
            windowOpen = false
            unavailable = true
            logDebug("window request #$attempt failed: $t")
            deliver(null, null, null)
        }
    }

    /**
     * Opens the window without capturing anything and returns a latch that counts
     * down when it closes. Used by `--youtube-webview-check` to prove the Google
     * page loads in this build.
     */
    fun openForCheck(): CountDownLatch {
        val closed = CountDownLatch(1)
        try {
            ensureCookieHandler()
            windowOpen = true
            openedAtMs = System.currentTimeMillis()
            ensureFxStarted()
            FxPlatform.runLater { createWindow(closed = closed, captureOnClose = false) }
        } catch (t: Throwable) {
            System.err.println("[weblogin] ${t.message}")
            closed.countDown()
        }
        return closed
    }

    /**
     * Removes every YouTube/Google cookie from the shared store, so the next
     * sign-in starts from nothing instead of from a half-finished session.
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
            val uri = runCatching {
                URI("https://" + cookie.domain.removePrefix(".") + cookie.path.ifBlank { "/" })
            }.getOrNull() ?: return@forEach
            if (runCatching { store.remove(uri, cookie) }.getOrDefault(false)) removed++
        }
        logDebug("fresh sign-in: dropped $removed of ${doomed.size} YouTube/Google cookie(s)")
        return removed
    }

    private fun ensureCookieHandler() {
        if (CookieHandler.getDefault() !is CookieManager) {
            CookieHandler.setDefault(CookieManager(null, CookiePolicy.ACCEPT_ALL))
        }
    }

    /**
     * Starts the JavaFX toolkit once. The old implementation also forced the
     * software renderer: without it the WebView can load the page but paint a
     * white surface when Compose/Skia already owns the display.
     */
    private fun ensureFxStarted() {
        if (toolkitStarted.compareAndSet(false, true)) {
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
                    FxPlatform.startup {
                        FxPlatform.setImplicitExit(false)
                        ready.countDown()
                    }
                } catch (_: IllegalStateException) {
                    ready.countDown()
                } catch (t: Throwable) {
                    failure[0] = t
                    ready.countDown()
                }
            }.apply { name = "vivimusic-javafx-startup"; isDaemon = true }.start()
            if (!ready.await(15, TimeUnit.SECONDS)) {
                toolkitStarted.set(false)
                throw IllegalStateException("JavaFX toolkit startup timed out")
            }
            failure[0]?.let {
                toolkitStarted.set(false)
                throw it
            }
        }
    }

    private fun createWindow(closed: CountDownLatch?, captureOnClose: Boolean) {
        try {
            val status = Label("Waiting for the Google sign-in. This window closes by itself when the session is ready.")
            status.textFill = Color.web("#e6e1e5")
            status.isWrapText = true
            HBox.setHgrow(status, Priority.ALWAYS)

            val spinner = ProgressIndicator().apply {
                prefWidth = 18.0
                prefHeight = 18.0
            }
            val header = HBox(10.0, spinner, status).apply {
                padding = Insets(10.0, 14.0, 10.0, 14.0)
                background = Background(
                    BackgroundFill(Color.web("#1f1f2e"), CornerRadii.EMPTY, Insets.EMPTY)
                )
            }

            val steps = Label(
                "1. Sign in with your Google account.    " +
                    "2. Wait for the window to close.    " +
                    "3. VIVI Music DE keeps the session on this computer."
            ).apply {
                textFill = Color.web("#1c1b1f")
                isWrapText = true
                padding = Insets(4.0, 14.0, 4.0, 14.0)
                style = "-fx-background-color: #f3eef9; -fx-font-size: 11.5px;"
            }

            val browser = WebView().apply {
                prefWidth = 1000.0
                prefHeight = 640.0
                minWidth = 400.0
                minHeight = 300.0
                engine.userAgent = userAgent()
                engine.load(LOGIN_URL)
            }

            val root = VBox(header, steps, browser).apply {
                VBox.setVgrow(browser, Priority.ALWAYS)
            }
            val stage = Stage()
            stage.title = "VIVI Music DE - YouTube Music sign-in"
            stage.scene = Scene(root, 1000.0, 720.0)
            stage.setOnCloseRequest {
                windowOpen = false
                if (captureOnClose) {
                    val capture = capture()
                    deliver(capture.header, null, null)
                }
            }
            stage.setOnHidden {
                if (window === stage) window = null
                closed?.countDown()
            }
            stage.show()
            stage.toFront()
            window = stage

            // Kick the WebView so it paints its first frame. In a process where
            // Compose/AWT already owns the display, the WebView can stay blank
            // until it is nudged.
            browser.engine.loadWorker.stateProperty().addListener { _, _, newState ->
                val loaded = newState == Worker.State.SUCCEEDED
                val running = newState == Worker.State.RUNNING
                if (loaded || running) {
                    FxPlatform.runLater {
                        browser.resize(browser.width + 1.0, browser.height)
                        browser.resize(browser.width - 1.0, browser.height)
                        browser.requestLayout()
                        logDebug("[login-webview] state=$newState size=${browser.width}x${browser.height} title=${browser.engine.title}")
                    }
                }
            }

            if (captureOnClose) {
                Thread {
                    val deadline = System.currentTimeMillis() + 120_000
                    var loginInfoPass = false
                    while (windowOpen && !delivered) {
                        val cap = capture()
                        if (cap.hasFullSession) {
                            FxPlatform.runLater {
                                spinner.isVisible = false
                                status.text = "Saving the YouTube session..."
                            }
                            // Reload music.youtube.com WITH the session cookie so
                            // every youtube.com session cookie is set, then read
                            // the ids from the page itself.
                            FxPlatform.runLater { browser.engine.load("https://music.youtube.com/") }
                            Thread.sleep(4_500)
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
                        // Signed in, but LOGIN_INFO is missing. YouTube issues it
                        // for its own domain, so visit www.youtube.com and poll
                        // the jar instead of reading it once.
                        if (cap.hasSession && "LOGIN_INFO" in cap.missing && !loginInfoPass) {
                            loginInfoPass = true
                            logDebug("LOGIN_INFO missing - visiting www.youtube.com to complete the session")
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
                                "LOGIN_INFO still absent after $LOGIN_INFO_VISITS www.youtube.com visit(s) - " +
                                    "handing over what we have"
                            )
                        }
                        // The session exists but the critical set never
                        // completed. Hand it over anyway; the cookie header is
                        // still usable and the account card can retry.
                        if (cap.hasSession && (loginInfoPass || System.currentTimeMillis() > deadline - 60_000)) {
                            logDebug("delivering PARTIAL session, missing critical: ${cap.missing}")
                            FxPlatform.runLater {
                                spinner.isVisible = false
                                status.text = "Saving the YouTube session..."
                            }
                            val ids = extractPageIds(browser)
                            deliver(cap.header, ids.first, ids.second)
                            FxPlatform.runLater { stage.close() }
                            break
                        }
                        if (System.currentTimeMillis() > deadline) {
                            logDebug("capture timeout - no session cookies; closing the window")
                            FxPlatform.runLater { stage.close() }
                            deliver(null, null, null)
                            break
                        }
                        Thread.sleep(1_000)
                    }
                }.apply { name = "vivimusic-login-cookie-poll"; isDaemon = true }.start()
            }
        } catch (t: Throwable) {
            windowOpen = false
            unavailable = true
            logDebug("window creation failed: $t")
            closed?.countDown()
            if (captureOnClose) deliver(null, null, null)
        }
    }

    /**
     * Hands the answer to every request waiting for this window. The guard makes
     * a close request after an automatic delivery harmless.
     */
    private fun deliver(cookie: String?, dataSyncId: String?, visitorData: String?) {
        if (delivered) return
        delivered = true
        windowOpen = false
        val pending = synchronized(waiting) {
            val copy = waiting.toList()
            waiting.clear()
            copy
        }
        logDebug("delivering to ${pending.size} waiting request(s)")
        val result = if (cookie.isNullOrBlank()) {
            Result.failure(
                IllegalStateException(
                    "No YouTube session was found. Sign in on Google's page and try again."
                )
            )
        } else {
            Result.success(Session(cookie, dataSyncId, visitorData))
        }
        pending.forEach { callback -> runCatching { callback(result) } }
    }

    /**
     * Reads `DATASYNC_ID` and `VISITOR_DATA` from the loaded page's ytcfg.
     * These are the values that put the request in the account's context.
     */
    private fun extractPageIds(browser: WebView): Pair<String?, String?> {
        val result = arrayOfNulls<String>(2)
        val latch = CountDownLatch(1)
        FxPlatform.runLater {
            try {
                val getter =
                    "(function(k){ try { if (window.ytcfg && window.ytcfg.get) { return window.ytcfg.get(k) || ''; } " +
                        "if (window.ytcfg && window.ytcfg.data_) { return window.ytcfg.data_[k] || ''; } " +
                        "return ''; } catch (e) { return ''; } })"
                result[0] = sanitizeDataSyncId(
                    (browser.engine.executeScript("$getter('DATASYNC_ID')") as? String)
                )
                result[1] = (browser.engine.executeScript("$getter('VISITOR_DATA')") as? String)
                    ?.takeIf { it.isNotBlank() }
                if (result[0] == null || result[1] == null) {
                    logDebug(
                        "ytcfg ids incomplete: dataSyncId=${result[0] != null}, " +
                            "visitorData=${result[1] != null}"
                    )
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
     * Builds the Cookie header the same way the 1.55.0 window did.
     *
     * The authoritative part is what [CookieHandler] would send to
     * music.youtube.com. Every remaining cookie from the two sign-in domains is
     * appended, critical ones first, because the scoped lookup can return a
     * single cookie while the store holds the rest of the session.
     */
    private fun capture(): SessionCapture {
        val store = (CookieHandler.getDefault() as? CookieManager)?.cookieStore
        val cookies = store?.cookies.orEmpty().filter { cookie ->
            val domain = cookie.domain.removePrefix(".")
            domain.endsWith("youtube.com") || domain.endsWith("google.com")
        }
        // A name can exist on several domains. Keep the most specific one,
        // preferring the youtube.com variant on ties.
        val byName = cookies
            .groupBy { it.name }
            .mapValues { (_, list) ->
                list.maxByOrNull { (if ("youtube" in it.domain) 1 else 0) * 1000 + it.domain.length }
            }
            .mapNotNull { (_, cookie) -> cookie }
            .associateBy { it.name }

        val scopedHeader = runCatching {
            CookieHandler.getDefault()
                .get(URI("https://music.youtube.com/"), emptyMap<String, List<String>>())["Cookie"]
                ?.firstOrNull()
        }.getOrNull()?.takeIf { it.isNotBlank() }
        val scopedNames = scopedHeader.orEmpty().split(";")
            .mapNotNull { part -> part.substringBefore('=').trim().takeIf { it.isNotEmpty() } }
            .toSet()
        val names = (scopedNames + cookies.map { it.name }).distinct().sorted()
        val critical = listOf("SID", "HSID", "SSID", "APISID", "__Secure-3PSID", "LOGIN_INFO")
        val missing = critical.filter { it !in byName && it !in scopedNames }
        val authNames = listOf("SAPISID", "__Secure-1PAPISID", "__Secure-3PAPISID")
        val hasSession = authNames.any { it in scopedNames } || authNames.any { byName.containsKey(it) }
        val sessionIds = listOf("SID", "__Secure-1PSID", "__Secure-3PSID")
        val hasFullSession = hasSession &&
            (sessionIds.any { it in scopedNames } || sessionIds.any { byName.containsKey(it) }) &&
            missing.isEmpty()
        if (hasSession) {
            logDebug(
                "captured ${cookies.size} cookies: $names | missing critical: $missing | " +
                    "scoped=${scopedNames.size}"
            )
        }

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
            logDebug("session header: $sent cookie(s) sent (${scopedNames.size} via the scoped lookup)")
        }
        return SessionCapture(
            header = header,
            names = names,
            missing = missing,
            hasSession = hasSession,
            hasFullSession = hasFullSession,
        )
    }

    private fun userAgent(): String = when {
        System.getProperty("os.name").orEmpty().startsWith("Mac", ignoreCase = true) ->
            "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/537.36 Chrome/131 Safari/537.36"
        System.getProperty("os.name").orEmpty().startsWith("Linux", ignoreCase = true) ->
            "Mozilla/5.0 (X11; Linux x86_64) AppleWebKit/537.36 Chrome/131 Safari/537.36"
        else ->
            "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 Chrome/131 Safari/537.36"
    }

    private fun sanitizeDataSyncId(raw: String?): String? =
        raw?.substringBefore('|')?.trim()?.takeIf { it.isNotBlank() }

    private fun logDebug(message: String) {
        runCatching {
            debugLog.parentFile?.mkdirs()
            debugLog.appendText("[${java.time.LocalDateTime.now()}] $message\n")
        }
    }
}
