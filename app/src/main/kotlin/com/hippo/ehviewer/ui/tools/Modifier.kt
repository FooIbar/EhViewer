package com.hippo.ehviewer.ui.tools

import androidx.compose.animation.AnimatedVisibilityScope
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.SharedTransitionScope
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale

inline fun Modifier.thenIf(condition: Boolean, crossinline block: Modifier.() -> Modifier) =
    if (condition) block() else this

context(SharedTransitionScope, AnimatedVisibilityScope)
@Composable
fun Modifier.sharedBounds(
    key: Any,
    enter: EnterTransition = fadeIn() + scaleInSharedContentToBounds(ContentScale.Fit),
    exit: ExitTransition = fadeOut() + scaleOutSharedContentToBounds(ContentScale.Fit),
) = sharedBounds(
    rememberSharedContentState(key = key),
    this@AnimatedVisibilityScope,
    enter,
    exit,
)
