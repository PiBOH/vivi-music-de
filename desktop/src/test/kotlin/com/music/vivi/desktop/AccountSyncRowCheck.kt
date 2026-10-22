@file:OptIn(ExperimentalComposeUiApi::class)

package com.music.vivi.desktop

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Text
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.ImageComposeScene
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import org.jetbrains.skia.EncodedImageFormat
import java.io.File

/**
 * Headless check of the account screen's sync row, run with
 *
 *     ./gradlew :desktop:accountSyncRowCheck
 *
 * The row is the one control that runs the playlist sync, and it answered "how
 * far along is it?" with an 18 dp ring, which says only that something is
 * happening. A run now says `3/12` in the trailing slot (`SyncRunTrailing`), on
 * the right edge of the row, and the ring is left for the moment the run's total
 * is still unknown.
 *
 * Whether a number really sits on the right, and whether it is the run's own
 * number, cannot be read off the source: no display is attached here, so the row
 * is rendered into an off-screen `ImageComposeScene` in four states and the four
 * images are compared. The description is the same in all four, so the trailing
 * slot is the only thing that may differ:
 *
 *  * `3/12` against no run: the image changes, and every changed pixel is right
 *    of the trailing zone's left edge and none of it is at that edge - a counter
 *    that had been drawn after the title would fail here;
 *  * `7/8` against `3/12`: a different image, so the slot prints the run's own
 *    numbers instead of a fixed place holder;
 *  * no total yet against `3/12`: a different image, which is the ring standing
 *    in for the counter.
 *
 * PNGs are written under `.ignore/accountsrow/` so the four states can be looked
 * at.
 */
object AccountSyncRowCheck {

    private val outDir = File(".ignore/accountsrow")

    private const val WIDTH = 640
    private const val HEIGHT = 160

    /** Left edge of the trailing zone: the counter must live to the right of it. */
    private const val ZONE_FROM = 470

    @JvmStatic
    fun main(args: Array<String>) {
        outDir.mkdirs()
        val failures = mutableListOf<String>()

        val idle = render(status = null, name = "idle")
        val running = render(status = status(done = 3, total = 12), name = "running-3-12")
        val other = render(status = status(done = 7, total = 8), name = "running-7-8")
        val unknown = render(status = status(done = 0, total = 0), name = "running-no-total")

        val appeared = changes(running, idle)
        val leftmost = appeared.minOfOrNull { it.first } ?: -1
        println("no run against 3/12: ${appeared.size} px differ, leftmost x=$leftmost (zone starts at $ZONE_FROM)")
        if (appeared.isEmpty()) {
            failures += "a run draws nothing in the trailing slot: no pixel changed against no run"
        }
        if (leftmost in 0 until ZONE_FROM) {
            failures += "the run changed the row at x=$leftmost, left of the trailing zone ($ZONE_FROM)"
        }
        if (leftmost in 0 until ZONE_FROM + 40) {
            failures += "the counter starts at x=$leftmost, at the left of its zone instead of at the row's right edge"
        }

        val numbers = changes(other, running)
        println("3/12 against 7/8: ${numbers.size} px differ")
        if (numbers.isEmpty()) {
            failures += "changing the run's numbers changed nothing: the slot is not printing them"
        }

        val ring = changes(unknown, running)
        println("3/12 against no total: ${ring.size} px differ")
        if (ring.isEmpty()) {
            failures += "a run with no total draws the same thing as 3/12: the ring cannot stand in"
        }

        if (failures.isEmpty()) {
            println()
            println("OK: the counter sits on the row's right edge during a run, follows its numbers, and the ring stands in while the total is unknown")
            return
        }
        println()
        println("FAILED:")
        failures.forEach { println("  - $it") }
        throw IllegalStateException("account sync row check failed (${failures.size} case(s))")
    }

    private fun status(done: Int, total: Int) = PlaylistSync.Status(
        phase = PlaylistSync.Phase.RUNNING,
        done = done,
        total = total,
        message = "check",
    )

    private fun render(status: PlaylistSync.Status?, name: String): ByteArray {
        val scene = ImageComposeScene(WIDTH, HEIGHT) {
            AppTheme(mode = ThemeMode.LIGHT, accent = Color(0xFFED5564), language = "en") {
                Column(Modifier.fillMaxWidth().padding(24.dp)) {
                    M3SettingsGroup(
                        items = listOf(
                            M3SettingsItem(
                                icon = Icons.Filled.Refresh,
                                title = { Text("Sync now") },
                                // The same in every state, so only the trailing
                                // slot can tell the four renders apart.
                                description = { Text("Syncing your playlists…") },
                                trailing = if (status != null) {
                                    { SyncRunTrailing(status) }
                                } else {
                                    null
                                },
                            ),
                        ),
                    )
                }
            }
        }
        val png = try {
            scene.render().encodeToData(EncodedImageFormat.PNG, 100)!!.bytes
        } finally {
            scene.close()
        }
        File(outDir, "$name.png").writeBytes(png)
        return png
    }

    /** The pixels two renders disagree on, as (x, y). */
    private fun changes(a: ByteArray, b: ByteArray): List<Pair<Int, Int>> {
        val first = javax.imageio.ImageIO.read(a.inputStream())
        val second = javax.imageio.ImageIO.read(b.inputStream())
        val out = mutableListOf<Pair<Int, Int>>()
        for (x in 0 until first.width) for (y in 0 until first.height) {
            if (first.getRGB(x, y) != second.getRGB(x, y)) out += (x to y)
        }
        return out
    }
}
