package com.music.vivi.desktop

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.automirrored.filled.PlaylistPlay
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp

/**
 * The Spotify mark, taken from the mobile app's own drawable
 * (`app/src/main/res/drawable/spotify.xml`) so the import is recognisable as
 * Spotify on both platforms instead of carrying a generic library icon. Same
 * path data, same 50x50 viewport, and, like every icon here, tinted by the
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
 * whole credential: `SpotifyAuth` turns it into the short-lived token the web
 * player uses, and nothing else is asked for.
 *
 * The manual paste the desktop shipped first is still here, but only as the
 * fallback for a machine where the window cannot be created at all (no JavaFX,
 * no display): the sign-in window is the only way to get a cookie that does not
 * require reading one out of a browser's developer tools by hand.
 *
 * The screen is built from the same three pieces the Google sign-in and the
 * Account screen use: a hero that states what the screen is and whether an
 * account is connected, a numbered card that ends in the single button that opens
 * the window, and settings groups for the connected state (the account, its
 * sources). The manual paste folds into [M3CollapsibleCard], so it is one header
 * row until the user asks for the fields instead of a second full-size card
 * offering the same decision.
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
    // window could be created, and the user can ask for it at any time: a
    // Google-only account can never finish the sign-in inside the window (see
    // `spotify_google_blocked`), so the paste has to be reachable without
    // waiting for the window to fail. It opens by itself on a machine where the
    // window cannot exist at all, because on that machine the paste is not the
    // fallback: it is the only way in.
    var manualFallback by remember { mutableStateOf(SpotifyLoginWebView.isUnavailable) }

    // A refresh replaces the list: a selection pointing at playlists that are no
    // longer there would silently import nothing, so it is narrowed to what is
    // actually on screen.
    val sourceIds = state.playlists.map { it.id }
    LaunchedEffect(sourceIds) {
        selected = selected.filter { it == SpotifyImport.LIKED_SOURCE || it in sourceIds }.toSet()
    }

    SettingsSubScreen(language, onBack) {
        SpotifyHero(language = language, connectedAs = state.accountName.takeIf { state.connected })

        state.error?.let { message ->
            SpotifyErrorRow(language = language, message = message, onDismiss = { SpotifyImport.dismissError() })
        }

        if (!state.connected) {
            SpotifyConnectCard(
                language = language,
                loading = state.loading,
                // The manual card below owns the action while it is open, exactly
                // like the Google screen.
                manualOpen = manualFallback,
                onStart = {
                    if (!SpotifyImport.loginWithWindow(language)) manualFallback = true
                },
            )
            Spacer(Modifier.height(12.dp))
            M3CollapsibleCard(
                title = Localization.get(language, "login_manual_title"),
                expanded = manualFallback,
                onExpandedChange = { manualFallback = it },
            ) {
                // Why the fields are here at all, but only when the window really
                // cannot be created, since the same card also opens on request.
                if (SpotifyLoginWebView.isUnavailable) {
                    Text(
                        Localization.get(language, "login_webview_unavailable"),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                Text(
                    Localization.get(language, "spotify_cookie_hint"),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 8.dp),
                )
                // The two fields are labelled with the cookie names themselves:
                // `sp_dc` and `sp_key` are Spotify's identifiers, not words, so
                // there is nothing to translate and no key to keep in 53 tables.
                OutlinedTextField(
                    value = spDc,
                    onValueChange = { spDc = it },
                    label = { Text("sp_dc") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().padding(top = 12.dp),
                )
                OutlinedTextField(
                    value = spKey,
                    onValueChange = { spKey = it },
                    label = { Text("sp_key") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                )
                Button(
                    onClick = { SpotifyImport.connect(spDc, spKey) },
                    enabled = spDc.isNotBlank() && !state.loading,
                    modifier = Modifier.fillMaxWidth().padding(top = 14.dp),
                ) {
                    Text(
                        if (state.loading) Localization.get(language, "logging_in")
                        else Localization.get(language, "login"),
                    )
                }
            }
        } else {
            SpotifyAccountGroup(
                language = language,
                accountName = state.accountName,
                playlistCount = state.playlists.size,
                loading = state.loading,
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

        progress?.let { p -> ImportProgressCard(language, p) }
        Spacer(Modifier.height(24.dp))
    }
}

/**
 * What the screen is and whether it has an account, in the shape the Google
 * sign-in and the Account screen use ([SignInHero]).
 *
 * The state is the title, not a pill: with no account the head carries the
 * settings row's own name for the screen (`spotify_import`), and with one it is
 * about the account that is connected (`spotify_connected_title`), which is what
 * the screen is then for. There is no badge in either state: the card under the
 * head offers the single action, and a pill saying "not connected" only repeated
 * what the sentence under the title already says.
 */
@Composable
internal fun SpotifyHero(language: String, connectedAs: String?) {
    SignInHero(
        title = Localization.get(
            language,
            if (connectedAs != null) "spotify_connected_title" else "spotify_import",
        ),
        promise = Localization.get(language, "spotify_desc"),
        stateText = null,
        active = connectedAs != null,
        mark = { Icon(SpotifyIcon, contentDescription = null, modifier = Modifier.size(30.dp)) },
    )
}

/**
 * One card that numbers the three steps the screen already printed and ends in
 * the single button that opens the sign-in window, exactly like the Google card
 * next door: the two screens do the same job with two providers, so they read the
 * same way. The progress of the window's own preparation is drawn right under the
 * button that opened it, where the user is looking.
 *
 * That button is the card's only action and the manual-cookie card below has one
 * of its own, so only one is offered at a time: with [manualOpen] the button
 * leaves upwards and the card loses its row while the form unfolds, the two
 * movements starting in the same frame (the same hand-over as the Google card).
 */
@Composable
internal fun SpotifyConnectCard(
    language: String,
    loading: Boolean,
    manualOpen: Boolean,
    onStart: () -> Unit,
) {
    // What the sign-in window is doing while it gets ready. It used to be a bar in
    // the window's own header, which is not where the user is looking when the
    // first run spends a while downloading and unpacking Chromium.
    val preparing by SpotifyLoginProgress.value.collectAsState()

    Card(
        modifier = Modifier.fillMaxWidth().padding(top = 16.dp),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh),
    ) {
        Column(Modifier.padding(18.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .clip(RoundedCornerShape(14.dp))
                        .background(MaterialTheme.colorScheme.secondaryContainer),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        SpotifyIcon,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSecondaryContainer,
                        modifier = Modifier.size(22.dp),
                    )
                }
                Text(
                    Localization.get(language, "spotify_open_login"),
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.padding(start = 12.dp),
                )
            }
            SignInStepList(
                language = language,
                keys = listOf("spotify_step1", "spotify_step2", "spotify_step3"),
            )
            AnimatedVisibility(
                visible = !manualOpen,
                enter = Animations.signInActionEnter(),
                exit = Animations.signInActionExit(),
            ) {
                Button(
                    onClick = onStart,
                    enabled = !loading,
                    modifier = Modifier.fillMaxWidth().padding(top = 14.dp),
                ) {
                    if (loading) {
                        CircularProgressIndicator(Modifier.size(18.dp), strokeWidth = 2.dp)
                    } else {
                        Icon(SpotifyIcon, contentDescription = null, modifier = Modifier.size(18.dp))
                    }
                    Spacer(Modifier.width(10.dp))
                    Text(Localization.get(language, "spotify_open_login"))
                }
            }
            preparing?.let { percent ->
                if (percent < 0f) {
                    // Working, with no number to show (CEF reports no progress for
                    // a page load): a moving bar instead of a stuck 0 %.
                    LinearProgressIndicator(Modifier.fillMaxWidth().padding(top = 10.dp))
                } else {
                    LinearProgressIndicator(
                        progress = { (percent / 100f).coerceIn(0f, 1f) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 10.dp)
                            .clip(RoundedCornerShape(50)),
                    )
                    Text(
                        "${percent.toInt().coerceIn(0, 100)}%",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(top = 4.dp),
                    )
                }
            }
        }
    }
}

/** A failed request, in the theme's error container instead of bare red text. */
@Composable
private fun SpotifyErrorRow(language: String, message: String, onDismiss: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 12.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(MaterialTheme.colorScheme.errorContainer)
            .padding(horizontal = 12.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            Icons.Filled.ErrorOutline,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onErrorContainer,
            modifier = Modifier.size(18.dp),
        )
        Text(
            message,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onErrorContainer,
            modifier = Modifier.weight(1f).padding(start = 8.dp),
        )
        TextButton(
            onClick = onDismiss,
            colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.onErrorContainer),
        ) {
            Text(Localization.get(language, "close"))
        }
    }
}

/**
 * The connected account and what can be done with it, as rows of one card: who is
 * connected with how many playlists, a refresh, and the way out. Refresh and
 * disconnect used to be two outlined buttons floating to the right of the account
 * name, which made them look like a toolbar; as rows they are the same kind of
 * thing as the settings around them.
 */
@Composable
internal fun SpotifyAccountGroup(
    language: String,
    accountName: String,
    playlistCount: Int,
    loading: Boolean,
    onRefresh: () -> Unit,
    onDisconnect: () -> Unit,
) {
    Spacer(Modifier.height(16.dp))
    M3SettingsGroup(
        items = listOf(
            M3SettingsItem(
                icon = Icons.Filled.AccountCircle,
                title = {
                    Text(
                        Localization.get(language, "spotify_connected_as").format(accountName),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                },
                description = {
                    Text("$playlistCount ${Localization.get(language, "playlists").lowercase()}")
                },
            ),
            M3SettingsItem(
                icon = Icons.Filled.Refresh,
                title = { Text(Localization.get(language, "refresh")) },
                enabled = !loading,
                onClick = onRefresh,
                trailing = if (loading) {
                    { CircularProgressIndicator(Modifier.size(18.dp), strokeWidth = 2.dp) }
                } else {
                    null
                },
            ),
            M3SettingsItem(
                icon = Icons.AutoMirrored.Filled.Logout,
                title = {
                    Text(Localization.get(language, "disconnect"), color = MaterialTheme.colorScheme.error)
                },
                onClick = onDisconnect,
            ),
        ),
    )
}

/**
 * The sources the account offers, as checkbox rows of one card: Liked Songs first
 * (the phone's own order), then every playlist with its track count. The row is
 * the toggle and the checkbox only reports the state, so a click anywhere on the
 * row cannot toggle twice.
 */
@Composable
internal fun SpotifySourcesSection(
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

    M3SettingsGroup(
        title = Localization.get(language, "playlists"),
        items = buildList {
            add(
                M3SettingsItem(
                    icon = Icons.Filled.Favorite,
                    title = { Text(Localization.get(language, "spotify_liked_songs")) },
                    description = {
                        Text("${state.likedSongsCount} ${Localization.get(language, "songs").lowercase()}")
                    },
                    trailing = {
                        Checkbox(
                            checked = SpotifyImport.LIKED_SOURCE in selected,
                            onCheckedChange = null,
                        )
                    },
                    onClick = { onToggle(SpotifyImport.LIKED_SOURCE) },
                ),
            )
            state.playlists.forEach { playlist ->
                add(
                    M3SettingsItem(
                        icon = Icons.AutoMirrored.Filled.PlaylistPlay,
                        title = {
                            Text(playlist.name, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        },
                        description = {
                            Text("${playlist.tracks?.total ?: 0} ${Localization.get(language, "songs").lowercase()}")
                        },
                        trailing = {
                            Checkbox(checked = playlist.id in selected, onCheckedChange = null)
                        },
                        onClick = { onToggle(playlist.id) },
                    ),
                )
            }
        },
    )

    Button(
        onClick = onImport,
        enabled = selected.isNotEmpty(),
        modifier = Modifier.fillMaxWidth().padding(top = 12.dp),
    ) {
        Icon(Icons.Filled.Download, contentDescription = null, modifier = Modifier.size(18.dp))
        Spacer(Modifier.width(8.dp))
        // The count rides along in the label instead of a second string: the
        // button is the only place it is needed, and "(3)" needs no translating.
        Text("${Localization.get(language, "spotify_import_selected")} (${selected.size})")
    }
}

/** The import as it runs, in the same card shape as the rest of the screen. */
@Composable
private fun ImportProgressCard(language: String, progress: SpotifyImport.Progress) {
    Card(
        modifier = Modifier.fillMaxWidth().padding(top = 14.dp),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh),
    ) {
        Column(Modifier.padding(18.dp)) {
            val label = if (progress.finished) {
                Localization.get(language, "spotify_import_done")
                    .format(progress.matched, progress.total)
            } else {
                Localization.get(language, "spotify_importing")
                    .format(progress.playlistName, progress.matched, progress.total)
            }
            Text(label, style = MaterialTheme.typography.bodyMedium)
            if (!progress.finished) {
                Spacer(Modifier.height(10.dp))
                val fraction = if (progress.total <= 0) 0f
                else progress.matched.toFloat() / progress.total.toFloat()
                LinearProgressIndicator(
                    progress = { fraction.coerceIn(0f, 1f) },
                    modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(50)),
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
}
