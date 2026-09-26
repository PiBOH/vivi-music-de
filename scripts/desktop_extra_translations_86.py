# -*- coding: utf-8 -*-
"""`allow_notifications`, for every one of the 52 languages.

The mobile app's notification-permission row is labelled by a string that lives
in `updater_strings.xml`, and that file is translated for only a handful of
languages — mapping it left ~49 languages falling back to English (the audit
reported 98 missing). The desktop row therefore uses this short, dedicated key
and every language has to be written out here; a language left out would print
the English sentence, which is exactly what the translation audits catch.

The phrase is the label of a settings row that asks the OS for the notification
permission ("Allow notifications"), so each translation says that and nothing
more.
"""

EXTRA_TRANSLATIONS = {
    "allow_notifications": {
        "ar": "السماح بالإشعارات",
        "as": "জাননীৰ অনুমতি দিয়ক",
        "az": "Bildirişlərə icazə ver",
        "be": "Дазволіць апавяшчэнні",
        "bg": "Разрешаване на известия",
        "bn": "বিজ্ঞপ্তির অনুমতি দিন",
        "bs": "Dozvoli obavještenja",
        "ca": "Permet les notificacions",
        "cs": "Povolit oznámení",
        "de": "Benachrichtigungen erlauben",
        "el": "Να επιτρέπονται οι ειδοποιήσεις",
        "es": "Permitir notificaciones",
        "et": "Luba märguanded",
        "eu": "Baimendu jakinarazpenak",
        "fa": "اجازه اعلان‌ها",
        "fi": "Salli ilmoitukset",
        "fil": "Payagan ang mga notification",
        "fr": "Autoriser les notifications",
        "hi": "सूचनाओं की अनुमति दें",
        "hr": "Dopusti obavijesti",
        "hu": "Értesítések engedélyezése",
        "id": "Izinkan notifikasi",
        "in": "Izinkan notifikasi",
        "it": "Consenti notifiche",
        "iw": "אפשר התראות",
        "ja": "通知を許可",
        "km": "អនុញ្ញាតការជូនដំណឹង",
        "ko": "알림 허용",
        "lt": "Leisti pranešimus",
        "ml": "അറിയിപ്പുകൾ അനുവദിക്കുക",
        "ms": "Benarkan pemberitahuan",
        "nb": "Tillat varsler",
        "nb-rNO": "Tillat varsler",
        "nl": "Meldingen toestaan",
        "pa": "ਸੂਚਨਾਵਾਂ ਦੀ ਆਗਿਆ ਦਿਓ",
        "pl": "Zezwól na powiadomienia",
        "pt": "Permitir notificações",
        "pt-rBR": "Permitir notificações",
        "ro": "Permite notificările",
        "ru": "Разрешить уведомления",
        "sk": "Povoliť oznámenia",
        "sl": "Dovoli obvestila",
        "sr": "Дозволи обавештења",
        "sv": "Tillåt aviseringar",
        "ta": "அறிவிப்புகளை அனுமதி",
        "te": "నోటిఫికేషన్‌లను అనుమతించండి",
        "th": "อนุญาตการแจ้งเตือน",
        "tr": "Bildirimlere izin ver",
        "uk": "Дозволити сповіщення",
        "vi": "Cho phép thông báo",
        "zh-rCN": "允许通知",
        "zh-rTW": "允許通知",
    },
}
