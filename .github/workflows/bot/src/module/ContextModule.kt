package com.meowool.sweekt.gradle.module

import com.meowool.sweekt.gradle.model.Context
import com.meowool.sweekt.gradle.model.GitRef
import com.meowool.sweekt.gradle.utils.env
import com.meowool.sweekt.gradle.utils.envOrNull
import io.ktor.http.HttpHeaders
import org.koin.dsl.module

val ContextModule get() = module {
  single<Context> {
    object : Context {
      override val repositoryWithOwner = env("GITHUB_REPOSITORY")
      override val githubServerUrl = env("GITHUB_SERVER_URL")
      override val githubApiUrl = env("GITHUB_API_URL")
      override val githubToken = envOrNull("GITHUB_CLIENT_TOKEN")
        ?: env("GITHUB_TOKEN")
      override val workflowRunId = env("GITHUB_RUN_ID")
      override val workflowTriggerRef = GitRef(
        fullName = env("GITHUB_REF"),
        shortName = env("GITHUB_REF_NAME"),
        isTag = env("GITHUB_REF_TYPE") == "tag",
      )
      override val headers: Map<String, String> = mapOf(
        HttpHeaders.Authorization to "token $githubToken",
        HttpHeaders.Accept to "application/vnd.github.v3+json",
      )
    }
  }
}
