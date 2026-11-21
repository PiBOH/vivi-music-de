package com.music.vivi.desktop.presentation.screens.settings

import java.net.HttpURLConnection
import java.net.URI

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.toComposeImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.skia.Image as SkiaImage

import com.music.vivi.desktop.BuildConfig
import com.music.vivi.desktop.generated.resources.Res
import com.music.vivi.desktop.generated.resources.ic_github
import com.music.vivi.desktop.generated.resources.ic_rounded_group
import com.music.vivi.desktop.presentation.components.subcomps.AppTooltip
import com.music.vivi.desktop.presentation.navigation.DesktopBackHandler
import com.music.vivi.desktop.utils.BrowserUtils

/**
 * One person in `contributorsde.json`. The file is read from the repository on
 * every open, so people can be added, edited and reordered without an app
 * update; the copy bundled in the jar is only the offline fallback.
 */
@Serializable
data class ContributorEntry(
    val username: String = "",
    val description: String = "",
    val name: String? = null,
    val url: String? = null,
    val group: String? = null
)

@Serializable
private data class ContributorsFile(val contributors: List<ContributorEntry> = emptyList())

private const val CONTRIBUTORS_RAW_URL =
    "https://raw.githubusercontent.com/PiBOH/vivi-music-de/vivi-music-de/contributorsde.json"

/** The headings the screen shows, in this order, matching the JSON `group` values. */
private val ContributorGroupOrder = listOf(
    "developer" to "DEVELOPERS",
    "supporter" to "SUPPORTERS",
    "contributor" to "CONTRIBUTORS"
)

private val ContributorGroupKeys = ContributorGroupOrder.map { it.first }.toSet()

private val contributorsJson = Json { ignoreUnknownKeys = true }

private val contributorAvatarCache = mutableMapOf<String, ImageBitmap>()

/**
 * Reads the copy bundled in the jar, used while the repository cannot be
 * reached. Blanks and repeated handles are dropped so a malformed entry never
 * turns into an empty or duplicated card.
 */
private fun loadBundledContributors(): List<ContributorEntry> = runCatching {
    val stream = ContributorsFile::class.java.getResourceAsStream("/contributorsde.json")
        ?: return emptyList()
    stream.use { parseContributors(it.readBytes().decodeToString()) }
}.getOrDefault(emptyList())

/** The live list from the `vivi-music-de` branch; empty when offline. */
private fun fetchContributorsFromGitHub(): List<ContributorEntry> = runCatching {
    val connection = URI(CONTRIBUTORS_RAW_URL).toURL().openConnection() as HttpURLConnection
    try {
        connection.connectTimeout = 6000
        connection.readTimeout = 6000
        connection.setRequestProperty("User-Agent", "VIVIMusicDE")
        connection.setRequestProperty("Accept", "application/json")
        if (connection.responseCode == 200) {
            connection.inputStream.use { parseContributors(it.readBytes().decodeToString()) }
        } else {
            emptyList()
        }
    } finally {
        connection.disconnect()
    }
}.getOrDefault(emptyList())

/**
 * Decodes the file and keeps one card per person: entries without a handle are
 * skipped and the first card for a handle wins, so the same person listed twice
 * (for example in both the bundled and the live file) appears once.
 */
private fun parseContributors(text: String): List<ContributorEntry> =
    runCatching { contributorsJson.decodeFromString<ContributorsFile>(text).contributors }
        .getOrDefault(emptyList())
        .filter { it.username.isNotBlank() }
        .distinctBy { it.username.trim().lowercase() }

/**
 * Splits the list by the JSON `group` field, in the order the screen shows the
 * headings. A missing or unknown group is shown under CONTRIBUTORS so nobody
 * disappears from the screen, and the order inside each group stays the one in
 * the file.
 */
private fun groupContributors(
    contributors: List<ContributorEntry>
): List<Pair<String, List<ContributorEntry>>> {
    val byGroup = contributors.groupBy { entry ->
        entry.group?.trim()?.lowercase()?.takeIf { it in ContributorGroupKeys } ?: "contributor"
    }
    return ContributorGroupOrder
        .map { (key, heading) -> heading to byGroup[key].orEmpty() }
        .filter { it.second.isNotEmpty() }
}

/**
 * Cached GitHub avatar for a contributor profile URL. Shared with the
 * maintainer card in About so the same picture is downloaded once.
 */
internal suspend fun fetchContributorAvatar(url: String): ImageBitmap? {
    contributorAvatarCache[url]?.let { return it }
    return withContext(Dispatchers.IO) {
        runCatching {
            val connection = URI(url).toURL().openConnection() as HttpURLConnection
            connection.requestMethod = "GET"
            connection.connectTimeout = 4000
            connection.readTimeout = 4000
            connection.setRequestProperty("User-Agent", "VIVIMusicDE")
            val bytes = connection.inputStream.use { it.readBytes() }
            SkiaImage.makeFromEncoded(bytes).toComposeImageBitmap().also {
                contributorAvatarCache[url] = it
            }
        }.getOrNull()
    }
}

/**
 * Settings -> About -> Contributors: one card per person, read live from
 * `contributorsde.json` when the screen opens (the 1.55.0 fetch), with the
 * bundled copy as the offline fallback.
 */
@Composable
fun ContributorsDetailScreen(
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    var contributors by remember { mutableStateOf(loadBundledContributors()) }
    var loadedFromGitHub by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        val fresh = withContext(Dispatchers.IO) { fetchContributorsFromGitHub() }
        if (fresh.isNotEmpty()) {
            contributors = fresh
        }
        loadedFromGitHub = true
    }

    DesktopBackHandler(enabled = true) {
        onBack()
        true
    }

    val groups = remember(contributors) { groupContributors(contributors) }

    BoxWithConstraints(modifier = modifier.fillMaxSize()) {
        val horizontalPadding = if (maxWidth < 540.dp) 16.dp else 28.dp

        Column(modifier = Modifier.fillMaxSize()) {
            SettingsHeader(
                title = "Contributors",
                subtitle = "The people who build, test and support ${BuildConfig.APP_NAME}",
                horizontalPadding = horizontalPadding
            )

            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                contentPadding = PaddingValues(
                    start = horizontalPadding,
                    end = horizontalPadding,
                    top = 6.dp,
                    bottom = 24.dp
                ),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                if (contributors.isEmpty() && loadedFromGitHub) {
                    item {
                        Surface(
                            shape = RoundedCornerShape(24.dp),
                            color = MaterialTheme.colorScheme.surfaceContainer,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(20.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    painter = painterResource(Res.drawable.ic_rounded_group),
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.size(24.dp)
                                )
                                Spacer(modifier = Modifier.width(14.dp))
                                Text(
                                    text = "The contributor list is not available right now. Check your connection and open this screen again.",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }

                groups.forEach { (heading, people) ->
                    item(
                        key = "group-$heading",
                        contentType = { "group" }
                    ) {
                        SettingsSubsection(title = heading) {
                            Column(
                                modifier = Modifier.fillMaxWidth(),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                people.forEach { contributor ->
                                    ContributorEntryCard(
                                        contributor = contributor,
                                        onOpenUrl = BrowserUtils::openBrowser
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ContributorEntryCard(
    contributor: ContributorEntry,
    onOpenUrl: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = MaterialTheme.colorScheme
    val username = contributor.username.trim()
    val realName = contributor.name?.takeIf { it.isNotBlank() }
    val profileUrl = contributor.url?.takeIf { it.isNotBlank() } ?: "https://github.com/$username"

    Surface(
        shape = RoundedCornerShape(24.dp),
        color = colors.surfaceContainer,
        modifier = modifier
            .fillMaxWidth()
            .clickable { onOpenUrl(profileUrl) }
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 16.dp),
            verticalAlignment = Alignment.Top
        ) {
            ContributorEntryAvatar(
                username = username,
                fallbackName = realName ?: username,
                modifier = Modifier.size(56.dp)
            )

            Spacer(modifier = Modifier.width(16.dp))

            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(end = 8.dp),
                verticalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                Text(
                    text = realName ?: "@$username",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = colors.onSurface
                )
                // The handle is the title when there is no real name, so it is
                // only repeated under a name.
                if (realName != null) {
                    Text(
                        text = "@$username",
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.Medium,
                        color = colors.primary
                    )
                }
                if (contributor.description.isNotBlank()) {
                    Text(
                        text = contributor.description,
                        style = MaterialTheme.typography.bodyMedium,
                        color = colors.onSurfaceVariant,
                        lineHeight = 20.sp
                    )
                }
            }

            AppTooltip("Open GitHub profile") {
                IconButton(
                    onClick = { onOpenUrl(profileUrl) },
                    modifier = Modifier.size(36.dp)
                ) {
                    Icon(
                        painter = painterResource(Res.drawable.ic_github),
                        contentDescription = "GitHub profile",
                        tint = colors.primary,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }
    }
}

@Composable
internal fun ContributorEntryAvatar(
    username: String,
    fallbackName: String,
    modifier: Modifier = Modifier
) {
    val colors = MaterialTheme.colorScheme
    val avatarUrl = "https://github.com/$username.png?size=128"
    var avatarBitmap by remember(avatarUrl) { mutableStateOf<ImageBitmap?>(null) }

    LaunchedEffect(avatarUrl) {
        if (avatarBitmap == null) {
            avatarBitmap = fetchContributorAvatar(avatarUrl)
        }
    }

    Surface(
        modifier = modifier,
        shape = CircleShape,
        color = colors.surfaceContainerHigh,
        border = BorderStroke(2.dp, colors.surfaceContainer)
    ) {
        if (avatarBitmap != null) {
            Image(
                bitmap = avatarBitmap!!,
                contentDescription = fallbackName,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop
            )
        } else {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = fallbackName.removePrefix("@").firstOrNull()?.uppercase() ?: "?",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = colors.primary
                )
            }
        }
    }
}
