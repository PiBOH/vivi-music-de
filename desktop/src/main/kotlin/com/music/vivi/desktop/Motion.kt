package com.music.vivi.desktop

import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.Easing
import androidx.compose.animation.core.FiniteAnimationSpec
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.snap
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.hoverable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsHoveredAsState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

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

    // --- Pointer movement ----------------------------------------------------

    /**
     * How far the pointer turns a shape, in degrees per axis.
     *
     * Orion-Store's `data-tilt` reads `-(y / height - .5) * 9` on rotateX and
     * `(x / width - .5) * 9` on rotateY, so a pointer at an edge of the box is
     * `4.5` degrees off flat and the middle is 0.
     */
    const val TILT_MAX_DEGREES = 4.5f

    /**
     * The perspective the tilt is seen through, in px: Orion sets
     * `transformPerspective: 900`, and a smaller value would make the same 4.5
     * degrees read as a wobble rather than as a surface turning.
     */
    const val TILT_PERSPECTIVE = 900f

    /**
     * How far an icon lifts and turns under the pointer, after Orion-Store's
     * `.rel-icon` (`translateY(-2px) rotate(-6deg)`).
     */
    const val ICON_LIFT_DP = 2f
    const val ICON_TURN_DEGREES = -6f

    /**
     * How long the pointer tilt takes to reach the pointer, after Orion-Store's
     * `data-tilt`: its `gsap.quickTo` runs the two axes over `duration: 0.8`,
     * which is [EXTRA_LONG2].
     */
    const val POINTER_TILT_MS = EXTRA_LONG2

    /**
     * How long the icon's lift and turn take, after Orion-Store's `.rel-icon`
     * (`transition: transform .6s`), which is [LONG4].
     */
    const val ICON_HOVER_MS = LONG4

    /**
     * The angle of one tilt axis for a pointer at [position] of the box (0..1):
     * the middle is flat and the edges are [maxDegrees] off it.
     *
     * It is the expression Orion-Store's `data-tilt` runs,
     * `(x / width - .5) * 9`, with the 9 split into the `2 * 4.5` above so the
     * number here is the angle a reader can check rather than half of it.
     */
    fun tiltPosition(position: Float, maxDegrees: Float = TILT_MAX_DEGREES): Float =
        (position - 0.5f) * 2f * maxDegrees

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

    /**
     * The pointer movement's own spec: [ms] on the emphasized-decelerate curve.
     *
     * The cookies and the icons follow the pointer with a movement that leaves
     * at once and settles slowly, which is what Orion-Store's `power3.out` and
     * its `--ease` token both are; the published decelerating token is the same
     * shape, so the table keeps one curve. The `Animations` master switch still
     * makes it instant, and the speed setting still scales it.
     */
    fun pointerSpec(ms: Int = POINTER_TILT_MS): FiniteAnimationSpec<Float> =
        if (Animations.instant) snap() else tween(Animations.ms(ms), easing = emphasizedDecelerate)
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

/**
 * The pointer tilt, after Orion-Store's `data-tilt`: a shape turns in 3D toward
 * the place the pointer is, up to [Motion.TILT_MAX_DEGREES] per axis, seen
 * through [Motion.TILT_PERSPECTIVE].
 *
 * The middle of the surface is flat and the edges are not, so the movement
 * follows the pointer instead of repeating the same spin wherever on the shape
 * it lands, and leaving the shape returns it to flat. That is the difference
 * between this and a hover scale: the cookies and the clover already round their
 * corners and grow, and this is the movement that says the surface knows where
 * the mouse is.
 *
 * The two axes travel over [POINTER_TILT_MS] on the decelerating curve, which is
 * Orion's own 0.8 s `power3.out`: the surface starts turning the moment the
 * pointer moves and settles slowly behind it, and it is back to flat by the time
 * the pointer has left.
 */
@Composable
fun Modifier.pointerTilt(
    enabled: Boolean = true,
    maxDegrees: Float = Motion.TILT_MAX_DEGREES,
    label: String = "pointerTilt",
): Modifier {
    // Where in the box (0..1 on each axis) the pointer is, and the middle when it
    // is not on the shape at all.
    val pointer = remember { mutableStateOf(Offset(0.5f, 0.5f)) }
    val track = Modifier.pointerInput(enabled) {
        if (!enabled) return@pointerInput
        awaitPointerEventScope {
            while (true) {
                val event = awaitPointerEvent()
                when (event.type) {
                    PointerEventType.Move -> {
                        val at = event.changes.firstOrNull()?.position ?: continue
                        pointer.value = Offset(
                            (at.x / size.width.coerceAtLeast(1)).coerceIn(0f, 1f),
                            (at.y / size.height.coerceAtLeast(1)).coerceIn(0f, 1f),
                        )
                    }
                    PointerEventType.Exit -> pointer.value = Offset(0.5f, 0.5f)
                    else -> Unit
                }
            }
        }
    }
    val x by animateFloatAsState(pointer.value.x, Motion.pointerSpec(), label = "$label-x")
    val y by animateFloatAsState(pointer.value.y, Motion.pointerSpec(), label = "$label-y")
    return this.then(track).graphicsLayer {
        // The signs are Orion's: the edge under the pointer turns away from the
        // viewer on the vertical axis and toward it on the horizontal one, which
        // is what makes the surface read as following the mouse instead of
        // rocking the same way wherever it is entered.
        rotationX = -Motion.tiltPosition(y, maxDegrees)
        rotationY = Motion.tiltPosition(x, maxDegrees)
        cameraDistance = Motion.TILT_PERSPECTIVE * density
    }
}

/**
 * The icon micro-hover, after Orion-Store's `.rel-icon`: under the pointer the
 * glyph lifts [lift] and turns [degrees], and it settles back on its own when the
 * pointer leaves.
 *
 * [progress] is 0..1 (see [Motion.hoverProgress]). A glyph is too small to see a
 * 3D tilt on, which is why Orion gives its icons this movement and keeps the tilt
 * for the surfaces that carry them.
 */
fun Modifier.iconHoverLift(
    progress: Float,
    lift: Dp = Motion.ICON_LIFT_DP.dp,
    degrees: Float = Motion.ICON_TURN_DEGREES,
): Modifier = graphicsLayer {
    translationY = -lift.toPx() * progress
    rotationZ = degrees * progress
}

/**
 * [Modifier.iconHoverLift] plus the interaction source that drives it, for an
 * icon that is not inside a row with a hover state of its own.
 */
@Composable
fun Modifier.iconHoverLift(
    enabled: Boolean = true,
    lift: Dp = Motion.ICON_LIFT_DP.dp,
    degrees: Float = Motion.ICON_TURN_DEGREES,
    label: String = "iconHoverLift",
): Modifier {
    val interaction = remember { MutableInteractionSource() }
    val hovered by interaction.collectIsHoveredAsState()
    val progress by animateFloatAsState(
        targetValue = if (hovered && enabled) 1f else 0f,
        animationSpec = Motion.pointerSpec(Motion.ICON_HOVER_MS),
        label = label,
    )
    return this.hoverable(interaction, enabled = enabled).iconHoverLift(progress, lift, degrees)
}
