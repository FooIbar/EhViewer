package com.ehviewer.core.database.model

import androidx.room.Entity
import androidx.room.ForeignKey
import kotlin.time.Clock

@Entity(tableName = "LOCAL_FAVORITES", foreignKeys = [ForeignKey(GalleryEntity::class, ["GID"], ["GID"])])
class LocalFavoriteInfo(gid: Long = 0, time: Long = Clock.System.now().toEpochMilliseconds()) : TimeInfo(gid, time)
