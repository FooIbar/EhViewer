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
import arrow.core.getOrElse
import com.hippo.ehviewer.client.data.GalleryCommentList
import com.hippo.ehviewer.client.data.GalleryDetail
import com.hippo.ehviewer.client.data.GalleryPreviewList
import com.hippo.ehviewer.client.exception.ParseException
import java.nio.ByteBuffer
import kotlinx.serialization.Serializable
import kotlinx.serialization.cbor.CborArray

object GalleryDetailParser {
    fun parse(body: ByteBuffer) = Either.catch {
        unmarshalParsingAs<Result>(body, ::nativeParse)
    }.getOrElse {
        throw ParseException("Failed to parse gallery detail", it)
    }

    fun parseComments(body: ByteBuffer) = Either.catch {
        unmarshalParsingAs<GalleryCommentList>(body, ::nativeParseComments)
    }.getOrElse {
        throw ParseException("Failed to parse comments", it)
    }

    fun parsePreviews(body: ByteBuffer) = Either.catch {
        unmarshalParsingAs<GalleryPreviewList>(body, ::nativeParsePreviews)
    }.getOrElse {
        throw ParseException("Failed to parse previews", it)
    }

    @Serializable
    @CborArray
    class Result(val detail: GalleryDetail, val event: String?)

    private external fun nativeParse(body: ByteBuffer, size: Int = body.limit()): Int
    private external fun nativeParseComments(body: ByteBuffer, size: Int = body.limit()): Int
    private external fun nativeParsePreviews(body: ByteBuffer, size: Int = body.limit()): Int
}
