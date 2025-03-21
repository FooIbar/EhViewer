package com.hippo.ehviewer.ui.tools

import androidx.compose.animation.AnimatedVisibilityScope
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.SharedTransitionScope
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Shape
import kotlin.concurrent.atomics.AtomicInt
import kotlin.concurrent.atomics.fetchAndIncrement

val NoopTransitionsVisibilityScope = TransitionsVisibilityScope(emptySet())

@JvmInline
@Stable
value class TransitionsVisibilityScope(val scopes: Set<AnimatedVisibilityScope>)

context(TransitionsVisibilityScope)
@Composable
inline fun <T> togetherWith(scope: AnimatedVisibilityScope, block: @Composable TransitionsVisibilityScope.() -> T) = block(remember(scopes, scope) { TransitionsVisibilityScope(scopes + scope) })

context(SharedTransitionScope, TransitionsVisibilityScope, SETNodeGenerator)
@Composable
fun Modifier.sharedBounds(
    key: String,
    enter: EnterTransition = fadeIn(),
    exit: ExitTransition = fadeOut(),
) = remember(key) { summon(key) }.let { node ->
    DisposableEffect(key) {
        onDispose {
            dispose(node)
        }
    }
    scopes.fold(this) { modifier, scope ->
        modifier.sharedBounds(
            rememberSharedContentState("${node.syntheticKey} + ${scope.transition.label}"),
            scope,
            enter,
            exit,
        )
    }
}

context(SharedTransitionScope, TransitionsVisibilityScope, SETNodeGenerator)
@Composable
inline fun SharedElementBox(key: String, shape: Shape, crossinline content: @Composable BoxScope.() -> Unit) {
    val modifier = Modifier.sharedBounds(key = key).clip(shape)
    CompositionLocalProvider { Box(modifier = modifier, content = content) }
}

data class SETNode(
    val contentKey: String,
    val uniqueID: String,
)

val SETNode.syntheticKey: String
    get() = contentKey + uniqueID

val listThumbGenerator = SETNodeGenerator()
val detailThumbGenerator = SETNodeGenerator()

fun initSETConnection() {
    listThumbGenerator connectTo detailThumbGenerator
}

private val atomicIncId = AtomicInt(0)

class SETNodeGenerator {
    private val tracker = hashMapOf<String, SETNode>()
    private val opposites = mutableListOf<SETNodeGenerator>()

    fun summon(contentKey: String): SETNode {
        val node = opposites.firstNotNullOfOrNull { it.tracker[contentKey] } ?: SETNode(contentKey = contentKey, uniqueID = "${atomicIncId.fetchAndIncrement()}")
        tracker[contentKey] = node
        return node
    }

    fun dispose(node: SETNode) = tracker.remove(node.contentKey, node)

    infix fun connectTo(other: SETNodeGenerator) {
        opposites.add(other)
        other.opposites.add(this)
    }
}
