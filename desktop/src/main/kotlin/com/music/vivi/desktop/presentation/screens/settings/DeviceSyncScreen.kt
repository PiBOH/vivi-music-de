package com.music.vivi.desktop.presentation.screens.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
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
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.music.vivi.desktop.generated.resources.Res
import com.music.vivi.desktop.generated.resources.ic_automirrored_rounded_arrow_back
import com.music.vivi.desktop.generated.resources.ic_rounded_computer
import com.music.vivi.desktop.generated.resources.ic_rounded_phone_android
import com.music.vivi.desktop.generated.resources.ic_rounded_sync
import com.music.vivi.desktop.presentation.components.ExpressiveSwitchRow
import com.music.vivi.desktop.presentation.components.subcomps.AppTooltip
import com.music.vivi.desktop.presentation.viewmodel.PlayerViewModel
import com.music.vivi.sync.SyncConnectionState
import com.music.vivi.sync.SyncServer
import java.net.URLEncoder
import org.jetbrains.compose.resources.painterResource

/**
 * Device Sync, the 1.55 pairing surface rebuilt as two Material 3 cards:
 *
 * - left: the "Axolotl Music DE" identity and every sync option;
 * - right: one state at a time — the button, then the QR, then the live
 *   terminal once the Android app has paired.
 *
 * The pairing identity and the whole wire protocol stay the 1.55 ones; only
 * this presentation is new.
 */
@Composable
fun DeviceSyncScreen(
    viewModel: PlayerViewModel,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val manager = viewModel.desktopSyncManager ?: return
    val colors = MaterialTheme.colorScheme

    val paired by manager.paired.collectAsState()
    val pairCode by manager.pairCode.collectAsState()
    val connectionState by manager.connectionState.collectAsState()
    val lanAddress by manager.lanAddress.collectAsState()
    val peerDeviceName by manager.peerDeviceName.collectAsState()
    val peerDeviceId by manager.peerDeviceId.collectAsState()
    val logs by manager.logs.collectAsState()

    val syncVolume by viewModel.deviceSyncViviVolume.collectAsState()
    val autoResync by viewModel.deviceSyncAutoResync.collectAsState()
    val mode by viewModel.deviceSyncMode.collectAsState()
    val relayUrl by viewModel.deviceSyncRelayUrl.collectAsState()

    var pairingRequested by remember { mutableStateOf(false) }

    LaunchedEffect(autoResync) { manager.autoResync = autoResync }
    LaunchedEffect(paired) { if (paired) pairingRequested = false }

    val qrAddr = if (mode == "lan") lanAddress else relayUrl.ifBlank { SyncServer.DEFAULT_URL }
    val qrContent = if (qrAddr.isNotBlank() && pairCode.isNotBlank()) {
        "vivimusic://pair?addr=${URLEncoder.encode(qrAddr, "UTF-8")}&code=$pairCode"
    } else {
        qrAddr
    }

    Column(modifier = modifier.fillMaxSize()) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(start = 10.dp, top = 10.dp, end = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            AppTooltip("Back") {
                IconButton(onClick = onBack) {
                    Icon(
                        painter = painterResource(Res.drawable.ic_automirrored_rounded_arrow_back),
                        contentDescription = "Back",
                        tint = colors.onSurface,
                    )
                }
            }
            SettingsHeader(
                title = "Device Sync",
                subtitle = "Pair the desktop with the Android app",
                horizontalPadding = 0.dp,
                modifier = Modifier.weight(1f),
            )
        }

        BoxWithConstraints(
            modifier = Modifier.fillMaxWidth().weight(1f).padding(horizontal = 24.dp, vertical = 8.dp)
        ) {
            val sideBySide = maxWidth >= 760.dp
            if (sideBySide) {
                Row(
                    modifier = Modifier.fillMaxSize(),
                    horizontalArrangement = Arrangement.spacedBy(20.dp),
                ) {
                    IdentityCard(
                        viewModel = viewModel,
                        deviceId = manager.deviceId,
                        deviceName = manager.deviceName,
                        syncVolume = syncVolume,
                        autoResync = autoResync,
                        mode = mode,
                        relayUrl = relayUrl,
                        paired = paired,
                        onDisconnect = { manager.disconnect() },
                        modifier = Modifier.weight(1f).fillMaxHeight(),
                    )
                    PairingCard(
                        paired = paired,
                        pairCode = pairCode,
                        qrContent = qrContent,
                        pairingRequested = pairingRequested,
                        connectionState = connectionState,
                        peerDeviceName = peerDeviceName,
                        peerDeviceId = peerDeviceId,
                        desktopName = manager.deviceName,
                        desktopId = manager.deviceId,
                        logs = logs,
                        onGenerate = {
                            pairingRequested = true
                            manager.beginPairing(mode, relayUrl)
                        },
                        onReset = {
                            pairingRequested = false
                            manager.disconnect()
                        },
                        modifier = Modifier.weight(1f).fillMaxHeight(),
                    )
                }
            } else {
                Column(
                    modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(20.dp),
                ) {
                    IdentityCard(
                        viewModel = viewModel,
                        deviceId = manager.deviceId,
                        deviceName = manager.deviceName,
                        syncVolume = syncVolume,
                        autoResync = autoResync,
                        mode = mode,
                        relayUrl = relayUrl,
                        paired = paired,
                        onDisconnect = { manager.disconnect() },
                        modifier = Modifier.fillMaxWidth(),
                    )
                    PairingCard(
                        paired = paired,
                        pairCode = pairCode,
                        qrContent = qrContent,
                        pairingRequested = pairingRequested,
                        connectionState = connectionState,
                        peerDeviceName = peerDeviceName,
                        peerDeviceId = peerDeviceId,
                        desktopName = manager.deviceName,
                        desktopId = manager.deviceId,
                        logs = logs,
                        onGenerate = {
                            pairingRequested = true
                            manager.beginPairing(mode, relayUrl)
                        },
                        onReset = {
                            pairingRequested = false
                            manager.disconnect()
                        },
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
            }
        }
    }
}

@Composable
private fun IdentityCard(
    viewModel: PlayerViewModel,
    deviceId: String,
    deviceName: String,
    syncVolume: Boolean,
    autoResync: Boolean,
    mode: String,
    relayUrl: String,
    paired: Boolean,
    onDisconnect: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = MaterialTheme.colorScheme
    var relayDraft by remember(relayUrl) { mutableStateOf(relayUrl) }

    Surface(
        shape = RoundedCornerShape(28.dp),
        color = colors.surfaceContainer,
        modifier = modifier,
    ) {
        Column(
            modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(18.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier.size(52.dp).clip(CircleShape).background(colors.primaryContainer),
                ) {
                    Icon(
                        painter = painterResource(Res.drawable.ic_rounded_sync),
                        contentDescription = null,
                        tint = colors.onPrimaryContainer,
                        modifier = Modifier.size(26.dp),
                    )
                }
                Spacer(Modifier.width(14.dp))
                Column {
                    Text(
                        text = "Axolotl Music DE",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = colors.onSurface,
                    )
                    Text(
                        text = deviceName,
                        style = MaterialTheme.typography.bodyMedium,
                        color = colors.onSurfaceVariant,
                    )
                }
            }

            Text(
                text = "Device ID  ${deviceId.take(18)}",
                style = MaterialTheme.typography.bodySmall,
                fontFamily = FontFamily.Monospace,
                color = colors.onSurfaceVariant,
            )

            ExpressiveSwitchRow(
                title = "Sync volume",
                subtitle = "Keep the player volume aligned on both devices",
                checked = syncVolume,
                onCheckedChange = { viewModel.setDeviceSyncViviVolume(it) },
                leadingIcon = {
                    Icon(
                        painter = painterResource(Res.drawable.ic_rounded_computer),
                        contentDescription = null,
                        tint = colors.secondary,
                        modifier = Modifier.size(22.dp),
                    )
                },
                shape = getGroupShape(0, 2),
            )

            ExpressiveSwitchRow(
                title = "Automatic resync",
                subtitle = "Re-send the current state every few seconds",
                checked = autoResync,
                onCheckedChange = { viewModel.setDeviceSyncAutoResync(it) },
                leadingIcon = {
                    Icon(
                        painter = painterResource(Res.drawable.ic_rounded_sync),
                        contentDescription = null,
                        tint = colors.secondary,
                        modifier = Modifier.size(22.dp),
                    )
                },
                shape = getGroupShape(1, 2),
            )

            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = "CONNECTION",
                    style = MaterialTheme.typography.labelMedium,
                    color = colors.onSurfaceVariant,
                    fontWeight = FontWeight.SemiBold,
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilterChip(
                        selected = mode != "lan",
                        onClick = { viewModel.setDeviceSyncMode("relay") },
                        label = { Text("Relay") },
                    )
                    FilterChip(
                        selected = mode == "lan",
                        onClick = { viewModel.setDeviceSyncMode("lan") },
                        label = { Text("LAN") },
                    )
                }
            }

            if (mode != "lan") {
                OutlinedTextField(
                    value = relayDraft,
                    onValueChange = { relayDraft = it },
                    label = { Text("Relay URL") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
                TextButton(
                    onClick = { viewModel.setDeviceSyncRelayUrl(relayDraft) },
                    enabled = relayDraft.trim() != relayUrl,
                ) {
                    Text("Save relay URL")
                }
            } else {
                Text(
                    text = "The phone connects to this computer on the same Wi-Fi. " +
                        "The LAN relay starts when you generate the code.",
                    style = MaterialTheme.typography.bodySmall,
                    color = colors.onSurfaceVariant,
                )
            }

            if (paired) {
                TextButton(onClick = onDisconnect) {
                    Text("Disconnect")
                }
            }
        }
    }
}

@Composable
private fun PairingCard(
    paired: Boolean,
    pairCode: String,
    qrContent: String,
    pairingRequested: Boolean,
    connectionState: SyncConnectionState,
    peerDeviceName: String,
    peerDeviceId: String,
    desktopName: String,
    desktopId: String,
    logs: List<com.music.vivi.desktop.data.sync.SyncLogLine>,
    onGenerate: () -> Unit,
    onReset: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = MaterialTheme.colorScheme
    Surface(
        shape = RoundedCornerShape(28.dp),
        color = colors.surfaceContainerHigh,
        modifier = modifier,
    ) {
        Box(Modifier.fillMaxSize().padding(24.dp)) {
            when {
                paired -> PairedTerminal(
                    peerDeviceName = peerDeviceName,
                    peerDeviceId = peerDeviceId,
                    desktopName = desktopName,
                    desktopId = desktopId,
                    logs = logs,
                    onDisconnect = onReset,
                )
                pairingRequested -> PairingQr(
                    pairCode = pairCode,
                    qrContent = qrContent,
                    connectionState = connectionState,
                )
                // State 1: only the button, exactly as the mission asks.
                else -> Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center,
                ) {
                    AppTooltip("Generate QR Code") {
                        Button(onClick = onGenerate) {
                            Text("Genera QR Code")
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun PairingQr(
    pairCode: String,
    qrContent: String,
    connectionState: SyncConnectionState,
) {
    val colors = MaterialTheme.colorScheme
    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        if (pairCode.isBlank() || qrContent.isBlank()) {
            CircularProgressIndicator()
            Spacer(Modifier.height(14.dp))
            Text(
                text = if (connectionState == SyncConnectionState.ERROR) {
                    "Relay unreachable. Check the URL and try again."
                } else {
                    "Generating pairing code…"
                },
                style = MaterialTheme.typography.bodyMedium,
                color = colors.onSurfaceVariant,
            )
        } else {
            QrCode(text = qrContent, size = 220.dp)
            Spacer(Modifier.height(16.dp))
            Text(
                text = pairCode,
                style = MaterialTheme.typography.headlineSmall,
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.Bold,
                color = colors.onSurface,
            )
            Text(
                text = "Scan with the Android app",
                style = MaterialTheme.typography.bodyMedium,
                color = colors.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun PairedTerminal(
    peerDeviceName: String,
    peerDeviceId: String,
    desktopName: String,
    desktopId: String,
    logs: List<com.music.vivi.desktop.data.sync.SyncLogLine>,
    onDisconnect: () -> Unit,
) {
    val colors = MaterialTheme.colorScheme
    Column(modifier = Modifier.fillMaxSize(), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        DeviceLine(
            name = desktopName,
            id = desktopId,
            isDesktop = true,
            connected = true,
        )
        DeviceLine(
            name = peerDeviceName.ifBlank { "APK Android" },
            id = peerDeviceId,
            isDesktop = false,
            connected = true,
        )

        Surface(
            shape = RoundedCornerShape(16.dp),
            color = colors.surfaceContainerLowest,
            modifier = Modifier.fillMaxWidth().weight(1f),
        ) {
            if (logs.isEmpty()) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text(
                        text = "Waiting for the first packet…",
                        style = MaterialTheme.typography.bodySmall,
                        color = colors.onSurfaceVariant,
                    )
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize().padding(12.dp),
                    verticalArrangement = Arrangement.spacedBy(3.dp),
                ) {
                    items(logs) { line ->
                        Text(
                            text = line.text,
                            style = MaterialTheme.typography.bodySmall,
                            fontFamily = FontFamily.Monospace,
                            color = if (line.text.contains(" ERR ")) colors.error else colors.onSurface,
                        )
                    }
                }
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.End,
        ) {
            TextButton(onClick = onDisconnect) { Text("Disconnect") }
        }
    }
}

@Composable
private fun DeviceLine(
    name: String,
    id: String,
    isDesktop: Boolean,
    connected: Boolean,
) {
    val colors = MaterialTheme.colorScheme
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            painter = painterResource(
                if (isDesktop) Res.drawable.ic_rounded_computer else Res.drawable.ic_rounded_phone_android
            ),
            contentDescription = null,
            tint = if (connected) colors.primary else colors.onSurfaceVariant,
            modifier = Modifier.size(22.dp),
        )
        Spacer(Modifier.width(10.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = name,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
                color = colors.onSurface,
            )
            Text(
                text = if (id.isBlank()) "waiting for the device id" else id.take(24),
                style = MaterialTheme.typography.bodySmall,
                fontFamily = FontFamily.Monospace,
                color = colors.onSurfaceVariant,
            )
        }
        Box(
            modifier = Modifier.size(8.dp).clip(CircleShape).background(
                if (connected) colors.primary else colors.outline
            )
        )
    }
}
