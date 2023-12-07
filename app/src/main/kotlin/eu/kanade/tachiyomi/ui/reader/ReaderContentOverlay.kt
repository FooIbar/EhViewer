package eu.kanade.tachiyomi.ui.reader

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Color
import kotlin.math.abs

@Composable
fun ReaderContentOverlay(
    brightness: (() -> Int)?,
    color: (() -> Int)?,
    colorBlendMode: BlendMode = BlendMode.SrcOver,
) {
    if (brightness != null) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val alpha = abs(brightness()) / 100f
            drawRect(Color.Black, alpha = alpha)
        }
    }

    if (color != null) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            drawRect(
                color = Color(color()),
                blendMode = colorBlendMode,
            )
        }
    }
}
