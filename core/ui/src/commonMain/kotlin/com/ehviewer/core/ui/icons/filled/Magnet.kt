package com.ehviewer.core.ui.icons.filled

import androidx.compose.foundation.Image
import androidx.compose.material.icons.materialIcon
import androidx.compose.material.icons.materialPath
import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import com.ehviewer.core.ui.icons.EhIcons
import com.ehviewer.core.util.unsafeLazy

val EhIcons.Filled.Magnet by unsafeLazy {
    materialIcon(name = "Magnet") {
        materialPath {
            moveTo(3.0F, 7.0F)
            verticalLineTo(13.0F)
            arcTo(9.0F, 9.0F, 0.0F, false, false, 12.0F, 22.0F)
            arcTo(9.0F, 9.0F, 0.0F, false, false, 21.0F, 13.0F)
            verticalLineTo(7.0F)
            horizontalLineTo(17.0F)
            verticalLineTo(13.0F)
            arcTo(5.0F, 5.0F, 0.0F, false, true, 12.0F, 18.0F)
            arcTo(5.0F, 5.0F, 0.0F, false, true, 7.0F, 13.0F)
            verticalLineTo(7.0F)
            moveTo(17.0F, 5.0F)
            horizontalLineTo(21.0F)
            verticalLineTo(2.0F)
            horizontalLineTo(17.0F)
            moveTo(3.0F, 5.0F)
            horizontalLineTo(7.0F)
            verticalLineTo(2.0F)
            horizontalLineTo(3.0F)
            close()
        }
    }
}

@Preview
@Composable
@Suppress("UnusedPrivateMember")
private fun IconMagnetPreview() {
    Image(imageVector = EhIcons.Default.Magnet, contentDescription = null)
}
