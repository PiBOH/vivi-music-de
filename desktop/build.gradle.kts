import org.jetbrains.compose.desktop.application.dsl.TargetFormat
import org.jetbrains.compose.desktop.application.tasks.AbstractJPackageTask
import java.io.File
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream
import java.util.zip.ZipOutputStream

// version.txt is the single source of truth for release metadata. Layout:
//   line 1 = mobile (Android) version, e.g. 6.4.21
//   line 2 = mobile version code (monotonic int)
//   line 3 = mobile release channel (stable / rc / beta / alpha / nightly)
//   line 4 = desktop ("DE") version, e.g. 1.33.52 (the program's own SemVer)
//   line 5 = desktop version code (monotonic counter)
//   line 6 = desktop release channel (stable / rc / beta / alpha / nightly)
//   (comment lines, prefixed with '#', may follow and are ignored)
// The human-readable desktop version is "<mobile>_DE-<de>", e.g. 6.4.21_DE-1.33.52.
val versionLines: List<String> = rootProject.file("version.txt")
    .takeIf { it.exists() }
    ?.readLines()
    ?.map { it.trim() }
    ?.filter { it.isNotEmpty() && !it.startsWith("#") }
    ?: emptyList()

val mobileVersion: String = versionLines.getOrNull(0)?.takeIf { it.isNotEmpty() } ?: "0.0.0"
val deVersion: String = versionLines.getOrNull(3)?.takeIf { it.isNotEmpty() } ?: "0.0.0"
val releaseChannel: String = versionLines.getOrNull(5)?.takeIf { it.isNotEmpty() } ?: "stable"
val fullVersion: String = "${mobileVersion}_DE-${deVersion}"

// Installers/package managers require a purely numeric MAJOR.MINOR.PATCH on
// Windows and macOS (jpackage JDK-8283707, Inno Setup AppVersion). That numeric
// version is the DE version — the part after "DE-".
val numericPackageVersion: String = deVersion.substringBefore('+').substringBefore('-')

plugins {
    kotlin("jvm")
    alias(libs.plugins.compose.compiler)
    alias(libs.plugins.compose.multiplatform)
    alias(libs.plugins.kotlin.serialization)
}

// Ship version.txt + CHANGELOG.md + contributorsde.json as classpath resources
// so the About screen can read build metadata, the changelog and the dynamic
// contributor list at runtime (config-cache friendly).
tasks.processResources {
    from(rootProject.file("version.txt"))
    from(rootProject.file("CHANGELOG.md"))
    from(rootProject.file("contributorsde.json"))
    // The licence, so About → Licence can show it with no network at all (the
    // live copy is fetched from the repository, exactly like the contributors).
    from(rootProject.file("LICENSE"))
    // The vector brand mark: the master lives with the other brand assets in
    // `icons/` (next to logo_vmde.png), and the app reads it from the classpath
    // as `images/logo_vmde.svg`. Copied rather than committed a second time so
    // there is exactly one master to edit.
    from(project.file("icons/logo_vmde.svg")) { into("images") }
}

kotlin {
    jvmToolchain(21)
}

// --- Keep every installer small: runtime icon minimization -----------------
// The extended Material icons artifact bundles ~10k vector icons (~36 MB) while
// the desktop app references fewer than a hundred. Compilation must keep using
// the full artifact (every reference resolves at compile time), but no packaged
// runtime ever needs the other icons: this task copies the full jar into a
// minimized jar that keeps every non-icon class plus the per-icon classes whose
// icon name is referenced in desktop/src (all styles). The minimized jar
// replaces the original on the RUNTIME classpath — the classpath that dev
// `run`, tests and every jpackage setup (hence every installer) consume.
//
// The extended jar holds ONLY per-icon `...Kt.class` files: the `Icons`
// accessors the sources use (`Icons.Filled.Home`, `Icons.Outlined.X`, ...) live
// in the separate material-icons-core artifact, which stays on the runtime
// classpath on purpose — see the runtimeClasspath block at the end of this
// file. Once minimized, this task can therefore never fix a missing `Icons$...`
// class by keeping more entries: that class was never in this jar.
abstract class MinimizeIconsJarTask : DefaultTask() {
    @get:InputFiles
    @get:PathSensitive(PathSensitivity.RELATIVE)
    abstract val sources: ConfigurableFileCollection

    // Compile classpath: contains the full extended icons jar + core jar.
    @get:InputFiles
    @get:PathSensitive(PathSensitivity.NAME_ONLY)
    abstract val compileClasspath: ConfigurableFileCollection

    @get:OutputFile
    abstract val minimizedJar: RegularFileProperty

    @TaskAction
    fun minimize() {
        val styleDirs = setOf("filled", "outlined", "rounded", "sharp", "twotone", "automirrored")
        // Base accessor names kept unconditionally so `Icons.*` never breaks.
        val used = mutableSetOf("icons", "automirrored")
        fun normalize(name: String): String = name.lowercase().removePrefix("kt").trimStart('_')
        val iconRef = Regex(
            """Icons\.(?:Filled|Outlined|Rounded|Sharp|TwoTone|AutoMirrored)(?:\.(?:Filled|Outlined|Rounded|Sharp|TwoTone))?\.([A-Za-z_][A-Za-z0-9_]*)"""
        )
        sources.files.filter { it.isDirectory }.forEach { dir ->
            dir.walkTopDown().filter { it.extension == "kt" }.forEach { file ->
                iconRef.findAll(file.readText()).forEach { used.add(normalize(it.groupValues[1])) }
            }
        }

        val fullJarFile = compileClasspath.files.first { it.name.startsWith("material-icons-extended-desktop") }
        val coreJarFile = compileClasspath.files.firstOrNull { it.name.startsWith("material-icons-core-desktop") }

        // Per-icon classes already provided by the core artifact must not be
        // duplicated on the runtime classpath.
        val coreClasses = HashSet<String>()
        coreJarFile?.let { core ->
            ZipInputStream(core.inputStream().buffered()).use { zin ->
                while (true) {
                    val entry = zin.nextEntry ?: break
                    if (!entry.isDirectory && entry.name.endsWith(".class")) coreClasses.add(entry.name)
                    zin.closeEntry()
                }
            }
        }

        val out = minimizedJar.get().asFile
        out.parentFile.mkdirs()
        var kept = 0
        var total = 0
        ZipInputStream(fullJarFile.inputStream().buffered()).use { zin ->
            ZipOutputStream(out.outputStream().buffered()).use { zout ->
                while (true) {
                    val entry = zin.nextEntry ?: break
                    total++
                    val keep = when {
                        entry.isDirectory -> true
                        entry.name.endsWith(".class") && coreClasses.contains(entry.name) -> false
                        !entry.name.endsWith(".class") -> true
                        !entry.name.startsWith("androidx/compose/material/icons/") -> true
                        else -> {
                            val simple = entry.name.substringAfterLast('/').removeSuffix(".class")
                            if (!simple.endsWith("Kt")) true
                            else {
                                val segments = entry.name.split('/')
                                val inStyleDir = segments.size >= 2 && segments[segments.size - 2] in styleDirs
                                !inStyleDir || normalize(simple.removeSuffix("Kt")) in used
                            }
                        }
                    }
                    if (keep) {
                        kept++
                        zout.putNextEntry(ZipEntry(entry.name))
                        zin.copyTo(zout)
                        zout.closeEntry()
                    }
                    zin.closeEntry()
                }
            }
        }
        logger.lifecycle("minimizeIconsJar: kept $kept/$total entries (referenced icon names: ${used.size})")
    }
}

dependencies {
    implementation(compose.desktop.currentOs)
    implementation(compose.material3)
    implementation(compose.materialIconsExtended)

    // `runComposeUiTest` for `:desktop:dropdownPositionCheck`: the settings rows
    // open a Material 3 `DropdownMenu`, which is a popup, and an
    // `ImageComposeScene` paints no popups. The test harness composes the
    // popups and exposes their semantics, which is the only way to measure
    // where a menu really opens.
    // The `compose.uiTest` accessor resolves an older line of the artifact than
    // the one the runtime ends up with (ui resolves to 1.10.0 through the
    // material3 graph), and that mix fails on the first frame
    // (`PlatformContext` has no `Companion` in it). Pinned to what ui resolves
    // to, so the harness and the app are the same Compose.
    testImplementation("org.jetbrains.compose.ui:ui-test:1.10.0")

    // Material 3 tonal color palette (same seed-based scheme as the Android app)
    implementation(libs.materialKolor)

    // Reused JVM-pure network/parsing modules (same code as the Android app)
    implementation(project(":innertube"))
    implementation(project(":spotify"))
    implementation(project(":lastfm"))
    implementation(project(":kizzy"))
    implementation(project(":shazamkit"))
    implementation(project(":jiosaavn"))
    implementation(project(":lyricsProvider"))
    implementation(project(":sync"))
    implementation(project(":canvas"))
    implementation(project(":applecanvas"))
    implementation(project(":vivimusiccanvas"))

    implementation(libs.kotlinx.coroutines.core)
    // Provides the Swing-based Main dispatcher backed by the AWT/Swing event
    // dispatch thread, required by Dispatchers.Main on desktop. Without it
    // running code that hops back to the Main dispatcher throws
    // "Module with the Main dispatcher is missing". Same version as core.
    implementation(libs.kotlinx.coroutines.swing)

    // Thumbnail / artwork loading (Coil 3, desktop JVM support)
    implementation(libs.coil)
    implementation(libs.coil.network.okhttp)

    // Pure-Java MP4 (incl. fragmented/DASH fMP4) demuxer + bundled JAAD AAC decoder
    // for self-contained desktop audio playback.
    implementation("org.jcodec:jcodec:0.2.5")

    // Native OS system-volume access (WinMM wave output on Windows) so the
    // Android system volume can be mirrored on the desktop and vice versa.
    implementation("net.java.dev.jna:jna:5.14.0")
    // Cross-platform global key hook (macOS/Linux media keys). The Windows
    // path keeps its own low-level hook (see MediaKeys.kt); JNativeHook is
    // only used on the other OSes, where it needs macOS Accessibility
    // permission (fails gracefully when not granted).
    implementation("com.github.kwhat:jnativehook:2.2.2")

    // JavaFX WebView for the embedded YouTube sign-in window. The platform
    // classifier is picked from the BUILDING machine: each OS CI job builds its
    // own package, so the packaged app always ships the matching natives.
    val buildOs = System.getProperty("os.name").lowercase()
    val buildArm = System.getProperty("os.arch").lowercase().let { it.contains("aarch64") || it.contains("arm") }
    val javafxClassifier = when {
        buildOs.contains("win") -> "win"
        buildOs.contains("mac") || buildOs.contains("darwin") -> if (buildArm) "mac-aarch64" else "mac"
        else -> if (buildArm) "linux-aarch64" else "linux"
    }
    val javafxVersion = "21.0.4"
    implementation("org.openjfx:javafx-base:$javafxVersion:$javafxClassifier")
    implementation("org.openjfx:javafx-graphics:$javafxVersion:$javafxClassifier")
    implementation("org.openjfx:javafx-controls:$javafxVersion:$javafxClassifier")
    implementation("org.openjfx:javafx-media:$javafxVersion:$javafxClassifier")
    implementation("org.openjfx:javafx-web:$javafxVersion:$javafxClassifier")

    // JCEF (a real Chromium) for the Spotify sign-in window, so the page sees the
    // Chromium engine and not a WebView it can flag as embedded (issue #97). Only
    // the small Java layer is a build dependency: the ~100 MB of natives are
    // downloaded and unpacked under ~/.vivimusic/cef the first time the window is
    // opened, so the packaged installers stay the size they were.
    implementation("me.friwi:jcefmaven:152.0.6")

    implementation(libs.ktor.client.core)
    implementation(libs.ktor.client.okhttp)
    implementation(libs.ktor.client.content.negotiation)
    implementation(libs.ktor.serialization.json)

    // Local LAN relay (embedded WebSocket server) for offline device pairing
    implementation(libs.ktor.server.core)
    implementation(libs.ktor.server.netty)
    implementation(libs.ktor.server.websockets)

    // QR code (ZXing) + mDNS service registration (JmDNS) for LAN discovery
    implementation(libs.zxing.core)
    implementation(libs.jmdns)

    // Drag-to-reorder for the Queue screen (same lib as the Android app)
    implementation(libs.compose.reorderable)
}

val minimizeIconsJar = tasks.register<MinimizeIconsJarTask>("minimizeIconsJar") {
    group = "build"
    description = "Ships only the Material icons the desktop app actually references"
    sources.from(files("src/main/kotlin", "src/main/java"))
    compileClasspath.from(configurations.compileClasspath)
    minimizedJar.set(layout.buildDirectory.file("libs/material-icons-extended-desktop-minimized.jar"))
}

// Compilation keeps the full icons artifact; the packaged runtime (dev run,
// tests and every jpackage/Inno Setup image) gets the minimized jar instead.
configurations.runtimeClasspath {
    exclude(group = "org.jetbrains.compose.material", module = "material-icons-extended-desktop")
}
dependencies {
    runtimeOnly(files(minimizeIconsJar))

    // The extended artifact is the ONLY route through which material-icons-core
    // reaches this project (extended -> core), and core is the artifact that
    // declares the `Icons` accessors the sources use (`Icons.class`,
    // `Icons$Filled`, `Icons$Outlined`, ...); the extended jar itself only holds
    // the per-icon `...Kt.class` files. Excluding the extended artifact above
    // therefore took core away with it, and every build from 1.50.72 to 1.50.74
    // crashed on the very first icon it drew — the crash dump reads
    // `NoClassDefFoundError: androidx/compose/material/icons/Icons$Outlined`
    // from `MainKt.Sidebar`, i.e. the app never got past its first frame.
    //
    // So the exclude above may only ever drop the ~36 MB extended jar, and core
    // is put back here. Its version is read from the already-resolved compile
    // classpath instead of being written out by hand, so it can never drift away
    // from the version the Compose plugin selects; the `check` turns a future
    // change that removes core from the graph into a loud packaging failure
    // instead of a release that crashes on startup for every user.
    runtimeOnly(
        files(
            configurations.compileClasspath.map { compileClasspath ->
                val iconsCore = compileClasspath.files.filter {
                    it.name.startsWith("material-icons-core-desktop")
                }
                check(iconsCore.isNotEmpty()) {
                    "material-icons-core-desktop is missing from the compile classpath: without it " +
                        "the runtime classpath has no `androidx.compose.material.icons.Icons` " +
                        "accessor classes and the packaged app crashes on its first icon " +
                        "(NoClassDefFoundError: Icons\$Outlined)."
                }
                iconsCore
            },
        ),
    )
}

// --- Class-data sharing: the JVM's part of the start ------------------------
// The runtime jpackage bundles is a jlinked image, and jlink writes no CDS
// archive into it: next to the runtime's JVM library (`bin/server` on Windows,
// `lib/server` elsewhere) there is nothing but the library itself. Every JDK
// class the app touches is therefore parsed out of `lib/modules` on each start -
// AWT for the first window, `javax.sound` for the audio line, java.base's
// collections - before a single frame is drawn.
//
// This task dumps those classes once, at build time, into the archive the JVM
// reads by default (`classes.jsa`, beside its own library), so every installer
// carries it and no `-XX:SharedArchiveFile` has to be passed to anything: the
// archive is found wherever the app is installed, because it travels inside the
// runtime installed with it.
//
// Two properties make that work for every user's install path:
//  - it is dumped with NO classpath (`-Xshare:dump` plus the class list, nothing
//    else). A CDS archive records the classpath it was built with, and the JVM
//    discards it when the classpath at start differs - which it always does when
//    the installed directory is not the build machine's. With no classpath
//    recorded, the archive is accepted from any directory;
//  - only JDK classes are in `cds/classlist.txt`, i.e. the ones that come from
//    `modules` instead of the jars on that classpath. Archiving the app's own
//    classes would record the classpath again and tie the archive to this
//    machine.
//
// The list is the JDK classes a real start loads, recorded with
// `./gradlew :desktop:startupProbe -PcdsList=...` (see that task): regenerate it
// when the app starts using parts of the JDK it did not touch before. A class in
// the list that this JDK does not have is reported by the dump and skipped, so a
// list recorded on Windows still dumps on Linux and macOS.
//
// The dump has to be run BY the runtime that ships, not by the build JDK: an
// archive records the `lib/modules` image it was dumped from (`This file is not
// the one used while building the shared archive file ... size has changed`) and
// a jlinked image is a different, smaller one. Dumping with the build JDK and
// copying the file in was measured and does nothing at all: the JVM maps the
// archive, sees the image does not match and loads every class from `modules`
// anyway. jlink leaves the runtime without a launcher, so the build JDK's `java`
// is put in the runtime for the dump and taken away again.
abstract class CdsArchiveTask : DefaultTask() {

    /** The classes the archive holds, one `java/lang/Object` per line. */
    @get:InputFile
    @get:PathSensitive(PathSensitivity.RELATIVE)
    abstract val classList: RegularFileProperty

    /** The JDK the launcher comes from, as `<version>|<java.home>`. */
    @get:Input
    abstract val javaRuntime: Property<String>

    /** The runtime image jpackage copies into every image it builds. */
    @get:Internal
    abstract val runtimeImageDir: DirectoryProperty

    /** `classes.jsa`, in the directory of the runtime's own JVM library. */
    @get:OutputFile
    abstract val archive: RegularFileProperty

    @TaskAction
    fun dump() {
        val runtime = runtimeImageDir.get().asFile
        // The JVM looks for its archive beside its own library. That directory
        // is found rather than guessed, so a layout change can never leave an
        // archive behind that no JVM would open.
        val jvmLibrary = runtime.walkTopDown().firstOrNull {
            it.name == "jvm.dll" || it.name == "libjvm.so" || it.name == "libjvm.dylib"
        } ?: throw GradleException(
            "The runtime image has no JVM library under ${runtime.path}: there is no " +
                "directory the JVM would look in for a CDS archive.",
        )
        val target = File(jvmLibrary.parentFile, "classes.jsa")
        target.delete() // -Xshare:dump writes a new file and refuses a stale one

        val javaHome = File(javaRuntime.get().substringAfter('|'))
        val copied = mutableListOf<File>()
        try {
            val launcher = File(javaHome, "bin/java.exe").takeIf { it.exists() }
                ?: File(javaHome, "bin/java")
            val deployed = File(runtime, "bin/${launcher.name}").also {
                launcher.copyTo(it, overwrite = true)
                // `Files.copy` copies the bytes, not the mode: the copy lands with
                // the process umask (644 on a runner), and a launcher that is not
                // executable cannot be spawned at all. On Linux and macOS that made
                // the very first `-Xshare:dump` fail with an IOException before the
                // JVM printed anything, which is what held both desktop releases
                // back. The bit is set for every user and not only the owner: the
                // runtime image is shipped as it is, and the JVM inside it is
                // expected to be startable by whoever unpacks the installer.
                if (!it.setExecutable(true, false)) {
                    throw GradleException(
                        "The copied JDK launcher ${it.path} could not be made executable, so " +
                            "-Xshare:dump cannot start.",
                    )
                }
                copied += it
            }
            var result = runDump(deployed, target)
            if (result.first != 0) {
                // The launcher's own support library is part of the jlinked image
                // on Windows; where it is not, the launcher cannot start at all,
                // so it is taken along and the dump tried once more.
                listOf("bin/jli.dll", "lib/libjli.so", "lib/libjli.dylib").forEach { path ->
                    val from = File(javaHome, path)
                    val to = File(runtime, path)
                    if (from.exists() && !to.exists()) {
                        to.parentFile.mkdirs()
                        from.copyTo(to, overwrite = true)
                        copied += to
                    }
                }
                result = runDump(deployed, target)
            }
            val (exit, output) = result
            if (exit != 0 || !target.exists()) {
                throw GradleException(
                    "The CDS archive could not be dumped (exit $exit):\n" + output.takeLast(4000),
                )
            }
            logger.lifecycle("CDS archive: ${target.length() / 1024} KB at ${target.path}")
        } finally {
            copied.forEach { it.delete() }
        }
    }

    /** Runs `-Xshare:dump` with [java] and answers `exit code to output`. */
    private fun runDump(java: File, target: File): Pair<Int, String> {
        val process = ProcessBuilder(
            java.absolutePath,
            "-Xshare:dump",
            "-XX:SharedClassListFile=${classList.get().asFile.absolutePath}",
            "-XX:SharedArchiveFile=${target.absolutePath}",
        ).directory(target.parentFile).redirectErrorStream(true).start()
        val output = process.inputStream.bufferedReader().use { it.readText() }
        return process.waitFor() to output
    }
}

val cdsArchive = tasks.register<CdsArchiveTask>("cdsArchive") {
    group = "build"
    description = "Dumps the class-data-sharing archive that ships inside the runtime"
    // The runtime image has to exist first: the archive goes beside its JVM.
    dependsOn(tasks.matching { it.name == "createRuntimeImage" })
    classList.set(layout.projectDirectory.file("cds/classlist.txt"))
    javaRuntime.set(
        javaToolchains.launcherFor { languageVersion = JavaLanguageVersion.of(21) }.map {
            "${it.metadata.languageVersion.asInt()}|${it.metadata.installationPath.asFile.absolutePath}"
        },
    )
    runtimeImageDir.set(layout.buildDirectory.dir("compose/tmp/main/runtime"))
    archive.set(
        layout.buildDirectory.file(
            "compose/tmp/main/runtime/" +
                (if (System.getProperty("os.name").startsWith("Windows", ignoreCase = true)) "bin" else "lib") +
                "/server/classes.jsa",
        ),
    )
}

// Every jpackage invocation copies that runtime image into the image it builds
// (createDistributable, packageMsi, packageDeb, packageDmg, packagePkg, the
// AppImage, the rpm), so each one waits for the archive to be in it.
tasks.withType<AbstractJPackageTask>().configureEach { dependsOn(cdsArchive) }

compose.desktop {
    application {
        // The entry point lives in `MainWindow.kt` (the window, the bar and the
        // queue were moved out of `Main.kt`), so this is `MainWindowKt`. It was
        // left on `MainKt` for one release and the packaged app died on launch
        // with "main method not found in class com.music.vivi.desktop.MainKt" -
        // the dev checks call `MainWindowKt.main` by name and never read this
        // setting, so only the installer was broken.
        mainClass = "com.music.vivi.desktop.MainWindowKt"

        // JVM tuning for glitch-free audio (issue #3).
        //
        // Audio is played by a Java thread that hands PCM to the sound card, and
        // a stop-the-world pause freezes that thread no matter how much audio
        // the app has buffered in its own queues: the device ring drains while
        // the JVM is frozen, and the user hears a gap (the same pause is what
        // makes the UI hitch at that moment).
        //
        // The default heap is 25% of the machine's RAM (a 6 GB max heap on a
        // 24 GB Mac, as reported in a user's system info) and G1 is then free to
        // grow the young generation up to 60% of it — i.e. collection sizes (and
        // pauses) far beyond what a music player needs. Capping the heap and the
        // young generation, and asking G1 for a 20 ms pause target, keeps the
        // stop-the-world part in the low-millisecond range. All flags are
        // product flags (no -XX:+UnlockExperimentalVMOptions needed) and were
        // verified to start on Temurin 21.
        jvmArgs += listOf(
            "-Xmx2g",
            "-XX:+UseG1GC",
            "-XX:MaxGCPauseMillis=20",
            // The young generation was capped at 384 MB, and the owner's own
            // `gc.log` measures what that costs: a `G1 Evacuation Pause` of
            // 279/371/412/444/480 ms every two to four minutes, each one matched
            // by an `audio priority stall` of the same size in `playback.log`
            // (497/357/386/441 ms) while the live set is only ~67 MB — i.e. the
            // pauses are the young size, not the heap. `MaxGCPauseMillis=20`
            // cannot help against a young generation that is allowed to grow to
            // 384 MB. 128 MB is the same policy at a third of the pause, paid for
            // with more frequent (short) collections.
            "-XX:NewSize=64m",
            "-XX:MaxNewSize=128m",
            "-XX:MaxMetaspaceSize=256m",
            // Skiko (Compose Desktop's renderer) calls `System.gc()` every 30 s by
            // design, to trim memory on a parked window. Its `FrameWatcher`
            // coroutine reads `gcDelayMillis = 30000`, waits, and does
            // `if (frameCounter.get() < minFramesToRenderer /* 1000 */) System.gc()`,
            // i.e. whenever the UI is not animating at high frame rate. The
            // counter is reset each round, so a player that only redraws a seek
            // bar never reaches 1000 frames and gets the call every single time.
            // An explicit gc() is a **full, stop-the-world** collection: measured
            // on the packaged 1.50.76 image with -Xlog:gc it is `Pause Full
            // (System.gc())` every 30.05 s, 46-69 ms on a fresh session and up to
            // 1976 ms once the session has grown, which freezes the audio writer
            // thread and the UI together (the "pauses/skips + UI hitch" of #3).
            // This flag turns every explicit gc() into a concurrent G1 cycle
            // instead: same memory reclamation, no stop-the-world full GC. Same
            // run after the flag: zero `Pause Full`, the 30 s event becomes a
            // bounded 2.9-9.9 ms young pause that the 8 s PCM queue absorbs.
            //
            // That was not the end of it. The reporter's own 1.54.29 session
            // (13 Oct, issue #3 while NOT paired - his own finding) shows the
            // same 30.0 s event still starting a concurrent cycle whose
            // `G1 Concurrent GC` event ran 33 / 104 / 120 / 214 ms, and the
            // 214 ms one held the audio writer's 50 ms sleep 221 ms late
            // (`audio priority stall: ... 171ms held up at the writer's
            // priority`, heap 119 MB of 2 GB, cpu 13% of 59% busy - the process
            // was neither busy nor short of memory). The explicit call itself is
            // what has to go: `-XX:+DisableExplicitGC` ignores every
            // `System.gc()`, so Skiko's every-30-s trim buys nothing and costs
            // nothing at all (G1 keeps collecting on its own heuristics), and
            // the periodic disturbance disappears instead of merely becoming
            // concurrent - which is what "not one millisecond is played fluidly"
            // was still measuring after the previous flag.
            "-XX:+DisableExplicitGC",
            // JCEF (the Chromium sign-in window) needs reflective access to AWT
            // internals on JDK 16+; the macOS packages need the three lwawt ones,
            // and they are harmless where the package does not exist. See
            // https://github.com/jcefmaven/jcefmaven ("To run on JDK 16 or later").
            "--add-opens=java.desktop/sun.awt=ALL-UNNAMED",
            "--add-opens=java.desktop/sun.lwawt=ALL-UNNAMED",
            "--add-opens=java.desktop/sun.lwawt.macosx=ALL-UNNAMED",
        )

        nativeDistributions {
            targetFormats(
                TargetFormat.Msi,
                TargetFormat.Exe,
                TargetFormat.Dmg,
                TargetFormat.Pkg,
                TargetFormat.Deb,
            )
            // jlink bundles only a minimal set of modules by default. The dev
            // tools (CPU/RAM/thread stats) use `java.lang.management` and the
            // richer `com.sun.management` bean; without these modules the
            // packaged launcher crashes on startup with "Failed to launch JVM".
            // jlink includes only JDK modules. JavaFX is shipped as regular runtime
            // dependencies in the application image and is launched via Stage,
            // so the packaged embedded WebView does not need Swing interop.
            // jdk.jsobject provides netscape.javascript.JSObject, which the
            // JavaFX WebView requires at runtime — without it WebView creation
            // throws NoClassDefFoundError in packaged builds.
            //
            // JavaFX is shipped as CLASSPATH jars, so jlink cannot see its
            // module requirements. Missing modules silently break the WebView:
            //  - java.net.http  → javafx.web requires it; without it the page
            //    load hangs at RUNNING forever (white window, no error dialog);
            //  - jdk.unsupported → sun.misc.Unsafe needed by the Marlin 2D
            //    rendering engine; without it QuantumRenderer aborts painting
            //    (white window).
            // Both verified with a limited-modules reproduction (DE 1.34.2).
            modules(
                "java.management", "jdk.management", "jdk.jsobject",
                "java.net.http", "jdk.unsupported",
            )
            packageName = "VIVIMusic"
            packageVersion = numericPackageVersion
            description = "VIVI Music Desktop Edition"
            vendor = "PiBOH"

            windows {
                // The Start Menu folder: the SAME name the Inno Setup installer
                // uses (DefaultGroupName in installer/windows/VIVIMusic.iss). It
                // used to say "VIVI Music" here, so an install made with the MSI
                // put the app under one name and an install made with the .exe
                // under another.
                menuGroup = "VIVI Music DE"
                // Machine-wide install into Program Files (requires admin/UAC).
                perUserInstall = false
                installationPath = "C:/Program Files/VIVIMusic"
                iconFile.set(project.file("icons/logo_vmde.ico"))
            }

            linux {
                debMaintainer = "PiBOH"
                appCategory = "Audio"
                iconFile.set(project.file("icons/logo_vmde.png"))
            }

            macOS {
                bundleID = "com.vivi.vivimusic.desktop"
                minimumSystemVersion = "10.15"
                iconFile.set(project.file("icons/logo_vmde.icns"))
            }
        }
    }
}

/**
 * Headless check of the changelog emoji:
 *
 *     ./gradlew :desktop:changelogEmojiCheck
 *
 * See `desktop/src/test/kotlin/com/music/vivi/desktop/ChangelogEmojiRenderCheck.kt`.
 * It renders the changelog headings and the emoji on their own through the real
 * Markdown path into an off-screen `ImageComposeScene` (no window, no GPU) and
 * counts ink and chromatic pixels, which tells "drawn in colour" apart from
 * "drawn by the wrong, glyph-less font" and from "not drawn at all". It is a
 * manual check, not part of `check` (it needs no test framework, writes PNGs
 * under .ignore/ and is about pixels, not assertions).
 */
tasks.register<JavaExec>("changelogEmojiCheck") {
    group = "verification"
    description = "Renders the changelog emoji headlessly and reports ink and colour"
    dependsOn(tasks.named("testClasses"))
    mainClass.set("com.music.vivi.desktop.ChangelogEmojiRenderCheck")
    classpath = sourceSets.getByName("test").runtimeClasspath
}

/**
 * Headless check of the frameless window's drag surface:
 *
 *     ./gradlew :desktop:framelessHitTestCheck
 *
 * See `desktop/src/test/kotlin/com/music/vivi/desktop/FramelessHitTestCheck.kt`.
 * `WindowDraggableArea` begins moving the window on the pointer down (verified
 * in the shipped bytecode), so where the drag surface is hit matters: behind the
 * interface means "only over empty space", in front would mean "every button
 * click nudges the window". The check renders both stacks off screen and counts
 * which layer received the press.
 */
tasks.register<JavaExec>("framelessHitTestCheck") {
    group = "verification"
    description = "Checks that the frameless window's drag surface only takes presses over empty space"
    dependsOn(tasks.named("testClasses"))
    mainClass.set("com.music.vivi.desktop.FramelessHitTestCheck")
    classpath = sourceSets.getByName("test").runtimeClasspath
}

/**
 * Headless check of the automatic deletion of old logs:
 *
 *     ./gradlew :desktop:localDataMaintenanceCheck
 *
 * See `desktop/src/test/kotlin/com/music/vivi/desktop/LogPruneCheck.kt`. It runs
 * the real pruning rule (`AppLog.staleLogs`) against files in a temporary
 * directory and asserts which ones it selects: only logs older than seven days,
 * never the session being written and never a data file.
 */
tasks.register<JavaExec>("localDataMaintenanceCheck") {
    group = "verification"
    description = "Checks that only logs older than seven days are deleted automatically"
    dependsOn(tasks.named("testClasses"))
    mainClass.set("com.music.vivi.desktop.LogPruneCheck")
    classpath = sourceSets.getByName("test").runtimeClasspath
}

/**
 * Headless smoke check of the first-run screen:
 *
 *     ./gradlew :desktop:firstRunRenderCheck
 *
 * See `desktop/src/test/kotlin/com/music/vivi/desktop/FirstRunRenderCheck.kt`. It
 * renders `FirstRunFlow` off screen at the app's minimum window size and at a
 * large one, which is the only screen a fresh install shows before the app and
 * therefore the one that cannot be allowed to crash or to draw nothing.
 */
/**
 * Headless check of the Listen Together reply envelope:
 *
 *     ./gradlew :desktop:ltReplyCodecCheck
 *
 * See `desktop/src/test/kotlin/com/music/vivi/desktop/LtReplyCodecCheck.kt`. The
 * relay carries no field for a quote, so the Android client embeds it in the
 * message text as `<ZWSP>[RPLY:<base64(author|message)>]<ZWSP>` and strips it on
 * receive. This check builds the envelope and reads it back, and asserts the
 * mobile's own byte layout (marker, closing marker, base64 body of
 * `author|message`), so a reply written here is a reply there and vice versa.
 */
tasks.register<JavaExec>("ltReplyCodecCheck") {
    group = "verification"
    description = "Checks the Listen Together reply envelope against the mobile wire format"
    dependsOn(tasks.named("testClasses"))
    mainClass.set("com.music.vivi.desktop.LtReplyCodecCheck")
    classpath = sourceSets.getByName("test").runtimeClasspath
}

/**
 * Headless check of the player design screen:
 *
 *     ./gradlew :desktop:playerDesignRenderCheck
 *
 * See `desktop/src/test/kotlin/com/music/vivi/desktop/PlayerDesignRenderCheck.kt`.
 * The screen holds four cards (the full player, the player background with the
 * rotating artwork, the mini player, the slider style); the check renders it off
 * screen, counts the cards and their rows in the middle column of the pixels, and
 * asserts that the chips draw the value they are given.
 */
tasks.register<JavaExec>("playerDesignRenderCheck") {
    group = "verification"
    description = "Renders the player design screen headlessly and checks its four cards"
    dependsOn(tasks.named("testClasses"))
    mainClass.set("com.music.vivi.desktop.PlayerDesignRenderCheck")
    classpath = sourceSets.getByName("test").runtimeClasspath
}

tasks.register<JavaExec>("firstRunRenderCheck") {
    group = "verification"
    description = "Renders the first-run flow headlessly and checks that it paints"
    dependsOn(tasks.named("testClasses"))
    mainClass.set("com.music.vivi.desktop.FirstRunRenderCheck")
    classpath = sourceSets.getByName("test").runtimeClasspath
}

/**
 * Headless check of the Material 3 Expressive shapes:
 *
 *     ./gradlew :desktop:cookieShapesCheck
 *
 * See `desktop/src/test/kotlin/com/music/vivi/desktop/CookieShapesCheck.kt`. The
 * 4-sided cookie, the 7-sided cookie and the clover are arithmetic, so each is
 * painted off screen and probed where the three differ: the flat top of the
 * first, the apex of the second, the lobe of every side of the third, and the
 * empty corners all three leave.
 */
tasks.register<JavaExec>("cookieShapesCheck") {
    group = "verification"
    description = "Paints the cookie and clover shapes headlessly and checks their outlines"
    dependsOn(tasks.named("testClasses"))
    mainClass.set("com.music.vivi.desktop.CookieShapesCheck")
    classpath = sourceSets.getByName("test").runtimeClasspath
}

/**
 * Headless check of the Material 3 motion tokens:
 *
 *     ./gradlew :desktop:motionCheck
 *
 * See `desktop/src/test/kotlin/com/music/vivi/desktop/MotionCheck.kt`. The whole
 * app moves on `Motion`, and a table that drifted from the published durations
 * or curves would keep compiling and simply move differently. The check samples
 * every easing token against its published control points, walks the sixteen
 * duration steps, proves the Animation-speed setting still scales them, and
 * paints `Modifier.hoverGrow` at rest and at its hovered end (the hovered shape
 * must cover strictly more pixels).
 */
tasks.register<JavaExec>("motionCheck") {
    group = "verification"
    description = "Checks the M3 motion tokens and the hover movement headlessly"
    dependsOn(tasks.named("testClasses"))
    mainClass.set("com.music.vivi.desktop.MotionCheck")
    classpath = sourceSets.getByName("test").runtimeClasspath
}

/**
 * Headless check of the Listen Together chat list:
 *
 *     ./gradlew :desktop:chatWindowRenderCheck
 *
 * See `desktop/src/test/kotlin/com/music/vivi/desktop/ChatWindowRenderCheck.kt`.
 * The chat is a `Window`, so the conversation was split into
 * `ListenTogetherChatList`, which this renders off screen: an empty conversation
 * must draw its empty state, a conversation must draw its bubbles, and a bubble
 * that quotes another one must draw strictly more than the same message without
 * the quote, which is what proves the quote reaches the screen.
 */
tasks.register<JavaExec>("chatWindowRenderCheck") {
    group = "verification"
    description = "Renders the Listen Together chat list headlessly and checks the quoted reply"
    dependsOn(tasks.named("testClasses"))
    mainClass.set("com.music.vivi.desktop.ChatWindowRenderCheck")
    classpath = sourceSets.getByName("test").runtimeClasspath
}

/**
 * Headless check of the live log window and its scroll controls:
 *
 *     ./gradlew :desktop:liveLogRenderCheck
 *
 * See `desktop/src/test/kotlin/com/music/vivi/desktop/LiveLogWindowRenderCheck.kt`.
 * A log with lines must draw, and the sidebar scrollbar must be a real slider:
 * the thumb of a 4000-line log has to be strictly shorter than the thumb of a
 * 120-line one, which a painted full-height strip could not be. The two jump
 * buttons and the follow state are interaction, not visible in a still frame.
 */
/**
 * Controlled check of the account's write path:
 *
 *     ./gradlew :desktop:playlistWriteCheck
 *
 * See `desktop/src/test/kotlin/com/music/vivi/desktop/PlaylistWriteCheck.kt`.
 * Creates one `VIVI write check <timestamp>` playlist, adds one song, reads it
 * back, asks the library whether it lists it, and deletes it again — one line
 * per step, so a failure names the request instead of leaving it a guess. It
 * writes to the signed-in account (one playlist, removed at the end).
 */
tasks.register<JavaExec>("playlistWriteCheck") {
    group = "verification"
    description = "Creates, fills, reads back and deletes one test playlist on the account"
    dependsOn(tasks.named("testClasses"))
    mainClass.set("com.music.vivi.desktop.PlaylistWriteCheck")
    classpath = sourceSets.getByName("test").runtimeClasspath
}

tasks.register<JavaExec>("liveLogRenderCheck") {
    group = "verification"
    description = "Renders the live log window headlessly and checks its scrollbar"
    dependsOn(tasks.named("testClasses"))
    mainClass.set("com.music.vivi.desktop.LiveLogWindowRenderCheck")
    classpath = sourceSets.getByName("test").runtimeClasspath
}

/**
 * Headless check of the Listen Together lobby and room:
 *
 *     ./gradlew :desktop:ltRoomRenderCheck
 *
 * See `desktop/src/test/kotlin/com/music/vivi/desktop/LtRoomRenderCheck.kt`. The
 * room needs a live relay connection, so it was split into `LtLobby` and
 * `LtInRoom`, which this renders off screen: the lobby must draw, an avatar of
 * each kind (initial, emoji) must draw, the room must draw for a host and for a
 * guest, and the two roles must NOT draw the same picture — the host sees the
 * moderation controls and the guest the lock notice, so identical output would
 * mean one of the two branches is not on screen.
 */
tasks.register<JavaExec>("ltRoomRenderCheck") {
    group = "verification"
    description = "Renders the Listen Together lobby and room headlessly and checks both roles"
    dependsOn(tasks.named("testClasses"))
    mainClass.set("com.music.vivi.desktop.LtRoomRenderCheck")
    classpath = sourceSets.getByName("test").runtimeClasspath
}

/**
 * End-to-end check of backup restore:
 *
 *     ./gradlew :desktop:backupRestoreCheck [--args="<file> <file> …"]
 *
 * See `desktop/src/test/kotlin/com/music/vivi/desktop/BackupRestoreCheck.kt`. It
 * restores a backup this build just wrote, the legacy single-JSON file (with and
 * without a byte-order mark), an archive whose entries sit under a folder, and an
 * archive with a corrupt settings entry, and it refuses to call the last one a
 * success. Runs against a temporary user.home; pass a real
 * `*.vivide.backup` on the command line to check that file too.
 */
tasks.register<JavaExec>("backupRestoreCheck") {
    group = "verification"
    description = "Restores real and synthetic backups through BackupManager and reports what each one does"
    dependsOn(tasks.named("testClasses"))
    mainClass.set("com.music.vivi.desktop.BackupRestoreCheck")
    classpath = sourceSets.getByName("test").runtimeClasspath
}

/**
 * Network probe for the "HTTP 403 downloading audio" playback failure:
 *
 *     ./gradlew :desktop:streamResolveProbe [--args="videoId videoId"]
 *
 * See `desktop/src/test/kotlin/com/music/vivi/desktop/StreamResolveProbe.kt`. It
 * runs the real resolver and fetches the first bytes of every candidate URL with
 * that candidate's own User-Agent, printing the client each URL came from and
 * the HTTP status, which is what tells "the chain is refused" apart from "the
 * chain is fine but the wrong candidate was used". Needs the network.
 */
tasks.register<JavaExec>("streamResolveProbe") {
    group = "verification"
    description = "Resolves real audio stream URLs and reports which candidates answer 403"
    dependsOn(tasks.named("testClasses"))
    mainClass.set("com.music.vivi.desktop.StreamResolveProbe")
    classpath = sourceSets.getByName("test").runtimeClasspath
}

/**
 * Headless check of the custom colour wheel:
 *
 *     ./gradlew :desktop:colorWheelRenderCheck
 *
 * See `desktop/src/test/kotlin/com/music/vivi/desktop/ColorWheelRenderCheck.kt`.
 * The picker is one round wheel on the right of the Theme & Colors screen (hue
 * around the rim, saturation from the centre out) with the brightness strip
 * beside it, in place of the three HSV bars it used to be. This renders the real
 * `ThemeSection` into an off-screen `ImageComposeScene` with the picker open and
 * reads the pixels back, because a wheel is a picture and nothing else can tell a
 * wheel from a rectangle: the disc must be round and the wheel's own size, its
 * four cardinal samples must be red, yellow-green, cyan and blue the way the
 * sweep gradient and the pointer's clockwise angle put them, its centre white and
 * its rim saturated, the marker must sit where the accent the screen opened on
 * is (right for red, lower-left for green), the strip beside it must run from the
 * full colour to black, and no wide thin colour bar may be left on the screen.
 */
tasks.register<JavaExec>("colorWheelRenderCheck") {
    group = "verification"
    description = "Renders the custom colour wheel headlessly and checks the disc, its hues and its marker"
    dependsOn(tasks.named("testClasses"))
    mainClass.set("com.music.vivi.desktop.ColorWheelRenderCheck")
    classpath = sourceSets.getByName("test").runtimeClasspath
}

/**
 * Headless check of the Google sign-in screen:
 *
 *     ./gradlew :desktop:loginRenderCheck
 *
 * See `desktop/src/test/kotlin/com/music/vivi/desktop/LoginRenderCheck.kt`.
 * The screen is a hero (the Google mark on a filled tile, what it is for, the
 * state as one pill) plus one card that numbers the three steps and carries the
 * single button. This renders both into an off-screen `ImageComposeScene` and
 * measures the pixels: the tile must be a solid filled square, the button below
 * it must share that fill, and the state pill must change when the account
 * changes. A manual check, not part of `check` (pixels, no test framework, PNGs
 * under .ignore/).
 */
tasks.register<JavaExec>("loginRenderCheck") {
    group = "verification"
    description = "Renders the Google sign-in screen headlessly and checks its tile, button and state"
    dependsOn(tasks.named("testClasses"))
    mainClass.set("com.music.vivi.desktop.LoginRenderCheck")
    classpath = sourceSets.getByName("test").runtimeClasspath
}

/**
 * Headless check of the folding group on the Device sync screen:
 *
 *     ./gradlew :desktop:deviceSyncFoldCheck
 *
 * See `desktop/src/test/kotlin/com/music/vivi/desktop/DeviceSyncFoldCheck.kt`.
 * The two volume channels sit in `M3SettingsCollapsibleGroup`, whose header is the
 * card's first row: folding it has to leave that one row, draw the two switches
 * only while it is open, and turn the chevron. This renders both states into an
 * off-screen `ImageComposeScene` and reads the pixels back - one card band against
 * three joined by 3 dp seams, a header of at least the 56 dp a list row is, the
 * switches' `primary` track in exactly two body rows, and the chevron's ink wide at
 * the top and narrow at the bottom folded against narrow-then-wide open, in
 * `primary`. A manual check, not part of `check` (pixels, no test framework, PNGs
 * under .ignore/).
 */
tasks.register<JavaExec>("deviceSyncFoldCheck") {
    group = "verification"
    description = "Renders the folding Advanced group headlessly and checks the fold, the two switches and the chevron"
    dependsOn(tasks.named("testClasses"))
    mainClass.set("com.music.vivi.desktop.DeviceSyncFoldCheck")
    classpath = sourceSets.getByName("test").runtimeClasspath
}

/**
 * Headless check of the Material 3 rebuild of *Import from Spotify* and of the
 * signed-in Account head:
 *
 *     ./gradlew :desktop:spotifyAccountRenderCheck
 *
 * See `desktop/src/test/kotlin/com/music/vivi/desktop/SpotifyAccountRenderCheck.kt`.
 * The two screens are now the shared `SignInHero` (a 64 dp `primary` tile, a
 * promise and one state pill), the shared numbered steps and the settings groups;
 * the manual-cookie form folds through `M3CollapsibleCard`. This renders those
 * pieces into an off-screen `ImageComposeScene` and measures them: the tile must
 * be the 64 dp square and the import button must share its fill, the three source
 * rows must be one card joined by 3 dp seams with exactly the two selected rows
 * carrying `primary` checkbox ink, the Account head must change with the name, and
 * the folding card must be one header row against header + body joined by 3 dp. A
 * manual check, not part of `check` (pixels, no test framework, PNGs under
 * .ignore/).
 */
tasks.register<JavaExec>("spotifyAccountRenderCheck") {
    group = "verification"
    description = "Renders the Spotify import and Account screens headlessly and checks the hero, the source card and the folding form"
    dependsOn(tasks.named("testClasses"))
    mainClass.set("com.music.vivi.desktop.SpotifyAccountRenderCheck")
    classpath = sourceSets.getByName("test").runtimeClasspath
}

/**
 * Headless check of the language picker's search box:
 *
 *     ./gradlew :desktop:languageSearchCheck
 *
 * See `desktop/src/test/kotlin/com/music/vivi/desktop/LanguageSearchCheck.kt`.
 * Settings -> Language lists 48 languages, so the menu carries a search field.
 * This reads `Languages.matching` directly (a blank query is every language,
 * "ital" is Italiano alone, the accents and the Azerbaijani schwa do not matter,
 * the code works, an unmatched query is empty) and renders
 * `LanguagePickerRows` off screen to check the field is drawn and the list below
 * it really narrows. A manual check, not part of `check` (pixels, no test
 * framework, PNGs under .ignore/).
 */
tasks.register<JavaExec>("languageSearchCheck") {
    group = "verification"
    description = "Checks the language picker's search filter and renders the field and the narrowed list"
    dependsOn(tasks.named("testClasses"))
    mainClass.set("com.music.vivi.desktop.LanguageSearchCheck")
    classpath = sourceSets.getByName("test").runtimeClasspath
}

/**
 * Headless check of the account screen's sync row:
 *
 *     ./gradlew :desktop:accountSyncRowCheck
 *
 * See `desktop/src/test/kotlin/com/music/vivi/desktop/AccountSyncRowCheck.kt`.
 * The row that runs the playlist sync answers "how far along is it?" with a
 * counter in its trailing slot (`3/12`, `SyncRunTrailing`) instead of a ring that
 * only says something is happening. This renders the row into an off-screen
 * `ImageComposeScene` and reads the right quarter of the pixels: no ink with no
 * run, ink right of the 80 % line during one, a different image when the run's
 * numbers change, and the ring standing in while the total is unknown. A manual
 * check, not part of `check` (pixels, no test framework, PNGs under .ignore/).
 */
/**
 * Headless check of the Romanization screen:
 *
 *     ./gradlew :desktop:romanizeRenderCheck
 *
 * See `desktop/src/test/kotlin/com/music/vivi/desktop/RomanizeRenderCheck.kt`.
 * Romanization has its own screen now (the master switch, the twelve script
 * switches, the as-main choice) instead of a card in the middle of *Lyrics*.
 * This renders it off screen in both states and reads the pixels: off is one row,
 * on is fourteen rows in two cards. A manual check, not part of `check` (pixels,
 * no test framework, PNGs under .ignore/).
 */
tasks.register<JavaExec>("romanizeRenderCheck") {
    group = "verification"
    description = "Renders the Romanization screen headlessly and checks its rows in both states"
    dependsOn(tasks.named("testClasses"))
    mainClass.set("com.music.vivi.desktop.RomanizeRenderCheck")
    classpath = sourceSets.getByName("test").runtimeClasspath
}

tasks.register<JavaExec>("accountSyncRowCheck") {
    group = "verification"
    description = "Renders the account sync row headlessly and checks the run's counter in its trailing slot"
    dependsOn(tasks.named("testClasses"))
    mainClass.set("com.music.vivi.desktop.AccountSyncRowCheck")
    classpath = sourceSets.getByName("test").runtimeClasspath
}

/**
 * Headless check of the Player & audio screen's cards:
 *
 *     ./gradlew :desktop:playerAudioCardCheck
 *
 * See `desktop/src/test/kotlin/com/music/vivi/desktop/PlayerAudioCardCheck.kt`.
 * The crossfade duration and the stream cache are thresholds of their own card's
 * setting, and the skip-silence sub-option belongs to its switch: this renders the
 * section in four states into an off-screen `ImageComposeScene` and reads the column
 * through its middle, where the 3 dp seams of a card and the 8 dp gaps between two
 * cards say which card took a row. Turning either switch on must add rows and no
 * card, and the seams must say the rows went into the switch's own card. A manual
 * check, not part of `check` (pixels, no test framework, PNGs under .ignore/).
 */
tasks.register<JavaExec>("playerAudioCardCheck") {
    group = "verification"
    description = "Renders Player & audio headlessly and checks the duration, the cache slider and the skip-silence row"
    dependsOn(tasks.named("testClasses"))
    mainClass.set("com.music.vivi.desktop.PlayerAudioCardCheck")
    classpath = sourceSets.getByName("test").runtimeClasspath
}

/**
 * Headless check of the rebuilt Equalizer screen:
 *
 *     ./gradlew :desktop:equalizerRenderCheck
 *
 * See `desktop/src/test/kotlin/com/music/vivi/desktop/EqualizerRenderCheck.kt`.
 * The screen is the phone's list (no equalization plus the imported AutoEQ
 * profiles) and, for the active one, the response curve, the preamp and one row
 * per band with the Q factors folded. This renders both states off screen and
 * reads the pixels: the empty state is one card of four rows and no `primary`
 * ink at all, the active one adds four cards including a curve drawn in
 * `primary` and a band card of ten rows. A manual check, not part of `check`
 * (pixels, no test framework, PNGs under .ignore/equalizer/).
 */
tasks.register<JavaExec>("equalizerRenderCheck") {
    group = "verification"
    description = "Renders the Equalizer screen headlessly and checks its list, curve and band rows"
    dependsOn(tasks.named("testClasses"))
    mainClass.set("com.music.vivi.desktop.EqualizerRenderCheck")
    classpath = sourceSets.getByName("test").runtimeClasspath
}

/**
 * Headless check of the Material 3 Expressive settings slider:
 *
 *     ./gradlew :desktop:sliderRenderCheck
 *
 * See `desktop/src/test/kotlin/com/music/vivi/desktop/SliderRenderCheck.kt`.
 * The shape of a slider is a pixel fact no compiler can see, so the composable is
 * rendered into an off-screen `ImageComposeScene` at density 1 and a row through
 * the track centre is read as runs: primary up to the cut, 6 px of page, the
 * handle, 6 px of page, `secondaryContainer` to the end. Pressing widens the
 * handle to 8 px and draws its state layer, releasing puts both back, and a
 * stepped slider reports only its ticks. A manual check, not part of `check`
 * (pixels, no test framework, PNGs under .ignore/slider/).
 */
tasks.register<JavaExec>("sliderRenderCheck") {
    group = "verification"
    description = "Renders the settings slider headlessly and checks its track, cut, handle and ticks"
    dependsOn(tasks.named("testClasses"))
    mainClass.set("com.music.vivi.desktop.SliderRenderCheck")
    classpath = sourceSets.getByName("test").runtimeClasspath
}

/**
 * Headless check of the heads the Storage and Commits screens carry:
 *
 *     ./gradlew :desktop:storageCommitsRenderCheck
 *
 * See `desktop/src/test/kotlin/com/music/vivi/desktop/StorageCommitsRenderCheck.kt`.
 * The hero is one card on the theme's own colours, and Storage's bar has to be
 * the proportion of its parts rather than four numbers in a row: the composables
 * are rendered into an off-screen `ImageComposeScene` and the colours are read
 * back as blobs (a 56 dp `secondaryContainer` tile, a 12 dp bar with the pills at
 * 6 : 3 : 1, three chips on Commits, two day rules and three rows under them). A
 * manual check, not part of `check` (pixels, no test framework, PNGs under
 * .ignore/hero/).
 */
tasks.register<JavaExec>("storageCommitsRenderCheck") {
    group = "verification"
    description = "Renders the Storage and Commits heads headlessly and checks the tile, the bar and the chips"
    dependsOn(tasks.named("testClasses"))
    mainClass.set("com.music.vivi.desktop.StorageCommitsRenderCheck")
    classpath = sourceSets.getByName("test").runtimeClasspath
}

/**
 * Headless check of the fold's own rule:
 *
 *     ./gradlew :desktop:foldAnimationCheck
 *
 * `M3SettingsFold` is the block every row that belongs to a switch is drawn
 * through, and it must move only while the Animations switch is on. The check
 * renders the fold alone over a 200 dp box and reads the box's painted height at
 * nine moments after the switch is flipped, in BOTH directions: closing (from the
 * full box to nothing) and opening (from nothing to the full box). With
 * animations on each direction must be drawn part-way at least twice (a tween,
 * not a jump) and must arrive at its end state; with them off every frame must be
 * either the whole box or nothing, in both directions. See
 * `desktop/src/test/kotlin/com/music/vivi/desktop/FoldAnimationCheck.kt`.
 */
tasks.register<JavaExec>("foldAnimationCheck") {
    group = "verification"
    description = "Checks that a settings fold animates only with the Animations switch on"
    dependsOn(tasks.named("testClasses"))
    mainClass.set("com.music.vivi.desktop.FoldAnimationCheck")
    classpath = sourceSets.getByName("test").runtimeClasspath
}

/**
 * Headless check of the Updates and Notifications screens:
 *
 *     ./gradlew :desktop:settingsScreensRenderCheck
 *
 * See `desktop/src/test/kotlin/com/music/vivi/desktop/SettingsScreensRenderCheck.kt`.
 * Both are a column of settings cards, and what a card holds is a pixel fact: a
 * row is joined to the next by a 3 dp seam while two cards stand 8 dp apart, so
 * the runs of card fill down a column say how many cards the screen is and how
 * many rows each one carries. Updates must read six bands with the two seams of
 * its two-row cards; Notifications the three rows of its one card with the two
 * seams that join them. Content, Language, Privacy and AI Lyrics Translation are
 * read the same way: the dropdowns stand alone, the five content switches are
 * one card, the language list is one card under its field, Privacy's two
 * switches are one, and the AI screen's credentials and mode rows are two.
 * Appearance's Canvas, App font and Display density and the About screens
 * (About, Contributors, Desktop features) are read the same way: the Canvas
 * source only exists with its switch, the fonts are one card with the import as
 * its last row, About is a head and three cards, Contributors is one card, and
 * Player & mini-player design is five cards whose sensitivity row leaves with
 * the swipe it tunes.
 *
 * What it cannot read is a dropdown's menu: the rows open a Material 3
 * `DropdownMenu`, which is a popup, and an off-screen `ImageComposeScene` paints
 * no popups (a click on a row leaves the frame without a menu in it). Where that
 * menu lands is `:desktop:dropdownPositionCheck`'s subject, on the ui-test
 * harness whose scene does hold the popup.
 * A manual check, not part of `check` (pixels, no test framework, PNGs under
 * .ignore/settings/).
 */
tasks.register<JavaExec>("settingsScreensRenderCheck") {
    group = "verification"
    description = "Renders the rebuilt settings screens headlessly and checks their cards, rows and seams"
    dependsOn(tasks.named("testClasses"))
    mainClass.set("com.music.vivi.desktop.SettingsScreensRenderCheck")
    classpath = sourceSets.getByName("test").runtimeClasspath
}

/**
 * Headless check of where a settings dropdown opens:
 *
 *     ./gradlew :desktop:dropdownPositionCheck
 *
 * See `desktop/src/test/kotlin/com/music/vivi/desktop/DropdownPositionCheck.kt`.
 * The rows open a Material 3 `DropdownMenu`, a popup, and an
 * `ImageComposeScene` paints no popups - which is why no render check could ever
 * say where the menu went. This one runs on the compose ui-test harness, whose
 * scene does hold the popup, so the menu's own bounds can be read against the
 * row's: under the row with its left edge on the row's, and above it when there
 * is no room below. A manual check, not part of `check`.
 */
tasks.register<JavaExec>("dropdownPositionCheck") {
    group = "verification"
    description = "Opens a settings dropdown and reads where its menu lands against the row"
    dependsOn(tasks.named("testClasses"))
    mainClass.set("com.music.vivi.desktop.DropdownPositionCheck")
    classpath = sourceSets.getByName("test").runtimeClasspath
}

/**
 * Real-window check of where every settings dropdown opens:
 *
 *     ./gradlew :desktop:settingsDropdownWindowCheck
 *
 * See `desktop/src/test/kotlin/com/music/vivi/desktop/SettingsDropdownWindowCheck.kt`.
 * `dropdownPositionCheck` reads the menu on the ui-test harness, whose scene is
 * off-screen. This one opens a real OS window on the machine's display (a
 * `ComposePanel` in a `JFrame`), presses each settings dropdown with the real
 * pointer, captures the screen and reads the menu back from the pixels: under
 * the row that was pressed, right-aligned to the value that opened it, and above
 * it when
 * the row sits at the floor of the window. Every dropdown the settings screens
 * draw is a case, in the shape its own screen gives it. Needs a display: with
 * none it prints `NOT RUN` and verifies nothing. A manual check, not part of
 * `check`.
 */
tasks.register<JavaExec>("settingsDropdownWindowCheck") {
    group = "verification"
    description = "Presses every settings dropdown in a real window and reads where its menu opens"
    dependsOn(tasks.named("testClasses"))
    mainClass.set("com.music.vivi.desktop.SettingsDropdownWindowCheck")
    classpath = sourceSets.getByName("test").runtimeClasspath
    // `-Pcase=<part of a name>` measures only the matching case(s), for
    // chasing one row without pressing the other twenty.
    (project.findProperty("case") as String?)?.let { args(it) }
}

/**
 * Manual: one cold start of the real app against a throwaway home
 * (`.ignore/startup-home`), stopped after 20 s, so the `startup:` lines in its
 * `app.log` can be read. Opens a window, needs a display.
 *
 * The CDS archive's effect is measured with this probe, the way the installers
 * get it:
 *
 *     ./gradlew :desktop:startupProbe                      # no archive
 *     ./gradlew :desktop:startupProbe -Pcds=<file.jsa>     # same run, archive on
 *
 * `-Pcds` adds the `-XX:SharedArchiveFile` the installers pass and nothing
 * else, so the two runs differ by the archive alone. `-PcdsList=<file>` records
 * the classes the run loads instead (`.ignore/cds-classlist.txt`), which is the
 * list the archive is dumped from. `-Phome=<dir>` and `-Pseconds=<n>` move the
 * throwaway home and the run length; a warm home is what makes two runs
 * comparable (a fresh one would show the first-run flow instead).
 *
 * The run opens a window: `-Djava.awt.headless=true` is not an option here,
 * because Compose asks the screen device for its density before the first stage
 * is written and dies with a `HeadlessException` on a machine with no display.
 */
tasks.register<JavaExec>("startupProbe") {
    group = "verification"
    description = "Launches the app against a throwaway home and stops it, to read its startup stages"
    dependsOn(tasks.named("testClasses"))
    mainClass.set("com.music.vivi.desktop.StartupProbeKt")
    classpath = sourceSets.getByName("test").runtimeClasspath
    workingDir = rootProject.projectDir
    args(
        (project.findProperty("home") as String?) ?: ".ignore/startup-home",
        (project.findProperty("seconds") as String?) ?: "20",
    )
    (project.findProperty("cds") as String?)?.let {
        jvmArgs("-XX:SharedArchiveFile=$it", "-Xshare:auto")
    }
    (project.findProperty("cdsList") as String?)?.let { jvmArgs("-XX:DumpLoadedClassList=$it") }
    jvmArgs("-XX:StartFlightRecording=filename=.ignore/startup.jfr,duration=14s,settings=profile,dumponexit=true")
}
