package com.hippo.ehviewer.ui.scene

import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.fragment.app.Fragment

/**
 * compose view with fragment view lifecycle
 * use it after [Fragment.onCreateView]
 * also @see [Fragment.getViewLifecycleOwner]
 */
@Suppress("FunctionName")
fun Fragment.ComposeWithViewLifecycle() = ComposeView(requireContext()).apply {
    setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnLifecycleDestroyed(viewLifecycleOwner))
}
