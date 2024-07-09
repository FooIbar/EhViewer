package com.hippo.ehviewer.util

import android.content.ContentResolver
import android.net.Uri
import android.os.Environment
import android.provider.DocumentsContract
import com.hippo.files.openFileDescriptor
import okio.Path
import splitties.init.appCtx

val Uri.displayPath: String?
    get() {
        if (scheme == ContentResolver.SCHEME_FILE) {
            return path
        }

        val context = appCtx
        if (DocumentsContract.isDocumentUri(context, this)) {
            val (type, path) = DocumentsContract.getDocumentId(this).split(":", limit = 2)
            if (authority == "com.android.externalstorage.documents") {
                if (type == "primary") {
                    return Environment.getExternalStorageDirectory().path + "/" + path
                }
            }

            context.externalCacheDirs.forEach {
                val cachePath = it.path
                val index = cachePath.indexOf(type)
                if (index != -1) {
                    return cachePath.substring(0, index + type.length) + "/" + path
                }
            }
        }

        return toString()
    }

fun Path.sha1() = openFileDescriptor("r").use { com.hippo.ehviewer.jni.sha1(it.fd) }
