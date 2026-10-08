# Fonts bundled with VIVI Music DE (desktop)

Every file here is loaded from the classpath, never from the internet. This note
exists so the origin and the licence of each one is written down next to it.

| File | What it is | Licence |
| --- | --- | --- |
| `google_sans_flex.ttf` | Google Sans Flex, the default app font | SIL OFL 1.1 |
| `sans_flex.ttf` | Google Sans Flex, second selectable weight set | SIL OFL 1.1 |
| `outfit.ttf` | Outfit | SIL OFL 1.1 |
| `plus_jakarta_sans.ttf` | Plus Jakarta Sans | SIL OFL 1.1 |
| `NotoEmoji.ttf` | Noto Emoji, monochrome | SIL OFL 1.1 |
| `TwemojiColorEmoji.ttf` | Twemoji Mozilla, colour (COLRv0) | graphics CC-BY 4.0 (Twemoji, Twitter/X), font build MIT (twemoji-colr) |

## The two emoji fonts

They are the Markdown renderer's glyph fallback (`MarkdownFonts`): the app font
carries no emoji, and Compose Desktop does not fall back to a second font on its
own, so without them a changelog heading such as `🐛 Fixed` came out as the app
font's `.notdef` box.

* `TwemojiColorEmoji.ttf` is the COLOUR face. It is Twemoji Mozilla v0.7.0,
  downloaded from the twemoji-colr release page:
  `https://github.com/mozilla/twemoji-colr/releases/download/v0.7.0/Twemoji.Mozilla.ttf`
  Its emoji are COLRv0 layers over the Twemoji artwork from
  `https://github.com/jdecked/twemoji`.
* `NotoEmoji.ttf` is the MONOCHROME face, the fallback for codepoints the colour
  face does not carry, from `https://github.com/google/fonts` (`ofl/notoemoji`).

Both are chosen by measurement, not by reputation, because "it is a colour font"
is not enough: the renderer has to actually paint it. `scripts/build_desktop_emoji_coverage.py`
reads both `cmap` tables into `EmojiCoverage.kt`, and the colour face's glyphs
were rasterised with the same Skiko version the app ships before it was trusted.

Two fonts were measured and REJECTED for this slot, so nobody tries them again:

* the previous `NotoColorEmoji.ttf`, a 165 KB COLR/CPAL subset: every listed
  codepoint had a glyph and none of them painted a single pixel;
* the full `NotoColorEmoji-Regular.ttf` from Google Fonts (25 MB, COLRv1): the
  glyphs are there and the renderer paints nothing for them either.
  COLRv0 is what this Compose/Skiko stack paints.

## Regenerating the coverage list

After replacing either emoji font, run

```
python3 scripts/build_desktop_emoji_coverage.py
```

and commit the regenerated `desktop/src/main/kotlin/com/music/vivi/desktop/EmojiCoverage.kt`.
`python3 scripts/build_desktop_emoji_coverage.py --check` verifies (exit 1) that
the committed list still matches the bundled fonts.
