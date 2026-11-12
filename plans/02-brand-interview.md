# Brand interview - VIVI Music DE x BitChord

Reference: `plans/01-site-dna.md`
Target version: `6.0.8.11_DE-1.58.6-beta10`, DE version code 449.

## Answers

1. Product identity
VIVI Music DE, a desktop client for YouTube Music with the BitChord interface.

2. Audience
Desktop listeners aged 18 to 40 who use YouTube Music regularly and care about a dense, polished interface.

3. Brand feeling
Dark, calm, precise.

4. Palette and type
Exact BitChord grey ladder and semantic colours. Use an existing bundled font. `Sans Flex` is the closest available neutral face. Settings keep the user's current font.

5. Screens
All surfaces found in the audit: Home, Explore, Library, Search, History, Downloads, Local Music, playlist, album, artist, mood and genre, Replay, full queue, right panel, Listen Together and the shared menus. Settings remain VIVI's. The main player is cloned from BitChord. Mini-player and lyrics remain VIVI's.

6. Home heading
A dynamic greeting such as "Buonasera", localized in English and Italian. Below it, the BitChord shelf feed and real content.

7. Primary action
Play, as in BitChord. BitChord has no marketing call to action.

8. Differentiator
BitChord's desktop layout and interaction model with VIVI's YouTube engine, account, sync and local features.

9. Animation
A 0 to 5 selector in Settings. 0 turns animations off. The default is 4. The same setting controls screen transitions, hover growth, the right panel slide and lyric emphasis. Essential progress and focus feedback remain.

10. Technology
Use the existing stack only: Kotlin, Compose Desktop, Material 3, Coil and Haze. Add no UI library unless a screen is impossible without it; in that case stop and explain the gap first.

11. Assets
Use an existing bundled font, the VIVI logo, YouTube cover art and the BitChord icons under GPL-3.0. Do not bundle SF Pro. No stock photography.

12. Section changes
Keep and adapt every audited screen. Do not clone BitChord's Settings. Clone the main BitChord player's appearance and controls, but keep VIVI's audio engine and keep mini-player and lyrics as VIVI's. Add the "Apple Music" inline lyric animation option for the Apple Music v17 player only.

## Resolved conflicts

- The requested 1.56.0-beta10 is older than the current 1.58.5. The owner chose 1.58.6-beta10, DE code 449.
- `musickit` does not apply to YouTube playback. The player keeps the VIVI engine; Apple Motion and Apple Design guide the visuals.
- The initial exclusion of all players was later narrowed by the owner: clone only the main player. Mini-player and lyrics remain VIVI's.
- The existing `BitChord*` screens are partial ports. They are the starting point, not finished screens.
