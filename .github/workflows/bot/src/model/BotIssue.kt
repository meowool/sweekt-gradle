@file:Suppress("MemberVisibilityCanBePrivate")

package com.meowool.sweekt.gradle.model

/**
 * @author chachako
 */
data class BotIssue(private val raw: GithubIssue) {
  val related = RelatedRegex.find(body)?.groupValues?.get(1)

  val number get() = raw.number
  val title get() = raw.title
  val body get() = raw.body
  val url get() = raw.url
  val labels get() = raw.labels

  override fun toString(): String =
    "BotIssue(number=$number, title=$title, related=$related, url=$url)"

  companion object {
    private val RelatedRegex = Regex(
      pattern = "^<!-- related: (.*) -->\$",
      option = RegexOption.MULTILINE,
    )

    val PrimaryLabel = GithubIssue.Label("bot")
    val MergeLabel = GithubIssue.Label("bug: merge")
    val TestLabel = GithubIssue.Label("bug: test")
    val DistributeLabel = GithubIssue.Label("bug: distribute")
  }
}
