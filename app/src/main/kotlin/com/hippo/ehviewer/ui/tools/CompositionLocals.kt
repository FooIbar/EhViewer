package com.hippo.ehviewer.ui.tools

import androidx.compose.runtime.compositionLocalOf
import androidx.window.core.layout.WindowSizeClass

val LocalWindowSizeClass = compositionLocalOf<WindowSizeClass> { error("CompositionLocal LocalWindowSizeClass not present!") }
