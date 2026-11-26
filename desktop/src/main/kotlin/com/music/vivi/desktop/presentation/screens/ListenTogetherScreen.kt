package com.music.vivi.desktop.presentation.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.jetbrains.compose.resources.painterResource

import com.music.vivi.desktop.data.listentogether.ListenTogetherManager
import com.music.vivi.desktop.data.listentogether.LtConnectionState
import com.music.vivi.desktop.data.listentogether.LtRoomRole
import com.music.vivi.desktop.data.listentogether.LtUserInfo
import com.music.vivi.desktop.generated.resources.Res
import com.music.vivi.desktop.generated.resources.ic_rounded_add
import com.music.vivi.desktop.generated.resources.ic_rounded_check_circle
import com.music.vivi.desktop.generated.resources.ic_rounded_group
import com.music.vivi.desktop.generated.resources.ic_rounded_person
import com.music.vivi.desktop.generated.resources.ic_rounded_public
import com.music.vivi.desktop.presentation.components.subcomps.AppTooltip
import com.music.vivi.desktop.presentation.viewmodel.PlayerViewModel

/**
 * Listen together: start a party or join one with a six-character code, and
 * watch the room while the host drives the music. The layout follows the
 * two-card lobby the desktop already had, drawn with the clone's Material 3
 * surfaces.
 */
@Composable
fun ListenTogetherScreen(
    viewModel: PlayerViewModel,
    manager: ListenTogetherManager,
    onBack: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val room by manager.roomState.collectAsState()
    val role by manager.role.collectAsState()
    val connection by manager.connectionState.collectAsState()
    val busy by manager.busy.collectAsState()
    val latency by manager.latencyMs.collectAsState()
    val serverOnline by manager.serverOnline.collectAsState()
    val error by manager.lastError.collectAsState()

    val savedUsername by viewModel.listenTogetherUsername.collectAsState()
    val savedPartySize by viewModel.listenTogetherPartySize.collectAsState()
    val savedServerUrl by viewModel.listenTogetherServerUrl.collectAsState()

    var nickname by remember(savedUsername) { mutableStateOf(savedUsername) }
    var joinNickname by remember(savedUsername) { mutableStateOf(savedUsername) }
    var partySize by remember(savedPartySize) { mutableStateOf(savedPartySize.coerceIn(2, 10)) }
    var code by remember { mutableStateOf("") }
    var serverAddress by remember(savedServerUrl) {
        mutableStateOf(if (savedServerUrl == ListenTogetherManager.DEFAULT_SERVER_URL) "" else savedServerUrl)
    }

    LaunchedEffect(savedPartySize) { partySize = savedPartySize.coerceIn(2, 10) }
    // One probe when the screen opens: the manager no longer keeps a socket open
    // for an app that never uses Listen Together.
    LaunchedEffect(Unit) {
        if (serverOnline == null) {
            manager.testServer(savedServerUrl.ifBlank { ListenTogetherManager.DEFAULT_SERVER_URL })
        }
    }

    val colors = MaterialTheme.colorScheme

    BoxWithConstraints(
        modifier = modifier
            .fillMaxSize()
            .background(colors.surface)
    ) {
        val horizontalPadding = if (maxWidth < 700.dp) 16.dp else 28.dp

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = horizontalPadding, vertical = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .widthIn(max = 1080.dp),
                verticalArrangement = Arrangement.spacedBy(20.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    Surface(
                        shape = CircleShape,
                        color = colors.primaryContainer,
                        modifier = Modifier.size(52.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                painter = painterResource(Res.drawable.ic_rounded_group),
                                contentDescription = null,
                                tint = colors.onPrimaryContainer,
                                modifier = Modifier.size(26.dp)
                            )
                        }
                    }
                    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                        Text(
                            text = "Listen together",
                            style = MaterialTheme.typography.headlineMedium,
                            fontWeight = FontWeight.Bold,
                            color = colors.onSurface
                        )
                        Text(
                            text = "Start a room or join friends with a six-character code.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = colors.onSurfaceVariant
                        )
                    }
                }

                if (error != null) {
                    Surface(
                        shape = MaterialTheme.shapes.medium,
                        color = colors.errorContainer,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Text(
                                text = error.orEmpty(),
                                style = MaterialTheme.typography.bodyMedium,
                                color = colors.onErrorContainer,
                                modifier = Modifier.weight(1f)
                            )
                            TextButton(onClick = { manager.clearError() }) {
                                Text("Dismiss")
                            }
                        }
                    }
                }

                if (room == null) {
                    BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
                        if (maxWidth >= 820.dp) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(16.dp),
                                verticalAlignment = Alignment.Top
                            ) {
                                StartPartyCard(
                                    nickname = nickname,
                                    onNicknameChange = { nickname = it.take(32) },
                                    partySize = partySize,
                                    onPartySizeChange = { partySize = it.coerceIn(2, 10) },
                                    busy = busy,
                                    modifier = Modifier.weight(1f),
                                    onStart = {
                                        manager.startParty(
                                            nickname.ifBlank { "Listener" },
                                            partySize
                                        )
                                    }
                                )
                                JoinPartyCard(
                                    nickname = joinNickname,
                                    onNicknameChange = { joinNickname = it.take(32) },
                                    code = code,
                                    onCodeChange = { value ->
                                        code = value.uppercase().filter { it.isLetterOrDigit() }.take(6)
                                    },
                                    busy = busy,
                                    modifier = Modifier.weight(1f),
                                    onJoin = {
                                        manager.joinParty(
                                            code,
                                            joinNickname.ifBlank { "Listener" }
                                        )
                                    }
                                )
                            }
                        } else {
                            Column(
                                modifier = Modifier.fillMaxWidth(),
                                verticalArrangement = Arrangement.spacedBy(16.dp)
                            ) {
                                StartPartyCard(
                                    nickname = nickname,
                                    onNicknameChange = { nickname = it.take(32) },
                                    partySize = partySize,
                                    onPartySizeChange = { partySize = it.coerceIn(2, 10) },
                                    busy = busy,
                                    onStart = {
                                        manager.startParty(
                                            nickname.ifBlank { "Listener" },
                                            partySize
                                        )
                                    }
                                )
                                JoinPartyCard(
                                    nickname = joinNickname,
                                    onNicknameChange = { joinNickname = it.take(32) },
                                    code = code,
                                    onCodeChange = { value ->
                                        code = value.uppercase().filter { it.isLetterOrDigit() }.take(6)
                                    },
                                    busy = busy,
                                    onJoin = {
                                        manager.joinParty(
                                            code,
                                            joinNickname.ifBlank { "Listener" }
                                        )
                                    }
                                )
                            }
                        }
                    }

                    PartyServerCard(
                        address = serverAddress,
                        onAddressChange = { serverAddress = it },
                        serverOnline = serverOnline,
                        latencyMs = latency,
                        connection = connection,
                        onTest = {
                            manager.testServer(
                                serverAddress.trim().ifBlank { ListenTogetherManager.DEFAULT_SERVER_URL }
                            )
                        },
                        onUseBuiltIn = {
                            serverAddress = ""
                            manager.useBuiltInServer()
                        }
                    )
                } else {
                    RoomCard(
                        roomCode = room?.roomCode.orEmpty(),
                        users = room?.users.orEmpty(),
                        hostId = room?.hostId.orEmpty(),
                        isHost = role == LtRoomRole.HOST,
                        connection = connection,
                        onLeave = { manager.leaveParty() }
                    )
                }
            }
        }
    }
}

@Composable
private fun StartPartyCard(
    nickname: String,
    onNicknameChange: (String) -> Unit,
    partySize: Int,
    onPartySizeChange: (Int) -> Unit,
    busy: Boolean,
    onStart: () -> Unit,
    modifier: Modifier = Modifier
) {
    LobbyCard(
        title = "Start a party",
        subtitle = "Choose how friends see you and how many can join.",
        icon = Res.drawable.ic_rounded_add,
        modifier = modifier
    ) {
        PartyTextField(
            value = nickname,
            onValueChange = onNicknameChange,
            label = "Nickname",
            placeholder = "Your name"
        )

        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text(
                        text = "Party size",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "2–10 people",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    AppTooltip("Fewer people") {
                        FilledTonalButton(
                            onClick = { onPartySizeChange(partySize - 1) },
                            enabled = partySize > 2,
                            modifier = Modifier.size(40.dp),
                            contentPadding = androidx.compose.foundation.layout.PaddingValues(0.dp)
                        ) {
                            Text("−", style = MaterialTheme.typography.titleLarge)
                        }
                    }
                    Text(
                        text = partySize.toString(),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.width(24.dp)
                    )
                    AppTooltip("More people") {
                        FilledTonalButton(
                            onClick = { onPartySizeChange(partySize + 1) },
                            enabled = partySize < 10,
                            modifier = Modifier.size(40.dp),
                            contentPadding = androidx.compose.foundation.layout.PaddingValues(0.dp)
                        ) {
                            Icon(
                                painter = painterResource(Res.drawable.ic_rounded_add),
                                contentDescription = "More people",
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                }
            }
        }

        Button(
            onClick = onStart,
            enabled = nickname.isNotBlank() && !busy,
            modifier = Modifier.fillMaxWidth()
        ) {
            if (busy) {
                CircularProgressIndicator(
                    modifier = Modifier.size(18.dp),
                    strokeWidth = 2.dp,
                    color = MaterialTheme.colorScheme.onPrimary
                )
                Spacer(modifier = Modifier.width(10.dp))
            }
            Text("Start party")
        }
    }
}

@Composable
private fun JoinPartyCard(
    nickname: String,
    onNicknameChange: (String) -> Unit,
    code: String,
    onCodeChange: (String) -> Unit,
    busy: Boolean,
    onJoin: () -> Unit,
    modifier: Modifier = Modifier
) {
    LobbyCard(
        title = "Join a party",
        subtitle = "Enter the code the host gave you.",
        icon = Res.drawable.ic_rounded_person,
        modifier = modifier
    ) {
        PartyTextField(
            value = nickname,
            onValueChange = onNicknameChange,
            label = "Nickname",
            placeholder = "Your name"
        )

        PartyTextField(
            value = code,
            onValueChange = onCodeChange,
            label = "Party code",
            placeholder = "ABC123",
            monospace = true
        )

        FilledTonalButton(
            onClick = onJoin,
            enabled = code.length == 6 && nickname.isNotBlank() && !busy,
            modifier = Modifier.fillMaxWidth()
        ) {
            if (busy) {
                CircularProgressIndicator(
                    modifier = Modifier.size(18.dp),
                    strokeWidth = 2.dp,
                    color = MaterialTheme.colorScheme.onSecondaryContainer
                )
                Spacer(modifier = Modifier.width(10.dp))
            }
            Text("Join party")
        }
    }
}

@Composable
private fun PartyServerCard(
    address: String,
    onAddressChange: (String) -> Unit,
    serverOnline: Boolean?,
    latencyMs: Long?,
    connection: LtConnectionState,
    onTest: () -> Unit,
    onUseBuiltIn: () -> Unit
) {
    LobbyCard(
        title = "Party server",
        subtitle = "Leave blank to use the server included with this build.",
        icon = Res.drawable.ic_rounded_public
    ) {
        PartyTextField(
            value = address,
            onValueChange = onAddressChange,
            label = "Custom server address",
            placeholder = ListenTogetherManager.DEFAULT_SERVER_URL
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            val (dotColor, label) = when {
                serverOnline == true ->
                    MaterialTheme.colorScheme.primary to
                        "Party server online${latencyMs?.let { " · $it ms" }.orEmpty()}"
                serverOnline == false ->
                    MaterialTheme.colorScheme.error to "Party server offline"
                connection == LtConnectionState.CONNECTED ->
                    MaterialTheme.colorScheme.primary to "Party server online"
                else ->
                    MaterialTheme.colorScheme.onSurfaceVariant to "Not tested"
            }

            Box(
                modifier = Modifier
                    .size(8.dp)
                    .clip(CircleShape)
                    .background(dotColor)
            )
            Text(
                text = label,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.weight(1f)
            )

            TextButton(onClick = onTest) { Text("Test") }
            FilledTonalButton(onClick = onUseBuiltIn) { Text("Use built-in") }
        }
    }
}

@Composable
private fun RoomCard(
    roomCode: String,
    users: List<LtUserInfo>,
    hostId: String,
    isHost: Boolean,
    connection: LtConnectionState,
    onLeave: () -> Unit
) {
    var copied by remember { mutableStateOf(false) }
    LaunchedEffect(copied) {
        if (copied) {
            kotlinx.coroutines.delay(1600)
            copied = false
        }
    }

    Surface(
        shape = RoundedCornerShape(24.dp),
        color = MaterialTheme.colorScheme.surfaceContainer,
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(18.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    painter = painterResource(Res.drawable.ic_rounded_check_circle),
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(20.dp)
                )
                Text(
                    text = if (isHost) "You are hosting" else "You joined the party",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }

            Text(
                text = roomCode,
                style = MaterialTheme.typography.displaySmall.copy(
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 10.sp
                ),
                color = MaterialTheme.colorScheme.onSurface
            )

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = when (connection) {
                        LtConnectionState.CONNECTED -> "Connected"
                        LtConnectionState.CONNECTING -> "Connecting…"
                        LtConnectionState.RECONNECTING -> "Reconnecting…"
                        LtConnectionState.ERROR -> "Connection error"
                        LtConnectionState.DISCONNECTED -> "Disconnected"
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                TextButton(onClick = {
                    runCatching {
                        java.awt.Toolkit.getDefaultToolkit().systemClipboard.setContents(
                            java.awt.datatransfer.StringSelection(roomCode),
                            null
                        )
                    }
                    copied = true
                }) {
                    Text(if (copied) "Copied" else "Copy code")
                }
            }

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .widthIn(max = 520.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = "${users.size} ${if (users.size == 1) "listener" else "listeners"}",
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                users.forEach { user ->
                    Surface(
                        shape = RoundedCornerShape(14.dp),
                        color = MaterialTheme.colorScheme.surfaceContainerHigh,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Surface(
                                shape = CircleShape,
                                color = MaterialTheme.colorScheme.primaryContainer,
                                modifier = Modifier.size(32.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Text(
                                        text = user.username.take(1).uppercase(),
                                        style = MaterialTheme.typography.titleSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onPrimaryContainer
                                    )
                                }
                            }
                            Text(
                                text = user.username,
                                style = MaterialTheme.typography.bodyLarge,
                                color = MaterialTheme.colorScheme.onSurface,
                                modifier = Modifier.weight(1f)
                            )
                            if (user.userId == hostId) {
                                Text(
                                    text = "Host",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                            if (!user.isConnected) {
                                Text(
                                    text = "Away",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
            }

            if (!isHost) {
                Text(
                    text = "The host controls playback while you are in the party.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center
                )
            }

            TextButton(onClick = onLeave) {
                Text("Leave party", color = MaterialTheme.colorScheme.error)
            }
        }
    }
}

@Composable
private fun LobbyCard(
    title: String,
    subtitle: String,
    icon: org.jetbrains.compose.resources.DrawableResource,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(24.dp),
        color = MaterialTheme.colorScheme.surfaceContainer,
        modifier = modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.primaryContainer),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        painter = painterResource(icon),
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onPrimaryContainer,
                        modifier = Modifier.size(20.dp)
                    )
                }
                Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Text(
                        text = title,
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = subtitle,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            content()
        }
    }
}

@Composable
private fun PartyTextField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    placeholder: String,
    monospace: Boolean = false
) {
    androidx.compose.material3.OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(label) },
        placeholder = { Text(placeholder, color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)) },
        singleLine = true,
        shape = MaterialTheme.shapes.medium,
        textStyle = MaterialTheme.typography.bodyLarge.copy(
            fontFamily = if (monospace) FontFamily.Monospace else FontFamily.Default,
            letterSpacing = if (monospace) 4.sp else 0.sp
        ),
        modifier = Modifier.fillMaxWidth()
    )
}
