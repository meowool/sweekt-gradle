package com.meowool.sweekt.gradle.job

import com.meowool.sweekt.gradle.service.GitService
import com.meowool.sweekt.gradle.service.GithubRepositoryService
import com.meowool.sweekt.gradle.service.GradleService
import com.meowool.sweekt.gradle.service.GradleService.Companion.WrapperPropFile

/**
 * @author chachako
 */
class PinGradleWrapperJob(
  private val git: GitService,
  private val gradle: GradleService,
  private val repository: GithubRepositoryService,
) : BotJob() {
  override suspend fun start(input: BotJobData): BotJobResult {
    input[Keys.Branches].forEach {
      // The default branch never needs to try to pin
      if (it == repository.defaultBranch()) return@forEach

      // Pin the Gradle wrapper version
      gradle.pinWrapperVersion()?.also { newVersion ->
        git.checkout(it).commit(
          path = WrapperPropFile,
          message = "chore: pin gradle wrapper version to `$newVersion`",
          description = "Using the released version can prevent snapshot " +
            "versions from being removed from the server in the future.",
        ).push()
      }
    }

    return jobSuccess()
  }
}
