@file:OptIn(ExperimentalComposeUiApi::class)

package com.music.vivi.desktop

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.ImageComposeScene
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import org.jetbrains.skia.EncodedImageFormat
import java.io.File

/**
 * Headless check of the language picker's search box, run with
 *
 *     ./gradlew :desktop:languageSearchCheck
 *
 * Settings -> Language lists 48 languages on the screen, and the field above the
 * list is the only way to reach one of them without reading the whole list. Two
 * halves are checked, because they fail differently:
 *
 *  * **the filter** (`Languages.matching`) is read directly: a blank query is
 *    every language, "ital" is Italiano alone, the accents do not matter
 *    ("cestina" finds Čeština), the Azerbaijani schwa does not matter
 *    ("azerbaycan" finds Azərbaycan dili), the code works ("de" finds Deutsch)
 *    and a query that matches nothing returns nothing;
 *  * **the rows** are rendered into an off-screen `ImageComposeScene` and the
 *    pixels are read back: the search field is drawn, and the list below it is
 *    a screenful of languages for a blank query while "ital" leaves one row and
 *    "zzz" leaves the line that says so.
 *
 * It does NOT check typing: a `KeyEvent` cannot be sent to a headless scene, so
 * the field's own wiring (a keystroke sets the query) is what the running app
 * exercises. PNGs are written under `.ignore/language/`.
 */
object LanguageSearchCheck {

    private val outDir = File(".ignore/language")

    private const val WIDTH = 520
    private const val HEIGHT = 600

    @JvmStatic
    fun main(args: Array<String>) {
        outDir.mkdirs()
        val failures = mutableListOf<String>()

        // ------------------------------------------------------------- the filter
        val all = Languages.matching("")
        println("== filter")
        println("   '' -> ${all.size} language(s)")
        if (all.size != Languages.all.size) {
            failures += "a blank query must list every language (${Languages.all.size}), read ${all.size}"
        }
        println("   'ital' -> ${names("ital")}")
        if (names("ital") != listOf("Italiano")) failures += "ital must find Italiano alone, read ${names("ital")}"
        println("   'cestina' -> ${names("cestina")}")
        if (names("cestina") != listOf("Čeština")) {
            failures += "cestina must find Čeština (the accent must not matter), read ${names("cestina")}"
        }
        println("   'azerbaycan' -> ${names("azerbaycan")}")
        if (names("azerbaycan") != listOf("Azərbaycan dili")) {
            failures += "azerbaycan must find Azərbaycan dili (the schwa must not matter), read ${names("azerbaycan")}"
        }
        println("   'de' -> ${names("de")}")
        if (Languages.matching("de").none { it.code == "de" }) {
            failures += "de must find Deutsch by its code, read ${names("de")}"
        }
        println("   'ZZZ' -> ${names("ZZZ")}")
        if (Languages.matching("ZZZ").isNotEmpty()) failures += "ZZZ must match nothing, read ${names("ZZZ")}"
        println("   '  it  ' -> ${names("  it  ")}")
        if (names("  it  ").none { it == "Italiano" }) {
            failures += "a query with spaces around it must still find Italiano, read ${names("  it  ")}"
        }

        // --------------------------------------------------------------- the rows
        val empty = render("")
        val italian = render("ital")
        val none = render("zzz")
        File(outDir, "all.png").writeBytes(empty)
        File(outDir, "ital.png").writeBytes(italian)
        File(outDir, "none.png").writeBytes(none)

        println("== rows")
        for ((label, png) in listOf("''" to empty, "ital" to italian, "zzz" to none)) {
            val field = firstBand(png)
            val bottom = lastPaintedRow(png)
            println("   query=$label field=${field?.let { "${it.first}..${it.last} (${it.last - it.first + 1})" }} " +
                "width=${field?.let { bandWidth(png, it) }} last painted row=$bottom")
            val height = field?.let { it.last - it.first + 1 }
            if (height == null || height !in 40..80) {
                failures += "query=$label: the search field must be drawn as a row of 40..80 px, read $height"
            }
            val width = field?.let { bandWidth(png, it) } ?: 0
            // The field is a row of the page now, so it reads the page's own
            // width: a field narrower than the list it narrows would be the
            // regression this catches.
            if (width !in 400..(WIDTH - 20)) {
                failures += "query=$label: the search field must span the page ($WIDTH px), read $width"
            }
            when (label) {
                "''" -> if (bottom < HEIGHT - 40) {
                    failures += "a blank query must fill the menu with languages, but the content ends at $bottom"
                }
                "ital" -> if (bottom !in 100..260) {
                    failures += "ital must leave the field and a row or two ($bottom), read $bottom"
                }
                "zzz" -> if (bottom !in 100..260) {
                    failures += "a query that matches nothing must leave the field and its line ($bottom), read $bottom"
                }
            }
        }
        if (same(italian, none)) {
            failures += "the one matching row and the no-result line are the same picture"
        }

        if (failures.isEmpty()) {
            println()
            println("OK: the picker's search narrows the list, ignores case, accents and the schwa")
            return
        }
        println()
        println("FAILED:")
        failures.forEach { println("  - $it") }
        throw IllegalStateException("language search check failed (${failures.size} case(s))")
    }

    private fun names(query: String): List<String> = Languages.matching(query).map { it.name }

    private fun render(query: String): ByteArray {
        val scene = ImageComposeScene(WIDTH, HEIGHT) {
            AppTheme(mode = ThemeMode.LIGHT, accent = Color(0xFFED5564), language = "en") {
                Box(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
                    Column(
                        Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 12.dp)
                            .verticalScroll(rememberScrollState()),
                    ) {
                        LanguagePickerRows(
                            language = "en",
                            selected = "it",
                            onSelect = {},
                            initialQuery = query,
                        )
                    }
                }
            }
        }
        return try {
            scene.render().encodeToData(EncodedImageFormat.PNG, 100)!!.bytes
        } finally {
            scene.close()
        }
    }

    /** The first run of rows that paint anything: the search field's outline. */
    private fun firstBand(png: ByteArray): IntRange? {
        val image = javax.imageio.ImageIO.read(png.inputStream())
        val page = image.getRGB(image.width / 2, 2)
        var start = -1
        for (y in 0 until image.height) {
            val painted = rowInk(image, y, page)
            if (painted > 0) {
                if (start < 0) start = y
            } else if (start >= 0) {
                return start until (y - 1)
            }
        }
        return if (start >= 0) start until (image.height - 1) else null
    }

    /** How wide the field's outline is drawn, in px: a menu body is not the window. */
    private fun bandWidth(png: ByteArray, band: IntRange): Int {
        val image = javax.imageio.ImageIO.read(png.inputStream())
        val page = image.getRGB(image.width / 2, 2)
        var first = -1
        var last = -1
        for (x in 0 until image.width) {
            var painted = false
            for (y in band.first..band.last) {
                if (image.getRGB(x, y) != page) {
                    painted = true
                    break
                }
            }
            if (painted) {
                if (first < 0) first = x
                last = x
            }
        }
        return if (first < 0) 0 else last - first + 1
    }

    /** The last row that paints anything, page included: where the content ends. */
    private fun lastPaintedRow(png: ByteArray): Int {
        val image = javax.imageio.ImageIO.read(png.inputStream())
        val page = image.getRGB(image.width / 2, 2)
        for (y in image.height - 1 downTo 0) {
            if (rowInk(image, y, page) > 0) return y
        }
        return 0
    }

    private fun rowInk(image: java.awt.image.BufferedImage, y: Int, page: Int): Int {
        var n = 0
        for (x in 0 until image.width) {
            if (image.getRGB(x, y) != page) n++
        }
        return n
    }

    private fun same(a: ByteArray, b: ByteArray): Boolean {
        val left = javax.imageio.ImageIO.read(a.inputStream())
        val right = javax.imageio.ImageIO.read(b.inputStream())
        if (left.width != right.width || left.height != right.height) return false
        for (y in 0 until left.height) {
            for (x in 0 until left.width) {
                if (left.getRGB(x, y) != right.getRGB(x, y)) return false
            }
        }
        return true
    }
}
