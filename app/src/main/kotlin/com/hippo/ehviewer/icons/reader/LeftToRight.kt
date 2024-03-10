package com.hippo.ehviewer.icons.reader

import androidx.compose.foundation.Image
import androidx.compose.material.icons.materialIcon
import androidx.compose.material.icons.materialPath
import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import com.hippo.ehviewer.icons.EhIcons
import com.hippo.ehviewer.util.unsafeLazy

@Suppress("BooleanLiteralArgument")
val EhIcons.Reader.LeftToRight by unsafeLazy {
    materialIcon(name = "LeftToRight") {
        materialPath {
            moveTo(17.3159F, 18.0F)
            horizontalLineToRelative(-10.0F)
            lineTo(7.3159F, 16.0F)
            horizontalLineToRelative(-2.0F)
            verticalLineToRelative(5.0F)
            arcToRelative(2.0059F, 2.0059F, 0.0F, false, false, 2.0F, 2.0F)
            horizontalLineToRelative(10.0F)
            arcToRelative(2.0059F, 2.0059F, 0.0F, false, false, 2.0F, -2.0F)
            lineTo(19.3159F, 16.0F)
            horizontalLineToRelative(-2.0F)
            close()
            moveTo(17.3159F, 21.0F)
            horizontalLineToRelative(-10.0F)
            lineTo(7.3159F, 20.0F)
            horizontalLineToRelative(10.0F)
            close()
            moveTo(7.3159F, 6.0F)
            horizontalLineToRelative(10.0F)
            lineTo(17.3159F, 8.0F)
            horizontalLineToRelative(2.0F)
            lineTo(19.3159F, 3.0F)
            arcToRelative(2.0059F, 2.0059F, 0.0F, false, false, -2.0F, -2.0F)
            horizontalLineToRelative(-10.0F)
            arcToRelative(2.0059F, 2.0059F, 0.0F, false, false, -2.0F, 2.0F)
            lineTo(5.3159F, 8.0F)
            horizontalLineToRelative(2.0F)
            close()
            moveTo(7.3159F, 3.0F)
            horizontalLineToRelative(10.0F)
            lineTo(17.3159F, 4.0F)
            horizontalLineToRelative(-10.0F)
            close()
            moveTo(22.311F, 12.0F)
            lineToRelative(-3.0F, -3.0F)
            lineToRelative(0.0F, 2.0F)
            lineToRelative(-11.99F, 0.0F)
            lineToRelative(0.0F, 2.0F)
            lineToRelative(11.99F, 0.0F)
            lineToRelative(0.0F, 2.0F)
            lineToRelative(3.0F, -3.0F)
            close()
            moveTo(2.3206F, 11.0F)
            horizontalLineToRelative(1.5F)
            verticalLineToRelative(2.0F)
            horizontalLineToRelative(-1.5F)
            close()
            moveTo(4.8206F, 11.0F)
            horizontalLineToRelative(1.5F)
            verticalLineToRelative(2.0F)
            horizontalLineToRelative(-1.5F)
            close()
        }
    }
}

@Preview
@Composable
@Suppress("UnusedPrivateMember")
private fun IconLeftToRightPreview() {
    Image(imageVector = EhIcons.Reader.LeftToRight, contentDescription = null)
}
