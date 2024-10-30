package com.hippo.ehviewer.util

import android.content.ContentResolver
import android.net.Uri
import android.os.Environment
import android.provider.DocumentsContract
import androidx.core.provider.DocumentsContractCompat
import arrow.core.Either
import com.hippo.files.openFileDescriptor
import com.hippo.files.toUri
import okio.Path
import splitties.init.appCtx

val Uri.displayPath: String?
    get() {
        if (scheme == ContentResolver.SCHEME_FILE) {
            return path
        }

        val context = appCtx
        if (DocumentsContract.isDocumentUri(context, this)) {
            val (type, path) = DocumentsContract.getDocumentId(this).split(":", limit = 2).also {
                if (it.size < 2) return toString()
            }
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

val Path.displayName: String
    get() {
        val uri = toUri()
        // The Path is constructed by us if the URI is a tree URI, so we don't need to query
        if (uri.scheme != ContentResolver.SCHEME_FILE && !DocumentsContractCompat.isTreeUri(uri)) {
            Either.catch {
                val proj = arrayOf(DocumentsContract.Document.COLUMN_DISPLAY_NAME)
                appCtx.contentResolver.query(uri, proj, null, null, null)?.use { c ->
                    if (c.moveToNext()) return c.getString(0)
                }
            }
        }
        return name
    }

fun Path.sha1() = openFileDescriptor("r").use { com.hippo.ehviewer.jni.sha1(it.fd) }
