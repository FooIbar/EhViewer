package com.hippo.ehviewer.dao

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Fts4
import com.hippo.ehviewer.client.data.BaseGalleryInfo

@Fts4(contentEntity = BaseGalleryInfo::class)
@Entity
class GalleryFts(
    @ColumnInfo(name = "TITLE")
    val title: String,

    @ColumnInfo(name = "TITLE_JPN")
    val titleJpn: String,
)
