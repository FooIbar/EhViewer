package com.hippo.ehviewer.util

inline fun <R : Any, C : MutableCollection<in R>> LongArray.mapNotNullTo(destination: C, transform: (Long) -> R?): C {
    forEach { element -> transform(element)?.let { destination.add(it) } }
    return destination
}

inline fun <T> Collection<T>.mapToLongArray(transform: (T) -> Long): LongArray {
    val result = LongArray(size)
    var index = 0
    for (element in this)
        result[index++] = transform(element)
    return result
}

inline fun <R : Any> LongArray.mapNotNull(transform: (Long) -> R?): List<R> {
    return mapNotNullTo(ArrayList(), transform)
}
