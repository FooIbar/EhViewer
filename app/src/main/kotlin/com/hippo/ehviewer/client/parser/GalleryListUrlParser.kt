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
package com.hippo.ehviewer.client.parser

import arrow.core.Either
import com.hippo.ehviewer.client.EhUrl
import com.hippo.ehviewer.client.EhUtils
import com.hippo.ehviewer.client.data.ListUrlBuilder
import io.ktor.http.Url
import io.ktor.http.decodeURLQueryComponent

object GalleryListUrlParser {
    fun parse(urlStr: String) = Either.catch {
        val url = Url(urlStr)
        if (url.host != EhUrl.DOMAIN_E && url.host != EhUrl.DOMAIN_EX) {
            return null
        }
        val segments = url.segments
        if (segments.isEmpty()) {
            ListUrlBuilder(url.parameters).apply {
                url.parameters["f_shash"]?.let {
                    mode = ListUrlBuilder.MODE_IMAGE_SEARCH
                    hash = it
                }
            }
        } else {
            when (val head = segments[0]) {
                "uploader", "tag" -> ListUrlBuilder(url.parameters).apply {
                    mode = if (head == "uploader") ListUrlBuilder.MODE_UPLOADER else ListUrlBuilder.MODE_TAG
                    keyword = segments[1].decodeURLQueryComponent(plusIsSpace = true)
                }

                "toplist.php" -> {
                    val tl = url.parameters["tl"]
                    if (tl != null && tl in arrayOf("11", "12", "13", "15")) {
                        ListUrlBuilder(
                            mode = ListUrlBuilder.MODE_TOPLIST,
                            jumpTo = url.parameters["p"],
                            mKeyword = tl,
                        )
                    } else {
                        null
                    }
                }

                else -> {
                    val category = EhUtils.getCategory(head)
                    if (category != EhUtils.UNKNOWN) {
                        ListUrlBuilder(url.parameters).also { it.category = category }
                    } else {
                        null
                    }
                }
            }
        }
    }.getOrNull()
}
