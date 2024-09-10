package com.hippo.ehviewer.ui.reader

import androidx.compose.ui.geometry.Offset
import com.hippo.ehviewer.Settings
import me.saket.telephoto.zoomable.DoubleClickToZoomListener
import me.saket.telephoto.zoomable.ZoomableState

object DoubleTapZoom : DoubleClickToZoomListener {
    override suspend fun onDoubleClick(state: ZoomableState, centroid: Offset) {
        if (Settings.doubleTapToZoom.value) {
            val zoomFraction = state.zoomFraction ?: return // Content isn't ready yet
            if (zoomFraction > 0.05f) {
                state.resetZoom()
            } else {
                // Workaround for https://github.com/saket/telephoto/issues/45
                state.zoomTo(
                    zoomFactor = state.contentTransformation.scaleMetadata.initialScale.scaleX * 2f,
                    centroid = centroid,
                )
            }
        }
    }
}
