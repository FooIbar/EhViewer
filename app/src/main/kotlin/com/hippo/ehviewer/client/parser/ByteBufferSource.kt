package com.hippo.ehviewer.client.parser

import com.hippo.ehviewer.client.parseAs
import eu.kanade.tachiyomi.util.system.logcat
import java.nio.ByteBuffer
import kotlin.system.measureNanoTime
import okio.Buffer
import okio.Source
import okio.Timeout
import okio.buffer

fun ByteBuffer.asSource() = object : Source {
    private val buffer = this@asSource.slice()
    private val len = buffer.capacity()

    override fun close() = Unit

    override fun read(sink: Buffer, byteCount: Long): Long {
        if (buffer.position() == len) return -1
        val pos = buffer.position()
        val newLimit = (pos + byteCount).toInt().coerceAtMost(len)
        buffer.limit(newLimit)
        return sink.write(buffer).toLong()
    }

    override fun timeout() = Timeout.NONE
}

inline fun <reified T> unmarshalParsingAs(body: ByteBuffer, parser: (ByteBuffer, Int) -> Int): T {
    // We want to use cbor as it have smaller output size
    // But we would rather reuse allocated ByteArrays (i.e. decode with okio Buffer)
    val jsonBytes: Int
    measureNanoTime {
        jsonBytes = parser(body, body.limit())
    }.also { it.logcat { "Parse + marshal use $it ns" } }
    body.clear()
    body.limit(jsonBytes)
    val t: T
    measureNanoTime {
        t = body.asSource().buffer().use { it.parseAs<T>() }
    }.also { it.logcat { "Unmarshal + allocate java use $it ns" } }
    return t
}
