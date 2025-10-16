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
package com.ehviewer.core.model

import com.ehviewer.core.model.GalleryInfo.Companion.NOT_FAVORITED
import kotlinx.serialization.Serializable

@Serializable
open class BaseGalleryInfo(
    override var gid: Long = 0,
    override var token: String = "",
    override var title: String? = null,
    override var titleJpn: String? = null,
    override var thumbKey: String? = null,
    override var category: Int = 0,
    override var posted: String? = null,
    override var uploader: String? = null,
    override var disowned: Boolean = false,
    override var rating: Float = 0f,
    override var rated: Boolean = false,
    override var simpleTags: List<String>? = null,
    override var pages: Int = 0,
    override var thumbWidth: Int = 0,
    override var thumbHeight: Int = 0,
    override var simpleLanguage: String? = null,
    override var favoriteSlot: Int = NOT_FAVORITED,
    override var favoriteName: String? = null,
    override var favoriteNote: String? = null,
) : GalleryInfo {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is BaseGalleryInfo) return false

        if (gid != other.gid) return false
        if (category != other.category) return false
        if (disowned != other.disowned) return false
        if (rating != other.rating) return false
        if (rated != other.rated) return false
        if (pages != other.pages) return false
        if (thumbWidth != other.thumbWidth) return false
        if (thumbHeight != other.thumbHeight) return false
        if (favoriteSlot != other.favoriteSlot) return false
        if (token != other.token) return false
        if (title != other.title) return false
        if (titleJpn != other.titleJpn) return false
        if (thumbKey != other.thumbKey) return false
        if (posted != other.posted) return false
        if (uploader != other.uploader) return false
        if (simpleTags != other.simpleTags) return false
        if (simpleLanguage != other.simpleLanguage) return false
        if (favoriteName != other.favoriteName) return false
        if (favoriteNote != other.favoriteNote) return false

        return true
    }

    override fun hashCode(): Int {
        var result = gid.hashCode()
        result = 31 * result + category
        result = 31 * result + disowned.hashCode()
        result = 31 * result + rating.hashCode()
        result = 31 * result + rated.hashCode()
        result = 31 * result + pages
        result = 31 * result + thumbWidth
        result = 31 * result + thumbHeight
        result = 31 * result + favoriteSlot
        result = 31 * result + token.hashCode()
        result = 31 * result + (title?.hashCode() ?: 0)
        result = 31 * result + (titleJpn?.hashCode() ?: 0)
        result = 31 * result + (thumbKey?.hashCode() ?: 0)
        result = 31 * result + (posted?.hashCode() ?: 0)
        result = 31 * result + (uploader?.hashCode() ?: 0)
        result = 31 * result + (simpleTags?.hashCode() ?: 0)
        result = 31 * result + (simpleLanguage?.hashCode() ?: 0)
        result = 31 * result + (favoriteName?.hashCode() ?: 0)
        result = 31 * result + (favoriteNote?.hashCode() ?: 0)
        return result
    }
}
