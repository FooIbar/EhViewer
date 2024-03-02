package com.hippo.ehviewer.ui.reader

import android.content.ClipData
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.webkit.MimeTypeMap
import androidx.compose.material3.SnackbarHostState
import androidx.core.content.FileProvider
import com.hippo.ehviewer.BuildConfig.APPLICATION_ID
import com.hippo.ehviewer.R
import com.hippo.ehviewer.client.EhUrl
import com.hippo.ehviewer.client.data.GalleryInfo
import com.hippo.ehviewer.gallery.PageLoader2
import com.hippo.ehviewer.util.AppConfig
import com.hippo.ehviewer.util.FileUtils
import com.hippo.ehviewer.util.isAtLeastT
import com.hippo.unifile.asUniFile
import eu.kanade.tachiyomi.ui.reader.model.ReaderPage
import java.io.File
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
