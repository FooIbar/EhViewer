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

import com.hippo.ehviewer.Settings
import com.hippo.ehviewer.client.data.GalleryInfo

// Normal Preview: https://*.hath.network/cm/[timed token]/[gid]-[index].jpg
// ExHentai Large Preview: https://s.exhentai.org/t/***
// E-Hentai Large Preview: https://ehgt.org/***

const val URL_PREFIX_THUMB_E = "https://ehgt.org/"
const val URL_PREFIX_THUMB_EX = "https://s.exhentai.org/t/"
const val URL_SIGNATURE_THUMB_NORMAL = ".hath.network/cm/"
private val NormalPreviewKeyRegex = Regex("(/\\d+-\\d+)\\.\\w+$")

fun getImageKey(gid: Long, index: Int): String = "image:$gid:$index"

fun getThumbKey(url: String): String = url.removePrefix(URL_PREFIX_THUMB_E).removePrefix(URL_PREFIX_THUMB_EX)

fun getNormalPreviewKey(url: String) = NormalPreviewKeyRegex.find(url)?.groupValues[1] ?: url

val String.isNormalPreviewKey
    get() = startsWith('/')

val GalleryInfo.thumbUrl
    get() = thumbPrefix + EhUtils.handleThumbUrlResolution(thumbKey!!)

val thumbPrefix
    get() = if (EhUtils.isExHentai && !Settings.forceEhThumb.value) URL_PREFIX_THUMB_EX else URL_PREFIX_THUMB_E
