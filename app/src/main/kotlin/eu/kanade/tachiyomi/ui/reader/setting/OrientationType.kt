package eu.kanade.tachiyomi.ui.reader.setting

import android.content.pm.ActivityInfo
import androidx.compose.ui.graphics.vector.ImageVector
import com.ehviewer.core.i18n.R
import com.ehviewer.core.ui.icons.EhIcons
import com.ehviewer.core.ui.icons.reader.Landscape
import com.ehviewer.core.ui.icons.reader.LandscapeLocked
import com.ehviewer.core.ui.icons.reader.Portrait
import com.ehviewer.core.ui.icons.reader.PortraitLocked
import com.ehviewer.core.ui.icons.reader.ScreenRotation

enum class OrientationType(
    override val prefValue: Int,
    val flag: Int,
    override val stringRes: Int,
    override val icon: ImageVector,
) : PreferenceType {
    DEFAULT(0, ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED, R.string.label_default, EhIcons.Reader.ScreenRotation),
    FREE(1, ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED, R.string.rotation_free, EhIcons.Reader.ScreenRotation),
    PORTRAIT(2, ActivityInfo.SCREEN_ORIENTATION_SENSOR_PORTRAIT, R.string.rotation_portrait, EhIcons.Reader.Portrait),
    LANDSCAPE(3, ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE, R.string.rotation_landscape, EhIcons.Reader.Landscape),
    LOCKED_PORTRAIT(4, ActivityInfo.SCREEN_ORIENTATION_PORTRAIT, R.string.rotation_force_portrait, EhIcons.Reader.PortraitLocked),
    LOCKED_LANDSCAPE(5, ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE, R.string.rotation_force_landscape, EhIcons.Reader.LandscapeLocked),
    REVERSE_PORTRAIT(6, ActivityInfo.SCREEN_ORIENTATION_REVERSE_PORTRAIT, R.string.rotation_reverse_portrait, EhIcons.Reader.Portrait),
    ;

    companion object {
        fun fromPreference(preference: Int): OrientationType = entries.find { it.prefValue == preference } ?: DEFAULT
    }
}
