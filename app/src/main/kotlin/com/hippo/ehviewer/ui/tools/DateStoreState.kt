package com.hippo.ehviewer.ui.tools

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisallowComposableCalls
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.byteArrayPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.preferencesDataStoreFile
import androidx.lifecycle.compose.LifecycleResumeEffect
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.serialization.KSerializer
import kotlinx.serialization.cbor.Cbor
import kotlinx.serialization.serializer
import splitties.init.appCtx

val dataStoreScope = CoroutineScope(Dispatchers.IO + SupervisorJob())
val dataStore = PreferenceDataStoreFactory.create { appCtx.preferencesDataStoreFile("Remembered") }
val dataStateFlow = dataStore.data.stateIn(dataStoreScope, SharingStarted.Eagerly, emptyPreferences())

// Find out how make this work with any generic `Serializable` type
@Composable
inline fun <reified T> rememberMutableStateInDataStore(
    key: String,
    serializer: KSerializer<T> = serializer<T>(),
    crossinline defaultValue: @DisallowComposableCalls () -> T,
) = with(dataStateFlow) {
    remember {
        val keyObj = byteArrayPreferencesKey(key)
        val r = value[keyObj]?.let { bytes -> Cbor.decodeFromByteArray(serializer, bytes) } ?: defaultValue()
        mutableStateOf(r)
    }.also { mutableState ->
        LifecycleResumeEffect(key) {
            onPauseOrDispose {
                dataStoreScope.launch {
                    val keyObj = byteArrayPreferencesKey(key)
                    dataStore.edit { p ->
                        p[keyObj] = Cbor.encodeToByteArray(serializer, mutableState.value)
                    }
                }
            }
        }
    }
}
