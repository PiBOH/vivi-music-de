package com.music.vivi.desktop.presentation.screens.settings

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.toggleableState
import androidx.compose.ui.state.ToggleableState
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.music.vivi.desktop.data.preferences.AlbumArtPaletteStyle
import com.music.vivi.desktop.data.preferences.UserSettings
import com.music.vivi.desktop.generated.resources.Res
import com.music.vivi.desktop.generated.resources.ic_automirrored_rounded_arrow_back
import com.music.vivi.desktop.generated.resources.ic_rounded_chevron_right
import com.music.vivi.desktop.generated.resources.ic_rounded_expand_more
import com.music.vivi.desktop.presentation.components.subcomps.AppTooltip
import com.music.vivi.desktop.presentation.viewmodel.PlayerViewModel
import org.jetbrains.compose.resources.painterResource

/** The 1.55 home, in its original four groups and order. */
val Legacy155HomeGroups: List<Pair<String, List<SettingsSection>>> = listOf(
    "PIXELPLAYER" to listOf(SettingsSection.PIXELPLAYER),
    "GENERAL" to listOf(
        SettingsSection.V155_UPDATES,
        SettingsSection.V155_ACCOUNT,
        SettingsSection.V155_NOTIFICATIONS,
    ),
    "MEDIA & PLAYER" to listOf(
        SettingsSection.V155_APPEARANCE,
        SettingsSection.V155_LYRICS,
        SettingsSection.V155_PLAYER,
        SettingsSection.V155_DEVICES,
        SettingsSection.V155_CONTENT,
        SettingsSection.V155_LANGUAGE,
    ),
    "PRIVACY & DATA" to listOf(
        SettingsSection.V155_AI,
        SettingsSection.V155_PRIVACY,
        SettingsSection.V155_STORAGE,
        SettingsSection.V155_DATA_SAVER,
    ),
    "SYSTEM & ABOUT" to listOf(
        SettingsSection.V155_BACKUP,
        SettingsSection.V155_WRAPPED,
        SettingsSection.V155_INTEGRATIONS,
        SettingsSection.V155_SPOTIFY,
        SettingsSection.V155_DESKTOP,
        SettingsSection.V155_SYSTEM,
        SettingsSection.V155_ABOUT,
    ),
)

/** Every home row, in the order the home paints it. */
val Legacy155HomeEntries: List<SettingsSection>
    get() = Legacy155HomeGroups.flatMap { it.second }.filter { it != SettingsSection.PIXELPLAYER }

enum class LegacyKind { SWITCH, DROPDOWN, TEXT, SLIDER, ACTION }

/**
 * A 1.55 option that is still a typed [UserSettings] field. The 1.55 page and
 * the 1.56 page both go through this enum, so one write is visible on the other
 * surface without a copy or a local state.
 */
enum class LegacyCommon {
    DARK_MODE,
    DYNAMIC_COLOR,
    UI_SCALE,
    SHOW_PLAYER_INFO,
    CROSSFADE,
    CROSSFADE_DURATION,
    AUDIO_QUALITY,
    AUDIO_CACHE,
    LYRICS_ALIGNMENT,
    LYRICS_TEXT_SIZE,
    SHOW_ROMANIZATION,
    SHOW_INTRO,
    INTRO_STYLE,
    INTRO_BACKGROUND,
    CLOSE_TO_TRAY,
    EQUALIZER_ENABLED,
    ALBUM_ART_CACHE,
    PALETTE_STYLE,
}

data class LegacyOption(
    val key: String,
    val title: String,
    val subtitle: String = "",
    val kind: LegacyKind = LegacyKind.SWITCH,
    val common: LegacyCommon? = null,
    val choices: List<Pair<String, String>> = emptyList(),
    val default: String = "",
    val range: ClosedFloatingPointRange<Float> = 0f..1f,
    val steps: Int = 0,
    val target: SettingsSection? = null,
)

data class LegacySectionSpec(val title: String, val options: List<LegacyOption>)

data class LegacyPageSpec(
    val title: String,
    val subtitle: String,
    val sections: List<LegacySectionSpec>,
)

private fun sw(
    key: String,
    title: String,
    subtitle: String = "",
    default: Boolean = false,
    common: LegacyCommon? = null,
) = LegacyOption(key, title, subtitle, LegacyKind.SWITCH, common, default = default.toString())

private fun dd(
    key: String,
    title: String,
    choices: List<Pair<String, String>>,
    default: String,
    subtitle: String = "",
    common: LegacyCommon? = null,
) = LegacyOption(key, title, subtitle, LegacyKind.DROPDOWN, common, choices, default)

private fun txt(key: String, title: String, default: String = "", subtitle: String = "") =
    LegacyOption(key, title, subtitle, LegacyKind.TEXT, default = default)

private fun sl(
    key: String,
    title: String,
    range: ClosedFloatingPointRange<Float>,
    steps: Int,
    default: Float,
    subtitle: String = "",
    common: LegacyCommon? = null,
) = LegacyOption(
    key = key,
    title = title,
    subtitle = subtitle,
    kind = LegacyKind.SLIDER,
    common = common,
    default = default.toString(),
    range = range,
    steps = steps,
)

private fun go(key: String, title: String, subtitle: String, target: SettingsSection) =
    LegacyOption(key, title, subtitle, LegacyKind.ACTION, target = target)

private fun group(title: String, vararg options: LegacyOption) =
    LegacySectionSpec(title, options.toList())

private fun page(title: String, subtitle: String, vararg sections: LegacySectionSpec) =
    LegacyPageSpec(title, subtitle, sections.toList())

private val languageCodeToName: Map<String, String> = mapOf(
    "af" to "Afrikaans",
    "az" to "Azərbaycan",
    "id" to "Bahasa Indonesia",
    "ms" to "Bahasa Malaysia",
    "ca" to "Català",
    "cs" to "Čeština",
    "da" to "Dansk",
    "de" to "Deutsch",
    "et" to "Eesti",
    "en-GB" to "English (UK)",
    "en" to "English (US)",
    "es" to "Español (España)",
    "es-419" to "Español (Latinoamérica)",
    "eu" to "Euskara",
    "fil" to "Filipino",
    "fr" to "Français",
    "fr-CA" to "Français (Canada)",
    "gl" to "Galego",
    "hr" to "Hrvatski",
    "zu" to "IsiZulu",
    "is" to "Íslenska",
    "it" to "Italiano",
    "sw" to "Kiswahili",
    "lt" to "Lietuvių",
    "hu" to "Magyar",
    "nl" to "Nederlands",
    "no" to "Norsk",
    "or" to "Odia",
    "uz" to "O‘zbe",
    "pl" to "Polski",
    "pt-PT" to "Português",
    "pt" to "Português (Brasil)",
    "ro" to "Română",
    "sq" to "Shqip",
    "sk" to "Slovenčina",
    "sl" to "Slovenščina",
    "fi" to "Suomi",
    "sv" to "Svenska",
    "bo" to "Tibetan བོད་སྐད།",
    "vi" to "Tiếng Việt",
    "tr" to "Türkçe",
    "bg" to "Български",
    "ky" to "Кыргызча",
    "kk" to "Қазақ Тілі",
    "mk" to "Македонски",
    "mn" to "Монгол",
    "ru" to "Русский",
    "sr" to "Српски",
    "uk" to "Українська",
    "el" to "Ελληνικά",
    "hy" to "Հայերեն",
    "iw" to "עברית",
    "ur" to "اردو",
    "ar" to "العربية",
    "fa" to "فارسی",
    "ne" to "नेपाली",
    "mr" to "मराठी",
    "hi" to "हिन्दी",
    "bn" to "বাংলা",
    "pa" to "ਪੰਜਾਬੀ",
    "gu" to "ગુજરાતી",
    "ta" to "தமிழ்",
    "te" to "తెలుగు",
    "kn" to "ಕನ್ನಡ",
    "ml" to "മലയാളം",
    "si" to "සිංහල",
    "th" to "ภาษาไทย",
    "lo" to "ລາວ",
    "my" to "ဗမာ",
    "ka" to "ქართული",
    "am" to "አማርኛ",
    "km" to "ខ្មែរ",
    "zh-CN" to "中文 (简体)",
    "zh-TW" to "中文 (繁體)",
    "zh-HK" to "中文 (香港)",
    "ja" to "日本語",
    "ko" to "한국어",
)

private val countryCodeToName: Map<String, String> = mapOf(
    "DZ" to "Algeria",
    "AR" to "Argentina",
    "AU" to "Australia",
    "AT" to "Austria",
    "AZ" to "Azerbaijan",
    "BH" to "Bahrain",
    "BD" to "Bangladesh",
    "BY" to "Belarus",
    "BE" to "Belgium",
    "BO" to "Bolivia",
    "BA" to "Bosnia and Herzegovina",
    "BR" to "Brazil",
    "BG" to "Bulgaria",
    "KH" to "Cambodia",
    "CA" to "Canada",
    "CL" to "Chile",
    "HK" to "Hong Kong",
    "CO" to "Colombia",
    "CR" to "Costa Rica",
    "HR" to "Croatia",
    "CY" to "Cyprus",
    "CZ" to "Czech Republic",
    "DK" to "Denmark",
    "DO" to "Dominican Republic",
    "EC" to "Ecuador",
    "EG" to "Egypt",
    "SV" to "El Salvador",
    "EE" to "Estonia",
    "FI" to "Finland",
    "FR" to "France",
    "GE" to "Georgia",
    "DE" to "Germany",
    "GH" to "Ghana",
    "GR" to "Greece",
    "GT" to "Guatemala",
    "HN" to "Honduras",
    "HU" to "Hungary",
    "IS" to "Iceland",
    "IN" to "India",
    "ID" to "Indonesia",
    "IQ" to "Iraq",
    "IE" to "Ireland",
    "IL" to "Israel",
    "IT" to "Italy",
    "JM" to "Jamaica",
    "JP" to "Japan",
    "JO" to "Jordan",
    "KZ" to "Kazakhstan",
    "KE" to "Kenya",
    "KR" to "South Korea",
    "KW" to "Kuwait",
    "LA" to "Lao",
    "LV" to "Latvia",
    "LB" to "Lebanon",
    "LY" to "Libya",
    "LI" to "Liechtenstein",
    "LT" to "Lithuania",
    "LU" to "Luxembourg",
    "MK" to "Macedonia",
    "MY" to "Malaysia",
    "MT" to "Malta",
    "MX" to "Mexico",
    "ME" to "Montenegro",
    "MA" to "Morocco",
    "NP" to "Nepal",
    "NL" to "Netherlands",
    "NZ" to "New Zealand",
    "NI" to "Nicaragua",
    "NG" to "Nigeria",
    "NO" to "Norway",
    "OM" to "Oman",
    "PK" to "Pakistan",
    "PA" to "Panama",
    "PG" to "Papua New Guinea",
    "PY" to "Paraguay",
    "PE" to "Peru",
    "PH" to "Philippines",
    "PL" to "Poland",
    "PT" to "Portugal",
    "PR" to "Puerto Rico",
    "QA" to "Qatar",
    "RO" to "Romania",
    "RU" to "Russian Federation",
    "SA" to "Saudi Arabia",
    "SN" to "Senegal",
    "RS" to "Serbia",
    "SG" to "Singapore",
    "SK" to "Slovakia",
    "SI" to "Slovenia",
    "ZA" to "South Africa",
    "ES" to "Spain",
    "LK" to "Sri Lanka",
    "SE" to "Sweden",
    "CH" to "Switzerland",
    "TW" to "Taiwan",
    "TZ" to "Tanzania",
    "TH" to "Thailand",
    "TN" to "Tunisia",
    "TR" to "Turkey",
    "UG" to "Uganda",
    "UA" to "Ukraine",
    "AE" to "United Arab Emirates",
    "GB" to "United Kingdom",
    "US" to "United States",
    "UY" to "Uruguay",
    "VE" to "Venezuela (Bolivarian Republic)",
    "VN" to "Vietnam",
    "YE" to "Yemen",
    "ZW" to "Zimbabwe",
)

private val languageChoices: List<Pair<String, String>> = languageCodeToName.map { it.key to it.value }
/** The 1.55 app-language picker: Languages.all, native names, 48 entries. */
private val appLanguageChoices: List<Pair<String, String>> = listOf(
    "en" to "English",
    "az" to "Azərbaycan dili",
    "bs" to "Bosanski",
    "ca" to "Català",
    "cs" to "Čeština",
    "de" to "Deutsch",
    "et" to "Eesti",
    "es" to "Español",
    "eu" to "Euskara",
    "fil" to "Filipino",
    "fr" to "Français",
    "hr" to "Hrvatski",
    "id" to "Bahasa Indonesia",
    "it" to "Italiano",
    "lt" to "Lietuvių",
    "hu" to "Magyar",
    "ms" to "Bahasa Melayu",
    "nl" to "Nederlands",
    "nb" to "Norsk bokmål",
    "pl" to "Polski",
    "pt" to "Português",
    "ro" to "Română",
    "sk" to "Slovenčina",
    "sl" to "Slovenščina",
    "sr" to "Српски",
    "fi" to "Suomi",
    "sv" to "Svenska",
    "vi" to "Tiếng Việt",
    "tr" to "Türkçe",
    "el" to "Ελληνικά",
    "be" to "Беларуская",
    "bg" to "Български",
    "ru" to "Русский",
    "uk" to "Українська",
    "ar" to "العربية",
    "hi" to "हिन्दी",
    "as" to "অসমীয়া",
    "bn" to "বাংলা",
    "pa" to "ਪੰਜਾਬੀ",
    "ta" to "தமிழ்",
    "te" to "తెలుగు",
    "ml" to "മലയാളം",
    "th" to "ไทย",
    "km" to "ខ្មែរ",
    "ko" to "한국어",
    "zh-rCN" to "简体中文",
    "zh-rTW" to "繁體中文",
    "ja" to "日本語",
)


private val countryChoices: List<Pair<String, String>> = countryCodeToName.map { it.key to it.value }

private val themeChoices = listOf("system" to "System", "light" to "Light", "dark" to "Dark")

private val paletteChoices = AlbumArtPaletteStyle.entries.map { it.storageKey to it.label }

private val romanizationLanguages = listOf(
    "Japanese" to "lyricsRomanizeJapanese",
    "Korean" to "lyricsRomanizeKorean",
    "Chinese" to "lyricsRomanizeChinese",
    "Russian" to "lyricsRomanizeRussian",
    "Ukrainian" to "lyricsRomanizeUkrainian",
    "Serbian" to "lyricsRomanizeSerbian",
    "Bulgarian" to "lyricsRomanizeBulgarian",
    "Belarusian" to "lyricsRomanizeBelarusian",
    "Kyrgyz" to "lyricsRomanizeKyrgyz",
    "Macedonian" to "lyricsRomanizeMacedonian",
    "Hindi" to "lyricsRomanizeHindi",
    "Punjabi" to "lyricsRomanizePunjabi",
)

/** The full 1.55 tree, recovered from the 1.55.0 tag and restyled for 1.56. */
fun legacyPageFor(section: SettingsSection): LegacyPageSpec = when (section) {
    SettingsSection.V155_UPDATES -> page(
        "Updates", "Channel, interval and update source",
        group(
            "UPDATE CHANNEL",
            sw("includePreReleases", "Include pre-releases", "Offer beta builds as well as stable builds."),
            dd(
                "updateSource", "Update source",
                listOf("fork" to "Axolotl Music (official)", "original" to "Vivi Music upstream"),
                "fork",
                "The repository the updater reads.",
            ),
            dd(
                "updateCheckIntervalHours", "Check interval",
                listOf(
                    "0" to "Manual only",
                    "6" to "Every 6 hours",
                    "12" to "Every 12 hours",
                    "24" to "Every day",
                    "72" to "Every 3 days",
                    "168" to "Every week",
                ),
                "24",
                "How often the app asks the release feed.",
            ),
        ),
        group(
            "STATUS",
            txt("currentVersion", "Current version", "1.56.0-beta5", "Read-only status."),
            txt("updateLastCheck", "Last check", "At the next start", "Read-only status."),
        ),
    )
    SettingsSection.V155_ACCOUNT -> page(
        "Account", "YouTube Music account",
        group(
            "YOUTUBE MUSIC",
            txt("accountName", "Account", "Not signed in", "The active YouTube Music identity."),
            txt("accountEmail", "Email", "", "Shown only when the account exposes it."),
        ),
        group(
            "SYNC",
            sw("syncPlaylistsWithYoutube", "Sync playlists with YouTube", "Keep account playlists and the local library together.", true),
        ),
    )
    SettingsSection.V155_NOTIFICATIONS -> page(
        "Notifications", "Mode, duration and history",
        group(
            "NOTIFICATION MODE",
            dd(
                "notificationMode", "Mode",
                listOf("in_app" to "Main window", "native" to "Native notification"),
                "in_app",
                "Where a track change is announced.",
            ),
            sw("allowNotifications", "Allow notifications", "Let the app post system notifications.", true),
            dd(
                "inAppNotificationDurationSeconds", "Duration",
                listOf("3" to "3s", "5" to "5s", "10" to "10s", "15" to "15s", "30" to "30s"),
                "5",
                "How long the banner stays on screen.",
            ),
            sw("saveNotificationHistory", "Save notification history", "Keep the records in Settings.", true),
        ),
    )
    SettingsSection.V155_APPEARANCE -> page(
        "Appearance", "Theme, player, canvas, font and layout",
        group(
            "APPEARANCE",
            go("theme", "Theme", "Mode, accent and palette", SettingsSection.V155_THEME),
            go("playerDesign", "Player & mini-player design", "Player, mini player and slider", SettingsSection.V155_PLAYER_DESIGN),
            go("canvas", "ViviMusic Canvas", "Animated artwork behind the player", SettingsSection.V155_CANVAS),
            go("font", "App font", "App font and lyrics", SettingsSection.V155_FONT),
            go("density", "Density & grid", "Interface scale and grid size", SettingsSection.V155_DENSITY),
            go("transitions", "Screen transitions", "Fade, slide or off", SettingsSection.V155_TRANSITIONS),
            go("intro", "Intro", "The splash shown before the shell", SettingsSection.INTRO),
        ),
        group(
            "UI",
            sw("animationsEnabled", "Animations", "Play the interface animations.", true),
            dd(
                "animationSpeed", "Animation speed",
                listOf("fast" to "Fast", "normal" to "Normal", "slow" to "Slow"),
                "normal",
            ),
            sw("nativeTitleBar", "Native title bar", "Use the window's own title bar."),
            sw("showRightSidebar", "Right panel", "Show the Now Playing column when the window is wide enough.", true),
        ),
    )
    SettingsSection.V155_LYRICS -> page(
        "Lyrics", "Style, position and romanization",
        group(
            "DISPLAY",
            sw("syncedLyrics", "Synced lyrics", "Highlight the line that is playing.", true),
            dd(
                "lyricsAnimationStyle", "Animation style",
                listOf(
                    "NONE" to "None",
                    "FADE" to "Fade",
                    "GLOW" to "Glow",
                    "SLIDE" to "Slide",
                    "KARAOKE" to "Karaoke",
                    "APPLE" to "Apple Music",
                    "APPLE_V2" to "Apple Music v2",
                    "VIVIMUSIC_1" to "Vivi Music 1.55",
                    "LYRICS_V2" to "Lyrics v2",
                    "METRO_LYRICS" to "Metro",
                    "ALPHA" to "Alpha",
                ),
                "VIVIMUSIC_1",
            ),
            sw("lyricsGlowEffect", "Glow", "Glow the active line.", true),
            sw("lyricsAppleMusicBlur", "Apple Music blur", "Blur the inactive lines in the Vivi Music style."),
            sw("lyricsStandardBlur", "Standard blur", "Blur the inactive lines in the standard style."),
        ),
        group(
            "POSITION & TYPE",
            dd(
                "lyricsTextPosition", "Text position",
                listOf("LEFT" to "Left", "CENTER" to "Center", "RIGHT" to "Right"),
                "CENTER",
            ),
            dd(
                "lyricsAlignment", "Alignment",
                listOf("left" to "Left", "center" to "Center", "right" to "Right"),
                "left",
                common = LegacyCommon.LYRICS_ALIGNMENT,
            ),
            dd(
                "lyricsTextSize", "Text size",
                listOf("small" to "Small", "medium" to "Medium", "large" to "Large"),
                "medium",
                common = LegacyCommon.LYRICS_TEXT_SIZE,
            ),
            sl("lyricsLineSpacing", "Line spacing", 1f..2f, 10, 1.35f),
            sw("lyricsUseAppFont", "Use the app font", "Draw lyrics with the selected app font.", true),
        ),
        group(
            "INTERACTION",
            sw("lyricsClickToSeek", "Click to seek", "A lyric line seeks the player.", true),
            sw("lyricsAutoScroll", "Auto-scroll", "Follow the active line.", true),
            sw("lyricsThumbnailPlayPause", "Thumbnail play/pause", "Tap the artwork to play or pause."),
            sw("showLyricsRomanization", "Romanization", "Show the romanized line under the original.", true, LegacyCommon.SHOW_ROMANIZATION),
            go("romanize", "Romanization languages", "Choose the scripts that are romanized", SettingsSection.V155_ROMANIZE),
            sw("translateLyrics", "Translate lyrics", "Use the AI translation on the lyrics screen."),
        ),
    )
    SettingsSection.V155_PLAYER -> page(
        "Player & audio", "Queue, crossfade, quality and equalizer",
        group(
            "PLAYER",
            go("playerDesign", "Player & mini-player design", "Player, mini player and slider", SettingsSection.V155_PLAYER_DESIGN),
            go("equalizerScreen", "Vivi Equalizer", "Profiles, bands and preamp", SettingsSection.V155_EQUALIZER),
            dd(
                "audioQuality", "Audio quality",
                listOf("auto" to "Auto", "high" to "High (256 kbps)", "low" to "Low (128 kbps)"),
                "auto",
                common = LegacyCommon.AUDIO_QUALITY,
            ),
        ),
        group(
            "CROSSFADE",
            sw("crossfade", "Crossfade", "Blend the end of one track into the next.", false, LegacyCommon.CROSSFADE),
            sw("disableCrossfadeGapless", "Disable gapless", "Do not join tracks that are meant to be gapless."),
            sl("crossfadeDurationSeconds", "Crossfade duration", 1f..12f, 10, 3f, "Seconds.", LegacyCommon.CROSSFADE_DURATION),
        ),
        group(
            "HISTORY",
            sl("historyDurationSeconds", "History duration", 1f..100f, 98, 30f, "Seconds before a play is recorded."),
        ),
        group(
            "PLAYBACK",
            sw("autoPlayNext", "Autoplay next", "Continue with a related track when the queue ends.", true),
            sw("rememberShuffleRepeat", "Remember shuffle and repeat", "Restore both modes on the next start."),
            sw("persistentQueue", "Persistent queue", "Restore the queue on the next start.", true),
            sw("autoLoadMore", "Load more", "Ask the catalogue for more when the queue runs low.", true),
            sw("preventDuplicateTracksInQueue", "Prevent duplicates", "Do not add a track the queue already holds."),
            sw("autoSkipNextOnError", "Skip on error", "Move to the next track when the stream fails."),
            sw("persistentShuffle", "Persistent shuffle", "Keep shuffle on across restarts."),
            sw("progressiveSeek", "Progressive seek", "Seek while the bar is still being dragged."),
            sw("autoDownloadOnLike", "Download on like", "Cache a track when it is liked."),
            sw("skipSilence", "Skip silence", "Jump over silent passages."),
            sw("skipSilenceInstant", "Instantly skip silence", "Skip silence without the short analysis."),
        ),
        group(
            "MISC",
            sw("pauseWhenMediaMuted", "Pause when media is muted", "Pause when the system volume is muted."),
            sw("keepScreenOnWhenPlayerExpanded", "Keep the screen on", "While the player is expanded."),
        ),
        group(
            "CACHE",
            sl("streamCacheMinutes", "Stream cache", 10f..61f, 50, 10f, "Minutes; 61 keeps the resolved URL forever."),
        ),
    )
    SettingsSection.V155_DEVICES -> page(
        "Device sync", "Pairing and volume sync",
        group(
            "THIS DEVICE",
            txt("deviceName", "Device name", "Desktop", "The name the other device sees."),
            txt("pairId", "Pairing", "Not paired", "The pairing identity."),
            sw("syncViviVolume", "Sync app volume", "The phone follows this window's volume.", true),
            sw("syncNativeVolume", "Sync system volume", "The phone follows the OS volume.", true),
            sw("listenTogetherSyncVolume", "Sync Listen Together volume", "Apply the room volume.", true),
            sw("listenTogetherSmartResync", "Smart resync", "Recover a room after a short disconnection.", true),
        ),
    )

    SettingsSection.V155_CONTENT -> page(
        "Content", "Language, country and filters",
        group(
            "REGION",
            dd("contentLanguage", "Content language", listOf("" to "System default") + languageChoices, "", "The language of the catalogue's own labels."),
            dd("contentCountry", "Content country", listOf("" to "System default") + countryChoices, "", "The catalogue's own country."),
        ),
        group(
            "FILTERS",
            sw("hideExplicit", "Hide explicit songs", "Remove explicit results from the catalogue."),
            sw("hideVideoSongs", "Hide video songs", "Keep video results out of the song lists."),
            sw("hideYoutubeShorts", "Hide Shorts", "Remove Shorts shelves from Home."),
        ),
        group(
            "ARTIST PAGE",
            sw("showArtistDescription", "Artist description", "Show the biography on an artist page.", true),
            sw("showArtistSubscriberCount", "Subscriber count", "Show the follower count on an artist page.", true),
        ),
        group(
            "LYRICS PROVIDERS",
            sw("lyricsProviderLrcLib", "LrcLib", "Use this provider in the lyrics search.", true),
            sw("lyricsProviderBetterLyrics", "BetterLyrics", "Use this provider in the lyrics search.", true),
            sw("lyricsProviderYouLyPlus", "YouLyPlus", "Use this provider in the lyrics search.", true),
            sw("lyricsProviderKuGou", "KuGou", "Use this provider in the lyrics search.", true),
            sw("lyricsProviderMusixmatch", "Musixmatch", "Use this provider in the lyrics search.", true),
            sw("lyricsProviderPaxsenix", "Paxsenix", "Use this provider in the lyrics search.", true),
            sw("lyricsProviderUnison", "Unison", "Use this provider in the lyrics search.", true),
        ),
    )
    SettingsSection.V155_LANGUAGE -> page(
        "Language", "The language of the app",
        group(
            "APP LANGUAGE",
            dd("language", "Language", appLanguageChoices, "en", "The interface language. Restart to apply."),
        ),
    )
    SettingsSection.V155_AI -> page(
        "AI lyrics & translation", "Provider, key and translation",
        group(
            "PROVIDER",
            dd(
                "aiProvider", "Provider",
                listOf(
                    "OpenRouter" to "OpenRouter",
                    "OpenAI" to "OpenAI",
                    "Perplexity" to "Perplexity",
                    "Claude" to "Claude",
                    "Gemini" to "Google Gemini",
                    "XAi" to "xAI",
                    "Mistral" to "Mistral",
                    "DeepL" to "DeepL",
                    "Custom" to "Custom",
                ),
                "OpenRouter",
            ),
            txt("aiApiKey", "API key", "", "Stored in settings.json."),
            txt("aiBaseUrl", "Base URL", "https://openrouter.ai/api/v1/chat/completions"),
            txt("aiModel", "Model", "google/gemini-2.5-flash-lite"),
        ),
        group(
            "TRANSLATION",
            dd("translateLanguage", "Target language", languageChoices, "en"),
            dd(
                "translateMode", "Translation mode",
                listOf("Literal" to "Literal", "Transcribed" to "Transcribed"),
                "Literal",
            ),
            txt("deeplApiKey", "DeepL key", "", "Used only by the DeepL provider."),
            dd(
                "deeplFormality", "DeepL formality",
                listOf("default" to "Default", "more" to "More formal", "less" to "Less formal"),
                "default",
            ),
        ),
    )
    SettingsSection.V155_PRIVACY -> page(
        "Privacy", "History and search",
        group(
            "HISTORY",
            sw("pauseListenHistory", "Pause listen history", "Do not record new listens."),
            sw("pauseSearchHistory", "Pause search history", "Do not record new searches."),
        ),
        group(
            "SEARCH",
            txt("clearSearchHistory", "Search history", "Managed from the search bar", "The 1.55 clear action removes the saved searches."),
        ),
    )
    SettingsSection.V155_STORAGE -> page(
        "Storage", "Caches and library folders",
        group(
            "CACHES",
            sl("albumArtCacheLimitMb", "Album art cache", 50f..1000f, 19, 200f, "Megabytes.", LegacyCommon.ALBUM_ART_CACHE),
            sl("audioCacheLimitMb", "Audio cache", 0f..4096f, 16, 1024f, "Megabytes."),
            sw("audioCacheEnabled", "Keep audio cache", "Store played catalogue tracks.", true, LegacyCommon.AUDIO_CACHE),
        ),
        group(
            "LIBRARY",
            txt("musicDirectories", "Music folders", "Configured in Music Management", "The local library roots."),
            sw("autoScanLrcFiles", "Auto-scan lyric files", "Assign .lrc and .ttml files beside a track."),
            txt("librarySize", "Library", "Read from Music Management", "Songs, albums and playlists."),
        ),
    )

    SettingsSection.V155_DATA_SAVER -> page(
        "Data saver", "Less data and canvas backups",
        group(
            "DATA SAVER",
            sw("dataSaver", "Data saver", "Prefer lower-resolution artwork and streams."),
        ),
        group(
            "TURNS OFF",
            txt("dataSaverPlayerCanvas", "Player canvas", "Off while data saver is on"),
            txt("dataSaverArtistVideo", "Artist video", "Off while data saver is on"),
            txt("dataSaverArtistBgVideo", "Artist background video", "Off while data saver is on"),
            txt("dataSaverAlbumCanvas", "Album canvas", "Off while data saver is on"),
            txt("dataSaverHighQualityImages", "High-quality images", "Off while data saver is on"),
        ),
    )
    SettingsSection.V155_BACKUP -> page(
        "Backup & restore", "Automatic backups and restore",
        group(
            "AUTOMATIC BACKUPS",
            sw("autoBackupEnabled", "Automatic backups", "Write a full backup once a week.", true),
            sw("autoBackupWeekly", "Weekly backup", "One backup every seven days.", true),
            sw("backupBeforeUpdate", "Before an update", "Back up before downloading an installer.", true),
            txt("lastAutoBackupAt", "Last automatic backup", "Never", "Read-only status."),
        ),
        group(
            "MANUAL",
            txt("backupFile", "Backup file", ".vivide.lbkp", "The full backup written by the 1.55 surface."),
        ),
    )

    SettingsSection.V155_WRAPPED -> page(
        "Wrapped", "Your listening year",
        group(
            "WRAPPED",
            sw("showWrappedOnHome", "Show on Home", "Put the Wrapped card on the Home screen."),
            txt("wrappedTracks", "Tracks started", "0", "Read-only summary."),
            txt("wrappedMinutes", "Minutes listened", "0", "Read-only summary."),
            txt("wrappedTopArtist", "Top artist", "Not enough plays", "Read-only summary."),
        ),
    )

    SettingsSection.V155_INTEGRATIONS -> page(
        "Integrations", "Discord and Last.fm",
        group(
            "DISCORD",
            sw("discordRpcEnabled", "Discord Rich Presence", "Show the current track in Discord."),
            txt("discordClientId", "Discord client ID", "", "Your own Discord application id."),
        ),
        group(
            "LAST.FM",
            sw("lastfmEnabled", "Last.fm scrobbling", "Send the tracks you play to Last.fm."),
            txt("lastfmSession", "Session", "Not connected", "The scrobbling session."),
            sw("lastfmNowPlaying", "Now playing", "Send the now-playing update as well.", true),
        ),
    )
    SettingsSection.V155_SPOTIFY -> page(
        "Spotify import", "Import playlists from Spotify",
        group(
            "SPOTIFY",
            txt("spotifyAccount", "Account", "Not connected", "The Spotify profile used for the import."),
            sw("spotifyLayout", "Spotify layout", "Group the playlist import at the top of Library.", true),
            txt("spotifyImportState", "Last import", "Never", "Read-only status."),
        ),
    )

    SettingsSection.V155_DESKTOP -> page(
        "Desktop features", "Widget, media keys and tray",
        group(
            "SYSTEM INTEGRATION",
            sw("showNowPlayingWidget", "Now-playing widget", "A small always-on-top card on the desktop."),
            sw("mediaKeysEnabled", "Media keys", "Let the keyboard's media keys control playback.", true),
            sw("trayMenuEnabled", "Tray menu", "Keep playback controls in the system tray.", true),
        ),
    )
    SettingsSection.V155_SYSTEM -> page(
        "System", "Performance, developer tools and intro",
        group(
            "SYSTEM",
            go("developer", "Developer options", "Heap, overlays, profiles and logs", SettingsSection.V155_DEVELOPER),
            go("intro", "Intro", "The splash shown before the shell", SettingsSection.INTRO),
            txt("appVersion", "Version", "1.56.0-beta5", "Read-only status."),
            txt("javaVersion", "Runtime", "Java 21", "Read-only status."),
        ),
        group(
            "PERFORMANCE",
            sw("animationsEnabled", "Animations", "Play the interface animations.", true),
            sl("jvmHeapMb", "JVM heap", 512f..8192f, 30, 2048f, "Megabytes; 0 keeps the runtime default."),
            sw("detailedStartupLogs", "Detailed startup logs", "Time every startup stage."),
            sw("detailedPlaybackLogging", "Detailed playback logs", "Time every writer pass."),
        ),
    )

    SettingsSection.V155_ABOUT -> page(
        "About", "Version, contributors and licenses",
        group(
            "ABOUT",
            txt("appName", "App", "Axolotl Music", "Read-only status."),
            txt("appVersion", "Version", "1.56.0-beta5", "Read-only status."),
            txt("appBuild", "Build", "454", "Read-only status."),
            txt("officialSite", "Website", "https://piboh.github.io/axolotl-music/", "Read-only status."),
            txt("repository", "Repository", "https://github.com/PiBOH/axolotl-music", "Read-only status."),
        ),
    )

    SettingsSection.V155_THEME -> page(
        "Theme", "Mode, accent and palette",
        group(
            "MODE",
            dd("themeMode", "Theme mode", themeChoices, "dark"),
            sw("darkMode", "Dark theme", "Use the dark Material 3 scheme.", true, LegacyCommon.DARK_MODE),
            sw("pureBlack", "Pure black", "Use black surfaces in the dark theme."),
        ),
        group(
            "COLOUR",
            sw("dynamicColor", "Dynamic color", "Tint the interface from the artwork.", true, LegacyCommon.DYNAMIC_COLOR),
            dd("albumArtPaletteStyle", "Album art palette", paletteChoices, "tonal_spot", common = LegacyCommon.PALETTE_STYLE),
            sl("accentIntensity", "Accent intensity", 0f..1f, 10, 1f),
            txt("accentColor", "Accent", "From the artwork", "Read-only preview."),
        ),
    )
    SettingsSection.V155_FONT -> page(
        "App font", "App font and lyrics",
        group(
            "APP FONT",
            dd(
                "selectedFont", "App font",
                listOf(
                    "system" to "System",
                    "google_sans" to "Google Sans",
                    "sans_flex" to "Sans Flex",
                    "outfit" to "Outfit",
                    "plus_jakarta_sans" to "Plus Jakarta Sans",
                    "custom" to "Custom font",
                ),
                "system",
            ),
            txt("customFontPath", "Custom font", "", "Path to a .ttf or .otf file."),
            sw("lyricsUseAppFont", "Use the app font in lyrics", "Draw lyrics with the same face.", true),
        ),
    )
    SettingsSection.V155_CANVAS -> page(
        "ViviMusic Canvas", "Animated artwork",
        group(
            "CANVAS",
            sw("canvasEnabled", "ViviMusic Canvas", "Play the animated artwork.", true),
            dd(
                "canvasSource", "Canvas source",
                listOf(
                    "AUTO" to "Automatic",
                    "APPLE_MUSIC" to "Apple Music",
                    "VIVIMUSIC" to "ViviMusic",
                    "TIDAL" to "Tidal",
                ),
                "AUTO",
            ),
        ),
    )
    SettingsSection.V155_PLAYER_DESIGN -> page(
        "Player & mini-player design", "Player, mini player and slider",
        group(
            "PLAYER",
            dd(
                "playerDesign", "Player design",
                listOf(
                    "classic" to "Classic",
                    "new" to "New",
                    "expressive" to "Expressive",
                    "expressive_translucent" to "Expressive (translucent tab)",
                ),
                "classic",
            ),
            dd(
                "playerBackground", "Player background",
                listOf(
                    "canvas" to "Canvas",
                    "gradient" to "Gradient",
                    "blur" to "Blur",
                    "glow" to "Glow",
                    "apple_music" to "Apple Music",
                    "live_mesh" to "Live mesh",
                    "visualizer" to "Visualizer",
                ),
                "canvas",
            ),
            sw("rotatingThumbnail", "Rotating artwork", "Spin the artwork while a track plays."),
        ),
        group(
            "MINI PLAYER",
            dd(
                "miniPlayerDesign", "Mini player design",
                listOf("classic" to "Classic", "new" to "New", "apple" to "Apple"),
                "classic",
            ),
            dd(
                "miniPlayerBackgroundStyle", "Mini player background",
                listOf(
                    "follow_theme" to "Follow theme",
                    "gradient" to "Gradient",
                    "blur" to "Blur",
                    "glow_motion" to "Glow motion",
                    "live_mesh" to "Live mesh",
                ),
                "follow_theme",
            ),
            sw("pureBlackMiniPlayer", "Pure black mini player", "Use a black bar in the dark theme."),
            sw("swipeThumbnail", "Swipe to change song", "Swipe the artwork.", true),
            sl("swipeSensitivity", "Swipe sensitivity", 0.1f..1f, 9, 0.73f),
        ),
        group(
            "SLIDER",
            dd(
                "sliderStyle", "Slider style",
                listOf("slim" to "Slim", "squiggly" to "Squiggly", "wavy" to "Wavy"),
                "slim",
            ),
        ),
    )
    SettingsSection.V155_DENSITY -> page(
        "Density & grid", "Interface scale and grid size",
        group(
            "DENSITY",
            dd(
                "densityScale", "Density & grid",
                listOf(
                    "2.0" to "200%", "1.8" to "180%", "1.5" to "150%", "1.4" to "140%",
                    "1.3" to "130%", "1.25" to "125%", "1.2" to "120%", "1.1" to "110%",
                    "1" to "100%", "0.85" to "85%", "0.75" to "75%", "0.65" to "65%",
                    "0.55" to "55%",
                ),
                "1",
                common = LegacyCommon.UI_SCALE,
            ),
            dd(
                "gridItemSize", "Grid item size",
                listOf("140" to "Small", "160" to "Medium", "200" to "Large", "240" to "Extra large"),
                "160",
            ),
        ),
    )

    SettingsSection.V155_TRANSITIONS -> page(
        "Screen transitions", "Fade, slide or off",
        group(
            "TRANSITION",
            dd(
                "screenTransition", "Screen transitions",
                listOf("off" to "Off", "fade" to "Fade", "slide" to "Slide"),
                "fade",
            ),
        ),
    )

    SettingsSection.V155_ROMANIZE -> page(
        "Romanization", "Per-language lyrics",
        group(
            "LANGUAGES",
            *romanizationLanguages.map { (label, key) ->
                sw(key, label, "Romanize $label lyrics.", true)
            }.toTypedArray(),
        ),
        group(
            "BEHAVIOUR",
            sw("lyricsRomanizeAsMain", "Romanized as main", "Show the romanized line in place of the original."),
            sw("showLyricsRomanization", "Romanization", "Master switch for the romanized line.", true, LegacyCommon.SHOW_ROMANIZATION),
        ),
    )

    SettingsSection.V155_DEVELOPER -> page(
        "Developer options", "Heap, overlays and logs",
        group(
            "DEVELOPER",
            sw("developerOptions", "Enable developer options", "Show the developer tools."),
            dd(
                "devToolsMode", "Tools mode",
                listOf("OVERLAY" to "Overlay", "WINDOW" to "Window", "CONSOLE" to "Console"),
                "OVERLAY",
            ),
            sw("devOverlayMovable", "Movable overlay", "Drag the overlay around the window.", true),
            sw("devShowInTitleBar", "Show in title bar", "Put the stats line in the title bar."),
            dd(
                "devProfile", "Profile",
                listOf("FULL" to "Full", "COMPACT" to "Compact", "MINIMAL" to "Minimal"),
                "FULL",
            ),
        ),
        group(
            "LOGS",
            sw("detailedStartupLogs", "Detailed startup logs", "Time every startup stage."),
            sw("detailedPlaybackLogging", "Detailed playback logs", "Time every writer pass."),
            sw("superLogsWriter", "Writer logs", "Log every audio writer pass."),
        ),
    )

    SettingsSection.V155_EQUALIZER -> page(
        "Equalizer", "10 bands and presets",
        group(
            "EQUALIZER",
            sw("equalizerEnabled", "Enable equalizer", "Apply the 10-band chain.", false, LegacyCommon.EQUALIZER_ENABLED),
            dd(
                "equalizerPreset", "Preset",
                listOf("flat" to "Flat", "rock" to "Rock", "pop" to "Pop", "jazz" to "Jazz", "classical" to "Classical", "bass" to "Bass boost"),
                "flat",
            ),
            sl("equalizerPreamp", "Preamp", -12f..12f, 24, 0f, "Decibels."),
        ),
        group(
            "BANDS",
            sl("eq_31", "31 Hz", -12f..12f, 24, 0f),
            sl("eq_62", "62 Hz", -12f..12f, 24, 0f),
            sl("eq_125", "125 Hz", -12f..12f, 24, 0f),
            sl("eq_250", "250 Hz", -12f..12f, 24, 0f),
            sl("eq_500", "500 Hz", -12f..12f, 24, 0f),
            sl("eq_1k", "1 kHz", -12f..12f, 24, 0f),
            sl("eq_2k", "2 kHz", -12f..12f, 24, 0f),
            sl("eq_4k", "4 kHz", -12f..12f, 24, 0f),
            sl("eq_8k", "8 kHz", -12f..12f, 24, 0f),
            sl("eq_16k", "16 kHz", -12f..12f, 24, 0f),
        ),
    )

    else -> page(section.title, section.subtitle)
}

@Composable
fun Legacy155SettingsScreen(
    section: SettingsSection,
    viewModel: PlayerViewModel,
    onBack: () -> Unit,
    onNavigate: (SettingsSection) -> Unit,
    modifier: Modifier = Modifier,
) {
    val spec = legacyPageFor(section)
    val settings by viewModel.userSettings.collectAsState()

    Column(modifier = modifier.fillMaxSize().testTag("legacy155-page")) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(start = 10.dp, top = 10.dp, end = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            AppTooltip("Back") {
                IconButton(onClick = onBack) {
                    Icon(
                        painter = painterResource(Res.drawable.ic_automirrored_rounded_arrow_back),
                        contentDescription = "Back",
                        tint = MaterialTheme.colorScheme.onSurface,
                    )
                }
            }
            SettingsHeader(
                title = spec.title,
                subtitle = spec.subtitle,
                horizontalPadding = 0.dp,
                modifier = Modifier.weight(1f),
            )
        }

        LazyColumn(
            modifier = Modifier.fillMaxWidth().weight(1f),
            contentPadding = PaddingValues(horizontal = 28.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            spec.sections.forEach { sectionSpec ->
                item(key = sectionSpec.title) {
                    SettingsSubsection(title = sectionSpec.title) {
                        sectionSpec.options.forEachIndexed { index, option ->
                            Legacy155Row(
                                option = option,
                                index = index,
                                count = sectionSpec.options.size,
                                settings = settings,
                                viewModel = viewModel,
                                onNavigate = onNavigate,
                            )
                        }
                    }
                }
            }
            item { Spacer(Modifier.height(16.dp)) }
        }
    }
}

@Composable
private fun Legacy155Row(
    option: LegacyOption,
    index: Int,
    count: Int,
    settings: UserSettings,
    viewModel: PlayerViewModel,
    onNavigate: (SettingsSection) -> Unit,
) {
    val colors = MaterialTheme.colorScheme
    val shape = getGroupShape(index, count)
    val raw = option.common?.let { commonValue(it, settings) }
        ?: settings.legacySettings[option.key]
        ?: option.default
    val write: (String) -> Unit = { value ->
        if (option.common != null) writeCommon(viewModel, option.common, value)
        else viewModel.setLegacySetting(option.key, value)
    }

    when (option.kind) {
        LegacyKind.SWITCH -> {
            val checked = raw.toBoolean()
            Surface(
                onClick = { write((!checked).toString()) },
                shape = shape,
                color = colors.surfaceContainer,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("legacy-${option.key}")
                    .semantics { toggleableState = if (checked) ToggleableState.On else ToggleableState.Off },
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth().padding(16.dp),
                ) {
                    Column(Modifier.weight(1f).padding(end = 12.dp)) {
                        Text(option.title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                        if (option.subtitle.isNotBlank()) {
                            Text(option.subtitle, style = MaterialTheme.typography.bodyMedium, color = colors.onSurfaceVariant)
                        }
                    }
                    Switch(
                        checked = checked,
                        onCheckedChange = null,
                        modifier = Modifier.testTag("legacy-switch-${option.key}"),
                    )
                }
            }
        }

        LegacyKind.DROPDOWN -> {
            var open by remember { mutableStateOf(false) }
            val label = option.choices.firstOrNull { it.first == raw }?.second ?: raw
            Surface(shape = shape, color = colors.surfaceContainer, modifier = Modifier.fillMaxWidth()) {
                Box {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth().clickable { open = true }.padding(16.dp),
                    ) {
                        Column(Modifier.weight(1f).padding(end = 12.dp)) {
                            Text(option.title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                            if (option.subtitle.isNotBlank()) {
                                Text(option.subtitle, style = MaterialTheme.typography.bodyMedium, color = colors.onSurfaceVariant)
                            }
                        }
                        Text(label, style = MaterialTheme.typography.bodyLarge, color = colors.primary)
                        Spacer(Modifier.width(6.dp))
                        Icon(
                            painter = painterResource(Res.drawable.ic_rounded_expand_more),
                            contentDescription = null,
                            tint = colors.onSurfaceVariant,
                            modifier = Modifier.size(20.dp),
                        )
                    }
                    DropdownMenu(expanded = open, onDismissRequest = { open = false }) {
                        option.choices.forEach { (value, text) ->
                            DropdownMenuItem(
                                text = { Text(text) },
                                onClick = {
                                    open = false
                                    write(value)
                                },
                            )
                        }
                    }
                }
            }
        }

        LegacyKind.TEXT -> {
            var draft by remember(raw) { mutableStateOf(raw) }
            Surface(shape = shape, color = colors.surfaceContainer, modifier = Modifier.fillMaxWidth()) {
                Column(Modifier.padding(16.dp)) {
                    Text(option.title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                    if (option.subtitle.isNotBlank()) {
                        Text(option.subtitle, style = MaterialTheme.typography.bodyMedium, color = colors.onSurfaceVariant)
                    }
                    Spacer(Modifier.height(8.dp))
                    OutlinedTextField(
                        value = draft,
                        onValueChange = {
                            draft = it
                            write(it)
                        },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
            }
        }

        LegacyKind.SLIDER -> {
            val start = raw.toFloatOrNull() ?: option.default.toFloatOrNull() ?: option.range.start
            var draft by remember(raw) { mutableStateOf(start.coerceIn(option.range)) }
            Surface(shape = shape, color = colors.surfaceContainer, modifier = Modifier.fillMaxWidth()) {
                Column(Modifier.padding(16.dp)) {
                    Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                        Column(Modifier.weight(1f)) {
                            Text(option.title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                            if (option.subtitle.isNotBlank()) {
                                Text(option.subtitle, style = MaterialTheme.typography.bodyMedium, color = colors.onSurfaceVariant)
                            }
                        }
                        Text(
                            if (draft == draft.toInt().toFloat()) draft.toInt().toString() else "%.2f".format(draft),
                            style = MaterialTheme.typography.labelLarge,
                            color = colors.primary,
                        )
                    }
                    Slider(
                        value = draft,
                        onValueChange = { draft = it },
                        onValueChangeFinished = {
                            write(if (draft == draft.toInt().toFloat()) draft.toInt().toString() else draft.toString())
                        },
                        valueRange = option.range,
                        steps = option.steps,
                        modifier = Modifier.fillMaxWidth().testTag("legacy-${option.key}"),
                    )
                }
            }
        }

        LegacyKind.ACTION -> {
            Surface(
                onClick = { option.target?.let(onNavigate) },
                shape = shape,
                color = colors.surfaceContainer,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth().padding(16.dp),
                ) {
                    Column(Modifier.weight(1f).padding(end = 12.dp)) {
                        Text(option.title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                        if (option.subtitle.isNotBlank()) {
                            Text(option.subtitle, style = MaterialTheme.typography.bodyMedium, color = colors.onSurfaceVariant)
                        }
                    }
                    Icon(
                        painter = painterResource(Res.drawable.ic_rounded_chevron_right),
                        contentDescription = null,
                        tint = colors.onSurfaceVariant,
                        modifier = Modifier.size(22.dp),
                    )
                }
            }
        }
    }
}

private fun commonValue(common: LegacyCommon, settings: UserSettings): String = when (common) {
    LegacyCommon.DARK_MODE -> settings.isDarkMode.toString()
    LegacyCommon.DYNAMIC_COLOR -> settings.isDynamicColorEnabled.toString()
    LegacyCommon.UI_SCALE -> settings.uiScale.toString()
    LegacyCommon.SHOW_PLAYER_INFO -> settings.showPlayerFileInfo.toString()
    LegacyCommon.CROSSFADE -> settings.crossfadeEnabled.toString()
    LegacyCommon.CROSSFADE_DURATION -> settings.crossfadeDurationSeconds.toString()
    LegacyCommon.AUDIO_QUALITY -> settings.audioQuality
    LegacyCommon.AUDIO_CACHE -> settings.audioCacheEnabled.toString()
    LegacyCommon.LYRICS_ALIGNMENT -> settings.lyricsAlignment
    LegacyCommon.LYRICS_TEXT_SIZE -> settings.lyricsTextSize
    LegacyCommon.SHOW_ROMANIZATION -> settings.showLyricsRomanization.toString()
    LegacyCommon.SHOW_INTRO -> settings.showIntroSplash.toString()
    LegacyCommon.INTRO_STYLE -> settings.introStyle
    LegacyCommon.INTRO_BACKGROUND -> settings.introBackground
    LegacyCommon.CLOSE_TO_TRAY -> settings.closeToTray.toString()
    LegacyCommon.EQUALIZER_ENABLED -> settings.equalizerEnabled.toString()
    LegacyCommon.ALBUM_ART_CACHE -> settings.albumArtCacheLimitMb.toString()
    LegacyCommon.PALETTE_STYLE -> settings.albumArtPaletteStyle
}

private fun writeCommon(viewModel: PlayerViewModel, common: LegacyCommon?, value: String) {
    when (common) {
        LegacyCommon.DARK_MODE -> viewModel.setDarkMode(value.toBoolean())
        LegacyCommon.DYNAMIC_COLOR -> viewModel.setDynamicColorEnabled(value.toBoolean())
        LegacyCommon.UI_SCALE -> viewModel.setUiScale(value.toFloat())
        LegacyCommon.SHOW_PLAYER_INFO -> viewModel.setShowPlayerFileInfo(value.toBoolean())
        LegacyCommon.CROSSFADE -> viewModel.setCrossfadeEnabled(value.toBoolean())
        LegacyCommon.CROSSFADE_DURATION -> viewModel.setCrossfadeDurationSeconds(value.toInt())
        LegacyCommon.AUDIO_QUALITY -> viewModel.setAudioQuality(value)
        LegacyCommon.AUDIO_CACHE -> viewModel.setAudioCacheEnabled(value.toBoolean())
        LegacyCommon.LYRICS_ALIGNMENT -> viewModel.setLyricsAlignment(value)
        LegacyCommon.LYRICS_TEXT_SIZE -> viewModel.setLyricsTextSize(value)
        LegacyCommon.SHOW_ROMANIZATION -> viewModel.setShowLyricsRomanization(value.toBoolean())
        LegacyCommon.SHOW_INTRO -> viewModel.setShowIntroSplash(value.toBoolean())
        LegacyCommon.INTRO_STYLE -> viewModel.setIntroStyle(value)
        LegacyCommon.INTRO_BACKGROUND -> viewModel.setIntroBackground(value)
        LegacyCommon.CLOSE_TO_TRAY -> viewModel.setCloseToTray(value.toBoolean())
        LegacyCommon.EQUALIZER_ENABLED -> viewModel.setEqualizerEnabled(value.toBoolean())
        LegacyCommon.ALBUM_ART_CACHE -> viewModel.setAlbumArtCacheLimitMb(value.toInt())
        LegacyCommon.PALETTE_STYLE -> viewModel.setAlbumArtPaletteStyle(AlbumArtPaletteStyle.fromStorageKey(value))
        null -> Unit
    }
}
