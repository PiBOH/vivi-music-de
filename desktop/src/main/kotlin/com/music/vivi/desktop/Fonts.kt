package com.music.vivi.desktop

import androidx.compose.runtime.mutableStateListOf
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.platform.Font
import kotlinx.serialization.Serializable
import java.io.File

/**
 * Selectable app font (mirrors the Android app's `AppFont` enum). The same
 * `.ttf` variable fonts are bundled in `desktop/src/main/resources/fonts/`.
 */
enum class AppFont(val value: String) {
    SYSTEM("system"),
    GOOGLE_SANS("google_sans"),
    SANS_FLEX("sans_flex"),
    OUTFIT("outfit"),
    PLUS_JAKARTA_SANS("plus_jakarta_sans"),
    CUSTOM("custom");

    companion object {
        fun fromValue(value: String): AppFont = entries.find { it.value == value } ?: SYSTEM
    }
}

/** Loads a bundled `.ttf` (classpath resource) as a 3-weight [FontFamily]. */
private fun fontFamily(resource: String): FontFamily = FontFamily(
    Font(resource, FontWeight.Normal),
    Font(resource, FontWeight.Medium),
    Font(resource, FontWeight.Bold),
)

/** Loads a `.ttf`/`.otf` [File] (user-imported) as a 3-weight [FontFamily]. */
private fun fileFontFamily(file: File): FontFamily? {
    if (!file.exists()) return null
    return FontFamily(
        Font(file, FontWeight.Normal),
        Font(file, FontWeight.Medium),
        Font(file, FontWeight.Bold),
    )
}

/** The bundled app fonts (lazy so the app starts fast and only loads when used). */
object AppFonts {
    val googleSans: FontFamily by lazy { fontFamily("fonts/google_sans_flex.ttf") }
    val sansFlex: FontFamily by lazy { fontFamily("fonts/sans_flex.ttf") }
    val outfit: FontFamily by lazy { fontFamily("fonts/outfit.ttf") }
    val plusJakartaSans: FontFamily by lazy { fontFamily("fonts/plus_jakarta_sans.ttf") }

    /** Runtime-imported custom font path (copied into `~/.vivimusic/fonts/`). */
    @Volatile var customFontPath: String? = null

    /** Custom-font bundle with a file fallback to plain text for robustness. */
    fun customFamily(path: String? = null): FontFamily {
        val p = path ?: customFontPath
        if (!p.isNullOrBlank()) {
            fileFontFamily(File(p))?.let { return it }
        }
        return FontFamily.Default
    }

    fun familyFor(font: AppFont, customPath: String? = null): FontFamily = when (font) {
        AppFont.SYSTEM -> FontFamily.Default
        AppFont.GOOGLE_SANS -> googleSans
        AppFont.SANS_FLEX -> sansFlex
        AppFont.OUTFIT -> outfit
        AppFont.PLUS_JAKARTA_SANS -> plusJakartaSans
        AppFont.CUSTOM -> customFamily(customPath)
    }
}

/**
 * One face the user imported: the file copied into `~/.vivimusic/fonts/` and the
 * name its row shows. The name starts as the face's own, read out of the file,
 * and a rename changes only that string - never the file and never the choice.
 */
@Serializable
data class CustomFont(
    val id: Int,
    val path: String,
    val name: String,
)

/**
 * The name a font file carries for itself: its `name` table's typographic family
 * (nameID 16) and, without one, its family (nameID 1), preferring the Windows
 * English record, which is the one stored as UTF-16BE. A file whose table cannot
 * be read (a WOFF, a `.ttc` holding several faces) answers null, and its row is
 * named after the file instead until it is renamed.
 */
fun faceNameOf(file: File): String? {
    val bytes = runCatching { file.readBytes() }.getOrNull() ?: return null
    if (bytes.size < 12) return null
    fun u8(i: Int) = bytes[i].toInt() and 0xFF
    fun u16(i: Int) = (u8(i) shl 8) or u8(i + 1)
    fun u32(i: Int) = (u16(i).toLong() shl 16) or u16(i + 2).toLong()

    val tables = u16(4)
    var nameTable = -1
    for (index in 0 until tables) {
        val record = 12 + index * 16
        if (record + 16 > bytes.size) break
        if (String(bytes, record, 4, Charsets.US_ASCII) == "name") {
            nameTable = u32(record + 8).toInt()
            break
        }
    }
    if (nameTable < 0 || nameTable + 6 > bytes.size) return null

    val count = u16(nameTable + 2)
    val strings = nameTable + u16(nameTable + 4)
    var best: Pair<Int, String>? = null
    for (index in 0 until count) {
        val record = nameTable + 6 + index * 12
        if (record + 12 > bytes.size) break
        val platform = u16(record)
        val encoding = u16(record + 2)
        val language = u16(record + 4)
        val nameId = u16(record + 6)
        if (nameId != 16 && nameId != 1) continue
        val length = u16(record + 8)
        val start = strings + u16(record + 10)
        if (length <= 0 || start + length > bytes.size) continue
        val value = runCatching {
            if (platform == 3 || platform == 0) {
                String(bytes, start, length, Charsets.UTF_16BE)
            } else {
                String(bytes, start, length, Charsets.ISO_8859_1)
            }
        }.getOrNull()?.trim().orEmpty()
        if (value.isEmpty()) continue
        // The typographic family first, then the family; inside each, the record a
        // Windows application reads (platform 3, UTF-16BE, English) and then any.
        val rank = (if (nameId == 16) 0 else 10) +
            (if (platform == 3 && encoding == 1 && language == 0x409) 0 else 1)
        if (best == null || rank < best.first) best = rank to value
    }
    return best?.second
}

/** The file's own name, for a face whose name table cannot be read. */
fun fileNameOf(path: String): String =
    path.substringAfterLast('\\').substringAfterLast('/').substringBeforeLast('.')

/**
 * The imported faces, in the order they were added.
 *
 * The app kept one imported face (`customFontPath`) named after the file it came
 * from, so a second import replaced the first and every row read *Custom font*.
 * The faces are a list now, in the settings store, each named after the face it
 * draws until the user renames it; a row's file is the app's own copy, so a
 * rename never touches a font the system owns.
 *
 * The choice is still `selectedFont` + `customFontPath`: picking a face from this
 * list writes both, so the theme, the lyrics and the settings mirror read it
 * exactly as they did.
 */
object CustomFonts {

    /** The imported faces, observable: a row added, renamed or dropped redraws. */
    val faces = mutableStateListOf<CustomFont>()

    /** Why the last [add] failed, for the notice the caller shows. */
    var lastError: String? = null
        private set

    /**
     * Reads the stored list once at startup, and turns the single path an older
     * store carries into the first row, named after the face it holds.
     */
    fun load(stored: List<CustomFont>, legacyPath: String) {
        faces.clear()
        faces += stored
        if (faces.isEmpty() && legacyPath.isNotBlank()) {
            val name = faceNameOf(File(legacyPath)) ?: fileNameOf(legacyPath)
            faces += CustomFont(1, legacyPath, name)
        }
    }

    /** The row `path` names, when it is one of the imported faces. */
    fun byPath(path: String): CustomFont? = faces.firstOrNull { it.path == path }

    /**
     * Copies [src] into the app's own font folder under a name of its own, reads
     * the face's name out of the file and adds the row. Null when the copy fails,
     * which the caller reports: a row with no file behind it names nothing.
     */
    fun add(src: File): CustomFont? {
        val dir = File(System.getProperty("user.home"), ".vivimusic/fonts").apply { mkdirs() }
        val id = (faces.maxOfOrNull { it.id } ?: 0) + 1
        val ext = src.extension.ifBlank { "ttf" }.lowercase()
        val dest = File(dir, "custom_font_$id.$ext")
        val failure = runCatching {
            java.nio.file.Files.copy(
                src.toPath(),
                dest.toPath(),
                java.nio.file.StandardCopyOption.REPLACE_EXISTING,
            )
        }.exceptionOrNull()
        if (failure != null || !dest.exists()) {
            val why = failure?.let { "${it.javaClass.simpleName}: ${it.message}" }
                ?: "the copy did not produce a file"
            AppLog.log("app", "font import failed for '${src.absolutePath}': $why")
            lastError = why
            return null
        }
        lastError = null
        val face = CustomFont(id, dest.absolutePath, faceNameOf(dest) ?: src.nameWithoutExtension)
        faces += face
        save()
        AppLog.log("app", "font imported: '${src.name}' as '${face.name}' -> '${dest.absolutePath}'")
        return face
    }

    /** Renames a row. The file stays where it is; only the name it shows changes. */
    fun rename(id: Int, name: String) {
        val clean = name.trim()
        val index = faces.indexOfFirst { it.id == id }
        if (clean.isEmpty() || index < 0) return
        faces[index] = faces[index].copy(name = clean)
        save()
    }

    /** Drops a row and deletes the app's own copy of its file. */
    fun remove(id: Int): CustomFont? {
        val index = faces.indexOfFirst { it.id == id }
        if (index < 0) return null
        val face = faces.removeAt(index)
        runCatching { File(face.path).delete() }
        save()
        return face
    }

    private fun save() {
        DesktopSettings.update { it.copy(customFonts = faces.toList()) }
    }
}