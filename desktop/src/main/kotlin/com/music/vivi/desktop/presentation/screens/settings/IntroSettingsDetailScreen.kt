package com.music.vivi.desktop.presentation.screens.settings

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.music.vivi.desktop.generated.resources.Res
import com.music.vivi.desktop.generated.resources.ic_automirrored_rounded_arrow_back
import com.music.vivi.desktop.generated.resources.ic_rounded_auto_awesome
import com.music.vivi.desktop.generated.resources.ic_rounded_chevron_right
import com.music.vivi.desktop.generated.resources.ic_rounded_tune
import com.music.vivi.desktop.presentation.components.ExpressiveSwitchRow
import com.music.vivi.desktop.presentation.components.IntroSplash
import com.music.vivi.desktop.presentation.viewmodel.PlayerViewModel
import org.jetbrains.compose.resources.painterResource

private val introStyles = listOf(
    "logo" to "Logo only",
    "logo_name" to "Logo + name",
    "logo_tagline" to "Logo + name + version",
)

private val introBackgrounds = listOf(
    "gradient" to "Gradient",
    "glow" to "Glow",
    "dark" to "Dark",
)

/**
 * The Intro page from the 1.55 Appearance settings: the startup switch, the
 * style and background dropdowns, and a full-screen preview of the real
 * animation. The three keys live in the shared UserSettings, so switching them
 * from here and from the PixelPlayer surface is the same write.
 */
@Composable
fun IntroSettingsDetailScreen(
    viewModel: PlayerViewModel,
    onBack: () -> Unit,
) {
    val showIntroSplash by viewModel.showIntroSplash.collectAsState()
    val introStyle by viewModel.introStyle.collectAsState()
    val introBackground by viewModel.introBackground.collectAsState()
    var previewing by remember { mutableStateOf(false) }

    if (previewing) {
        Dialog(
            onDismissRequest = { previewing = false },
            properties = DialogProperties(usePlatformDefaultWidth = false),
        ) {
            IntroSplash(
                style = introStyle,
                background = introBackground,
                onFinished = { previewing = false },
            )
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState()),
    ) {
        SettingsHeader(
            title = "Intro",
            horizontalPadding = 28.dp,
            subtitle = "The animated splash drawn before the app shell",
            actions = {
                IconButton(onClick = onBack) {
                    Icon(
                        painter = painterResource(Res.drawable.ic_automirrored_rounded_arrow_back),
                        contentDescription = "Back",
                        tint = MaterialTheme.colorScheme.onSurface,
                    )
                }
            },
        )

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 28.dp),
        ) {
            SettingsSubsection(title = "STARTUP") {
                ExpressiveSwitchRow(
                    title = "Show intro on startup",
                    subtitle = "Play the animated intro every time the app opens",
                    checked = showIntroSplash,
                    onCheckedChange = { viewModel.setShowIntroSplash(it) },
                    leadingIcon = {
                        Icon(
                            painter = painterResource(Res.drawable.ic_rounded_auto_awesome),
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.secondary,
                            modifier = Modifier.size(22.dp),
                        )
                    },
                    shape = getGroupShape(0, 1),
                    modifier = Modifier.testTag("intro-show-switch"),
                )
            }

            Spacer(modifier = Modifier.height(18.dp))

            SettingsSubsection(title = "APPEARANCE") {
                IntroDropdownRow(
                    title = "Style",
                    subtitle = "What the intro shows",
                    value = introStyle,
                    options = introStyles,
                    shape = getGroupShape(0, 2),
                    onSelect = { viewModel.setIntroStyle(it) },
                )
                IntroDropdownRow(
                    title = "Background",
                    subtitle = "The backdrop behind the logo",
                    value = introBackground,
                    options = introBackgrounds,
                    shape = getGroupShape(1, 2),
                    onSelect = { viewModel.setIntroBackground(it) },
                )
            }

            Spacer(modifier = Modifier.height(18.dp))

            SettingsSubsection(title = "PREVIEW") {
                ExpressiveSettingsRow(
                    title = "Play preview",
                    subtitle = "Open the real intro full screen",
                    leadingIcon = {
                        Icon(
                            painter = painterResource(Res.drawable.ic_rounded_auto_awesome),
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.secondary,
                            modifier = Modifier.size(22.dp),
                        )
                    },
                    trailingIcon = {
                        Icon(
                            painter = painterResource(Res.drawable.ic_rounded_chevron_right),
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                            modifier = Modifier.size(22.dp),
                        )
                    },
                    shape = getGroupShape(0, 1),
                    onClick = { previewing = true },
                )
            }

            Spacer(modifier = Modifier.height(28.dp))
        }
    }
}

@Composable
private fun IntroDropdownRow(
    title: String,
    subtitle: String,
    value: String,
    options: List<Pair<String, String>>,
    shape: Shape,
    onSelect: (String) -> Unit,
) {
    var expanded by remember { mutableStateOf(false) }
    val label = options.firstOrNull { it.first == value }?.second ?: value

    Box {
        ExpressiveSettingsRow(
            title = title,
            subtitle = subtitle,
            leadingIcon = {
                Icon(
                    painter = painterResource(Res.drawable.ic_rounded_tune),
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.secondary,
                    modifier = Modifier.size(22.dp),
                )
            },
            trailingIcon = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = label,
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.primary,
                    )
                    Icon(
                        painter = painterResource(Res.drawable.ic_rounded_chevron_right),
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                        modifier = Modifier.size(22.dp),
                    )
                }
            },
            shape = shape,
            onClick = { expanded = true },
        )

        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
        ) {
            options.forEach { (key, optionLabel) ->
                DropdownMenuItem(
                    text = { Text(optionLabel) },
                    onClick = {
                        expanded = false
                        onSelect(key)
                    },
                )
            }
        }
    }
}
