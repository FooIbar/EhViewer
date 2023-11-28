package com.hippo.ehviewer.ui.tools

import androidx.compose.runtime.Applier
import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReusableComposition
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCompositionContext
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.VectorPainter
import androidx.compose.ui.graphics.vector.rememberVectorPainter

@Stable
@Composable
fun rememberNeverRecomposeVectorPainter(image: ImageVector): VectorPainter {
    val context = rememberCompositionContext()
    var b by remember { mutableStateOf<VectorPainter?>(null) }
    remember {
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
        composition.setContent {
            b = rememberVectorPainter(image = image)
            composition.deactivate()
        }
    }
    return b!!
}
