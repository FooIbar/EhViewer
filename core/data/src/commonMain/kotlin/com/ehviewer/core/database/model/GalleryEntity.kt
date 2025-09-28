package com.ehviewer.core.database.model

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Ignore
import androidx.room.PrimaryKey
import androidx.room.TypeConverters
import com.ehviewer.core.database.util.SimpleTagsConverter
import com.ehviewer.core.model.BaseGalleryInfo

@Entity(tableName = "GALLERIES")
@TypeConverters(SimpleTagsConverter::class)
class GalleryEntity(
    @PrimaryKey
    @ColumnInfo(name = "GID")
    override var gid: Long,

    @ColumnInfo(name = "TOKEN")
    override var token: String,

    @ColumnInfo(name = "TITLE")
    override var title: String?,

    @ColumnInfo(name = "TITLE_JPN")
    override var titleJpn: String?,

    @ColumnInfo(name = "THUMB")
    override var thumbKey: String?,

    @ColumnInfo(name = "CATEGORY")
    override var category: Int,

    @ColumnInfo(name = "POSTED")
    override var posted: String?,

    @ColumnInfo(name = "UPLOADER")
    override var uploader: String?,

    @ColumnInfo(name = "RATING")
    override var rating: Float,

    @ColumnInfo(name = "SIMPLE_TAGS")
    override var simpleTags: List<String>?,

    @ColumnInfo(name = "PAGES", defaultValue = "0")
    override var pages: Int,

    @ColumnInfo(name = "SIMPLE_LANGUAGE")
    override var simpleLanguage: String?,

    @ColumnInfo(name = "FAVORITE_SLOT")
    override var favoriteSlot: Int,
) : BaseGalleryInfo() {

    @Ignore
    override var disowned: Boolean = false

    @Ignore
    override var rated: Boolean = false

    @Ignore
    override var thumbWidth: Int = 0

    @Ignore
    override var thumbHeight: Int = 0

    @Ignore
    override var favoriteName: String? = null

    @Ignore
    override var favoriteNote: String? = null
}
