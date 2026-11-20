# Album and Playlist visual comparison

Date: 20 November 2026.

Reference: PixelPlayerDesktop 1.1.0, launched from its own Gradle build.
Clone: VIVI Music DE, branch `vivi-music-de`, launched from `:desktop:run`.

Both windows were 1936 x 1096 and were captured at the same tab. The library is
identical in content: three albums, three playlists, two albums with a cover,
one album without art, one playlist with an image cover, one playlist with a
three-song collage and one empty playlist.

## How the two libraries were populated

PixelPlayerDesktop reads a Room database, not the VIVI JSON store. Its reference
library was six 1-second WAV files with ID3 tags and folder covers, scanned by
PixelPlayer itself, plus three playlist rows written into its own
`pixelplayer.db`. The images are the same files under
`docs/ui-parity/album-playlist/fixtures/covers/`.

VIVI Music DE uses `LocalLibraryStore` and `~/.vivimusic/device-sync.json`.
`--seed-demo-library=<dir>` writes the fixture through the same `save` call a
user change uses. For the screenshots the store and settings were redirected to
`build/demo-run/` with `VIVI_SYNC_STATE_FILE` and `VIVI_SETTINGS_FILE`, so the
real library was never replaced and the local scanner had no music folders to
add. The seed also keeps a `device-sync.json.demo-backup-<millis>` copy when it
runs against a normal store.

The first attempt used the normal store and the scanner added a real local song
("The Spectre", Alan Walker) to the six fixture songs. That is why the run uses
the two overrides: it makes the frame exactly three albums, not four.

## Screenshots

| Frame | File |
|---|---|
| PixelPlayerDesktop, Albums | `docs/ui-parity/album-playlist/screenshots/pixelplayer-albums.png` |
| VIVI Music DE, Albums | `docs/ui-parity/album-playlist/screenshots/vivi-music-albums.png` |
| Album cards, PixelPlayerDesktop | `docs/ui-parity/album-playlist/screenshots/album-cards-pixel.png` |
| Album cards, VIVI Music DE | `docs/ui-parity/album-playlist/screenshots/album-cards-vivi.png` |
| PixelPlayerDesktop, Playlists | `docs/ui-parity/album-playlist/screenshots/pixelplayer-playlists.png` |
| VIVI Music DE, Playlists | `docs/ui-parity/album-playlist/screenshots/vivi-music-playlists.png` |
| Playlist rows and covers, PixelPlayerDesktop | `docs/ui-parity/album-playlist/screenshots/playlist-covers-pixel.png` |
| Playlist rows and covers, VIVI Music DE | `docs/ui-parity/album-playlist/screenshots/playlist-covers-vivi.png` |

## Album view, element by element

| Visible element | PixelPlayerDesktop resource or image | VIVI Music DE resource or image | Result |
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

| Visible element | PixelPlayerDesktop resource or image | VIVI Music DE resource or image | Result |
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

## Whole resource set and source bodies

A SHA-256 comparison of both
`src/main/composeResources/drawable` directories gives:

- PixelPlayerDesktop: 154 drawable XML files.
- VIVI Music DE: 154 drawable XML files.
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

No image, icon, placeholder or default cover had to be copied or adapted: the
clone already contains the reference bytes. The only visible differences in the
two frames are the product branding in the sidebar (the VIVI logo and name) and
the extra YouTube Music entry, which are intentional shell features, not Album
or Playlist assets.

## Why PixelPlayerDesktop did not start before

Its project resource folder shipped `libmpv-2.dll` but not the Vulkan loader the
DLL needs. JNA then tried `mpv`, `libmpv-2` and the `.dylib` fallbacks and
reported `UnsatisfiedLinkError`. Copying
`desktop/app-resources/windows/mpv/vulkan-1.dll` from VIVI beside
PixelPlayerDesktop's `libmpv-2.dll` in `src/main/resources-packaged/windows/mpv`
and in `build/compose/tmp/prepareAppResources/mpv` made `./gradlew run` launch
the window. The reference screenshots in this folder come from that build.

The portable PixelPlayerDesktop package was also made runnable the same way and
produced the same frame, so the fix is the DLL, not the portable wrapper.

## Still open

- Hover-only controls (the row play button and the row menus) are not visible in
  a static frame. Their drawables are included in the 154-file hash comparison
  and their composable bodies are identical, but there is no screenshot of the
  hover state.
- The comparison covers Album and Playlist. The rest of the shell is out of
  scope for this check.
