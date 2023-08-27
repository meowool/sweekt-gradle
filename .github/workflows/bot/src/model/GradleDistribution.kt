package com.meowool.sweekt.gradle.model

import kotlin.io.path.Path

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
    val BuildDirectory = Path(
      "subprojects/distributions-full/build/distributions"
    )
  }
}
