package com.meowool.sweekt.gradle.module

import com.meowool.sweekt.gradle.service.BotService
import com.meowool.sweekt.gradle.service.GitService
import com.meowool.sweekt.gradle.service.GithubRepositoryService
import com.meowool.sweekt.gradle.service.GradleService
import org.koin.core.qualifier.named
import org.koin.dsl.module

val ServiceModule get() = module {
  single { BotService(get(), get(), get()) }
  single { GradleService(get()) }
  single { GitService(get()) }
  factory { param ->
    GithubRepositoryService(
      context = param.getOrNull() ?: get(),
      client = get(named<GithubRepositoryService>()),
      json = get(),
    )
  }
}
