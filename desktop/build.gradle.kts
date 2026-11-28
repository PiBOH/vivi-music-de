@file:OptIn(org.jetbrains.compose.ExperimentalComposeLibrary::class)

import org.jetbrains.compose.desktop.application.dsl.TargetFormat
import java.net.URI

// version.txt is the single source of truth: mobile version/code/channel, then
// the DE version/code/channel. The human-readable release string is
// "<mobile>_DE-<de>-<channel>" (the channel is dropped for a stable release).
val versionLines: List<String> = rootProject.file("version.txt")
    .takeIf { it.exists() }
    ?.readLines()
    ?.map { it.trim() }
    ?.filter { it.isNotEmpty() && !it.startsWith("#") }
    ?: emptyList()

val mobileVersion: String = versionLines.getOrNull(0) ?: "0.0.0"
val mobileVersionCode: Int = versionLines.getOrNull(1)?.toIntOrNull() ?: 1
val deVersion: String = versionLines.getOrNull(3) ?: "0.0.0"
val deVersionCode: Int = versionLines.getOrNull(4)?.toIntOrNull() ?: 1
val releaseChannel: String = versionLines.getOrNull(5) ?: "stable"

val fullVersion: String = buildString {
    append(mobileVersion).append("_DE-").append(deVersion)
    if (!releaseChannel.equals("stable", ignoreCase = true)) append("-").append(releaseChannel)
}

// jpackage (and Inno Setup) need a purely numeric MAJOR.MINOR.PATCH on Windows/macOS.
val numericPackageVersion: String = deVersion.substringBefore('+').substringBefore('-')

// Debian: pre-releases use tilde notation (Debian Policy Manual 5.6.12).
val debianPackageVersion: String = "$deVersion~$releaseChannel"
val rpmRelease: String = if (releaseChannel.equals("stable", ignoreCase = true)) "1" else "0.$deVersionCode.$releaseChannel"

/**
 * The hardware renderer is chosen per host so the packaged build never falls back
 * to Skia's software surface: Direct3D on Windows, Metal on macOS, OpenGL on Linux.
 * `skiko.vsync.enabled=false` removes the presenter's vsync cap, which is what
 * pinned the window to the display refresh instead of letting it run past it.
 */
val skikoRenderApi: String = when {
    org.gradle.internal.os.OperatingSystem.current().isWindows -> "DIRECT3D"
    org.gradle.internal.os.OperatingSystem.current().isMacOsX -> "METAL"
    else -> "OPENGL"
}

plugins {
    kotlin("jvm")
    alias(libs.plugins.compose.compiler)
    alias(libs.plugins.compose.multiplatform)
    alias(libs.plugins.kotlin.serialization)
}

group = "com.music.vivi.desktop"
version = fullVersion

dependencies {
    implementation(compose.desktop.currentOs)
    implementation(compose.material3)
    implementation(compose.materialIconsExtended)
    implementation(compose.components.resources)

    // Pure-JVM VIVI backends: YouTube Music (innertube) and the lyrics providers.
    implementation(project(":innertube"))
    implementation(project(":lyricsProvider"))
    implementation(project(":spotify"))
    // The 1.55 device-sync protocol: shared WebSocket client and models.
    implementation(project(":sync"))

    // Coroutines
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.10.1")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-swing:1.10.1")

    // Serialization
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.8.0")
    implementation("org.yaml:snakeyaml:2.4")

    // HTTP client (OkHttp)
    implementation("com.squareup.okhttp3:okhttp:4.12.0")
    implementation("com.squareup.okhttp3:logging-interceptor:4.12.0")

    // DI (Koin)
    implementation("io.insert-koin:koin-core:4.0.2")
    implementation("io.insert-koin:koin-compose:4.0.2")

    // JSON store (the 1.55.0 device-sync.json model, no database engine)

    // Audio tag reader + the libmpv audio engine (JNA)
    implementation("net.jthink:jaudiotagger:3.0.1")
    implementation("org.gagravarr:vorbis-java-core:0.8")
    implementation("net.java.dev.jna:jna:5.16.0")
    implementation("net.java.dev.jna:jna-platform:5.16.0")
    implementation("io.methvin:directory-watcher:0.19.1")

    // Reorderable lazy lists
    implementation("sh.calvin.reorderable:reorderable:2.4.3")

    // Device sync: LAN relay (Ktor server), QR pairing (ZXing) and mDNS.
    implementation(libs.ktor.server.core)
    implementation(libs.ktor.server.netty)
    implementation(libs.ktor.server.websockets)
    implementation(libs.zxing.core)
    implementation(libs.jmdns)

    // MaterialKolor (M3 dynamic colours and quantizer)
    implementation("com.materialkolor:material-kolor:2.0.2")

    // Lyrics romanization
    implementation("com.atilika.kuromoji:kuromoji-ipadic:0.9.0")
    implementation("com.belerweb:pinyin4j:2.5.1")
    implementation("com.ibm.icu:icu4j:76.1")

    // FileKit (native file and directory picker)
    implementation("io.github.vinceglb:filekit-dialogs:0.16.0")

    // JavaFX WebView: the in-app YouTube Music sign-in window. The fallback is the
    // cookie jar of a browser already signed in, read by BrowserCookies.
    // The platform classifier is explicit: the unclassified JavaFX jars are empty
    // module descriptors, and Gradle does not apply the JavaFX variant attributes
    // to this plain JVM project.
    implementation("org.openjfx:javafx-base:21.0.5:win")
    implementation("org.openjfx:javafx-graphics:21.0.5:win")
    implementation("org.openjfx:javafx-controls:21.0.5:win")
    implementation("org.openjfx:javafx-media:21.0.5:win")
    implementation("org.openjfx:javafx-web:21.0.5:win")

    // Linux D-Bus and MPRIS
    implementation("com.github.hypfvieh:dbus-java-core:5.1.0")
    implementation("com.github.hypfvieh:dbus-java-transport-native-unixsocket:5.1.0")

    // Testing
    testImplementation(kotlin("test-junit5"))
    testImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:1.10.1")
    testImplementation(compose.uiTest)
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}

configurations.all {
    resolutionStrategy.dependencySubstitution {
        substitute(module("net.java.dev.jna:jna-jpms"))
            .using(module("net.java.dev.jna:jna:5.19.1"))
        substitute(module("net.java.dev.jna:jna-platform-jpms"))
            .using(module("net.java.dev.jna:jna-platform:5.19.1"))
    }
}

val generateBuildConfig = tasks.register("generateBuildConfig") {
    val outputDir = layout.buildDirectory.dir("generated/sources/buildConfig/main")
    val vName = fullVersion
    val vCode = deVersionCode

    inputs.property("versionName", vName)
    inputs.property("versionCode", vCode)
    outputs.dir(outputDir)

    doLast {
        val dir = outputDir.get().asFile
        val file = File(dir, "com/music/vivi/desktop/BuildConfig.kt")
        file.parentFile.mkdirs()
        file.writeText(
            """
            package com.music.vivi.desktop

            object BuildConfig {
                const val APP_NAME: String = "Axolotl Music"
                const val APP_VERSION_NAME: String = "$vName"
                const val APP_VERSION_CODE: Int = $vCode
                const val APPLICATION_ID: String = "com.music.vivi.desktop"
            }
            """.trimIndent() + "\n"
        )
    }
}

kotlin {
    jvmToolchain(21)
    sourceSets.getByName("main").kotlin.srcDir(generateBuildConfig)
}

tasks.test {
    useJUnitPlatform()
}

compose.desktop {
    application {
        mainClass = "MainKt"

        jvmArgs(
            "-Dfile.encoding=UTF-8",
            "-Dsun.jnu.encoding=UTF-8",
            "-Djna.encoding=UTF-8",
            // Hardware renderer. Skiko tries Direct3D first and falls back on its own
            // when the driver cannot create a device (the Intel HD 4000 and GeForce 610M
            // in this class of machine have no Direct3D 12, so the fallback is the normal
            // path there, not an error).
            "-Dskiko.renderApi=$skikoRenderApi",
            // Vsync is deliberately left at its default (on): it paces the frame clock
            // to the panel. Disabled, the presenter repaints as fast as the GPU allows,
            // and while the window is being resized that loop competes with the AWT
            // event pump: the measured result on this machine was one frame every 4.3 s
            // during a resize, against 60 fps with it on. A 120/144 Hz panel is still
            // served at its own rate - the cap is the display, not a frame limiter.
            "-Xms256m",
            // 512 MB was not enough for artwork, the catalogue and mpv together: the app
            // spent whole resize gestures collecting. 1.25 GB leaves the collector idle.
            "-Xmx1280m",
            "-XX:+UseG1GC",
            "-XX:G1HeapRegionSize=8m",
            "-XX:MinHeapFreeRatio=20",
            "-XX:MaxHeapFreeRatio=40",
            "-XX:G1PeriodicGCInterval=3000",
            "-XX:+UseStringDeduplication"
        )

        buildTypes.release.proguard {
            configurationFiles.from(project.file("proguard-rules.pro"))
        }

        nativeDistributions {
            modules(
                "java.naming",
                "java.desktop",
                // JavaFX Web's network layer loads java.net.http on the first page.
                // Without it the packaged WebView dies with NoClassDefFoundError
                // while the Gradle run works against the full JDK.
                "java.net.http",
                "jdk.unsupported",
                "jdk.security.auth",
                "jdk.crypto.ec",
                "jdk.net",
                "java.instrument"
            )
            // libmpv for Windows (libmpv-2.dll + the Vulkan loader it needs) lives in the
            // gitignored desktop/app-resources tree: 117 MB of binaries do not belong in the
            // repository, and Compose skips a root under a hidden directory (.prepareAppResources
            // reported NO-SOURCE for it). `setupMpvWindows` re-fetches libmpv there.
            appResourcesRootDir.set(project.layout.projectDirectory.dir("app-resources"))

            targetFormats(
                TargetFormat.Exe,
                TargetFormat.Msi,
                TargetFormat.Deb,
                TargetFormat.Rpm,
                TargetFormat.Dmg,
                TargetFormat.Pkg
            )

            packageName = "AxolotlMusic"
            packageVersion = numericPackageVersion
            description = "Axolotl Music - YouTube Music desktop client"
            vendor = "PiBOH"
            copyright = "Copyright (C) 2026 PiBOH"

            linux {
                packageName = "axolotlmusic"
                iconFile.set(project.file("src/main/resources/icons/icon.png"))
                debPackageVersion = debianPackageVersion
                rpmPackageVersion = deVersion
                appRelease = rpmRelease
                menuGroup = "AudioVideo;Player;Audio;"
                appCategory = "AudioVideo"
                rpmLicenseType = "GPL-3.0"
            }

            windows {
                iconFile.set(project.file("src/main/resources/icons/icon.ico"))
                packageVersion = numericPackageVersion
                exePackageVersion = numericPackageVersion
                menuGroup = "Axolotl Music"
                // Distinct from PixelPlayer's UUID so both products can coexist.
                upgradeUuid = "5f1c8f52-6d3a-4a60-9a1e-3f0d6b7c9e21"
            }

            macOS {
                iconFile.set(project.file("src/main/resources/icons/icon.icns"))
                packageVersion = numericPackageVersion
                dmgPackageVersion = numericPackageVersion
                bundleID = "com.music.vivi.desktop"
                packageName = "Axolotl Music"
                appCategory = "public.app-category.music"
                minimumSystemVersion = "12.0"
            }
        }
    }
}

/**
 * Downloads libmpv for Windows into the gitignored `app-resources` tree. The DLL is
 * 117 MB, so it ships with the checkout instead of the repository history.
 */
tasks.register("setupMpvWindows") {
    group = "mpv"
    description = "Downloads and bundles libmpv for Windows x64"
    val targetDir = layout.projectDirectory.dir("app-resources/windows/mpv").asFile
    val mpvUrl = "https://github.com/shinchiro/mpv-winbuild-cmake/releases/download/20261021/mpv-dev-x86_64-20261021-git-e471496c56.7z"
    val downloadDir = layout.buildDirectory.dir("downloads").get().asFile
    val archiveFile = downloadDir.resolve("mpv-dev-x86_64.7z")

    // The directory is an input of Compose's prepareAppResources, so declaring it here as an
    // output makes Gradle reject the pair as an undeclared dependency. The task stays untyped
    // and is ordered through the dependsOn below.
    onlyIf {
        !targetDir.resolve("libmpv-2.dll").exists()
    }

    doLast {
        downloadDir.mkdirs()
        targetDir.mkdirs()
        if (!archiveFile.exists()) {
            println("Downloading libmpv Windows x64 from $mpvUrl...")
            val tempFile = downloadDir.resolve("mpv-dev-x86_64.7z.part")
            tempFile.delete()
            URI(mpvUrl).toURL().openStream().use { input ->
                tempFile.outputStream().use { output -> input.copyTo(output) }
            }
            if (!tempFile.renameTo(archiveFile)) {
                tempFile.copyTo(archiveFile, overwrite = true)
                tempFile.delete()
            }
        }

        val candidates = listOf(
            "7z",
            "C:\\Program Files\\7-Zip\\7z.exe",
            "C:\\Program Files (x86)\\7-Zip\\7z.exe"
        )
        val sevenZip = candidates.firstOrNull { bin ->
            try {
                ProcessBuilder(bin).redirectErrorStream(true).start().waitFor()
                true
            } catch (_: Exception) {
                false
            }
        } ?: throw GradleException("7-Zip (7z) not found. Install 7-Zip, or restore the bundled libmpv-2.dll.")

        val process = ProcessBuilder(
            listOf(sevenZip, "e", "-y", archiveFile.absolutePath, "-o${targetDir.absolutePath}", "libmpv-2.dll")
        ).redirectErrorStream(true).start()
        val output = process.inputStream.bufferedReader().readText()
        if (process.waitFor() != 0) {
            throw GradleException("Failed to extract the libmpv archive with 7z:\n$output")
        }

        val loader = targetDir.resolve("vulkan-1.dll")
        if (!loader.isFile) {
            throw GradleException(
                "vulkan-1.dll is missing from ${loader.parentFile}. It ships with the repository " +
                    "(desktop/app-resources/windows/mpv/vulkan-1.dll); restore it before packaging."
            )
        }
    }
}

// Everything that reads app-resources needs the engine present. A clean checkout carries the
// 600 KB Vulkan loader in git and nothing else, so libmpv is fetched before the first use.
val mpvConsumers = setOf(
    "prepareAppResources", "createDistributable", "run", "runDistributable",
    "packageExe", "packageMsi", "packageDeb", "packageRpm", "packageDmg", "packagePkg"
)
tasks.matching { it.name in mpvConsumers }.configureEach { dependsOn("setupMpvWindows") }


// The Windows setup: the same Inno Setup script and pinned compiler the CI uses,
// driven from here so producing an installer is one command, not a shell session.
// The Windows setup, one command: the app image is packaged by jpackage, then the
// project's own Inno Setup script compiles it into build/installer. The compiler is
// invoked directly instead of through a shell wrapper, so its output is drained and
// logged and a build can never hang on a full pipe.
tasks.register("windowsInstaller") {
    group = "distribution"
    description = "Builds the Windows setup from the packaged app image (Inno Setup 7)."
    dependsOn("createDistributable")
    // Everything this task reads is resolved here, at configuration time. A doLast
    // closure that touches `rootProject` or `layout` cannot be stored in the
    // configuration cache, and the repository enables that cache.
    val isWindows = org.gradle.internal.os.OperatingSystem.current().isWindows
    val rootDir = rootProject.projectDir
    val installerOutputDir = File(rootDir, "build/installer")
    val appImageDir = layout.buildDirectory.dir("compose/binaries/main/app/AxolotlMusic").get().asFile
    val iconsDir = File(projectDir, "icons")
    val issScript = File(rootDir, "installer/windows/AxolotlMusic.iss")
    // The release numbers are script properties; read them into locals so the task
    // action captures strings instead of the build script itself.
    val appVersion = fullVersion
    val installerVersion = deVersion
    val fileVersion = numericPackageVersion
    onlyIf { isWindows }
    doLast {
        val roots = listOfNotNull(
            System.getenv("LOCALAPPDATA")?.let { File(it, "Programs") },
            System.getenv("ProgramFiles")?.let { File(it) },
            System.getenv("ProgramFiles(x86)")?.let { File(it) }
        )
        val iscc = roots.asSequence()
            .map { File(it, "Inno Setup 7/ISCC.exe") }
            .firstOrNull { it.isFile }
            ?: throw GradleException(
                "Inno Setup 7 was not found. Install it (per user under %LOCALAPPDATA%/Programs/Inno Setup 7) " +
                    "and run this task again."
            )

        val outputDir = installerOutputDir.apply { mkdirs() }
        val logDir = File(outputDir, "logs").apply { mkdirs() }
        val logFile = File(logDir, "iscc.log")
        val icons = iconsDir

        val command = listOf(
            iscc.absolutePath,
            "/DAppVersion=$appVersion",
            "/DInstallerVersion=$installerVersion",
            "/DVersionInfoVersion=$fileVersion",
            "/DSourceDir=${appImageDir.absolutePath}",
            "/DOutputDir=${outputDir.absolutePath}",
            "/DIconFile=${File(icons, "logo_axolotl.ico").absolutePath}",
            "/DWizardImageFile=${File(icons, "logo_axolotl_wizard.bmp").absolutePath}",
            "/DWizardSmallImageFile=${File(icons, "logo_axolotl_small.bmp").absolutePath}",
            "/DWizardImageFileDark=${File(icons, "logo_axolotl_wizard_dark.bmp").absolutePath}",
            "/DWizardSmallImageFileDark=${File(icons, "logo_axolotl_small_dark.bmp").absolutePath}",
            issScript.absolutePath
        )
        logger.lifecycle("Using Inno Setup compiler: ${iscc.absolutePath}")
        val process = ProcessBuilder(command).redirectErrorStream(true).start()
        val collected = StringBuilder()
        process.inputStream.bufferedReader().useLines { lines ->
            lines.forEach { line ->
                collected.append(line).append(System.lineSeparator())
                logger.lifecycle("iscc: $line")
            }
        }
        val exit = process.waitFor()
        logFile.writeText(collected.toString())
        if (exit != 0) {
            throw GradleException("Inno Setup compilation failed (exit $exit); log: $logFile")
        }
        val setup = File(outputDir, "AxolotlMusic-$appVersion-setup.exe")
        if (!setup.isFile) throw GradleException("ISCC exited 0 but $setup is missing")
        val engine = Regex("Compiler engine version: (.+)").find(collected)?.groupValues?.get(1)?.trim()
        if (engine == null || !engine.startsWith("Inno Setup 7")) {
            throw GradleException("Expected the Inno Setup 7 compiler, the log says: $engine")
        }
        logger.lifecycle(
            "Installer: ${setup.absolutePath} (${setup.length() / (1024 * 1024)} MB, $engine)"
        )
    }
}

compose.resources {
    packageOfResClass = "com.music.vivi.desktop.generated.resources"
    generateResClass = always
}

// Ship the release metadata and the changelog on the classpath so About can read them offline.
// contributorsde.json is the offline fallback of the Contributors screen.
// rootProject.file is required: project.file resolves inside desktop/, where these files do not exist.
tasks.processResources {
    from(rootProject.file("version.txt"))
    from(rootProject.file("CHANGELOG.md"))
    from(rootProject.file("LICENSE"))
    from(rootProject.file("contributorsde.json"))
}
