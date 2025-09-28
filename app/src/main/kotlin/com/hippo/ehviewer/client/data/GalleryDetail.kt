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

import androidx.compose.ui.util.fastFlatMap
import com.ehviewer.core.model.GalleryDetail
import com.ehviewer.core.model.GalleryInfo
import com.ehviewer.core.model.GalleryTagGroup
import com.ehviewer.core.model.PowerStatus
import com.hippo.ehviewer.EhDB
import com.hippo.ehviewer.Settings
import com.hippo.ehviewer.client.EhFilter

private val LANGUAGES = arrayOf(
    "English",
    "Chinese",
    "Spanish",
    "Korean",
    "Russian",
    "French",
    "Portuguese",
    "Thai",
    "German",
    "Italian",
    "Vietnamese",
    "Polish",
    "Hungarian",
    "Dutch",
)

suspend fun GalleryDetail.fillInfo() {
    val index = LANGUAGES.indexOf(language)
    if (index != -1) simpleLanguage = GalleryInfo.S_LANGS[index]
    simpleTags = tagGroups.fastFlatMap(GalleryTagGroup::tags).map { (text, power, _) ->
        if (power == PowerStatus.Weak) "_$text" else text
    }
    if (favoriteSlot == GalleryInfo.NOT_FAVORITED && EhDB.containLocalFavorites(gid)) {
        favoriteSlot = GalleryInfo.LOCAL_FAVORITED
    }
}

suspend fun GalleryDetail.filterComments() {
    comments = with(comments) {
        val scoreThreshold = Settings.commentThreshold.value
        copy(
            comments = comments.filter {
                it.uploader ||
                    it.score > scoreThreshold &&
                    !EhFilter.filterCommenter(it.user.orEmpty()) &&
                    !EhFilter.filterComment(it.comment)
            },
        )
    }
}
