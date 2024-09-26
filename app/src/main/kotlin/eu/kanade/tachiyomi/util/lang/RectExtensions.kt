package eu.kanade.tachiyomi.util.lang

import androidx.compose.ui.geometry.Rect
import eu.kanade.tachiyomi.ui.reader.setting.TappingInvertMode

fun Rect.invert(invertMode: TappingInvertMode): Rect {
    val horizontal = invertMode.shouldInvertHorizontal
    val vertical = invertMode.shouldInvertVertical
    return when {
        horizontal && vertical -> Rect(1f - right, 1f - bottom, 1f - left, 1f - top)
        vertical -> Rect(left, 1f - bottom, right, 1f - top)
        horizontal -> Rect(1f - right, top, 1f - left, bottom)
        else -> this
    }
}
