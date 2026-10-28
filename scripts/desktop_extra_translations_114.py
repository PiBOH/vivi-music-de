# -*- coding: utf-8 -*-
"""The two player designs that were renamed.

`player_design_classic` was "Classic" and is "Basic" now: it draws the plain
player every other layout is compared against, and "Classic" promised a style the
app never had. `player_design_new` was "New" and is "Apple Music": the layout is
the web player's full-screen view, and a name that only said it was newer than
the other one would say nothing about what it looks like.

Apple Music is a brand and keeps its Latin spelling in every language, the way the
app already keeps "Spotify", "Discord" and "YouTube" untranslated. "Basic" is an
ordinary word and is translated everywhere.

The mini-player keeps its own "Classic" / "New": those names describe that
widget's layouts and were not part of this rename.
"""

BASIC = {
    "ar": "أساسي",
    "as": "সাধাৰণ",
    "az": "Əsas",
    "be": "Базавы",
    "bg": "Основен",
    "bn": "সাধারণ",
    "bs": "Osnovno",
    "ca": "Bàsic",
    "cs": "Základní",
    "de": "Einfach",
    "el": "Απλό",
    "es": "Básico",
    "et": "Lihtne",
    "eu": "Oinarrizkoa",
    "fa": "پایه",
    "fi": "Perus",
    "fil": "Payak",
    "fr": "Basique",
    "hi": "सामान्य",
    "hr": "Osnovno",
    "hu": "Alap",
    "id": "Dasar",
    "in": "Dasar",
    "it": "Base",
    "iw": "בסיסי",
    "ja": "ベーシック",
    "km": "មូលដ្ឋាន",
    "ko": "기본",
    "lt": "Paprastas",
    "ml": "അടിസ്ഥാനം",
    "ms": "Asas",
    "nb": "Enkel",
    "nb-rNO": "Enkel",
    "nl": "Basis",
    "pa": "ਸਾਧਾਰਨ",
    "pl": "Podstawowy",
    "pt": "Básico",
    "pt-rBR": "Básico",
    "ro": "De bază",
    "ru": "Базовый",
    "sk": "Základný",
    "sl": "Osnovno",
    "sr": "Основно",
    "sv": "Enkel",
    "ta": "அடிப்படை",
    "te": "ప్రాథమిక",
    "th": "พื้นฐาน",
    "tr": "Temel",
    "uk": "Базовий",
    "vi": "Cơ bản",
    "zh-rCN": "基础",
    "zh-rTW": "基本",
}

APPLE_MUSIC = {lang: "Apple Music" for lang in BASIC}

EXTRA_TRANSLATIONS = {
    "player_design_classic": BASIC,
    "player_design_new": APPLE_MUSIC,
}
