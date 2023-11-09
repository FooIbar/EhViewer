package com.hippo.ehviewer.client.parser

import androidx.annotation.Keep
import com.hippo.ehviewer.EhDB
import com.hippo.ehviewer.R
import com.hippo.ehviewer.client.data.BaseGalleryInfo
import com.hippo.ehviewer.client.data.GalleryInfo.Companion.LOCAL_FAVORITED
import com.hippo.ehviewer.client.data.GalleryInfo.Companion.NOT_FAVORITED
import com.hippo.ehviewer.client.exception.EhException
import com.hippo.ehviewer.client.exception.ParseException

object GalleryListParser {
    suspend fun parse(body: String) = runCatching {
        parseGalleryInfoList(body).apply {
            galleryInfoList.onEach {
                if (it.favoriteSlot == NOT_FAVORITED && EhDB.containLocalFavorites(it.gid)) {
                    it.favoriteSlot = LOCAL_FAVORITED
                }
                it.generateSLang()
            }
        }
    }.getOrElse {
        if (body.contains("<p>You do not have any watched tags")) {
            throw EhException(R.string.gallery_list_empty_hit_subscription)
        }
        if (body.contains("No hits found</p>")) {
            throw EhException(R.string.gallery_list_empty_hit)
        }
        throw ParseException("Can't parse gallery list", it)
    }

    val emptyResult = GalleryListResult(null, null, arrayListOf())
}

class GalleryListResult @Keep constructor(
    val prev: String?,
    val next: String?,
    val galleryInfoList: ArrayList<BaseGalleryInfo>,
)

private external fun parseGalleryInfoList(e: String): GalleryListResult
