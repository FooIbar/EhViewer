package com.hippo.ehviewer.icons.reader

import androidx.compose.foundation.Image
import androidx.compose.material.icons.materialIcon
import androidx.compose.material.icons.materialPath
import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import com.hippo.ehviewer.icons.EhIcons
import com.hippo.ehviewer.util.unsafeLazy

val EhIcons.Reader.PortraitLocked by unsafeLazy {
    materialIcon(name = "PortraitLocked") {
        materialPath {
            moveTo(10.0F, 16.0F)
            horizontalLineToRelative(4.0F)
            curveToRelative(0.55F, 0.0F, 1.0F, -0.45F, 1.0F, -1.0F)
            verticalLineToRelative(-3.0F)
            curveToRelative(0.0F, -0.55F, -0.45F, -1.0F, -1.0F, -1.0F)
            verticalLineToRelative(-1.0F)
            curveToRelative(0.0F, -1.11F, -0.9F, -2.0F, -2.0F, -2.0F)
            curveToRelative(-1.11F, 0.0F, -2.0F, 0.9F, -2.0F, 2.0F)
            verticalLineToRelative(1.0F)
            curveToRelative(-0.55F, 0.0F, -1.0F, 0.45F, -1.0F, 1.0F)
            verticalLineToRelative(3.0F)
            curveToRelative(0.0F, 0.55F, 0.45F, 1.0F, 1.0F, 1.0F)
            close()
            moveTo(10.8F, 10.0F)
            curveToRelative(0.0F, -0.66F, 0.54F, -1.2F, 1.2F, -1.2F)
            curveToRelative(0.66F, 0.0F, 1.2F, 0.54F, 1.2F, 1.2F)
            verticalLineToRelative(1.0F)
            horizontalLineToRelative(-2.4F)
            verticalLineToRelative(-1.0F)
            close()
            moveTo(17.0F, 1.0F)
            lineTo(7.0F, 1.0F)
            curveToRelative(-1.1F, 0.0F, -2.0F, 0.9F, -2.0F, 2.0F)
            verticalLineToRelative(18.0F)
            curveToRelative(0.0F, 1.1F, 0.9F, 2.0F, 2.0F, 2.0F)
            horizontalLineToRelative(10.0F)
            curveToRelative(1.1F, 0.0F, 2.0F, -0.9F, 2.0F, -2.0F)
            lineTo(19.0F, 3.0F)
            curveToRelative(0.0F, -1.1F, -0.9F, -2.0F, -2.0F, -2.0F)
            close()
            moveTo(17.0F, 19.0F)
            lineTo(7.0F, 19.0F)
            lineTo(7.0F, 5.0F)
            horizontalLineToRelative(10.0F)
            verticalLineToRelative(14.0F)
            close()
        }
    }
}

@Preview
@Composable
@Suppress("UnusedPrivateMember")
private fun IconPortraitLockedPreview() {
    Image(imageVector = EhIcons.Reader.PortraitLocked, contentDescription = null)
}
