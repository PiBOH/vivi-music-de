@file:OptIn(ExperimentalComposeUiApi::class)

package com.music.vivi.desktop

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.ImageComposeScene
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import org.jetbrains.skia.EncodedImageFormat
import java.io.File
import java.awt.image.BufferedImage

/**
 * Headless check of the Updates and Notifications screens, run with
 *
 *     ./gradlew :desktop:settingsScreensRenderCheck
 *
 * The two screens are a column of cards, and what a card holds is a pixel fact:
 * a row is joined to the next by a 3 dp seam, while two cards stand 8 dp apart,
 * so the runs of card fill down a column and the gaps between them say how many
 * cards the screen is and how many rows each one carries. The check renders each
 * screen into an off-screen `ImageComposeScene` at density 1, reads that column
 * and asserts the shape the preview draws:
 *
 *  * **Updates**: six bands - the check row with the version under it, the
 *    pre-release switch, the interval, the source, and the Changelog plus
 *    Commits destinations - with the two 3 dp seams of the two two-row cards.
 *  * **Notifications**: the three rows of the card the mode owns, joined by its
 *    two seams, with the dropdowns and the test button under and above them.
 *  * **Content**: two dropdown cards that stand alone, one card of the five
 *    switches (four seams) and one for the lyrics providers.
 *  * **Language**: every language in one card under the search field, its rows
 *    joined by one seam each, the list reaching the bottom of the frame.
 *  * **Privacy**: one card of the two history switches (one seam).
 *  * **AI Lyrics Translation**: six rows in three cards - the provider, the
 *    three credentials rows, the two rows of the mode card - with three seams
 *    between rows that share one, and the note between them as text. The two
 *    providers that fold a row away are read too: four rows under DeepL (the
 *    mode row leaves) and five under Custom (the model row leaves).
 *
 * A frame is written under `.ignore/settings/` so it can be looked at.
 */
object SettingsScreensRenderCheck {

    private val outDir = File(".ignore/settings")

    private const val WIDTH = 640
    private const val HEIGHT = 1200

    /** A run of card fill down the measured column, in pixels from the top. */
    private data class Band(val top: Int, val bottom: Int) {
        val height: Int get() = bottom - top + 1
    }

    @JvmStatic
    fun main(args: Array<String>) {
        outDir.mkdirs()
        val failures = mutableListOf<String>()

        val updates = render {
            UpdateSection(
                language = "en",
                status = UpdateStatus.Idle,
                includePreReleases = false,
                updateIntervalHours = 24,
                updateSource = UpdateSource.FORK,
                onIntervalChange = {},
                onTogglePreReleases = {},
                onUpdateSourceChange = {},
                onCheckUpdates = {},
                onOpenChangelog = {},
                onOpenCommits = {},
            )
        }
        File(outDir, "updates.png").writeBytes(updates)
        val updateBands = bands(updates)
        println("updates: bands=${updateBands.size} ${updateBands.map { "${it.top}..${it.bottom}" }}")

        // The same screen with an update waiting: its download control is a filled
        // `Button` under the cards, and a button IS a band of this scan (page
        // colour inside an outlined one, which is why the old arm missed it).
        val updatesAvailable = render {
            UpdateSection(
                language = "en",
                status = UpdateStatus.Available(
                    version = "9.9.9_DE-9.9.9",
                    url = "https://github.com/PiBOH/vivi-music-de/releases",
                ),
                includePreReleases = false,
                updateIntervalHours = 24,
                updateSource = UpdateSource.FORK,
                onIntervalChange = {},
                onTogglePreReleases = {},
                onUpdateSourceChange = {},
                onCheckUpdates = {},
                onOpenChangelog = {},
                onOpenCommits = {},
            )
        }
        File(outDir, "updates_available.png").writeBytes(updatesAvailable)
        val updateAvailableBands = bands(updatesAvailable)
        println(
            "updates (available): bands=${updateAvailableBands.size} " +
                "${updateAvailableBands.map { "${it.top}..${it.bottom}" }}",
        )

        val notifications = render {
            SettingsNotificationsScreen(
                language = "en",
                onBack = {},
                notificationMode = "in_app",
                onNotificationModeChange = {},
                notificationDurationSeconds = 5,
                onNotificationDurationChange = {},
                saveHistory = true,
                onSaveHistoryChange = {},
                onOpenHistory = {},
                onTestNotification = {},
            )
        }
        File(outDir, "notifications.png").writeBytes(notifications)
        val notificationBands = bands(notifications)
        println("notifications: bands=${notificationBands.size} ${notificationBands.map { "${it.top}..${it.bottom}" }}")

        // A taller frame than the page is the only way to see the whole card:
        // the fourteen bands of this screen no longer fit the 1200 px one, and a
        // row cut off by the frame's edge is read as a missing seam.
        val content = render(height = 1400) {
            ContentSection(
                language = "en",
                contentLanguage = "",
                contentCountry = "",
                onContentLanguageChange = {},
                onContentCountryChange = {},
            )
        }
        File(outDir, "content.png").writeBytes(content)
        val contentBands = bands(content)
        println("content: bands=${contentBands.size} ${contentBands.map { "${it.top}..${it.bottom}" }}")

        val language = render {
            LanguageSection(language = "en", onLanguageChange = {})
        }
        File(outDir, "language.png").writeBytes(language)
        val languageBands = bands(language)
        println("language: bands=${languageBands.size} first=${languageBands.firstOrNull()?.top} last=${languageBands.lastOrNull()?.bottom}")

        val privacy = render {
            SettingsPrivacyScreen(
                language = "en",
                onBack = {},
                pauseListenHistory = false,
                onPauseListenHistoryChange = {},
                pauseSearchHistory = false,
                onPauseSearchHistoryChange = {},
                onClearSearchHistory = {},
            )
        }
        File(outDir, "privacy.png").writeBytes(privacy)
        val privacyBands = bands(privacy)
        println("privacy: bands=${privacyBands.size} ${privacyBands.map { "${it.top}..${it.bottom}" }}")

        val lyrics = render {
            SettingsLyricsScreen(
                language = "en",
                onBack = {},
                syncedLyrics = true,
                onToggleSyncedLyrics = {},
                lyricsTextSize = 18f,
                onLyricsTextSizeChange = {},
                lyricsLineSpacing = 1.35f,
                onLyricsLineSpacingChange = {},
                translationAvailable = true,
            )
        }
        File(outDir, "lyrics.png").writeBytes(lyrics)
        val lyricsBands = bands(lyrics)
        println("lyrics: bands=${lyricsBands.size} ${lyricsBands.map { "${it.top}..${it.bottom}" }}")

        val ai = render {
            SettingsAiScreen(
                language = "en",
                onBack = {},
                aiProvider = "OpenRouter",
                aiApiKey = "",
                aiBaseUrl = "https://openrouter.ai/api/v1/chat/completions",
                aiModel = "google/gemini-2.5-flash-lite",
                translateLanguage = "it",
                translateMode = "Literal",
                deeplApiKey = "",
                deeplFormality = "default",
                onAiProviderChange = {},
                onAiApiKeyChange = {},
                onAiBaseUrlChange = {},
                onAiModelChange = {},
                onTranslateLanguageChange = {},
                onTranslateModeChange = {},
                onDeeplApiKeyChange = {},
                onDeeplFormalityChange = {},
            )
        }
        File(outDir, "ai.png").writeBytes(ai)
        val aiBands = bands(ai)
        println("ai: bands=${aiBands.size} ${aiBands.map { "${it.top}..${it.bottom}" }}")

        // The two providers that fold rows away: DeepL has a key and a formality
        // instead of the catalogue's key/base URL/model, and it is not asked for a
        // translation mode; Custom keeps the key and the base URL and loses the
        // model, because the endpoint has no catalogue to pick from.
        val aiDeepL = render {
            SettingsAiScreen(
                language = "en",
                onBack = {},
                aiProvider = "DeepL",
                aiApiKey = "",
                aiBaseUrl = "",
                aiModel = "",
                translateLanguage = "it",
                translateMode = "Literal",
                deeplApiKey = "12345678-abcd",
                deeplFormality = "more",
                onAiProviderChange = {},
                onAiApiKeyChange = {},
                onAiBaseUrlChange = {},
                onAiModelChange = {},
                onTranslateLanguageChange = {},
                onTranslateModeChange = {},
                onDeeplApiKeyChange = {},
                onDeeplFormalityChange = {},
            )
        }
        File(outDir, "ai_deepl.png").writeBytes(aiDeepL)
        val aiDeepLBands = bands(aiDeepL)
        println("ai (DeepL): bands=${aiDeepLBands.size} ${aiDeepLBands.map { "${it.top}..${it.bottom}" }}")

        val aiCustom = render {
            SettingsAiScreen(
                language = "en",
                onBack = {},
                aiProvider = "Custom",
                aiApiKey = "sk-test",
                aiBaseUrl = "",
                aiModel = "local-model",
                translateLanguage = "it",
                translateMode = "Transcribed",
                deeplApiKey = "",
                deeplFormality = "default",
                onAiProviderChange = {},
                onAiApiKeyChange = {},
                onAiBaseUrlChange = {},
                onAiModelChange = {},
                onTranslateLanguageChange = {},
                onTranslateModeChange = {},
                onDeeplApiKeyChange = {},
                onDeeplFormalityChange = {},
            )
        }
        File(outDir, "ai_custom.png").writeBytes(aiCustom)
        val aiCustomBands = bands(aiCustom)
        println("ai (Custom): bands=${aiCustomBands.size} ${aiCustomBands.map { "${it.top}..${it.bottom}" }}")

        val contributors = render { ContributorsSection(language = "en") }
        File(outDir, "contributors.png").writeBytes(contributors)
        val contributorBands = bands(contributors)
        println("contributors: bands=${contributorBands.size} ${contributorBands.map { "${it.top}..${it.bottom}" }}")

        val license = render { LicenseSection(language = "en") }
        File(outDir, "license.png").writeBytes(license)
        val licenseEdge = bands(license, column = 596)
        println("license: right-edge bands=${licenseEdge.map { "${it.top}..${it.bottom}" }}")

        val playerDesign = render {
            PlayerDesignScreen(
                language = "en",
                design = PlayerDesign.CLASSIC,
                onDesignChange = {},
                background = PlayerBackgroundStyle.GRADIENT,
                onBackgroundChange = {},
                rotatingThumbnail = true,
                onRotatingThumbnailChange = {},
                sliderStyle = "slim",
                onSliderStyleChange = {},
                swipeThumbnail = true,
                onSwipeThumbnailChange = {},
                swipeSensitivity = 0.73f,
                onSwipeSensitivityChange = {},
            )
        }
        File(outDir, "player_design.png").writeBytes(playerDesign)
        val playerDesignBands = bands(playerDesign)
        println("player design: bands=${playerDesignBands.size} ${playerDesignBands.map { "${it.top}..${it.bottom}" }}")

        val playerDesignNoSwipe = render {
            PlayerDesignScreen(
                language = "en",
                design = PlayerDesign.CLASSIC,
                onDesignChange = {},
                background = PlayerBackgroundStyle.GRADIENT,
                onBackgroundChange = {},
                rotatingThumbnail = true,
                onRotatingThumbnailChange = {},
                sliderStyle = "slim",
                onSliderStyleChange = {},
                swipeThumbnail = false,
                onSwipeThumbnailChange = {},
                swipeSensitivity = 0.73f,
                onSwipeSensitivityChange = {},
            )
        }
        File(outDir, "player_design_no_swipe.png").writeBytes(playerDesignNoSwipe)
        val playerDesignNoSwipeBands = bands(playerDesignNoSwipe)
        println("player design (no swipe): bands=${playerDesignNoSwipeBands.size}")

        val about = render { AboutSection(language = "en", onOpenContributors = {}, onOpenLicense = {}) }
        File(outDir, "about.png").writeBytes(about)
        val aboutBands = bands(about)
        println("about: bands=${aboutBands.size} ${aboutBands.map { "${it.top}..${it.bottom}" }}")

        val developer = render { DeveloperOptionsSection(language = "en") }
        File(outDir, "developer_options.png").writeBytes(developer)
        val developerBands = bands(developer)
        println("developer: bands=${developerBands.size} ${developerBands.map { "${it.top}..${it.bottom}" }}")

        val desktopFeatures = render {
            SettingsDesktopScreen(
                language = "en",
                onBack = {},
                showWidget = true,
                onShowWidgetChange = {},
                mediaKeysEnabled = false,
                onMediaKeysChange = {},
                trayMenuEnabled = true,
                onTrayMenuChange = {},
            )
        }
        File(outDir, "desktop_features.png").writeBytes(desktopFeatures)
        val desktopBands = bands(desktopFeatures)
        println("desktop: bands=${desktopBands.size} ${desktopBands.map { "${it.top}..${it.bottom}" }}")

        val canvasOn = render {
            CanvasSection(
                language = "en",
                canvasEnabled = true,
                onCanvasEnabledChange = {},
                canvasSource = CanvasSource.AUTO,
                onCanvasSourceChange = {},
            )
        }
        File(outDir, "canvas.png").writeBytes(canvasOn)
        val canvasBands = bands(canvasOn)
        println("canvas(on): bands=${canvasBands.size} ${canvasBands.map { "${it.top}..${it.bottom}" }}")

        val canvasOff = render {
            CanvasSection(
                language = "en",
                canvasEnabled = false,
                onCanvasEnabledChange = {},
                canvasSource = CanvasSource.AUTO,
                onCanvasSourceChange = {},
            )
        }
        File(outDir, "canvas_off.png").writeBytes(canvasOff)
        val canvasOffBands = bands(canvasOff)
        println("canvas(off): bands=${canvasOffBands.size} ${canvasOffBands.map { "${it.top}..${it.bottom}" }}")

        val font = render {
            FontSection(language = "en", selectedFont = AppFont.SYSTEM, onFontChange = {})
        }
        File(outDir, "font.png").writeBytes(font)
        val fontBands = bands(font)
        println("font: bands=${fontBands.size} ${fontBands.map { "${it.top}..${it.bottom}" }}")

        val density = render {
            DensityScreen(language = "en", densityScale = 1f, onDensityScaleChange = {}, gridItemSize = 160, onGridItemSizeChange = {})
        }
        File(outDir, "density.png").writeBytes(density)
        val densityBands = bands(density)
        println("density: bands=${densityBands.size} ${densityBands.map { "${it.top}..${it.bottom}" }}")

        // The seam is the 3 dp the next row's own top padding takes; anything
        // wider is the gap the screen leaves between two cards.
        fun seams(bands: List<Band>) = bands.zipWithNext().count { (a, b) -> b.top - a.bottom - 1 in 1..4 }
        // A screen title or a group label paints a few rows of glyphs, which the
        // 6-unit cut above keeps as a band: the cards are the ones a row tall.
        fun cards(all: List<Band>) = all.filter { it.height >= 20 }

        val updateSeams = seams(updateBands)
        val notificationSeams = seams(notificationBands)
        val contentSeams = seams(contentBands)
        val languageSeams = seams(languageBands)
        val privacySeams = seams(privacyBands)
        val aiCards = cards(aiBands)
        val aiSeams = seams(aiCards)
        val aboutCards = cards(aboutBands)
        val desktopSeams = seams(desktopBands)
        val fontSeams = seams(fontBands)

        // Updates: two cards of two rows (the check with the version under it and
        // the pre-release switch, then the two destinations) and the interval and
        // source choices between them, each a card of its own.
        if (updateBands.size != 6) {
            failures += "Updates should read its two two-row cards and its two choices (6), read ${updateBands.size}"
        }
        if (updateSeams != 2) failures += "Updates should join its two two-row cards (2 seams), read $updateSeams"
        // With an update waiting there is one block more: the filled download
        // action under the cards. Below it sits the version line and the
        // one-line "no installer" note, neither of which is card fill.
        if (updateAvailableBands.size != updateBands.size + 1) {
            failures += "Updates with an update waiting should read its six cards and the filled action, " +
                "read ${updateAvailableBands.size}"
        }

        // Notifications: the mode's own card, the three rows of the card under it
        // (two seams), the duration card and the filled action. The mode used to
        // be a bare dropdown row on the page's background, which this scan could
        // not see as a card at all, and the action used to be outlined, which it
        // reads as page colour.
        if (notificationBands.size != 6) failures += "Notifications should read its mode card, three rows, the duration and the filled action, read ${notificationBands.size}"
        if (notificationSeams != 2) failures += "Notifications should join its three rows (2 seams), read $notificationSeams"

        // Content: the two dropdown cards stand alone, the five switches are one
        // card (four seams) and the provider priority rows are another (one seam
        // fewer than there are providers).
        val providerSeams = DesktopLyrics.PROVIDER_ORDER.size - 1
        if (contentSeams != 4 + providerSeams) {
            failures += "Content should join its five switches (4 seams) and its ${DesktopLyrics.PROVIDER_ORDER.size} " +
                "providers ($providerSeams seams), read $contentSeams"
        }

        // Language: a row per language, joined into one card under the field.
        if (languageBands.size < 15) failures += "Language should list the languages, read ${languageBands.size} row(s)"
        if (languageBands.firstOrNull()?.top?.let { it in 100..220 } != true) {
            failures += "Language's list must start under its field, read ${languageBands.firstOrNull()?.top}"
        }
        if (languageBands.lastOrNull()?.bottom?.let { it >= 1000 } != true) {
            failures += "Language's list must fill the page, read ${languageBands.lastOrNull()?.bottom}"
        }
        if (languageSeams != languageBands.size - 1) {
            failures += "Language's rows must be one card (${languageBands.size - 1} seams), read $languageSeams"
        }

        // Lyrics: the page's order. The screen opens on the card of the three
        // switches it lists first (synced, tap a line, auto scroll), one 3-px seam
        // apart, then the two sliders as rows of their own, then the two choices -
        // one card each, like every dropdown - then the display card the page does
        // not draw, and it ends on the Romanization row.
        val lyricsFirstCard = lyricsBands.take(3)
        if (lyricsBands.size != 12) failures += "Lyrics should read twelve rows, read ${lyricsBands.size}"
        if (lyricsFirstCard.size != 3 || seams(lyricsFirstCard) != 2) {
            failures += "Lyrics should open on one card of three switches, read ${lyricsFirstCard.size} row(s) and ${seams(lyricsFirstCard)} seam(s)"
        }
        if (lyricsBands.getOrNull(3)?.height?.let { it < 80 } != false || lyricsBands.getOrNull(4)?.height?.let { it < 80 } != false) {
            failures += "Lyrics' two sliders should be tall rows, read ${lyricsBands.getOrNull(3)?.height}/${lyricsBands.getOrNull(4)?.height}"
        }
        if (lyricsBands.getOrNull(5)?.height?.let { it > 60 } != false || lyricsBands.getOrNull(6)?.height?.let { it > 60 } != false) {
            failures += "Lyrics' two choices should be cards of one row, read ${lyricsBands.getOrNull(5)?.height}/${lyricsBands.getOrNull(6)?.height}"
        }

        // Privacy: the two history switches in one card, joined by one seam, and
        // the page's one action as a tonal button 16 dp under the card - a band
        // of its own, outside the two rows.
        if (privacyBands.size != 3) {
            failures += "Privacy should read its two switches and the action under them, read ${privacyBands.size}"
        }
        if (privacySeams != 1) failures += "Privacy should join its two switches (1 seam), read $privacySeams"
        if (privacyBands.lastOrNull()?.let { it.bottom - it.top }?.let { it !in 24..44 } != false) {
            failures += "Privacy's action should be one button row, read ${privacyBands.lastOrNull()?.let { it.bottom - it.top }}"
        }

        // AI: the provider card, the three credentials rows and the two rows of
        // the mode card - six rows in three cards, joined by three seams (the
        // title, the two group labels and the training note are no rows at all).
        if (aiCards.size != 6) failures += "AI should read six rows, read ${aiCards.size}"
        if (aiSeams != 3) failures += "AI should join its three credentials rows and its two mode rows (3 seams), read $aiSeams"

        // DeepL: the provider row, the DeepL key and the formality (one seam),
        // then the target language alone - the mode row folds away.
        val deepLCards = cards(aiDeepLBands)
        if (deepLCards.size != 4) {
            failures += "AI under DeepL should read four rows (provider, key, formality, language), read ${deepLCards.size}"
        }
        if (seams(deepLCards) != 1) failures += "AI under DeepL should join the key and the formality (1 seam), read ${seams(deepLCards)}"

        // Custom: the model row and the training note fold away; the mode row
        // stays, because a custom endpoint is still asked what to translate.
        val customCards = cards(aiCustomBands)
        if (customCards.size != 5) {
            failures += "AI under Custom should read five rows (provider, key, base URL, mode, language), read ${customCards.size}"
        }
        if (seams(customCards) != 2) {
            failures += "AI under Custom should join the key and the base URL and the two mode rows (2 seams), read ${seams(customCards)}"
        }

        // Contributors: one card, a row per person (the bundled list is the
        // fallback the frame draws); License is a document, so it is not read
        // by the band scan at all.
        if (contributorBands.isEmpty()) failures += "Contributors should list the bundled contributors, read nothing"
        if (seams(contributorBands) != contributorBands.size - 1) {
            failures += "Contributors should be one card (${contributorBands.size - 1} seams), read ${seams(contributorBands)}"
        }

        // Player & mini-player design: the design row alone, the background card
        // of two, the mini player card of five (four with the swipe off) and the
        // slider style.
        val designCards = cards(playerDesignBands)
        val designNoSwipeCards = cards(playerDesignNoSwipeBands)
        if (designCards.size != 9) failures += "Player design should read nine rows, read ${designCards.size}"
        if (seams(designCards) != 5) failures += "Player design should join 2 + 5 rows (5 seams), read ${seams(designCards)}"
        if (designNoSwipeCards.size != 8) {
            failures += "Player design without the swipe should lose its sensitivity row (8), read ${designNoSwipeCards.size}"
        }

        // About: the head card, then three cards of three, three and two rows
        // (what this build is, the ways out under the Community label, and the
        // two documents).
        if (aboutCards.size != 9) failures += "About should read nine rows (the head plus 3 + 3 + 2), read ${aboutCards.size}"
        if (seams(aboutCards) != 5) failures += "About should join 3 + 3 + 2 rows (5 seams), read ${seams(aboutCards)}"

        // Debug & Developer Options: the switch that locks the screen again, then
        // the Advanced card of the heap and the three switches.
        // The sentence under the title is a note, not a row: `cards` drops it.
        val developerCards = cards(developerBands)
        if (developerCards.size != 5) {
            failures += "Debug & Developer Options should read five rows (the lock row plus 4), read ${developerCards.size}"
        }
        if (seams(developerCards) != 3) {
            failures += "Debug & Developer Options should join the four Advanced rows (3 seams), read ${seams(developerCards)}"
        }

        // Desktop features: one card of the three switches.
        if (desktopBands.size != 3) failures += "Desktop features should read three rows, read ${desktopBands.size}"
        if (desktopSeams != 2) failures += "Desktop features should join its three switches (2 seams), read $desktopSeams"

        // Canvas: the source row only exists with the Canvas on.
        if (canvasBands.size != 2) failures += "Canvas on should read the switch and its source, read ${canvasBands.size}"
        if (canvasOffBands.size != 1) failures += "Canvas off should read its switch alone, read ${canvasOffBands.size}"

        // Font: the preview, then one card of five faces plus the import row (5
        // seams), then the lyrics switch in its own card.
        if (fontBands.size != 8) failures += "App font should read the preview, six list rows and the lyrics card, read ${fontBands.size}"
        if (fontSeams != 5) failures += "App font should join its five faces and the import (5 seams), read $fontSeams"

        // Display density: the two dropdown cards.
        if (densityBands.size != 2) failures += "Display density should read two cards, read ${densityBands.size}"

        if (failures.isEmpty()) {
            println()
            println(
                "OK: Updates is six bands with the two seams of its two-row cards, its action appearing " +
                    "with an available update, " +
                    "Notifications is the mode card over a card of three rows joined by two seams, " +
                    "Content joins its five switches and its providers, " +
                    "Language is one card of every language under its field, " +
                    "Privacy is one card of two switches with its action under them, Lyrics is ${lyricsBands.size} rows in the page order, Debug & Developer Options is ${developerCards.size} rows, the four of Advanced in one card, AI is ${aiCards.size} rows in three cards with $aiSeams seams " +
                    "(${cards(aiDeepLBands).size} under DeepL, ${cards(aiCustomBands).size} under Custom), " +
                    "Contributors is one card of ${contributorBands.size} rows, " +
                    "About is a head and three cards of 3 + 3 + 2 rows under its Community label, Desktop features one card of three switches, " +
                    "Canvas's source folds with its switch, App font joins its six rows, Display density is two cards, " +
                    "and Player & mini-player design is five cards whose sensitivity row leaves with the swipe",
            )
            return
        }
        println()
        println("FAILED:")
        failures.forEach { println("  - $it") }
        throw IllegalStateException("settings screens render check failed (${failures.size} case(s))")
    }

    /** One frame of [content], in the light scheme, on the app's own surface. */
    private fun render(height: Int = HEIGHT, content: @Composable () -> Unit): ByteArray {
        val scene = ImageComposeScene(WIDTH, height) {
            AppTheme(mode = ThemeMode.LIGHT, accent = Color(0xFFED5564), language = "en") {
                Box(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.surface)) {
                    Column(Modifier.padding(start = 20.dp, top = 4.dp).width(600.dp)) { content() }
                }
            }
        }
        try {
            scene.render(1_000_000_000L)
            return png(scene.render(1_500_000_000L))
        } finally {
            scene.close()
        }
    }

    /**
     * The runs of card fill down the column at x = 40.
     *
     * The card is `surfaceVariant` at 30 % over the page, so "not the page
     * colour" is the test: an icon tile, a switch and a label inside the row all
     * differ from the page, and the 3 dp seam and the 16 dp gap between cards are
     * the only places where the page shows through again.
     */
    private fun bands(png: ByteArray, column: Int = 40): List<Band> {
        val image: BufferedImage = javax.imageio.ImageIO.read(png.inputStream())
        val page = image.getRGB(2, 2)
        val runs = mutableListOf<Band>()
        var start = -1
        for (y in 0 until image.height) {
            val onCard = !near(image.getRGB(column, y), page, 6)
            if (onCard && start < 0) start = y
            if (!onCard && start >= 0) {
                runs += Band(start, y - 1)
                start = -1
            }
        }
        if (start >= 0) runs += Band(start, image.height - 1)
        // Text between two rows leaves a 1 px line of page colour: those are not
        // gaps, they are glyphs, so a short run is folded into its neighbour
        // instead of counting as a band. The threshold is a row's own height, not
        // a few pixels: the measured column runs through the group titles as well
        // as through the cards, and a `labelLarge` title paints a 12 px slice of
        // glyph at the column - which was counted as a band of its own the moment
        // the titles moved 4 dp in (the preview's `padding: 6px 4px`) and made the
        // App font screen read nine bands instead of eight. A card row is at least
        // 56 dp tall, so nothing a band is meant to be is lost.
        return runs.filter { it.height >= 20 }
    }

    private fun near(a: Int, b: Int, threshold: Int): Boolean {
        val dr = kotlin.math.abs(((a shr 16) and 255) - ((b shr 16) and 255))
        val dg = kotlin.math.abs(((a shr 8) and 255) - ((b shr 8) and 255))
        val db = kotlin.math.abs((a and 255) - (b and 255))
        return dr + dg + db <= threshold
    }

    private fun png(image: org.jetbrains.skia.Image): ByteArray =
        image.encodeToData(EncodedImageFormat.PNG)?.bytes ?: error("the scene produced no image")
}
