# -*- coding: utf-8 -*-
"""The Account screen's session card.

The preview states the session in one word under the account's name, where the
DE said it with a hero ("Connected to Google") and a hint sentence. `login`
("Log in") is the action and cannot be reused for the state, so this is a key of
its own.
"""

ACCOUNT_SIGNED_IN = {
    "en": "Signed in",
    "ar": "تم تسجيل الدخول",
    "as": "ছাইন ইন কৰা হৈছে",
    "az": "Daxil olunub",
    "be": "Увайшлі ў сістэму",
    "bg": "Влезли сте",
    "bn": "সাইন ইন করা হয়েছে",
    "bs": "Prijavljeni ste",
    "ca": "Sessió iniciada",
    "cs": "Přihlášeno",
    "de": "Angemeldet",
    "el": "Συνδεδεμένος",
    "es": "Sesión iniciada",
    "et": "Sisse logitud",
    "eu": "Saioa hasita",
    "fa": "وارد شده‌اید",
    "fi": "Kirjautunut sisään",
    "fil": "Nakapasok",
    "fr": "Connecté",
    "hi": "साइन इन है",
    "hr": "Prijavljeni ste",
    "hu": "Bejelentkezve",
    "id": "Sudah masuk",
    "in": "Sudah masuk",
    "it": "Accesso effettuato",
    "iw": "מחובר",
    "ja": "ログイン済み",
    "km": "បានចូល",
    "ko": "로그인됨",
    "lt": "Prisijungta",
    "ml": "സൈൻ ഇൻ ചെയ്തു",
    "ms": "Telah log masuk",
    "nb": "Logget inn",
    "nb-rNO": "Logget inn",
    "nl": "Aangemeld",
    "pa": "ਸਾਈਨ ਇਨ ਹੈ",
    "pl": "Zalogowano",
    "pt": "Sessão iniciada",
    "pt-rBR": "Sessão iniciada",
    "ro": "Conectat",
    "ru": "Вы вошли",
    "sk": "Prihlásené",
    "sl": "Prijavljeni",
    "sr": "Пријављени сте",
    "sv": "Inloggad",
    "ta": "உள்நுழைந்துள்ளது",
    "te": "సైన్ ఇన్ అయ్యారు",
    "th": "เข้าสู่ระบบแล้ว",
    "tr": "Oturum açık",
    "uk": "Ви увійшли",
    "vi": "Đã đăng nhập",
    "zh-rCN": "已登录",
    "zh-rTW": "已登入",
}

EXTRA_TRANSLATIONS = {
    "account_signed_in": ACCOUNT_SIGNED_IN,
}
