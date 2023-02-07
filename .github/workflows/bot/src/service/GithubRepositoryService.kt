@file:Suppress("MemberVisibilityCanBePrivate")

package com.meowool.sweekt.gradle.service

import com.meowool.sweekt.gradle.model.Context
import com.meowool.sweekt.gradle.model.GithubBranch
import com.meowool.sweekt.gradle.model.GithubCommitsComparison
import com.meowool.sweekt.gradle.model.GithubIssue
import com.meowool.sweekt.gradle.model.GithubRelease
import com.meowool.sweekt.gradle.model.GithubRepository
import com.meowool.sweekt.gradle.utils.githubFlatPaginate
import com.meowool.sweekt.gradle.utils.jsonMapOf
import com.meowool.sweekt.gradle.utils.nodeFetch
import com.meowool.sweekt.gradle.utils.readFileBuffer
import com.meowool.sweekt.gradle.utils.withDebug
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.HttpRequestBuilder
import io.ktor.client.request.get
import io.ktor.client.request.parameter
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.ContentType.Application
import io.ktor.http.HttpHeaders.ContentType
import io.ktor.http.contentType
import kotlinx.coroutines.await
import kotlinx.coroutines.flow.asFlow
import kotlinx.coroutines.flow.onCompletion
import kotlinx.coroutines.flow.onEach
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json

/**
 * @author chachako
 */
class GithubRepositoryService(
  private val context: Context,
  private val client: HttpClient,
  private val json: Json,
) {
  private val baseUrl = context.run {
    "$githubApiUrl/repos/$repositoryWithOwner"
  } // ktlint-disable max-line-length
  private var paginateRequests = mutableMapOf<String, dynamic>()
  private var info: GithubRepository? = null
  private var latestRelease: GithubRelease? = null

  val branches = paginate<GithubBranch>("branches")

  val tags = paginate<GithubBranch>("tags")

  fun branchUrl(branch: Any) = "${context.repositoryUrl}/tree/$branch"

  fun issues(
    state: GithubIssue.State,
    vararg labels: Any,
  ) = withDebug("issues") {
    paginate<GithubIssue>("issues") {
      parameter("state", state)
      if (labels.isNotEmpty()) parameter("labels", labels.joinToString(","))
    }
  }

  suspend fun defaultBranch() = GithubBranch(info().defaultBranch)

  /**
   * Creates a new issue.
   *
   * @param title The title of the issue.
   * @param body The contents of the issue.
   * @param milestone The number of the milestone to associate this issue with.
   *   NOTE: Only users with push access can set the milestone for new issues.
   *   The milestone is silently dropped otherwise.
   * @param labels Labels to associate with this issue.
   *   NOTE: Only users with push access can set labels for new issues. Labels
   *   are silently dropped otherwise.
   * @param assignees Logins for Users to assign to this issue.
   *   NOTE: Only users with push access can set assignees for new issues.
   *   Assignees are silently dropped otherwise.
   */
  suspend fun createIssue(
    title: String,
    body: String,
    milestone: Int? = null,
    labels: List<String> = emptyList(),
    assignees: List<String> = emptyList(),
  ) = withDebug("createIssue") {
    client.post("$baseUrl/issues") {
      contentType(Application.Json)
      setBody(
        jsonMapOf(
          "title" to title,
          "body" to body,
          "milestone" to milestone,
          "labels" to labels,
          "assignees" to assignees,
        ),
      )
    }
  }

  suspend fun commentIssue(
    number: Int,
    body: String,
  ) = withDebug("commentIssue") {
    client.post("$baseUrl/issues/$number/comments") {
      contentType(Application.Json)
      setBody(jsonMapOf("body" to body))
    }
  }

  suspend fun updateIssue(
    number: Int,
    title: String? = null,
    body: String? = null,
    state: GithubIssue.State? = null,
    stateReason: GithubIssue.State.Reason? = null,
    milestone: Int? = null,
    labels: List<String>? = null,
    assignees: List<String>? = null,
  ) = withDebug("updateIssue") {
    client.post("$baseUrl/issues/$number") {
      contentType(Application.Json)
      setBody(
        buildMap {
          title?.let { put("title", it) }
          body?.let { put("body", it) }
          state?.let { put("state", it) }
          stateReason?.let { put("state_reason", it) }
          milestone?.let { put("milestone", it) }
          labels?.let { put("labels", it) }
          assignees?.let { put("assignees", it) }
        },
      )
    }
  }

  suspend fun closeIssue(number: Int) = withDebug("closeIssue") {
    updateIssue(number, state = GithubIssue.State.Closed)
  }

  suspend fun compareCommits(baseHead: String) = withDebug("compareCommits") {
    client.get(
      "${context.githubApiUrl}/repos/gradle/gradle/compare/$baseHead",
    ).body<GithubCommitsComparison>()
  }

  suspend fun latestRelease() = withDebug("latestRelease") {
    latestRelease ?: client
      .get("$baseUrl/releases/latest")
      .body<GithubRelease>()
      .also { latestRelease = it }
  }

  suspend fun release(tag: String) = withDebug("release") {
    client.get("$baseUrl/releases/tags/$tag").body<GithubRelease>()
  }

  suspend fun uploadReleaseAsset(
    release: GithubRelease,
    name: String,
    file: String,
    contentType: ContentType = Application.Zip,
  ): GithubRelease.Asset = withDebug("uploadReleaseAsset") {
    val response = nodeFetch(
      url = release.uploadUrl.removeSuffix("{?name,label}") + "?name=$name",
      body = file.readFileBuffer(),
      headers = context.headers + (ContentType to contentType.toString()),
      method = "POST",
    ).text().await()
    return try {
      json.decodeFromString(response)
    } catch (e: Throwable) {
      error(
        buildString {
          appendLine("Failed to upload release asset: $response")
          appendLine(e.stackTraceToString())
        },
      )
    }
  }

  private suspend fun info() = withDebug("info") {
    info ?: client.get(baseUrl)
      .body<GithubRepository>()
      .also { info = it }
  }

  private inline fun <reified T> paginate(
    paths: String,
    crossinline block: HttpRequestBuilder.() -> Unit = {},
  ) = paginateRequests[paths]
    ?.unsafeCast<List<T>>()
    ?.asFlow() ?: mutableListOf<T>().let { result ->
    client.githubFlatPaginate<T>("$baseUrl/$paths", json, block)
      .onEach { result += it }
      .onCompletion { paginateRequests[paths] = result }
  }
}
