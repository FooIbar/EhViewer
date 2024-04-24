package com.hippo.ehviewer.ui.screen

// https://github.com/Kotlin/KEEP/blob/context-parameters/proposals/context-parameters.md#standard-library-support
context(T)
inline fun <reified T> implicit(): T = this@T
