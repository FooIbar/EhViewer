package com.hippo.ehviewer.updater

import com.hippo.ehviewer.BuildConfig
import com.hippo.ehviewer.EhApplication.Companion.ktorClient
import com.hippo.ehviewer.Settings
import com.hippo.ehviewer.client.executeAndParseAs
import com.hippo.ehviewer.spider.timeoutBySpeed
import com.hippo.ehviewer.util.copyTo
import com.hippo.files.write
import io.ktor.client.request.HttpRequestBuilder
import io.ktor.client.request.accept
import io.ktor.client.request.bearerAuth
import io.ktor.client.request.prepareGet
import io.ktor.client.statement.bodyAsChannel
import io.ktor.http.ContentType
import io.ktor.utils.io.jvm.javaio.toInputStream
import java.util.zip.ZipInputStream
import kotlin.time.Clock
import kotlin.time.Duration.Companion.days
import kotlin.time.Instant
import kotlinx.io.asSource
import moe.tarsin.coroutines.runSuspendCatching
import okio.Path
import tachiyomi.data.release.GithubArtifacts
import tachiyomi.data.release.GithubCommitComparison
import tachiyomi.data.release.GithubRelease
import tachiyomi.data.release.GithubRepo
import tachiyomi.data.release.GithubWorkflowRuns

private const val API_URL = "https://api.github.com/repos/${BuildConfig.REPO_NAME}"
private const val LATEST_RELEASE_URL = "$API_URL/releases/latest"

object AppUpdater {
    suspend fun checkForUpdate(forceCheck: Boolean = false): Release? {
        val now = Clock.System.now()
        val last = Instant.fromEpochSeconds(Settings.lastUpdateTime)
        val interval = Settings.updateIntervalDays.value
        if (forceCheck || interval != 0 && now > last + interval.days) {
            Settings.lastUpdateTime = now.epochSeconds
            if (Settings.useCIUpdateChannel.value) {
                val curSha = BuildConfig.COMMIT_SHA
                val branch = ghStatement(API_URL).executeAndParseAs<GithubRepo>().defaultBranch
                val workflowRunsUrl = "$API_URL/actions/workflows/ci.yml/runs?branch=$branch&event=push&status=success&per_page=1"
                val workflowRun = ghStatement(workflowRunsUrl).executeAndParseAs<GithubWorkflowRuns>().workflowRuns[0]
                val shortSha = workflowRun.headSha.take(7)
                if (shortSha != curSha) {
                    val artifacts = ghStatement(workflowRun.artifactsUrl).executeAndParseAs<GithubArtifacts>()
                    val archiveUrl = artifacts.getDownloadLink()
                    val changelog = runSuspendCatching {
                        val commitComparisonUrl = "$API_URL/compare/$curSha...$shortSha"
                        val result = ghStatement(commitComparisonUrl).executeAndParseAs<GithubCommitComparison>()
                        // TODO: Prettier format, Markdown?
                        result.commits.joinToString("\n") { commit ->
                            "${commit.commit.message.takeWhile { it != '\n' }} (@${commit.commit.author.name})"
                        }
                    }.getOrDefault(workflowRun.title)
                    return Release(shortSha, changelog, archiveUrl)
                }
            } else {
                val curVersion = BuildConfig.RAW_VERSION_NAME
                val release = ghStatement(LATEST_RELEASE_URL).executeAndParseAs<GithubRelease>()
                val latestVersion = release.version
                val description = release.info
                val downloadUrl = release.getDownloadLink()
                if (latestVersion != curVersion) {
                    return Release(latestVersion, description, downloadUrl)
                }
            }
        }
        return null
    }

    suspend fun downloadUpdate(url: String, path: Path) {
        val isZip = url.endsWith("zip")
        timeoutBySpeed(
            url,
            {
                ghStatement(url) {
                    // https://docs.github.com/en/rest/releases/assets?apiVersion=2022-11-28#get-a-release-asset
                    if (!isZip) accept(ContentType.Application.OctetStream)
                    it()
                }
            },
            { _, _, _ -> },
            { response ->
                if (isZip) {
                    response.bodyAsChannel().toInputStream().use { stream ->
                        ZipInputStream(stream).use { zip ->
                            zip.nextEntry
                            path.write { transferFrom(zip.asSource()) }
                        }
                    }
                } else {
                    response.bodyAsChannel().copyTo(path)
                }
            },
        )
    }
}

private suspend inline fun ghStatement(
    url: String,
    builder: HttpRequestBuilder.() -> Unit = {},
) = ktorClient.prepareGet(url) {
    bearerAuth(GithubTokenParts.joinToString("_"))
    apply(builder)
}

private val GithubTokenParts = arrayOf(
    "github",
    "pat",
    "11A4H2ACI0iGDuL1O6wPYW",
    "OTFg8xaCNUwR1NHaJE1AT3LoYPfz6bouI7E7ReLf8GjIRFHCL5UsHL9EnWP",
)

data class Release(
    val version: String,
    val changelog: String,
    val downloadLink: String,
)
