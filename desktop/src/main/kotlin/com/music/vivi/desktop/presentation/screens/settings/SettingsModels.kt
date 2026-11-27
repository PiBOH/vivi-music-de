package com.music.vivi.desktop.presentation.screens.settings

import com.music.vivi.desktop.generated.resources.Res
import com.music.vivi.desktop.generated.resources.ic_lyrics
import com.music.vivi.desktop.generated.resources.ic_outlined_auto_graph
import com.music.vivi.desktop.generated.resources.ic_outlined_bolt
import com.music.vivi.desktop.generated.resources.ic_outlined_security
import com.music.vivi.desktop.generated.resources.ic_rounded_abc
import com.music.vivi.desktop.generated.resources.ic_rounded_account_circle
import com.music.vivi.desktop.generated.resources.ic_rounded_auto_awesome
import com.music.vivi.desktop.generated.resources.ic_rounded_auto_fix_high
import com.music.vivi.desktop.generated.resources.ic_rounded_cloud_download
import com.music.vivi.desktop.generated.resources.ic_rounded_code
import com.music.vivi.desktop.generated.resources.ic_rounded_computer
import com.music.vivi.desktop.generated.resources.ic_rounded_dataset
import com.music.vivi.desktop.generated.resources.ic_rounded_dark_mode
import com.music.vivi.desktop.generated.resources.ic_rounded_delete
import com.music.vivi.desktop.generated.resources.ic_rounded_font_download
import com.music.vivi.desktop.generated.resources.ic_rounded_image
import com.music.vivi.desktop.generated.resources.ic_rounded_layers
import com.music.vivi.desktop.generated.resources.ic_rounded_public
import com.music.vivi.desktop.generated.resources.ic_rounded_refresh
import com.music.vivi.desktop.generated.resources.ic_rounded_schedule
import com.music.vivi.desktop.generated.resources.ic_rounded_sync
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
    // The imported 1.55 tree. Every one of these entries opens a 1.55 page; the
    // 1.56 sections above stay reachable only inside PixelPlayer.
    V155_UPDATES(
        id = "v155_updates",
        title = "Updates",
        subtitle = "Channel, interval and update source",
        icon = Res.drawable.ic_rounded_refresh
    ),
    V155_ACCOUNT(
        id = "v155_account",
        title = "Account",
        subtitle = "YouTube Music account",
        icon = Res.drawable.ic_rounded_account_circle
    ),
    V155_NOTIFICATIONS(
        id = "v155_notifications",
        title = "Notifications",
        subtitle = "Mode, history and widgets",
        icon = Res.drawable.ic_rounded_schedule
    ),
    V155_APPEARANCE(
        id = "v155_appearance",
        title = "Appearance",
        subtitle = "Theme, font, canvas and player design",
        icon = Res.drawable.ic_rounded_palette
    ),
    V155_LYRICS(
        id = "v155_lyrics",
        title = "Lyrics",
        subtitle = "Style, position and romanization",
        icon = Res.drawable.ic_lyrics
    ),
    V155_PLAYER(
        id = "v155_player",
        title = "Player & audio",
        subtitle = "Queue, crossfade, quality and equalizer",
        icon = Res.drawable.ic_rounded_music_note
    ),
    V155_DEVICES(
        id = "v155_devices",
        title = "Device sync",
        subtitle = "Pairing and volume sync",
        icon = Res.drawable.ic_rounded_sync
    ),
    V155_CONTENT(
        id = "v155_content",
        title = "Content",
        subtitle = "Language, country and filters",
        icon = Res.drawable.ic_rounded_public
    ),
    V155_LANGUAGE(
        id = "v155_language",
        title = "Language",
        subtitle = "The language of the app",
        icon = Res.drawable.ic_rounded_abc
    ),
    V155_AI(
        id = "v155_ai",
        title = "AI lyrics & translation",
        subtitle = "Provider, key and translation",
        icon = Res.drawable.ic_rounded_auto_fix_high
    ),
    V155_PRIVACY(
        id = "v155_privacy",
        title = "Privacy",
        subtitle = "History, search and sign-out",
        icon = Res.drawable.ic_outlined_security
    ),
    V155_STORAGE(
        id = "v155_storage",
        title = "Storage",
        subtitle = "Caches and library folders",
        icon = Res.drawable.ic_rounded_dataset
    ),
    V155_DATA_SAVER(
        id = "v155_data_saver",
        title = "Data saver",
        subtitle = "Less data and canvas backups",
        icon = Res.drawable.ic_outlined_bolt
    ),
    V155_BACKUP(
        id = "v155_backup",
        title = "Backup & restore",
        subtitle = "Automatic backups and restore",
        icon = Res.drawable.ic_rounded_settings_backup_restore
    ),
    V155_WRAPPED(
        id = "v155_wrapped",
        title = "Wrapped",
        subtitle = "Your listening year",
        icon = Res.drawable.ic_outlined_auto_graph
    ),
    V155_INTEGRATIONS(
        id = "v155_integrations",
        title = "Integrations",
        subtitle = "Discord, Last.fm and Listen Together",
        icon = Res.drawable.ic_rounded_tune
    ),
    V155_SPOTIFY(
        id = "v155_spotify",
        title = "Spotify import",
        subtitle = "Import playlists from Spotify",
        icon = Res.drawable.ic_rounded_cloud_download
    ),
    V155_DESKTOP(
        id = "v155_desktop",
        title = "Desktop features",
        subtitle = "Tray, media keys and widget",
        icon = Res.drawable.ic_rounded_computer
    ),
    V155_SYSTEM(
        id = "v155_system",
        title = "System",
        subtitle = "Performance, developer tools and intro",
        icon = Res.drawable.ic_rounded_settings
    ),
    V155_ABOUT(
        id = "v155_about",
        title = "About",
        subtitle = "Version, contributors and licenses",
        icon = Res.drawable.ic_rounded_info
    ),
    V155_THEME(
        id = "v155_theme",
        title = "Theme",
        subtitle = "Mode, accent and palette",
        icon = Res.drawable.ic_rounded_dark_mode
    ),
    V155_FONT(
        id = "v155_font",
        title = "Font",
        subtitle = "App font and lyrics",
        icon = Res.drawable.ic_rounded_font_download
    ),
    V155_CANVAS(
        id = "v155_canvas",
        title = "Canvas",
        subtitle = "Animated artwork",
        icon = Res.drawable.ic_rounded_image
    ),
    V155_PLAYER_DESIGN(
        id = "v155_player_design",
        title = "Player design",
        subtitle = "Player, mini player and slider",
        icon = Res.drawable.ic_rounded_layers
    ),
    V155_DENSITY(
        id = "v155_density",
        title = "Density & grid",
        subtitle = "Interface scale and grid size",
        icon = Res.drawable.ic_rounded_tune
    ),
    V155_TRANSITIONS(
        id = "v155_transitions",
        title = "Screen transitions",
        subtitle = "Fade, slide or off",
        icon = Res.drawable.ic_rounded_schedule
    ),
    V155_ROMANIZE(
        id = "v155_romanize",
        title = "Romanization",
        subtitle = "Per-language lyrics",
        icon = Res.drawable.ic_rounded_abc
    ),
    V155_DEVELOPER(
        id = "v155_developer",
        title = "Developer options",
        subtitle = "Heap, overlays and logs",
        icon = Res.drawable.ic_rounded_code
    ),
    V155_EQUALIZER(
        id = "v155_equalizer",
        title = "Equalizer",
        subtitle = "10 bands and presets",
        icon = Res.drawable.ic_rounded_graphic_eq
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
            else -> Color(0xFF3F474D) to Color(0xFFDEE3EB)
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
            else -> Color(0xFFEFF1F7) to Color(0xFF44474F)
        }
    }
}

val SettingsSection.isLegacy155: Boolean
    get() = name.startsWith("V155_")

fun getGroupShape(index: Int, totalItems: Int): Shape = when {
    totalItems <= 1 -> RoundedCornerShape(24.dp)
    index == 0 -> RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp, bottomStart = 4.dp, bottomEnd = 4.dp)
    index == totalItems - 1 -> RoundedCornerShape(topStart = 4.dp, topEnd = 4.dp, bottomStart = 24.dp, bottomEnd = 24.dp)
    else -> RoundedCornerShape(4.dp)
}
