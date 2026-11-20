package com.music.vivi.desktop.data.youtube

/**
 * Keeps cookie values out of the diagnostic logs.
 *
 * `youtube.log` and `login-debug.log` are meant to answer "which client
 * answered what", not to hold a session: the sign-in window already logs cookie
 * *names*, and this is the barrier that makes a future message quoting a header
 * harmless. It masks any `NAME=value` / `NAME: value` for the YouTube/Google
 * session cookie names, any `Cookie:`/`Authorization:` line and any long opaque
 * token, so a value cannot be written by accident.
 */
internal object SecretRedactor {

    private const val MASK = "<redacted>"

    private val names = listOf(
        "SID", "HSID", "SSID", "APISID", "SAPISID", "SIDCC", "LSID", "OSID",
        "__Secure-1PSID", "__Secure-3PSID",
        "__Secure-1PAPISID", "__Secure-3PAPISID",
        "__Secure-1PSIDTS", "__Secure-3PSIDTS",
        "__Secure-1PSIDCC", "__Secure-3PSIDCC",
        "LOGIN_INFO", "AEC", "NID", "SIDTS",
    )

    /** `NAME=<value>` or `NAME: <value>`, quoted or bare. */
    private val assignment = Regex(
        "(?i)\\b(${names.joinToString("|") { Regex.escape(it) }})\\s*[=:]\\s*(?:\"[^\"]*\"|[^\\s;,]+)"
    )

    /** A whole header, whose value can be a list of cookies. */
    private val header = Regex("(?i)\\b(cookie|set-cookie|authorization)\\s*[:=]\\s*[^\\r\\n]+")

    /** The hash taken from the APISID token. */
    private val sapisidHash = Regex("(?i)\\bSAPISIDHASH\\s+\\S+")

    /** Session values are long opaque runs; request ids and titles are not. */
    private val longToken = Regex("\\b[A-Za-z0-9_\\-+/=]{40,}\\b")

    fun redact(message: String): String = message
        .replace(header) { "${it.groupValues[1]}=$MASK" }
        .replace(assignment) { "${it.groupValues[1]}=$MASK" }
        .replace(sapisidHash, "SAPISIDHASH $MASK")
        .replace(longToken, MASK)
}
