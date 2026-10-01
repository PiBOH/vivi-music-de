# -*- coding: utf-8 -*-
"""`spotify_google_blocked` — the message the Spotify sign-in window shows when
Google refuses to serve its sign-in page inside the app.

Google answers any embedded browser with `disallowed_useragent` ("This browser or
app may not be secure"), which is what the user reported: pressing "Continue with
Google" on Spotify's login page lands on Google's block page and the sign-in can
never be finished there. The window now notices it and says what to do instead:
go back (the Retry button) and use Spotify's own email/password form.

The key has no Android resource behind it (`:spotify`/`app` have no equivalent),
so every language is written out here. A language left out falls back to English
(`Localization.get`), which the audit then reports as a gap — never a raw key.

"Google" and "Spotify" stay Latin on purpose: they are product names, and the
audit's script check ignores Latin letters.
"""

EXTRA_TRANSLATIONS = {
    "spotify_google_blocked": {
        "ar": "لا يسمح Google بتسجيل الدخول داخل نافذة التطبيق. اضغط على «إعادة المحاولة» ثم سجّل الدخول ببريد Spotify وكلمة المرور (عيّن كلمة مرور على spotify.com أولًا إذا أنشأت الحساب عبر Google).",
        "as": "Google-এ এপৰ উইণ্ডোৰ ভিতৰত ছাইন ইন কৰিবলৈ অনুমতি নিদিয়ে। 'পুনৰ চেষ্টা কৰক' টিপি Spotify-ৰ ইমেইল আৰু পাছৱৰ্ডেৰে ছাইন ইন কৰক (Google-ৰ জৰিয়তে একাউণ্ট বনোৱা হ'লে প্ৰথমে spotify.com-ত এটা পাছৱৰ্ড সাজি লওক)।",
        "az": "Google tətbiqin pəncərəsi daxilində girişə icazə vermir. \"Yenidən cəhd edin\" düyməsini basın və Spotify e-poçtunuzla və parolunuzla daxil olun (hesabı Google ilə yaratmısınızsa, əvvəlcə spotify.com saytında parol təyin edin).",
        "be": "Google не дазваляе ўваходзіць унутры акна праграмы. Націсніце «Паўтарыць спробу» і ўвайдзіце праз электронную пошту і пароль Spotify (калі ўліковы запіс створаны праз Google, спачатку задайце пароль на spotify.com).",
        "bg": "Google не позволява влизане в прозореца на приложението. Натиснете „Опитай отново“ и влезте с имейла и паролата си за Spotify (ако акаунтът е създаден с Google, първо задайте парола на spotify.com).",
        "bn": "Google অ্যাপের উইন্ডোর ভিতরে সাইন-ইন করতে দেয় না। 'আবার চেষ্টা করুন' চাপুন এবং Spotify-এর ইমেল ও পাসওয়ার্ড দিয়ে সাইন ইন করুন (Google দিয়ে অ্যাকাউন্ট তৈরি হলে আগে spotify.com-এ একটি পাসওয়ার্ড সেট করুন)।",
        "bs": "Google ne dozvoljava prijavu unutar prozora aplikacije. Pritisni \"Pokušaj ponovo\" i prijavi se svojim Spotify e-mailom i lozinkom (ako je račun napravljen preko Googlea, prvo postavi lozinku na spotify.com).",
        "ca": "Google no permet iniciar sessió dins de la finestra de l'aplicació. Prem «Torna-ho a provar» i inicia sessió amb el correu i la contrasenya de Spotify (si el compte es va crear amb Google, defineix primer una contrasenya a spotify.com).",
        "cs": "Google neumožňuje přihlášení uvnitř okna aplikace. Stiskni „Zkusit znovu“ a přihlas se e-mailem a heslem k Spotify (pokud byl účet vytvořen přes Google, nejprve si nastav heslo na spotify.com).",
        "de": "Google erlaubt keine Anmeldung im Fenster der App. Klicke auf „Erneut versuchen“ und melde dich mit deiner Spotify-E-Mail und deinem Passwort an (lege zuerst ein Passwort auf spotify.com fest, wenn das Konto mit Google erstellt wurde).",
        "el": "Το Google δεν επιτρέπει τη σύνδεση μέσα στο παράθυρο της εφαρμογής. Πάτησε «Δοκιμή ξανά» και συνδέσου με το email και τον κωδικό σου στο Spotify (αν ο λογαριασμός δημιουργήθηκε με Google, όρισε πρώτα κωδικό στο spotify.com).",
        "es": "Google no permite iniciar sesión dentro de la ventana de la aplicación. Pulsa «Reintentar» e inicia sesión con tu correo y contraseña de Spotify (si la cuenta se creó con Google, define primero una contraseña en spotify.com).",
        "et": "Google ei luba rakenduse aknas sisse logida. Vajuta „Proovi uuesti“ ja logi sisse Spotify e-posti ja parooliga (kui konto loodi Google'iga, määra esmalt parool saidil spotify.com).",
        "eu": "Googlek ez du uzten aplikazioaren leihoan saioa hasten. Sakatu «Saiatu berriro» eta hasi saioa Spotify-ko posta eta pasahitzarekin (kontua Google-rekin sortu bada, ezarri lehenik pasahitz bat spotify.com-en).",
        "fa": "‏Google اجازهٔ ورود در پنجرهٔ برنامه را نمی‌دهد. «تلاش مجدد» را بزن و با ایمیل و رمز Spotify وارد شو (اگر حساب با Google ساخته شده، ابتدا در spotify.com رمز تعیین کن).",
        "fi": "Google ei salli kirjautumista sovelluksen ikkunassa. Paina ”Yritä uudelleen” ja kirjaudu Spotify-sähköpostillasi ja salasanallasi (jos tili luotiin Googlella, aseta ensin salasana osoitteessa spotify.com).",
        "fil": "Hindi pinapayagan ng Google ang pag-sign in sa loob ng window ng app. Pindutin ang \"Subukan muli\" at mag-sign in gamit ang iyong Spotify email at password (kung ginawa ang account sa Google, magtakda muna ng password sa spotify.com).",
        "fr": "Google n'autorise pas la connexion dans la fenêtre de l'application. Clique sur « Réessayer » et connecte-toi avec ton e-mail et ton mot de passe Spotify (si le compte a été créé avec Google, définis d'abord un mot de passe sur spotify.com).",
        "hi": "Google ऐप की विंडो के भीतर साइन इन की अनुमति नहीं देता। \"पुनः प्रयास करें\" दबाएँ और अपने Spotify ईमेल और पासवर्ड से साइन इन करें (अगर खाता Google से बनाया गया है, तो पहले spotify.com पर पासवर्ड सेट करें)।",
        "hr": "Google ne dopušta prijavu unutar prozora aplikacije. Pritisni \"Pokušaj ponovno\" i prijavi se svojim Spotify e-mailom i lozinkom (ako je račun izrađen putem Googlea, prvo postavi lozinku na spotify.com).",
        "hu": "A Google nem engedélyezi a bejelentkezést az alkalmazás ablakában. Nyomd meg az „Újra” gombot, és jelentkezz be a Spotify e-mail-címeddel és jelszavaddal (ha a fiók Google-lal készült, előbb állíts be jelszót a spotify.com oldalon).",
        "id": "Google tidak mengizinkan masuk di dalam jendela aplikasi. Tekan \"Coba lagi\" dan masuk dengan email dan kata sandi Spotify Anda (jika akun dibuat dengan Google, atur kata sandi di spotify.com lebih dulu).",
        "it": "Google non consente l'accesso dentro la finestra dell'app. Premi «Riprova» e accedi con l'email e la password di Spotify (se l'account è stato creato con Google, imposta prima una password su spotify.com).",
        "iw": "‏Google אינה מאפשרת התחברות בתוך חלון האפליקציה. לחץ על \"נסה שוב\" והתחבר עם הדוא\"ל והסיסמה של Spotify (אם החשבון נוצר עם Google, קבע תחילה סיסמה ב-spotify.com).",
        "ja": "Google はアプリのウィンドウ内でのサインインを許可していません。「再試行」を押し、Spotify のメールアドレスとパスワードでサインインしてください（Google でアカウントを作成した場合は、先に spotify.com でパスワードを設定してください）。",
        "km": "Google មិនអនុញ្ញាតឱ្យចូលក្នុងវីនដូរបស់កម្មវិធីទេ។ ចុច «ព្យាយាមម្តងទៀត» ហើយចូលដោយអ៊ីមែល និងពាក្យសម្ងាត់ Spotify របស់អ្នក (បើគណនីបង្កើតដោយ Google សូមកំណត់ពាក្យសម្ងាត់នៅ spotify.com ជាមុន)។",
        "ko": "Google은 앱 창 안에서의 로그인을 허용하지 않습니다. \"다시 시도\"를 누르고 Spotify 이메일과 비밀번호로 로그인하세요(Google로 계정을 만들었다면 먼저 spotify.com에서 비밀번호를 설정하세요).",
        "lt": "„Google“ neleidžia prisijungti programos lango viduje. Spausk „Bandyti dar kartą“ ir prisijunk su „Spotify“ el. paštu ir slaptažodžiu (jei paskyra sukurta su „Google“, pirmiausia nustatyk slaptažodį spotify.com).",
        "ml": "ആപ്പിന്റെ ജാലകത്തിനുള്ളിൽ സൈൻ ഇൻ ചെയ്യാൻ Google അനുവദിക്കുന്നില്ല. \"വീണ്ടും ശ്രമിക്കുക\" അമർത്തി നിങ്ങളുടെ Spotify ഇമെയിലും പാസ്‌വേഡും ഉപയോഗിച്ച് സൈൻ ഇൻ ചെയ്യുക (Google ഉപയോഗിച്ച് അക്കൗണ്ട് ഉണ്ടാക്കിയതാണെങ്കിൽ ആദ്യം spotify.com-ൽ ഒരു പാസ്‌വേഡ് സെറ്റ് ചെയ്യുക).",
        "ms": "Google tidak membenarkan daftar masuk di dalam tetingkap aplikasi. Tekan \"Cuba lagi\" dan daftar masuk dengan e-mel dan kata laluan Spotify anda (jika akaun dibuat dengan Google, tetapkan kata laluan di spotify.com dahulu).",
        "nb": "Google tillater ikke pålogging inne i appens vindu. Trykk på «Prøv igjen» og logg inn med Spotify-e-posten og passordet ditt (hvis kontoen ble opprettet med Google, angir du først et passord på spotify.com).",
        "nl": "Google staat aanmelden in het venster van de app niet toe. Klik op 'Opnieuw proberen' en meld je aan met je Spotify-e-mailadres en wachtwoord (stel eerst een wachtwoord in op spotify.com als het account met Google is aangemaakt).",
        "pa": "Google ਐਪ ਦੀ ਵਿੰਡੋ ਵਿੱਚ ਸਾਈਨ ਇਨ ਕਰਨ ਦੀ ਆਗਿਆ ਨਹੀਂ ਦਿੰਦਾ। \"ਦੁਬਾਰਾ ਕੋਸ਼ਿਸ਼ ਕਰੋ\" ਦਬਾਓ ਅਤੇ ਆਪਣੇ Spotify ਈਮੇਲ ਅਤੇ ਪਾਸਵਰਡ ਨਾਲ ਸਾਈਨ ਇਨ ਕਰੋ (ਜੇ ਖਾਤਾ Google ਨਾਲ ਬਣਾਇਆ ਹੈ ਤਾਂ ਪਹਿਲਾਂ spotify.com 'ਤੇ ਪਾਸਵਰਡ ਸੈੱਟ ਕਰੋ)।",
        "pl": "Google nie pozwala zalogować się w oknie aplikacji. Naciśnij „Spróbuj ponownie” i zaloguj się swoim e-mailem i hasłem do Spotify (jeśli konto utworzono przez Google, najpierw ustaw hasło na spotify.com).",
        "pt": "O Google não permite iniciar sessão dentro da janela da aplicação. Prime «Tentar novamente» e inicia sessão com o teu e-mail e palavra-passe do Spotify (se a conta foi criada com o Google, define primeiro uma palavra-passe em spotify.com).",
        "pt-rBR": "O Google não permite entrar dentro da janela do aplicativo. Toque em \"Tentar novamente\" e entre com seu e-mail e senha do Spotify (se a conta foi criada com o Google, defina primeiro uma senha em spotify.com).",
        "ro": "Google nu permite conectarea în fereastra aplicației. Apasă „Încearcă din nou” și conectează-te cu e-mailul și parola de Spotify (dacă contul a fost creat cu Google, setează mai întâi o parolă pe spotify.com).",
        "ru": "Google не разрешает вход внутри окна приложения. Нажми «Повторить» и войди с электронной почтой и паролем Spotify (если аккаунт создан через Google, сначала задай пароль на spotify.com).",
        "sk": "Google neumožňuje prihlásenie v okne aplikácie. Stlač „Skúsiť znova“ a prihlás sa e-mailom a heslom k Spotify (ak bol účet vytvorený cez Google, najprv si nastav heslo na spotify.com).",
        "sl": "Google ne dovoli prijave v oknu aplikacije. Pritisni »Poskusi znova« in se prijavi z e-pošto in geslom za Spotify (če je bil račun ustvarjen z Googlom, najprej nastavi geslo na spotify.com).",
        "sr": "Google не дозвољава пријаву унутар прозора апликације. Притисни „Покушај поново“ и пријави се својим Spotify имејлом и лозинком (ако је налог направљен преко Google-а, прво постави лозинку на spotify.com).",
        "sv": "Google tillåter inte inloggning i appens fönster. Tryck på ”Försök igen” och logga in med din Spotify-e-post och ditt lösenord (om kontot skapades med Google, ange först ett lösenord på spotify.com).",
        "ta": "Google ஆப்பின் சாளரத்திற்குள் உள்நுழைவதை அனுமதிக்காது. \"மீண்டும் முயற்சிக்கவும்\" என்பதை அழுத்தி உங்கள் Spotify மின்னஞ்சல் மற்றும் கடவுச்சொல்லுடன் உள்நுழையுங்கள் (Google மூலம் கணக்கு உருவாக்கப்பட்டிருந்தால், முதலில் spotify.com-இல் கடவுச்சொல்லை அமைக்கவும்).",
        "te": "Google యాప్ విండోలో సైన్ ఇన్ చేయడాన్ని అనుమతించదు. \"మళ్ళీ ప్రయత్నించండి\" నొక్కి మీ Spotify ఇమెయిల్ మరియు పాస్‌వర్డ్‌తో సైన్ ఇన్ చేయండి (Googleతో ఖాతా సృష్టించినట్లయితే, ముందుగా spotify.comలో పాస్‌వర్డ్ సెట్ చేయండి).",
        "th": "Google ไม่อนุญาตให้ลงชื่อเข้าใช้ภายในหน้าต่างของแอป กด \"ลองอีกครั้ง\" แล้วลงชื่อเข้าใช้ด้วยอีเมลและรหัสผ่าน Spotify ของคุณ (หากสร้างบัญชีด้วย Google ให้ตั้งรหัสผ่านที่ spotify.com ก่อน)",
        "tr": "Google, uygulamanın penceresi içinde oturum açmaya izin vermiyor. \"Tekrar dene\"ye bas ve Spotify e-postan ve şifrenle oturum aç (hesabı Google ile oluşturduysan önce spotify.com'da bir şifre belirle).",
        "uk": "Google не дозволяє вхід усередині вікна програми. Натисни «Повторити» і увійди з електронною поштою та паролем Spotify (якщо акаунт створено через Google, спершу задай пароль на spotify.com).",
        "vi": "Google không cho phép đăng nhập trong cửa sổ của ứng dụng. Nhấn \"Thử lại\" và đăng nhập bằng email và mật khẩu Spotify của bạn (nếu tài khoản được tạo bằng Google, hãy đặt mật khẩu trên spotify.com trước).",
        "zh-rCN": "Google 不允许在应用窗口内登录。请按“重试”，然后用你的 Spotify 邮箱和密码登录（如果账号是用 Google 创建的，请先在 spotify.com 上设置密码）。",
        "zh-rTW": "Google 不允許在應用程式視窗內登入。請按「重試」，然後用你的 Spotify 電子郵件與密碼登入（若帳號是用 Google 建立的，請先在 spotify.com 設定密碼）。",
    },
}
