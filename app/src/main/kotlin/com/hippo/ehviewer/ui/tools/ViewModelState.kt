package com.hippo.ehviewer.ui.tools

import androidx.collection.mutableLongObjectMapOf
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisallowComposableCalls
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.State
import androidx.compose.runtime.currentCompositeKeyHashCode
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.snapshotFlow
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import kotlin.coroutines.CoroutineContext
import kotlin.coroutines.EmptyCoroutineContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.mapLatest
import kotlinx.coroutines.launch

@Suppress("UNCHECKED_CAST")
inline fun <T : Any> nonNullState(block: MutableState<T>.() -> Unit) = (mutableStateOf<T?>(null) as MutableState<T>).apply(block) as State<T>

class StateMapViewModel : ViewModel() {
    val statesMap = mutableLongObjectMapOf<ArrayDeque<Any>>()
}

@Composable
inline fun <reified T : Any> rememberInVM(
    crossinline init: @DisallowComposableCalls ViewModel.() -> T,
) = with(viewModel<StateMapViewModel>()) {
    val compositeKey = currentCompositeKeyHashCode
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
inline fun <T : Any, K : Any> rememberInVM(
    key: K,
    crossinline init: @DisallowComposableCalls ViewModel.() -> T,
) = with(viewModel<StateMapViewModel>()) {
    val compositeKey = currentCompositeKeyHashCode
    remember(key) {
        val states = statesMap.getOrPut(compositeKey, ::ArrayDeque)
        @Suppress("UNCHECKED_CAST")
        (states.removeLastOrNull() as Pair<K, T>?)?.let { (k, v) -> v.takeIf { k == key } } ?: init()
    }.also { value ->
        val keyState by rememberUpdatedState(key)
        val valueState by rememberUpdatedState(value)
        DisposableEffect(compositeKey) {
            onDispose {
                statesMap[compositeKey]?.addFirst(keyState to valueState)
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
    key: Any?,
    context: CoroutineContext = EmptyCoroutineContext,
    start: CoroutineStart = CoroutineStart.DEFAULT,
    block: suspend CoroutineScope.(CoroutineScope) -> R,
) = rememberUpdatedStateInVM(key).let { key ->
    val f by rememberUpdatedStateInVM(block)
    rememberInVM {
        nonNullState {
            viewModelScope.launch(start = CoroutineStart.UNDISPATCHED) {
                snapshotFlow { key.value }.mapLatest {
                    coroutineScope {
                        val outerScope = this
                        value = async(context, start) { f(outerScope) }
                        awaitCancellation()
                    }
                }.collect()
            }
        }
    }
}

@Composable
fun <T> rememberUpdatedStateInVM(newValue: T) = rememberInVM { mutableStateOf(newValue) }.apply { value = newValue }
