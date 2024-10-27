package com.hippo.ehviewer.ui.theme

import android.app.WallpaperManager
import android.os.Build
import androidx.annotation.RequiresApi
import androidx.compose.foundation.scrollbar.LocalScrollbarStyle
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MotionScheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.expressiveLightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import com.hippo.ehviewer.Settings
import com.hippo.ehviewer.collectAsState
import com.hippo.ehviewer.ui.tools.scrollbarStyle
import com.hippo.ehviewer.util.isAtLeastOMR1
import com.hippo.ehviewer.util.isAtLeastS
import com.materialkolor.dynamicColorScheme

fun ColorScheme.amoled(amoled: Boolean) = if (amoled) {
    copy(
        surface = Color.Black,
        onSurface = Color.White,
        background = Color.Black,
        onBackground = Color.White,
    )
} else {
    this
}

@Composable
fun EhTheme(useDarkTheme: Boolean, content: @Composable () -> Unit) {
    val amoled by Settings.blackDarkTheme.collectAsState()
    val context = LocalContext.current
    val colors = if (isAtLeastS) {
        if (useDarkTheme) {
            dynamicDarkColorScheme(context).amoled(amoled)
        } else {
            dynamicLightColorScheme(context)
        }
    } else {
        val color = if (isAtLeastOMR1) extractWallPaperPalette() else null
        if (color != null) {
            dynamicColorScheme(
                primary = color.first,
                isDark = useDarkTheme,
                isAmoled = amoled,
                secondary = color.second,
                tertiary = color.third,
            )
        } else {
            if (useDarkTheme) {
                darkColorScheme().amoled(amoled)
            } else {
                expressiveLightColorScheme()
            }
        }
    }

    MaterialTheme(colorScheme = colors, motionScheme = CustomMotionScheme) {
        val scrollbarStyle = scrollbarStyle(color = MaterialTheme.colorScheme.primary)
        CompositionLocalProvider(
            LocalContentColor provides MaterialTheme.colorScheme.onBackground,
            LocalScrollbarStyle provides scrollbarStyle,
            content = content,
        )
    }
}

typealias WallPaperPalette = Triple<Color, Color?, Color?>

@Composable
@RequiresApi(Build.VERSION_CODES.O_MR1)
fun extractWallPaperPalette(): WallPaperPalette? {
    val colors = WallpaperManager.getInstance(LocalContext.current)?.getWallpaperColors(WallpaperManager.FLAG_SYSTEM) ?: return null
    val primary = colors.primaryColor.toArgb().let { Color(it) }
    val secondary = colors.secondaryColor?.toArgb()?.let { Color(it) }
    val tertiary = colors.tertiaryColor?.toArgb()?.let { Color(it) }
    return WallPaperPalette(primary, secondary, tertiary)
}

// https://issuetracker.google.com/363892346
object CustomMotionScheme : MotionScheme by MotionScheme.expressive() {
    override fun <T> defaultSpatialSpec() = defaultEffectsSpec<T>()
}
