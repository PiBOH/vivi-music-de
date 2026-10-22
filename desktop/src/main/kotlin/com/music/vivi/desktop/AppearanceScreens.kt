package com.music.vivi.desktop

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ViewSidebar
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Album
import androidx.compose.material.icons.filled.Brightness1
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Devices
import androidx.compose.material.icons.filled.Gradient
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material.icons.filled.ViewAgenda
import androidx.compose.material.icons.filled.Wallpaper
import androidx.compose.material.icons.filled.DesktopWindows
import androidx.compose.material.icons.filled.FontDownload
import androidx.compose.material.icons.filled.Lyrics
import androidx.compose.material.icons.filled.Layers
import androidx.compose.material.icons.filled.Movie
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.MotionPhotosOn
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.SettingsBrightness
import androidx.compose.material.icons.filled.Swipe
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.text.style.TextAlign
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.hypot
import kotlin.math.roundToInt
import kotlin.math.sin

/**
 * Appearance hub: rows mirroring the Android app's Appearance sub-menu.
 * Single-choice options (font, canvas source, density, transitions, player
 * design) are inline dropdowns — no extra sub-screen, matching mobile.
 */
@Composable
fun AppearanceSection(
    language: String,
    onOpenTheme: () -> Unit,
    onOpenFont: () -> Unit,
    onOpenCanvas: () -> Unit,
    onOpenDensity: () -> Unit,
    onOpenTransitions: () -> Unit,
    onOpenPlayerDesign: () -> Unit,
    onOpenIntro: () -> Unit,
    animationsEnabled: Boolean = true,
    onAnimationsEnabledChange: (Boolean) -> Unit = {},
    animationSpeed: String = "normal",
    onAnimationSpeedChange: (String) -> Unit = {},
    nativeTitleBar: Boolean = false,
    onNativeTitleBarChange: (Boolean) -> Unit = {},
    showRightSidebar: Boolean = true,
    onShowRightSidebarChange: (Boolean) -> Unit = {},
    onRestart: () -> Unit = {},
) {
    var showRestartDialog by remember { mutableStateOf(false) }

    Text(Localization.get(language, "appearance"), style = MaterialTheme.typography.titleLarge, modifier = Modifier.padding(top = 12.dp))

    // Hub rows: each opens its M3 sub-screen, where the single-choice options
    // are picked from anchored dropdowns (CRITICAL FIX 1.49.1 — the sub-screens
    // were wrongly removed in 1.44.0 and are restored here).
    M3SettingsGroup(
        items = listOf(
            M3SettingsItem(
                icon = Icons.Filled.Palette,
                title = { Text(Localization.get(language, "theme_colors")) },
                trailing = { SettingsChevron() },
                onClick = onOpenTheme,
            ),
            // Mobile's Appearance screen leads with Theme, then the player
            // (mini player / player design) and only after that the rest; the
            // two rows are ordered the same way here.
            // "Player & mini-player design": the screen this row opens holds the
            // full player AND the mini player, so the old wording named only half
            // of it.
            M3SettingsItem(
                icon = Icons.Filled.MusicNote,
                title = { Text(Localization.get(language, "player_and_mini_player_design")) },
                trailing = { SettingsChevron() },
                onClick = onOpenPlayerDesign,
            ),
            M3SettingsItem(
                icon = Icons.Filled.Movie,
                title = { Text(Localization.get(language, "vivimusic_canvas")) },
                trailing = { SettingsChevron() },
                onClick = onOpenCanvas,
            ),
            M3SettingsItem(
                icon = Icons.Filled.FontDownload,
                title = { Text(Localization.get(language, "app_font")) },
                trailing = { SettingsChevron() },
                onClick = onOpenFont,
            ),
            M3SettingsItem(
                icon = Icons.Filled.SettingsBrightness,
                title = { Text(Localization.get(language, "density_and_grid")) },
                trailing = { SettingsChevron() },
                onClick = onOpenDensity,
            ),
            M3SettingsItem(
                icon = Icons.Filled.PlayArrow,
                title = { Text(Localization.get(language, "screen_transitions")) },
                trailing = { SettingsChevron() },
                onClick = onOpenTransitions,
            ),
            M3SettingsItem(
                icon = Icons.Filled.Movie,
                title = { Text(Localization.get(language, "intro")) },
                description = { Text(Localization.get(language, "show_intro_on_startup")) },
                trailing = { SettingsChevron() },
                onClick = onOpenIntro,
            ),
        ),
    )

    M3SettingsGroup(
        items = listOf(
            M3SettingsItem(
                icon = Icons.Filled.MotionPhotosOn,
                title = { Text(Localization.get(language, "animations")) },
                description = { Text(Localization.get(language, "animations_desc")) },
                trailing = { Switch(checked = animationsEnabled, onCheckedChange = onAnimationsEnabledChange) },
                onClick = { onAnimationsEnabledChange(!animationsEnabled) },
            ),
            M3SettingsItem(
                icon = Icons.Filled.DesktopWindows,
                title = { Text(Localization.get(language, "native_title_bar")) },
                description = {
                    Column {
                        Text(Localization.get(language, "native_title_bar_desc"))
                        Text(
                            Localization.get(language, "native_title_bar_desc_hint"),
                            color = MaterialTheme.colorScheme.primary,
                        )
                    }
                },
                trailing = {
                    Switch(
                        checked = nativeTitleBar,
                        onCheckedChange = { v ->
                            onNativeTitleBarChange(v)
                            showRestartDialog = true
                        },
                    )
                },
                onClick = {
                    onNativeTitleBarChange(!nativeTitleBar)
                    showRestartDialog = true
                },
            ),
            M3SettingsItem(
                icon = Icons.AutoMirrored.Filled.ViewSidebar,
                title = { Text(Localization.get(language, "right_panel")) },
                description = { Text(Localization.get(language, "right_panel_desc")) },
                trailing = {
                    Switch(
                        checked = showRightSidebar,
                        onCheckedChange = onShowRightSidebarChange,
                    )
                },
                onClick = { onShowRightSidebarChange(!showRightSidebar) },
            ),
        ),
    )

    // "Animation speed" applies a multiplier to every UI transition (screen
    // transitions, player crossfade, the lyric animations, …). It is a
    // derivative of the master "Animations" switch, so it is only offered
    // while transitions are enabled at all.
    // Folds, like every other option that belongs to a switch: turning the
    // Animations switch off closes this card instead of dropping it.
    M3SettingsFold(visible = animationsEnabled) {
        M3SettingsDropdownItem(
            icon = Icons.Filled.MotionPhotosOn,
            title = Localization.get(language, "animation_speed"),
            value = Localization.get(language, when (animationSpeed) {
                "fast" -> "animation_speed_fast"
                "slow" -> "animation_speed_slow"
                else -> "animation_speed_normal"
            }),
            options = listOf(
                "fast" to Localization.get(language, "animation_speed_fast"),
                "normal" to Localization.get(language, "animation_speed_normal"),
                "slow" to Localization.get(language, "animation_speed_slow"),
            ),
            onSelect = onAnimationSpeedChange,
        )
    }

    if (showRestartDialog) {
        AlertDialog(
            onDismissRequest = { showRestartDialog = false },
            title = { Text(Localization.get(language, "restart_required_title")) },
            text = { Text(Localization.get(language, "restart_required")) },
            confirmButton = {
                Button(onClick = { onRestart() }) {
                    Text(Localization.get(language, "restart_now"))
                }
            },
            dismissButton = {
                OutlinedButton(onClick = { showRestartDialog = false }) {
                    Text(Localization.get(language, "later"))
                }
            },
        )
    }
}

/**
 * Theme sub-screen: 4 mode circles (System / Light / Dark / Pure black) +
 * the full 21-color accent palette + a live preview card. Mirrors the Android
 * `ThemeScreen` (pixel-perfect mode selection).
 */
@Composable
fun ThemeSection(
    language: String,
    mode: ThemeMode,
    accent: Color,
    onModeChange: (ThemeMode) -> Unit,
    onAccentChange: (Color) -> Unit,
    accentIntensity: Float = 1f,
    onAccentIntensityChange: (Float) -> Unit = {},
    onAccentIntensityChangeFinished: () -> Unit = {},
    pureBlack: Boolean,
    onPureBlackChange: (Boolean) -> Unit,
    customAccents: List<Int> = emptyList(),
    onAddCustomAccent: (Int) -> Unit = {},
    onRemoveCustomAccent: (Int) -> Unit = {},
) {
    Column(Modifier.fillMaxWidth()) {
        Text(Localization.get(language, "theme_colors"), style = MaterialTheme.typography.titleLarge, modifier = Modifier.padding(top = 12.dp))

        // Live preview card (uses the currently applied theme).
        ThemePreviewCard(Modifier.fillMaxWidth().padding(top = 16.dp).height(140.dp))

        Spacer(Modifier.height(24.dp))

        Text(Localization.get(language, "theme_mode"), style = MaterialTheme.typography.titleMedium)
        Row(
            Modifier.fillMaxWidth().padding(top = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp, Alignment.CenterHorizontally),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            ThemeModeCircle(
                language = language,
                label = Localization.get(language, "theme_system"),
                selected = mode == ThemeMode.SYSTEM && !pureBlack,
                previewDark = false,
                pureBlack = false,
                isAuto = true,
                onClick = { onModeChange(ThemeMode.SYSTEM); onPureBlackChange(false) },
            )
            ThemeModeCircle(
                language = language,
                label = Localization.get(language, "theme_light"),
                selected = mode == ThemeMode.LIGHT && !pureBlack,
                previewDark = false,
                pureBlack = false,
                isAuto = false,
                onClick = { onModeChange(ThemeMode.LIGHT); onPureBlackChange(false) },
            )
            ThemeModeCircle(
                language = language,
                label = Localization.get(language, "theme_dark"),
                selected = mode == ThemeMode.DARK && !pureBlack,
                previewDark = true,
                pureBlack = false,
                isAuto = false,
                onClick = { onModeChange(ThemeMode.DARK); onPureBlackChange(false) },
            )
            ThemeModeCircle(
                language = language,
                label = Localization.get(language, "pure_black"),
                selected = mode == ThemeMode.DARK && pureBlack,
                previewDark = true,
                pureBlack = true,
                isAuto = false,
                onClick = { onModeChange(ThemeMode.DARK); onPureBlackChange(true) },
            )
        }

        Spacer(Modifier.height(24.dp))

        // The custom-colour state lives BEFORE the palette because the palette
        // draws the custom circle as its own last entry (the picker belongs at
        // the end of the row the user is already looking at, not in a section of
        // its own further down the screen).
        //
        // All three bars and that circle share this ONE state, keyed on the
        // current accent: choosing a colour from the predefined palette moves the
        // picker onto it, instead of leaving the picker on whatever was dragged
        // there last (the bars used a plain `remember`, so they never followed).
        val initialHsv = remember(accent) { colorToHsv(accent) }
        var hue by remember(accent) { mutableStateOf(initialHsv[0]) }
        var saturation by remember(accent) { mutableStateOf(initialHsv[1]) }
        var brightness by remember(accent) { mutableStateOf(initialHsv[2]) }
        val customColor = hsvToColor(hue, saturation, brightness)
        // The picker is open the moment the screen is: the round wheel is the
        // point of the section, and having to find the palette's last dot and
        // click it before anything appeared was the old behaviour ("the wheel
        // must always be visible"). The section's own **Hide** label still
        // collapses it, so the wheel is what the screen opens on and one click
        // away from being put aside.
        var showCustomPicker by remember { mutableStateOf(true) }

        // Two columns, the layout the owner drew: the palette and the saved
        // custom colours on the left, the picker on the right - the wheel with
        // its brightness strip and, rightmost, the accent intensity, and the hex
        // field with *Add to palette* under the wheel.
        //
        // The intensity used to be a full-width slider with a row of five preview
        // circles under it, in a section of its own further down the screen: the
        // same value in two places, and a screen two screens tall. It is one
        // vertical strip beside the wheel now (issue #103).
        Row(
            Modifier.fillMaxWidth().padding(top = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(40.dp),
        ) {
            Column(Modifier.weight(1f)) {
                // "Accent color", not "Color palette": the block is the accent the
                // theme is built from, not a catalogue of colours (the same heading
                // the redesign preview carries). The key already existed, so no new
                // string had to be translated.
                Text(Localization.get(language, "accent_color"), style = MaterialTheme.typography.titleMedium)
                // Palette swatches in rows of six, with the custom-colour circle
                // as the LAST swatch of the flow - right of the predefined
                // colours, on the next row when the current one is full.
                val paletteSize = AccentPalette.colors.size
                (0..paletteSize).chunked(6).forEach { row ->
                    Row(
                        Modifier.fillMaxWidth().padding(top = 10.dp),
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                    ) {
                        row.forEach { index ->
                            if (index == paletteSize) {
                                Tooltip(Localization.get(language, "custom_color")) {
                                    AccentSwatch(
                                        color = customColor,
                                        selected = showCustomPicker,
                                        onClick = {
                                            // Seed the picker from the accent in use, so the
                                            // circle opens ON the current colour.
                                            val hsv = colorToHsv(accent)
                                            hue = hsv[0]
                                            saturation = hsv[1]
                                            brightness = hsv[2]
                                            showCustomPicker = true
                                        },
                                    )
                                }
                                return@forEach
                            }
                            val entry = AccentPalette.colors[index]
                            val isSelected = if (entry.color == Color.Transparent) {
                                accent == Color.Transparent
                            } else {
                                accent == entry.color
                            }
                            val colorName = Localization.get(language, "accent_${entry.key}")
                            Tooltip(colorName) {
                                AccentSwatch(
                                    color = entry.color,
                                    selected = isSelected,
                                    onClick = {
                                        if (entry.color == Color.Transparent) {
                                            AccentPalette.refreshSystemAccent()
                                            onAccentChange(Color.Transparent)
                                        } else {
                                            onAccentChange(entry.color)
                                        }
                                    },
                                )
                            }
                        }
                    }
                }

                if (customAccents.isNotEmpty()) {
                    Spacer(Modifier.height(20.dp))
                    Text(Localization.get(language, "custom_colors"), style = MaterialTheme.typography.titleMedium)
                    customAccents.chunked(6).forEach { row ->
                        Row(
                            Modifier.fillMaxWidth().padding(top = 10.dp),
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                        ) {
                            row.forEach { argb ->
                                val c = argbIntToColor(argb)
                                val isSelected = accent == c
                                Tooltip(Localization.get(language, "remove_custom_color")) {
                                    Box {
                                        AccentSwatch(
                                            color = c,
                                            selected = isSelected,
                                            onClick = { onAccentChange(c) },
                                        )
                                        Icon(
                                            Icons.Filled.Close,
                                            contentDescription = null,
                                            tint = MaterialTheme.colorScheme.onSurface,
                                            modifier = Modifier
                                                .align(Alignment.TopEnd)
                                                .size(16.dp)
                                                .clip(CircleShape)
                                                .background(MaterialTheme.colorScheme.surface)
                                                .clickable { onRemoveCustomAccent(argb) }
                                                .padding(2.dp),
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // --- Custom color picker (one round wheel) ---
            // A fixed width, not a weight: the picker needs the room for the wheel,
            // its two strips and the widest caption, and the palette is the part
            // that can give it away. It is also what keeps the header row below
            // from sizing the column (a full-width row in an UNWEIGHTED column
            // takes 100 % of the Row, which left the palette with nothing: the
            // wheel rendered at x = 0, as the render check reported).
            Column(Modifier.width(PickerColumnWidth)) {
                // The section's own title and the switch that puts the picker
                // aside; the title stays because it is what names the block, and
                // the Hide/Show label lands above the strips, as in the mockup.
                Row(
                    Modifier.fillMaxWidth().clickable { showCustomPicker = !showCustomPicker },
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        Localization.get(language, "custom_color"),
                        style = MaterialTheme.typography.titleMedium,
                        modifier = Modifier.weight(1f),
                    )
                    Text(
                        Localization.get(language, if (showCustomPicker) "login_hide" else "login_show"),
                        color = MaterialTheme.colorScheme.primary,
                        style = MaterialTheme.typography.labelLarge,
                    )
                }
                if (!showCustomPicker) {
                    Spacer(Modifier.height(36.dp))
                } else {
                    val alreadySaved = remember(customAccents, customColor) {
                        customAccents.any { argbIntToColor(it) == customColor }
                    }
                    Spacer(Modifier.height(14.dp))
                    // The picker's three controls, left to right: the wheel (hue
                    // around the rim, saturation from the centre out), the
                    // brightness strip, and the intensity - rightmost, where the
                    // owner moved it.
                    Row(verticalAlignment = Alignment.Top) {
                        HsvColorWheel(
                            hue = hue,
                            saturation = saturation,
                            brightness = brightness,
                            onPick = { h, s -> hue = h; saturation = s },
                        )
                        Spacer(Modifier.width(StripGap))
                        Tooltip(Localization.get(language, "brightness")) {
                            BrightnessStrip(
                                hue = hue,
                                saturation = saturation,
                                brightness = brightness,
                                onBrightnessChange = { brightness = it },
                            )
                        }
                        Spacer(Modifier.width(StripGap))
                        Tooltip(Localization.get(language, "accent_intensity")) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                IntensityStrip(
                                    accent = accent,
                                    intensity = accentIntensity,
                                    onIntensityChange = onAccentIntensityChange,
                                    onIntensityChangeFinished = onAccentIntensityChangeFinished,
                                )
                                Text(
                                    Localization.get(language, "accent_intensity"),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    maxLines = 2,
                                    textAlign = TextAlign.Center,
                                    modifier = Modifier.padding(top = 6.dp).width(StripCaptionWidth),
                                )
                                Text(
                                    "${(accentIntensity * 100).roundToInt()}%",
                                    style = MaterialTheme.typography.labelLarge,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.padding(top = 2.dp),
                                )
                            }
                        }
                    }
                    // The hex field and the button that saves the colour: UNDER
                    // the wheel, centred with it.
                    Column(
                        Modifier.padding(top = 18.dp).width(WheelSize),
                        horizontalAlignment = Alignment.CenterHorizontally,
                    ) {
                        // Live-updating HEX field (issue #61): typing a 6-digit value applies it
                        // to the picker immediately, so a color can be pasted from anywhere.
                        val liveHex = "%06X".format(java.util.Locale.US, colorToArgbInt(customColor) and 0xFFFFFF)
                        var hexInput by remember { mutableStateOf(liveHex) }
                        var hexFocused by remember { mutableStateOf(false) }
                        // While the field is not focused it mirrors the colour picked on the
                        // wheel; typing in it drives hue/saturation/brightness instead.
                        LaunchedEffect(hexFocused, liveHex) {
                            if (!hexFocused) hexInput = liveHex
                        }
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                Modifier
                                    .size(40.dp)
                                    .clip(CircleShape)
                                    .background(customColor)
                                    .border(BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant), CircleShape),
                            )
                            Spacer(Modifier.width(12.dp))
                            OutlinedTextField(
                                value = hexInput,
                                onValueChange = { raw ->
                                    val cleaned = raw.uppercase()
                                        .filter { it.isDigit() || it in 'A'..'F' }
                                        .take(6)
                                    hexInput = cleaned
                                    if (cleaned.length == 6) {
                                        cleaned.toIntOrNull(16)?.let { rgb ->
                                            val hsv = colorToHsv(Color(0xFF000000.toInt() or rgb))
                                            hue = hsv[0]
                                            saturation = hsv[1]
                                            brightness = hsv[2]
                                        }
                                    }
                                },
                                singleLine = true,
                                isError = hexInput.length != 6,
                                placeholder = { Text("#RRGGBB") },
                                textStyle = MaterialTheme.typography.bodyMedium,
                                modifier = Modifier
                                    .width(150.dp)
                                    .onFocusChanged { hexFocused = it.isFocused },
                            )
                        }
                        Spacer(Modifier.height(16.dp))
                        Button(
                            onClick = { onAddCustomAccent(colorToArgbInt(customColor)) },
                            enabled = !alreadySaved,
                        ) {
                            Icon(Icons.Filled.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(6.dp))
                            Text(Localization.get(language, "add_to_palette"))
                        }
                    }
                }
            }
        }

        Spacer(Modifier.height(36.dp))
    }
}

/** The colour wheel's diameter, and the height of the strip beside it. */
private val WheelSize = 220.dp

/** The width one of the strips beside the wheel takes, and the gap before it. */
private val StripSize = 20.dp
private val StripGap = 14.dp

/**
 * The picker column's width: the wheel, the brightness strip and the intensity
 * strip with the gaps between them, plus the width of the caption under the
 * intensity (the widest text in the column, which is what the strip is centred
 * in).
 */
private val StripCaptionWidth = 92.dp
private val PickerColumnWidth = WheelSize + StripGap + StripSize + StripGap + StripCaptionWidth

/**
 * The colour wheel: hue around the rim, saturation from the centre out, both
 * picked with a single drag. It is drawn from two gradients - a sweep for the
 * hue and a radial white-to-transparent for the saturation - and darkened to the
 * current brightness, so the disc always shows the colour being selected. The
 * marker is the same colour's position: the hue is its angle and the saturation
 * its distance from the centre.
 */
@Composable
private fun HsvColorWheel(
    hue: Float,
    saturation: Float,
    brightness: Float,
    onPick: (Float, Float) -> Unit,
) {
    var radiusPx by remember { mutableStateOf(1f) }
    Box(
        Modifier
            .size(WheelSize)
            .onSizeChanged { radiusPx = it.width / 2f }
            .pointerInput(Unit) {
                fun pick(position: Offset) {
                    val dx = position.x - radiusPx
                    val dy = position.y - radiusPx
                    // The sweep gradient starts at 3 o'clock and runs clockwise,
                    // and the angle is read the same way (y grows downwards), so
                    // the marker always sits under the pointer.
                    val degrees = Math.toDegrees(atan2(dy.toDouble(), dx.toDouble())).toFloat()
                    onPick((degrees + 360f) % 360f, (hypot(dx, dy) / radiusPx).coerceIn(0f, 1f))
                }
                // ISSUE #61: `detectTapGestures` never returns (it loops
                // forever), so a `detectDragGestures` call after it was dead code
                // and the surface only reacted on mouse release. A single gesture
                // loop tracks the press AND every drag movement, so the marker
                // follows the pointer in real time.
                awaitEachGesture {
                    val down = awaitFirstDown(requireUnconsumed = false)
                    pick(down.position)
                    down.consume()
                    while (true) {
                        val event = awaitPointerEvent()
                        val change = event.changes.firstOrNull { it.id == down.id } ?: break
                        if (!change.pressed) break
                        pick(change.position)
                        change.consume()
                    }
                }
            },
    ) {
        Canvas(Modifier.fillMaxSize()) {
            val r = size.minDimension / 2f
            val middle = center
            drawCircle(
                brush = Brush.sweepGradient(
                    colors = listOf(Color.Red, Color.Yellow, Color.Green, Color.Cyan, Color.Blue, Color.Magenta, Color.Red),
                    center = middle,
                ),
                radius = r,
                center = middle,
            )
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(Color.White, Color.White.copy(alpha = 0f)),
                    center = middle,
                    radius = r,
                ),
                radius = r,
                center = middle,
            )
            if (brightness < 1f) {
                drawCircle(Color.Black.copy(alpha = 1f - brightness), radius = r, center = middle)
            }
            val angle = Math.toRadians(hue.toDouble())
            val marker = Offset(
                x = middle.x + (r * saturation * cos(angle)).toFloat(),
                y = middle.y + (r * saturation * sin(angle)).toFloat(),
            )
            val markRadius = (r * 0.06f).coerceAtLeast(4f)
            drawCircle(Color.White, radius = markRadius, center = marker)
            drawCircle(
                Color.Black.copy(alpha = 0.55f),
                radius = markRadius,
                center = marker,
                style = Stroke(width = 2f),
            )
        }
    }
}

/**
 * The wheel's value control: the picked colour at full brightness at the top,
 * black at the bottom. It stays a strip beside the wheel because a wheel carries
 * hue and saturation only, and it is dragged exactly like the wheel is.
 */
@Composable
private fun BrightnessStrip(
    hue: Float,
    saturation: Float,
    brightness: Float,
    onBrightnessChange: (Float) -> Unit,
) {
    var stripHeightPx by remember { mutableStateOf(1f) }
    BoxWithConstraints(
        modifier = Modifier
            .width(StripSize)
            .height(WheelSize)
            .clip(RoundedCornerShape(10.dp))
            .background(Brush.verticalGradient(listOf(hsvToColor(hue, saturation, 1f), Color.Black)))
            .onSizeChanged { stripHeightPx = it.height.toFloat() }
            .pointerInput(stripHeightPx) {
                fun pick(y: Float) = onBrightnessChange((1f - y / stripHeightPx).coerceIn(0f, 1f))
                awaitEachGesture {
                    val down = awaitFirstDown(requireUnconsumed = false)
                    pick(down.position.y)
                    down.consume()
                    while (true) {
                        val event = awaitPointerEvent()
                        val change = event.changes.firstOrNull { it.id == down.id } ?: break
                        if (!change.pressed) break
                        pick(change.position.y)
                        change.consume()
                    }
                }
            },
    ) {
        val thumbY = ((1f - brightness) * maxHeight.value).dp.coerceIn(0.dp, maxHeight)
        Box(
            Modifier
                .offset(y = thumbY - 5.dp)
                .height(10.dp)
                .fillMaxWidth()
                .border(2.dp, Color.White, RoundedCornerShape(3.dp))
                .shadow(2.dp, RoundedCornerShape(3.dp)),
        )
    }
}

/**
 * The accent intensity, as the strip the owner moved beside the wheel: the
 * rightmost of the picker's three controls.
 *
 * It carries what the removed row of five preview circles used to: the gradient
 * is the accent itself at the top and the same accent at 0 % at the bottom, so
 * the strip shows what the drag is doing while the screen applies it live. The
 * pointer loop is [BrightnessStrip]'s, and the ticks are the slider's own
 * `steps = 18` (20 values), so a drag lands on the values it used to. The drag
 * only moves the in-memory state and the file is written once, on release
 * (issue #61: persisting per frame made the thumb stutter).
 */
@Composable
private fun IntensityStrip(
    accent: Color,
    intensity: Float,
    onIntensityChange: (Float) -> Unit,
    onIntensityChangeFinished: () -> Unit,
) {
    var stripHeightPx by remember { mutableStateOf(1f) }
    BoxWithConstraints(
        modifier = Modifier
            .width(StripSize)
            .height(WheelSize)
            .clip(RoundedCornerShape(10.dp))
            .background(Brush.verticalGradient(listOf(accent, adjustAccentIntensity(accent, 0f))))
            .onSizeChanged { stripHeightPx = it.height.toFloat() }
            .pointerInput(stripHeightPx) {
                fun pick(y: Float) = onIntensityChange((1f - y / stripHeightPx).coerceIn(0f, 1f))
                awaitEachGesture {
                    val down = awaitFirstDown(requireUnconsumed = false)
                    pick(down.position.y)
                    down.consume()
                    while (true) {
                        val event = awaitPointerEvent()
                        val change = event.changes.firstOrNull { it.id == down.id } ?: break
                        if (!change.pressed) break
                        pick(change.position.y)
                        change.consume()
                    }
                    // One write per gesture, on the release.
                    onIntensityChangeFinished()
                }
            },
    ) {
        repeat(20) { step ->
            Box(
                Modifier
                    .offset(y = maxHeight * step / 19f)
                    .fillMaxWidth()
                    .height(1.dp)
                    .background(Color.White.copy(alpha = 0.5f)),
            )
        }
        val thumbY = ((1f - intensity) * maxHeight.value).dp.coerceIn(0.dp, maxHeight)
        Box(
            Modifier
                .offset(y = thumbY - 5.dp)
                .height(10.dp)
                .fillMaxWidth()
                .border(2.dp, Color.White, RoundedCornerShape(3.dp))
                .shadow(2.dp, RoundedCornerShape(3.dp)),
        )
    }
}

@Composable
private fun ThemeModeCircle(
    language: String,
    label: String,
    selected: Boolean,
    previewDark: Boolean,
    pureBlack: Boolean,
    isAuto: Boolean,
    onClick: () -> Unit,
) {
    val innerColor = when {
        pureBlack -> Color.Black
        previewDark -> MaterialTheme.colorScheme.surface
        else -> Color.White
    }
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Box(
            Modifier
                .size(64.dp)
                .clip(RoundedCornerShape(18.dp))
                .background(
                    if (selected) MaterialTheme.colorScheme.surfaceVariant
                    else MaterialTheme.colorScheme.surfaceContainerHigh,
                )
                .border(
                    width = if (selected) 2.dp else 1.dp,
                    color = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant,
                    shape = RoundedCornerShape(18.dp),
                )
                .clickable(onClick = onClick),
            contentAlignment = Alignment.Center,
        ) {
            Box(
                Modifier
                    .size(36.dp)
                    .clip(CircleShape)
                    .background(innerColor)
                    .border(
                        width = 1.dp,
                        color = MaterialTheme.colorScheme.outlineVariant,
                        shape = CircleShape,
                    ),
                contentAlignment = Alignment.Center,
            ) {
                if (isAuto) {
                    Icon(
                        Icons.Filled.Palette,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.size(16.dp),
                    )
                } else if (selected) {
                    Icon(
                        Icons.Filled.Check,
                        contentDescription = null,
                        tint = if (pureBlack) Color.White else MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(16.dp),
                    )
                }
            }
        }
        Text(
            label,
            style = MaterialTheme.typography.labelMedium,
            color = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(top = 6.dp),
        )
    }
}

@Composable
private fun AccentSwatch(color: Color, selected: Boolean, onClick: () -> Unit) {
    Box(
        Modifier
            .size(36.dp)
            .clip(CircleShape)
            .then(
                if (color == Color.Transparent) {
                    Modifier.background(MaterialTheme.colorScheme.surfaceVariant)
                } else {
                    Modifier.background(color)
                },
            )
            .border(
                width = if (selected) 3.dp else 1.dp,
                color = if (selected) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.outlineVariant,
                shape = CircleShape,
            )
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        if (selected) {
            Icon(
                Icons.Filled.Check,
                contentDescription = null,
                tint = if (color == Color.Transparent) MaterialTheme.colorScheme.onSurface else Color.White,
                modifier = Modifier.size(16.dp),
            )
        } else if (color == Color.Transparent) {
            Icon(
                Icons.Filled.Palette,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(18.dp),
            )
        }
    }
}

@Composable
private fun ThemePreviewCard(modifier: Modifier = Modifier) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
    ) {
        Column(
            Modifier.fillMaxSize().padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Box(
                    Modifier
                        .weight(1f)
                        .height(14.dp)
                        .clip(RoundedCornerShape(7.dp))
                        .background(MaterialTheme.colorScheme.surfaceContainerHigh),
                )
                Spacer(Modifier.width(12.dp))
                Box(
                    Modifier
                        .size(28.dp)
                        .background(MaterialTheme.colorScheme.primary, CircleShape),
                )
            }
            Row(
                Modifier.fillMaxWidth().weight(1f),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Box(
                    Modifier
                        .weight(2f)
                        .fillMaxSize()
                        .clip(RoundedCornerShape(12.dp))
                        .background(MaterialTheme.colorScheme.primary),
                )
                Column(
                    Modifier.weight(1f).fillMaxSize(),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Box(
                        Modifier
                            .fillMaxWidth()
                            .weight(1f)
                            .clip(RoundedCornerShape(12.dp))
                            .background(MaterialTheme.colorScheme.secondary),
                    )
                    Box(
                        Modifier
                            .fillMaxWidth()
                            .weight(1f)
                            .clip(RoundedCornerShape(12.dp))
                            .background(MaterialTheme.colorScheme.tertiary),
                    )
                }
                Box(
                    Modifier
                        .weight(1f)
                        .fillMaxSize()
                        .clip(RoundedCornerShape(12.dp))
                        .background(MaterialTheme.colorScheme.primaryContainer),
                )
            }
        }
    }
}

/** Human-readable density label (e.g. "110%") for a scale value. */
private fun densityLabel(scale: Float): String = "${(scale * 100).roundToInt()}%"

/** Grid cell width presets in dp (small / medium / large). */
private val GRID_PRESETS = listOf(140 to "grid_small", 160 to "grid_medium", 200 to "grid_large", 240 to "grid_xlarge")

// ============================================================================
// Restored sub-screens (Material 3 redesign): the Appearance section navigates
// here; single-choice options inside use anchored dropdowns instead of plain
// radio rows. CRITICAL FIX 1.49.1 — these had been wrongly removed in 1.44.0.
// ============================================================================

@Composable
fun TransitionsScreen(
    language: String,
    screenTransition: String,
    onScreenTransitionChange: (String) -> Unit,
) {
    Column(Modifier.fillMaxWidth()) {
        Text(Localization.get(language, "screen_transitions"), style = MaterialTheme.typography.titleLarge, modifier = Modifier.padding(top = 12.dp))
        Spacer(Modifier.height(4.dp))
        M3SettingsDropdownItem(
            icon = Icons.Filled.PlayArrow,
            title = Localization.get(language, "screen_transitions"),
            value = Localization.get(language, when (screenTransition) {
                "slide" -> "transition_slide"
                "off" -> "transition_off"
                else -> "transition_fade"
            }),
            options = listOf(
                "off" to Localization.get(language, "transition_off"),
                "fade" to Localization.get(language, "transition_fade"),
                "slide" to Localization.get(language, "transition_slide"),
            ),
            onSelect = onScreenTransitionChange,
        )
    }
}

@Composable
fun FontSection(
    language: String,
    selectedFont: AppFont,
    onFontChange: (AppFont) -> Unit,
    customFontPath: String = "",
    onImportFont: () -> Unit = {},
    useFontInLyrics: Boolean = true,
    onUseFontInLyricsChange: (Boolean) -> Unit = {},
) {
    // Restored to the pre-1.44.0 rich layout: a big themed typography preview
    // card + each font listed as its own radio row rendered in that typeface.
    val activeFamily = AppFonts.familyFor(selectedFont, customFontPath)

    Column(Modifier.fillMaxWidth()) {
        Text(Localization.get(language, "app_font"), style = MaterialTheme.typography.titleLarge, modifier = Modifier.padding(top = 12.dp))

        Card(
            shape = MaterialTheme.shapes.large,
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.primaryContainer,
                contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
            ),
            modifier = Modifier.fillMaxWidth().padding(vertical = 16.dp),
        ) {
            Column(Modifier.fillMaxWidth().padding(24.dp)) {
                Text(
                    Localization.get(language, "typography_preview").uppercase(),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(bottom = 8.dp),
                )
                Text(
                    Localization.get(language, "preview_text_quote"),
                    fontFamily = activeFamily,
                    style = MaterialTheme.typography.headlineMedium,
                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                    modifier = Modifier.padding(bottom = 12.dp),
                )
                Text(
                    Localization.get(language, "font_selection"),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f),
                )
            }
        }

        Spacer(Modifier.height(16.dp))

        Text(Localization.get(language, "font_selection"), style = MaterialTheme.typography.titleMedium)

        FontOption(
            title = Localization.get(language, "font_system"),
            desc = Localization.get(language, "font_system_desc"),
            family = FontFamily.Default,
            selected = selectedFont == AppFont.SYSTEM,
            onClick = { onFontChange(AppFont.SYSTEM) },
        )
        FontOption(
            title = Localization.get(language, "font_google_sans"),
            desc = Localization.get(language, "font_google_sans_desc"),
            family = AppFonts.googleSans,
            selected = selectedFont == AppFont.GOOGLE_SANS,
            onClick = { onFontChange(AppFont.GOOGLE_SANS) },
        )
        FontOption(
            title = Localization.get(language, "font_sans_flex"),
            desc = Localization.get(language, "font_sans_flex_desc"),
            family = AppFonts.sansFlex,
            selected = selectedFont == AppFont.SANS_FLEX,
            onClick = { onFontChange(AppFont.SANS_FLEX) },
        )
        FontOption(
            title = Localization.get(language, "font_outfit"),
            desc = Localization.get(language, "font_outfit_desc"),
            family = AppFonts.outfit,
            selected = selectedFont == AppFont.OUTFIT,
            onClick = { onFontChange(AppFont.OUTFIT) },
        )
        FontOption(
            title = Localization.get(language, "font_plus_jakarta_sans"),
            desc = Localization.get(language, "font_plus_jakarta_sans_desc"),
            family = AppFonts.plusJakartaSans,
            selected = selectedFont == AppFont.PLUS_JAKARTA_SANS,
            onClick = { onFontChange(AppFont.PLUS_JAKARTA_SANS) },
        )

        if (customFontPath.isNotBlank()) {
            FontOption(
                title = Localization.get(language, "custom_font"),
                desc = customFontPath.substringAfterLast("\\").substringAfterLast("/"),
                family = AppFonts.familyFor(AppFont.CUSTOM, customFontPath),
                selected = selectedFont == AppFont.CUSTOM,
                onClick = { onFontChange(AppFont.CUSTOM) },
            )
        }

        Spacer(Modifier.height(12.dp))
        OutlinedButton(onClick = onImportFont) {
            Icon(Icons.Filled.FontDownload, contentDescription = null, modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(8.dp))
            Text(Localization.get(language, "import_font"))
        }

        // The lyrics draw from a bare text style of their own, so the face chosen
        // here never reached them even though the theme carries it everywhere
        // else. This is the switch that decides it, on by default: a font chosen
        // on this screen is expected to be the app's font, lyrics included.
        Spacer(Modifier.height(16.dp))
        M3SettingsGroup(
            items = listOf(
                M3SettingsItem(
                    icon = Icons.Filled.Lyrics,
                    title = { Text(Localization.get(language, "font_in_lyrics")) },
                    description = { Text(Localization.get(language, "font_in_lyrics_desc")) },
                    trailing = {
                        Switch(checked = useFontInLyrics, onCheckedChange = onUseFontInLyricsChange)
                    },
                    onClick = { onUseFontInLyricsChange(!useFontInLyrics) },
                ),
            ),
        )

        Spacer(Modifier.height(36.dp))
    }
}

@Composable
private fun FontOption(
    title: String,
    desc: String,
    family: FontFamily,
    selected: Boolean,
    onClick: () -> Unit,
) {
    Row(
        Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        RadioButton(selected = selected, onClick = null)
        Column(Modifier.weight(1f).padding(start = 12.dp)) {
            Text(title, fontFamily = family, style = MaterialTheme.typography.bodyLarge)
            Text(
                desc,
                fontFamily = family,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
fun CanvasSection(
    language: String,
    canvasEnabled: Boolean,
    onCanvasEnabledChange: (Boolean) -> Unit,
    canvasSource: CanvasSource,
    onCanvasSourceChange: (CanvasSource) -> Unit,
) {
    Column(Modifier.fillMaxWidth()) {
        Text(Localization.get(language, "vivimusic_canvas"), style = MaterialTheme.typography.titleLarge, modifier = Modifier.padding(top = 12.dp))
        Spacer(Modifier.height(4.dp))
        M3SettingsGroup(
            items = listOf(
                M3SettingsItem(
                    icon = Icons.Filled.Movie,
                    title = { Text(Localization.get(language, "vivimusic_canvas")) },
                    description = { Text(Localization.get(language, "vivimusic_canvas_desc")) },
                    trailing = { Switch(checked = canvasEnabled, onCheckedChange = onCanvasEnabledChange) },
                    onClick = { onCanvasEnabledChange(!canvasEnabled) },
                ),
            ),
        )
        Spacer(Modifier.height(8.dp))
        M3SettingsDropdownItem(
            icon = Icons.Filled.Palette,
            title = Localization.get(language, "canvas_source"),
            value = Localization.get(language, when (canvasSource) {
                CanvasSource.APPLE_MUSIC -> "canvas_source_apple_music"
                CanvasSource.VIVIMUSIC -> "canvas_source_vivimusic"
                CanvasSource.TIDAL -> "canvas_source_tidal"
                else -> "canvas_source_auto"
            }),
            options = listOf(
                CanvasSource.AUTO to "canvas_source_auto",
                CanvasSource.APPLE_MUSIC to "canvas_source_apple_music",
                CanvasSource.VIVIMUSIC to "canvas_source_vivimusic",
                CanvasSource.TIDAL to "canvas_source_tidal",
            ).map { (v, k) -> v.key to Localization.get(language, k) },
            onSelect = { key -> onCanvasSourceChange(CanvasSource.from(key)) },
        )
    }
}

@Composable
fun DensityScreen(
    language: String,
    densityScale: Float,
    onDensityScaleChange: (Float) -> Unit,
    gridItemSize: Int,
    onGridItemSizeChange: (Int) -> Unit,
) {
    Column(Modifier.fillMaxWidth()) {
        Text(Localization.get(language, "density_and_grid"), style = MaterialTheme.typography.titleLarge, modifier = Modifier.padding(top = 12.dp))
        Spacer(Modifier.height(4.dp))
        // Single selector for the UI density: the duplicate info row was folded
        // into this dropdown's description so the screen offers exactly one
        // "Density & grid" option (plus the separate grid-item-size picker).
        M3SettingsDropdownItem(
            icon = Icons.Filled.SettingsBrightness,
            title = Localization.get(language, "density_and_grid"),
            description = Localization.get(language, "density_desc"),
            value = densityLabel(densityScale),
            options = DENSITY_PRESETS.map { it.toString() to densityLabel(it) },
            onSelect = { s -> onDensityScaleChange(s.toFloatOrNull() ?: densityScale) },
        )
        Spacer(Modifier.height(8.dp))
        M3SettingsDropdownItem(
            icon = Icons.Filled.ViewAgenda,
            title = Localization.get(language, "grid_item_size"),
            value = Localization.get(language, when (gridItemSize) {
                140 -> "grid_small"
                200 -> "grid_large"
                240 -> "grid_xlarge"
                else -> "grid_medium"
            }),
            options = GRID_PRESETS.map { (size, key) -> size.toString() to Localization.get(language, key) },
            onSelect = { s -> onGridItemSizeChange(s.toIntOrNull() ?: gridItemSize) },
        )
    }
}

/**
 * The label of one entry of the player picker.
 *
 * The two expressive finishes are separate entries, and their names are built
 * from two keys that are **already translated in all 52 languages** — the
 * player's own name ("Expressive") and the tab finish ("Translucent tab") —
 * rather than from a brand-new key that would have to be translated 52 times to
 * say one word in a parenthesis. There is no English fallback hiding here: the
 * tab key exists in every table, and [Localization.get] is asked for it in the
 * selected language like every other label on this screen.
 */
internal fun playerDesignLabel(language: String, design: PlayerDesign): String {
    val name = Localization.get(language, when (design) {
        PlayerDesign.NEW -> "player_design_new"
        PlayerDesign.EXPRESSIVE, PlayerDesign.EXPRESSIVE_TRANSLUCENT -> "player_design_expressive"
        else -> "player_design_classic"
    })
    return if (design == PlayerDesign.EXPRESSIVE_TRANSLUCENT) {
        "$name (${Localization.get(language, "expressive_tab_translucent")})"
    } else {
        name
    }
}

@Composable
fun PlayerDesignScreen(
    language: String,
    design: PlayerDesign,
    onDesignChange: (PlayerDesign) -> Unit,
    background: PlayerBackgroundStyle,
    onBackgroundChange: (PlayerBackgroundStyle) -> Unit,
    rotatingThumbnail: Boolean,
    onRotatingThumbnailChange: (Boolean) -> Unit,
    miniPlayerDesign: MiniPlayerDesign = MiniPlayerDesign.CLASSIC,
    onMiniPlayerDesignChange: (MiniPlayerDesign) -> Unit = {},
    miniPlayerBackgroundStyle: MiniPlayerBackgroundStyle = MiniPlayerBackgroundStyle.FOLLOW_THEME,
    onMiniPlayerBackgroundStyleChange: (MiniPlayerBackgroundStyle) -> Unit = {},
    pureBlackMiniPlayer: Boolean = false,
    onPureBlackMiniPlayerChange: (Boolean) -> Unit = {},
    sliderStyle: String = "slim",
    onSliderStyleChange: (String) -> Unit = {},
    swipeThumbnail: Boolean = true,
    onSwipeThumbnailChange: (Boolean) -> Unit = {},
    swipeSensitivity: Float = 0.73f,
    onSwipeSensitivityChange: (Float) -> Unit = {},
) {
    // One subject - how the player looks - so the screen reads in the order the
    // player is built: the full player first (its design, then the background
    // behind it and the artwork turning on it), then the mini player (design,
    // background, its pure-black finish and the swipe that changes track), then
    // the seek bar both share. Every choice about the full player is in the same
    // card and every choice about the mini player in another: seven loose cards
    // said the seven choices were unrelated, two cards plus one say what each
    // belongs to. Slider style stands alone at the end because it styles both
    // players and neither of them alone.
    Column(Modifier.fillMaxWidth()) {
        Text(
            Localization.get(language, "player_and_mini_player_design"),
            style = MaterialTheme.typography.titleLarge,
            modifier = Modifier.padding(top = 12.dp),
        )
        Spacer(Modifier.height(4.dp))
        M3SettingsGroup(
            items = listOf(
                M3SettingsItem(
                    icon = Icons.Filled.MusicNote,
                    title = { Text(Localization.get(language, "player_design")) },
                    // The old "V2" entry was the CLASSIC layout with a bigger
                    // artwork (one player, two names) and has been removed. The
                    // two expressive finishes ARE two entries here rather than a
                    // separate switch: the tab is the only thing that
                    // distinguishes them, so picking the player and picking its
                    // finish are the same decision - and a switch that only
                    // applied to one of the layouts was a dead control in the
                    // other two.
                    dropdown = M3Dropdown(
                        value = playerDesignLabel(language, design),
                        options = PlayerDesign.entries.map { v -> v.key to playerDesignLabel(language, v) },
                        onSelect = { key -> onDesignChange(PlayerDesign.from(key)) },
                    ),
                ),
            ),
        )
        Spacer(Modifier.height(8.dp))
        M3SettingsGroup(
            items = listOf(
                M3SettingsItem(
                    icon = Icons.Filled.Wallpaper,
                    title = { Text(Localization.get(language, "player_background")) },
                    dropdown = M3Dropdown(
                        value = Localization.get(language, playerBackgroundLabelKey(background)),
                        options = PlayerBackgroundStyle.entries.map { b ->
                            b.key to Localization.get(language, playerBackgroundLabelKey(b))
                        },
                        onSelect = { key -> onBackgroundChange(PlayerBackgroundStyle.from(key)) },
                    ),
                ),
                M3SettingsItem(
                    icon = Icons.Filled.Album,
                    title = { Text(Localization.get(language, "rotating_thumbnail")) },
                    description = { Text(Localization.get(language, "rotating_thumbnail_desc")) },
                    trailing = { Switch(checked = rotatingThumbnail, onCheckedChange = onRotatingThumbnailChange) },
                    onClick = { onRotatingThumbnailChange(!rotatingThumbnail) },
                ),
            ),
        )
        Spacer(Modifier.height(8.dp))
        M3SettingsGroup(
            items = listOf(
                M3SettingsItem(
                    icon = Icons.Filled.Devices,
                    title = { Text(Localization.get(language, "mini_player_design")) },
                    dropdown = M3Dropdown(
                        value = Localization.get(language, miniPlayerDesignLabelKey(miniPlayerDesign)),
                        options = listOf(
                            MiniPlayerDesign.CLASSIC to "mini_player_classic",
                            MiniPlayerDesign.NEW to "mini_player_new",
                            MiniPlayerDesign.APPLE to "mini_player_apple",
                        ).map { (v, k) -> v.key to Localization.get(language, k) },
                        onSelect = { key -> onMiniPlayerDesignChange(MiniPlayerDesign.from(key)) },
                    ),
                ),
                M3SettingsItem(
                    icon = Icons.Filled.Gradient,
                    title = { Text(Localization.get(language, "mini_player_background")) },
                    dropdown = M3Dropdown(
                        value = Localization.get(language, miniPlayerBackgroundLabelKey(miniPlayerBackgroundStyle)),
                        options = MiniPlayerBackgroundStyle.entries.map { b ->
                            b.key to Localization.get(language, miniPlayerBackgroundLabelKey(b))
                        },
                        onSelect = { key -> onMiniPlayerBackgroundStyleChange(MiniPlayerBackgroundStyle.from(key)) },
                    ),
                ),
                M3SettingsItem(
                    icon = Icons.Filled.Brightness1,
                    title = { Text(Localization.get(language, "pure_black_mini")) },
                    description = { Text(Localization.get(language, "pure_black_mini_desc")) },
                    trailing = { Switch(checked = pureBlackMiniPlayer, onCheckedChange = onPureBlackMiniPlayerChange) },
                    onClick = { onPureBlackMiniPlayerChange(!pureBlackMiniPlayer) },
                ),
                // Mobile "Enable swipe to change song": the artwork of the mini
                // player changes track when dragged sideways. It belongs to the
                // mini player, so it rides the mini player's own card.
                M3SettingsItem(
                    icon = Icons.Filled.Swipe,
                    title = { Text(Localization.get(language, "enable_swipe_thumbnail")) },
                    description = { Text(Localization.get(language, "enable_swipe_thumbnail_desc")) },
                    trailing = { Switch(checked = swipeThumbnail, onCheckedChange = onSwipeThumbnailChange) },
                    onClick = { onSwipeThumbnailChange(!swipeThumbnail) },
                ),
            ) + if (swipeThumbnail) {
                // The sensitivity is a derivative of the switch above and a row of
                // the same card, directly under it: it tunes the gesture that row
                // turns on, so a card of its own made it look like an unrelated
                // setting that happened to be nearby. It exists only while the
                // swipe is on, the same rule as the other derived rows (the
                // skip-silence sub-options).
                listOf(
                    M3SettingsItem(
                        icon = Icons.Filled.Swipe,
                        title = {
                            Text(
                                "${Localization.get(language, "swipe_sensitivity")}: " +
                                    "${(swipeSensitivity * 100).roundToInt()}%",
                            )
                        },
                        description = { Text(Localization.get(language, "swipe_sensitivity_desc")) },
                        slider = {
                            M3SettingsSlider(
                                value = swipeSensitivity,
                                onValueChange = onSwipeSensitivityChange,
                                valueRange = 0f..1f,
                                steps = 9,
                                modifier = Modifier.fillMaxWidth(),
                            )
                        },
                    ),
                )
            } else {
                emptyList()
            },
        )
        Spacer(Modifier.height(8.dp))
        M3SettingsGroup(
            items = listOf(
                M3SettingsItem(
                    icon = Icons.Filled.SwapHoriz,
                    title = { Text(Localization.get(language, "slider_style")) },
                    dropdown = M3Dropdown(
                        value = sliderStyleLabel(language, sliderStyle),
                        options = listOf("slim", "squiggly", "wavy").map { it to sliderStyleLabel(language, it) },
                        onSelect = onSliderStyleChange,
                    ),
                ),
            ),
        )
    }
}

/** The localized name of a [PlayerBackgroundStyle]. */
private fun playerBackgroundLabelKey(style: PlayerBackgroundStyle): String = when (style) {
    PlayerBackgroundStyle.CANVAS -> "canvas"
    PlayerBackgroundStyle.GRADIENT -> "player_background_gradient"
    PlayerBackgroundStyle.BLUR -> "player_background_blur"
    PlayerBackgroundStyle.GLOW -> "player_background_glow"
    PlayerBackgroundStyle.APPLE_MUSIC -> "player_background_apple"
    PlayerBackgroundStyle.LIVE_MESH -> "player_background_mesh"
    PlayerBackgroundStyle.VISUALIZER -> "player_background_visualizer"
}

/** The localized name of a [MiniPlayerDesign]. */
private fun miniPlayerDesignLabelKey(design: MiniPlayerDesign): String = when (design) {
    MiniPlayerDesign.NEW -> "mini_player_new"
    MiniPlayerDesign.APPLE -> "mini_player_apple"
    else -> "mini_player_classic"
}

/** The localized name of a [MiniPlayerBackgroundStyle]. */
private fun miniPlayerBackgroundLabelKey(style: MiniPlayerBackgroundStyle): String = when (style) {
    MiniPlayerBackgroundStyle.GRADIENT -> "mini_player_bg_gradient"
    MiniPlayerBackgroundStyle.BLUR -> "mini_player_bg_blur"
    MiniPlayerBackgroundStyle.GLOW_MOTION -> "mini_player_bg_glow_motion"
    MiniPlayerBackgroundStyle.LIVE_MESH -> "mini_player_bg_live_mesh"
    else -> "mini_player_bg_follow_theme"
}

/** The localized name of a [ViviSliderStyle] key. */
internal fun sliderStyleLabel(language: String, style: String): String = when (style) {
    "squiggly" -> Localization.get(language, "slider_squiggly")
    "wavy" -> Localization.get(language, "slider_wavy")
    else -> Localization.get(language, "slider_slim")
}
