package com.hippo.ehviewer.download

import com.hippo.ehviewer.dao.DownloadInfo
import com.hippo.ehviewer.spider.getComicInfo

enum class DownloadsFilterMode(
    val flag: Int,
    val take: (info: DownloadInfo, label: String?, keyword: String) -> Boolean,
) {
    CUSTOM(flag = 0, take = { info, label, _ ->
        label == "" || info.label == label
    }),
    ARTIST(flag = 1, take = { info, label, _ ->
        label == "" || info.getComicInfo().penciller?.contains(label) ?: false
    }),
    ;

    companion object {
        val Default = CUSTOM
        fun from(flag: Int) = DownloadsFilterMode.entries[flag]
    }
}
