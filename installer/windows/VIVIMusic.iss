; VIVI Music DE — Windows installer (Inno Setup).
;
; The self-contained app image is produced by jpackage (Gradle
; `createDistributable`) and passed in through /DSourceDir. Version and icon
; paths are also supplied from the CI workflow so this file stays the single
; source of the installer layout.
;
; ---------------------------------------------------------------------------
; Branding: why the wizard used to show the Inno Setup logo
; ---------------------------------------------------------------------------
; Inno Setup has its OWN wizard artwork built in — a large image on the left of
; the Welcome and Finished pages and a small one in the header of every other
; page. When `WizardImageFile` / `WizardSmallImageFile` are not declared, that
; artwork is what the user sees, so the setup advertised Inno Setup instead of
; VIVI Music DE. Both are declared below and point at the brand bitmaps kept in
; `desktop/icons` (the same mark as the app icon): the images already existed,
; nothing referenced them.
;
; ---------------------------------------------------------------------------
; Languages
; ---------------------------------------------------------------------------
; Every language the program itself supports is selectable, over 51 entries.
; English is first and English is the default (`LanguageDetectionMethod=none`
; ignores the Windows UI language); the Select Language page is always shown so
; the choice stays the user's.
;
; Where Inno Setup ships a translation of its own wizard (29 of them, in
; `compiler:Languages`), that file is loaded and the whole wizard follows the
; choice. For the other languages the wizard chrome stays English — there is no
; translation to load — while everything THIS installer authors does follow the
; selection: the task and Run descriptions are `{cm:...}` lookups into Inno's
; own (already translated) tables, plus the one [CustomMessages] entry below.

#ifndef AppVersion
#define AppVersion "0.0.0-dev"
#endif
#ifndef InstallerVersion
#define InstallerVersion "0.0.0"
#endif
#ifndef SourceDir
#define SourceDir "."
#endif
#ifndef OutputDir
#define OutputDir "dist"
#endif
; Defaults resolve against the script's own location, so the installer can be
; compiled straight from the repo without a single /D. CI passes them as
; absolute paths anyway.
#define RepoRoot AddBackslash(SourcePath) + "..\.."
#ifndef IconFile
#define IconFile RepoRoot + "\desktop\icons\logo_vmde.ico"
#endif
#ifndef WizardImageFile
#define WizardImageFile RepoRoot + "\desktop\icons\logo_vmde_wizard.bmp"
#endif
#ifndef WizardSmallImageFile
#define WizardSmallImageFile RepoRoot + "\desktop\icons\logo_vmde_small.bmp"
#endif

#define AppName "VIVI Music DE"
#define AppExe "VIVIMusic.exe"
#define AppPublisher "PiBOH"
#define AppId "com.vivi.vivimusic.desktop"
#define AppURL "https://github.com/PiBOH/vivi-music-de"

[Setup]
AppId={#AppId}
AppName={#AppName}
; Inno Setup requires a numeric application version for AppVersion; the full
; `-DE` SemVer stays visible in AppVerName, in the Programs & Features entry and
; in the output filename.
AppVersion={#InstallerVersion}
AppVerName={#AppName} {#AppVersion}
AppPublisher={#AppPublisher}
AppPublisherURL={#AppURL}
AppSupportURL={#AppURL}
AppUpdatesURL={#AppURL}/releases
AppComments={#AppName} — the desktop edition of VIVI Music.
DefaultDirName={autopf}\VIVIMusic
DefaultGroupName={#AppName}
DisableProgramGroupPage=yes
; Always show the "Select Destination Location" page so the user can see (and
; change) the folder the app will install into — matching the MSI installer,
; which shows the destination path.
DisableDirPage=no
; Keep an existing install's folder, shortcuts and language when the setup is
; run again (the usual upgrade in place) instead of re-asking every time.
UsePreviousAppDir=yes
UsePreviousTasks=yes
UsePreviousLanguage=yes
AllowNoIcons=yes
; The installer's own log in %TEMP%: the app ships a log exporter, so a failed
; install should be diagnosable the same way.
SetupLogging=yes
AllowUNCPath=no
OutputDir={#OutputDir}
OutputBaseFilename=VIVIMusic-{#AppVersion}-setup
SetupIconFile={#IconFile}
; --- Branding: the wizard's own artwork (see the file header) ---------------
WizardStyle=modern
WizardImageFile={#WizardImageFile}
WizardSmallImageFile={#WizardSmallImageFile}
; --- Language selection -----------------------------------------------------
; `none` = never guess from the Windows UI language, so the first [Languages]
; entry (English) is the default. The dialog is always shown, so the choice is
; the user's either way.
LanguageDetectionMethod=none
ShowLanguageDialog=yes
; Maximize compression: the shipped image is trimmed (see MinimizeIconsJarTask in
; desktop/build.gradle.kts), so the lzma2/max + solid pass is affordable and the
; setup stays as small as possible. There are no optional [Components], so solid
; compression does not make a partial install decompress the whole block.
Compression=lzma2/max
SolidCompression=yes
PrivilegesRequired=admin
ArchitecturesAllowed=x64compatible
ArchitecturesInstallIn64BitMode=x64compatible
CloseApplications=yes
RestartApplications=no
Uninstallable=yes
UninstallDisplayName={#AppName} {#AppVersion}
UninstallDisplayIcon={app}\{#AppExe}
VersionInfoCompany={#AppPublisher}
VersionInfoDescription={#AppName} desktop client
VersionInfoProductName={#AppName}
VersionInfoVersion={#InstallerVersion}
VersionInfoProductVersion={#InstallerVersion}
VersionInfoProductTextVersion={#AppVersion}
VersionInfoTextVersion={#AppVersion}
VersionInfoCopyright=Copyright (c) 2026 PiBOH

[Languages]
; --- English, always the default (first entry) ------------------------------
Name: "english"; MessagesFile: "compiler:Default.isl"

; --- Full wizard translations shipped with Inno Setup -----------------------
Name: "arabic"; MessagesFile: "compiler:Languages\Arabic.isl"
Name: "brazilianportuguese"; MessagesFile: "compiler:Languages\BrazilianPortuguese.isl"
Name: "bulgarian"; MessagesFile: "compiler:Languages\Bulgarian.isl"
Name: "catalan"; MessagesFile: "compiler:Languages\Catalan.isl"
Name: "chinesesimplified"; MessagesFile: "compiler:Languages\ChineseSimplified.isl"
Name: "chinesetraditional"; MessagesFile: "compiler:Languages\ChineseTraditional.isl"
Name: "czech"; MessagesFile: "compiler:Languages\Czech.isl"
Name: "dutch"; MessagesFile: "compiler:Languages\Dutch.isl"
Name: "finnish"; MessagesFile: "compiler:Languages\Finnish.isl"
Name: "french"; MessagesFile: "compiler:Languages\French.isl"
Name: "german"; MessagesFile: "compiler:Languages\German.isl"
Name: "hebrew"; MessagesFile: "compiler:Languages\Hebrew.isl"
Name: "hungarian"; MessagesFile: "compiler:Languages\Hungarian.isl"
Name: "italian"; MessagesFile: "compiler:Languages\Italian.isl"
Name: "japanese"; MessagesFile: "compiler:Languages\Japanese.isl"
Name: "korean"; MessagesFile: "compiler:Languages\Korean.isl"
Name: "lithuanian"; MessagesFile: "compiler:Languages\Lithuanian.isl"
Name: "norwegian"; MessagesFile: "compiler:Languages\Norwegian.isl"
Name: "polish"; MessagesFile: "compiler:Languages\Polish.isl"
Name: "portuguese"; MessagesFile: "compiler:Languages\Portuguese.isl"
Name: "russian"; MessagesFile: "compiler:Languages\Russian.isl"
Name: "slovak"; MessagesFile: "compiler:Languages\Slovak.isl"
Name: "slovenian"; MessagesFile: "compiler:Languages\Slovenian.isl"
Name: "spanish"; MessagesFile: "compiler:Languages\Spanish.isl"
Name: "swedish"; MessagesFile: "compiler:Languages\Swedish.isl"
Name: "tamil"; MessagesFile: "compiler:Languages\Tamil.isl"
Name: "thai"; MessagesFile: "compiler:Languages\Thai.isl"
Name: "turkish"; MessagesFile: "compiler:Languages\Turkish.isl"
Name: "ukrainian"; MessagesFile: "compiler:Languages\Ukrainian.isl"

; --- Supported by the program, no wizard translation in Inno Setup ----------
; `LanguageName` is what the Select Language page shows, so each of these is
; still listed under its own name even though the wizard chrome falls back to
; English (Default.isl).
Name: "assamese"; MessagesFile: "compiler:Default.isl,languages\assamese.isl"
Name: "azerbaijani"; MessagesFile: "compiler:Default.isl,languages\azerbaijani.isl"
Name: "basque"; MessagesFile: "compiler:Default.isl,languages\basque.isl"
Name: "belarusian"; MessagesFile: "compiler:Default.isl,languages\belarusian.isl"
Name: "bengali"; MessagesFile: "compiler:Default.isl,languages\bengali.isl"
Name: "bosnian"; MessagesFile: "compiler:Default.isl,languages\bosnian.isl"
Name: "croatian"; MessagesFile: "compiler:Default.isl,languages\croatian.isl"
Name: "estonian"; MessagesFile: "compiler:Default.isl,languages\estonian.isl"
Name: "filipino"; MessagesFile: "compiler:Default.isl,languages\filipino.isl"
Name: "greek"; MessagesFile: "compiler:Default.isl,languages\greek.isl"
Name: "hindi"; MessagesFile: "compiler:Default.isl,languages\hindi.isl"
Name: "indonesian"; MessagesFile: "compiler:Default.isl,languages\indonesian.isl"
Name: "khmer"; MessagesFile: "compiler:Default.isl,languages\khmer.isl"
Name: "malay"; MessagesFile: "compiler:Default.isl,languages\malay.isl"
Name: "malayalam"; MessagesFile: "compiler:Default.isl,languages\malayalam.isl"
Name: "persian"; MessagesFile: "compiler:Default.isl,languages\persian.isl"
Name: "punjabi"; MessagesFile: "compiler:Default.isl,languages\punjabi.isl"
Name: "romanian"; MessagesFile: "compiler:Default.isl,languages\romanian.isl"
Name: "serbian"; MessagesFile: "compiler:Default.isl,languages\serbian.isl"
Name: "telugu"; MessagesFile: "compiler:Default.isl,languages\telugu.isl"
Name: "vietnamese"; MessagesFile: "compiler:Default.isl,languages\vietnamese.isl"

[Tasks]
; Descriptions come from Inno's own message tables — already translated in every
; language that has a wizard translation.
Name: "desktopicon"; Description: "{cm:CreateDesktopIcon}"; GroupDescription: "{cm:AdditionalIcons}"; Flags: unchecked

[Files]
Source: "{#SourceDir}\*"; DestDir: "{app}"; Flags: ignoreversion recursesubdirs createallsubdirs

[Icons]
; The Start Menu shortcut is unconditional: it is the normal way in, and the task
; that used to gate it could only be described with a literal English string
; (Inno ships no message for it) in all 51 languages.
Name: "{group}\{#AppName}"; Filename: "{app}\{#AppExe}"
Name: "{autodesktop}\{#AppName}"; Filename: "{app}\{#AppExe}"; Tasks: desktopicon

[Registry]
; Makes the app launchable by name from the Run dialog / the Explorer address
; bar (`VIVIMusic`), the way most Windows apps register themselves.
Root: HKLM; Subkey: "SOFTWARE\Microsoft\Windows\CurrentVersion\App Paths\{#AppExe}"; ValueType: string; ValueName: ""; ValueData: "{app}\{#AppExe}"; Flags: uninsdeletekey

[Run]
Filename: "{app}\{#AppExe}"; Description: "{cm:LaunchProgram,{#AppName}}"; Flags: nowait postinstall skipifsilent

; ---------------------------------------------------------------------------
; Installer's own strings
; ---------------------------------------------------------------------------
; Everything else the user reads is a standard Inno Setup message and follows
; the selected language on its own. What is left is the one sentence this
; installer invented — the note above the two paths the uninstaller keeps.
[CustomMessages]
english.UninstallKept=Only your newest backup and your settings were kept:
italian.UninstallKept=Sono stati conservati solo il backup più recente e le tue impostazioni:
arabic.UninstallKept=تم الاحتفاظ فقط بأحدث نسخة احتياطية وإعداداتك:
assamese.UninstallKept=কেৱল আপোনাৰ শেহতীয়া বেকআপ আৰু আপোনাৰ ছেটিংছহে ৰখা হৈছে:
azerbaijani.UninstallKept=Yalnız ən yeni ehtiyat nüsxəniz və parametrləriniz saxlanıldı:
basque.UninstallKept=Zure azken babeskopia eta zure ezarpenak bakarrik gorde dira:
belarusian.UninstallKept=Захаваныя толькі апошняя рэзервовая копія і вашы налады:
bengali.UninstallKept=শুধু আপনার সর্বশেষ ব্যাকআপ আর আপনার সেটিংস রাখা হয়েছে:
bosnian.UninstallKept=Sačuvani su samo najnovija sigurnosna kopija i vaše postavke:
brazilianportuguese.UninstallKept=Só o seu backup mais recente e as suas configurações foram mantidos:
bulgarian.UninstallKept=Запазени са само най-новият архив и вашите настройки:
catalan.UninstallKept=Només s'han conservat la còpia de seguretat més recent i la configuració:
chinesesimplified.UninstallKept=仅保留了最新的备份和您的设置：
chinesetraditional.UninstallKept=僅保留了最新的備份與您的設定：
croatian.UninstallKept=Sačuvane su samo najnovija sigurnosna kopija i vaše postavke:
czech.UninstallKept=Zachována byla pouze nejnovější záloha a vaše nastavení:
dutch.UninstallKept=Alleen je nieuwste back-up en je instellingen zijn bewaard:
estonian.UninstallKept=Alles jäid vaid uusim varukoopia ja sinu seaded:
filipino.UninstallKept=Ang pinakabagong backup at ang iyong mga setting lang ang itinira:
finnish.UninstallKept=Vain uusin varmuuskopio ja asetuksesi säilytettiin:
french.UninstallKept=Seules votre sauvegarde la plus récente et vos réglages ont été conservés :
german.UninstallKept=Behalten wurden nur deine neueste Sicherung und deine Einstellungen:
greek.UninstallKept=Διατηρήθηκαν μόνο το πιο πρόσφατο αντίγραφο ασφαλείας και οι ρυθμίσεις σας:
hebrew.UninstallKept=נשמרו רק הגיבוי האחרון וההגדרות שלך:
hindi.UninstallKept=केवल आपका नवीनतम बैकअप और आपकी सेटिंग्स रखी गई हैं:
hungarian.UninstallKept=Csak a legfrissebb biztonsági mentés és a beállításaid maradtak meg:
indonesian.UninstallKept=Hanya cadangan terbaru dan pengaturan Anda yang disimpan:
japanese.UninstallKept=最新のバックアップと設定のみを残しました:
khmer.UninstallKept=មានតែការបម្រុងទុកចុងក្រោយបំផុត និងការកំណត់របស់អ្នកប៉ុណ្ណោះដែលត្រូវបានរក្សាទុក៖
korean.UninstallKept=최신 백업과 설정만 남겼습니다:
lithuanian.UninstallKept=Išsaugota tik naujausia atsarginė kopija ir jūsų nustatymai:
malay.UninstallKept=Hanya sandaran terbaharu dan tetapan anda disimpan:
malayalam.UninstallKept=ഏറ്റവും പുതിയ ബാക്കപ്പും നിങ്ങളുടെ ക്രമീകരണങ്ങളും മാത്രം സൂക്ഷിച്ചിരിക്കുന്നു:
norwegian.UninstallKept=Bare den nyeste sikkerhetskopien og innstillingene dine ble beholdt:
persian.UninstallKept=فقط جدیدترین پشتیبان و تنظیمات شما نگه داشته شد:
polish.UninstallKept=Zachowano tylko najnowszą kopię zapasową i Twoje ustawienia:
portuguese.UninstallKept=Apenas a cópia de segurança mais recente e as suas definições foram mantidas:
punjabi.UninstallKept=ਸਿਰਫ਼ ਤੁਹਾਡਾ ਸਭ ਤੋਂ ਨਵਾਂ ਬੈਕਅੱਪ ਅਤੇ ਤੁਹਾਡੀਆਂ ਸੈਟਿੰਗਾਂ ਰੱਖੀਆਂ ਗਈਆਂ ਹਨ:
romanian.UninstallKept=Au fost păstrate doar cea mai recentă copie de siguranță și setările tale:
russian.UninstallKept=Сохранены только последняя резервная копия и ваши настройки:
serbian.UninstallKept=Сачувани су само најновија резервна копија и ваша подешавања:
slovak.UninstallKept=Zachovaná bola iba najnovšia záloha a vaše nastavenia:
slovenian.UninstallKept=Ohranjena sta bila samo najnovejša varnostna kopija in vaše nastavitve:
spanish.UninstallKept=Solo se han conservado tu copia de seguridad más reciente y tus ajustes:
swedish.UninstallKept=Endast din senaste säkerhetskopia och dina inställningar behölls:
tamil.UninstallKept=உங்கள் சமீபத்திய காப்புப்பிரதியும் உங்கள் அமைப்புகளும் மட்டுமே வைக்கப்பட்டன:
telugu.UninstallKept=మీ తాజా బ్యాకప్ మరియు మీ సెట్టింగ్‌లు మాత్రమే ఉంచబడ్డాయి:
thai.UninstallKept=เก็บไว้เฉพาะข้อมูลสำรองล่าสุดและการตั้งค่าของคุณเท่านั้น:
turkish.UninstallKept=Yalnızca en yeni yedeğiniz ve ayarlarınız saklandı:
ukrainian.UninstallKept=Збережено лише найновішу резервну копію та ваші налаштування:
vietnamese.UninstallKept=Chỉ giữ lại bản sao lưu mới nhất và cài đặt của bạn:

[Code]
var
  InstallDetailsMemo: TNewMemo;
  UninstallDetailsMemo: TNewMemo;
  LastInstallLine: String;
  UninstallCleanupOk: Boolean;

// Uninstall any previously-installed jpackage MSI of this app. The MSI and this
// Inno Setup installer are two different installer technologies, so each
// registers its own entry under "Apps & features". Removing the MSI here keeps
// a single uninstall entry when the user installs the .exe after the .msi.
procedure UninstallExistingMsi();
var
  RootKeys: array of String;
  Names: TArrayOfString;
  I, J: Integer;
  DisplayName, UninstallString: String;
  ResultCode: Integer;
begin
  SetArrayLength(RootKeys, 2);
  RootKeys[0] := 'SOFTWARE\Microsoft\Windows\CurrentVersion\Uninstall';
  RootKeys[1] := 'SOFTWARE\WOW6432Node\Microsoft\Windows\CurrentVersion\Uninstall';
  for J := 0 to GetArrayLength(RootKeys) - 1 do
  begin
    if RegGetSubkeyNames(HKLM, RootKeys[J], Names) then
    begin
      for I := 0 to GetArrayLength(Names) - 1 do
      begin
        if RegQueryStringValue(HKLM, RootKeys[J] + '\' + Names[I], 'DisplayName', DisplayName) and
           RegQueryStringValue(HKLM, RootKeys[J] + '\' + Names[I], 'UninstallString', UninstallString) then
        begin
          if (Pos('VIVI', Uppercase(DisplayName)) > 0) and (Pos('MSIEXEC', Uppercase(UninstallString)) > 0) then
          begin
            Exec('msiexec.exe', '/x ' + Names[I] + ' /qn /norestart', '', SW_HIDE, ewWaitUntilTerminated, ResultCode);
          end;
        end;
      end;
    end;
  end;
end;

// ---------------------------------------------------------------------------
// Details/log box helpers
// ---------------------------------------------------------------------------
// Inno Setup 6 removed the built-in "Show details" memo/button that existed in
// Inno Setup 5 (there is no WizardForm.Memo / DetailsMemo / DetailsButton), so
// the log box is created at runtime below the progress gauge and filled with
// the files being extracted (install) and the cleanup steps (uninstall).

procedure AddInstallLine(const Line: String);
begin
  if InstallDetailsMemo = nil then Exit;
  if Line = '' then Exit;
  if InstallDetailsMemo.Lines.Count > 2000 then
    InstallDetailsMemo.Lines.Clear;
  InstallDetailsMemo.Lines.Add(Line);
end;

// The uninstall details box is only usable while the uninstall progress page
// exists: Inno Setup tears that page down as soon as the file-removal step is
// over, and touching the memo afterwards raises "Control 'TNewMemo' has no
// parent window". Every write is therefore guarded - a dead box must never
// abort the uninstallation - and the line falls back to the uninstall log file
// in %TEMP% so the record is never lost.
function UninstallLogPath(): String;
begin
  Result := GetEnv('TEMP') + '\VIVIMusic-uninstall.log';
end;

procedure AddUninstallLine(const Line: String);
begin
  if Line = '' then Exit;
  if UninstallDetailsMemo <> nil then begin
    try
      if UninstallDetailsMemo.Lines.Count > 2000 then
        UninstallDetailsMemo.Lines.Clear;
      UninstallDetailsMemo.Lines.Add(Line);
      Exit;
    except
      { The details box is gone: keep the record on disk instead. }
    end;
  end;
  SaveStringToFile(UninstallLogPath(), Line + #13#10, True);
end;

procedure CreateInstallDetailsMemo();
begin
  if InstallDetailsMemo <> nil then Exit;
  InstallDetailsMemo := TNewMemo.Create(WizardForm);
  InstallDetailsMemo.Parent := WizardForm.InstallingPage;
  InstallDetailsMemo.Left := 0;
  InstallDetailsMemo.Top := WizardForm.ProgressGauge.Top + WizardForm.ProgressGauge.Height + ScaleY(8);
  InstallDetailsMemo.Width := WizardForm.InstallingPage.ClientWidth;
  InstallDetailsMemo.Height := WizardForm.InstallingPage.ClientHeight - InstallDetailsMemo.Top - ScaleY(8);
  InstallDetailsMemo.ReadOnly := True;
  InstallDetailsMemo.ScrollBars := ssVertical;
  InstallDetailsMemo.WordWrap := False;
  InstallDetailsMemo.Font.Name := 'Consolas';
  InstallDetailsMemo.Font.Size := 8;
  LastInstallLine := '';
end;

procedure CreateUninstallDetailsMemo();
begin
  if UninstallDetailsMemo <> nil then Exit;
  if UninstallProgressForm = nil then Exit;
  if UninstallProgressForm.InstallingPage = nil then Exit;
  try
    UninstallDetailsMemo := TNewMemo.Create(UninstallProgressForm);
    UninstallDetailsMemo.Parent := UninstallProgressForm.InstallingPage;
  except
    { No box on this machine: AddUninstallLine writes to the log file instead. }
    Exit;
  end;
  UninstallDetailsMemo.Left := 0;
  UninstallDetailsMemo.Top := UninstallProgressForm.ProgressBar.Top + UninstallProgressForm.ProgressBar.Height + ScaleY(8);
  UninstallDetailsMemo.Width := UninstallProgressForm.InstallingPage.ClientWidth;
  UninstallDetailsMemo.Height := UninstallProgressForm.InstallingPage.ClientHeight - UninstallDetailsMemo.Top - ScaleY(8);
  UninstallDetailsMemo.ReadOnly := True;
  UninstallDetailsMemo.ScrollBars := ssVertical;
  UninstallDetailsMemo.WordWrap := False;
  UninstallDetailsMemo.Font.Name := 'Consolas';
  UninstallDetailsMemo.Font.Size := 8;
end;

// ---------------------------------------------------------------------------
// Uninstall cleanup: keep the newest *.vivide.backup and device-sync.json,
// delete everything else
// ---------------------------------------------------------------------------
// The app keeps its user data under `%USERPROFILE%\.vivimusic`: settings
// (device-sync.json), playlists, imported fonts, the backup history
// (`backups\*.vivide.backup`) and every cache (downloaded updates, audio,
// video/canvas, lyrics, logs, extracted helper libraries and artwork). On
// uninstall exactly two things survive: the NEWEST .vivide.backup - a full
// restore point with settings and playlists - and device-sync.json. Every other
// file, older backups included, and every cache are deleted.

{ "prefix_YYYYMMDD_HHMMSS.vivide.backup" -> "YYYYMMDD_HHMMSS". The timestamp is
  always the last 15 characters before the extension, so comparing those
  strings is chronological even when the prefixes differ (auto_backup_* vs
  vivimusic-de_*). }
function BackupTimestamp(const FileName: String): String;
var
  Base: String;
begin
  Base := Copy(FileName, 1, Length(FileName) - 14);
  if Length(Base) < 15 then
    Result := ''
  else
    Result := Copy(Base, Length(Base) - 14, 15);
end;

procedure BackupAndCleanUserData();
var
  ViviDir, BackupsDir, Entry, ItemPath, KeepBackup, KeepKey, Key: String;
  FindRec: TFindRec;
begin
  ViviDir := GetEnv('USERPROFILE') + '\.vivimusic';
  if not DirExists(ViviDir) then begin
    AddUninstallLine('No user data found at ' + ViviDir + ' - nothing to clean.');
    Exit;
  end;

  BackupsDir := ViviDir + '\backups';

  { Keep the newest backup: the largest timestamp wins. }
  KeepBackup := '';
  KeepKey := '';
  if FindFirst(BackupsDir + '\*.vivide.backup', FindRec) then begin
    try
      repeat
        Key := BackupTimestamp(FindRec.Name);
        if (Key <> '') and (Key > KeepKey) then begin
          KeepKey := Key;
          KeepBackup := FindRec.Name;
        end;
      until not FindNext(FindRec);
    finally
      FindClose(FindRec);
    end;
  end;
  if KeepBackup = '' then
    AddUninstallLine('No .vivide.backup found - nothing to keep.')
  else
    AddUninstallLine('Keeping the newest backup: ' + KeepBackup);

  { Delete everything under ~/.vivimusic except device-sync.json and backups. }
  if FindFirst(ViviDir + '\*', FindRec) then begin
    try
      repeat
        Entry := FindRec.Name;
        if (Entry = '.') or (Entry = '..') then Continue;
        ItemPath := ViviDir + '\' + Entry;
        if (FindRec.Attributes and FILE_ATTRIBUTE_DIRECTORY) <> 0 then begin
          if CompareText(Entry, 'backups') <> 0 then begin
            DelTree(ItemPath, True, True, True);
            AddUninstallLine('Removed cache: ' + Entry);
          end;
        end else begin
          if CompareText(Entry, 'device-sync.json') <> 0 then begin
            DeleteFile(ItemPath);
            AddUninstallLine('Removed file: ' + Entry);
          end;
        end;
      until not FindNext(FindRec);
    finally
      FindClose(FindRec);
    end;
  end;

  { Inside backups/, only the newest .vivide.backup survives. }
  if FindFirst(BackupsDir + '\*', FindRec) then begin
    try
      repeat
        Entry := FindRec.Name;
        if (Entry = '.') or (Entry = '..') then Continue;
        if CompareText(Entry, KeepBackup) <> 0 then begin
          ItemPath := BackupsDir + '\' + Entry;
          if (FindRec.Attributes and FILE_ATTRIBUTE_DIRECTORY) <> 0 then
            DelTree(ItemPath, True, True, True)
          else
            DeleteFile(ItemPath);
          AddUninstallLine('Removed old backup: ' + Entry);
        end;
      until not FindNext(FindRec);
    finally
      FindClose(FindRec);
    end;
  end;

  AddUninstallLine('Uninstall cleanup complete. Only the newest backup and device-sync.json remain.');
end;

// ---------------------------------------------------------------------------
// Installer events
// ---------------------------------------------------------------------------

procedure InitializeWizard();
begin
  CreateInstallDetailsMemo();
end;

procedure CurStepChanged(CurStep: TSetupStep);
begin
  if CurStep = ssInstall then
    UninstallExistingMsi();
end;

procedure CurPageChanged(CurPageID: Integer);
begin
  if CurPageID = wpInstalling then
    CreateInstallDetailsMemo();
end;

// Mirror the file currently being extracted into the details box so the user
// sees a live log of what is being written (the IS5 "Show details" behavior).
procedure CurInstallProgressChanged(Current, Total: Integer);
var
  Line: String;
begin
  Line := Trim(WizardForm.FilenameLabel.Caption);
  if (Line <> '') and (Line <> LastInstallLine) then begin
    LastInstallLine := Line;
    AddInstallLine(Line);
  end;
end;

// ---------------------------------------------------------------------------
// Uninstaller events
// ---------------------------------------------------------------------------

procedure InitializeUninstallProgressForm();
begin
  { Deliberately empty: at this point the uninstall progress page exists but is
    not on screen yet, so it has no window handle and parenting a control to it
    is unreliable. The details box is created in
    CurUninstallStepChanged(usUninstall) instead, once the page is really up -
    and that is the ONLY window in which the memo can be written, so the box is
    created, filled and left alone inside that one step. }
end;

procedure CurUninstallStepChanged(CurUninstallStep: TUninstallStep);
var
  KeptPaths: String;
begin
  if CurUninstallStep = usUninstall then begin
    CreateUninstallDetailsMemo();
    AddUninstallLine('Removing application files…');
    { The user-data cleanup runs here - in the same step as the details box -
      instead of in usPostUninstall. Two reasons: the progress page (and with it
      the box) is destroyed as soon as the file-removal step is over, so a later
      write would only be possible in the log file; and keeping it here means
      every cleanup line is shown in the box while the page is still up. }
    UninstallCleanupOk := True;
    try
      BackupAndCleanUserData();
    except
      UninstallCleanupOk := False;
      AddUninstallLine('Cleanup failed - user data left untouched.');
      AddUninstallLine('Details: ' + UninstallLogPath());
    end;
  end
  else if CurUninstallStep = usPostUninstall then begin
    { Both sentences below are Inno's own, so they follow the selected language;
      the two paths need no translating. }
    KeptPaths := GetEnv('USERPROFILE') + '\.vivimusic\backups' + #13#10 +
                 GetEnv('USERPROFILE') + '\.vivimusic\device-sync.json';
    if UninstallCleanupOk then
      MsgBox(
        ExpandConstant('{cm:UninstalledAll,{#AppName}}') + #13#10 + #13#10 +
        ExpandConstant('{cm:UninstallKept}') + #13#10 + KeptPaths,
        mbInformation, MB_OK)
    else
      MsgBox(
        ExpandConstant('{cm:UninstalledAll,{#AppName}}') + #13#10 + #13#10 +
        'The cache cleanup could not be completed, so your data was left untouched at:' + #13#10 +
        GetEnv('USERPROFILE') + '\.vivimusic' + #13#10 + #13#10 +
        'Log: ' + UninstallLogPath(),
        mbInformation, MB_OK);
  end;
end;