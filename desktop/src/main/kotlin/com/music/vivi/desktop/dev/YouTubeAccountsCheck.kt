package com.music.vivi.desktop.dev

import com.music.innertube.models.AccountInfo
import com.music.vivi.desktop.data.youtube.SessionVault
import com.music.vivi.desktop.data.youtube.YouTubeAccount
import com.music.vivi.desktop.data.youtube.YouTubeAccountBook
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import java.io.File
import java.nio.file.Files
import kotlin.system.exitProcess

/**
 * Proves the account list without touching the account this machine is signed
 * in with: the book is created in a temporary directory and every step is read
 * back from that directory.
 *
 * - two sessions become two entries, the second one active;
 * - a switch changes the active id and survives a reload from disk;
 * - the same email with a rotated cookie updates the same entry instead of
 *   adding a duplicate;
 * - removing the active account falls back to the other one, and removing the
 *   last one leaves no file;
 * - the file is sealed: `VIVICOK1`, and no cookie name or value appears.
 */
object YouTubeAccountsCheck {

    const val FLAG = "--youtube-accounts-check"

    /** Prints the real installation's saved identities without touching them. */
    const val STATE_FLAG = "--youtube-account-state"

    /** Prints the state and asks YouTube Music who the active session belongs to. */
    const val VERIFY_FLAG = "--youtube-account-verify"

    private const val ALPHA_COOKIE = "SID=ALPHAvalue0123456789; SAPISID=ALPHAsapisid987654321"
    private const val BETA_COOKIE = "SID=BETAvalue0123456789; SAPISID=BETAsapisid987654321"

    fun handle(args: Array<String>): Boolean {
        if (args.contains(STATE_FLAG) || args.contains(VERIFY_FLAG)) {
            exitProcess(printState(verify = args.contains(VERIFY_FLAG)))
        }
        if (!args.contains(FLAG)) return false
        exitProcess(run())
    }

    /**
     * Migrates the session this installation has into the account list, then
     * prints what is saved. No cookie, no email body and no full id is printed.
     */
    private fun printState(verify: Boolean): Int {
        YouTubeAccount.restore()
        if (verify) {
            val active = YouTubeAccount.activeAccountId()
            if (active == null) {
                println("[accounts] verify: no active account")
            } else {
                val info = kotlinx.coroutines.runBlocking { YouTubeAccount.info() }
                info.onSuccess { account ->
                    YouTubeAccount.updateIdentity(active, account)
                    println(
                        "[accounts] verify: ACTIVE (name set, email=${if (account.email != null) "set" else "none"})"
                    )
                }.onFailure { error ->
                    println(
                        "[accounts] verify: FAILED (${error.message?.take(140) ?: error.javaClass.simpleName})"
                    )
                }
            }
        }
        val summaries = YouTubeAccount.summaries()
        val active = YouTubeAccount.activeAccountId()
        println("[accounts] signedIn=${YouTubeAccount.isSignedIn} saved=${summaries.size}")
        summaries.forEach { account ->
            println(
                "[accounts] ${if (account.isActive) "*" else "-"} ${account.id.take(10)}... " +
                    "name='${account.name}' email=${if (account.email != null) "set" else "none"}"
            )
        }
        val consistent = YouTubeAccount.isSignedIn == (active != null) && (active == null || summaries.any { it.isActive })
        println("[accounts] active=${active?.take(10) ?: "none"}")
        println("[accounts] RESULT: ${if (consistent) "PASS" else "FAIL"}")
        return if (consistent) 0 else 1
    }

    private fun run(): Int {
        val failures = mutableListOf<String>()
        val directory = Files.createTempDirectory("vivi-accounts-check").toFile()

        fun expect(condition: Boolean, message: String) {
            println("[accounts] ${if (condition) "ok" else "FAIL"}: $message")
            if (!condition) failures += message
        }

        try {
            val file = File(directory, "yt-accounts.json")
            val book = YouTubeAccountBook(file)
            book.load()

            val alpha = book.upsert(
                cookie = ALPHA_COOKIE,
                dataSyncId = "ds-alpha",
                visitorData = "visitor-alpha",
                name = "Alpha Listener",
                email = "alpha@example.com",
                handle = "@alpha",
                thumbnailUrl = "https://example.com/alpha.png"
            )
            expect(book.accounts().size == 1, "the first session becomes one account")
            expect(book.activeId() == alpha, "the first session is active")

            val beta = book.upsert(
                cookie = BETA_COOKIE,
                dataSyncId = "ds-beta",
                visitorData = "visitor-beta",
                name = "Beta Listener",
                email = "beta@example.com",
                handle = "@beta"
            )
            expect(book.accounts().size == 2, "a second session becomes a second account")
            expect(book.activeId() == beta, "adding an account makes it active")

            val switched = book.switchTo(alpha)
            expect(book.activeId() == alpha && switched?.name == "Alpha Listener", "switching changes the active account")

            val reloaded = YouTubeAccountBook(file).also { it.load() }
            expect(reloaded.accounts().size == 2, "the list survives a reload from disk")
            expect(reloaded.activeId() == alpha, "the active account survives a reload from disk")
            expect(
                reloaded.record(alpha)?.dataSyncId == "ds-alpha",
                "the account identity survives a reload from disk"
            )

            val rotated = book.upsert(
                cookie = "SID=ALPHArotated0123456789; SAPISID=ALPHAsapisid987654321",
                dataSyncId = "ds-alpha-2",
                visitorData = null,
                name = "Alpha Listener",
                email = "alpha@example.com"
            )
            expect(rotated == alpha, "a rotated cookie with the same email keeps the same entry")
            expect(book.accounts().size == 2, "a rotated cookie does not add a duplicate")

            val next = book.remove(alpha)
            expect(next?.id == beta, "removing the active account falls back to another one")
            expect(book.activeId() == beta, "the fallback account is active")

            val last = book.remove(beta)
            expect(last == null && book.accounts().isEmpty(), "removing the last account leaves an empty list")
            expect(!file.exists(), "removing the last account removes the sealed list file")

            // The file is written and sealed again, then read raw for the audit.
            book.upsert(BETA_COOKIE, "ds-beta", "visitor-beta", name = "Beta Listener", email = "beta@example.com")
            val bytes = file.readBytes()
            val raw = String(bytes, Charsets.ISO_8859_1)
            expect(
                bytes.size > 8 && String(bytes, 0, 8, Charsets.US_ASCII) == "VIVICOK1",
                "the list file carries the VIVICOK1 container header"
            )
            expect(
                !raw.contains("SID=") && !raw.contains("BETAvalue") && !raw.contains("SAPISID"),
                "the list file holds no cookie name or value"
            )

            // The stored JSON is readable by the vault after the seal, no field lost.
            val audit = Json.parseToJsonElement(
                SessionVault.read(file) ?: "{}"
            ).jsonObject
            val accounts = audit["accounts"]?.jsonArray?.size ?: 0
            val active = audit["activeId"]?.jsonPrimitive?.content
            expect(accounts == 1 && active?.isNotBlank() == true, "the sealed list decodes to one active account")

            val pass = failures.isEmpty()
            println("[accounts] RESULT: ${if (pass) "PASS" else "FAIL"}")
            return if (pass) 0 else 1
        } catch (t: Throwable) {
            println("[accounts] FAIL: ${t.javaClass.simpleName} - ${t.message}")
            println("[accounts] RESULT: FAIL")
            return 1
        } finally {
            directory.deleteRecursively()
        }
    }

}
