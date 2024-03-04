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
import com.hippo.ehviewer.BuildConfig.APPLICATION_ID
import com.hippo.ehviewer.R
import com.hippo.ehviewer.client.EhUrl
import com.hippo.ehviewer.client.data.GalleryInfo
import com.hippo.ehviewer.gallery.PageLoader2
import com.hippo.ehviewer.util.AppConfig
import com.hippo.ehviewer.util.FileUtils
import com.hippo.ehviewer.util.awaitActivityResult
import com.hippo.ehviewer.util.isAtLeastQ
import com.hippo.ehviewer.util.isAtLeastT
import com.hippo.ehviewer.util.requestPermission
import com.hippo.unifile.asUniFile
import com.hippo.unifile.displayPath
import eu.kanade.tachiyomi.ui.reader.model.ReaderPage
import eu.kanade.tachiyomi.util.system.logcat
import java.io.File
import kotlinx.datetime.Clock
import moe.tarsin.coroutines.runSuspendCatching
import splitties.systemservices.clipboardManager

context(PageLoader2)
private fun Context.provideImage(index: Int): Uri? {
    val dir = AppConfig.externalTempDir ?: return null
    val name = saveToDir(index, dir.asUniFile())?.name ?: return null
    return FileProvider.getUriForFile(this, "$APPLICATION_ID.fileprovider", File(dir, name))
}

context(SnackbarHostState, Context, PageLoader2)
suspend fun shareImage(page: ReaderPage, info: GalleryInfo? = null) {
    val error = getString(R.string.error_cant_save_image)
    val share = getString(R.string.share_image)
    val noActivity = getString(R.string.error_cant_find_activity)
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

context(SnackbarHostState, Context, PageLoader2)
suspend fun copy(page: ReaderPage) {
    val error = getString(R.string.error_cant_save_image)
    val copied = getString(R.string.copied_to_clipboard)
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

context(SnackbarHostState, Context, PageLoader2)
suspend fun save(page: ReaderPage) {
    val granted = isAtLeastQ || requestPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE)
    val cannotSave = getString(R.string.error_cant_save_image)
    if (granted) {
        val filename = getImageFilename(page.index)
        val extension = FileUtils.getExtensionFromFilename(filename)
        val mimeType = MimeTypeMap.getSingleton().getMimeTypeFromExtension(extension) ?: "image/jpeg"
        val values = ContentValues()
        val realPath: String
        values.put(MediaStore.MediaColumns.DISPLAY_NAME, filename)
        values.put(MediaStore.Images.Media.DATE_ADDED, Clock.System.now().toEpochMilliseconds())
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
            if (!save(page.index, imageUri.asUniFile())) {
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
            showSnackbar(getString(R.string.image_saved, realPath + File.separator + filename))
        } else {
            showSnackbar(cannotSave)
        }
    } else {
        showSnackbar(getString(R.string.permission_denied))
    }
}

context(SnackbarHostState, Context, PageLoader2)
suspend fun saveTo(page: ReaderPage) {
    val filename = getImageFilename(page.index)
    val extension = FileUtils.getExtensionFromFilename(filename)
    val mimeType = MimeTypeMap.getSingleton().getMimeTypeFromExtension(extension) ?: "image/jpeg"
    page.runSuspendCatching {
        val uri = awaitActivityResult(ActivityResultContracts.CreateDocument(mimeType), filename)
        if (uri != null) {
            save(index, uri.asUniFile())
            showSnackbar(getString(R.string.image_saved, uri.displayPath))
        }
    }.onFailure {
        it.logcat(it)
        showSnackbar(getString(R.string.error_cant_find_activity))
    }
}
