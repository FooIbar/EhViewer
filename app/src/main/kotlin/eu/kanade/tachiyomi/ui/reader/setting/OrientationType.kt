package eu.kanade.tachiyomi.ui.reader.setting

import android.content.pm.ActivityInfo
import androidx.annotation.StringRes
import androidx.compose.ui.graphics.vector.ImageVector
import com.hippo.ehviewer.R
import com.hippo.ehviewer.icons.EhIcons
import com.hippo.ehviewer.icons.reader.Landscape
import com.hippo.ehviewer.icons.reader.LandscapeLocked
import com.hippo.ehviewer.icons.reader.Portrait
import com.hippo.ehviewer.icons.reader.PortraitLocked
import com.hippo.ehviewer.icons.reader.ScreenRotation

enum class OrientationType(val prefValue: Int, val flag: Int, @StringRes val stringRes: Int, val icon: ImageVector, val flagValue: Int) {
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
