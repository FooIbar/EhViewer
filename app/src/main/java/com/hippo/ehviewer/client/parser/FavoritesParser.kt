package com.hippo.ehviewer.client.parser

import com.hippo.ehviewer.client.exception.NotLoggedInException
import com.hippo.ehviewer.client.exception.ParseException

object FavoritesParser {
    suspend fun parse(body: String): Result {
        if (body.contains("This page requires you to log on.</p>")) {
            throw NotLoggedInException()
        }
        val catArray = arrayOfNulls<String>(10)
        val countArray = runCatching {
            parseFav(body, catArray).also { check(it.isNotEmpty()) }
        }.getOrElse {
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

private external fun parseFav(body: String, favCat: Array<String?>): IntArray
