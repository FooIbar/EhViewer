package com.hippo.ehviewer.legacy

import com.hippo.ehviewer.spider.SpiderInfo
import com.hippo.files.read
import kotlinx.io.readLineStrict
import okio.Path

fun Path.readLegacySpiderInfo() = read {
    fun read() = readLineStrict()
    fun readInt() = read().toInt()
    fun readLong() = read().toLong()

    repeat(2) { read() } // We assert that only info v2
    val gid = readLong()
    val token = read()
    read()
    val previewPages = readInt()
    val previewPerPage = readInt()
    val pages = read().toInt()
    val pTokenMap = hashMapOf<Int, String>()
    SpiderInfo(gid, token, pages, pTokenMap, previewPages, previewPerPage).apply {
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
    }
}
