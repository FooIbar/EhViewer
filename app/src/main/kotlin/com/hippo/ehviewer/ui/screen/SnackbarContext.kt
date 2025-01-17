package com.hippo.ehviewer.ui.screen

import androidx.compose.foundation.MutatorMutex
import androidx.compose.material3.SnackbarHostState

class SnackbarContext(
    val state: SnackbarHostState,
    val mutex: MutatorMutex,
)
