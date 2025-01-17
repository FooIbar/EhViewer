package com.hippo.ehviewer.ui.screen

import androidx.compose.foundation.MutatorMutex
import androidx.compose.material3.SnackbarHostState
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

class SnackbarContext(val state: SnackbarHostState) {
    private val mutex = MutatorMutex()

    fun CoroutineScope.launchSnackbar(msg: String) = launch {
        mutex.mutate { state.showSnackbar(msg) }
    }
}
