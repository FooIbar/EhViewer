package com.hippo.ehviewer.ui.tools

import android.graphics.RuntimeShader
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ShaderBrush
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.painter.BrushPainter
import androidx.compose.ui.graphics.painter.Painter
import com.hippo.ehviewer.util.isAtLeastT

private val sksl = """
uniform float uTime;
uniform vec3 uResolution;

vec4 main( vec2 fragCoord )
{
    float mr = min(uResolution.x, uResolution.y);
    vec2 uv = (fragCoord * 2.0 - uResolution.xy) / mr;

    float d = -uTime * 0.5;
    float a = 0.0;
    for (float i = 0.0; i < 8.0; ++i) {
        a += cos(i - d - a * uv.x);
        d += sin(uv.y * i + a);
    }
    d += uTime * 0.5;
    vec3 col = vec3(cos(uv * vec2(d, a)) * 0.6 + 0.4, cos(a + d) * 0.5 + 0.5);
    col = cos(col * cos(vec3(d, a, 2.5)) * 0.5 + 0.5);
    return vec4(col,1.0);
}
"""

val thumbPlaceholder = if (isAtLeastT) {
    object : Painter() {
        private var draws by mutableFloatStateOf(0f)
        private val shader = RuntimeShader(sksl)
        private val brush = object : ShaderBrush() {
            override fun createShader(size: Size) = shader
        }
        override val intrinsicSize = Size.Unspecified
        override fun DrawScope.onDraw() {
            val (width, height) = size
            shader.setFloatUniform("uResolution", width, height, width / height)
            shader.setFloatUniform("uTime", draws++ / 100)
            drawRect(brush = brush)
        }
    }
} else {
    BrushPainter(Brush.linearGradient(listOf(Color.Transparent)))
}
