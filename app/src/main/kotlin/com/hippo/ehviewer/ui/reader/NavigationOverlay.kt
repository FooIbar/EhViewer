package com.hippo.ehviewer.ui.reader

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import eu.kanade.tachiyomi.ui.reader.viewer.NavigationRegions

@Composable
fun NavigationOverlay(
    visible: Boolean,
    regions: NavigationRegions,
    modifier: Modifier = Modifier,
) {
    AnimatedVisibility(
        visible = visible && regions.isNotEmpty(),
        enter = fadeIn(AnimationSpec),
        exit = fadeOut(AnimationSpec),
    ) {
        BoxWithConstraints(modifier) {
            val fontSize = MaterialTheme.typography.headlineSmall.fontSize
            val style = TextStyle(
                color = Color.White,
                fontSize = fontSize,
            )
            val strokeStyle = TextStyle(
                color = Color.Black,
                fontSize = fontSize,
                drawStyle = Stroke(8f),
            )
            val scaleX = maxWidth
            val scaleY = maxHeight
            regions.forEach { region ->
                val rect = region.rectF
                val color = colorResource(region.type.colorRes)
                val text = stringResource(region.type.nameRes)
                Box(
                    modifier = Modifier.size(scaleX * rect.width(), scaleY * rect.height())
                        .offset(scaleX * rect.left, scaleY * rect.top).background(color),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(text, style = strokeStyle)
                    Text(text, style = style)
                }
            }
        }
    }
}

private val AnimationSpec = tween<Float>(1000)
