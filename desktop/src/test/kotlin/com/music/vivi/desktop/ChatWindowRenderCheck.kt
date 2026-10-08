@file:OptIn(ExperimentalComposeUiApi::class)

package com.music.vivi.desktop

import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.ui.ImageComposeScene
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import org.jetbrains.skia.EncodedImageFormat
import java.io.File

/**
 * Headless check of the Listen Together chat list, run with
 *
 *     ./gradlew :desktop:chatWindowRenderCheck
 *
 * The chat lives in a `Window`, which needs a display, so the conversation was
 * split into [ListenTogetherChatList] (the bubbles, the day separators, the empty
 * state) and the window around it. This renders the list into an off-screen
 * `ImageComposeScene` and checks the three things that would be visible as
 * broken:
 *
 *  * an empty conversation draws its empty state instead of nothing;
 *  * a conversation draws bubbles at all (own and other people's, which are two
 *    different shapes and two different colours);
 *  * a bubble that quotes another one draws **strictly more** than the same
 *    conversation with the quote removed, which is what proves the quote is
 *    actually on screen and not a field that is parsed and dropped.
 *
 * What it does NOT check is whether it looks right: no display, no hover, so the
 * hover-revealed reply button is not part of the picture. PNGs land under
 * `.ignore/chat/`.
 */
object ChatWindowRenderCheck {

    private val outDir = File(".ignore/chat")
    private const val WIDTH = 640
    private const val HEIGHT = 720

    @JvmStatic
    fun main(args: Array<String>) {
        outDir.mkdirs()
        val failures = mutableListOf<String>()
        val now = System.currentTimeMillis()

        val quoted = listOf(
            message("u1", "Ada", "Anyone else hearing the bass drop out?", now - 120_000),
            message("me", "You", "Yes, on the second chorus.", now - 90_000),
            message(
                "u2",
                "Linus",
                "Same here, it comes back a second later.",
                now - 60_000,
                replyTo = LtRepliedMessage("Ada", "Anyone else hearing the bass drop out?"),
            ),
            message("u1", "Ada", "Good, not just me then.", now - 30_000),
        )
        // The same conversation with the quote dropped from the third message.
        // Everything else is identical, so the difference in ink is the quote.
        val plain = quoted.mapIndexed { index, m ->
            if (index == 2) m.copy(replyTo = null) else m
        }

        val emptyInk = ink(render("empty", emptyList()))
        println("== empty   ink=$emptyInk")
        if (emptyInk <= 0) failures += "an empty conversation painted nothing (no empty state)"

        val plainInk = ink(render("messages", plain))
        println("== messages (no quote)  ink=$plainInk")
        if (plainInk <= 0) failures += "a conversation with messages painted nothing"

        val quotedInk = ink(render("quoted", quoted))
        println("== quoted   ink=$quotedInk")
        if (quotedInk <= plainInk) {
            failures += "the quoted reply drew nothing: ink $quotedInk is not more than $plainInk without it"
        }

        if (failures.isEmpty()) {
            println()
            println("OK: the chat list draws, and a quoted reply draws more than the same message without it")
            return
        }
        println()
        println("FAILED:")
        failures.forEach { println("  - $it") }
        throw IllegalStateException("chat window render check failed (${failures.size} case(s))")
    }

    private fun message(
        userId: String,
        username: String,
        text: String,
        timestamp: Long,
        replyTo: LtRepliedMessage? = null,
    ) = LtChatMessage(userId, username, text, timestamp, replyTo)

    private fun render(name: String, chat: List<LtChatMessage>): ByteArray {
        val scene = ImageComposeScene(WIDTH, HEIGHT) {
            AppTheme(mode = ThemeMode.LIGHT, accent = Color(0xFFED5564)) {
                ListenTogetherChatList(
                    chat = chat,
                    myUserId = "me",
                    language = "en",
                    onReply = {},
                    modifier = Modifier.fillMaxSize(),
                )
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

    private fun ink(png: ByteArray): Int {
        val image = javax.imageio.ImageIO.read(png.inputStream())
        var ink = 0
        for (y in 0 until image.height) {
            for (x in 0 until image.width) {
                val rgb = image.getRGB(x, y)
                val r = (rgb shr 16) and 255
                val g = (rgb shr 8) and 255
                val b = rgb and 255
                if (r > 245 && g > 245 && b > 245) continue
                ink++
            }
        }
        return ink
    }
}
