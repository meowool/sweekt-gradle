package com.meowool.sweekt.gradle.model

/**
 * @author chachako
 */
data class MergeChangesResult(
  val successBranches: Collection<GithubBranch>,
  val failedBranches: Collection<GithubBranch>,
) {
  val isSuccess: Boolean inline get() = failedBranches.isEmpty()
  val isFailure: Boolean inline get() = !isSuccess
}
