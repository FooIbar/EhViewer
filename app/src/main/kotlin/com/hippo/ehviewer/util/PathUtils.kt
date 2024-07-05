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

        if (DocumentsContract.isDocumentUri(appCtx, this)) {
            if (authority == "com.android.externalstorage.documents") {
                val (type, id) = DocumentsContract.getDocumentId(this).split(":", limit = 2)
                if (type == "primary") {
                    return Environment.getExternalStorageDirectory().absolutePath + "/" + id
                }
            }
        }

        return toString()
    }

fun Path.sha1() = openFileDescriptor("r").use { com.hippo.ehviewer.jni.sha1(it.fd) }
