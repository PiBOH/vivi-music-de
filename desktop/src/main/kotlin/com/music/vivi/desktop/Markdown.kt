package com.music.vivi.desktop

import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.PointerIcon
import androidx.compose.ui.input.pointer.pointerHoverIcon
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * A small, dependency-free Markdown renderer for the changelog and the release
 * notes.
 *
 * The release body GitHub publishes (and `CHANGELOG.md`, which feeds it) is real
 * Markdown: headings, bold/italic, inline and fenced code, links, issue
 * references, bullet and numbered lists (nested), block quotes, tables,
 * horizontal rules and emoji. The changelog used to be shown by stripping the
 * few markers it understood, which read poorly and dropped everything else; this
 * renders the whole document instead.
 *
 * It is deliberately a renderer, not a spec-complete parser: it covers the
 * constructs these documents actually use, never crashes on anything else (an
 * unknown line is just a paragraph), and pulls in no new dependency.
 */

/** One parsed Markdown block. Nested lists are flattened to a depth-tagged row. */
internal sealed interface MdBlock {
    data class Heading(val level: Int, val text: String) : MdBlock
    data class Paragraph(val text: String) : MdBlock
    data class ListRow(val bullet: Boolean, val marker: String, val depth: Int, val text: String) : MdBlock
    data class Quote(val text: String) : MdBlock
    data class Code(val code: String) : MdBlock
    data class Table(val headers: List<String>, val rows: List<List<String>>) : MdBlock
    data object Divider : MdBlock
}

internal object MarkdownParser {
    private val headingRegex = Regex("""^(#{1,6})\s+(.*)$""")
    private val bulletRegex = Regex("""^(\s*)([-*+])\s+(.*)$""")
    private val orderedRegex = Regex("""^(\s*)(\d+)[.)]\s+(.*)$""")
    private val dividerRegex = Regex("""^\s*(?:-{3,}|\*{3,}|_{3,})\s*$""")
    private val tableSeparatorRegex = Regex("""^\s*\|?\s*:?-{1,}:?\s*(?:\|\s*:?-{1,}:?\s*)*\|?\s*$""")

    fun parse(markdown: String): List<MdBlock> {
        val blocks = mutableListOf<MdBlock>()
        val lines = markdown.replace("\r\n", "\n").replace('\r', '\n').lines()
        var i = 0
        while (i < lines.size) {
            val line = lines[i]
            val trimmed = line.trim()

            // Fenced code.
            if (trimmed.startsWith("```")) {
                val code = StringBuilder()
                i++
                while (i < lines.size && !lines[i].trim().startsWith("```")) {
                    code.appendLine(lines[i])
                    i++
                }
                if (i < lines.size) i++ // closing fence
                blocks.add(MdBlock.Code(code.toString().trimEnd('\n')))
                continue
            }

            if (trimmed.isEmpty()) {
                i++
                continue
            }

            // Table: a header row followed by a separator row.
            if (trimmed.startsWith("|") && i + 1 < lines.size && tableSeparatorRegex.matches(lines[i + 1])) {
                val headers = splitTableRow(trimmed)
                val rows = mutableListOf<List<String>>()
                i += 2
                while (i < lines.size && lines[i].trim().startsWith("|")) {
                    rows.add(splitTableRow(lines[i].trim()))
                    i++
                }
                blocks.add(MdBlock.Table(headers, rows))
                continue
            }

            headingRegex.matchEntire(trimmed)?.let { m ->
                blocks.add(MdBlock.Heading(m.groupValues[1].length, m.groupValues[2].trim()))
                i++
                continue
            }

            if (dividerRegex.matches(trimmed)) {
                blocks.add(MdBlock.Divider)
                i++
                continue
            }

            // `ListRow.bullet` means "this is a bullet list", so it is true for
            // `-`/`*`/`+` and false for a numbered item. It used to be the other
            // way round, which made every `-` row render as the literal "-." and
            // every numbered row render as a bullet.
            bulletRegex.matchEntire(line)?.let { m ->
                blocks.add(MdBlock.ListRow(true, "-", indentDepth(m.groupValues[1]), m.groupValues[3].trim()))
                i++
                continue
            }

            orderedRegex.matchEntire(line)?.let { m ->
                blocks.add(MdBlock.ListRow(false, m.groupValues[2], indentDepth(m.groupValues[1]), m.groupValues[3].trim()))
                i++
                continue
            }

            if (trimmed.startsWith(">")) {
                val quote = StringBuilder()
                while (i < lines.size && lines[i].trim().startsWith(">")) {
                    if (quote.isNotEmpty()) quote.append(' ')
                    quote.append(lines[i].trim().removePrefix(">").trim())
                    i++
                }
                blocks.add(MdBlock.Quote(quote.toString()))
                continue
            }

            // Paragraph: consecutive plain lines, joined with a space.
            val paragraph = StringBuilder()
            while (i < lines.size) {
                val l = lines[i]
                val t = l.trim()
                if (t.isEmpty() || t.startsWith("```") || t.startsWith("|") || t.startsWith(">") ||
                    headingRegex.matches(t) || bulletRegex.matches(l) || orderedRegex.matches(l) ||
                    dividerRegex.matches(t)
                ) {
                    break
                }
                if (paragraph.isNotEmpty()) paragraph.append(' ')
                paragraph.append(t)
                i++
            }
            if (paragraph.isNotEmpty()) blocks.add(MdBlock.Paragraph(paragraph.toString()))
        }
        return blocks
    }

    /** Two spaces of indentation is one nesting level. */
    private fun indentDepth(leading: String): Int = (leading.replace("\t", "  ").length / 2).coerceAtLeast(0)

    private fun splitTableRow(row: String): List<String> {
        var s = row.trim()
        if (s.startsWith("|")) s = s.substring(1)
        if (s.endsWith("|")) s = s.dropLast(1)
        return s.split('|').map { it.trim() }
    }
}

/**
 * Renders [markdown] as Compose content.
 *
 * Inline links (`[text](url)`, bare `http(s)://…` and `#N` issue references) are
 * clickable and handed to [onOpenUrl]; the rest of the text stays selectable.
 */
@Composable
internal fun MarkdownView(
    markdown: String,
    modifier: Modifier = Modifier,
    onOpenUrl: (String) -> Unit,
) {
    val blocks = remember(markdown) { MarkdownParser.parse(markdown) }
    val issueBase = remember { "https://github.com/${UpdateSource.repo()}/issues/" }

    Column(modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(6.dp)) {
        blocks.forEach { block ->
            when (block) {
                is MdBlock.Heading -> MarkdownHeading(block)
                is MdBlock.Paragraph -> MarkdownText(
                    text = block.text,
                    style = MaterialTheme.typography.bodyMedium,
                    issueBase = issueBase,
                    onOpenUrl = onOpenUrl,
                )
                is MdBlock.ListRow -> Row(
                    Modifier
                        .fillMaxWidth()
                        .padding(start = (block.depth * 16).dp),
                ) {
                    Text(
                        if (block.bullet) "\u2022" else "${block.marker}.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(end = 8.dp),
                    )
                    MarkdownText(
                        text = block.text,
                        style = MaterialTheme.typography.bodyMedium,
                        issueBase = issueBase,
                        onOpenUrl = onOpenUrl,
                        modifier = Modifier.weight(1f),
                    )
                }
                is MdBlock.Quote -> Box(
                    Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(4.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant)
                        .padding(8.dp),
                ) {
                    MarkdownText(
                        text = block.text,
                        style = MaterialTheme.typography.bodyMedium,
                        issueBase = issueBase,
                        onOpenUrl = onOpenUrl,
                    )
                }
                is MdBlock.Code -> Box(
                    Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(6.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant)
                        .padding(10.dp),
                ) {
                    SelectionContainer {
                        Text(
                            block.code,
                            style = MaterialTheme.typography.bodySmall.copy(fontFamily = FontFamily.Monospace),
                            modifier = Modifier.horizontalScroll(rememberScrollState()),
                        )
                    }
                }
                is MdBlock.Table -> MarkdownTable(block, issueBase, onOpenUrl)
                MdBlock.Divider -> HorizontalDivider(Modifier.padding(vertical = 4.dp))
            }
        }
    }
}

@Composable
private fun MarkdownHeading(block: MdBlock.Heading) {
    val style = when (block.level) {
        1 -> MaterialTheme.typography.headlineSmall
        2 -> MaterialTheme.typography.titleLarge
        3 -> MaterialTheme.typography.titleMedium
        else -> MaterialTheme.typography.titleSmall
    }
    Text(
        markdownInline(block.text, issueBase = "", onOpenUrl = {}, linkColor = MaterialTheme.colorScheme.primary),
        style = style.copy(fontWeight = FontWeight.Bold),
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.padding(top = 4.dp),
    )
}

@Composable
private fun MarkdownTable(block: MdBlock.Table, issueBase: String, onOpenUrl: (String) -> Unit) {
    Column(Modifier.fillMaxWidth()) {
        HorizontalDivider()
        Row(Modifier.fillMaxWidth().padding(vertical = 6.dp)) {
            block.headers.forEachIndexed { index, header ->
                MarkdownText(
                    text = header,
                    style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold),
                    issueBase = issueBase,
                    onOpenUrl = onOpenUrl,
                    modifier = Modifier.weight(1f).padding(end = 6.dp),
                )
                if (index < block.headers.size - 1) Spacer(Modifier.width(6.dp))
            }
        }
        HorizontalDivider()
        block.rows.forEach { row ->
            Row(Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
                row.forEachIndexed { index, cell ->
                    MarkdownText(
                        text = cell,
                        style = MaterialTheme.typography.bodySmall,
                        issueBase = issueBase,
                        onOpenUrl = onOpenUrl,
                        modifier = Modifier.weight(1f).padding(end = 6.dp),
                    )
                    if (index < row.size - 1) Spacer(Modifier.width(6.dp))
                }
            }
            HorizontalDivider()
        }
    }
}

/** Renders one inline-formatted line: clickable links when present, selectable otherwise. */
@Composable
private fun MarkdownText(
    text: String,
    style: TextStyle,
    issueBase: String,
    onOpenUrl: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val linkColor = MaterialTheme.colorScheme.primary
    val annotated = remember(text, issueBase, linkColor) { markdownInline(text, issueBase, onOpenUrl, linkColor) }
    val hasLinks = remember(annotated) {
        annotated.getStringAnnotations("URL", 0, annotated.length).isNotEmpty()
    }
    if (hasLinks) {
        // The pointing hand belongs to the link, not to the line: the hand used
        // to be set for the whole clickable line, so a paragraph that merely
        // contained a link showed it over its entire surface. The layout result
        // is what maps the pointer back to a text offset, so the hand appears
        // only while the pointer is inside a `URL` annotation and the rest of
        // the line keeps the ordinary arrow.
        var layout by remember { mutableStateOf<TextLayoutResult?>(null) }
        var overLink by remember { mutableStateOf(false) }
        BasicText(
            text = annotated,
            style = style.copy(color = MaterialTheme.colorScheme.onSurface),
            onTextLayout = { layout = it },
            modifier = modifier
                .pointerHoverIcon(if (overLink) PointerIcon.Hand else PointerIcon.Default)
                .pointerInput(annotated) {
                    awaitPointerEventScope {
                        while (true) {
                            val position = awaitPointerEvent().changes.firstOrNull()?.position
                            val result = layout
                            val next = if (position != null && result != null) {
                                val offset = result.getOffsetForPosition(position)
                                annotated.getStringAnnotations(URL_TAG, offset, offset).isNotEmpty()
                            } else {
                                false
                            }
                            if (next != overLink) overLink = next
                        }
                    }
                }
                .pointerInput(annotated) {
                    detectTapGestures { position ->
                        val result = layout ?: return@detectTapGestures
                        val offset = result.getOffsetForPosition(position)
                        annotated.getStringAnnotations(URL_TAG, offset, offset)
                            .firstOrNull()
                            ?.let { onOpenUrl(it.item) }
                    }
                },
        )
    } else {
        SelectionContainer {
            Text(
                text = annotated,
                style = style.copy(color = MaterialTheme.colorScheme.onSurface),
                modifier = modifier,
            )
        }
    }
}

private const val URL_TAG = "URL"

/**
 * Parses the inline Markdown of one line/paragraph into an [AnnotatedString] of
 * [SpanStyle]s and `URL`-tagged link annotations.
 *
 * Supported: `` `code` ``, `**bold**`, `__bold__`, `*italic*`, `_italic_`,
 * `~~strike~~`, `[text](url)`, `![alt](url)` (shown as its alt text), bare
 * `http(s)://` URLs and `#N` issue references. Markers can nest (bold around
 * code, a link inside italic, ...). Links are underlined and coloured with
 * [linkColor].
 */
internal fun markdownInline(
    text: String,
    issueBase: String,
    onOpenUrl: (String) -> Unit,
    linkColor: Color = Color.Unspecified,
): AnnotatedString {
    val base = buildAnnotatedString { appendInline(text, SpanStyle(), issueBase, linkColor) }
    return applyFontFallback(base)
}

/**
 * Points every character the app's own font cannot draw at the operating
 * system font that can (see [MarkdownFonts]): the emoji in a section heading, a
 * rare symbol in a bullet (`→`, `⋮`, `⠿`), a CJK or fullwidth character. Without
 * this those codepoints are laid out as the "tofu" box the changelog was
 * reported to show, because Compose Desktop does not fall back to a second font
 * on its own. Characters the app font covers get no span and keep their style.
 */
private fun applyFontFallback(annotated: AnnotatedString): AnnotatedString {
    val source = annotated.text
    var probe = 0
    var needed = false
    while (probe < source.length) {
        val codePoint = source.codePointAt(probe)
        if (MarkdownFonts.familyFor(codePoint) != null) {
            needed = true
            break
        }
        probe += Character.charCount(codePoint)
    }
    if (!needed) return annotated
    return buildAnnotatedString {
        append(annotated)
        var index = 0
        while (index < source.length) {
            val codePoint = source.codePointAt(index)
            val count = Character.charCount(codePoint)
            MarkdownFonts.familyFor(codePoint)?.let { family ->
                addStyle(SpanStyle(fontFamily = family), index, index + count)
            }
            index += count
        }
    }
}

private fun AnnotatedString.Builder.appendInline(
    text: String,
    style: SpanStyle,
    issueBase: String,
    linkColor: Color,
) {
    val linkStyle = style.copy(color = linkColor, textDecoration = TextDecoration.Underline)
    var i = 0
    val n = text.length
    while (i < n) {
        // Inline code.
        if (text[i] == '`') {
            val end = text.indexOf('`', i + 1)
            if (end > i) {
                withStyle(style.copy(fontFamily = FontFamily.Monospace)) {
                    append(text.substring(i + 1, end))
                }
                i = end + 1
                continue
            }
        }
        // Bold.
        if (i + 1 < n && (text.startsWith("**", i) || text.startsWith("__", i))) {
            val marker = text.substring(i, i + 2)
            val end = text.indexOf(marker, i + 2)
            if (end > i + 1) {
                val boldStyle = style.copy(fontWeight = FontWeight.Bold)
                withStyle(boldStyle) {
                    appendInline(text.substring(i + 2, end), boldStyle, issueBase, linkColor)
                }
                i = end + 2
                continue
            }
        }
        // Strikethrough.
        if (i + 1 < n && text.startsWith("~~", i)) {
            val end = text.indexOf("~~", i + 2)
            if (end > i + 1) {
                val strikeStyle = style.copy(textDecoration = TextDecoration.LineThrough)
                withStyle(strikeStyle) {
                    appendInline(text.substring(i + 2, end), strikeStyle, issueBase, linkColor)
                }
                i = end + 2
                continue
            }
        }
        // Italic (single marker). A "_" inside a word is left alone.
        if (text[i] == '*' || text[i] == '_') {
            val single = text[i]
            val prevWord = i > 0 && text[i - 1].isLetterOrDigit()
            if (!(single == '_' && prevWord)) {
                val end = text.indexOf(single, i + 1)
                if (end > i + 1) {
                    val italicStyle = style.copy(fontStyle = FontStyle.Italic)
                    withStyle(italicStyle) {
                        appendInline(text.substring(i + 1, end), italicStyle, issueBase, linkColor)
                    }
                    i = end + 1
                    continue
                }
            }
        }
        // Link or image: ![alt](url) / [text](url)
        val imagePrefix = text.startsWith("![", i)
        val linkPrefix = text.startsWith("[", i)
        if (imagePrefix || linkPrefix) {
            val textStart = if (imagePrefix) i + 2 else i + 1
            val closeBracket = text.indexOf(']', textStart)
            if (closeBracket > 0 && closeBracket + 1 < n && text[closeBracket + 1] == '(') {
                val closeParen = text.indexOf(')', closeBracket + 2)
                if (closeParen > closeBracket) {
                    val label = text.substring(textStart, closeBracket)
                    val url = text.substring(closeBracket + 2, closeParen).trim().substringBefore(' ')
                    val start = length
                    withStyle(if (imagePrefix) style else linkStyle) {
                        appendInline(label, if (imagePrefix) style else linkStyle, issueBase, linkColor)
                    }
                    if (url.isNotEmpty()) {
                        addStringAnnotation(URL_TAG, url, start, length)
                    }
                    i = closeParen + 1
                    continue
                }
            }
        }
        // Bare URL.
        if (text.startsWith("http://", i) || text.startsWith("https://", i)) {
            var end = i
            while (end < n && !text[end].isWhitespace() && text[end] != ')' && text[end] != ']') end++
            val url = text.substring(i, end)
            val start = length
            withStyle(linkStyle) { append(url) }
            addStringAnnotation(URL_TAG, url, start, length)
            i = end
            continue
        }
        // Issue reference #N (not part of a word and not an anchor link).
        if (text[i] == '#' && (i == 0 || !text[i - 1].isLetterOrDigit())) {
            var end = i + 1
            while (end < n && text[end].isDigit()) end++
            if (end > i + 1) {
                val number = text.substring(i + 1, end)
                val start = length
                withStyle(style.copy(color = linkColor, fontWeight = FontWeight.Bold)) {
                    append("#$number")
                }
                if (issueBase.isNotEmpty()) {
                    addStringAnnotation(URL_TAG, issueBase + number, start, length)
                }
                i = end
                continue
            }
        }
        // Plain character.
        withStyle(style) { append(text[i]) }
        i++
    }
}
