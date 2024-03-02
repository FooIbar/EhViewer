package com.hippo.ehviewer.ui

import android.content.Context
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import com.hippo.ehviewer.ui.tools.DialogState
import com.hippo.ehviewer.ui.tools.LocalDialogState
import com.ramcosta.composedestinations.navigation.DestinationsNavigator

@Composable
inline fun <R> composing(
    navigator: DestinationsNavigator,
    block: @Composable context(Context, SnackbarHostState, DialogState, DestinationsNavigator)
    () -> R,
) = block(LocalContext.current, LocalSnackBarHostState.current, LocalDialogState.current, navigator)
