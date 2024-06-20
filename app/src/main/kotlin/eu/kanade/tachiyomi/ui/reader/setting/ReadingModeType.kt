package eu.kanade.tachiyomi.ui.reader.setting

import androidx.compose.runtime.Stable
import androidx.compose.ui.graphics.vector.ImageVector
import com.hippo.ehviewer.R
import com.hippo.ehviewer.icons.EhIcons
import com.hippo.ehviewer.icons.reader.ContinuousVertical
import com.hippo.ehviewer.icons.reader.Default
import com.hippo.ehviewer.icons.reader.LeftToRight
import com.hippo.ehviewer.icons.reader.RightToLeft
import com.hippo.ehviewer.icons.reader.Vertical
import com.hippo.ehviewer.icons.reader.Webtoon
import eu.kanade.tachiyomi.ui.reader.ReaderActivity
import eu.kanade.tachiyomi.ui.reader.viewer.BaseViewer
import eu.kanade.tachiyomi.ui.reader.viewer.pager.PagerViewer
import eu.kanade.tachiyomi.ui.reader.viewer.webtoon.WebtoonViewer

@Stable
enum class ReadingModeType(
    override val prefValue: Int,
    override val stringRes: Int,
    override val icon: ImageVector,
    override val flagValue: Int,
) : PreferenceType {
    DEFAULT(0, R.string.label_default, EhIcons.Reader.Default, 0x00000000),
    LEFT_TO_RIGHT(1, R.string.left_to_right_viewer, EhIcons.Reader.LeftToRight, 0x00000001),
    RIGHT_TO_LEFT(2, R.string.right_to_left_viewer, EhIcons.Reader.RightToLeft, 0x00000002),
    VERTICAL(3, R.string.vertical_viewer, EhIcons.Reader.Vertical, 0x00000003),
    WEBTOON(4, R.string.webtoon_viewer, EhIcons.Reader.Webtoon, 0x00000004),
    CONTINUOUS_VERTICAL(5, R.string.vertical_plus_viewer, EhIcons.Reader.ContinuousVertical, 0x00000005),
    ;

    companion object {
        @Stable
        fun isWebtoon(type: ReadingModeType) = when (type) {
            DEFAULT, LEFT_TO_RIGHT, RIGHT_TO_LEFT, VERTICAL -> false
            WEBTOON, CONTINUOUS_VERTICAL -> true
        }

        const val MASK = 0x00000007

        fun fromPreference(preference: Int?): ReadingModeType = entries.find { it.flagValue == preference } ?: DEFAULT

        fun isPagerType(preference: Int): Boolean {
            val mode = fromPreference(preference)
            return mode == LEFT_TO_RIGHT || mode == RIGHT_TO_LEFT || mode == VERTICAL || mode == DEFAULT
        }

        fun fromSpinner(position: Int?) = entries.find { value -> value.prefValue == position } ?: DEFAULT

        fun toViewer(preference: Int?, activity: ReaderActivity): BaseViewer = when (fromPreference(preference)) {
            LEFT_TO_RIGHT -> PagerViewer(activity)
            RIGHT_TO_LEFT -> PagerViewer(activity, true)
            VERTICAL -> PagerViewer(activity, isVertical = true)
            WEBTOON -> WebtoonViewer(activity)
            CONTINUOUS_VERTICAL -> WebtoonViewer(activity, isContinuous = false)
            DEFAULT -> PagerViewer(activity)
        }
    }
}
