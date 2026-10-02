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
 * (`✨ Added`, `🐛 Fixed`, ...) or a stray symbol in a bullet (`→`, `⋮`, `⠿`)
 * has no glyph there, and Compose Desktop does not fall back to a second font on
 * its own: the character comes out as the "tofu" box the changelog was reported
 * to show. This resolves those characters to the operating system's own emoji /
 * symbol / CJK fonts and hands the renderer a single-character span for each.
 *
 * It is deliberately NOT part of `AppFonts` / the font picker: these families
 * cover a handful of codepoints, not a text face, so offering them as an app
 * font would be wrong (and selecting one would leave the whole UI without
 * letters). They are used only where [MarkdownView] draws text.
 *
 * Emoji are drawn from a font bundled with the app (`fonts/NotoEmoji.ttf`, the
 * monochrome Noto Emoji, SIL OFL 1.1), NOT from the OS font: Windows' Segoe UI
 * Emoji is a colour font (COLR/CBDT) and the renderer drew the outline-poor
 * colour glyphs as tofu, while the monochrome font draws every codepoint as a
 * plain outline the renderer can always paint. The OS emoji font stays only as a
 * fallback for the very few emoji Noto Emoji omits. The symbol/CJK files are
 * read from the OS at startup; a missing file simply means no fallback for that
 * class of character (the renderer then behaves exactly as it did before).
 */
internal object MarkdownFonts {

    /**
     * The bundled monochrome emoji family. Loaded from the classpath, so it is
     * always present in the packaged app; `runCatching` keeps a missing or
     * unreadable resource from taking the changelog down with it.
     */
    private val bundledEmoji: FontFamily? by lazy {
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

    /** The family that can draw [codePoint], or null when nothing is loaded for it. */
    fun familyFor(codePoint: Int): FontFamily? = when {
        isEmoji(codePoint) -> bundledEmoji ?: emoji ?: symbols
        isCjk(codePoint) -> cjk
        isSymbol(codePoint) -> symbols ?: bundledEmoji ?: emoji
        else -> null
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
