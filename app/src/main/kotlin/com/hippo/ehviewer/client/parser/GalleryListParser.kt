package com.hippo.ehviewer.client.parser

import com.hippo.ehviewer.EhDB
import com.hippo.ehviewer.R
import com.hippo.ehviewer.client.data.BaseGalleryInfo
import com.hippo.ehviewer.client.data.GalleryInfo.Companion.LOCAL_FAVORITED
import com.hippo.ehviewer.client.data.GalleryInfo.Companion.NOT_FAVORITED
import com.hippo.ehviewer.client.exception.EhException
import com.hippo.ehviewer.client.exception.ParseException
import java.nio.ByteBuffer
import kotlinx.serialization.Serializable
import moe.tarsin.coroutines.runSuspendCatching

object GalleryListParser {
    suspend fun parse(body: ByteBuffer) = runSuspendCatching {
        unmarshalParsingAs<GalleryListResult>(body, ::parseGalleryInfoList)
    }.onFailure {
        if (it is RuntimeException) {
            if (it.message?.startsWith("Your IP address") == true) {
                throw EhException(it.message)
            }
            if (it.message == "No watched tags!") {
                throw EhException(R.string.gallery_list_empty_hit_subscription)
            }
            if (it.message == "No hits found!") {
                throw EhException(R.string.gallery_list_empty_hit)
            }
        }
        throw ParseException("Can't parse gallery list", it)
    }.getOrThrow().apply {
        galleryInfoList.onEach {
            if (it.favoriteSlot == NOT_FAVORITED && EhDB.containLocalFavorites(it.gid)) {
                it.favoriteSlot = LOCAL_FAVORITED
            }
            it.generateSLang()
        }
    }

    val emptyResult = GalleryListResult(null, null, arrayListOf())
}

@Serializable
data class GalleryListResult(
    val prev: String?,
    val next: String?,
    val galleryInfoList: ArrayList<BaseGalleryInfo>,
)

private external fun parseGalleryInfoList(body: ByteBuffer, size: Int = body.limit()): Int
