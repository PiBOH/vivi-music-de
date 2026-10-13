/**
 * vivimusic Project (C) 2026
 * Licensed under GPL-3.0 | See git history for contributors
 */

package com.music.vivi.ui.screens.settings

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.navigation.NavController
import com.journeyapps.barcodescanner.ScanContract
import com.journeyapps.barcodescanner.ScanIntentResult
import com.journeyapps.barcodescanner.ScanOptions
import com.music.vivi.LocalPlayerAwareWindowInsets
import com.music.vivi.R
import com.music.vivi.constants.DeviceSyncServerUrlKey
import com.music.vivi.constants.SyncNativeVolumeKey
import com.music.vivi.constants.SyncViviVolumeKey
import com.music.vivi.devicesync.LanDiscovery
import com.music.vivi.sync.SyncServer
import com.music.vivi.ui.component.ExpressiveSettingGroup
import com.music.vivi.ui.component.IconButton
import com.music.vivi.ui.component.Material3SettingsItem
import com.music.vivi.ui.utils.backToMain
import com.music.vivi.utils.dataStore
import com.music.vivi.utils.get
import com.music.vivi.utils.rememberPreference
import com.music.vivi.viewmodels.DeviceSyncViewModel

/**
 * Device sync, as cards.
 *
 * The screen is the phone's end of the pairing, so it says three things, one
 * card each: **where it stands** (paired or not, to which computer, and the
 * live status line the relay answers with), **how it connects** (the relay
 * address, the LAN discovery and the QR scan, or the six-digit code from the
 * computer), and **what travels with the music** (the two volume channels -
 * the in-app slider and the operating system's own volume - which are the
 * switches the sync actually reads, see `MusicService`).
 *
 * Everything is a Material 3 surface with the project's expressive group for
 * the switches, so the screen matches the rest of Settings instead of the
 * column of loose rows it used to be.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DeviceSyncScreen(
    navController: NavController,
    scrollBehavior: TopAppBarScrollBehavior,
) {
    val viewModel: DeviceSyncViewModel = hiltViewModel()
    val paired by viewModel.paired.collectAsState()
    val status by viewModel.status.collectAsState()
    val peerDeviceName by viewModel.peerDeviceName.collectAsState()

    val (syncViviVolume, onSyncViviVolumeChange) = rememberPreference(SyncViviVolumeKey, true)
    val (syncNativeVolume, onSyncNativeVolumeChange) = rememberPreference(SyncNativeVolumeKey, true)
    var joinCode by remember { mutableStateOf("") }

    val context = LocalContext.current

    // Relay server URL: held as local state so editing is instant and clearing
    // actually sticks; persisted through the app-lifetime manager scope (a
    // screen-scoped coroutine is cancelled on exit, which restored the
    // last-used URL). A blank stored value means "not set": when not paired,
    // leaving and re-entering this screen always shows the default cloud relay.
    val savedServerUrl = remember { context.dataStore[DeviceSyncServerUrlKey] ?: "" }
    var serverUrl by remember { mutableStateOf(savedServerUrl.ifBlank { SyncServer.DEFAULT_URL }) }
    fun persistServerUrl(value: String) {
        serverUrl = value
        viewModel.saveServerUrl(value)
    }

    val lanDiscovery = remember { LanDiscovery(context) }
    var discovering by remember { mutableStateOf(false) }

    DisposableEffect(Unit) {
        onDispose { lanDiscovery.stop() }
    }

    val scanLauncher = rememberLauncherForActivityResult(ScanContract()) { result: ScanIntentResult ->
        result.contents?.let { scanned ->
            val trimmed = scanned.trim()
            val uri = android.net.Uri.parse(trimmed)
            // New desktop QR: vivimusic://pair?addr=<relay>&code=<6-digit>.
            // Both fields are filled AND the pairing starts right away: the QR
            // carries everything, so requiring a separate confirmation tap made
            // a good scan look like the code had not been recognized. Plain
            // ws:// URLs keep working (they only set the relay address).
            if (uri.scheme == "vivimusic" && uri.host == "pair") {
                val addr = uri.getQueryParameter("addr")
                val code = uri.getQueryParameter("code")?.filter { it.isDigit() }?.take(6).orEmpty()
                addr?.let { persistServerUrl(it) }
                joinCode = code
                if (code.length == 6) viewModel.joinPairFromScan(addr, code)
            } else {
                persistServerUrl(trimmed)
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {},
                navigationIcon = {
                    IconButton(
                        onClick = navController::navigateUp,
                        onLongClick = navController::backToMain,
                    ) {
                        Icon(
                            painterResource(R.drawable.arrow_back),
                            contentDescription = null,
                        )
                    }
                },
                scrollBehavior = scrollBehavior,
            )
        },
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .windowInsetsPadding(
                    LocalPlayerAwareWindowInsets.current.only(WindowInsetsSides.Horizontal + WindowInsetsSides.Bottom)
                )
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(
                text = stringResource(R.string.device_sync),
                style = MaterialTheme.typography.headlineLarge,
                color = MaterialTheme.colorScheme.onBackground,
                modifier = Modifier.padding(top = 12.dp),
            )

            Text(
                text = stringResource(R.string.device_sync_desc),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )

            // ---- Where the pairing stands -------------------------------------
            // The state card: one glance answers "is this phone connected, and
            // to what", which the old screen asked the user to reconstruct from
            // two loose labels and a colour.
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = if (paired) {
                        MaterialTheme.colorScheme.primaryContainer
                    } else {
                        MaterialTheme.colorScheme.surfaceContainerHigh
                    },
                ),
            ) {
                Column(Modifier.fillMaxWidth().padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = if (paired) {
                                stringResource(R.string.device_sync_paired)
                            } else {
                                stringResource(R.string.device_sync_not_paired)
                            },
                            style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.SemiBold),
                            color = if (paired) {
                                MaterialTheme.colorScheme.onPrimaryContainer
                            } else {
                                MaterialTheme.colorScheme.onSurface
                            },
                            modifier = Modifier.weight(1f),
                        )
                        if (paired) {
                            Badge(
                                containerColor = MaterialTheme.colorScheme.primary,
                                contentColor = MaterialTheme.colorScheme.onPrimary,
                            ) {
                                Text(stringResource(R.string.device_sync_paired))
                            }
                        }
                    }

                    if (paired && peerDeviceName.isNotBlank()) {
                        Text(
                            text = "${stringResource(R.string.device_sync_paired_device)}: $peerDeviceName",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(top = 6.dp),
                        )
                    }

                    if (status.isNotBlank()) {
                        Text(
                            text = status,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(top = 6.dp),
                        )
                    }

                    if (paired) {
                        Spacer(Modifier.height(12.dp))
                        OutlinedButton(
                            onClick = { viewModel.unpair() },
                            modifier = Modifier.fillMaxWidth(),
                        ) {
                            Text(stringResource(R.string.device_sync_unpair))
                        }
                    }
                }
            }

            // ---- How it connects ----------------------------------------------
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceContainer,
                ),
            ) {
                Column(Modifier.fillMaxWidth().padding(16.dp)) {
                    Text(
                        text = stringResource(R.string.device_sync_server),
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
                        color = MaterialTheme.colorScheme.onSurface,
                    )

                    OutlinedTextField(
                        value = serverUrl,
                        onValueChange = { persistServerUrl(it) },
                        modifier = Modifier.fillMaxWidth().padding(top = 10.dp),
                        singleLine = true,
                        label = { Text(stringResource(R.string.device_sync_server)) },
                    )

                    Spacer(Modifier.height(12.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        FilledTonalButton(
                            onClick = {
                                discovering = true
                                lanDiscovery.discover(
                                    onFound = { url ->
                                        discovering = false
                                        persistServerUrl(url)
                                    },
                                    onError = { discovering = false },
                                )
                            },
                            enabled = !discovering,
                            modifier = Modifier.weight(1f),
                        ) {
                            if (discovering) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(16.dp),
                                    strokeWidth = 2.dp,
                                )
                                Spacer(Modifier.width(8.dp))
                            }
                            Text(stringResource(R.string.device_sync_discover))
                        }
                        FilledTonalButton(
                            onClick = {
                                // If we're already paired/connected, drop that
                                // first so scanning a new QR starts from a clean
                                // slate and can pair to a (possibly different)
                                // desktop.
                                viewModel.unpair()
                                scanLauncher.launch(
                                    ScanOptions()
                                        .setDesiredBarcodeFormats(ScanOptions.QR_CODE)
                                        .setPrompt(context.getString(R.string.device_sync_scan_qr))
                                        .setBeepEnabled(false)
                                        // Not locked to portrait: the code then
                                        // decodes whichever way the phone is
                                        // held, and the preview resolution stays
                                        // at its best instead of being rotated
                                        // into a smaller buffer (which is what
                                        // made locking onto the desktop QR take
                                        // several attempts).
                                        .setOrientationLocked(false)
                                        .setBarcodeImageEnabled(false)
                                )
                            },
                            modifier = Modifier.weight(1f),
                        ) {
                            Text(stringResource(R.string.device_sync_scan_qr))
                        }
                    }

                    // The six-digit code is only useful while there is nothing
                    // to unpair: once paired the card shows the Unpair action
                    // instead, so the two states never offer opposite actions
                    // at the same time.
                    if (!paired) {
                        Spacer(Modifier.height(12.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            OutlinedTextField(
                                value = joinCode,
                                onValueChange = { raw -> joinCode = raw.filter { it.isDigit() }.take(6) },
                                modifier = Modifier.weight(1f),
                                singleLine = true,
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                label = { Text(stringResource(R.string.device_sync_join_code)) },
                            )
                            Button(
                                onClick = { viewModel.joinPair(joinCode.trim()) },
                                enabled = joinCode.isNotBlank(),
                            ) {
                                Text(stringResource(R.string.device_sync_pair))
                            }
                        }
                    }
                }
            }

            // ---- What travels with the music -----------------------------------
            // The two volume channels the sync reads: the in-app slider and the
            // operating system's own volume. They are separate switches on
            // purpose (each is its own data-store key), so a phone can mirror
            // the player volume without touching the system one, or the other
            // way round.
            ExpressiveSettingGroup(
                title = stringResource(R.string.volume),
                items = listOf(
                    Material3SettingsItem(
                        title = { Text(stringResource(R.string.sync_vivi_volume)) },
                        description = { Text(stringResource(R.string.sync_vivi_volume_desc)) },
                        trailingContent = {
                            Switch(
                                checked = syncViviVolume,
                                onCheckedChange = onSyncViviVolumeChange,
                            )
                        },
                    ),
                    Material3SettingsItem(
                        title = { Text(stringResource(R.string.sync_native_volume)) },
                        description = { Text(stringResource(R.string.sync_native_volume_desc)) },
                        trailingContent = {
                            Switch(
                                checked = syncNativeVolume,
                                onCheckedChange = onSyncNativeVolumeChange,
                            )
                        },
                    ),
                ),
            )

            Spacer(Modifier.height(50.dp))
        }
    }
}
