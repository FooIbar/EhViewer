package com.hippo.ehviewer.ui

import androidx.compose.animation.AnimatedVisibilityScope
import androidx.compose.animation.SharedTransitionScope
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import com.hippo.ehviewer.ui.screen.implicit
import com.hippo.ehviewer.ui.tools.DialogState
import com.hippo.ehviewer.ui.tools.LocalGlobalDialogState
import com.hippo.ehviewer.ui.tools.NoopTransitionsVisibilityScope
import com.hippo.ehviewer.ui.tools.TransitionsVisibilityScope
import com.hippo.ehviewer.ui.tools.togetherWith
import com.hippo.ehviewer.util.findActivity
import com.ramcosta.composedestinations.navigation.DestinationsNavigator
import kotlinx.coroutines.CoroutineScope

@Composable
inline fun <R> AnimatedVisibilityScope.Screen(
    navigator: DestinationsNavigator,
    block: @Composable context(MainActivity, SnackbarHostState, DialogState, SharedTransitionScope, TransitionsVisibilityScope, DestinationsNavigator, CoroutineScope)
    () -> R,
) = Box(modifier = Modifier.fillMaxSize()) {
    val dialogState = with(LocalGlobalDialogState.current) { rememberLocal() }
    with(NoopTransitionsVisibilityScope) {
        togetherWith(implicit<AnimatedVisibilityScope>()) {
            block(
                with(LocalContext.current) { remember { findActivity() } },
                LocalSnackBarHostState.current,
                dialogState,
                LocalSharedTransitionScope.current,
                this,
                navigator,
                rememberCoroutineScope(),
            )
            dialogState.Place()
        }
    }
}
