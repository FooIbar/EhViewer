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
) : GalleryInfo
