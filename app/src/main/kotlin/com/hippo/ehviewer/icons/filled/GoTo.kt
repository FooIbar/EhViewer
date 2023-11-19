package com.hippo.ehviewer.icons.filled

import androidx.compose.material.icons.materialIcon
import androidx.compose.material.icons.materialPath
import com.hippo.ehviewer.icons.EhIcons
import com.hippo.ehviewer.util.unsafeLazy

val EhIcons.Filled.GoTo by unsafeLazy {
    materialIcon(name = "GoTo") {
        materialPath {
            moveTo(13.0F, 13.0F)
            lineTo(17.0F, 17.0F)
            lineTo(21.0F, 13.0F)
            horizontalLineTo(18.0F)
            verticalLineTo(10.0F)
            curveTo(18.0F, 6.1F, 14.9F, 3.0F, 11.0F, 3.0F)
            reflectiveCurveTo(4.0F, 6.1F, 4.0F, 10.0F)
            verticalLineTo(17.0F)
            horizontalLineTo(6.0F)
            verticalLineTo(10.0F)
            curveTo(6.0F, 7.2F, 8.2F, 5.0F, 11.0F, 5.0F)
            reflectiveCurveTo(16.0F, 7.2F, 16.0F, 10.0F)
            verticalLineTo(13.0F)
            horizontalLineTo(13.0F)
            moveTo(16.0F, 19.0F)
            horizontalLineTo(18.0F)
            verticalLineTo(21.0F)
            horizontalLineTo(16.0F)
            verticalLineTo(19.0F)
            close()
        }
    }
}
