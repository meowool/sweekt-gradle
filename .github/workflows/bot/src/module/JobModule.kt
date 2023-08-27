package com.meowool.sweekt.gradle.module

import com.meowool.sweekt.gradle.job.BotJob
import com.meowool.sweekt.gradle.job.BumpVersionFileJob
import com.meowool.sweekt.gradle.job.CloseIssuesJob
import com.meowool.sweekt.gradle.job.MergeChangesJob
import com.meowool.sweekt.gradle.job.PinGradleWrapperJob
import com.meowool.sweekt.gradle.job.ReleaseDistributionJob
import com.meowool.sweekt.gradle.job.SyncUpstreamTagsJob
import org.koin.core.definition.Definition
import org.koin.core.module.Module
import org.koin.dsl.module

val JobModule get() = module {
  job { CloseIssuesJob(get(), get()) }
  job { MergeChangesJob(get(), get(), get()) }
  job { PinGradleWrapperJob(get(), get(), get()) }
  job { BumpVersionFileJob(get(), get(), get(), get()) }
  job { ReleaseDistributionJob(get(), get(), get(), get()) }
  job { SyncUpstreamTagsJob(get(), get()) }
}

private inline fun <reified T : BotJob> Module.job(
  noinline definition: Definition<T>,
) = factory {
  definition(it).apply {
    // Inject arguments
    context = get()
  }
}
