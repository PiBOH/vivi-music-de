# -*- coding: utf-8 -*-
"""The Listen Together log viewer's two buttons, where the phone has no wording.

`lt_copy_logs` and `lt_clear_logs` are mapped (see `MAPPING`) onto the APK's own
`copy` and `clear` resources, which are translated in 18 of the 52 languages, so
those tables get the wording for free and stay in step with the phone.

The other 32 — plus `nb-rNO`, whose twin `nb` is one of them — would fall back to
English and leave a button in the middle of a translated dialog reading "Copy".
These are their two words, authored here: `Copy` and `Clear` are short enough to
translate without borrowing a sentence from anywhere, and both are verbs about
the log list, which is what the dialog shows.

Keys and languages are the only thing in this file: it adds no English (that
comes from the `copy` / `clear` resources) and overrides nothing.
"""

EXTRA_TRANSLATIONS = {
    "lt_copy_logs": {
        "as": "প্ৰতিলিপি কৰক",
        "be": "Капіяваць",
        "bg": "Копиране",
        "bn": "কপি করুন",
        "bs": "Kopiraj",
        "el": "Αντιγραφή",
        "et": "Kopeeri",
        "eu": "Kopiatu",
        "fa": "کپی",
        "fi": "Kopioi",
        "fil": "Kopyahin",
        "hi": "कॉपी करें",
        "hr": "Kopiraj",
        "hu": "Másolás",
        "iw": "העתק",
        "km": "ចម្លង",
        "ko": "복사",
        "lt": "Kopijuoti",
        "ml": "പകർത്തുക",
        "ms": "Salin",
        "nb": "Kopier",
        "nb-rNO": "Kopier",
        "nl": "Kopiëren",
        "pa": "ਕਾਪੀ ਕਰੋ",
        "pl": "Kopiuj",
        "pt": "Copiar",
        "sk": "Kopírovať",
        "sl": "Kopiraj",
        "sr": "Копирај",
        "ta": "நகலெடு",
        "te": "కాపీ చేయి",
        "uk": "Копіювати",
        "zh-rTW": "複製",
    },
    "lt_clear_logs": {
        "as": "পৰিষ্কাৰ কৰক",
        "be": "Ачысціць",
        "bg": "Изчистване",
        "bn": "মুছুন",
        "bs": "Očisti",
        "el": "Εκκαθάριση",
        "et": "Tühjenda",
        "eu": "Garbitu",
        "fa": "پاک کردن",
        "fi": "Tyhjennä",
        "fil": "I-clear",
        "hi": "साफ़ करें",
        "hr": "Očisti",
        "hu": "Törlés",
        "iw": "נקה",
        "km": "សម្អាត",
        "ko": "지우기",
        "lt": "Išvalyti",
        "ml": "മായ്ക്കുക",
        "ms": "Padam",
        "nb": "Tøm",
        "nb-rNO": "Tøm",
        "nl": "Wissen",
        "pa": "ਸਾਫ਼ ਕਰੋ",
        "pl": "Wyczyść",
        "pt": "Limpar",
        "sk": "Vyčistiť",
        "sl": "Počisti",
        "sr": "Обриши",
        "ta": "அழி",
        "te": "తొలగించు",
        "uk": "Очистити",
        "zh-rTW": "清除",
    },
}
