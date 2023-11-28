package com.hippo.ehviewer.ui.tools

import androidx.compose.material3.Icon
import androidx.compose.material3.LocalContentColor
import androidx.compose.runtime.Applier
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.ReusableComposition
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCompositionContext
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.VectorPainter
import androidx.compose.ui.graphics.vector.rememberVectorPainter

@Stable
@Composable
private fun rememberNeverRecomposeVectorPainter(image: ImageVector): VectorPainter {
    val context = rememberCompositionContext()
    var b by remember(image) { mutableStateOf<VectorPainter?>(null) }
    val composition = remember(image) {
        val composition = ReusableComposition(
            object : Applier<Unit> {
                override val current = Unit
                override fun clear() = Unit
                override fun move(from: Int, to: Int, count: Int) = Unit
                override fun remove(index: Int, count: Int) = Unit
                override fun up() = Unit
                override fun insertTopDown(index: Int, instance: Unit) = Unit
                override fun insertBottomUp(index: Int, instance: Unit) = Unit
                override fun down(node: Unit) = Unit
            },
            context,
        )
        composition.apply {
            setContent {
                b = rememberVectorPainter(image = image)
                composition.deactivate()
            }
        }
    }
    DisposableEffect(composition) {
        onDispose {
            composition.dispose()
        }
    }
    return b!!
}

@Composable
fun IconFix(
    imageVector: ImageVector,
    contentDescription: String?,
    modifier: Modifier = Modifier,
    tint: Color = LocalContentColor.current,
) {
    Icon(
        painter = rememberNeverRecomposeVectorPainter(imageVector),
        contentDescription = contentDescription,
        modifier = modifier,
        tint = tint,
    )
}
