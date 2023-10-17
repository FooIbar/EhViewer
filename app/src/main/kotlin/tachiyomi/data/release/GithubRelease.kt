package tachiyomi.data.release

import android.os.Build
import com.hippo.ehviewer.BuildConfig
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

const val FLAVOR = "${BuildConfig.FLAVOR_api}-${BuildConfig.FLAVOR_oss}"
val apkVariant = "$FLAVOR-${
    Build.SUPPORTED_ABIS[0].takeIf {
        it in setOf("arm64-v8a", "x86_64", "armeabi-v7a", "x86")
    } ?: "universal"
}"

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
        val asset = assets.find { it.downloadLink.contains(apkVariant) } ?: assets[0]
        return asset.downloadLink
    }
}

/**
 * Assets class containing download url.
 */
@Serializable
data class GitHubAssets(@SerialName("browser_download_url") val downloadLink: String)
