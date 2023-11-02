package com.hippo.ehviewer.ui.tools

import androidx.compose.material3.windowsizeclass.WindowSizeClass
import androidx.compose.runtime.compositionLocalOf

val LocalWindowSizeClass = compositionLocalOf<WindowSizeClass> { error("CompositionLocal LocalWindowSizeClass not present!") }
