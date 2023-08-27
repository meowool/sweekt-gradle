package com.meowool.sweekt.gradle.model

import kotlinx.serialization.Serializable

/**
 * @author chachako
 */
@Serializable
data class GithubBranch(val name: String) {
  override fun toString(): String = name
}
