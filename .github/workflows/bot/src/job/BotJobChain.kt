package com.meowool.sweekt.gradle.job

import AnsiColors.magentaBright
import com.meowool.sweekt.gradle.model.Context
import com.meowool.sweekt.gradle.utils.log
import kotlin.reflect.KClass
import org.koin.core.context.GlobalContext.get as getKoin

/**
 * A chain of [BotJob]s that can be used to enqueue jobs in order.
 *
 * @author chachako
 */
class BotJobChain : Context.Delegate() {
  override val context: Context = getKoin().get()

  /**
   * Begins a chain with a job, which can be enqueued with
   * [Continuation.enqueue] in the future.
   *
   * If any job in the chain fails or is cancelled, all of its dependent
   * job inherits that state and will never run.
   *
   * @return A [Continuation] that allows for further chaining of
   *   dependent jobs.
   */
  inline fun <reified T : BotJob> beginWith(): Continuation =
    Continuation().then(T::class)

  /**
   * A class that allows you to chain jobs together in order.
   *
   * @author chachako
   */
  class Continuation {
    private val jobs = mutableListOf<BotJob>()

    /**
     * Adds a new job that depend on the successful completion of all
     * previously added jobs.
     *
     * @return This [Continuation] for further chaining.
     */
    fun <T : BotJob> then(job: KClass<T>): Continuation = apply {
      jobs.add(getKoin().get(job))
    }

    /**
     * Adds a new job that depend on the successful completion of all
     * previously added jobs.
     *
     * @return This [Continuation] for further chaining.
     */
    inline fun <reified T : BotJob> then(): Continuation = then(T::class)

    /**
     * Enqueues all jobs in the chain.
     */
    @Suppress("ktlint:max-line-length", "ktlint:argument-list-wrapping")
    suspend fun enqueue() {
      jobs.fold(emptyJobData()) { input, job ->
        log(magentaBright("*****************************************************"))
        log(magentaBright("🌈 Starting '${job::class.simpleName}'..."))
        val result = job.start(input)
        log(magentaBright("🌈 Finished '${job::class.simpleName}'"))
        log(magentaBright("*****************************************************"))
        when (result) {
          is BotJobResult.Success -> input + result.output
          is BotJobResult.Failure -> return
        }
      }
    }
  }
}

/**
 * Creates a chain of jobs that can be enqueued in the future.
 *
 * @return A [BotJobChain.Continuation] that allows you to enqueue the
 *   jobs in the chain.
 */
inline fun jobChain(
  block: BotJobChain.() -> BotJobChain.Continuation,
): BotJobChain.Continuation = BotJobChain().let(block)
