package eu.kanade.tachiyomi.ui.reader.setting

import androidx.compose.runtime.Stable
import androidx.compose.ui.graphics.vector.ImageVector
import com.ehviewer.core.i18n.R
import com.ehviewer.core.ui.icons.EhIcons
import com.ehviewer.core.ui.icons.reader.ContinuousVertical
import com.ehviewer.core.ui.icons.reader.Default
import com.ehviewer.core.ui.icons.reader.LeftToRight
import com.ehviewer.core.ui.icons.reader.RightToLeft
import com.ehviewer.core.ui.icons.reader.Vertical
import com.ehviewer.core.ui.icons.reader.Webtoon

@Stable
enum class ReadingModeType(
    override val prefValue: Int,
    override val stringRes: Int,
    override val icon: ImageVector,
) : PreferenceType {
    DEFAULT(0, R.string.label_default, EhIcons.Reader.Default),
    LEFT_TO_RIGHT(1, R.string.left_to_right_viewer, EhIcons.Reader.LeftToRight),
    RIGHT_TO_LEFT(2, R.string.right_to_left_viewer, EhIcons.Reader.RightToLeft),
    VERTICAL(3, R.string.vertical_viewer, EhIcons.Reader.Vertical),
    WEBTOON(4, R.string.webtoon_viewer, EhIcons.Reader.Webtoon),
    CONTINUOUS_VERTICAL(5, R.string.vertical_plus_viewer, EhIcons.Reader.ContinuousVertical),
    ;

    companion object {
        @Stable
        fun isWebtoon(type: ReadingModeType) = when (type) {
            DEFAULT, LEFT_TO_RIGHT, RIGHT_TO_LEFT, VERTICAL -> false
            WEBTOON, CONTINUOUS_VERTICAL -> true
        }

        fun isVertical(type: ReadingModeType) = when (type) {
            DEFAULT, LEFT_TO_RIGHT, RIGHT_TO_LEFT -> false
            VERTICAL, WEBTOON, CONTINUOUS_VERTICAL -> true
        }

        fun fromPreference(preference: Int): ReadingModeType = entries.find { it.prefValue == preference } ?: DEFAULT
    }
}
