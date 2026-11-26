package com.music.vivi.desktop.data.listentogether

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.decodeFromJsonElement
import kotlinx.serialization.json.encodeToJsonElement
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.put
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import okhttp3.WebSocket
import okhttp3.WebSocketListener
import java.util.concurrent.ConcurrentLinkedQueue
import java.util.concurrent.TimeUnit

// ---------------------------------------------------------------------------
// Wire protocol: the same JSON the mobile client and the public relay speak.
// ---------------------------------------------------------------------------

object LtMessageTypes {
    const val CREATE_ROOM = "create_room"
    const val JOIN_ROOM = "join_room"
    const val LEAVE_ROOM = "leave_room"
    const val APPROVE_JOIN = "approve_join"
    const val REJECT_JOIN = "reject_join"
    const val PLAYBACK_ACTION = "playback_action"
    const val KICK_USER = "kick_user"
    const val TRANSFER_HOST = "transfer_host"
    const val PING = "ping"
    const val REQUEST_SYNC = "request_sync"
    const val RECONNECT = "reconnect"

    const val ROOM_CREATED = "room_created"
    const val JOIN_REQUEST = "join_request"
    const val JOIN_APPROVED = "join_approved"
    const val JOIN_REJECTED = "join_rejected"
    const val USER_JOINED = "user_joined"
    const val USER_LEFT = "user_left"
    const val SYNC_PLAYBACK = "sync_playback"
    const val ERROR = "error"
    const val PONG = "pong"
    const val HOST_CHANGED = "host_changed"
    const val KICKED = "kicked"
    const val SYNC_STATE = "sync_state"
    const val RECONNECTED = "reconnected"
    const val USER_RECONNECTED = "user_reconnected"
    const val USER_DISCONNECTED = "user_disconnected"
}

object LtPlaybackActions {
    const val PLAY = "play"
    const val PAUSE = "pause"
    const val SEEK = "seek"
    const val SKIP_NEXT = "skip_next"
    const val SKIP_PREV = "skip_prev"
    const val CHANGE_TRACK = "change_track"
    const val QUEUE_ADD = "queue_add"
    const val QUEUE_REMOVE = "queue_remove"
    const val QUEUE_CLEAR = "queue_clear"
    const val SYNC_QUEUE = "sync_queue"
    const val SET_VOLUME = "set_volume"
}

@Serializable
data class LtMessage(val type: String, val payload: JsonElement? = null)

@Serializable
data class LtTrackInfo(
    val id: String,
    val title: String,
    val artist: String,
    val album: String? = null,
    val duration: Long = 0L,
    val thumbnail: String? = null
)

@Serializable
data class LtUserInfo(
    @SerialName("user_id") val userId: String,
    val username: String,
    @SerialName("is_host") val isHost: Boolean = false,
    @SerialName("is_connected") val isConnected: Boolean = true,
    @SerialName("avatar_index") val avatarIndex: Int = 0
)

@Serializable
data class LtRoomState(
    @SerialName("room_code") val roomCode: String,
    @SerialName("host_id") val hostId: String,
    val users: List<LtUserInfo> = emptyList(),
    @SerialName("current_track") val currentTrack: LtTrackInfo? = null,
    @SerialName("is_playing") val isPlaying: Boolean = false,
    val position: Long = 0L,
    @SerialName("last_update") val lastUpdate: Long = 0L,
    val volume: Float = 1f,
    val queue: List<LtTrackInfo> = emptyList()
)

@Serializable
data class LtCreateRoom(val username: String, @SerialName("avatar_index") val avatarIndex: Int = 0)

@Serializable
data class LtJoinRoom(
    @SerialName("room_code") val roomCode: String,
    val username: String,
    @SerialName("avatar_index") val avatarIndex: Int = 0
)

@Serializable
data class LtApproveJoin(@SerialName("user_id") val userId: String)

@Serializable
data class LtRejectJoin(@SerialName("user_id") val userId: String, val reason: String? = null)

@Serializable
data class LtPlaybackAction(
    val action: String,
    @SerialName("track_id") val trackId: String? = null,
    val position: Long? = null,
    @SerialName("track_info") val trackInfo: LtTrackInfo? = null,
    @SerialName("insert_next") val insertNext: Boolean? = null,
    val queue: List<LtTrackInfo>? = null,
    @SerialName("queue_title") val queueTitle: String? = null,
    val volume: Float? = null,
    @SerialName("server_time") val serverTime: Long? = null
)

@Serializable
data class LtKickUser(@SerialName("user_id") val userId: String, val reason: String? = null)

@Serializable
data class LtTransferHost(@SerialName("new_host_id") val newHostId: String)

@Serializable
data class LtReconnect(@SerialName("session_token") val sessionToken: String)

@Serializable
data class LtRoomCreated(
    @SerialName("room_code") val roomCode: String,
    @SerialName("user_id") val userId: String,
    @SerialName("session_token") val sessionToken: String
)

@Serializable
data class LtJoinRequest(@SerialName("user_id") val userId: String, val username: String)

@Serializable
data class LtJoinApproved(
    @SerialName("room_code") val roomCode: String,
    @SerialName("user_id") val userId: String,
    @SerialName("session_token") val sessionToken: String,
    val state: LtRoomState
)

@Serializable
data class LtJoinRejected(val reason: String)

@Serializable
data class LtUserJoined(
    @SerialName("user_id") val userId: String,
    val username: String,
    @SerialName("avatar_index") val avatarIndex: Int = 0
)

@Serializable
data class LtUserLeft(@SerialName("user_id") val userId: String, val username: String)

@Serializable
data class LtError(val code: String, val message: String)

@Serializable
data class LtHostChanged(
    @SerialName("new_host_id") val newHostId: String,
    @SerialName("new_host_name") val newHostName: String
)

@Serializable
data class LtKicked(val reason: String)

@Serializable
data class LtSyncState(
    @SerialName("current_track") val currentTrack: LtTrackInfo? = null,
    @SerialName("is_playing") val isPlaying: Boolean = false,
    val position: Long = 0L,
    @SerialName("last_update") val lastUpdate: Long = 0L,
    val queue: List<LtTrackInfo>? = null,
    val volume: Float? = null
)

@Serializable
data class LtReconnected(
    @SerialName("room_code") val roomCode: String,
    @SerialName("user_id") val userId: String,
    val state: LtRoomState,
    @SerialName("is_host") val isHost: Boolean
)

@Serializable
data class LtUserReconnected(@SerialName("user_id") val userId: String, val username: String)

@Serializable
data class LtUserDisconnected(@SerialName("user_id") val userId: String, val username: String)

enum class LtConnectionState { DISCONNECTED, CONNECTING, CONNECTED, RECONNECTING, ERROR }

enum class LtRoomRole { HOST, GUEST, NONE }

sealed class LtEvent {
    data class RoomCreated(val roomCode: String, val userId: String) : LtEvent()
    data class JoinApproved(val roomCode: String, val state: LtRoomState) : LtEvent()
    data class JoinRejected(val reason: String) : LtEvent()
    data class PlaybackSync(val action: LtPlaybackAction) : LtEvent()
    data class SyncStateReceived(val state: LtSyncState) : LtEvent()
    data class Reconnected(val roomCode: String, val isHost: Boolean, val state: LtRoomState) : LtEvent()
    data class UserJoined(val userId: String, val username: String) : LtEvent()
    data class UserLeft(val userId: String, val username: String) : LtEvent()
    data class HostChanged(val newHostId: String, val newHostName: String) : LtEvent()
    data class Kicked(val reason: String) : LtEvent()
    data class Error(val message: String) : LtEvent()
}

/**
 * The Listen Together WebSocket client.
 *
 * It speaks the relay protocol used by the mobile app: create and join rooms,
 * presence, approval, host transfer, playback actions and session reconnection.
 * The URL is a provider because Settings can change it while the app is open.
 */
class ListenTogetherClient(
    private val serverUrlProvider: () -> String
) {
    private val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
        encodeDefaults = true
        explicitNulls = false
    }

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    private val http = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .writeTimeout(30, TimeUnit.SECONDS)
        .pingInterval(25, TimeUnit.SECONDS)
        .build()

    private val _events = MutableSharedFlow<LtEvent>(extraBufferCapacity = 128)
    val events: SharedFlow<LtEvent> = _events.asSharedFlow()

    private val _connectionState = MutableStateFlow(LtConnectionState.DISCONNECTED)
    val connectionState: StateFlow<LtConnectionState> = _connectionState.asStateFlow()

    private val _roomState = MutableStateFlow<LtRoomState?>(null)
    val roomState: StateFlow<LtRoomState?> = _roomState.asStateFlow()

    private val _role = MutableStateFlow(LtRoomRole.NONE)
    val role: StateFlow<LtRoomRole> = _role.asStateFlow()

    private val _userId = MutableStateFlow<String?>(null)
    val userId: StateFlow<String?> = _userId.asStateFlow()

    private val _pendingJoinRequests = MutableStateFlow<List<LtJoinRequest>>(emptyList())
    val pendingJoinRequests: StateFlow<List<LtJoinRequest>> = _pendingJoinRequests.asStateFlow()

    private val _busy = MutableStateFlow(false)
    val busy: StateFlow<Boolean> = _busy.asStateFlow()

    private val _latencyMs = MutableStateFlow<Long?>(null)
    val latencyMs: StateFlow<Long?> = _latencyMs.asStateFlow()

    private val _serverOnline = MutableStateFlow<Boolean?>(null)
    val serverOnline: StateFlow<Boolean?> = _serverOnline.asStateFlow()

    private var socket: WebSocket? = null
    private var username = ""
    private var sessionToken: String? = null
    private var storedRoomCode: String? = null
    private var manualClose = false
    private var reconnectAttempts = 0
    private var pingJob: Job? = null
    private var reconnectJob: Job? = null
    private var connectStartedAt = 0L
    private var pingSentAt = 0L

    /** Last code a join attempt carried, for the widget-to-manager test. */
    internal var lastJoinCodeForTest: String? = null
        private set

    private val pendingMessages = ConcurrentLinkedQueue<String>()

    val isInRoom: Boolean get() = _roomState.value != null
    val isHost: Boolean get() = _role.value == LtRoomRole.HOST

    private fun withHostFlag(state: LtRoomState): LtRoomState =
        state.copy(users = state.users.distinctBy { it.userId }.map { it.copy(isHost = it.userId == state.hostId) })

    private val listener = object : WebSocketListener() {
        override fun onOpen(webSocket: WebSocket, response: Response) {
            reconnectAttempts = 0
            _connectionState.value = LtConnectionState.CONNECTED
            _serverOnline.value = true
            _latencyMs.value = (System.currentTimeMillis() - connectStartedAt).coerceAtLeast(0L)
            startPing()
            while (true) {
                val next = pendingMessages.poll() ?: break
                webSocket.send(next)
            }
            val token = sessionToken
            if (token != null && storedRoomCode != null) {
                sendNow(LtMessage(LtMessageTypes.RECONNECT, json.encodeToJsonElement(LtReconnect(token))))
            }
        }

        override fun onMessage(webSocket: WebSocket, text: String) = handle(text)

        override fun onClosed(webSocket: WebSocket, code: Int, reason: String) = handleDisconnect()

        override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) {
            _busy.value = false
            _serverOnline.value = false
            scope.launch { _events.emit(LtEvent.Error(t.message ?: "Connection failed")) }
            handleDisconnect()
        }
    }

    fun connect() {
        if (_connectionState.value == LtConnectionState.CONNECTED ||
            _connectionState.value == LtConnectionState.CONNECTING
        ) return
        manualClose = false
        connectStartedAt = System.currentTimeMillis()
        _connectionState.value = LtConnectionState.CONNECTING
        runCatching {
            socket = http.newWebSocket(Request.Builder().url(serverUrlProvider()).build(), listener)
        }.onFailure {
            _connectionState.value = LtConnectionState.ERROR
            _serverOnline.value = false
            _busy.value = false
            scope.launch { _events.emit(LtEvent.Error(it.message ?: "Invalid server address")) }
        }
    }

    fun disconnect() {
        manualClose = true
        reconnectJob?.cancel()
        pingJob?.cancel()
        socket?.close(1000, "bye")
        socket = null
        pendingMessages.clear()
        _connectionState.value = LtConnectionState.DISCONNECTED
        _roomState.value = null
        _userId.value = null
        _role.value = LtRoomRole.NONE
        _pendingJoinRequests.value = emptyList()
        _busy.value = false
        sessionToken = null
        storedRoomCode = null
    }

    private fun handleDisconnect() {
        if (_connectionState.value == LtConnectionState.RECONNECTING) return
        _connectionState.value = LtConnectionState.DISCONNECTED
        _serverOnline.value = false
        if (_roomState.value == null) _busy.value = false
        pingJob?.cancel()
        if (manualClose) return
        if (_roomState.value != null || sessionToken != null) scheduleReconnect()
    }

    private fun scheduleReconnect() {
        reconnectJob?.cancel()
        reconnectJob = scope.launch {
            reconnectAttempts++
            _connectionState.value = LtConnectionState.RECONNECTING
            delay(minOf(2000L * reconnectAttempts, 15000L))
            if (!manualClose) connect()
        }
    }

    private fun startPing() {
        pingJob?.cancel()
        pingJob = scope.launch {
            while (true) {
                delay(20_000L)
                pingSentAt = System.currentTimeMillis()
                sendNow(LtMessage(LtMessageTypes.PING, null))
            }
        }
    }

    private fun abandonSession() {
        sessionToken = null
        storedRoomCode = null
    }

    fun createRoom(name: String) {
        abandonSession()
        username = name.trim()
        _busy.value = true
        send(
            LtMessage(
                LtMessageTypes.CREATE_ROOM,
                json.encodeToJsonElement(LtCreateRoom(username))
            )
        )
    }

    fun joinRoom(code: String, name: String) {
        abandonSession()
        lastJoinCodeForTest = code.trim().uppercase()
        username = name.trim()
        _busy.value = true
        send(
            LtMessage(
                LtMessageTypes.JOIN_ROOM,
                json.encodeToJsonElement(LtJoinRoom(code.trim().uppercase(), username))
            )
        )
    }

    /**
     * Seeds the room state for a UI test, so the screen's room branch can be
     * exercised without a relay. Runtime behaviour is unchanged.
     */
    internal fun installRoomStateForTest(
        state: LtRoomState?,
        role: LtRoomRole = LtRoomRole.GUEST,
        connection: LtConnectionState = LtConnectionState.CONNECTED
    ) {
        _roomState.value = state
        _role.value = role
        _connectionState.value = connection
        _serverOnline.value = connection == LtConnectionState.CONNECTED
        _busy.value = false
    }

    fun leaveRoom() {
        send(LtMessage(LtMessageTypes.LEAVE_ROOM, null))
        _roomState.value = null
        _role.value = LtRoomRole.NONE
        _pendingJoinRequests.value = emptyList()
        abandonSession()
        reconnectJob?.cancel()
        socket?.close(1000, "Left room")
        socket = null
        _connectionState.value = LtConnectionState.DISCONNECTED
        manualClose = false
        scope.launch {
            delay(200)
            connect()
        }
    }

    fun approveJoin(userId: String) {
        if (_pendingJoinRequests.value.any { it.userId == userId }) {
            _pendingJoinRequests.value = _pendingJoinRequests.value.filter { it.userId != userId }
        }
        send(LtMessage(LtMessageTypes.APPROVE_JOIN, json.encodeToJsonElement(LtApproveJoin(userId))))
    }

    fun rejectJoin(userId: String, reason: String? = null) {
        _pendingJoinRequests.value = _pendingJoinRequests.value.filter { it.userId != userId }
        send(LtMessage(LtMessageTypes.REJECT_JOIN, json.encodeToJsonElement(LtRejectJoin(userId, reason))))
    }

    fun kickUser(userId: String, reason: String? = null) {
        send(LtMessage(LtMessageTypes.KICK_USER, json.encodeToJsonElement(LtKickUser(userId, reason))))
    }

    fun transferHost(newHostId: String) {
        send(LtMessage(LtMessageTypes.TRANSFER_HOST, json.encodeToJsonElement(LtTransferHost(newHostId))))
    }

    fun requestSync() {
        send(LtMessage(LtMessageTypes.REQUEST_SYNC, null))
    }

    fun sendPlaybackAction(
        action: String,
        trackId: String? = null,
        position: Long? = null,
        trackInfo: LtTrackInfo? = null,
        insertNext: Boolean? = null,
        queue: List<LtTrackInfo>? = null,
        volume: Float? = null
    ) {
        send(
            LtMessage(
                LtMessageTypes.PLAYBACK_ACTION,
                json.encodeToJsonElement(
                    LtPlaybackAction(
                        action = action,
                        trackId = trackId,
                        position = position,
                        trackInfo = trackInfo,
                        insertNext = insertNext,
                        queue = queue,
                        queueTitle = "Listen Together",
                        volume = volume
                    )
                )
            )
        )
    }

    /**
     * Opens a throwaway socket and measures how long the relay takes to accept
     * it, so the lobby can say whether the address the user typed is reachable.
     */
    fun testConnection() {
        _serverOnline.value = null
        _latencyMs.value = null
        val startedAt = System.currentTimeMillis()
        val client = OkHttpClient.Builder()
            .connectTimeout(6, TimeUnit.SECONDS)
            .readTimeout(6, TimeUnit.SECONDS)
            .build()
        val probe = object : WebSocketListener() {
            override fun onOpen(webSocket: WebSocket, response: Response) {
                _serverOnline.value = true
                _latencyMs.value = (System.currentTimeMillis() - startedAt).coerceAtLeast(0L)
                webSocket.close(1000, "probe")
            }

            override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) {
                _serverOnline.value = false
            }
        }
        runCatching {
            client.newWebSocket(Request.Builder().url(serverUrlProvider()).build(), probe)
        }.onFailure {
            _serverOnline.value = false
        }
    }

    private fun send(msg: LtMessage) {
        val text = json.encodeToString(msg)
        val ws = socket
        if (ws != null && _connectionState.value == LtConnectionState.CONNECTED) {
            ws.send(text)
        } else {
            pendingMessages.add(text)
            connect()
        }
    }

    private fun sendNow(msg: LtMessage) {
        socket?.send(json.encodeToString(msg))
    }

    private fun handle(text: String) {
        val msg = try {
            json.decodeFromString<LtMessage>(text)
        } catch (e: Exception) {
            return
        }
        val p = msg.payload
        when (msg.type) {
            LtMessageTypes.PONG -> {
                if (pingSentAt > 0L) {
                    _latencyMs.value = (System.currentTimeMillis() - pingSentAt).coerceAtLeast(0L)
                    pingSentAt = 0L
                }
            }

            LtMessageTypes.ROOM_CREATED -> {
                _busy.value = false
                p?.let { json.decodeFromJsonElement<LtRoomCreated>(it) }?.let { c ->
                    _userId.value = c.userId
                    _role.value = LtRoomRole.HOST
                    sessionToken = c.sessionToken
                    storedRoomCode = c.roomCode
                    _roomState.value = withHostFlag(
                        LtRoomState(
                            roomCode = c.roomCode,
                            hostId = c.userId,
                            users = listOf(LtUserInfo(c.userId, username, isHost = true))
                        )
                    )
                    scope.launch { _events.emit(LtEvent.RoomCreated(c.roomCode, c.userId)) }
                }
            }

            LtMessageTypes.JOIN_REQUEST -> {
                p?.let { json.decodeFromJsonElement<LtJoinRequest>(it) }?.let { r ->
                    _pendingJoinRequests.value =
                        _pendingJoinRequests.value.filter { it.userId != r.userId } + r
                }
            }

            LtMessageTypes.JOIN_APPROVED -> {
                _busy.value = false
                p?.let { json.decodeFromJsonElement<LtJoinApproved>(it) }?.let { c ->
                    _userId.value = c.userId
                    _role.value = LtRoomRole.GUEST
                    sessionToken = c.sessionToken
                    storedRoomCode = c.roomCode
                    _roomState.value = withHostFlag(c.state)
                    scope.launch { _events.emit(LtEvent.JoinApproved(c.roomCode, c.state)) }
                }
            }

            LtMessageTypes.JOIN_REJECTED -> {
                _busy.value = false
                val reason = p?.let { json.decodeFromJsonElement<LtJoinRejected>(it).reason } ?: "Join rejected"
                scope.launch { _events.emit(LtEvent.JoinRejected(reason)) }
            }

            LtMessageTypes.USER_JOINED -> {
                p?.let { json.decodeFromJsonElement<LtUserJoined>(it) }?.let { u ->
                    _pendingJoinRequests.value = _pendingJoinRequests.value.filter { it.userId != u.userId }
                    _roomState.value = _roomState.value?.let { state ->
                        withHostFlag(state.copy(users = state.users.filter { it.userId != u.userId } + LtUserInfo(u.userId, u.username, avatarIndex = u.avatarIndex)))
                    }
                    scope.launch { _events.emit(LtEvent.UserJoined(u.userId, u.username)) }
                }
            }

            LtMessageTypes.USER_LEFT -> {
                p?.let { json.decodeFromJsonElement<LtUserLeft>(it) }?.let { u ->
                    _roomState.value = _roomState.value?.let { it.copy(users = it.users.filter { x -> x.userId != u.userId }) }
                    _pendingJoinRequests.value = _pendingJoinRequests.value.filter { it.userId != u.userId }
                    scope.launch { _events.emit(LtEvent.UserLeft(u.userId, u.username)) }
                }
            }

            LtMessageTypes.USER_RECONNECTED -> {
                p?.let { json.decodeFromJsonElement<LtUserReconnected>(it) }?.let { u ->
                    _roomState.value = _roomState.value?.let { state ->
                        state.copy(users = state.users.map { if (it.userId == u.userId) it.copy(isConnected = true) else it })
                    }
                    scope.launch { _events.emit(LtEvent.UserJoined(u.userId, u.username)) }
                }
            }

            LtMessageTypes.USER_DISCONNECTED -> {
                p?.let { json.decodeFromJsonElement<LtUserDisconnected>(it) }?.let { u ->
                    _roomState.value = _roomState.value?.let { state ->
                        state.copy(users = state.users.map { if (it.userId == u.userId) it.copy(isConnected = false) else it })
                    }
                }
            }

            LtMessageTypes.SYNC_PLAYBACK -> {
                p?.let { json.decodeFromJsonElement<LtPlaybackAction>(it) }?.let { action ->
                    scope.launch { _events.emit(LtEvent.PlaybackSync(action)) }
                }
            }

            LtMessageTypes.SYNC_STATE -> {
                p?.let { json.decodeFromJsonElement<LtSyncState>(it) }?.let { state ->
                    scope.launch { _events.emit(LtEvent.SyncStateReceived(state)) }
                }
            }

            LtMessageTypes.HOST_CHANGED -> {
                p?.let { json.decodeFromJsonElement<LtHostChanged>(it) }?.let { h ->
                    _roomState.value = _roomState.value?.let { withHostFlag(it.copy(hostId = h.newHostId)) }
                    _role.value = if (_userId.value == h.newHostId) LtRoomRole.HOST else LtRoomRole.GUEST
                    scope.launch { _events.emit(LtEvent.HostChanged(h.newHostId, h.newHostName)) }
                }
            }

            LtMessageTypes.KICKED -> {
                val reason = p?.let { json.decodeFromJsonElement<LtKicked>(it).reason } ?: "Kicked"
                _roomState.value = null
                _role.value = LtRoomRole.NONE
                _pendingJoinRequests.value = emptyList()
                abandonSession()
                scope.launch { _events.emit(LtEvent.Kicked(reason)) }
            }

            LtMessageTypes.RECONNECTED -> {
                p?.let { json.decodeFromJsonElement<LtReconnected>(it) }?.let { r ->
                    _userId.value = r.userId
                    _roomState.value = withHostFlag(r.state)
                    _role.value = if (r.isHost) LtRoomRole.HOST else LtRoomRole.GUEST
                    username = r.state.users.firstOrNull { it.userId == r.userId }?.username ?: username
                    scope.launch { _events.emit(LtEvent.Reconnected(r.roomCode, r.isHost, r.state)) }
                }
            }

            LtMessageTypes.ERROR -> {
                _busy.value = false
                val reason = p?.let {
                    runCatching { json.decodeFromJsonElement<LtError>(it).message }.getOrNull()
                } ?: "Server error"
                scope.launch { _events.emit(LtEvent.Error(reason)) }
            }
        }
    }
}
