package com.music.vivi.desktop.presentation.navigation

import com.music.vivi.desktop.generated.resources.ic_artist
import org.jetbrains.compose.resources.DrawableResource
import com.music.vivi.desktop.generated.resources.Res
import com.music.vivi.desktop.generated.resources.ic_automirrored_rounded_playlist_play
import com.music.vivi.desktop.generated.resources.ic_rounded_album
import com.music.vivi.desktop.generated.resources.ic_rounded_favorite
import com.music.vivi.desktop.generated.resources.ic_rounded_folder
import com.music.vivi.desktop.generated.resources.ic_rounded_home
import com.music.vivi.desktop.generated.resources.ic_rounded_music_note
import com.music.vivi.desktop.generated.resources.ic_rounded_public
import com.music.vivi.desktop.generated.resources.ic_rounded_settings
enum class NavSection(val label: String) {
    MENU("Menu"),
    LIBRARY("Library"),
    SYSTEM("System")
}

enum class DesktopNavDestination(
    val title: String,
    val icon: DrawableResource,
    val section: NavSection
) {
    HOME("Home", Res.drawable.ic_rounded_home, NavSection.MENU),
    YOUTUBE("YouTube Music", Res.drawable.ic_rounded_public, NavSection.MENU),

    SONGS("Songs", Res.drawable.ic_rounded_music_note, NavSection.LIBRARY),
    ALBUMS("Albums", Res.drawable.ic_rounded_album, NavSection.LIBRARY),
    ARTISTS("Artists", Res.drawable.ic_artist, NavSection.LIBRARY),
    PLAYLISTS("Playlists", Res.drawable.ic_automirrored_rounded_playlist_play, NavSection.LIBRARY),
    FOLDERS("Folders", Res.drawable.ic_rounded_folder, NavSection.LIBRARY),
    LIKED("Liked", Res.drawable.ic_rounded_favorite, NavSection.LIBRARY),

    SETTINGS("Settings", Res.drawable.ic_rounded_settings, NavSection.SYSTEM)
}
