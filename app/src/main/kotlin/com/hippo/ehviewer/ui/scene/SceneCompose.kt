package com.hippo.ehviewer.ui.scene

import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.fragment.app.Fragment
import com.hippo.ehviewer.ui.setMD3Content

@Suppress("FunctionName")
inline fun Fragment.ComposeWithMD3(crossinline content: @Composable () -> Unit) = ComposeView(requireContext()).apply {
    setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnLifecycleDestroyed(viewLifecycleOwner))
    setMD3Content(content)
}
