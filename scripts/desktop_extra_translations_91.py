# -*- coding: utf-8 -*-
"""`auto_skip_next_on_error_desc`.

The row is "Auto skip to next song when error occurs" with a description that
read "Ensure your continuous playback experience" — a sentence about the *benefit*
that never says what the switch does, and which is not even a description of it in
most languages (the mobile app's own wording, inherited through MAPPING). The
description now states the behaviour: the next song is picked automatically when a
track fails, which is what the setting actually does. The screenshot the user took
showed the Italian row reading "Garantisci la tua esperienza di riproduzione
continua", which says nothing about skipping.

Every language is written out here because the old wording comes from the Android
resource `auto_skip_next_on_error_desc` — leaving any language out would keep the
old sentence there.

A language left out falls back to English (`Localization.get`), which the audit
then reports as a gap — never a raw key.
"""

EXTRA_TRANSLATIONS = {
    "auto_skip_next_on_error_desc": {
        "ar": "تخطَّ إلى الأغنية التالية تلقائيًا في حال حدوث خطأ",
        "as": "ত্ৰুটি হ'লে স্বয়ংক্ৰিয়ভাৱে পৰৱৰ্তী গীতলৈ যাওক",
        "az": "Xəta olduqda avtomatik olaraq növbəti mahnıya keç",
        "be": "Аўтаматычна пераходзіць да наступнай песні пры памылцы",
        "bg": "Автоматично преминаване към следващата песен при грешка",
        "bn": "ত্রুটি হলে স্বয়ংক্রিয়ভাবে পরবর্তী গানে যান",
        "bs": "Automatski pređi na sljedeću pjesmu u slučaju greške",
        "ca": "Salta automàticament a la cançó següent si hi ha un error",
        "cs": "Automaticky přejít na další skladbu při chybě",
        "de": "Bei einem Fehler automatisch zum nächsten Titel springen",
        "el": "Αυτόματη μετάβαση στο επόμενο τραγούδι σε περίπτωση σφάλματος",
        "es": "Pasar automáticamente a la siguiente canción si hay un error",
        "et": "Vea korral mine automaatselt järgmise laulu juurde",
        "eu": "Errore bat gertatuz gero, jauzi egin automatikoki hurrengo abestira",
        "fa": "در صورت بروز خطا به‌طور خودکار به آهنگ بعدی برو",
        "fi": "Siirry automaattisesti seuraavaan kappaleeseen virheen sattuessa",
        "fil": "Awtomatikong lumipat sa susunod na kanta kapag nagka-error",
        "fr": "Passer automatiquement au morceau suivant en cas d'erreur",
        "hi": "त्रुटि होने पर स्वतः अगले गाने पर जाएँ",
        "hr": "Automatski prijeđi na sljedeću pjesmu u slučaju pogreške",
        "hu": "Hiba esetén automatikus ugrás a következő számra",
        "id": "Otomatis lompat ke lagu berikutnya jika terjadi kesalahan",
        "it": "Passa automaticamente alla canzone successiva in caso di errore",
        "iw": "דלג אוטומטית לשיר הבא במקרה של שגיאה",
        "ja": "エラーが発生したら自動的に次の曲へスキップ",
        "km": "លោតទៅបទបន្ទាប់ដោយស្វ័យប្រវត្តិនៅពេលមានបញ្ហា",
        "ko": "오류가 발생하면 자동으로 다음 곡으로 넘어갑니다",
        "lt": "Klaidos atveju automatiškai pereiti prie kitos dainos",
        "ml": "പിശക് സംഭവിച്ചാൽ സ്വയമേവ അടുത്ത പാട്ടിലേക്ക് പോകുക",
        "ms": "Langkau secara automatik ke lagu seterusnya jika berlaku ralat",
        "nb": "Hopp automatisk til neste sang ved feil",
        "nl": "Automatisch naar het volgende nummer gaan bij een fout",
        "pa": "ਗਲਤੀ ਹੋਣ 'ਤੇ ਆਪਣੇ-ਆਪ ਅਗਲੇ ਗੀਤ 'ਤੇ ਜਾਓ",
        "pl": "Automatycznie przejdź do następnego utworu w razie błędu",
        "pt": "Passar automaticamente para a faixa seguinte em caso de erro",
        "pt-rBR": "Pular automaticamente para a próxima faixa em caso de erro",
        "ro": "Treci automat la piesa următoare în caz de eroare",
        "ru": "Автоматически переходить к следующему треку при ошибке",
        "sk": "Pri chybe automaticky prejsť na ďalšiu skladbu",
        "sl": "Ob napaki samodejno preskoči na naslednjo skladbo",
        "sr": "Аутоматски пређи на следећу песму у случају грешке",
        "sv": "Hoppa automatiskt till nästa låt vid fel",
        "ta": "பிழை ஏற்பட்டால் தானாகவே அடுத்த பாடலுக்குச் செல்லும்",
        "te": "లోపం సంభవించినప్పుడు స్వయంచాలకంగా తదుపరి పాటకు వెళ్లండి",
        "th": "ข้ามไปเพลงถัดไปโดยอัตโนมัติเมื่อเกิดข้อผิดพลาด",
        "tr": "Hata oluştuğunda otomatik olarak sonraki şarkıya geç",
        "uk": "Автоматично переходити до наступного треку в разі помилки",
        "vi": "Tự động chuyển sang bài tiếp theo khi xảy ra lỗi",
        "zh-rCN": "出错时自动跳到下一首",
        "zh-rTW": "發生錯誤時自動跳到下一首",
    },
}
