package com.hippo.ehviewer.spider

import com.hippo.ehviewer.client.data.GalleryInfo
import com.hippo.ehviewer.client.thumbUrl
import com.hippo.ehviewer.dao.DownloadInfo

object DownloadInfoMagics {
    private const val DOWNLOAD_INFO_DIRNAME_URL_MAGIC = "$"
    private const val DOWNLOAD_INFO_DIRNAME_URL_SEPARATOR = "|"

    fun encodeMagicRequestOrUrl(info: GalleryInfo): String {
        val url = info.thumbUrl
        val location = (info as? DownloadInfo)?.dirname
        return if (location.isNullOrBlank()) {
            url
        } else {
            DOWNLOAD_INFO_DIRNAME_URL_MAGIC + url + DOWNLOAD_INFO_DIRNAME_URL_SEPARATOR + location
        }
    }

    fun decodeMagicRequestOrUrl(encoded: String): Pair<String, String?> {
        return if (encoded.startsWith(DOWNLOAD_INFO_DIRNAME_URL_MAGIC)) {
            val (a, b) = encoded.removePrefix(DOWNLOAD_INFO_DIRNAME_URL_MAGIC).split(DOWNLOAD_INFO_DIRNAME_URL_SEPARATOR)
            a to b
        } else {
            encoded to null
        }
    }
}
