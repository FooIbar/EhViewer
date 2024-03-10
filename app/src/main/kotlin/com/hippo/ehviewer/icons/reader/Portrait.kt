package com.hippo.ehviewer.icons.reader

import androidx.compose.foundation.Image
import androidx.compose.material.icons.materialIcon
import androidx.compose.material.icons.materialPath
import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import com.hippo.ehviewer.icons.EhIcons
import com.hippo.ehviewer.util.unsafeLazy

val EhIcons.Reader.Portrait by unsafeLazy {
    materialIcon(name = "Portrait") {
        materialPath {
            moveTo(17.0F, 1.01F)
            lineTo(7.0F, 1.0F)
            curveToRelative(-1.1F, 0.0F, -1.99F, 0.9F, -1.99F, 2.0F)
            verticalLineToRelative(18.0F)
            curveToRelative(0.0F, 1.1F, 0.89F, 2.0F, 1.99F, 2.0F)
            horizontalLineToRelative(10.0F)
            curveToRelative(1.1F, 0.0F, 2.0F, -0.9F, 2.0F, -2.0F)
            verticalLineTo(3.0F)
            curveToRelative(0.0F, -1.1F, -0.9F, -1.99F, -2.0F, -1.99F)
            close()
            moveTo(17.0F, 19.0F)
            horizontalLineTo(7.0F)
            verticalLineTo(5.0F)
            horizontalLineToRelative(10.0F)
            verticalLineToRelative(14.0F)
            close()
        }
    }
}

@Preview
@Composable
@Suppress("UnusedPrivateMember")
private fun IconPortraitPreview() {
    Image(imageVector = EhIcons.Reader.Portrait, contentDescription = null)
}
