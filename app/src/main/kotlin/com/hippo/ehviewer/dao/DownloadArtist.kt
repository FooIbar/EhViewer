package com.hippo.ehviewer.dao

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey

@Entity(
    tableName = "DOWNLOAD_ARTISTS",
    primaryKeys = ["GID", "ARTIST"],
    foreignKeys = [
        ForeignKey(DownloadEntity::class, ["GID"], ["GID"], onDelete = ForeignKey.CASCADE),
    ],
)
data class DownloadArtist(
    @ColumnInfo(name = "GID")
    val gid: Long,

    @ColumnInfo(name = "ARTIST")
    val artist: String,
) {
    companion object {
        fun from(gid: Long, artists: Iterable<String>) = artists.map { DownloadArtist(gid, it) }
    }
}
