package com.ehviewer.core.mainthread

internal expect val isMainThread: Boolean

fun checkMainThread() {
    check(isMainThread) { "Must be called from the main thread" }
}

fun checkNotMainThread() {
    check(!isMainThread) { "Must not be called from the main thread" }
}
