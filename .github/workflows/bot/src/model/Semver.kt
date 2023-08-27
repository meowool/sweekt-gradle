package com.meowool.sweekt.gradle.model

import io.github.z4kn4fein.semver.Version
import io.github.z4kn4fein.semver.toVersionOrNull

/**
 * @author chachako
 */
@JvmInline
value class Semver(private val str: String) : Comparable<Semver> {
  private val version: Version
    get() = str.toVersionOrNull(strict = false) ?: Version.min

  constructor(str: Any) : this(str.toString())

  override fun compareTo(other: Semver): Int = version.compareTo(other.version)

  override fun toString(): String = "v$version"
}
