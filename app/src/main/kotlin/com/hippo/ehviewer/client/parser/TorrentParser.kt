package com.hippo.ehviewer.client.parser

import com.hippo.ehviewer.client.exception.ParseException
import java.nio.ByteBuffer
import kotlinx.serialization.Serializable

object TorrentParser {
    fun parse(body: ByteBuffer) = runCatching {
        unmarshalParsingAs<TorrentResult>(body, ::parseTorrent)
    }.getOrElse {
        throw ParseException("Can't parse torrent list", it)
    }
}

@Serializable
data class Torrent(
    val posted: String,
    val size: String,
    val seeds: Int,
    val peers: Int,
    val downloads: Int,
    val url: String,
    val name: String,
)

fun Torrent.format() = "[$posted] $name [$size] [↑$seeds ↓$peers ✓$downloads]"

typealias TorrentResult = List<Torrent>

private external fun parseTorrent(body: ByteBuffer, size: Int = body.limit()): Int
