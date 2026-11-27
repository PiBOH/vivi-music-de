package com.music.vivi.desktop.presentation.screens.settings

import com.music.vivi.desktop.generated.resources.Res
import com.music.vivi.desktop.generated.resources.ic_rounded_account_circle
import com.music.vivi.desktop.generated.resources.ic_rounded_auto_awesome
import com.music.vivi.desktop.generated.resources.ic_rounded_graphic_eq
import com.music.vivi.desktop.generated.resources.ic_rounded_info
import com.music.vivi.desktop.generated.resources.ic_rounded_keyboard
import com.music.vivi.desktop.generated.resources.ic_rounded_library_music
import com.music.vivi.desktop.generated.resources.ic_rounded_music_note
import com.music.vivi.desktop.generated.resources.ic_rounded_palette
import com.music.vivi.desktop.generated.resources.ic_rounded_settings
import com.music.vivi.desktop.generated.resources.ic_rounded_settings_backup_restore
import com.music.vivi.desktop.generated.resources.ic_rounded_tune
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.dp
import org.jetbrains.compose.resources.DrawableResource

enum class SettingsSection(
    val id: String,
    val title: String,
    val subtitle: String,
    val icon: DrawableResource
) {
    LIBRARY(
        id = "library",
        title = "Music Management",
        subtitle = "Manage folders, refresh library, parsing options",
        icon = Res.drawable.ic_rounded_library_music
    ),
    APPEARANCE(
        id = "appearance",
        title = "Appearance",
        subtitle = "Themes, layout, and visual styles",
        icon = Res.drawable.ic_rounded_palette
    ),
    PLAYBACK(
        id = "playback",
        title = "Playback",
        subtitle = "Audio behavior, crossfade, and background play",
        icon = Res.drawable.ic_rounded_music_note
    ),
    EQUALIZER(
        id = "equalizer",
        title = "Equalizer",
        subtitle = "10-band frequencies, preamp, and audio presets",
        icon = Res.drawable.ic_rounded_graphic_eq
    ),
    ACCOUNTS(
        id = "accounts",
        title = "Accounts",
        subtitle = "YouTube Music, Navidrome, and Jellyfin",
        icon = Res.drawable.ic_rounded_account_circle
    ),
    SHORTCUTS(
        id = "shortcuts",
        title = "Keyboard Shortcuts",
        subtitle = "Hotkeys, playback controls, and navigation cheatsheet",
        icon = Res.drawable.ic_rounded_keyboard
    ),
    BACKUP(
        id = "backup",
        title = "Backup & Restore",
        subtitle = "Export and recover your personal data",
        icon = Res.drawable.ic_rounded_settings_backup_restore
    ),
    ABOUT(
        id = "about",
        title = "About",
        subtitle = "App info, version, and credits",
        icon = Res.drawable.ic_rounded_info
    ),
    // 1.55-only pages. They are filtered out of the 1.56 hub, which keeps the
    // PixelPlayerDesktop surface exactly as it was.
    INTRO(
        id = "intro",
        title = "Intro",
        subtitle = "Animated splash at startup",
        icon = Res.drawable.ic_rounded_auto_awesome
    ),
    // The 1.56 settings hub, moved into a submenu of the imported 1.55 home.
    PIXELPLAYER(
        id = "pixelplayer",
        title = "PixelPlayer",
        subtitle = "The 1.56 PixelPlayerDesktop settings, unchanged",
        icon = Res.drawable.ic_rounded_tune
    )
}

fun getCategoryColors(category: SettingsSection, isDark: Boolean): Pair<Color, Color> {
    return if (isDark) {
        when (category) {
            SettingsSection.LIBRARY -> Color(0xFF004A77) to Color(0xFFC2E7FF)
            SettingsSection.APPEARANCE -> Color(0xFF7D5260) to Color(0xFFFFD8E4)
            SettingsSection.PLAYBACK -> Color(0xFF633B48) to Color(0xFFFFD8EC)
            SettingsSection.EQUALIZER -> Color(0xFF6E4E13) to Color(0xFFFFDEAC)
            SettingsSection.ACCOUNTS -> Color(0xFF37474F) to Color(0xFFBBD9E8)
            SettingsSection.SHORTCUTS -> Color(0xFF2E4369) to Color(0xFFBFD2F8)
            SettingsSection.BACKUP -> Color(0xFF3B4869) to Color(0xFFD9E2FF)
            SettingsSection.ABOUT -> Color(0xFF3F474D) to Color(0xFFDEE3EB)
            SettingsSection.INTRO -> Color(0xFF4A3B63) to Color(0xFFE7D8FF)
            SettingsSection.PIXELPLAYER -> Color(0xFF243B53) to Color(0xFFD4E4F7)
        }
    } else {
        when (category) {
            SettingsSection.LIBRARY -> Color(0xFFD7E3FF) to Color(0xFF005AC1)
            SettingsSection.APPEARANCE -> Color(0xFFFFD8E4) to Color(0xFF631835)
            SettingsSection.PLAYBACK -> Color(0xFFFFD8EC) to Color(0xFF631B4B)
            SettingsSection.EQUALIZER -> Color(0xFFFFDEAC) to Color(0xFF281900)
            SettingsSection.ACCOUNTS -> Color(0xFFD6EAF5) to Color(0xFF103548)
            SettingsSection.SHORTCUTS -> Color(0xFFD6E3FF) to Color(0xFF285EA7)
            SettingsSection.BACKUP -> Color(0xFFD9E2FF) to Color(0xFF27304E)
            SettingsSection.ABOUT -> Color(0xFFEFF1F7) to Color(0xFF44474F)
            SettingsSection.INTRO -> Color(0xFFE9DDFF) to Color(0xFF4B2D7A)
            SettingsSection.PIXELPLAYER -> Color(0xFFDCE8F5) to Color(0xFF26476B)
        }
    }
}

fun getGroupShape(index: Int, totalItems: Int): Shape = when {
    totalItems <= 1 -> RoundedCornerShape(24.dp)
    index == 0 -> RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp, bottomStart = 4.dp, bottomEnd = 4.dp)
    index == totalItems - 1 -> RoundedCornerShape(topStart = 4.dp, topEnd = 4.dp, bottomStart = 24.dp, bottomEnd = 24.dp)
    else -> RoundedCornerShape(4.dp)
}
