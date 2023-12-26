/*
 * Copyright 2016 Hippo Seven
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.hippo.unifile

import android.content.Context
import android.graphics.ImageDecoder
import android.net.Uri

class SingleDocumentFile(parent: UniFile?, context: Context, override val uri: Uri) : UniFile(parent) {
    private val mContext = context.applicationContext

    override fun createFile(displayName: String) = null

    override fun createDirectory(displayName: String) = null

    override val name: String?
        get() = DocumentsContractApi19.getName(mContext, uri)
    override val type: String?
        get() = DocumentsContractApi19.getType(mContext, uri)
    override val isDirectory: Boolean
        get() = DocumentsContractApi19.isDirectory(mContext, uri)
    override val isFile: Boolean
        get() = DocumentsContractApi19.isFile(mContext, uri)

    override fun lastModified() = DocumentsContractApi19.lastModified(mContext, uri)

    override fun length() = DocumentsContractApi19.length(mContext, uri)

    override fun canRead() = DocumentsContractApi19.canRead(mContext, uri)

    override fun canWrite() = DocumentsContractApi19.canWrite(mContext, uri)

    override fun ensureDir() = isDirectory

    override fun ensureFile() = isFile

    override fun subFile(displayName: String) = null

    override fun delete() = DocumentsContractApi19.delete(mContext, uri)

    override fun exists() = DocumentsContractApi19.exists(mContext, uri)

    override fun listFiles() = null

    override fun findFirst(filter: (String) -> Boolean) = null

    override fun findFile(displayName: String) = null

    override fun renameTo(displayName: String) = false

    override val imageSource: ImageDecoder.Source
        get() = Contracts.getImageSource(mContext, uri)

    override fun openFileDescriptor(mode: String) = Contracts.openFileDescriptor(mContext, uri, mode)
}
