package com.music.vivi.desktop

import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.Easing
import androidx.compose.animation.core.FiniteAnimationSpec
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.hoverable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsHoveredAsState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer

/**
 * The Material 3 motion token table, and the one hover movement the DE shares.
 *
 * Every transition in the app is built from this object: a duration is one of
 * the published `short`/`medium`/`long`/`extra-long` steps, a curve is one of
 * the published easing tokens, and a movement that has to feel physical is one
 * of the expressive springs. [Animations] keeps the app's own API (the
 * Animation-speed setting still scales every duration, the master switch still
 * turns motion off) but no longer carries numbers of its own, so the whole
 * program moves on one table.
 *
 * The durations and the cubic-beziers are the published values
 * (<https://m3.material.io/styles/motion/easing-and-duration/tokens-specs>).
 * The springs are not: Material publishes spring tokens as
 * `md.sys.motion.spring.<fast|default|slow>.<spatial|effects>` and the damping
 * and stiffness behind them are not in that table, so the pairs here are the
 * DE's own, chosen for the two things the tokens say: a spatial movement
 * settles with a trace of overshoot (damping 0.8), an effect such as a fade or
 * a state layer settles without any (damping 1), and the three speeds sit in
 * the order fast < default < slow. They are not presented as Material's numbers.
 *
 * The one thing every interactive surface shares is [Modifier.hoverFeedback]:
 * a hover fades in and out on the effects spring, which is the shortest of the
 * three, so the pointer is acknowledged at once and a sweep across a list does
 * not leave a trail of half-finished animations behind it.
 */
object Motion {

    // --- Duration tokens, in milliseconds ------------------------------------

    const val SHORT1 = 50
    const val SHORT2 = 100
    const val SHORT3 = 150
    const val SHORT4 = 200
    const val MEDIUM1 = 250
    const val MEDIUM2 = 300
    const val MEDIUM3 = 350
    const val MEDIUM4 = 400
    const val LONG1 = 450
    const val LONG2 = 500
    const val LONG3 = 550
    const val LONG4 = 600
    const val EXTRA_LONG1 = 700
    const val EXTRA_LONG2 = 800
    const val EXTRA_LONG3 = 900
    const val EXTRA_LONG4 = 1000

    // --- Easing tokens -------------------------------------------------------

    /** The emphasized curve: the one an on-screen movement begins and ends on. */
    val emphasized: Easing = CubicBezierEasing(0.2f, 0f, 0f, 1f)

    /** The emphasized curve for something arriving: quick off the mark, slow to settle. */
    val emphasizedDecelerate: Easing = CubicBezierEasing(0.05f, 0.7f, 0.1f, 1f)

    /** The emphasized curve for something leaving: slow to start, quick to go. */
    val emphasizedAccelerate: Easing = CubicBezierEasing(0.3f, 0f, 0.8f, 0.15f)

    /** The standard curve, for a movement that neither enters nor leaves. */
    val standard: Easing = CubicBezierEasing(0.2f, 0f, 0f, 1f)

    /** The standard curve for an entrance. */
    val standardDecelerate: Easing = CubicBezierEasing(0f, 0f, 0f, 1f)

    /** The standard curve for an exit. */
    val standardAccelerate: Easing = CubicBezierEasing(0.3f, 0f, 1f, 1f)

    /** No acceleration at all: progress, a scroll, a waveform. */
    val linear: Easing = CubicBezierEasing(0f, 0f, 1f, 1f)

    // --- Expressive springs --------------------------------------------------

    /** dampingRatio of a spatial spring: it settles with a trace of overshoot. */
    private const val SPATIAL_DAMPING = 0.8f

    /** dampingRatio of an effects spring: opacity and state layers do not overshoot. */
    private const val EFFECTS_DAMPING = 1f

    /** The quickest spatial spring: a small element that follows the pointer. */
    fun <T> spatialFast(threshold: T? = null): FiniteAnimationSpec<T> =
        spring(dampingRatio = SPATIAL_DAMPING, stiffness = 1400f, visibilityThreshold = threshold)

    /** The spatial spring a panel or a screen uses. */
    fun <T> spatialDefault(threshold: T? = null): FiniteAnimationSpec<T> =
        spring(dampingRatio = SPATIAL_DAMPING, stiffness = 380f, visibilityThreshold = threshold)

    /** The spatial spring of a large surface, which gets longer to travel. */
    fun <T> spatialSlow(threshold: T? = null): FiniteAnimationSpec<T> =
        spring(dampingRatio = SPATIAL_DAMPING, stiffness = 200f, visibilityThreshold = threshold)

    /** The quickest effects spring: a press or a hover acknowledgement. */
    fun <T> effectsFast(threshold: T? = null): FiniteAnimationSpec<T> =
        spring(dampingRatio = EFFECTS_DAMPING, stiffness = 3800f, visibilityThreshold = threshold)

    /** The effects spring a hover or a state layer uses. */
    fun <T> effectsDefault(threshold: T? = null): FiniteAnimationSpec<T> =
        spring(dampingRatio = EFFECTS_DAMPING, stiffness = 1600f, visibilityThreshold = threshold)

    /** The effects spring of a large fade. */
    fun <T> effectsSlow(threshold: T? = null): FiniteAnimationSpec<T> =
        spring(dampingRatio = EFFECTS_DAMPING, stiffness = 800f, visibilityThreshold = threshold)

    /**
     * Hover progress of one surface, 0 when the pointer is elsewhere and 1 when
     * it is on it.
     *
     * Ride it on the effects spring rather than a tween: a hover is a state
     * change, not a transition, and a spring that starts the instant the pointer
     * arrives is what makes a list feel like it answers the mouse. [enabled]
     * false keeps the progress at 0 for a disabled surface.
     */
    @Composable
    fun hoverProgress(hovered: Boolean, enabled: Boolean = true, label: String = "hover"): Float {
        val progress by animateFloatAsState(
            targetValue = if (hovered && enabled) 1f else 0f,
            animationSpec = effectsDefault<Float>(),
            label = label,
        )
        return progress
    }
}

/**
 * True while the pointer is on the interactive row this content is drawn in.
 *
 * A settings row owns the only hover state it can see, but the things a row
 * draws - a person's cookie, a cover - should answer the pointer wherever on the
 * row it is. `M3SettingsItemRow` provides this around its content, so a widget
 * inside a row follows the whole row instead of only its own few dp.
 */
val LocalRowHovered = compositionLocalOf { false }

/**
 * The pointer acknowledgement of a surface: it grows slightly toward the pointer
 * and settles back when the pointer leaves.
 *
 * [progress] is 0..1 (see [Motion.hoverProgress]); the surface is scaled by
 * [grow] at 1. A hover is a state change, so this is deliberately small - a
 * surface that moved far would read as a press.
 */
fun Modifier.hoverGrow(progress: Float, grow: Float = 0.06f): Modifier = graphicsLayer {
    val scale = 1f + grow * progress
    scaleX = scale
    scaleY = scale
}

/**
 * [Modifier.hoverGrow] plus the interaction source that drives it, for a
 * surface that is not already inside a row with a hover state of its own.
 */
@Composable
fun Modifier.hoverGrow(
    interactionSource: MutableInteractionSource,
    enabled: Boolean = true,
    grow: Float = 0.06f,
    label: String = "hoverGrow",
): Modifier {
    val hovered by interactionSource.collectIsHoveredAsState()
    val progress = Motion.hoverProgress(hovered, enabled = enabled, label = label)
    return this.hoverable(interactionSource, enabled = enabled).hoverGrow(progress, grow)
}
