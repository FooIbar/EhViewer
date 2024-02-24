package com.hippo.ehviewer.ui.tools

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisallowComposableCalls
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.currentCompositeKeyHash
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
    val states: MutableMap<Int, ArrayDeque<Any>> = mutableMapOf()
}

@Composable
inline fun <T : Any> rememberInVM(
    vararg inputs: Any?,
    crossinline init: @DisallowComposableCalls ViewModel.() -> T,
): T {
    val vm: StateMapViewModel = viewModel()
    val key = currentCompositeKeyHash
    val value = remember(*inputs) {
        val states = vm.states[key] ?: ArrayDeque<Any>().also { vm.states[key] = it }
        @Suppress("UNCHECKED_CAST")
        states.removeLastOrNull() as T? ?: init(vm)
    }
    val valueState = rememberUpdatedState(value)
    DisposableEffect(key) {
        onDispose {
            vm.states[key]?.addFirst(valueState.value)
        }
    }
    return value
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
fun <T> rememberUpdatedStateInVM(newValue: T): State<T> = rememberInVM {
    mutableStateOf(newValue)
}.apply { value = newValue }
