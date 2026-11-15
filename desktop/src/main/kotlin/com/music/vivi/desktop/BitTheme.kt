package com.music.vivi.desktop

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.hoverable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsHoveredAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.Text
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage

/**
 * The palette measured on Bit's desktop window. The cloned shell paints
 * these values directly instead of the tonal scheme: Bit is not a Material
 * app, so every surface here is one of the three near-black steps separated by
 * 1 px rules.
 */
internal object BC {
    val Chrome = Color(0xFF0F0F11)
    val Page = Color(0xFF202020)
    val Divider = Color(0xFF121213)
    val Card = Color(0xFF0C0C0C)
    val CardBorder = Color(0xFF141414)
    val Search = Color(0xFF1F1F21)
    val SearchBorder = Color(0xFF2A2A2C)
    val Hover = Color(0xFF242424)
    val RailSelected = Color(0xFF2C2C2D)
    val TrackPill = Color(0xFF323236)
    val Secondary = Color(0xFF8E8E93)
    val Red = Color(0xFFFA2D48)
    val Green = Color(0xFF28C840)
    val Yellow = Color(0xFFFEBC2E)
    val White = Color.White

    /** A tinted page colour, for the album and artist headers. */
    fun tint(seed: Color, amount: Float = 0.20f): Color =
        Color(
            red = seed.red * amount,
            green = seed.green * amount,
            blue = seed.blue * amount,
            alpha = 1f,
        )
}

@Composable
internal fun BcTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = darkColorScheme(
            primary = BC.White,
            onPrimary = Color.Black,
            background = BC.Page,
            onBackground = BC.White,
            surface = BC.Chrome,
            onSurface = BC.White,
            surfaceVariant = BC.Search,
            onSurfaceVariant = BC.Secondary,
            outline = BC.CardBorder,
            outlineVariant = BC.Divider,
            error = BC.Red,
        ),
        typography = bcTypography(),
        shapes = Shapes(
            extraSmall = RoundedCornerShape(4.dp),
            small = RoundedCornerShape(7.dp),
            medium = RoundedCornerShape(10.dp),
            large = RoundedCornerShape(14.dp),
            extraLarge = RoundedCornerShape(20.dp),
        ),
        content = content,
    )
}

/**
 * The SF Pro Display scale Bit uses, drawn with the platform's own UI face
 * because SF Pro may not be redistributed. The sizes, the weights and the tight
 * negative tracking are the reference's; only the file is different.
 */
private fun bcTypography(): Typography {
    val f = FontFamily.Default
    fun style(size: Int, weight: FontWeight, tracking: Double = 0.0) = TextStyle(
        fontFamily = f,
        fontWeight = weight,
        fontSize = size.sp,
        letterSpacing = tracking.sp,
        color = BC.White,
    )
    val defaults = Typography()
    return defaults.copy(
        displayLarge = style(34, FontWeight.W800, -0.8),
        displayMedium = style(30, FontWeight.W800, -0.7),
        displaySmall = style(26, FontWeight.W700, -0.5),
        headlineLarge = style(30, FontWeight.W800, -0.7),
        headlineMedium = style(22, FontWeight.W700, -0.4),
        headlineSmall = style(20, FontWeight.W700, -0.3),
        titleLarge = style(20, FontWeight.W700, -0.3),
        titleMedium = style(16, FontWeight.W600, -0.2),
        titleSmall = style(14, FontWeight.W600, -0.1),
        bodyLarge = style(16, FontWeight.W400),
        bodyMedium = style(14, FontWeight.W400),
        bodySmall = style(12, FontWeight.W400),
        labelLarge = style(14, FontWeight.W600),
        labelMedium = style(12, FontWeight.W600, 0.2),
        labelSmall = style(11, FontWeight.W600, 0.2),
    )
}

/** A click target with Bit's hover wash, on one shared interaction source. */
@Composable
internal fun BcHover(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    shape: Shape = RoundedCornerShape(7.dp),
    hoverFill: Color = BC.Hover,
    enabled: Boolean = true,
    content: @Composable BoxScope.() -> Unit,
) {
    val source = remember { MutableInteractionSource() }
    val hovered by source.collectIsHoveredAsState()
    // The reference fades a wash in under the pointer instead of switching it.
    val wash by animateColorAsState(
        targetValue = if (hovered && enabled) hoverFill else Color.Transparent,
        animationSpec = tween(durationMillis = 120),
        label = "bit-hover",
    )
    Box(
        modifier
            .clip(shape)
            .background(wash)
            .hoverable(source, enabled)
            .clickable(source, indication = null, enabled = enabled, onClick = onClick),
        content = content,
    )
}

/** The one circular control the bar and the pages use. */
@Composable
internal fun BcIconButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    size: Int = 32,
    content: @Composable () -> Unit,
) {
    BcHover(
        onClick = onClick,
        modifier = modifier.size(size.dp),
        shape = CircleShape,
        enabled = enabled,
    ) {
        Box(Modifier.size(size.dp), contentAlignment = Alignment.Center) { content() }
    }
}

/** Bit's cover: a flat rounded square, no cookie morph, no border. */
@Composable
internal fun BcArtwork(
    url: String?,
    modifier: Modifier = Modifier,
    shape: Shape = RoundedCornerShape(4.dp),
) {
    Box(modifier.clip(shape).background(Color(0xFF2A2A2C))) {
        if (!url.isNullOrBlank()) {
            AsyncImage(
                model = url,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.matchParentSize(),
            )
        } else {
            Box(
                Modifier.matchParentSize().background(
                    Brush.linearGradient(listOf(Color(0xFF303034), Color(0xFF1A1A1C))),
                ),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    Icons.Filled.MusicNote,
                    contentDescription = null,
                    tint = BC.Secondary.copy(alpha = 0.7f),
                    modifier = Modifier.size(28.dp),
                )
            }
        }
    }
}

/**
 * What a card does under the pointer: it grows by 3.5% about its own centre
 * over 140 ms and casts a little more shadow. The reference's own numbers
 * (scale 1.035, 18 dp, 140 ms), so a shelf of cards reacts the same way.
 */
@Composable
internal fun Modifier.bcHoverLift(shape: Shape = RoundedCornerShape(6.dp)): Modifier {
    val source = remember { MutableInteractionSource() }
    val hovered by source.collectIsHoveredAsState()
    val lift by animateFloatAsState(
        targetValue = if (hovered) 1f else 0f,
        animationSpec = tween(durationMillis = 140),
        label = "bit-lift",
    )
    return this
        .hoverable(source)
        .graphicsLayer {
            val grown = 1f + 0.035f * lift
            scaleX = grown
            scaleY = grown
        }
        .shadow(18.dp * lift, shape)
}

/** One hairline, the only separator Bit draws. */
@Composable
internal fun BcDivider(modifier: Modifier = Modifier, color: Color = BC.Divider) {
    Box(modifier.height(1.dp).background(color))
}

@Composable
internal fun BcSectionTitle(
    text: String,
    modifier: Modifier = Modifier,
    trailing: @Composable (() -> Unit)? = null,
) {
    Row(modifier, verticalAlignment = Alignment.CenterVertically) {
        Text(
            text,
            style = MaterialTheme.typography.headlineMedium,
            color = BC.White,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        Spacer(Modifier.weight(1f))
        trailing?.invoke()
    }
}

@Composable
internal fun BcSecondary(text: String, modifier: Modifier = Modifier, maxLines: Int = 1) {
    Text(
        text,
        modifier = modifier,
        style = MaterialTheme.typography.bodyMedium,
        color = BC.Secondary,
        maxLines = maxLines,
        overflow = TextOverflow.Ellipsis,
    )
}

/** mm:ss for a duration in seconds, and h:mm:ss when it runs past an hour. */
internal fun bcDuration(seconds: Int?): String {
    if (seconds == null || seconds <= 0) return ""
    val h = seconds / 3600
    val m = (seconds % 3600) / 60
    val s = seconds % 60
    return if (h > 0) "%d:%02d:%02d".format(h, m, s) else "%d:%02d".format(m, s)
}

internal fun bcDurationMs(ms: Long): String = bcDuration((ms / 1000L).toInt())

/**
 * Submits on a physical Enter key.
 *
 * Compose only wires `KeyboardActions(onSearch = ...)` to a soft-keyboard IME
 * action. On a desktop with a real keyboard there is no IME action, so the
 * search fields received the query and never ran it: Enter did nothing until
 * the user clicked a filter chip. Every text field that submits binds this.
 */
internal fun Modifier.bcSubmitOnEnter(onSubmit: () -> Unit): Modifier = onPreviewKeyEvent { event ->
    val isEnter = event.key == Key.Enter || event.key == Key.NumPadEnter
    if (event.type == KeyEventType.KeyDown && isEnter) {
        onSubmit()
        true
    } else {
        false
    }
}

/** The three window buttons, drawn as macOS traffic lights like Bit. */
@Composable
internal fun BcWindowDots(
    isMaximized: Boolean,
    onMinimize: () -> Unit,
    onMaximize: () -> Unit,
    onClose: () -> Unit,
) {
    Row(horizontalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(8.dp)) {
        WindowDot(BC.Red, "Close", onClose)
        WindowDot(BC.Yellow, "Minimize", onMinimize)
        WindowDot(BC.Green, if (isMaximized) "Restore" else "Maximize", onMaximize)
    }
}

@Composable
private fun WindowDot(color: Color, label: String, onClick: () -> Unit) {
    Box(
        Modifier
            .size(12.dp)
            .clip(CircleShape)
            .background(color)
            .clickable(onClick = onClick),
    )
}

@Composable
internal fun BcAvatar(name: String, size: Int = 20) {
    Box(
        Modifier.size(size.dp).clip(CircleShape).background(Color(0xFF6E4AA8)),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            name.trim().take(1).uppercase().ifBlank { "V" },
            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold, fontSize = (size * 0.48f).sp),
            color = BC.White,
        )
    }
}

/** A thin white slider, the shape the top bar's volume control uses. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun BcThinSlider(
    value: Float,
    onValueChange: (Float) -> Unit,
    modifier: Modifier = Modifier,
    height: Int = 4,
) {
    androidx.compose.material3.Slider(
        value = value.coerceIn(0f, 1f),
        onValueChange = onValueChange,
        modifier = modifier.height(18.dp),
        colors = androidx.compose.material3.SliderDefaults.colors(
            thumbColor = BC.White,
            activeTrackColor = BC.White,
            inactiveTrackColor = Color.White.copy(alpha = 0.22f),
        ),
        thumb = {
            Box(Modifier.size(height.dp * 2f).clip(CircleShape).background(BC.White))
        },
    )
}
