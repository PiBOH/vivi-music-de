# Screenshots

Real app screenshots live in **`images/screenshots/`** on this (`gh-pages`)
branch. They are shown automatically on the **Screenshots** page
(`screenshots.html`) and on the home page, sorted by file name — you never edit
those pages to add one.

## Mandatory rules

1. **Format: `.webp`, `.png` or `.jpg`** (also `.jpeg`) — all of them are shown
   by the gallery. `.webp` is preferred (smallest), but you can upload any of
   them directly, no conversion needed. If the same shot exists in several
   formats, the gallery shows the best one once (`.webp` > `.png` > `.jpg`).
2. **Aspect ratio: 16:9, exactly** (that is the only ratio accepted).
3. Recommended resolutions (16:9):
   - **1920 × 1080** — best quality (full screen),
   - **1280 × 720** — fine for most shots,
   - **2560 × 1440** — only if the detail matters.
4. Keep each file **under 500 KB** — .webp at quality ~80 looks identical and
   keeps the page fast.

## How to add one

1. Take the screenshot, crop it to **16:9** and export it as **`.webp`**
   (or `.png` / `.jpg` — all are supported).
2. Name it with a short, descriptive name, e.g. `full-player.webp`,
   `library.webp`, `synced-lyrics.webp`, `device-sync.webp`. A leading
   `000.`-style number only orders the gallery; it is stripped from the caption.
3. Put the file in `images/screenshots/` and push it on `gh-pages`.
4. The gallery page picks it up automatically after the Pages deploy.

## How the gallery finds the files

Pages serves no directory listing, so `window.vmShots` (in `main.js`) reads
**`images/screenshots.json`**. That file is regenerated from the folder's real
contents by the *Refresh the screenshot manifest* step of the Pages deploy
workflow, which is why step 3 is all you have to do. The copy committed next to
the images is only the fallback for a deploy that never runs (a hand copy of the
site); if it goes stale, the next deploy rewrites it.

## Tips

- Filenames become the captions (`full-player.webp`, `library.png`, `device-sync.jpg` → “Full Player”, “Library”, “Device Sync”).
- Want to hide a shot? Just delete the file.
- The little previews that are not screenshots are CSS mockups — they never need
  updating and are not related to these files.
