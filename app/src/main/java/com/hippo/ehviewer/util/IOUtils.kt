package com.hippo.ehviewer.util

import okhttp3.ResponseBody
import okio.buffer
import okio.sink
import java.io.File

fun ResponseBody.copyToFile(file: File) {
    file.outputStream().use { os ->
        // Prior to the adoption of OpenJDK, transferFrom will call ByteBuffer.allocate((int) count)
        if (isAtLeastN) {
            os.channel.transferFrom(source(), 0, Long.MAX_VALUE)
        } else {
            os.sink().buffer().use { it.writeAll(source()) }
        }
    }
}
