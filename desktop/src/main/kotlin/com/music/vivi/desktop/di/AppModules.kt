package com.music.vivi.desktop.di

import com.music.vivi.desktop.data.database.FavoritesDao
import com.music.vivi.desktop.data.database.ListenBrainzDao
import com.music.vivi.desktop.data.database.LocalPlaylistDao
import com.music.vivi.desktop.data.database.LyricsDao
import com.music.vivi.desktop.data.database.MusicDao
import com.music.vivi.desktop.data.library.LibraryHealthChecker
import com.music.vivi.desktop.data.store.LocalLibraryStore
import com.music.vivi.desktop.data.backup.AutoBackupScheduler
import com.music.vivi.desktop.data.backup.BackupManager
import com.music.vivi.desktop.data.backup.format.BackupFormatDetector
import com.music.vivi.desktop.data.backup.format.BackupReader
import com.music.vivi.desktop.data.backup.format.BackupWriter
import com.music.vivi.desktop.data.backup.model.BackupSection
import com.music.vivi.desktop.data.backup.module.ArtistImagesModuleHandler
import com.music.vivi.desktop.data.backup.module.BackupModuleHandler
import com.music.vivi.desktop.data.backup.module.BackupSongResolver
import com.music.vivi.desktop.data.backup.module.EngagementStatsModuleHandler
import com.music.vivi.desktop.data.backup.module.EqualizerModuleHandler
import com.music.vivi.desktop.data.backup.module.FavoritesModuleHandler
import com.music.vivi.desktop.data.backup.module.GlobalSettingsModuleHandler
import com.music.vivi.desktop.data.backup.module.LyricsModuleHandler
import com.music.vivi.desktop.data.backup.module.PlaybackHistoryModuleHandler
import com.music.vivi.desktop.data.backup.module.PlaylistsModuleHandler
import com.music.vivi.desktop.data.backup.restore.RestoreExecutor
import com.music.vivi.desktop.data.backup.restore.RestorePlanner
import com.music.vivi.desktop.data.backup.validation.BackupFileValidator
import com.music.vivi.desktop.data.backup.validation.ManifestValidator
import com.music.vivi.desktop.data.backup.validation.ModuleSchemaValidator
import com.music.vivi.desktop.data.backup.validation.ValidationPipeline
import com.music.vivi.desktop.data.network.github.GitHubReleaseClient
import com.music.vivi.desktop.data.network.jellyfin.JellyfinApiClient
import com.music.vivi.desktop.data.network.jellyfin.JellyfinRepositoryImpl
import com.music.vivi.desktop.data.network.listenbrainz.ListenBrainzApiClient
import com.music.vivi.desktop.data.network.listenbrainz.ListenBrainzRepositoryImpl
import com.music.vivi.desktop.data.network.listenbrainz.ScrobbleManager
import com.music.vivi.desktop.data.network.navidrome.NavidromeApiClient
import com.music.vivi.desktop.data.network.navidrome.NavidromeRepositoryImpl
import com.music.vivi.desktop.domain.repository.JellyfinRepository
import com.music.vivi.desktop.domain.repository.ListenBrainzRepository
import com.music.vivi.desktop.domain.repository.NavidromeRepository
import com.music.vivi.desktop.data.network.RemoteArtworkResolver
import com.music.vivi.desktop.data.player.PlaybackUriResolver
import com.music.vivi.desktop.data.youtube.YouTubeLyrics
import com.music.vivi.desktop.data.youtube.YouTubeMusicRepository
import com.music.vivi.desktop.data.youtube.YouTubePlaylistsRepository
import com.music.vivi.desktop.presentation.viewmodel.YouTubeAccountStateHolder
import com.music.vivi.desktop.presentation.viewmodel.YouTubeMusicStateHolder
import com.music.vivi.desktop.data.player.QueueManager
import com.music.vivi.desktop.data.player.mpv.MpvAudioEngine
import com.music.vivi.desktop.data.metadata.DesktopMetadataEditor
import com.music.vivi.desktop.data.network.lyrics.LrcLibClient
import com.music.vivi.desktop.data.network.musicbrainz.MusicBrainzClient
import com.music.vivi.desktop.data.repository.LyricsRepositoryImpl
import com.music.vivi.desktop.data.repository.MusicRepositoryImpl
import com.music.vivi.desktop.data.repository.SettingsRepositoryImpl
import com.music.vivi.desktop.data.scanner.DesktopMusicScanner
import com.music.vivi.desktop.data.watcher.LibraryWatcherService
import com.music.vivi.desktop.domain.player.AudioEngine
import com.music.vivi.desktop.domain.player.SleepTimerSession
import com.music.vivi.desktop.domain.repository.LyricsRepository
import com.music.vivi.desktop.domain.repository.MusicRepository
import com.music.vivi.desktop.domain.repository.SettingsRepository
import com.music.vivi.desktop.platform.mediasession.MediaSessionBridge
import com.music.vivi.desktop.presentation.viewmodel.DesktopListeningStatsTracker
import com.music.vivi.desktop.presentation.viewmodel.LibraryStateHolder
import com.music.vivi.desktop.presentation.viewmodel.PlaybackStateHolder
import com.music.vivi.desktop.presentation.viewmodel.PlayerViewModel
import com.music.vivi.desktop.presentation.viewmodel.SleepTimerStateHolder
import com.music.vivi.desktop.presentation.viewmodel.StatsStateHolder
import com.music.vivi.desktop.presentation.viewmodel.ThemeStateHolder
import com.music.vivi.desktop.presentation.viewmodel.UpdateStateHolder
import com.music.vivi.desktop.utils.DesktopPathProvider
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.serialization.json.Json
import okhttp3.OkHttpClient
import org.koin.core.module.Module
import org.koin.dsl.module
import java.io.File
import java.util.concurrent.TimeUnit

val coreModule: Module = module {
    single<CoroutineDispatcher> { Dispatchers.IO }
    single<CoroutineScope> { CoroutineScope(SupervisorJob() + Dispatchers.Default) }
    single<Json> {
        Json {
            prettyPrint = true
            ignoreUnknownKeys = true
            encodeDefaults = true
        }
    }
    single<OkHttpClient> {
        OkHttpClient.Builder()
            .connectTimeout(15, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .build()
    }
}

val settingsModule: Module = module {
    single<SettingsRepository> { SettingsRepositoryImpl(ioDispatcher = get(), json = get()) }
}

val databaseModule: Module = module {
    // One JSON store, the 1.55.0 `~/.vivimusic/device-sync.json`. The old
    // `vivimusicde.db`, when it exists, is read once by the store itself.
    single { LocalLibraryStore(DesktopPathProvider.syncStateFile, DesktopPathProvider.databaseFile) }
    single { MusicDao(store = get(), ioDispatcher = get()) }
    single { LocalPlaylistDao(store = get(), ioDispatcher = get()) }
    single { FavoritesDao(store = get(), ioDispatcher = get()) }
    single { LyricsDao(store = get(), ioDispatcher = get()) }
    single { ListenBrainzDao(store = get(), ioDispatcher = get()) }
}

val repositoryModule: Module = module {
    single { MusicBrainzClient(ioDispatcher = get()) }
    single<MusicRepository> {
        MusicRepositoryImpl(
            store = get(),
            musicDao = get(),
            favoritesDao = get(),
            playlistDao = get(),
            metadataEditor = get(),
            musicBrainzClient = get(),
            ioDispatcher = get()
        )
    }
    single { LrcLibClient(ioDispatcher = get()) }
    single<LyricsRepository> {
        LyricsRepositoryImpl(
            lyricsDao = get(),
            lrcLibClient = get(),
            settingsRepository = get(),
            youtubeLyrics = get(),
            ioDispatcher = get()
        )
    }
    single { YouTubeLyrics(ioDispatcher = get()) }
    single { YouTubeMusicRepository(ioDispatcher = get()) }
    single { ListenBrainzApiClient(okHttpClient = get()) }
    single<ListenBrainzRepository> { ListenBrainzRepositoryImpl(apiClient = get(), dao = get(), settingsRepository = get(), coroutineScope = get()) }
    single { NavidromeApiClient(okHttpClient = get(), json = get()) }
    single<NavidromeRepository> { NavidromeRepositoryImpl(apiClient = get(), musicDao = get(), playlistDao = get(), settingsRepository = get(), coroutineScope = get()) }
    single { JellyfinApiClient(okHttpClient = get(), json = get()) }
    single<JellyfinRepository> { JellyfinRepositoryImpl(apiClient = get(), musicDao = get(), playlistDao = get(), settingsRepository = get(), coroutineScope = get()) }
    single { ScrobbleManager(listenBrainzRepository = get(), navidromeRepository = get(), settingsRepository = get(), coroutineScope = get()) }
    single { RemoteArtworkResolver(navidromeRepository = get(), jellyfinRepository = get(), okHttpClient = get()) }
    single {
        PlaybackUriResolver(
            navidromeRepository = get(),
            jellyfinRepository = get(),
            youtubeMusicRepository = get()
        )
    }
    single { DesktopMusicScanner(musicDao = get(), lyricsDao = get(), ioDispatcher = get()) }
    single { DesktopMetadataEditor(musicDao = get(), ioDispatcher = get()) }
    single { GitHubReleaseClient(okHttpClient = get(), json = get(), ioDispatcher = get()) }
    single {
        LibraryWatcherService(
            scanner = get(),
            musicDao = get(),
            settingsRepository = get(),
            coroutineScope = get(),
            ioDispatcher = get()
        )
    }
}

val playerModule: Module = module {
    single<AudioEngine> { MpvAudioEngine(coroutineScope = get()) }
    single { QueueManager() }
    single { SleepTimerSession() }
}

val stateModule: Module = module {
    single { DesktopListeningStatsTracker(musicRepository = get(), scrobbleManager = get(), coroutineScope = get()) }
    single {
        PlaybackStateHolder(
            audioEngine = get(),
            queueManager = get(),
            musicRepository = get(),
            lyricsRepository = get(),
            settingsRepository = get(),
            listeningStatsTracker = get(),
            playbackUriResolver = get(),
            sleepTimerSession = get(),
            coroutineScope = get()
        )
    }
    single {
        SleepTimerStateHolder(
            session = get(),
            audioEngine = get(),
            queueManager = get(),
            coroutineScope = get()
        )
    }
    single {
        LibraryStateHolder(
            musicRepository = get(),
            settingsRepository = get(),
            lyricsRepository = get(),
            scanner = get(),
            coroutineScope = get(),
            watcherService = get()
        )
    }
    single {
        ThemeStateHolder(
            settingsRepository = get(),
            coroutineScope = get()
        )
    }
    single {
        StatsStateHolder(
            musicRepository = get(),
            coroutineScope = get()
        )
    }
    single {
        UpdateStateHolder(
            releaseClient = get(),
            settingsRepository = get(),
            coroutineScope = get(),
            autoBackupScheduler = get()
        )
    }
    single {
        AutoBackupScheduler(
            storeFile = DesktopPathProvider.syncStateFile,
            backupDirectory = File(DesktopPathProvider.syncStateFile.absoluteFile.parentFile, "backups"),
            settingsRepository = get(),
            coroutineScope = get()
        )
    }
    single { LibraryHealthChecker(store = get()) }
    single { YouTubePlaylistsRepository(store = get()) }
    single {
        MediaSessionBridge(
            playbackStateHolder = get(),
            coroutineScope = get()
        )
    }
    single {
        YouTubeMusicStateHolder(repository = get(), coroutineScope = get())
    }
    single {
        YouTubeAccountStateHolder(coroutineScope = get())
    }
    single {
        PlayerViewModel(
            playbackStateHolder = get(),
            libraryStateHolder = get(),
            themeStateHolder = get(),
            statsStateHolder = get(),
            updateStateHolder = get(),
            sleepTimerStateHolder = get(),
            mediaSessionBridge = get(),
            youtubeMusicStateHolder = get(),
            youtubeAccountStateHolder = get()
        )
    }
}

val backupModule: Module = module {
    single { BackupFormatDetector() }
    single { BackupWriter(json = get()) }
    single { BackupReader(formatDetector = get(), json = get()) }
    single { BackupSongResolver(musicDao = get()) }
    single { BackupFileValidator(formatDetector = get()) }
    single { ManifestValidator() }
    single { ModuleSchemaValidator(json = get()) }
    single {
        ValidationPipeline(
            fileValidator = get(),
            manifestValidator = get(),
            moduleSchemaValidator = get()
        )
    }
    single { RestorePlanner(backupReader = get()) }
    single<Map<BackupSection, BackupModuleHandler>> {
        mapOf(
            BackupSection.PLAYLISTS to PlaylistsModuleHandler(
                localPlaylistDao = get(),
                store = get(),
                songResolver = get(),
                json = get()
            ),
            BackupSection.GLOBAL_SETTINGS to GlobalSettingsModuleHandler(
                settingsRepository = get(),
                json = get()
            ),
            BackupSection.FAVORITES to FavoritesModuleHandler(
                favoritesDao = get(),
                store = get(),
                songResolver = get(),
                json = get()
            ),
            BackupSection.LYRICS to LyricsModuleHandler(
                store = get(),
                songResolver = get(),
                json = get()
            ),
            BackupSection.EQUALIZER to EqualizerModuleHandler(
                settingsRepository = get(),
                json = get()
            ),
            BackupSection.PLAYBACK_HISTORY to PlaybackHistoryModuleHandler(
                store = get(),
                songResolver = get(),
                json = get()
            ),
            BackupSection.ENGAGEMENT_STATS to EngagementStatsModuleHandler(
                store = get(),
                songResolver = get(),
                json = get()
            ),
            BackupSection.ARTIST_IMAGES to ArtistImagesModuleHandler(
                store = get(),
                json = get()
            )
        )
    }
    single {
        RestoreExecutor(
            backupReader = get(),
            validationPipeline = get(),
            handlers = get()
        )
    }
    single {
        BackupManager(
            backupWriter = get(),
            backupReader = get(),
            restorePlanner = get(),
            restoreExecutor = get(),
            validationPipeline = get(),
            handlers = get()
        )
    }
}

val appModules: List<Module> = listOf(
    coreModule,
    settingsModule,
    databaseModule,
    repositoryModule,
    playerModule,
    stateModule,
    backupModule
)

