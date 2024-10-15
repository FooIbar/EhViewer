package com.hippo.ehviewer.ui.theme

import androidx.compose.foundation.scrollbar.LocalScrollbarStyle
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MotionScheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import com.hippo.ehviewer.Settings
import com.hippo.ehviewer.collectAsState
import com.hippo.ehviewer.ui.tools.scrollbarStyle
import com.hippo.ehviewer.util.isAtLeastS

@Composable
fun EhTheme(useDarkTheme: Boolean, content: @Composable () -> Unit) {
    val amoled by Settings.blackDarkTheme.collectAsState()
    val context = LocalContext.current
    val colors = if (useDarkTheme) {
        if (isAtLeastS) {
            dynamicDarkColorScheme(context)
        } else {
            darkColorScheme()
        }.let {
            if (amoled) {
                it.copy(
                    surface = Color.Black,
                    onSurface = Color.White,
                    background = Color.Black,
                    onBackground = Color.White,
                )
            } else {
                it
            }
        }
    } else {
        if (isAtLeastS) {
            dynamicLightColorScheme(context)
        } else {
            lightColorScheme()
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

// https://issuetracker.google.com/363892346
object CustomMotionScheme : MotionScheme {
    private val motionScheme = MotionScheme.standardMotionScheme()

    override fun <T> defaultSpatialSpec() = motionScheme.defaultEffectsSpec<T>()

    override fun <T> fastSpatialSpec() = motionScheme.fastSpatialSpec<T>()

    override fun <T> slowSpatialSpec() = motionScheme.slowSpatialSpec<T>()

    override fun <T> defaultEffectsSpec() = motionScheme.defaultEffectsSpec<T>()

    override fun <T> fastEffectsSpec() = motionScheme.fastEffectsSpec<T>()

    override fun <T> slowEffectsSpec() = motionScheme.slowEffectsSpec<T>()
}
