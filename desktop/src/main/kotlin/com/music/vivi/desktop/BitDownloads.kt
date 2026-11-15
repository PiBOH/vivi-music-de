package com.music.vivi.desktop

import com.music.innertube.models.SongItem
import com.music.vivi.desktop.player.PlayerController

/**
 * The one place a Bit menu sends "Download" to.
 *
 * The entry used to print "Added to downloads" and stop there: no handler ran
 * behind it. It now asks the player to cache the stream, which is what offline
 * playback reads, and the confirmation is shown only once the request is in.
 * The controller is attached once by the shell, so a row menu on any screen
 * downloads without threading a lambda through every page.
 */
internal object BitDownloads {
    private var player: PlayerController? = null

    fun attach(controller: PlayerController) {
        player = controller
    }

    fun song(song: SongItem) {
        val controller = player
        if (controller == null) {
            DesktopSnackbar.show("Player not ready")
            return
        }
        controller.downloadToCache(song.id)
        DesktopSnackbar.show("Downloading ${song.title}")
    }

    /** A whole album or playlist: one cache request per track. */
    fun songs(songs: List<SongItem>) {
        if (songs.isEmpty()) return
        songs.forEach { song(it) }
    }
}
