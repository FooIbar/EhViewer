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
uniform float t;
uniform vec2 dimen;

vec3 toGLColor(vec3 color) 
{
	return color * 0.00392156862;
}

vec4 main(vec2 fragCoord)
{
    float smoothness = 0.008;
    vec2 uv = fragCoord / dimen;
    
    // TODO: Uniform
    vec3 primary = toGLColor(vec3(203, 136, 180));
    vec3 surface = toGLColor(vec3(149, 165, 166));
    vec3 waveAppleColor = toGLColor(vec3(52, 152, 219));
    vec3 waveButterColor = toGLColor(vec3(14, 122, 160));
    
    float realtic = smoothstep(0, smoothness, uv.x) * smoothstep(0, smoothness, 1 - uv.x);
    realtic *= smoothstep(0, smoothness, uv.y) * smoothstep(0, smoothness, 1 - uv.y);
    vec3 mixed = mix(surface, primary, realtic);
    
    // Wave Apple
    float waveProgress = sin(t) * 0.35 + 0.5;
    waveProgress += (0.01 * sin(uv.x * 35. + t*2.)) + (0.005 * sin(uv.x * 20. + t*0.5));
    float waveOp = smoothstep(waveProgress, waveProgress + smoothness, uv.y);
    mixed = mix(mixed, waveAppleColor, waveOp);
    
    // Wave Butter
    waveProgress = sin(t) * 0.35 + 0.5;
    waveProgress += (0.02 * sin(uv.x * 20. + t*2.)) + (0.005 * sin(uv.x * 30. + t*0.7));
    waveOp = smoothstep(waveProgress, waveProgress + smoothness, uv.y);
    mixed = mix(mixed, waveButterColor, waveOp);
	return vec4(mixed, 1);
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
            shader.setFloatUniform("dimen", width, height)
            shader.setFloatUniform("t", draws++ / 120)
            drawRect(brush = brush)
        }
    }
} else {
    BrushPainter(Brush.linearGradient(listOf(Color.Transparent)))
}
