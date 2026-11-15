# Bit UI parity screenshots

Every PNG here is a real window of the packaged `VIVIMusic.exe` (the jpackage
image in `desktop/build/compose/binaries/main/app/VIVIMusic`), captured with
`agent-desktop screenshot --app VIVIMusic.exe`. Nothing is a mockup or a
hand-drawn frame.

## Side-by-side with the reference

`side/NN-name.png` pairs the running BitChord 1.8-beta1 window (left) with VIVI
Music DE (right) at the same height. Both windows are 1220 px wide; ours is
1220 x 803 because the capture includes the OS title bar, the reference is
1220 x 780 with its own custom bar.

| File | Screen |
|---|---|
| `side/01-home.png` | Home shelves |
| `side/02-explore.png` | Explore mood grid |
| `side/03-library.png` | Library |
| `side/04-search.png` | Search |
| `side/05-history.png` | History |
| `side/06-downloads.png` | Downloads |
| `side/07-local-music.png` | Local Music |
| `side/08-settings.png` | Settings |
| `side/09-player.png` | Full-window player (reference side is the owner's expectation image: the reference app exits its player screen under software rendering on this GPU) |

What the pairs changed in the code, all measured on the reference window rather
than estimated:

- Chrome `#171717` became `#0F0F11` (the reference's rail and transport bar are
  darker than the page), the selected rail row `#333` became `#2C2C2D`, and the
  search field went to `#1F1F21` with a `#2A2A2C` border.
- The rail is 208 dp, not 220, with the content inset going from 16 to 28 px so
  the type keeps the reference's absolute x (236 px at this width).
- Covers are 150 dp with a 15 dp gap (was 168/18), card titles 16/600 and the
  meta line 13 px, 12 dp under a cover, 44 dp between shelves.
- Home, Explore, Library, Search and History lost their 30 px page titles: the
  reference starts those pages at the first shelf, string or field. Settings
  keeps a title, at 22 px.
- Explore became the reference's single grid of 150 dp mood tiles, four to a
  row, with the label in the tile and no "New releases" shelf.
- The shelves lost their "Show all" link, which the reference does not draw.

**Closed in the pairs on 15 Nov 2026 (second pass).** The five gaps the first
pass left open are drawn now, with the numbers measured on the reference window
at 1220 x 780:

- **Library cards** are 300 x 189 px, 12 px apart, 16 px inset, in a row that
  scrolls and leaves the fourth card cut at the right edge (the reference draws
  four). The tint is taken from the artwork (`BitTint.kt`), so the card is a
  vertical gradient from rgb(72, 15, 65) to rgb(45, 12, 42) for a purple cover
  instead of a fixed grey; the figure is 28 px for a number and 21 px for a name.
- **Library "Show all"** sits on the right of the Playlist shelf title and opens
  the first playlist of the row.
- **Explore tiles** are 233 x 133 px with 12 px gaps. The stripe colour fills the
  tile, the label sits on it 14 px from the top left, and the artwork takes the
  right 72 percent with no overlay of any kind: the reference's photographs are
  bare, and its labels are read against the colour strip, so the 55 percent black
  scrim is gone.
- **Settings** is the reference's spacing: a centred 800 px column whose text
  starts at 340 px, 12 px above the title, 8 px under it, 10 px over each group
  label and 6 px under its rows, which reproduces the reference's 100 px pitch
  between group labels. The detail line caps at 630 px, so "Add a source" wraps
  on two lines exactly as the reference's does. The four groups this build really
  has (MISCELLANEOUS, LANGUAGE, ADVANCED OPTIONS, PLAYBACK) stay below AUDIO
  QUALITY, redrawn in the reference's row language (64 px row, 34 px icon tile,
  13 px detail, switch or chevron); that one divergence is deliberate and
  documented in `AGENTS.md`.
- **Verification without elevation**: every screenshot in this pass comes from
  the packaged image `desktop/build/compose/binaries/main/app/VIVIMusic/VIVIMusic.exe`,
  which is the payload the installer ships, so nothing depends on an
  administrator prompt.

Still not identical: the reference resolves artwork for only some of its moods,
so many of its tiles are flat colour, while this build resolves a playlist cover
for every mood and therefore shows photographs on nearly all of them; the
Library's first and second cards share the same cover and so the same tint,
where the reference tints each card from its own subject.

**Which build each pair comes from.** All six refreshed pairs
(`02-explore`, `03-library`, `04-search`, `06-downloads`, `07-local-music`,
`08-settings`) are captures of the packaged image above on 15 Nov, taken with
`VIVIMusic.exe --open=<section>` and `agent-desktop screenshot`. `01-home`,
`05-history` and `09-player` are from the 12:34 build of the same day and were
not part of this pass.

| File | Surface |
|---|---|
| `01-home.png` | Home, shelves and the transport strip |
| `02-explore.png` | Explore, five-column release grid |
| `03-library.png` | Library, downloads and on-device playlists |
| `04-search.png` | Search, empty state and filter chips |
| `05-history.png` | History rows |
| `06-downloads.png` | Downloads, filter chips and empty state |
| `07-local-music.png` | Local Music, filter chips and empty state |
| `08-settings.png` | Settings page |
| `09-album.png` | Album detail |
| `10-playlist.png` | Local playlist detail |
| `14-artist.png` | Artist detail |
| `16-lyrics.png` | Lyrics side panel |
| `19-search-results.png` | Search with results |
| `17-now-playing.png` | Full-window player (Apple Player v17) |
| `18-mini-player.png` | Mini player at the bottom, compact layout |
| `15-queue.png` | Queue side panel |
| `20-youtube-playlist.png` | A YouTube playlist opened from a Home card |
| `21-album-artist-link.png` | Artist page opened from the album header's artist credit |
| `22-menu-album.png` | The album collection menu (Play next, Add to queue, Save to library, Download) |
| `23-menu-song.png` | The song row menu (Play, Play next, Add to queue, Add to playlist, Download, Share) |
| `24-menu-playlist.png` | The playlist collection menu (Play next, Add to queue, Save to library, Download, Share) |
| `25-search-results.png` | A real query ("daft punk") returning YouTube Music songs |
| `26-search-playing.png` | The same list after clicking the first row: the track plays and the mini player shows it |
| `27-search-enter.png` | Enter submitted from the rail field on the rebuilt 1.56.0-beta, results returned |
| `28-installed-search.png` | The same submit on the installed copy in `C:\Program Files\VIVIMusic` |

The screens were reached the way a person reaches them: the rail rows for the
pages, a card for a release, a sidebar playlist for a local playlist, the
transport icons for the panels, the pill for the player, and the player's own
queue button for the queue. The window was resized to 940 px for the compact
shot so the rail folds away and the bottom bar is the only transport.

The queue shot comes from the player's own queue button, which replaces the
control column with `Queue / Now playing / Up next`. The artist shot comes from
Search, the `Artists` filter and the first row.

The album shot is reached from the Home shelf, the artist shot by clicking the
red artist credit in that album's header (the browse id comes from the album's
song credits when the header run has none), and the menu shot by clicking the
`...` beside Play. The window is decorated by the system title bar, so a click
at a screenshot coordinate has to be converted to client coordinates
(`ClientToScreen`, the title bar is about 30 px); a plain `+7` offset lands in
the wrong row and made earlier attempts look like dead buttons.

The full-window player was measured with the passive frame pacing probe
(`-Dvivi.frameProbe`): 60.0 fps average, p95 gap 17.3 ms, 0 dropped frames over
20 s on a 60 Hz display with vsync on, and 108 fps average with vsync unlocked,
which is how the app ships. Home measured 59.9 fps and Explore 59.3 fps, both
with 0 dropped frames apart from the cover decodes on entry.

Searching also exposed a real defect: a `NullPointerException` in
`BcSearchPage` when the results state was cleared while the lazy list was being
invalidated (`results!!`). The list now reads through the nullable state, which
is why these shots come from a build that no longer crashes on a second search.

A second search defect only appears with a keyboard: `KeyboardActions(onSearch)`
is the soft-keyboard IME action, and a desktop sends a plain Enter key instead,
so the query sat in the field and nothing ran until a filter chip was clicked.
`Modifier.bcSubmitOnEnter` (`BitTheme.kt`) now runs the submit on `Key.Enter`
and `Key.NumPadEnter`, and both search fields (rail and page) use it. The two
shots above are the proof of the whole path: the rail field submits, the page
runs the query against YouTube Music, the rows carry the covers, the artist line
and the duration, and clicking the first row started `Instant Crush`
(`playback.log`: "stream ready for 'Instant Crush'").

The playlist menu was exercised on the installed build, not only photographed:
`Play next` on *Presenting Daft Punk* wrote to the queue
(`queue.log` 11:19:41, "built the queue for 'Technologic' - 15 up-next tracks
appended"). `Download` on a song row reaches `PlayerController.downloadToCache`
(`app.log` "[cache] auto download on like"), and `Save to library` writes
`~/.vivimusic/playlists.json`.

The fixed build was rebuilt as
`dist/VIVIMusic-6.0.8.11_DE-1.56.0-setup.exe` and installed over the existing
copy. The parity pass rebuilt it again (150,837,066 bytes, 15 Nov 12:34) and
installed it at 12:36; the installed `app/VIVIMusic.cfg` points at
`desktop-2d4ab622...jar`, the jar that build produced. On the earlier 11:25 build
the installed launcher's `app/VIVIMusic.cfg` pointed at `desktop-4b92beaf...jar`
(11:24); the older `desktop-*.jar` files from earlier installs stay in
the folder, unused, because Inno Setup does not remove files it no longer
ships. The submit was then repeated on that installed exe (`28-installed-search.png`)
and the player measured there with the same probe: 59.9 fps average, p95 gap
17.3 ms, 0 dropped frames, max gap 19.2 ms over 20 s while a track played, with
the window on Direct3D. The 12:34 build repeats it on the installed copy with
the player open and a track playing: 59.9 fps, p95 gap 16.9 ms, 1 dropped frame,
max gap 33.2 ms over 20 s, which is the same 60 Hz ceiling with a single stutter
during the capture. A window that includes the player opening reports 58.5
fps and a 460 ms first gap, which is the transition, not playback.

Driving the packaged app from a script needs two things that cost an hour here:
the window has to own the foreground before a synthetic click, or Windows gives
the click to the activation and the widget never sees it (`.ignore/tmp/click7.ps1`
attaches the input queue and checks `GetForegroundWindow` first), and text has to
arrive as real keys (`agent-desktop press --app VIVIMusic.exe --headed <key>`):
`SendKeys` from a background PowerShell reaches nothing. A window that keeps
losing the foreground to the shell needs the focus attempt repeated until
`GetForegroundWindow` really returns it (`.ignore/tmp/click7.ps1` loops eight
times); one attempt is not enough, and the click that follows a failed one is
spent on the activation. A deep link starts the app on a screen without any
click at all: `VIVIMusic.exe --open=search` (also `player`, `queue`, `library`,
`settings` and the settings pages, `Main.kt` `screenForSection`), which is why
`docs/ui-parity/27-search-enter.png` was reached that way.

The final 13:43 build was measured the same way on the installed copy, player
open and a track playing, window on Direct3D:
`frames=1200 span_s=20.0 avg_fps=60.0 p95_gap_ms=16.9 p95_dropped=0
max_gap_ms=18.3` (the probe's own line: `frames=1200 span_s=20.0 avg_fps=60.0
p95_gap_ms=16.9 p95_draw_ms=0.0 dropped=0 max_gap_ms=18.3 budget_ms=16.7`).
