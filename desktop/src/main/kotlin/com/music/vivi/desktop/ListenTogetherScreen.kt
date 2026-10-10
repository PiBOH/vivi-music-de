package com.music.vivi.desktop

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
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
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Reply
import androidx.compose.material.icons.filled.Block
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Link
import androidx.compose.material3.Badge
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.music.innertube.YouTube
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/** Parses a YouTube watch URL / youtu.be link / bare video id into a video id. */
private fun extractVideoId(input: String): String {
    val raw = input.trim()
    if (raw.isEmpty()) return ""
    val watch = Regex("(?:youtube\\.com|youtu\\.be)/(?:watch\\?v=|shorts/|embed/|live/)?([\\w-]{11})").find(raw)
    if (watch != null) return watch.groupValues[1]
    return if (raw.length == 11) raw else ""
}

/**
 * The phone's avatar set, in the SAME order it stores them (`avatar_index`), so
 * the index that travels over the relay picks the same choice on both platforms:
 * the APK draws its own pictures, the desktop stands in with emoji for 1..13 and
 * the username's initial for 0 — which is what index 0 means there too.
 */
internal val LT_AVATARS: List<String> = listOf(
    "", "🎧", "🎵", "🎸", "🥁", "🎹", "🎤", "🎷", "🎺", "🐱", "🐶", "🦊", "🐼", "🌈",
)

/** One user's avatar: the initial of index 0, an emoji otherwise. */
@Composable
internal fun LtAvatar(
    avatarIndex: Int,
    username: String,
    size: Int = 36,
    selected: Boolean = false,
) {
    val emoji = LT_AVATARS.getOrNull(avatarIndex).orEmpty()
    Box(
        Modifier
            .size(size.dp)
            .clip(CircleShape)
            .background(
                if (selected) MaterialTheme.colorScheme.primaryContainer
                else MaterialTheme.colorScheme.surfaceContainerHighest,
            )
            .then(
                if (selected) Modifier.border(2.dp, MaterialTheme.colorScheme.primary, CircleShape)
                else Modifier,
            ),
        contentAlignment = Alignment.Center,
    ) {
        if (emoji.isEmpty()) {
            Text(
                username.trim().take(1).uppercase().ifBlank { "?" },
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface,
            )
        } else {
            Text(emoji, fontSize = (size * 0.5f).sp)
        }
    }
}

@Composable
fun ListenTogetherScreen(
    language: String,
    onBack: () -> Unit,
    manager: ListenTogetherManager,
) {
    val connectionState by manager.connectionState.collectAsState()
    val roomState by manager.roomState.collectAsState()
    val userId by manager.userId.collectAsState()
    val pendingJoin by manager.pendingJoinRequests.collectAsState()
    val buffering by manager.bufferingUsers.collectAsState()
    val pendingSuggestions by manager.pendingSuggestions.collectAsState()
    val messages by manager.chatMessages.collectAsState()
    val busy by manager.busy.collectAsState()

    var usernameInput by remember(settingsFileRevision()) { mutableStateOf(DesktopSettings.load().listenTogetherUsername) }
    var roomCodeInput by remember { mutableStateOf("") }
    var serverInput by remember(settingsFileRevision()) { mutableStateOf(DesktopSettings.load().listenTogetherServerUrl) }
    var autoApprove by remember(settingsFileRevision()) { mutableStateOf(DesktopSettings.load().listenTogetherAutoApproval) }
    var syncVolume by remember(settingsFileRevision()) { mutableStateOf(DesktopSettings.load().listenTogetherSyncVolume) }
    var smartResync by remember(settingsFileRevision()) { mutableStateOf(DesktopSettings.load().listenTogetherSmartResync) }
    var avatarIndex by remember(settingsFileRevision()) { mutableStateOf(DesktopSettings.load().listenTogetherAvatarIndex) }
    var error by remember { mutableStateOf<String?>(null) }
    var notice by remember { mutableStateOf<String?>(null) }
    var suggestInput by remember { mutableStateOf("") }
    var copied by remember { mutableStateOf(false) }
    var copiedFailed by remember { mutableStateOf(false) }
    var linkCopied by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    LaunchedEffect(manager) {
        manager.events.collect { e ->
            when (e) {
                is LtEvent.Error -> error = e.message
                is LtEvent.JoinRejected -> error = e.reason
                is LtEvent.Kicked -> error = Localization.get(language, "lt_kicked") + (if (e.reason.isNotBlank()) ": ${e.reason}" else "")
                is LtEvent.RoomCreated -> {
                    error = null
                    notice = null
                    // The code is the one thing a host has to hand out, and the
                    // phone copies it to the clipboard the moment the room
                    // exists: do the same, so the invite is one paste away even
                    // before the room screen is read.
                    copyToClipboard(e.roomCode)
                }
                is LtEvent.JoinApproved, is LtEvent.Reconnected -> {
                    error = null
                    notice = null
                }
                is LtEvent.SuggestionApproved -> notice = Localization.get(language, "lt_suggestion_approved")
                is LtEvent.SuggestionRejected -> notice = Localization.get(language, "lt_suggestion_rejected")
                is LtEvent.Chat -> Unit
                else -> Unit
            }
        }
    }

    Column(Modifier.fillMaxSize().padding(16.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            BackButton(language, onBack)
            Spacer(Modifier.width(8.dp))
            Text(Localization.get(language, "listen_together"), style = MaterialTheme.typography.headlineMedium)
            Spacer(Modifier.weight(1f))
            ConnectionBadge(connectionState, language)
        }
        Spacer(Modifier.height(8.dp))
        ConnectionActions(
            language = language,
            state = connectionState,
            onConnect = { manager.connect() },
            onDisconnect = { manager.disconnect() },
            onReconnect = { manager.forceReconnect() },
        )
        Spacer(Modifier.height(8.dp))

        if (roomState == null) {
            LtLobby(
                language = language,
                username = usernameInput,
                onUsername = { usernameInput = it },
                avatarIndex = avatarIndex,
                onAvatar = {
                    avatarIndex = it
                    DesktopSettings.update { s -> s.copy(listenTogetherAvatarIndex = it) }
                },
                roomCode = roomCodeInput,
                onRoomCode = { if (it.length <= 8) roomCodeInput = it.uppercase() },
                server = serverInput,
                onServer = {
                    serverInput = it
                    DesktopSettings.update { s -> s.copy(listenTogetherServerUrl = it) }
                },
                autoApprove = autoApprove,
                onAutoApprove = {
                    autoApprove = it
                    DesktopSettings.update { s -> s.copy(listenTogetherAutoApproval = it) }
                },
                busy = busy,
                error = error,
                onCreate = {
                    error = null
                    DesktopSettings.update { s -> s.copy(listenTogetherUsername = usernameInput.trim()) }
                    manager.createRoom(usernameInput)
                },
                onJoin = {
                    error = null
                    DesktopSettings.update { s -> s.copy(listenTogetherUsername = usernameInput.trim()) }
                    manager.joinRoom(roomCodeInput, usernameInput)
                },
            )
        } else {
            val r = roomState!!
            val isHost = r.hostId == userId
            LtInRoom(
                language = language,
                room = r,
                isHost = isHost,
                myUserId = userId,
                pendingJoin = pendingJoin,
                buffering = buffering,
                pendingSuggestions = pendingSuggestions,
                messages = messages,
                busy = busy,
                error = error,
                notice = notice,
                suggestInput = suggestInput,
                onSuggestInput = { suggestInput = it },
                autoApprove = autoApprove,
                onAutoApprove = {
                    autoApprove = it
                    DesktopSettings.update { s -> s.copy(listenTogetherAutoApproval = it) }
                },
                syncVolume = syncVolume,
                onSyncVolume = {
                    syncVolume = it
                    DesktopSettings.update { s -> s.copy(listenTogetherSyncVolume = it) }
                },
                smartResync = smartResync,
                onSmartResync = {
                    smartResync = it
                    DesktopSettings.update { s -> s.copy(listenTogetherSmartResync = it) }
                },
                server = serverInput,
                onServer = {
                    serverInput = it
                    DesktopSettings.update { s -> s.copy(listenTogetherServerUrl = it) }
                },
                copied = copied,
                copiedFailed = copiedFailed,
                linkCopied = linkCopied,
                onCopy = {
                    // A best-effort write with retries: a bare `setContents` can
                    // fail while another process holds the clipboard, and the old
                    // code said "copied" anyway (the "copy does nothing" report).
                    val ok = copyToClipboard(r.roomCode)
                    copied = ok
                    copiedFailed = !ok
                    if (ok) DesktopSnackbar.show(Localization.get(language, "copied_to_clipboard"))
                    scope.launch {
                        kotlinx.coroutines.delay(1800)
                        copied = false
                        copiedFailed = false
                    }
                },
                onCopyLink = {
                    val ok = copyToClipboard("https://vivimusic-listen-together.onrender.com/listen?code=${r.roomCode}")
                    linkCopied = ok
                    if (ok) DesktopSnackbar.show(Localization.get(language, "copied_to_clipboard"))
                    scope.launch { kotlinx.coroutines.delay(1800); linkCopied = false }
                },
                onApproveJoin = { manager.approveJoin(it) },
                onRejectJoin = { manager.rejectJoin(it) },
                onKick = { manager.kickUser(it) },
                onTransferHost = { manager.transferHost(it) },
                onBlock = { user ->
                    // The phone's "permanently block" is block + kick: hiding
                    // someone's requests while they keep listening makes no
                    // sense, so both go out together.
                    manager.blockUser(user.username)
                    manager.kickUser(user.userId, Localization.get(language, "lt_block_user"))
                },
                onRequestSync = { manager.requestSync() },
                onSuggest = {
                    val vid = extractVideoId(suggestInput)
                    if (vid.isNotEmpty()) {
                        scope.launch(Dispatchers.IO) {
                            YouTube.queue(listOf(vid)).onSuccess { q ->
                                val song = q.firstOrNull()
                                if (song != null) {
                                    manager.suggestTrack(
                                        LtTrackInfo(
                                            id = song.id,
                                            title = song.title,
                                            artist = song.artists.joinToString(", ") { it.name },
                                            duration = (song.duration ?: 0) * 1000L,
                                            thumbnail = song.thumbnail,
                                        )
                                    )
                                } else {
                                    manager.suggestTrack(LtTrackInfo(id = vid, title = vid, artist = ""))
                                }
                            }.onFailure {
                                manager.suggestTrack(LtTrackInfo(id = vid, title = vid, artist = ""))
                            }
                        }
                        suggestInput = ""
                    }
                },
                onApproveSuggestion = { manager.approveSuggestion(it) },
                onRejectSuggestion = { manager.rejectSuggestion(it) },
                onLeave = { manager.leaveRoom() },
                onReconnect = { manager.forceReconnect() },
                onDisconnect = { manager.disconnect() },
            )
        }
    }
}

@Composable
internal fun ConnectionBadge(state: LtConnectionState, language: String) {
    val (label, color) = when (state) {
        LtConnectionState.CONNECTED -> Localization.get(language, "connected") to MaterialTheme.colorScheme.primary
        LtConnectionState.CONNECTING -> Localization.get(language, "lt_connecting") to MaterialTheme.colorScheme.tertiary
        LtConnectionState.RECONNECTING -> Localization.get(language, "lt_reconnecting") to MaterialTheme.colorScheme.tertiary
        LtConnectionState.DISCONNECTED, LtConnectionState.ERROR -> Localization.get(language, "disconnected") to MaterialTheme.colorScheme.error
    }
    Card(colors = CardDefaults.cardColors(containerColor = color.copy(alpha = 0.15f))) {
        Row(
            Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            Box(Modifier.size(8.dp).background(color, CircleShape))
            Text(label, style = MaterialTheme.typography.labelMedium, color = color)
        }
    }
}

/**
 * The phone's connection card, minus the card: a Connect button while offline
 * and a Disconnect one while online, plus the reconnect the desktop already had.
 * The room screen used to leave the socket to itself, so a half-dead connection
 * could only be fixed by leaving the app or by finding the reconnect button
 * buried among the room's options.
 */
@Composable
internal fun ConnectionActions(
    language: String,
    state: LtConnectionState,
    onConnect: () -> Unit,
    onDisconnect: () -> Unit,
    onReconnect: () -> Unit,
) {
    Row(
        Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        when (state) {
            LtConnectionState.CONNECTED, LtConnectionState.CONNECTING, LtConnectionState.RECONNECTING ->
                OutlinedButton(onClick = onDisconnect) { Text(Localization.get(language, "disconnect")) }
            LtConnectionState.DISCONNECTED, LtConnectionState.ERROR ->
                Button(onClick = onConnect) { Text(Localization.get(language, "connect")) }
        }
        OutlinedButton(onClick = onReconnect) { Text(Localization.get(language, "lt_reconnect")) }
    }
}

/** A labelled switch row, the shape every Listen Together option uses. */
@Composable
private fun OptionRow(
    label: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
) {
    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
        Text(label, style = MaterialTheme.typography.bodySmall, modifier = Modifier.weight(1f))
        Switch(checked = checked, onCheckedChange = onCheckedChange)
    }
}

@Composable
internal fun LtLobby(
    language: String,
    username: String,
    onUsername: (String) -> Unit,
    avatarIndex: Int,
    onAvatar: (Int) -> Unit,
    roomCode: String,
    onRoomCode: (String) -> Unit,
    server: String,
    onServer: (String) -> Unit,
    autoApprove: Boolean,
    onAutoApprove: (Boolean) -> Unit,
    busy: Boolean,
    error: String?,
    onCreate: () -> Unit,
    onJoin: () -> Unit,
) {
    Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh)) {
        Column(Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text(
                Localization.get(language, "listen_together_description"),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            OutlinedTextField(
                value = username,
                onValueChange = onUsername,
                label = { Text(Localization.get(language, "username")) },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )
            // The avatar picker, like the phone's (index 0 is the initial). It
            // scrolls: fourteen of them do not fit a narrow window, and a picker
            // that clips its own options is worse than one that scrolls.
            Row(
                Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                LT_AVATARS.indices.forEach { index ->
                    Box(Modifier.clickable { onAvatar(index) }) {
                        LtAvatar(avatarIndex = index, username = username, size = 34, selected = index == avatarIndex)
                    }
                }
            }
            OutlinedTextField(
                value = roomCode,
                onValueChange = onRoomCode,
                label = { Text(Localization.get(language, "room_code")) },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )
            OutlinedTextField(
                value = server,
                onValueChange = onServer,
                label = { Text(Localization.get(language, "relay_server")) },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )
            OptionRow(
                label = Localization.get(language, "lt_auto_approve"),
                checked = autoApprove,
                onCheckedChange = onAutoApprove,
            )
            error?.let { SelectionContainer { Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall) } }
            // Single morphing action button, like the mobile app: it CREATES a
            // room when no code is entered and JOINS the typed code when it is
            // complete, so there is no ambiguity about which action fires.
            val joinMode = roomCode.length == 8
            Button(
                enabled = username.isNotBlank() && !busy,
                onClick = { if (joinMode) onJoin() else onCreate() },
                modifier = Modifier.fillMaxWidth(),
            ) {
                if (busy) CircularProgressIndicator(Modifier.size(16.dp), strokeWidth = 2.dp)
                else Text(Localization.get(language, if (joinMode) "join_room" else "create_room"))
            }
        }
    }
}

@Composable
internal fun LtInRoom(
    language: String,
    room: LtRoomState,
    isHost: Boolean,
    myUserId: String?,
    pendingJoin: List<LtJoinRequest>,
    buffering: List<String>,
    pendingSuggestions: List<LtSuggestionReceived>,
    messages: List<LtChatMessage>,
    busy: Boolean,
    error: String?,
    notice: String?,
    suggestInput: String,
    onSuggestInput: (String) -> Unit,
    autoApprove: Boolean,
    onAutoApprove: (Boolean) -> Unit,
    syncVolume: Boolean,
    onSyncVolume: (Boolean) -> Unit,
    smartResync: Boolean,
    onSmartResync: (Boolean) -> Unit,
    server: String,
    onServer: (String) -> Unit,
    copied: Boolean,
    copiedFailed: Boolean,
    linkCopied: Boolean,
    onCopy: () -> Unit,
    onCopyLink: () -> Unit,
    onApproveJoin: (String) -> Unit,
    onRejectJoin: (String) -> Unit,
    onKick: (String) -> Unit,
    onTransferHost: (String) -> Unit,
    onBlock: (LtUserInfo) -> Unit,
    onRequestSync: () -> Unit,
    onSuggest: () -> Unit,
    onApproveSuggestion: (String) -> Unit,
    onRejectSuggestion: (String) -> Unit,
    onLeave: () -> Unit,
    onReconnect: () -> Unit,
    onDisconnect: () -> Unit,
) {
    // Every row below is keyed by the identity of the thing it shows, and a
    // repeated key is a hard crash in Compose ("Key \"...\" was already used"),
    // not a warning: that is how a duplicated room entry took the whole app
    // down. ListenTogether already stores one entry per user (see USER_JOINED),
    // and this is the second line of defence, for a duplicate that reaches the
    // screen through any other path.
    val users = remember(room.users) { room.users.distinctBy { it.userId } }
    val joins = remember(pendingJoin) { pendingJoin.distinctBy { it.userId } }
    val suggestions = remember(pendingSuggestions) { pendingSuggestions.distinctBy { it.suggestionId } }
    val chat = remember(messages) {
        messages.distinctBy { "${it.timestamp}-${it.userId}-${it.message}" }
    }
    Column(Modifier.fillMaxSize()) {
        Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)) {
            Column(Modifier.fillMaxWidth().padding(16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                Text(Localization.get(language, "room_code"), style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onPrimaryContainer)
                Text(
                    room.roomCode,
                    style = MaterialTheme.typography.displaySmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                )
                // The two ways out of the room, as the phone draws them: real
                // buttons with icons, not two lines of text that read like
                // labels. The code is the thing a host actually has to hand
                // out, so it is no longer the least visible control on screen.
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Button(onClick = onCopy) {
                        Icon(Icons.Filled.ContentCopy, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(8.dp))
                        Text(
                            when {
                                copied -> Localization.get(language, "copied_to_clipboard")
                                copiedFailed -> Localization.get(language, "lt_copy_failed")
                                else -> Localization.get(language, "lt_copy_code")
                            },
                        )
                    }
                    OutlinedButton(onClick = onCopyLink) {
                        Icon(Icons.Filled.Link, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(8.dp))
                        Text(if (linkCopied) Localization.get(language, "copied_to_clipboard") else Localization.get(language, "lt_copy_link"))
                    }
                }
                Spacer(Modifier.height(8.dp))
                Text(
                    Localization.get(language, "connected_users") + " (${users.size})",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                )
                if (buffering.isNotEmpty()) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        CircularProgressIndicator(Modifier.size(12.dp), strokeWidth = 2.dp)
                        Text(
                            Localization.get(language, "lt_buffering") + " (${buffering.size})",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onPrimaryContainer,
                        )
                    }
                }
            }
        }

        Spacer(Modifier.height(12.dp))
        // The lock notice sits at the top of a guest's room, next to the "Copia"
        // buttons, and not only buried among the options at the bottom: it is
        // the answer to "why does nothing happen when I press play?".
        if (!isHost) {
            Text(
                "🔒 ${Localization.get(language, "lt_guest_note")}",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.tertiary,
            )
            Spacer(Modifier.height(8.dp))
        }
        error?.let {
            SelectionContainer { Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall) }
            Spacer(Modifier.height(8.dp))
        }
        notice?.let {
            Text(it, color = MaterialTheme.colorScheme.primary, style = MaterialTheme.typography.bodySmall)
            Spacer(Modifier.height(8.dp))
        }

        LazyColumn(Modifier.weight(1f).fillMaxWidth()) {
            // --- Users ---
            item(key = "users_header") {
                Text(
                    Localization.get(language, "connected_users"),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary,
                )
                Spacer(Modifier.height(4.dp))
            }
            items(users, key = { it.userId }) { user ->
                val isMe = user.userId == myUserId
                // The crown follows the ROOM's host, never a per-user flag that may
                // have been captured before a host transfer: that is what left the
                // crown on the old host after "transfer host".
                val userIsHost = user.userId == room.hostId
                Row(
                    Modifier.fillMaxWidth().padding(vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    LtAvatar(avatarIndex = user.avatarIndex, username = user.username, size = 32)
                    Spacer(Modifier.width(10.dp))
                    Text(
                        user.username + if (isMe) " (${Localization.get(language, "lt_you")})" else "",
                        style = MaterialTheme.typography.bodyLarge,
                        modifier = Modifier.weight(1f),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    if (!user.isConnected) {
                        Text("(${Localization.get(language, "disconnected")})", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    if (userIsHost) {
                        Text("👑", style = MaterialTheme.typography.bodyMedium)
                    } else if (isHost) {
                        TextButton(onClick = { onTransferHost(user.userId) }) { Text(Localization.get(language, "lt_transfer_host"), style = MaterialTheme.typography.labelSmall) }
                        Tooltip(Localization.get(language, "lt_kick")) {
                            IconButton(onClick = { onKick(user.userId) }) {
                                Icon(Icons.Filled.Close, contentDescription = Localization.get(language, "lt_kick"), modifier = Modifier.size(16.dp))
                            }
                        }
                        // "Permanently block": the phone has it, and the desktop
                        // had the state and the API but no button at all.
                        Tooltip(Localization.get(language, "lt_block_user")) {
                            IconButton(onClick = { onBlock(user) }) {
                                Icon(Icons.Filled.Block, contentDescription = Localization.get(language, "lt_block_user"), modifier = Modifier.size(16.dp))
                            }
                        }
                    }
                }
            }

            // --- Join requests (host) ---
            if (isHost && pendingJoin.isNotEmpty()) {
                item(key = "joins_header") {
                    Spacer(Modifier.height(8.dp))
                    Text(Localization.get(language, "lt_join_requests"), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                }
                items(joins, key = { "req-${it.userId}" }) { req ->
                    Row(Modifier.fillMaxWidth().padding(vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                        Text("${req.username} " + Localization.get(language, "connect"), modifier = Modifier.weight(1f))
                        TextButton(onClick = { onApproveJoin(req.userId) }) { Text("✓") }
                        TextButton(onClick = { onRejectJoin(req.userId) }) { Text("✕") }
                    }
                }
            }

            // --- Suggestions ---
            item(key = "suggest_header") {
                Spacer(Modifier.height(8.dp))
                Text(Localization.get(language, "suggestions"), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                Spacer(Modifier.height(4.dp))
            }
            if (isHost) {
                if (pendingSuggestions.isEmpty()) {
                    item(key = "no_suggestions") {
                        Text(Localization.get(language, "lt_no_suggestions"), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                } else {
                    items(suggestions, key = { it.suggestionId }) { s ->
                        Row(Modifier.fillMaxWidth().padding(vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                            Column(Modifier.weight(1f)) {
                                Text(s.trackInfo.title, style = MaterialTheme.typography.bodyMedium, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                Text(
                                    "${s.fromUsername} · ${s.trackInfo.artist}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                )
                            }
                            TextButton(onClick = { onApproveSuggestion(s.suggestionId) }) { Text("✓") }
                            TextButton(onClick = { onRejectSuggestion(s.suggestionId) }) { Text("✕") }
                        }
                    }
                }
            } else {
                item(key = "suggest_input") {
                    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                        OutlinedTextField(
                            value = suggestInput,
                            onValueChange = onSuggestInput,
                            placeholder = { Text(Localization.get(language, "lt_suggest_placeholder")) },
                            singleLine = true,
                            modifier = Modifier.weight(1f),
                        )
                        Spacer(Modifier.width(8.dp))
                        Button(onClick = onSuggest, enabled = suggestInput.isNotBlank()) { Text(Localization.get(language, "lt_suggest")) }
                    }
                }
            }

            // --- Settings ---
            // The phone keeps these in Settings -> Integrations -> Listen
            // Together; the desktop has no such screen, so the room itself is
            // where they live. "Smart resync" was read by the manager and shown
            // nowhere at all.
            item(key = "options") {
                Spacer(Modifier.height(8.dp))
                Text(
                    Localization.get(language, "settings"),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary,
                )
                Text(
                    Localization.get(language, "lt_settings_desc"),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(Modifier.height(8.dp))
                OutlinedTextField(
                    value = server,
                    onValueChange = onServer,
                    label = { Text(Localization.get(language, "relay_server")) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
                Spacer(Modifier.height(8.dp))
                OptionRow(
                    label = Localization.get(language, "lt_auto_approve"),
                    checked = autoApprove,
                    onCheckedChange = onAutoApprove,
                )
                if (isHost) {
                    OptionRow(
                        label = Localization.get(language, "lt_sync_volume"),
                        checked = syncVolume,
                        onCheckedChange = onSyncVolume,
                    )
                }
                OptionRow(
                    label = Localization.get(language, "lt_smart_resync"),
                    checked = smartResync,
                    onCheckedChange = onSmartResync,
                )
            }

            // --- Room actions ---
            item(key = "room_actions") {
                Spacer(Modifier.height(12.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    OutlinedButton(onClick = onRequestSync) { Text(Localization.get(language, "lt_request_sync")) }
                    OutlinedButton(onClick = onReconnect) { Text(Localization.get(language, "lt_reconnect")) }
                    OutlinedButton(onClick = onDisconnect) { Text(Localization.get(language, "disconnect")) }
                    Button(onClick = onLeave) { Text(Localization.get(language, "leave_room")) }
                }
            }

            // --- Chat ---
            // The conversation itself lives in its own window (Telegram-style
            // bubbles, quotes in replies, and notifications that honour the
            // notification mode while the window is closed). The room keeps only
            // the door and the unread count, so the player stays the focus here.
            item(key = "chat_header") {
                Spacer(Modifier.height(12.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        Localization.get(language, "comments"),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary,
                    )
                    val unread = ListenTogetherChatWindow.unread.value
                    if (unread > 0) {
                        Spacer(Modifier.width(8.dp))
                        Badge { Text(unread.toString()) }
                    }
                }
            }
            item(key = "chat_open") {
                val last = chat.lastOrNull()
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 4.dp)
                        .clickable { ListenTogetherChatWindow.open() },
                ) {
                    Row(
                        Modifier.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Icon(
                            Icons.AutoMirrored.Filled.Reply,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                        )
                        Spacer(Modifier.width(12.dp))
                        Column(Modifier.weight(1f)) {
                            Text(
                                last?.let { "${it.username}: ${it.message}" }
                                    ?: Localization.get(language, "lt_no_messages"),
                                style = MaterialTheme.typography.bodyMedium,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                            Text(
                                // The shortcut is written on the row that opens the
                                // window, because a keyboard shortcut nobody knows
                                // about is a shortcut nobody uses.
                                Localization.get(language, "comments") + "  ·  Ctrl+Shift+C",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                        SettingsChevron()
                    }
                }
            }
        }
    }
}
