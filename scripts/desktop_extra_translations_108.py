# -*- coding: utf-8 -*-
"""The account screen's sync row, labelled as the action it is.

The row ran a whole-library sync but was called by the thing it synchronises
("Sync playlist"), which reads like a setting next to the auto-sync switch above
it. It says "Sync now" in every language here, and the run's own progress is the
counter in its trailing slot (see `PlaylistSync.Status.detailText`). The phone
keeps its own label, so this key is desktop-only.
"""

EXTRA_TRANSLATIONS = {
    "sync_now": {
        "ar": "مزامنة الآن",
        "as": "এতিয়াই ছিংক কৰক",
        "az": "İndi sinxronlaşdır",
        "be": "Сінхранізаваць зараз",
        "bg": "Синхронизирай сега",
        "bn": "এখনই সিঙ্ক করুন",
        "bs": "Sinhronizuj sada",
        "ca": "Sincronitza ara",
        "cs": "Synchronizovat nyní",
        "de": "Jetzt synchronisieren",
        "el": "Συγχρονισμός τώρα",
        "es": "Sincronizar ahora",
        "et": "Sünkroniseeri kohe",
        "eu": "Sinkronizatu orain",
        "fa": "اکنون همگام‌سازی کن",
        "fi": "Synkronoi nyt",
        "fil": "I-sync ngayon",
        "fr": "Synchroniser maintenant",
        "hi": "अभी सिंक करें",
        "hr": "Sinhroniziraj sada",
        "hu": "Szinkronizálás most",
        "id": "Sinkronkan sekarang",
        "in": "Sinkronkan sekarang",
        "it": "Sincronizza ora",
        "iw": "סנכרן עכשיו",
        "ja": "今すぐ同期",
        "km": "ធ្វើសមកាលកម្មឥឡូវ",
        "ko": "지금 동기화",
        "lt": "Sinchronizuoti dabar",
        "ml": "ഇപ്പോൾ സമന്വയിപ്പിക്കുക",
        "ms": "Segerakkan sekarang",
        "nb": "Synkroniser nå",
        "nb-rNO": "Synkroniser nå",
        "nl": "Nu synchroniseren",
        "pa": "ਹੁਣ ਸਿੰਕ ਕਰੋ",
        "pl": "Synchronizuj teraz",
        "pt": "Sincronizar agora",
        "pt-rBR": "Sincronizar agora",
        "ro": "Sincronizează acum",
        "ru": "Синхронизировать сейчас",
        "sk": "Synchronizovať teraz",
        "sl": "Sinhroniziraj zdaj",
        "sr": "Синхронизуј сада",
        "sv": "Synkronisera nu",
        "ta": "இப்போது ஒத்திசை",
        "te": "ఇప్పుడే సమకాలీకరించు",
        "th": "ซิงค์ตอนนี้",
        "tr": "Şimdi eşitle",
        "uk": "Синхронізувати зараз",
        "vi": "Đồng bộ ngay",
        "zh-rCN": "立即同步",
        "zh-rTW": "立即同步",
    },
}
