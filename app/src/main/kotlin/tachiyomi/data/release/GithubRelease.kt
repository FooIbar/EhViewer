package tachiyomi.data.release

import com.hippo.ehviewer.util.AppConfig
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

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
        val asset = assets.find { it.downloadLink.contains(AppConfig.abi) } ?: assets[0]
        return asset.downloadLink
    }
}

/**
 * Assets class containing download url.
 */
@Serializable
data class GitHubAssets(@SerialName("browser_download_url") val downloadLink: String)
