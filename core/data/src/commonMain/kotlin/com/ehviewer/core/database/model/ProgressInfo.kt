package com.ehviewer.core.database.model

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.PrimaryKey

@Entity(
    tableName = "PROGRESS",
    foreignKeys = [
        ForeignKey(GalleryEntity::class, ["GID"], ["GID"], onDelete = ForeignKey.CASCADE),
    ],
)
class ProgressInfo(
    @PrimaryKey
    @ColumnInfo(name = "GID")
    val gid: Long,

    @ColumnInfo(name = "PAGE")
    val page: Int,
)
