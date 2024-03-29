package com.hippo.ehviewer.ui.tools

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember

@JvmInline
value class CompositionActiveState(val state: MutableState<Boolean>) {
    @Composable
    fun Anchor() = DisposableEffect(state) {
        state.value = true
        onDispose {
            state.value = false
        }
    }
}

@Composable
fun rememberCompositionActiveState() = remember {
    CompositionActiveState(mutableStateOf(false))
}
