package com.ehviewer.core.data.model

import com.ehviewer.core.database.model.GalleryEntity
import com.ehviewer.core.model.BaseGalleryInfo
import com.ehviewer.core.model.GalleryDetail
import com.ehviewer.core.model.GalleryInfo
import com.ehviewer.core.util.unreachable

fun GalleryInfo.findBaseInfo(): BaseGalleryInfo = when (this) {
    is BaseGalleryInfo -> this
    is GalleryDetail -> galleryInfo
    else -> unreachable()
}

fun GalleryInfo.asGalleryDetail(): GalleryDetail? = this as? GalleryDetail

fun GalleryInfo.asEntity() = this as? GalleryEntity ?: GalleryEntity(
    gid = gid,
    token = token,
    title = title,
    titleJpn = titleJpn,
    thumbKey = thumbKey,
    category = category,
    posted = posted,
    uploader = uploader,
    rating = rating,
    simpleTags = simpleTags,
    pages = pages,
    simpleLanguage = simpleLanguage,
    favoriteSlot = favoriteSlot,
)
