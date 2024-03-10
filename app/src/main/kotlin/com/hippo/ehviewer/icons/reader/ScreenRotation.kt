package com.hippo.ehviewer.icons.reader

import androidx.compose.foundation.Image
import androidx.compose.material.icons.materialIcon
import androidx.compose.material.icons.materialPath
import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import com.hippo.ehviewer.icons.EhIcons
import com.hippo.ehviewer.util.unsafeLazy

val EhIcons.Reader.ScreenRotation by unsafeLazy {
    materialIcon(name = "ScreenRotation") {
        materialPath {
            moveTo(16.48F, 2.52F)
            curveToRelative(3.27F, 1.55F, 5.61F, 4.72F, 5.97F, 8.48F)
            horizontalLineToRelative(1.5F)
            curveTo(23.44F, 4.84F, 18.29F, 0.0F, 12.0F, 0.0F)
            lineToRelative(-0.66F, 0.03F)
            lineToRelative(3.81F, 3.81F)
            lineToRelative(1.33F, -1.32F)
            close()
            moveTo(10.23F, 1.75F)
            curveToRelative(-0.59F, -0.59F, -1.54F, -0.59F, -2.12F, 0.0F)
            lineTo(1.75F, 8.11F)
            curveToRelative(-0.59F, 0.59F, -0.59F, 1.54F, 0.0F, 2.12F)
            lineToRelative(12.02F, 12.02F)
            curveToRelative(0.59F, 0.59F, 1.54F, 0.59F, 2.12F, 0.0F)
            lineToRelative(6.36F, -6.36F)
            curveToRelative(0.59F, -0.59F, 0.59F, -1.54F, 0.0F, -2.12F)
            lineTo(10.23F, 1.75F)
            close()
            moveTo(14.83F, 21.19F)
            lineTo(2.81F, 9.17F)
            lineToRelative(6.36F, -6.36F)
            lineToRelative(12.02F, 12.02F)
            lineToRelative(-6.36F, 6.36F)
            close()
            moveTo(7.52F, 21.48F)
            curveTo(4.25F, 19.94F, 1.91F, 16.76F, 1.55F, 13.0F)
            lineTo(0.05F, 13.0F)
            curveTo(0.56F, 19.16F, 5.71F, 24.0F, 12.0F, 24.0F)
            lineToRelative(0.66F, -0.03F)
            lineToRelative(-3.81F, -3.81F)
            lineToRelative(-1.33F, 1.32F)
            close()
        }
    }
}

@Preview
@Composable
@Suppress("UnusedPrivateMember")
private fun IconScreenRotationPreview() {
    Image(imageVector = EhIcons.Reader.ScreenRotation, contentDescription = null)
}
