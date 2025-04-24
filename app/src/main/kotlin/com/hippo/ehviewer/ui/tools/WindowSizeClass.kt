package com.hippo.ehviewer.ui.tools

import androidx.compose.runtime.compositionLocalOf
import androidx.window.core.layout.WindowSizeClass
import androidx.window.core.layout.WindowSizeClass.Companion.WIDTH_DP_EXPANDED_LOWER_BOUND

val LocalWindowSizeClass = compositionLocalOf<WindowSizeClass> { error("CompositionLocal LocalWindowSizeClass not present!") }

val WindowSizeClass.isExpanded
    get() = isWidthAtLeastBreakpoint(WIDTH_DP_EXPANDED_LOWER_BOUND)
