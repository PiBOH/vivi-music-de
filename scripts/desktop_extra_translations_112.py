# -*- coding: utf-8 -*-
"""The Google session as the title of its head.

`google_connected_title`: the Google sign-in and Account heads said the session
in a "Logged in as ..." badge, which repeated in one more line what the title and
the sidebar already say. The state is the title now, exactly as the Spotify head
does it (`spotify_connected_title` / `spotify_import`), so a signed-in screen
needs its own sentence and this is it. The signed-out half is the app's existing
`login_google`, and the name of the account stays in the sidebar.

The wording follows `spotify_connected_title` in every language, so the two
sign-ins read as one family.
"""

EXTRA_TRANSLATIONS = {
    "google_connected_title": {
        "ar": "متصل بـ Google",
        "as": "Google-ৰ সৈতে সংযুক্ত",
        "az": "Google ilə bağlıdır",
        "be": "Падключана да Google",
        "bg": "Свързано с Google",
        "bn": "Google-এর সাথে সংযুক্ত",
        "bs": "Povezano s Googleom",
        "ca": "Connectat a Google",
        "cs": "Připojeno ke Google",
        "de": "Mit Google verbunden",
        "el": "Συνδεδεμένο με την Google",
        "es": "Conectado a Google",
        "et": "Google'iga ühendatud",
        "eu": "Google-ra konektatuta",
        "fa": "متصل به Google",
        "fi": "Yhdistetty Googleen",
        "fil": "Nakakonekta sa Google",
        "fr": "Connecté à Google",
        "hi": "Google से कनेक्टेड",
        "hr": "Povezano s Googleom",
        "hu": "Csatlakozva a Google-höz",
        "id": "Terhubung ke Google",
        "in": "Terhubung ke Google",
        "it": "Connesso a Google",
        "iw": "מחובר ל-Google",
        "ja": "Google に接続済み",
        "km": "បានភ្ជាប់ទៅ Google",
        "ko": "Google에 연결됨",
        "lt": "Prijungta prie Google",
        "ml": "Google-ലേക്ക് ബന്ധിപ്പിച്ചു",
        "ms": "Disambungkan ke Google",
        "nb": "Koblet til Google",
        "nl": "Verbonden met Google",
        "pa": "Google ਨਾਲ ਕਨੈਕਟ ਕੀਤਾ",
        "pl": "Połączono z Google",
        "pt": "Ligado à Google",
        "pt_rBR": "Conectado ao Google",
        "ro": "Conectat la Google",
        "ru": "Подключено к Google",
        "sk": "Pripojené ku Google",
        "sl": "Povezano z Googlom",
        "sr": "Povezano sa Google-om",
        "sv": "Ansluten till Google",
        "ta": "Google உடன் இணைக்கப்பட்டது",
        "te": "Googleకు కనెక్ట్ చేయబడింది",
        "th": "เชื่อมต่อกับ Google แล้ว",
        "tr": "Google'a bağlı",
        "uk": "Підключено до Google",
        "vi": "Đã kết nối với Google",
        "zh_rCN": "已连接 Google",
        "zh_rTW": "已連結 Google",
    },
}
