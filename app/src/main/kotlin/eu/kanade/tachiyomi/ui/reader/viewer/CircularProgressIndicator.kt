package eu.kanade.tachiyomi.ui.reader.viewer

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.material3.CircularWavyProgressIndicator
import androidx.compose.material3.WavyProgressIndicatorDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue

@Composable
fun CombinedCircularProgressIndicator(progress: Float) {
    val animatedProgress by animateFloatAsState(
        targetValue = progress,
        animationSpec = WavyProgressIndicatorDefaults.ProgressAnimationSpec,
        label = "progress",
    )
    AnimatedContent(
        targetState = progress == 0f,
        transitionSpec = { fadeIn() togetherWith fadeOut() },
        label = "progressState",
    ) { indeterminate ->
        if (indeterminate) {
            CircularWavyProgressIndicator()
        } else {
            CircularWavyProgressIndicator(
                progress = { animatedProgress },
            )
        }
    }
}
