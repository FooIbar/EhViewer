/*
 * Copyright 2015 Hippo Seven
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

import com.hippo.ehviewer.util.toEpochMillis
import com.hippo.ehviewer.util.toIntOrDefault
import com.hippo.ehviewer.util.toLocalDateTime
import com.hippo.ehviewer.util.toLongOrDefault
import com.hippo.ehviewer.util.unescapeXml
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.format.char

object ParserUtils {
    // yyyy-MM-dd HH:mm
    private val formatter = LocalDateTime.Format {
        year()
        char('-')
        monthNumber()
        char('-')
        dayOfMonth()
        char(' ')
        hour()
        char(':')
        minute()
    }

    fun formatDate(time: Long): String = formatter.format(time.toLocalDateTime())

    fun parseDate(str: String): Long = formatter.parse(str).toEpochMillis()

    fun trim(str: String?): String = str?.unescapeXml()?.trim() ?: ""

    fun parseInt(str: String?, defValue: Int): Int = trim(str).replace(",", "").toIntOrDefault(defValue)

    fun parseLong(str: String?, defValue: Long): Long = trim(str).replace(",", "").toLongOrDefault(defValue)
}
