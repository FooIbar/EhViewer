package com.hippo.ehviewer.coil

import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import coil3.asCoilImage
import coil3.decode.DecodeResult
import coil3.decode.Decoder
import coil3.request.CachePolicy
import coil3.request.ImageRequest
import coil3.request.httpHeaders
import coil3.size.Size
import com.hippo.ehviewer.Settings
import com.hippo.ehviewer.client.CHROME_ACCEPT
import com.hippo.ehviewer.client.CHROME_ACCEPT_LANGUAGE
import com.hippo.ehviewer.client.data.GalleryInfo
import com.hippo.ehviewer.client.data.GalleryPreview
import com.hippo.ehviewer.client.data.NormalGalleryPreview
import com.hippo.ehviewer.client.url
import com.hippo.ehviewer.spider.DownloadInfoMagics.encodeMagicRequestOrUrl
import io.ktor.http.HttpHeaders
import io.ktor.http.headers

private val header = headers {
    append(HttpHeaders.UserAgent, Settings.userAgent)
    append(HttpHeaders.Accept, CHROME_ACCEPT)
    append(HttpHeaders.AcceptLanguage, CHROME_ACCEPT_LANGUAGE)
}

fun ImageRequest.Builder.ehUrl(info: GalleryInfo) = apply {
    val key = info.thumbKey!!
    data(encodeMagicRequestOrUrl(info))
    memoryCacheKey(key)
    diskCacheKey(key)
    httpHeaders(header)
}

fun ImageRequest.Builder.ehPreview(preview: GalleryPreview) = apply {
    data(preview.url)
    memoryCacheKey(preview.imageKey)
    diskCacheKey(preview.imageKey)
    if (preview is NormalGalleryPreview) size(Size.ORIGINAL)
    httpHeaders(header)
}

private val stubImage = ColorDrawable(Color.BLACK).asCoilImage(true)
private val stubResult = DecodeResult(stubImage, false)
private val stubFactory = Decoder { stubResult }

fun ImageRequest.Builder.justDownload() = apply {
    memoryCachePolicy(CachePolicy.DISABLED)
    decoderFactory { _, _, _ -> stubFactory }
}
