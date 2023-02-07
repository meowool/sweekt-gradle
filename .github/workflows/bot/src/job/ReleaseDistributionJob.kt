package com.meowool.sweekt.gradle.job

import actions.core.error
import actions.core.warning
import com.meowool.sweekt.gradle.model.BotIssue
import com.meowool.sweekt.gradle.model.BotIssueBodyTemplate.Companion.createBotIssueBodyTemplate
import com.meowool.sweekt.gradle.model.GithubRelease
import com.meowool.sweekt.gradle.model.GradleDistribution
import com.meowool.sweekt.gradle.model.GradleVersion
import com.meowool.sweekt.gradle.model.IntVersion.Companion.toIntVersion
import com.meowool.sweekt.gradle.service.BotService
import com.meowool.sweekt.gradle.service.GitService
import com.meowool.sweekt.gradle.service.GithubRepositoryService
import com.meowool.sweekt.gradle.service.GradleService
import com.meowool.sweekt.gradle.utils.div
import com.meowool.sweekt.gradle.utils.info
import com.meowool.sweekt.gradle.utils.retry
import com.meowool.sweekt.gradle.utils.withDebug

/**
 * @author chachako
 */
class ReleaseDistributionJob(
  private val bot: BotService,
  private val git: GitService,
  private val gradle: GradleService,
  private val repository: GithubRepositoryService,
) : BotJob() {

  override suspend fun start(input: BotJobData): BotJobResult {
    val version = gradle.calculateVersion()
    val latestRelease = repository.latestRelease()
    // We only release branches with the latest version of changes
    if (version.baseVersion.sweekt == latestRelease.tag.toIntVersion()) {
      if (test(version, latestRelease)) release(version, latestRelease)
    } else {
      warning(
        "🤔 ReleaseDistributionJob was accidentally triggered, " +
          "because the 'sweekt version' in the `version.txt` file of the " +
          "current branch does not match the latest release of Github, " +
          "so we cancel the job.",
      )
    }

    return jobSuccess()
  }

  /**
   * Test all modifications to Gradle in our distribution.
   *
   * @return `true` if all tests are passed.
   */
  private suspend fun test(
    version: GradleVersion,
    latestRelease: GithubRelease,
  ) = runCatching {
    gradle.disableVerification {
      gradle.clean()
      bot.defaultChangedModules().also { modules ->
        retry("testing '$modules' modules", max = 3) {
          gradle(*modules.map { ":$it:quickTest" }.toTypedArray())
        }
      }
    }
  }.onSuccess {
    info("✅ All tests are passed.")
    bot.relatedIssues(
      related = version.fullSweektVersion,
      BotIssue.TestLabel,
    ).forEach {
      repository.commentIssue(
        number = it.number,
        body = """
          Now all tests are passed, so we can close this issue.

          > View the full workflow running log in $workflowRunUrl
        """.trimIndent(),
      )
      repository.closeIssue(it.number)
    }
  }.onFailure {
    val failedBranch = git.currentBranch()
    error(
      "🚨 Failed to test the '$failedBranch' branch, stacktrace: \n" +
        it.stackTraceToString(),
    )
    bot.createIssue(
      title = "Failed to test the distribution on the `$failedBranch` branch",
      body = createBotIssueBodyTemplate(context) {
        sections += "Affected Version" to latestRelease.htmlUrl
        errors = it
        related = version.fullSweektVersion
      },
      labels = listOf(BotIssue.TestLabel),
    )
  }.isSuccess

  /**
   * Release all zip files of our distribution.
   */
  @Suppress("ktlint:max-line-length", "ktlint:argument-list-wrapping")
  private suspend fun release(
    version: GradleVersion,
    latestRelease: GithubRelease,
  ) = withDebug("release($version, $latestRelease)") {
    var isFailed = false

    gradle.disableVerification {
      GradleDistribution.values().forEach { distribution ->
        // File name after build
        val buildFileName = distribution.fileName(version.baseVersion.toSweekt())
        // File name when releasing
        val releaseFileName = distribution.fileName(version.fullSweektVersion)

        // Make sure this distribution has not been released before
        if (latestRelease.assetNames.contains(releaseFileName)) {
          info("🐣 Distribution '$releaseFileName' already exists, skipping...")
          return@forEach
        }

        bot.relatedIssues(releaseFileName, BotIssue.DistributeLabel).apply {
          if (isNotEmpty()) {
            info(
              "🚫 Skip releasing '$releaseFileName' because it has failed " +
                "before: ${joinToString { it.url }}",
            )
            return@forEach
          }
        }

        runCatching {
          // We need to retry because the CI build sometimes fails
          retry("build '$buildFileName'", max = 3) {
            val versionProperty = version.rcNumber?.let { "rcNumber=$it" }
              ?: version.milestoneNumber?.let { "milestoneNumber=$it" }
              ?: "finalRelease=true"

            gradle(distribution.buildTask, properties = arrayOf(versionProperty))
          }
        }.onFailure {
          error("❌ Failed to release the distribution: $it")
          // Create an issue to track errors
          bot.createIssue(
            title = "Unable to release `$releaseFileName` distribution " +
              "to `${latestRelease.tag}`",
            body = createBotIssueBodyTemplate(context) {
              errors = it
              related = releaseFileName
            },
            labels = listOf(BotIssue.DistributeLabel),
          )
          isFailed = true
        }

        info("⬆️ Uploading distribution '$releaseFileName' to GitHub...")

        val downloadUrl = repository.uploadReleaseAsset(
          release = latestRelease,
          name = releaseFileName,
          file = GradleDistribution.BuildDirectory / buildFileName,
        ).downloadUrl

        info(
          "🍾 Distribution has been uploaded to the ${latestRelease.htmlUrl}, " +
            "the download URL is $downloadUrl",
        )
      }
    }

    if (!isFailed) {
      info("🎉 All zip files of the distribution was released successfully.")
    }
  }
}
