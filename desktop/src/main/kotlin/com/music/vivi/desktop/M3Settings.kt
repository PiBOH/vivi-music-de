package com.music.vivi.desktop

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.snap
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsHoveredAsState
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
import androidx.compose.foundation.shape.CornerBasedShape
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
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.compositionLocalOf
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
import androidx.compose.ui.graphics.graphicsLayer
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
    /**
     * The option the menu marks as chosen, when the row names the value in a
     * shorter form than the menu does: the AI model row prints the last segment
     * of an id (`gemini-2.5-flash`) and the menu lists (and marks) the whole one
     * (`google/gemini-2.5-flash`). Defaults to [value], which is the usual case.
     */
    val selectedKey: String? = null,
)

/**
 * True inside a screen a navigation has just put on show, false for a screen
 * composed on its own - a check's frame approaches a screen that way.
 *
 * The preview gives a group its entrance only under `.screen--enter`, a marker
 * its router writes on a route change and a control that changes the page never
 * keeps. This is that marker: the screen host provides it, a group reads it once
 * when it is first composed, and anything that draws a screen outside the host
 * gets no entrance at all.
 */
val LocalScreenArrival = compositionLocalOf { false }

/**
 * A settings group fades up as its screen arrives, which is the preview's
 * `.screen--enter .md-group` rule: 8 dp of travel over the medium1 token on the
 * decelerating curve. It runs once per composition, so a switch that changes a
 * row inside the group does not replay it, and it is nothing at all with the
 * Animations switch off or outside a navigation.
 */
@Composable
internal fun Modifier.groupEnter(): Modifier {
    if (!Animations.enabled || !LocalScreenArrival.current) return this
    val progress = remember { Animatable(0f) }
    LaunchedEffect(Unit) {
        progress.animateTo(
            targetValue = 1f,
            animationSpec = tween(
                durationMillis = Animations.ms(Motion.MEDIUM1),
                easing = Animations.emphasizedDecelerate,
            ),
        )
    }
    return graphicsLayer {
        alpha = progress.value
        translationY = (1f - progress.value) * 8.dp.toPx()
    }
}

@Composable
fun M3SettingsGroup(
    title: String? = null,
    items: List<M3SettingsItem>,
    itemMinHeight: Dp? = null,
) {
    Column(Modifier.fillMaxWidth().groupEnter()) {
        title?.let {
            Text(
                text = it,
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.primary,
                // 6 dp top and bottom, 4 dp in from the card's edge: the
                // preview's `.md-group__title` padding, so the label sits just
                // inside the card's left edge instead of flush with it.
                modifier = Modifier.padding(start = 4.dp, end = 4.dp, top = 6.dp, bottom = 6.dp),
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
    // The fold exists only with the Animations switch on. `rowEnter()` returns
    // `EnterTransition.None` when it is off, which is the same frame in the end,
    // but it depends on a transition table cached by a plain flag: branching here
    // makes the rule the screen's own, so a block that belongs to a switch (the
    // performance reader's panels, the developer options' group) is added and
    // dropped in one frame when animations are off, whatever the table holds.
    if (!Animations.enabled) {
        if (visible) Column(Modifier.fillMaxWidth()) { content() }
        return
    }
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
private fun CollapsibleGroupRow(shape: CornerBasedShape, content: @Composable () -> Unit) {
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
@Composable
private fun collapsibleGroupShape(index: Int, size: Int): CornerBasedShape = when {
    size == 1 -> MaterialTheme.shapes.extraLarge
    index == 0 -> RoundedCornerShape(
        topStart = ShapeRadii.extraLarge,
        topEnd = ShapeRadii.extraLarge,
        bottomStart = ShapeRadii.extraSmall,
        bottomEnd = ShapeRadii.extraSmall,
    )
    index == size - 1 -> RoundedCornerShape(
        topStart = ShapeRadii.extraSmall,
        topEnd = ShapeRadii.extraSmall,
        bottomStart = ShapeRadii.extraLarge,
        bottomEnd = ShapeRadii.extraLarge,
    )
    else -> MaterialTheme.shapes.extraSmall
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
    // The pointer's acknowledgement: M3's 8 % hover state layer, faded in and out
    // on the effects spring. The row is the one surface a settings screen repeats
    // dozens of times, so the layer is what tells the pointer it is on a control
    // before the click; `LocalRowHovered` carries the same state to what the row
    // draws (a person's cookie grows, a cover lifts a little).
    val interaction = remember { MutableInteractionSource() }
    val hovered by interaction.collectIsHoveredAsState()
    val interactive = item.enabled && (dropdown != null || item.onClick != null)
    val hover = Motion.hoverProgress(hovered, enabled = interactive, label = "settingsRowHover")

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
        CompositionLocalProvider(LocalRowHovered provides (hovered && interactive)) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable(
                        enabled = interactive,
                        interactionSource = interaction,
                        indication = ripple(),
                        onClick = {
                            if (dropdown != null) expanded = true else item.onClick?.invoke()
                        },
                    )
                    .background(MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f * hover))
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
                            .clip(MaterialTheme.shapes.small)
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
        }
        if (dropdown != null) {
            M3DropdownMenu(
                expanded = expanded,
                onDismissRequest = { expanded = false },
                value = dropdown.value,
                selectedKey = dropdown.selectedKey,
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
    // A settings choice is a block of the page like any other: it used to be a
    // bare row on the page's own background, so on every screen that offers a
    // choice the row floated without the card its neighbours wear - the one thing
    // a block of a single element could be missing. It is the group card's fill
    // and corner now, the same [M3SettingsDropdownCard] draws.
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.extraLarge,
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
        shape = MaterialTheme.shapes.extraLarge,
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
            // The value is the trailing chip, once. It used to be drawn under the
            // title as well when the caller passed no description of its own, so
            // every choice without a sentence under it said its answer twice and
            // stood a line taller than the preview's row (`.md-row__desc` is drawn
            // only when the node carries a description).
            description = description?.let { text -> { Text(text) } },
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
    selectedKey: String? = null,
) {
    // The row may name the value shorter than the menu does (an id's last
    // segment), so the mark follows `selectedKey` when the caller gives one.
    val marked = selectedKey ?: value
    DropdownMenu(
        expanded = expanded,
        onDismissRequest = onDismissRequest,
        offset = DpOffset(offsetX, 0.dp),
        modifier = Modifier
            .width(menuWidth)
            .clip(MaterialTheme.shapes.large)
            .background(MaterialTheme.colorScheme.surfaceContainerHigh)
            .border(1.dp, MaterialTheme.colorScheme.onSurface.copy(alpha = 0.12f), MaterialTheme.shapes.large)
            .padding(vertical = 4.dp),
    ) {
        // Highlight the option whose translated label matches the currently
        // shown value. (Comparing `key == value` never matched: callers pass
        // the *translated* value, so the checked row was never highlighted.)
        options.forEach { (key, label) ->
            val selected = label == marked
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
