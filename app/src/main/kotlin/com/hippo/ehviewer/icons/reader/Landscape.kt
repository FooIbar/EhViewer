package com.hippo.ehviewer.icons.reader

import androidx.compose.foundation.Image
import androidx.compose.material.icons.materialIcon
import androidx.compose.material.icons.materialPath
import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import com.hippo.ehviewer.icons.EhIcons
import com.hippo.ehviewer.util.unsafeLazy

val EhIcons.Reader.Landscape by unsafeLazy {
    materialIcon(name = "Landscape") {
        materialPath {
            moveTo(1.01F, 7.0F)
            lineTo(1.0F, 17.0F)
            curveToRelative(0.0F, 1.1F, 0.9F, 2.0F, 2.0F, 2.0F)
            horizontalLineToRelative(18.0F)
            curveToRelative(1.1F, 0.0F, 2.0F, -0.9F, 2.0F, -2.0F)
            verticalLineTo(7.0F)
            curveToRelative(0.0F, -1.1F, -0.9F, -2.0F, -2.0F, -2.0F)
            horizontalLineTo(3.0F)
            curveToRelative(-1.1F, 0.0F, -1.99F, 0.9F, -1.99F, 2.0F)
            close()
            moveTo(19.0F, 7.0F)
            verticalLineToRelative(10.0F)
            horizontalLineTo(5.0F)
            verticalLineTo(7.0F)
            horizontalLineToRelative(14.0F)
            close()
        }
    }
}

@Preview
@Composable
@Suppress("UnusedPrivateMember")
private fun IconLandscapePreview() {
    Image(imageVector = EhIcons.Reader.Landscape, contentDescription = null)
}
