package com.music.vivi.desktop

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
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material.icons.filled.Speaker
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Build
import java.net.URLEncoder
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.setValue
import androidx.compose.runtime.getValue
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.key.key
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.music.vivi.sync.SyncConnectionState
import com.music.vivi.sync.SyncServer
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
fun DeviceSyncSection(
    language: String,
    syncManager: DesktopSyncManager,
    syncViviVolume: Boolean,
    onToggleSyncViviVolume: (Boolean) -> Unit,
    syncNativeVolume: Boolean,
    onToggleSyncNativeVolume: (Boolean) -> Unit,
) {
    var serverUrl by remember {
        val saved = DesktopSettings.load().serverUrl
        // Default to the same relay the Android app uses; treat the old
        // hardcoded localhost placeholder as "not set" so it gets migrated.
        mutableStateOf(if (saved.isBlank() || saved == "wss://localhost:8080") SyncServer.DEFAULT_URL else saved)
    }
    val connectionState by syncManager.connectionState.collectAsState()
    val status by syncManager.status.collectAsState()
    val pairCode by syncManager.pairCode.collectAsState()
    val pairCodeExpiresAt by syncManager.pairCodeExpiresAt.collectAsState()
    val paired by syncManager.paired.collectAsState()
    val peerDeviceName by syncManager.peerDeviceName.collectAsState()
    val lanRunning by syncManager.lanRunning.collectAsState()
    val lanAddress by syncManager.lanAddress.collectAsState()
    val syncedSettings by syncManager.syncedSettings.collectAsState()

    // Ticking clock for the pairing-code expiry countdown.
    var nowMs by remember { mutableStateOf(System.currentTimeMillis()) }
    LaunchedEffect(pairCodeExpiresAt) {
        while (pairCodeExpiresAt > 0L) {
            nowMs = System.currentTimeMillis()
            if (nowMs >= pairCodeExpiresAt) break
            delay(1000)
        }
        nowMs = System.currentTimeMillis()
    }
    val remainingMs = (pairCodeExpiresAt - nowMs).coerceAtLeast(0L)

    Text(Localization.get(language, "device_sync"), style = MaterialTheme.typography.titleLarge, modifier = Modifier.padding(top = 16.dp))

    // Connection method selector: the relay (recommended, works from any
    // network) or the local LAN server (same Wi-Fi). The how-to steps and the
    // shown controls follow the chosen method.
    var connectionMethod by remember { mutableStateOf("relay") }
    val relayMode = connectionMethod == "relay"
    var methodExpanded by remember { mutableStateOf(false) }

    // The QR code carries the active connection address (relay server or LAN
    // relay) plus the current 6-digit pairing code (when available), so the
    // phone can auto-fill both and the user only has to verify before pairing.
    // Shown for both connection methods.
    val qrAddr = if (relayMode) serverUrl.trim() else lanAddress
    val qrContent = if (qrAddr.isNotEmpty() && pairCode.isNotEmpty()) {
        "vivimusic://pair?addr=${URLEncoder.encode(qrAddr, "UTF-8")}&code=$pairCode"
    } else {
        qrAddr
    }
    Box(Modifier.padding(top = 8.dp)) {
        Tooltip(Localization.get(language, "tooltip_connection_method")) {
            OutlinedButton(onClick = { methodExpanded = true }) {
                Text(Localization.get(language, if (relayMode) "method_relay" else "method_lan"))
            }
        }
        DropdownMenu(expanded = methodExpanded, onDismissRequest = { methodExpanded = false }) {
            DropdownMenuItem(
                text = { Text(Localization.get(language, "method_relay")) },
                onClick = {
                    methodExpanded = false
                    connectionMethod = "relay"
                },
            )
            DropdownMenuItem(
                text = { Text(Localization.get(language, "method_lan")) },
                onClick = {
                    methodExpanded = false
                    connectionMethod = "lan"
                },
            )
        }
    }

    DeviceSyncHowTo(language, relayMode)

    // Download links for the Android APKs, so the phone runs the matching build
    // before pairing. They are not release assets any more: 'Build Android APK'
    // publishes the newest GMS/FOSS builds to .releases/apk/latest on the
    // apk-latest branch, so those fixed URLs always serve the latest build.
    //
    // The whole section disappears when `hide_custom_apk_download_button` is true
    // in settings.json — which is the default, and the only place that option
    // exists (there is no switch for it anywhere in the UI).
    val hideApkDownload by remember(settingsFileRevision()) {
        mutableStateOf(DesktopSettings.load().hideCustomApkDownloadButton)
    }
    val syncScope = rememberCoroutineScope()
    if (!hideApkDownload) {
        var downloadFailed by remember { mutableStateOf(false) }
        Text(
            Localization.get(language, "download_mobile_apk"),
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.padding(top = 8.dp),
        )
        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.padding(top = 6.dp),
        ) {
            // GMS first: it is the build that installs on any phone. "GMS"/"FOSS"
            // are variant names (file names), not prose, so they stay untranslated.
            listOf("GMS" to ApkDownloads.GMS_URL, "FOSS" to ApkDownloads.FOSS_URL).forEach { (label, url) ->
                OutlinedButton(onClick = { if (!openUrl(url)) downloadFailed = true }) {
                    Icon(
                        Icons.Filled.Download,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(18.dp),
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(label)
                }
            }
        }
        if (downloadFailed) {
            Text(
                Localization.get(language, "open_failed"),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.error,
                modifier = Modifier.padding(top = 4.dp),
            )
        }
    }

    // The two volume channels fold away under the app's own "Advanced" heading.
    // They are the channels the sync reads - the in-app slider and the operating
    // system's own media volume - each its own data-store key, so they are wanted
    // independently; but they are set once and then left alone, and as two loose
    // rows between the connection method and the relay field they read as a part
    // of the connection flow. The group opens unrolled, and where the pointer
    // left it is not a setting: nothing about it is written to disk.
    var advancedExpanded by remember { mutableStateOf(true) }
    M3SettingsCollapsibleGroup(
        title = Localization.get(language, "advanced"),
        expanded = advancedExpanded,
        onExpandedChange = { advancedExpanded = it },
        items = listOf(
            M3SettingsItem(
                icon = Icons.Filled.VolumeUp,
                title = { Text(Localization.get(language, "sync_vivi_volume")) },
                trailing = { Switch(checked = syncViviVolume, onCheckedChange = onToggleSyncViviVolume) },
                onClick = { onToggleSyncViviVolume(!syncViviVolume) },
            ),
            // The native (OS) volume has its own switch: it is a different channel
            // from the in-app slider above, and the two are wanted independently.
            // The label says NATIVE — next to "Sync VIVI volume" a bare "Sync
            // volume" read as a second app-volume sync.
            M3SettingsItem(
                icon = Icons.Filled.Speaker,
                title = { Text(Localization.get(language, "sync_os_volume")) },
                trailing = { Switch(checked = syncNativeVolume, onCheckedChange = onToggleSyncNativeVolume) },
                onClick = { onToggleSyncNativeVolume(!syncNativeVolume) },
            ),
        ),
    )

    val connecting = connectionState == SyncConnectionState.CONNECTING
    // Connected to the relay from the field above (not via the LAN server).
    val relayConnected = connectionState == SyncConnectionState.CONNECTED && !lanRunning

    if (relayMode) {
        OutlinedTextField(
            value = serverUrl,
            onValueChange = { serverUrl = it },
            modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
            singleLine = true,
            label = { Text(Localization.get(language, "relay_server")) },
        )
        Row(
            Modifier.fillMaxWidth().padding(top = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            when {
                connecting -> Button(onClick = {}, enabled = false) {
                    Text(Localization.get(language, "lt_connecting"))
                }
                // Already on the relay: the same button now just re-issues the
                // pairing code (pair another phone, or replace an expired one).
                relayConnected -> {
                    Button(onClick = { syncManager.requestPairingCode() }) {
                        Text(Localization.get(language, "regenerate_pair_code"))
                    }
                    OutlinedButton(onClick = { syncManager.disconnect() }) {
                        Text(Localization.get(language, "disconnect"))
                    }
                }
                else -> Button(onClick = {
                    syncManager.connect(serverUrl)
                    // A pairing code can only be issued once the socket is up:
                    // connect, then request it as soon as the relay reports
                    // CONNECTED (same retry pattern as the LAN server start).
                    syncScope.launch {
                        repeat(8) {
                            if (syncManager.pairCode.value.isNotEmpty()) return@launch
                            if (syncManager.connectionState.value == SyncConnectionState.CONNECTED) {
                                syncManager.requestPairingCode()
                            }
                            delay(1_000L)
                        }
                    }
                }) {
                    Text(Localization.get(language, "connect_and_generate_code"))
                }
            }
        }
        // The first connect to a sleeping free-tier relay can take a while: it
        // has to wake the server before the WebSocket handshake completes.
        if (connecting || connectionState == SyncConnectionState.ERROR) {
            Text(
                Localization.get(language, "connect_hint"),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 6.dp),
            )
        }
        // Auto-issue the pairing code whenever the relay is up, unpaired and no
        // code is pending, so pairing never stalls behind a hidden button.
        LaunchedEffect(relayMode, relayConnected, paired, pairCode) {
            if (relayMode && relayConnected && !paired && pairCode.isEmpty()) {
                syncManager.requestPairingCode()
            }
        }
    } else {
        Text(Localization.get(language, "lan_sync"), style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(top = 16.dp))
        Button(
            onClick = { if (lanRunning) syncManager.stopLan() else syncManager.startLan() },
            modifier = Modifier.padding(top = 4.dp),
        ) {
            Text(Localization.get(language, if (lanRunning) "stop_lan" else "start_lan"))
        }
        if (lanRunning && lanAddress.isNotEmpty()) {
            Text(
                Localization.get(language, "lan_hint"),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 8.dp),
            )
        }
    }

    // Relay mode: code generation lives in the single action button above, so
    // the panel here only shows the code + expiry (no duplicate button). LAN
    // mode keeps its own Generate button as before.
    val showQr = pairCode.isNotEmpty() && qrAddr.isNotEmpty()
    if (showQr) {
        Row(
            Modifier.fillMaxWidth().padding(top = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Column(
                Modifier.widthIn(max = 260.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                // 220 dp, not 180: the payload is a full `vivimusic://pair` URL, so
                // each module is only a few pixels at 180 dp and a phone camera
                // needed several attempts to lock onto it. The extra size makes
                // the code decode on the first frame.
                QrCode(qrContent, size = 220.dp)
                Text(
                    "${Localization.get(language, if (relayMode) "relay_server" else "lan_address")}: $qrAddr",
                    style = MaterialTheme.typography.bodySmall,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.padding(top = 8.dp),
                )
                Text(
                    Localization.get(language, "scan_qr"),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 4.dp),
                )
            }
            // Pairing code + generate button sit to the right of the QR code.
            PairingCodePanel(
                language = language,
                pairCode = pairCode,
                remainingMs = remainingMs,
                onGenerate = { syncManager.requestPairingCode() },
                showGenerate = !relayMode,
                modifier = Modifier.weight(1f),
            )
        }
    } else if (!relayMode || pairCode.isNotEmpty()) {
        PairingCodePanel(
            language = language,
            pairCode = pairCode,
            remainingMs = remainingMs,
            onGenerate = { syncManager.requestPairingCode() },
            showGenerate = !relayMode,
            modifier = Modifier.padding(top = 8.dp),
        )
    }

    // ONE leaving action, not two: the relay "Disconnect" above already drops
    // the pairing (it tells the peer and clears the pair id), so an Unpair
    // button next to a live link was the same action twice. This button only
    // appears when there is no link to disconnect — paired but offline, where
    // Disconnect is not on screen at all — so exactly one is visible at a time.
    if (paired && !relayConnected && !lanRunning) {
        Button(onClick = { syncManager.unpair() }, modifier = Modifier.padding(top = 8.dp)) {
            Text(Localization.get(language, "unpair"))
        }
    }

    if (paired && peerDeviceName.isNotBlank()) {
        Text(
            "${Localization.get(language, "paired_device")}: $peerDeviceName",
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.padding(top = 8.dp),
        )
    }

    // Honest status: CONNECTED only means the relay socket is open. Until a
    // device is actually paired, say it is waiting instead of implying that
    // sync is already active.
    val statusText = if (connectionState == SyncConnectionState.CONNECTED && !paired) {
        Localization.get(language, "waiting_for_pairing")
    } else {
        status
    }
    Text("${Localization.get(language, "status")}: $connectionState — $statusText", modifier = Modifier.padding(top = 8.dp))

    if (syncedSettings.isNotEmpty()) {
        Text(Localization.get(language, "synced_settings"), style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(top = 16.dp))
        Column(Modifier.padding(top = 4.dp)) {
            syncedSettings.entries.sortedBy { it.key }.forEach { (k, v) ->
                Text("$k = $v", style = MaterialTheme.typography.bodySmall, modifier = Modifier.padding(vertical = 1.dp))
            }
        }
    }
}

@Composable
private fun DeviceSyncHowTo(language: String, relayMode: Boolean) {
    Surface(
        tonalElevation = 2.dp,
        shape = RoundedCornerShape(12.dp),
        modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
    ) {
        Column(Modifier.padding(16.dp)) {
            Text(
                Localization.get(language, "how_to_connect"),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
            )
            Spacer(Modifier.height(12.dp))
            if (relayMode) {
                HowToStep(1, Localization.get(language, "how_to_relay_step1"))
                HowToStep(2, Localization.get(language, "how_to_relay_step2"))
                HowToStep(3, Localization.get(language, "how_to_relay_step3"))
            } else {
                HowToStep(1, Localization.get(language, "how_to_step1"))
                HowToStep(2, Localization.get(language, "how_to_step2"))
                HowToStep(3, Localization.get(language, "how_to_step3"))
                HowToStep(4, Localization.get(language, "how_to_step4"))
            }
        }
    }
}

@Composable
private fun HowToStep(number: Int, text: String) {
    Row(Modifier.padding(vertical = 4.dp)) {
        Text(
            number.toString(),
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.widthIn(min = 20.dp),
        )
        Text(
            text,
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.padding(start = 8.dp),
        )
    }
}

@Composable
private fun PairingCodePanel(
    language: String,
    pairCode: String,
    remainingMs: Long,
    onGenerate: () -> Unit,
    modifier: Modifier = Modifier,
    showGenerate: Boolean = true,
) {
    Column(modifier) {
        // The desktop is the code generator: the phone only enters this code.
        // In relay mode the action button above already covers generation, so
        // the panel hides its own to avoid two identical buttons.
        if (showGenerate) {
            Button(onClick = onGenerate) {
                Text(Localization.get(language, if (pairCode.isNotEmpty()) "generate_new_code" else "generate_code"))
            }
        }
        if (pairCode.isNotEmpty()) {
            // Selectable so the user can copy the code if the QR scan fails.
            SelectionContainer {
                Text(
                    "${Localization.get(language, "code_hint")}: $pairCode",
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.padding(top = 8.dp),
                )
            }
            Text(
                if (remainingMs > 0L) {
                    "${Localization.get(language, "code_expires_in")} ${formatCountdown(remainingMs)}"
                } else {
                    Localization.get(language, "code_expired")
                },
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 4.dp),
            )
        }
    }
}

/** Formats a millisecond duration as `M:SS` for the pairing-code countdown. */
private fun formatCountdown(ms: Long): String {
    val totalSec = (ms / 1000).coerceAtLeast(0)
    return "%d:%02d".format(totalSec / 60, totalSec % 60)
}
