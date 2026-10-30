## [Unreleased]

## [6.0.8.11_DE-1.55.0-beta] - 2026-10-23

### ✨ Added
- [DE] **The language picker has a search box.**
- [DE] **Storage opens on one summary head.**
- [DE] **Commits opens on the same head.**
- [DE] **The equalizer names the band being dragged over its point.**
- [DE] **Romanization has a screen of its own.**
- [DE] **The full player has an "Apple Music" design, and the two designs are named for what they are: "Basic" and "Apple Music".**
- [DE] **The cookies, the clover and the icons move like the Orion-Store site.**
- [DE] **Every icon button lifts and turns its glyph under the pointer.**
- [DE] **A bare icon button fails the build now.**
- [DE] **The equalizer can make a profile of its own, not only import one.**

### 🔧 Changed
- [DE] **The installers carry a start archive, and the JVM part of the start is about 160 ms shorter.**
- [DE] **Every bar in Settings is the Material 3 Expressive slider.**
- [DE] **The equalizer is a desktop screen.**
- [DE] **Updates is three blocks instead of a stack of loose rows.**
- [DE] **A settings dropdown's position is measured, not assumed.**
- [DE] **Player & mini-player design has a file of its own.**
- [DE] **About has a file of its own and the preview's three cards.**
- [DE] **App font is one card of faces.**
- [DE] **Canvas's source folds away with its switch.**
- [DE] **Desktop features is one card.**
- [DE] **Language is the list itself.**
- [DE] **Content's five switches are one card.**
- [DE] **Privacy is the two history switches and one action.**
- [DE] **Notifications is the mode, the card its three rows belong to, and one action.**
- [DE] **Theme & Colors takes every colour it draws from the theme.**
- [DE] **A row that belongs to a switch folds into place everywhere, and only once.**
- [DE] **A threshold is a row of the card it belongs to.**
- [DE] **Sign-in, Spotify import and the account share one shape.**
- [DE] **Names that said the wrong thing are fixed.**
- [DE] **The two volume channels on Device sync fold under an *Advanced* header.**
- [DE] **Animations is a screen of its own.**
- [DE] **The settings screens have files of their own.**
- [DE] **Every settings dropdown is measured, and its menu is read against the row it came from.**
- [DE] **Every settings card stands 16 dp from the next, the margin the page gives it.**
- [DE] **A block of one element wears the card too.**
- [DE] **The Google session is said by its title too, and the "Logged in as" badge is gone in both states.**
- [DE] **The settings-dropdown check measures the menu, not the machine.**
- [DE] **The page, the sidebar and the cards follow the accent.**
- [APK] **The phone follows the same two names:**
- [DE] **The first frame arrives about 1.3 s sooner: 8.6-9.0 s to 7.3-7.4 s.**
- [DE] **`Main.kt` is the app again: the player bar, the queue and the window left it.**
- [DE] **AI Lyrics Translation is the page's three cards.**
- [DE] **Player & audio is the preview's three cards.**
- [DE] **About is the page's cards, down to the line under the name.**
- [DE] **A settings screen says where you are in it: the content pane carries the app's own scrollbar.**
- [DE] **Lyrics is the page's screen, in the page's order.**
- [DE] **Privacy's one action is a tonal button.**
- [DE] **Canvas is the name and the switch.**
- [DE] **A block that belongs to a switch folds only while Animations are on.**
- [DE] **Debug & Developer Options is the page's two cards.**
- [DE] **A settings screen and the preview of it are the same picture again.**
- [DE] **A text the layout has to cut says the whole of itself on hover.**
- [DE] **Every corner in the DE is a level of the shape scale, and every colour is a theme role.**
- [DE] **No `OutlinedButton` is left in the DE.**
- [DE] **An artist, a song, a person and a room's face each have their own shape now.**
- [DE] **A person is the 9-sided cookie, not the 7-sided one.**
- [DE] **Every animation in the DE now comes from one Material 3 motion table, and a hover moves something.**
- [DE] **A cookie and the clover morph instead of staying still.**
- [DE] **Every cover in the desktop edition is the 4-sided cookie, so a picture has one shape wherever it is drawn.**
- [DE] **The Apple Music player's artwork casts the same shadow as the Basic player's, and that shadow is the cookie.**
- [DE] **An album's, a playlist's and an artist's artwork rounds its corners under the pointer, like a card's does.**
- [DE] **The Apple Music player's transport is one group in the middle of the column.**
- [DE] **`applePlayerRenderCheck` paints a track with a cover now, so the backdrop is measured.**
- [DE] **The Apple player's transport is the phone's Apple player, size for size.**
- [DE] **The preview's Apple player is the same row, and one column under 720 px.**
- [DE] **The Apple player's five controls are one group in the middle of the column, on the artwork's own axis.**
- [DE] **The rotating artwork is a squared cover, and the cookie is what it draws when the rotation is off.**
- [DE] **The Settings hub is rebuilt on the preview's own page.**
- [DE] **Notifications is the preview's screen, block for block, and the three rules it needed are now the settings' own.**
- [DE] **Updates is the preview's screen: the check's action is a button.**
- [DE] **Account is the preview's page: the title.**
- [DE] **Appearance is the preview's page: the heading in its own place.**

### 🐛 Fixed
- [DE] **Portuguese (Brazil), Chinese Simplified and Chinese Traditional get every string again.**
- [DE] **A settings dropdown opens under the value that was pressed.**
- [DE] **The desktop edition starts again.**
- [DE] **The class-data-sharing archive is dumped on Linux and macOS again.**
- [DE] **"Title bar only" and "Show in title bar" exist only while the window uses the native title bar.**
- [DE] **The Apple Music player's backdrop covers the window, and the screen follows the background chosen in the settings.**
- [DE] **The app stops re-running its whole UI every two seconds, and the dev stats stop launching PowerShell every two seconds.**
- [DE] **A lyrics provider that answers with a web page is reported as that instead of as a JSON dump.**

## [6.0.8.10_DE-1.54.34-beta] - 2026-10-17

### 🔧 Changed
- [DE] **The account sync is two-way: a local playlist that YouTube Music does not have yet is uploaded by the same run that pulls the account.**
- [DE] **The G1 young generation is capped at 128 MB instead of 384 MB.**
- [DE][APK] **Device sync now says whether each playlist still has to reach YouTube Music, instead of leaving the phone to guess.**

### 🐛 Fixed
- [APK] **A playlist created with the YouTube Music sync could fail and say nothing, and *Sync playlist* could spin forever.**
- [APK] **Songs still named after their own videoId are repaired at startup, not only while one account playlist is refreshed.**

## [6.0.8.9_DE-1.54.33-beta] - 2026-10-17

### 🔧 Changed
- [DE] **The live log can be read from either end.**

## [6.0.8.9_DE-1.54.32-beta] - 2026-10-16

### 🔧 Changed
- [DE] **Theme & Colors is one screen built around the wheel.**

### 🐛 Fixed
- [DE] **The account's liked list is no longer fetched and thrown away on every sync.**

## [6.0.8.9_DE-1.54.31-beta] - 2026-10-13

### 🔧 Changed
- [APK] **French and Azerbaijani are complete: every key the app declares now reads in those two languages.**

## [6.0.8.8_DE-1.54.31-beta] - 2026-10-13

### 🐛 Fixed
- [APK] **The release APK builds again: the string that failed aapt2 is escaped.**
- [APK] **A playlist's own songs are re-read from the account.**

## [6.0.8.7_DE-1.54.31-beta] - 2026-10-13

### 🐛 Fixed
- [DE] **The account's playlists are re-read while the app is open.**
- [DE] **A playlist the phone edited without a timestamp is no longer half-merged.**
- [DE] **Issue #3 while NOT paired: the ~200 ms the audio writer was losing every 30 seconds is gone.**
- [APK] **The Device sync screen is a Material 3 Expressive screen, and both volume channels are on it.**

## [6.0.8.6_DE-1.54.30-beta] - 2026-10-13

### 🐛 Fixed
- [APK] **The "code" the phone showed instead of a song's name is gone, and the rows that already carry it are repaired.**

## [6.0.8.6_DE-1.54.29-beta] - 2026-10-13

### 🐛 Fixed
- [DE] **The YouTube Music sync reads the playlists it already has, so a song added there reaches the desktop.**
- [APK] **A playlist edited on the phone now reaches the paired desktop, timestamp and all.**
- [DE] **The desktop no longer restarts the audio every five seconds while it is paired (issue #3, "not one millisecond is played fluidly").**
- [DE] **A drift correction that changes nothing is no longer repeated into a loop.**
- [DE] [APK] **A playlist can no longer lose its name on the way to the other device.**

## [6.0.8.6_DE-1.54.28-beta] - 2026-10-12

### 🐛 Fixed
- [DE] **A queue or a playlist whose rows are re-keyed mid-render no longer takes the app down.**

## [6.0.8.6_DE-1.54.27-beta] - 2026-10-11

### ✨ Added
- [DE] **A guest can put a local playlist's song in front of the host without opening the `⋮` menu.**

### 🔧 Changed
- [DE] **The custom colour wheel is on screen the moment Theme & Colors opens.**

## [6.0.8.6_DE-1.54.26-beta] - 2026-10-10

### 🐛 Fixed
- [DE] **A track queued twice no longer takes the app down.**

### 🔧 Changed
- [DE] **Listen Together: the relay moved to the top of the screen, and the connection buttons that duplicated it are gone.**
- [DE] **Listen Together: the room code card wears the Copy code button's own shape.**
- [DE] **Listen Together: the Suggestions section says how a suggestion is made.**
- [DE] **Listen Together: the avatars are drawn with the high-quality filter.**

## [6.0.8.6_DE-1.54.25-beta] - 2026-10-10

### 🔧 Changed
- [DE] **The custom colour is one round wheel, on the right of the section.**

## [6.0.8.5_DE-1.54.24-beta] - 2026-10-10

### 🐛 Fixed
- [DE] **A guest's transport is drawn disabled instead of only explained, and the whole app agrees on it.**
- [DE] **The Google sign-in window opens as many times as it is pressed, in one session.**
- [DE] **The localization generator can no longer delete the keys it does not know.**
- [DE] **Two more keys the generator would not have known about, and the sentence a restore failure needs.**
- [DE] **A backup that would not restore now restores, and one that cannot be read says which kind of unreadable it is.**
- [DE] **The Spotify import's manual-cookie card has one door again.**

### ✨ Added
- [DE] **The Listen Together screen, rebuilt as the phone's screen with the phone's own pictures.**

### 🔧 Changed
- [DE] **Storage lists the three caches apart, because they are three different costs.**
- [DE] **The custom colour is the palette's own last circle, and it follows a preset pick.**

### 🐛 Fixed ([APK])
- [APK] **The phone spends less time drawing lyrics, which is where it was warm for nothing.**

## [6.0.8.5_DE-1.54.23-beta] - 2026-10-10

### 🐛 Fixed
- [DE] **Creating a room no longer answers "the room does not exist", and the first room now works.**
- [DE] **An approved suggestion lands in the queue, and only once.**
- [DE] **The Google sign-in window can be opened again after a failed attempt.**
- [DE] **A guest's play/pause says why it does nothing.**
- [DE] **The Spotify import's manual-cookie card had two "Show" controls.**

### ✨ Added
- [DE] **The room screen carries the phone's options, and the avatars travel with them.**
- [DE] **The login screen says the second attempt is normal, and ERRORS.md says it too.**

### 🔧 Changed
- [DE] **800x600 keeps the whole interface.**

## [6.0.8.5_DE-1.54.22-beta] - 2026-10-09

### 🐛 Fixed
- [DE] **The sign-in window now waits for `LOGIN_INFO` instead of reading the cookie store once.**
- [DE] **A reply is a reply on both platforms, because the quote now travels in the mobile's own envelope.**
- [DE] **An approved guest leaves the join-request list at once, and the auto-approval switch finally does something.**
- [DE] **The host's crown follows the host, and a transfer updates every client.**
- [DE] **Copying the room code works, and the invite link can be copied too.**
- [DE] **Native notifications no longer arrive seconds late.**
- [DE] **A guest can no longer drive playback in a room, and every player says so.**

### ✨ Added
- [DE] **"Suggest to the host" is in a song's menu, and only where it belongs.**
- [DE] **The Spotify sign-in screen is built like the Google one.**

### 🔧 Changed
- [DE] **A small window compresses the interface instead of clipping it.**

## [6.0.8.5_DE-1.54.21-beta] - 2026-10-08

### 🐛 Fixed
- [DE] **The tracks that stopped with `giving up after 3 attempts.**
- [DE] **`VISIONOS` leads the client chain because it was measured to answer.**

### ✨ Added
- [DE] **The Listen Together chat window has a close button and a keyboard shortcut.**

## [6.0.8.5_DE-1.54.20-beta] - 2026-10-08

### 🐛 Fixed
- [DE] **The changelog emoji are drawn, in colour, on every path.**
- [DE] **The colour emoji font is bundled, COLRv0, and was measured before it was chosen.**
- [DE] **Listen Together no longer crashes with a duplicate `LazyColumn` key.**
- [DE] **Logs older than seven days are deleted automatically.**

### ✨ Added
- [DE] **Storage is a card screen now, ported from mobile.**
- [DE] **The first-run screen is rebuilt from scratch, on the mobile welcome flow.**
- [DE] **The Listen Together chat has a window of its own, with replies and notifications.**
- [DE] **Five strings were added to the localization tables.**

### 🔧 Changed
- [DE] **The window can be moved from anywhere the interface does not use.**
- [DE] **A carousel arrow exists only while its own direction can scroll.**
- [DE] **The update counter counts installs, not downloads.**
- [DE] **Content language and content region are cards.**
- [DE] **Material 3 Expressive, in the parts this app can reach.**

## [6.0.8.5_DE-1.54.19-beta] - 2026-10-07

### 🐛 Fixed
- [DE] **The uninstaller no longer dies with `Unknown custom message name "UninstalledAll"`.**
- [DE] **The Start Menu shows one name: VIVI Music DE.**
- [DE] **The sidebar no longer shows two "Liked Songs".**
- [DE] **And an imported playlist is never pushed to YouTube Music any more.**
- [DE] **"Episodes for Later" is empty because YouTube says it is empty.**
- [DE] **The account's own playlist names follow the app language, not Windows'.**
- [DE] **The embedded sign-in sends the whole session, and a retry after a failure starts from an empty cookie store (issue #101).**

### ✨ Added
- [DE] **The licence is readable inside the app, and it is loaded live.**

### 🔧 Changed
- [DE] **The error dialogs now say where the error is written down.**
- [DE] **The YouTube sign-in window's hints are one compact line now.**

## [6.0.8.5_DE-1.54.18-beta] - 2026-10-06

### 🐛 Fixed
- [DE] **The Spotify sign-in window captures the cookies now, and that is what made the import look broken.**
- [DE] **The sign-in window no longer claims Google blocked it while the user is typing on Spotify's own form.**
- [DE] **A language in place of another: two Developer options rows were labelled with a raw setting name.**
- [DE] **The JVM heap actually changes now, and the app says so.**

### ✨ Added
- [DE] **The translation audit also looks for a language standing in another language's place.**

### 🔧 Changed
- [DE] **The heap preparation bar moved to the screen that opened the sign-in window.**
- [DE] **The sign-in window's header is one row again:**
- [DE] **Turn off developer options is a switch now.**
- [DE] **The account row at the bottom left opens the Account screen directly.**
- [DE] **The expressive player's panel can no longer be closed by accident.**

## [6.0.8.5_DE-1.54.17-beta] - 2026-10-05

### 🐛 Fixed
- [DE] **The main window remembers its size and position again.**
- [DE] **Quitting from the tray keeps the window's placement too.**

## [6.0.8.5_DE-1.54.16-beta] - 2026-10-05

### 🔧 Changed
- [DE] **The Spotify sign-in window remembers its size and shows what it is doing.**

### 🐛 Fixed
- [DE] **The Retry and Open it in your browser buttons are no longer clipped.**
- [DE] **The JCEF sign-in can actually complete.**

## [6.0.8.5_DE-1.54.15-beta] - 2026-10-05

### ✨ Added
- [DE] **The Spotify sign-in window is a real Chromium now (JCEF).**

### 🔧 Changed
- [DE] **The Debug & Developer Options screen turned off the Android way.**

### 🐛 Fixed
- [DE] **The changelog is no longer letter-spaced.**
- [DE] **The changelog's section-heading emoji are drawn, not blank.**

## [6.0.8.5_DE-1.54.14-beta] - 2026-10-04

### ✨ Added
- [DE] **The Developer options screen is now "Debug & Developer Options", and it can turn itself off again.**

## [6.0.8.5_DE-1.54.13-beta] - 2026-10-02

### ✨ Added
- [DE] **The emoji are drawn from a custom colour font.**

### 🐛 Fixed
- [DE] **Changelog bullets are bullets again.**
- [DE] **The pointing hand appears only on a link.**
- [DE] **The System screen no longer shows the CPU/GPU description twice.**

## [6.0.8.5_DE-1.54.12-beta] - 2026-10-02

**Completes the 1.54.11 release.** `1.54.11` was published from an earlier commit before the work below landed, so this release carries it. The two real fixes are the missing About entry and the sign-in route for a Spotify account created with Google.

### ✨ Added
- [DE] **`DetailedStartupLogs`, off by default, and a switch in Developer options.**
- [DE] **`super_logs_writer` is now the JSON key `detailedPlaybackLogging`.**
- [DE] **Every new string has a real translation in all 52 languages.**

### 🔧 Changed
- [DE] **The live CPU / RAM / GPU / network monitor is now the "Performance impact reader".**

### 🐛 Fixed
- [DE] **The About entry is back in the settings menu.**
- [DE] **The right-click menu is only as wide as its widest item.**
- [DE] **A Spotify account created with Google can sign in now (issue #97).**

## [6.0.8.5_DE-1.54.11-beta] - 2026-10-02

### ✨ Added
- [DE] **A real Developer options screen, unlocked by tapping the version code seven times.**
- [DE] **A custom model field in the AI translation screen.**
- [DE] **The AI translation screen is rebuilt as grouped Material 3 cards.**

### 🔧 Changed
- [DE] **The old "Developer options" screen is now "Performance options".**

### 🐛 Fixed
- [DE] **The changelog heading emoji are drawn, not boxed.**
- [DE] **The right-click menu is Material 3 Expressive.**

## [6.0.8.5_DE-1.54.10-beta] - 2026-10-01

### ✨ Added
- [DE] **The changelog's emoji and rare symbols render through the operating system's own fonts.**

### 🐛 Fixed
- [DE] **The carousel shows one arrow at each end, on the correct side.**
- [DE] **Hovering a changelog link shows the pointing hand again.**
- [DE] **The sign-in window's texts sit on their own background again.**
- [DE] **The anti-embedded-browser shim is stronger and runs before the page's own scripts.**

### 🔧 Changed
- [DE] **The "set a password on spotify.com" button and its guidance are gone.**
- **The release body no longer repeats the version heading, and its commit list has an emoji.**

## [6.0.8.5_DE-1.54.9-beta] - 2026-10-01

### ✨ Added
- [DE] **The changelog is rendered as real Markdown now, emoji included.**
- [DE] **An available update shows its own release notes in the section below the update controls.**
- [DE] **`ERRORS.md` gives the Spotify sign-in and import failures stable codes (`SPOT-001` … `SPOT-013`).**

### 🔧 Changed
- **`Auto Release` no longer starts the Telegram bot.**
- [DE] **The sign-in windows' texts are readable and selectable.**

### 🐛 Fixed
- [DE] **The anti-embedded-browser check is now attempted, not only reported.**

## [6.0.8.5_DE-1.54.8-beta] - 2026-10-01

### ✨ Added
- [DE] **The way into a Google-created Spotify account is now on the import screen, not only inside the sign-in window's message (#97).**
- [DE] **Every horizontal row has scroll arrows, so a plain mouse can reach what is off the edge (#99).**

### 🐛 Fixed
- [DE] **The embedded sign-in window can be opened again in the same session (#98).**
- [DE] **Google's "This browser or app may not be secure" page is no longer a dead end in the Spotify sign-in window (#97).**
- [DE] **The AppImage wears the app's own icon instead of the JVM's.**
- [DE] **The Windows job's Inno Setup 7 step never even parsed, and that is why 1.54.6 and 1.54.7 were never published.**

### 🔧 Changed
- [DE] **The Spotify cookie paste is no longer hidden behind a broken window (#97).**
- [DE] **The chip rows got the same arrows as every other row (#99).**

## [6.0.8.5_DE-1.54.6-beta] - 2026-09-30

> **Never published.** Its `Auto Release` run failed in the Windows job before any release was created (see 1.54.8 below), so 1.54.5 is the last release that actually exists and everything under this heading shipped for the first time in **6.0.8.5_DE-1.54.8-beta**.

### ✨ Added
- **The release hands the bot one call, and that same file asks it every hour.**

### 🐛 Fixed
- [DE] **The language picker was drawn below the visible page, so the first screen never showed it.**

### 🔧 Changed
- **CI compiles the setup with Inno Setup 7.1.0, and a build that cannot see 7 fails instead of shipping.**
- **`Release Manifest` actually delivers the refreshed website data now.**

## [6.0.8.5_DE-1.54.5-beta] - 2026-09-29

### ✨ Added
- [DE] **The Windows setup asks for the language on its first screen, and it asks in all 51 of them.**
- [DE] **Import from Spotify signs in the way the phone does.**
- [DE] **Dark mode for the setup, with the brand on it.**

### 🐛 Fixed
- [DE] **The small wizard image was upside down.**

### 🔧 Changed
- [DE] **The Spotify import wears Spotify's mark.**

## [6.0.8.5_DE-1.54.4-beta] - 2026-09-29

### 🔧 Changed
- [DE] **"Auto skip to next song when error occurs" says what the switch does instead of what it is good for.**

## [6.0.8.5_DE-1.54.3-beta] - 2026-09-29

### ✨ Added
- [DE] **Import from Spotify.**
- [DE] **The Windows setup speaks every language the program does, and shows VIVI Music DE while doing it.**

## [6.0.8.5_DE-1.54.2-beta] - 2026-09-28

### 🐛 Fixed
- [DE] **"Sync OS volume" no longer switches itself back on.**

### 🔧 Changed
- [DE] **The row is called "Sync OS volume" and says so in all 52 languages.**
- [DE] **Disconnect and Unpair are one action, not two buttons.**

## [6.0.8.5_DE-1.54.1-beta] - 2026-09-28

### 🔧 Changed
- [DE] **The bulk playlist action now says what it will really do —.**

## [6.0.8.5_DE-1.54.0-beta] - 2026-09-28

### 🐛 Fixed
- [DE] **The seek bars move again, and the track time is real.**

### ✨ Added
- [DE] **"Sync volume": the native (OS) volume can now be kept out of the device sync.**

### 🔧 Changed
- [DE] **The desktop stops building 53 string tables before its first frame.**
- [DE] **"Disconnect" drops the pairing too.**

## [6.0.8.4_DE-1.53.34-alpha] - 2026-09-27

### 🐛 Fixed
- [DE] **A track skipped on the phone starts on the desktop instead of waiting for a play press.**
- [DE] **The right-click Copy / Cut / Paste / Select all labels come from the app's own table, in every language.**

## [6.0.8.3_DE-1.53.33-alpha] - 2026-09-27

### 🐛 Fixed
- [DE][APK] **A paired user seek is applied before the "peer is resolving" hold, in both directions.**
- [DE] **The device-sync log says what the receiver held, and why a command did nothing.**
- [DE] **An in-app settings change invalidates the reads keyed on the settings revision, so the expressive player's lyrics options apply live.**
- [DE] **The right-click (text-selection) menu follows the app language instead of the OS locale.**

## [6.0.8.2_DE-1.53.32-alpha] - 2026-09-27

### 🐛 Fixed
- [DE] **The changelog release body is laid out in its own bounded.**

## [6.0.8.1_DE-1.53.31-alpha] - 2026-09-26

### 🐛 Fixed
- [DE] **The changelog text is selectable (and copyable) like every error message in the app.**
- [DE] **The Windows toast no longer fails with `SQLITE_CONSTRAINT_UNIQUE` when two notifications are shown back to back.**

## [6.0.8.1_DE-1.53.30-alpha] - 2026-09-26

### 🐛 Fixed
- [DE][APK] **A paired seek is delivered in both directions, and a new track really starts on every device.**
- [APK] **The screen stays on while paired with the desktop.**
- [DE] **The track title is centred and scrolls, and the lyrics options apply live.**

### 🔧 Changed
- [DE] **The notification-permission row is shown on every desktop platform.**

### ✨ Added
- [DE][APK] **The device-sync log now records every change.**

## [6.0.8.1_DE-1.53.29-alpha] - 2026-09-26

### ✨ Added
- [DE] **`super_logs_writer`: the per-pass writer diagnostics are now an option, off by default.**
- [DE] **The Album, Artist and Playlist screens have a gradient header and the "⋮" context menu the song rows already had.**
- [DE] **The notification-permission row on the Notifications screen.**

### 🐛 Fixed
- [DE] **The canvas replaced only the player background, never the artwork.**

## [6.0.8.1_DE-1.53.28-alpha] - 2026-09-26

### 🐛 Fixed
- [DE] **The device no longer starts under-primed — the gap heard while a track is playing (#3).**

## [6.0.8.1_DE-1.53.27-alpha] - 2026-09-25

### 🔧 Changed
- [APK] **The mobile base is upstream 6.0.8, and the fork point was measured rather than guessed.**

### 🐛 Fixed
- [DE] **The writer verdict counted the sound card's own pacing as a stall (#3).**

## [6.0.6.8_DE-1.53.26-alpha] - 2026-09-25

### 🐛 Fixed
- [DE] **The device is primed from a measurement now, not from a 400 ms timer (#3).**

## [6.0.6.8_DE-1.53.25-alpha] - 2026-09-25

### ✨ Added
- [DE] **Every writer pass is timed now, so an export can say whether the stall repeats (#3).**

## [6.0.6.8_DE-1.53.24-alpha] - 2026-09-25

### 🔧 Changed
- [DE] **The translations are one file per language now, the way the APK keeps them.**

### 🐛 Fixed
- [DE] **The history drew a track length on some rows and nothing on the others.**
- [DE] **The artists tab can take seconds to appear and looked stuck while it did.**

## [6.0.6.8_DE-1.53.23-alpha] - 2026-09-25

### 🐛 Fixed
- [DE] **The sidebar logo and the tray icon came from a fully transparent render.**
- [DE] **The two expressive players are picked from the dropdown now, not from a switch.**
- [DE] **An upload updates the YouTube Music playlist that is already there instead of creating a second one.**
- [DE] **Lyrics are refetched when the cached copy predates the styled renderer.**

## [6.0.6.8_DE-1.53.22-alpha] - 2026-09-25

### 🐛 Fixed
- [DE] **The device now starts with a real cushion, not a third of a second (#3).**
- [DE] **The macOS "Now Playing" tile kept the first song's artwork (#63).**
- [DE] **Two diagnostics that sent this investigation the wrong way twice.**

## [6.0.6.8_DE-1.53.21-alpha] - 2026-09-25

### 🐛 Fixed
- [DE] **"Enable swipe to change song" could not be switched off and the player's swipe-sensitivity slider could not be moved.**
- [DE] **The "no text" dots appeared while the line was still being sung.**
- [DE] **The now-playing bars on the cover and in the queue did not move.**
- [DE] **No second expressive player.**
- [DE] **The VIVI mark was a 1024-px PNG everywhere.**
- [DE] **The translation audit found two ways a string could show the wrong language.**

## [6.0.6.8_DE-1.53.20-alpha] - 2026-09-24

### 🐛 Fixed
- [DE] **macOS: the system "Now Playing" tile appears at launch with a track restored from the persistent queue.**
- [DE] **The "no text" indicator in the lyrics is three horizontal dots, not a ring.**

## [6.0.6.8_DE-1.53.19-alpha] - 2026-09-24

### 🐛 Fixed
- [DE] **The expressive player no longer crashes when the lyrics options are opened.**
- [DE] **The “＋” next to the three dots is gone, and adding to the queue moved into the ⋮ menu.**
- [DE] **The tray icon and the logo in the sidebar are sharp.**
- [DE] **The lyrics show the “no text” indicator on plain LRC files too.**
- [DE] **The three “now playing” bars sit on the cover and they move.**
- [DE] **The Library screen refreshes when the library changes.**
- [DE] **The Artists screen lists the artists, not just one.**
- [DE] **The playlists that exist twice on YouTube Music are listed once in the sidebar.**
- [DE] **Skipping a track now syncs the position to the paired phone.**

## [6.0.6.8_DE-1.53.18-alpha] - 2026-09-24

### 🐛 Fixed
- [DE] **A like made on the phone reaches the desktop, and one made here reaches the phone.**
- [APK] **The liked songs are shared with the desktop.**
- [DE] **The copies a pairing made of the same playlist are collapsed again, and no second copy is created on YouTube Music (E1034).**
- [APK] **A playlist the desktop already has is no longer imported as a second one.**

## [6.0.6.7_DE-1.53.17-alpha] - 2026-09-23

### 🐛 Fixed
- [DE] **The seek bar can actually be dragged.**
- [DE] **A seek no longer snaps back to where it was.**
- [DE] **The expressive player's countdown no longer reads `-0:00` for a whole track.**
- [DE] **Pairing no longer multiplies the playlists, and no cleanup can delete one from YouTube Music (E1034).**
- [DE] **The expressive player reopens on the tab you left it on.**
- [DE] **The sidebar is opened and collapsed from the "VIVI Music" title.**
- [APK] **The account playlist id is sent with the playlists.**

## [6.0.6.6_DE-1.53.16-alpha] - 2026-09-23

### 🐛 Fixed
- [DE] **The lyrics show the "no text" indicator again.**
- [DE] **A line aligned left or right can no longer run off the window.**
- [DE] **Artwork is loaded at its real quality when Data saver is off.**
- [DE] **A forced playlist sync says where it is.**
- [DE] **The queue travels between paired devices as a whole.**
- [DE] **The player picker no longer offers two names for one player.**
- [DE] **A font can be picked straight out of `C:\Windows\Fonts`.**
- [APK] **The desktop pairing code is recognized at the first frame.**

## [6.0.6.5_DE-1.53.15-alpha] - 2026-09-22

### 🐛 Fixed
- [DE] **A full translation audit, and the class of bug behind "Alpha written in Arabic letters".**
- [DE] **The string table no longer breaks the build: `ClassTooLargeException`.**
- [DE] **Deleting a synced playlist now deletes the account's copy too.**

## [6.0.6.5_DE-1.53.14-alpha] - 2026-09-22

### ✨ Added
- [DE] **Creating a playlist offers to sync it with YouTube Music, like the mobile app.**
- [DE] **Account → *Create on YouTube Music*: the playlists that already exist locally can be created on the account in one confirmed action.**

## [6.0.6.5_DE-1.53.13-alpha] - 2026-09-22

### 🐛 Fixed
- [DE] **The History screen is the listening history, not the notifications list.**

## [6.0.6.5_DE-1.53.12-alpha] - 2026-09-22

### 🐛 Fixed
- [DE] **The Artists screen has content —.**

## [6.0.6.5_DE-1.53.11-alpha] - 2026-09-22

### 🐛 Fixed
- [DE] **The lyrics styles are ten genuinely different styles again, and they animate on every file.**
- [DE] **`MetroLyrics` finally runs on graphemes, not words.**
- [DE] **The lyrics move at the display's rate instead of in ~25 ms steps.**
- [DE] **Lyrics animation no longer depends on the "Animations" switch, and no longer desyncs from the audio.**
- [DE] **`{agent:v1|v2|v1000}` lines follow their own voice.**

## [6.0.6.5_DE-1.53.10-alpha] - 2026-09-22

### 🐛 Fixed
- [DE] **The sidebar groups stack again.**
- [DE] **The embedded sign-in no longer hands over a session without `LOGIN_INFO`.**
- [DE] **The player's transport buttons answer to a press.**

## [6.0.6.5_DE-1.53.9-alpha] - 2026-09-22

### 🐛 Fixed
- [DE] **The strings added with the player and sync work are translated in every language.**

## [6.0.6.5_DE-1.53.8-alpha] - 2026-09-22

### 🔧 Changed
- [DE] **The app stops redrawing while nothing is playing.**

## [6.0.6.5_DE-1.53.7-alpha] - 2026-09-22

### ✨ Added
- [DE] **`Auto sync with account`, the mobile app's playlist sync.**

## [6.0.6.5_DE-1.53.6-alpha] - 2026-09-22

### 🐛 Fixed
- [DE] **The player has the mobile lyrics controls.**
- [DE] **Four settings rows were showing their raw key.**

## [6.0.6.5_DE-1.53.5-alpha] - 2026-09-21

### 🐛 Fixed
- [DE] **Every expansion now animates, and all of them follow the settings.**

## [6.0.6.5_DE-1.53.4-alpha] - 2026-09-21

### 🐛 Fixed
- [DE] **The history now remembers.**
- [DE] **Six settings keys were showing as raw keys in the UI.**
- [DE] **The lyrics options are no longer English-only in every language.**

## [6.0.6.5_DE-1.53.3-alpha] - 2026-09-21

### ✨ Added
- [DE] **The Content screen is the mobile one.**

### 🐛 Fixed
- [DE] **The artist page gets its content rows and the description.**

## [6.0.6.5_DE-1.53.2-alpha] - 2026-09-21

### 🐛 Fixed
- [DE] **The Artists screen is no longer empty.**

## [6.0.6.5_DE-1.53.1-alpha] - 2026-09-21

### ✨ Added
- [DE] **A new lyrics style, `Alpha`.**

### 🐛 Fixed
- [DE] **Every animation style now follows the mobile recipe of the same name.**
- [DE] **The expressive player's lyrics ignored the lyrics options.**

## [6.0.6.5_DE-1.53.0-alpha] - 2026-09-21

### ✨ Added
- [DE] **Two more lyrics styles, so the picker matches the mobile one.**

### 🔧 Changed
- [DE] **Every lyrics style is now a style of its own.**
- [DE] **Settings are grouped and ordered like the mobile app's.**

### 🐛 Fixed
- [DE] **`Apple Music blur` is offered only where it does anything.**

## [6.0.6.5_DE-1.52.9-alpha] - 2026-09-21

### ✨ Added
- [DE] **GC pauses are recorded per session (`gc.log`).**

### 🐛 Fixed
- [DE] **The Windows audio threads now join the OS's "Audio" scheduling class, and the app can no longer be power-throttled (issue #3).**
- [DE] **The writer now says *why* it was late.**

## [6.0.6.5_DE-1.52.8-alpha] - 2026-09-21

### 🔧 Changed
- [DE] **The Arch/AUR files now ship as a single `VIVIMusic-<version>-AUR.tar.gz` asset.**

### 🐛 Fixed
- [DE] **A half-finished update download can no longer look finished (issue #82).**
- [DE] **Two playback diagnostics were measuring pauses (issue #3).**

## [6.0.6.5_DE-1.52.7-alpha] - 2026-09-21

### ✨ Added
- [DE] **Native Fedora/RPM package.**

## [6.0.6.5_DE-1.52.6-alpha] - 2026-09-20

### 🐛 Fixed
- [DE] **`Build Android APK` publishes the APKs again.**

## [6.0.6.5_DE-1.52.5-alpha] - 2026-09-20

### 🐛 Fixed
- [DE] **The lyrics animation speed, text position and line spacing are honored by every style.**
- [DE] **The lyrics style and text-position selectors are dropdowns.**
- [DE] **Resizing the player no longer stutters.**
- [DE] **Playing music no longer pushes work on the position tick.**
- [DE] **The "Artists" screen is no longer empty.**
- [DE] **Two playback diagnostics no longer cry wolf.**

## [6.0.6.5_DE-1.52.4-alpha] - 2026-09-20

### 🐛 Fixed
- [APK] **The Android build compiles again (no APK could be produced since 6.0.6.3).**

## [6.0.6.4_DE-1.52.4-alpha] - 2026-09-20

### 🔧 Changed
- [DE] **Every issue reference now uses the official repository's numbering.**

## [6.0.6.4_DE-1.52.3-alpha] - 2026-09-20

### 🐛 Fixed
- [DE] **The release-notes filter really filters.**

## [6.0.6.4_DE-1.52.2-alpha] - 2026-09-20

### 🔧 Changed
- [DE] **The website now lives on its own branch.**
- [DE] **Every change now goes to the official repository, on the branch that owns it.**

### 🐛 Fixed
- [DE] **The release notes no longer show the website bookkeeping commit.**
- [DE] **The dead build workflows are gone.**

### 📌 Notes
- [BOT] The Telegram bot (`PiBOH/vivimusicde_bot`) now follows the new layout.

## [6.0.6.4_DE-1.52.1-alpha] - 2026-09-20

### 🐛 Fixed
- [DE] **The update source and the remaining website links point at the official repository.**

## [6.0.6.4_DE-1.52.0-alpha] - 2026-09-20

### ✨ Added
- [DE] **`~/.vivimusic/settings.json` — every option in one editable file.**

### 🔧 Changed
- [DE] **The Android APKs are no longer release assets.**
- [APK] The companion app checks for updates against that channel.
- [DE] **Every repository link now points at `PiBOH/vivi-music-de`.**
- [WEBSITE] The APK buttons (download dialog and downloads page) point at the fixed `.releases/apk/latest` URLs.

### 🐛 Fixed
- [DE] General improvements and fixes.

## [6.0.6.3_DE-1.51.2-alpha] - 2026-09-19

### 🐛 Fixed
- [DE] **The 30-second music stutter and UI hitch of #3 were a hidden `System.gc()`, not the audio pipeline.**

## [6.0.6.3_DE-1.51.1-alpha] - 2026-09-19

### 🔧 Changed
- [DE] **Playback diagnostics now cover the two things the exported logs could not see (#3).**

### 📌 Notes
- [DE] Findings behind this change, from the exported 1.50.71/1.50.76 logs.

## [6.0.6.3_DE-1.51.0-alpha] - 2026-09-19

### ✨ Added
- [DE] **Animated lyrics, ported from the mobile app.**
- [DE] **Every display option around them.**
- [DE] **Romanization and lyric translation actually render.**

### 🔧 Changed
- [DE] **The lyrics parser keeps the per-word timings.**
- [DE] The lyrics position is read per line.
- [DE] Settings search (already able to reach sub-screens) now indexes the new lyrics keys as well.

## [6.0.6.3_DE-1.50.76-alpha] - 2026-09-18

### 🔧 Changed
- [DE] **The site's release manifest now refreshes itself every hour.**
- [DE] **A manual release can be published without the APKs.**

## [6.0.6.3_DE-1.50.75-alpha] - 2026-09-18

### 🐛 Fixed
- [DE] **Emergency fix — the app crashed on launch in 1.50.72, 1.50.73 and 1.50.74.**
- [DE] **If you are on 1.50.72, 1.50.73 or 1.50.74, install this build by hand.**

## [6.0.6.3_DE-1.50.74-alpha] - 2026-09-17

### 🔧 Changed
- [DE] **Exported logs stop growing forever.**

### 🐛 Fixed
- [DE] **No more dropout at the very beginning of a track.**

## [6.0.6.3_DE-1.50.73-alpha] - 2026-09-17

### 🔧 Changed
- [DE] **The activity log keeps the full picture.**

## [6.0.6.3_DE-1.50.72-alpha] - 2026-09-17

### 🔧 Changed
- [DE] **The installers are much smaller.**
- [DE] **Windows `.exe` setup compressed harder.**
- [DE] Published formats are unchanged: Windows ships both `setup.exe` and `.msi`, macOS both `.dmg` and `.pkg`, Linux `.deb` + `.AppImage` + `PKGBUILD`.

## [6.0.6.3_DE-1.50.71-alpha] - 2026-09-16

### 🐛 Fixed
- [DE] **The volume slider is heard immediately, not a second later.**
- [DE] **Lyrics are found for tracks whose title uses stylized Unicode.**
- [DE] **Playing music no longer recomposes the whole app ~20 times a second.**

## [6.0.6.3_DE-1.50.70-alpha] - 2026-09-16

### 🐛 Fixed
- [DE] **Wrong lyrics finally disappear.**

### 🗑️ Removed
- [DE] **The video-captions lyrics source added in 1.50.69 is gone again.**

## [6.0.6.3_DE-1.50.69-alpha] - 2026-09-16

### 🐛 Fixed
- [DE] **Volume changes are heard immediately.**
- [DE] **Dragging the volume no longer rewrites the settings file once per frame.**
- [DE] **The settings file is no longer read and parsed twice a second while playing.**
- [DE] **The silence-cut gate measures the right thing.**
- [DE] **The playback-stall probe now measures the audio thread instead of a lowest-priority helper.**
- [DE] **Lyrics for the tracks the community servers do not index.**

## [6.0.6.3_DE-1.50.68-alpha] - 2026-09-16

### 🐛 Fixed
- [DE] **Streaming playback keeps a real cushion instead of running on the download frontier.**
- [DE] **"Skip silence" can no longer starve the sound card.**
- [DE] **The stall diagnostics now say how far behind the download is.**
- [DE] **The look-ahead cache pass yields to the track that is playing.**

## [6.0.6.3_DE-1.50.67-alpha] - 2026-09-15

### 🐛 Fixed
- [DE] **The live log window can no longer crash the app.**
- [DE] **The lyrics shown are the ones of the song that is playing.**
- [DE] **Resuming from pause no longer starts with a short silence.**
- [DE] **The Home "Your artists feed" header opens a real page again.**

### ✨ Added
- [DE] **`window.log`: the window itself is now part of the exported diagnostics.**

## [6.0.6.3_DE-1.50.66-alpha] - 2026-09-15

### 🐛 Fixed
- [DE] **Nothing application-side can delay the audio thread any more.**
- [DE] **The device buffer — the cushion that actually absorbs a late writer — is now verified, maximized and logged.**
- [DE] **A real audio gap is finally measurable.**
- [DE] **PCM is handed to the sound card in ~120 ms blocks instead of one AAC frame (~23 ms).**
- [DE] **The JVM is tuned for low pause times.**
- [DE] **"Skip silence" now declares what it cuts.**
- [DE] **Turning "Media keys" off on macOS now takes effect immediately.**
- [DE] **macOS "Now Playing" keeps its claim while a track is loaded but paused.**
- [DE] macOS: the now-playing artwork is decoded once per track instead of on every metadata push (twice a second).

## [6.0.6.3_DE-1.50.65-alpha] - 2026-09-14

### 🐛 Fixed
- [DE] **Playback no longer pauses or skips: decoding and output are two separate threads now.**
- [DE] **The random audio gap is now measurable in the exported log.**
- [DE] **The macOS "Now Playing" tile appears on a fresh launch without touching the switch.**

## [6.0.6.3_DE-1.50.64-alpha] - 2026-09-13

### 🐛 Fixed
- [DE] **The header's audio output device button works.**

### 🔧 Changed
- [DE] **The mouse back/forward buttons are always on.**
- [DE] **The first button in the header is the sidebar toggle.**

## [6.0.6.3_DE-1.50.63-alpha] - 2026-09-13

### 🐛 Fixed
- [DE] **Audio micro-pauses/skips are greatly reduced and are now diagnosable.**
- [DE] **The macOS "Now Playing" tile and its media keys survive a restart.**
- [DE] **The Library empty state no longer shows the raw `refresh` key.**

### ✨ Added
- [DE] **Every new desktop string is translated into all 52 languages.**

## [6.0.6.3_DE-1.50.62-alpha] - 2026-09-12

### ✨ Added
- [DE] **The mouse side buttons navigate back and forward.**
- [DE] **Real audio output device picker.**
- [DE] **"Animation speed" option (Fast / Normal / Slow).**
- [DE] **The like button actually likes.**

### 🐛 Fixed
- [DE] **Restoring a backup restores the playlists again.**
- [DE] **Empty "Liked songs" / albums / artists / playlists library tabs can be reloaded.**

### 🔧 Changed
- [DE] **Seekbar lag reduced (~50 ms).**
- [DE] **Playback & network stack.**
- [DE] **Faster startup.**
- [DE] **Regression scan for the #61 class of bug.**

## [6.0.6.3_DE-1.50.61-alpha] - 2026-09-12

### 🐛 Fixed
- [DE] **Theme picker: the color bars follow the pointer and a HEX field was added.**
- [DE] **macOS "Now Playing" and the media keys no longer need the Accessibility permission.**

### ✨ Added
- [DE] **The macOS window chrome follows the app's theme.**

## [6.0.6.3_DE-1.50.60-alpha] - 2026-09-11

### 🐛 Fixed
- [DE] **The Windows uninstaller really is fixed this time - no `Control has no parent window` error and the user-data cleanup actually runs.**

## [6.0.6.3_DE-1.50.59-alpha] - 2026-09-11

### 🐛 Fixed
- [DE] **The floating "Now Playing" widget can no longer be dragged off screen.**
- [DE] **The Windows uninstaller no longer aborts with 'Control has no parent window'.**

### 🔧 Changed
- [WEBSITE] **Motion is now forced on every page and on every device.**

## [6.0.6.3_DE-1.50.58-alpha] - 2026-09-11

### 🐛 Fixed
- [DE] **The Windows uninstaller's closing message matches the new retention rule.**

## [6.0.6.3_DE-1.50.57-alpha] - 2026-09-11

### 🔧 Changed
- [DE] **Uninstall now keeps only the newest `.vivide.backup` plus `device-sync.json`.**

## [6.0.6.3_DE-1.50.56-alpha] - 2026-09-11

### 🐛 Fixed
- [DE] **The floating "Now Playing" widget no longer crashes the app on launch.**
- [DE] **Windows uninstall no longer aborts with 'Type Mismatch'.**

## [6.0.6.3_DE-1.50.55-alpha] - 2026-09-10

### 🐛 Fixed
- [DE] **Persian (fa) and Hebrew (iw) tables completed (batch 68).**

## [6.0.6.3_DE-1.50.54-alpha] - 2026-09-10

### 🐛 Fixed
- [DE] **Player background "Visualizer" no longer shows in Arabic.**
- [DE] **Completed the missing desktop translations (batch 67).**

## [6.0.6.3_DE-1.50.53-alpha] - 2026-09-10

### 🐛 Fixed
- [DE] **Seek bar stuck at ~19 s for some tracks (persisted).**

### 🔧 Changed
- [DE] De-duplicated `formatBytes`/`formatSpeed` (three `formatBytes` + two `formatSpeed` copies with slightly different behaviour in `Main.kt`.

## [6.0.6.3_DE-1.50.52-alpha] - 2026-09-10

### 🐛 Fixed
- [DE] **[CRITICAL] Tracks no longer appear to last ~19 seconds.**
- [DE] Listen Together: leaving a room now forces a clean socket reconnect.

### 🔧 Changed
- [DE] Listen Together lobby now has a single morphing action button like the mobile app.

## [6.0.6.3_DE-1.50.51-alpha] - 2026-09-09

### 🐛 Fixed
- [DE] **Browse pages no longer fail with 401 `BROWSE_UNAUTHENTICATED` (E1031).**

## [6.0.6.3_DE-1.50.50-alpha] - 2026-09-09

### 🐛 Fixed
- [DE] **Browse failures are no longer invisible in the logs and the app.**
- [DE] **Every log file now exists from the first launch.**

## [6.0.6.3_DE-1.50.49-alpha] - 2026-09-09

### 🐛 Fixed
- [DE] **Browsing no longer crashes with duplicate keys.**
- [DOCS] **New error code E1031 — `BROWSE_UNAUTHENTICATED`.**

## [6.0.6.3_DE-1.50.48-alpha] - 2026-09-08

### 🐛 Fixed
- [DE] **Full players and mini-player backgrounds now follow the Light/Dark/System theme.**

## [6.0.6.3_DE-1.50.47-alpha] - 2026-09-08

### 🐛 Fixed
- [DE] **The classic mini-player seek bar now works without opening the full player first.**

## [6.0.6.3_DE-1.50.46-alpha] - 2026-09-08

### 🔧 Changed
- [DE] **Remaining tooltips now use action verbs.**

## [6.0.6.3_DE-1.50.45-alpha] - 2026-09-08

### 🔧 Changed
- [DE] **The "Open live log" entry moved from Developer options to Settings → System and is now always available.**
- [DE] **Tooltips now use action verbs and reflect the current state.**

### 🐛 Fixed
- [DE] **The crash log is confirmed included in the exported log archive.**

## [6.0.6.3_DE-1.50.44-alpha] - 2026-09-08

### 🐛 Fixed
- [DE] **Clicking any button on Home no longer crashes with an NPE.**

### ✨ Added
- [DE] **Crash dumps are now written to disk on every uncaught error.**
- [DE] **Every click is now recorded in the session `actions.log`.**

## [6.0.6.3_DE-1.50.43-alpha] - 2026-09-08

### 🐛 Fixed
- [DE] **Crossfade sub-options are now hidden when crossfade is off.**
- [DE] **Browsing "Pinned for later" and similar playlists no longer crashes.**

## [6.0.6.3_DE-1.50.42-alpha] - 2026-09-08

### 🐛 Fixed
- [DE] **Windows uninstall no longer crashes with 'Type Mismatch'.**
- [DE] **Metadata now show 'PiBOH' as publisher and 'VIVI Music' as product name.**
- [DE] **In-app changelog now shows all released versions.**

## [6.0.6.3_DE-1.50.41-alpha] - 2026-09-08

### 🐛 Fixed
- [DE] **Stale cached lyrics can no longer hide the resolver fixes.**
- [DE] **Results fetched without a known duration are no longer cached.**
- [DE] **The album is now passed to every lyrics provider.**

## [6.0.6.3_DE-1.50.40-alpha] - 2026-09-08

### 🐛 Fixed
- [DE] **The two "keep the queue going" settings are merged into one.**
- [DE] **"Instantly skip silence" only appears when "Skip silence" is on.**
- [DE] **The slider style setting now applies to every player.**

## [6.0.6.3_DE-1.50.39-alpha] - 2026-09-07

### 🌍 Translations
- [DE] **Full translation sweep across all 47 supported languages.**

## [6.0.6.3_DE-1.50.38-alpha] - 2026-09-07

### 🐛 Fixed
- [DE] **The similar/up-next queue is no longer wiped when a track's first attempt fails.**
- [DE] **The "cannot find the file specified" playback error right after "stream ready" is fixed.**
- [DE] **Logs are now organized per session.**

## [6.0.6.3_DE-1.50.37-alpha] - 2026-09-07

### ✨ Added
- [DE] **Crossfade.**

## [6.0.6.3_DE-1.50.36-alpha] - 2026-09-07

### 🔧 Changed
- [DE] **Lyrics prefer the synced (timed) version when the "Synced lyrics" option is on.**

## [6.0.6.3_DE-1.50.35-alpha] - 2026-09-07

### 🐛 Fixed
- [DE] **[Critical] Tapping a song on Home / Library now creates a real queue again.**
- [DE] **[Critical] Lyrics are no longer missing, wrong-version or permanently wrong.**

## [6.0.6.3_DE-1.50.34-alpha] - 2026-09-07

### ✨ Added
- [DE] **"Skip silence" and "Instantly skip silence" options in Player & audio.**

## [6.0.6.3_DE-1.50.33-alpha] - 2026-09-07

### ✨ Added
- [DE] **More "Player & audio" options ported from the mobile app.**
- **Progressive seek.**

## [6.0.6.3_DE-1.50.32-alpha] - 2026-09-07

### ✨ Added
- [DE] **New "Player & audio" options.**
- **Pause music when media is muted.**

## [6.0.6.3_DE-1.50.31-alpha] - 2026-09-07

### ✨ Added
- [DE] **"Auto load more songs" and "Enable similar content" options in Player & audio.**

## [6.0.6.3_DE-1.50.30-alpha] - 2026-09-07

### 🐛 Fixed
- [APK] **Prerelease updates on the fork now come from GitHub Releases, not nightly runs.**
- [APK] **The About screen shows the real release channel.**

## [6.0.6.2_DE-1.50.29-alpha] - 2026-09-06

### 🔧 Changed
- [APK] **The Android release workflow lets you choose the signing key.**

## [6.0.6.2_DE-1.50.28-alpha] - 2026-09-06

### ✨ Added
- [DE] **Do Not Disturb is detected before sending native notifications.**
- [APK] **A dedicated release keystore now exists.**

### 🔧 Changed
- [APK] **Android versioning is back on the `6.0.6.x` line.**

### 🌍 Translations
- [DE] Added the `notif_dnd_title` / `notif_dnd_body` keys in all 52 supported languages (thanks to @codebuffai).

## [6.4.46.1_DE-1.50.27-alpha] - 2026-09-06

### 🐛 Fixed
- [DE] **Website footer cleaned up.**
- [DE] **Website animations now run on desktop too.**
- [DE] **Website changelog is lazy now.**

### 🔧 Changed
- [APK] **The Android companion app is now "VIVI for DE".**
- [APK] **`build-android.yml` rewritten.**

## [6.4.46_DE-1.50.26-alpha] - 2026-09-06

### ✨ Added
- [DE] **The settings search now covers every sub-screen.**

### 🌍 Translations

## [6.4.46_DE-1.50.25-alpha] - 2026-09-06

### ✨ Added
- [DE] **The activity log now records every settings change.**

### 🐛 Fixed
- [DE] **The Home "Recommended" row no longer disappears on fresh profiles.**
- [DE] **The Home empty state shows real text instead of a raw `home_empty` key.**

## [6.4.46_DE-1.50.24-alpha] - 2026-09-05

### ✨ Added
- [DE] **Detailed activity logging.**
- [DE] **Live log viewer window.**

### 🐛 Fixed
- [DE] **Stream cache minimum is now 10 minutes.**

### 🔧 Changed
- [APK] **One-off version bump to 6.4.46 (versionCode 130).**
- [APK] **Update check is now chronology-aware.**

## [6.0.6.1_DE-1.50.23-alpha] - 2026-09-05

### 🔧 Changed
- [DE][APK] **Playback resolution ported from upstream vivizzz007/main (6.0.6).**
- [APK] **Rebased on upstream 6.0.6.**
- [DE] Upstream fallback order also restored `Android VR 1.65.10` and the Chrome UA set that upstream 6.0.6 ships.

### ✨ Added
- [DE] **Home "Recommended" section.**

### 🐛 Fixed
- [APK] Device-sync now persists/restores the `EnableUnisonKey` lyrics preference (renamed upstream from SimpMusic), so lyrics-provider sync keeps working after the rebase.
- [APK] **Updater defaults now match the fork distribution.**
- [APK] **Fixed a broken CI workflow.**
- [DE] The release tag now correctly reads `6.0.6.1_DE-1.50.23-alpha`.

## [6.4.45_DE-1.50.22-alpha] - 2026-09-04

### 🔧 Changed
- [DE] Desktop releases now publish on the **alpha** channel instead of nightly (release channel in `version.txt`).

### 🐛 Fixed
- [DE] **Players, mini players and the sidebar now follow the app theme in light mode.**
- [DE] **Custom colors now appear in the palette live.**
- [APK] **Debug APK About screen now shows NIGHTLY for non-stable builds.**

## [6.4.45_DE-1.50.21-nightly] - 2026-09-04

### 🐛 Fixed
- [DE] **Tracks stuck in a resolve loop after clearing the cache.**

## [6.4.45_DE-1.50.20-nightly] - 2026-09-04

### 🐛 Fixed
- [DE] **Scrubbing the seek bar now starts the stream of an unresolved track.**
- [DE] **The YouTube-style buffered bar is now always visible.**

## [6.4.45_DE-1.50.19-nightly] - 2026-09-04

### 🐛 Fixed
- [DE] **Windows installer no longer fails to compile.**

### ✨ Added
- [DE] **Uninstall now keeps exactly one final backup and wipes every cache.**

## [6.4.45_DE-1.50.18-nightly] - 2026-09-04

### 🔧 Changed
- [DE] The Inno Setup installer now always shows the installation details box (the extraction log) below the progress bar on the Installing page.

## [6.4.45_DE-1.50.17-nightly] - 2026-09-04

### 🐛 Fixed
- [DE] **Radio/Charts discovery screen no longer stays empty.**
- [DE] **Native macOS notifications actually delivered.**
- [DE] **Home screen can no longer be silently blank.**
- [DE] **Changelog issue references are now clickable.**

## [6.4.45_DE-1.50.16-nightly] - 2026-09-04

### ✨ Added
- [DE] **Native system playback controls and "Now Playing" on macOS.**

## [6.4.45_DE-1.50.15-nightly] - 2026-09-04

### 🐛 Fixed
- [DE] **Audio micro-pauses/skips on macOS (random brief glitches on almost every track, coinciding with small UI hitches) reduced.**

## [6.4.45_DE-1.50.14-nightly] - 2026-09-04

### ✨ Added
- [DE] Sidebar "Playlists" entry now opens the full playlist list screen (all local playlists with create/rename/delete).

## [6.4.45_DE-1.50.13-nightly] - 2026-09-04

### 🐛 Fixed
- [DE] **Mini player seek bar now works on tracks restored from the persistent queue without opening the full player.**

## [6.4.45_DE-1.50.12-nightly] - 2026-09-03

### ✨ Added
- [DE] **Dedicated Contributors sub-screen.**
- [DE] **Seek bars usable before the duration is known.**

### 🔧 Changed
- [DE] `contributorsde.json` is now always read from the repository (`vivi-music-de` branch) with a silent refresh every time the Contributors screen opens —.

## [6.4.45_DE-1.50.11-nightly] - 2026-09-03

### ✨ Added
- [DE] **YouTube-style buffered progress on the seek bars of every player.**
- [DE] **Seek bars can now be scrubbed before playback starts.**

### 🔧 Changed
- [DE] The About -> Contributors list is now fetched live from GitHub (`contributorsde.json` on the `vivi-music-de` branch) every time the screen opens.

## [6.4.45_DE-1.50.10-nightly] - 2026-09-03

### 🔧 Changed
- [DE] The contributor list is now read from `~/.vivimusic/contributorsde.json` (the bundled default is copied there on the first launch).

## [6.4.45_DE-1.50.9-nightly] - 2026-09-03

### ✨ Added
- [DE] About screen: new CONTRIBUTORS section below the developer card.

## [6.4.45_DE-1.50.8-nightly] - 2026-09-03

### 🐛 Fixed
- [DE] **Classic mini player seeking.**
- [DE] **Instant start on the restored queue.**

## [6.4.45_DE-1.50.7-nightly] - 2026-09-03

### 🐛 Fixed
- [DE] **Radio/Charts screen can never be silently blank.**
- [DE] **Single density selector.**
- [DE] **Every error is selectable.**
- [DE] **Sign-in now strips the `||…` suffix from `DATASYNC_ID`.**
- [DE] Player-design dropdowns now highlight the selected option (the check compares the translated labels.

## [6.4.44_DE-1.50.6-nightly] - 2026-09-03

### 🐛 Fixed
- [DE] **Settings lists are ~15% more compact.**

## [6.4.44_DE-1.50.5-nightly] - 2026-09-03

### 🐛 Fixed
- [DE] **Seeking now lands precisely on the clicked position instead of re-scanning the whole seek bar.**
- [DE] **A fully cached track is played straight from disk without being "resolved" again.**
- [DE] **Look-ahead prefetch now covers the whole queue.**
- [DE] **The in-app (VIVI) volume no longer resets to 100% on every launch.**

## [6.4.44_DE-1.50.4-nightly] - 2026-09-03

### 🐛 Fixed
- [DE] **"Mood & genres" no longer crashes when it scrolls into view on the Home page.**
- [DE] **macOS: the Accessibility permission prompt no longer reappears on every launch.**
- [DE] **Expressive player: clearing the queue no longer strands the user on the full-player screen.**
- [DE] **Device sync (relay): the pairing code is now generated automatically when connecting.**
- [DE] **Audio micro-pauses/skips on macOS.**

## [6.4.44_DE-1.50.3-nightly] - 2026-09-03

### 🐛 Fixed
- [APK] **Startup freeze after in-place updates.**

## [6.4.43_DE-1.50.3-nightly] - 2026-09-03

### 🐛 Fixed
- [DE] [APK] The "Sync VIVI volume" toggle now gates only the in-app VIVI volume channel —.
- [DE] The Devices sync screen gained a connection-method selector at the top (Server relay.

## [6.4.42_DE-1.50.2-nightly] - 2026-09-03

### 🐛 Fixed
- [APK] The relay server field in Settings → Devices can be cleared and changed again.
- [DE] The Device sync section no longer claims to be "Connected" when it is only connected to the relay.

## [6.4.41_DE-1.50.1-nightly] - 2026-09-02

### 🐛 Fixed
- [DE] **Listen Together: creating a room no longer hangs on the spinner.**

## [6.4.41_DE-1.50.0-nightly] - 2026-09-02

### ✨ Added
- [DE] **Real Listen Together (full port of the mobile feature).**
- **Rooms.**
- [DE] **PlayerController: `insertNext` + `replaceQueuePreservingCurrent`.**
- [DE] 18 new Listen Together strings translated into all 47 languages (618 keys total; `check_localization` passes).

## [6.4.41_DE-1.49.9-nightly] - 2026-09-02

### 🐛 Fixed
- [DE] **Data saver toggle now updates live.**

## [6.4.41_DE-1.49.8-nightly] - 2026-09-02

### 🐛 Fixed
- [DE] **Tray menu toggle applies live.**
- [DE] **Home section songs now play as a queue like Android.**
- [DE] **Mac/Linux physical media keys work (with permission).**
- [DE] **Audio glitches.**
- [DE] **Log export moved to Settings → System.**
- [DE] **Remaining raw UI strings localized in all 47 languages.**

## [6.4.41_DE-1.49.7-nightly] - 2026-09-02

### 🐛 Fixed
- [DE] **Sign-in now requires `DATASYNC_ID` and `VISITOR_DATA`.**
- [DE] Login labels/hint updated from "optional" to "required" wording in all 47 languages.

## [6.4.41_DE-1.49.6-nightly] - 2026-09-02

### ✨ Added
- [DE] **Developer options → "Export logs (.zip)".**
- [DE] **Appearance → Native system title bar now shows a compatibility hint.**

### 🐛 Fixed
- [DE] **Native notifications on macOS now actually fire.**
- [DE] **Login validation failures caused by a YouTube Music server error (HTTP 5xx.

## [6.4.41_DE-1.49.5-nightly] - 2026-09-02

### 🐛 Fixed
- [DE] **"Player design" entry in Settings → Player & audio now opens the design screen.**

## [6.4.41_DE-1.49.4-nightly] - 2026-09-02

### 🔧 Changed
- [DE] **Font sub-screen restored to the pre-1.44.0 rich layout.**

## [6.4.41_DE-1.49.3-nightly] - 2026-09-02

### 🐛 Fixed
- [DE] **[CRITICAL FIX] All single-choice dropdown options were invisible since 1.44.0.**

## [6.4.41_DE-1.49.2-nightly] - 2026-09-02

### ✨ Added
- [DE] **EQ band range labels.**

## [6.4.41_DE-1.49.1-nightly] - 2026-09-02

### 🐛 Fixed
- [DE] **[CRITICAL FIX] Restored the Appearance sub-screens that were flattened into inline dropdowns in 1.44.0.**
- [DE] **Mini-player design style no longer resets on launch.**
- [DE] **Player background option "Canvas" was showing the raw key in every language.**

### 🔧 Changed
- [DE] **First-launch screen redesigned as a Material 3 welcome.**

## [6.4.41_DE-1.48.0-nightly] - 2026-09-01

### 🔧 Changed
- [DE] **Inner settings sub-screens restyled to Material 3.**

## [6.4.41_DE-1.47.0-nightly] - 2026-09-01

### ✨ Added
- [DE] **Animations master switch.**

## [6.4.41_DE-1.46.0-nightly] - 2026-09-01

### ✨ Added
- [DE] **Equalizer example profile + live editor.**

## [6.4.41_DE-1.45.0-nightly] - 2026-09-01

### ✨ Added
- [DE] **Custom accent color picker.**
- [DE] **6 new accent palette colors.**

## [6.4.41_DE-1.44.0-nightly] - 2026-09-01

### ✨ Added
- [DE] **Inline dropdowns for single-choice settings.**

## [6.4.41_DE-1.43.2-nightly] - 2026-09-01

### 🐛 Fixed
- [DE] **UI density calibrated.**
- [DE] **Duplicate option labels disambiguated.**

### ✨ Added
- [DOCS] `ERRORS.md` expanded from 13 to 29 VIVI-specific error codes (E1000–E1028).
- [WEBSITE] New interactive **Error codes** page (`errors.html`.

## [6.4.41_DE-1.43.1-nightly] - 2026-09-01

### 🔧 Changed
- [DE] License references now correctly say **modified GPL-3.0** (the LICENSE file itself was left untouched).

## [6.4.41_DE-1.43.0-nightly] - 2026-09-01

### ✨ Added
- [DE] **Settings sub-menus redesigned to match the mobile Material 3 look.**
- [DE] **ERRORS.md.**
- [WEBSITE] Footer now links to the error-code reference.

## [6.4.41_DE-1.42.1-nightly] - 2026-09-01

### ✨ Added
- [DE] **Full translation coverage for the new sub-screens.**

## [6.4.41_DE-1.42.0-nightly] - 2026-09-01

### ✨ Added
- [DE] **Equalizer.**
- [DE] **Data saver.**
- [DE] **AI Lyrics Translation.**

## [6.4.41_DE-1.41.17-nightly] - 2026-09-01

### 🐛 Fixed
- [DE] **WebView sign-in now captures the same cookies the manual method does.**

## [6.4.41_DE-1.41.16-nightly] - 2026-09-01

### 🐛 Fixed
- [DE] **WebView sign-in now captures the same cookie set the manual method does.**

## [6.4.41_DE-1.41.15-nightly] - 2026-08-29

### ✨ Added
- [DE] **Install guide shipped with every release and on the website.**

## [6.4.41_DE-1.41.14-nightly] - 2026-08-29

### 🐛 Fixed
- [DE] **Linux crash with `UnsatisfiedLinkError: OpenGLApi.glFlush()` now heals itself.**

### ✨ Added
- [DE] **AUR packaging assets are now attached to every release.**

## [6.4.41_DE-1.41.13-nightly] - 2026-08-29

### 🐛 Fixed
- [DE] **Login with the embedded WebView failed with `401 UNAUTHENTICATED` on `account_menu` after showing "saving session".**

## [6.4.41_DE-1.41.12-nightly] - 2026-08-29

### 🐛 Fixed
- [DE] **AppImage no longer crashes on Linux without a working libGL.**

## [6.4.41_DE-1.41.11-nightly] - 2026-08-29

### 🔧 Changed
- [DE] **Expressive theme now stays inside the Material palette.**

## [6.4.41_DE-1.41.10-nightly] - 2026-08-29

### 🐛 Fixed
- [DE] **12 strings showed raw key names in the UI.**

## [6.4.41_DE-1.41.9-nightly] - 2026-08-29

### 🐛 Fixed
- [DE] **Tooltips appear next to the pointer again.**

## [6.4.41_DE-1.41.8-nightly] - 2026-08-29

### 🔧 Changed
- [DE] **Tooltip placement is smarter.**

## [6.4.41_DE-1.41.7-nightly] - 2026-08-29

### 🐛 Fixed
- [DE] **Right Now Playing panel hides while the full player is open.**
- [DE] **Shuffle and repeat buttons now present in every player.**

## [6.4.41_DE-1.41.6-nightly] - 2026-08-29

### 🐛 Fixed
- [DE] **Tooltips no longer cover the click target.**

## [6.4.41_DE-1.41.5-nightly] - 2026-08-29

### 🐛 Fixed
- [DE] **Distinct icons for distinct actions.**

## [6.4.41_DE-1.41.4-nightly] - 2026-08-29

### ✨ Added
- [DE] **Right Now Playing panel toggle.**

## [6.4.41_DE-1.41.3-nightly] - 2026-08-29

### ✨ Added
- [DE] **Tooltips on accent color swatches.**

## [6.4.41_DE-1.41.2-nightly] - 2026-08-29

### ✨ Added
- [DE] **Hover tooltips on buttons.**

## [6.4.41_DE-1.41.1-nightly] - 2026-08-29

### 🐛 Fixed
- [DE] The sidebar "Artists" entry now opens the actual Artists screen (the library artists list) instead of the Queue.

## [6.4.41_DE-1.41.0-nightly] - 2026-08-29

### ✨ Added
- [DE] **Import your own font.**

### 🔧 Changed
- [DE] The font preview text in Italian now reads "Dove la parola fallisce la musica stupisce." (was "Il viaggio stesso è stato più bello della destinazione.").

## [6.4.41_DE-1.40.0-nightly] - 2026-08-29

### ✨ Added
- [DE] **Accent color intensity slider.**

## [6.4.41_DE-1.39.0-nightly] - 2026-08-29

### ✨ Added
- [DE] **Spotify-style shell (Phase 1).**
- **Sidebar.**

## [6.4.41_DE-1.38.2-nightly] - 2026-08-29

### 🐛 Fixed
- [DE] **No more accent color in the queue screen.**

## [6.4.41_DE-1.38.1-nightly] - 2026-08-29

### 🐛 Fixed
- [DE] **Accent color selection works again in Spotify style.**
- [DE] **Queue/Lyrics/History panel in the Expressive player no longer sits on an accent-tinted background.**

## [6.4.41_DE-1.38.0-nightly] - 2026-08-29

### ✨ Added
- [DE] **Spotify-style UI redesign — Phase 0 (theme foundations).**

## [6.4.41_DE-1.37.3-nightly] - 2026-08-29

### 🐛 Fixed
- [DE] **Mini-player design no longer resets to Apple on every start.**
- [DE] **Mini-player background style falls back to "Follow theme" for unknown keys.**

## [6.4.41_DE-1.37.2-nightly] - 2026-08-28

### 🐛 Fixed
- [DE] **Enabling the native title bar no longer crashes.**
- [DE] **Window no longer reopens stretched over the Windows taskbar.**

## [6.4.41_DE-1.37.1-nightly] - 2026-08-28

### 🐛 Fixed
- [DE] **"Must press play twice when paired".**
- [DE] **"Next" at the end of the queue now wraps to the first track.**

## [6.4.41_DE-1.37.0-nightly] - 2026-08-28

### ✨ Added
- [DE] **"Download VIVI for Android (APK)" button on the Devices/sync screen.**

## [6.4.41_DE-1.36.0-nightly] - 2026-08-28

### ✨ Added
- [DE] **Toggle between the native OS title bar and VIVI's custom one.**

## [6.4.41_DE-1.35.8-nightly] - 2026-08-28

### 🐛 Fixed
- [DE] **Fullscreen no longer traps the auto-hide Windows taskbar.**

## [6.4.41_DE-1.35.7-nightly] - 2026-08-28

### 🔧 Changed
- [DE] **Faster track start (time-to-audio roughly halved on first play).**

## [6.4.41_DE-1.35.6-nightly] - 2026-08-28

### 🐛 Fixed
- [DE] **Window controls (minimize / maximize / close) are now always visible.**

## [6.4.41_DE-1.35.5-nightly] - 2026-08-28

### 🔧 Changed
- [DE] **Playback starts while the track is still downloading (progressive streaming).**
- [DE] **Stream resolution is much faster.**
- [DE] **The "downloading" indicator is accurate.**

## [6.4.41_DE-1.35.4-nightly] - 2026-08-28

### 🐛 Fixed
- [DE] **"Module with the Main dispatcher is missing" crash.**

## [6.4.41_DE-1.35.3-nightly] - 2026-08-28

### 🐛 Fixed
- [DE] **Expressive player volume slider actually works.**
- [DE] **Pressing play after a track finished now restarts it.**
- [DE] **Truncated audio cache files are re-downloaded.**

## [6.4.41_DE-1.35.2-nightly] - 2026-08-28

### 🐛 Fixed
- [DE] Device sync no longer lets a stale remote snapshot override a fresh local action:

## [6.4.41_DE-1.35.1-nightly] - 2026-08-28

### 🔧 Changed
- [DE] Apple/Cider-style visual polish across the UI:

## [6.4.41_DE-1.35.0-nightly] - 2026-08-28

### ✨ Added
- [DE] Cider-inspired desktop features (Settings → Desktop features):
- [DE] New translations for all of the above across all 47 languages (13 new keys, `player_background_visualizer` … `quit`).

### 📌 Notes

## [6.4.41_DE-1.34.8-nightly] - 2026-08-28

### 🐛 Fixed
- [DE] The full player (Classic/New/V2 designs) had no visible way back.

## [6.4.41_DE-1.34.7-nightly] - 2026-08-28

### ⏪ Reverted
- [APK] Reverted the Telegram channel change on the Android app (About + Welcome screens) and the issue template.

### 🐛 Fixed
- [DE] The embedded sign-in now captures the FULL session cookie set.

## [6.4.40_DE-1.34.6-nightly] - 2026-08-28

### 🔧 Changed

## [6.4.39_DE-1.34.5-nightly] - 2026-08-28

### 🐛 Fixed
- [DE] Google sign-in via the embedded WebView ended with "unknown error" after the window closed.

## [6.4.39_DE-1.34.4-nightly] - 2026-08-28

### 🐛 Fixed
- [DE] Selecting a player design in Settings → Player & audio → player design (and the density screen) could make the whole UI explode and freeze the app.

## [6.4.39_DE-1.34.3-nightly] - 2026-08-28

### 🐛 Fixed
- [DE] Windows window management overhaul: the app forced Compose's `WindowPlacement.Maximized`/Fullscreen on every start and when opening the player.

## [6.4.39_DE-1.34.2-nightly] - 2026-08-28

### 🐛 Fixed
- [DE] The embedded login WebView stayed permanently white in the packaged app.

### 🔧 Changed
- [DE] The sidebar now compresses to a compact icon rail (72dp.

## [6.4.39_DE-1.34.1-nightly] - 2026-08-28

### ✨ Added
- [DE] Completed the translations of the 4 new UI strings (`close`.

## [6.4.39_DE-1.34.0-nightly] - 2026-08-28

### ✨ Added
- [DE] Ported the new UI (from the NewUI_desktop.zip line.
- [DE] Modernized MiniPlayer suite: 3 design variants (Classic.
- [DE] Spotify-inspired 3-panel card layout with top navigation header (`spotifyLayout`) and right Now-Playing panel (`showRightSidebar`) with multi-artist parsing and profile photo resolution.
- [DE] Player personalization options persisted in settings: `miniPlayerDesign`, `miniPlayerBackgroundStyle`, `pureBlackMiniPlayer`, `isFullscreen`, `showRightSidebar`, `spotifyLayout`, `playerArtSize`, `playerArtTopOffset`, `playerArtCornerRadius`.

### 🔧 Changed
- [DE] Version numbering now follows SemVer properly.
- [DE] Re-integrated all features added since the NewUI base.

## [6.4.39_DE-1.33.129-nightly] - 2026-08-28

### 🔧 Changed
- [DE] The Library sidebar entry (when not signed in) now shows the two sign-in options (Accedi con Google / manual cookie) directly instead of a "Log in" button that opened a separate screen —.

## [6.4.39_DE-1.33.128-nightly] - 2026-08-28

### 🔧 Changed
- [DE] Removed the intermediate "Log in" step in Settings → Account.

### 🐛 Fixed
- [DE] Embedded login WebView could stay blank white in the packaged app even though the page loaded.

## [6.4.39_DE-1.33.127-nightly] - 2026-08-28

### 🐛 Fixed
- [DE] Completed every missing desktop translation: 3,582 missing per-language strings were added across all 47 languages (new batches `desktop_extra_translations_24..31`).
- [DE] Fixed the desktop localization generator dropping translations.
- [DE] Added `scripts/check_localization.py` to verify used-key coverage and per-language completeness.

## [6.4.39_DE-1.33.126-nightly] - 2026-08-28

### 🔧 Changed
- [DE] Removed the redundant "Accesso" heading from the login screen.

## [6.4.39_DE-1.33.125-nightly] - 2026-08-27

### 🐛 Fixed
- [DE] Embedded login WebView could stay blank white after opening in the packaged app.

## [6.4.39_DE-1.33.124-nightly] - 2026-08-27

### 🐛 Fixed
- [DE] Fixed the embedded YouTube login WebView failing in packaged builds with `NoClassDefFoundError.

## [6.4.39_DE-1.33.123-nightly] - 2026-08-27

### 🐛 Fixed
- [DE] Fixed embedded JavaFX login startup after the `.122` build.

## [6.4.39_DE-1.33.122-nightly] - 2026-08-27

### 🐛 Fixed
- [DE] Fixed embedded WebView packaging by replacing the Swing-dependent `JFXPanel` with a direct JavaFX `Stage`.

## [6.4.39_DE-1.33.121-nightly] - 2026-08-27

### 🐛 Fixed
- [DE] Fixed all desktop packaging jobs failing at `createRuntimeImage`: external JavaFX modules are no longer incorrectly passed to jlink without a module path.

## [6.4.39_DE-1.33.120-nightly] - 2026-08-27

### 🐛 Fixed
- [DE] Fixed the embedded JavaFX login crash `NoClassDefFoundError: jdk/swing/interop/SwingInterOpUtils` by packaging the required `jdk.swing.interop` runtime module.
- [DE] Prevented repeated login WebView initialization failures from reopening the same error indefinitely; failed initialization now falls back cleanly and only once.

## [6.4.39_DE-1.33.119-nightly] - 2026-08-27

### 🐛 Fixed
- [DE] Fixed Windows toast details continuing to display the old logo from the persistent `~/.vivimusic/logo_vmde.png` cache.

## [6.4.39_DE-1.33.118-nightly] - 2026-08-27

### ✨ Added
- [DE] Reworked YouTube login with a real embedded JavaFX sign-in window that opens directly on Google's login page.
- [DE] Added a clear browser fallback and a collapsible manual Cookie / DATASYNC_ID / VISITOR_DATA login path for systems where the embedded WebView cannot start.
- [DE] Added a language-screen notice explaining that translations were created with AI tools and may not be 100% reliable.

### 🐛 Fixed
- [DE] Fixed GitHub Auto Release changelog extraction when versioned entries include a release-channel suffix such as `-nightly`; release notes are no longer empty.

### 🔧 Changed
- [DE] Documented in the README that Windows receives the most compatibility testing because it is the only platform currently available to the lead developer for local testing.

## [6.4.39_DE-1.33.117-nightly] - 2026-08-27

### ✨ Added
- [DE] When the Updates screen finds a new version.

## [6.4.39_DE-1.33.116-nightly] - 2026-08-27

### 🔧 Changed
- [DE] The official logo is now circular everywhere it appears.
- [DE] `scripts/generate_desktop_icons.py` applies an anti-aliased circular mask automatically, so future logo regenerations keep the round shape.

## [6.4.39_DE-1.33.115-nightly] - 2026-08-27

### ✨ Added
- [DE] Added a user-friendly YouTube Music login entry point with an optional embedded WebView attempt.

## [6.4.39_DE-1.33.114-nightly] - 2026-08-27

### ✨ Added
- [DE] Update installers older than seven days are removed automatically while recent installers remain available for reuse.

## [6.4.39_DE-1.33.113-nightly] - 2026-08-27

### 🔧 Changed
- [DE] Replaced the desktop branding in the intro.

## [6.4.39_DE-1.33.112] - 2026-08-19

### 🐛 Fixed

- [DE] The play/pause button could stay stuck on "play" while audio kept.

## [6.4.39_DE-1.33.111] - 2026-08-19

### ✨ Added

- [APK] Complete translations for the Devices section in all 54 supported.
- [DE] A "How to connect your phone" step-by-step card on the Device sync.

## [6.4.38_DE-1.33.110] - 2026-08-19

### 🐛 Fixed

- [DE] The desktop now waits for the phone while the phone is still resolving.

## [6.4.37_DE-1.33.109] - 2026-08-19

### 🐛 Fixed

- [APK] Added `VISIONOS` and `ANDROID_NO_SDK` to the mobile fallback client.

## [6.4.36_DE-1.33.109] - 2026-08-19

### 🐛 Fixed

- [APK] Some tracks still failed with `IO_UNSPECIFIED (2000): Video.

## [6.4.35_DE-1.33.109] - 2026-08-19

### 🔧 Changed

- [DE] On Debian (and Debian-derived distros such as Ubuntu) the updater now.

## [6.4.35_DE-1.33.108] - 2026-08-19

### 🐛 Fixed

- [APK] When every innerTube client is bot-flagged (`Video unavailable`), the.

## [6.4.34_DE-1.33.108] - 2026-08-19

### 🐛 Fixed

- [APK] Stream downloads now send a browser-like `User-Agent` (Firefox),.

## [6.4.33_DE-1.33.108] - 2026-08-19

### 🐛 Fixed

- [APK] Mobile stream resolution now prefers a NewPipe-signed URL (matched to.

## [6.4.32_DE-1.33.108] - 2026-08-19

### 🔧 Changed

- [APK] Simplified the mobile stream-retry path: extracted a shared.

## [6.4.31_DE-1.33.108] - 2026-08-19

### ✨ Added

- [APK] A visible on-screen toast now appears when playback fails, showing the.

## [6.4.30_DE-1.33.108] - 2026-08-19

### 🐛 Fixed

- [APK] Stream-resolution retry now rotates the guest identity (visitorData).

## [6.4.29_DE-1.33.108] - 2026-08-18

### 🐛 Fixed

- [DE] Developer options RAM now shows the program's real usage instead of the.
- [DE] Developer options overlay switches (movable / show in title bar) now.

## [6.4.29_DE-1.33.107] - 2026-08-18

### ✨ Added

- [DE] Look-ahead cache prefetch: while a track is loaded (playing or paused).

## [6.4.29_DE-1.33.106] - 2026-08-18

### 🐛 Fixed

- [DE] Songs failing to start or "ending" after a few seconds when the stream.

## [6.4.29_DE-1.33.105] - 2026-08-18

### 🐛 Fixed

- [DE] Backspace no longer navigates back while typing in a search/text field.

## [6.4.29_DE-1.33.104] - 2026-08-18

### 🐛 Fixed

- [DE] A failed track auto-skipped through the whole queue.

## [6.4.29_DE-1.33.103] - 2026-08-18

### 🐛 Fixed

- [DE] Song recognition (Shazam) always returned "No match found".

## [6.4.29_DE-1.33.102] - 2026-08-18

### ✨ Added

- [DE] Settings search: a search icon sits next to the "Settings" title; it.

## [6.4.29_DE-1.33.101] - 2026-08-18

### 🐛 Fixed

## [6.4.28_DE-1.33.100] - 2026-08-18

### 🔧 Changed

- [DE] Rebuilt the intro splash from scratch as a fully native Compose animation.

## [6.4.28_DE-1.33.99] - 2026-08-18

### ✨ Added

- [DE] "Preview intro" button in the Intro screen: plays the startup intro.

### 🔧 Changed

- [DE] In Settings → Appearance the intro is now a dedicated entry ("Intro") that.

## [6.4.28_DE-1.33.98] - 2026-08-18

### 🐛 Fixed

- [DE] Intro splash now plays with correct colours and smooth fps.

## [6.4.28_DE-1.33.97] - 2026-08-18

### ✨ Added

- [DE] "Show intro on startup" toggle also available in Settings → Appearance.

## [6.4.28_DE-1.33.96] - 2026-08-18

### 🐛 Fixed

- [DE] Changing the app font now actually changes the whole interface: the.

## [6.4.28_DE-1.33.95] - 2026-08-18

### 🔧 Changed

- [DE] Full redesign of the Developer options screen: a prominent enable.

## [6.4.28_DE-1.33.94] - 2026-08-18

### 🔧 Changed

- [DE] The startup intro is no longer a GIF: it now plays a sequence of.

## [6.4.28_DE-1.33.93] - 2026-08-18

### 🐛 Fixed

- [DE] Back navigation no longer bounces between repeated screens: navigating.

## [6.4.28_DE-1.33.92] - 2026-08-18

### ✨ Added

- [DE] Animated intro splash screen (the bundled GIF) played once at startup,.
- [DE] Settings → System sub-menu: Developer options moved here (under.

### 🗑️ Removed

- [DE] Removed the changelog button from the About screen (still available in.

## [6.4.28_DE-1.33.91] - 2026-08-18

### 🗑️ Removed

- [DE] Removed the standalone Stats screen (duplicate of VIVI Wrapped) and.

## [6.4.28_DE-1.33.90] - 2026-08-18

### ✨ Added

- [DE] Completed translations for the Phase 10 port across all 47 languages:

## [6.4.28_DE-1.33.89] - 2026-08-18

### ✨ Added

- [DE] Artist page sub-tabs: Songs / Albums / Items (TabRow).
- [DE] Song recognition (Shazam): records the microphone, resamples to.
- [DE] Keyboard navigation: Ctrl+Z / Ctrl+Y undo/redo the screen history,.

## [6.4.28_DE-1.33.88] - 2026-08-18

### ✨ Added

- [DE] New Release albums screen (grid of the latest albums, from the.
- [DE] Charts screen (trending + top songs/videos sections, from the.
- [DE] Dedicated Mood & genres screen (full list instead of the Home preview).
- [DE] Stats screen (session listening time, tracks played and top songs,.
- [DE] Auto-playlist detail screens: Liked / Top / etc.
- [DE] Listen Together screen: a JSON WebSocket client (same protocol as the.

## [6.4.28_DE-1.33.87] - 2026-08-18

### ✨ Added

- [DE] Commit screen (Settings → Updates → Commits): lists the most recent.

## [6.4.28_DE-1.33.86] - 2026-08-18

### 🐛 Fixed

- [DE] The Queue swipe-to-play hint ("▶ Play") no longer stays visible on top.

## [6.4.28_DE-1.33.85] - 2026-08-18

### 🐛 Fixed

- [DE] Player sync (seek bar + play/pause) now follows the peer even over a.

## [6.4.28_DE-1.33.84] - 2026-08-18

### ✨ Added

- [DE] Stream cache now offers a "Forever" option past 60 minutes: the resolved.

## [6.4.28_DE-1.33.83] - 2026-08-18

### 🔧 Changed

- [DE] The player design variants now actually differ: Classic is the.

## [6.4.28_DE-1.33.82] - 2026-08-18

### 🔧 Changed

- [DE] UI density scale now also offers values above 100% (110, 120, 125, 130,.

## [6.4.28_DE-1.33.81] - 2026-08-18

### 🐛 Fixed

## [6.4.27_DE-1.33.80] - 2026-08-18

### 🔧 Changed

- [DE] The VIVI Wrapped card is now hidden from the Home screen by default.

## [6.4.27_DE-1.33.79] - 2026-08-18

### ✨ Added

- [DE] Settings → VIVI Wrapped sub-screen: the session listening-stats card.
- [DE] Appearance now hosts the player personalization: a new "Player.

### 🐛 Fixed

- [DE] Raw keys no longer appear as "code language" UI: 10 keys were missing.
- [DE] Queue screen: the swipe-left remove hint ("✕ remove from queue").
- [DE] Full player: add-to-playlist now sits under the song title, next to.
- [DE] Startup volume guard: if the Windows master volume is muted, VIVI.

### 🗑️ Removed

- [DE] Quick settings (Tune) button in the sidebar.

## [6.4.27_DE-1.33.78] - 2026-08-18

### 🔧 Changed

- [DE] The stream resolution cache TTL is now configurable: a slider in.

## [6.4.27_DE-1.33.77] - 2026-08-17

### 🔧 Changed

- [DE] Stream resolution cache: resolved audio URLs are cached in memory for.

## [6.4.27_DE-1.33.76] - 2026-08-17

### ✨ Added

- [DE] Integrations sub-screen (Settings → Integrations):

## [6.4.27_DE-1.33.75] - 2026-08-17

### ✨ Added

- [DE] Advanced lyrics: line-spacing slider (1.0–2.0, Settings → Lyrics).

## [6.4.27_DE-1.33.74] - 2026-08-17

### ✨ Added

- [DE] Quick settings popup: a Tune button at the bottom of the sidebar.

## [6.4.27_DE-1.33.73] - 2026-08-17

### ✨ Added

- [DE] Local search history: recent searches appear as chips on the Search.
- [DE] Privacy sub-screen (Settings → Privacy): "Pause listen history".

## [6.4.27_DE-1.33.72] - 2026-08-17

### ✨ Added

- [DE] Home: "Quick Picks vs Last Listen" toggle (chip row) so only the.
- [DE] Home: "Randomize" button that shuffles the order of the home.
- [DE] Home: "VIVI Wrapped · This session" card with tracks played,.

## [6.4.27_DE-1.33.71] - 2026-08-17

### ✨ Added

- [DE] Sort chips in the Library (all tabs): A–Z / Z–A, plus "By artist".

## [6.4.27_DE-1.33.70] - 2026-08-17

### ✨ Added

- [DE] Dynamic theme (Material You): the "Dynamic" accent swatch now reads.

## [6.4.27_DE-1.33.69] - 2026-08-17

### ✨ Added

- [DE] Song swipe gestures in the Queue screen: swipe a row right to play.

## [6.4.27_DE-1.33.68] - 2026-08-17

### ✨ Added

- [DE] Mini-player styles: Standard / Apple / Outline / Pure black.
- [DE] Swipe-to-expand on the mini player: drag it up to open the full.

## [6.4.27_DE-1.33.67] - 2026-08-17

### ✨ Added

- [DE] Player design variants: Classic / New / V2 / Expressive (Settings →.
- [DE] Player background styles: Gradient / Blur / Glow / Apple Music /.
- [DE] Rotating artwork option (settings toggle): the album art rotates.

## [6.4.27_DE-1.33.66] - 2026-08-17

### ✨ Added

- [DE] Player slider styles: Slim / Squiggly / Wavy (Settings → Player & audio),.

## [6.4.27_DE-1.33.65] - 2026-08-17

### ✨ Added

- [DE] Screen transitions between navigations: Off / Fade / Slide (Settings →.

## [6.4.27_DE-1.33.64] - 2026-08-17

### ✨ Added

- [DE] UI density scale (100 / 85 / 75 / 65 / 55 %) applied to the whole.

## [6.4.27_DE-1.33.63] - 2026-08-17

### 🐛 Fixed

- [DE] The desktop no longer stays silent (appears in the Windows mixer but.

## [6.4.27_DE-1.33.62] - 2026-08-17

### 🐛 Fixed

## [6.4.26_DE-1.33.61] - 2026-08-17

### 🐛 Fixed

- [APK] Restore swap is no longer silent on failure: the database target.

## [6.4.25_DE-1.33.61] - 2026-08-17

### 🐛 Fixed

- [APK] The restore picker now accepts any file (`*/*`) so old `.backup` files.

## [6.4.24_DE-1.33.61] - 2026-08-17

### 🐛 Fixed

## [6.4.23_DE-1.33.60] - 2026-08-17

### 🐛 Fixed

- [APK] Update check from the fork source (`PiBOH/vivi-music-de`) now works:

## [6.4.22_DE-1.33.60] - 2026-08-17

### 🐛 Fixed

- [DE] Native Windows toast notifications now actually appear in the Action.

## [6.4.22_DE-1.33.59] - 2026-08-17

### 🐛 Fixed

- [DE] Developer options network stats (down/up speed + total traffic) now show.

## [6.4.22_DE-1.33.58] - 2026-08-17

### 🐛 Fixed

## [6.4.21_DE-1.33.57] - 2026-08-17

### 🐛 Fixed

- [DE] The in-app update notification no longer auto-dismisses while it is.

## [6.4.21_DE-1.33.56] - 2026-08-17

### ✨ Added

- [DE] A "Send test notification" button in Settings → Notifications so native.

### 🔧 Changed

- [DE] Native notification path now writes a diagnostic log to.

## [6.4.21_DE-1.33.55] - 2026-08-17

### 🔧 Changed

- [DE] The About "website" entry now points to the VIVI Music DE GitHub Pages.

### 🌐 Website

## [6.4.21_DE-1.33.54] - 2026-08-17

### 🐛 Fixed

- [DE] Tracks restored from the persistent queue (or whose load failed earlier).

## [6.4.21_DE-1.33.53] - 2026-08-17

### ✨ Added

- [DE] Windows native notifications now land in the **Action Center / notification.

## [6.4.21_DE-1.33.52] - 2026-08-17

### 🔧 Changed

- [DE] `version.txt` is reorganized into a self-documenting six-line layout:

## [6.4.21_DE-1.33.51] - 2026-08-17

### 🐛 Fixed

- [DE] Opening Appearance → Theme & colors, App font, or Canvas no longer.

## [6.4.21_DE-1.33.50] - 2026-08-17

### ✨ Added

- [APK] Restoring a backup now shows a confirmation dialog with the backup's.

## [6.4.20_DE-1.33.50] - 2026-08-17

### ✨ Added

- [APK] Restore now validates the backup's database before applying it (SQLite.

## [6.4.19_DE-1.33.50] - 2026-08-17

### 🐛 Fixed

## [6.4.18_DE-1.33.49] - 2026-08-17

### ✨ Added

- [DE] Complete port of the mobile Appearance sub-menu into three dedicated.

## [6.4.18_DE-1.33.48] - 2026-08-17

### 🐛 Fixed

## [6.4.17_DE-1.33.48] - 2026-08-17

### 🐛 Fixed

- [APK] Restoring an old backup (e.g.

## [6.4.16_DE-1.33.48] - 2026-08-17

### 🔧 Changed

- [APK] Mobile backups now use the `.vividroid.backup` extension (desktop keeps.

## [6.4.15_DE-1.33.48] - 2026-08-17

### 🐛 Fixed

- [DE] The Inno Setup installer now actually launches the app when "Start VIVI.

## [6.4.15_DE-1.33.47] - 2026-08-17

### 🔧 Changed

- [APK] Mobile backup files now use the `.vivide.backup` extension (manual and.

## [6.4.14_DE-1.33.47] - 2026-08-17

### 🐛 Fixed

- [APK] The debug APK build (CI) no longer fails during resource merge: the new.

## [6.4.13_DE-1.33.47] - 2026-08-17

### ✨ Added

### 🐛 Fixed

- [DE] The seek slider no longer stays disabled or stuck at the end: the track.
- [DE] Position sync no longer fights itself: the periodic re-sync tick only.

## [6.4.12_DE-1.33.46] - 2026-08-16

### 🐛 Fixed

- [APK] Restoring a backup no longer crashes while choosing the file or when.

## [6.4.11_DE-1.33.46] - 2026-08-16

### 🐛 Fixed

- [DE] All in-app notifications now auto-dismiss after the configured time.
- [DE] Native notifications keep a single persistent tray icon (created once.

## [6.4.11_DE-1.33.45] - 2026-08-16

### 🐛 Fixed

- [DE] LAN sync now works when the computer is connected to the phone's.

## [6.4.11_DE-1.33.44] - 2026-08-16

### 🔧 Changed

- [DE] Manual backups now include the date and timestamp in their filename.

## [6.4.11_DE-1.33.43] - 2026-08-16

### 🐛 Fixed

- [APK] Restoring a backup no longer freezes and then corrupts the app: the.

## [6.4.10_DE-1.33.43] - 2026-08-16

### ✨ Added

## [6.4.9_DE-1.33.42] - 2026-08-16

### ✨ Added

- [DE] The crash/error dialog now has a "Copy error" button alongside "OK":

## [6.4.9_DE-1.33.41] - 2026-08-16

### 🔧 Changed

- [DE] Backups (manual and automatic) now use a single `.vivide.backup` file.

### 🐛 Fixed

- [DE] "Restart now" after restoring a backup now actually relaunches the app:

## [6.4.9_DE-1.33.40] - 2026-08-16

### 🐛 Fixed

- [DE] Native system notifications now use the real VIVI Music DE logo as their.

## [6.4.9_DE-1.33.39] - 2026-08-16

### ✨ Added

## [6.4.8_DE-1.33.38] - 2026-08-16

### 🐛 Fixed

## [6.4.7_DE-1.33.37] - 2026-08-16

### 🐛 Fixed

- [DE] The update notification and the Updates screen now share a single.

## [6.4.7_DE-1.33.36] - 2026-08-16

### ✨ Added

- [DE] Full backup & restore, ported from mobile: a backup now includes.
- [DE] Automatic backups: optional weekly backup and an optional "backup before.

### 🔧 Changed

- [DE] Developer options screen reorganized into clear sections (display,.

## [6.4.7_DE-1.33.35] - 2026-08-16

### 🐛 Fixed

- [DE] Settings (e.g.

## [6.4.7_DE-1.33.34] - 2026-08-16

### 🐛 Fixed

- [DE] The player seek slider no longer snaps to the start or the end: it is.

## [6.4.7_DE-1.33.33] - 2026-08-16

### 🐛 Fixed

## [6.4.6_DE-1.33.32] - 2026-08-16

### 🔧 Changed

- [DE] The CI debug build now restores a persistent debug keystore from the.

## [6.4.6_DE-1.33.31] - 2026-08-16

### 🐛 Fixed

- [APK] Scanning a QR code now first disconnects and un-pairs an existing.

## [6.4.5_DE-1.33.31] - 2026-08-16

### 🔧 Changed

- [DE] On Windows the updater now prefers the Inno Setup `.exe` installer over.

## [6.4.5_DE-1.33.30] - 2026-08-16

### 🐛 Fixed

- [DE] The Updates screen no longer offers to re-download an installer that is.

## [6.4.5_DE-1.33.29] - 2026-08-16

### ✨ Added

- [DE] Notification history: every notification (in-app and native) is recorded.
- [DE] Configurable in-app notification auto-dismiss (3/5/10/15/30 seconds,.

## [6.4.5_DE-1.33.28] - 2026-08-16

### 🔧 Changed

- [DE] Windows system volume now drives the **master** volume (the speaker icon.

## [6.4.5_DE-1.33.27] - 2026-08-16

### 🐛 Fixed

- [DE] The Linux `.deb` now installs on Debian: jpackage auto-detected.

## [6.4.5_DE-1.33.26] - 2026-08-16

### 🔧 Changed

- [DE] Clearer updater wording: the update button now reads "Check for.

## [6.4.5_DE-1.33.25] - 2026-08-16

### 🐛 Fixed

- [DE] Critical startup crash on Windows: the WinMM binding looked up.

## [6.4.5_DE-1.33.24] - 2026-08-16

### 🔧 Changed

- [DE] Translation quality pass: filled in the remaining desktop-only keys.

## [6.4.5_DE-1.33.23] - 2026-08-16

### 🐛 Fixed

- [DE] Windows Inno Setup installer now always shows the "Select Destination.

## [6.4.5_DE-1.33.22] - 2026-08-16

### 🐛 Fixed

- [DE] Native (OS) volume now actually syncs on Windows: the WinMM call used.

## [6.4.4_DE-1.33.21] - 2026-08-16

### ✨ Added

- [DE] Single-instance guard: launching the app while another instance is.
- [DE] Notifications now cover **all** app notifications, not just updates:

### 🔧 Changed

- [DE] Completed all remaining desktop-only translations (developer options and.
- [DE] `Localization.kt` is now emitted as one top-level function per language.

## [6.4.4_DE-1.33.20] - 2026-08-16

### 🐛 Fixed

- [DE] Seek bar: decoded position is now reported at ~10 fps (throttled from.
- [DE] In-app (VIVI) volume sync now also pushes when nothing is playing, and.
- [DE] Windows OS volume sync: `waveOutGetVolume`/`waveOutSetVolume` were.

## [6.4.4_DE-1.33.19] - 2026-08-16

### ✨ Added

- [DE] New "Notifications" settings sub-menu: update notifications can now be.

## [6.4.4_DE-1.33.18] - 2026-08-16

### ✨ Added

- [DE] "Add to playlist" is now also available on the full Player screen.

## [6.4.4_DE-1.33.17] - 2026-08-16

### ✨ Added

- [DE] A dedicated "Add to playlist" button on every song row (Home, Search,.

## [6.4.4_DE-1.33.16] - 2026-08-16

### 🐛 Fixed

- [DE] Seek slider couldn't be dragged to the middle: the track duration is now.

### 🔧 Changed

## [6.4.3_DE-1.33.15] - 2026-08-16

### 🐛 Fixed

- [DE] Crash ("layouts are not part of the same hierarchy") when interacting.

## [6.4.3_DE-1.33.14] - 2026-08-16

### 🐛 Fixed

- [DE] Crash ("layouts are not part of the same hierarchy") when confirming a.

## [6.4.3_DE-1.33.13] - 2026-08-16

### 🐛 Fixed

- [DE] Apostrophes now render correctly everywhere instead of showing a.

## [6.4.3_DE-1.33.12] - 2026-08-16

### 🔧 Changed

- [DE] GitHub release titles now use the `Vivi Music <mobile>_DE <desktop>`.

## [6.4.3_DE-1.33.11] - 2026-08-16

### 🐛 Fixed

- [DE] Crash when changing the update-check interval: selecting an option in.

## [6.4.3_DE-1.33.10] - 2026-08-16

### 🐛 Fixed

### 📌 Note

## [6.4.3_DE-1.33.9] - 2026-08-16

### ✨ Added

- [DE] Backup & restore sub-menu in Settings: export the desktop settings to a.

## [6.4.3_DE-1.33.8] - 2026-08-16

### 🔧 Changed

- [DE] The GitHub logo in About → Community is now a vector icon (ported from.

## [6.4.3_DE-1.33.7] - 2026-08-16

### 🔧 Changed

### 📌 Note

## [6.4.3_DE-1.33.6] - 2026-08-16

### 🐛 Fixed

## [6.4.2_DE-1.33.6] - 2026-08-16

### ✨ Added

## [6.4.1_DE-1.33.5] - 2026-08-16

### ✨ Added

- [DE] The player now shows its load state while a track starts: "Resolving.

## [6.4.1_DE-1.33.4] - 2026-08-16

### 🐛 Fixed

- [DE] Playback now retries automatically: when the stream fails to resolve or.

## [6.4.1_DE-1.33.3] - 2026-08-16

### 🔧 Changed

- [DE] Completed the translations for the playlist and song-menu strings.

## [6.4.1_DE-1.33.2] - 2026-08-16

### ✨ Added

- [DE] Full song context menu (⋮): like / unlike, add to / remove from library,.

## [6.4.1_DE-1.33.1] - 2026-08-16

### ✨ Added

- [DE] Drag-to-reorder inside the playlist detail screen (drag the ⠿ handle);.

## [6.4.1_DE-1.33.0] - 2026-08-16

### ✨ Added

- [DE] Local playlist system: create / rename / delete playlists, add songs to.

### 🔧 Changed

- [DE] The sidebar gains a "Playlists" entry that opens the local playlist list.

## [6.4.0_DE-1.32.2] - 2026-08-16

### 🐛 Fixed

- [DE] The player's seek slider could only land on the start or the end: the.
- [DE] Seeking while paused now stays paused instead of forcing playback to.
- [DE] Stale or truncated cached audio files are now detected and re-downloaded,.

## [6.4.0_DE-1.32.1] - 2026-08-16

### 🐛 Fixed

- [DE] "Start LAN server" now retries the pairing-code request until the relay.

## [6.4.0_DE-1.32.0] - 2026-08-16

### ✨ Added

- [DE] Developer options "Title bar only" display mode (stats only in the.
- [DE] Queue entry in the sidebar (opens the Queue screen directly).
- [DE] "View changelog" button in Settings → Updates.

### 🔧 Changed

- [DE] Player layout is now two columns: a smaller artwork on the left and the.
- [DE] Clicking the mini-player toggles the full player (open / go back).
- [DE] The About screen's GitHub row now uses the correct GitHub logo.
- [DE] README: smaller logo and @PiBOH added to Special Thanks.

### 🐛 Fixed

- [DE] Stream resolution is more resilient: it retries transient failures and.

## [6.3.0_DE-1.31.0] - 2026-08-15

### ✨ Added

## [6.2.5_DE-1.30.3] - 2026-08-15

### 🔧 Changed

- [DE] The Changelog screen now uses a vertical version selector (left list of.

## [6.2.5_DE-1.30.2] - 2026-08-15

### 🔧 Changed

- [DE] The About screen now shows real thumbnails: the developer's avatar.

## [6.2.5_DE-1.30.1] - 2026-08-15

### 🔧 Changed

- [APK] The Devices entry in Settings now uses the same phone+monitor.

## [6.2.4_DE-1.30.1] - 2026-08-15

### 🐛 Fixed

## [6.2.3_DE-1.30.0] - 2026-08-15

### ✨ Added

- [DE] Developer options improvements: the "Developer options" entry is now.

### 🐛 Fixed

- [DE] Starting the LAN server now reliably auto-generates the pairing code:

## [6.2.3_DE-1.29.0] - 2026-08-15

### ✨ Added

## [6.2.2_DE-1.28.6] - 2026-08-15

### 🐛 Fixed

- [DE] Installing both the MSI and the Inno Setup EXE no longer leaves two.

## [6.2.2_DE-1.28.5] - 2026-08-15

### 🐛 Fixed

- [DE] **Critical:**

## [6.2.2_DE-1.28.4] - 2026-08-15

### 🐛 Fixed

- [APK] The Android APK now reports version `6.2.2` (it was still showing.

## [6.2.2_DE-1.28.3] - 2026-08-15

### 🐛 Fixed

- [DE] **Critical:**

## [6.2.2_DE-1.28.2] - 2026-08-15

### 🐛 Fixed

- [DE] The Changelog screen now lists every version vertically (newest first).

## [6.2.2_DE-1.28.1] - 2026-08-15

### 🐛 Fixed

- [DE] Audio playback no longer fails with "HTTP 403 downloading audio": the.

## [6.2.2_DE-1.28.0] - 2026-08-15

### ✨ Added

- [DE] The Updates screen now checks for updates automatically every time it.

### 🐛 Fixed

- [DE] "Download" in the Updates screen no longer opens the browser instead of.

## [6.2.2_DE-1.27.1] - 2026-08-15

### 🐛 Fixed

- [DE] Content no longer gets clipped when the window is resized smaller: the.

## [6.2.2_DE-1.27.0] - 2026-08-15

### ✨ Added

- [DE] Developer options: tap the About "Version code" seven times to enable.

## [6.2.2_DE-1.26.4] - 2026-08-15

### 🐛 Fixed

- [DE] Audio playback no longer fails with "HTTP 403 downloading audio": the.

## [6.2.2_DE-1.26.3] - 2026-08-15

### 🐛 Fixed

- [DE] The About screen "Version code" was derived from the SemVer.

## [6.2.2_DE-1.26.2] - 2026-08-15

### 🔧 Changed

- [DE] Starting the LAN server now automatically generates a pairing code, and.

## [6.2.2_DE-1.26.1] - 2026-08-15

### 🐛 Fixed

## [6.2.1_DE-1.26.0] - 2026-08-15

### ✨ Added

- [DE] Redesigned the About screen to mirror the mobile layout: centered.

## [6.2.1_DE-1.25.0] - 2026-08-15

### ✨ Added

## [6.2.1_DE-1.24.2] - 2026-08-15

### 🐛 Fixed

- [DE] "HTTP 403 downloading audio" is now far more robust: the resolver.

## [6.2.1_DE-1.24.1] - 2026-08-15

### 🔧 Changed

- [DE] The update notification now offers "Open installer" (instead of.

## [6.2.1_DE-1.24.0] - 2026-08-15

### ✨ Added

- [DE] A non-invasive banner now appears when a newer release is available,.

## [6.2.1_DE-1.23.0] - 2026-08-15

### ✨ Added

- [DE] The settings sub-screens now carry real functionality instead of being.

## [6.2.1_DE-1.22.1] - 2026-08-15

### 🐛 Fixed

- [DE] Audio download no longer fails with "HTTP 403 downloading audio".

## [6.2.1_DE-1.22.0] - 2026-08-15

### ✨ Added

- [DE] Search now has mobile-style filter chips (All / Songs / Videos / Albums /.
- [DE] Library tabs now use Material 3 filter chips and the Songs tab gains a.
- [DE] Album and Playlist screens gain a "Shuffle" action next to "Play all".

## [6.2.1_DE-1.21.0] - 2026-08-15

### ✨ Added

- [DE] Settings are now organized into mobile-style sub-screens (Language,.
- [DE] New Content sub-screen: pick the YouTube content language and region.
- [DE] New Lyrics sub-screen with a "synced lyrics" toggle that enables or.
- [DE] New Privacy sub-screen to clear the session, cache and downloaded.

## [6.2.1_DE-1.20.1] - 2026-08-15

### 🔧 Changed

- [DE] The auto-release no longer builds the APK itself: it waits for the.

## [6.2.1_DE-1.20.0] - 2026-08-15

### 🐛 Fixed

- [DE] Audio playback now works.
- [DE] The stream resolver now prefers AAC-LC (codec `mp4a.40.2`, itag 140/141).
- [DE] Language sync with the mobile app now maps the differing locale codes.

### ✨ Added

- [DE] All text (errors, options, settings, LAN server details, etc.) is now.

## [6.2.1_DE-1.19.3] - 2026-08-15

### 🔧 Changed

- [DE] APK delivery for desktop releases: a dedicated workflow.

## [6.2.1_DE-1.19.2] - 2026-08-15

### 🐛 Fixed

- [DE] Content now scales with the window: the Player artwork resizes with the.

## [6.2.1_DE-1.19.1] - 2026-08-15

### 🔧 Changed

- [APK] Bumped the mobile version to 6.2.1 (versionCode 77).

## [6.2.0_DE-1.19.1] - 2026-08-15

### 🔧 Changed

- [APK] The pairing-code field on Android now opens the numeric keypad and only.

## [6.2.0_DE-1.19.0] - 2026-08-15

### 🔧 Changed

- [DE] The About → Changelog screen now matches the mobile app: a horizontally.

## [6.2.0_DE-1.18.0] - 2026-08-15

### ✨ Added

- [DE] The desktop pairing code now shows a live countdown until it expires.

### 🔧 Changed

- [DE] The desktop Device sync section is now generate-only: it no longer shows.
- [APK] The Android Devices screen is now insert-only: the "Generate code".

## [6.2.0_DE-1.17.1] - 2026-08-15

### 🐛 Fixed

- [DE] Fixed desktop audio not playing: the player reused a stale cache file when.
- [DE] The app now closes itself right after launching an update installer so the.

### 🔧 Changed

- [DE] The About → Changelog screen now fetches `CHANGELOG.md` live from the.

## [6.2.0_DE-1.17.0] - 2026-08-15

### ✨ Added

- [DE] Ported the Home screen to the Android app's style: filter chips, mobile-style.
- [DE] The QR code in Device sync is now always rendered on a solid white card so it.

## [6.2.0_DE-1.16.2] - 2026-08-15

### 🐛 Fixed

- [DE] Fixed the changelog being unclear about desktop versions: releases are.

## [6.2.0_DE-1.16.1] - 2026-08-15

### 🐛 Fixed

- [DE] Fixed the language selector always showing English: the desktop string.

## [6.2.0_DE-1.16.0] - 2026-08-15

### ✨ Added

- [DE] Started the pixel-perfect UI port from the Android app: the theme now.

## [6.2.0_DE-1.15.1] - 2026-08-15

### 🐛 Fixed

- [DE] Fixed the update check picking the wrong release: it now selects the.

## [6.2.0_DE-1.15.0] - 2026-08-15

### ✨ Added

- [DE] Wired device sync end-to-end: the desktop now pushes its playback.
- [APK] Android now pushes its playback to the desktop and applies incoming.

## [6.2.0_DE-1.14.3] - 2026-08-15

### 🐛 Fixed

- [DE] Fixed most text not adapting to the dark/light theme: Material 3's.

## [6.2.0_DE-1.14.2] - 2026-08-15

### 🔧 Changed

- [DE] Debounced the Lyrics screen position updates: the highlighted line is.

## [6.2.0_DE-1.14.1] - 2026-08-15

### 🐛 Fixed

- [DE] Fixed the "Open installer" button not launching the downloaded.

## [6.2.0_DE-1.14.0] - 2026-08-15

### ✨ Added

- [DE] Added optional manual `DATASYNC_ID` / `VISITOR_DATA` fields to the.

## [6.2.0_DE-1.13.0] - 2026-08-15

### ✨ Added

- [DE] Rebuilt the Player screen as a full Material 3 player: a seek slider.

## [6.2.0_DE-1.12.1] - 2026-08-15

### 🐛 Fixed

- [DE] Fixed dark mode not repainting the page background: the app root now.

## [6.2.0_DE-1.12.0] - 2026-08-15

### ✨ Added

- [DE] Added the animated canvas to the Player: a blurred, slowly-zooming.

## [6.2.0_DE-1.11.0] - 2026-08-15

### ✨ Added

- [DE] Added synced lyrics: the Lyrics screen now parses LRC timestamps,.

## [6.2.0_DE-1.10.0] - 2026-08-15

### ✨ Added

- [DE] Added drag-to-reorder to the Queue screen (drag the ⠿ handle), reusing.

## [6.2.0_DE-1.9.0] - 2026-08-15

### ✨ Added

- [DE] Added YouTube login on desktop: paste the music.youtube.com `Cookie`.

## [6.2.0_DE-1.8.0] - 2026-08-14

### ✨ Added

- [DE] Added an Updates section in Settings with in-app downloads: it detects.
- [DE] Added Player & audio settings (autoplay next track) and a Storage.
- [DE] Added a changelog screen (About → Changelog) showing the bundled.

## [6.2.0_DE-1.7.0] - 2026-08-14

### ✨ Added

- [DE] Added a full playback queue: "add to queue" on every song row, "Play.
- [DE] Added a History screen (sidebar) listing the user's listening history.

## [6.2.0_DE-1.6.0] - 2026-08-14

### ✨ Added

- [DE] Added a light/dark/system theme with a selectable accent color palette.

## [6.2.0_DE-1.5.1] - 2026-08-14

### 🐛 Fixed

- [DE] Fixed the desktop player showing "could not resolve the audio stream":

## [6.2.0_DE-1.5.0] - 2026-08-14

### ✨ Added

- [APK] Added LAN discovery to the Android Devices screen: "Find desktop".

## [6.1.0_DE-1.5.0] - 2026-08-14

### ✨ Added

- [DE] Added LAN discovery aids to the desktop Device sync section: a QR code.

## [6.1.0_DE-1.4.0] - 2026-08-14

### ✨ Added

- [DE] Added offline LAN (same Wi-Fi) device pairing: the desktop can start a.

## [6.1.0_DE-1.3.1] - 2026-08-14

### 🔧 Changed

- [DE] The auto-release now attaches the mobile APKs by downloading them from.

## [6.1.0_DE-1.3.0] - 2026-08-14

### ✨ Added

- [DE] Added an Updates section in Settings: an automatic update check on.

## [6.1.0_DE-1.2.1] - 2026-08-14

### 🐛 Fixed

- [DE] Fixed the auto-release workflow's invalid YAML: `continue-on-error` is not.

## [6.1.0_DE-1.2.0] - 2026-08-14

### ✨ Added

- [DE] Integrated self-contained audio playback (Phase 4): AAC stream.

## [6.1.0_DE-1.1.2] - 2026-08-14

### 🔧 Changed

- [DE] Release notes now collapse the commit list into an expandable section.

## [6.1.0_DE-1.1.1] - 2026-08-14

### 🔧 Changed

- [DE] The Android APK build in the auto-release is now optional (per-step.

## [6.1.0_DE-1.1.0] - 2026-08-14

### ✨ Added

- [DE] Full desktop UI: sidebar navigation with Home, Search, Album, Artist,.

## [6.1.0_DE-1.0.4] - 2026-08-14

### 🔧 Changed

- [DE] The auto-release now also builds and attaches the Android APKs.

## [6.1.0_DE-1.0.3] - 2026-08-14

### 🐛 Fixed

- [DE] Fixed desktop device pairing: the desktop client now actually connects.

## [6.1.0_DE-1.0.2] - 2026-08-14

### 🔧 Changed

- [DE] Split the Linux build into independent DEB and AppImage jobs so a.

## [6.1.0_DE-1.0.1] - 2026-08-14

### 🔧 Changed

- [DE] The Windows build now produces both an Inno Setup installer and a.
- [DE] Removed the Inno Setup wizard images that could make the installer open.

## [6.1.0_DE-1.0.0] - 2026-08-14

### ✨ Added

- [APK] Added a "Devices" section in the Android Settings to pair the phone with.

## [6.0.5_DE-1.0.0] - 2026-08-14

### ✨ Added

- [DE] Compose Multiplatform desktop target (`desktop` module) reusing the.
- [DE] Native desktop icons (Windows `.ico`, macOS `.icns`, Linux `.png`) using.
- [DE] Per-OS GitHub Actions builds (MSI/EXE, DEB/AppImage, DMG/PKG) and an.
- [DE] Cross-device sync foundation: shared `sync` module, Node.js WebSocket.

### 🔧 Changed

- [DE] The desktop UI is now English-first with a 49-language picker (first.
- [DE] The Windows installer now performs a machine-wide install into.
- [DE] The Windows installer is now a branded Inno Setup wizard and shows a.

### 🐛 Fixed

- [DE] Made `gradlew` executable in the repository and in the desktop build.
- [DE] Replaced the retired `macos-13` runner with `macos-15-intel` for the.

### 🔒 Security
