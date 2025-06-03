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

import arrow.core.Either.Companion.catch
import arrow.core.getOrElse
import com.hippo.ehviewer.client.exception.ParseException
import java.nio.ByteBuffer
import kotlinx.serialization.Serializable

object ProfileParser {
    fun parse(body: ByteBuffer) = catch {
        unmarshalParsingAs<Result>(body, ::parseProfile)
    }.getOrElse {
        throw ParseException("Failed to parse profile", it)
    }

    fun parseProfileUrl(body: ByteBuffer) = catch {
        unmarshalParsingAs<String>(body, ::parseProfileUrl)
    }.getOrElse {
        throw ParseException("Failed to parse profile url", it)
    }

    @Serializable
    data class Result(val displayName: String, val avatar: String?)

    private external fun parseProfile(body: ByteBuffer, size: Int = body.limit()): Int
    private external fun parseProfileUrl(body: ByteBuffer, size: Int = body.limit()): Int
}
