package com.hippo.ehviewer.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import com.hippo.ehviewer.Settings
import com.hippo.ehviewer.collectAsState
import com.hippo.ehviewer.util.isAtLeastS

@Composable
fun EhTheme(content: @Composable () -> Unit) {
    val useDarkTheme = isSystemInDarkTheme()
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

    MaterialTheme(
        colorScheme = colors,
        content = content,
    )
}
