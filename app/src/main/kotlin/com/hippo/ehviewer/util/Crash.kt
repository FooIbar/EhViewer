package com.hippo.ehviewer.util

import android.os.Build
import android.os.Debug
import com.hippo.ehviewer.BuildConfig
import eu.kanade.tachiyomi.util.system.logcat
import java.io.File
import java.io.PrintWriter
import java.io.Writer

private fun joinIfStringArray(any: Any?): String = if (any is Array<*>) any.joinToString() else any.toString()

private fun collectClassStaticInfo(clazz: Class<*>): String = clazz.fields.joinToString("\n") {
    "${it.name}=${joinIfStringArray(it.get(null))}"
}

object Crash {
    fun collectInfo(writer: Writer) {
        writer.write("======== PackageInfo ========\n")
        writer.write("PackageName=${BuildConfig.APPLICATION_ID}\n")
        writer.write("VersionName=${BuildConfig.VERSION_NAME}\n")
        writer.write("VersionCode=${BuildConfig.VERSION_CODE}\n")
        writer.write("CommitSha=${BuildConfig.COMMIT_SHA}\n")
        writer.write("CommitTime=${AppConfig.commitTime}\n")
        writer.write("\n")

        // Device info
        writer.write("======== DeviceInfo ========\n")
        writer.write("${collectClassStaticInfo(Build::class.java)}\n")
        writer.write("${collectClassStaticInfo(Build.VERSION::class.java)}\n")
        writer.write("MEMORY=")
        writer.write(FileUtils.humanReadableByteCount(OSUtils.appAllocatedMemory, false))
        writer.write("\n")
        writer.write("MEMORY_NATIVE=")
        writer.write(FileUtils.humanReadableByteCount(Debug.getNativeHeapAllocatedSize(), false))
        writer.write("\n")
        writer.write("MEMORY_MAX=")
        writer.write(FileUtils.humanReadableByteCount(OSUtils.appMaxMemory, false))
        writer.write("\n")
        writer.write("MEMORY_TOTAL=")
        writer.write(FileUtils.humanReadableByteCount(OSUtils.totalMemory, false))
        writer.write("\n")
        writer.write("\n")

        writer.flush()
    }

    private fun getThrowableInfo(t: Throwable, writer: PrintWriter) {
        t.printStackTrace(writer)
        var cause = t.cause
        while (cause != null) {
            cause.printStackTrace(writer)
            cause = cause.cause
        }
    }

    fun saveCrashLog(t: Throwable) {
        val dir = AppConfig.externalCrashDir ?: return
        val nowString = ReadableTime.getFilenamableTime()
        val fileName = "crash-$nowString.log"
        val file = File(dir, fileName)
        runCatching {
            file.printWriter().use { writer ->
                writer.write("TIME=${nowString}\n")
                writer.write("\n")
                collectInfo(writer)
                writer.write("======== CrashInfo ========\n")
                getThrowableInfo(t, writer)
                writer.write("\n")
            }
        }.onFailure {
            logcat(it)
            file.delete()
        }
        if (t is OutOfMemoryError) {
            val dumpFile = File(dir, "hprof-$nowString.hprof")
            Debug.dumpHprofData(dumpFile.path)
        }
    }
}
