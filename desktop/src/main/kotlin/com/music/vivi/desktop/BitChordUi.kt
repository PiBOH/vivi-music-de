package com.music.vivi.desktop

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * The colours measured on BitChord's desktop window. They are used by the
 * cloned chrome only; VIVI's Settings keep the tonal scheme.
 */
internal object BitChordColors {
    val Chrome = Color(0xFF171717)
    val Page = Color(0xFF202020)
    val Divider = Color(0xFF121213)
    val Card = Color(0xFF0C0C0C)
    val CardBorder = Color(0xFF141414)
    val Search = Color(0xFF1D1D1D)
    val SearchBorder = Color(0xFF242424)
    val Hover = Color(0xFF242424)
    val Secondary = Color(0xFF8E8E93)
    val Replay = Color(0xFFFA2D48)

    /** Chrome for the current light/dark mode. */
    @Composable
    fun chrome(): Color = if (isAppInDarkTheme()) Chrome else MaterialTheme.colorScheme.surface

    /** Page background for the current light/dark mode. */
    @Composable
    fun page(): Color = if (isAppInDarkTheme()) Page else MaterialTheme.colorScheme.background

    /** Secondary text for the current light/dark mode. */
    @Composable
    fun secondary(): Color = if (isAppInDarkTheme()) Secondary else MaterialTheme.colorScheme.onSurfaceVariant
}

/**
 * BitChord's bottom control, narrowed to VIVI's two destinations: the account
 * half opens Account, the settings half opens VIVI's Settings. Collapsed to the
 * 72 dp rail, the same two destinations become two icon targets.
 */
@Composable
internal fun AccountSettingsPill(
    language: String,
    collapsed: Boolean,
    userName: String,
    userHandle: String,
    settingsSelected: Boolean,
    onAccount: () -> Unit,
    onSettings: () -> Unit,
) {
    val dark = isAppInDarkTheme()
    val fill = if (dark) BitChordColors.Search else MaterialTheme.colorScheme.surfaceContainerHigh
    val border = if (dark) BitChordColors.SearchBorder else MaterialTheme.colorScheme.outlineVariant
    val primary = if (dark) Color.White else MaterialTheme.colorScheme.onSurface
    val secondary = BitChordColors.secondary()
    val settingsLabel = Localization.get(language, "settings")

    if (collapsed) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Tooltip(userName) {
                BitChordAvatar(
                    userName = userName,
                    selected = currentAccountDestination(settingsSelected),
                    modifier = Modifier.size(26.dp).clickable { onAccount() },
                )
            }
            Spacer(Modifier.width(6.dp))
            Tooltip(settingsLabel) {
                Box(
                    modifier = Modifier.size(26.dp).clip(CircleShape).clickable { onSettings() },
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        Icons.Outlined.Settings,
                        contentDescription = settingsLabel,
                        tint = if (settingsSelected) primary else secondary,
                        modifier = Modifier.size(16.dp),
                    )
                }
            }
        }
        return
    }

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(50))
            .clickable { onAccount() },
        color = fill,
        border = BorderStroke(1.dp, border),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Row(
                modifier = Modifier
                    .weight(1f)
                    .clickable { onAccount() }
                    .padding(horizontal = 9.dp, vertical = 7.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                BitChordAvatar(userName = userName, selected = !settingsSelected, modifier = Modifier.size(26.dp))
                Spacer(Modifier.width(7.dp))
                Column(Modifier.weight(1f)) {
                    Tooltip(userName) {
                        Text(
                            userName,
                            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold, fontSize = 12.sp),
                            color = primary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                    Text(
                        userHandle,
                        style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                        color = secondary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
            Box(Modifier.width(1.dp).height(24.dp).background(border))
            Row(
                modifier = Modifier
                    .clickable { onSettings() }
                    .padding(horizontal = 9.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(
                    Icons.Outlined.Settings,
                    contentDescription = settingsLabel,
                    tint = if (settingsSelected) primary else secondary,
                    modifier = Modifier.size(15.dp),
                )
                Spacer(Modifier.width(4.dp))
                Text(
                    settingsLabel,
                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold, fontSize = 10.sp),
                    color = if (settingsSelected) primary else secondary,
                    maxLines = 1,
                )
            }
        }
    }
}

/** The avatar's selected ring follows whether Account is the current page. */
private fun currentAccountDestination(settingsSelected: Boolean): Boolean = !settingsSelected

@Composable
private fun BitChordAvatar(userName: String, selected: Boolean, modifier: Modifier = Modifier) {
    val dark = isAppInDarkTheme()
    val disc = if (dark) {
        if (selected) Color.White else BitChordColors.Hover
    } else {
        if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceContainerHighest
    }
    val ink = if (dark) {
        if (selected) Color.Black else Color.White
    } else {
        if (selected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface
    }
    Box(modifier.clip(CircleShape).background(disc), contentAlignment = Alignment.Center) {
        Text(
            userName.take(1).uppercase().ifBlank { "?" },
            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold, fontSize = 11.sp),
            color = ink,
        )
    }
}
