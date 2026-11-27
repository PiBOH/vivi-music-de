package com.music.vivi.desktop.data.network.github

import com.music.vivi.desktop.domain.model.AppReleaseInfo
import com.music.vivi.desktop.domain.model.ReleaseAsset

/**
 * The one checksum file a release publishes.
 *
 * `SHA256SUMS` is the `sha256sum` format: `<hash>  <file>`, two spaces (a tab
 * or the binary marker `*` are accepted too). A file name that contains a
 * backslash or a newline is written on a line that starts with `\`, with those
 * characters escaped as `\\` and `\n`; [entries] unescapes them so the name
 * still compares with the asset name GitHub reports.
 *
 * The updater reads this file first and uses the asset's GitHub digest only
 * when the release has no checksum file, or the file has no line for that
 * asset. The old per-file `.sha256` fallback is deliberately gone.
 */
internal object ReleaseChecksums {

    const val ASSET_NAME = "SHA256SUMS"

    data class Entry(val sha256: String, val name: String)

    data class Expected(val sha256: String, val source: Source)

    enum class Source { CHECKSUM_FILE, ASSET_DIGEST }

    private val HASH = Regex("^[0-9a-fA-F]{64}$")

    fun entries(text: String): List<Entry> = buildList {
        for (raw in text.lineSequence()) {
            val line = raw.trimEnd('\r')
            if (line.isBlank() || line.startsWith("#")) continue
            val escaped = line.startsWith("\\")
            val body = if (escaped) line.substring(1) else line
            val separator = body.indexOfFirst { it == ' ' || it == '\t' }
            if (separator <= 0) continue
            val hash = body.substring(0, separator)
            if (!HASH.matches(hash)) continue
            var name = body.substring(separator).trimStart(' ', '\t')
            if (name.startsWith("*")) name = name.substring(1)
            if (escaped) name = unescape(name)
            if (name.isNotEmpty()) add(Entry(hash.lowercase(), name))
        }
    }

    fun expectedFor(assetName: String, text: String): String? =
        entries(text).firstOrNull { it.name == assetName }?.sha256

    /**
     * Resolves the hash the updater must compare [asset] with. The published
     * checksum file wins; the GitHub asset digest is only the safety net for a
     * release that has no file, or a file that misses this asset.
     */
    suspend fun resolve(
        release: AppReleaseInfo,
        asset: ReleaseAsset,
        client: GitHubReleaseClient
    ): Expected? {
        val checksumAsset = release.assets.firstOrNull {
            it.name.equals(ASSET_NAME, ignoreCase = true)
        }
        val fromFile = checksumAsset
            ?.let { client.fetchText(it.downloadUrl).getOrNull() }
            ?.let { expectedFor(asset.name, it) }
        if (fromFile != null) return Expected(fromFile, Source.CHECKSUM_FILE)

        val digest = asset.sha256?.takeIf { it.length == 64 } ?: return null
        return Expected(digest.lowercase(), Source.ASSET_DIGEST)
    }

    fun matches(expected: String?, actual: String?): Boolean =
        expected != null &&
            actual != null &&
            expected.length == 64 &&
            actual.length == 64 &&
            expected.equals(actual, ignoreCase = true)

    private fun unescape(name: String): String {
        val out = StringBuilder(name.length)
        var index = 0
        while (index < name.length) {
            val character = name[index]
            if (character == '\\' && index + 1 < name.length) {
                when (name[index + 1]) {
                    'n' -> {
                        out.append('\n')
                        index += 2
                    }
                    '\\' -> {
                        out.append('\\')
                        index += 2
                    }
                    else -> {
                        out.append(character)
                        index += 1
                    }
                }
            } else {
                out.append(character)
                index += 1
            }
        }
        return out.toString()
    }
}
