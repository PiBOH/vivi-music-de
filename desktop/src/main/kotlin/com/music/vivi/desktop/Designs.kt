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
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Explore
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.LibraryMusic
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * The design language a screen is drawn in.
 *
 * [CLASSIC] is the layout the DE has had until now: Home's stack of shelves,
 * the album grid of Explore and the two library lists. [M3E] is the Material 3
 * Expressive one, drawn from the reference's own sketch (Home's rows of 180 dp
 * cards and the shape-morphing loader). The keys are what
 * `settings.json` holds, so they never change.
 */
enum class DesignStyle(val key: String) {
    CLASSIC("classic"),
    M3E("m3e");

    companion object {
        /** The saved value, or [CLASSIC] for an absent/unknown one. */
        fun fromKey(key: String?): DesignStyle = entries.firstOrNull { it.key == key } ?: CLASSIC
    }
}

/** A screen whose design language can be chosen one at a time. */
enum class DesignScreen(val settingKey: String) {
    HOME("homeDesign"),
    EXPLORE("exploreDesign"),
    LIBRARY("libraryDesign"),
    HISTORY("historyDesign"),
}

/** The name a style wears in the UI, in the running language. */
internal fun designStyleLabel(language: String, style: DesignStyle): String = when (style) {
    DesignStyle.CLASSIC -> Localization.get(language, "classic")
    DesignStyle.M3E -> Localization.get(language, "material_3_expressive")
}

/**
 * Appearance → Designs.
 *
 * Two previews show what the same Home looks like in each language, and tapping
 * one applies it to every screen; under them the four screens each carry their
 * own dropdown, so a screen can stay on the original layout while another is
 * Expressive. The preview is drawn from the same shapes the screens use
 * (Bit's equal cards with their names under them, the m3e 16 dp cards with
 * the title over the artwork, Explore's multi-browse lead), not from a picture:
 * a preview that lies about the layout is worse than no preview.
 */
@Composable
fun DesignsSection(
    language: String,
    home: DesignStyle,
    explore: DesignStyle,
    library: DesignStyle,
    history: DesignStyle,
    onStyleChange: (DesignScreen, DesignStyle) -> Unit,
    onApplyToAll: (DesignStyle) -> Unit,
) {
    Text(Localization.get(language, "designs"), style = MaterialTheme.typography.titleLarge)
    Spacer(Modifier.height(16.dp))

    val styles = listOf(home, explore, library, history)
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        DesignStyle.entries.forEach { style ->
            DesignPreview(
                language = language,
                style = style,
                selected = styles.all { it == style },
                modifier = Modifier.weight(1f),
                onClick = { onApplyToAll(style) },
            )
        }
    }

    Spacer(Modifier.height(16.dp))
    M3SettingsGroup(
        items = listOf(
            screenDesignRow(DesignScreen.HOME, Icons.Filled.Home, "home", home, language, onStyleChange),
            screenDesignRow(DesignScreen.EXPLORE, Icons.Filled.Explore, "explore", explore, language, onStyleChange),
            screenDesignRow(DesignScreen.LIBRARY, Icons.Filled.LibraryMusic, "library", library, language, onStyleChange),
            screenDesignRow(DesignScreen.HISTORY, Icons.Filled.History, "history", history, language, onStyleChange),
        ),
    )
}

/** Designs sub-screen: the previews and the four per-screen choices. */
@Composable
fun SettingsDesignsScreen(
    language: String,
    onBack: () -> Unit,
    home: DesignStyle,
    explore: DesignStyle,
    library: DesignStyle,
    history: DesignStyle,
    onStyleChange: (DesignScreen, DesignStyle) -> Unit,
    onApplyToAll: (DesignStyle) -> Unit,
) {
    SettingsSubScreen(language, onBack) {
        DesignsSection(
            language = language,
            home = home,
            explore = explore,
            library = library,
            history = history,
            onStyleChange = onStyleChange,
            onApplyToAll = onApplyToAll,
        )
    }
}

/** One screen's row: its name on the left, the chosen style as a dropdown. */
private fun screenDesignRow(
    screen: DesignScreen,
    icon: ImageVector,
    labelKey: String,
    value: DesignStyle,
    language: String,
    onStyleChange: (DesignScreen, DesignStyle) -> Unit,
): M3SettingsItem = M3SettingsItem(
    icon = icon,
    title = { Text(Localization.get(language, labelKey)) },
    dropdown = M3Dropdown(
        value = designStyleLabel(language, value),
        options = DesignStyle.entries.map { it.key to designStyleLabel(language, it) },
        onSelect = { key -> onStyleChange(screen, DesignStyle.fromKey(key)) },
        menuWidth = 240.dp,
    ),
)

/**
 * A style's preview: a miniature of the same Home, drawn in that language.
 *
 * The selected preview is the one all four screens are on; tapping the other
 * applies it everywhere (the per-screen rows below are the refinement).
 */
@Composable
private fun DesignPreview(
    language: String,
    style: DesignStyle,
    selected: Boolean,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
) {
    val shape = MaterialTheme.shapes.large
    Card(
        modifier = modifier
            .then(
                if (selected) Modifier.border(2.dp, MaterialTheme.colorScheme.primary, shape)
                else Modifier,
            )
            .clip(shape)
            .clickable(onClick = {
                AppLog.click("design '${designStyleLabel(language, style)}'")
                onClick()
            }),
        shape = shape,
        colors = CardDefaults.cardColors(
            containerColor = if (selected) {
                MaterialTheme.colorScheme.secondaryContainer
            } else {
                MaterialTheme.colorScheme.surfaceContainerHigh
            },
        ),
    ) {
        Column(Modifier.fillMaxWidth().padding(12.dp)) {
            Box(Modifier.fillMaxWidth().height(96.dp)) { DesignMock(style) }
            Spacer(Modifier.height(6.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                RadioButton(selected = selected, onClick = null)
                Spacer(Modifier.width(6.dp))
                Text(designStyleLabel(language, style), style = MaterialTheme.typography.titleSmall)
            }
        }
    }
}

/** The miniature itself: one shelf in Classic, three m3e rows with titles on the art. */
@Composable
private fun DesignMock(style: DesignStyle) {
    val accent = MaterialTheme.colorScheme.primary
    val soft = MaterialTheme.colorScheme.primaryContainer
    val label = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
    when (style) {
        DesignStyle.CLASSIC -> Column(Modifier.fillMaxSize()) {
            // Bit's shelf: a heading, one row of equal rounded squares and
            // their names under them, then the next shelf. No lead card and no
            // dot row, exactly as Classic's Home draws them.
            repeat(2) { index ->
                MockBar(if (index == 0) 48.dp else 40.dp, 6.dp, label)
                Spacer(Modifier.height(4.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(5.dp)) {
                    repeat(3) {
                        MockCover(30.dp, 30.dp, if (index == 0) accent else soft)
                    }
                }
                Spacer(Modifier.height(3.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(5.dp)) {
                    repeat(3) { MockBar(22.dp, 3.dp, label) }
                }
                if (index == 0) Spacer(Modifier.height(10.dp))
            }
        }
        DesignStyle.M3E -> Column(
            Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.SpaceBetween,
        ) {
            repeat(3) { index ->
                Column {
                    MockBar(if (index == 0) 40.dp else 52.dp, 6.dp, label)
                    Spacer(Modifier.height(3.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(5.dp)) {
                        MockTitleCover(46.dp, 22.dp, if (index == 0) accent else soft)
                        MockTitleCover(28.dp, 22.dp, soft)
                        MockTitleCover(28.dp, 22.dp, soft)
                    }
                }
            }
        }
    }
}

@Composable
private fun MockBar(width: Dp, height: Dp, color: Color) {
    Box(Modifier.width(width).height(height).clip(CircleShape).background(color))
}

@Composable
private fun MockCover(width: Dp, height: Dp, color: Color) {
    Box(Modifier.width(width).height(height).clip(MaterialTheme.shapes.extraSmall).background(color))
}

/** An m3e card: the cover with its title drawn on the bottom edge. */
@Composable
private fun MockTitleCover(width: Dp, height: Dp, color: Color) {
    Box(Modifier.width(width).height(height).clip(MaterialTheme.shapes.extraSmall).background(color)) {
        Box(
            Modifier
                .align(Alignment.BottomStart)
                .padding(start = 4.dp, bottom = 4.dp)
                .width(width * 0.52f)
                .height(3.dp)
                .clip(CircleShape)
                .background(PaletteInk.copy(alpha = 0.85f)),
        )
    }
}
