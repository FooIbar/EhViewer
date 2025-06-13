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

import com.hippo.ehviewer.client.getThumbKey
import com.hippo.ehviewer.client.getV2PreviewKey
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.cbor.CborArray

@Serializable
sealed interface GalleryPreview {
    val url: String
    val imageKey: String
    val position: Int
    val pToken: String
}

@Serializable
@SerialName("V1")
data class V1GalleryPreview(
    override val url: String,
    override val position: Int,
    override val pToken: String,
) : GalleryPreview {
    override val imageKey get() = getThumbKey(url)
}

@Serializable
@SerialName("V2")
data class V2GalleryPreview(
    override val url: String,
    override val position: Int,
    override val pToken: String,
    val offsetX: Int,
    val clipWidth: Int,
    val clipHeight: Int,
) : GalleryPreview {
    override val imageKey get() = getV2PreviewKey(url)
}

@Serializable
@CborArray
class GalleryPreviewList(
    val previews: List<GalleryPreview>,
    val total: Int,
)
