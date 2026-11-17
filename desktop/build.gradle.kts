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

    // Database (SQLite JDBC)
    implementation("org.xerial:sqlite-jdbc:3.47.2.0")

    // Audio tag reader + the libmpv audio engine (JNA)
    implementation("net.jthink:jaudiotagger:3.0.1")
    implementation("org.gagravarr:vorbis-java-core:0.8")
    implementation("net.java.dev.jna:jna:5.16.0")
    implementation("net.java.dev.jna:jna-platform:5.16.0")
    implementation("io.methvin:directory-watcher:0.19.1")

    // Reorderable lazy lists
    implementation("sh.calvin.reorderable:reorderable:2.4.3")

    // MaterialKolor (M3 dynamic colours and quantizer)
    implementation("com.materialkolor:material-kolor:2.0.2")

    // Lyrics romanization
    implementation("com.atilika.kuromoji:kuromoji-ipadic:0.9.0")
    implementation("com.belerweb:pinyin4j:2.5.1")
    implementation("com.ibm.icu:icu4j:76.1")

    // FileKit (native file and directory picker)
    implementation("io.github.vinceglb:filekit-dialogs:0.16.0")

    // Linux D-Bus and MPRIS
    implementation("com.github.hypfvieh:dbus-java-core:5.1.0")
    implementation("com.github.hypfvieh:dbus-java-transport-native-unixsocket:5.1.0")

    // Testing
    testImplementation(kotlin("test"))
    testImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:1.10.1")
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
                const val APP_NAME: String = "VIVI Music DE"
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

// Ship the release metadata and the changelog on the classpath so About can read them offline.
tasks.processResources {
    from(project.file("version.txt"))
    from(project.file("CHANGELOG.md"))
    from(project.file("LICENSE"))
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
            // Hardware renderer, forced: no silent software fallback on the packaged build.
            "-Dskiko.renderApi=$skikoRenderApi",
            // No presenter vsync cap: the frame clock follows the work, not the display.
            "-Dskiko.vsync.enabled=false",
            "-Xms64m",
            "-Xmx512m",
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
                "java.sql",
                "java.naming",
                "java.desktop",
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

            packageName = "VIVIMusic"
            packageVersion = numericPackageVersion
            description = "VIVI Music DE - YouTube Music desktop client"
            vendor = "PiBOH"
            copyright = "Copyright (C) 2026 PiBOH"

            linux {
                packageName = "vivimusicde"
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
                menuGroup = "VIVI Music DE"
                // Distinct from PixelPlayer's UUID so both products can coexist.
                upgradeUuid = "5f1c8f52-6d3a-4a60-9a1e-3f0d6b7c9e21"
            }

            macOS {
                iconFile.set(project.file("src/main/resources/icons/icon.icns"))
                packageVersion = numericPackageVersion
                dmgPackageVersion = numericPackageVersion
                bundleID = "com.music.vivi.desktop"
                packageName = "VIVI Music DE"
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

compose.resources {
    packageOfResClass = "com.music.vivi.desktop.generated.resources"
    generateResClass = always
}
