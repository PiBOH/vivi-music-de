package com.music.vivi.desktop

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
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
import androidx.compose.material.icons.rounded.ArrowForwardIos
import androidx.compose.material.icons.rounded.Brush
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.Lyrics
import androidx.compose.material.icons.rounded.MusicNote
import androidx.compose.material.icons.rounded.PlayCircle
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material.icons.rounded.Shuffle
import androidx.compose.material.icons.rounded.SkipNext
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
 * The reference's Settings page frame, carrying this build's own settings.
 *
 * Bit's page is a centred 800 px column with 64 dp rows, a group heading above
 * each block and a switch on the right; the rows here are the options VIVI
 * Music DE really has, read from and written to `DesktopSettings`, so a switch
 * moves the same value the rest of the app reads. The reference's ACCOUNT /
 * SOURCES rows that stand for services this build does not run are not shown:
 * a settings page that lies is worse than one with fewer rows.
 */
@Composable
internal fun BcSettingsPage(language: String) {
    // Every switch writes through the store, and the store bumps the settings
    // revision, so this read refreshes the page instead of keeping stale local
    // state that the rest of the app never saw.
    val revision = settingsFileRevision()
    val state = remember(revision) { DesktopSettings.load() }
    var query by remember { mutableStateOf("") }

    fun set(transform: (DesktopSyncState) -> DesktopSyncState) = DesktopSettings.update(transform)

    fun matches(vararg text: String) =
        query.isBlank() || text.any { it.contains(query, ignoreCase = true) }

    Box(Modifier.fillMaxSize().background(BC.Page), contentAlignment = Alignment.TopCenter) {
        Column(
            Modifier
                .widthIn(max = 800.dp)
                .fillMaxWidth()
                .fillMaxHeight()
                .verticalScroll(rememberScrollState())
                .padding(start = 32.dp, end = 32.dp, top = 12.dp, bottom = 104.dp),
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

            val accountName = state.accountName.ifBlank { state.accountEmail }
            if (matches("ACCOUNT", "Account", "Not signed in")) {
                BcSettingsGroup("ACCOUNT") {
                    BcSettingsNavRow(
                        Icons.Rounded.AccountCircle,
                        "Account",
                        accountName.ifBlank { "Not signed in" },
                    )
                }
            }

            if (matches("PLAYBACK", "Autoplay next", "Skip silence", "Shuffle", "Duplicate", "Audio output")) {
                BcSettingsGroup("PLAYBACK") {
                    BcSettingsSwitchRow(
                        icon = Icons.Rounded.PlayCircle,
                        title = "Autoplay next",
                        subtitle = "When the queue ends, keep playing related tracks",
                        checked = state.autoPlayNext,
                        onCheckedChange = { on -> set { it.copy(autoPlayNext = on) } },
                    )
                    BcSettingsSwitchRow(
                        icon = Icons.Rounded.SkipNext,
                        title = "Skip silence",
                        subtitle = "Drop the silent stretches inside a track",
                        checked = state.skipSilence,
                        onCheckedChange = { on -> set { it.copy(skipSilence = on) } },
                    )
                    BcSettingsSwitchRow(
                        icon = Icons.Rounded.Shuffle,
                        title = "Keep shuffle on for new songs",
                        subtitle = "Starting a song or a playlist does not turn shuffle off",
                        checked = state.persistentShuffle,
                        onCheckedChange = { on -> set { it.copy(persistentShuffle = on) } },
                    )
                    BcSettingsSwitchRow(
                        icon = Icons.Rounded.PlayCircle,
                        title = "Keep one copy in the queue",
                        subtitle = "Adding a track already queued moves the old copy instead of repeating it",
                        checked = state.preventDuplicateTracksInQueue,
                        onCheckedChange = { on -> set { it.copy(preventDuplicateTracksInQueue = on) } },
                    )
                    BcSettingsSwitchRow(
                        icon = Icons.Rounded.VolumeOff,
                        title = "Pause when the system is muted",
                        subtitle = "Pauses playback while the output volume is muted or at zero",
                        checked = state.pauseWhenMediaMuted,
                        onCheckedChange = { on -> set { it.copy(pauseWhenMediaMuted = on) } },
                    )
                    BcSettingsNavRow(
                        Icons.Rounded.MusicNote,
                        "Audio output",
                        state.outputDeviceName.ifBlank { "System default" },
                    )
                }
            }

            if (matches("AUDIO QUALITY", "Auto", "High", "Low")) {
                BcSettingsGroup("AUDIO QUALITY") {
                    BcSettingsQualityRow(state.audioQuality) { quality ->
                        set { it.copy(audioQuality = quality) }
                    }
                }
            }

            if (matches("LYRICS", "Synced lyrics", "Follow the sung line", "Click a line", "Inline", "Blur")) {
                BcSettingsGroup("LYRICS") {
                    BcSettingsSwitchRow(
                        icon = Icons.Rounded.Lyrics,
                        title = "Synced lyrics",
                        subtitle = "Use the timed words instead of the plain text",
                        checked = state.syncedLyrics,
                        onCheckedChange = { on -> set { it.copy(syncedLyrics = on) } },
                    )
                    BcSettingsSwitchRow(
                        icon = Icons.Rounded.Lyrics,
                        title = "Follow the sung line",
                        subtitle = "Scroll the words with the track as it plays",
                        checked = state.lyricsAutoScroll,
                        onCheckedChange = { on -> set { it.copy(lyricsAutoScroll = on) } },
                    )
                    BcSettingsSwitchRow(
                        icon = Icons.Rounded.Lyrics,
                        title = "Click a line to seek",
                        subtitle = "Tapping a line moves the track to that line",
                        checked = state.lyricsClickToSeek,
                        onCheckedChange = { on -> set { it.copy(lyricsClickToSeek = on) } },
                    )
                    BcSettingsSwitchRow(
                        icon = Icons.Rounded.Lyrics,
                        title = "Inline lyric line on the player",
                        subtitle = "Prints the line being sung above the scrubber",
                        checked = state.inlineLyrics,
                        onCheckedChange = { on -> set { it.copy(inlineLyrics = on) } },
                    )
                    BcSettingsSwitchRow(
                        icon = Icons.Rounded.Lyrics,
                        title = "Blur the other lines",
                        subtitle = "Keeps the words around the sung one out of focus",
                        checked = state.lyricsStandardBlur,
                        onCheckedChange = { on -> set { it.copy(lyricsStandardBlur = on) } },
                    )
                }
            }

            if (matches("APPEARANCE", "Animations", "Round artwork", "Pure black", "Accent")) {
                BcSettingsGroup("APPEARANCE") {
                    BcSettingsSwitchRow(
                        icon = Icons.Rounded.Brush,
                        title = "Animations",
                        subtitle = "Screen transitions and the player's motion",
                        checked = state.animationsEnabled,
                        onCheckedChange = { on -> set { it.copy(animationsEnabled = on) } },
                    )
                    BcSettingsSwitchRow(
                        icon = Icons.Rounded.Brush,
                        title = "Round artwork while playing",
                        subtitle = "Draws the cover as a disc instead of a square",
                        checked = state.rotatingThumbnail,
                        onCheckedChange = { on -> set { it.copy(rotatingThumbnail = on) } },
                    )
                    BcSettingsSwitchRow(
                        icon = Icons.Rounded.Brush,
                        title = "Accent from the artwork",
                        subtitle = "Seeds the theme from the dominant colour of the cover",
                        checked = state.accentFromArtwork,
                        onCheckedChange = { on -> set { it.copy(accentFromArtwork = on) } },
                    )
                    BcSettingsSwitchRow(
                        icon = Icons.Rounded.Brush,
                        title = "Pure black",
                        subtitle = "Uses black surfaces instead of the grey ones",
                        checked = state.pureBlack,
                        onCheckedChange = { on -> set { it.copy(pureBlack = on) } },
                    )
                }
            }

            if (matches("ADVANCED", "Stats for nerds", "Frame rate", "FPS")) {
                BcSettingsGroup("ADVANCED") {
                    BcSettingsSwitchRow(
                        icon = Icons.Rounded.Analytics,
                        title = "Show stats for nerds",
                        subtitle = "Codec, bitrate and sample rate on the player",
                        checked = state.statsForNerds,
                        onCheckedChange = { on -> set { it.copy(statsForNerds = on) } },
                    )
                    BcSettingsSwitchRow(
                        icon = Icons.Rounded.Analytics,
                        title = "Show FPS",
                        subtitle = "Prints the frame rate of the window",
                        checked = state.showFps,
                        onCheckedChange = { on -> set { it.copy(showFps = on) } },
                    )
                    BcSettingsSwitchRow(
                        icon = Icons.Rounded.Analytics,
                        title = "Unlock frame rate",
                        subtitle = "Lets the window draw above the display's refresh instead of capping it",
                        checked = state.unlockFrameRate,
                        onCheckedChange = { on -> set { it.copy(unlockFrameRate = on) } },
                    )
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

/** The quality bar, drawn as the reference's equal segmented pills. */
@Composable
private fun BcSettingsQualityRow(selected: String, onSelect: (String) -> Unit) {
    // The reference offers four steps because it streams from two services.
    // This build resolves YouTube streams only, so the three it really has are
    // the whole row: Auto, High, Low.
    val options = listOf("auto" to "Auto", "high" to "High", "low" to "Low")
    Row(
        Modifier.fillMaxWidth().height(44.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        options.forEach { (value, label) ->
            val chosen = selected == value
            Box(
                Modifier
                    .height(30.dp)
                    .clip(RoundedCornerShape(7.dp))
                    .background(if (chosen) BC.White else Color.White.copy(alpha = 0.08f))
                    .clickable { onSelect(value) }
                    .padding(horizontal = 14.dp),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    label,
                    style = MaterialTheme.typography.bodySmall.copy(fontSize = 13.sp),
                    color = if (chosen) Color.Black else BC.White,
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
private fun BcSecondary(text: String, style: androidx.compose.ui.text.TextStyle) {
    Text(text, style = style, color = BC.Secondary)
}
