package com.hippo.ehviewer.ui.tools

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.gestures.draggable2D
import androidx.compose.foundation.gestures.rememberDraggable2DState
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.offset
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalWindowInfo
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.round
import androidx.compose.ui.util.lerp
import kotlinx.serialization.KSerializer
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder

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
    val info = LocalWindowInfo.current.containerSize

    // Disappear at screen center
    val disappear = IntOffset(info.width / 2, 0)

    // Type annotation for Serializer
    var idle by rememberMutableStateInDataStore("DialogBubble", OffsetSerializer) { Offset.Zero }
    val state = rememberDraggable2DState { delta -> idle += delta }
    FilledTonalIconButton(
        onClick = { showDialog = true },
        modifier = Modifier.offset { lerp(disappear, idle.round(), fraction) }.graphicsLayer { alpha = fraction }.align(Alignment.CenterStart).draggable2D(state = state),
    ) {
        Icon(imageVector = idleIcon, contentDescription = null)
    }
}
