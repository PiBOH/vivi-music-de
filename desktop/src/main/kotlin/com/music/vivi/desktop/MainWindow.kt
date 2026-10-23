package com.music.vivi.desktop

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.tween
import androidx.compose.ui.draw.alpha
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.CropSquare
import androidx.compose.material.icons.filled.FilterNone
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.key.key
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.WindowPlacement
import androidx.compose.ui.window.rememberWindowState
import androidx.compose.ui.window.application
import com.music.innertube.YouTube
import com.music.innertube.YouTubeExtractor
import com.music.vivi.desktop.player.AudioOutput
import java.io.File
import java.util.Locale
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch

/**
 * Ensures Skiko can render on this machine. On Linux the default render API is
 * OpenGL; when the system has no working libGL (common with the AppImage on
 * Arch / minimal desktops), the first frame dies with
 * `UnsatisfiedLinkError: OpenGLApi.glFlush()`. Detecting that here and falling
 * back to `SKIKO_RENDER_API=SOFTWARE` keeps the app usable everywhere. The
 * check is cheap (a library load), so systems with a real GPU are unaffected.
 *
 * The libGL probe can't catch every broken GL stack (a loadable libGL is not
 * a working GLX/EGL context), so the error dialog also watches for the actual
 * Skiko crash: it writes `~/.vivimusic/.gl-software` and relaunches once with
 * `SKIKO_RENDER_API=SOFTWARE` (see ErrorDialog.kt). The marker below makes the
 * next launch start straight in software rendering instead of crashing again.
 */
private fun configureRenderApi() {
    // Explicit JVM property wins over everything.
    if (System.getProperty("skiko.renderApi") != null) return
    // The env var form is honored on every OS (also used by the crash fallback).
    val envApi = System.getenv("SKIKO_RENDER_API")
    if (!envApi.isNullOrBlank()) {
        System.setProperty("skiko.renderApi", envApi)
        return
    }
    val os = System.getProperty("os.name", "").lowercase(Locale.ROOT)
    if (!os.contains("linux")) return // Windows/macOS defaults are fine
    // A previous run crashed in Skiko's OpenGL renderer → stay on SOFTWARE.
    if (glSoftwareMarkerFile().exists()) {
        System.setProperty("skiko.renderApi", "SOFTWARE")
        return
    }
    if (canLoadOpenGL()) return
    System.setProperty("skiko.renderApi", "SOFTWARE")
}

/** Marker written by the crash fallback (ErrorDialog.kt) after a Skiko GL crash. */
private fun glSoftwareMarkerFile(): java.io.File =
    java.io.File(System.getProperty("user.home"), ".vivimusic/.gl-software")

private fun canLoadOpenGL(): Boolean = runCatching {
    // libGL on common Linux layouts (glvnd, distro paths); any one suffices.
    val candidates = listOf(
        "libGL.so.1",
        "libGL.so",
        "GL",
        "/usr/lib/x86_64-linux-gnu/libGL.so.1",
        "/usr/lib/libGL.so.1",
        "/usr/lib64/libGL.so.1",
    )
    candidates.forEach { lib ->
        runCatching {
            if (lib.startsWith("/")) System.load(lib) else System.loadLibrary(lib)
        }.onSuccess { return true }
    }
    false
}.getOrDefault(false)

fun main(args: Array<String>) {
    // Skiko render-API fallback: on Linux, if the system OpenGL library cannot
    // be loaded (e.g. the AppImage running on Arch without a working libGL,
    // which crashed with UnsatisfiedLinkError in OpenGLApi.glFlush on the first
    // frame), fall back to the software renderer before the first Skia layer is
    // created. Must run before application { }.
    configureRenderApi()

    // Apply a heap size chosen in Developer options before anything else grabs
    // the single-instance lock: when it differs from the current one this starts
    // a fresh process and exits, so the lock is never held by the dying one.
    runCatching { JvmMemory.relaunchIfNeeded() }

    // A toast / command-line launch can request a section (e.g. --open=updates).
    val openSection = AppCommand.parse(args)
    // Single-instance guard: if another instance is already running (or already
    // starting), forward the request to it and exit immediately.
    if (!SingleInstance.acquire()) {
        if (openSection != null) AppCommand.write(openSection)
        return
    }

    // Replace the default AWT "Error" dialog (OK only) with one that also
    // offers "Copy error", so crash details are easy to report.
    installGlobalErrorDialog()

    // Playback scheduling protection (issue #3): on Windows, ask the OS once
    // (1 ms timer, above-normal process, power throttling off) so the audio
    // threads are not the first thing the scheduler drops while the window is
    // in the background; [AudioPlayer] then registers each audio thread with
    // MMCSS through the same helper.
    runCatching { AudioThreadBoost.start() }
    // GC pauses are the other mechanism a stall can come from: record them
    // (`gc.log`) so an exported session can tell "the JVM was stopped" apart
    // from "the process was not scheduled".
    runCatching { GcMonitor.start() }
    // Native notifications, warmed in the background: the AUMID registration
    // compiles C# inline and the toast session needs a PowerShell process, so
    // both are paid for at startup instead of on the first notification (which
    // is what made native toasts show up late, see [WindowsToast]).
    runCatching { WindowsToast.prewarm() }

    // Startup timing. The launch path is long — JVM, Skiko, the string tables,
    // the settings/mixer/file loads — so "it takes 20 seconds" cannot be acted
    // on without knowing WHICH stage costs what. One line per stage in `app.log`
    // makes the next launch answer the question instead of guessing at it.
    val startupAt = System.currentTimeMillis()
    fun startupStage(name: String) {
        // Gated on DetailedStartupLogs (Developer options, off by default): with
        // it off these four lines are not written at all, so a normal launch has
        // a clean log.
        if (!DesktopSettings.load().detailedStartupLogs) return
        AppLog.log("app", "startup: $name at +${System.currentTimeMillis() - startupAt}ms")
    }

    application {
    startupStage("compose started")
    // The user-editable settings file (~/.vivimusic/settings.json): adopt what it
    // holds (a value edited while the app was closed must win over the last one
    // stored by the app), create it when it is missing, and watch it for edits
    // made while the app runs. It has to run before the first load of the
    // session, which is why it is the very first thing here.
    SettingsFile.start()

    val initialSettings = DesktopSettings.load()
    startupStage("settings loaded")

    // Bumped whenever settings.json is edited from outside the app. Every option
    // read from the store uses it as a remember key (`settingsFileRevision()`),
    // so a hand-edited file takes effect immediately instead of at the next
    // restart.
    val settingsRevision = settingsFileRevision()

    // Configure the shared YouTube client exactly like the Android App.onCreate()
    // does, honouring the saved content language/region (or the OS default).
    val startupLocale = resolveYouTubeLocale(
        initialSettings.contentLanguage,
        initialSettings.contentCountry,
        initialSettings.language,
    )
    // The Innertube client IS the whole HTTP stack (Ktor, OkHttp, the TLS and
    // keystore providers) and building it loads hundreds of classes: measured
    // at 0.9-1.2 s right on the AWT thread, on the way to the first frame,
    // because touching `YouTube` initialises `InnerTube` and its client.
    // Nothing before the first frame needs a request, so the client is built on
    // its own thread; a request that arrives first simply waits on the class
    // initialisation, which the JVM serialises for us.
    Thread({ runCatching { YouTube.locale = startupLocale } }, "vivi-innertube-warmup").apply {
        isDaemon = true
        start()
    }

    // The shared Compose strings (the text-selection context menu above all)
    // localise from the platform locale, not from the app's own table: on an
    // Italian machine an English build showed "Copia". Follow the app language.
    Languages.applyJvmLocale(initialSettings.language)

    // The options that are cached outside Compose follow an edit of the file too.
    LaunchedEffect(settingsRevision) {
        if (settingsRevision <= 0L) return@LaunchedEffect
        runCatching { AudioOutput.load() }
        runCatching { Animations.load() }
        runCatching { ContentFilters.load() }
        runCatching { DeveloperOptions.load() }
        val current = DesktopSettings.load()
        YouTube.locale = resolveYouTubeLocale(current.contentLanguage, current.contentCountry, current.language)
        // A language change made in the app (or in the file) has to reach the
        // platform locale too, so the right-click menu follows it.
        Languages.applyJvmLocale(current.language)
    }
    YouTubeExtractor.cacheDir = File(System.getProperty("user.home"), ".vivimusic/cache").apply { mkdirs() }
    LoginManager.restore()
    DesktopSettings.ensureFirstLaunchDate()
    // Restore the preferred audio output device (Java Sound mixer). NOT here:
    // enumerating the Java Sound mixers is a device round-trip that nothing
    // before the first play needs, and it used to sit on the way to the first
    // frame. The effect below still runs it before any audio line is opened.
    LaunchedEffect(Unit) { runCatching { AudioOutput.load() } }
    // Restore the global UI animation speed.
    Animations.load()
    // Restore the content filters (explicit/video/Shorts).
    ContentFilters.load()
    // Dev tools are non-critical: never let their initialization crash the app
    // at startup (which the jpackage launcher reports as "Failed to launch JVM").
    runCatching { DeveloperOptions.load() }
    startupStage("cached options loaded")

    // Bootup speed: the settings file is parsed exactly ONCE (initialSettings)
    // instead of one disk read + JSON parse per setting (~12 of them here).
    var language by remember(settingsFileRevision()) { mutableStateOf(initialSettings.language) }
    var themeMode by remember(settingsFileRevision()) { mutableStateOf(ThemeMode.from(initialSettings.darkMode)) }
    var accent by remember(settingsFileRevision()) { mutableStateOf(argbIntToColor(initialSettings.accentColor)) }
    var accentIntensity by remember(settingsFileRevision()) { mutableStateOf(initialSettings.accentIntensity) }
    var customAccents by remember(settingsFileRevision()) { mutableStateOf(initialSettings.customAccents) }
    var pureBlack by remember(settingsFileRevision()) { mutableStateOf(initialSettings.pureBlack) }
    var selectedFont by remember(settingsFileRevision()) { mutableStateOf(AppFont.fromValue(initialSettings.selectedFont)) }
    var customFontPath by remember(settingsFileRevision()) { mutableStateOf(initialSettings.customFontPath) }
    // Make the runtime-imported font resolvable before the first theme pass.
    if (customFontPath.isNotBlank()) AppFonts.customFontPath = customFontPath
    // Spotify-style layout (3 panels) + flat theme, applied together. Default
    // on; when false the app falls back to the Material 3 tonal look.
    var spotifyLayout by remember(settingsFileRevision()) { mutableStateOf(initialSettings.spotifyLayout) }

    fun saveTheme() {
        DesktopSettings.update {
            it.copy(
                darkMode = themeMode.key,
                accentColor = colorToArgbInt(accent),
                accentIntensity = accentIntensity,
                pureBlack = pureBlack,
            )
        }
    }

    // Import a user font (.ttf/.otf): pick the file, copy it into the app data
    // dir so the setting survives moving/deleting the original, and apply it.
    //
    // The picker is the Swing `JFileChooser`, NOT the native AWT `FileDialog`:
    // on Windows the native dialog refuses to open `C:\Windows\Fonts` for a
    // non-elevated process and answers "access is denied", which forced users to
    // copy the font elsewhere and only then select it. The Swing chooser lists
    // directories through the plain file APIs, so the system font folder can be
    // browsed and its files only READ (which the OS allows). A failure to copy
    // is reported (log + notification) instead of being swallowed.
    val importFont: () -> Unit = {
        val chooser = javax.swing.JFileChooser().apply {
            dialogTitle = Localization.get(language, "import_font")
            isMultiSelectionEnabled = false
            // System font files carry the hidden attribute; without this the
            // font folder would look empty in the chooser.
            isFileHidingEnabled = false
            fileSelectionMode = javax.swing.JFileChooser.FILES_ONLY
            fileFilter = javax.swing.filechooser.FileNameExtensionFilter("Fonts (*.ttf, *.otf)", "ttf", "otf")
        }
        if (chooser.showOpenDialog(null) == javax.swing.JFileChooser.APPROVE_OPTION) {
            val src = chooser.selectedFile
            if (src == null) {
                AppLog.click("import font: nothing selected")
            } else {
                AppLog.click("import font '${src.absolutePath}'")
                val fontsDir = java.io.File(System.getProperty("user.home"), ".vivimusic/fonts").apply { mkdirs() }
                val ext = src.extension.ifBlank { "ttf" }
                val dest = java.io.File(fontsDir, "custom_font.$ext")
                val failure = runCatching {
                    java.nio.file.Files.copy(
                        src.toPath(),
                        dest.toPath(),
                        java.nio.file.StandardCopyOption.REPLACE_EXISTING,
                    )
                }.exceptionOrNull()
                if (failure != null || !dest.exists()) {
                    val why = failure?.let { "${it.javaClass.simpleName}: ${it.message}" }
                        ?: "the copy did not produce a file"
                    AppLog.log("app", "font import failed for '${src.absolutePath}': $why")
                    DesktopNotifier.notify(
                        Localization.get(language, "import_font"),
                        Localization.get(language, "custom_font") + ": " + why,
                        "appearance",
                    )
                } else {
                    AppFonts.customFontPath = dest.absolutePath
                    customFontPath = dest.absolutePath
                    selectedFont = AppFont.CUSTOM
                    DesktopSettings.update {
                        it.copy(customFontPath = dest.absolutePath, selectedFont = AppFont.CUSTOM.value)
                    }
                    AppLog.log("app", "font imported: '${src.name}' → '${dest.absolutePath}'")
                }
            }
        }
    }

    // Live window title: shows CPU/RAM when dev options are on and either the
    // "show in title bar" toggle is set or the display mode is "title bar only".
    val devTitleEnabled by DeveloperOptions.enabled.collectAsState()
    val devTitleVisible by DeveloperOptions.showInTitleBar.collectAsState()
    val devMode by DeveloperOptions.mode.collectAsState()
    val titleStats by SystemMonitor.stats.collectAsState()
    val windowTitle = buildString {
        append("VIVI Music — desktop")
        if (devTitleEnabled && (devTitleVisible || devMode == DevToolsMode.TITLE_BAR)) {
            append(titleStats.titleBarText())
        }
    }

    var isFullscreen by remember(settingsFileRevision()) { mutableStateOf(DesktopSettings.load().isFullscreen) }
    // Native OS title bar vs VIVI's custom one (window chrome is fixed at
    // creation, so this applies on the next launch).
    // LIVE value used by the toggle UI (saved on change). The actual window
    // chrome is decided at creation (see `nativeTitleBarAtStartup` below) and
    // cannot change on a displayed frame — Compose's `SwingWindow` calls
    // `setUndecorated()` on the live frame when the parameter changes, which
    // throws `IllegalComponentStateException: The frame is displayable`.
    var nativeTitleBar by remember(settingsFileRevision()) { mutableStateOf(initialSettings.nativeTitleBar) }
    // FROZEN at first composition: the window chrome fixed at creation. It is
    // deliberately never updated after startup, so flipping the toggle can
    // never trigger a runtime `setUndecorated` on the shown frame; the new
    // value is picked up by the restart the toggle asks for.
    val nativeTitleBarAtStartup = remember { initialSettings.nativeTitleBar }
    // Tracks the OS-maximized state (updated by the AWT listener below) so the
    // custom title-bar buttons reflect the real window placement.
    var windowMaximized by remember(settingsFileRevision()) { mutableStateOf(initialSettings.windowMaximized) }
    // Placement before entering fullscreen, so leaving it restores exactly
    // where the window was (floating bounds or maximized).
    var preFullscreenMaximized by remember { mutableStateOf(false) }
    var preFullscreenBounds by remember { mutableStateOf<java.awt.Rectangle?>(null) }
    // Restore bounds captured right before an OS maximize: the persisted
    // geometry must be the floating bounds, so the window never reopens
    // stretched over the taskbar after a maximized session.
    var preMaximizeBounds by remember { mutableStateOf<java.awt.Rectangle?>(null) }
    // Start floating: the saved placement (maximized / bounds) is restored with
    // the OS APIs inside the Window block. Compose's WindowPlacement.Maximized
    // on an undecorated window can produce a window LARGER than the screen when
    // the Windows display scale is not 100%, leaving the title bar off-screen
    // (users then have to kill the app from Task Manager).
    val windowState = rememberWindowState(placement = WindowPlacement.Floating)
    // Captured from the Window content so onCloseRequest can persist geometry.
    val awtWindowRef = arrayOfNulls<java.awt.Frame>(1)

    startupStage("pre-window work done")
    Window(
        onCloseRequest = {
            runCatching { HistoryStore.flush() }
            awtWindowRef[0]?.let { persistWindowGeometry(it, preMaximizeBounds) }
            exitApplication()
        },
        title = windowTitle,
        state = windowState,
        // Undecorated = VIVI's custom title bar; decorated = the native OS bar.
        // Uses the startup-frozen value: never changes while the window is
        // displayed (setting it at runtime throws on a displayable frame).
        undecorated = !nativeTitleBarAtStartup,
    ) {
        val frameWindow = window
        awtWindowRef[0] = frameWindow

        // The window wears the app's own mark. The packaged Windows and macOS
        // bundles get it from the launcher, but a Linux launcher has nothing to
        // take it from: jpackage's app image carries no window icon, so the
        // AppImage showed the JVM's default icon wherever the window appeared
        // (taskbar, Alt+Tab, window list) even though the AppDir's `.desktop`
        // names the right one. AWT has no default to fall back on either way, so
        // it is set here, from the same brand mark the tray already uses.
        LaunchedEffect(frameWindow) {
            runCatching {
                BrandLogo.awtImage(256)?.let { mark -> frameWindow.iconImage = mark }
            }.onFailure { error ->
                AppLog.log("window", "could not set the window icon: $error")
            }
        }

        // Screen-recording tools (OBS and friends) list a window only when it
        // is VISIBLE and NOT minimized — they enumerate with
        // `EXCLUDE_MINIMIZED`, so a minimized VIVI (or one whose window has not
        // been shown yet) is simply absent from the list. Recording the state
        // turns "my recorder does not see VIVI" into something diagnosable
        // from an exported log instead of guesswork. VIVI's window is a plain
        // top-level frame (class `SunAwtFrame`) in BOTH chrome modes, without
        // WS_EX_TOOLWINDOW, so it passes that filter once it is on screen.
        DisposableEffect(frameWindow) {
            fun recordWindowState(reason: String) {
                runCatching {
                    val iconified = (frameWindow.extendedState and java.awt.Frame.ICONIFIED) != 0
                    AppLog.log(
                        "window",
                        "state=$reason decorated=${!frameWindow.isUndecorated} " +
                            "visible=${frameWindow.isVisible} iconified=$iconified " +
                            "bounds=${frameWindow.width}x${frameWindow.height}@${frameWindow.x},${frameWindow.y}",
                    )
                }
            }
            val listener = object : java.awt.event.WindowAdapter() {
                override fun windowOpened(e: java.awt.event.WindowEvent) {
                    recordWindowState("opened")
                    // Compose applies its own default placement when the window
                    // is shown, which can land after the restore effect; the
                    // saved placement is re-asserted once the window is up.
                    runCatching { applySavedWindowGeometry(frameWindow) }
                    recordWindowState("restored")
                }

                override fun windowStateChanged(e: java.awt.event.WindowEvent) = recordWindowState("state-changed")
            }
            frameWindow.addWindowListener(listener)
            AppLog.log(
                "window",
                "render: skiko.renderApi=${System.getProperty("skiko.renderApi") ?: "default"}, " +
                    "os=${System.getProperty("os.name")} ${System.getProperty("os.arch")} — " +
                    "screen recorders find VIVI as VIVIMusic.exe; if the capture is black, " +
                    "select the WGC method (Windows 10 19041+) instead of Automatic",
            )
            recordWindowState("created")
            onDispose { runCatching { frameWindow.removeWindowListener(listener) } }
        }

        // Restore the last placement with the OS APIs: OS maximize respects the
        // taskbar and the Windows DPI scaling, unlike Compose's placement which
        // can oversize an undecorated window. See [applySavedWindowGeometry].
        LaunchedEffect(Unit) {
            // A saved fullscreen state is applied by LaunchedEffect(isFullscreen)
            // below (it also fires on first composition), via the OS API so the
            // auto-hide taskbar stays reachable on hover instead of being
            // covered by an oversized Compose placement.
            runCatching { applySavedWindowGeometry(frameWindow) }
        }

        DisposableEffect(frameWindow) {
            val listener = java.awt.event.WindowStateListener { e ->
                val isIconified = (e.newState and java.awt.Frame.ICONIFIED) != 0
                if (!isIconified && windowState.isMinimized) {
                    windowState.isMinimized = false
                }
                // Track the OS maximize state so the title-bar buttons and the
                // persisted placement follow snapping / Win+Up as well.
                windowMaximized = (e.newState and java.awt.Frame.MAXIMIZED_BOTH) != 0
            }
            frameWindow.addWindowStateListener(listener)
            onDispose {
                frameWindow.removeWindowStateListener(listener)
            }
        }

        // Follow the fullscreen toggle with the OS API. Compose's
        // WindowPlacement.Fullscreen can oversize an undecorated window on
        // scaled displays (the same bug class as its Maximized placement),
        // pushing the window past the screen edge and trapping the auto-hide
        // taskbar behind it. OS-maximize never oversizes: on an auto-hide
        // taskbar it fills the whole screen and the taskbar still reveals on
        // hover at the bottom edge; with a visible taskbar it respects the
        // work area.
        LaunchedEffect(isFullscreen) {
            windowState.placement = WindowPlacement.Floating
            runCatching {
                if (isFullscreen) {
                    // Save where we were before the maximize kicks in (on a
                    // fresh start this is the initial floating placement).
                    preFullscreenMaximized =
                        (frameWindow.extendedState and java.awt.Frame.MAXIMIZED_BOTH) != 0
                    preFullscreenBounds = frameWindow.bounds
                    frameWindow.extendedState = java.awt.Frame.MAXIMIZED_BOTH
                } else {
                    if (preFullscreenMaximized) {
                        frameWindow.extendedState = java.awt.Frame.MAXIMIZED_BOTH
                    } else {
                        frameWindow.extendedState = java.awt.Frame.NORMAL
                        preFullscreenBounds?.let { frameWindow.setBounds(it) }
                    }
                }
            }
        }

        AppTheme(
            mode = themeMode,
            accent = accent,
            pureBlack = pureBlack,
            font = selectedFont,
            spotify = spotifyLayout,
            accentIntensity = accentIntensity,
            customFontPath = customFontPath,
            language = language,
        ) {
            // NOTE: do NOT wrap this in a global SelectionContainer. Popup-based
            // components (DropdownMenu, AlertDialog) inherit the selection
            // registrar and crash with "layouts are not part of the same
            // hierarchy" on pointer events (see Compose CMP-2326). Use targeted
            // SelectionContainer wrappers on individual text instead.
            // Deliberately NOT keyed on the settings revision: the splash is a
            // one-shot, and re-reading its option would replay it over the UI
            // every time settings.json is edited.
            var showIntro by remember { mutableStateOf(DesktopSettings.load().showIntroSplash) }
            // The first-run flow is shown while no language has been chosen yet,
            // i.e. on a fresh install, and it stays up until the user finishes it
            // even though its first step already saves the language: without this
            // flag the app would appear in the middle of its own welcome. A
            // restart after quitting halfway skips the flow, which is right, the
            // install is no longer new.
            var firstRun by remember { mutableStateOf(language.isBlank()) }
            // What the crossfade is between. It used to be just the splash flag,
            // which cannot express three stages.
            val stage = when {
                showIntro -> "intro"
                firstRun -> "first_run"
                else -> "app"
            }
            Crossfade(
                targetState = stage,
                animationSpec = if (DesktopSettings.load().animationsEnabled) tween(400) else tween(0),
                label = "intro",
            ) { current ->
                when (current) {
                    "intro" -> IntroSplash(
                        language = language,
                        style = DesktopSettings.load().introStyle,
                        background = DesktopSettings.load().introBackground,
                        onFinished = { showIntro = false },
                    )
                    "first_run" -> FirstRunFlow(
                        // English until a language is picked, which is also what
                        // the flow's own first step offers.
                        language = language.ifBlank { "en" },
                        onLanguageSelected = { selected ->
                            language = selected
                            DesktopSettings.update {
                                it.copy(language = selected, languageSeq = it.languageSeq + 1)
                            }
                            // The account's own names (playlists, auto playlists)
                            // follow the app language when the content language is
                            // "system": a language change has to reach the client
                            // at once.
                            val s = DesktopSettings.load()
                            YouTube.locale = resolveYouTubeLocale(s.contentLanguage, s.contentCountry, selected)
                        },
                        onFinish = { firstRun = false },
                    )
                    else -> App(
                        language = language,
                        onLanguageChange = { selected ->
                            language = selected
                            DesktopSettings.update {
                                it.copy(language = selected, languageSeq = it.languageSeq + 1)
                            }
                            // See the LanguageSelectionScreen branch above: the
                            // content language follows the app language when it is
                            // left on "system".
                            val s = DesktopSettings.load()
                            YouTube.locale = resolveYouTubeLocale(s.contentLanguage, s.contentCountry, selected)
                        },
                        font = selectedFont,
                        onFontChange = { f ->
                            selectedFont = f
                            DesktopSettings.update { it.copy(selectedFont = f.value) }
                        },
                        themeMode = themeMode,
                        accent = accent,
                        onThemeModeChange = {
                            themeMode = it
                            saveTheme()
                        },
                        onAccentChange = {
                            accent = it
                            saveTheme()
                        },
                        accentIntensity = accentIntensity,
                        // Live state during the drag; the file is written once the
                        // pointer is released (issue #61).
                        onAccentIntensityChange = { accentIntensity = it },
                        onAccentIntensityChangeFinished = { saveTheme() },
                        customAccents = customAccents,
                        onAddCustomAccent = { argb ->
                            customAccents = (customAccents + argb).distinct()
                            DesktopSettings.update { it.copy(customAccents = customAccents) }
                        },
                        onRemoveCustomAccent = { argb ->
                            customAccents = customAccents - argb
                            DesktopSettings.update { it.copy(customAccents = customAccents) }
                        },
                        customFontPath = customFontPath,
                        onImportFont = importFont,
                        pureBlack = pureBlack,
                        onPureBlackChange = {
                            pureBlack = it
                            saveTheme()
                        },
                        spotifyLayout = spotifyLayout,
                        initialSection = openSection,
                        isFullscreen = isFullscreen,
                        isMaximized = (windowMaximized || isFullscreen),
                        nativeTitleBar = nativeTitleBar,
                        onNativeTitleBarChange = { v ->
                            nativeTitleBar = v
                            DesktopSettings.update { it.copy(nativeTitleBar = v) }
                        },
                        onRestart = ::restartApplication,
                        onToggleFullscreen = {
                            isFullscreen = !isFullscreen
                            DesktopSettings.update { it.copy(isFullscreen = isFullscreen) }
                        },
                        bringToFront = {
                            runCatching {
                                frameWindow.state = java.awt.Frame.NORMAL
                                frameWindow.toFront()
                                frameWindow.requestFocus()
                            }
                        },
                        onMinimize = {
                            windowState.isMinimized = true
                            runCatching { frameWindow.extendedState = frameWindow.extendedState or java.awt.Frame.ICONIFIED }
                        },
                        onMaximize = {
                            if (windowState.isMinimized) {
                                windowState.isMinimized = false
                            }
                            if (isFullscreen) {
                                // The maximize button while fullscreen leaves
                                // fullscreen; LaunchedEffect(isFullscreen)
                                // restores the pre-fullscreen placement.
                                isFullscreen = false
                                DesktopSettings.update { it.copy(isFullscreen = false) }
                            } else if ((frameWindow.extendedState and java.awt.Frame.MAXIMIZED_BOTH) != 0) {
                                // OS restore: respects the work area (taskbar)
                                // and the Windows DPI scaling.
                                runCatching { frameWindow.extendedState = java.awt.Frame.NORMAL }
                            } else {
                                // Capture the floating bounds BEFORE maximizing so
                                // they can be restored (and persisted) later.
                                if (preMaximizeBounds == null) {
                                    preMaximizeBounds = frameWindow.bounds
                                }
                                runCatching { frameWindow.extendedState = java.awt.Frame.MAXIMIZED_BOTH }
                            }
                            runCatching {
                                val maximized = (frameWindow.extendedState and java.awt.Frame.MAXIMIZED_BOTH) != 0
                                // Persist the restore bounds (the floating ones
                                // captured before the maximize), never the
                                // maximized full-screen bounds that include the
                                // taskbar area.
                                val b = if (maximized) preMaximizeBounds ?: frameWindow.bounds else frameWindow.bounds
                                DesktopSettings.update {
                                    it.copy(
                                        windowMaximized = maximized,
                                        windowX = b.x,
                                        windowY = b.y,
                                        windowWidth = b.width,
                                        windowHeight = b.height,
                                    )
                                }
                            }
                        },
                        // The tray's Quit (and every other in-app exit) leaves
                        // through here, not through the close request, so the
                        // placement is persisted first: quitting from the tray
                        // used to lose a window's size and position.
                        onClose = {
                            awtWindowRef[0]?.let { persistWindowGeometry(it, preMaximizeBounds) }
                            exitApplication()
                        },
                    )
                }
            }
        }
    }
    }
}

/**
 * Persists the placement the window is leaving at.
 *
 * The RESTORE bounds are saved, not the maximized ones: while maximized,
 * `frame.bounds` spans the whole screen including the taskbar area, and
 * re-applying that as a normal placement on the next start makes the window open
 * over/under the taskbar. When maximized, the bounds kept by [applySavedWindowGeometry]'s
 * caller are used. An iconified frame is skipped entirely: a minimized window
 * reports a parked rectangle (`-32000,-32000 160x28`), which would be restored
 * as the placement.
 */
private fun persistWindowGeometry(frame: java.awt.Frame, preMaximizeBounds: java.awt.Rectangle?) {
    runCatching {
        val maximized = (frame.extendedState and java.awt.Frame.MAXIMIZED_BOTH) != 0
        val iconified = (frame.extendedState and java.awt.Frame.ICONIFIED) != 0
        val b = (if (maximized) preMaximizeBounds else null) ?: frame.bounds
        if (iconified || (!maximized && (b.width < 200 || b.height < 200))) return
        DesktopSettings.update {
            it.copy(
                windowMaximized = maximized,
                windowX = b.x,
                windowY = b.y,
                windowWidth = b.width,
                windowHeight = b.height,
            )
        }
    }
}

/**
 * Applies the saved window placement with the OS APIs.
 *
 * OS maximize respects the taskbar and the display scaling, unlike Compose's
 * placement on an undecorated window (which can oversize past the screen edge).
 *
 * The saved floating size is only ever clamped against a screen the environment
 * reports as *real*. Every session up to 1.54.16 opened at exactly 526x478,
 * which was the saved size (1207x1017) clamped into a 526x478 rectangle that
 * `maximumWindowBounds` reported before the window was shown: the clamp then both
 * shrank and moved a perfectly valid placement, which is why the window never
 * came back where it was left. A target smaller than any real display is
 * therefore ignored and the saved size is used as it is.
 */
private fun applySavedWindowGeometry(frame: java.awt.Frame) {
    val saved = DesktopSettings.load()
    if (saved.isFullscreen) return
    if (saved.windowMaximized) {
        runCatching { frame.extendedState = java.awt.Frame.MAXIMIZED_BOTH }
        return
    }
    if (saved.windowWidth <= 0 || saved.windowHeight <= 0) return

    val screens = runCatching {
        java.awt.GraphicsEnvironment.getLocalGraphicsEnvironment()
            .screenDevices.map { it.defaultConfiguration.bounds }
    }.getOrDefault(emptyList())
    val union = screens.reduceOrNull { a, b -> a.union(b) }
    // Prefer the screen the window was last on (so one saved on a secondary
    // display returns there); the largest one when the saved point is on none
    // of them (that monitor is gone).
    val target = screens.filter { it.contains(saved.windowX, saved.windowY) }
        .maxByOrNull { it.width.toLong() * it.height }
        ?: screens.maxByOrNull { it.width.toLong() * it.height }
    val usable = (target ?: union)?.takeIf { it.width >= 800 && it.height >= 600 }

    var w = saved.windowWidth
    var h = saved.windowHeight
    var x = saved.windowX
    var y = saved.windowY
    if (usable != null) {
        w = saved.windowWidth.coerceIn(400, usable.width)
        h = saved.windowHeight.coerceIn(300, usable.height)
        x = saved.windowX.coerceIn(usable.x, usable.x + usable.width - w)
        y = saved.windowY.coerceIn(usable.y, usable.y + usable.height - h)
    }
    runCatching { frame.setBounds(x, y, w, h) }
    AppLog.log(
        "window",
        "restore: saved=${saved.windowWidth}x${saved.windowHeight}@${saved.windowX},${saved.windowY} " +
            "usable=${usable?.let { "${it.width}x${it.height}@${it.x},${it.y}" } ?: "none"} applied=${w}x${h}@${x},${y}",
    )
}


/** Minimize / maximize / close buttons. These must be visible at all times
 * because the window is undecorated (no OS title bar): they are shown in the
 * Spotify header when it is visible and overlaid at the top-right otherwise. */
@Composable
internal fun WindowControls(
    language: String,
    isMaximized: Boolean,
    onMinimize: () -> Unit,
    onMaximize: () -> Unit,
    onClose: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Tooltip(Localization.get(language, "tooltip_minimize")) {
            IconButton(
                onClick = onMinimize,
                modifier = Modifier.size(32.dp),
            ) {
                Icon(
                    Icons.Filled.Remove,
                    contentDescription = "Minimize",
                    tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.85f),
                    modifier = Modifier.size(16.dp),
                )
            }
        }
        Tooltip(Localization.get(language, if (isMaximized) "tooltip_restore" else "tooltip_maximize")) {
            IconButton(
                onClick = onMaximize,
                modifier = Modifier.size(32.dp),
            ) {
                Icon(
                    if (isMaximized) Icons.Filled.FilterNone else Icons.Filled.CropSquare,
                    contentDescription = Localization.get(language, if (isMaximized) "tooltip_restore" else "tooltip_maximize"),
                    tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.85f),
                    modifier = Modifier.size(if (isMaximized) 13.dp else 14.dp),
                )
            }
        }
        Tooltip(Localization.get(language, "close")) {
            IconButton(
                onClick = onClose,
                modifier = Modifier.size(32.dp),
            ) {
                Icon(
                    Icons.Filled.Close,
                    contentDescription = "Close",
                    tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.85f),
                    modifier = Modifier.size(16.dp),
                )
            }
        }
    }
}


