package com.music.vivi.desktop

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.snap
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ProvideTextStyle
import androidx.compose.material3.Text
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.boundsInWindow
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * Desktop port of the mobile `Material3SettingsGroup` (Material 3): a column
 * of rounded cards with an optional section title, matching the mobile
 * settings look. Each item is a row with a tinted icon tile, title,
 * optional description and optional trailing content.
 */
data class M3SettingsItem(
    val icon: ImageVector?,
    val title: @Composable () -> Unit,
    /**
     * Drawn where [icon] would be, for a row whose mark is not a vector: the
     * contributor list shows each person's own avatar, and a letter tile in its
     * place would be the same row for everybody.
     */
    val leading: (@Composable () -> Unit)? = null,
    val description: (@Composable () -> Unit)? = null,
    val trailing: (@Composable () -> Unit)? = null,
    val onClick: (() -> Unit)? = null,
    val enabled: Boolean = true,
    /**
     * A choice carried by the row itself: its menu opens under it, the way
     * [M3SettingsDropdownItem] does. That composable draws a card of its own, so
     * a dropdown could never share a card with the switches it belongs to and
     * the player's design screen read as seven unrelated cards. As an item the
     * choice sits in a [M3SettingsGroup] like any other row, and the group's
     * uneven corners make it one card with its neighbours.
     */
    val dropdown: M3Dropdown? = null,
    /**
     * A control the row carries under its description, inside the row's own text
     * column: the mini player's swipe sensitivity is a slider that tunes the
     * switch above it, so it belongs to that card as a row of it, not to a card
     * of its own with the `Slider` floating under the group.
     */
    val slider: (@Composable () -> Unit)? = null,
    /**
     * Whether the row is drawn.
     *
     * A row that belongs to a switch ("Instantly skip silence" under "Skip
     * silence", the crossfade duration under "Crossfade") says so here instead
     * of being left out of the caller's list: the group keeps it while it folds
     * away, so turning the switch off closes the card by a row instead of
     * dropping one in a single frame.
     */
    val visible: Boolean = true,
)

/** The choice an [M3SettingsItem] carries, when it has one. */
data class M3Dropdown(
    val value: String,
    val options: List<Pair<String, String>>,
    val onSelect: (String) -> Unit,
    val menuWidth: Dp = 280.dp,
)

@Composable
fun M3SettingsGroup(
    title: String? = null,
    items: List<M3SettingsItem>,
    itemMinHeight: Dp? = null,
) {
    Column(Modifier.fillMaxWidth()) {
        title?.let {
            Text(
                text = it,
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(bottom = 6.dp, top = 6.dp),
            )
        }
        // The 3 dp seam is carried by the row under it (its own leading padding)
        // rather than by this column's arrangement: a row that folds away has to
        // take its seam with it, or the card would keep a double gap where it
        // closed. Each row sits in its own [AnimatedVisibility] for the same
        // reason - a row that belongs to a switch collapses upward instead of
        // being dropped between two frames.
        // The position every row that IS drawn has, by its place in the list (which
        // never changes: a row is hidden, not removed).
        val positions = HashMap<Int, Int>(items.size)
        items.forEachIndexed { index, item -> if (item.visible) positions[index] = positions.size }
        val drawnCount = positions.size
        // The position a row had the last time it was drawn. A row on its way out
        // keeps those corners while it shrinks, instead of jumping to a middle-row
        // shape in the frame the switch flipped. It is a plain map, written AFTER
        // the composition (never during it): a snapshot write in the middle of a
        // composition invalidates it again, and a recomposition on every frame is
        // what turned the fold's own transition into a stutter.
        val lastPosition = remember { HashMap<Int, Int>() }
        SideEffect { positions.forEach { (index, position) -> lastPosition[index] = position } }
        Column(Modifier.fillMaxWidth()) {
            items.forEachIndexed { index, item ->
                val position = positions[index] ?: -1
                val shownPosition = if (position >= 0) position else lastPosition[index] ?: -1
                val top = if (shownPosition == 0) 24.dp else 6.dp
                // The bottom end follows the rows that are drawn, and it MOVES there:
                // the row that becomes the last one owns the card's rounded bottom, and
                // a corner that appeared in one frame was the other half of the jolt.
                val bottom by animateDpAsState(
                    targetValue = if (shownPosition == drawnCount - 1) 24.dp else 6.dp,
                    animationSpec = tween(Animations.rowMs(), easing = Animations.emphasizedDecelerate),
                    label = "settingsRowCardBottom",
                )
                AnimatedVisibility(
                    visible = item.visible,
                    enter = Animations.rowEnter(),
                    exit = Animations.rowExit(),
                ) {
                    Column(Modifier.fillMaxWidth().padding(top = if (shownPosition == 0) 0.dp else 3.dp)) {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(
                                topStart = top,
                                topEnd = top,
                                bottomStart = bottom,
                                bottomEnd = bottom,
                            ),
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
                            ),
                            elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
                        ) {
                            M3SettingsItemRow(item = item, minHeight = itemMinHeight)
                        }
                    }
                }
            }
        }
    }
}

/**
 * A settings block that belongs to a switch (a whole card, a dropdown card or a
 * group) folding the way a row does, through [Animations.rowEnter] / [rowExit].
 *
 * A row gets this from [M3SettingsGroup] itself; this is for the blocks that are
 * not rows - the transition style card that only exists with animations on, the
 * developer options group - so that "an option that appears with a switch" folds
 * everywhere in the settings and not only inside a group.
 */
@Composable
fun M3SettingsFold(visible: Boolean, content: @Composable () -> Unit) {
    AnimatedVisibility(
        visible = visible,
        enter = Animations.rowEnter(),
        exit = Animations.rowExit(),
    ) {
        // One child: AnimatedVisibility lays several out on top of each other.
        Column(Modifier.fillMaxWidth()) { content() }
    }
}

/**
 * A settings group that folds.
 *
 * Its header IS the card's first row, so the label, the surface, the ripple, the
 * state layers and the group's uneven corners are the list item the rows under it
 * are made of: 24/24/6/6 for a header with a body under it, 6 dp where two rows
 * meet, 24/24 at the bottom, and 24 all round when it is the only row, which is
 * the folded state. The chevron turns to say which state the group is in and is
 * that row's own trailing affordance, never a second control.
 *
 * Device sync's Advanced options are the one group that folds, the shape the
 * settings preview gives them: the two volume channels are set once and then left
 * alone, and as two loose rows between the connection method and the relay field
 * they read as a part of the connection flow rather than as options of their own.
 *
 * The body folds through [Animations.sectionEnter] / [Animations.sectionExit], so
 * the master Animations switch reaches it, and the chevron snaps instead of
 * turning while that switch is off.
 */
@Composable
fun M3SettingsCollapsibleGroup(
    title: String,
    expanded: Boolean,
    onExpandedChange: (Boolean) -> Unit,
    items: List<M3SettingsItem>,
    itemMinHeight: Dp? = null,
) {
    // The card is drawn row by row instead of through [M3SettingsGroup]: the
    // header is one of those rows, and its own corners depend on whether the body
    // is there (see the note above).
    val rowCount = if (expanded) items.size + 1 else 1
    Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(3.dp)) {
        CollapsibleGroupRow(shape = collapsibleGroupShape(0, rowCount)) {
            CollapsibleHeader(title = title, expanded = expanded, onExpandedChange = onExpandedChange)
        }
        AnimatedVisibility(
            visible = expanded,
            enter = Animations.sectionEnter(),
            exit = Animations.sectionExit(),
        ) {
            // One Column: AnimatedVisibility lays multiple children out ON TOP of
            // each other, so the body has to be a single child (the same reason
            // the sidebar's Library section is one).
            Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
                items.forEachIndexed { index, item ->
                    CollapsibleGroupRow(shape = collapsibleGroupShape(index + 1, rowCount)) {
                        M3SettingsItemRow(item = item, minHeight = itemMinHeight)
                    }
                }
            }
        }
    }
}

/**
 * A card that folds around arbitrary content instead of a list of rows.
 *
 * [M3SettingsCollapsibleGroup] folds rows; the two sign-in screens fold a small
 * form (two cookie fields and their button), which is not a setting and cannot be
 * expressed as one. The header is the same row, with the same corners and the
 * same turning chevron, and the body is one card joined to it by the 3 dp seam
 * every group uses, so a folded form reads as part of the settings family rather
 * than as a second, differently shaped disclosure.
 */
@Composable
fun M3CollapsibleCard(
    title: String,
    expanded: Boolean,
    onExpandedChange: (Boolean) -> Unit,
    content: @Composable ColumnScope.() -> Unit,
) {
    Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(3.dp)) {
        CollapsibleGroupRow(shape = collapsibleGroupShape(0, if (expanded) 2 else 1)) {
            CollapsibleHeader(title = title, expanded = expanded, onExpandedChange = onExpandedChange)
        }
        AnimatedVisibility(
            visible = expanded,
            enter = Animations.sectionEnter(),
            exit = Animations.sectionExit(),
        ) {
            // A single child: AnimatedVisibility lays several children out on top
            // of each other (see the note in [M3SettingsCollapsibleGroup]).
            CollapsibleGroupRow(shape = collapsibleGroupShape(1, 2)) {
                Column(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 17.dp, vertical = 14.dp),
                    content = content,
                )
            }
        }
    }
}

/**
 * The header both folding cards share: the card's first row, its label in
 * `primary`, and the chevron that says which state the card is in.
 */
@Composable
private fun CollapsibleHeader(
    title: String,
    expanded: Boolean,
    onExpandedChange: (Boolean) -> Unit,
) {
    Tooltip(title) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                // The state layer is tinted with `primary` like the label:
                // a neutral tint under a red label goes muddy, and this row
                // is the card's title as much as it is its control.
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = ripple(color = MaterialTheme.colorScheme.primary),
                    role = Role.Button,
                    onClick = { onExpandedChange(!expanded) },
                )
                // A row of the card, so it keeps the list row's height and
                // its label never hugs the corners: the same 56 dp the
                // preview's `.md-row` holds, over the 17/14 dp padding the
                // rows under it use.
                .heightIn(min = 56.dp)
                .padding(horizontal = 17.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.weight(1f),
            )
            val angle by animateFloatAsState(
                targetValue = if (expanded) 180f else 0f,
                animationSpec = if (Animations.enabled) {
                    tween(Animations.ms(200), easing = FastOutSlowInEasing)
                } else {
                    snap()
                },
                label = "collapsibleGroupChevron",
            )
            // The 24 dp slot the preview gives the chevron, so its glyph is
            // centred on the row's rhythm instead of hanging on the padding.
            Box(Modifier.size(24.dp), contentAlignment = Alignment.Center) {
                Icon(
                    imageVector = Icons.Filled.ExpandMore,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(20.dp).rotate(angle),
                )
            }
        }
    }
}

/**
 * One row of a folding group: the card [M3SettingsGroup] gives each of its rows
 * (the group's own `surfaceVariant` at 30 %, no elevation), with the corner its
 * position in the group calls for - 24 at the two ends of the card, 6 where two
 * rows meet.
 */
@Composable
private fun CollapsibleGroupRow(shape: RoundedCornerShape, content: @Composable () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = shape,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
    ) {
        content()
    }
}

/** The uneven corners of the group holding [size] rows, for the row at [index]. */
private fun collapsibleGroupShape(index: Int, size: Int): RoundedCornerShape = when {
    size == 1 -> RoundedCornerShape(24.dp)
    index == 0 -> RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp, bottomStart = 6.dp, bottomEnd = 6.dp)
    index == size - 1 -> RoundedCornerShape(topStart = 6.dp, topEnd = 6.dp, bottomStart = 24.dp, bottomEnd = 24.dp)
    else -> RoundedCornerShape(6.dp)
}

@Composable
internal fun SettingsChevron() {
    Icon(
        imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
        contentDescription = null,
        tint = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.size(20.dp),
    )
}

@Composable
private fun M3SettingsItemRow(
    item: M3SettingsItem,
    minHeight: Dp? = null,
) {
    val dropdown = item.dropdown
    var expanded by remember(dropdown) { mutableStateOf(false) }
    // The menu hangs off the row's trailing control, not off its left edge: the
    // row is the width of the page, so a menu aligned to the row's start opened
    // half a screen away from the value that was pressed - on a two-card screen
    // like Content it read as "the menu opens on the left" (the second card's
    // menu started at the window's left edge). The anchor is therefore the
    // trailing slot (value + arrow) and the menu is right-aligned to it, which is
    // what Material's own exposed menus do. Both edges are read in window
    // coordinates so a row inside a scroll column measures the same way.
    val density = LocalDensity.current
    var rowLeftPx by remember { mutableStateOf(0f) }
    var trailingRightPx by remember { mutableStateOf(0f) }
    val menuOffsetX = if (dropdown != null && rowLeftPx != 0f && trailingRightPx != 0f) {
        with(density) { (trailingRightPx - rowLeftPx).toDp() - dropdown.menuWidth }
    } else {
        0.dp
    }
    Box(
        Modifier
            .fillMaxWidth()
            .onGloballyPositioned { rowLeftPx = it.boundsInWindow().left },
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable(
                    enabled = item.enabled && (dropdown != null || item.onClick != null),
                    onClick = {
                        if (dropdown != null) expanded = true else item.onClick?.invoke()
                    },
                )
                .let {
                    if (minHeight != null) it.heightIn(min = minHeight) else it
                }
                .padding(horizontal = 17.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            if (item.leading != null) {
                item.leading()
                Spacer(Modifier.width(13.dp))
            } else if (item.icon != null) {
                Box(
                    modifier = Modifier
                        .size(34.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(
                            MaterialTheme.colorScheme.primary.copy(alpha = if (item.enabled) 0.1f else 0.05f),
                        ),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        imageVector = item.icon,
                        contentDescription = null,
                        tint = if (!item.enabled) {
                            MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f)
                        } else {
                            MaterialTheme.colorScheme.primary.copy(alpha = 0.9f)
                        },
                        modifier = Modifier.size(20.dp),
                    )
                }
                Spacer(Modifier.width(13.dp))
            }

            Column(Modifier.weight(1f)) {
                ProvideTextStyle(
                    MaterialTheme.typography.titleMedium.copy(
                        color = if (!item.enabled) {
                            MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f)
                        } else {
                            MaterialTheme.colorScheme.onSurface
                        },
                    ),
                ) {
                    item.title()
                }
                item.description?.let { desc ->
                    Spacer(Modifier.height(2.dp))
                    ProvideTextStyle(
                        MaterialTheme.typography.bodyMedium.copy(
                            color = if (!item.enabled) {
                                MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f)
                            } else {
                                MaterialTheme.colorScheme.onSurfaceVariant
                            },
                        ),
                    ) {
                        desc()
                    }
                }
                item.slider?.let { control ->
                    Spacer(Modifier.height(4.dp))
                    control()
                }
            }

            if (item.trailing != null) {
                Spacer(Modifier.width(7.dp))
                Box(Modifier.onGloballyPositioned { trailingRightPx = it.boundsInWindow().right }) {
                    item.trailing()
                }
            } else if (dropdown != null) {
                Spacer(Modifier.width(7.dp))
                Box(Modifier.onGloballyPositioned { trailingRightPx = it.boundsInWindow().right }) {
                    M3DropdownTrailing(dropdown.value)
                }
            }
        }
        if (dropdown != null) {
            M3DropdownMenu(
                expanded = expanded,
                onDismissRequest = { expanded = false },
                value = dropdown.value,
                options = dropdown.options,
                onSelect = dropdown.onSelect,
                menuWidth = dropdown.menuWidth,
                offsetX = menuOffsetX,
            )
        }
    }
}

/** The value a dropdown row shows: the current choice, then the menu arrow. */
@Composable
private fun M3DropdownTrailing(value: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text(
            text = value,
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.primary,
        )
        Icon(
            Icons.Filled.ArrowDropDown,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

/**
 * A settings row that opens an inline dropdown anchored to the row itself
 * (click the row → the menu appears under the row, not at the window corner).
 * Used for single-choice options that on mobile are sub-screens or dialogs.
 *
 * Note that it is NOT one of the `items` of [M3SettingsGroup]: it is a
 * composable, not an item value, so it is placed between groups (see
 * `AppearanceScreens`). A screen that wants the choice in the group's own card
 * uses [M3SettingsDropdownCard] instead.
 */
@Composable
fun M3SettingsDropdownItem(
    icon: ImageVector?,
    title: String,
    value: String,
    options: List<Pair<String, String>>,
    onSelect: (String) -> Unit,
    modifier: Modifier = Modifier,
    description: String? = null,
    menuWidth: Dp = 280.dp,
) {
    Box(modifier = modifier.fillMaxWidth()) {
        M3SettingsDropdownRow(
            icon = icon,
            title = title,
            value = value,
            options = options,
            onSelect = onSelect,
            description = description,
            menuWidth = menuWidth,
        )
    }
}

/**
 * The same dropdown choice, in the shape of the group cards: one rounded card
 * holding the row. The Content screen offers two choices side by side (the
 * language and the region of the catalogue), and two buttons under two loose
 * headings used to be how that read; as cards they sit in the same visual system
 * as every other setting while keeping the inline dropdown (which is what the
 * mobile screen's dialogs do in a different shape).
 */
@Composable
fun M3SettingsDropdownCard(
    icon: ImageVector?,
    title: String,
    value: String,
    options: List<Pair<String, String>>,
    onSelect: (String) -> Unit,
    modifier: Modifier = Modifier,
    description: String? = null,
    menuWidth: Dp = 320.dp,
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
    ) {
        M3SettingsDropdownRow(
            icon = icon,
            title = title,
            value = value,
            options = options,
            onSelect = onSelect,
            description = description,
            menuWidth = menuWidth,
        )
    }
}

/**
 * The row plus its inline menu, shared by the two entry points above.
 *
 * The menu is the item's own ([M3SettingsItem.dropdown]), so a choice in a card
 * opens exactly where a choice in a group does: under the row, right-aligned to
 * the value that was pressed. It used to hang off this row's own full-width box,
 * which put the menu at the window's left edge whatever row had been pressed.
 */
@Composable
private fun M3SettingsDropdownRow(
    icon: ImageVector?,
    title: String,
    value: String,
    options: List<Pair<String, String>>,
    onSelect: (String) -> Unit,
    description: String?,
    menuWidth: Dp,
) {
    M3SettingsItemRow(
        item = M3SettingsItem(
            icon = icon,
            title = { Text(title) },
            description = { Text(description ?: value) },
            dropdown = M3Dropdown(
                value = value,
                options = options,
                onSelect = onSelect,
                menuWidth = menuWidth,
            ),
        ),
    )
}

/** The menu a dropdown row opens: one option per line, the current one marked. */
@Composable
private fun M3DropdownMenu(
    expanded: Boolean,
    onDismissRequest: () -> Unit,
    value: String,
    options: List<Pair<String, String>>,
    onSelect: (String) -> Unit,
    menuWidth: Dp,
    offsetX: Dp = 0.dp,
) {
    DropdownMenu(
        expanded = expanded,
        onDismissRequest = onDismissRequest,
        offset = DpOffset(offsetX, 0.dp),
        modifier = Modifier
            .width(menuWidth)
            .clip(RoundedCornerShape(18.dp))
            .background(MaterialTheme.colorScheme.surfaceContainerHigh)
            .border(1.dp, MaterialTheme.colorScheme.onSurface.copy(alpha = 0.12f), RoundedCornerShape(18.dp))
            .padding(vertical = 4.dp),
    ) {
        // Highlight the option whose translated label matches the currently
        // shown value. (Comparing `key == value` never matched: callers pass
        // the *translated* value, so the checked row was never highlighted.)
        options.forEach { (key, label) ->
            val selected = label == value
            DropdownMenuItem(
                text = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = label,
                            color = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
                            style = MaterialTheme.typography.bodyMedium.copy(
                                fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
                            ),
                            modifier = Modifier.weight(1f),
                        )
                        if (selected) {
                            Icon(
                                Icons.Filled.Check,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(16.dp),
                            )
                        }
                    }
                },
                onClick = {
                    onDismissRequest()
                    onSelect(key)
                },
            )
        }
    }
}
