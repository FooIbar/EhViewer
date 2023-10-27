@file:Suppress("ktlint:standard:property-naming")

package com.hippo.ehviewer.icons

import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp

object EhIcons {
    object Filled

    object Big {
        object Filled

        val Default = Filled
    }

    val Default = Filled
}

inline fun bigIcon(
    name: String,
    block: ImageVector.Builder.() -> ImageVector.Builder,
): ImageVector = ImageVector.Builder(
    name = name,
    defaultWidth = BigIconDimension.dp,
    defaultHeight = BigIconDimension.dp,
    viewportWidth = MaterialIconDimension,
    viewportHeight = MaterialIconDimension,
).block().build()

const val BigIconDimension = 120f
const val MaterialIconDimension = 24f
