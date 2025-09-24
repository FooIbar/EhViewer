package com.ehviewer.core.ui.util

import androidx.compose.runtime.snapshots.SnapshotStateMap
import androidx.compose.ui.util.fastForEach

inline fun <T> List<List<T>>.flattenForEach(action: (T) -> Unit) = fastForEach { list ->
    list.fastForEach { item -> action(item) }
}

fun <K, V> SnapshotStateMap<K, V>.takeAndClear() = toMap().values.also { clear() }
