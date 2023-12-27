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

import android.net.Uri

class SingleDocumentFile(parent: UniFile?, override val uri: Uri) : UniFile(parent) {

    override fun createFile(displayName: String) = null

    override fun createDirectory(displayName: String) = null

    override val name: String?
        get() = DocumentsContractApi19.getName(uri)
    override val type: String?
        get() = DocumentsContractApi19.getType(uri)
    override val isDirectory: Boolean
        get() = DocumentsContractApi19.isDirectory(uri)
    override val isFile: Boolean
        get() = DocumentsContractApi19.isFile(uri)

    override fun lastModified() = DocumentsContractApi19.lastModified(uri)

    override fun length() = DocumentsContractApi19.length(uri)

    override fun canRead() = DocumentsContractApi19.canRead(uri)

    override fun canWrite() = DocumentsContractApi19.canWrite(uri)

    override fun ensureDir() = isDirectory

    override fun ensureFile() = isFile

    override fun resolve(displayName: String) = error("SingleDocumentFile never have a children")

    override fun delete() = DocumentsContractApi19.delete(uri)

    override fun exists() = DocumentsContractApi19.exists(uri)

    override fun listFiles() = emptyList<SingleDocumentFile>()

    override fun findFirst(filter: (String) -> Boolean) = null

    override fun renameTo(displayName: String) = false
}
