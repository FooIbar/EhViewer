package com.hippo.ehviewer.util

import androidx.compose.runtime.snapshots.SnapshotStateMap
import androidx.compose.ui.util.fastForEach

fun <T> MutableList<T>.insertWith(element: T, comparator: Comparator<T>) {
    val index = binarySearch(element, comparator).let {
        if (it < 0) -it - 1 else it
    }
    add(index, element)
}

inline fun <T> List<List<T>>.flattenForEach(action: (T) -> Unit) = fastForEach { list ->
    list.fastForEach { item -> action(item) }
}

fun <K, V> SnapshotStateMap<K, V>.takeAndClear() = toMap().values.also { clear() }
