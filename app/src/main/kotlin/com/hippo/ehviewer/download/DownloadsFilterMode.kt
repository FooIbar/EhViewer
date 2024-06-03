package com.hippo.ehviewer.download

import com.hippo.ehviewer.dao.DownloadInfo
import com.hippo.ehviewer.dao.artists

enum class DownloadsFilterMode(
    val flag: Int,
    val take: (info: DownloadInfo, label: String?) -> Boolean,
) {
    CUSTOM(flag = 0, take = { info, label ->
        label == "" || info.label == label
    }),
    ARTIST(flag = 1, take = { info, label ->
        if (null == label) {
            info.artistInfoList.isEmpty()
        } else {
            label == "" || info.artists.contains(label)
        }
    }),
    ;

    companion object {
        val Default = ARTIST
        fun from(flag: Int) = DownloadsFilterMode.entries[flag]
    }
}
