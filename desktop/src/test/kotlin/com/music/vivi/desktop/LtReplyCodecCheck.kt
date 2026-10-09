package com.music.vivi.desktop

/**
 * Headless check of the Listen Together reply envelope, run with
 *
 *     ./gradlew :desktop:ltReplyCodecCheck
 *
 * The relay carries no field for a quote: the Android client embeds it in the
 * message text as `<ZWSP>[RPLY:<base64(author|text)>]<ZWSP>` and strips it again
 * on receive. A reply written here without that exact envelope is what made the
 * phone's replies show up as the literal `[RPLY:…]` text and this client's
 * replies arrive as plain messages, so the contract is checked against the
 * mobile's own byte layout rather than against itself:
 *
 *  * what [LtReplyCodec.encode] writes must start with `\u200B[RPLY:`, end the
 *    metadata with `]\u200B`, and carry a base64 body that decodes to
 *    `author|message` (the mobile's `Base64.decode` step),
 *  * feeding the encoder's own output back through [LtReplyCodec.decode] must
 *    return the original text plus the quote (this is a phone message arriving
 *    here, and a message written here arriving back in the same shape),
 *  * a message that merely *looks* like an envelope (no closing marker) or a
 *    body that is not base64 must be returned unchanged, never dropped.
 */
object LtReplyCodecCheck {

    private val ZWSP = "\u200B"

    @JvmStatic
    fun main(args: Array<String>) {
        val failures = mutableListOf<String>()

        fun check(name: String, ok: Boolean) {
            println(if (ok) "OK   $name" else "FAIL $name")
            if (!ok) failures += name
        }

        val quote = LtRepliedMessage("pbon2024", "Vivi Official")
        val text = "ciao, come va?"

        // ---- what we put on the wire matches the mobile's layout -------------
        val encoded = LtReplyCodec.encode(text, quote)
        check("starts with the zero-width-space marker", encoded.startsWith("$ZWSP[RPLY:"))
        val end = encoded.indexOf("]$ZWSP")
        check("closes the metadata before the text", end > 0)
        check("the text follows the closing marker", encoded.substring(end + 2) == text)
        val body = encoded.substring("$ZWSP[RPLY:".length, end)
        val decodedBody = runCatching {
            String(java.util.Base64.getDecoder().decode(body), Charsets.UTF_8)
        }.getOrNull()
        check("the body is base64 of 'author|message'", decodedBody == "pbon2024|Vivi Official")

        // ---- what arrives from a phone is read back correctly -----------------
        val fromPhone = LtReplyCodec.decode(encoded, null)
        check("the reply text is stripped off", fromPhone.message == text)
        check("the quote is recovered", fromPhone.replyTo == quote)
        check("nothing is left of the marker", !fromPhone.message.contains("RPLY"))

        // A message that also carried the JSON field keeps the quote it embeds.
        val withField = LtReplyCodec.decode(encoded, LtRepliedMessage("lele", "vecchio"))
        check("the embedded quote wins over the field", withField.replyTo == quote)

        // ---- a plain message is untouched ------------------------------------
        val plain = LtReplyCodec.decode("buonasera", null)
        check("a plain message is unchanged", plain.message == "buonasera" && plain.replyTo == null)
        check("encode with no quote is a pass-through", LtReplyCodec.encode("buonasera", null) == "buonasera")

        // ---- a broken envelope must never swallow the message ----------------
        val unclosed = "$ZWSP[RPLY:notclosed"
        check("an unclosed envelope is kept verbatim", LtReplyCodec.decode(unclosed, null).message == unclosed)
        val badBase64 = "$ZWSP[RPLY:***not base64***]$ZWSP$text"
        check("a bad body is kept verbatim", LtReplyCodec.decode(badBase64, null).message == badBase64)
        val noSeparator = "$ZWSP[RPLY:${java.util.Base64.getEncoder().encodeToString("no-separator".toByteArray())}]$ZWSP$text"
        check("a body without '|' is kept verbatim", LtReplyCodec.decode(noSeparator, null).message == noSeparator)

        if (failures.isEmpty()) {
            println("OK: the reply envelope matches the mobile wire format and survives a round trip")
        } else {
            println("FAILED: ${failures.size} check(s): ${failures.joinToString()}")
            kotlin.system.exitProcess(1)
        }
    }
}
