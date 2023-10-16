package tachiyomi.data.release

import android.os.Build
import com.hippo.ehviewer.BuildConfig
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

val apkVariant = "${BuildConfig.FLAVOR}-${Build.SUPPORTED_ABIS[0]}"

/**
 * Contains information about the latest release from GitHub.
 */
@Serializable
data class GithubRelease(
    @SerialName("tag_name") val version: String,
    @SerialName("body") val info: String,
    @SerialName("html_url") val releaseLink: String,
    @SerialName("assets") val assets: List<GitHubAssets>,
) {
    fun getDownloadLink(): String {
        val asset = assets.find { it.downloadLink.contains(apkVariant) }
            ?: assets.find { it.downloadLink.contains("${BuildConfig.FLAVOR}-universal") } ?: assets[0]
        return asset.downloadLink
    }
}

/**
 * Assets class containing download url.
 */
@Serializable
data class GitHubAssets(@SerialName("browser_download_url") val downloadLink: String)
