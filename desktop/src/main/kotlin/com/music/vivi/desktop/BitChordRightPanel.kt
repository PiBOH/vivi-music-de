package com.music.vivi.desktop

import androidx.compose.foundation.background
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.PlaylistAdd
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * BitChord's right drawer: 360 dp, a hairline on the page side, and two tabs
 * over the same player the rest of the app uses. The page narrows under it; the
 * panel never covers the content. Lyrics are VIVI's renderer, not a second one.
 */
@Composable
fun BitChordRightPanel(
    nowPlaying: NowPlaying?,
    isPlaying: Boolean,
    positionMs: () -> Long,
    queue: List<NowPlaying>,
    queueIndex: Int,
    language: String,
    lyricsDisplay: LyricsDisplayOptions,
    syncedLyrics: Boolean,
    onClose: () -> Unit,
    onTogglePlay: () -> Unit,
    onSeek: (Long) -> Unit,
    onOpenLyrics: () -> Unit,
    onSkipTo: (Int) -> Unit,
    onRemoveAt: (Int) -> Unit,
    onClear: () -> Unit,
    onAddToPlaylist: ((NowPlaying) -> Unit)? = null,
) {
    var tab by remember { mutableStateOf(0) } // 0 = lyrics, 1 = queue
    val dark = isAppInDarkTheme()
    val secondary = BitChordColors.secondary()
    val primary = if (dark) Color.White else MaterialTheme.colorScheme.onSurface

    Surface(
        modifier = Modifier.fillMaxHeight().width(360.dp),
        color = if (tab == 0) BitChordColors.page() else BitChordColors.chrome(),
        shape = RoundedCornerShape(topStart = 10.dp, bottomStart = 10.dp),
    ) {
        Row(Modifier.fillMaxSize()) {
            Box(Modifier.width(1.dp).fillMaxHeight().background(BitChordColors.Divider))
            Column(Modifier.fillMaxSize()) {
                Row(
                    Modifier.fillMaxWidth().height(46.dp).padding(start = 14.dp, end = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    PanelTab("Lyrics", selected = tab == 0, color = if (tab == 0) primary else secondary) { tab = 0 }
                    Spacer(Modifier.width(18.dp))
                    PanelTab("Queue", selected = tab == 1, color = if (tab == 1) primary else secondary) { tab = 1 }
                    Spacer(Modifier.weight(1f))
                    Tooltip(Localization.get(language, "close")) {
                        Box(
                            Modifier.size(28.dp).clip(RoundedCornerShape(7.dp)).clickable(onClick = onClose),
                            contentAlignment = Alignment.Center,
                        ) {
                            Icon(
                                Icons.Filled.Close,
                                contentDescription = Localization.get(language, "close"),
                                tint = secondary,
                                modifier = Modifier.size(16.dp),
                            )
                        }
                    }
                }
                HorizontalDivider(color = BitChordColors.Divider)
                if (tab == 0) {
                    Box(Modifier.fillMaxSize()) {
                        LyricsScreen(
                            nowPlaying = nowPlaying,
                            positionMs = positionMs,
                            isPlaying = isPlaying,
                            language = language,
                            synced = syncedLyrics,
                            display = lyricsDisplay,
                            onSeek = onSeek,
                            onTogglePlay = onTogglePlay,
                            onBack = null,
                            showHeader = false,
                        )
                        Text(
                            Localization.get(language, "open_lyrics"),
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold),
                            color = secondary,
                            modifier = Modifier
                                .align(Alignment.BottomEnd)
                                .padding(10.dp)
                                .clip(RoundedCornerShape(6.dp))
                                .clickable(onClick = onOpenLyrics)
                                .padding(horizontal = 8.dp, vertical = 5.dp),
                        )
                    }
                } else {
                    QueuePanel(
                        nowPlaying = nowPlaying,
                        queue = queue,
                        queueIndex = queueIndex,
                        language = language,
                        onPlay = onSkipTo,
                        onRemove = onRemoveAt,
                        onClear = onClear,
                        onAddToPlaylist = onAddToPlaylist,
                    )
                }
            }
        }
    }
}

@Composable
private fun PanelTab(text: String, selected: Boolean, color: Color, onClick: () -> Unit) {
    Column(
        Modifier.clip(RoundedCornerShape(6.dp)).clickable(onClick = onClick).padding(vertical = 4.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            text,
            style = MaterialTheme.typography.titleSmall.copy(fontSize = 15.sp, fontWeight = FontWeight.SemiBold),
            color = color,
        )
        Spacer(Modifier.height(3.dp))
        Box(Modifier.width(if (selected) 18.dp else 0.dp).height(2.dp).background(if (selected) color else Color.Transparent))
    }
}

@Composable
private fun QueuePanel(
    nowPlaying: NowPlaying?,
    queue: List<NowPlaying>,
    queueIndex: Int,
    language: String,
    onPlay: (Int) -> Unit,
    onRemove: (Int) -> Unit,
    onClear: () -> Unit,
    onAddToPlaylist: ((NowPlaying) -> Unit)?,
) {
    val current = nowPlaying ?: queue.getOrNull(queueIndex)
    val upNext = queue.withIndex().drop((queueIndex + 1).coerceAtLeast(0))
    LazyColumn(Modifier.fillMaxSize().padding(horizontal = 12.dp, vertical = 10.dp)) {
        item("current") {
            if (current != null) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Thumbnail(current.thumbnail, Modifier.size(56.dp).clip(RoundedCornerShape(7.dp)))
                    Spacer(Modifier.width(10.dp))
                    Column(Modifier.weight(1f)) {
                        Text(
                            current.title,
                            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold),
                            color = if (isAppInDarkTheme()) Color.White else MaterialTheme.colorScheme.onSurface,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                        Text(
                            current.artist,
                            style = MaterialTheme.typography.bodySmall,
                            color = BitChordColors.secondary(),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                    if (onAddToPlaylist != null) {
                        Tooltip(Localization.get(language, "add_to_playlist")) {
                            Box(
                                Modifier.size(28.dp).clip(RoundedCornerShape(7.dp)).clickable { onAddToPlaylist(current) },
                                contentAlignment = Alignment.Center,
                            ) {
                                Icon(
                                    Icons.AutoMirrored.Filled.PlaylistAdd,
                                    contentDescription = Localization.get(language, "add_to_playlist"),
                                    tint = BitChordColors.secondary(),
                                    modifier = Modifier.size(17.dp),
                                )
                            }
                        }
                    }
                }
                Spacer(Modifier.height(14.dp))
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        Localization.get(language, "up_next"),
                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
                        color = BitChordColors.secondary(),
                        modifier = Modifier.weight(1f),
                    )
                    if (queue.isNotEmpty()) {
                        Text(
                            Localization.get(language, "clear"),
                            style = MaterialTheme.typography.labelSmall,
                            color = BitChordColors.secondary(),
                            modifier = Modifier.clip(RoundedCornerShape(6.dp)).clickable(onClick = onClear).padding(6.dp),
                        )
                    }
                }
                HorizontalDivider(color = BitChordColors.Divider, modifier = Modifier.padding(vertical = 4.dp))
            }
        }
        if (upNext.isEmpty()) {
            item("empty") {
                Text(
                    Localization.get(language, "queue_empty"),
                    style = MaterialTheme.typography.bodySmall,
                    color = BitChordColors.secondary(),
                    modifier = Modifier.padding(vertical = 16.dp),
                )
            }
        } else {
            itemsIndexed(upNext, key = { _, entry -> "${entry.index}-${entry.value.videoId}" }) { _, entry ->
                Row(
                    Modifier.fillMaxWidth().clip(RoundedCornerShape(7.dp)).clickable { onPlay(entry.index) }.padding(vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Thumbnail(entry.value.thumbnail, Modifier.size(40.dp).clip(RoundedCornerShape(6.dp)))
                    Spacer(Modifier.width(9.dp))
                    Column(Modifier.weight(1f)) {
                        Text(
                            entry.value.title,
                            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium),
                            color = if (isAppInDarkTheme()) Color.White else MaterialTheme.colorScheme.onSurface,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                        Text(
                            entry.value.artist,
                            style = MaterialTheme.typography.bodySmall,
                            color = BitChordColors.secondary(),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                    Tooltip(Localization.get(language, "remove")) {
                        Box(
                            Modifier.size(28.dp).clip(RoundedCornerShape(7.dp)).clickable { onRemove(entry.index) },
                            contentAlignment = Alignment.Center,
                        ) {
                            Icon(Icons.Filled.Delete, contentDescription = Localization.get(language, "remove"), tint = BitChordColors.secondary(), modifier = Modifier.size(16.dp))
                        }
                    }
                }
            }
        }
    }
}
