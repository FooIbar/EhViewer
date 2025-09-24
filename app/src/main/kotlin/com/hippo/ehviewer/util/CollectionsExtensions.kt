package com.hippo.ehviewer.util

fun <T> MutableList<T>.insertWith(element: T, comparator: Comparator<T>) {
    val index = binarySearch(element, comparator).let {
        if (it < 0) -it - 1 else it
    }
    add(index, element)
}
