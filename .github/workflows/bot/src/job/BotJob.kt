@file:Suppress("unused")

package com.meowool.sweekt.gradle.job

import com.meowool.sweekt.gradle.model.Context
import com.meowool.sweekt.gradle.model.GithubBranch
import org.koin.core.component.KoinComponent

/**
 * @author chachako
 */
abstract class BotJob : Context.Delegate(), KoinComponent {
  override lateinit var context: Context

  /**
   * Override this method to implement the job. The method is called when
   * the job is ready to be executed. The method must return a [BotJobResult]
   * to indicate the result of the job.
   *
   * @param input The input data of the job, which is a combination of
   *   the input data of all jobs that this job depends on.
   */
  abstract suspend fun start(input: BotJobData): BotJobResult

  /**
   * A collection of common keys for storing and obtaining [BotJobData].
   *
   * @author chachako
   */
  object Keys {
    val Branches = jobDataKey<Collection<GithubBranch>>("Branches")
  }
}
