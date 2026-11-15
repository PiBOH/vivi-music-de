package com.music.vivi.desktop

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AccountCircle
import androidx.compose.material.icons.rounded.Analytics
import androidx.compose.material.icons.rounded.Computer
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.ArrowForwardIos
import androidx.compose.material.icons.rounded.Group
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.Language
import androidx.compose.material.icons.rounded.MusicNote
import androidx.compose.material.icons.rounded.PlayCircle
import androidx.compose.material.icons.rounded.Repeat
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material.icons.rounded.VolumeOff
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * The reference's Settings page frame: the title with its "Search settings"
 * field on the right, then ACCOUNT, LISTEN TOGETHER, SOURCES, TRIED IN THIS
 * ORDER and AUDIO QUALITY in that order, with the reference's rows and
 * controls. The playback options this build really has are kept below them, so
 * the frame matches and nothing that existed is lost.
 */
@Composable
internal fun BcSettingsPage(language: String) {
    var tray by remember { mutableStateOf(true) }
    var keepPlaying by remember { mutableStateOf(false) }
    var hideVolume by remember { mutableStateOf(false) }
    var statsForNerds by remember { mutableStateOf(false) }
    var dontRepeat by remember { mutableStateOf(true) }
    var quality by remember { mutableStateOf(1) }
    var jiosaavn by remember { mutableStateOf(false) }
    var youtube by remember { mutableStateOf(true) }
    var query by remember { mutableStateOf("") }

    fun matches(vararg text: String) =
        query.isBlank() || text.any { it.contains(query, ignoreCase = true) }

    // The reference keeps Settings in a centred column about 800 px wide rather
    // than across the whole page, which is why its detail lines wrap.
    Box(Modifier.fillMaxSize().background(BC.Page), contentAlignment = Alignment.TopCenter) {
    Column(
        Modifier
            .widthIn(max = 800.dp)
            .fillMaxWidth()
            .fillMaxHeight()
            .verticalScroll(rememberScrollState())
            .padding(start = 32.dp, end = 32.dp, top = 12.dp, bottom = 40.dp),
    ) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Text(
                "Settings",
                style = MaterialTheme.typography.headlineMedium,
                color = BC.White,
                modifier = Modifier.weight(1f),
            )
            Box(
                Modifier
                    .width(210.dp)
                    .height(34.dp)
                    .clip(RoundedCornerShape(7.dp))
                    .background(BC.Search)
                    .border(1.dp, Color.White.copy(alpha = 0.12f), RoundedCornerShape(7.dp))
                    .padding(horizontal = 10.dp),
                contentAlignment = Alignment.CenterStart,
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Rounded.Search, null, tint = BC.Secondary, modifier = Modifier.size(15.dp))
                    Spacer(Modifier.width(8.dp))
                    BasicTextField(
                        value = query,
                        onValueChange = { query = it },
                        singleLine = true,
                        cursorBrush = SolidColor(BC.White),
                        textStyle = MaterialTheme.typography.bodySmall.copy(fontSize = 13.sp, color = BC.White),
                        modifier = Modifier.fillMaxWidth(),
                        decorationBox = { inner ->
                            Box {
                                if (query.isEmpty()) {
                                    Text(
                                        "Search settings",
                                        style = MaterialTheme.typography.bodySmall.copy(fontSize = 13.sp),
                                        color = BC.Secondary,
                                    )
                                }
                                inner()
                            }
                        },
                    )
                }
            }
        }
        Spacer(Modifier.height(8.dp))

        if (matches("ACCOUNT", "Account & integrations", "Discord rich presence, Last.fm and ListenBrainz")) {
            BcSettingsGroup("ACCOUNT") {
                BcSettingsNavRow(
                    Icons.Rounded.AccountCircle,
                    "Account & integrations",
                    "Discord rich presence, Last.fm and ListenBrainz",
                )
            }
        }

        if (matches("LISTEN TOGETHER", "Listen together", "Get a code to listen with friends")) {
            BcSettingsGroup("LISTEN TOGETHER") {
                BcSettingsNavRow(Icons.Rounded.Group, "Listen together", "Get a code to share with friends")
            }
        }

        if (matches("SOURCES", "JioSaavn", "YouTube Music", "Add a source")) {
            BcSettingsGroup("SOURCES, TRIED IN THIS ORDER") {
                BcSettingsSourceRow(1, Icons.Rounded.MusicNote, "JioSaavn", "High Quality - 320kbps", jiosaavn) { jiosaavn = it }
                BcSettingsSourceRow(2, Icons.Rounded.PlayCircle, "YouTube Music", "Lossy full-catalogue catalogue", youtube) { youtube = it }
                BcSettingsNavRow(
                    Icons.Rounded.Add,
                    "Add a source",
                    "An addon server you host or were given a link to - searched and streamed over plain HTTP, no code is downloaded or run",
                )
            }
        }

        if (matches("AUDIO QUALITY", "Low", "Normal", "High", "Lossless")) {
            BcSettingsGroup("AUDIO QUALITY") {
                BcSettingsQualityRow(quality) { quality = it }
            }
        }

        if (matches("MISCELLANEOUS", "Don't repeat songs in current session", "Tray icon", "Keep playing when closed", "Hide volume bar")) {
            BcSettingsGroup("MISCELLANEOUS") {
                BcSettingsSwitchRow(
                    icon = Icons.Rounded.Repeat,
                    title = "Don't repeat songs in current session",
                    subtitle = "AutoPlay won't suggest a song already played or suggested this session",
                    checked = dontRepeat,
                    onCheckedChange = { dontRepeat = it },
                )
                BcSettingsSwitchRow(
                    icon = Icons.Rounded.Computer,
                    title = "Tray icon",
                    subtitle = "Show VIVI Music DE in the system tray, with playback controls",
                    checked = tray,
                    onCheckedChange = { tray = it },
                )
                BcSettingsSwitchRow(
                    icon = Icons.Rounded.PlayCircle,
                    title = "Keep playing when closed",
                    subtitle = "Closing the window leaves VIVI Music DE in the tray instead of quitting",
                    checked = keepPlaying,
                    onCheckedChange = { keepPlaying = it },
                )
                BcSettingsSwitchRow(
                    icon = Icons.Rounded.VolumeOff,
                    title = "Hide volume bar",
                    subtitle = "Removes the volume slider from the main player",
                    checked = hideVolume,
                    onCheckedChange = { hideVolume = it },
                )
            }
        }

        if (matches("LANGUAGE", "App language", "System default")) {
            BcSettingsGroup("LANGUAGE") {
                BcSettingsNavRow(Icons.Rounded.Language, "App language", "System default")
            }
        }

        if (matches("ADVANCED OPTIONS", "Show stats for nerds")) {
            BcSettingsGroup("ADVANCED OPTIONS") {
                BcSettingsSwitchRow(
                    icon = Icons.Rounded.Analytics,
                    title = "Show stats for nerds",
                    subtitle = "Codec, bitrate and sample rate on the player",
                    checked = statsForNerds,
                    onCheckedChange = { statsForNerds = it },
                )
            }
        }

        if (matches("PLAYBACK", "Audio output", "Default device")) {
            BcSettingsGroup("PLAYBACK") {
                BcSettingsNavRow(Icons.Rounded.MusicNote, "Audio output", "Default device")
            }
        }

        Spacer(Modifier.height(30.dp))
        Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Rounded.Settings, null, tint = BC.Secondary, modifier = Modifier.size(13.dp))
                Text(
                    "VIVI Music DE 1.56.0-beta",
                    style = MaterialTheme.typography.labelSmall,
                    color = BC.Secondary,
                )
            }
            Spacer(Modifier.height(4.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                listOf("GitHub", "Developer", "Discord", "Licenses").forEach { label ->
                    Text(
                        label,
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold),
                        color = BC.White.copy(alpha = 0.8f),
                        modifier = Modifier.clip(RoundedCornerShape(4.dp)).clickable { }.padding(2.dp),
                    )
                }
            }
            Spacer(Modifier.height(6.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Rounded.Info, null, tint = BC.Secondary.copy(alpha = 0.7f), modifier = Modifier.size(12.dp))
                Spacer(Modifier.width(4.dp))
                Text("- YouTube Music Backend", style = MaterialTheme.typography.labelSmall, color = BC.Secondary.copy(alpha = 0.8f))
            }
        }
    }
    }
}

/** One settings row: icon tile, title, detail line, chevron. */
@Composable
private fun BcSettingsNavRow(icon: ImageVector, title: String, subtitle: String) {
    Row(
        Modifier.fillMaxWidth().height(64.dp).clickable { },
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            Modifier.size(34.dp).clip(RoundedCornerShape(8.dp)).background(Color.White.copy(alpha = 0.08f)),
            contentAlignment = Alignment.Center,
        ) {
            Icon(icon, null, tint = BC.White, modifier = Modifier.size(18.dp))
        }
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f).widthIn(max = 630.dp).padding(end = 16.dp)) {
            Text(title, style = MaterialTheme.typography.titleMedium, color = BC.White)
            BcSecondary(subtitle, MaterialTheme.typography.bodySmall.copy(fontSize = 13.sp))
        }
        Icon(Icons.Rounded.ArrowForwardIos, null, tint = BC.Secondary, modifier = Modifier.size(13.dp))
    }
}

/** A source row: icon, name, quality line, switch and its "Always on" note. */
@Composable
private fun BcSettingsSourceRow(
    order: Int,
    icon: ImageVector,
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
) {
    Row(
        Modifier.fillMaxWidth().height(64.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            order.toString(),
            style = MaterialTheme.typography.bodySmall.copy(fontSize = 12.sp),
            color = BC.Secondary,
            modifier = Modifier.width(18.dp),
        )
        Box(
            Modifier.size(34.dp).clip(RoundedCornerShape(8.dp)).background(Color.White.copy(alpha = 0.08f)),
            contentAlignment = Alignment.Center,
        ) {
            Icon(icon, null, tint = BC.White, modifier = Modifier.size(18.dp))
        }
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f).widthIn(max = 630.dp)) {
            Text(title, style = MaterialTheme.typography.titleMedium, color = BC.White)
            BcSecondary(subtitle, MaterialTheme.typography.bodySmall.copy(fontSize = 13.sp))
        }
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Switch(
                checked = checked,
                onCheckedChange = onCheckedChange,
                colors = SwitchDefaults.colors(
                    checkedTrackColor = BC.White,
                    checkedThumbColor = Color.Black,
                    checkedBorderColor = Color.Transparent,
                    uncheckedTrackColor = Color.White.copy(alpha = 0.10f),
                    uncheckedThumbColor = Color.White.copy(alpha = 0.35f),
                    uncheckedBorderColor = Color.Transparent,
                ),
            )
            Spacer(Modifier.height(2.dp))
            Text("Always on", style = MaterialTheme.typography.labelSmall, color = BC.Secondary)
        }
    }
}

/** The four-step quality bar, drawn as the reference's equal segmented pills. */
@Composable
private fun BcSettingsQualityRow(selected: Int, onSelect: (Int) -> Unit) {
    Row(
        Modifier.fillMaxWidth().height(44.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        // Measured on the reference: four compact pills about 60 px wide and
        // 30 px tall, left aligned, with the chosen one filled white.
        listOf("Low", "Medium", "High", "Lossless").forEachIndexed { index, label ->
            Box(
                Modifier
                    .height(30.dp)
                    .clip(RoundedCornerShape(7.dp))
                    .background(if (selected == index) BC.White else Color.White.copy(alpha = 0.08f))
                    .clickable { onSelect(index) }
                    .padding(horizontal = 14.dp),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    label,
                    style = MaterialTheme.typography.bodySmall.copy(fontSize = 13.sp),
                    color = if (selected == index) Color.Black else BC.White,
                )
            }
        }
    }
}

@Composable
private fun BcSettingsGroup(title: String, content: @Composable () -> Unit) {
    Text(
        title,
        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold),
        color = BC.Secondary,
        modifier = Modifier.padding(top = 10.dp, bottom = 6.dp),
    )
    Column(Modifier.fillMaxWidth()) { content() }
    Spacer(Modifier.height(6.dp))
}

/** A switch row with the same icon tile and spacing as the reference's rows. */
@Composable
private fun BcSettingsSwitchRow(
    icon: ImageVector,
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
) {
    Row(
        Modifier.fillMaxWidth().height(64.dp).clickable { onCheckedChange(!checked) },
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            Modifier.size(34.dp).clip(RoundedCornerShape(8.dp)).background(Color.White.copy(alpha = 0.08f)),
            contentAlignment = Alignment.Center,
        ) {
            Icon(icon, null, tint = BC.White, modifier = Modifier.size(18.dp))
        }
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f).widthIn(max = 630.dp).padding(end = 16.dp)) {
            Text(title, style = MaterialTheme.typography.titleMedium, color = BC.White)
            BcSecondary(subtitle, MaterialTheme.typography.bodySmall.copy(fontSize = 13.sp))
        }
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(
                checkedTrackColor = BC.White,
                checkedThumbColor = Color.Black,
                checkedBorderColor = Color.Transparent,
                uncheckedTrackColor = Color.White.copy(alpha = 0.10f),
                uncheckedThumbColor = Color.White.copy(alpha = 0.35f),
                uncheckedBorderColor = Color.Transparent,
            ),
        )
    }
}

@Composable
private fun BcToggleRow(
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
) {
    Row(
        Modifier.fillMaxWidth().height(70.dp).clickable { onCheckedChange(!checked) },
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f).padding(end = 24.dp)) {
            Text(title, style = MaterialTheme.typography.titleMedium, color = BC.White)
            Spacer(Modifier.height(2.dp))
            Text(subtitle, style = MaterialTheme.typography.bodySmall, color = BC.Secondary)
        }
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(
                checkedTrackColor = BC.White,
                checkedThumbColor = Color.Black,
                checkedBorderColor = Color.Transparent,
                uncheckedTrackColor = Color.White.copy(alpha = 0.10f),
                uncheckedThumbColor = Color.White.copy(alpha = 0.35f),
                uncheckedBorderColor = Color.Transparent,
            ),
        )
    }
}

@Composable
private fun BcSecondary(text: String, style: androidx.compose.ui.text.TextStyle) {
    Text(text, style = style, color = BC.Secondary)
}
