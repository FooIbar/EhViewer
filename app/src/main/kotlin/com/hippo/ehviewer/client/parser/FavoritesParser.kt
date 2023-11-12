package com.hippo.ehviewer.client.parser

import com.hippo.ehviewer.client.exception.NotLoggedInException
import com.hippo.ehviewer.client.exception.ParseException
import java.nio.ByteBuffer
import moe.tarsin.coroutines.runSuspendCatching

object FavoritesParser {
    suspend fun parse(body: ByteBuffer): Result {
        val catArray = arrayOfNulls<String>(10)
        val countArray = runSuspendCatching {
            parseFav(body, favCat = catArray).also { check(it.isNotEmpty()) }
        }.getOrElse {
            if (it is RuntimeException && it.message == "Not logged in!") throw NotLoggedInException()
            throw ParseException("Parse favorites error", it)
        }
        val result = GalleryListParser.parse(body)
        return Result(catArray.requireNoNulls(), countArray, result)
    }

    class Result(
        val catArray: Array<String>,
        val countArray: IntArray,
        galleryListResult: GalleryListResult,
    ) {
        val prev = galleryListResult.prev
        val next = galleryListResult.next
        val galleryInfoList = galleryListResult.galleryInfoList
    }
}

private external fun parseFav(body: ByteBuffer, limit: Int = body.limit(), favCat: Array<String?>): IntArray
