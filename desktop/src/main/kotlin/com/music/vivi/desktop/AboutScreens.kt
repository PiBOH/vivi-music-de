package com.music.vivi.desktop

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Fingerprint
import androidx.compose.material.icons.filled.Group
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Public
import androidx.compose.material.icons.filled.Send
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.toComposeImageBitmap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.PathParser
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import java.io.File
import java.net.URI
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/** GitHub mark (octocat) taken from the mobile app's drawable, so it tints
 *  with the accent color and adapts to dark/light mode like a normal icon. */
private val GithubIcon: ImageVector by lazy {
    ImageVector.Builder(
        name = "Github",
        defaultWidth = 24.dp,
        defaultHeight = 24.dp,
        viewportWidth = 24f,
        viewportHeight = 24f,
    ).apply {
        addPath(
            pathData = PathParser().parsePathString(GITHUB_MARK_PATH).toNodes(),
            fill = SolidColor(Color.White),
        )
    }.build()
}

private const val GITHUB_MARK_PATH =
    "M12,2A10,10 0,0 0,2 12c0,4.42 2.87,8.17 6.84,9.5c0.5,0.08 0.66,-0.23 0.66,-0.5c0,-0.23 0,-0.86 0,-1.69c-2.77,0.6 -3.36,-1.34 -3.36,-1.34c-0.46,-1.16 -1.11,-1.47 -1.11,-1.47c-0.91,-0.62 0.07,-0.6 0.07,-0.6c1,0.07 1.53,1.03 1.53,1.03c0.87,1.52 2.34,1.07 2.91,0.83c0.09,-0.65 0.35,-1.09 0.63,-1.34c-2.22,-0.25 -4.55,-1.11 -4.55,-4.92c0,-1.11 0.38,-2 1.03,-2.71c-0.1,-0.25 -0.45,-1.29 0.1,-2.64c0,0 0.84,-0.27 2.75,1.02c0.79,-0.22 1.65,-0.33 2.5,-0.33c0.85,0 1.71,0.11 2.5,0.33c1.91,-1.29 2.75,-1.02 2.75,-1.02c0.55,1.35 0.2,2.39 0.1,2.64c0.65,0.71 1.03,1.6 1.03,2.71c0,3.82 -2.34,4.66 -4.57,4.91c0.36,0.31 0.69,0.92 0.69,1.85c0,1.34 0,2.42 0,2.74c0,0.27 0.16,0.59 0.67,0.5C19.14,20.16 22,16.42 22,12A10,10 0,0 0,12 2Z"

/**
 * Settings -> About, as the preview draws it: the app's own mark and name on a
 * card, then what this build is (the version, the version code that unlocks the
 * developer options, the developer, the install date), then the three doors out
 * of the app, then the two documents.
 *
 * The three blocks are the three subjects of the screen, which is why they are
 * three cards instead of one column of loose rows.
 */
@Composable
fun AboutSection(
    language: String,
    onOpenContributors: () -> Unit,
    onOpenLicense: () -> Unit,
) {
    val firstLaunchDate = remember { DesktopSettings.load().firstLaunchDate }
    var versionCodeTaps by remember { mutableStateOf(0) }
    val devUnlocked by DeveloperOptions.unlocked.collectAsState()
    val authorImage = remember { loadResourceImage("author.png") }
    val logo = remember { loadResourceImage("logo_vmde.png") }

    Text(
        Localization.get(language, "about"),
        style = MaterialTheme.typography.titleLarge,
        modifier = Modifier.padding(top = 12.dp),
    )
    Spacer(Modifier.height(12.dp))

    // The head: the app's own logo (the intro's, not a letter standing in for it)
    // and what this build is, so the answer is on the screen before any row.
    Card(
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.10f),
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Row(
            Modifier.fillMaxWidth().padding(20.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(Modifier.size(56.dp), contentAlignment = Alignment.Center) {
                if (logo != null) {
                    Image(
                        bitmap = logo,
                        contentDescription = null,
                        contentScale = ContentScale.Fit,
                        modifier = Modifier.fillMaxSize(),
                    )
                } else {
                    Icon(
                        Icons.Filled.Info,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(32.dp),
                    )
                }
            }
            Spacer(Modifier.width(16.dp))
            Column {
                Text("VIVI Music DE", style = MaterialTheme.typography.titleLarge)
                Text(
                    "${AppInfo.FULL_VERSION} • ${AppInfo.CHANNEL.uppercase()}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }

    Spacer(Modifier.height(8.dp))

    M3SettingsGroup(
        items = listOf(
            M3SettingsItem(
                icon = Icons.Filled.Info,
                title = { Text(Localization.get(language, "current_version")) },
                trailing = {
                    Text(
                        AppInfo.FULL_VERSION,
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.primary,
                    )
                },
            ),
            M3SettingsItem(
                icon = Icons.Filled.Fingerprint,
                title = { Text(Localization.get(language, "version_code")) },
                description = {
                    // The hint only exists while the tapping does (or once it
                    // worked), so the gesture is never advertised to someone who
                    // never starts it.
                    val hint = when {
                        devUnlocked -> Localization.get(language, "developer_options_unlocked")
                        versionCodeTaps > 0 ->
                            Localization.get(language, "tap_version_code_hint") + " (${7 - versionCodeTaps})"
                        else -> AppInfo.VERSION_CODE.toString()
                    }
                    Text(hint)
                },
                trailing = { SettingsChevron() },
                onClick = {
                    if (!devUnlocked) {
                        versionCodeTaps++
                        if (versionCodeTaps >= 7) {
                            DeveloperOptions.setUnlocked(true)
                            versionCodeTaps = 0
                        }
                    }
                },
            ),
            M3SettingsItem(
                icon = null,
                leading = authorImage?.let { image ->
                    {
                        Image(
                            bitmap = image,
                            contentDescription = null,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.size(34.dp).clip(CircleShape),
                        )
                    }
                },
                title = { Text("PiBOH") },
                description = { Text(Localization.get(language, "app_developer") + " (DE)") },
                trailing = { SettingsChevron() },
                onClick = { openUrl("https://github.com/PiBOH") },
            ),
            M3SettingsItem(
                icon = Icons.Filled.DateRange,
                title = { Text(Localization.get(language, "installed_date_title")) },
                trailing = {
                    Text(
                        formatInstalledDate(firstLaunchDate, language),
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.primary,
                    )
                },
            ),
        ),
    )

    Spacer(Modifier.height(8.dp))

    // The three doors out: all three open a browser, which is why they are one
    // card and not one row each beside the documents.
    M3SettingsGroup(
        items = listOf(
            M3SettingsItem(
                icon = GithubIcon,
                title = { Text(Localization.get(language, "github_repository")) },
                trailing = { SettingsChevron() },
                onClick = { openUrl("https://github.com/PiBOH/vivi-music-de") },
            ),
            M3SettingsItem(
                icon = Icons.Filled.Send,
                title = { Text(Localization.get(language, "telegram_channel")) },
                trailing = { SettingsChevron() },
                onClick = { openUrl("https://t.me/vivimusicde") },
            ),
            M3SettingsItem(
                icon = Icons.Filled.Public,
                title = { Text(Localization.get(language, "website")) },
                trailing = { SettingsChevron() },
                onClick = { openUrl("https://piboh.github.io/vivi-music-de/") },
            ),
        ),
    )

    Spacer(Modifier.height(8.dp))

    M3SettingsGroup(
        items = listOf(
            M3SettingsItem(
                icon = Icons.Filled.Group,
                title = { Text(Localization.get(language, "contributors_section")) },
                trailing = { SettingsChevron() },
                onClick = onOpenContributors,
            ),
            M3SettingsItem(
                icon = Icons.Filled.Description,
                title = { Text(Localization.get(language, "license")) },
                trailing = { SettingsChevron() },
                onClick = onOpenLicense,
            ),
        ),
    )

    Spacer(Modifier.height(24.dp))
}

/**
 * Dedicated Contributors sub-screen (Settings -> About -> Contributors). The list
 * is always read live from the repository when the screen opens, so it stays
 * current without an app update; the bundled copy is only the offline fallback.
 */
@Composable
fun ContributorsSection(language: String) {
    // Refresh silently on every open; never falls back to a local user file: the
    // only sources are the repository (online) and the bundled copy (offline).
    var contributors by remember { mutableStateOf(loadContributors()) }
    LaunchedEffect(Unit) {
        val fresh = withContext(Dispatchers.IO) { fetchContributorsFromGitHub() }
        if (fresh.isNotEmpty()) {
            contributors = fresh
        }
    }
    Text(
        Localization.get(language, "contributors_section"),
        style = MaterialTheme.typography.titleLarge,
        modifier = Modifier.padding(top = 12.dp),
    )
    Spacer(Modifier.height(12.dp))
    if (contributors.isEmpty()) {
        Text(
            Localization.get(language, "library_empty"),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        return
    }
    // One card, a row per person: the avatar is the row's own mark, so the list
    // reads as people rather than as an index of them.
    M3SettingsGroup(
        items = contributors.map { contributor -> contributorRow(contributor) },
    )
    Spacer(Modifier.height(24.dp))
}

private fun contributorRow(contributor: ContributorEntry): M3SettingsItem {
    val username = contributor.username
    val realName = contributor.name?.takeIf { it.isNotBlank() }
    val displayTitle = if (realName != null) "$realName (@$username)" else "@$username"
    val profileUrl = contributor.url?.takeIf { it.isNotBlank() } ?: "https://github.com/$username"
    return M3SettingsItem(
        icon = null,
        leading = { ContributorAvatar(username = username, realName = realName, title = displayTitle) },
        title = { Text(displayTitle) },
        description = contributor.description
            .takeIf { it.isNotBlank() }
            ?.let { desc -> { Text(desc) } },
        trailing = { SettingsChevron() },
        onClick = { openUrl(profileUrl) },
    )
}

/**
 * A person's GitHub avatar with an initials disc underneath: the disc shows while
 * the image streams in and stays when there is no network, so the row never
 * loses its mark.
 */
@Composable
private fun ContributorAvatar(username: String, realName: String?, title: String) {
    Box(Modifier.size(34.dp), contentAlignment = Alignment.Center) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .clip(CircleShape)
                .background(contributorAvatarColor(username)),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                (realName ?: username).take(1).uppercase(),
                style = MaterialTheme.typography.titleSmall,
                color = Color.White,
            )
        }
        AsyncImage(
            model = "https://github.com/$username.png?size=96",
            contentDescription = title,
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize().clip(CircleShape),
        )
    }
}

/** Stable per-username avatar disc color (deterministic, theme-independent). */
private fun contributorAvatarColor(username: String): Color {
    val palette = listOf(
        Color(0xFF6750A4), Color(0xFF00696D), Color(0xFF006D3A), Color(0xFF7D5260),
        Color(0xFF9A6700), Color(0xFF00639B), Color(0xFF704214), Color(0xFFB3261E),
    )
    val hash = username.fold(0) { acc, c -> (acc * 31 + c.code) and 0x7fffffff }
    return palette[hash % palette.size]
}

// ---------------------------------------------------------------------------
// Licence (About -> Licence): read LIVE from the repository (`LICENSE` on the
// `vivi-music-de` branch) every time the screen opens, exactly like the
// contributor list, so an edit to the licence (the special exception for the
// musixmatch module is part of it) is visible without an app update. The
// bundled classpath copy is only the offline fallback.
// ---------------------------------------------------------------------------

private const val LICENSE_RAW_URL: String =
    "https://raw.githubusercontent.com/PiBOH/vivi-music-de/vivi-music-de/LICENSE"

private const val LICENSE_BLOB_URL: String =
    "https://github.com/PiBOH/vivi-music-de/blob/vivi-music-de/LICENSE"

/**
 * The licence bundled as a classpath resource (the shipped copy, from the
 * repository root `LICENSE`): what the screen shows when the repository cannot
 * be reached. Empty when the resource is missing, and the screen then says so.
 */
private fun loadLicense(): String = runCatching {
    AppInfo::class.java.getResourceAsStream("/LICENSE")?.use { stream ->
        stream.readBytes().decodeToString()
    }.orEmpty()
}.getOrDefault("")

/**
 * Fetches the licence currently in the repository (raw.githubusercontent.com,
 * branch `vivi-music-de`). Empty string when offline/unreachable, in which case
 * the caller keeps the bundled copy.
 */
private fun fetchLicenseFromGitHub(): String = runCatching {
    val conn = URI(LICENSE_RAW_URL).toURL().openConnection() as java.net.HttpURLConnection
    try {
        conn.connectTimeout = 8000
        conn.readTimeout = 8000
        conn.setRequestProperty("User-Agent", "VIVI-Music-DE/${AppInfo.FULL_VERSION}")
        if (conn.responseCode == 200) {
            conn.inputStream.use { input -> input.readBytes().decodeToString() }
        } else {
            ""
        }
    } finally {
        conn.disconnect()
    }
}.getOrDefault("")

/**
 * Dedicated Licence sub-screen (Settings -> About -> Licence). Live text on every
 * open, bundled copy offline, and the repository entry at the bottom for the
 * reader who wants the file in its own page with the blame and the history.
 */
@Composable
fun LicenseSection(language: String) {
    var text by remember { mutableStateOf(loadLicense()) }
    LaunchedEffect(Unit) {
        val fresh = withContext(Dispatchers.IO) { fetchLicenseFromGitHub() }
        if (fresh.isNotBlank()) text = fresh
    }
    Text(
        Localization.get(language, "license"),
        style = MaterialTheme.typography.titleLarge,
        modifier = Modifier.padding(top = 12.dp),
    )
    Spacer(Modifier.height(12.dp))
    if (text.isBlank()) {
        // No bundled copy and no network: say it instead of showing nothing.
        Text(
            Localization.get(language, "library_empty"),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    } else {
        SelectionContainer {
            Text(
                text,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
    Spacer(Modifier.height(16.dp))
    M3SettingsGroup(
        items = listOf(
            M3SettingsItem(
                icon = GithubIcon,
                title = { Text(Localization.get(language, "github_repository")) },
                trailing = { SettingsChevron() },
                onClick = { openUrl(LICENSE_BLOB_URL) },
            ),
        ),
    )
    Spacer(Modifier.height(24.dp))
}

// ---------------------------------------------------------------------------
// Contributors (About -> Contributors). The list is read LIVE from the
// repository (`contributorsde.json` on the `vivi-music-de` branch) every time
// the screen opens, so people can be added/edited in the repo without an app
// update; the bundled classpath copy is only the offline fallback. No local
// user copy is created or read anymore. Descriptions are intentionally neutral
// English (not localized). The GitHub avatar is fetched from
// https://github.com/<username>.png (GitHub's public avatar redirect).
// ---------------------------------------------------------------------------

@Serializable
private data class ContributorsFile(val contributors: List<ContributorEntry> = emptyList())

@Serializable
private data class ContributorEntry(
    val username: String = "",
    val description: String = "",
    val name: String? = null,
    val url: String? = null,
)

private val contributorsJson = sharedJson

/**
 * Reads the contributor list bundled as a classpath resource (the shipped
 * default). This is ONLY the offline fallback: the live list is fetched from
 * the repository every time the Contributors screen opens (see
 * [fetchContributorsFromGitHub]). A stale `~/.vivimusic/contributorsde.json`
 * from older builds (when the file was user-editable) is removed best-effort,
 * because the repository is now the single source of truth.
 */
private fun loadContributors(): List<ContributorEntry> {
    runCatching {
        val stale = File(System.getProperty("user.home"), ".vivimusic/contributorsde.json")
        if (stale.exists()) stale.delete()
    }
    return runCatching {
        val stream = AppInfo::class.java.getResourceAsStream("/contributorsde.json") ?: return emptyList()
        stream.use {
            contributorsJson.decodeFromString<ContributorsFile>(it.readBytes().decodeToString()).contributors
        }.filter { it.username.isNotBlank() }
    }.getOrDefault(emptyList())
}

/**
 * Fetches the live contributor list from the repository
 * (`contributorsde.json` on branch `vivi-music-de`, raw.githubusercontent.com).
 * Returns an empty list when offline/unreachable, so the About screen degrades
 * gracefully to the bundled copy.
 */
private fun fetchContributorsFromGitHub(): List<ContributorEntry> = runCatching {
    val url = URI("https://raw.githubusercontent.com/PiBOH/vivi-music-de/vivi-music-de/contributorsde.json").toURL()
    val conn = url.openConnection() as java.net.HttpURLConnection
    try {
        conn.connectTimeout = 8000
        conn.readTimeout = 8000
        conn.setRequestProperty("User-Agent", "VIVI-Music-DE/${AppInfo.FULL_VERSION}")
        conn.setRequestProperty("Accept", "application/json")
        if (conn.responseCode == 200) {
            conn.inputStream.use { input ->
                val text = input.readBytes().decodeToString()
                contributorsJson.decodeFromString<ContributorsFile>(text).contributors
                    .filter { it.username.isNotBlank() }
            }
        } else {
            emptyList()
        }
    } finally {
        conn.disconnect()
    }
}.getOrDefault(emptyList())

/** Loads a bundled classpath image from `desktop/src/main/resources/images`. */
private fun loadResourceImage(name: String): ImageBitmap? = runCatching {
    val stream = AppInfo::class.java.getResourceAsStream("/images/$name") ?: return null
    stream.use { s -> javax.imageio.ImageIO.read(s)?.toComposeImageBitmap() }
}.getOrNull()

private fun formatInstalledDate(epochMs: Long, language: String): String {
    if (epochMs <= 0) return Localization.get(language, "unknown")
    return try {
        SimpleDateFormat("MMM dd, yyyy", Locale.getDefault()).format(Date(epochMs))
    } catch (_: Exception) {
        "-"
    }
}
