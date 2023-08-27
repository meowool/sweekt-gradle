package com.meowool.sweekt.gradle.model

/**
 * @author chachako
 */
interface Context {
  val repositoryWithOwner: String
  val githubServerUrl: String
  val githubApiUrl: String
  val githubToken: String
  val workflowRunId: String
  val workflowTriggerRef: GitRef
  val headers: Map<String, String>

  val repositoryName: String
    get() = repositoryWithOwner.substringAfter('/')

  val repositoryOwner: String
    get() = repositoryWithOwner.substringBefore('/')

  val repositoryUrl: String
    get() = "$githubServerUrl/$repositoryWithOwner"

  val workflowRunUrl: String
    get() = "$repositoryUrl/actions/runs/$workflowRunId"

  fun copy(
    repositoryWithOwner: String = this.repositoryWithOwner,
    githubServerUrl: String = this.githubServerUrl,
    githubApiUrl: String = this.githubApiUrl,
    githubToken: String = this.githubToken,
    workflowRunId: String = this.workflowRunId,
    workflowTriggerRef: GitRef = this.workflowTriggerRef,
    headers: Map<String, String> = this.headers,
  ): Context = object : Context {
    override val repositoryWithOwner = repositoryWithOwner
    override val githubServerUrl = githubServerUrl
    override val githubApiUrl = githubApiUrl
    override val githubToken = githubToken
    override val workflowRunId = workflowRunId
    override val workflowTriggerRef = workflowTriggerRef
    override val headers = headers
  }

  abstract class Delegate : Context {
    abstract val context: Context

    override val repositoryWithOwner get() = context.repositoryWithOwner
    override val githubServerUrl get() = context.githubServerUrl
    override val githubApiUrl get() = context.githubApiUrl
    override val githubToken get() = context.githubToken
    override val workflowRunId get() = context.workflowRunId
    override val workflowTriggerRef get() = context.workflowTriggerRef
    override val headers get() = context.headers
  }
}
