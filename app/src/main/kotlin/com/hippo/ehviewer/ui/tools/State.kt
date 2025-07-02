package com.hippo.ehviewer.ui.tools

import android.os.Bundle
import android.os.Parcelable
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.Saver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.savedstate.compose.serialization.serializers.MutableStateSerializer
import androidx.savedstate.serialization.SavedStateConfiguration
import androidx.savedstate.serialization.decodeFromSavedState
import androidx.savedstate.serialization.encodeToSavedState
import kotlin.reflect.KMutableProperty0
import kotlinx.parcelize.IgnoredOnParcel
import kotlinx.parcelize.Parcelize
import kotlinx.serialization.KSerializer
import kotlinx.serialization.serializer

val <T> KMutableProperty0<T>.observed: MutableState<T>
    @Composable
    get() = remember {
        val mutableState = mutableStateOf(this.get())
        object : MutableState<T> by mutableState {
            override var value: T
                get() = mutableState.value
                set(value) {
                    this@observed.set(value)
                    mutableState.value = value
                }
        }
    }

val <T> MutableState<T>.rememberedAccessor: KMutableProperty0<T>
    @Composable
    get() = remember {
        object {
            var value by this@rememberedAccessor
        }::value
    }

@Parcelize
data class SavedStateExt<T : Any>(
    val bundle: Bundle,
) : Parcelable {
    @IgnoredOnParcel
    var data: T? = null
}

fun <Serializable : Any> saver(
    serializer: KSerializer<Serializable>,
    configuration: SavedStateConfiguration = SavedStateConfiguration.DEFAULT,
) = Saver(
    save = { original: Serializable ->
        val bundle = encodeToSavedState(serializer, original, configuration)
        SavedStateExt<Serializable>(bundle).apply {
            data = original
        }
    },
    restore = { savedState ->
        savedState.data ?: decodeFromSavedState(serializer, savedState.bundle, configuration).also {
            savedState.data = it
        }
    },
)

@Composable
inline fun <reified T : Any> rememberSerializable(
    vararg inputs: Any?,
    serializer: KSerializer<T> = serializer<T>(),
    configuration: SavedStateConfiguration = SavedStateConfiguration.DEFAULT,
    noinline init: () -> MutableState<T>,
): MutableState<T> {
    val saver = saver(MutableStateSerializer(serializer), configuration)
    return rememberSaveable(*inputs, saver = saver, init = init)
}
