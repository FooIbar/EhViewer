package com.hippo.ehviewer.ui

import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.ComposeView
import com.hippo.ehviewer.ui.theme.EhTheme
import com.hippo.ehviewer.ui.tools.DialogState
import com.hippo.ehviewer.ui.tools.LocalDialogState
import com.hippo.ehviewer.ui.tools.ProvideVectorPainterCache
import com.ramcosta.composedestinations.animations.defaults.RootNavGraphDefaultAnimations

inline fun ComposeView.setMD3Content(crossinline content: @Composable () -> Unit) = setContent {
    EhTheme {
        val dialogState = remember { DialogState() }
        dialogState.Intercept()
        CompositionLocalProvider(
            LocalDialogState provides dialogState,
            LocalContentColor provides MaterialTheme.colorScheme.onBackground,
        ) {
            content()
        }
    }
}

inline fun ComponentActivity.setMD3Content(crossinline content: @Composable () -> Unit) = setContent {
    EhTheme {
        ProvideVectorPainterCache {
            val dialogState = remember { DialogState() }
            dialogState.Intercept()
            CompositionLocalProvider(
                LocalDialogState provides dialogState,
                LocalContentColor provides MaterialTheme.colorScheme.onBackground,
            ) {
                content()
            }
        }
    }
}

val ehNavAnim by lazy {
    RootNavGraphDefaultAnimations(
        enterTransition = { fadeIn(animationSpec = tween(500)) },
        exitTransition = { fadeOut(animationSpec = tween(500)) },
    )
}
