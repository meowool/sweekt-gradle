package com.meowool.sweekt.gradle.job

import com.meowool.sweekt.gradle.model.BotIssue
import com.meowool.sweekt.gradle.model.GitRef
import com.meowool.sweekt.gradle.service.BotService
import com.meowool.sweekt.gradle.service.GithubRepositoryService
import com.meowool.sweekt.gradle.utils.info

/**
 * @author chachako
 */
class CloseIssuesJob(
  private val bot: BotService,
  private val repository: GithubRepositoryService,
) : BotJob() {
  override suspend fun start(input: BotJobData): BotJobResult {
    require(input.isEmpty()) { "CloseIssuesJob doesn't need any input data." }

    val issues = when (val ref = workflowTriggerRef) {
      is GitRef.Branch -> bot.relatedIssues(
        related = ref.shortName,
        BotIssue.MergeLabel,
      ).onEach { closeIssue(it) }

      is GitRef.Tag -> repository.release(ref.shortName).let { release ->
        release.assets.flatMap { asset ->
          bot.relatedIssues(
            related = asset.name,
            BotIssue.DistributeLabel,
          ).onEach { closeIssue(it) }
        }
      }
    }

    if (issues.isEmpty()) {
      info("👍 No issues to close.")
    }

    return jobSuccess()
  }

  private suspend fun closeIssue(issue: BotIssue) {
    val related = issue.related ?: workflowTriggerRef
    info("🙌 Closing issue $issue because it's related to $related.")
    repository.commentIssue(
      number = issue.number,
      body = """
        Closing this issue because `$related` related to it has been updated.

        > View the full workflow running log in $workflowRunUrl
      """.trimIndent(),
    )
    repository.closeIssue(issue.number)
  }
}
