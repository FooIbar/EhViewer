package com.ehviewer.core.database.model

import androidx.room.ColumnInfo
import androidx.room.Embedded
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Ignore
import androidx.room.PrimaryKey
import androidx.room.Relation
import com.ehviewer.core.model.GalleryInfo
import kotlin.time.Clock

interface AbstractDownloadInfo {
    var state: Int
    var legacy: Int
    var time: Long
    var label: String?
    var speed: Long
    var remaining: Long
    var finished: Int
    var downloaded: Int
    var total: Int
}

@Entity(
    tableName = "DOWNLOADS",
    foreignKeys = [
        ForeignKey(GalleryEntity::class, ["GID"], ["GID"]),
        ForeignKey(
            DownloadLabel::class,
            ["LABEL"],
            ["LABEL"],
            onDelete = ForeignKey.SET_NULL,
            onUpdate = ForeignKey.CASCADE,
        ),
    ],
)
data class DownloadEntity(
    @PrimaryKey
    @ColumnInfo(name = "GID")
    var gid: Long = 0,

    @ColumnInfo(name = "STATE")
    override var state: Int = 0,

    @ColumnInfo(name = "LEGACY")
    override var legacy: Int = 0,

    @ColumnInfo(name = "TIME")
    override var time: Long = Clock.System.now().toEpochMilliseconds(),

    @ColumnInfo(name = "LABEL", index = true)
    override var label: String? = null,

    @Ignore
    override var speed: Long = 0,

    @Ignore
    override var remaining: Long = 0,

    @Ignore
    override var finished: Int = 0,

    @Ignore
    override var downloaded: Int = 0,

    @Ignore
    override var total: Int = 0,
) : AbstractDownloadInfo

data class DownloadInfo(
    @Relation(parentColumn = "GID", entityColumn = "GID")
    val galleryInfo: GalleryEntity,

    @ColumnInfo(name = "DIRNAME")
    val dirname: String?,

    @Relation(parentColumn = "GID", entityColumn = "GID")
    var artistInfoList: List<DownloadArtist> = emptyList(),

    @Embedded
    val downloadInfo: DownloadEntity = DownloadEntity(galleryInfo.gid),
) : GalleryInfo by galleryInfo, AbstractDownloadInfo by downloadInfo {

    companion object {
        const val STATE_INVALID = -1
        const val STATE_NONE = 0
        const val STATE_WAIT = 1
        const val STATE_DOWNLOAD = 2
        const val STATE_FINISH = 3
        const val STATE_FAILED = 4
    }
}

val DownloadInfo.artists: List<String>
    get() = artistInfoList.map { it.artist }
