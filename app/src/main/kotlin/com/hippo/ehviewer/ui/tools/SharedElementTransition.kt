package com.hippo.ehviewer.ui.tools

import androidx.compose.animation.AnimatedVisibilityScope
import androidx.compose.animation.BoundsTransform
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.SharedTransitionScope
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.layout.Placeable
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.LayoutDirection
import arrow.atomic.AtomicInt

val NoopTransitionsVisibilityScope = TransitionsVisibilityScope(emptySet())

@JvmInline
@Stable
value class TransitionsVisibilityScope(val scopes: Set<AnimatedVisibilityScope>)

context(TransitionsVisibilityScope)
@Composable
inline fun <T> togetherWith(scope: AnimatedVisibilityScope, block: @Composable TransitionsVisibilityScope.() -> T) =
    block(remember(scopes, scope) { TransitionsVisibilityScope(scopes + scope) })

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

private val atomicIncId = AtomicInt()

class SETNodeGenerator {
    private val tracker = hashMapOf<String, SETNode>()
    private val opposites = mutableListOf<SETNodeGenerator>()

    fun summon(contentKey: String): SETNode {
        val node = opposites.firstNotNullOfOrNull { it.tracker[contentKey] } ?: SETNode(contentKey = contentKey, uniqueID = "${atomicIncId.getAndIncrement()}")
        tracker[contentKey] = node
        return node
    }

    fun dispose(node: SETNode) = tracker.remove(node.contentKey, node)

    infix fun connectTo(other: SETNodeGenerator) {
        opposites.add(other)
        other.opposites.add(this)
    }
}

object NoopSharedTransitionScope : SharedTransitionScope {
    override val isTransitionActive: Boolean
        get() = TODO("Not yet implemented")
    override val Placeable.PlacementScope.lookaheadScopeCoordinates: LayoutCoordinates
        get() = TODO("Not yet implemented")

    override fun OverlayClip(clipShape: Shape): SharedTransitionScope.OverlayClip {
        TODO("Not yet implemented")
    }

    @Composable
    override fun rememberSharedContentState(key: Any): SharedTransitionScope.SharedContentState {
        TODO("Not yet implemented")
    }

    override fun Modifier.renderInSharedTransitionScopeOverlay(
        renderInOverlay: () -> Boolean,
        zIndexInOverlay: Float,
        clipInOverlayDuringTransition: (LayoutDirection, Density) -> Path?,
    ): Modifier {
        TODO("Not yet implemented")
    }

    override fun Modifier.sharedBounds(
        sharedContentState: SharedTransitionScope.SharedContentState,
        animatedVisibilityScope: AnimatedVisibilityScope,
        enter: EnterTransition,
        exit: ExitTransition,
        boundsTransform: BoundsTransform,
        resizeMode: SharedTransitionScope.ResizeMode,
        placeHolderSize: SharedTransitionScope.PlaceHolderSize,
        renderInOverlayDuringTransition: Boolean,
        zIndexInOverlay: Float,
        clipInOverlayDuringTransition: SharedTransitionScope.OverlayClip,
    ): Modifier {
        TODO("Not yet implemented")
    }

    override fun Modifier.sharedElement(
        state: SharedTransitionScope.SharedContentState,
        animatedVisibilityScope: AnimatedVisibilityScope,
        boundsTransform: BoundsTransform,
        placeHolderSize: SharedTransitionScope.PlaceHolderSize,
        renderInOverlayDuringTransition: Boolean,
        zIndexInOverlay: Float,
        clipInOverlayDuringTransition: SharedTransitionScope.OverlayClip,
    ): Modifier {
        TODO("Not yet implemented")
    }

    override fun Modifier.sharedElementWithCallerManagedVisibility(
        sharedContentState: SharedTransitionScope.SharedContentState,
        visible: Boolean,
        boundsTransform: BoundsTransform,
        placeHolderSize: SharedTransitionScope.PlaceHolderSize,
        renderInOverlayDuringTransition: Boolean,
        zIndexInOverlay: Float,
        clipInOverlayDuringTransition: SharedTransitionScope.OverlayClip,
    ): Modifier {
        TODO("Not yet implemented")
    }

    override fun Modifier.skipToLookaheadSize(): Modifier {
        TODO("Not yet implemented")
    }

    override fun LayoutCoordinates.toLookaheadCoordinates(): LayoutCoordinates {
        TODO("Not yet implemented")
    }
}
