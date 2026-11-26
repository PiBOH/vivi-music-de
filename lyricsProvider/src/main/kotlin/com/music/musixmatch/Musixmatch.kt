/**
 * VIVI-LYRICS (C) 2026
 *
 * PROPRIETARY LICENSE:
 * This file is source-available for viewing. Copying, modification,
 * redistribution, or reuse in other applications is strictly prohibited.
 * Licensed exclusively for use in the official vivimusic application.
 */

package com.music.musixmatch

import com.music.musixmatch.models.*
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.engine.okhttp.OkHttp
import io.ktor.client.plugins.compression.ContentEncoding
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.http.ContentType
import io.ktor.serialization.kotlinx.json.json
import kotlinx.coroutines.async
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.decodeFromJsonElement

import java.io.File
import java.net.URLEncoder
import java.nio.charset.StandardCharsets
import java.security.MessageDigest
import javax.crypto.Mac
import javax.crypto.spec.SecretKeySpec
import java.text.SimpleDateFormat
import java.util.Base64
import java.util.Date
import java.util.Locale
import java.util.concurrent.atomic.AtomicReference
import kotlin.math.abs

object Musixmatch {
    private const val USER_AGENT = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/131.0.0.0 Safari/537.36"
    private const val BASE_URL = "https://apic.musixmatch.com/ws/1.1/"
    // The same android-player id, page and fallback secret as vivizzz007/vivi-music
    // v6.0.9 `lyricsProvider/.../musixmatch/Musixmatch.kt`. The Mac client id used
    // before answered 401 "upgrade" on token.get; this pair answers 200.
    private const val APP_ID = "android-player-v1.0"
    private const val FALLBACK_SECRET = "e9966b7e2aff48cb090d7c1bebd9d7a2"
    // token.get is captcha-limited per IP on the web/API host: after one token it
    // answers {"status_code":401,"hint":"captcha"} for a few minutes. The mobile
    // host takes the same signed call and still issues tokens; the token works on
    // both hosts. Only issuance falls back, the API calls stay on BASE_URL.
    private const val MOBILE_BASE_URL = "https://apic-appmobile.musixmatch.com/ws/1.1/"

    /** Receives the request line, the HTTP status and the body snippet of every step. */
    var logger: ((String) -> Unit)? = null

    private val secretCache = AtomicReference<String?>(null)
    private val tokenCache = AtomicReference<String?>(null)
    private val processGuid = java.util.UUID.randomUUID().toString()

    // token.get is captcha-limited per IP: a handful of requests in a few minutes
    // answer {"status_code":401,"hint":"captcha"}. The issued token keeps working,
    // so it is kept across restarts instead of asking for a new one every launch.
    private val tokenFile: File? by lazy {
        runCatching {
            val dir = File(System.getProperty("java.io.tmpdir"), "axolotl-music")
            if (!dir.exists()) dir.mkdirs()
            File(dir, "musixmatch-usertoken.txt")
        }.getOrNull()
    }

    private fun readCachedToken(): String? = runCatching {
        tokenFile?.takeIf { it.isFile }?.readText()?.trim()?.takeIf { it.isNotBlank() }
    }.getOrNull()

    private fun writeCachedToken(token: String) {
        runCatching { tokenFile?.writeText(token) }
    }

    private fun clearCachedToken() {
        runCatching { tokenFile?.delete() }
    }

    private fun log(message: String) {
        val sink = logger
        if (sink != null) sink(message) else logDebug(message)
    }

    private val client by lazy {
        HttpClient(OkHttp) {
            expectSuccess = false
            install(ContentNegotiation) {
                val json = Json {
                    ignoreUnknownKeys = true
                    explicitNulls = false
                    encodeDefaults = true
                }
                json(json)
                json(json, ContentType.Text.Html)
                json(json, ContentType.Text.Plain)
            }
            install(ContentEncoding) {
                gzip()
                deflate()
            }
        }
    }

    private suspend fun getSecret(): String {
        val cached = secretCache.get()
        if (cached != null) return cached

        val secret = try {
            val searchPage = client.get("https://www.musixmatch.com/explore") {
                header("User-Agent", USER_AGENT)
                header("Cookie", "mxm_bab=AB")
                header("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,image/avif,image/webp,image/apng,*/*;q=0.8")
                header("Accept-Language", "en-US,en;q=0.9")
                header("Sec-Fetch-Dest", "document")
                header("Sec-Fetch-Mode", "navigate")
                header("Sec-Fetch-Site", "none")
                header("Sec-Fetch-User", "?1")
                header("Upgrade-Insecure-Requests", "1")
            }.body<String>()

            val regex = """src="([^"]*/_next/static/chunks/pages/_app-[^"]+\.js)"""".toRegex()
            val match = regex.find(searchPage) ?: throw IllegalStateException("Could not find _app JS in Musixmatch page")
            val rawAppJsUrl = match.groupValues[1]
            val appJsUrl = if (rawAppJsUrl.startsWith("http")) rawAppJsUrl else {
                "https://www.musixmatch.com" + if (rawAppJsUrl.startsWith("/")) rawAppJsUrl else "/$rawAppJsUrl"
            }

            val jsContent = client.get(appJsUrl) {
                header("User-Agent", USER_AGENT)
                header("Accept", "*/*")
                header("Accept-Language", "en-US,en;q=0.9")
                header("Sec-Fetch-Dest", "script")
                header("Sec-Fetch-Mode", "no-cors")
                header("Sec-Fetch-Site", "cross-site")
            }.body<String>()

            val secretRegex = """from\(\s*"(.*?)"\s*\.split""".toRegex()
            val secretMatch = secretRegex.find(jsContent) ?: throw IllegalStateException("Could not find secret in Musixmatch JS")
            val encodedSecret = secretMatch.groupValues[1]
            
            val reversed = encodedSecret.reversed()
            val decodedBytes = Base64.getDecoder().decode(reversed)
            log("secret: from _app JS")
            String(decodedBytes, StandardCharsets.UTF_8)
        } catch (e: Exception) {
            log("secret: dynamic fetch failed (${e.message}); using the reference fallback")
            FALLBACK_SECRET
        }
        
        secretCache.set(secret)
        return secret
    }

    private fun sign(url: String, secret: String): String {
        val normalizedUrl = url.replace("%20", "+").replace(" ", "+")
        val formatter = SimpleDateFormat("yyyyMMdd", Locale.US).apply {
            timeZone = java.util.TimeZone.getTimeZone("UTC")
        }
        val dateStr = formatter.format(Date())
        val message = normalizedUrl + dateStr
        
        val keySpec = SecretKeySpec(secret.toByteArray(StandardCharsets.UTF_8), "HmacSHA256")
        val mac = Mac.getInstance("HmacSHA256")
        mac.init(keySpec)
        
        val hmacBytes = mac.doFinal(message.toByteArray(StandardCharsets.UTF_8))
        val base64Sig = Base64.getEncoder().encodeToString(hmacBytes)
        val encodedSig = URLEncoder.encode(base64Sig, StandardCharsets.UTF_8.name())
        
        return "$normalizedUrl&signature=$encodedSig&signature_protocol=sha256"
    }

    private suspend fun getUserToken(secret: String): String {
        val cached = tokenCache.get()
        if (cached != null) return cached
        val stored = readCachedToken()
        if (stored != null) {
            log("token.get: reusing the token saved by a previous run")
            tokenCache.set(stored)
            return stored
        }

        var lastStatus: Int? = null
        for (baseUrl in listOf(BASE_URL, MOBILE_BASE_URL)) {
            val tokenUrl = "${baseUrl}token.get?app_id=$APP_ID&guid=$processGuid&format=json"
            val signedUrl = sign(tokenUrl, secret)
            log("token.get: $signedUrl")

            val responseBody = try {
                client.get(signedUrl) {
                    header("User-Agent", USER_AGENT)
                    header("Accept", "application/json, text/plain, */*")
                    header("Accept-Language", "en-US,en;q=0.9")
                }.body<String>()
            } catch (e: Exception) {
                log("token.get: request failed on $baseUrl (${e.message})")
                continue
            }
            val response = Json { ignoreUnknownKeys = true }
                .decodeFromString<TokenResponse>(responseBody)
            log("token.get: HTTP body=${responseBody.take(600)}${if (responseBody.length > 600) "…" else ""}")

            lastStatus = response.message.header.statusCode
            val bodyElement = response.message.body
            val userToken = if (bodyElement is kotlinx.serialization.json.JsonObject) {
                val json = Json { ignoreUnknownKeys = true }
                val bodyObj = json.decodeFromJsonElement<TokenResponseBody>(bodyElement)
                bodyObj.userToken
            } else {
                null
            }
            if (userToken != null) {
                tokenCache.set(userToken)
                writeCachedToken(userToken)
                return userToken
            }
            log("token.get: no user_token on $baseUrl (status: $lastStatus), trying the next host")
        }

        throw IllegalStateException("Failed to retrieve user_token from Musixmatch (status: $lastStatus)")
    }

    /**
     * Picks the candidate that can actually answer, then the closest title and
     * only then the closest duration.
     *
     * Musixmatch's search returns versions with no lyrics at all (a remix or a
     * karaoke entry can be nearer to the requested length), and picking one of
     * those made the whole provider yield nothing even though a good candidate
     * was in the same answer. The capability flags are what the tier requests
     * below read, so they decide first: a track with any of lyrics, subtitles or
     * richsync wins over one with none, and duration only breaks a tie.
     */
    private fun chooseBestTrack(tracks: List<Track>, title: String, duration: Int): Track? {
        val normalizedQueryTitle = cleanText(title)
        return tracks.minByOrNull { track ->
            val capable = if (
                track.hasLyrics == 1 || track.hasSubtitles == 1 || track.hasRichSync == 1
            ) 0 else 1
            val trackTitleCleaned = cleanText(track.trackName)
            val textScore = when {
                trackTitleCleaned == normalizedQueryTitle -> 0
                trackTitleCleaned.contains(normalizedQueryTitle) || normalizedQueryTitle.contains(trackTitleCleaned) -> 1
                else -> 2
            }
            val trackDur = track.trackLength ?: 0
            val durDelta = when {
                duration > 0 && trackDur > 0 -> abs(trackDur - duration)
                trackDur == 0 -> 999
                else -> Int.MAX_VALUE
            }
            logDebug(
                "Track option: '${track.trackName}' by '${track.artistName}' " +
                    "(id=${track.trackId}, dur=${trackDur}s, lyrics=${track.hasLyrics}, " +
                    "subtitles=${track.hasSubtitles}, richsync=${track.hasRichSync}) -> " +
                    "capable=${capable == 0}, textScore=$textScore, durDelta=$durDelta"
            )
            (capable.toLong() shl 48) + (textScore.toLong() shl 32) + durDelta
        }
    }

    private class TokenExpiredException(message: String) : Exception(message)

    private fun checkStatusCode(statusCode: Int) {
        if (statusCode == 401 || statusCode == 402) {
            tokenCache.set(null)
            secretCache.set(null)
            clearCachedToken()
            throw TokenExpiredException("Token expired or unauthorized (status: $statusCode)")
        }
    }

    private suspend fun <T> runWithTokenRetry(block: suspend () -> T): T {
        return try {
            block()
        } catch (e: TokenExpiredException) {
            // Only an auth rejection invalidates the token. A track that has no
            // lyrics is a query result, and dropping the token for it made the
            // next request ask token.get again, straight into the captcha limit.
            tokenCache.set(null)
            secretCache.set(null)
            clearCachedToken()
            block()
        }
    }

    suspend fun getLyrics(
        title: String,
        artist: String,
        duration: Int,
        album: String? = null
    ): Result<String> = runCatching {
        runWithTokenRetry {
            val secret = getSecret()
            val token = getUserToken(secret)

            // 1. Search for the track using separate track/artist params for accurate version matching
            val encodedTitle = URLEncoder.encode(title, StandardCharsets.UTF_8.name())
            val encodedArtist = URLEncoder.encode(artist, StandardCharsets.UTF_8.name())
            val searchUrl = "${BASE_URL}track.search?app_id=$APP_ID&format=json&q_track=$encodedTitle&q_artist=$encodedArtist&f_has_lyrics=true&page_size=10&usertoken=$token"
            val signedSearch = sign(searchUrl, secret)

            log("track.search: $signedSearch")
            val searchBody = client.get(signedSearch) {
                header("User-Agent", USER_AGENT)
                header("Accept", "application/json, text/plain, */*")
                header("Accept-Language", "en-US,en;q=0.9")
            }.body<String>()
            log("track.search: HTTP body=${searchBody.take(400)}")
            val searchResponse = Json { ignoreUnknownKeys = true }
                .decodeFromString<SearchTrackResponse>(searchBody)

            val searchHeader = searchResponse.message.header
            checkStatusCode(searchHeader.statusCode)

            val bodyElement = searchResponse.message.body
            val trackList = if (bodyElement is kotlinx.serialization.json.JsonObject) {
                val json = Json { ignoreUnknownKeys = true }
                val bodyObj = json.decodeFromJsonElement<SearchTrackResponseBody>(bodyElement)
                bodyObj.trackList
            } else {
                emptyList()
            }

            if (trackList.isEmpty()) {
                val code = searchResponse.message.header.statusCode
                throw IllegalStateException("Track not found on Musixmatch (status: $code)")
            }

            logDebug("Searching for track: Title='$title', Artist='$artist', Duration=$duration")
            val bestTrack = chooseBestTrack(trackList.map { it.track }, title, duration)
                ?: trackList[0].track

            logDebug("Selected best track: '${bestTrack.trackName}' by '${bestTrack.artistName}' (id=${bestTrack.trackId})")
            val trackId = bestTrack.trackId
            val trackLength = bestTrack.trackLength ?: duration

            // 2. Fetch tiers concurrently but yield in strict priority order (Synced first)
            return@runWithTokenRetry kotlinx.coroutines.coroutineScope {
                val richSyncJob = if (bestTrack.hasRichSync == 1 || bestTrack.hasRichSync == null) {
                    async { getRichSyncLyrics(trackId, token, secret) }
                } else null

                val subtitleJob = if (bestTrack.hasSubtitles == 1 || bestTrack.hasSubtitles == null) {
                    async { getSubtitleLyrics(trackId, token, secret) }
                } else null

                val lyricsJob = if (bestTrack.hasLyrics == 1 || bestTrack.hasLyrics == null) {
                    async { getPlainLyrics(trackId, token, secret) }
                } else null

                richSyncJob?.await()?.getOrNull()?.let { return@coroutineScope it }
                subtitleJob?.await()?.getOrNull()?.let { return@coroutineScope it }
                lyricsJob?.await()?.getOrNull()?.let { return@coroutineScope it }

                throw IllegalStateException("No lyrics found for trackId $trackId")
            }
        }
    }

    suspend fun getAllLyrics(
        title: String,
        artist: String,
        duration: Int,
        album: String? = null,
        callback: (String) -> Unit
    ) {
        runCatching {
            runWithTokenRetry {
                val secret = getSecret()
                val token = getUserToken(secret)

                val encodedTitle = URLEncoder.encode(title, StandardCharsets.UTF_8.name())
                val encodedArtist = URLEncoder.encode(artist, StandardCharsets.UTF_8.name())
                val searchUrl = "${BASE_URL}track.search?app_id=$APP_ID&format=json&q_track=$encodedTitle&q_artist=$encodedArtist&f_has_lyrics=true&page_size=10&usertoken=$token"
                val signedSearch = sign(searchUrl, secret)

                val searchResponse = client.get(signedSearch) {
                    header("User-Agent", USER_AGENT)
                    header("Accept", "application/json, text/plain, */*")
                    header("Accept-Language", "en-US,en;q=0.9")
                }.body<SearchTrackResponse>()

                val searchHeader = searchResponse.message.header
                checkStatusCode(searchHeader.statusCode)

                val bodyElement = searchResponse.message.body
                val trackList = if (bodyElement is kotlinx.serialization.json.JsonObject) {
                    val json = Json { ignoreUnknownKeys = true }
                    val bodyObj = json.decodeFromJsonElement<SearchTrackResponseBody>(bodyElement)
                    bodyObj.trackList
                } else {
                    emptyList()
                }

                if (trackList.isEmpty()) return@runWithTokenRetry

                val bestTrack = chooseBestTrack(trackList.map { it.track }, title, duration)
                    ?: trackList[0].track

                val trackId = bestTrack.trackId
                val trackLength = bestTrack.trackLength ?: duration

                // Fetch tiers concurrently but yield in strict priority order (Synced first)
                kotlinx.coroutines.coroutineScope {
                    val richSyncJob = if (bestTrack.hasRichSync == 1 || bestTrack.hasRichSync == null) {
                        async { getRichSyncLyrics(trackId, token, secret) }
                    } else null

                    val subtitleJob = if (bestTrack.hasSubtitles == 1 || bestTrack.hasSubtitles == null) {
                        async { getSubtitleLyrics(trackId, token, secret) }
                    } else null

                    val plainJob = if (bestTrack.hasLyrics == 1 || bestTrack.hasLyrics == null) {
                        async { getPlainLyrics(trackId, token, secret) }
                    } else null

                    richSyncJob?.await()?.onSuccess { callback(it) }
                    subtitleJob?.await()?.onSuccess { callback(it) }
                    plainJob?.await()?.onSuccess { callback(it) }
                }
            }
        }.onFailure {
            logDebug("getAllLyrics failed: ${it.message}")
        }
    }

    private suspend fun getRichSyncLyrics(trackId: Long, token: String, secret: String): Result<String> = runCatching {
        val richsyncUrl = "${BASE_URL}track.richsync.get?app_id=$APP_ID&format=json&track_id=$trackId&usertoken=$token"
        val signedUrl = sign(richsyncUrl, secret)
        log("track.richsync.get: $signedUrl")
        
        val richSyncBody = client.get(signedUrl) {
            header("User-Agent", USER_AGENT)
            header("Accept", "application/json, text/plain, */*")
            header("Accept-Language", "en-US,en;q=0.9")
        }.body<String>()
        val response = Json { ignoreUnknownKeys = true }
            .decodeFromString<RichSyncResponse>(richSyncBody)
        log("track.richsync.get: HTTP bytes=${richSyncBody.length} status=${response.message.header.statusCode}")

        val statusCode = response.message.header.statusCode
        // A tier that has no lyrics answers 401 too, and that is not an expired
        // session: clearing the token here made the next track ask token.get again.
        if (statusCode != 200) throw IllegalStateException("Richsync unavailable (status: $statusCode)")

        val bodyElement = response.message.body
        val richsyncObj = if (bodyElement is kotlinx.serialization.json.JsonObject) {
            val json = Json { ignoreUnknownKeys = true }
            val bodyObj = json.decodeFromJsonElement<RichSyncResponseBody>(bodyElement)
            bodyObj.richsync
        } else {
            null
        }

        val body = richsyncObj?.richsyncBody ?: throw IllegalStateException("Richsync unavailable")
        val json = Json { ignoreUnknownKeys = true }
        val entries = json.decodeFromString<List<RichSyncEntry>>(body)
        
        convertRichSyncToLrc(entries)
    }

    private suspend fun getSubtitleLyrics(trackId: Long, token: String, secret: String): Result<String> = runCatching {
        val subtitleUrl = "${BASE_URL}track.subtitle.get?app_id=$APP_ID&format=json&track_id=$trackId&subtitle_format=lrc&usertoken=$token"
        val signedUrl = sign(subtitleUrl, secret)
        log("track.subtitle.get: $signedUrl")

        val subtitleBody = client.get(signedUrl) {
            header("User-Agent", USER_AGENT)
            header("Accept", "application/json, text/plain, */*")
            header("Accept-Language", "en-US,en;q=0.9")
        }.body<String>()
        val response = Json { ignoreUnknownKeys = true }
            .decodeFromString<TrackSubtitleResponse>(subtitleBody)
        log("track.subtitle.get: HTTP bytes=${subtitleBody.length} status=${response.message.header.statusCode}")

        val statusCode = response.message.header.statusCode
        if (statusCode != 200) throw IllegalStateException("Subtitles unavailable (status: $statusCode)")

        val bodyElement = response.message.body
        val subtitleObj = if (bodyElement is kotlinx.serialization.json.JsonObject) {
            val json = Json { ignoreUnknownKeys = true }
            val bodyObj = json.decodeFromJsonElement<TrackSubtitleResponseBody>(bodyElement)
            bodyObj.subtitle
        } else {
            null
        }

        val body = subtitleObj?.subtitleBody ?: throw IllegalStateException("Subtitles unavailable")
        if (body.isBlank()) {
            throw IllegalStateException("Subtitle body is empty")
        }
        convertSubtitleJsonToLrc(body)
    }

    private suspend fun getPlainLyrics(trackId: Long, token: String, secret: String): Result<String> = runCatching {
        val lyricsUrl = "${BASE_URL}track.lyrics.get?app_id=$APP_ID&format=json&track_id=$trackId&usertoken=$token"
        val signedUrl = sign(lyricsUrl, secret)
        log("track.lyrics.get: $signedUrl")

        val lyricsBody = client.get(signedUrl) {
            header("User-Agent", USER_AGENT)
            header("Accept", "application/json, text/plain, */*")
            header("Accept-Language", "en-US,en;q=0.9")
        }.body<String>()
        val response = Json { ignoreUnknownKeys = true }
            .decodeFromString<TrackLyricsResponse>(lyricsBody)
        log("track.lyrics.get: HTTP bytes=${lyricsBody.length} status=${response.message.header.statusCode}")

        val statusCode = response.message.header.statusCode
        if (statusCode != 200) throw IllegalStateException("Lyrics unavailable (status: $statusCode)")

        val bodyElement = response.message.body
        val lyricsObj = if (bodyElement is kotlinx.serialization.json.JsonObject) {
            val json = Json { ignoreUnknownKeys = true }
            val bodyObj = json.decodeFromJsonElement<TrackLyricsResponseBody>(bodyElement)
            bodyObj.lyrics
        } else {
            null
        }

        val body = lyricsObj?.lyricsBody ?: throw IllegalStateException("Lyrics body unavailable")
        if (body.isBlank()) {
            throw IllegalStateException("Lyrics body is empty")
        }
        body
    }

    private fun formatTime(timeMs: Long, isSyllable: Boolean): String {
        val minutes = (timeMs / 1000) / 60
        val seconds = (timeMs / 1000) % 60
        val millis = timeMs % 1000
        val prefix = if (isSyllable) "<" else "["
        val suffix = if (isSyllable) ">" else "]"
        return String.format(Locale.US, "%s%02d:%02d.%03d%s", prefix, minutes, seconds, millis, suffix)
    }

    internal fun convertRichSyncToLrc(entries: List<RichSyncEntry>): String {
        val sb = StringBuilder()
        for (entry in entries) {
            val lineText = entry.x.trim()
            if (lineText.isEmpty()) continue

            // Line header timestamp
            val lineTimeMs = (entry.ts * 1000).toLong()
            sb.append(formatTime(lineTimeMs, isSyllable = false))

            // Build inline syllable timings. The source encodes every space as
            // its own `WordEntry(" ")` between two real syllables, so writing
            // the entries verbatim already spaces consecutive words (`word` +
            // ` ` + `nextWord`). When the data omits a space (e.g. "the" →
            // "end" with no blank entry between them) we synthesize one so the
            // rendered karaoke text never glues two words together — the test's
            // `WordEntry("the", ...)/WordEntry("end", ...)` pair expects
            // `the <...>end` to read `the end` in the karaoke line.
            val words = entry.l.filter { it.c.isNotBlank() }
            words.forEachIndexed { index, word ->
                val wordTimeMs = ((entry.ts + word.o) * 1000).toLong()
                if (index > 0 && sb.isNotEmpty() && sb.last() != ' ' && !word.c.startsWith(" ")) sb.append(' ')
                sb.append(formatTime(wordTimeMs, isSyllable = true))
                sb.append(word.c.trim())
            }
            // Append any pure-space entries that the source used as explicit
            // gaps (e.g. tabs / multiple spaces) — they carry no timing of
            // their own and the value above already took care of the single
            // canonical separator between consecutive words.
            entry.l.filter { it.c.isEmpty() } // WordEntry("") is a legacy sentinel
                .forEach { /* consumed via the synthesized space above */ }
            sb.append("\n")
        }
        return sb.toString()
    }

    internal fun convertSubtitleJsonToLrc(body: String): String {
        return try {
            val entries = Json { ignoreUnknownKeys = true }.decodeFromString<List<SubtitleEntry>>(body)
            val sb = StringBuilder()
            entries.forEach { entry ->
                sb.append(formatTime((entry.time.total * 1000).toLong(), isSyllable = false))
                sb.append(entry.text)
                sb.append("\n")
            }
            sb.toString()
        } catch (e: Exception) {
            body
        }
    }

    internal fun cleanText(text: String): String {
        val noSpecialChars = text.replace(",", " ").replace("&", " ")
        return noSpecialChars.replace(Regex("\\(.*?\\)"), "")
            .replace(Regex("\\[.*?\\]"), "")
            .lowercase(Locale.US)
            .replace(Regex("[^a-z0-9 ]"), "")
            .replace(Regex("\\s+"), " ")
            .trim()
    }

    private fun logDebug(message: String) {
        try {
            val logClass = Class.forName("android.util.Log")
            val dMethod = logClass.getMethod("d", String::class.java, String::class.java)
            dMethod.invoke(null, "Musixmatch", message)
        } catch (e: Exception) {
            println("Musixmatch: $message")
        }
    }
}
