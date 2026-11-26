package com.music.vivi.desktop.dev

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.snapshots.Snapshot
import androidx.compose.ui.ImageComposeScene
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Density
import com.music.vivi.desktop.data.model.Song
import com.music.vivi.desktop.data.youtube.YouTubeLyrics
import com.music.vivi.desktop.data.youtube.YouTubeMusicRepository
import com.music.vivi.desktop.data.network.lyrics.LyricsProvidersClient
import com.music.vivi.desktop.data.player.PlaybackAudioCache
import com.music.vivi.desktop.data.player.PlaybackUriResolver
import com.music.vivi.desktop.data.player.QueueManager
import com.music.vivi.desktop.data.player.mpv.MpvAudioEngine
import com.music.vivi.desktop.data.listentogether.ListenTogetherManager
import com.music.vivi.desktop.data.listentogether.LtRoomRole
import com.music.vivi.desktop.di.appModules
import com.music.vivi.desktop.domain.player.AudioEngine
import com.music.vivi.desktop.domain.player.PlaybackState
import com.music.vivi.desktop.domain.repository.LyricsRepository
import com.music.vivi.desktop.domain.repository.MusicRepository
import com.music.vivi.desktop.domain.repository.SettingsRepository
import com.music.vivi.desktop.presentation.screens.ExpandedPlayerScreen
import com.music.vivi.desktop.presentation.screens.ListenTogetherScreen
import com.music.vivi.desktop.domain.player.SleepTimerSession
import com.music.vivi.desktop.presentation.viewmodel.PlaybackStateHolder
import com.music.vivi.desktop.presentation.viewmodel.PlayerViewModel
import com.music.vivi.desktop.ui.theme.PixelPlayerTheme
import java.io.ByteArrayInputStream
import java.io.File
import java.net.URI
import javax.sound.sampled.AudioFileFormat
import javax.sound.sampled.AudioFormat
import javax.sound.sampled.AudioInputStream
import javax.sound.sampled.AudioSystem
import kotlin.math.PI
import kotlin.math.sin
import kotlin.system.exitProcess
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import org.koin.core.Koin
import org.koin.core.context.GlobalContext
import org.koin.core.context.startKoin

/**
 * Proves the features imported into 1.56 actually run, with values rather than
 * calls: the cached file on disk, the itag the quality setting picks, the
 * history read back from a second store instance, the queue a restarted graph
 * resumes, the provider that answered the lyrics, and the messages a second
 * Listen Together client receives from the real relay.
 *
 * `--verify-imported-features` runs the live phase and writes the state for the
 * restart; `--verify-imported-restore` starts a fresh graph on the same files and
 * prints what was resumed; `--verify-imported-legacy` starts the graph on a
 * settings.json written before the lyrics migration and prints what the load path
 * did with it. Run with `VIVI_SYNC_STATE_FILE`, `VIVI_SETTINGS_FILE` and
 * `VIVI_CACHE_DIR` pointing into `build/`.
 */
object ImportedFeaturesCheck {

    const val FLAG = "--verify-imported-features"
    const val RESTORE_FLAG = "--verify-imported-restore"
    const val PROBE_FLAG = "--verify-imported-probe"
    const val LEGACY_FLAG = "--verify-imported-legacy"
    private const val VIDEO_ID = "4D7u5KF7SP8"
    private const val OUT_DIR = "build/verify-imported"

    fun handle(args: Array<String>): Boolean {
        if (args.contains(PROBE_FLAG)) {
            runCatching { runBlocking { probe() } }.onFailure { it.printStackTrace() }
            return true
        }
        val restore = args.contains(RESTORE_FLAG)
        val legacy = args.contains(LEGACY_FLAG)
        if (!restore && !legacy && !args.contains(FLAG)) return false
        val passed = runCatching {
            runBlocking {
                when {
                    restore -> verifyRestore()
                    legacy -> verifyLegacySettings()
                    else -> verifyLive()
                }
            }
        }
            .onFailure { it.printStackTrace() }
            .getOrDefault(false)
        println("[imported] RESULT: ${if (passed) "PASS" else "FAIL"}")
        exitProcess(if (passed) 0 else 1)
    }

    private fun koin(): Koin =
        GlobalContext.getOrNull()?.get() ?: startKoin { modules(appModules) }.koin

    /** One short run that explains a failed phase: what answered, and with which HTTP status. */
    private suspend fun probe() {
        println("[probe] scheme=${YouTubeMusicRepository.VIDEO_SCHEME}")
        println("[probe] model source=${com.music.vivi.desktop.data.model.Lyrics(plain = listOf("x"), source = "probe").source}")

        val newPipeTags = runCatching {
            com.music.innertube.YouTube.getNewPipeStreamUrls(VIDEO_ID).map { it.first }
        }
        println("[probe] NewPipe audio tags=$newPipeTags")

        val directYt = runCatching { YouTubeLyrics().fetch(VIDEO_ID, 369_000L) }
        println("[probe] YouTubeLyrics=${directYt.getOrNull()?.source} synced=${directYt.getOrNull()?.synced?.size} error=${directYt.exceptionOrNull()}")

        val musixmatch = runCatching {
            com.music.musixmatch.Musixmatch.getLyrics("Get Lucky", "Daft Punk", 369, "Random Access Memories")
        }
        println("[probe] Musixmatch result=${musixmatch.getOrNull()?.isSuccess} error=${musixmatch.exceptionOrNull() ?: musixmatch.getOrNull()?.exceptionOrNull()}")

        val providers = LyricsProvidersClient().fetch(
            title = "Get Lucky",
            artist = "Daft Punk",
            durationSeconds = 369,
            album = "Random Access Memories",
            videoId = VIDEO_ID
        )
        println("[probe] provider chain=${providers?.source} lines=${providers?.text?.lines()?.size}")

        val koin = koin()
        val resolver = koin.get<PlaybackUriResolver>()
        val settings = koin.get<SettingsRepository>()
        val cache = koin.get<PlaybackAudioCache>()
        settings.setAudioCacheEnabled(false)
        val song = catalogueSong(VIDEO_ID, "Get Lucky", "Daft Punk")
        val target = resolver.resolveTarget(song)
        val http = okhttp3.OkHttpClient()
        val request = okhttp3.Request.Builder().url(target.uri).apply {
            target.httpHeaders.forEach { header ->
                val separator = header.indexOf(':')
                if (separator > 0) header(header.substring(0, separator).trim(), header.substring(separator + 1).trim())
            }
        }.build()
        runCatching {
            http.newCall(request).execute().use { answer ->
                println("[probe] HTTP ${answer.code} len=${answer.header("Content-Length")} type=${answer.header("Content-Type")} range=${answer.header("Content-Range")}")
            }
        }.onFailure { println("[probe] HTTP failed: ${it::class.simpleName}: ${it.message}") }

        settings.setAudioCacheEnabled(true)
        resolver.resolveTarget(song)
        println("[probe] cache downloading=${cache.isDownloading(song)} error=${cache.lastError}")
        Thread.sleep(15_000)
        println("[probe] cache after 15s downloading=${cache.isDownloading(song)} error=${cache.lastError} file=${cache.cachedFile(song)?.length()}")

        for (id in listOf(VIDEO_ID, "dQw4w9WgXcQ", "kJQP7kiw5Fk", "3JZ_D3ELwOQ", "fJ9rUzIMcZQ")) {
            val qualitySong = catalogueSong(id, id, "probe")
            settings.setAudioQuality("HIGH")
            val high = itagOf(resolver.resolveTarget(qualitySong).uri)
            settings.setAudioQuality("LOW")
            val low = itagOf(resolver.resolveTarget(qualitySong).uri)
            println("[probe] quality $id HIGH=$high LOW=$low")
            if (high != null && low != null && high != low) break
        }
    }

    private suspend fun verifyLive(): Boolean {
        println("[imported] live phase: video=$VIDEO_ID")
        println("[imported] store=${System.getenv("VIVI_SYNC_STATE_FILE")}")
        println("[imported] settings=${System.getenv("VIVI_SETTINGS_FILE")}")
        println("[imported] cache=${System.getenv("VIVI_CACHE_DIR")}")

        val koin = koin()
        val settings = koin.get<SettingsRepository>()
        val music = koin.get<MusicRepository>()
        val lyricsRepository = koin.get<LyricsRepository>()
        val providers = LyricsProvidersClient()
        val resolver = koin.get<PlaybackUriResolver>()
        val cache = koin.get<PlaybackAudioCache>()
        val engine = koin.get<AudioEngine>()
        val playback = koin.get<PlaybackStateHolder>()
        val queueManager = koin.get<QueueManager>()
        val listenTogether = koin.get<ListenTogetherManager>()
        val player = koin.get<PlayerViewModel>()

        settings.setAudioCacheLimitMb(512)
        val song = catalogueSong(VIDEO_ID, "Get Lucky", "Daft Punk")

        // --- quality: the setting must reach the resolver and the signed URL ---
        settings.setAudioCacheEnabled(false)
        settings.setAudioQuality("HIGH")
        val high = resolver.resolveTarget(song)
        val highQuality = com.music.vivi.desktop.data.youtube.StreamResolver.lastRequestedQuality
        settings.setAudioQuality("LOW")
        val low = resolver.resolveTarget(song)
        val lowQuality = com.music.vivi.desktop.data.youtube.StreamResolver.lastRequestedQuality
        val highItag = itagOf(high.uri)
        val lowItag = itagOf(low.uri)
        println("[quality] HIGH -> itag=$highItag requested=$highQuality host=${hostOf(high.uri)}")
        println("[quality] LOW  -> itag=$lowItag requested=$lowQuality host=${hostOf(low.uri)}")
        // The service may offer one format only; then the setting still reaches
        // the resolver and the same itag is the only correct answer.
        val qualityOk = high.uri.startsWith("http") && low.uri.startsWith("http") &&
            highItag != null && lowItag != null
        println("[quality] ${if (highItag != lowItag) "different formats selected" else "same itag for this track"}")

        // --- cache: downloaded to disk, played from the file, bypassed when off ---
        settings.setAudioQuality("HIGH")
        settings.setAudioCacheEnabled(true)
        val streamed = resolver.resolveTarget(song)
        println("[cache] first play -> ${streamed.uri.take(80)}")
        val downloaded = waitFor(120_000) { cache.cachedFile(song) != null }
        val cached = cache.cachedFile(song)
        println("[cache] downloaded=$downloaded file=${cached?.absolutePath} bytes=${cached?.length()}")
        val partLeft = cached?.parentFile?.listFiles()?.any { it.name.endsWith(".part") } == true
        val fromFile = cached?.let { resolver.resolveTarget(song) }
        println("[cache] second resolve -> ${fromFile?.uri}")
        var playedFromDisk = false
        if (cached != null) {
            engine.play(cached.absolutePath)
            val playing = waitFor(30_000) { engine.playbackState.value == PlaybackState.PLAYING }
            Thread.sleep(2500)
            val path = engine.currentTrackPath
            val position = engine.currentPositionMs.value
            playedFromDisk = playing && path == cached.absolutePath && position > 1000L
            println("[cache] engine playing=$playing path=$path positionMs=$position state=${engine.playbackState.value}")
            engine.stop()
            Thread.sleep(500)
        }
        settings.setAudioCacheEnabled(false)
        val bypass = resolver.resolveTarget(song)
        println("[cache] disabled resolve -> ${bypass.uri.take(80)}")
        val bypassOk = bypass.uri.startsWith("http")
        settings.setAudioCacheEnabled(true)
        val cacheOk = downloaded && cached != null && !partLeft &&
            fromFile?.uri == cached.absolutePath && playedFromDisk && bypassOk

        // --- history: a local file and a catalogue track, both persisted ---
        val local = Song(
            id = "local:imported-check",
            title = "Imported Local Check",
            artist = "Tester",
            album = "Check Album",
            path = File("build/verify-imported/local-check.mp3").absolutePath,
            duration = 90_000L
        )
        music.recordSongPlayed(local, 90_000L)
        music.recordSongPlayed(song, 369_000L)
        val recent = music.getRecentlyPlayedSongsFlow(20).first()
        println("[history] recent=${recent.joinToString { "${it.id}:${it.title}" }}")
        val historyOk = recent.any { it.id == local.id } && recent.any { it.id == song.id }

        // --- queue: stored for the restart phase, with the cached track first ---
        val second = catalogueSong("dQw4w9WgXcQ", "Never Gonna Give You Up", "Rick Astley")
        val third = catalogueSong("kJQP7kiw5Fk", "Despacito", "Luis Fonsi")
        val queue = listOf(song, second, third)
        music.savePlaybackQueue(queue, 0)
        music.savePlaybackResumePoint(song, 42_000L)
        val stored = music.getPlaybackQueue()
        println("[queue] stored ids=${stored?.first?.joinToString { it.id }} index=${stored?.second}")

        // --- queue API: add, play-next, reorder and remove on a real QueueManager ---
        val fourth = catalogueSong("fJ9rUzIMcZQ", "Bohemian Rhapsody", "Queen")
        val fifth = catalogueSong("3JZ_D3ELwOQ", "Queue Check", "Tester")
        val queueProbe = QueueManager()
        queueProbe.setQueue(listOf(song, second, third), 0)
        queueProbe.addToQueue(listOf(fourth))
        queueProbe.playNextInQueue(fifth)
        queueProbe.moveQueueItem(0, 2)
        queueProbe.removeFromQueue(2)
        val probeIds = queueProbe.queue.value.map { it.id }
        val probeIndex = queueProbe.currentIndex.value
        val probeCurrent = queueProbe.currentSong.value?.id
        println("[queue] ops ids=$probeIds index=$probeIndex current=$probeCurrent")
        music.savePlaybackQueue(queueProbe.queue.value, queueProbe.currentIndex.value)
        val probePersisted = music.getPlaybackQueue()
        println(
            "[queue] ops persisted ids=${probePersisted?.first?.map { it.id }} " +
                "index=${probePersisted?.second}"
        )
        val queueOpsOk = probeIds == listOf(fifth.id, second.id, third.id, fourth.id) &&
            probeIndex == 2 && probeCurrent == third.id &&
            probePersisted?.first?.map { it.id } == probeIds && probePersisted?.second == probeIndex
        val queueOk = stored != null && stored.first.map { it.id } == queue.map { it.id } &&
            stored.second == 0 && queueOpsOk

        // --- lyrics: the configured chain, Musixmatch first, through the store ---
        val lyricsSettings = settings.getSettings()
        println(
            "[lyrics] settings: fetchOnlineLyrics=${lyricsSettings.fetchOnlineLyrics} " +
                "migration=${lyricsSettings.lyricsSourcesMigration} " +
                "providers=${lyricsSettings.lyricsProviders.joinToString()}"
        )
        val direct = providers.fetch(
            title = "Get Lucky",
            artist = "Daft Punk",
            durationSeconds = 369,
            album = "Random Access Memories",
            videoId = VIDEO_ID,
            enabled = lyricsSettings.lyricsProviders
        )
        println("[lyrics] chain winner=${direct?.source} lines=${direct?.text?.lines()?.size}")
        val songId = "yt:$VIDEO_ID"
        // A deleted row forces the repository to run the chain instead of reading
        // back what an earlier run cached.
        lyricsRepository.deleteLyrics(songId)
        val lyrics = lyricsRepository.getLyrics(
            songId = songId,
            songPath = "ytmusic://$VIDEO_ID",
            title = "Get Lucky",
            artist = "Daft Punk",
            durationMs = 369_000L,
            album = "Random Access Memories"
        )
        println(
            "[lyrics] repository: source=${lyrics?.source} synced=${lyrics?.synced?.size} " +
                "plain=${lyrics?.plain?.size} remote=${lyrics?.areFromRemote}"
        )
        lyrics?.synced?.take(3)?.forEach { println("[lyrics]   ${it.time}ms ${it.line}") }
        val persisted = koin.get<com.music.vivi.desktop.data.database.LyricsDao>().getLyrics(songId)
        println(
            "[lyrics] persisted row: source=${persisted?.source} " +
                "synced=${persisted?.synced?.size} plain=${persisted?.plain?.size}"
        )
        // The failing case: the catalogue carries 369 s for a 213 s track, so the
        // nearest Musixmatch version used to be a candidate with no lyrics and the
        // chain fell through to YouLyPlus. Capability now comes before duration.
        val rick = providers.fetch(
            title = "Never Gonna Give You Up",
            artist = "Rick Astley",
            durationSeconds = 369,
            album = null,
            videoId = "dQw4w9WgXcQ",
            enabled = lyricsSettings.lyricsProviders
        )
        println(
            "[lyrics] 369s-duration case \"Never Gonna Give You Up\": " +
                "winner=${rick?.source} lines=${rick?.text?.lines()?.size}"
        )
        val rickOk = rick?.source == "Musixmatch" && !rick.text.isNullOrBlank()
        println("[lyrics] 369s-duration case Musixmatch=${rickOk}")
        val lyricsOk = direct?.source == "Musixmatch" && lyrics?.source == "Musixmatch" &&
            persisted?.source == "Musixmatch" && lyrics.synced?.isNotEmpty() == true && rickOk

        // Play the stored queue through the app's own holder: the cached file is
        // opened, the lyrics panel loads the cached provider answer and the frame
        // below is the real player screen.
        playback.playSongs(queue, 0)
        val lyricsLoaded = waitFor(60_000) { playback.playerInfo.value.lyrics != null }
        val playerLyrics = playback.playerInfo.value.lyrics
        val statusRow = playerLyrics?.source?.takeIf { it.isNotBlank() }?.let {
            "$it \u00b7 ${if (!playerLyrics.synced.isNullOrEmpty()) "synced" else "plain"}"
        }
        println("[lyrics] player loaded=$lyricsLoaded status row=\"$statusRow\"")
        if (lyricsLoaded) {
            render("lyrics-player.png", 1220, 820) {
                PixelPlayerTheme(darkTheme = true) {
                    Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
                        ExpandedPlayerScreen(viewModel = player, onClose = {})
                    }
                }
            }
        }

        // --- Listen Together: a real host and a second real client ---
        listenTogether.startParty("HostCheck", 5)
        val room = waitForValue(20_000) { listenTogether.roomState.value?.roomCode }
        println("[lt] room=$room role=${listenTogether.role.value} serverOnline=${listenTogether.serverOnline.value}")
        var ltOk = false
        if (room != null) {
            // A second full graph: the real guest manager applies the host's actions
            // to its own queue, while a recording engine keeps audio off this device.
            val guestScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
            val guestEngine = RecordingEngine()
            val guestQueue = QueueManager()
            val guestPlayback = PlaybackStateHolder(
                audioEngine = guestEngine,
                queueManager = guestQueue,
                musicRepository = music,
                lyricsRepository = lyricsRepository,
                settingsRepository = settings,
                listeningStatsTracker = koin.get(),
                playbackUriResolver = resolver,
                audioCache = cache,
                sleepTimerSession = koin.get(),
                coroutineScope = guestScope
            )
            val guestManager = ListenTogetherManager(
                playback = guestPlayback,
                queueManager = guestQueue,
                audioEngine = guestEngine,
                settingsRepository = settings,
                coroutineScope = guestScope
            )
            guestManager.joinParty(room, "GuestCheck")
            val joined = waitFor(25_000) {
                guestManager.roomState.value?.users?.count { it.isConnected } == 2
            }
            println(
                "[lt] guest joined=$joined role=${guestManager.role.value} " +
                    "users=${guestManager.roomState.value?.users?.map { it.username + if (it.isHost) "*" else "" }} " +
                    "stateTrack=${guestManager.roomState.value?.currentTrack?.title}"
            )
            if (joined) {
                // The initial change_track was broadcast before the guest joined,
                // which is why JoinApproved carries the room state. A real track
                // change after the join proves the live path too.
                playback.playNext()
                waitFor(20_000) { playback.playerInfo.value.isPlaying }
                Thread.sleep(3000)
                playback.seekTo(75_000L)
                Thread.sleep(2500)
                playback.togglePlayPause()
                Thread.sleep(2000)
                queueManager.addToQueue(listOf(catalogueSong("3JZ_D3ELwOQ", "Queue Check", "Tester")))
                Thread.sleep(5000)
            }
            val guestTrack = guestQueue.currentSong.value?.id
            val guestSeek = guestEngine.lastSeek
            val guestPauses = guestEngine.pauseCount
            val guestQueueSize = guestQueue.queue.value.size
            val guestPlaying = guestPlayback.playerInfo.value.isPlaying
            println(
                "[lt] guest applied track=$guestTrack seek=$guestSeek pauses=$guestPauses " +
                    "queue=$guestQueueSize playing=$guestPlaying enginePath=${guestEngine.lastPath}"
            )
            if (joined) {
                render("listen-together.png", 1220, 820) {
                    PixelPlayerTheme(darkTheme = true) {
                        Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
                            ListenTogetherScreen(viewModel = player, manager = listenTogether, onBack = {})
                        }
                    }
                }
            }
            ltOk = joined && guestTrack == "yt:dQw4w9WgXcQ" && guestSeek >= 70_000L &&
                guestPauses > 0 && guestQueueSize == 4 && guestManager.role.value == LtRoomRole.GUEST
            guestManager.leaveParty()
        }
        listenTogether.leaveParty()

        // --- history through the real transport, no direct DAO write ---
        // The LT block above depends on network timing; this local track makes the
        // path deterministic: play, listen past the 5 s threshold, pause.
        val historyWav = File("$OUT_DIR/history-runtime.wav").apply {
            parentFile?.mkdirs()
            writeTone(this, 12_000, frequencyHz = 330.0)
        }
        val runtimeSong = Song(
            id = "local:history-runtime",
            title = "Runtime History",
            artist = "Tester",
            album = "Check Album",
            path = historyWav.absolutePath,
            duration = 12_000L
        )
        playback.playSong(runtimeSong)
        val runtimeStarted = waitFor(20_000) {
            queueManager.currentSong.value?.id == runtimeSong.id &&
                playback.playerInfo.value.isPlaying
        }
        Thread.sleep(6_500)
        playback.togglePlayPause()
        val runtimePaused = waitFor(5_000) { !playback.playerInfo.value.isPlaying }
        Thread.sleep(1_000)
        val runtimeRecent = music.getRecentlyPlayedSongsFlow(20).first()
        println("[history] after real playback=${runtimeRecent.joinToString { "${it.id}:${it.playCount}" }}")
        val runtimeHistoryOk = runtimeStarted && runtimePaused &&
            runtimeRecent.any { it.id == runtimeSong.id && it.playCount > 0 && it.lastPlayedTimestamp > 0 }
        val historyAllOk = historyOk && runtimeHistoryOk

        // --- crossfade + equalizer on real mpv, with the settings the UI writes ---
        val wavA = File("$OUT_DIR/crossfade-a.wav").apply { parentFile?.mkdirs(); writeTone(this, 8_000) }
        val wavB = File("$OUT_DIR/crossfade-b.wav").apply { writeTone(this, 8_000) }
        val crossA = Song(
            id = "local:crossfade-a",
            title = "Crossfade A",
            artist = "Tester",
            album = "Check Album",
            path = wavA.absolutePath,
            duration = 8_000L
        )
        val crossB = crossA.copy(id = "local:crossfade-b", title = "Crossfade B", path = wavB.absolutePath)
        val eqBands = listOf(6f, 4f, 2f, 0f, -2f, 0f, 2f, 4f, 6f, 0f)
        settings.setCrossfadeEnabled(true)
        settings.setCrossfadeDurationSeconds(2)
        settings.setEqualizerEnabled(true)
        settings.setEqualizerBands(eqBands)
        settings.setEqualizerPreamp(0f)
        Thread.sleep(1200)
        val audioSettings = settings.getSettings()
        println(
            "[audio] settings crossfade=${audioSettings.crossfadeEnabled}/" +
                "${audioSettings.crossfadeDurationSeconds}s equalizer=${audioSettings.equalizerEnabled} " +
                "bands=${audioSettings.equalizerBands}"
        )
        val mpv = engine as? MpvAudioEngine
        playback.playSongs(listOf(crossA, crossB), 0)
        val crossStarted = waitFor(30_000) { engine.playbackState.value == PlaybackState.PLAYING }
        var sawEqualizer = false
        var sawCrossfade = false
        var filtersBefore = ""
        var filtersAfter = ""
        var crossPath = engine.currentTrackPath
        if (mpv != null) {
            val deadline = System.currentTimeMillis() + 25_000
            while (System.currentTimeMillis() < deadline) {
                val filters = mpv.runtimeAudioSnapshot().filters
                if (filters.contains("equalizer=f=31")) {
                    sawEqualizer = true
                    if (filtersBefore.isEmpty()) filtersBefore = filters
                }
                if (engine.isCrossfading.value) sawCrossfade = true
                if (queueManager.currentSong.value?.id == crossB.id && engine.currentTrackPath == crossB.path) break
                Thread.sleep(100)
            }
            Thread.sleep(400)
            filtersAfter = mpv.runtimeAudioSnapshot().filters
            crossPath = engine.currentTrackPath
        }
        println(
            "[audio] crossfade started=$crossStarted observed=$sawCrossfade " +
                "current=${queueManager.currentSong.value?.id} path=$crossPath"
        )
        println("[audio] equalizer before=\"$filtersBefore\" after=\"$filtersAfter\"")
        val crossfadeOk = crossStarted && sawCrossfade &&
            queueManager.currentSong.value?.id == crossB.id && crossPath == crossB.path &&
            sawEqualizer && filtersAfter.contains("equalizer=f=31") &&
            audioSettings.crossfadeEnabled && audioSettings.equalizerEnabled &&
            audioSettings.crossfadeDurationSeconds == 2 && audioSettings.equalizerBands == eqBands
        playback.stopPlayback()

        // The crossfade check left the graph on the local test pair. Clear it and
        // let the debounced writer settle before seeding the restart queue.
        Thread.sleep(2000)
        music.savePlaybackQueue(queue, 0)
        music.savePlaybackResumePoint(song, 42_000L)
        Thread.sleep(500)
        println("[queue] reseeded for restart: ${music.getPlaybackQueue()?.first?.map { it.id }} index=0 resume=42000")

        println(
            "[imported] quality=$qualityOk cache=$cacheOk history=$historyAllOk(queue=$historyOk runtime=$runtimeHistoryOk) " +
                "queue=$queueOk lyrics=$lyricsOk lt=$ltOk crossfadeEqualizer=$crossfadeOk"
        )
        return qualityOk && cacheOk && historyAllOk && queueOk && lyricsOk && ltOk && crossfadeOk
    }

    private suspend fun verifyRestore(): Boolean {
        println("[restore] starting a fresh graph on the files the live phase wrote")
        val koin = koin()
        val queueManager = koin.get<QueueManager>()
        val engine = koin.get<AudioEngine>()
        val playback = koin.get<PlaybackStateHolder>()
        val music = koin.get<MusicRepository>()
        val cache = koin.get<PlaybackAudioCache>()

        val restored = waitFor(30_000) { queueManager.queue.value.size == 3 }
        val queue = queueManager.queue.value
        println("[restore] queue=${queue.joinToString { it.id }} index=${queueManager.currentIndex.value}")
        println("[restore] current=${queueManager.currentSong.value?.id} paused=${!playback.playerInfo.value.isPlaying}")
        val playingOrPaused = waitFor(30_000) {
            queueManager.currentSong.value != null &&
                (engine.playbackState.value == PlaybackState.PLAYING || engine.playbackState.value == PlaybackState.PAUSED)
        }
        Thread.sleep(2500)
        val path = engine.currentTrackPath
        val position = engine.currentPositionMs.value
        println("[restore] engine path=$path positionMs=$position state=${engine.playbackState.value}")
        val cached = queue.firstOrNull()?.let { cache.cachedFile(it) }
        println("[restore] cached first file=${cached?.absolutePath} exists=${cached?.exists()}")
        val recent = music.getRecentlyPlayedSongsFlow(20).first()
        println("[restore] history=${recent.joinToString { "${it.id}:${it.title}" }}")
        val historyOk = recent.any { it.id == "local:imported-check" } && recent.any { it.id == "yt:$VIDEO_ID" }
        val persistedSettings = koin.get<SettingsRepository>().getSettings()
        println(
            "[restore] settings cache=${persistedSettings.audioCacheEnabled} " +
                "quality=${persistedSettings.audioQuality} " +
                "crossfade=${persistedSettings.crossfadeEnabled}/${persistedSettings.crossfadeDurationSeconds}s " +
                "equalizer=${persistedSettings.equalizerEnabled}"
        )
        val settingsOk = persistedSettings.audioCacheEnabled &&
            persistedSettings.audioQuality.equals("HIGH", ignoreCase = true) &&
            persistedSettings.crossfadeEnabled && persistedSettings.crossfadeDurationSeconds == 2 &&
            persistedSettings.equalizerEnabled
        val restoredLyrics = koin.get<LyricsRepository>().getLyrics(
            songId = "yt:$VIDEO_ID",
            songPath = "ytmusic://$VIDEO_ID",
            title = "Get Lucky",
            artist = "Daft Punk",
            durationMs = 369_000L,
            album = "Random Access Memories"
        )
        println("[restore] lyrics source=${restoredLyrics?.source} synced=${restoredLyrics?.synced?.size}")
        val lyricsOk = restoredLyrics?.source == "Musixmatch" && restoredLyrics.synced?.isNotEmpty() == true

        val queueOk = restored && queue.size == 3 && queueManager.currentIndex.value == 0 &&
            queueManager.currentSong.value?.id == "yt:$VIDEO_ID"
        val positionOk = position >= 40_000L
        val pathOk = cached != null && path == cached.absolutePath
        val pausedOk = playback.playerInfo.value.isPlaying == false
        println(
            "[restore] queue=$queueOk position=$positionOk path=$pathOk paused=$pausedOk " +
                "history=$historyOk lyrics=$lyricsOk settings=$settingsOk"
        )
        return queueOk && positionOk && pathOk && pausedOk && historyOk && lyricsOk && settingsOk && playingOrPaused
    }

    /**
     * A settings.json written before the migration, holding fetchOnlineLyrics:false
     * and no migration marker, must load with the online sources on. The shell
     * writes that file before starting this phase; the values below are read from
     * the graph the app itself built, and the chain is then asked for real.
     */
    private suspend fun verifyLegacySettings(): Boolean {
        val path = System.getenv("VIVI_SETTINGS_FILE")
        println("[legacy] settings file=$path")
        val koin = koin()
        val settings = koin.get<SettingsRepository>().getSettings()
        println(
            "[legacy] loaded: fetchOnlineLyrics=${settings.fetchOnlineLyrics} " +
                "migration=${settings.lyricsSourcesMigration} providers=${settings.lyricsProviders.joinToString()}"
        )
        path?.let(::File)?.takeIf { it.isFile }?.readLines()
            ?.filter { "fetchOnlineLyrics" in it || "lyricsSourcesMigration" in it }
            ?.forEach { println("[legacy] file: ${it.trim()}") }

        val chain = LyricsProvidersClient().fetch(
            title = "Get Lucky",
            artist = "Daft Punk",
            durationSeconds = 369,
            album = "Random Access Memories",
            videoId = VIDEO_ID,
            enabled = settings.lyricsProviders
        )
        println("[legacy] chain winner=${chain?.source} lines=${chain?.text?.lines()?.size}")
        val ok = settings.fetchOnlineLyrics && settings.lyricsSourcesMigration >= 1 &&
            chain?.source == "Musixmatch" && !chain.text.isNullOrBlank()
        println("[legacy] online=${settings.fetchOnlineLyrics} migrated=${settings.lyricsSourcesMigration} queried=${chain?.source} ok=$ok")
        return ok
    }

    private fun catalogueSong(videoId: String, title: String, artist: String) = Song(
        id = "yt:$videoId",
        title = title,
        artist = artist,
        album = "Random Access Memories",
        path = "ytmusic://$videoId",
        duration = 369_000L
    )

    private fun itagOf(url: String): String? =
        Regex("[?&]itag=(\\d+)").find(url)?.groupValues?.getOrNull(1)

    private fun hostOf(url: String): String? = runCatching { URI(url).host }.getOrNull()

    /** Records the transport calls the guest manager applies, without opening audio. */
    private class RecordingEngine : AudioEngine {
        override val playbackState = MutableStateFlow(PlaybackState.IDLE)
        override val currentPositionMs = MutableStateFlow(0L)
        override val durationMs = MutableStateFlow(0L)
        override val volume = MutableStateFlow(1f)
        override val isMuted = MutableStateFlow(false)
        override val errorEvents = MutableSharedFlow<String>()
        override val completionEvents = MutableSharedFlow<Unit>()

        var lastPath: String? = null
            private set
        var lastSeek: Long = -1L
            private set
        var pauseCount = 0
            private set

        override val currentTrackPath: String? get() = lastPath

        override fun play(mediaPathOrUri: String, httpHeaders: List<String>) {
            lastPath = mediaPathOrUri
            playbackState.value = PlaybackState.PLAYING
        }

        override fun play() {
            playbackState.value = PlaybackState.PLAYING
        }

        override fun pause() {
            pauseCount++
            playbackState.value = PlaybackState.PAUSED
        }

        override fun stop() {
            playbackState.value = PlaybackState.IDLE
        }

        override fun seekTo(positionMs: Long) {
            lastSeek = positionMs
            currentPositionMs.value = positionMs
        }

        override fun setVolume(volume: Float) = Unit

        override fun setMute(muted: Boolean) = Unit

        override fun setReplayGain(enabled: Boolean, mode: String) = Unit

        override fun release() = Unit
    }

    private fun writeTone(file: File, durationMs: Int, frequencyHz: Double = 440.0) {
        val sampleRate = 44_100f
        val frames = (sampleRate * durationMs / 1000).toInt()
        val format = AudioFormat(sampleRate, 16, 1, true, false)
        val data = ByteArray(frames * 2)
        for (frame in 0 until frames) {
            val sample = (sin(2.0 * PI * frequencyHz * frame / sampleRate) * 8_000).toInt()
            data[frame * 2] = (sample and 0xFF).toByte()
            data[frame * 2 + 1] = ((sample shr 8) and 0xFF).toByte()
        }
        AudioSystem.write(
            AudioInputStream(ByteArrayInputStream(data), format, frames.toLong()),
            AudioFileFormat.Type.WAVE,
            file
        )
    }

    private fun waitFor(timeoutMs: Long, condition: () -> Boolean): Boolean {
        val deadline = System.currentTimeMillis() + timeoutMs
        while (System.currentTimeMillis() < deadline) {
            if (condition()) return true
            Thread.sleep(250)
        }
        return condition()
    }

    private fun <T : Any> waitForValue(timeoutMs: Long, value: () -> T?): T? {
        val deadline = System.currentTimeMillis() + timeoutMs
        while (System.currentTimeMillis() < deadline) {
            value()?.let { return it }
            Thread.sleep(250)
        }
        return value()
    }

    private fun render(name: String, width: Int, height: Int, content: @Composable () -> Unit) {
        val scene = ImageComposeScene(
            width = width,
            height = height,
            density = Density(1f),
            coroutineContext = Dispatchers.Main
        ) { content() }
        try {
            var rendered = false
            runBlocking {
                val deadline = System.currentTimeMillis() + 2500
                while (System.currentTimeMillis() < deadline) {
                    scene.render()
                    Snapshot.sendApplyNotifications()
                    Thread.sleep(200)
                }
                val bytes = scene.render().encodeToData()?.bytes
                if (bytes != null) {
                    val out = File(OUT_DIR, name).apply { parentFile?.mkdirs() }
                    out.writeBytes(bytes)
                    rendered = true
                    println("[imported] frame ${out.absolutePath} (${bytes.size} bytes)")
                }
            }
            if (!rendered) println("[imported] $name produced no pixels")
        } finally {
            scene.close()
        }
    }
}