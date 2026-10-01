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
import androidx.compose.material3.Icon
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.PathParser
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp

/**
 * The Spotify mark, taken from the mobile app's own drawable
 * (`app/src/main/res/drawable/spotify.xml`) so the import is recognisable as
 * Spotify on both platforms instead of carrying a generic library icon. Same
 * path data, same 50x50 viewport, and — like every icon here — tinted by the
 * theme, which is why the path's own black fill does not matter.
 */
internal val SpotifyIcon: ImageVector by lazy {
    ImageVector.Builder(
        name = "Spotify",
        defaultWidth = 24.dp,
        defaultHeight = 24.dp,
        viewportWidth = 50f,
        viewportHeight = 50f,
    ).apply {
        addPath(
            pathData = PathParser().parsePathString(SPOTIFY_MARK_PATH).toNodes(),
            fill = SolidColor(Color.Black),
        )
    }.build()
}

private const val SPOTIFY_MARK_PATH =
    "M25.009,1.982C12.322,1.982,2,12.304,2,24.991S12.322,48,25.009,48s23.009-10.321,23.009-23.009S37.696,1.982,25.009,1.982z M34.748,35.333c-0.289,0.434-0.765,0.668-1.25,0.668c-0.286,0-0.575-0.081-0.831-0.252C30.194,34.1,26,33,22.5,33.001 c-3.714,0.002-6.498,0.914-6.526,0.923c-0.784,0.266-1.635-0.162-1.897-0.948s0.163-1.636,0.949-1.897 c0.132-0.044,3.279-1.075,7.474-1.077C26,30,30.868,30.944,34.332,33.253C35.022,33.713,35.208,34.644,34.748,35.333z M37.74,29.193 c-0.325,0.522-0.886,0.809-1.459,0.809c-0.31,0-0.624-0.083-0.906-0.26c-4.484-2.794-9.092-3.385-13.062-3.35 c-4.482,0.04-8.066,0.895-8.127,0.913c-0.907,0.258-1.861-0.272-2.12-1.183c-0.259-0.913,0.272-1.862,1.184-2.12 c0.277-0.079,3.854-0.959,8.751-1c4.465-0.037,10.029,0.61,15.191,3.826C37.995,27.328,38.242,28.388,37.74,29.193z M40.725,22.013 C40.352,22.647,39.684,23,38.998,23c-0.344,0-0.692-0.089-1.011-0.275c-5.226-3.068-11.58-3.719-15.99-3.725 c-0.021,0-0.042,0-0.063,0c-5.333,0-9.44,0.938-9.481,0.948c-1.078,0.247-2.151-0.419-2.401-1.495 c-0.25-1.075,0.417-2.149,1.492-2.4C11.729,16.01,16.117,15,21.934,15c0.023,0,0.046,0,0.069,0 c4.905,0.007,12.011,0.753,18.01,4.275C40.965,19.835,41.284,21.061,40.725,22.013z"


/**
 * Settings → Import from Spotify.
 *
 * Connecting is the phone's mechanism, ported: [SpotifyImport.loginWithWindow]
 * opens [SpotifyLoginWebView], the user signs in on Spotify's own page, and the
 * `sp_dc` cookie is read off the window when the page lands. That cookie is the
 * whole credential — `SpotifyAuth` turns it into the short-lived token the web
 * player uses, and nothing else is asked for.
 *
 * The manual paste the desktop shipped first is still here, but only as the
 * fallback for a machine where the window cannot be created at all (no JavaFX,
 * no display): the sign-in window is the only way to get a cookie that does not
 * require reading one out of a browser's developer tools by hand.
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
    // Turns on the manual cookie paste. It appears on its own when no sign-in
    // window could be created, and the user can ask for it at any time — a
    // Google-only account can never finish the sign-in inside the window (see
    // `spotify_google_blocked`), so the paste has to be reachable without
    // waiting for the window to fail.
    var manualFallback by remember { mutableStateOf(false) }

    // A refresh replaces the list: a selection pointing at playlists that are no
    // longer there would silently import nothing, so it is narrowed to what is
    // actually on screen.
    val sourceIds = state.playlists.map { it.id }
    LaunchedEffect(sourceIds) {
        selected = selected.filter { it == SpotifyImport.LIKED_SOURCE || it in sourceIds }.toSet()
    }

    SettingsSubScreen(language, onBack) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                SpotifyIcon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(32.dp),
            )
            Spacer(Modifier.width(12.dp))
            Text(
                Localization.get(language, "spotify_import"),
                style = MaterialTheme.typography.headlineMedium,
            )
        }
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
                manualFallback = manualFallback,
                spDc = spDc,
                onSpDcChange = { spDc = it },
                spKey = spKey,
                onSpKeyChange = { spKey = it },
                onConnect = { SpotifyImport.connect(spDc, spKey) },
                onOpenWindow = {
                    if (!SpotifyImport.loginWithWindow(language)) manualFallback = true
                },
                windowUnavailable = SpotifyLoginWebView.isUnavailable,
                onShowManual = { manualFallback = true },
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
    manualFallback: Boolean,
    spDc: String,
    onSpDcChange: (String) -> Unit,
    spKey: String,
    onSpKeyChange: (String) -> Unit,
    onConnect: () -> Unit,
    onOpenWindow: () -> Unit,
    /** True when no sign-in window can be created on this machine at all. */
    windowUnavailable: Boolean,
    onShowManual: () -> Unit,
) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(
            SpotifyIcon,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(26.dp),
        )
        Spacer(Modifier.width(10.dp))
        Text(
            Localization.get(language, "spotify_not_connected"),
            style = MaterialTheme.typography.titleMedium,
        )
    }

    // The button is the whole connect step: it opens the sign-in window, which
    // hands the cookie back on its own (see SpotifyLoginWebView).
    Button(
        onClick = onOpenWindow,
        enabled = !loading,
        modifier = Modifier.padding(top = 12.dp),
    ) {
        if (loading) {
            CircularProgressIndicator(Modifier.size(18.dp), strokeWidth = 2.dp)
            Spacer(Modifier.width(10.dp))
        } else {
            Icon(SpotifyIcon, contentDescription = null, modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(10.dp))
        }
        Text(Localization.get(language, "spotify_open_login"))
    }

    // The cookie paste is always one click away, not only after a window has
    // failed, so a window that cannot finish the sign-in is never a dead end.
    if (!manualFallback) {
        TextButton(onClick = onShowManual, modifier = Modifier.padding(top = 2.dp)) {
            Text(Localization.get(language, "login_manual_title"))
        }
        return
    }

    // Why the fields are here at all — but only when the window really cannot
    // be created, since the same block also opens on request.
    if (windowUnavailable) {
        Text(
            Localization.get(language, "login_webview_unavailable"),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(top = 10.dp),
        )
    }
    OutlinedButton(
        onClick = { openUrl(SpotifyImport.loginUrl()) },
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
