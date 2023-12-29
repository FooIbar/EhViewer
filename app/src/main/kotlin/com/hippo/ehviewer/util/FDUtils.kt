package com.hippo.ehviewer.util

import android.os.ParcelFileDescriptor
import android.system.Int64Ref
import android.system.Os
import com.hippo.unifile.UniFile
import java.io.FileDescriptor
import java.io.FileInputStream
import java.io.FileOutputStream

private fun sendFileTotally(from: FileDescriptor, to: FileDescriptor) {
    if (isAtLeastP) {
        Os.sendfile(to, from, Int64Ref(0), Long.MAX_VALUE)
    } else {
        FileInputStream(from).use { src ->
            FileOutputStream(to).use { dst ->
                src.channel.transferTo(0, Long.MAX_VALUE, dst.channel)
            }
        }
    }
}

infix fun ParcelFileDescriptor.sendTo(fd: ParcelFileDescriptor) {
    sendFileTotally(fileDescriptor, fd.fileDescriptor)
}

infix fun UniFile.sendTo(file: UniFile) = openFileDescriptor("r").use { src ->
    file.openFileDescriptor("w").use { dst -> src sendTo dst }
}
