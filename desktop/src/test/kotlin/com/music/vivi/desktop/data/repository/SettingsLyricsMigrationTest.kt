package com.music.vivi.desktop.data.repository

import java.io.File
import java.nio.file.Files
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.Json

class SettingsLyricsMigrationTest {

    private val directory: File = Files.createTempDirectory("vivi-settings-test").toFile()
    private val file = File(directory, "settings.json")
    private val json = Json {
        prettyPrint = true
        ignoreUnknownKeys = true
        encodeDefaults = true
    }

    @AfterTest
    fun tearDown() {
        directory.deleteRecursively()
    }

    private fun repository() = SettingsRepositoryImpl(settingsFile = file, json = json)

    /** A file written by a build that did not know the migration marker. */
    private fun legacyProfile(fetchOnlineLyrics: Boolean, providers: List<String>? = null) {
        val providersJson = providers?.joinToString(prefix = "[", postfix = "]") { "\"$it\"" }
        file.writeText(
            buildString {
                append("{\n")
                append("  \"fetchOnlineLyrics\": $fetchOnlineLyrics")
                if (providersJson != null) append(",\n  \"lyricsProviders\": $providersJson")
                append("\n}\n")
            }
        )
    }

    @Test
    fun legacyOffProfileLoadsWithOnlineSourcesOn() {
        legacyProfile(fetchOnlineLyrics = false, providers = listOf("Musixmatch", "YouLyPlus"))

        val loaded = runBlocking { repository().getSettings() }

        assertTrue(loaded.fetchOnlineLyrics)
        assertEquals(1, loaded.lyricsSourcesMigration)
        assertEquals(listOf("Musixmatch", "YouLyPlus"), loaded.lyricsProviders)
        val onDisk = file.readText()
        assertTrue("\"fetchOnlineLyrics\": true" in onDisk)
        assertTrue("\"lyricsSourcesMigration\": 1" in onDisk)
    }

    @Test
    fun emptyProviderListFallsBackToTheDefaultOrder() {
        legacyProfile(fetchOnlineLyrics = false, providers = emptyList())

        val loaded = runBlocking { repository().getSettings() }

        assertEquals(8, loaded.lyricsProviders.size)
        assertEquals("Musixmatch", loaded.lyricsProviders.first())
    }

    @Test
    fun turningTheSwitchOffAfterTheMigrationStaysOff() {
        legacyProfile(fetchOnlineLyrics = false)
        val first = repository()
        runBlocking { first.setFetchOnlineLyrics(false) }

        val reopened = runBlocking { repository().getSettings() }

        assertEquals(false, reopened.fetchOnlineLyrics)
        assertEquals(1, reopened.lyricsSourcesMigration)
    }
}
