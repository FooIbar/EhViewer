package tachiyomi.data.release

import com.hippo.ehviewer.util.AppConfig
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class GithubRepo(
    @SerialName("default_branch") val defaultBranch: String,
)

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
        // The default order is upload order, so we need to sort it
        return artifacts.sortedBy { it.name }.run {
            find { AppConfig.matchVariant(it.name) } ?: this[0]
        }.downloadLink
    }
}

@Serializable
data class GithubArtifact(
    val name: String,
    @SerialName("archive_download_url") val downloadLink: String,
)
