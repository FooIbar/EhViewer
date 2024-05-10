package com.hippo.ehviewer.ui.tools

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisallowComposableCalls
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.currentCompositeKeyHash
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import kotlin.coroutines.CoroutineContext
import kotlin.coroutines.EmptyCoroutineContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.launch

class StateMapViewModel : ViewModel() {
    val statesMap = mutableMapOf<Int, ArrayDeque<Any>>()
}

@Composable
inline fun <T : Any> rememberInVM(
    vararg inputs: Any?,
    crossinline init: @DisallowComposableCalls ViewModel.() -> T,
) = with(viewModel<StateMapViewModel>()) {
    val key = currentCompositeKeyHash
    remember(*inputs) {
        val states = statesMap[key] ?: ArrayDeque<Any>().also { statesMap[key] = it }
        @Suppress("UNCHECKED_CAST")
        states.removeLastOrNull() as T? ?: init()
    }.also { value ->
        val valueState by rememberUpdatedState(value)
        DisposableEffect(key) {
            onDispose {
                statesMap[key]?.addFirst(valueState)
            }
        }
    }
}

@Composable
fun launchInVM(
    context: CoroutineContext = EmptyCoroutineContext,
    start: CoroutineStart = CoroutineStart.DEFAULT,
    block: suspend CoroutineScope.() -> Unit,
) = rememberInVM {
    viewModelScope.launch(context, start, block)
}

@Composable
fun <T> rememberUpdatedStateInVM(newValue: T) = rememberInVM { mutableStateOf(newValue) }.apply { value = newValue }
