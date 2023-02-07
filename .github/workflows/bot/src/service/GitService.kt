@file:Suppress("MemberVisibilityCanBePrivate")

package com.meowool.sweekt.gradle.service

import actions.core.isDebug
import actions.core.warning
import actions.exec.ExecOutput
import com.meowool.sweekt.gradle.model.Context
import com.meowool.sweekt.gradle.service.GradleService.Companion.VersionFile
import com.meowool.sweekt.gradle.service.GradleService.Companion.WrapperPropFile
import com.meowool.sweekt.gradle.utils.exec
import com.meowool.sweekt.gradle.utils.group
import com.meowool.sweekt.gradle.utils.retry
import com.meowool.sweekt.gradle.utils.withDebug
import node.fs.writeFile

class GitService(private val context: Context) {
  private var initialized = false

  /**
   * Initialize the git environment.
   */
  private suspend fun init() {
    if (initialized) return; initialized = true

    this("config", "--local", "user.name", "Meowool Robot")
    this("config", "--local", "user.email", "meowool@proton.me")

    // We need to resolve some conflicts automatically,
    // because they are not important
    this("config", "--local", "merge.ours.driver", "true")
    writeFile(
      ".git/info/attributes",
      """
        .idea/**/* merge=ours
        .github/**/* merge=theirs
        .gitignore merge=ours
        .editorconfig merge=ours
        $WrapperPropFile merge=ours
        $VersionFile merge=ours
      """.trimIndent()
    )

    if (isDebug()) group("🍀 Git config") { this("config", "--list") }
  }

  /**
   * Executes the git command and returns the result.
   *
   * @param args the arguments of the git command
   */
  private suspend operator fun invoke(vararg args: Any?): ExecOutput {
    init()
    return exec("git", *args)
  }

  private inline fun swallowError(action: () -> Unit) = apply {
    try {
      action()
    } catch (e: Throwable) {
      warning("⚠️ Unexpected: $e")
    }
  }

  suspend fun currentBranch() = withDebug("currentBranch") {
    this("branch", "--show-current").stdout.trim()
  }

  suspend fun renameLocalBranch(newBranch: Any) = apply {
    this("branch", "-m", newBranch)
  }

  suspend fun renameRemoteBranch(newBranch: Any) = apply {
    val oldBranch = currentBranch()
    if (oldBranch == newBranch) return@apply
    renameLocalBranch(newBranch).push()
    swallowError { deleteRemoteBranch(oldBranch) }
  }

  suspend fun deleteRemoteBranch(branch: Any) = apply {
    this("push", "origin", "--delete", branch)
  }

  suspend fun addRemote(name: String, url: String) = apply {
    this("remote", "add", name, url)
  }

  suspend fun containsRemoteBranch(branch: Any) = runCatching {
    this("ls-remote", "--heads", "--exit-code", "origin", branch)
  }.isSuccess

  suspend fun fetch(
    source: Any,
    destination: Any = source,
    remote: String = "origin",
    vararg options: String,
  ) = swallowError {
    this(
      "fetch",
      remote,
      "$source:$destination",
      "--update-head-ok",
      *options,
    )
  }

  suspend fun checkout(branch: Any, newBranch: Any? = null) = apply {
    // We need to fetch before checkout, otherwise will be failed
    if (containsRemoteBranch(branch)) fetch(branch)
    when (newBranch) {
      null -> this("checkout", branch)
      else -> this("checkout", "-b", newBranch, branch)
    }
  }

  suspend fun switch(branch: Any) = apply {
    this("switch", branch)
  }

  suspend fun commit(
    path: String,
    message: String,
    description: String? = null,
  ) = apply {
    this("add", path)
    this(
      "commit",
      "-m",
      buildString {
        appendLine(message); appendLine()
        description?.let { appendLine(it); appendLine() }
        appendLine("Committed-by: ${context.workflowRunUrl}")
      },
    )
  }

  suspend fun mergeFrom(branch: Any) = apply {
    // We need to fetch before merge
    fetch(branch)
    this("merge", branch, "--verbose")
  }

  suspend fun abortMerge() = swallowError {
    this("merge", "--abort")
  }

  suspend fun pull(branch: Any) = swallowError {
    this("pull", "origin", branch, "--verbose")
  }

  suspend fun push(skipCI: Boolean = true) = apply {
    if (skipCI) {
      // Commit empty message to skip CI
      this(
        "commit",
        "--allow-empty",
        "--message",
        "ci: add skip checks [skip actions]",
      )
    }
    val currentBranch = currentBranch()
    // We need to retry because the push sometimes fails
    retry("push", max = 5) {
      // Make sure we pull on the latest code
      if (containsRemoteBranch(currentBranch)) pull(currentBranch)
      this(
        "push",
        "origin",
        currentBranch,
        "--set-upstream",
        "--verbose",
      )
    }
  }
}
