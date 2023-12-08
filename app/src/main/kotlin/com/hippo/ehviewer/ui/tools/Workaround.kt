package com.hippo.ehviewer.ui.tools

import androidx.compose.material3.Icon
import androidx.compose.material3.LocalContentColor
import androidx.compose.runtime.Applier
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Composition
import androidx.compose.runtime.CompositionContext
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.ReusableComposition
import androidx.compose.runtime.Stable
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCompositionContext
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.VectorPainter
import androidx.compose.ui.graphics.vector.rememberVectorPainter

@Stable
@Composable
private fun rememberNeverRecomposeVectorPainter(image: ImageVector): VectorPainter {
    val cache = LocalVectorPainterCache.current
    return remember(image) {
        val (context, map) = cache
        map.computeIfAbsent(image) {
            lateinit var b: VectorPainter
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
            composition to b
        }.second
    }
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

@Composable
fun ProvideVectorPainterCache(content: @Composable () -> Unit) {
    val compositionContext = rememberCompositionContext()
    val cache = remember { mutableMapOf<ImageVector, Pair<Composition, VectorPainter>>() }
    val provides = remember { compositionContext to cache }
    CompositionLocalProvider(
        LocalVectorPainterCache provides provides,
        content = content,
    )
    DisposableEffect(Unit) {
        onDispose {
            cache.values.forEach { (composition, _) -> composition.dispose() }
            cache.clear()
        }
    }
}

typealias VectorPainterCache = Pair<CompositionContext, MutableMap<ImageVector, Pair<Composition, VectorPainter>>>

val LocalVectorPainterCache = compositionLocalOf<VectorPainterCache> { error("CompositionLocal LocalSideSheetState not present!") }
