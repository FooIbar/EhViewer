package com.hippo.ehviewer.gallery

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import com.hippo.ehviewer.image.Image
import kotlinx.coroutines.flow.StateFlow

class Page(
    val index: Int,
    val statusFlow: StateFlow<PageStatus>,
)

val Page.status
    get() = statusFlow.value

val Page.statusObserved
    @Composable
    get() = statusFlow.collectAsState().value

val PageStatus.progressObserved
    @Composable
    get() = when (this) {
        is PageStatus.Loading -> progress.collectAsState().value
        else -> 0f
    }

sealed interface PageStatus {
    data object Queued : PageStatus
    data class Blocked(val ad: Image) : PageStatus
    data class Ready(val image: Image) : PageStatus
    data class Error(val message: String?) : PageStatus
    data class Loading(val progress: StateFlow<Float>) : PageStatus
}
