package com.hippo.ehviewer.ui.screen

import androidx.compose.foundation.MutatorMutex
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Stable
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

@Stable
class SnackbarContext {
    private val mutex = MutatorMutex()
    val state = SnackbarHostState()

    fun CoroutineScope.launchSnackbar(msg: String) = launch {
        mutex.mutate { state.showSnackbar(msg) }
    }

    suspend fun showSnackbar(
        message: String,
        actionLabel: String? = null,
        withDismissAction: Boolean = false,
    ) = mutex.mutate {
        state.showSnackbar(message, actionLabel, withDismissAction)
    }
}
