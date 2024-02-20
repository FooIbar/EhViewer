package com.hippo.unifile

import android.net.Uri
import android.os.Environment
import android.os.ParcelFileDescriptor.AutoCloseInputStream
import android.os.ParcelFileDescriptor.AutoCloseOutputStream
import android.provider.DocumentsContract
import eu.kanade.tachiyomi.util.system.logcat
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import okio.HashingSink
import okio.Path
import okio.blackholeSink
import okio.buffer
import okio.source

/**
 * Use Native IO/NIO directly if possible, unless you need process file content on JVM!
 */
fun UniFile.openInputStream(): FileInputStream {
    return AutoCloseInputStream(openFileDescriptor("r"))
}

/**
 * Use Native IO/NIO directly if possible, unless you need process file content on JVM!
 */
fun UniFile.openOutputStream(): FileOutputStream {
    return AutoCloseOutputStream(openFileDescriptor("w"))
}

fun UniFile.sha1() = runCatching {
    openInputStream().source().buffer().use { source ->
        HashingSink.sha1(blackholeSink()).use {
            source.readAll(it)
            it.hash.hex()
        }
    }
}.getOrElse {
    logcat(it)
    null
}

fun File.asUniFile() = UniFile.fromFile(this)

fun Path.asUniFile() = toFile().asUniFile()

fun Uri.asUniFileOrNull() = UniFile.fromUri(this)

fun Uri.asUniFile() = requireNotNull(asUniFileOrNull())

val Uri.displayPath: String?
    get() {
        if (UniFile.isFileUri(this)) {
            return path
        }
        if (UniFile.isDocumentUri(this)) {
            if (authority == "com.android.externalstorage.documents") {
                val (type, id) = DocumentsContract.getDocumentId(this).split(":", limit = 2)
                if (type == "primary") {
                    return Environment.getExternalStorageDirectory().absolutePath + "/" + id
                }
            }
        }
        return toString()
    }
