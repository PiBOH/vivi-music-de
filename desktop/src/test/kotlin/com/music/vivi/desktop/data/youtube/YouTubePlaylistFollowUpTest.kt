package com.music.vivi.desktop.data.youtube

import com.music.vivi.desktop.data.store.LocalLibraryState
import com.music.vivi.desktop.data.store.LocalLibraryStore
import com.music.vivi.desktop.data.store.StoredPlaylistSnapshot
import com.music.vivi.desktop.data.store.StoredPlaylistUndo
import java.io.File
import java.nio.file.Files
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlinx.coroutines.runBlocking

class YouTubePlaylistFollowUpTest {

    private val directory: File = Files.createTempDirectory("vivi-playlist-follow-up").toFile()
    private val storeFile = File(directory, "device-sync.json")
    private val store = LocalLibraryStore(storeFile, File(directory, "vivimusicde.db"))

    @AfterTest
    fun tearDown() {
        directory.deleteRecursively()
    }

    private fun playlist(
        id: String,
        title: String = "Playlist $id",
        editable: Boolean = true
    ) = RemotePlaylist(
        id = id,
        title = title,
        songCountText = null,
        thumbnail = null,
        isEditable = editable
    )

    @Test
    fun eachAccountKeepsItsOwnOrder() = runBlocking {
        val first = YouTubePlaylistsRepository(store = store, accountId = { "account-a" })
        val second = YouTubePlaylistsRepository(store = store, accountId = { "account-b" })

        first.saveOrder(listOf("b", "a"))
        second.saveOrder(listOf("x", "y"))

        assertEquals(listOf("b", "a"), storedPlaylistOrder(store.load(), "account-a"))
        assertEquals(listOf("x", "y"), storedPlaylistOrder(store.load(), "account-b"))
        assertEquals(emptyList(), storedPlaylistOrder(store.load(), "account-c"))
    }

    @Test
    fun theOldSingleOrderIsStillReadUntilTheMapIsWritten() {
        store.save(LocalLibraryState(youTubePlaylistOrder = listOf("old")))

        assertEquals(listOf("old"), storedPlaylistOrder(store.load(), "account-a"))

        store.save(
            store.load().copy(youTubePlaylistOrders = mapOf("account-b" to listOf("new")))
        )

        assertEquals(emptyList(), storedPlaylistOrder(store.load(), "account-a"))
        assertEquals(listOf("new"), storedPlaylistOrder(store.load(), "account-b"))
    }

    @Test
    fun undoSurvivesARestartAndBelongsToOneAccount() = runBlocking {
        val accountA = YouTubePlaylistsRepository(store = store, accountId = { "account-a" })
        accountA.rememberUndo(
            StoredPlaylistUndo(
                kind = StoredPlaylistUndo.DELETED,
                snapshots = listOf(StoredPlaylistSnapshot("Road Trip", listOf("v1", "v2"))),
                message = "Deleted 1 playlist."
            )
        )

        val reopened = LocalLibraryStore(storeFile, File(directory, "vivimusicde.db"))
        val sameAccount = YouTubePlaylistsRepository(store = reopened, accountId = { "account-a" })
        val otherAccount = YouTubePlaylistsRepository(store = reopened, accountId = { "account-b" })

        val pending = sameAccount.pendingUndo()
        assertEquals("Road Trip", pending?.snapshots?.single()?.title)
        assertEquals(listOf("v1", "v2"), pending?.snapshots?.single()?.videoIds)
        assertEquals("Deleted 1 playlist.", pending?.message)
        assertEquals("account-a", pending?.accountId)
        assertNull(otherAccount.pendingUndo())

        sameAccount.clearUndo()
        assertNull(sameAccount.pendingUndo())
    }

    @Test
    fun aPackRoundTripsAndRefusesAnotherFormat() {
        val pack = AccountPlaylistPack(
            exportedAt = 1L,
            source = "account-a",
            playlists = listOf(
                AccountPlaylistSnapshot("Road Trip", listOf("v1", "v2")),
                AccountPlaylistSnapshot("Empty", emptyList())
            )
        )

        val decoded = AccountPlaylistPacks.decode(AccountPlaylistPacks.encode(pack)).getOrThrow()

        assertEquals(pack.playlists, decoded.playlists)
        assertEquals(pack.source, decoded.source)
        assertTrue(
            AccountPlaylistPacks.decode(
                """{"format":"something-else","version":1,"playlists":[]}"""
            ).isFailure
        )
        assertTrue(
            AccountPlaylistPacks.decode(
                """{"format":"vivi-account-playlists","version":99,"playlists":[]}"""
            ).isFailure
        )
    }

    @Test
    fun anImportedTitleNeverReusesAnExistingName() {
        assertEquals("Road Trip", uniquePlaylistTitle("Road Trip", listOf("Other")))
        assertEquals("Road Trip (imported)", uniquePlaylistTitle("Road Trip", listOf("road trip")))
        assertEquals(
            "Road Trip (imported 2)",
            uniquePlaylistTitle("Road Trip", listOf("Road Trip", "Road Trip (imported)"))
        )
        assertEquals("Imported playlist", uniquePlaylistTitle("   ", emptyList()))
    }

    @Test
    fun healthReportsReadOnlyDuplicatesEmptiesAndRepeatedTracks() {
        val playlists = listOf(
            playlist("a", "Mix"),
            playlist("b", "mix"),
            playlist("c", "Saved", editable = false),
            playlist("d", "Empty")
        )
        val tracks = mapOf(
            "a" to listOf(PlaylistTrack("v1", "s1"), PlaylistTrack("v1", "s2")),
            "b" to listOf(PlaylistTrack("v2", "s3")),
            "c" to listOf(PlaylistTrack("v3", "s4")),
            "d" to emptyList()
        )

        val health = accountPlaylistHealth(playlists, tracks)

        assertEquals(4, health.playlistCount)
        assertEquals(3, health.editableCount)
        assertEquals(1, health.readOnlyCount)
        assertEquals(4, health.trackCount)
        val kinds = health.issues.map { it.kind }
        assertTrue(AccountPlaylistIssueKind.READ_ONLY in kinds)
        assertTrue(AccountPlaylistIssueKind.DUPLICATE_TITLE in kinds)
        assertTrue(AccountPlaylistIssueKind.EMPTY in kinds)
        assertTrue(AccountPlaylistIssueKind.DUPLICATE_TRACKS in kinds)
    }

    @Test
    fun anUnreadablePlaylistIsNotReportedAsEmpty() {
        val health = accountPlaylistHealth(
            playlists = listOf(playlist("a", "Mix")),
            tracksByPlaylist = emptyMap(),
            unreadable = setOf("a")
        )

        assertEquals(
            listOf(AccountPlaylistIssueKind.UNREADABLE),
            health.issues.map { it.kind }
        )
    }
}
