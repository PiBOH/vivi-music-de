# TODO — VIVI Music DE (desktop) + Android ↔ Desktop sync

Legend: `[x]` done · `[ ]` to do · `[~]` in progress

What shipped and *why* (with the constraint every fix carries) is in
`CHANGELOG.md`. This file is only what is still open, one line per release for
orientation.

## Open

### Audio (macOS)
- [ ] **The audible jump the macOS reporter still hears (#3).** His last export has *no* stall line and a 99-100 % device check with a 4000 ms ring, so nothing in the pipeline we measure starves: either the PCM leaving the decoder is already discontinuous (the AAC sample-table walk — `audio integrity` only counts *overlaps* today) or the machine froze for over 4 s. **Do not guess:** the next export taken *while the jump happens* has to name it (`jvm stall`, `audio priority stall`, `audio cushion low`, `audio integrity`).

### Audio (Windows)
- [x] **The device was being started under-primed, and that is the audible gap (done 26 Sep, 1.53.28).** The Windows sessions pair `audio output primed: device started with 139ms …` with `audio writer stalled: 1170ms for one pass with only 2ms of it inside the device write (queue 7987ms, cushion 139ms)`: a 139 ms ring drains in 139 ms, so the device went silent with 8 s of PCM queued. The prime now gives up on the 500 ms target only when the queue is empty (nothing left to wait for) or after a 3 s hard bound, not on a 500 ms wall clock. **Watch the next export for `reached the 500ms target` on the normal starts:** that is the line that says the prime is fixed instead of merely bounded.
- [ ] **A scheduling freeze longer than ~1 s is still audible.** Java Sound's device ring is hard-capped at 1000 ms there (measured: any request above 1 s is granted exactly 176400 bytes; macOS grants 4 s) and MMCSS now covers the ~1 s range. Beyond that it means a native WASAPI render path (event-driven, own ring) or a bigger ring in the current backend. **Do not start without a measurement:** the logs must first show the writer frozen *outside* `out.write()`.

### Audio (macOS, #3)
- [ ] **The 1 s cushion is retirable once this is measured, and 1.53.25 is what measures it.** The cushion is a legitimate pre-roll, but its *value* was raised around a writer pass that took 325-556 ms, and a buffer sized around a hiccup hides the hiccup instead of removing it. Every pass is timed from now on (the opening one, the first 30 / first 4 s in full, every pass over 200 ms after that, with `audio writer passes: … none of them over 200ms` in the 10 s device check and per track in `audio integrity`). **The decision rule:** `none of them slow outside the device write` after a few sessions means the slow pass really was the opening one, so the target can come back down to 0.3 s; a count that keeps growing means the thread is being taken off the CPU and the fix is its priority and allocation behaviour (`AudioThreadBoost`, no allocation on that path) — not another cushion. **Do not raise the cushion again without this line**, or the workaround becomes permanent by accident.
- [x] **The 1.53.20 export was read session by session (done 25 Sep).** It contains no playback defect: in its sixteen 1.53.20 sessions, 0 `sample-table overlaps`, 0 `device stalls`, 0 `cushion low`, 0 `starved`, and `audio device check` at 99-100 % on a 4000 ms ring. Its four 20 Sep sessions are leftovers from an older build (they still carry the removed `sample table discontinuity` message) and are the only place a `device stall` appears — every one of them spans a pause. The three `audio writer stalled` lines are the *pre-start* pass of a track start, logged 0-2 ms after their own `audio output primed`; that pass runs before `out.start()` and cannot drain the ring. The analysis and the marker table now live in §10 of `AGENTS.md`.
- [ ] **What the export does prove is start latency, and it is not in the writer.** Press play → first sound is 0.4-1.2 s for a cached track and 2.2-4.1 s over the network, of which `resolving` alone is 1608-2718 ms; the device ring is 99-100 % in every window, so none of it is an underrun. **Next step if the jump is still audible:** a fresh export from a build ≥ 1.53.26 read with the §10 table, and if it is clean again the target moves to the `resolving` stage (one player request per track, no prefetch of the *next* track's stream URL) — not to the output pipeline.
- [ ] **The cushion is now set by measurement, and the per-pass timing of 1.53.25 is what retires it.** All 30 starts in the 1.53.20 export were primed with 139 ms, 278 ms or 417 ms against a 1000 ms target, because a 400 ms wall-clock plateau (removed in 1.53.26) inferred "the ring is full" from "the producer went quiet" — a `SourceDataLine` that has never been started feeds the writer in bursts. A short write is now the only thing that concludes the ring is full. **The decision rule stands:** `audio writer passes: … none of them slow outside the device write` after a few sessions means the target can come back down to 0.3 s; a count that keeps growing means the thread is being taken off the CPU and the fix is its priority and allocation behaviour (`AudioThreadBoost`, no allocation on that path) — not another cushion. **Do not raise the cushion again without this line.**

### Performance (Windows)
- [ ] **Playing still costs ~0.5 core** on the reporting machine (`AWT-EventQueue-0` ≈ 3.1 s CPU in 6 s, Skiko OpenGL redrawer vs `dwmFlush`). The per-tick recomposition went in 1.50.71; what is left is the renderer itself (candidates: software/ANGLE backend, lower animation rate) (#3).
- [ ] **The setup is ~129 MB and cannot go below ~100 MB** while the sign-in WebView ships: `javafx-web` ~31 MB, `icudtl.dat` 10 MB, `skiko` 12 MB, the 78 MB jlink runtime. Levers left: subset the four bundled fonts, shrink the jlink image.

### Lyrics
- [ ] **An "exact video" timed source for the tracks the community servers do not index.** The captions live in the player response (`captions.playerCaptionsTracklistRenderer.captionTracks[].baseUrl`), which needs a model in the shared `innertube` module (#80). Mobile's `YouTubeSubtitle` was removed in 1.50.70 because the endpoint did not answer.

### Translations
- [ ] **Azerbaijani: the desktop is clean, the phone's long paragraphs are not.** Every string the desktop shows is proper Azerbaijani now (the batch 83 pass), but `app/src/main/res/values-az/vivi_strings.xml` still holds ~250 word-by-word ones like "göstər sıxlıq dəyiş will take effekt sonra restarting tətbiq. Do siz want -a yenidən başlat now?" — multi-sentence descriptions that need a native Azerbaijani pass, not a guess from here. They only reach the desktop through the keys it maps to an Android resource, and those were fixed in the extras batch; on the phone they show as-is. The generator's no-op guard already stops such a string from silently *becoming* the desktop wording, so the remaining work is visible instead of hidden.

### UI parity (mobile → desktop)
- [x] **Album / artist / playlist context menus (done 26 Sep, 1.53.29).** The header now carries the "⋮" the song rows already had: play next, add the whole collection to the queue, go to the artist, like the album/playlist (`YouTube.likePlaylist`) or subscribe to the artist (`YouTube.subscribeChannel`), share and refresh. The like/subscribe label is session-local — the desktop has no album/artist row to read it from — so it is optimistic and resets on restart.
- [x] **Gradient header on Album / Artist / Playlist (done 26 Sep, 1.53.29).** The plain row became the artwork behind a scrim that fades into the page background, the mobile shape. It is the same `CachedBlurBackdrop` the rest of the app uses, so the decode is cached per URL.
- [x] **Swipe / canvas thumbnails (done 26 Sep, 1.53.29).** Swipe-to-change-song was already on the full player artwork and the mini players; what was missing was the *canvas* thumbnail: the canvas only ever fed the player background, so the mobile "Use canvas" description ("replaces the static album artwork") was not true. The resolved canvas now replaces the artwork (classic/new and expressive), with the rotating artwork still winning when it is on.
- [x] **The notification-permission row (done 26 Sep, 1.53.29).** A row on the Notifications screen that asks macOS for the permission (the only desktop platform with one) and posts a notification right after, so the prompt appears and its result is visible.
- [ ] **Spotify import.** The mobile screen logs in through a WebView that captures the `sp_dc` cookie, then uses the `:spotify` module to list playlists/likes and match every track to YouTube. The desktop has the `:spotify` module and a JavaFX login window for YouTube already, so this is the same shape — WebView cookie capture, `SpotifyAuth.fetchAccessToken`, `SpotifyMapper.matchScore` against `YouTube.search`, then the matched tracks into a local playlist — but it is a whole feature and was **not** started in 1.53.29.
- [ ] **JioSaavn as a desktop source.** Mobile has the enable/quality screen (`EnableSaavnStreamingKey`, `SaavnAudioQuality`) and a `:jiosaavn` module the desktop already compiles against, but the desktop has no stream path that uses it and no settings at all. Not started in 1.53.29.

### Infra
- [ ] **`vivi-music-de-apk` still carries the desktop sources and stale leftovers** (`.websitede/`, `NewUI_desktop.zip`, `Changelog`, `News`, the superseded workflows) next to the Android app; it should hold only the app and the modules it compiles against.
- [ ] `WINDOWS_SIGNING_CERT` + `WINDOWS_SIGNING_PASSWORD` secrets, to sign the Windows installer.
- [ ] **Windows media flyout (SMTC):** needs WinRT COM interop (`ISystemMediaTransportControlsInterop::GetForWindow` + hwnd) that cannot be validated without a Windows machine — deferred, do not ship blind.
- [x] **OBS / screen capture:** window capture works in both chrome modes (verified with BitBlt); waiting for the exact symptom (window missing from the list vs black preview) before documenting the WGC method.
- [ ] **End-to-end encryption (Phase 7):** per-pair key exchanged at pairing, snapshots encrypted before the relay sees them.

### Mobile (APK) — the 6.0.8.1 update

- [x] **The base is upstream `v6.0.6`, and this was measured, not guessed.**
  `git diff --shortstat` between our `app/` and the upstream tags gives
  `up606` → 92 files / 3346 insertions (that is *our* diff), `up607` → 219
  files, `up608` → 226 files. So the fork point is the tag `v6.0.6` and our
  mobile version `6.0.6.8` is PiBOH's own counter on top of it. That is what
  turns the update into a diff to re-apply instead of a source merge.
- [x] **Imported on 25 Sep — 6.0.8, with our identity untouched.** The APK is
  built from **`vivi-music-de` itself** (`Auto Release` dispatches
  `build-android.yml --ref "$GITHUB_REF_NAME"`), so the mobile code that ships
  is this branch's `app/`. Upstream 6.0.8 went in over `app innertube jiosaavn
  lyricsProvider gradle/libs.versions.toml`, our own app diff was re-applied
  3-way on top, and it stopped on **17 conflicts across ten files** — the
  measured eight (`app/build.gradle.kts`, `MainActivity`, `PreferenceKeys`,
  `MusicService`, `Lyrics_v2`, `UpdateSettings`, `YTPlayerUtils`,
  `vivimusicupdater`) plus `values/strings.xml` and
  `values-fr`/`values-sv/vivi_strings.xml`. Every one was resolved by keeping
  both sides; `app/build.gradle.kts` then took our configuration back by hand:
  `applicationId = com.vivi.music.desktop`, the `-Pchannel` → `RELEASE_CHANNEL`
  block, the `apk-latest` updater, `versionCode 139`, `versionName 6.0.8.1`.
- [x] **The three shared modules could not become Android libraries.** 6.0.8
  turns `:innertube`, `:lyricsProvider` and `:jiosaavn` into
  `com.android.library` modules (`android.util.Base64`/`Log`, a `Context` in
  `Paxsenix.init`, the Android-only `timber` AAR) and a JVM target cannot
  consume an AAR — Gradle refused to configure `:lyricsProvider` at all
  (`No matching variant of com.jakewharton.timber:timber:5.0.1`). They keep
  `kotlin("jvm")` and the three Android call sites are platform-neutral again
  (`java.util.Base64`, the `Log` and `Timber` shims the modules already
  carried, `Paxsenix.init(BuildConfig.VERSION_NAME)`), so one copy of each
  serves both the APK and the desktop. Our `YouTubeExtractor` stays in
  `:innertube` for `StreamResolver` (`libs.rhino` came back with it);
  upstream's app uses the Android-only `:innertubex`, which `:app` now
  depends on directly.
- [x] **Our diffs outside `app/` were re-applied too.** The `LibraryPage`
  `shape` field, the tolerant artist rows, the JioSaavn v4 map response — all
  of it lives in `innertube`/`jiosaavn`/`lyricsProvider`, and a `checkout` of
  upstream's copy drops it silently (the desktop then fails to compile on
  `page?.shape` alone). `git diff up606 HEAD -- innertube jiosaavn
  lyricsProvider` was re-applied 3-way on top of 6.0.8 and applied cleanly;
  what 6.0.8 removed from `libs.versions.toml` and our modules still need came
  back (coroutines, ktor, `zxing`, `compose-multiplatform`, `okhttp`,
  `jmdns`), and the leftovers it no longer has were deleted (the
  `viviwrapped_v1/v2` PNGs, which 6.0.8 ships as `.jpg`, and the 45 strings
  written twice into `values-sv/vivi_strings.xml`).
- [ ] **The next import repeats this shape:** tag the upstream release,
  `git diff` our `app/` against the *previous* tag so the patch stays small,
  re-apply 3-way, keep both sides at every conflict, **re-apply our diffs
  for the shared modules too**, re-add what upstream dropped from
  `libs.versions.toml`, and do not commit before both
  `:app:compileUniversalGmsDebugKotlin` and `:desktop:compileKotlin` are
  green — the release dispatches the
  Android build from this branch, and scratch branches are banned (AGENTS.md).

## Done — one line per release

- [x] **DE 1.53.29** — `super_logs_writer` (the per-pass writer diagnostics are off by default, so `playback.log` is not filled at pass frequency), the Album/Artist/Playlist gradient headers and context menus, the canvas actually replacing the player artwork (mobile "Use canvas"), and the notification-permission row
- [x] **DE 1.53.27** — the writer verdict no longer counts the sound card's own pacing as a stall (a pass is a stall only when it is slow *outside* `out.write()`; on Windows four passes a second are legitimately over 200 ms), and the mobile base is upstream 6.0.8 with our `applicationId`, channel and updater kept
- [x] **DE 1.53.26** — the device is primed from a measurement (`out.write()` keeping only part of the block) instead of a 400 ms wall-clock plateau, so all 30 starts of the 1.53.20 export can no longer be primed at 139/278/417 ms against a 1000 ms target
- [x] **DE 1.53.25** — every writer pass is timed and reported (the opening pass, the passes in the first 30/4 s, every slow one after that, plus a per-window and per-track verdict), so an export can say whether the writer stall that forced the 1 s cushion repeats or was only the opening pass
- [x] **DE 1.53.24** — the translations as one file per language (the APK's `values-<lang>/strings.xml` shape, proven identical to the old tables language by language), the history row without a track length, the artists tab saying its list can take ten seconds
- [x] **DE 1.53.23** — the sidebar logo and the tray icon (an all-transparent vector render, so the tray never appeared), the two expressive finishes as entries of the player picker, a YouTube Music playlist that already exists is updated instead of uploaded again, the lyrics cache bumped to v7 for the styled renderer
- [x] **DE 1.53.22** — the device is primed with a real cushion instead of 0.3 s (#3), the macOS tile artwork follows the track instead of sticking to the first song (#63), and two diagnostics that misled this investigation are fixed
- [x] **DE 1.53.21** — the swipe switch and sensitivity slider, the "no text" dots starting too early (mobile's rule, no estimate), the frozen now-playing bars, the translucent-tab player variant, the VIVI mark as a vector, the translation audit (bare "listening" in 49 languages, English and half-English strings)
- [x] **DE 1.53.20** — macOS: the "Now Playing" tile is claimed at launch with a restored (paused) queue (#63); the lyrics "no text" marker is three horizontal dots instead of the ring
- [x] **DE 1.53.19** — the expressive player's lyrics-menu crash, the queue's "+" moved into the ⋮ menu, sharp tray/sidebar logos, the "no text" indicator on plain LRC, the now-playing bars on the cover, the Library live refresh, the Artists list, the duplicates left in the sidebar, the position after a skip
- [x] **DE 1.53.18** — the liked songs travel between phone and desktop (#96); the playlist copies a pairing left behind are collapsed and no second account copy is created (#93)
- [x] **DE 1.53.17** — seek bar drag and snap-back, the `-0:00` countdown, the playlist duplication (E1034), the expressive player's tab + autoplay, the rail toggled by the title
- [x] **DE 1.53.16** — the lyrics "no text" indicator, aligned-line overflow, artwork quality, playlist-sync progress, the whole queue in the device sync, the player picker, the font import, the pairing scan (APK)
- [x] **DE 1.53.15** — full translation audit: no key can leak another language
- [x] **DE 1.53.14** — a playlist can live on YouTube Music too (create, rename, delete propagation)
- [x] **DE 1.53.13 … 1.53.9** — the notifications empty state, the Artists screen, one distinct style per lyrics style, the sidebar/sign-in/transport fixes, the strings of 1.53.6-1.53.8, the UI idling while paused
- [x] **DE 1.53.8 … 1.53.0** — the mobile Content screen and the lyrics controls, the account's playlists mirrored locally, the local history, the mobile settings layout
- [x] **DE 1.52.x** — the audio diagnosis chain (MMCSS, the Skiko `System.gc()`, the ring/gap checks), the `.rpm` + AUR packages, the updater integrity, `settings.json`, the `apk-latest` download path, the website on `gh-pages`
- [x] **DE 1.51.x** — animated lyrics, romanization and AI translation
- [x] **≤ DE 1.50.76** — the desktop foundation: audio playback, persistence, authentication, the UI port, the in-app updater, the installer trimming
- [x] **APK 6.0.6.x** — the companion build (`com.vivi.music.desktop`, "VIVI for DE") and its two-way device sync with the desktop
