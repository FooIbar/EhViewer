package com.hippo.ehviewer.ui.reader

import android.Manifest
import android.content.ClipData
import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Environment
import android.provider.MediaStore
import android.webkit.MimeTypeMap
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.material3.SnackbarHostState
import androidx.core.content.FileProvider
import com.ehviewer.core.common.Res
import com.ehviewer.core.common.copied_to_clipboard
import com.ehviewer.core.common.error_cant_find_activity
import com.ehviewer.core.common.error_cant_save_image
import com.ehviewer.core.common.image_saved
import com.ehviewer.core.common.permission_denied
import com.ehviewer.core.common.share_image
import com.hippo.ehviewer.BuildConfig.APPLICATION_ID
import com.hippo.ehviewer.client.EhUrl
import com.hippo.ehviewer.client.data.GalleryInfo
import com.hippo.ehviewer.gallery.Page
import com.hippo.ehviewer.gallery.PageLoader
import com.hippo.ehviewer.util.AppConfig
import com.hippo.ehviewer.util.FileUtils
import com.hippo.ehviewer.util.awaitActivityResult
import com.hippo.ehviewer.util.displayPath
import com.hippo.ehviewer.util.isAtLeastQ
import com.hippo.ehviewer.util.isAtLeastT
import com.hippo.ehviewer.util.requestPermission
import com.hippo.files.toOkioPath
import eu.kanade.tachiyomi.util.system.logcat
import java.io.File
import kotlinx.datetime.Clock
import moe.tarsin.coroutines.runSuspendCatching
import org.jetbrains.compose.resources.getString
import splitties.systemservices.clipboardManager

context(PageLoader)
private fun Context.provideImage(index: Int): Uri? {
    val dir = AppConfig.externalTempDir ?: return null
    val name = getImageFilename(index) ?: return null
    val file = (dir / name).takeIf { save(index, it) } ?: return null
    return FileProvider.getUriForFile(this, "$APPLICATION_ID.fileprovider", file.toFile())
}

context(SnackbarHostState, Context, PageLoader)
suspend fun shareImage(page: Page, info: GalleryInfo? = null) {
    val error = getString(Res.string.error_cant_save_image)
    val share = getString(Res.string.share_image)
    val noActivity = getString(Res.string.error_cant_find_activity)
    val uri = provideImage(page.index)
    if (uri == null) {
        showSnackbar(error)
        return
    }
    val intent = Intent(Intent.ACTION_SEND).apply {
        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        putExtra(Intent.EXTRA_STREAM, uri)
        info?.apply { putExtra(Intent.EXTRA_TEXT, EhUrl.getGalleryDetailUrl(gid, token)) }
        val extension = FileUtils.getExtensionFromFilename(uri.path)
        val mimeType = MimeTypeMap.getSingleton().getMimeTypeFromExtension(extension) ?: "image/jpeg"
        setDataAndType(uri, mimeType)
    }
    try {
        startActivity(Intent.createChooser(intent, share))
    } catch (e: Throwable) {
        showSnackbar(noActivity)
    }
}

context(SnackbarHostState, Context, PageLoader)
suspend fun copy(page: Page) {
    val error = getString(Res.string.error_cant_save_image)
    val copied = getString(Res.string.copied_to_clipboard)
    val uri = provideImage(page.index)
    if (uri == null) {
        showSnackbar(error)
        return
    }
    val clipData = ClipData.newUri(contentResolver, "ehviewer", uri)
    clipboardManager.setPrimaryClip(clipData)
    if (!isAtLeastT) {
        showSnackbar(copied)
    }
}

context(SnackbarHostState, Context, PageLoader)
suspend fun save(page: Page) {
    val granted = isAtLeastQ || requestPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE)
    val cannotSave = getString(Res.string.error_cant_save_image)
    if (granted) {
        val filename = getImageFilename(page.index)
        if (filename == null) {
            showSnackbar(cannotSave)
            return
        }
        val extension = FileUtils.getExtensionFromFilename(filename)
        val mimeType = MimeTypeMap.getSingleton().getMimeTypeFromExtension(extension) ?: "image/jpeg"
        val values = ContentValues()
        val realPath: String
        values.put(MediaStore.MediaColumns.DISPLAY_NAME, filename)
        values.put(MediaStore.MediaColumns.DATE_ADDED, Clock.System.now().epochSeconds)
        values.put(MediaStore.MediaColumns.MIME_TYPE, mimeType)
        if (isAtLeastQ) {
            realPath = Environment.DIRECTORY_PICTURES + File.separator + AppConfig.APP_DIRNAME
            values.put(MediaStore.MediaColumns.RELATIVE_PATH, realPath)
            values.put(MediaStore.MediaColumns.IS_PENDING, 1)
        } else {
            val dir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES)
            val path = File(dir, AppConfig.APP_DIRNAME)
            realPath = path.toString()
            if (!FileUtils.ensureDirectory(path)) {
                showSnackbar(cannotSave)
                return
            }
            values.put(MediaStore.MediaColumns.DATA, realPath + File.separator + filename)
        }
        val imageUri = contentResolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values)
        if (imageUri != null) {
            if (!save(page.index, imageUri.toOkioPath())) {
                try {
                    contentResolver.delete(imageUri, null, null)
                } catch (e: Exception) {
                    e.logcat(e)
                }
                showSnackbar(cannotSave)
            } else if (isAtLeastQ) {
                val contentValues = ContentValues()
                contentValues.put(MediaStore.MediaColumns.IS_PENDING, 0)
                contentResolver.update(imageUri, contentValues, null, null)
            }
            showSnackbar(getString(Res.string.image_saved, realPath + File.separator + filename))
        } else {
            showSnackbar(cannotSave)
        }
    } else {
        showSnackbar(getString(Res.string.permission_denied))
    }
}

context(SnackbarHostState, Context, PageLoader)
suspend fun saveTo(page: Page) {
    val filename = getImageFilename(page.index)
    if (filename == null) {
        showSnackbar(getString(Res.string.error_cant_save_image))
        return
    }
    val extension = FileUtils.getExtensionFromFilename(filename)
    val mimeType = MimeTypeMap.getSingleton().getMimeTypeFromExtension(extension) ?: "image/jpeg"
    page.runSuspendCatching {
        val uri = awaitActivityResult(ActivityResultContracts.CreateDocument(mimeType), filename)
        if (uri != null) {
            save(index, uri.toOkioPath())
            showSnackbar(getString(Res.string.image_saved, uri.displayPath.orEmpty()))
        }
    }.onFailure {
        it.logcat(it)
        showSnackbar(getString(Res.string.error_cant_find_activity))
    }
}
