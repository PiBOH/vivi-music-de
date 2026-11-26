package com.music.vivi.desktop.dev

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.snapshots.Snapshot
import androidx.compose.ui.ImageComposeScene
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import com.music.vivi.desktop.data.library.LibraryHealthChecker
import com.music.vivi.desktop.data.listentogether.ListenTogetherManager
import com.music.vivi.desktop.data.youtube.YouTubeAccount
import com.music.vivi.desktop.data.youtube.YouTubeMusicRepository
import com.music.vivi.desktop.data.youtube.YouTubePlaylistsRepository
import com.music.vivi.desktop.di.appModules
import com.music.vivi.desktop.domain.model.AppReleaseInfo
import com.music.vivi.desktop.domain.model.UpdateDownloadState
import com.music.vivi.desktop.presentation.components.AppUpdateDialog
import com.music.vivi.desktop.presentation.screens.ListenTogetherScreen
import com.music.vivi.desktop.presentation.screens.settings.SettingsCategoryDetail
import com.music.vivi.desktop.presentation.screens.settings.SettingsSection
import com.music.vivi.desktop.presentation.screens.settings.AutoBackupCard
import com.music.vivi.desktop.presentation.screens.settings.DiagnosticsCard
import com.music.vivi.desktop.presentation.screens.settings.LibraryHealthCard
import com.music.vivi.desktop.presentation.screens.settings.SyncStatusCard
import com.music.vivi.desktop.presentation.screens.settings.YouTubePlaylistsCard
import com.music.vivi.desktop.presentation.viewmodel.PlayerViewModel
import com.music.vivi.desktop.ui.theme.PixelPlayerTheme
import java.io.File
import kotlin.system.exitProcess
import kotlinx.coroutines.Dispatchers
import org.koin.core.context.GlobalContext
import org.koin.core.context.startKoin

/**
 * Renders the new settings cards and the update dialog without a window.
 *
 * The settings cards are composed with the app's own Koin graph, so the account
 * playlist card talks to the signed-in session and the others read the real
 * settings and store paths. Run it with [VIVI_SYNC_STATE_FILE] and
 * [VIVI_SETTINGS_FILE] pointing into `build/` when the render must not start a
 * playback restore. The PNGs land in `build/cards/`, or in `VIVI_CARDS_OUTPUT`
 * when it is set.
 */
object CardsRenderCheck {

    const val FLAG = "--cards-render-check"

    fun handle(args: Array<String>): Boolean {
        if (!args.contains(FLAG)) return false
        val failed = run()
        exitProcess(if (failed) 1 else 0)
    }

    private fun run(): Boolean {
        println("[cards] rendering the new cards")
        YouTubeAccount.restore()
        val koin = GlobalContext.getOrNull()?.get() ?: startKoin { modules(appModules) }.koin
        val player = koin.get<PlayerViewModel>()
        val playlists = koin.get<YouTubePlaylistsRepository>()
        val music = koin.get<YouTubeMusicRepository>()
        val health = koin.get<LibraryHealthChecker>()
        val listenTogether = koin.get<ListenTogetherManager>()

        var failed = false
        fun card(
            name: String,
            width: Int,
            height: Int,
            settleMs: Long = 900,
            content: @Composable () -> Unit
        ) {
            val rendered = render(name, width, height, settleMs) {
                PixelPlayerTheme(darkTheme = true) {
                    Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .verticalScroll(rememberScrollState())
                                .padding(24.dp),
                            verticalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            content()
                        }
                    }
                }
            }
            if (!rendered) failed = true
        }

        card("10-playlists.png", 900, 760, settleMs = 14_000) {
            YouTubePlaylistsCard(
                repository = playlists,
                musicRepository = music,
                playerViewModel = player
            )
        }
        card("11-autobackup.png", 760, 430) { AutoBackupCard() }
        card("12-sync.png", 760, 380) { SyncStatusCard() }
        card("13-diagnostics.png", 760, 320) { DiagnosticsCard() }
        card("14-health.png", 760, 320) { LibraryHealthCard(checker = health) }
        if (!render("19-settings-playback.png", 1000, 3200, settleMs = 3000) {
                PixelPlayerTheme(darkTheme = true) {
                    Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
                        SettingsCategoryDetail(
                            section = SettingsSection.PLAYBACK,
                            viewModel = player,
                            isDarkTheme = true,
                            isDynamicColor = false,
                            onToggleDarkTheme = {},
                            onToggleDynamicColor = {},
                            onRescanLibrary = {},
                            onOpenFoldersDetail = {},
                            onOpenArtistsDetail = {},
                            onOpenDuplicatesDetail = {},
                            onOpenPaletteStyleDetail = {},
                            onBack = {}
                        )
                    }
                }
            }
        ) {
            failed = true
        }

        if (!render("21-settings-library.png", 1000, 3200, settleMs = 2500) {
                PixelPlayerTheme(darkTheme = true) {
                    Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
                        SettingsCategoryDetail(
                            section = SettingsSection.LIBRARY,
                            viewModel = player,
                            isDarkTheme = true,
                            isDynamicColor = false,
                            onToggleDarkTheme = {},
                            onToggleDynamicColor = {},
                            onRescanLibrary = {},
                            onOpenFoldersDetail = {},
                            onOpenArtistsDetail = {},
                            onOpenDuplicatesDetail = {},
                            onOpenPaletteStyleDetail = {},
                            onBack = {}
                        )
                    }
                }
            }
        ) {
            failed = true
        }

        if (!render("18-listen-together.png", 1220, 780, settleMs = 2500) {
                PixelPlayerTheme(darkTheme = true) {
                    Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
                        ListenTogetherScreen(viewModel = player, manager = listenTogether)
                    }
                }
            }
        ) {
            failed = true
        }

        val release = AppReleaseInfo(
            versionTag = "6.0.8.11_DE-1.57.0-beta1",
            versionName = "Vivi Music 6.0.8.11_DE 1.57.0",
            releaseNotes = "The account playlists are managed from Accounts. " +
                "A weekly backup runs on its own. The updater checks the SHA-256 " +
                "before it offers the installer.",
            htmlUrl = "https://github.com/PiBOH/axolotl-music/releases",
            publishedAt = "2026-11-21T12:00:00Z",
            isPrerelease = true,
            assets = emptyList()
        )
        card("15-update-idle.png", 700, 820, settleMs = 500) {
            AppUpdateDialog(
                release = release,
                onDismiss = {},
                onOpenRelease = {},
                downloadState = UpdateDownloadState.Idle,
                onDownload = {},
                onInstall = {}
            )
        }
        card("16-update-ready.png", 700, 820, settleMs = 500) {
            AppUpdateDialog(
                release = release,
                onDismiss = {},
                onOpenRelease = {},
                downloadState = UpdateDownloadState.Ready(
                    file = File("VIVIMusic-6.0.8.11_DE-1.57.0-setup.exe"),
                    sha256 = "0123456789abcdef0123456789abcdef0123456789abcdef0123456789abcdef"
                ),
                onDownload = {},
                onInstall = {}
            )
        }
        card("17-update-failed.png", 700, 820, settleMs = 500) {
            AppUpdateDialog(
                release = release,
                onDismiss = {},
                onOpenRelease = {},
                downloadState = UpdateDownloadState.Failed(
                    "The release does not publish a SHA-256 for this installer, so the download was not started."
                ),
                onDownload = {},
                onInstall = {}
            )
        }

        println("[cards] RESULT: ${if (failed) "FAIL" else "PASS"}")
        return failed
    }

    private fun render(
        name: String,
        width: Int,
        height: Int,
        settleMs: Long,
        content: @Composable () -> Unit
    ): Boolean {
        val scene = ImageComposeScene(
            width = width,
            height = height,
            density = Density(1f),
            coroutineContext = Dispatchers.Main
        ) { content() }
        return try {
            val deadline = System.currentTimeMillis() + settleMs
            while (System.currentTimeMillis() < deadline) {
                scene.render()
                Snapshot.sendApplyNotifications()
                Thread.sleep(200)
            }
            scene.render()
            Snapshot.sendApplyNotifications()
            val bytes = scene.render().encodeToData()?.bytes ?: run {
                println("[cards] $name: the scene produced no pixels")
                return false
            }
            val out = File(System.getenv("VIVI_CARDS_OUTPUT") ?: "build/cards", name)
            out.parentFile?.mkdirs()
            out.writeBytes(bytes)
            println("[cards] $name (${bytes.size} bytes)")
            true
        } catch (t: Throwable) {
            println("[cards] $name FAILED: ${t::class.simpleName}: ${t.message}")
            false
        } finally {
            scene.close()
        }
    }
}
