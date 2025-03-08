/*
 * Copyright 2016 Hippo Seven
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.hippo.ehviewer.client

import com.hippo.ehviewer.client.data.GalleryInfo

// Normal Preview (v2): https://*.hath.network/c(m|1|2)/[timed token]/[gid]-[index].(jpg|webp)
// ExHentai Large Preview (v1 Cover): https://s.exhentai.org/t/***
// E-Hentai Large Preview (v1 Cover): https://ehgt.org/***
// ExHentai v2 Cover: https://s.exhentai.org/**.webp
// E-Hentai v2 Cover: https://ehgt.org/**.webp

const val URL_PREFIX_THUMB_E = "https://ehgt.org/"
const val URL_PREFIX_THUMB_EX = "https://s.exhentai.org/"
private const val URL_PREFIX_V1_THUMB_EX = URL_PREFIX_THUMB_EX + "t/"
private val V2PreviewKeyRegex = Regex("/c([m12]/)[^/]+/(\\d+-\\d+)")

fun getImageKey(gid: Long, index: Int): String = "image:$gid:$index"

fun getThumbKey(url: String): String = url.removePrefix(URL_PREFIX_THUMB_E).removePrefix(URL_PREFIX_V1_THUMB_EX).removePrefix(URL_PREFIX_THUMB_EX)

fun getV2PreviewKey(url: String) = "$".plus(
    V2PreviewKeyRegex.find(url)?.let {
        it.groupValues[1] + it.groupValues[2]
    } ?: url,
)

val GalleryInfo.thumbUrl
    get() = keyToUrl(thumbKey!!)

fun keyToUrl(key: String) = if (key.startsWith("https:")) {
    key
} else {
    if (key.endsWith("webp")) {
        if (EhUtils.isExHentai) URL_PREFIX_THUMB_EX else URL_PREFIX_THUMB_E
    } else {
        if (EhUtils.isExHentai) URL_PREFIX_V1_THUMB_EX else URL_PREFIX_THUMB_E
    } + key
}
