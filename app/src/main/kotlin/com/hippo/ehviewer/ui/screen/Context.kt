package com.hippo.ehviewer.ui.screen

// https://github.com/Kotlin/KEEP/blob/context-parameters/proposals/context-parameters.md#standard-library-support
context(t: T)
inline fun <reified T> implicit() = t
