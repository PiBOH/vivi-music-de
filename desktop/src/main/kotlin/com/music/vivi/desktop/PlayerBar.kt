package com.music.vivi.desktop

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.music.vivi.desktop.player.PlayerController
import com.music.vivi.desktop.player.PlayerState

/**
 * The bottom bar: the mini player, docked under the content.
 *
 * It rises into place and drops away when the full player takes over, both
 * through the shared transition (master switch aware). Its state and its
 * callbacks are handed in, so the bar can be rendered on its own.
 */
@Composable
fun NowPlayingBar(
    spotifyLayout: Boolean,
    current: Screen,
    nowPlaying: NowPlaying?,
    isPlaying: Boolean,
    playerState: PlayerState,
    player: PlayerController,
    setLocalVolume: (Float) -> Unit,
    navigate: (Screen) -> Unit,
    goBack: () -> Unit,
    showRightSidebar: Boolean,
    onToggleRightSidebar: () -> Unit,
    isFullscreen: Boolean,
    onToggleFullscreen: () -> Unit,
    language: String,
    miniPlayerDesign: MiniPlayerDesign,
    miniPlayerBackgroundStyle: MiniPlayerBackgroundStyle,
    pureBlackMiniPlayer: Boolean,
    swipeThumbnail: Boolean,
    swipeSensitivity: Float,
    sliderStyle: String,
) {
    AnimatedVisibility(
        visible = current != Screen.Player,
        enter = Animations.barEnter(),
        exit = Animations.barExit(),
    ) {
        // Spotify style: a thin top border separates the bottom bar from the
        // content above (like the desktop app).
        Column {
            if (spotifyLayout) {
                Box(
                    Modifier
                        .fillMaxWidth()
                        .height(1.dp)
                        .background(MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                )
            }
        SpotifyPlayerBar(
        nowPlaying = nowPlaying,
        isPlaying = isPlaying,
        isLoading = playerState.isLoading,
        positionMs = playerState.positionMs,
        durationMs = playerState.durationMs,
        volume = playerState.volume,
        isShuffle = playerState.isShuffle,
        repeatMode = playerState.repeatMode,
        onTogglePlay = { player.toggle() },
        onNext = { player.next() },
        onPrevious = { player.previous() },
        onSeek = { player.seekTo(it) },
        onVolume = setLocalVolume,
        onToggleShuffle = { player.toggleShuffle() },
        onCycleRepeat = { player.cycleRepeatMode() },
        onOpenPlayer = {
            if (current == Screen.Player) {
                goBack()
            } else {
                // Windows-friendly: opening the player must not resize or
                // maximize the window; the sidebar is hidden on the player
                // screen anyway.
                navigate(Screen.Player)
            }
        },
        onOpenQueue = { navigate(Screen.Queue) },
        onOpenLyrics = { navigate(Screen.Lyrics) },
        showRightSidebar = showRightSidebar,
        onToggleRightSidebar = onToggleRightSidebar,
        isFullscreen = isFullscreen,
        onToggleFullscreen = onToggleFullscreen,
        language = language,
        miniPlayerDesign = miniPlayerDesign,
        miniPlayerBackgroundStyle = miniPlayerBackgroundStyle,
        pureBlackMiniPlayer = pureBlackMiniPlayer,
        swipeThumbnail = swipeThumbnail,
        swipeSensitivity = swipeSensitivity,
        sliderStyle = ViviSliderStyle.from(sliderStyle),
    )
        }
    }
}

