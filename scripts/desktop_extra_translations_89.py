# -*- coding: utf-8 -*-
"""`sync_os_volume`.

The native (OS) volume channel of the device sync got its own switch in 1.54.0,
but it borrowed the already-translated `lt_sync_volume` ("Sync volume") label,
because the alternative looked like 52 new translations for one row. Adjacent to
"Sync VIVI volume" that reads as a second volume sync rather than the *native*
one, so the row has its own key now and the wording says what it is: the volume
of the operating system (the Windows/Linux/macOS master volume, the phone's
media volume), not the app's own slider.

The key has no Android resource behind it (the switch only exists on the
desktop), so every language is written out here. "OS" is rendered with the
language's own word for the operating system where one is in common use
(Sistema, Système, System, …) instead of the English abbreviation.

A language left out falls back to English (`Localization.get`), which the audit
then reports as a gap — never a raw key.
"""

EXTRA_TRANSLATIONS = {
    "sync_os_volume": {
        "ar": "مزامنة صوت نظام التشغيل",
        "as": "OS ভলিউম ছিংক কৰক",
        "az": "OS səsini sinxronlaşdır",
        "be": "Сінхранізаваць сістэмную гучнасць",
        "bg": "Синхронизиране на системната сила на звука",
        "bn": "OS ভলিউম সিঙ্ক করুন",
        "bs": "Sinhronizuj sistemsku jačinu zvuka",
        "ca": "Sincronitza el volum del sistema",
        "cs": "Synchronizovat systémovou hlasitost",
        "de": "Systemlautstärke synchronisieren",
        "el": "Συγχρονισμός έντασης λειτουργικού συστήματος",
        "es": "Sincronizar el volumen del sistema",
        "et": "Sünkrooni süsteemi helitugevus",
        "eu": "Sinkronizatu sistemaren bolumena",
        "fa": "همگام‌سازی صدای سیستم‌عامل",
        "fi": "Synkronoi järjestelmän äänenvoimakkuus",
        "fil": "I-sync ang volume ng OS",
        "fr": "Synchroniser le volume du système",
        "hi": "OS वॉल्यूम सिंक करें",
        "hr": "Sinkroniziraj sistemsku glasnoću",
        "hu": "Rendszerhangerő szinkronizálása",
        "id": "Sinkronkan volume OS",
        "iw": "סנכרון עוצמת קול של המערכת",
        "it": "Sincronizza il volume del sistema",
        "ja": "OS の音量を同期",
        "km": "ធ្វើសមកាលកម្មសំឡេង OS",
        "ko": "OS 볼륨 동기화",
        "lt": "Sinchronizuoti sistemos garsumą",
        "ml": "OS വോളിയം സമന്വയിപ്പിക്കുക",
        "ms": "Segerakkan volum OS",
        "nb": "Synkroniser systemvolum",
        "nl": "Systeemvolume synchroniseren",
        "pa": "OS ਵੌਲਿਊਮ ਸਿੰਕ ਕਰੋ",
        "pl": "Synchronizuj głośność systemu",
        "pt": "Sincronizar o volume do sistema",
        "ro": "Sincronizează volumul sistemului",
        "ru": "Синхронизировать системную громкость",
        "sk": "Synchronizovať systémovú hlasitosť",
        "sl": "Sinhroniziraj sistemsko glasnost",
        "sr": "Синхрониши системску јачину звука",
        "sv": "Synkronisera systemvolym",
        "ta": "OS ஒலியளவை ஒத்திசைக்கவும்",
        "te": "OS వాల్యూమ్‌ను సమకాలీకరించండి",
        "th": "ซิงค์ระดับเสียงของระบบ",
        "tr": "İşletim sistemi sesini eşitle",
        "uk": "Синхронізувати системну гучність",
        "vi": "Đồng bộ âm lượng hệ thống",
        "zh-rCN": "同步系统音量",
        "zh-rTW": "同步系統音量",
    },
}
