package eu.kanade.tachiyomi.ui.reader.viewer

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import com.ehviewer.core.common.Res
import com.ehviewer.core.common.action_menu
import com.ehviewer.core.common.nav_zone_left
import com.ehviewer.core.common.nav_zone_next
import com.ehviewer.core.common.nav_zone_prev
import com.ehviewer.core.common.nav_zone_right
import com.hippo.ehviewer.R
import eu.kanade.tachiyomi.ui.reader.setting.TappingInvertMode
import eu.kanade.tachiyomi.ui.reader.viewer.navigation.DisabledNavigation
import eu.kanade.tachiyomi.ui.reader.viewer.navigation.EdgeNavigation
import eu.kanade.tachiyomi.ui.reader.viewer.navigation.KindlishNavigation
import eu.kanade.tachiyomi.ui.reader.viewer.navigation.LNavigation
import eu.kanade.tachiyomi.ui.reader.viewer.navigation.RightAndLeftNavigation
import eu.kanade.tachiyomi.util.lang.invert
import org.jetbrains.compose.resources.StringResource

abstract class ViewerNavigation {
    sealed class NavigationRegion(val nameRes: StringResource, val colorRes: Int) {
        data object MENU : NavigationRegion(Res.string.action_menu, R.color.navigation_menu)
        data object PREV : NavigationRegion(Res.string.nav_zone_prev, R.color.navigation_prev)
        data object NEXT : NavigationRegion(Res.string.nav_zone_next, R.color.navigation_next)
        data object LEFT : NavigationRegion(Res.string.nav_zone_left, R.color.navigation_left)
        data object RIGHT : NavigationRegion(Res.string.nav_zone_right, R.color.navigation_right)
    }

    data class Region(val rect: Rect, val type: NavigationRegion) {
        fun invert(invertMode: TappingInvertMode): Region {
            if (invertMode == TappingInvertMode.NONE) return this
            return copy(rect = rect.invert(invertMode))
        }
    }

    protected abstract val originalRegions: List<Region>

    fun regions(invertMode: TappingInvertMode = TappingInvertMode.NONE) = originalRegions.map { it.invert(invertMode) }

    companion object {
        fun fromPreference(value: Int, isVertical: Boolean) = when (value) {
            1 -> LNavigation
            2 -> KindlishNavigation
            3 -> EdgeNavigation
            4 -> RightAndLeftNavigation
            5 -> DisabledNavigation
            else -> if (isVertical) LNavigation else RightAndLeftNavigation
        }
    }
}

typealias NavigationRegions = List<ViewerNavigation.Region>

fun NavigationRegions.getAction(offset: Offset) = find { offset in it.rect }?.type ?: ViewerNavigation.NavigationRegion.MENU
