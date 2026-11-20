package com.music.vivi.desktop.presentation.screens.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.music.vivi.desktop.data.youtube.BrowserCookies
import com.music.vivi.desktop.generated.resources.Res
import com.music.vivi.desktop.generated.resources.ic_rounded_account_circle
import com.music.vivi.desktop.presentation.viewmodel.YouTubeAccountUiState
import org.jetbrains.compose.resources.painterResource

/**
 * The YouTube Music account, in the Accounts section.
 *
 * Signing in reuses the session a browser on this machine already holds: the
 * user picks a profile, its cookie jar is adopted and every innertube request is
 * then signed with it. Signing out returns to the guest identity, which still
 * searches and plays.
 */
@Composable
fun YouTubeAccountCard(
    state: YouTubeAccountUiState,
    onFindBrowsers: () -> Unit,
    onUseProfile: (BrowserCookies.Profile) -> Unit,
    onDisconnect: () -> Unit,
    onWebSignIn: () -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = MaterialTheme.colorScheme
    val subtitle = when {
        state.isBusy && !state.isSignedIn -> "Reading the browser cookie stores..."
        state.isSignedIn -> listOfNotNull(state.name, state.email, state.handle)
            .firstOrNull { it.isNotBlank() }
            ?: "Signed in" + if (state.isBusy) " (reading the account...)" else ""
        else -> "Sign in with the session your browser already holds"
    }

    Surface(
        shape = RoundedCornerShape(20.dp),
        color = colors.surfaceContainer,
        modifier = modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    painter = painterResource(Res.drawable.ic_rounded_account_circle),
                    contentDescription = null,
                    tint = if (state.isSignedIn) colors.primary else colors.secondary,
                    modifier = Modifier.size(24.dp)
                )
                Spacer(Modifier.width(16.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "YouTube Music",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = colors.onSurface
                    )
                    Spacer(Modifier.height(4.dp))
                    Text(
                        text = subtitle,
                        style = MaterialTheme.typography.bodyMedium,
                        color = colors.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                Spacer(Modifier.width(12.dp))
                if (state.isSignedIn) {
                    TextButton(onClick = onDisconnect) {
                        Text("Sign out")
                    }
                } else {
                    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        TextButton(onClick = onWebSignIn, enabled = !state.isBusy) {
                            Text("WebView")
                        }
                        Button(onClick = onFindBrowsers, enabled = !state.isBusy) {
                            Text("Find a signed-in browser")
                        }
                    }
                }
            }

            if (!state.isSignedIn) {
                Text(
                    text = "The in-app window opens Google's sign-in page. If Google refuses it, " +
                        "use the session of a browser that is already signed in.",
                    style = MaterialTheme.typography.bodySmall,
                    color = colors.onSurfaceVariant
                )
            }

            if (state.profiles.isNotEmpty() && !state.isSignedIn) {
                Text(
                    text = "Choose the browser whose YouTube session to use",
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
