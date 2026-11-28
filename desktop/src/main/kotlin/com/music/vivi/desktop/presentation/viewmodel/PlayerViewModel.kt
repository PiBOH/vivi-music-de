package com.music.vivi.desktop.presentation.viewmodel

import java.io.File

import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow

import com.music.vivi.desktop.data.metadata.AudioMetadataDetails
import com.music.vivi.desktop.data.metadata.BatchMetadataResult
import com.music.vivi.desktop.data.listentogether.ListenTogetherManager
import com.music.vivi.desktop.data.metadata.BatchSongMetadataUpdate
import com.music.vivi.desktop.data.metadata.EditableSongMetadata
import com.music.vivi.desktop.data.metadata.MetadataEditResult
import com.music.vivi.desktop.data.model.Album
import com.music.vivi.desktop.data.model.Artist
import com.music.vivi.desktop.data.model.EqualizerPreset
import com.music.vivi.desktop.data.model.Lyrics
import com.music.vivi.desktop.data.model.Playlist
import com.music.vivi.desktop.data.model.QuickAccessItem
import com.music.vivi.desktop.data.model.QuickAccessType
import com.music.vivi.desktop.data.model.Song
import com.music.vivi.desktop.data.model.SortOption
import com.music.vivi.desktop.data.model.StorageFilter
import com.music.vivi.desktop.data.network.musicbrainz.OnlineReleaseMetadata
import com.music.vivi.desktop.data.network.musicbrainz.OnlineTrackMetadata
import com.music.vivi.desktop.data.playlist.M3uParseResult
import com.music.vivi.desktop.data.preferences.AlbumArtPaletteStyle
import com.music.vivi.desktop.data.preferences.LyricsSourcePreference
import com.music.vivi.desktop.platform.mediasession.MediaSessionBridge

class PlayerViewModel(
    val playbackStateHolder: PlaybackStateHolder,
    val libraryStateHolder: LibraryStateHolder,
    val themeStateHolder: ThemeStateHolder,
    val statsStateHolder: StatsStateHolder,
    val updateStateHolder: UpdateStateHolder? = null,
    val sleepTimerStateHolder: SleepTimerStateHolder? = null,
    private val mediaSessionBridge: MediaSessionBridge? = null,
    /**
     * YouTube Music page state. The default keeps the view model constructible on
     * its own (tests); the app always injects the Koin instance.
     */
    val youtubeMusicStateHolder: YouTubeMusicStateHolder = YouTubeMusicStateHolder(
        repository = com.music.vivi.desktop.data.youtube.YouTubeMusicRepository(),
        coroutineScope = kotlinx.coroutines.CoroutineScope(
            kotlinx.coroutines.SupervisorJob() + kotlinx.coroutines.Dispatchers.Default
        )
    ),
    /**
     * YouTube Music account state. The default keeps the view model constructible
     * on its own (tests); the app always injects the Koin instance.
     */
    val youtubeAccountStateHolder: YouTubeAccountStateHolder = YouTubeAccountStateHolder(
        coroutineScope = kotlinx.coroutines.CoroutineScope(
            kotlinx.coroutines.SupervisorJob() + kotlinx.coroutines.Dispatchers.Default
        )
    ),
    /** Listen Together: the party screen and the player sync share this manager. */
    val listenTogetherManager: ListenTogetherManager? = null,
    /** The 1.55 device-sync service: one instance for the whole window. */
    val desktopSyncManager: com.music.vivi.desktop.data.sync.DesktopSyncManager? = null
) {
    val isEndOfTrackTimerActive = sleepTimerStateHolder?.isEndOfTrackTimerActive ?: MutableStateFlow(false)
    val activeTimerDurationMinutes = sleepTimerStateHolder?.activeTimerDurationMinutes ?: MutableStateFlow(null)
    val activeTimerValueDisplay = sleepTimerStateHolder?.activeTimerValueDisplay ?: MutableStateFlow(null)
    val sleepTimerRemainingSeconds = sleepTimerStateHolder?.remainingSeconds ?: MutableStateFlow(null)
    val sleepTimerPlayCount = sleepTimerStateHolder?.playCount ?: MutableStateFlow(1f)

    val homeStatsOverview = statsStateHolder.homeOverview
    val currentStatsSummary = statsStateHolder.currentSummary
    val selectedStatsRange = statsStateHolder.selectedRange

    val playerInfo = playbackStateHolder.playerInfo
    val volume = playbackStateHolder.volume
    val currentPositionMs = playbackStateHolder.currentPositionMs
    val currentSong = playbackStateHolder.currentSong
    val currentQueueIndex = playbackStateHolder.currentQueueIndex
    val queue = playbackStateHolder.queue
    val playbackState = playbackStateHolder.playbackState
    val playbackErrorEvents = playbackStateHolder.errorEvents
    val lyricsSearchUiState = playbackStateHolder.lyricsSearchUiState
    val playbackSpeed = playbackStateHolder.playbackSpeed
    val isCurrentSongSpeedSaved = playbackStateHolder.isCurrentSongSpeedSaved
    val savedSongSpeedsCount = playbackStateHolder.savedSongSpeedsCount

    private val _showShortcutsDialog = MutableStateFlow(false)
    val showShortcutsDialog: StateFlow<Boolean> = _showShortcutsDialog.asStateFlow()

    private val _isMiniPlayerMode = MutableStateFlow(false)
    val isMiniPlayerMode: StateFlow<Boolean> = _isMiniPlayerMode.asStateFlow()

    private val _isMiniPlayerAlwaysOnTop = MutableStateFlow(true)
    val isMiniPlayerAlwaysOnTop: StateFlow<Boolean> = _isMiniPlayerAlwaysOnTop.asStateFlow()

    private val _controlActionEvent = MutableSharedFlow<String>(extraBufferCapacity = 64)
    val controlActionEvent: SharedFlow<String> = _controlActionEvent.asSharedFlow()

    fun showShortcutsDialog() {
        _showShortcutsDialog.value = true
    }

    fun hideShortcutsDialog() {
        _showShortcutsDialog.value = false
    }

    fun toggleShortcutsDialog() {
        _showShortcutsDialog.value = !_showShortcutsDialog.value
    }

    fun setMiniPlayerMode(enabled: Boolean) {
        _isMiniPlayerMode.value = enabled
    }

    fun toggleMiniPlayerMode() {
        _isMiniPlayerMode.value = !_isMiniPlayerMode.value
    }

    fun toggleMiniPlayerAlwaysOnTop() {
        _isMiniPlayerAlwaysOnTop.value = !_isMiniPlayerAlwaysOnTop.value
    }

    fun searchLyrics(query: String) = playbackStateHolder.searchLyrics(query)
    fun applyLyrics(lyrics: Lyrics) = playbackStateHolder.applyLyrics(lyrics)
    fun importLyricsFromFile(file: File) = playbackStateHolder.importLyricsFromFile(file)
    fun resetLyricsSearchState() = playbackStateHolder.resetLyricsSearchState()
    fun resetCurrentSongLyrics() = playbackStateHolder.resetCurrentSongLyrics()
    fun saveLyricsBesideCurrentSong(preferSynced: Boolean = true) = playbackStateHolder.saveLyricsBesideCurrentSong(preferSynced)
    fun exportLyricsToFile(targetFile: File, preferSynced: Boolean = true) = playbackStateHolder.exportLyricsToFile(targetFile, preferSynced)
    suspend fun saveLyricsToEmbeddedMetadata(preferSynced: Boolean = true): Boolean =
        playbackStateHolder.saveLyricsToEmbeddedMetadata(preferSynced)
    fun setLyricsSyncOffset(offsetMs: Int) = playbackStateHolder.setLyricsSyncOffset(offsetMs)

    fun playSong(song: Song) = playbackStateHolder.playSong(song)
    fun playSongs(songs: List<Song>, startIndex: Int = 0) = playbackStateHolder.playSongs(songs, startIndex)
    fun shuffleSongs(songs: List<Song>) = playbackStateHolder.shuffleSongs(songs)
    fun togglePlayPause() {
        _controlActionEvent.tryEmit("PLAY_PAUSE")
        playbackStateHolder.togglePlayPause()
    }
    fun playNext() {
        _controlActionEvent.tryEmit("NEXT")
        playbackStateHolder.playNext()
    }
    fun playPrevious() {
        _controlActionEvent.tryEmit("PREVIOUS")
        playbackStateHolder.playPrevious()
    }
    fun seekTo(positionMs: Long) = playbackStateHolder.seekTo(positionMs)
    fun seekRelative(deltaMs: Long) = playbackStateHolder.seekRelative(deltaMs)
    fun seekForward(stepMs: Long = 5000L) = playbackStateHolder.seekRelative(stepMs)
    fun seekBackward(stepMs: Long = 5000L) = playbackStateHolder.seekRelative(-stepMs)
    fun setVolume(vol: Float) = playbackStateHolder.setVolume(vol)
    fun toggleMute() = playbackStateHolder.toggleMute()
    fun toggleShuffle() = playbackStateHolder.toggleShuffle()
    fun cycleRepeatMode() = playbackStateHolder.cycleRepeatMode()
    fun toggleFavoriteCurrentSong() = playbackStateHolder.toggleFavoriteCurrentSong()
    fun stopPlayback() = playbackStateHolder.stopPlayback()
    fun undoStopPlayback() = playbackStateHolder.undoStopPlayback()
    fun removeFromQueue(index: Int) = playbackStateHolder.removeFromQueue(index)
    fun removeSongFromQueue(songId: String) = playbackStateHolder.removeSongFromQueue(songId)
    fun moveQueueItem(fromIndex: Int, toIndex: Int) = playbackStateHolder.moveQueueItem(fromIndex, toIndex)
    fun clearQueue() = playbackStateHolder.clearQueue()
    fun playSongAtIndex(index: Int) = playbackStateHolder.playSongAtIndex(index)
    fun addSongToQueue(song: Song) = playbackStateHolder.addSongToQueue(song)
    fun addSongsToQueue(songs: List<Song>) = playbackStateHolder.addSongsToQueue(songs)
    fun playSongNext(song: Song) = playbackStateHolder.playSongNext(song)
    fun playSongsNext(songs: List<Song>) = playbackStateHolder.playSongsNext(songs)

    val replayGainEnabled = playbackStateHolder.replayGainEnabled
    val replayGainMode = playbackStateHolder.replayGainMode
    val crossfadeEnabled = playbackStateHolder.crossfadeEnabled
    val crossfadeDurationSeconds = playbackStateHolder.crossfadeDurationSeconds
    val audioFadeOnPlayPause = playbackStateHolder.audioFadeOnPlayPause
    val showQueueHistory = playbackStateHolder.showQueueHistory
    val audioQuality = playbackStateHolder.audioQuality
    val audioCacheEnabled = playbackStateHolder.audioCacheEnabled
    val audioCacheLimitMb = playbackStateHolder.audioCacheLimitMb
    val audioCacheSizeBytes = playbackStateHolder.audioCacheSizeBytes
    val listenTogetherServerUrl = playbackStateHolder.listenTogetherServerUrl
    val listenTogetherUsername = playbackStateHolder.listenTogetherUsername
    val listenTogetherPartySize = playbackStateHolder.listenTogetherPartySize
    val listenTogetherSyncVolume = playbackStateHolder.listenTogetherSyncVolume
    val listenTogetherSmartResync = playbackStateHolder.listenTogetherSmartResync
    val deviceSyncViviVolume = playbackStateHolder.deviceSyncViviVolume
    val deviceSyncNativeVolume = playbackStateHolder.deviceSyncNativeVolume
    val deviceSyncAutoResync = playbackStateHolder.deviceSyncAutoResync
    val deviceSyncMode = playbackStateHolder.deviceSyncMode
    val deviceSyncRelayUrl = playbackStateHolder.deviceSyncRelayUrl
    val closeToTray = playbackStateHolder.closeToTray
    val selectedAudioDevice = playbackStateHolder.selectedAudioDevice
    val availableAudioDevices = playbackStateHolder.availableAudioDevices
    val equalizerEnabled = playbackStateHolder.equalizerEnabled
    val equalizerPreset = playbackStateHolder.equalizerPreset
    val equalizerBands = playbackStateHolder.equalizerBands
    val equalizerPreamp = playbackStateHolder.equalizerPreamp
    val customEqualizerPresets = playbackStateHolder.customEqualizerPresets
    val pinnedEqualizerPresets = playbackStateHolder.pinnedEqualizerPresets
    val equalizerViewMode = playbackStateHolder.equalizerViewMode

    fun setReplayGainEnabled(enabled: Boolean) = playbackStateHolder.setReplayGainEnabled(enabled)
    fun setReplayGainMode(mode: String) = playbackStateHolder.setReplayGainMode(mode)
    fun setCrossfadeEnabled(enabled: Boolean) = playbackStateHolder.setCrossfadeEnabled(enabled)
    fun setCrossfadeDurationSeconds(duration: Int) = playbackStateHolder.setCrossfadeDurationSeconds(duration)
    fun setAudioFadeOnPlayPause(enabled: Boolean) = playbackStateHolder.setAudioFadeOnPlayPause(enabled)
    fun setShowQueueHistory(show: Boolean) = playbackStateHolder.setShowQueueHistory(show)
    fun setAudioQuality(quality: String) = playbackStateHolder.setAudioQuality(quality)
    fun setAudioCacheEnabled(enabled: Boolean) = playbackStateHolder.setAudioCacheEnabled(enabled)
    fun setAudioCacheLimitMb(limitMb: Int) = playbackStateHolder.setAudioCacheLimitMb(limitMb)
    fun refreshAudioCacheSize() = playbackStateHolder.refreshAudioCacheSize()
    fun clearAudioCache() = playbackStateHolder.clearAudioCache()
    fun setListenTogetherServerUrl(url: String) = playbackStateHolder.setListenTogetherServerUrl(url)
    fun setListenTogetherUsername(name: String) = playbackStateHolder.setListenTogetherUsername(name)
    fun setListenTogetherPartySize(size: Int) = playbackStateHolder.setListenTogetherPartySize(size)
    fun setListenTogetherSyncVolume(enabled: Boolean) = playbackStateHolder.setListenTogetherSyncVolume(enabled)
    fun setListenTogetherSmartResync(enabled: Boolean) = playbackStateHolder.setListenTogetherSmartResync(enabled)
    fun setDeviceSyncViviVolume(enabled: Boolean) = playbackStateHolder.setDeviceSyncViviVolume(enabled)
    fun setDeviceSyncNativeVolume(enabled: Boolean) = playbackStateHolder.setDeviceSyncNativeVolume(enabled)
    fun setDeviceSyncAutoResync(enabled: Boolean) = playbackStateHolder.setDeviceSyncAutoResync(enabled)
    fun setDeviceSyncMode(mode: String) = playbackStateHolder.setDeviceSyncMode(mode)
    fun setDeviceSyncRelayUrl(url: String) = playbackStateHolder.setDeviceSyncRelayUrl(url)
    fun setCloseToTray(enabled: Boolean) = playbackStateHolder.setCloseToTray(enabled)
    fun setSelectedAudioDevice(device: String) = playbackStateHolder.setSelectedAudioDevice(device)
    fun refreshAudioDevices() = playbackStateHolder.refreshAudioDevices()
    fun setEqualizerEnabled(enabled: Boolean) = playbackStateHolder.setEqualizerEnabled(enabled)
    fun setEqualizerPreset(preset: EqualizerPreset) = playbackStateHolder.setEqualizerPreset(preset)
    fun setEqualizerBand(index: Int, level: Float) = playbackStateHolder.setEqualizerBand(index, level)
    fun setEqualizerPreamp(preamp: Float) = playbackStateHolder.setEqualizerPreamp(preamp)
    fun saveCustomEqualizerPreset(name: String) = playbackStateHolder.saveCustomEqualizerPreset(name)
    fun deleteCustomEqualizerPreset(name: String) = playbackStateHolder.deleteCustomEqualizerPreset(name)
    fun renameCustomEqualizerPreset(oldName: String, newName: String) = playbackStateHolder.renameCustomEqualizerPreset(oldName, newName)
    fun updatePinnedEqualizerPresets(pinnedNames: List<String>) = playbackStateHolder.updatePinnedEqualizerPresets(pinnedNames)
    fun setEqualizerViewMode(mode: String) = playbackStateHolder.setEqualizerViewMode(mode)

    fun shuffleRandomAlbum(availableAlbums: List<Album>? = null) {
        val targetList = availableAlbums ?: albums.value
        if (targetList.isNotEmpty()) {
            val randomAlbum = targetList.random()
            val albumSongs = songs.value.filter { it.album == randomAlbum.title }
            if (albumSongs.isNotEmpty()) {
                playbackStateHolder.shuffleSongs(albumSongs)
            }
        }
    }

    fun shuffleRandomArtist(availableArtists: List<Artist>? = null) {
        val targetList = availableArtists ?: artists.value
        if (targetList.isNotEmpty()) {
            val randomArtist = targetList.random()
            val artistSongs = songs.value.filter { song ->
                song.artistId == randomArtist.id ||
                song.artists.any { it.id == randomArtist.id || it.name.equals(randomArtist.name, ignoreCase = true) } ||
                song.artist.contains(randomArtist.name, ignoreCase = true)
            }
            if (artistSongs.isNotEmpty()) {
                playbackStateHolder.shuffleSongs(artistSongs)
            }
        }
    }

    val storageFilter = libraryStateHolder.storageFilter
    val songs = libraryStateHolder.songs
    val ignoredDuplicateGroups = libraryStateHolder.ignoredDuplicateGroups
    val albums = libraryStateHolder.albums
    val artists = libraryStateHolder.artists
    val playlists = libraryStateHolder.playlists
    val favoriteSongs = libraryStateHolder.favoriteSongs
    val filteredSongs = libraryStateHolder.filteredSongs
    val filteredFavoriteSongs = libraryStateHolder.filteredFavoriteSongs
    val filteredAlbums = libraryStateHolder.filteredAlbums
    val filteredArtists = libraryStateHolder.filteredArtists
    val recentlyPlayedSongs = libraryStateHolder.recentlyPlayedSongs
    val scanProgress = libraryStateHolder.scanProgress
    val searchQuery = libraryStateHolder.searchQuery
    val searchResults = libraryStateHolder.searchResults
    val initialSetupDone = libraryStateHolder.initialSetupDone
    val fetchDeezerArtistImages = libraryStateHolder.fetchDeezerArtistImages
    val fetchOnlineLyrics = libraryStateHolder.fetchOnlineLyrics
    val musicDirectories = libraryStateHolder.musicDirectories
    val excludedDirectories = libraryStateHolder.excludedDirectories
    val folderSortOption = libraryStateHolder.folderSortOption
    val artistDelimiters = libraryStateHolder.artistDelimiters
    val wordDelimiters = libraryStateHolder.wordDelimiters
    val extractArtistFromTitle = libraryStateHolder.extractArtistFromTitle
    val rescanRequired = libraryStateHolder.rescanRequired
    val isAlbumsListView = libraryStateHolder.isAlbumsListView
    val isArtistsListView = libraryStateHolder.isArtistsListView
    val isFoldersPlaylistView = libraryStateHolder.isFoldersPlaylistView
    val minSongDurationMs = libraryStateHolder.minSongDurationMs
    val minTracksPerAlbum = libraryStateHolder.minTracksPerAlbum
    val albumArtCacheLimitMb = libraryStateHolder.albumArtCacheLimitMb
    val autoScanLrcFiles = libraryStateHolder.autoScanLrcFiles
    val lyricsSourcePreference = libraryStateHolder.lyricsSourcePreference
    val lyricsProviders = libraryStateHolder.lyricsProviders

    fun setMinSongDuration(durationMs: Int) = libraryStateHolder.setMinSongDuration(durationMs)
    fun setMinTracksPerAlbum(minTracks: Int) = libraryStateHolder.setMinTracksPerAlbum(minTracks)
    fun setAlbumArtCacheLimitMb(limitMb: Int) = libraryStateHolder.setAlbumArtCacheLimitMb(limitMb)
    fun setAutoScanLrcFiles(enabled: Boolean) = libraryStateHolder.setAutoScanLrcFiles(enabled)
    fun setLyricsSourcePreference(preference: LyricsSourcePreference) = libraryStateHolder.setLyricsSourcePreference(preference)
    fun setLyricsProviders(providers: List<String>) = libraryStateHolder.setLyricsProviders(providers)
    fun resetAllLyrics() = libraryStateHolder.resetAllLyrics()

    fun setStorageFilter(filter: StorageFilter) = libraryStateHolder.setStorageFilter(filter)
    fun toggleStorageFilter() = libraryStateHolder.toggleStorageFilter()

    fun setSongSortOption(sort: SortOption) = libraryStateHolder.setSongSortOption(sort)
    fun setAlbumSortOption(sort: SortOption) = libraryStateHolder.setAlbumSortOption(sort)
    fun setArtistSortOption(sort: SortOption) = libraryStateHolder.setArtistSortOption(sort)
    fun setPlaylistSortOption(sort: SortOption) = libraryStateHolder.setPlaylistSortOption(sort)
    fun setFolderSortOption(sort: SortOption) = libraryStateHolder.setFolderSortOption(sort)
    fun setAlbumsListView(isListView: Boolean) = libraryStateHolder.setAlbumsListView(isListView)
    fun setArtistsListView(isListView: Boolean) = libraryStateHolder.setArtistsListView(isListView)
    fun setFoldersPlaylistView(isPlaylistView: Boolean) = libraryStateHolder.setFoldersPlaylistView(isPlaylistView)
    fun setInitialSetupDone(done: Boolean) = libraryStateHolder.setInitialSetupDone(done)
    fun setFetchDeezerArtistImages(enabled: Boolean) = libraryStateHolder.setFetchDeezerArtistImages(enabled)
    fun toggleFetchDeezerArtistImages() = libraryStateHolder.toggleFetchDeezerArtistImages()
    fun setFetchOnlineLyrics(enabled: Boolean) = libraryStateHolder.setFetchOnlineLyrics(enabled)
    fun toggleFetchOnlineLyrics() = libraryStateHolder.toggleFetchOnlineLyrics()
    fun scanLibrary(directories: List<String>? = null, forceRescan: Boolean = false) = libraryStateHolder.scanLibrary(directories, forceRescan)
    fun rescanLibrary() = libraryStateHolder.scanLibrary(forceRescan = true)
    fun rebuildDatabase() = libraryStateHolder.rebuildDatabase()
    fun addMusicDirectory(path: String) = libraryStateHolder.addMusicDirectory(path)
    fun removeMusicDirectory(path: String) = libraryStateHolder.removeMusicDirectory(path)
    fun addExcludedDirectory(path: String) = libraryStateHolder.addExcludedDirectory(path)
    fun removeExcludedDirectory(path: String) = libraryStateHolder.removeExcludedDirectory(path)
    fun addArtistDelimiter(delimiter: String) = libraryStateHolder.addArtistDelimiter(delimiter)
    fun removeArtistDelimiter(delimiter: String) = libraryStateHolder.removeArtistDelimiter(delimiter)
    fun resetArtistDelimiters() = libraryStateHolder.resetArtistDelimiters()
    fun addWordDelimiter(delimiter: String) = libraryStateHolder.addWordDelimiter(delimiter)
    fun removeWordDelimiter(delimiter: String) = libraryStateHolder.removeWordDelimiter(delimiter)
    fun resetWordDelimiters() = libraryStateHolder.resetWordDelimiters()
    fun setExtractArtistFromTitle(enabled: Boolean) = libraryStateHolder.setExtractArtistFromTitle(enabled)
    fun onSearchQueryChanged(query: String) = libraryStateHolder.onSearchQueryChanged(query)
    suspend fun setCustomArtistImage(artistId: Long, sourceFile: File): String? = libraryStateHolder.setCustomArtistImage(artistId, sourceFile)
    suspend fun clearCustomArtistImage(artistId: Long) = libraryStateHolder.clearCustomArtistImage(artistId)
    fun createPlaylist(
        name: String,
        songIds: List<String> = emptyList(),
        coverImageUri: String? = null,
        coverColorArgb: Int? = null,
        coverIconName: String? = null,
        coverShapeType: String? = null
    ) = libraryStateHolder.createPlaylist(name, songIds, coverImageUri, coverColorArgb, coverIconName, coverShapeType)
    suspend fun savePlaylistCoverImage(sourceFile: File): String? = libraryStateHolder.savePlaylistCoverImage(sourceFile)
    fun deletePlaylist(id: String) = libraryStateHolder.deletePlaylist(id)
    fun deleteSong(song: Song) {
        playbackStateHolder.removeSongFromQueue(song.id)
        libraryStateHolder.deleteSong(song)
    }
    fun ignoreDuplicateGroup(key: String) = libraryStateHolder.ignoreDuplicateGroup(key)
    fun restoreIgnoredDuplicateGroups() = libraryStateHolder.restoreIgnoredDuplicateGroups()
    fun renamePlaylist(id: String, newName: String) = libraryStateHolder.renamePlaylist(id, newName)
    fun updatePlaylist(playlist: Playlist) = libraryStateHolder.updatePlaylist(playlist)
    fun addSongToPlaylist(playlistId: String, songId: String) = libraryStateHolder.addSongToPlaylist(playlistId, songId)
    fun addSongsToPlaylist(playlistId: String, songIds: List<String>) = libraryStateHolder.addSongsToPlaylist(playlistId, songIds)
    fun removeSongFromPlaylist(playlistId: String, songId: String) = libraryStateHolder.removeSongFromPlaylist(playlistId, songId)
    fun reorderPlaylistSongs(playlistId: String, songIds: List<String>) = libraryStateHolder.reorderPlaylistSongs(playlistId, songIds)
    fun importPlaylistFromM3u(file: File, onComplete: ((Result<Pair<Playlist, M3uParseResult>>) -> Unit)? = null) =
        libraryStateHolder.importPlaylistFromM3u(file, onComplete)
    fun exportPlaylistToM3u(playlistId: String, targetFile: File, onComplete: ((Result<Unit>) -> Unit)? = null) =
        libraryStateHolder.exportPlaylistToM3u(playlistId, targetFile, onComplete)
    fun toggleFavorite(song: Song) = playbackStateHolder.toggleFavorite(song)
    val quickAccessItems = libraryStateHolder.quickAccessItems
    val showQuickAccessLimitDialog = libraryStateHolder.showQuickAccessLimitDialog
    fun dismissQuickAccessLimitDialog() = libraryStateHolder.dismissQuickAccessLimitDialog()
    fun pinToQuickAccess(item: QuickAccessItem, onResult: ((Boolean) -> Unit)? = null) =
        libraryStateHolder.pinToQuickAccess(item, onResult)
    fun unpinFromQuickAccess(id: String) = libraryStateHolder.unpinFromQuickAccess(id)
    fun setQuickAccessItems(items: List<QuickAccessItem>) = libraryStateHolder.setQuickAccessItems(items)
    fun togglePin(
        id: String,
        title: String,
        type: QuickAccessType,
        targetKey: String,
        isPinned: Boolean,
        onResult: ((Boolean) -> Unit)? = null
    ) = libraryStateHolder.togglePin(id, title, type, targetKey, isPinned, onResult)

    val userSettings = themeStateHolder.userSettings
    val isDarkMode = themeStateHolder.isDarkMode
    val uiScale = themeStateHolder.uiScale
    val isDynamicColorEnabled = themeStateHolder.isDynamicColorEnabled
    val albumArtPaletteStyle = themeStateHolder.albumArtPaletteStyle
    val albumArtColorAccuracy = themeStateHolder.albumArtColorAccuracy
    val showPlayerFileInfo = themeStateHolder.showPlayerFileInfo
    val navBarCompactMode = themeStateHolder.navBarCompactMode
    val showIntroSplash = themeStateHolder.showIntroSplash
    val introStyle = themeStateHolder.introStyle
    val introBackground = themeStateHolder.introBackground
    val defaultTab = themeStateHolder.defaultTab
    fun setLegacySetting(key: String, value: String) = themeStateHolder.setLegacySetting(key, value)
    fun setDarkMode(dark: Boolean) = themeStateHolder.setDarkMode(dark)
    fun setDynamicColorEnabled(enabled: Boolean) = themeStateHolder.setDynamicColorEnabled(enabled)
    fun toggleDarkMode() = themeStateHolder.toggleDarkMode()
    fun setUiScale(scale: Float) = themeStateHolder.setUiScale(scale)
    fun adjustUiScale(delta: Float) = themeStateHolder.adjustUiScale(delta)
    fun resetUiScale() = themeStateHolder.resetUiScale()
    fun toggleDynamicColor() = themeStateHolder.toggleDynamicColor()
    fun setAlbumArtPaletteStyle(style: AlbumArtPaletteStyle) = themeStateHolder.setAlbumArtPaletteStyle(style)
    fun setAlbumArtColorAccuracy(accuracy: Int) = themeStateHolder.setAlbumArtColorAccuracy(accuracy)
    fun setShowPlayerFileInfo(show: Boolean) = themeStateHolder.setShowPlayerFileInfo(show)
    fun setShowIntroSplash(show: Boolean) = themeStateHolder.setShowIntroSplash(show)
    fun setIntroStyle(style: String) = themeStateHolder.setIntroStyle(style)
    fun setIntroBackground(background: String) = themeStateHolder.setIntroBackground(background)
    fun toggleShowPlayerFileInfo() = themeStateHolder.toggleShowPlayerFileInfo()
    fun setNavBarCompactMode(enabled: Boolean) = themeStateHolder.setNavBarCompactMode(enabled)
    fun toggleNavBarCompactMode() = themeStateHolder.toggleNavBarCompactMode()
    fun setDefaultTab(tab: String) = themeStateHolder.setDefaultTab(tab)
    val lyricsAlignment = themeStateHolder.lyricsAlignment
    fun setLyricsAlignment(alignment: String) = themeStateHolder.setLyricsAlignment(alignment)
    val lyricsTextSize = themeStateHolder.lyricsTextSize
    fun setLyricsTextSize(size: String) = themeStateHolder.setLyricsTextSize(size)
    val showLyricsRomanization = themeStateHolder.showLyricsRomanization
    fun setShowLyricsRomanization(enabled: Boolean) = themeStateHolder.setShowLyricsRomanization(enabled)

    suspend fun updateSongMetadata(
        song: Song,
        metadata: EditableSongMetadata
    ): MetadataEditResult {
        val result = libraryStateHolder.updateSongMetadata(song, metadata)
        if (result.success && result.updatedSong != null) {
            playbackStateHolder.onSongMetadataUpdated(result.updatedSong)
        }
        return result
    }

    suspend fun readSongMetadataDetails(file: File): Result<AudioMetadataDetails> {
        return libraryStateHolder.readSongMetadataDetails(file)
    }

    suspend fun batchUpdateMetadata(
        songs: List<Song>,
        update: BatchSongMetadataUpdate,
        onProgress: (completed: Int, total: Int) -> Unit = { _, _ -> }
    ): BatchMetadataResult {
        val result = libraryStateHolder.batchUpdateMetadata(songs, update, onProgress)
        if (result.updatedSongs.isNotEmpty()) {
            playbackStateHolder.onSongsMetadataUpdated(result.updatedSongs)
        }
        return result
    }

    suspend fun searchOnlineMetadata(title: String, artist: String): List<OnlineTrackMetadata> {
        return libraryStateHolder.searchOnlineMetadata(title, artist)
    }

    suspend fun searchOnlineMetadataByQuery(query: String): List<OnlineTrackMetadata> {
        return libraryStateHolder.searchOnlineMetadataByQuery(query)
    }

    suspend fun searchOnlineReleases(query: String): List<OnlineReleaseMetadata> {
        return libraryStateHolder.searchOnlineReleases(query)
    }

    suspend fun fetchOnlineCoverArt(releaseId: String): Result<ByteArray?> {
        return libraryStateHolder.fetchOnlineCoverArt(releaseId)
    }

    fun setPlaybackSpeed(speed: Float, persist: Boolean = true) = playbackStateHolder.setPlaybackSpeed(speed, persist)
    fun setRememberSpeedForCurrentSong(remember: Boolean) = playbackStateHolder.setRememberSpeedForCurrentSong(remember)
    fun resetPlaybackSpeed() = playbackStateHolder.resetPlaybackSpeed()
    fun clearAllSavedSongSpeeds() = playbackStateHolder.clearAllSavedSongSpeeds()

    fun setSleepTimer(minutes: Int) = sleepTimerStateHolder?.setSleepTimer(minutes)
    fun setEndOfTrackTimer(enabled: Boolean) = sleepTimerStateHolder?.setEndOfTrackTimer(enabled)
    fun playCounted(count: Int) = sleepTimerStateHolder?.playCounted(count)
    fun stopCountedPlay() = sleepTimerStateHolder?.stopCountedPlay()
    fun cancelSleepTimer() = sleepTimerStateHolder?.cancelTimer()
    fun cancelAllSleepTimers() = sleepTimerStateHolder?.cancelAll()
}
