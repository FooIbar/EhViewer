package com.hippo.ehviewer.ui

import com.hippo.ehviewer.client.EhUrl
import com.hippo.ehviewer.client.parser.Torrent
import io.ktor.http.encodeURLParameter

fun Torrent.toMagnetLink(gid: Long, key: String?): String {
    val hash = url.dropLast(8).takeLast(40)
    val name = name.encodeURLParameter()
    val tracker = EhUrl.getTrackerUrl(gid, key).encodeURLParameter()
    return "magnet:?xt=urn:btih:$hash&dn=$name&tr=$tracker"
}

fun Torrent.toCompactMagnetLink(): String {
    val hash = url.dropLast(8).takeLast(40)
    return "magnet:?xt=urn:btih:$hash"
}
