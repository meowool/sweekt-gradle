package com.meowool.sweekt.gradle.model

import kotlinx.serialization.Serializable

/**
 * @author chachako
 */
@Serializable
data class GithubCommitsComparison(val files: List<File>) {
  @Serializable
  data class File(val filename: String)
}
