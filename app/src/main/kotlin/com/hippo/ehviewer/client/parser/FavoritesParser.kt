package com.hippo.ehviewer.client.parser

import com.hippo.ehviewer.client.exception.NotLoggedInException
import com.hippo.ehviewer.client.exception.ParseException
import java.nio.ByteBuffer
import kotlinx.serialization.Serializable
import moe.tarsin.coroutines.runSuspendCatching

@Serializable
data class FavParserResult(
    val catArray: List<String>,
    val countArray: List<Int>,
    val galleryListResult: GalleryListResult,
) {
    val prev = galleryListResult.prev
    val next = galleryListResult.next
    val galleryInfoList = galleryListResult.galleryInfoList
}

object FavoritesParser {
    fun parse(body: ByteBuffer): FavParserResult {
        return runSuspendCatching {
            unmarshalParsingAs<FavParserResult>(body, ::parseFav)
        }.getOrElse {
            if (it is RuntimeException && it.message == "Not logged in!") throw NotLoggedInException()
            throw ParseException("Parse favorites error", it)
        }
    }
}

private external fun parseFav(body: ByteBuffer, limit: Int = body.limit()): Int
