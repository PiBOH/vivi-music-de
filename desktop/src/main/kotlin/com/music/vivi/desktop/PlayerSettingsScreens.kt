package com.music.vivi.desktop

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.animation.Crossfade
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Autorenew
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.FastForward
import androidx.compose.material.icons.filled.VolumeOff
import androidx.compose.material.icons.filled.BrightnessHigh
import androidx.compose.material.icons.filled.Shuffle
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.TouchApp
import androidx.compose.material.icons.filled.FastRewind
import androidx.compose.material.icons.filled.FlashOn
import androidx.compose.material.icons.automirrored.filled.QueueMusic
import androidx.compose.material.icons.automirrored.outlined.QueueMusic
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Repeat
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.outlined.MusicNote
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.CompareArrows
import androidx.compose.material.icons.filled.DiscFull
import androidx.compose.material.icons.filled.QueueMusic
import androidx.compose.material.icons.outlined.History
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.setValue
import androidx.compose.runtime.getValue
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import kotlin.math.roundToInt
import kotlinx.coroutines.flow.map

@Composable
fun PlayerSection(
    language: String,
    autoPlayNext: Boolean,
    onToggleAutoPlayNext: (Boolean) -> Unit,
    autoLoadMore: Boolean,
    onToggleAutoLoadMore: (Boolean) -> Unit,
    preventDuplicateTracksInQueue: Boolean,
    onTogglePreventDuplicateTracksInQueue: (Boolean) -> Unit,
    autoSkipNextOnError: Boolean,
    onToggleAutoSkipNextOnError: (Boolean) -> Unit,
    pauseWhenMediaMuted: Boolean,
    onTogglePauseWhenMediaMuted: (Boolean) -> Unit,
    keepScreenOnWhenPlayerExpanded: Boolean,
    onToggleKeepScreenOnWhenPlayerExpanded: (Boolean) -> Unit,
    persistentShuffle: Boolean,
    onTogglePersistentShuffle: (Boolean) -> Unit,
    progressiveSeek: Boolean,
    onToggleProgressiveSeek: (Boolean) -> Unit,
    autoDownloadOnLike: Boolean,
    onToggleAutoDownloadOnLike: (Boolean) -> Unit,
    historyDurationSeconds: Int,
    onHistoryDurationSecondsChange: (Int) -> Unit,
    skipSilence: Boolean,
    onToggleSkipSilence: (Boolean) -> Unit,
    skipSilenceInstant: Boolean,
    onToggleSkipSilenceInstant: (Boolean) -> Unit,
    crossfade: Boolean,
    onToggleCrossfade: (Boolean) -> Unit,
    crossfadeDurationSeconds: Int,
    onCrossfadeDurationSecondsChange: (Int) -> Unit,
    disableCrossfadeGapless: Boolean,
    onToggleDisableCrossfadeGapless: (Boolean) -> Unit,
    audioQuality: String,
    onAudioQualityChange: (String) -> Unit,
    rememberShuffleRepeat: Boolean,
    onToggleRememberShuffleRepeat: (Boolean) -> Unit,
    persistentQueue: Boolean,
    onTogglePersistentQueue: (Boolean) -> Unit,
    onOpenPlayerDesign: () -> Unit = {},
    onOpenEqualizer: () -> Unit = {},
    streamCacheMinutes: Int = 10,
    onStreamCacheMinutesChange: (Int) -> Unit = {},
) {
    Text(Localization.get(language, "player_audio"), style = MaterialTheme.typography.titleLarge, modifier = Modifier.padding(top = 12.dp))

    M3SettingsGroup(
        items = listOf(
            M3SettingsItem(
                icon = Icons.Filled.MusicNote,
                title = { Text(Localization.get(language, "player_and_mini_player_design")) },
                trailing = { SettingsChevron() },
                onClick = onOpenPlayerDesign,
            ),
            M3SettingsItem(
                icon = Icons.Filled.GraphicEq,
                title = { Text(Localization.get(language, "vivi_equalizer")) },
                trailing = { SettingsChevron() },
                onClick = onOpenEqualizer,
            ),
        ),
    )

    M3SettingsDropdownItem(
        icon = Icons.Filled.GraphicEq,
        title = Localization.get(language, "audio_quality"),
        value = audioQualityLabel(language, audioQuality),
        options = listOf("auto", "high", "low").map { it to audioQualityLabel(language, it) },
        onSelect = onAudioQualityChange,
    )

    // --- Crossfade (mobile keeps these three in their own group) ---------
    // One card: the switch, the gapless exception it enables and the duration
    // that only means something once it is on. The duration used to be a card of
    // its own with a loose `Slider` under it, so a threshold of the switch above
    // read as a setting of its own instead of a part of that switch.
    M3SettingsGroup(
        items = listOf(
            M3SettingsItem(
                icon = Icons.Filled.CompareArrows,
                title = { Text(Localization.get(language, "crossfade")) },
                description = { Text(Localization.get(language, "crossfade_desc")) },
                trailing = { Switch(checked = crossfade, onCheckedChange = onToggleCrossfade) },
                onClick = { onToggleCrossfade(!crossfade) },
            ),
            M3SettingsItem(
                icon = Icons.Filled.DiscFull,
                title = { Text(Localization.get(language, "disable_crossfade_gapless")) },
                description = { Text(Localization.get(language, "disable_crossfade_gapless_desc")) },
                visible = crossfade,
                trailing = { Switch(checked = disableCrossfadeGapless, onCheckedChange = onToggleDisableCrossfadeGapless) },
                onClick = { onToggleDisableCrossfadeGapless(!disableCrossfadeGapless) },
            ),
            M3SettingsItem(
                icon = Icons.Filled.CompareArrows,
                title = { Text("${Localization.get(language, "crossfade_duration")}: $crossfadeDurationSeconds s") },
                description = { Text(Localization.get(language, "crossfade_duration_desc")) },
                visible = crossfade,
                slider = {
                    M3SettingsSlider(
                        value = crossfadeDurationSeconds.coerceIn(1, 12).toFloat(),
                        onValueChange = { onCrossfadeDurationSecondsChange(it.roundToInt().coerceIn(1, 12)) },
                        valueRange = 1f..12f,
                        steps = 10,
                        modifier = Modifier.fillMaxWidth(),
                    )
                },
            ),
        ),
    )

    // --- History duration -------------------------------------------------
    M3SettingsGroup(
        items = listOf(
            M3SettingsItem(
                icon = Icons.Filled.Schedule,
                title = { Text("${Localization.get(language, "history_duration")}: $historyDurationSeconds s") },
                description = { Text(Localization.get(language, "history_duration_desc")) },
            ),
        ),
    )
    M3SettingsSlider(
        value = historyDurationSeconds.coerceIn(1, 100).toFloat(),
        onValueChange = { onHistoryDurationSecondsChange(it.roundToInt().coerceIn(1, 100)) },
        valueRange = 1f..100f,
        steps = 98,
        modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
    )

    // --- Playback (mobile's "Player" group) ------------------------------
    M3SettingsGroup(
        items = listOfNotNull(
            M3SettingsItem(
                icon = Icons.Filled.PlayArrow,
                title = { Text(Localization.get(language, "autoplay_next")) },
                trailing = { Switch(checked = autoPlayNext, onCheckedChange = onToggleAutoPlayNext) },
                onClick = { onToggleAutoPlayNext(!autoPlayNext) },
            ),
            M3SettingsItem(
                icon = Icons.Filled.Repeat,
                title = { Text(Localization.get(language, "remember_shuffle_repeat")) },
                trailing = { Switch(checked = rememberShuffleRepeat, onCheckedChange = onToggleRememberShuffleRepeat) },
                onClick = { onToggleRememberShuffleRepeat(!rememberShuffleRepeat) },
            ),
            M3SettingsItem(
                icon = Icons.Filled.QueueMusic,
                title = { Text(Localization.get(language, "persistent_queue")) },
                trailing = { Switch(checked = persistentQueue, onCheckedChange = onTogglePersistentQueue) },
                onClick = { onTogglePersistentQueue(!persistentQueue) },
            ),
            M3SettingsItem(
                icon = Icons.Filled.Autorenew,
                title = { Text(Localization.get(language, "auto_load_more")) },
                description = { Text(Localization.get(language, "auto_load_more_desc")) },
                trailing = { Switch(checked = autoLoadMore, onCheckedChange = onToggleAutoLoadMore) },
                onClick = { onToggleAutoLoadMore(!autoLoadMore) },
            ),
            M3SettingsItem(
                icon = Icons.Filled.ContentCopy,
                title = { Text(Localization.get(language, "prevent_duplicate_tracks")) },
                description = { Text(Localization.get(language, "prevent_duplicate_tracks_desc")) },
                trailing = { Switch(checked = preventDuplicateTracksInQueue, onCheckedChange = onTogglePreventDuplicateTracksInQueue) },
                onClick = { onTogglePreventDuplicateTracksInQueue(!preventDuplicateTracksInQueue) },
            ),
            M3SettingsItem(
                icon = Icons.Filled.FastForward,
                title = { Text(Localization.get(language, "auto_skip_next_on_error")) },
                description = { Text(Localization.get(language, "auto_skip_next_on_error_desc")) },
                trailing = { Switch(checked = autoSkipNextOnError, onCheckedChange = onToggleAutoSkipNextOnError) },
                onClick = { onToggleAutoSkipNextOnError(!autoSkipNextOnError) },
            ),
            M3SettingsItem(
                icon = Icons.Filled.Shuffle,
                title = { Text(Localization.get(language, "persistent_shuffle")) },
                description = { Text(Localization.get(language, "persistent_shuffle_desc")) },
                trailing = { Switch(checked = persistentShuffle, onCheckedChange = onTogglePersistentShuffle) },
                onClick = { onTogglePersistentShuffle(!persistentShuffle) },
            ),
            M3SettingsItem(
                icon = Icons.Filled.TouchApp,
                title = { Text(Localization.get(language, "progressive_seek")) },
                description = { Text(Localization.get(language, "progressive_seek_desc")) },
                trailing = { Switch(checked = progressiveSeek, onCheckedChange = onToggleProgressiveSeek) },
                onClick = { onToggleProgressiveSeek(!progressiveSeek) },
            ),
            M3SettingsItem(
                icon = Icons.Filled.Favorite,
                title = { Text(Localization.get(language, "auto_download_on_like")) },
                description = { Text(Localization.get(language, "auto_download_on_like_desc")) },
                trailing = { Switch(checked = autoDownloadOnLike, onCheckedChange = onToggleAutoDownloadOnLike) },
                onClick = { onToggleAutoDownloadOnLike(!autoDownloadOnLike) },
            ),
            M3SettingsItem(
                icon = Icons.Filled.FastRewind,
                title = { Text(Localization.get(language, "skip_silence")) },
                description = { Text(Localization.get(language, "skip_silence_desc")) },
                trailing = { Switch(checked = skipSilence, onCheckedChange = onToggleSkipSilence) },
                onClick = { onToggleSkipSilence(!skipSilence) },
            ),
            // "Instantly skip silence" is a derivative of the master toggle: it
            // only shows (and only has an effect) when "Skip silence" is on, and
            // it folds away with the card instead of vanishing from it.
            M3SettingsItem(
                icon = Icons.Filled.FlashOn,
                title = { Text(Localization.get(language, "skip_silence_instant")) },
                description = { Text(Localization.get(language, "skip_silence_instant_desc")) },
                visible = skipSilence,
                trailing = { Switch(checked = skipSilenceInstant, onCheckedChange = onToggleSkipSilenceInstant) },
                onClick = { onToggleSkipSilenceInstant(!skipSilenceInstant) },
            ),
        ),
    )

    // --- Misc (mobile's "Misc" group) ------------------------------------
    // The volume channel is *not* here: it is a device-sync option and lives in
    // Settings -> Device sync, next to the other volume channel, on both editions.
    M3SettingsGroup(
        items = listOf(
            M3SettingsItem(
                icon = Icons.Filled.VolumeOff,
                title = { Text(Localization.get(language, "pause_music_when_media_muted")) },
                trailing = { Switch(checked = pauseWhenMediaMuted, onCheckedChange = onTogglePauseWhenMediaMuted) },
                onClick = { onTogglePauseWhenMediaMuted(!pauseWhenMediaMuted) },
            ),
            M3SettingsItem(
                icon = Icons.Filled.BrightnessHigh,
                title = { Text(Localization.get(language, "keep_screen_on_player_expanded")) },
                trailing = { Switch(checked = keepScreenOnWhenPlayerExpanded, onCheckedChange = onToggleKeepScreenOnWhenPlayerExpanded) },
                onClick = { onToggleKeepScreenOnWhenPlayerExpanded(!keepScreenOnWhenPlayerExpanded) },
            ),
        ),
    )

    // The cache's number is the row's own, so the slider is drawn in the row's
    // text column instead of floating under the card.
    M3SettingsGroup(
        items = listOf(
            M3SettingsItem(
                icon = Icons.Filled.History,
                title = {
                    Text(
                        "${Localization.get(language, "stream_cache_minutes")}: " +
                            if (streamCacheMinutes <= 0) Localization.get(language, "stream_cache_forever")
                            else "${streamCacheMinutes} min",
                    )
                },
                description = { Text(Localization.get(language, "stream_cache_minutes_desc")) },
                slider = {
                    M3SettingsSlider(
                        value = if (streamCacheMinutes <= 0) 61f else streamCacheMinutes.toFloat(),
                        onValueChange = {
                            val v = it.roundToInt()
                            // Minimum is 10 minutes (issue #25): anything below means "forever" only at 61+.
                            onStreamCacheMinutesChange(if (v >= 61) 0 else v.coerceIn(10, 60))
                        },
                        valueRange = 10f..61f,
                        steps = 50,
                        modifier = Modifier.fillMaxWidth(),
                    )
                },
            ),
        ),
    )
}

@Composable
private fun audioQualityLabel(language: String, quality: String): String = when (quality) {
    "high" -> Localization.get(language, "audio_quality_high")
    "low" -> Localization.get(language, "audio_quality_low")
    else -> Localization.get(language, "audio_quality_auto")
}
