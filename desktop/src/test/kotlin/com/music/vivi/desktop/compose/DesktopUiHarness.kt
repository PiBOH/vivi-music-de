package com.music.vivi.desktop.compose

import java.io.File
import java.nio.file.Files
import java.time.LocalDate
import java.time.ZoneId

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.runBlocking

import com.music.vivi.desktop.data.database.FavoritesDao
import com.music.vivi.desktop.data.database.LocalPlaylistDao
import com.music.vivi.desktop.data.database.LyricsDao
import com.music.vivi.desktop.data.database.MusicDao
import com.music.vivi.desktop.data.listentogether.ListenTogetherManager
import com.music.vivi.desktop.data.model.Lyrics
import com.music.vivi.desktop.data.model.Song
import com.music.vivi.desktop.data.network.lyrics.LyricsSearchResultItem
import com.music.vivi.desktop.data.player.PlaybackAudioCache
import com.music.vivi.desktop.data.player.QueueManager
import com.music.vivi.desktop.data.repository.MusicRepositoryImpl
import com.music.vivi.desktop.data.repository.SettingsRepositoryImpl
import com.music.vivi.desktop.data.scanner.DesktopMusicScanner
import com.music.vivi.desktop.data.store.LocalLibraryStore
import com.music.vivi.desktop.domain.player.AudioDevice
import com.music.vivi.desktop.domain.player.AudioEngine
import com.music.vivi.desktop.domain.player.PlaybackState
import com.music.vivi.desktop.domain.repository.LyricsRepository
import com.music.vivi.desktop.presentation.viewmodel.LibraryStateHolder
import com.music.vivi.desktop.presentation.viewmodel.PlaybackStateHolder
import com.music.vivi.desktop.presentation.viewmodel.PlayerViewModel
import com.music.vivi.desktop.presentation.viewmodel.StatsStateHolder
import com.music.vivi.desktop.presentation.viewmodel.ThemeStateHolder
import com.music.vivi.desktop.presentation.viewmodel.YouTubeAccountStateHolder

/**
 * A real desktop graph with a temporary JSON store and a fake audio engine.
 *
 * The screens, view model, queue and Listen Together manager are the production
 * classes; only the engine is recorded instead of opening libmpv, so a widget
 * test can run on a machine without audio output.
 */
class DesktopUiHarness : AutoCloseable {

    val tempDir: File = Files.createTempDirectory("vivi-widget-test-").toFile()
    val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    val store = LocalLibraryStore(
        File(tempDir, "device-sync.json"),
        File(tempDir, "legacy.db")
    )
    val musicDao = MusicDao(store)
    val lyricsDao = LyricsDao(store)
    val musicRepository = MusicRepositoryImpl(
        store = store,
        musicDao = musicDao,
        favoritesDao = FavoritesDao(store),
        playlistDao = LocalPlaylistDao(store)
    )
    val settingsRepository = SettingsRepositoryImpl(File(tempDir, "settings.json"))
    val lyricsRepository = object : LyricsRepository {
        override suspend fun getLyrics(
            songId: String,
            songPath: String,
            title: String,
            artist: String,
            durationMs: Long,
            album: String
        ): Lyrics? = null

        override suspend fun saveLyrics(songId: String, lyrics: Lyrics) = Unit

        override suspend fun deleteLyrics(songId: String) = Unit

        override suspend fun resetAllLyrics() = Unit

        override suspend fun searchOnlineLyrics(query: String): List<LyricsSearchResultItem> = emptyList()
    }
    val engine = RecordingAudioEngine()
    val queueManager = QueueManager()
    val scanner = DesktopMusicScanner(
        musicDao = musicDao,
        lyricsDao = lyricsDao,
        artworkCacheDir = File(tempDir, "artworks")
    )
    val playbackStateHolder = PlaybackStateHolder(
        audioEngine = engine,
        queueManager = queueManager,
        musicRepository = musicRepository,
        lyricsRepository = lyricsRepository,
        settingsRepository = settingsRepository,
        audioCache = PlaybackAudioCache(
            okHttpClient = okhttp3.OkHttpClient(),
            coroutineScope = scope,
            directory = File(tempDir, "audio-cache")
        ),
        coroutineScope = scope
    )
    val libraryStateHolder = LibraryStateHolder(
        musicRepository = musicRepository,
        settingsRepository = settingsRepository,
        scanner = scanner,
        lyricsRepository = lyricsRepository,
        coroutineScope = scope
    )
    val themeStateHolder = ThemeStateHolder(settingsRepository, scope)
    val statsStateHolder = StatsStateHolder(musicRepository, scope)
    val listenTogetherManager = ListenTogetherManager(
        playback = playbackStateHolder,
        queueManager = queueManager,
        audioEngine = engine,
        settingsRepository = settingsRepository,
        coroutineScope = scope
    )
    val viewModel = PlayerViewModel(
        playbackStateHolder = playbackStateHolder,
        libraryStateHolder = libraryStateHolder,
        themeStateHolder = themeStateHolder,
        statsStateHolder = statsStateHolder,
        youtubeAccountStateHolder = YouTubeAccountStateHolder(scope, restoreOnInit = false),
        listenTogetherManager = listenTogetherManager
    )

    init {
        runBlocking {
            settingsRepository.updateSettings { settings ->
                settings.copy(
                    listenTogetherUsername = "Tester",
                    listenTogetherPartySize = 5,
                    listenTogetherServerUrl = "ws://127.0.0.1:1"
                )
            }
        }
    }

    override fun close() {
        listenTogetherManager.client.disconnect()
        scope.cancel()
        engine.release()
        store.close()
        tempDir.deleteRecursively()
    }

    companion object {
        fun song(id: String, title: String, lastPlayedTimestamp: Long, path: String): Song =
            Song(
                id = id,
                title = title,
                artist = "Widget Artist",
                album = "Widget Album",
                path = path,
                duration = 180_000L,
                lastPlayedTimestamp = lastPlayedTimestamp
            )

        fun historySongs(now: Long = System.currentTimeMillis()): List<Song> {
            val startOfToday = LocalDate.now()
                .atStartOfDay(ZoneId.systemDefault())
                .toInstant()
                .toEpochMilli()
            val songs = mutableListOf(
                song("today-newest", "Today Newest", startOfToday, "C:/fixture/today-newest.mp3")
            )
            repeat(20) { index ->
                songs += song(
                    "scroll-$index",
                    "Scroll ${index + 1}",
                    startOfToday,
                    "C:/fixture/scroll-$index.mp3"
                )
            }
            songs += song(
                "scroll-target",
                "Scroll Target",
                startOfToday,
                "C:/fixture/scroll-target.mp3"
            )
            songs += song(
                "last-month",
                "Last Month",
                now - 30L * 24 * 60 * 60 * 1000,
                "C:/fixture/last-month.mp3"
            )
            songs += song(
                "last-year",
                "Last Year",
                now - 400L * 24 * 60 * 60 * 1000,
                "C:/fixture/last-year.mp3"
            )
            return songs
        }
    }
}

class RecordingAudioEngine : AudioEngine {
    private val _playbackState = MutableStateFlow(PlaybackState.IDLE)
    override val playbackState: StateFlow<PlaybackState> = _playbackState.asStateFlow()
    private val _position = MutableStateFlow(0L)
    override val currentPositionMs: StateFlow<Long> = _position.asStateFlow()
    private val _duration = MutableStateFlow(0L)
    override val durationMs: StateFlow<Long> = _duration.asStateFlow()
    private val _volume = MutableStateFlow(1f)
    override val volume: StateFlow<Float> = _volume.asStateFlow()
    private val _muted = MutableStateFlow(false)
    override val isMuted: StateFlow<Boolean> = _muted.asStateFlow()
    override val errorEvents = MutableSharedFlow<String>(extraBufferCapacity = 8)
    override val completionEvents = MutableSharedFlow<Unit>(extraBufferCapacity = 8)
    override val currentTrackPath: String? get() = lastPlayedUri

    var playCalls = 0
        private set
    var lastPlayedUri: String? = null
        private set

    override fun play(mediaPathOrUri: String, httpHeaders: List<String>) {
        playCalls++
        lastPlayedUri = mediaPathOrUri
        _position.value = 0L
        _duration.value = 180_000L
        _playbackState.value = PlaybackState.PLAYING
    }

    override fun play() {
        _playbackState.value = PlaybackState.PLAYING
    }

    override fun pause() {
        _playbackState.value = PlaybackState.PAUSED
    }

    override fun stop() {
        _playbackState.value = PlaybackState.IDLE
        _position.value = 0L
    }

    override fun seekTo(positionMs: Long) {
        _position.value = positionMs
    }

    override fun setVolume(volume: Float) {
        _volume.value = volume
    }

    override fun setMute(muted: Boolean) {
        _muted.value = muted
    }

    override fun setReplayGain(enabled: Boolean, mode: String) = Unit

    override fun getAudioDevices(): List<AudioDevice> = emptyList()

    override fun release() = Unit
}
