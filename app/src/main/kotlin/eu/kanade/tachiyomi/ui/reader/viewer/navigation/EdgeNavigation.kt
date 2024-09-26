package eu.kanade.tachiyomi.ui.reader.viewer.navigation

import androidx.compose.ui.geometry.Rect
import eu.kanade.tachiyomi.ui.reader.viewer.ViewerNavigation

/**
 * Visualization of default state without any inversion
 * +---+---+---+
 * | N | N | N |   P: Previous
 * +---+---+---+
 * | N | M | N |   M: Menu
 * +---+---+---+
 * | N | P | N |   N: Next
 * +---+---+---+
*/
object EdgeNavigation : ViewerNavigation() {
    override val originalRegions = listOf(
        Region(
            rect = Rect(0f, 0f, 0.33f, 1f),
            type = NavigationRegion.NEXT,
        ),
        Region(
            rect = Rect(0.33f, 0.66f, 0.66f, 1f),
            type = NavigationRegion.PREV,
        ),
        Region(
            rect = Rect(0.66f, 0f, 1f, 1f),
            type = NavigationRegion.NEXT,
        ),
    )
}
