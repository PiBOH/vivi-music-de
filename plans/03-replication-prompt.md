# Replication prompt - VIVI Music DE x BitChord

## 1. ROLE + AESTHETIC IDENTITY

**Role:** Act as a senior Compose Desktop engineer and interface designer. Build inside the existing VIVI Music DE application. Do not replace its audio engine, network layer, account system, sync or Settings.

**Aesthetic identity:** Nocturne listening room, measured and exact.

The result must feel like BitChord's desktop client with VIVI's engine underneath: charcoal chrome, quiet grey hierarchy, dense music rows, one red accent used only by Replay, and motion that behaves like a desktop instrument rather than a marketing page.

## 2. CORE DESIGN SYSTEM

### Palette

| Semantic name | Value | Usage |
|---|---|---|
| Chrome | `#171717` | Sidebar, main player chrome, mini-player chrome |
| Page | `#202020` | Routed page background |
| Divider | `#121213` | 1 px rail edge and chrome edges |
| Card | `#0C0C0C` | Artwork card fill |
| Card border | `#141414` | Card outline |
| Search field | `#1D1D1D` | Sidebar search fill |
| Search border | `#242424` | Search outline |
| Hover | `#242424` | Rail row hover and selected support |
| Secondary text | `#8E8E93` | Subtitles, inactive labels |
| Primary text | `#FFFFFF` | Titles and primary actions |
| Replay accent | `#FA2D48` | Replay rank badge only |
| Dark surface | `#0D0D0F` | Menu and floating surface |
| Surface variant | `#1C1C1E` | Menu row hover |
| Outline | `#2C2C2E` | Menu divider |

Rules:

- The cloned BitChord chrome never uses the user's Material accent for primary text or the main Play control. White is the primary action.
- The user's accent remains available in the parts of VIVI that already own it, such as focus rings and existing Settings.
- Selected rail row: white at 12 percent, white label, user accent icon.
- Do not introduce gradients into the shell. Artwork supplies the only colour.

### Typography

Use the bundled `Sans Flex` variable font for the BitChord surfaces. Do not add SF Pro or Inter files. Settings keep the user's selected VIVI font.

| Role | Weight | Size | Tracking | Usage |
|---|---|---|---|---|
| Display large | 800 | 34 sp | -0.5 | Page-scale display |
| Headline large | 800 | 30 sp | -0.4 | Large page title |
| Headline medium | 700 | 22 sp | -0.25 | Shelf title |
| Title large | 700 | 20 sp | -0.2 | Section title |
| Title medium | 600 | 16 sp | 0.05 | Row title |
| Body large | 400 | 16 sp | 0.2 | Body |
| Body medium | 400 | 14 sp | 0.3 | Secondary body |
| Label medium | 600 | 12 sp | 0.25 | Metadata |
| Label small | 600 | 11 sp | 0.3 | Uppercase captions |

Draw each weight one cut lighter than requested, matching BitChord's `FontWeight` mapping. Preserve the negative tracking above 20 sp and the slightly open tracking below 16 sp.

### Spacing, radius and elevation

```
Base unit                 8 dp
Rail width                220 dp
Rail inset                12 dp
Rail search height        36 dp
Rail row pitch            36 dp
Rail row padding          7 dp vertical
Rail icon                 18 dp
Rail icon-label gap       10 dp
Content left inset        16 dp from the rail rule
Content right inset       18 dp
Artwork card              168 x 168 dp
Artwork gap               18 dp
Artwork pitch             186 dp
Player height             64 dp in BitChord; VIVI uses its bottom mini-player height
Right panel width         360 dp
Rail row radius           7 dp
Card and artwork radius   8 dp
Menu radius               12 dp
```

No shell shadow. Use 1 px rules and the grey ladder. Floating menus keep VIVI's popup elevation.

### Motion tokens

```
Hover fill                120 ms, VIVI hover spring
Right panel slide         260 ms, FastOutSlowInEasing
Right panel content fade  180 ms
Right panel close         260 ms slide, 130 ms fade
Page transition           existing VIVI screen transition
Lyric emphasis            220 ms, no bounce
```

All motion is multiplied by the user's animation level. Level 0 is off. The default is 4. Essential progress, focus and loading feedback never disappear.

## 3. COMPONENT ARCHITECTURE

### 3.1 Global shell - "The Charcoal Frame"

```
BITCHORD SHELL - adapted to VIVI
total width variable | sidebar 220 | content rest
┌──────────────────────┬──────────────────────────────────────────────┐
│ SIDEBAR 220          │ PAGE #202020                                 │
│ #171717              │ 16 dp left, 18 dp right                      │
│ 1 px #121213 right   │                                              │
│                      │  page header when the route has one          │
│ search 36            │                                              │
│ Home                 │  shelves, grids, tables, queue or lyrics     │
│ Explore              │                                              │
│ Library              │                                              │
│ Search               │                                              │
│ History              │                                              │
│ Downloads            │                                              │
│ Local Music          │                                              │
│ playlists            │                                              │
│ Account | Settings   │                                              │
└──────────────────────┴──────────────────────────────────────────────┘
                       MINI-PLAYER at the bottom of the content column
```

- The sidebar runs the full window height. The mini-player sits at the bottom of the content column, not across the sidebar.
- The page narrows when the right panel opens. The panel never covers the page.
- Keep VIVI's native window frame.
- The rail order is Home, Explore, Library, Search, History, Downloads, Local Music, then the real playlist list. Dividers separate the groups.
- Replace the old bottom Settings row with one outlined pill split control: `Account | Settings`. The Account half shows `Guest` or the signed-in name and opens Account. The Settings half opens VIVI Settings. One outline, one internal 1 px divider.
- Long playlist names ellipsize and expose a tooltip.
- Party-session locks disable destructive controls, not navigation.

### 3.2 Sidebar row - "The Quiet Rail"

```
DEFAULT   transparent, #8E8E93 text and icon
HOVER     #242424 fill, #FFFFFF text
SELECTED  white 12% fill, #FFFFFF text, accent icon
PRESSED   white 16% fill
```

Height 36 dp. Icon 18 dp. Label 14 sp. Radius 7 dp. Hover and press use the VIVI hover spring, gated by the animation level. Selected rows keep the icon in the user's accent.

### 3.3 Home - "The Evening Shelves"

```
┌─────────────────────────────────────────────────────┐
│  Buonasera / Good evening                           │
│  ┌──────────┐ ┌──────────┐ ┌──────────┐ ┌────────┐  │
│  │ 168x168  │ │ 168x168  │ │ 168x168  │ │ 168... │  │
│  └──────────┘ └──────────┘ └──────────┘ └────────┘  │
│  22 sp shelf title                    Show all      │
│  horizontal shelf, 18 dp gap, 186 dp pitch          │
└─────────────────────────────────────────────────────┘
```

- The greeting is time-based and localized. Use `Buongiorno`, `Buon pomeriggio`, `Buonasera` in Italian and `Good morning`, `Good afternoon`, `Good evening` in English.
- The greeting is the only new heading. Do not add marketing copy.
- Each shelf has a 22 sp title, an optional `Show all` text action and square cards.
- Card: 168 dp, 8 dp radius, bottom scrim, title over the lower edge.
- Hover: cover brightens. Press: scale 0.98. Duration 120 ms.
- Card click opens album, playlist, artist or track. Right click opens the shared context menu.
- States: loading skeleton shelves, empty library, signed-out banner, error with Retry. No fake content.
- Preserve scroll position when leaving and returning.

### 3.4 Explore - "The Mood Grid"

```
Mood & genres
┌────────────┐ ┌────────────┐ ┌────────────┐
│ artwork    │ │ artwork    │ │ artwork    │
│ title      │ │ title      │ │ title      │
└────────────┘ └────────────┘ └────────────┘
```

- No large page title on desktop. A responsive artwork grid, 8 dp corners, title over the lower edge.
- Column count follows available width. Tiles open the mood or genre route.
- Loading shimmer tiles. Error with Retry. Empty state with one short sentence.

### 3.5 Library - "The Listening Ledger"

```
Replay summary cards, 4 across, artwork-tinted
┌────────────┐ ┌────────────┐ ┌────────────┐
│ 63         │ │ Top artist │ │ Top song   │
└────────────┘ └────────────┘ └────────────┘
Downloads                              >
Local Music                            >
On device
┌────────┐
│ cover  │  playlist title / count
└────────┘
Playlist                             Show all
shelf...
```

- Lead with four Replay summary cards: 12 dp gap, 24 dp top inset, vertical artwork tint to `#202020`, large white figure, uppercase caption, two-column footer separated by a hairline.
- Hover brightens the tint. No scale.
- Device links open Downloads and Local Music.
- On-device shelf uses real local playlists. Saved collections follow.
- `Show all` opens a grid with `Sort: Default, A to Z, Z to A`.
- States: signed out, loading, content, empty, error.

### 3.6 Search - "The Immediate Field"

```
suggestion row   "banana joe bud spencer"      Enter
suggestion row   "banana joe sigla"            Enter
─────────────────────────────────────────────────────
result row: cover 56, title, subtitle, menu
result row: cover 56, title, subtitle, menu
```

- The sidebar search field is the only query input. The page shows recent entities, suggestions and results.
- Debounce 220 ms. Enter submits. Up and Down move through suggestions. Escape clears focus.
- Result groups: songs, albums, artists, playlists, each with a true count.
- Result row: 56 dp cover, title, subtitle, `...` menu. Click plays or opens the entity.
- States: IDLE, TYPING, SUGGESTIONS, SEARCHING, RESULTS, EMPTY, ERROR. Retry returns to SEARCHING.

### 3.7 History

- Page title `History` and a `Clear` action when entries exist.
- Table: 48 dp cover, title, artist, album, duration, `...`.
- Row click plays. Menu uses the shared song menu.
- Empty state is a short explanation, no fake rows.

### 3.8 Downloads

- Page title `Downloads`.
- Active, queued and failed downloads show progress and a `...` action.
- Completed section lists size and `...`.
- Header actions: clear completed, open folder.
- Empty state explains that downloads are stored locally.

### 3.9 Local Music

- Page title `Local Music`, subtitle `Audio files found in Music and Downloads`.
- Folder source rows, scan status, track count, real local track table.
- Rescan action in the header.
- Local tracks omit YouTube rating, share and remote-only menu rows.

### 3.10 Playlist - "The Four-Cover Seal"

```
┌────────┐  PLAYLIST
│ 2x2    │  Playlist name
│ mosaic │  12 tracks
└────────┘  (shuffle) (play) (...)
#   cover  title / artist                  duration
1   ...
```

- Header uses a 2 x 2 mosaic of the first four distinct covers, no gap, 8 dp outer radius, missing quadrants use the generated placeholder.
- Small uppercase `PLAYLIST` label, display name, true track count, round controls.
- Table follows stored order. Row click plays. Heart writes the same like as the rest of the app. `...` opens rename, delete and shared actions.
- Missing playlist shows a plain not-found state.

### 3.11 Album and Artist

Album: large cover, title, artist, year, track count, Play, Shuffle, `...`, real track table. Optional low-contrast cover tint. No fake related content.

Artist: 340 dp banner, name over image, subscriber count when enabled, Play, Shuffle, Follow or available actions, popular tracks, albums, singles, related artists. Banner scrolls away with the header.

### 3.12 Queue and right panel - "The Sliding Drawer"

```
Right panel: 360 dp, 1 px divider at left
┌──────────────────────────────┐
│ Lyrics | Queue          close│
│                              │
│ current track card           │
│ Up next                      │
│ draggable rows               │
│ remove / ... / Clear         │
└──────────────────────────────┘
```

```
ANIMATION: right panel open
t=0 ms    width 0, opacity 0
t=0 ms    expand horizontally, 260 ms FastOutSlowInEasing
t=0 ms    fade in 260 ms
t=0 ms    content crossfade 180 ms
PROPERTIES: width, opacity, content alpha
RESET: shrink 260 ms, fade out 130 ms
```

- Two tabs: Lyrics and Queue. Header 30 dp for Queue, 46 dp for Lyrics with a 15 sp semibold title and a 28 dp close target.
- Queue: current track card, `Up next`, draggable rows, remove, `...`, Clear. Related discovery uses real browse data.
- Page narrows; it does not move its scroll position.
- Party lock disables remove, reorder and clear.
- States: EMPTY, CURRENT_ONLY, CURRENT_AND_UP_NEXT, REORDERING.
- The full Queue route uses the same row grammar without panel chrome.
- Reduce motion skips slide and crossfade.

### 3.13 Main player - "The Black Instrument"

Clone the main BitChord player's appearance and control layout, but keep VIVI's audio engine, queue, media keys and playback state.

- Dark chrome, large artwork, white primary Play, secondary transport in grey.
- Keep the real progress, shuffle, repeat, volume, output, lyrics and queue actions.
- Do not clone BitChord's expressive player variants. Do not clone the mini-player. The bottom mini-player remains VIVI's, restyled only to the BitChord chrome tokens.
- The player must not become a second source of playback state. It reads and writes the existing VIVI player.
- Keep VIVI's lyrics renderer. Add one inline lyric emphasis option named `Apple Music`, available only when the `Apple Music v17` player is active. It uses the right-panel lyric timing below, not BitChord's full lyric animation.

```
ANIMATION: inline lyrics, Apple Music option
t=0 ms    previous line to secondary
t=0 ms    new line to white, weight up
t=220 ms  settle, no bounce
PROPERTIES: colour, weight, scale <= 1.02
RESET: next line takes over
```

### 3.14 Listen Together

- Create or join with a code, member list with host badge, now-playing row, shared queue, chat and reactions.
- Host controls lock guest transport. Disconnected and reconnecting states are explicit.
- Use the BitChord dark glass, not a light modal.
- Real session data only. No simulated members or messages.

### 3.15 Replay

- Listening summaries and ranked cards.
- `#FA2D48` appears only on the rank badge.
- Artwork-tinted cards, large white figures.
- Empty state explains that Replay fills as tracks are played.

### 3.16 Shared song menu - "The Full Ledger"

```
MAIN
  Like / Remove like
  Dislike / Undo dislike
  Add to playlist
  Download / Cancel / Saved
  Start radio
  Play next
  Add to queue
  Open album
  Open artist
  Sleep timer
  Share
SLEEP TIMER
  5 / 10 / 15 / 30 / 45 / 60 minutes
  End of this track
  Turn off when active
```

- Width 232 to 300 dp, radius 12 dp, maximum height 460 dp, internal scroll.
- Row: 18 dp icon, 14 sp label, optional value. Hover white 8 percent. Divider `#2C2C2E`.
- Rating rows stay open after click. Every other row closes the menu.
- Offline tracks hide rating, add-to-playlist, download, album and share rows.
- Right click and `...` open the same menu.
- Every row has the real VIVI action and feedback. No dead items.

### 3.17 States and empty surfaces

Every routed screen needs default, hover, pressed, disabled, loading, empty and error states. Loading uses skeletons that match the final geometry. Empty states use one short sentence and one real action when available. Errors use one sentence and Retry. Signed-out states use a real sign-in action.

## 4. TECHNICAL REQUIREMENTS

```
TECHNICAL REQUIREMENTS
  Stack:                Kotlin/JVM, Compose Desktop, Material 3, Coil 3, Haze
  New UI dependencies:  none unless a screen is impossible; stop and explain first
  Audio:                existing VIVI engine and queue, no MusicKit, no second player
  Settings:             existing VIVI Settings; add only the animation level and Account entry
  Version:              6.0.8.11_DE-1.58.6-beta10, DE code 449
  Localization:         English and Italian keys for every new string
  Motion:               level 0 to 5, default 4, level 0 disables non-essential motion
  Performance:          no network optimization; lazy lists, keyed rows, stable lambdas,
                        no per-frame state reads, artwork sized per target
  Accessibility:        focus rings, keyboard navigation, tooltips for truncated labels,
                        contrast at least 4.5:1 for body text
  Licensing:            BitChord is GPL-3.0; keep attribution in ported files
  Verification:         compile, run the affected screen, compare against the audit,
                        commit each milestone with `chore(backup): ... [skip ci]`
```

Implementation order:

1. Shell, rail, Account | Settings split pill, right panel, version and animation setting.
2. Home.
3. Explore.
4. Library.
5. Search.
6. History.
7. Downloads.
8. Local Music.
9. Playlist, album and artist.
10. Queue, right panel and shared menus.
11. Listen Together and Replay.
12. Main player visual clone.
13. Localization, quality check, performance pass and installer.

Do not mark a screen complete until it renders with real data, its states are reachable and its menus call real actions.

## 5. EXECUTION DIRECTIVE

*Build it like a quiet desktop instrument: every pixel earns its place, every movement answers the hand, and nothing pretends to be there.*
