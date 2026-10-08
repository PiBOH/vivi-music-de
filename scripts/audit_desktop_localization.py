#!/usr/bin/env python3
"""
Audit the generated desktop string table (`Localization.kt` + `Localization_<lang>.kt`).

Run from the repo root:  python3 scripts/audit_desktop_localization.py

Why this exists — `Localization.get` resolves a key in three steps:

    1. the language's own map;
    2. the English map;
    3. "the first map that has the key", as a last resort so a raw snake_case
       key never reaches the screen.

Step 3 is where a *translation* bug hides: if a key is missing from English but
present in some other language, an English build shows that other language. That
is how the Alpha lyrics style came out in Arabic letters, and how a key that only
the extras batches defined (never the Android resources) could show up in any
language but English. Both classes are reported here as errors.

The audit fails (exit 1) on

  * a key the UI asks for that no map defines (shows the raw key), and
  * a key missing from a language map, or present in one but absent from
    English (the step-3 leak).

It only *reports*, without failing, the two remaining "not translated yet"
classes, because they are the mobile resources' own gaps rather than a bug:

  * English text left in a language map (the resource was never translated
    upstream, or the key is desktop-only and no batch has covered it yet);
  * brand/technical strings that are meant to read the same everywhere.

`BRAND_KEYS` and `TECHNICAL_KEYS` below are that documented exception list.
"""

import os
import re
import sys

sys.stdout.reconfigure(encoding="utf-8", errors="replace")

REPO = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
DESKTOP = os.path.join(
    REPO, "desktop", "src", "main", "kotlin", "com", "music", "vivi", "desktop"
)

# Strings that stay in Latin / stay identical in every language on purpose.
BRAND_KEYS = {
    "lyrics_style_apple", "lyrics_style_apple_v2", "lyrics_style_vivimusic",
    "lyrics_style_metro", "lyrics_style_lyrics_v2", "lyrics_style_alpha",
    "lyrics_style_karaoke", "apple_mini_player", "mini_player_apple",
    "player_background_apple", "canvas_source_tidal", "canvas_source_vivimusic",
    "canvas_source_apple_music", "vivi_equalizer", "lastfm", "discord",
    "de", "mobile", "website", "telegram_channel", "wrapped_title",
    "lyrics_style_none",
    # Discord's own feature name, kept as-is upstream.
    "discord_presence",
    # The first-run screen's two support links: UPI is the payment rail's own
    # name and Buy Me a Coffee is the service's, so both read the same in every
    # language. Supplied by desktop_extra_translations_94.
    "support_upi", "support_buy_me_a_coffee",
    # The window/app title; "desktop" is the same loanword everywhere it is
    # not already translated, and the product name never changes.
    "header",
}
# Device/technical readouts, font names and unit labels: English by design.
TECHNICAL_KEYS = {
    "gpu", "cpu", "heap", "memory", "threads", "process", "uptime", "status",
    "radix", "arch", "distribution",
    "density_55", "density_65", "density_75", "density_85", "density_100",
    "font_google_sans", "font_sans_flex", "font_outfit", "font_plus_jakarta_sans",
    "sort_az", "sort_za", "ok", "preview_text_quote",
    "ai_api_key", "ai_base_url", "ai_deepl_formality_default",
}

# Letters that belong to one language's alphabet and to no other in the list,
# with every language that legitimately uses them. A value in language X that
# carries a letter whose set does not contain X is a translation pasted under the
# wrong language tag: this is the "a language showed up in place of another"
# case that the script check cannot see (they are all Latin).
# Letters shared by several languages of the list (c, s, z with any accent,
# ae, oe, aa) are deliberately absent: they cannot tell two languages apart.
SIGNATURE_LETTERS = {}
for _chars, _langs in [
    ("\u00df", {"de"}),
    ("\u0105\u0119", {"pl", "lt"}),
    ("\u0142\u0144\u015b\u017a\u017c", {"pl"}),
    ("\u011f\u0131\u015f\u0219", {"tr", "az", "ro"}),
    ("\u0159\u011b\u016f", {"cs"}),
    ("\u013a\u013e\u0155", {"sk"}),
    ("\u0151\u0171", {"hu"}),
    ("\u0103\u021b", {"ro", "vi"}),
    ("\u0111", {"hr", "bs", "vi"}),
    ("\u0259", {"az"}),
    ("\u0117\u012f\u0173", {"lt"}),
    ("\u00f5", {"et", "pt", "vi"}),
    ("\u00f1", {"es", "eu", "fil"}),
    ("\u00e3", {"pt", "vi"}),
    ("\u0153\u00ff\u00fb", {"fr"}),
    ("\u01a1\u01b0\u1ea1\u1ec7\u1ed1\u1ea7\u1ebf\u1ed9\u1ee5\u1eed\u1eef\u1ea9\u1ead\u1eaf\u1eb1\u1eb7\u1eb5\u1ebb\u1ebd\u1eb9\u1ec9\u1ecb\u1ecd\u1ecf\u1ed7\u1ed5\u1edb\u1edd\u1ee3\u1edf\u1ee1\u1ef3\u1ef5\u1ef7\u1ef9", {"vi"}),
]:
    # Union, never assignment: several of these letters belong to more than one
    # language (\u00f5 to Estonian and Portuguese, \u0111 to Croatian, Bosnian and
    # Vietnamese), and overwriting would report one of them as "the other
    # language" in its own text.
    for _ch in _chars:
        SIGNATURE_LETTERS.setdefault(_ch, set()).update(_langs)

# Which scripts a language may use. A value written in a script the language
# does not use means a translation was pasted under the wrong language tag.
SCRIPT_RANGES = [
    ("ARABIC", 0x0600, 0x06FF), ("ARABIC", 0x0750, 0x077F), ("ARABIC", 0xFB50, 0xFDFF),
    ("HEBREW", 0x0590, 0x05FF),
    ("CYRILLIC", 0x0400, 0x04FF),
    ("GREEK", 0x0370, 0x03FF),
    ("INDIC", 0x0900, 0x097F), ("INDIC", 0x0980, 0x09FF), ("INDIC", 0x0A00, 0x0A7F),
    ("INDIC", 0x0A80, 0x0AFF), ("INDIC", 0x0B00, 0x0B7F), ("INDIC", 0x0B80, 0x0BFF),
    ("INDIC", 0x0C00, 0x0C7F), ("INDIC", 0x0C80, 0x0CFF),
    ("KHMER", 0x1780, 0x17FF), ("THAI", 0x0E00, 0x0E7F),
    ("JA", 0x3040, 0x30FF), ("JA", 0x31F0, 0x31FF),
    ("KO", 0x1100, 0x11FF), ("KO", 0xAC00, 0xD7AF),
    ("HAN", 0x3400, 0x4DBF), ("HAN", 0x4E00, 0x9FFF), ("HAN", 0xF900, 0xFAFF),
]
ONLY_SCRIPT = {
    "ar": "ARABIC", "fa": "ARABIC", "iw": "HEBREW",
    "ru": "CYRILLIC", "uk": "CYRILLIC", "be": "CYRILLIC", "bg": "CYRILLIC", "sr": "CYRILLIC",
    "el": "GREEK",
    "hi": "INDIC", "bn": "INDIC", "as": "INDIC", "pa": "INDIC", "ml": "INDIC",
    "ta": "INDIC", "te": "INDIC", "km": "KHMER", "th": "THAI",
    "zh-rCN": "HAN", "zh-rTW": "HAN",
}
# Languages that legitimately mix scripts (Japanese mixes kana with kanji,
# Korean is heading to hangul-only but still uses hanja).
MIXED_SCRIPT = {"ja": {"JA", "HAN"}, "ko": {"KO", "HAN"}}


def read(path):
    with open(path, encoding="utf-8") as f:
        return f.read()


def parse_tables():
    """{lang: {key: value}} from the generated per-language files.

    One `Localization_<lang>.kt` per language (the layout the APK uses with its
    `values-<lang>/strings.xml`), each exporting one `internal fun strings_<lang>()`.
    """
    funcs = {}
    for name in sorted(os.listdir(DESKTOP)):
        if not (name.startswith("Localization_") and name.endswith(".kt")):
            continue
        text = read(os.path.join(DESKTOP, name))
        for fn, body in re.findall(
            r"internal fun (strings_\w+)\(\): Map<String, String> =\s*mapOf\((.*?)\n    \)",
            text,
            re.S,
        ):
            funcs[fn] = dict(re.findall(r'"([^"]+)" to "([^"]*)"', body))
    localization = read(os.path.join(DESKTOP, "Localization.kt"))
    # The table may be emitted as `"it" to { strings_it() }` (the on-demand
    # shape: the map is built when the language is first read) or, from an older
    # run, as `"it" to strings_it()`. Both are accepted, so the audit keeps
    # seeing the languages instead of passing vacuously.
    lang_of = dict(
        re.findall(r'"([^"]+)" to \{?\s*(strings_\w+)\(\)', localization)
    )
    missing = [fn for fn in lang_of.values() if fn not in funcs]
    if missing:
        raise SystemExit(
            "Localization.kt references table functions that no file defines: %s"
            % ", ".join(missing)
        )
    return {lang: funcs[fn] for lang, fn in lang_of.items()}


def requested_keys():
    """Every key a screen passes to Localization.get.

    Calls whose key comes from a `when` are skipped as a whole: the literals in
    them are the *values* of the branch (`"fast" -> `), not keys, and every key
    such a call can resolve to is a literal somewhere else in the call sites.
    """
    keys = set()
    for name in sorted(os.listdir(DESKTOP)):
        if not name.endswith(".kt") or name.startswith("Localization"):
            continue
        text = read(os.path.join(DESKTOP, name))
        for m in re.finditer(r"Localization\.get\s*\(", text):
            i = m.end() - 1
            depth = 0
            while i < len(text):
                c = text[i]
                if c == "(":
                    depth += 1
                elif c == ")":
                    depth -= 1
                    if depth == 0:
                        break
                i += 1
            call = text[m.end() - 1:i]
            if "when" in call:
                continue
            for match in re.finditer(r'"([^"]+)"', call):
                lit = match.group(1)
                before = call[:match.start()].rstrip()
                # `if (mode == "native") "key_a" else "key_b"`: the compared
                # literal is a stored setting value, not a key.
                if before.endswith("==") or before.endswith("!="):
                    continue
                if re.fullmatch(r"[a-z][a-z0-9_]*", lit):
                    keys.add(lit)
    return keys


# The endonym each language code must be listed under in the picker. A code
# paired with another language's name is "a language in place of another" at the
# very top of the UI: the user picks "Polski" and gets Czech.
EXPECTED_ENDONYMS = {
    "en": "English", "az": "Az\u0259rbaycan dili", "bs": "Bosanski", "ca": "Catal\u00e0",
    "cs": "\u010ce\u0161tina", "de": "Deutsch", "et": "Eesti", "es": "Espa\u00f1ol",
    "eu": "Euskara", "fil": "Filipino", "fr": "Fran\u00e7ais", "hr": "Hrvatski",
    "id": "Bahasa Indonesia", "it": "Italiano", "lt": "Lietuvi\u0173", "hu": "Magyar",
    "ms": "Bahasa Melayu", "nl": "Nederlands", "nb": "Norsk bokm\u00e5l", "pl": "Polski",
    "pt": "Portugu\u00eas", "ro": "Rom\u00e2n\u0103", "sk": "Sloven\u010dina", "sl": "Sloven\u0161\u010dina",
    "sr": "\u0421\u0440\u043f\u0441\u043a\u0438", "fi": "Suomi", "sv": "Svenska", "vi": "Ti\u1ebfng Vi\u1ec7t",
    "tr": "T\u00fcrk\u00e7e", "el": "\u0395\u03bb\u03bb\u03b7\u03bd\u03b9\u03ba\u03ac", "be": "\u0411\u0435\u043b\u0430\u0440\u0443\u0441\u043a\u0430\u044f",
    "bg": "\u0411\u044a\u043b\u0433\u0430\u0440\u0441\u043a\u0438", "ru": "\u0420\u0443\u0441\u0441\u043a\u0438\u0439", "uk": "\u0423\u043a\u0440\u0430\u0457\u043d\u0441\u044c\u043a\u0430",
    "ar": "\u0627\u0644\u0639\u0631\u0628\u064a\u0629", "hi": "\u0939\u093f\u0928\u094d\u0926\u0940", "as": "\u0985\u09b8\u09ae\u09c0\u09af\u09bc\u09be",
    "bn": "\u09ac\u09be\u0982\u09b2\u09be", "pa": "\u0a2a\u0a70\u0a1c\u0a3e\u0a2c\u0a40", "ta": "\u0ba4\u0bae\u0bbf\u0bb4\u0bcd",
    "te": "\u0c24\u0c46\u0c32\u0c41\u0c17\u0c41", "ml": "\u0d2e\u0d32\u0d2f\u0d3e\u0d33\u0d02", "th": "\u0e44\u0e17\u0e22",
    "km": "\u1781\u17d2\u1798\u17c2\u179a", "ko": "\ud55c\uad6d\uc5b4", "zh-rCN": "\u7b80\u4f53\u4e2d\u6587",
    "zh-rTW": "\u7e41\u9ad4\u4e2d\u6587", "ja": "\u65e5\u672c\u8a9e",
}


def picker_mismatches():
    """AppLanguage(code, name) pairs whose name is not that language's own name."""
    text = read(os.path.join(DESKTOP, "Languages.kt"))
    out = []
    for code, name in re.findall(r'AppLanguage\("([^"]+)",\s*"([^"]+)"\)', text):
        expected = EXPECTED_ENDONYMS.get(code)
        if expected is None:
            out.append("Languages.kt: '%s' is not a known language code (name '%s')" % (code, name))
        elif expected != name:
            out.append("Languages.kt: '%s' is named '%s', expected '%s'" % (code, name, expected))
    for code in sorted(set(EXPECTED_ENDONYMS) - set(re.findall(r'AppLanguage\("([^"]+)"', text))):
        out.append("Languages.kt: no picker entry for '%s'" % code)
    return out


def table_mismatches(lang_of):
    """A language code served by another language's table."""
    out = []
    for lang, fn in sorted(lang_of.items()):
        expected = "strings_" + lang.replace("-", "_")
        if fn != expected:
            out.append("%s: served by %s(), expected %s()" % (lang, fn, expected))
    return out


def foreign_letters(lang, entries):
    """Values carrying a letter that belongs to some other language's alphabet."""
    out = []
    for key, value in entries.items():
        own = set()
        foreign = set()
        for ch in value:
            langs = SIGNATURE_LETTERS.get(ch)
            if not langs:
                continue
            if lang in langs:
                own |= langs
            else:
                foreign |= langs
        if foreign and not own and len(foreign) == 1:
            out.append("%s: %s -> %s" % (lang, key, value))
    return out


def scripts_in(value):
    found = set()
    for ch in value:
        for family, lo, hi in SCRIPT_RANGES:
            if lo <= ord(ch) <= hi:
                found.add(family)
    return found


def main():
    tables = parse_tables()
    en = tables["en"]
    errors = []
    wrong_script = []
    english_left = []
    misplaced = []
    # A language served by another language's table, and a picker entry named
    # after a different language: both are "a language in place of another"
    # before a single string is even rendered.
    lang_of = dict(re.findall(
        r'"([^"]+)" to \{?\s*(strings_\w+)\(\)',
        read(os.path.join(DESKTOP, "Localization.kt")),
    ))
    errors.extend(table_mismatches(lang_of))
    errors.extend(picker_mismatches())

    for key in sorted(requested_keys()):
        if key not in en:
            errors.append("key '%s' is asked for but no table defines it" % key)

    for lang, entries in sorted(tables.items()):
        if lang != "en":
            for key in sorted(set(en) - set(entries)):
                errors.append("%s: missing '%s' (would fall back)" % (lang, key))
            for key in sorted(set(entries) - set(en)):
                errors.append(
                    "%s: defines '%s' but English does not (English builds would show %s)"
                    % (lang, key, lang)
                )
        only = ONLY_SCRIPT.get(lang)
        mixed = MIXED_SCRIPT.get(lang)
        for key, value in entries.items():
            found = scripts_in(value)
            if not found:
                continue
            if only and found != {only}:
                wrong_script.append("%s: %s -> %s" % (lang, key, value))
            elif mixed and found - mixed:
                wrong_script.append("%s: %s -> %s" % (lang, key, value))
            elif not only and not mixed:
                wrong_script.append("%s: %s -> %s" % (lang, key, value))
        if lang == "en":
            continue
        for key, value in entries.items():
            if key in BRAND_KEYS or key in TECHNICAL_KEYS:
                continue
            if en.get(key) == value and len(value.split()) >= 2:
                english_left.append("%s: %s -> %s" % (lang, key, value))
        # Letters of another language, in this language's table.
        misplaced.extend(foreign_letters(lang, entries))

    print("Languages: %d   English keys: %d" % (len(tables) - 1, len(en)))
    print("Missing / leaking keys: %d" % len(errors))
    for line in errors[:40]:
        print("  " + line)
    print("Values in a script the language does not use: %d" % len(wrong_script))
    for line in wrong_script[:40]:
        print("  " + line)
    print("Values carrying another language's letters: %d" % len(misplaced))
    for line in misplaced[:60]:
        print("  " + line)
    print("English text left in a language map: %d" % len(english_left))
    print("  (brand and technical strings excluded; %d of them are brands)"% len(BRAND_KEYS))
    per_key = {}
    for line in english_left:
        per_key.setdefault(line.split(": ")[1], []).append(line.split(": ")[0])
    for key, langs in sorted(per_key.items(), key=lambda kv: -len(kv[1]))[:30]:
        print("    %-32s %2d language(s): %s" % (key, len(langs), ", ".join(langs[:6]) + ("…" if len(langs) > 6 else "")))
    return 1 if (errors or wrong_script or misplaced) else 0


if __name__ == "__main__":
    sys.exit(main())
