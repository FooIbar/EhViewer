package com.hippo.ehviewer.ui.tools

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.platform.LocalViewConfiguration
import androidx.compose.ui.platform.ViewConfiguration

// TODO: https://issuetracker.google.com/issues/269627294
@Composable
fun LocalTouchSlopProvider(factor: Float, content: @Composable () -> Unit) {
    val current = LocalViewConfiguration.current
    CompositionLocalProvider(
        value = LocalViewConfiguration provides object : ViewConfiguration by current {
            override val touchSlop: Float
                get() = current.touchSlop * factor
        },
        content = content,
    )
}
