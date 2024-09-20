package com.hippo.ehviewer.gallery

import com.hippo.ehviewer.image.Image
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.update

class Page(
    val index: Int,
    val status: MutableStateFlow<PageStatus> = MutableStateFlow(PageStatus.Queued),
)

fun Page.reset() = status.update { PageStatus.Queued }

sealed interface PageStatus {
    data object Queued : PageStatus
    data class Blocked(val ad: Image) : PageStatus
    data class Ready(val image: Image) : PageStatus
    data class Error(val message: String?) : PageStatus
    data class Loading(val progress: MutableStateFlow<Int>) : PageStatus
}
