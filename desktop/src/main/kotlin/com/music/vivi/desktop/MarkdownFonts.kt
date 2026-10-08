package com.music.vivi.desktop

import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.platform.Font
import java.io.File

/**
 * Glyph fallbacks for the Markdown renderer.
 *
 * The app draws every string with its own font (`AppFonts`), and those fonts
 * carry an alphabet, not the whole of Unicode: an emoji in a changelog heading
 * (`✨ Added`, `🐛 Fixed`, `🔧 Changed`, `📝 Commits`) or a stray symbol in a
 * bullet has no glyph there, and Compose Desktop does not fall back to a second
 * font on its own. The character then comes out as the font's `.notdef`, which
 * in this app's face is a question mark inside a rhombus: that is exactly what
 * was reported next to Fixed, Changed and Commits, while Added showed, because
 * its codepoint happened to be covered. This resolves those characters to a
 * family that really has them and hands the renderer a single-character span per
 * codepoint.
 *
 * It is deliberately NOT part of `AppFonts` / the font picker: these families
 * cover a handful of codepoints, not a text face, so offering them as an app
 * font would be wrong (selecting one would leave the whole UI without letters).
 * They are used only where [MarkdownView] draws text.
 *
 * The emoji come from two bundled fonts, in this order:
 *
 *  1. the COLOUR face, `fonts/TwemojiColorEmoji.ttf` (Twemoji Mozilla, COLRv0
 *     layers over the Twemoji artwork). Measured with the Skiko rasteriser this
 *     app renders through: it paints every emoji listed for it in
 *     [EmojiCoverage.colorEmoji], colour included. The Noto Color Emoji subset
 *     that sat here before carried a COLR table this renderer painted as
 *     nothing at all, so its emoji came out blank, and the code then routed the
 *     same codepoints to the monochrome font to make them visible again. A full
 *     Noto Color Emoji was measured too: its COLRv1 glyphs are not painted by
 *     this renderer either, which is why the bundled colour face is COLRv0.
 *  2. the MONOCHROME face, `fonts/NotoEmoji.ttf` (SIL OFL 1.1), for every
 *     codepoint the colour face does not have. A live changelog can use an emoji
 *     from a newer Unicode version than the bundled colour font, and a
 *     monochrome glyph is still a glyph: it is never allowed to fall through to
 *     a face without it.
 *
 * [EmojiCoverage] lists what each bundled font carries, read from the fonts'
 * own `cmap` by `scripts/build_desktop_emoji_coverage.py`, so a character is
 * only ever asked of a family that can draw it. The OS emoji / symbol / CJK
 * fonts stay as the last resort for everything the bundled pair does not have,
 * including the codepoints newer than both; a missing OS file simply means no
 * fallback for that class of character (the renderer then behaves exactly as it
 * did before).
 */
internal object MarkdownFonts {

    /**
     * The bundled COLOUR emoji family. Loaded from the classpath, so it is
     * always present in the packaged app; `runCatching` keeps a missing or
     * unreadable resource from taking the changelog down with it (the
     * monochrome family below then takes over).
     */
    private val colorEmoji: FontFamily? by lazy {
        runCatching {
            FontFamily(
                Font("fonts/TwemojiColorEmoji.ttf", FontWeight.Normal),
                Font("fonts/TwemojiColorEmoji.ttf", FontWeight.Medium),
                Font("fonts/TwemojiColorEmoji.ttf", FontWeight.Bold),
            )
        }.getOrNull()
    }

    /**
     * The bundled monochrome emoji family, the fallback for every codepoint the
     * colour face does not have.
     */
    private val monochromeEmoji: FontFamily? by lazy {
        runCatching {
            FontFamily(
                Font("fonts/NotoEmoji.ttf", FontWeight.Normal),
                Font("fonts/NotoEmoji.ttf", FontWeight.Medium),
                Font("fonts/NotoEmoji.ttf", FontWeight.Bold),
            )
        }.getOrNull()
    }

    private val emojiPaths: List<String>
    private val symbolPaths: List<String>
    private val cjkPaths: List<String>

    init {
        val windowsFonts = "${System.getenv("WINDIR") ?: "C:\\Windows"}\\Fonts"
        val home = System.getProperty("user.home")
        when (Platform.os) {
            DesktopOs.WINDOWS -> {
                emojiPaths = listOf("$windowsFonts\\seguiemj.ttf")
                symbolPaths = listOf("$windowsFonts\\seguisym.ttf")
                cjkPaths = listOf(
                    "$windowsFonts\\YuGothM.ttc",
                    "$windowsFonts\\meiryo.ttc",
                    "$windowsFonts\\msgothic.ttc",
                    "$windowsFonts\\msyh.ttc",
                )
            }

            DesktopOs.MACOS -> {
                emojiPaths = listOf(
                    "/System/Library/Fonts/Apple Color Emoji.ttc",
                    "/System/Library/Fonts/AppleColorEmoji.ttf",
                )
                symbolPaths = listOf(
                    "/System/Library/Fonts/Apple Symbols.ttf",
                    "/System/Library/Fonts/Symbol.ttf",
                )
                cjkPaths = listOf(
                    "/System/Library/Fonts/PingFang.ttc",
                    "/System/Library/Fonts/Hiragino Sans GB.ttc",
                )
            }

            DesktopOs.LINUX -> {
                emojiPaths = listOf(
                    "/usr/share/fonts/truetype/noto/NotoColorEmoji.ttf",
                    "/usr/share/fonts/noto/NotoColorEmoji.ttf",
                    "/usr/share/fonts/truetype/noto/NotoEmoji-Regular.ttf",
                    "$home/.local/share/fonts/NotoColorEmoji.ttf",
                    "$home/.fonts/NotoColorEmoji.ttf",
                )
                symbolPaths = listOf(
                    "/usr/share/fonts/truetype/noto/NotoSansSymbols-Regular.ttf",
                    "/usr/share/fonts/truetype/noto/NotoSansSymbols2-Regular.ttf",
                    "/usr/share/fonts/truetype/noto/NotoSansMath-Regular.ttf",
                )
                cjkPaths = listOf(
                    "/usr/share/fonts/opentype/noto/NotoSansCJK-Regular.ttc",
                    "/usr/share/fonts/truetype/noto/NotoSansCJK-Regular.ttc",
                )
            }
        }
    }

    private val emoji: FontFamily? by lazy { load(emojiPaths) }
    private val symbols: FontFamily? by lazy { load(symbolPaths) }
    private val cjk: FontFamily? by lazy { load(cjkPaths) }

    /**
     * The family that can draw [codePoint], or null when nothing is loaded for
     * it. Both bundled emoji fonts are consulted by their real coverage
     * ([EmojiCoverage]) rather than by a codepoint range, so a character is
     * never asked of a font that does not carry it: that is what produced the
     * tofu box, and it is the reason the emoji ranges note the bundled fonts
     * BEFORE the OS symbol font.
     */
    fun familyFor(codePoint: Int): FontFamily? {
        // Everything below '©' is ordinary text: letters, digits, the space,
        // '#', '*', punctuation. The bundled emoji fonts also map some of those
        // (as keycap bases), and routing them to an emoji family drew them
        // inside its wide cell: every digit of `1.54.11`, every `#97` and,
        // through the space, every word gap in the changelog came out
        // letter-spaced. They are always the app font's business, so nothing at
        // or below ASCII is ever handed to a fallback.
        if (codePoint < 0xA9) return null
        return when {
            // Variation selectors have no glyph of their own.
            codePoint in 0xFE00..0xFE0F -> null
            // Colour first: the bundled COLRv0 face paints these, so a heading
            // emoji comes out in colour instead of monochrome.
            EmojiCoverage.colorEmoji.contains(codePoint) ->
                colorEmoji ?: monochromeEmoji ?: emoji ?: symbols
            // Then the monochrome face, for everything the colour one lacks.
            EmojiCoverage.monochromeEmoji.contains(codePoint) ->
                monochromeEmoji ?: emoji ?: symbols
            isCjk(codePoint) -> cjk
            isSymbol(codePoint) -> symbols ?: monochromeEmoji ?: emoji
            // An emoji from neither bundled font (a live changelog newer than
            // the shipped fonts): the OS emoji face has the widest coverage, so
            // it is asked before the symbol font, which would answer with its
            // own notdef for an emoji it does not have.
            isEmoji(codePoint) -> emoji ?: monochromeEmoji ?: symbols
            else -> null
        }
    }

    /**
     * Loads the first existing file of [paths] as a three-weight family, so a
     * bold heading resolves to the fallback too instead of dropping back to the
     * app font (and its missing glyph).
     */
    private fun load(paths: List<String>): FontFamily? {
        val file = paths.map(::File).firstOrNull { it.isFile && it.length() > 0 } ?: return null
        return runCatching {
            FontFamily(
                Font(file, FontWeight.Normal),
                Font(file, FontWeight.Medium),
                Font(file, FontWeight.Bold),
            )
        }.getOrNull()
    }

    private fun isEmoji(cp: Int): Boolean =
        cp in 0x1F000..0x1FAFF || // emoji, pictographs, emoticons, symbols
            cp in 0x2600..0x27BF || // misc symbols + dingbats
            cp in 0x2B00..0x2BFF || // arrows/stars with emoji presentation
            cp in 0x25A0..0x25FF || // geometric shapes (▶ ...)
            cp == 0xFE0F // variation selector-16

    private fun isCjk(cp: Int): Boolean =
        cp in 0x2E80..0x2FFF || // radicals, Kangxi
            cp in 0x3000..0x30FF || // CJK punctuation, kana
            cp in 0x3100..0x31FF || // Bopomofo
            cp in 0x3200..0x33FF || // enclosed CJK, compatibility
            cp in 0x3400..0x4DBF ||
            cp in 0x4E00..0x9FFF ||
            cp in 0xF900..0xFAFF ||
            cp in 0xFE30..0xFE4F || // CJK compatibility forms
            cp in 0xFF00..0xFFEF || // fullwidth / halfwidth forms
            cp in 0x20000..0x3FFFF

    private fun isSymbol(cp: Int): Boolean =
        cp in 0x2190..0x21FF || // arrows
            cp in 0x2200..0x22FF || // mathematical operators
            cp in 0x2300..0x23FF || // misc technical
            cp in 0x2460..0x24FF || // enclosed alphanumerics
            cp in 0x2500..0x257F || // box drawing
            cp in 0x2580..0x259F || // block elements
            cp in 0x27C0..0x27EF || // misc math
            cp in 0x2800..0x28FF || // braille
            cp in 0x2900..0x2AFF || // supplemental arrows / math
            cp in 0x2E00..0x2E7F || // supplemental punctuation
            cp in 0x1D400..0x1D7FF || // mathematical alphanumeric symbols
            cp in 0xFE00..0xFE0F
}
