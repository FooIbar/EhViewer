package com.hippo.ehviewer.ui

import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.remember
import androidx.navigation.NavBackStackEntry
import com.hippo.ehviewer.ui.theme.EhTheme
import com.hippo.ehviewer.ui.tools.DialogState
import com.hippo.ehviewer.ui.tools.LocalDialogState
import com.hippo.ehviewer.ui.tools.ProvideVectorPainterCache
import com.ramcosta.composedestinations.animations.NavHostAnimatedDestinationStyle
import soup.compose.material.motion.animation.materialSharedAxisXIn
import soup.compose.material.motion.animation.materialSharedAxisXOut
import soup.compose.material.motion.animation.rememberSlideDistance

inline fun ComponentActivity.setMD3Content(crossinline content: @Composable () -> Unit) = setContent {
    EhTheme(useDarkTheme = isSystemInDarkTheme()) {
        ProvideVectorPainterCache {
            val dialogState = remember { DialogState() }
            dialogState.Intercept()
            CompositionLocalProvider(
                LocalDialogState provides dialogState,
            ) {
                content()
            }
        }
    }
}

private typealias Ty = AnimatedContentTransitionScope<NavBackStackEntry>

@Composable
fun rememberEhNavAnim(): NavHostAnimatedDestinationStyle {
    val slideDistance = rememberSlideDistance()
    return remember(slideDistance) {
        object : NavHostAnimatedDestinationStyle() {
            override val enterTransition: Ty.() -> EnterTransition = { materialSharedAxisXIn(true, slideDistance) }
            override val exitTransition: Ty.() -> ExitTransition = { materialSharedAxisXOut(true, slideDistance) }
            override val popEnterTransition: Ty.() -> EnterTransition = { materialSharedAxisXIn(false, slideDistance) }
            override val popExitTransition: Ty.() -> ExitTransition = { materialSharedAxisXOut(false, slideDistance) }
        }
    }
}
