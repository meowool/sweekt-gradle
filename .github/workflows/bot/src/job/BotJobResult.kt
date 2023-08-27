package com.meowool.sweekt.gradle.job

/**
 * The result of a [BotJob]s operation.
 *
 * @author chachako
 */
sealed class BotJobResult {
  /**
   * Used to indicate that the job completed successfully. Any job that
   * depends on this can continue to be executed.
   *
   * @param output The output data of the job which will be merged into
   *   any input that depends on this job.
   */
  data class Success(val output: BotJobData) : BotJobResult()

  /**
   * Used to indicate that the job completed with a permanent failure.
   * Any job that depends on this will also be marked as failed and will
   * not be run. This means that the chain of job will stop permanently.
   */
  object Failure : BotJobResult()
}

/**
 * Creates an instance of [BotJobResult.Success] with the given [output].
 */
fun jobSuccess(output: BotJobData = emptyJobData()) =
  BotJobResult.Success(output)

/**
 * Returns an instance of [BotJobResult.Failure].
 */
fun jobFailure() = BotJobResult.Failure
