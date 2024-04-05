package com.hippo.ehviewer.util

import android.os.ParcelFileDescriptor
import android.system.Int64Ref
import android.system.Os
import com.hippo.unifile.UniFile
import eu.kanade.tachiyomi.util.system.logcat
import java.io.FileDescriptor
import java.io.FileInputStream
import java.io.FileOutputStream

private fun sendFileTotally(from: FileDescriptor, to: FileDescriptor): Long {
    if (isAtLeastP) {
        // sendfile may fail on some devices
        try {
            return Os.sendfile(to, from, Int64Ref(0), Long.MAX_VALUE)
        } catch (e: Exception) {
            logcat("sendfile", e)
        }
    }
    return FileInputStream(from).use { src ->
        FileOutputStream(to).use { dst ->
            src.channel.transferTo(0, Long.MAX_VALUE, dst.channel)
        }
    }
}

infix fun ParcelFileDescriptor.sendTo(fd: ParcelFileDescriptor) {
    sendFileTotally(fileDescriptor, fd.fileDescriptor)
}

infix fun UniFile.sendTo(file: UniFile) = openFileDescriptor("r").use { src ->
    file.openFileDescriptor("wt").use { dst -> src sendTo dst }
}
