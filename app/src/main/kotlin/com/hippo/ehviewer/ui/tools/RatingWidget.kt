package com.hippo.ehviewer.ui.tools

import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.StarHalf
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.StarOutline
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.hippo.ehviewer.R
import kotlin.math.roundToInt

private val colorYellow800 = Color(0xfff9a825)

@Stable
private fun getImageVector(index: Int, rating: Int) = when {
    index * 2 + 1 < rating -> Icons.Default.Star
    index * 2 + 1 == rating -> Icons.AutoMirrored.Default.StarHalf
    else -> Icons.Default.StarOutline
}

@Composable
fun MetaRatingWidgetReuse(rating: Float, ratingSize: Dp, ratingInterval: Dp, modifier: Modifier = Modifier) {
    val r = (rating * 2).roundToInt().coerceIn(0, 10)
    Row(modifier = modifier) {
        repeat(5) {
            IconCached(
                imageVector = getImageVector(it, r),
                contentDescription = null,
                modifier = Modifier.size(ratingSize),
                tint = colorYellow800,
            )
            Spacer(modifier = Modifier.width(ratingInterval))
        }
    }
}

@Composable
fun MetaRatingWidget(rating: Float, ratingSize: Dp, ratingInterval: Dp, modifier: Modifier = Modifier, onRatingChange: ((Float) -> Unit)? = null) {
    val r = (rating * 2).roundToInt().coerceIn(0, 10)
    val density = LocalDensity.current
    val ratingSizePx = remember { with(density) { ratingSize.toPx() } }
    val ratingIntervalPx = remember { with(density) { ratingInterval.toPx() } }
    fun calculateRating(offset: Float): Float = ((offset * 2 + ratingIntervalPx) / (ratingSizePx + ratingIntervalPx))
        .roundToInt().coerceIn(0, 10).div(2f)
    Row(
        modifier = onRatingChange?.let {
            modifier
                .pointerInput(onRatingChange) {
                    detectHorizontalDragGestures { change, _ ->
                        change.consume()
                        val newRating = calculateRating(change.position.x)
                        onRatingChange(newRating)
                    }
                }
                .pointerInput(onRatingChange) {
                    detectTapGestures {
                        val newRating = calculateRating(it.x)
                        onRatingChange(newRating)
                    }
                }
        } ?: modifier,
    ) {
        repeat(5) {
            Icon(
                imageVector = getImageVector(it, r),
                contentDescription = null,
                modifier = Modifier.size(ratingSize),
                tint = colorYellow800,
            )
            Spacer(modifier = Modifier.width(ratingInterval))
        }
    }
}

@Composable
fun GalleryListCardRating(rating: Float, modifier: Modifier = Modifier) {
    MetaRatingWidgetReuse(
        rating = rating,
        ratingSize = dimensionResource(id = R.dimen.rating_size),
        ratingInterval = dimensionResource(id = R.dimen.rating_interval),
        modifier = modifier,
    )
}

@Composable
fun GalleryDetailRating(rating: Float) {
    MetaRatingWidget(rating = rating, ratingSize = 48.dp, ratingInterval = 12.dp)
}

@Composable
fun GalleryRatingBar(rating: Float, onRatingChange: (Float) -> Unit, modifier: Modifier = Modifier) {
    MetaRatingWidget(
        rating = rating,
        ratingSize = 48.dp,
        ratingInterval = 0.dp,
        modifier = modifier,
        onRatingChange = onRatingChange,
    )
}
