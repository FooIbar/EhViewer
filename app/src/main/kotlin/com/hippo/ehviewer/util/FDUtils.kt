package com.hippo.ehviewer.util

import android.os.ParcelFileDescriptor
import android.system.Int64Ref
import android.system.Os
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

infix fun ParcelFileDescriptor.sendTo(fd: FileDescriptor) {
    sendFileTotally(fileDescriptor, fd)
}

infix fun ParcelFileDescriptor.sendTo(fd: ParcelFileDescriptor) {
    sendFileTotally(fileDescriptor, fd.fileDescriptor)
}
