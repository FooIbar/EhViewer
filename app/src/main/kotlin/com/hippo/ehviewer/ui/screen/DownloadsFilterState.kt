package com.hippo.ehviewer.ui.screen

import android.os.Parcelable
import com.hippo.ehviewer.dao.DownloadInfo
import com.hippo.ehviewer.download.DownloadsFilterMode
import com.hippo.ehviewer.util.containsIgnoreCase
import kotlinx.parcelize.Parcelize

@Parcelize
data class DownloadsFilterState(
    val mode: DownloadsFilterMode,
    val label: String?,
    val state: Int = -1,
    val keyword: String = "",
) : Parcelable

fun DownloadsFilterState.take(info: DownloadInfo) =
    mode.take(info, label) &&
        (state == -1 || info.state == state) &&
        with(info) {
            title.containsIgnoreCase(keyword) ||
                titleJpn.containsIgnoreCase(keyword) ||
                simpleTags?.any { it.containsIgnoreCase(keyword) } ?: false
        }
