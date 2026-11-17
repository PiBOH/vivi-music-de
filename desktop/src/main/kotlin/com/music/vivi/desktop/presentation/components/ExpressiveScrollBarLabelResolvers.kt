package com.music.vivi.desktop.presentation.components

import com.music.vivi.desktop.data.model.Album
import com.music.vivi.desktop.data.model.Artist
import com.music.vivi.desktop.data.model.MusicFolder
import com.music.vivi.desktop.data.model.Playlist
import com.music.vivi.desktop.data.model.Song
import com.music.vivi.desktop.data.model.SortOption

internal fun songFastScrollLabel(song: Song?, sortOption: SortOption): String? =
    when (sortOption) {
        SortOption.SongTitleAZ,
        SortOption.SongTitleZA -> extractFastScrollGlyph(song?.title)

        SortOption.SongArtist,
        SortOption.SongArtistDesc -> extractFastScrollGlyph(song?.displayArtist)

        SortOption.SongAlbum,
        SortOption.SongAlbumDesc -> extractFastScrollGlyph(song?.album)

        else -> extractFastScrollGlyph(song?.title)
    }

internal fun albumFastScrollLabel(album: Album?, sortOption: SortOption): String? =
    when (sortOption) {
        SortOption.AlbumTitleAZ,
        SortOption.AlbumTitleZA -> extractFastScrollGlyph(album?.title)

        SortOption.AlbumArtist,
        SortOption.AlbumArtistDesc -> extractFastScrollGlyph(album?.artist)

        else -> extractFastScrollGlyph(album?.title)
    }

internal fun artistFastScrollLabel(artist: Artist?, sortOption: SortOption): String? =
    when (sortOption) {
        SortOption.ArtistNameAZ,
        SortOption.ArtistNameZA -> extractFastScrollGlyph(artist?.name)
        else -> extractFastScrollGlyph(artist?.name)
    }

internal fun playlistFastScrollLabel(playlist: Playlist?, sortOption: SortOption?): String? =
    when (sortOption) {
        SortOption.PlaylistNameAZ,
        SortOption.PlaylistNameZA -> extractFastScrollGlyph(playlist?.name)
        else -> extractFastScrollGlyph(playlist?.name)
    }

internal fun folderFastScrollLabel(folder: MusicFolder?, sortOption: SortOption?): String? =
    when (sortOption) {
        SortOption.FolderNameAZ,
        SortOption.FolderNameZA -> extractFastScrollGlyph(folder?.name)
        else -> extractFastScrollGlyph(folder?.name)
    }
