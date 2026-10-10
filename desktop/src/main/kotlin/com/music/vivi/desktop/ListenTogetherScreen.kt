package com.music.vivi.desktop

import androidx.compose.foundation.Image
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
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Reply
import androidx.compose.material.icons.filled.Block
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material.icons.filled.Link
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PersonOff
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Badge
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
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
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.toComposeImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.music.innertube.YouTube
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.util.concurrent.ConcurrentHashMap

/**
 * The phone's avatar set, in the SAME order it stores them (`avatar_index`), so
 * the index that travels over the relay picks the same picture on both
 * platforms. Index 0 is the username's initial (the APK draws its `person`
 * vector there), and 1..13 are the APK's own images, bundled here verbatim:
 * `.ignore/apkmirror` holds them and `desktop/src/main/resources/images/avatars`
 * is where the desktop loads them from, so a user picks the face they pick on
 * the phone instead of a stand-in emoji.
 */
internal val LT_AVATARS: List<String> = listOf(
    "",
    "man.png",
    "woman.png",
    "man_1.png",
    "man_2.png",
    "man_3.png",
    "man_4.png",
    "man_5.png",
    "man_6.png",
    "woman_1.png",
    "woman_2.png",
    "woman_3.png",
    "woman_4.jpg",
    "luxury_women.png",
)

/** The avatar images, decoded once each (a picker redraws on every keystroke). */
private object LtAvatarImages {
    private val cache = ConcurrentHashMap<String, ImageBitmap?>()

    fun load(file: String): ImageBitmap? {
        if (file.isEmpty()) return null
        cache[file]?.let { return it }
        val bitmap = runCatching {
            LtAvatarImages::class.java.getResourceAsStream("/images/avatars/$file")
                ?.use { javax.imageio.ImageIO.read(it)?.toComposeImageBitmap() }
        }.getOrNull()
        cache[file] = bitmap
        return bitmap
    }
}

/**
 * One user's avatar: the username's initial for index 0, the phone's own picture
 * for 1..13. Anything out of range falls back to `person`, the same as the APK.
 */
@Composable
internal fun LtAvatar(
    avatarIndex: Int,
    username: String,
    size: Int = 36,
    selected: Boolean = false,
) {
    val file = LT_AVATARS.getOrNull(avatarIndex).orEmpty()
    val bitmap = if (file.isEmpty()) null else remember(file) { LtAvatarImages.load(file) }
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
                else Modifier.border(1.dp, MaterialTheme.colorScheme.outlineVariant, CircleShape),
            ),
        contentAlignment = Alignment.Center,
    ) {
        when {
            bitmap != null -> Image(
                bitmap = bitmap,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize().clip(CircleShape),
            )
            // Index 0, and the fallback for a picture that could not be read.
            avatarIndex == 0 || file.isEmpty() -> Text(
                username.trim().take(1).uppercase().ifBlank { "?" },
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface,
            )
            else -> Icon(
                Icons.Filled.Person,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size((size * 0.6f).dp),
            )
        }
    }
}

/** The avatar row: the phone's fourteen choices, in its own order. */
@Composable
internal fun LtAvatarPicker(
    avatarIndex: Int,
    username: String,
    onPick: (Int) -> Unit,
    enabled: Boolean = true,
) {
    Row(
        Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        LT_AVATARS.indices.forEach { index ->
            Box(
                Modifier.clickable(enabled = enabled) { onPick(index) },
                contentAlignment = Alignment.Center,
            ) {
                LtAvatar(
                    avatarIndex = index,
                    username = username,
                    size = 40,
                    selected = index == avatarIndex,
                )
            }
        }
    }
}

/** A relay server offered by the phone, with the wording its picker prints. */
internal data class LtServer(
    val name: String,
    val url: String,
    val location: String,
    val operator: String,
)

/**
 * The same two relays the APK offers (`ListenTogetherServers`): the address is a
 * protocol detail, and a user who has the app on their phone should not have to
 * type one to find the other.
 */
internal val LT_SERVERS = listOf(
    LtServer("Hugging Face Sync", "wss://devilmi-vivi-music-listen-together.hf.space", "Global", "VIVIDH"),
    LtServer("ViviMusic Sync Server", "wss://vivimusic-listen-together.onrender.com", "USA", "Vividh"),
)

/** Parses a YouTube watch URL / youtu.be link / bare video id into a video id. */
private fun extractVideoId(input: String): String {
    val raw = input.trim()
    if (raw.isEmpty()) return ""
    val watch = Regex("(?:youtube\\.com|youtu\\.be)/(?:watch\\?v=|shorts/|embed/|live/)?([\\w-]{11})").find(raw)
    if (watch != null) return watch.groupValues[1]
    return if (raw.length == 11) raw else ""
}

/**
 * Listen Together, rebuilt on the phone's own screen.
 *
 * What changed and why, in one place:
 *
 *  * **The avatars are the phone's pictures.** They used to be emoji chosen to
 *    *stand in* for the APK's images, so an index that meant "the fourth man" on
 *    the phone was a guitar here. Both platforms now draw the same file
 *    (`LT_AVATARS`, bundled from the APK).
 *  * **The settings are the phone's settings**, in one section that both the
 *    lobby and the room show: username and avatar, the relay picker with the two
 *    public servers and a custom address, blocked users (with unblock), the
 *    auto-approval switch (greyed for a guest, as the APK greys it), volume sync,
 *    smart resync and the log viewer. The room used to keep four of these in a
 *    column of loose rows and the rest nowhere at all.
 *  * **The layout is one expressive card per idea** — a hero block with the
 *    room code, the users as one grouped card, the requests and suggestions as
 *    their own — instead of headings over a single flat list: everything the
 *    screen can do is a target the user can see.
 *
 * The relay protocol, the reply envelope, the queue lock and the notification
 * behaviour are untouched: this is the same client with the screen it deserved.
 */
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
    val blockedUsers by manager.blockedUsernames.collectAsState()
    val logs by manager.logs.collectAsState()
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

    /** Every switch writes straight through: the manager reads the file. */
    fun saveAvatar(index: Int) {
        avatarIndex = index
        DesktopSettings.update { s -> s.copy(listenTogetherAvatarIndex = index) }
    }

    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            BackButton(language, onBack)
            Spacer(Modifier.weight(1f))
        }
        LtHeader(language)
        Spacer(Modifier.height(16.dp))
        LtConnectionCard(
            language = language,
            state = connectionState,
            onConnect = { manager.connect() },
            onDisconnect = { manager.disconnect() },
            onReconnect = { manager.forceReconnect() },
        )
        Spacer(Modifier.height(16.dp))

        val r = roomState
        if (r == null) {
            LtLobby(
                language = language,
                username = usernameInput,
                onUsername = { usernameInput = it },
                avatarIndex = avatarIndex,
                onAvatar = ::saveAvatar,
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
                blockedUsers = blockedUsers,
                onUnblock = { manager.unblockUser(it) },
                logs = logs,
                onClearLogs = { manager.clearLogs() },
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
                avatarIndex = avatarIndex,
                onAvatar = if (isHost || r.hostId == null) ::saveAvatar else { _ -> },
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
                blockedUsers = blockedUsers,
                onUnblock = { manager.unblockUser(it) },
                logs = logs,
                onClearLogs = { manager.clearLogs() },
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
                                        ),
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
        Spacer(Modifier.height(32.dp))
    }
}

/**
 * The connection pill: a dot in the state's own colour and its name. It lives
 * here because the room screen wears it in its header and the chat window wears
 * it in its title bar — the phone shows the same badge in both places.
 */
@Composable
internal fun ConnectionBadge(state: LtConnectionState, language: String) {
    val (label, color) = when (state) {
        LtConnectionState.CONNECTED -> Localization.get(language, "connected") to MaterialTheme.colorScheme.primary
        LtConnectionState.CONNECTING -> Localization.get(language, "lt_connecting") to MaterialTheme.colorScheme.tertiary
        LtConnectionState.RECONNECTING -> Localization.get(language, "lt_reconnecting") to MaterialTheme.colorScheme.tertiary
        LtConnectionState.DISCONNECTED, LtConnectionState.ERROR ->
            Localization.get(language, "disconnected") to MaterialTheme.colorScheme.error
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

/** The screen's own heading: the phone's icon in a circle, title and subtitle. */
@Composable
internal fun LtHeader(language: String) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Box(
            Modifier
                .size(64.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.primaryContainer),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                Icons.Filled.Groups,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onPrimaryContainer,
                modifier = Modifier.size(34.dp),
            )
        }
        Spacer(Modifier.width(14.dp))
        Column(Modifier.weight(1f)) {
            Text(
                Localization.get(language, "listen_together"),
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
            )
            Text(
                Localization.get(language, "listen_together_description"),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

/** Connection state + the three ways to act on it, as one expressive card. */
@Composable
internal fun LtConnectionCard(
    language: String,
    state: LtConnectionState,
    onConnect: () -> Unit,
    onDisconnect: () -> Unit,
    onReconnect: () -> Unit,
) {
    val (label, color) = when (state) {
        LtConnectionState.CONNECTED -> Localization.get(language, "connected") to MaterialTheme.colorScheme.primary
        LtConnectionState.CONNECTING -> Localization.get(language, "lt_connecting") to MaterialTheme.colorScheme.tertiary
        LtConnectionState.RECONNECTING -> Localization.get(language, "lt_reconnecting") to MaterialTheme.colorScheme.tertiary
        LtConnectionState.DISCONNECTED, LtConnectionState.ERROR ->
            Localization.get(language, "disconnected") to MaterialTheme.colorScheme.error
    }
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh),
        shape = RoundedCornerShape(24.dp),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Row(
            Modifier.fillMaxWidth().padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Box(Modifier.size(10.dp).background(color, CircleShape))
            Column(Modifier.weight(1f)) {
                Text(label, style = MaterialTheme.typography.titleSmall, color = color)
                Text(
                    Localization.get(language, "relay_server"),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            when (state) {
                LtConnectionState.CONNECTED, LtConnectionState.CONNECTING, LtConnectionState.RECONNECTING -> {
                    OutlinedButton(onClick = onDisconnect) { Text(Localization.get(language, "disconnect")) }
                    OutlinedButton(onClick = onReconnect) { Text(Localization.get(language, "lt_reconnect")) }
                }
                LtConnectionState.DISCONNECTED, LtConnectionState.ERROR -> {
                    Button(onClick = onConnect) { Text(Localization.get(language, "connect")) }
                }
            }
        }
    }
}

/** A switch row shaped like every other option card in the app. */
@Composable
private fun OptionRow(
    label: String,
    checked: Boolean,
    enabled: Boolean = true,
    description: String? = null,
    onCheckedChange: (Boolean) -> Unit,
) {
    Row(
        Modifier.fillMaxWidth().padding(vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f)) {
            Text(
                label,
                style = MaterialTheme.typography.bodyMedium,
                color = if (enabled) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant,
            )
            description?.let {
                Text(
                    it,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        Switch(
            checked = checked,
            enabled = enabled,
            onCheckedChange = onCheckedChange,
            thumbContent = {
                Icon(
                    if (checked) Icons.Filled.Check else Icons.Filled.Close,
                    contentDescription = null,
                    modifier = Modifier.size(SwitchDefaults.IconSize),
                )
            },
        )
    }
}

/**
 * The phone's Listen Together settings, in one section both the lobby and the
 * room show: the account (username + avatar), the relay, the blocked list, the
 * three switches and the log viewer.
 */
@Composable
internal fun LtSettingsSection(
    language: String,
    username: String,
    onUsername: ((String) -> Unit)?,
    avatarIndex: Int,
    onAvatar: (Int) -> Unit,
    server: String,
    onServer: (String) -> Unit,
    autoApprove: Boolean,
    autoApproveEnabled: Boolean,
    onAutoApprove: (Boolean) -> Unit,
    syncVolume: Boolean,
    syncVolumeVisible: Boolean,
    onSyncVolume: (Boolean) -> Unit,
    smartResync: Boolean,
    onSmartResync: (Boolean) -> Unit,
    blockedUsers: Set<String>,
    onUnblock: (String) -> Unit,
    logs: List<LtLogEntry>,
    onClearLogs: () -> Unit,
) {
    var showBlocked by remember { mutableStateOf(false) }
    var showLogs by remember { mutableStateOf(false) }
    var serverMenu by remember { mutableStateOf(false) }
    var customServerOpen by remember { mutableStateOf(false) }
    var customServer by remember(server) { mutableStateOf(server) }

    Text(
        Localization.get(language, "settings"),
        style = MaterialTheme.typography.titleMedium,
        fontWeight = FontWeight.Bold,
        modifier = Modifier.padding(top = 20.dp, bottom = 2.dp),
    )
    Text(
        Localization.get(language, "lt_settings_desc"),
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
    Spacer(Modifier.height(8.dp))

    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh),
        shape = RoundedCornerShape(24.dp),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(Modifier.padding(16.dp)) {
            // --- the account: avatar + name, exactly the phone's first row -----
            Row(verticalAlignment = Alignment.CenterVertically) {
                LtAvatar(avatarIndex = avatarIndex, username = username, size = 46)
                Spacer(Modifier.width(12.dp))
                if (onUsername != null) {
                    OutlinedTextField(
                        value = username,
                        onValueChange = onUsername,
                        label = { Text(Localization.get(language, "lt_username")) },
                        placeholder = { Text(Localization.get(language, "lt_enter_username")) },
                        singleLine = true,
                        modifier = Modifier.weight(1f),
                    )
                } else {
                    Column {
                        Text(
                            username.ifBlank { Localization.get(language, "not_set") },
                            style = MaterialTheme.typography.titleSmall,
                        )
                        Text(
                            Localization.get(language, "lt_username_locked"),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }
            if (onUsername != null) {
                Spacer(Modifier.height(10.dp))
                LtAvatarPicker(
                    avatarIndex = avatarIndex,
                    username = username,
                    onPick = onAvatar,
                )
            }

            HorizontalDivider(Modifier.padding(vertical = 14.dp))

            // --- the relay: the phone's picker, plus a custom address ----------
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    Icons.Filled.Sync,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(18.dp),
                )
                Spacer(Modifier.width(10.dp))
                Column(Modifier.weight(1f)) {
                    Text(Localization.get(language, "lt_server_url"), style = MaterialTheme.typography.bodyMedium)
                    Text(
                        server.ifBlank { LT_SERVERS.first().url },
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
                TextButton(onClick = { serverMenu = true }) {
                    Text(Localization.get(language, "lt_choose_server"))
                }
                DropdownMenu(expanded = serverMenu, onDismissRequest = { serverMenu = false }) {
                    LT_SERVERS.forEach { known ->
                        DropdownMenuItem(
                            text = {
                                Column {
                                    Text(known.name)
                                    Text(
                                        "${known.location} · ${known.operator}",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    )
                                }
                            },
                            onClick = {
                                serverMenu = false
                                onServer(known.url)
                            },
                        )
                    }
                    HorizontalDivider()
                    DropdownMenuItem(
                        text = { Text(Localization.get(language, "lt_custom_server")) },
                        onClick = {
                            serverMenu = false
                            customServerOpen = true
                        },
                    )
                }
            }
            if (customServerOpen) {
                Spacer(Modifier.height(8.dp))
                OutlinedTextField(
                    value = customServer,
                    onValueChange = { customServer = it },
                    label = { Text(Localization.get(language, "relay_server")) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
                Row(Modifier.padding(top = 8.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(
                        onClick = {
                            onServer(customServer.trim())
                            customServerOpen = false
                        },
                        enabled = customServer.isNotBlank(),
                    ) { Text(Localization.get(language, "lt_use_custom_server")) }
                    TextButton(onClick = { customServerOpen = false }) {
                        Text(Localization.get(language, "close"))
                    }
                }
            }

            HorizontalDivider(Modifier.padding(vertical = 14.dp))

            // --- the three switches, in the phone's order ----------------------
            OptionRow(
                label = Localization.get(language, "lt_auto_approve"),
                checked = autoApprove,
                // The APK greys this one for a guest: the host owns admission.
                enabled = autoApproveEnabled,
                onCheckedChange = onAutoApprove,
            )
            if (syncVolumeVisible) {
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

            HorizontalDivider(Modifier.padding(vertical = 14.dp))

            // --- blocked users and the log viewer ------------------------------
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    Icons.Filled.PersonOff,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(18.dp),
                )
                Spacer(Modifier.width(10.dp))
                Column(Modifier.weight(1f)) {
                    Text(Localization.get(language, "lt_blocked_users"), style = MaterialTheme.typography.bodyMedium)
                    Text(
                        if (blockedUsers.isEmpty()) Localization.get(language, "lt_no_blocked_users")
                        else blockedUsers.size.toString(),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                TextButton(
                    onClick = { showBlocked = true },
                    enabled = blockedUsers.isNotEmpty(),
                ) { Text(Localization.get(language, "lt_unblock")) }
            }
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(top = 6.dp)) {
                Icon(
                    Icons.Filled.Link,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(18.dp),
                )
                Spacer(Modifier.width(10.dp))
                Column(Modifier.weight(1f)) {
                    Text(Localization.get(language, "lt_view_logs"), style = MaterialTheme.typography.bodyMedium)
                    Text(
                        Localization.get(language, "lt_view_logs_desc"),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                TextButton(onClick = { showLogs = true }) {
                    Text(Localization.get(language, "lt_logs"))
                }
            }
        }
    }

    if (showBlocked) {
        LtBlockedUsersDialog(
            language = language,
            blockedUsers = blockedUsers,
            onUnblock = onUnblock,
            onDismiss = { showBlocked = false },
        )
    }
    if (showLogs) {
        LtLogsDialog(
            language = language,
            logs = logs,
            onClear = onClearLogs,
            onDismiss = { showLogs = false },
        )
    }
}

/** The blocked list, with the phone's own Unblock action per row. */
@Composable
private fun LtBlockedUsersDialog(
    language: String,
    blockedUsers: Set<String>,
    onUnblock: (String) -> Unit,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(Localization.get(language, "lt_blocked_users")) },
        text = {
            if (blockedUsers.isEmpty()) {
                Text(Localization.get(language, "lt_no_blocked_users"))
            } else {
                LazyColumn(
                    Modifier.height(260.dp).fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    items(blockedUsers.toList(), key = { it }) { name ->
                        Row(
                            Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(14.dp))
                                .background(MaterialTheme.colorScheme.surfaceContainerHigh)
                                .padding(horizontal = 12.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Text(name, modifier = Modifier.weight(1f), maxLines = 1, overflow = TextOverflow.Ellipsis)
                            TextButton(onClick = { onUnblock(name) }) {
                                Text(Localization.get(language, "lt_unblock"))
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(onClick = onDismiss) { Text(Localization.get(language, "close")) }
        },
    )
}

/** The client's own log, the phone's "View logs" screen: copy or clear. */
@Composable
private fun LtLogsDialog(
    language: String,
    logs: List<LtLogEntry>,
    onClear: () -> Unit,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(Localization.get(language, "lt_logs")) },
        text = {
            if (logs.isEmpty()) {
                Text(Localization.get(language, "lt_no_logs"))
            } else {
                val listState = androidx.compose.foundation.lazy.rememberLazyListState()
                LaunchedEffect(logs.size) { listState.animateScrollToItem((logs.size - 1).coerceAtLeast(0)) }
                LazyColumn(state = listState, modifier = Modifier.height(320.dp).fillMaxWidth()) {
                    items(logs) { entry ->
                        Column(Modifier.fillMaxWidth().padding(vertical = 2.dp)) {
                            Text(
                                "[${entry.level}] ${entry.message}",
                                style = MaterialTheme.typography.bodySmall,
                                fontFamily = FontFamily.Monospace,
                            )
                            entry.detail?.let {
                                Text(
                                    it,
                                    style = MaterialTheme.typography.labelSmall,
                                    fontFamily = FontFamily.Monospace,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    maxLines = 2,
                                    overflow = TextOverflow.Ellipsis,
                                )
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    val text = logs.joinToString("\n") { "${it.level} ${it.message}" + (it.detail?.let { d -> " — $d" } ?: "") }
                    if (copyToClipboard(text)) DesktopSnackbar.show(Localization.get(language, "copied_to_clipboard"))
                },
                enabled = logs.isNotEmpty(),
            ) { Text(Localization.get(language, "lt_copy_logs")) }
        },
        dismissButton = {
            Row {
                TextButton(onClick = onClear, enabled = logs.isNotEmpty()            ) { Text(Localization.get(language, "lt_clear_logs")) }
                TextButton(onClick = onDismiss) { Text(Localization.get(language, "close")) }
            }
        },
    )
}

/**
 * The lobby: who you are, the room code, and the same settings section the room
 * shows. One card per idea, the phone's own order — profile, then the code.
 */
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
    syncVolume: Boolean,
    onSyncVolume: (Boolean) -> Unit,
    smartResync: Boolean,
    onSmartResync: (Boolean) -> Unit,
    blockedUsers: Set<String>,
    onUnblock: (String) -> Unit,
    logs: List<LtLogEntry>,
    onClearLogs: () -> Unit,
    busy: Boolean,
    error: String?,
    onCreate: () -> Unit,
    onJoin: () -> Unit,
) {
    // One morphing action, like the phone: a complete code JOINS, an empty one
    // CREATES, so there is no ambiguity about which button does what.
    val joinMode = roomCode.length == 8
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh),
        shape = RoundedCornerShape(28.dp),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(Modifier.fillMaxWidth().padding(18.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text(
                Localization.get(language, "room_code"),
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.primary,
            )
            // The phone draws the code as eight boxes; so does this, and the
            // field above is what fills them.
            Row(horizontalArrangement = Arrangement.spacedBy(4.dp), modifier = Modifier.fillMaxWidth()) {
                repeat(8) { index ->
                    val char = roomCode.getOrNull(index)?.toString().orEmpty()
                    Box(
                        Modifier
                            .weight(1f)
                            .height(48.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(
                                if (char.isNotEmpty()) MaterialTheme.colorScheme.primaryContainer
                                else MaterialTheme.colorScheme.surfaceContainerHighest,
                            )
                            .border(
                                1.dp,
                                if (char.isNotEmpty()) MaterialTheme.colorScheme.primary.copy(alpha = 0.4f)
                                else MaterialTheme.colorScheme.outlineVariant,
                                RoundedCornerShape(12.dp),
                            ),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(char, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    }
                }
            }
            OutlinedTextField(
                value = roomCode,
                onValueChange = onRoomCode,
                label = { Text(Localization.get(language, "room_code")) },
                placeholder = { Text(Localization.get(language, "code_placeholder")) },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )
            Text(
                Localization.get(language, "lt_create_room_desc"),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            error?.let {
                SelectionContainer {
                    Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
                }
            }
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

    LtSettingsSection(
        language = language,
        username = username,
        onUsername = onUsername,
        avatarIndex = avatarIndex,
        onAvatar = onAvatar,
        server = server,
        onServer = onServer,
        autoApprove = autoApprove,
        autoApproveEnabled = true,
        onAutoApprove = onAutoApprove,
        syncVolume = syncVolume,
        syncVolumeVisible = true,
        onSyncVolume = onSyncVolume,
        smartResync = smartResync,
        onSmartResync = onSmartResync,
        blockedUsers = blockedUsers,
        onUnblock = onUnblock,
        logs = logs,
        onClearLogs = onClearLogs,
    )
}

/**
 * The room: the code as a hero card, the users as one grouped card, the requests
 * and the suggestions as their own, then the actions and the same settings
 * section the lobby uses.
 *
 * Every row is keyed by the identity of the thing it shows, and a repeated key
 * is a hard crash in Compose ("Key ... was already used"), not a warning: that is
 * how a duplicated room entry took the whole app down. ListenTogether already
 * stores one entry per user (see USER_JOINED), and the `distinctBy` calls are the
 * second line of defence, for a duplicate that reaches the screen any other way.
 */
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
    avatarIndex: Int,
    onAvatar: (Int) -> Unit,
    autoApprove: Boolean,
    onAutoApprove: (Boolean) -> Unit,
    syncVolume: Boolean,
    onSyncVolume: (Boolean) -> Unit,
    smartResync: Boolean,
    onSmartResync: (Boolean) -> Unit,
    server: String,
    onServer: (String) -> Unit,
    blockedUsers: Set<String>,
    onUnblock: (String) -> Unit,
    logs: List<LtLogEntry>,
    onClearLogs: () -> Unit,
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
    val users = remember(room.users) { room.users.distinctBy { it.userId } }
    val joins = remember(pendingJoin) { pendingJoin.distinctBy { it.userId } }
    val suggestions = remember(pendingSuggestions) { pendingSuggestions.distinctBy { it.suggestionId } }
    val chat = remember(messages) {
        messages.distinctBy { "${it.timestamp}-${it.userId}-${it.message}" }
    }

    // --- the room code, as the phone's hero card -----------------------------
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
        shape = RoundedCornerShape(28.dp),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(
            Modifier.fillMaxWidth().padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(
                Localization.get(language, "room_code"),
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onPrimaryContainer,
            )
            Text(
                room.roomCode,
                style = MaterialTheme.typography.displaySmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onPrimaryContainer,
            )
            Spacer(Modifier.height(10.dp))
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
            Spacer(Modifier.height(10.dp))
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    Localization.get(language, "connected_users") + " (${users.size})",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                )
                if (buffering.isNotEmpty()) {
                    CircularProgressIndicator(
                        Modifier.size(12.dp),
                        strokeWidth = 2.dp,
                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                    )
                    Text(
                        Localization.get(language, "lt_buffering") + " (${buffering.size})",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                    )
                }
            }
        }
    }

    if (!isHost) {
        Spacer(Modifier.height(12.dp))
        Card(
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.tertiaryContainer),
            shape = RoundedCornerShape(20.dp),
            modifier = Modifier.fillMaxWidth(),
        ) {
            Row(Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    Icons.Filled.Lock,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onTertiaryContainer,
                    modifier = Modifier.size(18.dp),
                )
                Spacer(Modifier.width(10.dp))
                Text(
                    Localization.get(language, "lt_guest_note"),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onTertiaryContainer,
                )
            }
        }
    }

    error?.let {
        Spacer(Modifier.height(10.dp))
        SelectionContainer {
            Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
        }
    }
    notice?.let {
        Spacer(Modifier.height(10.dp))
        Text(it, color = MaterialTheme.colorScheme.primary, style = MaterialTheme.typography.bodySmall)
    }

    // --- the users ------------------------------------------------------------
    Spacer(Modifier.height(18.dp))
    Text(
        Localization.get(language, "connected_users"),
        style = MaterialTheme.typography.titleMedium,
        fontWeight = FontWeight.Bold,
    )
    Spacer(Modifier.height(8.dp))
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh),
        shape = RoundedCornerShape(24.dp),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
            users.forEachIndexed { index, user ->
                if (index > 0) HorizontalDivider(Modifier.padding(horizontal = 14.dp))
                var menu by remember(user.userId) { mutableStateOf(false) }
                Row(
                    Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    LtAvatar(avatarIndex = user.avatarIndex, username = user.username, size = 38)
                    Spacer(Modifier.width(12.dp))
                    Column(Modifier.weight(1f)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                user.username,
                                style = MaterialTheme.typography.bodyLarge,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                            if (user.userId == room.hostId) {
                                Spacer(Modifier.width(6.dp))
                                Icon(
                                    Icons.Filled.Star,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(15.dp),
                                )
                            }
                            if (user.userId == myUserId) {
                                Spacer(Modifier.width(6.dp))
                                Text(
                                    "(${Localization.get(language, "lt_you")})",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                        }
                        if (!user.isConnected) {
                            Text(
                                Localization.get(language, "disconnected"),
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                    // The host's own actions live behind one "⋮", the shape the
                    // phone's long-press dialog takes, so a row stays a row.
                    if (isHost && user.userId != room.hostId) {
                        Box {
                            IconButton(onClick = { menu = true }) {
                                Icon(
                                    Icons.Filled.MoreVert,
                                    contentDescription = Localization.get(language, "settings"),
                                )
                            }
                            DropdownMenu(expanded = menu, onDismissRequest = { menu = false }) {
                                DropdownMenuItem(
                                    text = { Text(Localization.get(language, "lt_transfer_host")) },
                                    leadingIcon = { Icon(Icons.Filled.SwapHoriz, contentDescription = null) },
                                    onClick = {
                                        menu = false
                                        onTransferHost(user.userId)
                                    },
                                )
                                DropdownMenuItem(
                                    text = { Text(Localization.get(language, "lt_kick")) },
                                    leadingIcon = { Icon(Icons.Filled.Close, contentDescription = null) },
                                    onClick = {
                                        menu = false
                                        onKick(user.userId)
                                    },
                                )
                                DropdownMenuItem(
                                    text = { Text(Localization.get(language, "lt_block_user")) },
                                    leadingIcon = { Icon(Icons.Filled.Block, contentDescription = null) },
                                    onClick = {
                                        menu = false
                                        onBlock(user)
                                    },
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    // --- join requests (host) -------------------------------------------------
    if (isHost && joins.isNotEmpty()) {
        Spacer(Modifier.height(18.dp))
        Text(
            Localization.get(language, "lt_join_requests"),
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
        )
        Spacer(Modifier.height(8.dp))
        Card(
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh),
            shape = RoundedCornerShape(24.dp),
            modifier = Modifier.fillMaxWidth(),
        ) {
            Column(Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
                joins.forEachIndexed { index, req ->
                    if (index > 0) HorizontalDivider(Modifier.padding(horizontal = 14.dp))
                    Row(
                        Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        // LtJoinRequest carries no avatar in the wire format, so
                        // the picture comes from the room entry if the relay has
                        // already announced the user, and the initial otherwise.
                        LtAvatar(
                            avatarIndex = users.firstOrNull { it.userId == req.userId }?.avatarIndex ?: 0,
                            username = req.username,
                            size = 34,
                        )
                        Spacer(Modifier.width(12.dp))
                        Column(Modifier.weight(1f)) {
                            Text(req.username, style = MaterialTheme.typography.bodyLarge, maxLines = 1)
                            Text(
                                Localization.get(language, "lt_waiting_approval"),
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                        IconButton(onClick = { onApproveJoin(req.userId) }) {
                            Icon(
                                Icons.Filled.Check,
                                contentDescription = Localization.get(language, "lt_approve"),
                                tint = MaterialTheme.colorScheme.primary,
                            )
                        }
                        IconButton(onClick = { onRejectJoin(req.userId) }) {
                            Icon(
                                Icons.Filled.Close,
                                contentDescription = Localization.get(language, "lt_reject"),
                                tint = MaterialTheme.colorScheme.error,
                            )
                        }
                    }
                }
            }
        }
    }

    // --- suggestions ----------------------------------------------------------
    Spacer(Modifier.height(18.dp))
    Text(
        Localization.get(language, "suggestions"),
        style = MaterialTheme.typography.titleMedium,
        fontWeight = FontWeight.Bold,
    )
    Spacer(Modifier.height(8.dp))
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh),
        shape = RoundedCornerShape(24.dp),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(Modifier.fillMaxWidth().padding(14.dp)) {
            if (isHost) {
                if (suggestions.isEmpty()) {
                    Text(
                        Localization.get(language, "lt_no_suggestions"),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                } else {
                    suggestions.forEachIndexed { index, s ->
                        if (index > 0) HorizontalDivider(Modifier.padding(vertical = 6.dp))
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Column(Modifier.weight(1f)) {
                                Text(
                                    s.trackInfo.title,
                                    style = MaterialTheme.typography.bodyMedium,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                )
                                Text(
                                    "${s.fromUsername} · ${s.trackInfo.artist}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                )
                            }
                            IconButton(onClick = { onApproveSuggestion(s.suggestionId) }) {
                                Icon(
                                    Icons.Filled.Check,
                                    contentDescription = Localization.get(language, "lt_approve"),
                                    tint = MaterialTheme.colorScheme.primary,
                                )
                            }
                            IconButton(onClick = { onRejectSuggestion(s.suggestionId) }) {
                                Icon(
                                    Icons.Filled.Close,
                                    contentDescription = Localization.get(language, "lt_reject"),
                                    tint = MaterialTheme.colorScheme.error,
                                )
                            }
                        }
                    }
                }
            } else {
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    OutlinedTextField(
                        value = suggestInput,
                        onValueChange = onSuggestInput,
                        placeholder = { Text(Localization.get(language, "lt_suggest_placeholder")) },
                        singleLine = true,
                        modifier = Modifier.weight(1f),
                    )
                    Spacer(Modifier.width(8.dp))
                    Button(onClick = onSuggest, enabled = suggestInput.isNotBlank() && !busy) {
                        Text(Localization.get(language, "lt_suggest"))
                    }
                }
            }
        }
    }

    // --- the room's own actions ----------------------------------------------
    Spacer(Modifier.height(18.dp))
    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        OutlinedButton(onClick = onRequestSync, enabled = !busy) {
            Text(Localization.get(language, "lt_request_sync"))
        }
        OutlinedButton(onClick = onReconnect) { Text(Localization.get(language, "lt_reconnect")) }
        OutlinedButton(onClick = onDisconnect) { Text(Localization.get(language, "disconnect")) }
        Button(onClick = onLeave) { Text(Localization.get(language, "leave_room")) }
    }

    // --- chat -----------------------------------------------------------------
    Spacer(Modifier.height(18.dp))
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text(
            Localization.get(language, "comments"),
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
        )
        val unread = ListenTogetherChatWindow.unread.value
        if (unread > 0) {
            Spacer(Modifier.width(8.dp))
            Badge { Text(unread.toString()) }
        }
    }
    Spacer(Modifier.height(8.dp))
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh),
        shape = RoundedCornerShape(20.dp),
        modifier = Modifier.fillMaxWidth().clickable { ListenTogetherChatWindow.open() },
    ) {
        Row(Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(
                Icons.AutoMirrored.Filled.Reply,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
            )
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(
                    chat.lastOrNull()?.let { "${it.username}: ${it.message}" }
                        ?: Localization.get(language, "lt_no_messages"),
                    style = MaterialTheme.typography.bodyMedium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    // The shortcut is written on the row that opens the window,
                    // because a keyboard shortcut nobody knows about is a
                    // shortcut nobody uses.
                    Localization.get(language, "comments") + "  ·  Ctrl+Shift+C",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            SettingsChevron()
        }
    }

    // --- the phone's settings, the same section the lobby shows ---------------
    LtSettingsSection(
        language = language,
        username = users.firstOrNull { it.userId == myUserId }?.username.orEmpty(),
        // A guest cannot rename themselves in a room (the phone says so too).
        onUsername = null,
        avatarIndex = avatarIndex,
        onAvatar = onAvatar,
        server = server,
        onServer = onServer,
        autoApprove = autoApprove,
        autoApproveEnabled = isHost,
        onAutoApprove = onAutoApprove,
        syncVolume = syncVolume,
        syncVolumeVisible = isHost,
        onSyncVolume = onSyncVolume,
        smartResync = smartResync,
        onSmartResync = onSmartResync,
        blockedUsers = blockedUsers,
        onUnblock = onUnblock,
        logs = logs,
        onClearLogs = onClearLogs,
    )
}
