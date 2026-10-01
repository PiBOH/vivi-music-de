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
 * that is not editable and not focus-traversable, styled fully transparent with
 * an explicit [textColor], gives both the requested contrast (light text on the
 * dark header, dark text on a light bar) and normal mouse selection plus copy.
 *
 * The height follows the text: `prefRowCount` is recomputed whenever the text
 * changes, so a long message wraps onto more rows instead of growing an inner
 * scrollbar.
 */
internal fun selectableText(
    text: String,
    textColor: Color,
    fontSize: Double? = null,
): TextArea {
    fun rows(value: String?): Int = ((value?.length ?: 0) / 90) + 1
    fun style(): String = buildString {
        append("-fx-background-color: transparent;")
        append("-fx-control-inner-background: transparent;")
        append("-fx-background-insets: 0;")
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

private fun toHex(color: Color): String = String.format(
    "#%02x%02x%02x",
    (color.red * 255).toInt().coerceIn(0, 255),
    (color.green * 255).toInt().coerceIn(0, 255),
    (color.blue * 255).toInt().coerceIn(0, 255),
)
