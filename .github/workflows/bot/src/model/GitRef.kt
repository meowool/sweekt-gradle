@file:Suppress("unused")

package com.meowool.sweekt.gradle.model

/**
 * @author chachako
 */
sealed class GitRef(
  val fullName: String,
  val shortName: String,
) {
  abstract val isTag: Boolean
  abstract val isBranch: Boolean

  val branch get() = GithubBranch(shortName)

  class Tag(
    fullName: String,
    shortName: String,
  ) : GitRef(fullName, shortName) {
    override val isTag: Boolean = true
    override val isBranch: Boolean = false
  }

  class Branch(
    fullName: String,
    shortName: String,
  ) : GitRef(fullName, shortName) {
    override val isTag: Boolean = false
    override val isBranch: Boolean = true
  }

  companion object {
    operator fun invoke(
      fullName: String,
      shortName: String,
      isTag: Boolean,
    ): GitRef = when (isTag) {
      true -> Tag(fullName, shortName)
      false -> Branch(fullName, shortName)
    }
  }
}
