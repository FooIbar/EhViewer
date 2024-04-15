package com.hippo.ehviewer.dao

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "DOWNLOAD_ARTISTS",
    indices = [Index("GID", "ARTIST")],
    foreignKeys = [
        ForeignKey(DownloadEntity::class, ["GID"], ["GID"], onDelete = ForeignKey.CASCADE),
    ],
)
data class DownloadArtist(
    @ColumnInfo(name = "GID")
    val gid: Long,

    @ColumnInfo(name = "ARTIST")
    val artist: String,

    @PrimaryKey
    @ColumnInfo(name = "_id")
    var id: Long? = null,
) {
    companion object {
        fun from(gid: Long, artists: Iterable<String>?) = artists?.map { DownloadArtist(gid, it) }
    }
}
