package com.music.vivi.desktop

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ViewSidebar
import androidx.compose.material.icons.filled.BrightnessAuto
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.ViewAgenda
import androidx.compose.material.icons.filled.DesktopWindows
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.FontDownload
import androidx.compose.material.icons.filled.Lyrics
import androidx.compose.material.icons.filled.Movie
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.MotionPhotosOn
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.SettingsBrightness
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import kotlin.math.roundToInt

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
        // No top padding of its own: the screen host already puts the page's 16 dp
        // under the Back row, and a second one made this heading stand at 32.
        Text(
            Localization.get(language, "animations"),
            style = MaterialTheme.typography.titleLarge,
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
                    // The switch's own sentence, which the page draws and the row
                    // did not: without it the two switches of the card read as two
                    // names and nothing says what the second one turns on.
                    description = { Text(Localization.get(language, "screen_transitions_desc")) },
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

        // Both choices belong to the switches above and fold with them, each with
        // the page's 16 dp above it *inside* the fold: a spacer outside would be
        // left behind by a card that folded away, which is the gap the page does
        // not have.
        M3SettingsFold(visible = animationsEnabled && transitionsOn) {
            Spacer(Modifier.height(16.dp))
            M3SettingsDropdownItem(
                icon = Icons.Filled.Movie,
                // A standalone dropdown card is not a group of this screen's own, and
                // the page fades every card in, so it asks for the same entrance the
                // group above gets.
                modifier = Modifier.groupEnter(),
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
            Spacer(Modifier.height(16.dp))
            M3SettingsDropdownItem(
                icon = Icons.Filled.Speed,
                modifier = Modifier.groupEnter(),
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
    onSelectCustomFont: (CustomFont) -> Unit = {},
    onRenameFont: (CustomFont, String) -> Unit = { _, _ -> },
    onRemoveFont: (CustomFont) -> Unit = {},
    useFontInLyrics: Boolean = true,
    onUseFontInLyricsChange: (Boolean) -> Unit = {},
) {
    // A face renamed from this screen: which row the dialog is open for, and the
    // name being typed into it.
    var renaming by remember { mutableStateOf<CustomFont?>(null) }
    var draft by remember { mutableStateOf("") }
    // Restored to the pre-1.44.0 rich layout: a big themed typography preview
    // card + each font listed as its own radio row rendered in that typeface.
    val activeFamily = AppFonts.familyFor(selectedFont, customFontPath)

    Column(Modifier.fillMaxWidth()) {
        // No top padding of its own: `SettingsSubScreen` already puts the page's
        // 16 dp under the Back row, and a second one made this screen's heading
        // stand 32 dp down where every other settings screen stands 16.
        Text(Localization.get(language, "app_font"), style = MaterialTheme.typography.titleLarge)

        // The card a face is chosen by, in the page's own four lines: the label,
        // the quote set in the face being chosen, the figures under it (a face is
        // chosen for its digits as much as for its letters, and the quote never
        // shows them) and the face's own name - which is what makes the card
        // follow the choice instead of naming the list under it.
        // The card names the face it is drawing: a preset by its own name, an
        // imported row by the name the user gave it (its file, until renamed).
        val caption = when {
            selectedFont != AppFont.CUSTOM -> Localization.get(language, selectedFont.nameKey())
            customFontPath.isBlank() -> Localization.get(language, "custom_font")
            else -> CustomFonts.byPath(customFontPath)?.name ?: fileNameOf(customFontPath)
        }
        Card(
            shape = MaterialTheme.shapes.extraLarge,
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.primaryContainer,
                contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
            ),
            modifier = Modifier.fillMaxWidth().padding(top = 16.dp),
        ) {
            Column(Modifier.fillMaxWidth().padding(24.dp)) {
                Text(
                    Localization.get(language, "typography_preview").uppercase(),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 0.08.em,
                )
                Text(
                    Localization.get(language, "preview_text_quote"),
                    fontFamily = activeFamily,
                    style = MaterialTheme.typography.headlineMedium,
                    modifier = Modifier.padding(top = 8.dp),
                )
                Text(
                    TYPOGRAPHY_FIGURES,
                    fontFamily = activeFamily,
                    style = MaterialTheme.typography.labelLarge,
                    letterSpacing = 0.02.em,
                    color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.65f),
                    modifier = Modifier.padding(top = 8.dp),
                )
                Text(
                    caption,
                    fontFamily = activeFamily,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f),
                    modifier = Modifier.padding(top = 12.dp),
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
                // Every imported face is a row of this card, named after the face
                // it draws and carrying the two things only it can do: a rename,
                // which is only a name, and a delete, which drops its own copy
                // of the file. The radio stays where every other row keeps it.
                CustomFonts.faces.forEach { face ->
                    val family = AppFonts.familyFor(AppFont.CUSTOM, face.path)
                    add(
                        M3SettingsItem(
                            icon = Icons.Filled.FontDownload,
                            title = { Text(face.name, fontFamily = family) },
                            description = { Text(Localization.get(language, "custom_font"), fontFamily = family) },
                            trailing = {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Tooltip(Localization.get(language, "rename")) {
                                        M3IconButton(
                                            onClick = {
                                                draft = face.name
                                                renaming = face
                                            },
                                        ) {
                                            Icon(
                                                Icons.Filled.Edit,
                                                contentDescription = Localization.get(language, "rename"),
                                                modifier = Modifier.size(20.dp),
                                            )
                                        }
                                    }
                                    Tooltip(Localization.get(language, "delete")) {
                                        M3IconButton(onClick = { onRemoveFont(face) }) {
                                            Icon(
                                                Icons.Filled.Delete,
                                                contentDescription = Localization.get(language, "delete"),
                                                modifier = Modifier.size(20.dp),
                                            )
                                        }
                                    }
                                    RadioButton(
                                        selected = selectedFont == AppFont.CUSTOM && customFontPath == face.path,
                                        onClick = null,
                                    )
                                }
                            },
                            onClick = { onSelectCustomFont(face) },
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

        renaming?.let { face ->
            AlertDialog(
                onDismissRequest = { renaming = null },
                title = { Text(Localization.get(language, "rename")) },
                text = {
                    OutlinedTextField(
                        value = draft,
                        onValueChange = { draft = it },
                        singleLine = true,
                        label = { Text(Localization.get(language, "custom_font")) },
                    )
                },
                confirmButton = {
                    Button(
                        enabled = draft.isNotBlank(),
                        onClick = {
                            onRenameFont(face, draft)
                            renaming = null
                        },
                    ) { Text(Localization.get(language, "ok")) }
                },
                dismissButton = {
                    TextButton(onClick = { renaming = null }) { Text(Localization.get(language, "cancel")) }
                },
            )
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

/** The figures the typography card sets under the quote, as the page draws them:
 *  a face is chosen for its digits as much as for its letters. */
private const val TYPOGRAPHY_FIGURES = "0123456789"

/** The localization key of a preset face's name. An imported face is named
 *  after its own file instead, which is what the card's caption reads. */
private fun AppFont.nameKey(): String = when (this) {
    AppFont.SYSTEM -> "font_system"
    AppFont.GOOGLE_SANS -> "font_google_sans"
    AppFont.SANS_FLEX -> "font_sans_flex"
    AppFont.OUTFIT -> "font_outfit"
    AppFont.PLUS_JAKARTA_SANS -> "font_plus_jakarta_sans"
    AppFont.CUSTOM -> "custom_font"
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
        // No top padding of its own: `SettingsSubScreen` already puts the page's
        // 16 dp under the Back row, which is where the preview's heading stands.
        Text(Localization.get(language, "vivimusic_canvas"), style = MaterialTheme.typography.titleLarge)
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
            // The page draws this row's card inside a `.md-group`, so it fades up
            // with the groups above it as the screen arrives. A standalone
            // dropdown card is not a group of this screen's own, so the entrance
            // is asked for here.
            modifier = Modifier.groupEnter(),
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
    /** The scale actually in use, after the small-window step-down. */
    effectiveDensityScale: Float = densityScale,
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
            // The value the picker reads is the one the interface is DRAWN at: a
            // window small enough to step the scale down shows "100% → 75%", so
            // the setting and the screen agree (the owner's report: the shrink
            // was visible everywhere except here).
            value = if (effectiveDensityScale == densityScale) {
                densityLabel(densityScale)
            } else {
                "${densityLabel(densityScale)} → ${densityLabel(effectiveDensityScale)}"
            },
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
