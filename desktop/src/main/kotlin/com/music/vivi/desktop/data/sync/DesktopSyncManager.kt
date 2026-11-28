package com.music.vivi.desktop.data.sync

import com.music.vivi.sync.LibrarySnapshot
import com.music.vivi.sync.PlaybackSnapshot
import com.music.vivi.sync.SyncClient
import com.music.vivi.sync.SyncConnectionState
import com.music.vivi.sync.SyncEvent
import com.music.vivi.sync.SyncSnapshot
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull

/** One line of the live terminal on the Device Sync screen. */
data class SyncLogLine(val at: Long, val text: String)

/**
 * Persistent device-sync bridge for the desktop edition, carried over from
 * 1.55.0.
 *
 * The [SyncClient] (cloud relay) and the [LanSyncRelay] (same Wi-Fi) are
 * unchanged: this class still owns the pairing state, pushes playback
 * snapshots, applies incoming ones and suppresses the echo of a remote apply.
 * Only persistence moved from the old `DesktopSettings` object to
 * [SyncIdentityStore], which reads and writes the same flat keys at the root of
 * `device-sync.json`.
 */
class DesktopSyncManager {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val relay = LanSyncRelay()
    private val identity = SyncIdentityStore.load()

    val deviceId: String = identity.deviceId
    val deviceName: String = identity.deviceName

    private val _connectionState = MutableStateFlow(SyncConnectionState.DISCONNECTED)
    val connectionState: StateFlow<SyncConnectionState> = _connectionState.asStateFlow()

    private val _status = MutableStateFlow("")
    val status: StateFlow<String> = _status.asStateFlow()

    private val _pairCode = MutableStateFlow("")
    val pairCode: StateFlow<String> = _pairCode.asStateFlow()

    /** Epoch millis when the current pairing code expires (0 = no active code). */
    private val _pairCodeExpiresAt = MutableStateFlow(0L)
    val pairCodeExpiresAt: StateFlow<Long> = _pairCodeExpiresAt.asStateFlow()

    private val _paired = MutableStateFlow(false)
    val paired: StateFlow<Boolean> = _paired.asStateFlow()

    /** Name/model of the paired peer device (e.g. the phone's manufacturer + model). */
    private val _peerDeviceName = MutableStateFlow("")
    val peerDeviceName: StateFlow<String> = _peerDeviceName.asStateFlow()

    private val _peerDeviceId = MutableStateFlow("")
    val peerDeviceId: StateFlow<String> = _peerDeviceId.asStateFlow()

    private val _lanRunning = MutableStateFlow(false)
    val lanRunning: StateFlow<Boolean> = _lanRunning.asStateFlow()

    private val _lanAddress = MutableStateFlow("")
    val lanAddress: StateFlow<String> = _lanAddress.asStateFlow()

    private val _syncedSettings = MutableStateFlow<Map<String, String>>(emptyMap())
    val syncedSettings: StateFlow<Map<String, String>> = _syncedSettings.asStateFlow()

    private val _logs = MutableStateFlow<List<SyncLogLine>>(emptyList())
    val logs: StateFlow<List<SyncLogLine>> = _logs.asStateFlow()

    /** Incoming playback snapshots to apply to the player. */
    private val _incomingPlayback = MutableSharedFlow<PlaybackSnapshot>(extraBufferCapacity = 8)
    val incomingPlayback: SharedFlow<PlaybackSnapshot> = _incomingPlayback.asSharedFlow()

    /** Incoming settings snapshots to apply to the theme/language layer. */
    private val _incomingSettings = MutableSharedFlow<Map<String, String>>(extraBufferCapacity = 8)
    val incomingSettings: SharedFlow<Map<String, String>> = _incomingSettings.asSharedFlow()

    /** Incoming library snapshots to apply to the local library. */
    private val _incomingLibrary = MutableSharedFlow<LibrarySnapshot?>(extraBufferCapacity = 8)
    val incomingLibrary: SharedFlow<LibrarySnapshot?> = _incomingLibrary.asSharedFlow()

    private val _syncedLibrary = MutableStateFlow<LibrarySnapshot?>(null)
    val syncedLibrary: StateFlow<LibrarySnapshot?> = _syncedLibrary.asStateFlow()

    private var client: SyncClient? = null

    private var lastPlayback: PlaybackSnapshot? = null
    private var lastSettings: Map<String, String> = emptyMap()
    private var lastLibrary: LibrarySnapshot? = null

    /** Queue fingerprint + last-write-wins timestamp (shared relay-time frame). */
    private var lastQueueFingerprint: String = ""
    private var queueUpdatedAt: Long = 0L

    @Volatile
    private var suppressPushUntil = 0L

    /** Resolving state of the last snapshot we actually sent. */
    private var lastPushedResolving: Boolean? = null

    /** A user seek is still waiting to leave this device. */
    private var userSeekPending = false

    /** Periodic re-sync; the Device Sync card's Automatic resync switch. */
    @Volatile
    var autoResync: Boolean = true
        set(value) {
            field = value
            log("CFG", "automatic resync = $value")
        }

    init {
        lastLibrary = null
        Runtime.getRuntime().addShutdownHook(Thread { runCatching { relay.shutdownNotify() } })
        scope.launch {
            while (isActive) {
                delay(5_000L)
                if (autoResync && _paired.value && _connectionState.value == SyncConnectionState.CONNECTED) {
                    pushSnapshot()
                }
            }
        }
    }

    // ---------------------------------------------------------------------
    // Public API
    // ---------------------------------------------------------------------

    fun connect(serverUrl: String) {
        val url = serverUrl.trim()
        if (url.isEmpty()) return
        teardownClient()
        val created = SyncClient(
            serverUrl = url,
            deviceId = deviceId,
            deviceName = deviceName,
        )
        client = created
        log("NET", "connecting to $url")
        scope.launch { created.connectionState.collect { _connectionState.value = it } }
        scope.launch { created.events.collect { handleEvent(it) } }
        created.connect()
        if (!url.startsWith("ws://localhost")) {
            SyncIdentityStore.update { it.copy(serverUrl = url) }
        }
    }

    /**
     * Starts the local LAN relay, connects the desktop to it, and generates a
     * pairing code so the phone can pair right away.
     */
    fun startLan() {
        scope.launch {
            try {
                val port = relay.start()
                _lanRunning.value = true
                _lanAddress.value = "ws://${lanIpAddress()}:$port"
                log("LAN", "relay listening on ${_lanAddress.value}")
                connect("ws://localhost:$port")
                val c = client ?: return@launch
                repeat(5) {
                    if (_pairCode.value.isNotEmpty()) return@launch
                    withTimeoutOrNull(3_000L) {
                        c.connectionState.first { it == SyncConnectionState.CONNECTED }
                    }
                    if (c.connectionState.value == SyncConnectionState.CONNECTED) {
                        requestPairingCode()
                    }
                    delay(1_000L)
                }
            } catch (e: Exception) {
                _status.value = "LAN error: ${e.message}"
                log("LAN", "error: ${e.message}")
                _lanRunning.value = false
                _lanAddress.value = ""
            }
        }
    }

    fun stopLan() {
        scope.launch { runCatching { relay.stop() } }
        _lanRunning.value = false
        _lanAddress.value = ""
        teardownClient()
        clearPairing()
    }

    /**
     * The one action behind "Genera QR Code": pick the transport, connect and
     * ask the relay for a code. The retry is the 1.55 one, so a relay that is
     * still waking up does not leave the screen without a code.
     */
    fun beginPairing(mode: String, relayUrl: String) {
        log("PAIR", "begin pairing over ${if (mode == "lan") "LAN" else "relay"}")
        if (mode == "lan") {
            if (!_lanRunning.value) startLan()
            return
        }
        connect(relayUrl.ifBlank { com.music.vivi.sync.SyncServer.DEFAULT_URL })
        scope.launch {
            repeat(10) {
                if (_pairCode.value.isNotEmpty() || _paired.value) return@launch
                if (_connectionState.value == SyncConnectionState.CONNECTED) {
                    requestPairingCode()
                }
                delay(1_000L)
            }
        }
    }

    fun requestPairingCode() {
        ensureConnected()
        log("PAIR", "requesting a pairing code")
        client?.requestPairingCode()
    }

    fun joinPair(code: String) {
        ensureConnected()
        client?.joinPair(code)
    }

    fun disconnect() {
        runCatching { client?.unpair() }
        teardownClient()
        clearPairing()
        _status.value = ""
        log("NET", "disconnected")
    }

    fun unpair() {
        client?.unpair()
        clearPairing()
        log("PAIR", "pairing cleared")
    }

    fun shutdown() {
        runCatching { client?.disconnect() }
        runCatching { relay.shutdownNotify() }
    }

    private fun clearPairing() {
        SyncIdentityStore.update { it.copy(pairId = "") }
        _paired.value = false
        _pairCode.value = ""
        _pairCodeExpiresAt.value = 0L
        _peerDeviceName.value = ""
        _peerDeviceId.value = ""
    }

    /**
     * Update the local playback snapshot and push it to the peer (if paired).
     *
     * @return true if the snapshot was actually sent this call.
     */
    fun updatePlayback(playback: PlaybackSnapshot?): Boolean {
        if (playback?.userSeek == true) userSeekPending = true
        lastPlayback = playback?.let { p ->
            val withSeek = if (userSeekPending) p.copy(userSeek = true) else p
            val stamp = withSeek.positionAtMs == 0L && client?.hasServerOffset == true
            val snap = if (stamp) withSeek.copy(positionAtMs = serverNowMs()) else withSeek
            val fp = queueFingerprint(snap)
            if (fp.isNotEmpty() && fp != lastQueueFingerprint) {
                lastQueueFingerprint = fp
                queueUpdatedAt = serverNowMs()
            }
            snap.copy(queueUpdatedAt = queueUpdatedAt)
        }
        val resolving = lastPlayback?.isResolving
        val force = userSeekPending || resolving != lastPushedResolving
        val sent = pushSnapshot(force = force)
        if (sent) userSeekPending = false
        return sent
    }

    /** Last-write-wins timestamp of the local queue (relay frame); 0 = none. */
    fun queueUpdatedAt(): Long = queueUpdatedAt

    /** Adopts the remote queue's fingerprint + timestamp right after applying it. */
    fun noteQueueApplied(snapshot: PlaybackSnapshot) {
        val fp = queueFingerprint(snapshot)
        if (fp.isNotEmpty()) {
            lastQueueFingerprint = fp
            if (snapshot.queueUpdatedAt > 0L) queueUpdatedAt = snapshot.queueUpdatedAt
        }
    }

    private fun queueFingerprint(p: PlaybackSnapshot): String =
        if (p.queue.isEmpty()) "" else p.queue.joinToString("|") { it.id } + "@" + p.queueIndex

    fun updateSettings(settings: Map<String, String>) {
        lastSettings = settings
        pushSnapshot()
    }

    fun updateLibrary(library: LibrarySnapshot?) {
        lastLibrary = library
        pushSnapshot()
    }

    val serverOffsetMs: Long get() = client?.serverOffsetMs ?: 0L

    fun serverNowMs(): Long = System.currentTimeMillis() + serverOffsetMs

    fun effectivePosition(snapshot: PlaybackSnapshot): Long {
        val base = snapshot.positionMs.coerceAtLeast(0L)
        val at = snapshot.positionAtMs
        if (at <= 0L || !snapshot.isPlaying || snapshot.isResolving) return base
        val elapsed = (serverNowMs() - at).coerceAtLeast(0L)
        return (base + elapsed).coerceAtLeast(0L)
    }

    fun snapshotAgeMs(snapshot: PlaybackSnapshot): Long? =
        if (snapshot.positionAtMs > 0L) (serverNowMs() - snapshot.positionAtMs).coerceAtLeast(0L) else null

    // ---------------------------------------------------------------------
    // Internals
    // ---------------------------------------------------------------------

    private fun ensureConnected() {
        val c = client
        if (c == null) {
            val saved = SyncIdentityStore.load().serverUrl
            if (saved.isNotBlank()) connect(saved)
            return
        }
        if (c.connectionState.value != SyncConnectionState.CONNECTED) c.connect()
    }

    private fun teardownClient() {
        client?.disconnect()
        client = null
        _connectionState.value = SyncConnectionState.DISCONNECTED
    }

    private fun pushSnapshot(force: Boolean = false): Boolean {
        if (!force && System.currentTimeMillis() < suppressPushUntil) return false
        val c = client ?: return false
        if (c.connectionState.value != SyncConnectionState.CONNECTED) return false
        if (!_paired.value) return false
        lastPlayback?.let { p ->
            val kind = when {
                p.userSeek -> "USER SEEK"
                p.isResolving -> "resolving"
                else -> "tick/state"
            }
            log(
                "TX",
                "sync_push [$kind] track='${p.trackTitle ?: p.trackId}' queue=${p.queue.size} " +
                    "index=${p.queueIndex} playing=${p.isPlaying} pos=${p.positionMs}ms",
            )
        }
        c.pushSnapshot(
            SyncSnapshot(
                deviceId = c.deviceId,
                deviceName = deviceName,
                updatedAt = System.currentTimeMillis(),
                settings = lastSettings,
                playback = lastPlayback,
                library = lastLibrary,
            )
        )
        lastPushedResolving = lastPlayback?.isResolving
        return true
    }

    private fun handleEvent(event: SyncEvent) {
        when (event) {
            is SyncEvent.Connected -> {
                _status.value = "Connected"
                log("NET", "relay connected")
                if (SyncIdentityStore.load().pairId.isNotEmpty()) client?.pullSnapshot()
            }
            is SyncEvent.Disconnected -> {
                _status.value = "Disconnected"
                log("NET", "relay disconnected")
            }
            is SyncEvent.PairCode -> {
                _pairCode.value = event.code
                _pairCodeExpiresAt.value = System.currentTimeMillis() + PAIR_CODE_TTL_MS
                _status.value = event.code
                log("RX", "pair_code ${event.code} (5 min)")
            }
            is SyncEvent.Paired -> {
                _paired.value = true
                _pairCode.value = ""
                _pairCodeExpiresAt.value = 0L
                _peerDeviceName.value = event.peerDeviceName.ifBlank { "APK Android" }
                _peerDeviceId.value = event.peerDeviceId
                SyncIdentityStore.update { it.copy(pairId = event.pairId) }
                _status.value = "Paired with ${_peerDeviceName.value}"
                log("PAIR", "pair_joined peer='${_peerDeviceName.value}' (${event.peerDeviceId})")
                pushSnapshot()
            }
            is SyncEvent.NoSnapshot -> {
                _paired.value = true
                _status.value = "Paired"
                log("PAIR", "reconnected as paired (no mailbox snapshot yet)")
            }
            is SyncEvent.SnapshotReceived -> {
                _paired.value = true
                event.snapshot.deviceName.takeIf { it.isNotBlank() }?.let { _peerDeviceName.value = it }
                suppressPushUntil = System.currentTimeMillis() + ECHO_SUPPRESS_MS
                _status.value = "Snapshot received"
                event.snapshot.playback?.let { p ->
                    log(
                        "RX",
                        "sync from '${event.snapshot.deviceName}': track='${p.trackTitle ?: p.trackId}' " +
                            "queue=${p.queue.size} index=${p.queueIndex} playing=${p.isPlaying} " +
                            "pos=${p.positionMs}ms seek=${p.userSeek}",
                    )
                } ?: log("RX", "sync from '${event.snapshot.deviceName}' (no playback)")
                if (event.snapshot.settings.isNotEmpty()) {
                    _syncedSettings.value = event.snapshot.settings
                    scope.launch { _incomingSettings.emit(event.snapshot.settings) }
                }
                event.snapshot.playback?.let { pb ->
                    scope.launch { _incomingPlayback.emit(pb) }
                }
                event.snapshot.library?.let { lib ->
                    _syncedLibrary.value = lib
                    scope.launch { _incomingLibrary.emit(lib) }
                }
            }
            is SyncEvent.Error -> {
                _status.value = event.message
                if (event.message.contains("unpaired", ignoreCase = true) ||
                    event.message.contains("not paired", ignoreCase = true)
                ) {
                    log("PAIR", "peer unpaired us — dropping the local pairing")
                    _paired.value = false
                    _pairCode.value = ""
                    _pairCodeExpiresAt.value = 0L
                    _peerDeviceName.value = ""
                    _peerDeviceId.value = ""
                    SyncIdentityStore.update { it.copy(pairId = "") }
                } else {
                    log("ERR", event.message)
                }
            }
        }
    }

    private fun log(direction: String, message: String) {
        val time = SimpleDateFormat("HH:mm:ss.SSS", Locale.getDefault()).format(Date())
        val line = SyncLogLine(System.currentTimeMillis(), "$time  $direction  $message")
        _logs.update { current ->
            (current + line).takeLast(MAX_LOG_LINES)
        }
    }

    companion object {
        private const val ECHO_SUPPRESS_MS = 1500L
        private const val MAX_LOG_LINES = 400

        /** Pairing codes are valid for 5 minutes (matches the relay servers). */
        const val PAIR_CODE_TTL_MS = 5 * 60 * 1000L
    }
}
