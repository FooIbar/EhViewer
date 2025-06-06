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
package com.hippo.ehviewer.client.data

import com.hippo.ehviewer.client.EhUrl
import com.hippo.ehviewer.client.addQueryParameter
import com.hippo.ehviewer.client.addQueryParameterIfNotBlank
import com.hippo.ehviewer.client.ehUrl
import kotlinx.serialization.Serializable

@Serializable
data class FavListUrlBuilder(
    val favCat: Int = FAV_CAT_ALL,
    val keyword: String? = null,
    var jumpTo: String? = null,
    var prev: String? = null,
    var next: String? = null,
) {
    val isLocal
        get() = favCat == FAV_CAT_LOCAL

    fun setIndex(index: String?, isNext: Boolean) {
        next = index.takeIf { isNext }
        prev = index.takeUnless { isNext }
    }

    fun build() = ehUrl(EhUrl.FAV_PATH) {
        when {
            isValidFavCat(favCat) -> addQueryParameter("favcat", "$favCat")
            favCat == FAV_CAT_ALL -> addQueryParameter("favcat", "all")
        }
        addQueryParameterIfNotBlank("f_search", keyword)
        addQueryParameterIfNotBlank("prev", prev)
        addQueryParameterIfNotBlank("next", next)
        addQueryParameterIfNotBlank("seek", jumpTo)
    }.buildString()

    companion object {
        const val FAV_CAT_ALL = -1
        const val FAV_CAT_LOCAL = -2
        fun isValidFavCat(favCat: Int) = favCat in 0..9
    }
}
