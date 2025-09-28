package com.ehviewer.core.database.model

import androidx.room.Entity
import androidx.room.ForeignKey
import kotlin.time.Clock

@Entity(tableName = "HISTORY", foreignKeys = [ForeignKey(GalleryEntity::class, ["GID"], ["GID"])])
class HistoryInfo(gid: Long, time: Long = Clock.System.now().toEpochMilliseconds()) : TimeInfo(gid, time)
