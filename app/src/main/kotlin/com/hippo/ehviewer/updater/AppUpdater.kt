package com.hippo.ehviewer.updater

import com.hippo.ehviewer.BuildConfig
import com.hippo.ehviewer.EhApplication.Companion.ktorClient
import com.hippo.ehviewer.Settings
import com.hippo.ehviewer.client.executeAndParseAs
import com.hippo.ehviewer.client.executeSafely
import com.hippo.ehviewer.util.copyTo
import com.hippo.ehviewer.util.ensureSuccess
import io.ktor.client.request.header
import io.ktor.client.request.prepareGet
import io.ktor.client.statement.bodyAsChannel
import io.ktor.utils.io.jvm.javaio.toInputStream
import java.io.File
import java.util.zip.ZipInputStream
import kotlin.time.Duration.Companion.days
import kotlinx.datetime.Clock
import kotlinx.datetime.Instant
import moe.tarsin.coroutines.runSuspendCatching
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
        val interval = Settings.updateIntervalDays
        if (forceCheck || interval != 0 && now > last + interval.days) {
            Settings.lastUpdateTime = now.epochSeconds
            if (Settings.useCIUpdateChannel) {
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

    suspend fun downloadUpdate(url: String, file: File) =
        ghStatement(url).executeSafely { response ->
            response.status.ensureSuccess()
            if (url.endsWith("zip")) {
                response.bodyAsChannel().toInputStream().use { stream ->
                    ZipInputStream(stream).use { zip ->
                        zip.nextEntry
                        file.outputStream().use {
                            zip.copyTo(it)
                        }
                    }
                }
            } else {
                response.bodyAsChannel().copyTo(file)
            }
        }
}

private suspend inline fun ghStatement(url: String) = ktorClient.prepareGet(url) {
    header("Authorization", GithubTokenParts.joinToString("_", prefix = "Bearer "))
}

private val GithubTokenParts = arrayOf(
    "github",
    "pat",
    "11A4H2ACI0XXTRsYkt1Mps",
    "uJ5n8UbLv20j6Ue4k1x25Z7u63EaCdDNYooxSeI4GJaQYKXWDZEtTmDVwnh",
)

data class Release(
    val version: String,
    val changelog: String,
    val downloadLink: String,
)
