package com.meowool.sweekt.gradle.job

import com.meowool.sweekt.gradle.model.IntVersion.Companion.toIntVersion
import com.meowool.sweekt.gradle.service.BotService
import com.meowool.sweekt.gradle.service.GitService
import com.meowool.sweekt.gradle.service.GithubRepositoryService
import com.meowool.sweekt.gradle.service.GradleService
import com.meowool.sweekt.gradle.service.GradleService.Companion.VersionFile
import com.meowool.sweekt.gradle.utils.info
import com.meowool.sweekt.gradle.utils.writeFile
import kotlinx.coroutines.flow.toList

/**
 * @author chachako
 */
class BumpVersionFileJob(
  private val bot: BotService,
  private val git: GitService,
  private val gradle: GradleService,
  private val repo: GithubRepositoryService,
) : BotJob() {

  @Suppress("ktlint:max-line-length", "ktlint:argument-list-wrapping")
  override suspend fun start(input: BotJobData): BotJobResult {
    // There are two situations for this job, one is triggered when
    // the branch is pushed, and the other is triggered when the release
    // is created.
    //
    // 1. For the first case, we need to bump all pushed branches to the latest
    //    tag of repository release.
    // 2. For the second case, we need to bump all branches to the new created
    //    release tag.
    val (branches, tag) = when (val branches = input.getOrNull(Keys.Branches)) {
      // This means that the new release creates
      null -> {
        val tag = workflowTriggerRef.shortName
        require(workflowTriggerRef.isTag) {
          "The trigger ref must be a tag: `$tag`."
        }
        info("🆕 Release '$tag' is created, bumping version for 'changed/*' branches...")
        Pair(bot.changedBranches.toList(), tag)
      }
      // This means that the branches pushes
      else -> Pair(branches, repo.latestRelease().tag)
    }

    branches.forEach { bumpVersion(it, tag) }

    return jobSuccess()
  }

  private suspend fun bumpVersion(branch: Any, latestReleaseTag: String) {
    val version = gradle.calculateVersion(branch)
    val sweektVersion = version.baseVersion.sweekt
    val latestSweektVersion = latestReleaseTag.toIntVersion()

    // All branches have merged the latest changes before this step,
    // so we don't need to worry
    val result = if (sweektVersion != latestSweektVersion) {
      val newVersion = version.copy(
        baseVersion = version.baseVersion.copy(sweekt = latestSweektVersion),
      )

      // Update the version file
      VersionFile.writeFile(newVersion.baseVersion.toSweekt())
      git.commit(
        path = VersionFile,
        message = "release: ${newVersion.fullSweektVersion}"
      ).push(skipCI = false)

      "Current Sweekt version is bumped to ${newVersion.fullSweektVersion}"
    } else {
      "Current Sweekt version is consistent with latest release"
    }

    info("🎉 $result")
  }
}
