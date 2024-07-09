package com.hippo.files

import android.content.ContentResolver
import android.net.Uri
import android.os.ParcelFileDescriptor
import androidx.core.net.toUri
import java.io.FileInputStream
import java.io.FileOutputStream
import okio.Path
import okio.Path.Companion.toPath

val Path.isDirectory get() = SystemFileSystem.metadataOrNull(this)?.isDirectory == true

val Path.isFile get() = SystemFileSystem.metadataOrNull(this)?.isRegularFile == true

fun Path.metadataOrNull() = SystemFileSystem.metadataOrNull(this)

fun Path.find(name: String) = resolve(name).takeIf { it.exists() }

fun Path.exists() = SystemFileSystem.exists(this)

fun Path.delete() = SystemFileSystem.deleteRecursively(this)

fun Path.list() = SystemFileSystem.listOrNull(this).orEmpty()

fun Path.openFileDescriptor(mode: String) = SystemFileSystem.openFileDescriptor(this, mode)

fun Path.openInputStream(): FileInputStream =
    ParcelFileDescriptor.AutoCloseInputStream(SystemFileSystem.openFileDescriptor(this, "r"))

fun Path.openOutputStream(): FileOutputStream =
    ParcelFileDescriptor.AutoCloseOutputStream(SystemFileSystem.openFileDescriptor(this, "wt"))

fun Path.toUri(): Uri {
    val str = toString()
    if (str.startsWith('/')) {
        return toFile().toUri()
    }

    val uri = str.replaceFirst("content:/", "content://").toUri()
    val paths = uri.pathSegments
    return if (paths.size > 3 && paths[0] == "tree") {
        uri.buildUpon().apply {
            path(null)
            repeat(3) { i ->
                appendPath(paths[i])
            }
            appendPath(paths.subList(3, paths.size).joinToString("/").replaceFirst(":/", ":"))
        }.build()
    } else {
        uri
    }
}

fun Uri.toOkioPath() = if (scheme == ContentResolver.SCHEME_FILE) {
    path!!
} else {
    toString()
}.toPath()
