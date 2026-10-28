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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Fingerprint
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material.icons.filled.Gavel
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Public
import androidx.compose.material.icons.filled.Sell
import androidx.compose.material.icons.filled.OpenInNew
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
            fill = SolidColor(PaletteInk),
        )
    }.build()
}

private const val GITHUB_MARK_PATH =
    "M12,2A10,10 0,0 0,2 12c0,4.42 2.87,8.17 6.84,9.5c0.5,0.08 0.66,-0.23 0.66,-0.5c0,-0.23 0,-0.86 0,-1.69c-2.77,0.6 -3.36,-1.34 -3.36,-1.34c-0.46,-1.16 -1.11,-1.47 -1.11,-1.47c-0.91,-0.62 0.07,-0.6 0.07,-0.6c1,0.07 1.53,1.03 1.53,1.03c0.87,1.52 2.34,1.07 2.91,0.83c0.09,-0.65 0.35,-1.09 0.63,-1.34c-2.22,-0.25 -4.55,-1.11 -4.55,-4.92c0,-1.11 0.38,-2 1.03,-2.71c-0.1,-0.25 -0.45,-1.29 0.1,-2.64c0,0 0.84,-0.27 2.75,1.02c0.79,-0.22 1.65,-0.33 2.5,-0.33c0.85,0 1.71,0.11 2.5,0.33c1.91,-1.29 2.75,-1.02 2.75,-1.02c0.55,1.35 0.2,2.39 0.1,2.64c0.65,0.71 1.03,1.6 1.03,2.71c0,3.82 -2.34,4.66 -4.57,4.91c0.36,0.31 0.69,0.92 0.69,1.85c0,1.34 0,2.42 0,2.74c0,0.27 0.16,0.59 0.67,0.5C19.14,20.16 22,16.42 22,12A10,10 0,0 0,12 2Z"

/**
 * Settings -> About, as the preview draws it: the app's own mark, name and
 * tagline on a card, then what this build is (the version, the version code that
 * unlocks the developer options and the author of this edition), then the three
 * doors out of the app under *Community*, then the two documents.
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
    var versionCodeTaps by remember { mutableStateOf(0) }
    val devUnlocked by DeveloperOptions.unlocked.collectAsState()
    val authorImage = remember { loadResourceImage("author.png") }
    val logo = remember { loadResourceImage("logo_vmde.png") }

    Text(
        Localization.get(language, "about"),
        style = MaterialTheme.typography.titleLarge,
        modifier = Modifier.padding(top = 16.dp),
    )
    Spacer(Modifier.height(16.dp))

    // The head: the app's own logo (the intro's, not a letter standing in for it)
    // and what this build is, so the answer is on the screen before any row.
    Card(
        shape = MaterialTheme.shapes.extraLarge,
        // The fill every settings card wears (the page's `md-card`, a 30 % mix of
        // `surface-variant`), not the accent tint a hero would take: the head is a
        // card of this screen, and a tinted one read as a different kind of block.
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.30f),
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
                    Localization.get(language, "app_tagline"),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }

    Spacer(Modifier.height(16.dp))

    // What this build is: the version and the code that unlocks the developer
    // options. Both figures are the row's own description (the page says them
    // there, not in a chip).
    M3SettingsGroup(
        items = listOf(
            M3SettingsItem(
                icon = Icons.Filled.Sell,
                title = { Text(Localization.get(language, "current_version")) },
                description = { Text(AppInfo.FULL_VERSION) },
            ),
            M3SettingsItem(
                icon = Icons.Filled.Fingerprint,
                title = { Text(Localization.get(language, "version_code")) },
                description = {
                    Column {
                        Text(AppInfo.VERSION_CODE.toString())
                        // The hint only exists while the tapping does (or once it
                        // worked), so the gesture is never advertised to someone
                        // who never starts it.
                        val hint = when {
                            devUnlocked -> Localization.get(language, "developer_options_unlocked")
                            versionCodeTaps > 0 ->
                                Localization.get(language, "tap_version_code_hint") + " (${7 - versionCodeTaps})"
                            else -> null
                        }
                        if (hint != null) {
                            Text(
                                hint,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.primary,
                            )
                        }
                    }
                },
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
            // The person behind this edition: kept on purpose, one row more than
            // the page draws, because a fork of this size has one author and the
            // credit belongs on the screen that names the build.
            M3SettingsItem(
                icon = null,
                leading = authorImage?.let { image ->
                    {
                        // The author's cookie is inside the row, so it answers the
                        // pointer where the row does: hovering anywhere on it both
                        // grows the face and rounds its corners, not only the 34 dp
                        // the picture covers.
                        val rowHovered = LocalRowHovered.current
                        val grow = Motion.hoverProgress(rowHovered, label = "authorCookieHover")
                        val cookie = rememberPersonCookie(rowHovered, label = "authorCookie")
                        Image(
                            bitmap = image,
                            contentDescription = null,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier
                                .size(34.dp)
                                .hoverGrow(grow, grow = 0.08f)
                                .clip(cookie),
                        )
                    }
                },
                title = { Text("PiBOH") },
                description = { Text(Localization.get(language, "app_developer") + " (DE)") },
                trailing = { LeavesTheAppMark() },
                onClick = { openUrl("https://github.com/PiBOH") },
            ),
        ),
    )

    Spacer(Modifier.height(16.dp))

    // The three doors out of the app, under the page's own label for them: all
    // three open a browser, which the arrow says (a chevron would promise
    // another screen of the app).
    M3SettingsGroup(
        title = Localization.get(language, "community"),
        items = listOf(
            aboutLinkRow(Icons.Filled.Code, "github_repository", language, "https://github.com/PiBOH/vivi-music-de"),
            aboutLinkRow(Icons.Filled.Send, "telegram_channel", language, "https://t.me/vivimusicde"),
            aboutLinkRow(Icons.Filled.Public, "website", language, "https://piboh.github.io/vivi-music-de/"),
        ),
    )

    Spacer(Modifier.height(16.dp))

    // The two documents.
    M3SettingsGroup(
        items = listOf(
            M3SettingsItem(
                icon = Icons.Filled.Groups,
                title = { Text(Localization.get(language, "contributors_section")) },
                trailing = { SettingsChevron() },
                onClick = onOpenContributors,
            ),
            M3SettingsItem(
                icon = Icons.Filled.Gavel,
                title = { Text(Localization.get(language, "license")) },
                trailing = { SettingsChevron() },
                onClick = onOpenLicense,
            ),
        ),
    )

    Spacer(Modifier.height(24.dp))
}

/** The mark of a row that leaves the app for a browser page (`open_in_new`). */
@Composable
private fun LeavesTheAppMark() {
    Icon(
        Icons.Filled.OpenInNew,
        contentDescription = null,
        tint = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.size(18.dp),
    )
}

/** A row of the About screen's links: one label, one address, the page's mark. */
private fun aboutLinkRow(icon: ImageVector, titleKey: String, language: String, url: String): M3SettingsItem =
    M3SettingsItem(
        icon = icon,
        title = { Text(Localization.get(language, titleKey)) },
        trailing = { LeavesTheAppMark() },
        onClick = { openUrl(url) },
    )

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
        modifier = Modifier.padding(top = 16.dp),
    )
    Spacer(Modifier.height(16.dp))
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
    // Hovering the person's row moves the cookie by the same movement everywhere
    // on the row: the shape is what says "a person", so it is what answers, and
    // it rounds its corners as it grows instead of only getting bigger.
    val rowHovered = LocalRowHovered.current
    val grow = Motion.hoverProgress(rowHovered, label = "contributorCookieHover")
    val cookie = rememberPersonCookie(rowHovered, label = "contributorCookie")
    Box(Modifier.size(34.dp).hoverGrow(grow, grow = 0.08f), contentAlignment = Alignment.Center) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .clip(cookie)
                .background(contributorAvatarColor(username)),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                (realName ?: username).take(1).uppercase(),
                style = MaterialTheme.typography.titleSmall,
                color = PaletteInk,
            )
        }
        AsyncImage(
            model = "https://github.com/$username.png?size=96",
            contentDescription = title,
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize().clip(cookie),
        )
    }
}

/** Stable per-username avatar disc color (deterministic, theme-independent). */
private fun contributorAvatarColor(username: String): Color {
    val hash = username.fold(0) { acc, c -> (acc * 31 + c.code) and 0x7fffffff }
    return AvatarDiscs[hash % AvatarDiscs.size]
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
        modifier = Modifier.padding(top = 16.dp),
    )
    Spacer(Modifier.height(16.dp))
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

