package tachiyomi.data.release

import kotlinx.serialization.Serializable

@Serializable
data class GithubCommitComparison(val commits: List<GithubCommit>)

@Serializable
data class GithubCommit(val commit: GithubCommitDetail)

@Serializable
data class GithubCommitDetail(val author: GithubCommitAuthor, val message: String)

@Serializable
data class GithubCommitAuthor(val name: String)
