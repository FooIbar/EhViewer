package com.hippo.ehviewer.ui.tools

import android.graphics.RuntimeShader
import android.os.Build
import androidx.annotation.RequiresApi
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
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

// Dyn colors
uniform vec3 primaryContainer;
uniform vec3 primary;
uniform vec3 secondary;
uniform vec3 tertiary;

vec4 main(vec2 fragCoord)
{
    float smoothness = 0.002;
    vec2 uv = fragCoord / dimen;

    vec3 waveAppleColor = primary;
    vec3 waveButterColor = tertiary;
    vec3 deepWaterColor = secondary;
    
    // Wave Apple
    float waveProgress = sin(t) * 0.35 + 0.5;
    waveProgress += (0.01 * sin(uv.x * 35. + t*2.)) + (0.005 * sin(uv.x * 20. + t*0.5));
    float waveOp = smoothstep(waveProgress, waveProgress + smoothness, uv.y);
    vec3 mixed = mix(primaryContainer, waveAppleColor, waveOp);
    
    // Wave Butter
    waveProgress = sin(t) * 0.35 + 0.5;
    waveProgress += (0.02 * sin(uv.x * 20. + t*2.)) + (0.005 * sin(uv.x * 30. + t*0.7));
    float waveTop = smoothstep(waveProgress, waveProgress + 0.005, uv.y);
    vec3 gradDeepWater = mix(deepWaterColor, waveButterColor, 1. - uv.y * 0.5);
    vec3 harmonized = mix(waveAppleColor, gradDeepWater, waveTop);
    waveOp = smoothstep(waveProgress, waveProgress + smoothness, uv.y);
    mixed = mix(mixed, harmonized, waveOp);
    return vec4(mixed, 1);
}
"""

@Composable
fun UpdateShaderColor() {
    if (isAtLeastT) {
        fun configure(name: String, color: Color) = shader.setFloatUniform(name, color.red, color.green, color.blue)
        configure("primaryContainer", MaterialTheme.colorScheme.primaryContainer)
        configure("primary", MaterialTheme.colorScheme.primary)
        configure("secondary", MaterialTheme.colorScheme.secondary)
        configure("tertiary", MaterialTheme.colorScheme.tertiary)
    }
}

@RequiresApi(Build.VERSION_CODES.TIRAMISU)
private val shader = RuntimeShader(sksl)

val thumbPlaceholder = if (isAtLeastT) {
    object : Painter() {
        var draws by mutableFloatStateOf(0f)
        val brush = object : ShaderBrush() {
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
