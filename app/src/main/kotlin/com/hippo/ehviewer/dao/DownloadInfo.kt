package com.hippo.ehviewer.dao

import androidx.room.ColumnInfo
import androidx.room.Embedded
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Ignore
import androidx.room.PrimaryKey
import androidx.room.Relation
import com.hippo.ehviewer.client.data.AbstractDownloadInfo
import com.hippo.ehviewer.client.data.BaseGalleryInfo
import com.hippo.ehviewer.client.data.GalleryInfo
import kotlinx.datetime.Clock

@Entity(
    tableName = "DOWNLOADS",
    foreignKeys = [
        ForeignKey(BaseGalleryInfo::class, ["GID"], ["GID"]),
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
    var gid: Long,

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
) : AbstractDownloadInfo {
    constructor() : this(0)
}

data class DownloadInfo(
    @Relation(parentColumn = "GID", entityColumn = "GID")
    val galleryInfo: BaseGalleryInfo,

    @ColumnInfo(name = "DIRNAME")
    val dirname: String?,

    @Relation(parentColumn = "GID", entityColumn = "GID")
    var artistInfoList: List<DownloadArtist> = emptyList(),

    @Embedded
    val downloadInfo: DownloadEntity = DownloadEntity(galleryInfo.gid),
) : GalleryInfo by galleryInfo,
    AbstractDownloadInfo by downloadInfo {

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
