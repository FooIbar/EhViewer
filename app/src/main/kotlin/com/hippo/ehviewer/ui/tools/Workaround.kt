package com.hippo.ehviewer.ui.tools

import androidx.compose.material3.Icon
import androidx.compose.material3.LocalContentColor
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Stable
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.VectorPainter
import androidx.compose.ui.graphics.vector.rememberVectorPainter

private inline fun <K, V> MutableMap<K, V>.computeIfAbsentInline(key: K, compute: () -> V) = get(key) ?: compute().also { put(key, it) }

@Stable
@Composable
private fun rememberCachedVectorPainter(image: ImageVector): VectorPainter {
    val cache = LocalVectorPainterCache.current
    return cache.computeIfAbsentInline(image) { rememberVectorPainter(image = image) }
}

@Composable
fun IconCached(
    imageVector: ImageVector,
    contentDescription: String?,
    modifier: Modifier = Modifier,
    tint: Color = LocalContentColor.current,
) {
    Icon(
        painter = rememberCachedVectorPainter(imageVector),
        contentDescription = contentDescription,
        modifier = modifier,
        tint = tint,
    )
}

@Composable
fun ProvideVectorPainterCache(content: @Composable () -> Unit) {
    val cache = remember { mutableMapOf<ImageVector, VectorPainter>() }
    CompositionLocalProvider(
        LocalVectorPainterCache provides cache,
        content = content,
    )
}

typealias VectorPainterCache = MutableMap<ImageVector, VectorPainter>

val LocalVectorPainterCache = compositionLocalOf<VectorPainterCache> { error("CompositionLocal LocalSideSheetState not present!") }
