package com.music.vivi.desktop

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material.icons.filled.CallSplit
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Verified
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import okhttp3.OkHttpClient
import okhttp3.Request
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import java.util.Locale
import java.util.concurrent.TimeUnit

/** A single commit, reduced to what the list needs to render. */
data class CommitData(
    val sha: String,
    val message: String,
    val authorName: String,
    val authorAvatarUrl: String?,
    val date: String,
    val htmlUrl: String,
    /** The time it landed, which the row says because the day heads the group. */
    val time: String = "",
)

/** Minimal subset of the GitHub commits API response. */
@Serializable
data class GitHubCommit(
    val sha: String = "",
    @SerialName("html_url") val htmlUrl: String = "",
    val commit: GitHubCommitDetail? = null,
    val author: GitHubCommitUser? = null,
)

@Serializable
data class GitHubCommitDetail(
    val message: String = "",
    val author: GitHubCommitAuthor? = null,
)

@Serializable
data class GitHubCommitAuthor(
    val name: String = "",
    val date: String = "",
)

@Serializable
data class GitHubCommitUser(
    val login: String? = null,
    @SerialName("avatar_url") val avatarUrl: String? = null,
)

/**
 * Fetches the most recent commits of the configured update source (fork or
 * original) from the GitHub API, mirroring the mobile Commit screen but using
 * `UpdateSource.repo()/branch()` so the DE shows its own branch history.
 */
object CommitFetcher {
    private val json = sharedJson
    private val client = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(15, TimeUnit.SECONDS)
        .build()

    fun fetch(): List<CommitData> = try {
        val request = Request.Builder()
            .url("https://api.github.com/repos/${UpdateSource.repo()}/commits?branch=${UpdateSource.branch()}&per_page=50")
            .header("Accept", "application/vnd.github+json")
            .header("User-Agent", "VIVIMusic-Desktop-Updater")
            .build()
        client.newCall(request).execute().use { response ->
            if (!response.isSuccessful) return emptyList()
            val commits = json.decodeFromString<List<GitHubCommit>>(response.body.string())
            commits.map { c ->
                val detail = c.commit
                val fullMessage = detail?.message.orEmpty()
                val message = fullMessage.lines().firstOrNull { it.isNotBlank() } ?: fullMessage
                val authorName = detail?.author?.name?.takeIf { it.isNotBlank() }
                    ?: c.author?.login?.takeIf { it.isNotBlank() }
                    ?: "Unknown"
                val (day, time) = formatStamp(detail?.author?.date.orEmpty())
                CommitData(
                    sha = c.sha,
                    message = message,
                    authorName = authorName,
                    authorAvatarUrl = c.author?.avatarUrl,
                    date = day,
                    htmlUrl = c.htmlUrl,
                    time = time,
                )
            }
        }
    } catch (_: Exception) {
        emptyList()
    }

    /** `2026-10-16T17:23:04Z` as the day the list groups by and the time the row shows. */
    private fun formatStamp(raw: String): Pair<String, String> = try {
        val zoned = ZonedDateTime.parse(raw)
        zoned.format(DateTimeFormatter.ofPattern("MMM d, yyyy", Locale.getDefault())) to
            zoned.format(DateTimeFormatter.ofPattern("HH:mm", Locale.getDefault()))
    } catch (_: Exception) {
        // An unparsable stamp still groups by whatever it says, with no time.
        raw to ""
    }
}

/**
 * Settings → Updates → Commits: lists the most recent repository commits
 * (port of the mobile `settings/commits` screen). Clicking a commit opens it
 * in the browser.
 */
@Composable
fun CommitsScreen(language: String, onBack: () -> Unit) {
    var commits by remember { mutableStateOf<List<CommitData>?>(null) }

    LaunchedEffect(Unit) {
        commits = withContext(Dispatchers.IO) { CommitFetcher.fetch() }
    }

    Column(Modifier.fillMaxSize().padding(16.dp)) {
        BackButton(language, onBack)
        CommitsContent(language, commits)
    }
}

/** `1 commit` or `12 commits`, the chip's own sentence. */
internal fun commitsCountLabel(language: String, count: Int): String =
    if (count == 1) Localization.get(language, "commits_one")
    else Localization.get(language, "commits_count").replace("%d", count.toString())

/**
 * The list under the head, grouped by the day each commit landed.
 *
 * The head is one card: the glyph, what the list is, and three chips saying where
 * it comes from (the branch, how many commits are in it, the repository). The
 * grouping is the one thing an Expressive list adds over the app's flat rows -
 * the day is said once, in a chip with the count and a hairline that runs to the
 * end of the row - instead of being repeated on every commit it belongs to. The
 * rows themselves are unchanged: the history glyph, the message, the author, the
 * time and the shortened SHA.
 *
 * Takes the commits instead of fetching them, so the same screen can be rendered
 * off screen with a known list (`:desktop:storageCommitsRenderCheck`).
 */
@Composable
internal fun CommitsContent(language: String, commits: List<CommitData>?) {
    val list = commits
    LazyColumn(Modifier.fillMaxSize().padding(top = 8.dp)) {
        item {
            M3ScreenHero(
                icon = Icons.Filled.History,
                title = Localization.get(language, "commits"),
                description = Localization.get(language, "commits_desc"),
                content = {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        M3InfoChip(UpdateSource.branch(), Icons.Filled.CallSplit, tonal = true)
                        if (list != null) {
                            M3InfoChip(commitsCountLabel(language, list.size), Icons.Filled.History)
                        }
                        M3InfoChip(UpdateSource.repo(), Icons.Filled.Verified)
                    }
                },
            )
            Spacer(Modifier.height(8.dp))
        }
        when {
            list == null -> item {
                Text(
                    Localization.get(language, "loading"),
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 16.dp),
                )
            }
            list.isEmpty() -> item {
                Text(
                    Localization.get(language, "error_loading_commits"),
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.error,
                    modifier = Modifier.padding(top = 16.dp),
                )
            }
            else -> list.groupBy { it.date }.forEach { (day, dayCommits) ->
                item(key = "day-$day") {
                    CommitDayHeader(day, commitsCountLabel(language, dayCommits.size))
                }
                items(dayCommits, key = { it.sha }) { commit ->
                    CommitItem(commit, onClick = { openUrl(commit.htmlUrl) })
                    HorizontalDivider(
                        modifier = Modifier.padding(start = 68.dp),
                        color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f),
                    )
                }
            }
        }
        item { Spacer(Modifier.height(8.dp)) }
    }
}

/** The day a group of commits belongs to: its chip, its count and the rule. */
@Composable
private fun CommitDayHeader(day: String, count: String) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(top = 10.dp, bottom = 2.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Text(
            day,
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.primary,
        )
        HorizontalDivider(
            modifier = Modifier.weight(1f),
            color = MaterialTheme.colorScheme.outlineVariant,
        )
        Text(
            count,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun CommitItem(commit: CommitData, onClick: () -> Unit) {
    Row(
        Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 4.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        // Left: commit icon in a circle (the mobile's R.drawable.commit indicator).
        Box(
            modifier = Modifier
                .size(40.dp)
                .background(MaterialTheme.colorScheme.primaryContainer, CircleShape),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                Icons.Filled.History,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onPrimaryContainer,
                modifier = Modifier.size(20.dp),
            )
        }

        // Center: commit message + author/time + short SHA. The day is the group's
        // header above, so the row carries the time instead of the date.
        Column(
            Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Tooltip(commit.message) {
                Text(
                    commit.message,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                Tooltip(commit.authorName) {
                    Text(
                        commit.authorName,
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.primary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
                Text(
                    "·",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                if (commit.time.isNotBlank()) {
                    Icon(
                        Icons.Filled.Schedule,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(14.dp),
                    )
                    Text(
                        commit.time,
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
            Surface(
                color = MaterialTheme.colorScheme.secondaryContainer,
                shape = MaterialTheme.shapes.extraSmall,
            ) {
                Text(
                    commit.sha.take(7),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSecondaryContainer,
                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                )
            }
        }

        Spacer(Modifier.width(4.dp))

        // Right: the row opens on GitHub, then who wrote it (fallback to initials).
        Icon(
            Icons.AutoMirrored.Filled.OpenInNew,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(16.dp),
        )
        Spacer(Modifier.width(4.dp))

        // The author's avatar (fallback to initials).
        val avatar = commit.authorAvatarUrl
        if (avatar != null) {
            AsyncImage(
                model = avatar,
                contentDescription = commit.authorName,
                modifier = Modifier
                    .size(36.dp)
                    .clip(CircleShape),
            )
        } else {
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .background(MaterialTheme.colorScheme.tertiaryContainer, CircleShape),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    commit.authorName.firstOrNull()?.uppercaseChar()?.toString() ?: "?",
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onTertiaryContainer,
                )
            }
        }
    }
}
