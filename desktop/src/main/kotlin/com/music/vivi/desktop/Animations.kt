package com.music.vivi.desktop

import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.core.Easing
import androidx.compose.animation.core.FiniteAnimationSpec
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
 * The app's transitions, built out of the Material 3 motion tokens in [Motion].
 *
 * Two settings reach all of them. "Animation speed" (Fast / Normal / Slow) is a
 * multiplier on every duration: the value is cached in memory ([speed]) so
 * scaling a duration never touches the settings file, loaded once at startup
 * and refreshed by the settings screen. "Animations" is the master switch
 * ([enabled]): off makes every transition below instant instead of merely
 * faster.
 *
 * The durations here are token steps ([Motion.SHORT3], [Motion.SHORT4],
 * [Motion.MEDIUM1], ...), never a number of its own, and the direction of a
 * movement picks the curve: something arriving decelerates into place
 * ([Motion.emphasizedDecelerate]), something leaving accelerates out of it
 * ([Motion.emphasizedAccelerate]). A movement that does both - a size that
 * changes under the pointer - stays on the emphasized curve, which begins and
 * ends on screen.
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
     * The emphasized-decelerate token: what a surface arriving on screen uses.
     *
     * This used to be `CubicBezierEasing(0.2, 0, 0, 1)`, which is the plain
     * `emphasized` curve; the decelerating token is a different curve, and it is
     * the one that leaves quickly and settles slowly - the part that makes a
     * panel feel like it belongs to the pointer that pulled it.
     */
    val emphasizedDecelerate: Easing = Motion.emphasizedDecelerate

    /** The curve of something leaving the screen. */
    val emphasizedAccelerate: Easing = Motion.emphasizedAccelerate

    /**
     * Material-expressive spring for the panels.
     *
     * It is [Motion.spatialDefault] with the caller's own threshold, kept as a
     * named entry point because the panels pass visibility thresholds of their
     * own (`IntSize(1, 0)`, `IntOffset(0, 24)`): a threshold is what tells the
     * animation when it has arrived, so a panel does not keep springing for a
     * pixel it cannot show.
     */
    fun <T> expressiveSpring(threshold: T? = null): FiniteAnimationSpec<T> =
        Motion.spatialDefault(threshold)

    // ------------------------------------------------------------------
    // Shared panel transitions (sidebar, right panel, bottom bar, player)
    // ------------------------------------------------------------------
    // Before this, each surface animated with its own default spec - the
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
        ) + fadeIn(animationSpec = tween(ms(Motion.SHORT4), easing = Motion.standardDecelerate))

    /** Collapses a panel sideways. */
    fun panelExit(): ExitTransition =
        if (!enabled) ExitTransition.None
        else shrinkHorizontally(
            animationSpec = tween(ms(Motion.SHORT3), easing = emphasizedAccelerate),
            shrinkTowards = androidx.compose.ui.Alignment.Start,
        ) + fadeOut(animationSpec = tween(ms(Motion.SHORT2), easing = Motion.standardAccelerate))

    /** Raises a surface from the bottom (the player expanding out of the bar). */
    fun playerEnter(): EnterTransition =
        if (!enabled) EnterTransition.None
        else slideInVertically(
            animationSpec = expressiveSpring(threshold = IntOffset(0, 24)),
            initialOffsetY = { it / 8 },
        ) + fadeIn(animationSpec = tween(ms(Motion.SHORT4), easing = Motion.standardDecelerate)) + expandVertically(
            animationSpec = expressiveSpring(threshold = IntSize(0, 12)),
            expandFrom = androidx.compose.ui.Alignment.Bottom,
        )

    /** Drops a surface back to the bottom. */
    fun playerExit(): ExitTransition =
        if (!enabled) ExitTransition.None
        else slideOutVertically(
            animationSpec = tween(ms(Motion.SHORT4), easing = emphasizedAccelerate),
            targetOffsetY = { it / 8 },
        ) + fadeOut(animationSpec = tween(ms(Motion.SHORT3))) + shrinkVertically(
            animationSpec = tween(ms(Motion.SHORT4), easing = emphasizedAccelerate),
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
        ) + fadeIn(animationSpec = tween(ms(Motion.SHORT3), easing = Motion.standardDecelerate))

    /** Collapses a section that grows downwards. */
    fun sectionExit(): ExitTransition =
        if (!enabled) ExitTransition.None
        else shrinkVertically(
            animationSpec = tween(ms(Motion.SHORT3), easing = emphasizedAccelerate),
            shrinkTowards = androidx.compose.ui.Alignment.Top,
        ) + fadeOut(animationSpec = tween(ms(Motion.SHORT2)))

    /**
     * A settings row that belongs to a switch, opening and closing inside its card.
     *
     * [sectionEnter]/[sectionExit] ride a spring, which suits a panel that appears
     * once; a row that folds under the user's finger is measured against the row
     * under it, and a spring that settles on its own clock after the height looks
     * finished makes the card's bottom edge creep for another 250 ms. These two are
     * one tween each, both [rowMs] long, so the height and the opacity land
     * together: the row is never a half-height strip already painted at full
     * strength. [rowMs] is also what the card's bottom corners animate over, so
     * the two movements are one.
     *
     * The two directions keep the token rule: the row unfolds on the
     * decelerating curve and folds on the accelerating one. Same duration
     * either way, which is what keeps the reverse a reverse.
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

    /** A settings row folding away: the same [rowMs] as [rowEnter], in reverse. */
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
                animationSpec = tween(rowMs(), easing = emphasizedAccelerate),
                shrinkTowards = androidx.compose.ui.Alignment.Top,
            ) + fadeOut(animationSpec = tween(rowMs(), easing = emphasizedAccelerate))
        }
    }

    /** How long a settings row takes to fold (0 with the master switch off). */
    fun rowMs(): Int = if (enabled) ms(Motion.SHORT4) else 0

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
            animationSpec = tween(ms(Motion.SHORT4), easing = emphasizedAccelerate),
            targetOffsetY = { -it / 3 },
        ) + fadeOut(animationSpec = tween(ms(Motion.SHORT3))) + shrinkVertically(
            animationSpec = tween(ms(Motion.SHORT4), easing = emphasizedAccelerate),
            shrinkTowards = androidx.compose.ui.Alignment.Top,
        )

    /** The window button coming back down as the manual-cookie card folds away. */
    fun signInActionEnter(): EnterTransition =
        if (!enabled) EnterTransition.None
        else slideInVertically(
            animationSpec = tween(ms(Motion.SHORT4), easing = emphasizedDecelerate),
            initialOffsetY = { -it / 3 },
        ) + fadeIn(animationSpec = tween(ms(Motion.SHORT3))) + expandVertically(
            animationSpec = tween(ms(Motion.SHORT4), easing = emphasizedDecelerate),
            expandFrom = androidx.compose.ui.Alignment.Top,
        )

    /** Duration for an `animateDpAsState`/`animateFloatAsState` panel size (0 = instant). */
    fun sizeMs(): Int = if (enabled) ms(Motion.SHORT4) else 0

    /** True when a transition can be skipped entirely (master switch off). */
    val instant: Boolean get() = !enabled
}
