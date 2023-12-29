package com.hippo.unifile

import android.graphics.ImageDecoder
import android.net.Uri
import android.os.Build
import android.os.ParcelFileDescriptor
import android.os.ParcelFileDescriptor.AutoCloseInputStream
import android.os.ParcelFileDescriptor.AutoCloseOutputStream
import android.os.ParcelFileDescriptor.open
import android.os.ParcelFileDescriptor.parseMode
import androidx.annotation.RequiresApi
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import okio.Path
import splitties.init.appCtx

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

fun UniFile.openFileDescriptor(mode: String): ParcelFileDescriptor {
    if (this is RawFile) return open(file, parseMode(mode))
    return appCtx.contentResolver.openFileDescriptor(uri, mode) ?: error("Can't open ParcelFileDescriptor")
}

@RequiresApi(Build.VERSION_CODES.P)
fun UniFile.imageSource() = ImageDecoder.createSource(appCtx.contentResolver, uri)

fun File.asUniFile() = UniFile.fromFile(this)

fun Path.asUniFile() = toFile().asUniFile()

fun Uri.asUniFileOrNull() = UniFile.fromUri(this)

fun Uri.asUniFile() = requireNotNull(asUniFileOrNull())
