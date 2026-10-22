# -*- coding: utf-8 -*-
"""The title the Spotify import head carries once an account is connected.

The head used to say *Import from Spotify* in both states and stated the state
in a pill under it ("Not connected to Spotify" / "Connected as ..."). The pill is
gone: with no account the head reads *Import from Spotify* like the settings row
that opens it, and with an account it reads *Connected to Spotify*, which is what
the screen is about at that point. That second title is a new string, authored
here for every supported language; the first one is the existing
`spotify_import` key.
"""

EXTRA_TRANSLATIONS = {
    "spotify_connected_title": {
        "ar": "متصل بـ Spotify",
        "as": "Spotify-ৰ সৈতে সংযুক্ত",
        "az": "Spotify-a qoşulub",
        "be": "Падключана да Spotify",
        "bg": "Свързано със Spotify",
        "bn": "Spotify-তে সংযুক্ত",
        "bs": "Povezano sa Spotify",
        "ca": "Connectat a Spotify",
        "cs": "Připojeno k Spotify",
        "de": "Mit Spotify verbunden",
        "el": "Συνδεδεμένο με το Spotify",
        "es": "Conectado a Spotify",
        "et": "Spotifyga ühendatud",
        "eu": "Spotifyrekin konektatuta",
        "fa": "متصل به Spotify",
        "fi": "Yhdistetty Spotifyiin",
        "fil": "Konektado sa Spotify",
        "fr": "Connecté à Spotify",
        "hi": "Spotify से जुड़ा",
        "hr": "Povezano sa Spotifyjem",
        "hu": "Spotifyhoz csatlakoztatva",
        "id": "Terhubung ke Spotify",
        "in": "Terhubung ke Spotify",
        "it": "Connesso a Spotify",
        "iw": "מחובר ל-Spotify",
        "ja": "Spotify に接続済み",
        "km": "បានភ្ជាប់ទៅ Spotify",
        "ko": "Spotify에 연결됨",
        "lt": "Prijungta prie Spotify",
        "ml": "Spotify-യിലേക്ക് ബന്ധിപ്പിച്ചു",
        "ms": "Disambungkan ke Spotify",
        "nb": "Koblet til Spotify",
        "nb-rNO": "Koblet til Spotify",
        "nl": "Verbonden met Spotify",
        "pa": "Spotify ਨਾਲ ਜੁੜਿਆ",
        "pl": "Połączono ze Spotify",
        "pt": "Ligado ao Spotify",
        "pt-rBR": "Conectado ao Spotify",
        "ro": "Conectat la Spotify",
        "ru": "Подключено к Spotify",
        "sk": "Pripojené k Spotify",
        "sl": "Povezano s Spotifyjem",
        "sr": "Povezano sa Spotify",
        "sv": "Ansluten till Spotify",
        "ta": "Spotify-யுடன் இணைக்கப்பட்டது",
        "te": "Spotifyకు కనెక్ట్ చేయబడింది",
        "th": "เชื่อมต่อกับ Spotify แล้ว",
        "tr": "Spotify'a bağlı",
        "uk": "Підключено до Spotify",
        "vi": "Đã kết nối với Spotify",
        "zh-rCN": "已连接 Spotify",
        "zh-rTW": "已連線 Spotify",
    },
}
