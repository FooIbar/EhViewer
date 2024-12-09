package com.hippo.ehviewer.util

import android.os.Build
import android.os.Debug
import com.hippo.ehviewer.BuildConfig
import com.hippo.ehviewer.Settings
import eu.kanade.tachiyomi.util.system.logcat
import java.io.File
import java.io.PrintWriter
import java.io.Writer

object CrashHandler {
    fun install() {
        val handler = Thread.getDefaultUncaughtExceptionHandler()
        Thread.setDefaultUncaughtExceptionHandler { t, e ->
            if (Settings.saveCrashLog) {
                runCatching { saveCrashLog(e) }
            }
            handler?.uncaughtException(t, e)
        }
    }

    fun collectInfo(writer: Writer) {
        val staticInfo = listOf(
            Build::class.java,
            Build.VERSION::class.java,
        ).flatMap { clazz ->
            clazz.fields.map {
                it.name to it[null].let { x -> if (x is Array<*>) x.joinToString() else x.toString() }
            }
        } + listOf(
            "MEMORY" to FileUtils.humanReadableByteCount(OSUtils.appAllocatedMemory, false),
            "MEMORY_NATIVE" to FileUtils.humanReadableByteCount(Debug.getNativeHeapAllocatedSize(), false),
            "MEMORY_MAX" to FileUtils.humanReadableByteCount(OSUtils.appMaxMemory, false),
            "MEMORY_TOTAL" to FileUtils.humanReadableByteCount(OSUtils.totalMemory, false),
        )
        val deviceInfo = staticInfo.unzip().let { (a, b) ->
            a.maxBy(String::length).length.let { len -> a.map { it.padEnd(len) } } zip b
        }.joinToString(separator = "\n") { (a, b) -> "$a = $b" }
        writer.write(
            """
                ======== PackageInfo ========
                PackageName = ${BuildConfig.APPLICATION_ID}
                VersionName = ${BuildConfig.VERSION_NAME}
                VersionCode = ${BuildConfig.VERSION_CODE}
                CommitSha   = ${BuildConfig.COMMIT_SHA}
                CommitTime  = ${AppConfig.commitTime}
                
                ======== DeviceInfo ========
                $deviceInfo
                
            """.trimIndent(),
        )
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

    private fun saveCrashLog(t: Throwable) {
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
