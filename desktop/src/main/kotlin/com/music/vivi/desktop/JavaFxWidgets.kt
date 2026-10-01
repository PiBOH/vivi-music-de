package com.music.vivi.desktop

import javafx.scene.control.TextArea
import javafx.scene.layout.HBox
import javafx.scene.layout.Priority
import javafx.scene.paint.Color

/**
 * A read-only, selectable, wrapping line of text for the sign-in windows.
 *
 * The header bars and the numbered steps were plain labels: on the dark header
 * their default (dark) text was unreadable ("dark on dark"), and a `Label`
 * cannot be selected, so the message could not even be copied. A `TextArea`
 * that is not editable and not focus-traversable, styled with an explicit
 * [textColor] and [background], gives both the requested contrast and normal
 * mouse selection plus copy.
 *
 * [background] must be the colour of the bar the field sits on. A `TextArea`
 * paints its own `-fx-control-inner-background` behind the text, and leaving it
 * transparent is not enough: the field then falls back to the skin's default
 * white, which is why the light header text came out "light on a white box"
 * instead of on the dark header. Setting the inner background to the exact bar
 * colour makes the field invisible against it whichever container CSS wins.
 *
 * The height follows the text: `prefRowCount` is recomputed whenever the text
 * changes, so a long message wraps onto more rows instead of growing an inner
 * scrollbar.
 */
internal fun selectableText(
    text: String,
    textColor: Color,
    fontSize: Double? = null,
    background: Color = Color.TRANSPARENT,
): TextArea {
    fun rows(value: String?): Int = ((value?.length ?: 0) / 90) + 1
    fun style(): String = buildString {
        // The whole field is painted with the bar's own colour, on the control
        // and on its inner regions alike, so no white box can show through.
        val fill = cssColor(background)
        append("-fx-background-color: ").append(fill).append(';')
        append("-fx-control-inner-background: ").append(fill).append(';')
        append("-fx-background-insets: 0;")
        append("-fx-background-radius: 0;")
        append("-fx-padding: 0;")
        append("-fx-focus-color: transparent;")
        append("-fx-faint-focus-color: transparent;")
        append("-fx-highlight-fill: #7f6bb3;")
        append("-fx-text-fill: ").append(toHex(textColor)).append(';')
        if (fontSize != null) append("-fx-font-size: ").append(fontSize).append("px;")
    }
    val area = TextArea(text).apply {
        isWrapText = true
        isEditable = false
        isFocusTraversable = false
        maxWidth = Double.MAX_VALUE
        prefRowCount = rows(text)
        style = style()
    }
    area.textProperty().addListener { _, _, value -> area.prefRowCount = rows(value) }
    HBox.setHgrow(area, Priority.ALWAYS)
    return area
}

/** CSS colour for [color]: "transparent" when it is fully clear, else `#rrggbb`. */
private fun cssColor(color: Color): String = if (color.opacity <= 0.0) "transparent" else toHex(color)

private fun toHex(color: Color): String = String.format(
    "#%02x%02x%02x",
    (color.red * 255).toInt().coerceIn(0, 255),
    (color.green * 255).toInt().coerceIn(0, 255),
    (color.blue * 255).toInt().coerceIn(0, 255),
)
