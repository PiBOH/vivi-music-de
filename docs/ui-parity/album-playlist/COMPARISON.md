# Album and Playlist visual comparison

Date: 20 November 2026.

Reference: PixelPlayerDesktop 1.1.0, launched from its own Gradle build.
Clone: Axolotl Music, branch `axolotl-music-de`, launched from `:desktop:run`.

Both windows were 1936 x 1096 and were captured at the same tab. The library is
identical in content: three albums, three playlists, two albums with a cover,
one album without art, one playlist with an image cover, one playlist with a
three-song collage and one empty playlist. The empty frames use no songs and no
music folders, and the hover frames use the populated fixture with the pointer
on the first playlist row.

## How the two libraries were populated

PixelPlayerDesktop reads a Room database, not the VIVI JSON store. Its reference
library was six 1-second WAV files with ID3 tags and folder covers, read by
PixelPlayer, plus three playlist rows written into its own `pixelplayer.db`.
The artwork cache and the playlist row use the same JPEG bytes as
`docs/ui-parity/album-playlist/fixtures/covers/`.

Axolotl Music uses `LocalLibraryStore` and `~/.vivimusic/device-sync.json`.
`--seed-demo-library=<dir>` writes the fixture through the same `save` call a
user change uses. The flag refuses to run without `VIVI_SYNC_STATE_FILE`, so the
default store is outside its write path. For the screenshots the store and
settings were redirected to `build/demo-run/` with `VIVI_SYNC_STATE_FILE` and
`VIVI_SETTINGS_FILE`: the store holds only the fixture and the local scanner
has no music folders to add, which keeps the frame at exactly three albums.

## Screenshots

| Frame | File |
|---|---|
| PixelPlayerDesktop, Albums | `docs/ui-parity/album-playlist/screenshots/pixelplayer-albums.png` |
| Axolotl Music, Albums | `docs/ui-parity/album-playlist/screenshots/vivi-music-albums.png` |
| Album cards, PixelPlayerDesktop | `docs/ui-parity/album-playlist/screenshots/album-cards-pixel.png` |
| Album cards, Axolotl Music | `docs/ui-parity/album-playlist/screenshots/album-cards-vivi.png` |
| PixelPlayerDesktop, Playlists | `docs/ui-parity/album-playlist/screenshots/pixelplayer-playlists.png` |
| Axolotl Music, Playlists | `docs/ui-parity/album-playlist/screenshots/vivi-music-playlists.png` |
| Playlist rows and covers, PixelPlayerDesktop | `docs/ui-parity/album-playlist/screenshots/playlist-covers-pixel.png` |
| Playlist rows and covers, Axolotl Music | `docs/ui-parity/album-playlist/screenshots/playlist-covers-vivi.png` |
| Empty Albums, PixelPlayerDesktop | `docs/ui-parity/album-playlist/screenshots/pixelplayer-albums-empty.png` |
| Empty Albums, Axolotl Music | `docs/ui-parity/album-playlist/screenshots/vivi-music-albums-empty.png` |
| Empty Playlists, PixelPlayerDesktop | `docs/ui-parity/album-playlist/screenshots/pixelplayer-playlists-empty.png` |
| Empty Playlists, Axolotl Music | `docs/ui-parity/album-playlist/screenshots/vivi-music-playlists-empty.png` |
| Playlist hover, PixelPlayerDesktop | `docs/ui-parity/album-playlist/screenshots/pixelplayer-playlists-hover.png` |
| Playlist hover, Axolotl Music | `docs/ui-parity/album-playlist/screenshots/vivi-music-playlists-hover.png` |
| Play button close-up, PixelPlayerDesktop | `docs/ui-parity/album-playlist/screenshots/playlist-hover-pixel.png` |
| Play button close-up, Axolotl Music | `docs/ui-parity/album-playlist/screenshots/playlist-hover-vivi.png` |

## Album view, element by element

| Visible element | PixelPlayerDesktop resource or image | Axolotl Music resource or image | Result |
|---|---|---|---|
| Shuffle button | `ic_rounded_shuffle` | `ic_rounded_shuffle` | Same XML, byte for byte |
| Grid view button | `ic_rounded_grid_view` | `ic_rounded_grid_view` | Same XML |
| List view button | `ic_automirrored_rounded_view_list` | `ic_automirrored_rounded_view_list` | Same XML |
| Sort button | `ic_automirrored_rounded_sort` | `ic_automirrored_rounded_sort` | Same XML |
| After Hours card cover | `fixtures/covers/after-hours.jpg` | same file | Same JPEG, rendered with the same crop and 3:2 card |
| Kind of Blue card, no art | `ic_rounded_album`, 44 dp, on the extracted card colour | `ic_rounded_album`, 44 dp, same fallback | Same glyph and same visible position |
| Random Access Memories card cover | `fixtures/covers/random-access-memories.jpg` | same file | Same JPEG |
| Album card gradient | colour extracted from the cover by `DynamicColorProcessor` | same class body and same palette settings | Same blue, blue and orange cards after extraction settles |
| Album list row placeholder | `ic_rounded_album` | `ic_rounded_album` | Same XML |
| Album list row menu | `ic_rounded_more_vert` | `ic_rounded_more_vert` | Same XML |
| Album detail header placeholder | `ic_rounded_album` | `ic_rounded_album` | Same XML |
| Album detail actions | `ic_rounded_shuffle`, `ic_rounded_auto_fix_high` | same two names | Same XML |
| Album detail song rows | `ic_rounded_music_note`, `ic_rounded_more_vert`, `ic_rounded_close` | same three names | Same XML |
| Empty Albums state | `LibraryEmptyState`: `ic_rounded_album`, `ic_rounded_search_off`, `ic_rounded_favorite_border`, `ic_rounded_computer`, `ic_rounded_cloud_off`, `ic_artist`, `ic_rounded_music_note` | same seven names, same composable body | Same XML for every name |

## Playlist view, element by element

| Visible element | PixelPlayerDesktop resource or image | Axolotl Music resource or image | Result |
|---|---|---|---|
| New button | `ic_automirrored_rounded_playlist_add` | `ic_automirrored_rounded_playlist_add` | Same XML |
| Import button | `ic_rounded_upload_file` | `ic_rounded_upload_file` | Same XML |
| Sort button | `ic_automirrored_rounded_sort` | `ic_automirrored_rounded_sort` | Same XML |
| Late Night Drive cover | `fixtures/covers/late-night-drive.jpg` | same file | Same JPEG, 58 dp, 10 dp corner |
| Collage Mix, two songs with art | `after-hours.jpg` twice | same file twice | Same two circular crops |
| Collage Mix, third song without art | `ic_rounded_music_note` | `ic_rounded_music_note` | Same glyph in the third circle |
| Empty Queue cover | `ic_automirrored_rounded_queue_music` in the `secondaryContainer` circle | same glyph and same container | Same icon and colour |
| Row overflow | `ic_rounded_more_vert` | `ic_rounded_more_vert` | Same XML |
| Row hover play | `ic_rounded_play_arrow` | `ic_rounded_play_arrow` | Same XML |
| Row menu pin, edit, delete, export | `ic_rounded_push_pin`, `ic_rounded_edit`, `ic_rounded_delete`, `ic_outlined_file_upload` | same four names | Same XML |
| Empty Playlists state | `ic_automirrored_rounded_playlist_play`, `ic_rounded_add`, `ic_rounded_upload_file` | same three names, same composable body | Same XML |
| Playlist icon choices | `ic_rounded_music_note`, `ic_rounded_headphones`, `ic_rounded_album`, `ic_rounded_mic_external_on`, `ic_rounded_speaker`, `ic_rounded_favorite`, `ic_rounded_piano`, `ic_automirrored_rounded_queue_music` | same eight names | Same XML for every mapping |
| Playlist detail actions | add, delete, drag, edit, more, music note, play, pin, remove circle, shuffle, upload file | same names | Same XML |

## Empty states

The empty run used no songs, no playlists and an empty `musicDirectories` list.
PixelPlayer's database was copied to a temporary backup, its library tables were
emptied, and the database and settings were restored after the captures. VIVI
used `build/empty-run` through `VIVI_SYNC_STATE_FILE` and `VIVI_SETTINGS_FILE`.

| Visible element | PixelPlayerDesktop | Axolotl Music | Result |
|---|---|---|---|
| Album empty icon | `ic_rounded_album`, 54 dp, 45% `onSurfaceVariant` | same drawable, size and tint | Same XML and geometry |
| Album empty title | "No albums in library" | same string | Same `LibraryEmptyState` body |
| Album empty subtitle | "Add music directories in Settings to scan" | same string | Same body |
| Album count chip | "0 albums" | same string | Same `LibraryActionRow` |
| Playlist empty icon | `ic_automirrored_rounded_playlist_play`, 54 dp, 40% `onSurfaceVariant` | same drawable, size and tint | Same XML and geometry |
| Playlist empty title | "No playlists created yet" | same string | Same `PlaylistsTab` body |
| Create Playlist button | filled `Button`, `ic_rounded_add` 18 dp | same button and drawable | Same XML and role |
| Import M3U button | `FilledTonalButton`, `ic_rounded_upload_file` 18 dp | same button and drawable | Same XML and role |
| Header actions | New, Import, "0 playlists", sort | same controls | Same body |
| Sidebar | PixelPlayer branding | VIVI branding and YouTube Music entry | Intentional shell difference, no Album or Playlist asset |

The Album and Playlist empty frames match in icon, title, subtitle, button order
and button roles. The only shifts are the VIVI branding and the extra YouTube
Music row in the sidebar.

## Playlist row hover

The pointer was placed on the first row ("Collage Mix", 3 songs) and the frame
was taken after the hover state settled. The OS screenshot does not draw the
pointer itself; the visible evidence is the 38 dp Play button that exists only
while the row is hovered and the playlist has at least one song.

| Visible element | PixelPlayerDesktop | Axolotl Music | Result |
|---|---|---|---|
| Hover-only Play button | `ic_rounded_play_arrow`, 20 dp in a 38 dp `primary` circle | same drawable and geometry | Same XML and body; present in both frames |
| Play button condition | `isHovered && playlistSongs.isNotEmpty()` | same condition | Same `PlaylistListItem` body |
| Overflow button | `ic_rounded_more_vert`, 18 dp in a 36 dp transparent target | same drawable and geometry | Same XML and body; visible in both frames |
| Row hover fill | `surfaceContainerHigh` over `surfaceContainerLow` | same two roles | Same code; the rendered tone follows each app's current palette |
| Tooltip names | "Play playlist", "More options" | same strings | Same body |

The play control and the overflow button sit in the same trailing position in
both frames. The Pixel frame's card fill reads darker than the VIVI frame's
because the two apps resolve the current dynamic scheme differently; the
control, its 38 dp target and its icon are identical. The 0-song "Empty Queue"
row has no Play button by design, so the hover frame uses the 3-song row.

## Whole resource set and source bodies

A SHA-256 comparison of both
`src/main/composeResources/drawable` directories gives:

- PixelPlayerDesktop: 154 drawable XML files.
- Axolotl Music: 154 drawable XML files.
- Missing in VIVI: 0.
- Extra in VIVI: 0.
- Different content: 0.

The Album and Playlist screen and component bodies are identical after
normalising only the package/import prefix (`com.kayoi.pixelplayerdesktop` to
`com.music.vivi.desktop`): `AlbumsTab.kt`, `PlaylistsTab.kt`,
`AlbumGridItem.kt`, `AlbumListItem.kt`, `PlaylistListItem.kt`,
`PlaylistCover.kt`, `PlaylistArtCollage.kt`, `AlbumArtCollage.kt`,
`AlbumDetailScreen.kt`, `PlaylistDetailScreen.kt`, `LibraryActionRow.kt`,
`LibraryEmptyState.kt` and `EnhancedSongListItem.kt`.

No Album or Playlist image, icon, placeholder or default cover had to be copied
or adapted: the clone already contains the reference bytes. The only visible
differences in the populated Album and Playlist frames are the product branding
in the sidebar (the VIVI logo and name) and the extra YouTube Music entry, which
are intentional shell features, not Album or Playlist assets.

## Why PixelPlayerDesktop did not start before

Its project resource folder had `libmpv-2.dll` but not the Vulkan loader the
DLL needs. JNA then tried `mpv`, `libmpv-2` and the `.dylib` fallbacks and
reported `UnsatisfiedLinkError`. Copying
`desktop/app-resources/windows/mpv/vulkan-1.dll` from VIVI beside
PixelPlayerDesktop's `libmpv-2.dll` in `src/main/resources-packaged/windows/mpv`
and in `build/compose/tmp/prepareAppResources/mpv` made `./gradlew run` launch
the window. The reference screenshots in this folder come from that build.

The portable PixelPlayerDesktop package was also made runnable the same way. Its
Album and Playlist PNGs are byte-identical to the Gradle build's, so the fix is
the DLL, not the portable wrapper.

## Still open

- The fixture has metadata and covers only: `Song.path` points at
  `demo-library/...` and no audio file exists, so its rows are not playable.
- The comparison covers Album and Playlist. The rest of the shell is out of
  scope for this check.
