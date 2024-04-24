package com.hippo.ehviewer.ui.screen

// https://github.com/Kotlin/KEEP/blob/context-parameters/proposals/context-parameters.md#standard-library-support
inline fun <reified T> T.implicit(): T = this
