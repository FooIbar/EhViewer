package eu.kanade.tachiyomi.ui.reader.setting

import android.content.pm.ActivityInfo
import androidx.compose.ui.graphics.vector.ImageVector
import com.ehviewer.core.common.Res
import com.ehviewer.core.common.label_default
import com.ehviewer.core.common.rotation_force_landscape
import com.ehviewer.core.common.rotation_force_portrait
import com.ehviewer.core.common.rotation_free
import com.ehviewer.core.common.rotation_landscape
import com.ehviewer.core.common.rotation_portrait
import com.ehviewer.core.common.rotation_reverse_portrait
import com.hippo.ehviewer.icons.EhIcons
import com.hippo.ehviewer.icons.reader.Landscape
import com.hippo.ehviewer.icons.reader.LandscapeLocked
import com.hippo.ehviewer.icons.reader.Portrait
import com.hippo.ehviewer.icons.reader.PortraitLocked
import com.hippo.ehviewer.icons.reader.ScreenRotation
import org.jetbrains.compose.resources.StringResource

enum class OrientationType(
    override val prefValue: Int,
    val flag: Int,
    override val stringRes: StringResource,
    override val icon: ImageVector,
    override val flagValue: Int,
) : PreferenceType {
    DEFAULT(0, ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED, Res.string.label_default, EhIcons.Reader.ScreenRotation, 0x00000000),
    FREE(1, ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED, Res.string.rotation_free, EhIcons.Reader.ScreenRotation, 0x00000008),
    PORTRAIT(2, ActivityInfo.SCREEN_ORIENTATION_SENSOR_PORTRAIT, Res.string.rotation_portrait, EhIcons.Reader.Portrait, 0x00000010),
    LANDSCAPE(3, ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE, Res.string.rotation_landscape, EhIcons.Reader.Landscape, 0x00000018),
    LOCKED_PORTRAIT(4, ActivityInfo.SCREEN_ORIENTATION_PORTRAIT, Res.string.rotation_force_portrait, EhIcons.Reader.PortraitLocked, 0x00000020),
    LOCKED_LANDSCAPE(5, ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE, Res.string.rotation_force_landscape, EhIcons.Reader.LandscapeLocked, 0x00000028),
    REVERSE_PORTRAIT(6, ActivityInfo.SCREEN_ORIENTATION_REVERSE_PORTRAIT, Res.string.rotation_reverse_portrait, EhIcons.Reader.Portrait, 0x00000030),
    ;

    companion object {
        const val MASK = 0x00000038

        fun fromPreference(preference: Int?): OrientationType = entries.find { it.flagValue == preference } ?: FREE

        fun fromSpinner(position: Int?) = entries.find { value -> value.prefValue == position } ?: DEFAULT
    }
}
