package com.hippo.unifile

import android.net.Uri
import android.os.Environment
import android.os.ParcelFileDescriptor.AutoCloseInputStream
import android.os.ParcelFileDescriptor.AutoCloseOutputStream
import android.provider.DocumentsContract
import com.hippo.ehviewer.jni.sha1
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import okio.Path

/**
 * Use Native IO/NIO directly if possible, unless you need process file content on JVM!
 */
fun UniFile.openInputStream(): FileInputStream = AutoCloseInputStream(openFileDescriptor("r"))

/**
 * Use Native IO/NIO directly if possible, unless you need process file content on JVM!
 */
fun UniFile.openOutputStream(): FileOutputStream = AutoCloseOutputStream(openFileDescriptor("wt"))

fun UniFile.sha1() = openFileDescriptor("r").use { sha1(it.fd) }

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
