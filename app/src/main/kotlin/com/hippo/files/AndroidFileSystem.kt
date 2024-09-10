package com.hippo.files

import android.content.Context
import android.net.Uri
import android.os.ParcelFileDescriptor
import android.provider.DocumentsContract
import android.provider.DocumentsContract.Document
import android.provider.MediaStore
import android.webkit.MimeTypeMap
import androidx.core.database.getLongOrNull
import okio.FileHandle
import okio.FileMetadata
import okio.FileSystem
import okio.IOException
import okio.Path
import okio.Sink
import okio.Source
import splitties.init.appCtx

class AndroidFileSystem(context: Context) : FileSystem() {
    private val contentResolver = context.contentResolver
    private val physicalFileSystem = SYSTEM

    override fun appendingSink(file: Path, mustExist: Boolean): Sink {
        TODO("Not yet implemented")
    }

    override fun atomicMove(source: Path, target: Path) {
        if (source.isPhysicalFile()) {
            return physicalFileSystem.atomicMove(source, target)
        }

        DocumentsContract.renameDocument(contentResolver, source.toUri(), target.name)
            ?: throw IOException("Failed to move $source to $target")
    }

    override fun canonicalize(path: Path): Path {
        TODO("Not yet implemented")
    }

    override fun createDirectory(dir: Path, mustCreate: Boolean) {
        if (dir.isPhysicalFile()) {
            return physicalFileSystem.createDirectory(dir, mustCreate)
        }

        val alreadyExist = metadataOrNull(dir)?.isDirectory == true
        if (alreadyExist) {
            if (mustCreate) {
                throw IOException("$dir already exist")
            } else {
                return
            }
        }

        dir.parent?.let {
            DocumentsContract.createDocument(contentResolver, it.toUri(), Document.MIME_TYPE_DIR, dir.name)
        } ?: throw IOException("Failed to create directory: $dir")
    }

    override fun createSymlink(source: Path, target: Path) {
        TODO("Not yet implemented")
    }

    override fun delete(path: Path, mustExist: Boolean) {
        if (path.isPhysicalFile()) {
            return physicalFileSystem.delete(path, mustExist)
        }

        val metadata = metadataOrNull(path)

        if (metadata != null) {
            var uri = path.toUri()
            if (uri.isCifsDocument() && metadata.isDirectory) {
                uri = DocumentsContract.buildDocumentUriUsingTree(uri, DocumentsContract.getDocumentId(uri) + '/')
            }

            val deleted = DocumentsContract.deleteDocument(contentResolver, uri)
            if (!deleted) {
                throw IOException("Failed to delete $path")
            }
        } else if (mustExist) {
            throw IOException("$path does not exist")
        }
    }

    override fun deleteRecursively(fileOrDirectory: Path, mustExist: Boolean) {
        if (fileOrDirectory.isPhysicalFile()) {
            if (metadataOrNull(fileOrDirectory)?.isDirectory == true) {
                physicalFileSystem.deleteRecursively(fileOrDirectory, mustExist)
            } else {
                physicalFileSystem.delete(fileOrDirectory, mustExist)
            }
        } else {
            delete(fileOrDirectory, mustExist)
        }
    }

    override fun list(dir: Path): List<Path> = list(dir, throwOnFailure = true)!!

    override fun listOrNull(dir: Path): List<Path>? = list(dir, throwOnFailure = false)

    private fun list(dir: Path, throwOnFailure: Boolean): List<Path>? {
        if (dir.isPhysicalFile()) {
            return if (throwOnFailure) {
                physicalFileSystem.list(dir)
            } else {
                physicalFileSystem.listOrNull(dir)
            }
        }

        return runCatching {
            val uri = dir.toUri()
            var documentId = DocumentsContract.getDocumentId(uri)
            if (uri.isCifsDocument()) {
                documentId += '/'
            }
            val childrenUri = DocumentsContract.buildChildDocumentsUriUsingTree(uri, documentId)

            contentResolver.query(childrenUri, arrayOf(Document.COLUMN_DISPLAY_NAME), null, null, null)?.use { c ->
                List(c.count) {
                    c.moveToNext()
                    val displayName = c.getString(0)
                    dir / displayName
                }
            }
        }.getOrElse { if (throwOnFailure) throw it else null }
    }

    override fun metadataOrNull(path: Path): FileMetadata? {
        if (path.isPhysicalFile()) {
            return physicalFileSystem.metadataOrNull(path)
        }

        return runCatching {
            val uri = path.toUri()
            val isMediaUri = uri.authority == MediaStore.AUTHORITY
            val projection = if (isMediaUri) {
                arrayOf(MediaStore.MediaColumns.MIME_TYPE, MediaStore.MediaColumns.DATE_MODIFIED)
            } else {
                arrayOf(Document.COLUMN_MIME_TYPE, Document.COLUMN_LAST_MODIFIED)
            }

            contentResolver.query(uri, projection, null, null, null)?.use { c ->
                if (!c.moveToNext()) return null

                val mimeType = c.getString(0)
                val lastModified = c.getLongOrNull(1)?.let { if (isMediaUri) it * 1000 else it }
                val isDirectory = mimeType == Document.MIME_TYPE_DIR

                FileMetadata(
                    isRegularFile = !isDirectory,
                    isDirectory = isDirectory,
                    lastModifiedAtMillis = lastModified,
                )
            }
        }.getOrNull()
    }

    override fun openReadOnly(file: Path): FileHandle {
        TODO("Not yet implemented")
    }

    override fun openReadWrite(file: Path, mustCreate: Boolean, mustExist: Boolean): FileHandle {
        TODO("Not yet implemented")
    }

    override fun sink(file: Path, mustCreate: Boolean): Sink {
        TODO("Not yet implemented")
    }

    override fun source(file: Path): Source {
        TODO("Not yet implemented")
    }

    fun openFileDescriptor(path: Path, mode: String): ParcelFileDescriptor {
        if ('w' in mode && !exists(path)) {
            val parent = path.parent ?: throw IOException("Failed to open file: $path")
            createDirectories(parent)
            if (path.isPhysicalFile()) {
                physicalFileSystem.openReadWrite(path).close()
            } else {
                val displayName = path.name
                val extension = displayName.substringAfterLast('.', "").ifEmpty { null }?.lowercase()
                val mimeType = MimeTypeMap.getSingleton().getMimeTypeFromExtension(extension) ?: "application/octet-stream"
                DocumentsContract.createDocument(contentResolver, parent.toUri(), mimeType, displayName)
                    ?: throw IOException("Failed to open file: $path")
            }
        }

        return if (path.isPhysicalFile()) {
            ParcelFileDescriptor.open(path.toFile(), ParcelFileDescriptor.parseMode(mode))
        } else {
            contentResolver.openFileDescriptor(path.toUri(), mode)
        } ?: throw IOException("Failed to open file: $path")
    }
}

private fun Path.isPhysicalFile() = toString().startsWith('/')

private fun Uri.isCifsDocument() = authority == "com.wa2c.android.cifsdocumentsprovider.documents"

val SystemFileSystem = AndroidFileSystem(appCtx)
