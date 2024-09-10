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

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

interface GalleryPreview : Parcelable {
    val url: String
    val position: Int
}

@Parcelize
data class LargeGalleryPreview(
    override val url: String,
    override val position: Int,
) : GalleryPreview

@Parcelize
data class NormalGalleryPreview(
    override val url: String,
    override val position: Int,
    val offsetX: Int,
    val clipWidth: Int,
    val clipHeight: Int,
) : GalleryPreview
