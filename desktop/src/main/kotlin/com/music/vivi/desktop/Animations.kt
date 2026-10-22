package com.music.vivi.desktop

import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.Easing
import androidx.compose.animation.core.FiniteAnimationSpec
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandHorizontally
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkHorizontally
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import kotlin.math.roundToInt

/**
 * Global UI animation-speed control ("Animation speed": Fast / Normal / Slow).
 *
 * The value is cached in memory ([speed]) so scaling an animation duration never
 * touches the settings file: it is loaded once at startup and refreshed by the
 * settings screen when the user changes it. [ms] scales a base duration, [f] a
 * tween/spring fraction, so any transition can opt in.
 *
 * [enabled] mirrors the master "Animations" switch: when it is off, the panel
 * and player transitions below become instant instead of being merely faster.
 */
object Animations {

    /** Current speed name: "fast" / "normal" / "slow". */
    @Volatile
    var speed: String = "normal"

    /** Master switch; when off every transition below is instant. */
    @Volatile
    var enabled: Boolean = true

    /** Reloads the persisted values (startup + on change). */
    fun load() {
        val settings = DesktopSettings.load()
        speed = settings.animationSpeed
        enabled = settings.animationsEnabled
    }

    /** Multiplier applied to every base duration. */
    private val multiplier: Float
        get() = when (speed) {
            "fast" -> 0.6f
            "slow" -> 1.6f
            else -> 1f
        }

    /** Scales a base duration in milliseconds (never below 1 ms). */
    fun ms(base: Int): Int = (base * multiplier).roundToInt().coerceAtLeast(1)

    /** Scales a base duration in milliseconds as a Long. */
    fun ms(base: Long): Long = ms(base.toInt()).toLong()

    /**
     * Material 3 Expressive "emphasized decelerate": the curve the expressive
     * motion system uses for anything arriving on screen. It leaves the start
     * quickly and settles slowly, which is what makes a panel feel like it
     * belongs to the finger that pulled it. Plain Material 3 used
     * `FastOutSlowInEasing` here, whose slow start is the part the expressive
     * style replaced.
     */
    val emphasizedDecelerate: Easing = CubicBezierEasing(0.2f, 0f, 0f, 1f)

    /**
     * Material-expressive spring for the panels: a light overshoot on the way
     * in and no bounce on the way out. [damping] < 1 makes it expressive.
     */
    fun <T> expressiveSpring(damping: Float = 0.75f, stiffness: Float = 380f, threshold: T? = null): FiniteAnimationSpec<T> =
        spring(
            dampingRatio = damping,
            stiffness = stiffness,
            visibilityThreshold = threshold,
        )

    // ------------------------------------------------------------------
    // Shared panel transitions (sidebar, right panel, bottom bar, player)
    // ------------------------------------------------------------------
    // Before this, each surface animated with its own default spec — the
    // sidebar used a bare `expandHorizontally()`, the right Now Playing panel
    // appeared and disappeared with no transition at all, and none of them
    // followed either the master switch or the speed setting. They all go
    // through here now, so "Animations" and "Animation speed" reach them
    // together.

    /** Expands a panel sideways (sidebar, right panel). */
    fun panelEnter(): EnterTransition =
        if (!enabled) EnterTransition.None
        else expandHorizontally(
            animationSpec = expressiveSpring(threshold = IntSize(1, 0)),
            expandFrom = androidx.compose.ui.Alignment.Start,
        ) + fadeIn(animationSpec = tween(ms(180)))

    /** Collapses a panel sideways. */
    fun panelExit(): ExitTransition =
        if (!enabled) ExitTransition.None
        else shrinkHorizontally(
            animationSpec = tween(ms(180), easing = emphasizedDecelerate),
            shrinkTowards = androidx.compose.ui.Alignment.Start,
        ) + fadeOut(animationSpec = tween(ms(140)))

    /** Raises a surface from the bottom (the player expanding out of the bar). */
    fun playerEnter(): EnterTransition =
        if (!enabled) EnterTransition.None
        else slideInVertically(
            animationSpec = expressiveSpring(threshold = IntOffset(0, 24)),
            initialOffsetY = { it / 8 },
        ) + fadeIn(animationSpec = tween(ms(200))) + expandVertically(
            animationSpec = expressiveSpring(threshold = IntSize(0, 12)),
            expandFrom = androidx.compose.ui.Alignment.Bottom,
        )

    /** Drops a surface back to the bottom. */
    fun playerExit(): ExitTransition =
        if (!enabled) ExitTransition.None
        else slideOutVertically(
            animationSpec = tween(ms(180), easing = emphasizedDecelerate),
            targetOffsetY = { it / 8 },
        ) + fadeOut(animationSpec = tween(ms(160))) + shrinkVertically(
            animationSpec = tween(ms(180), easing = emphasizedDecelerate),
            shrinkTowards = androidx.compose.ui.Alignment.Bottom,
        )

    /** Raises the bottom bar (mini player) into place. */
    fun barEnter(): EnterTransition = playerEnter()

    /** Lowers the bottom bar out of view. */
    fun barExit(): ExitTransition = playerExit()

    /** Expands a section that grows downwards. */
    fun sectionEnter(): EnterTransition =
        if (!enabled) EnterTransition.None
        else expandVertically(
            animationSpec = expressiveSpring(threshold = IntSize(0, 1)),
            expandFrom = androidx.compose.ui.Alignment.Top,
        ) + fadeIn(animationSpec = tween(ms(160)))

    /** Collapses a section that grows downwards. */
    fun sectionExit(): ExitTransition =
        if (!enabled) ExitTransition.None
        else shrinkVertically(
            animationSpec = tween(ms(150), easing = emphasizedDecelerate),
            shrinkTowards = androidx.compose.ui.Alignment.Top,
        ) + fadeOut(animationSpec = tween(ms(120)))

    /**
     * A settings row that belongs to a switch, opening and closing inside its card.
     *
     * [sectionEnter]/[sectionExit] ride a spring, which suits a panel that appears
     * once; a row that folds under the user's finger is measured against the row
     * under it, and a spring that settles on its own clock after the height looks
     * finished makes the card's bottom edge creep for another 250 ms. These two are
     * one tween each, both [rowMs] long and on the same curve as the fade, so the
     * height and the opacity land together: the row is never a half-height strip
     * already painted at full strength. [rowMs] is also what the card's bottom
     * corners animate over, so the two movements are one.
     */
    /**
     * The two row transitions, built once per value of [enabled].
     *
     * `AnimatedVisibility` is given a transition on every recomposition, and a NEW
     * spec instance mid-flight restarts the animation: a row on its way out would
     * run its exit, be handed a fresh spec the next frame and start over, which is
     * the "it closes, comes back and closes again" stutter. These are the same
     * objects for as long as the master switch does not move, so the transition
     * keeps the instance it already animates with.
     */
    private var rowTransitionsFor: Boolean? = null
    private var rowEnterTransition: EnterTransition = EnterTransition.None
    private var rowExitTransition: ExitTransition = ExitTransition.None

    fun rowEnter(): EnterTransition {
        buildRowTransitions()
        return rowEnterTransition
    }

    /** A settings row folding away: the same 200 ms as [rowEnter], in reverse. */
    fun rowExit(): ExitTransition {
        buildRowTransitions()
        return rowExitTransition
    }

    private fun buildRowTransitions() {
        if (rowTransitionsFor == enabled) return
        rowTransitionsFor = enabled
        rowEnterTransition = if (!enabled) {
            EnterTransition.None
        } else {
            expandVertically(
                animationSpec = tween(rowMs(), easing = emphasizedDecelerate),
                expandFrom = androidx.compose.ui.Alignment.Top,
            ) + fadeIn(animationSpec = tween(rowMs(), easing = emphasizedDecelerate))
        }
        rowExitTransition = if (!enabled) {
            ExitTransition.None
        } else {
            shrinkVertically(
                animationSpec = tween(rowMs(), easing = emphasizedDecelerate),
                shrinkTowards = androidx.compose.ui.Alignment.Top,
            ) + fadeOut(animationSpec = tween(rowMs(), easing = emphasizedDecelerate))
        }
    }

    /** How long a settings row takes to fold (0 with the master switch off). */
    fun rowMs(): Int = if (enabled) ms(200) else 0

    /**
     * The sign-in window button taking its leave.
     *
     * The two sign-in screens keep one action on offer at a time: while the
     * manual-cookie card is open, its own submit button is the action and the
     * window button is gone. That hand-over is one movement of two halves, and
     * both halves are driven by the same state change, so Compose starts them in
     * the same frame: this one lifts the button out of the card (upwards, the
     * direction it came from) and takes its row with it (the shrink), while the
     * card below unfolds downward through [sectionEnter].
     *
     * The shrink is what keeps the geometry honest: the card above must lose
     * exactly the height of the action, not keep an empty gap where the button
     * used to be.
     */
    fun signInActionExit(): ExitTransition =
        if (!enabled) ExitTransition.None
        else slideOutVertically(
            animationSpec = tween(ms(180), easing = emphasizedDecelerate),
            targetOffsetY = { -it / 3 },
        ) + fadeOut(animationSpec = tween(ms(140))) + shrinkVertically(
            animationSpec = tween(ms(200), easing = emphasizedDecelerate),
            shrinkTowards = androidx.compose.ui.Alignment.Top,
        )

    /** The window button coming back down as the manual-cookie card folds away. */
    fun signInActionEnter(): EnterTransition =
        if (!enabled) EnterTransition.None
        else slideInVertically(
            animationSpec = tween(ms(200), easing = emphasizedDecelerate),
            initialOffsetY = { -it / 3 },
        ) + fadeIn(animationSpec = tween(ms(160))) + expandVertically(
            animationSpec = tween(ms(200), easing = emphasizedDecelerate),
            expandFrom = androidx.compose.ui.Alignment.Top,
        )

    /** Duration for an `animateDpAsState`/`animateFloatAsState` panel size (0 = instant). */
    fun sizeMs(): Int = if (enabled) ms(220) else 0

    /** True when a transition can be skipped entirely (master switch off). */
    val instant: Boolean get() = !enabled
}
