@file:OptIn(ExperimentalComposeUiApi::class)

package com.music.vivi.desktop

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.ImageComposeScene
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import org.jetbrains.skia.EncodedImageFormat
import java.io.File

/**
 * Headless check of the Listen Together room screen, run with
 *
 *     ./gradlew :desktop:ltRoomRenderCheck
 *
 * The room needs a live relay connection, so the screen was split into
 * [LtLobby] and [LtInRoom] — plain composables taking the state and a handful of
 * callbacks — and this renders them into an off-screen `ImageComposeScene`. It
 * checks the things that would be visible as broken and that no compiler sees:
 *
 *  * the lobby draws at all, and the avatars draw — index 0 is the username's
 *    initial and index 1 is the phone's own picture, and both must paint
 *    something;
 *  * the room draws for a host and for a guest, and the two draw DIFFERENTLY:
 *    the host sees the transfer/kick/block controls, the guest the lock notice
 *    and the suggestion box, so identical ink would mean one of the two
 *    branches is not on screen at all.
 *
 * What it does NOT check is whether it looks right: no display, no hover, no
 * window chrome. PNGs land under `.ignore/lt/`.
 */
object LtRoomRenderCheck {

    private val outDir = File(".ignore/lt")
    private const val WIDTH = 820
    private const val HEIGHT = 900

    @JvmStatic
    fun main(args: Array<String>) {
        outDir.mkdirs()
        val failures = mutableListOf<String>()

        val lobbyInk = ink(render("lobby") {
            LtLobby(
                language = "en",
                username = "Ada",
                onUsername = {},
                avatarIndex = 1,
                onAvatar = {},
                roomCode = "",
                onRoomCode = {},
                autoApprove = false,
                onAutoApprove = {},
                syncVolume = true,
                onSyncVolume = {},
                smartResync = true,
                onSmartResync = {},
                blockedUsers = emptySet(),
                onUnblock = {},
                logs = emptyList(),
                onClearLogs = {},
                busy = false,
                error = null,
                onCreate = {},
                onJoin = {},
            )
        })
        println("== lobby                ink=$lobbyInk")
        if (lobbyInk <= 0) failures += "the lobby painted nothing"

        // Does a headless render paint glyphs at all? Every assertion below
        // about the avatar's contents rests on the answer, so it is measured
        // rather than assumed.
        val probeEmpty = ink(render("probe-no-text") { Box(Modifier.fillMaxSize()) {} })
        val probeText = ink(render("probe-text") {
            Box(Modifier.fillMaxSize()) {
                androidx.compose.material3.Text("ABCDEFGHIJKL", color = Color.Black)
            }
        })
        println("== probe (no text)      ink=$probeEmpty")
        println("== probe (text)         ink=$probeText")

        // The avatar's contents are checked by comparing PICTURES, not ink: a
        // glyph inside the circle replaces circle pixels, so "pixels that differ
        // from the background" is the same number with and without it. The
        // empty circle, the initial and the emoji must produce three different
        // images — otherwise one of the two branches drew nothing.
        val barePng = render("avatar-empty") {
            Box(Modifier.fillMaxSize()) { AvatarShell(size = 80) }
        }
        val initialPng = render("avatar-initial") {
            Box(Modifier.fillMaxSize()) { LtAvatar(avatarIndex = 0, username = "Ada", size = 80) }
        }
        val emojiPng = render("avatar-picture") {
            Box(Modifier.fillMaxSize()) { LtAvatar(avatarIndex = 1, username = "Ada", size = 80) }
        }
        val bareInk = ink(barePng)
        println("== avatar (empty)       ink=$bareInk")
        println("== avatar (initial)     ink=${ink(initialPng)}")
        println("== avatar (picture)     ink=${ink(emojiPng)}")
        if (bareInk <= 0) failures += "the avatar circle itself painted nothing"
        if (probeText > probeEmpty) {
            if (digest(initialPng) == digest(barePng)) {
                failures += "index 0 drew the same picture as an empty avatar (the initial is missing)"
            }
            if (digest(emojiPng) == digest(barePng)) {
                failures += "index 1 drew the same picture as an empty avatar (the mobile image is missing)"
            }
            if (digest(emojiPng) == digest(initialPng)) {
                failures += "index 0 and index 1 drew the same picture (one avatar kind is missing)"
            }
            // The pictures are the phone's own files, so the LAST one has to
            // load too: a missing resource would fall back to the person vector
            // and look plausible while showing the wrong face.
            val lastPng = render("avatar-last") {
                Box(Modifier.fillMaxSize()) { LtAvatar(avatarIndex = LT_AVATARS.lastIndex, username = "Ada", size = 80) }
            }
            println("== avatar (last)        ink=${ink(lastPng)}")
            if (digest(lastPng) == digest(barePng)) {
                failures += "the last mobile avatar drew an empty circle (its image did not load)"
            }
        } else {
            println("   (this renderer paints no glyphs, so the avatar's contents cannot be checked here)")
        }

        val hostInk = ink(render("room-host") { Room(isHost = true) })
        val guestInk = ink(render("room-guest") { Room(isHost = false) })
        println("== room (host)          ink=$hostInk")
        println("== room (guest)         ink=$guestInk")
        if (hostInk <= 0) failures += "the room painted nothing for the host"
        if (guestInk <= 0) failures += "the room painted nothing for the guest"
        if (hostInk == guestInk) {
            failures += "the host and the guest room painted identically: the role branches are not on screen"
        }

        if (failures.isEmpty()) {
            println()
            println("OK: the lobby, both avatars and both room roles draw, and the two roles differ")
            return
        }
        println()
        println("FAILED:")
        failures.forEach { println("  - $it") }
        throw IllegalStateException("Listen Together room render check failed (${failures.size} case(s))")
    }

    /** The avatar's circle with nothing in it, as the render check's baseline. */
    @Composable
    private fun AvatarShell(size: Int) {
        Box(
            Modifier
                .size(size.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.surfaceContainerHighest),
            contentAlignment = Alignment.Center,
        ) {}
    }

    /**
     * One room, rendered as either role. Everything except [isHost] is constant,
     * so the difference in ink between the two renders is the role's own
     * controls and nothing else.
     */
    @Composable
    private fun Room(isHost: Boolean) {
        LtInRoom(
            language = "en",
            room = room,
            isHost = isHost,
            myUserId = "me",
            pendingJoin = emptyList(),
            buffering = emptyList(),
            pendingSuggestions = emptyList(),
            messages = emptyList(),
            busy = false,
            error = null,
            notice = null,
            suggestInput = "",
            onSuggestInput = {},
            avatarIndex = 1,
            onAvatar = {},
            autoApprove = false,
            onAutoApprove = {},
            syncVolume = true,
            onSyncVolume = {},
            smartResync = true,
            onSmartResync = {},
            blockedUsers = emptySet(),
            onUnblock = {},
            logs = emptyList(),
            onClearLogs = {},
            copied = false,
            copiedFailed = false,
            linkCopied = false,
            onCopy = {},
            onCopyLink = {},
            onApproveJoin = {},
            onRejectJoin = {},
            onKick = {},
            onTransferHost = {},
            onBlock = {},
            onRequestSync = {},
            onSuggest = {},
            onApproveSuggestion = {},
            onRejectSuggestion = {},
            onLeave = {},
        )
    }

    private val room = LtRoomState(
        roomCode = "AB12CD34",
        hostId = "u1",
        users = listOf(
            LtUserInfo("u1", "Ada", isHost = true, isConnected = true, avatarIndex = 1),
            LtUserInfo("u2", "Linus", isHost = false, isConnected = true, avatarIndex = 2),
            LtUserInfo("me", "You", isHost = false, isConnected = true, avatarIndex = 0),
        ),
        isPlaying = true,
        position = 42_000L,
    )

    private fun render(name: String, content: @Composable () -> Unit): ByteArray {
        val scene = ImageComposeScene(WIDTH, HEIGHT) {
            AppTheme(mode = ThemeMode.LIGHT, accent = Color(0xFFED5564)) {
                content()
            }
        }
        return try {
            val png = scene.render().encodeToData(EncodedImageFormat.PNG, 100)!!.bytes
            File(outDir, "$name.png").writeBytes(png)
            png
        } finally {
            scene.close()
        }
    }

    /**
     * How many pixels differ from the page's own background (the pixel in the
     * top-left corner). Counting "non-white" pixels instead made the whole
     * screen "ink" — the app's background is a light lavender, not white — and
     * drowned every glyph in it.
     */
    private fun digest(bytes: ByteArray): String =
        java.security.MessageDigest.getInstance("MD5").digest(bytes).joinToString("") { "%02x".format(it) }

    private fun ink(png: ByteArray): Int {
        val image = javax.imageio.ImageIO.read(png.inputStream())
        val background = image.getRGB(0, 0)
        var ink = 0
        for (y in 0 until image.height) {
            for (x in 0 until image.width) {
                if (image.getRGB(x, y) != background) ink++
            }
        }
        return ink
    }
}
