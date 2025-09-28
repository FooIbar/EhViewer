package com.ehviewer.core.database.model

import androidx.room.ColumnInfo
import androidx.room.PrimaryKey

open class TimeInfo(
    @PrimaryKey
    @ColumnInfo(name = "GID")
    var gid: Long,

    @ColumnInfo(name = "TIME")
    val time: Long,
)
