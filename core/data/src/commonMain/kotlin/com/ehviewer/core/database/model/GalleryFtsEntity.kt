package com.ehviewer.core.database.model

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Fts4

@Fts4(contentEntity = GalleryEntity::class)
@Entity(tableName = "GALLERIES_FTS")
class GalleryFtsEntity(
    @ColumnInfo(name = "TITLE")
    val title: String,

    @ColumnInfo(name = "TITLE_JPN")
    val titleJpn: String,

    @ColumnInfo(name = "SIMPLE_TAGS")
    val simpleTags: String?,
)
