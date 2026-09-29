#!/usr/bin/env python3
"""Keep the Windows setup's welcome screen in the app's own words.

The installer asks for the language on its first screen (Inno Setup loads its
messages once at startup, so the wizard cannot be re-languaged while it runs —
the picker therefore re-runs Setup itself with `/LANG=`, and the page it lands
back on is rebuilt from the data below).

Because that page is rebuilt from scratch, its text cannot come from Inno's
message table alone: only 29 of the 51 languages this app supports ship a wizard
translation, so 22 of them would show English on the very screen that asks for a
language. The welcome title, the welcome description and the word "Language" are
therefore taken from the desktop app's own tables
(`desktop/src/main/kotlin/com/music/vivi/desktop/Localization_*.kt`), which cover
every language the app speaks and already word this screen — the app's own
first-run screen uses the same two keys.

Two blocks are written into `installer/windows/VIVIMusic.iss`, between markers:

  [CustomMessages]  english.WelcomeTitle / english.WelcomeDesc, one pair per
                    language, resolved by Inno as `{cm:...}` for the language
                    that is active; and
  [Code]            one `AddLanguage(<id>, <endonym>, <"language">)` call per
                    language, which is what fills the picker's list (the list
                    must show every language by its own name whatever the
                    currently active one is, so it cannot be a message lookup).

Run from the repository root:

    python3 scripts/generate_installer_welcome.py
"""

import os
import re
import sys

ROOT = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
ISS = os.path.join(ROOT, "installer", "windows", "VIVIMusic.iss")
LOC_DIR = os.path.join(ROOT, "desktop", "src", "main", "kotlin", "com", "music", "vivi", "desktop")

BEGIN_MSG = "; --- BEGIN GENERATED: welcome strings ---"
END_MSG = "; --- END GENERATED: welcome strings ---"
BEGIN_CODE = "{ --- BEGIN GENERATED: languages --- }"
END_CODE = "{ --- END GENERATED: languages --- }"

# The desktop language code -> the [Languages] name this installer declares.
# `in` (the legacy Android code) and `nb-rNO` are aliases of id and nb that the
# installer does not declare separately.
LANG_MAP = {
    "en": "english",
    "ar": "arabic",
    "pt-rBR": "brazilianportuguese",
    "bg": "bulgarian",
    "ca": "catalan",
    "zh-rCN": "chinesesimplified",
    "zh-rTW": "chinesetraditional",
    "cs": "czech",
    "nl": "dutch",
    "fi": "finnish",
    "fr": "french",
    "de": "german",
    "iw": "hebrew",
    "hu": "hungarian",
    "it": "italian",
    "ja": "japanese",
    "ko": "korean",
    "lt": "lithuanian",
    "nb": "norwegian",
    "pl": "polish",
    "pt": "portuguese",
    "ru": "russian",
    "sk": "slovak",
    "sl": "slovenian",
    "es": "spanish",
    "sv": "swedish",
    "ta": "tamil",
    "th": "thai",
    "tr": "turkish",
    "uk": "ukrainian",
    "as": "assamese",
    "az": "azerbaijani",
    "eu": "basque",
    "be": "belarusian",
    "bn": "bengali",
    "bs": "bosnian",
    "hr": "croatian",
    "et": "estonian",
    "fil": "filipino",
    "el": "greek",
    "hi": "hindi",
    "id": "indonesian",
    "km": "khmer",
    "ms": "malay",
    "ml": "malayalam",
    "fa": "persian",
    "pa": "punjabi",
    "ro": "romanian",
    "sr": "serbian",
    "te": "telugu",
    "vi": "vietnamese",
}

# Each language's own name, as the .isl files the [Languages] section loads
# spell it (`LanguageName=`). Kept here rather than read from the compiler:
# the endonyms must be identical on every build machine, and the compiler that
# supplies 26 of them differs between Inno Setup 6 and 7.
ENDONYMS = {
    "english": "English",
    "arabic": "العربية",
    "brazilianportuguese": "Português Brasileiro",
    "bulgarian": "Български",
    "catalan": "Català",
    "chinesesimplified": "简体中文",
    "chinesetraditional": "繁體中文",
    "czech": "Čeština",
    "dutch": "Nederlands",
    "finnish": "Suomi",
    "french": "Français",
    "german": "Deutsch",
    "hebrew": "עִבְרִית",
    "hungarian": "Magyar",
    "italian": "Italiano",
    "japanese": "日本語",
    "korean": "한국어",
    "lithuanian": "Lietuvių",
    "norwegian": "Norsk",
    "polish": "Polski",
    "portuguese": "Português (Portugal)",
    "russian": "Русский",
    "slovak": "Slovenčina",
    "slovenian": "Slovenščina",
    "spanish": "Español",
    "swedish": "Svenska",
    "tamil": "தமிழ்",
    "thai": "ไทย",
    "turkish": "Türkçe",
    "ukrainian": "Українська",
    "assamese": "অসমীয়া",
    "azerbaijani": "Azərbaycanca",
    "basque": "Euskara",
    "belarusian": "Беларуская",
    "bengali": "বাংলা",
    "bosnian": "Bosanski",
    "croatian": "Hrvatski",
    "estonian": "Eesti",
    "filipino": "Filipino",
    "greek": "Ελληνικά",
    "hindi": "हिन्दी",
    "indonesian": "Bahasa Indonesia",
    "khmer": "ខ្មែរ",
    "malay": "Bahasa Melayu",
    "malayalam": "മലയാളം",
    "persian": "فارسی",
    "punjabi": "ਪੰਜਾਬੀ",
    "romanian": "Română",
    "serbian": "Српски",
    "telugu": "తెలుగు",
    "vietnamese": "Tiếng Việt",
}

KEYS = {"language": "LangWord", "welcome_title": "WelcomeTitle", "welcome_desc": "WelcomeDesc"}


def read_table(code):
    """The three strings this screen needs, from one generated language file."""
    path = os.path.join(LOC_DIR, "Localization_" + code.replace("-", "_") + ".kt")
    if not os.path.exists(path):
        return None
    text = open(path, encoding="utf-8").read()
    found = {}
    for key, out in KEYS.items():
        match = re.search(r'"' + re.escape(key) + r'"\s+to\s+"((?:[^"\\]|\\.)*)"', text)
        if not match:
            return None
        found[out] = match.group(1)
    return found


def escape_message(value):
    """`{cm:...}` values are format strings: a lone `%` must be doubled."""
    return re.sub(r"%(?![0-9n])", "%%", value)


def escape_pascal(value):
    return value.replace("'", "''")


def splice(text, begin, end, body, newline):
    start = text.index(begin) + len(begin)
    stop = text.index(end)
    return text[:start] + newline + body + text[stop:]


def main():
    missing = [code for code in LANG_MAP if read_table(code) is None]
    if missing:
        print("missing keys in: " + ", ".join(sorted(missing)), file=sys.stderr)
        return 1

    entries = []
    for code, lang_id in LANG_MAP.items():
        table = read_table(code)
        entries.append((lang_id, table["LangWord"], table["WelcomeTitle"], table["WelcomeDesc"]))

    # The picker lists English first (it is the default) and everything else by
    # the name each language calls itself.
    entries.sort(key=lambda e: (e[0] != "english", ENDONYMS[e[0]].lower()))

    msg_lines = []
    for lang_id, word, title, desc in entries:
        msg_lines.append(f"{lang_id}.WelcomeTitle={escape_message(title)}")
        msg_lines.append(f"{lang_id}.WelcomeDesc={escape_message(desc)}")

    code_lines = []
    for lang_id, word, title, desc in entries:
        code_lines.append(
            "  AddLanguage('%s', '%s', '%s');"
            % (lang_id, escape_pascal(ENDONYMS[lang_id]), escape_pascal(word))
        )

    # `utf-8-sig` reads a BOM when one is there and leaves it out of the text.
    text = open(ISS, encoding="utf-8-sig", newline="").read()
    newline = "\r\n" if "\r\n" in text else "\n"
    text = splice(text, BEGIN_MSG, END_MSG, newline.join(msg_lines), newline)
    text = splice(text, BEGIN_CODE, END_CODE, newline.join(code_lines), newline)
    # Same encoding the script is committed in: UTF-8 without a BOM, which Inno
    # Setup has read as UTF-8 since 6.3 (the compiler CI installs is 6.7+).
    open(ISS, "w", encoding="utf-8", newline="").write(text)
    print(f"Wrote {len(entries)} languages into {os.path.relpath(ISS, ROOT)}")
    return 0


if __name__ == "__main__":
    sys.exit(main())
