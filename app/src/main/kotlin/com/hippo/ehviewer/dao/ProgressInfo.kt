package com.hippo.ehviewer.dao

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.PrimaryKey
import com.hippo.ehviewer.client.data.BaseGalleryInfo

@Entity(
    tableName = "PROGRESS",
    foreignKeys = [
        ForeignKey(BaseGalleryInfo::class, ["GID"], ["GID"], onDelete = ForeignKey.CASCADE),
    ],
)
class ProgressInfo(
    @PrimaryKey
    @ColumnInfo(name = "GID")
    val gid: Long,

    @ColumnInfo(name = "PAGE")
    val page: Int,
)
