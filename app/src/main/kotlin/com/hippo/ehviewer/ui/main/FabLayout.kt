package com.hippo.ehviewer.ui.main

import androidx.activity.compose.PredictiveBackHandler
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.AnimationSpec
import androidx.compose.animation.core.EaseOut
import androidx.compose.animation.core.tween
import androidx.compose.foundation.MutatePriority
import androidx.compose.foundation.MutatorMutex
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.SmallFloatingActionButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.layout
import androidx.compose.ui.unit.dp
import androidx.compose.ui.util.lerp
import com.hippo.ehviewer.ui.tools.delegateSnapshotUpdate
import com.hippo.ehviewer.ui.tools.snackBarPadding
import eu.kanade.tachiyomi.util.lang.launchIO
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import moe.tarsin.coroutines.onEachLatest
import moe.tarsin.coroutines.runSuspendCatching

enum class FabLayoutValue {
    Hidden,
    Primary,
    Expand,
}

@Composable
fun rememberFabLayoutState(initialValue: FabLayoutValue = FabLayoutValue.Primary): FabLayoutState {
    return remember { FabLayoutState(initialValue) }
}

@Stable
class FabLayoutState(
    initialValue: FabLayoutValue,
    private val animationSpec: AnimationSpec<Float> = tween(FAB_ANIMATE_TIME),
) {
    val mutatorMutex = MutatorMutex()

    val appearProgress = Animatable(if (initialValue != FabLayoutValue.Hidden) 1f else 0f)
        .apply { updateBounds(0f, 1f) }

    val expandProgress = Animatable(if (initialValue == FabLayoutValue.Expand) 1f else 0f)
        .apply { updateBounds(0f, 1f) }

    private suspend fun downTo(value: FabLayoutValue, priority: MutatePriority) {
        mutatorMutex.mutate(priority) {
            if (expandProgress.value != 0f) expandProgress.animateTo(0f, animationSpec)
            if (value == FabLayoutValue.Hidden) {
                appearProgress.animateTo(0f, animationSpec)
            }
        }
    }

    private suspend fun upTo(value: FabLayoutValue, priority: MutatePriority) {
        mutatorMutex.mutate(priority) {
            if (appearProgress.value != 1f) appearProgress.animateTo(1f, animationSpec)
            if (value == FabLayoutValue.Expand) {
                expandProgress.animateTo(1f, animationSpec)
            }
        }
    }

    suspend fun waitCollapse() {
        if (expandProgress.value != 0f) {
            snapshotFlow { expandProgress.value }.first { it == 0f }
        }
    }
    suspend fun show(priority: MutatePriority = MutatePriority.Default) = upTo(FabLayoutValue.Primary, priority)
    suspend fun hide(priority: MutatePriority = MutatePriority.Default) = downTo(FabLayoutValue.Hidden, priority)
    suspend fun collapse(priority: MutatePriority = MutatePriority.Default) = downTo(FabLayoutValue.Primary, priority)
    suspend fun expand(priority: MutatePriority = MutatePriority.Default) = upTo(FabLayoutValue.Expand, priority)
}

fun interface FabBuilder {
    fun onClick(icon: ImageVector, that: suspend () -> Unit)
}

@Composable
fun FabLayout(
    hidden: Boolean,
    expanded: Boolean,
    onExpandChanged: (Boolean) -> Unit,
    autoCancel: Boolean,
    fabBuilder: FabBuilder.() -> Unit,
) {
    val updatedHidden by rememberUpdatedState(hidden)
    val updatedExpanded by rememberUpdatedState(expanded)
    val newState = when {
        updatedHidden -> FabLayoutValue.Hidden
        updatedExpanded -> FabLayoutValue.Expand
        else -> FabLayoutValue.Primary
    }
    val state = rememberFabLayoutState(newState)
    LaunchedEffect(newState) {
        when (newState) {
            FabLayoutValue.Expand -> state.expand()
            FabLayoutValue.Hidden -> state.hide()
            FabLayoutValue.Primary -> {
                state.collapse()
                state.show()
            }
        }
    }
    val builder by rememberUpdatedState(fabBuilder)

    val secondaryFab by delegateSnapshotUpdate {
        record { buildFab(builder) }
        transform { onEachLatest { state.waitCollapse() } }
    }
    val coroutineScope = rememberCoroutineScope()
    if (updatedExpanded && autoCancel) {
        Spacer(
            modifier = Modifier.fillMaxSize().pointerInput(Unit) {
                detectTapGestures { onExpandChanged(false) }
            },
        )
    }
    val appearState by state.appearProgress.asState()
    Box(
        modifier = Modifier.fillMaxSize().navigationBarsPadding().padding(16.dp).snackBarPadding(),
        contentAlignment = Alignment.BottomEnd,
    ) {
        Box(
            modifier = Modifier.graphicsLayer {
                rotationZ = lerp(-90f, 0f, appearState)
                scaleX = appearState
                scaleY = appearState
            },
            contentAlignment = Alignment.Center,
        ) {
            val animatedProgress by state.expandProgress.asState()
            PredictiveBackHandler(updatedExpanded) { flow ->
                try {
                    state.mutatorMutex.mutate(MutatePriority.UserInput) {
                        flow.collect {
                            val eased = EaseOut.transform(it.progress)
                            state.expandProgress.snapTo(1 - eased)
                        }
                    }
                    onExpandChanged(false)
                } catch (e: CancellationException) {
                    coroutineScope.launch {
                        state.expand()
                    }
                }
            }
            FloatingActionButton(onClick = { onExpandChanged(!updatedExpanded) }) {
                Icon(
                    imageVector = Icons.Default.Close,
                    contentDescription = null,
                    modifier = Modifier.graphicsLayer {
                        rotationZ = lerp(-135f, 0f, animatedProgress)
                    },
                )
            }
            secondaryFab.run {
                forEachIndexed { index, (imageVector, onClick) ->
                    SmallFloatingActionButton(
                        onClick = {
                            coroutineScope.launchIO {
                                runSuspendCatching {
                                    onClick()
                                }
                                onExpandChanged(false)
                            }
                        },
                        modifier = Modifier.layout { measurable, constraints ->
                            val placeable = measurable.measure(constraints)
                            layout(placeable.width, placeable.height) {
                                val distance = lerp(0, 150 * (size - index) + 50, animatedProgress)
                                placeable.placeRelative(0, -distance, -(size - index).toFloat())
                            }
                        }.graphicsLayer {
                            alpha = animatedProgress
                        },
                    ) {
                        Icon(imageVector = imageVector, contentDescription = null)
                    }
                }
            }
        }
    }
}

private fun buildFab(builder: FabBuilder.() -> Unit) = buildList {
    builder { icon, action ->
        add(icon to action)
    }
}

const val FAB_ANIMATE_TIME = 300
