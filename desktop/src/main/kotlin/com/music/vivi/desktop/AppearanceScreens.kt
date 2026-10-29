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
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.unit.Constraints
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
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ViewSidebar
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Album
import androidx.compose.material.icons.filled.Brightness1
import androidx.compose.material.icons.filled.BrightnessAuto
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
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.SettingsBrightness
import androidx.compose.material.icons.filled.Swipe
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalMinimumInteractiveComponentSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.text.style.TextAlign
import com.materialkolor.PaletteStyle
import com.materialkolor.dynamiccolor.ColorSpec
import com.materialkolor.rememberDynamicColorScheme
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
    nativeTitleBar: Boolean = false,
    onNativeTitleBarChange: (Boolean) -> Unit = {},
    showRightSidebar: Boolean = true,
    onShowRightSidebarChange: (Boolean) -> Unit = {},
    onRestart: () -> Unit = {},
) {
    var showRestartDialog by remember { mutableStateOf(false) }

    // The heading carries no top padding of its own: `SettingsSubScreen` already
    // puts the preview's 16 dp between the Back row and the screen's first block,
    // and the card under it stands 16 dp below (`.md-group`), not against it.
    Text(Localization.get(language, "appearance"), style = MaterialTheme.typography.titleLarge)
    Spacer(Modifier.height(16.dp))

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
                icon = Icons.Filled.Movie,
                title = { Text(Localization.get(language, "intro")) },
                description = { Text(Localization.get(language, "show_intro_on_startup")) },
                trailing = { SettingsChevron() },
                onClick = onOpenIntro,
            ),
        ),
    )

    // The hub's second card: the preview gives every card 16 dp above it
    // (`.md-group`'s `margin-top`), and the two cards of the hub were drawn one on
    // top of the other.
    Spacer(Modifier.height(16.dp))
    M3SettingsGroup(
        items = listOf(
            // A destination, not a switch: the master switch, the transitions it
            // governs, their style and the speed live on the screen this opens,
            // instead of one of them sitting in this hub and the rest elsewhere.
            M3SettingsItem(
                icon = Icons.Filled.MotionPhotosOn,
                title = { Text(Localization.get(language, "animations")) },
                // The row is a door, so it describes the screen behind it (the
                // switch, the transitions, their style and the speed) instead of
                // repeating `animations_desc`, which is the master switch's own
                // sentence on that screen.
                description = { Text(Localization.get(language, "animations_hub_desc")) },
                trailing = { SettingsChevron() },
                onClick = onOpenTransitions,
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
                TextButton(onClick = { showRestartDialog = false }) {
                    Text(Localization.get(language, "later"))
                }
            },
        )
    }
}

/**
 * Theme sub-screen, in the redesign page's own order: the heading, the live
 * preview card, the four mode circles, the accent palette and the colour wheel
 * under them. Mirrors the Android `ThemeScreen` for what it offers.
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
        // The heading carries no top padding of its own: `SettingsSubScreen`
        // already puts the page's 16 dp under the Back row.
        Text(Localization.get(language, "theme_colors"), style = MaterialTheme.typography.titleLarge)

        // Live preview card (uses the currently applied theme).
        ThemePreviewCard(Modifier.fillMaxWidth().padding(top = 16.dp).height(140.dp))

        // The page's own section title: 16 dp under the card, and the circles 12
        // dp under it (`.md-section-title`, then `.md-modes`' own margin).
        Text(
            Localization.get(language, "theme_mode"),
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.padding(top = 16.dp),
        )
        FlowRow(
            Modifier.fillMaxWidth().padding(top = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
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
        // must always be visible"). The page opens with it closed instead; the
        // close button in its header puts it away and the custom circle brings
        // it back, so it is never more than one click from either state.
        var showCustomPicker by remember { mutableStateOf(true) }

        // The page's own column: "Accent color", the circles, then the wheel
        // under them. The previous layout put the palette and the picker side by
        // side so the wheel sat beside the colours, which the page does not draw
        // and which made one screen read as two.
        //
        // "Accent color", not "Color palette": the block is the accent the theme
        // is built from, not a catalogue of colours (the same heading the
        // redesign preview carries). The key already existed, so no new string
        // had to be translated.
        Text(
            Localization.get(language, "accent_color"),
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.padding(top = 16.dp),
        )
        // Every circle in one flow - the presets, the colours *Add to palette*
        // saved (painted with their own hex) and the custom-colour circle last -
        // which is where the page puts them (`.md-palette`: a grid of 40 dp
        // circles, gap 12, filled automatically), instead of rows of six with the
        // saved ones under a heading of their own.
        //
        // A preset shows the primary its scheme builds, the accent softened to
        // tone 40 in a light scheme and 80 in a dark one, which is the fill the
        // page computes (`accentSwatch`); a saved colour shows itself, because
        // that is the colour that was chosen.
        val dark = isAppInDarkTheme()
        PaletteGrid(Modifier.fillMaxWidth().padding(top = 12.dp)) {
            AccentPalette.colors.forEach { entry ->
                val isSelected = if (entry.color == Color.Transparent) {
                    accent == Color.Transparent
                } else {
                    accent == entry.color
                }
                val seed = if (entry.color == Color.Transparent) {
                    AccentPalette.effective(entry.color)
                } else {
                    entry.color
                }
                val scheme = rememberDynamicColorScheme(
                    seedColor = seed,
                    isDark = dark,
                    specVersion = ColorSpec.SpecVersion.SPEC_2025,
                    style = PaletteStyle.TonalSpot,
                )
                val colorName = Localization.get(language, "accent_${entry.key}")
                Tooltip(colorName) {
                    AccentSwatch(
                        fill = scheme.primary,
                        ink = scheme.onPrimary,
                        selected = isSelected,
                        // The system colour is not one of the app's own, so its
                        // circle says whose it is: the page draws the OS's mark
                        // (`osLogo`) and falls back to `brightness_auto` where
                        // there is no mark, which is the glyph available here.
                        mark = if (entry.color == Color.Transparent) {
                            {
                                Icon(
                                    Icons.Filled.BrightnessAuto,
                                    contentDescription = null,
                                    tint = scheme.onPrimary,
                                    modifier = Modifier.size(18.dp),
                                )
                            }
                        } else {
                            null
                        },
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
            customAccents.forEach { argb ->
                val saved = argbIntToColor(argb)
                val isSelected = accent == saved
                Tooltip(Localization.get(language, "remove_custom_color")) {
                    Box {
                        AccentSwatch(
                            fill = saved,
                            ink = contrastInk(saved),
                            selected = isSelected,
                            onClick = { onAccentChange(saved) },
                        )
                        // Dropping a colour the user added is an action and not a
                        // drawing detail, so the badge stays even though the page
                        // draws no removal: reported, not removed.
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
            // The way into the wheel, last in the flow and in every state: the
            // page draws the colour wheel itself there - a spectrum disc with a
            // dark hub and a `+` - so the way in never changes shape once a
            // colour is in use.
            Tooltip(Localization.get(language, "custom_color")) {
                CustomColorSwatch(
                    selected = showCustomPicker,
                    onClick = {
                        // Seeded from the accent in use, so the circle opens ON
                        // the current colour, and put away on a second press.
                        val hsv = colorToHsv(accent)
                        hue = hsv[0]
                        saturation = hsv[1]
                        brightness = hsv[2]
                        showCustomPicker = !showCustomPicker
                    },
                )
            }
        }

        // The wheel, 16 dp under the palette and only while it is open: the
        // page's `.md-group` around it is a block of the page and not a card, so
        // its title and its close button sit on the page's own surface.
        if (showCustomPicker) {
            val alreadySaved = remember(customAccents, customColor) {
                customAccents.any { argbIntToColor(it) == customColor }
            }
            Column(Modifier.fillMaxWidth().padding(top = 16.dp).groupEnter()) {
                // As wide as the wheel's box, so the close button lands on the
                // wheel's right edge (`.md-picker__head`).
                Row(
                    Modifier.width(WheelSize + 2.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        Localization.get(language, "custom_color"),
                        style = MaterialTheme.typography.titleMedium,
                        modifier = Modifier.weight(1f),
                    )
                    // The page's head button is 32 dp with a 20 dp glyph
                    // (`.md-picker__head`), so the head row is 32 dp tall and
                    // not Material's 48 dp touch target: a settings screen on a
                    // desktop is a pointer surface, and the extra 16 dp would
                    // push the wheel down from where the page puts it.
                    CompositionLocalProvider(LocalMinimumInteractiveComponentSize provides 0.dp) {
                        M3IconButton(
                            onClick = { showCustomPicker = false },
                            modifier = Modifier.size(32.dp),
                        ) {
                            Icon(
                                Icons.Filled.Close,
                                contentDescription = Localization.get(language, "login_hide"),
                                modifier = Modifier.size(20.dp),
                            )
                        }
                    }
                }
                // The wheel and the strip that belongs to it, on the page's own
                // 32 dp gap, and the app's intensity beside them: the page no
                // longer draws intensity, so it is kept and named rather than
                // dropped.
                Row(
                    Modifier.padding(top = 12.dp),
                    horizontalArrangement = Arrangement.spacedBy(32.dp),
                    verticalAlignment = Alignment.Top,
                ) {
                    HsvColorWheel(
                        hue = hue,
                        saturation = saturation,
                        brightness = brightness,
                        onPick = { h, s -> hue = h; saturation = s },
                    )
                    Tooltip(Localization.get(language, "brightness")) {
                        BrightnessStrip(
                            hue = hue,
                            saturation = saturation,
                            brightness = brightness,
                            onBrightnessChange = { brightness = it },
                        )
                    }
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
                // The hex field and the button that saves the colour: UNDER the
                // wheel, centred with it.
                Column(
                    Modifier.padding(top = 16.dp).width(WheelSize),
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
                                        val hsv = colorToHsv(opaqueRgb(rgb))
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
}

/** The colour wheel's diameter, and the height of the strip beside it. */
private val WheelSize = 220.dp

/** The width one of the strips beside the wheel takes. */
private val StripSize = 20.dp

/**
 * The width of the caption under the intensity strip: the widest text beside
 * the wheel, which is what that strip is centred in.
 */
private val StripCaptionWidth = 92.dp

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
            // The page's own 1 dp outline around the disc (`.md-picker__wheel`).
            .border(1.dp, MaterialTheme.colorScheme.outlineVariant, CircleShape)
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
                    colors = HueSpectrum,
                    center = middle,
                ),
                radius = r,
                center = middle,
            )
            // The page's saturation ramp reaches fully transparent at 70 % of
            // the radius (`radial-gradient(..., #fff 0%, transparent 70%)`), so
            // the last third of the disc is the hue itself.
            drawCircle(
                brush = Brush.radialGradient(
                    0f to PaletteInk,
                    0.7f to PaletteInk.copy(alpha = 0f),
                    1f to PaletteInk.copy(alpha = 0f),
                    center = middle,
                    radius = r,
                ),
                radius = r,
                center = middle,
            )
            if (brightness < 1f) {
                drawCircle(PaletteBlack.copy(alpha = 1f - brightness), radius = r, center = middle)
            }
            val angle = Math.toRadians(hue.toDouble())
            val marker = Offset(
                x = middle.x + (r * saturation * cos(angle)).toFloat(),
                y = middle.y + (r * saturation * sin(angle)).toFloat(),
            )
            // The page's marker: a 16 dp ring of white, 2 dp thick, with the
            // colour being picked showing through it and a soft dark edge so a
            // white ring still reads on a white part of the disc.
            val markRadius = 8.dp.toPx() - 1.dp.toPx()
            drawCircle(
                color = PaletteInk,
                radius = markRadius,
                center = marker,
                style = Stroke(width = 2.dp.toPx()),
            )
            drawCircle(
                color = PaletteBlack.copy(alpha = 0.28f),
                radius = markRadius + 1.dp.toPx(),
                center = marker,
                style = Stroke(width = 1.dp.toPx()),
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
            // The page's pill, outlined by 1 dp of `outlineVariant`
            // (`.md-picker__strip`).
            .clip(CircleShape)
            // The bottom of a brightness ramp is the theme's scrim, not a literal
            // black: it is the same ink in every scheme, and it is the role M3
            // provides for "the darkest thing on this surface".
            .background(
                Brush.verticalGradient(
                    listOf(hsvToColor(hue, saturation, 1f), MaterialTheme.colorScheme.scrim),
                ),
            )
            .border(1.dp, MaterialTheme.colorScheme.outlineVariant, CircleShape)
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
                // The page's thumb: a 10 dp pill with a 2 dp white border and a
                // small shadow, drawn on the strip and clipped by its ends
                // (`.md-picker__thumb`). White is `mode-light`, an ink the page
                // fixes because the marker sits on an arbitrary colour.
                .border(2.dp, PaletteInk, CircleShape)
                .shadow(2.dp, CircleShape),
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
            .clip(CircleShape)
            .background(Brush.verticalGradient(listOf(accent, adjustAccentIntensity(accent, 0f))))
            .border(1.dp, MaterialTheme.colorScheme.outlineVariant, CircleShape)
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
                    // The ticks sit on the accent itself, so they take the ink the
                    // accent can carry (a white tick disappears on Yellow).
                    .background(contrastInk(accent).copy(alpha = 0.5f)),
            )
        }
        val thumbY = ((1f - intensity) * maxHeight.value).dp.coerceIn(0.dp, maxHeight)
        Box(
            Modifier
                .offset(y = thumbY - 5.dp)
                .height(10.dp)
                .fillMaxWidth()
                .border(2.dp, PaletteInk, CircleShape)
                .shadow(2.dp, CircleShape),
        )
    }
}

/**
 * One of the four mode circles: the page's `.md-mode`.
 *
 * A 64 dp circle filled with the scheme it stands for - the split for the
 * system's own choice, white for light, the near-black for dark, true black for
 * pure black - with `brightness_auto` on the split so the system circle says it
 * follows the system. Selection is the page's own: the border turns primary, a
 * 3 dp primary ring stands outside it and the circle grows by 4 %, while the
 * label takes the primary colour and a heavier weight.
 */
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
    val scale by animateFloatAsState(
        targetValue = if (selected) 1.04f else 1f,
        animationSpec = tween(
            durationMillis = Animations.ms(Motion.SHORT4),
            easing = Animations.emphasizedDecelerate,
        ),
        label = "themeModeScale",
    )
    val fill = when {
        // The page's `--md-comp-mode-split` is a hard split, not a fade: a conic
        // whose white half is the right one, so the two schemes read as a
        // half-and-half disc. Compose's sweep starts at 3 o'clock, which puts the
        // white half at 270..90 degrees - the right side - with the two stops
        // 0.25 apart at each edge.
        isAuto -> Brush.sweepGradient(
            0f to PaletteInk,
            0.249f to PaletteInk,
            0.25f to ComponentDark,
            0.749f to ComponentDark,
            0.75f to PaletteInk,
            1f to PaletteInk,
        )
        pureBlack -> SolidColor(PaletteBlack)
        previewDark -> SolidColor(ComponentDark)
        else -> SolidColor(PaletteInk)
    }
    val ring = MaterialTheme.colorScheme.primary.copy(alpha = 0.35f)
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Box(
            Modifier
                .size(64.dp)
                .graphicsLayer {
                    scaleX = scale
                    scaleY = scale
                }
                .drawBehind {
                    if (selected) {
                        drawCircle(
                            color = ring,
                            radius = size.minDimension / 2f + 3.dp.toPx(),
                        )
                    }
                }
                .clip(CircleShape)
                .background(fill)
                .border(
                    width = 2.dp,
                    color = if (selected) {
                        MaterialTheme.colorScheme.primary
                    } else {
                        MaterialTheme.colorScheme.outline
                    },
                    shape = CircleShape,
                )
                .clickable(onClick = onClick),
            contentAlignment = Alignment.Center,
        ) {
            if (isAuto) {
                Icon(
                    Icons.Filled.BrightnessAuto,
                    contentDescription = null,
                    tint = ComponentModeInk,
                    modifier = Modifier.size(22.dp),
                )
            }
        }
        Text(
            label,
            // The page's label inherits the page font (14 px, `bodyMedium`) and
            // only takes a heavier weight when the circle is selected.
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal,
            color = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(top = 8.dp),
        )
    }
}

/**
 * One accent circle: the page's `.md-swatch`.
 *
 * 40 dp, a 1 dp border of the surface's own ink at a fifth of its strength, and
 * the page's selection ring - 3 dp of surface and then 6 dp of primary, drawn
 * outside the 40 dp box so the row does not move when a colour is chosen. The
 * check sits on the fill's own ink, which the scheme supplies, so a pale accent
 * cannot hide its own mark.
 */
@Composable
private fun AccentSwatch(
    fill: Color,
    ink: Color,
    selected: Boolean,
    mark: (@Composable () -> Unit)? = null,
    onClick: () -> Unit,
) {
    val ring = MaterialTheme.colorScheme.primary
    val page = MaterialTheme.colorScheme.surface
    Box(
        Modifier
            .size(40.dp)
            .drawBehind {
                if (selected) {
                    val radius = size.minDimension / 2f
                    drawCircle(ring, radius = radius + 6.dp.toPx())
                    drawCircle(page, radius = radius + 3.dp.toPx())
                }
            }
            .clip(CircleShape)
            .background(fill)
            .border(
                width = 1.dp,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.2f),
                shape = CircleShape,
            )
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        if (selected) {
            Icon(
                Icons.Filled.Check,
                contentDescription = null,
                tint = ink,
                modifier = Modifier.size(18.dp),
            )
        } else {
            mark?.invoke()
        }
    }
}

/**
 * The palette's grid: the page's `repeat(auto-fill, minmax(44px, 1fr))`
 * (`.md-palette`).
 *
 * A fixed 12 dp gap is not the same grid: the page fits as many 44 dp columns as
 * the width allows, stretches them to share it, and stands each 40 dp circle at
 * the left of its own cell, so the pitch follows the window instead of staying
 * at 40 + 12. A FlowRow would pack the circles at one fixed gap and would also
 * let one more fit than the page's own column count, which is why this places
 * them on the page's cells instead. The 12 dp between rows is the page's row gap.
 */
@Composable
private fun PaletteGrid(modifier: Modifier = Modifier, content: @Composable () -> Unit) {
    Layout(content = content, modifier = modifier) { measurables, constraints ->
        val gap = 12.dp.roundToPx()
        val minCell = 44.dp.roundToPx()
        val side = 40.dp.roundToPx()
        val width = constraints.maxWidth
        val columns = ((width + gap) / (minCell + gap)).coerceAtLeast(1)
        // Each cell shares the row, exactly as `1fr` does.
        val cell = (width - gap * (columns - 1)).toFloat() / columns
        val placeables = measurables.map { it.measure(Constraints.fixed(side, side)) }
        val rows = if (placeables.isEmpty()) 0 else (placeables.size + columns - 1) / columns
        val height = (rows * side + (rows - 1).coerceAtLeast(0) * gap).coerceAtLeast(0)
        layout(width, height) {
            placeables.forEachIndexed { index, placeable ->
                val column = index % columns
                val row = index / columns
                placeable.place((column * (cell + gap)).toInt(), row * (side + gap))
            }
        }
    }
}

/**
 * The custom-colour circle: the page's `.md-swatch--custom`.
 *
 * A spectrum disc with a dark hub and a `+` on it, in every state - the way into
 * the wheel never changes shape once a colour is in use - and the same selection
 * ring as the other circles while the wheel is open.
 */
@Composable
private fun CustomColorSwatch(selected: Boolean, onClick: () -> Unit) {
    val ring = MaterialTheme.colorScheme.primary
    val page = MaterialTheme.colorScheme.surface
    Box(
        Modifier
            .size(40.dp)
            .drawBehind {
                if (selected) {
                    val radius = size.minDimension / 2f
                    drawCircle(ring, radius = radius + 6.dp.toPx())
                    drawCircle(page, radius = radius + 3.dp.toPx())
                }
            }
            .clip(CircleShape)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Canvas(Modifier.fillMaxSize()) {
            drawCircle(
                brush = Brush.sweepGradient(colors = HueSpectrum, center = center),
                radius = size.minDimension / 2f,
                center = center,
            )
        }
        Box(
            Modifier
                .size(22.dp)
                .clip(CircleShape)
                .background(ComponentDark),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                Icons.Filled.Add,
                contentDescription = null,
                tint = PaletteInk,
                modifier = Modifier.size(16.dp),
            )
        }
    }
}

@Composable
private fun ThemePreviewCard(modifier: Modifier = Modifier) {
    Card(
        modifier = modifier,
        shape = MaterialTheme.shapes.extraLarge,
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
                        // The page's own pill: `border-radius: full` on
                        // `.md-preview__bar`.
                        .clip(CircleShape)
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
                        .clip(MaterialTheme.shapes.small)
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
                            .clip(MaterialTheme.shapes.small)
                            .background(MaterialTheme.colorScheme.secondary),
                    )
                    Box(
                        Modifier
                            .fillMaxWidth()
                            .weight(1f)
                            .clip(MaterialTheme.shapes.small)
                            .background(MaterialTheme.colorScheme.tertiary),
                    )
                }
                Box(
                    Modifier
                        .weight(1f)
                        .fillMaxSize()
                        .clip(MaterialTheme.shapes.small)
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
    animationsEnabled: Boolean = true,
    onAnimationsEnabledChange: (Boolean) -> Unit = {},
    animationSpeed: String = "normal",
    onAnimationSpeedChange: (String) -> Unit = {},
) {
    // The style's own *Off* is the transitions switch, so the setting is set in
    // one place: off stores "off", and on restores the fade it falls back to.
    val transitionsOn = screenTransition != "off"

    Column(Modifier.fillMaxWidth()) {
        Text(
            Localization.get(language, "animations"),
            style = MaterialTheme.typography.titleLarge,
            modifier = Modifier.padding(top = 16.dp),
        )
        Spacer(Modifier.height(16.dp))

        // The master switch and the transitions it governs, in one card: the
        // second row folds away with the first instead of sitting further down
        // the hub among rows about other subjects.
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
                    icon = Icons.Filled.PlayArrow,
                    title = { Text(Localization.get(language, "screen_transitions")) },
                    trailing = {
                        Switch(
                            checked = transitionsOn,
                            onCheckedChange = { on -> onScreenTransitionChange(if (on) "fade" else "off") },
                        )
                    },
                    onClick = { onScreenTransitionChange(if (transitionsOn) "off" else "fade") },
                    visible = animationsEnabled,
                ),
            ),
        )

        // Both choices belong to the switches above and fold with them.
        Spacer(Modifier.height(16.dp))
        M3SettingsFold(visible = animationsEnabled && transitionsOn) {
            M3SettingsDropdownItem(
                icon = Icons.Filled.Movie,
                title = Localization.get(language, "transition_style"),
                value = Localization.get(language, if (screenTransition == "slide") "transition_slide" else "transition_fade"),
                options = listOf(
                    "fade" to Localization.get(language, "transition_fade"),
                    "slide" to Localization.get(language, "transition_slide"),
                ),
                onSelect = onScreenTransitionChange,
            )
        }
        M3SettingsFold(visible = animationsEnabled) {
            M3SettingsDropdownItem(
                icon = Icons.Filled.Speed,
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
        Text(Localization.get(language, "app_font"), style = MaterialTheme.typography.titleLarge, modifier = Modifier.padding(top = 16.dp))

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

        // One card of fonts, each row drawn in the face it offers, the radio in
        // the row's own trailing slot; the import is the list's last row, so the
        // screen is the choices and nothing else (the preview's shape).
        M3SettingsGroup(
            title = Localization.get(language, "font_selection"),
            items = buildList {
                add(fontRow(language, "font_system", "font_system_desc", FontFamily.Default, selectedFont == AppFont.SYSTEM) { onFontChange(AppFont.SYSTEM) })
                add(fontRow(language, "font_google_sans", "font_google_sans_desc", AppFonts.googleSans, selectedFont == AppFont.GOOGLE_SANS) { onFontChange(AppFont.GOOGLE_SANS) })
                add(fontRow(language, "font_sans_flex", "font_sans_flex_desc", AppFonts.sansFlex, selectedFont == AppFont.SANS_FLEX) { onFontChange(AppFont.SANS_FLEX) })
                add(fontRow(language, "font_outfit", "font_outfit_desc", AppFonts.outfit, selectedFont == AppFont.OUTFIT) { onFontChange(AppFont.OUTFIT) })
                add(
                    fontRow(
                        language,
                        "font_plus_jakarta_sans",
                        "font_plus_jakarta_sans_desc",
                        AppFonts.plusJakartaSans,
                        selectedFont == AppFont.PLUS_JAKARTA_SANS,
                    ) { onFontChange(AppFont.PLUS_JAKARTA_SANS) },
                )
                if (customFontPath.isNotBlank()) {
                    add(
                        M3SettingsItem(
                            icon = Icons.Filled.FontDownload,
                            title = {
                                Text(
                                    Localization.get(language, "custom_font"),
                                    fontFamily = AppFonts.familyFor(AppFont.CUSTOM, customFontPath),
                                )
                            },
                            description = { Text(customFontPath.substringAfterLast("\\").substringAfterLast("/")) },
                            trailing = { RadioButton(selected = selectedFont == AppFont.CUSTOM, onClick = null) },
                            onClick = { onFontChange(AppFont.CUSTOM) },
                        ),
                    )
                }
                add(
                    M3SettingsItem(
                        icon = Icons.Filled.FontDownload,
                        title = { Text(Localization.get(language, "import_font")) },
                        onClick = onImportFont,
                    ),
                )
            },
        )

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

/** One font of [FontSection]'s list: its name and its own sentence, both set in
 *  the face they describe, with the radio the row is chosen by. */
@Composable
private fun fontRow(
    language: String,
    titleKey: String,
    descKey: String,
    family: FontFamily,
    selected: Boolean,
    onClick: () -> Unit,
): M3SettingsItem = M3SettingsItem(
    icon = Icons.Filled.FontDownload,
    title = { Text(Localization.get(language, titleKey), fontFamily = family) },
    description = { Text(Localization.get(language, descKey), fontFamily = family) },
    trailing = { RadioButton(selected = selected, onClick = null) },
    onClick = onClick,
)

@Composable
fun CanvasSection(
    language: String,
    canvasEnabled: Boolean,
    onCanvasEnabledChange: (Boolean) -> Unit,
    canvasSource: CanvasSource,
    onCanvasSourceChange: (CanvasSource) -> Unit,
) {
    Column(Modifier.fillMaxWidth()) {
        Text(Localization.get(language, "vivimusic_canvas"), style = MaterialTheme.typography.titleLarge, modifier = Modifier.padding(top = 16.dp))
        Spacer(Modifier.height(16.dp))
        // The page's row is the name and the switch, with nothing under them: the
        // sentence that stood here said what the Canvas is to someone who has
        // already opened its screen.
        M3SettingsGroup(
            items = listOf(
                M3SettingsItem(
                    icon = Icons.Filled.Movie,
                    title = { Text(Localization.get(language, "vivimusic_canvas")) },
                    trailing = { Switch(checked = canvasEnabled, onCheckedChange = onCanvasEnabledChange) },
                    onClick = { onCanvasEnabledChange(!canvasEnabled) },
                ),
            ),
        )
        // The source only means something while the Canvas is on (the preview
        // hides it otherwise), and it folds away with the switch it belongs to.
        M3SettingsFold(visible = canvasEnabled) {
        Spacer(Modifier.height(16.dp))
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
        Text(Localization.get(language, "density_and_grid"), style = MaterialTheme.typography.titleLarge, modifier = Modifier.padding(top = 16.dp))
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
        Spacer(Modifier.height(16.dp))
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
