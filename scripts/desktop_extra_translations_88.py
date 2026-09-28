# -*- coding: utf-8 -*-
"""`playlists_sync_ytm` / `playlists_upload_and_sync`.

The bulk action in Account → Playlists and in the playlist list was always
labelled "Create on YouTube Music" (`playlists_upload`), which is wrong as soon
as the local playlists already exist on the account: the action then does not
create anything, it brings the account's copies up to date (the run matches a
local playlist to an account playlist of the same name and only pushes the songs
that copy is missing). The label now follows what the pending work really is —
`PlaylistSync.UploadPlan` counts, per pending playlist, whether it would be
created or updated:

  * create only            -> `playlists_upload`         ("Create on YouTube Music")
  * update only            -> `playlists_sync_ytm`       ("Sync with YouTube Music")
  * create and update both -> `playlists_upload_and_sync` ("Create and sync with YouTube Music")

Neither key has an Android resource behind it (the phone only creates a playlist
at creation time, from its create dialog's switch), so both are written out for
every language here. "YouTube Music" stays as it is — it is the service's name,
not a translatable word.

A language left out falls back to English (`Localization.get`), which the audit
then reports as a gap — never a raw key.
"""

EXTRA_TRANSLATIONS = {
    "playlists_sync_ytm": {
        "ar": "مزامنة مع YouTube Music",
        "as": "YouTube Music ৰ সৈতে ছিংক কৰক",
        "az": "YouTube Music ilə sinxronlaşdır",
        "be": "Сінхранізаваць з YouTube Music",
        "bg": "Синхронизиране с YouTube Music",
        "bn": "YouTube Music-এর সাথে সিঙ্ক করুন",
        "bs": "Sinhronizuj sa YouTube Music",
        "ca": "Sincronitza amb YouTube Music",
        "cs": "Synchronizovat s YouTube Music",
        "de": "Mit YouTube Music synchronisieren",
        "el": "Συγχρονισμός με το YouTube Music",
        "es": "Sincronizar con YouTube Music",
        "et": "Sünkrooni YouTube Musicuga",
        "eu": "Sinkronizatu YouTube Music-ekin",
        "fa": "همگام‌سازی با YouTube Music",
        "fi": "Synkronoi YouTube Musicin kanssa",
        "fil": "I-sync sa YouTube Music",
        "fr": "Synchroniser avec YouTube Music",
        "hi": "YouTube Music के साथ सिंक करें",
        "hr": "Sinkroniziraj s YouTube Music",
        "hu": "Szinkronizálás a YouTube Music-kal",
        "id": "Sinkronkan dengan YouTube Music",
        "iw": "סנכרון עם YouTube Music",
        "it": "Sincronizza con YouTube Music",
        "ja": "YouTube Music と同期",
        "km": "ធ្វើសមកាលកម្មជាមួយ YouTube Music",
        "ko": "YouTube Music과 동기화",
        "lt": "Sinchronizuoti su YouTube Music",
        "ml": "YouTube Music-മായി സമന്വയിപ്പിക്കുക",
        "ms": "Segerakkan dengan YouTube Music",
        "nb": "Synkroniser med YouTube Music",
        "nl": "Synchroniseren met YouTube Music",
        "pa": "YouTube Music ਨਾਲ ਸਿੰਕ ਕਰੋ",
        "pl": "Synchronizuj z YouTube Music",
        "pt": "Sincronizar com o YouTube Music",
        "ro": "Sincronizează cu YouTube Music",
        "ru": "Синхронизировать с YouTube Music",
        "sk": "Synchronizovať s YouTube Music",
        "sl": "Sinhroniziraj z YouTube Music",
        "sr": "Синхрониши са YouTube Music",
        "sv": "Synkronisera med YouTube Music",
        "ta": "YouTube Music உடன் ஒத்திசைக்கவும்",
        "te": "YouTube Music తో సమకాలీకరించండి",
        "th": "ซิงค์กับ YouTube Music",
        "tr": "YouTube Music ile senkronize et",
        "uk": "Синхронізувати з YouTube Music",
        "vi": "Đồng bộ với YouTube Music",
        "zh-rCN": "与 YouTube Music 同步",
        "zh-rTW": "與 YouTube Music 同步",
    },
    "playlists_upload_and_sync": {
        "ar": "إنشاء ومزامنة مع YouTube Music",
        "as": "YouTube Music-ত সৃষ্টি কৰি ছিংক কৰক",
        "az": "YouTube Music-də yarat və sinxronlaşdır",
        "be": "Стварыць і сінхранізаваць з YouTube Music",
        "bg": "Създаване и синхронизиране с YouTube Music",
        "bn": "YouTube Music-এ তৈরি করে সিঙ্ক করুন",
        "bs": "Kreiraj i sinhronizuj sa YouTube Music",
        "ca": "Crea i sincronitza amb YouTube Music",
        "cs": "Vytvořit a synchronizovat s YouTube Music",
        "de": "Erstellen und mit YouTube Music synchronisieren",
        "el": "Δημιουργία και συγχρονισμός με το YouTube Music",
        "es": "Crear y sincronizar con YouTube Music",
        "et": "Loo ja sünkrooni YouTube Musicuga",
        "eu": "Sortu eta sinkronizatu YouTube Music-ekin",
        "fa": "ایجاد و همگام‌سازی با YouTube Music",
        "fi": "Luo ja synkronoi YouTube Musicin kanssa",
        "fil": "Gumawa at i-sync sa YouTube Music",
        "fr": "Créer et synchroniser avec YouTube Music",
        "hi": "YouTube Music पर बनाएँ और सिंक करें",
        "hr": "Stvori i sinkroniziraj s YouTube Music",
        "hu": "Létrehozás és szinkronizálás a YouTube Music-kal",
        "id": "Buat dan sinkronkan dengan YouTube Music",
        "iw": "יצירה וסנכרון עם YouTube Music",
        "it": "Crea e sincronizza con YouTube Music",
        "ja": "YouTube Music に作成して同期",
        "km": "បង្កើត និងធ្វើសមកាលកម្មជាមួយ YouTube Music",
        "ko": "YouTube Music에 만들고 동기화",
        "lt": "Sukurti ir sinchronizuoti su YouTube Music",
        "ml": "YouTube Music-ൽ സൃഷ്ടിച്ച് സമന്വയിപ്പിക്കുക",
        "ms": "Cipta dan segerakkan dengan YouTube Music",
        "nb": "Opprett og synkroniser med YouTube Music",
        "nl": "Aanmaken en synchroniseren met YouTube Music",
        "pa": "YouTube Music 'ਤੇ ਬਣਾਓ ਅਤੇ ਸਿੰਕ ਕਰੋ",
        "pl": "Utwórz i synchronizuj z YouTube Music",
        "pt": "Criar e sincronizar com o YouTube Music",
        "ro": "Creează și sincronizează cu YouTube Music",
        "ru": "Создать и синхронизировать с YouTube Music",
        "sk": "Vytvoriť a synchronizovať s YouTube Music",
        "sl": "Ustvari in sinhroniziraj z YouTube Music",
        "sr": "Направи и синхрониши са YouTube Music",
        "sv": "Skapa och synkronisera med YouTube Music",
        "ta": "YouTube Music-ல் உருவாக்கி ஒத்திசைக்கவும்",
        "te": "YouTube Music లో సృష్టించి సమకాలీకరించండి",
        "th": "สร้างและซิงค์กับ YouTube Music",
        "tr": "YouTube Music'te oluştur ve senkronize et",
        "uk": "Створити й синхронізувати з YouTube Music",
        "vi": "Tạo và đồng bộ với YouTube Music",
        "zh-rCN": "创建并与 YouTube Music 同步",
        "zh-rTW": "建立並與 YouTube Music 同步",
    },
}
