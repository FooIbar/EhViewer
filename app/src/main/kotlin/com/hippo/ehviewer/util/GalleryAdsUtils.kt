package com.hippo.ehviewer.util

import com.hippo.ehviewer.Settings
import com.hippo.ehviewer.client.data.GalleryInfo
import okio.Path.Companion.toOkioPath
import splitties.init.appCtx

fun detectAds(index: Int, size: Int) = index > size - 10 && Settings.stripExtraneousAds.value

val GalleryInfo.hasAds
    get() = simpleTags?.any { "extraneous ads" in it } == true

val AdsPlaceholderFile = appCtx.filesDir.toOkioPath() / "AdsPlaceholder"
