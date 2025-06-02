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
import arrow.core.partially3
import com.hippo.ehviewer.client.exception.ParseException
import java.nio.ByteBuffer
import kotlinx.serialization.Serializable

object ArchiveParser {
    fun parse(body: ByteBuffer, maybeFunds: Funds?) = catch {
        if (maybeFunds != null) {
            val archives = unmarshalParsingAs<List<Archive>>(body, ::parseArchives)
            Result(archives, maybeFunds)
        } else {
            unmarshalParsingAs<Result>(body, ::parseArchives.partially3(true))
        }
    }.getOrElse {
        throw ParseException("Can't parse archive list", it)
    }

    fun parseArchiveUrl(body: ByteBuffer) = catch {
        unmarshalParsingAs<String?>(body, ::parseArchiveUrl)
    }.getOrElse {
        throw ParseException("Can't parse archive url", it)
    }

    @Serializable
    data class Result(val archiveList: List<Archive>, val funds: Funds)
}

@Serializable
class Archive(
    val res: String,
    val name: String,
    val size: String,
    val cost: String,
    val isHath: Boolean,
)

private external fun parseArchives(body: ByteBuffer, size: Int = body.limit(), parseFunds: Boolean = false): Int
private external fun parseArchiveUrl(body: ByteBuffer, size: Int = body.limit()): Int
