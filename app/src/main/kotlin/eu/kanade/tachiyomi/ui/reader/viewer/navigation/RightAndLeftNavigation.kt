package eu.kanade.tachiyomi.ui.reader.viewer.navigation

import androidx.compose.ui.geometry.Rect
import eu.kanade.tachiyomi.ui.reader.viewer.ViewerNavigation

/**
 * Visualization of default state without any inversion
 * +---+---+---+
 * | N | M | P |   P: Move Right
 * +---+---+---+
 * | N | M | P |   M: Menu
 * +---+---+---+
 * | N | M | P |   N: Move Left
 * +---+---+---+
 */
object RightAndLeftNavigation : ViewerNavigation() {
    override val originalRegions = listOf(
        Region(
            rect = Rect(0f, 0f, 0.33f, 1f),
            type = NavigationRegion.LEFT,
        ),
        Region(
            rect = Rect(0.66f, 0f, 1f, 1f),
            type = NavigationRegion.RIGHT,
        ),
    )
}
