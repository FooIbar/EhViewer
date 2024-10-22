package com.hippo.ehviewer.ui.tools

import androidx.collection.mutableIntObjectMapOf
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
import kotlinx.coroutines.async
import kotlinx.coroutines.launch

class StateMapViewModel : ViewModel() {
    val statesMap = mutableIntObjectMapOf<ArrayDeque<Any>>()
}

@Composable
inline fun <reified T : Any> rememberInVM(
    crossinline init: @DisallowComposableCalls ViewModel.() -> T,
) = with(viewModel<StateMapViewModel>()) {
    val compositeKey = currentCompositeKeyHash
    remember {
        val states = statesMap.getOrPut(compositeKey, ::ArrayDeque)
        states.removeLastOrNull() as T? ?: init()
    }.also { value ->
        val valueState by rememberUpdatedState(value)
        DisposableEffect(compositeKey) {
            onDispose {
                statesMap[compositeKey]?.addFirst(valueState)
            }
        }
    }
}

@Composable
inline fun <reified T : Any> rememberInVM(
    key: Any?,
    crossinline init: @DisallowComposableCalls ViewModel.() -> T,
) = with(viewModel<StateMapViewModel>()) {
    val compositeKey = currentCompositeKeyHash
    remember(key) {
        val states = statesMap.getOrPut(compositeKey, ::ArrayDeque)
        states.removeLastOrNull() as T? ?: init()
    }.also { value ->
        val valueState by rememberUpdatedState(value)
        DisposableEffect(compositeKey) {
            onDispose {
                statesMap[compositeKey]?.addFirst(valueState)
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
fun <R> asyncInVM(
    context: CoroutineContext = EmptyCoroutineContext,
    start: CoroutineStart = CoroutineStart.DEFAULT,
    block: suspend CoroutineScope.() -> R,
) = rememberInVM {
    viewModelScope.async(context, start, block)
}

@Composable
fun <T> rememberUpdatedStateInVM(newValue: T) = rememberInVM { mutableStateOf(newValue) }.apply { value = newValue }
