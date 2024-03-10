package com.hippo.ehviewer.icons.reader

import androidx.compose.foundation.Image
import androidx.compose.material.icons.materialIcon
import androidx.compose.material.icons.materialPath
import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import com.hippo.ehviewer.icons.EhIcons
import com.hippo.ehviewer.util.unsafeLazy

@Suppress("BooleanLiteralArgument")
val EhIcons.Reader.ContinuousVertical by unsafeLazy {
    materialIcon(name = "ContinuousVertical") {
        materialPath {
            moveTo(17.3159F, 1.0F)
            horizontalLineToRelative(-10.0F)
            arcToRelative(2.0059F, 2.0059F, 0.0F, false, false, -2.0F, 2.0F)
            lineTo(5.3159F, 21.0F)
            arcToRelative(2.0059F, 2.0059F, 0.0F, false, false, 2.0F, 2.0F)
            horizontalLineToRelative(10.0F)
            arcToRelative(2.0059F, 2.0059F, 0.0F, false, false, 2.0F, -2.0F)
            lineTo(19.3159F, 3.0F)
            arcTo(2.0059F, 2.0059F, 0.0F, false, false, 17.3159F, 1.0F)
            close()
            moveTo(17.3159F, 21.0F)
            horizontalLineToRelative(-10.0F)
            lineTo(7.3159F, 3.0F)
            horizontalLineToRelative(10.0F)
            close()
            moveTo(11.3083F, 5.0F)
            horizontalLineToRelative(2.0F)
            verticalLineToRelative(5.5F)
            horizontalLineToRelative(-2.0F)
            close()
            moveTo(15.308F, 16.0F)
            lineToRelative(-2.0F, 0.0F)
            lineToRelative(0.0F, -4.5F)
            lineToRelative(-2.0F, 0.0F)
            lineToRelative(0.0F, 4.5F)
            lineToRelative(-2.0F, 0.0F)
            lineToRelative(3.0F, 3.0F)
            lineToRelative(3.0F, -3.0F)
            close()
        }
    }
}

@Preview
@Composable
@Suppress("UnusedPrivateMember")
private fun IconContinuousVerticalPreview() {
    Image(imageVector = EhIcons.Reader.ContinuousVertical, contentDescription = null)
}
