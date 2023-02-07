package com.meowool.sweekt.gradle.service

import actions.core.debug
import actions.core.warning
import com.meowool.sweekt.gradle.model.BotIssue
import com.meowool.sweekt.gradle.model.BotIssueBodyTemplate
import com.meowool.sweekt.gradle.model.BotIssueBodyTemplate.Companion.createBotIssueBodyTemplate
import com.meowool.sweekt.gradle.model.Context
import com.meowool.sweekt.gradle.model.GithubBranch
import com.meowool.sweekt.gradle.model.GithubIssue
import com.meowool.sweekt.gradle.model.MergeChangesResult
import com.meowool.sweekt.gradle.model.Semver
import com.meowool.sweekt.gradle.utils.group
import com.meowool.sweekt.gradle.utils.info
import com.meowool.sweekt.gradle.utils.preserveFile
import com.meowool.sweekt.gradle.utils.withDebug
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.asFlow
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.takeWhile
import kotlinx.coroutines.flow.toList
import com.meowool.sweekt.gradle.service.GradleService.Companion.VersionFile as GradleVersionFile

/**
 * @author chachako
 */
class BotService(
  private val context: Context,
  private val repository: GithubRepositoryService,
  private val git: GitService,
) {
  private val openIssues = repository.issues(
    GithubIssue.State.Open,
    BotIssue.PrimaryLabel,
  ).map(::BotIssue).onEach { debug("🫣 openIssues.each: $it") }

  val changedBranches = flow {
    val defaultBranch = repository.defaultBranch()
    emit(defaultBranch)
    emitAll(
      repository.branches.filter {
        it.name != defaultBranch.name && it.name.startsWith(ChangedPrefix)
      },
    )
  }.onEach { debug("🫣 changedBranches.each: $it") }

  suspend fun createIssue(
    title: Any,
    body: Any,
    milestone: Int? = null,
    labels: List<GithubIssue.Label> = emptyList(),
    assignees: List<String> = emptyList(),
    assign: Boolean = false,
  ) = repository.createIssue(
    title.toString(),
    body.toString(),
    milestone,
    labels = listOf("bot", *labels.map { it.name }.toTypedArray()),
    assignees = listOfNotNull(
      "chachako".takeIf { assign },
      *assignees.toTypedArray(),
    ),
  )

  /**
   * Tries to merge Sweekt changes using any [fromBranches] into each
   * [toBranches].
   *
   * ### How does it work?
   *
   * 1. Start iterating all [toBranches].
   * 2. Try to merge any [fromBranches] into the current `toBranch`.
   * 3. When merging succeeds, call [onSuccess], otherwise record the
   *    failure reason to "errorReasons".
   * 4. If all [fromBranches] tries fail, call [onFail].
   * 5. Finally, return the successful and failed branches.
   *
   * @param fromBranches the branch or branches flow to merge from.
   * @param toBranches the branch or branches flow to merge to.
   */
  suspend fun mergeChanges(
    fromBranches: Any,
    toBranches: Any,
    onFail: suspend BotIssueBodyTemplate.(GithubBranch) -> Unit = {},
    onSuccess: suspend (GithubBranch) -> Unit = {},
  ): MergeChangesResult {
    fun flowBranches(branches: Any) = when (branches) {
      is Flow<*> -> branches.map { it as GithubBranch }
      is List<*> -> branches.map { it as GithubBranch }.asFlow()
      is GithubBranch -> flowOf(branches)
      else -> error("Unsupported branches: ${branches::class.simpleName}")
    }

    val fromFlow = flowBranches(fromBranches)
    val toFlow = flowBranches(toBranches)
    val successBranches = mutableListOf<GithubBranch>()
    val failedBranches = mutableListOf<GithubBranch>()

    toFlow.collect { toBranch ->
      val errorReasons = mutableMapOf<String, Throwable>()
      val toVersion = parseBranchVersion(toBranch)
      val isSuccess = fromFlow.filter {
        // Make sure that the version of the 'fromBranch' is smaller than
        // the 'toBranch', otherwise we will mistakenly merge the high
        // version history to the lower version
        val isSmaller = parseBranchVersion(it) < toVersion
        if (!isSmaller) info("🚫 Skip merge from '$it' because it's not smaller than the target '$toBranch'.") // ktlint-disable argument-list-wrapping max-line-length
        isSmaller
      }.takeWhile { fromBranch ->
        group("🧪 Try to merge from '$fromBranch' into '$toBranch'") {
          runCatching {
            git.checkout(toBranch)
            val versionChanged = preserveFile(GradleVersionFile) {
              git.mergeFrom(fromBranch)
            }
            if (versionChanged) {
              git.commit(GradleVersionFile, "revert `$GradleVersionFile`")
            }
          }.onSuccess {
            info("✅ Merge from '$fromBranch' into '$toBranch' successfully!")
            successBranches += toBranch
            onSuccess(toBranch)
          }.onFailure {
            git.abortMerge()
            errorReasons["Merge from `$fromBranch`"] = it
            warning("💣 Skip merge from '$fromBranch' because it failed: $it")
          }.isSuccess // Stop merging once the merge is successful
        }
      }.firstOrNull() != null

      if (!isSuccess) {
        failedBranches += toBranch
        // Report all errors of the current 'toBranch' if all
        // "fromBranches" fails
        createBotIssueBodyTemplate(context) {
          errors = errorReasons
        }.onFail(toBranch)
      }
    }

    return MergeChangesResult(successBranches, failedBranches)
  }

  fun parseBranchVersion(branch: Any) =
    Semver(branch.toString().substringAfter('/'))

  fun renameToChangedBranch(branch: Any) =
    ChangedPrefix + parseBranchVersion(branch)

  fun renameToUnchangedBranch(branch: Any) =
    UnchangedPrefix + parseBranchVersion(branch)

  suspend fun relatedIssues(
    related: String,
    vararg labels: GithubIssue.Label,
  ): List<BotIssue> = withDebug("relateIssues($related, $labels)") {
    openIssues.filter {
      it.related == related && it.labels.containsAll(labels.toList())
    }.toList().apply {
      if (isNotEmpty()) {
        info(
          "🔗 Issues ${joinToString { it.url }} are related to $related",
        )
      }
    }
  }

  /**
   * Returns the name of the Gradle module modified by the default branch.
   *
   * This reflects all the modifications to Gradle in our distribution.
   */
  suspend fun defaultChangedModules(): Set<String> = context.run {
    val branch = repository.defaultBranch()
    val upstream = parseBranchVersion(branch)

    return repository
      .compareCommits("$upstream...$repositoryOwner:$repositoryName:$branch")
      .files.filter { it.filename.startsWith("subprojects/") }
      // subprojects/module/... -> module
      .map { it.filename.substringAfter('/').substringBefore('/') }
      .toSet()
  }

  companion object {
    const val ChangedPrefix = "changed/"
    const val UnchangedPrefix = "unchanged/"
  }
}
