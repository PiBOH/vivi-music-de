package com.music.vivi.desktop

import com.music.vivi.desktop.player.PlayerController
import com.music.vivi.desktop.player.PlayerState
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
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.decodeFromJsonElement
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import okhttp3.WebSocket
import okhttp3.WebSocketListener
import java.util.concurrent.ConcurrentLinkedQueue
import java.util.concurrent.TimeUnit
import kotlin.math.abs

// ---------------------------------------------------------------------------
// Wire protocol (full port of the mobile `Protocol.kt`, same JSON names so the
// desktop client can talk to the same public relay servers).
// ---------------------------------------------------------------------------

object LtMessageTypes {
    // Client -> Server
    const val CREATE_ROOM = "create_room"
    const val JOIN_ROOM = "join_room"
    const val LEAVE_ROOM = "leave_room"
    const val APPROVE_JOIN = "approve_join"
    const val REJECT_JOIN = "reject_join"
    const val PLAYBACK_ACTION = "playback_action"
    const val BUFFER_READY = "buffer_ready"
    const val KICK_USER = "kick_user"
    const val TRANSFER_HOST = "transfer_host"
    const val PING = "ping"
    const val CHAT = "chat"
    const val REQUEST_SYNC = "request_sync"
    const val RECONNECT = "reconnect"
    const val SUGGEST_TRACK = "suggest_track"
    const val APPROVE_SUGGESTION = "approve_suggestion"
    const val REJECT_SUGGESTION = "reject_suggestion"

    // Server -> Client
    const val ROOM_CREATED = "room_created"
    const val JOIN_REQUEST = "join_request"
    const val JOIN_APPROVED = "join_approved"
    const val JOIN_REJECTED = "join_rejected"
    const val USER_JOINED = "user_joined"
    const val USER_LEFT = "user_left"
    const val SYNC_PLAYBACK = "sync_playback"
    const val BUFFER_WAIT = "buffer_wait"
    const val BUFFER_COMPLETE = "buffer_complete"
    const val ERROR = "error"
    const val PONG = "pong"
    const val HOST_CHANGED = "host_changed"
    const val KICKED = "kicked"
    const val SYNC_STATE = "sync_state"
    const val RECONNECTED = "reconnected"
    const val USER_RECONNECTED = "user_reconnected"
    const val USER_DISCONNECTED = "user_disconnected"
    const val SUGGESTION_RECEIVED = "suggestion_received"
    const val SUGGESTION_APPROVED = "suggestion_approved"
    const val SUGGESTION_REJECTED = "suggestion_rejected"
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
    val duration: Long = 0L, // milliseconds
    val thumbnail: String? = null,
    @SerialName("suggested_by") val suggestedBy: String? = null,
)

@Serializable
data class LtUserInfo(
    @SerialName("user_id") val userId: String,
    val username: String,
    @SerialName("is_host") val isHost: Boolean = false,
    @SerialName("is_connected") val isConnected: Boolean = true,
    @SerialName("avatar_index") val avatarIndex: Int = 0,
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
    val queue: List<LtTrackInfo> = emptyList(),
)

// Request payloads
//
// `avatar_index` is part of both, exactly as on the phone: without it the relay
// stores the room with the default avatar and a desktop user shows up as a bare
// initial next to phone users who picked one.
@Serializable
data class LtCreateRoom(val username: String, @SerialName("avatar_index") val avatarIndex: Int = 0)

@Serializable
data class LtJoinRoom(
    @SerialName("room_code") val roomCode: String,
    val username: String,
    @SerialName("avatar_index") val avatarIndex: Int = 0,
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
    @SerialName("server_time") val serverTime: Long? = null,
)

@Serializable
data class LtBufferReady(@SerialName("track_id") val trackId: String)

@Serializable
data class LtKickUser(@SerialName("user_id") val userId: String, val reason: String? = null)

@Serializable
data class LtTransferHost(@SerialName("new_host_id") val newHostId: String)

@Serializable
data class LtChat(val message: String, @SerialName("reply_to") val replyTo: LtRepliedMessage? = null)

@Serializable
data class LtRepliedMessage(val username: String, val message: String)

@Serializable
data class LtSuggestTrack(@SerialName("track_info") val trackInfo: LtTrackInfo)

@Serializable
data class LtApproveSuggestion(@SerialName("suggestion_id") val suggestionId: String)

@Serializable
data class LtRejectSuggestion(@SerialName("suggestion_id") val suggestionId: String, val reason: String? = null)

@Serializable
data class LtReconnect(@SerialName("session_token") val sessionToken: String)

// Response payloads
@Serializable
data class LtRoomCreated(
    @SerialName("room_code") val roomCode: String,
    @SerialName("user_id") val userId: String,
    @SerialName("session_token") val sessionToken: String,
)

@Serializable
data class LtJoinRequest(@SerialName("user_id") val userId: String, val username: String)

@Serializable
data class LtJoinApproved(
    @SerialName("room_code") val roomCode: String,
    @SerialName("user_id") val userId: String,
    @SerialName("session_token") val sessionToken: String,
    val state: LtRoomState,
)

@Serializable
data class LtJoinRejected(val reason: String)

@Serializable
data class LtUserJoined(
    @SerialName("user_id") val userId: String,
    val username: String,
    @SerialName("avatar_index") val avatarIndex: Int = 0,
)

@Serializable
data class LtUserLeft(@SerialName("user_id") val userId: String, val username: String)

@Serializable
data class LtBufferWait(
    @SerialName("track_id") val trackId: String,
    @SerialName("waiting_for") val waitingFor: List<String> = emptyList(),
)

@Serializable
data class LtBufferComplete(@SerialName("track_id") val trackId: String)

@Serializable
data class LtError(val code: String, val message: String)

@Serializable
data class LtChatMessage(
    @SerialName("user_id") val userId: String,
    val username: String,
    val message: String,
    val timestamp: Long,
    @SerialName("reply_to") val replyTo: LtRepliedMessage? = null,
)

@Serializable
data class LtHostChanged(
    @SerialName("new_host_id") val newHostId: String,
    @SerialName("new_host_name") val newHostName: String,
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
    val volume: Float? = null,
)

@Serializable
data class LtReconnected(
    @SerialName("room_code") val roomCode: String,
    @SerialName("user_id") val userId: String,
    val state: LtRoomState,
    @SerialName("is_host") val isHost: Boolean,
)

@Serializable
data class LtUserReconnected(@SerialName("user_id") val userId: String, val username: String)

@Serializable
data class LtUserDisconnected(@SerialName("user_id") val userId: String, val username: String)

@Serializable
data class LtSuggestionReceived(
    @SerialName("suggestion_id") val suggestionId: String,
    @SerialName("from_user_id") val fromUserId: String,
    @SerialName("from_username") val fromUsername: String,
    @SerialName("track_info") val trackInfo: LtTrackInfo,
)

@Serializable
data class LtSuggestionApproved(
    @SerialName("suggestion_id") val suggestionId: String,
    @SerialName("track_info") val trackInfo: LtTrackInfo,
)

@Serializable
data class LtSuggestionRejected(
    @SerialName("suggestion_id") val suggestionId: String,
    val reason: String? = null,
)

// ---------------------------------------------------------------------------
// Connection state / role / events
// ---------------------------------------------------------------------------

enum class LtConnectionState { DISCONNECTED, CONNECTING, CONNECTED, RECONNECTING, ERROR }

enum class LtRoomRole { HOST, GUEST, NONE }

sealed class LtEvent {
    data class Connected(val userId: String) : LtEvent()
    data object Disconnected : LtEvent()
    data class Reconnecting(val attempt: Int, val maxAttempts: Int) : LtEvent()
    data class Reconnected(val roomCode: String, val isHost: Boolean, val state: LtRoomState) : LtEvent()
    data class RoomCreated(val roomCode: String, val userId: String) : LtEvent()
    data class JoinApproved(val roomCode: String, val state: LtRoomState) : LtEvent()
    data class JoinRejected(val reason: String) : LtEvent()
    data class JoinRequest(val userId: String, val username: String) : LtEvent()
    data class UserJoined(val userId: String, val username: String) : LtEvent()
    data class UserLeft(val userId: String, val username: String) : LtEvent()
    data class UserReconnected(val userId: String, val username: String) : LtEvent()
    data class UserDisconnected(val userId: String, val username: String) : LtEvent()
    data class PlaybackSync(val action: LtPlaybackAction) : LtEvent()
    data class BufferWait(val trackId: String, val waitingFor: List<String>) : LtEvent()
    data class BufferComplete(val trackId: String) : LtEvent()
    data class SyncStateReceived(val state: LtSyncState) : LtEvent()
    data class HostChanged(val newHostId: String, val newHostName: String) : LtEvent()
    data class Kicked(val reason: String) : LtEvent()
    data class Chat(val message: LtChatMessage) : LtEvent()
    data class SuggestionReceived(val suggestion: LtSuggestionReceived) : LtEvent()
    data class SuggestionApproved(val suggestion: LtSuggestionApproved) : LtEvent()
    data class SuggestionRejected(val suggestion: LtSuggestionRejected) : LtEvent()
    data class Error(val message: String) : LtEvent()
}

/** A log line shown in the Listen Together debug section. */
data class LtLogEntry(val level: String, val message: String, val detail: String? = null)

// ---------------------------------------------------------------------------
// Embedded replies ("universal reply fix", the mobile wire format)
// ---------------------------------------------------------------------------

/**
 * The public relay only forwards the chat text: there is no field on the wire
 * for a quote. The Android client therefore embeds the reply inside the message
 * itself as `\u200B[RPLY:<base64(username|message)>]\u200B` and strips it again on
 * receive. Replying here without the same envelope is what showed the phone's
 * replies as literal `[RPLY:…]` text in this client, and made this client's
 * replies arrive on the phone as plain messages. Both halves live here.
 */
/** A chat message split into its text and its (possibly embedded) quote. */
data class LtReplySplit(val message: String, val replyTo: LtRepliedMessage?)

object LtReplyCodec {
    private const val PREFIX = "\u200B[RPLY:"
    private const val SUFFIX = "]\u200B"

    /** Wraps [message] with the quoted [replyTo], in the mobile's exact format. */
    fun encode(message: String, replyTo: LtRepliedMessage?): String {
        if (replyTo == null) return message
        val metadata = "${replyTo.username}|${replyTo.message}"
        val encoded = java.util.Base64.getEncoder().encodeToString(metadata.toByteArray(Charsets.UTF_8))
        return PREFIX + encoded + SUFFIX + message
    }

    /**
     * Reads the embedded quote out of [message]. Returns the message unchanged
     * when there is none, or when the envelope is malformed (the raw text is
     * better than dropping the message).
     */
    fun decode(message: String, fallback: LtRepliedMessage?): LtReplySplit {
        if (!message.startsWith(PREFIX)) return LtReplySplit(message, fallback)
        return runCatching {
            val end = message.indexOf(SUFFIX)
            if (end < 0) return LtReplySplit(message, fallback)
            val encoded = message.substring(PREFIX.length, end)
            val decoded = String(java.util.Base64.getDecoder().decode(encoded), Charsets.UTF_8)
            val parts = decoded.split("|", limit = 2)
            if (parts.size != 2) return LtReplySplit(message, fallback)
            LtReplySplit(message.substring(end + SUFFIX.length), LtRepliedMessage(parts[0], parts[1]))
        }.getOrElse { LtReplySplit(message, fallback) }
    }
}

/**
 * Live "who owns the transport" state, readable from any composable without
 * threading the manager down.
 *
 * The room screen, the song menus and every player read this: while the user is
 * a GUEST in a room the host owns the transport, so the seek bars and the
 * play/pause/skip controls are inert (exactly like the mobile app). The state
 * is snapshot state, so a composable that reads it re-renders when the role
 * changes.
 */
object ListenTogetherGate {
    private val _inRoom = mutableStateOf(false)
    val inRoom: State<Boolean> = _inRoom

    private val _isHost = mutableStateOf(false)
    val isHost: State<Boolean> = _isHost

    /** True while inside a room without being its host. */
    private val _locked = mutableStateOf(false)
    val locked: State<Boolean> = _locked

    val lockedNow: Boolean get() = _locked.value

    fun update(inRoom: Boolean, isHost: Boolean) {
        _inRoom.value = inRoom
        _isHost.value = isHost
        _locked.value = inRoom && !isHost
    }
}

/**
 * The running manager, published once at startup so pieces far from the room
 * screen (a song's "⋮" menu, for instance) can offer the guest-only actions
 * without every call site taking the manager as a parameter. Null until the main
 * window has built it.
 */
object ListenTogetherBridge {
    @Volatile
    var manager: ListenTogetherManager? = null
}

// ---------------------------------------------------------------------------
// Client
// ---------------------------------------------------------------------------

/**
 * Full Listen Together WebSocket client (pure JVM, OkHttp). Speaks the same
 * JSON protocol as the mobile client: create/join rooms, chat, playback sync,
 * buffering protocol, suggestions, kick/transfer host and session-based
 * reconnection. Session and blocked users are persisted in [DesktopSettings].
 */
class ListenTogetherClient(
    private val serverUrlProvider: () -> String = { DesktopSettings.load().listenTogetherServerUrl },
) {
    private val json = sharedJsonLenient
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

    private val _bufferingUsers = MutableStateFlow<List<String>>(emptyList())
    val bufferingUsers: StateFlow<List<String>> = _bufferingUsers.asStateFlow()

    private val _pendingSuggestions = MutableStateFlow<List<LtSuggestionReceived>>(emptyList())
    val pendingSuggestions: StateFlow<List<LtSuggestionReceived>> = _pendingSuggestions.asStateFlow()

    private val _chatMessages = MutableStateFlow<List<LtChatMessage>>(emptyList())
    val chatMessages: StateFlow<List<LtChatMessage>> = _chatMessages.asStateFlow()

    private val _blockedUsernames = MutableStateFlow(DesktopSettings.load().listenTogetherBlockedUsers.toSet())
    val blockedUsernames: StateFlow<Set<String>> = _blockedUsernames.asStateFlow()

    private val _logs = MutableStateFlow<List<LtLogEntry>>(emptyList())
    val logs: StateFlow<List<LtLogEntry>> = _logs.asStateFlow()

    private val _busy = MutableStateFlow(false)
    val busy: StateFlow<Boolean> = _busy.asStateFlow()

    private var socket: WebSocket? = null
    private var username = ""
    // Messages queued while the socket was offline/connecting. They are flushed
    // from onOpen using the *actual* open socket; capturing the socket value in
    // a closure before connect() would capture null and silently drop them.
    private val pendingMessages = ConcurrentLinkedQueue<String>()
    private var reconnectAttempts = 0
    private var manualClose = false
    private var reconnectJob: Job? = null
    private var pingJob: Job? = null
    private var sessionToken: String? = null
    private var storedRoomCode: String? = null

    init {
        sessionToken = DesktopSettings.load().listenTogetherSessionToken.ifBlank { null }
        storedRoomCode = DesktopSettings.load().listenTogetherRoomCode.ifBlank { null }
    }

    val isInRoom: Boolean get() = _roomState.value != null
    val isHost: Boolean get() = _role.value == LtRoomRole.HOST
    val hasPersistedSession: Boolean get() = sessionToken != null && storedRoomCode != null
    val currentRoomCode: String? get() = _roomState.value?.roomCode ?: storedRoomCode

    private fun log(level: String, message: String, detail: String? = null) {
        _logs.value = (_logs.value + LtLogEntry(level, message, detail)).takeLast(200)
    }

    /**
     * Re-derives every user's `isHost` from the room's `hostId`.
     *
     * The server's own flags are not reliably carried in every frame (a
     * `host_changed` used to move `hostId` alone), so after a host transfer the
     * crown stayed on the old host and the user list contradicted the room state.
     * The `hostId` is the single source of truth here.
     */
    private fun withHostFlag(state: LtRoomState): LtRoomState =
        state.copy(users = state.users.map { it.copy(isHost = it.userId == state.hostId) })

    /** Public log sink so the manager can mirror its own events into the UI log. */
    fun addLog(level: String, message: String, detail: String? = null) {
        log(level, message, detail)
    }

    private fun saveSession() {
        DesktopSettings.update {
            it.copy(
                listenTogetherSessionToken = sessionToken ?: "",
                listenTogetherRoomCode = storedRoomCode ?: "",
                listenTogetherUserId = _userId.value ?: "",
                listenTogetherIsHost = isHost,
                listenTogetherSessionTimestamp = System.currentTimeMillis(),
                listenTogetherBlockedUsers = _blockedUsernames.value.toList(),
            )
        }
    }

    private fun clearSession() {
        sessionToken = null
        storedRoomCode = null
        DesktopSettings.update {
            it.copy(
                listenTogetherSessionToken = "",
                listenTogetherRoomCode = "",
                listenTogetherUserId = "",
                listenTogetherIsHost = false,
                listenTogetherSessionTimestamp = 0L,
            )
        }
    }

    private val listener = object : WebSocketListener() {
        override fun onOpen(webSocket: WebSocket, response: Response) {
            reconnectAttempts = 0
            _connectionState.value = LtConnectionState.CONNECTED
            startPing()
            // Flush everything queued while we were offline/connecting.
            while (true) {
                val next = pendingMessages.poll() ?: break
                webSocket.send(next)
            }
            // If we have a valid session, try to resume the room.
            val token = sessionToken
            if (token != null && storedRoomCode != null) {
                log("INFO", "Reconnecting to room", storedRoomCode)
                send(LtMessage(LtMessageTypes.RECONNECT, json.encodeToJsonElement(LtReconnect.serializer(), LtReconnect(token))))
            }
        }

        override fun onMessage(webSocket: WebSocket, text: String) = handle(text)

        override fun onClosed(webSocket: WebSocket, code: Int, reason: String) {
            handleDisconnect()
        }

        override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) {
            log("ERROR", "Connection failed", t.message)
            // Release the create/join spinner so the user can retry instead of
            // waiting forever on a room that was never created.
            _busy.value = false
            scope.launch { _events.emit(LtEvent.Error(t.message ?: "Connection failed")) }
            handleDisconnect()
        }
    }

    fun connect() {
        if (_connectionState.value == LtConnectionState.CONNECTED ||
            _connectionState.value == LtConnectionState.CONNECTING
        ) return
        manualClose = false
        _connectionState.value = LtConnectionState.CONNECTING
        val url = serverUrlProvider()
        log("INFO", "Connecting", url)
        socket = http.newWebSocket(Request.Builder().url(url).build(), listener)
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
        _bufferingUsers.value = emptyList()
        _pendingSuggestions.value = emptyList()
        _chatMessages.value = emptyList()
        clearSession()
    }

    private fun handleDisconnect() {
        if (_connectionState.value != LtConnectionState.CONNECTED) return
        _connectionState.value = LtConnectionState.DISCONNECTED
        // A create/join in flight has no room yet: release the spinner so the
        // user can retry once the connection comes back, instead of waiting
        // forever on a room that never materialized.
        if (_roomState.value == null) _busy.value = false
        scope.launch { _events.emit(LtEvent.Disconnected) }
        pingJob?.cancel()
        if (manualClose) return
        scheduleReconnect()
    }

    private fun scheduleReconnect() {
        reconnectJob?.cancel()
        reconnectJob = scope.launch {
            reconnectAttempts++
            val delayMs = minOf(2000L * reconnectAttempts, 30000L)
            _connectionState.value = LtConnectionState.RECONNECTING
            scope.launch { _events.emit(LtEvent.Reconnecting(reconnectAttempts, 5)) }
            delay(delayMs)
            if (!manualClose) connect()
        }
    }

    private fun startPing() {
        pingJob?.cancel()
        pingJob = scope.launch {
            while (true) {
                delay(30_000L)
                send(LtMessage(LtMessageTypes.PING, null))
            }
        }
    }

    /**
     * Drops the stored room session without touching the socket.
     *
     * Called before CREATE_ROOM / JOIN_ROOM, exactly like the mobile client
     * ("clear any existing session to ensure we create a new room instead of
     * reconnecting"). Without it the socket's `onOpen` sees a stored session and
     * sends a RECONNECT carrying the PREVIOUS room's token right after the new
     * room was created: the relay answers that stale token with an error (the
     * room it names does not exist any more) and the user was told "the room
     * does not exist" every time a room was created while an old session was
     * still on disk — and had to create a second one.
     */
    private fun abandonSession() {
        sessionToken = null
        storedRoomCode = null
        clearSession()
    }

    fun createRoom(name: String) {
        abandonSession()
        username = name.trim()
        _busy.value = true
        send(
            LtMessage(
                LtMessageTypes.CREATE_ROOM,
                json.encodeToJsonElement(
                    LtCreateRoom.serializer(),
                    LtCreateRoom(username, DesktopSettings.load().listenTogetherAvatarIndex),
                ),
            ),
        )
    }

    fun joinRoom(code: String, name: String) {
        abandonSession()
        username = name.trim()
        _busy.value = true
        send(
            LtMessage(
                LtMessageTypes.JOIN_ROOM,
                json.encodeToJsonElement(
                    LtJoinRoom.serializer(),
                    LtJoinRoom(code.trim().uppercase(), username, DesktopSettings.load().listenTogetherAvatarIndex),
                ),
            ),
        )
    }

    fun leaveRoom() {
        send(LtMessage(LtMessageTypes.LEAVE_ROOM, null))
        _roomState.value = null
        _role.value = LtRoomRole.NONE
        _pendingJoinRequests.value = emptyList()
        _bufferingUsers.value = emptyList()
        _pendingSuggestions.value = emptyList()
        _chatMessages.value = emptyList()
        clearSession()
        // The relay may close its side after LEAVE_ROOM (or keep a half-dead
        // socket on which the next CREATE_ROOM/JOIN_ROOM is silently dropped),
        // which showed up as "after leaving a room I can't leave or re-enter
        // one": the lobby buttons did nothing and the spinner never cleared.
        // Force a clean reconnect so the next create/join always starts from a
        // fresh, open connection (the pending message queue is flushed on
        // open, so a click right after leaving is not lost either).
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
        send(LtMessage(LtMessageTypes.APPROVE_JOIN, json.encodeToJsonElement(LtApproveJoin.serializer(), LtApproveJoin(userId))))
        // An approved user must leave the "requests" list at once. Waiting for
        // the server's `user_joined` left the approved person drawn among the
        // ones still waiting to be approved.
        _pendingJoinRequests.value = _pendingJoinRequests.value.filter { it.userId != userId }
    }

    fun rejectJoin(userId: String, reason: String? = null) {
        send(LtMessage(LtMessageTypes.REJECT_JOIN, json.encodeToJsonElement(LtRejectJoin.serializer(), LtRejectJoin(userId, reason ?: "Rejected by host"))))
        _pendingJoinRequests.value = _pendingJoinRequests.value.filter { it.userId != userId }
    }

    fun kickUser(userId: String, reason: String? = null) {
        send(LtMessage(LtMessageTypes.KICK_USER, json.encodeToJsonElement(LtKickUser.serializer(), LtKickUser(userId, reason ?: "Kicked by host"))))
    }

    fun transferHost(newHostId: String) {
        send(LtMessage(LtMessageTypes.TRANSFER_HOST, json.encodeToJsonElement(LtTransferHost.serializer(), LtTransferHost(newHostId))))
    }

    /**
     * Sends a chat message, optionally quoting an earlier one. The relay speaks
     * the same `chat` frame as the mobile client and the quote travels in the
     * `reply_to` field, so a reply written here is shown as a reply on Android
     * (and vice versa).
     */
    fun sendChat(text: String, replyTo: LtRepliedMessage? = null) {
        if (text.isBlank()) return
        // The quote is embedded in the text (mobile wire format) AND sent in the
        // `reply_to` field: the relay only forwards the text, so the envelope is
        // what makes the reply come back quoted on the phone.
        val payload = LtChat(LtReplyCodec.encode(text.trim(), replyTo), replyTo)
        send(LtMessage(LtMessageTypes.CHAT, json.encodeToJsonElement(LtChat.serializer(), payload)))
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
        queueTitle: String? = null,
        volume: Float? = null,
    ) {
        if (_role.value != LtRoomRole.HOST) {
            log("ERROR", "Cannot control playback", "Not host")
            return
        }
        send(
            LtMessage(
                LtMessageTypes.PLAYBACK_ACTION,
                json.encodeToJsonElement(
                    LtPlaybackAction.serializer(),
                    LtPlaybackAction(action, trackId, position, trackInfo, insertNext, queue, queueTitle, volume),
                ),
            )
        )
    }

    fun sendBufferReady(trackId: String) {
        send(LtMessage(LtMessageTypes.BUFFER_READY, json.encodeToJsonElement(LtBufferReady.serializer(), LtBufferReady(trackId))))
    }

    fun suggestTrack(trackInfo: LtTrackInfo) {
        if (!isInRoom) {
            log("ERROR", "Cannot suggest track", "Not in room")
            return
        }
        if (isHost) return
        send(LtMessage(LtMessageTypes.SUGGEST_TRACK, json.encodeToJsonElement(LtSuggestTrack.serializer(), LtSuggestTrack(trackInfo))))
    }

    fun approveSuggestion(suggestionId: String) {
        if (!isHost) return
        send(LtMessage(LtMessageTypes.APPROVE_SUGGESTION, json.encodeToJsonElement(LtApproveSuggestion.serializer(), LtApproveSuggestion(suggestionId))))
        _pendingSuggestions.value = _pendingSuggestions.value.filter { it.suggestionId != suggestionId }
    }

    fun rejectSuggestion(suggestionId: String, reason: String? = null) {
        if (!isHost) return
        send(LtMessage(LtMessageTypes.REJECT_SUGGESTION, json.encodeToJsonElement(LtRejectSuggestion.serializer(), LtRejectSuggestion(suggestionId, reason))))
        _pendingSuggestions.value = _pendingSuggestions.value.filter { it.suggestionId != suggestionId }
    }

    fun blockUser(username: String) {
        val updated = _blockedUsernames.value.toMutableSet().apply { add(username) }
        _blockedUsernames.value = updated
        _pendingJoinRequests.value = _pendingJoinRequests.value.filter { it.username !in updated }
        _pendingSuggestions.value = _pendingSuggestions.value.filter { it.fromUsername !in updated }
        DesktopSettings.update { it.copy(listenTogetherBlockedUsers = updated.toList()) }
    }

    fun unblockUser(username: String) {
        val updated = _blockedUsernames.value.toMutableSet().apply { remove(username) }
        _blockedUsernames.value = updated
        DesktopSettings.update { it.copy(listenTogetherBlockedUsers = updated.toList()) }
    }

    fun forceReconnect() {
        reconnectAttempts = 0
        socket?.close(1000, "Forcing reconnection")
        socket = null
        _connectionState.value = LtConnectionState.DISCONNECTED
        scope.launch {
            delay(500)
            connect()
        }
    }

    fun clearLogs() {
        _logs.value = emptyList()
    }

    /**
     * One incoming chat frame: strips the embedded quote (mobile envelope) and
     * appends the message.
     */
    private fun handleChat(payload: JsonElement?) {
        val c = payload?.let { runCatching { json.decodeFromJsonElement<LtChatMessage>(it) }.getOrNull() } ?: return
        val split = LtReplyCodec.decode(c.message, c.replyTo)
        val message = c.copy(message = split.message, replyTo = split.replyTo)
        val messages = _chatMessages.value
        // The chat list is keyed by timestamp + author + text, so a message that
        // arrives twice is a duplicate key and crashes the room. Identical
        // messages from the same author in the same millisecond are
        // indistinguishable anyway, so the second copy is dropped instead of
        // drawn.
        val duplicate = messages.any {
            it.timestamp == message.timestamp && it.userId == message.userId && it.message == message.message
        }
        if (!duplicate) {
            _chatMessages.value = (messages + message).takeLast(200)
        }
        scope.launch { _events.emit(LtEvent.Chat(message)) }
    }

    private fun send(msg: LtMessage) {
        val text = json.encodeToString(LtMessage.serializer(), msg)
        val ws = socket
        if (ws != null && _connectionState.value == LtConnectionState.CONNECTED) {
            ws.send(text)
        } else {
            pendingMessages.add(text)
            connect()
        }
    }

    private fun handle(text: String) {
        val msg = try { json.decodeFromString(LtMessage.serializer(), text) } catch (e: Exception) { return }
        if (msg.type == LtMessageTypes.CHAT) {
            handleChat(msg.payload)
            return
        }
        val p = msg.payload
        when (msg.type) {
            LtMessageTypes.PONG -> Unit

            LtMessageTypes.ROOM_CREATED -> {
                _busy.value = false
                p?.let { json.decodeFromJsonElement<LtRoomCreated>(it) }?.let { c ->
                    _userId.value = c.userId
                    _role.value = LtRoomRole.HOST
                    sessionToken = c.sessionToken
                    storedRoomCode = c.roomCode
                    _roomState.value = withHostFlag(
                        LtRoomState(
                            c.roomCode,
                            c.userId,
                            listOf(
                                LtUserInfo(
                                    c.userId,
                                    username,
                                    isHost = true,
                                    avatarIndex = DesktopSettings.load().listenTogetherAvatarIndex,
                                ),
                            ),
                        ),
                    )
                    saveSession()
                    scope.launch { _events.emit(LtEvent.RoomCreated(c.roomCode, c.userId)) }
                }
            }

            LtMessageTypes.JOIN_REQUEST -> {
                p?.let { json.decodeFromJsonElement<LtJoinRequest>(it) }?.let { r ->
                    if (r.username in _blockedUsernames.value) {
                        rejectJoin(r.userId, "Blocked")
                        return@let
                    }
                    // "Auto approve join requests": the setting existed and was
                    // shown in the room, but nothing ever read it, so the switch
                    // did exactly nothing and every guest waited for a tap. The
                    // host's own approval is what the setting promises.
                    if (DesktopSettings.load().listenTogetherAutoApproval) {
                        log("INFO", "Auto-approved ${r.username}")
                        approveJoin(r.userId)
                        // No local `UserJoined` is emitted here: the relay sends
                        // its own the moment the guest is really in, and emitting
                        // a second one re-broadcast the track for a user who was
                        // not in yet.
                        return@let
                    }
                    // One entry per user: a repeated request (a retry, a rejoin)
                    // appended a second row with the same `userId`, and the join
                    // request list is keyed by it, so the second copy took the
                    // whole app down with `Key "…" was already used`.
                    _pendingJoinRequests.value =
                        _pendingJoinRequests.value.filter { it.userId != r.userId } + r
                    scope.launch { _events.emit(LtEvent.JoinRequest(r.userId, r.username)) }
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
                    saveSession()
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
                    // Whoever joined is no longer "waiting to be approved".
                    _pendingJoinRequests.value = _pendingJoinRequests.value.filter { it.userId != u.userId }
                    // Replacing rather than appending: a user who joins twice
                    // (a reconnect the host did not see as one) used to get two
                    // rows with the same `userId`, and the user list is keyed by
                    // `userId`, so Compose refused the duplicate key and crashed
                    // the app while the room was open.
                    _roomState.value = _roomState.value?.let { state ->
                        withHostFlag(
                            state.copy(
                                users = state.users.filter { x -> x.userId != u.userId } +
                                    LtUserInfo(u.userId, u.username, avatarIndex = u.avatarIndex),
                            ),
                        )
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
                    scope.launch { _events.emit(LtEvent.UserReconnected(u.userId, u.username)) }
                }
            }

            LtMessageTypes.USER_DISCONNECTED -> {
                p?.let { json.decodeFromJsonElement<LtUserDisconnected>(it) }?.let { u ->
                    _roomState.value = _roomState.value?.let { state ->
                        state.copy(users = state.users.map { if (it.userId == u.userId) it.copy(isConnected = false) else it })
                    }
                    scope.launch { _events.emit(LtEvent.UserDisconnected(u.userId, u.username)) }
                }
            }

            LtMessageTypes.SYNC_PLAYBACK -> {
                p?.let { json.decodeFromJsonElement<LtPlaybackAction>(it) }?.let { pb ->
                    scope.launch { _events.emit(LtEvent.PlaybackSync(pb)) }
                }
            }

            LtMessageTypes.BUFFER_WAIT -> {
                p?.let { json.decodeFromJsonElement<LtBufferWait>(it) }?.let { b ->
                    _bufferingUsers.value = b.waitingFor
                    scope.launch { _events.emit(LtEvent.BufferWait(b.trackId, b.waitingFor)) }
                }
            }

            LtMessageTypes.BUFFER_COMPLETE -> {
                p?.let { json.decodeFromJsonElement<LtBufferComplete>(it) }?.let { b ->
                    _bufferingUsers.value = emptyList()
                    scope.launch { _events.emit(LtEvent.BufferComplete(b.trackId)) }
                }
            }

            LtMessageTypes.SYNC_STATE -> {
                p?.let { json.decodeFromJsonElement<LtSyncState>(it) }?.let { s ->
                    scope.launch { _events.emit(LtEvent.SyncStateReceived(s)) }
                }
            }

            LtMessageTypes.HOST_CHANGED -> {
                p?.let { json.decodeFromJsonElement<LtHostChanged>(it) }?.let { h ->
                    // Move the host AND re-flag the users: the crown has to
                    // follow the new host, on every client.
                    _roomState.value = _roomState.value?.let { withHostFlag(it.copy(hostId = h.newHostId)) }
                    val me = _userId.value
                    _role.value = if (me == h.newHostId) LtRoomRole.HOST else LtRoomRole.GUEST
                    saveSession()
                    scope.launch { _events.emit(LtEvent.HostChanged(h.newHostId, h.newHostName)) }
                }
            }

            LtMessageTypes.KICKED -> {
                val reason = p?.let { json.decodeFromJsonElement<LtKicked>(it).reason } ?: "Kicked"
                _roomState.value = null
                _role.value = LtRoomRole.NONE
                _pendingJoinRequests.value = emptyList()
                _pendingSuggestions.value = emptyList()
                _chatMessages.value = emptyList()
                clearSession()
                scope.launch { _events.emit(LtEvent.Kicked(reason)) }
            }

            LtMessageTypes.RECONNECTED -> {
                p?.let { json.decodeFromJsonElement<LtReconnected>(it) }?.let { r ->
                    _userId.value = r.userId
                    // The state comes from the network, so its user list is
                    // deduplicated here as well: it is keyed by `userId` when
                    // drawn, and a repeated entry is a crash, not a cosmetic
                    // problem.
                    _roomState.value = withHostFlag(r.state.copy(users = r.state.users.distinctBy { it.userId }))
                    _role.value = if (r.isHost) LtRoomRole.HOST else LtRoomRole.GUEST
                    username = r.state.users.firstOrNull { it.userId == r.userId }?.username ?: username
                    saveSession()
                    scope.launch { _events.emit(LtEvent.Reconnected(r.roomCode, r.isHost, r.state)) }
                }
            }

            LtMessageTypes.SUGGESTION_RECEIVED -> {
                p?.let { json.decodeFromJsonElement<LtSuggestionReceived>(it) }?.let { s ->
                    if (s.fromUsername !in _blockedUsernames.value) {
                        // One entry per suggestion id, for the same reason as
                        // above: the suggestion list is keyed by it.
                        _pendingSuggestions.value =
                            _pendingSuggestions.value.filter { it.suggestionId != s.suggestionId } + s
                        scope.launch { _events.emit(LtEvent.SuggestionReceived(s)) }
                    }
                }
            }

            LtMessageTypes.SUGGESTION_APPROVED -> {
                p?.let { json.decodeFromJsonElement<LtSuggestionApproved>(it) }?.let { s ->
                    scope.launch { _events.emit(LtEvent.SuggestionApproved(s)) }
                }
            }

            LtMessageTypes.SUGGESTION_REJECTED -> {
                p?.let { json.decodeFromJsonElement<LtSuggestionRejected>(it) }?.let { s ->
                    scope.launch { _events.emit(LtEvent.SuggestionRejected(s)) }
                }
            }

            LtMessageTypes.ERROR -> {
                _busy.value = false
                val m = p?.let { json.decodeFromJsonElement<LtError>(it).message } ?: "Server error"
                scope.launch { _events.emit(LtEvent.Error(m)) }
            }
        }
    }
}

// ---------------------------------------------------------------------------
// Manager: bridges the client with the desktop player (host + guest roles)
// ---------------------------------------------------------------------------

/**
 * Bridges the Listen Together client with the [PlayerController] exactly like
 * the mobile manager: as HOST it observes the local player (track changes,
 * play/pause, seeks, queue, volume) and broadcasts them; as GUEST it applies
 * the host's playback actions with debounce/tolerance, the buffering protocol,
 * a 10s heartbeat and the "smart re-sync" request after reconnection.
 */
class ListenTogetherManager(private val player: PlayerController) {

    companion object {
        private const val SYNC_DEBOUNCE_THRESHOLD_MS = 1000L
        private const val POSITION_TOLERANCE_MS = 2000L
        private const val PLAYBACK_POSITION_TOLERANCE_MS = 3000L
    }

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    val client = ListenTogetherClient()

    // Exposed state
    val connectionState = client.connectionState
    val roomState = client.roomState
    val role = client.role
    val userId = client.userId
    val pendingJoinRequests = client.pendingJoinRequests
    val bufferingUsers = client.bufferingUsers
    val pendingSuggestions = client.pendingSuggestions
    val chatMessages = client.chatMessages
    val blockedUsernames = client.blockedUsernames
    val logs = client.logs
    val busy = client.busy
    val events = client.events

    val isInRoom: Boolean get() = client.isInRoom
    val isHost: Boolean get() = client.isHost

    private var eventCollectorJob: Job? = null
    private var hostObserverJob: Job? = null
    private var queueObserverJob: Job? = null
    private var volumeObserverJob: Job? = null
    private var heartbeatJob: Job? = null
    private var playerListenerRegistered = false

    // Anti-feedback-loop guards
    @Volatile private var isSyncing = false
    private var lastSyncedIsPlaying: Boolean? = null
    private var lastSyncedTrackId: String? = null
    private var lastSyncActionTime = 0L
    private var bufferingTrackId: String? = null
    private var pendingSyncState: LtSyncState? = null
    private var bufferCompleteReceivedForTrack: String? = null
    private var currentTrackGeneration = 0
    private var lastSyncedVolume: Float? = null

    private val syncHostVolumeEnabled: Boolean
        get() = DesktopSettings.load().listenTogetherSyncVolume
    private val smartResyncEnabled: Boolean
        get() = DesktopSettings.load().listenTogetherSmartResync

    /** Initialize event collection + role observation. Call once at app start. */
    fun initialize() {
        eventCollectorJob?.cancel()
        eventCollectorJob = scope.launch {
            client.events.collect { event ->
                try {
                    handleEvent(event)
                } catch (e: Exception) {
                    log("ERROR", "Error handling event", e.message)
                }
            }
        }
        scope.launch {
            client.role.collect { newRole ->
                // The host owns the transport: as a guest this user's own play /
                // pause / seek / skip controls are inert (the server refuses the
                // actions anyway, but the UI must not look like it works either).
                // The mobile app locks the same controls, here and in every
                // player, from one shared state.
                val locked = newRole == LtRoomRole.GUEST
                player.userControlLocked = locked
                ListenTogetherGate.update(inRoom = newRole != LtRoomRole.NONE, isHost = newRole == LtRoomRole.HOST)
                when (newRole) {
                    LtRoomRole.HOST -> {
                        startHostObservation()
                        startQueueObservation()
                        startVolumeObservation()
                        startHeartbeat()
                    }
                    LtRoomRole.GUEST, LtRoomRole.NONE -> {
                        stopHostObservation()
                        stopQueueObservation()
                        stopVolumeObservation()
                        stopHeartbeat()
                    }
                }
            }
        }
    }

    private fun log(level: String, message: String, detail: String? = null) {
        // Mirrored into the client log list (UI shows client.logs).
        client.addLog(level, message, detail)
    }

    // ---- Host: observe the local player and broadcast ----

    private fun startHostObservation() {
        if (hostObserverJob?.isActive == true) return
        hostObserverJob = scope.launch {
            // Track change + play/pause
            player.state
                .map { it.current?.videoId to it.isPlaying }
                .distinctUntilChanged()
                .collect { (trackId, playing) ->
                    if (isSyncing) return@collect
                    val s = player.state.value
                    if (trackId != null && trackId != lastSyncedTrackId) {
                        lastSyncedTrackId = trackId
                        lastSyncedIsPlaying = false
                        s.current?.let { sendTrackChange(it, s) }
                        if (playing) {
                            lastSyncedIsPlaying = true
                            client.sendPlaybackAction(LtPlaybackActions.PLAY, position = s.positionMs)
                        }
                    } else if (playing != lastSyncedIsPlaying && trackId != null) {
                        lastSyncedIsPlaying = playing
                        if (playing) {
                            client.sendPlaybackAction(LtPlaybackActions.PLAY, position = s.positionMs)
                        } else {
                            client.sendPlaybackAction(LtPlaybackActions.PAUSE, position = s.positionMs)
                        }
                    }
                }
        }
        // User seeks
        scope.launch {
            player.seekEvents.collect { pos ->
                if (!isSyncing && isHost && isInRoom) {
                    client.sendPlaybackAction(LtPlaybackActions.SEEK, position = pos)
                }
            }
        }
    }

    private fun sendTrackChange(track: NowPlaying, s: PlayerState) {
        val queue = s.queue.map { it.toLtTrackInfo() }
        client.sendPlaybackAction(
            LtPlaybackActions.CHANGE_TRACK,
            trackInfo = track.toLtTrackInfo(),
            queue = queue,
            queueTitle = "Listen Together",
        )
    }

    private fun startQueueObservation() {
        if (queueObserverJob?.isActive == true) return
        queueObserverJob = scope.launch {
            player.state
                .map { it.queue.map { q -> q.videoId } }
                .distinctUntilChanged()
                .collect {
                    if (!isHost || !isInRoom || isSyncing) return@collect
                    delay(500) // debounce rapid manipulations
                    val s = player.state.value
                    client.sendPlaybackAction(
                        LtPlaybackActions.SYNC_QUEUE,
                        queue = s.queue.map { q -> q.toLtTrackInfo() },
                        queueTitle = "Listen Together",
                    )
                }
        }
    }

    private fun startVolumeObservation() {
        if (volumeObserverJob?.isActive == true) return
        volumeObserverJob = scope.launch {
            player.state
                .map { it.volume }
                .distinctUntilChanged()
                .collect { volume ->
                    if (!isHost || !isInRoom || !syncHostVolumeEnabled) return@collect
                    val last = lastSyncedVolume
                    if (last != null && abs(last - volume) < 0.01f) return@collect
                    lastSyncedVolume = volume
                    client.sendPlaybackAction(LtPlaybackActions.SET_VOLUME, volume = volume)
                }
        }
    }

    private fun startHeartbeat() {
        if (heartbeatJob?.isActive == true) return
        heartbeatJob = scope.launch {
            while (heartbeatJob?.isActive == true && isHost && isInRoom) {
                delay(10_000L)
                val s = player.state.value
                if (s.isPlaying && s.current != null) {
                    client.sendPlaybackAction(LtPlaybackActions.PLAY, position = s.positionMs)
                }
            }
        }
    }

    private fun stopHostObservation() {
        hostObserverJob?.cancel()
        hostObserverJob = null
    }

    private fun stopQueueObservation() {
        queueObserverJob?.cancel()
        queueObserverJob = null
    }

    private fun stopVolumeObservation() {
        volumeObserverJob?.cancel()
        volumeObserverJob = null
        lastSyncedVolume = null
    }

    private fun stopHeartbeat() {
        heartbeatJob?.cancel()
        heartbeatJob = null
    }

    // ---- Guest: apply the host's actions ----

    private fun handleEvent(event: LtEvent) {
        when (event) {
            is LtEvent.JoinApproved -> {
                applyPlaybackState(
                    currentTrack = event.state.currentTrack,
                    isPlaying = event.state.isPlaying,
                    position = event.state.position,
                    queue = event.state.queue,
                )
                applyHostVolumeIfNeeded(event.state.volume)
            }

            is LtEvent.PlaybackSync -> {
                val actionType = event.action.action
                val isQueueOp = actionType == LtPlaybackActions.QUEUE_ADD ||
                    actionType == LtPlaybackActions.QUEUE_REMOVE ||
                    actionType == LtPlaybackActions.QUEUE_CLEAR ||
                    actionType == LtPlaybackActions.SYNC_QUEUE
                if (!isHost || isQueueOp) {
                    handlePlaybackSync(event.action)
                }
            }

            is LtEvent.UserJoined -> {
                if (isHost) {
                    val s = player.state.value
                    s.current?.let {
                        sendTrackChange(it, s)
                        if (s.isPlaying) {
                            client.sendPlaybackAction(LtPlaybackActions.PLAY, position = s.positionMs)
                        }
                    }
                }
            }

            is LtEvent.BufferComplete -> {
                if (!isHost && bufferingTrackId == event.trackId) {
                    bufferCompleteReceivedForTrack = event.trackId
                    applyPendingSyncIfReady()
                }
            }

            is LtEvent.SyncStateReceived -> {
                if (!isHost) handleSyncState(event.state)
            }

            is LtEvent.Reconnected -> {
                if (event.isHost) {
                    lastSyncedIsPlaying = player.state.value.isPlaying
                    lastSyncedTrackId = player.state.value.current?.videoId
                    val s = player.state.value
                    val serverTrackId = event.state.currentTrack?.id
                    val current = s.current
                    if (current != null && serverTrackId != current.videoId) {
                        sendTrackChange(current, s)
                    }
                    scope.launch {
                        delay(500)
                        val cur = player.state.value
                        if (cur.isPlaying) {
                            client.sendPlaybackAction(LtPlaybackActions.PLAY, position = cur.positionMs)
                        }
                    }
                } else {
                    applyPlaybackState(
                        currentTrack = event.state.currentTrack,
                        isPlaying = event.state.isPlaying,
                        position = event.state.position,
                        queue = event.state.queue,
                        bypassBuffer = true,
                    )
                    applyHostVolumeIfNeeded(event.state.volume)
                    if (smartResyncEnabled) {
                        scope.launch {
                            delay(1000)
                            if (isInRoom && !isHost) client.requestSync()
                        }
                    }
                }
            }

            is LtEvent.Kicked -> {
                cleanup()
            }

            is LtEvent.SuggestionApproved -> {
                // Host approved a guest suggestion: this inserts it (remote
                // action, so a guest's own lock does not swallow it). When this
                // device IS the approving host the insert already happened in
                // [approveSuggestion] — the relay does not reliably echo the
                // frame back to the approver, which is why the mobile client
                // queues it locally too — and [applyApprovedSuggestion] drops
                // the echo instead of queueing the same track twice.
                applyApprovedSuggestion(event.suggestion.suggestionId, event.suggestion.trackInfo)
            }

            is LtEvent.HostChanged -> {
                val me = userId.value
                val nowHost = event.newHostId == me
                if (nowHost) {
                    // Gained host: send current state so guests catch up.
                    val s = player.state.value
                    s.current?.let {
                        sendTrackChange(it, s)
                        if (s.isPlaying) {
                            client.sendPlaybackAction(LtPlaybackActions.PLAY, position = s.positionMs)
                        }
                    }
                }
            }

            is LtEvent.Chat -> {
                // Chat lives in its own window, which may be closed while the
                // user listens: a message from someone else is then a real
                // notification. It goes through [DesktopNotifier] so it honours
                // the notification-mode setting (native OS toast vs in-app
                // banner) and lands in the notification history like every
                // other notice. Own messages and messages read in an open
                // window are not notified, and the badge on the Chat button
                // counts everything unread.
                val mine = event.message.userId == userId.value
                val read = ListenTogetherChatWindow.visible.value
                if (!mine && !read) {
                    ListenTogetherChatWindow.bumpUnread()
                    val quote = event.message.replyTo?.let { "↪ ${it.message}\n" }.orEmpty()
                    DesktopNotifier.notify(
                        title = event.message.username,
                        message = quote + event.message.message,
                        section = "listen_together",
                    )
                }
            }
            is LtEvent.Error -> Unit
            else -> Unit
        }
    }

    private fun handlePlaybackSync(action: LtPlaybackAction) {
        val s = player.state.value
        val now = System.currentTimeMillis()
        isSyncing = true
        try {
            when (action.action) {
                LtPlaybackActions.PLAY -> {
                    val basePos = action.position ?: 0L
                    val adjustedPos = action.serverTime?.let { st -> basePos + maxOf(0L, now - st) } ?: basePos
                    if (bufferingTrackId != null) {
                        pendingSyncState = (pendingSyncState ?: LtSyncState()).copy(
                            isPlaying = true,
                            position = adjustedPos,
                            lastUpdate = now,
                        )
                        applyPendingSyncIfReady()
                        return
                    }
                    val posDiff = abs(player.state.value.positionMs - adjustedPos)
                    val alreadyPlaying = player.state.value.isPlaying
                    if (alreadyPlaying && posDiff < POSITION_TOLERANCE_MS &&
                        (now - lastSyncActionTime) < SYNC_DEBOUNCE_THRESHOLD_MS
                    ) {
                        return
                    }
                    if (alreadyPlaying) {
                        if (posDiff > PLAYBACK_POSITION_TOLERANCE_MS) {
                            player.seekRemote(adjustedPos, true, 0L)
                        }
                    } else {
                        player.seekRemote(adjustedPos, true, POSITION_TOLERANCE_MS)
                    }
                    lastSyncActionTime = now
                }

                LtPlaybackActions.PAUSE -> {
                    val pos = action.position ?: 0L
                    if (bufferingTrackId != null) {
                        pendingSyncState = (pendingSyncState ?: LtSyncState()).copy(
                            isPlaying = false,
                            position = pos,
                            lastUpdate = now,
                        )
                        applyPendingSyncIfReady()
                        return
                    }
                    val posDiff = abs(player.state.value.positionMs - pos)
                    val alreadyPaused = !player.state.value.isPlaying
                    if (alreadyPaused && posDiff < POSITION_TOLERANCE_MS &&
                        (now - lastSyncActionTime) < SYNC_DEBOUNCE_THRESHOLD_MS
                    ) {
                        return
                    }
                    if (posDiff > POSITION_TOLERANCE_MS) {
                        player.seekRemote(pos, false, 0L)
                    } else if (!alreadyPaused) {
                        player.seekRemote(pos, false, 0L)
                    }
                    lastSyncActionTime = now
                }

                LtPlaybackActions.SEEK -> {
                    val pos = action.position ?: 0L
                    if (now - lastSyncActionTime < SYNC_DEBOUNCE_THRESHOLD_MS) return
                    if (abs(player.state.value.positionMs - pos) > POSITION_TOLERANCE_MS) {
                        player.seekRemote(pos, player.state.value.isPlaying, 0L)
                        lastSyncActionTime = now
                    }
                }

                LtPlaybackActions.CHANGE_TRACK -> {
                    action.trackInfo?.let { track ->
                        lastSyncActionTime = 0L
                        if (action.queue != null && action.queue.isNotEmpty()) {
                            applyPlaybackState(
                                currentTrack = track,
                                isPlaying = false,
                                position = 0,
                                queue = action.queue,
                            )
                        } else {
                            bufferingTrackId = track.id
                            syncToTrack(track, false, 0)
                        }
                    }
                }

                // These are the HOST's actions arriving over the wire, so they
                // run as remote (they bypass the guest's own control lock).
                LtPlaybackActions.SKIP_NEXT -> player.asRemote { player.next() }
                LtPlaybackActions.SKIP_PREV -> player.asRemote { player.previous() }

                LtPlaybackActions.QUEUE_ADD -> {
                    action.trackInfo?.let { track ->
                        player.asRemote {
                            if (action.insertNext == true) {
                                player.insertNext(track.toNowPlaying())
                            } else {
                                player.addToQueue(track.toNowPlaying())
                            }
                        }
                    }
                }

                LtPlaybackActions.QUEUE_REMOVE -> {
                    val removeId = action.trackId
                    if (!removeId.isNullOrEmpty()) {
                        val q = player.state.value.queue
                        val idx = q.indexOfFirst { it.videoId == removeId }
                        if (idx >= 0) player.asRemote { player.removeAt(idx) }
                    }
                }

                LtPlaybackActions.QUEUE_CLEAR -> player.asRemote { player.clearQueue() }

                LtPlaybackActions.SYNC_QUEUE -> {
                    val queue = action.queue
                    if (queue != null) {
                        val currentId = player.state.value.current?.videoId
                        val index = queue.indexOfFirst { it.id == currentId }
                        val nowPlaying = queue.map { it.toNowPlaying() }
                        if (index >= 0) {
                            player.replaceQueuePreservingCurrent(nowPlaying, player.state.value.positionMs, player.state.value.isPlaying)
                        } else {
                            player.applyRemotePlayback(nowPlaying, 0, player.state.value.positionMs, player.state.value.isPlaying)
                        }
                    }
                }

                LtPlaybackActions.SET_VOLUME -> {
                    applyHostVolumeIfNeeded(action.volume)
                }
            }
        } finally {
            scope.launch {
                delay(200)
                isSyncing = false
            }
        }
    }

    private fun handleSyncState(state: LtSyncState) {
        val now = System.currentTimeMillis()
        val adjustedPos = if (state.isPlaying) {
            state.position + maxOf(0L, now - state.lastUpdate)
        } else {
            state.position
        }
        applyPlaybackState(
            currentTrack = state.currentTrack,
            isPlaying = state.isPlaying,
            position = adjustedPos,
            queue = state.queue,
            bypassBuffer = true,
        )
        applyHostVolumeIfNeeded(state.volume)
    }

    private fun applyPlaybackState(
        currentTrack: LtTrackInfo?,
        isPlaying: Boolean,
        position: Long,
        queue: List<LtTrackInfo>?,
        bypassBuffer: Boolean = false,
    ) {
        val s = player.state.value
        if (currentTrack == null) {
            // No track: pause and (re)set the queue.
            val generation = ++currentTrackGeneration
            scope.launch {
                if (currentTrackGeneration != generation) return@launch
                isSyncing = true
                if (queue != null) {
                    if (queue.isNotEmpty()) {
                        player.restoreQueue(queue.map { it.toNowPlaying() }, 0)
                    } else {
                        player.asRemote { player.clearQueue() }
                    }
                }
                player.seekRemote(0, false, 0L)
                scope.launch {
                    delay(200)
                    isSyncing = false
                }
            }
            return
        }

        bufferingTrackId = currentTrack.id
        val generation = ++currentTrackGeneration
        scope.launch {
            if (currentTrackGeneration != generation) return@launch
            isSyncing = true
            try {
                val tracks = if (queue != null && queue.isNotEmpty()) queue.map { it.toNowPlaying() } else listOf(currentTrack.toNowPlaying())
                val index = tracks.indexOfFirst { it.videoId == currentTrack.id }.coerceAtLeast(0)
                // Load the queue paused at the right track.
                player.applyRemotePlayback(tracks, index, position, isPlaying = false, isResolving = true)

                if (bypassBuffer) {
                    // Manual sync / reconnect: apply immediately once ready.
                    scope.launch {
                        waitForReady()
                        player.seekRemote(position, isPlaying, 0L)
                        bufferingTrackId = null
                        pendingSyncState = null
                        bufferCompleteReceivedForTrack = null
                    }
                } else {
                    pendingSyncState = LtSyncState(
                        currentTrack = currentTrack,
                        isPlaying = isPlaying,
                        position = position,
                        lastUpdate = System.currentTimeMillis(),
                    )
                    applyPendingSyncIfReady()
                    client.sendBufferReady(currentTrack.id)
                }
            } finally {
                scope.launch {
                    delay(200)
                    isSyncing = false
                }
            }
        }
    }

    private suspend fun waitForReady() {
        var attempts = 0
        while (attempts < 100 && player.state.value.isResolving) {
            delay(50)
            attempts++
        }
    }

    private fun applyPendingSyncIfReady() {
        val pending = pendingSyncState ?: return
        val pendingTrackId = pending.currentTrack?.id ?: bufferingTrackId ?: return
        val completeForTrack = bufferCompleteReceivedForTrack
        if (completeForTrack != pendingTrackId) return

        val s = player.state.value
        isSyncing = true
        val targetPos = pending.position
        val posDiff = abs(s.positionMs - targetPos)
        if (posDiff > POSITION_TOLERANCE_MS) {
            player.seekRemote(targetPos, pending.isPlaying, 0L)
        } else {
            player.seekRemote(s.positionMs, pending.isPlaying, 0L)
        }
        bufferingTrackId = null
        pendingSyncState = null
        bufferCompleteReceivedForTrack = null
        scope.launch {
            delay(200)
            isSyncing = false
        }
    }

    private fun syncToTrack(track: LtTrackInfo, shouldPlay: Boolean, position: Long) {
        // No queue provided: load just the track.
        val generation = currentTrackGeneration
        scope.launch {
            if (currentTrackGeneration != generation) return@launch
            isSyncing = true
            try {
                player.applyRemotePlayback(listOf(track.toNowPlaying()), 0, position, isPlaying = false, isResolving = true)
                scope.launch {
                    waitForReady()
                    pendingSyncState = LtSyncState(
                        currentTrack = track,
                        isPlaying = shouldPlay,
                        position = position,
                        lastUpdate = System.currentTimeMillis(),
                    )
                    applyPendingSyncIfReady()
                    client.sendBufferReady(track.id)
                    delay(100)
                    isSyncing = false
                }
            } catch (e: Exception) {
                isSyncing = false
            }
        }
    }

    private fun applyHostVolumeIfNeeded(volume: Float?) {
        if (!syncHostVolumeEnabled || isHost || !isInRoom) return
        val target = volume?.coerceIn(0f, 1f) ?: return
        if (abs(player.state.value.volume - target) > 0.01f) {
            player.setVolume(target)
        }
    }

    // ---- Public API (mirrors the mobile manager) ----

    fun connect() = client.connect()
    fun disconnect() {
        cleanup()
        client.disconnect()
    }
    fun createRoom(username: String) = client.createRoom(username)
    fun joinRoom(roomCode: String, username: String) = client.joinRoom(roomCode, username)
    fun leaveRoom() {
        cleanup()
        client.leaveRoom()
    }
    fun approveJoin(userId: String) = client.approveJoin(userId)
    fun rejectJoin(userId: String, reason: String? = null) = client.rejectJoin(userId, reason)
    fun kickUser(userId: String, reason: String? = null) = client.kickUser(userId, reason)
    fun transferHost(newHostId: String) = client.transferHost(newHostId)
    fun blockUser(username: String) = client.blockUser(username)
    fun unblockUser(username: String) = client.unblockUser(username)
    fun sendChatMessage(message: String, replyTo: LtRepliedMessage? = null) = client.sendChat(message, replyTo)
    fun requestSync() = client.requestSync()
    fun suggestTrack(track: LtTrackInfo) = client.suggestTrack(track)

    /**
     * Approves a guest's suggestion.
     *
     * The approved track is queued HERE, immediately, instead of waiting for the
     * relay to echo `suggestion_approved` back: the host is the one who pressed
     * "approve", and the mobile client queues it locally for the same reason
     * (`LocalSuggestionApproved` -> `playNext`). The server's own echo, when it
     * does arrive, only re-inserts the track on the *other* clients and is
     * ignored here (see [appliedSuggestions]).
     */
    fun approveSuggestion(suggestionId: String) {
        client.pendingSuggestions.value
            .firstOrNull { it.suggestionId == suggestionId }
            ?.let { applyApprovedSuggestion(it.suggestionId, it.trackInfo) }
        client.approveSuggestion(suggestionId)
    }

    /**
     * Suggestion ids already inserted into the local queue. The relay may echo
     * the approval back to the approver (and a re-approved id may come twice),
     * and "play next" is not idempotent: without this the same track landed in
     * the queue two or three times.
     */
    private val appliedSuggestions = LinkedHashSet<String>()

    private fun applyApprovedSuggestion(suggestionId: String, trackInfo: LtTrackInfo) {
        if (!appliedSuggestions.add(suggestionId)) return
        // Bounded: a listening session can last hours and this only has to
        // remember the recent ids it already queued.
        while (appliedSuggestions.size > 200) {
            appliedSuggestions.remove(appliedSuggestions.first())
        }
        player.asRemote { player.insertNext(trackInfo.toNowPlaying()) }
    }
    fun rejectSuggestion(suggestionId: String, reason: String? = null) = client.rejectSuggestion(suggestionId, reason)
    fun forceReconnect() = client.forceReconnect()
    fun clearLogs() = client.clearLogs()

    private fun cleanup() {
        appliedSuggestions.clear()
        player.userControlLocked = false
        ListenTogetherGate.update(inRoom = false, isHost = false)
        if (isSyncing) isSyncing = false
        bufferingTrackId = null
        pendingSyncState = null
        bufferCompleteReceivedForTrack = null
        lastSyncedIsPlaying = null
        lastSyncedTrackId = null
        ++currentTrackGeneration
        stopHostObservation()
        stopQueueObservation()
        stopVolumeObservation()
        stopHeartbeat()
    }
}

// ---------------------------------------------------------------------------
// Conversions
// ---------------------------------------------------------------------------

private fun NowPlaying.toLtTrackInfo(): LtTrackInfo = LtTrackInfo(
    id = videoId,
    title = title,
    artist = artist,
    duration = durationMs,
    thumbnail = thumbnail,
)

private fun LtTrackInfo.toNowPlaying(): NowPlaying = NowPlaying(
    videoId = id,
    title = title,
    artist = artist,
    thumbnail = thumbnail,
    durationMs = duration,
)