package com.hippo.ehviewer.ui.tools

import androidx.compose.animation.AnimatedVisibilityScope
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.SharedTransitionScope
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale

inline fun Modifier.thenIf(condition: Boolean, crossinline block: Modifier.() -> Modifier) =
    if (condition) block() else this

val NoopTransitionsVisibilityScope = TransitionsVisibilityScope(emptySet())

@Stable
class TransitionsVisibilityScope(val scopes: Set<AnimatedVisibilityScope>)

context(TransitionsVisibilityScope)
@Composable
inline fun <T> AnimatedVisibilityScope.advance(block: @Composable TransitionsVisibilityScope.() -> T) = block(remember(scopes, this) { TransitionsVisibilityScope(scopes + this) })

context(SharedTransitionScope, TransitionsVisibilityScope)
@Composable
fun Modifier.sharedBounds(
    key: Any,
    enter: EnterTransition = fadeIn() + scaleInSharedContentToBounds(ContentScale.Fit),
    exit: ExitTransition = fadeOut() + scaleOutSharedContentToBounds(ContentScale.Fit),
) = scopes.fold(this) { modifier, scope ->
    modifier.sharedBounds(
        rememberSharedContentState(key = key),
        scope,
        enter,
        exit,
    )
}
