@file:Suppress("BlockingMethodInNonBlockingContext")

package com.hippo.ehviewer.coil

import android.webkit.MimeTypeMap
import coil3.ComponentRegistry
import coil3.ImageLoader
import coil3.Uri
import coil3.decode.DataSource
import coil3.decode.ImageSource
import coil3.fetch.FetchResult
import coil3.fetch.Fetcher
import coil3.fetch.SourceFetchResult
import coil3.request.Options
import com.hippo.ehviewer.client.ehRequest
import com.hippo.ehviewer.util.copyTo
import io.ktor.client.statement.bodyAsChannel

/**
 * A HttpUriFetcher impl use Ktor (Will do jvmheapless IO when backing engine is Cronet)
 * cacheHeader is never respected | recorded since thumb is immutable
 */
class KtorHttpUriFetcher(private val data: String, private val options: Options, private val imageLoader: ImageLoader) : Fetcher {
    override suspend fun fetch(): FetchResult {
        val diskCacheKey = options.diskCacheKey ?: data
        val diskCache = requireNotNull(imageLoader.diskCache)
        val snapshot = diskCache.openSnapshot(diskCacheKey) ?: run {
            val success = ehRequest(data).execute {
                diskCache.suspendEdit(diskCacheKey) {
                    it.bodyAsChannel().copyTo(data.toFile())
                }
            }
            check(success)
            // diskcache snapshot MUST exist here
            requireNotNull(diskCache.openSnapshot(diskCacheKey))
        }
        return SourceFetchResult(
            source = ImageSource(snapshot.data, diskCache.fileSystem, diskCacheKey, snapshot),
            mimeType = getMimeType(data),
            dataSource = DataSource.DISK,
        )
    }
}

fun ComponentRegistry.Builder.installKtorHttpUriFetcher() = add { data: Uri, options, loader ->
    if (data.scheme != "http" && data.scheme != "https") return@add null
    KtorHttpUriFetcher(data.toString(), options, loader)
}

private fun getMimeType(url: String) = MimeTypeMap.getSingleton().getMimeTypeFromUrl(url)

private fun MimeTypeMap.getMimeTypeFromUrl(url: String?): String? {
    if (url.isNullOrBlank()) return null
    val extension = url
        .substringBeforeLast('#') // Strip the fragment.
        .substringBeforeLast('?') // Strip the query.
        .substringAfterLast('/') // Get the last path segment.
        .substringAfterLast('.', missingDelimiterValue = "") // Get the file extension.
    return getMimeTypeFromExtension(extension)
}
