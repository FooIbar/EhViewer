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

inline fun Modifier.thenIf(condition: Boolean, crossinline block: Modifier.() -> Modifier) =
    if (condition) block() else this

val NoopTransitionsVisibilityScope = TransitionsVisibilityScope(emptySet())

@JvmInline
@Stable
value class TransitionsVisibilityScope(val scopes: Set<AnimatedVisibilityScope>)

context(TransitionsVisibilityScope)
@Composable
inline fun <T> togetherWith(scope: AnimatedVisibilityScope, block: @Composable TransitionsVisibilityScope.() -> T) =
    block(remember(scopes, scope) { TransitionsVisibilityScope(scopes + scope) })

context(SharedTransitionScope, TransitionsVisibilityScope)
@Composable
fun Modifier.sharedBounds(
    key: String,
    enter: EnterTransition = fadeIn(),
    exit: ExitTransition = fadeOut(),
) = scopes.fold(this) { modifier, scope ->
    modifier.sharedBounds(
        rememberSharedContentState("$key + ${scope.transition.label}"),
        scope,
        enter,
        exit,
    )
}
