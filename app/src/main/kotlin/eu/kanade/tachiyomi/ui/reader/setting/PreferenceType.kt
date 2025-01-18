package eu.kanade.tachiyomi.ui.reader.setting

import androidx.compose.ui.graphics.vector.ImageVector
import org.jetbrains.compose.resources.StringResource

interface PreferenceType {
    val prefValue: Int

    val stringRes: StringResource

    val icon: ImageVector

    val flagValue: Int
}
