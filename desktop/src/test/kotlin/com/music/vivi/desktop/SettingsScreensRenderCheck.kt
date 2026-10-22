@file:OptIn(ExperimentalComposeUiApi::class)

package com.music.vivi.desktop

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
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
 *  * **Notifications**: five bands - the mode, the three rows of the card it
 *    belongs to, the duration - and the test button under them, with the two
 *    seams of the three-row card.
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

        // The seam is the 3 dp the next row's own top padding takes; anything
        // wider is the gap the screen leaves between two cards.
        fun seams(bands: List<Band>) = bands.zipWithNext().count { (a, b) -> b.top - a.bottom - 1 in 1..4 }

        val updateSeams = seams(updateBands)
        val notificationSeams = seams(notificationBands)

        // Updates: 2 rows, two cards of one row, 2 rows.
        if (updateBands.size != 6) failures += "Updates should read six bands, read ${updateBands.size}"
        if (updateSeams != 2) failures += "Updates should join its two two-row cards (2 seams), read $updateSeams"

        // Notifications: the three rows of one card, joined by its two seams. The
        // two dropdown cards and the button sit closer to the page than the
        // 6-unit threshold this scan uses, so they are not bands; the card is
        // what the seams are for.
        if (notificationBands.size != 3) failures += "Notifications should read the three rows of its card, read ${notificationBands.size}"
        if (notificationSeams != 2) failures += "Notifications should join its three rows (2 seams), read $notificationSeams"

        if (failures.isEmpty()) {
            println()
            println(
                "OK: Updates is six bands with the two seams of its two-row cards, " +
                    "and Notifications is one card of three rows joined by two seams",
            )
            return
        }
        println()
        println("FAILED:")
        failures.forEach { println("  - $it") }
        throw IllegalStateException("settings screens render check failed (${failures.size} case(s))")
    }

    /** One frame of [content], in the light scheme, on the app's own surface. */
    private fun render(content: @Composable () -> Unit): ByteArray {
        val scene = ImageComposeScene(WIDTH, HEIGHT) {
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
     * differ from the page, and the 3 dp seam and the 8 dp gap between cards are
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
        // gaps, they are glyphs, so runs shorter than 6 px are folded into their
        // neighbour instead of counting as a band.
        return runs.filter { it.height >= 6 }
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
