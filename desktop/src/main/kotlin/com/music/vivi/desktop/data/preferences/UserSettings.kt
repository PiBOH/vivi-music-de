package com.music.vivi.desktop.data.preferences

import kotlinx.serialization.Serializable

import com.music.vivi.desktop.data.model.EqualizerPreset
import com.music.vivi.desktop.data.model.QuickAccessItem

@Serializable
data class UserSettings(
    // A fresh install opens the app directly: the YouTube Music catalogue works
    // without a music folder, and the local library is added from Settings.
    val initialSetupDone: Boolean = true,
    val musicDirectories: List<String> = emptyList(),
    val isDynamicColorEnabled: Boolean = true,
    val isDarkMode: Boolean = true,
    val uiScale: Float = 1.0f,
    val lastKnownAccentColor: Int? = null,
    val albumArtPaletteStyle: String = "tonal_spot",
    val albumArtColorAccuracy: Int = 0,
    val volume: Float = 1.0f,
    val isMuted: Boolean = false,
    val isShuffleEnabled: Boolean = false,
    val repeatMode: Int = 0, // 0: OFF, 1: ALL, 2: ONE
    val fetchDeezerArtistImages: Boolean = false,
    // The imported mobile chain fetches online lyrics by default; the switch in
    // Settings still turns it off and the sources below decide who is asked.
    val fetchOnlineLyrics: Boolean = true,
    // Online lyrics sources, in the mobile app's priority order. Musixmatch is
    // included with the owner's authorization.
    val lyricsProviders: List<String> = listOf(
        "Musixmatch",
        "YouLyPlus",
        "Paxsenix",
        "BetterLyrics",
        "LrcLib",
        "Kugou",
        "Unison",
        "SimpMusic"
    ),
    // 1 = the imported online sources were applied once. Profiles written before
    // 1.56 can carry fetchOnlineLyrics=false; loadSettingsFromDisk turns them on
    // the first time it sees this marker missing, then leaves later choices alone.
    val lyricsSourcesMigration: Int = 0,
    val excludedDirectories: List<String> = emptyList(),
    val artistDelimiters: List<String> = listOf("/", ";", ",", "+", "&"),
    val wordDelimiters: List<String> = listOf("featuring", "feat.", "feat", "ft.", "ft", "vs.", "vs", "versus", "with", "prod.", "prod"),
    val extractArtistFromTitle: Boolean = true,
    val artistSettingsRescanRequired: Boolean = false,
    val isAlbumsListView: Boolean = false,
    val isArtistsListView: Boolean = false,
    val isFoldersPlaylistView: Boolean = false,
    val minSongDurationMs: Int = 10000,
    val minTracksPerAlbum: Int = 1,
    val albumArtCacheLimitMb: Int = 200,
    val autoScanLrcFiles: Boolean = false,
    val lyricsSourcePreference: String = "embedded_first",
    val showPlayerFileInfo: Boolean = true,
    val navBarCompactMode: Boolean = false,
    val defaultTab: String = "HOME",
    val lyricsAlignment: String = "left",
    val lyricsTextSize: String = "medium",
    val lyricsSyncOffsets: Map<String, Int> = emptyMap(),
    val songPlaybackSpeeds: Map<String, Float> = emptyMap(),
    val showLyricsRomanization: Boolean = true,
    val replayGainEnabled: Boolean = false,
    val replayGainMode: String = "track",
    val crossfadeEnabled: Boolean = false,
    val crossfadeDurationSeconds: Int = 3,
    val audioFadeOnPlayPause: Boolean = true,
    val showQueueHistory: Boolean = true,
    // Catalogue audio quality: "auto" keeps the best available format,
    // "high" prefers AAC 256 kbps and "low" prefers AAC 128 kbps.
    val audioQuality: String = "auto",
    // Keep played catalogue tracks on disk so the next play is local.
    val audioCacheEnabled: Boolean = true,
    val audioCacheLimitMb: Int = 1024,
    // Listen Together: the relay, the name last used and the room cap the host
    // enforces when a join request arrives.
    val listenTogetherServerUrl: String = "wss://devilmi-vivi-music-listen-together.hf.space",
    val listenTogetherUsername: String = "",
    val listenTogetherPartySize: Int = 5,
    val listenTogetherSyncVolume: Boolean = true,
    val listenTogetherSmartResync: Boolean = true,
    val closeToTray: Boolean = false,
    val selectedAudioDevice: String = "auto",
    val equalizerEnabled: Boolean = false,
    val equalizerPreset: String = "flat",
    val equalizerBands: List<Float> = listOf(0f, 0f, 0f, 0f, 0f, 0f, 0f, 0f, 0f, 0f),
    val equalizerPreamp: Float = 0f,
    val customEqualizerPresets: List<EqualizerPreset> = emptyList(),
    val pinnedEqualizerPresets: List<String> = EqualizerPreset.DEFAULT_PINNED,
    val equalizerViewMode: String = "SLIDERS",
    val listenBrainzToken: String? = null,
    val listenBrainzUserName: String? = null,
    val listenBrainzServerUrl: String? = null,
    val listenBrainzEnabled: Boolean = false,
    val listenBrainzScrobbleLocal: Boolean = true,
    val listenBrainzScrobbleNavidrome: Boolean = true,
    val listenBrainzScrobbleJellyfin: Boolean = true,
    val navidromeServerUrl: String? = null,
    val navidromeUsername: String? = null,
    val navidromePassword: String? = null,
    val navidromeAuthMethod: String = "TOKEN",
    val navidromeEnabled: Boolean = false,
    val jellyfinServerUrl: String? = null,
    val jellyfinUsername: String? = null,
    val jellyfinAccessToken: String? = null,
    val jellyfinUserId: String? = null,
    val jellyfinEnabled: Boolean = false,
    val autoBackupEnabled: Boolean = true,
    val backupBeforeUpdate: Boolean = true,
    val lastAutoBackupAt: Long = 0L,
    val checkUpdatesOnStartup: Boolean = true,
    val lastDismissedUpdateVersion: String? = null,
    val quickAccessItems: List<QuickAccessItem> = QuickAccessItem.DEFAULT_ITEMS
)
