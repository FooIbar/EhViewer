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

fun Path.moveTo(target: Path) = SystemFileSystem.atomicMove(this, target)

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
    val path = requireNotNull(uri.encodedPath) { "Invalid path: $str" }
    val paths = path.split('/').dropWhile { it.isEmpty() }
    return if (paths.size > 4 && paths[0] == "tree") {
        uri.buildUpon().apply {
            path(null)
            repeat(3) { i ->
                appendEncodedPath(paths[i])
            }
            val root = Uri.decode(paths[3])
            val prefix = if (root.endsWith(':')) root else "$root/"
            val suffix = uri.encodedFragment?.let { "#$it" }.orEmpty()
            appendPath(paths.subList(4, paths.size).joinToString("/", prefix, suffix))
        }.build()
    } else {
        uri
    }
}

fun Uri.toOkioPath() = if (scheme == ContentResolver.SCHEME_FILE) {
    requireNotNull(path) { "Invalid URI: $this" }
} else {
    toString()
}.toPath()
