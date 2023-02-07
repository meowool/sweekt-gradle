package com.meowool.sweekt.gradle.model

import semverCompare
import semverValid

/**
 * @author chachako
 */
value class Semver(private val version: String) : Comparable<Semver> {
  constructor(version: Any) : this(version.toString())

  override fun compareTo(other: Semver): Int =
    semverCompare(validVersion(), other.version)

  override fun toString(): String = "v" + validVersion()

  private fun validVersion() = semverValid(version) ?: "0.0.0"
}
