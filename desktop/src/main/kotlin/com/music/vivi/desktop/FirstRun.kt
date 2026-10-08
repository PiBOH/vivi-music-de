package com.music.vivi.desktop

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.Public
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.SmartDisplay
import androidx.compose.material.icons.filled.ViewAgenda
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
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
import androidx.compose.ui.graphics.FilterQuality
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp

/**
 * The first-run flow: what a brand-new install shows before the app itself.
 *
 * Rebuilt from scratch, taking the mobile `WelcomeActivity` as the design
 * source (its four pages: welcome, permissions, community and features) and
 * leaving out what does not exist on the desktop: there is no notification or
 * battery permission to ask for, and no pager, because the desktop user is
 * holding a mouse and not a thumb. What is left is three steps, in the order the
 * mobile app has them:
 *
 *  1. **Welcome**: the logo, the title, the developer, and the language picker.
 *     The picker is a card that opens a searchable list: the desktop ships 48
 *     languages, and picking one from a scroll of 48 rows was the weak part of
 *     the screen this replaces, so the list is filtered as you type (by name and
 *     by code) and the chosen language takes effect immediately.
 *  2. **Community and support**: the four links of the mobile welcome screen, on
 *     the same cards the rest of the settings use: star the repository, join the
 *     Telegram channel, support via UPI, buy the developer a coffee.
 *  3. **Discover the desktop features**: the three things the desktop edition has
 *     that the phone does not (the now-playing widget, the media keys, the tray
 *     menu), which is the mobile "discover features" page translated to this
 *     platform.
 *
 * Where the links point. The repository and the channel are the DE ones
 * (`github.com/PiBOH/vivi-music-de` and `t.me/vivimusicde`, the same two the
 * About screen opens). The two support links are the MOBILE developer's, by
 * request: `upi://pay?pa=vividhpashokan@axl&pn=Vividh P Ashokan` and
 * `https://ko-fi.com/vividhpashokan`. They are kept exactly as the mobile
 * welcome screen has them, so both editions send support to the same person.
 *
 * When it runs. Only on a fresh install, i.e. while no language has been chosen
 * yet, and never again afterwards: the flow is not re-openable from Settings.
 * Because step 1 saves the language as soon as it is picked, [Main] keeps the
 * flow on screen with its own flag rather than by re-reading the setting: the
 * app must not appear in the middle of its own welcome. Quitting halfway still
 * leaves the language saved, so the next launch starts the app; that matches
 * "first run" being about the install, not about this particular session.
 *
 * The step content is drawn inside a scrolling column so every step fits a small
 * window, and the language list is height-limited: a first-run screen that cannot
 * be completed in a 800x600 window is a broken first-run screen.
 */
@Composable
fun FirstRunFlow(
    language: String,
    onLanguageSelected: (String) -> Unit,
    onFinish: () -> Unit,
) {
    var step by remember { mutableStateOf(0) }
    val stepCount = 3

    Column(
        Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 32.dp, vertical = 28.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        StepProgress(step = step, steps = stepCount)
        Spacer(Modifier.height(20.dp))

        when (step) {
            0 -> WelcomeStep(
                language = language,
                onLanguageSelected = onLanguageSelected,
            )
            1 -> CommunityStep(language = language)
            else -> FeaturesStep(language = language)
        }

        Spacer(Modifier.height(24.dp))
        Row(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            if (step > 0) {
                OutlinedButton(
                    onClick = { step-- },
                    modifier = Modifier.height(48.dp),
                ) {
                    Text(Localization.get(language, "back"))
                }
                Spacer(Modifier.width(12.dp))
            }
            Button(
                onClick = { if (step == stepCount - 1) onFinish() else step++ },
                modifier = Modifier.height(48.dp).width(180.dp),
            ) {
                Text(
                    Localization.get(language, if (step == stepCount - 1) "continue" else "next"),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                )
            }
        }
    }
}

/**
 * The step indicator: one dot per step, the current one wide and in the primary
 * colour. It replaces the mobile app's pager dots, which is the only part of
 * that pager this screen keeps.
 */
@Composable
private fun StepProgress(step: Int, steps: Int) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        repeat(steps) { index ->
            val current = index == step
            Box(
                Modifier
                    .height(6.dp)
                    .width(if (current) 22.dp else 6.dp)
                    .clip(CircleShape)
                    .background(
                        if (current) MaterialTheme.colorScheme.primary
                        else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.35f)
                    ),
            )
        }
    }
}

/** Step 1: the logo, who made it, and the language to run it in. */
@Composable
private fun WelcomeStep(
    language: String,
    onLanguageSelected: (String) -> Unit,
) {
    val density = LocalDensity.current.density
    val logo = remember(density) { loadLogo(104, density) }
    if (logo != null) {
        Image(
            bitmap = logo,
            contentDescription = "VIVI Music DE",
            filterQuality = FilterQuality.High,
            modifier = Modifier
                .size(104.dp)
                .clip(RoundedCornerShape(26.dp)),
        )
    }
    Spacer(Modifier.height(18.dp))
    Text(
        Localization.get(language, "welcome_title"),
        style = MaterialTheme.typography.headlineMedium,
        fontWeight = FontWeight.Bold,
        textAlign = TextAlign.Center,
    )
    Spacer(Modifier.height(8.dp))
    Text(
        Localization.get(language, "welcome_desc"),
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        textAlign = TextAlign.Center,
        modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp),
    )
    Spacer(Modifier.height(14.dp))
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        // The version the user just installed, and the person behind this
        // edition, as the mobile welcome screen shows them (its edition chip and
        // its "By ..." chip). `app_developer` and the name are the same pair the
        // About screen credits, so the two screens cannot drift apart.
        AssistChip(
            onClick = { openUrl("https://github.com/PiBOH/vivi-music-de") },
            label = { Text("${AppInfo.CHANNEL.replaceFirstChar { it.uppercase() }} ${AppInfo.DE_VERSION}") },
        )
        AssistChip(
            onClick = { openUrl("https://github.com/PiBOH") },
            label = {
                Text(Localization.get(language, "app_developer") + " (DE): PiBOH")
            },
        )
    }
    Spacer(Modifier.height(22.dp))

    Text(
        Localization.get(language, "language"),
        style = MaterialTheme.typography.titleSmall,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.fillMaxWidth().padding(start = 6.dp, bottom = 8.dp),
    )
    LanguagePicker(
        language = language,
        selected = language,
        onSelect = onLanguageSelected,
    )
}

/**
 * The language card: the current language, and a searchable list of the 48 the
 * desktop edition ships. Searching matches the native name and the code, so both
 * "ital" and "it" find Italiano, which is what someone looking for their own
 * language actually types.
 */
@Composable
private fun LanguagePicker(
    language: String,
    selected: String,
    onSelect: (String) -> Unit,
) {
    var open by remember { mutableStateOf(false) }
    var query by remember { mutableStateOf("") }
    val selectedName = Languages.name(selected)

    M3SettingsGroup(
        items = listOf(
            M3SettingsItem(
                icon = Icons.Filled.Language,
                title = { Text(Localization.get(language, "language")) },
                description = { Text(selectedName) },
                trailing = { SettingsChevron() },
                onClick = {
                    query = ""
                    open = true
                },
            ),
        ),
    )

    if (open) {
        val matches = remember(query) {
            val q = query.trim().lowercase()
            if (q.isEmpty()) {
                Languages.all
            } else {
                Languages.all.filter {
                    it.name.lowercase().contains(q) || it.code.lowercase().startsWith(q)
                }
            }
        }
        AlertDialog(
            onDismissRequest = { open = false },
            title = { Text(Localization.get(language, "language")) },
            text = {
                Column(Modifier.fillMaxWidth()) {
                    OutlinedTextField(
                        value = query,
                        onValueChange = { query = it },
                        singleLine = true,
                        leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null) },
                        placeholder = { Text(Localization.get(language, "search")) },
                        modifier = Modifier.fillMaxWidth(),
                    )
                    Spacer(Modifier.height(8.dp))
                    if (matches.isEmpty()) {
                        Text(
                            Localization.get(language, "no_results_found"),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    LazyColumn(Modifier.fillMaxWidth().height(320.dp)) {
                        items(matches, key = { it.code }) { lang ->
                            val isSelected = lang.code == selected
                            Row(
                                Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        onSelect(lang.code)
                                        open = false
                                    }
                                    .padding(horizontal = 8.dp, vertical = 12.dp),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Text(
                                    lang.name,
                                    style = MaterialTheme.typography.bodyLarge,
                                    color = if (isSelected) MaterialTheme.colorScheme.primary
                                    else MaterialTheme.colorScheme.onSurface,
                                    modifier = Modifier.weight(1f),
                                )
                                if (isSelected) {
                                    Icon(
                                        Icons.Filled.Check,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(18.dp),
                                    )
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { open = false }) {
                    Text(Localization.get(language, "close"))
                }
            },
        )
    }
}

/** Step 2: the four community and support links, on the settings cards. */
@Composable
private fun CommunityStep(language: String) {
    StepHeading(
        title = Localization.get(language, "community_section"),
        description = Localization.get(language, "privacy_desc"),
    )
    M3SettingsGroup(
        items = listOf(
            M3SettingsItem(
                icon = Icons.Filled.Public,
                title = { Text(Localization.get(language, "github_repository")) },
                description = { Text("github.com/PiBOH/vivi-music-de") },
                trailing = { SettingsChevron() },
                onClick = { openUrl("https://github.com/PiBOH/vivi-music-de") },
            ),
            M3SettingsItem(
                icon = Icons.Filled.Public,
                title = { Text(Localization.get(language, "telegram_channel")) },
                description = { Text("t.me/vivimusicde") },
                trailing = { SettingsChevron() },
                onClick = { openUrl("https://t.me/vivimusicde") },
            ),
            M3SettingsItem(
                icon = Icons.Filled.Favorite,
                title = { Text(Localization.get(language, "support_upi")) },
                description = { Text("vividhpashokan@axl") },
                trailing = { SettingsChevron() },
                onClick = { openUrl("upi://pay?pa=vividhpashokan@axl&pn=Vividh P Ashokan") },
            ),
            M3SettingsItem(
                icon = Icons.Filled.Favorite,
                title = { Text(Localization.get(language, "support_buy_me_a_coffee")) },
                description = { Text("ko-fi.com/vividhpashokan") },
                trailing = { SettingsChevron() },
                onClick = { openUrl("https://ko-fi.com/vividhpashokan") },
            ),
        ),
    )
}

/** Step 3: what is different on the desktop, which is what the phone cannot show. */
@Composable
private fun FeaturesStep(language: String) {
    StepHeading(
        title = Localization.get(language, "desktop_features"),
        description = Localization.get(language, "desktop_features_desc"),
    )
    M3SettingsGroup(
        items = listOf(
            M3SettingsItem(
                icon = Icons.Filled.SmartDisplay,
                title = { Text(Localization.get(language, "now_playing_widget")) },
                description = { Text(Localization.get(language, "now_playing_widget_desc")) },
            ),
            M3SettingsItem(
                icon = Icons.Filled.GraphicEq,
                title = { Text(Localization.get(language, "media_keys")) },
                description = { Text(Localization.get(language, "media_keys_desc")) },
            ),
            M3SettingsItem(
                icon = Icons.Filled.ViewAgenda,
                title = { Text(Localization.get(language, "tray_menu")) },
                description = { Text(Localization.get(language, "tray_menu_desc")) },
            ),
        ),
    )
}

@Composable
private fun StepHeading(title: String, description: String) {
    Text(
        title,
        style = MaterialTheme.typography.headlineSmall,
        fontWeight = FontWeight.Bold,
        textAlign = TextAlign.Center,
    )
    Spacer(Modifier.height(8.dp))
    Text(
        description,
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        textAlign = TextAlign.Center,
        modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp),
    )
    Spacer(Modifier.height(16.dp))
}
