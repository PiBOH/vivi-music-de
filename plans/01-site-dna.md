# Site DNA - BitChord desktop 1.8-beta1

AUDIT_MODE: high-fidelity

Reference: local BitChord 1.8-beta1 source and Windows build.

- Source: `.ignore/BitChord-source`, commit `7c673dc`, GPL-3.0.
- Installed build: `.ignore/BitChord/BitChord-1.8-beta1-windows-x64-portable`.
- Measured window: 1220 x 780, PrintWindow capture.
- Token source: `.ignore/BitChord/STYLE-GUIDE.md`, read from the running window and BitChord's `Theme.kt`.
- Screen source: `desktopApp` and `sharedUi`, plus the reference images under `.ignore/BitChord/vivimusic vs bitchord`.
- Screenshots cover the main pages. Hover, focus and transient states that the images do not show are marked `capture` and must be confirmed against the running build before the iterator closes the screen.

This audit adapts the SRIP website procedure to a Compose Desktop app. The page is the window, sections are the shell and routed screens, and CSS values are Compose dp/sp values.

## 1.1 - Window architecture

BitChord uses a three-zone shell. The left rail runs the full window height. The content column starts at its right edge. A 64 dp player strip crosses the content column, not the rail. A right panel can narrow the page by 360 dp while the page stays usable.

```
BITCHORD SHELL - 1220 x 780
total width 1220 | sidebar 220 | content 1000 | right panel 0 or 360
┌──────────────────────┬─────────────────────────────────────────────────────┐
│ SIDEBAR 220          │ PLAYER STRIP 64                                     │
│ bg #171717           │ bg #171717, 1 px #121213 bottom                     │
│ 1 px rule at right   │ logo, back, shuffle, previous, play, next, repeat,  │
│                      │ artwork 40, title + artist, volume, output, lyrics, │
│ search 36            │ queue, more, avatar                                 │
│ Home                 ├─────────────────────────────────────────────────────┤
│ Explore              │ PAGE #202020                                        │
│ Library              │ 16 dp left inset, 18 dp right inset                 │
│ Search               │ page header when the route has one                  │
│ History              │                                                     │
│ Downloads            │  shelves, grids, tables, queue or lyrics            │
│ Local Music          │                                                     │
│ playlists            │                                                     │
│ Account / Settings   │                                                     │
└──────────────────────┴─────────────────────────────────────────────────────┘
```

The sidebar and player strip use the same chrome colour, so the top of the window reads as one continuous bar. A 1 px line separates the rail from the page and the player strip from the body. The page has no outer card and no page shadow.

Required VIVI adaptation:

- Keep the BitChord rail width, order, spacing, search field and playlist list.
- Replace the bottom Settings row with a pill split control: `Account | Settings`. The left half shows Guest or the signed-in name and opens Account. The right half opens Settings.
- Move the now-playing strip from the top of the content column to the bottom mini-player, as requested. The strip keeps BitChord's 64 dp height, transport order, 40 dp cover, title stack and utility cluster.
- Keep the right panel structure and its 360 dp width. The page narrows; it is not covered.
- Keep VIVI's window chrome and Settings screens. Do not copy BitChord's light dialog surface `#ECE6F0` into the dark shell.

Overlap: the player strip shares the content column's x-axis. The right panel starts at the content column's right edge and runs under the player strip when open. The sidebar never sits under either.

## 1.2 - Design tokens

```
PALETTE
  Chrome / rail / player strip       #171717
  Page background                    #202020
  Divider, rail edge, strip edge     #121213
  Card fill                          #0C0C0C
  Card border                        #141414
  Search field                       #1D1D1D
  Search border                      #242424
  Selected or hovered rail row       #242424
  Secondary text                     #8E8E93
  Primary text and primary action    #FFFFFF
  Replay rank accent                 #FA2D48
  Dark surface role                  #0D0D0F
  Surface variant                    #1C1C1E
  Outline                            #2C2C2E
```

The red is not the interface accent. It appears on the Replay rank only. White is the primary action colour. VIVI's user accent remains available where the existing theme owns it, but the cloned BitChord chrome uses the grey ladder above.

```
TYPOGRAPHY - source scale, SF Pro Display
  Role            Family          Weight  Size  Tracking  Notes
  displayLarge    SF Pro Display  W800    34    -0.5      page-scale display
  headlineLarge   SF Pro Display  W800    30    -0.4
  headlineMedium  SF Pro Display  W700    22    -0.25     shelf titles
  titleLarge      SF Pro Display  W700    20    -0.2
  titleMedium     SF Pro Display  W600    16     0.05
  bodyLarge       SF Pro Display  W400    16     0.2
  bodyMedium      SF Pro Display  W400    14     0.3
  labelMedium     SF Pro Display  W600    12     0.25
  labelSmall      SF Pro Display  W600    11     0.3
```

BitChord draws each weight one cut lighter than the requested weight. Do not bundle the SF Pro files: the licence does not allow redistribution. Use the current VIVI font or Inter, and preserve the size, tracking and weight ratios. The tight negative tracking above 20 sp and the opening below 16 sp are the typographic signature.

```
SPACING
  Base unit              8 dp
  Rail width             220 dp measured 219 px
  Rail inner inset        12 dp
  Rail search height      36 dp
  Rail row vertical pad   7 dp, 18 dp icon, 10 dp gap, 7 dp corner
  Content left inset      16 dp from the 1 px rule
  Content right inset     18 dp
  Cover                  168 x 168 dp
  Cover gap               18 dp
  Cover pitch            186 dp
  Player strip height     64 dp
  Side panel width       360 dp

RADIUS
  Rail row                7 dp
  Card / artwork          8 dp
  Menu surface           12 dp
  Play control            full circle or pill
  Search field            rounded rectangle, 8 dp

SHADOW
  None on the shell. Structure comes from 1 px rules and the grey ladder.
  Floating menus use the existing VIVI popup elevation, not a hard drop shadow.

TEXTURE
  No noise. Album artwork and its palette supply the only colour.
```

## 1.3 - Screen blueprints

### 1.3.1 - Sidebar

Height: full window | BG: `#171717` | Right rule: 1 px `#121213`
Width: 220 dp, measured 219 px.

```
┌──────────────────────────────┐
│ 34 dp caption / window drag  │
│ ┌──────────────────────────┐ │
│ │ Search                   │ │ 36 dp
│ └──────────────────────────┘ │
│  Home                        │ 36 dp rows
│  Explore                     │
│  Library                     │
│  Search                      │
│  ──────────────────────────  │
│  History                     │
│  Downloads                   │
│  Local Music                 │
│  ──────────────────────────  │
│  playlist rows               │ flexible list
│  ...                         │
│  ──────────────────────────  │
│  Account | Settings          │ VIVI split pill
└──────────────────────────────┘
```

Content map:

- Search field: 36 dp, fill `#1D1D1D`, 1 px `#242424`, placeholder "Search".
- Main rows: 36 dp pitch, icon 18 dp, label 14 sp, secondary `#8E8E93`.
- Selected row: white 12 percent fill, white label, accent icon, 7 dp corner. Hover uses `#242424`.
- Dividers: 1 px `#121213`, with 6 dp above and 12 dp below.
- Playlist rows use a playlist glyph, not the album cover.
- Bottom control: Account half shows `Guest` or the account name, Settings half is the existing VIVI Settings entry. The pill has one outline and an internal 1 px divider.

States: default, hover, selected, pressed, disabled while a party session locks controls. Long playlist names ellipsize and expose a tooltip.

### 1.3.2 - Home

Height: fills the page under the player strip | BG: `#202020`
No page heading on desktop. Content starts 16 dp from the rail rule and 18 dp from the right edge.

```
┌─────────────────────────────────────────────────────┐
│  Recents / greeting line, optional                  │
│  ┌──────────┐ ┌──────────┐ ┌──────────┐ ┌────────┐  │
│  │ 168x168  │ │ 168x168  │ │ 168x168  │ │ 168... │  │
│  └──────────┘ └──────────┘ └──────────┘ └────────┘  │
│  22 sp shelf title                    Show all      │
│  horizontal shelf, 18 dp gap, 186 dp pitch          │
│  ...                                                │
└─────────────────────────────────────────────────────┘
```

Home is a vertical feed of horizontal shelves. Each shelf has a 22 sp bold title, an optional "Show all" text action on the right, and square artwork cards. The desktop suppresses the phone's large hero treatment; all shelves use the ordinary row. Cards use an 8 dp corner, a bottom scrim and the title over the lower edge. A card opens its album, playlist, artist or track. A right-click or long press opens the matching context menu.

States: loading skeleton rows, local-only empty state, signed-out sign-in banner, error with Retry, pulling-to-refresh on touch, hover lift, playing accent.

### 1.3.3 - Explore

BG: `#202020`. No large title on desktop.

```
┌─────────────────────────────────────────────────────┐
│  Mood & genres                                      │
│  ┌────────────┐ ┌────────────┐ ┌────────────┐       │
│  │ artwork    │ │ artwork    │ │ artwork    │       │
│  │ title      │ │ title      │ │ title      │       │
│  └────────────┘ └────────────┘ └────────────┘       │
│  more mood rows                                     │
└─────────────────────────────────────────────────────┘
```

Moods and genres form a responsive grid of artwork tiles. The grid's column count follows the available width. The source groups the server's sections into one visual grid. A tile opens the mood or genre page. Loading uses shimmer tiles; error offers Retry.

### 1.3.4 - Library

BG: `#202020`.

```
┌─────────────────────────────────────────────────────┐
│  Replay summary cards, 4 across, artwork-tinted       │
│  ┌────────────┐ ┌────────────┐ ┌────────────┐        │
│  │ 63         │ │ Top artist │ │ Top song   │  ...   │
│  └────────────┘ └────────────┘ └────────────┘        │
│  Downloads                              >            │
│  Local Music                            >            │
│  On device                                          │
│  ┌────────┐                                          │
│  │ cover  │  playlist title / count                   │
│  └────────┘                                          │
│  Playlist                             Show all       │
│  shelf...                                            │
└─────────────────────────────────────────────────────┘
```

The page leads with Replay summary cards, then the device links, then the on-device shelf, then the account's saved collections. The source intentionally omits separate Liked Music and Songs track runs; liked music is reached as a playlist. Replay cards use artwork-driven gradient fills and large white numbers.

States: signed-out view with sign-in banner, empty library, loading skeleton, error, pull-to-refresh, Sort library menu for Show all.

### 1.3.5 - Search

BG: `#202020`. The sidebar search field is the only query input; the page shows suggestions and results.

```
┌─────────────────────────────────────────────────────┐
│  suggestion row   "banana joe bud spencer"      ⏎   │
│  suggestion row   "banana joe sigla"             ⏎   │
│  ───────────────────────────────────────────────    │
│  result row: cover 56, title, subtitle, menu        │
│  result row: cover 56, title, subtitle, menu        │
└─────────────────────────────────────────────────────┘
```

Empty state: recent search entities with artwork and type. Typing: debounced suggestions, 220 ms, with a fill-query action. Submitted: results grouped by songs, albums, artists and playlists, with result counts. A result opens its page or starts playback. The menu exposes the same song actions as everywhere else.

States: idle, typing, suggestions, searching, results, empty, error. Keyboard: Enter submits, Up/Down moves through suggestions, Escape clears focus.

### 1.3.6 - History

BG: `#202020`. Page title "History" at the top, with a Clear action when entries exist. A table of cover 48, title, artist, album, duration and `...`. Rows play on click and open the song menu. Empty state has a short explanation and no fake rows.

### 1.3.7 - Downloads

BG: `#202020`. Page title "Downloads". A queue section shows active, queued and failed downloads with progress. A saved section lists completed tracks with size and `...` actions. Clear completed and open-folder actions sit in the header. Empty state explains that downloads are saved locally.

### 1.3.8 - Local Music

BG: `#202020`. Page title "Local Music", subtitle "Audio files found in Music and Downloads". Folder source rows, scan status, track count, and a table of local tracks. A rescan action is available. Local tracks have no YouTube rating or share link, so those menu rows are omitted.

### 1.3.9 - Playlist

BG: `#202020`.

```
┌─────────────────────────────────────────────────────┐
│ ┌────────┐  PLAYLIST                                │
│ │ 2x2    │  Playlist name                           │
│ │ mosaic │  12 tracks                                │
│ └────────┘  (shuffle) (play) (...)                  │
│                                                     │
│  #   cover  title / artist                  duration │
│  1   ...                                            │
└─────────────────────────────────────────────────────┘
```

The header uses a 2x2 mosaic from the first four real covers, a small uppercase PLAYLIST label, the name in display size, the true track count and a row of round controls. The table is the real stored order. Row click plays; heart writes the same like as the rest of the app; `...` opens rename, delete and the shared actions. A missing playlist shows a plain not-found state.

### 1.3.10 - Album and Artist

Album: large cover, title, artist, year, track count, Play/Shuffle, `...`, then the real track table. Background may take a low-contrast tint from the cover. No fake related content.

Artist: 340 dp banner, name over the image, subscriber count when enabled, Play/Shuffle/Follow or the available actions, popular tracks, albums, singles and related artists. Each section is a real browse result. Header scrolls with the page.

### 1.3.11 - Queue and right panel

The right panel is 360 dp wide and has two tabs, Lyrics and Queue. It slides in from the right in 260 ms with `FastOutSlowInEasing`; the content crossfades in 180 ms. The page narrows. Queue header is 30 dp; Lyrics header is 46 dp with a 15 sp Semibold title and a 28 dp close button.

Queue content: current track card, "Up next" section, draggable rows, remove control, `...` menu, Clear. Related discovery is a real browse of the current track. Empty queue has a short state. The full Queue route uses the same row grammar without the panel chrome.

### 1.3.12 - Listen Together

A dialog and a full session surface. Create or join with a code, member list with host badge, now-playing row, shared queue, chat and reactions. Host controls lock local transport for guests. Disconnected and reconnecting states are explicit. The layout keeps the BitChord dark glass, not a separate light modal.

### 1.3.13 - Replay

A page of listening summaries and ranked cards. `#FA2D48` is used only for the rank badge. Cards use artwork-driven tints. Empty state explains that Replay fills as tracks are played.

## 1.3b - Composition maps

### Library Replay cards

Element count: 4 cards plus their internal text stack.

- Row: four equal cards, 12 dp gap, 24 dp top inset.
- Card fill: a vertical artwork tint that falls to `#202020`, with a faint large glyph at the top right.
- Top label: "LA TUA ESPERIENZA DI ASCOLTO" style uppercase label.
- Main figure: 28 to 34 sp bold white.
- Caption: 11 sp uppercase secondary.
- Footer: two columns separated by a hairline, left is the top item, right is the count and date.
- Hover: brightens the artwork tint, no scale.

### Playlist mosaic

Element count: four square covers.

- 2 x 2 grid, no gap, 8 dp outer corner.
- Each quadrant uses the first four distinct covers in stored order.
- Missing quadrants use the playlist's generated placeholder.
- The mosaic is one click target for Play.

### Right panel

Element count: divider, header, close, content.

- 1 px divider at the left edge.
- Queue content gets the sidebar glass; lyrics sit directly on the page.
- Header text is 15 sp Semibold; close icon 16 dp in a 28 dp target.
- Slide and crossfade run together; reduce-motion skips both.

## 1.4 - Animation timelines

```
ANIMATION: Right panel open
Trigger: click Lyrics or Queue
Library: Compose AnimatedVisibility + Crossfade
t=0 ms    panel width 0, opacity 0
t=0 ms    enter expandHorizontally from Start, 260 ms FastOutSlowInEasing
t=0 ms    fadeIn 260 ms
t=0 ms    content Crossfade 180 ms
PROPERTIES: width, opacity, content alpha
RESET: exit shrink 260 ms, fadeOut 130 ms
```

```
ANIMATION: Screen change
Trigger: sidebar destination or collection open
Library: VIVI Motion transition, or none when animations are off
t=0 ms    old page fades out, new page fades in with the existing screen transition
PROPERTIES: content alpha, optional 8 dp horizontal shift
RESET: back reverses
```

```
ANIMATION: Hover on rail row, card and icon button
Trigger: pointer enter
Library: Compose interaction + VIVI hover motion
t=0 ms    fill transparent or #202020
t=120 ms  fill #242424 or white 8 percent, icon brightens
PROPERTIES: background, icon tint
RESET: pointer exit over the same duration
```

```
ANIMATION: Now-playing strip
Trigger: track change, play/pause, volume, queue
Library: Compose state
t=0 ms    cover crossfades, title and artist update
t=0 ms    play/pause glyph swaps
t=180 ms  progress line catches the new value
PROPERTIES: cover alpha, text, icon, progress
```

```
ANIMATION: Inline lyrics, Apple Music option
Trigger: only when the Apple Music v17 player is active
Library: BitChord lyric emphasis
t=0 ms    previous active line drops to secondary
t=0 ms    new active line lifts to white and gains weight
t=220 ms  the line settles, no bounce
PROPERTIES: colour, weight, scale <= 1.02, blur only if the existing VIVI setting is on
RESET: none; the next line takes over
```

The animation toggle in VIVI Settings must gate screen transitions, hover grow, panel slide, mesh and lyric emphasis. Essential feedback such as a progress bar or a focus ring remains when motion is off.

## 1.5 - Micro-interactions

```
INTERACTION: Sidebar row
DEFAULT   bg transparent, text #8E8E93, icon #8E8E93
HOVER     bg #242424, text #FFFFFF
SELECTED  bg white 12%, text #FFFFFF, icon accent
PRESSED   bg white 16%
MECHANISM: Compose background + clickable
DURATION: 120 ms  EASING: VIVI hover spring
```

```
INTERACTION: Shelf card
DEFAULT   cover 168, 8 dp corner, title over bottom scrim
HOVER     cover brightens, title stays white, pointer cursor
PRESSED   cover scale 0.98
MECHANISM: graphicsLayer scale + overlay
DURATION: 120 ms
```

```
INTERACTION: Primary Play pill
DEFAULT   white fill, black label, full pill
HOVER     white 92%, slight lift
PRESSED   white 84%, scale 0.97
FOCUS     2 dp white ring offset 2 dp
MECHANISM: Compose Button with custom surface
```

```
INTERACTION: Song menu
DEFAULT   dark glass card, 232 to 300 dp wide, 12 dp corner, max 460 dp tall
ROW       icon 18, label 14, optional value on the right
HOVER     row fill white 8%
DIVIDER   1 px #2C2C2E
MECHANISM: Popup with focus, scrolls when taller than the window
```

## 1.6 - State machines

```
STATE MACHINE: Search
States: IDLE, TYPING, SUGGESTIONS, SEARCHING, RESULTS, EMPTY, ERROR
IDLE -> TYPING: query changes
TYPING -> SUGGESTIONS: 220 ms debounce, non-empty query
SUGGESTIONS -> SEARCHING: Enter, suggestion click or Search
SEARCHING -> RESULTS: response has items
SEARCHING -> EMPTY: response has no items
SEARCHING -> ERROR: request failure, Retry returns to SEARCHING
RESULTS -> TYPING: query changes
```

```
STATE MACHINE: Song menu
States: MAIN, SLEEP_TIMER
MAIN rows: Like/Remove like, Dislike/Undo dislike, Add to playlist, Download/Cancel/Saved,
Start radio, Play next, Add to queue, Open album, Open artist, Sleep timer, Share.
Offline tracks hide rating, add-to-playlist, download, album and share rows.
SLEEP_TIMER rows: presets 5/10/15/30/45/60, End of this track, Turn off when active.
Rating rows stay open after a click; every other row closes the menu.
```

```
STATE MACHINE: Library
States: SIGNED_OUT, LOADING, CONTENT, EMPTY, ERROR
CONTENT tabs: Replay, device links, on-device shelf, saved shelves.
Show all opens a grid with Sort: Default, A to Z, Z to A.
```

```
STATE MACHINE: Queue
States: EMPTY, CURRENT_ONLY, CURRENT_AND_UP_NEXT, REORDERING
Drag commits the full order through the player.
Party lock disables remove, reorder and clear.
```

## 1.7 - Scroll and fixed chrome

- The rail, player strip and page header do not scroll. Only the page body scrolls.
- Shelves scroll horizontally; the page itself does not scroll sideways.
- Library and Home keep their own scroll position when the user opens and returns.
- Search suggestions and results share one vertical scroll.
- The right panel content scrolls independently; opening it does not move the page scroll.
- The queue keeps the current row visible when the panel opens.
- Artist banner scrolls away with the header; it is not sticky.
- Menus cap at 460 dp and scroll internally.
- On Windows, the window content is not inset by a native title bar when the custom frame is active.

## 1.8 - Technical stack

```
Framework: Compose Multiplatform desktop, Kotlin/JVM 21
UI: Compose Material 3 1.7.x, custom BitChord surfaces and icons
Images: Coil 3, artwork sized by target (CARD_ART_PX, ROW_ART_PX, HEADER_ART_PX)
Glass: Haze 1.7.x on Windows
Animation: Compose AnimatedVisibility, Crossfade, tween, VIVI Motion
Audio: BitChord's custom engine, FFmpeg, JavaFX WebView for auth
Persistence: desktop preferences and local stores
Licence: GPL-3.0, compatible with VIVI's GPL-3.0
```

`musickit` is not a playback backend for this app. VIVI plays YouTube Music through its own JVM player, not Apple Music. Use Apple design and motion guidance for the controls, but keep VIVI's audio engine and queue model.

## 1.9 - Motion philosophy and copy voice

```
MOTION PHILOSOPHY
BitChord moves like a desktop music app, not a marketing site. Most motion is
short state feedback: a row darkens, a panel slides, artwork crossfades. The
large surfaces are calm and never bounce. The right panel is the one deliberate
choreographed move, and it is a width change with a crossfade, not a sheet that
covers the page. Removing motion would cost the sense of place when opening the
queue or lyrics, but would not change how any control works.

COPY VOICE
Tone: direct, quiet, music-literate.
Sentence form: short labels and short empty states, no marketing paragraphs.
Key device: the action's name, then the object. "Add to queue", "Open album",
"Saved to Downloads".
Example pattern: "Your playlists will appear here."
```

## Missing screens and surfaces

These exist in BitChord but were not in the requested list. They need an explicit keep, adapt or remove decision.

- Album page.
- Artist page.
- Playlist detail for account and local playlists.
- Mood and genre page.
- Library Show all grid with sort.
- Replay and listening statistics.
- Local Music and its folders.
- Download manager, active queue and saved files.
- Full queue route, in addition to the right panel.
- Right panel tabs, Lyrics and Queue.
- Listen Together create, join, members, chat and reactions.
- Search history and suggestions.
- Stats for nerds and the audio pipeline dialog.
- Equalizer, audio output and source modules.
- Account, sign-in and Spotify Canvas setup.
- Integrations, lyrics sources and translation language.
- Licences.
- Settings subpages: present in BitChord, excluded by the request; keep VIVI's.
- Expressive player, mini players and lyric animations: present in BitChord, excluded by the request.

## Current VIVI delta

| Area | Current VIVI | Required delta |
|---|---|---|
| Shell | VIVI sidebar and bottom mini player | BitChord rail width, order, grey ladder, Account/Settings split pill |
| Navigation | `Navigation.kt` has Home, Search, Library, Settings and secondary routes | Add Explore, History, Downloads, Local Music as top-level routes |
| Home | `M3EHome` and `HomeScreen` | BitChord shelf grammar, card scrim and 186 dp pitch |
| Explore | `BitChordExploreScreen` exists | Replace its M3 heading and Material surfaces with BitChord chrome and tile grid |
| Library | `BitChordLibraryScreen` exists | Replay cards, device links, on-device shelf and BitChord typography |
| Search | `BitChordSearchScreen` exists | BitChord suggestion rows, result density and states |
| History | VIVI `HistoryScreen` | BitChord table and title treatment |
| Downloads | No top-level screen | New Downloads route and manager UI |
| Local Music | Local playlists only | New Local Music route, folders and scan state |
| Playlist | `BitChordLocalPlaylistScreen` exists | Verify mosaic, header and menu parity |
| Album / Artist | VIVI detail screens | BitChord header, action row and table rhythm |
| Queue | `BitChordQueueScreen` exists | Full route and right panel share one row grammar |
| Right panel | Partial VIVI surface | 360 dp slide panel with Lyrics and Queue tabs |
| Song menu | VIVI `SongMenu` | Add every BitChord row and the sleep timer submenu |
| Settings | VIVI screens | Keep, add only the animation toggle and Account entry |
| Players / lyrics | VIVI players | Keep, add "Apple Music" inline lyric option only for v17 |

## Risks and blocking decisions

1. Version `6.0.8.11_DE-1.56.0-beta10` is lower than the current `6.0.8.11_DE-1.58.5-beta9`. Applying it would move version metadata backwards and break update ordering. A new beta needs a version at or above 1.58.6.
2. SF Pro Display cannot be redistributed. The clone keeps the scale and tracking, not the font files.
3. `musickit` cannot replace VIVI's YouTube playback. The clone keeps the existing engine.
4. BitChord is GPL-3.0. Ported code needs a source comment and the repository's GPL notice.
5. The existing `BitChord*` screens are partial ports with Material 3 surfaces. They are a base, not a finished clone.
6. Settings remain VIVI's, so BitChord's settings subpages and light modal surface are out of scope.
7. The requested 1:1 clone spans at least 19 surfaces and a full shell rewrite. It cannot be completed as one commit without losing the audit and screen-by-screen verification the request asks for.
