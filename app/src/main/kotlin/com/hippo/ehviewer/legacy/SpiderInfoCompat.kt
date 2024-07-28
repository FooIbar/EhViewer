package com.hippo.ehviewer.legacy

import com.hippo.ehviewer.spider.SpiderInfo
import java.io.InputStream
import okio.buffer
import okio.source

fun readLegacySpiderInfo(inputStream: InputStream): SpiderInfo {
    val source = inputStream.source().buffer()
    fun read(): String = source.readUtf8LineStrict()

    fun readInt(): Int = read().toInt()

    fun readLong(): Long = read().toLong()
    repeat(2) { read() } // We assert that only info v2
    val gid = readLong()
    val token = read()
    read()
    val previewPages = readInt()
    val previewPerPage = readInt()
    val pages = read().toInt()
    val pTokenMap = hashMapOf<Int, String>()
    val info = SpiderInfo(gid, token, pages, pTokenMap, previewPages, previewPerPage)
    runCatching {
        while (true) {
            val line = read()
            val pos = line.indexOf(" ")
            if (pos > 0) {
                val index = line.substring(0, pos).toInt()
                val pToken = line.substring(pos + 1)
                if (pToken.isNotEmpty()) {
                    pTokenMap[index] = pToken
                }
            }
        }
    }
    return info
}
