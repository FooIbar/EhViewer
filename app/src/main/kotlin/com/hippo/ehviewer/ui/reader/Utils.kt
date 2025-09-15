package com.hippo.ehviewer.ui.reader

import android.app.Activity
import android.view.WindowManager
import androidx.compose.foundation.gestures.animateScrollBy
import androidx.compose.foundation.gestures.scrollBy
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.pager.PagerState
import androidx.compose.runtime.Stable
import androidx.compose.ui.AbsoluteAlignment
import androidx.compose.ui.Alignment
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.FixedScale
import com.hippo.ehviewer.Settings
import eu.kanade.tachiyomi.ui.reader.setting.OrientationType

/**
 * Forces the user preferred [orientation] on the activity.
 */
fun Activity.setOrientation(orientation: Int) {
    val newOrientation = OrientationType.fromPreference(orientation)
    if (newOrientation.flag != requestedOrientation) {
        requestedOrientation = newOrientation.flag
    }
}

/**
 * Sets the brightness of the screen. Range is [-75, 100].
 * From -75 to -1 a semi-transparent black view is overlaid with the minimum brightness.
 * From 1 to 100 it sets that value as brightness.
 * 0 sets system brightness and hides the overlay.
 */
fun Activity.setCustomBrightnessValue(value: Int) {
    val readerBrightness = when {
        value > 0 -> value / 100f
        value < 0 -> 0.01f
        else -> WindowManager.LayoutParams.BRIGHTNESS_OVERRIDE_NONE
    }

    window.attributes = window.attributes.apply { screenBrightness = readerBrightness }
}

@Stable
fun Alignment.Companion.fromPreferences(value: Int, isRtl: Boolean, isVertical: Boolean) = when (value) {
    1 -> when {
        isVertical -> CenterHorizontally
        isRtl -> AbsoluteAlignment.Right
        else -> AbsoluteAlignment.Left
    }
    2 -> AbsoluteAlignment.Left
    3 -> AbsoluteAlignment.Right
    else -> CenterHorizontally
}

@Stable
fun ContentScale.Companion.fromPreferences(value: Int, srcSize: Size, dstSize: Size) = when (value) {
    2 -> Crop
    3 -> FillWidth
    4 -> FillHeight
    5 -> FixedScale(1 / Inside.computeScaleFactor(srcSize, dstSize).scaleX)
    6 -> if (srcSize.width > srcSize.height) FillHeight else FillWidth
    else -> Fit
}

suspend fun PagerState.performScrollToPage(page: Int) {
    if (Settings.pageTransitions.value) {
        animateScrollToPage(page)
    } else {
        scrollToPage(page)
    }
}

suspend fun PagerState.moveToPrevious() {
    val target = currentPage - 1
    if (target >= 0) {
        performScrollToPage(target)
    }
}

suspend fun PagerState.moveToNext() {
    val target = currentPage + 1
    if (target < pageCount) {
        performScrollToPage(target)
    }
}

suspend fun LazyListState.performScrollBy(value: Float) {
    if (Settings.pageTransitions.value) {
        animateScrollBy(value)
    } else {
        scrollBy(value)
    }
}

suspend fun LazyListState.scrollUp() {
    performScrollBy(-scrollDistance)
}

suspend fun LazyListState.scrollDown() {
    performScrollBy(scrollDistance)
}

private val LazyListState.scrollDistance
    get() = layoutInfo.viewportSize.height * SCROLL_FRACTION

const val SCROLL_FRACTION = 0.75f
