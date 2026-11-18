package com.music.vivi.desktop.data.youtube

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths
import java.nio.file.StandardCopyOption
import java.util.Base64
import javax.crypto.Cipher
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.SecretKeySpec

/**
 * The YouTube session a browser on this machine already holds.
 *
 * This is BitChord's cookie reader reduced to what a desktop needs: Firefox's
 * plain `cookies.sqlite` and Chromium's DPAPI-protected store. A browser is only
 * a source of the `Cookie` header (SAPISID and friends); once read, the session
 * lives in [YouTubeAccount], so no browser is needed while the app runs.
 */
object BrowserCookies {

    enum class Family { FIREFOX, CHROMIUM }

    /** One browser profile that could hold a signed-in YouTube session. */
    data class Profile(
        val browser: String,
        val profile: String,
        val family: Family,
        val database: Path,
        val localState: Path? = null
    ) {
        val label: String get() = if (profile.isBlank()) browser else "$browser · $profile"
    }

    /** What a profile turned out to hold. */
    sealed interface Result {
        /** A cookie jar with a signing secret in it, so a usable session. */
        data class Session(val cookie: String) : Result

        /** The store was read and there is simply nobody signed in there. */
        data object SignedOut : Result

        /** The store could not be read, and why. */
        data class Unavailable(val reason: String) : Result
    }

    private val home: Path get() = Paths.get(System.getProperty("user.home"))

    private val isWindows: Boolean
        get() = System.getProperty("os.name").orEmpty().startsWith("Windows", ignoreCase = true)

    /** Every browser profile on this machine that might hold a session. */
    fun profiles(): List<Profile> = buildList {
        firefoxRoots().forEach { (name, root) ->
            if (!Files.isDirectory(root)) return@forEach
            runCatching {
                Files.list(root).use { entries ->
                    entries.filter { Files.isRegularFile(it.resolve(FIREFOX_COOKIES)) }
                        .forEach { profile ->
                            add(
                                Profile(
                                    browser = name,
                                    profile = profile.fileName.toString(),
                                    family = Family.FIREFOX,
                                    database = profile.resolve(FIREFOX_COOKIES)
                                )
                            )
                        }
                }
            }
        }
        chromiumRoots().forEach { (name, root) ->
            if (!Files.isDirectory(root)) return@forEach
            runCatching {
                val localState = root.resolve(LOCAL_STATE).takeIf(Files::isRegularFile)
                Files.list(root).use { entries ->
                    entries.forEach { profile ->
                        val id = profile.fileName.toString()
                        if (id in HIDDEN_PROFILES) return@forEach
                        val database = chromiumCookieStore(profile) ?: return@forEach
                        add(
                            Profile(
                                browser = name,
                                profile = id,
                                family = Family.CHROMIUM,
                                database = database,
                                localState = localState
                            )
                        )
                    }
                }
            }
        }
    }.sortedBy { it.label }

    /** Reads [profile]'s YouTube cookies as a `Cookie` header value. */
    fun read(profile: Profile): Result {
        if (!Files.isReadable(profile.database)) return Result.Unavailable("its cookie store cannot be read")
        val copy = runCatching {
            Files.createTempFile("vivi-cookies", ".sqlite").also {
                Files.copy(profile.database, it, StandardCopyOption.REPLACE_EXISTING)
            }
        }.getOrElse { return Result.Unavailable("its cookie store could not be copied: ${it.message}") }
        return try {
            val jar = when (profile.family) {
                Family.FIREFOX -> readFirefox(copy)
                Family.CHROMIUM -> readChromium(copy, profile)
            }
            when {
                jar is Result.Unavailable -> jar
                jar is Result.Session && !hasSigningSecret(jar.cookie) -> Result.SignedOut
                else -> jar
            }
        } finally {
            runCatching { Files.deleteIfExists(copy) }
        }
    }

    /** Whether a jar can actually sign a request. */
    fun hasSigningSecret(cookie: String): Boolean = cookie.split(';').any { entry ->
        entry.substringBefore('=').trim() in SIGNING_COOKIES &&
            entry.substringAfter('=', "").trim().isNotEmpty()
    }

    private fun readFirefox(database: Path): Result = query(database) { connection ->
        val cookies = LinkedHashMap<String, String>()
        connection.prepareStatement(
            "SELECT name, value FROM moz_cookies WHERE host LIKE '%youtube.com'"
        ).use { statement ->
            statement.executeQuery().use { rows ->
                while (rows.next()) {
                    val name = rows.getString(1) ?: continue
                    val value = rows.getString(2) ?: continue
                    if (value.isNotBlank()) cookies[name] = value
                }
            }
        }
        Result.Session(cookies.entries.joinToString("; ") { "${it.key}=${it.value}" })
    }

    private fun readChromium(database: Path, profile: Profile): Result {
        if (!isWindows) return Result.Unavailable("Chromium's cookie store is supported on Windows here")
        val masterKey = windowsMasterKey(profile.localState)
        return query(database) { connection ->
            val cookies = LinkedHashMap<String, String>()
            var undecipherable = 0
            var appBoundSigningCookie = false
            connection.prepareStatement(
                "SELECT host_key, name, value, encrypted_value FROM cookies WHERE host_key LIKE '%youtube.com'"
            ).use { statement ->
                statement.executeQuery().use { rows ->
                    while (rows.next()) {
                        val host = rows.getString(1).orEmpty()
                        val name = rows.getString(2) ?: continue
                        val plain = rows.getString(3)
                        val sealed = rows.getBytes(4)
                        val value = when {
                            !plain.isNullOrBlank() -> plain
                            sealed == null || sealed.isEmpty() -> null
                            else -> decryptWindows(sealed, masterKey, host).also {
                                if (it == null) undecipherable++
                                if (name in SIGNING_COOKIES && sealed.hasPrefix(APP_BOUND)) {
                                    appBoundSigningCookie = true
                                }
                            }
                        }
                        if (!value.isNullOrBlank()) cookies[name] = value
                    }
                }
            }
            val jar = cookies.entries.joinToString("; ") { "${it.key}=${it.value}" }
            when {
                !hasSigningSecret(jar) && appBoundSigningCookie -> Result.Unavailable(
                    "its YouTube sign-in is protected by Windows App-Bound Encryption. " +
                        "Use a signed-in Firefox profile"
                )
                cookies.isEmpty() && undecipherable > 0 ->
                    Result.Unavailable("its cookies are encrypted with a key this session cannot reach")
                else -> Result.Session(jar)
            }
        }
    }

    /**
     * Chromium's cookie encryption on Windows: a `v10`/`v11` value is AES-GCM
     * under the browser key, which is itself DPAPI-wrapped in Local State; an
     * older value is plain DPAPI.
     */
    private fun decryptWindows(sealed: ByteArray, masterKey: ByteArray?, host: String): String? {
        if (!sealed.hasPrefix(CHROMIUM_V10) && !sealed.hasPrefix(CHROMIUM_V11)) {
            return runCatching {
                stripHostDigest(com.sun.jna.platform.win32.Crypt32Util.cryptUnprotectData(sealed), host)
                    .toString(Charsets.UTF_8)
            }.getOrNull()
        }
        if (masterKey == null || sealed.size <= PREFIX_BYTES + GCM_NONCE_BYTES + GCM_TAG_BYTES) return null
        return runCatching {
            val nonceOffset = PREFIX_BYTES
            val bodyOffset = nonceOffset + GCM_NONCE_BYTES
            val nonce = sealed.copyOfRange(nonceOffset, bodyOffset)
            val body = sealed.copyOfRange(bodyOffset, sealed.size)
            val plain = Cipher.getInstance("AES/GCM/NoPadding").run {
                init(Cipher.DECRYPT_MODE, SecretKeySpec(masterKey, "AES"), GCMParameterSpec(GCM_TAG_BITS, nonce))
                doFinal(body)
            }
            stripHostDigest(plain, host).toString(Charsets.UTF_8)
        }.getOrNull()
    }

    /** Cookie databases at schema 24+ bind encrypted values to their host with this digest. */
    private fun stripHostDigest(plain: ByteArray, host: String): ByteArray {
        if (host.isBlank()) return plain
        val hashed = java.security.MessageDigest.getInstance("SHA-256")
            .digest(host.toByteArray(Charsets.UTF_8))
        return if (plain.size > hashed.size && plain.copyOf(hashed.size).contentEquals(hashed)) {
            plain.copyOfRange(hashed.size, plain.size)
        } else {
            plain
        }
    }

    /** Reads and unwraps the per-browser Windows key from Chromium's Local State file. */
    private fun windowsMasterKey(localState: Path?): ByteArray? = runCatching {
        val state = localState?.takeIf(Files::isReadable)?.let(Files::readString) ?: return null
        val encoded = Json.parseToJsonElement(state).jsonObject["os_crypt"]
            ?.jsonObject
            ?.get("encrypted_key")
            ?.jsonPrimitive
            ?.content
            ?: return null
        val wrapped = Base64.getDecoder().decode(encoded)
        if (!wrapped.hasPrefix(DPAPI_PREFIX)) return null
        com.sun.jna.platform.win32.Crypt32Util.cryptUnprotectData(
            wrapped.copyOfRange(DPAPI_PREFIX.size, wrapped.size)
        )
    }.getOrNull()

    private fun query(database: Path, block: (java.sql.Connection) -> Result): Result = runCatching {
        runCatching { Class.forName("org.sqlite.JDBC") }
        java.sql.DriverManager.getConnection("jdbc:sqlite:${database.toAbsolutePath()}").use(block)
    }.getOrElse { Result.Unavailable("its cookie store could not be opened: ${it.message}") }

    private fun firefoxRoots(): List<Pair<String, Path>> = if (isWindows) {
        val roaming = System.getenv("APPDATA")?.let(Paths::get) ?: home.resolve("AppData/Roaming")
        listOf(
            "Firefox" to roaming.resolve("Mozilla/Firefox/Profiles"),
            "LibreWolf" to roaming.resolve("librewolf/Profiles"),
            "Waterfox" to roaming.resolve("Waterfox/Profiles"),
            "Zen" to roaming.resolve("zen/Profiles")
        )
    } else {
        listOf("Firefox" to home.resolve(".mozilla/firefox"))
    }

    private fun chromiumRoots(): List<Pair<String, Path>> = if (isWindows) {
        val local = System.getenv("LOCALAPPDATA")?.let(Paths::get) ?: home.resolve("AppData/Local")
        listOf(
            "Chrome" to local.resolve("Google/Chrome/User Data"),
            "Chromium" to local.resolve("Chromium/User Data"),
            "Edge" to local.resolve("Microsoft/Edge/User Data"),
            "Brave" to local.resolve("BraveSoftware/Brave-Browser/User Data"),
            "Vivaldi" to local.resolve("Vivaldi/User Data")
        )
    } else {
        emptyList()
    }

    private fun chromiumCookieStore(profile: Path): Path? =
        profile.resolve(NETWORK).resolve(CHROMIUM_COOKIES).takeIf(Files::isRegularFile)
            ?: profile.resolve(CHROMIUM_COOKIES).takeIf(Files::isRegularFile)

    private fun ByteArray.hasPrefix(prefix: ByteArray): Boolean =
        size >= prefix.size && indices.take(prefix.size).all { this[it] == prefix[it] }

    private val SIGNING_COOKIES = setOf("SAPISID", "__Secure-3PAPISID", "__Secure-1PAPISID")

    private const val FIREFOX_COOKIES = "cookies.sqlite"
    private const val CHROMIUM_COOKIES = "Cookies"
    private const val NETWORK = "Network"
    private const val LOCAL_STATE = "Local State"
    private const val PREFIX_BYTES = 3
    private const val GCM_NONCE_BYTES = 12
    private const val GCM_TAG_BYTES = 16
    private const val GCM_TAG_BITS = GCM_TAG_BYTES * 8

    private val CHROMIUM_V10 = "v10".toByteArray(Charsets.US_ASCII)
    private val CHROMIUM_V11 = "v11".toByteArray(Charsets.US_ASCII)
    private val APP_BOUND = "v20".toByteArray(Charsets.US_ASCII)
    private val DPAPI_PREFIX = "DPAPI".toByteArray(Charsets.US_ASCII)
    private val HIDDEN_PROFILES = setOf("Guest Profile", "System Profile")
}
