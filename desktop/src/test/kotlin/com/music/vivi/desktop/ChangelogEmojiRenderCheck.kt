@file:OptIn(ExperimentalComposeUiApi::class)

package com.music.vivi.desktop

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.ImageComposeScene
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.platform.Font
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.jetbrains.skia.EncodedImageFormat
import java.io.File

/**
 * Headless check of the changelog emoji, run with
 *
 *     ./gradlew :desktop:changelogEmojiCheck
 *
 * It renders through the app's REAL Markdown path (`markdownInline` +
 * `MarkdownView`, the same functions the changelog screen uses) into an
 * off-screen `ImageComposeScene`, so no window, display or GPU is involved, and
 * then counts the pixels: how much ink each emoji paints, and how much of it is
 * chromatic.
 *
 * Why pixels at all. Compose Desktop does not fall back to a second font on its
 * own, so an emoji the resolved family does not carry is laid out as that
 * family's `.notdef`: ink, but no shape of the emoji. That is the "question mark
 * in a rhombus" the changelog was reported to show next to Fixed, Changed and
 * Commits, while Added (a one-unit codepoint) rendered normally. A colour font
 * whose COLR glyphs this renderer cannot paint is worse: it paints NOTHING.
 * Both are invisible to a compile check and to any assertion on the text, so the
 * check has to look at what actually lands on the canvas.
 *
 * Two independent signals are used, because neither is enough alone:
 *
 *  * INK against the reference. The emoji is first drawn through an explicit
 *    `SpanStyle(fontFamily = the family MarkdownFonts resolves it to)`, at the
 *    same font size, which is the glyph the fallback is supposed to produce. The
 *    real path must paint within [INK_TOLERANCE] of that. A `.notdef` box is a
 *    different shape and a different amount of ink, so it fails; this is the
 *    check that also covers the emoji whose ARTWORK IS GREY (see below).
 *  * CHROMA. For every emoji whose reference render is chromatic, the real path
 *    must be chromatic too, which is what a user sees as "coloured emoji".
 *
 * Grey artwork, i.e. why 5 of 7 headings are the chroma check and not all of
 * them: Twemoji Mozilla draws `🔧` (U+1F527) and `🗑️` (U+1F5D1) in greys, so
 * their reference render has ink but no chroma at all. They are still colour
 * (COLR) glyphs of the right shape, and demanding chroma of them would fail on
 * correct output. They are therefore held to the ink check only.
 *
 * The theme is seeded with a grey so the surrounding text carries no chroma of
 * its own: a coloured pixel in the image can only come from an emoji.
 */
object ChangelogEmojiRenderCheck {

    /** Heading emoji, i.e. what the changelog sections actually use. */
    private val headings = linkedMapOf(
        "🐛" to "Fixed",
        "🔧" to "Changed",
        "📝" to "Commits",
        "✨" to "Added",
        "🗑️" to "Removed",
        "📌" to "Notes",
        "🌍" to "Translations",
    )

    /**
     * How far the real path may differ from the explicit-span reference, as a
     * fraction of the reference ink. Generous: antialiasing and the odd
     * sub-pixel layout shift move a few pixels, while a `.notdef` box is off by
     * a factor, not a percentage.
     */
    private const val INK_TOLERANCE = 0.25

    /** The size both the reference and the direct markdownInline probes use. */
    private val PROBE_SIZE = 32.sp

    private val outDir = File(".ignore/emojiprobe/out")

    @JvmStatic
    fun main(args: Array<String>) {
        outDir.mkdirs()
        val failures = mutableListOf<String>()

        println("== how each family draws one emoji (ink / colour pixels)")
        println("   emoji   direct Twemoji   direct NotoEmoji   span Twemoji   markdownInline   app font (notdef)")
        for ((emoji, _) in headings) {
            val tag = emoji.first().code
            val direct = renderExplicit(emoji, explicitColorFamily, "direct-colour-$tag")
            val mono = renderExplicit(emoji, explicitMonoFamily, "direct-mono-$tag")
            val span = renderSpan(emoji, explicitColorFamily, PROBE_SIZE, "span-colour-$tag")
            val inline = renderInline(emoji, PROBE_SIZE, "inline-$tag")
            val notdef = renderExplicit(emoji, appFamily, "notdef-$tag")
            println(
                "   $emoji   ${cell(direct)}   ${cell(mono)}   ${cell(span)}   ${cell(inline)}   ${cell(notdef)}"
            )
        }
        println()

        println("== coverage read from the bundled fonts")
        println("   colour face: ${EmojiCoverage.colorEmoji.size} codepoints")
        println("   monochrome face: ${EmojiCoverage.monochromeEmoji.size} codepoints")
        reportFamilies()
        println()

        println("== the emoji alone, through markdownInline, against the family it resolves to")
        for ((emoji, _) in headings) {
            val family = MarkdownFonts.familyFor(emoji.codePointAt(0))
            val tag = emoji.first().code
            val ref = renderSpan(emoji, family ?: appFamily, PROBE_SIZE, "ref-$tag")
            val inline = renderInline(emoji, PROBE_SIZE, "inline-$tag")
            println(
                "   $emoji  ref=${ref.ink}/${ref.colour}  inline=${inline.ink}/${inline.colour}  " +
                    "${inkVerdict(inline, ref)}"
            )
            failures += checkInk(emoji, inline, ref)
            failures += checkChroma(emoji, inline, ref)
        }

        println()
        println("== the real changelog headings, through MarkdownView")
        for ((emoji, word) in headings) {
            val family = MarkdownFonts.familyFor(emoji.codePointAt(0))
            val tag = emoji.first().code
            val ref = renderSpan(emoji, family ?: appFamily, PROBE_SIZE, "ref-$tag")
            val withEmoji = renderMarkdown("### $emoji $word", "md-$tag")
            val withoutEmoji = renderMarkdown("### $word", "md-noemoji-$tag")
            // The word is identical in both, so the difference is the emoji.
            val drawn = withEmoji.ink - withoutEmoji.ink
            val ok = drawn > 0 && (ref.colour == 0 || withEmoji.colour > withoutEmoji.colour)
            println(
                "   $emoji $word  with=${withEmoji.ink}/${withEmoji.colour}  " +
                    "without=${withoutEmoji.ink}/${withoutEmoji.colour}  emojiInk=$drawn  " +
                    if (ok) "drawn" else "NOT DRAWN (or not in colour)"
            )
            if (drawn <= 0) failures += "$emoji $word: MarkdownView drew no ink for the emoji"
            else if (ref.colour > 0 && withEmoji.colour <= withoutEmoji.colour) {
                failures += "$emoji $word: markdown heading without colour"
            }
        }

        println()
        println("== emoji and the following word on one line, through markdownInline")
        for ((emoji, word) in headings) {
            val family = MarkdownFonts.familyFor(emoji.codePointAt(0))
            val tag = emoji.first().code
            val ref = renderSpan(emoji, family ?: appFamily, PROBE_SIZE, "ref-$tag")
            val bare = renderInline(emoji, PROBE_SIZE, "inline-bare-$tag")
            val line = renderInline("$emoji $word", PROBE_SIZE, "inline-line-$tag")
            val drawn = line.ink - renderInline(word, PROBE_SIZE, "inline-word-$tag").ink
            println(
                "   $emoji $word  line=${line.ink}/${line.colour}  emojiInk=$drawn  bare=${bare.ink}  " +
                    if (drawn > 0) "drawn" else "NOT DRAWN"
            )
            if (drawn <= 0) failures += "$emoji $word: inline line drew no ink for the emoji"
            if (ref.colour > 0 && line.colour <= 0) failures += "$emoji $word: inline line without colour"
        }

        if (failures.isEmpty()) {
            println()
            println("OK: every changelog emoji is drawn, in colour, on every path")
            return
        }
        println()
        println("FAILED:")
        failures.forEach { println("  - $it") }
        throw IllegalStateException("changelog emoji check failed (${failures.size} case(s))")
    }

    private data class Stats(val ink: Int, val colour: Int)

    private fun cell(stats: Stats): String = "${stats.ink}/${stats.colour}".padEnd(10)

    /** The real path must paint the glyph its family owns, not a `.notdef`. */
    private fun checkInk(emoji: String, inline: Stats, ref: Stats): List<String> {
        if (ref.ink <= 0) return listOf("$emoji: the bundled family does not draw it at all")
        val diff = kotlin.math.abs(inline.ink - ref.ink).toDouble() / ref.ink
        if (diff > INK_TOLERANCE) {
            return listOf(
                "$emoji: markdownInline paints ${inline.ink} ink where its family paints ${ref.ink} " +
                    "(a .notdef box, not the emoji)"
            )
        }
        return emptyList()
    }

    /** A chromatic reference has to come out chromatic, otherwise it is not coloured. */
    private fun checkChroma(emoji: String, inline: Stats, ref: Stats): List<String> {
        if (ref.colour > 0 && inline.colour <= 0) {
            return listOf("$emoji: the family paints it in colour, the real path does not")
        }
        return emptyList()
    }

    private fun inkVerdict(inline: Stats, ref: Stats): String {
        if (inline.ink <= 0) return "NOT DRAWN"
        val diff = kotlin.math.abs(inline.ink - ref.ink).toDouble() / ref.ink
        return when {
            ref.colour > 0 && inline.colour <= 0 -> "NO COLOUR"
            diff > INK_TOLERANCE -> "WRONG GLYPH"
            else -> "colour"
        }
    }

    /**
     * Which family each class of codepoint resolves to, by object identity:
     * two codepoints that report the same family are painted by the same face.
     * A colour emoji and a monochrome-only emoji must NOT report the same one.
     */
    private fun reportFamilies() {
        val colourA = MarkdownFonts.familyFor(0x1F41B) // 🐛 in both faces
        val colourB = MarkdownFonts.familyFor(0x2728) // ✨ in both faces
        val monoOnly = MarkdownFonts.familyFor(0x1FA75) // 🩵 only in the monochrome face
        val osOnly = MarkdownFonts.familyFor(0x1FAE9) // newer than both faces
        println("   colour emoji share one family: ${colourA === colourB}")
        println("   monochrome-only emoji uses the same family: ${monoOnly === colourA}")
        println("   an emoji from neither bundled face resolves: ${osOnly != null}")
    }

    private val explicitColorFamily = FontFamily(Font("fonts/TwemojiColorEmoji.ttf", FontWeight.Normal))

    private val explicitMonoFamily = FontFamily(Font("fonts/NotoEmoji.ttf", FontWeight.Normal))

    /** The app's own face: what draws the "question mark in a rhombus" notdef. */
    private val appFamily = FontFamily(Font("fonts/google_sans_flex.ttf", FontWeight.Normal))

    /** The same emoji, but reached through a SpanStyle instead of a Text style. */
    private fun renderSpan(text: String, family: FontFamily, size: androidx.compose.ui.unit.TextUnit, name: String): Stats {
        val annotated = AnnotatedString(
            text,
            spanStyles = listOf(
                AnnotatedString.Range(SpanStyle(fontFamily = family), 0, text.length)
            ),
        )
        return render(name) { Text(annotated, style = TextStyle(fontSize = size)) }
    }

    private fun renderExplicit(text: String, family: FontFamily, name: String): Stats =
        render(name, width = 120) {
            Text(text, style = TextStyle(fontFamily = family, fontSize = PROBE_SIZE))
        }

    private fun renderInline(text: String, size: androidx.compose.ui.unit.TextUnit, name: String): Stats =
        render(name, width = 260) {
            Text(markdownInline(text, issueBase = "", onOpenUrl = {}, linkColor = Color.Black), style = TextStyle(fontSize = size))
        }

    private fun renderMarkdown(markdown: String, name: String): Stats =
        render(name, width = 320, height = 120) {
            Column(Modifier.padding(8.dp)) { MarkdownView(markdown, onOpenUrl = {}) }
        }

    private fun render(
        name: String,
        width: Int = 120,
        height: Int = 96,
        content: @androidx.compose.runtime.Composable () -> Unit,
    ): Stats {
        val image = ImageComposeScene(width, height) {
            AppTheme(mode = ThemeMode.LIGHT, accent = NEUTRAL) { content() }
        }.use { it.render() }
        return stats(image.encodeToData(EncodedImageFormat.PNG, 100)!!.bytes, name)
    }

    private inline fun <T> ImageComposeScene.use(block: (ImageComposeScene) -> T): T =
        try {
            block(this)
        } finally {
            close()
        }

    private fun stats(png: ByteArray, name: String): Stats {
        File(outDir, "$name.png").writeBytes(png)
        val image = javax.imageio.ImageIO.read(png.inputStream())
        var ink = 0
        var colour = 0
        for (y in 0 until image.height) {
            for (x in 0 until image.width) {
                val rgb = image.getRGB(x, y)
                val r = (rgb shr 16) and 255
                val g = (rgb shr 8) and 255
                val b = rgb and 255
                if (r > 245 && g > 245 && b > 245) continue
                ink++
                if (maxOf(r, g, b) - minOf(r, g, b) > 40) colour++
            }
        }
        return Stats(ink, colour)
    }

    /** A grey seed: the surrounding text then carries no chroma of its own. */
    private val NEUTRAL = Color(0xFF7F7F7F)
}
