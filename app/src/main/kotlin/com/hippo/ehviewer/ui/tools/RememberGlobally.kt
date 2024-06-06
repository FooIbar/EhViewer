package com.hippo.ehviewer.ui.tools

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisallowComposableCalls
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.Stable
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
import arrow.core.memoize
import com.hippo.ehviewer.ui.screen.implicit
import kotlin.coroutines.CoroutineContext
import kotlin.coroutines.EmptyCoroutineContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.serialization.BinaryFormat
import kotlinx.serialization.KSerializer
import kotlinx.serialization.cbor.Cbor
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import kotlinx.serialization.serializer
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

// TODO: break change: record [SnapshotMutationPolicy] as well
class MutableStateSerializer<T>(private val ser: KSerializer<T>) : KSerializer<MutableState<T>> {
    override val descriptor: SerialDescriptor = ser.descriptor
    override fun serialize(encoder: Encoder, value: MutableState<T>) = ser.serialize(encoder, value.value)
    override fun deserialize(decoder: Decoder) = mutableStateOf(ser.deserialize(decoder))
}

private val cachedGetter = { p: KSerializer<*> -> MutableStateSerializer(p) }.memoize()

@Suppress("UNCHECKED_CAST")
fun <T> mutableStateSerializer(ser: KSerializer<T>) = cachedGetter(ser) as MutableStateSerializer<T>

@Composable
inline fun <reified T> rememberMutableStateInDataStore(
    key: String,
    crossinline defaultValue: @DisallowComposableCalls () -> T,
) = with(mutableStateSerializer(serializer<T>())) {
    Cbor.rememberInDataStoreWithExplicitKSerializer(key) {
        mutableStateOf(defaultValue())
    }
}

context(KSerializer<T>)
@Stable
@Composable
inline fun <reified T> BinaryFormat.rememberInDataStoreWithExplicitKSerializer(
    key: String,
    crossinline defaultValue: @DisallowComposableCalls () -> T,
) = with(dataStateFlow) {
    remember {
        value[byteArrayPreferencesKey(key)]?.let { bytes ->
            decodeFromByteArray(implicit<KSerializer<T>>(), bytes)
        } ?: defaultValue()
    }.also { value ->
        LifecycleResumeEffect(key) {
            onPauseOrDispose {
                dataStoreScope.launch {
                    dataStore.edit { p ->
                        p[byteArrayPreferencesKey(key)] = encodeToByteArray(
                            implicit<KSerializer<T>>(),
                            value,
                        )
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
