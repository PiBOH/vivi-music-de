package com.music.vivi.desktop.presentation.screens.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.music.vivi.desktop.data.youtube.BrowserCookies
import com.music.vivi.desktop.data.youtube.YouTubeAccount
import com.music.vivi.desktop.generated.resources.Res
import com.music.vivi.desktop.generated.resources.ic_rounded_account_circle
import com.music.vivi.desktop.generated.resources.ic_rounded_add
import com.music.vivi.desktop.generated.resources.ic_rounded_check
import com.music.vivi.desktop.generated.resources.ic_rounded_delete
import com.music.vivi.desktop.presentation.components.ArtworkSize
import com.music.vivi.desktop.presentation.components.AsyncArtworkImage
import com.music.vivi.desktop.presentation.components.subcomps.AppTooltip
import com.music.vivi.desktop.presentation.viewmodel.YouTubeAccountUiState
import com.music.vivi.desktop.presentation.viewmodel.YouTubeSessionStatus
import org.jetbrains.compose.resources.painterResource

/**
 * The YouTube Music accounts, in the Accounts section.
 *
 * The header is the identity the app is acting as right now and how its session
 * stands; the rows under it are every saved identity, one click away from being
 * the active one. Adding an account opens the in-app window (or adopts a signed
 * in browser), and the switch is a hot swap: the next catalogue request in this
 * window already uses the new cookie.
 */
@Composable
fun YouTubeAccountCard(
    state: YouTubeAccountUiState,
    onFindBrowsers: () -> Unit,
    onUseProfile: (BrowserCookies.Profile) -> Unit,
    onDisconnect: () -> Unit,
    onWebSignIn: () -> Unit,
    onSwitchAccount: (String) -> Unit = {},
    onRemoveAccount: (String) -> Unit = {},
    onRefreshSession: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val colors = MaterialTheme.colorScheme
    val active = state.accounts.firstOrNull { it.isActive }
    val title = when {
        active != null -> active.name
        state.isSignedIn -> state.name ?: "YouTube Music account"
        else -> "YouTube Music"
    }
    val detail = when {
        active != null -> listOfNotNull(active.email, active.handle)
            .filter { it.isNotBlank() }
            .distinct()
            .joinToString(" · ")
        state.isSignedIn -> listOfNotNull(state.email, state.handle)
            .filter { it.isNotBlank() }
            .distinct()
            .joinToString(" · ")
        else -> ""
    }

    Surface(
        shape = RoundedCornerShape(20.dp),
        color = colors.surfaceContainer,
        modifier = modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                AccountAvatar(
                    url = active?.thumbnailUrl ?: state.thumbnailUrl,
                    size = 48.dp
                )
                Spacer(Modifier.width(14.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = title,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = colors.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    if (detail.isNotBlank()) {
                        Spacer(Modifier.height(2.dp))
                        Text(
                            text = detail,
                            style = MaterialTheme.typography.bodySmall,
                            color = colors.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                    Spacer(Modifier.height(6.dp))
                    SessionStatusChip(status = state.status)
                }
                Spacer(Modifier.width(12.dp))
                if (state.isSignedIn) {
                    TextButton(onClick = onDisconnect, enabled = !state.isBusy) {
                        Text("Sign out")
                    }
                }
            }

            if (state.accounts.isNotEmpty()) {
                Text(
                    text = "Saved accounts",
                    style = MaterialTheme.typography.labelMedium,
                    color = colors.onSurfaceVariant
                )
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    state.accounts.forEach { account ->
                        AccountRow(
                            account = account,
                            enabled = !state.isBusy,
                            onSwitch = { onSwitchAccount(account.id) },
                            onRemove = { onRemoveAccount(account.id) }
                        )
                    }
                }
            }

            if (state.status == YouTubeSessionStatus.EXPIRED || state.status == YouTubeSessionStatus.ERROR) {
                TextButton(onClick = onRefreshSession, enabled = !state.isBusy) {
                    Text("Check the session again")
                }
            }

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Button(onClick = onWebSignIn, enabled = !state.isBusy) {
                    Icon(
                        painter = painterResource(Res.drawable.ic_rounded_add),
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(if (state.isSignedIn) "Add account" else "Sign in")
                }
                TextButton(onClick = onFindBrowsers, enabled = !state.isBusy) {
                    Text("Find a signed-in browser")
                }
            }

            if (!state.isSignedIn) {
                Text(
                    text = "The in-app window opens Google's sign-in page. If Google refuses it, " +
                        "use the session of a browser that is already signed in. Every account " +
                        "you add stays on this computer, sealed, and can be switched without a restart.",
                    style = MaterialTheme.typography.bodySmall,
                    color = colors.onSurfaceVariant
                )
            }

            if (state.profiles.isNotEmpty()) {
                Text(
                    text = "Choose the browser whose YouTube session to add",
                    style = MaterialTheme.typography.labelMedium,
                    color = colors.onSurfaceVariant
                )
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    state.profiles.forEach { profile ->
                        TextButton(onClick = { onUseProfile(profile) }) {
                            Text(profile.label, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        }
                    }
                }
            }

            state.message?.let { message ->
                Text(
                    text = message,
                    style = MaterialTheme.typography.bodySmall,
                    color = colors.error
                )
            }
        }
    }
}

@Composable
private fun AccountRow(
    account: YouTubeAccount.Summary,
    enabled: Boolean,
    onSwitch: () -> Unit,
    onRemove: () -> Unit
) {
    val colors = MaterialTheme.colorScheme
    Surface(
        shape = MaterialTheme.shapes.medium,
        color = if (account.isActive) {
            colors.primaryContainer.copy(alpha = 0.55f)
        } else {
            colors.surfaceContainerHigh
        },
        modifier = Modifier
            .fillMaxWidth()
            .clickable(enabled = enabled && !account.isActive, onClick = onSwitch)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            AccountAvatar(url = account.thumbnailUrl, size = 36.dp)
            Spacer(Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = account.name,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = if (account.isActive) FontWeight.Bold else FontWeight.Medium,
                    color = colors.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                val detail = listOfNotNull(account.email, account.handle)
                    .filter { it.isNotBlank() }
                    .distinct()
                    .joinToString(" · ")
                if (detail.isNotBlank()) {
                    Text(
                        text = detail,
                        style = MaterialTheme.typography.bodySmall,
                        color = colors.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
            Spacer(Modifier.width(8.dp))
            if (account.isActive) {
                Icon(
                    painter = painterResource(Res.drawable.ic_rounded_check),
                    contentDescription = "Active account",
                    tint = colors.primary,
                    modifier = Modifier.size(20.dp)
                )
            } else {
                TextButton(onClick = onSwitch, enabled = enabled) {
                    Text("Switch")
                }
                AppTooltip(text = "Remove account") {
                    IconButton(onClick = onRemove, enabled = enabled) {
                        Icon(
                            painter = painterResource(Res.drawable.ic_rounded_delete),
                            contentDescription = "Remove account",
                            tint = colors.onSurfaceVariant,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun AccountAvatar(url: String?, size: Dp) {
    Box(modifier = Modifier.size(size)) {
        AsyncArtworkImage(
            artworkUri = url,
            targetSize = ArtworkSize.SMALL,
            shape = CircleShape,
            placeholderIcon = Res.drawable.ic_rounded_account_circle,
            placeholderIconSize = size * 0.55f,
            modifier = Modifier.size(size)
        )
    }
}

@Composable
private fun SessionStatusChip(status: YouTubeSessionStatus) {
    val colors = MaterialTheme.colorScheme
    val container: Color
    val content: Color
    val label: String
    when (status) {
        YouTubeSessionStatus.GUEST -> {
            label = "Signed out"
            container = colors.surfaceContainerHighest
            content = colors.onSurfaceVariant
        }
        YouTubeSessionStatus.CHECKING -> {
            label = "Checking session..."
            container = colors.secondaryContainer
            content = colors.onSecondaryContainer
        }
        YouTubeSessionStatus.ACTIVE -> {
            label = "Session active"
            container = colors.primaryContainer
            content = colors.onPrimaryContainer
        }
        YouTubeSessionStatus.EXPIRED -> {
            label = "Session expired"
            container = colors.errorContainer
            content = colors.onErrorContainer
        }
        YouTubeSessionStatus.ERROR -> {
            label = "Session check failed"
            container = colors.errorContainer
            content = colors.onErrorContainer
        }
    }
    Surface(shape = RoundedCornerShape(999.dp), color = container) {
        Row(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(6.dp)
                    .clip(CircleShape)
                    .background(content.copy(alpha = 0.9f))
            )
            Spacer(Modifier.width(6.dp))
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = content
            )
        }
    }
}
