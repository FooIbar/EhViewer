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
        val asset = assets.find { AppConfig.matchVariant(it.name) } ?: assets[0]
        return asset.url
    }
}

/**
 * Assets class containing download url.
 */
@Serializable
data class GitHubAssets(val url: String, val name: String)
