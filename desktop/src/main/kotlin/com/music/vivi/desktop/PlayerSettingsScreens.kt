package com.music.vivi.desktop

import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Album
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.BrightnessHigh
import androidx.compose.material.icons.filled.CompareArrows
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.FastForward
import androidx.compose.material.icons.filled.FilterAlt
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.HighQuality
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.MusicOff
import androidx.compose.material.icons.filled.PlaylistAdd
import androidx.compose.material.icons.filled.QueueMusic
import androidx.compose.material.icons.filled.Repeat
import androidx.compose.material.icons.filled.Shuffle
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material.icons.filled.VolumeOff
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import kotlin.math.roundToInt

/**
 * Player & audio, as the design preview draws it: one card for the player, one
 * for the queue and one for the tail.
 *
 * The order is the phone's (`PlayerSettings.kt`), which is also the page's: the
 * player card opens on the audio quality and closes on the equalizer, and every
 * choice the two editions share sits where the phone puts it. The desktop-only
 * rows (the stream cache, autoplay next, progressive seek and the two desktop
 * switches) follow the row they belong to, in the card they belong to.
 *
 * Three things are deliberately NOT here: *Player & mini-player design* (an
 * Appearance screen, and it was drawn on both), the stream cache slider (a
 * choice with five values, so a dropdown, not a threshold on a strip) and the
 * history duration slider (the phone offers five durations, and so does this).
 */
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
    onOpenEqualizer: () -> Unit = {},
    streamCacheMinutes: Int = 10,
    onStreamCacheMinutesChange: (Int) -> Unit = {},
) {
    Text(
        Localization.get(language, "player_audio"),
        style = MaterialTheme.typography.titleLarge,
        modifier = Modifier.padding(top = 12.dp),
    )

    // The player card. Crossfade's two rows and skip silence's one row are rows
    // of this card, not cards of their own: they belong to the switch above them
    // and fold away with it.
    M3SettingsGroup(
        items = listOf(
            M3SettingsItem(
                icon = Icons.Filled.HighQuality,
                title = { Text(Localization.get(language, "audio_quality")) },
                dropdown = M3Dropdown(
                    value = audioQualityLabel(language, audioQuality),
                    options = AUDIO_QUALITY_CHOICES.map { it to audioQualityLabel(language, it) },
                    onSelect = onAudioQualityChange,
                ),
            ),
            M3SettingsItem(
                icon = Icons.Filled.Bolt,
                title = { Text(Localization.get(language, "stream_cache_minutes")) },
                description = { Text(Localization.get(language, "stream_cache_minutes_desc")) },
                dropdown = M3Dropdown(
                    value = streamCacheLabel(language, streamCacheMinutes),
                    options = STREAM_CACHE_CHOICES.map { it.toString() to streamCacheLabel(language, it) },
                    // The menu's last key is -1 ("Forever"), the store's 0.
                    onSelect = { key -> onStreamCacheMinutesChange(key.toInt().coerceAtLeast(0)) },
                ),
            ),
            M3SettingsItem(
                icon = Icons.Filled.CompareArrows,
                title = { Text(Localization.get(language, "crossfade")) },
                description = { Text(Localization.get(language, "crossfade_desc")) },
                trailing = { Switch(checked = crossfade, onCheckedChange = onToggleCrossfade) },
                onClick = { onToggleCrossfade(!crossfade) },
            ),
            M3SettingsItem(
                icon = Icons.Filled.Album,
                title = { Text(Localization.get(language, "disable_crossfade_gapless")) },
                description = { Text(Localization.get(language, "disable_crossfade_gapless_desc")) },
                visible = crossfade,
                trailing = {
                    Switch(checked = disableCrossfadeGapless, onCheckedChange = onToggleDisableCrossfadeGapless)
                },
                onClick = { onToggleDisableCrossfadeGapless(!disableCrossfadeGapless) },
            ),
            M3SettingsItem(
                icon = Icons.Filled.Timer,
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
            M3SettingsItem(
                icon = Icons.Filled.History,
                title = { Text(Localization.get(language, "history_duration")) },
                description = { Text(Localization.get(language, "history_duration_desc")) },
                dropdown = M3Dropdown(
                    value = historyDurationLabel(language, historyDurationSeconds),
                    options = HISTORY_DURATION_CHOICES.map { it.toString() to historyDurationLabel(language, it) },
                    onSelect = { key -> onHistoryDurationSecondsChange(key.toInt()) },
                ),
            ),
            M3SettingsItem(
                icon = Icons.Filled.MusicOff,
                title = { Text(Localization.get(language, "skip_silence")) },
                description = { Text(Localization.get(language, "skip_silence_desc")) },
                trailing = { Switch(checked = skipSilence, onCheckedChange = onToggleSkipSilence) },
                onClick = { onToggleSkipSilence(!skipSilence) },
            ),
            // "Instantly skip silence" is a derivative of the master toggle: it
            // only shows (and only has an effect) when "Skip silence" is on, and
            // it folds away with the card instead of vanishing from it.
            M3SettingsItem(
                icon = Icons.Filled.Bolt,
                title = { Text(Localization.get(language, "skip_silence_instant")) },
                description = { Text(Localization.get(language, "skip_silence_instant_desc")) },
                visible = skipSilence,
                trailing = { Switch(checked = skipSilenceInstant, onCheckedChange = onToggleSkipSilenceInstant) },
                onClick = { onToggleSkipSilenceInstant(!skipSilenceInstant) },
            ),
            // The phone closes its player group with the equalizer.
            M3SettingsItem(
                icon = Icons.Filled.GraphicEq,
                title = { Text(Localization.get(language, "vivi_equalizer")) },
                trailing = { SettingsChevron() },
                onClick = onOpenEqualizer,
            ),
        ),
    )

    // 12 dp is the gap the page gives every card (its `.md-card` margin), and the
    // gap every settings screen of the DE leaves between two of them.
    Spacer(Modifier.height(12.dp))
    M3SettingsGroup(
        title = Localization.get(language, "queue"),
        items = listOf(
            M3SettingsItem(
                icon = Icons.Filled.QueueMusic,
                title = { Text(Localization.get(language, "persistent_queue")) },
                trailing = { Switch(checked = persistentQueue, onCheckedChange = onTogglePersistentQueue) },
                onClick = { onTogglePersistentQueue(!persistentQueue) },
            ),
            M3SettingsItem(
                icon = Icons.Filled.PlaylistAdd,
                title = { Text(Localization.get(language, "auto_load_more")) },
                description = { Text(Localization.get(language, "auto_load_more_desc")) },
                trailing = { Switch(checked = autoLoadMore, onCheckedChange = onToggleAutoLoadMore) },
                onClick = { onToggleAutoLoadMore(!autoLoadMore) },
            ),
            M3SettingsItem(
                icon = Icons.Filled.Download,
                title = { Text(Localization.get(language, "auto_download_on_like")) },
                description = { Text(Localization.get(language, "auto_download_on_like_desc")) },
                trailing = { Switch(checked = autoDownloadOnLike, onCheckedChange = onToggleAutoDownloadOnLike) },
                onClick = { onToggleAutoDownloadOnLike(!autoDownloadOnLike) },
            ),
            M3SettingsItem(
                icon = Icons.Filled.Shuffle,
                title = { Text(Localization.get(language, "persistent_shuffle")) },
                description = { Text(Localization.get(language, "persistent_shuffle_desc")) },
                trailing = { Switch(checked = persistentShuffle, onCheckedChange = onTogglePersistentShuffle) },
                onClick = { onTogglePersistentShuffle(!persistentShuffle) },
            ),
            M3SettingsItem(
                icon = Icons.Filled.Repeat,
                title = { Text(Localization.get(language, "remember_shuffle_repeat")) },
                trailing = { Switch(checked = rememberShuffleRepeat, onCheckedChange = onToggleRememberShuffleRepeat) },
                onClick = { onToggleRememberShuffleRepeat(!rememberShuffleRepeat) },
            ),
            M3SettingsItem(
                icon = Icons.Filled.FilterAlt,
                title = { Text(Localization.get(language, "prevent_duplicate_tracks")) },
                description = { Text(Localization.get(language, "prevent_duplicate_tracks_desc")) },
                trailing = {
                    Switch(
                        checked = preventDuplicateTracksInQueue,
                        onCheckedChange = onTogglePreventDuplicateTracksInQueue,
                    )
                },
                onClick = { onTogglePreventDuplicateTracksInQueue(!preventDuplicateTracksInQueue) },
            ),
            M3SettingsItem(
                icon = Icons.Filled.Error,
                title = { Text(Localization.get(language, "auto_skip_next_on_error")) },
                description = { Text(Localization.get(language, "auto_skip_next_on_error_desc")) },
                trailing = { Switch(checked = autoSkipNextOnError, onCheckedChange = onToggleAutoSkipNextOnError) },
                onClick = { onToggleAutoSkipNextOnError(!autoSkipNextOnError) },
            ),
            // DE only: the phone has no autoplay switch.
            M3SettingsItem(
                icon = Icons.Filled.SkipNext,
                title = { Text(Localization.get(language, "autoplay_next")) },
                trailing = { Switch(checked = autoPlayNext, onCheckedChange = onToggleAutoPlayNext) },
                onClick = { onToggleAutoPlayNext(!autoPlayNext) },
            ),
        ),
    )

    // The tail card: the rows that are the desktop's own. The volume channel is
    // NOT here: it is a device-sync option and lives in Settings -> Device sync,
    // next to the other volume channel, on both editions.
    Spacer(Modifier.height(12.dp))
    M3SettingsGroup(
        items = listOf(
            M3SettingsItem(
                icon = Icons.Filled.FastForward,
                title = { Text(Localization.get(language, "progressive_seek")) },
                description = { Text(Localization.get(language, "progressive_seek_desc")) },
                trailing = { Switch(checked = progressiveSeek, onCheckedChange = onToggleProgressiveSeek) },
                onClick = { onToggleProgressiveSeek(!progressiveSeek) },
            ),
            M3SettingsItem(
                icon = Icons.Filled.VolumeOff,
                title = { Text(Localization.get(language, "pause_music_when_media_muted")) },
                trailing = { Switch(checked = pauseWhenMediaMuted, onCheckedChange = onTogglePauseWhenMediaMuted) },
                onClick = { onTogglePauseWhenMediaMuted(!pauseWhenMediaMuted) },
            ),
            M3SettingsItem(
                icon = Icons.Filled.BrightnessHigh,
                title = { Text(Localization.get(language, "keep_screen_on_player_expanded")) },
                trailing = {
                    Switch(
                        checked = keepScreenOnWhenPlayerExpanded,
                        onCheckedChange = onToggleKeepScreenOnWhenPlayerExpanded,
                    )
                },
                onClick = { onToggleKeepScreenOnWhenPlayerExpanded(!keepScreenOnWhenPlayerExpanded) },
            ),
        ),
    )
}

/** The three catalogue qualities, in the phone's order. */
private val AUDIO_QUALITY_CHOICES = listOf("auto", "high", "low")

/** The cache choices the page offers: minutes, and -1 for "Forever" (the store's 0). */
private val STREAM_CACHE_CHOICES = listOf(5, 10, 30, 60, -1)

/** The five listen-history thresholds, in seconds; 0 is "Forever" (never recorded). */
private val HISTORY_DURATION_CHOICES = listOf(10, 30, 60, 300, 0)

@Composable
private fun audioQualityLabel(language: String, quality: String): String = when (quality) {
    "high" -> Localization.get(language, "audio_quality_high")
    "low" -> Localization.get(language, "audio_quality_low")
    else -> Localization.get(language, "audio_quality_auto")
}

/** A cache choice as the row reads it: minutes, or "Forever" for 0. */
private fun streamCacheLabel(language: String, minutes: Int): String =
    if (minutes <= 0) Localization.get(language, "stream_cache_forever") else "$minutes min"

/** A history threshold as the row reads it; 0 means it is never recorded. */
private fun historyDurationLabel(language: String, seconds: Int): String = when {
    seconds <= 0 -> Localization.get(language, "stream_cache_forever")
    seconds >= 60 && seconds % 60 == 0 -> "${seconds / 60} min"
    else -> "$seconds s"
}
