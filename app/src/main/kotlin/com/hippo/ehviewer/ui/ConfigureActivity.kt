package com.hippo.ehviewer.ui

import android.app.Activity
import android.os.Bundle
import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.layout.imePadding
import androidx.compose.material3.windowsizeclass.calculateWindowSizeClass
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import com.hippo.ehviewer.Settings
import com.hippo.ehviewer.client.EhCookieStore
import com.hippo.ehviewer.ui.destinations.BaseScreenDestination
import com.hippo.ehviewer.ui.destinations.SelectSiteScreenDestination
import com.hippo.ehviewer.ui.destinations.SignInScreenDestination
import com.hippo.ehviewer.ui.tools.LocalWindowSizeClass
import com.hippo.ehviewer.util.findActivity
import com.ramcosta.composedestinations.DestinationsNavHost
import com.ramcosta.composedestinations.animations.defaults.RootNavGraphDefaultAnimations
import com.ramcosta.composedestinations.annotation.Destination
import com.ramcosta.composedestinations.rememberNavHostEngine

class ConfigureActivity : EhActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val rootNavGraphDefaultAnimations = RootNavGraphDefaultAnimations(
            enterTransition = { slideIntoContainer(AnimatedContentTransitionScope.SlideDirection.Up, tween(200)) },
            exitTransition = { fadeOut(tween(200)) },
            popEnterTransition = { fadeIn(tween(200)) },
            popExitTransition = { slideOutOfContainer(AnimatedContentTransitionScope.SlideDirection.Down, tween(200)) },
        )
        setMD3Content {
            val windowSizeClass = calculateWindowSizeClass(this)
            CompositionLocalProvider(LocalWindowSizeClass provides windowSizeClass) {
                DestinationsNavHost(
                    navGraph = NavGraphs.root,
                    modifier = Modifier.imePadding(),
                    startRoute = if (Settings.needSignIn) {
                        if (EhCookieStore.hasSignedIn()) SelectSiteScreenDestination else SignInScreenDestination
                    } else {
                        BaseScreenDestination
                    },
                    engine = rememberNavHostEngine(rootDefaultAnimations = rootNavGraphDefaultAnimations),
                )
            }
        }
    }
}

@Destination
@Composable
fun Finish() {
    val context = LocalContext.current
    SideEffect {
        context.findActivity<Activity>().finish()
    }
}
