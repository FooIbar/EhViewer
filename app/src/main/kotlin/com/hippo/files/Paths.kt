package com.hippo.files

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

fun Path.toUri(): Uri = toString().replaceFirst("content:/", "content://").toUri().let {
    val paths = it.pathSegments
    if (paths.size > 3 && paths[0] == "tree") {
        it.buildUpon().apply {
            path(null)
            repeat(3) { i ->
                appendPath(paths[i])
            }
            appendPath(paths.subList(3, paths.size).joinToString("/").replaceFirst(":/", ":"))
        }.build()
    } else {
        it
    }
}

fun Uri.toOkioPath() = toString().toPath()
