package com.music.vivi.desktop

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.LocalScrollbarStyle
import androidx.compose.foundation.VerticalScrollbar
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.rememberScrollbarAdapter
import androidx.compose.ui.input.pointer.AwaitPointerEventScope
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.SystemUpdate
import androidx.compose.material.icons.filled.Forum
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.PlayCircle
import androidx.compose.material.icons.filled.SwapVert
import androidx.compose.material.icons.filled.GTranslate
import androidx.compose.material.icons.filled.FormatSize
import androidx.compose.material.icons.filled.FormatLineSpacing
import androidx.compose.material.icons.filled.Animation
import androidx.compose.material.icons.automirrored.filled.FormatAlignLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.RocketLaunch
import androidx.compose.material.icons.filled.ReceiptLong
import androidx.compose.material.icons.filled.Android
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Memory
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.DesktopWindows
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.ShowChart
import androidx.compose.material.icons.filled.UploadFile
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Block
import androidx.compose.material.icons.filled.BlurOn
import androidx.compose.material.icons.filled.EnergySavingsLeaf
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.Public
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.ModelTraining
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.Link
import androidx.compose.material.icons.filled.Lyrics
import androidx.compose.material.icons.filled.Movie
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.PictureInPicture
import androidx.compose.material.icons.filled.Keyboard
import androidx.compose.material.icons.filled.Dock
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.SmartDisplay
import androidx.compose.material.icons.filled.TouchApp
import androidx.compose.material.icons.filled.Translate
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.filled.VerticalAlignCenter
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.key
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.awaitTouchSlopOrCancellation
import androidx.compose.foundation.gestures.drag
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.input.pointer.PointerIcon
import androidx.compose.ui.input.pointer.pointerHoverIcon
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.sp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.music.innertube.models.YouTubeLocale
import com.music.lyrics.LyricsRomanizer
import java.io.File
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.math.hypot
import kotlin.math.roundToInt
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Devices
import androidx.compose.material.icons.filled.Group
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.SettingsBackupRestore
import androidx.compose.material.icons.filled.Storage
import androidx.compose.material.icons.filled.SmartToy
import androidx.compose.material.icons.outlined.AutoAwesome
import androidx.compose.material.icons.outlined.Group
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.ui.input.key.key
import com.music.innertube.YouTube
import kotlinx.coroutines.flow.map

/** YouTube host-language codes (mirrors the Android app's `LanguageCodeToName`). */
val LanguageCodeToName: Map<String, String> = mapOf(
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

/** YouTube geolocation codes (mirrors the Android app's `CountryCodeToName`). */
val CountryCodeToName: Map<String, String> = mapOf(
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

/**
 * Resolve the innerTube locale from the saved content language/country.
 *
 * The content language is its own setting, so when the user picked one it wins.
 * Blank means "follow" and it used to follow the **OS** locale, which is how an
 * English build showed the account's own playlists in the language of Windows
 * (the sidebar listed "Liked Music" as "Musica che ti piace" and "Episodes for
 * Later" as "Puntate per dopo", a mix that reads like a bug). [appLanguage] is
 * the language of the app itself and is what a blank content language follows
 * now; only when *that* is blank too (the language picker has not run yet) does
 * the OS locale apply, and English is the last resort.
 */
fun resolveYouTubeLocale(
    contentLanguage: String,
    contentCountry: String,
    appLanguage: String,
): YouTubeLocale {
    val system = java.util.Locale.getDefault()
    val hl = contentLanguage.ifBlank {
        appLanguage.ifBlank { system.language.ifBlank { "en" } }
    }
    val gl = contentCountry.ifBlank { system.country.ifBlank { "US" } }
    return YouTubeLocale(gl = gl, hl = hl)
}

/**
 * Shared scaffold for settings sub-screens: back button + scrollable content.
 *
 * The content pane carries the app's own scrollbar, as the page does: a settings
 * screen is taller than the window often enough that where you are in it has to
 * be visible, and the bare `verticalScroll` draws nothing (the page's own bar
 * was the one thing a screen could be missing while everything else matched).
 */
@Composable
fun SettingsSubScreen(language: String, onBack: () -> Unit, content: @Composable () -> Unit) {
    val scrollState = rememberScrollState()
    Box(Modifier.fillMaxSize()) {
        Column(
            Modifier
                .fillMaxSize()
                .verticalScroll(scrollState)
                .padding(14.dp),
        ) {
            BackButton(language, onBack)
            content()
        }
        CompositionLocalProvider(LocalScrollbarStyle provides themedScrollbarStyle()) {
            VerticalScrollbar(
                adapter = rememberScrollbarAdapter(scrollState),
                modifier = Modifier.align(Alignment.CenterEnd).fillMaxHeight(),
            )
        }
    }
}

@Composable
fun SettingsLanguageScreen(language: String, onBack: () -> Unit, onLanguageChange: (String) -> Unit) {
    SettingsSubScreen(language, onBack) { LanguageSection(language, onLanguageChange) }
}

@Composable
fun SettingsAppearanceScreen(
    language: String,
    onBack: () -> Unit,
    onOpenTheme: () -> Unit = {},
    onOpenFont: () -> Unit = {},
    onOpenCanvas: () -> Unit = {},
    onOpenDensity: () -> Unit = {},
    onOpenTransitions: () -> Unit = {},
    onOpenPlayerDesign: () -> Unit = {},
    onOpenIntro: () -> Unit = {},
    nativeTitleBar: Boolean = false,
    onNativeTitleBarChange: (Boolean) -> Unit = {},
    showRightSidebar: Boolean = true,
    onShowRightSidebarChange: (Boolean) -> Unit = {},
    onRestart: () -> Unit = {},
) {
    SettingsSubScreen(language, onBack) {
        AppearanceSection(
            language = language,
            onOpenTheme = onOpenTheme,
            onOpenFont = onOpenFont,
            onOpenCanvas = onOpenCanvas,
            onOpenDensity = onOpenDensity,
            onOpenTransitions = onOpenTransitions,
            onOpenPlayerDesign = onOpenPlayerDesign,
            onOpenIntro = onOpenIntro,
            nativeTitleBar = nativeTitleBar,
            onNativeTitleBarChange = onNativeTitleBarChange,
            showRightSidebar = showRightSidebar,
            onShowRightSidebarChange = onShowRightSidebarChange,
            onRestart = onRestart,
        )
    }
}

@Composable
fun SettingsFontScreen(
    language: String,
    onBack: () -> Unit,
    selectedFont: AppFont,
    onFontChange: (AppFont) -> Unit,
    customFontPath: String = "",
    onImportFont: () -> Unit = {},
    useFontInLyrics: Boolean = true,
    onUseFontInLyricsChange: (Boolean) -> Unit = {},
) {
    SettingsSubScreen(language, onBack) {
        FontSection(
            language = language,
            selectedFont = selectedFont,
            onFontChange = onFontChange,
            customFontPath = customFontPath,
            onImportFont = onImportFont,
            useFontInLyrics = useFontInLyrics,
            onUseFontInLyricsChange = onUseFontInLyricsChange,
        )
    }
}

@Composable
fun SettingsCanvasScreen(
    language: String,
    onBack: () -> Unit,
    canvasEnabled: Boolean,
    onCanvasEnabledChange: (Boolean) -> Unit,
    canvasSource: CanvasSource,
    onCanvasSourceChange: (CanvasSource) -> Unit,
) {
    SettingsSubScreen(language, onBack) {
        CanvasSection(
            language = language,
            canvasEnabled = canvasEnabled,
            onCanvasEnabledChange = onCanvasEnabledChange,
            canvasSource = canvasSource,
            onCanvasSourceChange = onCanvasSourceChange,
        )
    }
}

@Composable
fun SettingsDensityScreen(
    language: String,
    onBack: () -> Unit,
    densityScale: Float,
    onDensityScaleChange: (Float) -> Unit,
    gridItemSize: Int,
    onGridItemSizeChange: (Int) -> Unit,
) {
    SettingsSubScreen(language, onBack) {
        DensityScreen(
            language = language,
            densityScale = densityScale,
            onDensityScaleChange = onDensityScaleChange,
            gridItemSize = gridItemSize,
            onGridItemSizeChange = onGridItemSizeChange,
        )
    }
}

@Composable
fun SettingsTransitionsScreen(
    language: String,
    onBack: () -> Unit,
    screenTransition: String,
    onScreenTransitionChange: (String) -> Unit,
    animationsEnabled: Boolean = true,
    onAnimationsEnabledChange: (Boolean) -> Unit = {},
    animationSpeed: String = "normal",
    onAnimationSpeedChange: (String) -> Unit = {},
) {
    SettingsSubScreen(language, onBack) {
        TransitionsScreen(
            language = language,
            screenTransition = screenTransition,
            onScreenTransitionChange = onScreenTransitionChange,
            animationsEnabled = animationsEnabled,
            onAnimationsEnabledChange = onAnimationsEnabledChange,
            animationSpeed = animationSpeed,
            onAnimationSpeedChange = onAnimationSpeedChange,
        )
    }
}

@Composable
fun SettingsWrappedScreen(
    language: String,
    onBack: () -> Unit,
    wrappedStats: WrappedStats = WrappedStats(),
    showWrappedOnHome: Boolean = false,
    onShowWrappedOnHomeChange: (Boolean) -> Unit = {},
) {
    SettingsSubScreen(language, onBack) {
        Text(Localization.get(language, "wrapped_title"), style = MaterialTheme.typography.titleLarge, modifier = Modifier.padding(vertical = 12.dp))
        WrappedCard(wrappedStats = wrappedStats, language = language)
        Row(
            Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Switch(checked = showWrappedOnHome, onCheckedChange = onShowWrappedOnHomeChange)
            Column(Modifier.clickable { onShowWrappedOnHomeChange(!showWrappedOnHome) }) {
                Text(Localization.get(language, "wrapped_show_on_home"))
                Text(
                    Localization.get(language, "wrapped_show_on_home_desc"),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        Text(
            Localization.get(language, "wrapped_desc"),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
        )
    }
}

@Composable
fun SettingsThemeScreen(
    language: String,
    onBack: () -> Unit,
    themeMode: ThemeMode,
    accent: androidx.compose.ui.graphics.Color,
    onThemeModeChange: (ThemeMode) -> Unit,
    onAccentChange: (androidx.compose.ui.graphics.Color) -> Unit,
    accentIntensity: Float = 1f,
    onAccentIntensityChange: (Float) -> Unit = {},
    onAccentIntensityChangeFinished: () -> Unit = {},
    pureBlack: Boolean,
    onPureBlackChange: (Boolean) -> Unit,
    customAccents: List<Int> = emptyList(),
    onAddCustomAccent: (Int) -> Unit = {},
    onRemoveCustomAccent: (Int) -> Unit = {},
) {
    SettingsSubScreen(language, onBack) {
        ThemeSection(
            language = language,
            mode = themeMode,
            accent = accent,
            onModeChange = onThemeModeChange,
            onAccentChange = onAccentChange,
            accentIntensity = accentIntensity,
            onAccentIntensityChange = onAccentIntensityChange,
            onAccentIntensityChangeFinished = onAccentIntensityChangeFinished,
            pureBlack = pureBlack,
            onPureBlackChange = onPureBlackChange,
            customAccents = customAccents,
            onAddCustomAccent = onAddCustomAccent,
            onRemoveCustomAccent = onRemoveCustomAccent,
        )
    }
}

@Composable
fun SettingsPlayerScreen(
    language: String,
    onBack: () -> Unit,
    autoPlayNext: Boolean,
    onToggleAutoPlayNext: (Boolean) -> Unit,
    autoLoadMore: Boolean,
    onToggleAutoLoadMore: (Boolean) -> Unit,
    preventDuplicateTracksInQueue: Boolean,
    onTogglePreventDuplicateTracksInQueue: (Boolean) -> Unit,
    autoSkipNextOnError: Boolean,
    onToggleAutoSkipNextOnError: (Boolean) -> Unit,
    pauseWhenMediaMuted: Boolean,
    onTogglePauseWhenMediaMuted: (Boolean) -> Unit,
    keepScreenOnWhenPlayerExpanded: Boolean,
    onToggleKeepScreenOnWhenPlayerExpanded: (Boolean) -> Unit,
    persistentShuffle: Boolean,
    onTogglePersistentShuffle: (Boolean) -> Unit,
    progressiveSeek: Boolean,
    onToggleProgressiveSeek: (Boolean) -> Unit,
    autoDownloadOnLike: Boolean,
    onToggleAutoDownloadOnLike: (Boolean) -> Unit,
    historyDurationSeconds: Int,
    onHistoryDurationSecondsChange: (Int) -> Unit,
    skipSilence: Boolean,
    onToggleSkipSilence: (Boolean) -> Unit,
    skipSilenceInstant: Boolean,
    onToggleSkipSilenceInstant: (Boolean) -> Unit,
    crossfade: Boolean,
    onToggleCrossfade: (Boolean) -> Unit,
    crossfadeDurationSeconds: Int,
    onCrossfadeDurationSecondsChange: (Int) -> Unit,
    disableCrossfadeGapless: Boolean,
    onToggleDisableCrossfadeGapless: (Boolean) -> Unit,
    audioQuality: String,
    onAudioQualityChange: (String) -> Unit,
    rememberShuffleRepeat: Boolean,
    onToggleRememberShuffleRepeat: (Boolean) -> Unit,
    persistentQueue: Boolean,
    onTogglePersistentQueue: (Boolean) -> Unit,
    onOpenEqualizer: () -> Unit = {},
    streamCacheMinutes: Int = 10,
    onStreamCacheMinutesChange: (Int) -> Unit = {},
) {
    SettingsSubScreen(language, onBack) {
        PlayerSection(
            language,
            autoPlayNext,
            onToggleAutoPlayNext,
            autoLoadMore,
            onToggleAutoLoadMore,
            preventDuplicateTracksInQueue,
            onTogglePreventDuplicateTracksInQueue,
            autoSkipNextOnError,
            onToggleAutoSkipNextOnError,
            pauseWhenMediaMuted,
            onTogglePauseWhenMediaMuted,
            keepScreenOnWhenPlayerExpanded,
            onToggleKeepScreenOnWhenPlayerExpanded,
            persistentShuffle,
            onTogglePersistentShuffle,
            progressiveSeek,
            onToggleProgressiveSeek,
            autoDownloadOnLike,
            onToggleAutoDownloadOnLike,
            historyDurationSeconds,
            onHistoryDurationSecondsChange,
            skipSilence,
            onToggleSkipSilence,
            skipSilenceInstant,
            onToggleSkipSilenceInstant,
            crossfade,
            onToggleCrossfade,
            crossfadeDurationSeconds,
            onCrossfadeDurationSecondsChange,
            disableCrossfadeGapless,
            onToggleDisableCrossfadeGapless,
            audioQuality,
            onAudioQualityChange,
            rememberShuffleRepeat,
            onToggleRememberShuffleRepeat,
            persistentQueue,
            onTogglePersistentQueue,
            onOpenEqualizer,
            streamCacheMinutes,
            onStreamCacheMinutesChange,
        )
    }
}

@Composable
fun SettingsAccountScreen(
    language: String,
    onBack: () -> Unit,
    isLoggedIn: Boolean,
    onOpenLogin: () -> Unit,
    onLogout: () -> Unit,
    onLoggedIn: () -> Unit,
) {
    SettingsSubScreen(language, onBack) {
        AccountSection(language, isLoggedIn, onOpenLogin, onLogout, onLoggedIn)
    }
}

@Composable
fun SettingsDevicesScreen(
    language: String,
    onBack: () -> Unit,
    syncManager: DesktopSyncManager,
    syncViviVolume: Boolean,
    onToggleSyncViviVolume: (Boolean) -> Unit,
    syncNativeVolume: Boolean,
    onToggleSyncNativeVolume: (Boolean) -> Unit,
) {
    SettingsSubScreen(language, onBack) {
        DeviceSyncSection(
            language,
            syncManager,
            syncViviVolume,
            onToggleSyncViviVolume,
            syncNativeVolume,
            onToggleSyncNativeVolume,
        )
    }
}

@Composable
fun SettingsUpdatesScreen(
    language: String,
    onBack: () -> Unit,
    updateStatus: UpdateStatus,
    includePreReleases: Boolean,
    updateIntervalHours: Int,
    updateSource: String,
    onIntervalChange: (Int) -> Unit,
    onTogglePreReleases: (Boolean) -> Unit,
    onUpdateSourceChange: (String) -> Unit,
    onCheckUpdates: () -> Unit,
    onOpenChangelog: () -> Unit,
    onOpenCommits: () -> Unit,
) {
    // Check for updates every time the section is opened.
    LaunchedEffect(Unit) { onCheckUpdates() }
    SettingsSubScreen(language, onBack) {
        UpdateSection(
            language,
            updateStatus,
            includePreReleases,
            updateIntervalHours,
            updateSource,
            onIntervalChange,
            onTogglePreReleases,
            onUpdateSourceChange,
            onCheckUpdates,
            onOpenChangelog,
            onOpenCommits,
        )
    }
}

@Composable
fun SettingsAboutScreen(
    language: String,
    onBack: () -> Unit,
    onOpenContributors: () -> Unit,
    onOpenLicense: () -> Unit,
) {
    SettingsSubScreen(language, onBack) {
        AboutSection(
            language = language,
            onOpenContributors = onOpenContributors,
            onOpenLicense = onOpenLicense,
        )
    }
}

@Composable
fun SettingsLicenseScreen(language: String, onBack: () -> Unit) {
    SettingsSubScreen(language, onBack) { LicenseSection(language) }
}

@Composable
fun SettingsContributorsScreen(language: String, onBack: () -> Unit) {
    SettingsSubScreen(language, onBack) { ContributorsSection(language) }
}

@Composable
fun SettingsPerformanceScreen(language: String, onBack: () -> Unit, syncManager: DesktopSyncManager) {
    SettingsSubScreen(language, onBack) { PerformanceSection(language, syncManager) }
}

/** The real Developer options screen, reachable only after the seven-tap unlock. */
@Composable
fun SettingsDeveloperOptionsScreen(language: String, onBack: () -> Unit) {
    SettingsSubScreen(language, onBack) { DeveloperOptionsSection(language, onDisabled = onBack) }
}

@Composable
fun SettingsIntroScreen(
    language: String,
    onBack: () -> Unit,
    showIntroSplash: Boolean,
    onShowIntroSplashChange: (Boolean) -> Unit,
    introStyle: String,
    onIntroStyleChange: (String) -> Unit,
    introBackground: String,
    onIntroBackgroundChange: (String) -> Unit,
) {
    var previewing by remember { mutableStateOf(false) }

    // Fullscreen preview of the actual startup intro (click or end dismisses it).
    if (previewing) {
        Dialog(
            onDismissRequest = { previewing = false },
            properties = DialogProperties(usePlatformDefaultWidth = false),
        ) {
            IntroSplash(
                language = language,
                style = introStyle,
                background = introBackground,
                onFinished = { previewing = false },
            )
        }
    }

    SettingsSubScreen(language, onBack) {
        Text(Localization.get(language, "intro"), style = MaterialTheme.typography.titleLarge, modifier = Modifier.padding(vertical = 12.dp))

        M3SettingsGroup(
            items = listOf(
                M3SettingsItem(
                    icon = Icons.Filled.Movie,
                    title = { Text(Localization.get(language, "show_intro_on_startup")) },
                    description = { Text(Localization.get(language, "intro_desc")) },
                    trailing = { Switch(checked = showIntroSplash, onCheckedChange = onShowIntroSplashChange) },
                    onClick = { onShowIntroSplashChange(!showIntroSplash) },
                ),
            ),
        )

        Spacer(Modifier.height(16.dp))

        M3SettingsDropdownItem(
            icon = Icons.Filled.AutoAwesome,
            title = Localization.get(language, "intro_style"),
            value = Localization.get(language, when (introStyle) {
                "logo" -> "intro_style_logo"
                "logo_name" -> "intro_style_logo_name"
                else -> "intro_style_logo_tagline"
            }),
            options = listOf("logo", "logo_name", "logo_tagline").map { v ->
                v to Localization.get(language, when (v) {
                    "logo" -> "intro_style_logo"
                    "logo_name" -> "intro_style_logo_name"
                    else -> "intro_style_logo_tagline"
                })
            },
            onSelect = onIntroStyleChange,
        )

        Spacer(Modifier.height(16.dp))

        M3SettingsDropdownItem(
            icon = Icons.Filled.Palette,
            title = Localization.get(language, "intro_background"),
            value = Localization.get(language, when (introBackground) {
                "gradient" -> "intro_background_gradient"
                "glow" -> "intro_background_glow"
                else -> "intro_background_dark"
            }),
            options = listOf("gradient", "glow", "dark").map { v ->
                v to Localization.get(language, when (v) {
                    "gradient" -> "intro_background_gradient"
                    "glow" -> "intro_background_glow"
                    else -> "intro_background_dark"
                })
            },
            onSelect = onIntroBackgroundChange,
        )

        Spacer(Modifier.height(20.dp))

        Button(
            onClick = { previewing = true },
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text(Localization.get(language, "preview_intro"))
        }
    }
}

@Composable
fun SettingsStorageScreen(language: String, onBack: () -> Unit) {
    SettingsSubScreen(language, onBack) { StorageSection(language) }
}

/**
 * Backup & restore: exports/imports a full backup (settings, playlists, account
 * and library) via [BackupManager], plus the automatic-backup preferences and
 * the list of stored automatic backups.
 */
@Composable
fun SettingsBackupScreen(language: String, onBack: () -> Unit) {
    val scope = rememberCoroutineScope()
    var busy by remember { mutableStateOf(false) }
    var showRestartDialog by remember { mutableStateOf(false) }

    var autoBackupEnabled by remember(settingsFileRevision()) { mutableStateOf(DesktopSettings.load().autoBackupEnabled) }
    var autoBackupWeekly by remember(settingsFileRevision()) { mutableStateOf(DesktopSettings.load().autoBackupWeekly) }
    var autoBackupBeforeUpdate by remember(settingsFileRevision()) { mutableStateOf(DesktopSettings.load().autoBackupBeforeUpdate) }

    var backups by remember { mutableStateOf<List<File>>(emptyList()) }
    var restoreTarget by remember { mutableStateOf<File?>(null) }
    var deleteTarget by remember { mutableStateOf<File?>(null) }
    var pendingRestore by remember { mutableStateOf<File?>(null) }
    var pendingDelete by remember { mutableStateOf<File?>(null) }

    fun reloadBackups() { backups = BackupManager.listAuto() }

    LaunchedEffect(Unit) { reloadBackups() }

    // Defer destructive actions until the dialog is dismissed, so the list
    // reflows after the popup window is torn down (avoids the Compose
    // "layouts are not part of the same hierarchy" crash).
    LaunchedEffect(restoreTarget) {
        val f = pendingRestore
        if (restoreTarget == null && f != null) {
            pendingRestore = null
            // The row's own restore reports its outcome too: it used to open the
            // "restart to apply" dialog whatever happened, so a restore that did
            // nothing still looked like it had worked.
            val result = withContext(Dispatchers.IO) { BackupManager.import(f) }
            if (result.ok) showRestartDialog = true
            else DesktopSnackbar.show(Localization.get(language, result.messageKey))
        }
    }
    LaunchedEffect(deleteTarget) {
        val f = pendingDelete
        if (deleteTarget == null && f != null) {
            pendingDelete = null
            withContext(Dispatchers.IO) { BackupManager.deleteAuto(f) }
            reloadBackups()
        }
    }

    SettingsSubScreen(language, onBack) {
        Text(
            Localization.get(language, "backup_restore"),
            style = MaterialTheme.typography.titleLarge,
        )
        Text(
            Localization.get(language, "backup_restore_desc"),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(top = 4.dp),
        )

        Spacer(Modifier.height(16.dp))

        Button(
            onClick = {
                if (busy) return@Button
                busy = true
                scope.launch {
                    val file = withContext(Dispatchers.IO) { chooseBackupFile(save = true) }
                    val ok = file != null && withContext(Dispatchers.IO) { BackupManager.export(file) }
                    busy = false
                    DesktopSnackbar.show(Localization.get(language, if (ok) "backup_create_success" else "backup_create_failed"))
                }
            },
            enabled = !busy,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text(Localization.get(language, "action_backup"))
        }
        Text(
            Localization.get(language, "backup_desc"),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(top = 4.dp),
        )

        Spacer(Modifier.height(16.dp))

        Button(
            onClick = {
                if (busy) return@Button
                busy = true
                scope.launch {
                    val file = withContext(Dispatchers.IO) { chooseBackupFile(save = false) }
                    val result = if (file == null) null
                    else withContext(Dispatchers.IO) { BackupManager.import(file) }
                    busy = false
                    // The reason is no longer always the same sentence: a file
                    // that is not a DE backup (the phone's archive, say) and one
                    // whose settings entry cannot be read are different answers,
                    // and the log keeps the sentence for whoever has to dig.
                    when {
                        result == null -> Unit
                        result.ok -> showRestartDialog = true
                        else -> DesktopSnackbar.show(Localization.get(language, result.messageKey))
                    }
                }
            },
            enabled = !busy,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text(Localization.get(language, "action_restore"))
        }
        Text(
            Localization.get(language, "restore_desc"),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(top = 4.dp),
        )

        HorizontalDivider(Modifier.padding(vertical = 20.dp))

        // Automatic backups
        Text(Localization.get(language, "auto_backup"), style = MaterialTheme.typography.titleMedium)

        // The three switches are one card: they are one subject, and a row of a
        // settings screen wears the card every other setting wears instead of
        // standing on the page's own background. The two that depend on the
        // automatic backup fold away with it.
        Spacer(Modifier.height(16.dp))
        M3SettingsGroup(
            items = listOf(
                M3SettingsItem(
                    icon = Icons.Filled.Schedule,
                    title = { Text(Localization.get(language, "enable_automatic_backup")) },
                    description = { Text(Localization.get(language, "automatic_backup_desc")) },
                    trailing = {
                        Switch(
                            checked = autoBackupEnabled,
                            onCheckedChange = { v ->
                                autoBackupEnabled = v
                                DesktopSettings.update { s -> s.copy(autoBackupEnabled = v) }
                            },
                        )
                    },
                    onClick = {
                        autoBackupEnabled = !autoBackupEnabled
                        DesktopSettings.update { s ->
                            s.copy(autoBackupEnabled = !autoBackupEnabled)
                        }
                    },
                ),
                M3SettingsItem(
                    icon = Icons.Filled.DateRange,
                    title = { Text(Localization.get(language, "weekly_backup")) },
                    description = { Text(Localization.get(language, "weekly_backup_desc")) },
                    visible = autoBackupEnabled,
                    trailing = {
                        Switch(
                            checked = autoBackupWeekly,
                            onCheckedChange = { v ->
                                autoBackupWeekly = v
                                DesktopSettings.update { s -> s.copy(autoBackupWeekly = v) }
                            },
                        )
                    },
                    onClick = {
                        autoBackupWeekly = !autoBackupWeekly
                        DesktopSettings.update { s -> s.copy(autoBackupWeekly = !autoBackupWeekly) }
                    },
                ),
                M3SettingsItem(
                    icon = Icons.Filled.SystemUpdate,
                    title = { Text(Localization.get(language, "backup_before_update")) },
                    description = { Text(Localization.get(language, "backup_before_update_desc")) },
                    visible = autoBackupEnabled,
                    trailing = {
                        Switch(
                            checked = autoBackupBeforeUpdate,
                            onCheckedChange = { v ->
                                autoBackupBeforeUpdate = v
                                DesktopSettings.update { s -> s.copy(autoBackupBeforeUpdate = v) }
                            },
                        )
                    },
                    onClick = {
                        autoBackupBeforeUpdate = !autoBackupBeforeUpdate
                        DesktopSettings.update { s ->
                            s.copy(autoBackupBeforeUpdate = !autoBackupBeforeUpdate)
                        }
                    },
                ),
            ),
        )

        // Stored automatic backups
        Text(
            Localization.get(language, "stored_backups"),
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.padding(top = 20.dp),
        )
        if (backups.isEmpty()) {
            Text(
                Localization.get(language, "backups_empty"),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 4.dp),
            )
        } else {
            backups.forEach { file ->
                val (date, type) = parseAutoBackupName(file.name)
                Row(
                    Modifier.fillMaxWidth().padding(top = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Column(Modifier.weight(1f)) {
                        Text(date, style = MaterialTheme.typography.bodyLarge)
                        Text(
                            Localization.get(language, type),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    TextButton(
                        onClick = {
                            pendingRestore = file
                            restoreTarget = file
                        },
                    ) {
                        Text(Localization.get(language, "action_restore"))
                    }
                    TextButton(
                        onClick = {
                            pendingDelete = file
                            deleteTarget = file
                        },
                    ) {
                        Text(Localization.get(language, "delete"), color = MaterialTheme.colorScheme.error)
                    }
                }
            }
        }
    }

    if (showRestartDialog) {
        AlertDialog(
            onDismissRequest = { showRestartDialog = false },
            title = { Text(Localization.get(language, "restore_success_title")) },
            text = { Text(Localization.get(language, "restore_success")) },
            confirmButton = {
                Button(onClick = { restartApplication() }) {
                    Text(Localization.get(language, "restart_now"))
                }
            },
            dismissButton = {
                TextButton(onClick = { showRestartDialog = false }) {
                    Text(Localization.get(language, "later"))
                }
            },
        )
    }

    restoreTarget?.let { file ->
        AlertDialog(
            onDismissRequest = { restoreTarget = null; pendingRestore = null },
            title = { Text(Localization.get(language, "action_restore")) },
            text = { Text(Localization.get(language, "restore_backup_confirm")) },
            confirmButton = {
                TextButton(onClick = { restoreTarget = null }) {
                    Text(Localization.get(language, "action_restore"))
                }
            },
            dismissButton = {
                TextButton(onClick = { restoreTarget = null; pendingRestore = null }) {
                    Text(Localization.get(language, "later"))
                }
            },
        )
    }

    deleteTarget?.let { file ->
        AlertDialog(
            onDismissRequest = { deleteTarget = null; pendingDelete = null },
            title = { Text(Localization.get(language, "delete")) },
            text = { Text(Localization.get(language, "delete_backup_confirm")) },
            confirmButton = {
                TextButton(onClick = { deleteTarget = null }) {
                    Text(Localization.get(language, "delete"))
                }
            },
            dismissButton = {
                TextButton(onClick = { deleteTarget = null; pendingDelete = null }) {
                    Text(Localization.get(language, "later"))
                }
            },
        )
    }
}


/** Extracts a display date + a type key (`backup_type_weekly`/`backup_type_before_update`) from a backup filename. */
private fun parseAutoBackupName(name: String): Pair<String, String> {
    val ts = Regex("""(\d{8}_\d{6})\.vivide\.backup$""").find(name)?.groupValues?.getOrNull(1)
    val date = if (ts != null) {
        runCatching {
            LocalDateTime.parse(ts, DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"))
                .format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm"))
        }.getOrDefault(ts)
    } else {
        name
    }
    val type = if (name.contains("before_update")) "backup_type_before_update" else "backup_type_weekly"
    return date to type
}

/** Native save/open dialog for the backup file (blocks; call on Dispatchers.IO). */
private fun chooseBackupFile(save: Boolean): File? = runCatching {
    val dialog = java.awt.FileDialog(
        null as java.awt.Frame?,
        if (save) "Backup settings" else "Restore settings",
        if (save) java.awt.FileDialog.SAVE else java.awt.FileDialog.LOAD,
    )
    if (save) dialog.file = BackupManager.defaultBackupFileName()
    dialog.isVisible = true
    val dir = dialog.directory
    val name = dialog.file
    dialog.dispose()
    if (dir != null && name != null) File(dir, name) else null
}.getOrNull()

@Composable
fun SettingsContentScreen(
    language: String,
    onBack: () -> Unit,
    contentLanguage: String,
    contentCountry: String,
    onContentLanguageChange: (String) -> Unit,
    onContentCountryChange: (String) -> Unit,
    hideExplicit: Boolean = false,
    hideVideoSongs: Boolean = false,
    hideYoutubeShorts: Boolean = false,
    onHideExplicitChange: (Boolean) -> Unit = {},
    onHideVideoSongsChange: (Boolean) -> Unit = {},
    onHideYoutubeShortsChange: (Boolean) -> Unit = {},
    showArtistDescription: Boolean = true,
    showArtistSubscriberCount: Boolean = true,
    onShowArtistDescriptionChange: (Boolean) -> Unit = {},
    onShowArtistSubscriberCountChange: (Boolean) -> Unit = {},
    lyricsProviders: List<String> = emptyList(),
    onLyricsProvidersChange: (List<String>) -> Unit = {},
) {
    SettingsSubScreen(language, onBack) {
        ContentSection(
            language = language,
            contentLanguage = contentLanguage,
            contentCountry = contentCountry,
            onContentLanguageChange = onContentLanguageChange,
            onContentCountryChange = onContentCountryChange,
            hideExplicit = hideExplicit,
            hideVideoSongs = hideVideoSongs,
            hideYoutubeShorts = hideYoutubeShorts,
            onHideExplicitChange = onHideExplicitChange,
            onHideVideoSongsChange = onHideVideoSongsChange,
            onHideYoutubeShortsChange = onHideYoutubeShortsChange,
            showArtistDescription = showArtistDescription,
            showArtistSubscriberCount = showArtistSubscriberCount,
            onShowArtistDescriptionChange = onShowArtistDescriptionChange,
            onShowArtistSubscriberCountChange = onShowArtistSubscriberCountChange,
            lyricsProviders = lyricsProviders,
            onLyricsProvidersChange = onLyricsProvidersChange,
        )
    }
}

@Composable
fun SettingsLyricsScreen(
    language: String,
    onBack: () -> Unit,
    syncedLyrics: Boolean,
    onToggleSyncedLyrics: (Boolean) -> Unit,
    lyricsTextSize: Float,
    onLyricsTextSizeChange: (Float) -> Unit,
    lyricsLineSpacing: Float = 1.35f,
    onLyricsLineSpacingChange: (Float) -> Unit = {},
    /** Animation style + display options (mobile port). */
    options: LyricsDisplayOptions = LyricsDisplayOptions(),
    onOptionsChange: (LyricsDisplayOptions) -> Unit = {},
    translateLyrics: Boolean = false,
    onToggleTranslateLyrics: (Boolean) -> Unit = {},
    lyricsThumbnailPlayPause: Boolean = false,
    onToggleLyricsThumbnailPlayPause: (Boolean) -> Unit = {},
    onOpenRomanize: () -> Unit = {},
    translationAvailable: Boolean = false,
) {
    SettingsSubScreen(language, onBack) {
        LyricsSection(
            language = language,
            syncedLyrics = syncedLyrics,
            onToggleSyncedLyrics = onToggleSyncedLyrics,
            lyricsTextSize = lyricsTextSize,
            onLyricsTextSizeChange = onLyricsTextSizeChange,
            lyricsLineSpacing = lyricsLineSpacing,
            onLyricsLineSpacingChange = onLyricsLineSpacingChange,
            options = options,
            onOptionsChange = onOptionsChange,
            translateLyrics = translateLyrics,
            onToggleTranslateLyrics = onToggleTranslateLyrics,
            lyricsThumbnailPlayPause = lyricsThumbnailPlayPause,
            onToggleLyricsThumbnailPlayPause = onToggleLyricsThumbnailPlayPause,
            onOpenRomanize = onOpenRomanize,
            translationAvailable = translationAvailable,
        )
    }
}

/** Content section: innerTube host language + region (hl/gl). */
@Composable
fun ContentSection(
    language: String,
    contentLanguage: String,
    contentCountry: String,
    onContentLanguageChange: (String) -> Unit,
    onContentCountryChange: (String) -> Unit,
    hideExplicit: Boolean = false,
    hideVideoSongs: Boolean = false,
    hideYoutubeShorts: Boolean = false,
    onHideExplicitChange: (Boolean) -> Unit = {},
    onHideVideoSongsChange: (Boolean) -> Unit = {},
    onHideYoutubeShortsChange: (Boolean) -> Unit = {},
    showArtistDescription: Boolean = true,
    showArtistSubscriberCount: Boolean = true,
    onShowArtistDescriptionChange: (Boolean) -> Unit = {},
    onShowArtistSubscriberCountChange: (Boolean) -> Unit = {},
    lyricsProviders: List<String> = emptyList(),
    onLyricsProvidersChange: (List<String>) -> Unit = {},
) {
    // The effective order: what the user saved, then whatever the resolver knows
    // that the saved list does not mention (a provider added by an update shows
    // up instead of disappearing).
    val providerOrder = remember(lyricsProviders) {
        lyricsProviders + DesktopLyrics.PROVIDER_ORDER.filterNot { it in lyricsProviders }
    }
    val enabledProviders = lyricsProviders.ifEmpty { DesktopLyrics.PROVIDER_ORDER }.toSet()
    val saveProviders: (List<String>) -> Unit = { onLyricsProvidersChange(it) }

    Text(Localization.get(language, "content"), style = MaterialTheme.typography.titleLarge, modifier = Modifier.padding(top = 16.dp))

    // Language and region are the same kind of choice ("yours, or the system's"),
    // so they are two cards of the same shape instead of two loose buttons
    // sitting under two loose headings. Each card's own row opens the list
    // inline, which is what the mobile Content screen does with its dialogs, and
    // the row shows the current pick on its trailing side so the answer is
    // readable without opening anything.
    //
    // `system_default` is the first entry of both lists: a blank code means
    // "follow" and is what the resolver in `resolveYouTubeLocale` expects.
    val systemDefault = Localization.get(language, "system_default")
    Text(
        Localization.get(language, "content_language"),
        style = MaterialTheme.typography.labelLarge,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.padding(top = 16.dp, bottom = 6.dp),
    )
    M3SettingsDropdownCard(
        icon = Icons.Filled.Language,
        title = Localization.get(language, "content_language"),
        value = LanguageCodeToName[contentLanguage] ?: systemDefault,
        options = listOf("" to systemDefault) +
            LanguageCodeToName.map { (code, name) -> code to name },
        onSelect = onContentLanguageChange,
    )

    Text(
        Localization.get(language, "content_country"),
        style = MaterialTheme.typography.labelLarge,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.padding(top = 16.dp, bottom = 6.dp),
    )
    M3SettingsDropdownCard(
        icon = Icons.Filled.Public,
        title = Localization.get(language, "content_country"),
        value = CountryCodeToName[contentCountry] ?: systemDefault,
        options = listOf("" to systemDefault) +
            CountryCodeToName.map { (code, name) -> code to name },
        onSelect = onContentCountryChange,
    )

    // The region card and the filters card were drawn touching: 16 dp is the gap
    // the page gives every card (`.md-group`'s `margin-top`, the 4 step of the 4 dp
    // scale), and this screen gives it the same.
    Spacer(Modifier.height(16.dp))

    // --- Filters and the artist page, one card -----------------------------
    // The preview draws them as one list of five switches, and they are one
    // subject (what the catalogue shows); two cards split the group in half for
    // no reason the screen can state.
    M3SettingsGroup(
        items = listOf(
            M3SettingsItem(
                icon = Icons.Filled.VisibilityOff,
                title = { Text(Localization.get(language, "hide_explicit")) },
                trailing = { Switch(checked = hideExplicit, onCheckedChange = onHideExplicitChange) },
                onClick = { onHideExplicitChange(!hideExplicit) },
            ),
            M3SettingsItem(
                icon = Icons.Filled.MusicNote,
                title = { Text(Localization.get(language, "hide_video_songs")) },
                trailing = { Switch(checked = hideVideoSongs, onCheckedChange = onHideVideoSongsChange) },
                onClick = { onHideVideoSongsChange(!hideVideoSongs) },
            ),
            M3SettingsItem(
                icon = Icons.Filled.SmartDisplay,
                title = { Text(Localization.get(language, "hide_youtube_shorts")) },
                trailing = { Switch(checked = hideYoutubeShorts, onCheckedChange = onHideYoutubeShortsChange) },
                onClick = { onHideYoutubeShortsChange(!hideYoutubeShorts) },
            ),
            M3SettingsItem(
                icon = Icons.Filled.Info,
                title = { Text(Localization.get(language, "show_artist_description")) },
                trailing = {
                    Switch(checked = showArtistDescription, onCheckedChange = onShowArtistDescriptionChange)
                },
                onClick = { onShowArtistDescriptionChange(!showArtistDescription) },
            ),
            M3SettingsItem(
                icon = Icons.Filled.Groups,
                title = { Text(Localization.get(language, "show_artist_subscriber_count")) },
                trailing = {
                    Switch(checked = showArtistSubscriberCount, onCheckedChange = onShowArtistSubscriberCountChange)
                },
                onClick = { onShowArtistSubscriberCountChange(!showArtistSubscriberCount) },
            ),
        ),
    )

    // --- Lyrics providers (order + on/off) ---------------------------------
    Text(
        Localization.get(language, "lyrics_provider_priority"),
        style = MaterialTheme.typography.titleMedium,
        modifier = Modifier.padding(top = 16.dp),
    )
    M3SettingsGroup(
        items = providerOrder.mapIndexed { index, name ->
            val enabled = name in enabledProviders
            M3SettingsItem(
                icon = Icons.Filled.Lyrics,
                title = { Text(name) },
                trailing = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Tooltip("Move up") {
                            M3IconButton(
                                onClick = {
                                    if (index > 0) {
                                        val next = providerOrder.toMutableList()
                                        next[index - 1] = name
                                        next[index] = providerOrder[index - 1]
                                        saveProviders(next)
                                    }
                                },
                                enabled = index > 0,
                            ) {
                                Icon(Icons.Filled.KeyboardArrowUp, contentDescription = null, modifier = Modifier.size(20.dp))
                            }
                        }
                        Tooltip("Move down") {
                            M3IconButton(
                                onClick = {
                                    if (index < providerOrder.lastIndex) {
                                        val next = providerOrder.toMutableList()
                                        next[index] = providerOrder[index + 1]
                                        next[index + 1] = name
                                        saveProviders(next)
                                    }
                                },
                                enabled = index < providerOrder.lastIndex,
                            ) {
                                Icon(Icons.Filled.KeyboardArrowDown, contentDescription = null, modifier = Modifier.size(20.dp))
                            }
                        }
                        Switch(
                            checked = enabled,
                            // An empty saved list means "the built-in order, all
                            // of them on", so the last enabled provider cannot be
                            // switched off (it would turn everything back on).
                            enabled = !(enabled && enabledProviders.size <= 1),
                            onCheckedChange = { on ->
                                saveProviders(
                                    if (on) {
                                        providerOrder.filterNot { it == name }
                                            .toMutableList()
                                            .apply { add(index.coerceAtMost(size), name) }
                                    } else {
                                        providerOrder.filterNot { it == name }
                                    },
                                )
                            },
                        )
                    }
                },
                onClick = {
                    saveProviders(
                        if (enabled) providerOrder.filterNot { it == name } else providerOrder,
                    )
                },
            )
        },
    )
}

/**
 * Lyrics section: line-by-line highlighting, the animation style (port of the
 * mobile renderer) and every display option around it.
 *
 * The romanization sub-switches are created options: like the mobile app, they
 * only appear once "Romanize lyrics" is on, so the list does not fill up with
 * toggles that cannot do anything.
 */
@Composable
fun LyricsSection(
    language: String,
    syncedLyrics: Boolean,
    onToggleSyncedLyrics: (Boolean) -> Unit,
    lyricsTextSize: Float,
    onLyricsTextSizeChange: (Float) -> Unit,
    lyricsLineSpacing: Float = 1.35f,
    onLyricsLineSpacingChange: (Float) -> Unit = {},
    options: LyricsDisplayOptions = LyricsDisplayOptions(),
    onOptionsChange: (LyricsDisplayOptions) -> Unit = {},
    translateLyrics: Boolean = false,
    onToggleTranslateLyrics: (Boolean) -> Unit = {},
    lyricsThumbnailPlayPause: Boolean = false,
    onToggleLyricsThumbnailPlayPause: (Boolean) -> Unit = {},
    /** Opens the Romanization screen (its master switch and script list live there). */
    onOpenRomanize: () -> Unit = {},
    /** Whether an AI key is configured: without one the translation switch is not offered. */
    translationAvailable: Boolean = false,
) {
    val romanize = options.romanize

    Text(
        Localization.get(language, "lyrics"),
        style = MaterialTheme.typography.titleLarge,
        modifier = Modifier.padding(top = 16.dp),
    )
    Spacer(Modifier.height(16.dp))

    // The card the page opens with: the three switches that decide what the
    // lyrics do while a song plays, in its order - synced, tap a line, auto
    // scroll. Only the first one explains itself; the other two are their own
    // sentence, and the page gives them no description.
    M3SettingsGroup(
        items = listOf(
            M3SettingsItem(
                icon = Icons.Filled.Lyrics,
                title = { Text(Localization.get(language, "synced_lyrics")) },
                description = { Text(Localization.get(language, "synced_lyrics_desc")) },
                trailing = { Switch(checked = syncedLyrics, onCheckedChange = onToggleSyncedLyrics) },
                onClick = { onToggleSyncedLyrics(!syncedLyrics) },
            ),
            M3SettingsItem(
                icon = Icons.Filled.TouchApp,
                title = { Text(Localization.get(language, "lyrics_click_to_seek")) },
                trailing = {
                    Switch(
                        checked = options.clickToSeek,
                        onCheckedChange = { onOptionsChange(options.copy(clickToSeek = it)) },
                    )
                },
                onClick = { onOptionsChange(options.copy(clickToSeek = !options.clickToSeek)) },
            ),
            M3SettingsItem(
                icon = Icons.Filled.SwapVert,
                title = { Text(Localization.get(language, "lyrics_auto_scroll")) },
                trailing = {
                    Switch(
                        checked = options.autoScroll,
                        onCheckedChange = { onOptionsChange(options.copy(autoScroll = it)) },
                    )
                },
                onClick = { onOptionsChange(options.copy(autoScroll = !options.autoScroll)) },
            ),
        ),
    )

    // The two sizes are cards of one row each, the way the page draws them: the
    // row carries the name and its value, the track sits under them in the row's
    // own text column (no floating bar and no label above it).
    Spacer(Modifier.height(16.dp))
    M3SettingsGroup(
        items = listOf(
            M3SettingsItem(
                icon = Icons.Filled.FormatSize,
                title = { Text(Localization.get(language, "lyrics_text_size")) },
                trailing = {
                    Text(
                        "${lyricsTextSize.toInt()} sp",
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                },
                slider = {
                    M3SettingsSlider(
                        value = lyricsTextSize,
                        onValueChange = onLyricsTextSizeChange,
                        valueRange = 12f..30f,
                        steps = 17,
                    )
                },
            ),
        ),
    )
    Spacer(Modifier.height(16.dp))
    M3SettingsGroup(
        items = listOf(
            M3SettingsItem(
                icon = Icons.Filled.FormatLineSpacing,
                title = { Text(Localization.get(language, "lyrics_line_spacing")) },
                trailing = {
                    Text(
                        String.format("%.2f", lyricsLineSpacing),
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                },
                slider = {
                    M3SettingsSlider(
                        value = lyricsLineSpacing,
                        onValueChange = onLyricsLineSpacingChange,
                        valueRange = 1f..2.4f,
                    )
                },
            ),
        ),
    )

    // --- Animation style -------------------------------------------------
    Spacer(Modifier.height(16.dp))
    M3SettingsDropdownItem(
        icon = Icons.Filled.Animation,
        title = Localization.get(language, "lyrics_animation_style"),
        value = lyricsStyleLabel(language, options.style),
        options = LyricsAnimationStyle.entries.map { it.id to lyricsStyleLabel(language, it) },
        onSelect = { id -> onOptionsChange(options.copy(style = LyricsAnimationStyle.from(id))) },
    )

    // --- Text position ---------------------------------------------------
    Spacer(Modifier.height(16.dp))
    M3SettingsDropdownItem(
        icon = Icons.AutoMirrored.Filled.FormatAlignLeft,
        title = Localization.get(language, "lyrics_text_position"),
        value = lyricsPositionLabel(language, options.position),
        options = LyricsPosition.entries.map { it.name to lyricsPositionLabel(language, it) },
        onSelect = { name -> onOptionsChange(options.copy(position = LyricsPosition.from(name))) },
    )

    // --- The display options the page does not draw ----------------------
    // Glow, the two blurs and the thumbnail's play/pause belong to the style
    // above, so they stay one card under it instead of being spread through the
    // screen. The Apple Music blur only exists for the style that applies it
    // (`visible`, so choosing another style folds the row away).
    Spacer(Modifier.height(16.dp))
    M3SettingsGroup(
        items = listOf(
            M3SettingsItem(
                icon = Icons.Filled.AutoAwesome,
                title = { Text(Localization.get(language, "lyrics_glow_effect")) },
                description = { Text(Localization.get(language, "lyrics_glow_effect_desc")) },
                trailing = {
                    Switch(
                        checked = options.glowEffect,
                        onCheckedChange = { onOptionsChange(options.copy(glowEffect = it)) },
                    )
                },
                onClick = { onOptionsChange(options.copy(glowEffect = !options.glowEffect)) },
            ),
            M3SettingsItem(
                icon = Icons.Filled.BlurOn,
                title = { Text(Localization.get(language, "lyrics_apple_blur")) },
                description = { Text(Localization.get(language, "lyrics_apple_blur_desc")) },
                visible = options.style == LyricsAnimationStyle.VIVIMUSIC_1,
                trailing = {
                    Switch(
                        checked = options.appleMusicBlur,
                        onCheckedChange = { onOptionsChange(options.copy(appleMusicBlur = it)) },
                    )
                },
                onClick = { onOptionsChange(options.copy(appleMusicBlur = !options.appleMusicBlur)) },
            ),
            M3SettingsItem(
                icon = Icons.Filled.BlurOn,
                title = { Text(Localization.get(language, "lyrics_standard_blur")) },
                description = { Text(Localization.get(language, "lyrics_standard_blur_desc")) },
                trailing = {
                    Switch(
                        checked = options.standardBlur,
                        onCheckedChange = { onOptionsChange(options.copy(standardBlur = it)) },
                    )
                },
                onClick = { onOptionsChange(options.copy(standardBlur = !options.standardBlur)) },
            ),
            // Mobile "Show play/pause on thumbnail": clicking the artwork in the
            // player starts/stops the song instead of doing nothing.
            M3SettingsItem(
                icon = Icons.Filled.TouchApp,
                title = { Text(Localization.get(language, "lyrics_thumbnail_play_pause")) },
                description = { Text(Localization.get(language, "lyrics_thumbnail_play_pause_desc")) },
                trailing = {
                    Switch(
                        checked = lyricsThumbnailPlayPause,
                        onCheckedChange = onToggleLyricsThumbnailPlayPause,
                    )
                },
                onClick = { onToggleLyricsThumbnailPlayPause(!lyricsThumbnailPlayPause) },
            ),
        ),
    )

    // --- Romanization: one row that opens its own screen -------------------
    // The master switch, the twelve script switches and the "as main" choice
    // were one card in the middle of this screen, so the lyrics settings were
    // mostly a list of twelve languages visible only while the switch was on.
    // They live on their own screen now (`SettingsRomanizeScreen`), reached from
    // here, and this row says whether it is on and which languages it covers.
    Spacer(Modifier.height(16.dp))
    M3SettingsGroup(
        items = listOf(
            M3SettingsItem(
                icon = Icons.Filled.Translate,
                title = { Text(Localization.get(language, "lyrics_romanize")) },
                description = {
                    Text(
                        romanize?.let { ROMANIZE_SCRIPTS.count { script -> script.isEnabled(it) } }
                            ?.let { "$it / ${ROMANIZE_SCRIPTS.size}" }
                            ?: Localization.get(language, "lyrics_romanize_desc"),
                    )
                },
                trailing = {
                    Icon(
                        Icons.AutoMirrored.Filled.KeyboardArrowRight,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                },
                onClick = onOpenRomanize,
            ),
        ),
    )

    // --- AI translation ---------------------------------------------------
    // The switch is only offered when there is a key to translate with: an on
    // switch with no key behind it is a setting that silently does nothing, and
    // the row said nothing about that. Without a key the row is disabled and its
    // description says why, with the AI screen named as the place to fix it.
    Spacer(Modifier.height(16.dp))
    M3SettingsGroup(
        items = listOf(
            M3SettingsItem(
                icon = Icons.Filled.GTranslate,
                title = { Text(Localization.get(language, "translate_lyrics")) },
                description = {
                    Text(
                        if (translationAvailable) {
                            Localization.get(language, "translate_lyrics_desc")
                        } else {
                            Localization.get(language, "translate_needs_api_key")
                        },
                    )
                },
                trailing = {
                    Switch(
                        checked = translateLyrics && translationAvailable,
                        enabled = translationAvailable,
                        onCheckedChange = { on -> onToggleTranslateLyrics(on) },
                    )
                },
                onClick = { if (translationAvailable) onToggleTranslateLyrics(!translateLyrics) },
            ),
        ),
    )
}

/**
 * Romanization on its own screen.
 *
 * The master switch, one switch per script (Japanese, Korean, Chinese, Russian,
 * Ukrainian, Serbian, Bulgarian, Belarusian, Kyrgyz, Macedonian, Hindi, Punjabi)
 * and the choice every translated line already had: show the romanized text as
 * the main line with the original under it, or keep the original in front. The
 * script rows are `visible = romanize != null`, so they fold away with the
 * master switch instead of being dropped between two frames, and their readers
 * stay null-safe because a row on its way out is still composed.
 */
@Composable
fun RomanizeSection(
    language: String,
    options: LyricsDisplayOptions = LyricsDisplayOptions(),
    onOptionsChange: (LyricsDisplayOptions) -> Unit = {},
) {
    val romanize = options.romanize

    Text(Localization.get(language, "lyrics_romanize"), style = MaterialTheme.typography.titleLarge, modifier = Modifier.padding(top = 16.dp))
    Text(
        Localization.get(language, "lyrics_romanize_desc"),
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.padding(top = 8.dp, bottom = 8.dp),
    )

    M3SettingsGroup(
        items = buildList {
            add(
                M3SettingsItem(
                    icon = Icons.Filled.Translate,
                    title = { Text(Localization.get(language, "lyrics_romanize")) },
                    description = { Text(Localization.get(language, "lyrics_romanize_desc")) },
                    trailing = {
                        Switch(
                            checked = romanize != null,
                            onCheckedChange = { on ->
                                onOptionsChange(options.copy(romanize = if (on) LyricsRomanizer.Options() else null))
                            },
                        )
                    },
                    onClick = {
                        onOptionsChange(options.copy(romanize = if (romanize == null) LyricsRomanizer.Options() else null))
                    },
                ),
            )
            // The script switches belong to the romanize switch: `visible` keeps
            // them in the card and folds them in and out with it. Their readers are
            // null-safe because the row exists (and is composed) while it folds.
            ROMANIZE_SCRIPTS.forEach { script ->
                add(
                    M3SettingsItem(
                        icon = null,
                        title = { Text(Localization.get(language, script.labelKey)) },
                        visible = romanize != null,
                        trailing = {
                            Switch(
                                checked = romanize?.let { script.isEnabled(it) } == true,
                                onCheckedChange = { on ->
                                    romanize?.let { onOptionsChange(options.copy(romanize = script.set(it, on))) }
                                },
                            )
                        },
                        onClick = {
                            romanize?.let { onOptionsChange(options.copy(romanize = script.set(it, !script.isEnabled(it)))) }
                        },
                    ),
                )
            }
        },
    )

    // The lyrics' own choice, the same one the lines read when they are drawn:
    // the romanized text in front, or the original in front with the romanized
    // form under it.
    Text(
        Localization.get(language, "lyrics_romanize_as_main"),
        style = MaterialTheme.typography.labelLarge,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.padding(top = 16.dp, bottom = 6.dp),
    )
    M3SettingsGroup(
        items = listOf(
            M3SettingsItem(
                icon = null,
                title = { Text(Localization.get(language, "lyrics_romanize_as_main")) },
                description = { Text(Localization.get(language, "lyrics_romanize_as_main_desc")) },
                visible = romanize != null,
                trailing = {
                    Switch(
                        checked = options.romanizeAsMain,
                        onCheckedChange = { on -> onOptionsChange(options.copy(romanizeAsMain = on)) },
                    )
                },
                onClick = { onOptionsChange(options.copy(romanizeAsMain = !options.romanizeAsMain)) },
            ),
        ),
    )
}

/** The Romanization sub-screen: its own destination, reached from Lyrics. */
@Composable
fun SettingsRomanizeScreen(
    language: String,
    onBack: () -> Unit,
    options: LyricsDisplayOptions = LyricsDisplayOptions(),
    onOptionsChange: (LyricsDisplayOptions) -> Unit = {},
) {
    SettingsSubScreen(language, onBack) { RomanizeSection(language, options, onOptionsChange) }
}

/** Localized label of an animation style. Shared with the player's lyrics menu. */
internal fun lyricsStyleLabel(language: String, style: LyricsAnimationStyle): String {
    val key = when (style) {
        LyricsAnimationStyle.NONE -> "lyrics_style_none"
        LyricsAnimationStyle.FADE -> "lyrics_style_fade"
        LyricsAnimationStyle.GLOW -> "lyrics_style_glow"
        LyricsAnimationStyle.SLIDE -> "lyrics_style_slide"
        LyricsAnimationStyle.KARAOKE -> "lyrics_style_karaoke"
        LyricsAnimationStyle.APPLE -> "lyrics_style_apple"
        LyricsAnimationStyle.APPLE_V2 -> "lyrics_style_apple_v2"
        LyricsAnimationStyle.VIVIMUSIC_1 -> "lyrics_style_vivimusic"
        LyricsAnimationStyle.LYRICS_V2 -> "lyrics_style_lyrics_v2"
        LyricsAnimationStyle.METRO_LYRICS -> "lyrics_style_metro"
        LyricsAnimationStyle.ALPHA -> "lyrics_style_alpha"
    }
    return Localization.get(language, key)
}

/** Localized label of the text position. Shared with the player's lyrics menu. */
internal fun lyricsPositionLabel(language: String, position: LyricsPosition): String = when (position) {
    LyricsPosition.LEFT -> Localization.get(language, "lyrics_position_left")
    LyricsPosition.CENTER -> Localization.get(language, "lyrics_position_center")
    LyricsPosition.RIGHT -> Localization.get(language, "lyrics_position_right")
}

/** One romanizable script: its label key and how to flip it on/off. */
private data class RomanizeScript(
    val labelKey: String,
    val isEnabled: (LyricsRomanizer.Options) -> Boolean,
    val set: (LyricsRomanizer.Options, Boolean) -> LyricsRomanizer.Options,
)

private val ROMANIZE_SCRIPTS = listOf(
    RomanizeScript("romanize_japanese", { it.japanese }, { o, v -> o.copy(japanese = v) }),
    RomanizeScript("romanize_korean", { it.korean }, { o, v -> o.copy(korean = v) }),
    RomanizeScript("romanize_chinese", { it.chinese }, { o, v -> o.copy(chinese = v) }),
    RomanizeScript("romanize_russian", { it.russian }, { o, v -> o.copy(russian = v) }),
    RomanizeScript("romanize_ukrainian", { it.ukrainian }, { o, v -> o.copy(ukrainian = v) }),
    RomanizeScript("romanize_serbian", { it.serbian }, { o, v -> o.copy(serbian = v) }),
    RomanizeScript("romanize_bulgarian", { it.bulgarian }, { o, v -> o.copy(bulgarian = v) }),
    RomanizeScript("romanize_belarusian", { it.belarusian }, { o, v -> o.copy(belarusian = v) }),
    RomanizeScript("romanize_kyrgyz", { it.kyrgyz }, { o, v -> o.copy(kyrgyz = v) }),
    RomanizeScript("romanize_macedonian", { it.macedonian }, { o, v -> o.copy(macedonian = v) }),
    RomanizeScript("romanize_hindi", { it.hindi }, { o, v -> o.copy(hindi = v) }),
    RomanizeScript("romanize_punjabi", { it.punjabi }, { o, v -> o.copy(punjabi = v) }),
)

/** Performance options: enable/disable the live stats, pick a profile and placement. */
@Composable
fun PerformanceSection(language: String, syncManager: DesktopSyncManager) {
    val enabled by DeveloperOptions.enabled.collectAsState()
    val mode by DeveloperOptions.mode.collectAsState()
    val profile by DeveloperOptions.profile.collectAsState()
    val movable by DeveloperOptions.overlayMovable.collectAsState()
    val titleBar by DeveloperOptions.showInTitleBar.collectAsState()
    val stats by SystemMonitor.stats.collectAsState()
    // "Title bar only" and "Show in title bar" write into the OS window title,
    // which only exists while the native title bar is in use: with the custom bar
    // the window is undecorated and the app draws its own, so the two options
    // would offer a title bar the window does not have. They are therefore drawn
    // only with the native bar, and a mode saved as TITLE_BAR while it was on
    // goes back to the overlay once it is off, so the stats keep a place to
    // draw instead of vanishing with the title they were written into.
    val revision = settingsFileRevision()
    val nativeTitleBar = remember(revision) { DesktopSettings.load().nativeTitleBar }
    LaunchedEffect(nativeTitleBar, mode) {
        if (!nativeTitleBar && mode == DevToolsMode.TITLE_BAR) {
            DeveloperOptions.setMode(DevToolsMode.OVERLAY)
            AppLog.log("dev", "title bar only needs the native title bar: the reader is back on the overlay")
        }
    }
    val peerName = syncManager.peerDeviceName.collectAsState().value.orEmpty()
    val paired = syncManager.paired.collectAsState().value == true

    Column(Modifier.fillMaxWidth().padding(top = 8.dp, bottom = 24.dp)) {
        Text(
            Localization.get(language, "performance_options"),
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
        )
        Text(
            Localization.get(language, "performance_options_desc"),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(top = 4.dp),
        )

        Spacer(Modifier.height(16.dp))

        // Master switch, always visible (unlock is also available from About).
        Card(
            shape = MaterialTheme.shapes.extraLarge,
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        ) {
            Row(
                Modifier
                    .fillMaxWidth()
                    .clickable { DeveloperOptions.setEnabled(!enabled) }
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(Modifier.weight(1f)) {
                    Text(
                        Localization.get(language, "performance_options"),
                        style = MaterialTheme.typography.titleMedium,
                    )
                    Spacer(Modifier.height(2.dp))
                    Text(
                        Localization.get(language, if (enabled) "performance_options_enabled" else "dev_tools_disabled"),
                        style = MaterialTheme.typography.bodySmall,
                        color = if (enabled) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                Spacer(Modifier.width(12.dp))
                Switch(checked = enabled, onCheckedChange = { DeveloperOptions.setEnabled(it) })
            }
        }

        // Folds like every other block that belongs to a switch: turning Developer
        // options off closes the monitor instead of dropping it.
        M3SettingsFold(visible = enabled) {
            Spacer(Modifier.height(16.dp))

            // Live monitor
            Card(
                shape = MaterialTheme.shapes.extraLarge,
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
            ) {
                Column(Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            Localization.get(language, "dev_tools_live_monitor"),
                            style = MaterialTheme.typography.titleMedium,
                            modifier = Modifier.weight(1f),
                        )
                        Box(
                            Modifier
                                .width(8.dp)
                                .height(8.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.primary),
                        )
                    }
                    Spacer(Modifier.height(12.dp))
                    DevLiveMonitor(
                        stats = stats,
                        performance = profile == DevToolsProfile.PERFORMANCE,
                        language = language,
                        paired = paired,
                        peerName = peerName,
                    )
                }
            }

            Spacer(Modifier.height(24.dp))

            // Display mode
            DevSectionHeader(language, "dev_tools_mode")
            Card(
                shape = MaterialTheme.shapes.extraLarge,
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
            ) {
                Column {
                    DevRadioRow(
                        title = Localization.get(language, "dev_tools_overlay"),
                        selected = mode == DevToolsMode.OVERLAY,
                        onClick = { DeveloperOptions.setMode(DevToolsMode.OVERLAY) },
                    )
                    HorizontalDivider(Modifier.padding(horizontal = 16.dp))
                    DevRadioRow(
                        title = Localization.get(language, "dev_tools_window"),
                        selected = mode == DevToolsMode.WINDOW,
                        onClick = { DeveloperOptions.setMode(DevToolsMode.WINDOW) },
                    )
                    // The title bar options exist only with the native title bar
                    // (see `nativeTitleBar` above).
                    if (nativeTitleBar) {
                        HorizontalDivider(Modifier.padding(horizontal = 16.dp))
                        DevRadioRow(
                            title = Localization.get(language, "dev_tools_title_bar_only"),
                            selected = mode == DevToolsMode.TITLE_BAR,
                            onClick = { DeveloperOptions.setMode(DevToolsMode.TITLE_BAR) },
                        )
                    }
                }
            }

            Spacer(Modifier.height(24.dp))

            // Display profile
            DevSectionHeader(language, "dev_tools_profile")
            Card(
                shape = MaterialTheme.shapes.extraLarge,
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
            ) {
                Column {
                    DevRadioRow(
                        title = Localization.get(language, "dev_tools_profile_full"),
                        selected = profile == DevToolsProfile.FULL,
                        onClick = { DeveloperOptions.setProfile(DevToolsProfile.FULL) },
                    )
                    HorizontalDivider(Modifier.padding(horizontal = 16.dp))
                    DevRadioRow(
                        title = Localization.get(language, "dev_tools_profile_performance"),
                        selected = profile == DevToolsProfile.PERFORMANCE,
                        onClick = { DeveloperOptions.setProfile(DevToolsProfile.PERFORMANCE) },
                    )
                }
            }

            // Folds like every other block that belongs to a choice: switching the
            // display mode closes or opens this card instead of dropping it.
            M3SettingsFold(visible = mode == DevToolsMode.OVERLAY) {
                Spacer(Modifier.height(24.dp))

                // Overlay behaviour (only relevant in overlay mode)
                Card(
                    shape = MaterialTheme.shapes.extraLarge,
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                ) {
                    Column {
                        DevSwitchRow(
                            title = Localization.get(language, "dev_tools_movable"),
                            desc = Localization.get(language, "dev_tools_movable_desc"),
                            checked = movable,
                            onCheckedChange = { DeveloperOptions.setOverlayMovable(it) },
                        )
                        // The window title is the OS one: with the custom bar there
                        // is no title to write the stats into.
                        if (nativeTitleBar) {
                            HorizontalDivider(Modifier.padding(horizontal = 16.dp))
                            DevSwitchRow(
                                title = Localization.get(language, "dev_tools_title_bar"),
                                desc = Localization.get(language, "dev_tools_title_bar_desc"),
                                checked = titleBar,
                                onCheckedChange = { DeveloperOptions.setShowInTitleBar(it) },
                            )
                        }
                    }
                }
            }
        }
    }
}

/**
 * The real Developer options screen (System > Advanced, unlocked by tapping the
 * version code seven times). It exposes the settings that normally have no UI:
 * the JVM heap, the detailed playback log and the download-APK button.
 */
@Composable
fun DeveloperOptionsSection(language: String, onDisabled: () -> Unit = {}) {
    val revision = settingsFileRevision()
    var heapMb by remember(revision) { mutableStateOf(DesktopSettings.load().jvmHeapMb) }
    var detailedLogs by remember(revision) { mutableStateOf(DesktopSettings.load().detailedPlaybackLogging) }
    var startupLogs by remember(revision) { mutableStateOf(DesktopSettings.load().detailedStartupLogs) }
    var hideApk by remember(revision) { mutableStateOf(DesktopSettings.load().hideCustomApkDownloadButton) }
    var confirmDisable by remember { mutableStateOf(false) }
    // A heap size the user has just picked and that is waiting for the "restart
    // now or later" answer. The value is stored the moment it is picked either
    // way, so "later" is a real choice and not a way to lose the setting.
    var pendingHeap by remember { mutableStateOf<Int?>(null) }

    val currentLabel = if (heapMb <= 0) {
        Localization.get(language, "jvm_memory_default").format(JvmMemory.label(JvmMemory.DEFAULT_MB))
    } else {
        JvmMemory.label(heapMb)
    }
    val heapOptions = buildList {
        add("0" to Localization.get(language, "jvm_memory_default").format(JvmMemory.label(JvmMemory.DEFAULT_MB)))
        JvmMemory.PRESETS_MB.forEach { add(it.toString() to JvmMemory.label(it)) }
    }
    val currentMb = JvmMemory.currentMaxMb

    Column(Modifier.fillMaxWidth().padding(top = 8.dp, bottom = 24.dp)) {
        Text(
            Localization.get(language, "debug_developer_options"),
            style = MaterialTheme.typography.titleLarge,
        )
        Text(
            Localization.get(language, "developer_options_desc"),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(horizontal = 4.dp).padding(top = 8.dp),
        )

        Spacer(Modifier.height(16.dp))

        // Android-style "turn off developer options" sits first, exactly like
        // Android's own developer-options screen: it hides this screen, locks it
        // again and puts every option below back to its default, so the seven-tap
        // on the version code is the only way back. Android turns them off with a
        // switch, so this is a switch too — it reads as the state of the thing
        // rather than as a row that opens a question. Turning it off still asks
        // once, because it resets every option below.
        M3SettingsGroup(
            items = listOf(
                M3SettingsItem(
                    icon = Icons.Filled.Delete,
                    title = { Text(Localization.get(language, "disable_developer_options")) },
                    description = { Text(Localization.get(language, "disable_developer_options_desc")) },
                    trailing = {
                        Switch(
                            checked = true,
                            onCheckedChange = { on -> if (!on) confirmDisable = true },
                        )
                    },
                    onClick = { confirmDisable = true },
                ),
            ),
        )

        Spacer(Modifier.height(16.dp))

        // The page's Advanced card: the heap the app asks for, the two logging
        // switches and the hidden APK button, one row each. The heap is a choice
        // with a fixed set of sizes, so it is the row's own dropdown (its menu
        // opens under the row); the restart it needs is asked when it changes.
        M3SettingsGroup(
            title = Localization.get(language, "advanced"),
            items = listOf(
                M3SettingsItem(
                    icon = Icons.Filled.Memory,
                    title = { Text(Localization.get(language, "jvm_memory")) },
                    description = {
                        Text(
                            Localization.get(language, "jvm_memory_desc")
                                .format(JvmMemory.label(currentMb)),
                        )
                    },
                    dropdown = M3Dropdown(
                        value = currentLabel,
                        options = heapOptions,
                        onSelect = { key ->
                            val mb = key.toIntOrNull() ?: 0
                            heapMb = mb
                            // Stored at once, and then the app offers to apply it:
                            // the heap is a JVM start-up option, so it can only come
                            // from a restart, and the user has to be told that
                            // rather than have the app close on them.
                            DesktopSettings.update { it.copy(jvmHeapMb = mb) }
                            pendingHeap = mb
                        },
                    ),
                ),
                M3SettingsItem(
                    icon = Icons.Filled.ReceiptLong,
                    title = { Text(Localization.get(language, "detailed_playback_logging")) },
                    description = { Text(Localization.get(language, "detailed_playback_logging_desc")) },
                    trailing = {
                        Switch(
                            checked = detailedLogs,
                            onCheckedChange = { v ->
                                detailedLogs = v
                                DesktopSettings.update { it.copy(detailedPlaybackLogging = v) }
                            },
                        )
                    },
                    onClick = {
                        detailedLogs = !detailedLogs
                        DesktopSettings.update { it.copy(detailedPlaybackLogging = detailedLogs) }
                    },
                ),
                M3SettingsItem(
                    icon = Icons.Filled.RocketLaunch,
                    title = { Text(Localization.get(language, "detailed_startup_logs")) },
                    description = { Text(Localization.get(language, "detailed_startup_logs_desc")) },
                    trailing = {
                        Switch(
                            checked = startupLogs,
                            onCheckedChange = { v ->
                                startupLogs = v
                                DesktopSettings.update { it.copy(detailedStartupLogs = v) }
                            },
                        )
                    },
                    onClick = {
                        startupLogs = !startupLogs
                        DesktopSettings.update { it.copy(detailedStartupLogs = startupLogs) }
                    },
                ),
                M3SettingsItem(
                    icon = Icons.Filled.Android,
                    title = { Text(Localization.get(language, "hide_custom_apk_download_button")) },
                    description = { Text(Localization.get(language, "hide_custom_apk_download_button_desc")) },
                    trailing = {
                        Switch(
                            checked = hideApk,
                            onCheckedChange = { v ->
                                hideApk = v
                                DesktopSettings.update { it.copy(hideCustomApkDownloadButton = v) }
                            },
                        )
                    },
                    onClick = {
                        hideApk = !hideApk
                        DesktopSettings.update { it.copy(hideCustomApkDownloadButton = hideApk) }
                    },
                ),
            ),
        )

        pendingHeap?.let { mb ->
            AlertDialog(
                onDismissRequest = { pendingHeap = null },
                title = { Text(Localization.get(language, "restart_required_title")) },
                text = {
                    Text(
                        if (mb <= 0) {
                            Localization.get(language, "jvm_memory_default")
                                .format(JvmMemory.label(JvmMemory.DEFAULT_MB))
                        } else {
                            JvmMemory.label(mb)
                        } + ". " + Localization.get(language, "restart_required"),
                    )
                },
                confirmButton = {
                    TextButton(
                        onClick = {
                            pendingHeap = null
                            // The restart re-runs the app's own runtime with the
                            // new heap; if that process dies immediately the app
                            // stays up and `app.log` says why.
                            JvmMemory.restartNow(mb)
                        },
                    ) {
                        Text(Localization.get(language, "restart_now"))
                    }
                },
                dismissButton = {
                    TextButton(onClick = { pendingHeap = null }) {
                        Text(Localization.get(language, "later"))
                    }
                },
            )
        }

        if (confirmDisable) {
            AlertDialog(
                onDismissRequest = { confirmDisable = false },
                title = { Text(Localization.get(language, "disable_developer_options_confirm")) },
                text = { Text(Localization.get(language, "disable_developer_options_desc")) },
                confirmButton = {
                    TextButton(
                        onClick = {
                            confirmDisable = false
                            DeveloperOptions.resetToDefaults()
                            onDisabled()
                        },
                    ) {
                        Text(Localization.get(language, "ok"))
                    }
                },
                dismissButton = {
                    TextButton(onClick = { confirmDisable = false }) {
                        Text(Localization.get(language, "cancel"))
                    }
                },
            )
        }
    }
}

/** Compact live preview of [SystemMonitor] metrics shown inside the developer options. */
@Composable
private fun DevLiveMonitor(
    stats: SystemStats,
    performance: Boolean,
    language: String,
    paired: Boolean,
    peerName: String,
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            DevStatTile(
                "${Localization.get(language, "cpu")} · ${Localization.get(language, "process")}",
                devPct(stats.cpuProcess),
                Modifier.weight(1f),
            )
            DevStatTile(
                "${Localization.get(language, "cpu")} · ${Localization.get(language, "system")}",
                devPct(stats.cpuSystem),
                Modifier.weight(1f),
            )
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            DevStatTile(
                "${Localization.get(language, "memory")} · ${Localization.get(language, "process")}",
                devFormatBytes(stats.processRamBytes),
                Modifier.weight(1f),
            )
            DevStatTile(
                "${Localization.get(language, "memory")} · ${Localization.get(language, "system")}",
                if (stats.sysRamTotalBytes >= 0) {
                    "${devFormatBytes(stats.sysRamUsedBytes)} / ${devFormatBytes(stats.sysRamTotalBytes)}"
                } else "—",
                Modifier.weight(1f),
            )
        }
        DevStatRow(Localization.get(language, "gpu"), stats.gpuDevice.ifBlank { "—" })
        if (!performance) {
            DevStatRow(
                "${Localization.get(language, "memory")} · ${Localization.get(language, "heap")}",
                "${devFormatBytes(stats.heapUsedBytes)} / ${devFormatBytes(stats.heapMaxBytes)}",
            )
        }
        if (!performance) {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                DevStatTile(
                    "${Localization.get(language, "network")} ↓",
                    devFormatSpeed(stats.netDownBps),
                    Modifier.weight(1f),
                )
                DevStatTile(
                    "${Localization.get(language, "network")} ↑",
                    devFormatSpeed(stats.netUpBps),
                    Modifier.weight(1f),
                )
            }
            DevStatRow(
                Localization.get(language, "total_traffic"),
                "↓ ${devFormatBytes(stats.netDownTotalBytes)} · ↑ ${devFormatBytes(stats.netUpTotalBytes)}",
            )
            DevStatRow(
                Localization.get(language, "paired_device"),
                if (paired && peerName.isNotBlank()) peerName else Localization.get(language, "no_paired_device"),
            )
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                DevStatTile(Localization.get(language, "threads"), stats.threadCount.toString(), Modifier.weight(1f))
                DevStatTile(Localization.get(language, "uptime"), devFormatUptime(stats.uptimeMs), Modifier.weight(1f))
            }
            DevStatRow(
                Localization.get(language, "system_info"),
                "${stats.osName} · Java ${stats.javaVersion} · ${stats.availableProcessors} cores",
            )
        }
    }
}

/** Single labelled metric row (label left, value right). */
@Composable
private fun DevStatRow(label: String, value: String) {
    Row(Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
        Text(
            label,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.weight(1f),
        )
        Text(value, style = MaterialTheme.typography.bodySmall)
    }
}

/** A rounded metric tile (small label above a bold value). */
@Composable
private fun DevStatTile(label: String, value: String, modifier: Modifier = Modifier) {
    Column(
        modifier
            .clip(MaterialTheme.shapes.medium)
            .background(MaterialTheme.colorScheme.surface)
            .padding(12.dp),
    ) {
        Text(
            label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(Modifier.height(4.dp))
        Text(
            value,
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurface,
        )
    }
}

/** A radio row used for exclusive choices (display mode / profile). */
@Composable
private fun DevRadioRow(title: String, selected: Boolean, onClick: () -> Unit) {
    Row(
        Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        RadioButton(selected = selected, onClick = null)
        Text(title, style = MaterialTheme.typography.bodyLarge, modifier = Modifier.padding(start = 12.dp))
    }
}

/** A switch row used inside the developer options cards. */
@Composable
private fun DevSwitchRow(title: String, desc: String, checked: Boolean, onCheckedChange: (Boolean) -> Unit) {
    Row(
        Modifier
            .fillMaxWidth()
            .clickable { onCheckedChange(!checked) }
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.bodyLarge)
            Text(
                desc,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Switch(checked = checked, onCheckedChange = onCheckedChange)
    }
}

/** Section heading inside the (redesigned) developer options screen. */
@Composable
private fun DevSectionHeader(language: String, key: String) {
    Text(
        Localization.get(language, key),
        style = MaterialTheme.typography.titleSmall,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.padding(start = 4.dp, bottom = 8.dp),
    )
}

// Local formatting helpers (mirror the dev-tools overlay formatting).
private fun devFormatBytes(bytes: Long): String {
    if (bytes < 0) return "—"
    if (bytes < 1024) return "$bytes B"
    val kb = bytes / 1024.0
    if (kb < 1024) return "%.1f KB".format(kb)
    if (kb / 1024.0 < 1024) return "%.1f MB".format(kb / 1024.0)
    return "%.2f GB".format(kb / 1024.0 / 1024.0)
}

private fun devFormatSpeed(bps: Long): String = if (bps < 0) "—" else "${devFormatBytes(bps)}/s"

private fun devPct(x: Double): String = if (x < 0) "—" else "%.1f%%".format(x * 100)

private fun devFormatUptime(ms: Long): String {
    if (ms < 0) return "—"
    val totalSec = ms / 1000
    val d = totalSec / 86400
    val h = (totalSec % 86400) / 3600
    val m = (totalSec % 3600) / 60
    val s = totalSec % 60
    return if (d > 0) "${d}d ${h}h" else if (h > 0) "${h}h ${m}m" else "${m}m ${s}s"
}

/** Notification preferences: where update notifications are shown. */
@Composable
fun SettingsNotificationsScreen(
    language: String,
    onBack: () -> Unit,
    notificationMode: String,
    onNotificationModeChange: (String) -> Unit,
    notificationDurationSeconds: Int,
    onNotificationDurationChange: (Int) -> Unit,
    saveHistory: Boolean,
    onSaveHistoryChange: (Boolean) -> Unit,
    onOpenHistory: () -> Unit,
    onTestNotification: () -> Unit,
    /** Asks the OS for the notification permission (macOS only). */
    onRequestNotificationPermission: () -> Unit = {},
) {
    SettingsSubScreen(language, onBack) {
        // The preview's shape: the mode and what it means, then the three rows
        // that belong to it in one card, then the one action that proves it
        // works. The duration is the app's own extra and stays a choice of its
        // own under the card.
        Text(Localization.get(language, "notifications"), style = MaterialTheme.typography.titleLarge)
        Spacer(Modifier.height(16.dp))

        M3SettingsDropdownItem(
            icon = Icons.Filled.NotificationsActive,
            title = Localization.get(language, "notification_mode"),
            value = Localization.get(language, if (notificationMode == "native") "notification_native" else "notification_main_window"),
            options = listOf(
                "in_app" to Localization.get(language, "notification_main_window"),
                "native" to Localization.get(language, "notification_native"),
            ),
            onSelect = onNotificationModeChange,
        )
        Text(
            Localization.get(language, "notification_mode_desc"),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(top = 6.dp, start = 6.dp, end = 6.dp),
        )

        Spacer(Modifier.height(16.dp))
        // The mobile app's notification-permission row. It is shown on every
        // desktop platform (the options belong in the UI everywhere): on macOS
        // it asks UNUserNotificationCenter for the permission, on Windows and
        // Linux, which have no runtime permission, it posts a notification so
        // the row still verifies that notifications arrive.
        M3SettingsGroup(
            items = listOf(
                M3SettingsItem(
                    icon = Icons.Filled.Notifications,
                    title = { Text(Localization.get(language, "allow_notifications")) },
                    onClick = onRequestNotificationPermission,
                ),
                M3SettingsItem(
                    icon = Icons.Filled.Save,
                    title = { Text(Localization.get(language, "save_notification_history")) },
                    trailing = { Switch(checked = saveHistory, onCheckedChange = onSaveHistoryChange) },
                    onClick = { onSaveHistoryChange(!saveHistory) },
                ),
                M3SettingsItem(
                    icon = Icons.Filled.History,
                    title = { Text(Localization.get(language, "notification_history")) },
                    trailing = { SettingsChevron() },
                    onClick = onOpenHistory,
                ),
            ),
        )

        Spacer(Modifier.height(16.dp))
        M3SettingsDropdownItem(
            icon = Icons.Filled.AccessTime,
            title = Localization.get(language, "notification_duration"),
            description = Localization.get(language, "notification_duration_desc"),
            value = "${notificationDurationSeconds}s",
            options = listOf(3, 5, 10, 15, 30).map { it.toString() to "${it}s" },
            onSelect = { s -> onNotificationDurationChange(s.toIntOrNull() ?: notificationDurationSeconds) },
        )

        Spacer(Modifier.height(16.dp))
        Button(
            onClick = onTestNotification,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text(Localization.get(language, "test_notification"))
        }
    }
}

/** Scrollable list of recent notifications (in-app and native). */
@Composable
fun NotificationHistoryScreen(
    language: String,
    onBack: () -> Unit,
) {
    var history by remember { mutableStateOf(NotificationHistory.list()) }
    SettingsSubScreen(language, onBack) {
        Text(Localization.get(language, "notification_history"), style = MaterialTheme.typography.titleLarge)
        Text(
            Localization.get(language, "notification_history_desc"),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(top = 4.dp),
        )
        // One card, as the preview draws it: every notification is a row with the
        // bell it would have shown, its own title and the message a moment ago.
        // The mode and the time are the row's trailing column, the two things the
        // list exists to say about it.
        if (history.isEmpty()) {
            // Its own key: "history_empty" belongs to the listening-history
            // screen, and sharing it made this list and that screen print each
            // other's wording (the History screen said "No notifications yet").
            Text(
                Localization.get(language, "notification_history_empty"),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(vertical = 24.dp),
            )
        } else {
            Spacer(Modifier.height(16.dp))
            M3SettingsGroup(
                items = history.map { record ->
                    M3SettingsItem(
                        icon = Icons.Filled.Notifications,
                        title = { Text(record.title) },
                        description = { Text(record.message) },
                        trailing = {
                            Column(horizontalAlignment = Alignment.End) {
                                Text(
                                    formatNotificationTime(record.timestamp),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                                Text(
                                    if (record.mode == "native") Localization.get(language, "notification_native")
                                    else Localization.get(language, "notification_main_window"),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.primary,
                                )
                            }
                        },
                    )
                },
            )
        }
        Spacer(Modifier.height(16.dp))
        TextButton(onClick = {
            NotificationHistory.clear()
            history = emptyList()
        }) {
            Text(Localization.get(language, "clear_history"), color = MaterialTheme.colorScheme.error)
        }
    }
}

private fun formatNotificationTime(epochMillis: Long): String = runCatching {
    val dt = java.time.Instant.ofEpochMilli(epochMillis).atZone(java.time.ZoneId.systemDefault())
    java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm").format(dt)
}.getOrDefault("")

@Composable
private fun NotificationModeOption(
    language: String,
    title: String,
    tag: String? = null,
    selected: Boolean,
    onClick: () -> Unit,
) {
    Row(
        Modifier.fillMaxWidth().clickable(onClick = onClick).padding(vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(title, style = MaterialTheme.typography.bodyLarge, modifier = Modifier.weight(1f))
        if (tag != null) {
            Surface(
                shape = MaterialTheme.shapes.extraSmall,
                color = MaterialTheme.colorScheme.secondaryContainer,
            ) {
                Text(
                    tag,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSecondaryContainer,
                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                )
            }
            Spacer(Modifier.width(8.dp))
        }
        if (selected) {
            Text("✓", color = MaterialTheme.colorScheme.primary, style = MaterialTheme.typography.titleLarge)
        }
    }
}

/** Privacy sub-screen: listen/search history toggles (port of the mobile PrivacySettings screen). */
@Composable
fun SettingsPrivacyScreen(
    language: String,
    onBack: () -> Unit,
    pauseListenHistory: Boolean,
    onPauseListenHistoryChange: (Boolean) -> Unit,
    pauseSearchHistory: Boolean,
    onPauseSearchHistoryChange: (Boolean) -> Unit,
    onClearSearchHistory: () -> Unit,
) {
    var showClearDialog by remember { mutableStateOf(false) }

    if (showClearDialog) {
        AlertDialog(
            onDismissRequest = { showClearDialog = false },
            title = { Text(Localization.get(language, "clear_search_history")) },
            text = { Text(Localization.get(language, "clear_search_history_confirm")) },
            confirmButton = {
                TextButton(onClick = { showClearDialog = false; onClearSearchHistory() }) {
                    Text(Localization.get(language, "ok"))
                }
            },
            dismissButton = {
                TextButton(onClick = { showClearDialog = false }) {
                    Text(Localization.get(language, "cancel"))
                }
            },
        )
    }

    SettingsSubScreen(language, onBack) {
        Text(Localization.get(language, "privacy"), style = MaterialTheme.typography.titleLarge)
        Spacer(Modifier.height(16.dp))

        M3SettingsGroup(
            items = listOf(
                M3SettingsItem(
                    icon = Icons.Filled.History,
                    title = { Text(Localization.get(language, "pause_listen_history")) },
                    description = { Text(Localization.get(language, "pause_listen_history_desc")) },
                    trailing = { Switch(checked = pauseListenHistory, onCheckedChange = onPauseListenHistoryChange) },
                    onClick = { onPauseListenHistoryChange(!pauseListenHistory) },
                ),
                M3SettingsItem(
                    icon = Icons.Filled.Search,
                    title = { Text(Localization.get(language, "pause_search_history")) },
                    description = { Text(Localization.get(language, "pause_search_history_desc")) },
                    trailing = { Switch(checked = pauseSearchHistory, onCheckedChange = onPauseSearchHistoryChange) },
                    onClick = { onPauseSearchHistoryChange(!pauseSearchHistory) },
                ),
            ),
        )
        // The page's one action is a tonal button 16 dp under the card, not an
        // outlined one: on this screen it is the action, and the outline read as
        // a second, quieter card.
        Spacer(Modifier.height(16.dp))
        FilledTonalButton(onClick = { showClearDialog = true }) {
            Text(Localization.get(language, "clear_search_history"))
        }
        Spacer(Modifier.height(16.dp))
    }
}

/** Integrations sub-screen: Discord Rich Presence + Last.fm scrobbling. */
@Composable
fun SettingsIntegrationsScreen(
    language: String,
    onBack: () -> Unit,
    discordEnabled: Boolean,
    onDiscordEnabledChange: (Boolean) -> Unit,
    discordClientId: String,
    onDiscordClientIdChange: (String) -> Unit,
    lastfmEnabled: Boolean,
    onLastfmEnabledChange: (Boolean) -> Unit,
    lastfmSession: String,
    onLastfmSessionChange: (String) -> Unit,
    lastfmNowPlaying: Boolean,
    onLastfmNowPlayingChange: (Boolean) -> Unit,
) {
    SettingsSubScreen(language, onBack) {
        Text(Localization.get(language, "integrations"), style = MaterialTheme.typography.titleLarge)

        Text(
            Localization.get(language, "discord_presence"),
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(top = 16.dp),
        )
        Spacer(Modifier.height(16.dp))
        M3SettingsGroup(
            items = listOf(
                M3SettingsItem(
                    icon = Icons.Filled.Forum,
                    title = { Text(Localization.get(language, "discord_presence_enable")) },
                    description = { Text(Localization.get(language, "discord_presence_desc")) },
                    trailing = {
                        Switch(checked = discordEnabled, onCheckedChange = onDiscordEnabledChange)
                    },
                    onClick = { onDiscordEnabledChange(!discordEnabled) },
                ),
            ),
        )
        OutlinedTextField(
            value = discordClientId,
            onValueChange = onDiscordClientIdChange,
            label = { Text(Localization.get(language, "discord_client_id")) },
            singleLine = true,
            modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
        )
        if (discordEnabled && discordClientId.isBlank()) {
            Text(
                Localization.get(language, "discord_client_id_hint"),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.error,
                modifier = Modifier.padding(top = 4.dp),
            )
        }

        Spacer(Modifier.height(24.dp))

        Text(
            Localization.get(language, "lastfm"),
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(Modifier.height(16.dp))
        M3SettingsGroup(
            items = listOf(
                M3SettingsItem(
                    icon = Icons.Filled.MusicNote,
                    title = { Text(Localization.get(language, "lastfm_enable")) },
                    description = { Text(Localization.get(language, "lastfm_enable_desc")) },
                    trailing = {
                        Switch(checked = lastfmEnabled, onCheckedChange = onLastfmEnabledChange)
                    },
                    onClick = { onLastfmEnabledChange(!lastfmEnabled) },
                ),
            ),
        )
        // The session field and the Now Playing switch belong to the switch above
        // them, so they fold away with it instead of vanishing in one frame.
        M3SettingsFold(visible = lastfmEnabled) {
            OutlinedTextField(
                value = lastfmSession,
                onValueChange = onLastfmSessionChange,
                label = { Text(Localization.get(language, "lastfm_session")) },
                singleLine = true,
                modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
            )
            Text(
                Localization.get(language, "lastfm_session_hint"),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 4.dp),
            )
            Spacer(Modifier.height(16.dp))
            M3SettingsGroup(
                items = listOf(
                    M3SettingsItem(
                        icon = Icons.Filled.PlayCircle,
                        title = { Text(Localization.get(language, "lastfm_now_playing")) },
                        description = { Text(Localization.get(language, "lastfm_now_playing_desc")) },
                        trailing = {
                            Switch(
                                checked = lastfmNowPlaying,
                                onCheckedChange = onLastfmNowPlayingChange,
                            )
                        },
                        onClick = { onLastfmNowPlayingChange(!lastfmNowPlaying) },
                    ),
                ),
            )
        }
        Spacer(Modifier.height(16.dp))
    }
}

/**
 * Cider-style desktop features: floating Now Playing widget, global media keys
 * and the tray icon menu. Everything is a simple toggle.
 *
 * The media-keys switch needs no OS permission on any platform: Windows/Linux
 * use the low-level hook, macOS uses the native MediaPlayer session (issue #63),
 * which is an OS-level integration and therefore never blocked by the
 * Accessibility permission.
 */
@Composable
fun SettingsDesktopScreen(
    language: String,
    onBack: () -> Unit,
    showWidget: Boolean,
    onShowWidgetChange: (Boolean) -> Unit,
    mediaKeysEnabled: Boolean,
    onMediaKeysChange: (Boolean) -> Unit,
    trayMenuEnabled: Boolean,
    onTrayMenuChange: (Boolean) -> Unit,
) {
    SettingsSubScreen(language, onBack) {
        Text(
            Localization.get(language, "desktop_features"),
            style = MaterialTheme.typography.titleLarge,
            modifier = Modifier.padding(top = 16.dp),
        )
        Text(
            Localization.get(language, "desktop_features_desc"),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(top = 6.dp),
        )
        Spacer(Modifier.height(16.dp))

        // The three are one subject (what this build does with the desktop around
        // it), so they are one card, as the preview draws them.
        M3SettingsGroup(
            items = listOf(
                M3SettingsItem(
                    icon = Icons.Filled.PictureInPicture,
                    title = { Text(Localization.get(language, "now_playing_widget")) },
                    description = { Text(Localization.get(language, "now_playing_widget_desc")) },
                    trailing = { Switch(checked = showWidget, onCheckedChange = onShowWidgetChange) },
                    onClick = { onShowWidgetChange(!showWidget) },
                ),
                M3SettingsItem(
                    icon = Icons.Filled.Keyboard,
                    title = { Text(Localization.get(language, "media_keys")) },
                    description = { Text(Localization.get(language, "media_keys_desc")) },
                    trailing = { Switch(checked = mediaKeysEnabled, onCheckedChange = onMediaKeysChange) },
                    onClick = { onMediaKeysChange(!mediaKeysEnabled) },
                ),
                M3SettingsItem(
                    icon = Icons.Filled.Dock,
                    title = { Text(Localization.get(language, "tray_menu")) },
                    description = { Text(Localization.get(language, "tray_menu_desc")) },
                    trailing = { Switch(checked = trayMenuEnabled, onCheckedChange = onTrayMenuChange) },
                    onClick = { onTrayMenuChange(!trayMenuEnabled) },
                ),
            ),
        )
        Spacer(Modifier.height(24.dp))
    }
}

/* =====================================================================
 * Phase 10 settings sub-screens: Equalizer, Data saver, AI translation
 * (ports of the mobile `EqScreen` / `DataSaverSetting` / `AiSettings`).
 * ===================================================================== */

/**
 * Predefined example EQ profile (V-shape) so new users can see real values and
 * tweak them with the live editor instead of importing a file blindly.
 */
fun exampleEQProfile(): SavedEQProfile = SavedEQProfile(
    id = "example_v_shape",
    name = "Example: V-Shape",
    deviceModel = "VIVI Example",
    bands = listOf(
        ParametricEQBand(frequency = 60.0, gain = 6.0, q = 0.9),
        ParametricEQBand(frequency = 150.0, gain = 3.0, q = 1.0),
        ParametricEQBand(frequency = 400.0, gain = 0.0, q = 1.0),
        ParametricEQBand(frequency = 1000.0, gain = -2.0, q = 1.1),
        ParametricEQBand(frequency = 2500.0, gain = 2.0, q = 1.0),
        ParametricEQBand(frequency = 6000.0, gain = 4.0, q = 0.9),
        ParametricEQBand(frequency = 15000.0, gain = 5.0, q = 0.8),
    ),
    preamp = 0.0,
    isCustom = true,
)

/**
 * A profile made here instead of imported: the ten bands the app's chart and
 * band rows already draw, every gain at 0 dB, so the editor opens on a flat line
 * that can be shaped by hand. Its id is stamped, so two profiles made in the same
 * session stay two profiles.
 */
fun customEQProfile(name: String): SavedEQProfile = SavedEQProfile(
    id = "custom_${System.currentTimeMillis()}",
    name = name,
    deviceModel = name,
    bands = listOf(31.0, 62.0, 125.0, 250.0, 500.0, 1000.0, 2000.0, 4000.0, 8000.0, 16000.0)
        .map { ParametricEQBand(frequency = it, gain = 0.0, q = 1.0) },
    preamp = 0.0,
    isCustom = true,
)

/**
 * Maps an EQ band center frequency to its localized range-name key, so the
 * live editor can show whether a band sits in the sub-bass, bass, low-mid,
 * mid, high-mid or treble region next to its frequency (e.g. "Band 2 · 150 Hz · Bass").
 */
private fun eqRangeKey(frequency: Double): String = when {
    frequency < 60.0 -> "eq_range_sub_bass"
    frequency < 250.0 -> "eq_range_bass"
    frequency < 500.0 -> "eq_range_low_mid"
    frequency < 2000.0 -> "eq_range_mid"
    frequency < 8000.0 -> "eq_range_high_mid"
    else -> "eq_range_treble"
}

/**
 * A band's own line: the frequency and the region it sits in, with its Q beside
 * them.
 *
 * The Q is what sets how wide a band is, and it lived only in the folded *Q
 * factor* card at the bottom of the screen: reading a band's Q meant scrolling to
 * a second list of the same ten frequencies. It is a small muted number on the
 * line of the band it belongs to instead, where it is read without competing with
 * the gain - the number a listener actually turns.
 */
@Composable
private fun EqBandTitle(language: String, band: ParametricEQBand) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text(
            java.lang.String.format(java.util.Locale.US, "%.0f", band.frequency) + " Hz - " +
                Localization.get(language, eqRangeKey(band.frequency)),
        )
        Spacer(Modifier.width(8.dp))
        Text(
            "Q " + java.lang.String.format(java.util.Locale.US, "%.2f", band.q),
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
        )
    }
}

/**
 * The gain a height in the chart means: 0 dB at the middle, +/-12 dB at the two
 * edges, on the sliders' own half-dB stops.
 *
 * One map for both gestures on the curve. A drag sets it on every move; a click
 * sets it once, at the height that was clicked, so a listener can put a peak
 * where it belongs with one click instead of dragging it there by eye.
 */
internal fun eqGainAt(y: Float, height: Float): Double {
    val mid = height / 2f
    val span = mid * 0.92f
    val raw = ((mid - y) / span).coerceIn(-1f, 1f) * 12f
    return (raw * 2f).roundToInt() / 2.0
}

/**
 * What the readout over a dragged band says: the frequency it stands for and the
 * gain it is being given, on the same half-dB stops its own slider snaps to.
 *
 * A drag on the curve moves a height, and a height is not a number: the band's
 * own row (with its `Hz` and its `dB`) is a scroll away from the chart, so the
 * value has to travel with the point under the pointer.
 */
internal fun eqBandReadout(band: ParametricEQBand): String =
    java.lang.String.format(java.util.Locale.US, "%.0f Hz", band.frequency) + "   " +
        (if (band.gain > 0) "+" else "") +
        java.lang.String.format(java.util.Locale.US, "%.1f dB", band.gain)

/**
 * Equalizer: the phone's own list (no equalization, then every profile imported
 * here as an AutoEQ `ParametricEQ.txt`, each with the band count the app shows)
 * and a desktop-shaped editor for the active one.
 *
 * The editor was 10 cards of two sliders each, one card per band, which is a
 * phone column scrolled tall: on a desktop window ten bands stacked vertically
 * read as one long list and the shape of the profile, the only thing an
 * equalizer is about, was nowhere on screen. So the screen is the shape first (a
 * response curve drawing the active profile's gains, redrawn through the
 * animation whenever a slider moves), then one card of band rows carrying the
 * gain: the numbers a listener touches are the gains.
 *
 * The Q of a band is read where the band is named, in small muted text beside
 * its frequency (`EqBandTitle`), and it is not hand-tuned here: it is the width
 * the profile was imported with, and the phone's equalizer has no Q of its own
 * either. A second card of Q sliders stood at the bottom of this screen and said
 * the same ten frequencies again.
 *
 * Everything is the same family as the rest of Settings: [M3SettingsGroup] rows
 * with the group's uneven corners and [M3SettingsItem.slider] for a slider that
 * belongs to its row, so the master Animations switch reaches the fold.
 */
@Composable
fun SettingsEqualizerScreen(
    language: String,
    onBack: () -> Unit,
    profiles: List<SavedEQProfile>,
    activeProfileId: String,
    onSelectProfile: (String?) -> Unit,
    onImportProfile: (String, ParametricEQ) -> Unit,
    onDeleteProfile: (String) -> Unit,
    onAddExampleProfile: () -> Unit = {},
    onCreateProfile: (String) -> Unit = {},
    onUpdateProfile: (String, List<ParametricEQBand>, Double) -> Unit = { _, _, _ -> },
) {
    var error by remember { mutableStateOf<String?>(null) }
    var deleteTarget by remember { mutableStateOf<SavedEQProfile?>(null) }
    var creatingProfile by remember { mutableStateOf(false) }

    fun pickFile(): File? = runCatching {
        val dialog = java.awt.FileDialog(
            null as java.awt.Frame?,
            Localization.get(language, "import_profile"),
            java.awt.FileDialog.LOAD,
        )
        dialog.isVisible = true
        val dir = dialog.directory
        val name = dialog.file
        dialog.dispose()
        if (dir != null && name != null) File(dir, name) else null
    }.getOrNull()

    fun importProfile(file: File) {
        runCatching {
            val eq = ParametricEQParser.parseText(file.readText())
            if (eq.bands.isEmpty()) throw IllegalArgumentException("No filters found")
            onImportProfile(file.nameWithoutExtension, eq)
        }.onFailure {
            error = it.message ?: Localization.get(language, "import_error_title")
        }
    }

    val active = profiles.firstOrNull { it.id == activeProfileId }

    SettingsSubScreen(language, onBack) {
        Text(
            Localization.get(language, "equalizer_header"),
            style = MaterialTheme.typography.headlineSmall,
            modifier = Modifier.padding(bottom = 12.dp),
        )

        // --- The profile list (the phone's own: no equalization + imports) ---
        M3SettingsGroup(
            items = buildList {
                add(
                    M3SettingsItem(
                        icon = Icons.Filled.Block,
                        title = { Text(Localization.get(language, "eq_disabled")) },
                        trailing = { RadioButton(selected = activeProfileId.isEmpty(), onClick = null) },
                        onClick = { onSelectProfile(null) },
                    ),
                )
                profiles.forEach { profile ->
                    add(
                        M3SettingsItem(
                            icon = Icons.Filled.GraphicEq,
                            title = { Text(profile.deviceModel) },
                            description = {
                                Text(
                                    Localization.get(language, "band_count")
                                        .replace("%d", profile.bands.size.toString()),
                                )
                            },
                            trailing = {
                                // The radio that marks the active profile and the delete
                                // action are one line, 4 dp apart, in the same 40 dp
                                // slot: a 20 dp ring beside a 48 dp button read as two
                                // controls stacked when the row was narrow.
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                                ) {
                                    RadioButton(selected = activeProfileId == profile.id, onClick = null)
                                    Tooltip(Localization.get(language, "delete_profile_desc")) {
                                        M3IconButton(
                                            onClick = { deleteTarget = profile },
                                            modifier = Modifier.size(40.dp),
                                        ) {
                                            Icon(
                                                Icons.Filled.Delete,
                                                contentDescription = null,
                                                tint = MaterialTheme.colorScheme.error,
                                            )
                                        }
                                    }
                                }
                            },
                            onClick = { onSelectProfile(profile.id) },
                        ),
                    )
                }
                // A profile can be made here as well as imported: the equalizer is
                // a list of profiles, and until now the only way to get one was a
                // file from AutoEQ or the bundled example, which is not a profile
                // of one's own. It opens the editor on a flat line.
                add(
                    M3SettingsItem(
                        icon = Icons.Filled.Add,
                        title = { Text(Localization.get(language, "new_profile")) },
                        onClick = { creatingProfile = true },
                    ),
                )
                add(
                    M3SettingsItem(
                        icon = Icons.Filled.UploadFile,
                        title = { Text(Localization.get(language, "import_profile")) },
                        description = if (profiles.isEmpty()) {
                            { Text(Localization.get(language, "no_profiles")) }
                        } else {
                            null
                        },
                        onClick = {
                            val file = pickFile()
                            if (file != null) importProfile(file)
                        },
                    ),
                )
                add(
                    M3SettingsItem(
                        icon = Icons.Filled.AutoAwesome,
                        title = { Text(Localization.get(language, "add_example_profile")) },
                        onClick = onAddExampleProfile,
                    ),
                )
            },
        )

        // --- The shape of the active profile, then the controls that move it ---
        // Choosing a profile opens the editor by folding it out from under the
        // list, and choosing *No equalization* folds it back: the curve, the
        // preamp, the bands and the Q factors are one block that arrives with the
        // movement every other switch in Settings uses, instead of four cards
        // dropped in between two frames.
        //
        // The profile the editor draws is the chosen one, and for the length of
        // the collapse the one that was chosen before: without it the block would
        // empty out while its height is still travelling.
        val lastActive = remember { mutableStateOf<SavedEQProfile?>(null) }
        SideEffect { active?.let { lastActive.value = it } }
        val drawn = active ?: lastActive.value
        AnimatedVisibility(
            visible = active != null,
            enter = Animations.rowEnter(),
            exit = Animations.rowExit(),
        ) {
            // One child: AnimatedVisibility lays several out on top of each other.
            Column(Modifier.fillMaxWidth()) {
                val profile = drawn ?: return@Column
                key(profile.id) {
                    var draftBands by remember { mutableStateOf(profile.bands) }
                    var draftPreamp by remember { mutableStateOf(profile.preamp) }
                    fun commit() = onUpdateProfile(profile.id, draftBands, draftPreamp)

                    Spacer(Modifier.height(16.dp))
                    EqualizerResponseCard(
                        language = language,
                        profile = profile,
                        bands = draftBands,
                        modifier = Modifier.fillMaxWidth(),
                        // A point on the curve is the band it stands for: dragging it
                        // up or down is the same edit as the band's own slider, which
                        // is what makes ten numbers into a shape that can be worked
                        // on directly.
                        onBandGainChange = { index, gain ->
                            draftBands = draftBands.toMutableList().also {
                                it[index] = it[index].copy(gain = gain)
                            }
                            commit()
                        },
                    )

                    Spacer(Modifier.height(16.dp))
                    M3SettingsGroup(
                        items = listOf(
                            M3SettingsItem(
                                icon = Icons.Filled.VolumeUp,
                                title = {
                                    Text(
                                        Localization.get(language, "eq_preamp") + ": " +
                                            (if (draftPreamp > 0) "+" else "") +
                                            java.lang.String.format(java.util.Locale.US, "%.1f", draftPreamp) + " dB",
                                    )
                                },
                                slider = {
                                    M3SettingsSlider(
                                        value = draftPreamp.toFloat(),
                                        onValueChange = { draftPreamp = it.toDouble(); commit() },
                                        valueRange = -12f..12f,
                                        steps = 47,
                                        modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                                    )
                                },
                            ),
                        ),
                    )

                    Spacer(Modifier.height(16.dp))
                    M3SettingsGroup(
                        items = draftBands.mapIndexed { index, band ->
                            M3SettingsItem(
                                icon = null,
                                title = { EqBandTitle(language, band) },
                                description = {
                                    Text(
                                        Localization.get(language, "eq_gain") + ": " +
                                            (if (band.gain > 0) "+" else "") +
                                            java.lang.String.format(java.util.Locale.US, "%.1f", band.gain) + " dB",
                                    )
                                },
                                slider = {
                                    M3SettingsSlider(
                                        value = band.gain.toFloat(),
                                        onValueChange = { gain ->
                                            draftBands = draftBands.toMutableList().also {
                                                it[index] = band.copy(gain = gain.toDouble())
                                            }
                                            commit()
                                        },
                                        valueRange = -12f..12f,
                                        steps = 47,
                                        modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                                    )
                                },
                            )
                        },
                    )

                }
            }
        }
    }

    error?.let { message ->
        AlertDialog(
            onDismissRequest = { error = null },
            title = { Text(Localization.get(language, "import_error_title")) },
            text = { Text(message) },
            confirmButton = {
                TextButton(onClick = { error = null }) { Text(Localization.get(language, "ok")) }
            },
        )
    }

    if (creatingProfile) {
        var name by remember { mutableStateOf("") }
        AlertDialog(
            onDismissRequest = { creatingProfile = false },
            title = { Text(Localization.get(language, "new_profile")) },
            text = {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    singleLine = true,
                    label = { Text(Localization.get(language, "new_profile")) },
                )
            },
            confirmButton = {
                TextButton(
                    enabled = name.isNotBlank(),
                    onClick = {
                        if (name.isNotBlank()) {
                            onCreateProfile(name.trim())
                            creatingProfile = false
                        }
                    },
                ) {
                    Text(Localization.get(language, "create"))
                }
            },
            dismissButton = {
                TextButton(onClick = { creatingProfile = false }) {
                    Text(Localization.get(language, "cancel"))
                }
            },
        )
    }

    deleteTarget?.let { profile ->
        AlertDialog(
            onDismissRequest = { deleteTarget = null },
            title = { Text(Localization.get(language, "delete_profile_desc")) },
            text = {
                Text(
                    Localization.get(language, "delete_profile_confirmation")
                        .replace("%1\$s", profile.name)
                        .replace("%s", profile.name),
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    onDeleteProfile(profile.id)
                    deleteTarget = null
                }) {
                    Text(Localization.get(language, "delete"))
                }
            },
            dismissButton = {
                TextButton(onClick = { deleteTarget = null }) {
                    Text(Localization.get(language, "cancel"))
                }
            },
        )
    }
}

/**
 * The active profile's response, drawn band by band.
 *
 * The gains are the numbers the band rows below edit, so this is not decoration:
 * it is the only place where the ten values read as one shape. Each band's height
 * comes through `animateFloatAsState` on the same 200 ms the rows fold on, so
 * dragging a gain redraws the curve through the movement instead of jumping the
 * line under the finger, and it follows the master Animations switch like every
 * other transition.
 *
 * While a point is dragged, the band it belongs to is named over it - its
 * frequency and the gain being set ([eqBandReadout]) - because the row that says
 * the same numbers is below the chart: the point is what the pointer holds, and a
 * height on its own is not a value.
 */
@Composable
internal fun EqualizerResponseCard(
    language: String,
    profile: SavedEQProfile,
    bands: List<ParametricEQBand>,
    modifier: Modifier = Modifier,
    onBandGainChange: (index: Int, gain: Double) -> Unit = { _, _ -> },
) {
    val gains = bands.map { band ->
        animateFloatAsState(
            targetValue = band.gain.toFloat(),
            animationSpec = tween(Animations.rowMs(), easing = Animations.emphasizedDecelerate),
            label = "eqBandGain",
        ).value
    }
    val curve = MaterialTheme.colorScheme.primary
    val grid = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.18f)
    val fill = curve.copy(alpha = 0.16f)
    // The readout of the band being dragged: a small pill of the card's own
    // raised surface, so it sits over the curve instead of hiding behind it.
    val readout = rememberTextMeasurer()
    val readoutStyle = MaterialTheme.typography.labelMedium.copy(
        color = MaterialTheme.colorScheme.onSurface,
        fontWeight = FontWeight.Medium,
    )
    val pillColor = MaterialTheme.colorScheme.surfaceContainerHigh
    val pillBorder = MaterialTheme.colorScheme.outlineVariant

    Card(
        modifier = modifier,
        shape = MaterialTheme.shapes.extraLarge,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
    ) {
        Column(Modifier.fillMaxWidth().padding(horizontal = 17.dp, vertical = 14.dp)) {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    Icons.Filled.ShowChart,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                )
                Spacer(Modifier.width(12.dp))
                Column(Modifier.weight(1f)) {
                    Text(profile.deviceModel, style = MaterialTheme.typography.titleSmall)
                    Text(
                        Localization.get(language, "band_count").replace("%d", bands.size.toString()),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
            // Which band the gesture in flight owns. Kept outside the drawing so a
            // vertical drag on one point does not slide to its neighbour when the
            // pointer drifts sideways: the point under the finger is the one that
            // moves, and the curve redraws through the same animated gains the
            // sliders below drive.
            var dragIndex by remember(bands.size) { mutableStateOf(-1) }
            // A click (a press that never moved) is the second way to set a band:
            // it takes the gain of the height clicked. Its readout stays up for a
            // second after the click, because a single half-dB step is a small
            // move on the curve and the pill is what says the click landed.
            var tapIndex by remember(bands.size) { mutableStateOf(-1) }
            var tapTick by remember { mutableStateOf(0) }
            LaunchedEffect(tapTick) {
                if (tapTick == 0) return@LaunchedEffect
                delay(1000)
                tapIndex = -1
            }
            // Which band the chart is naming: the dragged one while a gesture is
            // in flight, the clicked one for as long as its readout lasts.
            val namedIndex = if (dragIndex in bands.indices) dragIndex else tapIndex
            // The gesture block outlives a recomposition, so the callback it calls
            // is the one in force rather than the one it started with.
            val reportGain = rememberUpdatedState(onBandGainChange)
            Canvas(
                Modifier
                    .fillMaxWidth()
                    .height(132.dp)
                    .padding(top = 12.dp)
                    // The curve is a control, so the pointer says so over it.
                    .pointerHoverIcon(PointerIcon.Hand)
                    // One gesture block for both edits, because a click is what a
                    // drag is when it never moved: the press is followed until
                    // either the slop is passed (a drag, from there on) or the
                    // pointer goes up (a click).
                    //
                    // A drag takes the band under the pointer and sets its gain to
                    // the height of every move; a click takes the height that was
                    // clicked, but only on a point - within the 24 dp a dot is
                    // aimed at. Anywhere else a click would move a value the
                    // listener did not mean to touch, so it does nothing.
                    .pointerInput(bands.size) {
                        val radius = 24.dp.toPx()
                        fun bandX(index: Int): Float = if (bands.size > 1) {
                            size.width * index / (bands.size - 1).toFloat()
                        } else {
                            0f
                        }
                        fun bandY(index: Int): Float {
                            val mid = size.height / 2f
                            val span = mid * 0.92f
                            return mid - (bands[index].gain.toFloat() / 12f).coerceIn(-1f, 1f) * span
                        }
                        // The point under the press, or -1 when the press was not on
                        // one. The nearest point wins, so overlapping dots on a
                        // crowded profile still pick the one being aimed at.
                        fun pointAt(position: Offset): Int {
                            var nearest = -1
                            var distance = radius
                            bands.indices.forEach { index ->
                                val d = hypot(position.x - bandX(index), position.y - bandY(index))
                                if (d <= distance) {
                                    nearest = index
                                    distance = d
                                }
                            }
                            return nearest
                        }
                        // The column under the press: a drag works anywhere on the
                        // chart, and the nearest frequency in x is the one it means.
                        fun columnAt(x: Float): Int {
                            if (bands.size < 2) return 0
                            val step = size.width / (bands.size - 1).toFloat()
                            return (x / step).roundToInt().coerceIn(0, bands.lastIndex)
                        }
                        suspend fun AwaitPointerEventScope.run() {
                            val down = awaitFirstDown(requireUnconsumed = false)
                            val slop = awaitTouchSlopOrCancellation(down.id) { change, _ ->
                                change.consume()
                            }
                            if (slop == null) {
                                // The pointer went up without passing the slop: a
                                // click on a point sets that band, one on nothing
                                // leaves every gain as it was.
                                val index = pointAt(down.position)
                                if (index >= 0) {
                                    reportGain.value(index, eqGainAt(down.position.y, size.height.toFloat()))
                                    tapIndex = index
                                    tapTick += 1
                                }
                                return
                            }
                            var index = columnAt(slop.position.x)
                            dragIndex = index
                            reportGain.value(index, eqGainAt(slop.position.y, size.height.toFloat()))
                            drag(slop.id) { change ->
                                index = if (index in bands.indices) index else columnAt(change.position.x)
                                reportGain.value(index, eqGainAt(change.position.y, size.height.toFloat()))
                                change.consume()
                            }
                            dragIndex = -1
                        }
                        awaitEachGesture { run() }
                    },
            ) {
                val mid = size.height / 2f
                // 0 dB in the middle, +12 at the top edge and -12 at the bottom.
                repeat(5) { line ->
                    val y = size.height * line / 4f
                    drawLine(
                        color = if (line == 2) grid.copy(alpha = 0.4f) else grid,
                        start = Offset(0f, y),
                        end = Offset(size.width, y),
                        strokeWidth = if (line == 2) 2f else 1f,
                    )
                }
                if (gains.size >= 2) {
                    val step = size.width / (gains.size - 1).toFloat()
                    val points = gains.mapIndexed { index, gain ->
                        Offset(
                            x = step * index,
                            y = mid - (gain / 12f).coerceIn(-1f, 1f) * mid * 0.92f,
                        )
                    }
                    val line = Path().apply {
                        moveTo(points.first().x, points.first().y)
                        points.zipWithNext().forEach { (from, to) ->
                            val controlX = (from.x + to.x) / 2f
                            cubicTo(controlX, from.y, controlX, to.y, to.x, to.y)
                        }
                    }
                    val area = Path().apply {
                        addPath(line)
                        lineTo(points.last().x, size.height)
                        lineTo(points.first().x, size.height)
                        close()
                    }
                    drawPath(area, fill)
                    drawPath(line, curve, style = Stroke(width = 3f))
                    // The band under the pointer is drawn larger, so a drag says
                    // which of the ten values it is moving.
                    points.forEachIndexed { index, point ->
                        drawCircle(
                            color = curve,
                            radius = if (index == namedIndex) 6f else 3.5f,
                            center = point,
                        )
                    }
                    // The band being dragged says what it is: its frequency and
                    // the gain it is being given, on a pill just above its point,
                    // held inside the chart so a band at either end is still read.
                    val active = namedIndex.takeIf { it in bands.indices }
                    if (active != null) {
                        val band = bands[active]
                        val laid = readout.measure(AnnotatedString(eqBandReadout(band)), readoutStyle)
                        val padH = 9.dp.toPx()
                        val padV = 4.dp.toPx()
                        val width = laid.size.width + padH * 2
                        val height = laid.size.height + padV * 2
                        val point = points[active]
                        val x = (point.x - width / 2f).coerceIn(0f, size.width - width)
                        val y = (point.y - height - 12f).coerceIn(0f, size.height - height)
                        val corner = CornerRadius(height / 2f)
                        drawRoundRect(
                            color = pillColor,
                            topLeft = Offset(x, y),
                            size = Size(width, height),
                            cornerRadius = corner,
                        )
                        drawRoundRect(
                            color = pillBorder,
                            topLeft = Offset(x, y),
                            size = Size(width, height),
                            cornerRadius = corner,
                            style = Stroke(width = 1f),
                        )
                        drawText(laid, topLeft = Offset(x + padH, y + padV))
                    }
                }
            }
        }
    }
}

/** Data saver: master toggle + the list of what gets turned off. */
@Composable
fun SettingsDataSaverScreen(
    language: String,
    onBack: () -> Unit,
    dataSaver: Boolean,
    onToggleDataSaver: (Boolean) -> Unit,
) {
    SettingsSubScreen(language, onBack) {
        Text(
            Localization.get(language, "data_saver"),
            style = MaterialTheme.typography.titleLarge,
            modifier = Modifier.padding(vertical = 12.dp),
        )

        Text(
            Localization.get(language, "data_saver_desc"),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(bottom = 16.dp),
        )

        M3SettingsGroup(
            items = listOf(
                M3SettingsItem(
                    icon = Icons.Filled.EnergySavingsLeaf,
                    title = { Text(Localization.get(language, "data_saver")) },
                    trailing = { Switch(checked = dataSaver, onCheckedChange = onToggleDataSaver) },
                    onClick = { onToggleDataSaver(!dataSaver) },
                ),
            ),
        )

        Spacer(Modifier.height(16.dp))

        Text(
            Localization.get(language, "data_saver_turns_off_header"),
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.padding(top = 8.dp, bottom = 4.dp),
        )
        listOf(
            "data_saver_player_canvas",
            "data_saver_artist_video",
            "data_saver_artist_bg_video",
            "data_saver_album_canvas",
            "data_saver_high_quality_images",
        ).forEach { key ->
            Text(
                "•  " + Localization.get(language, key),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(vertical = 3.dp),
            )
        }
        Spacer(Modifier.height(24.dp))
    }
}

/** The three DeepL formality choices, in the page's order. */
private val DEEPL_FORMALITY_CHOICES = listOf("default", "more", "less")

private fun deeplFormalityLabel(language: String, formality: String): String = when (formality) {
    "more" -> Localization.get(language, "ai_deepl_formality_more")
    "less" -> Localization.get(language, "ai_deepl_formality_less")
    else -> Localization.get(language, "ai_deepl_formality_default")
}

/** A key as the row says it: masked when there is one, "Not set" when there is not. */
private fun maskedKey(key: String, language: String): String =
    if (key.isNotEmpty()) "•".repeat(minOf(key.length, 8)) else Localization.get(language, "not_set")

/** AI lyrics translation settings (provider, keys, model, target language…). */
@Composable
fun SettingsAiScreen(
    language: String,
    onBack: () -> Unit,
    aiProvider: String,
    aiApiKey: String,
    aiBaseUrl: String,
    aiModel: String,
    translateLanguage: String,
    translateMode: String,
    deeplApiKey: String,
    deeplFormality: String,
    onAiProviderChange: (String) -> Unit,
    onAiApiKeyChange: (String) -> Unit,
    onAiBaseUrlChange: (String) -> Unit,
    onAiModelChange: (String) -> Unit,
    onTranslateLanguageChange: (String) -> Unit,
    onTranslateModeChange: (String) -> Unit,
    onDeeplApiKeyChange: (String) -> Unit,
    onDeeplFormalityChange: (String) -> Unit,
) {
    val aiProviders = mapOf(
        "OpenRouter" to "https://openrouter.ai/api/v1/chat/completions",
        "OpenAI" to "https://api.openai.com/v1/chat/completions",
        "Perplexity" to "https://api.perplexity.ai/chat/completions",
        "Claude" to "https://api.anthropic.com/v1/messages",
        "Gemini" to "https://generativelanguage.googleapis.com/v1beta/openai/chat/completions",
        "XAi" to "https://api.x.ai/v1/chat/completions",
        "Mistral" to "https://api.mistral.ai/v1/chat/completions",
        "DeepL" to "https://api.deepl.com/v2/translate",
        "Custom" to "",
    )
    val modelsByProvider = mapOf(
        "OpenRouter" to listOf(
            "google/gemini-2.5-flash-lite", "google/gemini-2.5-flash", "x-ai/grok-4.1-fast",
            "deepseek/deepseek-v3.1-terminus:exacto", "openai/gpt-4o-mini", "google/gemini-3-flash-preview",
            "apodex/apodex-1.1-mini:free", "qwen/qwen3.8-27b:free",
        ),
        "OpenAI" to listOf("gpt-4o-mini", "gpt-4o", "gpt-4-turbo"),
        "Claude" to listOf("claude-3-5-haiku-latest", "claude-3-5-sonnet-latest", "claude-3-opus-latest"),
        "Gemini" to listOf(
            "gemini-2.5-flash-lite", "gemini-2.5-flash", "gemini-2.5-pro", "gemini-2.0-flash",
            "gemini-1.5-flash", "gemini-3.5-flash", "gemini-3-flash", "gemini-3.1-flash-lite", "gemini-3.1-pro",
        ),
        "Perplexity" to listOf("sonar", "sonar-pro", "sonar-reasoning"),
        "XAi" to listOf("grok-4-1-fast", "grok-vision-beta"),
        "Mistral" to listOf("mistral-large-latest", "mistral-medium-latest", "mistral-small-latest", "mistral-tiny-latest"),
        "DeepL" to emptyList(),
        "Custom" to emptyList(),
    )

    var editingKey by remember { mutableStateOf<String?>(null) }
    var keyInput by remember { mutableStateOf("") }
    var editingBaseUrl by remember { mutableStateOf(false) }
    var baseUrlInput by remember { mutableStateOf("") }
    var editingModel by remember { mutableStateOf(false) }
    var modelInput by remember { mutableStateOf("") }

    SettingsSubScreen(language, onBack) {
        Text(
            Localization.get(language, "ai_lyrics_translation"),
            style = MaterialTheme.typography.titleLarge,
            modifier = Modifier.padding(vertical = 12.dp),
        )

        // Provider: the choice the rest of the screen hangs off, in the card the
        // page gives it. Changing it writes that provider's base URL and resets the
        // model to the first of its own list, so a gpt- id can never stay behind
        // under Gemini.
        M3SettingsGroup(
            title = Localization.get(language, "ai_provider"),
            items = listOf(
                M3SettingsItem(
                    icon = Icons.Filled.SmartToy,
                    title = { Text(Localization.get(language, "ai_provider")) },
                    description = { Text(aiProvider) },
                    dropdown = M3Dropdown(
                        value = aiProvider,
                        options = aiProviders.keys.map { it to it },
                        onSelect = { p ->
                            onAiBaseUrlChange(if (p == "Custom" || p == "DeepL") "" else aiProviders[p] ?: "")
                            onAiProviderChange(p)
                            onAiModelChange(modelsByProvider[p]?.firstOrNull() ?: "")
                        },
                    ),
                ),
            ),
        )

        Spacer(Modifier.height(16.dp))

        // Keys, base URL and model: one card, titled with the page's own words
        // (`ai_setup_guide` is "API Credentials").
        M3SettingsGroup(
            title = Localization.get(language, "ai_setup_guide"),
            items = if (aiProvider == "DeepL") {
                listOf(
                    M3SettingsItem(
                        icon = Icons.Filled.Key,
                        title = { Text("DeepL " + Localization.get(language, "ai_api_key")) },
                        description = { Text(maskedKey(deeplApiKey, language)) },
                        onClick = { editingKey = "deepl"; keyInput = deeplApiKey },
                    ),
                    M3SettingsItem(
                        icon = Icons.Filled.Tune,
                        title = { Text(Localization.get(language, "ai_deepl_formality")) },
                        description = { Text(deeplFormalityLabel(language, deeplFormality)) },
                        dropdown = M3Dropdown(
                            value = deeplFormalityLabel(language, deeplFormality),
                            options = DEEPL_FORMALITY_CHOICES.map { it to deeplFormalityLabel(language, it) },
                            onSelect = onDeeplFormalityChange,
                        ),
                    ),
                )
            } else {
                listOf(
                    M3SettingsItem(
                        icon = Icons.Filled.Key,
                        title = { Text(Localization.get(language, "ai_api_key")) },
                        description = { Text(maskedKey(aiApiKey, language)) },
                        onClick = { editingKey = "main"; keyInput = aiApiKey },
                    ),
                    M3SettingsItem(
                        icon = Icons.Filled.Link,
                        title = { Text(Localization.get(language, "ai_base_url")) },
                        description = { Text(aiBaseUrl.ifBlank { Localization.get(language, "not_set") }) },
                        onClick = { editingBaseUrl = true; baseUrlInput = aiBaseUrl },
                    ),
                    M3SettingsItem(
                        icon = Icons.Filled.ModelTraining,
                        title = { Text(Localization.get(language, "ai_model")) },
                        description = { Text(aiModel.ifBlank { Localization.get(language, "not_set") }) },
                        // A custom endpoint has no catalogue to pick from: the model
                        // is whatever that endpoint takes, so the row folds away.
                        visible = aiProvider != "Custom",
                        dropdown = M3Dropdown(
                            // The page names the model in full under the row and only
                            // its last segment in the row, while the menu marks the
                            // choice by its whole id - so the chip and the mark are
                            // two values here.
                            value = aiModel.substringAfterLast('/').ifBlank {
                                Localization.get(language, "not_set")
                            },
                            selectedKey = aiModel,
                            options = (modelsByProvider[aiProvider] ?: emptyList()).map { it to it } +
                                ("__custom__" to Localization.get(language, "ai_custom_model")),
                            onSelect = { m ->
                                if (m == "__custom__") {
                                    modelInput = aiModel
                                    editingModel = true
                                } else {
                                    onAiModelChange(m)
                                }
                            },
                        ),
                    ),
                )
            },
        )

        if (aiProvider != "DeepL" && aiProvider != "Custom") {
            Text(
                Localization.get(language, "ai_training_note"),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = 4.dp).padding(top = 8.dp),
            )
        }

        Spacer(Modifier.height(16.dp))

        // Translation mode and target language: the page's last card. DeepL is not
        // asked for a mode (it only translates), so that row folds away for it
        // instead of the card being rebuilt without it.
        M3SettingsGroup(
            title = Localization.get(language, "ai_translation_mode"),
            items = listOf(
                M3SettingsItem(
                    icon = Icons.Filled.Translate,
                    title = { Text(Localization.get(language, "ai_translation_mode")) },
                    description = {
                        Text(
                            if (translateMode == "Transcribed") {
                                Localization.get(language, "ai_translation_transcribed")
                            } else {
                                Localization.get(language, "ai_translation_literal")
                            },
                        )
                    },
                    visible = aiProvider != "DeepL",
                    dropdown = M3Dropdown(
                        value = if (translateMode == "Transcribed") {
                            Localization.get(language, "ai_translation_transcribed")
                        } else {
                            Localization.get(language, "ai_translation_literal")
                        },
                        options = listOf(
                            "Literal" to Localization.get(language, "ai_translation_literal"),
                            "Transcribed" to Localization.get(language, "ai_translation_transcribed"),
                        ),
                        onSelect = onTranslateModeChange,
                    ),
                ),
                M3SettingsItem(
                    icon = Icons.Filled.Language,
                    title = { Text(Localization.get(language, "ai_target_language")) },
                    description = { Text(LanguageCodeToName[translateLanguage] ?: translateLanguage) },
                    dropdown = M3Dropdown(
                        value = LanguageCodeToName[translateLanguage] ?: translateLanguage,
                        options = LanguageCodeToName.toList().sortedBy { it.second }.map { it.first to it.second },
                        onSelect = onTranslateLanguageChange,
                    ),
                ),
            ),
        )

        Spacer(Modifier.height(24.dp))
    }

    // API key dialog
    editingKey?.let { which ->
        AlertDialog(
            onDismissRequest = { editingKey = null },
            title = { Text(Localization.get(language, "ai_api_key")) },
            text = {
                OutlinedTextField(
                    value = keyInput,
                    onValueChange = { keyInput = it },
                    singleLine = true,
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    when (which) {
                        "main" -> onAiApiKeyChange(keyInput.trim())
                        "deepl" -> onDeeplApiKeyChange(keyInput.trim())
                    }
                    editingKey = null
                }) {
                    Text(Localization.get(language, "save"))
                }
            },
            dismissButton = {
                TextButton(onClick = { editingKey = null }) { Text(Localization.get(language, "cancel")) }
            },
        )
    }

    // Base URL dialog
    if (editingBaseUrl) {
        AlertDialog(
            onDismissRequest = { editingBaseUrl = false },
            title = { Text(Localization.get(language, "ai_base_url")) },
            text = {
                OutlinedTextField(
                    value = baseUrlInput,
                    onValueChange = { baseUrlInput = it },
                    singleLine = true,
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    onAiBaseUrlChange(baseUrlInput.trim())
                    editingBaseUrl = false
                }) {
                    Text(Localization.get(language, "save"))
                }
            },
            dismissButton = {
                TextButton(onClick = { editingBaseUrl = false }) { Text(Localization.get(language, "cancel")) }
            },
        )
    }

    // Custom model dialog (the "Custom" entry in the model dropdown).
    if (editingModel) {
        AlertDialog(
            onDismissRequest = { editingModel = false },
            title = { Text(Localization.get(language, "ai_custom_model")) },
            text = {
                OutlinedTextField(
                    value = modelInput,
                    onValueChange = { modelInput = it },
                    singleLine = true,
                    placeholder = { Text("vendor/model-id") },
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    onAiModelChange(modelInput.trim())
                    editingModel = false
                }) {
                    Text(Localization.get(language, "save"))
                }
            },
            dismissButton = {
                TextButton(onClick = { editingModel = false }) { Text(Localization.get(language, "cancel")) }
            },
        )
    }
}

@Composable
fun SettingsScreen(
    language: String,
    themeMode: ThemeMode,
    isLoggedIn: Boolean,
    accountName: String,
    updateStatus: UpdateStatus,
    wrappedStats: WrappedStats = WrappedStats(),
    onOpen: (Screen) -> Unit,
) {
    val devEnabled by DeveloperOptions.enabled.collectAsState()
    var searchActive by remember { mutableStateOf(false) }
    var searchQuery by remember { mutableStateOf("") }

    val themeLabel = when (themeMode) {
        ThemeMode.SYSTEM -> Localization.get(language, "theme_system")
        ThemeMode.LIGHT -> Localization.get(language, "theme_light")
        ThemeMode.DARK -> Localization.get(language, "theme_dark")
    }
    // The four groups below mirror the mobile app's settings screen, in the
    // same order and with the same composition: the account/update group, the
    // media & player group (where mobile also keeps the lyrics options, inside
    // Appearance), the features & data group and the system & support group.
    val generalItems = listOf(
        M3SettingsItem(
            icon = Icons.Filled.Refresh,
            title = { Text(Localization.get(language, "updates")) },
            description = {
                Text(if (updateStatus is UpdateStatus.Available) Localization.get(language, "update_available") else AppInfo.FULL_VERSION)
            },
            trailing = { SettingsChevron() },
            onClick = { onOpen(Screen.SettingsUpdates) },
        ),
        M3SettingsItem(
            icon = Icons.Filled.Person,
            title = { Text(Localization.get(language, "account")) },
            description = { Text(if (isLoggedIn) accountName.ifBlank { "YouTube" } else Localization.get(language, "not_logged_in")) },
            trailing = { SettingsChevron() },
            onClick = { onOpen(Screen.SettingsAccount) },
        ),
        M3SettingsItem(
            icon = Icons.Filled.Notifications,
            title = { Text(Localization.get(language, "notifications")) },
            description = {
                Text(Localization.get(language, if (DesktopSettings.load().notificationMode == "native") "notification_native" else "notification_main_window"))
            },
            trailing = { SettingsChevron() },
            onClick = { onOpen(Screen.SettingsNotifications) },
        ),
    )
    val mediaItems = listOf(
        M3SettingsItem(
            icon = Icons.Filled.Palette,
            title = { Text(Localization.get(language, "appearance")) },
            description = { Text(themeLabel) },
            trailing = { SettingsChevron() },
            onClick = { onOpen(Screen.SettingsAppearance) },
        ),
        M3SettingsItem(
            icon = Icons.Filled.Lyrics,
            title = { Text(Localization.get(language, "lyrics")) },
            trailing = { SettingsChevron() },
            onClick = { onOpen(Screen.SettingsLyrics) },
        ),
        M3SettingsItem(
            icon = Icons.Filled.GraphicEq,
            title = { Text(Localization.get(language, "player_audio")) },
            trailing = { SettingsChevron() },
            onClick = { onOpen(Screen.SettingsPlayer) },
        ),
        M3SettingsItem(
            icon = Icons.Filled.Devices,
            title = { Text(Localization.get(language, "device_sync")) },
            trailing = { SettingsChevron() },
            onClick = { onOpen(Screen.SettingsDevices) },
        ),
        M3SettingsItem(
            icon = Icons.Filled.Language,
            title = { Text(Localization.get(language, "content")) },
            trailing = { SettingsChevron() },
            onClick = { onOpen(Screen.SettingsContent) },
        ),
        M3SettingsItem(
            icon = Icons.Filled.Translate,
            title = { Text(Localization.get(language, "language")) },
            description = { Text(Languages.name(language)) },
            trailing = { SettingsChevron() },
            onClick = { onOpen(Screen.SettingsLanguage) },
        ),
    )
    val privacyItems = listOf(
        M3SettingsItem(
            icon = Icons.Filled.AutoAwesome,
            title = { Text(Localization.get(language, "ai_lyrics_translation")) },
            trailing = { SettingsChevron() },
            onClick = { onOpen(Screen.SettingsAi) },
        ),
        M3SettingsItem(
            icon = Icons.Filled.Security,
            title = { Text(Localization.get(language, "privacy")) },
            trailing = { SettingsChevron() },
            onClick = { onOpen(Screen.SettingsPrivacy) },
        ),
        M3SettingsItem(
            icon = Icons.Filled.Storage,
            title = { Text(Localization.get(language, "storage")) },
            trailing = { SettingsChevron() },
            onClick = { onOpen(Screen.SettingsStorage) },
        ),
        M3SettingsItem(
            icon = Icons.Filled.EnergySavingsLeaf,
            title = { Text(Localization.get(language, "data_saver")) },
            trailing = { SettingsChevron() },
            onClick = { onOpen(Screen.SettingsDataSaver) },
        ),
    )
    val aboutItems = listOf(
        M3SettingsItem(
            icon = Icons.Filled.SettingsBackupRestore,
            title = { Text(Localization.get(language, "backup_restore")) },
            trailing = { SettingsChevron() },
            onClick = { onOpen(Screen.SettingsBackup) },
        ),
        M3SettingsItem(
            icon = Icons.Filled.AutoAwesome,
            title = { Text(Localization.get(language, "wrapped_title")) },
            description = { Text("${wrappedStats.trackStarts} ${Localization.get(language, "wrapped_tracks")}") },
            trailing = { SettingsChevron() },
            onClick = { onOpen(Screen.SettingsWrapped) },
        ),
        M3SettingsItem(
            icon = Icons.Filled.Tune,
            title = { Text(Localization.get(language, "integrations")) },
            description = {
                Text(if (DesktopSettings.load().discordRpcEnabled || DesktopSettings.load().lastfmEnabled) Localization.get(language, "integrations_active") else Localization.get(language, "integrations_inactive"))
            },
            trailing = { SettingsChevron() },
            onClick = { onOpen(Screen.SettingsIntegrations) },
        ),
        M3SettingsItem(
            // The Spotify mark rather than a generic library icon: the entry is
            // "import from Spotify", and the phone has drawn it this way all
            // along (same path data as its own drawable).
            icon = SpotifyIcon,
            title = { Text(Localization.get(language, "spotify_import")) },
            description = {
                val name = SpotifyImport.state.collectAsState().value.accountName
                Text(
                    if (name.isBlank()) Localization.get(language, "spotify_not_connected")
                    else Localization.get(language, "spotify_connected_as").format(name),
                )
            },
            trailing = { SettingsChevron() },
            onClick = { onOpen(Screen.SettingsSpotifyImport) },
        ),
        M3SettingsItem(
            icon = Icons.Filled.DesktopWindows,
            title = { Text(Localization.get(language, "desktop_features")) },
            trailing = { SettingsChevron() },
            onClick = { onOpen(Screen.SettingsDesktop) },
        ),
        M3SettingsItem(
            icon = Icons.Filled.Build,
            title = { Text(Localization.get(language, "system")) },
            description = { Text(if (devEnabled) Localization.get(language, "performance_options_enabled") else Localization.get(language, "dev_tools_disabled")) },
            trailing = { SettingsChevron() },
            onClick = { onOpen(Screen.SettingsSystem) },
        ),
        M3SettingsItem(
            icon = Icons.Filled.Info,
            title = { Text(Localization.get(language, "about")) },
            trailing = { SettingsChevron() },
            onClick = { onOpen(Screen.SettingsAbout) },
        ),
    )

    val allRows = listOf(
        "language" to Localization.get(language, "language"),
        "updates" to Localization.get(language, "updates"),
        "notifications" to Localization.get(language, "notifications"),
        "appearance" to Localization.get(language, "appearance"),
        "player_audio" to Localization.get(language, "player_audio"),
        "account" to Localization.get(language, "account"),
        "device_sync" to Localization.get(language, "device_sync"),
        "content" to Localization.get(language, "content"),
        "ai_lyrics_translation" to Localization.get(language, "ai_lyrics_translation"),
        "lyrics" to Localization.get(language, "lyrics"),
        "privacy" to Localization.get(language, "privacy"),
        "data_saver" to Localization.get(language, "data_saver"),
        "storage" to Localization.get(language, "storage"),
        "backup_restore" to Localization.get(language, "backup_restore"),
        "wrapped_title" to Localization.get(language, "wrapped_title"),
        "integrations" to Localization.get(language, "integrations"),
        "desktop_features" to Localization.get(language, "desktop_features"),
        "system" to Localization.get(language, "system"),
        "about" to Localization.get(language, "about"),
    )

    val rowSearchTerms: Map<String, List<String>> = mapOf(
        "language" to listOf("choose_language", "content_language", "language"),
        "updates" to listOf("update_available", "up_to_date", "check_updates", "update_check_interval", "include_prereleases", "update_source", "changelog", "commits", "latest_release", "install_now", "restart_now", "open_release_page"),
        "notifications" to listOf("notification_duration", "notification_duration_desc", "notification_history", "notification_main_window", "notification_mode", "notification_mode_desc", "notification_native", "notifications", "save_notification_history", "test_notification"),
        "appearance" to listOf("animations", "animations_desc", "app_font", "appearance", "density_and_grid", "intro", "native_title_bar", "native_title_bar_desc", "native_title_bar_desc_hint", "player_design", "restart_required", "restart_required_title", "right_panel", "right_panel_desc", "screen_transitions", "show_intro_on_startup", "theme_colors", "vivimusic_canvas", "accent_intensity", "add_to_palette", "brightness", "color_palette", "custom_color", "custom_colors", "hue", "pure_black", "remove_custom_color", "saturation", "theme_dark", "theme_light", "theme_mode", "theme_system", "custom_font", "font_google_sans", "font_outfit", "font_plus_jakarta_sans", "font_sans_flex", "font_selection", "font_system", "import_font", "preview_text_quote", "typography_preview", "canvas_source", "vivimusic_canvas_desc", "grid_item_size", "transition_fade", "transition_off", "transition_slide", "mini_player_background", "mini_player_design", "player_background", "pure_black_mini", "pure_black_mini_desc", "rotating_thumbnail", "rotating_thumbnail_desc", "intro_background", "intro_desc", "intro_style", "preview_intro"),
        "player_audio" to listOf("add_example_profile", "new_profile", "band_count", "delete_profile_confirmation", "delete_profile_desc", "eq_band", "eq_disabled", "eq_edit_profile", "eq_gain", "eq_preamp", "eq_q_factor", "equalizer_header", "import_error_title", "import_profile", "no_profiles", "mini_player_background", "mini_player_design", "player_background", "player_design", "pure_black_mini", "pure_black_mini_desc", "rotating_thumbnail", "rotating_thumbnail_desc", "autoplay_next", "audio_quality", "audio_quality_auto", "audio_quality_high", "audio_quality_low", "persistent_queue", "remember_shuffle_repeat", "sync_vivi_volume", "stream_cache_minutes", "stream_cache_minutes_desc", "stream_cache_forever", "eq_range_sub_bass", "eq_range_bass", "eq_range_low_mid", "eq_range_mid", "eq_range_high_mid", "eq_range_treble"),
        "account" to listOf("clear_session", "logout", "login", "login_google", "login_manual_title", "logged_in_as", "not_logged_in", "account", "cookie_label", "visitor_data_label", "data_sync_id_label"),
        "device_sync" to listOf("connection_method", "method_relay", "method_lan", "relay_server", "lan_sync", "connect", "generate_code", "regenerate_pair_code", "code_expires_in", "code_hint", "lan_hint", "pair", "unpair", "scan_qr", "connected", "disconnected", "status", "download_mobile_apk", "how_to_connect", "waiting_for_pairing"),
        "content" to listOf("content", "content_country", "content_language", "system_default"),
        "ai_lyrics_translation" to listOf("ai_api_key", "ai_base_url", "ai_deepl_formality", "ai_deepl_formality_default", "ai_deepl_formality_less", "ai_deepl_formality_more", "ai_lyrics_translation", "ai_model", "ai_provider", "ai_target_language", "ai_translation_literal", "ai_translation_mode", "ai_translation_transcribed", "not_set", "ai_setup_guide", "ai_custom_model", "ai_training_note"),
        "lyrics" to listOf(
            "lyrics", "lyrics_line_spacing", "lyrics_text_size", "synced_lyrics", "synced_lyrics_desc", "lyrics_focus",
            // Advanced lyrics (mobile port): animation styles, display options,
            // romanization and translation.
            "lyrics_animation_style", "lyrics_animation_style_desc",
            "lyrics_style_none", "lyrics_style_fade", "lyrics_style_glow", "lyrics_style_slide",
            "lyrics_style_karaoke", "lyrics_style_apple", "lyrics_style_apple_v2", "lyrics_style_vivimusic",
            "lyrics_style_lyrics_v2", "lyrics_style_metro", "animation_speed",
            "lyrics_glow_effect", "lyrics_glow_effect_desc",
            "lyrics_apple_blur", "lyrics_apple_blur_desc", "lyrics_standard_blur", "lyrics_standard_blur_desc",
            "lyrics_click_to_seek", "lyrics_click_to_seek_desc", "lyrics_auto_scroll", "lyrics_auto_scroll_desc",
            "lyrics_text_position", "lyrics_position_left", "lyrics_position_center", "lyrics_position_right",
            "lyrics_romanize", "lyrics_romanize_desc", "lyrics_romanize_as_main", "lyrics_romanize_as_main_desc",
            "romanize_japanese", "romanize_korean", "romanize_chinese", "romanize_russian",
            "romanize_ukrainian", "romanize_serbian", "romanize_bulgarian", "romanize_belarusian",
            "romanize_kyrgyz", "romanize_macedonian", "romanize_hindi", "romanize_punjabi",
            "translate_lyrics", "translate_lyrics_desc", "ai_lyrics_translation",
        ),
        "privacy" to listOf("clear_search_history", "pause_listen_history", "pause_search_history", "privacy", "privacy_desc"),
        "data_saver" to listOf("data_saver", "data_saver_desc", "data_saver_turns_off_header", "data_saver_album_canvas", "data_saver_player_canvas", "data_saver_artist_video", "data_saver_artist_bg_video", "data_saver_high_quality_images"),
        "storage" to listOf(
            "storage",
            "cache_size",
            "clear_cache",
            "cache_cleared",
            "delete_installers",
            "installers_deleted",
            // Both cards of the screen are searchable, including the installer
            // one: it is the row a user looks for when the disk is filling up.
            "installers_downloaded",
        ),
        "backup_restore" to listOf("action_backup", "action_restore", "auto_backup", "automatic_backup_desc", "backup_desc", "backup_restore", "backup_restore_desc", "backups_empty", "delete_backup_confirm", "restore_backup_confirm", "restore_desc", "restore_failed", "restore_success", "restore_success_title", "stored_backups"),
        "wrapped_title" to listOf("wrapped_desc", "wrapped_show_on_home", "wrapped_show_on_home_desc", "wrapped_title"),
        "integrations" to listOf("discord_client_id", "discord_presence", "lastfm", "lastfm_session", "discord_presence_desc", "lastfm_enable", "lastfm_now_playing"),
        "desktop_features" to listOf("desktop_features", "desktop_features_desc", "media_keys", "media_keys_desc", "now_playing_widget", "now_playing_widget_desc", "requires_accessibility", "tray_menu", "tray_menu_desc"),
        "system" to listOf("system", "performance_options", "performance_options_desc", "performance_options_enabled", "developer_options", "developer_options_desc", "developer_options_unlocked", "debug_developer_options", "disable_developer_options", "disable_developer_options_desc", "disable_developer_options_confirm", "advanced", "jvm_memory", "jvm_memory_desc", "jvm_memory_default", "jvm_memory_restart", "detailed_playback_logging", "detailed_playback_logging_desc", "detailed_startup_logs", "detailed_startup_logs_desc", "hide_custom_apk_download_button", "hide_custom_apk_download_button_desc", "dev_tools_live_monitor", "dev_tools_mode", "dev_tools_movable", "dev_tools_overlay", "dev_tools_window", "dev_tools_profile", "dev_tools_title_bar", "dev_tools_disabled", "intro", "show_intro_on_startup", "intro_style", "intro_background", "intro_desc", "preview_intro", "dev_open_live_log", "dev_open_live_log_desc", "dev_logs_export", "dev_logs_export_desc", "dev_unlocked_title", "dev_unlocked_desc", "dev_unlocked_open", "tap_version_code_hint"),
        "about" to listOf("about", "version_code", "current_version", "app_developer", "developer_section", "community", "community_section", "license", "github_repository", "telegram_channel", "website", "changelog", "contributors_section", "app_info_section", "installed_date_title"),
    )

    val query = searchQuery.trim()
    val searching = query.isNotEmpty()

    // The page's own header, in the preview's order (`renderHub`): the title and
    // the one icon button on a line of their own, the search field under it on a
    // full-width line, the rule under both. The field used to share the title's
    // line and take the button's place, which made the page reflow as soon as it
    // opened and put the field at half the width the preview gives it.
    val searchFocus = remember { FocusRequester() }
    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp)
    ) {
        Row(
            Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                Localization.get(language, "settings"),
                style = MaterialTheme.typography.headlineMedium,
                modifier = Modifier.weight(1f).padding(top = 12.dp, bottom = 8.dp),
            )
            Spacer(Modifier.width(16.dp))
            // The same button, either way: it opens the field and closes it. The
            // preview never shows a close icon inside the field, and clearing the
            // query with the field is what it does when the button closes it.
            Tooltip(Localization.get(language, if (searchActive) "dismiss" else "search")) {
                M3IconButton(onClick = {
                    searchActive = !searchActive
                    searchQuery = ""
                }) {
                    Icon(
                        if (searchActive) Icons.Filled.Close else Icons.Filled.Search,
                        contentDescription = Localization.get(language, if (searchActive) "dismiss" else "search"),
                    )
                }
            }
        }
        if (searchActive) {
            // The preview's `.md-search`: one full-width line, 56 dp high, the
            // outline rounded to a pill, the magnifier in the leading slot.
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                modifier = Modifier.fillMaxWidth().padding(top = 12.dp).focusRequester(searchFocus),
                singleLine = true,
                shape = CircleShape,
                placeholder = { Text(Localization.get(language, "search")) },
                leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null) },
            )
            LaunchedEffect(Unit) { searchFocus.requestFocus() }
        }
        // 12 dp above the rule and none under it: the first group carries its own
        // 16 dp, the way the preview's `.md-group` margin does.
        HorizontalDivider(Modifier.padding(top = 12.dp))

        // The query matches a row when either its own label or any of the
        // localized labels of the options inside its sub-screen(s) contains it,
        // so every setting is reachable from the settings search bar.
        val rowTermsText = { key: String ->
            (rowSearchTerms[key].orEmpty()).joinToString(" ") { Localization.get(language, it) }
        }
        val matches = { key: String ->
            query.isEmpty() ||
                allRows.any { it.first == key && it.second.contains(query, ignoreCase = true) } ||
                rowTermsText(key).contains(query, ignoreCase = true)
        }

        // Group 1 — account and updates (mobile's first group).
        val generalKeys = listOf("updates", "account", "notifications")
        if (!searching || generalKeys.any { matches(it) }) {
            Spacer(Modifier.height(16.dp))
            M3SettingsGroup(
                title = if (searching) null else Localization.get(language, "general"),
                items = listOfNotNull(
                    generalItems[0].takeIf { !searching || matches("updates") },
                    generalItems[1].takeIf { !searching || matches("account") },
                    generalItems[2].takeIf { !searching || matches("notifications") },
                ),
            )
        }
        // Group 2 — media & player experience (mobile's second group; the
        // lyrics options live in it because that is where mobile keeps them).
        val mediaKeys = listOf("appearance", "lyrics", "player_audio", "device_sync", "content", "language")
        if (!searching || mediaKeys.any { matches(it) }) {
            Spacer(Modifier.height(16.dp))
            M3SettingsGroup(
                title = if (searching) null else Localization.get(language, "appearance"),
                items = listOfNotNull(
                    mediaItems[0].takeIf { !searching || matches("appearance") },
                    mediaItems[1].takeIf { !searching || matches("lyrics") },
                    mediaItems[2].takeIf { !searching || matches("player_audio") },
                    mediaItems[3].takeIf { !searching || matches("device_sync") },
                    mediaItems[4].takeIf { !searching || matches("content") },
                    mediaItems[5].takeIf { !searching || matches("language") },
                ),
            )
        }
        // Group 3 — features & data (mobile's third group).
        val privacyKeys = listOf("ai_lyrics_translation", "privacy", "storage", "data_saver")
        if (!searching || privacyKeys.any { matches(it) }) {
            Spacer(Modifier.height(16.dp))
            M3SettingsGroup(
                title = if (searching) null else Localization.get(language, "privacy"),
                items = listOfNotNull(
                    privacyItems[0].takeIf { !searching || matches("ai_lyrics_translation") },
                    privacyItems[1].takeIf { !searching || matches("privacy") },
                    privacyItems[2].takeIf { !searching || matches("storage") },
                    privacyItems[3].takeIf { !searching || matches("data_saver") },
                ),
            )
        }
        // Group 4, system and support (mobile's fourth group). The keys must be
        // in the SAME order as `aboutItems`: the Spotify import row was inserted
        // in the middle of the list and the mapping below was not extended, so
        // every row from that point on was keyed to the wrong item and the last
        // item (About) was never rendered at all.
        val aboutKeys = listOf("backup_restore", "wrapped_title", "integrations", "spotify_import", "desktop_features", "system", "about")
        if (!searching || aboutKeys.any { matches(it) }) {
            Spacer(Modifier.height(16.dp))
            M3SettingsGroup(
                title = if (searching) null else Localization.get(language, "about"),
                items = listOfNotNull(
                    aboutItems[0].takeIf { !searching || matches("backup_restore") },
                    aboutItems[1].takeIf { !searching || matches("wrapped_title") },
                    aboutItems[2].takeIf { !searching || matches("integrations") },
                    aboutItems[3].takeIf { !searching || matches("spotify_import") },
                    aboutItems[4].takeIf { !searching || matches("desktop_features") },
                    aboutItems[5].takeIf { !searching || matches("system") },
                    aboutItems[6].takeIf { !searching || matches("about") },
                ),
            )
        }
        if (searching && !allRows.any { r -> r.second.contains(query, ignoreCase = true) || rowTermsText(r.first).contains(query, ignoreCase = true) }) {
            Text(
                Localization.get(language, "no_results_found"),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(vertical = 24.dp),
            )
        }
    }
}

@Composable
fun SettingsSystemScreen(
    language: String,
    onBack: () -> Unit,
    showIntroSplash: Boolean,
    onOpenPerformance: () -> Unit,
    onOpenDeveloperOptions: () -> Unit,
    onOpenIntro: () -> Unit,
) {
    val developerUnlocked by DeveloperOptions.unlocked.collectAsState()

    // Log export (moved here from Developer options: it belongs under System).
    var logExporting by remember { mutableStateOf(false) }
    var logExportDone by remember { mutableStateOf(false) }
    var logExportPath by remember { mutableStateOf<String?>(null) }
    val logScope = rememberCoroutineScope()

    SettingsSubScreen(language, onBack) {
        Text(Localization.get(language, "system"), style = MaterialTheme.typography.titleLarge, modifier = Modifier.padding(vertical = 12.dp))
        M3SettingsGroup(
            items = listOf(
                M3SettingsItem(
                    icon = Icons.Filled.Build,
                    title = { Text(Localization.get(language, "performance_options")) },
                    description = { Text(Localization.get(language, "performance_options_desc")) },
                    trailing = { SettingsChevron() },
                    onClick = onOpenPerformance,
                ),
                M3SettingsItem(
                    icon = Icons.Filled.Movie,
                    title = { Text(Localization.get(language, "intro")) },
                    description = {
                        Text(if (showIntroSplash) Localization.get(language, "integrations_active") else Localization.get(language, "integrations_inactive"))
                    },
                    trailing = { SettingsChevron() },
                    onClick = onOpenIntro,
                ),
                M3SettingsItem(
                    icon = Icons.Filled.Info,
                    title = { Text(Localization.get(language, "dev_open_live_log")) },
                    description = { Text(Localization.get(language, "dev_open_live_log_desc")) },
                    trailing = { SettingsChevron() },
                    onClick = { DeveloperOptions.setLogWindowVisible(true) },
                ),
            ),
        )

        // Advanced: Developer options is hidden here until the version code in
        // About is tapped seven times. A section of its own (not a row in the
        // group above) so the unlock is the only way it can appear.
        M3SettingsFold(visible = developerUnlocked) {
            Spacer(Modifier.height(16.dp))
            M3SettingsGroup(
                title = Localization.get(language, "advanced"),
                items = listOf(
                    M3SettingsItem(
                        icon = Icons.Filled.Build,
                        title = { Text(Localization.get(language, "debug_developer_options")) },
                        description = { Text(Localization.get(language, "developer_options_desc")) },
                        trailing = { SettingsChevron() },
                        onClick = onOpenDeveloperOptions,
                    ),
                ),
            )
        }

        Spacer(Modifier.height(16.dp))

        // Export logs (for support requests)
        M3SettingsGroup(
            title = Localization.get(language, "dev_logs_export"),
            items = listOf(
                M3SettingsItem(
                    icon = Icons.Filled.Download,
                    title = { Text(Localization.get(language, "dev_logs_export_desc")) },
                    trailing = {
                        Button(
                            onClick = {
                                logExporting = true
                                logExportDone = false
                                logExportPath = null
                                logScope.launch {
                                    val target = withContext(Dispatchers.IO) {
                                        val dialog = java.awt.FileDialog(
                                            null as java.awt.Frame?,
                                            Localization.get(language, "dev_logs_export"),
                                            java.awt.FileDialog.SAVE,
                                        )
                                        dialog.file = LogExporter.defaultFileName()
                                        dialog.isVisible = true
                                        val dir = dialog.directory
                                        val name = dialog.file
                                        dialog.dispose()
                                        if (dir != null && name != null) java.io.File(dir, name) else null
                                    }
                                    if (target != null) {
                                        val ok = withContext(Dispatchers.IO) {
                                            runCatching { LogExporter.export(target); true }.getOrDefault(false)
                                        }
                                        if (ok) {
                                            logExportDone = true
                                            logExportPath = target.absolutePath
                                        }
                                    }
                                    logExporting = false
                                }
                            },
                            enabled = !logExporting,
                        ) {
                            Text(
                                if (logExporting) Localization.get(language, "dev_logs_exporting")
                                else Localization.get(language, "dev_logs_export")
                            )
                        }
                    },
                    onClick = {},
                ),
            ),
        )
        logExportPath?.let { path ->
            Text(
                if (logExportDone) "${Localization.get(language, "dev_logs_exported")}: $path"
                else Localization.get(language, "dev_logs_export_failed"),
                style = MaterialTheme.typography.bodySmall,
                color = if (logExportDone) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error,
                modifier = Modifier.padding(start = 16.dp, top = 6.dp),
            )
        }
    }
}

/**
 * Settings -> Language: the current language, and a menu over the other 48.
 *
 * The menu carries a search box: forty-eight rows is not a list to scroll with a
 * wheel, and "Magyar" is not something to find by reading from the top. The
 * box matches the language's own name and its code, ignoring case and accents
 * ([Languages.matching]), so "ital", "cestina" and "de" all land where they
 * should. It is the same field the Browse screen already uses; a language the
 * list does not contain is a query that matches nothing, and that says so
 * instead of drawing an empty menu.
 */
@Composable
fun LanguageSection(language: String, onLanguageChange: (String) -> Unit) {
    Text(Localization.get(language, "language"), style = MaterialTheme.typography.titleLarge, modifier = Modifier.padding(top = 16.dp))
    Text(
        Localization.get(language, "translation_ai_disclaimer"),
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.padding(top = 6.dp),
    )

    // The list is on the screen, not behind a button: 48 languages read as the
    // page's own content in the preview, and a menu that had to be opened first
    // hid the field that narrows it. The field and the rows are
    // [LanguagePickerRows], the same body `:desktop:languageSearchCheck` reads.
    Spacer(Modifier.height(10.dp))
    LanguagePickerRows(
        language = language,
        selected = language,
        onSelect = onLanguageChange,
    )
}

/**
 * The language list: the search field, then the languages that match what is in
 * it, the current one carrying a check.
 *
 * It is a composable of its own because `:desktop:languageSearchCheck` renders
 * it off screen and reads it back, which is what says the field is drawn and the
 * list really narrows.
 */
@Composable
internal fun LanguagePickerRows(
    language: String,
    selected: String,
    onSelect: (String) -> Unit,
    initialQuery: String = "",
) {
    var query by remember { mutableStateOf(initialQuery) }
    val matches = Languages.matching(query)

    OutlinedTextField(
        value = query,
        onValueChange = { query = it },
        modifier = Modifier.fillMaxWidth(),
        singleLine = true,
        textStyle = MaterialTheme.typography.bodyMedium,
        placeholder = {
            Text(
                Localization.get(language, "language_search"),
                style = MaterialTheme.typography.bodyMedium,
            )
        },
        leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null, modifier = Modifier.size(18.dp)) },
    )

    Spacer(Modifier.height(16.dp))
    if (matches.isEmpty()) {
        Text(
            Localization.get(language, "language_search_none"),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(horizontal = 6.dp, vertical = 14.dp),
        )
    } else {
        M3SettingsGroup(
            items = matches.map { lang ->
                M3SettingsItem(
                    icon = null,
                    title = { Text(lang.name) },
                    trailing = if (lang.code == selected) {
                        {
                            Icon(
                                Icons.Filled.Check,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(20.dp),
                            )
                        }
                    } else {
                        null
                    },
                    onClick = { onSelect(lang.code) },
                )
            },
        )
    }
}

