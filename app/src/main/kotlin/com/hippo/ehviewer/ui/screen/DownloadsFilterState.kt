package com.hippo.ehviewer.ui.screen

import android.os.Parcelable
import com.hippo.ehviewer.dao.DownloadInfo
import com.hippo.ehviewer.util.containsIgnoreCase
import kotlinx.parcelize.Parcelize

@Parcelize
data class DownloadsFilterState(
    val label: String?,
    val state: Int = -1,
    val keyword: String = "",
) : Parcelable

fun DownloadsFilterState.take(info: DownloadInfo) =
    (label == "" || info.label == label) && (state == -1 || info.state == state) &&
        with(info) { title.containsIgnoreCase(keyword) || titleJpn.containsIgnoreCase(keyword) }
