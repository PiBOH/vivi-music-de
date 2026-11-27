package com.music.vivi.desktop.presentation.screens.settings

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.unit.dp
import com.music.vivi.desktop.generated.resources.Res
import com.music.vivi.desktop.generated.resources.ic_rounded_chevron_right
import org.jetbrains.compose.resources.painterResource

/**
 * The 1.55 settings home: grouped sections instead of the PixelPlayerDesktop
 * tile grid. Every entry opens its restored 1.55 page; the options shared with
 * PixelPlayer read and write the same UserSettings, and the current 1.56
 * surface is untouched behind the PixelPlayer entry.
 */
// Allocated once: the hub re-composes on every navigation, the groups do not
// change between them.
private val settingsGroups = Legacy155HomeGroups

@Composable
fun ViviSettingsOverview(
    onSelectSection: (SettingsSection) -> Unit,
    modifier: Modifier = Modifier,
) {

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 28.dp),
    ) {
        SettingsHeader(
            title = "Settings",
            horizontalPadding = 0.dp,
        )

        settingsGroups.forEach { (label, sections) ->
            SettingsSubsection(title = label) {
                sections.forEachIndexed { index, section ->
                    SettingsEntryRow(
                        section = section,
                        shape = getGroupShape(index, sections.size),
                        onClick = { onSelectSection(section) },
                    )
                }
            }
            Spacer(modifier = Modifier.height(18.dp))
        }

        Spacer(modifier = Modifier.height(24.dp))
    }
}

@Composable
private fun SettingsEntryRow(
    section: SettingsSection,
    shape: androidx.compose.ui.graphics.Shape,
    onClick: () -> Unit,
) {
    val colors = MaterialTheme.colorScheme
    val iconColors = getCategoryColors(section, colors.surface.luminance() < 0.5f)

    ExpressiveSettingsRow(
        title = section.title,
        subtitle = section.subtitle,
        leadingIcon = {
            Icon(
                painter = painterResource(section.icon),
                contentDescription = null,
                tint = iconColors.second,
                modifier = Modifier.size(22.dp),
            )
        },
        trailingIcon = {
            Icon(
                painter = painterResource(Res.drawable.ic_rounded_chevron_right),
                contentDescription = null,
                tint = colors.onSurfaceVariant.copy(alpha = 0.6f),
                modifier = Modifier.size(22.dp),
            )
        },
        shape = shape,
        onClick = onClick,
    )
}
