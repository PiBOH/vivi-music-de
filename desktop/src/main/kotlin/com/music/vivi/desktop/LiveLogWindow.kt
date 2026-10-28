package com.music.vivi.desktop

import androidx.compose.foundation.LocalScrollbarStyle
import androidx.compose.foundation.ScrollbarStyle
import androidx.compose.foundation.VerticalScrollbar
import androidx.compose.foundation.defaultScrollbarStyle
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollbarAdapter
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.launch

/**
 * Dedicated real-time log viewer opened from the Developer options ("Open"
 * next to the Live monitor card). Shows [AppLog] lines as they are emitted,
 * auto-scrolled to the newest entry, with a Clear action and selectable text
 * so any line can be copied for a bug report. The window title is a native
 * title (hardcoded English), matching the existing DevTools window.
 *
 * A log is read from both ends: the newest lines while a bug happens, and the
 * first lines of a session when the report is "it goes wrong from the start".
 * So the view has a jump-to-top and a jump-to-bottom button next to the log
 * area, plus a visible scrollbar down its right side — before this, reaching
 * the other end of a 5000-line log meant dragging the mouse wheel for a long
 * time, and the wheel could not be used at all to read back through the log:
 * the auto-scroll re-pinned the view to the newest line on the next entry.
 *
 * Following the newest line is now a state, not a constant: it is on while the
 * view sits at the bottom and turns off as soon as the user scrolls up (so
 * reading an older line is no longer yanked away), and the bottom button turns
 * it back on.
 */
@Composable
fun LiveLogWindowContent(language: String, onClose: () -> Unit) {
    val entries by AppLog.entries.collectAsState()
    val listState = rememberLazyListState()
    val scope = rememberCoroutineScope()

    /** True while the view follows the newest line (see the class docs). */
    var followNewest by remember { mutableStateOf(true) }

    // Keep the view pinned to the newest line while the log grows — but only
    // when the user has not scrolled away from the bottom.
    LaunchedEffect(entries.size, followNewest) {
        if (followNewest && entries.isNotEmpty()) listState.scrollToItem(entries.lastIndex)
    }

    // Recompute "the user is at the bottom" when a scroll ENDS, never on a new
    // log line: a line arriving moves the total count, and reading that as
    // "the user scrolled away" would switch following off on its own.
    LaunchedEffect(listState) {
        snapshotFlow { listState.isScrollInProgress }
            .distinctUntilChanged()
            .collect { scrolling ->
                if (scrolling) return@collect
                val info = listState.layoutInfo
                val lastVisible = info.visibleItemsInfo.lastOrNull()?.index
                followNewest = lastVisible == null || lastVisible >= info.totalItemsCount - 1
            }
    }

    val jumpToTop: () -> Unit = {
        followNewest = false
        scope.launch { listState.scrollToItem(0) }
    }
    val jumpToBottom: () -> Unit = {
        followNewest = true
        if (entries.isNotEmpty()) scope.launch { listState.animateScrollToItem(entries.lastIndex) }
    }

    Surface(Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
        Column(Modifier.fillMaxSize().padding(12.dp)) {
            Row(
                Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    "VIVI Music DE — Live log",
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.weight(1f),
                )
                // The two ends of the log, next to the log itself: the newest
                // line is what a live report is about, the first one is where a
                // session that went wrong from the start begins.
                IconButton(onClick = jumpToTop, enabled = entries.isNotEmpty()) {
                    Icon(
                        Icons.Filled.ArrowUpward,
                        contentDescription = null,
                        modifier = Modifier.width(20.dp),
                    )
                }
                IconButton(onClick = jumpToBottom, enabled = entries.isNotEmpty()) {
                    Icon(
                        Icons.Filled.ArrowDownward,
                        contentDescription = null,
                        modifier = Modifier.width(20.dp),
                    )
                }
                TextButton(onClick = { AppLog.clear() }) {
                    Text(Localization.get(language, "tooltip_clear"))
                }
                Spacer(Modifier.width(4.dp))
                TextButton(onClick = onClose) {
                    Text(Localization.get(language, "close"))
                }
            }
            Spacer(Modifier.padding(top = 4.dp))
            Row(Modifier.fillMaxSize()) {
                Box(Modifier.weight(1f)) {
                    SelectionContainer {
                        LazyColumn(
                            state = listState,
                            modifier = Modifier.fillMaxSize(),
                            verticalArrangement = Arrangement.spacedBy(1.dp),
                        ) {
                            // Keyed by the line's unique sequence id, never by
                            // its text: identical lines are normal (a resolve
                            // logged twice in the same millisecond) and a
                            // duplicated key crashed the window with
                            // `Key "…" was already used`.
                            items(entries, key = { it.seq }) { entry ->
                                Text(
                                    entry.text,
                                    style = MaterialTheme.typography.bodySmall.copy(
                                        fontFamily = FontFamily.Monospace,
                                    ),
                                )
                            }
                        }
                    }
                }
                // The sidebar scrollbar: a drag handle for a 5000-line log,
                // where the wheel is 100 lines a flick. Themed from the app's
                // own colours instead of the platform default so it stays
                // visible in both the light and the pure-black theme.
                CompositionLocalProvider(
                    LocalScrollbarStyle provides themedScrollbarStyle(),
                ) {
                    VerticalScrollbar(
                        adapter = rememberScrollbarAdapter(listState),
                        modifier = Modifier.fillMaxHeight().padding(start = 6.dp),
                    )
                }
            }
        }
    }
}

/**
 * The desktop scrollbar's colours, taken from the current theme.
 *
 * `defaultScrollbarStyle()` is a fixed grey that disappears on the pure-black
 * background this app ships with; [MaterialTheme] is not readable from a
 * non-composable slot, so this is built where the theme is in scope.
 */
@Composable
internal fun themedScrollbarStyle(): ScrollbarStyle {
    val base = defaultScrollbarStyle()
    val fg = MaterialTheme.colorScheme.onSurface
    return base.copy(
        thickness = 10.dp,
        shape = MaterialTheme.shapes.extraSmall,
        unhoverColor = fg.copy(alpha = 0.22f),
        hoverColor = fg.copy(alpha = 0.55f),
    )
}
