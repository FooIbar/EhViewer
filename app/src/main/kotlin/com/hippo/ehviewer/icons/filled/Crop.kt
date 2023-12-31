package com.hippo.ehviewer.icons.filled

import androidx.compose.material.icons.materialIcon
import androidx.compose.material.icons.materialPath
import com.hippo.ehviewer.icons.EhIcons
import com.hippo.ehviewer.util.unsafeLazy

val EhIcons.Filled.Crop by unsafeLazy {
    materialIcon(name = "Crop") {
        materialPath {
            moveTo(17.0F, 15.0F)
            horizontalLineToRelative(2.0F)
            verticalLineTo(7.0F)
            curveToRelative(0.0F, -1.1F, -0.9F, -2.0F, -2.0F, -2.0F)
            horizontalLineTo(9.0F)
            verticalLineToRelative(2.0F)
            horizontalLineToRelative(8.0F)
            verticalLineToRelative(8.0F)
            close()
            moveTo(7.0F, 17.0F)
            verticalLineTo(1.0F)
            horizontalLineTo(5.0F)
            verticalLineToRelative(4.0F)
            horizontalLineTo(1.0F)
            verticalLineToRelative(2.0F)
            horizontalLineToRelative(4.0F)
            verticalLineToRelative(10.0F)
            curveToRelative(0.0F, 1.1F, 0.9F, 2.0F, 2.0F, 2.0F)
            horizontalLineToRelative(10.0F)
            verticalLineToRelative(4.0F)
            horizontalLineToRelative(2.0F)
            verticalLineToRelative(-4.0F)
            horizontalLineToRelative(4.0F)
            verticalLineToRelative(-2.0F)
            horizontalLineTo(7.0F)
            close()
        }
    }
}
