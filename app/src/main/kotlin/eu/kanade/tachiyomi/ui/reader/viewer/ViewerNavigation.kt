package eu.kanade.tachiyomi.ui.reader.viewer

import android.graphics.RectF
import androidx.annotation.StringRes
import com.hippo.ehviewer.R
import eu.kanade.tachiyomi.ui.reader.setting.TappingInvertMode
import eu.kanade.tachiyomi.ui.reader.viewer.navigation.DisabledNavigation
import eu.kanade.tachiyomi.ui.reader.viewer.navigation.EdgeNavigation
import eu.kanade.tachiyomi.ui.reader.viewer.navigation.KindlishNavigation
import eu.kanade.tachiyomi.ui.reader.viewer.navigation.LNavigation
import eu.kanade.tachiyomi.ui.reader.viewer.navigation.RightAndLeftNavigation
import eu.kanade.tachiyomi.util.lang.invert

abstract class ViewerNavigation {

    sealed class NavigationRegion(@StringRes val nameRes: Int, val colorRes: Int) {
        data object MENU : NavigationRegion(R.string.action_menu, R.color.navigation_menu)
        data object PREV : NavigationRegion(R.string.nav_zone_prev, R.color.navigation_prev)
        data object NEXT : NavigationRegion(R.string.nav_zone_next, R.color.navigation_next)
        data object LEFT : NavigationRegion(R.string.nav_zone_left, R.color.navigation_left)
        data object RIGHT : NavigationRegion(R.string.nav_zone_right, R.color.navigation_right)
    }

    data class Region(
        val rectF: RectF,
        val type: NavigationRegion,
    ) {
        fun invert(invertMode: TappingInvertMode): Region {
            if (invertMode == TappingInvertMode.NONE) return this
            return this.copy(
                rectF = this.rectF.invert(invertMode),
            )
        }
    }

    protected abstract val originalRegions: List<Region>

    val regions get() = originalRegions.map { it.invert(invertMode) }

    var invertMode = TappingInvertMode.NONE

    companion object {
        fun fromPreference(value: Int, isVertical: Boolean) = when (value) {
            1 -> LNavigation()
            2 -> KindlishNavigation()
            3 -> EdgeNavigation()
            4 -> RightAndLeftNavigation()
            5 -> DisabledNavigation()
            else -> if (isVertical) LNavigation() else RightAndLeftNavigation()
        }
    }
}

typealias NavigationRegions = List<ViewerNavigation.Region>

fun NavigationRegions.getAction(x: Float, y: Float) =
    find { it.rectF.contains(x, y) }?.type ?: ViewerNavigation.NavigationRegion.MENU
