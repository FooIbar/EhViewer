package com.hippo.ehviewer.ui.tools

import androidx.activity.compose.PredictiveBackHandler
import androidx.compose.animation.core.AnimationSpec
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import com.hippo.ehviewer.util.isAtLeastU
import kotlin.math.max
import kotlinx.coroutines.CancellationException

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
@Composable
fun animateFloatMergePredictiveBackAsState(
    enable: Boolean,
    animationSpec: AnimationSpec<Float> = spring(),
    finishedListener: ((Float) -> Unit)? = null,
    onBack: () -> Unit,
): State<Float> {
    // Natural animator state correspond with principal value
    val coState by animateFloatAsState(
        targetValue = if (enable) 0f else 1f,
        animationSpec = animationSpec,
        label = "animationProgress",
        finishedListener = finishedListener,
    )
    // User predictive back animation progress holder && value correspond with UI State
    val ret = remember { mutableFloatStateOf(1F) }
    var animationProgress by ret
    // Update UI animation state
    animationProgress = if (enable || !isAtLeastU) coState else max(animationProgress, coState)

    PredictiveBackHandler(enable) { progress ->
        try {
            progress.collect {
                animationProgress = it.progress
            }
            onBack()
        } catch (e: CancellationException) {
            animationProgress = 0F
        }
    }
    return ret
}
