package com.hippo.ehviewer.ui

import androidx.compose.animation.AnimatedVisibilityScope
import androidx.compose.animation.SharedTransitionScope
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.platform.LocalContext
import com.hippo.ehviewer.ui.tools.DialogState
import com.hippo.ehviewer.ui.tools.LocalDialogState
import com.hippo.ehviewer.ui.tools.NoopTransitionsVisibilityScope
import com.hippo.ehviewer.ui.tools.TransitionsVisibilityScope
import com.hippo.ehviewer.ui.tools.advance
import com.hippo.ehviewer.util.findActivity
import com.ramcosta.composedestinations.navigation.DestinationsNavigator
import kotlinx.coroutines.CoroutineScope

@Composable
inline fun <R> AnimatedVisibilityScope.composing(
    navigator: DestinationsNavigator,
    block: @Composable context(MainActivity, SnackbarHostState, DialogState, SharedTransitionScope, TransitionsVisibilityScope, DestinationsNavigator, CoroutineScope)
    () -> R,
) = with(NoopTransitionsVisibilityScope) {
    advance {
        block(
            LocalContext.current.run { remember { findActivity<MainActivity>() } },
            LocalSnackBarHostState.current,
            LocalDialogState.current,
            LocalSharedTransitionScope.current,
            this,
            navigator,
            rememberCoroutineScope(),
        )
    }
}
