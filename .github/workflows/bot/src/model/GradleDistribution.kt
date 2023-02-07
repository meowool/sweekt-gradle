package com.meowool.sweekt.gradle.model

/**
 * @author chachako
 */
enum class GradleDistribution {
  Bin,
  Src;

  val buildTask: String get() = "${this}DistributionZip"

  fun fileName(version: String) = "gradle-$version-$this.zip"

  override fun toString() = name.lowercase()

  companion object {
    const val BuildDirectory = "subprojects/distributions-full/build/distributions" // ktlint-disable max-line-length
  }
}
