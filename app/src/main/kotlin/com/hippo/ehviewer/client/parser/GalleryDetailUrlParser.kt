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

import com.hippo.ehviewer.client.EhUrl

/**
 * Like http://exhentai.org/g/1234567/a1b2c3d4e5<br></br>
 */
object GalleryDetailUrlParser {
    private val URL_STRICT_PATTERN = Regex(
        "https?://(?:${EhUrl.DOMAIN_EX}|${EhUrl.DOMAIN_E}(?:/lofi)?)/(?:g|mpv)/(\\d+)/([0-9a-f]{10})",
    )
    private val URL_PATTERN = Regex("(\\d+)/([0-9a-f]{10})(?:[^0-9a-f]|$)")

    fun parse(url: String?, strict: Boolean = true): Result? {
        url ?: return null
        val pattern = if (strict) URL_STRICT_PATTERN else URL_PATTERN
        return pattern.find(url)?.destructured?.let { (gid, token) ->
            Result(gid.toLong(), token)
        }
    }

    data class Result(val gid: Long, val token: String)
}
