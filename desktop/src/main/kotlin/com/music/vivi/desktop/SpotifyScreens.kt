package com.music.vivi.desktop

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import java.awt.Desktop
import java.net.URI

/**
 * Settings → Import from Spotify.
 *
 * The desktop has no embedded browser for Spotify (`LoginWebView` is the YouTube
 * one and Spotify's sign-in would not survive in it either), so this screen uses
 * the same manual path the mobile app keeps as its fallback: open the sign-in
 * page in the user's browser and paste the `sp_dc` cookie back. That cookie is
 * the whole credential — `SpotifyAuth` turns it into the short-lived token the
 * web player uses, and nothing else is asked for.
 *
 * Everything below the connect step mirrors the phone's sheet: the account, the
 * playlists with their track counts, Liked Songs, and an import that reports how
 * many tracks it matched while it runs.
 */
@Composable
fun SettingsSpotifyImportScreen(language: String, onBack: () -> Unit) {
    val state by SpotifyImport.state.collectAsState()
    val progress by SpotifyImport.progress.collectAsState()

    // Opening the screen is what restores and refreshes the session: no request
    // is made for it at startup, so the app does not talk to Spotify unless the
    // user is actually here.
    LaunchedEffect(Unit) { SpotifyImport.restore() }

    var spDc by remember { mutableStateOf("") }
    var spKey by remember { mutableStateOf("") }
    var selected by remember { mutableStateOf(emptySet<String>()) }

    // A refresh replaces the list: a selection pointing at playlists that are no
    // longer there would silently import nothing, so it is narrowed to what is
    // actually on screen.
    val sourceIds = state.playlists.map { it.id }
    LaunchedEffect(sourceIds) {
        selected = selected.filter { it == SpotifyImport.LIKED_SOURCE || it in sourceIds }.toSet()
    }

    SettingsSubScreen(language, onBack) {
        Text(
            Localization.get(language, "spotify_import"),
            style = MaterialTheme.typography.headlineMedium,
            modifier = Modifier.padding(bottom = 4.dp),
        )
        Text(
            Localization.get(language, "spotify_desc"),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(bottom = 16.dp),
        )

        state.error?.let { message ->
            Row(
                Modifier
                    .fillMaxWidth()
                    .padding(bottom = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    message,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error,
                    modifier = Modifier.weight(1f),
                )
                TextButton(onClick = { SpotifyImport.dismissError() }) {
                    Text(Localization.get(language, "close"))
                }
            }
        }

        if (!state.connected) {
            SpotifyConnectSection(
                language = language,
                loading = state.loading,
                spDc = spDc,
                onSpDcChange = { spDc = it },
                spKey = spKey,
                onSpKeyChange = { spKey = it },
                onConnect = { SpotifyImport.connect(spDc, spKey) },
            )
        } else {
            SpotifyAccountRow(
                language = language,
                state = state,
                onRefresh = { SpotifyImport.refresh() },
                onDisconnect = { SpotifyImport.disconnect() },
            )
            SpotifySourcesSection(
                language = language,
                state = state,
                selected = selected,
                onToggle = { id ->
                    selected = if (id in selected) selected - id else selected + id
                },
                onImport = { SpotifyImport.import(selected.toList()) },
            )
        }

        progress?.let { p -> ImportProgressRow(language, p) }
        Spacer(Modifier.height(24.dp))
    }
}

@Composable
private fun SpotifyConnectSection(
    language: String,
    loading: Boolean,
    spDc: String,
    onSpDcChange: (String) -> Unit,
    spKey: String,
    onSpKeyChange: (String) -> Unit,
    onConnect: () -> Unit,
) {
    Text(
        Localization.get(language, "spotify_not_connected"),
        style = MaterialTheme.typography.titleMedium,
    )
    OutlinedButton(
        onClick = { runCatching { Desktop.getDesktop().browse(URI(SpotifyImport.loginUrl())) } },
        modifier = Modifier.padding(top = 8.dp),
    ) {
        Text(Localization.get(language, "spotify_open_login"))
    }
    // The two fields are labelled with the cookie names themselves: `sp_dc` and
    // `sp_key` are Spotify's identifiers, not words, so there is nothing to
    // translate and no key to keep in 51 tables.
    OutlinedTextField(
        value = spDc,
        onValueChange = onSpDcChange,
        label = { Text("sp_dc") },
        singleLine = true,
        modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
    )
    OutlinedTextField(
        value = spKey,
        onValueChange = onSpKeyChange,
        label = { Text("sp_key") },
        singleLine = true,
        modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
    )
    Text(
        Localization.get(language, "spotify_cookie_hint"),
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.padding(top = 6.dp),
    )
    Row(
        Modifier.padding(top = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Button(onClick = onConnect, enabled = spDc.isNotBlank() && !loading) {
            Text(Localization.get(language, "connect"))
        }
        if (loading) {
            Spacer(Modifier.width(12.dp))
            CircularProgressIndicator(Modifier.size(18.dp), strokeWidth = 2.dp)
        }
    }
}

@Composable
private fun SpotifyAccountRow(
    language: String,
    state: SpotifyImport.State,
    onRefresh: () -> Unit,
    onDisconnect: () -> Unit,
) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(1f)) {
            Text(
                Localization.get(language, "spotify_connected_as").format(state.accountName),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            val counts = buildString {
                append(state.playlists.size)
                append(" ")
                append(Localization.get(language, "playlists").lowercase())
            }
            Text(
                counts,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        OutlinedButton(onClick = onRefresh, enabled = !state.loading) {
            Text(Localization.get(language, "refresh"))
        }
        Spacer(Modifier.width(8.dp))
        OutlinedButton(onClick = onDisconnect) {
            Text(Localization.get(language, "disconnect"))
        }
    }
}

@Composable
private fun SpotifySourcesSection(
    language: String,
    state: SpotifyImport.State,
    selected: Set<String>,
    onToggle: (String) -> Unit,
    onImport: () -> Unit,
) {
    Spacer(Modifier.height(16.dp))
    if (state.loading) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            CircularProgressIndicator(Modifier.size(18.dp), strokeWidth = 2.dp)
            Spacer(Modifier.width(10.dp))
            Text(
                Localization.get(language, "loading"),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        return
    }
    if (state.playlists.isEmpty() && state.likedSongsCount == 0) {
        Text(
            Localization.get(language, "spotify_no_sources"),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        return
    }

    Text(
        Localization.get(language, "playlists"),
        style = MaterialTheme.typography.titleMedium,
        modifier = Modifier.padding(top = 16.dp, bottom = 6.dp),
    )
    SourceRow(
        language = language,
        title = Localization.get(language, "spotify_liked_songs"),
        count = state.likedSongsCount,
        checked = SpotifyImport.LIKED_SOURCE in selected,
        onToggle = { onToggle(SpotifyImport.LIKED_SOURCE) },
    )
    state.playlists.forEach { playlist ->
        SourceRow(
            language = language,
            title = playlist.name,
            count = playlist.tracks?.total ?: 0,
            checked = playlist.id in selected,
            onToggle = { onToggle(playlist.id) },
        )
    }

    Button(
        onClick = onImport,
        enabled = selected.isNotEmpty(),
        modifier = Modifier.padding(top = 14.dp),
    ) {
        // The count rides along in the label instead of a second string: the
        // button is the only place it is needed, and "(3)" needs no translating.
        Text("${Localization.get(language, "spotify_import_selected")} (${selected.size})")
    }
}

@Composable
private fun SourceRow(
    language: String,
    title: String,
    count: Int,
    checked: Boolean,
    onToggle: () -> Unit,
) {
    Row(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .clickable { onToggle() }
            .padding(horizontal = 4.dp, vertical = 2.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Checkbox(checked = checked, onCheckedChange = { onToggle() })
        Text(
            title,
            style = MaterialTheme.typography.bodyMedium,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f),
        )
        Text(
            "$count ${Localization.get(language, "songs").lowercase()}",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun ImportProgressRow(
    language: String,
    progress: SpotifyImport.Progress,
) {
    Column(
        Modifier
            .fillMaxWidth()
            .padding(top = 14.dp)
            .clip(RoundedCornerShape(10.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .padding(12.dp),
    ) {
        val label = if (progress.finished) {
            Localization.get(language, "spotify_import_done")
                .format(progress.matched, progress.total)
        } else {
            Localization.get(language, "spotify_importing")
                .format(progress.playlistName, progress.matched, progress.total)
        }
        Text(label, style = MaterialTheme.typography.bodyMedium)
        if (!progress.finished) {
            Spacer(Modifier.height(8.dp))
            val fraction = if (progress.total <= 0) 0f
            else progress.matched.toFloat() / progress.total.toFloat()
            LinearProgressIndicator(
                progress = { fraction.coerceIn(0f, 1f) },
                modifier = Modifier.fillMaxWidth(),
            )
        }
        Row(Modifier.fillMaxWidth().padding(top = 6.dp), horizontalArrangement = Arrangement.End) {
            if (progress.finished) {
                TextButton(onClick = { SpotifyImport.dismissProgress() }) {
                    Text(Localization.get(language, "close"))
                }
            } else {
                TextButton(onClick = { SpotifyImport.cancelImport() }) {
                    Text(Localization.get(language, "cancel"))
                }
            }
        }
    }
}
