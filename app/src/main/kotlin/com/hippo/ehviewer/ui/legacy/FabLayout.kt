package com.hippo.ehviewer.ui.legacy

import android.content.Context
import android.util.AttributeSet
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.SmallFloatingActionButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.layout
import androidx.compose.ui.platform.AbstractComposeView
import androidx.compose.ui.unit.dp
import androidx.compose.ui.util.lerp
import com.google.accompanist.themeadapter.material3.Mdc3Theme
import com.hippo.ehviewer.ui.tools.animateFloatMergePredictiveBackAsState
import eu.kanade.tachiyomi.util.lang.launchIO
import moe.tarsin.coroutines.runSuspendCatching

typealias OnExpandStateListener = (Boolean) -> Unit
typealias SecondaryFab = Pair<ImageVector, suspend () -> Unit>

class FabLayout @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyle: Int = 0,
) : AbstractComposeView(context, attrs, defStyle) {
    var secondaryFab by mutableStateOf<List<SecondaryFab>?>(null)
    var autoCancel by mutableStateOf(true)

    private var hidden by mutableStateOf(false)
    var expanded by mutableStateOf(false)
    private val listeners = mutableListOf<OnExpandStateListener>()

    private var mHidden by mutableStateOf(hidden)
    private var mExpanded by mutableStateOf(expanded)
    private var showPrimaryFab by mutableStateOf(true)

    fun addOnExpandStateListener(listener: OnExpandStateListener) {
        listeners.add(listener)
    }

    fun show() {
        hidden = false
    }

    fun hide() {
        expanded = false
        hidden = true
    }

    fun hidePrimaryFab() {
        mHidden = true
        showPrimaryFab = false
        hide()
    }

    @Composable
    override fun Content() {
        Mdc3Theme {
            if (expanded) {
                DisposableEffect(Unit) {
                    listeners.forEach { it.invoke(true) }
                    onDispose {
                        listeners.forEach { it.invoke(false) }
                    }
                }
                if (autoCancel) {
                    Spacer(
                        modifier = Modifier.fillMaxSize().pointerInput(expanded) {
                            detectTapGestures {
                                expanded = false
                            }
                        },
                    )
                }
            }
            val coroutineScope = rememberCoroutineScope()
            val appearState by animateFloatAsState(
                targetValue = if (hidden) 0f else 1f,
                animationSpec = tween(FAB_ANIMATE_TIME, if (mExpanded) FAB_ANIMATE_TIME else 0),
                label = "hiddenState",
            ) { mHidden = hidden }
            Box(
                modifier = Modifier.fillMaxSize().padding(16.dp),
                contentAlignment = Alignment.BottomEnd,
            ) {
                Box(
                    modifier = Modifier.rotate(lerp(-90f, 0f, appearState)).scale(appearState),
                    contentAlignment = Alignment.Center,
                ) {
                    val animatedProgress by animateFloatMergePredictiveBackAsState(
                        enable = expanded,
                        animationSpec = tween(FAB_ANIMATE_TIME, if (mHidden) FAB_ANIMATE_TIME else 0),
                        finishedListener = { mExpanded = expanded },
                    ) { expanded = false }
                    FloatingActionButton(
                        onClick = { expanded = !expanded },
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = null,
                            modifier = if (showPrimaryFab) {
                                Modifier.rotate(lerp(0f, -135f, animatedProgress))
                            } else {
                                Modifier
                            },
                        )
                    }
                    secondaryFab?.run {
                        forEachIndexed { index, (imageVector, onClick) ->
                            SmallFloatingActionButton(
                                onClick = {
                                    coroutineScope.launchIO {
                                        runSuspendCatching {
                                            onClick()
                                        }
                                        expanded = false
                                    }
                                },
                                modifier = Modifier.layout { measurable, constraints ->
                                    val placeable = measurable.measure(constraints)
                                    layout(placeable.width, placeable.height) {
                                        val distance = lerp(150 * (size - index) + 50, 0, animatedProgress)
                                        placeable.placeRelative(0, -distance, -(size - index).toFloat())
                                    }
                                }.alpha(1 - animatedProgress),
                            ) {
                                Icon(imageVector = imageVector, contentDescription = null)
                            }
                        }
                    }
                }
            }
        }
    }
}

const val FAB_ANIMATE_TIME = 300
