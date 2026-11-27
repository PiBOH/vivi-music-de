package com.music.vivi.desktop.dev

import java.io.File
import java.io.RandomAccessFile
import java.nio.file.Files

import kotlin.system.exitProcess
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.Json
import okhttp3.OkHttpClient

import com.music.vivi.desktop.data.network.github.GitHubReleaseClient
import com.music.vivi.desktop.data.network.github.ReleaseChecksums
import com.music.vivi.desktop.data.network.github.UpdateAssets

/**
 * Proves the update verification against the release GitHub is serving now.
 *
 * It does not reuse a fixture: it reads the newest published release, selects
 * the installer `UpdateAssets.forCurrentPlatform` picks for this system (the
 * Windows setup here), downloads the real `SHA256SUMS`, resolves that
 * installer's line, downloads the installer and compares the hash.
 * It then proves a corrupted published hash and a corrupted file are refused,
 * and that a name missing from the file resolves to nothing.
 *
 * Usage: `VIVIMusic.exe --verify-update-checksum`
 */
object UpdateChecksumCheck {

    const val FLAG = "--verify-update-checksum"

    fun handle(args: Array<String>): Boolean {
        if (!args.contains(FLAG)) return false
        exitProcess(runBlocking { run() })
    }

    private suspend fun run(): Int {
        val client = GitHubReleaseClient(
            okHttpClient = OkHttpClient(),
            json = Json { ignoreUnknownKeys = true }
        )

        println("[update-checksum] repository: PiBOH/axolotl-music")
        val release = client.fetchLatestRelease() ?: return fail("could not read the release list")
        println("[update-checksum] newest release: ${release.versionTag} (${release.assets.size} assets)")

        val checksumAsset = release.assets.firstOrNull {
            it.name.equals(ReleaseChecksums.ASSET_NAME, ignoreCase = true)
        } ?: return fail("the release has no ${ReleaseChecksums.ASSET_NAME}")
        val checksumText = client.fetchText(checksumAsset.downloadUrl)
            .getOrElse { return fail("SHA256SUMS download failed: ${it.message}") }
        val entries = ReleaseChecksums.entries(checksumText)
        println("[update-checksum] SHA256SUMS: ${checksumText.length} bytes, ${entries.size} entries")
        if (entries.isEmpty()) return fail("SHA256SUMS has no parsable entry")

        val platform = System.getProperty("os.name").orEmpty()
        val asset = UpdateAssets.forCurrentPlatform(release)
            ?: return fail("the release has no installer for $platform")
        println("[update-checksum] platform: $platform")
        println("[update-checksum] installer: ${asset.name} (${asset.size} bytes)")
        if (entries.none { it.name == asset.name }) {
            return fail("${asset.name} is missing from SHA256SUMS")
        }

        val expected = ReleaseChecksums.resolve(release, asset, client)
            ?: return fail("the release publishes no checksum for ${asset.name}")
        println("[update-checksum] expected: ${expected.sha256} (source ${expected.source})")
        if (expected.source != ReleaseChecksums.Source.CHECKSUM_FILE) {
            return fail("the checksum did not come from SHA256SUMS")
        }

        val directory = Files.createTempDirectory("vivi-update-checksum").toFile()
        try {
            val target = File(directory, asset.name)
            val file = client.download(asset, target) { _, _ -> }
                .getOrElse { return fail("download failed: ${it.message}") }
            val actual = client.sha256(file)
            println("[update-checksum] actual:   $actual")
            if (!ReleaseChecksums.matches(expected.sha256, actual)) {
                return fail("the published hash does not match the downloaded file")
            }
            println("[update-checksum] positive: MATCH")

            val corruptedHash = expected.sha256.let { hash ->
                (if (hash[0] == '0') '1' else '0') + hash.substring(1)
            }
            if (ReleaseChecksums.matches(corruptedHash, actual)) {
                return fail("a corrupted published hash was accepted")
            }
            println("[update-checksum] negative (corrupted hash): REJECTED")

            if (file.length() == 0L) return fail("the downloaded asset is empty")
            // Flip the first byte of the downloaded installer in place: the
            // same bytes the updater has just hashed, now one bit different.
            RandomAccessFile(file, "rw").use { handle ->
                val first = handle.read()
                handle.seek(0)
                handle.write(first xor 0xFF)
            }
            val corruptedActual = client.sha256(file)
            if (ReleaseChecksums.matches(expected.sha256, corruptedActual)) {
                return fail("a corrupted file was accepted")
            }
            println("[update-checksum] negative (corrupted bytes): REJECTED")

            val missing = ReleaseChecksums.expectedFor("asset-not-in-the-file", checksumText)
            if (missing != null) return fail("a name missing from SHA256SUMS resolved to a hash")
            println("[update-checksum] missing asset: null")
        } finally {
            directory.deleteRecursively()
        }

        println("[update-checksum] RESULT: PASS")
        return 0
    }

    private fun fail(message: String): Int {
        println("[update-checksum] $message")
        println("[update-checksum] RESULT: FAIL")
        return 1
    }
}
