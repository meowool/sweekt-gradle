package com.meowool.sweekt.gradle.job

import com.meowool.sweekt.gradle.model.BotIssue
import com.meowool.sweekt.gradle.model.BotIssueBodyTemplate
import com.meowool.sweekt.gradle.model.BotIssueBodyTemplate.Companion.buildIssueBody
import com.meowool.sweekt.gradle.model.GithubBranch
import com.meowool.sweekt.gradle.service.BotService
import com.meowool.sweekt.gradle.service.GitService
import com.meowool.sweekt.gradle.service.GithubRepositoryService
import com.meowool.sweekt.gradle.utils.debug
import com.meowool.sweekt.gradle.utils.info
import com.meowool.sweekt.gradle.utils.warning
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.toList
import com.meowool.sweekt.gradle.service.GradleService.Companion.RepositoryUrl as GradleRepositoryUrl

/**
 * @author chachako
 */
class MergeChangesJob(
  private val bot: BotService,
  private val git: GitService,
  private val repository: GithubRepositoryService,
) : BotJob() {

  override suspend fun start(input: BotJobData): BotJobResult =
    when (val currentBranch = workflowTriggerRef.branch) {
      repository.defaultBranch() -> triggeredByDefaultBranch(currentBranch)
      else -> triggeredByChangedBranch(currentBranch)
    }

  /**
   * This method will be called when the [defaultBranch] is triggered.
   * In this case, we need to merge the [defaultBranch] into all other
   * 'changed/_' branches. Any branch that fails to merge will be renamed
   * 'unchanged/_'.
   */
  private suspend fun triggeredByDefaultBranch(
    defaultBranch: GithubBranch,
  ): BotJobResult = debug("triggeredByDefaultBranch($defaultBranch)") {
    val outputBranches = mutableListOf(defaultBranch)
    // Here is an uncertainty:
    // The default branch may not be successfully merged into other
    // branches, so we need to add a layer of insurance by trying to
    // merge other successfully "changed" branches into other failed
    // "unchanged" branches again.
    //
    // 1. Try to merge and filter failed branches
    val otherBranches = bot.changedBranches
      .filter { it != defaultBranch }
      .toList()
    val firstResult = bot.mergeChanges(
      fromBranches = defaultBranch,
      toBranches = otherBranches,
      onSuccess = ::onSuccessfulMerge,
    ).also { outputBranches += it.successBranches }

    // 2. Try to merge again with success branches
    if (firstResult.isFailure) bot.mergeChanges(
      fromBranches = firstResult.successBranches,
      toBranches = firstResult.failedBranches,
      onSuccess = ::onSuccessfulMerge,
      // If the try again fails, we need to rename it and open an issue
      onFail = { onFailedMerge(it) },
    ).also { outputBranches += it.successBranches }

    // 3. Output default branch and merged branches
    return jobSuccess(jobDataOf(Keys.Branches to outputBranches))
  }

  /**
   * This method will be called when the 'changed/_' branch is triggered.
   * In this case, we need to merge other 'changed/_' branches into the
   * [triggeredBranch].
   */
  private suspend fun triggeredByChangedBranch(
    triggeredBranch: GithubBranch,
  ): BotJobResult = debug("triggeredByChangedBranch($triggeredBranch)") {
    val result = bot.mergeChanges(
      fromBranches = bot.changedBranches.filter { it != triggeredBranch },
      toBranches = triggeredBranch,
      onSuccess = ::onSuccessfulMerge,
      onFail = { onFailedMerge(it) },
    )

    return when (result.isSuccess) {
      true -> jobSuccess(jobDataOf(Keys.Branches to result.successBranches))
      false -> jobFailure()
    }
  }

  private suspend fun BotIssueBodyTemplate.onFailedMerge(failedBranch: Any) {
    warning("🧨 Failed to merge changes to `$failedBranch` branch.")

    // Rename the branch that failed to merge
    val defaultBranch = repository.defaultBranch()
    val defaultVersion = bot.parseBranchVersion(defaultBranch)
    val renamedBranch = bot.renameToUnchangedBranch(failedBranch)
    val renamedBranchUrl = repository.branchUrl(renamedBranch)

    // Rename a branch that has failed to merge changes
    git.checkout(failedBranch).renameRemoteBranch(renamedBranch).push()

    // Create an issue to track errors
    val issueBody = buildIssueBody {
      sections += "Important" to
        "Since the latest **distribution changes** cannot be merged " +
        "into the `$failedBranch` branch, so it has now been renamed " +
        "to [`$renamedBranch`]($renamedBranchUrl) branch."

      sections += "Distribution Changes" to
        "$GradleRepositoryUrl/compare/$defaultVersion..." +
        "meowool:sweekt-gradle:$defaultBranch"

      stepsToReproduce = """
        ```console
        $ git checkout $failedBranch
        $ git merge $defaultBranch
        ```
      """.trimIndent()

      related = failedBranch
    }

    bot.createIssue(
      title = "Cannot merge Sweekt distribution changes to `$failedBranch` branch",
      body = issueBody,
      labels = listOf(BotIssue.MergeLabel),
    )
  }

  private suspend fun onSuccessfulMerge(mergedBranch: Any) {
    info("🎉 Successfully merged changes to `$mergedBranch` branch.")

    val renamedBranch = bot.renameToChangedBranch(mergedBranch)

    // It has been merged, so we can rename it and push it
    git.switch(mergedBranch).renameRemoteBranch(renamedBranch).push()
  }
}
