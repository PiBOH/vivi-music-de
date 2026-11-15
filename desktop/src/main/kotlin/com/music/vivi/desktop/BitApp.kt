package com.music.vivi.desktop

import androidx.compose.foundation.window.WindowDraggableArea
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.window.WindowScope
import com.music.vivi.desktop.player.PlayerController

/**
 * The desktop entry point, now drawn as the Bit clone.
 *
 * The signature is the one MainWindow already passes: the theme parameters are
 * accepted and deliberately ignored, because the cloned window has one palette
 * (the measured Bit ladder) rather than a generated tonal scheme. Playback,
 * window controls and the shell's own navigation are live.
 */
@Composable
fun WindowScope.App(
    language: String,
    onLanguageChange: (String) -> Unit,
    font: AppFont,
    onFontChange: (AppFont) -> Unit,
    customFontPath: String = "",
    onImportFont: () -> Unit = {},
    onSelectCustomFont: (CustomFont) -> Unit = {},
    onRenameCustomFont: (CustomFont, String) -> Unit = { _, _ -> },
    onRemoveCustomFont: (CustomFont) -> Unit = {},
    themeMode: ThemeMode,
    accent: Color,
    onThemeModeChange: (ThemeMode) -> Unit,
    onAccentChange: (Color) -> Unit,
    accentIntensity: Float = 1f,
    onAccentIntensityChange: (Float) -> Unit = {},
    onAccentIntensityChangeFinished: () -> Unit = {},
    customAccents: List<Int> = emptyList(),
    onAddCustomAccent: (Int) -> Unit = {},
    onRemoveCustomAccent: (Int) -> Unit = {},
    pureBlack: Boolean,
    onPureBlackChange: (Boolean) -> Unit,
    spotifyLayout: Boolean = false,
    initialSection: String? = null,
    isFullscreen: Boolean = false,
    isMaximized: Boolean = false,
    nativeTitleBar: Boolean = false,
    onNativeTitleBarChange: (Boolean) -> Unit = {},
    onRestart: () -> Unit = {},
    onToggleFullscreen: () -> Unit = {},
    bringToFront: () -> Unit = {},
    onMinimize: () -> Unit = {},
    onMaximize: () -> Unit = {},
    onClose: () -> Unit = {},
) {
    val player = remember { PlayerController() }

    LaunchedEffect(Unit) {
        val settings = DesktopSettings.load()
        if (settings.activeEqProfileId.isNotEmpty()) {
            player.setEqualizer(settings.eqProfiles.find { it.id == settings.activeEqProfileId })
        }
    }

    BcTheme {
        BcRoot(
            player = player,
            language = language,
            initialSection = initialSection,
            isMaximized = isMaximized || isFullscreen,
            nativeTitleBar = nativeTitleBar,
            onMinimize = onMinimize,
            onMaximize = {
                if (isFullscreen) onToggleFullscreen() else onMaximize()
            },
            onClose = onClose,
            dragArea = { modifier, content ->
                WindowDraggableArea(modifier = modifier) { content() }
            },
        )
    }
}
