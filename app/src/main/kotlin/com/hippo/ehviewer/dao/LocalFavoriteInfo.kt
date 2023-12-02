package com.hippo.ehviewer.dao

import androidx.room.Entity
import androidx.room.ForeignKey
import com.hippo.ehviewer.client.data.BaseGalleryInfo
import kotlinx.datetime.Clock

@Entity(tableName = "LOCAL_FAVORITES", foreignKeys = [ForeignKey(BaseGalleryInfo::class, ["GID"], ["GID"])])
class LocalFavoriteInfo(gid: Long = 0, time: Long = Clock.System.now().toEpochMilliseconds()) : TimeInfo(gid, time)
