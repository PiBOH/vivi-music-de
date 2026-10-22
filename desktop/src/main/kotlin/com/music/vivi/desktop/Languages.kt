package com.music.vivi.desktop

import java.text.Normalizer

/** A selectable language: locale tag + native display name. */
data class AppLanguage(val code: String, val name: String)

/**
 * Languages supported by the desktop edition, mirroring the Android app's
 * `LanguageCodeToName` map. English is the primary (source) language.
 */
object Languages {

    /** Combining marks, i.e. what NFD turns an accent into. */
    private val DIACRITICS = Regex("\\p{Mn}+")

    /**
     * The letters that are letters of their own and not a marked vowel, so NFD
     * leaves them alone: the Azerbaijani schwa is the one this list needs
     * ("azerbaycan" must find Azərbaycan dili), and the rest are here because a
     * keyboard without them should still land on the language.
     */
    private val LETTERS = mapOf(
        'ə' to "e", 'ı' to "i", 'ø' to "o", 'ł' to "l", 'đ' to "d",
        'ß' to "ss", 'æ' to "ae", 'þ' to "th", 'ð' to "d",
    )

    /**
     * [text] without its accents and in lower case, so "Čeština" and "cestina"
     * are the same word to a search box and "Azərbaycan" is found by "azerbaycan".
     */
    internal fun fold(text: String): String {
        val plain = StringBuilder(text.length)
        for (ch in text.lowercase()) {
            val folded = LETTERS[ch]
            if (folded != null) plain.append(folded) else plain.append(ch)
        }
        return Normalizer.normalize(plain, Normalizer.Form.NFD).replace(DIACRITICS, "")
    }

    /**
     * The languages whose name or code contains [query], ignoring case and
     * diacritics; every language for a blank query.
     */
    fun matching(query: String): List<AppLanguage> {
        val needle = fold(query.trim())
        if (needle.isEmpty()) return all
        return all.filter { fold(it.name).contains(needle) || it.code.lowercase().contains(needle) }
    }

    val all: List<AppLanguage> = listOf(
        AppLanguage("en", "English"),
        AppLanguage("az", "Azərbaycan dili"),
        AppLanguage("bs", "Bosanski"),
        AppLanguage("ca", "Català"),
        AppLanguage("cs", "Čeština"),
        AppLanguage("de", "Deutsch"),
        AppLanguage("et", "Eesti"),
        AppLanguage("es", "Español"),
        AppLanguage("eu", "Euskara"),
        AppLanguage("fil", "Filipino"),
        AppLanguage("fr", "Français"),
        AppLanguage("hr", "Hrvatski"),
        AppLanguage("id", "Bahasa Indonesia"),
        AppLanguage("it", "Italiano"),
        AppLanguage("lt", "Lietuvių"),
        AppLanguage("hu", "Magyar"),
        AppLanguage("ms", "Bahasa Melayu"),
        AppLanguage("nl", "Nederlands"),
        AppLanguage("nb", "Norsk bokmål"),
        AppLanguage("pl", "Polski"),
        AppLanguage("pt", "Português"),
        AppLanguage("ro", "Română"),
        AppLanguage("sk", "Slovenčina"),
        AppLanguage("sl", "Slovenščina"),
        AppLanguage("sr", "Српски"),
        AppLanguage("fi", "Suomi"),
        AppLanguage("sv", "Svenska"),
        AppLanguage("vi", "Tiếng Việt"),
        AppLanguage("tr", "Türkçe"),
        AppLanguage("el", "Ελληνικά"),
        AppLanguage("be", "Беларуская"),
        AppLanguage("bg", "Български"),
        AppLanguage("ru", "Русский"),
        AppLanguage("uk", "Українська"),
        AppLanguage("ar", "العربية"),
        AppLanguage("hi", "हिन्दी"),
        AppLanguage("as", "অসমীয়া"),
        AppLanguage("bn", "বাংলা"),
        AppLanguage("pa", "ਪੰਜਾਬੀ"),
        AppLanguage("ta", "தமிழ்"),
        AppLanguage("te", "తెలుగు"),
        AppLanguage("ml", "മലയാളം"),
        AppLanguage("th", "ไทย"),
        AppLanguage("km", "ខ្មែរ"),
        AppLanguage("ko", "한국어"),
        AppLanguage("zh-rCN", "简体中文"),
        AppLanguage("zh-rTW", "繁體中文"),
        AppLanguage("ja", "日本語"),
    )

    fun name(code: String): String = all.firstOrNull { it.code == code }?.name ?: code

    /**
     * Maps an Android locale code (as stored by the mobile app's DataStore,
     * e.g. "no", "pt-PT", "zh-CN") to the equivalent desktop code ("nb", "pt",
     * "zh-rCN"). The two apps use slightly different locale tags for the same
     * languages, which otherwise breaks settings sync (a language changed on
     * one device would be silently ignored on the other).
     */
    fun fromMobileCode(code: String): String = when (code) {
        "no" -> "nb"
        "pt-PT", "pt-BR" -> "pt"
        "zh-CN", "zh-Hans" -> "zh-rCN"
        "zh-TW", "zh-HK", "zh-Hant" -> "zh-rTW"
        "en-GB", "en-US" -> "en"
        "es-419" -> "es"
        "fr-CA" -> "fr"
        else -> code
    }

    /** Inverse of [fromMobileCode]: desktop code -> Android locale code. */
    fun toMobileCode(code: String): String = when (code) {
        "nb" -> "no"
        "zh-rCN" -> "zh-CN"
        "zh-rTW" -> "zh-TW"
        else -> code
    }

    /** A `java.util.Locale` for a desktop language code ("zh-rCN" -> `zh-CN`). */
    fun jvmLocale(code: String): java.util.Locale =
        java.util.Locale.forLanguageTag(code.replace("-r", "-"))

    /**
     * Makes the platform locale follow the app language.
     *
     * The shared Compose components localise themselves from
     * `java.util.Locale.getDefault()` through `PlatformLocalization` — the
     * text-selection context menu (Copy / Cut / Paste / Select all) and every
     * other built-in string — never from the app's own string table. On an
     * Italian machine an English build therefore showed "Copia" in the
     * right-click menu. Pointing the default locale at the selected language
     * makes those strings match the rest of the UI (and the date formatting
     * with them); languages Compose has no translation for fall back to English.
     */
    fun applyJvmLocale(code: String) {
        if (code.isBlank()) return
        runCatching { java.util.Locale.setDefault(jvmLocale(code)) }
    }
}
