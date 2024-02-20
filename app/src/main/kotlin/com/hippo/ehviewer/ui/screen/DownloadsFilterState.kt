package com.hippo.ehviewer.ui.screen

import androidx.compose.runtime.saveable.Saver
import androidx.compose.runtime.saveable.listSaver
import com.hippo.ehviewer.dao.DownloadInfo
import com.hippo.ehviewer.util.containsIgnoreCase

data class DownloadsFilterState(val label: String?, val state: Int = -1, val keyword: String = "") {
    companion object {
        val Saver: Saver<DownloadsFilterState, *> = listSaver(
            save = { listOf(it.label, it.state.toString(), it.keyword) },
            restore = { (label, state, keyword) ->
                DownloadsFilterState(label, state!!.toInt(), keyword!!)
            },
        )
    }
}

fun DownloadsFilterState.take(info: DownloadInfo) =
    (label == "" || info.label == label) && (state == -1 || info.state == state) &&
        with(info) { title.containsIgnoreCase(keyword) || titleJpn.containsIgnoreCase(keyword) }
