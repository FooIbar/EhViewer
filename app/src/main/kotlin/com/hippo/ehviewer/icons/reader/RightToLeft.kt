package com.hippo.ehviewer.icons.reader

import androidx.compose.foundation.Image
import androidx.compose.material.icons.materialIcon
import androidx.compose.material.icons.materialPath
import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import com.hippo.ehviewer.icons.EhIcons
import com.hippo.ehviewer.util.unsafeLazy

@Suppress("BooleanLiteralArgument")
val EhIcons.Reader.RightToLeft by unsafeLazy {
    materialIcon(name = "RightToLeft") {
        materialPath {
            moveTo(7.0F, 6.0F)
            horizontalLineTo(17.0F)
            verticalLineTo(8.0F)
            horizontalLineToRelative(2.0F)
            verticalLineTo(3.0F)
            arcToRelative(2.0059F, 2.0059F, 0.0F, false, false, -2.0F, -2.0F)
            horizontalLineTo(7.0F)
            arcTo(2.0059F, 2.0059F, 0.0F, false, false, 5.0F, 3.0F)
            verticalLineTo(8.0F)
            horizontalLineTo(7.0F)
            close()
            moveTo(7.0F, 3.0F)
            horizontalLineTo(17.0F)
            verticalLineTo(4.0F)
            horizontalLineTo(7.0F)
            close()
            moveTo(17.0F, 18.0F)
            lineTo(7.0F, 18.0F)
            lineTo(7.0F, 16.0F)
            lineTo(5.0F, 16.0F)
            verticalLineToRelative(5.0F)
            arcToRelative(2.0059F, 2.0059F, 0.0F, false, false, 2.0F, 2.0F)
            lineTo(17.0F, 23.0F)
            arcToRelative(2.0059F, 2.0059F, 0.0F, false, false, 2.0F, -2.0F)
            lineTo(19.0F, 16.0F)
            lineTo(17.0F, 16.0F)
            close()
            moveTo(17.0F, 21.0F)
            lineTo(7.0F, 21.0F)
            lineTo(7.0F, 20.0F)
            lineTo(17.0F, 20.0F)
            close()
            moveTo(5.005F, 13.0F)
            lineToRelative(11.99F, 0.0F)
            lineToRelative(0.0F, -2.0F)
            lineToRelative(-11.99F, 0.0F)
            lineToRelative(0.0F, -2.0F)
            lineToRelative(-3.0F, 3.0F)
            lineToRelative(3.0F, 3.0F)
            lineToRelative(0.0F, -2.0F)
            close()
            moveTo(20.4953F, 11.0F)
            horizontalLineToRelative(1.5F)
            verticalLineToRelative(2.0F)
            horizontalLineToRelative(-1.5F)
            close()
            moveTo(17.9953F, 11.0F)
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
private fun IconRightToLeftPreview() {
    Image(imageVector = EhIcons.Reader.RightToLeft, contentDescription = null)
}
