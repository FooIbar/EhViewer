package com.hippo.ehviewer.ui.main

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.layout
import androidx.compose.ui.unit.dp
import androidx.compose.ui.util.lerp
import com.hippo.ehviewer.ui.tools.animateFloatMergePredictiveBackAsState
import eu.kanade.tachiyomi.util.lang.launchIO
import moe.tarsin.coroutines.runSuspendCatching

fun interface FabBuilder {
    fun onClick(icon: ImageVector, that: suspend () -> Unit)
}

@Composable
fun FabLayout(
    hidden: Boolean,
    expanded: Boolean,
    onExpandChanged: (Boolean) -> Unit,
    autoCancel: Boolean,
    showAddButton: Boolean = true,
    delayOnShow: Boolean = false,
    fabBuilder: FabBuilder.() -> Unit,
) {
    var delayOnExpand by remember { mutableStateOf(hidden) }
    var delayOnHide by remember { mutableStateOf(expanded) }
    val secondaryFab = remember(fabBuilder) {
        buildList {
            fabBuilder { icon, action ->
                add(icon to action)
            }
        }
    }
    if (expanded) {
        if (autoCancel) {
            Spacer(
                modifier = Modifier.fillMaxSize().pointerInput(expanded) {
                    detectTapGestures {
                        onExpandChanged(false)
                    }
                },
            )
        }
    }
    val coroutineScope = rememberCoroutineScope()
    val appearState by animateFloatAsState(
        targetValue = if (hidden) 0f else 1f,
        animationSpec = tween(FAB_ANIMATE_TIME, if (delayOnHide || !hidden && delayOnShow) FAB_ANIMATE_TIME else 0),
        label = "hiddenState",
    ) { delayOnExpand = hidden }
    Box(
        modifier = Modifier.fillMaxSize().navigationBarsPadding().padding(16.dp),
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
            val animatedProgress by animateFloatMergePredictiveBackAsState(
                expanded,
                tween(FAB_ANIMATE_TIME, if (delayOnExpand) FAB_ANIMATE_TIME else 0),
                finishedListener = { delayOnHide = expanded },
            ) {
                onExpandChanged(false)
            }
            FloatingActionButton(
                onClick = { onExpandChanged(!expanded) },
            ) {
                Icon(
                    imageVector = Icons.Default.Close,
                    contentDescription = null,
                    modifier = if (showAddButton) {
                        Modifier.graphicsLayer {
                            rotationZ = lerp(0f, -135f, animatedProgress)
                        }
                    } else {
                        Modifier
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
                                val distance = lerp(150 * (size - index) + 50, 0, animatedProgress)
                                placeable.placeRelative(0, -distance, -(size - index).toFloat())
                            }
                        }.graphicsLayer {
                            alpha = 1 - animatedProgress
                        },
                    ) {
                        Icon(imageVector = imageVector, contentDescription = null)
                    }
                }
            }
        }
    }
}

const val FAB_ANIMATE_TIME = 300
