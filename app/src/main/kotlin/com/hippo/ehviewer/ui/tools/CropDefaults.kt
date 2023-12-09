package com.hippo.ehviewer.ui.tools

import coil3.Image
import com.hippo.ehviewer.client.data.NormalGalleryPreview

private const val CROP_MIN_ASPECT = 0.5F
private const val CROP_MAX_ASPECT = 0.8f
private val CROP_GOOD_RANGE = CROP_MIN_ASPECT..CROP_MAX_ASPECT
private fun shouldCrop(width: Int, height: Int) = (width.toFloat() / height) in CROP_GOOD_RANGE

val NormalGalleryPreview.shouldCrop: Boolean
    get() = shouldCrop(clipWidth, clipHeight)

val Image.shouldCrop: Boolean
    get() = shouldCrop(width, height)
