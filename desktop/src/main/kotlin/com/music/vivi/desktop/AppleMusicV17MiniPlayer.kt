package com.music.vivi.desktop

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp

/**
 * Bit's mini player, the floating capsule at the bottom of the window.
 *
 * Copied from the reference's `DesktopMiniPlayer`: one capsule capped at 440 dp,
 * a 40 dp sleeve with an 8 dp radius, the title and the artist beside it, then
 * play/pause and next. Pressing anywhere else on the capsule opens the full
 * player. It floats over the page instead of taking a bar's row, which is why
 * every page keeps a bottom inset for it.
 */
@Composable
internal fun AppleMusicV17MiniPlayer(
    nowPlaying: NowPlaying,
    isPlaying: Boolean,
    isLoading: Boolean,
    language: String,
    onTogglePlay: () -> Unit,
    onNext: () -> Unit,
    onOpenPlayer: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val shape = RoundedCornerShape(percent = 50)
    Row(
        modifier
            .widthIn(max = 440.dp)
            .fillMaxWidth()
            .clip(shape)
            // The reference's bar glass sits on the page; the Bit UI has no
            // blur, so the capsule is the rail's colour with a hairline edge.
            .background(BC.Chrome)
            .border(1.dp, Color.White.copy(alpha = 0.08f), shape)
            .clickable(onClick = onOpenPlayer)
            .padding(horizontal = 12.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(Modifier.size(40.dp).clip(RoundedCornerShape(8.dp))) {
            PlayerThumbnail(nowPlaying.thumbnail, 40.dp, false)
        }
        Spacer(Modifier.width(10.dp))
        Column(Modifier.weight(1f)) {
            Text(
                nowPlaying.title,
                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                color = BC.White,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                nowPlaying.artist,
                style = MaterialTheme.typography.bodySmall,
                color = BC.Secondary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
        M3IconButton(onClick = onTogglePlay) {
            if (isLoading) {
                // The reference has no spinner here; a dimmed play is the same
                // lie either way, so the capsule keeps the player's glyph and
                // the press still toggles.
                V17Glyph(
                    if (isPlaying) V17TransportGlyph.PAUSE else V17TransportGlyph.PLAY,
                    MiniPlayGlyph,
                    BC.White.copy(alpha = 0.35f),
                )
            } else {
                V17Glyph(
                    if (isPlaying) V17TransportGlyph.PAUSE else V17TransportGlyph.PLAY,
                    MiniPlayGlyph,
                    BC.White,
                )
            }
        }
        M3IconButton(onClick = onNext) {
            V17Glyph(
                V17TransportGlyph.NEXT,
                MiniNextGlyph,
                BC.White,
                heightScale = 0.85f,
            )
        }
    }
}

/** The capsule's play/pause glyph: the reference's 24 dp icon. */
private val MiniPlayGlyph = 24.dp

/** The capsule's next glyph, beside a larger play/pause like the player's row. */
private val MiniNextGlyph = 20.dp
