package com.hippo.ehviewer.icons.reader

import androidx.compose.foundation.Image
import androidx.compose.material.icons.materialIcon
import androidx.compose.material.icons.materialPath
import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import com.hippo.ehviewer.icons.EhIcons
import com.hippo.ehviewer.util.unsafeLazy

@Suppress("BooleanLiteralArgument")
val EhIcons.Reader.Vertical by unsafeLazy {
    materialIcon(name = "Vertical") {
        materialPath {
            moveTo(17.0F, 1.0F)
            lineTo(7.0F, 1.0F)
            arcTo(2.0059F, 2.0059F, 0.0F, false, false, 5.0F, 3.0F)
            lineTo(5.0F, 21.0F)
            arcToRelative(2.0059F, 2.0059F, 0.0F, false, false, 2.0F, 2.0F)
            lineTo(17.0F, 23.0F)
            arcToRelative(2.0059F, 2.0059F, 0.0F, false, false, 2.0F, -2.0F)
            lineTo(19.0F, 3.0F)
            arcTo(2.0059F, 2.0059F, 0.0F, false, false, 17.0F, 1.0F)
            close()
            moveTo(17.0F, 3.0F)
            verticalLineToRelative(1.0462F)
            lineTo(7.0F, 4.0462F)
            lineTo(7.0F, 3.0F)
            close()
            moveTo(17.0F, 6.0462F)
            verticalLineToRelative(12.0F)
            lineTo(7.0F, 18.0462F)
            verticalLineToRelative(-12.0F)
            close()
            moveTo(7.0F, 21.0F)
            verticalLineToRelative(-0.9538F)
            lineTo(17.0F, 20.0462F)
            lineTo(17.0F, 21.0F)
            close()
            moveTo(9.0F, 13.0F)
            lineToRelative(3.0F, 3.0F)
            lineToRelative(3.0F, -3.0F)
            lineToRelative(-2.0F, 0.0F)
            lineToRelative(0.0F, -5.0F)
            lineToRelative(-2.0F, 0.0F)
            lineToRelative(0.0F, 5.0F)
            lineToRelative(-2.0F, 0.0F)
            close()
        }
    }
}

@Preview
@Composable
@Suppress("UnusedPrivateMember")
private fun IconVerticalPreview() {
    Image(imageVector = EhIcons.Reader.Vertical, contentDescription = null)
}
