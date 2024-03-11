package tachiyomi.data.release

import com.hippo.ehviewer.util.AppConfig
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class GithubWorkflowRuns(
    @SerialName("workflow_runs") val workflowRuns: List<GithubWorkflowRun>,
)

@Serializable
data class GithubWorkflowRun(
    @SerialName("head_sha") val headSha: String,
    @SerialName("display_title") val title: String,
    @SerialName("artifacts_url") val artifactsUrl: String,
)

@Serializable
data class GithubArtifacts(
    @SerialName("artifacts") val artifacts: List<GithubArtifact>,
) {
    fun getDownloadLink(): String {
        return (artifacts.find { it.name.contains(AppConfig.abi) } ?: artifacts[0]).downloadLink
    }
}

@Serializable
data class GithubArtifact(
    val name: String,
    @SerialName("archive_download_url") val downloadLink: String,
)
