/*
 * Copyright (C) 2015 Hippo Seven
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.hippo.ehviewer.client.data

import com.hippo.ehviewer.Settings
import com.hippo.ehviewer.client.EhUrl
import com.hippo.ehviewer.client.EhUtils
import com.hippo.ehviewer.client.addQueryParameter
import com.hippo.ehviewer.client.addQueryParameterIfNotBlank
import com.hippo.ehviewer.client.ehUrl
import com.hippo.ehviewer.dao.QuickSearch
import com.hippo.ehviewer.ui.main.AdvanceTable
import io.ktor.http.Parameters
import kotlin.text.toIntOrNull
import kotlinx.serialization.Serializable

@Serializable
data class ListUrlBuilder(
    var mode: Int = MODE_NORMAL,
    private var prev: String? = null,
    var next: String? = null,
    // Reset to null after initial loading
    var jumpTo: String? = null,
    private var range: Int = 0,
    var category: Int = EhUtils.NONE,
    private var mKeyword: String? = null,
    var hash: String? = null,
    var language: Int = -1,
    var advanceSearch: Int = -1,
    var minRating: Int = -1,
    var pageFrom: Int = -1,
    var pageTo: Int = -1,
) {

    fun setIndex(index: String, isNext: Boolean = true) {
        next = index.takeIf { isNext }
        prev = index.takeUnless { isNext }
        range = 0
    }

    fun setJumpTo(to: Int) {
        jumpTo = to.takeUnless { it == 0 }?.toString()
    }

    fun setRange(to: Int) {
        range = to
        prev = null
        next = null
        jumpTo = null
    }

    var keyword: String?
        get() = if (MODE_UPLOADER == mode) "uploader:$mKeyword" else mKeyword
        set(keyword) {
            mKeyword = keyword
        }

    constructor(q: QuickSearch) : this(
        mode = q.mode,
        category = q.category,
        mKeyword = q.keyword,
        advanceSearch = q.advanceSearch,
        minRating = q.minRating,
        pageFrom = q.pageFrom,
        pageTo = q.pageTo,
        next = q.name.substringAfterLast('@', "").ifEmpty { null },
    )

    fun toQuickSearch(name: String) = QuickSearch(
        name = name,
        mode = mode,
        category = category,
        keyword = mKeyword,
        advanceSearch = advanceSearch,
        minRating = minRating,
        pageFrom = pageFrom,
        pageTo = pageTo,
    )

    fun equalsQuickSearch(q: QuickSearch?): Boolean {
        if (null == q) {
            return false
        }
        if (q.mode != mode) {
            return false
        }
        if (q.category != this.category) {
            return false
        }
        if (q.keyword != mKeyword) {
            return false
        }
        if (q.advanceSearch != advanceSearch) {
            return false
        }
        if (q.minRating != minRating) {
            return false
        }
        return if (q.pageFrom != pageFrom) {
            false
        } else {
            q.pageTo == pageTo
        }
    }

    constructor(params: Parameters) : this(
        prev = params["prev"],
        next = params["next"],
        jumpTo = params["seek"],
        range = params["range"]?.toIntOrNull() ?: 0,
        category = params["f_cats"]?.toIntOrNull()?.let(EhUtils::invCategory) ?: EhUtils.NONE,
        mKeyword = params["f_search"],
    ) {
        if (params["advsearch"] == "1") {
            advanceSearch = 0
            if (params["f_sh"] == "on") {
                advanceSearch = advanceSearch or AdvanceTable.SH
            }
            if (params["f_sto"] == "on") {
                advanceSearch = advanceSearch or AdvanceTable.STO
            }
            if (params["f_sfl"] == "on") {
                advanceSearch = advanceSearch or AdvanceTable.SFL
            }
            if (params["f_sfu"] == "on") {
                advanceSearch = advanceSearch or AdvanceTable.SFU
            }
            if (params["f_sft"] == "on") {
                advanceSearch = advanceSearch or AdvanceTable.SFT
            }
            minRating = params["f_srdd"]?.toIntOrNull() ?: -1
            pageFrom = params["f_spf"]?.toIntOrNull() ?: -1
            pageTo = params["f_spt"]?.toIntOrNull() ?: -1
        }
    }

    fun build(): String = when (mode) {
        MODE_NORMAL, MODE_SUBSCRIPTION -> ehUrl(EhUrl.WATCHED_PATH.takeIf { mode == MODE_SUBSCRIPTION }) {
            val category = if (!Settings.hasSignedIn.value && category <= 0) EhUtils.NON_H else category
            if (category > 0) {
                addQueryParameter("f_cats", "${EhUtils.invCategory(category)}")
            }
            val query = mKeyword?.let { keyword ->
                if (language == -1 || "gid:" in keyword || "l:" in keyword || "language:" in keyword) {
                    keyword
                } else {
                    val tag = GalleryInfo.S_LANG_TAGS[language]
                    if (keyword.isNotEmpty()) {
                        "$tag $keyword"
                    } else {
                        tag
                    }
                }
            }
            addQueryParameterIfNotBlank("f_search", query)
            addQueryParameterIfNotBlank("prev", prev)
            addQueryParameterIfNotBlank("next", next)
            addQueryParameterIfNotBlank("seek", jumpTo)
            addQueryParameterIfNotBlank("range", range.takeIf { it > 0 }?.toString())
            // Advance search
            if (advanceSearch > 0 || minRating > 0 || pageFrom > 0 || pageTo > 0) {
                addQueryParameter("advsearch", "1")
                if (advanceSearch and AdvanceTable.SH != 0) {
                    addQueryParameter("f_sh", "on")
                }
                if (advanceSearch and AdvanceTable.STO != 0) {
                    addQueryParameter("f_sto", "on")
                }
                if (advanceSearch and AdvanceTable.SFL != 0) {
                    addQueryParameter("f_sfl", "on")
                }
                if (advanceSearch and AdvanceTable.SFU != 0) {
                    addQueryParameter("f_sfu", "on")
                }
                if (advanceSearch and AdvanceTable.SFT != 0) {
                    addQueryParameter("f_sft", "on")
                }
                // Set min star
                if (minRating > 0) {
                    addQueryParameter("f_srdd", "$minRating")
                }
                // Pages
                if (pageFrom > 0 || pageTo > 0) {
                    addQueryParameterIfNotBlank(
                        "f_spf",
                        pageFrom.takeIf { it > 0 }?.toString(),
                    )
                    addQueryParameterIfNotBlank(
                        "f_spt",
                        pageTo.takeIf { it > 0 }?.toString(),
                    )
                }
            }
        }.buildString()

        MODE_UPLOADER, MODE_TAG -> {
            val path = if (mode == MODE_UPLOADER) "uploader" else "tag"
            ehUrl(listOf(path, requireNotNull(mKeyword))) {
                addQueryParameterIfNotBlank("prev", prev)
                addQueryParameterIfNotBlank("next", next)
                addQueryParameterIfNotBlank("seek", jumpTo)
                addQueryParameterIfNotBlank("range", range.takeIf { it > 0 }?.toString())
            }.buildString()
        }

        MODE_WHATS_HOT -> EhUrl.popularUrl
        MODE_IMAGE_SEARCH -> ehUrl {
            addQueryParameter("f_shash", requireNotNull(hash))
        }.buildString()
        MODE_TOPLIST -> ehUrl("toplist.php", EhUrl.DOMAIN_E) {
            addQueryParameter("tl", requireNotNull(mKeyword))
            addQueryParameterIfNotBlank("p", jumpTo)
        }.buildString()

        else -> throw IllegalStateException("Unexpected value: $mode")
    }

    companion object {
        const val MODE_NORMAL = 0x0
        const val MODE_UPLOADER = 0x1
        const val MODE_TAG = 0x2
        const val MODE_WHATS_HOT = 0x3
        const val MODE_IMAGE_SEARCH = 0x4
        const val MODE_SUBSCRIPTION = 0x5
        const val MODE_TOPLIST = 0x6
    }
}
