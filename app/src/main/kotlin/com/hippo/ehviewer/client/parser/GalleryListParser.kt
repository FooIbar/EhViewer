package com.hippo.ehviewer.client.parser

import com.hippo.ehviewer.client.data.BaseGalleryInfo
import com.hippo.ehviewer.client.exception.ParseException
import java.nio.ByteBuffer
import kotlinx.serialization.Serializable
import moe.tarsin.coroutines.runSuspendCatching

object GalleryListParser {
    fun parse(body: ByteBuffer) = runSuspendCatching {
        unmarshalParsingAs<GalleryListResult>(body, ::parseGalleryInfoList)
    }.getOrElse {
        throw ParseException("Can't parse gallery list", it)
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
