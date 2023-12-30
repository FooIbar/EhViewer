package com.hippo.ehviewer.client.parser

import java.nio.ByteBuffer
import okio.Buffer
import okio.Source
import okio.Timeout

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
