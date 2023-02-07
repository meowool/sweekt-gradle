package com.meowool.sweekt.gradle

import com.meowool.sweekt.gradle.job.BumpVersionFileJob
import com.meowool.sweekt.gradle.job.CloseIssuesJob
import com.meowool.sweekt.gradle.job.MergeChangesJob
import com.meowool.sweekt.gradle.job.PinGradleWrapperJob
import com.meowool.sweekt.gradle.job.ReleaseDistributionJob
import com.meowool.sweekt.gradle.job.SyncUpstreamTagsJob
import com.meowool.sweekt.gradle.job.jobChain
import com.meowool.sweekt.gradle.model.GitRef
import com.meowool.sweekt.gradle.module.ContextModule
import com.meowool.sweekt.gradle.module.JobModule
import com.meowool.sweekt.gradle.module.NetworkModule
import com.meowool.sweekt.gradle.module.ServiceModule
import com.meowool.sweekt.gradle.utils.envOrNull
import com.meowool.sweekt.gradle.utils.runAction
import org.koin.core.context.startKoin

suspend fun main() = runAction {
  // Inject dependencies
  startKoin {
    modules(
      ContextModule,
      NetworkModule,
      ServiceModule,
      JobModule,
    )
  }
  // Put all the jobs in a chain
  jobChain {
    when (envOrNull("RUN_TYPE")) {
      "Sync" -> beginWith<SyncUpstreamTagsJob>()
      "Release" -> beginWith<ReleaseDistributionJob>()
      else -> when (workflowTriggerRef) {
        is GitRef.Branch -> beginWith<CloseIssuesJob>()
          .then<MergeChangesJob>()
          .then<PinGradleWrapperJob>()
          .then<BumpVersionFileJob>()
        is GitRef.Tag -> beginWith<CloseIssuesJob>()
          .then<BumpVersionFileJob>()
      }
    }
  }.enqueue()
}
