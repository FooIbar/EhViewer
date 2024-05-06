package eu.kanade.tachiyomi.ui.reader.setting

import androidx.annotation.StringRes
import androidx.compose.ui.graphics.vector.ImageVector

interface PreferenceType {
    val prefValue: Int

    @get:StringRes
    val stringRes: Int

    val icon: ImageVector

    val flagValue: Int
}
