package com.hippo.ehviewer.ui

import android.content.Context
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.platform.LocalContext
import com.hippo.ehviewer.ui.tools.DialogState
import com.hippo.ehviewer.ui.tools.LocalDialogState
import com.ramcosta.composedestinations.navigation.DestinationsNavigator
import kotlinx.coroutines.CoroutineScope

@Composable
inline fun <R> composing(
    navigator: DestinationsNavigator,
    block: @Composable context(Context, SnackbarHostState, DialogState, DestinationsNavigator, CoroutineScope)
    () -> R,
) = block(LocalContext.current, LocalSnackBarHostState.current, LocalDialogState.current, navigator, rememberCoroutineScope())
