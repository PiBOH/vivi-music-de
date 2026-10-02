package com.music.vivi.desktop

import androidx.compose.foundation.ContextMenuItem
import androidx.compose.foundation.ContextMenuRepresentation
import androidx.compose.foundation.ContextMenuState
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.LocalContextMenuRepresentation
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Popup
import androidx.compose.ui.window.PopupProperties
import kotlin.math.roundToInt

/**
 * App-wide Material 3 context menu.
 *
 * Compose Desktop's default text-selection menu (right click on selectable text)
 * is drawn by its own `ContextMenuRepresentation`, which looks nothing like the
 * rest of the app: a bare, unstyled box. Material 3 has no desktop context menu
 * of its own, so this provides one: the same items the default menu builds
 * (Copy / Cut / Paste / Select all, plus anything added through
 * `ContextMenuDataProvider`), drawn as an expressive `surfaceContainerHigh`
 * card with rounded corners, tonal elevation and Material 3 typography.
 *
 * Because it is installed through `LocalContextMenuRepresentation`, it also
 * covers every `ContextMenuArea` in the app, so all context menus match. The
 * macOS-only items and the ones that also exist on the APK are left untouched:
 * this only changes how the items are drawn, never which items there are.
 */
@OptIn(ExperimentalFoundationApi::class)
val Material3ContextMenuRepresentation = object : ContextMenuRepresentation {
    @Composable
    override fun Representation(state: ContextMenuState, items: () -> List<ContextMenuItem>) {
        val status = state.status
        if (status !is ContextMenuState.Status.Open) return
        Popup(
            offset = IntOffset(status.rect.left.roundToInt(), status.rect.bottom.roundToInt()),
            onDismissRequest = { state.status = ContextMenuState.Status.Closed },
            properties = PopupProperties(focusable = true),
        ) {
            Surface(
                shape = RoundedCornerShape(20.dp),
                color = MaterialTheme.colorScheme.surfaceContainerHigh,
                tonalElevation = 3.dp,
                shadowElevation = 8.dp,
                modifier = Modifier.widthIn(min = 200.dp),
            ) {
                Column(Modifier.padding(vertical = 6.dp)) {
                    items().forEach { item ->
                        Row(
                            Modifier
                                .fillMaxWidth()
                                .clickable {
                                    item.onClick()
                                    state.status = ContextMenuState.Status.Closed
                                }
                                .padding(horizontal = 16.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Text(
                                item.label,
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurface,
                            )
                        }
                    }
                }
            }
        }
    }
}

/** Installs [Material3ContextMenuRepresentation] for [content]. */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun ProvideMaterial3ContextMenu(content: @Composable () -> Unit) {
    CompositionLocalProvider(
        LocalContextMenuRepresentation provides Material3ContextMenuRepresentation,
        content = content,
    )
}
