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
    override val flagValue: Int,
) : PreferenceType {
    DEFAULT(0, ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED, R.string.label_default, EhIcons.Reader.ScreenRotation, 0x00000000),
    FREE(1, ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED, R.string.rotation_free, EhIcons.Reader.ScreenRotation, 0x00000008),
    PORTRAIT(2, ActivityInfo.SCREEN_ORIENTATION_SENSOR_PORTRAIT, R.string.rotation_portrait, EhIcons.Reader.Portrait, 0x00000010),
    LANDSCAPE(3, ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE, R.string.rotation_landscape, EhIcons.Reader.Landscape, 0x00000018),
    LOCKED_PORTRAIT(4, ActivityInfo.SCREEN_ORIENTATION_PORTRAIT, R.string.rotation_force_portrait, EhIcons.Reader.PortraitLocked, 0x00000020),
    LOCKED_LANDSCAPE(5, ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE, R.string.rotation_force_landscape, EhIcons.Reader.LandscapeLocked, 0x00000028),
    REVERSE_PORTRAIT(6, ActivityInfo.SCREEN_ORIENTATION_REVERSE_PORTRAIT, R.string.rotation_reverse_portrait, EhIcons.Reader.Portrait, 0x00000030),
    ;

    companion object {
        const val MASK = 0x00000038

        fun fromPreference(preference: Int?): OrientationType = entries.find { it.flagValue == preference } ?: FREE

        fun fromSpinner(position: Int?) = entries.find { value -> value.prefValue == position } ?: DEFAULT
    }
}
