# -*- coding: utf-8 -*-
"""`subscribe`, `subscribed`, `play_next`, `view_artist` for the languages whose
Android resources do not carry them.

The four keys of the Album / Artist / Playlist context menus are mapped to
existing Android resources (`play_next` / `view_artist` in strings.xml,
`subscribe` / `subscribed` in vivi_strings.xml), so most languages get their
wording from the app itself. The resources are simply absent for the languages
below, and a missing key falls back to English at runtime — which is exactly the
complaint the translation audits exist for (`audit_desktop_localization.py`
reported them as "would fall back" and exited non-zero).

`subscribe` / `subscribed` are the two states of the artist menu's one entry
(mobile shows "Subscribed" once subscribed); `play_next` and `view_artist` are
the queue and navigation entries of all three menus. The sentences are short and
their meaning is fixed, so each translation says that and nothing more.
"""

EXTRA_TRANSLATIONS = {
    "subscribe": {
        "as": "চাবস্ক্ৰাইব কৰক",
        "be": "Падпісацца",
        "bn": "সাবস্ক্রাইব করুন",
        "bs": "Pretplati se",
        "fa": "دنبال کردن",
        "fi": "Tilaa",
        "fil": "Mag-subscribe",
        "hi": "सब्सक्राइब करें",
        "iw": "הירשם כמנוי",
        "km": "ជាវ",
        "ml": "സബ്സ്ക്രൈബ് ചെയ്യുക",
        "ms": "Langgan",
        "nb": "Abonner",
        "nb-rNO": "Abonner",
        "pa": "ਸਬਸਕ੍ਰਾਈਬ ਕਰੋ",
        "sr": "Претплати се",
        "ta": "சந்தா செலுத்து",
        "te": "సభ్యత్వం తీసుకోండి",
    },
    "subscribed": {
        "as": "চাবস্ক্ৰাইব কৰা হৈছে",
        "be": "Падпісана",
        "bn": "সাবস্ক্রাইব করা হয়েছে",
        "bs": "Pretplaćen",
        "fa": "دنبال می‌کنید",
        "fi": "Tilattu",
        "fil": "Naka-subscribe",
        "hi": "सब्सक्राइब किया गया",
        "iw": "נרשמת כמנוי",
        "km": "បានជាវ",
        "ml": "സബ്സ്ക്രൈബ് ചെയ്തു",
        "ms": "Dilanggan",
        "nb": "Abonnert",
        "nb-rNO": "Abonnert",
        "pa": "ਸਬਸਕ੍ਰਾਈਬ ਕੀਤਾ",
        "sr": "Претплаћен",
        "ta": "சந்தா செலுத்தப்பட்டது",
        "te": "సభ్యత్వం తీసుకున్నారు",
    },
    "play_next": {
        "as": "পৰৱৰ্তী বজাওক",
        "eu": "Erreproduzitu hurrengoa",
        "fil": "I-play ang susunod",
        "km": "លេងបន្ទាប់",
        "lt": "Leisti kitą",
        "ms": "Main seterusnya",
        "sl": "Predvajaj naslednjo",
        "th": "เล่นถัดไป",
    },
    "view_artist": {
        "as": "শিল্পীক চাওক",
        "eu": "Ikusi artista",
        "fil": "Tingnan ang artist",
        "km": "មើលសិល្បករ",
        "lt": "Rodyti atlikėją",
        "ms": "Lihat artis",
        "sl": "Poglej izvajalca",
        "th": "ดูศิลปิน",
    },
}
