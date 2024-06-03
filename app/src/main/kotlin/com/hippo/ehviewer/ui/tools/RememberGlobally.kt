package com.hippo.ehviewer.ui.tools

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisallowComposableCalls
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.currentCompositeKeyHash
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.datastore.dataStoreFile
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.byteArrayPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.LifecycleResumeEffect
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import kotlin.coroutines.CoroutineContext
import kotlin.coroutines.EmptyCoroutineContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.serialization.cbor.Cbor
import kotlinx.serialization.decodeFromByteArray
import kotlinx.serialization.encodeToByteArray
import splitties.init.appCtx

val dataStoreScope = CoroutineScope(Dispatchers.IO + SupervisorJob())
val dataStore = PreferenceDataStoreFactory.create { appCtx.dataStoreFile("Remembered.preferences_pb") }
val dataStateFlow = dataStore.data.stateIn(dataStoreScope, SharingStarted.Eagerly, emptyPreferences())

class StateMapViewModel : ViewModel() {
    val statesMap = mutableMapOf<Int, ArrayDeque<Any>>()
}

@Composable
inline fun <reified T : Any> rememberInVM(
    vararg inputs: Any?,
    crossinline init: @DisallowComposableCalls ViewModel.() -> T,
) = with(viewModel<StateMapViewModel>()) {
    val key = currentCompositeKeyHash
    remember(*inputs) {
        val states = statesMap[key] ?: ArrayDeque<Any>().also { statesMap[key] = it }
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

// Find out how make this work with any generic `Serializable` type
@Composable
inline fun <reified T> rememberMutableStateInDataStore(
    key: String,
    crossinline defaultValue: @DisallowComposableCalls () -> T,
) = with(dataStateFlow) {
    remember {
        val keyObj = byteArrayPreferencesKey(key)
        val r = value[keyObj]?.let { bytes -> Cbor.decodeFromByteArray(bytes) } ?: defaultValue()
        mutableStateOf(r)
    }.also { mutableState ->
        LifecycleResumeEffect(key) {
            onPauseOrDispose {
                dataStoreScope.launch {
                    val keyObj = byteArrayPreferencesKey(key)
                    dataStore.edit { p ->
                        p[keyObj] = Cbor.encodeToByteArray(mutableState.value)
                    }
                }
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
