package com.music.vivi.desktop

/**
 * BitChord's artwork ladder, in px, for the sizes the desktop asks its covers at.
 *
 * The reference names one size per surface so every surface showing the same
 * picture asks for the same number and shares one cache entry: a row is 160 px,
 * a shelf card 480, a page header 720, the media session 544 and the full player
 * 1200. Copied here unchanged, because the desktop's images have to come from
 * the same requests the reference makes.
 */
object ArtworkPx {
    const val ROW = 160
    const val CARD = 480
    const val HEADER = 720
    const val NOTIFICATION = 544
    const val PLAYER = 1200
}

/**
 * Google CDN size path segment, e.g. `w120-h120`: the hint [artworkAt]
 * rewrites. The same pattern the reference uses.
 */
private val artworkSizeHint = Regex("""w\d+-h\d+""")

/**
 * BitChord's `String?.artworkAt(px)`: the provider's own URL, at another size.
 *
 * The hint already inside the URL is replaced in place, so the crop and the
 * palette the provider put there stay exactly as they were - `=w120-h120-p-l90-rj`
 * becomes `=w1200-h1200-p-l90-rj`, which is what the reference loads. A URL with
 * no hint is a provider that serves one size: the hint is appended with the same
 * square crop the rest of the desktop asks for.
 */
fun String?.artworkAt(px: Int): String? {
    val url = this ?: return null
    if (url.isBlank()) return this
    if (artworkSizeHint.containsMatchIn(url)) {
        return url.replace(artworkSizeHint, "w$px-h$px")
    }
    return "${url.substringBefore("=s")}=w$px-h$px-p-l90-rj"
}
