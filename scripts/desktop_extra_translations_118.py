# -*- coding: utf-8 -*-
"""The Appearance hub's Animations row.

The row is a door to the Animations screen, so it says what is behind it. The
master switch on that screen keeps `animations_desc` ("Enable UI animations ..."),
which is the sentence about the switch itself; the two are not the same text, so
the hub row is a key of its own.
"""

ANIMATIONS_HUB_DESC = {
    "en": "The master switch, screen transitions, the transition style and the speed.",
    "ar": "المفتاح الرئيسي وانتقالات الشاشة ونمط الانتقال وسرعته.",
    "as": "মূল ছুইচ, স্ক্ৰীন পৰিৱৰ্তন, পৰিৱৰ্তনৰ শৈলী আৰু গতি।",
    "az": "Əsas açar, ekran keçidləri, keçid üslubu və sürəti.",
    "be": "Галоўны перамыкач, пераходы паміж экранамі, стыль пераходу і хуткасць.",
    "bg": "Главният превключвател, преходите между екраните, стилът на прехода и скоростта.",
    "bn": "প্রধান সুইচ, স্ক্রিন ট্রানজিশন, ট্রানজিশনের ধরন এবং গতি।",
    "bs": "Glavni prekidač, prijelazi ekrana, stil prijelaza i brzina.",
    "ca": "L'interruptor principal, les transicions de pantalla, l'estil de transició i la velocitat.",
    "cs": "Hlavní přepínač, přechody obrazovek, styl přechodu a rychlost.",
    "de": "Der Hauptschalter, die Bildschirmübergänge, der Übergangsstil und die Geschwindigkeit.",
    "el": "Ο κύριος διακόπτης, οι μεταβάσεις οθονών, το στυλ μετάβασης και η ταχύτητα.",
    "es": "El interruptor principal, las transiciones de pantalla, el estilo de transición y la velocidad.",
    "et": "Pealüliti, ekraanisiirded, ülemineku stiil ja kiirus.",
    "eu": "Etengailu nagusia, pantaila-trantsizioak, trantsizio-estiloa eta abiadura.",
    "fa": "کلید اصلی، انتقال بین صفحه‌ها، سبک انتقال و سرعت.",
    "fi": "Pääkytkin, näyttösiirtymät, siirtymätyyli ja nopeus.",
    "fil": "Ang master switch, mga screen transition, ang transition style at ang bilis.",
    "fr": "L'interrupteur principal, les transitions d'écran, le style de transition et la vitesse.",
    "hi": "मुख्य स्विच, स्क्रीन ट्रांज़िशन, ट्रांज़िशन शैली और गति।",
    "hr": "Glavni prekidač, prijelazi zaslona, stil prijelaza i brzina.",
    "hu": "A főkapcsoló, a képernyőátmenetek, az átmenet stílusa és a sebesség.",
    "id": "Sakelar utama, transisi layar, gaya transisi, dan kecepatan.",
    "in": "Sakelar utama, transisi layar, gaya transisi, dan kecepatan.",
    "it": "L'interruttore principale, le transizioni tra schermate, lo stile di transizione e la velocità.",
    "iw": "המתג הראשי, מעברי המסך, סגנון המעבר והמהירות.",
    "ja": "マスタースイッチ、画面の切り替え、切り替えスタイル、速度。",
    "km": "កុងតាក់មេ ការផ្លាស់ប្តូរអេក្រង់ រចនាប័ទ្មផ្លាស់ប្តូរ និងល្បឿន។",
    "ko": "마스터 스위치, 화면 전환, 전환 스타일 및 속도.",
    "lt": "Pagrindinis jungiklis, ekranų perėjimai, perėjimo stilius ir greitis.",
    "ml": "പ്രധാന സ്വിച്ച്, സ്ക്രീൻ ട്രാൻസിഷനുകൾ, ട്രാൻസിഷൻ ശൈലി, വേഗത.",
    "ms": "Suis utama, peralihan skrin, gaya peralihan dan kelajuan.",
    "nb": "Hovedbryteren, skjermoverganger, overgangsstilen og hastigheten.",
    "nb-rNO": "Hovedbryteren, skjermoverganger, overgangsstilen og hastigheten.",
    "nl": "De hoofdschakelaar, de schermovergangen, de overgangsstijl en de snelheid.",
    "pa": "ਮੁੱਖ ਸਵਿੱਚ, ਸਕ੍ਰੀਨ ਟ੍ਰਾਂਜ਼ੀਸ਼ਨ, ਟ੍ਰਾਂਜ਼ੀਸ਼ਨ ਸਟਾਈਲ ਅਤੇ ਗਤੀ।",
    "pl": "Główny przełącznik, przejścia ekranów, styl przejścia i szybkość.",
    "pt": "O interruptor principal, as transições de ecrã, o estilo de transição e a velocidade.",
    "pt-rBR": "O interruptor principal, as transições de tela, o estilo de transição e a velocidade.",
    "ro": "Comutatorul principal, tranzițiile ecranelor, stilul tranziției și viteza.",
    "ru": "Главный переключатель, переходы между экранами, стиль перехода и скорость.",
    "sk": "Hlavný prepínač, prechody obrazoviek, štýl prechodu a rýchlosť.",
    "sl": "Glavno stikalo, prehodi zaslona, slog prehoda in hitrost.",
    "sr": "Главни прекидач, прелази екрана, стил прелаза и брзина.",
    "sv": "Huvudbrytaren, skärmövergångarna, övergångsstilen och hastigheten.",
    "ta": "முதன்மைச் சுவிட்ச், திரை மாற்றங்கள், மாற்றப் பாணி மற்றும் வேகம்.",
    "te": "ప్రధాన స్విచ్, స్క్రీన్ ట్రాన్సిషన్లు, ట్రాన్సిషన్ శైలి మరియు వేగం.",
    "th": "สวิตช์หลัก การเปลี่ยนหน้าจอ รูปแบบการเปลี่ยน และความเร็ว",
    "tr": "Ana anahtar, ekran geçişleri, geçiş stili ve hız.",
    "uk": "Головний перемикач, переходи між екранами, стиль переходу та швидкість.",
    "vi": "Công tắc chính, hiệu ứng chuyển màn hình, kiểu chuyển và tốc độ.",
    "zh-rCN": "主开关、屏幕切换、切换样式和速度。",
    "zh-rTW": "主開關、畫面切換、切換樣式與速度。",
}

EXTRA_TRANSLATIONS = {
    "animations_hub_desc": ANIMATIONS_HUB_DESC,
}
