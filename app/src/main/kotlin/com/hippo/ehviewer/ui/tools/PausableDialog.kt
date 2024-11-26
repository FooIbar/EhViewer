package com.hippo.ehviewer.ui.tools

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.VectorConverter
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.MutatorMutex
import androidx.compose.foundation.gestures.draggable2D
import androidx.compose.foundation.gestures.rememberDraggable2DState
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.offset
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalWindowInfo
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.round
import androidx.compose.ui.unit.toSize
import androidx.compose.ui.util.lerp
import kotlinx.coroutines.launch
import kotlinx.serialization.KSerializer
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import moe.tarsin.kt.unreachable

fun lerp(start: IntOffset, stop: IntOffset, fraction: Float) = IntOffset(
    lerp(start.x, stop.x, fraction),
    lerp(start.y, stop.y, fraction),
)

object OffsetSerializer : KSerializer<Offset> {
    override val descriptor = PrimitiveSerialDescriptor("Offset", PrimitiveKind.LONG)
    override fun deserialize(decoder: Decoder) = Offset(decoder.decodeLong())
    override fun serialize(encoder: Encoder, value: Offset) = encoder.encodeLong(value.packedValue)
}

@Composable
fun BoxScope.PausableAlertDialog(
    confirmButton: @Composable () -> Unit,
    dismissButton: @Composable () -> Unit,
    title: @Composable () -> Unit,
    text: @Composable () -> Unit,
    idleIcon: ImageVector,
) {
    val scope = rememberCoroutineScope()
    val mutex = remember { MutatorMutex() }
    var showDialog by remember { mutableStateOf(true) }
    if (showDialog) {
        AlertDialog(
            onDismissRequest = { showDialog = false },
            confirmButton = confirmButton,
            dismissButton = dismissButton,
            title = title,
            text = text,
        )
    }
    val fraction by animateFloatAsState(if (showDialog) 0f else 1f)
    val (viewportW, viewportH) = LocalWindowInfo.current.containerSize.toSize()
    val density = LocalDensity.current
    val (buttonW, buttonH) = with(density) { IconButtonDefaults.smallContainerSize().toSize() }
    val padding = with(density) { 4.dp.toPx() }
    val safe = Rect(left = 0f, top = (buttonH - viewportH) / 2, right = viewportW - buttonW, bottom = (viewportH - buttonH) / 2)
    val safeWithPadding = safe.deflate(padding)

    // Disappear at screen center
    val disappear = safeWithPadding.center.round()
    var idle by rememberMutableStateInDataStore("DialogBubble", OffsetSerializer) { Offset.Zero }
    val state = rememberDraggable2DState { delta -> idle += delta }
    FilledTonalIconButton(
        onClick = { showDialog = true },
        modifier = Modifier.offset {
            lerp(disappear, idle.round(), fraction)
        }.graphicsLayer {
            alpha = fraction
        }.align(Alignment.CenterStart).draggable2D(
            state = state,
            onDragStopped = { v ->
                val idleNow = idle
                val (x, y) = idleNow
                val (l, t, r, b) = safeWithPadding
                val dl = x - l
                val dr = r - x
                val dt = y - t
                val db = b - y
                val target = when (minOf(dl, dr, dt, db)) {
                    dl -> Offset(l, y)
                    dr -> Offset(r, y)
                    dt -> Offset(x, t)
                    db -> Offset(x, b)
                    else -> unreachable()
                }
                scope.launch {
                    mutex.mutate {
                        Animatable(idle, Offset.VectorConverter).animateTo(target, initialVelocity = Offset(v.x, v.y)) {
                            idle = value
                        }
                    }
                }
            },
        ),
    ) {
        Icon(imageVector = idleIcon, contentDescription = null)
    }
}
