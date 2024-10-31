package com.hippo.ehviewer.gallery

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import com.hippo.ehviewer.image.Image
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.update

class Page(
    val index: Int,
    val statusFlow: MutableStateFlow<PageStatus> = MutableStateFlow(PageStatus.Queued),
)

fun Page.unblock() = statusFlow.update { status ->
    when (status) {
        is PageStatus.Blocked -> PageStatus.Ready(status.ad)
        else -> error("Call unblock on page not blocked!!!")
    }
}

fun Page.reset() = statusFlow.update { PageStatus.Queued }

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

    @JvmInline value class Blocked(val ad: Image) : PageStatus

    @JvmInline value class Ready(val image: Image) : PageStatus

    @JvmInline value class Error(val message: String?) : PageStatus

    @JvmInline value class Loading(val progress: MutableStateFlow<Float>) : PageStatus
}
