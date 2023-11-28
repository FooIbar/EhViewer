package com.hippo.ehviewer.ui.tools

import androidx.activity.BackEventCompat
import androidx.activity.compose.PredictiveBackHandler
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.AnimationSpec
import androidx.compose.animation.core.EaseOut
import androidx.compose.animation.core.Easing
import androidx.compose.animation.core.VectorConverter
import androidx.compose.animation.core.spring
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.State
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.LayoutDirection
import eu.kanade.tachiyomi.util.lang.withNonCancellableContext
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.launch

/* enable               0               1
 * animationProgress    1F              0F
 *
 * BackHandler enabling:
 *     enable 0 -> 1 immediately
 *     animationProgress 1F -> 0F by animationSpec
 *
 * BackGesture handling:
 *     enable 1 -> 0 when finished
 *     animationProgress 0F -> 1F
 *         SDK >= 34 by userGesture progress
 *         else by animationSpec when finished
 */
@Stable
@Composable
fun animateFloatMergePredictiveBackAsState(
    enable: Boolean,
    animationSpec: AnimationSpec<Float> = spring(),
    predictiveBackInterpolator: Easing = EaseOut,
    finishedListener: ((Float) -> Unit)? = null,
    onBack: () -> Unit,
): State<Float> {
    val targetValue = if (enable) 0f else 1f
    val animatable = remember { Animatable(targetValue, Float.VectorConverter) }
    val animSpec by rememberUpdatedState(animationSpec)
    val listener by rememberUpdatedState(finishedListener)

    val channel = remember { Channel<Float>(Channel.CONFLATED) }
    SideEffect {
        channel.trySend(targetValue)
    }
    LaunchedEffect(channel) {
        for (target in channel) {
            val newTarget = channel.tryReceive().getOrNull() ?: target
            launch {
                if (newTarget != animatable.targetValue) {
                    animatable.animateTo(newTarget, animSpec)
                    listener?.invoke(animatable.value)
                }
            }
        }
    }

    PredictiveBackHandler(enable) { progress ->
        try {
            progress.collect {
                val transformed = predictiveBackInterpolator.transform(it.progress)
                animatable.snapTo(transformed)
            }
            onBack()
        } catch (e: CancellationException) {
            channel.trySend(0f)
        }
    }
    return animatable.asState()
}

@Composable
fun animateFloatMergeOneWayPredictiveBackAsState(
    enable: Boolean,
    animationSpec: AnimationSpec<Float> = spring(),
    predictiveBackInterpolator: Easing = EaseOut,
    onBack: suspend () -> Unit,
): State<Float> {
    val animatable = remember { Animatable(0f, Float.VectorConverter) }
    val isRtl = LocalLayoutDirection.current == LayoutDirection.Rtl
    PredictiveBackHandler(enable) { progress ->
        try {
            progress.collect {
                val isEdgeStart = (it.swipeEdge == BackEventCompat.EDGE_LEFT).xor(isRtl)
                val transformed = predictiveBackInterpolator.transform(it.progress)
                animatable.snapTo(if (isEdgeStart) transformed else -transformed)
            }
        } catch (e: CancellationException) {
            withNonCancellableContext {
                animatable.animateTo(0f, animationSpec)
            }
            throw e
        }
        withNonCancellableContext {
            onBack()
            animatable.snapTo(0f)
        }
    }
    return animatable.asState()
}
