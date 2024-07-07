package com.hippo.ehviewer.util

import android.os.Build
import android.system.Int64Ref
import android.system.Os
import androidx.annotation.RequiresApi
import com.hippo.files.openFileDescriptor
import com.hippo.files.openInputStream
import com.hippo.files.openOutputStream
import eu.kanade.tachiyomi.util.system.logcat
import java.io.FileDescriptor
import okio.Path

@RequiresApi(Build.VERSION_CODES.P)
private fun sendFileTotally(from: FileDescriptor, to: FileDescriptor): Long =
    Os.sendfile(to, from, Int64Ref(0), Long.MAX_VALUE)

infix fun Path.sendTo(file: Path): Long {
    if (isAtLeastP) {
        // sendfile may fail on some devices
        try {
            return openFileDescriptor("r").use { src ->
                file.openFileDescriptor("wt").use { dst ->
                    sendFileTotally(src.fileDescriptor, dst.fileDescriptor)
                }
            }
        } catch (e: Exception) {
            logcat("sendfile", e)
        }
    }
    return openInputStream().use { src ->
        file.openOutputStream().use { dst ->
            src.channel.transferTo(0, Long.MAX_VALUE, dst.channel)
        }
    }
}
