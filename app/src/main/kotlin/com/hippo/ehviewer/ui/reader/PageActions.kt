package com.hippo.ehviewer.ui.reader

import android.Manifest
import android.content.ActivityNotFoundException
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
import moe.tarsin.snackbar
import moe.tarsin.string
import splitties.systemservices.clipboardManager

context(loader: PageLoader, ctx: Context)
private fun provideImage(index: Int): Uri? {
    val dir = AppConfig.externalTempDir ?: return null
    val name = loader.getImageFilename(index) ?: return null
    val file = (dir / name).takeIf { loader.save(index, it) } ?: return null
    return FileProvider.getUriForFile(ctx, "$APPLICATION_ID.fileprovider", file.toFile())
}

context(_: SnackbarHostState, ctx: Context, _: PageLoader)
suspend fun shareImage(page: Page, info: GalleryInfo? = null) {
    val error = string(R.string.error_cant_save_image)
    val share = string(R.string.share_image)
    val noActivity = string(R.string.error_cant_find_activity)
    val uri = provideImage(page.index)
    if (uri == null) {
        snackbar(error)
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
        ctx.startActivity(Intent.createChooser(intent, share))
    } catch (_: ActivityNotFoundException) {
        snackbar(noActivity)
    }
}

context(_: SnackbarHostState, ctx: Context, _: PageLoader)
suspend fun copy(page: Page) {
    val error = string(R.string.error_cant_save_image)
    val copied = string(R.string.copied_to_clipboard)
    val uri = provideImage(page.index)
    if (uri == null) {
        snackbar(error)
        return
    }
    val clipData = ClipData.newUri(ctx.contentResolver, "ehviewer", uri)
    clipboardManager.setPrimaryClip(clipData)
    if (!isAtLeastT) {
        snackbar(copied)
    }
}

context(_: SnackbarHostState, ctx: Context, loader: PageLoader)
suspend fun save(page: Page) {
    val granted = isAtLeastQ || requestPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE)
    val cannotSave = string(R.string.error_cant_save_image)
    if (granted) {
        val filename = loader.getImageFilename(page.index)
        if (filename == null) {
            snackbar(cannotSave)
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
                snackbar(cannotSave)
                return
            }
            values.put(MediaStore.MediaColumns.DATA, realPath + File.separator + filename)
        }
        val imageUri = ctx.contentResolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values)
        if (imageUri != null) {
            if (!loader.save(page.index, imageUri.toOkioPath())) {
                try {
                    ctx.contentResolver.delete(imageUri, null, null)
                } catch (e: Exception) {
                    e.logcat(e)
                }
                snackbar(cannotSave)
            } else if (isAtLeastQ) {
                val contentValues = ContentValues()
                contentValues.put(MediaStore.MediaColumns.IS_PENDING, 0)
                ctx.contentResolver.update(imageUri, contentValues, null, null)
            }
            snackbar(string(R.string.image_saved, realPath + File.separator + filename))
        } else {
            snackbar(cannotSave)
        }
    } else {
        snackbar(string(R.string.permission_denied))
    }
}

context(_: SnackbarHostState, _: Context, loader: PageLoader)
suspend fun saveTo(page: Page) {
    val filename = loader.getImageFilename(page.index)
    if (filename == null) {
        snackbar(string(R.string.error_cant_save_image))
        return
    }
    val extension = FileUtils.getExtensionFromFilename(filename)
    val mimeType = MimeTypeMap.getSingleton().getMimeTypeFromExtension(extension) ?: "image/jpeg"
    page.runSuspendCatching {
        val uri = awaitActivityResult(ActivityResultContracts.CreateDocument(mimeType), filename)
        if (uri != null) {
            loader.save(index, uri.toOkioPath())
            snackbar(string(R.string.image_saved, uri.displayPath))
        }
    }.onFailure {
        it.logcat(it)
        snackbar(string(R.string.error_cant_find_activity))
    }
}
