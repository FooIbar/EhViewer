package eu.kanade.tachiyomi.ui.reader.viewer

import android.graphics.PointF
import android.graphics.RectF
import androidx.annotation.StringRes
import com.hippo.ehviewer.R
import eu.kanade.tachiyomi.data.preference.PreferenceValues
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
        fun invert(invertMode: PreferenceValues.TappingInvertMode): Region {
            if (invertMode == PreferenceValues.TappingInvertMode.NONE) return this
            return this.copy(
                rectF = this.rectF.invert(invertMode),
            )
        }
    }

    private val constantMenuRegion: RectF = RectF(0f, 0f, 1f, 0.05f)

    protected abstract val originalRegions: List<Region>

    val regions get() = originalRegions.map { it.invert(invertMode) }

    var invertMode: PreferenceValues.TappingInvertMode = PreferenceValues.TappingInvertMode.NONE

    fun getAction(pos: PointF): NavigationRegion {
        val x = pos.x
        val y = pos.y
        val region = regions.find { it.rectF.contains(x, y) }
        return when {
            region != null -> region.type
            constantMenuRegion.contains(x, y) -> NavigationRegion.MENU
            else -> NavigationRegion.MENU
        }
    }

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
