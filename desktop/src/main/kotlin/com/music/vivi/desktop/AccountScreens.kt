package com.music.vivi.desktop

import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material.icons.filled.CloudUpload
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.setValue
import androidx.compose.runtime.getValue
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun AccountSection(
    language: String,
    isLoggedIn: Boolean,
    onOpenLogin: () -> Unit,
    onLogout: () -> Unit,
    onLoggedIn: () -> Unit,
) {
    // The preview's page, in both states: its title is not gated on the session,
    // so a signed-out Account still opens on the word Account over the sign-in.
    Text(Localization.get(language, "account"), style = MaterialTheme.typography.titleLarge)
    Spacer(Modifier.height(16.dp))
    if (isLoggedIn) {
        // The account's own card: who is signed in, and the one action that
        // belongs to that account. It replaces a hero that said "Connected to
        // Google" (which the session card now says in one word) and the way out
        // that stood on a card of its own, in the error colour, as if signing out
        // were a delete.
        val accountName = remember(settingsFileRevision()) { DesktopSettings.load().accountName }
        M3SettingsGroup(
            items = listOf(
                M3SettingsItem(
                    icon = Icons.Filled.Person,
                    title = { Text(accountName.ifBlank { "YouTube" }) },
                    description = { Text(Localization.get(language, "account_signed_in")) },
                    trailing = {
                        Tooltip(Localization.get(language, "logout")) {
                            Button(onClick = onLogout) {
                                Text(Localization.get(language, "logout"))
                            }
                        }
                    },
                ),
            ),
        )
        Spacer(Modifier.height(16.dp))
        // Mobile's "Auto sync with account": the account's playlists are
        // mirrored into the local store, so they can be played, queued and
        // edited offline instead of only being listed by the sidebar.
        var syncPlaylists by remember(settingsFileRevision()) { mutableStateOf(DesktopSettings.load().syncPlaylistsWithYoutube) }
        val syncStatus by PlaylistSync.status.collectAsState()
        // The switch and the run it triggers are one subject (the account's
        // playlists) and so one card: as a row, "Sync playlist" is the same kind
        // of control as the switch above it, and its state is that row's own
        // description instead of a text floating beside a button.
        val syncRunning = syncStatus.phase == PlaylistSync.Phase.RUNNING
        val syncStatusText = when (syncStatus.phase) {
            // A whole-library sync is minutes of network work, so the line reports
            // where it actually is (playlists done / total, songs moved, current
            // playlist), not just "in progress".
            // The counter in the row's trailing slot already says `3/12`, so
            // the sentence under the title carries the rest of the run: the
            // songs moved and the playlist being worked on.
            PlaylistSync.Phase.RUNNING -> syncStatus.detailText(
                Localization.get(
                    language,
                    if (syncStatus.uploading) "playlists_upload" else "sync_in_progress",
                ),
            )
            PlaylistSync.Phase.DONE -> if (syncStatus.playlists == 0 && syncStatus.created == 0) {
                Localization.get(language, "sync_finished")
            } else {
                "${Localization.get(language, "sync_finished")} (${syncStatus.playlists + syncStatus.created})"
            }
            PlaylistSync.Phase.FAILED -> Localization.get(language, "update_failed")
            PlaylistSync.Phase.IDLE -> null
        }
        // One card of three rows: the switch, the run it triggers and the upload
        // that closes the same job. The preview folds them together because the
        // button belongs to the switch above it and the upload is the same job
        // one step further; as three cards the page read as three subjects.
        val syncItems = mutableListOf(
                M3SettingsItem(
                    icon = Icons.Filled.Sync,
                    title = { Text(Localization.get(language, "yt_sync")) },
                    description = { Text(Localization.get(language, "ytm_sync")) },
                    trailing = { Switch(checked = syncPlaylists, onCheckedChange = null) },
                    onClick = {
                        val next = !syncPlaylists
                        syncPlaylists = next
                        DesktopSettings.update { it.copy(syncPlaylistsWithYoutube = next) }
                        if (next) PlaylistSync.sync("switched on")
                    },
                ),
                M3SettingsItem(
                    // The preview's own glyph: this row runs the same sync the
                    // switch above it enables, so it carries the same mark, not a
                    // refresh that reads as "check for updates".
                    icon = Icons.Filled.Sync,
                    title = { Text(Localization.get(language, "sync_now")) },
                    description = if (syncStatusText != null) {
                        { Text(syncStatusText) }
                    } else {
                        null
                    },
                    enabled = syncPlaylists && !syncRunning,
                    onClick = { PlaylistSync.sync("manual") },
                    // The run's state is a counter, not a spinner alone: `3/12`
                    // says how much of the library is done and how much is
                    // left, which a ring cannot.
                    trailing = if (syncRunning) {
                        { SyncRunTrailing(syncStatus) }
                    } else {
                        null
                    },
                ),
            )
        // The other direction, and the one the user asks for explicitly: the
        // local playlists that are NOT on the account yet are created there and
        // their songs uploaded (the mobile app only offers this at creation time,
        // from its create dialog's switch — this is the same for the playlists
        // that already exist). The account's playlists are pulled down in the
        // same run, so the two sides end up mirroring each other.
        val localPlaylists by PlaylistStore.all.collectAsState()
        val pendingUpload = localPlaylists.count { p ->
            !p.deleted && p.remoteId == null && !PlaylistSync.isMirrored(p.id)
        }
        // Same plan as the playlist list's own button: the row is labelled by
        // what it would really do (create, sync, or both), not always "Create"
        // — which was wrong once every local playlist already existed on the
        // account under its own name.
        val uploadPlan by PlaylistSync.uploadPlan.collectAsState()
        val uploadPlanKey = localPlaylists
            .filterNot { it.deleted }
            .joinToString("|") {
                "${it.id}:${PlaylistSync.accountPlaylistIdOf(it)}:${it.name}"
            }
        LaunchedEffect(uploadPlanKey) { PlaylistSync.refreshUploadPlan() }
        val uploadLabel = Localization.get(language, uploadPlan.labelKey)
        var confirmUpload by remember { mutableStateOf(false) }
        syncItems += M3SettingsItem(
            icon = Icons.Filled.CloudUpload,
            title = { Text(uploadLabel) },
            description = {
                Text(
                    if (pendingUpload > 0) {
                        Localization.get(language, "playlists_upload_desc")
                    } else {
                        Localization.get(language, "playlists_upload_none")
                    },
                )
            },
            enabled = pendingUpload > 0 && syncStatus.phase != PlaylistSync.Phase.RUNNING,
            onClick = { confirmUpload = true },
        )
        M3SettingsGroup(items = syncItems)
        if (confirmUpload) {
            AlertDialog(
                onDismissRequest = { confirmUpload = false },
                title = { Text(uploadLabel) },
                text = {
                    Text(
                        // Nothing new to create: the generic description of the
                        // action is the honest body (it creates the missing ones
                        // — none — and syncs the account's playlists back),
                        // instead of a sentence about creating N playlists.
                        if (uploadPlan.create == 0 && uploadPlan.update > 0) {
                            Localization.get(language, "playlists_upload_desc")
                        } else {
                            Localization.get(language, "playlists_upload_confirm")
                                .replace(
                                    "%d",
                                    (if (uploadPlan.create > 0) uploadPlan.create else pendingUpload)
                                        .toString(),
                                )
                        },
                    )
                },
                confirmButton = {
                    TextButton(onClick = {
                        confirmUpload = false
                        PlaylistSync.uploadMissing("manual")
                    }) { Text(Localization.get(language, "ok")) }
                },
                dismissButton = {
                    TextButton(onClick = { confirmUpload = false }) {
                        Text(Localization.get(language, "cancel"))
                    }
                },
            )
        }
    } else {
        // Show the sign-in options directly here instead of a "Log in" button
        // that opens a second screen: the user picks Google or manual cookies
        // without an extra navigation step.
        LoginContent(language = language, onLoggedIn = {
            onLoggedIn()
        })
    }
}

/**
 * The trailing slot of the account screen's sync row (`Sync now`): the run's
 * counter, `3/12`, against the playlist total the run was given.
 *
 * A ring alone said "something is happening" and nothing about how much was
 * left, which is the wrong half of the answer for a whole-library sync that
 * takes minutes. Before the total is known (`0`) there is nothing to count yet,
 * so the 18 dp ring stands in - the same state the app had before, kept for the
 * first second of a run instead of for all of it.
 */
@Composable
fun SyncRunTrailing(status: PlaylistSync.Status) {
    if (status.total > 0) {
        Text(
            "${status.done}/${status.total}",
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    } else {
        CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
    }
}
