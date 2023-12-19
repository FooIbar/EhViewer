package com.hippo.ehviewer.util

import android.os.Build
import android.os.Debug
import com.hippo.ehviewer.BuildConfig
import java.io.File
import java.io.FileWriter
import java.io.OutputStreamWriter
import java.io.PrintWriter

private fun joinIfStringArray(any: Any?): String {
    return if (any is Array<*>) any.joinToString() else any.toString()
}

private fun collectClassStaticInfo(clazz: Class<*>): String {
    return clazz.fields.joinToString("\n") {
        "${it.name}=${joinIfStringArray(it.get(null))}"
    }
}

object Crash {
    fun collectInfo(writer: OutputStreamWriter) {
        writer.write("======== PackageInfo ========\n")
        writer.write("PackageName=${BuildConfig.APPLICATION_ID}\n")
        writer.write("VersionName=${BuildConfig.VERSION_NAME}\n")
        writer.write("VersionCode=${BuildConfig.VERSION_CODE}\n")
        writer.write("CommitSha=${BuildConfig.COMMIT_SHA}\n")
        writer.write("BuildTime=${BuildConfig.BUILD_TIME}\n")
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

    private fun getThrowableInfo(t: Throwable, fw: FileWriter) {
        val printWriter = PrintWriter(fw)
        t.printStackTrace(printWriter)
        var cause = t.cause
        while (cause != null) {
            cause.printStackTrace(printWriter)
            cause = cause.cause
        }
    }

    fun saveCrashLog(t: Throwable) {
        val dir = AppConfig.externalCrashDir ?: return
        val nowString = ReadableTime.getFilenamableTime(System.currentTimeMillis())
        val fileName = "crash-$nowString.log"
        val file = File(dir, fileName)
        runCatching {
            FileWriter(file).use { fw ->
                fw.write("TIME=${nowString}\n")
                fw.write("\n")
                collectInfo(fw)
                fw.write("======== CrashInfo ========\n")
                getThrowableInfo(t, fw)
                fw.write("\n")
                fw.flush()
            }
        }.onFailure {
            it.printStackTrace()
            file.delete()
        }
        if (t is OutOfMemoryError) {
            val dumpFile = File(dir, "hprof-$nowString.hprof")
            Debug.dumpHprofData(dumpFile.absolutePath)
        }
    }
}
