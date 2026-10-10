# -*- coding: utf-8 -*-
"""The desktop's "this backup cannot be read" sentence, in every language.

The Android app defines `restore_failed_corrupt` ("The backup is corrupt and
cannot be restored") in its default resources only — no `values-*/` file carries
it — so mapping the desktop key onto that resource would give every language the
English wording and the audit would report it as missing.

The desktop needs the distinction it did not have: a restore that failed because
the *file* cannot be read (the phone's archive, a truncated download, a settings
entry that will not decode) is not the same answer as "restoring did not work",
and both used to be the single key `restore_failed`. The phone's own screens make
the same distinction, so the wording follows it. Authored here for all 52
languages; `MAPPING` still points the key at the Android resource for the English
side, and this batch is applied on top.
"""

EXTRA_TRANSLATIONS = {
    "restore_failed_corrupt": {
        "ar": "النسخة الاحتياطية تالفة ولا يمكن استعادتها",
        "as": "বেকআপটো নষ্ট হৈছে আৰু পুনৰুদ্ধাৰ কৰিব নোৱাৰি",
        "az": "Ehtiyat nüsxəsi zədələnib və bərpa edilə bilməz",
        "be": "Рэзервовая копія пашкоджана і не можа быць адноўлена",
        "bg": "Архивът е повреден и не може да бъде възстановен",
        "bn": "ব্যাকআপটি নষ্ট এবং পুনরুদ্ধার করা যাবে না",
        "bs": "Sigurnosna kopija je oštećena i ne može se vratiti",
        "ca": "La còpia de seguretat està malmesa i no es pot restaurar",
        "cs": "Záloha je poškozená a nelze ji obnovit",
        "de": "Die Sicherung ist beschädigt und kann nicht wiederhergestellt werden",
        "el": "Το αντίγραφο ασφαλείας είναι κατεστραμμένο και δεν μπορεί να αποκατασταθεί",
        "es": "La copia de seguridad está dañada y no se puede restaurar",
        "et": "Varukoopia on rikkis ega ole taastatav",
        "eu": "Babeskopia hondatuta dago eta ezin da berreskuratu",
        "fa": "نسخه پشتیبان خراب است و قابل بازیابی نیست",
        "fi": "Varmuuskopio on vioittunut eikä sitä voi palauttaa",
        "fil": "Sira ang backup at hindi maibabalik",
        "fr": "La sauvegarde est corrompue et ne peut pas être restaurée",
        "hi": "बैकअप खराब है और इसे पुनर्स्थापित नहीं किया जा सकता",
        "hr": "Sigurnosna kopija je oštećena i ne može se vratiti",
        "hu": "A biztonsági mentés sérült, és nem állítható vissza",
        "id": "Cadangan rusak dan tidak dapat dipulihkan",
        "in": "Cadangan rusak dan tidak dapat dipulihkan",
        "it": "Il backup è danneggiato e non può essere ripristinato",
        "iw": "הגיבוי פגום ולא ניתן לשחזר אותו",
        "ja": "バックアップが破損しているため復元できません",
        "km": "ការបម្រុងទុកខូច ហើយមិនអាចស្តារឡើងវិញបានទេ",
        "ko": "백업이 손상되어 복원할 수 없습니다",
        "lt": "Atsarginė kopija sugadinta ir negali būti atkurta",
        "ml": "ബാക്കപ്പ് കേടായതിനാൽ പുനഃസ്ഥാപിക്കാനാകില്ല",
        "ms": "Sandaran rosak dan tidak boleh dipulihkan",
        "nb": "Sikkerhetskopien er skadet og kan ikke gjenopprettes",
        "nb-rNO": "Sikkerhetskopien er skadet og kan ikke gjenopprettes",
        "nl": "De back-up is beschadigd en kan niet worden hersteld",
        "pa": "ਬੈਕਅੱਪ ਖਰਾਬ ਹੈ ਅਤੇ ਬਹਾਲ ਨਹੀਂ ਕੀਤਾ ਜਾ ਸਕਦਾ",
        "pl": "Kopia zapasowa jest uszkodzona i nie można jej przywrócić",
        "pt": "O backup está corrompido e não pode ser restaurado",
        "pt-rBR": "O backup está corrompido e não pode ser restaurado",
        "ro": "Copia de siguranță este deteriorată și nu poate fi restabilită",
        "ru": "Резервная копия повреждена и не может быть восстановлена",
        "sk": "Záloha je poškodená a nedá sa obnoviť",
        "sl": "Varnostna kopija je poškodovana in je ni mogoče obnoviti",
        "sr": "Сигурносна копија је оштећена и не може се вратити",
        "sv": "Säkerhetskopian är skadad och kan inte återställas",
        "ta": "காப்புப்பிரதி சேதமடைந்துள்ளது, மீட்டெடுக்க முடியாது",
        "te": "బ్యాకప్ పాడైంది మరియు పునరుద్ధరించలేము",
        "th": "ข้อมูลสำรองเสียหายและไม่สามารถกู้คืนได้",
        "tr": "Yedek bozuk olduğu için geri yüklenemiyor",
        "uk": "Резервну копію пошкоджено, її не можна відновити",
        "vi": "Bản sao lưu bị hỏng và không thể khôi phục",
        "zh-rCN": "备份已损坏，无法还原",
        "zh-rTW": "備份已損毀，無法還原",
    },
}
