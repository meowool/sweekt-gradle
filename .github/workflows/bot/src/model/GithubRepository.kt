package com.meowool.sweekt.gradle.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * @author chachako
 */
@Serializable
data class GithubRepository(
  @SerialName("default_branch") val defaultBranch: String,
  @SerialName("open_issues_count") val openIssuesCount: Int,
)
