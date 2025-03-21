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

import com.hippo.ehviewer.client.exception.InsufficientFundsException
import com.hippo.ehviewer.client.exception.NoHAtHClientException
import eu.kanade.tachiyomi.util.system.logcat
import org.jsoup.Jsoup

object ArchiveParser {
    private val PATTERN_ARCHIVE_URL =
        Regex("<strong>(.*)</strong>.*<a href=\"([^\"]*)\">Click Here To Start Downloading</a>")
    private val PATTERN_CURRENT_FUNDS =
        Regex("<p>([\\d,]+) GP \\[[^]]*] &nbsp; ([\\d,]+) Credits \\[[^]]*]</p>")
    private val PATTERN_HATH_ARCHIVE =
        Regex("<p><a href=\"[^\"]*\" onclick=\"return do_hathdl\\('([0-9]+|org)'\\)\">([^<]+)</a></p>\\s*<p>([\\w. ]+)</p>\\s*<p>([\\w. ]+)</p>")
    private const val ERROR_NEED_HATH_CLIENT =
        "You must have a H@H client assigned to your account to use this feature."
    private const val ERROR_INSUFFICIENT_FUNDS =
        "You do not have enough funds to download this archive."

    fun parse(body: String, maybeFunds: Funds?): Result {
        val archiveList = ArrayList<Archive>()
        Jsoup.parse(body).select("#db>div>div").forEach { element ->
            if (element.childrenSize() > 0 && !element.attr("style").contains("color:#CCCCCC")) {
                runCatching {
                    val res = element.selectFirst("form>input")!!.attr("value")
                    val name = element.selectFirst("form>div>input")!!.attr("value")
                    val size = element.selectFirst("p>strong")!!.text()
                    val cost = element.selectFirst("div>strong")!!.text().replace(",", "")
                    Archive(res, name, size, cost, false)
                }.onSuccess {
                    archiveList.add(it)
                }.onFailure {
                    logcat(it)
                }
            }
        }
        PATTERN_HATH_ARCHIVE.findAll(body).forEach { matchResult ->
            val (res, name, size, cost) = matchResult.groupValues.slice(1..4)
                .map { ParserUtils.trim(it) }
            val item = Archive(res, name, size, cost, true)
            archiveList.add(item)
        }
        val funds = maybeFunds ?: PATTERN_CURRENT_FUNDS.find(body)!!.groupValues.run {
            val fundsGP = ParserUtils.parseInt(get(1), 0) / 1000
            val fundsC = ParserUtils.parseInt(get(2), 0)
            Funds(fundsGP, fundsC)
        }
        return Result(archiveList, funds)
    }

    fun parseArchiveUrl(body: String): String? {
        if (body.contains(ERROR_NEED_HATH_CLIENT)) {
            throw NoHAtHClientException("No H@H client")
        } else if (body.contains(ERROR_INSUFFICIENT_FUNDS)) {
            throw InsufficientFundsException()
        }
        return Jsoup.parse(body).selectFirst("#continue>a[href]")
            ?.let { it.attr("href") + "?start=1" }
        // TODO: Check more errors
    }

    data class Result(val archiveList: List<Archive>, val funds: Funds)
}

class Archive(
    val res: String,
    val name: String,
    val size: String,
    val cost: String,
    val isHAtH: Boolean,
)
