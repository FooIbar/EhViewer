package com.hippo.ehviewer.ui.legacy

import android.content.Context
import android.util.AttributeSet
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.SmallFloatingActionButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
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

typealias OnExpandStateListener = (Boolean) -> Unit

class ComposeFabLayout @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyle: Int = 0,
) : AbstractComposeView(context, attrs, defStyle) {
    private var expandable by mutableStateOf(true)
    var onPrimaryFabClick by mutableStateOf<(() -> Unit)?>(null)
    var primaryFabIcon by mutableStateOf(Icons.Default.Add)
    var secondaryFab by mutableStateOf<List<Pair<ImageVector, () -> Unit>>?>(null)

    private var hidden by mutableStateOf(false)
    private val expandedBackingField = mutableStateOf(false)
    private var expanded by expandedBackingField
    private val listeners = mutableListOf<OnExpandStateListener>()

    fun addOnExpandStateListener(listener: OnExpandStateListener) {
        listeners.add(listener)
    }

    fun show() {
        hidden = true
    }

    fun hide() {
        expanded = false
        hidden = false
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
                Spacer(
                    modifier = Modifier.fillMaxSize().pointerInput(expanded) {
                        awaitPointerEventScope { expanded = false }
                    },
                )
            }
            val hiddenState by animateFloatAsState(
                targetValue = if (hidden) 1f else 0f,
                animationSpec = tween(300),
                label = "hiddenState",
            )
            Box(
                modifier = Modifier.fillMaxSize().padding(16.dp),
                contentAlignment = Alignment.BottomEnd,
            ) {
                Box(contentAlignment = Alignment.Center) {
                    val animatedProgress by animateFloatMergePredictiveBackAsState(expandedBackingField)
                    FloatingActionButton(
                        onClick = {
                            if (expandable) {
                                expanded = !expanded
                            } else {
                                onPrimaryFabClick?.invoke()
                            }
                        },
                        modifier = Modifier.rotate(lerp(90f, 0f, hiddenState)).scale(hiddenState),
                    ) {
                        Icon(
                            imageVector = primaryFabIcon,
                            contentDescription = null,
                            modifier = Modifier.rotate(lerp(135f, 0f, animatedProgress)),
                        )
                    }
                    secondaryFab?.forEachIndexed { index, (imageVector, onClick) ->
                        SmallFloatingActionButton(
                            onClick = {
                                expanded = false
                                onClick()
                            },
                            modifier = Modifier.layout { measurable, constraints ->
                                val placeable = measurable.measure(constraints)
                                layout(placeable.width, placeable.height) {
                                    val distance = lerp((150 * (index + 1) + 50), 0, animatedProgress)
                                    placeable.placeRelative(0, -distance, -(index + 1).toFloat())
                                }
                            }.rotate(lerp(90f, 0f, hiddenState)).scale(hiddenState),
                        ) {
                            Icon(imageVector = imageVector, contentDescription = null)
                        }
                    }
                }
            }
        }
    }
}
